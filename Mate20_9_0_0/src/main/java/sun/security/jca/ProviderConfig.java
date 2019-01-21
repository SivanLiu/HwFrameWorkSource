package sun.security.jca;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.ProviderException;
import sun.security.util.Debug;
import sun.security.util.PropertyExpander;

final class ProviderConfig {
    private static final Class[] CL_STRING = new Class[]{String.class};
    private static final int MAX_LOAD_TRIES = 30;
    private static final String P11_SOL_ARG = "${java.home}/lib/security/sunpkcs11-solaris.cfg";
    private static final String P11_SOL_NAME = "sun.security.pkcs11.SunPKCS11";
    private static final Debug debug = Debug.getInstance("jca", "ProviderConfig");
    private final String argument;
    private final String className;
    private boolean isLoading;
    private volatile Provider provider;
    private int tries;

    ProviderConfig(String className, String argument) {
        if (className.equals(P11_SOL_NAME) && argument.equals(P11_SOL_ARG)) {
            checkSunPKCS11Solaris();
        }
        this.className = className;
        this.argument = expand(argument);
    }

    ProviderConfig(String className) {
        this(className, "");
    }

    ProviderConfig(Provider provider) {
        this.className = provider.getClass().getName();
        this.argument = "";
        this.provider = provider;
    }

    private void checkSunPKCS11Solaris() {
        if (((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                if (!new File("/usr/lib/libpkcs11.so").exists()) {
                    return Boolean.FALSE;
                }
                if ("false".equalsIgnoreCase(System.getProperty("sun.security.pkcs11.enable-solaris"))) {
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        })) == Boolean.FALSE) {
            this.tries = 30;
        }
    }

    private boolean hasArgument() {
        return this.argument.length() != 0;
    }

    private boolean shouldLoad() {
        return this.tries < 30;
    }

    private void disableLoad() {
        this.tries = 30;
    }

    boolean isLoaded() {
        return this.provider != null;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProviderConfig)) {
            return false;
        }
        ProviderConfig other = (ProviderConfig) obj;
        if (!(this.className.equals(other.className) && this.argument.equals(other.argument))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return this.className.hashCode() + this.argument.hashCode();
    }

    public String toString() {
        if (!hasArgument()) {
            return this.className;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.className);
        stringBuilder.append("('");
        stringBuilder.append(this.argument);
        stringBuilder.append("')");
        return stringBuilder.toString();
    }

    /* JADX WARNING: Missing block: B:19:0x0039, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    synchronized Provider getProvider() {
        Provider p = this.provider;
        if (p != null) {
            return p;
        }
        if (!shouldLoad()) {
            return null;
        }
        if (!this.isLoading) {
            try {
                this.isLoading = true;
                this.tries++;
                p = doLoadProvider();
                this.provider = p;
                return p;
            } finally {
                this.isLoading = false;
            }
        } else if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Recursion loading provider: ");
            stringBuilder.append((Object) this);
            debug.println(stringBuilder.toString());
            new Exception("Call trace").printStackTrace();
        }
    }

    private Provider doLoadProvider() {
        return (Provider) AccessController.doPrivileged(new PrivilegedAction<Provider>() {
            public Provider run() {
                Throwable t;
                if (ProviderConfig.debug != null) {
                    Debug access$000 = ProviderConfig.debug;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Loading provider: ");
                    stringBuilder.append(ProviderConfig.this);
                    access$000.println(stringBuilder.toString());
                }
                try {
                    return ProviderConfig.this.initProvider(ProviderConfig.this.className, Object.class.getClassLoader());
                } catch (Exception e) {
                    try {
                        return ProviderConfig.this.initProvider(ProviderConfig.this.className, ClassLoader.getSystemClassLoader());
                    } catch (Exception e2) {
                        if (e2 instanceof InvocationTargetException) {
                            t = ((InvocationTargetException) e2).getCause();
                        } else {
                            t = e2;
                        }
                        if (ProviderConfig.debug != null) {
                            Debug access$0002 = ProviderConfig.debug;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Error loading provider ");
                            stringBuilder2.append(ProviderConfig.this);
                            access$0002.println(stringBuilder2.toString());
                            t.printStackTrace();
                        }
                        if (t instanceof ProviderException) {
                            throw ((ProviderException) t);
                        }
                        if (t instanceof UnsupportedOperationException) {
                            ProviderConfig.this.disableLoad();
                        }
                        return null;
                    }
                }
            }
        });
    }

    private Provider initProvider(String className, ClassLoader cl) throws Exception {
        Class<?> provClass;
        Object obj;
        if (cl != null) {
            provClass = cl.loadClass(className);
        } else {
            provClass = Class.forName(className);
        }
        if (hasArgument()) {
            obj = provClass.getConstructor(CL_STRING).newInstance(this.argument);
        } else {
            obj = provClass.newInstance();
        }
        Debug debug;
        StringBuilder stringBuilder;
        if (obj instanceof Provider) {
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Loaded provider ");
                stringBuilder.append(obj);
                debug.println(stringBuilder.toString());
            }
            return (Provider) obj;
        }
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append(className);
            stringBuilder.append(" is not a provider");
            debug.println(stringBuilder.toString());
        }
        disableLoad();
        return null;
    }

    private static String expand(final String value) {
        if (value.contains("${")) {
            return (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    try {
                        return PropertyExpander.expand(value);
                    } catch (GeneralSecurityException e) {
                        throw new ProviderException(e);
                    }
                }
            });
        }
        return value;
    }
}
