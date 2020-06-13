package com.gmail.marcosav2010.command

import com.gmail.marcosav2010.logger.ILog

abstract class CommandBase(val label: String) {

    protected val log: ILog = CommandHandler.log
}