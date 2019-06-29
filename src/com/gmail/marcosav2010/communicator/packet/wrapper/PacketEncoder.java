package com.gmail.marcosav2010.communicator.packet.wrapper;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.gmail.marcosav2010.communicator.packet.AbstractPacket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PacketEncoder implements Closeable, AutoCloseable {

	@Getter
	private final OutputStream out;
	private int length;

	public void write(boolean b) throws IOException {
		write((byte) (b ? 1 : 0));
	}

	public void write(byte b) throws IOException {
		addAndCheck(1);
		out.write(b);
	}

	public void write(short b) throws IOException {
		out.write(serialize(b, 2));
	}

	public void write(int b) throws IOException {
		out.write(serialize(b, 4));
	}

	public void write(long b) throws IOException {
		out.write(serialize(b, 8));
	}

	public void write(byte[] buf) throws IOException {
		addAndCheck(buf.length);
		write(buf.length);
		out.write(buf);
	}

	public void write(String s) throws IOException {
		write(s.getBytes("UTF-8"));
	}

	public void write(UUID uuid) throws IOException {
		write(uuid.getMostSignificantBits());
		write(uuid.getLeastSignificantBits());
	}

	private byte[] serialize(long o, int size) {
		addAndCheck(size);
		byte[] array = new byte[size];
		for (int i = 0; i < size; ++i)
			array[i] = (byte) (o >> 8 * (size - i - 1) & 0xFFL);
		return array;
	}

	private void addAndCheck(int i) {
		length += i;
		if (length > AbstractPacket.BASE_SIZE)
			throw new OverExceededByteLimitException(length);
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
}
