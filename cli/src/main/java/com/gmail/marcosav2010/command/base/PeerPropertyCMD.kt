package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.Peer

internal class PeerPropertyCMD : Command(
        "peerproperty",
        arrayOf("pp", "pprop"),
        "[peer (if one leave)] <property name> <value>"
) {

    override fun execute(arg: Array<String>, length: Int) {
        val peerManager = Main.instance.peerManager!!

        val pCount = peerManager.count()
        if (pCount > 1 && length < 1 || pCount == 0) {
            log.log("ERROR: Specify peer, property and value.")
            return
        }

        val autoPeer = pCount == 1 && (length == 2 || length == 0)
        val peer: Peer = if (!autoPeer) {
            val peerName = arg[0]
            if (peerManager.exists(peerName)) {
                peerManager[peerName]!!
            } else {
                log.log("ERROR: Peer \"$peerName\" doesn't exists.")
                return
            }
        } else peerManager.firstPeer

        val props = peer.properties
        if (pCount > 1 && length < 3 || pCount <= 1 && length < 2) {
            log.log("Showing peer ${peer.name} properties:")
            log.log(props.toString())
            return
        }

        val prop = arg[if (autoPeer) 0 else 1]
        val value = arg[if (autoPeer) 1 else 2]

        if (props.exist(prop)) {
            try {
                props[prop] = value
                log.log("Property \"$prop\" set to: $value")

            } catch (e: Exception) {
                log.log("There was an error while setting the property \"$prop\" in ${peer.name}:")
            }
        } else log.log("Unrecognized property \"$prop\", current properties in ${peer.name}:")
    }
}