package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import java.io.IOException;

public class DERBMPString extends ASN1Primitive implements ASN1String {
    private final char[] string;

    public static DERBMPString getInstance(Object obj) {
        if (obj == null || (obj instanceof DERBMPString)) {
            return (DERBMPString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERBMPString) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERBMPString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERBMPString)) {
            return getInstance(o);
        }
        return new DERBMPString(ASN1OctetString.getInstance(o).getOctets());
    }

    DERBMPString(byte[] string) {
        char[] cs = new char[(string.length / 2)];
        for (int i = 0; i != cs.length; i++) {
            cs[i] = (char) ((string[2 * i] << 8) | (string[(2 * i) + 1] & 255));
        }
        this.string = cs;
    }

    DERBMPString(char[] string) {
        this.string = string;
    }

    public DERBMPString(String string) {
        this.string = string.toCharArray();
    }

    public String getString() {
        return new String(this.string);
    }

    public String toString() {
        return getString();
    }

    public int hashCode() {
        return Arrays.hashCode(this.string);
    }

    protected boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof DERBMPString)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((DERBMPString) o).string);
    }

    boolean isConstructed() {
        return false;
    }

    int encodedLength() {
        return (1 + StreamUtil.calculateBodyLength(this.string.length * 2)) + (this.string.length * 2);
    }

    void encode(ASN1OutputStream out) throws IOException {
        out.write(30);
        out.writeLength(this.string.length * 2);
        for (int i = 0; i != this.string.length; i++) {
            char c = this.string[i];
            out.write((byte) (c >> 8));
            out.write((byte) c);
        }
    }
}
