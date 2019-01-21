package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Date;
import java.util.Enumeration;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class CertificateValidity implements CertAttrSet<String> {
    public static final String IDENT = "x509.info.validity";
    public static final String NAME = "validity";
    public static final String NOT_AFTER = "notAfter";
    public static final String NOT_BEFORE = "notBefore";
    private static final long YR_2050 = 2524636800000L;
    private Date notAfter;
    private Date notBefore;

    private Date getNotBefore() {
        return new Date(this.notBefore.getTime());
    }

    private Date getNotAfter() {
        return new Date(this.notAfter.getTime());
    }

    private void construct(DerValue derVal) throws IOException {
        if (derVal.tag != (byte) 48) {
            throw new IOException("Invalid encoded CertificateValidity, starting sequence tag missing.");
        } else if (derVal.data.available() != 0) {
            DerValue[] seq = new DerInputStream(derVal.toByteArray()).getSequence(2);
            if (seq.length == 2) {
                if (seq[0].tag == (byte) 23) {
                    this.notBefore = derVal.data.getUTCTime();
                } else if (seq[0].tag == (byte) 24) {
                    this.notBefore = derVal.data.getGeneralizedTime();
                } else {
                    throw new IOException("Invalid encoding for CertificateValidity");
                }
                if (seq[1].tag == (byte) 23) {
                    this.notAfter = derVal.data.getUTCTime();
                    return;
                } else if (seq[1].tag == (byte) 24) {
                    this.notAfter = derVal.data.getGeneralizedTime();
                    return;
                } else {
                    throw new IOException("Invalid encoding for CertificateValidity");
                }
            }
            throw new IOException("Invalid encoding for CertificateValidity");
        } else {
            throw new IOException("No data encoded for CertificateValidity");
        }
    }

    public CertificateValidity(Date notBefore, Date notAfter) {
        this.notBefore = notBefore;
        this.notAfter = notAfter;
    }

    public CertificateValidity(DerInputStream in) throws IOException {
        construct(in.getDerValue());
    }

    public String toString() {
        if (this.notBefore == null || this.notAfter == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Validity: [From: ");
        stringBuilder.append(this.notBefore.toString());
        stringBuilder.append(",\n               To: ");
        stringBuilder.append(this.notAfter.toString());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public void encode(OutputStream out) throws IOException {
        if (this.notBefore == null || this.notAfter == null) {
            throw new IOException("CertAttrSet:CertificateValidity: null values to encode.\n");
        }
        DerOutputStream pair = new DerOutputStream();
        if (this.notBefore.getTime() < YR_2050) {
            pair.putUTCTime(this.notBefore);
        } else {
            pair.putGeneralizedTime(this.notBefore);
        }
        if (this.notAfter.getTime() < YR_2050) {
            pair.putUTCTime(this.notAfter);
        } else {
            pair.putGeneralizedTime(this.notAfter);
        }
        DerOutputStream seq = new DerOutputStream();
        seq.write((byte) 48, pair);
        out.write(seq.toByteArray());
    }

    public void set(String name, Object obj) throws IOException {
        if (!(obj instanceof Date)) {
            throw new IOException("Attribute must be of type Date.");
        } else if (name.equalsIgnoreCase(NOT_BEFORE)) {
            this.notBefore = (Date) obj;
        } else if (name.equalsIgnoreCase(NOT_AFTER)) {
            this.notAfter = (Date) obj;
        } else {
            throw new IOException("Attribute name not recognized by CertAttrSet: CertificateValidity.");
        }
    }

    public Date get(String name) throws IOException {
        if (name.equalsIgnoreCase(NOT_BEFORE)) {
            return getNotBefore();
        }
        if (name.equalsIgnoreCase(NOT_AFTER)) {
            return getNotAfter();
        }
        throw new IOException("Attribute name not recognized by CertAttrSet: CertificateValidity.");
    }

    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(NOT_BEFORE)) {
            this.notBefore = null;
        } else if (name.equalsIgnoreCase(NOT_AFTER)) {
            this.notAfter = null;
        } else {
            throw new IOException("Attribute name not recognized by CertAttrSet: CertificateValidity.");
        }
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(NOT_BEFORE);
        elements.addElement(NOT_AFTER);
        return elements.elements();
    }

    public String getName() {
        return "validity";
    }

    public void valid() throws CertificateNotYetValidException, CertificateExpiredException {
        valid(new Date());
    }

    public void valid(Date now) throws CertificateNotYetValidException, CertificateExpiredException {
        StringBuilder stringBuilder;
        if (this.notBefore.after(now)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("NotBefore: ");
            stringBuilder.append(this.notBefore.toString());
            throw new CertificateNotYetValidException(stringBuilder.toString());
        } else if (this.notAfter.before(now)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("NotAfter: ");
            stringBuilder.append(this.notAfter.toString());
            throw new CertificateExpiredException(stringBuilder.toString());
        }
    }
}
