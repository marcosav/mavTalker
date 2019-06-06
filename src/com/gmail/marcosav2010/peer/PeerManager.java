package com.gmail.marcosav2010.peer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

/**
 * This class manages all local hosted @Peer.
 * 
 * @author Marcos
 */
public class PeerManager {

	private static final int DEFAULT_PORT_BASE = 55551;
	public static final int MAX_NAME_LENGTH = 16;

	private final ThreadGroup parentPeerThreadGroup;

	private int peersCreated;

	private final Map<String, Peer> peers;

	public PeerManager(GeneralConfiguration config) {
		peersCreated = 0;
		parentPeerThreadGroup = new ThreadGroup("parentPeerThreadGroup");
		peers = new ConcurrentHashMap<>();
	}

	public Peer get(String name) {
		return peers.get(name);
	}

	public boolean exists(String name) {
		return peers.containsKey(name);
	}

	private int getSuitablePort() {
		return DEFAULT_PORT_BASE + peersCreated;
	}

	public String suggestName() {
		return "P" + (peersCreated + 1);
	}

	public int suggestPort() {
		return getSuitablePort() + 1;
	}

	public Peer remove(Peer peer) {
		return peers.remove(peer.getName());
	}

	public Peer create() {
		return create(suggestName());
	}

	public Peer create(String name) {
		return create(name, getSuitablePort());
	}

	public Peer create(String name, int port) {
		if (!isValidName(name))
			return null;
		Peer peer = new Peer(name, port);
		peers.put(name, peer);
		peersCreated++;
		return peer;
	}

	public void shutdown() {
		if (peers.isEmpty())
			return;
		
		log("Shutting down all peers...");
		var iterator = peers.entrySet().iterator();
		while (iterator.hasNext()) {
			Peer p = iterator.next().getValue();
			iterator.remove();
			p.stop(false);
		}
		log("All peers have been shutdown.");
	}

	public void shutdown(String peer) {
		if (exists(peer))
			get(peer).stop(false);
	}

	private boolean isValidName(String name) {
		if (name.contains(" ")) {
			log("\"" + name + "\" contains spaces, which are not allowed.");
			return false;
		}
		if (name.length() > MAX_NAME_LENGTH) {
			log("\"" + name + "\" exceeds the " + MAX_NAME_LENGTH + " char limit.");
			return false;
		}
		if (exists(name)) {
			log("This name \"" + name + "\" is being used by an existing Peer, try again with a different one.");
			return false;
		}
		return true;
	}

	public int count() {
		return peers.size();
	}

	public Peer getFirstPeer() {
		return peers.values().iterator().next();
	}

	ThreadGroup getPeerThreadGroup() {
		return parentPeerThreadGroup;
	}

	public void printInfo() {
		log("Peers Running -> " + peers.values().stream().map(p -> p.getName() + " (" + p.getConnectionManager().getIdentificator().getConnectedPeers().stream()
				.map(c -> c.getName() + " " + c.getConnectionUUID()).collect(Collectors.joining(" ")) + ")").collect(Collectors.joining(", ")));
	}

	public void log(String str) {
		Logger.log("[PeerManager]: " + str);
	}

	public void log(String str, VerboseLevel level) {
		Logger.log("[PeerManager]: " + str, level);
	}
}
