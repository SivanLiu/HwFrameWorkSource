package javax.xml.xpath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import libcore.io.IoUtils;

final class XPathFactoryFinder {
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static final Class SERVICE_CLASS = XPathFactory.class;
    private static final String SERVICE_ID;
    private static boolean debug;
    private final ClassLoader classLoader;

    private static class CacheHolder {
        private static Properties cacheProps = new Properties();

        private CacheHolder() {
        }

        static {
            String javah = System.getProperty("java.home");
            String configFile = new StringBuilder();
            configFile.append(javah);
            configFile.append(File.separator);
            configFile.append("lib");
            configFile.append(File.separator);
            configFile.append("jaxp.properties");
            File f = new File(configFile.toString());
            if (f.exists()) {
                if (XPathFactoryFinder.debug) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Read properties file ");
                    stringBuilder.append(f);
                    XPathFactoryFinder.debugPrintln(stringBuilder.toString());
                }
                FileInputStream inputStream;
                try {
                    inputStream = new FileInputStream(f);
                    cacheProps.load(inputStream);
                    inputStream.close();
                } catch (Exception ex) {
                    if (XPathFactoryFinder.debug) {
                        ex.printStackTrace();
                    }
                } catch (Throwable th) {
                    r4.addSuppressed(th);
                }
            }
        }
    }

    static {
        boolean z = false;
        debug = false;
        String val = System.getProperty("jaxp.debug");
        if (!(val == null || "false".equals(val))) {
            z = true;
        }
        debug = z;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("META-INF/services/");
        stringBuilder.append(SERVICE_CLASS.getName());
        SERVICE_ID = stringBuilder.toString();
    }

    private static void debugPrintln(String msg) {
        if (debug) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("JAXP: ");
            stringBuilder.append(msg);
            printStream.println(stringBuilder.toString());
        }
    }

    public XPathFactoryFinder(ClassLoader loader) {
        this.classLoader = loader;
        if (debug) {
            debugDisplayClassLoader();
        }
    }

    private void debugDisplayClassLoader() {
        StringBuilder stringBuilder;
        if (this.classLoader == Thread.currentThread().getContextClassLoader()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("using thread context class loader (");
            stringBuilder.append(this.classLoader);
            stringBuilder.append(") for search");
            debugPrintln(stringBuilder.toString());
        } else if (this.classLoader == ClassLoader.getSystemClassLoader()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("using system class loader (");
            stringBuilder.append(this.classLoader);
            stringBuilder.append(") for search");
            debugPrintln(stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("using class loader (");
            stringBuilder.append(this.classLoader);
            stringBuilder.append(") for search");
            debugPrintln(stringBuilder.toString());
        }
    }

    public XPathFactory newFactory(String uri) {
        if (uri != null) {
            XPathFactory f = _newFactory(uri);
            if (debug) {
                StringBuilder stringBuilder;
                if (f != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("factory '");
                    stringBuilder.append(f.getClass().getName());
                    stringBuilder.append("' was found for ");
                    stringBuilder.append(uri);
                    debugPrintln(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to find a factory for ");
                    stringBuilder.append(uri);
                    debugPrintln(stringBuilder.toString());
                }
            }
            return f;
        }
        throw new NullPointerException("uri == null");
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x0081 A:{Catch:{ Exception -> 0x00a4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x009c A:{Catch:{ Exception -> 0x00a4 }} */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00ba  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x011d  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x010d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private XPathFactory _newFactory(String uri) {
        String propertyName = new StringBuilder();
        propertyName.append(SERVICE_CLASS.getName());
        propertyName.append(":");
        propertyName.append(uri);
        propertyName = propertyName.toString();
        try {
            if (debug) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Looking up system property '");
                stringBuilder.append(propertyName);
                stringBuilder.append("'");
                debugPrintln(stringBuilder.toString());
            }
            String r = System.getProperty(propertyName);
            StringBuilder stringBuilder2;
            XPathFactory xpf;
            if (r == null || r.length() <= 0) {
                if (debug) {
                    debugPrintln("The property is undefined.");
                }
                try {
                    r = CacheHolder.cacheProps.getProperty(propertyName);
                    if (debug) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("found ");
                        stringBuilder2.append(r);
                        stringBuilder2.append(" in $java.home/jaxp.properties");
                        debugPrintln(stringBuilder2.toString());
                    }
                    if (r != null) {
                        xpf = createInstance(r);
                        if (xpf != null) {
                            return xpf;
                        }
                    }
                } catch (Exception ex) {
                    if (debug) {
                        ex.printStackTrace();
                    }
                }
                for (URL resource : createServiceFileIterator()) {
                    if (debug) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("looking into ");
                        stringBuilder3.append(resource);
                        debugPrintln(stringBuilder3.toString());
                    }
                    try {
                        XPathFactory xpf2 = loadFromServicesFile(uri, resource.toExternalForm(), resource.openStream());
                        if (xpf2 != null) {
                            return xpf2;
                        }
                    } catch (IOException e) {
                        if (debug) {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("failed to read ");
                            stringBuilder4.append(resource);
                            debugPrintln(stringBuilder4.toString());
                            e.printStackTrace();
                        }
                    }
                }
                if (uri.equals("http://java.sun.com/jaxp/xpath/dom")) {
                    if (debug) {
                        debugPrintln("all things were tried, but none was found. bailing out.");
                    }
                    return null;
                }
                if (debug) {
                    debugPrintln("attempting to use the platform default W3C DOM XPath lib");
                }
                return createInstance("org.apache.xpath.jaxp.XPathFactoryImpl");
            }
            if (debug) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("The value is '");
                stringBuilder2.append(r);
                stringBuilder2.append("'");
                debugPrintln(stringBuilder2.toString());
            }
            xpf = createInstance(r);
            if (xpf != null) {
                return xpf;
            }
            r = CacheHolder.cacheProps.getProperty(propertyName);
            if (debug) {
            }
            if (r != null) {
            }
            for (URL resource2 : createServiceFileIterator()) {
            }
            if (uri.equals("http://java.sun.com/jaxp/xpath/dom")) {
            }
        } catch (Exception ex2) {
            ex2.printStackTrace();
        }
    }

    XPathFactory createInstance(String className) {
        StringBuilder stringBuilder;
        try {
            Class clazz;
            if (debug) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("instantiating ");
                stringBuilder2.append(className);
                debugPrintln(stringBuilder2.toString());
            }
            if (this.classLoader != null) {
                clazz = this.classLoader.loadClass(className);
            } else {
                clazz = Class.forName(className);
            }
            if (debug) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("loaded it from ");
                stringBuilder.append(which(clazz));
                debugPrintln(stringBuilder.toString());
            }
            Object o = clazz.newInstance();
            if (o instanceof XPathFactory) {
                return (XPathFactory) o;
            }
            if (debug) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(className);
                stringBuilder3.append(" is not assignable to ");
                stringBuilder3.append(SERVICE_CLASS.getName());
                debugPrintln(stringBuilder3.toString());
            }
            return null;
        } catch (VirtualMachineError vme) {
            throw vme;
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            if (debug) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to instantiate ");
                stringBuilder.append(className);
                debugPrintln(stringBuilder.toString());
                t.printStackTrace();
            }
        }
    }

    private XPathFactory loadFromServicesFile(String uri, String resourceName, InputStream in) {
        AutoCloseable rd;
        if (debug) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading ");
            stringBuilder.append(resourceName);
            debugPrintln(stringBuilder.toString());
        }
        try {
            rd = new BufferedReader(new InputStreamReader(in, "UTF-8"), 80);
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(in), 80);
        }
        XPathFactory resultFactory = null;
        while (true) {
            try {
                String factoryClassName = rd.readLine();
                if (factoryClassName == null) {
                    break;
                }
                int hashIndex = factoryClassName.indexOf(35);
                if (hashIndex != -1) {
                    factoryClassName = factoryClassName.substring(0, hashIndex);
                }
                factoryClassName = factoryClassName.trim();
                if (factoryClassName.length() != 0) {
                    try {
                        XPathFactory foundFactory = createInstance(factoryClassName);
                        if (foundFactory.isObjectModelSupported(uri)) {
                            resultFactory = foundFactory;
                            break;
                        }
                    } catch (Exception e2) {
                    }
                }
            } catch (IOException e3) {
            }
        }
        IoUtils.closeQuietly(rd);
        return resultFactory;
    }

    private Iterable<URL> createServiceFileIterator() {
        if (this.classLoader == null) {
            return Collections.singleton(XPathFactoryFinder.class.getClassLoader().getResource(SERVICE_ID));
        }
        StringBuilder stringBuilder;
        try {
            Enumeration<URL> e = this.classLoader.getResources(SERVICE_ID);
            if (debug && !e.hasMoreElements()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("no ");
                stringBuilder.append(SERVICE_ID);
                stringBuilder.append(" file was found");
                debugPrintln(stringBuilder.toString());
            }
            return Collections.list(e);
        } catch (IOException e2) {
            if (debug) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to enumerate resources ");
                stringBuilder.append(SERVICE_ID);
                debugPrintln(stringBuilder.toString());
                e2.printStackTrace();
            }
            return Collections.emptySet();
        }
    }

    private static String which(Class clazz) {
        return which(clazz.getName(), clazz.getClassLoader());
    }

    private static String which(String classname, ClassLoader loader) {
        String classnameAsResource = new StringBuilder();
        classnameAsResource.append(classname.replace('.', '/'));
        classnameAsResource.append(".class");
        classnameAsResource = classnameAsResource.toString();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        URL it = loader.getResource(classnameAsResource);
        return it != null ? it.toString() : null;
    }
}
