package com.gmail.marcosav2010.communicator.module;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.PacketRegistry;
import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

import lombok.Getter;

public abstract class Module implements Comparable<Module> {

	@Getter
	private final String name;
	@Getter
	private final int priority;
	private List<PacketListener> listeners;

	private ModuleManager manager;

	public Module(String name) {
		this(name, 0);
	}

	public Module(String name, int priority) {
		this.name = name;
		this.priority = priority;
		listeners = new LinkedList<>();
	}

	void registerListeners() {
		manager.registerListeners(listeners);
	}
	
	protected void registerListeners(PacketListener... l) {
		Stream.of(l).forEach(listeners::add);
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
