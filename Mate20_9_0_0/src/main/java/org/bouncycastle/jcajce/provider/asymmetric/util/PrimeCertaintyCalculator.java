package org.bouncycastle.jcajce.provider.asymmetric.util;

public class PrimeCertaintyCalculator {
    private PrimeCertaintyCalculator() {
    }

    public static int getDefaultCertainty(int i) {
        return i <= 1024 ? 80 : 96 + (16 * ((i - 1) / 1024));
    }
}
