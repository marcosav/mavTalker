package com.gmail.marcosav2010.command.base;

import java.util.concurrent.TimeUnit;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.communicator.packet.packets.PacketPing;
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

class PingCMD extends Command {

    PingCMD() {
        super("ping");
    }

    @Override
    public void execute(String[] arg, int args) {
        if (args < 2) {
            log.log("ERROR: Needed transmitter and target.");
            return;
        }

        String peerName = arg[0], remoteName = arg[1];
        Peer peer;

        if (Main.getInstance().getPeerManager().exists(peerName)) {
            peer = Main.getInstance().getPeerManager().get(peerName);
        } else {
            log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
            return;
        }

        ConnectionManager cManager = peer.getConnectionManager();
        ConnectionIdentificator cIdentificator = cManager.getIdentificator();

        Connection connection;

        if (!cIdentificator.hasPeer(remoteName)) {
            log.log("ERROR: " + peerName + " peer is not connected to that " + remoteName + ".");
            return;
        }

        connection = cIdentificator.getPeer(remoteName).getConnection();

        long l = System.currentTimeMillis();

        try {
            connection.sendPacket(new PacketPing(), () -> log.log((System.currentTimeMillis() - l) / 2 + "ms"),
                    () -> log.log("Ping timed out."), 10L, TimeUnit.SECONDS);
        } catch (PacketWriteException e) {
            log.log(e);
        }
    }
}