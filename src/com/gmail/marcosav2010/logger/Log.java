package com.gmail.marcosav2010.logger;

import com.gmail.marcosav2010.common.Color;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@RequiredArgsConstructor
public class Log implements ILog {

    private static String BRACKET_COLOR = Color.WHITE;

    private final ILog log;

    @Setter
    @Getter
    private String prefix = "";

    public Log(Loggable loggable, String prefix) {
        log = loggable.getLog();
        this.prefix = prefix;
    }

    @Override
    public void log(Object o) {
        log.log(format(o));
    }

    @Override
    public void log(Object o, VerboseLevel level) {
        log.log(format(o), level);
    }

    @Override
    public void log(Throwable t) {
        log.log(t);
    }

    @Override
    public void log(Throwable t, String msg) {
        log.log(t, msg);
    }

    private String format(Object o) {
        return String.format("%s[%s%s%s]%s %s", BRACKET_COLOR, Color.CYAN_BRIGHT, prefix, BRACKET_COLOR, Color.RESET,
                o);
    }
}