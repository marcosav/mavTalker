package com.gmail.marcosav2010.module.fth.command;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.command.base.BaseCommandUtils;
import com.gmail.marcosav2010.module.fth.FileSendInfo;
import com.gmail.marcosav2010.module.fth.FileTransferHandler;
import com.gmail.marcosav2010.peer.ConnectedPeer;

class FileCMD extends Command {

    FileCMD() {
        super("file", new String[] { "f" }, "<from> <to (P1,P2...) (B = all)> <filename>");
    }

    @Override
    public void execute(String[] arg, int args) {
        if (args < 3) {
            log.log("ERROR: Needed transmitter, targets, and a file name.");
            return;
        }

        Set<ConnectedPeer> to = BaseCommandUtils.getTargets(log, arg[0], arg[1]);
        if (to.isEmpty())
            return;

        String filename = arg[2];

        File fIn = new File(filename);

        FileSendInfo info;
        try {
            info = FileTransferHandler.createRequest(fIn);
        } catch (IllegalArgumentException ex) {
            log.log("ERROR: " + ex.getMessage());
            return;
        }

        to.forEach(c -> FTCommandRegistry.getFTH(c.getConnection()).sendRequest(info));

        log.log("INFO: File \"" + info.getFileName() + "\" transfer request has been sent to "
                + to.stream().map(t -> t.getName()).collect(Collectors.joining(",")) + ".");
    }
}