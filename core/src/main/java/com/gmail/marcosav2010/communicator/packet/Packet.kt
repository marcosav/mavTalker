package com.gmail.marcosav2010.communicator.packet

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder

abstract class Packet : AbstractPacket() {

    companion object {
        var MAX_SIZE = AbstractPacket.MAX_SIZE - Integer.BYTES
    }

    var packetID = 0
        private set

    fun setPacketID(packetID: Int): Packet {
        if (hasPacketID()) throw RuntimeException("This packet already has an ID set")
        this.packetID = packetID
        return this
    }

    private fun hasPacketID() = packetID != 0

    open fun shouldSendResponse() = true

    override val isStandard
        get() = false

    override fun encode(out: PacketEncoder) {
        if (!hasPacketID()) throw RuntimeException("Cannot encode without ID set")
        out.write(packetID)
        encodeContent(out)
    }

    protected abstract fun encodeContent(out: PacketEncoder)

    override fun decode(`in`: PacketDecoder) {
        packetID = `in`.readInt()
        decodeContent(`in`)
    }

    protected abstract fun decodeContent(`in`: PacketDecoder)
}