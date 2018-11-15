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
                    FileInputStream fileInputStream = null;
                    try {
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
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            th = null;
                            if (th != null) {
                                throw th;
                            }
                            return null;
                        } catch (Throwable th4) {
                            th = th4;
                            fileInputStream = fis;
                            th2 = null;
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (Throwable th5) {
                                    if (th2 == null) {
                                        th2 = th5;
                                    } else if (th2 != th5) {
                                        th2.addSuppressed(th5);
                                    }
                                }
                            }
                            if (th2 == null) {
                                throw th2;
                            } else {
                                throw th;
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        th2 = null;
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        if (th2 == null) {
                            throw th;
                        }
                        throw th2;
                    }
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
            debug.println("AnchorCertificate.contains: matched " + cert.getSubjectDN());
        }
        return result;
    }

    private AnchorCertificates() {
    }
}
