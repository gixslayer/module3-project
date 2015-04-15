package protocol;

import java.security.AlgorithmParameterGenerator;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import utils.ByteUtils;

public class EncryptionPacket extends Packet {
	private byte[] data;
	private byte[] encryptedData;
	private byte[] key;
	private int paddingSize;
	private Cipher cipher;
	
	public EncryptionPacket() {
		super(Packet.TYPE_ENCRYPTION);
	}
	
	public EncryptionPacket(byte[] data, byte[] key) {
		super(Packet.TYPE_ENCRYPTION);
		this.data = data;
		this.key = key;
	}
	
	@Override
	protected byte[] serializeContent() {
		byte[] returnBytes = new byte[encryptedData.length + 4];
		ByteUtils.getIntBytes(paddingSize, returnBytes, encryptedData.length);
		System.arraycopy(encryptedData, 0, returnBytes, 0, encryptedData.length);
		return returnBytes;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		encryptedData = new byte[length - 4];
		System.arraycopy(buffer, offset, encryptedData, 0, length-4);
		paddingSize = ByteUtils.getIntFromBytes(buffer, offset + (length-4));
	}
	
	public void setKey(byte[] key) {
		this.key = key;
	}
	
	public byte[] getData() {
		return this.data;
	}
	
	public byte[] getEncryptedData() {
		return this.encryptedData;
	}
	
	public void encrypt() {
		try {
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
			SecretKeySpec k = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.ENCRYPT_MODE, k);
			int size = data.length % 16;
			if(size != 0) {
				size = 16 - size;
				byte[] datapad = new byte[size + data.length];
				System.arraycopy(data, 0, datapad, 0, data.length);
				data = datapad;
			}
			encryptedData = cipher.doFinal(data);
			paddingSize = size;
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) { e.printStackTrace(); }	
	}
	
	public void decrypt() {
		try {
			cipher = Cipher.getInstance("AES/ECB/NoPadding");
			SecretKeySpec k = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, k);
			byte[] buffer = cipher.doFinal(encryptedData);
			System.arraycopy(buffer, 0, data, 0, buffer.length - paddingSize);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) { e.printStackTrace(); }
	}
}
