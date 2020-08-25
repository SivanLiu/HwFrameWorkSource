package org.bouncycastle.pqc.crypto.qteslarnd1;

public class QTESLASecurityCategory {
    public static final int HEURISTIC_I = 0;
    public static final int HEURISTIC_III_SIZE = 1;
    public static final int HEURISTIC_III_SPEED = 2;
    public static final int PROVABLY_SECURE_I = 3;
    public static final int PROVABLY_SECURE_III = 4;

    private QTESLASecurityCategory() {
    }

    public static String getName(int i) {
        if (i == 0) {
            return "qTESLA-I";
        }
        if (i == 1) {
            return "qTESLA-III-size";
        }
        if (i == 2) {
            return "qTESLA-III-speed";
        }
        if (i == 3) {
            return "qTESLA-p-I";
        }
        if (i == 4) {
            return "qTESLA-p-III";
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static int getPrivateSize(int i) {
        if (i == 0) {
            return Polynomial.PRIVATE_KEY_I;
        }
        if (i == 1) {
            return Polynomial.PRIVATE_KEY_III_SIZE;
        }
        if (i == 2) {
            return Polynomial.PRIVATE_KEY_III_SPEED;
        }
        if (i == 3) {
            return Polynomial.PRIVATE_KEY_I_P;
        }
        if (i == 4) {
            return Polynomial.PRIVATE_KEY_III_P;
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static int getPublicSize(int i) {
        if (i == 0) {
            return Polynomial.PUBLIC_KEY_I;
        }
        if (i == 1) {
            return Polynomial.PUBLIC_KEY_III_SIZE;
        }
        if (i == 2) {
            return Polynomial.PUBLIC_KEY_III_SPEED;
        }
        if (i == 3) {
            return Polynomial.PUBLIC_KEY_I_P;
        }
        if (i == 4) {
            return Polynomial.PUBLIC_KEY_III_P;
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static int getSignatureSize(int i) {
        if (i == 0) {
            return Polynomial.SIGNATURE_I;
        }
        if (i == 1) {
            return Polynomial.SIGNATURE_III_SIZE;
        }
        if (i == 2 || i == 3) {
            return 2848;
        }
        if (i == 4) {
            return Polynomial.SIGNATURE_III_P;
        }
        throw new IllegalArgumentException("unknown security category: " + i);
    }

    static void validate(int i) {
        if (i != 0 && i != 1 && i != 2 && i != 3 && i != 4) {
            throw new IllegalArgumentException("unknown security category: " + i);
        }
    }
}
