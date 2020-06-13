package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.main.Main

internal class HelpCMD : Command("help") {

    override fun execute(arg: Array<String>, length: Int) {
        log.log(">> cmd <required> [optional] (info)")
        Main.instance.commandManager!!.commands.forEach { c -> log.log(">> " + c.usage) }
    }
}