package com.gmail.marcosav2010.communicator.packet.wrapper.exception

import com.gmail.marcosav2010.communicator.packet.AbstractPacket

class OverExceededByteLimitException(incoming: Long) :
        RuntimeException("Exceeded max byte array length: ${AbstractPacket.MAX_SIZE} < $incoming.") {

    companion object {
        private const val serialVersionUID = -3543010566122316736L
    }
}