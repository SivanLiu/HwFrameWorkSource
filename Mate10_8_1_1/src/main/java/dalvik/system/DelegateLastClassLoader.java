package dalvik.system;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import sun.misc.CompoundEnumeration;

public final class DelegateLastClassLoader extends PathClassLoader {
    public DelegateLastClassLoader(String dexPath, ClassLoader parent) {
        super(dexPath, parent);
    }

    public DelegateLastClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        super(dexPath, librarySearchPath, parent);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> cl = findLoadedClass(name);
        if (cl != null) {
            return cl;
        }
        try {
            return Object.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            try {
                return findClass(name);
            } catch (ClassNotFoundException ex) {
                ClassNotFoundException fromSuper = ex;
                try {
                    return getParent().loadClass(name);
                } catch (ClassNotFoundException e2) {
                    throw ex;
                }
            }
        }
    }

    public URL getResource(String name) {
        URL url = null;
        URL resource = Object.class.getClassLoader().getResource(name);
        if (resource != null) {
            return resource;
        }
        resource = findResource(name);
        if (resource != null) {
            return resource;
        }
        ClassLoader cl = getParent();
        if (cl != null) {
            url = cl.getResource(name);
        }
        return url;
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration enumeration = null;
        Enumeration<URL>[] resources = new Enumeration[3];
        resources[0] = Object.class.getClassLoader().getResources(name);
        resources[1] = findResources(name);
        if (getParent() != null) {
            enumeration = getParent().getResources(name);
        }
        resources[2] = enumeration;
        return new CompoundEnumeration(resources);
    }
}
