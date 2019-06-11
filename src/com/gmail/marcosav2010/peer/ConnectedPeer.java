package com.gmail.marcosav2010.peer;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.handling.PacketAction;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.NetworkIdentificator;

/**
 * Represents a connected @NetworkPeer which is at the other side of the @Connection (with a @Peer),
 * acting like a node.
 * 
 * @author Marcos
 */
public class ConnectedPeer extends KnownPeer {

	private Connection connection;
	private NetworkIdentificator<NetworkPeer> networkManager;

	public ConnectedPeer(String name, UUID uuid, Connection connection) {
		super(name, connection.getPort(), uuid);
		networkManager = new NetworkIdentificator<>();
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	@Override
	public NetworkIdentificator<NetworkPeer> getNetworkIdentificator() {
		return networkManager;
	}

	public void disconnect(boolean silent) throws IOException {
		connection.disconnect(silent);
	}

	public void sendPacket(Packet packet) throws PacketWriteException {
		connection.sendPacket(packet);
	}

	public void sendPacket(Packet packet, PacketAction action) throws PacketWriteException {
		connection.sendPacket(packet, action);
	}

	public void sendPacket(Packet packet, PacketAction action, long timeout, TimeUnit timeUnit) throws PacketWriteException {
		connection.sendPacket(packet, action, timeout, timeUnit);
	}
}
