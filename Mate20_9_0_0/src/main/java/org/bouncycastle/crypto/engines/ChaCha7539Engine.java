package org.bouncycastle.crypto.engines;

import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.util.Pack;

public class ChaCha7539Engine extends Salsa20Engine {
    protected void advanceCounter() {
        int[] iArr = this.engineState;
        int i = iArr[12] + 1;
        iArr[12] = i;
        if (i == 0) {
            throw new IllegalStateException("attempt to increase counter past 2^32.");
        }
    }

    protected void advanceCounter(long j) {
        int i = (int) (j >>> 32);
        int i2 = (int) j;
        if (i <= 0) {
            int i3 = this.engineState[12];
            int[] iArr = this.engineState;
            iArr[12] = iArr[12] + i2;
            if (i3 != 0 && this.engineState[12] < i3) {
                throw new IllegalStateException("attempt to increase counter past 2^32.");
            }
            return;
        }
        throw new IllegalStateException("attempt to increase counter past 2^32.");
    }

    protected void generateKeyStream(byte[] bArr) {
        ChaChaEngine.chachaCore(this.rounds, this.engineState, this.x);
        Pack.intToLittleEndian(this.x, bArr, 0);
    }

    public String getAlgorithmName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ChaCha7539-");
        stringBuilder.append(this.rounds);
        return stringBuilder.toString();
    }

    protected long getCounter() {
        return ((long) this.engineState[12]) & BodyPartID.bodyIdMax;
    }

    protected int getNonceSize() {
        return 12;
    }

    protected void resetCounter() {
        this.engineState[12] = 0;
    }

    protected void retreatCounter() {
        if (this.engineState[12] != 0) {
            int[] iArr = this.engineState;
            iArr[12] = iArr[12] - 1;
            return;
        }
        throw new IllegalStateException("attempt to reduce counter past zero.");
    }

    protected void retreatCounter(long j) {
        int i = (int) (j >>> 32);
        int i2 = (int) j;
        if (i != 0) {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        } else if ((((long) this.engineState[12]) & BodyPartID.bodyIdMax) >= (BodyPartID.bodyIdMax & ((long) i2))) {
            int[] iArr = this.engineState;
            iArr[12] = iArr[12] - i2;
        } else {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }
    }

    protected void setKey(byte[] bArr, byte[] bArr2) {
        if (bArr != null) {
            if (bArr.length == 32) {
                packTauOrSigma(bArr.length, this.engineState, 0);
                Pack.littleEndianToInt(bArr, 0, this.engineState, 4, 8);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getAlgorithmName());
                stringBuilder.append(" requires 256 bit key");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        Pack.littleEndianToInt(bArr2, 0, this.engineState, 13, 3);
    }
}
