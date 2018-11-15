package org.bouncycastle.asn1;

import java.io.IOException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class DERGeneralString extends ASN1Primitive implements ASN1String {
    private final byte[] string;

    public DERGeneralString(String str) {
        this.string = Strings.toByteArray(str);
    }

    DERGeneralString(byte[] bArr) {
        this.string = bArr;
    }

    public static DERGeneralString getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof DERGeneralString)) {
            return (DERGeneralString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERGeneralString) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERGeneralString getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        return (z || (object instanceof DERGeneralString)) ? getInstance(object) : new DERGeneralString(((ASN1OctetString) object).getOctets());
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof DERGeneralString)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERGeneralString) aSN1Primitive).string);
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(27, this.string);
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
