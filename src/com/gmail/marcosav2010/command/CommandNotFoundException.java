package com.gmail.marcosav2010.command;

public class CommandNotFoundException extends RuntimeException {
    
    public static final long serialVersionUID = 2L;
    
    public CommandNotFoundException(String msg) {
        super(msg);
    }
}
