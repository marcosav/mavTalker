package com.gmail.marcosav2010.connection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.gmail.marcosav2010.communicator.packet.packets.PacketIdentify;
import com.gmail.marcosav2010.peer.ConnectedPeer;

/**
 * This clase manages all peer connections when connected peer registers
 * 
 * @author Marcos
 */
public class ConnectionIdentificator extends NetworkIdentificator<ConnectedPeer> {

	private Map<String, ConnectedPeer> namePeer;

	public ConnectionIdentificator() {
		namePeer = new ConcurrentHashMap<>();
	}

	protected ConnectedPeer identifyConnection(Connection connection, PacketIdentify info) {
		String name = getSuitableName(info.getName());
		ConnectedPeer c = new ConnectedPeer(name, info.getPeerUUID(), connection);
		namePeer.put(name, c);
		peers.put(connection.getUUID(), c);
		return c;
	}

	private String getSuitableName(String providedName) {
		while (namePeer.containsKey(providedName))
			providedName += "_";
		return providedName;
	}

	public ConnectedPeer getPeer(String name) {
		return namePeer.get(name);
	}

	public boolean hasPeer(String name) {
		return namePeer.containsKey(name);
	}

	protected ConnectedPeer removePeer(UUID uuid) {
		ConnectedPeer c = peers.remove(uuid);
		if (c == null)
			return null;
		return namePeer.remove(c.getName());
	}

	protected ConnectedPeer removePeer(String name) {
		ConnectedPeer c = namePeer.remove(name);
		if (c == null)
			return null;
		return peers.remove(c.getConnection().getUUID());
	}
}
