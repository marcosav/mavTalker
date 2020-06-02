package com.gmail.marcosav2010.main;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.gmail.marcosav2010.command.CommandHandler;
import com.gmail.marcosav2010.logger.Logger;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Launcher {

	public static void main(String[] args) throws NumberFormatException, IOException, GeneralSecurityException {
		var pkg = Launcher.class.getPackage();
		Logger.log(
				"Loading " + pkg.getImplementationTitle() + " v" + pkg.getImplementationVersion() + " by marcosav...");

		Main main = new Main();
		Main.setInstance(main);
		main.init();

		main.main(args);

		listenForCommands();
	}

	private static void listenForCommands() throws IOException {
		Terminal terminal = TerminalBuilder.terminal();
		LineReader lineReader = LineReaderBuilder.builder().terminal(terminal).history(new DefaultHistory()).build();

		try {
			while (!Main.getInstance().isShuttingDown())
				CommandHandler.handleCommand(lineReader.readLine(">> "));

		} catch (org.jline.reader.UserInterruptException ex) {
			if (Main.getInstance() != null)
				Main.getInstance().shutdown();
		}
	}
}
