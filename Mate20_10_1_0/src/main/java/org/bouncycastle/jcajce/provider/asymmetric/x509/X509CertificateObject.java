package org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.util.Date;
import java.util.Enumeration;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;

class X509CertificateObject extends X509CertificateImpl implements PKCS12BagAttributeCarrier {
    private PKCS12BagAttributeCarrier attrCarrier = new PKCS12BagAttributeCarrierImpl();
    private final Object cacheLock = new Object();
    private volatile int hashValue;
    private volatile boolean hashValueSet;
    private X509CertificateInternal internalCertificateValue;
    private X500Principal issuerValue;
    private PublicKey publicKeyValue;
    private X500Principal subjectValue;
    private long[] validityValues;

    X509CertificateObject(JcaJceHelper jcaJceHelper, Certificate certificate) throws CertificateParsingException {
        super(jcaJceHelper, certificate, createBasicConstraints(certificate), createKeyUsage(certificate));
    }

    private static BasicConstraints createBasicConstraints(Certificate certificate) throws CertificateParsingException {
        try {
            byte[] extensionOctets = getExtensionOctets(certificate, "2.5.29.19");
            if (extensionOctets == null) {
                return null;
            }
            return BasicConstraints.getInstance(ASN1Primitive.fromByteArray(extensionOctets));
        } catch (Exception e) {
            throw new CertificateParsingException("cannot construct BasicConstraints: " + e);
        }
    }

    private static boolean[] createKeyUsage(Certificate certificate) throws CertificateParsingException {
        try {
            byte[] extensionOctets = getExtensionOctets(certificate, "2.5.29.15");
            if (extensionOctets == null) {
                return null;
            }
            DERBitString instance = DERBitString.getInstance(ASN1Primitive.fromByteArray(extensionOctets));
            byte[] bytes = instance.getBytes();
            int length = (bytes.length * 8) - instance.getPadBits();
            int i = 9;
            if (length >= 9) {
                i = length;
            }
            boolean[] zArr = new boolean[i];
            for (int i2 = 0; i2 != length; i2++) {
                zArr[i2] = (bytes[i2 / 8] & (128 >>> (i2 % 8))) != 0;
            }
            return zArr;
        } catch (Exception e) {
            throw new CertificateParsingException("cannot construct KeyUsage: " + e);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:?, code lost:
        r0 = getEncoded();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0013, code lost:
        r0 = null;
     */
    private X509CertificateInternal getInternalCertificate() {
        byte[] bArr;
        X509CertificateInternal x509CertificateInternal;
        synchronized (this.cacheLock) {
            if (this.internalCertificateValue != null) {
                return this.internalCertificateValue;
            }
        }
        X509CertificateInternal x509CertificateInternal2 = new X509CertificateInternal(this.bcHelper, this.c, this.basicConstraints, this.keyUsage, bArr);
        synchronized (this.cacheLock) {
            if (this.internalCertificateValue == null) {
                this.internalCertificateValue = x509CertificateInternal2;
            }
            x509CertificateInternal = this.internalCertificateValue;
        }
        return x509CertificateInternal;
    }

    @Override // java.security.cert.X509Certificate, org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateImpl
    public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
        long time = date.getTime();
        long[] validityValues2 = getValidityValues();
        if (time > validityValues2[1]) {
            throw new CertificateExpiredException("certificate expired on " + this.c.getEndDate().getTime());
        } else if (time < validityValues2[0]) {
            throw new CertificateNotYetValidException("certificate not valid till " + this.c.getStartDate().getTime());
        }
    }

    public boolean equals(Object obj) {
        DERBitString signature;
        if (obj == this) {
            return true;
        }
        if (obj instanceof X509CertificateObject) {
            X509CertificateObject x509CertificateObject = (X509CertificateObject) obj;
            if (!this.hashValueSet || !x509CertificateObject.hashValueSet) {
                if ((this.internalCertificateValue == null || x509CertificateObject.internalCertificateValue == null) && (signature = this.c.getSignature()) != null && !signature.equals((ASN1Primitive) x509CertificateObject.c.getSignature())) {
                    return false;
                }
            } else if (this.hashValue != x509CertificateObject.hashValue) {
                return false;
            }
        }
        return getInternalCertificate().equals(obj);
    }

    @Override // org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier
    public ASN1Encodable getBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.attrCarrier.getBagAttribute(aSN1ObjectIdentifier);
    }

    @Override // org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier
    public Enumeration getBagAttributeKeys() {
        return this.attrCarrier.getBagAttributeKeys();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0012, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0015, code lost:
        if (r3.issuerValue != null) goto L_0x0019;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0017, code lost:
        r3.issuerValue = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0019, code lost:
        r0 = r3.issuerValue;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x001b, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x001c, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x000c, code lost:
        r0 = super.getIssuerX500Principal();
        r1 = r3.cacheLock;
     */
    @Override // org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateImpl
    public X500Principal getIssuerX500Principal() {
        synchronized (this.cacheLock) {
            if (this.issuerValue != null) {
                return this.issuerValue;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0010, code lost:
        if (r0 != null) goto L_0x0014;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0012, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0014, code lost:
        r1 = r3.cacheLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0016, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0019, code lost:
        if (r3.publicKeyValue != null) goto L_0x001d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x001b, code lost:
        r3.publicKeyValue = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x001d, code lost:
        r0 = r3.publicKeyValue;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x001f, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0020, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x000c, code lost:
        r0 = super.getPublicKey();
     */
    @Override // org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateImpl
    public PublicKey getPublicKey() {
        synchronized (this.cacheLock) {
            if (this.publicKeyValue != null) {
                return this.publicKeyValue;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0012, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0015, code lost:
        if (r3.subjectValue != null) goto L_0x0019;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0017, code lost:
        r3.subjectValue = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0019, code lost:
        r0 = r3.subjectValue;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x001b, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x001c, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x000c, code lost:
        r0 = super.getSubjectX500Principal();
        r1 = r3.cacheLock;
     */
    @Override // org.bouncycastle.jcajce.provider.asymmetric.x509.X509CertificateImpl
    public X500Principal getSubjectX500Principal() {
        synchronized (this.cacheLock) {
            if (this.subjectValue != null) {
                return this.subjectValue;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0027, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x002a, code lost:
        if (r4.validityValues != null) goto L_0x002e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x002c, code lost:
        r4.validityValues = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x002e, code lost:
        r0 = r4.validityValues;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0030, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0031, code lost:
        return r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x000c, code lost:
        r0 = new long[]{super.getNotBefore().getTime(), super.getNotAfter().getTime()};
        r1 = r4.cacheLock;
     */
    public long[] getValidityValues() {
        synchronized (this.cacheLock) {
            if (this.validityValues != null) {
                return this.validityValues;
            }
        }
    }

    public int hashCode() {
        if (!this.hashValueSet) {
            this.hashValue = getInternalCertificate().hashCode();
            this.hashValueSet = true;
        }
        return this.hashValue;
    }

    public int originalHashCode() {
        try {
            byte[] encoded = getInternalCertificate().getEncoded();
            int i = 0;
            for (int i2 = 1; i2 < encoded.length; i2++) {
                i += encoded[i2] * i2;
            }
            return i;
        } catch (CertificateEncodingException e) {
            return 0;
        }
    }

    @Override // org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier
    public void setBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        this.attrCarrier.setBagAttribute(aSN1ObjectIdentifier, aSN1Encodable);
    }
}
