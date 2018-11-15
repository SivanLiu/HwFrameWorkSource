package org.bouncycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ntt.NTTObjectIdentifiers;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.engines.CamelliaWrapEngine;
import org.bouncycastle.crypto.engines.RFC3211WrapEngine;
import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;
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

public final class Camellia {

    public static class AlgParamGen extends BaseAlgorithmParameterGenerator {
        protected AlgorithmParameters engineGenerateParameters() {
            byte[] bArr = new byte[16];
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            this.random.nextBytes(bArr);
            try {
                AlgorithmParameters createParametersInstance = createParametersInstance("Camellia");
                createParametersInstance.init(new IvParameterSpec(bArr));
                return createParametersInstance;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for Camellia parameter generation.");
        }
    }

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            this(256);
        }

        public KeyGen(int i) {
            super("Camellia", i, new CipherKeyGenerator());
        }
    }

    public static class Poly1305KeyGen extends BaseKeyGenerator {
        public Poly1305KeyGen() {
            super("Poly1305-Camellia", 256, new Poly1305KeyGenerator());
        }
    }

    public static class AlgParams extends IvAlgorithmParameters {
        protected String engineToString() {
            return "Camellia IV";
        }
    }

    public static class GMAC extends BaseMac {
        public GMAC() {
            super(new GMac(new GCMBlockCipher(new CamelliaEngine())));
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
        private static final String PREFIX = Camellia.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.CAMELLIA", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters", NTTObjectIdentifiers.id_camellia128_cbc, "CAMELLIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters", NTTObjectIdentifiers.id_camellia192_cbc, "CAMELLIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters", NTTObjectIdentifiers.id_camellia256_cbc, "CAMELLIA");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGen");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.CAMELLIA", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NTTObjectIdentifiers.id_camellia128_cbc, "CAMELLIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NTTObjectIdentifiers.id_camellia192_cbc, "CAMELLIA");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator", NTTObjectIdentifiers.id_camellia256_cbc, "CAMELLIA");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB");
            configurableProvider.addAlgorithm("Cipher.CAMELLIA", stringBuilder.toString());
            ASN1ObjectIdentifier aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia128_cbc;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia192_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia256_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$RFC3211Wrap");
            configurableProvider.addAlgorithm("Cipher.CAMELLIARFC3211WRAP", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap");
            configurableProvider.addAlgorithm("Cipher.CAMELLIAWRAP", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NTTObjectIdentifiers.id_camellia128_wrap, "CAMELLIAWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NTTObjectIdentifiers.id_camellia192_wrap, "CAMELLIAWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher", NTTObjectIdentifiers.id_camellia256_wrap, "CAMELLIAWRAP");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.CAMELLIA", stringBuilder.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia128_wrap;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia192_wrap;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia256_wrap;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia128_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia192_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen192");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NTTObjectIdentifiers.id_camellia256_cbc;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GMAC");
            String stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen");
            addGMacAlgorithm(configurableProvider, "CAMELLIA", stringBuilder3, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Poly1305");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Poly1305KeyGen");
            addPoly1305Algorithm(configurableProvider, "CAMELLIA", stringBuilder3, stringBuilder2.toString());
        }
    }

    public static class Poly1305 extends BaseMac {
        public Poly1305() {
            super(new org.bouncycastle.crypto.macs.Poly1305(new CamelliaEngine()));
        }
    }

    public static class RFC3211Wrap extends BaseWrapCipher {
        public RFC3211Wrap() {
            super(new RFC3211WrapEngine(new CamelliaEngine()), 16);
        }
    }

    public static class Wrap extends BaseWrapCipher {
        public Wrap() {
            super(new CamelliaWrapEngine());
        }
    }

    public static class CBC extends BaseBlockCipher {
        public CBC() {
            super(new CBCBlockCipher(new CamelliaEngine()), 128);
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new BlockCipherProvider() {
                public BlockCipher get() {
                    return new CamelliaEngine();
                }
            });
        }
    }

    private Camellia() {
    }
}
