package com.gmail.marcosav2010.logger

import com.gmail.marcosav2010.common.Color
import com.gmail.marcosav2010.logger.Logger.VerboseLevel

class Log(private val log: ILog, var prefix: String = "") : ILog {

    companion object {
        private const val BRACKET_COLOR = Color.WHITE
    }

    constructor(loggable: Loggable, prefix: String = "") : this(loggable.log, prefix)

    override fun log(o: Any?) = log.log(format(o))

    override fun log(o: Any?, level: VerboseLevel) = log.log(format(o), level)

    override fun log(t: Throwable) = log.log(t)

    override fun log(t: Throwable, msg: String?) = log.log(t, msg)

    private fun format(o: Any?): String {
        return String.format("%s[%s%s%s]%s %s", BRACKET_COLOR, Color.CYAN_BRIGHT, prefix, BRACKET_COLOR, Color.RESET, o)
    }
}