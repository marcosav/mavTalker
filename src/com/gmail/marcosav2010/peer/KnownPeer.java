package com.gmail.marcosav2010.peer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.logger.Logger;

/**
 * Represents the base class of a known peer, whose name and host port are known, it can be a @Peer
 * or a @ConnectedPeer.
 * 
 * @author Marcos
 */
public abstract class KnownPeer implements NetworkPeer {

	private String name;
	private int port;
	private InetAddress address;
	private UUID uuid;

	public KnownPeer(String name, int port, UUID uuid) {
		this(name, null, port, uuid);
		
		try {
			this.address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			Logger.log(e);
		}
	}
	
	public KnownPeer(String name, InetAddress address, int port, UUID uuid) {
		this.name = name;
		this.port = port;
		this.uuid = uuid;
		this.address = address;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getDisplayID() {
		return Utils.toBase64(uuid);
	}
}
