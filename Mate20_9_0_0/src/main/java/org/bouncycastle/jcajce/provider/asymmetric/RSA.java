package org.bouncycastle.jcajce.provider.asymmetric;

import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import org.bouncycastle.pqc.jcajce.spec.SPHINCS256KeyGenParameterSpec;

public class RSA {
    private static final String PREFIX = "org.bouncycastle.jcajce.provider.asymmetric.rsa.";
    private static final Map<String, String> generalRsaAttributes = new HashMap();

    public static class Mappings extends AsymmetricAlgorithmProvider {
        private void addDigestSignature(ConfigurableProvider configurableProvider, String str, String str2, ASN1ObjectIdentifier aSN1ObjectIdentifier) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append("WITHRSA");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("withRSA");
            String stringBuilder4 = stringBuilder3.toString();
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append(str);
            stringBuilder5.append("WithRSA");
            String stringBuilder6 = stringBuilder5.toString();
            StringBuilder stringBuilder7 = new StringBuilder();
            stringBuilder7.append(str);
            stringBuilder7.append("/RSA");
            String stringBuilder8 = stringBuilder7.toString();
            StringBuilder stringBuilder9 = new StringBuilder();
            stringBuilder9.append(str);
            stringBuilder9.append("WITHRSAENCRYPTION");
            String stringBuilder10 = stringBuilder9.toString();
            StringBuilder stringBuilder11 = new StringBuilder();
            stringBuilder11.append(str);
            stringBuilder11.append("withRSAEncryption");
            String stringBuilder12 = stringBuilder11.toString();
            StringBuilder stringBuilder13 = new StringBuilder();
            stringBuilder13.append(str);
            stringBuilder13.append("WithRSAEncryption");
            str = stringBuilder13.toString();
            stringBuilder13 = new StringBuilder();
            stringBuilder13.append("Signature.");
            stringBuilder13.append(stringBuilder2);
            configurableProvider.addAlgorithm(stringBuilder13.toString(), str2);
            StringBuilder stringBuilder14 = new StringBuilder();
            stringBuilder14.append("Alg.Alias.Signature.");
            stringBuilder14.append(stringBuilder4);
            configurableProvider.addAlgorithm(stringBuilder14.toString(), stringBuilder2);
            stringBuilder14 = new StringBuilder();
            stringBuilder14.append("Alg.Alias.Signature.");
            stringBuilder14.append(stringBuilder6);
            configurableProvider.addAlgorithm(stringBuilder14.toString(), stringBuilder2);
            stringBuilder14 = new StringBuilder();
            stringBuilder14.append("Alg.Alias.Signature.");
            stringBuilder14.append(stringBuilder10);
            configurableProvider.addAlgorithm(stringBuilder14.toString(), stringBuilder2);
            stringBuilder14 = new StringBuilder();
            stringBuilder14.append("Alg.Alias.Signature.");
            stringBuilder14.append(stringBuilder12);
            configurableProvider.addAlgorithm(stringBuilder14.toString(), stringBuilder2);
            stringBuilder14 = new StringBuilder();
            stringBuilder14.append("Alg.Alias.Signature.");
            stringBuilder14.append(str);
            configurableProvider.addAlgorithm(stringBuilder14.toString(), stringBuilder2);
            StringBuilder stringBuilder15 = new StringBuilder();
            stringBuilder15.append("Alg.Alias.Signature.");
            stringBuilder15.append(stringBuilder8);
            configurableProvider.addAlgorithm(stringBuilder15.toString(), stringBuilder2);
            if (aSN1ObjectIdentifier != null) {
                stringBuilder15 = new StringBuilder();
                stringBuilder15.append("Alg.Alias.Signature.");
                stringBuilder15.append(aSN1ObjectIdentifier);
                configurableProvider.addAlgorithm(stringBuilder15.toString(), stringBuilder2);
                stringBuilder15 = new StringBuilder();
                stringBuilder15.append("Alg.Alias.Signature.OID.");
                stringBuilder15.append(aSN1ObjectIdentifier);
                configurableProvider.addAlgorithm(stringBuilder15.toString(), stringBuilder2);
            }
        }

        private void addISO9796Signature(ConfigurableProvider configurableProvider, String str, String str2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("withRSA/ISO9796-2");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSA/ISO9796-2");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("WithRSA/ISO9796-2");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSA/ISO9796-2");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(str);
            stringBuilder.append("WITHRSA/ISO9796-2");
            configurableProvider.addAlgorithm(stringBuilder.toString(), str2);
        }

        private void addPSSSignature(ConfigurableProvider configurableProvider, String str, String str2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("withRSA/PSS");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSAANDMGF1");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("WithRSA/PSS");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSAANDMGF1");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("withRSAandMGF1");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSAANDMGF1");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("WithRSAAndMGF1");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSAANDMGF1");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(str);
            stringBuilder.append("WITHRSAANDMGF1");
            configurableProvider.addAlgorithm(stringBuilder.toString(), str2);
        }

        private void addX931Signature(ConfigurableProvider configurableProvider, String str, String str2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("withRSA/X9.31");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSA/X9.31");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.Signature.");
            stringBuilder.append(str);
            stringBuilder.append("WithRSA/X9.31");
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("WITHRSA/X9.31");
            configurableProvider.addAlgorithm(stringBuilder2, stringBuilder3.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(str);
            stringBuilder.append("WITHRSA/X9.31");
            configurableProvider.addAlgorithm(stringBuilder.toString(), str2);
        }

        public void configure(ConfigurableProvider configurableProvider) {
            configurableProvider.addAlgorithm("AlgorithmParameters.OAEP", "org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$OAEP");
            configurableProvider.addAlgorithm("AlgorithmParameters.PSS", "org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.RSAPSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.RSASSA-PSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA224withRSA/PSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA256withRSA/PSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA384withRSA/PSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA512withRSA/PSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA224WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA256WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA384WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA512WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA3-224WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA3-256WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA3-384WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA3-512WITHRSAANDMGF1", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.RAWRSAPSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.NONEWITHRSAPSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.NONEWITHRSASSA-PSS", "PSS");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.NONEWITHRSAANDMGF1", "PSS");
            configurableProvider.addAttributes("Cipher.RSA", RSA.generalRsaAttributes);
            configurableProvider.addAlgorithm("Cipher.RSA", "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$NoPadding");
            configurableProvider.addAlgorithm("Cipher.RSA/RAW", "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$NoPadding");
            configurableProvider.addAlgorithm("Cipher.RSA/PKCS1", "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding");
            configurableProvider.addAlgorithm("Cipher", PKCSObjectIdentifiers.rsaEncryption, "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding");
            configurableProvider.addAlgorithm("Cipher", X509ObjectIdentifiers.id_ea_rsa, "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding");
            configurableProvider.addAlgorithm("Cipher.RSA/1", "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding_PrivateOnly");
            configurableProvider.addAlgorithm("Cipher.RSA/2", "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$PKCS1v1_5Padding_PublicOnly");
            configurableProvider.addAlgorithm("Cipher.RSA/OAEP", "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$OAEPPadding");
            configurableProvider.addAlgorithm("Cipher", PKCSObjectIdentifiers.id_RSAES_OAEP, "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$OAEPPadding");
            configurableProvider.addAlgorithm("Cipher.RSA/ISO9796-1", "org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$ISO9796d1Padding");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA//RAW", "RSA");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA//NOPADDING", "RSA");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA//PKCS1PADDING", "RSA/PKCS1");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA//OAEPPADDING", "RSA/OAEP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA//ISO9796-1PADDING", "RSA/ISO9796-1");
            configurableProvider.addAlgorithm("KeyFactory.RSA", "org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi");
            configurableProvider.addAlgorithm("KeyPairGenerator.RSA", "org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyPairGeneratorSpi");
            KeyFactorySpi keyFactorySpi = new KeyFactorySpi();
            registerOid(configurableProvider, PKCSObjectIdentifiers.rsaEncryption, "RSA", keyFactorySpi);
            registerOid(configurableProvider, X509ObjectIdentifiers.id_ea_rsa, "RSA", keyFactorySpi);
            registerOid(configurableProvider, PKCSObjectIdentifiers.id_RSAES_OAEP, "RSA", keyFactorySpi);
            registerOid(configurableProvider, PKCSObjectIdentifiers.id_RSASSA_PSS, "RSA", keyFactorySpi);
            registerOidAlgorithmParameters(configurableProvider, PKCSObjectIdentifiers.rsaEncryption, "RSA");
            registerOidAlgorithmParameters(configurableProvider, X509ObjectIdentifiers.id_ea_rsa, "RSA");
            registerOidAlgorithmParameters(configurableProvider, PKCSObjectIdentifiers.id_RSAES_OAEP, "OAEP");
            registerOidAlgorithmParameters(configurableProvider, PKCSObjectIdentifiers.id_RSASSA_PSS, "PSS");
            configurableProvider.addAlgorithm("Signature.RSASSA-PSS", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$PSSwithRSA");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.");
            stringBuilder.append(PKCSObjectIdentifiers.id_RSASSA_PSS);
            configurableProvider.addAlgorithm(stringBuilder.toString(), "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$PSSwithRSA");
            stringBuilder = new StringBuilder();
            stringBuilder.append("Signature.OID.");
            stringBuilder.append(PKCSObjectIdentifiers.id_RSASSA_PSS);
            configurableProvider.addAlgorithm(stringBuilder.toString(), "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$PSSwithRSA");
            configurableProvider.addAlgorithm("Signature.RSA", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$noneRSA");
            configurableProvider.addAlgorithm("Signature.RAWRSASSA-PSS", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$nonePSS");
            configurableProvider.addAlgorithm("Alg.Alias.Signature.RAWRSA", "RSA");
            configurableProvider.addAlgorithm("Alg.Alias.Signature.NONEWITHRSA", "RSA");
            configurableProvider.addAlgorithm("Alg.Alias.Signature.RAWRSAPSS", "RAWRSASSA-PSS");
            configurableProvider.addAlgorithm("Alg.Alias.Signature.NONEWITHRSAPSS", "RAWRSASSA-PSS");
            configurableProvider.addAlgorithm("Alg.Alias.Signature.NONEWITHRSASSA-PSS", "RAWRSASSA-PSS");
            configurableProvider.addAlgorithm("Alg.Alias.Signature.NONEWITHRSAANDMGF1", "RAWRSASSA-PSS");
            configurableProvider.addAlgorithm("Alg.Alias.Signature.RSAPSS", "RSASSA-PSS");
            addPSSSignature(configurableProvider, "SHA224", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA224withRSA");
            addPSSSignature(configurableProvider, "SHA256", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSA");
            addPSSSignature(configurableProvider, "SHA384", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA384withRSA");
            addPSSSignature(configurableProvider, "SHA512", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512withRSA");
            addPSSSignature(configurableProvider, "SHA512(224)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_224withRSA");
            addPSSSignature(configurableProvider, "SHA512(256)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA512_256withRSA");
            addPSSSignature(configurableProvider, "SHA3-224", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_224withRSA");
            addPSSSignature(configurableProvider, SPHINCS256KeyGenParameterSpec.SHA3_256, "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_256withRSA");
            addPSSSignature(configurableProvider, "SHA3-384", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_384withRSA");
            addPSSSignature(configurableProvider, "SHA3-512", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA3_512withRSA");
            if (configurableProvider.hasAlgorithm("MessageDigest", "MD2")) {
                addDigestSignature(configurableProvider, "MD2", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD2", PKCSObjectIdentifiers.md2WithRSAEncryption);
            }
            if (configurableProvider.hasAlgorithm("MessageDigest", "MD4")) {
                addDigestSignature(configurableProvider, "MD4", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD4", PKCSObjectIdentifiers.md4WithRSAEncryption);
            }
            if (configurableProvider.hasAlgorithm("MessageDigest", "MD5")) {
                addDigestSignature(configurableProvider, "MD5", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD5", PKCSObjectIdentifiers.md5WithRSAEncryption);
                addISO9796Signature(configurableProvider, "MD5", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$MD5WithRSAEncryption");
            }
            if (configurableProvider.hasAlgorithm("MessageDigest", "SHA1")) {
                configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA1withRSA/PSS", "PSS");
                configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.SHA1WITHRSAANDMGF1", "PSS");
                addPSSSignature(configurableProvider, "SHA1", "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA1withRSA");
                addDigestSignature(configurableProvider, "SHA1", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA1", PKCSObjectIdentifiers.sha1WithRSAEncryption);
                addISO9796Signature(configurableProvider, "SHA1", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA1WithRSAEncryption");
                stringBuilder = new StringBuilder();
                stringBuilder.append("Alg.Alias.Signature.");
                stringBuilder.append(OIWObjectIdentifiers.sha1WithRSA);
                configurableProvider.addAlgorithm(stringBuilder.toString(), "SHA1WITHRSA");
                stringBuilder = new StringBuilder();
                stringBuilder.append("Alg.Alias.Signature.OID.");
                stringBuilder.append(OIWObjectIdentifiers.sha1WithRSA);
                configurableProvider.addAlgorithm(stringBuilder.toString(), "SHA1WITHRSA");
                addX931Signature(configurableProvider, "SHA1", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA1WithRSAEncryption");
            }
            addDigestSignature(configurableProvider, "SHA224", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA224", PKCSObjectIdentifiers.sha224WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA256", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA256", PKCSObjectIdentifiers.sha256WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA384", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA384", PKCSObjectIdentifiers.sha384WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA512", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512", PKCSObjectIdentifiers.sha512WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA512(224)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512_224", PKCSObjectIdentifiers.sha512_224WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA512(256)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512_256", PKCSObjectIdentifiers.sha512_256WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA3-224", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_224", NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_224);
            addDigestSignature(configurableProvider, SPHINCS256KeyGenParameterSpec.SHA3_256, "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_256", NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_256);
            addDigestSignature(configurableProvider, "SHA3-384", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_384", NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_384);
            addDigestSignature(configurableProvider, "SHA3-512", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA3_512", NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_512);
            addISO9796Signature(configurableProvider, "SHA224", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA224WithRSAEncryption");
            addISO9796Signature(configurableProvider, "SHA256", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA256WithRSAEncryption");
            addISO9796Signature(configurableProvider, "SHA384", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA384WithRSAEncryption");
            addISO9796Signature(configurableProvider, "SHA512", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA512WithRSAEncryption");
            addISO9796Signature(configurableProvider, "SHA512(224)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA512_224WithRSAEncryption");
            addISO9796Signature(configurableProvider, "SHA512(256)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$SHA512_256WithRSAEncryption");
            addX931Signature(configurableProvider, "SHA224", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA224WithRSAEncryption");
            addX931Signature(configurableProvider, "SHA256", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA256WithRSAEncryption");
            addX931Signature(configurableProvider, "SHA384", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA384WithRSAEncryption");
            addX931Signature(configurableProvider, "SHA512", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA512WithRSAEncryption");
            addX931Signature(configurableProvider, "SHA512(224)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA512_224WithRSAEncryption");
            addX931Signature(configurableProvider, "SHA512(256)", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$SHA512_256WithRSAEncryption");
            if (configurableProvider.hasAlgorithm("MessageDigest", "RIPEMD128")) {
                addDigestSignature(configurableProvider, "RIPEMD128", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD128", TeleTrusTObjectIdentifiers.rsaSignatureWithripemd128);
                addDigestSignature(configurableProvider, "RMD128", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD128", null);
                addX931Signature(configurableProvider, "RMD128", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$RIPEMD128WithRSAEncryption");
                addX931Signature(configurableProvider, "RIPEMD128", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$RIPEMD128WithRSAEncryption");
            }
            if (configurableProvider.hasAlgorithm("MessageDigest", "RIPEMD160")) {
                addDigestSignature(configurableProvider, "RIPEMD160", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD160", TeleTrusTObjectIdentifiers.rsaSignatureWithripemd160);
                addDigestSignature(configurableProvider, "RMD160", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD160", null);
                configurableProvider.addAlgorithm("Alg.Alias.Signature.RIPEMD160WithRSA/ISO9796-2", "RIPEMD160withRSA/ISO9796-2");
                configurableProvider.addAlgorithm("Signature.RIPEMD160withRSA/ISO9796-2", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$RIPEMD160WithRSAEncryption");
                addX931Signature(configurableProvider, "RMD160", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$RIPEMD160WithRSAEncryption");
                addX931Signature(configurableProvider, "RIPEMD160", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$RIPEMD160WithRSAEncryption");
            }
            if (configurableProvider.hasAlgorithm("MessageDigest", "RIPEMD256")) {
                addDigestSignature(configurableProvider, "RIPEMD256", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD256", TeleTrusTObjectIdentifiers.rsaSignatureWithripemd256);
                addDigestSignature(configurableProvider, "RMD256", "org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$RIPEMD256", null);
            }
            if (configurableProvider.hasAlgorithm("MessageDigest", "WHIRLPOOL")) {
                addISO9796Signature(configurableProvider, "Whirlpool", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$WhirlpoolWithRSAEncryption");
                addISO9796Signature(configurableProvider, "WHIRLPOOL", "org.bouncycastle.jcajce.provider.asymmetric.rsa.ISOSignatureSpi$WhirlpoolWithRSAEncryption");
                addX931Signature(configurableProvider, "Whirlpool", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$WhirlpoolWithRSAEncryption");
                addX931Signature(configurableProvider, "WHIRLPOOL", "org.bouncycastle.jcajce.provider.asymmetric.rsa.X931SignatureSpi$WhirlpoolWithRSAEncryption");
            }
        }
    }

    static {
        generalRsaAttributes.put("SupportedKeyClasses", "javax.crypto.interfaces.RSAPublicKey|javax.crypto.interfaces.RSAPrivateKey");
        generalRsaAttributes.put("SupportedKeyFormats", "PKCS#8|X.509");
    }
}
