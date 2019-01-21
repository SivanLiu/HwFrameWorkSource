package sun.security.provider.certpath;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CRLSelector;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertStoreSpi;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import sun.security.action.GetIntegerAction;
import sun.security.util.Cache;
import sun.security.util.Debug;
import sun.security.x509.AccessDescription;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.URIName;

class URICertStore extends CertStoreSpi {
    private static final int CACHE_SIZE = 185;
    private static final int CHECK_INTERVAL = 30000;
    private static final int CRL_CONNECT_TIMEOUT = initializeTimeout();
    private static final int DEFAULT_CRL_CONNECT_TIMEOUT = 15000;
    private static final Cache<URICertStoreParameters, CertStore> certStoreCache = Cache.newSoftMemoryCache(CACHE_SIZE);
    private static final Debug debug = Debug.getInstance("certpath");
    private Collection<X509Certificate> certs = Collections.emptySet();
    private X509CRL crl;
    private final CertificateFactory factory;
    private long lastChecked;
    private long lastModified;
    private boolean ldap = false;
    private CertStore ldapCertStore;
    private CertStoreHelper ldapHelper;
    private String ldapPath;
    private URI uri;

    private static class UCS extends CertStore {
        protected UCS(CertStoreSpi spi, Provider p, String type, CertStoreParameters params) {
            super(spi, p, type, params);
        }
    }

    static class URICertStoreParameters implements CertStoreParameters {
        private volatile int hashCode = 0;
        private final URI uri;

        URICertStoreParameters(URI uri) {
            this.uri = uri;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof URICertStoreParameters)) {
                return false;
            }
            return this.uri.equals(((URICertStoreParameters) obj).uri);
        }

        public int hashCode() {
            if (this.hashCode == 0) {
                this.hashCode = (37 * 17) + this.uri.hashCode();
            }
            return this.hashCode;
        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e.toString(), e);
            }
        }
    }

    private static int initializeTimeout() {
        Integer tmp = (Integer) AccessController.doPrivileged(new GetIntegerAction("com.sun.security.crl.timeout"));
        if (tmp == null || tmp.intValue() < 0) {
            return DEFAULT_CRL_CONNECT_TIMEOUT;
        }
        return tmp.intValue() * 1000;
    }

    URICertStore(CertStoreParameters params) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        super(params);
        if (params instanceof URICertStoreParameters) {
            this.uri = ((URICertStoreParameters) params).uri;
            if (this.uri.getScheme().toLowerCase(Locale.ENGLISH).equals("ldap")) {
                this.ldap = true;
                this.ldapHelper = CertStoreHelper.getInstance("LDAP");
                this.ldapCertStore = this.ldapHelper.getCertStore(this.uri);
                this.ldapPath = this.uri.getPath();
                if (this.ldapPath.charAt(0) == '/') {
                    this.ldapPath = this.ldapPath.substring(1);
                }
            }
            try {
                this.factory = CertificateFactory.getInstance("X.509");
                return;
            } catch (CertificateException e) {
                throw new RuntimeException();
            }
        }
        throw new InvalidAlgorithmParameterException("params must be instanceof URICertStoreParameters");
    }

    static synchronized CertStore getInstance(URICertStoreParameters params) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        CertStore ucs;
        synchronized (URICertStore.class) {
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CertStore URI:");
                stringBuilder.append(params.uri);
                debug.println(stringBuilder.toString());
            }
            ucs = (CertStore) certStoreCache.get(params);
            if (ucs == null) {
                ucs = new UCS(new URICertStore(params), null, "URI", params);
                certStoreCache.put(params, ucs);
            } else if (debug != null) {
                debug.println("URICertStore.getInstance: cache hit");
            }
        }
        return ucs;
    }

    static CertStore getInstance(AccessDescription ad) {
        if (!ad.getAccessMethod().equals(AccessDescription.Ad_CAISSUERS_Id)) {
            return null;
        }
        GeneralNameInterface gn = ad.getAccessLocation().getName();
        if (!(gn instanceof URIName)) {
            return null;
        }
        try {
            return getInstance(new URICertStoreParameters(((URIName) gn).getURI()));
        } catch (Exception ex) {
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception creating CertStore: ");
                stringBuilder.append(ex);
                debug.println(stringBuilder.toString());
                ex.printStackTrace();
            }
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:46:0x008d, code skipped:
            return r9;
     */
    /* JADX WARNING: Missing block: B:60:0x00b4, code skipped:
            return r10;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized Collection<X509Certificate> engineGetCertificates(CertSelector selector) throws CertStoreException {
        if (this.ldap) {
            X509CertSelector xsel = (X509CertSelector) selector;
            try {
                return this.ldapCertStore.getCertificates(this.ldapHelper.wrap(xsel, xsel.getSubject(), this.ldapPath));
            } catch (IOException ioe) {
                throw new CertStoreException(ioe);
            }
        }
        long time = System.currentTimeMillis();
        if (time - this.lastChecked < 30000) {
            if (debug != null) {
                debug.println("Returning certificates from cache");
            }
            return getMatchingCerts(this.certs, selector);
        }
        this.lastChecked = time;
        InputStream in;
        try {
            URLConnection connection = this.uri.toURL().openConnection();
            if (this.lastModified != 0) {
                connection.setIfModifiedSince(this.lastModified);
            }
            long oldLastModified = this.lastModified;
            in = connection.getInputStream();
            this.lastModified = connection.getLastModified();
            if (oldLastModified != 0) {
                if (oldLastModified == this.lastModified) {
                    if (debug != null) {
                        debug.println("Not modified, using cached copy");
                    }
                    Collection matchingCerts = getMatchingCerts(this.certs, selector);
                    if (in != null) {
                        $closeResource(null, in);
                    }
                } else if ((connection instanceof HttpURLConnection) && ((HttpURLConnection) connection).getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    if (debug != null) {
                        debug.println("Not modified, using cached copy");
                    }
                    Collection matchingCerts2 = getMatchingCerts(this.certs, selector);
                    if (in != null) {
                        $closeResource(null, in);
                    }
                }
            }
            if (debug != null) {
                debug.println("Downloading new certificates...");
            }
            this.certs = this.factory.generateCertificates(in);
            if (in != null) {
                $closeResource(null, in);
            }
            return getMatchingCerts(this.certs, selector);
        } catch (IOException | CertificateException e) {
            if (debug != null) {
                debug.println("Exception fetching certificates:");
                e.printStackTrace();
            }
            this.lastModified = 0;
            this.certs = Collections.emptySet();
            return this.certs;
        } catch (Throwable th) {
            if (in != null) {
                $closeResource(r8, in);
            }
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private static Collection<X509Certificate> getMatchingCerts(Collection<X509Certificate> certs, CertSelector selector) {
        if (selector == null) {
            return certs;
        }
        List<X509Certificate> matchedCerts = new ArrayList(certs.size());
        for (X509Certificate cert : certs) {
            if (selector.match(cert)) {
                matchedCerts.add(cert);
            }
        }
        return matchedCerts;
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:5:0x0009, B:8:0x0013, B:30:0x0050] */
    /* JADX WARNING: Missing block: B:12:0x001b, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:15:0x0023, code skipped:
            throw new sun.security.provider.certpath.PKIX.CertStoreTypeException("LDAP", r1);
     */
    /* JADX WARNING: Missing block: B:16:0x0024, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:18:0x002a, code skipped:
            throw new java.security.cert.CertStoreException(r1);
     */
    /* JADX WARNING: Missing block: B:48:0x0097, code skipped:
            return r9;
     */
    /* JADX WARNING: Missing block: B:62:0x00be, code skipped:
            return r10;
     */
    /* JADX WARNING: Missing block: B:85:0x00f0, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:88:0x00f3, code skipped:
            if (debug != null) goto L_0x00f5;
     */
    /* JADX WARNING: Missing block: B:89:0x00f5, code skipped:
            debug.println("Exception fetching CRL:");
            r0.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:90:0x00ff, code skipped:
            r13.lastModified = 0;
            r13.crl = null;
     */
    /* JADX WARNING: Missing block: B:91:0x010f, code skipped:
            throw new sun.security.provider.certpath.PKIX.CertStoreTypeException("URI", new java.security.cert.CertStoreException(r0));
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized Collection<X509CRL> engineGetCRLs(CRLSelector selector) throws CertStoreException {
        InputStream in;
        Throwable th;
        Throwable th2;
        if (this.ldap) {
            return this.ldapCertStore.getCRLs(this.ldapHelper.wrap((X509CRLSelector) selector, null, this.ldapPath));
        }
        long time = System.currentTimeMillis();
        if (time - this.lastChecked < 30000) {
            if (debug != null) {
                debug.println("Returning CRL from cache");
            }
            return getMatchingCRLs(this.crl, selector);
        }
        this.lastChecked = time;
        URLConnection connection = this.uri.toURL().openConnection();
        if (this.lastModified != 0) {
            connection.setIfModifiedSince(this.lastModified);
        }
        long oldLastModified = this.lastModified;
        connection.setConnectTimeout(CRL_CONNECT_TIMEOUT);
        in = connection.getInputStream();
        try {
            this.lastModified = connection.getLastModified();
            if (oldLastModified != 0) {
                if (oldLastModified == this.lastModified) {
                    if (debug != null) {
                        debug.println("Not modified, using cached copy");
                    }
                    Collection matchingCRLs = getMatchingCRLs(this.crl, selector);
                    if (in != null) {
                        $closeResource(null, in);
                    }
                } else if ((connection instanceof HttpURLConnection) && ((HttpURLConnection) connection).getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    if (debug != null) {
                        debug.println("Not modified, using cached copy");
                    }
                    Collection matchingCRLs2 = getMatchingCRLs(this.crl, selector);
                    if (in != null) {
                        $closeResource(null, in);
                    }
                }
            }
            if (debug != null) {
                debug.println("Downloading new CRL...");
            }
            this.crl = (X509CRL) this.factory.generateCRL(in);
            if (in != null) {
                $closeResource(null, in);
            }
            return getMatchingCRLs(this.crl, selector);
        } catch (Throwable th22) {
            Throwable th3 = th22;
            th22 = th;
            th = th3;
        }
        if (in != null) {
            $closeResource(th22, in);
        }
        throw th;
    }

    private static Collection<X509CRL> getMatchingCRLs(X509CRL crl, CRLSelector selector) {
        if (selector == null || (crl != null && selector.match(crl))) {
            return Collections.singletonList(crl);
        }
        return Collections.emptyList();
    }
}
