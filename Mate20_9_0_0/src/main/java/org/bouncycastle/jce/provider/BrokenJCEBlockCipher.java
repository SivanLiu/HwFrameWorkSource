package org.bouncycastle.jce.provider;

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
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RC2Parameters;
import org.bouncycastle.crypto.params.RC5Parameters;
import org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey;
import org.bouncycastle.jce.provider.BrokenPBE.Util;

public class BrokenJCEBlockCipher implements BrokenPBE {
    private Class[] availableSpecs;
    private BufferedBlockCipher cipher;
    private AlgorithmParameters engineParams;
    private int ivLength;
    private ParametersWithIV ivParam;
    private int pbeHash;
    private int pbeIvSize;
    private int pbeKeySize;
    private int pbeType;

    public static class BrokePBEWithMD5AndDES extends BrokenJCEBlockCipher {
        public BrokePBEWithMD5AndDES() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new DESEngine());
            super(cBCBlockCipher, 0, 0, 64, 64);
        }
    }

    public static class BrokePBEWithSHA1AndDES extends BrokenJCEBlockCipher {
        public BrokePBEWithSHA1AndDES() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new DESEngine());
            super(cBCBlockCipher, 0, 1, 64, 64);
        }
    }

    public static class BrokePBEWithSHAAndDES2Key extends BrokenJCEBlockCipher {
        public BrokePBEWithSHAAndDES2Key() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new DESedeEngine());
            super(cBCBlockCipher, 2, 1, 128, 64);
        }
    }

    public static class BrokePBEWithSHAAndDES3Key extends BrokenJCEBlockCipher {
        public BrokePBEWithSHAAndDES3Key() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new DESedeEngine());
            super(cBCBlockCipher, 2, 1, 192, 64);
        }
    }

    public static class OldPBEWithSHAAndDES3Key extends BrokenJCEBlockCipher {
        public OldPBEWithSHAAndDES3Key() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new DESedeEngine());
            super(cBCBlockCipher, 3, 1, 192, 64);
        }
    }

    public static class OldPBEWithSHAAndTwofish extends BrokenJCEBlockCipher {
        public OldPBEWithSHAAndTwofish() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new TwofishEngine());
            super(cBCBlockCipher, 3, 1, 256, 128);
        }
    }

    protected BrokenJCEBlockCipher(BlockCipher blockCipher) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, RC2ParameterSpec.class, RC5ParameterSpec.class};
        this.pbeType = 2;
        this.pbeHash = 1;
        this.ivLength = 0;
        this.engineParams = null;
        this.cipher = new PaddedBufferedBlockCipher(blockCipher);
    }

    protected BrokenJCEBlockCipher(BlockCipher blockCipher, int i, int i2, int i3, int i4) {
        this.availableSpecs = new Class[]{IvParameterSpec.class, PBEParameterSpec.class, RC2ParameterSpec.class, RC5ParameterSpec.class};
        this.pbeType = 2;
        this.pbeHash = 1;
        this.ivLength = 0;
        this.engineParams = null;
        this.cipher = new PaddedBufferedBlockCipher(blockCipher);
        this.pbeType = i;
        this.pbeHash = i2;
        this.pbeKeySize = i3;
        this.pbeIvSize = i4;
    }

    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalBlockSizeException, BadPaddingException {
        int processBytes = i2 != 0 ? this.cipher.processBytes(bArr, i, i2, bArr2, i3) : 0;
        try {
            return processBytes + this.cipher.doFinal(bArr2, i3 + processBytes);
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        } catch (InvalidCipherTextException e2) {
            throw new BadPaddingException(e2.getMessage());
        }
    }

    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException {
        byte[] bArr2 = new byte[engineGetOutputSize(i2)];
        int processBytes = i2 != 0 ? this.cipher.processBytes(bArr, i, i2, bArr2, 0) : 0;
        try {
            processBytes += this.cipher.doFinal(bArr2, processBytes);
            byte[] bArr3 = new byte[processBytes];
            System.arraycopy(bArr2, 0, bArr3, 0, processBytes);
            return bArr3;
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        } catch (InvalidCipherTextException e2) {
            throw new BadPaddingException(e2.getMessage());
        }
    }

    protected int engineGetBlockSize() {
        return this.cipher.getBlockSize();
    }

    protected byte[] engineGetIV() {
        return this.ivParam != null ? this.ivParam.getIV() : null;
    }

    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length;
    }

    protected int engineGetOutputSize(int i) {
        return this.cipher.getOutputSize(i);
    }

    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null && this.ivParam != null) {
            String algorithmName = this.cipher.getUnderlyingCipher().getAlgorithmName();
            if (algorithmName.indexOf(47) >= 0) {
                algorithmName = algorithmName.substring(0, algorithmName.indexOf(47));
            }
            try {
                this.engineParams = AlgorithmParameters.getInstance(algorithmName, "BC");
                this.engineParams.init(this.ivParam.getIV());
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
        }
        return this.engineParams;
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

    /* JADX WARNING: Removed duplicated region for block: B:36:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00ee  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x00fd  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00f6  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00cd  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00ee  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x00fd  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00f6  */
    /* JADX WARNING: Missing block: B:3:0x0020, code skipped:
            if (r8.pbeIvSize != 0) goto L_0x0022;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        CipherParameters parametersWithIV;
        byte[] bArr;
        ParametersWithIV parametersWithIV2;
        if (!(key instanceof BCPBEKey)) {
            KeyParameter keyParameter;
            if (algorithmParameterSpec == null) {
                keyParameter = new KeyParameter(key.getEncoded());
            } else {
                ParametersWithIV parametersWithIV3;
                if (algorithmParameterSpec instanceof IvParameterSpec) {
                    if (this.ivLength != 0) {
                        parametersWithIV3 = new ParametersWithIV(new KeyParameter(key.getEncoded()), ((IvParameterSpec) algorithmParameterSpec).getIV());
                        this.ivParam = parametersWithIV3;
                    } else {
                        keyParameter = new KeyParameter(key.getEncoded());
                    }
                } else if (algorithmParameterSpec instanceof RC2ParameterSpec) {
                    RC2ParameterSpec rC2ParameterSpec = (RC2ParameterSpec) algorithmParameterSpec;
                    parametersWithIV3 = new RC2Parameters(key.getEncoded(), rC2ParameterSpec.getEffectiveKeyBits());
                    if (!(rC2ParameterSpec.getIV() == null || this.ivLength == 0)) {
                        parametersWithIV = new ParametersWithIV(parametersWithIV3, rC2ParameterSpec.getIV());
                    }
                } else if (algorithmParameterSpec instanceof RC5ParameterSpec) {
                    RC5ParameterSpec rC5ParameterSpec = (RC5ParameterSpec) algorithmParameterSpec;
                    parametersWithIV3 = new RC5Parameters(key.getEncoded(), rC5ParameterSpec.getRounds());
                    if (rC5ParameterSpec.getWordSize() != 32) {
                        throw new IllegalArgumentException("can only accept RC5 word size 32 (at the moment...)");
                    } else if (!(rC5ParameterSpec.getIV() == null || this.ivLength == 0)) {
                        parametersWithIV = new ParametersWithIV(parametersWithIV3, rC5ParameterSpec.getIV());
                    }
                } else {
                    throw new InvalidAlgorithmParameterException("unknown parameter type.");
                }
                parametersWithIV = parametersWithIV3;
                if (!(this.ivLength == 0 || (parametersWithIV instanceof ParametersWithIV))) {
                    if (secureRandom == null) {
                        secureRandom = new SecureRandom();
                    }
                    if (i != 1 || i == 3) {
                        bArr = new byte[this.ivLength];
                        secureRandom.nextBytes(bArr);
                        parametersWithIV2 = new ParametersWithIV(parametersWithIV, bArr);
                        this.ivParam = parametersWithIV2;
                        parametersWithIV = parametersWithIV2;
                    } else {
                        throw new InvalidAlgorithmParameterException("no IV set when one expected");
                    }
                }
                switch (i) {
                    case 1:
                    case 3:
                        this.cipher.init(true, parametersWithIV);
                        return;
                    case 2:
                    case 4:
                        this.cipher.init(false, parametersWithIV);
                        return;
                    default:
                        System.out.println("eeek!");
                        return;
                }
            }
            parametersWithIV = keyParameter;
            if (secureRandom == null) {
            }
            if (i != 1) {
            }
            bArr = new byte[this.ivLength];
            secureRandom.nextBytes(bArr);
            parametersWithIV2 = new ParametersWithIV(parametersWithIV, bArr);
            this.ivParam = parametersWithIV2;
            parametersWithIV = parametersWithIV2;
            switch (i) {
                case 1:
                case 3:
                    break;
                case 2:
                case 4:
                    break;
                default:
                    break;
            }
        }
        parametersWithIV = Util.makePBEParameters((BCPBEKey) key, algorithmParameterSpec, this.pbeType, this.pbeHash, this.cipher.getUnderlyingCipher().getAlgorithmName(), this.pbeKeySize, this.pbeIvSize);
        this.ivParam = (ParametersWithIV) parametersWithIV;
        if (secureRandom == null) {
        }
        if (i != 1) {
        }
        bArr = new byte[this.ivLength];
        secureRandom.nextBytes(bArr);
        parametersWithIV2 = new ParametersWithIV(parametersWithIV, bArr);
        this.ivParam = parametersWithIV2;
        parametersWithIV = parametersWithIV2;
        switch (i) {
            case 1:
            case 3:
                break;
            case 2:
            case 4:
                break;
            default:
                break;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:22:0x00de in {2, 4, 7, 12, 14, 15, 20, 21, 24} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected void engineSetMode(java.lang.String r5) {
        /*
        r4 = this;
        r0 = org.bouncycastle.util.Strings.toUpperCase(r5);
        r1 = "ECB";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x001d;
        r5 = 0;
        r4.ivLength = r5;
        r5 = new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
        r0 = r4.cipher;
        r0 = r0.getUnderlyingCipher();
        r5.<init>(r0);
        r4.cipher = r5;
        return;
        r1 = "CBC";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0042;
        r5 = r4.cipher;
        r5 = r5.getUnderlyingCipher();
        r5 = r5.getBlockSize();
        r4.ivLength = r5;
        r5 = new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.CBCBlockCipher;
        r1 = r4.cipher;
        r1 = r1.getUnderlyingCipher();
        r0.<init>(r1);
        r5.<init>(r0);
        goto L_0x001a;
        r1 = "OFB";
        r1 = r0.startsWith(r1);
        r2 = 8;
        r3 = 3;
        if (r1 == 0) goto L_0x0092;
        r5 = r4.cipher;
        r5 = r5.getUnderlyingCipher();
        r5 = r5.getBlockSize();
        r4.ivLength = r5;
        r5 = r0.length();
        if (r5 == r3) goto L_0x007a;
        r5 = r0.substring(r3);
        r5 = java.lang.Integer.parseInt(r5);
        r0 = new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.OFBBlockCipher;
        r2 = r4.cipher;
        r2 = r2.getUnderlyingCipher();
        r1.<init>(r2, r5);
        r0.<init>(r1);
        r4.cipher = r0;
        return;
        r5 = new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.OFBBlockCipher;
        r1 = r4.cipher;
        r1 = r1.getUnderlyingCipher();
        r3 = r4.cipher;
        r3 = r3.getBlockSize();
        r2 = r2 * r3;
        r0.<init>(r1, r2);
        r5.<init>(r0);
        goto L_0x001a;
        r1 = "CFB";
        r1 = r0.startsWith(r1);
        if (r1 == 0) goto L_0x00df;
        r5 = r4.cipher;
        r5 = r5.getUnderlyingCipher();
        r5 = r5.getBlockSize();
        r4.ivLength = r5;
        r5 = r0.length();
        if (r5 == r3) goto L_0x00c5;
        r5 = r0.substring(r3);
        r5 = java.lang.Integer.parseInt(r5);
        r0 = new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.CFBBlockCipher;
        r2 = r4.cipher;
        r2 = r2.getUnderlyingCipher();
        r1.<init>(r2, r5);
        r0.<init>(r1);
        goto L_0x0077;
        r5 = new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.CFBBlockCipher;
        r1 = r4.cipher;
        r1 = r1.getUnderlyingCipher();
        r3 = r4.cipher;
        r3 = r3.getBlockSize();
        r2 = r2 * r3;
        r0.<init>(r1, r2);
        r5.<init>(r0);
        goto L_0x001a;
        return;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "can't support mode ";
        r1.append(r2);
        r1.append(r5);
        r5 = r1.toString();
        r0.<init>(r5);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.BrokenJCEBlockCipher.engineSetMode(java.lang.String):void");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:18:0x006f in {2, 4, 11, 14, 16, 17} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected void engineSetPadding(java.lang.String r4) throws javax.crypto.NoSuchPaddingException {
        /*
        r3 = this;
        r0 = org.bouncycastle.util.Strings.toUpperCase(r4);
        r1 = "NOPADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x001a;
        r4 = new org.bouncycastle.crypto.BufferedBlockCipher;
        r0 = r3.cipher;
        r0 = r0.getUnderlyingCipher();
        r4.<init>(r0);
        r3.cipher = r4;
        return;
        r1 = "PKCS5PADDING";
        r1 = r0.equals(r1);
        if (r1 != 0) goto L_0x0063;
        r1 = "PKCS7PADDING";
        r1 = r0.equals(r1);
        if (r1 != 0) goto L_0x0063;
        r1 = "ISO10126PADDING";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0033;
        goto L_0x0063;
        r1 = "WITHCTS";
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x0047;
        r4 = new org.bouncycastle.crypto.modes.CTSBlockCipher;
        r0 = r3.cipher;
        r0 = r0.getUnderlyingCipher();
        r4.<init>(r0);
        goto L_0x0017;
        r0 = new javax.crypto.NoSuchPaddingException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Padding ";
        r1.append(r2);
        r1.append(r4);
        r4 = " unknown.";
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        r4 = new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
        r0 = r3.cipher;
        r0 = r0.getUnderlyingCipher();
        r4.<init>(r0);
        goto L_0x0017;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.BrokenJCEBlockCipher.engineSetPadding(java.lang.String):void");
    }

    protected Key engineUnwrap(byte[] bArr, String str, int i) throws InvalidKeyException {
        StringBuilder stringBuilder;
        try {
            bArr = engineDoFinal(bArr, 0, bArr.length);
            if (i == 3) {
                return new SecretKeySpec(bArr, str);
            }
            try {
                KeyFactory instance = KeyFactory.getInstance(str, "BC");
                if (i == 1) {
                    return instance.generatePublic(new X509EncodedKeySpec(bArr));
                }
                if (i == 2) {
                    return instance.generatePrivate(new PKCS8EncodedKeySpec(bArr));
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown key type ");
                stringBuilder2.append(i);
                throw new InvalidKeyException(stringBuilder2.toString());
            } catch (NoSuchProviderException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown key type ");
                stringBuilder.append(e.getMessage());
                throw new InvalidKeyException(stringBuilder.toString());
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
            }
        } catch (BadPaddingException e4) {
            throw new InvalidKeyException(e4.getMessage());
        } catch (IllegalBlockSizeException e5) {
            throw new InvalidKeyException(e5.getMessage());
        }
    }

    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        return this.cipher.processBytes(bArr, i, i2, bArr2, i3);
    }

    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        int updateOutputSize = this.cipher.getUpdateOutputSize(i2);
        if (updateOutputSize > 0) {
            byte[] bArr2 = new byte[updateOutputSize];
            this.cipher.processBytes(bArr, i, i2, bArr2, 0);
            return bArr2;
        }
        this.cipher.processBytes(bArr, i, i2, null, 0);
        return null;
    }

    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException, InvalidKeyException {
        byte[] encoded = key.getEncoded();
        if (encoded != null) {
            try {
                return engineDoFinal(encoded, 0, encoded.length);
            } catch (BadPaddingException e) {
                throw new IllegalBlockSizeException(e.getMessage());
            }
        }
        throw new InvalidKeyException("Cannot wrap key, null encoding.");
    }
}
