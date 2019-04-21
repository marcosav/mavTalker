package com.gmail.marcosav2010.communicator.module.tm;

import java.util.Set;
import java.util.stream.Collectors;

import com.gmail.marcosav2010.command.BaseCommandRegistry;
import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.ConnectedPeer;

public class TMCommandRegistry extends CommandRegistry {
	
	public TMCommandRegistry() {
		super(Set.of(new MessageCMD()));
	}

	private static class MessageCMD extends Command {

		MessageCMD() {
			super("message", new String[] { "msg", "m" }, "<from> <to (P1,P2...) (B = all)> <msg>");
		}

		@Override
		public void execute(String[] arg, int args) {
			if (args < 3) {
				Logger.log("ERROR: Needed transmitter, targets, and a message.");
				return;
			}

			Set<ConnectedPeer> to = BaseCommandRegistry.getTargets(arg[0], arg[1]);
			if (to.isEmpty())
				return;

			String toWrite = "";
			for (int i = 2; i < args; i++) {
				toWrite += arg[i] + " ";
			}

			String finalMsg = toWrite.trim();

			Logger.log("INFO: Sending to \"" + to.stream().map(t -> t.getName()).collect(Collectors.joining(",")) + "\" message \"" + finalMsg + "\".",
					VerboseLevel.MEDIUM);

			to.forEach(c -> {
				try {
					c.sendPacket(new PacketMessage(finalMsg));
				} catch (PacketWriteException e) {
					Logger.log(e);
				}
			});
		}
	}
}