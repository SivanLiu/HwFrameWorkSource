package org.bouncycastle.asn1.misc;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.Arrays;

public class ScryptParams extends ASN1Object {
    private final BigInteger blockSize;
    private final BigInteger costParameter;
    private final BigInteger keyLength;
    private final BigInteger parallelizationParameter;
    private final byte[] salt;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:13:0x0084 in {4, 6, 9, 11, 12} preds:[]
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
    private ScryptParams(org.bouncycastle.asn1.ASN1Sequence r4) {
        /*
        r3 = this;
        r3.<init>();
        r0 = r4.size();
        r1 = 5;
        r2 = 4;
        if (r0 == r2) goto L_0x002d;
        r0 = r4.size();
        if (r0 != r1) goto L_0x0012;
        goto L_0x002d;
        r0 = new java.lang.IllegalArgumentException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "invalid sequence: size = ";
        r1.append(r2);
        r4 = r4.size();
        r1.append(r4);
        r4 = r1.toString();
        r0.<init>(r4);
        throw r0;
        r0 = 0;
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r0);
        r0 = r0.getOctets();
        r0 = org.bouncycastle.util.Arrays.clone(r0);
        r3.salt = r0;
        r0 = 1;
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1Integer.getInstance(r0);
        r0 = r0.getValue();
        r3.costParameter = r0;
        r0 = 2;
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1Integer.getInstance(r0);
        r0 = r0.getValue();
        r3.blockSize = r0;
        r0 = 3;
        r0 = r4.getObjectAt(r0);
        r0 = org.bouncycastle.asn1.ASN1Integer.getInstance(r0);
        r0 = r0.getValue();
        r3.parallelizationParameter = r0;
        r0 = r4.size();
        if (r0 != r1) goto L_0x0082;
        r4 = r4.getObjectAt(r2);
        r4 = org.bouncycastle.asn1.ASN1Integer.getInstance(r4);
        r4 = r4.getValue();
        r3.keyLength = r4;
        return;
        r4 = 0;
        goto L_0x007f;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.misc.ScryptParams.<init>(org.bouncycastle.asn1.ASN1Sequence):void");
    }

    public ScryptParams(byte[] bArr, int i, int i2, int i3) {
        this(bArr, BigInteger.valueOf((long) i), BigInteger.valueOf((long) i2), BigInteger.valueOf((long) i3), null);
    }

    public ScryptParams(byte[] bArr, int i, int i2, int i3, int i4) {
        this(bArr, BigInteger.valueOf((long) i), BigInteger.valueOf((long) i2), BigInteger.valueOf((long) i3), BigInteger.valueOf((long) i4));
    }

    public ScryptParams(byte[] bArr, BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, BigInteger bigInteger4) {
        this.salt = Arrays.clone(bArr);
        this.costParameter = bigInteger;
        this.blockSize = bigInteger2;
        this.parallelizationParameter = bigInteger3;
        this.keyLength = bigInteger4;
    }

    public static ScryptParams getInstance(Object obj) {
        return obj instanceof ScryptParams ? (ScryptParams) obj : obj != null ? new ScryptParams(ASN1Sequence.getInstance(obj)) : null;
    }

    public BigInteger getBlockSize() {
        return this.blockSize;
    }

    public BigInteger getCostParameter() {
        return this.costParameter;
    }

    public BigInteger getKeyLength() {
        return this.keyLength;
    }

    public BigInteger getParallelizationParameter() {
        return this.parallelizationParameter;
    }

    public byte[] getSalt() {
        return Arrays.clone(this.salt);
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new DEROctetString(this.salt));
        aSN1EncodableVector.add(new ASN1Integer(this.costParameter));
        aSN1EncodableVector.add(new ASN1Integer(this.blockSize));
        aSN1EncodableVector.add(new ASN1Integer(this.parallelizationParameter));
        if (this.keyLength != null) {
            aSN1EncodableVector.add(new ASN1Integer(this.keyLength));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
