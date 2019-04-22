package com.gmail.marcosav2010.communicator.module;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.PacketRegistry;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

public abstract class Module implements Comparable<Module> {

	private final String name;
	private final int priority;

	private ModuleManager manager;

	public Module(ModuleManager manager, String name) {
		this(manager, name, 0);
	}

	public Module(ModuleManager manager, String name, int priority) {
		this.manager = manager;
		this.name = name;
		this.priority = priority;
	}

	public String getName() {
		return name;
	}

	public int getPriority() {
		return priority;
	}

	protected void registerListeners(PacketListener... listeners) {
		manager.registerListeners(listeners);
	}

	protected static void registerPacket(int id, Class<? extends Packet> packet) {
		if (id > Byte.MAX_VALUE)
			throw new IllegalArgumentException("ID must be byte");
		
		PacketRegistry.register((byte) id, packet);
	}

	protected void onEnable(Connection connection) {
	}
	
	protected void onDisable() {
	}

	public void log(String str) {
		manager.clog("[" + name + "] " + str);
	}

	public void log(String str, VerboseLevel level) {
		manager.clog("[" + name + "] " + str, level);
	}
	
	@Override
	public int compareTo(Module m) {
		return priority - m.priority;
	}
}
