package com.gmail.marcosav2010.module

import com.gmail.marcosav2010.command.CommandRegistry
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import java.util.*
import kotlin.reflect.KClass

object CommandModuleLoader {

    private val log: ILog = Log(Logger.global, "CML")
    private val commandRegistries: MutableSet<KClass<out CommandRegistry>> = HashSet()
    private var loaded = false

    private fun addCommands(commands: KClass<out CommandRegistry>) = commandRegistries.add(commands)

    fun flushRegistries(): Set<KClass<out CommandRegistry>> {
        val r = HashSet(commandRegistries)
        commandRegistries.clear()
        return r
    }

    fun load() {
        check(!loaded) { "Module commands are already loaded." }

        loaded = true

        ModuleLoader.modules.forEach { (moduleDesc, m) ->
            try {
                val mcr = m.getAnnotation(ModuleCommandRegistry::class.java)

                if (!moduleDesc.load) return
                val registryClass = mcr.value

                addCommands(registryClass)

                log.log("Found command module in class \"" + m.simpleName + "\".", VerboseLevel.HIGH)
            } catch (e: Exception) {
                log.log(e, "There was an error while loading command registries in \"" + m.name + "\"")
            }
        }
    }
}