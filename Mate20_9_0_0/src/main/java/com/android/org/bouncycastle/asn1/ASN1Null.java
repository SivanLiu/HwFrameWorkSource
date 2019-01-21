package com.android.org.bouncycastle.asn1;

import java.io.IOException;

public abstract class ASN1Null extends ASN1Primitive {
    abstract void encode(ASN1OutputStream aSN1OutputStream) throws IOException;

    public static ASN1Null getInstance(Object o) {
        StringBuilder stringBuilder;
        if (o instanceof ASN1Null) {
            return (ASN1Null) o;
        }
        if (o == null) {
            return null;
        }
        try {
            return getInstance(ASN1Primitive.fromByteArray((byte[]) o));
        } catch (IOException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed to construct NULL from byte[]: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        } catch (ClassCastException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unknown object in getInstance(): ");
            stringBuilder.append(o.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public int hashCode() {
        return -1;
    }

    boolean asn1Equals(ASN1Primitive o) {
        if (o instanceof ASN1Null) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "NULL";
    }
}
