package com.gmail.marcosav2010.main;

import java.io.Console;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.gmail.marcosav2010.command.CommandHandler;
import com.gmail.marcosav2010.logger.Logger;

public class Launcher {

	public static void main(String[] args) throws NumberFormatException, IOException, GeneralSecurityException {
		var pkg = Launcher.class.getPackage();
		Logger.log("Loading " + pkg.getImplementationTitle() + " v" + pkg.getImplementationVersion() + " by marcosav...");

		addSignalHook();

		Main main = new Main();
		Main.setInstance(main);

		main.main(args);

		listenForCommands();
	}

	private static void listenForCommands() {
		Console console = System.console();

		if (console == null) {
			Logger.log("No console found, shutting down...");
			System.exit(0);
		}

		while (!Main.getInstance().isShuttingDown())
			CommandHandler.handleCommand(console.readLine());
	}

	private static void addSignalHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (Main.getInstance() != null)
				Main.getInstance().shutdown();
		}));
	}
}
