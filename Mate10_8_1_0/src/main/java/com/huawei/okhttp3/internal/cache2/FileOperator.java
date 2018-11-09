package com.huawei.okhttp3.internal.cache2;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

final class FileOperator {
    private static final int BUFFER_SIZE = 8192;
    private final byte[] byteArray = new byte[8192];
    private final ByteBuffer byteBuffer = ByteBuffer.wrap(this.byteArray);
    private final FileChannel fileChannel;

    public void read(long r7, com.huawei.okio.Buffer r9, long r10) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: java.lang.NullPointerException
	at jadx.core.dex.visitors.ssa.SSATransform.placePhi(SSATransform.java:82)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:50)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r6 = this;
        r4 = 0;
        r1 = (r10 > r4 ? 1 : (r10 == r4 ? 0 : -1));
        if (r1 >= 0) goto L_0x0021;
    L_0x0006:
        r1 = new java.lang.IndexOutOfBoundsException;
        r1.<init>();
        throw r1;
    L_0x000c:
        r1 = r6.byteBuffer;	 Catch:{ all -> 0x0042 }
        r0 = r1.position();	 Catch:{ all -> 0x0042 }
        r1 = r6.byteArray;	 Catch:{ all -> 0x0042 }
        r2 = 0;	 Catch:{ all -> 0x0042 }
        r9.write(r1, r2, r0);	 Catch:{ all -> 0x0042 }
        r2 = (long) r0;
        r7 = r7 + r2;
        r2 = (long) r0;
        r10 = r10 - r2;
        r1 = r6.byteBuffer;
        r1.clear();
    L_0x0021:
        r1 = (r10 > r4 ? 1 : (r10 == r4 ? 0 : -1));
        if (r1 <= 0) goto L_0x0049;
    L_0x0025:
        r1 = r6.byteBuffer;	 Catch:{ all -> 0x0042 }
        r2 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;	 Catch:{ all -> 0x0042 }
        r2 = java.lang.Math.min(r2, r10);	 Catch:{ all -> 0x0042 }
        r2 = (int) r2;	 Catch:{ all -> 0x0042 }
        r1.limit(r2);	 Catch:{ all -> 0x0042 }
        r1 = r6.fileChannel;	 Catch:{ all -> 0x0042 }
        r2 = r6.byteBuffer;	 Catch:{ all -> 0x0042 }
        r1 = r1.read(r2, r7);	 Catch:{ all -> 0x0042 }
        r2 = -1;	 Catch:{ all -> 0x0042 }
        if (r1 != r2) goto L_0x000c;	 Catch:{ all -> 0x0042 }
    L_0x003c:
        r1 = new java.io.EOFException;	 Catch:{ all -> 0x0042 }
        r1.<init>();	 Catch:{ all -> 0x0042 }
        throw r1;	 Catch:{ all -> 0x0042 }
    L_0x0042:
        r1 = move-exception;
        r2 = r6.byteBuffer;
        r2.clear();
        throw r1;
    L_0x0049:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.okhttp3.internal.cache2.FileOperator.read(long, com.huawei.okio.Buffer, long):void");
    }

    public void write(long r7, com.huawei.okio.Buffer r9, long r10) throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: java.lang.NullPointerException
	at jadx.core.dex.visitors.ssa.SSATransform.placePhi(SSATransform.java:82)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:50)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r6 = this;
        r4 = 0;
        r2 = (r10 > r4 ? 1 : (r10 == r4 ? 0 : -1));
        if (r2 < 0) goto L_0x000e;
    L_0x0006:
        r2 = r9.size();
        r2 = (r10 > r2 ? 1 : (r10 == r2 ? 0 : -1));
        if (r2 <= 0) goto L_0x001b;
    L_0x000e:
        r2 = new java.lang.IndexOutOfBoundsException;
        r2.<init>();
        throw r2;
    L_0x0014:
        r2 = (long) r1;
        r10 = r10 - r2;
        r2 = r6.byteBuffer;
        r2.clear();
    L_0x001b:
        r2 = (r10 > r4 ? 1 : (r10 == r4 ? 0 : -1));
        if (r2 <= 0) goto L_0x004b;
    L_0x001f:
        r2 = 8192; // 0x2000 float:1.14794E-41 double:4.0474E-320;
        r2 = java.lang.Math.min(r2, r10);	 Catch:{ all -> 0x0044 }
        r1 = (int) r2;	 Catch:{ all -> 0x0044 }
        r2 = r6.byteArray;	 Catch:{ all -> 0x0044 }
        r3 = 0;	 Catch:{ all -> 0x0044 }
        r9.read(r2, r3, r1);	 Catch:{ all -> 0x0044 }
        r2 = r6.byteBuffer;	 Catch:{ all -> 0x0044 }
        r2.limit(r1);	 Catch:{ all -> 0x0044 }
    L_0x0031:
        r2 = r6.fileChannel;	 Catch:{ all -> 0x0044 }
        r3 = r6.byteBuffer;	 Catch:{ all -> 0x0044 }
        r0 = r2.write(r3, r7);	 Catch:{ all -> 0x0044 }
        r2 = (long) r0;	 Catch:{ all -> 0x0044 }
        r7 = r7 + r2;	 Catch:{ all -> 0x0044 }
        r2 = r6.byteBuffer;	 Catch:{ all -> 0x0044 }
        r2 = r2.hasRemaining();	 Catch:{ all -> 0x0044 }
        if (r2 == 0) goto L_0x0014;
    L_0x0043:
        goto L_0x0031;
    L_0x0044:
        r2 = move-exception;
        r3 = r6.byteBuffer;
        r3.clear();
        throw r2;
    L_0x004b:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.okhttp3.internal.cache2.FileOperator.write(long, com.huawei.okio.Buffer, long):void");
    }

    public FileOperator(FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }
}
