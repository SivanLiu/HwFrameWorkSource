package org.bouncycastle.jcajce.provider.asymmetric.util;

import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public abstract class BaseCipherSpi extends CipherSpi {
    private Class[] availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, RC2ParameterSpec.class, RC5ParameterSpec.class};
    protected AlgorithmParameters engineParams = null;
    private final JcaJceHelper helper = new BCJcaJceHelper();
    private byte[] iv;
    private int ivSize;
    protected Wrapper wrapEngine = null;

    protected BaseCipherSpi() {
    }

    protected final AlgorithmParameters createParametersInstance(String str) throws NoSuchAlgorithmException, NoSuchProviderException {
        return this.helper.createAlgorithmParameters(str);
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

    protected int engineGetOutputSize(int i) {
        return -1;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    protected void engineSetMode(String str) throws NoSuchAlgorithmException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("can't support mode ");
        stringBuilder.append(str);
        throw new NoSuchAlgorithmException(stringBuilder.toString());
    }

    protected void engineSetPadding(String str) throws NoSuchPaddingException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Padding ");
        stringBuilder.append(str);
        stringBuilder.append(" unknown.");
        throw new NoSuchPaddingException(stringBuilder.toString());
    }

    protected Key engineUnwrap(byte[] bArr, String str, int i) throws InvalidKeyException {
        try {
            Object engineDoFinal = this.wrapEngine == null ? engineDoFinal(bArr, 0, bArr.length) : this.wrapEngine.unwrap(bArr, 0, bArr.length);
            if (i == 3) {
                return new SecretKeySpec(engineDoFinal, str);
            }
            StringBuilder stringBuilder;
            if (str.equals("") && i == 2) {
                try {
                    PrivateKeyInfo instance = PrivateKeyInfo.getInstance(engineDoFinal);
                    Key privateKey = BouncyCastleProvider.getPrivateKey(instance);
                    if (privateKey != null) {
                        return privateKey;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("algorithm ");
                    stringBuilder.append(instance.getPrivateKeyAlgorithm().getAlgorithm());
                    stringBuilder.append(" not supported");
                    throw new InvalidKeyException(stringBuilder.toString());
                } catch (Exception e) {
                    throw new InvalidKeyException("Invalid key encoding.");
                }
            }
            try {
                KeyFactory createKeyFactory = this.helper.createKeyFactory(str);
                if (i == 1) {
                    return createKeyFactory.generatePublic(new X509EncodedKeySpec(engineDoFinal));
                }
                if (i == 2) {
                    return createKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(engineDoFinal));
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown key type ");
                stringBuilder2.append(i);
                throw new InvalidKeyException(stringBuilder2.toString());
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
        } catch (final BadPaddingException e6) {
            throw new InvalidKeyException("unable to unwrap") {
                public synchronized Throwable getCause() {
                    return e6;
                }
            };
        } catch (IllegalBlockSizeException e7) {
            throw new InvalidKeyException(e7.getMessage());
        }
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        byte[] encoded = key.getEncoded();
        if (encoded != null) {
            try {
                return this.wrapEngine == null ? engineDoFinal(encoded, 0, encoded.length) : this.wrapEngine.wrap(encoded, 0, encoded.length);
            } catch (BadPaddingException e) {
                throw new IllegalBlockSizeException(e.getMessage());
            }
        }
        throw new InvalidKeyException("Cannot wrap key, null encoding.");
    }
}
