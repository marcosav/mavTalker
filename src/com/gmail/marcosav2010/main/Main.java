package com.gmail.marcosav2010.main;

import java.io.IOException;
import java.net.InetAddress;

import com.gmail.marcosav2010.command.CommandManager;
import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.module.ModuleLoader;
import com.gmail.marcosav2010.communicator.module.ModuleManager;
import com.gmail.marcosav2010.communicator.module.ModuleScope;
import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.logger.BaseLog;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.Peer;
import com.gmail.marcosav2010.peer.PeerManager;
import com.gmail.marcosav2010.tasker.Tasker;

import lombok.Getter;
import lombok.Setter;

public class Main implements ModuleScope {

	@Getter
	@Setter
	private static Main instance;

	@Getter
	private CommandManager commandManager;
	@Getter
	private GeneralConfiguration generalConfig;
	@Getter
	private PeerManager peerManager;
	@Getter
	private Tasker tasker;
	@Getter
	private ModuleManager moduleManager;
	@Getter
	private ILog log;

	@Getter
	private InetAddress publicAddress;
	@Getter
	private boolean shuttingDown;

	void init() {
		log = new BaseLog();
		generalConfig = new GeneralConfiguration();

		Logger.setVerboseLevel(generalConfig.getVerboseLevel());

		ModuleLoader.getInstance().loadModules();

		commandManager = new CommandManager();
		peerManager = new PeerManager(generalConfig);
		tasker = new Tasker();

		moduleManager = new ModuleManager(this);
		moduleManager.initializeModules();
	}

	public void main(String[] args) {
		obtainPublicAddress();

		log.log("Starting application...");

		moduleManager.onEnable();

		if (args.length == 2)
			run(args[0], Integer.parseInt(args[1]));
	}

	private void run(String name, int port) {
		Peer startPeer = peerManager.create(name, port);
		startPeer.start();
	}

	private void obtainPublicAddress() {
		log.log("Obtaining public address...", VerboseLevel.MEDIUM);
		try {
			long m = System.currentTimeMillis();
			publicAddress = Utils.obtainExternalAddress();
			log.log("Public address got in " + (System.currentTimeMillis() - m) + "ms: " + publicAddress.getHostName(),
					VerboseLevel.MEDIUM);
		} catch (IOException e) {
			log.log(e, "There was an error while obtaining public address, shutting down...");
		}
	}

	public void shutdown() {
		if (shuttingDown)
			return;

		shuttingDown = true;

		moduleManager.onDisable();

		log.log("Exiting application...");
		if (getPeerManager() != null)
			getPeerManager().shutdown();

		if (generalConfig != null)
			try {
				generalConfig.store();
			} catch (IOException e) {
				e.printStackTrace();
			}

		log.log("Bye :)");
	}
}