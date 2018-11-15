package org.bouncycastle.asn1;

import java.util.Enumeration;

class LazyConstructionEnumeration implements Enumeration {
    private ASN1InputStream aIn;
    private Object nextObj = readObject();

    public LazyConstructionEnumeration(byte[] bArr) {
        this.aIn = new ASN1InputStream(bArr, true);
    }

    private Object readObject() {
        try {
            return this.aIn.readObject();
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("malformed DER construction: ");
            stringBuilder.append(e);
            throw new ASN1ParsingException(stringBuilder.toString(), e);
        }
    }

    public boolean hasMoreElements() {
        return this.nextObj != null;
    }

    public Object nextElement() {
        Object obj = this.nextObj;
        this.nextObj = readObject();
        return obj;
    }
}
