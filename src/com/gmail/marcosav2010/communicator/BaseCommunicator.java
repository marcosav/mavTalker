package com.gmail.marcosav2010.communicator;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.gmail.marcosav2010.tasker.TaskOwner;

/**
 * This communicator sends and receives raw bytes, with a fixed buffer length.
 * 
 * @author Marcos
 *
 */
public class BaseCommunicator extends Communicator {

	public synchronized byte[] read(int bytes) throws IOException {
		byte[] read = getIn().readNBytes(bytes);

		return read;
	}

	public byte[] read(int bytes, TaskOwner taskOwner, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return taskOwner.getExecutorService().submit(() -> read(bytes)).get(timeout, unit);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		getOut().write(bytes);
	}

	@Override
	public void close() throws IOException {
		if (getIn() != null)
			getIn().close();

		if (getOut() != null)
			getOut().close();
	}

	@Override
	public void closeQuietly() {
		try {
			close();
		} catch (IOException ex) {
		}
	}
}
