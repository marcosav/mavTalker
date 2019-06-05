package com.gmail.marcosav2010.peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.tasker.TaskOwner;

/**
 * Represents a local hosted peer.
 * 
 * @author Marcos
 */
public class Peer extends KnownPeer implements TaskOwner {

	private ServerSocket server;

	private boolean started;
	private UUID uuid;

	private ConnectionManager connectionManager;

	private ExecutorService executorService;

	public Peer(String name, int port) {
		super(name, port);
		connectionManager = new ConnectionManager(this);
		executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new PeerThreadFactory());
		started = false;
		uuid = UUID.randomUUID();
	}

	private class PeerThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		PeerThreadFactory() {
			group = new ThreadGroup(Main.getInstance().getPeerManager().getPeerThreadGroup(), "peerThreadGroup-" + getName());
			namePrefix = "peerPool-" + getName() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}

	public void start() {
		try {
			log("Starting server on port " + getPort() + "...");

			server = new ServerSocket(getPort());

			started = true;

			Main.getInstance().getTasker().run(this, findClient()).setName(getName() + " Find Client");

			log("Server created and finding a someone to connect...");

		} catch (Exception ex) {
			Logger.log(ex, "There was an exception while starting peer " + getName() + ".");
			stop(true);
		}
	}

	private Runnable findClient() {
		log("Starting connection finding thread...", VerboseLevel.HIGH);
		return () -> {
			while (started) {
				try {
					log("Waiting for connection...", VerboseLevel.MEDIUM);

					Socket remoteSocket = server.accept();
					log("Someone connected, accepting...", VerboseLevel.MEDIUM);

					connectionManager.manageSocketConnection(remoteSocket);

				} catch (SocketException e) {
				} catch (Exception e) {
					Logger.log(e, "There was an exception in client find task in peer " + getName() + ".");
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

	public boolean isStarted() {
		return started;
	}

	public UUID getUUID() {
		return uuid;
	}

	public String getDisplayID() {
		return Utils.toBase64(uuid);
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	@Override
	public ConnectionIdentificator getNetworkIdentificator() {
		return connectionManager.getIdentificator();
	}

	@Override
	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void printInfo() {
		var ci = connectionManager.getIdentificator();
		var peers = ci.getConnectedPeers();
		log("Currently " + peers.size() + " peers connected: " + peers.stream().map(ConnectedPeer::getName).collect(Collectors.joining(", ")));
	}

	public void stop(boolean silent) {
		log("Shutting down peer...", VerboseLevel.MEDIUM);
		started = false;

		connectionManager.disconnectAll(silent);

		if (server != null)
			try {
				log("Closing server...", VerboseLevel.MEDIUM);
				server.close();
			} catch (IOException e) {
			}

		log("Shutting down thread pool executor...", VerboseLevel.MEDIUM);

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log("There was an error while terminating the pool, forcing shutdown...", VerboseLevel.MEDIUM);
			executorService.shutdownNow();
		}

		Main.getInstance().getPeerManager().remove(this);

		log("Shutdown done successfully.", VerboseLevel.LOW);
	}

	public void log(String str) {
		Logger.log(getName() + ": " + str);
	}

	public void log(String str, VerboseLevel level) {
		Logger.log(getName() + ": " + str, level);
	}
}
