package com.gmail.marcosav2010.peer;

/**
 * Represents the base class of a known peer, whose name and host port are known, it can be a @Peer
 * or a @ConnectedPeer.
 * 
 * @author Marcos
 */
public abstract class KnownPeer implements NetworkPeer {

	private String name;
	private int port;

	public KnownPeer(String name, int port) {
		this.name = name;
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}
}
