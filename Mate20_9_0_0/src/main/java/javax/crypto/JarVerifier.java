package javax.crypto;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;

final class JarVerifier {
    private CryptoPermissions appPerms = null;
    private URL jarURL;
    private boolean savePerms;

    JarVerifier(URL jarURL, boolean savePerms) {
        this.jarURL = jarURL;
        this.savePerms = savePerms;
    }

    void verify() throws JarException, IOException {
        if (this.savePerms) {
            URL url;
            if (this.jarURL.getProtocol().equalsIgnoreCase("jar")) {
                url = this.jarURL;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("jar:");
                stringBuilder.append(this.jarURL.toString());
                stringBuilder.append("!/");
                url = new URL(stringBuilder.toString());
            }
            JarFile jf = null;
            try {
                jf = (JarFile) AccessController.doPrivileged(new PrivilegedExceptionAction<JarFile>() {
                    public JarFile run() throws Exception {
                        JarURLConnection conn = (JarURLConnection) url.openConnection();
                        conn.setUseCaches(false);
                        return conn.getJarFile();
                    }
                });
                if (jf != null) {
                    JarEntry je = jf.getJarEntry("cryptoPerms");
                    if (je != null) {
                        this.appPerms = new CryptoPermissions();
                        this.appPerms.load(jf.getInputStream(je));
                    } else {
                        throw new JarException("Can not find cryptoPerms");
                    }
                }
                if (jf != null) {
                    jf.close();
                }
            } catch (PrivilegedActionException pae) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Cannot load ");
                stringBuilder2.append(url.toString());
                throw new SecurityException(stringBuilder2.toString(), pae);
            } catch (Exception ex) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Cannot load/parse");
                stringBuilder3.append(this.jarURL.toString());
                JarException jex = new JarException(stringBuilder3.toString());
                jex.initCause(ex);
                throw jex;
            } catch (Throwable th) {
                if (jf != null) {
                    jf.close();
                }
            }
        }
    }

    static void verifyPolicySigned(Certificate[] certs) throws Exception {
    }

    CryptoPermissions getPermissions() {
        return this.appPerms;
    }
}
