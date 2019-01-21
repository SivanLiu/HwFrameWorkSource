package com.android.org.bouncycastle.jcajce.provider.util;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;

public abstract class AsymmetricAlgorithmProvider extends AlgorithmProvider {
    protected void addSignatureAlgorithm(ConfigurableProvider provider, String digest, String algorithm, String className, ASN1ObjectIdentifier oid) {
        String mainName = new StringBuilder();
        mainName.append(digest);
        mainName.append("WITH");
        mainName.append(algorithm);
        mainName = mainName.toString();
        String jdk11Variation1 = new StringBuilder();
        jdk11Variation1.append(digest);
        jdk11Variation1.append("with");
        jdk11Variation1.append(algorithm);
        jdk11Variation1 = jdk11Variation1.toString();
        String jdk11Variation2 = new StringBuilder();
        jdk11Variation2.append(digest);
        jdk11Variation2.append("With");
        jdk11Variation2.append(algorithm);
        jdk11Variation2 = jdk11Variation2.toString();
        String alias = new StringBuilder();
        alias.append(digest);
        alias.append("/");
        alias.append(algorithm);
        alias = alias.toString();
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
        stringBuilder.append(alias);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.Signature.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.Signature.OID.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
    }

    protected void registerOid(ConfigurableProvider provider, ASN1ObjectIdentifier oid, String name, AsymmetricKeyInfoConverter keyFactory) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyFactory.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), name);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyPairGenerator.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), name);
        provider.addKeyInfoConverter(oid, keyFactory);
    }

    protected void registerOidAlgorithmParameters(ConfigurableProvider provider, ASN1ObjectIdentifier oid, String name) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.AlgorithmParameters.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), name);
    }

    protected void registerOidAlgorithmParameterGenerator(ConfigurableProvider provider, ASN1ObjectIdentifier oid, String name) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.AlgorithmParameterGenerator.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), name);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.AlgorithmParameters.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), name);
    }
}
