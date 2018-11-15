package org.bouncycastle.asn1.ocsp;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;

public class ServiceLocator extends ASN1Object {
    private final X500Name issuer;
    private final AuthorityInformationAccess locator;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0023 in {2, 4, 5} preds:[]
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
    private ServiceLocator(org.bouncycastle.asn1.ASN1Sequence r3) {
        /*
        r2 = this;
        r2.<init>();
        r0 = 0;
        r0 = r3.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.x500.X500Name.getInstance(r0);
        r2.issuer = r0;
        r0 = r3.size();
        r1 = 2;
        if (r0 != r1) goto L_0x0021;
    L_0x0015:
        r0 = 1;
        r3 = r3.getObjectAt(r0);
        r3 = org.bouncycastle.asn1.x509.AuthorityInformationAccess.getInstance(r3);
    L_0x001e:
        r2.locator = r3;
        return;
    L_0x0021:
        r3 = 0;
        goto L_0x001e;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.ocsp.ServiceLocator.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public static ServiceLocator getInstance(Object obj) {
        return obj instanceof ServiceLocator ? (ServiceLocator) obj : obj != null ? new ServiceLocator(ASN1Sequence.getInstance(obj)) : null;
    }

    public X500Name getIssuer() {
        return this.issuer;
    }

    public AuthorityInformationAccess getLocator() {
        return this.locator;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.issuer);
        if (this.locator != null) {
            aSN1EncodableVector.add(this.locator);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
