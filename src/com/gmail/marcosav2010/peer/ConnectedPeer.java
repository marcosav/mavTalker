package com.gmail.marcosav2010.peer;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.NetworkIdentificator;

import lombok.Getter;

/**
 * Represents a connected @NetworkPeer which is at the other side of the @Connection (with a @Peer),
 * acting like a node.
 * 
 * @author Marcos
 */
public class ConnectedPeer extends KnownPeer {

	@Getter
	private Connection connection;
	@Getter
	private NetworkIdentificator<NetworkPeer> networkIdentificator;

	public ConnectedPeer(String name, UUID uuid, Connection connection) {
		super(name, connection.getRemoteAddress(), connection.getRemotePort(), uuid);
		networkIdentificator = new NetworkIdentificator<>();
		this.connection = connection;
	}

	public void disconnect(boolean silent) throws IOException {
		connection.disconnect(silent);
	}

	public void sendPacket(Packet packet) throws PacketWriteException {
		connection.sendPacket(packet);
	}

	public void sendPacket(Packet packet, Runnable action) throws PacketWriteException {
		connection.sendPacket(packet, action);
	}

	public void sendPacket(Packet packet, Runnable action, Runnable onTimeOut, long timeout, TimeUnit timeUnit) throws PacketWriteException {
		connection.sendPacket(packet, action, onTimeOut, timeout, timeUnit);
	}
}
