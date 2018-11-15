package org.bouncycastle.crypto;

public class MaxBytesExceededException extends RuntimeCryptoException {
    public MaxBytesExceededException(String str) {
        super(str);
    }
}
