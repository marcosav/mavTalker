package com.gmail.marcosav2010.cipher

import com.gmail.marcosav2010.common.Utils.intToBytes

class EncryptedMessage internal constructor(val encryptedSymmetricKeyBytes: ByteArray, val encryptedData: ByteArray) {

    val byteLength = intToBytes(intLength().inv())

    private fun intLength() = encryptedData.size
}