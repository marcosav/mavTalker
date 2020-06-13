package com.gmail.marcosav2010.fth.packet

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import com.gmail.marcosav2010.fth.FileTransferHandler
import kotlin.properties.Delegates

class PacketFileSend() : Packet() {

    companion object {
        /**
         * Data fields + byte arrays + array length
         */
        @JvmField
        val MAX_BLOCK_SIZE = MAX_SIZE - 2 * Integer.BYTES - FileTransferHandler.HASH_SIZE - 2 * Integer.BYTES
    }

    var fileID by Delegates.notNull<Int>()
        private set

    var pointer by Delegates.notNull<Int>()
        private set

    var bytes by Delegates.notNull<ByteArray>()
        private set

    var hash by Delegates.notNull<ByteArray>()
        private set

    constructor(fileID: Int, pointer: Int, bytes: ByteArray, hash: ByteArray) : this() {
        require(bytes.size <= MAX_BLOCK_SIZE) { "Byte block size cannot exceed $MAX_BLOCK_SIZE bytes" }
        require(hash.size <= FileTransferHandler.HASH_SIZE) { "Hash size cannot exceed ${FileTransferHandler.HASH_SIZE} bytes" }
        this.fileID = fileID
        this.pointer = pointer
        this.bytes = bytes
        this.hash = hash
    }

    override fun shouldSendResponse() = false

    override fun encodeContent(out: PacketEncoder) {
        out.write(fileID)
        out.write(pointer)
        out.write(bytes)
        out.write(hash)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        fileID = `in`.readInt()
        pointer = `in`.readInt()
        bytes = `in`.readBytes()
        hash = `in`.readBytes()
    }
}