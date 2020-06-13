package com.gmail.marcosav2010.handshake

import com.gmail.marcosav2010.common.PublicIPResolver.publicAddress
import com.gmail.marcosav2010.common.Utils.concat
import com.gmail.marcosav2010.common.Utils.decode
import com.gmail.marcosav2010.common.Utils.encode
import com.gmail.marcosav2010.communicator.BaseCommunicator
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.logger.Logger.VerboseLevel
import com.gmail.marcosav2010.peer.Peer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 * This class manages the handshake of a starting @Connection
 *
 * @author Marcos
 */
class HandshakeAuthenticator(private val peer: Peer) {

    private val log: ILog = Log(peer, "HA")

    private val random = SecureRandom()
    private val localStorage: MutableMap<String, ConnectionToken> = ConcurrentHashMap()
    private val localTempStorage: MutableMap<String, ConnectionToken> = ConcurrentHashMap()
    private val remoteStorage: MutableMap<InetSocketAddress, ConnectionToken> = ConcurrentHashMap()

    @get:Synchronized
    var connectionKey: ByteArray? = null
        get() {
            if (field != null)
                return field

            field = ByteArray(C_KEY_LENGTH)
            random.nextBytes(field)
            markBytes(field!!, C_KEY_MARK)

            return field
        }
        private set

    private var publicConnectionToken: ConnectionToken? = null
    private var publicAddressKey: String? = null

    val connectionKeyString: String
        get() = encode(connectionKey!!)

    fun generatePublicAddressKey(): String {
        if (publicAddressKey != null)
            return publicAddressKey!!

        publicConnectionToken = ConnectionToken(generateHandshakeKey(), generateBaseKey())
        return generateAddressKey(PUBLIC_CONNECTION_KEY, publicConnectionToken!!, getMark(PUBLIC_KEY))
                .also { publicAddressKey = it }
    }

    fun generatePrivateAddressKey(requesterConnectionKeyC: CharArray): String {
        val requesterConnectionKey = String(requesterConnectionKeyC)
        require(!localStorage.containsKey(requesterConnectionKey)) { "There is already an Address Key for that Connection Key." }
        val ct = ConnectionToken(generateHandshakeKey(), generateBaseKey())
        localStorage[requesterConnectionKey] = ct

        return generateAddressKey(decode(requesterConnectionKey), ct, getMark(PRIVATE_KEY))
    }

    private fun generateAddressKey(requesterConnectionKeyBytes: ByteArray, ct: ConnectionToken, mark: Byte): String {
        require(requesterConnectionKeyBytes.size == C_KEY_LENGTH) { "Invalid connection key format." }
        checkMark(requesterConnectionKeyBytes, C_KEY_MARK)

        val ip = publicAddress
        val ipBytes = ip!!.address
        val handshakeKey = ct.handshakeKey
        val baseKey = ct.baseKey

        val addressKeyBytes = ByteBuffer.allocate(RAW_ADDRESS_KEY_LENGTH).put(ipBytes).putInt(peer.port)
                .put(baseKey).put(handshakeKey).array()

        val c = Cipher.getInstance("AES")
        val password = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM).digest(requesterConnectionKeyBytes)
        val secretKey: SecretKey = SecretKeySpec(password, "AES")

        c.init(Cipher.ENCRYPT_MODE, secretKey)

        val cipheredBytes = c.doFinal(addressKeyBytes)
        val markedBytes = ByteArray(cipheredBytes.size + 1)

        System.arraycopy(cipheredBytes, 0, markedBytes, 0, AK_KEY_MARK)
        System.arraycopy(cipheredBytes, AK_KEY_MARK, markedBytes, AK_KEY_MARK + 1, cipheredBytes.size - AK_KEY_MARK)

        markedBytes[AK_KEY_MARK] = mark

        return encode(markedBytes)
    }

    fun parseAddressKey(remoteAddressKey: CharArray): ConnectionToken {
        val remoteAddressKeyBytes = decode(String(remoteAddressKey))
        val unmarkedBytes = ByteArray(remoteAddressKeyBytes.size - 1)

        System.arraycopy(remoteAddressKeyBytes, 0, unmarkedBytes, 0, AK_KEY_MARK)
        System.arraycopy(remoteAddressKeyBytes, AK_KEY_MARK + 1, unmarkedBytes, AK_KEY_MARK,
                unmarkedBytes.size - AK_KEY_MARK)

        val mark = remoteAddressKeyBytes[AK_KEY_MARK]
        val type = mark and KEY_TYPE_BITMASK.toByte()

        return parseAddressKey(unmarkedBytes, type)
    }

    private fun parseAddressKey(remoteAddressKeyBytes: ByteArray, type: Byte): ConnectionToken {
        val connectionKey: ByteArray
        var isPublic = false

        when (type) {
            PRIVATE_KEY -> connectionKey = this.connectionKey!!
            PUBLIC_KEY -> {
                connectionKey = PUBLIC_CONNECTION_KEY
                isPublic = true
            }
            else -> throw IllegalArgumentException("Invalid address key type.")
        }

        val parsedIpBytes = ByteArray(HOST_LENGTH)
        val parsedPortBytes = ByteArray(PORT_LENGTH)
        val baseKeyBytes = ByteArray(B_KEY_LENGTH)
        val handshakeKeyBytes = ByteArray(H_KEY_LENGTH)

        val password = MessageDigest.getInstance(PASSWORD_HASH_ALGORITHM).digest(connectionKey)

        val secretKey = SecretKeySpec(password, "AES")
        val c = Cipher.getInstance("AES")

        c.init(Cipher.DECRYPT_MODE, secretKey)

        val decipheredBytes = c.doFinal(remoteAddressKeyBytes)

        require(decipheredBytes.size == RAW_ADDRESS_KEY_LENGTH) { "Invalid address key format." }

        System.arraycopy(decipheredBytes, 0, parsedIpBytes, 0, HOST_LENGTH)
        System.arraycopy(decipheredBytes, HOST_LENGTH, parsedPortBytes, 0, PORT_LENGTH)
        System.arraycopy(decipheredBytes, PORT_LENGTH + HOST_LENGTH, baseKeyBytes, 0, B_KEY_LENGTH)
        System.arraycopy(decipheredBytes, PORT_LENGTH + HOST_LENGTH + B_KEY_LENGTH, handshakeKeyBytes, 0, H_KEY_LENGTH)

        val p = ByteBuffer.wrap(parsedPortBytes).int
        val address = InetSocketAddress(InetAddress.getByAddress(parsedIpBytes), p)

        return registerHandshakeKey(address,
                ConnectionToken(handshakeKeyBytes, address, baseKeyBytes).setPublic(isPublic))
    }

    fun sendHandshake(b: BaseCommunicator, a: InetSocketAddress): ConnectionToken? {
        log.log("Sending handshake to remote...", VerboseLevel.HIGH)

        var hk = EMPTY_HANDSHAKE_KEY
        var ck: ByteArray? = PUBLIC_CONNECTION_KEY
        var ct: ConnectionToken? = null

        if (remoteStorage.containsKey(a)) {
            hk = remoteStorage.remove(a).also { ct = it }!!.handshakeKey
            if (!ct!!.isPublic)
                ck = connectionKey
        }

        b.write(concat(ck!!, hk))
        return ct
    }

    fun readHandshake(remoteSocket: Socket): ConnectionToken? {
        log.log("Waiting for handshake, timeout set to " + HANDSHAKE_TIMEOUT + "s...", VerboseLevel.HIGH)

        val b = ByteArray(C_KEY_LENGTH + H_KEY_LENGTH)
        peer.executorService.submit<Int> { remoteSocket.getInputStream().read(b) }[HANDSHAKE_TIMEOUT, TimeUnit.SECONDS]

        return handle(b)
    }

    private fun handle(b: ByteArray): ConnectionToken? {
        val connectionKeyBytes = ByteArray(C_KEY_LENGTH)
        val handshakeKey = ByteArray(H_KEY_LENGTH)

        System.arraycopy(b, 0, connectionKeyBytes, 0, C_KEY_LENGTH)
        System.arraycopy(b, C_KEY_LENGTH, handshakeKey, 0, H_KEY_LENGTH)

        if (hrl == HandshakeRequirementLevel.NONE)
            if (EMPTY_HANDSHAKE_KEY.contentEquals(handshakeKey)
                    && PUBLIC_CONNECTION_KEY.contentEquals(connectionKeyBytes))
                return null

        if (hrl <= HandshakeRequirementLevel.PUBLIC)
            if (publicConnectionToken != null &&
                    PUBLIC_CONNECTION_KEY.contentEquals(connectionKeyBytes) &&
                    publicConnectionToken!!.handshakeKey.contentEquals(handshakeKey))
                return publicConnectionToken

        val connectionKey = encode(connectionKeyBytes)

        if (localStorage.containsKey(connectionKey)
                && localStorage[connectionKey]!!.handshakeKey.contentEquals(handshakeKey))
            return localStorage.remove(connectionKey)

        val hKey = encode(handshakeKey)

        if (localTempStorage.containsKey(hKey))
            return localTempStorage.remove(hKey)

        throw InvalidHandshakeKey()
    }

    fun generateTemporalHandshakeKey(): ConnectionToken {
        val handshakeKey = generateHandshakeKey()
        val ct = ConnectionToken(handshakeKey, generateBaseKey())
        localTempStorage[ct.handshakeKeyAsString] = ct
        return ct
    }

    private fun generateBaseKey(): ByteArray {
        val baseKey = ByteArray(B_KEY_LENGTH)
        random.nextBytes(baseKey)
        return baseKey
    }

    private fun generateHandshakeKey(): ByteArray {
        val handshakeKey = ByteArray(H_KEY_LENGTH)
        random.nextBytes(handshakeKey)
        return handshakeKey
    }

    private fun registerHandshakeKey(address: InetSocketAddress, token: ConnectionToken): ConnectionToken {
        remoteStorage[address] = token
        return token
    }

    fun storeHandshakeKey(address: InetSocketAddress, handshakeKeyBytes: ByteArray, baseKeyBytes: ByteArray) =
            registerHandshakeKey(address, ConnectionToken(handshakeKeyBytes, baseKeyBytes))

    private val hrl: HandshakeRequirementLevel
        get() = peer.properties.hrl

    enum class HandshakeRequirementLevel {
        NONE, PUBLIC, PRIVATE
    }

    companion object {
        const val B_KEY_LENGTH = 128 / 8
        const val H_KEY_LENGTH = B_KEY_LENGTH
        private const val HANDSHAKE_TIMEOUT = 10L
        private const val C_KEY_MARK = 3
        private const val AK_KEY_MARK = 13
        private const val KEY_TYPE_BITMASK = 7
        private const val PUBLIC_KEY: Byte = 0
        private const val PRIVATE_KEY: Byte = 1
        private const val C_KEY_LENGTH = 128 / 8
        private const val HOST_LENGTH = Byte.SIZE_BYTES * 4
        private const val PORT_LENGTH = Integer.BYTES
        private val PUBLIC_CONNECTION_KEY = markBytes(String(CharArray(C_KEY_LENGTH)).toByteArray(), C_KEY_MARK)
        private val EMPTY_HANDSHAKE_KEY = ByteArray(H_KEY_LENGTH)
        private const val PASSWORD_HASH_ALGORITHM = "SHA3-256"
        private const val RAW_ADDRESS_KEY_LENGTH = HOST_LENGTH + PORT_LENGTH + B_KEY_LENGTH + H_KEY_LENGTH

        private fun getMark(type: Byte): Byte {
            return type
        }

        private fun markBytes(bytes: ByteArray, pos: Int): ByteArray {
            var count: Byte = Byte.MIN_VALUE

            for (i in bytes.indices)
                if (i != pos)
                    count = (count + Integer.bitCount(java.lang.Byte.toUnsignedInt(bytes[i])).toByte()).toByte()

            bytes[pos] = count
            return bytes
        }

        private fun checkMark(bytes: ByteArray, pos: Int) {
            var count: Byte = Byte.MIN_VALUE

            for (i in bytes.indices)
                if (i != pos)
                    count = (count + Integer.bitCount(java.lang.Byte.toUnsignedInt(bytes[i])).toByte()).toByte()

            require(count == bytes[pos]) { "Invalid connection key format." }
        }
    }
}