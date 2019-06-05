package com.gmail.marcosav2010.communicator.module;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

import org.atteo.classindex.IndexAnnotated;

import com.gmail.marcosav2010.command.CommandRegistry;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@IndexAnnotated
public @interface LoadModule {
	
	Class<? extends CommandRegistry> registry();
	
	boolean load() default true;
}
