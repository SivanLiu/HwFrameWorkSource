package org.bouncycastle.asn1;

import java.io.OutputStream;

public class BEROutputStream extends DEROutputStream {
    public BEROutputStream(OutputStream outputStream) {
        super(outputStream);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:11:0x001b in {2, 5, 7, 10, 13} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void writeObject(java.lang.Object r2) throws java.io.IOException {
        /*
        r1 = this;
        if (r2 != 0) goto L_0x0006;
        r1.writeNull();
        return;
        r0 = r2 instanceof org.bouncycastle.asn1.ASN1Primitive;
        if (r0 == 0) goto L_0x0010;
        r2 = (org.bouncycastle.asn1.ASN1Primitive) r2;
        r2.encode(r1);
        return;
        r0 = r2 instanceof org.bouncycastle.asn1.ASN1Encodable;
        if (r0 == 0) goto L_0x001c;
        r2 = (org.bouncycastle.asn1.ASN1Encodable) r2;
        r2 = r2.toASN1Primitive();
        goto L_0x000c;
        return;
        r2 = new java.io.IOException;
        r0 = "object not BEREncodable";
        r2.<init>(r0);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.BEROutputStream.writeObject(java.lang.Object):void");
    }
}
