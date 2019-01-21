package org.bouncycastle.asn1.pkcs;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

public class RC2CBCParameter extends ASN1Object {
    ASN1OctetString iv;
    ASN1Integer version;

    public RC2CBCParameter(int i, byte[] bArr) {
        this.version = new ASN1Integer((long) i);
        this.iv = new DEROctetString(bArr);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0024 in {2, 4, 5} preds:[]
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
    private RC2CBCParameter(org.bouncycastle.asn1.ASN1Sequence r4) {
        /*
        r3 = this;
        r3.<init>();
        r0 = r4.size();
        r1 = 0;
        r2 = 1;
        if (r0 != r2) goto L_0x0017;
        r0 = 0;
        r3.version = r0;
        r4 = r4.getObjectAt(r1);
        r4 = (org.bouncycastle.asn1.ASN1OctetString) r4;
        r3.iv = r4;
        return;
        r0 = r4.getObjectAt(r1);
        r0 = (org.bouncycastle.asn1.ASN1Integer) r0;
        r3.version = r0;
        r4 = r4.getObjectAt(r2);
        goto L_0x0012;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.pkcs.RC2CBCParameter.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public RC2CBCParameter(byte[] bArr) {
        this.version = null;
        this.iv = new DEROctetString(bArr);
    }

    public static RC2CBCParameter getInstance(Object obj) {
        return obj instanceof RC2CBCParameter ? (RC2CBCParameter) obj : obj != null ? new RC2CBCParameter(ASN1Sequence.getInstance(obj)) : null;
    }

    public byte[] getIV() {
        return this.iv.getOctets();
    }

    public BigInteger getRC2ParameterVersion() {
        return this.version == null ? null : this.version.getValue();
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.version != null) {
            aSN1EncodableVector.add(this.version);
        }
        aSN1EncodableVector.add(this.iv);
        return new DERSequence(aSN1EncodableVector);
    }
}
