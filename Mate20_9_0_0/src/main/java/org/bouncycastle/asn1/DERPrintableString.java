package org.bouncycastle.asn1;

import java.io.IOException;
import org.bouncycastle.asn1.eac.EACTags;
import org.bouncycastle.pqc.math.linearalgebra.Matrix;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class DERPrintableString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public DERPrintableString(String str) {
        this(str, false);
    }

    public DERPrintableString(String str, boolean z) {
        if (!z || isPrintableString(str)) {
            this.string = Strings.toByteArray(str);
            return;
        }
        throw new IllegalArgumentException("string contains illegal characters");
    }

    DERPrintableString(byte[] bArr) {
        this.string = bArr;
    }

    public static DERPrintableString getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof DERPrintableString)) {
            return (DERPrintableString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERPrintableString) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERPrintableString getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof DERPrintableString)) ? getInstance(object) : new DERPrintableString(ASN1OctetString.getInstance(object).getOctets());
    }

    public static boolean isPrintableString(String str) {
        for (int length = str.length() - 1; length >= 0; length--) {
            char charAt = str.charAt(length);
            if (charAt > 127) {
                return false;
            }
            if (('a' > charAt || charAt > 'z') && (('A' > charAt || charAt > Matrix.MATRIX_TYPE_ZERO) && !(('0' <= charAt && charAt <= '9') || charAt == ' ' || charAt == ':' || charAt == '=' || charAt == '?'))) {
                switch (charAt) {
                    case EACTags.INTERCHANGE_CONTROL /*39*/:
                    case '(':
                    case EACTags.INTERCHANGE_PROFILE /*41*/:
                        continue;
                    default:
                        switch (charAt) {
                            case EACTags.DATE_OF_BIRTH /*43*/:
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

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERPrintableString)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERPrintableString) aSN1Primitive).string);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(19, this.string);
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
