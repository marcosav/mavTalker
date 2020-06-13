package com.gmail.marcosav2010.communicator.packet.wrapper.exception

import java.io.IOException

class PacketWriteException : IOException {

    constructor(exception: Exception?) : super(exception)
    constructor(msg: String?) : super(msg)

    companion object {
        private const val serialVersionUID = 3252190216554456161L
    }
}