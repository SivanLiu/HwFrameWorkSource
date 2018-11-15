package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERUTF8String;

public class DisplayText extends ASN1Object implements ASN1Choice {
    public static final int CONTENT_TYPE_BMPSTRING = 1;
    public static final int CONTENT_TYPE_IA5STRING = 0;
    public static final int CONTENT_TYPE_UTF8STRING = 2;
    public static final int CONTENT_TYPE_VISIBLESTRING = 3;
    public static final int DISPLAY_TEXT_MAXIMUM_SIZE = 200;
    int contentType;
    ASN1String contents;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:12:0x0035 in {2, 5, 7, 8, 9, 10, 11} preds:[]
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
    public DisplayText(int r3, java.lang.String r4) {
        /*
        r2 = this;
        r2.<init>();
        r0 = r4.length();
        r1 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;
        if (r0 <= r1) goto L_0x0010;
    L_0x000b:
        r0 = 0;
        r4 = r4.substring(r0, r1);
    L_0x0010:
        r2.contentType = r3;
        switch(r3) {
            case 0: goto L_0x002f;
            case 1: goto L_0x0029;
            case 2: goto L_0x0023;
            case 3: goto L_0x001d;
            default: goto L_0x0015;
        };
    L_0x0015:
        r3 = new org.bouncycastle.asn1.DERUTF8String;
        r3.<init>(r4);
    L_0x001a:
        r2.contents = r3;
        return;
    L_0x001d:
        r3 = new org.bouncycastle.asn1.DERVisibleString;
        r3.<init>(r4);
        goto L_0x001a;
    L_0x0023:
        r3 = new org.bouncycastle.asn1.DERUTF8String;
        r3.<init>(r4);
        goto L_0x001a;
    L_0x0029:
        r3 = new org.bouncycastle.asn1.DERBMPString;
        r3.<init>(r4);
        goto L_0x001a;
    L_0x002f:
        r3 = new org.bouncycastle.asn1.DERIA5String;
        r3.<init>(r4);
        goto L_0x001a;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.DisplayText.<init>(int, java.lang.String):void");
    }

    public DisplayText(String str) {
        if (str.length() > DISPLAY_TEXT_MAXIMUM_SIZE) {
            str = str.substring(0, DISPLAY_TEXT_MAXIMUM_SIZE);
        }
        this.contentType = 2;
        this.contents = new DERUTF8String(str);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:14:0x001f in {2, 4, 7, 10, 13, 16} preds:[]
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
    private DisplayText(org.bouncycastle.asn1.ASN1String r2) {
        /*
        r1 = this;
        r1.<init>();
        r1.contents = r2;
        r0 = r2 instanceof org.bouncycastle.asn1.DERUTF8String;
        if (r0 == 0) goto L_0x000d;
    L_0x0009:
        r2 = 2;
    L_0x000a:
        r1.contentType = r2;
        return;
    L_0x000d:
        r0 = r2 instanceof org.bouncycastle.asn1.DERBMPString;
        if (r0 == 0) goto L_0x0013;
    L_0x0011:
        r2 = 1;
        goto L_0x000a;
    L_0x0013:
        r0 = r2 instanceof org.bouncycastle.asn1.DERIA5String;
        if (r0 == 0) goto L_0x0019;
    L_0x0017:
        r2 = 0;
        goto L_0x000a;
    L_0x0019:
        r2 = r2 instanceof org.bouncycastle.asn1.DERVisibleString;
        if (r2 == 0) goto L_0x0020;
    L_0x001d:
        r2 = 3;
        goto L_0x000a;
        return;
    L_0x0020:
        r2 = new java.lang.IllegalArgumentException;
        r0 = "unknown STRING type in DisplayText";
        r2.<init>(r0);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.DisplayText.<init>(org.bouncycastle.asn1.ASN1String):void");
    }

    public static DisplayText getInstance(Object obj) {
        if (obj instanceof ASN1String) {
            return new DisplayText((ASN1String) obj);
        }
        if (obj == null || (obj instanceof DisplayText)) {
            return (DisplayText) obj;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static DisplayText getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(aSN1TaggedObject.getObject());
    }

    public String getString() {
        return this.contents.getString();
    }

    public ASN1Primitive toASN1Primitive() {
        return (ASN1Primitive) this.contents;
    }
}
