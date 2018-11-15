package org.bouncycastle.crypto.generators;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.DigestDerivationFunction;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.util.Pack;

public class BaseKDFBytesGenerator implements DigestDerivationFunction {
    private int counterStart;
    private Digest digest;
    private byte[] iv;
    private byte[] shared;

    protected BaseKDFBytesGenerator(int i, Digest digest) {
        this.counterStart = i;
        this.digest = digest;
    }

    public int generateBytes(byte[] bArr, int i, int i2) throws DataLengthException, IllegalArgumentException {
        if (bArr.length - i2 >= i) {
            long j = (long) i2;
            int digestSize = this.digest.getDigestSize();
            if (j <= 8589934591L) {
                long j2 = (long) digestSize;
                int i3 = (int) (((j + j2) - 1) / j2);
                Object obj = new byte[this.digest.getDigestSize()];
                byte[] bArr2 = new byte[4];
                Pack.intToBigEndian(this.counterStart, bArr2, 0);
                int i4 = this.counterStart & -256;
                int i5 = i;
                for (i = 0; i < i3; i++) {
                    this.digest.update(this.shared, 0, this.shared.length);
                    this.digest.update(bArr2, 0, bArr2.length);
                    if (this.iv != null) {
                        this.digest.update(this.iv, 0, this.iv.length);
                    }
                    this.digest.doFinal(obj, 0);
                    if (i2 > digestSize) {
                        System.arraycopy(obj, 0, bArr, i5, digestSize);
                        i5 += digestSize;
                        i2 -= digestSize;
                    } else {
                        System.arraycopy(obj, 0, bArr, i5, i2);
                    }
                    byte b = (byte) (bArr2[3] + 1);
                    bArr2[3] = b;
                    if (b == (byte) 0) {
                        i4 += 256;
                        Pack.intToBigEndian(i4, bArr2, 0);
                    }
                }
                this.digest.reset();
                return (int) j;
            }
            throw new IllegalArgumentException("Output length too large");
        }
        throw new OutputLengthException("output buffer too small");
    }

    public Digest getDigest() {
        return this.digest;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0021 in {2, 4, 7, 10} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void init(org.bouncycastle.crypto.DerivationParameters r2) {
        /*
        r1 = this;
        r0 = r2 instanceof org.bouncycastle.crypto.params.KDFParameters;
        if (r0 == 0) goto L_0x0013;
    L_0x0004:
        r2 = (org.bouncycastle.crypto.params.KDFParameters) r2;
        r0 = r2.getSharedSecret();
        r1.shared = r0;
        r2 = r2.getIV();
    L_0x0010:
        r1.iv = r2;
        return;
    L_0x0013:
        r0 = r2 instanceof org.bouncycastle.crypto.params.ISO18033KDFParameters;
        if (r0 == 0) goto L_0x0022;
    L_0x0017:
        r2 = (org.bouncycastle.crypto.params.ISO18033KDFParameters) r2;
        r2 = r2.getSeed();
        r1.shared = r2;
        r2 = 0;
        goto L_0x0010;
        return;
    L_0x0022:
        r2 = new java.lang.IllegalArgumentException;
        r0 = "KDF parameters required for generator";
        r2.<init>(r0);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.generators.BaseKDFBytesGenerator.init(org.bouncycastle.crypto.DerivationParameters):void");
    }
}
