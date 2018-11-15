package org.bouncycastle.jcajce.provider.symmetric;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.VMPCEngine;
import org.bouncycastle.crypto.macs.VMPCMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseStreamCipher;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

public final class VMPC {

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            super("VMPC", 128, new CipherKeyGenerator());
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PREFIX = VMPC.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Base");
            configurableProvider.addAlgorithm("Cipher.VMPC", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.VMPC", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Mac");
            configurableProvider.addAlgorithm("Mac.VMPCMAC", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.VMPC", "VMPCMAC");
            configurableProvider.addAlgorithm("Alg.Alias.Mac.VMPC-MAC", "VMPCMAC");
        }
    }

    public static class Mac extends BaseMac {
        public Mac() {
            super(new VMPCMac());
        }
    }

    public static class Base extends BaseStreamCipher {
        public Base() {
            super(new VMPCEngine(), 16);
        }
    }

    private VMPC() {
    }
}
