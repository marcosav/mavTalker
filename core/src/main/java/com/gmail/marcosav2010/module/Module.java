package com.gmail.marcosav2010.module;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.PacketRegistry;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Loggable;
import lombok.Getter;

public abstract class Module implements Comparable<Module>, Loggable {

	@Getter
	private final String name;
	@Getter
	private final int priority;
	@Getter
	private ILog log;

	public Module(ModuleDescriptor moduleDescriptor) {
		name = moduleDescriptor.name();
		priority = moduleDescriptor.priority();
	}

	protected static void registerPacket(int id, Class<? extends Packet> packet) {
		if (id > Byte.MAX_VALUE)
			throw new IllegalArgumentException("ID must be byte");

		PacketRegistry.register((byte) id, packet);
	}

	/**
	 * Called when module is created: Connection, Peer: instantiation. Sets
	 * up logger.
	 *
	 * @param scope in which module lives.
	 */
	protected void onInit(ModuleScope scope) {
		log = new Log(scope, name);
	}

	/**
	 * Called on: Connection: pairing complete. Peer: start.
	 *
	 * @param scope in which module lives.
	 */
	protected void onEnable(ModuleScope scope) {
	}

	/**
	 * Called on: Connection: beginning of disconnection. Peer: beginning
	 * of shutdown.
	 *
	 * @param scope in which module lives.
	 */
	protected void onDisable(ModuleScope scope) {
	}

	@Override
	public int compareTo(Module m) {
		return priority - m.priority;
	}
}
