package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;

class ExitCMD extends Command {

    ExitCMD() {
        super("exit", new String[]{"e"});
    }

    @Override
    public void execute(String[] arg, int args) {
        System.exit(0);
    }
}