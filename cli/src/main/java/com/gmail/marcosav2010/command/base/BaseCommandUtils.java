package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;

import java.util.HashSet;
import java.util.Set;

public class BaseCommandUtils {

    public static Set<ConnectedPeer> getTargets(ILog log, String fromName, String targets) {
        Set<ConnectedPeer> to = new HashSet<>();
        Peer from;

        if (Main.getInstance().getPeerManager().exists(fromName)) {
            from = Main.getInstance().getPeerManager().get(fromName);
        } else {
            log.log("ERROR: Peer \"" + fromName + "\" doesn't exists.");
            return to;
        }

        if (targets.equalsIgnoreCase("b")) {
            to = from.getConnectionManager().getIdentificator().getConnectedPeers();

        } else {
            String[] toNames = targets.split(",");

            for (String toName : toNames)
                if (from.getConnectionManager().getIdentificator().hasPeer(toName)) {
                    to.add(from.getConnectionManager().getIdentificator().getPeer(toName));
                } else {
                    log.log("ERROR: \"" + from.getName() + "\" isn't connected to \"" + toName + "\".");
                }
        }

        return to;
    }
}