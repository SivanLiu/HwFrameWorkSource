package org.bouncycastle.pqc.crypto.mceliece;

import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.pqc.crypto.MessageEncryptor;
import org.bouncycastle.pqc.math.linearalgebra.GF2Matrix;
import org.bouncycastle.pqc.math.linearalgebra.GF2Vector;
import org.bouncycastle.pqc.math.linearalgebra.GF2mField;
import org.bouncycastle.pqc.math.linearalgebra.GoppaCode;
import org.bouncycastle.pqc.math.linearalgebra.Permutation;
import org.bouncycastle.pqc.math.linearalgebra.PolynomialGF2mSmallM;
import org.bouncycastle.pqc.math.linearalgebra.Vector;

public class McElieceCipher implements MessageEncryptor {
    public static final String OID = "1.3.6.1.4.1.8301.3.1.3.4.1";
    public int cipherTextSize;
    private boolean forEncryption;
    private int k;
    private McElieceKeyParameters key;
    public int maxPlainTextSize;
    private int n;
    private SecureRandom sr;
    private int t;

    private byte[] computeMessage(GF2Vector gF2Vector) throws InvalidCipherTextException {
        Object encoded = gF2Vector.getEncoded();
        int length = encoded.length - 1;
        while (length >= 0 && encoded[length] == (byte) 0) {
            length--;
        }
        if (length < 0 || encoded[length] != (byte) 1) {
            throw new InvalidCipherTextException("Bad Padding: invalid ciphertext");
        }
        Object obj = new byte[length];
        System.arraycopy(encoded, 0, obj, 0, length);
        return obj;
    }

    private GF2Vector computeMessageRepresentative(byte[] bArr) {
        Object obj = new byte[(this.maxPlainTextSize + ((this.k & 7) != 0 ? 1 : 0))];
        System.arraycopy(bArr, 0, obj, 0, bArr.length);
        obj[bArr.length] = 1;
        return GF2Vector.OS2VP(this.k, obj);
    }

    private void initCipherDecrypt(McEliecePrivateKeyParameters mcEliecePrivateKeyParameters) {
        this.n = mcEliecePrivateKeyParameters.getN();
        this.k = mcEliecePrivateKeyParameters.getK();
        this.maxPlainTextSize = this.k >> 3;
        this.cipherTextSize = this.n >> 3;
    }

    private void initCipherEncrypt(McEliecePublicKeyParameters mcEliecePublicKeyParameters) {
        this.sr = this.sr != null ? this.sr : new SecureRandom();
        this.n = mcEliecePublicKeyParameters.getN();
        this.k = mcEliecePublicKeyParameters.getK();
        this.t = mcEliecePublicKeyParameters.getT();
        this.cipherTextSize = this.n >> 3;
        this.maxPlainTextSize = this.k >> 3;
    }

    public int getKeySize(McElieceKeyParameters mcElieceKeyParameters) {
        if (mcElieceKeyParameters instanceof McEliecePublicKeyParameters) {
            return ((McEliecePublicKeyParameters) mcElieceKeyParameters).getN();
        }
        if (mcElieceKeyParameters instanceof McEliecePrivateKeyParameters) {
            return ((McEliecePrivateKeyParameters) mcElieceKeyParameters).getN();
        }
        throw new IllegalArgumentException("unsupported type");
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.forEncryption = z;
        if (z) {
            if (cipherParameters instanceof ParametersWithRandom) {
                ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
                this.sr = parametersWithRandom.getRandom();
                this.key = (McEliecePublicKeyParameters) parametersWithRandom.getParameters();
            } else {
                this.sr = new SecureRandom();
                this.key = (McEliecePublicKeyParameters) cipherParameters;
            }
            initCipherEncrypt((McEliecePublicKeyParameters) this.key);
            return;
        }
        this.key = (McEliecePrivateKeyParameters) cipherParameters;
        initCipherDecrypt((McEliecePrivateKeyParameters) this.key);
    }

    public byte[] messageDecrypt(byte[] bArr) throws InvalidCipherTextException {
        if (this.forEncryption) {
            throw new IllegalStateException("cipher initialised for decryption");
        }
        GF2Vector OS2VP = GF2Vector.OS2VP(this.n, bArr);
        McEliecePrivateKeyParameters mcEliecePrivateKeyParameters = (McEliecePrivateKeyParameters) this.key;
        GF2mField field = mcEliecePrivateKeyParameters.getField();
        PolynomialGF2mSmallM goppaPoly = mcEliecePrivateKeyParameters.getGoppaPoly();
        GF2Matrix sInv = mcEliecePrivateKeyParameters.getSInv();
        Permutation p1 = mcEliecePrivateKeyParameters.getP1();
        Permutation p2 = mcEliecePrivateKeyParameters.getP2();
        GF2Matrix h = mcEliecePrivateKeyParameters.getH();
        PolynomialGF2mSmallM[] qInv = mcEliecePrivateKeyParameters.getQInv();
        p2 = p1.rightMultiply(p2);
        Vector vector = (GF2Vector) OS2VP.multiply(p2.computeInverse());
        Vector syndromeDecode = GoppaCode.syndromeDecode((GF2Vector) h.rightMultiply(vector), field, goppaPoly, qInv);
        OS2VP = (GF2Vector) ((GF2Vector) vector.add(syndromeDecode)).multiply(p1);
        GF2Vector gF2Vector = (GF2Vector) syndromeDecode.multiply(p2);
        return computeMessage((GF2Vector) sInv.leftMultiply(OS2VP.extractRightVector(this.k)));
    }

    public byte[] messageEncrypt(byte[] bArr) {
        if (this.forEncryption) {
            Vector computeMessageRepresentative = computeMessageRepresentative(bArr);
            return ((GF2Vector) ((McEliecePublicKeyParameters) this.key).getG().leftMultiply(computeMessageRepresentative).add(new GF2Vector(this.n, this.t, this.sr))).getEncoded();
        }
        throw new IllegalStateException("cipher initialised for decryption");
    }
}
