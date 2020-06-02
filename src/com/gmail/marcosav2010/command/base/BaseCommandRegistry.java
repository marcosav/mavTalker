package com.gmail.marcosav2010.command.base;

import java.util.Set;

import com.gmail.marcosav2010.command.CommandRegistry;

public class BaseCommandRegistry extends CommandRegistry {

	public BaseCommandRegistry() {
		super(Set.of(new ExitCMD(), new VerboseCMD(), new InfoCMD(), new StopCMD(), new NewCMD(), new HelpCMD(),
				new DisconnectCMD(), new GenerateAddressCMD(), new ConnectionKeyCMD(), new PeerPropertyCMD(),
				new PingCMD()));
	}
}
