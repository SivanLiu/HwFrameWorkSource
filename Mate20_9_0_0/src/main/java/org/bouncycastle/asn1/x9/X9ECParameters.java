package org.bouncycastle.asn1.x9;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

public class X9ECParameters extends ASN1Object implements X9ObjectIdentifiers {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private ECCurve curve;
    private X9FieldID fieldID;
    private X9ECPoint g;
    private BigInteger h;
    private BigInteger n;
    private byte[] seed;

    private X9ECParameters(ASN1Sequence aSN1Sequence) {
        if ((aSN1Sequence.getObjectAt(0) instanceof ASN1Integer) && ((ASN1Integer) aSN1Sequence.getObjectAt(0)).getValue().equals(ONE)) {
            X9Curve x9Curve = new X9Curve(X9FieldID.getInstance(aSN1Sequence.getObjectAt(1)), ASN1Sequence.getInstance(aSN1Sequence.getObjectAt(2)));
            this.curve = x9Curve.getCurve();
            ASN1Encodable objectAt = aSN1Sequence.getObjectAt(3);
            if (objectAt instanceof X9ECPoint) {
                this.g = (X9ECPoint) objectAt;
            } else {
                this.g = new X9ECPoint(this.curve, (ASN1OctetString) objectAt);
            }
            this.n = ((ASN1Integer) aSN1Sequence.getObjectAt(4)).getValue();
            this.seed = x9Curve.getSeed();
            if (aSN1Sequence.size() == 6) {
                this.h = ((ASN1Integer) aSN1Sequence.getObjectAt(5)).getValue();
                return;
            }
            return;
        }
        throw new IllegalArgumentException("bad version in X9ECParameters");
    }

    public X9ECParameters(ECCurve eCCurve, X9ECPoint x9ECPoint, BigInteger bigInteger, BigInteger bigInteger2) {
        this(eCCurve, x9ECPoint, bigInteger, bigInteger2, null);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:13:0x005a in {2, 4, 9, 12, 15, 17} preds:[]
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
    public X9ECParameters(org.bouncycastle.math.ec.ECCurve r2, org.bouncycastle.asn1.x9.X9ECPoint r3, java.math.BigInteger r4, java.math.BigInteger r5, byte[] r6) {
        /*
        r1 = this;
        r1.<init>();
        r1.curve = r2;
        r1.g = r3;
        r1.n = r4;
        r1.h = r5;
        r1.seed = r6;
        r3 = org.bouncycastle.math.ec.ECAlgorithms.isFpCurve(r2);
        if (r3 == 0) goto L_0x0023;
    L_0x0013:
        r3 = new org.bouncycastle.asn1.x9.X9FieldID;
        r2 = r2.getField();
        r2 = r2.getCharacteristic();
        r3.<init>(r2);
    L_0x0020:
        r1.fieldID = r3;
        return;
    L_0x0023:
        r3 = org.bouncycastle.math.ec.ECAlgorithms.isF2mCurve(r2);
        if (r3 == 0) goto L_0x0063;
    L_0x0029:
        r2 = r2.getField();
        r2 = (org.bouncycastle.math.field.PolynomialExtensionField) r2;
        r2 = r2.getMinimalPolynomial();
        r2 = r2.getExponentsPresent();
        r3 = r2.length;
        r4 = 1;
        r5 = 2;
        r6 = 3;
        if (r3 != r6) goto L_0x0047;
    L_0x003d:
        r3 = new org.bouncycastle.asn1.x9.X9FieldID;
        r5 = r2[r5];
        r2 = r2[r4];
        r3.<init>(r5, r2);
        goto L_0x0020;
    L_0x0047:
        r3 = r2.length;
        r0 = 5;
        if (r3 != r0) goto L_0x005b;
    L_0x004b:
        r3 = new org.bouncycastle.asn1.x9.X9FieldID;
        r0 = 4;
        r0 = r2[r0];
        r4 = r2[r4];
        r5 = r2[r5];
        r2 = r2[r6];
        r3.<init>(r0, r4, r5, r2);
        goto L_0x0020;
        return;
    L_0x005b:
        r2 = new java.lang.IllegalArgumentException;
        r3 = "Only trinomial and pentomial curves are supported";
        r2.<init>(r3);
        throw r2;
    L_0x0063:
        r2 = new java.lang.IllegalArgumentException;
        r3 = "'curve' is of an unsupported type";
        r2.<init>(r3);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x9.X9ECParameters.<init>(org.bouncycastle.math.ec.ECCurve, org.bouncycastle.asn1.x9.X9ECPoint, java.math.BigInteger, java.math.BigInteger, byte[]):void");
    }

    public X9ECParameters(ECCurve eCCurve, ECPoint eCPoint, BigInteger bigInteger) {
        this(eCCurve, eCPoint, bigInteger, null, null);
    }

    public X9ECParameters(ECCurve eCCurve, ECPoint eCPoint, BigInteger bigInteger, BigInteger bigInteger2) {
        this(eCCurve, eCPoint, bigInteger, bigInteger2, null);
    }

    public X9ECParameters(ECCurve eCCurve, ECPoint eCPoint, BigInteger bigInteger, BigInteger bigInteger2, byte[] bArr) {
        this(eCCurve, new X9ECPoint(eCPoint), bigInteger, bigInteger2, bArr);
    }

    public static X9ECParameters getInstance(Object obj) {
        return obj instanceof X9ECParameters ? (X9ECParameters) obj : obj != null ? new X9ECParameters(ASN1Sequence.getInstance(obj)) : null;
    }

    public X9ECPoint getBaseEntry() {
        return this.g;
    }

    public ECCurve getCurve() {
        return this.curve;
    }

    public X9Curve getCurveEntry() {
        return new X9Curve(this.curve, this.seed);
    }

    public X9FieldID getFieldIDEntry() {
        return this.fieldID;
    }

    public ECPoint getG() {
        return this.g.getPoint();
    }

    public BigInteger getH() {
        return this.h;
    }

    public BigInteger getN() {
        return this.n;
    }

    public byte[] getSeed() {
        return this.seed;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new ASN1Integer(ONE));
        aSN1EncodableVector.add(this.fieldID);
        aSN1EncodableVector.add(new X9Curve(this.curve, this.seed));
        aSN1EncodableVector.add(this.g);
        aSN1EncodableVector.add(new ASN1Integer(this.n));
        if (this.h != null) {
            aSN1EncodableVector.add(new ASN1Integer(this.h));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
