package com.gmail.marcosav2010.command;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.gmail.marcosav2010.communicator.module.ModuleManager;

public class CommandManager {

	private final Map<String, Command> registeredLabels;
	private final Set<Class<? extends CommandRegistry>> registries;

	public CommandManager() {
		registeredLabels = new HashMap<>();
		registries = new HashSet<>();
		
		register(BaseCommandRegistry.class);
		ModuleManager.flushRegistries().forEach(this::register);
	}

	Set<Command> getCommands() {
		return Collections.unmodifiableSet(new HashSet<>(registeredLabels.values()));
	}

	Command fetch(String label) {
		return registeredLabels.get(label);
	}
	
	<T extends CommandRegistry> void register(Class<T> clazz) {
		if (registries.contains(clazz))
			return;
		
		registries.add(clazz);
		
		T r;
		try {
			r = clazz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			return;
		}
		r.getCommands().forEach(c -> {
			registeredLabels.put(c.getLabel(), c);
			Stream.of(c.getAliases()).forEach(a -> registeredLabels.put(a, c));
		});
	}
}
