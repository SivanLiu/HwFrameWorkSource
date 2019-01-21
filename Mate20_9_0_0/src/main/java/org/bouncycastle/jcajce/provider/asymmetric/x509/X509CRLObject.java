package org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.TBSCertList.CRLEntry;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

class X509CRLObject extends X509CRL {
    private JcaJceHelper bcHelper;
    private CertificateList c;
    private int hashCodeValue;
    private boolean isHashCodeSet = false;
    private boolean isIndirect;
    private String sigAlgName;
    private byte[] sigAlgParams;

    protected X509CRLObject(JcaJceHelper jcaJceHelper, CertificateList certificateList) throws CRLException {
        this.bcHelper = jcaJceHelper;
        this.c = certificateList;
        try {
            this.sigAlgName = X509SignatureUtil.getSignatureName(certificateList.getSignatureAlgorithm());
            if (certificateList.getSignatureAlgorithm().getParameters() != null) {
                this.sigAlgParams = certificateList.getSignatureAlgorithm().getParameters().toASN1Primitive().getEncoded(ASN1Encoding.DER);
            } else {
                this.sigAlgParams = null;
            }
            this.isIndirect = isIndirectCRL(this);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CRL contents invalid: ");
            stringBuilder.append(e);
            throw new CRLException(stringBuilder.toString());
        }
    }

    private void doVerify(PublicKey publicKey, Signature signature) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if (this.c.getSignatureAlgorithm().equals(this.c.getTBSCertList().getSignature())) {
            signature.initVerify(publicKey);
            signature.update(getTBSCertList());
            if (!signature.verify(getSignature())) {
                throw new SignatureException("CRL does not verify with supplied public key.");
            }
            return;
        }
        throw new CRLException("Signature algorithm on CertificateList does not match TBSCertList.");
    }

    private Set getExtensionOIDs(boolean z) {
        if (getVersion() == 2) {
            Extensions extensions = this.c.getTBSCertList().getExtensions();
            if (extensions != null) {
                HashSet hashSet = new HashSet();
                Enumeration oids = extensions.oids();
                while (oids.hasMoreElements()) {
                    ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) oids.nextElement();
                    if (z == extensions.getExtension(aSN1ObjectIdentifier).isCritical()) {
                        hashSet.add(aSN1ObjectIdentifier.getId());
                    }
                }
                return hashSet;
            }
        }
        return null;
    }

    static boolean isIndirectCRL(X509CRL x509crl) throws CRLException {
        try {
            byte[] extensionValue = x509crl.getExtensionValue(Extension.issuingDistributionPoint.getId());
            return extensionValue != null && IssuingDistributionPoint.getInstance(ASN1OctetString.getInstance(extensionValue).getOctets()).isIndirectCRL();
        } catch (Exception e) {
            throw new ExtCRLException("Exception reading IssuingDistributionPoint", e);
        }
    }

    private Set loadCRLEntries() {
        HashSet hashSet = new HashSet();
        Enumeration revokedCertificateEnumeration = this.c.getRevokedCertificateEnumeration();
        X500Name x500Name = null;
        while (revokedCertificateEnumeration.hasMoreElements()) {
            CRLEntry cRLEntry = (CRLEntry) revokedCertificateEnumeration.nextElement();
            hashSet.add(new X509CRLEntryObject(cRLEntry, this.isIndirect, x500Name));
            if (this.isIndirect && cRLEntry.hasExtensions()) {
                Extension extension = cRLEntry.getExtensions().getExtension(Extension.certificateIssuer);
                if (extension != null) {
                    x500Name = X500Name.getInstance(GeneralNames.getInstance(extension.getParsedValue()).getNames()[0].getName());
                }
            }
        }
        return hashSet;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof X509CRL)) {
            return false;
        }
        if (!(obj instanceof X509CRLObject)) {
            return super.equals(obj);
        }
        X509CRLObject x509CRLObject = (X509CRLObject) obj;
        return (this.isHashCodeSet && x509CRLObject.isHashCodeSet && x509CRLObject.hashCodeValue != this.hashCodeValue) ? false : this.c.equals(x509CRLObject.c);
    }

    public Set getCriticalExtensionOIDs() {
        return getExtensionOIDs(true);
    }

    public byte[] getEncoded() throws CRLException {
        try {
            return this.c.getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            throw new CRLException(e.toString());
        }
    }

    public byte[] getExtensionValue(String str) {
        Extensions extensions = this.c.getTBSCertList().getExtensions();
        if (extensions != null) {
            Extension extension = extensions.getExtension(new ASN1ObjectIdentifier(str));
            if (extension != null) {
                try {
                    return extension.getExtnValue().getEncoded();
                } catch (Exception e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("error parsing ");
                    stringBuilder.append(e.toString());
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
        }
        return null;
    }

    public Principal getIssuerDN() {
        return new X509Principal(X500Name.getInstance(this.c.getIssuer().toASN1Primitive()));
    }

    public X500Principal getIssuerX500Principal() {
        try {
            return new X500Principal(this.c.getIssuer().getEncoded());
        } catch (IOException e) {
            throw new IllegalStateException("can't encode issuer DN");
        }
    }

    public Date getNextUpdate() {
        return this.c.getNextUpdate() != null ? this.c.getNextUpdate().getDate() : null;
    }

    public Set getNonCriticalExtensionOIDs() {
        return getExtensionOIDs(false);
    }

    public X509CRLEntry getRevokedCertificate(BigInteger bigInteger) {
        Enumeration revokedCertificateEnumeration = this.c.getRevokedCertificateEnumeration();
        X500Name x500Name = null;
        while (revokedCertificateEnumeration.hasMoreElements()) {
            CRLEntry cRLEntry = (CRLEntry) revokedCertificateEnumeration.nextElement();
            if (bigInteger.equals(cRLEntry.getUserCertificate().getValue())) {
                return new X509CRLEntryObject(cRLEntry, this.isIndirect, x500Name);
            }
            if (this.isIndirect && cRLEntry.hasExtensions()) {
                Extension extension = cRLEntry.getExtensions().getExtension(Extension.certificateIssuer);
                if (extension != null) {
                    x500Name = X500Name.getInstance(GeneralNames.getInstance(extension.getParsedValue()).getNames()[0].getName());
                }
            }
        }
        return null;
    }

    public Set getRevokedCertificates() {
        Set loadCRLEntries = loadCRLEntries();
        return !loadCRLEntries.isEmpty() ? Collections.unmodifiableSet(loadCRLEntries) : null;
    }

    public String getSigAlgName() {
        return this.sigAlgName;
    }

    public String getSigAlgOID() {
        return this.c.getSignatureAlgorithm().getAlgorithm().getId();
    }

    public byte[] getSigAlgParams() {
        if (this.sigAlgParams == null) {
            return null;
        }
        byte[] bArr = new byte[this.sigAlgParams.length];
        System.arraycopy(this.sigAlgParams, 0, bArr, 0, bArr.length);
        return bArr;
    }

    public byte[] getSignature() {
        return this.c.getSignature().getOctets();
    }

    public byte[] getTBSCertList() throws CRLException {
        try {
            return this.c.getTBSCertList().getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            throw new CRLException(e.toString());
        }
    }

    public Date getThisUpdate() {
        return this.c.getThisUpdate().getDate();
    }

    public int getVersion() {
        return this.c.getVersionNumber();
    }

    public boolean hasUnsupportedCriticalExtension() {
        Set criticalExtensionOIDs = getCriticalExtensionOIDs();
        if (criticalExtensionOIDs == null) {
            return false;
        }
        criticalExtensionOIDs.remove(Extension.issuingDistributionPoint.getId());
        criticalExtensionOIDs.remove(Extension.deltaCRLIndicator.getId());
        return criticalExtensionOIDs.isEmpty() ^ 1;
    }

    public int hashCode() {
        if (!this.isHashCodeSet) {
            this.isHashCodeSet = true;
            this.hashCodeValue = super.hashCode();
        }
        return this.hashCodeValue;
    }

    public boolean isRevoked(Certificate certificate) {
        if (certificate.getType().equals("X.509")) {
            Enumeration revokedCertificateEnumeration = this.c.getRevokedCertificateEnumeration();
            X500Name issuer = this.c.getIssuer();
            if (revokedCertificateEnumeration.hasMoreElements()) {
                X509Certificate x509Certificate = (X509Certificate) certificate;
                BigInteger serialNumber = x509Certificate.getSerialNumber();
                while (revokedCertificateEnumeration.hasMoreElements()) {
                    CRLEntry instance = CRLEntry.getInstance(revokedCertificateEnumeration.nextElement());
                    if (this.isIndirect && instance.hasExtensions()) {
                        Extension extension = instance.getExtensions().getExtension(Extension.certificateIssuer);
                        if (extension != null) {
                            issuer = X500Name.getInstance(GeneralNames.getInstance(extension.getParsedValue()).getNames()[0].getName());
                        }
                    }
                    if (instance.getUserCertificate().getValue().equals(serialNumber)) {
                        Object instance2;
                        if (certificate instanceof X509Certificate) {
                            instance2 = X500Name.getInstance(x509Certificate.getIssuerX500Principal().getEncoded());
                        } else {
                            try {
                                instance2 = org.bouncycastle.asn1.x509.Certificate.getInstance(certificate.getEncoded()).getIssuer();
                            } catch (CertificateEncodingException e) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Cannot process certificate: ");
                                stringBuilder.append(e.getMessage());
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                        }
                        return issuer.equals(instance2);
                    }
                }
            }
            return false;
        }
        throw new IllegalArgumentException("X.509 CRL used with non X.509 Cert");
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x00c4  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        String lineSeparator = Strings.lineSeparator();
        stringBuffer.append("              Version: ");
        stringBuffer.append(getVersion());
        stringBuffer.append(lineSeparator);
        stringBuffer.append("             IssuerDN: ");
        stringBuffer.append(getIssuerDN());
        stringBuffer.append(lineSeparator);
        stringBuffer.append("          This update: ");
        stringBuffer.append(getThisUpdate());
        stringBuffer.append(lineSeparator);
        stringBuffer.append("          Next update: ");
        stringBuffer.append(getNextUpdate());
        stringBuffer.append(lineSeparator);
        stringBuffer.append("  Signature Algorithm: ");
        stringBuffer.append(getSigAlgName());
        stringBuffer.append(lineSeparator);
        byte[] signature = getSignature();
        stringBuffer.append("            Signature: ");
        stringBuffer.append(new String(Hex.encode(signature, 0, 20)));
        stringBuffer.append(lineSeparator);
        for (int i = 20; i < signature.length; i += 20) {
            String str;
            if (i < signature.length - 20) {
                stringBuffer.append("                       ");
                str = new String(Hex.encode(signature, i, 20));
            } else {
                stringBuffer.append("                       ");
                str = new String(Hex.encode(signature, i, signature.length - i));
            }
            stringBuffer.append(str);
            stringBuffer.append(lineSeparator);
        }
        Extensions extensions = this.c.getTBSCertList().getExtensions();
        if (extensions != null) {
            String str2;
            Enumeration oids = extensions.oids();
            if (oids.hasMoreElements()) {
                str2 = "           Extensions: ";
                stringBuffer.append(str2);
                stringBuffer.append(lineSeparator);
            }
            while (oids.hasMoreElements()) {
                ASN1InputStream aSN1InputStream;
                ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) oids.nextElement();
                Extension extension = extensions.getExtension(aSN1ObjectIdentifier);
                if (extension.getExtnValue() != null) {
                    aSN1InputStream = new ASN1InputStream(extension.getExtnValue().getOctets());
                    stringBuffer.append("                       critical(");
                    stringBuffer.append(extension.isCritical());
                    stringBuffer.append(") ");
                } else {
                    stringBuffer.append(lineSeparator);
                    while (oids.hasMoreElements()) {
                    }
                }
                aSN1InputStream = new ASN1InputStream(extension.getExtnValue().getOctets());
                stringBuffer.append("                       critical(");
                stringBuffer.append(extension.isCritical());
                stringBuffer.append(") ");
                try {
                } catch (Exception e) {
                    stringBuffer.append(aSN1ObjectIdentifier.getId());
                    stringBuffer.append(" value = ");
                    str2 = "*****";
                }
                if (aSN1ObjectIdentifier.equals(Extension.cRLNumber)) {
                    stringBuffer.append(new CRLNumber(ASN1Integer.getInstance(aSN1InputStream.readObject()).getPositiveValue()));
                } else if (aSN1ObjectIdentifier.equals(Extension.deltaCRLIndicator)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Base CRL: ");
                    stringBuilder.append(new CRLNumber(ASN1Integer.getInstance(aSN1InputStream.readObject()).getPositiveValue()));
                    stringBuffer.append(stringBuilder.toString());
                } else if (aSN1ObjectIdentifier.equals(Extension.issuingDistributionPoint)) {
                    stringBuffer.append(IssuingDistributionPoint.getInstance(aSN1InputStream.readObject()));
                } else if (aSN1ObjectIdentifier.equals(Extension.cRLDistributionPoints)) {
                    stringBuffer.append(CRLDistPoint.getInstance(aSN1InputStream.readObject()));
                } else if (aSN1ObjectIdentifier.equals(Extension.freshestCRL)) {
                    stringBuffer.append(CRLDistPoint.getInstance(aSN1InputStream.readObject()));
                } else {
                    stringBuffer.append(aSN1ObjectIdentifier.getId());
                    stringBuffer.append(" value = ");
                    stringBuffer.append(ASN1Dump.dumpAsString(aSN1InputStream.readObject()));
                }
                stringBuffer.append(lineSeparator);
            }
        }
        Set<Object> revokedCertificates = getRevokedCertificates();
        if (revokedCertificates != null) {
            for (Object append : revokedCertificates) {
                stringBuffer.append(append);
                stringBuffer.append(lineSeparator);
            }
        }
        return stringBuffer.toString();
    }

    public void verify(PublicKey publicKey) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        Signature createSignature;
        try {
            createSignature = this.bcHelper.createSignature(getSigAlgName());
        } catch (Exception e) {
            createSignature = Signature.getInstance(getSigAlgName());
        }
        doVerify(publicKey, createSignature);
    }

    public void verify(PublicKey publicKey, String str) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        doVerify(publicKey, str != null ? Signature.getInstance(getSigAlgName(), str) : Signature.getInstance(getSigAlgName()));
    }

    public void verify(PublicKey publicKey, Provider provider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        doVerify(publicKey, provider != null ? Signature.getInstance(getSigAlgName(), provider) : Signature.getInstance(getSigAlgName()));
    }
}
