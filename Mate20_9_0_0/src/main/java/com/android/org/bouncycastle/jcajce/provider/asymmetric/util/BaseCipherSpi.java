package com.android.org.bouncycastle.jcajce.provider.asymmetric.util;

import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.Wrapper;
import com.android.org.bouncycastle.jcajce.util.BCJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class BaseCipherSpi extends CipherSpi {
    private Class[] availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class};
    protected AlgorithmParameters engineParams = null;
    private final JcaJceHelper helper = new BCJcaJceHelper();
    private byte[] iv;
    private int ivSize;
    protected Wrapper wrapEngine = null;

    protected BaseCipherSpi() {
    }

    protected int engineGetBlockSize() {
        return 0;
    }

    protected byte[] engineGetIV() {
        return null;
    }

    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length;
    }

    protected int engineGetOutputSize(int inputLen) {
        return -1;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    protected final AlgorithmParameters createParametersInstance(String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        return this.helper.createAlgorithmParameters(algorithm);
    }

    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("can't support mode ");
        stringBuilder.append(mode);
        throw new NoSuchAlgorithmException(stringBuilder.toString());
    }

    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Padding ");
        stringBuilder.append(padding);
        stringBuilder.append(" unknown.");
        throw new NoSuchPaddingException(stringBuilder.toString());
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        byte[] encoded = key.getEncoded();
        if (encoded != null) {
            try {
                if (this.wrapEngine == null) {
                    return engineDoFinal(encoded, 0, encoded.length);
                }
                return this.wrapEngine.wrap(encoded, 0, encoded.length);
            } catch (BadPaddingException e) {
                throw new IllegalBlockSizeException(e.getMessage());
            }
        }
        throw new InvalidKeyException("Cannot wrap key, null encoding.");
    }

    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws InvalidKeyException {
        StringBuilder stringBuilder;
        try {
            byte[] encoded;
            if (this.wrapEngine == null) {
                encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            } else {
                encoded = this.wrapEngine.unwrap(wrappedKey, 0, wrappedKey.length);
            }
            if (wrappedKeyType == 3) {
                return new SecretKeySpec(encoded, wrappedKeyAlgorithm);
            }
            if (wrappedKeyAlgorithm.equals("") && wrappedKeyType == 2) {
                try {
                    PrivateKeyInfo in = PrivateKeyInfo.getInstance(encoded);
                    PrivateKey privKey = BouncyCastleProvider.getPrivateKey(in);
                    if (privKey != null) {
                        return privKey;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("algorithm ");
                    stringBuilder2.append(in.getPrivateKeyAlgorithm().getAlgorithm());
                    stringBuilder2.append(" not supported");
                    throw new InvalidKeyException(stringBuilder2.toString());
                } catch (Exception e) {
                    throw new InvalidKeyException("Invalid key encoding.");
                }
            }
            try {
                KeyFactory kf = this.helper.createKeyFactory(wrappedKeyAlgorithm);
                if (wrappedKeyType == 1) {
                    return kf.generatePublic(new X509EncodedKeySpec(encoded));
                }
                if (wrappedKeyType == 2) {
                    return kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unknown key type ");
                stringBuilder3.append(wrappedKeyType);
                throw new InvalidKeyException(stringBuilder3.toString());
            } catch (NoSuchAlgorithmException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e2.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            } catch (InvalidKeySpecException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e3.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            } catch (NoSuchProviderException e4) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e4.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            }
        } catch (InvalidCipherTextException e5) {
            throw new InvalidKeyException(e5.getMessage());
        } catch (BadPaddingException e6) {
            throw new InvalidKeyException("unable to unwrap") {
                public synchronized Throwable getCause() {
                    return e6;
                }
            };
        } catch (IllegalBlockSizeException e22) {
            throw new InvalidKeyException(e22.getMessage());
        }
    }
}
