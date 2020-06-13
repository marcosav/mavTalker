package com.gmail.marcosav2010.tm

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import kotlin.properties.Delegates

class PacketMessage() : Packet() {

    var message by Delegates.notNull<String>()
        private set

    constructor(message: String) : this() {
        this.message = message
    }

    override fun encodeContent(out: PacketEncoder) {
        out.write(message)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        message = `in`.readString()
    }
}