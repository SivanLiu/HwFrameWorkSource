package org.bouncycastle.asn1.x509;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;

public class ExtensionsGenerator {
    private Vector extOrdering = new Vector();
    private Hashtable extensions = new Hashtable();

    public void addExtension(ASN1ObjectIdentifier aSN1ObjectIdentifier, boolean z, ASN1Encodable aSN1Encodable) throws IOException {
        addExtension(aSN1ObjectIdentifier, z, aSN1Encodable.toASN1Primitive().getEncoded(ASN1Encoding.DER));
    }

    public void addExtension(ASN1ObjectIdentifier aSN1ObjectIdentifier, boolean z, byte[] bArr) {
        if (this.extensions.containsKey(aSN1ObjectIdentifier)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("extension ");
            stringBuilder.append(aSN1ObjectIdentifier);
            stringBuilder.append(" already added");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.extOrdering.addElement(aSN1ObjectIdentifier);
        this.extensions.put(aSN1ObjectIdentifier, new Extension(aSN1ObjectIdentifier, z, new DEROctetString(bArr)));
    }

    public void addExtension(Extension extension) {
        if (this.extensions.containsKey(extension.getExtnId())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("extension ");
            stringBuilder.append(extension.getExtnId());
            stringBuilder.append(" already added");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.extOrdering.addElement(extension.getExtnId());
        this.extensions.put(extension.getExtnId(), extension);
    }

    public Extensions generate() {
        Extension[] extensionArr = new Extension[this.extOrdering.size()];
        for (int i = 0; i != this.extOrdering.size(); i++) {
            extensionArr[i] = (Extension) this.extensions.get(this.extOrdering.elementAt(i));
        }
        return new Extensions(extensionArr);
    }

    public boolean isEmpty() {
        return this.extOrdering.isEmpty();
    }

    public void reset() {
        this.extensions = new Hashtable();
        this.extOrdering = new Vector();
    }
}
