package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.x509.CRLNumberExtension;
import sun.security.x509.X500Name;

public class X509CRLSelector implements CRLSelector {
    private static final Debug debug = Debug.getInstance("certpath");
    private X509Certificate certChecking;
    private Date dateAndTime;
    private HashSet<Object> issuerNames;
    private HashSet<X500Principal> issuerX500Principals;
    private BigInteger maxCRL;
    private BigInteger minCRL;
    private long skew = 0;

    static {
        CertPathHelperImpl.initialize();
    }

    public void setIssuers(Collection<X500Principal> issuers) {
        if (issuers == null || issuers.isEmpty()) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
            return;
        }
        this.issuerX500Principals = new HashSet((Collection) issuers);
        this.issuerNames = new HashSet();
        Iterator it = this.issuerX500Principals.iterator();
        while (it.hasNext()) {
            this.issuerNames.add(((X500Principal) it.next()).getEncoded());
        }
    }

    public void setIssuerNames(Collection<?> names) throws IOException {
        if (names == null || names.size() == 0) {
            this.issuerNames = null;
            this.issuerX500Principals = null;
            return;
        }
        HashSet<Object> tempNames = cloneAndCheckIssuerNames(names);
        this.issuerX500Principals = parseIssuerNames(tempNames);
        this.issuerNames = tempNames;
    }

    public void addIssuer(X500Principal issuer) {
        addIssuerNameInternal(issuer.getEncoded(), issuer);
    }

    public void addIssuerName(String name) throws IOException {
        addIssuerNameInternal(name, new X500Name(name).asX500Principal());
    }

    public void addIssuerName(byte[] name) throws IOException {
        addIssuerNameInternal(name.clone(), new X500Name(name).asX500Principal());
    }

    private void addIssuerNameInternal(Object name, X500Principal principal) {
        if (this.issuerNames == null) {
            this.issuerNames = new HashSet();
        }
        if (this.issuerX500Principals == null) {
            this.issuerX500Principals = new HashSet();
        }
        this.issuerNames.add(name);
        this.issuerX500Principals.add(principal);
    }

    private static HashSet<Object> cloneAndCheckIssuerNames(Collection<?> names) throws IOException {
        HashSet<Object> namesCopy = new HashSet();
        for (Object nameObject : names) {
            if (!(nameObject instanceof byte[]) && !(nameObject instanceof String)) {
                throw new IOException("name not byte array or String");
            } else if (nameObject instanceof byte[]) {
                namesCopy.add(((byte[]) nameObject).clone());
            } else {
                namesCopy.add(nameObject);
            }
        }
        return namesCopy;
    }

    private static HashSet<Object> cloneIssuerNames(Collection<Object> names) {
        try {
            return cloneAndCheckIssuerNames(names);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static HashSet<X500Principal> parseIssuerNames(Collection<Object> names) throws IOException {
        HashSet<X500Principal> x500Principals = new HashSet();
        for (Object nameObject : names) {
            if (nameObject instanceof String) {
                x500Principals.add(new X500Name((String) nameObject).asX500Principal());
            } else {
                try {
                    x500Principals.add(new X500Principal((byte[]) nameObject));
                } catch (IllegalArgumentException e) {
                    throw ((IOException) new IOException("Invalid name").initCause(e));
                }
            }
        }
        return x500Principals;
    }

    public void setMinCRLNumber(BigInteger minCRL) {
        this.minCRL = minCRL;
    }

    public void setMaxCRLNumber(BigInteger maxCRL) {
        this.maxCRL = maxCRL;
    }

    public void setDateAndTime(Date dateAndTime) {
        if (dateAndTime == null) {
            this.dateAndTime = null;
        } else {
            this.dateAndTime = new Date(dateAndTime.getTime());
        }
        this.skew = 0;
    }

    void setDateAndTime(Date dateAndTime, long skew) {
        this.dateAndTime = dateAndTime == null ? null : new Date(dateAndTime.getTime());
        this.skew = skew;
    }

    public void setCertificateChecking(X509Certificate cert) {
        this.certChecking = cert;
    }

    public Collection<X500Principal> getIssuers() {
        if (this.issuerX500Principals == null) {
            return null;
        }
        return Collections.unmodifiableCollection(this.issuerX500Principals);
    }

    public Collection<Object> getIssuerNames() {
        if (this.issuerNames == null) {
            return null;
        }
        return cloneIssuerNames(this.issuerNames);
    }

    public BigInteger getMinCRL() {
        return this.minCRL;
    }

    public BigInteger getMaxCRL() {
        return this.maxCRL;
    }

    public Date getDateAndTime() {
        if (this.dateAndTime == null) {
            return null;
        }
        return (Date) this.dateAndTime.clone();
    }

    public X509Certificate getCertificateChecking() {
        return this.certChecking;
    }

    public String toString() {
        StringBuilder stringBuilder;
        StringBuffer sb = new StringBuffer();
        sb.append("X509CRLSelector: [\n");
        if (this.issuerNames != null) {
            sb.append("  IssuerNames:\n");
            Iterator<Object> i = this.issuerNames.iterator();
            while (i.hasNext()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    ");
                stringBuilder2.append(i.next());
                stringBuilder2.append("\n");
                sb.append(stringBuilder2.toString());
            }
        }
        if (this.minCRL != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  minCRLNumber: ");
            stringBuilder.append(this.minCRL);
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.maxCRL != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  maxCRLNumber: ");
            stringBuilder.append(this.maxCRL);
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.dateAndTime != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  dateAndTime: ");
            stringBuilder.append(this.dateAndTime);
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.certChecking != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Certificate being checked: ");
            stringBuilder.append(this.certChecking);
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean match(CRL crl) {
        if (!(crl instanceof X509CRL)) {
            return false;
        }
        byte[] crlNumExtVal;
        X509CRL xcrl = (X509CRL) crl;
        if (this.issuerNames != null) {
            X500Principal issuer = xcrl.getIssuerX500Principal();
            Iterator<X500Principal> i = this.issuerX500Principals.iterator();
            boolean found = false;
            while (!found && i.hasNext()) {
                if (((X500Principal) i.next()).equals(issuer)) {
                    found = true;
                }
            }
            if (!found) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: issuer DNs don't match");
                }
                return false;
            }
        }
        if (!(this.minCRL == null && this.maxCRL == null)) {
            crlNumExtVal = xcrl.getExtensionValue("2.5.29.20");
            if (crlNumExtVal == null && debug != null) {
                debug.println("X509CRLSelector.match: no CRLNumber");
            }
            try {
                BigInteger crlNum = new CRLNumberExtension(Boolean.FALSE, new DerInputStream(crlNumExtVal).getOctetString()).get("value");
                if (this.minCRL != null && crlNum.compareTo(this.minCRL) < 0) {
                    if (debug != null) {
                        debug.println("X509CRLSelector.match: CRLNumber too small");
                    }
                    return false;
                } else if (this.maxCRL != null && crlNum.compareTo(this.maxCRL) > 0) {
                    if (debug != null) {
                        debug.println("X509CRLSelector.match: CRLNumber too large");
                    }
                    return false;
                }
            } catch (IOException e) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: exception in decoding CRL number");
                }
                return false;
            }
        }
        if (this.dateAndTime != null) {
            crlNumExtVal = xcrl.getThisUpdate();
            Date nextUpdate = xcrl.getNextUpdate();
            if (nextUpdate == null) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: nextUpdate null");
                }
                return false;
            }
            Date nowPlusSkew = this.dateAndTime;
            Date nowMinusSkew = this.dateAndTime;
            if (this.skew > 0) {
                nowPlusSkew = new Date(this.dateAndTime.getTime() + this.skew);
                nowMinusSkew = new Date(this.dateAndTime.getTime() - this.skew);
            }
            if (nowMinusSkew.after(nextUpdate) || nowPlusSkew.before(crlNumExtVal)) {
                if (debug != null) {
                    debug.println("X509CRLSelector.match: update out-of-range");
                }
                return false;
            }
        }
        return true;
    }

    public Object clone() {
        try {
            X509CRLSelector copy = (X509CRLSelector) super.clone();
            if (this.issuerNames != null) {
                copy.issuerNames = new HashSet(this.issuerNames);
                copy.issuerX500Principals = new HashSet(this.issuerX500Principals);
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
