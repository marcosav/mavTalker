package com.gmail.marcosav2010.command.base;

import com.gmail.marcosav2010.command.Command;
import com.gmail.marcosav2010.main.Main;

class InfoCMD extends Command {

    InfoCMD() {
        super("info", new String[]{"i"}, "[peer] (empty = manager)");
    }

    @Override
    public void execute(String[] arg, int args) {
        if (args == 0) {
            Main.getInstance().getPeerManager().printInfo();

        } else {
            String target = arg[0];
            if (Main.getInstance().getPeerManager().exists(target)) {
                Main.getInstance().getPeerManager().get(target).printInfo();
            }
        }
    }
}