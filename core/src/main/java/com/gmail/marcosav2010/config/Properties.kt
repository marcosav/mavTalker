package com.gmail.marcosav2010.config

import com.gmail.marcosav2010.handshake.HandshakeAuthenticator.HandshakeRequirementLevel
import com.gmail.marcosav2010.logger.Logger
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.peer.PeerProperties
import java.util.*

open class Properties(private val category: PropertyCategory, config: IConfiguration) {

    companion object {
        val propCategory: MutableMap<String, Property<*>> = HashMap()

        init {
            propCategory[Logger.VERBOSE_LEVEL_PROP] = Property(
                    PropertyCategory.GLOBAL,
                    VerboseLevel::class.java,
                    VerboseLevel.MINIMAL
            )

            propCategory[PeerProperties.HANDSHAKE_REQUIREMENT_LEVEL] = Property(
                    PropertyCategory.PEER,
                    HandshakeRequirementLevel::class.java,
                    HandshakeRequirementLevel.PRIVATE
            )
        }
    }

    private val properties: MutableMap<String, Any> = HashMap()

    init {
        propCategory.entries.filter { e -> e.value.category === category }.forEach { e -> set(e.key, config[e.key, e.value.default.toString()]) }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(prop: String): T = propCategory[prop]!!.type.cast(properties[prop]) as T

    @Suppress("UNCHECKED_CAST")
    operator fun set(prop: String, value: String?) {
        requireNotNull(value)

        val t: Class<*> = propCategory[prop]!!.type
        var o: Any = value

        if (t.isEnum) {
            val enumClz = t.enumConstants as? Array<Enum<*>>
            o = enumClz?.firstOrNull { it.name == value.toUpperCase() }
                    ?: throw IllegalArgumentException("Property value for $prop not found")

        } else if (t.isAssignableFrom(Int::class.java)) {
            o = value.toInt()
        }

        properties[prop] = o
    }

    operator fun <T> set(prop: String, value: T) {
        properties[prop] = propCategory[prop]!!.type.cast(value)!!
    }

    fun exist(prop: String) = properties.containsKey(prop)

    override fun toString() = propCategory.entries
            .filter { e -> e.value.category == category }
            .map { e ->
                val t: Class<*> = e.value.type
                return "  - NAME: ${e.key}\tVALUE: ${get<Any>(e.key)}\tOPTIONS(" +
                        (if (t.isEnum)
                            t.enumConstants.joinToString(", ") { obj -> obj.toString() }
                        else
                            t.simpleName) + ")"
            }.joinToString("\n")
}