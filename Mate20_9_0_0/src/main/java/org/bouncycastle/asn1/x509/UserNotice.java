package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;

public class UserNotice extends ASN1Object {
    private final DisplayText explicitText;
    private final NoticeReference noticeRef;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:16:0x0051 in {2, 4, 9, 11, 12, 15, 18} preds:[]
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
    private UserNotice(org.bouncycastle.asn1.ASN1Sequence r5) {
        /*
        r4 = this;
        r4.<init>();
        r0 = r5.size();
        r1 = 1;
        r2 = 0;
        r3 = 2;
        if (r0 != r3) goto L_0x0021;
    L_0x000c:
        r0 = r5.getObjectAt(r2);
        r0 = org.bouncycastle.asn1.x509.NoticeReference.getInstance(r0);
        r4.noticeRef = r0;
        r5 = r5.getObjectAt(r1);
    L_0x001a:
        r5 = org.bouncycastle.asn1.x509.DisplayText.getInstance(r5);
        r4.explicitText = r5;
        return;
    L_0x0021:
        r0 = r5.size();
        r3 = 0;
        if (r0 != r1) goto L_0x0048;
    L_0x0028:
        r0 = r5.getObjectAt(r2);
        r0 = r0.toASN1Primitive();
        r0 = r0 instanceof org.bouncycastle.asn1.ASN1Sequence;
        if (r0 == 0) goto L_0x0041;
    L_0x0034:
        r5 = r5.getObjectAt(r2);
        r5 = org.bouncycastle.asn1.x509.NoticeReference.getInstance(r5);
        r4.noticeRef = r5;
    L_0x003e:
        r4.explicitText = r3;
        return;
    L_0x0041:
        r4.noticeRef = r3;
        r5 = r5.getObjectAt(r2);
        goto L_0x001a;
    L_0x0048:
        r0 = r5.size();
        if (r0 != 0) goto L_0x0052;
    L_0x004e:
        r4.noticeRef = r3;
        goto L_0x003e;
        return;
    L_0x0052:
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Bad sequence size: ";
        r1.append(r2);
        r5 = r5.size();
        r1.append(r5);
        r5 = r1.toString();
        r0.<init>(r5);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.UserNotice.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public UserNotice(NoticeReference noticeReference, String str) {
        this(noticeReference, new DisplayText(str));
    }

    public UserNotice(NoticeReference noticeReference, DisplayText displayText) {
        this.noticeRef = noticeReference;
        this.explicitText = displayText;
    }

    public static UserNotice getInstance(Object obj) {
        return obj instanceof UserNotice ? (UserNotice) obj : obj != null ? new UserNotice(ASN1Sequence.getInstance(obj)) : null;
    }

    public DisplayText getExplicitText() {
        return this.explicitText;
    }

    public NoticeReference getNoticeRef() {
        return this.noticeRef;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.noticeRef != null) {
            aSN1EncodableVector.add(this.noticeRef);
        }
        if (this.explicitText != null) {
            aSN1EncodableVector.add(this.explicitText);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
