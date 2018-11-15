package org.bouncycastle.asn1;

import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Properties;

public class ASN1Enumerated extends ASN1Primitive {
    private static ASN1Enumerated[] cache = new ASN1Enumerated[12];
    private final byte[] bytes;

    public ASN1Enumerated(int i) {
        this.bytes = BigInteger.valueOf((long) i).toByteArray();
    }

    public ASN1Enumerated(BigInteger bigInteger) {
        this.bytes = bigInteger.toByteArray();
    }

    public ASN1Enumerated(byte[] bArr) {
        if (Properties.isOverrideSet("org.bouncycastle.asn1.allow_unsafe_integer") || !ASN1Integer.isMalformed(bArr)) {
            this.bytes = Arrays.clone(bArr);
            return;
        }
        throw new IllegalArgumentException("malformed enumerated");
    }

    static ASN1Enumerated fromOctetString(byte[] bArr) {
        if (bArr.length > 1) {
            return new ASN1Enumerated(bArr);
        }
        if (bArr.length != 0) {
            int i = bArr[0] & 255;
            if (i >= cache.length) {
                return new ASN1Enumerated(Arrays.clone(bArr));
            }
            ASN1Enumerated aSN1Enumerated = cache[i];
            if (aSN1Enumerated == null) {
                ASN1Enumerated[] aSN1EnumeratedArr = cache;
                ASN1Enumerated aSN1Enumerated2 = new ASN1Enumerated(Arrays.clone(bArr));
                aSN1EnumeratedArr[i] = aSN1Enumerated2;
                aSN1Enumerated = aSN1Enumerated2;
            }
            return aSN1Enumerated;
        }
        throw new IllegalArgumentException("ENUMERATED has zero length");
    }

    public static ASN1Enumerated getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof ASN1Enumerated)) {
            return (ASN1Enumerated) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1Enumerated) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static ASN1Enumerated getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof ASN1Enumerated)) ? getInstance(object) : fromOctetString(((ASN1OctetString) object).getOctets());
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1Enumerated)) {
            return false;
        }
        return Arrays.areEqual(this.bytes, ((ASN1Enumerated) aSN1Primitive).bytes);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(10, this.bytes);
    }

    int encodedLength() {
        return (1 + StreamUtil.calculateBodyLength(this.bytes.length)) + this.bytes.length;
    }

    public BigInteger getValue() {
        return new BigInteger(this.bytes);
    }

    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    boolean isConstructed() {
        return false;
    }
}
