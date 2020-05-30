package com.gmail.marcosav2010.communicator.module;

import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

public interface ModuleScope {

    ModuleManager getModuleManager();

    void log(String str);

    void log(String str, VerboseLevel level);
}