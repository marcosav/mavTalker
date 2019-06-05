package com.gmail.marcosav2010.main;

import java.io.IOException;
import java.net.InetAddress;
import com.gmail.marcosav2010.command.CommandManager;
import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.module.ModuleManager;
import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.Peer;
import com.gmail.marcosav2010.peer.PeerManager;
import com.gmail.marcosav2010.tasker.Tasker;

public class Main {

	private static boolean STARTER_PEER = true;

	private static Main mainInstance;

	private final CommandManager commandManager;
	private GeneralConfiguration generalConfig;
	private PeerManager peerManager;
	private Tasker tasker;
	
	private InetAddress publicAddress;

	protected Main() {
		generalConfig = new GeneralConfiguration();
		
		Logger.setVerboseLevel(generalConfig.getVerboseLevel());
		
		ModuleManager.loadModules();
		
		commandManager = new CommandManager();
		peerManager = new PeerManager(generalConfig);
		tasker = new Tasker();
	}

	public void main(String[] args) {
		obtainPublicAddress();
		
		Logger.log("\nStarting application...");

		if (args.length == 2)
			run(args[0], Integer.parseInt(args[1]));
	}

	private void run(String name, int port) {
		if (STARTER_PEER) {
			Peer startPeer = peerManager.create(name, port);
			startPeer.start();
		}
	}
	
	private void obtainPublicAddress() {
		Logger.log("Obtaining public address...", VerboseLevel.MEDIUM);
		try {
			long m = System.currentTimeMillis();
			publicAddress = Utils.obtainExternalAddress();
			Logger.log("Public address got in " + (System.currentTimeMillis() - m) + "ms: " + publicAddress.getHostName(), VerboseLevel.MEDIUM);
		} catch (IOException e) {
			Logger.log("There was an error while obtaining public address, shutting down...");
			Logger.log(e);
		}
	}

	public CommandManager getCommandManager() {
		return commandManager;
	}

	public PeerManager getPeerManager() {
		return peerManager;
	}
	
	public GeneralConfiguration getGeneralConfig() {
		return generalConfig;
	}

	public Tasker getTasker() {
		return tasker;
	}
	
	public InetAddress getPublicAddress() {
		return publicAddress;
	}

	public static void setInstance(Main instance) {
		mainInstance = instance;
	}

	public void shutdown() {
		Logger.log("Exiting application...");
		if (getPeerManager() != null)
			getPeerManager().shutdown();

		if (generalConfig != null)
			try {
				generalConfig.store();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		Logger.log("Bye");
	}

	public static Main getInstance() {
		return mainInstance;
	}
}