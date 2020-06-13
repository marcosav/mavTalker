package com.gmail.marcosav2010.command

import com.gmail.marcosav2010.command.base.BaseCommandRegistry
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.main.Main
import com.gmail.marcosav2010.module.CommandModuleLoader
import java.util.*
import kotlin.reflect.KClass

class CommandManager {

    private val log: ILog
    private val registeredLabels: MutableMap<String, Command> = HashMap()
    private val registries: MutableSet<KClass<out CommandRegistry>>
    val commands: Set<Command>
        get() = registeredLabels.values.toSet()

    init {
        log = Log(Main.instance, "CommandManager")

        registries = HashSet()
        register(BaseCommandRegistry::class)
        CommandModuleLoader.flushRegistries().forEach { clazz -> register(clazz) }
    }

    fun fetch(label: String) = registeredLabels[label] ?: throw CommandNotFoundException("Command not found")

    private fun <T : CommandRegistry> register(clazz: KClass<T>) {
        if (registries.contains(clazz)) return

        registries.add(clazz)

        val r: CommandRegistry
        try {
            r = clazz.constructors.first().call()
        } catch (e: Exception) {
            log.log(e, "There was an error while initializing registry \"" + clazz.qualifiedName + "\"")
            return
        }

        val v = r.commands.map { c ->
            registeredLabels[c.label] = c
            c.aliases.forEach { a -> registeredLabels[a] = c }
            1
        }.sum()

        log.log("Loaded " + v + " commands from registry \"" + clazz.simpleName + "\"", VerboseLevel.HIGH)
    }
}