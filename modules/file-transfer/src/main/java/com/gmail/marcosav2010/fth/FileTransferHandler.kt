package com.gmail.marcosav2010.fth

import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.fth.packet.PacketFileAccept
import com.gmail.marcosav2010.fth.packet.PacketFileRequest
import com.gmail.marcosav2010.fth.packet.PacketFileSend
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.tasker.Task
import com.gmail.marcosav2010.tasker.Tasker
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

/**
 * This class handles file related packets, this includes file sending,
 * downloading and accepting.
 *
 * @author Marcos
 */
class FileTransferHandler(module: FTModule, private val connection: Connection) {

    internal val log: ILog = module.log

    private val pendingFileRequests: MutableMap<Int, FileReceiveInfo> = ConcurrentHashMap()
    private val pendingForAcceptFiles: MutableMap<Int, FileSendInfo> = ConcurrentHashMap()
    private val pendingReceiveFiles: MutableMap<Int, FileReceiveInfo?> = ConcurrentHashMap()
    private val downloading: MutableMap<Int, Int> = ConcurrentHashMap()
    private val pendingTasks: MutableMap<Int, Task> = HashMap()
    private val nextId: AtomicInteger = AtomicInteger(1)

    fun isPendingRequest(id: Int) = pendingFileRequests.containsKey(id)

    private fun isPendingForAccept(id: Int) = pendingForAcceptFiles.containsKey(id)

    private fun isPendingReceive(id: Int) = pendingReceiveFiles.containsKey(id)

    //fun isDownloading(id: Int) = downloading.containsKey(id)

    fun getRequest(id: Int) = pendingFileRequests[id]

    fun handleRequest(pf: PacketFileRequest) {
        val id = pf.fileID
        if (isPendingRequest(id)) return
        pendingFileRequests[id] = FileReceiveInfo(pf.name, pf.blocks)
    }

    fun handleReceiveFile(p: PacketFileSend): FileDownloadResult {
        val id = p.fileID
        removeTask(id)

        if (!isPendingReceive(id))
            return FileDownloadResult.NOT_PENDING_OR_TIMED_OUT

        val info = pendingReceiveFiles[id]
        info!!.setFirstArrivalTime()

        val single = info.isSingle
        val blocks = info.blocks
        var remaining = 0

        if (!single) {
            remaining = downloading.getOrDefault(id, blocks) - 1
            downloading[id] = remaining
        }

        val last = remaining == 0

        if (last) {
            pendingReceiveFiles.remove(id)
            downloading.remove(id)
        }

        val bytes = p.bytes
        val hash = p.hash
        val bBuffer = ByteBuffer.wrap(bytes)
        val pointer = p.pointer

        try {
            val newHash = getHash(bytes)
            if (!hash.contentEquals(newHash))
                return FileDownloadResult.HASH_MISMATCH

            val file = File(DOWNLOAD_FOLDER + info.fileName)
            file.parentFile.mkdirs()
            FileChannel.open(file.toPath(), StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE).use { channel -> channel.write(bBuffer, pointer.toLong()) }
            val dw = 100.0 - remaining.toDouble() / blocks * 100.0

            if (last) {
                val elapsed = System.currentTimeMillis() - info.firstArrivalTime
                val speed = file.length() / elapsed * 1000
                log.log("File #" + id + " \"" + info.fileName + "\" has been downloaded successfully (" + elapsed
                        + "ms | " + Utils.formatSize(speed) + "/s).")

            } else if (info.updateLastArrivalTime(DOWNLOAD_STATUS_INFO_WAIT))
                log.log(String.format("File #%s \"%s\" %.2f%% downloaded.", id, info.fileName, dw))

        } catch (ex: Exception) {
            log.log(ex, "There was an exception writing the file #$id.")
            return FileDownloadResult.WRITE_EXCEPTION
        }

        if (!last)
            addTask(id, { onDownloadTimeout(id) }, 20L, TimeUnit.SECONDS)

        return FileDownloadResult.SUCCESS
    }

    fun handleAcceptResponse(p: PacketFileAccept): FileSendResult {
        val id = p.fileID
        removeTask(id)

        if (!isPendingForAccept(id))
            return FileSendResult.NOT_PENDING_OR_TIMED_OUT

        val info = pendingForAcceptFiles.remove(id)

        if (!connection.isConnected())
            return FileSendResult.CONNECTION_EXPIRED

        val path = info!!.path
        val file = path.toFile()
        val fileBytes = info.size
        val blocks = info.blocks
        val blockSize = info.blockSize

        try {
            checkFile(file, fileBytes)
        } catch (ex: IllegalArgumentException) {
            log.log(ex)
            return FileSendResult.FILE_EXCEPTION
        }

        val tasks = blocks.coerceAtMost(MAX_TASKS)
        val blocksPerThread = ceil(blocks.toDouble() / tasks).toInt()

        log.log("Computing File #" + id + " \"" + info.fileName + "\" send, splitting up in " + blocks
                + " block(s) [" + Utils.formatSize(blockSize.toLong()) + "/block], using " + tasks + " tasks(s) ["
                + blocksPerThread + " block(s)/task] and hashing with " + HASH_ALGORITHM + "...", VerboseLevel.HIGH)

        val exception = AtomicReference<FileSendResult>()
        val execService = Executors.newFixedThreadPool(tasks)
        val t1 = System.currentTimeMillis()

        try {
            FileChannel.open(info.path, StandardOpenOption.READ).use { channel ->
                for (t in 0 until tasks) {
                    val startBlock = t * blocksPerThread
                    val endBlock = (startBlock + blocksPerThread).coerceAtMost(blocks)
                    execService.submit(
                            createSendProcess(channel, fileBytes, startBlock, endBlock, blockSize, exception, id))
                }
                execService.shutdown()
                execService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
            }
        } catch (e: IOException) {
            log.log(e)
            return FileSendResult.READ_EXCEPTION
        } catch (e: InterruptedException) {
            log.log(e)
            return FileSendResult.OTHER_EXCEPTION
        }

        val t2 = System.currentTimeMillis()
        val r = exception.get()
        if (r != null) return r

        val elapsed = t2 - t1
        val speed = fileBytes / elapsed * 1000
        log.log("File #" + id + " \"" + info.fileName + "\" has been sent successfully (" + elapsed + "ms | "
                + Utils.formatSize(speed) + "/s).")

        return FileSendResult.SUCCESS
    }

    private fun createSendProcess(channel: FileChannel,
                                  fileBytes: Int,
                                  startBlock: Int,
                                  endBlock: Int,
                                  blockSize: Int,
                                  exception: AtomicReference<FileSendResult>,
                                  id: Int)
            : () -> FileSendResult = task@{

        var i = startBlock

        while (i < endBlock) {
            val ex = exception.get()
            if (ex != null)
                return@task ex

            val start = i * blockSize
            val length = (fileBytes - start).coerceAtMost(blockSize)
            val temp = ByteBuffer.allocate(length)
            channel.read(temp, start.toLong())
            val block = temp.array()

            val hash = try {
                getHash(block)
            } catch (e: NoSuchAlgorithmException) {
                log.log(e)
                return@task exception.getAndSet(FileSendResult.HASHING_EXCEPTION)
            }

            if (!connection.isConnected())
                return@task exception.getAndSet(FileSendResult.CONNECTION_EXPIRED)

            try {
                connection.sendPacket(PacketFileSend(id, start, block, hash))
            } catch (e: PacketWriteException) {
                log.log(e)
                return@task exception.getAndSet(FileSendResult.PACKET_WRITE_EXCEPTION)
            }
            i++
        }

        FileSendResult.SUCCESS
    }

    internal fun sendRequest(info: FileSendInfo,
                             expireTimeout: Long = ACCEPT_TIMEOUT,
                             timeUnit: TimeUnit = TimeUnit.SECONDS) {

        val id = nextId.getAndIncrement()
        info.setFileID(id)
        val p = PacketFileRequest(info)
        try {
            connection.sendPacket(p)
        } catch (e: PacketWriteException) {
            log.log(e)
            return
        }
        pendingForAcceptFiles[id] = info

        addTask(id, { onAcceptTimeout(id) }, expireTimeout, timeUnit)
    }

    @JvmOverloads
    fun acceptRequest(id: Int, receiveTimeout: Long = RECEIVE_TIMEOUT, timeUnit: TimeUnit = TimeUnit.SECONDS) {
        pendingReceiveFiles[id] = pendingFileRequests.remove(id)
        try {
            connection.sendPacket(PacketFileAccept(id))
        } catch (e: PacketWriteException) {
            log.log(e)
        }

        addTask(id, { onReceiveTimeout(id) }, receiveTimeout, timeUnit)
    }

    fun rejectRequest(id: Int) = pendingFileRequests.remove(id)

    @Synchronized
    private fun removeTask(packetId: Int) {
        if (pendingTasks.containsKey(packetId)) pendingTasks.remove(packetId)!!.cancel()
    }

    @Synchronized
    private fun addTask(id: Int, runnable: () -> Unit, receiveTimeout: Long, timeUnit: TimeUnit) {
        removeTask(id)
        pendingTasks[id] = Tasker.schedule(connection.peer, runnable, receiveTimeout, timeUnit)
    }

    private fun onReceiveTimeout(id: Int) = pendingReceiveFiles.remove(id)

    private fun onAcceptTimeout(id: Int) = pendingForAcceptFiles.remove(id)

    private fun onDownloadTimeout(id: Int) = downloading.remove(id)

    enum class FileSendResult {
        NOT_PENDING_OR_TIMED_OUT, CONNECTION_EXPIRED, FILE_EXCEPTION, READ_EXCEPTION, HASHING_EXCEPTION, PACKET_WRITE_EXCEPTION, OTHER_EXCEPTION, SUCCESS
    }

    enum class FileDownloadResult {
        NOT_PENDING_OR_TIMED_OUT, WRITE_EXCEPTION, HASH_MISMATCH, SUCCESS
    }

    companion object {
        const val DOWNLOAD_FOLDER = "FilesTransferred/"

        private const val HASH_BITS: Short = 224
        const val HASH_SIZE = (224 / java.lang.Byte.SIZE.toByte()).toByte()

        //private const val MAX_FILE_SIZE = Int.MAX_VALUE // 4 GB
        //val BLOCK_LIMIT = (MAX_FILE_SIZE / PacketFileSend.MAX_BLOCK_SIZE).toShort()

        private const val MIN_BLOCK_SIZE = Short.MAX_VALUE * 4 // 128 KB
        val MAX_TASKS = Runtime.getRuntime().availableProcessors() / 2 + 1

        private const val DOWNLOAD_STATUS_INFO_WAIT: Long = 2000
        private const val HASH_ALGORITHM = "SHA3-$HASH_BITS"
        private const val ACCEPT_TIMEOUT = 30L
        private const val RECEIVE_TIMEOUT = 10L

        fun createRequest(file: File): FileSendInfo {
            val size = file.length().toInt()
            val upSizeThreshold = Short.MAX_VALUE * 32 * 200
            val blocks: Int
            val blockSize: Int

            when {
                size <= MIN_BLOCK_SIZE -> {
                    blockSize = size
                    blocks = 1
                }
                size >= upSizeThreshold -> {
                    blockSize = PacketFileSend.MAX_BLOCK_SIZE
                    blocks = ceil(size.toDouble() / blockSize).toInt()
                }
                else -> {
                    val k = (PacketFileSend.MAX_BLOCK_SIZE - MIN_BLOCK_SIZE).toDouble() / (upSizeThreshold - MIN_BLOCK_SIZE)
                    blockSize = (size * k).toInt() + MIN_BLOCK_SIZE
                    blocks = ceil(size.toDouble() / blockSize).toInt()
                }
            }

            checkFile(file, size)
            val path = file.toPath()

            return FileSendInfo(path, size, blocks, blockSize)
        }

        fun getHash(bytes: ByteArray?): ByteArray = MessageDigest.getInstance(HASH_ALGORITHM).digest(bytes)

        fun checkFile(file: File, size: Int) {
            require(file.length() == size.toLong()) { "File \"${file.name}\" has now a different size, try again." }
            require(file.exists()) { "File \"${file.name}\" doesn't exists." }
        }
    }
}