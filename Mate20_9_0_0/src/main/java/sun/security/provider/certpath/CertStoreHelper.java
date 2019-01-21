package sun.security.provider.certpath;

import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Cache;

public abstract class CertStoreHelper {
    private static final int NUM_TYPES = 2;
    private static Cache<String, CertStoreHelper> cache = Cache.newSoftMemoryCache(2);
    private static final Map<String, String> classMap = new HashMap(2);

    public abstract CertStore getCertStore(URI uri) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    public abstract boolean isCausedByNetworkIssue(CertStoreException certStoreException);

    public abstract X509CRLSelector wrap(X509CRLSelector x509CRLSelector, Collection<X500Principal> collection, String str) throws IOException;

    public abstract X509CertSelector wrap(X509CertSelector x509CertSelector, X500Principal x500Principal, String str) throws IOException;

    static {
        classMap.put("LDAP", "sun.security.provider.certpath.ldap.LDAPCertStoreHelper");
        classMap.put("SSLServer", "sun.security.provider.certpath.ssl.SSLServerCertStoreHelper");
    }

    public static CertStoreHelper getInstance(final String type) throws NoSuchAlgorithmException {
        CertStoreHelper helper = (CertStoreHelper) cache.get(type);
        if (helper != null) {
            return helper;
        }
        final String cl = (String) classMap.get(type);
        if (cl != null) {
            try {
                return (CertStoreHelper) AccessController.doPrivileged(new PrivilegedExceptionAction<CertStoreHelper>() {
                    public CertStoreHelper run() throws ClassNotFoundException {
                        try {
                            CertStoreHelper csh = (CertStoreHelper) Class.forName(cl, true, null).newInstance();
                            CertStoreHelper.cache.put(type, csh);
                            return csh;
                        } catch (IllegalAccessException | InstantiationException e) {
                            throw new AssertionError(e);
                        }
                    }
                });
            } catch (PrivilegedActionException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(type);
                stringBuilder.append(" not available");
                throw new NoSuchAlgorithmException(stringBuilder.toString(), e.getException());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(type);
        stringBuilder2.append(" not available");
        throw new NoSuchAlgorithmException(stringBuilder2.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0038 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0046 A:{SYNTHETIC, Splitter:B:24:0x0046} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0039  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0038 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0046 A:{SYNTHETIC, Splitter:B:24:0x0046} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0039  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0038 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0046 A:{SYNTHETIC, Splitter:B:24:0x0046} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0039  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static boolean isCausedByNetworkIssue(String type, CertStoreException cse) {
        boolean z;
        int hashCode = type.hashCode();
        boolean z2 = true;
        if (hashCode == 84300) {
            if (type.equals("URI")) {
                z = true;
                switch (z) {
                    case false:
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 2331559) {
            if (type.equals("LDAP")) {
                z = false;
                switch (z) {
                    case false:
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 133315663 && type.equals("SSLServer")) {
            z = true;
            switch (z) {
                case false:
                case true:
                    try {
                        return getInstance(type).isCausedByNetworkIssue(cse);
                    } catch (NoSuchAlgorithmException e) {
                        return false;
                    }
                case true:
                    Throwable t = cse.getCause();
                    if (t == null || !(t instanceof IOException)) {
                        z2 = false;
                    }
                    return z2;
                default:
                    return false;
            }
        }
        z = true;
        switch (z) {
            case false:
            case true:
                break;
            case true:
                break;
            default:
                break;
        }
    }
}
