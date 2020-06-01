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

import com.gmail.marcosav2010.communicator.module.ModuleManager;
import com.gmail.marcosav2010.communicator.module.ModuleScope;
import com.gmail.marcosav2010.config.GeneralConfiguration;
import com.gmail.marcosav2010.config.GeneralConfiguration.Properties;
import com.gmail.marcosav2010.config.GeneralConfiguration.PropertyCategory;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.connection.ConnectionIdentificator;
import com.gmail.marcosav2010.connection.ConnectionManager;
import com.gmail.marcosav2010.handshake.HandshakeAuthentificator.HandshakeRequirementLevel;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.tasker.TaskOwner;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a local hosted peer.
 * 
 * @author Marcos
 */
public class Peer extends KnownPeer implements TaskOwner, ModuleScope {

	@Getter
	private ConnectionManager connectionManager;
	@Getter
	private PeerProperties properties;
	@Getter
	private ExecutorService executorService;
	@Getter
	private ModuleManager moduleManager;
	@Getter
	private final ILog log;

	private ServerSocket server;

	@Getter
	private boolean started;

	@Getter
	@Setter
	private AtomicInteger connectionCount;

	public Peer(PeerManager peerManager, String name, int port) {
		super(name, port, UUID.randomUUID());

		log = new Log(peerManager, name);
		connectionCount = new AtomicInteger();
		properties = new PeerProperties();
		connectionManager = new ConnectionManager(this);
		executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), new PeerThreadFactory());
		started = false;
	}

	private class PeerThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		PeerThreadFactory() {
			group = new ThreadGroup(Main.getInstance().getPeerManager().getPeerThreadGroup(),
					"peerThreadGroup-" + getName());
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
			log.log("Initializing module manager and loading modules...", VerboseLevel.LOW);

			moduleManager = new ModuleManager(this);
			moduleManager.initializeModules();
			moduleManager.onEnable();

			log.log("Starting server on port " + getPort() + "...");

			server = new ServerSocket(getPort());

			started = true;

			Main.getInstance().getTasker().run(this, findClient()).setName(getName() + " Find Client");

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

				} catch (SocketException e) {
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
			} catch (IOException e) {
			}

		log.log("Shutting down thread pool executor...", VerboseLevel.MEDIUM);

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.log("There was an error while terminating the pool, forcing shutdown...", VerboseLevel.MEDIUM);
			executorService.shutdownNow();
		}

		Main.getInstance().getPeerManager().remove(this);

		log.log("Shutdown done successfully.", VerboseLevel.LOW);
	}

	public static class PeerProperties extends Properties {

		public PeerProperties() {
			super(PropertyCategory.PEER, Main.getInstance().getGeneralConfig());
		}

		public HandshakeRequirementLevel getHRL() {
			return super.get(GeneralConfiguration.HANDSHAKE_REQUIREMENT_LEVEL);
		}

		public void setHRL(HandshakeRequirementLevel level) {
			super.set(GeneralConfiguration.HANDSHAKE_REQUIREMENT_LEVEL, level);
		}
	}
}
