package org.bouncycastle.crypto.engines;

import org.bouncycastle.util.Pack;

public class XSalsa20Engine extends Salsa20Engine {
    public String getAlgorithmName() {
        return "XSalsa20";
    }

    protected int getNonceSize() {
        return 24;
    }

    protected void setKey(byte[] bArr, byte[] bArr2) {
        StringBuilder stringBuilder;
        if (bArr == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(getAlgorithmName());
            stringBuilder.append(" doesn't support re-init with null key");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (bArr.length == 32) {
            super.setKey(bArr, bArr2);
            Pack.littleEndianToInt(bArr2, 8, this.engineState, 8, 2);
            int[] iArr = new int[this.engineState.length];
            Salsa20Engine.salsaCore(20, this.engineState, iArr);
            this.engineState[1] = iArr[0] - this.engineState[0];
            this.engineState[2] = iArr[5] - this.engineState[5];
            this.engineState[3] = iArr[10] - this.engineState[10];
            this.engineState[4] = iArr[15] - this.engineState[15];
            this.engineState[11] = iArr[6] - this.engineState[6];
            this.engineState[12] = iArr[7] - this.engineState[7];
            this.engineState[13] = iArr[8] - this.engineState[8];
            this.engineState[14] = iArr[9] - this.engineState[9];
            Pack.littleEndianToInt(bArr2, 16, this.engineState, 6, 2);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(getAlgorithmName());
            stringBuilder.append(" requires a 256 bit key");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }
}
