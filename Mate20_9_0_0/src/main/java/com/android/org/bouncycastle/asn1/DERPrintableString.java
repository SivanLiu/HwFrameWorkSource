package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERPrintableString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public static DERPrintableString getInstance(Object obj) {
        if (obj == null || (obj instanceof DERPrintableString)) {
            return (DERPrintableString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERPrintableString) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERPrintableString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERPrintableString)) {
            return getInstance(o);
        }
        return new DERPrintableString(ASN1OctetString.getInstance(o).getOctets());
    }

    DERPrintableString(byte[] string) {
        this.string = string;
    }

    public DERPrintableString(String string) {
        this(string, false);
    }

    public DERPrintableString(String string, boolean validate) {
        if (!validate || isPrintableString(string)) {
            this.string = Strings.toByteArray(string);
            return;
        }
        throw new IllegalArgumentException("string contains illegal characters");
    }

    public String getString() {
        return Strings.fromByteArray(this.string);
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
        out.writeEncoded(19, this.string);
    }

    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof DERPrintableString)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERPrintableString) o).string);
    }

    public String toString() {
        return getString();
    }

    public static boolean isPrintableString(String str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            char ch = str.charAt(i);
            if (ch > 127) {
                return false;
            }
            if (('a' > ch || ch > 'z') && (('A' > ch || ch > 'Z') && !(('0' <= ch && ch <= '9') || ch == ' ' || ch == ':' || ch == '=' || ch == '?'))) {
                switch (ch) {
                    case '\'':
                    case '(':
                    case ')':
                        continue;
                    default:
                        switch (ch) {
                            case '+':
                            case ',':
                            case '-':
                            case '.':
                            case '/':
                                break;
                            default:
                                return false;
                        }
                }
            }
        }
        return true;
    }
}
