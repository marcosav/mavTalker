package com.gmail.marcosav2010.peer

import com.gmail.marcosav2010.config.IConfiguration
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.connection.ConnectionIdentificator
import com.gmail.marcosav2010.connection.ConnectionManager
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.logger.Logger.global
import com.gmail.marcosav2010.module.ModuleManager
import com.gmail.marcosav2010.module.ModuleScope
import com.gmail.marcosav2010.tasker.TaskOwner
import com.gmail.marcosav2010.tasker.Tasker
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents a local hosted peer.
 *
 * @author Marcos
 */
class Peer(name: String,
           port: Int,
           log: ILog,
           override val executorService: ExecutorService,
           configuration: IConfiguration) :
        KnownPeer(name, port, UUID.randomUUID()), TaskOwner, ModuleScope {

    override val log: ILog = Log(log, name)

    val connectionManager = ConnectionManager(this)
    val properties = PeerProperties(configuration)

    override val moduleManager = ModuleManager(this)

    private var externalExecutor = true
    private var server: ServerSocket? = null

    var isStarted = false
        private set

    private var connectionCount: AtomicInteger = AtomicInteger()

    constructor(name: String, port: Int, configuration: IConfiguration) :
            this(name, port, global, DefaultPeerExecutor(name), configuration) {

        this.externalExecutor = false
    }

    init {
        moduleManager.initializeModules()
    }

    fun start() {
        try {
            log.log("Loading modules...", VerboseLevel.LOW)
            moduleManager.onEnable()
            log.log("Starting server on port $port...")

            server = ServerSocket(port)
            isStarted = true

            Tasker.run(this, findClient()).setName("$name Find Client")

            log.log("Server created and waiting for someone to connect...")
        } catch (ex: Exception) {
            log.log(ex, "There was an exception while starting peer $name.")
            stop(true)
        }
    }

    private fun findClient(): () -> Unit {
        log.log("Starting connection finding thread...", VerboseLevel.HIGH)

        return {
            while (isStarted) {
                try {
                    log.log("Waiting for connection...", VerboseLevel.HIGH)
                    val remoteSocket = server!!.accept()
                    log.log("Someone connected, accepting...", VerboseLevel.MEDIUM)
                    connectionManager.manageSocketConnection(remoteSocket)

                } catch (ignored: SocketException) {
                } catch (e: Exception) {
                    log.log(e, "There was an exception in client find task in peer $name.")
                    stop(true)
                }
            }
        }
    }

    fun connect(peerAddress: InetSocketAddress): Connection {
        val connection = Connection(this)
        connection.connect(peerAddress)
        return connectionManager.registerConnection(connection)
    }

    override val networkIdentificator: ConnectionIdentificator
        get() = connectionManager.identificator

    fun printInfo() {
        val ci = connectionManager.identificator
        val peers = ci.connectedPeers
        log.log("""Name: $name
Display ID: $displayID
Currently ${peers.size} peers connected: """ + peers.joinToString(", ") { cp ->
            """
- ${cp.name} #${cp.displayID} CUUID: ${cp.connection.uuid}"""
        })
    }

    val andInc: Int
        get() = connectionCount.getAndIncrement()

    fun stop(silent: Boolean) {
        log.log("Shutting down peer...", VerboseLevel.MEDIUM)

        isStarted = false
        moduleManager.onDisable()
        connectionManager.disconnectAll(silent)

        if (server != null) try {
            log.log("Closing server...", VerboseLevel.MEDIUM)
            server!!.close()
        } catch (ignored: IOException) {
        }

        if (!externalExecutor) {
            log.log("Shutting down thread pool executor...", VerboseLevel.MEDIUM)
            executorService.shutdown()
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                log.log("There was an error while terminating the pool, forcing shutdown...", VerboseLevel.MEDIUM)
                executorService.shutdownNow()
            }
        }

        log.log("Shutdown done successfully.", VerboseLevel.LOW)
    }
}