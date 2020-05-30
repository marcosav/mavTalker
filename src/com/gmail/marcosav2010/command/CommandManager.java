package com.gmail.marcosav2010.command;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.gmail.marcosav2010.communicator.module.ModuleLoader;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

public class CommandManager {

	private static final String LOGGER_PREFIX = "[CommandManager] ";

	private final Map<String, Command> registeredLabels;
	private final Set<Class<? extends CommandRegistry>> registries;

	public CommandManager() {
		registeredLabels = new HashMap<>();
		registries = new HashSet<>();

		register(BaseCommandRegistry.class);
		ModuleLoader.flushRegistries().forEach(this::register);
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
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			log("There was an error while initializing registry \"" + clazz.getName() + "\"");
			Logger.log(e);
			return;
		}

		int v = r.getCommands().stream().mapToInt(c -> {
			registeredLabels.put(c.getLabel(), c);
			Stream.of(c.getAliases()).forEach(a -> registeredLabels.put(a, c));
			return 1;
		}).sum();

		log("Loaded " + v + " commands from registry \"" + clazz.getName() + "\"", VerboseLevel.HIGH);
	}

	private void log(String str) {
		Logger.log(LOGGER_PREFIX + str);
	}

	private void log(String str, VerboseLevel level) {
		Logger.log(LOGGER_PREFIX + str, level);
	}
}
