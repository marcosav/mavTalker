package com.gmail.marcosav2010.fth.command

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.fth.command.FTCommandRegistry.Companion.getFTH
import com.gmail.marcosav2010.main.Main.Companion.instance
import com.gmail.marcosav2010.peer.Peer

internal class DownloadCMD : Command(
        "download",
        arrayOf("d", "dw"),
        "<host peer> <remote peer> <file id> <yes/no> (default = yes)"
) {

    override fun execute(arg: Array<String>, length: Int) {
        if (length < 3) {
            log.log("ERROR: Needed host and remote peer, file id and yes/no option (yes by default).")
            return
        }

        val peerName = arg[0]

        val peer: Peer = if (instance.peerManager!!.exists(peerName)) {
            instance.peerManager!![peerName]!!
        } else {
            log.log("ERROR: Peer \"$peerName\" doesn't exists.")
            return
        }

        val remoteName = arg[1]
        val cManager = peer.connectionManager
        val cIdentificator = cManager.identificator
        val connection: Connection

        if (!cIdentificator.hasPeer(remoteName)) {
            log.log("ERROR: $peerName peer is not connected to that $remoteName.")
            return
        }

        connection = cIdentificator[remoteName]!!.connection
        val fth = getFTH(connection)
        val id = try {
            arg[2].toInt()
        } catch (ex: NumberFormatException) {
            log.log("ERROR: Invalid file id.")
            return
        }

        if (!fth.isPendingRequest(id)) {
            log.log("ERROR: File ID #$id hasn't got requests.")
            return
        }

        val info = fth.getRequest(id)!!
        var yes = length < 4

        if (!yes) {
            val o = arg[3].toLowerCase()
            yes = o == "yes" || o == "y"
        }
        if (yes) {
            log.log("Accepted file #$id (${info.fileName}) transfer request.")
            fth.acceptRequest(id)
        } else {
            fth.rejectRequest(id)
            log.log("Rejected file #$id (${info.fileName}) transfer request.")
        }
    }
}