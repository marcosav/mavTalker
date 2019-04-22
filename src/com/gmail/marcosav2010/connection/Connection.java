package com.gmail.marcosav2010.connection;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gmail.marcosav2010.cipher.CipheredCommunicator;
import com.gmail.marcosav2010.cipher.EncryptedMessage;
import com.gmail.marcosav2010.cipher.SessionCipher;
import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.BaseCommunicator;
import com.gmail.marcosav2010.communicator.module.ModuleManager;
import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.handling.PacketAction;
import com.gmail.marcosav2010.communicator.packet.handling.PacketMessager;
import com.gmail.marcosav2010.communicator.packet.packets.PacketShutdown;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketReader;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;
import com.gmail.marcosav2010.tasker.Task;

/**
 * Represents the connection between two @Peer
 * 
 * @author Marcos
 */
public class Connection extends NetworkConnection {

	private static final long AUTH_TIMEOUT = 20L;
	private static final long PORT_TIMEOUT = 3L;
	private static final int SOCKET_CONNECT_TIMEOUT = 5000;

	private String LOGGER_PREFIX = "[Connection] [-] ";

	private Peer peer;
	private ConnectedPeer connectedPeer;

	private Socket hostSocket;

	private AtomicBoolean connected;

	private Task listenTask;

	private IdentificationController idController;

	private int remotePort;
	private InetAddress remoteAddress;

	private BaseCommunicator baseCommunicator;

	private SessionCipher sessionCipher;
	private CipheredCommunicator cipheredCommunicator;

	private PacketReader reader;
	private PacketMessager messager;
	
	private ModuleManager moduleManager;

	public Connection(Peer peer) {
		this.peer = peer;
		connected = new AtomicBoolean(false);
		idController = new IdentificationController(this);
		init();
	}

	public Connection(Peer peer, UUID uuid) {
		this(peer);
		idController = new IdentificationController(this, uuid);
	}

	private void init() {
		remotePort = -1;
		baseCommunicator = new BaseCommunicator();
		sessionCipher = SessionCipher.create(this);
	}

	public void connect(Socket remoteSocket) throws IOException {
		connected.set(true);
		remoteAddress = remoteSocket.getInetAddress();

		log("Setting Input stream...", VerboseLevel.MEDIUM);
		baseCommunicator.setIn(remoteSocket.getInputStream());

		try {
			listenForAuth();
			listenForPort();
		} catch (Exception e) {
			onException(e);
			return;
		}

		log("Setup completed, now executing listen task...", VerboseLevel.LOW);
		listenTask = Main.getInstance().getTasker().run(peer, listenSocket());
	}

	private Runnable listenSocket() {
		return () -> {
			while (connected.get()) {
				EncryptedMessage read;

				try {
					read = cipheredCommunicator.read();

					cipheredCommunicator.decrypt(read, bytes -> onRead(bytes));

				} catch (IOException e) {
					if (!connected.get())
						return;
					log("Connection lost unexpectedly: " + e.getMessage());
					Logger.log(e);
					disconnect(true);
					return;

				} catch (Exception e) {
					Main.handleException(e, peer.getName());
					continue;
				}
			}
		};
	}

	public void connect(InetSocketAddress address) throws IOException, GeneralSecurityException {
		if (address == null)
			throw new IllegalArgumentException("Address cannot be null.");

		if (!(hostSocket == null || hostSocket.isBound()))
			throw new IllegalStateException("Already connected");

		if (address.getPort() == peer.getPort())
			throw new IllegalStateException("Cannon connect to itself");

		log("Connecting to " + address.toString() + "...");

		Socket connectingSocket = new Socket();
		connectingSocket.connect(address, SOCKET_CONNECT_TIMEOUT);

		log("Setting host socket...", VerboseLevel.MEDIUM);

		hostSocket = connectingSocket;
		remotePort = address.getPort();

		log("Setting Output stream...", VerboseLevel.MEDIUM);
		baseCommunicator.setOut(hostSocket.getOutputStream());

		idController.handleTemporaryUUID();

		log("Generating session input cipher using " + SessionCipher.RSA_KEY_ALGORITHM + "-" + SessionCipher.RSA_KEY_SIZE + "...", VerboseLevel.MEDIUM);
		sessionCipher.generate();

		log("Starting authentification...", VerboseLevel.MEDIUM);
		startAuthentication();

		if (!isConnected()) {
			log("Providing port to connect...", VerboseLevel.MEDIUM);
			writePort();
		}

		log("Successfully connected to " + hostSocket.getRemoteSocketAddress() + ".");
	}

	private void listenForAuth() throws InterruptedException, ExecutionException, TimeoutException, PacketWriteException, GeneralSecurityException {
		if (!sessionCipher.isWaitingForRemoteAuth())
			return;

		log("Waiting for remote authentication, timeout set to " + AUTH_TIMEOUT + "s...", VerboseLevel.MEDIUM);

		byte[] respose;

		try {
			respose = baseCommunicator.read(SessionCipher.RSA_KEY_MSG, peer, AUTH_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log("Remote peer didn't send authentication at time.");
			throw e;
		}

		log("Loading authentication respose...", VerboseLevel.LOW);
		sessionCipher.loadAuthenticationRespose(respose);
	}

	private void listenForPort() throws InterruptedException, ExecutionException, TimeoutException, IOException, GeneralSecurityException {
		if (!shouldReadPort())
			return;
		log("Waiting for remote port, timeout set to " + PORT_TIMEOUT + "s...", VerboseLevel.MEDIUM);

		byte[] respose;

		try {
			respose = baseCommunicator.read(Integer.BYTES, peer, PORT_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			log("Remote peer didn't send port at time.");
			throw e;
		}

		handlePortRead(respose);
	}

	private void writePort() throws IOException {
		writeRawBytes(Utils.intToBytes(peer.getPort()));
	}

	private boolean shouldReadPort() {
		return remotePort == -1;
	}

	private void handlePortRead(byte[] portBytes) throws IOException, GeneralSecurityException {
		log("Reading remote port...", VerboseLevel.MEDIUM);
		remotePort = Utils.bytesToInt(portBytes);
		log("Remote port loaded, connecting to " + remotePort + "...", VerboseLevel.LOW);

		connect(new InetSocketAddress(remoteAddress, remotePort));
	}

	private void onRead(byte[] bytes) {
		if (!isConnected())
			return;

		try {
			messager.onReceive(reader.read(bytes));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | IOException e) {
			Main.handleException(e, peer.getName());
		}
	}

	public void onAuth() throws PacketWriteException {
		log("Authenticacion process done, ciphering I/O streams using " + CipheredCommunicator.AES_KEY_ALGORITHM + "-" + CipheredCommunicator.AES_KEY_SIZE
				+ "...", VerboseLevel.MEDIUM);

		cipheredCommunicator = new CipheredCommunicator(baseCommunicator, sessionCipher, peer);
		reader = new PacketReader();
		messager = new PacketMessager(this, cipheredCommunicator);
		idController.setMessager(messager);
		
		log("Initializing connection module manager and loading modules...", VerboseLevel.LOW);
		
		moduleManager = new ModuleManager(this);
		moduleManager.initializeModules();

		log("Session ciphering done, communicator ciphered and packet messager set.", VerboseLevel.LOW);

		messager.setupEventHandler();

		idController.startIdentification();
	}

	public void onPairCompleted() {
		moduleManager.enable();
	}

	private void startAuthentication() throws IOException, GeneralSecurityException {
		sessionCipher.sendAuthentication();
	}

	public int sendPacket(Packet packet) throws PacketWriteException {
		return sendPacket(packet, null);
	}

	public int sendPacket(Packet packet, PacketAction action) throws PacketWriteException {
		return sendPacket(packet, action, -1L, null);
	}

	public int sendPacket(Packet packet, PacketAction action, long timeout, TimeUnit timeUnit) throws PacketWriteException {
		return messager.sendPacket(packet, action, timeout, timeUnit);
	}

	public void writeRawBytes(byte[] bytes) throws IOException {
		baseCommunicator.write(bytes);
	}

	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * When called, @ConnectedPeer instance is set, and paring is done.
	 */
	public void setConnectedPeer(ConnectedPeer cPeer) {
		if (connectedPeer != null)
			throw new ConnectionIdentificationException("ConnectedPeer instance is already set");

		connectedPeer = cPeer;
		LOGGER_PREFIX = LOGGER_PREFIX.replaceFirst("-", connectedPeer.getName());
	}

	public ConnectedPeer getConnectedPeer() {
		return connectedPeer;
	}

	public UUID getUUID() {
		return idController.getUUID();
	}

	public int getPort() {
		return remotePort;
	}

	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}

	public Peer getPeer() {
		return peer;
	}

	public IdentificationController getIdentificationController() {
		return idController;
	}

	public ModuleManager getModuleManager() {
		return moduleManager;
	}

	private void onException(Exception ex) throws IOException {
		Main.handleException(ex, peer.getName());
		disconnect(true);
	}

	public void disconnect(boolean silent) {
		if (!isConnected())
			return;

		log("Disabling connection modules...", VerboseLevel.MEDIUM);
		moduleManager.disable();
		
		log("Disconnecting from peer...", VerboseLevel.LOW);
		connected.set(false);

		log("Unregistering listen events...", VerboseLevel.HIGH);
		messager.stopEventHandler();

		if (listenTask != null) {
			log("Stopping listening task...", VerboseLevel.MEDIUM);
			listenTask.cancelNow();
		}

		if (!silent) {
			log("Sending disconnect message to remote peer...", VerboseLevel.HIGH);
			try {
				messager.sendStandardPacket(new PacketShutdown());
			} catch (PacketWriteException e) {
				log("Could not send disconnect message: " + e.getMessage(), VerboseLevel.HIGH);
			}
		}

		log("Stopping communicator pool and closing I/O streams...", VerboseLevel.MEDIUM);
		cipheredCommunicator.closeQuietly();

		if (hostSocket != null) {
			log("Closing host socket...", VerboseLevel.MEDIUM);
			try {
				hostSocket.close();
			} catch (IOException e) {
			}
		}

		peer.getConnectionManager().removeConnection(getUUID());

		log("Disconnected successfully.");
	}

	public void log(String str) {
		peer.log(LOGGER_PREFIX + str);
	}

	public void log(String str, VerboseLevel level) {
		peer.log(LOGGER_PREFIX + str, level);
	}
}
