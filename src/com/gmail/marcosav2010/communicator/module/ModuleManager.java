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
import java.util.stream.Stream;

import org.atteo.classindex.ClassIndex;

import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.communicator.module.fth.FTModule;
import com.gmail.marcosav2010.communicator.module.fth.FileTransferHandler;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Launcher;

public class ModuleManager {

	private static Set<Class<? extends CommandRegistry>> registries = new HashSet<>();
	private static Set<Class<? extends Module>> loadedModules = new HashSet<>();

	static {
		loadModules();
	}
	
	private Map<String, Module> names;
	private PriorityQueue<Module> modules;
	private List<PacketListener> listeners;

	private Connection connection;

	public ModuleManager(Connection connection) {
		this.connection = connection;
		
		modules = new PriorityQueue<>();
		names = new HashMap<>();
		listeners = new LinkedList<>();
	}
	
	public int initializeModules() {
		loadedModules.forEach(m -> {
			try {
				Module module = m.getConstructor(ModuleManager.class).newInstance(this);
				
				names.put(module.getName(), module);
				modules.add(module);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				Logger.log(e);
			}
		});
		
		return modules.size();
	}

	public void enable() {
		modules.forEach(m -> m.onEnable(connection));
	}

	public void disable() {
		modules.forEach(m -> m.onDisable());
	}

	public Module get(String name) {
		return names.get(name);
	}

	public FileTransferHandler getFTH() {
		return ((FTModule) get("FTH")).getFTH();
	}

	void registerListeners(PacketListener... l) {
		Stream.of(l).forEach(listeners::add);
	}

	static void addCommands(Class<? extends CommandRegistry> cmds) {
		registries.add(cmds);
	}

	public static Set<Class<? extends CommandRegistry>> flushRegistries() {
		var r = new HashSet<>(registries);
		registries.clear();
		return r;
	}

	public Collection<PacketListener> getListeners() {
		return listeners;
	}

	void log(String str) {
		connection.log(str);
	}

	void log(String str, VerboseLevel level) {
		connection.log(str, level);
	}

	private static void loadModules() {
		Iterable<Class<?>> matches;

		matches = ClassIndex.getAnnotated(LoadModule.class, Launcher.class.getClassLoader());

		for (Class<?> clazz : matches) {
			if (Modifier.isAbstract(clazz.getModifiers()))
				continue;
			
			var c = (Class<? extends Module>) clazz;
			
			LoadModule m = (LoadModule) clazz.getAnnotation(LoadModule.class);
			
			loadedModules.add(c);
			
			Class<? extends CommandRegistry> registryClass = m.registry();

			addCommands(registryClass);
		}
	}
}
