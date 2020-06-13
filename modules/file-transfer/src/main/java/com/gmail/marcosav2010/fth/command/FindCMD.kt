package com.gmail.marcosav2010.fth.command

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException
import com.gmail.marcosav2010.fth.packet.PacketFindFile
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.Peer

internal class FindCMD : Command("find", "[peer] <filename>") {

    override fun execute(arg: Array<String>, length: Int) {
        val peerManager = Main.instance.peerManager!!

        val pCount = peerManager.count()
        if (length < 1 && pCount == 1 || length < 2 && pCount > 1 || pCount == 0) {
            log.log("ERROR: Needed filename at least.")
            return
        }

        val autoPeer = pCount > 1
        val peer: Peer = if (!autoPeer) {
            val peerName = arg[0]
            if (peerManager.exists(peerName)) {
                peerManager[peerName]!!
            } else {
                log.log("ERROR: Peer \"$peerName\" doesn't exists.")
                return
            }
        } else peerManager.firstPeer

        val filename = arg[if (autoPeer) 0 else 1]
        log.log("Finding file \"$filename\"...")

        val connectedPeers = peer.connectionManager.identificator.connectedPeers
        val p = PacketFindFile(filename, 1, connectedPeers.map { obj -> obj.uuid }.toMutableSet())

        connectedPeers.forEach { c ->
            try {
                c.sendPacket(p)
            } catch (e: PacketWriteException) {
                log.log(e, "There was a problem while sending find packet to " + c.name + ".")
            }
        }
    }
}