package org.bouncycastle.x509.extension;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.util.Integers;

public class X509ExtensionUtil {
    public static ASN1Primitive fromExtensionValue(byte[] bArr) throws IOException {
        return ASN1Primitive.fromByteArray(((ASN1OctetString) ASN1Primitive.fromByteArray(bArr)).getOctets());
    }

    private static Collection getAlternativeNames(byte[] bArr) throws CertificateParsingException {
        if (bArr == null) {
            return Collections.EMPTY_LIST;
        }
        try {
            Collection arrayList = new ArrayList();
            Enumeration objects = ASN1Sequence.getInstance(fromExtensionValue(bArr)).getObjects();
            while (objects.hasMoreElements()) {
                Object toASN1Primitive;
                GeneralName instance = GeneralName.getInstance(objects.nextElement());
                List arrayList2 = new ArrayList();
                arrayList2.add(Integers.valueOf(instance.getTagNo()));
                switch (instance.getTagNo()) {
                    case 0:
                    case 3:
                    case 5:
                        toASN1Primitive = instance.getName().toASN1Primitive();
                        break;
                    case 1:
                    case 2:
                    case 6:
                        toASN1Primitive = ((ASN1String) instance.getName()).getString();
                        break;
                    case 4:
                        toASN1Primitive = X500Name.getInstance(instance.getName()).toString();
                        break;
                    case 7:
                        toASN1Primitive = ASN1OctetString.getInstance(instance.getName()).getOctets();
                        break;
                    case 8:
                        toASN1Primitive = ASN1ObjectIdentifier.getInstance(instance.getName()).getId();
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad tag number: ");
                        stringBuilder.append(instance.getTagNo());
                        throw new IOException(stringBuilder.toString());
                }
                arrayList2.add(toASN1Primitive);
                arrayList.add(arrayList2);
            }
            return Collections.unmodifiableCollection(arrayList);
        } catch (Exception e) {
            throw new CertificateParsingException(e.getMessage());
        }
    }

    public static Collection getIssuerAlternativeNames(X509Certificate x509Certificate) throws CertificateParsingException {
        return getAlternativeNames(x509Certificate.getExtensionValue(X509Extension.issuerAlternativeName.getId()));
    }

    public static Collection getSubjectAlternativeNames(X509Certificate x509Certificate) throws CertificateParsingException {
        return getAlternativeNames(x509Certificate.getExtensionValue(X509Extension.subjectAlternativeName.getId()));
    }
}
