package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.config.IConfiguration;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.module.ModuleManager;
import com.gmail.marcosav2010.module.ModuleScope;
import com.gmail.marcosav2010.tasker.TaskOwner;
import com.gmail.marcosav2010.tasker.Tasker;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Represents a local hosted peer.
 *
 * @author Marcos
 */
public class Peer extends KnownPeer implements TaskOwner, ModuleScope {

    @Getter
    private final ILog log;
    @Getter
    private final ConnectionManager connectionManager;
    @Getter
    private final PeerProperties properties;
    @Getter
    private final ExecutorService executorService;
    @Getter
    private final ModuleManager moduleManager;
    private final boolean externalExecutor;
    private ServerSocket server;
    @Getter
    private boolean started;
    @Getter
    @Setter
    private AtomicInteger connectionCount;

    public Peer(String name, int port, IConfiguration configuration) {
        this(name, port, Logger.getGlobal(), new DefaultPeerExecutor(name), configuration);
    }

    public Peer(String name, int port, ILog log, ExecutorService executorService, IConfiguration configuration) {
        super(name, port, UUID.randomUUID());

        this.log = new Log(log, name);

        connectionCount = new AtomicInteger();

        properties = new PeerProperties(configuration);
        connectionManager = new ConnectionManager(this);

        externalExecutor = true;
        this.executorService = executorService;

        moduleManager = new ModuleManager(this);
        moduleManager.initializeModules();
    }

    public void start() {
        try {
            log.log("Loading modules...", VerboseLevel.LOW);

            moduleManager.onEnable();

            log.log("Starting server on port " + getPort() + "...");

            server = new ServerSocket(getPort());

            started = true;

            Tasker.getInstance().run(this, findClient()).setName(getName() + " Find Client");

            log.log("Server created and waiting for someone to connect...");

        } catch (Exception ex) {
            log.log(ex, "There was an exception while starting peer " + getName() + ".");
            stop(true);
        }
    }

    private Runnable findClient() {
        log.log("Starting connection finding thread...", VerboseLevel.HIGH);
        return () -> {
            while (started) {
                try {
                    log.log("Waiting for connection...", VerboseLevel.HIGH);

                    Socket remoteSocket = server.accept();
                    log.log("Someone connected, accepting...", VerboseLevel.MEDIUM);

                    connectionManager.manageSocketConnection(remoteSocket);

                } catch (SocketException ignored) {
                } catch (Exception e) {
                    log.log(e, "There was an exception in client find task in peer " + getName() + ".");
                    stop(true);
                }
            }
        };
    }

    public Connection connect(InetSocketAddress peerAddress) throws IOException, GeneralSecurityException {
        Connection connection = new Connection(this);
        connection.connect(peerAddress);

        return connectionManager.registerConnection(connection);
    }

    @Override
    public ConnectionIdentificator getNetworkIdentificator() {
        return connectionManager.getIdentificator();
    }

    public void printInfo() {
        var ci = connectionManager.getIdentificator();
        var peers = ci.getConnectedPeers();
        log.log("Name: " + getName() + "\n" + "Display ID: " + getDisplayID() + "\n" + "Currently " + peers.size()
                + " peers connected: " + peers.stream().map(cp -> "\n - " + cp.getName() + " #" + cp.getDisplayID()
                + " CUUID: " + cp.getConnection().getUUID()).collect(Collectors.joining(", ")));
    }

    public int getAndInc() {
        return connectionCount.getAndIncrement();
    }

    public void stop(boolean silent) {
        log.log("Shutting down peer...", VerboseLevel.MEDIUM);
        started = false;

        moduleManager.onDisable();

        connectionManager.disconnectAll(silent);

        if (server != null)
            try {
                log.log("Closing server...", VerboseLevel.MEDIUM);
                server.close();
            } catch (IOException ignored) {
            }

        if (!externalExecutor) {
            log.log("Shutting down thread pool executor...", VerboseLevel.MEDIUM);

            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.log("There was an error while terminating the pool, forcing shutdown...", VerboseLevel.MEDIUM);
                executorService.shutdownNow();
            }
        }

        log.log("Shutdown done successfully.", VerboseLevel.LOW);
    }
}
