package org.bouncycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.RC532Engine;
import org.bouncycastle.crypto.engines.RC564Engine;
import org.bouncycastle.crypto.macs.CBCBlockCipherMac;
import org.bouncycastle.crypto.macs.CFBBlockCipherMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

public final class RC5 {

    public static class AlgParamGen extends BaseAlgorithmParameterGenerator {
        protected AlgorithmParameters engineGenerateParameters() {
            byte[] bArr = new byte[8];
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            this.random.nextBytes(bArr);
            try {
                AlgorithmParameters createParametersInstance = createParametersInstance("RC5");
                createParametersInstance.init(new IvParameterSpec(bArr));
                return createParametersInstance;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for RC5 parameter generation.");
        }
    }

    public static class KeyGen32 extends BaseKeyGenerator {
        public KeyGen32() {
            super("RC5", 128, new CipherKeyGenerator());
        }
    }

    public static class KeyGen64 extends BaseKeyGenerator {
        public KeyGen64() {
            super("RC5-64", 256, new CipherKeyGenerator());
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PREFIX = RC5.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB32");
            configurableProvider.addAlgorithm("Cipher.RC5", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RC5-32", "RC5");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB64");
            configurableProvider.addAlgorithm("Cipher.RC5-64", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen32");
            configurableProvider.addAlgorithm("KeyGenerator.RC5", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.KeyGenerator.RC5-32", "RC5");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen64");
            configurableProvider.addAlgorithm("KeyGenerator.RC5-64", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.RC5", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.RC5-64", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Mac32");
            configurableProvider.addAlgorithm("Mac.RC5MAC", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.RC5", "RC5MAC");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$CFB8Mac32");
            configurableProvider.addAlgorithm("Mac.RC5MAC/CFB8", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.RC5/CFB8", "RC5MAC/CFB8");
        }
    }

    public static class AlgParams extends IvAlgorithmParameters {
        protected String engineToString() {
            return "RC5 IV";
        }
    }

    public static class CFB8Mac32 extends BaseMac {
        public CFB8Mac32() {
            super(new CFBBlockCipherMac(new RC532Engine()));
        }
    }

    public static class Mac32 extends BaseMac {
        public Mac32() {
            super(new CBCBlockCipherMac(new RC532Engine()));
        }
    }

    public static class CBC32 extends BaseBlockCipher {
        public CBC32() {
            super(new CBCBlockCipher(new RC532Engine()), 64);
        }
    }

    public static class ECB32 extends BaseBlockCipher {
        public ECB32() {
            super(new RC532Engine());
        }
    }

    public static class ECB64 extends BaseBlockCipher {
        public ECB64() {
            super(new RC564Engine());
        }
    }

    private RC5() {
    }
}
