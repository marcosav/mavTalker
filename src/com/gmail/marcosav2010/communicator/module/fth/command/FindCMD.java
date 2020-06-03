package com.gmail.marcosav2010.communicator.module.fth.command;

import java.util.stream.Collectors;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.communicator.module.fth.packet.PacketFindFile;
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

class FindCMD extends Command {

    FindCMD() {
        super("find", "[peer] <filename>");
    }

    @Override
    public void execute(String[] arg, int args) {
        int pCount = Main.getInstance().getPeerManager().count();

        if (args < 1 && pCount == 1 || args < 2 && pCount > 1 || pCount == 0) {
            log.log("ERROR: Needed filename at least.");
            return;
        }

        boolean autoPeer = pCount > 1;
        Peer peer;

        if (!autoPeer) {
            String peerName = arg[0];

            if (Main.getInstance().getPeerManager().exists(peerName)) {
                peer = Main.getInstance().getPeerManager().get(peerName);
            } else {
                log.log("ERROR: Peer \"" + peerName + "\" doesn't exists.");
                return;
            }
        } else
            peer = Main.getInstance().getPeerManager().getFirstPeer();

        String filename = arg[autoPeer ? 0 : 1];

        log.log("Finding file \"" + filename + "\"...");

        var connectedPeers = peer.getConnectionManager().getIdentificator().getConnectedPeers();
        var p = new PacketFindFile(filename, 1,
                connectedPeers.stream().map(ConnectedPeer::getUUID).collect(Collectors.toSet()));

        connectedPeers.forEach(c -> {
            try {
                c.sendPacket(p);
            } catch (PacketWriteException e) {
                log.log(e, "There was a problem while sending find packet to " + c.getName() + ".");
            }
        });
    }
}