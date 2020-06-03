package com.gmail.marcosav2010.module;

import com.gmail.marcosav2010.command.CommandRegistry;
import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@IndexAnnotated
public @interface ModuleCommandRegistry {

    Class<? extends CommandRegistry> value();
}
