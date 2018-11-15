package org.bouncycastle.jcajce.provider.asymmetric.rsa;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.encodings.ISO9796d1Encoding;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseCipherSpi;
import org.bouncycastle.jcajce.provider.util.BadBlockException;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.util.Strings;

public class CipherSpi extends BaseCipherSpi {
    private ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    private AsymmetricBlockCipher cipher;
    private AlgorithmParameters engineParams;
    private final JcaJceHelper helper = new BCJcaJceHelper();
    private AlgorithmParameterSpec paramSpec;
    private boolean privateKeyOnly = false;
    private boolean publicKeyOnly = false;

    public static class ISO9796d1Padding extends CipherSpi {
        public ISO9796d1Padding() {
            super(new ISO9796d1Encoding(new RSABlindedEngine()));
        }
    }

    public static class NoPadding extends CipherSpi {
        public NoPadding() {
            super(new RSABlindedEngine());
        }
    }

    public static class OAEPPadding extends CipherSpi {
        public OAEPPadding() {
            super(OAEPParameterSpec.DEFAULT);
        }
    }

    public static class PKCS1v1_5Padding extends CipherSpi {
        public PKCS1v1_5Padding() {
            super(new PKCS1Encoding(new RSABlindedEngine()));
        }
    }

    public static class PKCS1v1_5Padding_PrivateOnly extends CipherSpi {
        public PKCS1v1_5Padding_PrivateOnly() {
            super(false, true, new PKCS1Encoding(new RSABlindedEngine()));
        }
    }

    public static class PKCS1v1_5Padding_PublicOnly extends CipherSpi {
        public PKCS1v1_5Padding_PublicOnly() {
            super(true, false, new PKCS1Encoding(new RSABlindedEngine()));
        }
    }

    public CipherSpi(OAEPParameterSpec oAEPParameterSpec) {
        try {
            initFromSpec(oAEPParameterSpec);
        } catch (NoSuchPaddingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public CipherSpi(AsymmetricBlockCipher asymmetricBlockCipher) {
        this.cipher = asymmetricBlockCipher;
    }

    public CipherSpi(boolean z, boolean z2, AsymmetricBlockCipher asymmetricBlockCipher) {
        this.publicKeyOnly = z;
        this.privateKeyOnly = z2;
        this.cipher = asymmetricBlockCipher;
    }

    private byte[] getOutput() throws BadPaddingException {
        try {
            byte[] toByteArray = this.bOut.toByteArray();
            toByteArray = this.cipher.processBlock(toByteArray, 0, toByteArray.length);
            this.bOut.reset();
            return toByteArray;
        } catch (Throwable e) {
            throw new BadBlockException("unable to decrypt block", e);
        } catch (Throwable e2) {
            throw new BadBlockException("unable to decrypt block", e2);
        } catch (Throwable th) {
            this.bOut.reset();
        }
    }

    private void initFromSpec(OAEPParameterSpec oAEPParameterSpec) throws NoSuchPaddingException {
        MGF1ParameterSpec mGF1ParameterSpec = (MGF1ParameterSpec) oAEPParameterSpec.getMGFParameters();
        Digest digest = DigestFactory.getDigest(mGF1ParameterSpec.getDigestAlgorithm());
        if (digest != null) {
            this.cipher = new OAEPEncoding(new RSABlindedEngine(), digest, ((PSpecified) oAEPParameterSpec.getPSource()).getValue());
            this.paramSpec = oAEPParameterSpec;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no match on OAEP constructor for digest algorithm: ");
        stringBuilder.append(mGF1ParameterSpec.getDigestAlgorithm());
        throw new NoSuchPaddingException(stringBuilder.toString());
    }

    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalBlockSizeException, BadPaddingException {
        if (bArr != null) {
            this.bOut.write(bArr, i, i2);
        }
        if (this.cipher instanceof RSABlindedEngine) {
            if (this.bOut.size() > this.cipher.getInputBlockSize() + 1) {
                throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
            }
        } else if (this.bOut.size() > this.cipher.getInputBlockSize()) {
            throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
        }
        bArr = getOutput();
        for (i = 0; i != bArr.length; i++) {
            bArr2[i3 + i] = bArr[i];
        }
        return bArr.length;
    }

    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException {
        if (bArr != null) {
            this.bOut.write(bArr, i, i2);
        }
        if (this.cipher instanceof RSABlindedEngine) {
            if (this.bOut.size() > this.cipher.getInputBlockSize() + 1) {
                throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
            }
        } else if (this.bOut.size() > this.cipher.getInputBlockSize()) {
            throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
        }
        return getOutput();
    }

    protected int engineGetBlockSize() {
        try {
            return this.cipher.getInputBlockSize();
        } catch (NullPointerException e) {
            throw new IllegalStateException("RSA Cipher not initialised");
        }
    }

    protected int engineGetKeySize(Key key) {
        BigInteger modulus;
        if (key instanceof RSAPrivateKey) {
            modulus = ((RSAPrivateKey) key).getModulus();
        } else if (key instanceof RSAPublicKey) {
            modulus = ((RSAPublicKey) key).getModulus();
        } else {
            throw new IllegalArgumentException("not an RSA key!");
        }
        return modulus.bitLength();
    }

    protected int engineGetOutputSize(int i) {
        try {
            return this.cipher.getOutputBlockSize();
        } catch (NullPointerException e) {
            throw new IllegalStateException("RSA Cipher not initialised");
        }
    }

    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null && this.paramSpec != null) {
            try {
                this.engineParams = this.helper.createAlgorithmParameters("OAEP");
                this.engineParams.init(this.paramSpec);
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
        }
        return this.engineParams;
    }

    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec parameterSpec;
        if (algorithmParameters != null) {
            try {
                parameterSpec = algorithmParameters.getParameterSpec(OAEPParameterSpec.class);
            } catch (Throwable e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cannot recognise parameters: ");
                stringBuilder.append(e.toString());
                throw new InvalidAlgorithmParameterException(stringBuilder.toString(), e);
            }
        }
        parameterSpec = null;
        this.engineParams = algorithmParameters;
        engineInit(i, key, parameterSpec, secureRandom);
    }

    protected void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        try {
            engineInit(i, key, (AlgorithmParameterSpec) null, secureRandom);
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Eeeek! ");
            stringBuilder.append(e.toString());
            throw new InvalidKeyException(stringBuilder.toString(), e);
        }
    }

    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        StringBuilder stringBuilder;
        if (algorithmParameterSpec == null || (algorithmParameterSpec instanceof OAEPParameterSpec)) {
            CipherParameters generatePublicKeyParameter;
            if (key instanceof RSAPublicKey) {
                if (this.privateKeyOnly && i == 1) {
                    throw new InvalidKeyException("mode 1 requires RSAPrivateKey");
                }
                generatePublicKeyParameter = RSAUtil.generatePublicKeyParameter((RSAPublicKey) key);
            } else if (!(key instanceof RSAPrivateKey)) {
                throw new InvalidKeyException("unknown key type passed to RSA");
            } else if (this.publicKeyOnly && i == 1) {
                throw new InvalidKeyException("mode 2 requires RSAPublicKey");
            } else {
                generatePublicKeyParameter = RSAUtil.generatePrivateKeyParameter((RSAPrivateKey) key);
            }
            if (algorithmParameterSpec != null) {
                OAEPParameterSpec oAEPParameterSpec = (OAEPParameterSpec) algorithmParameterSpec;
                this.paramSpec = algorithmParameterSpec;
                if (!oAEPParameterSpec.getMGFAlgorithm().equalsIgnoreCase("MGF1") && !oAEPParameterSpec.getMGFAlgorithm().equals(PKCSObjectIdentifiers.id_mgf1.getId())) {
                    throw new InvalidAlgorithmParameterException("unknown mask generation function specified");
                } else if (oAEPParameterSpec.getMGFParameters() instanceof MGF1ParameterSpec) {
                    Digest digest = DigestFactory.getDigest(oAEPParameterSpec.getDigestAlgorithm());
                    if (digest != null) {
                        MGF1ParameterSpec mGF1ParameterSpec = (MGF1ParameterSpec) oAEPParameterSpec.getMGFParameters();
                        Digest digest2 = DigestFactory.getDigest(mGF1ParameterSpec.getDigestAlgorithm());
                        if (digest2 != null) {
                            this.cipher = new OAEPEncoding(new RSABlindedEngine(), digest, digest2, ((PSpecified) oAEPParameterSpec.getPSource()).getValue());
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("no match on MGF digest algorithm: ");
                            stringBuilder.append(mGF1ParameterSpec.getDigestAlgorithm());
                            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("no match on digest algorithm: ");
                    stringBuilder.append(oAEPParameterSpec.getDigestAlgorithm());
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                } else {
                    throw new InvalidAlgorithmParameterException("unkown MGF parameters");
                }
            }
            CipherParameters parametersWithRandom = !(this.cipher instanceof RSABlindedEngine) ? secureRandom != null ? new ParametersWithRandom(generatePublicKeyParameter, secureRandom) : new ParametersWithRandom(generatePublicKeyParameter, new SecureRandom()) : generatePublicKeyParameter;
            this.bOut.reset();
            switch (i) {
                case 1:
                case 3:
                    this.cipher.init(true, parametersWithRandom);
                    return;
                case 2:
                case 4:
                    this.cipher.init(false, parametersWithRandom);
                    return;
                default:
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("unknown opmode ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" passed to RSA");
                    throw new InvalidParameterException(stringBuilder2.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unknown parameter type: ");
        stringBuilder.append(algorithmParameterSpec.getClass().getName());
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }

    protected void engineSetMode(String str) throws NoSuchAlgorithmException {
        String toUpperCase = Strings.toUpperCase(str);
        if (!toUpperCase.equals("NONE") && !toUpperCase.equals("ECB")) {
            if (toUpperCase.equals("1")) {
                this.privateKeyOnly = true;
                this.publicKeyOnly = false;
            } else if (toUpperCase.equals("2")) {
                this.privateKeyOnly = false;
                this.publicKeyOnly = true;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("can't support mode ");
                stringBuilder.append(str);
                throw new NoSuchAlgorithmException(stringBuilder.toString());
            }
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:62:0x0184 in {2, 4, 7, 10, 13, 15, 18, 23, 28, 33, 38, 43, 46, 49, 52, 55, 57, 58, 59, 60, 61} preds:[]
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
        if (r1 == 0) goto L_0x0014;
    L_0x000c:
        r5 = new org.bouncycastle.crypto.engines.RSABlindedEngine;
        r5.<init>();
    L_0x0011:
        r4.cipher = r5;
        return;
    L_0x0014:
        r1 = "PKCS1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0027;
    L_0x001c:
        r5 = new org.bouncycastle.crypto.encodings.PKCS1Encoding;
        r0 = new org.bouncycastle.crypto.engines.RSABlindedEngine;
        r0.<init>();
        r5.<init>(r0);
        goto L_0x0011;
    L_0x0027:
        r1 = "ISO9796-1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x003a;
    L_0x002f:
        r5 = new org.bouncycastle.crypto.encodings.ISO9796d1Encoding;
        r0 = new org.bouncycastle.crypto.engines.RSABlindedEngine;
        r0.<init>();
        r5.<init>(r0);
        goto L_0x0011;
    L_0x003a:
        r1 = "OAEPWITHMD5ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0058;
    L_0x0042:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "MD5";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "MD5";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
    L_0x0054:
        r4.initFromSpec(r5);
        return;
    L_0x0058:
        r1 = "OAEPPADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0063;
    L_0x0060:
        r5 = javax.crypto.spec.OAEPParameterSpec.DEFAULT;
        goto L_0x0054;
    L_0x0063:
        r1 = "OAEPWITHSHA1ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 != 0) goto L_0x0060;
    L_0x006b:
        r1 = "OAEPWITHSHA-1ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0074;
    L_0x0073:
        goto L_0x0060;
    L_0x0074:
        r1 = "OAEPWITHSHA224ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 != 0) goto L_0x0170;
    L_0x007c:
        r1 = "OAEPWITHSHA-224ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0086;
    L_0x0084:
        goto L_0x0170;
    L_0x0086:
        r1 = "OAEPWITHSHA256ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 != 0) goto L_0x0161;
    L_0x008e:
        r1 = "OAEPWITHSHA-256ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0098;
    L_0x0096:
        goto L_0x0161;
    L_0x0098:
        r1 = "OAEPWITHSHA384ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 != 0) goto L_0x0152;
    L_0x00a0:
        r1 = "OAEPWITHSHA-384ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00aa;
    L_0x00a8:
        goto L_0x0152;
    L_0x00aa:
        r1 = "OAEPWITHSHA512ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 != 0) goto L_0x0143;
    L_0x00b2:
        r1 = "OAEPWITHSHA-512ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00bc;
    L_0x00ba:
        goto L_0x0143;
    L_0x00bc:
        r1 = "OAEPWITHSHA3-224ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00d8;
    L_0x00c4:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA3-224";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "SHA3-224";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
    L_0x00d8:
        r1 = "OAEPWITHSHA3-256ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00f4;
    L_0x00e0:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA3-256";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "SHA3-256";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
    L_0x00f4:
        r1 = "OAEPWITHSHA3-384ANDMGF1PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0110;
    L_0x00fc:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA3-384";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "SHA3-384";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
    L_0x0110:
        r1 = "OAEPWITHSHA3-512ANDMGF1PADDING";
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x012c;
    L_0x0118:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA3-512";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "SHA3-512";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
    L_0x012c:
        r0 = new javax.crypto.NoSuchPaddingException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r1.append(r5);
        r5 = " unavailable with RSA.";
        r1.append(r5);
        r5 = r1.toString();
        r0.<init>(r5);
        throw r0;
    L_0x0143:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-512";
        r1 = "MGF1";
        r2 = java.security.spec.MGF1ParameterSpec.SHA512;
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
    L_0x0152:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-384";
        r1 = "MGF1";
        r2 = java.security.spec.MGF1ParameterSpec.SHA384;
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
    L_0x0161:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-256";
        r1 = "MGF1";
        r2 = java.security.spec.MGF1ParameterSpec.SHA256;
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
    L_0x0170:
        r5 = new javax.crypto.spec.OAEPParameterSpec;
        r0 = "SHA-224";
        r1 = "MGF1";
        r2 = new java.security.spec.MGF1ParameterSpec;
        r3 = "SHA-224";
        r2.<init>(r3);
        r3 = javax.crypto.spec.PSource.PSpecified.DEFAULT;
        r5.<init>(r0, r1, r2, r3);
        goto L_0x0054;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi.engineSetPadding(java.lang.String):void");
    }

    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        this.bOut.write(bArr, i, i2);
        if (this.cipher instanceof RSABlindedEngine) {
            if (this.bOut.size() > this.cipher.getInputBlockSize() + 1) {
                throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
            }
        } else if (this.bOut.size() > this.cipher.getInputBlockSize()) {
            throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
        }
        return 0;
    }

    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        this.bOut.write(bArr, i, i2);
        if (this.cipher instanceof RSABlindedEngine) {
            if (this.bOut.size() > this.cipher.getInputBlockSize() + 1) {
                throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
            }
        } else if (this.bOut.size() > this.cipher.getInputBlockSize()) {
            throw new ArrayIndexOutOfBoundsException("too much data for RSA block");
        }
        return null;
    }
}
