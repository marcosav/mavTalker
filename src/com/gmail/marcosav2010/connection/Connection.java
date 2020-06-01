package com.gmail.marcosav2010.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.Arrays;
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
import com.gmail.marcosav2010.communicator.module.ModuleScope;
import com.gmail.marcosav2010.communicator.packet.AbstractPacket;
import com.gmail.marcosav2010.communicator.packet.Packet;
import com.gmail.marcosav2010.communicator.packet.handling.PacketMessager;
import com.gmail.marcosav2010.communicator.packet.packets.PacketShutdown;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketReadException;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketReader;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.handshake.HandshakeAuthentificator;
import com.gmail.marcosav2010.handshake.HandshakeAuthentificator.ConnectionToken;
import com.gmail.marcosav2010.handshake.HandshakeCommunicator;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.ConnectedPeer;
import com.gmail.marcosav2010.peer.Peer;
import com.gmail.marcosav2010.tasker.Task;

import lombok.Getter;

/**
 * Represents the connection between two @Peer
 * 
 * @author Marcos
 */
public class Connection extends NetworkConnection implements ModuleScope {

	private static final long AUTH_TIMEOUT = 20L;
	private static final long HP_TIMEOUT = 3L;
	private static final int SOCKET_CONNECT_TIMEOUT = 7000;
	private static final long REMOTE_CONNECT_BACK_TIMEOUT = 10L;
	private static final byte[] DISCONNECT_REQUEST_BYTES = new byte[] { 123, -123 };

	@Getter
	private final Log log;

	@Getter
	private Peer peer;
	@Getter
	private ConnectedPeer connectedPeer;

	private Socket hostSocket;

	private AtomicBoolean connected;

	private Task listenTask;
	private Task remoteConnectBackTimeout;

	@Getter
	private IdentificationController identificationController;

	@Getter
	private int remotePort;
	private InetAddress remoteAddress;

	private BaseCommunicator baseCommunicator;

	private SessionCipher sessionCipher;
	private CipheredCommunicator cipheredCommunicator;

	private PacketReader reader;
	private PacketMessager messager;

	@Getter
	private ModuleManager moduleManager;

	public Connection(Peer peer) {
		this.peer = peer;
		log = new Log(peer, "");
		updateLogPrefix(peer.getAndInc());
		connected = new AtomicBoolean(false);
		identificationController = new IdentificationController(this);
		init();
	}

	Connection(Peer peer, UUID uuid) {
		this(peer);
		identificationController = new IdentificationController(this, uuid);
		init();
	}

	private void init() {
		remotePort = -1;
		sessionCipher = SessionCipher.create(this);
		baseCommunicator = new BaseCommunicator();
		moduleManager = new ModuleManager(this);
		moduleManager.initializeModules();
	}

	private void updateLogPrefix(Object prefix) {
		log.setPrefix("Connection] [" + prefix.toString());
	}

	public void connect(Socket remoteSocket, ConnectionToken ct) throws IOException {
		connected.set(true);
		cancelConnectBackTimeout();

		remoteAddress = remoteSocket.getInetAddress();

		if (ct != null) {
			log.log("Setting handshake ciphered communicator input key...", VerboseLevel.HIGH);

			HandshakeCommunicator hCommunicator;
			if (baseCommunicator instanceof HandshakeCommunicator)
				hCommunicator = (HandshakeCommunicator) baseCommunicator;
			else
				hCommunicator = new HandshakeCommunicator(baseCommunicator);

			hCommunicator.setIn(ct.getBaseKey(), ct.getHandshakeKey());

			baseCommunicator = hCommunicator;
		}

		log.log("Setting input stream...", VerboseLevel.MEDIUM);
		baseCommunicator.setIn(remoteSocket.getInputStream());

		try {
			listenForAuth();
			listenForHandshakeKeyPortAndConnect();

		} catch (Exception e) {
			log.log(e);
			disconnect(true);
			return;
		}

		log.log("Setup completed, now executing listen task...", VerboseLevel.LOW);
		listenTask = Main.getInstance().getTasker().run(peer, listenSocket()).setName("Listen Task");
	}

	private Runnable listenSocket() {
		return () -> {
			while (connected.get()) {
				EncryptedMessage read;

				try {
					read = cipheredCommunicator.read();

					cipheredCommunicator.decrypt(read, this::onRead);

				} catch (IOException e) {
					if (!connected.get())
						return;

					log.log(e, "Connection lost unexpectedly: " + e.getMessage());
					disconnect(true);
					return;

				} catch (Exception e) {
					log.log(e, "An unknown exception has ocurred: " + e.getMessage());
					continue;
				}
			}
		};
	}

	public void connect(InetSocketAddress address) throws IOException, GeneralSecurityException {
		if (address == null)
			throw new IllegalArgumentException("Address cannot be null.");

		if (!(hostSocket == null || hostSocket.isBound()))
			throw new IllegalStateException("Already connected.");

		if ((address.getAddress().equals(Main.getInstance().getPublicAddress())
				|| address.getAddress().getHostName().equals(InetAddress.getLocalHost().getHostName()))
				&& address.getPort() == peer.getPort())
			throw new IllegalStateException("Cannon connect to itself.");

		log.log("Connecting to " + address.toString() + "...");

		Socket connectingSocket = new Socket();
		connectingSocket.connect(address, SOCKET_CONNECT_TIMEOUT);

		log.log("Setting host socket...", VerboseLevel.MEDIUM);

		hostSocket = connectingSocket;
		remotePort = address.getPort();

		log.log("Setting output stream...", VerboseLevel.MEDIUM);
		baseCommunicator.setOut(hostSocket.getOutputStream());

		HandshakeAuthentificator ha = peer.getConnectionManager().getHandshakeAuthentificator();
		ConnectionToken ct = ha.sendHandshake(baseCommunicator, address);

		identificationController.sendTemporaryUUID();

		if (ct != null) {
			log.log("Setting handshake ciphered communicator output key...", VerboseLevel.HIGH);

			HandshakeCommunicator hCommunicator;
			if (baseCommunicator instanceof HandshakeCommunicator)
				hCommunicator = (HandshakeCommunicator) baseCommunicator;
			else
				hCommunicator = new HandshakeCommunicator(baseCommunicator);

			hCommunicator.setOut(ct.getBaseKey(), ct.getHandshakeKey());
			baseCommunicator = hCommunicator;
		}

		log.log("Generating session input cipher using " + SessionCipher.RSA_KEY_ALGORITHM + "-"
				+ SessionCipher.RSA_KEY_SIZE + "...", VerboseLevel.MEDIUM);
		sessionCipher.generate();

		log.log("Starting authentification...", VerboseLevel.MEDIUM);
		startAuthentication();

		if (!isConnected()) {
			log.log("Providing handshake key and port to connect...", VerboseLevel.MEDIUM);
			writeHandshakeKeyAndPort();
			startConnectTimeout();
		}

		log.log("Connected to remote, please wait...");
	}

	private void listenForAuth() throws ConnectionException, PacketWriteException, GeneralSecurityException {
		if (!sessionCipher.isWaitingForRemoteAuth())
			return;

		log.log("Waiting for remote authentication, timeout set to " + AUTH_TIMEOUT + "s...", VerboseLevel.MEDIUM);

		byte[] respose;

		try {
			respose = baseCommunicator.read(SessionCipher.RSA_KEY_MSG, peer, AUTH_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new ConnectionException("Remote peer didn't send authentication at time, aborting.");

		} catch (ExecutionException | InterruptedException e) {
			throw new ConnectionException("There was an error while reading authentication.", e);
		}

		log.log("Loading authentication respose...", VerboseLevel.LOW);
		sessionCipher.loadAuthenticationRespose(respose);
	}

	private void listenForHandshakeKeyPortAndConnect()
			throws ConnectionException, IOException, GeneralSecurityException {
		if (!shouldReadPort())
			return;

		log.log("Waiting for remote handshake key and port, timeout set to " + HP_TIMEOUT + "s...",
				VerboseLevel.MEDIUM);

		byte[] respose;

		try {
			respose = baseCommunicator.read(
					Integer.BYTES + HandshakeAuthentificator.H_KEY_LENGTH + HandshakeAuthentificator.B_KEY_LENGTH, peer,
					HP_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new ConnectionException("Remote peer didn't send handshake key and port at time, aborting.");

		} catch (InterruptedException | ExecutionException e) {
			throw new ConnectionException("There was an error while reading handshake key and port.", e);
		}

		handleHandshakeKeyPortReadAndConnect(respose);
	}

	private void writeHandshakeKeyAndPort() throws IOException {
		var c = peer.getConnectionManager().getHandshakeAuthentificator().generateTemporalHandshakeKey();

		writeRawBytes(Utils.concat(Utils.intToBytes(peer.getPort()), c.getHandshakeKey(), c.getBaseKey()));
	}

	private void startConnectTimeout() {
		log.log("Waiting for remote connection back, timeout set to " + REMOTE_CONNECT_BACK_TIMEOUT + "s...",
				VerboseLevel.MEDIUM);

		remoteConnectBackTimeout = Main.getInstance().getTasker().schedule(peer, () -> {
			log.log("Remote peer didn't connect back at time, stopping connection.");

			try {
				writeRawBytes(DISCONNECT_REQUEST_BYTES); // Try to send disconnect request to avoid unnecesary a future
															// connection try
			} catch (IOException e) {
			}

			disconnect(true, true);

		}, REMOTE_CONNECT_BACK_TIMEOUT, TimeUnit.SECONDS);
	}

	private void cancelConnectBackTimeout() {
		if (remoteConnectBackTimeout != null) {
			remoteConnectBackTimeout.cancel();
			try {
				writeRawBytes(new byte[DISCONNECT_REQUEST_BYTES.length]); // With this we avoid a comunicator
																			// asynchronization
			} catch (IOException e) {
			}
		}
	}

	private boolean shouldReadPort() {
		return remotePort == -1;
	}

	private void handleHandshakeKeyPortReadAndConnect(byte[] bytes)
			throws IOException, GeneralSecurityException, ConnectionException {
		log.log("Reading remote port and handshake...", VerboseLevel.MEDIUM);

		byte[][] sBytes = Utils.split(bytes, Integer.BYTES);
		remotePort = Utils.bytesToInt(sBytes[0]);

		byte[][] hbBytes = Utils.split(sBytes[1], HandshakeAuthentificator.H_KEY_LENGTH);

		InetSocketAddress address = new InetSocketAddress(remoteAddress, remotePort);
		peer.getConnectionManager().getHandshakeAuthentificator().storeHandshakeKey(address, hbBytes[0], hbBytes[1]);

		log.log("Read remote port " + remotePort + " and handshake key.", VerboseLevel.MEDIUM);

		// We wait 500ms for a disconnect request, usually send if connection back has
		// token too much time
		try {
			if (Arrays.equals(baseCommunicator.read(DISCONNECT_REQUEST_BYTES.length, peer, 500l, TimeUnit.MILLISECONDS),
					DISCONNECT_REQUEST_BYTES))
				throw new ConnectionException("Got remote disconnect request, aborting...");

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
		}

		connect(address);
	}

	private void onRead(byte[] bytes) {
		if (!isConnected())
			return;

		AbstractPacket p;

		try {
			p = reader.read(bytes);

		} catch (PacketReadException e) {
			log.log(e, "There was an error while reading bytes.");
			return;
		}

		messager.onReceive(p);
	}

	public void onAuth() throws PacketWriteException {
		log.log("Authenticacion process done, ciphering I/O streams using " + CipheredCommunicator.AES_KEY_ALGORITHM
				+ "-" + CipheredCommunicator.AES_KEY_SIZE + "...", VerboseLevel.MEDIUM);

		cipheredCommunicator = new CipheredCommunicator(baseCommunicator, sessionCipher, peer);
		reader = new PacketReader();
		messager = new PacketMessager(this, cipheredCommunicator);
		identificationController.setMessager(messager);

		log.log("Session ciphering done, communicator ciphered and packet messager set.", VerboseLevel.LOW);

		identificationController.startIdentification();
	}

	public void onPairCompleted() {
		messager.setupEventHandler();

		moduleManager.onEnable();

		log.log("Connection completed.");
	}

	private void startAuthentication() throws IOException, GeneralSecurityException {
		sessionCipher.sendAuthentication();
	}

	public int sendPacket(Packet packet) throws PacketWriteException {
		return sendPacket(packet, null);
	}

	public int sendPacket(Packet packet, Runnable action) throws PacketWriteException {
		return sendPacket(packet, action, null, -1L, null);
	}

	public int sendPacket(Packet packet, Runnable action, Runnable onTimeOut, long timeout, TimeUnit timeUnit)
			throws PacketWriteException {
		return messager.sendPacket(packet, action, onTimeOut, timeout, timeUnit);
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
			throw new ConnectionIdentificationException("ConnectedPeer instance is already set.");

		connectedPeer = cPeer;
		log.log("Setting connection tag to " + connectedPeer.getName(), VerboseLevel.MEDIUM);
		updateLogPrefix(connectedPeer.getName());
	}

	public UUID getUUID() {
		return identificationController.getUUID();
	}

	public void disconnect(boolean silent) {
		disconnect(silent, false);
	}

	public void disconnect(boolean silent, boolean force) {
		if (!force && !isConnected())
			return;

		if (peer.getModuleManager() != null) {
			log.log("Disabling connection modules...", VerboseLevel.MEDIUM);
			moduleManager.onDisable();
		}

		log.log("Disconnecting from peer...", VerboseLevel.LOW);
		connected.set(false);

		if (messager != null) {
			log.log("Unregistering listen events...", VerboseLevel.HIGH);
			messager.stopEventHandler();
		}

		if (listenTask != null) {
			log.log("Stopping listening task...", VerboseLevel.MEDIUM);
			listenTask.cancelNow();
		}

		if (!silent) {
			log.log("Sending disconnect message to remote peer...", VerboseLevel.HIGH);
			try {
				messager.sendStandardPacket(new PacketShutdown());
			} catch (PacketWriteException e) {
				log.log("Could not send disconnect message: " + e.getMessage(), VerboseLevel.HIGH);
			}
		}

		if (cipheredCommunicator != null) {
			log.log("Stopping communicator pool and closing I/O streams...", VerboseLevel.MEDIUM);
			cipheredCommunicator.closeQuietly();
		}

		if (baseCommunicator != null) {
			baseCommunicator.closeQuietly();
		}

		if (hostSocket != null) {
			log.log("Closing host socket...", VerboseLevel.MEDIUM);
			try {
				hostSocket.close();
			} catch (IOException e) {
			}
		}

		peer.getConnectionManager().removeConnection(getUUID());

		log.log("Disconnected successfully.");
	}
}
