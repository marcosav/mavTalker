package com.gmail.marcosav2010.cipher

import com.gmail.marcosav2010.common.Utils
import com.gmail.marcosav2010.communicator.BaseCommunicator
import com.gmail.marcosav2010.communicator.Communicator
import com.gmail.marcosav2010.logger.ILog
import com.gmail.marcosav2010.logger.Log
import com.gmail.marcosav2010.tasker.Task
import com.gmail.marcosav2010.tasker.TaskOwner
import com.gmail.marcosav2010.tasker.Tasker.run
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.Key
import java.security.SecureRandom
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * This is a communicator that encrypts byte arrays with a random symmetric key
 * under @SessionCipher asymmetric encryption, it also handles other message
 * parameters.
 *
 * @author Marcos
 */
class CipheredCommunicator(baseCommunicator: BaseCommunicator,
                           private val sessionCipher: SessionCipher,
                           private val taskOwner: TaskOwner) :
        Communicator() {

    private val log: ILog = Log(taskOwner, "CC")

    private val writeTask: Task
    private val writePool = WritePool()

    init {
        input = BufferedInputStream(baseCommunicator.input!!)
        output = BufferedOutputStream(baseCommunicator.output!!)

        writeTask = run(taskOwner, writePool)
    }

    @Synchronized
    fun read(): EncryptedMessage? {
        // Leer tamaño del mensaje cifrado en complemento
        val lengthBytes = input!!.readNBytes(LENGTH_BYTES)
        if (lengthBytes.isEmpty()) return null

        val length = Utils.bytesToInt(lengthBytes).inv()
        if (length < 0) return null

        // Leer parte RSA donde esta la llave AES
        val encryptedAESData = input!!.readNBytes(RSA_MSG_SIZE)

        // Leer mensaje encriptado con AES de longitud dada antes
        val encryptedData = input!!.readNBytes(length)

        return EncryptedMessage(encryptedAESData, encryptedData)
    }

    fun read(taskOwner: TaskOwner, timeout: Long, unit: TimeUnit): EncryptedMessage =
            taskOwner.executorService.submit(Callable { read() })[timeout, unit]!!

    fun decrypt(encryptedMessage: EncryptedMessage?, onDecrypt: (ByteArray) -> Unit) {
        if (encryptedMessage == null) return

        run(taskOwner) {
            try {
                val symmetricKeyBytes = sessionCipher.decode(encryptedMessage.encryptedSymmetricKeyBytes)
                val symmetricKeyData = deconstructAESData(symmetricKeyBytes)
                val decryptedSymmetricKey = symmetricKeyData[0]
                val decryptedSymmetricKeyIV = symmetricKeyData[1]

                val symmetricKey = getSecretKey(decryptedSymmetricKey)
                val gcmPS = getGCMParameterSpec(decryptedSymmetricKeyIV)

                onDecrypt.invoke(getCipher(Cipher.DECRYPT_MODE, symmetricKey, gcmPS)
                        .doFinal(encryptedMessage.encryptedData))
            } catch (e: GeneralSecurityException) {
                log.log(e)
            } catch (e: InterruptedException) {
                log.log(e)
            }
        }
    }

    override fun write(bytes: ByteArray) {
        try {
            writePool.queue(encrypt(bytes))
        } catch (ex: GeneralSecurityException) {
            log.log(ex)
        } catch (ex: InterruptedException) {
            log.log(ex)
        }
    }

    private fun encrypt(bytes: ByteArray): EncryptedMessage {
        val gcmPS = generateGCMParameterSpec()
        val iv = gcmPS.iv
        val symmetricKey = generateAESSecretKey()
        val encryptedData = getCipher(Cipher.ENCRYPT_MODE, symmetricKey, gcmPS).doFinal(bytes)
        val aesDataToRSAEncrypt = constructAESData(symmetricKey.encoded, iv)
        val encryptedAESData = sessionCipher.encode(aesDataToRSAEncrypt)

        return EncryptedMessage(encryptedAESData, encryptedData)
    }

    private fun beforeClose() {
        writePool.cancel()
        writeTask.cancelNow()
    }

    override fun close() {
        beforeClose()

        input?.close()
        output?.close()
    }

    override fun closeQuietly() {
        beforeClose()
        try {
            close()
        } catch (ignored: IOException) {
        }
    }

    private inner class WritePool : () -> Unit {

        private val queue: BlockingQueue<EncryptedMessage> = LinkedBlockingQueue()
        private val write = AtomicBoolean(true)

        override fun invoke() {
            while (write.get()) try {
                write(queue.take())
            } catch (e: IOException) {
                log.log(e)
            } catch (ignored: InterruptedException) {
            }
        }

        fun queue(msg: EncryptedMessage) = queue.offer(msg)

        private fun write(msg: EncryptedMessage) {
            output!!.write(msg.byteLength)
            output!!.write(msg.encryptedSymmetricKeyBytes)
            output!!.write(msg.encryptedData)
            output!!.flush()
        }

        fun cancel() {
            write.set(false)
            queue.clear()
        }
    }

    companion object {
        const val AES_KEY_ALGORITHM = "AES"
        const val AES_KEY_SIZE = 256
        private const val CIPHER_SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding" // JDK 14 does not recognize AES/GCM/PKCS5Padding
        private const val IV_SIZE = 96
        private const val TAG_LENGTH = 128
        private const val AES_KEY_BYTES = AES_KEY_SIZE / java.lang.Byte.SIZE
        private const val LENGTH_BYTES = Integer.BYTES
        private const val AES_DATA_SIZE = AES_KEY_BYTES + IV_SIZE /* + LENGTH_BYTES */ // For AES256: 32 + 96 + 4 = 132
        private const val RSA_MSG_SIZE = SessionCipher.RSA_KEY_SIZE / java.lang.Byte.SIZE

        private fun generateAESSecretKey(): SecretKey {
            val symmetricKeyGen = KeyGenerator.getInstance(AES_KEY_ALGORITHM)
            symmetricKeyGen.init(AES_KEY_SIZE, SecureRandom())
            return symmetricKeyGen.generateKey()
        }

        private fun generateGCMParameterSpec(): GCMParameterSpec {
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            return GCMParameterSpec(TAG_LENGTH, iv)
        }

        private fun getCipher(mode: Int, key: Key, gcmParameterSpec: GCMParameterSpec): Cipher {
            val c = Cipher.getInstance(CIPHER_SYMMETRIC_ALGORITHM)
            c.init(mode, key, gcmParameterSpec)
            return c
        }

        private fun getGCMParameterSpec(iv: ByteArray) = GCMParameterSpec(TAG_LENGTH, iv)

        private fun getSecretKey(key: ByteArray) = SecretKeySpec(key, 0, key.size, AES_KEY_ALGORITHM)

        private fun constructAESData(simKey: ByteArray, iv: ByteArray /* , byte[] length */): ByteArray {
            val out = ByteArray(AES_DATA_SIZE)
            System.arraycopy(simKey, 0, out, 0, AES_KEY_BYTES)
            System.arraycopy(iv, 0, out, AES_KEY_BYTES, IV_SIZE)
            // System.arraycopy(length, 0, out, AES_KEY_BYTES + IV_SIZE, LENGTH_BYTES);
            return out
        }

        private fun deconstructAESData(b: ByteArray): Array<ByteArray> {
            val keyBytes = ByteArray(AES_KEY_BYTES)
            val ivBytes = ByteArray(IV_SIZE)
            // byte[] lengthBytes = new byte[LENGTH_BYTES];
            System.arraycopy(b, 0, keyBytes, 0, AES_KEY_BYTES)
            System.arraycopy(b, AES_KEY_BYTES, ivBytes, 0, IV_SIZE)
            // System.arraycopy(b, AES_KEY_BYTES + IV_SIZE, lengthBytes, 0, LENGTH_BYTES);
            return arrayOf(keyBytes, ivBytes /* , lengthBytes */)
        }
    }
}