package org.bouncycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ua.UAObjectIdentifiers;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.DSTU7624Engine;
import org.bouncycastle.crypto.engines.DSTU7624WrapEngine;
import org.bouncycastle.crypto.macs.KGMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.KCCMBlockCipher;
import org.bouncycastle.crypto.modes.KCTRBlockCipher;
import org.bouncycastle.crypto.modes.KGCMBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BlockCipherProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;

public class DSTU7624 {

    public static class AlgParamGen extends BaseAlgorithmParameterGenerator {
        private final int ivLength;

        public AlgParamGen(int i) {
            this.ivLength = i / 8;
        }

        protected AlgorithmParameters engineGenerateParameters() {
            byte[] bArr = new byte[this.ivLength];
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            this.random.nextBytes(bArr);
            try {
                AlgorithmParameters createParametersInstance = createParametersInstance("DSTU7624");
                createParametersInstance.init(new IvParameterSpec(bArr));
                return createParametersInstance;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for DSTU7624 parameter generation.");
        }
    }

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            this(256);
        }

        public KeyGen(int i) {
            super("DSTU7624", i, new CipherKeyGenerator());
        }
    }

    public static class AlgParamGen128 extends AlgParamGen {
        AlgParamGen128() {
            super(128);
        }
    }

    public static class AlgParamGen256 extends AlgParamGen {
        AlgParamGen256() {
            super(256);
        }
    }

    public static class AlgParamGen512 extends AlgParamGen {
        AlgParamGen512() {
            super(512);
        }
    }

    public static class AlgParams extends IvAlgorithmParameters {
        protected String engineToString() {
            return "DSTU7624 IV";
        }
    }

    public static class GMAC128 extends BaseMac {
        public GMAC128() {
            super(new KGMac(new KGCMBlockCipher(new DSTU7624Engine(128)), 128));
        }
    }

    public static class GMAC256 extends BaseMac {
        public GMAC256() {
            super(new KGMac(new KGCMBlockCipher(new DSTU7624Engine(256)), 256));
        }
    }

    public static class GMAC512 extends BaseMac {
        public GMAC512() {
            super(new KGMac(new KGCMBlockCipher(new DSTU7624Engine(512)), 512));
        }
    }

    public static class GMAC extends BaseMac {
        public GMAC() {
            super(new KGMac(new KGCMBlockCipher(new DSTU7624Engine(128)), 128));
        }
    }

    public static class KeyGen128 extends KeyGen {
        public KeyGen128() {
            super(128);
        }
    }

    public static class KeyGen256 extends KeyGen {
        public KeyGen256() {
            super(256);
        }
    }

    public static class KeyGen512 extends KeyGen {
        public KeyGen512() {
            super(512);
        }
    }

    public static class Mappings extends SymmetricAlgorithmProvider {
        private static final String PREFIX = DSTU7624.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams128");
            configurableProvider.addAlgorithm("AlgorithmParameters.DSTU7624", stringBuilder.toString());
            ASN1ObjectIdentifier aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_128;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGen128");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.DSTU7624", stringBuilder.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$AlgParamGen128");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$AlgParamGen256");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$AlgParamGen512");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB_128");
            configurableProvider.addAlgorithm("Cipher.DSTU7624", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB_128");
            configurableProvider.addAlgorithm("Cipher.DSTU7624-128", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB_256");
            configurableProvider.addAlgorithm("Cipher.DSTU7624-256", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB_512");
            configurableProvider.addAlgorithm("Cipher.DSTU7624-512", stringBuilder.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ecb_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$ECB128");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ecb_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$ECB256");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ecb_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$ECB512");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC128");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC256");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC512");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ofb_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$OFB128");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ofb_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$OFB256");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ofb_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$OFB512");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cfb_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CFB128");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cfb_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CFB256");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cfb_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CFB512");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ctr_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CTR128");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ctr_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CTR256");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ctr_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CTR512");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ccm_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CCM128");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ccm_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CCM256");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ccm_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CCM512");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap");
            configurableProvider.addAlgorithm("Cipher.DSTU7624KW", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.DSTU7624WRAP", "DSTU7624KW");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap128");
            configurableProvider.addAlgorithm("Cipher.DSTU7624-128KW", stringBuilder.toString());
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Cipher.");
            stringBuilder3.append(UAObjectIdentifiers.dstu7624kw_128.getId());
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DSTU7624-128KW");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.DSTU7624-128WRAP", "DSTU7624-128KW");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap256");
            configurableProvider.addAlgorithm("Cipher.DSTU7624-256KW", stringBuilder.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Cipher.");
            stringBuilder3.append(UAObjectIdentifiers.dstu7624kw_256.getId());
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DSTU7624-256KW");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.DSTU7624-256WRAP", "DSTU7624-256KW");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap512");
            configurableProvider.addAlgorithm("Cipher.DSTU7624-512KW", stringBuilder.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Cipher.");
            stringBuilder3.append(UAObjectIdentifiers.dstu7624kw_512.getId());
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DSTU7624-512KW");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.DSTU7624-512WRAP", "DSTU7624-512KW");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GMAC");
            configurableProvider.addAlgorithm("Mac.DSTU7624GMAC", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GMAC128");
            configurableProvider.addAlgorithm("Mac.DSTU7624-128GMAC", stringBuilder.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Mac.");
            stringBuilder3.append(UAObjectIdentifiers.dstu7624gmac_128.getId());
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DSTU7624-128GMAC");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GMAC256");
            configurableProvider.addAlgorithm("Mac.DSTU7624-256GMAC", stringBuilder.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Mac.");
            stringBuilder3.append(UAObjectIdentifiers.dstu7624gmac_256.getId());
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DSTU7624-256GMAC");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$GMAC512");
            configurableProvider.addAlgorithm("Mac.DSTU7624-512GMAC", stringBuilder.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.Mac.");
            stringBuilder3.append(UAObjectIdentifiers.dstu7624gmac_512.getId());
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DSTU7624-512GMAC");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.DSTU7624", stringBuilder.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624kw_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624kw_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624kw_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ecb_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ecb_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ecb_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cbc_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ofb_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ofb_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ofb_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cfb_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cfb_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624cfb_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ctr_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ctr_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ctr_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ccm_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ccm_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624ccm_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624gmac_128;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen128");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624gmac_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen256");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7624gmac_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGen512");
            configurableProvider.addAlgorithm("KeyGenerator", aSN1ObjectIdentifier, stringBuilder2.toString());
        }
    }

    public static class Wrap128 extends BaseWrapCipher {
        public Wrap128() {
            super(new DSTU7624WrapEngine(128));
        }
    }

    public static class Wrap256 extends BaseWrapCipher {
        public Wrap256() {
            super(new DSTU7624WrapEngine(256));
        }
    }

    public static class Wrap512 extends BaseWrapCipher {
        public Wrap512() {
            super(new DSTU7624WrapEngine(512));
        }
    }

    public static class Wrap extends BaseWrapCipher {
        public Wrap() {
            super(new DSTU7624WrapEngine(128));
        }
    }

    public static class CBC128 extends BaseBlockCipher {
        public CBC128() {
            super(new CBCBlockCipher(new DSTU7624Engine(128)), 128);
        }
    }

    public static class CBC256 extends BaseBlockCipher {
        public CBC256() {
            super(new CBCBlockCipher(new DSTU7624Engine(256)), 256);
        }
    }

    public static class CBC512 extends BaseBlockCipher {
        public CBC512() {
            super(new CBCBlockCipher(new DSTU7624Engine(512)), 512);
        }
    }

    public static class CCM128 extends BaseBlockCipher {
        public CCM128() {
            super(new KCCMBlockCipher(new DSTU7624Engine(128)));
        }
    }

    public static class CCM256 extends BaseBlockCipher {
        public CCM256() {
            super(new KCCMBlockCipher(new DSTU7624Engine(256)));
        }
    }

    public static class CCM512 extends BaseBlockCipher {
        public CCM512() {
            super(new KCCMBlockCipher(new DSTU7624Engine(512)));
        }
    }

    public static class CFB128 extends BaseBlockCipher {
        public CFB128() {
            super(new BufferedBlockCipher(new CFBBlockCipher(new DSTU7624Engine(128), 128)), 128);
        }
    }

    public static class CFB256 extends BaseBlockCipher {
        public CFB256() {
            super(new BufferedBlockCipher(new CFBBlockCipher(new DSTU7624Engine(256), 256)), 256);
        }
    }

    public static class CFB512 extends BaseBlockCipher {
        public CFB512() {
            super(new BufferedBlockCipher(new CFBBlockCipher(new DSTU7624Engine(512), 512)), 512);
        }
    }

    public static class CTR128 extends BaseBlockCipher {
        public CTR128() {
            super(new BufferedBlockCipher(new KCTRBlockCipher(new DSTU7624Engine(128))), 128);
        }
    }

    public static class CTR256 extends BaseBlockCipher {
        public CTR256() {
            super(new BufferedBlockCipher(new KCTRBlockCipher(new DSTU7624Engine(256))), 256);
        }
    }

    public static class CTR512 extends BaseBlockCipher {
        public CTR512() {
            super(new BufferedBlockCipher(new KCTRBlockCipher(new DSTU7624Engine(512))), 512);
        }
    }

    public static class ECB128 extends BaseBlockCipher {
        public ECB128() {
            super(new DSTU7624Engine(128));
        }
    }

    public static class ECB256 extends BaseBlockCipher {
        public ECB256() {
            super(new DSTU7624Engine(256));
        }
    }

    public static class ECB512 extends BaseBlockCipher {
        public ECB512() {
            super(new DSTU7624Engine(512));
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new BlockCipherProvider() {
                public BlockCipher get() {
                    return new DSTU7624Engine(128);
                }
            });
        }
    }

    public static class ECB_128 extends BaseBlockCipher {
        public ECB_128() {
            super(new DSTU7624Engine(128));
        }
    }

    public static class ECB_256 extends BaseBlockCipher {
        public ECB_256() {
            super(new DSTU7624Engine(256));
        }
    }

    public static class ECB_512 extends BaseBlockCipher {
        public ECB_512() {
            super(new DSTU7624Engine(512));
        }
    }

    public static class GCM128 extends BaseBlockCipher {
        public GCM128() {
            super(new KGCMBlockCipher(new DSTU7624Engine(128)));
        }
    }

    public static class GCM256 extends BaseBlockCipher {
        public GCM256() {
            super(new KGCMBlockCipher(new DSTU7624Engine(256)));
        }
    }

    public static class GCM512 extends BaseBlockCipher {
        public GCM512() {
            super(new KGCMBlockCipher(new DSTU7624Engine(512)));
        }
    }

    public static class OFB128 extends BaseBlockCipher {
        public OFB128() {
            super(new BufferedBlockCipher(new OFBBlockCipher(new DSTU7624Engine(128), 128)), 128);
        }
    }

    public static class OFB256 extends BaseBlockCipher {
        public OFB256() {
            super(new BufferedBlockCipher(new OFBBlockCipher(new DSTU7624Engine(256), 256)), 256);
        }
    }

    public static class OFB512 extends BaseBlockCipher {
        public OFB512() {
            super(new BufferedBlockCipher(new OFBBlockCipher(new DSTU7624Engine(512), 512)), 512);
        }
    }

    private DSTU7624() {
    }
}
