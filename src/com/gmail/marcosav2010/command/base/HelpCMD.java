package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.main.Main;

class HelpCMD extends Command {

	HelpCMD() {
		super("help");
	}

	@Override
	public void execute(String[] arg, int args) {
		log.log(">> cmd <required> [optional] (info)");
		Main.getInstance().getCommandManager().getCommands().forEach(c -> log.log(">> " + c.getUsage()));
	}
}