package com.gmail.marcosav2010.main;

import java.io.Console;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.gmail.marcosav2010.command.CommandHandler;
import com.gmail.marcosav2010.logger.Logger;

public class Launcher {

	public static void main(String[] args) throws NumberFormatException, IOException, GeneralSecurityException {
		Logger.log("Loading...");
		
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
		
		while (true)
			CommandHandler.handleCommand(console.readLine());
	}
	
	private static void addSignalHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Main.getInstance().shutdown();
		}));
	}
}
