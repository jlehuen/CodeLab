package codelab.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
*	Classe utilitaire d'encryption
*	@author Jérôme Lehuen
*	@version 16/12/23
*/

public class Encryption {

	private static final String ALGORITHM = "Blowfish";
	private static final Charset ENCODING = StandardCharsets.UTF_8;

	public static String encrypt(String to_encrypt, String masterkey) throws Exception {
		Key key = new SecretKeySpec(masterkey.getBytes(ENCODING), ALGORITHM);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] data = cipher.doFinal(to_encrypt.getBytes(ENCODING));
		return Base64.getEncoder().encodeToString(data);
	}

	public static String decrypt(String encrypted, String masterkey) throws Exception {
		Key key = new SecretKeySpec(masterkey.getBytes(ENCODING), ALGORITHM);
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key);
		byte[] data = Base64.getDecoder().decode(encrypted);
		return new String(cipher.doFinal(data), ENCODING);
	}
}
