package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.communicator.packet.packets.PacketPing
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.Peer
import java.util.concurrent.TimeUnit

internal class PingCMD : Command("ping") {

    override fun execute(arg: Array<String>, length: Int) {
        if (length < 2) {
            log.log("ERROR: Needed transmitter and target.")
            return
        }

        val peerName = arg[0]
        val remoteName = arg[1]
        val peer: Peer = if (Main.instance.peerManager!!.exists(peerName)) {
            Main.instance.peerManager!![peerName]!!
        } else {
            log.log("ERROR: Peer \"$peerName\" doesn't exists.")
            return
        }

        val cManager = peer.connectionManager
        val cIdentificator = cManager.identificator
        val connection: Connection

        if (!cIdentificator.hasPeer(remoteName)) {
            log.log("ERROR: $peerName peer is not connected to that $remoteName.")
            return
        }

        connection = cIdentificator[remoteName]!!.connection
        val l = System.currentTimeMillis()

        try {
            connection.sendPacket(
                    PacketPing(),
                    { log.log("${((System.currentTimeMillis() - l) / 2)} ms") },
                    { log.log("Ping timed out.") },
                    10L,
                    TimeUnit.SECONDS
            )
        } catch (e: PacketWriteException) {
            log.log(e)
        }
    }
}