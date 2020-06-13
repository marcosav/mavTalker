package com.gmail.marcosav2010.connection

import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify
import com.gmail.marcosav2010.peer.ConnectedPeer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This class manages all peer connections when connected peer registers
 *
 * @author Marcos
 */
open class ConnectionIdentificator : NetworkIdentificator<ConnectedPeer>() {

    private val namePeer: MutableMap<String, ConnectedPeer> = ConcurrentHashMap()

    fun identifyConnection(connection: Connection, info: PacketIdentify): ConnectedPeer {
        val name = getSuitableName(info.name!!)
        val c = ConnectedPeer(name, info.peerUUID, connection)
        namePeer[name] = c
        put(connection.uuid!!, c)
        return c
    }

    private fun getSuitableName(providedName: String): String {
        var output = providedName
        while (namePeer.containsKey(providedName)) output += "_"
        return providedName
    }

    operator fun get(name: String) = namePeer[name]

    fun hasPeer(name: String) = namePeer.containsKey(name)

    fun removePeer(uuid: UUID): ConnectedPeer? {
        val c = remove(uuid) ?: return null
        return namePeer.remove(c.name)
    }

    protected fun removePeer(name: String): ConnectedPeer? {
        val c = namePeer.remove(name) ?: return null
        return remove(c.connection.uuid!!)
    }
}