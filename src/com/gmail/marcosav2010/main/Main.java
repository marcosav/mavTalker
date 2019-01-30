package com.gmail.marcosav2010.main;

import com.gmail.marcosav2010.command.CommandManager;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.peer.Peer;
import com.gmail.marcosav2010.peer.PeerManager;
import com.gmail.marcosav2010.tasker.Tasker;

public class Main {

	private static boolean STARTER_PEER = true;

	private static Main mainInstance;

	private final CommandManager commandManager;
	private PeerManager peerManager;
	private Tasker tasker;

	protected Main() {
		commandManager = new CommandManager();
		peerManager = new PeerManager();
		tasker = new Tasker();
	}

	public void main(String[] args) {
		try {
			if (args.length == 2)
				run(args[0], Integer.parseInt(args[1]));
		} catch (Exception ex) {
			handleException(ex, "");
		}
	}

	private void run(String name, int port) {
		if (STARTER_PEER) {
			Peer startPeer = peerManager.create(name, port);
			startPeer.start();
		}
	}

	public CommandManager getCommandManager() {
		return commandManager;
	}

	public PeerManager getPeerManager() {
		return peerManager;
	}

	public Tasker getTasker() {
		return tasker;
	}

	public static void setInstance(Main instance) {
		mainInstance = instance;
	}

	public static Main getInstance() {
		return mainInstance;
	}

	public static void handleException(Throwable ex, String name) {
		Logger.log(ex, name + ": There was a problem while running: " + ex.getMessage());
	}
}