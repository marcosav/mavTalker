package com.gmail.marcosav2010.peer

import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.logger.Loggable
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * Represents the base class of a known peer, whose name and host port are
 * known, it can be a @Peer or a @ConnectedPeer.
 *
 * @author Marcos
 */
abstract class KnownPeer(
        val name: String,
        val port: Int,
        final override val uuid: UUID,
        private var address: InetAddress?)
    : NetworkPeer, Loggable {

    val displayID: String = Utils.toBase64(uuid)

    constructor(name: String, port: Int, uuid: UUID) : this(name, port, uuid, null) {
        try {
            address = InetAddress.getLocalHost()
        } catch (e: UnknownHostException) {
            log.log(e)
        }
    }
}