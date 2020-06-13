package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.Peer

internal class ConnectionKeyCMD : Command("connectionkey", arrayOf("ck", "ckey"), "[peer]") {

    override fun execute(arg: Array<String>, length: Int) {
        val peerManager = Main.instance.peerManager!!

        val pCount = peerManager.count()
        if (pCount > 1 && length == 0) {
            log.log("ERROR: Specify peer.")
            return
        }

        val autoPeer = pCount == 1 && length == 0
        val peer: Peer = if (!autoPeer) {
            val peerName = arg[0]
            if (peerManager.exists(peerName)) {
                peerManager[peerName]!!
            } else {
                log.log("ERROR: Peer \"$peerName\" doesn't exists.")
                return
            }
        } else peerManager.firstPeer

        val k: String
        k = peer.connectionManager.handshakeAuthenticator.connectionKeyString

        log.log("-----------------------------------------------------")
        log.log("\tConnection Key => $k")
        log.log("-----------------------------------------------------")
    }
}