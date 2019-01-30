package com.gmail.marcosav2010.cipher;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;

/**
 * This class manages the asymmetric encryption between peers.
 * 
 * @author Marcos
 *
 */
public class SessionCipher {

	public static final String RSA_KEY_ALGORITHM = "RSA";
	public static final int RSA_KEY_SIZE = 4096;
	public static final int RSA_KEY_MSG = SessionCipher.RSA_KEY_SIZE / Byte.SIZE + 38;

	private Connection connection;

	private PublicKey publicKey;

	private CipherPool in, out;

	private boolean isAuth;

	private SessionCipher(Connection connection) {
		this.connection = connection;
		isAuth = false;
	}

	public void generate() throws GeneralSecurityException {
		KeyPairGenerator kPairGen = KeyPairGenerator.getInstance(RSA_KEY_ALGORITHM);
		kPairGen.initialize(RSA_KEY_SIZE);
		KeyPair keyPair = kPairGen.generateKeyPair();

		in = new CipherPool(Cipher.PRIVATE_KEY, keyPair.getPrivate(), 2, 15);
		publicKey = keyPair.getPublic();
	}

	public void sendAuthentication() throws IOException, GeneralSecurityException {
		byte[] keyBytes = publicKey.getEncoded();
		connection.writeRawBytes(keyBytes);
		isAuth = true;
		checkDone();
	}

	public void loadAuthenticationRespose(byte[] bytes) throws GeneralSecurityException, PacketWriteException {
		PublicKey remotePublicKey = getPublicKey(bytes, RSA_KEY_ALGORITHM);

		out = new CipherPool(Cipher.PUBLIC_KEY, remotePublicKey, 1, 10);

		checkDone();
	}

	public boolean isWaitingForRemoteAuth() {
		return out == null;
	}

	public boolean isDone() {
		return isAuth && !isWaitingForRemoteAuth();
	}

	private void checkDone() throws PacketWriteException {
		if (isDone())
			connection.onAuth();
	}

	public byte[] decode(byte[] bytes) throws GeneralSecurityException, InterruptedException {
		if (in == null)
			throw new IllegalStateException("There is no decoding cipher pool created.");

		return in.doFinal(bytes);
	}

	public byte[] encode(byte[] bytes) throws GeneralSecurityException, InterruptedException {
		if (out == null)
			throw new IllegalStateException("There is no encoding cipher pool created.");

		return out.doFinal(bytes);
	}

	private static PublicKey getPublicKey(byte[] key, String algorithm) throws InvalidKeySpecException, NoSuchAlgorithmException {
		return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(key));
	}

	public static SessionCipher create(Connection connection) {
		return new SessionCipher(connection);
	}
}
