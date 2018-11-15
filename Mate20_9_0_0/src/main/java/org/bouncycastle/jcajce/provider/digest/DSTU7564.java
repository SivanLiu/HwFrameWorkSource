package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ua.UAObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.DSTU7564Digest;
import org.bouncycastle.crypto.macs.DSTU7564Mac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;

public class DSTU7564 {

    public static class DigestDSTU7564 extends BCMessageDigest implements Cloneable {
        public DigestDSTU7564(int i) {
            super(new DSTU7564Digest(i));
        }

        public Object clone() throws CloneNotSupportedException {
            BCMessageDigest bCMessageDigest = (BCMessageDigest) super.clone();
            bCMessageDigest.digest = new DSTU7564Digest((DSTU7564Digest) this.digest);
            return bCMessageDigest;
        }
    }

    public static class KeyGenerator256 extends BaseKeyGenerator {
        public KeyGenerator256() {
            super("HMACDSTU7564-256", 256, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator384 extends BaseKeyGenerator {
        public KeyGenerator384() {
            super("HMACDSTU7564-384", 384, new CipherKeyGenerator());
        }
    }

    public static class KeyGenerator512 extends BaseKeyGenerator {
        public KeyGenerator512() {
            super("HMACDSTU7564-512", 512, new CipherKeyGenerator());
        }
    }

    public static class Digest256 extends DigestDSTU7564 {
        public Digest256() {
            super(256);
        }
    }

    public static class Digest384 extends DigestDSTU7564 {
        public Digest384() {
            super(384);
        }
    }

    public static class Digest512 extends DigestDSTU7564 {
        public Digest512() {
            super(512);
        }
    }

    public static class HashMac256 extends BaseMac {
        public HashMac256() {
            super(new DSTU7564Mac(256));
        }
    }

    public static class HashMac384 extends BaseMac {
        public HashMac384() {
            super(new DSTU7564Mac(384));
        }
    }

    public static class HashMac512 extends BaseMac {
        public HashMac512() {
            super(new DSTU7564Mac(512));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = DSTU7564.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest256");
            configurableProvider.addAlgorithm("MessageDigest.DSTU7564-256", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest384");
            configurableProvider.addAlgorithm("MessageDigest.DSTU7564-384", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest512");
            configurableProvider.addAlgorithm("MessageDigest.DSTU7564-512", stringBuilder.toString());
            ASN1ObjectIdentifier aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7564digest_256;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Digest256");
            configurableProvider.addAlgorithm("MessageDigest", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7564digest_384;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Digest384");
            configurableProvider.addAlgorithm("MessageDigest", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = UAObjectIdentifiers.dstu7564digest_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Digest512");
            configurableProvider.addAlgorithm("MessageDigest", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac256");
            String stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGenerator256");
            addHMACAlgorithm(configurableProvider, "DSTU7564-256", stringBuilder3, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac384");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGenerator384");
            addHMACAlgorithm(configurableProvider, "DSTU7564-384", stringBuilder3, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac512");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGenerator512");
            addHMACAlgorithm(configurableProvider, "DSTU7564-512", stringBuilder3, stringBuilder2.toString());
            addHMACAlias(configurableProvider, "DSTU7564-256", UAObjectIdentifiers.dstu7564mac_256);
            addHMACAlias(configurableProvider, "DSTU7564-384", UAObjectIdentifiers.dstu7564mac_384);
            addHMACAlias(configurableProvider, "DSTU7564-512", UAObjectIdentifiers.dstu7564mac_512);
        }
    }

    private DSTU7564() {
    }
}
