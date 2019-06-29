package com.gmail.marcosav2010.communicator;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This is the base class for byte exchange.
 * 
 * @author Marcos
 *
 */
@NoArgsConstructor
public abstract class Communicator implements Closeable, AutoCloseable {

	@Getter
	@Setter
	private InputStream in;
	@Getter
	@Setter
	private OutputStream out;

	public abstract void write(byte[] bytes) throws IOException;

	@Override
	public abstract void close() throws IOException;
	
	public abstract void closeQuietly();
}
