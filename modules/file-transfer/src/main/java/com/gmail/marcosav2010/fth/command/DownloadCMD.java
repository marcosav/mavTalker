package com.gmail.marcosav2010.fth.command;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.fth.FileReceiveInfo;
import com.gmail.marcosav2010.fth.FileTransferHandler;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

class DownloadCMD extends Command {

    DownloadCMD() {
        super("download", new String[]{"d", "dw"}, "<host peer> <remote peer> <file id> <yes/no> (default = yes)");
    }

    @Override
    public void execute(String[] arg, int args) {
        if (args < 3) {
            log.log("ERROR: Needed host and remote peer, file id and yes/no option (yes by default).");
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

        String remoteName = arg[1];
        ConnectionManager cManager = peer.getConnectionManager();
        ConnectionIdentificator cIdentificator = cManager.getIdentificator();

        Connection connection;

        if (!cIdentificator.hasPeer(remoteName)) {
            log.log("ERROR: " + peerName + " peer is not connected to that " + remoteName + ".");
            return;
        }
        connection = cIdentificator.getPeer(remoteName).getConnection();

        FileTransferHandler fth = FTCommandRegistry.getFTH(connection);

        int id;
        try {
            id = Integer.parseInt(arg[2]);
        } catch (NumberFormatException ex) {
            log.log("ERROR: Invalid file id.");
            return;
        }

        if (!fth.isPendingRequest(id)) {
            log.log("ERROR: File ID #" + id + " hasn't got requests.");
            return;
        }
        FileReceiveInfo info = fth.getRequest(id);

        boolean yes = args < 4;

        if (!yes) {
            String o = arg[3].toLowerCase();
            yes = o.equals("yes") || o.equals("y");
        }

        if (yes) {
            log.log("Accepted file #" + id + " (" + info.getFileName() + ") transfer request.");
            fth.acceptRequest(id);
        } else {
            fth.rejectRequest(id);
            log.log("Rejected file #" + id + " (" + info.getFileName() + ") transfer request.");
        }
    }
}