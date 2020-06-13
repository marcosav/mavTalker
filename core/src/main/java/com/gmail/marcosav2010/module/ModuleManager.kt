package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.communicator.packet.handling.listener.PacketListener
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.module.ModuleLoader.isLoaded
import java.util.*

class ModuleManager(private val scope: ModuleScope) {

    val log: ILog = Log(scope, "MM")

    private val names: MutableMap<String, Module> = HashMap()
    private val modules = PriorityQueue<Module>()

    val listeners: MutableList<PacketListener> = LinkedList()

    fun initializeModules() {
        if (!isLoaded) throw RuntimeException("Modules has not been loaded yet!")

        ModuleLoader.modules.forEach { (desc, m) ->

            if (!scope.javaClass.isAssignableFrom(desc.scope.java)) return@forEach

            log.log("Loading module " + desc.name + ".", VerboseLevel.HIGH)

            try {
                val module = m.getConstructor(ModuleDescriptor::class.java).newInstance(desc)
                module!!.onInit(scope)

                for (listenerKlass in desc.listeners) {
                    val lc = listenerKlass.java
                    val pl = if (!m.isAssignableFrom(lc)) {
                        try {
                            lc.getConstructor(m).newInstance(module)
                        } catch (ex: NoSuchMethodException) {
                            lc.getConstructor().newInstance()
                        }
                    } else module as PacketListener

                    listeners.add(pl)
                }

                names[desc.name] = module
                modules.add(module)

                log.log("Successfully loaded module " + desc.name + ".", VerboseLevel.MEDIUM)

            } catch (e: Exception) {
                log.log(e, "There was an error while initializing module \"" + desc.name + "\".")
            }
        }
        log.log("Loaded " + modules.size + " modules.", VerboseLevel.LOW)
    }

    fun onEnable() = modules.forEach { m -> m!!.onEnable(scope) }

    fun onDisable() = modules.forEach { m -> m!!.onDisable(scope) }

    fun getModule(name: String) = names[name]
}