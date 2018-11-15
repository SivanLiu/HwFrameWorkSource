package org.bouncycastle.crypto;

public abstract class StreamBlockCipher implements BlockCipher, StreamCipher {
    private final BlockCipher cipher;

    protected StreamBlockCipher(BlockCipher blockCipher) {
        this.cipher = blockCipher;
    }

    protected abstract byte calculateByte(byte b);

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException {
        int i4 = i + i2;
        if (i4 > bArr.length) {
            throw new DataLengthException("input buffer too small");
        } else if (i3 + i2 <= bArr2.length) {
            while (i < i4) {
                int i5 = i3 + 1;
                int i6 = i + 1;
                bArr2[i3] = calculateByte(bArr[i]);
                i3 = i5;
                i = i6;
            }
            return i2;
        } else {
            throw new OutputLengthException("output buffer too short");
        }
    }

    public final byte returnByte(byte b) {
        return calculateByte(b);
    }
}
