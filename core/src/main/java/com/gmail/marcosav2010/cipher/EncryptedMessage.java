package com.gmail.marcosav2010.cipher;

import com.gmail.marcosav2010.common.Utils;
import lombok.Getter;

public class EncryptedMessage {

    @Getter
    private final byte[] encryptedSimmetricKeyBytes, encryptedData, byteLength;

    EncryptedMessage(byte[] encryptedSimmetricKeyBytes, byte[] encryptedData) {
        this.encryptedSimmetricKeyBytes = encryptedSimmetricKeyBytes;
        this.encryptedData = encryptedData;
        byteLength = Utils.intToBytes(~intLength());
    }

    int intLength() {
        return encryptedData.length;
    }
}
