package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;

public class Keccak {

    public static class DigestKeccak extends BCMessageDigest implements Cloneable {
        public DigestKeccak(int i) {
            super(new KeccakDigest(i));
        }

        public Object clone() throws CloneNotSupportedException {
            BCMessageDigest bCMessageDigest = (BCMessageDigest) super.clone();
            bCMessageDigest.digest = new KeccakDigest((KeccakDigest) this.digest);
            return bCMessageDigest;
        }
    }

    public static class KeyGenerator224 extends BaseKeyGenerator {
        public KeyGenerator224() {
            super("HMACKECCAK224", 224, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator256 extends BaseKeyGenerator {
        public KeyGenerator256() {
            super("HMACKECCAK256", 256, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator288 extends BaseKeyGenerator {
        public KeyGenerator288() {
            super("HMACKECCAK288", 288, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator384 extends BaseKeyGenerator {
        public KeyGenerator384() {
            super("HMACKECCAK384", 384, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator512 extends BaseKeyGenerator {
        public KeyGenerator512() {
            super("HMACKECCAK512", 512, new CipherKeyGenerator());
        }
    }

    public static class Digest224 extends DigestKeccak {
        public Digest224() {
            super(224);
        }
    }

    public static class Digest256 extends DigestKeccak {
        public Digest256() {
            super(256);
        }
    }

    public static class Digest288 extends DigestKeccak {
        public Digest288() {
            super(288);
        }
    }

    public static class Digest384 extends DigestKeccak {
        public Digest384() {
            super(384);
        }
    }

    public static class Digest512 extends DigestKeccak {
        public Digest512() {
            super(512);
        }
    }

    public static class HashMac224 extends BaseMac {
        public HashMac224() {
            super(new HMac(new KeccakDigest(224)));
        }
    }

    public static class HashMac256 extends BaseMac {
        public HashMac256() {
            super(new HMac(new KeccakDigest(256)));
        }
    }

    public static class HashMac288 extends BaseMac {
        public HashMac288() {
            super(new HMac(new KeccakDigest(288)));
        }
    }

    public static class HashMac384 extends BaseMac {
        public HashMac384() {
            super(new HMac(new KeccakDigest(384)));
        }
    }

    public static class HashMac512 extends BaseMac {
        public HashMac512() {
            super(new HMac(new KeccakDigest(512)));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = Keccak.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest224");
            configurableProvider.addAlgorithm("MessageDigest.KECCAK-224", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest288");
            configurableProvider.addAlgorithm("MessageDigest.KECCAK-288", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest256");
            configurableProvider.addAlgorithm("MessageDigest.KECCAK-256", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest384");
            configurableProvider.addAlgorithm("MessageDigest.KECCAK-384", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest512");
            configurableProvider.addAlgorithm("MessageDigest.KECCAK-512", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac224");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGenerator224");
            addHMACAlgorithm(configurableProvider, "KECCAK224", stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac256");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGenerator256");
            addHMACAlgorithm(configurableProvider, "KECCAK256", stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac288");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGenerator288");
            addHMACAlgorithm(configurableProvider, "KECCAK288", stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac384");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGenerator384");
            addHMACAlgorithm(configurableProvider, "KECCAK384", stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac512");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGenerator512");
            addHMACAlgorithm(configurableProvider, "KECCAK512", stringBuilder2, stringBuilder3.toString());
        }
    }

    private Keccak() {
    }
}
