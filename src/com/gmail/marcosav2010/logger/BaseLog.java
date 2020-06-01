package com.gmail.marcosav2010.logger;

import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

public class BaseLog implements ILog {

    @Override
    public void log(Object o) {
        Logger.log(o);
    }

    @Override
    public void log(Object o, VerboseLevel level) {
        Logger.log(o, level);
    }

    @Override
    public void log(Throwable t) {
        Logger.log(t);
    }

    @Override
    public void log(Throwable t, String msg) {
        Logger.log(t, msg);
    }
}