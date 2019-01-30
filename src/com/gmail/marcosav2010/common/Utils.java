package com.gmail.marcosav2010.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Utils {

	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}

	public static int bytesToInt(byte[] array) {
		return array[0] << 24 | (array[1] & 0xFF) << 16 | (array[2] & 0xFF) << 8 | (array[3] & 0xFF);
	}

	public static String formatSize(long bytes) {
		if (bytes == 0)
			return "0 Byte";
		int k = 1024;
		int i = (int) Math.floor(Math.log(bytes) / Math.log(k));
		String[] sizes = { "Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };
		return round(bytes / Math.pow(k, i), 3) + " " + sizes[i];
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static byte[] concat(byte[] ba1, byte[] ba2) {
		byte[] out = new byte[ba1.length + ba2.length];

		System.arraycopy(ba1, 0, out, 0, ba1.length);
		System.arraycopy(ba2, 0, out, ba1.length, ba2.length);

		return out;
	}

	public static byte[][] split(byte[] ba, int pos) {
		byte[] ba1 = new byte[pos];
		byte[] ba2 = new byte[ba.length - pos];

		System.arraycopy(ba, 0, ba1, 0, pos);
		System.arraycopy(ba, pos, ba2, 0, ba2.length);

		return new byte[][] { ba1, ba2 };
	}

	public static byte[] getBytesFromUUID(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());

		return bb.array();
	}

	public static UUID getUUIDFromBytes(byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		Long high = byteBuffer.getLong();
		Long low = byteBuffer.getLong();

		return new UUID(high, low);
	}

	public static <K, V> void put(Map<K, Set<V>> map, K key, V value) {
		Set<V> collection = map.get(key);
		if (collection == null)
			collection = new HashSet<>();

		collection.add(value);
		map.put(key, collection);
	}

	public static <K, V> void remove(Map<K, Set<V>> map, V value) {
		map.forEach((k, v) -> v.remove(value));
	}
}
