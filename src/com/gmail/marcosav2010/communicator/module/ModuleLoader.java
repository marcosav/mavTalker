package com.gmail.marcosav2010.communicator.module;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import com.gmail.marcosav2010.main.Launcher;

import org.atteo.classindex.ClassIndex;

public class ModuleLoader {

    private static ModuleLoader instance;

    private ILog log = new Log(Logger.getGlobal(), "ML");

    private Set<Class<? extends CommandRegistry>> commandRegistries = new HashSet<>();
    private Map<ModuleDescriptor, Class<? extends Module>> loadedModules = new HashMap<>();

    private boolean loaded;

    Map<ModuleDescriptor, Class<? extends Module>> getModules() {
        return Collections.unmodifiableMap(loadedModules);
    }

    boolean isLoaded() {
        return loaded;
    }

    void addCommands(Class<? extends CommandRegistry> cmds) {
        commandRegistries.add(cmds);
    }

    public Set<Class<? extends CommandRegistry>> flushRegistries() {
        var r = new HashSet<>(commandRegistries);
        commandRegistries.clear();
        return r;
    }

    public void loadModules() {
        if (loaded)
            throw new IllegalStateException("Modules are already loaded.");

        loaded = true;

        var matches = ClassIndex.getAnnotated(ModuleDescriptor.class, Launcher.class.getClassLoader());

        for (var clazz : matches) {
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

                log.log("Found module in class \"" + c.getName() + "\".", VerboseLevel.HIGH);

            } catch (Exception e) {
                log.log(e, "There was an error while loading class \"" + clazz.getName() + "\"");
            }
        }
    }

    public static ModuleLoader getInstance() {
        if (instance == null)
            instance = new ModuleLoader();
        return instance;
    }
}