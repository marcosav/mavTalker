package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.ConnectedPeer
import com.gmail.marcosav2010.peer.Peer
import java.util.*

object BaseCommandUtils {

    fun getTargets(log: ILog, fromName: String, targets: String): Set<ConnectedPeer> {
        var to: MutableSet<ConnectedPeer> = HashSet()

        val from: Peer = if (Main.instance.peerManager!!.exists(fromName)) {
            Main.instance.peerManager!![fromName]!!
        } else {
            log.log("ERROR: Peer \"$fromName\" doesn't exists.")
            return to
        }

        if (targets.equals("b", ignoreCase = true)) {
            to = from.connectionManager.identificator.connectedPeers.toMutableSet()

        } else {
            val toNames = targets.split(",")

            for (toName in toNames)
                if (from.connectionManager.identificator.hasPeer(toName)) {
                    to.add(from.connectionManager.identificator[toName]!!)
                } else {
                    log.log("ERROR: \"${from.name}\" isn't connected to \"$toName\".")
                }
        }
        return to
    }
}