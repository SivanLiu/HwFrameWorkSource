package org.apache.xml.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.apache.xalan.templates.Constants;

class ObjectFactory {
    private static final boolean DEBUG = false;
    private static final String DEFAULT_PROPERTIES_FILENAME = "xalan.properties";
    private static final String SERVICES_PATH = "META-INF/services/";
    private static long fLastModified = -1;
    private static Properties fXalanProperties = null;

    static class ConfigurationError extends Error {
        static final long serialVersionUID = 2036619216663421552L;
        private Exception exception;

        ConfigurationError(String msg, Exception x) {
            super(msg);
            this.exception = x;
        }

        Exception getException() {
            return this.exception;
        }
    }

    ObjectFactory() {
    }

    static Object createObject(String factoryId, String fallbackClassName) throws ConfigurationError {
        return createObject(factoryId, null, fallbackClassName);
    }

    static Object createObject(String factoryId, String propertiesFilename, String fallbackClassName) throws ConfigurationError {
        Class factoryClass = lookUpFactoryClass(factoryId, propertiesFilename, fallbackClassName);
        StringBuilder stringBuilder;
        if (factoryClass != null) {
            try {
                Object instance = factoryClass.newInstance();
                stringBuilder = new StringBuilder();
                stringBuilder.append("created new instance of factory ");
                stringBuilder.append(factoryId);
                debugPrintln(stringBuilder.toString());
                return instance;
            } catch (Exception x) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Provider for factory ");
                stringBuilder2.append(factoryId);
                stringBuilder2.append(" could not be instantiated: ");
                stringBuilder2.append(x);
                throw new ConfigurationError(stringBuilder2.toString(), x);
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Provider for ");
        stringBuilder.append(factoryId);
        stringBuilder.append(" cannot be found");
        throw new ConfigurationError(stringBuilder.toString(), null);
    }

    static Class lookUpFactoryClass(String factoryId) throws ConfigurationError {
        return lookUpFactoryClass(factoryId, null, null);
    }

    static Class lookUpFactoryClass(String factoryId, String propertiesFilename, String fallbackClassName) throws ConfigurationError {
        StringBuilder stringBuilder;
        String factoryClassName = lookUpFactoryClassName(factoryId, propertiesFilename, fallbackClassName);
        ClassLoader cl = findClassLoader();
        if (factoryClassName == null) {
            factoryClassName = fallbackClassName;
        }
        try {
            Class providerClass = findProviderClass(factoryClassName, cl, true);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("created new instance of ");
            stringBuilder2.append(providerClass);
            stringBuilder2.append(" using ClassLoader: ");
            stringBuilder2.append(cl);
            debugPrintln(stringBuilder2.toString());
            return providerClass;
        } catch (ClassNotFoundException x) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Provider ");
            stringBuilder.append(factoryClassName);
            stringBuilder.append(" not found");
            throw new ConfigurationError(stringBuilder.toString(), x);
        } catch (Exception x2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Provider ");
            stringBuilder.append(factoryClassName);
            stringBuilder.append(" could not be instantiated: ");
            stringBuilder.append(x2);
            throw new ConfigurationError(stringBuilder.toString(), x2);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x00c2  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x0125  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0108  */
    /* JADX WARNING: Missing block: B:31:0x00a8, code skipped:
            if (r12 != null) goto L_0x00aa;
     */
    /* JADX WARNING: Missing block: B:33:?, code skipped:
            r12.close();
     */
    /* JADX WARNING: Missing block: B:40:0x00b9, code skipped:
            if (r12 == null) goto L_0x00bd;
     */
    /* JADX WARNING: Missing block: B:59:0x00ef, code skipped:
            if (r5 != null) goto L_0x00f1;
     */
    /* JADX WARNING: Missing block: B:61:?, code skipped:
            r5.close();
     */
    /* JADX WARNING: Missing block: B:71:0x0103, code skipped:
            if (r5 == null) goto L_0x0106;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static String lookUpFactoryClassName(String factoryId, String propertiesFilename, String fallbackClassName) {
        String systemProp;
        String propertiesFilename2;
        String str = factoryId;
        String propertiesFilename3 = propertiesFilename;
        SecuritySupport ss = SecuritySupport.getInstance();
        try {
            systemProp = ss.getSystemProperty(str);
            if (systemProp != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("found system property, value=");
                stringBuilder.append(systemProp);
                debugPrintln(stringBuilder.toString());
                return systemProp;
            }
        } catch (SecurityException e) {
        }
        String factoryClassName = null;
        FileInputStream fis = null;
        if (propertiesFilename3 == null) {
            File propertiesFile = null;
            boolean propertiesFileExists = false;
            try {
                systemProp = ss.getSystemProperty("java.home");
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(systemProp);
                stringBuilder2.append(File.separator);
                stringBuilder2.append("lib");
                stringBuilder2.append(File.separator);
                stringBuilder2.append(DEFAULT_PROPERTIES_FILENAME);
                propertiesFilename3 = stringBuilder2.toString();
                propertiesFile = new File(propertiesFilename3);
                propertiesFileExists = ss.getFileExists(propertiesFile);
            } catch (SecurityException e2) {
                fLastModified = -1;
                fXalanProperties = null;
            }
            propertiesFilename2 = propertiesFilename3;
            synchronized (ObjectFactory.class) {
                propertiesFilename3 = null;
                FileInputStream fis2 = null;
                try {
                    if (fLastModified >= 0) {
                        if (propertiesFileExists) {
                            long j = fLastModified;
                            long lastModified = ss.getLastModified(propertiesFile);
                            fLastModified = lastModified;
                            if (j < lastModified) {
                                propertiesFilename3 = true;
                            }
                        }
                        if (!propertiesFileExists) {
                            fLastModified = -1;
                            fXalanProperties = null;
                        }
                    } else if (propertiesFileExists) {
                        propertiesFilename3 = true;
                        fLastModified = ss.getLastModified(propertiesFile);
                    }
                    if (propertiesFilename3 != null) {
                        fXalanProperties = new Properties();
                        fis2 = ss.getFileInputStream(propertiesFile);
                        fXalanProperties.load(fis2);
                    }
                } catch (Exception e3) {
                    try {
                        fXalanProperties = null;
                        fLastModified = -1;
                    } catch (Throwable th) {
                        fis = null;
                        Throwable propertiesFilename4 = th;
                        if (fis2 != null) {
                            try {
                                fis2.close();
                            } catch (IOException e4) {
                            }
                        }
                    }
                }
            }
            if (fXalanProperties != null) {
                factoryClassName = fXalanProperties.getProperty(str);
            }
            propertiesFilename3 = propertiesFilename2;
        } else {
            try {
                fis = ss.getFileInputStream(new File(propertiesFilename3));
                Properties props = new Properties();
                props.load(fis);
                factoryClassName = props.getProperty(str);
            } catch (Exception e5) {
            } catch (Throwable th2) {
                FileInputStream fis3 = fis;
                Throwable fis4 = th2;
                if (fis3 != null) {
                    try {
                        fis3.close();
                    } catch (IOException e6) {
                    }
                }
            }
        }
        if (factoryClassName != null) {
            return findJarServiceProviderName(factoryId);
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("found in ");
        stringBuilder3.append(propertiesFilename3);
        stringBuilder3.append(", value=");
        stringBuilder3.append(factoryClassName);
        debugPrintln(stringBuilder3.toString());
        return factoryClassName;
        if (fXalanProperties != null) {
        }
        propertiesFilename3 = propertiesFilename2;
        if (factoryClassName != null) {
        }
    }

    private static void debugPrintln(String msg) {
    }

    static ClassLoader findClassLoader() throws ConfigurationError {
        ClassLoader chain;
        SecuritySupport ss = SecuritySupport.getInstance();
        ClassLoader context = ss.getContextClassLoader();
        ClassLoader system = ss.getSystemClassLoader();
        for (chain = system; context != chain; chain = ss.getParentClassLoader(chain)) {
            if (chain == null) {
                return context;
            }
        }
        ClassLoader current = ObjectFactory.class.getClassLoader();
        for (chain = system; current != chain; chain = ss.getParentClassLoader(chain)) {
            if (chain == null) {
                return current;
            }
        }
        return system;
    }

    static Object newInstance(String className, ClassLoader cl, boolean doFallback) throws ConfigurationError {
        StringBuilder stringBuilder;
        try {
            Class providerClass = findProviderClass(className, cl, doFallback);
            Object instance = providerClass.newInstance();
            stringBuilder = new StringBuilder();
            stringBuilder.append("created new instance of ");
            stringBuilder.append(providerClass);
            stringBuilder.append(" using ClassLoader: ");
            stringBuilder.append(cl);
            debugPrintln(stringBuilder.toString());
            return instance;
        } catch (ClassNotFoundException x) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Provider ");
            stringBuilder.append(className);
            stringBuilder.append(" not found");
            throw new ConfigurationError(stringBuilder.toString(), x);
        } catch (Exception x2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Provider ");
            stringBuilder.append(className);
            stringBuilder.append(" could not be instantiated: ");
            stringBuilder.append(x2);
            throw new ConfigurationError(stringBuilder.toString(), x2);
        }
    }

    static Class findProviderClass(String className, ClassLoader cl, boolean doFallback) throws ClassNotFoundException, ConfigurationError {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            try {
                int lastDot = className.lastIndexOf(Constants.ATTRVAL_THIS);
                String packageName = className;
                if (lastDot != -1) {
                    packageName = className.substring(0, lastDot);
                }
                security.checkPackageAccess(packageName);
            } catch (SecurityException e) {
                throw e;
            }
        }
        if (cl == null) {
            return Class.forName(className);
        }
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException x) {
            if (doFallback) {
                Class providerClass;
                ClassLoader current = ObjectFactory.class.getClassLoader();
                if (current == null) {
                    providerClass = Class.forName(className);
                } else if (cl != current) {
                    providerClass = current.loadClass(className);
                } else {
                    throw x;
                }
                return providerClass;
            }
            throw x;
        }
    }

    private static String findJarServiceProviderName(String factoryId) {
        SecuritySupport ss = SecuritySupport.getInstance();
        String serviceId = new StringBuilder();
        serviceId.append(SERVICES_PATH);
        serviceId.append(factoryId);
        serviceId = serviceId.toString();
        ClassLoader cl = findClassLoader();
        InputStream is = ss.getResourceAsStream(cl, serviceId);
        if (is == null) {
            ClassLoader current = ObjectFactory.class.getClassLoader();
            if (cl != current) {
                cl = current;
                is = ss.getResourceAsStream(cl, serviceId);
            }
        }
        if (is == null) {
            return null;
        }
        BufferedReader rd;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("found jar resource=");
        stringBuilder.append(serviceId);
        stringBuilder.append(" using ClassLoader: ");
        stringBuilder.append(cl);
        debugPrintln(stringBuilder.toString());
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is));
        }
        String factoryClassName = null;
        try {
            factoryClassName = rd.readLine();
            try {
                rd.close();
            } catch (IOException e2) {
            }
            if (factoryClassName == null || "".equals(factoryClassName)) {
                return null;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("found in resource, value=");
            stringBuilder2.append(factoryClassName);
            debugPrintln(stringBuilder2.toString());
            return factoryClassName;
        } catch (IOException e3) {
            try {
                rd.close();
            } catch (IOException e4) {
            }
            return null;
        } catch (Throwable th) {
            try {
                rd.close();
            } catch (IOException e5) {
            }
            throw th;
        }
    }
}
