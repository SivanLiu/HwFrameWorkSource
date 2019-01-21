package org.bouncycastle.crypto.engines;

public class DESedeEngine extends DESEngine {
    protected static final int BLOCK_SIZE = 8;
    private boolean forEncryption;
    private int[] workingKey1 = null;
    private int[] workingKey2 = null;
    private int[] workingKey3 = null;

    public String getAlgorithmName() {
        return "DESede";
    }

    public int getBlockSize() {
        return 8;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:15:0x004f in {6, 8, 11, 13, 14, 17} preds:[]
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
    public void init(boolean r7, org.bouncycastle.crypto.CipherParameters r8) {
        /*
        r6 = this;
        r0 = r8 instanceof org.bouncycastle.crypto.params.KeyParameter;
        if (r0 == 0) goto L_0x0050;
        r8 = (org.bouncycastle.crypto.params.KeyParameter) r8;
        r8 = r8.getKey();
        r0 = r8.length;
        r1 = 16;
        r2 = 24;
        if (r0 == r2) goto L_0x001d;
        r0 = r8.length;
        if (r0 != r1) goto L_0x0015;
        goto L_0x001d;
        r7 = new java.lang.IllegalArgumentException;
        r8 = "key size must be 16 or 24 bytes.";
        r7.<init>(r8);
        throw r7;
        r6.forEncryption = r7;
        r0 = 8;
        r3 = new byte[r0];
        r4 = r3.length;
        r5 = 0;
        java.lang.System.arraycopy(r8, r5, r3, r5, r4);
        r3 = r6.generateWorkingKey(r7, r3);
        r6.workingKey1 = r3;
        r3 = new byte[r0];
        r4 = r3.length;
        java.lang.System.arraycopy(r8, r0, r3, r5, r4);
        r4 = r7 ^ 1;
        r3 = r6.generateWorkingKey(r4, r3);
        r6.workingKey2 = r3;
        r3 = r8.length;
        if (r3 != r2) goto L_0x004c;
        r0 = new byte[r0];
        r2 = r0.length;
        java.lang.System.arraycopy(r8, r1, r0, r5, r2);
        r7 = r6.generateWorkingKey(r7, r0);
        r6.workingKey3 = r7;
        return;
        r7 = r6.workingKey1;
        goto L_0x0049;
        return;
        r7 = new java.lang.IllegalArgumentException;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "invalid parameter passed to DESede init - ";
        r0.append(r1);
        r8 = r8.getClass();
        r8 = r8.getName();
        r0.append(r8);
        r8 = r0.toString();
        r7.<init>(r8);
        throw r7;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.engines.DESedeEngine.init(boolean, org.bouncycastle.crypto.CipherParameters):void");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:12:0x0043 in {8, 10, 11, 14, 16, 18} preds:[]
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
    public int processBlock(byte[] r9, int r10, byte[] r11, int r12) {
        /*
        r8 = this;
        r0 = r8.workingKey1;
        if (r0 == 0) goto L_0x0054;
        r0 = r10 + 8;
        r1 = r9.length;
        if (r0 > r1) goto L_0x004c;
        r0 = r12 + 8;
        r1 = r11.length;
        if (r0 > r1) goto L_0x0044;
        r0 = 8;
        r7 = new byte[r0];
        r1 = r8.forEncryption;
        if (r1 == 0) goto L_0x002f;
        r2 = r8.workingKey1;
        r6 = 0;
        r1 = r8;
        r3 = r9;
        r4 = r10;
        r5 = r7;
        r1.desFunc(r2, r3, r4, r5, r6);
        r2 = r8.workingKey2;
        r4 = 0;
        r3 = r7;
        r1.desFunc(r2, r3, r4, r5, r6);
        r2 = r8.workingKey3;
        r5 = r11;
        r6 = r12;
        r1.desFunc(r2, r3, r4, r5, r6);
        return r0;
        r2 = r8.workingKey3;
        r6 = 0;
        r1 = r8;
        r3 = r9;
        r4 = r10;
        r5 = r7;
        r1.desFunc(r2, r3, r4, r5, r6);
        r2 = r8.workingKey2;
        r4 = 0;
        r3 = r7;
        r1.desFunc(r2, r3, r4, r5, r6);
        r2 = r8.workingKey1;
        goto L_0x0029;
        return r0;
        r9 = new org.bouncycastle.crypto.OutputLengthException;
        r10 = "output buffer too short";
        r9.<init>(r10);
        throw r9;
        r9 = new org.bouncycastle.crypto.DataLengthException;
        r10 = "input buffer too short";
        r9.<init>(r10);
        throw r9;
        r9 = new java.lang.IllegalStateException;
        r10 = "DESede engine not initialised";
        r9.<init>(r10);
        throw r9;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.engines.DESedeEngine.processBlock(byte[], int, byte[], int):int");
    }

    public void reset() {
    }
}
