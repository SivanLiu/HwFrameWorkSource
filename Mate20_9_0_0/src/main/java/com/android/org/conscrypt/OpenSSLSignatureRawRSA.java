package com.android.org.conscrypt;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public final class OpenSSLSignatureRawRSA extends SignatureSpi {
    private byte[] inputBuffer;
    private boolean inputIsTooLong;
    private int inputOffset;
    private OpenSSLKey key;

    protected void engineUpdate(byte input) {
        int oldOffset = this.inputOffset;
        this.inputOffset = oldOffset + 1;
        if (this.inputOffset > this.inputBuffer.length) {
            this.inputIsTooLong = true;
        } else {
            this.inputBuffer[oldOffset] = input;
        }
    }

    protected void engineUpdate(byte[] input, int offset, int len) {
        int oldOffset = this.inputOffset;
        this.inputOffset += len;
        if (this.inputOffset > this.inputBuffer.length) {
            this.inputIsTooLong = true;
        } else {
            System.arraycopy(input, offset, this.inputBuffer, oldOffset, len);
        }
    }

    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (privateKey instanceof OpenSSLRSAPrivateKey) {
            this.key = ((OpenSSLRSAPrivateKey) privateKey).getOpenSSLKey();
        } else if (privateKey instanceof RSAPrivateCrtKey) {
            this.key = OpenSSLRSAPrivateCrtKey.getInstance((RSAPrivateCrtKey) privateKey);
        } else if (privateKey instanceof RSAPrivateKey) {
            this.key = OpenSSLRSAPrivateKey.getInstance((RSAPrivateKey) privateKey);
        } else {
            throw new InvalidKeyException("Need RSA private key");
        }
        this.inputBuffer = new byte[NativeCrypto.RSA_size(this.key.getNativeRef())];
        this.inputOffset = 0;
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        if (publicKey instanceof OpenSSLRSAPublicKey) {
            this.key = ((OpenSSLRSAPublicKey) publicKey).getOpenSSLKey();
        } else if (publicKey instanceof RSAPublicKey) {
            this.key = OpenSSLRSAPublicKey.getInstance((RSAPublicKey) publicKey);
        } else {
            throw new InvalidKeyException("Need RSA public key");
        }
        this.inputBuffer = new byte[NativeCrypto.RSA_size(this.key.getNativeRef())];
        this.inputOffset = 0;
    }

    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
    }

    protected byte[] engineSign() throws SignatureException {
        if (this.key == null) {
            throw new SignatureException("Need RSA private key");
        } else if (this.inputIsTooLong) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("input length ");
            stringBuilder.append(this.inputOffset);
            stringBuilder.append(" != ");
            stringBuilder.append(this.inputBuffer.length);
            stringBuilder.append(" (modulus size)");
            throw new SignatureException(stringBuilder.toString());
        } else {
            byte[] outputBuffer = new byte[this.inputBuffer.length];
            try {
                NativeCrypto.RSA_private_encrypt(this.inputOffset, this.inputBuffer, outputBuffer, this.key.getNativeRef(), 1);
                this.inputOffset = 0;
                return outputBuffer;
            } catch (Exception ex) {
                throw new SignatureException(ex);
            } catch (Throwable th) {
                this.inputOffset = 0;
            }
        }
    }

    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (this.key == null) {
            throw new SignatureException("Need RSA public key");
        } else if (this.inputIsTooLong) {
            return false;
        } else {
            if (sigBytes.length <= this.inputBuffer.length) {
                byte[] outputBuffer = new byte[this.inputBuffer.length];
                try {
                    boolean matches = true;
                    int resultSize = NativeCrypto.RSA_public_decrypt(sigBytes.length, sigBytes, outputBuffer, this.key.getNativeRef(), 1);
                    if (resultSize != this.inputOffset) {
                        matches = false;
                    }
                    boolean z = matches;
                    for (int i = 0; i < resultSize; i++) {
                        if (this.inputBuffer[i] != outputBuffer[i]) {
                            matches = false;
                        }
                    }
                    this.inputOffset = 0;
                    return matches;
                } catch (SignatureException e) {
                    throw e;
                } catch (Exception e2) {
                    this.inputOffset = 0;
                    return false;
                } catch (Exception ex) {
                    try {
                        throw new SignatureException(ex);
                    } catch (Throwable th) {
                        this.inputOffset = 0;
                    }
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Input signature length is too large: ");
                stringBuilder.append(sigBytes.length);
                stringBuilder.append(" > ");
                stringBuilder.append(this.inputBuffer.length);
                throw new SignatureException(stringBuilder.toString());
            }
        }
    }
}
