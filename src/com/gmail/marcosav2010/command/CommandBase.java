package com.gmail.marcosav2010.command;

import com.gmail.marcosav2010.logger.ILog;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class CommandBase {

    protected static ILog log = CommandHandler.log;

    @Getter
    private final String label;
}
