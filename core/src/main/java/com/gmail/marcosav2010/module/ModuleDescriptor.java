package com.gmail.marcosav2010.module;

import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener;
import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.*;

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@IndexAnnotated
public @interface ModuleDescriptor {

    boolean load() default true;

    String name();

    int priority() default 0;

    Class<? extends ModuleScope> scope();

    Class<? extends PacketListener>[] listeners();
}
