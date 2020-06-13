package com.gmail.marcosav2010.main

import com.gmail.marcosav2010.command.CommandHandler.handleCommand
import com.gmail.marcosav2010.logger.Logger
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder

object Launcher {

    private val handleShutdown = { Main.instance.shutdown() }

    @JvmStatic
    fun main(args: Array<String>) {
        val pkg = Launcher::class.java.getPackage()

        Logger.global.log(
                "Loading " + pkg.implementationTitle + " v" + pkg.implementationVersion +
                        " by " + pkg.specificationVendor + "...")

        addSignalHook()

        val main = Main()
        Main.instance = main
        main.init()
        main.main(args)

        listenForCommands()
    }

    private fun listenForCommands() {
        val terminal = TerminalBuilder.terminal()
        val lineReader = LineReaderBuilder.builder().terminal(terminal).history(DefaultHistory()).build()

        try {
            while (!Main.instance.shuttingDown)
                handleCommand(lineReader.readLine(">> "))
        } catch (ex: UserInterruptException) {
            handleShutdown.invoke()
        }
    }

    private fun addSignalHook() = Runtime.getRuntime().addShutdownHook(Thread(handleShutdown))
}