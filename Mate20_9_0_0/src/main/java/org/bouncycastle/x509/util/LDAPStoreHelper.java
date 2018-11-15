package org.bouncycastle.x509.util;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificatePair;
import org.bouncycastle.jce.X509LDAPCertStoreParameters;
import org.bouncycastle.jce.provider.X509AttrCertParser;
import org.bouncycastle.jce.provider.X509CRLParser;
import org.bouncycastle.jce.provider.X509CertPairParser;
import org.bouncycastle.jce.provider.X509CertParser;
import org.bouncycastle.util.StoreException;
import org.bouncycastle.x509.X509AttributeCertStoreSelector;
import org.bouncycastle.x509.X509AttributeCertificate;
import org.bouncycastle.x509.X509CRLStoreSelector;
import org.bouncycastle.x509.X509CertPairStoreSelector;
import org.bouncycastle.x509.X509CertStoreSelector;
import org.bouncycastle.x509.X509CertificatePair;

public class LDAPStoreHelper {
    private static String LDAP_PROVIDER = "com.sun.jndi.ldap.LdapCtxFactory";
    private static String REFERRALS_IGNORE = "ignore";
    private static final String SEARCH_SECURITY_LEVEL = "none";
    private static final String URL_CONTEXT_PREFIX = "com.sun.jndi.url";
    private static int cacheSize = 32;
    private static long lifeTime = 60000;
    private Map cacheMap = new HashMap(cacheSize);
    private X509LDAPCertStoreParameters params;

    public LDAPStoreHelper(X509LDAPCertStoreParameters x509LDAPCertStoreParameters) {
        this.params = x509LDAPCertStoreParameters;
    }

    private synchronized void addToCache(String str, List list) {
        Map map;
        Date date = new Date(System.currentTimeMillis());
        List arrayList = new ArrayList();
        arrayList.add(date);
        arrayList.add(list);
        if (this.cacheMap.containsKey(str)) {
            map = this.cacheMap;
        } else {
            if (this.cacheMap.size() >= cacheSize) {
                long time = date.getTime();
                Object obj = null;
                for (Entry entry : this.cacheMap.entrySet()) {
                    long time2 = ((Date) ((List) entry.getValue()).get(0)).getTime();
                    if (time2 < time) {
                        obj = entry.getKey();
                        time = time2;
                    }
                }
                this.cacheMap.remove(obj);
            }
            map = this.cacheMap;
        }
        map.put(str, arrayList);
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0044  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x008d  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x009a A:{LOOP_START, LOOP:0: B:25:0x009a->B:27:0x009d, PHI: r4 } */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00db A:{LOOP_END, LOOP:1: B:33:0x00d5->B:35:0x00db} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private List attrCertSubjectSerialSearch(X509AttributeCertStoreSelector x509AttributeCertStoreSelector, String[] strArr, String[] strArr2, String[] strArr3) throws StoreException {
        Principal[] entityNames;
        int i;
        List arrayList = new ArrayList();
        Collection<String> hashSet = new HashSet();
        String str = null;
        if (x509AttributeCertStoreSelector.getHolder() != null) {
            if (x509AttributeCertStoreSelector.getHolder().getSerialNumber() != null) {
                hashSet.add(x509AttributeCertStoreSelector.getHolder().getSerialNumber().toString());
            }
            if (x509AttributeCertStoreSelector.getHolder().getEntityNames() != null) {
                entityNames = x509AttributeCertStoreSelector.getHolder().getEntityNames();
                if (x509AttributeCertStoreSelector.getAttributeCert() != null) {
                    if (x509AttributeCertStoreSelector.getAttributeCert().getHolder().getEntityNames() != null) {
                        entityNames = x509AttributeCertStoreSelector.getAttributeCert().getHolder().getEntityNames();
                    }
                    hashSet.add(x509AttributeCertStoreSelector.getAttributeCert().getSerialNumber().toString());
                }
                i = 0;
                if (entityNames != null) {
                    str = entityNames[0] instanceof X500Principal ? ((X500Principal) entityNames[0]).getName("RFC1779") : entityNames[0].getName();
                }
                if (x509AttributeCertStoreSelector.getSerialNumber() != null) {
                    hashSet.add(x509AttributeCertStoreSelector.getSerialNumber().toString());
                }
                if (str != null) {
                    while (i < strArr3.length) {
                        String parseDN = parseDN(str, strArr3[i]);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("*");
                        stringBuilder.append(parseDN);
                        stringBuilder.append("*");
                        arrayList.addAll(search(strArr2, stringBuilder.toString(), strArr));
                        i++;
                    }
                }
                if (hashSet.size() > 0 && this.params.getSearchForSerialNumberIn() != null) {
                    for (String search : hashSet) {
                        arrayList.addAll(search(splitString(this.params.getSearchForSerialNumberIn()), search, strArr));
                    }
                }
                if (hashSet.size() == 0 && str == null) {
                    arrayList.addAll(search(strArr2, "*", strArr));
                }
                return arrayList;
            }
        }
        entityNames = null;
        if (x509AttributeCertStoreSelector.getAttributeCert() != null) {
        }
        i = 0;
        if (entityNames != null) {
        }
        if (x509AttributeCertStoreSelector.getSerialNumber() != null) {
        }
        if (str != null) {
        }
        while (r7.hasNext()) {
        }
        arrayList.addAll(search(strArr2, "*", strArr));
        return arrayList;
    }

    private List cRLIssuerSearch(X509CRLStoreSelector x509CRLStoreSelector, String[] strArr, String[] strArr2, String[] strArr3) throws StoreException {
        int i;
        List arrayList = new ArrayList();
        Collection<X500Principal> hashSet = new HashSet();
        if (x509CRLStoreSelector.getIssuers() != null) {
            hashSet.addAll(x509CRLStoreSelector.getIssuers());
        }
        if (x509CRLStoreSelector.getCertificateChecking() != null) {
            hashSet.add(getCertificateIssuer(x509CRLStoreSelector.getCertificateChecking()));
        }
        if (x509CRLStoreSelector.getAttrCertificateChecking() != null) {
            Principal[] principals = x509CRLStoreSelector.getAttrCertificateChecking().getIssuer().getPrincipals();
            for (i = 0; i < principals.length; i++) {
                if (principals[i] instanceof X500Principal) {
                    hashSet.add(principals[i]);
                }
            }
        }
        String str = null;
        for (X500Principal name : hashSet) {
            str = name.getName("RFC1779");
            for (String parseDN : strArr3) {
                String parseDN2 = parseDN(str, parseDN2);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("*");
                stringBuilder.append(parseDN2);
                stringBuilder.append("*");
                arrayList.addAll(search(strArr2, stringBuilder.toString(), strArr));
            }
        }
        if (str == null) {
            arrayList.addAll(search(strArr2, "*", strArr));
        }
        return arrayList;
    }

    private List certSubjectSerialSearch(X509CertStoreSelector x509CertStoreSelector, String[] strArr, String[] strArr2, String[] strArr3) throws StoreException {
        List arrayList = new ArrayList();
        String subjectAsString = getSubjectAsString(x509CertStoreSelector);
        String bigInteger = x509CertStoreSelector.getSerialNumber() != null ? x509CertStoreSelector.getSerialNumber().toString() : null;
        if (x509CertStoreSelector.getCertificate() != null) {
            subjectAsString = x509CertStoreSelector.getCertificate().getSubjectX500Principal().getName("RFC1779");
            bigInteger = x509CertStoreSelector.getCertificate().getSerialNumber().toString();
        }
        if (subjectAsString != null) {
            for (String parseDN : strArr3) {
                String parseDN2 = parseDN(subjectAsString, parseDN2);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("*");
                stringBuilder.append(parseDN2);
                stringBuilder.append("*");
                arrayList.addAll(search(strArr2, stringBuilder.toString(), strArr));
            }
        }
        if (!(bigInteger == null || this.params.getSearchForSerialNumberIn() == null)) {
            arrayList.addAll(search(splitString(this.params.getSearchForSerialNumberIn()), bigInteger, strArr));
        }
        if (bigInteger == null && subjectAsString == null) {
            arrayList.addAll(search(strArr2, "*", strArr));
        }
        return arrayList;
    }

    private DirContext connectLDAP() throws NamingException {
        Hashtable properties = new Properties();
        properties.setProperty("java.naming.factory.initial", LDAP_PROVIDER);
        properties.setProperty("java.naming.batchsize", "0");
        properties.setProperty("java.naming.provider.url", this.params.getLdapURL());
        properties.setProperty("java.naming.factory.url.pkgs", URL_CONTEXT_PREFIX);
        properties.setProperty("java.naming.referral", REFERRALS_IGNORE);
        properties.setProperty("java.naming.security.authentication", SEARCH_SECURITY_LEVEL);
        return new InitialDirContext(properties);
    }

    private Set createAttributeCertificates(List list, X509AttributeCertStoreSelector x509AttributeCertStoreSelector) throws StoreException {
        Set hashSet = new HashSet();
        X509AttrCertParser x509AttrCertParser = new X509AttrCertParser();
        for (byte[] byteArrayInputStream : list) {
            try {
                x509AttrCertParser.engineInit(new ByteArrayInputStream(byteArrayInputStream));
                X509AttributeCertificate x509AttributeCertificate = (X509AttributeCertificate) x509AttrCertParser.engineRead();
                if (x509AttributeCertStoreSelector.match(x509AttributeCertificate)) {
                    hashSet.add(x509AttributeCertificate);
                }
            } catch (StreamParsingException e) {
            }
        }
        return hashSet;
    }

    private Set createCRLs(List list, X509CRLStoreSelector x509CRLStoreSelector) throws StoreException {
        Set hashSet = new HashSet();
        X509CRLParser x509CRLParser = new X509CRLParser();
        for (byte[] byteArrayInputStream : list) {
            try {
                x509CRLParser.engineInit(new ByteArrayInputStream(byteArrayInputStream));
                Object obj = (X509CRL) x509CRLParser.engineRead();
                if (x509CRLStoreSelector.match(obj)) {
                    hashSet.add(obj);
                }
            } catch (StreamParsingException e) {
            }
        }
        return hashSet;
    }

    private Set createCerts(List list, X509CertStoreSelector x509CertStoreSelector) throws StoreException {
        Set hashSet = new HashSet();
        X509CertParser x509CertParser = new X509CertParser();
        for (byte[] byteArrayInputStream : list) {
            try {
                x509CertParser.engineInit(new ByteArrayInputStream(byteArrayInputStream));
                Object obj = (X509Certificate) x509CertParser.engineRead();
                if (x509CertStoreSelector.match(obj)) {
                    hashSet.add(obj);
                }
            } catch (Exception e) {
            }
        }
        return hashSet;
    }

    /* JADX WARNING: Removed duplicated region for block: B:5:0x0028 A:{Splitter: B:7:0x002b, ExcHandler: java.security.cert.CertificateParsingException (e java.security.cert.CertificateParsingException)} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Set createCrossCertificatePairs(List list, X509CertPairStoreSelector x509CertPairStoreSelector) throws StoreException {
        Set hashSet = new HashSet();
        int i = 0;
        while (i < list.size()) {
            Object obj;
            try {
                X509CertPairParser x509CertPairParser = new X509CertPairParser();
                x509CertPairParser.engineInit(new ByteArrayInputStream((byte[]) list.get(i)));
                obj = (X509CertificatePair) x509CertPairParser.engineRead();
            } catch (StreamParsingException e) {
                try {
                    int i2 = i + 1;
                    i = i2;
                    obj = new X509CertificatePair(new CertificatePair(Certificate.getInstance(new ASN1InputStream((byte[]) list.get(i)).readObject()), Certificate.getInstance(new ASN1InputStream((byte[]) list.get(i2)).readObject())));
                } catch (CertificateParsingException e2) {
                }
            }
            if (x509CertPairStoreSelector.match(obj)) {
                hashSet.add(obj);
            }
            i++;
        }
        return hashSet;
    }

    private List crossCertificatePairSubjectSearch(X509CertPairStoreSelector x509CertPairStoreSelector, String[] strArr, String[] strArr2, String[] strArr3) throws StoreException {
        List arrayList = new ArrayList();
        String subjectAsString = x509CertPairStoreSelector.getForwardSelector() != null ? getSubjectAsString(x509CertPairStoreSelector.getForwardSelector()) : null;
        if (!(x509CertPairStoreSelector.getCertPair() == null || x509CertPairStoreSelector.getCertPair().getForward() == null)) {
            subjectAsString = x509CertPairStoreSelector.getCertPair().getForward().getSubjectX500Principal().getName("RFC1779");
        }
        if (subjectAsString != null) {
            for (String parseDN : strArr3) {
                String parseDN2 = parseDN(subjectAsString, parseDN2);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("*");
                stringBuilder.append(parseDN2);
                stringBuilder.append("*");
                arrayList.addAll(search(strArr2, stringBuilder.toString(), strArr));
            }
        }
        if (subjectAsString == null) {
            arrayList.addAll(search(strArr2, "*", strArr));
        }
        return arrayList;
    }

    private X500Principal getCertificateIssuer(X509Certificate x509Certificate) {
        return x509Certificate.getIssuerX500Principal();
    }

    private List getFromCache(String str) {
        List list = (List) this.cacheMap.get(str);
        return (list == null || ((Date) list.get(0)).getTime() < System.currentTimeMillis() - lifeTime) ? null : (List) list.get(1);
    }

    private String getSubjectAsString(X509CertStoreSelector x509CertStoreSelector) {
        try {
            byte[] subjectAsBytes = x509CertStoreSelector.getSubjectAsBytes();
            return subjectAsBytes != null ? new X500Principal(subjectAsBytes).getName("RFC1779") : null;
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception processing name: ");
            stringBuilder.append(e.getMessage());
            throw new StoreException(stringBuilder.toString(), e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x0042  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String parseDN(String str, String str2) {
        String toLowerCase = str.toLowerCase();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str2.toLowerCase());
        stringBuilder.append("=");
        int indexOf = toLowerCase.indexOf(stringBuilder.toString());
        if (indexOf == -1) {
            return "";
        }
        str = str.substring(indexOf + str2.length());
        indexOf = str.indexOf(44);
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
        if (str.endsWith("\"")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /* JADX WARNING: Removed duplicated region for block: B:44:0x0118 A:{SYNTHETIC, Splitter: B:44:0x0118} */
    /* JADX WARNING: Missing block: B:33:0x0108, code:
            if (r0 != null) goto L_0x010a;
     */
    /* JADX WARNING: Missing block: B:35:?, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:36:0x010d, code:
            return r6;
     */
    /* JADX WARNING: Missing block: B:38:0x010f, code:
            return r6;
     */
    /* JADX WARNING: Missing block: B:50:0x0120, code:
            if (r0 != null) goto L_0x010a;
     */
    /* JADX WARNING: Missing block: B:51:0x0123, code:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private List search(String[] strArr, String str, String[] strArr2) throws StoreException {
        String str2;
        Throwable th;
        int i = 0;
        if (strArr == null) {
            str2 = null;
        } else {
            String str3 = "";
            if (str.equals("**")) {
                str = "*";
            }
            String str4 = str3;
            for (String str42 : strArr) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str42);
                stringBuilder.append("(");
                stringBuilder.append(str42);
                stringBuilder.append("=");
                stringBuilder.append(str);
                stringBuilder.append(")");
                str42 = stringBuilder.toString();
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("(|");
            stringBuilder2.append(str42);
            stringBuilder2.append(")");
            str2 = stringBuilder2.toString();
        }
        str = "";
        while (i < strArr2.length) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append("(");
            stringBuilder3.append(strArr2[i]);
            stringBuilder3.append("=*)");
            str = stringBuilder3.toString();
            i++;
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("(|");
        stringBuilder4.append(str);
        stringBuilder4.append(")");
        str = stringBuilder4.toString();
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("(&");
        stringBuilder4.append(str2);
        stringBuilder4.append("");
        stringBuilder4.append(str);
        stringBuilder4.append(")");
        String stringBuilder5 = stringBuilder4.toString();
        if (str2 != null) {
            str = stringBuilder5;
        }
        List fromCache = getFromCache(str);
        if (fromCache != null) {
            return fromCache;
        }
        fromCache = new ArrayList();
        DirContext connectLDAP;
        try {
            connectLDAP = connectLDAP();
            try {
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(2);
                searchControls.setCountLimit(0);
                searchControls.setReturningAttributes(strArr2);
                NamingEnumeration search = connectLDAP.search(this.params.getBaseDN(), str, searchControls);
                while (search.hasMoreElements()) {
                    NamingEnumeration all = ((Attribute) ((SearchResult) search.next()).getAttributes().getAll().next()).getAll();
                    while (all.hasMore()) {
                        fromCache.add(all.next());
                    }
                }
                addToCache(str, fromCache);
            } catch (NamingException e) {
            } catch (Throwable th2) {
                th = th2;
                if (connectLDAP != null) {
                    try {
                        connectLDAP.close();
                    } catch (Exception e2) {
                    }
                }
                throw th;
            }
        } catch (NamingException e3) {
            connectLDAP = null;
        } catch (Throwable th3) {
            th = th3;
            connectLDAP = null;
            if (connectLDAP != null) {
            }
            throw th;
        }
    }

    private String[] splitString(String str) {
        return str.split("\\s+");
    }

    public Collection getAACertificates(X509AttributeCertStoreSelector x509AttributeCertStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getAACertificateAttribute());
        String[] splitString2 = splitString(this.params.getLdapAACertificateAttributeName());
        String[] splitString3 = splitString(this.params.getAACertificateSubjectAttributeName());
        Collection createAttributeCertificates = createAttributeCertificates(attrCertSubjectSerialSearch(x509AttributeCertStoreSelector, splitString, splitString2, splitString3), x509AttributeCertStoreSelector);
        if (createAttributeCertificates.size() == 0) {
            createAttributeCertificates.addAll(createAttributeCertificates(attrCertSubjectSerialSearch(new X509AttributeCertStoreSelector(), splitString, splitString2, splitString3), x509AttributeCertStoreSelector));
        }
        return createAttributeCertificates;
    }

    public Collection getAttributeAuthorityRevocationLists(X509CRLStoreSelector x509CRLStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getAttributeAuthorityRevocationListAttribute());
        String[] splitString2 = splitString(this.params.getLdapAttributeAuthorityRevocationListAttributeName());
        String[] splitString3 = splitString(this.params.getAttributeAuthorityRevocationListIssuerAttributeName());
        Collection createCRLs = createCRLs(cRLIssuerSearch(x509CRLStoreSelector, splitString, splitString2, splitString3), x509CRLStoreSelector);
        if (createCRLs.size() == 0) {
            createCRLs.addAll(createCRLs(cRLIssuerSearch(new X509CRLStoreSelector(), splitString, splitString2, splitString3), x509CRLStoreSelector));
        }
        return createCRLs;
    }

    public Collection getAttributeCertificateAttributes(X509AttributeCertStoreSelector x509AttributeCertStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getAttributeCertificateAttributeAttribute());
        String[] splitString2 = splitString(this.params.getLdapAttributeCertificateAttributeAttributeName());
        String[] splitString3 = splitString(this.params.getAttributeCertificateAttributeSubjectAttributeName());
        Collection createAttributeCertificates = createAttributeCertificates(attrCertSubjectSerialSearch(x509AttributeCertStoreSelector, splitString, splitString2, splitString3), x509AttributeCertStoreSelector);
        if (createAttributeCertificates.size() == 0) {
            createAttributeCertificates.addAll(createAttributeCertificates(attrCertSubjectSerialSearch(new X509AttributeCertStoreSelector(), splitString, splitString2, splitString3), x509AttributeCertStoreSelector));
        }
        return createAttributeCertificates;
    }

    public Collection getAttributeCertificateRevocationLists(X509CRLStoreSelector x509CRLStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getAttributeCertificateRevocationListAttribute());
        String[] splitString2 = splitString(this.params.getLdapAttributeCertificateRevocationListAttributeName());
        String[] splitString3 = splitString(this.params.getAttributeCertificateRevocationListIssuerAttributeName());
        Collection createCRLs = createCRLs(cRLIssuerSearch(x509CRLStoreSelector, splitString, splitString2, splitString3), x509CRLStoreSelector);
        if (createCRLs.size() == 0) {
            createCRLs.addAll(createCRLs(cRLIssuerSearch(new X509CRLStoreSelector(), splitString, splitString2, splitString3), x509CRLStoreSelector));
        }
        return createCRLs;
    }

    public Collection getAttributeDescriptorCertificates(X509AttributeCertStoreSelector x509AttributeCertStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getAttributeDescriptorCertificateAttribute());
        String[] splitString2 = splitString(this.params.getLdapAttributeDescriptorCertificateAttributeName());
        String[] splitString3 = splitString(this.params.getAttributeDescriptorCertificateSubjectAttributeName());
        Collection createAttributeCertificates = createAttributeCertificates(attrCertSubjectSerialSearch(x509AttributeCertStoreSelector, splitString, splitString2, splitString3), x509AttributeCertStoreSelector);
        if (createAttributeCertificates.size() == 0) {
            createAttributeCertificates.addAll(createAttributeCertificates(attrCertSubjectSerialSearch(new X509AttributeCertStoreSelector(), splitString, splitString2, splitString3), x509AttributeCertStoreSelector));
        }
        return createAttributeCertificates;
    }

    public Collection getAuthorityRevocationLists(X509CRLStoreSelector x509CRLStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getAuthorityRevocationListAttribute());
        String[] splitString2 = splitString(this.params.getLdapAuthorityRevocationListAttributeName());
        String[] splitString3 = splitString(this.params.getAuthorityRevocationListIssuerAttributeName());
        Collection createCRLs = createCRLs(cRLIssuerSearch(x509CRLStoreSelector, splitString, splitString2, splitString3), x509CRLStoreSelector);
        if (createCRLs.size() == 0) {
            createCRLs.addAll(createCRLs(cRLIssuerSearch(new X509CRLStoreSelector(), splitString, splitString2, splitString3), x509CRLStoreSelector));
        }
        return createCRLs;
    }

    public Collection getCACertificates(X509CertStoreSelector x509CertStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getCACertificateAttribute());
        String[] splitString2 = splitString(this.params.getLdapCACertificateAttributeName());
        String[] splitString3 = splitString(this.params.getCACertificateSubjectAttributeName());
        Collection createCerts = createCerts(certSubjectSerialSearch(x509CertStoreSelector, splitString, splitString2, splitString3), x509CertStoreSelector);
        if (createCerts.size() == 0) {
            createCerts.addAll(createCerts(certSubjectSerialSearch(new X509CertStoreSelector(), splitString, splitString2, splitString3), x509CertStoreSelector));
        }
        return createCerts;
    }

    public Collection getCertificateRevocationLists(X509CRLStoreSelector x509CRLStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getCertificateRevocationListAttribute());
        String[] splitString2 = splitString(this.params.getLdapCertificateRevocationListAttributeName());
        String[] splitString3 = splitString(this.params.getCertificateRevocationListIssuerAttributeName());
        Collection createCRLs = createCRLs(cRLIssuerSearch(x509CRLStoreSelector, splitString, splitString2, splitString3), x509CRLStoreSelector);
        if (createCRLs.size() == 0) {
            createCRLs.addAll(createCRLs(cRLIssuerSearch(new X509CRLStoreSelector(), splitString, splitString2, splitString3), x509CRLStoreSelector));
        }
        return createCRLs;
    }

    public Collection getCrossCertificatePairs(X509CertPairStoreSelector x509CertPairStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getCrossCertificateAttribute());
        String[] splitString2 = splitString(this.params.getLdapCrossCertificateAttributeName());
        String[] splitString3 = splitString(this.params.getCrossCertificateSubjectAttributeName());
        Collection createCrossCertificatePairs = createCrossCertificatePairs(crossCertificatePairSubjectSearch(x509CertPairStoreSelector, splitString, splitString2, splitString3), x509CertPairStoreSelector);
        if (createCrossCertificatePairs.size() == 0) {
            X509CertStoreSelector x509CertStoreSelector = new X509CertStoreSelector();
            X509CertPairStoreSelector x509CertPairStoreSelector2 = new X509CertPairStoreSelector();
            x509CertPairStoreSelector2.setForwardSelector(x509CertStoreSelector);
            x509CertPairStoreSelector2.setReverseSelector(x509CertStoreSelector);
            createCrossCertificatePairs.addAll(createCrossCertificatePairs(crossCertificatePairSubjectSearch(x509CertPairStoreSelector2, splitString, splitString2, splitString3), x509CertPairStoreSelector));
        }
        return createCrossCertificatePairs;
    }

    public Collection getDeltaCertificateRevocationLists(X509CRLStoreSelector x509CRLStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getDeltaRevocationListAttribute());
        String[] splitString2 = splitString(this.params.getLdapDeltaRevocationListAttributeName());
        String[] splitString3 = splitString(this.params.getDeltaRevocationListIssuerAttributeName());
        Collection createCRLs = createCRLs(cRLIssuerSearch(x509CRLStoreSelector, splitString, splitString2, splitString3), x509CRLStoreSelector);
        if (createCRLs.size() == 0) {
            createCRLs.addAll(createCRLs(cRLIssuerSearch(new X509CRLStoreSelector(), splitString, splitString2, splitString3), x509CRLStoreSelector));
        }
        return createCRLs;
    }

    public Collection getUserCertificates(X509CertStoreSelector x509CertStoreSelector) throws StoreException {
        String[] splitString = splitString(this.params.getUserCertificateAttribute());
        String[] splitString2 = splitString(this.params.getLdapUserCertificateAttributeName());
        String[] splitString3 = splitString(this.params.getUserCertificateSubjectAttributeName());
        Collection createCerts = createCerts(certSubjectSerialSearch(x509CertStoreSelector, splitString, splitString2, splitString3), x509CertStoreSelector);
        if (createCerts.size() == 0) {
            createCerts.addAll(createCerts(certSubjectSerialSearch(new X509CertStoreSelector(), splitString, splitString2, splitString3), x509CertStoreSelector));
        }
        return createCerts;
    }
}
