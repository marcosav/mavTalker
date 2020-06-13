package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.logger.Logger
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.main.Main

internal class VerboseCMD : Command("verbose", arrayOf("v"), "[level]") {

    override fun execute(arg: Array<String>, length: Int) {
        val level: VerboseLevel
        val levels = VerboseLevel.values()

        level = if (arg.isEmpty()) {
            if (Logger.verboseLevel == levels[0]) levels[levels.size - 1] else levels[0]
        } else {
            try {
                VerboseLevel.valueOf(arg[0].toUpperCase())
            } catch (ex: IllegalArgumentException) {
                log.log("ERROR: Invalid verbose level, use "
                        + levels.joinToString(", ") { obj -> obj.toString() })
                return
            }
        }

        Main.instance.generalConfig!![Logger.VERBOSE_LEVEL_PROP] = level.name
        Logger.verboseLevel = level
    }
}