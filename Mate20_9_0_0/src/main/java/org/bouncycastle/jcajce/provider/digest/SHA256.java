package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;

public class SHA256 {

    public static class Digest extends BCMessageDigest implements Cloneable {
        public Digest() {
            super(new SHA256Digest());
        }

        public Object clone() throws CloneNotSupportedException {
            Digest digest = (Digest) super.clone();
            digest.digest = new SHA256Digest((SHA256Digest) this.digest);
            return digest;
        }
    }

    public static class KeyGenerator extends BaseKeyGenerator {
        public KeyGenerator() {
            super("HMACSHA256", 256, new CipherKeyGenerator());
        }
    }

    public static class HashMac extends BaseMac {
        public HashMac() {
            super(new HMac(new SHA256Digest()));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = SHA256.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest");
            configurableProvider.addAlgorithm("MessageDigest.SHA-256", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.MessageDigest.SHA256", McElieceCCA2KeyGenParameterSpec.SHA256);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(NISTObjectIdentifiers.id_sha256);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), McElieceCCA2KeyGenParameterSpec.SHA256);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithMacKeyFactory");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWITHHMACSHA256", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHHMACSHA-256", "PBEWITHHMACSHA256");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.SecretKeyFactory.");
            stringBuilder2.append(NISTObjectIdentifiers.id_sha256);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBEWITHHMACSHA256");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            configurableProvider.addAlgorithm("Mac.PBEWITHHMACSHA256", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            String stringBuilder3 = stringBuilder.toString();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PREFIX);
            stringBuilder4.append("$KeyGenerator");
            addHMACAlgorithm(configurableProvider, "SHA256", stringBuilder3, stringBuilder4.toString());
            addHMACAlias(configurableProvider, "SHA256", PKCSObjectIdentifiers.id_hmacWithSHA256);
            addHMACAlias(configurableProvider, "SHA256", NISTObjectIdentifiers.id_sha256);
        }
    }

    public static class PBEWithMacKeyFactory extends PBESecretKeyFactory {
        public PBEWithMacKeyFactory() {
            super("PBEwithHmacSHA256", null, false, 2, 4, 256, 0);
        }
    }

    private SHA256() {
    }
}
