package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.Command
import com.gmail.marcosav2010.main.Main

internal class InfoCMD : Command("info", arrayOf("i"), "[peer] (empty = manager)") {

    override fun execute(arg: Array<String>, length: Int) {
        val peerManager = Main.instance.peerManager!!

        if (length == 0) {
            peerManager.printInfo()
        } else {
            val target = arg[0]
            if (peerManager.exists(target))
                peerManager[target]!!.printInfo()
        }
    }
}