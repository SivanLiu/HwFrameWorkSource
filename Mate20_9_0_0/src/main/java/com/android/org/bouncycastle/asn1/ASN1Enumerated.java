package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import java.io.IOException;
import java.math.BigInteger;

public class ASN1Enumerated extends ASN1Primitive {
    private static ASN1Enumerated[] cache = new ASN1Enumerated[12];
    private final byte[] bytes;

    public static ASN1Enumerated getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1Enumerated)) {
            return (ASN1Enumerated) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1Enumerated) ASN1Primitive.fromByteArray((byte[]) obj);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("encoding error in getInstance: ");
                stringBuilder.append(e.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("illegal object in getInstance: ");
        stringBuilder2.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public static ASN1Enumerated getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof ASN1Enumerated)) {
            return getInstance(o);
        }
        return fromOctetString(((ASN1OctetString) o).getOctets());
    }

    public ASN1Enumerated(int value) {
        this.bytes = BigInteger.valueOf((long) value).toByteArray();
    }

    public ASN1Enumerated(BigInteger value) {
        this.bytes = value.toByteArray();
    }

    public ASN1Enumerated(byte[] bytes) {
        if (bytes.length > 1) {
            if (bytes[0] == (byte) 0 && (bytes[1] & 128) == 0) {
                throw new IllegalArgumentException("malformed enumerated");
            } else if (bytes[0] == (byte) -1 && (bytes[1] & 128) != 0) {
                throw new IllegalArgumentException("malformed enumerated");
            }
        }
        this.bytes = Arrays.clone(bytes);
    }

    public BigInteger getValue() {
        return new BigInteger(this.bytes);
    }

    boolean isConstructed() {
        return false;
    }

    int encodedLength() {
        return (1 + StreamUtil.calculateBodyLength(this.bytes.length)) + this.bytes.length;
    }

    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(10, this.bytes);
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof ASN1Enumerated)) {
            return false;
        }
        return Arrays.areEqual(this.bytes, ((ASN1Enumerated) o).bytes);
    }

    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    static ASN1Enumerated fromOctetString(byte[] enc) {
        if (enc.length > 1) {
            return new ASN1Enumerated(enc);
        }
        if (enc.length != 0) {
            int value = enc[0] & 255;
            if (value >= cache.length) {
                return new ASN1Enumerated(Arrays.clone(enc));
            }
            ASN1Enumerated possibleMatch = cache[value];
            if (possibleMatch == null) {
                ASN1Enumerated[] aSN1EnumeratedArr = cache;
                ASN1Enumerated aSN1Enumerated = new ASN1Enumerated(Arrays.clone(enc));
                aSN1EnumeratedArr[value] = aSN1Enumerated;
                possibleMatch = aSN1Enumerated;
            }
            return possibleMatch;
        }
        throw new IllegalArgumentException("ENUMERATED has zero length");
    }
}
