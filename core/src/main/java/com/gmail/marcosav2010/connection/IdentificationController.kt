package com.gmail.marcosav2010.connection

import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.communicator.packet.handling.PacketMessager
import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.peer.ConnectedPeer
import com.gmail.marcosav2010.tasker.Task
import com.gmail.marcosav2010.tasker.Tasker
import java.util.*
import java.util.concurrent.TimeUnit

class IdentificationController(private val connection: Connection) {

    companion object {
        private const val IDENTIFICATION_TIMEOUT = 10L
    }

    private val log: ILog = Log(connection, "IC")

    private val cManager = connection.peer.connectionManager
    internal var messager: PacketMessager? = null
    private var uuidProvider = false

    var uuid: UUID? = null
        private set

    private var idTask: Task? = null

    constructor(connection: Connection, uuid: UUID) : this(connection) {
        this.uuid = uuid
    }

    fun sendTemporaryUUID() {
        if (hasUUID()) {
            log.log("Providing temporary connection UUID...", VerboseLevel.MEDIUM)
            connection.writeRawBytes(Utils.getBytesFromUUID(uuid!!))
        } else {
            log.log("Generating and providing temporary connection UUID...", VerboseLevel.MEDIUM)
            uuidProvider = true
            val uuid = UUID.randomUUID()
            setUUID(uuid)
            connection.writeRawBytes(Utils.getBytesFromUUID(uuid))
        }
    }

    private fun setUUID(uuid: UUID): Boolean {
        if (!cManager.identificator.hasPeer(uuid)) { // We don't trust UUID randomness
            this.uuid = uuid
            return true
        }
        return false
    }

    fun startIdentification() {
        if (uuidProvider) {
            log.log("Generating new connection UUID and sending identification packet...", VerboseLevel.MEDIUM)
            cManager.removeConnection(uuid!!)

            var newUUID: UUID
            do {
                newUUID = UUID.randomUUID()
            } while (!setUUID(newUUID))

            cManager.registerConnection(connection)
            messager!!.sendStandardPacket(PacketIdentify(connection.peer.name, newUUID,
                    connection.peer.uuid, PacketIdentify.SUCCESS))
        } else {
            log.log("Waiting for UUID renewal and identification, timeout set to ${IDENTIFICATION_TIMEOUT}s...",
                    VerboseLevel.MEDIUM)
            startIdentificationCountdown()
        }
    }

    private fun startIdentificationCountdown() {
        idTask = Tasker.schedule(connection.peer, {
            log.log("Identification failure, remote peer didn't send identification at time.")

            try {
                sendIdentifyResponse(PacketIdentify.TIMED_OUT)
            } catch (ignored: PacketWriteException) {
            }

            connection.disconnect(true)
        }, IDENTIFICATION_TIMEOUT, TimeUnit.SECONDS)
    }

    fun identifyConnection(info: PacketIdentify): ConnectedPeer? {
        if (info.providesUUID()) {
            idTask!!.cancel()

            log.log("Received identification from peer \"" + info.name + "\".", VerboseLevel.HIGH)
            log.log("Updating new remote connection UUID and setting info...", VerboseLevel.MEDIUM)

            cManager.removeConnection(uuid!!)
            val newUUID = info.newUUID!!

            if (!setUUID(newUUID)) {
                log.log("Identification failure, UUID could not be renewed because it was already identified, " +
                        "sending response and disconnecting...", VerboseLevel.LOW)
                try {
                    sendIdentifyResponse(PacketIdentify.INVALID_UUID)
                } catch (e: PacketWriteException) {
                    log.log("There was an exception sending identification response, disconnecting anyway.")
                }
                connection.disconnect(true)
                return null
            }

            cManager.registerConnection(connection)
            log.log("Identification successfully, sending response...", VerboseLevel.MEDIUM)

            try {
                sendIdentifyResponse(PacketIdentify.SUCCESS)
            } catch (e: PacketWriteException) {
                log.log(e, "There was an exception sending identification response.")
            }

        } else {
            log.log("Received identification response from peer \"" + info.name + "\".", VerboseLevel.HIGH)

            when (info.result) {
                PacketIdentify.SUCCESS -> log.log("Setting remote peer info...", VerboseLevel.MEDIUM)
                PacketIdentify.INVALID_UUID -> {
                    log.log("There was an error identifying peer, invalid UUID (try again), aborting.",
                            VerboseLevel.MEDIUM)
                    connection.disconnect(true)
                    return null
                }
                PacketIdentify.TIMED_OUT -> {
                    log.log("Identification timed out, aborting.", VerboseLevel.MEDIUM)
                    connection.disconnect(true)
                    return null
                }
            }
        }
        val cp = cManager.identificator.identifyConnection(connection, info)
        connection.setConnectedPeer(cp)
        log.log("Identification completed, peers paired successfully.", VerboseLevel.MEDIUM)
        connection.onPairCompleted()
        return cp
    }

    private fun sendIdentifyResponse(result: Byte) {
        messager!!.sendStandardPacket(
                PacketIdentify(connection.peer.name, null, connection.peer.uuid, result))
    }

    private fun hasUUID() = uuid != null
}