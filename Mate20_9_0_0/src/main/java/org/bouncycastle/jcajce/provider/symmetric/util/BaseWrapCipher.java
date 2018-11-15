package org.bouncycastle.jcajce.provider.symmetric.util;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Wrapper;
import org.bouncycastle.jcajce.spec.GOST28147WrapParameterSpec;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;

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
        this.availableSpecs = new Class[]{GOST28147WrapParameterSpec.class, PBEParameterSpec.class, RC2ParameterSpec.class, RC5ParameterSpec.class, IvParameterSpec.class};
        this.pbeType = 2;
        this.pbeHash = 1;
        this.engineParams = null;
        this.wrapEngine = null;
        this.helper = new BCJcaJceHelper();
    }

    protected BaseWrapCipher(Wrapper wrapper) {
        this(wrapper, 0);
    }

    protected BaseWrapCipher(Wrapper wrapper, int i) {
        this.availableSpecs = new Class[]{GOST28147WrapParameterSpec.class, PBEParameterSpec.class, RC2ParameterSpec.class, RC5ParameterSpec.class, IvParameterSpec.class};
        this.pbeType = 2;
        this.pbeHash = 1;
        this.engineParams = null;
        this.wrapEngine = null;
        this.helper = new BCJcaJceHelper();
        this.wrapEngine = wrapper;
        this.ivSize = i;
    }

    protected final AlgorithmParameters createParametersInstance(String str) throws NoSuchAlgorithmException, NoSuchProviderException {
        return this.helper.createAlgorithmParameters(str);
    }

    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        return 0;
    }

    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException {
        return null;
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

    protected int engineGetOutputSize(int i) {
        return -1;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec algorithmParameterSpec = null;
        if (algorithmParameters != null) {
            int i2 = 0;
            while (i2 != this.availableSpecs.length) {
                try {
                    algorithmParameterSpec = algorithmParameters.getParameterSpec(this.availableSpecs[i2]);
                    break;
                } catch (Exception e) {
                    i2++;
                }
            }
            if (algorithmParameterSpec == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("can't handle parameter ");
                stringBuilder.append(algorithmParameters.toString());
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }
        this.engineParams = algorithmParameters;
        engineInit(i, key, algorithmParameterSpec, secureRandom);
    }

    protected void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        try {
            engineInit(i, key, (AlgorithmParameterSpec) null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:36:0x00a0 in {4, 7, 9, 10, 13, 18, 19, 24, 26, 29, 30, 31, 33, 35} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected void engineInit(int r3, java.security.Key r4, java.security.spec.AlgorithmParameterSpec r5, java.security.SecureRandom r6) throws java.security.InvalidKeyException, java.security.InvalidAlgorithmParameterException {
        /*
        r2 = this;
        r0 = r4 instanceof org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey;
        if (r0 == 0) goto L_0x0028;
    L_0x0004:
        r4 = (org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey) r4;
        r0 = r5 instanceof javax.crypto.spec.PBEParameterSpec;
        if (r0 == 0) goto L_0x0015;
    L_0x000a:
        r0 = r2.wrapEngine;
        r0 = r0.getAlgorithmName();
        r4 = org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util.makePBEParameters(r4, r5, r0);
        goto L_0x0032;
    L_0x0015:
        r0 = r4.getParam();
        if (r0 == 0) goto L_0x0020;
    L_0x001b:
        r4 = r4.getParam();
        goto L_0x0032;
    L_0x0020:
        r3 = new java.security.InvalidAlgorithmParameterException;
        r4 = "PBE requires PBE parameters to be set.";
        r3.<init>(r4);
        throw r3;
    L_0x0028:
        r0 = new org.bouncycastle.crypto.params.KeyParameter;
        r4 = r4.getEncoded();
        r0.<init>(r4);
        r4 = r0;
    L_0x0032:
        r0 = r5 instanceof javax.crypto.spec.IvParameterSpec;
        if (r0 == 0) goto L_0x0043;
    L_0x0036:
        r0 = r5;
        r0 = (javax.crypto.spec.IvParameterSpec) r0;
        r1 = new org.bouncycastle.crypto.params.ParametersWithIV;
        r0 = r0.getIV();
        r1.<init>(r4, r0);
        r4 = r1;
    L_0x0043:
        r0 = r5 instanceof org.bouncycastle.jcajce.spec.GOST28147WrapParameterSpec;
        if (r0 == 0) goto L_0x005f;
    L_0x0047:
        r5 = (org.bouncycastle.jcajce.spec.GOST28147WrapParameterSpec) r5;
        r0 = r5.getSBox();
        if (r0 == 0) goto L_0x0055;
    L_0x004f:
        r1 = new org.bouncycastle.crypto.params.ParametersWithSBox;
        r1.<init>(r4, r0);
        r4 = r1;
    L_0x0055:
        r0 = new org.bouncycastle.crypto.params.ParametersWithUKM;
        r5 = r5.getUKM();
        r0.<init>(r4, r5);
        r4 = r0;
    L_0x005f:
        r5 = r4 instanceof org.bouncycastle.crypto.params.KeyParameter;
        if (r5 == 0) goto L_0x007a;
    L_0x0063:
        r5 = r2.ivSize;
        if (r5 == 0) goto L_0x007a;
    L_0x0067:
        r5 = r2.ivSize;
        r5 = new byte[r5];
        r2.iv = r5;
        r5 = r2.iv;
        r6.nextBytes(r5);
        r5 = new org.bouncycastle.crypto.params.ParametersWithIV;
        r0 = r2.iv;
        r5.<init>(r4, r0);
        r4 = r5;
    L_0x007a:
        if (r6 == 0) goto L_0x0082;
    L_0x007c:
        r5 = new org.bouncycastle.crypto.params.ParametersWithRandom;
        r5.<init>(r4, r6);
        r4 = r5;
    L_0x0082:
        switch(r3) {
            case 1: goto L_0x0098;
            case 2: goto L_0x0098;
            case 3: goto L_0x0091;
            case 4: goto L_0x008d;
            default: goto L_0x0085;
        };
    L_0x0085:
        r3 = java.lang.System.out;
        r4 = "eeek!";
        r3.println(r4);
        return;
    L_0x008d:
        r3 = r2.wrapEngine;
        r5 = 0;
        goto L_0x0094;
    L_0x0091:
        r3 = r2.wrapEngine;
        r5 = 1;
    L_0x0094:
        r3.init(r5, r4);
        return;
    L_0x0098:
        r3 = new java.lang.IllegalArgumentException;
        r4 = "engine only valid for wrapping";
        r3.<init>(r4);
        throw r3;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.symmetric.util.BaseWrapCipher.engineInit(int, java.security.Key, java.security.spec.AlgorithmParameterSpec, java.security.SecureRandom):void");
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

    protected Key engineUnwrap(byte[] bArr, String str, int i) throws InvalidKeyException, NoSuchAlgorithmException {
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
            } catch (NoSuchProviderException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e2.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            } catch (InvalidKeySpecException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e3.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
            }
        } catch (InvalidCipherTextException e4) {
            throw new InvalidKeyException(e4.getMessage());
        } catch (BadPaddingException e5) {
            throw new InvalidKeyException(e5.getMessage());
        } catch (IllegalBlockSizeException e6) {
            throw new InvalidKeyException(e6.getMessage());
        }
    }

    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException {
        throw new RuntimeException("not supported for wrapping");
    }

    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        throw new RuntimeException("not supported for wrapping");
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
