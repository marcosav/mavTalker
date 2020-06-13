package com.gmail.marcosav2010.fth.packet

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import com.gmail.marcosav2010.fth.FileSendInfo
import kotlin.properties.Delegates

class PacketFileRequest() : Packet() {

    var name by Delegates.notNull<String>()
        private set

    var size by Delegates.notNull<Int>()
        private set

    var blocks by Delegates.notNull<Int>()
        private set

    var fileID by Delegates.notNull<Int>()
        private set

    val isSingle: Boolean
        get() = blocks == 1

    constructor(info: FileSendInfo) : this() {
        name = info.path.fileName.toString()
        size = info.size
        blocks = info.blocks
        fileID = info.fileID
    }

    override fun encodeContent(out: PacketEncoder) {
        out.write(name)
        out.write(size)
        out.write(blocks)
        out.write(fileID)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        name = `in`.readString()
        size = `in`.readInt()
        blocks = `in`.readInt()
        fileID = `in`.readInt()
    }
}