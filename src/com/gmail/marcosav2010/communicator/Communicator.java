package com.gmail.marcosav2010.communicator;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is the base class for byte exchange.
 * 
 * @author Marcos
 *
 */
public abstract class Communicator implements Closeable, AutoCloseable {

	private InputStream in;
	private OutputStream out;

	public Communicator() {
	}

	public abstract void write(byte[] bytes) throws IOException;

	public InputStream getIn() {
		return in;
	}

	public OutputStream getOut() {
		return out;
	}

	public void setIn(InputStream in) {
		this.in = in;
	}

	public void setOut(OutputStream out) {
		this.out = out;
	}

	@Override
	public abstract void close() throws IOException;
	
	public abstract void closeQuietly();
}
