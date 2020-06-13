package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.main.Main.Companion.instance

internal class StopCMD : Command("stop", arrayOf("st", "shutdown"), "[peer] (empty = all)") {

    override fun execute(arg: Array<String>, length: Int) {
        if (length > 0) {
            val target = arg[0]
            instance.peerManager!!.shutdown(target)
        } else {
            instance.peerManager!!.shutdown()
        }
    }
}