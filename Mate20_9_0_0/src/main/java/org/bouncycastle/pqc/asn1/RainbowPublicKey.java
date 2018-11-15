package org.bouncycastle.pqc.asn1;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.pqc.crypto.rainbow.util.RainbowUtil;

public class RainbowPublicKey extends ASN1Object {
    private byte[][] coeffQuadratic;
    private byte[] coeffScalar;
    private byte[][] coeffSingular;
    private ASN1Integer docLength;
    private ASN1ObjectIdentifier oid;
    private ASN1Integer version;

    public RainbowPublicKey(int i, short[][] sArr, short[][] sArr2, short[] sArr3) {
        this.version = new ASN1Integer(0);
        this.docLength = new ASN1Integer((long) i);
        this.coeffQuadratic = RainbowUtil.convertArray(sArr);
        this.coeffSingular = RainbowUtil.convertArray(sArr2);
        this.coeffScalar = RainbowUtil.convertArray(sArr3);
    }

    private RainbowPublicKey(ASN1Sequence aSN1Sequence) {
        int i;
        if (aSN1Sequence.getObjectAt(0) instanceof ASN1Integer) {
            this.version = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(0));
        } else {
            this.oid = ASN1ObjectIdentifier.getInstance(aSN1Sequence.getObjectAt(0));
        }
        this.docLength = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(1));
        ASN1Sequence instance = ASN1Sequence.getInstance(aSN1Sequence.getObjectAt(2));
        this.coeffQuadratic = new byte[instance.size()][];
        for (i = 0; i < instance.size(); i++) {
            this.coeffQuadratic[i] = ASN1OctetString.getInstance(instance.getObjectAt(i)).getOctets();
        }
        instance = (ASN1Sequence) aSN1Sequence.getObjectAt(3);
        this.coeffSingular = new byte[instance.size()][];
        for (i = 0; i < instance.size(); i++) {
            this.coeffSingular[i] = ASN1OctetString.getInstance(instance.getObjectAt(i)).getOctets();
        }
        this.coeffScalar = ASN1OctetString.getInstance(((ASN1Sequence) aSN1Sequence.getObjectAt(4)).getObjectAt(0)).getOctets();
    }

    public static RainbowPublicKey getInstance(Object obj) {
        return obj instanceof RainbowPublicKey ? (RainbowPublicKey) obj : obj != null ? new RainbowPublicKey(ASN1Sequence.getInstance(obj)) : null;
    }

    public short[][] getCoeffQuadratic() {
        return RainbowUtil.convertArray(this.coeffQuadratic);
    }

    public short[] getCoeffScalar() {
        return RainbowUtil.convertArray(this.coeffScalar);
    }

    public short[][] getCoeffSingular() {
        return RainbowUtil.convertArray(this.coeffSingular);
    }

    public int getDocLength() {
        return this.docLength.getValue().intValue();
    }

    public ASN1Integer getVersion() {
        return this.version;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.version != null ? this.version : this.oid);
        aSN1EncodableVector.add(this.docLength);
        ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
        int i = 0;
        for (byte[] dEROctetString : this.coeffQuadratic) {
            aSN1EncodableVector2.add(new DEROctetString(dEROctetString));
        }
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector2 = new ASN1EncodableVector();
        while (i < this.coeffSingular.length) {
            aSN1EncodableVector2.add(new DEROctetString(this.coeffSingular[i]));
            i++;
        }
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector2 = new ASN1EncodableVector();
        aSN1EncodableVector2.add(new DEROctetString(this.coeffScalar));
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        return new DERSequence(aSN1EncodableVector);
    }
}
