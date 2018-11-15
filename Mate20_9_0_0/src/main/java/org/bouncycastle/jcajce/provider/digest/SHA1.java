package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.iana.IANAObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;

public class SHA1 {

    public static class Digest extends BCMessageDigest implements Cloneable {
        public Digest() {
            super(new SHA1Digest());
        }

        public Object clone() throws CloneNotSupportedException {
            Digest digest = (Digest) super.clone();
            digest.digest = new SHA1Digest((SHA1Digest) this.digest);
            return digest;
        }
    }

    public static class KeyGenerator extends BaseKeyGenerator {
        public KeyGenerator() {
            super("HMACSHA1", CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256, new CipherKeyGenerator());
        }
    }

    public static class HashMac extends BaseMac {
        public HashMac() {
            super(new HMac(new SHA1Digest()));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = SHA1.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest");
            configurableProvider.addAlgorithm("MessageDigest.SHA-1", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.MessageDigest.SHA1", McElieceCCA2KeyGenParameterSpec.SHA1);
            configurableProvider.addAlgorithm("Alg.Alias.MessageDigest.SHA", McElieceCCA2KeyGenParameterSpec.SHA1);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(OIWObjectIdentifiers.idSHA1);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), McElieceCCA2KeyGenParameterSpec.SHA1);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            String stringBuilder3 = stringBuilder.toString();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PREFIX);
            stringBuilder4.append("$KeyGenerator");
            addHMACAlgorithm(configurableProvider, "SHA1", stringBuilder3, stringBuilder4.toString());
            addHMACAlias(configurableProvider, "SHA1", PKCSObjectIdentifiers.id_hmacWithSHA1);
            addHMACAlias(configurableProvider, "SHA1", IANAObjectIdentifiers.hmacSHA1);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$SHA1Mac");
            configurableProvider.addAlgorithm("Mac.PBEWITHHMACSHA", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$SHA1Mac");
            configurableProvider.addAlgorithm("Mac.PBEWITHHMACSHA1", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHHMACSHA", "PBEWITHHMACSHA1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.SecretKeyFactory.");
            stringBuilder2.append(OIWObjectIdentifiers.idSHA1);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBEWITHHMACSHA1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.Mac.");
            stringBuilder2.append(OIWObjectIdentifiers.idSHA1);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "PBEWITHHMACSHA");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithMacKeyFactory");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWITHHMACSHA1", stringBuilder.toString());
        }
    }

    public static class SHA1Mac extends BaseMac {
        public SHA1Mac() {
            super(new HMac(new SHA1Digest()));
        }
    }

    public static class PBEWithMacKeyFactory extends PBESecretKeyFactory {
        public PBEWithMacKeyFactory() {
            super("PBEwithHmacSHA", null, false, 2, 1, CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256, 0);
        }
    }

    private SHA1() {
    }
}
