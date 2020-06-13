package com.gmail.marcosav2010.fth.packet

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import kotlin.properties.Delegates

class PacketGotFile() : Packet() {

    var fileName by Delegates.notNull<String>()
        private set

    var owner by Delegates.notNull<String>()
        private set

    constructor(fileName: String, owner: String) : this() {
        this.fileName = fileName
        this.owner = owner
    }

    override fun encodeContent(out: PacketEncoder) {
        out.write(fileName)
        out.write(owner)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        fileName = `in`.readString()
        owner = `in`.readString()
    }
}