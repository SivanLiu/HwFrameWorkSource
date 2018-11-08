package com.android.org.bouncycastle.jcajce.provider.asymmetric.util;

public class PrimeCertaintyCalculator {
    private PrimeCertaintyCalculator() {
    }

    public static int getDefaultCertainty(int keySizeInBits) {
        return keySizeInBits <= 1024 ? 80 : (((keySizeInBits - 1) / 1024) * 16) + 96;
    }
}
