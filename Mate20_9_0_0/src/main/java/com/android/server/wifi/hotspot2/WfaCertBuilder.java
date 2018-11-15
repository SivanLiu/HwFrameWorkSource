package com.android.server.wifi.hotspot2;

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

public class WfaCertBuilder {
    private static final String TAG = "WfaCertBuilder";

    /* JADX WARNING: Removed duplicated region for block: B:14:0x003b A:{Splitter: B:1:0x0005, ExcHandler: java.security.cert.CertificateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x003b A:{Splitter: B:1:0x0005, ExcHandler: java.security.cert.CertificateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:14:0x003b, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:15:0x003c, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Unable to read cert ");
            r3.append(r1.getMessage());
            android.util.Log.e(r2, r3.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static Set<X509Certificate> loadCertsFromDisk(String directory) {
        Set<X509Certificate> certs = new HashSet();
        try {
            File[] certFiles = new File(directory).listFiles();
            if (certFiles == null || certFiles.length <= 0) {
                return certs;
            }
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            for (File certFile : certFiles) {
                FileInputStream fis = new FileInputStream(certFile);
                Certificate cert = certFactory.generateCertificate(fis);
                if (cert instanceof X509Certificate) {
                    certs.add((X509Certificate) cert);
                }
                fis.close();
            }
            return certs;
        } catch (Exception e) {
        }
    }
}
