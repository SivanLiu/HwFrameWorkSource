package org.apache.commons.logging;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.NoOpLog;

@Deprecated
public class LogSource {
    protected static boolean jdk14IsAvailable;
    protected static boolean log4jIsAvailable;
    protected static Constructor logImplctor = null;
    protected static Hashtable logs = new Hashtable();

    static {
        log4jIsAvailable = false;
        jdk14IsAvailable = false;
        try {
            if (Class.forName("org.apache.log4j.Logger") != null) {
                log4jIsAvailable = true;
            } else {
                log4jIsAvailable = false;
            }
        } catch (Throwable th) {
            log4jIsAvailable = false;
        }
        try {
            String name;
            if (Class.forName("java.util.logging.Logger") == null || Class.forName("org.apache.commons.logging.impl.Jdk14Logger") == null) {
                jdk14IsAvailable = false;
                name = null;
                try {
                    name = System.getProperty("org.apache.commons.logging.log");
                    if (name == null) {
                        name = System.getProperty(LogFactoryImpl.LOG_PROPERTY);
                    }
                } catch (Throwable th2) {
                }
                if (name != null) {
                    try {
                        setLogImplementation(name);
                    } catch (Throwable th3) {
                        return;
                    }
                }
                try {
                    if (log4jIsAvailable) {
                        setLogImplementation("org.apache.commons.logging.impl.Log4JLogger");
                    } else if (jdk14IsAvailable) {
                        setLogImplementation("org.apache.commons.logging.impl.NoOpLog");
                    } else {
                        setLogImplementation("org.apache.commons.logging.impl.Jdk14Logger");
                    }
                } catch (Throwable th4) {
                    return;
                }
            }
            jdk14IsAvailable = true;
            name = null;
            name = System.getProperty("org.apache.commons.logging.log");
            if (name == null) {
                name = System.getProperty(LogFactoryImpl.LOG_PROPERTY);
            }
            if (name != null) {
                setLogImplementation(name);
            } else if (log4jIsAvailable) {
                setLogImplementation("org.apache.commons.logging.impl.Log4JLogger");
            } else if (jdk14IsAvailable) {
                setLogImplementation("org.apache.commons.logging.impl.NoOpLog");
            } else {
                setLogImplementation("org.apache.commons.logging.impl.Jdk14Logger");
            }
        } catch (Throwable th5) {
            jdk14IsAvailable = false;
        }
    }

    private LogSource() {
    }

    public static void setLogImplementation(String classname) throws LinkageError, ExceptionInInitializerError, NoSuchMethodException, SecurityException, ClassNotFoundException {
        try {
            logImplctor = Class.forName(classname).getConstructor(new Class[]{"".getClass()});
        } catch (Throwable th) {
            logImplctor = null;
        }
    }

    public static void setLogImplementation(Class logclass) throws LinkageError, ExceptionInInitializerError, NoSuchMethodException, SecurityException {
        logImplctor = logclass.getConstructor(new Class[]{"".getClass()});
    }

    public static Log getInstance(String name) {
        Log log = (Log) logs.get(name);
        if (log != null) {
            return log;
        }
        log = makeNewLogInstance(name);
        logs.put(name, log);
        return log;
    }

    public static Log getInstance(Class clazz) {
        return getInstance(clazz.getName());
    }

    public static Log makeNewLogInstance(String name) {
        Log log;
        try {
            log = (Log) logImplctor.newInstance(new Object[]{name});
        } catch (Throwable th) {
            log = null;
        }
        if (log == null) {
            return new NoOpLog(name);
        }
        return log;
    }

    public static String[] getLogNames() {
        return (String[]) logs.keySet().toArray(new String[logs.size()]);
    }
}
