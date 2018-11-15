package org.bouncycastle.asn1;

import java.io.IOException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class DERUTF8String extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public DERUTF8String(String str) {
        this.string = Strings.toUTF8ByteArray(str);
    }

    DERUTF8String(byte[] bArr) {
        this.string = bArr;
    }

    public static DERUTF8String getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof DERUTF8String)) {
            return (DERUTF8String) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERUTF8String) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERUTF8String getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof DERUTF8String)) ? getInstance(object) : new DERUTF8String(ASN1OctetString.getInstance(object).getOctets());
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERUTF8String)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERUTF8String) aSN1Primitive).string);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(12, this.string);
    }

    int encodedLength() throws IOException {
        return (1 + StreamUtil.calculateBodyLength(this.string.length)) + this.string.length;
    }

    public String getString() {
        return Strings.fromUTF8ByteArray(this.string);
    }

    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    boolean isConstructed() {
        return false;
    }

    public String toString() {
        return getString();
    }
}
