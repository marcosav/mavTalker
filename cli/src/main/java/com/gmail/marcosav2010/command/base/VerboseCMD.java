package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Main;

import java.util.stream.Collectors;
import java.util.stream.Stream;

class VerboseCMD extends Command {

    VerboseCMD() {
        super("verbose", new String[]{"v"}, "[level]");
    }

    @Override
    public void execute(String[] arg, int args) {
        VerboseLevel level;
        var levels = VerboseLevel.values();
        if (arg.length == 0) {
            level = Logger.getVerboseLevel() == levels[0] ? levels[levels.length - 1] : levels[0];
        } else {
            try {
                level = VerboseLevel.valueOf(arg[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.log("ERROR: Invalid verbose level, use "
                        + Stream.of(levels).map(Enum::toString).collect(Collectors.joining(", ")));
                return;
            }
        }

        Main.getInstance().getGeneralConfig().set(Logger.VERBOSE_LEVEL_PROP, level.name());
        Logger.setVerboseLevel(level);
    }
}