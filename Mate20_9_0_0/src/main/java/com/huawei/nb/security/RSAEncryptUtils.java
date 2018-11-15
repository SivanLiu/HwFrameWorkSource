package com.huawei.nb.security;

import com.huawei.nb.utils.logger.DSLog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAEncryptUtils {
    private static final String CIPHER_ALGORITHM = "RSA/NONE/OAEPwithSHA-1andMGF1Padding";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final int MAX_DECRYPT_BLOCK = 256;
    private static final int MAX_ENCRYPT_BLOCK = 128;
    private static final String PRIVATE_KEY = "RSAPrivateKey";
    private static final String PUBLIC_KEY = "RSAPublicKey";

    public static Map<String, Object> generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            Map<String, Object> keyMap = new HashMap(2);
            keyMap.put(PUBLIC_KEY, publicKey);
            keyMap.put(PRIVATE_KEY, privateKey);
            return keyMap;
        } catch (NoSuchAlgorithmException e) {
            DSLog.e("Failed to generate key pair, error: NoSuchAlgorithmException.", new Object[0]);
            return null;
        }
    }

    public static String getPrivateKey(Map<String, Object> keyMap) {
        if (keyMap == null) {
            return null;
        }
        return encodeToString(((Key) keyMap.get(PRIVATE_KEY)).getEncoded());
    }

    public static String getPublicKey(Map<String, Object> keyMap) {
        if (keyMap == null) {
            return null;
        }
        return encodeToString(((Key) keyMap.get(PUBLIC_KEY)).getEncoded());
    }

    private static String encodeToString(byte[] data) {
        if (data != null) {
            return Base64.getEncoder().encodeToString(data);
        }
        return null;
    }

    private static byte[] decodeToByte(String str) {
        if (str != null) {
            return Base64.getDecoder().decode(str);
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0051 A:{SYNTHETIC, Splitter: B:26:0x0051} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0051 A:{SYNTHETIC, Splitter: B:26:0x0051} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0051 A:{SYNTHETIC, Splitter: B:26:0x0051} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0061 A:{SYNTHETIC, Splitter: B:32:0x0061} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static byte[] processByCipher(byte[] data, Cipher cipher, int maxCountOnce) {
        byte[] toByteArray;
        GeneralSecurityException e;
        GeneralSecurityException e2;
        Throwable th;
        ByteArrayOutputStream out = null;
        try {
            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
            int offSet = 0;
            try {
                int inputLen = data.length;
                while (inputLen - offSet > 0) {
                    byte[] cache;
                    if (inputLen - offSet > maxCountOnce) {
                        cache = cipher.doFinal(data, offSet, maxCountOnce);
                    } else {
                        cache = cipher.doFinal(data, offSet, inputLen - offSet);
                    }
                    out2.write(cache, 0, cache.length);
                    offSet += maxCountOnce;
                }
                toByteArray = out2.toByteArray();
                if (out2 != null) {
                    try {
                        out2.close();
                    } catch (IOException e3) {
                        DSLog.e("Failed to close ByteArrayOutputStream.", new Object[0]);
                    }
                }
                out = out2;
            } catch (IllegalBlockSizeException e4) {
                e = e4;
                out = out2;
                e2 = e;
                try {
                    DSLog.e("Failed to process data by cipher, error: %s.", e2.getClass().getSimpleName());
                    toByteArray = new byte[0];
                    if (out != null) {
                    }
                    return toByteArray;
                } catch (Throwable th2) {
                    th = th2;
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e5) {
                            DSLog.e("Failed to close ByteArrayOutputStream.", new Object[0]);
                        }
                    }
                    throw th;
                }
            } catch (BadPaddingException e6) {
                e = e6;
                out = out2;
                e2 = e;
                DSLog.e("Failed to process data by cipher, error: %s.", e2.getClass().getSimpleName());
                toByteArray = new byte[0];
                if (out != null) {
                }
                return toByteArray;
            } catch (Throwable th3) {
                th = th3;
                out = out2;
                if (out != null) {
                }
                throw th;
            }
        } catch (IllegalBlockSizeException e7) {
            e = e7;
            e2 = e;
            DSLog.e("Failed to process data by cipher, error: %s.", e2.getClass().getSimpleName());
            toByteArray = new byte[0];
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e8) {
                    DSLog.e("Failed to close ByteArrayOutputStream.", new Object[0]);
                }
            }
            return toByteArray;
        } catch (BadPaddingException e9) {
            e = e9;
            e2 = e;
            DSLog.e("Failed to process data by cipher, error: %s.", e2.getClass().getSimpleName());
            toByteArray = new byte[0];
            if (out != null) {
            }
            return toByteArray;
        }
        return toByteArray;
    }

    private static byte[] encrypt(byte[] data, Key key) {
        GeneralSecurityException e;
        if (data == null || data.length == 0 || key == null) {
            DSLog.e("Failed to encrypt data, error: invalid parameters.", new Object[0]);
            return new byte[0];
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(1, key);
            return processByCipher(data, cipher, MAX_ENCRYPT_BLOCK);
        } catch (NoSuchAlgorithmException e2) {
            e = e2;
        } catch (NoSuchPaddingException e3) {
            e = e3;
        } catch (InvalidKeyException e4) {
            e = e4;
        }
        DSLog.e("Failed to encrypt data, error: %s.", e.getClass().getSimpleName());
        return new byte[0];
    }

    private static byte[] decrypt(byte[] data, Key key) {
        GeneralSecurityException e;
        if (data == null || data.length == 0 || key == null) {
            DSLog.e("Failed to decrypt data, error: invalid parameters.", new Object[0]);
            return new byte[0];
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(2, key);
            return processByCipher(data, cipher, 256);
        } catch (NoSuchAlgorithmException e2) {
            e = e2;
        } catch (NoSuchPaddingException e3) {
            e = e3;
        } catch (InvalidKeyException e4) {
            e = e4;
        }
        DSLog.e("Failed to encrypt data, error: %s.", e.getClass().getSimpleName());
        return new byte[0];
    }

    private static Key convertToPublicKey(String publicKey) {
        GeneralSecurityException e;
        Key key = null;
        byte[] keyBytes = decodeToByte(publicKey);
        if (keyBytes == null || keyBytes.length == 0) {
            DSLog.e("Failed to convert to public key, error: invalid parameter.", new Object[0]);
            return key;
        }
        try {
            return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e2) {
            e = e2;
        } catch (InvalidKeySpecException e3) {
            e = e3;
        }
        DSLog.e("Failed to convert to public key, error: %s.", e.getClass().getSimpleName());
        return key;
    }

    private static Key convertToPrivateKey(String privateKey) {
        GeneralSecurityException e;
        Key key = null;
        byte[] keyBytes = decodeToByte(privateKey);
        if (keyBytes == null) {
            DSLog.e("Failed to convert to private key, error: invalid parameter.", new Object[0]);
            return key;
        }
        try {
            return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e2) {
            e = e2;
        } catch (InvalidKeySpecException e3) {
            e = e3;
        }
        DSLog.e("Failed to convert to private key, error: %s.", e.getClass().getSimpleName());
        return key;
    }

    private static byte[] decryptByPrivateKey(byte[] encryptedData, String privateKey) {
        return decrypt(encryptedData, convertToPrivateKey(privateKey));
    }

    private static byte[] encryptByPublicKey(byte[] data, String publicKey) {
        return encrypt(data, convertToPublicKey(publicKey));
    }

    public static String encryptString(String text, String publicKey) {
        if (text == null || publicKey == null) {
            DSLog.e("Failed to encrypt given string, error: invalid parameter.", new Object[0]);
            return null;
        } else if (text.isEmpty()) {
            return text;
        } else {
            try {
                return encodeToString(encryptByPublicKey(text.getBytes("UTF-8"), publicKey));
            } catch (UnsupportedEncodingException e) {
                DSLog.e("Failed to encrypt given string, error: unsupported encoding.", new Object[0]);
                return null;
            }
        }
    }

    public static String decryptString(String text, String privateKey) {
        if (text == null || privateKey == null) {
            DSLog.e("Failed to decrypt given string, error: invalid parameter.", new Object[0]);
            return null;
        } else if (text.isEmpty()) {
            return text;
        } else {
            try {
                byte[] decryptedData = decryptByPrivateKey(decodeToByte(text), privateKey);
                if (decryptedData.length != 0) {
                    return new String(decryptedData, "UTF-8");
                }
                return null;
            } catch (UnsupportedEncodingException e) {
                DSLog.e("Failed to decrypt given string, error: unsupported encoding.", new Object[0]);
                return null;
            }
        }
    }

    public static String encryptBytesToString(byte[] data, String publicKey) {
        return encodeToString(encryptByPublicKey(data, publicKey));
    }

    public static byte[] decryptStringToBytes(String data, String privateKey) {
        return decryptByPrivateKey(decodeToByte(data), privateKey);
    }
}
