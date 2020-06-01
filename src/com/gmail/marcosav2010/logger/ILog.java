package com.gmail.marcosav2010.logger;

import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

public interface ILog {

    void log(Object o);

    void log(Object o, VerboseLevel level);

    void log(Throwable t);

    void log(Throwable t, String msg);
}