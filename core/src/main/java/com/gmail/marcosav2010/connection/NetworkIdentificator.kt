package com.gmail.marcosav2010.connection

import com.gmail.marcosav2010.peer.NetworkPeer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This class manages all remote peer connections to connected peers
 *
 * @author Marcos
 */
open class NetworkIdentificator<T : NetworkPeer> {

    private val connectionPeer: MutableMap<UUID, T> = ConcurrentHashMap()
    private val connectionPeerUUID: MutableMap<UUID, UUID> = ConcurrentHashMap()

    fun getPeer(uuid: UUID): T? = connectionPeer[uuid]

    fun getConnection(peer: NetworkPeer) =
            connectionPeer.entries.filter { e -> e.value == peer }.map { it.key }.firstOrNull()

    fun hasPeer(uuid: UUID) = connectionPeer.containsKey(uuid)

    fun hasPeer(peer: NetworkPeer) = connectionPeerUUID.containsKey(peer.uuid)

    protected fun put(connectionUUID: UUID, peer: T) {
        connectionPeerUUID[connectionUUID] = peer.uuid
        connectionPeer[connectionUUID] = peer
    }

    protected fun remove(connectionUUID: UUID): T? {
        connectionPeerUUID.remove(connectionUUID)
        return connectionPeer.remove(connectionUUID)
    }

    val connectedPeers: Set<T>
        get() = connectionPeer.values.toSet()

    fun size() = connectionPeer.size
}