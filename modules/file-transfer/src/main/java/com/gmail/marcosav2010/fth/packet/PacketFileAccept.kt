package com.gmail.marcosav2010.fth.packet

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import kotlin.properties.Delegates.notNull

class PacketFileAccept() : Packet() {

    var fileID by notNull<Int>()
        private set

    constructor(fileID: Int) : this() {
        this.fileID = fileID
    }

    override fun encodeContent(out: PacketEncoder) {
        out.write(fileID)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        fileID = `in`.readInt()
    }
}