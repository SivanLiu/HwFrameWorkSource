package org.bouncycastle.asn1;

import java.io.IOException;

public abstract class ASN1Null extends ASN1Primitive {
    public static ASN1Null getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj instanceof ASN1Null) {
            return (ASN1Null) obj;
        }
        if (obj == null) {
            return null;
        }
        try {
            return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
        } catch (IOException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed to construct NULL from byte[]: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        } catch (ClassCastException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unknown object in getInstance(): ");
            stringBuilder.append(obj.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        return aSN1Primitive instanceof ASN1Null;
    }

    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public int hashCode() {
        return -1;
    }

    public String toString() {
        return "NULL";
    }
}
