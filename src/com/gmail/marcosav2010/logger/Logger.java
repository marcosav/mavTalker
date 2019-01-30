package com.gmail.marcosav2010.logger;

public class Logger {

	private static VerboseLevel VERBOSE_LEVEL = VerboseLevel.HIGH;
	private static VerboseLevel DEFAULT_LEVEL = VerboseLevel.MINIMAL;

	private static Object lock = new Object();

	public static void log(String str) {
		log(str, DEFAULT_LEVEL);
	}

	public static void log(String str, VerboseLevel level) {
		if (level.ordinal() <= VERBOSE_LEVEL.ordinal())
			synchronized (lock) {
				System.out.println(str);
			}
	}

	public static void log(Throwable ex) {
		synchronized (lock) {
			ex.printStackTrace(System.err);
		}
	}

	public static void log(Throwable ex, String msg) {
		synchronized (lock) {
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

	public static void setVerboseLevel(VerboseLevel level) {
		VERBOSE_LEVEL = level;
	}

	public static VerboseLevel getVerboseLevel() {
		return VERBOSE_LEVEL;
	}

	public static enum VerboseLevel {

		MINIMAL, LOW, MEDIUM, HIGH;
	}
}
