package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import kotlin.system.exitProcess

internal class ExitCMD : Command("exit", arrayOf("e")) {

    override fun execute(arg: Array<String>, length: Int) {
        exitProcess(0)
    }
}