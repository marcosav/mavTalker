package com.gmail.marcosav2010.cipher;

import com.gmail.marcosav2010.common.Utils;

public class EncryptedMessage {

	private final byte[] encryptedSimmetricKeyBytes;
	private final byte[] encryptedData;

	EncryptedMessage(byte[] encryptedSimmetricKeyBytes, byte[] encryptedData) {
		this.encryptedSimmetricKeyBytes = encryptedSimmetricKeyBytes;
		this.encryptedData = encryptedData;
	}

	byte[] getEncryptedSimmetricKeyBytes() {
		return encryptedSimmetricKeyBytes;
	}

	byte[] getEncryptedData() {
		return encryptedData;
	}
	
	int intLength() {
		return encryptedData.length;
	}
	
	byte[] byteLength() {
		return Utils.intToBytes(~intLength());
	}
}
