package org.apache.commons.logging;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.commons.logging.impl.Jdk14Logger;

@Deprecated
public abstract class LogFactory {
    public static final String DIAGNOSTICS_DEST_PROPERTY = "org.apache.commons.logging.diagnostics.dest";
    public static final String FACTORY_DEFAULT = "org.apache.commons.logging.impl.LogFactoryImpl";
    public static final String FACTORY_PROPERTIES = "commons-logging.properties";
    public static final String FACTORY_PROPERTY = "org.apache.commons.logging.LogFactory";
    public static final String HASHTABLE_IMPLEMENTATION_PROPERTY = "org.apache.commons.logging.LogFactory.HashtableImpl";
    public static final String PRIORITY_KEY = "priority";
    protected static final String SERVICE_ID = "META-INF/services/org.apache.commons.logging.LogFactory";
    public static final String TCCL_KEY = "use_tccl";
    private static final String WEAK_HASHTABLE_CLASSNAME = "org.apache.commons.logging.impl.WeakHashtable";
    private static String diagnosticPrefix;
    private static PrintStream diagnosticsStream = null;
    protected static Hashtable factories;
    protected static LogFactory nullClassLoaderFactory = null;
    private static ClassLoader thisClassLoader = getClassLoader(LogFactory.class);

    public abstract Object getAttribute(String str);

    public abstract String[] getAttributeNames();

    public abstract Log getInstance(Class cls) throws LogConfigurationException;

    public abstract Log getInstance(String str) throws LogConfigurationException;

    public abstract void release();

    public abstract void removeAttribute(String str);

    public abstract void setAttribute(String str, Object obj);

    static {
        factories = null;
        initDiagnostics();
        logClassLoaderEnvironment(LogFactory.class);
        factories = createFactoryStore();
        if (isDiagnosticsEnabled()) {
            logDiagnostic("BOOTSTRAP COMPLETED");
        }
    }

    protected LogFactory() {
    }

    private static final Hashtable createFactoryStore() {
        Hashtable result = null;
        String storeImplementationClass = System.getProperty(HASHTABLE_IMPLEMENTATION_PROPERTY);
        if (storeImplementationClass == null) {
            storeImplementationClass = WEAK_HASHTABLE_CLASSNAME;
        }
        try {
            result = (Hashtable) Class.forName(storeImplementationClass).newInstance();
        } catch (Throwable th) {
            if (!WEAK_HASHTABLE_CLASSNAME.equals(storeImplementationClass)) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[ERROR] LogFactory: Load of custom hashtable failed");
                } else {
                    System.err.println("[ERROR] LogFactory: Load of custom hashtable failed");
                }
            }
        }
        if (result == null) {
            return new Hashtable();
        }
        return result;
    }

    public static LogFactory getFactory() throws LogConfigurationException {
        ClassLoader contextClassLoader = getContextClassLoader();
        if (contextClassLoader == null && isDiagnosticsEnabled()) {
            logDiagnostic("Context classloader is null.");
        }
        LogFactory factory = getCachedFactory(contextClassLoader);
        if (factory != null) {
            return factory;
        }
        String useTCCLStr;
        StringBuilder stringBuilder;
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[LOOKUP] LogFactory implementation requested for the first time for context classloader ");
            stringBuilder2.append(objectId(contextClassLoader));
            logDiagnostic(stringBuilder2.toString());
            logHierarchy("[LOOKUP] ", contextClassLoader);
        }
        Properties props = getConfigurationFile(contextClassLoader, FACTORY_PROPERTIES);
        ClassLoader baseClassLoader = contextClassLoader;
        if (props != null) {
            useTCCLStr = props.getProperty(TCCL_KEY);
            if (!(useTCCLStr == null || Boolean.valueOf(useTCCLStr).booleanValue())) {
                baseClassLoader = thisClassLoader;
            }
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("[LOOKUP] Looking for system property [org.apache.commons.logging.LogFactory] to define the LogFactory subclass to use...");
        }
        try {
            useTCCLStr = System.getProperty(FACTORY_PROPERTY);
            if (useTCCLStr != null) {
                if (isDiagnosticsEnabled()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[LOOKUP] Creating an instance of LogFactory class '");
                    stringBuilder.append(useTCCLStr);
                    stringBuilder.append("' as specified by system property ");
                    stringBuilder.append(FACTORY_PROPERTY);
                    logDiagnostic(stringBuilder.toString());
                }
                factory = newFactory(useTCCLStr, baseClassLoader, contextClassLoader);
            } else if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] No system property [org.apache.commons.logging.LogFactory] defined.");
            }
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[LOOKUP] A security exception occurred while trying to create an instance of the custom factory class: [");
                stringBuilder.append(e.getMessage().trim());
                stringBuilder.append("]. Trying alternative implementations...");
                logDiagnostic(stringBuilder.toString());
            }
        } catch (RuntimeException e2) {
            if (isDiagnosticsEnabled()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[LOOKUP] An exception occurred while trying to create an instance of the custom factory class: [");
                stringBuilder.append(e2.getMessage().trim());
                stringBuilder.append("] as specified by a system property.");
                logDiagnostic(stringBuilder.toString());
            }
            throw e2;
        }
        if (factory == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] Looking for a resource file of name [META-INF/services/org.apache.commons.logging.LogFactory] to define the LogFactory subclass to use...");
            }
            try {
                InputStream is = getResourceAsStream(contextClassLoader, SERVICE_ID);
                if (is != null) {
                    BufferedReader rd;
                    try {
                        rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    } catch (UnsupportedEncodingException e3) {
                        rd = new BufferedReader(new InputStreamReader(is));
                    }
                    String factoryClassName = rd.readLine();
                    rd.close();
                    if (!(factoryClassName == null || "".equals(factoryClassName))) {
                        if (isDiagnosticsEnabled()) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("[LOOKUP]  Creating an instance of LogFactory class ");
                            stringBuilder3.append(factoryClassName);
                            stringBuilder3.append(" as specified by file '");
                            stringBuilder3.append(SERVICE_ID);
                            stringBuilder3.append("' which was present in the path of the context classloader.");
                            logDiagnostic(stringBuilder3.toString());
                        }
                        factory = newFactory(factoryClassName, baseClassLoader, contextClassLoader);
                    }
                } else if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] No resource file with name 'META-INF/services/org.apache.commons.logging.LogFactory' found.");
                }
            } catch (Exception ex) {
                if (isDiagnosticsEnabled()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[LOOKUP] A security exception occurred while trying to create an instance of the custom factory class: [");
                    stringBuilder.append(ex.getMessage().trim());
                    stringBuilder.append("]. Trying alternative implementations...");
                    logDiagnostic(stringBuilder.toString());
                }
            }
        }
        if (factory == null) {
            if (props != null) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] Looking in properties file for entry with key 'org.apache.commons.logging.LogFactory' to define the LogFactory subclass to use...");
                }
                useTCCLStr = props.getProperty(FACTORY_PROPERTY);
                if (useTCCLStr != null) {
                    if (isDiagnosticsEnabled()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("[LOOKUP] Properties file specifies LogFactory subclass '");
                        stringBuilder.append(useTCCLStr);
                        stringBuilder.append("'");
                        logDiagnostic(stringBuilder.toString());
                    }
                    factory = newFactory(useTCCLStr, baseClassLoader, contextClassLoader);
                } else if (isDiagnosticsEnabled()) {
                    logDiagnostic("[LOOKUP] Properties file has no entry specifying LogFactory subclass.");
                }
            } else if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] No properties file available to determine LogFactory subclass from..");
            }
        }
        if (factory == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[LOOKUP] Loading the default LogFactory implementation 'org.apache.commons.logging.impl.LogFactoryImpl' via the same classloader that loaded this LogFactory class (ie not looking in the context classloader).");
            }
            factory = newFactory(FACTORY_DEFAULT, thisClassLoader, contextClassLoader);
        }
        if (factory != null) {
            cacheFactory(contextClassLoader, factory);
            if (props != null) {
                Enumeration names = props.propertyNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    factory.setAttribute(name, props.getProperty(name));
                }
            }
        }
        return factory;
    }

    public static Log getLog(Class clazz) throws LogConfigurationException {
        return getLog(clazz.getName());
    }

    public static Log getLog(String name) throws LogConfigurationException {
        return new Jdk14Logger(name);
    }

    public static void release(ClassLoader classLoader) {
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Releasing factory for classloader ");
            stringBuilder.append(objectId(classLoader));
            logDiagnostic(stringBuilder.toString());
        }
        synchronized (factories) {
            if (classLoader == null) {
                try {
                    if (nullClassLoaderFactory != null) {
                        nullClassLoaderFactory.release();
                        nullClassLoaderFactory = null;
                    }
                } finally {
                }
            } else {
                LogFactory factory = (LogFactory) factories.get(classLoader);
                if (factory != null) {
                    factory.release();
                    factories.remove(classLoader);
                }
            }
        }
    }

    public static void releaseAll() {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Releasing factory for all classloaders.");
        }
        synchronized (factories) {
            Enumeration elements = factories.elements();
            while (elements.hasMoreElements()) {
                ((LogFactory) elements.nextElement()).release();
            }
            factories.clear();
            if (nullClassLoaderFactory != null) {
                nullClassLoaderFactory.release();
                nullClassLoaderFactory = null;
            }
        }
    }

    protected static ClassLoader getClassLoader(Class clazz) {
        try {
            return clazz.getClassLoader();
        } catch (SecurityException ex) {
            if (isDiagnosticsEnabled()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to get classloader for class '");
                stringBuilder.append(clazz);
                stringBuilder.append("' due to security restrictions - ");
                stringBuilder.append(ex.getMessage());
                logDiagnostic(stringBuilder.toString());
            }
            throw ex;
        }
    }

    protected static ClassLoader getContextClassLoader() throws LogConfigurationException {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return LogFactory.directGetContextClassLoader();
            }
        });
    }

    protected static ClassLoader directGetContextClassLoader() throws LogConfigurationException {
        ClassLoader classLoader = null;
        try {
            classLoader = (ClassLoader) Thread.class.getMethod("getContextClassLoader", (Class[]) null).invoke(Thread.currentThread(), (Object[]) null);
        } catch (IllegalAccessException e) {
            throw new LogConfigurationException("Unexpected IllegalAccessException", e);
        } catch (InvocationTargetException e2) {
            if (!(e2.getTargetException() instanceof SecurityException)) {
                throw new LogConfigurationException("Unexpected InvocationTargetException", e2.getTargetException());
            }
        } catch (NoSuchMethodException e3) {
            return getClassLoader(LogFactory.class);
        }
        return classLoader;
    }

    private static LogFactory getCachedFactory(ClassLoader contextClassLoader) {
        if (contextClassLoader == null) {
            return nullClassLoaderFactory;
        }
        return (LogFactory) factories.get(contextClassLoader);
    }

    private static void cacheFactory(ClassLoader classLoader, LogFactory factory) {
        if (factory == null) {
            return;
        }
        if (classLoader == null) {
            nullClassLoaderFactory = factory;
        } else {
            factories.put(classLoader, factory);
        }
    }

    protected static LogFactory newFactory(final String factoryClass, final ClassLoader classLoader, ClassLoader contextClassLoader) throws LogConfigurationException {
        LogConfigurationException result = AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return LogFactory.createFactory(factoryClass, classLoader);
            }
        });
        if (result instanceof LogConfigurationException) {
            LogConfigurationException ex = result;
            if (isDiagnosticsEnabled()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("An error occurred while loading the factory class:");
                stringBuilder.append(ex.getMessage());
                logDiagnostic(stringBuilder.toString());
            }
            throw ex;
        }
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Created object ");
            stringBuilder2.append(objectId(result));
            stringBuilder2.append(" to manage classloader ");
            stringBuilder2.append(objectId(contextClassLoader));
            logDiagnostic(stringBuilder2.toString());
        }
        return (LogFactory) result;
    }

    protected static LogFactory newFactory(String factoryClass, ClassLoader classLoader) {
        return newFactory(factoryClass, classLoader, null);
    }

    protected static Object createFactory(String factoryClass, ClassLoader classLoader) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (classLoader != null) {
            try {
                Class logFactoryClass = classLoader.loadClass(factoryClass);
                if (LogFactory.class.isAssignableFrom(logFactoryClass)) {
                    if (isDiagnosticsEnabled()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Loaded class ");
                        stringBuilder.append(logFactoryClass.getName());
                        stringBuilder.append(" from classloader ");
                        stringBuilder.append(objectId(classLoader));
                        logDiagnostic(stringBuilder.toString());
                    }
                } else if (isDiagnosticsEnabled()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Factory class ");
                    stringBuilder.append(logFactoryClass.getName());
                    stringBuilder.append(" loaded from classloader ");
                    stringBuilder.append(objectId(logFactoryClass.getClassLoader()));
                    stringBuilder.append(" does not extend '");
                    stringBuilder.append(LogFactory.class.getName());
                    stringBuilder.append("' as loaded by this classloader.");
                    logDiagnostic(stringBuilder.toString());
                    logHierarchy("[BAD CL TREE] ", classLoader);
                }
                return (LogFactory) logFactoryClass.newInstance();
            } catch (ClassNotFoundException ex) {
                if (classLoader == thisClassLoader) {
                    if (isDiagnosticsEnabled()) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unable to locate any class called '");
                        stringBuilder2.append(factoryClass);
                        stringBuilder2.append("' via classloader ");
                        stringBuilder2.append(objectId(classLoader));
                        logDiagnostic(stringBuilder2.toString());
                    }
                    throw ex;
                }
            } catch (NoClassDefFoundError e) {
                if (classLoader == thisClassLoader) {
                    if (isDiagnosticsEnabled()) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Class '");
                        stringBuilder2.append(factoryClass);
                        stringBuilder2.append("' cannot be loaded via classloader ");
                        stringBuilder2.append(objectId(classLoader));
                        stringBuilder2.append(" - it depends on some other class that cannot be found.");
                        logDiagnostic(stringBuilder2.toString());
                    }
                    throw e;
                }
            } catch (ClassCastException e2) {
                if (classLoader == thisClassLoader) {
                    StringBuilder stringBuilder3;
                    boolean implementsLogFactory = implementsLogFactory(null);
                    String msg = new StringBuilder();
                    msg.append("The application has specified that a custom LogFactory implementation should be used but Class '");
                    msg.append(factoryClass);
                    msg.append("' cannot be converted to '");
                    msg.append(LogFactory.class.getName());
                    msg.append("'. ");
                    msg = msg.toString();
                    if (implementsLogFactory) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(msg);
                        stringBuilder3.append("The conflict is caused by the presence of multiple LogFactory classes in incompatible classloaders. Background can be found in http://jakarta.apache.org/commons/logging/tech.html. If you have not explicitly specified a custom LogFactory then it is likely that the container has set one without your knowledge. In this case, consider using the commons-logging-adapters.jar file or specifying the standard LogFactory from the command line. ");
                        msg = stringBuilder3.toString();
                    } else {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(msg);
                        stringBuilder3.append("Please check the custom implementation. ");
                        msg = stringBuilder3.toString();
                    }
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(msg);
                    stringBuilder3.append("Help can be found @http://jakarta.apache.org/commons/logging/troubleshooting.html.");
                    msg = stringBuilder3.toString();
                    if (isDiagnosticsEnabled()) {
                        logDiagnostic(msg);
                    }
                    throw new ClassCastException(msg);
                }
            } catch (Exception e3) {
                if (isDiagnosticsEnabled()) {
                    logDiagnostic("Unable to create LogFactory instance.");
                }
                if (null == null || LogFactory.class.isAssignableFrom(null)) {
                    return new LogConfigurationException(e3);
                }
                return new LogConfigurationException("The chosen LogFactory implementation does not extend LogFactory. Please check your configuration.", e3);
            }
        }
        if (isDiagnosticsEnabled()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to load factory class via classloader ");
            stringBuilder.append(objectId(classLoader));
            stringBuilder.append(" - trying the classloader associated with this LogFactory.");
            logDiagnostic(stringBuilder.toString());
        }
        return (LogFactory) Class.forName(factoryClass).newInstance();
    }

    private static boolean implementsLogFactory(Class logFactoryClass) {
        StringBuilder stringBuilder;
        boolean implementsLogFactory = false;
        if (logFactoryClass != null) {
            try {
                ClassLoader logFactoryClassLoader = logFactoryClass.getClassLoader();
                if (logFactoryClassLoader == null) {
                    logDiagnostic("[CUSTOM LOG FACTORY] was loaded by the boot classloader");
                } else {
                    logHierarchy("[CUSTOM LOG FACTORY] ", logFactoryClassLoader);
                    implementsLogFactory = Class.forName(FACTORY_PROPERTY, false, logFactoryClassLoader).isAssignableFrom(logFactoryClass);
                    StringBuilder stringBuilder2;
                    if (implementsLogFactory) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[CUSTOM LOG FACTORY] ");
                        stringBuilder2.append(logFactoryClass.getName());
                        stringBuilder2.append(" implements LogFactory but was loaded by an incompatible classloader.");
                        logDiagnostic(stringBuilder2.toString());
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("[CUSTOM LOG FACTORY] ");
                        stringBuilder2.append(logFactoryClass.getName());
                        stringBuilder2.append(" does not implement LogFactory.");
                        logDiagnostic(stringBuilder2.toString());
                    }
                }
            } catch (SecurityException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[CUSTOM LOG FACTORY] SecurityException thrown whilst trying to determine whether the compatibility was caused by a classloader conflict: ");
                stringBuilder.append(e.getMessage());
                logDiagnostic(stringBuilder.toString());
            } catch (LinkageError e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[CUSTOM LOG FACTORY] LinkageError thrown whilst trying to determine whether the compatibility was caused by a classloader conflict: ");
                stringBuilder.append(e2.getMessage());
                logDiagnostic(stringBuilder.toString());
            } catch (ClassNotFoundException e3) {
                logDiagnostic("[CUSTOM LOG FACTORY] LogFactory class cannot be loaded by classloader which loaded the custom LogFactory implementation. Is the custom factory in the right classloader?");
            }
        }
        return implementsLogFactory;
    }

    private static InputStream getResourceAsStream(final ClassLoader loader, final String name) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (loader != null) {
                    return loader.getResourceAsStream(name);
                }
                return ClassLoader.getSystemResourceAsStream(name);
            }
        });
    }

    private static Enumeration getResources(final ClassLoader loader, final String name) {
        return (Enumeration) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    if (loader != null) {
                        return loader.getResources(name);
                    }
                    return ClassLoader.getSystemResources(name);
                } catch (IOException e) {
                    if (LogFactory.isDiagnosticsEnabled()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Exception while trying to find configuration file ");
                        stringBuilder.append(name);
                        stringBuilder.append(":");
                        stringBuilder.append(e.getMessage());
                        LogFactory.logDiagnostic(stringBuilder.toString());
                    }
                    return null;
                } catch (NoSuchMethodError e2) {
                    return null;
                }
            }
        });
    }

    private static Properties getProperties(final URL url) {
        return (Properties) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    InputStream stream = url.openStream();
                    if (stream != null) {
                        Properties props = new Properties();
                        props.load(stream);
                        stream.close();
                        return props;
                    }
                } catch (IOException e) {
                    if (LogFactory.isDiagnosticsEnabled()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to read URL ");
                        stringBuilder.append(url);
                        LogFactory.logDiagnostic(stringBuilder.toString());
                    }
                }
                return null;
            }
        });
    }

    private static final Properties getConfigurationFile(ClassLoader classLoader, String fileName) {
        Properties props = null;
        double priority = 0.0d;
        URL propsUrl = null;
        try {
            Enumeration urls = getResources(classLoader, fileName);
            if (urls == null) {
                return null;
            }
            while (urls.hasMoreElements()) {
                URL url = (URL) urls.nextElement();
                Properties newProps = getProperties(url);
                if (newProps != null) {
                    String priorityStr;
                    if (props == null) {
                        propsUrl = url;
                        props = newProps;
                        priorityStr = props.getProperty(PRIORITY_KEY);
                        priority = 0.0d;
                        if (priorityStr != null) {
                            priority = Double.parseDouble(priorityStr);
                        }
                        if (isDiagnosticsEnabled()) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("[LOOKUP] Properties file found at '");
                            stringBuilder.append(url);
                            stringBuilder.append("' with priority ");
                            stringBuilder.append(priority);
                            logDiagnostic(stringBuilder.toString());
                        }
                    } else {
                        priorityStr = newProps.getProperty(PRIORITY_KEY);
                        double newPriority = 0.0d;
                        if (priorityStr != null) {
                            newPriority = Double.parseDouble(priorityStr);
                        }
                        StringBuilder stringBuilder2;
                        if (newPriority > priority) {
                            if (isDiagnosticsEnabled()) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[LOOKUP] Properties file at '");
                                stringBuilder2.append(url);
                                stringBuilder2.append("' with priority ");
                                stringBuilder2.append(newPriority);
                                stringBuilder2.append(" overrides file at '");
                                stringBuilder2.append(propsUrl);
                                stringBuilder2.append("' with priority ");
                                stringBuilder2.append(priority);
                                logDiagnostic(stringBuilder2.toString());
                            }
                            propsUrl = url;
                            props = newProps;
                            priority = newPriority;
                        } else if (isDiagnosticsEnabled()) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("[LOOKUP] Properties file at '");
                            stringBuilder2.append(url);
                            stringBuilder2.append("' with priority ");
                            stringBuilder2.append(newPriority);
                            stringBuilder2.append(" does not override file at '");
                            stringBuilder2.append(propsUrl);
                            stringBuilder2.append("' with priority ");
                            stringBuilder2.append(priority);
                            logDiagnostic(stringBuilder2.toString());
                        }
                    }
                }
            }
            if (isDiagnosticsEnabled()) {
                StringBuilder stringBuilder3;
                if (props == null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("[LOOKUP] No properties file of name '");
                    stringBuilder3.append(fileName);
                    stringBuilder3.append("' found.");
                    logDiagnostic(stringBuilder3.toString());
                } else {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("[LOOKUP] Properties file of name '");
                    stringBuilder3.append(fileName);
                    stringBuilder3.append("' found at '");
                    stringBuilder3.append(propsUrl);
                    stringBuilder3.append('\"');
                    logDiagnostic(stringBuilder3.toString());
                }
            }
            return props;
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("SecurityException thrown while trying to find/read config files.");
            }
        }
    }

    private static void initDiagnostics() {
        try {
            String dest = System.getProperty(DIAGNOSTICS_DEST_PROPERTY);
            if (dest != null) {
                String classLoaderName;
                if (dest.equals("STDOUT")) {
                    diagnosticsStream = System.out;
                } else if (dest.equals("STDERR")) {
                    diagnosticsStream = System.err;
                } else {
                    try {
                        diagnosticsStream = new PrintStream(new FileOutputStream(dest, true));
                    } catch (IOException e) {
                        return;
                    }
                }
                try {
                    String classLoaderName2;
                    ClassLoader classLoader = thisClassLoader;
                    if (thisClassLoader == null) {
                        classLoaderName2 = "BOOTLOADER";
                    } else {
                        classLoaderName2 = objectId(classLoader);
                    }
                    classLoaderName = classLoaderName2;
                } catch (SecurityException e2) {
                    classLoaderName = "UNKNOWN";
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[LogFactory from ");
                stringBuilder.append(classLoaderName);
                stringBuilder.append("] ");
                diagnosticPrefix = stringBuilder.toString();
            }
        } catch (SecurityException e3) {
        }
    }

    protected static boolean isDiagnosticsEnabled() {
        return diagnosticsStream != null;
    }

    private static final void logDiagnostic(String msg) {
        if (diagnosticsStream != null) {
            diagnosticsStream.print(diagnosticPrefix);
            diagnosticsStream.println(msg);
            diagnosticsStream.flush();
        }
    }

    protected static final void logRawDiagnostic(String msg) {
        if (diagnosticsStream != null) {
            diagnosticsStream.println(msg);
            diagnosticsStream.flush();
        }
    }

    private static void logClassLoaderEnvironment(Class clazz) {
        if (isDiagnosticsEnabled()) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[ENV] Extension directories (java.ext.dir): ");
                stringBuilder.append(System.getProperty("java.ext.dir"));
                logDiagnostic(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("[ENV] Application classpath (java.class.path): ");
                stringBuilder.append(System.getProperty("java.class.path"));
                logDiagnostic(stringBuilder.toString());
            } catch (SecurityException e) {
                logDiagnostic("[ENV] Security setting prevent interrogation of system classpaths.");
            }
            String className = clazz.getName();
            StringBuilder stringBuilder2;
            try {
                ClassLoader classLoader = getClassLoader(clazz);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[ENV] Class ");
                stringBuilder2.append(className);
                stringBuilder2.append(" was loaded via classloader ");
                stringBuilder2.append(objectId(classLoader));
                logDiagnostic(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[ENV] Ancestry of classloader which loaded ");
                stringBuilder2.append(className);
                stringBuilder2.append(" is ");
                logHierarchy(stringBuilder2.toString(), classLoader);
            } catch (SecurityException e2) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[ENV] Security forbids determining the classloader for ");
                stringBuilder2.append(className);
                logDiagnostic(stringBuilder2.toString());
            }
        }
    }

    private static void logHierarchy(String prefix, ClassLoader classLoader) {
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder;
            if (classLoader != null) {
                String classLoaderString = classLoader.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append(objectId(classLoader));
                stringBuilder.append(" == '");
                stringBuilder.append(classLoaderString);
                stringBuilder.append("'");
                logDiagnostic(stringBuilder.toString());
            }
            try {
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                if (classLoader != null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(prefix);
                    stringBuilder2.append("ClassLoader tree:");
                    StringBuffer buf = new StringBuffer(stringBuilder2.toString());
                    do {
                        buf.append(objectId(classLoader));
                        if (classLoader == systemClassLoader) {
                            buf.append(" (SYSTEM) ");
                        }
                        try {
                            classLoader = classLoader.getParent();
                            buf.append(" --> ");
                        } catch (SecurityException e) {
                            buf.append(" --> SECRET");
                        }
                    } while (classLoader != null);
                    buf.append("BOOT");
                    logDiagnostic(buf.toString());
                }
            } catch (SecurityException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("Security forbids determining the system classloader.");
                logDiagnostic(stringBuilder.toString());
            }
        }
    }

    public static String objectId(Object o) {
        if (o == null) {
            return "null";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(o.getClass().getName());
        stringBuilder.append("@");
        stringBuilder.append(System.identityHashCode(o));
        return stringBuilder.toString();
    }
}
