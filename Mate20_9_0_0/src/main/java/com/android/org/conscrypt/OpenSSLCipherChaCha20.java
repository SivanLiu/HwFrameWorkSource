package com.android.org.conscrypt;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

public class OpenSSLCipherChaCha20 extends OpenSSLCipher {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int BLOCK_SIZE_BYTES = 64;
    private static final int NONCE_SIZE_BYTES = 12;
    private int blockCounter = 0;
    private int currentBlockConsumedBytes = 0;

    void engineInitInternal(byte[] encodedKey, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params instanceof IvParameterSpec) {
            IvParameterSpec ivParams = (IvParameterSpec) params;
            if (ivParams.getIV().length == 12) {
                this.iv = ivParams.getIV();
                return;
            }
            throw new InvalidAlgorithmParameterException("IV must be 12 bytes long");
        } else if (isEncrypting()) {
            this.iv = new byte[12];
            if (random != null) {
                random.nextBytes(this.iv);
            } else {
                NativeCrypto.RAND_bytes(this.iv);
            }
        } else {
            throw new InvalidAlgorithmParameterException("IV must be specified when decrypting");
        }
    }

    int updateInternal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int maximumLen) throws ShortBufferException {
        byte[] bArr;
        int inputOffset2 = inputOffset;
        int outputOffset2 = outputOffset;
        int inputLenRemaining = inputLen;
        if (this.currentBlockConsumedBytes > 0) {
            int len = Math.min(64 - this.currentBlockConsumedBytes, inputLenRemaining);
            byte[] singleBlock = new byte[BLOCK_SIZE_BYTES];
            byte[] singleBlockOut = new byte[BLOCK_SIZE_BYTES];
            System.arraycopy(input, inputOffset2, singleBlock, this.currentBlockConsumedBytes, len);
            NativeCrypto.chacha20_encrypt_decrypt(singleBlock, 0, singleBlockOut, 0, BLOCK_SIZE_BYTES, this.encodedKey, this.iv, this.blockCounter);
            bArr = output;
            System.arraycopy(singleBlockOut, this.currentBlockConsumedBytes, bArr, outputOffset2, len);
            this.currentBlockConsumedBytes += len;
            if (this.currentBlockConsumedBytes < BLOCK_SIZE_BYTES) {
                return len;
            }
            this.currentBlockConsumedBytes = 0;
            inputOffset2 += len;
            outputOffset2 += len;
            inputLenRemaining -= len;
            this.blockCounter++;
        } else {
            bArr = output;
        }
        NativeCrypto.chacha20_encrypt_decrypt(input, inputOffset2, bArr, outputOffset2, inputLenRemaining, this.encodedKey, this.iv, this.blockCounter);
        this.currentBlockConsumedBytes = inputLenRemaining % BLOCK_SIZE_BYTES;
        this.blockCounter += inputLenRemaining / BLOCK_SIZE_BYTES;
        return inputLen;
    }

    int doFinalInternal(byte[] output, int outputOffset, int maximumLen) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        reset();
        return 0;
    }

    private void reset() {
        this.blockCounter = 0;
        this.currentBlockConsumedBytes = 0;
    }

    String getBaseCipherName() {
        return "ChaCha20";
    }

    void checkSupportedKeySize(int keySize) throws InvalidKeyException {
        if (keySize != 32) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key size: ");
            stringBuilder.append(keySize);
            stringBuilder.append(" bytes (must be 32)");
            throw new InvalidKeyException(stringBuilder.toString());
        }
    }

    void checkSupportedMode(Mode mode) throws NoSuchAlgorithmException {
        if (mode != Mode.NONE) {
            throw new NoSuchAlgorithmException("Mode must be NONE");
        }
    }

    void checkSupportedPadding(Padding padding) throws NoSuchPaddingException {
        if (padding != Padding.NOPADDING) {
            throw new NoSuchPaddingException("Must be NoPadding");
        }
    }

    int getCipherBlockSize() {
        return 0;
    }

    int getOutputSizeForFinal(int inputLen) {
        return inputLen;
    }

    int getOutputSizeForUpdate(int inputLen) {
        return inputLen;
    }
}
