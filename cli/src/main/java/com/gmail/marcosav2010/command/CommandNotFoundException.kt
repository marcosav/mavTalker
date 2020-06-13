package com.gmail.marcosav2010.command

class CommandNotFoundException(msg: String?) : RuntimeException(msg) {

    companion object {
        const val serialVersionUID = 2L
    }
}