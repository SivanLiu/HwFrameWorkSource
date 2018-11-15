package org.bouncycastle.asn1.cms.ecc;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.util.Arrays;

public class ECCCMSSharedInfo extends ASN1Object {
    private final byte[] entityUInfo;
    private final AlgorithmIdentifier keyInfo;
    private final byte[] suppPubInfo;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x003f in {2, 4, 5} preds:[]
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
    private ECCCMSSharedInfo(org.bouncycastle.asn1.ASN1Sequence r4) {
        /*
        r3 = this;
        r3.<init>();
        r0 = 0;
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.x509.AlgorithmIdentifier.getInstance(r0);
        r3.keyInfo = r0;
        r0 = r4.size();
        r1 = 2;
        r2 = 1;
        if (r0 != r1) goto L_0x002a;
    L_0x0016:
        r0 = 0;
        r3.entityUInfo = r0;
        r4 = r4.getObjectAt(r2);
    L_0x001d:
        r4 = (org.bouncycastle.asn1.ASN1TaggedObject) r4;
        r4 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r4, r2);
        r4 = r4.getOctets();
        r3.suppPubInfo = r4;
        return;
    L_0x002a:
        r0 = r4.getObjectAt(r2);
        r0 = (org.bouncycastle.asn1.ASN1TaggedObject) r0;
        r0 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r0, r2);
        r0 = r0.getOctets();
        r3.entityUInfo = r0;
        r4 = r4.getObjectAt(r1);
        goto L_0x001d;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.cms.ecc.ECCCMSSharedInfo.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public ECCCMSSharedInfo(AlgorithmIdentifier algorithmIdentifier, byte[] bArr) {
        this.keyInfo = algorithmIdentifier;
        this.entityUInfo = null;
        this.suppPubInfo = Arrays.clone(bArr);
    }

    public ECCCMSSharedInfo(AlgorithmIdentifier algorithmIdentifier, byte[] bArr, byte[] bArr2) {
        this.keyInfo = algorithmIdentifier;
        this.entityUInfo = Arrays.clone(bArr);
        this.suppPubInfo = Arrays.clone(bArr2);
    }

    public static ECCCMSSharedInfo getInstance(Object obj) {
        return obj instanceof ECCCMSSharedInfo ? (ECCCMSSharedInfo) obj : obj != null ? new ECCCMSSharedInfo(ASN1Sequence.getInstance(obj)) : null;
    }

    public static ECCCMSSharedInfo getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.keyInfo);
        if (this.entityUInfo != null) {
            aSN1EncodableVector.add(new DERTaggedObject(true, 0, new DEROctetString(this.entityUInfo)));
        }
        aSN1EncodableVector.add(new DERTaggedObject(true, 2, new DEROctetString(this.suppPubInfo)));
        return new DERSequence(aSN1EncodableVector);
    }
}
