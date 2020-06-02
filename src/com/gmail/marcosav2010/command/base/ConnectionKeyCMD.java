package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

class ConnectionKeyCMD extends Command {

	ConnectionKeyCMD() {
		super("connectionkey", new String[] { "ck", "ckey" }, "[peer]");
	}

	@Override
	public void execute(String[] arg, int args) {
		int pCount = Main.getInstance().getPeerManager().count();

		if (pCount > 1 && args == 0) {
			log.log("ERROR: Specify peer.");
			return;
		}

		boolean autoPeer = pCount == 1 && args == 0;

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

		String k;

		k = peer.getConnectionManager().getHandshakeAuthentificator().getConnectionKeyString();

		log.log("-----------------------------------------------------");
		log.log("\tConnection Key => " + k);
		log.log("-----------------------------------------------------");
	}
}