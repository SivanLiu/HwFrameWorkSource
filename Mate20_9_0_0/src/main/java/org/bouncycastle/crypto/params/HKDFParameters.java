package org.bouncycastle.crypto.params;

import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.util.Arrays;

public class HKDFParameters implements DerivationParameters {
    private final byte[] ikm;
    private final byte[] info;
    private final byte[] salt;
    private final boolean skipExpand;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:15:0x0028 in {6, 7, 8, 11, 13, 14, 17} preds:[]
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
    private HKDFParameters(byte[] r1, boolean r2, byte[] r3, byte[] r4) {
        /*
        r0 = this;
        r0.<init>();
        if (r1 == 0) goto L_0x0029;
        r1 = org.bouncycastle.util.Arrays.clone(r1);
        r0.ikm = r1;
        r0.skipExpand = r2;
        if (r3 == 0) goto L_0x0018;
        r1 = r3.length;
        if (r1 != 0) goto L_0x0013;
        goto L_0x0018;
        r1 = org.bouncycastle.util.Arrays.clone(r3);
        goto L_0x0019;
        r1 = 0;
        r0.salt = r1;
        if (r4 != 0) goto L_0x0023;
        r1 = 0;
        r1 = new byte[r1];
        r0.info = r1;
        return;
        r1 = org.bouncycastle.util.Arrays.clone(r4);
        goto L_0x0020;
        return;
        r1 = new java.lang.IllegalArgumentException;
        r2 = "IKM (input keying material) should not be null";
        r1.<init>(r2);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.params.HKDFParameters.<init>(byte[], boolean, byte[], byte[]):void");
    }

    public HKDFParameters(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        this(bArr, false, bArr2, bArr3);
    }

    public static HKDFParameters defaultParameters(byte[] bArr) {
        return new HKDFParameters(bArr, false, null, null);
    }

    public static HKDFParameters skipExtractParameters(byte[] bArr, byte[] bArr2) {
        return new HKDFParameters(bArr, true, null, bArr2);
    }

    public byte[] getIKM() {
        return Arrays.clone(this.ikm);
    }

    public byte[] getInfo() {
        return Arrays.clone(this.info);
    }

    public byte[] getSalt() {
        return Arrays.clone(this.salt);
    }

    public boolean skipExtract() {
        return this.skipExpand;
    }
}
