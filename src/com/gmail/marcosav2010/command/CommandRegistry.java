package com.gmail.marcosav2010.command;

import java.util.Set;

public abstract class CommandRegistry {
	
	protected Set<Command> commands;
	
	public CommandRegistry(Set<Command> commands) {
		this.commands = commands;
	}
	
	public Set<Command> getCommands() {
		return commands;
	}
}
