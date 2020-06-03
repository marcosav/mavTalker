package com.gmail.marcosav2010.module;

import com.gmail.marcosav2010.command.CommandRegistry;
import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import org.atteo.classindex.ClassIndex;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class CommandModuleLoader {

    private static CommandModuleLoader instance;

    private final ILog log = new Log(Logger.getGlobal(), "CML");

    private final Set<Class<? extends CommandRegistry>> commandRegistries = new HashSet<>();

    private boolean loaded;

    public static CommandModuleLoader getInstance() {
        if (instance == null)
            instance = new CommandModuleLoader();
        return instance;
    }

    boolean isLoaded() {
        return loaded;
    }

    void addCommands(Class<? extends CommandRegistry> commands) {
        commandRegistries.add(commands);
    }

    public Set<Class<? extends CommandRegistry>> flushRegistries() {
        var r = new HashSet<>(commandRegistries);
        commandRegistries.clear();
        return r;
    }

    public void load() {
        if (loaded)
            throw new IllegalStateException("Module commands are already loaded.");

        loaded = true;

        var matches = ClassIndex.getAnnotated(ModuleCommandRegistry.class, CommandModuleLoader.class.getClassLoader());

        for (var clazz : matches) {
            if (Modifier.isAbstract(clazz.getModifiers()))
                continue;

            var moduleDesc = ModuleLoader.getInstance().getDescription(clazz);
            if (moduleDesc == null)
                continue;

            try {
                ModuleCommandRegistry m = clazz.getAnnotation(ModuleCommandRegistry.class);

                if (!moduleDesc.load())
                    continue;

                Class<? extends CommandRegistry> registryClass = m.value();

                addCommands(registryClass);

                log.log("Found command module in class \"" + clazz.getSimpleName() + "\".", VerboseLevel.HIGH);

            } catch (Exception e) {
                log.log(e, "There was an error while loading class \"" + clazz.getName() + "\"");
            }
        }
    }
}