package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.handshake.HandshakeAuthenticator.HandshakeRequirementLevel
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.Peer

internal class GenerateAddressCMD : Command("generate", arrayOf("g", "gen"), "[peer] [-p (public)]") {

    override fun execute(arg: Array<String>, length: Int) {
        val peerManager = Main.instance.peerManager!!

        val pCount = peerManager.count()
        if (pCount > 1 && length == 0) {
            log.log("ERROR: Specify peer.")
            return
        }

        var generatePublic = length == 1 && arg[0].equals("-p", ignoreCase = true)
        val autoPeer = pCount == 1 && (length == 0 || generatePublic)

        generatePublic = generatePublic or (length == 2 && arg[1].equals("-p", ignoreCase = true))

        val peer: Peer = if (!autoPeer) {
            val peerName = arg[0]
            if (peerManager.exists(peerName)) {
                peerManager[peerName]!!
            } else {
                log.log("ERROR: Peer \"$peerName\" doesn't exists.")
                return
            }
        } else peerManager.firstPeer

        val addressKey: String

        addressKey = if (generatePublic) {
            if (peer.properties.hrl >= HandshakeRequirementLevel.PRIVATE) {
                log.log("Peer \"${peer.name}\" does only allow private keys, you can change this in peer configuration.")
                return
            } else try {
                peer.connectionManager.handshakeAuthenticator.generatePublicAddressKey()
            } catch (e: Exception) {
                log.log("There was an error generating the public address key, ${e.message}.")
                return
            }
        } else {
            log.log("Enter requester Connection Key: ")
            val requesterConnectionKey = System.console().readPassword()
            if (requesterConnectionKey == null || requesterConnectionKey.isEmpty()) {
                log.log("Please enter a Connection Key.")
                return
            }

            try {
                peer.connectionManager.handshakeAuthenticator.generatePrivateAddressKey(requesterConnectionKey)
            } catch (e: IllegalArgumentException) {
                log.log(e.message)
                return
            } catch (e: Exception) {
                log.log("There was an error reading the provided address key, ${e.message}.")
                return
            }
        }

        log.log("--------------------------------------------------------")
        log.log("\tAddress Key => $addressKey")
        log.log("--------------------------------------------------------")
    }
}