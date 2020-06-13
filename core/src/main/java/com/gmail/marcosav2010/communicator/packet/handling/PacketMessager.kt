package com.gmail.marcosav2010.communicator.packet.handling

import com.gmail.marcosav2010.cipher.CipheredCommunicator
import com.gmail.marcosav2010.communicator.packet.AbstractPacket
import com.gmail.marcosav2010.communicator.packet.Packet
import com.gmail.marcosav2010.communicator.packet.StandardPacket
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketEventHandlerManager
import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify
import com.gmail.marcosav2010.communicator.packet.packets.PacketResponse
import com.gmail.marcosav2010.communicator.packet.packets.PacketShutdown
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriter
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class handles a @Connection @IPacket traffic.
 *
 * @author Marcos
 */
class PacketMessager(private val connection: Connection, private val communicator: CipheredCommunicator) {

    private val log: ILog = Log(connection, "PM")
    private val eventHandlerManager = PacketEventHandlerManager(connection)
    private val writer = PacketWriter()
    private val actionHandler = PacketActionHandler(connection)
    private val lastPacket = AtomicInteger()

    fun onReceive(p: AbstractPacket) {
        if (p.isStandard)
            handleStandardPacket(p)
        else
            handlePacket(p as Packet)
    }

    private fun handleStandardPacket(sp: AbstractPacket) {
        when (sp) {
            is PacketResponse -> {
                val id = sp.responsePacketId
                actionHandler.handleResponse(id)
                log.log("Successfully sent packet #$id.", VerboseLevel.HIGH)
            }
            is PacketIdentify -> connection.identificationController.identifyConnection(sp)
            is PacketShutdown -> connection.disconnect(true)
        }
    }

    private fun handlePacket(packet: Packet) {
        val id = packet.packetID

        log.log("Received packet #$id.", VerboseLevel.HIGH)
        eventHandlerManager.handlePacket(packet)

        if (packet.shouldSendResponse()) try {
            sendStandardPacket(PacketResponse(id.toLong()))
        } catch (ex: PacketWriteException) {
            log.log(ex, "There was an exception sending receive response in packet #$id.")
        }
    }

    fun sendPacket(packet: Packet,
                   action: (() -> Unit)?,
                   onTimeOut: (() -> Unit)?,
                   timeout: Long?,
                   timeUnit: TimeUnit?): Int {

        val id = lastPacket.incrementAndGet()
        log.log("Sending packet #$id.", VerboseLevel.HIGH)

        communicator.write(writer.write(packet.setPacketID(id)))

        action?.let {
            checkNotNull(timeout)
            checkNotNull(timeUnit)
            actionHandler.handleSend(connection.peer, id.toLong(), packet, action, onTimeOut, timeout, timeUnit)
        }

        return lastPacket.get()
    }

    fun sendStandardPacket(packet: StandardPacket) = communicator.write(writer.write(packet))

    fun setupEventHandler() {
        log.log("Registering packet handlers...", VerboseLevel.MEDIUM)

        eventHandlerManager.registerListeners(connection.moduleManager.listeners)
        eventHandlerManager.registerListeners(connection.peer.moduleManager.listeners)

        log.log("Registered " + eventHandlerManager.handlerCount + " handlers in "
                + eventHandlerManager.listenerCount + " listeners.", VerboseLevel.LOW)
    }

    fun stopEventHandler() = eventHandlerManager.unregisterEvents()
}