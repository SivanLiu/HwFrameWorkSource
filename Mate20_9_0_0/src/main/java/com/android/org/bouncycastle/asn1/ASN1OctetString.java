package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ASN1OctetString extends ASN1Primitive implements ASN1OctetStringParser {
    byte[] string;

    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static ASN1OctetString getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Primitive o = obj.getObject();
        if (explicit || (o instanceof ASN1OctetString)) {
            return getInstance(o);
        }
        return BEROctetString.fromSequence(ASN1Sequence.getInstance(o));
    }

    public static ASN1OctetString getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1OctetString)) {
            return (ASN1OctetString) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("failed to construct OCTET STRING from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Primitive primitive = ((ASN1Encodable) obj).toASN1Primitive();
            if (primitive instanceof ASN1OctetString) {
                return (ASN1OctetString) primitive;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("illegal object in getInstance: ");
        stringBuilder2.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public ASN1OctetString(byte[] string) {
        if (string != null) {
            this.string = string;
            return;
        }
        throw new NullPointerException("string cannot be null");
    }

    public InputStream getOctetStream() {
        return new ByteArrayInputStream(this.string);
    }

    public ASN1OctetStringParser parser() {
        return this;
    }

    public byte[] getOctets() {
        return this.string;
    }

    public int hashCode() {
        return Arrays.hashCode(getOctets());
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (!(o instanceof ASN1OctetString)) {
            return false;
        }
        return Arrays.areEqual(this.string, ((ASN1OctetString) o).string);
    }

    public ASN1Primitive getLoadedObject() {
        return toASN1Primitive();
    }

    ASN1Primitive toDERObject() {
        return new DEROctetString(this.string);
    }

    ASN1Primitive toDLObject() {
        return new DEROctetString(this.string);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("#");
        stringBuilder.append(Strings.fromByteArray(Hex.encode(this.string)));
        return stringBuilder.toString();
    }
}
