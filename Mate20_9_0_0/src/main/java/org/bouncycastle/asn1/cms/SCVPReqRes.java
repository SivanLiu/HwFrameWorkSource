package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class SCVPReqRes extends ASN1Object {
    private final ContentInfo request;
    private final ContentInfo response;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x002e in {2, 4, 5} preds:[]
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
    private SCVPReqRes(org.bouncycastle.asn1.ASN1Sequence r3) {
        /*
        r2 = this;
        r2.<init>();
        r0 = 0;
        r1 = r3.getObjectAt(r0);
        r1 = r1 instanceof org.bouncycastle.asn1.ASN1TaggedObject;
        if (r1 == 0) goto L_0x0026;
    L_0x000c:
        r0 = r3.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1TaggedObject.getInstance(r0);
        r1 = 1;
        r0 = org.bouncycastle.asn1.cms.ContentInfo.getInstance(r0, r1);
        r2.request = r0;
        r3 = r3.getObjectAt(r1);
    L_0x001f:
        r3 = org.bouncycastle.asn1.cms.ContentInfo.getInstance(r3);
        r2.response = r3;
        return;
    L_0x0026:
        r1 = 0;
        r2.request = r1;
        r3 = r3.getObjectAt(r0);
        goto L_0x001f;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.cms.SCVPReqRes.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public SCVPReqRes(ContentInfo contentInfo) {
        this.request = null;
        this.response = contentInfo;
    }

    public SCVPReqRes(ContentInfo contentInfo, ContentInfo contentInfo2) {
        this.request = contentInfo;
        this.response = contentInfo2;
    }

    public static SCVPReqRes getInstance(Object obj) {
        return obj instanceof SCVPReqRes ? (SCVPReqRes) obj : obj != null ? new SCVPReqRes(ASN1Sequence.getInstance(obj)) : null;
    }

    public ContentInfo getRequest() {
        return this.request;
    }

    public ContentInfo getResponse() {
        return this.response;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.request != null) {
            aSN1EncodableVector.add(new DERTaggedObject(true, 0, this.request));
        }
        aSN1EncodableVector.add(this.response);
        return new DERSequence(aSN1EncodableVector);
    }
}
