package org.bouncycastle.pqc.jcajce.provider.mceliece;

import org.bouncycastle.pqc.crypto.mceliece.McElieceCCA2PrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mceliece.McElieceCCA2PublicKeyParameters;
import org.bouncycastle.pqc.math.linearalgebra.GF2Matrix;
import org.bouncycastle.pqc.math.linearalgebra.GF2Vector;
import org.bouncycastle.pqc.math.linearalgebra.GF2mField;
import org.bouncycastle.pqc.math.linearalgebra.GoppaCode;
import org.bouncycastle.pqc.math.linearalgebra.Permutation;
import org.bouncycastle.pqc.math.linearalgebra.PolynomialGF2mSmallM;
import org.bouncycastle.pqc.math.linearalgebra.Vector;

public final class McElieceCCA2Primitives {
    private McElieceCCA2Primitives() {
    }

    public static GF2Vector[] decryptionPrimitive(McElieceCCA2PrivateKeyParameters mcElieceCCA2PrivateKeyParameters, GF2Vector gF2Vector) {
        int k = mcElieceCCA2PrivateKeyParameters.getK();
        Permutation p = mcElieceCCA2PrivateKeyParameters.getP();
        GF2mField field = mcElieceCCA2PrivateKeyParameters.getField();
        PolynomialGF2mSmallM goppaPoly = mcElieceCCA2PrivateKeyParameters.getGoppaPoly();
        GF2Matrix h = mcElieceCCA2PrivateKeyParameters.getH();
        Vector vector = (GF2Vector) gF2Vector.multiply(p.computeInverse());
        Vector syndromeDecode = GoppaCode.syndromeDecode((GF2Vector) h.rightMultiply(vector), field, goppaPoly, mcElieceCCA2PrivateKeyParameters.getQInv());
        gF2Vector = (GF2Vector) ((GF2Vector) vector.add(syndromeDecode)).multiply(p);
        GF2Vector gF2Vector2 = (GF2Vector) syndromeDecode.multiply(p);
        gF2Vector = gF2Vector.extractRightVector(k);
        return new GF2Vector[]{gF2Vector, gF2Vector2};
    }

    public static GF2Vector[] decryptionPrimitive(BCMcElieceCCA2PrivateKey bCMcElieceCCA2PrivateKey, GF2Vector gF2Vector) {
        int k = bCMcElieceCCA2PrivateKey.getK();
        Permutation p = bCMcElieceCCA2PrivateKey.getP();
        GF2mField field = bCMcElieceCCA2PrivateKey.getField();
        PolynomialGF2mSmallM goppaPoly = bCMcElieceCCA2PrivateKey.getGoppaPoly();
        GF2Matrix h = bCMcElieceCCA2PrivateKey.getH();
        Vector vector = (GF2Vector) gF2Vector.multiply(p.computeInverse());
        Vector syndromeDecode = GoppaCode.syndromeDecode((GF2Vector) h.rightMultiply(vector), field, goppaPoly, bCMcElieceCCA2PrivateKey.getQInv());
        gF2Vector = (GF2Vector) ((GF2Vector) vector.add(syndromeDecode)).multiply(p);
        GF2Vector gF2Vector2 = (GF2Vector) syndromeDecode.multiply(p);
        gF2Vector = gF2Vector.extractRightVector(k);
        return new GF2Vector[]{gF2Vector, gF2Vector2};
    }

    public static GF2Vector encryptionPrimitive(McElieceCCA2PublicKeyParameters mcElieceCCA2PublicKeyParameters, GF2Vector gF2Vector, GF2Vector gF2Vector2) {
        return (GF2Vector) mcElieceCCA2PublicKeyParameters.getG().leftMultiplyLeftCompactForm(gF2Vector).add(gF2Vector2);
    }

    public static GF2Vector encryptionPrimitive(BCMcElieceCCA2PublicKey bCMcElieceCCA2PublicKey, GF2Vector gF2Vector, GF2Vector gF2Vector2) {
        return (GF2Vector) bCMcElieceCCA2PublicKey.getG().leftMultiplyLeftCompactForm(gF2Vector).add(gF2Vector2);
    }
}
