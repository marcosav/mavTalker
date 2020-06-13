package com.gmail.marcosav2010.fth.packet

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import com.gmail.marcosav2010.fth.FileTransferHandler.FileSendResult
import kotlin.properties.Delegates

class PacketFileSendFailed() : Packet() {

    var fileID by Delegates.notNull<Int>()
        private set

    var cause by Delegates.notNull<FileSendResult>()
        private set

    constructor(fileID: Int, cause: FileSendResult) : this() {
        this.fileID = fileID
        this.cause = cause
    }

    override fun encodeContent(out: PacketEncoder) {
        out.write(fileID)
        out.write(cause.ordinal.toByte())
    }

    override fun decodeContent(`in`: PacketDecoder) {
        fileID = `in`.readInt()
        cause = FileSendResult.values()[`in`.readByte().toInt()]
    }
}