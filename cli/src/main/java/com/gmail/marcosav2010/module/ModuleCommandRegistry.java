package com.gmail.marcosav2010.module;

import com.gmail.marcosav2010.command.CommandRegistry;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface ModuleCommandRegistry {

    Class<? extends CommandRegistry> value();
}
