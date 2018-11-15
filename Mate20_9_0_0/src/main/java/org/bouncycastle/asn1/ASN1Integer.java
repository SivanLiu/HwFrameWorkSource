package org.bouncycastle.asn1;

import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Properties;

public class ASN1Integer extends ASN1Primitive {
    private final byte[] bytes;

    public ASN1Integer(long j) {
        this.bytes = BigInteger.valueOf(j).toByteArray();
    }

    public ASN1Integer(BigInteger bigInteger) {
        this.bytes = bigInteger.toByteArray();
    }

    public ASN1Integer(byte[] bArr) {
        this(bArr, true);
    }

    ASN1Integer(byte[] bArr, boolean z) {
        if (Properties.isOverrideSet("org.bouncycastle.asn1.allow_unsafe_integer") || !isMalformed(bArr)) {
            if (z) {
                bArr = Arrays.clone(bArr);
            }
            this.bytes = bArr;
            return;
        }
        throw new IllegalArgumentException("malformed integer");
    }

    public static ASN1Integer getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1Integer)) {
            return (ASN1Integer) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1Integer) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("encoding error in getInstance: ");
                stringBuilder.append(e.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static ASN1Integer getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof ASN1Integer)) ? getInstance(object) : new ASN1Integer(ASN1OctetString.getInstance(object).getOctets());
    }

    static boolean isMalformed(byte[] bArr) {
        if (bArr.length > 1) {
            return (bArr[0] == (byte) 0 && (bArr[1] & 128) == 0) ? true : bArr[0] == (byte) -1 && (bArr[1] & 128) != 0;
        }
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1Integer)) {
            return false;
        }
        return Arrays.areEqual(this.bytes, ((ASN1Integer) aSN1Primitive).bytes);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(2, this.bytes);
    }

    int encodedLength() {
        return (1 + StreamUtil.calculateBodyLength(this.bytes.length)) + this.bytes.length;
    }

    public BigInteger getPositiveValue() {
        return new BigInteger(1, this.bytes);
    }

    public BigInteger getValue() {
        return new BigInteger(this.bytes);
    }

    public int hashCode() {
        int i = 0;
        int i2 = 0;
        while (i != this.bytes.length) {
            i2 ^= (this.bytes[i] & 255) << (i % 4);
            i++;
        }
        return i2;
    }

    boolean isConstructed() {
        return false;
    }

    public String toString() {
        return getValue().toString();
    }
}
