package com.gmail.marcosav2010.command;

public abstract class CommandBase {
    
    private final String label;
    
    public CommandBase(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
