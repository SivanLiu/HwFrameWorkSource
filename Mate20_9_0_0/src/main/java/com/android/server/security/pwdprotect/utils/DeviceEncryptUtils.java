package com.android.server.security.pwdprotect.utils;

import android.security.keystore.KeyGenParameterSpec.Builder;
import android.util.Log;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class DeviceEncryptUtils {
    private static final String AES_MODE_CBC = "AES/CBC/PKCS7Padding";
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String HMACMODE = "HmacSHA256";
    private static final String KEY_ALIAS_AES = "com_huawei_securitymgr_aes_alias";
    private static final String KEY_ALIAS_HMAC = "com_huawei_securitymgr_hmac_alias";
    private static final String TAG = "PwdProtectService";

    public static byte[] deviceEncode(byte[] plainText) {
        try {
            Cipher cipher = Cipher.getInstance(AES_MODE_CBC);
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            cipher.init(1, (SecretKey) keyStore.getKey(KEY_ALIAS_AES, null));
            byte[] encodedBytes = cipher.doFinal(plainText);
            return StringUtils.byteMerger(cipher.getIV(), encodedBytes);
        } catch (IOException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deviceEncode: failed");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return new byte[0];
        }
    }

    public static byte[] deviceDecode(byte[] decodedText, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_MODE_CBC);
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            cipher.init(2, (SecretKey) keyStore.getKey(KEY_ALIAS_AES, null), new IvParameterSpec(iv));
            return cipher.doFinal(decodedText);
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deviceDecode: failed");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return new byte[0];
        }
    }

    public static SecretKey createKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", AndroidKeyStore);
            keyGenerator.init(new Builder(KEY_ALIAS_AES, 3).setBlockModes(new String[]{"CBC"}).setEncryptionPaddings(new String[]{"PKCS7Padding"}).build());
            return keyGenerator.generateKey();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createKey: failed");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static byte[] hmacSign(byte[] decodedText) {
        byte[] encodedBytes = new byte[null];
        try {
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS_HMAC, null);
            Mac mac = Mac.getInstance(HMACMODE);
            mac.init(secretKey);
            return mac.doFinal(decodedText);
        } catch (IOException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hmacSign failed");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return encodedBytes;
        }
    }

    public static SecretKey createHmacKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(HMACMODE, AndroidKeyStore);
            keyGenerator.init(new Builder(KEY_ALIAS_HMAC, 4).build());
            return keyGenerator.generateKey();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createKey: failed");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }
}
