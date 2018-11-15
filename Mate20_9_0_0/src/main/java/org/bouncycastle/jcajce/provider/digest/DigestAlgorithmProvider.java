package org.bouncycastle.jcajce.provider.digest;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

abstract class DigestAlgorithmProvider extends AlgorithmProvider {
    DigestAlgorithmProvider() {
    }

    protected void addHMACAlgorithm(ConfigurableProvider configurableProvider, String str, String str2, String str3) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HMAC");
        stringBuilder.append(str);
        String stringBuilder2 = stringBuilder.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Mac.");
        stringBuilder3.append(stringBuilder2);
        configurableProvider.addAlgorithm(stringBuilder3.toString(), str2);
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Alg.Alias.Mac.HMAC-");
        stringBuilder4.append(str);
        configurableProvider.addAlgorithm(stringBuilder4.toString(), stringBuilder2);
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Alg.Alias.Mac.HMAC/");
        stringBuilder4.append(str);
        configurableProvider.addAlgorithm(stringBuilder4.toString(), stringBuilder2);
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("KeyGenerator.");
        stringBuilder4.append(stringBuilder2);
        configurableProvider.addAlgorithm(stringBuilder4.toString(), str3);
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Alg.Alias.KeyGenerator.HMAC-");
        stringBuilder4.append(str);
        configurableProvider.addAlgorithm(stringBuilder4.toString(), stringBuilder2);
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Alg.Alias.KeyGenerator.HMAC/");
        stringBuilder4.append(str);
        configurableProvider.addAlgorithm(stringBuilder4.toString(), stringBuilder2);
    }

    protected void addHMACAlias(ConfigurableProvider configurableProvider, String str, ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HMAC");
        stringBuilder.append(str);
        str = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.Mac.");
        stringBuilder.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Alg.Alias.KeyGenerator.");
        stringBuilder.append(aSN1ObjectIdentifier);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str);
    }
}
