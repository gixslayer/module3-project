package protocol;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionPacket extends Packet {
	private byte[] data;
	private byte[] encryptedData;
	private byte[] key;
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
		return encryptedData;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		System.arraycopy(buffer, offset, encryptedData, 0, length);
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
			cipher = Cipher.getInstance("AES");
			SecretKeySpec k = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.ENCRYPT_MODE, k);
			encryptedData = cipher.doFinal(data);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) { e.printStackTrace(); }	
	}
	
	public void decrypt() {
		try {
			cipher = Cipher.getInstance("AES");
			SecretKeySpec k = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, k);
			data = cipher.doFinal(encryptedData);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) { e.printStackTrace(); }
	}
}
