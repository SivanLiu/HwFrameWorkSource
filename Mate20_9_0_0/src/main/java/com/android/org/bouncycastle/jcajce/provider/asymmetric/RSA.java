package com.android.org.bouncycastle.jcajce.provider.asymmetric;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import java.util.HashMap;
import java.util.Map;

public class RSA {
    private static final String PREFIX = "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.";
    private static final Map<String, String> generalRsaAttributes = new HashMap();

    public static class Mappings extends AsymmetricAlgorithmProvider {
        public void configure(ConfigurableProvider provider) {
            provider.addAlgorithm("AlgorithmParameters.OAEP", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$OAEP");
            provider.addAlgorithm("AlgorithmParameters.PSS", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$PSS");
            provider.addAttributes("Cipher.RSA", RSA.generalRsaAttributes);
            provider.addAlgorithm("Cipher.RSA", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$NoPadding");
            provider.addAlgorithm("Alg.Alias.Cipher.RSA/RAW", "RSA");
            provider.addAlgorithm("Alg.Alias.Cipher.RSA//RAW", "RSA");
            provider.addAlgorithm("Alg.Alias.Cipher.RSA//NOPADDING", "RSA");
            provider.addAlgorithm("KeyFactory.RSA", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi");
            provider.addAlgorithm("KeyPairGenerator.RSA", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyPairGeneratorSpi");
            AsymmetricKeyInfoConverter keyFact = new KeyFactorySpi();
            registerOid(provider, PKCSObjectIdentifiers.rsaEncryption, "RSA", keyFact);
            registerOid(provider, X509ObjectIdentifiers.id_ea_rsa, "RSA", keyFact);
            registerOid(provider, PKCSObjectIdentifiers.id_RSAES_OAEP, "RSA", keyFact);
            if (provider.hasAlgorithm("MessageDigest", "MD5")) {
                addDigestSignature(provider, "MD5", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD5", PKCSObjectIdentifiers.md5WithRSAEncryption);
            }
            if (provider.hasAlgorithm("MessageDigest", "SHA1")) {
                addDigestSignature(provider, "SHA1", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA1", PKCSObjectIdentifiers.sha1WithRSAEncryption);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Alg.Alias.Signature.");
                stringBuilder.append(OIWObjectIdentifiers.sha1WithRSA);
                provider.addAlgorithm(stringBuilder.toString(), "SHA1WITHRSA");
                stringBuilder = new StringBuilder();
                stringBuilder.append("Alg.Alias.Signature.OID.");
                stringBuilder.append(OIWObjectIdentifiers.sha1WithRSA);
                provider.addAlgorithm(stringBuilder.toString(), "SHA1WITHRSA");
            }
            addDigestSignature(provider, "SHA224", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA224", PKCSObjectIdentifiers.sha224WithRSAEncryption);
            addDigestSignature(provider, "SHA256", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA256", PKCSObjectIdentifiers.sha256WithRSAEncryption);
            addDigestSignature(provider, "SHA384", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA384", PKCSObjectIdentifiers.sha384WithRSAEncryption);
            addDigestSignature(provider, "SHA512", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512", PKCSObjectIdentifiers.sha512WithRSAEncryption);
        }

        private void addDigestSignature(ConfigurableProvider provider, String digest, String className, ASN1ObjectIdentifier oid) {
            String mainName = new StringBuilder();
            mainName.append(digest);
            mainName.append("WITHRSA");
            mainName = mainName.toString();
            String jdk11Variation1 = new StringBuilder();
            jdk11Variation1.append(digest);
            jdk11Variation1.append("withRSA");
            jdk11Variation1 = jdk11Variation1.toString();
            String jdk11Variation2 = new StringBuilder();
            jdk11Variation2.append(digest);
            jdk11Variation2.append("WithRSA");
            jdk11Variation2 = jdk11Variation2.toString();
            String alias = new StringBuilder();
            alias.append(digest);
            alias.append("/RSA");
            alias = alias.toString();
            String longName = new StringBuilder();
            longName.append(digest);
            longName.append("WITHRSAENCRYPTION");
            longName = longName.toString();
            String longJdk11Variation1 = new StringBuilder();
            longJdk11Variation1.append(digest);
            longJdk11Variation1.append("withRSAEncryption");
            longJdk11Variation1 = longJdk11Variation1.toString();
            String longJdk11Variation2 = new StringBuilder();
            longJdk11Variation2.append(digest);
            longJdk11Variation2.append("WithRSAEncryption");
            longJdk11Variation2 = longJdk11Variation2.toString();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(mainName);
            provider.addAlgorithm(stringBuilder.toString(), className);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(jdk11Variation1);
            provider.addAlgorithm(stringBuilder.toString(), mainName);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(jdk11Variation2);
            provider.addAlgorithm(stringBuilder.toString(), mainName);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(longName);
            provider.addAlgorithm(stringBuilder.toString(), mainName);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(longJdk11Variation1);
            provider.addAlgorithm(stringBuilder.toString(), mainName);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(longJdk11Variation2);
            provider.addAlgorithm(stringBuilder.toString(), mainName);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(alias);
            provider.addAlgorithm(stringBuilder.toString(), mainName);
            if (oid != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Alg.Alias.Signature.");
                stringBuilder.append(oid);
                provider.addAlgorithm(stringBuilder.toString(), mainName);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Alg.Alias.Signature.OID.");
                stringBuilder.append(oid);
                provider.addAlgorithm(stringBuilder.toString(), mainName);
            }
        }

        private void addISO9796Signature(ConfigurableProvider provider, String digest, String className) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("withRSA/ISO9796-2");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSA/ISO9796-2");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("WithRSA/ISO9796-2");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSA/ISO9796-2");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("WITHRSA/ISO9796-2");
            provider.addAlgorithm(stringBuilder.toString(), className);
        }

        private void addPSSSignature(ConfigurableProvider provider, String digest, String className) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("withRSA/PSS");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSAANDMGF1");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("WithRSA/PSS");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSAANDMGF1");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("withRSAandMGF1");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSAANDMGF1");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("WithRSAAndMGF1");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSAANDMGF1");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("WITHRSAANDMGF1");
            provider.addAlgorithm(stringBuilder.toString(), className);
        }

        private void addX931Signature(ConfigurableProvider provider, String digest, String className) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("withRSA/X9.31");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSA/X9.31");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("WithRSA/X9.31");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(digest);
            stringBuilder3.append("WITHRSA/X9.31");
            provider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(digest);
            stringBuilder.append("WITHRSA/X9.31");
            provider.addAlgorithm(stringBuilder.toString(), className);
        }
    }

    static {
        generalRsaAttributes.put("SupportedKeyClasses", "javax.crypto.interfaces.RSAPublicKey|javax.crypto.interfaces.RSAPrivateKey");
        generalRsaAttributes.put("SupportedKeyFormats", "PKCS#8|X.509");
    }
}
