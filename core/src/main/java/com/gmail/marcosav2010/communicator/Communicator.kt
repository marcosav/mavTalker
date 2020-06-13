package com.gmail.marcosav2010.communicator

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * This is the base class for byte exchange.
 *
 * @author Marcos
 */
abstract class Communicator : Closeable {

    var input: InputStream? = null

    var output: OutputStream? = null

    abstract fun write(bytes: ByteArray)

    abstract override fun close()

    abstract fun closeQuietly()
}