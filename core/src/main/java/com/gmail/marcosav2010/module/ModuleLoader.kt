package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.logger.Logger.global
import org.atteo.classindex.ClassIndex
import java.lang.reflect.Modifier
import java.util.*

object ModuleLoader {

    private val log: ILog = Log(global, "ML")

    private val loadedModules: MutableMap<ModuleDescriptor, Class<out Module>> = HashMap()
    private val modulesByClass: MutableMap<Class<out Module>, ModuleDescriptor> = HashMap()

    var isLoaded = false
        private set

    val modules: Map<ModuleDescriptor, Class<out Module>>
        get() = Collections.unmodifiableMap(loadedModules)

    fun getDescription(clazz: Class<*>) = modulesByClass[clazz]

    fun registerModule(clazz: Class<*>): ModuleDescriptor? {
        if (!Module::class.java.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.modifiers))
            return null

        @Suppress("UNCHECKED_CAST")
        val c = clazz as Class<out Module>
        val descriptor = c.getAnnotation(ModuleDescriptor::class.java)
        if (!descriptor.load) return null

        loadedModules[descriptor] = c
        modulesByClass[c] = descriptor
        return descriptor
    }

    fun load() {
        check(!isLoaded) { "Modules are already registered." }

        isLoaded = true

        val matches = ClassIndex.getAnnotated(ModuleDescriptor::class.java, ModuleLoader::class.java.classLoader)

        for (clazz in matches) {
            try {
                if (registerModule(clazz) != null)
                    log.log("Registered module in class \"" + clazz.simpleName + "\".", VerboseLevel.HIGH)
            } catch (e: Exception) {
                log.log(e, "There was an error while loading class \"" + clazz.name + "\"")
            }
        }
    }
}