package com.gmail.marcosav2010.peer

import com.gmail.marcosav2010.connection.NetworkIdentificator
import java.util.*

/**
 * Represents the base of any kind of remote peer, which can be connected or not to a @Peer,
 * resembles a node.
 *
 * @author Marcos
 */
interface NetworkPeer {

    val networkIdentificator: NetworkIdentificator<out NetworkPeer>

    val uuid: UUID
}