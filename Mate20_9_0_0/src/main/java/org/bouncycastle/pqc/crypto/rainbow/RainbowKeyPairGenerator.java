package org.bouncycastle.pqc.crypto.rainbow;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.pqc.crypto.rainbow.util.ComputeInField;
import org.bouncycastle.pqc.crypto.rainbow.util.GF2Field;

public class RainbowKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private short[][] A1;
    private short[][] A1inv;
    private short[][] A2;
    private short[][] A2inv;
    private short[] b1;
    private short[] b2;
    private boolean initialized = false;
    private Layer[] layers;
    private int numOfLayers;
    private short[][] pub_quadratic;
    private short[] pub_scalar;
    private short[][] pub_singular;
    private RainbowKeyGenerationParameters rainbowParams;
    private SecureRandom sr;
    private int[] vi;

    private void compactPublicKey(short[][][] sArr) {
        int length = sArr.length;
        int length2 = sArr[0].length;
        this.pub_quadratic = (short[][]) Array.newInstance(short.class, new int[]{length, ((length2 + 1) * length2) / 2});
        for (int i = 0; i < length; i++) {
            int i2 = 0;
            int i3 = i2;
            while (i2 < length2) {
                int i4 = i3;
                for (i3 = i2; i3 < length2; i3++) {
                    if (i3 == i2) {
                        this.pub_quadratic[i][i4] = sArr[i][i2][i3];
                    } else {
                        this.pub_quadratic[i][i4] = GF2Field.addElem(sArr[i][i2][i3], sArr[i][i3][i2]);
                    }
                    i4++;
                }
                i2++;
                i3 = i4;
            }
        }
    }

    private void computePublicKey() {
        ComputeInField computeInField = new ComputeInField();
        int i = 0;
        int i2 = this.vi[this.vi.length - 1] - this.vi[0];
        int i3 = this.vi[this.vi.length - 1];
        short[][][] sArr = (short[][][]) Array.newInstance(short.class, new int[]{i2, i3, i3});
        this.pub_singular = (short[][]) Array.newInstance(short.class, new int[]{i2, i3});
        this.pub_scalar = new short[i2];
        short[] sArr2 = new short[i3];
        int i4 = 0;
        int i5 = i4;
        while (i4 < this.layers.length) {
            int i6;
            int i7;
            short[][][] coeffAlpha = this.layers[i4].getCoeffAlpha();
            short[][][] coeffBeta = this.layers[i4].getCoeffBeta();
            short[][] coeffGamma = this.layers[i4].getCoeffGamma();
            short[] coeffEta = this.layers[i4].getCoeffEta();
            int length = coeffAlpha[i].length;
            int length2 = coeffBeta[i].length;
            int i8 = i;
            while (i8 < length) {
                int i9;
                short[] sArr3;
                short[][][] sArr4;
                int i10 = i;
                while (i10 < length) {
                    while (i < length2) {
                        i6 = i2;
                        i7 = i3;
                        int i11 = i10 + length2;
                        short[] multVect = computeInField.multVect(coeffAlpha[i8][i10][i], this.A2[i11]);
                        i3 = i5 + i8;
                        i9 = i4;
                        sArr3 = coeffEta;
                        sArr[i3] = computeInField.addSquareMatrix(sArr[i3], computeInField.multVects(multVect, this.A2[i]));
                        this.pub_singular[i3] = computeInField.addVect(computeInField.multVect(this.b2[i], multVect), this.pub_singular[i3]);
                        this.pub_singular[i3] = computeInField.addVect(computeInField.multVect(this.b2[i11], computeInField.multVect(coeffAlpha[i8][i10][i], this.A2[i])), this.pub_singular[i3]);
                        sArr4 = coeffAlpha;
                        this.pub_scalar[i3] = GF2Field.addElem(this.pub_scalar[i3], GF2Field.multElem(GF2Field.multElem(coeffAlpha[i8][i10][i], this.b2[i11]), this.b2[i]));
                        i++;
                        i2 = i6;
                        i3 = i7;
                        i4 = i9;
                        coeffEta = sArr3;
                        coeffAlpha = sArr4;
                    }
                    i6 = i2;
                    i7 = i3;
                    i9 = i4;
                    sArr4 = coeffAlpha;
                    sArr3 = coeffEta;
                    i10++;
                    i = 0;
                }
                i6 = i2;
                i7 = i3;
                i9 = i4;
                sArr4 = coeffAlpha;
                sArr3 = coeffEta;
                for (i2 = 0; i2 < length2; i2++) {
                    for (i3 = 0; i3 < length2; i3++) {
                        short[] multVect2 = computeInField.multVect(coeffBeta[i8][i2][i3], this.A2[i2]);
                        i4 = i5 + i8;
                        sArr[i4] = computeInField.addSquareMatrix(sArr[i4], computeInField.multVects(multVect2, this.A2[i3]));
                        this.pub_singular[i4] = computeInField.addVect(computeInField.multVect(this.b2[i3], multVect2), this.pub_singular[i4]);
                        this.pub_singular[i4] = computeInField.addVect(computeInField.multVect(this.b2[i2], computeInField.multVect(coeffBeta[i8][i2][i3], this.A2[i3])), this.pub_singular[i4]);
                        this.pub_scalar[i4] = GF2Field.addElem(this.pub_scalar[i4], GF2Field.multElem(GF2Field.multElem(coeffBeta[i8][i2][i3], this.b2[i2]), this.b2[i3]));
                    }
                }
                for (i2 = 0; i2 < length2 + length; i2++) {
                    i4 = i5 + i8;
                    this.pub_singular[i4] = computeInField.addVect(computeInField.multVect(coeffGamma[i8][i2], this.A2[i2]), this.pub_singular[i4]);
                    this.pub_scalar[i4] = GF2Field.addElem(this.pub_scalar[i4], GF2Field.multElem(coeffGamma[i8][i2], this.b2[i2]));
                }
                i3 = i5 + i8;
                this.pub_scalar[i3] = GF2Field.addElem(this.pub_scalar[i3], sArr3[i8]);
                i8++;
                i2 = i6;
                i3 = i7;
                i4 = i9;
                coeffEta = sArr3;
                coeffAlpha = sArr4;
                i = 0;
            }
            i6 = i2;
            i7 = i3;
            i5 += length;
            i4++;
            i = 0;
        }
        short[][][] sArr5 = (short[][][]) Array.newInstance(short.class, new int[]{i2, i3, i3});
        short[][] sArr6 = (short[][]) Array.newInstance(short.class, new int[]{i2, i3});
        sArr2 = new short[i2];
        for (i5 = 0; i5 < i2; i5++) {
            for (int i12 = 0; i12 < this.A1.length; i12++) {
                sArr5[i5] = computeInField.addSquareMatrix(sArr5[i5], computeInField.multMatrix(this.A1[i5][i12], sArr[i12]));
                sArr6[i5] = computeInField.addVect(sArr6[i5], computeInField.multVect(this.A1[i5][i12], this.pub_singular[i12]));
                sArr2[i5] = GF2Field.addElem(sArr2[i5], GF2Field.multElem(this.A1[i5][i12], this.pub_scalar[i12]));
            }
            sArr2[i5] = GF2Field.addElem(sArr2[i5], this.b1[i5]);
        }
        this.pub_singular = sArr6;
        this.pub_scalar = sArr2;
        compactPublicKey(sArr5);
    }

    private void generateF() {
        this.layers = new Layer[this.numOfLayers];
        int i = 0;
        while (i < this.numOfLayers) {
            int i2 = i + 1;
            this.layers[i] = new Layer(this.vi[i], this.vi[i2], this.sr);
            i = i2;
        }
    }

    private void generateL1() {
        int i = 0;
        int i2 = this.vi[this.vi.length - 1] - this.vi[0];
        this.A1 = (short[][]) Array.newInstance(short.class, new int[]{i2, i2});
        this.A1inv = (short[][]) null;
        ComputeInField computeInField = new ComputeInField();
        while (this.A1inv == null) {
            for (int i3 = 0; i3 < i2; i3++) {
                for (int i4 = 0; i4 < i2; i4++) {
                    this.A1[i3][i4] = (short) (this.sr.nextInt() & 255);
                }
            }
            this.A1inv = computeInField.inverse(this.A1);
        }
        this.b1 = new short[i2];
        while (i < i2) {
            this.b1[i] = (short) (this.sr.nextInt() & 255);
            i++;
        }
    }

    private void generateL2() {
        int i;
        int i2 = this.vi[this.vi.length - 1];
        this.A2 = (short[][]) Array.newInstance(short.class, new int[]{i2, i2});
        this.A2inv = (short[][]) null;
        ComputeInField computeInField = new ComputeInField();
        while (true) {
            i = 0;
            if (this.A2inv != null) {
                break;
            }
            for (int i3 = 0; i3 < i2; i3++) {
                for (int i4 = 0; i4 < i2; i4++) {
                    this.A2[i3][i4] = (short) (this.sr.nextInt() & 255);
                }
            }
            this.A2inv = computeInField.inverse(this.A2);
        }
        this.b2 = new short[i2];
        while (i < i2) {
            this.b2[i] = (short) (this.sr.nextInt() & 255);
            i++;
        }
    }

    private void initializeDefault() {
        initialize(new RainbowKeyGenerationParameters(new SecureRandom(), new RainbowParameters()));
    }

    private void keygen() {
        generateL1();
        generateL2();
        generateF();
        computePublicKey();
    }

    public AsymmetricCipherKeyPair genKeyPair() {
        if (!this.initialized) {
            initializeDefault();
        }
        keygen();
        return new AsymmetricCipherKeyPair(new RainbowPublicKeyParameters(this.vi[this.vi.length - 1] - this.vi[0], this.pub_quadratic, this.pub_singular, this.pub_scalar), new RainbowPrivateKeyParameters(this.A1inv, this.b1, this.A2inv, this.b2, this.vi, this.layers));
    }

    public AsymmetricCipherKeyPair generateKeyPair() {
        return genKeyPair();
    }

    public void init(KeyGenerationParameters keyGenerationParameters) {
        initialize(keyGenerationParameters);
    }

    public void initialize(KeyGenerationParameters keyGenerationParameters) {
        this.rainbowParams = (RainbowKeyGenerationParameters) keyGenerationParameters;
        this.sr = this.rainbowParams.getRandom();
        this.vi = this.rainbowParams.getParameters().getVi();
        this.numOfLayers = this.rainbowParams.getParameters().getNumOfLayers();
        this.initialized = true;
    }
}
