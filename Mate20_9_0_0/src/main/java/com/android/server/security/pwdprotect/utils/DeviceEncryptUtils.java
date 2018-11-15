package com.android.server.security.pwdprotect.utils;

import android.security.keystore.KeyGenParameterSpec.Builder;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class DeviceEncryptUtils {
    private static final String AES_MODE_CBC = "AES/CBC/PKCS7Padding";
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String HMACMODE = "HmacSHA256";
    private static final String KEY_ALIAS_AES = "com_huawei_securitymgr_aes_alias";
    private static final String KEY_ALIAS_HMAC = "com_huawei_securitymgr_hmac_alias";
    private static final String TAG = "PwdProtectService";

    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Removed duplicated region for block: B:5:0x0032 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Missing block: B:5:0x0032, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:6:0x0033, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("deviceEncode: failed");
            r3.append(r1.getMessage());
            android.util.Log.e(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:7:0x004f, code:
            return new byte[0];
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] deviceEncode(byte[] plainText) {
        try {
            Cipher cipher = Cipher.getInstance(AES_MODE_CBC);
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            cipher.init(1, (SecretKey) keyStore.getKey(KEY_ALIAS_AES, null));
            byte[] encodedBytes = cipher.doFinal(plainText);
            return StringUtils.byteMerger(cipher.getIV(), encodedBytes);
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0026 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:3:0x0026, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:4:0x0027, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("deviceDecode: failed");
            r2.append(r0.getMessage());
            android.util.Log.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:5:0x0044, code:
            return new byte[0];
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] deviceDecode(byte[] decodedText, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_MODE_CBC);
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            cipher.init(2, (SecretKey) keyStore.getKey(KEY_ALIAS_AES, null), new IvParameterSpec(iv));
            return cipher.doFinal(decodedText);
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0030 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0030 A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:3:0x0030, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:4:0x0031, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("createKey: failed");
            r2.append(r0.getMessage());
            android.util.Log.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:5:0x004c, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static SecretKey createKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", AndroidKeyStore);
            keyGenerator.init(new Builder(KEY_ALIAS_AES, 3).setBlockModes(new String[]{"CBC"}).setEncryptionPaddings(new String[]{"PKCS7Padding"}).build());
            return keyGenerator.generateKey();
        } catch (GeneralSecurityException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x0024 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0003} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0024 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0003} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0024 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0003} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0024 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0003} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x0024 A:{ExcHandler: java.security.NoSuchAlgorithmException (r1_2 'e' java.lang.Exception), Splitter: B:1:0x0003} */
    /* JADX WARNING: Missing block: B:4:0x0024, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0025, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("hmacSign failed");
            r3.append(r1.getMessage());
            android.util.Log.e(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:6:?, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] hmacSign(byte[] decodedText) {
        byte[] encodedBytes = new byte[null];
        try {
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS_HMAC, null);
            Mac mac = Mac.getInstance(HMACMODE);
            mac.init(secretKey);
            return mac.doFinal(decodedText);
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x001c A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x001c A:{ExcHandler: java.security.NoSuchAlgorithmException (r0_2 'e' java.security.GeneralSecurityException), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:3:0x001c, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:4:0x001d, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("createKey: failed");
            r2.append(r0.getMessage());
            android.util.Log.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:5:0x0038, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static SecretKey createHmacKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(HMACMODE, AndroidKeyStore);
            keyGenerator.init(new Builder(KEY_ALIAS_HMAC, 4).build());
            return keyGenerator.generateKey();
        } catch (GeneralSecurityException e) {
        }
    }
}
