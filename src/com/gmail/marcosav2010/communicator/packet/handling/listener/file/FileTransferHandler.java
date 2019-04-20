package com.gmail.marcosav2010.communicator.packet.handling.listener.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.gmail.marcosav2010.common.Utils;
import com.gmail.marcosav2010.communicator.packet.packets.PacketFileAccept;
import com.gmail.marcosav2010.communicator.packet.packets.PacketFileRequest;
import com.gmail.marcosav2010.communicator.packet.packets.PacketFileSend;
import com.gmail.marcosav2010.communicator.packet.wrapper.PacketWriteException;
import com.gmail.marcosav2010.connection.Connection;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;
import com.gmail.marcosav2010.tasker.Task;

/**
 * This class handles file related packets, this includes file sending, downloading and accepting.
 * 
 * @author Marcos
 *
 */
public class FileTransferHandler {

	public static final String DOWNLOAD_FOLDER = "FilesTransferred/";

	public static final short HASH_BITS = 256;
	public static final byte HASH_SIZE = 256 / Byte.SIZE;
	private static final String HASH_ALGORITHM = "SHA-" + HASH_BITS;

	private static final long ACCEPT_TIMEOUT = 20L;
	private static final long RECEIVE_TIMEOUT = 10L;

	public static final int MAX_FILE_SIZE = Integer.MAX_VALUE; // 4 GB
	public static final short BLOCK_LIMIT = (short) (MAX_FILE_SIZE / PacketFileSend.MAX_BLOCK_SIZE);

	public static final int MIN_BLOCK_SIZE = Short.MAX_VALUE * 4; // 128 KB
	public static final int MAX_TASKS = Runtime.getRuntime().availableProcessors() / 2 + 1;

	private Connection connection;

	private Map<Integer, FileReceiveInfo> pendingFileRequests;
	private Map<Integer, FileSendInfo> pendingForAcceptFiles;
	private Map<Integer, FileReceiveInfo> pendingReceiveFiles;
	private Map<Integer, Integer> downloading;

	private Map<Integer, Task> pendingTasks;

	public FileTransferHandler(Connection connection) {
		this.connection = connection;

		/*pendingFileRequests = Collections.synchronizedMap(new HashMap<>());
		pendingForAcceptFiles = Collections.synchronizedMap(new HashMap<>());
		pendingReceiveFiles = Collections.synchronizedMap(new HashMap<>());
		downloading = Collections.synchronizedMap(new HashMap<>());*/
		
		pendingFileRequests = new ConcurrentHashMap<>();
		pendingForAcceptFiles = new ConcurrentHashMap<>();
		pendingReceiveFiles = new ConcurrentHashMap<>();
		downloading = new ConcurrentHashMap<>();

		pendingTasks = new HashMap<>();
	}

	public boolean isPendingRequest(int id) {
		return pendingFileRequests.containsKey(id);
	}

	public boolean isPendingForAccept(int id) {
		return pendingForAcceptFiles.containsKey(id);
	}

	public boolean isPendingReceive(int id) {
		return pendingReceiveFiles.containsKey(id);
	}

	public boolean isDownloading(int id) {
		return downloading.containsKey(id);
	}

	public FileReceiveInfo getRequest(int id) {
		return pendingFileRequests.get(id);
	}

	public void handleRequest(PacketFileRequest pf) {
		int id = pf.getID();
		if (isPendingRequest(id))
			return;

		pendingFileRequests.put(id, new FileReceiveInfo(pf.getName(), pf.getBlocks()));
	}

	public FileDownloadResult handleReceiveFile(PacketFileSend p) {
		int id = p.getFileId();

		removeTask(id);

		if (!isPendingReceive(id))
			return FileDownloadResult.NOT_PENDING_OR_TIMED_OUT;

		FileReceiveInfo info = pendingReceiveFiles.get(id);
		boolean single = info.isSingle();
		int blocks = info.getBlocks();
		int remaining = 0;

		if (!single) {
			remaining = downloading.getOrDefault(id, blocks) - 1;
			downloading.put(id, remaining);
		}

		boolean last = remaining == 0;

		if (last) {
			pendingReceiveFiles.remove(id);
			downloading.remove(id);
		}

		byte[] bytes = p.getBytes();
		byte[] hash = p.getHash();
		ByteBuffer bBuffer = ByteBuffer.wrap(bytes);
		int pointer = p.getPointer();

		try {
			byte[] newHash = getHash(bytes);

			if (!Arrays.equals(hash, newHash))
				return FileDownloadResult.HASH_MISMATCH;

			File file = new File(DOWNLOAD_FOLDER + info.getFileName());
			file.getParentFile().mkdirs();

			try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
				channel.write(bBuffer, pointer);
			}

			double dw = 100D - ((double) remaining / blocks) * 100D;
			if (last)
				log("File #" + id + " \"" + info.getFileName() + "\" has been downloaded successfully.");

			else if (dw % 10 == 0) {
				log(String.format("File #%s \"%s\" %.2f%% downloaded.", id, info.getFileName(), dw));
			}

		} catch (Exception ex) {
			Main.handleException(ex, "FileTransferHandler");
			return FileDownloadResult.WRITE_EXCEPTION;
		}

		if (!last)
			addTask(id, () -> onDownloadTimeout(id), 20L, TimeUnit.SECONDS);

		return FileDownloadResult.SUCCESS;
	}

	public FileSendResult handleAcceptRespose(PacketFileAccept p) {
		int id = p.getFileId();

		removeTask(id);

		if (!isPendingForAccept(id))
			return FileSendResult.NOT_PENDING_OR_TIMED_OUT;

		FileSendInfo info = pendingForAcceptFiles.remove(id);
		if (!connection.isConnected())
			return FileSendResult.CONNECTION_EXPIRED;

		Path path = info.getPath();
		File file = path.toFile();
		int fileBytes = info.getSize();
		int blocks = info.getBlocks();
		int blockSize = info.getBlockSize();

		try {
			checkFile(file, fileBytes);
		} catch (IllegalArgumentException ex) {
			Logger.log(ex);
			return FileSendResult.FILE_EXCEPTION;
		}

		int tasks = blocks < MAX_TASKS ? blocks : MAX_TASKS;
		int blocksPerThread = (int) Math.ceil((double) blocks / tasks);

		log("Computing File #" + id + " \"" + info.getFileName() + "\" send, splitting up in " + blocks + " block(s) [" + Utils.formatSize(blockSize)
				+ "/block], using " + tasks + " tasks(s) [" + blocksPerThread + " block(s)/task] and hashing with " + HASH_ALGORITHM + "...",
				VerboseLevel.HIGH);

		AtomicReference<FileSendResult> exception = new AtomicReference<>();
		ExecutorService execServ = Executors.newFixedThreadPool(tasks);

		long t1 = System.currentTimeMillis();

		try (FileChannel channel = FileChannel.open(info.getPath(), StandardOpenOption.READ)) {
			for (int t = 0; t < tasks; t++) {
				int startBlock = t * blocksPerThread;
				int endBlock = Math.min(startBlock + blocksPerThread, blocks);

				execServ.submit(createSendProcess(channel, fileBytes, startBlock, endBlock, blockSize, exception, id, info));
			}

			execServ.shutdown();
			execServ.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

		} catch (IOException e) {
			Logger.log(e);
			return FileSendResult.READ_EXCEPTION;

		} catch (InterruptedException e) {
			Logger.log(e);
			return FileSendResult.OTHER_EXCEPTION;
		}

		long t2 = System.currentTimeMillis();

		FileSendResult r = exception.get();
		if (r != null)
			return r;

		long elapsed = t2 - t1;
		long speed = fileBytes / elapsed * 1000;
		log("File #" + id + " \"" + info.getFileName() + "\" has been sent successfully (" + elapsed + "ms | " + Utils.formatSize(speed) + "/s).");

		return FileSendResult.SUCCESS;
	}

	private Callable<FileSendResult> createSendProcess(FileChannel channel, int fileBytes, int startBlock, int endBlock, int blockSize,
			AtomicReference<FileSendResult> exception, int id, FileSendInfo info) {
		return () -> {
			for (int i = startBlock; i < endBlock; i++) {
				FileSendResult ex = exception.get();
				if (ex != null)
					return ex;

				int n = i;

				int start = n * blockSize;
				int length = Math.min(fileBytes - start, blockSize);

				ByteBuffer temp = ByteBuffer.allocate(length);

				channel.read(temp, start);

				byte[] block = temp.array();
				byte[] hash;

				try {
					hash = getHash(block);
				} catch (NoSuchAlgorithmException e) {
					Logger.log(e);
					return exception.getAndSet(FileSendResult.HASHING_EXCEPTION);
				}

				if (!connection.isConnected())
					return exception.getAndSet(FileSendResult.CONNECTION_EXPIRED);

				try {
					connection.sendPacket(new PacketFileSend(id, start, block, hash));
				} catch (PacketWriteException e) {
					Logger.log(e);
					return exception.getAndSet(FileSendResult.PACKET_WRITE_EXCEPTION);
				}

				// log("File #" + id + " \"" + info.getFileName() + "\" block #" + n + " sent.", VerboseLevel.HIGH);

			}
			return FileSendResult.SUCCESS;
		};
	}

	
	
	public static FileSendInfo createRequest(File file) {
		int size = (int) file.length();
		int upSizeThreshold = Short.MAX_VALUE * 32 * 200;
		int blocks;
		int blockSize;

		if (size <= MIN_BLOCK_SIZE) {
			blockSize = size;
			blocks = 1;
		} else if (size >= upSizeThreshold) {
			blockSize = PacketFileSend.MAX_BLOCK_SIZE;
			blocks = (int) Math.ceil((double) size / blockSize);
		} else {
			double k = (double) (PacketFileSend.MAX_BLOCK_SIZE - MIN_BLOCK_SIZE) / (upSizeThreshold - MIN_BLOCK_SIZE);
			blockSize = (int) (size * k) + MIN_BLOCK_SIZE;
			blocks = (int) Math.ceil((double) size / blockSize);
		}

		checkFile(file, size);

		Path path = file.toPath();

		return new FileSendInfo(path, size, blocks, blockSize);
	}

	public void sendRequest(FileSendInfo fileInfo) {
		sendRequest(fileInfo, -1, null);
	}

	public void sendRequest(FileSendInfo info, long expireTimeout, TimeUnit timeUnit) {
		PacketFileRequest p = new PacketFileRequest(info);

		int id;

		try {
			id = connection.sendPacket(p);
		} catch (PacketWriteException e) {
			Logger.log(e);
			return;
		}

		pendingForAcceptFiles.put(id, info);

		if (timeUnit == null || expireTimeout < 0) {
			expireTimeout = ACCEPT_TIMEOUT;
			timeUnit = TimeUnit.SECONDS;
		}
		addTask(id, () -> onAcceptTimeout(id), expireTimeout, timeUnit);
	}

	public void acceptRequest(int id) {
		acceptRequest(id, -1, null);
	}

	public void acceptRequest(int id, long receiveTimeout, TimeUnit timeUnit) {
		pendingReceiveFiles.put(id, pendingFileRequests.remove(id));

		try {
			connection.sendPacket(new PacketFileAccept(id));
		} catch (PacketWriteException e) {
			Logger.log(e);
		}

		if (timeUnit == null || receiveTimeout < 0) {
			receiveTimeout = RECEIVE_TIMEOUT;
			timeUnit = TimeUnit.SECONDS;
		}
		addTask(id, () -> onReceiveTimeout(id), receiveTimeout, timeUnit);
	}

	public void rejectRequest(int id) {
		pendingFileRequests.remove(id);
	}

	public static byte[] getHash(byte[] bytes) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
		return md.digest(bytes);
	}

	public static void checkFile(File file, int size) {
		if (file.length() != size)
			throw new IllegalArgumentException("File \"" + file.getName() + "\" has now a different size, try again.");

		if (!file.exists())
			throw new IllegalArgumentException("File \"" + file.getName() + "\" doesn't exists.");
	}

	private synchronized void removeTask(int packetId) {
		if (pendingTasks.containsKey(packetId))
			pendingTasks.remove(packetId).cancel();
	}

	private synchronized void addTask(int id, Runnable runnable, long receiveTimeout, TimeUnit timeUnit) {
		removeTask(id);
		pendingTasks.put(id, Main.getInstance().getTasker().schedule(connection.getPeer(), runnable, receiveTimeout, timeUnit));
	}

	private void onReceiveTimeout(int id) {
		pendingReceiveFiles.remove(id);
	}

	private void onAcceptTimeout(int id) {
		pendingForAcceptFiles.remove(id);
	}

	private void onDownloadTimeout(int id) {
		downloading.remove(id);
	}

	public static enum FileSendResult {
		NOT_PENDING_OR_TIMED_OUT, CONNECTION_EXPIRED, FILE_EXCEPTION, READ_EXCEPTION, HASHING_EXCEPTION, PACKET_WRITE_EXCEPTION, OTHER_EXCEPTION, SUCCESS
	}

	public static enum FileDownloadResult {
		NOT_PENDING_OR_TIMED_OUT, WRITE_EXCEPTION, HASH_MISMATCH, SUCCESS
	}

	public void log(String str) {
		connection.log("[FTH] " + str);
	}

	public void log(String str, VerboseLevel level) {
		connection.log("[FTH] " + str, level);
	}
}
