package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

class DisconnectCMD extends Command {

    DisconnectCMD() {
        super("disconnect", new String[]{"dis"}, "<peer> <remote peer>/<address:port> (no address = localhost)");
    }

    @Override
    public void execute(String[] arg, int args) {
        if (args < 2) {
            log.log("ERROR: Specify local and remote peer or address.");
            return;
        }

        Peer peer;
        String peerName = arg[0];

        if (Main.getInstance().getPeerManager().exists(peerName)) {
            peer = Main.getInstance().getPeerManager().get(peerName);
        } else {
            log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
            return;
        }

        Connection connection;

        String remoteName = arg[1];

        ConnectionManager cManager = peer.getConnectionManager();
        ConnectionIdentificator cIdentificator = cManager.getIdentificator();

        if (cIdentificator.hasPeer(remoteName))
            connection = cIdentificator.getPeer(remoteName).getConnection();
        else {
            String[] rawAddress = remoteName.split(":");
            boolean local = rawAddress.length == 1;

            int port;
            try {
                port = Integer.parseInt(rawAddress[local ? 0 : 1]);
            } catch (NumberFormatException ex) {
                log.log("ERROR: Invalid address format or peer.");
                return;
            }

            try {
                InetSocketAddress address = local ? new InetSocketAddress(InetAddress.getLocalHost(), port)
                        : new InetSocketAddress(rawAddress[0], port);

                if (cManager.isConnectedTo(address))
                    connection = cManager.getConnection(address);
                else {
                    log.log("ERROR: " + peerName + " peer is not connected to that address.");
                    return;
                }

            } catch (UnknownHostException ex) {
                log.log("ERROR: Invalid address.");
                return;
            }
        }

        connection.disconnect(false);
    }
}