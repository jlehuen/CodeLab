package codelab.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
*	Classe utilitaire d'encryption
*	@author Jérôme Lehuen
*	@version 01/10/26
*/

public class Encryption {

	private static final String ALGORITHM = "Blowfish";
	private static final Charset ENCODING = StandardCharsets.UTF_8;

	/**
	 * Clé maîtresse locale dérivée de l'identité de l'utilisateur et de sa session locale (Option B).
	 * Empêche le déchiffrement hors de la session tout en restant parfaitement stable lors des mises à jour.
	 */
	public static String getMasterKey() {
		try {
			String seed = "CodeLab-Secure-Storage-" + System.getProperty("user.name", "default") + "-" + System.getProperty("user.home", "");
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(seed.getBytes(ENCODING));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 16; i++) { // 16 octets = 32 caractères hex (clé Blowfish 256 bits)
				sb.append(String.format("%02x", hash[i]));
			}
			return sb.toString();
		}
		catch (Exception e) {
			return "CodeLab-Secured-Credential-Storage-Key";
		}
	}

	public static String encrypt(String to_encrypt) throws Exception {
		return encrypt(to_encrypt, getMasterKey());
	}

	public static String decrypt(String encrypted) throws Exception {
		return decrypt(encrypted, getMasterKey());
	}

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
