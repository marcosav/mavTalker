package com.gmail.marcosav2010.command;

import java.util.Objects;

import lombok.Getter;

public abstract class Command extends CommandBase {
    
	@Getter
    private String[] aliases;
	@Getter
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
    
    public abstract void execute(String[] arg, int length);
    
    @Override
    public boolean equals(Object o) {
    	if (o == null)
    		return false;
    	
		if (o instanceof Command) {
			Command c = (Command) o;
			return c.getLabel().equalsIgnoreCase(getLabel());
			
		} else return false;
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(getLabel());
    }
}
