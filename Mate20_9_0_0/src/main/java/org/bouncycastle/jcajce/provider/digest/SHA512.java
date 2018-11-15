package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.SHA512tDigest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.macs.OldHMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;

public class SHA512 {

    public static class Digest extends BCMessageDigest implements Cloneable {
        public Digest() {
            super(new SHA512Digest());
        }

        public Object clone() throws CloneNotSupportedException {
            Digest digest = (Digest) super.clone();
            digest.digest = new SHA512Digest((SHA512Digest) this.digest);
            return digest;
        }
    }

    public static class DigestT extends BCMessageDigest implements Cloneable {
        public DigestT(int i) {
            super(new SHA512tDigest(i));
        }

        public Object clone() throws CloneNotSupportedException {
            DigestT digestT = (DigestT) super.clone();
            digestT.digest = new SHA512tDigest((SHA512tDigest) this.digest);
            return digestT;
        }
    }

    public static class KeyGenerator extends BaseKeyGenerator {
        public KeyGenerator() {
            super("HMACSHA512", 512, new CipherKeyGenerator());
        }
    }

    public static class KeyGeneratorT224 extends BaseKeyGenerator {
        public KeyGeneratorT224() {
            super("HMACSHA512/224", 224, new CipherKeyGenerator());
        }
    }

    public static class KeyGeneratorT256 extends BaseKeyGenerator {
        public KeyGeneratorT256() {
            super("HMACSHA512/256", 256, new CipherKeyGenerator());
        }
    }

    public static class DigestT224 extends DigestT {
        public DigestT224() {
            super(224);
        }
    }

    public static class DigestT256 extends DigestT {
        public DigestT256() {
            super(256);
        }
    }

    public static class HashMac extends BaseMac {
        public HashMac() {
            super(new HMac(new SHA512Digest()));
        }
    }

    public static class HashMacT224 extends BaseMac {
        public HashMacT224() {
            super(new HMac(new SHA512tDigest(224)));
        }
    }

    public static class HashMacT256 extends BaseMac {
        public HashMacT256() {
            super(new HMac(new SHA512tDigest(256)));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = SHA512.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest");
            configurableProvider.addAlgorithm("MessageDigest.SHA-512", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.MessageDigest.SHA512", McElieceCCA2KeyGenParameterSpec.SHA512);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(NISTObjectIdentifiers.id_sha512);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), McElieceCCA2KeyGenParameterSpec.SHA512);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$DigestT224");
            configurableProvider.addAlgorithm("MessageDigest.SHA-512/224", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.MessageDigest.SHA512/224", "SHA-512/224");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(NISTObjectIdentifiers.id_sha512_224);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "SHA-512/224");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$DigestT256");
            configurableProvider.addAlgorithm("MessageDigest.SHA-512/256", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.MessageDigest.SHA512256", "SHA-512/256");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(NISTObjectIdentifiers.id_sha512_256);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "SHA-512/256");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$OldSHA512");
            configurableProvider.addAlgorithm("Mac.OLDHMACSHA512", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            configurableProvider.addAlgorithm("Mac.PBEWITHHMACSHA512", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            String stringBuilder3 = stringBuilder.toString();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PREFIX);
            stringBuilder4.append("$KeyGenerator");
            addHMACAlgorithm(configurableProvider, "SHA512", stringBuilder3, stringBuilder4.toString());
            addHMACAlias(configurableProvider, "SHA512", PKCSObjectIdentifiers.id_hmacWithSHA512);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMacT224");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PREFIX);
            stringBuilder4.append("$KeyGeneratorT224");
            addHMACAlgorithm(configurableProvider, "SHA512/224", stringBuilder3, stringBuilder4.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMacT256");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PREFIX);
            stringBuilder4.append("$KeyGeneratorT256");
            addHMACAlgorithm(configurableProvider, "SHA512/256", stringBuilder3, stringBuilder4.toString());
        }
    }

    public static class OldSHA512 extends BaseMac {
        public OldSHA512() {
            super(new OldHMac(new SHA512Digest()));
        }
    }

    private SHA512() {
    }
}
