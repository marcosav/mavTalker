package com.gmail.marcosav2010.communicator.module;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.atteo.classindex.ClassIndex;

import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Launcher;
import com.gmail.marcosav2010.peer.Peer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ModuleManager {

	private static Set<Class<? extends CommandRegistry>> registries = new HashSet<>();
	private static Set<Class<? extends Module>> loadedModules = new HashSet<>();
	
	private static final String LOGGER_PREFIX = "[MM] ";
	private static final String STATIC_LOGGER_PREFIX = "[Static MM] ";
	
	private static boolean loaded;
	
	private Map<String, Module> names = new HashMap<>();
	private PriorityQueue<Module> modules = new PriorityQueue<>();
	@Getter
	private List<PacketListener> listeners = new LinkedList<>();
	
	private final Peer peer;

	public void initializeModules() {
		loadedModules.forEach(m -> {
			try {
				Module module = m.getConstructor().newInstance();
				
				var manager = m.getSuperclass().getDeclaredField("manager");
				manager.setAccessible(true);
				manager.set(module, this);
				
				module.registerListeners();
				
				manager.setAccessible(false);
				
				names.put(module.getName(), module);
				modules.add(module);
				
				log("Successfully loaded module " + module.getName() + ".", VerboseLevel.HIGH);
				
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException | NoSuchFieldException e) {
				
				log("There was an error while initializing module \"" + m.getName() + "\".");
				Logger.log(e);
			}
		});
		
		log("Loaded " + modules.size() + " modules.", VerboseLevel.LOW);
	}

	public void onConnectionEnable(Connection connection) {
		modules.forEach(m -> m.onEnable(connection));
	}

	public void onConnectionDisable(Connection connection) {
		modules.forEach(m -> m.onDisable(connection));
	}

	public Module getModule(String name) {
		return names.get(name);
	}

	void registerListeners(Collection<PacketListener> l) {
		listeners.addAll(l);
	}

	static void addCommands(Class<? extends CommandRegistry> cmds) {
		registries.add(cmds);
	}

	public static Set<Class<? extends CommandRegistry>> flushRegistries() {
		var r = new HashSet<>(registries);
		registries.clear();
		return r;
	}

	private void log(String str) {
		plog(LOGGER_PREFIX + str);
	}

	private void log(String str, VerboseLevel level) {
		plog(LOGGER_PREFIX + str, level);
	}
	
	void plog(String str) {
		peer.log(str);
	}

	void plog(String str, VerboseLevel level) {
		peer.log(str, level);
	}

	public static void loadModules() {
		if (loaded)
			throw new IllegalStateException("Modules are already loaded.");
			
		loaded = true;
		
		Iterable<Class<?>> matches;

		matches = ClassIndex.getAnnotated(LoadModule.class, Launcher.class.getClassLoader());

		for (Class<?> clazz : matches) {
			if (Modifier.isAbstract(clazz.getModifiers()))
				continue;
			
			try {
				var c = (Class<? extends Module>) clazz;
				
				LoadModule m = (LoadModule) clazz.getAnnotation(LoadModule.class);
				
				if (!m.load())
					continue;
				
				loadedModules.add(c);
				
				Class<? extends CommandRegistry> registryClass = m.registry();
	
				addCommands(registryClass);
				
				Logger.log(STATIC_LOGGER_PREFIX + "Found module in class \"" + c.getName() + "\".", VerboseLevel.HIGH);
				
			} catch (Exception e) {
				Logger.log(STATIC_LOGGER_PREFIX + "There was an error while loading class \"" + clazz.getName() + "\"");
				Logger.log(e);
			}
		}
	}
}
