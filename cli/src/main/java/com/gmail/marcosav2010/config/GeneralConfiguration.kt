package com.gmail.marcosav2010.config

import com.gmail.marcosav2010.logger.Logger

class GeneralConfiguration : Configuration(GENERAL_CONFIG_NAME) {

    companion object {
        private const val GENERAL_CONFIG_NAME = "general"
    }

    init {
        val dp = java.util.Properties()
        Properties.propCategory.forEach { (s, p) -> dp.setProperty(s, p.default.toString()) }
        load(dp)
    }

    override fun get(key: String) = get(key, Properties.propCategory[key]!!.default.toString())

    val verboseLevel: String
        get() = get(Logger.VERBOSE_LEVEL_PROP)!!.toUpperCase()
}