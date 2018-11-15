package org.bouncycastle.pqc.crypto.rainbow;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import org.bouncycastle.pqc.crypto.rainbow.util.GF2Field;
import org.bouncycastle.pqc.crypto.rainbow.util.RainbowUtil;
import org.bouncycastle.util.Arrays;

public class Layer {
    private short[][][] coeff_alpha;
    private short[][][] coeff_beta;
    private short[] coeff_eta;
    private short[][] coeff_gamma;
    private int oi;
    private int vi;
    private int viNext;

    public Layer(byte b, byte b2, short[][][] sArr, short[][][] sArr2, short[][] sArr3, short[] sArr4) {
        this.vi = b & 255;
        this.viNext = b2 & 255;
        this.oi = this.viNext - this.vi;
        this.coeff_alpha = sArr;
        this.coeff_beta = sArr2;
        this.coeff_gamma = sArr3;
        this.coeff_eta = sArr4;
    }

    public Layer(int i, int i2, SecureRandom secureRandom) {
        int i3;
        int i4;
        int i5;
        this.vi = i;
        this.viNext = i2;
        this.oi = i2 - i;
        this.coeff_alpha = (short[][][]) Array.newInstance(short.class, new int[]{this.oi, this.oi, this.vi});
        this.coeff_beta = (short[][][]) Array.newInstance(short.class, new int[]{this.oi, this.vi, this.vi});
        this.coeff_gamma = (short[][]) Array.newInstance(short.class, new int[]{this.oi, this.viNext});
        this.coeff_eta = new short[this.oi];
        i = this.oi;
        i2 = 0;
        for (i3 = 0; i3 < i; i3++) {
            for (i4 = 0; i4 < this.oi; i4++) {
                for (i5 = 0; i5 < this.vi; i5++) {
                    this.coeff_alpha[i3][i4][i5] = (short) (secureRandom.nextInt() & 255);
                }
            }
        }
        for (i3 = 0; i3 < i; i3++) {
            for (i4 = 0; i4 < this.vi; i4++) {
                for (i5 = 0; i5 < this.vi; i5++) {
                    this.coeff_beta[i3][i4][i5] = (short) (secureRandom.nextInt() & 255);
                }
            }
        }
        for (i3 = 0; i3 < i; i3++) {
            for (i4 = 0; i4 < this.viNext; i4++) {
                this.coeff_gamma[i3][i4] = (short) (secureRandom.nextInt() & 255);
            }
        }
        while (i2 < i) {
            this.coeff_eta[i2] = (short) (secureRandom.nextInt() & 255);
            i2++;
        }
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj != null) {
            if (!(obj instanceof Layer)) {
                return false;
            }
            Layer layer = (Layer) obj;
            if (this.vi == layer.getVi() && this.viNext == layer.getViNext() && this.oi == layer.getOi() && RainbowUtil.equals(this.coeff_alpha, layer.getCoeffAlpha()) && RainbowUtil.equals(this.coeff_beta, layer.getCoeffBeta()) && RainbowUtil.equals(this.coeff_gamma, layer.getCoeffGamma()) && RainbowUtil.equals(this.coeff_eta, layer.getCoeffEta())) {
                z = true;
            }
        }
        return z;
    }

    public short[][][] getCoeffAlpha() {
        return this.coeff_alpha;
    }

    public short[][][] getCoeffBeta() {
        return this.coeff_beta;
    }

    public short[] getCoeffEta() {
        return this.coeff_eta;
    }

    public short[][] getCoeffGamma() {
        return this.coeff_gamma;
    }

    public int getOi() {
        return this.oi;
    }

    public int getVi() {
        return this.vi;
    }

    public int getViNext() {
        return this.viNext;
    }

    public int hashCode() {
        return (((((((((((this.vi * 37) + this.viNext) * 37) + this.oi) * 37) + Arrays.hashCode(this.coeff_alpha)) * 37) + Arrays.hashCode(this.coeff_beta)) * 37) + Arrays.hashCode(this.coeff_gamma)) * 37) + Arrays.hashCode(this.coeff_eta);
    }

    public short[][] plugInVinegars(short[] sArr) {
        int i;
        int i2;
        int i3;
        int i4;
        short[][] sArr2 = (short[][]) Array.newInstance(short.class, new int[]{this.oi, this.oi + 1});
        short[] sArr3 = new short[this.oi];
        int i5 = 0;
        for (i = 0; i < this.oi; i++) {
            for (i2 = 0; i2 < this.vi; i2++) {
                for (i3 = 0; i3 < this.vi; i3++) {
                    sArr3[i] = GF2Field.addElem(sArr3[i], GF2Field.multElem(GF2Field.multElem(this.coeff_beta[i][i2][i3], sArr[i2]), sArr[i3]));
                }
            }
        }
        for (i = 0; i < this.oi; i++) {
            for (i2 = 0; i2 < this.oi; i2++) {
                for (i3 = 0; i3 < this.vi; i3++) {
                    sArr2[i][i2] = GF2Field.addElem(sArr2[i][i2], GF2Field.multElem(this.coeff_alpha[i][i2][i3], sArr[i3]));
                }
            }
        }
        for (i = 0; i < this.oi; i++) {
            for (i2 = 0; i2 < this.vi; i2++) {
                sArr3[i] = GF2Field.addElem(sArr3[i], GF2Field.multElem(this.coeff_gamma[i][i2], sArr[i2]));
            }
        }
        for (i4 = 0; i4 < this.oi; i4++) {
            for (i = this.vi; i < this.viNext; i++) {
                sArr2[i4][i - this.vi] = GF2Field.addElem(this.coeff_gamma[i4][i], sArr2[i4][i - this.vi]);
            }
        }
        for (i4 = 0; i4 < this.oi; i4++) {
            sArr3[i4] = GF2Field.addElem(sArr3[i4], this.coeff_eta[i4]);
        }
        while (i5 < this.oi) {
            sArr2[i5][this.oi] = sArr3[i5];
            i5++;
        }
        return sArr2;
    }
}
