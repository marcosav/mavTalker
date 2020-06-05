package com.gmail.marcosav2010.module;

import com.gmail.marcosav2010.logger.ILog;
import com.gmail.marcosav2010.logger.Log;
import com.gmail.marcosav2010.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.atteo.classindex.ClassIndex;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

@RequiredArgsConstructor
public class ExternalModuleLoader {

    private final File folder;
    private final ILog log = new Log(Logger.getGlobal(), "EML");

    public void load() {
        if (!folder.exists())
            folder.mkdir();

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                try {
                    URL[] urls = {new URL("jar:file:" + file.getPath() + "!/")};
                    URLClassLoader cl = URLClassLoader.newInstance(urls);

                    var matches = ClassIndex.getAnnotated(ModuleDescriptor.class, cl);

                    log.log("Registering modules from \"" + file.getName() + "\".", Logger.VerboseLevel.HIGH);

                    matches.forEach(m -> {
                        var descriptor = ModuleLoader.getInstance().registerModule(m);
                        if (descriptor != null)
                            log.log("Registered external module " + descriptor.name() + ".", Logger.VerboseLevel.MEDIUM);
                    });

                } catch (Exception ex) {
                    log.log("There was an error while loading " + file.getName());
                }
            }
        }
    }
}
