package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.Peer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException

internal class DisconnectCMD : Command(
        "disconnect",
        arrayOf("dis"),
        "<peer> <remote peer>/<address:port> (no address = localhost)"
) {

    override fun execute(arg: Array<String>, length: Int) {
        if (length < 2) {
            log.log("ERROR: Specify local and remote peer or address.")
            return
        }

        val peerManager = Main.instance.peerManager!!
        val peerName = arg[0]

        val peer: Peer = if (peerManager.exists(peerName)) {
            peerManager[peerName]!!
        } else {
            log.log("ERROR: Peer \"$peerName\" doesn't exists.")
            return
        }

        val connection: Connection
        val remoteName = arg[1]
        val cManager = peer.connectionManager
        val cIdentificator = cManager.identificator

        if (cIdentificator.hasPeer(remoteName))
            connection = cIdentificator[remoteName]!!.connection
        else {
            val rawAddress = remoteName.split(":").toTypedArray()
            val local = rawAddress.size == 1
            val port: Int
            port = try {
                rawAddress[if (local) 0 else 1].toInt()
            } catch (ex: NumberFormatException) {
                log.log("ERROR: Invalid address format or peer.")
                return
            }

            try {
                val address = if (local)
                    InetSocketAddress(InetAddress.getLocalHost(), port)
                else
                    InetSocketAddress(rawAddress[0], port)

                if (cManager.isConnectedTo(address))
                    connection = cManager.getConnection(address)!!
                else {
                    log.log("ERROR: $peerName peer is not connected to that address.")
                    return
                }
            } catch (ex: UnknownHostException) {
                log.log("ERROR: Invalid address.")
                return
            }
        }

        connection.disconnect(false)
    }
}