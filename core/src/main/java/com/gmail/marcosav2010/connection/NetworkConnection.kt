package com.gmail.marcosav2010.connection

import com.gmail.marcosav2010.peer.NetworkPeer
import java.net.InetAddress
import java.util.*

/**
 * Represents a connection between two @NetworkPeer, resembles a graph arist
 *
 * @author Marcos
 */
open class NetworkConnection {

    open val connectedPeer: NetworkPeer?
        get() = null

    open val peer: NetworkPeer?
        get() = null

    open val uuid: UUID?
        get() = null

    open val remoteAddress: InetAddress?
        get() = null
}