package com.gmail.marcosav2010.command;

import com.gmail.marcosav2010.command.base.BaseCommandRegistry;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.module.CommandModuleLoader;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class CommandManager {

	private final ILog log;

	private final Map<String, Command> registeredLabels;
	private final Set<Class<? extends CommandRegistry>> registries;

	public CommandManager() {
		log = new Log(Main.getInstance(), "CommandManager");
		registeredLabels = new HashMap<>();
		registries = new HashSet<>();

		register(BaseCommandRegistry.class);
		CommandModuleLoader.getInstance().flushRegistries().forEach(this::register);
	}

	public Set<Command> getCommands() {
		return Set.copyOf(registeredLabels.values());
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
			log.log(e, "There was an error while initializing registry \"" + clazz.getName() + "\"");
			return;
		}

		int v = r.getCommands().stream().mapToInt(c -> {
			registeredLabels.put(c.getLabel(), c);
			Stream.of(c.getAliases()).forEach(a -> registeredLabels.put(a, c));
			return 1;
		}).sum();

		log.log("Loaded " + v + " commands from registry \"" + clazz.getName() + "\"", VerboseLevel.HIGH);
	}
}
