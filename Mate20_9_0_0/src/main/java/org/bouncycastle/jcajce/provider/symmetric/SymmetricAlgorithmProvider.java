package org.bouncycastle.jcajce.provider.symmetric;

import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

abstract class SymmetricAlgorithmProvider extends AlgorithmProvider {
    SymmetricAlgorithmProvider() {
    }

    protected void addCMacAlgorithm(ConfigurableProvider configurableProvider, String str, String str2, String str3) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mac.");
        stringBuilder.append(str);
        stringBuilder.append("-CMAC");
        configurableProvider.addAlgorithm(stringBuilder.toString(), str2);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Alg.Alias.Mac.");
        stringBuilder2.append(str);
        stringBuilder2.append("CMAC");
        str2 = stringBuilder2.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("-CMAC");
        configurableProvider.addAlgorithm(str2, stringBuilder.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("KeyGenerator.");
        stringBuilder2.append(str);
        stringBuilder2.append("-CMAC");
        configurableProvider.addAlgorithm(stringBuilder2.toString(), str3);
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Alg.Alias.KeyGenerator.");
        stringBuilder2.append(str);
        stringBuilder2.append("CMAC");
        str2 = stringBuilder2.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(str);
        stringBuilder3.append("-CMAC");
        configurableProvider.addAlgorithm(str2, stringBuilder3.toString());
    }

    protected void addGMacAlgorithm(ConfigurableProvider configurableProvider, String str, String str2, String str3) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mac.");
        stringBuilder.append(str);
        stringBuilder.append("-GMAC");
        configurableProvider.addAlgorithm(stringBuilder.toString(), str2);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Alg.Alias.Mac.");
        stringBuilder2.append(str);
        stringBuilder2.append("GMAC");
        str2 = stringBuilder2.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append("-GMAC");
        configurableProvider.addAlgorithm(str2, stringBuilder.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("KeyGenerator.");
        stringBuilder2.append(str);
        stringBuilder2.append("-GMAC");
        configurableProvider.addAlgorithm(stringBuilder2.toString(), str3);
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Alg.Alias.KeyGenerator.");
        stringBuilder2.append(str);
        stringBuilder2.append("GMAC");
        str2 = stringBuilder2.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(str);
        stringBuilder3.append("-GMAC");
        configurableProvider.addAlgorithm(str2, stringBuilder3.toString());
    }

    protected void addPoly1305Algorithm(ConfigurableProvider configurableProvider, String str, String str2, String str3) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mac.POLY1305-");
        stringBuilder.append(str);
        configurableProvider.addAlgorithm(stringBuilder.toString(), str2);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Alg.Alias.Mac.POLY1305");
        stringBuilder2.append(str);
        str2 = stringBuilder2.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("POLY1305-");
        stringBuilder.append(str);
        configurableProvider.addAlgorithm(str2, stringBuilder.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("KeyGenerator.POLY1305-");
        stringBuilder2.append(str);
        configurableProvider.addAlgorithm(stringBuilder2.toString(), str3);
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Alg.Alias.KeyGenerator.POLY1305");
        stringBuilder2.append(str);
        str2 = stringBuilder2.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("POLY1305-");
        stringBuilder3.append(str);
        configurableProvider.addAlgorithm(str2, stringBuilder3.toString());
    }
}
