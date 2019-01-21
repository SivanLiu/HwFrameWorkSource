package javax.crypto;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;

final class JceSecurity {
    private static final URL NULL_URL;
    private static final Object PROVIDER_VERIFIED = Boolean.TRUE;
    static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<Class<?>, URL> codeBaseCacheRef = new WeakHashMap();
    private static CryptoPermissions defaultPolicy = null;
    private static CryptoPermissions exemptPolicy = null;
    private static final Map<Provider, Object> verificationResults = new IdentityHashMap();
    private static final Map<Provider, Object> verifyingProviders = new IdentityHashMap();

    static {
        try {
            NULL_URL = new URL("http://null.oracle.com/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JceSecurity() {
    }

    static Instance getInstance(String type, Class<?> clazz, String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        Service s = GetInstance.getService(type, algorithm, provider);
        Exception ve = getVerificationResult(s.getProvider());
        if (ve == null) {
            return GetInstance.getInstance(s, clazz);
        }
        String msg = new StringBuilder();
        msg.append("JCE cannot authenticate the provider ");
        msg.append(provider);
        throw ((NoSuchProviderException) new NoSuchProviderException(msg.toString()).initCause(ve));
    }

    static Instance getInstance(String type, Class<?> clazz, String algorithm, Provider provider) throws NoSuchAlgorithmException {
        Service s = GetInstance.getService(type, algorithm, provider);
        Exception ve = getVerificationResult(provider);
        if (ve == null) {
            return GetInstance.getInstance(s, clazz);
        }
        String msg = new StringBuilder();
        msg.append("JCE cannot authenticate the provider ");
        msg.append(provider.getName());
        throw new SecurityException(msg.toString(), ve);
    }

    static Instance getInstance(String type, Class<?> clazz, String algorithm) throws NoSuchAlgorithmException {
        NoSuchAlgorithmException failure = null;
        for (Service s : GetInstance.getServices(type, algorithm)) {
            if (canUseProvider(s.getProvider())) {
                try {
                    return GetInstance.getInstance(s, clazz);
                } catch (NoSuchAlgorithmException e) {
                    failure = e;
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Algorithm ");
        stringBuilder.append(algorithm);
        stringBuilder.append(" not available");
        throw new NoSuchAlgorithmException(stringBuilder.toString(), failure);
    }

    static CryptoPermissions verifyExemptJar(URL codeBase) throws Exception {
        JarVerifier jv = new JarVerifier(codeBase, true);
        jv.verify();
        return jv.getPermissions();
    }

    static void verifyProviderJar(URL codeBase) throws Exception {
        new JarVerifier(codeBase, false).verify();
    }

    static synchronized Exception getVerificationResult(Provider p) {
        synchronized (JceSecurity.class) {
            Object o = verificationResults.get(p);
            Exception exception;
            if (o == PROVIDER_VERIFIED) {
                return null;
            } else if (o != null) {
                exception = (Exception) o;
                return exception;
            } else if (verifyingProviders.get(p) != null) {
                NoSuchProviderException noSuchProviderException = new NoSuchProviderException("Recursion during verification");
                return noSuchProviderException;
            } else {
                try {
                    verifyingProviders.put(p, Boolean.FALSE);
                    verifyProviderJar(getCodeBase(p.getClass()));
                    verificationResults.put(p, PROVIDER_VERIFIED);
                    verifyingProviders.remove(p);
                    return null;
                } catch (Exception exception2) {
                    try {
                        verificationResults.put(p, exception2);
                    } finally {
                        verifyingProviders.remove(p);
                    }
                    return exception2;
                }
            }
        }
    }

    static boolean canUseProvider(Provider p) {
        return true;
    }

    static URL getCodeBase(final Class<?> clazz) {
        URL url;
        synchronized (codeBaseCacheRef) {
            URL url2 = (URL) codeBaseCacheRef.get(clazz);
            if (url2 == null) {
                url2 = (URL) AccessController.doPrivileged(new PrivilegedAction<URL>() {
                    public URL run() {
                        ProtectionDomain pd = clazz.getProtectionDomain();
                        if (pd != null) {
                            CodeSource cs = pd.getCodeSource();
                            if (cs != null) {
                                return cs.getLocation();
                            }
                        }
                        return JceSecurity.NULL_URL;
                    }
                });
                codeBaseCacheRef.put(clazz, url2);
            }
            url = url2 == NULL_URL ? null : url2;
        }
        return url;
    }

    private static void loadPolicies(File jarPathName, CryptoPermissions defaultPolicy, CryptoPermissions exemptPolicy) throws Exception {
        JarFile jf = new JarFile(jarPathName);
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = (JarEntry) entries.nextElement();
            InputStream is = null;
            try {
                if (je.getName().startsWith("default_")) {
                    is = jf.getInputStream(je);
                    defaultPolicy.load(is);
                } else if (je.getName().startsWith("exempt_")) {
                    is = jf.getInputStream(je);
                    exemptPolicy.load(is);
                } else if (is != null) {
                    is.close();
                }
                if (is != null) {
                    is.close();
                }
                JarVerifier.verifyPolicySigned(je.getCertificates());
            } catch (Throwable th) {
                if (is != null) {
                    is.close();
                }
            }
        }
        jf.close();
    }

    static CryptoPermissions getDefaultPolicy() {
        return defaultPolicy;
    }

    static CryptoPermissions getExemptPolicy() {
        return exemptPolicy;
    }
}
