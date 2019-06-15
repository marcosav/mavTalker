package com.gmail.marcosav2010.peer;

import java.util.UUID;

import com.gmail.marcosav2010.connection.NetworkIdentificator;

/**
 * Represents the base of any kind of remote peer, which can be connected or not to a @Peer,
 * resembles a node.
 * 
 * @author Marcos
 */
public interface NetworkPeer {

	public NetworkIdentificator<? extends NetworkPeer> getNetworkIdentificator();
	
	public UUID getUUID();
}
