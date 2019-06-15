package com.gmail.marcosav2010.handshake;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.BaseCommunicator;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.peer.Peer;

/**
 * This clase manages the handshake of a starting @Connection
 * 
 * @author Marcos
 */
public class HandshakeAuthentificator {

	private static final long HANDSHAKE_TIMEOUT = 10L;

	private static final int C_KEY_MARK = 3;
	private static final int AK_KEY_MARK = 13;

	private static final int KEY_TYPE_BITMASK = 0b00000111;
	private static final byte PUBLIC_KEY = 0b000;
	private static final byte PRIVATE_KEY = 0b001;

	public static final int B_KEY_LENGTH = 128 / 8;
	public static final int H_KEY_LENGTH = B_KEY_LENGTH;
	private static final int C_KEY_LENGTH = 128 / 8;
	private static final int HOST_LENGTH = Byte.BYTES * 4;
	private static final int PORT_LENGTH = Integer.BYTES;

	private static final byte[] PUBLIC_CONNECTION_KEY = markBytes(new String(new char[C_KEY_LENGTH]).getBytes(), C_KEY_MARK);
	private static final byte[] EMPTY_HANDSHAKE_KEY = new byte[H_KEY_LENGTH];

	private static final String PASSWORD_HASH_ALGORITHM = "SHA3-256";

	private static final int RAW_ADDRESSKEY_LENGTH = HOST_LENGTH + PORT_LENGTH + B_KEY_LENGTH + H_KEY_LENGTH;

	private final SecureRandom random;

	private Peer peer;

	private byte[] connectionKey;
	private Map<String, ConnectionToken> localStorage;
	private Map<String, ConnectionToken> localTempStorage;

	private Map<InetSocketAddress, ConnectionToken> remoteStorage;

	private ConnectionToken publicConnectionToken;
	private String publicAddressKey;

	public HandshakeAuthentificator(Peer peer) {
		this.peer = peer;

		localTempStorage = new ConcurrentHashMap<>();
		localStorage = new ConcurrentHashMap<>();
		remoteStorage = new ConcurrentHashMap<>();
		random = new SecureRandom();
	}

	public synchronized byte[] getConnectionKey() {
		if (connectionKey != null)
			return connectionKey;

		connectionKey = new byte[C_KEY_LENGTH];
		random.nextBytes(connectionKey);

		markBytes(connectionKey, C_KEY_MARK);

		return connectionKey;
	}

	public String getConnectionKeyString() {
		return Utils.encode(getConnectionKey());
	}

	private static byte getMark(byte type) {
		return type;
	}

	public String generatePublicAddressKey()
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

		if (publicAddressKey != null)
			return publicAddressKey;

		publicConnectionToken = new ConnectionToken(generateHandshakeKey(), generateBaseKey());

		return publicAddressKey = generateAddressKey(PUBLIC_CONNECTION_KEY, publicConnectionToken, getMark(PUBLIC_KEY));
	}

	public String generatePrivateAddressKey(char[] requesterConnectionKeyC)
			throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

		String requesterConnectionKey = new String(requesterConnectionKeyC);

		if (localStorage.containsKey(requesterConnectionKey))
			throw new IllegalArgumentException("There is already an Address Key for that Connection Key.");

		ConnectionToken ct = new ConnectionToken(generateHandshakeKey(), generateBaseKey());

		localStorage.put(requesterConnectionKey, ct);

		return generateAddressKey(Utils.decode(requesterConnectionKey), ct, getMark(PRIVATE_KEY));
	}

	private String generateAddressKey(byte[] requesterConnectionKeyBytes, ConnectionToken ct, byte mark)
			throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

		if (requesterConnectionKeyBytes.length != C_KEY_LENGTH)
			throw new IllegalArgumentException("Invalid connection key format.");

		checkMark(requesterConnectionKeyBytes, C_KEY_MARK);

		var ip = Main.getInstance().getPublicAddress();
		var ipBytes = ip.getAddress();

		var handshakeKey = ct.getHandshakeKey();

		var baseKey = ct.getBaseKey();

		var addressKeyBytes = ByteBuffer.allocate(RAW_ADDRESSKEY_LENGTH).put(ipBytes).putInt(peer.getPort()).put(baseKey).put(handshakeKey).array();

		Cipher c = Cipher.getInstance("AES");

		byte[] password = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM).digest(requesterConnectionKeyBytes);

		SecretKey secretKey = new SecretKeySpec(password, "AES");
		c.init(Cipher.ENCRYPT_MODE, secretKey);

		var cipheredBytes = c.doFinal(addressKeyBytes);

		var markedBytes = new byte[cipheredBytes.length + 1];
		System.arraycopy(cipheredBytes, 0, markedBytes, 0, AK_KEY_MARK);
		System.arraycopy(cipheredBytes, AK_KEY_MARK, markedBytes, AK_KEY_MARK + 1, cipheredBytes.length - AK_KEY_MARK);
		markedBytes[AK_KEY_MARK] = mark;

		var out = Utils.encode(markedBytes);

		return out;
	}

	public ConnectionToken parseAddressKey(char[] remoteAddressKey)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, UnknownHostException {

		var remoteAddressKeyBytes = Utils.decode(new String(remoteAddressKey));

		var unmarkedBytes = new byte[remoteAddressKeyBytes.length - 1];
		System.arraycopy(remoteAddressKeyBytes, 0, unmarkedBytes, 0, AK_KEY_MARK);
		System.arraycopy(remoteAddressKeyBytes, AK_KEY_MARK + 1, unmarkedBytes, AK_KEY_MARK, unmarkedBytes.length - AK_KEY_MARK);
		byte mark = remoteAddressKeyBytes[AK_KEY_MARK];
		byte type = (byte) (mark & KEY_TYPE_BITMASK);

		return parseAddressKey(unmarkedBytes, type);
	}

	public ConnectionToken parseAddressKey(byte[] remoteAddressKeyBytes, byte type)
			throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, UnknownHostException, InvalidKeyException {

		byte[] connectionKey;
		boolean isPublic = false;

		switch (type) {
		case PRIVATE_KEY:
			connectionKey = getConnectionKey();
			break;
		case PUBLIC_KEY:
			connectionKey = PUBLIC_CONNECTION_KEY;
			isPublic = true;
			break;
		default:
			throw new IllegalArgumentException("Invalid address key type.");
		}

		byte[] parsedIpBytes = new byte[HOST_LENGTH], parsedPortBytes = new byte[PORT_LENGTH], baseKeyBytes = new byte[B_KEY_LENGTH],
				handshakeKeyBytes = new byte[H_KEY_LENGTH];

		var password = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM).digest(connectionKey);
		var secretKey = new SecretKeySpec(password, "AES");

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.DECRYPT_MODE, secretKey);
		var decipheredBytes = c.doFinal(remoteAddressKeyBytes);

		if (decipheredBytes.length != RAW_ADDRESSKEY_LENGTH)
			throw new IllegalArgumentException("Invalid address key format.");

		System.arraycopy(decipheredBytes, 0, parsedIpBytes, 0, HOST_LENGTH);
		System.arraycopy(decipheredBytes, HOST_LENGTH, parsedPortBytes, 0, PORT_LENGTH);
		System.arraycopy(decipheredBytes, PORT_LENGTH + HOST_LENGTH, baseKeyBytes, 0, B_KEY_LENGTH);
		System.arraycopy(decipheredBytes, PORT_LENGTH + HOST_LENGTH + B_KEY_LENGTH, handshakeKeyBytes, 0, H_KEY_LENGTH);

		int p = ByteBuffer.wrap(parsedPortBytes).getInt();

		var address = new InetSocketAddress(InetAddress.getByAddress(parsedIpBytes), p);

		return registerHandshakeKey(address, new ConnectionToken(handshakeKeyBytes, address, baseKeyBytes).setPublic(isPublic));
	}

	public ConnectionToken sendHandshake(BaseCommunicator b, InetSocketAddress a) throws IOException {
		log("Sending handshake to remote...", VerboseLevel.HIGH);

		byte[] hk = EMPTY_HANDSHAKE_KEY, ck = PUBLIC_CONNECTION_KEY;
		ConnectionToken ct = null;

		if (remoteStorage.containsKey(a)) {
			hk = (ct = remoteStorage.remove(a)).getHandshakeKey();

			if (!ct.isPublic())
				ck = getConnectionKey();
		}

		b.write(Utils.concat(ck, hk));

		return ct;
	}

	public ConnectionToken readHandshake(Socket remoteSocket) throws InterruptedException, ExecutionException, TimeoutException, InvalidHandshakeKey {
		log("Waiting for handshake, timeout set to " + HANDSHAKE_TIMEOUT + "s...", VerboseLevel.HIGH);

		byte[] b = new byte[C_KEY_LENGTH + H_KEY_LENGTH];
		peer.getExecutorService().submit(() -> remoteSocket.getInputStream().read(b)).get(HANDSHAKE_TIMEOUT, TimeUnit.SECONDS);

		return handle(b);
	}

	private ConnectionToken handle(byte[] b) throws InvalidHandshakeKey {
		byte[] connectionKeyBytes = new byte[C_KEY_LENGTH], handshakeKey = new byte[H_KEY_LENGTH];
		System.arraycopy(b, 0, connectionKeyBytes, 0, C_KEY_LENGTH);
		System.arraycopy(b, C_KEY_LENGTH, handshakeKey, 0, H_KEY_LENGTH);

		if (getHRL() == HandshakeRequirementLevel.NONE)
			if (Arrays.equals(EMPTY_HANDSHAKE_KEY, handshakeKey) && Arrays.equals(PUBLIC_CONNECTION_KEY, connectionKeyBytes))
				return null;
		
		if (getHRL().compareTo(HandshakeRequirementLevel.PUBLIC) <= 0)
			if (publicConnectionToken != null && Arrays.equals(PUBLIC_CONNECTION_KEY, connectionKeyBytes)
					&& Arrays.equals(publicConnectionToken.getHandshakeKey(), handshakeKey))
				return publicConnectionToken;

		String connectionKey = Utils.encode(connectionKeyBytes);

		if (localStorage.containsKey(connectionKey) && Arrays.equals(localStorage.get(connectionKey).getHandshakeKey(), handshakeKey))
			return localStorage.remove(connectionKey);

		String hKey = Utils.encode(handshakeKey);

		if (localTempStorage.containsKey(hKey))
			return localTempStorage.remove(hKey);

		throw new InvalidHandshakeKey();
	}

	public ConnectionToken generateTemporalHandshakeKey() {
		var handshakeKey = generateHandshakeKey();

		ConnectionToken ct = new ConnectionToken(handshakeKey, generateBaseKey());

		localTempStorage.put(ct.getHandshakeKeyAsString(), ct);

		return ct;
	}

	private byte[] generateBaseKey() {
		var baseKey = new byte[B_KEY_LENGTH];
		random.nextBytes(baseKey);

		return baseKey;
	}

	private byte[] generateHandshakeKey() {
		var handshakeKey = new byte[H_KEY_LENGTH];
		random.nextBytes(handshakeKey);

		return handshakeKey;
	}

	private ConnectionToken registerHandshakeKey(InetSocketAddress address, ConnectionToken token) {
		remoteStorage.put(address, token);
		return token;
	}

	public void storeHandshakeKey(InetSocketAddress address, byte[] handshakeKeyBytes, byte[] baseKeyBytes) {
		registerHandshakeKey(address, new ConnectionToken(handshakeKeyBytes, baseKeyBytes));
	}

	public HandshakeRequirementLevel getHRL() {
		return peer.getProperties().getHRL();
	}

	private static byte[] markBytes(byte[] bytes, int pos) {
		byte count = Byte.MIN_VALUE;
		for (int i = 0; i < bytes.length; i++)
			if (i != pos)
				count += Integer.bitCount(Byte.toUnsignedInt(bytes[i]));

		bytes[pos] = count;

		return bytes;
	}

	private static void checkMark(byte[] bytes, int pos) {
		byte count = Byte.MIN_VALUE;
		for (int i = 0; i < bytes.length; i++)
			if (i != pos)
				count += Integer.bitCount(Byte.toUnsignedInt(bytes[i]));

		if (count != bytes[pos])
			throw new IllegalArgumentException("Invalid connection key format.");
	}

	public static class ConnectionToken {

		private byte[] handshakeKey, baseKey;
		private InetSocketAddress address;
		private String handshakeKeyStr;
		private boolean isPublic = false;

		public ConnectionToken(byte[] handshakeKey) {
			this.handshakeKey = handshakeKey;
			handshakeKeyStr = Utils.encode(handshakeKey);
		}

		public ConnectionToken(byte[] handshakeKey, byte[] baseKey) {
			this(handshakeKey);
			this.baseKey = baseKey;
		}

		public ConnectionToken(byte[] handshakeKey, InetSocketAddress address, byte[] baseKey) {
			this(handshakeKey, baseKey);
			this.address = address;
		}

		public byte[] getHandshakeKey() {
			return handshakeKey;
		}

		public byte[] getBaseKey() {
			return baseKey;
		}

		public String getHandshakeKeyAsString() {
			return handshakeKeyStr;
		}

		public InetSocketAddress getAddress() {
			return address;
		}

		public boolean isPublic() {
			return isPublic;
		}

		public ConnectionToken setPublic(boolean b) {
			isPublic = b;
			return this;
		}
	}

	public static class InvalidHandshakeKey extends Exception {

		private static final long serialVersionUID = -1705689877146623444L;

		public InvalidHandshakeKey() {
			super("Invalid handshake key");
		}
	}

	public static enum HandshakeRequirementLevel {

		NONE, PUBLIC, PRIVATE;
	}

	public void log(String str) {
		peer.log("[HA] " + str);
	}

	public void log(String str, VerboseLevel level) {
		peer.log("[HA] " + str, level);
	}
}
