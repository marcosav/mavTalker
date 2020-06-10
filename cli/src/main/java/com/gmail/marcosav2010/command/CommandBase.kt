package com.gmail.marcosav2010.command

import com.gmail.marcosav2010.logger.ILog

abstract class CommandBase(protected val label: String) {

    companion object {
        @JvmField
        protected var log: ILog = CommandHandler.log
    }
}