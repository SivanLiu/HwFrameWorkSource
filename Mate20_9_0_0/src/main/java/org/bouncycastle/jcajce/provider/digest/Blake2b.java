package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;

public class Blake2b {

    public static class Blake2b160 extends BCMessageDigest implements Cloneable {
        public Blake2b160() {
            super(new Blake2bDigest((int) CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256));
        }

        public Object clone() throws CloneNotSupportedException {
            Blake2b160 blake2b160 = (Blake2b160) super.clone();
            blake2b160.digest = new Blake2bDigest((Blake2bDigest) this.digest);
            return blake2b160;
        }
    }

    public static class Blake2b256 extends BCMessageDigest implements Cloneable {
        public Blake2b256() {
            super(new Blake2bDigest(256));
        }

        public Object clone() throws CloneNotSupportedException {
            Blake2b256 blake2b256 = (Blake2b256) super.clone();
            blake2b256.digest = new Blake2bDigest((Blake2bDigest) this.digest);
            return blake2b256;
        }
    }

    public static class Blake2b384 extends BCMessageDigest implements Cloneable {
        public Blake2b384() {
            super(new Blake2bDigest(384));
        }

        public Object clone() throws CloneNotSupportedException {
            Blake2b384 blake2b384 = (Blake2b384) super.clone();
            blake2b384.digest = new Blake2bDigest((Blake2bDigest) this.digest);
            return blake2b384;
        }
    }

    public static class Blake2b512 extends BCMessageDigest implements Cloneable {
        public Blake2b512() {
            super(new Blake2bDigest(512));
        }

        public Object clone() throws CloneNotSupportedException {
            Blake2b512 blake2b512 = (Blake2b512) super.clone();
            blake2b512.digest = new Blake2bDigest((Blake2bDigest) this.digest);
            return blake2b512;
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = Blake2b.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Blake2b512");
            configurableProvider.addAlgorithm("MessageDigest.BLAKE2B-512", stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(MiscObjectIdentifiers.id_blake2b512);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "BLAKE2B-512");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Blake2b384");
            configurableProvider.addAlgorithm("MessageDigest.BLAKE2B-384", stringBuilder.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(MiscObjectIdentifiers.id_blake2b384);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "BLAKE2B-384");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Blake2b256");
            configurableProvider.addAlgorithm("MessageDigest.BLAKE2B-256", stringBuilder.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(MiscObjectIdentifiers.id_blake2b256);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "BLAKE2B-256");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Blake2b160");
            configurableProvider.addAlgorithm("MessageDigest.BLAKE2B-160", stringBuilder.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(MiscObjectIdentifiers.id_blake2b160);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "BLAKE2B-160");
        }
    }

    private Blake2b() {
    }
}
