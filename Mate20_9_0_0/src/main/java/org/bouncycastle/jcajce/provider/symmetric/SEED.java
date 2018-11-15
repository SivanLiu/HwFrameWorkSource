package org.bouncycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.kisa.KISAObjectIdentifiers;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.SEEDEngine;
import org.bouncycastle.crypto.engines.SEEDWrapEngine;
import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.macs.GMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BlockCipherProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;

public final class SEED {

    public static class AlgParamGen extends BaseAlgorithmParameterGenerator {
        protected AlgorithmParameters engineGenerateParameters() {
            byte[] bArr = new byte[16];
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            this.random.nextBytes(bArr);
            try {
                AlgorithmParameters createParametersInstance = createParametersInstance("SEED");
                createParametersInstance.init(new IvParameterSpec(bArr));
                return createParametersInstance;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for SEED parameter generation.");
        }
    }

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            super("SEED", 128, new CipherKeyGenerator());
        }
    }

    public static class Poly1305KeyGen extends BaseKeyGenerator {
        public Poly1305KeyGen() {
            super("Poly1305-SEED", 256, new Poly1305KeyGenerator());
        }
    }

    public static class AlgParams extends IvAlgorithmParameters {
        protected String engineToString() {
            return "SEED IV";
        }
    }

    public static class CMAC extends BaseMac {
        public CMAC() {
            super(new CMac(new SEEDEngine()));
        }
    }

    public static class GMAC extends BaseMac {
        public GMAC() {
            super(new GMac(new GCMBlockCipher(new SEEDEngine())));
        }
    }

    public static class Mappings extends SymmetricAlgorithmProvider {
        private static final String PREFIX = SEED.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.SEED", stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder2.append(KISAObjectIdentifiers.id_seedCBC);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "SEED");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGen");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.SEED", stringBuilder.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder2.append(KISAObjectIdentifiers.id_seedCBC);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "SEED");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB");
            configurableProvider.addAlgorithm("Cipher.SEED", stringBuilder.toString());
            ASN1ObjectIdentifier aSN1ObjectIdentifier = KISAObjectIdentifiers.id_seedCBC;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap");
            configurableProvider.addAlgorithm("Cipher.SEEDWRAP", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", KISAObjectIdentifiers.id_npki_app_cmsSeed_wrap, "SEEDWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.SEEDKW", "SEEDWRAP");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.SEED", stringBuilder.toString());
            aSN1ObjectIdentifier = KISAObjectIdentifiers.id_seedCBC;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder3.toString());
            aSN1ObjectIdentifier = KISAObjectIdentifiers.id_npki_app_cmsSeed_wrap;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$CMAC");
            String stringBuilder4 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGen");
            addCMacAlgorithm(configurableProvider, "SEED", stringBuilder4, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GMAC");
            stringBuilder4 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGen");
            addGMacAlgorithm(configurableProvider, "SEED", stringBuilder4, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Poly1305");
            stringBuilder4 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$Poly1305KeyGen");
            addPoly1305Algorithm(configurableProvider, "SEED", stringBuilder4, stringBuilder3.toString());
        }
    }

    public static class Poly1305 extends BaseMac {
        public Poly1305() {
            super(new org.bouncycastle.crypto.macs.Poly1305(new SEEDEngine()));
        }
    }

    public static class Wrap extends BaseWrapCipher {
        public Wrap() {
            super(new SEEDWrapEngine());
        }
    }

    public static class CBC extends BaseBlockCipher {
        public CBC() {
            super(new CBCBlockCipher(new SEEDEngine()), 128);
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new BlockCipherProvider() {
                public BlockCipher get() {
                    return new SEEDEngine();
                }
            });
        }
    }

    private SEED() {
    }
}
