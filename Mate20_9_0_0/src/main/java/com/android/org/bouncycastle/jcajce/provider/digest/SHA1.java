package com.android.org.bouncycastle.jcajce.provider.digest;

import com.android.org.bouncycastle.asn1.iana.IANAObjectIdentifiers;
import com.android.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.crypto.CipherKeyGenerator;
import com.android.org.bouncycastle.crypto.digests.SHA1Digest;
import com.android.org.bouncycastle.crypto.macs.HMac;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;

public class SHA1 {

    public static class Digest extends BCMessageDigest implements Cloneable {
        public Digest() {
            super(new SHA1Digest());
        }

        public Object clone() throws CloneNotSupportedException {
            Digest d = (Digest) super.clone();
            d.digest = new SHA1Digest((SHA1Digest) this.digest);
            return d;
        }
    }

    public static class KeyGenerator extends BaseKeyGenerator {
        public KeyGenerator() {
            super("HMACSHA1", 160, new CipherKeyGenerator());
        }
    }

    public static class HashMac extends BaseMac {
        public HashMac() {
            super(new HMac(new SHA1Digest()));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = SHA1.class.getName();

        public void configure(ConfigurableProvider provider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest");
            provider.addAlgorithm("MessageDigest.SHA-1", stringBuilder.toString());
            provider.addAlgorithm("Alg.Alias.MessageDigest.SHA1", "SHA-1");
            provider.addAlgorithm("Alg.Alias.MessageDigest.SHA", "SHA-1");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(OIWObjectIdentifiers.idSHA1);
            provider.addAlgorithm(stringBuilder2.toString(), "SHA-1");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            String stringBuilder3 = stringBuilder.toString();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PREFIX);
            stringBuilder4.append("$KeyGenerator");
            addHMACAlgorithm(provider, "SHA1", stringBuilder3, stringBuilder4.toString());
            addHMACAlias(provider, "SHA1", PKCSObjectIdentifiers.id_hmacWithSHA1);
            addHMACAlias(provider, "SHA1", IANAObjectIdentifiers.hmacSHA1);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$SHA1Mac");
            provider.addAlgorithm("Mac.PBEWITHHMACSHA", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$SHA1Mac");
            provider.addAlgorithm("Mac.PBEWITHHMACSHA1", stringBuilder.toString());
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHHMACSHA", "PBEWITHHMACSHA1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.SecretKeyFactory.");
            stringBuilder2.append(OIWObjectIdentifiers.idSHA1);
            provider.addAlgorithm(stringBuilder2.toString(), "PBEWITHHMACSHA1");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.Mac.");
            stringBuilder2.append(OIWObjectIdentifiers.idSHA1);
            provider.addAlgorithm(stringBuilder2.toString(), "PBEWITHHMACSHA");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithMacKeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHHMACSHA1", stringBuilder.toString());
        }
    }

    public static class SHA1Mac extends BaseMac {
        public SHA1Mac() {
            super(new HMac(new SHA1Digest()));
        }
    }

    public static class PBEWithMacKeyFactory extends PBESecretKeyFactory {
        public PBEWithMacKeyFactory() {
            super("PBEwithHmacSHA", null, false, 2, 1, 160, 0);
        }
    }

    private SHA1() {
    }
}
