package com.android.org.bouncycastle.asn1.x509;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.DEROctetString;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class X509ExtensionsGenerator {
    private Vector extOrdering = new Vector();
    private Hashtable extensions = new Hashtable();

    public void reset() {
        this.extensions = new Hashtable();
        this.extOrdering = new Vector();
    }

    public void addExtension(ASN1ObjectIdentifier oid, boolean critical, ASN1Encodable value) {
        try {
            addExtension(oid, critical, value.toASN1Primitive().getEncoded(ASN1Encoding.DER));
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error encoding value: ");
            stringBuilder.append(e);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void addExtension(ASN1ObjectIdentifier oid, boolean critical, byte[] value) {
        if (this.extensions.containsKey(oid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("extension ");
            stringBuilder.append(oid);
            stringBuilder.append(" already added");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.extOrdering.addElement(oid);
        this.extensions.put(oid, new X509Extension(critical, new DEROctetString(value)));
    }

    public boolean isEmpty() {
        return this.extOrdering.isEmpty();
    }

    public X509Extensions generate() {
        return new X509Extensions(this.extOrdering, this.extensions);
    }
}
