package org.bouncycastle.cert;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.TBSCertList.CRLEntry;

public class X509CRLEntryHolder {
    private GeneralNames ca;
    private CRLEntry entry;

    X509CRLEntryHolder(CRLEntry cRLEntry, boolean z, GeneralNames generalNames) {
        this.entry = cRLEntry;
        this.ca = generalNames;
        if (z && cRLEntry.hasExtensions()) {
            Extension extension = cRLEntry.getExtensions().getExtension(Extension.certificateIssuer);
            if (extension != null) {
                this.ca = GeneralNames.getInstance(extension.getParsedValue());
            }
        }
    }

    public GeneralNames getCertificateIssuer() {
        return this.ca;
    }

    public Set getCriticalExtensionOIDs() {
        return CertUtils.getCriticalExtensionOIDs(this.entry.getExtensions());
    }

    public Extension getExtension(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        Extensions extensions = this.entry.getExtensions();
        return extensions != null ? extensions.getExtension(aSN1ObjectIdentifier) : null;
    }

    public List getExtensionOIDs() {
        return CertUtils.getExtensionOIDs(this.entry.getExtensions());
    }

    public Extensions getExtensions() {
        return this.entry.getExtensions();
    }

    public Set getNonCriticalExtensionOIDs() {
        return CertUtils.getNonCriticalExtensionOIDs(this.entry.getExtensions());
    }

    public Date getRevocationDate() {
        return this.entry.getRevocationDate().getDate();
    }

    public BigInteger getSerialNumber() {
        return this.entry.getUserCertificate().getValue();
    }

    public boolean hasExtensions() {
        return this.entry.hasExtensions();
    }
}
