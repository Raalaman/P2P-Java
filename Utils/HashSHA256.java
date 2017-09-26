package Utils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//Esta clase de métodos estáticos sirve para
//calcular el hash de los ficheros.
public class HashSHA256 {
	public static String getHash(File fichero) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			FileInputStream fis = new FileInputStream(fichero);
			byte[] dataBytes = new byte[1024];
			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			byte[] mdbytes = md.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			fis.close();
			return sb.toString();
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getHash(String nameFichero) {
		File fichero = new File(nameFichero);
		return getHash(fichero);
	}
}