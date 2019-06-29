package com.gmail.marcosav2010.peer;

import java.util.UUID;

import com.gmail.marcosav2010.connection.NetworkIdentificator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a @NetworkPeer which is not connected to any @Peer, being able to be connected to
 * other @NetworkPeer, it represents the opposite of a @ConnectedPeer, acting like a node.
 * 
 * @author Marcos
 */
@RequiredArgsConstructor
public class UnpairedPeer implements NetworkPeer {

	@Getter
	private NetworkIdentificator<NetworkPeer> networkIdentificator = new NetworkIdentificator<>();
	@Getter
	private final UUID UUID;
}
