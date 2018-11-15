package org.bouncycastle.jcajce.provider.symmetric;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

public final class SipHash {

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            super("SipHash", 128, new CipherKeyGenerator());
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PREFIX = SipHash.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Mac24");
            configurableProvider.addAlgorithm("Mac.SIPHASH-2-4", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.SIPHASH", "SIPHASH-2-4");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Mac48");
            configurableProvider.addAlgorithm("Mac.SIPHASH-4-8", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.SIPHASH", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.KeyGenerator.SIPHASH-2-4", "SIPHASH");
            configurableProvider.addAlgorithm("Alg.Alias.KeyGenerator.SIPHASH-4-8", "SIPHASH");
        }
    }

    public static class Mac24 extends BaseMac {
        public Mac24() {
            super(new org.bouncycastle.crypto.macs.SipHash());
        }
    }

    public static class Mac48 extends BaseMac {
        public Mac48() {
            super(new org.bouncycastle.crypto.macs.SipHash(4, 8));
        }
    }

    private SipHash() {
    }
}
