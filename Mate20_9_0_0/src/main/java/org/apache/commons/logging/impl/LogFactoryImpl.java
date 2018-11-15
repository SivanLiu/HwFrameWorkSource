package org.apache.commons.logging.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class LogFactoryImpl extends LogFactory {
    public static final String ALLOW_FLAWED_CONTEXT_PROPERTY = "org.apache.commons.logging.Log.allowFlawedContext";
    public static final String ALLOW_FLAWED_DISCOVERY_PROPERTY = "org.apache.commons.logging.Log.allowFlawedDiscovery";
    public static final String ALLOW_FLAWED_HIERARCHY_PROPERTY = "org.apache.commons.logging.Log.allowFlawedHierarchy";
    private static final String LOGGING_IMPL_JDK14_LOGGER = "org.apache.commons.logging.impl.Jdk14Logger";
    private static final String LOGGING_IMPL_LOG4J_LOGGER = "org.apache.commons.logging.impl.Log4JLogger";
    private static final String LOGGING_IMPL_LUMBERJACK_LOGGER = "org.apache.commons.logging.impl.Jdk13LumberjackLogger";
    private static final String LOGGING_IMPL_SIMPLE_LOGGER = "org.apache.commons.logging.impl.SimpleLog";
    public static final String LOG_PROPERTY = "org.apache.commons.logging.Log";
    protected static final String LOG_PROPERTY_OLD = "org.apache.commons.logging.log";
    private static final String PKG_IMPL = "org.apache.commons.logging.impl.";
    private static final int PKG_LEN = PKG_IMPL.length();
    private static final String[] classesToDiscover = new String[]{LOGGING_IMPL_LOG4J_LOGGER, LOGGING_IMPL_JDK14_LOGGER, LOGGING_IMPL_LUMBERJACK_LOGGER, LOGGING_IMPL_SIMPLE_LOGGER};
    private boolean allowFlawedContext;
    private boolean allowFlawedDiscovery;
    private boolean allowFlawedHierarchy;
    protected Hashtable attributes = new Hashtable();
    private String diagnosticPrefix;
    protected Hashtable instances = new Hashtable();
    private String logClassName;
    protected Constructor logConstructor = null;
    protected Class[] logConstructorSignature = new Class[]{String.class};
    protected Method logMethod = null;
    protected Class[] logMethodSignature = new Class[]{LogFactory.class};
    private boolean useTCCL = true;

    public LogFactoryImpl() {
        initDiagnostics();
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Instance created.");
        }
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public String[] getAttributeNames() {
        Vector names = new Vector();
        Enumeration keys = this.attributes.keys();
        while (keys.hasMoreElements()) {
            names.addElement((String) keys.nextElement());
        }
        String[] results = new String[names.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = (String) names.elementAt(i);
        }
        return results;
    }

    public Log getInstance(Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    public Log getInstance(String name) throws LogConfigurationException {
        Log instance = (Log) this.instances.get(name);
        if (instance != null) {
            return instance;
        }
        instance = newInstance(name);
        this.instances.put(name, instance);
        return instance;
    }

    public void release() {
        logDiagnostic("Releasing all known loggers");
        this.instances.clear();
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    public void setAttribute(String name, Object value) {
        if (this.logConstructor != null) {
            logDiagnostic("setAttribute: call too late; configuration already performed.");
        }
        if (value == null) {
            this.attributes.remove(name);
        } else {
            this.attributes.put(name, value);
        }
        if (name.equals(LogFactory.TCCL_KEY)) {
            this.useTCCL = Boolean.valueOf(value.toString()).booleanValue();
        }
    }

    protected static ClassLoader getContextClassLoader() throws LogConfigurationException {
        return LogFactory.getContextClassLoader();
    }

    protected static boolean isDiagnosticsEnabled() {
        return LogFactory.isDiagnosticsEnabled();
    }

    protected static ClassLoader getClassLoader(Class clazz) {
        return LogFactory.getClassLoader(clazz);
    }

    private void initDiagnostics() {
        String classLoaderName;
        ClassLoader classLoader = getClassLoader(getClass());
        if (classLoader == null) {
            try {
                classLoaderName = "BOOTLOADER";
            } catch (SecurityException e) {
                classLoaderName = "UNKNOWN";
            }
        } else {
            classLoaderName = LogFactory.objectId(classLoader);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[LogFactoryImpl@");
        stringBuilder.append(System.identityHashCode(this));
        stringBuilder.append(" from ");
        stringBuilder.append(classLoaderName);
        stringBuilder.append("] ");
        this.diagnosticPrefix = stringBuilder.toString();
    }

    protected void logDiagnostic(String msg) {
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.diagnosticPrefix);
            stringBuilder.append(msg);
            LogFactory.logRawDiagnostic(stringBuilder.toString());
        }
    }

    protected String getLogClassName() {
        if (this.logClassName == null) {
            discoverLogImplementation(getClass().getName());
        }
        return this.logClassName;
    }

    protected Constructor getLogConstructor() throws LogConfigurationException {
        if (this.logConstructor == null) {
            discoverLogImplementation(getClass().getName());
        }
        return this.logConstructor;
    }

    protected boolean isJdk13LumberjackAvailable() {
        return isLogLibraryAvailable("Jdk13Lumberjack", LOGGING_IMPL_LUMBERJACK_LOGGER);
    }

    protected boolean isJdk14Available() {
        return isLogLibraryAvailable("Jdk14", LOGGING_IMPL_JDK14_LOGGER);
    }

    protected boolean isLog4JAvailable() {
        return isLogLibraryAvailable("Log4J", LOGGING_IMPL_LOG4J_LOGGER);
    }

    protected Log newInstance(String name) throws LogConfigurationException {
        try {
            Log instance;
            if (this.logConstructor == null) {
                instance = discoverLogImplementation(name);
            } else {
                instance = (Log) this.logConstructor.newInstance(new Object[]{name});
            }
            if (this.logMethod != null) {
                this.logMethod.invoke(instance, new Object[]{this});
            }
            return instance;
        } catch (LogConfigurationException lce) {
            throw lce;
        } catch (Throwable e) {
            Throwable c = e.getTargetException();
            if (c != null) {
                throw new LogConfigurationException(c);
            }
            throw new LogConfigurationException(e);
        } catch (Throwable e2) {
            LogConfigurationException logConfigurationException = new LogConfigurationException(e2);
        }
    }

    private boolean isLogLibraryAvailable(String name, String classname) {
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Checking for '");
            stringBuilder.append(name);
            stringBuilder.append("'.");
            logDiagnostic(stringBuilder.toString());
        }
        StringBuilder stringBuilder2;
        try {
            if (createLogFromClass(classname, getClass().getName(), false) == null) {
                if (isDiagnosticsEnabled()) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Did not find '");
                    stringBuilder2.append(name);
                    stringBuilder2.append("'.");
                    logDiagnostic(stringBuilder2.toString());
                }
                return false;
            }
            if (isDiagnosticsEnabled()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Found '");
                stringBuilder2.append(name);
                stringBuilder2.append("'.");
                logDiagnostic(stringBuilder2.toString());
            }
            return true;
        } catch (LogConfigurationException e) {
            if (isDiagnosticsEnabled()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Logging system '");
                stringBuilder2.append(name);
                stringBuilder2.append("' is available but not useable.");
                logDiagnostic(stringBuilder2.toString());
            }
            return false;
        }
    }

    private String getConfigurationValue(String property) {
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ENV] Trying to get configuration for item ");
            stringBuilder.append(property);
            logDiagnostic(stringBuilder.toString());
        }
        Object valueObj = getAttribute(property);
        StringBuilder stringBuilder2;
        if (valueObj != null) {
            if (isDiagnosticsEnabled()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[ENV] Found LogFactory attribute [");
                stringBuilder2.append(valueObj);
                stringBuilder2.append("] for ");
                stringBuilder2.append(property);
                logDiagnostic(stringBuilder2.toString());
            }
            return valueObj.toString();
        }
        if (isDiagnosticsEnabled()) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[ENV] No LogFactory attribute found for ");
            stringBuilder2.append(property);
            logDiagnostic(stringBuilder2.toString());
        }
        StringBuilder stringBuilder3;
        try {
            String value = System.getProperty(property);
            if (value != null) {
                if (isDiagnosticsEnabled()) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("[ENV] Found system property [");
                    stringBuilder3.append(value);
                    stringBuilder3.append("] for ");
                    stringBuilder3.append(property);
                    logDiagnostic(stringBuilder3.toString());
                }
                return value;
            }
            if (isDiagnosticsEnabled()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[ENV] No system property found for property ");
                stringBuilder3.append(property);
                logDiagnostic(stringBuilder3.toString());
            }
            if (isDiagnosticsEnabled()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[ENV] No configuration defined for item ");
                stringBuilder2.append(property);
                logDiagnostic(stringBuilder2.toString());
            }
            return null;
        } catch (SecurityException e) {
            if (isDiagnosticsEnabled()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[ENV] Security prevented reading system property ");
                stringBuilder3.append(property);
                logDiagnostic(stringBuilder3.toString());
            }
        }
    }

    private boolean getBooleanConfiguration(String key, boolean dflt) {
        String val = getConfigurationValue(key);
        if (val == null) {
            return dflt;
        }
        return Boolean.valueOf(val).booleanValue();
    }

    private void initConfiguration() {
        this.allowFlawedContext = getBooleanConfiguration(ALLOW_FLAWED_CONTEXT_PROPERTY, true);
        this.allowFlawedDiscovery = getBooleanConfiguration(ALLOW_FLAWED_DISCOVERY_PROPERTY, true);
        this.allowFlawedHierarchy = getBooleanConfiguration(ALLOW_FLAWED_HIERARCHY_PROPERTY, true);
    }

    private Log discoverLogImplementation(String logCategory) throws LogConfigurationException {
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Discovering a Log implementation...");
        }
        initConfiguration();
        Log result = null;
        String specifiedLogClassName = findUserSpecifiedLogClassName();
        if (specifiedLogClassName != null) {
            if (isDiagnosticsEnabled()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Attempting to load user-specified log class '");
                stringBuilder.append(specifiedLogClassName);
                stringBuilder.append("'...");
                logDiagnostic(stringBuilder.toString());
            }
            result = createLogFromClass(specifiedLogClassName, logCategory, true);
            if (result != null) {
                return result;
            }
            StringBuffer messageBuffer = new StringBuffer("User-specified log class '");
            messageBuffer.append(specifiedLogClassName);
            messageBuffer.append("' cannot be found or is not useable.");
            if (specifiedLogClassName != null) {
                informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_LOG4J_LOGGER);
                informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_JDK14_LOGGER);
                informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_LUMBERJACK_LOGGER);
                informUponSimilarName(messageBuffer, specifiedLogClassName, LOGGING_IMPL_SIMPLE_LOGGER);
            }
            throw new LogConfigurationException(messageBuffer.toString());
        }
        if (isDiagnosticsEnabled()) {
            logDiagnostic("No user-specified Log implementation; performing discovery using the standard supported logging implementations...");
        }
        for (int i = 0; i < classesToDiscover.length && result == null; i++) {
            result = createLogFromClass(classesToDiscover[i], logCategory, true);
        }
        if (result != null) {
            return result;
        }
        throw new LogConfigurationException("No suitable Log implementation");
    }

    private void informUponSimilarName(StringBuffer messageBuffer, String name, String candidate) {
        if (!name.equals(candidate)) {
            if (name.regionMatches(true, 0, candidate, 0, PKG_LEN + 5)) {
                messageBuffer.append(" Did you mean '");
                messageBuffer.append(candidate);
                messageBuffer.append("'?");
            }
        }
    }

    private String findUserSpecifiedLogClassName() {
        StringBuilder stringBuilder;
        if (isDiagnosticsEnabled()) {
            logDiagnostic("Trying to get log class from attribute 'org.apache.commons.logging.Log'");
        }
        String specifiedClass = (String) getAttribute(LOG_PROPERTY);
        if (specifiedClass == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from attribute 'org.apache.commons.logging.log'");
            }
            specifiedClass = (String) getAttribute(LOG_PROPERTY_OLD);
        }
        if (specifiedClass == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from system property 'org.apache.commons.logging.Log'");
            }
            try {
                specifiedClass = System.getProperty(LOG_PROPERTY);
            } catch (SecurityException e) {
                if (isDiagnosticsEnabled()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No access allowed to system property 'org.apache.commons.logging.Log' - ");
                    stringBuilder.append(e.getMessage());
                    logDiagnostic(stringBuilder.toString());
                }
            }
        }
        if (specifiedClass == null) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("Trying to get log class from system property 'org.apache.commons.logging.log'");
            }
            try {
                specifiedClass = System.getProperty(LOG_PROPERTY_OLD);
            } catch (SecurityException e2) {
                if (isDiagnosticsEnabled()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No access allowed to system property 'org.apache.commons.logging.log' - ");
                    stringBuilder.append(e2.getMessage());
                    logDiagnostic(stringBuilder.toString());
                }
            }
        }
        if (specifiedClass != null) {
            return specifiedClass.trim();
        }
        return specifiedClass;
    }

    private Log createLogFromClass(String logAdapterClassName, String logCategory, boolean affectState) throws LogConfigurationException {
        StringBuilder stringBuilder;
        String msg;
        String str = logAdapterClassName;
        if (isDiagnosticsEnabled()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Attempting to instantiate '");
            stringBuilder.append(str);
            stringBuilder.append("'");
            logDiagnostic(stringBuilder.toString());
        }
        Object[] params = new Object[]{logCategory};
        Log logAdapter = null;
        ClassLoader currentCL = getBaseClassLoader();
        Class logAdapterClass = null;
        Constructor constructor = null;
        while (true) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Trying to load '");
            stringBuilder.append(str);
            stringBuilder.append("' from classloader ");
            stringBuilder.append(LogFactory.objectId(currentCL));
            logDiagnostic(stringBuilder.toString());
            StringBuilder stringBuilder2;
            try {
                Class c;
                if (isDiagnosticsEnabled()) {
                    URL url;
                    String resourceName = new StringBuilder();
                    resourceName.append(str.replace('.', '/'));
                    resourceName.append(".class");
                    resourceName = resourceName.toString();
                    if (currentCL != null) {
                        url = currentCL.getResource(resourceName);
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(resourceName);
                        stringBuilder2.append(".class");
                        url = ClassLoader.getSystemResource(stringBuilder2.toString());
                    }
                    StringBuilder stringBuilder3;
                    if (url == null) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Class '");
                        stringBuilder3.append(str);
                        stringBuilder3.append("' [");
                        stringBuilder3.append(resourceName);
                        stringBuilder3.append("] cannot be found.");
                        logDiagnostic(stringBuilder3.toString());
                    } else {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Class '");
                        stringBuilder3.append(str);
                        stringBuilder3.append("' was found at '");
                        stringBuilder3.append(url);
                        stringBuilder3.append("'");
                        logDiagnostic(stringBuilder3.toString());
                    }
                }
                Class c2 = null;
                try {
                    c = Class.forName(str, true, currentCL);
                } catch (ClassNotFoundException e) {
                    ClassNotFoundException originalClassNotFoundException = e;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(originalClassNotFoundException.getMessage());
                    String msg2 = stringBuilder.toString();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("The log adapter '");
                    stringBuilder.append(str);
                    stringBuilder.append("' is not available via classloader ");
                    stringBuilder.append(LogFactory.objectId(currentCL));
                    stringBuilder.append(": ");
                    stringBuilder.append(msg2.trim());
                    logDiagnostic(stringBuilder.toString());
                    try {
                        c = Class.forName(logAdapterClassName);
                    } catch (ClassNotFoundException e2) {
                        ClassNotFoundException classNotFoundException = e2;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("");
                        stringBuilder4.append(e2.getMessage());
                        msg2 = stringBuilder4.toString();
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("The log adapter '");
                        stringBuilder4.append(str);
                        stringBuilder4.append("' is not available via the LogFactoryImpl class classloader: ");
                        stringBuilder4.append(msg2.trim());
                        logDiagnostic(stringBuilder4.toString());
                    }
                }
                constructor = c.getConstructor(this.logConstructorSignature);
                c2 = constructor.newInstance(params);
                if (c2 instanceof Log) {
                    logAdapterClass = c;
                    logAdapter = (Log) c2;
                    break;
                }
                handleFlawedHierarchy(currentCL, c);
                if (currentCL == null) {
                    break;
                }
                currentCL = currentCL.getParent();
            } catch (NoClassDefFoundError e3) {
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("");
                stringBuilder5.append(e3.getMessage());
                msg = stringBuilder5.toString();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("The log adapter '");
                stringBuilder2.append(str);
                stringBuilder2.append("' is missing dependencies when loaded via classloader ");
                stringBuilder2.append(LogFactory.objectId(currentCL));
                stringBuilder2.append(": ");
                stringBuilder2.append(msg.trim());
                logDiagnostic(stringBuilder2.toString());
            } catch (ExceptionInInitializerError e4) {
                msg = new StringBuilder();
                msg.append("");
                msg.append(e4.getMessage());
                msg = msg.toString();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("The log adapter '");
                stringBuilder2.append(str);
                stringBuilder2.append("' is unable to initialize itself when loaded via classloader ");
                stringBuilder2.append(LogFactory.objectId(currentCL));
                stringBuilder2.append(": ");
                stringBuilder2.append(msg.trim());
                logDiagnostic(stringBuilder2.toString());
            } catch (LogConfigurationException e5) {
                throw e5;
            } catch (Throwable t) {
                handleFlawedDiscovery(str, currentCL, t);
            }
        }
        if (logAdapter != null && affectState) {
            this.logClassName = str;
            this.logConstructor = constructor;
            try {
                this.logMethod = logAdapterClass.getMethod("setLogFactory", this.logMethodSignature);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Found method setLogFactory(LogFactory) in '");
                stringBuilder.append(str);
                stringBuilder.append("'");
                logDiagnostic(stringBuilder.toString());
            } catch (Throwable th) {
                this.logMethod = null;
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("[INFO] '");
                stringBuilder6.append(str);
                stringBuilder6.append("' from classloader ");
                stringBuilder6.append(LogFactory.objectId(currentCL));
                stringBuilder6.append(" does not declare optional method setLogFactory(LogFactory)");
                logDiagnostic(stringBuilder6.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Log adapter '");
            stringBuilder.append(str);
            stringBuilder.append("' from classloader ");
            stringBuilder.append(LogFactory.objectId(logAdapterClass.getClassLoader()));
            stringBuilder.append(" has been selected for use.");
            logDiagnostic(stringBuilder.toString());
        }
        return logAdapter;
    }

    private ClassLoader getBaseClassLoader() throws LogConfigurationException {
        ClassLoader thisClassLoader = getClassLoader(LogFactoryImpl.class);
        if (!this.useTCCL) {
            return thisClassLoader;
        }
        ClassLoader contextClassLoader = getContextClassLoader();
        ClassLoader baseClassLoader = getLowestClassLoader(contextClassLoader, thisClassLoader);
        if (baseClassLoader != null) {
            if (baseClassLoader != contextClassLoader) {
                if (!this.allowFlawedContext) {
                    throw new LogConfigurationException("Bad classloader hierarchy; LogFactoryImpl was loaded via a classloader that is not related to the current context classloader.");
                } else if (isDiagnosticsEnabled()) {
                    logDiagnostic("Warning: the context classloader is an ancestor of the classloader that loaded LogFactoryImpl; it should be the same or a descendant. The application using commons-logging should ensure the context classloader is used correctly.");
                }
            }
            return baseClassLoader;
        } else if (this.allowFlawedContext) {
            if (isDiagnosticsEnabled()) {
                logDiagnostic("[WARNING] the context classloader is not part of a parent-child relationship with the classloader that loaded LogFactoryImpl.");
            }
            return contextClassLoader;
        } else {
            throw new LogConfigurationException("Bad classloader hierarchy; LogFactoryImpl was loaded via a classloader that is not related to the current context classloader.");
        }
    }

    private ClassLoader getLowestClassLoader(ClassLoader c1, ClassLoader c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        ClassLoader current;
        for (current = c1; current != null; current = current.getParent()) {
            if (current == c2) {
                return c1;
            }
        }
        for (current = c2; current != null; current = current.getParent()) {
            if (current == c1) {
                return c2;
            }
        }
        return null;
    }

    private void handleFlawedDiscovery(String logAdapterClassName, ClassLoader classLoader, Throwable discoveryFlaw) {
        if (isDiagnosticsEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not instantiate Log '");
            stringBuilder.append(logAdapterClassName);
            stringBuilder.append("' -- ");
            stringBuilder.append(discoveryFlaw.getClass().getName());
            stringBuilder.append(": ");
            stringBuilder.append(discoveryFlaw.getLocalizedMessage());
            logDiagnostic(stringBuilder.toString());
        }
        if (!this.allowFlawedDiscovery) {
            throw new LogConfigurationException(discoveryFlaw);
        }
    }

    private void handleFlawedHierarchy(ClassLoader badClassLoader, Class badClass) throws LogConfigurationException {
        boolean implementsLog = false;
        String logInterfaceName = Log.class.getName();
        Class[] interfaces = badClass.getInterfaces();
        for (Class name : interfaces) {
            if (logInterfaceName.equals(name.getName())) {
                implementsLog = true;
                break;
            }
        }
        StringBuffer msg;
        if (implementsLog) {
            if (isDiagnosticsEnabled()) {
                StringBuilder stringBuilder;
                try {
                    ClassLoader logInterfaceClassLoader = getClassLoader(Log.class);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Class '");
                    stringBuilder.append(badClass.getName());
                    stringBuilder.append("' was found in classloader ");
                    stringBuilder.append(LogFactory.objectId(badClassLoader));
                    stringBuilder.append(". It is bound to a Log interface which is not the one loaded from classloader ");
                    stringBuilder.append(LogFactory.objectId(logInterfaceClassLoader));
                    logDiagnostic(stringBuilder.toString());
                } catch (Throwable th) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error while trying to output diagnostics about bad class '");
                    stringBuilder.append(badClass);
                    stringBuilder.append("'");
                    logDiagnostic(stringBuilder.toString());
                }
            }
            if (!this.allowFlawedHierarchy) {
                msg = new StringBuffer();
                msg.append("Terminating logging for this context ");
                msg.append("due to bad log hierarchy. ");
                msg.append("You have more than one version of '");
                msg.append(Log.class.getName());
                msg.append("' visible.");
                if (isDiagnosticsEnabled()) {
                    logDiagnostic(msg.toString());
                }
                throw new LogConfigurationException(msg.toString());
            } else if (isDiagnosticsEnabled()) {
                msg = new StringBuffer();
                msg.append("Warning: bad log hierarchy. ");
                msg.append("You have more than one version of '");
                msg.append(Log.class.getName());
                msg.append("' visible.");
                logDiagnostic(msg.toString());
            }
        } else if (!this.allowFlawedDiscovery) {
            msg = new StringBuffer();
            msg.append("Terminating logging for this context. ");
            msg.append("Log class '");
            msg.append(badClass.getName());
            msg.append("' does not implement the Log interface.");
            if (isDiagnosticsEnabled()) {
                logDiagnostic(msg.toString());
            }
            throw new LogConfigurationException(msg.toString());
        } else if (isDiagnosticsEnabled()) {
            msg = new StringBuffer();
            msg.append("[WARNING] Log class '");
            msg.append(badClass.getName());
            msg.append("' does not implement the Log interface.");
            logDiagnostic(msg.toString());
        }
    }
}
