package com.gmail.marcosav2010.command;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class CommandRegistry {
	
	@Getter
	protected Set<Command> commands;
}
