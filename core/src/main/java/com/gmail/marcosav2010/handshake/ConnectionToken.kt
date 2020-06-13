package com.gmail.marcosav2010.handshake

import com.gmail.marcosav2010.common.Utils.encode
import java.net.InetSocketAddress

class ConnectionToken(val handshakeKey: ByteArray, val baseKey: ByteArray) {

    val handshakeKeyAsString = encode(handshakeKey)

    lateinit var address: InetSocketAddress
        private set

    var isPublic = false
        private set

    constructor(handshakeKey: ByteArray, address: InetSocketAddress, baseKey: ByteArray) : this(handshakeKey, baseKey) {
        this.address = address
    }

    fun setPublic(b: Boolean): ConnectionToken {
        isPublic = b
        return this
    }
}