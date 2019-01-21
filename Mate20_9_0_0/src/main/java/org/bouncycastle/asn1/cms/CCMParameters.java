package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.Arrays;

public class CCMParameters extends ASN1Object {
    private int icvLen;
    private byte[] nonce;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0030 in {2, 4, 5} preds:[]
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
    private CCMParameters(org.bouncycastle.asn1.ASN1Sequence r3) {
        /*
        r2 = this;
        r2.<init>();
        r0 = 0;
        r0 = r3.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r0);
        r0 = r0.getOctets();
        r2.nonce = r0;
        r0 = r3.size();
        r1 = 2;
        if (r0 != r1) goto L_0x002d;
        r0 = 1;
        r3 = r3.getObjectAt(r0);
        r3 = org.bouncycastle.asn1.ASN1Integer.getInstance(r3);
        r3 = r3.getValue();
        r3 = r3.intValue();
        r2.icvLen = r3;
        return;
        r3 = 12;
        goto L_0x002a;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.cms.CCMParameters.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public CCMParameters(byte[] bArr, int i) {
        this.nonce = Arrays.clone(bArr);
        this.icvLen = i;
    }

    public static CCMParameters getInstance(Object obj) {
        return obj instanceof CCMParameters ? (CCMParameters) obj : obj != null ? new CCMParameters(ASN1Sequence.getInstance(obj)) : null;
    }

    public int getIcvLen() {
        return this.icvLen;
    }

    public byte[] getNonce() {
        return Arrays.clone(this.nonce);
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new DEROctetString(this.nonce));
        if (this.icvLen != 12) {
            aSN1EncodableVector.add(new ASN1Integer((long) this.icvLen));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
