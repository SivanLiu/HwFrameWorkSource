package com.android.org.conscrypt;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.KeyAgreementSpi;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public final class OpenSSLECDHKeyAgreement extends KeyAgreementSpi {
    private int mExpectedResultLength;
    private OpenSSLKey mOpenSslPrivateKey;
    private byte[] mResult;

    public Key engineDoPhase(Key key, boolean lastPhase) throws InvalidKeyException {
        if (this.mOpenSslPrivateKey == null) {
            throw new IllegalStateException("Not initialized");
        } else if (!lastPhase) {
            throw new IllegalStateException("ECDH only has one phase");
        } else if (key == null) {
            throw new InvalidKeyException("key == null");
        } else if (key instanceof PublicKey) {
            byte[] buffer = new byte[this.mExpectedResultLength];
            int actualResultLength = NativeCrypto.ECDH_compute_key(buffer, 0, OpenSSLKey.fromPublicKey((PublicKey) key).getNativeRef(), this.mOpenSslPrivateKey.getNativeRef());
            StringBuilder stringBuilder;
            if (actualResultLength != -1) {
                byte[] result;
                if (actualResultLength == this.mExpectedResultLength) {
                    result = buffer;
                } else if (actualResultLength < this.mExpectedResultLength) {
                    result = new byte[actualResultLength];
                    System.arraycopy(buffer, 0, this.mResult, 0, this.mResult.length);
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Engine produced a longer than expected result. Expected: ");
                    stringBuilder.append(this.mExpectedResultLength);
                    stringBuilder.append(", actual: ");
                    stringBuilder.append(actualResultLength);
                    throw new RuntimeException(stringBuilder.toString());
                }
                this.mResult = result;
                return null;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Engine returned ");
            stringBuilder.append(actualResultLength);
            throw new RuntimeException(stringBuilder.toString());
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Not a public key: ");
            stringBuilder2.append(key.getClass());
            throw new InvalidKeyException(stringBuilder2.toString());
        }
    }

    protected int engineGenerateSecret(byte[] sharedSecret, int offset) throws ShortBufferException {
        checkCompleted();
        int available = sharedSecret.length - offset;
        if (this.mResult.length <= available) {
            System.arraycopy(this.mResult, 0, sharedSecret, offset, this.mResult.length);
            return this.mResult.length;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Needed: ");
        stringBuilder.append(this.mResult.length);
        stringBuilder.append(", available: ");
        stringBuilder.append(available);
        throw new ShortBufferException(stringBuilder.toString());
    }

    protected byte[] engineGenerateSecret() {
        checkCompleted();
        return this.mResult;
    }

    protected SecretKey engineGenerateSecret(String algorithm) {
        checkCompleted();
        return new SecretKeySpec(engineGenerateSecret(), algorithm);
    }

    protected void engineInit(Key key, SecureRandom random) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        } else if (key instanceof PrivateKey) {
            OpenSSLKey openSslKey = OpenSSLKey.fromPrivateKey((PrivateKey) key);
            this.mExpectedResultLength = (NativeCrypto.EC_GROUP_get_degree(new EC_GROUP(NativeCrypto.EC_KEY_get1_group(openSslKey.getNativeRef()))) + 7) / 8;
            this.mOpenSslPrivateKey = openSslKey;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not a private key: ");
            stringBuilder.append(key.getClass());
            throw new InvalidKeyException(stringBuilder.toString());
        }
    }

    protected void engineInit(Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params == null) {
            engineInit(key, random);
            return;
        }
        throw new InvalidAlgorithmParameterException("No algorithm parameters supported");
    }

    private void checkCompleted() {
        if (this.mResult == null) {
            throw new IllegalStateException("Key agreement not completed");
        }
    }
}
