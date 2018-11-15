package org.bouncycastle.crypto.engines;

import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.util.Pack;

public class ChaChaEngine extends Salsa20Engine {
    public ChaChaEngine(int i) {
        super(i);
    }

    public static void chachaCore(int i, int[] iArr, int[] iArr2) {
        int[] iArr3 = iArr;
        int[] iArr4 = iArr2;
        int i2 = 16;
        if (iArr3.length != 16) {
            throw new IllegalArgumentException();
        } else if (iArr4.length != 16) {
            throw new IllegalArgumentException();
        } else if (i % 2 == 0) {
            int i3 = 0;
            int i4 = iArr3[0];
            int i5 = iArr3[1];
            int i6 = iArr3[2];
            int i7 = iArr3[3];
            int i8 = iArr3[4];
            int i9 = iArr3[5];
            int i10 = iArr3[6];
            int i11 = 7;
            int i12 = iArr3[7];
            int i13 = 8;
            int i14 = iArr3[8];
            int i15 = iArr3[9];
            int i16 = iArr3[10];
            int i17 = iArr3[11];
            int i18 = 12;
            int i19 = iArr3[12];
            int i20 = iArr3[13];
            int i21 = iArr3[14];
            int i22 = iArr3[15];
            int i23 = i;
            while (i23 > 0) {
                i4 += i8;
                int rotl = Salsa20Engine.rotl(i19 ^ i4, i2);
                i14 += rotl;
                i8 = Salsa20Engine.rotl(i8 ^ i14, i18);
                i4 += i8;
                rotl = Salsa20Engine.rotl(rotl ^ i4, i13);
                i14 += rotl;
                i8 = Salsa20Engine.rotl(i8 ^ i14, i11);
                i5 += i9;
                int rotl2 = Salsa20Engine.rotl(i20 ^ i5, i2);
                i15 += rotl2;
                i9 = Salsa20Engine.rotl(i9 ^ i15, i18);
                i5 += i9;
                rotl2 = Salsa20Engine.rotl(rotl2 ^ i5, i13);
                i15 += rotl2;
                i9 = Salsa20Engine.rotl(i9 ^ i15, i11);
                i6 += i10;
                i3 = Salsa20Engine.rotl(i21 ^ i6, i2);
                i16 += i3;
                i2 = Salsa20Engine.rotl(i10 ^ i16, i18);
                i6 += i2;
                i3 = Salsa20Engine.rotl(i3 ^ i6, i13);
                i16 += i3;
                i2 = Salsa20Engine.rotl(i2 ^ i16, i11);
                i7 += i12;
                i11 = Salsa20Engine.rotl(i22 ^ i7, 16);
                i17 += i11;
                i13 = Salsa20Engine.rotl(i12 ^ i17, i18);
                i7 += i13;
                i11 = Salsa20Engine.rotl(i11 ^ i7, 8);
                i17 += i11;
                i18 = Salsa20Engine.rotl(i13 ^ i17, 7);
                i4 += i9;
                i13 = Salsa20Engine.rotl(i11 ^ i4, 16);
                i16 += i13;
                i11 = Salsa20Engine.rotl(i9 ^ i16, 12);
                i4 += i11;
                i22 = Salsa20Engine.rotl(i13 ^ i4, 8);
                i16 += i22;
                i9 = Salsa20Engine.rotl(i11 ^ i16, 7);
                i5 += i2;
                rotl = Salsa20Engine.rotl(rotl ^ i5, 16);
                i17 += rotl;
                i2 = Salsa20Engine.rotl(i2 ^ i17, 12);
                i5 += i2;
                i19 = Salsa20Engine.rotl(rotl ^ i5, 8);
                i17 += i19;
                i10 = Salsa20Engine.rotl(i2 ^ i17, 7);
                i6 += i18;
                i2 = Salsa20Engine.rotl(rotl2 ^ i6, 16);
                i14 += i2;
                rotl2 = Salsa20Engine.rotl(i18 ^ i14, 12);
                i6 += rotl2;
                i20 = Salsa20Engine.rotl(i2 ^ i6, 8);
                i14 += i20;
                i12 = Salsa20Engine.rotl(rotl2 ^ i14, 7);
                i7 += i8;
                i2 = Salsa20Engine.rotl(i3 ^ i7, 16);
                i15 += i2;
                rotl2 = Salsa20Engine.rotl(i8 ^ i15, 12);
                i7 += rotl2;
                i21 = Salsa20Engine.rotl(i2 ^ i7, 8);
                i15 += i21;
                i8 = Salsa20Engine.rotl(rotl2 ^ i15, 7);
                i23 -= 2;
                i2 = 16;
                i3 = 0;
                i18 = 12;
                i13 = 8;
                i11 = 7;
            }
            i23 = i3;
            iArr4[i23] = i4 + iArr3[i23];
            iArr4[1] = i5 + iArr3[1];
            iArr4[2] = i6 + iArr3[2];
            iArr4[3] = i7 + iArr3[3];
            iArr4[4] = i8 + iArr3[4];
            iArr4[5] = i9 + iArr3[5];
            iArr4[6] = i10 + iArr3[6];
            iArr4[7] = i12 + iArr3[7];
            iArr4[8] = i14 + iArr3[8];
            iArr4[9] = i15 + iArr3[9];
            iArr4[10] = i16 + iArr3[10];
            iArr4[11] = i17 + iArr3[11];
            iArr4[12] = i19 + iArr3[12];
            iArr4[13] = i20 + iArr3[13];
            iArr4[14] = i21 + iArr3[14];
            iArr4[15] = i22 + iArr3[15];
        } else {
            throw new IllegalArgumentException("Number of rounds must be even");
        }
    }

    protected void advanceCounter() {
        int[] iArr = this.engineState;
        int i = iArr[12] + 1;
        iArr[12] = i;
        if (i == 0) {
            iArr = this.engineState;
            iArr[13] = iArr[13] + 1;
        }
    }

    protected void advanceCounter(long j) {
        int i = (int) (j >>> 32);
        int i2 = (int) j;
        if (i > 0) {
            int[] iArr = this.engineState;
            iArr[13] = iArr[13] + i;
        }
        i = this.engineState[12];
        int[] iArr2 = this.engineState;
        iArr2[12] = iArr2[12] + i2;
        if (i != 0 && this.engineState[12] < i) {
            int[] iArr3 = this.engineState;
            iArr3[13] = iArr3[13] + 1;
        }
    }

    protected void generateKeyStream(byte[] bArr) {
        chachaCore(this.rounds, this.engineState, this.x);
        Pack.intToLittleEndian(this.x, bArr, 0);
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ChaCha");
        stringBuilder.append(this.rounds);
        return stringBuilder.toString();
    }

    protected long getCounter() {
        return (((long) this.engineState[13]) << 32) | (((long) this.engineState[12]) & BodyPartID.bodyIdMax);
    }

    protected void resetCounter() {
        int[] iArr = this.engineState;
        this.engineState[13] = 0;
        iArr[12] = 0;
    }

    protected void retreatCounter() {
        if (this.engineState[12] == 0 && this.engineState[13] == 0) {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }
        int[] iArr = this.engineState;
        int i = iArr[12] - 1;
        iArr[12] = i;
        if (i == -1) {
            iArr = this.engineState;
            iArr[13] = iArr[13] - 1;
        }
    }

    protected void retreatCounter(long j) {
        int i = (int) (j >>> 32);
        int i2 = (int) j;
        if (i != 0) {
            if ((((long) this.engineState[13]) & BodyPartID.bodyIdMax) >= (((long) i) & BodyPartID.bodyIdMax)) {
                int[] iArr = this.engineState;
                iArr[13] = iArr[13] - i;
            } else {
                throw new IllegalStateException("attempt to reduce counter past zero.");
            }
        }
        int[] iArr2;
        if ((((long) this.engineState[12]) & BodyPartID.bodyIdMax) >= (((long) i2) & BodyPartID.bodyIdMax)) {
            iArr2 = this.engineState;
            iArr2[12] = iArr2[12] - i2;
        } else if (this.engineState[13] != 0) {
            int[] iArr3 = this.engineState;
            iArr3[13] = iArr3[13] - 1;
            iArr2 = this.engineState;
            iArr2[12] = iArr2[12] - i2;
        } else {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }
    }

    protected void setKey(byte[] bArr, byte[] bArr2) {
        if (bArr != null) {
            if (bArr.length == 16 || bArr.length == 32) {
                packTauOrSigma(bArr.length, this.engineState, 0);
                Pack.littleEndianToInt(bArr, 0, this.engineState, 4, 4);
                Pack.littleEndianToInt(bArr, bArr.length - 16, this.engineState, 8, 4);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getAlgorithmName());
                stringBuilder.append(" requires 128 bit or 256 bit key");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        Pack.littleEndianToInt(bArr2, 0, this.engineState, 14, 2);
    }
}
