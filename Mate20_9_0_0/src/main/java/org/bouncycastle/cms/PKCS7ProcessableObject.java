package org.bouncycastle.cms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.eac.CertificateBody;

public class PKCS7ProcessableObject implements CMSTypedData {
    private final ASN1Encodable structure;
    private final ASN1ObjectIdentifier type;

    public PKCS7ProcessableObject(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        this.type = aSN1ObjectIdentifier;
        this.structure = aSN1Encodable;
    }

    public Object getContent() {
        return this.structure;
    }

    public ASN1ObjectIdentifier getContentType() {
        return this.type;
    }

    public void write(OutputStream outputStream) throws IOException, CMSException {
        if (this.structure instanceof ASN1Sequence) {
            Iterator it = ASN1Sequence.getInstance(this.structure).iterator();
            while (it.hasNext()) {
                outputStream.write(((ASN1Encodable) it.next()).toASN1Primitive().getEncoded(ASN1Encoding.DER));
            }
            return;
        }
        byte[] encoded = this.structure.toASN1Primitive().getEncoded(ASN1Encoding.DER);
        int i = 1;
        while ((encoded[i] & 255) > CertificateBody.profileType) {
            i++;
        }
        i++;
        outputStream.write(encoded, i, encoded.length - i);
    }
}
