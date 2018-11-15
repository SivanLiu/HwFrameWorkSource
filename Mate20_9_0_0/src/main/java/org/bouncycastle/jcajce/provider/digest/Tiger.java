package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.iana.IANAObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.TigerDigest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;

public class Tiger {

    public static class Digest extends BCMessageDigest implements Cloneable {
        public Digest() {
            super(new TigerDigest());
        }

        public Object clone() throws CloneNotSupportedException {
            Digest digest = (Digest) super.clone();
            digest.digest = new TigerDigest((TigerDigest) this.digest);
            return digest;
        }
    }

    public static class KeyGenerator extends BaseKeyGenerator {
        public KeyGenerator() {
            super("HMACTIGER", 192, new CipherKeyGenerator());
        }
    }

    public static class HashMac extends BaseMac {
        public HashMac() {
            super(new HMac(new TigerDigest()));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = Tiger.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest");
            configurableProvider.addAlgorithm("MessageDigest.TIGER", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest");
            configurableProvider.addAlgorithm("MessageDigest.Tiger", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PREFIX);
            stringBuilder3.append("$KeyGenerator");
            addHMACAlgorithm(configurableProvider, "TIGER", stringBuilder2, stringBuilder3.toString());
            addHMACAlias(configurableProvider, "TIGER", IANAObjectIdentifiers.hmacTIGER);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithMacKeyFactory");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWITHHMACTIGER", stringBuilder.toString());
        }
    }

    public static class PBEWithHashMac extends BaseMac {
        public PBEWithHashMac() {
            super(new HMac(new TigerDigest()), 2, 3, 192);
        }
    }

    public static class TigerHmac extends BaseMac {
        public TigerHmac() {
            super(new HMac(new TigerDigest()));
        }
    }

    public static class PBEWithMacKeyFactory extends PBESecretKeyFactory {
        public PBEWithMacKeyFactory() {
            super("PBEwithHmacTiger", null, false, 2, 3, 192, 0);
        }
    }

    private Tiger() {
    }
}
