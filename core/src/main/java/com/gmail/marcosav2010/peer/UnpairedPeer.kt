package com.gmail.marcosav2010.peer

import com.gmail.marcosav2010.connection.NetworkIdentificator
import java.util.*

/**
 * Represents a @NetworkPeer which is not connected to any @Peer, being able to be connected to
 * other @NetworkPeer, it represents the opposite of a @ConnectedPeer, acting like a node.
 *
 * @author Marcos
 */
class UnpairedPeer(override val uuid: UUID) : NetworkPeer {

    override val networkIdentificator = NetworkIdentificator<NetworkPeer>()
}