package com.gmail.marcosav2010.communicator.packet.packets

import com.gmail.marcosav2010.communicator.packet.StandardPacket
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder
import kotlin.properties.Delegates

class PacketResponse() : StandardPacket() {

    var responsePacketId by Delegates.notNull<Long>()
        private set

    constructor(responsePacketId: Long) : this() {
        this.responsePacketId = responsePacketId
    }

    override fun encodeContent(out: PacketEncoder) {
        out.write(responsePacketId)
    }

    override fun decodeContent(`in`: PacketDecoder) {
        responsePacketId = `in`.readLong()
    }
}