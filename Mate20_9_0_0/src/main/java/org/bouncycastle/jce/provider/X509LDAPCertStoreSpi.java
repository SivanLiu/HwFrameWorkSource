package org.bouncycastle.jce.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CRL;
import java.security.cert.CRLSelector;
import java.security.cert.CertSelector;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x509.CertificatePair;
import org.bouncycastle.jce.X509LDAPCertStoreParameters;

public class X509LDAPCertStoreSpi extends CertStoreSpi {
    private static String LDAP_PROVIDER = "com.sun.jndi.ldap.LdapCtxFactory";
    private static String REFERRALS_IGNORE = "ignore";
    private static final String SEARCH_SECURITY_LEVEL = "none";
    private static final String URL_CONTEXT_PREFIX = "com.sun.jndi.url";
    private X509LDAPCertStoreParameters params;

    public X509LDAPCertStoreSpi(CertStoreParameters certStoreParameters) throws InvalidAlgorithmParameterException {
        super(certStoreParameters);
        if (certStoreParameters instanceof X509LDAPCertStoreParameters) {
            this.params = (X509LDAPCertStoreParameters) certStoreParameters;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(X509LDAPCertStoreSpi.class.getName());
        stringBuilder.append(": parameter must be a ");
        stringBuilder.append(X509LDAPCertStoreParameters.class.getName());
        stringBuilder.append(" object\n");
        stringBuilder.append(certStoreParameters.toString());
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }

    private Set certSubjectSerialSearch(X509CertSelector x509CertSelector, String[] strArr, String str, String str2) throws CertStoreException {
        HashSet hashSet = new HashSet();
        StringBuilder stringBuilder;
        try {
            Collection search;
            String str3;
            if (x509CertSelector.getSubjectAsBytes() == null && x509CertSelector.getSubjectAsString() == null) {
                if (x509CertSelector.getCertificate() == null) {
                    search = search(str, "*", strArr);
                    hashSet.addAll(search);
                    return hashSet;
                }
            }
            String str4 = null;
            if (x509CertSelector.getCertificate() != null) {
                String name = x509CertSelector.getCertificate().getSubjectX500Principal().getName("RFC1779");
                str4 = x509CertSelector.getCertificate().getSerialNumber().toString();
                str3 = name;
            } else {
                str3 = x509CertSelector.getSubjectAsBytes() != null ? new X500Principal(x509CertSelector.getSubjectAsBytes()).getName("RFC1779") : x509CertSelector.getSubjectAsString();
            }
            str3 = parseDN(str3, str2);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("*");
            stringBuilder2.append(str3);
            stringBuilder2.append("*");
            hashSet.addAll(search(str, stringBuilder2.toString(), strArr));
            if (str4 == null || this.params.getSearchForSerialNumberIn() == null) {
                return hashSet;
            }
            str3 = this.params.getSearchForSerialNumberIn();
            stringBuilder = new StringBuilder();
            stringBuilder.append("*");
            stringBuilder.append(str4);
            stringBuilder.append("*");
            search = search(str3, stringBuilder.toString(), strArr);
            hashSet.addAll(search);
            return hashSet;
        } catch (IOException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception processing selector: ");
            stringBuilder.append(e);
            throw new CertStoreException(stringBuilder.toString());
        }
    }

    private DirContext connectLDAP() throws NamingException {
        Properties properties = new Properties();
        properties.setProperty("java.naming.factory.initial", LDAP_PROVIDER);
        properties.setProperty("java.naming.batchsize", "0");
        properties.setProperty("java.naming.provider.url", this.params.getLdapURL());
        properties.setProperty("java.naming.factory.url.pkgs", URL_CONTEXT_PREFIX);
        properties.setProperty("java.naming.referral", REFERRALS_IGNORE);
        properties.setProperty("java.naming.security.authentication", SEARCH_SECURITY_LEVEL);
        return new InitialDirContext(properties);
    }

    private Set getCACertificates(X509CertSelector x509CertSelector) throws CertStoreException {
        String[] strArr = new String[]{this.params.getCACertificateAttribute()};
        Set certSubjectSerialSearch = certSubjectSerialSearch(x509CertSelector, strArr, this.params.getLdapCACertificateAttributeName(), this.params.getCACertificateSubjectAttributeName());
        if (certSubjectSerialSearch.isEmpty()) {
            certSubjectSerialSearch.addAll(search(null, "*", strArr));
        }
        return certSubjectSerialSearch;
    }

    private Set getCrossCertificates(X509CertSelector x509CertSelector) throws CertStoreException {
        String[] strArr = new String[]{this.params.getCrossCertificateAttribute()};
        Set certSubjectSerialSearch = certSubjectSerialSearch(x509CertSelector, strArr, this.params.getLdapCrossCertificateAttributeName(), this.params.getCrossCertificateSubjectAttributeName());
        if (certSubjectSerialSearch.isEmpty()) {
            certSubjectSerialSearch.addAll(search(null, "*", strArr));
        }
        return certSubjectSerialSearch;
    }

    private Set getEndCertificates(X509CertSelector x509CertSelector) throws CertStoreException {
        return certSubjectSerialSearch(x509CertSelector, new String[]{this.params.getUserCertificateAttribute()}, this.params.getLdapUserCertificateAttributeName(), this.params.getUserCertificateSubjectAttributeName());
    }

    /* JADX WARNING: Removed duplicated region for block: B:5:0x002c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String parseDN(String str, String str2) {
        str = str.substring(str.toLowerCase().indexOf(str2.toLowerCase()) + str2.length());
        int indexOf = str.indexOf(44);
        if (indexOf == -1) {
            indexOf = str.length();
        }
        while (str.charAt(indexOf - 1) == '\\') {
            indexOf = str.indexOf(44, indexOf + 1);
            if (indexOf == -1) {
                indexOf = str.length();
                while (str.charAt(indexOf - 1) == '\\') {
                }
            }
        }
        str = str.substring(0, indexOf);
        str = str.substring(str.indexOf(61) + 1);
        if (str.charAt(0) == ' ') {
            str = str.substring(1);
        }
        if (str.startsWith("\"")) {
            str = str.substring(1);
        }
        return str.endsWith("\"") ? str.substring(0, str.length() - 1) : str;
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x00db A:{SYNTHETIC, Splitter:B:38:0x00db} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set search(String str, String str2, String[] strArr) throws CertStoreException {
        Object e;
        StringBuilder stringBuilder;
        Throwable th;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append("=");
        stringBuilder2.append(str2);
        str2 = stringBuilder2.toString();
        DirContext dirContext = null;
        if (str == null) {
            str2 = null;
        }
        HashSet hashSet = new HashSet();
        try {
            DirContext connectLDAP = connectLDAP();
            try {
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(2);
                searchControls.setCountLimit(0);
                for (int i = 0; i < strArr.length; i++) {
                    String[] strArr2 = new String[]{strArr[i]};
                    searchControls.setReturningAttributes(strArr2);
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("(&(");
                    stringBuilder3.append(str2);
                    stringBuilder3.append(")(");
                    stringBuilder3.append(strArr2[0]);
                    stringBuilder3.append("=*))");
                    String stringBuilder4 = stringBuilder3.toString();
                    if (str2 == null) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("(");
                        stringBuilder3.append(strArr2[0]);
                        stringBuilder3.append("=*)");
                        stringBuilder4 = stringBuilder3.toString();
                    }
                    NamingEnumeration search = connectLDAP.search(this.params.getBaseDN(), stringBuilder4, searchControls);
                    while (search.hasMoreElements()) {
                        NamingEnumeration all = ((Attribute) ((SearchResult) search.next()).getAttributes().getAll().next()).getAll();
                        while (all.hasMore()) {
                            hashSet.add(all.next());
                        }
                    }
                }
                if (connectLDAP != null) {
                    try {
                        connectLDAP.close();
                        return hashSet;
                    } catch (Exception e2) {
                    }
                }
                return hashSet;
            } catch (Exception e3) {
                e = e3;
                dirContext = connectLDAP;
                try {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error getting results from LDAP directory ");
                    stringBuilder.append(e);
                    throw new CertStoreException(stringBuilder.toString());
                } catch (Throwable th2) {
                    th = th2;
                    connectLDAP = dirContext;
                    if (connectLDAP != null) {
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                if (connectLDAP != null) {
                    try {
                        connectLDAP.close();
                    } catch (Exception e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e = e5;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error getting results from LDAP directory ");
            stringBuilder.append(e);
            throw new CertStoreException(stringBuilder.toString());
        }
    }

    public Collection engineGetCRLs(CRLSelector cRLSelector) throws CertStoreException {
        String[] strArr = new String[]{this.params.getCertificateRevocationListAttribute()};
        if (cRLSelector instanceof X509CRLSelector) {
            X509CRLSelector x509CRLSelector = (X509CRLSelector) cRLSelector;
            HashSet hashSet = new HashSet();
            String ldapCertificateRevocationListAttributeName = this.params.getLdapCertificateRevocationListAttributeName();
            HashSet<byte[]> hashSet2 = new HashSet();
            if (x509CRLSelector.getIssuerNames() != null) {
                for (Object next : x509CRLSelector.getIssuerNames()) {
                    String certificateRevocationListIssuerAttributeName;
                    String str;
                    if (next instanceof String) {
                        certificateRevocationListIssuerAttributeName = this.params.getCertificateRevocationListIssuerAttributeName();
                        str = (String) next;
                    } else {
                        certificateRevocationListIssuerAttributeName = this.params.getCertificateRevocationListIssuerAttributeName();
                        str = new X500Principal((byte[]) next).getName("RFC1779");
                    }
                    str = parseDN(str, certificateRevocationListIssuerAttributeName);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("*");
                    stringBuilder.append(str);
                    stringBuilder.append("*");
                    hashSet2.addAll(search(ldapCertificateRevocationListAttributeName, stringBuilder.toString(), strArr));
                }
            } else {
                hashSet2.addAll(search(ldapCertificateRevocationListAttributeName, "*", strArr));
            }
            hashSet2.addAll(search(null, "*", strArr));
            try {
                CertificateFactory instance = CertificateFactory.getInstance("X.509", "BC");
                for (byte[] byteArrayInputStream : hashSet2) {
                    CRL generateCRL = instance.generateCRL(new ByteArrayInputStream(byteArrayInputStream));
                    if (x509CRLSelector.match(generateCRL)) {
                        hashSet.add(generateCRL);
                    }
                }
                return hashSet;
            } catch (Exception e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("CRL cannot be constructed from LDAP result ");
                stringBuilder2.append(e);
                throw new CertStoreException(stringBuilder2.toString());
            }
        }
        throw new CertStoreException("selector is not a X509CRLSelector");
    }

    public Collection engineGetCertificates(CertSelector certSelector) throws CertStoreException {
        if (certSelector instanceof X509CertSelector) {
            X509CertSelector x509CertSelector = (X509CertSelector) certSelector;
            HashSet hashSet = new HashSet();
            Set<byte[]> endCertificates = getEndCertificates(x509CertSelector);
            endCertificates.addAll(getCACertificates(x509CertSelector));
            endCertificates.addAll(getCrossCertificates(x509CertSelector));
            try {
                CertificateFactory instance = CertificateFactory.getInstance("X.509", "BC");
                for (byte[] bArr : endCertificates) {
                    if (bArr != null) {
                        if (bArr.length != 0) {
                            ArrayList<byte[]> arrayList = new ArrayList();
                            arrayList.add(bArr);
                            try {
                                CertificatePair instance2 = CertificatePair.getInstance(new ASN1InputStream(bArr).readObject());
                                arrayList.clear();
                                if (instance2.getForward() != null) {
                                    arrayList.add(instance2.getForward().getEncoded());
                                }
                                if (instance2.getReverse() != null) {
                                    arrayList.add(instance2.getReverse().getEncoded());
                                }
                            } catch (IOException | IllegalArgumentException e) {
                            }
                            for (byte[] byteArrayInputStream : arrayList) {
                                try {
                                    Certificate generateCertificate = instance.generateCertificate(new ByteArrayInputStream(byteArrayInputStream));
                                    if (x509CertSelector.match(generateCertificate)) {
                                        hashSet.add(generateCertificate);
                                    }
                                } catch (Exception e2) {
                                }
                            }
                        }
                    }
                }
                return hashSet;
            } catch (Exception e3) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("certificate cannot be constructed from LDAP result: ");
                stringBuilder.append(e3);
                throw new CertStoreException(stringBuilder.toString());
            }
        }
        throw new CertStoreException("selector is not a X509CertSelector");
    }
}
