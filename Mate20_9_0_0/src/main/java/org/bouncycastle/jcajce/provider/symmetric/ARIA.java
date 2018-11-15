package org.bouncycastle.jcajce.provider.symmetric;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CCMParameters;
import org.bouncycastle.asn1.cms.GCMParameters;
import org.bouncycastle.asn1.nsri.NSRIObjectIdentifiers;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.ARIAEngine;
import org.bouncycastle.crypto.engines.ARIAWrapEngine;
import org.bouncycastle.crypto.engines.ARIAWrapPadEngine;
import org.bouncycastle.crypto.engines.RFC3211WrapEngine;
import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;
import org.bouncycastle.crypto.macs.GMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BlockCipherProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.bouncycastle.jcajce.spec.AEADParameterSpec;

public final class ARIA {

    public static class AlgParamGen extends BaseAlgorithmParameterGenerator {
        protected AlgorithmParameters engineGenerateParameters() {
            byte[] bArr = new byte[16];
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            this.random.nextBytes(bArr);
            try {
                AlgorithmParameters createParametersInstance = createParametersInstance("ARIA");
                createParametersInstance.init(new IvParameterSpec(bArr));
                return createParametersInstance;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for ARIA parameter generation.");
        }
    }

    public static class AlgParamsCCM extends BaseAlgorithmParameters {
        private CCMParameters ccmParams;

        protected byte[] engineGetEncoded() throws IOException {
            return this.ccmParams.getEncoded();
        }

        protected byte[] engineGetEncoded(String str) throws IOException {
            if (isASN1FormatString(str)) {
                return this.ccmParams.getEncoded();
            }
            throw new IOException("unknown format specified");
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
            if (GcmSpecUtil.isGcmSpec(algorithmParameterSpec)) {
                this.ccmParams = CCMParameters.getInstance(GcmSpecUtil.extractGcmParameters(algorithmParameterSpec));
            } else if (algorithmParameterSpec instanceof AEADParameterSpec) {
                AEADParameterSpec aEADParameterSpec = (AEADParameterSpec) algorithmParameterSpec;
                this.ccmParams = new CCMParameters(aEADParameterSpec.getNonce(), aEADParameterSpec.getMacSizeInBits() / 8);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AlgorithmParameterSpec class not recognized: ");
                stringBuilder.append(algorithmParameterSpec.getClass().getName());
                throw new InvalidParameterSpecException(stringBuilder.toString());
            }
        }

        protected void engineInit(byte[] bArr) throws IOException {
            this.ccmParams = CCMParameters.getInstance(bArr);
        }

        protected void engineInit(byte[] bArr, String str) throws IOException {
            if (isASN1FormatString(str)) {
                this.ccmParams = CCMParameters.getInstance(bArr);
                return;
            }
            throw new IOException("unknown format specified");
        }

        protected String engineToString() {
            return "CCM";
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
            if (cls == AlgorithmParameterSpec.class || GcmSpecUtil.isGcmSpec(cls)) {
                return GcmSpecUtil.gcmSpecExists() ? GcmSpecUtil.extractGcmSpec(this.ccmParams.toASN1Primitive()) : new AEADParameterSpec(this.ccmParams.getNonce(), this.ccmParams.getIcvLen() * 8);
            } else {
                if (cls == AEADParameterSpec.class) {
                    return new AEADParameterSpec(this.ccmParams.getNonce(), this.ccmParams.getIcvLen() * 8);
                }
                if (cls == IvParameterSpec.class) {
                    return new IvParameterSpec(this.ccmParams.getNonce());
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AlgorithmParameterSpec not recognized: ");
                stringBuilder.append(cls.getName());
                throw new InvalidParameterSpecException(stringBuilder.toString());
            }
        }
    }

    public static class AlgParamsGCM extends BaseAlgorithmParameters {
        private GCMParameters gcmParams;

        protected byte[] engineGetEncoded() throws IOException {
            return this.gcmParams.getEncoded();
        }

        protected byte[] engineGetEncoded(String str) throws IOException {
            if (isASN1FormatString(str)) {
                return this.gcmParams.getEncoded();
            }
            throw new IOException("unknown format specified");
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
            if (GcmSpecUtil.isGcmSpec(algorithmParameterSpec)) {
                this.gcmParams = GcmSpecUtil.extractGcmParameters(algorithmParameterSpec);
            } else if (algorithmParameterSpec instanceof AEADParameterSpec) {
                AEADParameterSpec aEADParameterSpec = (AEADParameterSpec) algorithmParameterSpec;
                this.gcmParams = new GCMParameters(aEADParameterSpec.getNonce(), aEADParameterSpec.getMacSizeInBits() / 8);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AlgorithmParameterSpec class not recognized: ");
                stringBuilder.append(algorithmParameterSpec.getClass().getName());
                throw new InvalidParameterSpecException(stringBuilder.toString());
            }
        }

        protected void engineInit(byte[] bArr) throws IOException {
            this.gcmParams = GCMParameters.getInstance(bArr);
        }

        protected void engineInit(byte[] bArr, String str) throws IOException {
            if (isASN1FormatString(str)) {
                this.gcmParams = GCMParameters.getInstance(bArr);
                return;
            }
            throw new IOException("unknown format specified");
        }

        protected String engineToString() {
            return "GCM";
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
            if (cls == AlgorithmParameterSpec.class || GcmSpecUtil.isGcmSpec(cls)) {
                return GcmSpecUtil.gcmSpecExists() ? GcmSpecUtil.extractGcmSpec(this.gcmParams.toASN1Primitive()) : new AEADParameterSpec(this.gcmParams.getNonce(), this.gcmParams.getIcvLen() * 8);
            } else {
                if (cls == AEADParameterSpec.class) {
                    return new AEADParameterSpec(this.gcmParams.getNonce(), this.gcmParams.getIcvLen() * 8);
                }
                if (cls == IvParameterSpec.class) {
                    return new IvParameterSpec(this.gcmParams.getNonce());
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AlgorithmParameterSpec not recognized: ");
                stringBuilder.append(cls.getName());
                throw new InvalidParameterSpecException(stringBuilder.toString());
            }
        }
    }

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            this(256);
        }

        public KeyGen(int i) {
            super("ARIA", i, new CipherKeyGenerator());
        }
    }

    public static class Poly1305KeyGen extends BaseKeyGenerator {
        public Poly1305KeyGen() {
            super("Poly1305-ARIA", 256, new Poly1305KeyGenerator());
        }
    }

    public static class AlgParams extends IvAlgorithmParameters {
        protected String engineToString() {
            return "ARIA IV";
        }
    }

    public static class GMAC extends BaseMac {
        public GMAC() {
            super(new GMac(new GCMBlockCipher(new ARIAEngine())));
        }
    }

    public static class KeyGen128 extends KeyGen {
        public KeyGen128() {
            super(128);
        }
    }

    public static class KeyGen192 extends KeyGen {
        public KeyGen192() {
            super(192);
        }
    }

    public static class KeyGen256 extends KeyGen {
        public KeyGen256() {
            super(256);
        }
    }

    public static class Mappings extends SymmetricAlgorithmProvider {
        private static final String PREFIX = ARIA.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.ARIA", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters", NSRIObjectIdentifiers.id_aria128_cbc, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters", NSRIObjectIdentifiers.id_aria192_cbc, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters", NSRIObjectIdentifiers.id_aria256_cbc, "ARIA");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGen");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.ARIA", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria128_cbc, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria192_cbc, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria256_cbc, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria128_ofb, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria192_ofb, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria256_ofb, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria128_cfb, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria192_cfb, "ARIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NSRIObjectIdentifiers.id_aria256_cfb, "ARIA");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB");
            configurableProvider.addAlgorithm("Cipher.ARIA", stringBuilder.toString());
            ASN1ObjectIdentifier aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_ecb;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$ECB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_ecb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$ECB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_ecb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$ECB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_cfb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CFB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_cfb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CFB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_cfb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CFB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_ofb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$OFB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_ofb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$OFB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_ofb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$OFB");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$RFC3211Wrap");
            configurableProvider.addAlgorithm("Cipher.ARIARFC3211WRAP", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap");
            configurableProvider.addAlgorithm("Cipher.ARIAWRAP", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria128_kw, "ARIAWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria192_kw, "ARIAWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria256_kw, "ARIAWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.ARIAKW", "ARIAWRAP");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$WrapPad");
            configurableProvider.addAlgorithm("Cipher.ARIAWRAPPAD", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria128_kwp, "ARIAWRAPPAD");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria192_kwp, "ARIAWRAPPAD");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria256_kwp, "ARIAWRAPPAD");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.ARIAKWP", "ARIAWRAPPAD");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.ARIA", stringBuilder.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_kw;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_kw;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_kw;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_kwp;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_kwp;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_kwp;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_ecb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_ecb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_ecb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_cfb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_cfb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_cfb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_ofb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_ofb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_ofb;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_ccm;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_ccm;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_ccm;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria128_gcm;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria192_gcm;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NSRIObjectIdentifiers.id_aria256_gcm;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGenCCM");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.ARIACCM", stringBuilder.toString());
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder3.append(NSRIObjectIdentifiers.id_aria128_ccm);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "CCM");
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder3.append(NSRIObjectIdentifiers.id_aria192_ccm);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "CCM");
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder3.append(NSRIObjectIdentifiers.id_aria256_ccm);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "CCM");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria128_ccm, "CCM");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria192_ccm, "CCM");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria256_ccm, "CCM");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGenGCM");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.ARIAGCM", stringBuilder.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder3.append(NSRIObjectIdentifiers.id_aria128_gcm);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "GCM");
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder3.append(NSRIObjectIdentifiers.id_aria192_gcm);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "GCM");
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder3.append(NSRIObjectIdentifiers.id_aria256_gcm);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "GCM");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria128_gcm, "GCM");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria192_gcm, "GCM");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NSRIObjectIdentifiers.id_aria256_gcm, "GCM");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GMAC");
            String stringBuilder4 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen");
            addGMacAlgorithm(configurableProvider, "ARIA", stringBuilder4, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Poly1305");
            stringBuilder4 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Poly1305KeyGen");
            addPoly1305Algorithm(configurableProvider, "ARIA", stringBuilder4, stringBuilder2.toString());
        }
    }

    public static class Poly1305 extends BaseMac {
        public Poly1305() {
            super(new org.bouncycastle.crypto.macs.Poly1305(new ARIAEngine()));
        }
    }

    public static class RFC3211Wrap extends BaseWrapCipher {
        public RFC3211Wrap() {
            super(new RFC3211WrapEngine(new ARIAEngine()), 16);
        }
    }

    public static class Wrap extends BaseWrapCipher {
        public Wrap() {
            super(new ARIAWrapEngine());
        }
    }

    public static class WrapPad extends BaseWrapCipher {
        public WrapPad() {
            super(new ARIAWrapPadEngine());
        }
    }

    public static class CBC extends BaseBlockCipher {
        public CBC() {
            super(new CBCBlockCipher(new ARIAEngine()), 128);
        }
    }

    public static class CFB extends BaseBlockCipher {
        public CFB() {
            super(new BufferedBlockCipher(new CFBBlockCipher(new ARIAEngine(), 128)), 128);
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new BlockCipherProvider() {
                public BlockCipher get() {
                    return new ARIAEngine();
                }
            });
        }
    }

    public static class OFB extends BaseBlockCipher {
        public OFB() {
            super(new BufferedBlockCipher(new OFBBlockCipher(new ARIAEngine(), 128)), 128);
        }
    }

    private ARIA() {
    }
}
