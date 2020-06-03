package com.gmail.marcosav2010.module.tm;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.command.base.BaseCommandUtils;
import com.gmail.marcosav2010.communicator.packet.wrapper.exception.PacketWriteException;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.KnownPeer;

import java.util.Set;
import java.util.stream.Collectors;

public class TMCommandRegistry extends CommandRegistry {

    public TMCommandRegistry() {
        super(Set.of(new MessageCMD()));
    }

    private static class MessageCMD extends Command {

        MessageCMD() {
            super("message", new String[]{"msg", "m"}, "<from> <to (P1,P2...) (B = all)> <msg>");
        }

        @Override
        public void execute(String[] arg, int args) {
            if (args < 3) {
                log.log("ERROR: Needed transmitter, targets, and a message.");
                return;
            }

            Set<ConnectedPeer> to = BaseCommandUtils.getTargets(log, arg[0], arg[1]);
            if (to.isEmpty())
                return;

            StringBuilder toWrite = new StringBuilder();
            for (int i = 2; i < args; i++) {
                toWrite.append(arg[i]).append(" ");
            }

            String finalMsg = toWrite.toString().trim();

            log.log("INFO: Sending to \"" + to.stream().map(KnownPeer::getName).collect(Collectors.joining(","))
                    + "\" message \"" + finalMsg + "\".", VerboseLevel.MEDIUM);

            to.forEach(c -> {
                try {
                    c.sendPacket(new PacketMessage(finalMsg));
                } catch (PacketWriteException e) {
                    log.log(e);
                }
            });
        }
    }
}