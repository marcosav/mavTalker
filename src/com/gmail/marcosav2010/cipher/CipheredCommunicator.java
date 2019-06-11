package com.gmail.marcosav2010.cipher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.BaseCommunicator;
import com.gmail.marcosav2010.communicator.Communicator;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.tasker.Task;
import com.gmail.marcosav2010.tasker.TaskOwner;

/**
 * This is a communicator that encrypts byte arrays with a random symmetric key under @SessionCipher
 * asymmetric encryption, it also handles other message parameters.
 * 
 * @author Marcos
 *
 */
public class CipheredCommunicator extends Communicator {

	private static final String CIPHER_SYMMETRIC_ALGORITHM = "AES/GCM/PKCS5Padding";

	private static final int IV_SIZE = 96;
	private static final int TAG_LENGTH = 128;
	public static final String AES_KEY_ALGORITHM = "AES";
	public static final int AES_KEY_SIZE = 256;

	private static final int AES_KEY_BYTES = AES_KEY_SIZE / Byte.SIZE;

	private static final int LENGTH_BYTES = Integer.BYTES;

	private static final int AES_DATA_SIZE = AES_KEY_BYTES + IV_SIZE/* + LENGTH_BYTES */; // Para AES256: 32 + 96 + 4 = 132
	private static final int RSA_MSG_SIZE = SessionCipher.RSA_KEY_SIZE / Byte.SIZE;

	private final SessionCipher sessionCipher;

	private final TaskOwner taskOwner;

	private Task writeTask;
	private WritePool writePool;

	public CipheredCommunicator(BaseCommunicator baseCommunicator, SessionCipher sessionCipher, TaskOwner taskOwner) {
		setIn(new BufferedInputStream(baseCommunicator.getIn()));
		setOut(new BufferedOutputStream(baseCommunicator.getOut()));

		this.sessionCipher = sessionCipher;
		this.taskOwner = taskOwner;
		start();
	}

	private void start() {
		writeTask = Main.getInstance().getTasker().run(taskOwner, writePool = new WritePool());
	}

	public synchronized EncryptedMessage read() throws IOException {
		// Leer tamaño del mensaje cifrado en complemento
		
		byte[] lengthBytes = getIn().readNBytes(LENGTH_BYTES);
		if (lengthBytes.length <= 0)
			return null;
		
		int length = ~Utils.bytesToInt(lengthBytes);
		if (length < 0)
			return null;

		// Leer parte RSA donde esta la llave AES
		byte[] encryptedAESData = getIn().readNBytes(RSA_MSG_SIZE);

		// Leer mensaje encriptado con AES de longitud dada antes
		byte[] encryptedData = getIn().readNBytes(length);

		return new EncryptedMessage(encryptedAESData, encryptedData);
	}

	public EncryptedMessage read(TaskOwner taskOwner, long timeout, TimeUnit unit)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		return taskOwner.getExecutorService().submit(() -> read()).get(timeout, unit);
	}

	public void decrypt(EncryptedMessage encryptedMessage, Consumer<byte[]> onDecrypt) {
		if (encryptedMessage == null)
			return;

		Main.getInstance().getTasker().run(taskOwner, () -> {
			try {
				byte[] simmetricKeyBytes = sessionCipher.decode(encryptedMessage.getEncryptedSimmetricKeyBytes());

				var simmetricKeyData = deconstructAESData(simmetricKeyBytes);

				byte[] decryptedSimmetricKey = simmetricKeyData[0];
				byte[] decryptedSimmetricKeyIV = simmetricKeyData[1];

				SecretKey symmetricKey = getSecretKey(decryptedSimmetricKey, AES_KEY_ALGORITHM);
				GCMParameterSpec gcmPS = getGCMParameterSpec(decryptedSimmetricKeyIV);

				onDecrypt.accept(getCipher(Cipher.DECRYPT_MODE, symmetricKey, gcmPS).doFinal(encryptedMessage.getEncryptedData()));

			} catch (GeneralSecurityException | InterruptedException e) {
				Logger.log(e);
			}
		});
	}

	private class WritePool implements Runnable {

		private BlockingQueue<EncryptedMessage> queue = new LinkedBlockingQueue<>();
		private AtomicBoolean write = new AtomicBoolean(true);

		public void run() {
			while (write.get())
				try {
					write(queue.take());
				} catch (IOException e) {
					Logger.log(e);
				} catch (InterruptedException e) {
				}
		}

		public void queue(EncryptedMessage msg) {
			queue.offer(msg);
		}

		private void write(EncryptedMessage msg) throws IOException {
			getOut().write(msg.byteLength());
			getOut().write(msg.getEncryptedSimmetricKeyBytes());
			getOut().write(msg.getEncryptedData());
			getOut().flush();
		}

		public void cancel() {
			write.set(false);
			queue.clear();
		}
	}

	@Override
	public void write(byte[] bytes) {
		try {
			writePool.queue(encrypt(bytes));

		} catch (GeneralSecurityException | InterruptedException ex) {
			Logger.log(ex);
		}
	}

	public EncryptedMessage encrypt(byte[] bytes) throws GeneralSecurityException, InterruptedException {
		GCMParameterSpec gcmPS = generateGCMParameterSpec();
		byte[] iv = gcmPS.getIV();
		SecretKey symmetricKey = generateAESSecretKey();

		byte[] encryptedData = getCipher(Cipher.ENCRYPT_MODE, symmetricKey, gcmPS).doFinal(bytes);

		byte[] aesDataToRSAEncrypt = constructAESData(symmetricKey.getEncoded(), iv);
		byte[] encryptedAESData = sessionCipher.encode(aesDataToRSAEncrypt);

		return new EncryptedMessage(encryptedAESData, encryptedData);
	}

	public void beforeClose() {
		writePool.cancel();
		writeTask.cancelNow();
	}

	private static SecretKey generateAESSecretKey() throws NoSuchAlgorithmException {
		KeyGenerator symmetricKeyGen = KeyGenerator.getInstance(AES_KEY_ALGORITHM);
		symmetricKeyGen.init(AES_KEY_SIZE, new SecureRandom());
		return symmetricKeyGen.generateKey();
	}

	private static GCMParameterSpec generateGCMParameterSpec() {
		byte[] iv = new byte[IV_SIZE];
		new SecureRandom().nextBytes(iv);
		return new GCMParameterSpec(TAG_LENGTH, iv);
	}

	private static Cipher getCipher(int mode, Key key, GCMParameterSpec gcmParameterSpec) throws GeneralSecurityException {
		Cipher c = Cipher.getInstance(CIPHER_SYMMETRIC_ALGORITHM);
		c.init(mode, key, gcmParameterSpec);
		return c;
	}

	private static GCMParameterSpec getGCMParameterSpec(byte[] iv) {
		return new GCMParameterSpec(TAG_LENGTH, iv);
	}

	private static SecretKey getSecretKey(byte[] key, String algorithm) {
		return new SecretKeySpec(key, 0, key.length, algorithm);
	}

	private static byte[] constructAESData(byte[] simKey, byte[] iv/* , byte[] length */) {
		byte[] out = new byte[AES_DATA_SIZE];

		System.arraycopy(simKey, 0, out, 0, AES_KEY_BYTES);
		System.arraycopy(iv, 0, out, AES_KEY_BYTES, IV_SIZE);
		// System.arraycopy(length, 0, out, AES_KEY_BYTES + IV_SIZE, LENGTH_BYTES);

		return out;
	}

	private static byte[][] deconstructAESData(byte[] b) {
		byte[] keyBytes = new byte[AES_KEY_BYTES];
		byte[] ivBytes = new byte[IV_SIZE];
		// byte[] lengthBytes = new byte[LENGTH_BYTES];

		System.arraycopy(b, 0, keyBytes, 0, AES_KEY_BYTES);
		System.arraycopy(b, AES_KEY_BYTES, ivBytes, 0, IV_SIZE);
		// System.arraycopy(b, AES_KEY_BYTES + IV_SIZE, lengthBytes, 0, LENGTH_BYTES);

		return new byte[][] { keyBytes, ivBytes/* , lengthBytes */ };
	}

	@Override
	public void close() throws IOException {
		beforeClose();
		if (getIn() != null)
			getIn().close();

		if (getOut() != null)
			getOut().close();
	}

	@Override
	public void closeQuietly() {
		beforeClose();
		try {
			close();
		} catch (IOException ex) {
		}
	}
}
