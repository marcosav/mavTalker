package com.gmail.marcosav2010.communicator.packet

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketDecoder
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketEncoder

abstract class AbstractPacket {

    companion object {
        var BASE_SIZE = Short.MAX_VALUE * 32 * 4 // ~4 MB

        @JvmField
        var MAX_SIZE = BASE_SIZE - java.lang.Byte.BYTES
    }

    abstract fun encode(out: PacketEncoder)

    abstract fun decode(`in`: PacketDecoder)

    abstract val isStandard: Boolean
}