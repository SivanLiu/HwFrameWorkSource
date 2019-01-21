package com.android.org.conscrypt;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public abstract class KeyGeneratorImpl extends KeyGeneratorSpi {
    private final String algorithm;
    private int keySizeBits;
    protected SecureRandom secureRandom;

    public static final class AES extends KeyGeneratorImpl {
        public AES() {
            super("AES", 128);
        }

        protected void checkKeySize(int keySize) {
            if (keySize != 128 && keySize != 192 && keySize != PSKKeyManager.MAX_KEY_LENGTH_BYTES) {
                throw new InvalidParameterException("Key size must be either 128, 192, or 256 bits");
            }
        }
    }

    public static final class ARC4 extends KeyGeneratorImpl {
        public ARC4() {
            super("ARC4", 128);
        }

        protected void checkKeySize(int keySize) {
            if (keySize < 40 || 2048 < keySize) {
                throw new InvalidParameterException("Key size must be between 40 and 2048 bits");
            }
        }
    }

    public static final class ChaCha20 extends KeyGeneratorImpl {
        public ChaCha20() {
            super("ChaCha20", PSKKeyManager.MAX_KEY_LENGTH_BYTES);
        }

        protected void checkKeySize(int keySize) {
            if (keySize != PSKKeyManager.MAX_KEY_LENGTH_BYTES) {
                throw new InvalidParameterException("Key size must be 256 bits");
            }
        }
    }

    public static final class DESEDE extends KeyGeneratorImpl {
        public DESEDE() {
            super("DESEDE", 192);
        }

        protected void checkKeySize(int keySize) {
            if (keySize != 112 && keySize != 168) {
                throw new InvalidParameterException("Key size must be either 112 or 168 bits");
            }
        }

        protected byte[] doKeyGeneration(int keyBytes) {
            byte[] keyData = new byte[24];
            this.secureRandom.nextBytes(keyData);
            for (int i = 0; i < keyData.length; i++) {
                if (Integer.bitCount(keyData[i]) % 2 == 0) {
                    keyData[i] = (byte) (keyData[i] ^ 1);
                }
            }
            if (keyBytes == 14) {
                System.arraycopy(keyData, 0, keyData, 16, 8);
            }
            return keyData;
        }
    }

    public static final class HmacMD5 extends KeyGeneratorImpl {
        public HmacMD5() {
            super("HmacMD5", 128);
        }
    }

    public static final class HmacSHA1 extends KeyGeneratorImpl {
        public HmacSHA1() {
            super("HmacSHA1", 160);
        }
    }

    public static final class HmacSHA224 extends KeyGeneratorImpl {
        public HmacSHA224() {
            super("HmacSHA224", 224);
        }
    }

    public static final class HmacSHA256 extends KeyGeneratorImpl {
        public HmacSHA256() {
            super("HmacSHA256", PSKKeyManager.MAX_KEY_LENGTH_BYTES);
        }
    }

    public static final class HmacSHA384 extends KeyGeneratorImpl {
        public HmacSHA384() {
            super("HmacSHA384", 384);
        }
    }

    public static final class HmacSHA512 extends KeyGeneratorImpl {
        public HmacSHA512() {
            super("HmacSHA512", 512);
        }
    }

    private KeyGeneratorImpl(String algorithm, int defaultKeySizeBits) {
        this.algorithm = algorithm;
        this.keySizeBits = defaultKeySizeBits;
    }

    protected void checkKeySize(int keySize) {
        if (keySize <= 0) {
            throw new InvalidParameterException("Key size must be positive");
        }
    }

    protected void engineInit(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    protected void engineInit(AlgorithmParameterSpec params, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
        if (params == null) {
            throw new InvalidAlgorithmParameterException("No params provided");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown param type: ");
        stringBuilder.append(params.getClass().getName());
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }

    protected void engineInit(int keySize, SecureRandom secureRandom) {
        checkKeySize(keySize);
        this.keySizeBits = keySize;
        this.secureRandom = secureRandom;
    }

    protected byte[] doKeyGeneration(int keyBytes) {
        byte[] keyData = new byte[keyBytes];
        this.secureRandom.nextBytes(keyData);
        return keyData;
    }

    protected SecretKey engineGenerateKey() {
        if (this.secureRandom == null) {
            this.secureRandom = new SecureRandom();
        }
        return new SecretKeySpec(doKeyGeneration((this.keySizeBits + 7) / 8), this.algorithm);
    }
}
