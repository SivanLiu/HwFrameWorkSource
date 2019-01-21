package sun.net.www.protocol.jar;

import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.HashMap;
import java.util.jar.JarFile;
import sun.net.util.URLUtil;
import sun.net.www.protocol.jar.URLJarFile.URLJarFileCloseController;
import sun.security.util.SecurityConstants;

class JarFileFactory implements URLJarFileCloseController {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final HashMap<String, JarFile> fileCache = new HashMap();
    private static final JarFileFactory instance = new JarFileFactory();
    private static final HashMap<JarFile, URL> urlCache = new HashMap();

    private JarFileFactory() {
    }

    public static JarFileFactory getInstance() {
        return instance;
    }

    URLConnection getConnection(JarFile jarFile) throws IOException {
        URL u;
        synchronized (instance) {
            u = (URL) urlCache.get(jarFile);
        }
        if (u != null) {
            return u.openConnection();
        }
        return null;
    }

    public JarFile get(URL url) throws IOException {
        return get(url, true);
    }

    JarFile get(URL url, boolean useCaches) throws IOException {
        JarFile result;
        JarFile local_result;
        if (useCaches) {
            synchronized (instance) {
                result = getCachedJarFile(url);
            }
            if (result == null) {
                local_result = URLJarFile.getJarFile(url, this);
                synchronized (instance) {
                    result = getCachedJarFile(url);
                    if (result == null) {
                        fileCache.put(URLUtil.urlNoFragString(url), local_result);
                        urlCache.put(local_result, url);
                        result = local_result;
                    } else if (local_result != null) {
                        local_result.close();
                    }
                }
            }
        } else {
            result = URLJarFile.getJarFile(url, this);
        }
        local_result = result;
        if (local_result != null) {
            return local_result;
        }
        throw new FileNotFoundException(url.toString());
    }

    public void close(JarFile jarFile) {
        synchronized (instance) {
            URL urlRemoved = (URL) urlCache.remove(jarFile);
            if (urlRemoved != null) {
                fileCache.remove(URLUtil.urlNoFragString(urlRemoved));
            }
        }
    }

    private JarFile getCachedJarFile(URL url) {
        JarFile result = (JarFile) fileCache.get(URLUtil.urlNoFragString(url));
        if (result != null) {
            Permission perm = getPermission(result);
            if (perm != null) {
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    try {
                        sm.checkPermission(perm);
                    } catch (SecurityException se) {
                        if ((perm instanceof FilePermission) && perm.getActions().indexOf("read") != -1) {
                            sm.checkRead(perm.getName());
                        } else if (!(perm instanceof SocketPermission) || perm.getActions().indexOf(SecurityConstants.SOCKET_CONNECT_ACTION) == -1) {
                            throw se;
                        } else {
                            sm.checkConnect(url.getHost(), url.getPort());
                        }
                    }
                }
            }
        }
        return result;
    }

    private Permission getPermission(JarFile jarFile) {
        try {
            URLConnection uc = getConnection(jarFile);
            if (uc != null) {
                return uc.getPermission();
            }
        } catch (IOException e) {
        }
        return null;
    }
}
