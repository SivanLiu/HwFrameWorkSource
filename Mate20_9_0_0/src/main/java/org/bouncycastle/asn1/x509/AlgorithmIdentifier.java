package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;

public class AlgorithmIdentifier extends ASN1Object {
    private ASN1ObjectIdentifier algorithm;
    private ASN1Encodable parameters;

    public AlgorithmIdentifier(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        this.algorithm = aSN1ObjectIdentifier;
    }

    public AlgorithmIdentifier(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        this.algorithm = aSN1ObjectIdentifier;
        this.parameters = aSN1Encodable;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:10:0x002b in {6, 8, 9, 12} preds:[]
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
    private AlgorithmIdentifier(org.bouncycastle.asn1.ASN1Sequence r4) {
        /*
        r3 = this;
        r3.<init>();
        r0 = r4.size();
        r1 = 1;
        if (r0 < r1) goto L_0x002c;
        r0 = r4.size();
        r2 = 2;
        if (r0 > r2) goto L_0x002c;
        r0 = 0;
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1ObjectIdentifier.getInstance(r0);
        r3.algorithm = r0;
        r0 = r4.size();
        if (r0 != r2) goto L_0x0029;
        r4 = r4.getObjectAt(r1);
        r3.parameters = r4;
        return;
        r4 = 0;
        goto L_0x0026;
        return;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "Bad sequence size: ";
        r1.append(r2);
        r4 = r4.size();
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x509.AlgorithmIdentifier.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public static AlgorithmIdentifier getInstance(Object obj) {
        return obj instanceof AlgorithmIdentifier ? (AlgorithmIdentifier) obj : obj != null ? new AlgorithmIdentifier(ASN1Sequence.getInstance(obj)) : null;
    }

    public static AlgorithmIdentifier getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public ASN1ObjectIdentifier getAlgorithm() {
        return this.algorithm;
    }

    public ASN1Encodable getParameters() {
        return this.parameters;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.algorithm);
        if (this.parameters != null) {
            aSN1EncodableVector.add(this.parameters);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
