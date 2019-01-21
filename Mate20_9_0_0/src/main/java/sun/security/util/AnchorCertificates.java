package sun.security.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import sun.security.x509.X509CertImpl;

public class AnchorCertificates {
    private static final String HASH = "SHA-256";
    private static HashSet<String> certs;
    private static final Debug debug = Debug.getInstance("certpath");

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Throwable th;
                Throwable th2;
                File f = new File(System.getProperty("java.home"), "lib/security/cacerts");
                try {
                    KeyStore cacerts = KeyStore.getInstance("JKS");
                    FileInputStream fis = new FileInputStream(f);
                    try {
                        cacerts.load(fis, null);
                        AnchorCertificates.certs = new HashSet();
                        Enumeration<String> list = cacerts.aliases();
                        while (list.hasMoreElements()) {
                            String alias = (String) list.nextElement();
                            if (alias.contains(" [jdk")) {
                                AnchorCertificates.certs.add(X509CertImpl.getFingerprint(AnchorCertificates.HASH, (X509Certificate) cacerts.getCertificate(alias)));
                            }
                        }
                        fis.close();
                        return null;
                    } catch (Throwable th22) {
                        Throwable th3 = th22;
                        th22 = th;
                        th = th3;
                    }
                    throw th;
                    if (th22 != null) {
                        try {
                            fis.close();
                        } catch (Throwable th4) {
                            th22.addSuppressed(th4);
                        }
                    } else {
                        fis.close();
                    }
                    throw th;
                } catch (Exception e) {
                    if (AnchorCertificates.debug != null) {
                        AnchorCertificates.debug.println("Error parsing cacerts");
                    }
                    e.printStackTrace();
                }
            }
        });
    }

    public static boolean contains(X509Certificate cert) {
        boolean result = certs.contains(X509CertImpl.getFingerprint(HASH, cert));
        if (result && debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AnchorCertificate.contains: matched ");
            stringBuilder.append(cert.getSubjectDN());
            debug.println(stringBuilder.toString());
        }
        return result;
    }

    private AnchorCertificates() {
    }
}
