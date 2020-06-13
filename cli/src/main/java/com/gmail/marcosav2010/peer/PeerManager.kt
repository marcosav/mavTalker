package com.gmail.marcosav2010.peer

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Loggable
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.logger.Logger.global
import com.gmail.marcosav2010.main.Main.Companion.instance
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * This class manages all local hosted @Peer.
 *
 * @author Marcos
 */
class PeerManager(private val executorService: ExecutorService =
                          DefaultPeerExecutor("peerManager", ThreadGroup("parentPeerThreadGroup"))) :
        Loggable {

    companion object {
        const val MAX_NAME_LENGTH = 16
        private const val DEFAULT_PORT_BASE = 55551
    }

    override val log: ILog = Log(global, "PeerMan")

    private val peers: MutableMap<String, Peer> = ConcurrentHashMap()
    private var peersCreated = 0

    operator fun get(name: String) = peers[name]

    fun exists(name: String) = peers.containsKey(name)

    private val suitablePort: Int
        get() = DEFAULT_PORT_BASE + peersCreated

    private fun suggestName() = "P${(peersCreated + 1)}"

    private fun remove(peer: String) = peers.remove(peer)

    fun create(name: String = suggestName(), port: Int = suitablePort): Peer {
        require(!name.contains(" ")) { "\"$name\" contains spaces, which are not allowed." }
        require(name.length <= MAX_NAME_LENGTH) { "\"$name\" exceeds the $MAX_NAME_LENGTH char limit." }
        require(!exists(name)) {
            "This name \"$name\", is being used by an existing Peer, try again with a different one."
        }

        val peer = Peer(name, port, log, executorService, instance.generalConfig!!)
        peers[name] = peer
        peersCreated++

        return peer
    }

    fun shutdown() {
        if (peers.isEmpty()) return

        log.log("Shutting down all peers...")

        val iterator = peers.entries.iterator()

        while (iterator.hasNext()) {
            val p = iterator.next().value
            iterator.remove()
            p.stop(false)
        }

        log.log("All peers have been shutdown.")
        log.log("Shutting down thread pool executor...", VerboseLevel.MEDIUM)

        executorService.shutdown()

        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            log.log("There was an error while terminating the pool, forcing shutdown...", VerboseLevel.MEDIUM)
            executorService.shutdownNow()
        }
    }

    fun shutdown(peer: String) {
        if (exists(peer)) {
            val p = remove(peer)
            p!!.stop(false)
        }
    }

    fun count() = peers.size

    val firstPeer: Peer
        get() = peers.values.iterator().next()

    fun printInfo() {
        log.log("Peers Running -> " + peers.values.joinToString(", ") { p ->
            (p.name + " ("
                    + p.connectionManager.identificator.connectedPeers
                    .joinToString(" ") { c -> c.name + " " + c.displayID }
                    + ")")
        })
    }
}