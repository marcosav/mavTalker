package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.logger.Loggable

interface ModuleScope : Loggable {

    val moduleManager: ModuleManager
}