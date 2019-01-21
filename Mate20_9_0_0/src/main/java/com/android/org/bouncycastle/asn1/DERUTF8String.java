package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERUTF8String extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static DERUTF8String getInstance(Object obj) {
        if (obj == null || (obj instanceof DERUTF8String)) {
            return (DERUTF8String) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERUTF8String) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERUTF8String getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERUTF8String)) {
            return getInstance(o);
        }
        return new DERUTF8String(ASN1OctetString.getInstance(o).getOctets());
    }

    DERUTF8String(byte[] string) {
        this.string = string;
    }

    public DERUTF8String(String string) {
        this.string = Strings.toUTF8ByteArray(string);
    }

    public String getString() {
        return Strings.fromUTF8ByteArray(this.string);
    }

    public String toString() {
        return getString();
    }

    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof DERUTF8String)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERUTF8String) o).string);
    }

    boolean isConstructed() {
        return false;
    }

    int encodedLength() throws IOException {
        return (1 + StreamUtil.calculateBodyLength(this.string.length)) + this.string.length;
    }

    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(12, this.string);
    }
}
