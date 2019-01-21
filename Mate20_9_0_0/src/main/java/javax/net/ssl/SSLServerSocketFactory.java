package javax.net.ssl;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import javax.net.ServerSocketFactory;

public abstract class SSLServerSocketFactory extends ServerSocketFactory {
    private static SSLServerSocketFactory defaultServerSocketFactory;
    private static int lastVersion = -1;

    public abstract String[] getDefaultCipherSuites();

    public abstract String[] getSupportedCipherSuites();

    private static void log(String msg) {
        if (SSLSocketFactory.DEBUG) {
            System.out.println(msg);
        }
    }

    protected SSLServerSocketFactory() {
    }

    public static synchronized ServerSocketFactory getDefault() {
        StringBuilder stringBuilder;
        synchronized (SSLServerSocketFactory.class) {
            SSLServerSocketFactory previousDefaultServerSocketFactory;
            if (defaultServerSocketFactory == null || lastVersion != Security.getVersion()) {
                SSLServerSocketFactory fac;
                lastVersion = Security.getVersion();
                previousDefaultServerSocketFactory = defaultServerSocketFactory;
                Exception e = null;
                defaultServerSocketFactory = null;
                String clsName = SSLSocketFactory.getSecurityProperty("ssl.ServerSocketFactory.provider");
                if (clsName != null) {
                    if (previousDefaultServerSocketFactory == null || !clsName.equals(previousDefaultServerSocketFactory.getClass().getName())) {
                        log("setting up default SSLServerSocketFactory");
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
                                stringBuilder.append("SSLServerSocketFactory instantiation failed: ");
                                stringBuilder.append(e3);
                                log(stringBuilder.toString());
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("class ");
                        stringBuilder.append(clsName);
                        stringBuilder.append(" is loaded");
                        log(stringBuilder.toString());
                        fac = (SSLServerSocketFactory) e.newInstance();
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("instantiated an instance of class ");
                        stringBuilder2.append(clsName);
                        log(stringBuilder2.toString());
                        defaultServerSocketFactory = fac;
                        return fac;
                    }
                    defaultServerSocketFactory = previousDefaultServerSocketFactory;
                    Class<?> cls = defaultServerSocketFactory;
                    return cls;
                }
                try {
                    SSLContext context = SSLContext.getDefault();
                    if (context != null) {
                        defaultServerSocketFactory = context.getServerSocketFactory();
                    } else {
                        defaultServerSocketFactory = new DefaultSSLServerSocketFactory(new IllegalStateException("No factory found."));
                    }
                    fac = defaultServerSocketFactory;
                    return fac;
                } catch (NoSuchAlgorithmException e4) {
                    return new DefaultSSLServerSocketFactory(e4);
                }
            }
            previousDefaultServerSocketFactory = defaultServerSocketFactory;
            return previousDefaultServerSocketFactory;
        }
    }
}
