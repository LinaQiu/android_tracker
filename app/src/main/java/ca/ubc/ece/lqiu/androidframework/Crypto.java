package ca.ubc.ece.lqiu.androidframework;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by lina on 16-02-22.
 * This class is created to deal with crypto-related stuff. For example, data encryption/decryption,
 * encryption/decryption key production, and etc.
 */

public class Crypto {

    public static String encrypt(byte[] raw, byte[] plain) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encrypted = cipher.doFinal(plain);
        return toHex(encrypted);
    }

    public static String decrypt(byte[] raw, String cipherText)
            throws Exception {
        byte[] encrypted = toByte(cipherText);
        SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted);
    }

    public static String encrypt_key(byte[] rawKey, String pubKeyPath) throws InvalidKeyException,
            GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        // void init(int opmode, Key key): Initializes this cipher with a key.
        cipher.init(Cipher.ENCRYPT_MODE, readPublicKey(pubKeyPath));

        // byte [] doFinal(byte[] input): Encrypts or decrypts data in a single-part operation,
        // or finishes a multiple-part operation.
        byte[] encryptedKey = cipher.doFinal(rawKey);
        return toHex(encryptedKey);
    }

    public static PublicKey readPublicKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
        // read the encoded public key bytes

        FileInputStream keyFile = new FileInputStream(path);
        byte[] encKey = new byte[keyFile.available()];
        keyFile.read(encKey);
        keyFile.close();

        // generate public key from the encoded its encoding
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);
        // Key factories are used to convert keys (opaque cryptographic keys of type Key)
        // into key specifications (transparent representations of the underlying key material),
        // and vice versa.
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(pubKeySpec);
    }

    public static String decrypt_key(String cipherKey, String prKeyPath) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IOException, IllegalBlockSizeException, BadPaddingException
    {
        byte[] encryptedKey = toByte(cipherKey);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, readPrivateKey(prKeyPath));
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        return toHex(decryptedKey);
    }

    public static PrivateKey readPrivateKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
        // read the encoded public key bytes
        FileInputStream keyFile = new FileInputStream(path);
        byte[] encKey = new byte[keyFile.available()];
        keyFile.read(encKey);
        keyFile.close();

        // generate public key from the encoded its encoding
        // private key?
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(privateKeySpec);
    }

    public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
                    16).byteValue();
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private final static String HEX = "0123456789ABCDEF";

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        // Generate a 128-bit key
        final int outputKeyLength = 256;

        SecureRandom secureRandom = new SecureRandom();

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(outputKeyLength, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey;
    }

    public static void generateKeyPair(String path)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            IOException {
        // Create a Key Pair Generator
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        // Initialize the Key Pair Generator
        SecureRandom secureRandom = new SecureRandom();
        keyGen.initialize(1024, secureRandom);

        // Generate private and public keys
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Save public keys
        byte[] storedPublicKey = publicKey.getEncoded();
        FileOutputStream publicKeyStoreFile = new FileOutputStream(path + "/rsa.pub");
        publicKeyStoreFile.write(storedPublicKey);
        publicKeyStoreFile.close();

        // Save private keys
        byte[] storedPrivateKey = privateKey.getEncoded();
        FileOutputStream privateKeyStoreFile = new FileOutputStream(path + "/rsa");
        privateKeyStoreFile.write(storedPrivateKey);
        privateKeyStoreFile.close();
    }

}
