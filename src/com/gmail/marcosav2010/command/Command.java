package com.gmail.marcosav2010.command;

import java.util.stream.Stream;

public abstract class Command extends CommandBase {
    
    private String[] aliases;
    private String usage;
    
    public Command(String label) {
        super(label);
        this.aliases = new String[0];
        this.usage = label;
    }
    
    public Command(String label, String usage) {
        this(label);
        this.aliases = new String[0];
        this.usage += " " + usage;
    }
    
    public Command(String label, String[] aliases) {
        this(label);
        this.aliases = aliases;
    }
    
    public Command(String label, String[] aliases, String usage) {
        this(label, usage);
        this.aliases = aliases;
    }
    
    public String[] getAliases() {
        return aliases;
    }
    
    public String getUsage() {
        return usage;
    }
    
    public boolean isCommand(String label) {
        return label.equalsIgnoreCase(getLabel())
                || (aliases.length > 0 && Stream.of(aliases).anyMatch(l -> l.equalsIgnoreCase(label)));
    }
    
    public abstract void execute(String[] arg, int length);
}
