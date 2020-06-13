package com.gmail.marcosav2010.cipher

import com.gmail.marcosav2010.connection.Connection
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * This class manages the asymmetric encryption between peers.
 *
 * @author Marcos
 */
class SessionCipher private constructor(private val connection: Connection) {

    companion object {
        const val RSA_KEY_ALGORITHM = "RSA"
        const val RSA_KEY_SIZE = 4096
        const val RSA_KEY_MSG = RSA_KEY_SIZE / Byte.SIZE_BITS + 38

        private fun getPublicKey(key: ByteArray) =
                KeyFactory.getInstance(RSA_KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(key))

        fun create(connection: Connection) = SessionCipher(connection)
    }

    private lateinit var publicKey: PublicKey

    private var input: CipherPool? = null
    private var output: CipherPool? = null

    private var isAuth = false

    fun generate() {
        val kPairGen = KeyPairGenerator.getInstance(RSA_KEY_ALGORITHM)
        kPairGen.initialize(RSA_KEY_SIZE)
        val keyPair = kPairGen.generateKeyPair()
        input = CipherPool(Cipher.PRIVATE_KEY, keyPair.private, 2, 15)
        publicKey = keyPair.public
    }

    fun sendAuthentication() {
        val keyBytes = publicKey.encoded
        connection.writeRawBytes(keyBytes)
        isAuth = true
        checkDone()
    }

    fun loadAuthenticationResponse(bytes: ByteArray) {
        val remotePublicKey = getPublicKey(bytes)
        output = CipherPool(Cipher.PUBLIC_KEY, remotePublicKey, 1, 10)
        checkDone()
    }

    val isWaitingForRemoteAuth: Boolean
        get() = output == null

    private val isDone: Boolean
        get() = isAuth && !isWaitingForRemoteAuth

    private fun checkDone() {
        if (isDone) connection.onAuth()
    }

    fun decode(bytes: ByteArray): ByteArray {
        checkNotNull(input) { "There is no decoding cipher pool created." }
        return input!!.doFinal(bytes)
    }

    fun encode(bytes: ByteArray): ByteArray {
        checkNotNull(output) { "There is no encoding cipher pool created." }
        return output!!.doFinal(bytes)
    }
}