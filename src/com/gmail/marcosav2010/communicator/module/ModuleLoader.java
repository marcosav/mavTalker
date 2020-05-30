package com.gmail.marcosav2010.communicator.module;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Launcher;

import org.atteo.classindex.ClassIndex;

public class ModuleLoader {

    private static Set<Class<? extends CommandRegistry>> commandRegistries = new HashSet<>();
    private static Map<ModuleDescriptor, Class<? extends Module>> loadedModules = new HashMap<>();

    private static final String LOGGER_PREFIX = "[ML] ";

    private static boolean loaded;

    static Map<ModuleDescriptor, Class<? extends Module>> getModules() {
        return Collections.unmodifiableMap(loadedModules);
    }

    static boolean isLoaded() {
        return loaded;
    }

    static void addCommands(Class<? extends CommandRegistry> cmds) {
        commandRegistries.add(cmds);
    }

    public static Set<Class<? extends CommandRegistry>> flushRegistries() {
        var r = new HashSet<>(commandRegistries);
        commandRegistries.clear();
        return r;
    }

    public static void loadModules() {
        if (loaded)
            throw new IllegalStateException("Modules are already loaded.");

        loaded = true;

        Iterable<Class<?>> matches;

        matches = ClassIndex.getAnnotated(ModuleDescriptor.class, Launcher.class.getClassLoader());

        for (Class<?> clazz : matches) {
            if (Modifier.isAbstract(clazz.getModifiers()))
                continue;

            try {
                @SuppressWarnings("unchecked")
                var c = (Class<? extends Module>) clazz;

                ModuleDescriptor m = (ModuleDescriptor) clazz.getAnnotation(ModuleDescriptor.class);

                if (!m.load())
                    continue;

                loadedModules.put(m, c);

                Class<? extends CommandRegistry> registryClass = m.registry();

                addCommands(registryClass);

                Logger.log(LOGGER_PREFIX + "Found module in class \"" + c.getName() + "\".", VerboseLevel.HIGH);

            } catch (Exception e) {
                Logger.log(LOGGER_PREFIX + "There was an error while loading class \"" + clazz.getName() + "\"");
                Logger.log(e);
            }
        }
    }
}