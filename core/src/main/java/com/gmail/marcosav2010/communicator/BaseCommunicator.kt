package com.gmail.marcosav2010.communicator

import com.gmail.marcosav2010.tasker.TaskOwner
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * This communicator sends and receives raw bytes, with a fixed buffer length.
 *
 * @author Marcos
 */
open class BaseCommunicator : Communicator() {

    @Synchronized
    open fun read(bytes: Int): ByteArray? = input!!.readNBytes(bytes)

    open fun read(bytes: Int, taskOwner: TaskOwner, timeout: Long, unit: TimeUnit): ByteArray? =
            taskOwner.executorService.submit<ByteArray?> { read(bytes) }[timeout, unit]

    override fun write(bytes: ByteArray) = output!!.write(bytes)

    override fun close() {
        input?.close()
        output?.close()
    }

    override fun closeQuietly() {
        try {
            close()
        } catch (ignored: IOException) {
        }
    }
}