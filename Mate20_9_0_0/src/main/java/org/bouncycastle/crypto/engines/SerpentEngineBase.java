package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.params.KeyParameter;

public abstract class SerpentEngineBase implements BlockCipher {
    protected static final int BLOCK_SIZE = 16;
    static final int PHI = -1640531527;
    static final int ROUNDS = 32;
    protected int X0;
    protected int X1;
    protected int X2;
    protected int X3;
    protected boolean encrypting;
    protected int[] wKey;

    SerpentEngineBase() {
    }

    protected static int rotateLeft(int i, int i2) {
        return (i >>> (-i2)) | (i << i2);
    }

    protected static int rotateRight(int i, int i2) {
        return (i << (-i2)) | (i >>> i2);
    }

    protected final void LT() {
        int rotateLeft = rotateLeft(this.X0, 13);
        int rotateLeft2 = rotateLeft(this.X2, 3);
        int i = (this.X3 ^ rotateLeft2) ^ (rotateLeft << 3);
        this.X1 = rotateLeft((this.X1 ^ rotateLeft) ^ rotateLeft2, 1);
        this.X3 = rotateLeft(i, 7);
        this.X0 = rotateLeft((rotateLeft ^ this.X1) ^ this.X3, 5);
        this.X2 = rotateLeft((this.X3 ^ rotateLeft2) ^ (this.X1 << 7), 22);
    }

    protected abstract void decryptBlock(byte[] bArr, int i, byte[] bArr2, int i2);

    protected abstract void encryptBlock(byte[] bArr, int i, byte[] bArr2, int i2);

    public String getAlgorithmName() {
        return "Serpent";
    }

    public int getBlockSize() {
        return 16;
    }

    protected final void ib0(int i, int i2, int i3, int i4) {
        int i5 = ~i;
        i2 ^= i;
        int i6 = (i5 | i2) ^ i4;
        i3 ^= i6;
        this.X2 = i2 ^ i3;
        i2 = (i2 & i4) ^ i5;
        this.X1 = (this.X2 & i2) ^ i6;
        this.X3 = (i & i6) ^ (this.X1 | i3);
        this.X0 = this.X3 ^ (i2 ^ i3);
    }

    protected final void ib1(int i, int i2, int i3, int i4) {
        i4 ^= i2;
        i ^= i2 & i4;
        int i5 = i4 ^ i;
        this.X3 = i3 ^ i5;
        i2 ^= i4 & i;
        this.X1 = i ^ (this.X3 | i2);
        i = ~this.X1;
        i2 ^= this.X3;
        this.X0 = i ^ i2;
        this.X2 = (i | i2) ^ i5;
    }

    protected final void ib2(int i, int i2, int i3, int i4) {
        int i5 = i2 ^ i4;
        int i6 = ~i5;
        int i7 = i ^ i3;
        i3 ^= i5;
        this.X0 = (i2 & i3) ^ i7;
        this.X3 = (((i | i6) ^ i4) | i7) ^ i5;
        i = ~i3;
        i2 = this.X0 | this.X3;
        this.X1 = i ^ i2;
        this.X2 = (i & i4) ^ (i2 ^ i7);
    }

    protected final void ib3(int i, int i2, int i3, int i4) {
        int i5 = i | i2;
        int i6 = i2 ^ i3;
        i ^= i2 & i6;
        i2 = i3 ^ i;
        i3 = i4 | i;
        this.X0 = i6 ^ i3;
        i3 = (i3 | i6) ^ i4;
        this.X2 = i2 ^ i3;
        i2 = i5 ^ i3;
        this.X3 = i ^ (this.X0 & i2);
        this.X1 = this.X3 ^ (i2 ^ this.X0);
    }

    protected final void ib4(int i, int i2, int i3, int i4) {
        i2 ^= (i3 | i4) & i;
        i3 ^= i & i2;
        this.X1 = i4 ^ i3;
        i = ~i;
        this.X3 = (i3 & this.X1) ^ i2;
        i3 = (this.X1 | i) ^ i4;
        this.X0 = this.X3 ^ i3;
        this.X2 = (i ^ this.X1) ^ (i2 & i3);
    }

    protected final void ib5(int i, int i2, int i3, int i4) {
        int i5 = ~i3;
        int i6 = (i2 & i5) ^ i4;
        int i7 = i & i6;
        this.X3 = (i2 ^ i5) ^ i7;
        int i8 = this.X3 | i2;
        this.X1 = i6 ^ (i & i8);
        i4 |= i;
        this.X0 = (i5 ^ i8) ^ i4;
        this.X2 = ((i ^ i3) | i7) ^ (i2 & i4);
    }

    protected final void ib6(int i, int i2, int i3, int i4) {
        int i5 = ~i;
        i ^= i2;
        int i6 = i3 ^ i;
        i3 = (i3 | i5) ^ i4;
        this.X1 = i6 ^ i3;
        i ^= i6 & i3;
        this.X3 = i3 ^ (i2 | i);
        i2 |= this.X3;
        this.X0 = i ^ i2;
        this.X2 = (i4 & i5) ^ (i2 ^ i6);
    }

    protected final void ib7(int i, int i2, int i3, int i4) {
        int i5 = (i & i2) | i3;
        int i6 = (i | i2) & i4;
        this.X3 = i5 ^ i6;
        i2 ^= i6;
        this.X1 = ((this.X3 ^ (~i4)) | i2) ^ i;
        this.X0 = (i2 ^ i3) ^ (this.X1 | i4);
        this.X2 = ((i & this.X3) ^ this.X0) ^ (this.X1 ^ i5);
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof KeyParameter) {
            this.encrypting = z;
            this.wKey = makeWorkingKey(((KeyParameter) cipherParameters).getKey());
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter passed to ");
        stringBuilder.append(getAlgorithmName());
        stringBuilder.append(" init - ");
        stringBuilder.append(cipherParameters.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected final void inverseLT() {
        int rotateRight = (rotateRight(this.X2, 22) ^ this.X3) ^ (this.X1 << 7);
        int rotateRight2 = (rotateRight(this.X0, 5) ^ this.X1) ^ this.X3;
        int rotateRight3 = rotateRight(this.X3, 7);
        int rotateRight4 = rotateRight(this.X1, 1);
        this.X3 = (rotateRight3 ^ rotateRight) ^ (rotateRight2 << 3);
        this.X1 = (rotateRight4 ^ rotateRight2) ^ rotateRight;
        this.X2 = rotateRight(rotateRight, 3);
        this.X0 = rotateRight(rotateRight2, 13);
    }

    protected abstract int[] makeWorkingKey(byte[] bArr);

    public final int processBlock(byte[] bArr, int i, byte[] bArr2, int i2) {
        if (this.wKey == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getAlgorithmName());
            stringBuilder.append(" not initialised");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (i + 16 > bArr.length) {
            throw new DataLengthException("input buffer too short");
        } else if (i2 + 16 <= bArr2.length) {
            if (this.encrypting) {
                encryptBlock(bArr, i, bArr2, i2);
            } else {
                decryptBlock(bArr, i, bArr2, i2);
            }
            return 16;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public void reset() {
    }

    protected final void sb0(int i, int i2, int i3, int i4) {
        int i5 = i ^ i4;
        int i6 = i3 ^ i5;
        int i7 = i2 ^ i6;
        this.X3 = (i4 & i) ^ i7;
        i ^= i2 & i5;
        this.X2 = (i3 | i) ^ i7;
        i2 = this.X3 & (i6 ^ i);
        this.X1 = (~i6) ^ i2;
        this.X0 = (~i) ^ i2;
    }

    protected final void sb1(int i, int i2, int i3, int i4) {
        int i5 = (~i) ^ i2;
        i = (i | i5) ^ i3;
        this.X2 = i4 ^ i;
        i2 ^= i4 | i5;
        i3 = this.X2 ^ i5;
        this.X3 = (i & i2) ^ i3;
        i2 ^= i;
        this.X1 = this.X3 ^ i2;
        this.X0 = i ^ (i2 & i3);
    }

    protected final void sb2(int i, int i2, int i3, int i4) {
        int i5 = ~i;
        int i6 = i2 ^ i4;
        this.X0 = (i3 & i5) ^ i6;
        int i7 = i3 ^ i5;
        i2 &= i3 ^ this.X0;
        this.X3 = i7 ^ i2;
        this.X2 = i ^ ((i2 | i4) & (this.X0 | i7));
        this.X1 = (this.X3 ^ i6) ^ (this.X2 ^ (i4 | i5));
    }

    protected final void sb3(int i, int i2, int i3, int i4) {
        int i5 = i ^ i2;
        int i6 = i & i3;
        i |= i4;
        i3 ^= i4;
        i6 |= i5 & i;
        this.X2 = i3 ^ i6;
        i = (i ^ i2) ^ i6;
        this.X0 = i5 ^ (i3 & i);
        i5 = this.X2 & this.X0;
        this.X1 = i ^ i5;
        this.X3 = (i2 | i4) ^ (i3 ^ i5);
    }

    protected final void sb4(int i, int i2, int i3, int i4) {
        int i5 = i ^ i4;
        i3 ^= i4 & i5;
        i4 = i2 | i3;
        this.X3 = i5 ^ i4;
        i2 = ~i2;
        this.X0 = (i5 | i2) ^ i3;
        i2 ^= i5;
        this.X2 = (i4 & i2) ^ (this.X0 & i);
        this.X1 = (i ^ i3) ^ (i2 & this.X2);
    }

    protected final void sb5(int i, int i2, int i3, int i4) {
        int i5 = ~i;
        int i6 = i ^ i2;
        i ^= i4;
        this.X0 = (i3 ^ i5) ^ (i6 | i);
        i3 = this.X0 & i4;
        this.X1 = (this.X0 ^ i6) ^ i3;
        i ^= this.X0 | i5;
        this.X2 = (i6 | i3) ^ i;
        this.X3 = (i & this.X1) ^ (i2 ^ i3);
    }

    protected final void sb6(int i, int i2, int i3, int i4) {
        int i5 = ~i;
        i ^= i4;
        int i6 = i2 ^ i;
        i3 ^= i5 | i;
        this.X1 = i2 ^ i3;
        i = (i | this.X1) ^ i4;
        this.X2 = (i3 & i) ^ i6;
        i ^= i3;
        this.X0 = this.X2 ^ i;
        this.X3 = (i & i6) ^ (~i3);
    }

    protected final void sb7(int i, int i2, int i3, int i4) {
        int i5 = i2 ^ i3;
        i3 = (i3 & i5) ^ i4;
        int i6 = i ^ i3;
        this.X1 = i2 ^ ((i4 | i5) & i6);
        i2 = this.X1 | i3;
        this.X3 = (i & i6) ^ i5;
        i = i6 ^ i2;
        this.X2 = (this.X3 & i) ^ i3;
        this.X0 = (~i) ^ (this.X3 & this.X2);
    }
}
