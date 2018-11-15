package org.bouncycastle.crypto.paddings;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.OutputLengthException;

public class PaddedBufferedBlockCipher extends BufferedBlockCipher {
    BlockCipherPadding padding;

    public PaddedBufferedBlockCipher(BlockCipher blockCipher) {
        this(blockCipher, new PKCS7Padding());
    }

    public PaddedBufferedBlockCipher(BlockCipher blockCipher, BlockCipherPadding blockCipherPadding) {
        this.cipher = blockCipher;
        this.padding = blockCipherPadding;
        this.buf = new byte[blockCipher.getBlockSize()];
        this.bufOff = 0;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:19:0x0062 in {6, 8, 9, 10, 12, 18, 22, 24} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public int doFinal(byte[] r6, int r7) throws org.bouncycastle.crypto.DataLengthException, java.lang.IllegalStateException, org.bouncycastle.crypto.InvalidCipherTextException {
        /*
        r5 = this;
        r0 = r5.cipher;
        r0 = r0.getBlockSize();
        r1 = r5.forEncryption;
        r2 = 0;
        if (r1 == 0) goto L_0x0043;
    L_0x000b:
        r1 = r5.bufOff;
        if (r1 != r0) goto L_0x002b;
    L_0x000f:
        r1 = 2;
        r1 = r1 * r0;
        r1 = r1 + r7;
        r0 = r6.length;
        if (r1 > r0) goto L_0x0020;
    L_0x0015:
        r0 = r5.cipher;
        r1 = r5.buf;
        r0 = r0.processBlock(r1, r2, r6, r7);
        r5.bufOff = r2;
        goto L_0x002c;
    L_0x0020:
        r5.reset();
        r6 = new org.bouncycastle.crypto.OutputLengthException;
        r7 = "output buffer too short";
        r6.<init>(r7);
        throw r6;
    L_0x002b:
        r0 = r2;
    L_0x002c:
        r1 = r5.padding;
        r3 = r5.buf;
        r4 = r5.bufOff;
        r1.addPadding(r3, r4);
        r1 = r5.cipher;
        r3 = r5.buf;
        r7 = r7 + r0;
        r6 = r1.processBlock(r3, r2, r6, r7);
        r0 = r0 + r6;
    L_0x003f:
        r5.reset();
        return r0;
    L_0x0043:
        r1 = r5.bufOff;
        if (r1 != r0) goto L_0x0068;
    L_0x0047:
        r0 = r5.cipher;
        r1 = r5.buf;
        r3 = r5.buf;
        r0 = r0.processBlock(r1, r2, r3, r2);
        r5.bufOff = r2;
        r1 = r5.padding;	 Catch:{ all -> 0x0063 }
        r3 = r5.buf;	 Catch:{ all -> 0x0063 }
        r1 = r1.padCount(r3);	 Catch:{ all -> 0x0063 }
        r0 = r0 - r1;	 Catch:{ all -> 0x0063 }
        r1 = r5.buf;	 Catch:{ all -> 0x0063 }
        java.lang.System.arraycopy(r1, r2, r6, r7, r0);	 Catch:{ all -> 0x0063 }
        goto L_0x003f;
        return r0;
    L_0x0063:
        r6 = move-exception;
        r5.reset();
        throw r6;
    L_0x0068:
        r5.reset();
        r6 = new org.bouncycastle.crypto.DataLengthException;
        r7 = "last block incomplete in decryption";
        r6.<init>(r7);
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher.doFinal(byte[], int):int");
    }

    public int getOutputSize(int i) {
        i += this.bufOff;
        int length = i % this.buf.length;
        if (length != 0) {
            i -= length;
            length = this.buf.length;
        } else if (!this.forEncryption) {
            return i;
        } else {
            length = this.buf.length;
        }
        return i + length;
    }

    public int getUpdateOutputSize(int i) {
        i += this.bufOff;
        int length = i % this.buf.length;
        return length == 0 ? Math.max(0, i - this.buf.length) : i - length;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0027 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void init(boolean r3, org.bouncycastle.crypto.CipherParameters r4) throws java.lang.IllegalArgumentException {
        /*
        r2 = this;
        r2.forEncryption = r3;
        r2.reset();
        r0 = r4 instanceof org.bouncycastle.crypto.params.ParametersWithRandom;
        if (r0 == 0) goto L_0x001e;
    L_0x0009:
        r4 = (org.bouncycastle.crypto.params.ParametersWithRandom) r4;
        r0 = r2.padding;
        r1 = r4.getRandom();
        r0.init(r1);
        r0 = r2.cipher;
        r4 = r4.getParameters();
    L_0x001a:
        r0.init(r3, r4);
        return;
    L_0x001e:
        r0 = r2.padding;
        r1 = 0;
        r0.init(r1);
        r0 = r2.cipher;
        goto L_0x001a;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher.init(boolean, org.bouncycastle.crypto.CipherParameters):void");
    }

    public int processByte(byte b, byte[] bArr, int i) throws DataLengthException, IllegalStateException {
        int processBlock;
        if (this.bufOff == this.buf.length) {
            processBlock = this.cipher.processBlock(this.buf, 0, bArr, i);
            this.bufOff = 0;
        } else {
            processBlock = 0;
        }
        byte[] bArr2 = this.buf;
        int i2 = this.bufOff;
        this.bufOff = i2 + 1;
        bArr2[i2] = b;
        return processBlock;
    }

    public int processBytes(byte[] bArr, int i, int i2, byte[] bArr2, int i3) throws DataLengthException, IllegalStateException {
        if (i2 >= 0) {
            int blockSize = getBlockSize();
            int updateOutputSize = getUpdateOutputSize(i2);
            if (updateOutputSize <= 0 || updateOutputSize + i3 <= bArr2.length) {
                updateOutputSize = this.buf.length - this.bufOff;
                int i4 = 0;
                if (i2 > updateOutputSize) {
                    System.arraycopy(bArr, i, this.buf, this.bufOff, updateOutputSize);
                    int processBlock = this.cipher.processBlock(this.buf, 0, bArr2, i3) + 0;
                    this.bufOff = 0;
                    i2 -= updateOutputSize;
                    i += updateOutputSize;
                    i4 = processBlock;
                    while (i2 > this.buf.length) {
                        i4 += this.cipher.processBlock(bArr, i, bArr2, i3 + i4);
                        i2 -= blockSize;
                        i += blockSize;
                    }
                }
                System.arraycopy(bArr, i, this.buf, this.bufOff, i2);
                this.bufOff += i2;
                return i4;
            }
            throw new OutputLengthException("output buffer too short");
        }
        throw new IllegalArgumentException("Can't have a negative input length!");
    }
}
