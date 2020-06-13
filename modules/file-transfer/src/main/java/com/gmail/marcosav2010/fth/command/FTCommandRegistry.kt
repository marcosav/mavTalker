package com.gmail.marcosav2010.fth.command

import com.gmail.marcosav2010.command.CommandRegistry
import com.gmail.marcosav2010.connection.Connection
import com.gmail.marcosav2010.fth.FTModule

class FTCommandRegistry : CommandRegistry(setOf(ClearDownloadsCMD(), FileCMD(), FindCMD(), DownloadCMD())) {

    companion object {
        fun getFTH(c: Connection) = (c.moduleManager.getModule("FTH") as FTModule).fth
    }
}