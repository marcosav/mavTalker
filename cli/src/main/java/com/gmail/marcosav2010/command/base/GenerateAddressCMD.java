package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.handshake.HandshakeAuthentificator.HandshakeRequirementLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

class GenerateAddressCMD extends Command {

    GenerateAddressCMD() {
        super("generate", new String[]{"g", "gen"}, "[peer] [-p (public)]");
    }

    @Override
    public void execute(String[] arg, int args) {
        int pCount = Main.getInstance().getPeerManager().count();

        if (pCount > 1 && args == 0) {
            log.log("ERROR: Specify peer.");
            return;
        }

        boolean generatePublic = args == 1 && arg[0].equalsIgnoreCase("-p");
        boolean autoPeer = pCount == 1 && (args == 0 || generatePublic);
        generatePublic |= args == 2 && arg[1].equalsIgnoreCase("-p");

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

        String addressKey;

        if (generatePublic) {
            if (peer.getProperties().getHRL().compareTo(HandshakeRequirementLevel.PRIVATE) >= 0) {
                log.log("Peer \"" + peer.getName()
                        + "\" does only allow private keys, you can change this in peer configuration.");
                return;

            } else
                try {
                    addressKey = peer.getConnectionManager().getHandshakeAuthentificator()
                            .generatePublicAddressKey();

                } catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException
                        | NoSuchPaddingException | IllegalBlockSizeException e) {
                    log.log("There was an error generating the public address key, " + e.getMessage() + ".");
                    return;
                }

        } else {

            log.log("Enter requester Connection Key: ");

            var requesterConnectionKey = System.console().readPassword();
            if (requesterConnectionKey == null || requesterConnectionKey.length == 0) {
                log.log("Please enter a Connection Key.");
                return;
            }

            try {
                addressKey = peer.getConnectionManager().getHandshakeAuthentificator()
                        .generatePrivateAddressKey(requesterConnectionKey);

            } catch (IllegalArgumentException e) {
                log.log(e.getMessage());
                return;
            } catch (BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                    | IllegalBlockSizeException e) {
                log.log("There was an error reading the provided address key, " + e.getMessage() + ".");
                return;
            }
        }

        log.log("--------------------------------------------------------");
        log.log("\tAddress Key => " + addressKey);
        log.log("--------------------------------------------------------");
    }
}