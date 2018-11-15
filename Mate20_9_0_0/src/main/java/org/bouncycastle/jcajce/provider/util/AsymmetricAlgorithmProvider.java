package org.bouncycastle.jcajce.provider.util;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;

public abstract class AsymmetricAlgorithmProvider extends AlgorithmProvider {
    protected void addSignatureAlgorithm(ConfigurableProvider configurableProvider, String str, String str2, String str3, ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("WITH");
        stringBuilder.append(str2);
        String stringBuilder2 = stringBuilder.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(str);
        stringBuilder3.append("with");
        stringBuilder3.append(str2);
        String stringBuilder4 = stringBuilder3.toString();
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append(str);
        stringBuilder5.append("With");
        stringBuilder5.append(str2);
        String stringBuilder6 = stringBuilder5.toString();
        StringBuilder stringBuilder7 = new StringBuilder();
        stringBuilder7.append(str);
        stringBuilder7.append("/");
        stringBuilder7.append(str2);
        str = stringBuilder7.toString();
        StringBuilder stringBuilder8 = new StringBuilder();
        stringBuilder8.append("Signature.");
        stringBuilder8.append(stringBuilder2);
        configurableProvider.addAlgorithm(stringBuilder8.toString(), str3);
        stringBuilder8 = new StringBuilder();
        stringBuilder8.append("Alg.Alias.Signature.");
        stringBuilder8.append(stringBuilder4);
        configurableProvider.addAlgorithm(stringBuilder8.toString(), stringBuilder2);
        stringBuilder8 = new StringBuilder();
        stringBuilder8.append("Alg.Alias.Signature.");
        stringBuilder8.append(stringBuilder6);
        configurableProvider.addAlgorithm(stringBuilder8.toString(), stringBuilder2);
        stringBuilder8 = new StringBuilder();
        stringBuilder8.append("Alg.Alias.Signature.");
        stringBuilder8.append(str);
        configurableProvider.addAlgorithm(stringBuilder8.toString(), stringBuilder2);
        StringBuilder stringBuilder9 = new StringBuilder();
        stringBuilder9.append("Alg.Alias.Signature.");
        stringBuilder9.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder9.toString(), stringBuilder2);
        stringBuilder9 = new StringBuilder();
        stringBuilder9.append("Alg.Alias.Signature.OID.");
        stringBuilder9.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder9.toString(), stringBuilder2);
    }

    protected void registerOid(ConfigurableProvider configurableProvider, ASN1ObjectIdentifier aSN1ObjectIdentifier, String str, AsymmetricKeyInfoConverter asymmetricKeyInfoConverter) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyFactory.");
        stringBuilder.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyPairGenerator.");
        stringBuilder.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str);
        configurableProvider.addKeyInfoConverter(aSN1ObjectIdentifier, asymmetricKeyInfoConverter);
    }

    protected void registerOidAlgorithmParameterGenerator(ConfigurableProvider configurableProvider, ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.AlgorithmParameterGenerator.");
        stringBuilder.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.AlgorithmParameters.");
        stringBuilder.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str);
    }

    protected void registerOidAlgorithmParameters(ConfigurableProvider configurableProvider, ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.AlgorithmParameters.");
        stringBuilder.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str);
    }
}
