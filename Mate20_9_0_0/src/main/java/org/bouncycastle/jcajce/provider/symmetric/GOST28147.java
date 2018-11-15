package org.bouncycastle.jcajce.provider.symmetric;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.cryptopro.GOST28147Parameters;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.CryptoProWrapEngine;
import org.bouncycastle.crypto.engines.GOST28147Engine;
import org.bouncycastle.crypto.engines.GOST28147WrapEngine;
import org.bouncycastle.crypto.macs.GOST28147Mac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.GCFBBlockCipher;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;
import org.bouncycastle.jcajce.spec.GOST28147ParameterSpec;

public final class GOST28147 {
    private static Map<String, ASN1ObjectIdentifier> nameMappings = new HashMap();
    private static Map<ASN1ObjectIdentifier, String> oidMappings = new HashMap();

    public static class AlgParamGen extends BaseAlgorithmParameterGenerator {
        byte[] iv = new byte[8];
        byte[] sBox = GOST28147Engine.getSBox("E-A");

        protected AlgorithmParameters engineGenerateParameters() {
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            this.random.nextBytes(this.iv);
            try {
                AlgorithmParameters createParametersInstance = createParametersInstance("GOST28147");
                createParametersInstance.init(new GOST28147ParameterSpec(this.sBox, this.iv));
                return createParametersInstance;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            if (algorithmParameterSpec instanceof GOST28147ParameterSpec) {
                this.sBox = ((GOST28147ParameterSpec) algorithmParameterSpec).getSBox();
                return;
            }
            throw new InvalidAlgorithmParameterException("parameter spec not supported");
        }
    }

    public static abstract class BaseAlgParams extends BaseAlgorithmParameters {
        private byte[] iv;
        private ASN1ObjectIdentifier sBox = CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_A_ParamSet;

        protected static ASN1ObjectIdentifier getSBoxOID(String str) {
            ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) GOST28147.nameMappings.get(str);
            if (aSN1ObjectIdentifier != null) {
                return aSN1ObjectIdentifier;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown SBOX name: ");
            stringBuilder.append(str);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        protected static ASN1ObjectIdentifier getSBoxOID(byte[] bArr) {
            return getSBoxOID(GOST28147Engine.getSBoxName(bArr));
        }

        protected final byte[] engineGetEncoded() throws IOException {
            return engineGetEncoded("ASN.1");
        }

        protected final byte[] engineGetEncoded(String str) throws IOException {
            if (isASN1FormatString(str)) {
                return localGetEncoded();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown parameter format: ");
            stringBuilder.append(str);
            throw new IOException(stringBuilder.toString());
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
            if (algorithmParameterSpec instanceof IvParameterSpec) {
                this.iv = ((IvParameterSpec) algorithmParameterSpec).getIV();
            } else if (algorithmParameterSpec instanceof GOST28147ParameterSpec) {
                this.iv = ((GOST28147ParameterSpec) algorithmParameterSpec).getIV();
                try {
                    this.sBox = getSBoxOID(((GOST28147ParameterSpec) algorithmParameterSpec).getSBox());
                } catch (IllegalArgumentException e) {
                    throw new InvalidParameterSpecException(e.getMessage());
                }
            } else {
                throw new InvalidParameterSpecException("IvParameterSpec required to initialise a IV parameters algorithm parameters object");
            }
        }

        protected final void engineInit(byte[] bArr) throws IOException {
            engineInit(bArr, "ASN.1");
        }

        protected final void engineInit(byte[] bArr, String str) throws IOException {
            StringBuilder stringBuilder;
            if (bArr == null) {
                throw new NullPointerException("Encoded parameters cannot be null");
            } else if (isASN1FormatString(str)) {
                try {
                    localInit(bArr);
                } catch (IOException e) {
                    throw e;
                } catch (Exception e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Parameter parsing failed: ");
                    stringBuilder.append(e2.getMessage());
                    throw new IOException(stringBuilder.toString());
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown parameter format: ");
                stringBuilder.append(str);
                throw new IOException(stringBuilder.toString());
            }
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
            if (cls == IvParameterSpec.class) {
                return new IvParameterSpec(this.iv);
            }
            if (cls == GOST28147ParameterSpec.class || cls == AlgorithmParameterSpec.class) {
                return new GOST28147ParameterSpec(this.sBox, this.iv);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AlgorithmParameterSpec not recognized: ");
            stringBuilder.append(cls.getName());
            throw new InvalidParameterSpecException(stringBuilder.toString());
        }

        protected byte[] localGetEncoded() throws IOException {
            return new GOST28147Parameters(this.iv, this.sBox).getEncoded();
        }

        abstract void localInit(byte[] bArr) throws IOException;
    }

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            this(256);
        }

        public KeyGen(int i) {
            super("GOST28147", i, new CipherKeyGenerator());
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PREFIX = GOST28147.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB");
            configurableProvider.addAlgorithm("Cipher.GOST28147", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.GOST", "GOST28147");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.GOST-28147", "GOST28147");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Cipher.");
            stringBuilder2.append(CryptoProObjectIdentifiers.gostR28147_gcfb);
            String stringBuilder3 = stringBuilder2.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GCFB");
            configurableProvider.addAlgorithm(stringBuilder3, stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.GOST28147", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.KeyGenerator.GOST", "GOST28147");
            configurableProvider.addAlgorithm("Alg.Alias.KeyGenerator.GOST-28147", "GOST28147");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.KeyGenerator.");
            stringBuilder2.append(CryptoProObjectIdentifiers.gostR28147_gcfb);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "GOST28147");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.GOST28147", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGen");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.GOST28147", stringBuilder.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder2.append(CryptoProObjectIdentifiers.gostR28147_gcfb);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "GOST28147");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder2.append(CryptoProObjectIdentifiers.gostR28147_gcfb);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "GOST28147");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Cipher.");
            stringBuilder2.append(CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_KeyWrap);
            stringBuilder3 = stringBuilder2.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$CryptoProWrap");
            configurableProvider.addAlgorithm(stringBuilder3, stringBuilder.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Cipher.");
            stringBuilder2.append(CryptoProObjectIdentifiers.id_Gost28147_89_None_KeyWrap);
            stringBuilder3 = stringBuilder2.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GostWrap");
            configurableProvider.addAlgorithm(stringBuilder3, stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Mac");
            configurableProvider.addAlgorithm("Mac.GOST28147MAC", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.GOST28147", "GOST28147MAC");
        }
    }

    public static class AlgParams extends BaseAlgParams {
        private byte[] iv;
        private ASN1ObjectIdentifier sBox = CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_A_ParamSet;

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
            if (algorithmParameterSpec instanceof IvParameterSpec) {
                this.iv = ((IvParameterSpec) algorithmParameterSpec).getIV();
            } else if (algorithmParameterSpec instanceof GOST28147ParameterSpec) {
                this.iv = ((GOST28147ParameterSpec) algorithmParameterSpec).getIV();
                try {
                    this.sBox = BaseAlgParams.getSBoxOID(((GOST28147ParameterSpec) algorithmParameterSpec).getSBox());
                } catch (IllegalArgumentException e) {
                    throw new InvalidParameterSpecException(e.getMessage());
                }
            } else {
                throw new InvalidParameterSpecException("IvParameterSpec required to initialise a IV parameters algorithm parameters object");
            }
        }

        protected String engineToString() {
            return "GOST 28147 IV Parameters";
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
            if (cls == IvParameterSpec.class) {
                return new IvParameterSpec(this.iv);
            }
            if (cls == GOST28147ParameterSpec.class || cls == AlgorithmParameterSpec.class) {
                return new GOST28147ParameterSpec(this.sBox, this.iv);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AlgorithmParameterSpec not recognized: ");
            stringBuilder.append(cls.getName());
            throw new InvalidParameterSpecException(stringBuilder.toString());
        }

        protected byte[] localGetEncoded() throws IOException {
            return new GOST28147Parameters(this.iv, this.sBox).getEncoded();
        }

        /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
            jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0026 in {2, 4, 7, 10} preds:[]
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
            	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
            	at jadx.api.JavaClass.decompile(JavaClass.java:62)
            	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
            */
        protected void localInit(byte[] r2) throws java.io.IOException {
            /*
            r1 = this;
            r2 = org.bouncycastle.asn1.ASN1Primitive.fromByteArray(r2);
            r0 = r2 instanceof org.bouncycastle.asn1.ASN1OctetString;
            if (r0 == 0) goto L_0x0013;
        L_0x0008:
            r2 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r2);
            r2 = r2.getOctets();
        L_0x0010:
            r1.iv = r2;
            return;
        L_0x0013:
            r0 = r2 instanceof org.bouncycastle.asn1.ASN1Sequence;
            if (r0 == 0) goto L_0x0027;
        L_0x0017:
            r2 = org.bouncycastle.asn1.cryptopro.GOST28147Parameters.getInstance(r2);
            r0 = r2.getEncryptionParamSet();
            r1.sBox = r0;
            r2 = r2.getIV();
            goto L_0x0010;
            return;
        L_0x0027:
            r2 = new java.io.IOException;
            r0 = "Unable to recognize parameters";
            r2.<init>(r0);
            throw r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.symmetric.GOST28147.AlgParams.localInit(byte[]):void");
        }
    }

    public static class CryptoProWrap extends BaseWrapCipher {
        public CryptoProWrap() {
            super(new CryptoProWrapEngine());
        }
    }

    public static class GostWrap extends BaseWrapCipher {
        public GostWrap() {
            super(new GOST28147WrapEngine());
        }
    }

    public static class Mac extends BaseMac {
        public Mac() {
            super(new GOST28147Mac());
        }
    }

    public static class CBC extends BaseBlockCipher {
        public CBC() {
            super(new CBCBlockCipher(new GOST28147Engine()), 64);
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new GOST28147Engine());
        }
    }

    public static class GCFB extends BaseBlockCipher {
        public GCFB() {
            super(new BufferedBlockCipher(new GCFBBlockCipher(new GOST28147Engine())), 64);
        }
    }

    static {
        oidMappings.put(CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_TestParamSet, "E-TEST");
        oidMappings.put(CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_A_ParamSet, "E-A");
        oidMappings.put(CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_B_ParamSet, "E-B");
        oidMappings.put(CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_C_ParamSet, "E-C");
        oidMappings.put(CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_D_ParamSet, "E-D");
        nameMappings.put("E-A", CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_A_ParamSet);
        nameMappings.put("E-B", CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_B_ParamSet);
        nameMappings.put("E-C", CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_C_ParamSet);
        nameMappings.put("E-D", CryptoProObjectIdentifiers.id_Gost28147_89_CryptoPro_D_ParamSet);
    }

    private GOST28147() {
    }
}
