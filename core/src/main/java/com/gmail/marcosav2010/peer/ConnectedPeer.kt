package com.gmail.marcosav2010.peer

import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.connection.NetworkIdentificator
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Represents a connected @NetworkPeer which is at the other side of
 * the @Connection (with a @Peer), acting like a node.
 *
 * @author Marcos
 */
class ConnectedPeer(name: String, uuid: UUID, val connection: Connection) :
        KnownPeer(name, connection.remotePort, uuid, connection.remoteAddress) {

    override val log: ILog = Log(Logger.global, name)

    override val networkIdentificator = NetworkIdentificator<ConnectedPeer>()

    fun disconnect(silent: Boolean) = connection.disconnect(silent)

    fun sendPacket(packet: Packet,
                   action: (() -> Unit)? = null,
                   onTimeOut: (() -> Unit)? = null,
                   timeout: Long = 1L,
                   timeUnit: TimeUnit? = null) {
        connection.sendPacket(packet, action, onTimeOut, timeout, timeUnit)
    }
}