package com.gmail.marcosav2010.peer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.logger.Logger;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the base class of a known peer, whose name and host port are known, it can be a @Peer
 * or a @ConnectedPeer.
 * 
 * @author Marcos
 */
@AllArgsConstructor
public abstract class KnownPeer implements NetworkPeer {

	@Getter
	private String name;
	@Getter
	private InetAddress address;
	@Getter
	private int port;
	@Getter
	private UUID UUID;

	public KnownPeer(String name, int port, UUID uuid) {
		this(name, null, port, uuid);
		
		try {
			this.address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			Logger.log(e);
		}
	}
	
	public String getDisplayID() {
		return Utils.toBase64(UUID);
	}
}
