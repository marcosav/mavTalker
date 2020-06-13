package com.gmail.marcosav2010.config

interface IConfiguration {

    fun exists(key: String): Boolean

    operator fun get(key: String): String?

    operator fun get(key: String, def: String?): String?

    operator fun set(key: String, value: String?)
}