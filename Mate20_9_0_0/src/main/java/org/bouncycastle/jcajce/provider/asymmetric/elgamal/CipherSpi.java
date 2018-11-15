package org.bouncycastle.jcajce.provider.asymmetric.elgamal;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.BufferedAsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.ElGamalEngine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseCipherSpi;
import org.bouncycastle.jcajce.provider.util.BadBlockException;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.bouncycastle.jce.interfaces.ElGamalKey;
import org.bouncycastle.jce.interfaces.ElGamalPrivateKey;
import org.bouncycastle.jce.interfaces.ElGamalPublicKey;
import org.bouncycastle.util.Strings;

public class CipherSpi extends BaseCipherSpi {
    private BufferedAsymmetricBlockCipher cipher;
    private AlgorithmParameters engineParams;
    private AlgorithmParameterSpec paramSpec;

    public static class NoPadding extends CipherSpi {
        public NoPadding() {
            super(new ElGamalEngine());
        }
    }

    public static class PKCS1v1_5Padding extends CipherSpi {
        public PKCS1v1_5Padding() {
            super(new PKCS1Encoding(new ElGamalEngine()));
        }
    }

    public CipherSpi(AsymmetricBlockCipher asymmetricBlockCipher) {
        this.cipher = new BufferedAsymmetricBlockCipher(asymmetricBlockCipher);
    }

    private byte[] getOutput() throws BadPaddingException {
        try {
            return this.cipher.doFinal();
        } catch (final InvalidCipherTextException e) {
            throw new BadPaddingException("unable to decrypt block") {
                public synchronized Throwable getCause() {
                    return e;
                }
            };
        } catch (Throwable e2) {
            throw new BadBlockException("unable to decrypt block", e2);
        }
    }

    private void initFromSpec(OAEPParameterSpec oAEPParameterSpec) throws NoSuchPaddingException {
        MGF1ParameterSpec mGF1ParameterSpec = (MGF1ParameterSpec) oAEPParameterSpec.getMGFParameters();
        Digest digest = DigestFactory.getDigest(mGF1ParameterSpec.getDigestAlgorithm());
        if (digest != null) {
            this.cipher = new BufferedAsymmetricBlockCipher(new OAEPEncoding(new ElGamalEngine(), digest, ((PSpecified) oAEPParameterSpec.getPSource()).getValue()));
            this.paramSpec = oAEPParameterSpec;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no match on OAEP constructor for digest algorithm: ");
        stringBuilder.append(mGF1ParameterSpec.getDigestAlgorithm());
        throw new NoSuchPaddingException(stringBuilder.toString());
    }

    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalBlockSizeException, BadPaddingException {
        this.cipher.processBytes(bArr, i, i2);
        bArr = getOutput();
        for (i = 0; i != bArr.length; i++) {
            bArr2[i3 + i] = bArr[i];
        }
        return bArr.length;
    }

    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException {
        this.cipher.processBytes(bArr, i, i2);
        return getOutput();
    }

    protected int engineGetBlockSize() {
        return this.cipher.getInputBlockSize();
    }

    protected int engineGetKeySize(Key key) {
        BigInteger p;
        if (key instanceof ElGamalKey) {
            p = ((ElGamalKey) key).getParameters().getP();
        } else if (key instanceof DHKey) {
            p = ((DHKey) key).getParams().getP();
        } else {
            throw new IllegalArgumentException("not an ElGamal key!");
        }
        return p.bitLength();
    }

    protected int engineGetOutputSize(int i) {
        return this.cipher.getOutputBlockSize();
    }

    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null && this.paramSpec != null) {
            try {
                this.engineParams = createParametersInstance("OAEP");
                this.engineParams.init(this.paramSpec);
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
        }
        return this.engineParams;
    }

    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        throw new InvalidAlgorithmParameterException("can't handle parameters in ElGamal");
    }

    protected void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        engineInit(i, key, (AlgorithmParameterSpec) null, secureRandom);
    }

    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException {
        if (algorithmParameterSpec == null) {
            CipherParameters generatePublicKeyParameter;
            BufferedAsymmetricBlockCipher bufferedAsymmetricBlockCipher;
            boolean z;
            if (key instanceof ElGamalPublicKey) {
                generatePublicKeyParameter = ElGamalUtil.generatePublicKeyParameter((PublicKey) key);
            } else if (key instanceof ElGamalPrivateKey) {
                generatePublicKeyParameter = ElGamalUtil.generatePrivateKeyParameter((PrivateKey) key);
            } else {
                throw new InvalidKeyException("unknown key type passed to ElGamal");
            }
            if (secureRandom != null) {
                generatePublicKeyParameter = new ParametersWithRandom(generatePublicKeyParameter, secureRandom);
            }
            switch (i) {
                case 1:
                case 3:
                    bufferedAsymmetricBlockCipher = this.cipher;
                    z = true;
                    break;
                case 2:
                case 4:
                    bufferedAsymmetricBlockCipher = this.cipher;
                    z = false;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknown opmode ");
                    stringBuilder.append(i);
                    stringBuilder.append(" passed to ElGamal");
                    throw new InvalidParameterException(stringBuilder.toString());
            }
            bufferedAsymmetricBlockCipher.init(z, generatePublicKeyParameter);
            return;
        }
        throw new IllegalArgumentException("unknown parameter type.");
    }

    protected void engineSetMode(String str) throws NoSuchAlgorithmException {
        String toUpperCase = Strings.toUpperCase(str);
        if (!toUpperCase.equals("NONE") && !toUpperCase.equals("ECB")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("can't support mode ");
            stringBuilder.append(str);
            throw new NoSuchAlgorithmException(stringBuilder.toString());
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:34:0x00d9 in {2, 4, 7, 10, 13, 15, 18, 21, 24, 27, 30, 33, 36} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected void engineSetPadding(java.lang.String r5) throws javax.crypto.NoSuchPaddingException {
        /*
        r4 = this;
        r0 = org.bouncycastle.util.Strings.toUpperCase(r5);
        r1 = "NOPADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0019;
    L_0x000c:
        r5 = new org.bouncycastle.crypto.BufferedAsymmetricBlockCipher;
        r0 = new org.bouncycastle.crypto.engines.ElGamalEngine;
        r0.<init>();
        r5.<init>(r0);
    L_0x0016:
        r4.cipher = r5;
        return;
    L_0x0019:
        r1 = "PKCS1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0031;
    L_0x0021:
        r5 = new org.bouncycastle.crypto.BufferedAsymmetricBlockCipher;
        r0 = new org.bouncycastle.crypto.encodings.PKCS1Encoding;
        r1 = new org.bouncycastle.crypto.engines.ElGamalEngine;
        r1.<init>();
        r0.<init>(r1);
        r5.<init>(r0);
        goto L_0x0016;
    L_0x0031:
        r1 = "ISO9796-1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0049;
    L_0x0039:
        r5 = new org.bouncycastle.crypto.BufferedAsymmetricBlockCipher;
        r0 = new org.bouncycastle.crypto.encodings.ISO9796d1Encoding;
        r1 = new org.bouncycastle.crypto.engines.ElGamalEngine;
        r1.<init>();
        r0.<init>(r1);
        r5.<init>(r0);
        goto L_0x0016;
    L_0x0049:
        r1 = "OAEPPADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0057;
    L_0x0051:
        r5 = javax.crypto.spec.OAEPParameterSpec.DEFAULT;
    L_0x0053:
        r4.initFromSpec(r5);
        return;
    L_0x0057:
        r1 = "OAEPWITHMD5ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0072;
    L_0x005f:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "MD5";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "MD5";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0053;
    L_0x0072:
        r1 = "OAEPWITHSHA1ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x007b;
    L_0x007a:
        goto L_0x0051;
    L_0x007b:
        r1 = "OAEPWITHSHA224ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0096;
    L_0x0083:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-224";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "SHA-224";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0053;
    L_0x0096:
        r1 = "OAEPWITHSHA256ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00ac;
    L_0x009e:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-256";
        r1 = "MGF1";
        r2 = java.security.spec.MGF1ParameterSpec.SHA256;
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0053;
    L_0x00ac:
        r1 = "OAEPWITHSHA384ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00c2;
    L_0x00b4:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-384";
        r1 = "MGF1";
        r2 = java.security.spec.MGF1ParameterSpec.SHA384;
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0053;
    L_0x00c2:
        r1 = "OAEPWITHSHA512ANDMGF1PADDING";
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x00da;
    L_0x00ca:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-512";
        r1 = "MGF1";
        r2 = java.security.spec.MGF1ParameterSpec.SHA512;
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0053;
        return;
    L_0x00da:
        r0 = new javax.crypto.NoSuchPaddingException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r1.append(r5);
        r5 = " unavailable with ElGamal.";
        r1.append(r5);
        r5 = r1.toString();
        r0.<init>(r5);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.elgamal.CipherSpi.engineSetPadding(java.lang.String):void");
    }

    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        this.cipher.processBytes(bArr, i, i2);
        return 0;
    }

    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        this.cipher.processBytes(bArr, i, i2);
        return null;
    }
}
