package com.gmail.marcosav2010.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class CommandBase {
    
	@Getter
    private final String label;
}
