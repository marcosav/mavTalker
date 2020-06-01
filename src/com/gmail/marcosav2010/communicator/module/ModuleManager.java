package com.gmail.marcosav2010.communicator.module;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

import lombok.Getter;

public class ModuleManager {

	@Getter
	private final ILog log;

	private Map<String, Module> names = new HashMap<>();
	private PriorityQueue<Module> modules = new PriorityQueue<>();
	@Getter
	private List<PacketListener> listeners = new LinkedList<>();

	private final ModuleScope scope;

	public ModuleManager(ModuleScope scope) {
		this.scope = scope;
		log = new Log(scope, "MM");
	}

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

				log.log("Successfully loaded module " + desc.name() + ".", VerboseLevel.HIGH);

			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException | NoSuchFieldException e) {

				log.log(e, "There was an error while initializing module \"" + desc.name() + "\".");
			}
		});

		log.log("Loaded " + modules.size() + " modules.", VerboseLevel.LOW);
	}

	public void onEnable() {
		modules.forEach(m -> m.onEnable(scope));
	}

	public void onDisable() {
		modules.forEach(m -> m.onDisable(scope));
	}

	public Module getModule(String name) {
		return names.get(name);
	}

	void registerListeners(Collection<PacketListener> l) {
		listeners.addAll(l);
	}
}
