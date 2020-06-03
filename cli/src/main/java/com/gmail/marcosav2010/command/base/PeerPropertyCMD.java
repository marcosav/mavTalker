package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

class PeerPropertyCMD extends Command {

    PeerPropertyCMD() {
        super("peerproperty", new String[]{"pp", "pprop"}, "[peer (if one leave)] <property name> <value>");
    }

    @Override
    public void execute(String[] arg, int args) {
        int pCount = Main.getInstance().getPeerManager().count();

        if (pCount > 1 && args < 1 || pCount == 0) {
            log.log("ERROR: Specify peer, property and value.");
            return;
        }

        boolean autoPeer = pCount == 1 && (args == 2 || args == 0);

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

        var props = peer.getProperties();

        if (pCount > 1 && args < 3 || pCount <= 1 && args < 2) {
            log.log("Showing peer " + peer.getName() + " properties:");
            log.log(props.toString());
            return;
        }

        String prop = arg[autoPeer ? 0 : 1], value = arg[autoPeer ? 1 : 2];

        if (props.exist(prop)) {
            if (props.set(prop, value)) {
                log.log("Property \"" + prop + "\" set to: " + value);
                return;

            } else
                log.log("There was an error while setting the property \"" + prop + "\" in " + peer.getName()
                        + ":");

        } else
            log.log("Unrecognized property \"" + prop + "\", current properties in " + peer.getName() + ":");

        log.log(props.toString());
    }
}