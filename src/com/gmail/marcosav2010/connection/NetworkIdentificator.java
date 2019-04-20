package com.gmail.marcosav2010.connection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.gmail.marcosav2010.peer.NetworkPeer;

/**
 * This clase manages all remote peer connections to connected peers
 * 
 * @author Marcos
 */
public class NetworkIdentificator<T extends NetworkPeer> {

	protected Map<UUID, T> peers;

	public NetworkIdentificator() {
		//peers = Collections.synchronizedMap(new HashMap<>());
		peers = new ConcurrentHashMap<>();
	}

	public T getPeer(UUID uuid) {
		return peers.get(uuid);
	}
	
	public UUID getConnection(NetworkPeer peer) {
		return peers.entrySet().stream().filter(e -> e.getValue().equals(peer)).map(e -> e.getKey()).findFirst().orElseGet(null);
	}

	public boolean hasPeer(UUID uuid) {
		return peers.containsKey(uuid);
	}

	public boolean hasPeer(NetworkPeer peer) {
		return peers.containsValue(peer);
	}
	
	public Set<T> getConnectedPeers() {
		return Collections.unmodifiableSet(new HashSet<>(peers.values()));
	}
	
	public int size() {
		return peers.size();
	}
}
