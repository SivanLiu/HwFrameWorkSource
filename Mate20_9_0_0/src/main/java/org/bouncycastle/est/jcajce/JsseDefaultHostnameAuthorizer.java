package org.bouncycastle.est.jcajce;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLSession;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.est.ESTException;
import org.bouncycastle.util.Strings;

public class JsseDefaultHostnameAuthorizer implements JsseHostnameAuthorizer {
    private final Set<String> knownSuffixes;

    public JsseDefaultHostnameAuthorizer(Set<String> set) {
        this.knownSuffixes = set;
    }

    /* JADX WARNING: Missing block: B:35:0x00c1, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isValidNameMatch(String str, String str2, Set<String> set) throws IOException {
        if (!str2.contains("*")) {
            return str.equalsIgnoreCase(str2);
        }
        int indexOf = str2.indexOf(42);
        boolean z = false;
        if (indexOf != str2.lastIndexOf("*") || str2.contains("..") || str2.charAt(str2.length() - 1) == '*') {
            return false;
        }
        int indexOf2 = str2.indexOf(46, indexOf);
        if (set == null || !set.contains(Strings.toLowerCase(str2.substring(indexOf2)))) {
            String toLowerCase = Strings.toLowerCase(str2.substring(indexOf + 1));
            str = Strings.toLowerCase(str);
            if (str.equals(toLowerCase) || toLowerCase.length() > str.length()) {
                return false;
            }
            if (indexOf <= 0) {
                return str.substring(0, str.length() - toLowerCase.length()).indexOf(46) > 0 ? false : str.endsWith(toLowerCase);
            } else {
                if (str.startsWith(str2.substring(0, indexOf - 1)) && str.endsWith(toLowerCase) && str.substring(indexOf, str.length() - toLowerCase.length()).indexOf(46) < 0) {
                    z = true;
                }
                return z;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Wildcard `");
        stringBuilder.append(str2);
        stringBuilder.append("` matches known public suffix.");
        throw new IOException(stringBuilder.toString());
    }

    public boolean verified(String str, SSLSession sSLSession) throws IOException {
        try {
            return verify(str, (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(sSLSession.getPeerCertificates()[0].getEncoded())));
        } catch (Throwable e) {
            if (e instanceof ESTException) {
                throw ((ESTException) e);
            }
            throw new ESTException(e.getMessage(), e);
        }
    }

    public boolean verify(String str, X509Certificate x509Certificate) throws IOException {
        try {
            Collection<List> subjectAlternativeNames = x509Certificate.getSubjectAlternativeNames();
            if (subjectAlternativeNames != null) {
                for (List list : subjectAlternativeNames) {
                    int intValue = ((Number) list.get(0)).intValue();
                    if (intValue != 2) {
                        if (intValue != 7) {
                            throw new RuntimeException("Unable to handle ");
                        } else if (InetAddress.getByName(str).equals(InetAddress.getByName(list.get(1).toString()))) {
                            return true;
                        }
                    } else if (isValidNameMatch(str, list.get(1).toString(), this.knownSuffixes)) {
                        return true;
                    }
                }
                return false;
            } else if (x509Certificate.getSubjectX500Principal() == null) {
                return false;
            } else {
                RDN[] rDNs = X500Name.getInstance(x509Certificate.getSubjectX500Principal().getEncoded()).getRDNs();
                for (int i = 0; i != rDNs.length; i++) {
                    RDN rdn = rDNs[i];
                    AttributeTypeAndValue[] typesAndValues = rdn.getTypesAndValues();
                    for (int i2 = 0; i2 != typesAndValues.length; i2++) {
                        if (typesAndValues[i2].getType().equals(BCStyle.CN)) {
                            return isValidNameMatch(str, rdn.getFirst().getValue().toString(), this.knownSuffixes);
                        }
                    }
                }
                return false;
            }
        } catch (Throwable e) {
            throw new ESTException(e.getMessage(), e);
        }
    }
}
