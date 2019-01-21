package javax.xml.validation;

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
import javax.xml.XMLConstants;
import libcore.io.IoUtils;

final class SchemaFactoryFinder {
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static final Class SERVICE_CLASS = SchemaFactory.class;
    private static final String SERVICE_ID;
    private static final String W3C_XML_SCHEMA10_NS_URI = "http://www.w3.org/XML/XMLSchema/v1.0";
    private static final String W3C_XML_SCHEMA11_NS_URI = "http://www.w3.org/XML/XMLSchema/v1.1";
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
                if (SchemaFactoryFinder.debug) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Read properties file ");
                    stringBuilder.append(f);
                    SchemaFactoryFinder.debugPrintln(stringBuilder.toString());
                }
                FileInputStream inputStream;
                try {
                    inputStream = new FileInputStream(f);
                    cacheProps.load(inputStream);
                    inputStream.close();
                } catch (Exception ex) {
                    if (SchemaFactoryFinder.debug) {
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

    public SchemaFactoryFinder(ClassLoader loader) {
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

    public SchemaFactory newFactory(String schemaLanguage) {
        if (schemaLanguage != null) {
            SchemaFactory f = _newFactory(schemaLanguage);
            if (debug) {
                StringBuilder stringBuilder;
                if (f != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("factory '");
                    stringBuilder.append(f.getClass().getName());
                    stringBuilder.append("' was found for ");
                    stringBuilder.append(schemaLanguage);
                    debugPrintln(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to find a factory for ");
                    stringBuilder.append(schemaLanguage);
                    debugPrintln(stringBuilder.toString());
                }
            }
            return f;
        }
        throw new NullPointerException("schemaLanguage == null");
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x009e A:{Catch:{ Exception -> 0x00c1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00b9 A:{Catch:{ Exception -> 0x00c1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x00d7  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x015a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private SchemaFactory _newFactory(String schemaLanguage) {
        String propertyName = new StringBuilder();
        propertyName.append(SERVICE_CLASS.getName());
        propertyName.append(":");
        propertyName.append(schemaLanguage);
        propertyName = propertyName.toString();
        StringBuilder stringBuilder;
        try {
            if (debug) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Looking up system property '");
                stringBuilder2.append(propertyName);
                stringBuilder2.append("'");
                debugPrintln(stringBuilder2.toString());
            }
            String r = System.getProperty(propertyName);
            SchemaFactory sf;
            if (r == null || r.length() <= 0) {
                if (debug) {
                    debugPrintln("The property is undefined.");
                }
                try {
                    r = CacheHolder.cacheProps.getProperty(propertyName);
                    if (debug) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("found ");
                        stringBuilder.append(r);
                        stringBuilder.append(" in $java.home/jaxp.properties");
                        debugPrintln(stringBuilder.toString());
                    }
                    if (r != null) {
                        sf = createInstance(r);
                        if (sf != null) {
                            return sf;
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
                        SchemaFactory sf2 = loadFromServicesFile(schemaLanguage, resource.toExternalForm(), resource.openStream());
                        if (sf2 != null) {
                            return sf2;
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
                if (!schemaLanguage.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI) || schemaLanguage.equals(W3C_XML_SCHEMA10_NS_URI)) {
                    if (debug) {
                        debugPrintln("attempting to use the platform default XML Schema 1.0 validator");
                    }
                    return createInstance("org.apache.xerces.jaxp.validation.XMLSchemaFactory");
                } else if (schemaLanguage.equals(W3C_XML_SCHEMA11_NS_URI)) {
                    if (debug) {
                        debugPrintln("attempting to use the platform default XML Schema 1.1 validator");
                    }
                    return createInstance("org.apache.xerces.jaxp.validation.XMLSchema11Factory");
                } else {
                    if (debug) {
                        debugPrintln("all things were tried, but none was found. bailing out.");
                    }
                    return null;
                }
            }
            if (debug) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("The value is '");
                stringBuilder.append(r);
                stringBuilder.append("'");
                debugPrintln(stringBuilder.toString());
            }
            sf = createInstance(r);
            if (sf != null) {
                return sf;
            }
            r = CacheHolder.cacheProps.getProperty(propertyName);
            if (debug) {
            }
            if (r != null) {
            }
            for (URL resource2 : createServiceFileIterator()) {
            }
            if (schemaLanguage.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            }
            if (debug) {
            }
            return createInstance("org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        } catch (VirtualMachineError vme) {
            throw vme;
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            if (debug) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to look up system property '");
                stringBuilder.append(propertyName);
                stringBuilder.append("'");
                debugPrintln(stringBuilder.toString());
                t.printStackTrace();
            }
        }
    }

    SchemaFactory createInstance(String className) {
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
            if (o instanceof SchemaFactory) {
                return (SchemaFactory) o;
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
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed to instantiate ");
            stringBuilder.append(className);
            debugPrintln(stringBuilder.toString());
            if (debug) {
                t.printStackTrace();
            }
        }
    }

    private Iterable<URL> createServiceFileIterator() {
        if (this.classLoader == null) {
            return Collections.singleton(SchemaFactoryFinder.class.getClassLoader().getResource(SERVICE_ID));
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

    private SchemaFactory loadFromServicesFile(String schemaLanguage, String resourceName, InputStream in) {
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
        SchemaFactory resultFactory = null;
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
                        SchemaFactory foundFactory = createInstance(factoryClassName);
                        if (foundFactory.isSchemaLanguageSupported(schemaLanguage)) {
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
