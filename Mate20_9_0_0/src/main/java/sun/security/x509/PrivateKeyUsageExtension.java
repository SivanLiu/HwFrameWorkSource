package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Objects;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class PrivateKeyUsageExtension extends Extension implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.extensions.PrivateKeyUsage";
    public static final String NAME = "PrivateKeyUsage";
    public static final String NOT_AFTER = "not_after";
    public static final String NOT_BEFORE = "not_before";
    private static final byte TAG_AFTER = (byte) 1;
    private static final byte TAG_BEFORE = (byte) 0;
    private Date notAfter;
    private Date notBefore;

    private void encodeThis() throws IOException {
        if (this.notBefore == null && this.notAfter == null) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream tmp;
        DerOutputStream seq = new DerOutputStream();
        DerOutputStream tagged = new DerOutputStream();
        if (this.notBefore != null) {
            tmp = new DerOutputStream();
            tmp.putGeneralizedTime(this.notBefore);
            tagged.writeImplicit(DerValue.createTag(Byte.MIN_VALUE, false, (byte) 0), tmp);
        }
        if (this.notAfter != null) {
            tmp = new DerOutputStream();
            tmp.putGeneralizedTime(this.notAfter);
            tagged.writeImplicit(DerValue.createTag(Byte.MIN_VALUE, false, (byte) 1), tmp);
        }
        seq.write((byte) 48, tagged);
        this.extensionValue = seq.toByteArray();
    }

    public PrivateKeyUsageExtension(Date notBefore, Date notAfter) throws IOException {
        this.notBefore = null;
        this.notAfter = null;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.extensionId = PKIXExtensions.PrivateKeyUsage_Id;
        this.critical = false;
        encodeThis();
    }

    public PrivateKeyUsageExtension(Boolean critical, Object value) throws CertificateException, IOException {
        this.notBefore = null;
        this.notAfter = null;
        this.extensionId = PKIXExtensions.PrivateKeyUsage_Id;
        this.critical = critical.booleanValue();
        this.extensionValue = (byte[]) value;
        DerInputStream str = new DerInputStream(this.extensionValue);
        DerValue[] seq = str.getSequence(2);
        for (DerValue opt : seq) {
            if (!opt.isContextSpecific((byte) 0) || opt.isConstructed()) {
                if (!opt.isContextSpecific((byte) 1) || opt.isConstructed()) {
                    throw new IOException("Invalid encoding of PrivateKeyUsageExtension");
                } else if (this.notAfter == null) {
                    opt.resetTag((byte) 24);
                    this.notAfter = new DerInputStream(opt.toByteArray()).getGeneralizedTime();
                } else {
                    throw new CertificateParsingException("Duplicate notAfter in PrivateKeyUsage.");
                }
            } else if (this.notBefore == null) {
                opt.resetTag((byte) 24);
                this.notBefore = new DerInputStream(opt.toByteArray()).getGeneralizedTime();
            } else {
                throw new CertificateParsingException("Duplicate notBefore in PrivateKeyUsage.");
            }
        }
    }

    public String toString() {
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(super.toString());
        stringBuilder2.append("PrivateKeyUsage: [\n");
        if (this.notBefore == null) {
            str = "";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("From: ");
            stringBuilder.append(this.notBefore.toString());
            stringBuilder.append(", ");
            str = stringBuilder.toString();
        }
        stringBuilder2.append(str);
        if (this.notAfter == null) {
            str = "";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("To: ");
            stringBuilder.append(this.notAfter.toString());
            str = stringBuilder.toString();
        }
        stringBuilder2.append(str);
        stringBuilder2.append("]\n");
        return stringBuilder2.toString();
    }

    public void valid() throws CertificateNotYetValidException, CertificateExpiredException {
        valid(new Date());
    }

    public void valid(Date now) throws CertificateNotYetValidException, CertificateExpiredException {
        Objects.requireNonNull(now);
        StringBuilder stringBuilder;
        if (this.notBefore != null && this.notBefore.after(now)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("NotBefore: ");
            stringBuilder.append(this.notBefore.toString());
            throw new CertificateNotYetValidException(stringBuilder.toString());
        } else if (this.notAfter != null && this.notAfter.before(now)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("NotAfter: ");
            stringBuilder.append(this.notAfter.toString());
            throw new CertificateExpiredException(stringBuilder.toString());
        }
    }

    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.PrivateKeyUsage_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    public void set(String name, Object obj) throws CertificateException, IOException {
        if (obj instanceof Date) {
            if (name.equalsIgnoreCase(NOT_BEFORE)) {
                this.notBefore = (Date) obj;
            } else if (name.equalsIgnoreCase(NOT_AFTER)) {
                this.notAfter = (Date) obj;
            } else {
                throw new CertificateException("Attribute name not recognized by CertAttrSet:PrivateKeyUsage.");
            }
            encodeThis();
            return;
        }
        throw new CertificateException("Attribute must be of type Date.");
    }

    public Date get(String name) throws CertificateException {
        if (name.equalsIgnoreCase(NOT_BEFORE)) {
            return new Date(this.notBefore.getTime());
        }
        if (name.equalsIgnoreCase(NOT_AFTER)) {
            return new Date(this.notAfter.getTime());
        }
        throw new CertificateException("Attribute name not recognized by CertAttrSet:PrivateKeyUsage.");
    }

    public void delete(String name) throws CertificateException, IOException {
        if (name.equalsIgnoreCase(NOT_BEFORE)) {
            this.notBefore = null;
        } else if (name.equalsIgnoreCase(NOT_AFTER)) {
            this.notAfter = null;
        } else {
            throw new CertificateException("Attribute name not recognized by CertAttrSet:PrivateKeyUsage.");
        }
        encodeThis();
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(NOT_BEFORE);
        elements.addElement(NOT_AFTER);
        return elements.elements();
    }

    public String getName() {
        return NAME;
    }
}
