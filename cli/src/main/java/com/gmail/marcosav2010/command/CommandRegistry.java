package com.gmail.marcosav2010.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public abstract class CommandRegistry {

    @Getter
    protected final Set<Command> commands;
}
