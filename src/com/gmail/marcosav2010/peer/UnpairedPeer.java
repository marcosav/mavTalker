package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.connection.NetworkIdentificator;

/**
 * Represents a @NetworkPeer which is not connected to any @Peer, being able to be connected to
 * other @NetworkPeer, it represents the opposite of a @ConnectedPeer, acting like a node.
 * 
 * @author Marcos
 */
public class UnpairedPeer implements NetworkPeer {

	private NetworkIdentificator<NetworkPeer> networkManager;

	public UnpairedPeer() {
		networkManager = new NetworkIdentificator<>();
	}

	@Override
	public NetworkIdentificator<NetworkPeer> getNetworkIdentificator() {
		return networkManager;
	}
}
