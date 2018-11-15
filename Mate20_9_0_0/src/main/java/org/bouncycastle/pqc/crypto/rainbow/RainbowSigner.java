package org.bouncycastle.pqc.crypto.rainbow;

import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.pqc.crypto.MessageSigner;
import org.bouncycastle.pqc.crypto.rainbow.util.ComputeInField;
import org.bouncycastle.pqc.crypto.rainbow.util.GF2Field;

public class RainbowSigner implements MessageSigner {
    private static final int MAXITS = 65536;
    private ComputeInField cf = new ComputeInField();
    RainbowKeyParameters key;
    private SecureRandom random;
    int signableDocumentLength;
    private short[] x;

    private short[] initSign(Layer[] layerArr, short[] sArr) {
        short[] sArr2 = new short[sArr.length];
        sArr = this.cf.multiplyMatrix(((RainbowPrivateKeyParameters) this.key).getInvA1(), this.cf.addVect(((RainbowPrivateKeyParameters) this.key).getB1(), sArr));
        for (int i = 0; i < layerArr[0].getVi(); i++) {
            this.x[i] = (short) this.random.nextInt();
            this.x[i] = (short) (this.x[i] & 255);
        }
        return sArr;
    }

    private short[] makeMessageRepresentative(byte[] bArr) {
        short[] sArr = new short[this.signableDocumentLength];
        int i = 0;
        int i2 = 0;
        while (i < bArr.length) {
            sArr[i] = (short) bArr[i2];
            sArr[i] = (short) (sArr[i] & 255);
            i2++;
            i++;
            if (i >= sArr.length) {
                return sArr;
            }
        }
        return sArr;
    }

    private short[] verifySignatureIntern(short[] sArr) {
        short[][] coeffQuadratic = ((RainbowPublicKeyParameters) this.key).getCoeffQuadratic();
        short[][] coeffSingular = ((RainbowPublicKeyParameters) this.key).getCoeffSingular();
        short[] coeffScalar = ((RainbowPublicKeyParameters) this.key).getCoeffScalar();
        short[] sArr2 = new short[coeffQuadratic.length];
        int length = coeffSingular[0].length;
        for (int i = 0; i < coeffQuadratic.length; i++) {
            int i2 = 0;
            int i3 = i2;
            while (i2 < length) {
                int i4 = i3;
                for (i3 = i2; i3 < length; i3++) {
                    sArr2[i] = GF2Field.addElem(sArr2[i], GF2Field.multElem(coeffQuadratic[i][i4], GF2Field.multElem(sArr[i2], sArr[i3])));
                    i4++;
                }
                sArr2[i] = GF2Field.addElem(sArr2[i], GF2Field.multElem(coeffSingular[i][i2], sArr[i2]));
                i2++;
                i3 = i4;
            }
            sArr2[i] = GF2Field.addElem(sArr2[i], coeffScalar[i]);
        }
        return sArr2;
    }

    public byte[] generateSignature(byte[] bArr) {
        Layer[] layers = ((RainbowPrivateKeyParameters) this.key).getLayers();
        int length = layers.length;
        this.x = new short[((RainbowPrivateKeyParameters) this.key).getInvA2().length];
        byte[] bArr2 = new byte[layers[length - 1].getViNext()];
        short[] makeMessageRepresentative = makeMessageRepresentative(bArr);
        int i = 0;
        do {
            Object obj;
            try {
                short[] initSign = initSign(layers, makeMessageRepresentative);
                int i2 = 0;
                int i3 = i2;
                while (i2 < length) {
                    short[] sArr = new short[layers[i2].getOi()];
                    short[] sArr2 = new short[layers[i2].getOi()];
                    int i4 = i3;
                    for (i3 = 0; i3 < layers[i2].getOi(); i3++) {
                        sArr[i3] = initSign[i4];
                        i4++;
                    }
                    short[] solveEquation = this.cf.solveEquation(layers[i2].plugInVinegars(this.x), sArr);
                    if (solveEquation != null) {
                        for (int i5 = 0; i5 < solveEquation.length; i5++) {
                            this.x[layers[i2].getVi() + i5] = solveEquation[i5];
                        }
                        i2++;
                        i3 = i4;
                    } else {
                        throw new Exception("LES is not solveable!");
                    }
                }
                initSign = this.cf.multiplyMatrix(((RainbowPrivateKeyParameters) this.key).getInvA2(), this.cf.addVect(((RainbowPrivateKeyParameters) this.key).getB2(), this.x));
                for (i2 = 0; i2 < bArr2.length; i2++) {
                    bArr2[i2] = (byte) initSign[i2];
                }
                obj = 1;
            } catch (Exception e) {
                obj = null;
            }
            if (obj != null) {
                break;
            }
            i++;
        } while (i < 65536);
        if (i != 65536) {
            return bArr2;
        }
        throw new IllegalStateException("unable to generate signature - LES not solvable");
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        RainbowKeyParameters rainbowKeyParameters;
        if (!z) {
            rainbowKeyParameters = (RainbowPublicKeyParameters) cipherParameters;
        } else if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.random = parametersWithRandom.getRandom();
            this.key = (RainbowPrivateKeyParameters) parametersWithRandom.getParameters();
            this.signableDocumentLength = this.key.getDocLength();
        } else {
            this.random = new SecureRandom();
            rainbowKeyParameters = (RainbowPrivateKeyParameters) cipherParameters;
        }
        this.key = rainbowKeyParameters;
        this.signableDocumentLength = this.key.getDocLength();
    }

    public boolean verifySignature(byte[] bArr, byte[] bArr2) {
        int i;
        short[] sArr = new short[bArr2.length];
        for (i = 0; i < bArr2.length; i++) {
            sArr[i] = (short) (((short) bArr2[i]) & 255);
        }
        short[] makeMessageRepresentative = makeMessageRepresentative(bArr);
        short[] verifySignatureIntern = verifySignatureIntern(sArr);
        if (makeMessageRepresentative.length != verifySignatureIntern.length) {
            return false;
        }
        boolean z = true;
        for (i = 0; i < makeMessageRepresentative.length; i++) {
            z = z && makeMessageRepresentative[i] == verifySignatureIntern[i];
        }
        return z;
    }
}
