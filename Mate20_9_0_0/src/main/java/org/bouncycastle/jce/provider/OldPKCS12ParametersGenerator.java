package org.bouncycastle.jce.provider;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

class OldPKCS12ParametersGenerator extends PBEParametersGenerator {
    public static final int IV_MATERIAL = 2;
    public static final int KEY_MATERIAL = 1;
    public static final int MAC_MATERIAL = 3;
    private Digest digest;
    private int u;
    private int v;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:11:0x0020 in {2, 4, 7, 10, 13} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
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
    public OldPKCS12ParametersGenerator(org.bouncycastle.crypto.Digest r4) {
        /*
        r3 = this;
        r3.<init>();
        r3.digest = r4;
        r0 = r4 instanceof org.bouncycastle.crypto.digests.MD5Digest;
        r1 = 64;
        if (r0 == 0) goto L_0x0012;
        r4 = 16;
        r3.u = r4;
        r3.v = r1;
        return;
        r0 = r4 instanceof org.bouncycastle.crypto.digests.SHA1Digest;
        r2 = 20;
        if (r0 == 0) goto L_0x001b;
        r3.u = r2;
        goto L_0x000f;
        r0 = r4 instanceof org.bouncycastle.crypto.digests.RIPEMD160Digest;
        if (r0 == 0) goto L_0x0021;
        goto L_0x0018;
        return;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Digest ";
        r1.append(r2);
        r4 = r4.getAlgorithmName();
        r1.append(r4);
        r4 = " unsupported";
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jce.provider.OldPKCS12ParametersGenerator.<init>(org.bouncycastle.crypto.Digest):void");
    }

    private void adjust(byte[] bArr, int i, byte[] bArr2) {
        int i2 = ((bArr2[bArr2.length - 1] & 255) + (bArr[(bArr2.length + i) - 1] & 255)) + 1;
        bArr[(bArr2.length + i) - 1] = (byte) i2;
        i2 >>>= 8;
        for (int length = bArr2.length - 2; length >= 0; length--) {
            int i3 = i + length;
            i2 += (bArr2[length] & 255) + (bArr[i3] & 255);
            bArr[i3] = (byte) i2;
            i2 >>>= 8;
        }
    }

    private byte[] generateDerivedKey(int i, int i2) {
        Object obj;
        int i3;
        Object obj2;
        byte[] bArr = new byte[this.v];
        byte[] bArr2 = new byte[i2];
        for (int i4 = 0; i4 != bArr.length; i4++) {
            bArr[i4] = (byte) i;
        }
        if (this.salt == null || this.salt.length == 0) {
            obj = new byte[0];
        } else {
            obj = new byte[(this.v * (((this.salt.length + this.v) - 1) / this.v))];
            for (i3 = 0; i3 != obj.length; i3++) {
                obj[i3] = this.salt[i3 % this.salt.length];
            }
        }
        if (this.password == null || this.password.length == 0) {
            obj2 = new byte[0];
        } else {
            obj2 = new byte[(this.v * (((this.password.length + this.v) - 1) / this.v))];
            for (int i5 = 0; i5 != obj2.length; i5++) {
                obj2[i5] = this.password[i5 % this.password.length];
            }
        }
        byte[] bArr3 = new byte[(obj.length + obj2.length)];
        System.arraycopy(obj, 0, bArr3, 0, obj.length);
        System.arraycopy(obj2, 0, bArr3, obj.length, obj2.length);
        byte[] bArr4 = new byte[this.v];
        i2 = ((i2 + this.u) - 1) / this.u;
        for (i3 = 1; i3 <= i2; i3++) {
            int i6;
            byte[] bArr5 = new byte[this.u];
            this.digest.update(bArr, 0, bArr.length);
            this.digest.update(bArr3, 0, bArr3.length);
            this.digest.doFinal(bArr5, 0);
            for (i6 = 1; i6 != this.iterationCount; i6++) {
                this.digest.update(bArr5, 0, bArr5.length);
                this.digest.doFinal(bArr5, 0);
            }
            for (i6 = 0; i6 != bArr4.length; i6++) {
                bArr4[i3] = bArr5[i6 % bArr5.length];
            }
            for (i6 = 0; i6 != bArr3.length / this.v; i6++) {
                adjust(bArr3, this.v * i6, bArr4);
            }
            if (i3 == i2) {
                i6 = i3 - 1;
                System.arraycopy(bArr5, 0, bArr2, this.u * i6, bArr2.length - (i6 * this.u));
            } else {
                System.arraycopy(bArr5, 0, bArr2, (i3 - 1) * this.u, bArr5.length);
            }
        }
        return bArr2;
    }

    public CipherParameters generateDerivedMacParameters(int i) {
        i /= 8;
        return new KeyParameter(generateDerivedKey(3, i), 0, i);
    }

    public CipherParameters generateDerivedParameters(int i) {
        i /= 8;
        return new KeyParameter(generateDerivedKey(1, i), 0, i);
    }

    public CipherParameters generateDerivedParameters(int i, int i2) {
        i /= 8;
        i2 /= 8;
        byte[] generateDerivedKey = generateDerivedKey(1, i);
        return new ParametersWithIV(new KeyParameter(generateDerivedKey, 0, i), generateDerivedKey(2, i2), 0, i2);
    }
}
