package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.peer.Peer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

internal class NewCMD : Command(
        "new",
        arrayOf("create"),
        "<peer [name] [port]/connection [peer] [address:port] (no address = localhost) [-k]>"
) {

    override fun execute(arg: Array<String>, length: Int) {
        if (length < 1) {
            log.log("ERROR: Specify entity type.")
            return
        }

        val peerManager = Main.instance.peerManager!!

        when (arg[0].toLowerCase()) {
            "peer" -> {
                val peer = if (length >= 3) {
                    val port = try {
                        arg[2].toInt()
                    } catch (ex: NumberFormatException) {
                        log.log("ERROR: Invalid port.")
                        return
                    }
                    peerManager.create(arg[1], port)
                } else if (length >= 2) {
                    peerManager.create(arg[1])
                } else {
                    peerManager.create()
                }

                peer.start()
            }

            "c", "conn", "connection" -> {
                val autoPeer = peerManager.count() == 1 && length == 2

                if (autoPeer || length >= 3) {
                    val peer: Peer = if (!autoPeer) {
                        val peerName = arg[1]
                        if (peerManager.exists(peerName)) {
                            peerManager[peerName]!!
                        } else {
                            log.log("ERROR: Peer \"$peerName\" doesn't exists.")
                            return
                        }
                    } else
                        peerManager.firstPeer

                    val addressKeyConnection = arg[if (autoPeer) 1 else 2].equals("-k", ignoreCase = true)

                    if (addressKeyConnection) {
                        log.log("Enter remote Address Key: ")

                        val read = System.console().readPassword()
                        if (read == null || read.isEmpty()) {
                            log.log("Please enter an Address Key.")
                            return
                        }

                        try {
                            val address = peer.connectionManager.handshakeAuthenticator.parseAddressKey(read).address
                            try {
                                peer.connect(address)
                            } catch (e: Exception) {
                                log.log(e, "There was an error creating peer")
                            }
                        } catch (e: IllegalArgumentException) {
                            log.log(e.message)

                        } catch (e: UnknownHostException) {
                            log.log("This address key references an unknown host.")

                        } catch (e: Exception) {
                            log.log(when (e) {
                                is BadPaddingException,
                                is InvalidKeyException,
                                is NoSuchAlgorithmException,
                                is NoSuchPaddingException,
                                is IllegalBlockSizeException ->
                                    "There was an error reading the provided address key: ${e.message}."
                                else -> "There was an unknown error reading the provided address key."
                            })
                        }

                    } else {
                        val rawAddress = arg[if (autoPeer) 1 else 2].split(":").toTypedArray()
                        val local = rawAddress.size == 1
                        val port = try {
                            rawAddress[if (local) 0 else 1].toInt()
                        } catch (ex: NumberFormatException) {
                            log.log("ERROR: Invalid address format.")
                            return
                        }

                        try {
                            val address = if (local)
                                InetSocketAddress(InetAddress.getLocalHost(), port)
                            else
                                InetSocketAddress(rawAddress[0], port)

                            if (peer.connectionManager.isConnectedTo(address)) {
                                log.log("ERROR: \"${peer.name}\" peer is already connected to this address.")
                                return
                            }

                            if (local)
                                log.log("INFO: No hostname provided, connecting to localhost.")

                            peer.connect(address)
                        } catch (e: UnknownHostException) {
                            log.log("ERROR: Invalid address.")
                        } catch (e: IOException) {
                            log.log(e)
                        } catch (e: GeneralSecurityException) {
                            log.log(e)
                        }
                    }
                }
            }
        }
    }
}