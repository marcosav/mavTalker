package com.gmail.marcosav2010.handshake

import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.communicator.BaseCommunicator
import com.gmail.marcosav2010.tasker.TaskOwner
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * This communicator encrypts data using AES and a key, only used for handshake
 * to guarantee some security
 *
 * @author Marcos
 */
class HandshakeCommunicator(baseCommunicator: BaseCommunicator) : BaseCommunicator() {

    companion object {
        private const val LENGTH_BYTES = Integer.BYTES
        private const val CIPHER_SYMMETRIC_ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val KEY_ALGORITHM = "AES"
    }

    private var inCipher: Cipher
    private var outCipher: Cipher
    private var inSet = false
    private var outSet = false

    init {
        try {
            inCipher = Cipher.getInstance(CIPHER_SYMMETRIC_ALGORITHM)
            outCipher = Cipher.getInstance(CIPHER_SYMMETRIC_ALGORITHM)
            input = baseCommunicator.input
            output = baseCommunicator.output
        } catch (e: NoSuchAlgorithmException) {
            throw IOException(e)
        } catch (e: NoSuchPaddingException) {
            throw IOException(e)
        }
    }

    fun setIn(key: ByteArray, iv: ByteArray) {
        val ivParameterSpecIn = IvParameterSpec(iv)
        val secretKey: SecretKey = SecretKeySpec(key, KEY_ALGORITHM)

        inSet = try {
            inCipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpecIn)
            true
        } catch (e: InvalidKeyException) {
            throw IOException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw IOException(e)
        }
    }

    fun setOut(key: ByteArray, iv: ByteArray) {
        val ivParameterSpecOut = IvParameterSpec(iv)
        val secretKey: SecretKey = SecretKeySpec(key, KEY_ALGORITHM)

        outSet = try {
            outCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpecOut)
            true
        } catch (e: InvalidKeyException) {
            throw IOException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw IOException(e)
        }
    }

    @Synchronized
    override fun read(bytes: Int): ByteArray? {
        return if (!inSet)
            super.read(bytes)
        else try {
            val lengthBytes = super.read(LENGTH_BYTES)
            if (lengthBytes?.isEmpty() != false) return null

            val length = Utils.bytesToInt(lengthBytes)
            if (length < 0) return null

            val encryptedData = super.read(length)

            inCipher.doFinal(encryptedData)
        } catch (e: IllegalBlockSizeException) {
            throw IOException(e)
        } catch (e: BadPaddingException) {
            throw IOException(e)
        }
    }

    override fun read(bytes: Int, taskOwner: TaskOwner, timeout: Long, unit: TimeUnit): ByteArray? =
            taskOwner.executorService.submit<ByteArray> { read(bytes) }[timeout, unit]

    override fun write(bytes: ByteArray) {
        if (!outSet) {
            super.write(bytes)
            return
        }

        try {
            val b = outCipher.doFinal(bytes)
            super.write(Utils.intToBytes(b.size))
            super.write(b)
        } catch (e: IllegalBlockSizeException) {
            throw IOException(e)
        } catch (e: BadPaddingException) {
            throw IOException(e)
        }
    }
}