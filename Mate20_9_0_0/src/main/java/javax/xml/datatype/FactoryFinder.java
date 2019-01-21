package javax.xml.datatype;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Properties;
import libcore.io.IoUtils;

final class FactoryFinder {
    private static final String CLASS_NAME = "javax.xml.datatype.FactoryFinder";
    private static final int DEFAULT_LINE_LENGTH = 80;
    private static boolean debug;

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
                if (FactoryFinder.debug) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Read properties file ");
                    stringBuilder.append(f);
                    FactoryFinder.debugPrintln(stringBuilder.toString());
                }
                FileInputStream inputStream;
                try {
                    inputStream = new FileInputStream(f);
                    cacheProps.load(inputStream);
                    inputStream.close();
                } catch (Exception ex) {
                    if (FactoryFinder.debug) {
                        ex.printStackTrace();
                    }
                } catch (Throwable th) {
                    r4.addSuppressed(th);
                }
            }
        }
    }

    static class ConfigurationError extends Error {
        private static final long serialVersionUID = -3644413026244211347L;
        private Exception exception;

        ConfigurationError(String msg, Exception x) {
            super(msg);
            this.exception = x;
        }

        Exception getException() {
            return this.exception;
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
    }

    private FactoryFinder() {
    }

    private static void debugPrintln(String msg) {
        if (debug) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("javax.xml.datatype.FactoryFinder:");
            stringBuilder.append(msg);
            printStream.println(stringBuilder.toString());
        }
    }

    private static ClassLoader findClassLoader() throws ConfigurationError {
        StringBuilder stringBuilder;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (debug) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Using context class loader: ");
            stringBuilder.append(classLoader);
            debugPrintln(stringBuilder.toString());
        }
        if (classLoader == null) {
            classLoader = FactoryFinder.class.getClassLoader();
            if (debug) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Using the class loader of FactoryFinder: ");
                stringBuilder.append(classLoader);
                debugPrintln(stringBuilder.toString());
            }
        }
        return classLoader;
    }

    static Object newInstance(String className, ClassLoader classLoader) throws ConfigurationError {
        ClassNotFoundException x;
        StringBuilder stringBuilder;
        if (classLoader == null) {
            try {
                x = Class.forName(className);
            } catch (ClassNotFoundException x2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Provider ");
                stringBuilder.append(className);
                stringBuilder.append(" not found");
                throw new ConfigurationError(stringBuilder.toString(), x2);
            } catch (Exception x22) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Provider ");
                stringBuilder.append(className);
                stringBuilder.append(" could not be instantiated: ");
                stringBuilder.append(x22);
                throw new ConfigurationError(stringBuilder.toString(), x22);
            }
        }
        x22 = classLoader.loadClass(className);
        if (debug) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Loaded ");
            stringBuilder2.append(className);
            stringBuilder2.append(" from ");
            stringBuilder2.append(which(x22));
            debugPrintln(stringBuilder2.toString());
        }
        return x22.newInstance();
    }

    static Object find(String factoryId, String fallbackClassName) throws ConfigurationError {
        ClassLoader classLoader = findClassLoader();
        String systemProp = System.getProperty(factoryId);
        if (systemProp == null || systemProp.length() <= 0) {
            StringBuilder stringBuilder;
            try {
                String factoryClassName = CacheHolder.cacheProps.getProperty(factoryId);
                if (debug) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("found ");
                    stringBuilder.append(factoryClassName);
                    stringBuilder.append(" in $java.home/jaxp.properties");
                    debugPrintln(stringBuilder.toString());
                }
                if (factoryClassName != null) {
                    return newInstance(factoryClassName, classLoader);
                }
            } catch (Exception ex) {
                if (debug) {
                    ex.printStackTrace();
                }
            }
            Object provider = findJarServiceProvider(factoryId);
            if (provider != null) {
                return provider;
            }
            if (fallbackClassName != null) {
                if (debug) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("loaded from fallback value: ");
                    stringBuilder.append(fallbackClassName);
                    debugPrintln(stringBuilder.toString());
                }
                return newInstance(fallbackClassName, classLoader);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Provider for ");
            stringBuilder2.append(factoryId);
            stringBuilder2.append(" cannot be found");
            throw new ConfigurationError(stringBuilder2.toString(), null);
        }
        if (debug) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("found ");
            stringBuilder3.append(systemProp);
            stringBuilder3.append(" in the system property ");
            stringBuilder3.append(factoryId);
            debugPrintln(stringBuilder3.toString());
        }
        return newInstance(systemProp, classLoader);
    }

    private static Object findJarServiceProvider(String factoryId) throws ConfigurationError {
        String serviceId = new StringBuilder();
        serviceId.append("META-INF/services/");
        serviceId.append(factoryId);
        serviceId = serviceId.toString();
        InputStream is = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            is = cl.getResourceAsStream(serviceId);
        }
        if (is == null) {
            cl = FactoryFinder.class.getClassLoader();
            is = cl.getResourceAsStream(serviceId);
        }
        if (is == null) {
            return null;
        }
        AutoCloseable rd;
        if (debug) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("found jar resource=");
            stringBuilder.append(serviceId);
            stringBuilder.append(" using ClassLoader: ");
            stringBuilder.append(cl);
            debugPrintln(stringBuilder.toString());
        }
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"), 80);
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is), 80);
        }
        String factoryClassName = null;
        try {
            factoryClassName = rd.readLine();
            if (factoryClassName == null || "".equals(factoryClassName)) {
                return null;
            }
            if (debug) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("found in resource, value=");
                stringBuilder2.append(factoryClassName);
                debugPrintln(stringBuilder2.toString());
            }
            return newInstance(factoryClassName, cl);
        } catch (IOException e2) {
            return null;
        } finally {
            IoUtils.closeQuietly(rd);
        }
    }

    private static String which(Class clazz) {
        try {
            URL it;
            String classnameAsResource = new StringBuilder();
            classnameAsResource.append(clazz.getName().replace('.', '/'));
            classnameAsResource.append(".class");
            classnameAsResource = classnameAsResource.toString();
            ClassLoader loader = clazz.getClassLoader();
            if (loader != null) {
                it = loader.getResource(classnameAsResource);
            } else {
                it = ClassLoader.getSystemResource(classnameAsResource);
            }
            if (it != null) {
                return it.toString();
            }
        } catch (VirtualMachineError vme) {
            throw vme;
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace();
            }
        }
        return "unknown location";
    }
}
