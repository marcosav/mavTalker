package com.gmail.marcosav2010.command;

import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class CommandRegistry {

	@Getter
	protected final Set<Command> commands;
}
