package com.gmail.marcosav2010.logger

import com.gmail.marcosav2010.logger.Logger.VerboseLevel

interface ILog {

    fun log(o: Any?)
    fun log(o: Any?, level: VerboseLevel)
    fun log(t: Throwable)
    fun log(t: Throwable, msg: String?)
}