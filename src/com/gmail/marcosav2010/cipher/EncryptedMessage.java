package com.gmail.marcosav2010.cipher;

import com.gmail.marcosav2010.common.Utils;

public class EncryptedMessage {

	private final byte[] encryptedSimmetricKeyBytes;
	private final byte[] encryptedData;
	private final byte[] byteLength;

	EncryptedMessage(byte[] encryptedSimmetricKeyBytes, byte[] encryptedData) {
		this.encryptedSimmetricKeyBytes = encryptedSimmetricKeyBytes;
		this.encryptedData = encryptedData;
		byteLength = Utils.intToBytes(~intLength());
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
		return byteLength;
	}
}
