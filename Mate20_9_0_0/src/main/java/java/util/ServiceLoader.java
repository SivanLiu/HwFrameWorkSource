package java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map.Entry;

public final class ServiceLoader<S> implements Iterable<S> {
    private static final String PREFIX = "META-INF/services/";
    private final ClassLoader loader;
    private LazyIterator lookupIterator;
    private LinkedHashMap<String, S> providers = new LinkedHashMap();
    private final Class<S> service;

    private class LazyIterator implements Iterator<S> {
        Enumeration<URL> configs;
        ClassLoader loader;
        String nextName;
        Iterator<String> pending;
        Class<S> service;

        /* synthetic */ LazyIterator(ServiceLoader x0, Class x1, ClassLoader x2, AnonymousClass1 x3) {
            this(x1, x2);
        }

        private LazyIterator(Class<S> service, ClassLoader loader) {
            this.configs = null;
            this.pending = null;
            this.nextName = null;
            this.service = service;
            this.loader = loader;
        }

        private boolean hasNextService() {
            if (this.nextName != null) {
                return true;
            }
            if (this.configs == null) {
                try {
                    String fullName = new StringBuilder();
                    fullName.append(ServiceLoader.PREFIX);
                    fullName.append(this.service.getName());
                    fullName = fullName.toString();
                    if (this.loader == null) {
                        this.configs = ClassLoader.getSystemResources(fullName);
                    } else {
                        this.configs = this.loader.getResources(fullName);
                    }
                } catch (IOException x) {
                    ServiceLoader.fail(this.service, "Error locating configuration files", x);
                }
            }
            while (true) {
                if (this.pending != null && this.pending.hasNext()) {
                    this.nextName = (String) this.pending.next();
                    return true;
                } else if (!this.configs.hasMoreElements()) {
                    return false;
                } else {
                    this.pending = ServiceLoader.this.parse(this.service, (URL) this.configs.nextElement());
                }
            }
        }

        private S nextService() {
            Class cls;
            StringBuilder stringBuilder;
            if (hasNextService()) {
                String cn = this.nextName;
                Class<?> c = null;
                this.nextName = null;
                try {
                    c = Class.forName(cn, false, this.loader);
                } catch (ClassNotFoundException x) {
                    cls = this.service;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider ");
                    stringBuilder.append(cn);
                    stringBuilder.append(" not found");
                    ServiceLoader.fail(cls, stringBuilder.toString(), x);
                }
                if (!this.service.isAssignableFrom(c)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this.service.getCanonicalName());
                    stringBuilder2.append(" is not assignable from ");
                    stringBuilder2.append(c.getCanonicalName());
                    ClassCastException cce = new ClassCastException(stringBuilder2.toString());
                    cls = this.service;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider ");
                    stringBuilder.append(cn);
                    stringBuilder.append(" not a subtype");
                    ServiceLoader.fail(cls, stringBuilder.toString(), cce);
                }
                try {
                    S p = this.service.cast(c.newInstance());
                    ServiceLoader.this.providers.put(cn, p);
                    return p;
                } catch (Throwable x2) {
                    cls = this.service;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Provider ");
                    stringBuilder.append(cn);
                    stringBuilder.append(" could not be instantiated");
                    ServiceLoader.fail(cls, stringBuilder.toString(), x2);
                    Error error = new Error();
                }
            } else {
                throw new NoSuchElementException();
            }
        }

        public boolean hasNext() {
            return hasNextService();
        }

        public S next() {
            return nextService();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public void reload() {
        this.providers.clear();
        this.lookupIterator = new LazyIterator(this, this.service, this.loader, null);
    }

    private ServiceLoader(Class<S> svc, ClassLoader cl) {
        this.service = (Class) Objects.requireNonNull((Object) svc, "Service interface cannot be null");
        this.loader = cl == null ? ClassLoader.getSystemClassLoader() : cl;
        reload();
    }

    private static void fail(Class<?> service, String msg, Throwable cause) throws ServiceConfigurationError {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(service.getName());
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        throw new ServiceConfigurationError(stringBuilder.toString(), cause);
    }

    private static void fail(Class<?> service, String msg) throws ServiceConfigurationError {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(service.getName());
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        throw new ServiceConfigurationError(stringBuilder.toString());
    }

    private static void fail(Class<?> service, URL u, int line, String msg) throws ServiceConfigurationError {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((Object) u);
        stringBuilder.append(":");
        stringBuilder.append(line);
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        fail(service, stringBuilder.toString());
    }

    private int parseLine(Class<?> service, URL u, BufferedReader r, int lc, List<String> names) throws IOException, ServiceConfigurationError {
        String ln = r.readLine();
        if (ln == null) {
            return -1;
        }
        int ci = ln.indexOf(35);
        if (ci >= 0) {
            ln = ln.substring(0, ci);
        }
        ln = ln.trim();
        int n = ln.length();
        if (n != 0) {
            if (ln.indexOf(32) >= 0 || ln.indexOf(9) >= 0) {
                fail(service, u, lc, "Illegal configuration-file syntax");
            }
            int cp = ln.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal provider-class name: ");
                stringBuilder.append(ln);
                fail(service, u, lc, stringBuilder.toString());
            }
            int i = Character.charCount(cp);
            while (i < n) {
                cp = ln.codePointAt(i);
                if (!(Character.isJavaIdentifierPart(cp) || cp == 46)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Illegal provider-class name: ");
                    stringBuilder2.append(ln);
                    fail(service, u, lc, stringBuilder2.toString());
                }
                i += Character.charCount(cp);
            }
            if (!(this.providers.containsKey(ln) || names.contains(ln))) {
                names.add(ln);
            }
        }
        return lc + 1;
    }

    private Iterator<String> parse(Class<?> service, URL u) throws ServiceConfigurationError {
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList();
        try {
            in = u.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while (true) {
                int parseLine = parseLine(service, u, r, lc, names);
                lc = parseLine;
                if (parseLine < 0) {
                    try {
                        break;
                    } catch (IOException y) {
                        fail(service, "Error closing configuration file", y);
                    }
                }
            }
            r.close();
            if (in != null) {
                in.close();
            }
        } catch (IOException y2) {
            fail(service, "Error reading configuration file", y2);
            if (r != null) {
                r.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException y3) {
                    fail(service, "Error closing configuration file", y3);
                }
            }
            if (in != null) {
                in.close();
            }
        }
        return names.iterator();
    }

    public Iterator<S> iterator() {
        return new Iterator<S>() {
            Iterator<Entry<String, S>> knownProviders = ServiceLoader.this.providers.entrySet().iterator();

            public boolean hasNext() {
                if (this.knownProviders.hasNext()) {
                    return true;
                }
                return ServiceLoader.this.lookupIterator.hasNext();
            }

            public S next() {
                if (this.knownProviders.hasNext()) {
                    return ((Entry) this.knownProviders.next()).getValue();
                }
                return ServiceLoader.this.lookupIterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new ServiceLoader(service, loader);
    }

    public static <S> ServiceLoader<S> load(Class<S> service) {
        return load(service, Thread.currentThread().getContextClassLoader());
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        ClassLoader prev = null;
        for (ClassLoader cl = ClassLoader.getSystemClassLoader(); cl != null; cl = cl.getParent()) {
            prev = cl;
        }
        return load(service, prev);
    }

    public static <S> S loadFromSystemProperty(Class<S> service) {
        try {
            String className = System.getProperty(service.getName());
            if (className != null) {
                return ClassLoader.getSystemClassLoader().loadClass(className).newInstance();
            }
            return null;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("java.util.ServiceLoader[");
        stringBuilder.append(this.service.getName());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
