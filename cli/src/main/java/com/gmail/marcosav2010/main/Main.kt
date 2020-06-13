package com.gmail.marcosav2010.main

import com.gmail.marcosav2010.command.CommandManager
import com.gmail.marcosav2010.common.PublicIPResolver
import com.gmail.marcosav2010.config.GeneralConfiguration
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Logger
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.module.*
import com.gmail.marcosav2010.peer.PeerManager
import java.io.File
import java.io.IOException
import kotlin.properties.Delegates

class Main : ModuleScope {

    companion object {

        lateinit var instance: Main
            internal set
    }

    var commandManager: CommandManager? = null
        private set

    var generalConfig: GeneralConfiguration? = null
        private set

    var peerManager: PeerManager? = null
        private set

    var externalModuleLoader: ExternalModuleLoader? = null
        private set

    override var moduleManager by Delegates.notNull<ModuleManager>()
        private set

    override val log: ILog = Logger.global

    var shuttingDown = false

    fun init() {
        generalConfig = GeneralConfiguration()

        Logger.setVerboseLevel(generalConfig!!.verboseLevel)

        externalModuleLoader = ExternalModuleLoader(File("modules"))
        externalModuleLoader!!.load()

        ModuleLoader.load()
        CommandModuleLoader.load()
        commandManager = CommandManager()

        peerManager = PeerManager()

        moduleManager = ModuleManager(this)
        moduleManager.initializeModules()
    }

    fun main(args: Array<String>) {
        PublicIPResolver.obtainPublicAddress()

        log.log("Starting application...", VerboseLevel.MEDIUM)
        moduleManager.onEnable()
        log.log("Done")

        if (args.size == 2) {
            val name = args[0]
            val port = args[1]
            log.log("Trying to create Peer \"$name\" in localhost:$port...", VerboseLevel.MEDIUM)
            run(name, port.toInt())
        }
    }

    private fun run(name: String, port: Int) {
        val startPeer = peerManager!!.create(name, port)
        startPeer.start()
    }

    fun shutdown() {
        if (shuttingDown) return
        shuttingDown = true

        moduleManager.onDisable()
        log.log("Exiting application...")

        peerManager?.shutdown()

        try {
            generalConfig?.store()
        } catch (e: IOException) {
            log.log(e)
        }

        log.log("Bye :)")
    }
}