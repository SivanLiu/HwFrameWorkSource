package org.bouncycastle.asn1.pkcs;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.util.Arrays;

public class MacData extends ASN1Object {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    DigestInfo digInfo;
    BigInteger iterationCount;
    byte[] salt;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0037 in {2, 4, 5} preds:[]
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
    private MacData(org.bouncycastle.asn1.ASN1Sequence r3) {
        /*
        r2 = this;
        r2.<init>();
        r0 = 0;
        r0 = r3.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.x509.DigestInfo.getInstance(r0);
        r2.digInfo = r0;
        r0 = 1;
        r0 = r3.getObjectAt(r0);
        r0 = (org.bouncycastle.asn1.ASN1OctetString) r0;
        r0 = r0.getOctets();
        r0 = org.bouncycastle.util.Arrays.clone(r0);
        r2.salt = r0;
        r0 = r3.size();
        r1 = 3;
        if (r0 != r1) goto L_0x0034;
    L_0x0026:
        r0 = 2;
        r3 = r3.getObjectAt(r0);
        r3 = (org.bouncycastle.asn1.ASN1Integer) r3;
        r3 = r3.getValue();
    L_0x0031:
        r2.iterationCount = r3;
        return;
    L_0x0034:
        r3 = ONE;
        goto L_0x0031;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.pkcs.MacData.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public MacData(DigestInfo digestInfo, byte[] bArr, int i) {
        this.digInfo = digestInfo;
        this.salt = Arrays.clone(bArr);
        this.iterationCount = BigInteger.valueOf((long) i);
    }

    public static MacData getInstance(Object obj) {
        return obj instanceof MacData ? (MacData) obj : obj != null ? new MacData(ASN1Sequence.getInstance(obj)) : null;
    }

    public BigInteger getIterationCount() {
        return this.iterationCount;
    }

    public DigestInfo getMac() {
        return this.digInfo;
    }

    public byte[] getSalt() {
        return Arrays.clone(this.salt);
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.digInfo);
        aSN1EncodableVector.add(new DEROctetString(this.salt));
        if (!this.iterationCount.equals(ONE)) {
            aSN1EncodableVector.add(new ASN1Integer(this.iterationCount));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
