package com.gmail.marcosav2010.config

import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

open class Configuration(configName: String) : IConfiguration {

    private val log: ILog = Log(Logger.global, "Conf-$configName")
    private val configName = "$configName.properties"
    private lateinit var path: Path
    private var save: Boolean = false
    private lateinit var properties: Properties

    fun load(defaultProperties: Properties) {
        properties = defaultProperties
        val f = File(configName)
        path = f.toPath()

        if (f.exists()) {
            try {
                properties.load(Files.newInputStream(path))
            } catch (e: IOException) {
                log.log(e)
            }
        } else {
            try {
                f.createNewFile()
                internalStore()
            } catch (e: IOException) {
                log.log(e)
            }
        }
    }

    override fun exists(key: String) = properties.getProperty(key) != null

    override fun get(key: String): String? = properties.getProperty(key)

    override fun get(key: String, def: String?): String? = properties.getProperty(key, def)

    override fun set(key: String, value: String?) = properties.setProperty(key, value).let { save = true }

    fun store() {
        if (save) internalStore()
    }

    private fun internalStore() = Files.newOutputStream(path).use { out -> properties.store(out, null) }
}