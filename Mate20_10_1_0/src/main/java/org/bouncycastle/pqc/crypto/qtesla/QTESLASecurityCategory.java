package org.bouncycastle.pqc.crypto.qtesla;

import org.bouncycastle.pqc.crypto.qteslarnd1.Polynomial;

public class QTESLASecurityCategory {
    public static final int PROVABLY_SECURE_I = 5;
    public static final int PROVABLY_SECURE_III = 6;

    private QTESLASecurityCategory() {
    }

    public static String getName(int i) {
        if (i == 5) {
            return "qTESLA-p-I";
        }
        if (i == 6) {
            return "qTESLA-p-III";
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static int getPrivateSize(int i) {
        if (i == 5) {
            return Polynomial.PRIVATE_KEY_I_P;
        }
        if (i == 6) {
            return Polynomial.PRIVATE_KEY_III_P;
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static int getPublicSize(int i) {
        if (i == 5) {
            return Polynomial.PUBLIC_KEY_I_P;
        }
        if (i == 6) {
            return 38432;
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static int getSignatureSize(int i) {
        if (i == 5) {
            return 2592;
        }
        if (i == 6) {
            return 5664;
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static void validate(int i) {
        if (i != 5 && i != 6) {
            throw new IllegalArgumentException("unknown security category: " + i);
        }
    }
}
