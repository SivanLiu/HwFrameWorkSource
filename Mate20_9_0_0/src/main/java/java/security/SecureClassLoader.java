package java.security;

import java.nio.ByteBuffer;
import java.util.HashMap;
import sun.security.util.Debug;

public class SecureClassLoader extends ClassLoader {
    private static final Debug debug = Debug.getInstance("scl");
    private final boolean initialized;
    private final HashMap<CodeSource, ProtectionDomain> pdcache = new HashMap(11);

    static {
        ClassLoader.registerAsParallelCapable();
    }

    protected SecureClassLoader(ClassLoader parent) {
        super(parent);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        this.initialized = true;
    }

    protected SecureClassLoader() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        this.initialized = true;
    }

    protected final Class<?> defineClass(String name, byte[] b, int off, int len, CodeSource cs) {
        return defineClass(name, b, off, len, getProtectionDomain(cs));
    }

    protected final Class<?> defineClass(String name, ByteBuffer b, CodeSource cs) {
        return defineClass(name, b, getProtectionDomain(cs));
    }

    protected PermissionCollection getPermissions(CodeSource codesource) {
        check();
        return new Permissions();
    }

    private ProtectionDomain getProtectionDomain(CodeSource cs) {
        if (cs == null) {
            return null;
        }
        ProtectionDomain pd;
        synchronized (this.pdcache) {
            pd = (ProtectionDomain) this.pdcache.get(cs);
            if (pd == null) {
                pd = new ProtectionDomain(cs, getPermissions(cs), this, null);
                this.pdcache.put(cs, pd);
                if (debug != null) {
                    Debug debug = debug;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" getPermissions ");
                    stringBuilder.append((Object) pd);
                    debug.println(stringBuilder.toString());
                    debug.println("");
                }
            }
        }
        return pd;
    }

    private void check() {
        if (!this.initialized) {
            throw new SecurityException("ClassLoader object not initialized");
        }
    }
}
