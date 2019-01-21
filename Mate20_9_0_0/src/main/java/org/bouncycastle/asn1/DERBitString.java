package org.bouncycastle.asn1;

import java.io.IOException;

public class DERBitString extends ASN1BitString {
    protected DERBitString(byte b, int i) {
        this(toByteArray(b), i);
    }

    public DERBitString(int i) {
        super(ASN1BitString.getBytes(i), ASN1BitString.getPadBits(i));
    }

    public DERBitString(ASN1Encodable aSN1Encodable) throws IOException {
        super(aSN1Encodable.toASN1Primitive().getEncoded(ASN1Encoding.DER), 0);
    }

    public DERBitString(byte[] bArr) {
        this(bArr, 0);
    }

    public DERBitString(byte[] bArr, int i) {
        super(bArr, i);
    }

    static DERBitString fromOctetString(byte[] bArr) {
        if (bArr.length >= 1) {
            int i = bArr[0];
            byte[] bArr2 = new byte[(bArr.length - 1)];
            if (bArr2.length != 0) {
                System.arraycopy(bArr, 1, bArr2, 0, bArr.length - 1);
            }
            return new DERBitString(bArr2, i);
        }
        throw new IllegalArgumentException("truncated BIT STRING detected");
    }

    public static DERBitString getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof DERBitString)) {
            return (DERBitString) obj;
        }
        if (obj instanceof DLBitString) {
            DLBitString dLBitString = (DLBitString) obj;
            return new DERBitString(dLBitString.data, dLBitString.padBits);
        } else if (obj instanceof byte[]) {
            try {
                return (DERBitString) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("encoding error in getInstance: ");
                stringBuilder.append(e.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("illegal object in getInstance: ");
            stringBuilder.append(obj.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static DERBitString getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof DERBitString)) ? getInstance(object) : fromOctetString(((ASN1OctetString) object).getOctets());
    }

    private static byte[] toByteArray(byte b) {
        return new byte[]{b};
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        byte[] derForm = ASN1BitString.derForm(this.data, this.padBits);
        byte[] bArr = new byte[(derForm.length + 1)];
        bArr[0] = (byte) getPadBits();
        System.arraycopy(derForm, 0, bArr, 1, bArr.length - 1);
        aSN1OutputStream.writeEncoded(3, bArr);
    }

    int encodedLength() {
        return ((StreamUtil.calculateBodyLength(this.data.length + 1) + 1) + this.data.length) + 1;
    }

    boolean isConstructed() {
        return false;
    }
}
