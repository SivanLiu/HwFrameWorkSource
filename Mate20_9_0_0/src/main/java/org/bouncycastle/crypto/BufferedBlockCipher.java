package org.bouncycastle.crypto;

public class BufferedBlockCipher {
    protected byte[] buf;
    protected int bufOff;
    protected BlockCipher cipher;
    protected boolean forEncryption;
    protected boolean partialBlockOkay;
    protected boolean pgpCFB;

    protected BufferedBlockCipher() {
    }

    public BufferedBlockCipher(BlockCipher blockCipher) {
        this.cipher = blockCipher;
        this.buf = new byte[blockCipher.getBlockSize()];
        boolean z = false;
        this.bufOff = 0;
        String algorithmName = blockCipher.getAlgorithmName();
        int indexOf = algorithmName.indexOf(47) + 1;
        boolean z2 = indexOf > 0 && algorithmName.startsWith("PGP", indexOf);
        this.pgpCFB = z2;
        if (this.pgpCFB || (blockCipher instanceof StreamCipher)) {
            this.partialBlockOkay = true;
            return;
        }
        if (indexOf > 0 && algorithmName.startsWith("OpenPGP", indexOf)) {
            z = true;
        }
        this.partialBlockOkay = z;
    }

    public int doFinal(byte[] bArr, int i) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
        try {
            if (this.bufOff + i <= bArr.length) {
                int i2;
                if (this.bufOff == 0) {
                    i2 = 0;
                } else if (this.partialBlockOkay) {
                    this.cipher.processBlock(this.buf, 0, this.buf, 0);
                    i2 = this.bufOff;
                    this.bufOff = 0;
                    System.arraycopy(this.buf, 0, bArr, i, i2);
                } else {
                    throw new DataLengthException("data not block size aligned");
                }
                reset();
                return i2;
            }
            throw new OutputLengthException("output buffer too short for doFinal()");
        } catch (Throwable th) {
            reset();
        }
    }

    public int getBlockSize() {
        return this.cipher.getBlockSize();
    }

    public int getOutputSize(int i) {
        return i + this.bufOff;
    }

    public BlockCipher getUnderlyingCipher() {
        return this.cipher;
    }

    public int getUpdateOutputSize(int i) {
        int length;
        i += this.bufOff;
        if (!this.pgpCFB) {
            length = this.buf.length;
        } else if (this.forEncryption) {
            length = (i % this.buf.length) - (this.cipher.getBlockSize() + 2);
            return i - length;
        } else {
            length = this.buf.length;
        }
        length = i % length;
        return i - length;
    }

    public void init(boolean z, CipherParameters cipherParameters) throws IllegalArgumentException {
        this.forEncryption = z;
        reset();
        this.cipher.init(z, cipherParameters);
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException, IllegalStateException {
        byte[] bArr2 = this.buf;
        int i2 = this.bufOff;
        this.bufOff = i2 + 1;
        bArr2[i2] = b;
        if (this.bufOff != this.buf.length) {
            return 0;
        }
        int processBlock = this.cipher.processBlock(this.buf, 0, bArr, i);
        this.bufOff = 0;
        return processBlock;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException, IllegalStateException {
        if (i2 >= 0) {
            int blockSize = getBlockSize();
            int updateOutputSize = getUpdateOutputSize(i2);
            if (updateOutputSize <= 0 || updateOutputSize + i3 <= bArr2.length) {
                int processBlock;
                updateOutputSize = this.buf.length - this.bufOff;
                if (i2 > updateOutputSize) {
                    System.arraycopy(bArr, i, this.buf, this.bufOff, updateOutputSize);
                    processBlock = this.cipher.processBlock(this.buf, 0, bArr2, i3) + 0;
                    this.bufOff = 0;
                    i2 -= updateOutputSize;
                    i += updateOutputSize;
                    while (i2 > this.buf.length) {
                        processBlock += this.cipher.processBlock(bArr, i, bArr2, i3 + processBlock);
                        i2 -= blockSize;
                        i += blockSize;
                    }
                } else {
                    processBlock = 0;
                }
                System.arraycopy(bArr, i, this.buf, this.bufOff, i2);
                this.bufOff += i2;
                if (this.bufOff != this.buf.length) {
                    return processBlock;
                }
                processBlock += this.cipher.processBlock(this.buf, 0, bArr2, i3 + processBlock);
                this.bufOff = 0;
                return processBlock;
            }
            throw new OutputLengthException("output buffer too short");
        }
        throw new IllegalArgumentException("Can't have a negative input length!");
    }

    public void reset() {
        for (int i = 0; i < this.buf.length; i++) {
            this.buf[i] = (byte) 0;
        }
        this.bufOff = 0;
        this.cipher.reset();
    }
}
