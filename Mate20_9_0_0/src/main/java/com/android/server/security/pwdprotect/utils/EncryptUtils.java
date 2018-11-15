package com.android.server.security.pwdprotect.utils;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {
    private static final String CBC_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int MAX_ENCRYPT_BLOCK = 117;
    private static String RSA = "RSA";
    private static final String TAG = "PwdProtectService";

    public static KeyPair generateRSAKeyPair(int keyLength) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA);
            kpg.initialize(keyLength);
            return kpg.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "generate RSA KeyPair failed");
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x003a A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x003a A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x003a A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x003a A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x003a A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Missing block: B:12:0x003a, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:0x003b, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("RSA encryptData error");
            r3.append(r1.getMessage());
            android.util.Log.e(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:14:0x0057, code:
            return new byte[0];
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] encryptData(byte[] data, PublicKey publicKey) {
        try {
            byte[] cache;
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(1, publicKey);
            int inputLen = data.length;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = 0;
            int i = 0;
            while (inputLen - offSet > 0) {
                if (inputLen - offSet > 117) {
                    cache = cipher.doFinal(data, offSet, 117);
                } else {
                    cache = cipher.doFinal(data, offSet, inputLen - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * 117;
            }
            cache = out.toByteArray();
            out.close();
            return cache;
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x000f A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x000f A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x000f A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x000f A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:3:0x000f, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:4:0x0010, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("RSA decryptData error");
            r2.append(r0.getMessage());
            android.util.Log.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:5:0x002d, code:
            return new byte[0];
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] decryptData(byte[] encryptedData, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(2, privateKey);
            return cipher.doFinal(encryptedData);
        } catch (GeneralSecurityException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:4:0x001c, code:
            android.util.Log.e(TAG, "AES Encrypt failed");
     */
    /* JADX WARNING: Missing block: B:5:0x0026, code:
            return new byte[0];
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] aesCbcEncode(byte[] plainText, byte[] key, byte[] IVParameter) {
        try {
            IvParameterSpec ivParameterSpec = new IvParameterSpec(IVParameter);
            Cipher cipher = Cipher.getInstance(CBC_CIPHER_ALGORITHM);
            cipher.init(1, new SecretKeySpec(key, "AES"), ivParameterSpec);
            return cipher.doFinal(plainText);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:1:0x0005} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:1:0x0005} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:1:0x0005} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:1:0x0005} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x001b A:{ExcHandler: java.security.NoSuchAlgorithmException (e java.security.NoSuchAlgorithmException), Splitter: B:1:0x0005} */
    /* JADX WARNING: Missing block: B:5:0x001c, code:
            android.util.Log.e(TAG, "AES Decode failed");
     */
    /* JADX WARNING: Missing block: B:6:0x0026, code:
            return new byte[0];
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] aesCbcDecode(byte[] decodedText, byte[] key, byte[] IVParameter) {
        IvParameterSpec ivParameterSpec = new IvParameterSpec(IVParameter);
        try {
            Cipher cipher = Cipher.getInstance(CBC_CIPHER_ALGORITHM);
            cipher.init(2, new SecretKeySpec(key, "AES"), ivParameterSpec);
            return cipher.doFinal(decodedText);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0010 A:{ExcHandler: java.security.spec.InvalidKeySpecException (r0_1 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:3:0x0010, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:4:0x0011, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("getPublicKey error");
            r2.append(r0.getMessage());
            android.util.Log.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:5:0x002c, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static PublicKey getPublicKey(byte[] keyBytes) {
        try {
            return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x0010 A:{ExcHandler: java.security.spec.InvalidKeySpecException (r1_2 'e' java.security.GeneralSecurityException), Splitter: B:1:0x0005} */
    /* JADX WARNING: Missing block: B:4:0x0010, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0011, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("getPrivateKey error");
            r3.append(r1.getMessage());
            android.util.Log.e(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:6:0x002c, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static PrivateKey getPrivateKey(byte[] keyBytes) {
        try {
            return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException e) {
        }
    }
}
