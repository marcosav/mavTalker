package com.gmail.marcosav2010.command;

import com.gmail.marcosav2010.main.Main;

public class ExecutedCommand extends CommandBase {

    private final String[] args;
    private final int length;
    
    public ExecutedCommand(String label, String[] args) {
        super(label);
        this.args = args;
        this.length = args.length;
    }
    
    public String[] getArgs() {
        return args;
    }
    
    public int getLength() {
        return length;
    }
    
    public void tryExecute() {
        Main.getInstance().getCommandManager().getCommands().stream().filter(c -> c.isCommand(getLabel())).findFirst()
                .orElseThrow(() -> new CommandNotFoundException("Command not found.")).execute(args, length);
    }
}
