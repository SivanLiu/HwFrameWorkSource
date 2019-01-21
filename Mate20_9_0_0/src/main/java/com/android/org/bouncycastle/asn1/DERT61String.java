package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public class DERT61String extends ASN1Primitive implements ASN1String {
    private byte[] string;

    public static DERT61String getInstance(Object obj) {
        if (obj == null || (obj instanceof DERT61String)) {
            return (DERT61String) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (DERT61String) ASN1Primitive.fromByteArray((byte[]) obj);
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

    public static DERT61String getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof DERT61String)) {
            return getInstance(o);
        }
        return new DERT61String(ASN1OctetString.getInstance(o).getOctets());
    }

    public DERT61String(byte[] string) {
        this.string = Arrays.clone(string);
    }

    public DERT61String(String string) {
        this.string = Strings.toByteArray(string);
    }

    public String getString() {
        return Strings.fromByteArray(this.string);
    }

    public String toString() {
        return getString();
    }

    boolean isConstructed() {
        return false;
    }

    int encodedLength() {
        return (1 + StreamUtil.calculateBodyLength(this.string.length)) + this.string.length;
    }

    void encode(ASN1OutputStream out) throws IOException {
        out.writeEncoded(20, this.string);
    }

    public byte[] getOctets() {
        return Arrays.clone(this.string);
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (o instanceof DERT61String) {
            return Arrays.areEqual(this.string, ((DERT61String) o).string);
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(this.string);
    }
}
