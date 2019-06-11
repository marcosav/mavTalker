package com.gmail.marcosav2010.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.HandshakeAuthentificator;
import com.gmail.marcosav2010.peer.HandshakeAuthentificator.ConnectionToken;
import com.gmail.marcosav2010.peer.HandshakeAuthentificator.InvalidHandshakeKey;
import com.gmail.marcosav2010.peer.Peer;

/**
 * This clase manages all @Peer stablished @Connection
 * 
 * @author Marcos
 */
public class ConnectionManager {

	private static final int UUID_BYTES = Long.BYTES * 2;

	private static final long UUID_TIMEOUT = 10L;

	private Peer peer;
	private Map<UUID, Connection> connections;

	private HandshakeAuthentificator handshakeAuthentificator;
	private ConnectionIdentificator connectionIdentificador;

	public ConnectionManager(Peer peer) {
		this.peer = peer;
		handshakeAuthentificator = new HandshakeAuthentificator(peer);
		connectionIdentificador = new ConnectionIdentificator();
		connections = new ConcurrentHashMap<>();
	}

	public HandshakeAuthentificator getHandshakeAuthentificator() {
		return handshakeAuthentificator;
	}

	public ConnectionIdentificator getIdentificator() {
		return connectionIdentificador;
	}

	public Connection getConnection(UUID uuid) {
		return connections.get(uuid);
	}

	public Connection removeConnection(UUID uuid) {
		Connection c = connections.remove(uuid);
		connectionIdentificador.removePeer(uuid);
		return c;
	}

	public Set<Map.Entry<UUID, Connection>> getConnectionUUIDs() {
		return Collections.unmodifiableSet(connections.entrySet());
	}

	public boolean isConnectedTo(InetSocketAddress address) {
		return connections.values().stream()
				.anyMatch(c -> c.getPort() == address.getPort() && c.getRemoteAddress().getHostAddress().equals(address.getAddress().getHostAddress()));
	}

	public Connection getConnection(InetSocketAddress address) {
		return connections.values().stream()
				.filter(c -> c.getPort() == address.getPort() && c.getRemoteAddress().getHostAddress().equals(address.getAddress().getHostAddress()))
				.findFirst().orElseGet(null);
	}

	public Connection registerConnection(Connection c) {
		UUID u = c.getUUID();
		if (u == null)
			throw new ConnectionRegistryException("Connection UUID cannot be null.");

		if (!connections.containsKey(u)) {
			connections.put(u, c);
			return c;

		} else
			return connections.get(u);
	}
	
	private Connection registerConnection(Peer peer, UUID uuid) {
		if (uuid == null)
			throw new ConnectionRegistryException("Connection UUID cannot be null.");

		if (!connections.containsKey(uuid)) {
			Connection c = new Connection(peer, uuid);
			connections.put(uuid, c);
			return c;

		} else
			return connections.get(uuid);
	}

	public void manageSocketConnection(Socket remoteSocket) throws IOException {
		log("Accepted " + remoteSocket.getRemoteSocketAddress().toString());

		ConnectionToken ct;

		try {
			ct = peer.getConnectionManager().getHandshakeAuthentificator().readHandshake(remoteSocket);

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log("Remote peer didn't send handshake at time, closing remote socket...");
			remoteSocket.close();
			return;

		} catch (InvalidHandshakeKey e) {
			log(e.getMessage() + ", closing remote socket.");
			remoteSocket.close();
			return;
		}

		log("Reading temporary remote connection UUID, timeout set to " + UUID_TIMEOUT + "s...", VerboseLevel.MEDIUM);

		UUID uuid;
		try {
			uuid = readUUID(remoteSocket);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log("Remote peer didn't send UUID at time, closing remote socket...");
			remoteSocket.close();
			return;
		}

		log("Finding and registering remote connection from temporary UUID...", VerboseLevel.MEDIUM);

		registerConnection(peer, uuid).connect(remoteSocket, ct);
	}

	private UUID readUUID(Socket remoteSocket) throws InterruptedException, ExecutionException, TimeoutException {
		byte[] b = new byte[UUID_BYTES];

		peer.getExecutorService().submit(() -> remoteSocket.getInputStream().read(b, 0, UUID_BYTES)).get(UUID_TIMEOUT, TimeUnit.SECONDS);

		return Utils.getUUIDFromBytes(b);
	}

	public void disconnectAll(boolean silent) {
		if (connections.isEmpty())
			return;

		log("Closing and removing client connections...", VerboseLevel.LOW);

		var iterator = connections.entrySet().iterator();
		while (iterator.hasNext()) {
			Connection c = iterator.next().getValue();
			iterator.remove();
			c.disconnect(silent);
		}

		log("Connections closed and removed successfully.", VerboseLevel.LOW);
	}

	public void log(String str) {
		peer.log("[ConnectionManager] " + str);
	}

	public void log(String str, VerboseLevel level) {
		peer.log("[ConnectionManager] " + str, level);
	}
}
