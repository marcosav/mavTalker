package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.InputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

import com.gmail.marcosav2010.communicator.packet.AbstractPacket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PacketDecoder implements Closeable, AutoCloseable {

	@Getter
	private final InputStream in;
	private int length;

	public boolean readBoolean() throws IOException {
		return readByte() == 1;
	}

	public byte readByte() throws IOException {
		addAndCheck(1);
		return (byte) in.read();
	}

	public int readUByte() throws IOException {
		addAndCheck(1);
		return in.read();
	}

	public short readShort() throws IOException {
		return (short) deserialize(2);
	}

	public int readUShort() throws IOException {
		return (int) deserialize(2);
	}

	public int readInt() throws IOException {
		return (int) deserialize(4);
	}

	public long readUInt() throws IOException {
		return deserialize(4);
	}

	public long readLong() throws IOException {
		return deserialize(8);
	}

	public byte[] readBytes() throws IOException {
		int len = readInt();
		return readFully(len);
	}

	public String readString() throws IOException {
		byte[] data = readBytes();
		return new String(data, "UTF-8");
	}

	public UUID readUUID() throws IOException {
		long high = readLong();
		long low = readLong();

		return new UUID(high, low);
	}

	public void readFully(byte[] buffer) throws IOException {
		int r;
		for (int offset = 0; offset < buffer.length; offset += r) {
			r = in.read(buffer, offset, buffer.length - offset);
			if (r < 0)
				throw new EOFException();
		}
	}

	public byte[] readFully(int size) throws IOException {
		addAndCheck(size);
		byte[] b = new byte[size];
		readFully(b);
		return b;
	}

	private long deserialize(int size) throws IOException {
		return deserialize(readFully(size));
	}

	private long deserialize(byte[] data) {
		long ret = 0L;
		for (int i = 0; i < data.length; ++i)
			ret += (data[i] & 0xFFL) << 8 * (data.length - i - 1);
		return ret;
	}

	private void addAndCheck(int i) {
		length += i;
		if (length > AbstractPacket.BASE_SIZE)
			throw new OverExceededByteLimitException(length);
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
