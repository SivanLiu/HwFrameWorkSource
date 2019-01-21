package org.bouncycastle.asn1.misc;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.Arrays;

public class IDEACBCPar extends ASN1Object {
    ASN1OctetString iv;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0016 in {2, 4, 5} preds:[]
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
    public IDEACBCPar(org.bouncycastle.asn1.ASN1Sequence r3) {
        /*
        r2 = this;
        r2.<init>();
        r0 = r3.size();
        r1 = 1;
        if (r0 != r1) goto L_0x0014;
        r0 = 0;
        r3 = r3.getObjectAt(r0);
        r3 = (org.bouncycastle.asn1.ASN1OctetString) r3;
        r2.iv = r3;
        return;
        r3 = 0;
        goto L_0x0011;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.misc.IDEACBCPar.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public IDEACBCPar(byte[] bArr) {
        this.iv = new DEROctetString(bArr);
    }

    public static IDEACBCPar getInstance(Object obj) {
        return obj instanceof IDEACBCPar ? (IDEACBCPar) obj : obj != null ? new IDEACBCPar(ASN1Sequence.getInstance(obj)) : null;
    }

    public byte[] getIV() {
        return this.iv != null ? Arrays.clone(this.iv.getOctets()) : null;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.iv != null) {
            aSN1EncodableVector.add(this.iv);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
