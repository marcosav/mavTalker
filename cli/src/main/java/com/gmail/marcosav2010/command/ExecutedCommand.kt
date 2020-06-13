package com.gmail.marcosav2010.command

import com.gmail.marcosav2010.main.Main

class ExecutedCommand(label: String, val args: Array<String>) : CommandBase(label) {

    private val length: Int = args.size

    fun tryExecute() {
        Main.instance.commandManager?.fetch(label)?.execute(args, length)
    }
}