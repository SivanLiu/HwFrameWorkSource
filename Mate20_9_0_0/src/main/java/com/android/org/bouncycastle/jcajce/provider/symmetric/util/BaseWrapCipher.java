package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.Wrapper;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util;
import com.android.org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.util.Arrays;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class BaseWrapCipher extends CipherSpi implements PBE {
    private Class[] availableSpecs;
    protected AlgorithmParameters engineParams;
    private final JcaJceHelper helper;
    private byte[] iv;
    private int ivSize;
    protected int pbeHash;
    protected int pbeIvSize;
    protected int pbeKeySize;
    protected int pbeType;
    protected Wrapper wrapEngine;

    protected BaseWrapCipher() {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class};
        this.pbeType = 2;
        this.pbeHash = 1;
        this.engineParams = null;
        this.wrapEngine = null;
        this.helper = new DefaultJcaJceHelper();
    }

    protected BaseWrapCipher(Wrapper wrapEngine) {
        this(wrapEngine, 0);
    }

    protected BaseWrapCipher(Wrapper wrapEngine, int ivSize) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class};
        this.pbeType = 2;
        this.pbeHash = 1;
        this.engineParams = null;
        this.wrapEngine = null;
        this.helper = new DefaultJcaJceHelper();
        this.wrapEngine = wrapEngine;
        this.ivSize = ivSize;
    }

    protected int engineGetBlockSize() {
        return 0;
    }

    protected byte[] engineGetIV() {
        return Arrays.clone(this.iv);
    }

    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length * 8;
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

    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters param;
        if (key instanceof BCPBEKey) {
            CipherParameters param2;
            BCPBEKey k = (BCPBEKey) key;
            if (params instanceof PBEParameterSpec) {
                param2 = Util.makePBEParameters(k, params, this.wrapEngine.getAlgorithmName());
            } else if (k.getParam() != null) {
                param2 = k.getParam();
            } else {
                throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
            }
            param = param2;
        } else {
            param = new KeyParameter(key.getEncoded());
        }
        if (params instanceof IvParameterSpec) {
            param = new ParametersWithIV(param, ((IvParameterSpec) params).getIV());
        }
        if ((param instanceof KeyParameter) && this.ivSize != 0) {
            this.iv = new byte[this.ivSize];
            random.nextBytes(this.iv);
            param = new ParametersWithIV(param, this.iv);
        }
        if (random != null) {
            param = new ParametersWithRandom(param, random);
        }
        switch (opmode) {
            case 1:
            case 2:
                throw new IllegalArgumentException("engine only valid for wrapping");
            case 3:
                this.wrapEngine.init(true, param);
                return;
            case 4:
                this.wrapEngine.init(false, param);
                return;
            default:
                System.out.println("eeek!");
                return;
        }
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec paramSpec = null;
        if (params != null) {
            int i = 0;
            while (i != this.availableSpecs.length) {
                try {
                    paramSpec = params.getParameterSpec(this.availableSpecs[i]);
                    break;
                } catch (Exception e) {
                    i++;
                }
            }
            if (paramSpec == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("can't handle parameter ");
                stringBuilder.append(params.toString());
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }
        this.engineParams = params;
        engineInit(opmode, key, paramSpec, random);
    }

    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        try {
            engineInit(opmode, key, (AlgorithmParameterSpec) null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        throw new RuntimeException("not supported for wrapping");
    }

    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        throw new RuntimeException("not supported for wrapping");
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        return null;
    }

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        return 0;
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

    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm, int wrappedKeyType) throws InvalidKeyException, NoSuchAlgorithmException {
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
            } catch (NoSuchProviderException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e2.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            } catch (InvalidKeySpecException e22) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e22.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            }
        } catch (InvalidCipherTextException e3) {
            throw new InvalidKeyException(e3.getMessage());
        } catch (BadPaddingException e4) {
            throw new InvalidKeyException(e4.getMessage());
        } catch (IllegalBlockSizeException e23) {
            throw new InvalidKeyException(e23.getMessage());
        }
    }
}
