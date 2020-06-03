package com.gmail.marcosav2010.module.fth.command;

import java.util.Set;

import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.module.fth.FTModule;
import com.gmail.marcosav2010.module.fth.FileTransferHandler;
import com.gmail.marcosav2010.connection.Connection;

public class FTCommandRegistry extends CommandRegistry {

	public FTCommandRegistry() {
		super(Set.of(new FileCMD(), new ClearDownloadsCMD(), new DownloadCMD()/* , new FindCMD() */));
	}

	static FileTransferHandler getFTH(Connection c) {
		return ((FTModule) c.getModuleManager().getModule("FTH")).getFTH();
	}
}