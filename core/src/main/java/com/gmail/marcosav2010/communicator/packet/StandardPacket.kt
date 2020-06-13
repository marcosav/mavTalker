package com.gmail.marcosav2010.communicator.packet

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder

abstract class StandardPacket : AbstractPacket() {

    override val isStandard
        get() = true

    override fun encode(out: PacketEncoder) = encodeContent(out)

    protected abstract fun encodeContent(out: PacketEncoder)

    override fun decode(`in`: PacketDecoder) = decodeContent(`in`)

    protected abstract fun decodeContent(`in`: PacketDecoder)
}