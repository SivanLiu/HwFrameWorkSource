package com.android.org.bouncycastle.jcajce.provider.symmetric.util;

import com.android.org.bouncycastle.asn1.cms.GCMParameters;
import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.BufferedBlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.OutputLengthException;
import com.android.org.bouncycastle.crypto.modes.AEADBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CBCBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CCMBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CFBBlockCipher;
import com.android.org.bouncycastle.crypto.modes.CTSBlockCipher;
import com.android.org.bouncycastle.crypto.modes.GCMBlockCipher;
import com.android.org.bouncycastle.crypto.modes.OFBBlockCipher;
import com.android.org.bouncycastle.crypto.modes.SICBlockCipher;
import com.android.org.bouncycastle.crypto.paddings.BlockCipherPadding;
import com.android.org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import com.android.org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import com.android.org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import com.android.org.bouncycastle.crypto.paddings.TBCPadding;
import com.android.org.bouncycastle.crypto.paddings.X923Padding;
import com.android.org.bouncycastle.crypto.paddings.ZeroBytePadding;
import com.android.org.bouncycastle.crypto.params.AEADParameters;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.jcajce.PKCS12Key;
import com.android.org.bouncycastle.jcajce.PKCS12KeyWithParameters;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util;
import com.android.org.bouncycastle.jcajce.spec.AEADParameterSpec;
import com.android.org.bouncycastle.util.Strings;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class BaseBlockCipher extends BaseWrapCipher implements PBE {
    private static final Class gcmSpecClass = lookup("javax.crypto.spec.GCMParameterSpec");
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

        InvalidKeyOrParametersException(String msg, Throwable cause) {
            super(msg);
            this.cause = cause;
        }

        public Throwable getCause() {
            return this.cause;
        }
    }

    private static class AEADGenericBlockCipher implements GenericBlockCipher {
        private static final Constructor aeadBadTagConstructor;
        private AEADBlockCipher cipher;

        static {
            Class aeadBadTagClass = BaseBlockCipher.lookup("javax.crypto.AEADBadTagException");
            if (aeadBadTagClass != null) {
                aeadBadTagConstructor = findExceptionConstructor(aeadBadTagClass);
            } else {
                aeadBadTagConstructor = null;
            }
        }

        private static Constructor findExceptionConstructor(Class clazz) {
            try {
                return clazz.getConstructor(new Class[]{String.class});
            } catch (Exception e) {
                return null;
            }
        }

        AEADGenericBlockCipher(AEADBlockCipher cipher) {
            this.cipher = cipher;
        }

        public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
            this.cipher.init(forEncryption, params);
        }

        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public boolean wrapOnNoPadding() {
            return false;
        }

        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len) {
            return this.cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len) {
            return this.cipher.getUpdateOutputSize(len);
        }

        public void updateAAD(byte[] input, int offset, int length) {
            this.cipher.processAADBytes(input, offset, length);
        }

        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(out, outOff);
            } catch (InvalidCipherTextException e) {
                if (aeadBadTagConstructor != null) {
                    BadPaddingException aeadBadTag = null;
                    try {
                        aeadBadTag = (BadPaddingException) aeadBadTagConstructor.newInstance(new Object[]{e.getMessage()});
                    } catch (Exception e2) {
                    }
                    if (aeadBadTag != null) {
                        throw aeadBadTag;
                    }
                }
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

    private static class BufferedGenericBlockCipher implements GenericBlockCipher {
        private BufferedBlockCipher cipher;

        BufferedGenericBlockCipher(BufferedBlockCipher cipher) {
            this.cipher = cipher;
        }

        BufferedGenericBlockCipher(BlockCipher cipher) {
            this.cipher = new PaddedBufferedBlockCipher(cipher);
        }

        BufferedGenericBlockCipher(BlockCipher cipher, BlockCipherPadding padding) {
            this.cipher = new PaddedBufferedBlockCipher(cipher, padding);
        }

        public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException {
            this.cipher.init(forEncryption, params);
        }

        public boolean wrapOnNoPadding() {
            return !(this.cipher instanceof CTSBlockCipher);
        }

        public String getAlgorithmName() {
            return this.cipher.getUnderlyingCipher().getAlgorithmName();
        }

        public BlockCipher getUnderlyingCipher() {
            return this.cipher.getUnderlyingCipher();
        }

        public int getOutputSize(int len) {
            return this.cipher.getOutputSize(len);
        }

        public int getUpdateOutputSize(int len) {
            return this.cipher.getUpdateOutputSize(len);
        }

        public void updateAAD(byte[] input, int offset, int length) {
            throw new UnsupportedOperationException("AAD is not supported in the current mode.");
        }

        public int processByte(byte in, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processByte(in, out, outOff);
        }

        public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
            return this.cipher.processBytes(in, inOff, len, out, outOff);
        }

        public int doFinal(byte[] out, int outOff) throws IllegalStateException, BadPaddingException {
            try {
                return this.cipher.doFinal(out, outOff);
            } catch (InvalidCipherTextException e) {
                throw new BadPaddingException(e.getMessage());
            }
        }
    }

    private static Class lookup(String className) {
        try {
            return BaseBlockCipher.class.getClassLoader().loadClass(className);
        } catch (Exception e) {
            return null;
        }
    }

    protected BaseBlockCipher(BlockCipher engine) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.cipher = new BufferedGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipher engine, int scheme, int digest, int keySizeInBits, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.scheme = scheme;
        this.digest = digest;
        this.keySizeInBits = keySizeInBits;
        this.ivLength = ivLength;
        this.cipher = new BufferedGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipherProvider provider) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = provider.get();
        this.engineProvider = provider;
        this.cipher = new BufferedGenericBlockCipher(provider.get());
    }

    protected BaseBlockCipher(AEADBlockCipher engine) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.ivLength = this.baseEngine.getBlockSize();
        this.cipher = new AEADGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(AEADBlockCipher engine, boolean fixedIv, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.fixedIv = fixedIv;
        this.ivLength = ivLength;
        this.cipher = new AEADGenericBlockCipher(engine);
    }

    protected BaseBlockCipher(BlockCipher engine, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine;
        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    protected BaseBlockCipher(BufferedBlockCipher engine, int ivLength) {
        this.availableSpecs = new Class[]{gcmSpecClass, IvParameterSpec.class, PBEParameterSpec.class};
        this.scheme = -1;
        this.ivLength = 0;
        this.fixedIv = true;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.modeName = null;
        this.baseEngine = engine.getUnderlyingCipher();
        this.cipher = new BufferedGenericBlockCipher(engine);
        this.ivLength = ivLength / 8;
    }

    protected int engineGetBlockSize() {
        return this.baseEngine.getBlockSize();
    }

    protected byte[] engineGetIV() {
        if (this.aeadParams != null) {
            return this.aeadParams.getNonce();
        }
        return this.ivParam != null ? this.ivParam.getIV() : null;
    }

    protected int engineGetKeySize(Key key) {
        return key.getEncoded().length * 8;
    }

    protected int engineGetOutputSize(int inputLen) {
        return this.cipher.getOutputSize(inputLen);
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
                String name = this.cipher.getUnderlyingCipher().getAlgorithmName();
                if (name.indexOf(47) >= 0) {
                    name = name.substring(0, name.indexOf(47));
                }
                try {
                    this.engineParams = createParametersInstance(name);
                    this.engineParams.init(new IvParameterSpec(this.ivParam.getIV()));
                } catch (Exception e3) {
                    throw new RuntimeException(e3.toString());
                }
            }
        }
        return this.engineParams;
    }

    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        this.modeName = Strings.toUpperCase(mode);
        if (this.modeName.equals("ECB")) {
            this.ivLength = 0;
            this.cipher = new BufferedGenericBlockCipher(this.baseEngine);
        } else if (this.modeName.equals("CBC")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new CBCBlockCipher(this.baseEngine));
        } else if (this.modeName.startsWith("OFB")) {
            this.ivLength = this.baseEngine.getBlockSize();
            if (this.modeName.length() != 3) {
                this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, Integer.parseInt(this.modeName.substring(3))));
                return;
            }
            this.cipher = new BufferedGenericBlockCipher(new OFBBlockCipher(this.baseEngine, 8 * this.baseEngine.getBlockSize()));
        } else if (this.modeName.startsWith("CFB")) {
            this.ivLength = this.baseEngine.getBlockSize();
            if (this.modeName.length() != 3) {
                this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, Integer.parseInt(this.modeName.substring(3))));
                return;
            }
            this.cipher = new BufferedGenericBlockCipher(new CFBBlockCipher(this.baseEngine, 8 * this.baseEngine.getBlockSize()));
        } else if (this.modeName.startsWith("CTR")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.fixedIv = false;
            this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(new SICBlockCipher(this.baseEngine)));
        } else if (this.modeName.startsWith("CTS")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(new CBCBlockCipher(this.baseEngine)));
        } else if (this.modeName.startsWith("CCM")) {
            this.ivLength = 13;
            this.cipher = new AEADGenericBlockCipher(new CCMBlockCipher(this.baseEngine));
        } else if (this.modeName.startsWith("GCM")) {
            this.ivLength = this.baseEngine.getBlockSize();
            this.cipher = new AEADGenericBlockCipher(new GCMBlockCipher(this.baseEngine));
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("can't support mode ");
            stringBuilder.append(mode);
            throw new NoSuchAlgorithmException(stringBuilder.toString());
        }
    }

    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        String paddingName = Strings.toUpperCase(padding);
        if (paddingName.equals("NOPADDING")) {
            if (this.cipher.wrapOnNoPadding()) {
                this.cipher = new BufferedGenericBlockCipher(new BufferedBlockCipher(this.cipher.getUnderlyingCipher()));
            }
        } else if (paddingName.equals("WITHCTS")) {
            this.cipher = new BufferedGenericBlockCipher(new CTSBlockCipher(this.cipher.getUnderlyingCipher()));
        } else {
            this.padded = true;
            if (isAEADModeName(this.modeName)) {
                throw new NoSuchPaddingException("Only NoPadding can be used with AEAD modes.");
            } else if (paddingName.equals("PKCS5PADDING") || paddingName.equals("PKCS7PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher());
            } else if (paddingName.equals("ZEROBYTEPADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ZeroBytePadding());
            } else if (paddingName.equals("ISO10126PADDING") || paddingName.equals("ISO10126-2PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO10126d2Padding());
            } else if (paddingName.equals("X9.23PADDING") || paddingName.equals("X923PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new X923Padding());
            } else if (paddingName.equals("ISO7816-4PADDING") || paddingName.equals("ISO9797-1PADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new ISO7816d4Padding());
            } else if (paddingName.equals("TBCPADDING")) {
                this.cipher = new BufferedGenericBlockCipher(this.cipher.getUnderlyingCipher(), new TBCPadding());
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Padding ");
                stringBuilder.append(padding);
                stringBuilder.append(" unknown.");
                throw new NoSuchPaddingException(stringBuilder.toString());
            }
        }
    }

    private boolean isBCPBEKeyWithoutIV(Key key) {
        return (key instanceof BCPBEKey) && !(((BCPBEKey) key).getParam() instanceof ParametersWithIV);
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        int i = opmode;
        Key key2 = key;
        AlgorithmParameterSpec algorithmParameterSpec = params;
        SecureRandom secureRandom = random;
        String str = null;
        this.pbeSpec = null;
        this.pbeAlgorithm = null;
        this.engineParams = null;
        this.aeadParams = null;
        if (!(key2 instanceof SecretKey)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Key for algorithm ");
            if (key2 != null) {
                str = key.getAlgorithm();
            }
            stringBuilder.append(str);
            stringBuilder.append(" not suitable for symmetric enryption.");
            throw new InvalidKeyException(stringBuilder.toString());
        } else if (algorithmParameterSpec == null && this.baseEngine.getAlgorithmName().startsWith("RC5-64")) {
            throw new InvalidAlgorithmParameterException("RC5 requires an RC5ParametersSpec to be passed in.");
        } else {
            CipherParameters pbeKeyParam;
            SecureRandom secureRandom2;
            int i2;
            if ((this.scheme == 2 || (key2 instanceof PKCS12Key)) && !isBCPBEKeyWithoutIV(key2)) {
                try {
                    SecretKey k = (SecretKey) key2;
                    if (algorithmParameterSpec instanceof PBEParameterSpec) {
                        this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
                    }
                    if ((k instanceof PBEKey) && this.pbeSpec == null) {
                        PBEKey pbeKey = (PBEKey) k;
                        if (pbeKey.getSalt() != null) {
                            this.pbeSpec = new PBEParameterSpec(pbeKey.getSalt(), pbeKey.getIterationCount());
                        } else {
                            throw new InvalidAlgorithmParameterException("PBEKey requires parameters to specify salt");
                        }
                    }
                    if (this.pbeSpec != null || (k instanceof PBEKey)) {
                        CipherParameters param;
                        if (key2 instanceof BCPBEKey) {
                            pbeKeyParam = ((BCPBEKey) key2).getParam();
                            if (pbeKeyParam instanceof ParametersWithIV) {
                                param = pbeKeyParam;
                            } else if (pbeKeyParam == null) {
                                throw new AssertionError("Unreachable code");
                            } else {
                                throw new InvalidKeyException("Algorithm requires a PBE key suitable for PKCS12");
                            }
                        }
                        param = Util.makePBEParameters(k.getEncoded(), 2, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
                        pbeKeyParam = param;
                        if (pbeKeyParam instanceof ParametersWithIV) {
                            this.ivParam = (ParametersWithIV) pbeKeyParam;
                        }
                    } else {
                        throw new InvalidKeyException("Algorithm requires a PBE key");
                    }
                } catch (Exception e) {
                    Exception exception = e;
                    throw new InvalidKeyException("PKCS12 requires a SecretKey/PBEKey");
                }
            } else if (key2 instanceof BCPBEKey) {
                BCPBEKey k2 = (BCPBEKey) key2;
                if (k2.getOID() != null) {
                    this.pbeAlgorithm = k2.getOID().getId();
                } else {
                    this.pbeAlgorithm = k2.getAlgorithm();
                }
                if (k2.getParam() != null) {
                    pbeKeyParam = adjustParameters(algorithmParameterSpec, k2.getParam());
                } else if (algorithmParameterSpec instanceof PBEParameterSpec) {
                    this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
                    if (this.pbeSpec.getSalt().length != 0 && this.pbeSpec.getIterationCount() > 0) {
                        k2 = new BCPBEKey(k2.getAlgorithm(), k2.getOID(), k2.getType(), k2.getDigest(), k2.getKeySize(), k2.getIvSize(), new PBEKeySpec(k2.getPassword(), this.pbeSpec.getSalt(), this.pbeSpec.getIterationCount(), k2.getKeySize()), null);
                    }
                    pbeKeyParam = Util.makePBEParameters(k2, algorithmParameterSpec, this.cipher.getUnderlyingCipher().getAlgorithmName());
                } else {
                    throw new InvalidAlgorithmParameterException("PBE requires PBE parameters to be set.");
                }
                if (pbeKeyParam instanceof ParametersWithIV) {
                    this.ivParam = (ParametersWithIV) pbeKeyParam;
                }
            } else if (key2 instanceof PBEKey) {
                PBEKey k3 = (PBEKey) key2;
                this.pbeSpec = (PBEParameterSpec) algorithmParameterSpec;
                if ((k3 instanceof PKCS12KeyWithParameters) && this.pbeSpec == null) {
                    this.pbeSpec = new PBEParameterSpec(k3.getSalt(), k3.getIterationCount());
                }
                pbeKeyParam = Util.makePBEParameters(k3.getEncoded(), this.scheme, this.digest, this.keySizeInBits, this.ivLength * 8, this.pbeSpec, this.cipher.getAlgorithmName());
                if (pbeKeyParam instanceof ParametersWithIV) {
                    this.ivParam = (ParametersWithIV) pbeKeyParam;
                }
            } else if (this.scheme == 0 || this.scheme == 4 || this.scheme == 1 || this.scheme == 5) {
                secureRandom2 = random;
                i2 = opmode;
                throw new InvalidKeyException("Algorithm requires a PBE key");
            } else {
                pbeKeyParam = new KeyParameter(key.getEncoded());
            }
            CipherParameters param2 = pbeKeyParam;
            if (algorithmParameterSpec instanceof AEADParameterSpec) {
                if (isAEADModeName(this.modeName) || (this.cipher instanceof AEADGenericBlockCipher)) {
                    KeyParameter keyParam;
                    AEADParameterSpec aeadSpec = (AEADParameterSpec) algorithmParameterSpec;
                    if (param2 instanceof ParametersWithIV) {
                        keyParam = (KeyParameter) ((ParametersWithIV) param2).getParameters();
                    } else {
                        keyParam = (KeyParameter) param2;
                    }
                    AEADParameters aEADParameters = new AEADParameters(keyParam, aeadSpec.getMacSizeInBits(), aeadSpec.getNonce(), aeadSpec.getAssociatedData());
                    this.aeadParams = aEADParameters;
                    param2 = aEADParameters;
                } else {
                    throw new InvalidAlgorithmParameterException("AEADParameterSpec can only be used with AEAD modes.");
                }
            } else if (algorithmParameterSpec instanceof IvParameterSpec) {
                if (this.ivLength != 0) {
                    IvParameterSpec p = (IvParameterSpec) algorithmParameterSpec;
                    if (p.getIV().length == this.ivLength || (this.cipher instanceof AEADGenericBlockCipher) || !this.fixedIv) {
                        if (param2 instanceof ParametersWithIV) {
                            param2 = new ParametersWithIV(((ParametersWithIV) param2).getParameters(), p.getIV());
                        } else {
                            param2 = new ParametersWithIV(param2, p.getIV());
                        }
                        this.ivParam = (ParametersWithIV) param2;
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("IV must be ");
                        stringBuilder2.append(this.ivLength);
                        stringBuilder2.append(" bytes long.");
                        throw new InvalidAlgorithmParameterException(stringBuilder2.toString());
                    }
                } else if (this.modeName != null && this.modeName.equals("ECB")) {
                    throw new InvalidAlgorithmParameterException("ECB mode does not use an IV");
                }
            } else if (gcmSpecClass == null || !gcmSpecClass.isInstance(algorithmParameterSpec)) {
                if (!(algorithmParameterSpec == null || (algorithmParameterSpec instanceof PBEParameterSpec))) {
                    throw new InvalidAlgorithmParameterException("unknown parameter type.");
                }
            } else if (isAEADModeName(this.modeName) || (this.cipher instanceof AEADGenericBlockCipher)) {
                try {
                    KeyParameter keyParam2;
                    Method tLen = gcmSpecClass.getDeclaredMethod("getTLen", new Class[0]);
                    Method iv = gcmSpecClass.getDeclaredMethod("getIV", new Class[0]);
                    if (param2 instanceof ParametersWithIV) {
                        keyParam2 = (KeyParameter) ((ParametersWithIV) param2).getParameters();
                    } else {
                        keyParam2 = (KeyParameter) param2;
                    }
                    AEADParameters aEADParameters2 = new AEADParameters(keyParam2, ((Integer) tLen.invoke(algorithmParameterSpec, new Object[0])).intValue(), (byte[]) iv.invoke(algorithmParameterSpec, new Object[0]));
                    this.aeadParams = aEADParameters2;
                    param2 = aEADParameters2;
                } catch (Exception e2) {
                    throw new InvalidAlgorithmParameterException("Cannot process GCMParameterSpec.");
                }
            } else {
                throw new InvalidAlgorithmParameterException("GCMParameterSpec can only be used with AEAD modes.");
            }
            if (this.ivLength == 0 || (param2 instanceof ParametersWithIV) || (param2 instanceof AEADParameters)) {
                secureRandom2 = random;
                i2 = opmode;
            } else {
                secureRandom2 = random;
                SecureRandom ivRandom = secureRandom2;
                if (ivRandom == null) {
                    ivRandom = new SecureRandom();
                }
                i2 = opmode;
                if (i2 == 1 || i2 == 3) {
                    byte[] iv2 = new byte[this.ivLength];
                    if (isBCPBEKeyWithoutIV(key2)) {
                        System.err.println(" ******** DEPRECATED FUNCTIONALITY ********");
                        System.err.println(" * You have initialized a cipher with a PBE key with no IV and");
                        System.err.println(" * have not provided an IV in the AlgorithmParameterSpec.  This");
                        System.err.println(" * configuration is deprecated.  The cipher will be initialized");
                        System.err.println(" * with an all-zero IV, but in a future release this call will");
                        System.err.println(" * throw an exception.");
                        new InvalidAlgorithmParameterException("No IV set when using PBE key").printStackTrace(System.err);
                    } else {
                        ivRandom.nextBytes(iv2);
                    }
                    param2 = new ParametersWithIV(param2, iv2);
                    this.ivParam = (ParametersWithIV) param2;
                } else if (this.cipher.getUnderlyingCipher().getAlgorithmName().indexOf("PGPCFB") < 0) {
                    if (isBCPBEKeyWithoutIV(key2)) {
                        System.err.println(" ******** DEPRECATED FUNCTIONALITY ********");
                        System.err.println(" * You have initialized a cipher with a PBE key with no IV and");
                        System.err.println(" * have not provided an IV in the AlgorithmParameterSpec.  This");
                        System.err.println(" * configuration is deprecated.  The cipher will be initialized");
                        System.err.println(" * with an all-zero IV, but in a future release this call will");
                        System.err.println(" * throw an exception.");
                        new InvalidAlgorithmParameterException("No IV set when using PBE key").printStackTrace(System.err);
                        param2 = new ParametersWithIV(param2, new byte[this.ivLength]);
                        this.ivParam = (ParametersWithIV) param2;
                    } else {
                        throw new InvalidAlgorithmParameterException("no IV set when one expected");
                    }
                }
            }
            if (secureRandom2 != null && this.padded) {
                param2 = new ParametersWithRandom(param2, secureRandom2);
            }
            switch (i2) {
                case 1:
                case 3:
                    this.cipher.init(true, param2);
                    break;
                case 2:
                case 4:
                    this.cipher.init(false, param2);
                    break;
                default:
                    try {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("unknown opmode ");
                        stringBuilder3.append(i2);
                        stringBuilder3.append(" passed");
                        throw new InvalidParameterException(stringBuilder3.toString());
                    } catch (Exception e3) {
                        throw new InvalidKeyOrParametersException(e3.getMessage(), e3);
                    }
            }
            if ((this.cipher instanceof AEADGenericBlockCipher) && this.aeadParams == null) {
                this.aeadParams = new AEADParameters((KeyParameter) this.ivParam.getParameters(), ((AEADGenericBlockCipher) this.cipher).cipher.getMac().length * 8, this.ivParam.getIV());
            }
        }
    }

    private CipherParameters adjustParameters(AlgorithmParameterSpec params, CipherParameters param) {
        if (param instanceof ParametersWithIV) {
            CipherParameters key = ((ParametersWithIV) param).getParameters();
            if (!(params instanceof IvParameterSpec)) {
                return param;
            }
            this.ivParam = new ParametersWithIV(key, ((IvParameterSpec) params).getIV());
            return this.ivParam;
        } else if (!(params instanceof IvParameterSpec)) {
            return param;
        } else {
            this.ivParam = new ParametersWithIV(param, ((IvParameterSpec) params).getIV());
            return this.ivParam;
        }
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec paramSpec = null;
        if (params != null) {
            for (int i = 0; i != this.availableSpecs.length; i++) {
                if (this.availableSpecs[i] != null) {
                    try {
                        paramSpec = params.getParameterSpec(this.availableSpecs[i]);
                        break;
                    } catch (Exception e) {
                    }
                }
            }
            if (paramSpec == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("can't handle parameter ");
                stringBuilder.append(params.toString());
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }
        engineInit(opmode, key, paramSpec, random);
        this.engineParams = params;
    }

    protected void engineInit(int opmode, Key key, SecureRandom random) throws InvalidKeyException {
        try {
            engineInit(opmode, key, (AlgorithmParameterSpec) null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    protected void engineUpdateAAD(byte[] input, int offset, int length) {
        this.cipher.updateAAD(input, offset, length);
    }

    protected void engineUpdateAAD(ByteBuffer bytebuffer) {
        engineUpdateAAD(bytebuffer.array(), bytebuffer.arrayOffset() + bytebuffer.position(), bytebuffer.limit() - bytebuffer.position());
    }

    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        int length = this.cipher.getUpdateOutputSize(inputLen);
        if (length > 0) {
            byte[] out = new byte[length];
            int len = this.cipher.processBytes(input, inputOffset, inputLen, out, 0);
            if (len == 0) {
                return null;
            }
            if (len == out.length) {
                return out;
            }
            byte[] tmp = new byte[len];
            System.arraycopy(out, 0, tmp, 0, len);
            return tmp;
        }
        this.cipher.processBytes(input, inputOffset, inputLen, null, 0);
        return null;
    }

    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        if (this.cipher.getUpdateOutputSize(inputLen) + outputOffset <= output.length) {
            try {
                return this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
            } catch (DataLengthException e) {
                throw new IllegalStateException(e.toString());
            }
        }
        throw new ShortBufferException("output buffer too short for input.");
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        int len = 0;
        byte[] tmp = new byte[engineGetOutputSize(inputLen)];
        if (inputLen != 0) {
            len = this.cipher.processBytes(input, inputOffset, inputLen, tmp, 0);
        }
        try {
            len += this.cipher.doFinal(tmp, len);
            if (len == tmp.length) {
                return tmp;
            }
            byte[] out = new byte[len];
            System.arraycopy(tmp, 0, out, 0, len);
            return out;
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        }
    }

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws IllegalBlockSizeException, BadPaddingException, ShortBufferException {
        int len = 0;
        if (engineGetOutputSize(inputLen) + outputOffset <= output.length) {
            if (inputLen != 0) {
                try {
                    len = this.cipher.processBytes(input, inputOffset, inputLen, output, outputOffset);
                } catch (OutputLengthException e) {
                    throw new IllegalBlockSizeException(e.getMessage());
                } catch (DataLengthException e2) {
                    throw new IllegalBlockSizeException(e2.getMessage());
                }
            }
            return this.cipher.doFinal(output, outputOffset + len) + len;
        }
        throw new ShortBufferException("output buffer too short for input.");
    }

    private boolean isAEADModeName(String modeName) {
        return "CCM".equals(modeName) || "GCM".equals(modeName);
    }
}
