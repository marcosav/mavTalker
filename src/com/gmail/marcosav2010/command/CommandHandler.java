package com.gmail.marcosav2010.command;

import java.util.Arrays;

import com.gmail.marcosav2010.logger.Logger;

public class CommandHandler {

    public static void handleCommand(String command) {
    	if (command == null || command.isBlank())
    		return;
    	
        String[] args = command.split(" ");
        int argsLength = args.length;
        
        ExecutedCommand executedCommand = new ExecutedCommand(args[0],
                argsLength > 1 ? Arrays.copyOfRange(args, 1, argsLength) : new String[0]);
                    
        try {
            executedCommand.tryExecute();
        } catch (CommandNotFoundException ex) {
            Logger.log("Unknown command, use \"help\" to see available commands.");
        } catch (Exception ex) {
        	Logger.log("There was an error executing command \"" + command + "\"");
        	Logger.log(ex);
        }
    }
}
