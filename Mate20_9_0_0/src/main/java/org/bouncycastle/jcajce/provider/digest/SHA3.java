package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.pqc.jcajce.spec.SPHINCS256KeyGenParameterSpec;

public class SHA3 {

    public static class DigestSHA3 extends BCMessageDigest implements Cloneable {
        public DigestSHA3(int i) {
            super(new SHA3Digest(i));
        }

        public Object clone() throws CloneNotSupportedException {
            BCMessageDigest bCMessageDigest = (BCMessageDigest) super.clone();
            bCMessageDigest.digest = new SHA3Digest((SHA3Digest) this.digest);
            return bCMessageDigest;
        }
    }

    public static class KeyGeneratorSHA3 extends BaseKeyGenerator {
        public KeyGeneratorSHA3(int i) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HMACSHA3-");
            stringBuilder.append(i);
            super(stringBuilder.toString(), i, new CipherKeyGenerator());
        }
    }

    public static class Digest224 extends DigestSHA3 {
        public Digest224() {
            super(224);
        }
    }

    public static class Digest256 extends DigestSHA3 {
        public Digest256() {
            super(256);
        }
    }

    public static class Digest384 extends DigestSHA3 {
        public Digest384() {
            super(384);
        }
    }

    public static class Digest512 extends DigestSHA3 {
        public Digest512() {
            super(512);
        }
    }

    public static class HashMacSHA3 extends BaseMac {
        public HashMacSHA3(int i) {
            super(new HMac(new SHA3Digest(i)));
        }
    }

    public static class KeyGenerator224 extends KeyGeneratorSHA3 {
        public KeyGenerator224() {
            super(224);
        }
    }

    public static class KeyGenerator256 extends KeyGeneratorSHA3 {
        public KeyGenerator256() {
            super(256);
        }
    }

    public static class KeyGenerator384 extends KeyGeneratorSHA3 {
        public KeyGenerator384() {
            super(384);
        }
    }

    public static class KeyGenerator512 extends KeyGeneratorSHA3 {
        public KeyGenerator512() {
            super(512);
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = SHA3.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest224");
            configurableProvider.addAlgorithm("MessageDigest.SHA3-224", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest256");
            configurableProvider.addAlgorithm("MessageDigest.SHA3-256", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest384");
            configurableProvider.addAlgorithm("MessageDigest.SHA3-384", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest512");
            configurableProvider.addAlgorithm("MessageDigest.SHA3-512", stringBuilder.toString());
            ASN1ObjectIdentifier aSN1ObjectIdentifier = NISTObjectIdentifiers.id_sha3_224;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Digest224");
            configurableProvider.addAlgorithm("MessageDigest", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NISTObjectIdentifiers.id_sha3_256;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Digest256");
            configurableProvider.addAlgorithm("MessageDigest", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NISTObjectIdentifiers.id_sha3_384;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Digest384");
            configurableProvider.addAlgorithm("MessageDigest", aSN1ObjectIdentifier, stringBuilder2.toString());
            aSN1ObjectIdentifier = NISTObjectIdentifiers.id_sha3_512;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Digest512");
            configurableProvider.addAlgorithm("MessageDigest", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac224");
            String stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGenerator224");
            addHMACAlgorithm(configurableProvider, "SHA3-224", stringBuilder3, stringBuilder2.toString());
            addHMACAlias(configurableProvider, "SHA3-224", NISTObjectIdentifiers.id_hmacWithSHA3_224);
            String str = SPHINCS256KeyGenParameterSpec.SHA3_256;
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac256");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGenerator256");
            addHMACAlgorithm(configurableProvider, str, stringBuilder3, stringBuilder2.toString());
            addHMACAlias(configurableProvider, SPHINCS256KeyGenParameterSpec.SHA3_256, NISTObjectIdentifiers.id_hmacWithSHA3_256);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac384");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGenerator384");
            addHMACAlgorithm(configurableProvider, "SHA3-384", stringBuilder3, stringBuilder2.toString());
            addHMACAlias(configurableProvider, "SHA3-384", NISTObjectIdentifiers.id_hmacWithSHA3_384);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac512");
            stringBuilder3 = stringBuilder.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyGenerator512");
            addHMACAlgorithm(configurableProvider, "SHA3-512", stringBuilder3, stringBuilder2.toString());
            addHMACAlias(configurableProvider, "SHA3-512", NISTObjectIdentifiers.id_hmacWithSHA3_512);
        }
    }

    public static class HashMac224 extends HashMacSHA3 {
        public HashMac224() {
            super(224);
        }
    }

    public static class HashMac256 extends HashMacSHA3 {
        public HashMac256() {
            super(256);
        }
    }

    public static class HashMac384 extends HashMacSHA3 {
        public HashMac384() {
            super(384);
        }
    }

    public static class HashMac512 extends HashMacSHA3 {
        public HashMac512() {
            super(512);
        }
    }

    private SHA3() {
    }
}
