package com.gmail.marcosav2010.command;

import com.gmail.marcosav2010.main.Main;

import lombok.Getter;

public class ExecutedCommand extends CommandBase {

	@Getter
    private final String[] args;
	@Getter
    private final int length;
    
    public ExecutedCommand(String label, String[] args) {
        super(label);
        this.args = args;
        this.length = args.length;
    }
    
    public void tryExecute() {
        var res = Main.getInstance().getCommandManager().fetch(getLabel());
        if (res == null)
        	throw new CommandNotFoundException("Command not found.");

        res.execute(args, length);
    }
}
