package com.gmail.marcosav2010.communicator.module;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ModuleManager {

	private static final String LOGGER_PREFIX = "[MM] ";

	private Map<String, Module> names = new HashMap<>();
	private PriorityQueue<Module> modules = new PriorityQueue<>();
	@Getter
	private List<PacketListener> listeners = new LinkedList<>();

	private final ModuleScope scope;

	public void initializeModules() {
		if (!ModuleLoader.isLoaded())
			throw new RuntimeException("Modules has not been loaded yet!");

		ModuleLoader.getModules().forEach((desc, m) -> {
			if (!scope.getClass().isAssignableFrom(desc.scope()))
				return;

			try {
				Module module = m.getConstructor(ModuleDescriptor.class).newInstance(desc);

				var manager = m.getSuperclass().getDeclaredField("manager");
				manager.setAccessible(true);
				manager.set(module, this);

				module.registerListeners();

				manager.setAccessible(false);

				names.put(desc.name(), module);
				modules.add(module);

				log("Successfully loaded module " + desc.name() + ".", VerboseLevel.HIGH);

			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException | NoSuchFieldException e) {

				log("There was an error while initializing module \"" + desc.name() + "\".");
				Logger.log(e);
			}
		});

		log("Loaded " + modules.size() + " modules.", VerboseLevel.LOW);
	}

	public void onEnable(ModuleScope scope) {
		modules.forEach(m -> m.onEnable(scope));
	}

	public void onDisable(ModuleScope scope) {
		modules.forEach(m -> m.onDisable(scope));
	}

	public Module getModule(String name) {
		return names.get(name);
	}

	void registerListeners(Collection<PacketListener> l) {
		listeners.addAll(l);
	}

	private void log(String str) {
		plog(LOGGER_PREFIX + str);
	}

	private void log(String str, VerboseLevel level) {
		plog(LOGGER_PREFIX + str, level);
	}

	void plog(String str) {
		scope.log(str);
	}

	void plog(String str, VerboseLevel level) {
		scope.log(str, level);
	}
}
