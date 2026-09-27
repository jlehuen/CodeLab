package codelab.client;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Base64;

// Librairie Apache pour la méthode clearFolder
import org.apache.commons.io.FileUtils;

// Librairie Jackson pour la méthode printAsJson
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

public class MessagePackUtils {

	public static void wait(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] convertObjectArrayToByteArray(Object[] objectArray) {
		// Créer un tableau byte[] de la même taille que l'Object[]
		byte[] byteArray = new byte[objectArray.length];
		// Parcourir l'Object[] et extraire chaque Integer pour remplir le byte[]
		for (int i = 0; i < objectArray.length; i++) {
			// Cast de Integer en int puis en byte
			byteArray[i] = (byte) (int) objectArray[i];
		}
		return byteArray;
	}

	public static String encodeSHA256(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	/*
	Caractères problématiques dans Base64 standard
	Le Base64 standard utilise ces caractères : A-Z, a-z, 0-9, +, /, =
	Les caractères + et / peuvent poser problème lors de la transmission réseau ou du parsing,

	# Ces mots de passe génèrent des hashes Base64 avec + et /
	echo -n "password123" | sha256sum | xxd -r -p | base64
	echo -n "test" | sha256sum | xxd -r -p | base64

	# Comparez avec l'URL-safe
	echo -n "password123" | sha256sum | xxd -r -p | base64 | tr '+/' '-_' | tr -d '='

	public static String encodeSHA256(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
			// Utilise - et _ au lieu de + et /, sans padding =
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	*/

	public static boolean cleanDirectory(String path) {
		try {
			File folder = new File(path);
			FileUtils.cleanDirectory(folder);
			return true;
		}
		catch (IOException e) {
			System.err.println("Erreur lors de la suppression du contenu de " + path);
			return false;
		}
	}
	
	public static void printAsJson(byte[] data) {
		// Affiche un message MessagePack en JSON
		ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
		try {
			Object deserialized = objectMapper.readValue(data, Object.class);
			String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(deserialized);
			System.out.println("MessagePack (JSON): " + json);
		}
		catch (Exception e) {
			System.out.println("Error converting MessagePack to JSON: " + e.getMessage());
		}
	}
}
