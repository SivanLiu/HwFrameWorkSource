package org.bouncycastle.asn1.x9;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECCurve.F2m;
import org.bouncycastle.math.ec.ECCurve.Fp;

public class X9Curve extends ASN1Object implements X9ObjectIdentifiers {
    private ECCurve curve;
    private ASN1ObjectIdentifier fieldIdentifier = null;
    private byte[] seed;

    public X9Curve(X9FieldID x9FieldID, ASN1Sequence aSN1Sequence) {
        ECCurve fp;
        this.fieldIdentifier = x9FieldID.getIdentifier();
        if (this.fieldIdentifier.equals(prime_field)) {
            BigInteger value = ((ASN1Integer) x9FieldID.getParameters()).getValue();
            fp = new Fp(value, new X9FieldElement(value, (ASN1OctetString) aSN1Sequence.getObjectAt(0)).getValue().toBigInteger(), new X9FieldElement(value, (ASN1OctetString) aSN1Sequence.getObjectAt(1)).getValue().toBigInteger());
        } else if (this.fieldIdentifier.equals(characteristic_two_field)) {
            int intValue;
            int i;
            int i2;
            int intValue2;
            ASN1Sequence instance = ASN1Sequence.getInstance(x9FieldID.getParameters());
            int intValue3 = ((ASN1Integer) instance.getObjectAt(0)).getValue().intValue();
            ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) instance.getObjectAt(1);
            if (aSN1ObjectIdentifier.equals(tpBasis)) {
                intValue = ASN1Integer.getInstance(instance.getObjectAt(2)).getValue().intValue();
                i = 0;
                i2 = i;
            } else if (aSN1ObjectIdentifier.equals(ppBasis)) {
                instance = ASN1Sequence.getInstance(instance.getObjectAt(2));
                int intValue4 = ASN1Integer.getInstance(instance.getObjectAt(0)).getValue().intValue();
                intValue2 = ASN1Integer.getInstance(instance.getObjectAt(1)).getValue().intValue();
                i2 = ASN1Integer.getInstance(instance.getObjectAt(2)).getValue().intValue();
                intValue = intValue4;
                i = intValue2;
            } else {
                throw new IllegalArgumentException("This type of EC basis is not implemented");
            }
            intValue2 = intValue3;
            int i3 = intValue;
            int i4 = i;
            int i5 = i2;
            ECCurve f2m = new F2m(intValue2, i3, i4, i5, new X9FieldElement(intValue2, i3, i4, i5, (ASN1OctetString) aSN1Sequence.getObjectAt(0)).getValue().toBigInteger(), new X9FieldElement(intValue2, i3, i4, i5, (ASN1OctetString) aSN1Sequence.getObjectAt(1)).getValue().toBigInteger());
        } else {
            throw new IllegalArgumentException("This type of ECCurve is not implemented");
        }
        this.curve = fp;
        if (aSN1Sequence.size() == 3) {
            this.seed = ((DERBitString) aSN1Sequence.getObjectAt(2)).getBytes();
        }
    }

    public X9Curve(ECCurve eCCurve) {
        this.curve = eCCurve;
        this.seed = null;
        setFieldIdentifier();
    }

    public X9Curve(ECCurve eCCurve, byte[] bArr) {
        this.curve = eCCurve;
        this.seed = bArr;
        setFieldIdentifier();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0018 in {2, 4, 7, 10} preds:[]
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
    private void setFieldIdentifier() {
        /*
        r2 = this;
        r0 = r2.curve;
        r0 = org.bouncycastle.math.ec.ECAlgorithms.isFpCurve(r0);
        if (r0 == 0) goto L_0x000d;
    L_0x0008:
        r0 = prime_field;
    L_0x000a:
        r2.fieldIdentifier = r0;
        return;
    L_0x000d:
        r0 = r2.curve;
        r0 = org.bouncycastle.math.ec.ECAlgorithms.isF2mCurve(r0);
        if (r0 == 0) goto L_0x0019;
    L_0x0015:
        r0 = characteristic_two_field;
        goto L_0x000a;
        return;
    L_0x0019:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "This type of ECCurve is not implemented";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.asn1.x9.X9Curve.setFieldIdentifier():void");
    }

    public ECCurve getCurve() {
        return this.curve;
    }

    public byte[] getSeed() {
        return this.seed;
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x0060  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ASN1Primitive toASN1Primitive() {
        X9FieldElement x9FieldElement;
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.fieldIdentifier.equals(prime_field)) {
            aSN1EncodableVector.add(new X9FieldElement(this.curve.getA()).toASN1Primitive());
            x9FieldElement = new X9FieldElement(this.curve.getB());
        } else {
            if (this.fieldIdentifier.equals(characteristic_two_field)) {
                aSN1EncodableVector.add(new X9FieldElement(this.curve.getA()).toASN1Primitive());
                x9FieldElement = new X9FieldElement(this.curve.getB());
            }
            if (this.seed != null) {
                aSN1EncodableVector.add(new DERBitString(this.seed));
            }
            return new DERSequence(aSN1EncodableVector);
        }
        aSN1EncodableVector.add(x9FieldElement.toASN1Primitive());
        if (this.seed != null) {
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
