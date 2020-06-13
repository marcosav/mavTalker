package com.gmail.marcosav2010.logger

import com.gmail.marcosav2010.common.Color
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*

object Logger {

    val global: ILog = BaseLog()

    private val DEFAULT_LEVEL = VerboseLevel.MINIMAL
    private const val TIMESTAMP_COLOR = Color.WHITE
    private const val TIMESTAMP = true
    private const val TIME_PATTERN = "HH:mm:ss.SSS"
    private val TIME_FORMAT = SimpleDateFormat(TIME_PATTERN)
    private val lock = Any()
    private val out = System.out
    private val err = System.err
    private var VERBOSE_LEVEL = VerboseLevel.HIGH

    const val VERBOSE_LEVEL_PROP = "verboseLevel"

    @JvmOverloads
    fun log(o: Any?, level: VerboseLevel = DEFAULT_LEVEL) {
        if (level.ordinal <= VERBOSE_LEVEL.ordinal) synchronized(lock) {
            printTimestamp(out)
            out.printf("%s\n", o)
        }
    }

    fun log(ex: Throwable) {
        synchronized(lock) {
            printTimestamp(err)
            printErrorPrefix()
            ex.printStackTrace(err)
        }
    }

    @JvmStatic
    fun log(ex: Throwable, msg: String?) {
        synchronized(lock) {
            printTimestamp(err)
            printErrorPrefix()
            err.println(msg)
            ex.printStackTrace(err)
        }
    }

    var verboseLevel: VerboseLevel
        get() = VERBOSE_LEVEL
        set(level) {
            VERBOSE_LEVEL = level
            log("Verbose level set to $VERBOSE_LEVEL.")
        }

    fun setVerboseLevel(level: String) {
        VERBOSE_LEVEL = VerboseLevel.valueOf(level)
        log("Verbose level set to $VERBOSE_LEVEL.", VerboseLevel.MEDIUM)
    }

    private fun printErrorPrefix(ps: PrintStream = err) {
        ps.printf("%s[%sERROR%s]%s ", Color.WHITE, Color.RED, Color.WHITE, Color.RESET)
    }

    private fun printTimestamp(ps: PrintStream) {
        if (TIMESTAMP) ps.printf("%s[%s]%s ", TIMESTAMP_COLOR, TIME_FORMAT.format(Date()), Color.RESET)
    }

    enum class VerboseLevel {
        MINIMAL, LOW, MEDIUM, HIGH
    }
}