package javax.net.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Locale;
import javax.net.SocketFactory;
import sun.security.action.GetPropertyAction;

public abstract class SSLSocketFactory extends SocketFactory {
    static final boolean DEBUG;
    private static SSLSocketFactory defaultSocketFactory;
    private static int lastVersion = -1;

    public abstract Socket createSocket(Socket socket, String str, int i, boolean z) throws IOException;

    public abstract String[] getDefaultCipherSuites();

    public abstract String[] getSupportedCipherSuites();

    static {
        String s = ((String) AccessController.doPrivileged(new GetPropertyAction("javax.net.debug", ""))).toLowerCase(Locale.ENGLISH);
        boolean z = s.contains("all") || s.contains("ssl");
        DEBUG = z;
    }

    private static void log(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }

    public static synchronized SocketFactory getDefault() {
        StringBuilder stringBuilder;
        synchronized (SSLSocketFactory.class) {
            SSLSocketFactory previousDefaultSocketFactory;
            if (defaultSocketFactory == null || lastVersion != Security.getVersion()) {
                SSLSocketFactory fac;
                lastVersion = Security.getVersion();
                previousDefaultSocketFactory = defaultSocketFactory;
                Exception e = null;
                defaultSocketFactory = null;
                String clsName = getSecurityProperty("ssl.SocketFactory.provider");
                if (clsName != null) {
                    if (previousDefaultSocketFactory == null || !clsName.equals(previousDefaultSocketFactory.getClass().getName())) {
                        log("setting up default SSLSocketFactory");
                        try {
                            e = Class.forName(clsName);
                        } catch (ClassNotFoundException e2) {
                            try {
                                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                                if (cl == null) {
                                    cl = ClassLoader.getSystemClassLoader();
                                }
                                if (cl != null) {
                                    e = Class.forName(clsName, true, cl);
                                }
                            } catch (Exception e3) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("SSLSocketFactory instantiation failed: ");
                                stringBuilder.append(e3.toString());
                                log(stringBuilder.toString());
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("class ");
                        stringBuilder.append(clsName);
                        stringBuilder.append(" is loaded");
                        log(stringBuilder.toString());
                        fac = (SSLSocketFactory) e3.newInstance();
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("instantiated an instance of class ");
                        stringBuilder2.append(clsName);
                        log(stringBuilder2.toString());
                        defaultSocketFactory = fac;
                        return fac;
                    }
                    defaultSocketFactory = previousDefaultSocketFactory;
                    Class<?> cls = defaultSocketFactory;
                    return cls;
                }
                try {
                    SSLContext context = SSLContext.getDefault();
                    if (context != null) {
                        defaultSocketFactory = context.getSocketFactory();
                    } else {
                        defaultSocketFactory = new DefaultSSLSocketFactory(new IllegalStateException("No factory found."));
                    }
                    fac = defaultSocketFactory;
                    return fac;
                } catch (NoSuchAlgorithmException e4) {
                    return new DefaultSSLSocketFactory(e4);
                }
            }
            previousDefaultSocketFactory = defaultSocketFactory;
            return previousDefaultSocketFactory;
        }
    }

    static String getSecurityProperty(final String name) {
        return (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                String s = Security.getProperty(name);
                if (s == null) {
                    return s;
                }
                s = s.trim();
                if (s.length() == 0) {
                    return null;
                }
                return s;
            }
        });
    }

    public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException {
        throw new UnsupportedOperationException();
    }
}
