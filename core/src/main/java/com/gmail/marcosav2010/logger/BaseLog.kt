package com.gmail.marcosav2010.logger

import com.gmail.marcosav2010.logger.Logger.VerboseLevel

class BaseLog internal constructor() : ILog {

    override fun log(o: Any?) = Logger.log(o)

    override fun log(o: Any?, level: VerboseLevel) = Logger.log(o, level)

    override fun log(t: Throwable) = Logger.log(t)

    override fun log(t: Throwable, msg: String?) = Logger.log(t, msg)
}