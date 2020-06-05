package com.gmail.marcosav2010.connection;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.connection.exception.ConnectionRegistryException;
import com.gmail.marcosav2010.handshake.ConnectionToken;
import com.gmail.marcosav2010.handshake.HandshakeAuthenticator;
import com.gmail.marcosav2010.handshake.InvalidHandshakeKey;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Loggable;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.peer.Peer;
import lombok.Getter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class manages all @Peer established @Connection
 *
 * @author Marcos
 */
public class ConnectionManager implements Loggable {

    private static final int UUID_BYTES = Long.BYTES * 2;

    private static final long UUID_TIMEOUT = 10L;

    @Getter
    private final ILog log;

    private final Peer peer;
    private final Map<UUID, Connection> connections;

    private final HandshakeAuthenticator handshakeAuthenticator;
    private final ConnectionIdentificator connectionIdentificator;

    public ConnectionManager(Peer peer) {
        this.peer = peer;

        log = new Log(peer, "ConnectionManager");
        handshakeAuthenticator = new HandshakeAuthenticator(peer);
        connectionIdentificator = new ConnectionIdentificator();
        connections = new ConcurrentHashMap<>();
    }

    public HandshakeAuthenticator getHandshakeAuthenticator() {
        return handshakeAuthenticator;
    }

    public ConnectionIdentificator getIdentificator() {
        return connectionIdentificator;
    }

    public Connection getConnection(UUID uuid) {
        return connections.get(uuid);
    }

    public Connection removeConnection(UUID uuid) {
        Connection c = connections.remove(uuid);
        connectionIdentificator.removePeer(uuid);
        return c;
    }

    public Map<UUID, Connection> getConnectionUUIDs() {
        return Collections.unmodifiableMap(connections);
    }

    public Collection<Connection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    public boolean isConnectedTo(InetSocketAddress address) {
        return connections.values().stream().anyMatch(c -> c.getRemotePort() == address.getPort()
                && c.getRemoteAddress().getHostAddress().equals(address.getAddress().getHostAddress()));
    }

    public Connection getConnection(InetSocketAddress address) {
        return connections.values().stream()
                .filter(c -> c.getRemotePort() == address.getPort()
                        && c.getRemoteAddress().getHostAddress().equals(address.getAddress().getHostAddress()))
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
        log.log("Accepted " + remoteSocket.getRemoteSocketAddress().toString());

        ConnectionToken ct;

        try {
            ct = handshakeAuthenticator.readHandshake(remoteSocket);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.log("Remote peer didn't send handshake at time, closing remote socket...");
            remoteSocket.close();
            return;

        } catch (InvalidHandshakeKey e) {
            log.log(e.getMessage() + ", closing remote socket.");
            remoteSocket.close();
            return;
        }

        log.log("Reading temporary remote connection UUID, timeout set to " + UUID_TIMEOUT + "s...",
                VerboseLevel.MEDIUM);

        UUID uuid;
        try {
            uuid = readUUID(remoteSocket);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.log("Remote peer didn't send UUID at time, closing remote socket...");
            remoteSocket.close();
            return;
        }

        log.log("Finding and registering remote connection from temporary UUID...", VerboseLevel.MEDIUM);

        registerConnection(peer, uuid).connect(remoteSocket, ct);
    }

    private UUID readUUID(Socket remoteSocket) throws InterruptedException, ExecutionException, TimeoutException {
        byte[] b = new byte[UUID_BYTES];

        peer.getExecutorService().submit(() -> remoteSocket.getInputStream().read(b, 0, UUID_BYTES)).get(UUID_TIMEOUT,
                TimeUnit.SECONDS);

        return Utils.getUUIDFromBytes(b);
    }

    public void disconnectAll(boolean silent) {
        if (connections.isEmpty())
            return;

        log.log("Closing and removing client connections...", VerboseLevel.LOW);

        var iterator = connections.entrySet().iterator();
        while (iterator.hasNext()) {
            Connection c = iterator.next().getValue();
            iterator.remove();
            c.disconnect(silent);
        }

        log.log("Connections closed and removed successfully.", VerboseLevel.LOW);
    }
}
