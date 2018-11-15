package org.bouncycastle.jcajce.provider.asymmetric;

import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.dh.KeyFactorySpi;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;

public class DH {
    private static final String PREFIX = "org.bouncycastle.jcajce.provider.asymmetric.dh.";
    private static final Map<String, String> generalDhAttributes = new HashMap();

    public static class Mappings extends AsymmetricAlgorithmProvider {
        public void configure(ConfigurableProvider configurableProvider) {
            configurableProvider.addAlgorithm("KeyPairGenerator.DH", "org.bouncycastle.jcajce.provider.asymmetric.dh.KeyPairGeneratorSpi");
            configurableProvider.addAlgorithm("Alg.Alias.KeyPairGenerator.DIFFIEHELLMAN", "DH");
            configurableProvider.addAttributes("KeyAgreement.DH", DH.generalDhAttributes);
            configurableProvider.addAlgorithm("KeyAgreement.DH", "org.bouncycastle.jcajce.provider.asymmetric.dh.KeyAgreementSpi");
            configurableProvider.addAlgorithm("Alg.Alias.KeyAgreement.DIFFIEHELLMAN", "DH");
            configurableProvider.addAlgorithm("KeyAgreement", PKCSObjectIdentifiers.id_alg_ESDH, "org.bouncycastle.jcajce.provider.asymmetric.dh.KeyAgreementSpi$DHwithRFC2631KDF");
            configurableProvider.addAlgorithm("KeyAgreement", PKCSObjectIdentifiers.id_alg_SSDH, "org.bouncycastle.jcajce.provider.asymmetric.dh.KeyAgreementSpi$DHwithRFC2631KDF");
            configurableProvider.addAlgorithm("KeyFactory.DH", "org.bouncycastle.jcajce.provider.asymmetric.dh.KeyFactorySpi");
            configurableProvider.addAlgorithm("Alg.Alias.KeyFactory.DIFFIEHELLMAN", "DH");
            configurableProvider.addAlgorithm("AlgorithmParameters.DH", "org.bouncycastle.jcajce.provider.asymmetric.dh.AlgorithmParametersSpi");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.DIFFIEHELLMAN", "DH");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator.DIFFIEHELLMAN", "DH");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.DH", "org.bouncycastle.jcajce.provider.asymmetric.dh.AlgorithmParameterGeneratorSpi");
            configurableProvider.addAlgorithm("Cipher.IES", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IES");
            configurableProvider.addAlgorithm("Cipher.IESwithAES-CBC", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IESwithAESCBC");
            configurableProvider.addAlgorithm("Cipher.IESWITHAES-CBC", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IESwithAESCBC");
            configurableProvider.addAlgorithm("Cipher.IESWITHDESEDE-CBC", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IESwithDESedeCBC");
            configurableProvider.addAlgorithm("Cipher.DHIES", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IES");
            configurableProvider.addAlgorithm("Cipher.DHIESwithAES-CBC", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IESwithAESCBC");
            configurableProvider.addAlgorithm("Cipher.DHIESWITHAES-CBC", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IESwithAESCBC");
            configurableProvider.addAlgorithm("Cipher.DHIESWITHDESEDE-CBC", "org.bouncycastle.jcajce.provider.asymmetric.dh.IESCipher$IESwithDESedeCBC");
            registerOid(configurableProvider, PKCSObjectIdentifiers.dhKeyAgreement, "DH", new KeyFactorySpi());
            registerOid(configurableProvider, X9ObjectIdentifiers.dhpublicnumber, "DH", new KeyFactorySpi());
        }
    }

    static {
        generalDhAttributes.put("SupportedKeyClasses", "javax.crypto.interfaces.DHPublicKey|javax.crypto.interfaces.DHPrivateKey");
        generalDhAttributes.put("SupportedKeyFormats", "PKCS#8|X.509");
    }
}
