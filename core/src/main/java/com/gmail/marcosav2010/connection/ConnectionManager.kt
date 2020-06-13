package com.gmail.marcosav2010.connection

import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.handshake.HandshakeAuthenticator
import com.gmail.marcosav2010.handshake.InvalidHandshakeKey
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Loggable
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.peer.Peer
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * This class manages all @Peer established @Connection
 *
 * @author Marcos
 */
class ConnectionManager(private val peer: Peer) : Loggable {

    companion object {
        private const val UUID_BYTES = java.lang.Long.BYTES * 2
        private const val UUID_TIMEOUT = 10L
    }

    override val log: ILog = Log(peer, "ConnectionManager")

    private val connections: MutableMap<UUID, Connection> = ConcurrentHashMap()
    val handshakeAuthenticator = HandshakeAuthenticator(peer)
    val identificator = ConnectionIdentificator()

    fun getConnection(uuid: UUID) = connections[uuid]

    fun removeConnection(uuid: UUID): Connection? {
        val c = connections.remove(uuid)
        identificator.removePeer(uuid)
        return c
    }

    val connectionUUIDs: Map<UUID, Connection>
        get() = Collections.unmodifiableMap(connections)

    fun getConnections(): Collection<Connection> = Collections.unmodifiableCollection(connections.values)

    fun isConnectedTo(address: InetSocketAddress): Boolean {
        return connections.values.any { c ->
            c.remotePort == address.port && c.remoteAddress!!.hostAddress == address.address.hostAddress
        }
    }

    fun getConnection(address: InetSocketAddress): Connection? {
        return connections.values.firstOrNull { c ->
            c.remotePort == address.port && c.remoteAddress!!.hostAddress == address.address.hostAddress
        }
    }

    fun registerConnection(c: Connection): Connection {
        val u: UUID = c.uuid!!
        return if (!connections.containsKey(u)) {
            connections[u] = c
            c
        } else connections[u]!!
    }

    private fun registerConnection(peer: Peer, uuid: UUID): Connection {
        return if (!connections.containsKey(uuid)) {
            val c = Connection(peer, uuid)
            connections[uuid] = c
            c
        } else connections[uuid]!!
    }

    fun manageSocketConnection(remoteSocket: Socket) {
        log.log("Accepted " + remoteSocket.remoteSocketAddress.toString())

        val ct = try {
            handshakeAuthenticator.readHandshake(remoteSocket)
        } catch (e: InterruptedException) {
            log.log("Remote peer didn't send handshake at time, closing remote socket...")
            remoteSocket.close()
            return
        } catch (e: ExecutionException) {
            log.log("Remote peer didn't send handshake at time, closing remote socket...")
            remoteSocket.close()
            return
        } catch (e: TimeoutException) {
            log.log("Remote peer didn't send handshake at time, closing remote socket...")
            remoteSocket.close()
            return
        } catch (e: InvalidHandshakeKey) {
            log.log(e.message + ", closing remote socket.")
            remoteSocket.close()
            return
        }

        log.log("Reading temporary remote connection UUID, timeout set to " + UUID_TIMEOUT + "s...",
                VerboseLevel.MEDIUM)

        val uuid = try {
            readUUID(remoteSocket)
        } catch (e: InterruptedException) {
            log.log("Remote peer didn't send UUID at time, closing remote socket...")
            remoteSocket.close()
            return
        } catch (e: ExecutionException) {
            log.log("Remote peer didn't send UUID at time, closing remote socket...")
            remoteSocket.close()
            return
        } catch (e: TimeoutException) {
            log.log("Remote peer didn't send UUID at time, closing remote socket...")
            remoteSocket.close()
            return
        }

        log.log("Finding and registering remote connection from temporary UUID...", VerboseLevel.MEDIUM)
        registerConnection(peer, uuid).connect(remoteSocket, ct)
    }

    private fun readUUID(remoteSocket: Socket): UUID {
        val b = ByteArray(UUID_BYTES)
        peer.executorService.submit<Int> { remoteSocket.getInputStream().read(b, 0, UUID_BYTES) }[UUID_TIMEOUT, TimeUnit.SECONDS]
        return Utils.getUUIDFromBytes(b)
    }

    fun disconnectAll(silent: Boolean) {
        if (connections.isEmpty()) return

        log.log("Closing and removing client connections...", VerboseLevel.LOW)

        val iterator: MutableIterator<Map.Entry<UUID, Connection>> = connections.entries.iterator()

        while (iterator.hasNext()) {
            val c = iterator.next().value
            iterator.remove()
            c.disconnect(silent)
        }

        log.log("Connections closed and removed successfully.", VerboseLevel.LOW)
    }
}