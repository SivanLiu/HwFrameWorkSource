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
import org.bouncycastle.pqc.crypto.rainbow.Layer;
import org.bouncycastle.pqc.crypto.rainbow.util.RainbowUtil;

public class RainbowPrivateKey extends ASN1Object {
    private byte[] b1;
    private byte[] b2;
    private byte[][] invA1;
    private byte[][] invA2;
    private Layer[] layers;
    private ASN1ObjectIdentifier oid;
    private ASN1Integer version;
    private byte[] vi;

    private RainbowPrivateKey(ASN1Sequence aSN1Sequence) {
        int i;
        ASN1Sequence aSN1Sequence2 = aSN1Sequence;
        int i2 = 0;
        if (aSN1Sequence2.getObjectAt(0) instanceof ASN1Integer) {
            this.version = ASN1Integer.getInstance(aSN1Sequence2.getObjectAt(0));
        } else {
            this.oid = ASN1ObjectIdentifier.getInstance(aSN1Sequence2.getObjectAt(0));
        }
        ASN1Sequence aSN1Sequence3 = (ASN1Sequence) aSN1Sequence2.getObjectAt(1);
        this.invA1 = new byte[aSN1Sequence3.size()][];
        for (i = 0; i < aSN1Sequence3.size(); i++) {
            this.invA1[i] = ((ASN1OctetString) aSN1Sequence3.getObjectAt(i)).getOctets();
        }
        this.b1 = ((ASN1OctetString) ((ASN1Sequence) aSN1Sequence2.getObjectAt(2)).getObjectAt(0)).getOctets();
        ASN1Sequence aSN1Sequence4 = (ASN1Sequence) aSN1Sequence2.getObjectAt(3);
        this.invA2 = new byte[aSN1Sequence4.size()][];
        for (int i3 = 0; i3 < aSN1Sequence4.size(); i3++) {
            this.invA2[i3] = ((ASN1OctetString) aSN1Sequence4.getObjectAt(i3)).getOctets();
        }
        this.b2 = ((ASN1OctetString) ((ASN1Sequence) aSN1Sequence2.getObjectAt(4)).getObjectAt(0)).getOctets();
        this.vi = ((ASN1OctetString) ((ASN1Sequence) aSN1Sequence2.getObjectAt(5)).getObjectAt(0)).getOctets();
        aSN1Sequence2 = (ASN1Sequence) aSN1Sequence2.getObjectAt(6);
        byte[][][][] bArr = new byte[aSN1Sequence2.size()][][][];
        byte[][][][] bArr2 = new byte[aSN1Sequence2.size()][][][];
        byte[][][] bArr3 = new byte[aSN1Sequence2.size()][][];
        byte[][] bArr4 = new byte[aSN1Sequence2.size()][];
        int i4 = 0;
        while (i4 < aSN1Sequence2.size()) {
            int i5;
            ASN1Sequence aSN1Sequence5 = (ASN1Sequence) aSN1Sequence2.getObjectAt(i4);
            ASN1Sequence aSN1Sequence6 = (ASN1Sequence) aSN1Sequence5.getObjectAt(i2);
            bArr[i4] = new byte[aSN1Sequence6.size()][][];
            for (i5 = i2; i5 < aSN1Sequence6.size(); i5++) {
                ASN1Sequence aSN1Sequence7 = (ASN1Sequence) aSN1Sequence6.getObjectAt(i5);
                bArr[i4][i5] = new byte[aSN1Sequence7.size()][];
                for (i2 = 0; i2 < aSN1Sequence7.size(); i2++) {
                    bArr[i4][i5][i2] = ((ASN1OctetString) aSN1Sequence7.getObjectAt(i2)).getOctets();
                }
            }
            ASN1Sequence aSN1Sequence8 = (ASN1Sequence) aSN1Sequence5.getObjectAt(1);
            bArr2[i4] = new byte[aSN1Sequence8.size()][][];
            for (i = 0; i < aSN1Sequence8.size(); i++) {
                aSN1Sequence6 = (ASN1Sequence) aSN1Sequence8.getObjectAt(i);
                bArr2[i4][i] = new byte[aSN1Sequence6.size()][];
                for (i5 = 0; i5 < aSN1Sequence6.size(); i5++) {
                    bArr2[i4][i][i5] = ((ASN1OctetString) aSN1Sequence6.getObjectAt(i5)).getOctets();
                }
            }
            aSN1Sequence8 = (ASN1Sequence) aSN1Sequence5.getObjectAt(2);
            bArr3[i4] = new byte[aSN1Sequence8.size()][];
            for (i = 0; i < aSN1Sequence8.size(); i++) {
                bArr3[i4][i] = ((ASN1OctetString) aSN1Sequence8.getObjectAt(i)).getOctets();
            }
            bArr4[i4] = ((ASN1OctetString) aSN1Sequence5.getObjectAt(3)).getOctets();
            i4++;
            i2 = 0;
        }
        int length = this.vi.length - 1;
        this.layers = new Layer[length];
        i2 = 0;
        while (i2 < length) {
            i = i2 + 1;
            this.layers[i2] = new Layer(this.vi[i2], this.vi[i], RainbowUtil.convertArray(bArr[i2]), RainbowUtil.convertArray(bArr2[i2]), RainbowUtil.convertArray(bArr3[i2]), RainbowUtil.convertArray(bArr4[i2]));
            i2 = i;
        }
    }

    public RainbowPrivateKey(short[][] sArr, short[] sArr2, short[][] sArr3, short[] sArr4, int[] iArr, Layer[] layerArr) {
        this.version = new ASN1Integer(1);
        this.invA1 = RainbowUtil.convertArray(sArr);
        this.b1 = RainbowUtil.convertArray(sArr2);
        this.invA2 = RainbowUtil.convertArray(sArr3);
        this.b2 = RainbowUtil.convertArray(sArr4);
        this.vi = RainbowUtil.convertIntArray(iArr);
        this.layers = layerArr;
    }

    public static RainbowPrivateKey getInstance(Object obj) {
        return obj instanceof RainbowPrivateKey ? (RainbowPrivateKey) obj : obj != null ? new RainbowPrivateKey(ASN1Sequence.getInstance(obj)) : null;
    }

    public short[] getB1() {
        return RainbowUtil.convertArray(this.b1);
    }

    public short[] getB2() {
        return RainbowUtil.convertArray(this.b2);
    }

    public short[][] getInvA1() {
        return RainbowUtil.convertArray(this.invA1);
    }

    public short[][] getInvA2() {
        return RainbowUtil.convertArray(this.invA2);
    }

    public Layer[] getLayers() {
        return this.layers;
    }

    public ASN1Integer getVersion() {
        return this.version;
    }

    public int[] getVi() {
        return RainbowUtil.convertArraytoInt(this.vi);
    }

    public ASN1Primitive toASN1Primitive() {
        int i;
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.version != null ? this.version : this.oid);
        ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
        for (byte[] dEROctetString : this.invA1) {
            aSN1EncodableVector2.add(new DEROctetString(dEROctetString));
        }
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector2 = new ASN1EncodableVector();
        aSN1EncodableVector2.add(new DEROctetString(this.b1));
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector2 = new ASN1EncodableVector();
        for (byte[] dEROctetString2 : this.invA2) {
            aSN1EncodableVector2.add(new DEROctetString(dEROctetString2));
        }
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector2 = new ASN1EncodableVector();
        aSN1EncodableVector2.add(new DEROctetString(this.b2));
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector2 = new ASN1EncodableVector();
        aSN1EncodableVector2.add(new DEROctetString(this.vi));
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        aSN1EncodableVector2 = new ASN1EncodableVector();
        for (i = 0; i < this.layers.length; i++) {
            int i2;
            ASN1EncodableVector aSN1EncodableVector3;
            ASN1EncodableVector aSN1EncodableVector4 = new ASN1EncodableVector();
            byte[][][] convertArray = RainbowUtil.convertArray(this.layers[i].getCoeffAlpha());
            ASN1EncodableVector aSN1EncodableVector5 = new ASN1EncodableVector();
            for (i2 = 0; i2 < convertArray.length; i2++) {
                aSN1EncodableVector3 = new ASN1EncodableVector();
                for (byte[] dEROctetString3 : convertArray[i2]) {
                    aSN1EncodableVector3.add(new DEROctetString(dEROctetString3));
                }
                aSN1EncodableVector5.add(new DERSequence(aSN1EncodableVector3));
            }
            aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector5));
            convertArray = RainbowUtil.convertArray(this.layers[i].getCoeffBeta());
            aSN1EncodableVector5 = new ASN1EncodableVector();
            for (i2 = 0; i2 < convertArray.length; i2++) {
                aSN1EncodableVector3 = new ASN1EncodableVector();
                for (byte[] dEROctetString32 : convertArray[i2]) {
                    aSN1EncodableVector3.add(new DEROctetString(dEROctetString32));
                }
                aSN1EncodableVector5.add(new DERSequence(aSN1EncodableVector3));
            }
            aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector5));
            byte[][] convertArray2 = RainbowUtil.convertArray(this.layers[i].getCoeffGamma());
            aSN1EncodableVector5 = new ASN1EncodableVector();
            for (byte[] dEROctetString4 : convertArray2) {
                aSN1EncodableVector5.add(new DEROctetString(dEROctetString4));
            }
            aSN1EncodableVector4.add(new DERSequence(aSN1EncodableVector5));
            aSN1EncodableVector4.add(new DEROctetString(RainbowUtil.convertArray(this.layers[i].getCoeffEta())));
            aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector4));
        }
        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
        return new DERSequence(aSN1EncodableVector);
    }
}
