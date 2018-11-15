package org.bouncycastle.asn1;

import java.io.IOException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class DERIA5String extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public DERIA5String(String str) {
        this(str, false);
    }

    public DERIA5String(String str, boolean z) {
        if (str == null) {
            throw new NullPointerException("string cannot be null");
        } else if (!z || isIA5String(str)) {
            this.string = Strings.toByteArray(str);
        } else {
            throw new IllegalArgumentException("string contains illegal characters");
        }
    }

    DERIA5String(byte[] bArr) {
        this.string = bArr;
    }

    public static DERIA5String getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof DERIA5String)) {
            return (DERIA5String) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERIA5String) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERIA5String getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof DERIA5String)) ? getInstance(object) : new DERIA5String(((ASN1OctetString) object).getOctets());
    }

    public static boolean isIA5String(String str) {
        for (int length = str.length() - 1; length >= 0; length--) {
            if (str.charAt(length) > 127) {
                return false;
            }
        }
        return true;
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERIA5String)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERIA5String) aSN1Primitive).string);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(22, this.string);
    }

    int encodedLength() {
        return (1 + StreamUtil.calculateBodyLength(this.string.length)) + this.string.length;
    }

    public byte[] getOctets() {
        return Arrays.clone(this.string);
    }

    public String getString() {
        return Strings.fromByteArray(this.string);
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
