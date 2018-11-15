package org.bouncycastle.jcajce.provider.symmetric.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import org.bouncycastle.asn1.cms.GCMParameters;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.CTSBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.TBCPadding;
import org.bouncycastle.crypto.paddings.X923Padding;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.ParametersWithSBox;
import org.bouncycastle.crypto.params.RC2Parameters;
import org.bouncycastle.crypto.params.RC5Parameters;
import org.bouncycastle.jcajce.PBKDF1Key;
import org.bouncycastle.jcajce.PBKDF1KeyWithParameters;
import org.bouncycastle.jcajce.PKCS12Key;
import org.bouncycastle.jcajce.PKCS12KeyWithParameters;
import org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util;
import org.bouncycastle.jcajce.spec.AEADParameterSpec;
import org.bouncycastle.jcajce.spec.GOST28147ParameterSpec;
import org.bouncycastle.jcajce.spec.RepeatedSecretKeySpec;
import org.bouncycastle.util.Strings;

public class BaseBlockCipher extends BaseWrapCipher implements PBE {
    private static final Class gcmSpecClass = ClassUtil.loadClass(BaseBlockCipher.class, "javax.crypto.spec.GCMParameterSpec");
    private AEADParameters aeadParams;
    private Class[] availableSpecs;
    private BlockCipher baseEngine;
    private GenericBlockCipher cipher;
    private int digest;
    private BlockCipherProvider engineProvider;
    private boolean fixedIv;
    private int ivLength;
    private ParametersWithIV ivParam;
    private int keySizeInBits;
    private String modeName;
    private boolean padded;
    private String pbeAlgorithm;
    private PBEParameterSpec pbeSpec;
    private int scheme;

    private interface GenericBlockCipher {
        int doFinal(byte[] bArr, int i) throws IllegalStateException, BadPaddingException;

        String getAlgorithmName();

        int getOutputSize(int i);

        BlockCipher getUnderlyingCipher();

        int getUpdateOutputSize(int i);

        void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException;

        int processByte(byte b, byte[] bArr, int i) throws DataLengthException;

        int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException;

        void updateAAD(byte[] bArr, int i, int i2);

        boolean wrapOnNoPadding();
    }

    private static class InvalidKeyOrParametersException extends InvalidKeyException {
        private final Throwable cause;

        InvalidKeyOrParametersException(String str, Throwable th) {
            super(str);
            this.cause = th;
        }

        public Throwable getCause() {
            return this.cause;
        }
    }

    private static class AEADGenericBlockCipher implements GenericBlockCipher {
        private static final Constructor aeadBadTagConstructor;
        private AEADBlockCipher cipher;

        /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
            jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0013 in {2, 4, 5} preds:[]
            	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
            	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
            	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
            	at java.util.ArrayList.forEach(ArrayList.java:1249)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
            	at java.util.ArrayList.forEach(ArrayList.java:1249)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
            	at jadx.core.ProcessClass.process(ProcessClass.java:32)
            	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
            	at java.lang.Iterable.forEach(Iterable.java:75)
            	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
            	at jadx.core.ProcessClass.process(ProcessClass.java:37)
            	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
            	at jadx.api.JavaClass.decompile(JavaClass.java:62)
            	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
            */
        static {
            /*
            r0 = org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.class;
            r1 = "javax.crypto.AEADBadTagException";
            r0 = org.bouncycastle.jcajce.provider.symmetric.util.ClassUtil.loadClass(r0, r1);
            if (r0 == 0) goto L_0x0011;
        L_0x000a:
            r0 = findExceptionConstructor(r0);
        L_0x000e:
            aeadBadTagConstructor = r0;
            return;
        L_0x0011:
            r0 = 0;
            goto L_0x000e;
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.AEADGenericBlockCipher.<clinit>():void");
        }

        AEADGenericBlockCipher(AEADBlockCipher aEADBlockCipher) {
            this.cipher = aEADBlockCipher;
        }

        private static Constructor findExceptionConstructor(Class cls) {
            try {
                return cls.getConstructor(new Class[]{String.class});
            } catch (Exception e) {
                return null;
            }
        }

        public int doFinal(byte[] bArr, int i) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(bArr, i);
            } catch (InvalidCipherTextException e) {
                if (aeadBadTagConstructor != null) {
                    BadPaddingException badPaddingException = null;
                    try {
                        badPaddingException = (BadPaddingException) aeadBadTagConstructor.newInstance(new Object[]{e.getMessage()});
                    } catch (Exception e2) {
                    }
                    if (badPaddingException != null) {
                        throw badPaddingException;
                    }
                }
                throw new BadPaddingException(e.getMessage());
            }
        }

        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public int getOutputSize(int i) {
            return this.cipher.getOutputSize(i);
        }

        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        public int getUpdateOutputSize(int i) {
            return this.cipher.getUpdateOutputSize(i);
        }

        public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
            this.cipher.init(z, cipherParameters);
        }

        public int processByte(byte b, byte[] bArr, int i) throws DataLengthException {
            return this.cipher.processByte(b, bArr, i);
        }

        public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
            return this.cipher.processBytes(bArr, i, i2, bArr2, i3);
        }

        public void updateAAD(byte[] bArr, int i, int i2) {
            this.cipher.processAADBytes(bArr, i, i2);
        }

        public boolean wrapOnNoPadding() {
            return false;
        }
    }

    private static class BufferedGenericBlockCipher implements GenericBlockCipher {
        private BufferedBlockCipher cipher;

        BufferedGenericBlockCipher(BlockCipher blockCipher) {
            this.cipher = new PaddedBufferedBlockCipher(blockCipher);
        }

        BufferedGenericBlockCipher(BlockCipher blockCipher, BlockCipherPadding blockCipherPadding) {
            this.cipher = new PaddedBufferedBlockCipher(blockCipher, blockCipherPadding);
        }

        BufferedGenericBlockCipher(BufferedBlockCipher bufferedBlockCipher) {
            this.cipher = bufferedBlockCipher;
        }

        public int doFinal(byte[] bArr, int i) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(bArr, i);
            } catch (InvalidCipherTextException e) {
                throw new BadPaddingException(e.getMessage());
            }
        }

        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public int getOutputSize(int i) {
            return this.cipher.getOutputSize(i);
        }

        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        public int getUpdateOutputSize(int i) {
            return this.cipher.getUpdateOutputSize(i);
        }

        public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
            this.cipher.init(z, cipherParameters);
        }

        public int processByte(byte b, byte[] bArr, int i) throws DataLengthException {
            return this.cipher.processByte(b, bArr, i);
        }

        public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
            return this.cipher.processBytes(bArr, i, i2, bArr2, i3);
        }

        public void updateAAD(byte[] bArr, int i, int i2) {
            throw new UnsupportedOperationException("AAD is not supported in the current mode.");
        }

        public boolean wrapOnNoPadding() {
            return !(this.cipher instanceof CTSBlockCipher);
        }
    }

    protected BaseBlockCipher(BlockCipher blockCipher) {
        this.availableSpecs = new Class[]{RC2ParameterSpec.class, RC5ParameterSpec.class, gcmSpecClass, GOST28147ParameterSpec.class, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipher;
        this.cipher = new BufferedGenericBlockCipher(blockCipher);
    }

    protected BaseBlockCipher(BlockCipher blockCipher, int i) {
        this(blockCipher, true, i);
    }

    protected BaseBlockCipher(BlockCipher blockCipher, int i, int i2, int i3, int i4) {
        this.availableSpecs = new Class[]{RC2ParameterSpec.class, RC5ParameterSpec.class, gcmSpecClass, GOST28147ParameterSpec.class, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipher;
        this.scheme = i;
        this.digest = i2;
        this.keySizeInBits = i3;
        this.ivLength = i4;
        this.cipher = new BufferedGenericBlockCipher(blockCipher);
    }

    protected BaseBlockCipher(BlockCipher blockCipher, boolean z, int i) {
        this.availableSpecs = new Class[]{RC2ParameterSpec.class, RC5ParameterSpec.class, gcmSpecClass, GOST28147ParameterSpec.class, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipher;
        this.fixedIv = z;
        this.cipher = new BufferedGenericBlockCipher(blockCipher);
        this.ivLength = i / 8;
    }

    protected BaseBlockCipher(BufferedBlockCipher bufferedBlockCipher, int i) {
        this(bufferedBlockCipher, true, i);
    }

    protected BaseBlockCipher(BufferedBlockCipher bufferedBlockCipher, boolean z, int i) {
        this.availableSpecs = new Class[]{RC2ParameterSpec.class, RC5ParameterSpec.class, gcmSpecClass, GOST28147ParameterSpec.class, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = bufferedBlockCipher.getUnderlyingCipher();
        this.cipher = new BufferedGenericBlockCipher(bufferedBlockCipher);
        this.fixedIv = z;
        this.ivLength = i / 8;
    }

    protected BaseBlockCipher(AEADBlockCipher aEADBlockCipher) {
        this.availableSpecs = new Class[]{RC2ParameterSpec.class, RC5ParameterSpec.class, gcmSpecClass, GOST28147ParameterSpec.class, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = aEADBlockCipher.getUnderlyingCipher();
        this.ivLength = this.baseEngine.getBlockSize();
        this.cipher = new AEADGenericBlockCipher(aEADBlockCipher);
    }

    protected BaseBlockCipher(AEADBlockCipher aEADBlockCipher, boolean z, int i) {
        this.availableSpecs = new Class[]{RC2ParameterSpec.class, RC5ParameterSpec.class, gcmSpecClass, GOST28147ParameterSpec.class, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = aEADBlockCipher.getUnderlyingCipher();
        this.fixedIv = z;
        this.ivLength = i;
        this.cipher = new AEADGenericBlockCipher(aEADBlockCipher);
    }

    protected BaseBlockCipher(BlockCipherProvider blockCipherProvider) {
        this.availableSpecs = new Class[]{RC2ParameterSpec.class, RC5ParameterSpec.class, gcmSpecClass, GOST28147ParameterSpec.class, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = blockCipherProvider.get();
        this.engineProvider = blockCipherProvider;
        this.cipher = new BufferedGenericBlockCipher(blockCipherProvider.get());
    }

    private CipherParameters adjustParameters(AlgorithmParameterSpec algorithmParameterSpec, CipherParameters cipherParameters) {
        CipherParameters parameters;
        GOST28147ParameterSpec gOST28147ParameterSpec;
        if (cipherParameters instanceof ParametersWithIV) {
            parameters = ((ParametersWithIV) cipherParameters).getParameters();
            if (algorithmParameterSpec instanceof IvParameterSpec) {
                this.ivParam = new ParametersWithIV(parameters, ((IvParameterSpec) algorithmParameterSpec).getIV());
            } else {
                if (algorithmParameterSpec instanceof GOST28147ParameterSpec) {
                    gOST28147ParameterSpec = (GOST28147ParameterSpec) algorithmParameterSpec;
                    ParametersWithSBox parametersWithSBox = new ParametersWithSBox(cipherParameters, gOST28147ParameterSpec.getSbox());
                    if (gOST28147ParameterSpec.getIV() == null || this.ivLength == 0) {
                        return parametersWithSBox;
                    }
                    this.ivParam = new ParametersWithIV(parameters, gOST28147ParameterSpec.getIV());
                    return this.ivParam;
                }
                return cipherParameters;
            }
        } else if (algorithmParameterSpec instanceof IvParameterSpec) {
            this.ivParam = new ParametersWithIV(cipherParameters, ((IvParameterSpec) algorithmParameterSpec).getIV());
        } else {
            if (algorithmParameterSpec instanceof GOST28147ParameterSpec) {
                gOST28147ParameterSpec = (GOST28147ParameterSpec) algorithmParameterSpec;
                parameters = new ParametersWithSBox(cipherParameters, gOST28147ParameterSpec.getSbox());
                if (gOST28147ParameterSpec.getIV() != null && this.ivLength != 0) {
                    return new ParametersWithIV(parameters, gOST28147ParameterSpec.getIV());
                }
                cipherParameters = parameters;
            }
            return cipherParameters;
        }
        return this.ivParam;
    }

    private boolean isAEADModeName(String str) {
        return "CCM".equals(str) || "EAX".equals(str) || "GCM".equals(str) || "OCB".equals(str);
    }

    protected int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        if (engineGetOutputSize(i2) + i3 <= bArr2.length) {
            int processBytes;
            if (i2 != 0) {
                try {
                    processBytes = this.cipher.processBytes(bArr, i, i2, bArr2, i3);
                } catch (OutputLengthException e) {
                    throw new IllegalBlockSizeException(e.getMessage());
                } catch (DataLengthException e2) {
                    throw new IllegalBlockSizeException(e2.getMessage());
                }
            }
            processBytes = 0;
            return processBytes + this.cipher.doFinal(bArr2, i3 + processBytes);
        }
        throw new ShortBufferException("output buffer too short for input.");
    }

    protected byte[] engineDoFinal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException {
        Object obj = new byte[engineGetOutputSize(i2)];
        int processBytes = i2 != 0 ? this.cipher.processBytes(bArr, i, i2, obj, 0) : 0;
        try {
            processBytes += this.cipher.doFinal(obj, processBytes);
            if (processBytes == obj.length) {
                return obj;
            }
            Object obj2 = new byte[processBytes];
            System.arraycopy(obj, 0, obj2, 0, processBytes);
            return obj2;
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        }
    }

    protected int engineGetBlockSize() {
        return this.baseEngine.getBlockSize();
    }

    protected byte[] engineGetIV() {
        return this.aeadParams != null ? this.aeadParams.getNonce() : this.ivParam != null ? this.ivParam.getIV() : null;
    }

    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length * 8;
    }

    protected int engineGetOutputSize(int i) {
        return this.cipher.getOutputSize(i);
    }

    protected AlgorithmParameters engineGetParameters() {
        if (this.engineParams == null) {
            if (this.pbeSpec != null) {
                try {
                    this.engineParams = createParametersInstance(this.pbeAlgorithm);
                    this.engineParams.init(this.pbeSpec);
                } catch (Exception e) {
                    return null;
                }
            } else if (this.aeadParams != null) {
                try {
                    this.engineParams = createParametersInstance("GCM");
                    this.engineParams.init(new GCMParameters(this.aeadParams.getNonce(), this.aeadParams.getMacSize() / 8).getEncoded());
                } catch (Exception e2) {
                    throw new RuntimeException(e2.toString());
                }
            } else if (this.ivParam != null) {
                String algorithmName = this.cipher.getUnderlyingCipher().getAlgorithmName();
                if (algorithmName.indexOf(47) >= 0) {
                    algorithmName = algorithmName.substring(0, algorithmName.indexOf(47));
                }
                try {
                    this.engineParams = createParametersInstance(algorithmName);
                    this.engineParams.init(new IvParameterSpec(this.ivParam.getIV()));
                } catch (Exception e22) {
                    throw new RuntimeException(e22.toString());
                }
            }
        }
        return this.engineParams;
    }

    protected void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec algorithmParameterSpec = null;
        if (algorithmParameters != null) {
            for (int i2 = 0; i2 != this.availableSpecs.length; i2++) {
                if (this.availableSpecs[i2] != null) {
                    try {
                        algorithmParameterSpec = algorithmParameters.getParameterSpec(this.availableSpecs[i2]);
                        break;
                    } catch (Exception e) {
                    }
                }
            }
            if (algorithmParameterSpec == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("can't handle parameter ");
                stringBuilder.append(algorithmParameters.toString());
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }
        engineInit(i, key, algorithmParameterSpec, secureRandom);
        this.engineParams = algorithmParameters;
    }

    protected void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        try {
            engineInit(i, key, (AlgorithmParameterSpec) null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:230:0x04d4 A:{SYNTHETIC, Splitter: B:230:0x04d4} */
    /* JADX WARNING: Removed duplicated region for block: B:233:0x04dd A:{Catch:{ Exception -> 0x052b }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x04d7 A:{Catch:{ Exception -> 0x052b }} */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x0244  */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x0490  */
    /* JADX WARNING: Removed duplicated region for block: B:214:0x048a  */
    /* JADX WARNING: Removed duplicated region for block: B:230:0x04d4 A:{SYNTHETIC, Splitter: B:230:0x04d4} */
    /* JADX WARNING: Removed duplicated region for block: B:233:0x04dd A:{Catch:{ Exception -> 0x052b }} */
    /* JADX WARNING: Removed duplicated region for block: B:232:0x04d7 A:{Catch:{ Exception -> 0x052b }} */
    /* JADX WARNING: Removed duplicated region for block: B:248:? A:{SYNTHETIC, RETURN, SKIP, Catch:{ Exception -> 0x052b }} */
    /* JADX WARNING: Removed duplicated region for block: B:236:0x04e8 A:{Catch:{ Exception -> 0x052b }} */
    /* JADX WARNING: Missing block: B:27:0x00a3, code:
            if ((r5 instanceof org.bouncycastle.crypto.params.ParametersWithIV) != false) goto L_0x00a5;
     */
    /* JADX WARNING: Missing block: B:43:0x00f1, code:
            if ((r5 instanceof org.bouncycastle.crypto.params.ParametersWithIV) != false) goto L_0x00a5;
     */
    /* JADX WARNING: Missing block: B:54:0x013b, code:
            if ((r5 instanceof org.bouncycastle.crypto.params.ParametersWithIV) != false) goto L_0x00a5;
     */
    /* JADX WARNING: Missing block: B:98:0x01fc, code:
            if ((r5 instanceof org.bouncycastle.crypto.params.ParametersWithIV) != false) goto L_0x00a5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        int i2 = i;
        Key key2 = key;
        AlgorithmParameterSpec algorithmParameterSpec2 = algorithmParameterSpec;
        SecureRandom secureRandom2 = secureRandom;
        CipherParameters cipherParameters = null;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.engineParams = null;
        this.aeadParams = null;
        StringBuilder stringBuilder;
        if (!(key2 instanceof SecretKey)) {
            String algorithm;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Key for algorithm ");
            if (key2 != null) {
                algorithm = key.getAlgorithm();
            }
            stringBuilder.append(algorithm);
            stringBuilder.append(" not suitable for symmetric enryption.");
            throw new InvalidKeyException(stringBuilder.toString());
        } else if (algorithmParameterSpec2 == null && this.baseEngine.getAlgorithmName().startsWith("RC5-64")) {
            throw new InvalidAlgorithmParameterException("RC5 requires an RC5ParametersSpec to be passed in.");
        } else {
            SecureRandom secureRandom3;
            byte[] bArr;
            CipherParameters parametersWithIV;
            if (this.scheme == 2 || (key2 instanceof PKCS12Key)) {
                try {
                    SecretKey secretKey = (SecretKey) key2;
                    if (algorithmParameterSpec2 instanceof PBEParameterSpec) {
                        this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec2;
                    }
                    boolean z = secretKey instanceof PBEKey;
                    if (z && this.pbeSpec == null) {
                        PBEKey pBEKey = (PBEKey) secretKey;
                        if (pBEKey.getSalt() != null) {
                            this.pbeSpec = new PBEParameterSpec(pBEKey.getSalt(), pBEKey.getIterationCount());
                        } else {
                            throw new InvalidAlgorithmParameterException("PBEKey requires parameters to specify salt");
                        }
                    }
                    if (this.pbeSpec == null && !z) {
                        throw new InvalidKeyException("Algorithm requires a PBE key");
                    } else if (key2 instanceof BCPBEKey) {
                        CipherParameters param = ((BCPBEKey) key2).getParam();
                        if (!(param instanceof ParametersWithIV)) {
                            if (param == null) {
                                param = Util.makePBEParameters(secretKey.getEncoded(), 2, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
                            } else {
                                throw new InvalidKeyException("Algorithm requires a PBE key suitable for PKCS12");
                            }
                        }
                        cipherParameters = param;
                    } else {
                        cipherParameters = Util.makePBEParameters(secretKey.getEncoded(), 2, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
                    }
                } catch (Exception e) {
                    throw new InvalidKeyException("PKCS12 requires a SecretKey/PBEKey");
                }
            } else if (key2 instanceof PBKDF1Key) {
                PBKDF1Key pBKDF1Key = (PBKDF1Key) key2;
                if (algorithmParameterSpec2 instanceof PBEParameterSpec) {
                    this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec2;
                }
                if ((pBKDF1Key instanceof PBKDF1KeyWithParameters) && this.pbeSpec == null) {
                    PBKDF1KeyWithParameters pBKDF1KeyWithParameters = (PBKDF1KeyWithParameters) pBKDF1Key;
                    this.pbeSpec = new PBEParameterSpec(pBKDF1KeyWithParameters.getSalt(), pBKDF1KeyWithParameters.getIterationCount());
                }
                cipherParameters = Util.makePBEParameters(pBKDF1Key.getEncoded(), 0, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
            } else if (key2 instanceof BCPBEKey) {
                BCPBEKey bCPBEKey = (BCPBEKey) key2;
                this.pbeAlgorithm = bCPBEKey.getOID() != null ? bCPBEKey.getOID().getId() : bCPBEKey.getAlgorithm();
                if (bCPBEKey.getParam() != null) {
                    cipherParameters = adjustParameters(algorithmParameterSpec2, bCPBEKey.getParam());
                } else if (algorithmParameterSpec2 instanceof PBEParameterSpec) {
                    this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec2;
                    cipherParameters = Util.makePBEParameters(bCPBEKey, algorithmParameterSpec2, this.cipher.getUnderlyingCipher().getAlgorithmName());
                } else {
                    throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
                }
            } else {
                StringBuilder stringBuilder2;
                if (key2 instanceof PBEKey) {
                    PBEKey pBEKey2 = (PBEKey) key2;
                    this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec2;
                    if ((pBEKey2 instanceof PKCS12KeyWithParameters) && this.pbeSpec == null) {
                        this.pbeSpec = new PBEParameterSpec(pBEKey2.getSalt(), pBEKey2.getIterationCount());
                    }
                    cipherParameters = Util.makePBEParameters(pBEKey2.getEncoded(), this.scheme, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
                } else if (!(key2 instanceof RepeatedSecretKeySpec)) {
                    if (this.scheme == 0 || this.scheme == 4 || this.scheme == 1 || this.scheme == 5) {
                        throw new InvalidKeyException("Algorithm requires a PBE key");
                    }
                    cipherParameters = new KeyParameter(key.getEncoded());
                }
                if (algorithmParameterSpec2 instanceof AEADParameterSpec) {
                    if (!(algorithmParameterSpec2 instanceof IvParameterSpec)) {
                        ParametersWithIV parametersWithIV2;
                        if (algorithmParameterSpec2 instanceof GOST28147ParameterSpec) {
                            GOST28147ParameterSpec gOST28147ParameterSpec = (GOST28147ParameterSpec) algorithmParameterSpec2;
                            cipherParameters = new ParametersWithSBox(new KeyParameter(key.getEncoded()), gOST28147ParameterSpec.getSbox());
                            if (!(gOST28147ParameterSpec.getIV() == null || this.ivLength == 0)) {
                                parametersWithIV2 = cipherParameters instanceof ParametersWithIV ? new ParametersWithIV(((ParametersWithIV) cipherParameters).getParameters(), gOST28147ParameterSpec.getIV()) : new ParametersWithIV(cipherParameters, gOST28147ParameterSpec.getIV());
                            }
                        } else if (algorithmParameterSpec2 instanceof RC2ParameterSpec) {
                            RC2ParameterSpec rC2ParameterSpec = (RC2ParameterSpec) algorithmParameterSpec2;
                            cipherParameters = new RC2Parameters(key.getEncoded(), rC2ParameterSpec.getEffectiveKeyBits());
                            if (!(rC2ParameterSpec.getIV() == null || this.ivLength == 0)) {
                                parametersWithIV2 = cipherParameters instanceof ParametersWithIV ? new ParametersWithIV(((ParametersWithIV) cipherParameters).getParameters(), rC2ParameterSpec.getIV()) : new ParametersWithIV(cipherParameters, rC2ParameterSpec.getIV());
                            }
                        } else if (algorithmParameterSpec2 instanceof RC5ParameterSpec) {
                            RC5ParameterSpec rC5ParameterSpec = (RC5ParameterSpec) algorithmParameterSpec2;
                            cipherParameters = new RC5Parameters(key.getEncoded(), rC5ParameterSpec.getRounds());
                            if (this.baseEngine.getAlgorithmName().startsWith("RC5")) {
                                if (this.baseEngine.getAlgorithmName().equals("RC5-32")) {
                                    if (rC5ParameterSpec.getWordSize() != 32) {
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("RC5 already set up for a word size of 32 not ");
                                        stringBuilder.append(rC5ParameterSpec.getWordSize());
                                        stringBuilder.append(".");
                                        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                                    }
                                } else if (this.baseEngine.getAlgorithmName().equals("RC5-64") && rC5ParameterSpec.getWordSize() != 64) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("RC5 already set up for a word size of 64 not ");
                                    stringBuilder.append(rC5ParameterSpec.getWordSize());
                                    stringBuilder.append(".");
                                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                                }
                                if (!(rC5ParameterSpec.getIV() == null || this.ivLength == 0)) {
                                    parametersWithIV2 = cipherParameters instanceof ParametersWithIV ? new ParametersWithIV(((ParametersWithIV) cipherParameters).getParameters(), rC5ParameterSpec.getIV()) : new ParametersWithIV(cipherParameters, rC5ParameterSpec.getIV());
                                }
                            } else {
                                throw new InvalidAlgorithmParameterException("RC5 parameters passed to a cipher that is not RC5.");
                            }
                        } else if (gcmSpecClass == null || !gcmSpecClass.isInstance(algorithmParameterSpec2)) {
                            if (!(algorithmParameterSpec2 == null || (algorithmParameterSpec2 instanceof PBEParameterSpec))) {
                                throw new InvalidAlgorithmParameterException("unknown parameter type.");
                            }
                        } else if (isAEADModeName(this.modeName) || (this.cipher instanceof AEADGenericBlockCipher)) {
                            try {
                                Method declaredMethod = gcmSpecClass.getDeclaredMethod("getTLen", new Class[0]);
                                Method declaredMethod2 = gcmSpecClass.getDeclaredMethod("getIV", new Class[0]);
                                if (cipherParameters instanceof ParametersWithIV) {
                                    cipherParameters = ((ParametersWithIV) cipherParameters).getParameters();
                                }
                                AEADParameters aEADParameters = new AEADParameters((KeyParameter) cipherParameters, ((Integer) declaredMethod.invoke(algorithmParameterSpec2, new Object[0])).intValue(), (byte[]) declaredMethod2.invoke(algorithmParameterSpec2, new Object[0]));
                                this.aeadParams = aEADParameters;
                                cipherParameters = aEADParameters;
                            } catch (Exception e2) {
                                throw new InvalidAlgorithmParameterException("Cannot process GCMParameterSpec.");
                            }
                        } else {
                            throw new InvalidAlgorithmParameterException("GCMParameterSpec can only be used with AEAD modes.");
                        }
                        this.ivParam = parametersWithIV2;
                        cipherParameters = parametersWithIV2;
                    } else if (this.ivLength != 0) {
                        IvParameterSpec ivParameterSpec = (IvParameterSpec) algorithmParameterSpec2;
                        if (ivParameterSpec.getIV().length == this.ivLength || (this.cipher instanceof AEADGenericBlockCipher) || !this.fixedIv) {
                            cipherParameters = cipherParameters instanceof ParametersWithIV ? new ParametersWithIV(((ParametersWithIV) cipherParameters).getParameters(), ivParameterSpec.getIV()) : new ParametersWithIV(cipherParameters, ivParameterSpec.getIV());
                            this.ivParam = (ParametersWithIV) cipherParameters;
                        } else {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("IV must be ");
                            stringBuilder2.append(this.ivLength);
                            stringBuilder2.append(" bytes long.");
                            throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
                        }
                    } else if (this.modeName != null && this.modeName.equals("ECB")) {
                        throw new InvalidAlgorithmParameterException("ECB mode does not use an IV");
                    }
                } else if (isAEADModeName(this.modeName) || (this.cipher instanceof AEADGenericBlockCipher)) {
                    AEADParameterSpec aEADParameterSpec = (AEADParameterSpec) algorithmParameterSpec2;
                    cipherParameters = new AEADParameters(cipherParameters instanceof ParametersWithIV ? (KeyParameter) ((ParametersWithIV) cipherParameters).getParameters() : (KeyParameter) cipherParameters, aEADParameterSpec.getMacSizeInBits(), aEADParameterSpec.getNonce(), aEADParameterSpec.getAssociatedData());
                    this.aeadParams = cipherParameters;
                } else {
                    throw new InvalidAlgorithmParameterException("AEADParameterSpec can only be used with AEAD modes.");
                }
                if (!(this.ivLength == 0 || (cipherParameters instanceof ParametersWithIV) || (cipherParameters instanceof AEADParameters))) {
                    secureRandom3 = secureRandom2 != null ? new SecureRandom() : secureRandom2;
                    if (i2 != 1 || i2 == 3) {
                        bArr = new byte[this.ivLength];
                        secureRandom3.nextBytes(bArr);
                        parametersWithIV = new ParametersWithIV(cipherParameters, bArr);
                        this.ivParam = (ParametersWithIV) parametersWithIV;
                        if (secureRandom2 != null && this.padded) {
                            parametersWithIV = new ParametersWithRandom(parametersWithIV, secureRandom2);
                        }
                        switch (i2) {
                            case 1:
                            case 3:
                                this.cipher.init(true, parametersWithIV);
                                break;
                            case 2:
                            case 4:
                                this.cipher.init(false, parametersWithIV);
                                break;
                            default:
                                try {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("unknown opmode ");
                                    stringBuilder2.append(i2);
                                    stringBuilder2.append(" passed");
                                    throw new InvalidParameterException(stringBuilder2.toString());
                                } catch (Throwable e3) {
                                    throw new InvalidKeyOrParametersException(e3.getMessage(), e3);
                                }
                        }
                        if (!(this.cipher instanceof AEADGenericBlockCipher) && this.aeadParams == null) {
                            this.aeadParams = new AEADParameters((KeyParameter) this.ivParam.getParameters(), ((AEADGenericBlockCipher) this.cipher).cipher.getMac().length * 8, this.ivParam.getIV());
                            return;
                        }
                        return;
                    } else if (this.cipher.getUnderlyingCipher().getAlgorithmName().indexOf("PGPCFB") < 0) {
                        throw new InvalidAlgorithmParameterException("no IV set when one expected");
                    }
                }
                parametersWithIV = cipherParameters;
                parametersWithIV = new ParametersWithRandom(parametersWithIV, secureRandom2);
                switch (i2) {
                    case 1:
                    case 3:
                        break;
                    case 2:
                    case 4:
                        break;
                    default:
                        break;
                }
                if (!(this.cipher instanceof AEADGenericBlockCipher)) {
                    return;
                }
                return;
            }
            this.ivParam = (ParametersWithIV) cipherParameters;
            if (algorithmParameterSpec2 instanceof AEADParameterSpec) {
            }
            if (secureRandom2 != null) {
            }
            if (i2 != 1) {
            }
            bArr = new byte[this.ivLength];
            secureRandom3.nextBytes(bArr);
            parametersWithIV = new ParametersWithIV(cipherParameters, bArr);
            this.ivParam = (ParametersWithIV) parametersWithIV;
            parametersWithIV = new ParametersWithRandom(parametersWithIV, secureRandom2);
            switch (i2) {
                case 1:
                case 3:
                    break;
                case 2:
                case 4:
                    break;
                default:
                    break;
            }
            if (!(this.cipher instanceof AEADGenericBlockCipher)) {
            }
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:72:0x02b3 in {2, 4, 7, 12, 14, 15, 20, 21, 24, 27, 32, 34, 39, 40, 43, 46, 49, 54, 55, 60, 62, 65, 70, 71, 74} preds:[]
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
    protected void engineSetMode(java.lang.String r6) throws java.security.NoSuchAlgorithmException {
        /*
        r5 = this;
        r0 = org.bouncycastle.util.Strings.toUpperCase(r6);
        r5.modeName = r0;
        r0 = r5.modeName;
        r1 = "ECB";
        r0 = r0.equals(r1);
        r1 = 0;
        if (r0 == 0) goto L_0x001d;
    L_0x0011:
        r5.ivLength = r1;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = r5.baseEngine;
        r6.<init>(r0);
    L_0x001a:
        r5.cipher = r6;
        return;
    L_0x001d:
        r0 = r5.modeName;
        r2 = "CBC";
        r0 = r0.equals(r2);
        if (r0 == 0) goto L_0x003c;
    L_0x0027:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.CBCBlockCipher;
        r1 = r5.baseEngine;
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x003c:
        r0 = r5.modeName;
        r2 = "OFB";
        r0 = r0.startsWith(r2);
        r2 = 8;
        r3 = 3;
        if (r0 == 0) goto L_0x0086;
    L_0x0049:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = r5.modeName;
        r6 = r6.length();
        if (r6 == r3) goto L_0x0072;
    L_0x0059:
        r6 = r5.modeName;
        r6 = r6.substring(r3);
        r6 = java.lang.Integer.parseInt(r6);
        r0 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.OFBBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2, r6);
        r0.<init>(r1);
    L_0x006f:
        r5.cipher = r0;
        return;
    L_0x0072:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.OFBBlockCipher;
        r1 = r5.baseEngine;
        r3 = r5.baseEngine;
        r3 = r3.getBlockSize();
        r2 = r2 * r3;
        r0.<init>(r1, r2);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x0086:
        r0 = r5.modeName;
        r4 = "CFB";
        r0 = r0.startsWith(r4);
        if (r0 == 0) goto L_0x00cc;
    L_0x0090:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = r5.modeName;
        r6 = r6.length();
        if (r6 == r3) goto L_0x00b7;
    L_0x00a0:
        r6 = r5.modeName;
        r6 = r6.substring(r3);
        r6 = java.lang.Integer.parseInt(r6);
        r0 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.CFBBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2, r6);
        r0.<init>(r1);
        goto L_0x006f;
    L_0x00b7:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.CFBBlockCipher;
        r1 = r5.baseEngine;
        r3 = r5.baseEngine;
        r3 = r3.getBlockSize();
        r2 = r2 * r3;
        r0.<init>(r1, r2);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x00cc:
        r0 = r5.modeName;
        r2 = "PGP";
        r0 = r0.startsWith(r2);
        if (r0 == 0) goto L_0x00f4;
    L_0x00d6:
        r6 = r5.modeName;
        r0 = "PGPCFBwithIV";
        r6 = r6.equalsIgnoreCase(r0);
        r0 = r5.baseEngine;
        r0 = r0.getBlockSize();
        r5.ivLength = r0;
        r0 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.PGPCFBBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2, r6);
        r0.<init>(r1);
        goto L_0x006f;
    L_0x00f4:
        r0 = r5.modeName;
        r2 = "OpenPGPCFB";
        r0 = r0.equalsIgnoreCase(r2);
        if (r0 == 0) goto L_0x010e;
    L_0x00fe:
        r5.ivLength = r1;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.OpenPGPCFBBlockCipher;
        r1 = r5.baseEngine;
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x010e:
        r0 = r5.modeName;
        r2 = "SIC";
        r0 = r0.startsWith(r2);
        if (r0 == 0) goto L_0x0143;
    L_0x0118:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = r5.ivLength;
        r0 = 16;
        if (r6 < r0) goto L_0x013b;
    L_0x0126:
        r5.fixedIv = r1;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.BufferedBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.SICBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2);
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x013b:
        r6 = new java.lang.IllegalArgumentException;
        r0 = "Warning: SIC-Mode can become a twotime-pad if the blocksize of the cipher is too small. Use a cipher with a block size of at least 128 bits (e.g. AES)";
        r6.<init>(r0);
        throw r6;
    L_0x0143:
        r0 = r5.modeName;
        r2 = "CTR";
        r0 = r0.startsWith(r2);
        if (r0 == 0) goto L_0x0183;
    L_0x014d:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r5.fixedIv = r1;
        r6 = r5.baseEngine;
        r6 = r6 instanceof org.bouncycastle.crypto.engines.DSTU7624Engine;
        if (r6 == 0) goto L_0x0170;
    L_0x015d:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.BufferedBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.KCTRBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2);
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x0170:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.BufferedBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.SICBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2);
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x0183:
        r0 = r5.modeName;
        r1 = "GOFB";
        r0 = r0.startsWith(r1);
        if (r0 == 0) goto L_0x01a8;
    L_0x018d:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.BufferedBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.GOFBBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2);
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x01a8:
        r0 = r5.modeName;
        r1 = "GCFB";
        r0 = r0.startsWith(r1);
        if (r0 == 0) goto L_0x01cd;
    L_0x01b2:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.BufferedBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.GCFBBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2);
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x01cd:
        r0 = r5.modeName;
        r1 = "CTS";
        r0 = r0.startsWith(r1);
        if (r0 == 0) goto L_0x01f2;
    L_0x01d7:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$BufferedGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.CTSBlockCipher;
        r1 = new org.bouncycastle.crypto.modes.CBCBlockCipher;
        r2 = r5.baseEngine;
        r1.<init>(r2);
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x01f2:
        r0 = r5.modeName;
        r1 = "CCM";
        r0 = r0.startsWith(r1);
        if (r0 == 0) goto L_0x0222;
    L_0x01fc:
        r6 = 12;
        r5.ivLength = r6;
        r6 = r5.baseEngine;
        r6 = r6 instanceof org.bouncycastle.crypto.engines.DSTU7624Engine;
        if (r6 == 0) goto L_0x0214;
    L_0x0206:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$AEADGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.KCCMBlockCipher;
        r1 = r5.baseEngine;
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x0214:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$AEADGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.CCMBlockCipher;
        r1 = r5.baseEngine;
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x0222:
        r0 = r5.modeName;
        r1 = "OCB";
        r0 = r0.startsWith(r1);
        if (r0 == 0) goto L_0x025f;
    L_0x022c:
        r0 = r5.engineProvider;
        if (r0 == 0) goto L_0x0248;
    L_0x0230:
        r6 = 15;
        r5.ivLength = r6;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$AEADGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.OCBBlockCipher;
        r1 = r5.baseEngine;
        r2 = r5.engineProvider;
        r2 = r2.get();
        r0.<init>(r1, r2);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x0248:
        r0 = new java.security.NoSuchAlgorithmException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "can't support mode ";
        r1.append(r2);
        r1.append(r6);
        r6 = r1.toString();
        r0.<init>(r6);
        throw r0;
    L_0x025f:
        r0 = r5.modeName;
        r1 = "EAX";
        r0 = r0.startsWith(r1);
        if (r0 == 0) goto L_0x027f;
    L_0x0269:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$AEADGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.EAXBlockCipher;
        r1 = r5.baseEngine;
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x027f:
        r0 = r5.modeName;
        r1 = "GCM";
        r0 = r0.startsWith(r1);
        if (r0 == 0) goto L_0x02b4;
    L_0x0289:
        r6 = r5.baseEngine;
        r6 = r6.getBlockSize();
        r5.ivLength = r6;
        r6 = r5.baseEngine;
        r6 = r6 instanceof org.bouncycastle.crypto.engines.DSTU7624Engine;
        if (r6 == 0) goto L_0x02a5;
    L_0x0297:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$AEADGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.KGCMBlockCipher;
        r1 = r5.baseEngine;
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
    L_0x02a5:
        r6 = new org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher$AEADGenericBlockCipher;
        r0 = new org.bouncycastle.crypto.modes.GCMBlockCipher;
        r1 = r5.baseEngine;
        r0.<init>(r1);
        r6.<init>(r0);
        goto L_0x001a;
        return;
    L_0x02b4:
        r0 = new java.security.NoSuchAlgorithmException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "can't support mode ";
        r1.append(r2);
        r1.append(r6);
        r6 = r1.toString();
        r0.<init>(r6);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.engineSetMode(java.lang.String):void");
    }

    protected void engineSetPadding(String str) throws NoSuchPaddingException {
        GenericBlockCipher bufferedGenericBlockCipher;
        String toUpperCase = Strings.toUpperCase(str);
        if (toUpperCase.equals("NOPADDING")) {
            if (this.cipher.wrapOnNoPadding()) {
                bufferedGenericBlockCipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(this.cipher.getUnderlyingCipher()));
            } else {
                return;
            }
        } else if (toUpperCase.equals("WITHCTS")) {
            bufferedGenericBlockCipher = new BufferedGenericBlockCipher(new CTSBlockCipher(this.cipher.getUnderlyingCipher()));
        } else {
            this.padded = true;
            if (isAEADModeName(this.modeName)) {
                throw new NoSuchPaddingException("Only NoPadding can be used with AEAD modes.");
            } else if (toUpperCase.equals("PKCS5PADDING") || toUpperCase.equals("PKCS7PADDING")) {
                bufferedGenericBlockCipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher());
            } else if (toUpperCase.equals("ZEROBYTEPADDING")) {
                bufferedGenericBlockCipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ZeroBytePadding());
            } else if (toUpperCase.equals("ISO10126PADDING") || toUpperCase.equals("ISO10126-2PADDING")) {
                bufferedGenericBlockCipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO10126d2Padding());
            } else if (toUpperCase.equals("X9.23PADDING") || toUpperCase.equals("X923PADDING")) {
                bufferedGenericBlockCipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new X923Padding());
            } else if (toUpperCase.equals("ISO7816-4PADDING") || toUpperCase.equals("ISO9797-1PADDING")) {
                bufferedGenericBlockCipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO7816d4Padding());
            } else if (toUpperCase.equals("TBCPADDING")) {
                bufferedGenericBlockCipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new TBCPadding());
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Padding ");
                stringBuilder.append(str);
                stringBuilder.append(" unknown.");
                throw new NoSuchPaddingException(stringBuilder.toString());
            }
        }
        this.cipher = bufferedGenericBlockCipher;
    }

    protected int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException {
        if (this.cipher.getUpdateOutputSize(i2) + i3 <= bArr2.length) {
            try {
                return this.cipher.processBytes(bArr, i, i2, bArr2, i3);
            } catch (DataLengthException e) {
                throw new IllegalStateException(e.toString());
            }
        }
        throw new ShortBufferException("output buffer too short for input.");
    }

    protected byte[] engineUpdate(byte[] bArr, int i, int i2) {
        int updateOutputSize = this.cipher.getUpdateOutputSize(i2);
        if (updateOutputSize > 0) {
            Object obj = new byte[updateOutputSize];
            int processBytes = this.cipher.processBytes(bArr, i, i2, obj, 0);
            if (processBytes == 0) {
                return null;
            }
            if (processBytes == obj.length) {
                return obj;
            }
            Object obj2 = new byte[processBytes];
            System.arraycopy(obj, 0, obj2, 0, processBytes);
            return obj2;
        }
        this.cipher.processBytes(bArr, i, i2, null, 0);
        return null;
    }

    protected void engineUpdateAAD(ByteBuffer byteBuffer) {
        engineUpdateAAD(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.limit() - byteBuffer.position());
    }

    protected void engineUpdateAAD(byte[] bArr, int i, int i2) {
        this.cipher.updateAAD(bArr, i, i2);
    }
}
