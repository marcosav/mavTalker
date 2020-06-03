package com.gmail.marcosav2010.connection;

import com.gmail.marcosav2010.peer.NetworkPeer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This clase manages all remote peer connections to connected peers
 *
 * @author Marcos
 */
public class NetworkIdentificator<T extends NetworkPeer> {

    private final Map<UUID, T> connectionPeer;
    private final Map<UUID, UUID> connectionPeerUUID;

    public NetworkIdentificator() {
        connectionPeer = new ConcurrentHashMap<>();
        connectionPeerUUID = new ConcurrentHashMap<>();
    }

    public T getPeer(UUID uuid) {
        return connectionPeer.get(uuid);
    }

    public UUID getConnection(NetworkPeer peer) {
        return connectionPeer.entrySet().stream().filter(e -> e.getValue().equals(peer)).map(Map.Entry::getKey).findFirst().orElseGet(null);
    }

    public boolean hasPeer(UUID uuid) {
        return connectionPeer.containsKey(uuid);
    }

    public boolean hasPeer(NetworkPeer peer) {
        return connectionPeerUUID.containsKey(peer.getUUID());
    }

    protected void put(UUID connectionUUID, T peer) {
        connectionPeerUUID.put(connectionUUID, peer.getUUID());
        connectionPeer.put(connectionUUID, peer);
    }

    protected T remove(UUID connectionUUID) {
        connectionPeerUUID.remove(connectionUUID);
        return connectionPeer.remove(connectionUUID);
    }

    public Set<T> getConnectedPeers() {
        return Set.copyOf(connectionPeer.values());
    }

    public int size() {
        return connectionPeer.size();
    }
}
