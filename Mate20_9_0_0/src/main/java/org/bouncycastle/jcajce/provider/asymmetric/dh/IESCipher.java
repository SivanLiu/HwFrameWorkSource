package org.bouncycastle.jcajce.provider.asymmetric.dh;

import java.io.ByteArrayOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.DHKey;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.KeyEncoder;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.IESEngine;
import org.bouncycastle.crypto.generators.DHKeyPairGenerator;
import org.bouncycastle.crypto.generators.EphemeralKeyPairGenerator;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHKeyParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.IESWithCipherParameters;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.parsers.DHIESPublicKeyParser;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.jcajce.provider.asymmetric.util.DHUtil;
import org.bouncycastle.jcajce.provider.asymmetric.util.IESUtil;
import org.bouncycastle.jcajce.provider.util.BadBlockException;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.interfaces.IESKey;
import org.bouncycastle.jce.spec.IESParameterSpec;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.Strings;

public class IESCipher extends CipherSpi {
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean dhaesMode = false;
    private IESEngine engine;
    private AlgorithmParameters engineParam = null;
    private IESParameterSpec engineSpec = null;
    private final JcaJceHelper helper = new BCJcaJceHelper();
    private final int ivLength;
    private AsymmetricKeyParameter key;
    private AsymmetricKeyParameter otherKeyParameter = null;
    private SecureRandom random;
    private int state = -1;

    public static class IES extends IESCipher {
        public IES() {
            super(new IESEngine(new DHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA1()), new HMac(DigestFactory.createSHA1())));
        }
    }

    public static class IESwithAESCBC extends IESCipher {
        public IESwithAESCBC() {
            super(new IESEngine(new DHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA1()), new HMac(DigestFactory.createSHA1()), new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()))), 16);
        }
    }

    public static class IESwithDESedeCBC extends IESCipher {
        public IESwithDESedeCBC() {
            super(new IESEngine(new DHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA1()), new HMac(DigestFactory.createSHA1()), new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESedeEngine()))), 8);
        }
    }

    public IESCipher(IESEngine iESEngine) {
        this.engine = iESEngine;
        this.ivLength = 0;
    }

    public IESCipher(IESEngine iESEngine, int i) {
        this.engine = iESEngine;
        this.ivLength = i;
    }

    public int engineDoFinal(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        Object engineDoFinal = engineDoFinal(bArr, i, i2);
        System.arraycopy(engineDoFinal, 0, bArr2, i3, engineDoFinal.length);
        return engineDoFinal.length;
    }

    public byte[] engineDoFinal(byte[] bArr, int i, int i2) throws IllegalBlockSizeException, BadPaddingException {
        if (i2 != 0) {
            this.buffer.write(bArr, i, i2);
        }
        bArr = this.buffer.toByteArray();
        this.buffer.reset();
        CipherParameters iESWithCipherParameters = new IESWithCipherParameters(this.engineSpec.getDerivationV(), this.engineSpec.getEncodingV(), this.engineSpec.getMacKeySize(), this.engineSpec.getCipherKeySize());
        if (this.engineSpec.getNonce() != null) {
            iESWithCipherParameters = new ParametersWithIV(iESWithCipherParameters, this.engineSpec.getNonce());
        }
        DHParameters parameters = ((DHKeyParameters) this.key).getParameters();
        if (this.otherKeyParameter != null) {
            try {
                if (this.state == 1 || this.state == 3) {
                    this.engine.init(true, this.otherKeyParameter, this.key, iESWithCipherParameters);
                } else {
                    this.engine.init(false, this.key, this.otherKeyParameter, iESWithCipherParameters);
                }
                return this.engine.processBlock(bArr, 0, bArr.length);
            } catch (Throwable e) {
                throw new BadBlockException("unable to process block", e);
            }
        } else if (this.state == 1 || this.state == 3) {
            AsymmetricCipherKeyPairGenerator dHKeyPairGenerator = new DHKeyPairGenerator();
            dHKeyPairGenerator.init(new DHKeyGenerationParameters(this.random, parameters));
            try {
                this.engine.init(this.key, iESWithCipherParameters, new EphemeralKeyPairGenerator(dHKeyPairGenerator, new KeyEncoder() {
                    public byte[] getEncoded(AsymmetricKeyParameter asymmetricKeyParameter) {
                        Object obj = new byte[((((DHKeyParameters) asymmetricKeyParameter).getParameters().getP().bitLength() + 7) / 8)];
                        Object asUnsignedByteArray = BigIntegers.asUnsignedByteArray(((DHPublicKeyParameters) asymmetricKeyParameter).getY());
                        if (asUnsignedByteArray.length <= obj.length) {
                            System.arraycopy(asUnsignedByteArray, 0, obj, obj.length - asUnsignedByteArray.length, asUnsignedByteArray.length);
                            return obj;
                        }
                        throw new IllegalArgumentException("Senders's public key longer than expected.");
                    }
                }));
                return this.engine.processBlock(bArr, 0, bArr.length);
            } catch (Throwable e2) {
                throw new BadBlockException("unable to process block", e2);
            }
        } else if (this.state == 2 || this.state == 4) {
            try {
                this.engine.init(this.key, iESWithCipherParameters, new DHIESPublicKeyParser(((DHKeyParameters) this.key).getParameters()));
                return this.engine.processBlock(bArr, 0, bArr.length);
            } catch (Throwable e22) {
                throw new BadBlockException("unable to process block", e22);
            }
        } else {
            throw new IllegalStateException("IESCipher not initialised");
        }
    }

    public int engineGetBlockSize() {
        return this.engine.getCipher() != null ? this.engine.getCipher().getBlockSize() : 0;
    }

    public byte[] engineGetIV() {
        return this.engineSpec != null ? this.engineSpec.getNonce() : null;
    }

    public int engineGetKeySize(Key key) {
        if (key instanceof DHKey) {
            return ((DHKey) key).getParams().getP().bitLength();
        }
        throw new IllegalArgumentException("not a DH key");
    }

    public int engineGetOutputSize(int i) {
        if (this.key != null) {
            int size;
            int macSize = this.engine.getMac().getMacSize();
            int bitLength = this.otherKeyParameter == null ? (((((DHKeyParameters) this.key).getParameters().getP().bitLength() + 7) * 2) / 8) + 1 : 0;
            if (this.engine.getCipher() != null) {
                BufferedBlockCipher cipher;
                if (this.state == 1 || this.state == 3) {
                    cipher = this.engine.getCipher();
                } else if (this.state == 2 || this.state == 4) {
                    cipher = this.engine.getCipher();
                    i = (i - macSize) - bitLength;
                } else {
                    throw new IllegalStateException("cipher not initialised");
                }
                i = cipher.getOutputSize(i);
            }
            if (this.state == 1 || this.state == 3) {
                size = (this.buffer.size() + macSize) + bitLength;
            } else if (this.state == 2 || this.state == 4) {
                size = (this.buffer.size() - macSize) - bitLength;
            } else {
                throw new IllegalStateException("IESCipher not initialised");
            }
            return size + i;
        }
        throw new IllegalStateException("cipher not initialised");
    }

    public AlgorithmParameters engineGetParameters() {
        if (this.engineParam == null && this.engineSpec != null) {
            try {
                this.engineParam = this.helper.createAlgorithmParameters("IES");
                this.engineParam.init(this.engineSpec);
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
        }
        return this.engineParam;
    }

    public void engineInit(int i, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec parameterSpec;
        if (algorithmParameters != null) {
            try {
                parameterSpec = algorithmParameters.getParameterSpec(IESParameterSpec.class);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cannot recognise parameters: ");
                stringBuilder.append(e.toString());
                throw new InvalidAlgorithmParameterException(stringBuilder.toString());
            }
        }
        parameterSpec = null;
        this.engineParam = algorithmParameters;
        engineInit(i, key, parameterSpec, secureRandom);
    }

    public void engineInit(int i, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        try {
            engineInit(i, key, (AlgorithmParameterSpec) null, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cannot handle supplied parameter spec: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void engineInit(int i, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] bArr;
        IESParameterSpec guessParameterSpec;
        if (algorithmParameterSpec == null) {
            bArr = null;
            if (this.ivLength != 0 && i == 1) {
                bArr = new byte[this.ivLength];
                secureRandom.nextBytes(bArr);
            }
            guessParameterSpec = IESUtil.guessParameterSpec(this.engine.getCipher(), bArr);
        } else if (algorithmParameterSpec instanceof IESParameterSpec) {
            guessParameterSpec = (IESParameterSpec) algorithmParameterSpec;
        } else {
            throw new InvalidAlgorithmParameterException("must be passed IES parameters");
        }
        this.engineSpec = guessParameterSpec;
        bArr = this.engineSpec.getNonce();
        if (this.ivLength == 0 || (bArr != null && bArr.length == this.ivLength)) {
            AsymmetricKeyParameter generatePublicKeyParameter;
            IESKey iESKey;
            if (i == 1 || i == 3) {
                if (key instanceof DHPublicKey) {
                    generatePublicKeyParameter = DHUtil.generatePublicKeyParameter((PublicKey) key);
                } else if (key instanceof IESKey) {
                    iESKey = (IESKey) key;
                    this.key = DHUtil.generatePublicKeyParameter(iESKey.getPublic());
                    this.otherKeyParameter = DHUtil.generatePrivateKeyParameter(iESKey.getPrivate());
                    this.random = secureRandom;
                    this.state = i;
                    this.buffer.reset();
                    return;
                } else {
                    throw new InvalidKeyException("must be passed recipient's public DH key for encryption");
                }
            } else if (i == 2 || i == 4) {
                PrivateKey privateKey;
                if (key instanceof DHPrivateKey) {
                    privateKey = (PrivateKey) key;
                } else if (key instanceof IESKey) {
                    iESKey = (IESKey) key;
                    this.otherKeyParameter = DHUtil.generatePublicKeyParameter(iESKey.getPublic());
                    privateKey = iESKey.getPrivate();
                } else {
                    throw new InvalidKeyException("must be passed recipient's private DH key for decryption");
                }
                generatePublicKeyParameter = DHUtil.generatePrivateKeyParameter(privateKey);
            } else {
                throw new InvalidKeyException("must be passed EC key");
            }
            this.key = generatePublicKeyParameter;
            this.random = secureRandom;
            this.state = i;
            this.buffer.reset();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NONCE in IES Parameters needs to be ");
        stringBuilder.append(this.ivLength);
        stringBuilder.append(" bytes long");
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x001a in {2, 4, 7, 10} preds:[]
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
    public void engineSetMode(java.lang.String r4) throws java.security.NoSuchAlgorithmException {
        /*
        r3 = this;
        r0 = org.bouncycastle.util.Strings.toUpperCase(r4);
        r1 = "NONE";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x0010;
    L_0x000c:
        r4 = 0;
    L_0x000d:
        r3.dhaesMode = r4;
        return;
    L_0x0010:
        r1 = "DHAES";
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x001b;
    L_0x0018:
        r4 = 1;
        goto L_0x000d;
        return;
    L_0x001b:
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "can't support mode ";
        r1.append(r2);
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher.engineSetMode(java.lang.String):void");
    }

    public void engineSetPadding(String str) throws NoSuchPaddingException {
        str = Strings.toUpperCase(str);
        if (!str.equals("NOPADDING") && !str.equals("PKCS5PADDING") && !str.equals("PKCS7PADDING")) {
            throw new NoSuchPaddingException("padding not available with IESCipher");
        }
    }

    public int engineUpdate(byte[] bArr, int i, int i2, byte[] bArr2, int i3) {
        this.buffer.write(bArr, i, i2);
        return 0;
    }

    public byte[] engineUpdate(byte[] bArr, int i, int i2) {
        this.buffer.write(bArr, i, i2);
        return null;
    }
}
