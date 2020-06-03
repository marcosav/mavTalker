package com.gmail.marcosav2010.peer;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Loggable;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class manages all local hosted @Peer.
 *
 * @author Marcos
 */
public class PeerManager implements Loggable {

    public static final int MAX_NAME_LENGTH = 16;
    private static final int DEFAULT_PORT_BASE = 55551;
    @Getter
    private final ILog log;

    private final ExecutorService executorService;
    private final Map<String, Peer> peers;
    private int peersCreated;

    public PeerManager() {
        this(new DefaultPeerExecutor("peerManager", new ThreadGroup("parentPeerThreadGroup")));
    }

    public PeerManager(ExecutorService executorService) {
        log = new Log(Logger.getGlobal(), "PeerMan");
        peersCreated = 0;

        this.executorService = executorService;

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

    private String suggestName() {
        return "P" + (peersCreated + 1);
    }

    public int suggestPort() {
        return getSuitablePort() + 1;
    }

    private Peer remove(String peer) {
        return peers.remove(peer);
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

        Peer peer = new Peer(name, port, log, executorService, Main.getInstance().getGeneralConfig());
        peers.put(name, peer);
        peersCreated++;
        return peer;
    }

    public void shutdown() {
        if (peers.isEmpty())
            return;

        log.log("Shutting down all peers...");
        var iterator = peers.entrySet().iterator();
        while (iterator.hasNext()) {
            Peer p = iterator.next().getValue();
            iterator.remove();
            p.stop(false);
        }
        log.log("All peers have been shutdown.");

        log.log("Shutting down thread pool executor...", VerboseLevel.MEDIUM);

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.log("There was an error while terminating the pool, forcing shutdown...", VerboseLevel.MEDIUM);
            executorService.shutdownNow();
        }
    }

    public void shutdown(String peer) {
        if (exists(peer)) {
            Peer p = remove(peer);
            p.stop(false);
        }
    }

    private boolean isValidName(String name) {
        if (name.contains(" ")) {
            log.log("\"" + name + "\" contains spaces, which are not allowed.");
            return false;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            log.log("\"" + name + "\" exceeds the " + MAX_NAME_LENGTH + " char limit.");
            return false;
        }
        if (exists(name)) {
            log.log("This name \"" + name + "\" is being used by an existing Peer, try again with a different one.");
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

    public void printInfo() {
        log.log("Peers Running -> " + peers.values().stream()
                .map(p -> p.getName() + " ("
                        + p.getConnectionManager().getIdentificator().getConnectedPeers().stream()
                        .map(c -> c.getName() + " " + c.getDisplayID()).collect(Collectors.joining(" "))
                        + ")")
                .collect(Collectors.joining(", ")));
    }
}
