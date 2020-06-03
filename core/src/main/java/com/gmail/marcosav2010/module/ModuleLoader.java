package com.gmail.marcosav2010.module;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger;
import com.gmail.marcosav2010.logger.Logger.VerboseLevel;
import org.atteo.classindex.ClassIndex;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModuleLoader {

    private static ModuleLoader instance;

    private final ILog log = new Log(Logger.getGlobal(), "ML");

    private final Map<ModuleDescriptor, Class<? extends Module>> loadedModules = new HashMap<>();
    private final Map<Class<? extends Module>, ModuleDescriptor> modulesByClass = new HashMap<>();

    private boolean loaded;

    public static ModuleLoader getInstance() {
        if (instance == null)
            instance = new ModuleLoader();
        return instance;
    }

    Map<ModuleDescriptor, Class<? extends Module>> getModules() {
        return Collections.unmodifiableMap(loadedModules);
    }

    ModuleDescriptor getDescription(Class<?> clazz) {
        return modulesByClass.get(clazz);
    }

    boolean isLoaded() {
        return loaded;
    }

    public void load() {
        if (loaded)
            throw new IllegalStateException("Modules are already loaded.");

        loaded = true;

        var matches = ClassIndex.getAnnotated(ModuleDescriptor.class, ModuleLoader.class.getClassLoader());

        for (var clazz : matches) {
            if (Modifier.isAbstract(clazz.getModifiers()))
                continue;

            try {
                @SuppressWarnings("unchecked")
                var c = (Class<? extends Module>) clazz;

                ModuleDescriptor m = clazz.getAnnotation(ModuleDescriptor.class);

                if (!m.load())
                    continue;

                loadedModules.put(m, c);
                modulesByClass.put(c, m);

                log.log("Found module in class \"" + c.getSimpleName() + "\".", VerboseLevel.HIGH);

            } catch (Exception e) {
                log.log(e, "There was an error while loading class \"" + clazz.getName() + "\"");
            }
        }
    }
}