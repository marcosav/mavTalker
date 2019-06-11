package com.gmail.marcosav2010.peer;

import java.util.UUID;

import com.gmail.marcosav2010.common.Utils;

/**
 * Represents the base class of a known peer, whose name and host port are known, it can be a @Peer
 * or a @ConnectedPeer.
 * 
 * @author Marcos
 */
public abstract class KnownPeer implements NetworkPeer {

	private String name;
	private int port;
	private UUID uuid;

	public KnownPeer(String name, int port, UUID uuid) {
		this.name = name;
		this.port = port;
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getDisplayID() {
		return Utils.toBase64(uuid);
	}
}
