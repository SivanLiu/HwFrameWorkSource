package com.android.org.bouncycastle.jcajce.provider.digest;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

abstract class DigestAlgorithmProvider extends AlgorithmProvider {
    DigestAlgorithmProvider() {
    }

    protected void addHMACAlgorithm(ConfigurableProvider provider, String algorithm, String algorithmClassName, String keyGeneratorClassName) {
        String mainName = new StringBuilder();
        mainName.append("HMAC");
        mainName.append(algorithm);
        mainName = mainName.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mac.");
        stringBuilder.append(mainName);
        provider.addAlgorithm(stringBuilder.toString(), algorithmClassName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.Mac.HMAC-");
        stringBuilder.append(algorithm);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.Mac.HMAC/");
        stringBuilder.append(algorithm);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("KeyGenerator.");
        stringBuilder.append(mainName);
        provider.addAlgorithm(stringBuilder.toString(), keyGeneratorClassName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyGenerator.HMAC-");
        stringBuilder.append(algorithm);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyGenerator.HMAC/");
        stringBuilder.append(algorithm);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
    }

    protected void addHMACAlias(ConfigurableProvider provider, String algorithm, ASN1ObjectIdentifier oid) {
        String mainName = new StringBuilder();
        mainName.append("HMAC");
        mainName.append(algorithm);
        mainName = mainName.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.Mac.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyGenerator.");
        stringBuilder.append(oid);
        provider.addAlgorithm(stringBuilder.toString(), mainName);
    }
}
