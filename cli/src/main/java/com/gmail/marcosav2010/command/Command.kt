package com.gmail.marcosav2010.command

import java.util.*

abstract class Command(label: String) : CommandBase(label) {

    var aliases: Array<String?>
    var usage: String

    init {
        aliases = arrayOfNulls(0)
        usage = label
    }

    constructor(label: String, usage: String) : this(label) {
        aliases = arrayOfNulls(0)
        this.usage += " $usage"
    }

    constructor(label: String, aliases: Array<String?>) : this(label) {
        this.aliases = aliases
    }

    constructor(label: String, aliases: Array<String?>, usage: String) : this(label, usage) {
        this.aliases = aliases
    }

    abstract fun execute(arg: Array<String?>?, length: Int)

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return if (other is Command) {
            other.label.equals(label, ignoreCase = true)
        } else false
    }

    override fun hashCode(): Int {
        return Objects.hash(label)
    }
}