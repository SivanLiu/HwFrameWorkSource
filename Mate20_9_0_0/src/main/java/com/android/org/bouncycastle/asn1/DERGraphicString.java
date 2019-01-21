package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERGraphicString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static DERGraphicString getInstance(Object obj) {
        if (obj == null || (obj instanceof DERGraphicString)) {
            return (DERGraphicString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERGraphicString) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERGraphicString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERGraphicString)) {
            return getInstance(o);
        }
        return new DERGraphicString(((ASN1OctetString) o).getOctets());
    }

    public DERGraphicString(byte[] string) {
        this.string = Arrays.clone(string);
    }

    public byte[] getOctets() {
        return Arrays.clone(this.string);
    }

    boolean isConstructed() {
        return false;
    }

    int encodedLength() {
        return (1 + StreamUtil.calculateBodyLength(this.string.length)) + this.string.length;
    }

    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(25, this.string);
    }

    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof DERGraphicString)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERGraphicString) o).string);
    }

    public String getString() {
        return Strings.fromByteArray(this.string);
    }
}
