package com.gmail.marcosav2010.command

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.main.Main

object CommandHandler {

    var log: ILog = Log(Main.instance, "CMD")

    @JvmStatic
    fun handleCommand(command: String) {
        if (!command.isBlank()) {
            val args = command.split(" ").toTypedArray()
            val argsLength = args.size
            val executedCommand = ExecutedCommand(args[0],
                    if (argsLength > 1) args.copyOfRange(1, argsLength) else emptyArray())
            try {
                executedCommand.tryExecute()
            } catch (ex: CommandNotFoundException) {
                log.log("Unknown command, use \"help\" to see available commands.")
            } catch (ex: Exception) {
                log.log(ex, "There was an error executing command \"$command\"")
            }
        }
    }
}