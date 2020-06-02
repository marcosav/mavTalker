package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.main.Main;

class StopCMD extends Command {

	StopCMD() {
		super("stop", new String[] { "st", "shutdown" }, "[peer] (empty = all)");
	}

	@Override
	public void execute(String[] arg, int args) {
		if (args > 0) {
			String target = arg[0];
			Main.getInstance().getPeerManager().shutdown(target);
		} else {
			Main.getInstance().getPeerManager().shutdown();
		}
	}
}