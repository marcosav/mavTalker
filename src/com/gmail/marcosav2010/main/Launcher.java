package com.gmail.marcosav2010.main;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Scanner;

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
		Scanner scn = new Scanner(System.in);

		while (scn.hasNextLine()) {
			String in = scn.nextLine();
			CommandHandler.handleCommand(in);
		}

		scn.close();
	}
	
	private static void addSignalHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Main.getInstance().shutdown();
		}));
	}
}
