package com.gmail.marcosav2010.command;

import java.util.Arrays;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.main.Main;

public class CommandHandler {

    static ILog log = new Log(Main.getInstance(), "CMD");

    public static void handleCommand(String command) {
        if (command != null && !command.isBlank()) {
            String[] args = command.split(" ");
            int argsLength = args.length;

            ExecutedCommand executedCommand = new ExecutedCommand(args[0],
                    argsLength > 1 ? Arrays.copyOfRange(args, 1, argsLength) : new String[0]);

            try {
                executedCommand.tryExecute();
            } catch (CommandNotFoundException ex) {
                log.log("Unknown command, use \"help\" to see available commands.");
            } catch (Exception ex) {
                log.log(ex, "There was an error executing command \"" + command + "\"");
            }
        }

        System.out.print(">> ");
    }
}
