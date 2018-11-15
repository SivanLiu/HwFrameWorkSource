package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.digests.MD2Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;

public class MD2 {

    public static class Digest extends BCMessageDigest implements Cloneable {
        public Digest() {
            super(new MD2Digest());
        }

        public Object clone() throws CloneNotSupportedException {
            Digest digest = (Digest) super.clone();
            digest.digest = new MD2Digest((MD2Digest) this.digest);
            return digest;
        }
    }

    public static class KeyGenerator extends BaseKeyGenerator {
        public KeyGenerator() {
            super("HMACMD2", 128, new CipherKeyGenerator());
        }
    }

    public static class HashMac extends BaseMac {
        public HashMac() {
            super(new HMac(new MD2Digest()));
        }
    }

    public static class Mappings extends DigestAlgorithmProvider {
        private static final String PREFIX = MD2.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Digest");
            configurableProvider.addAlgorithm("MessageDigest.MD2", stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.MessageDigest.");
            stringBuilder2.append(PKCSObjectIdentifiers.md2);
            configurableProvider.addAlgorithm(stringBuilder2.toString(), "MD2");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$HashMac");
            String stringBuilder3 = stringBuilder.toString();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(PREFIX);
            stringBuilder4.append("$KeyGenerator");
            addHMACAlgorithm(configurableProvider, "MD2", stringBuilder3, stringBuilder4.toString());
        }
    }

    private MD2() {
    }
}
