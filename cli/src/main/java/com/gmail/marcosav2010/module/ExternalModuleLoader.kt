package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger
import org.atteo.classindex.ClassIndex
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*

class ExternalModuleLoader(private val folder: File) {

    private val log: ILog = Log(Logger.global, "EML")

    fun load() {
        if (!folder.exists()) folder.mkdir()

        for (file in Objects.requireNonNull(folder.listFiles())) {
            if (file.isFile && file.name.endsWith(".jar")) {
                try {
                    val urls = arrayOf(URL("jar:file:" + file.path + "!/"))
                    val cl = URLClassLoader.newInstance(urls)
                    val matches = ClassIndex.getAnnotated(ModuleDescriptor::class.java, cl)

                    log.log("Registering modules from \"" + file.name + "\".", Logger.VerboseLevel.HIGH)

                    matches.forEach { m ->
                        val descriptor = ModuleLoader.registerModule(m)
                        if (descriptor != null)
                            log.log("Registered external module " + descriptor.name + ".", Logger.VerboseLevel.MEDIUM)
                    }
                } catch (ex: Exception) {
                    log.log("There was an error while loading " + file.name)
                }
            }
        }
    }
}