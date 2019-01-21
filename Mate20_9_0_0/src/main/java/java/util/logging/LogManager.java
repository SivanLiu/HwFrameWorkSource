package java.util.logging;

import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.WeakHashMap;
import sun.util.logging.PlatformLogger;

public class LogManager {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";
    private static final int MAX_ITERATIONS = 400;
    private static final Level defaultLevel = Level.INFO;
    private static LoggingMXBean loggingMXBean = null;
    private static final LogManager manager = ((LogManager) AccessController.doPrivileged(new PrivilegedAction<LogManager>() {
        public LogManager run() {
            LogManager mgr = null;
            try {
                String cname = System.getProperty("java.util.logging.manager");
                if (cname != null) {
                    mgr = (LogManager) LogManager.getClassInstance(cname).newInstance();
                }
            } catch (Exception ex) {
                PrintStream printStream = System.err;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not load Logmanager \"");
                stringBuilder.append(null);
                stringBuilder.append("\"");
                printStream.println(stringBuilder.toString());
                ex.printStackTrace();
            }
            if (mgr == null) {
                return new LogManager();
            }
            return mgr;
        }
    }));
    private WeakHashMap<Object, LoggerContext> contextsMap;
    private final Permission controlPermission;
    private boolean deathImminent;
    private volatile boolean initializationDone;
    private boolean initializedCalled;
    private boolean initializedGlobalHandlers;
    private final Map<Object, Integer> listenerMap;
    private final ReferenceQueue<Logger> loggerRefQueue;
    private volatile Properties props;
    private volatile boolean readPrimordialConfiguration;
    private volatile Logger rootLogger;
    private final LoggerContext systemContext;
    private final LoggerContext userContext;

    private static class Beans {
        private static final Class<?> propertyChangeEventClass = getClass("java.beans.PropertyChangeEvent");
        private static final Class<?> propertyChangeListenerClass = getClass("java.beans.PropertyChangeListener");
        private static final Method propertyChangeMethod = getMethod(propertyChangeListenerClass, "propertyChange", propertyChangeEventClass);
        private static final Constructor<?> propertyEventCtor = getConstructor(propertyChangeEventClass, Object.class, String.class, Object.class, Object.class);

        private Beans() {
        }

        private static Class<?> getClass(String name) {
            try {
                return Class.forName(name, true, Beans.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        private static Constructor<?> getConstructor(Class<?> c, Class<?>... types) {
            if (c == null) {
                return null;
            }
            try {
                return c.getDeclaredConstructor(types);
            } catch (NoSuchMethodException x) {
                throw new AssertionError(x);
            }
        }

        private static Method getMethod(Class<?> c, String name, Class<?>... types) {
            if (c == null) {
                return null;
            }
            try {
                return c.getMethod(name, types);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }

        static boolean isBeansPresent() {
            return (propertyChangeListenerClass == null || propertyChangeEventClass == null) ? LogManager.$assertionsDisabled : true;
        }

        static Object newPropertyChangeEvent(Object source, String prop, Object oldValue, Object newValue) {
            try {
                return propertyEventCtor.newInstance(source, prop, oldValue, newValue);
            } catch (IllegalAccessException | InstantiationException x) {
                throw new AssertionError(x);
            } catch (InvocationTargetException x2) {
                Throwable cause = x2.getCause();
                if (cause instanceof Error) {
                    throw ((Error) cause);
                } else if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                } else {
                    throw new AssertionError(x2);
                }
            }
        }

        static void invokePropertyChange(Object listener, Object ev) {
            try {
                propertyChangeMethod.invoke(listener, ev);
            } catch (IllegalAccessException x) {
                throw new AssertionError(x);
            } catch (InvocationTargetException x2) {
                Throwable cause = x2.getCause();
                if (cause instanceof Error) {
                    throw ((Error) cause);
                } else if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                } else {
                    throw new AssertionError(x2);
                }
            }
        }
    }

    private static class LogNode {
        HashMap<String, LogNode> children;
        final LoggerContext context;
        LoggerWeakRef loggerRef;
        LogNode parent;

        LogNode(LogNode parent, LoggerContext context) {
            this.parent = parent;
            this.context = context;
        }

        void walkAndSetParent(Logger parent) {
            if (this.children != null) {
                for (LogNode node : this.children.values()) {
                    LoggerWeakRef ref = node.loggerRef;
                    Logger logger = ref == null ? null : (Logger) ref.get();
                    if (logger == null) {
                        node.walkAndSetParent(parent);
                    } else {
                        LogManager.doSetParent(logger, parent);
                    }
                }
            }
        }
    }

    class LoggerContext {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private final Hashtable<String, LoggerWeakRef> namedLoggers;
        private final LogNode root;

        static {
            Class cls = LogManager.class;
        }

        /* synthetic */ LoggerContext(LogManager x0, AnonymousClass1 x1) {
            this();
        }

        private LoggerContext() {
            this.namedLoggers = new Hashtable();
            this.root = new LogNode(null, this);
        }

        final boolean requiresDefaultLoggers() {
            boolean requiresDefaultLoggers = getOwner() == LogManager.manager ? true : LogManager.$assertionsDisabled;
            if (requiresDefaultLoggers) {
                getOwner().ensureLogManagerInitialized();
            }
            return requiresDefaultLoggers;
        }

        final LogManager getOwner() {
            return LogManager.this;
        }

        final Logger getRootLogger() {
            return getOwner().rootLogger;
        }

        final Logger getGlobalLogger() {
            return Logger.global;
        }

        Logger demandLogger(String name, String resourceBundleName) {
            return getOwner().demandLogger(name, resourceBundleName, null);
        }

        private void ensureInitialized() {
            if (requiresDefaultLoggers()) {
                ensureDefaultLogger(getRootLogger());
                ensureDefaultLogger(getGlobalLogger());
            }
        }

        /* JADX WARNING: Missing block: B:12:0x001d, code skipped:
            return r1;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        synchronized Logger findLogger(String name) {
            ensureInitialized();
            LoggerWeakRef ref = (LoggerWeakRef) this.namedLoggers.get(name);
            if (ref == null) {
                return null;
            }
            Logger logger = (Logger) ref.get();
            if (logger == null) {
                ref.dispose();
            }
        }

        private void ensureAllDefaultLoggers(Logger logger) {
            if (requiresDefaultLoggers()) {
                String name = logger.getName();
                if (!name.isEmpty()) {
                    ensureDefaultLogger(getRootLogger());
                    if (!Logger.GLOBAL_LOGGER_NAME.equals(name)) {
                        ensureDefaultLogger(getGlobalLogger());
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:11:0x0027, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void ensureDefaultLogger(Logger logger) {
            if (requiresDefaultLoggers() && logger != null && ((logger == Logger.global || logger == LogManager.this.rootLogger) && !this.namedLoggers.containsKey(logger.getName()))) {
                addLocalLogger(logger, (boolean) LogManager.$assertionsDisabled);
            }
        }

        boolean addLocalLogger(Logger logger) {
            return addLocalLogger(logger, requiresDefaultLoggers());
        }

        boolean addLocalLogger(Logger logger, LogManager manager) {
            return addLocalLogger(logger, requiresDefaultLoggers(), manager);
        }

        boolean addLocalLogger(Logger logger, boolean addDefaultLoggersIfNeeded) {
            return addLocalLogger(logger, addDefaultLoggersIfNeeded, LogManager.manager);
        }

        synchronized boolean addLocalLogger(Logger logger, boolean addDefaultLoggersIfNeeded, LogManager manager) {
            if (addDefaultLoggersIfNeeded) {
                try {
                    ensureAllDefaultLoggers(logger);
                } catch (Throwable th) {
                }
            }
            String name = logger.getName();
            if (name != null) {
                LoggerWeakRef ref = (LoggerWeakRef) this.namedLoggers.get(name);
                if (ref != null) {
                    if (ref.get() != null) {
                        return LogManager.$assertionsDisabled;
                    }
                    ref.dispose();
                }
                LogManager owner = getOwner();
                logger.setLogManager(owner);
                Objects.requireNonNull(owner);
                ref = new LoggerWeakRef(logger);
                this.namedLoggers.put(name, ref);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(name);
                stringBuilder.append(".level");
                Level level = owner.getLevelProperty(stringBuilder.toString(), null);
                if (!(level == null || logger.isLevelInitialized())) {
                    LogManager.doSetLevel(logger, level);
                }
                processParentHandlers(logger, name);
                LogNode node = getNode(name);
                node.loggerRef = ref;
                Logger parent = null;
                for (LogNode nodep = node.parent; nodep != null; nodep = nodep.parent) {
                    LoggerWeakRef nodeRef = nodep.loggerRef;
                    if (nodeRef != null) {
                        parent = (Logger) nodeRef.get();
                        if (parent != null) {
                            break;
                        }
                    }
                }
                if (parent != null) {
                    LogManager.doSetParent(logger, parent);
                }
                node.walkAndSetParent(logger);
                ref.setNode(node);
                return true;
            }
            throw new NullPointerException();
        }

        synchronized void removeLoggerRef(String name, LoggerWeakRef ref) {
            this.namedLoggers.remove(name, ref);
        }

        synchronized Enumeration<String> getLoggerNames() {
            ensureInitialized();
            return this.namedLoggers.keys();
        }

        private void processParentHandlers(final Logger logger, final String name) {
            final LogManager owner = getOwner();
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    if (logger != owner.rootLogger) {
                        boolean useParent = owner;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(name);
                        stringBuilder.append(".useParentHandlers");
                        if (!useParent.getBooleanProperty(stringBuilder.toString(), true)) {
                            logger.setUseParentHandlers(LogManager.$assertionsDisabled);
                        }
                    }
                    return null;
                }
            });
            int ix = 1;
            while (true) {
                int ix2 = name.indexOf(".", ix);
                if (ix2 >= 0) {
                    String pname = name.substring(null, ix2);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(pname);
                    stringBuilder.append(".level");
                    if (owner.getProperty(stringBuilder.toString()) == null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(pname);
                        stringBuilder.append(".handlers");
                        if (owner.getProperty(stringBuilder.toString()) == null) {
                            ix = ix2 + 1;
                        }
                    }
                    demandLogger(pname, null);
                    ix = ix2 + 1;
                } else {
                    return;
                }
            }
        }

        LogNode getNode(String name) {
            if (name == null || name.equals("")) {
                return this.root;
            }
            LogNode node = this.root;
            while (name.length() > 0) {
                String head;
                int ix = name.indexOf(".");
                if (ix > 0) {
                    head = name.substring(null, ix);
                    name = name.substring(ix + 1);
                } else {
                    head = name;
                    name = "";
                }
                if (node.children == null) {
                    node.children = new HashMap();
                }
                LogNode child = (LogNode) node.children.get(head);
                if (child == null) {
                    child = new LogNode(node, this);
                    node.children.put(head, child);
                }
                node = child;
            }
            return node;
        }
    }

    private final class RootLogger extends Logger {
        /* synthetic */ RootLogger(LogManager x0, AnonymousClass1 x1) {
            this();
        }

        private RootLogger() {
            super("", null, null, LogManager.this, true);
        }

        public void log(LogRecord record) {
            LogManager.this.initializeGlobalHandlers();
            super.log(record);
        }

        public void addHandler(Handler h) {
            LogManager.this.initializeGlobalHandlers();
            super.addHandler(h);
        }

        public void removeHandler(Handler h) {
            LogManager.this.initializeGlobalHandlers();
            super.removeHandler(h);
        }

        Handler[] accessCheckedHandlers() {
            LogManager.this.initializeGlobalHandlers();
            return super.accessCheckedHandlers();
        }
    }

    final class SystemLoggerContext extends LoggerContext {
        SystemLoggerContext() {
            super(LogManager.this, null);
        }

        Logger demandLogger(String name, String resourceBundleName) {
            Logger result = findLogger(name);
            if (result == null) {
                Logger logger = new Logger(name, resourceBundleName, null, getOwner(), true);
                do {
                    if (addLocalLogger(logger)) {
                        result = logger;
                        continue;
                    } else {
                        result = findLogger(name);
                        continue;
                    }
                } while (result == null);
            }
            return result;
        }
    }

    private class Cleaner extends Thread {
        /* synthetic */ Cleaner(LogManager x0, AnonymousClass1 x1) {
            this();
        }

        private Cleaner() {
            setContextClassLoader(null);
        }

        public void run() {
            LogManager mgr = LogManager.manager;
            synchronized (LogManager.this) {
                LogManager.this.deathImminent = true;
                LogManager.this.initializedGlobalHandlers = true;
            }
            LogManager.this.reset();
        }
    }

    final class LoggerWeakRef extends WeakReference<Logger> {
        private boolean disposed = LogManager.$assertionsDisabled;
        private String name;
        private LogNode node;
        private WeakReference<Logger> parentRef;

        LoggerWeakRef(Logger logger) {
            super(logger, LogManager.this.loggerRefQueue);
            this.name = logger.getName();
        }

        /* JADX WARNING: Missing block: B:8:0x000b, code skipped:
            r0 = r5.node;
     */
        /* JADX WARNING: Missing block: B:9:0x000e, code skipped:
            if (r0 == null) goto L_0x0029;
     */
        /* JADX WARNING: Missing block: B:10:0x0010, code skipped:
            r2 = r0.context;
     */
        /* JADX WARNING: Missing block: B:11:0x0012, code skipped:
            monitor-enter(r2);
     */
        /* JADX WARNING: Missing block: B:13:?, code skipped:
            r0.context.removeLoggerRef(r5.name, r5);
            r5.name = null;
     */
        /* JADX WARNING: Missing block: B:14:0x001e, code skipped:
            if (r0.loggerRef != r5) goto L_0x0022;
     */
        /* JADX WARNING: Missing block: B:15:0x0020, code skipped:
            r0.loggerRef = null;
     */
        /* JADX WARNING: Missing block: B:16:0x0022, code skipped:
            r5.node = null;
     */
        /* JADX WARNING: Missing block: B:17:0x0024, code skipped:
            monitor-exit(r2);
     */
        /* JADX WARNING: Missing block: B:23:0x002b, code skipped:
            if (r5.parentRef == null) goto L_0x003c;
     */
        /* JADX WARNING: Missing block: B:24:0x002d, code skipped:
            r2 = (java.util.logging.Logger) r5.parentRef.get();
     */
        /* JADX WARNING: Missing block: B:25:0x0035, code skipped:
            if (r2 == null) goto L_0x003a;
     */
        /* JADX WARNING: Missing block: B:26:0x0037, code skipped:
            r2.removeChildLogger(r5);
     */
        /* JADX WARNING: Missing block: B:27:0x003a, code skipped:
            r5.parentRef = null;
     */
        /* JADX WARNING: Missing block: B:28:0x003c, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        void dispose() {
            synchronized (this) {
                if (this.disposed) {
                    return;
                }
                this.disposed = true;
            }
        }

        void setNode(LogNode node) {
            this.node = node;
        }

        void setParentRef(WeakReference<Logger> parentRef) {
            this.parentRef = parentRef;
        }
    }

    protected LogManager() {
        this(checkSubclassPermissions());
    }

    private LogManager(Void checked) {
        this.props = new Properties();
        this.listenerMap = new HashMap();
        this.systemContext = new SystemLoggerContext();
        this.userContext = new LoggerContext(this, null);
        this.initializedGlobalHandlers = true;
        this.initializedCalled = $assertionsDisabled;
        this.initializationDone = $assertionsDisabled;
        this.contextsMap = null;
        this.loggerRefQueue = new ReferenceQueue();
        this.controlPermission = new LoggingPermission("control", null);
        try {
            Runtime.getRuntime().addShutdownHook(new Cleaner(this, null));
        } catch (IllegalStateException e) {
        }
    }

    private static Void checkSubclassPermissions() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("shutdownHooks"));
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:25:0x002f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void ensureLogManagerInitialized() {
        if (!this.initializationDone && this == manager) {
            synchronized (this) {
                if (!(this.initializedCalled ? true : $assertionsDisabled)) {
                    if (!this.initializationDone) {
                        this.initializedCalled = true;
                        try {
                            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                                static final /* synthetic */ boolean $assertionsDisabled = false;

                                static {
                                    Class cls = LogManager.class;
                                }

                                public Object run() {
                                    this.readPrimordialConfiguration();
                                    LogManager logManager = this;
                                    LogManager logManager2 = this;
                                    Objects.requireNonNull(logManager2);
                                    logManager.rootLogger = new RootLogger(logManager2, null);
                                    this.addLogger(this.rootLogger);
                                    if (!this.rootLogger.isLevelInitialized()) {
                                        this.rootLogger.setLevel(LogManager.defaultLevel);
                                    }
                                    this.addLogger(Logger.global);
                                    return null;
                                }
                            });
                        } finally {
                            this.initializationDone = true;
                        }
                    }
                }
            }
        }
    }

    public static LogManager getLogManager() {
        if (manager != null) {
            manager.ensureLogManagerInitialized();
        }
        return manager;
    }

    private void readPrimordialConfiguration() {
        if (!this.readPrimordialConfiguration) {
            synchronized (this) {
                if (!this.readPrimordialConfiguration) {
                    if (System.out == null) {
                    } else {
                        this.readPrimordialConfiguration = true;
                        try {
                            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                                public Void run() throws Exception {
                                    LogManager.this.readConfiguration();
                                    PlatformLogger.redirectPlatformLoggers();
                                    return null;
                                }
                            });
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    public void addPropertyChangeListener(PropertyChangeListener l) throws SecurityException {
        PropertyChangeListener listener = (PropertyChangeListener) Objects.requireNonNull(l);
        checkPermission();
        synchronized (this.listenerMap) {
            Integer value = (Integer) this.listenerMap.get(listener);
            int i = 1;
            if (value != null) {
                i = 1 + value.intValue();
            }
            this.listenerMap.put(listener, Integer.valueOf(i));
        }
    }

    @Deprecated
    public void removePropertyChangeListener(PropertyChangeListener l) throws SecurityException {
        checkPermission();
        if (l != null) {
            PropertyChangeListener listener = l;
            synchronized (this.listenerMap) {
                Integer value = (Integer) this.listenerMap.get(listener);
                if (value != null) {
                    int i = value.intValue();
                    if (i == 1) {
                        this.listenerMap.remove(listener);
                    } else {
                        this.listenerMap.put(listener, Integer.valueOf(i - 1));
                    }
                }
            }
        }
    }

    private LoggerContext getUserContext() {
        return this.userContext;
    }

    final LoggerContext getSystemContext() {
        return this.systemContext;
    }

    private List<LoggerContext> contexts() {
        List<LoggerContext> cxs = new ArrayList();
        cxs.add(getSystemContext());
        cxs.add(getUserContext());
        return cxs;
    }

    Logger demandLogger(String name, String resourceBundleName, Class<?> caller) {
        Logger result = getLogger(name);
        if (result == null) {
            Logger logger = new Logger(name, resourceBundleName, caller, this, $assertionsDisabled);
            while (!addLogger(logger)) {
                result = getLogger(name);
                if (result != null) {
                }
            }
            return logger;
        }
        return result;
    }

    Logger demandSystemLogger(String name, String resourceBundleName) {
        Logger logger;
        final Logger sysLogger = getSystemContext().demandLogger(name, resourceBundleName);
        do {
            if (addLogger(sysLogger)) {
                logger = sysLogger;
                continue;
            } else {
                logger = getLogger(name);
                continue;
            }
        } while (logger == null);
        if (logger != sysLogger && sysLogger.accessCheckedHandlers().length == 0) {
            final Logger l = logger;
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    for (Handler hdl : l.accessCheckedHandlers()) {
                        sysLogger.addHandler(hdl);
                    }
                    return null;
                }
            });
        }
        return sysLogger;
    }

    private static Class getClassInstance(String cname) {
        if (cname == null) {
            return null;
        }
        try {
            return ClassLoader.getSystemClassLoader().loadClass(cname);
        } catch (ClassNotFoundException e) {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(cname);
            } catch (ClassNotFoundException e2) {
                return null;
            }
        }
    }

    private void loadLoggerHandlers(final Logger logger, String name, final String handlersPropertyName) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                String[] names = LogManager.this.parseClassNames(handlersPropertyName);
                for (String word : names) {
                    try {
                        Handler hdl = (Handler) LogManager.getClassInstance(word).newInstance();
                        String levs = LogManager.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(word);
                        stringBuilder.append(".level");
                        levs = levs.getProperty(stringBuilder.toString());
                        if (levs != null) {
                            Level l = Level.findLevel(levs);
                            if (l != null) {
                                hdl.setLevel(l);
                            } else {
                                PrintStream printStream = System.err;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Can't set level for ");
                                stringBuilder2.append(word);
                                printStream.println(stringBuilder2.toString());
                            }
                        }
                        logger.addHandler(hdl);
                    } catch (Exception ex) {
                        PrintStream printStream2 = System.err;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Can't load log handler \"");
                        stringBuilder3.append(word);
                        stringBuilder3.append("\"");
                        printStream2.println(stringBuilder3.toString());
                        printStream2 = System.err;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("");
                        stringBuilder3.append(ex);
                        printStream2.println(stringBuilder3.toString());
                        ex.printStackTrace();
                    }
                }
                return null;
            }
        });
    }

    final void drainLoggerRefQueueBounded() {
        int i = 0;
        while (i < 400 && this.loggerRefQueue != null) {
            LoggerWeakRef ref = (LoggerWeakRef) this.loggerRefQueue.poll();
            if (ref != null) {
                ref.dispose();
                i++;
            } else {
                return;
            }
        }
    }

    public boolean addLogger(Logger logger) {
        String name = logger.getName();
        if (name != null) {
            drainLoggerRefQueueBounded();
            if (!getUserContext().addLocalLogger(logger, this)) {
                return $assertionsDisabled;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append(".handlers");
            loadLoggerHandlers(logger, name, stringBuilder.toString());
            return true;
        }
        throw new NullPointerException();
    }

    private static void doSetLevel(final Logger logger, final Level level) {
        if (System.getSecurityManager() == null) {
            logger.setLevel(level);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    logger.setLevel(level);
                    return null;
                }
            });
        }
    }

    private static void doSetParent(final Logger logger, final Logger parent) {
        if (System.getSecurityManager() == null) {
            logger.setParent(parent);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    logger.setParent(parent);
                    return null;
                }
            });
        }
    }

    public Logger getLogger(String name) {
        return getUserContext().findLogger(name);
    }

    public Enumeration<String> getLoggerNames() {
        return getUserContext().getLoggerNames();
    }

    public void readConfiguration() throws IOException, SecurityException {
        Exception e;
        checkPermission();
        String cname = System.getProperty("java.util.logging.config.class");
        if (cname != null) {
            try {
                getClassInstance(cname).newInstance();
                return;
            } catch (Exception ex) {
                PrintStream printStream = System.err;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Logging configuration class \"");
                stringBuilder.append(cname);
                stringBuilder.append("\" failed");
                printStream.println(stringBuilder.toString());
                printStream = System.err;
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(ex);
                printStream.println(stringBuilder.toString());
            }
        }
        String fname = System.getProperty("java.util.logging.config.file");
        if (fname == null) {
            fname = System.getProperty("java.home");
            if (fname != null) {
                fname = new File(new File(fname, "lib"), "logging.properties").getCanonicalPath();
            } else {
                throw new Error("Can't find java.home ??");
            }
        }
        try {
            e = new FileInputStream(fname);
        } catch (Exception e2) {
            InputStream in = LogManager.class.getResourceAsStream("logging.properties");
            if (in != null) {
                e2 = in;
            } else {
                throw e2;
            }
        }
        try {
            readConfiguration(new BufferedInputStream(e2));
        } finally {
            if (e2 != null) {
                e2.close();
            }
        }
    }

    public void reset() throws SecurityException {
        checkPermission();
        synchronized (this) {
            this.props = new Properties();
            this.initializedGlobalHandlers = true;
        }
        for (LoggerContext cx : contexts()) {
            Enumeration<String> enum_ = cx.getLoggerNames();
            while (enum_.hasMoreElements()) {
                Logger logger = cx.findLogger((String) enum_.nextElement());
                if (logger != null) {
                    resetLogger(logger);
                }
            }
        }
    }

    private void resetLogger(Logger logger) {
        Handler[] targets = logger.getHandlers();
        for (Handler h : targets) {
            logger.removeHandler(h);
            try {
                h.close();
            } catch (Exception e) {
            }
        }
        String name = logger.getName();
        if (name == null || !name.equals("")) {
            logger.setLevel(null);
        } else {
            logger.setLevel(defaultLevel);
        }
    }

    private String[] parseClassNames(String propertyName) {
        String hands = getProperty(propertyName);
        if (hands == null) {
            return new String[0];
        }
        hands = hands.trim();
        int ix = 0;
        List<String> result = new ArrayList();
        while (ix < hands.length()) {
            int end = ix;
            while (end < hands.length() && !Character.isWhitespace(hands.charAt(end)) && hands.charAt(end) != ',') {
                end++;
            }
            String word = hands.substring(ix, end);
            ix = end + 1;
            word = word.trim();
            if (word.length() != 0) {
                result.add(word);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    public void readConfiguration(InputStream ins) throws IOException, SecurityException {
        checkPermission();
        reset();
        this.props.load(ins);
        String[] names = parseClassNames("config");
        for (String word : names) {
            try {
                getClassInstance(word).newInstance();
            } catch (Exception ex) {
                PrintStream printStream = System.err;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't load config class \"");
                stringBuilder.append(word);
                stringBuilder.append("\"");
                printStream.println(stringBuilder.toString());
                printStream = System.err;
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(ex);
                printStream.println(stringBuilder.toString());
            }
        }
        setLevelsOnExistingLoggers();
        Map<Object, Integer> listeners = null;
        synchronized (this.listenerMap) {
            if (!this.listenerMap.isEmpty()) {
                listeners = new HashMap(this.listenerMap);
            }
        }
        if (listeners != null) {
            Object ev = Beans.newPropertyChangeEvent(LogManager.class, null, null, null);
            for (Entry<Object, Integer> entry : listeners.entrySet()) {
                Object listener = entry.getKey();
                int count = ((Integer) entry.getValue()).intValue();
                for (int i = 0; i < count; i++) {
                    Beans.invokePropertyChange(listener, ev);
                }
            }
        }
        synchronized (this) {
            this.initializedGlobalHandlers = $assertionsDisabled;
        }
    }

    public String getProperty(String name) {
        return this.props.getProperty(name);
    }

    String getStringProperty(String name, String defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        return val.trim();
    }

    int getIntProperty(String name, int defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    boolean getBooleanProperty(String name, boolean defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        val = val.toLowerCase();
        if (val.equals("true") || val.equals("1")) {
            return true;
        }
        if (val.equals("false") || val.equals("0")) {
            return $assertionsDisabled;
        }
        return defaultValue;
    }

    Level getLevelProperty(String name, Level defaultValue) {
        String val = getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        Level l = Level.findLevel(val.trim());
        return l != null ? l : defaultValue;
    }

    Filter getFilterProperty(String name, Filter defaultValue) {
        String val = getProperty(name);
        if (val != null) {
            try {
                return (Filter) getClassInstance(val).newInstance();
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    Formatter getFormatterProperty(String name, Formatter defaultValue) {
        String val = getProperty(name);
        if (val != null) {
            try {
                return (Formatter) getClassInstance(val).newInstance();
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    private synchronized void initializeGlobalHandlers() {
        if (!this.initializedGlobalHandlers) {
            this.initializedGlobalHandlers = true;
            if (!this.deathImminent) {
                loadLoggerHandlers(this.rootLogger, null, "handlers");
            }
        }
    }

    void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(this.controlPermission);
        }
    }

    public void checkAccess() throws SecurityException {
        checkPermission();
    }

    private synchronized void setLevelsOnExistingLoggers() {
        Enumeration<?> enum_ = this.props.propertyNames();
        while (enum_.hasMoreElements()) {
            String key = (String) enum_.nextElement();
            if (key.endsWith(".level")) {
                String name = key.substring(null, key.length() - 6);
                Level level = getLevelProperty(key, null);
                if (level == null) {
                    PrintStream printStream = System.err;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad level value for property: ");
                    stringBuilder.append(key);
                    printStream.println(stringBuilder.toString());
                } else {
                    for (LoggerContext cx : contexts()) {
                        Logger l = cx.findLogger(name);
                        if (l != null) {
                            l.setLevel(level);
                        }
                    }
                }
            }
        }
    }

    public static synchronized LoggingMXBean getLoggingMXBean() {
        LoggingMXBean loggingMXBean;
        synchronized (LogManager.class) {
            if (loggingMXBean == null) {
                loggingMXBean = new Logging();
            }
            loggingMXBean = loggingMXBean;
        }
        return loggingMXBean;
    }
}
