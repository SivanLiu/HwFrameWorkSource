package com.android.server.location;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HwCryptoUtility {

    public static class AESLocalDbCrypto {
        public static String encrypt(String key, String plaintext) throws Exception {
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] ivBytes = new byte[16];
            new SecureRandom().nextBytes(ivBytes);
            cipher.init(1, skeySpec, new IvParameterSpec(ivBytes));
            String ciphertextStr = Sha256Encrypt.bytes2Hex(cipher.doFinal(plaintext.getBytes("UTF-8")));
            String ivStr = Sha256Encrypt.bytes2Hex(ivBytes);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ivStr);
            stringBuilder.append(ciphertextStr);
            return stringBuilder.toString();
        }

        public static String decrypt(String key, String encrypted) throws Exception {
            byte[] ivBytes = Sha256Encrypt.hex2Bytes(encrypted.substring(0, 32).toCharArray());
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, skeySpec, new IvParameterSpec(ivBytes));
            return new String(cipher.doFinal(Sha256Encrypt.hex2Bytes(encrypted.substring(32, encrypted.length()).toCharArray())));
        }
    }

    static class Sha256Encrypt {
        Sha256Encrypt() {
        }

        public static String bytes2Hex(byte[] bts) {
            StringBuffer des = new StringBuffer();
            for (byte b : bts) {
                String tmp = Integer.toHexString(b & 255);
                if (tmp.length() == 1) {
                    des.append("0");
                }
                des.append(tmp);
            }
            return des.toString();
        }

        /* JADX WARNING: Missing block: B:4:0x000e, code:
            if (r6.equals("") != false) goto L_0x0013;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static String Encrypt(String strSrc, String encName) {
            byte[] bt = strSrc.getBytes();
            if (encName != null) {
                try {
                } catch (NoSuchAlgorithmException e) {
                    return null;
                }
            }
            encName = "SHA-256";
            MessageDigest md = MessageDigest.getInstance(encName);
            md.update(bt);
            return bytes2Hex(md.digest());
        }

        public static byte[] hex2Bytes(char[] data) {
            int len = data.length;
            byte[] out = new byte[(len >> 1)];
            int i = 0;
            int j = 0;
            while (j < len) {
                j++;
                j++;
                out[i] = (byte) (((Character.digit(data[j], 16) << 4) | Character.digit(data[j], 16)) & 255);
                i++;
            }
            return out;
        }
    }
}
