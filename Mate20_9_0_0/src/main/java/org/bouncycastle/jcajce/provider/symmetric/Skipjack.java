package org.bouncycastle.jcajce.provider.symmetric;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.SkipjackEngine;
import org.bouncycastle.crypto.macs.CBCBlockCipherMac;
import org.bouncycastle.crypto.macs.CFBBlockCipherMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

public final class Skipjack {

    public static class KeyGen extends BaseKeyGenerator {
        public KeyGen() {
            super("Skipjack", 80, new CipherKeyGenerator());
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PREFIX = Skipjack.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB");
            configurableProvider.addAlgorithm("Cipher.SKIPJACK", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGen");
            configurableProvider.addAlgorithm("KeyGenerator.SKIPJACK", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParams");
            configurableProvider.addAlgorithm("AlgorithmParameters.SKIPJACK", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Mac");
            configurableProvider.addAlgorithm("Mac.SKIPJACKMAC", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.SKIPJACK", "SKIPJACKMAC");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$MacCFB8");
            configurableProvider.addAlgorithm("Mac.SKIPJACKMAC/CFB8", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.SKIPJACK/CFB8", "SKIPJACKMAC/CFB8");
        }
    }

    public static class AlgParams extends IvAlgorithmParameters {
        protected String engineToString() {
            return "Skipjack IV";
        }
    }

    public static class Mac extends BaseMac {
        public Mac() {
            super(new CBCBlockCipherMac(new SkipjackEngine()));
        }
    }

    public static class MacCFB8 extends BaseMac {
        public MacCFB8() {
            super(new CFBBlockCipherMac(new SkipjackEngine()));
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new SkipjackEngine());
        }
    }

    private Skipjack() {
    }
}
