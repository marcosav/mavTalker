package com.gmail.marcosav2010.cipher

import java.security.Key
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.MGF1ParameterSpec
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class CipherPool(private val mode: Int,
                 private val key: Key,
                 initialCiphers: Int = DEFAULT_INITIAL_CIPHERS,
                 private val maxCapacity: Int = DEFAULT_MAX_CAPACITY) {

    companion object {
        private const val DEFAULT_MAX_CAPACITY = 10
        private const val DEFAULT_INITIAL_CIPHERS = 2
        private const val CIPHER_ASYMMETRIC_ALGORITHM = "RSA/ECB/OAEPWithSHA-512AndMGF1Padding"
        private val ALGORITHM_SPEC: AlgorithmParameterSpec = OAEPParameterSpec("SHA-512", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT)
    }

    private val queue: BlockingQueue<Cipher> = ArrayBlockingQueue(maxCapacity)
    private val cipherCount: AtomicInteger = AtomicInteger()

    init {
        init(initialCiphers)
    }

    private fun init(count: Int) {
        require(count <= maxCapacity) { "Initial cipher number can't be higher than max ciphers" }
        for (i in 0 until count) queue.add(createCipher())
    }

    private fun createCipher(): Cipher {
        cipherCount.updateAndGet { i ->
            check(i + 1 <= maxCapacity) { "Cipher pool exceeded cipher of $maxCapacity" }
            i + 1
        }
        val c = Cipher.getInstance(CIPHER_ASYMMETRIC_ALGORITHM)
        c.init(mode, key, ALGORITHM_SPEC)
        return c
    }

    fun doFinal(bytes: ByteArray): ByteArray {
        val c: Cipher = if (!queue.isEmpty()) queue.take()
        else
            if (cipherCount.get() < maxCapacity) try {
                createCipher()
            } catch (ex: IllegalStateException) {
                queue.take()
            } else queue.take()

        val out = c.doFinal(bytes)
        queue.put(c)
        return out
    }
}