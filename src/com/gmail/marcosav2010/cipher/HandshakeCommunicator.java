package com.gmail.marcosav2010.cipher;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.BaseCommunicator;
import com.gmail.marcosav2010.tasker.TaskOwner;

/**
 * This communicator encripts data using AES and a key, only used for handshake to guarantee some
 * security
 * 
 * @author Marcos
 *
 */
public class HandshakeCommunicator extends BaseCommunicator {

	private static final int LENGTH_BYTES = Integer.BYTES;
	private static final String CIPHER_SYMMETRIC_ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final String KEY_ALGORITHM = "AES";

	private final Cipher inCipher, outCipher;
	private boolean in, out;

	public HandshakeCommunicator(BaseCommunicator baseCommunicator) throws IOException {
		try {
			inCipher = Cipher.getInstance(CIPHER_SYMMETRIC_ALGORITHM);
			outCipher = Cipher.getInstance(CIPHER_SYMMETRIC_ALGORITHM);

			setIn(baseCommunicator.getIn());
			setOut(baseCommunicator.getOut());

		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new IOException(e);
		}
	}

	public void setIn(byte[] key, byte[] iv) throws IOException {
		IvParameterSpec ivParameterSpecIn = new IvParameterSpec(iv);

		SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
		try {
			inCipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpecIn);
			in = true;
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}
	}

	public void setOut(byte[] key, byte[] iv) throws IOException {
		IvParameterSpec ivParameterSpecOut = new IvParameterSpec(iv);

		SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
		try {
			outCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpecOut);
			out = true;
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}
	}

	public synchronized byte[] read(int bytes) throws IOException {
		if (!in)
			return super.read(bytes);

		try {
			byte[] lengthBytes = super.read(LENGTH_BYTES);
			if (lengthBytes.length < 0)
				return null;
			
			int length = Utils.bytesToInt(lengthBytes);

			if (length < 0)
				return null;

			byte[] encryptedData = super.read(length);

			return inCipher.doFinal(encryptedData);

		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IOException(e);
		}
	}

	public byte[] read(int bytes, TaskOwner taskOwner, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return taskOwner.getExecutorService().submit(() -> read(bytes)).get(timeout, unit);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		if (!out) {
			super.write(bytes);
			return;
		}

		try {
			byte[] b = outCipher.doFinal(bytes);
			super.write(Utils.intToBytes(b.length));
			super.write(b);

		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new IOException(e);
		}
	}
}
