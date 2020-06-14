package com.gmail.marcosav2010.connection

import com.gmail.marcosav2010.cipher.CipheredCommunicator
import com.gmail.marcosav2010.cipher.EncryptedMessage
import com.gmail.marcosav2010.cipher.SessionCipher
import com.gmail.marcosav2010.common.PublicIPResolver
import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.communicator.BaseCommunicator
import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.handling.PacketMessager
import com.gmail.marcosav2010.communicator.packet.packets.PacketShutdown
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketReader
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketReadException
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.connection.exception.ConnectionException
import com.gmail.marcosav2010.connection.exception.ConnectionIdentificationException
import com.gmail.marcosav2010.handshake.ConnectionToken
import com.gmail.marcosav2010.handshake.HandshakeAuthenticator
import com.gmail.marcosav2010.handshake.HandshakeCommunicator
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.module.ModuleManager
import com.gmail.marcosav2010.module.ModuleScope
import com.gmail.marcosav2010.peer.ConnectedPeer
import com.gmail.marcosav2010.peer.Peer
import com.gmail.marcosav2010.tasker.Task
import com.gmail.marcosav2010.tasker.Tasker
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

/**
 * Represents the connection between two @Peer
 *
 * @author Marcos
 */
class Connection : NetworkConnection, ModuleScope {

    companion object {
        private const val AUTH_TIMEOUT = 20L
        private const val HP_TIMEOUT = 3L
        private const val SOCKET_CONNECT_TIMEOUT = 7000
        private const val REMOTE_CONNECT_BACK_TIMEOUT = 10L
        private val DISCONNECT_REQUEST_BYTES = byteArrayOf(123, -123)
    }

    override val log: Log

    override val peer: Peer

    private val connected: AtomicBoolean = AtomicBoolean(false)

    override var connectedPeer: ConnectedPeer? = null
        private set

    private var hostSocket: Socket? = null
    private var listenTask: Task? = null
    private var remoteConnectBackTimeout: Task? = null

    var identificationController: IdentificationController
        private set

    var remotePort = -1
        private set

    override var remoteAddress: InetAddress? = null
    private var baseCommunicator: BaseCommunicator? = null
    private var sessionCipher: SessionCipher? = null
    private var cipheredCommunicator: CipheredCommunicator? = null
    private var reader: PacketReader? = null
    private var messager: PacketMessager? = null

    override var moduleManager by Delegates.notNull<ModuleManager>()
        private set

    constructor(peer: Peer) {
        this.peer = peer
        log = Log(peer, "")
        updateLogTag("Connecting ${peer.andInc}")
        identificationController = IdentificationController(this)
        init()
    }

    internal constructor(peer: Peer, uuid: UUID) : this(peer) {
        identificationController = IdentificationController(this, uuid)
    }

    override val uuid: UUID?
        get() = identificationController.uuid

    private fun init() {
        sessionCipher = SessionCipher.create(this)
        baseCommunicator = BaseCommunicator()
        moduleManager = ModuleManager(this)
        moduleManager.initializeModules()
    }

    private fun updateLogTag(tag: String) {
        log.prefix = tag
    }

    fun connect(remoteSocket: Socket, ct: ConnectionToken?) {
        connected.set(true)
        cancelConnectBackTimeout()
        remoteAddress = remoteSocket.inetAddress

        if (ct != null) {
            log.log("Setting handshake ciphered communicator input key...", VerboseLevel.HIGH)

            val hCommunicator = if (baseCommunicator is HandshakeCommunicator)
                baseCommunicator as HandshakeCommunicator
            else
                HandshakeCommunicator(baseCommunicator!!)

            hCommunicator.setIn(ct.baseKey, ct.handshakeKey)
            baseCommunicator = hCommunicator
        }

        log.log("Setting input stream...", VerboseLevel.MEDIUM)
        baseCommunicator!!.input = remoteSocket.getInputStream()

        try {
            listenForAuth()
            listenForHandshakeKeyPortAndConnect()
        } catch (e: Exception) {
            log.log(e)
            disconnect(true)
            return
        }

        log.log("Setup completed, now executing listen task...", VerboseLevel.LOW)
        listenTask = Tasker.run(peer, listenSocket()).setName("Listen Task")
    }

    private fun listenSocket() =
            task@{
                while (connected.get()) {
                    var read: EncryptedMessage?

                    try {
                        read = cipheredCommunicator!!.read()
                        cipheredCommunicator!!.decrypt(read) { bytes -> onRead(bytes) }

                    } catch (e: IOException) {
                        if (!connected.get())
                            return@task

                        log.log(e, "Connection lost unexpectedly: ${e.message}")
                        disconnect(true)

                        return@task
                    } catch (e: Exception) {
                        log.log(e, "An unknown exception has occurred: ${e.message}")
                    }
                }
            }

    fun connect(address: InetSocketAddress) {

        check(hostSocket == null || hostSocket!!.isBound) { "Already connected." }

        check(!((address.address == PublicIPResolver.publicAddress ||
                address.address.hostName == InetAddress.getLocalHost().hostName)
                && address.port == peer.port)) { "Cannon connect to itself." }

        log.log("Connecting to $address...")

        val connectingSocket = Socket()
        connectingSocket.connect(address, SOCKET_CONNECT_TIMEOUT)

        log.log("Setting host socket...", VerboseLevel.MEDIUM)

        hostSocket = connectingSocket
        remotePort = address.port

        log.log("Setting output stream...", VerboseLevel.MEDIUM)

        baseCommunicator!!.output = hostSocket!!.getOutputStream()
        val ha = peer.connectionManager.handshakeAuthenticator
        val ct = ha.sendHandshake(baseCommunicator!!, address)

        identificationController.sendTemporaryUUID()

        if (ct != null) {
            log.log("Setting handshake ciphered communicator output key...", VerboseLevel.HIGH)

            val hCommunicator = if (baseCommunicator is HandshakeCommunicator)
                baseCommunicator as HandshakeCommunicator
            else
                HandshakeCommunicator(baseCommunicator!!)

            hCommunicator.setOut(ct.baseKey, ct.handshakeKey)
            baseCommunicator = hCommunicator
        }

        log.log("Generating session input cipher using ${SessionCipher.RSA_KEY_ALGORITHM}-"
                + "${SessionCipher.RSA_KEY_SIZE}...", VerboseLevel.MEDIUM)

        try {
            sessionCipher!!.generate()
        } catch (e: Exception) {
            log.log(e, "Aborting, there was an error while generating session cipher: ${e.message}")
            disconnect(silent = true, force = true)
            return
        }

        log.log("Starting authentication...", VerboseLevel.MEDIUM)

        startAuthentication()

        if (!isConnected()) {
            log.log("Providing handshake key and port to connect...", VerboseLevel.MEDIUM)
            writeHandshakeKeyAndPort()
            startConnectTimeout()
        }
        log.log("Connected to remote, please wait...")
    }

    private fun listenForAuth() {
        if (!sessionCipher!!.isWaitingForRemoteAuth) return

        log.log("Waiting for remote authentication, timeout set to ${AUTH_TIMEOUT}s...", VerboseLevel.MEDIUM)

        try {
            val response = baseCommunicator!!.read(SessionCipher.RSA_KEY_MSG, peer, AUTH_TIMEOUT, TimeUnit.SECONDS)
                    ?: throw ConnectionException("Got empty message, seems like remote disconnected.")

            log.log("Loading authentication response...", VerboseLevel.LOW)

            sessionCipher!!.loadAuthenticationResponse(response)
        } catch (e: TimeoutException) {
            throw ConnectionException("Remote peer didn't send authentication at time, aborting.")
        } catch (e: Exception) {
            throw ConnectionException("There was an error while reading authentication.", e)
        }
    }

    private fun listenForHandshakeKeyPortAndConnect() {
        if (!shouldReadPort()) return

        log.log("Waiting for remote handshake key and port, timeout set to ${HP_TIMEOUT}s...",
                VerboseLevel.MEDIUM)

        val response = try {
            baseCommunicator!!.read(
                    Integer.BYTES + HandshakeAuthenticator.H_KEY_LENGTH + HandshakeAuthenticator.B_KEY_LENGTH,
                    peer,
                    HP_TIMEOUT,
                    TimeUnit.SECONDS) ?: throw ConnectionException("Got empty message, seems like remote disconnected.")

        } catch (e: TimeoutException) {
            throw ConnectionException("Remote peer didn't send handshake key and port at time, aborting.")
        } catch (e: Exception) {
            throw ConnectionException("There was an error while reading handshake key and port.", e)
        }

        handleHandshakeKeyPortReadAndConnect(response)
    }

    private fun writeHandshakeKeyAndPort() {
        val c = peer.connectionManager.handshakeAuthenticator.generateTemporalHandshakeKey()
        writeRawBytes(Utils.concat(Utils.intToBytes(peer.port), c.handshakeKey, c.baseKey))
    }

    private fun startConnectTimeout() {
        log.log("Waiting for remote connection back, timeout set to ${REMOTE_CONNECT_BACK_TIMEOUT}s...",
                VerboseLevel.MEDIUM)

        remoteConnectBackTimeout = Tasker.schedule(peer, {
            log.log("Remote peer didn't connect back at time, stopping connection.")

            try {
                writeRawBytes(DISCONNECT_REQUEST_BYTES) // Try to send disconnect request to avoid unnecessary a future
                // connection try
            } catch (ignored: IOException) {
            }

            disconnect(silent = true, force = true)
        }, REMOTE_CONNECT_BACK_TIMEOUT, TimeUnit.SECONDS)
    }

    private fun cancelConnectBackTimeout() {
        if (remoteConnectBackTimeout != null) {
            remoteConnectBackTimeout!!.cancel()
            try {
                writeRawBytes(ByteArray(DISCONNECT_REQUEST_BYTES.size)) // With this we avoid a communicator asynchronization
            } catch (ignored: IOException) {
            }
        }
    }

    private fun shouldReadPort() = remotePort == -1

    private fun handleHandshakeKeyPortReadAndConnect(bytes: ByteArray) {
        log.log("Reading remote port and handshake...", VerboseLevel.MEDIUM)

        val sBytes = Utils.split(bytes, Integer.BYTES)
        remotePort = Utils.bytesToInt(sBytes[0])
        val hbBytes = Utils.split(sBytes[1], HandshakeAuthenticator.H_KEY_LENGTH)
        val address = InetSocketAddress(remoteAddress, remotePort)
        peer.connectionManager.handshakeAuthenticator.storeHandshakeKey(address, hbBytes[0], hbBytes[1])

        log.log("Read remote port $remotePort and handshake key.", VerboseLevel.MEDIUM)

        // We wait 500ms for a disconnect request, usually send if connection back has
        // token too much time
        try {
            if (Arrays.equals(baseCommunicator!!.read(DISCONNECT_REQUEST_BYTES.size, peer, 500L, TimeUnit.MILLISECONDS),
                            DISCONNECT_REQUEST_BYTES)) throw ConnectionException("Got remote disconnect request, aborting...")
        } catch (ignored: InterruptedException) {
        } catch (ignored: ExecutionException) {
        } catch (ignored: TimeoutException) {
        }

        connect(address)
    }

    private fun onRead(bytes: ByteArray) {
        if (!isConnected()) return

        val p = try {
            reader!!.read(bytes)
        } catch (e: PacketReadException) {
            log.log(e, "There was an error while reading bytes.")
            return
        }

        messager!!.onReceive(p)
    }

    fun onAuth() {
        log.log("Authentication process done, ciphering I/O streams using ${CipheredCommunicator.AES_KEY_ALGORITHM}"
                + "-${CipheredCommunicator.AES_KEY_SIZE}...", VerboseLevel.MEDIUM)

        cipheredCommunicator = CipheredCommunicator(baseCommunicator!!, sessionCipher!!, peer)
        reader = PacketReader()
        messager = PacketMessager(this, cipheredCommunicator!!)
        identificationController.messager = messager

        log.log("Session ciphering done, communicator ciphered and packet messager set.", VerboseLevel.LOW)

        identificationController.startIdentification()
    }

    fun onPairCompleted() {
        messager!!.setupEventHandler()
        moduleManager.onEnable()
        log.log("Connection completed.")
    }

    private fun startAuthentication() = sessionCipher!!.sendAuthentication()

    fun sendPacket(packet: Packet,
                   action: (() -> Unit)? = null,
                   onTimeOut: (() -> Unit)? = null,
                   timeout: Long = -1L,
                   timeUnit: TimeUnit? = null): Int = messager!!.sendPacket(packet, action, onTimeOut, timeout, timeUnit)

    fun writeRawBytes(bytes: ByteArray) = baseCommunicator!!.write(bytes)

    fun isConnected() = connected.get()

    /**
     * When called, @ConnectedPeer instance is set, and paring is done.
     */
    fun setConnectedPeer(cPeer: ConnectedPeer) {
        if (connectedPeer != null)
            throw ConnectionIdentificationException("ConnectedPeer instance is already set.")

        connectedPeer = cPeer

        log.log("Setting connection tag to \"${connectedPeer!!.name}\"", VerboseLevel.MEDIUM)
        updateLogTag(connectedPeer!!.name)
    }

    fun disconnect(silent: Boolean, force: Boolean = false) {
        if (!force && !isConnected()) return

        moduleManager.let {
            log.log("Disabling connection modules...", VerboseLevel.MEDIUM)
            it.onDisable()
        }

        log.log("Disconnecting from peer...", VerboseLevel.LOW)
        connected.set(false)

        messager?.let {
            log.log("Unregistering listen events...", VerboseLevel.HIGH)
            it.stopEventHandler()
        }

        listenTask?.let {
            log.log("Stopping listening task...", VerboseLevel.MEDIUM)
            it.cancelNow()
        }

        if (!silent) {
            log.log("Sending disconnect message to remote peer...", VerboseLevel.HIGH)
            try {
                messager?.sendStandardPacket(PacketShutdown())
            } catch (e: PacketWriteException) {
                log.log("Could not send disconnect message: ${e.message}", VerboseLevel.HIGH)
            }
        }

        cipheredCommunicator?.let {
            log.log("Stopping communicator pool and closing I/O streams...", VerboseLevel.MEDIUM)
            it.closeQuietly()
        }

        baseCommunicator?.closeQuietly()

        hostSocket?.let {
            log.log("Closing host socket...", VerboseLevel.MEDIUM)
            try {
                it.close()
            } catch (ignored: IOException) {
            }
        }

        uuid?.let { peer.connectionManager.removeConnection(it) }
        log.log("Disconnected successfully.")
    }
}