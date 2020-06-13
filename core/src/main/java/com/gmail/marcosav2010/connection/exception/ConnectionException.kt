package com.gmail.marcosav2010.connection.exception

class ConnectionException : Exception {

    constructor(msg: String) : super(msg)
    constructor(msg: String, t: Throwable) : super(msg, t)

    companion object {
        private const val serialVersionUID = 4764600295741975224L
    }
}