package com.gmail.marcosav2010.logger;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.gmail.marcosav2010.common.Color;

import lombok.Getter;

public class Logger {

	@Getter
	private static final ILog global = new BaseLog();

	private static VerboseLevel VERBOSE_LEVEL = VerboseLevel.HIGH;
	private static VerboseLevel DEFAULT_LEVEL = VerboseLevel.MINIMAL;

	private static String TIMESTAMP_COLOR = Color.WHITE;
	private static boolean TIMESTAMP = true;

	private static String TIME_PATTERN = "HH:mm:ss.SSS";
	private static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(TIME_PATTERN);

	private static Object lock = new Object();

	private static PrintStream out = System.out;
	private static PrintStream err = System.err;

	public static void log(Object o) {
		log(o, DEFAULT_LEVEL);
	}

	public static void log(Object o, VerboseLevel level) {
		if (level.ordinal() <= VERBOSE_LEVEL.ordinal())
			synchronized (lock) {
				printTimestamp(out);
				out.printf("%s\n", o);
			}
	}

	public static void log(Throwable ex) {
		synchronized (lock) {
			printTimestamp(err);
			printErrorPrefix(err);
			ex.printStackTrace(err);
		}
	}

	public static void log(Throwable ex, String msg) {
		synchronized (lock) {
			printTimestamp(err);
			printErrorPrefix(err);
			err.println(msg);
			ex.printStackTrace(err);
		}
	}

	public static void setVerboseLevel(VerboseLevel level) {
		VERBOSE_LEVEL = level;
		log("Verbose level set to " + VERBOSE_LEVEL + ".");
	}

	public static void setVerboseLevel(String level) {
		VERBOSE_LEVEL = VerboseLevel.valueOf(level);
		log("Verbose level set to " + VERBOSE_LEVEL + ".", VerboseLevel.MEDIUM);
	}

	public static VerboseLevel getVerboseLevel() {
		return VERBOSE_LEVEL;
	}

	private static void printErrorPrefix(PrintStream ps) {
		err.printf("%s[%sERROR%s]%s ", Color.WHITE, Color.RED, Color.WHITE, Color.RESET);
	}

	private static void printTimestamp(PrintStream ps) {
		if (TIMESTAMP)
			ps.printf("%s[%s]%s ", TIMESTAMP_COLOR, TIME_FORMAT.format(new Date()), Color.RESET);
	}

	public static enum VerboseLevel {

		MINIMAL, LOW, MEDIUM, HIGH;
	}
}
