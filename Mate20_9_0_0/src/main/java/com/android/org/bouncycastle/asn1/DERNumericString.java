package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERNumericString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static DERNumericString getInstance(Object obj) {
        if (obj == null || (obj instanceof DERNumericString)) {
            return (DERNumericString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERNumericString) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERNumericString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERNumericString)) {
            return getInstance(o);
        }
        return new DERNumericString(ASN1OctetString.getInstance(o).getOctets());
    }

    DERNumericString(byte[] string) {
        this.string = string;
    }

    public DERNumericString(String string) {
        this(string, false);
    }

    public DERNumericString(String string, boolean validate) {
        if (!validate || isNumericString(string)) {
            this.string = Strings.toByteArray(string);
            return;
        }
        throw new IllegalArgumentException("string contains illegal characters");
    }

    public String getString() {
        return Strings.fromByteArray(this.string);
    }

    public String toString() {
        return getString();
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
        out.writeEncoded(18, this.string);
    }

    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof DERNumericString)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERNumericString) o).string);
    }

    public static boolean isNumericString(String str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            char ch = str.charAt(i);
            if (ch > 127) {
                return false;
            }
            if (('0' > ch || ch > '9') && ch != ' ') {
                return false;
            }
        }
        return true;
    }
}
