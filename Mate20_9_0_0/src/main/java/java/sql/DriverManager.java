package java.sql;

import dalvik.system.VMStack;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import sun.reflect.CallerSensitive;

public class DriverManager {
    static final SQLPermission SET_LOG_PERMISSION = new SQLPermission("setLog");
    private static volatile PrintStream logStream = null;
    private static final Object logSync = new Object();
    private static volatile PrintWriter logWriter = null;
    private static volatile int loginTimeout = 0;
    private static final CopyOnWriteArrayList<DriverInfo> registeredDrivers = new CopyOnWriteArrayList();

    static {
        loadInitialDrivers();
        println("JDBC DriverManager initialized");
    }

    private DriverManager() {
    }

    public static PrintWriter getLogWriter() {
        return logWriter;
    }

    public static void setLogWriter(PrintWriter out) {
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_LOG_PERMISSION);
        }
        logStream = null;
        logWriter = out;
    }

    @CallerSensitive
    public static Connection getConnection(String url, Properties info) throws SQLException {
        return getConnection(url, info, VMStack.getCallingClassLoader());
    }

    @CallerSensitive
    public static Connection getConnection(String url, String user, String password) throws SQLException {
        Properties info = new Properties();
        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }
        return getConnection(url, info, VMStack.getCallingClassLoader());
    }

    @CallerSensitive
    public static Connection getConnection(String url) throws SQLException {
        return getConnection(url, new Properties(), VMStack.getCallingClassLoader());
    }

    @CallerSensitive
    public static Driver getDriver(String url) throws SQLException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DriverManager.getDriver(\"");
        stringBuilder.append(url);
        stringBuilder.append("\")");
        println(stringBuilder.toString());
        ClassLoader callerClassLoader = VMStack.getCallingClassLoader();
        Iterator it = registeredDrivers.iterator();
        while (it.hasNext()) {
            DriverInfo aDriver = (DriverInfo) it.next();
            StringBuilder stringBuilder2;
            if (isDriverAllowed(aDriver.driver, callerClassLoader)) {
                try {
                    if (aDriver.driver.acceptsURL(url)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("getDriver returning ");
                        stringBuilder2.append(aDriver.driver.getClass().getName());
                        println(stringBuilder2.toString());
                        return aDriver.driver;
                    }
                } catch (SQLException e) {
                }
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    skipping: ");
                stringBuilder2.append(aDriver.driver.getClass().getName());
                println(stringBuilder2.toString());
            }
        }
        println("getDriver: no suitable driver");
        throw new SQLException("No suitable driver", "08001");
    }

    public static synchronized void registerDriver(Driver driver) throws SQLException {
        synchronized (DriverManager.class) {
            if (driver != null) {
                registeredDrivers.addIfAbsent(new DriverInfo(driver));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("registerDriver: ");
                stringBuilder.append((Object) driver);
                println(stringBuilder.toString());
            } else {
                throw new NullPointerException();
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0044, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @CallerSensitive
    public static synchronized void deregisterDriver(Driver driver) throws SQLException {
        synchronized (DriverManager.class) {
            if (driver == null) {
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DriverManager.deregisterDriver: ");
            stringBuilder.append((Object) driver);
            println(stringBuilder.toString());
            Object aDriver = new DriverInfo(driver);
            if (!registeredDrivers.contains(aDriver)) {
                println("    couldn't find driver to unload");
            } else if (isDriverAllowed(driver, VMStack.getCallingClassLoader())) {
                registeredDrivers.remove(aDriver);
            } else {
                throw new SecurityException();
            }
        }
    }

    @CallerSensitive
    public static Enumeration<Driver> getDrivers() {
        Vector<Driver> result = new Vector();
        ClassLoader callerClassLoader = VMStack.getCallingClassLoader();
        Iterator it = registeredDrivers.iterator();
        while (it.hasNext()) {
            DriverInfo aDriver = (DriverInfo) it.next();
            if (isDriverAllowed(aDriver.driver, callerClassLoader)) {
                result.addElement(aDriver.driver);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("    skipping: ");
                stringBuilder.append(aDriver.getClass().getName());
                println(stringBuilder.toString());
            }
        }
        return result.elements();
    }

    public static void setLoginTimeout(int seconds) {
        loginTimeout = seconds;
    }

    public static int getLoginTimeout() {
        return loginTimeout;
    }

    @Deprecated
    public static void setLogStream(PrintStream out) {
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            sec.checkPermission(SET_LOG_PERMISSION);
        }
        logStream = out;
        if (out != null) {
            logWriter = new PrintWriter((OutputStream) out);
        } else {
            logWriter = null;
        }
    }

    @Deprecated
    public static PrintStream getLogStream() {
        return logStream;
    }

    public static void println(String message) {
        synchronized (logSync) {
            if (logWriter != null) {
                logWriter.println(message);
                logWriter.flush();
            }
        }
    }

    private static boolean isDriverAllowed(Driver driver, ClassLoader classLoader) {
        if (driver == null) {
            return false;
        }
        Class<?> aClass = null;
        boolean z = true;
        try {
            aClass = Class.forName(driver.getClass().getName(), true, classLoader);
        } catch (Exception e) {
        }
        if (aClass != driver.getClass()) {
            z = false;
        }
        return z;
    }

    private static void loadInitialDrivers() {
        String drivers;
        try {
            drivers = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("jdbc.drivers");
                }
            });
        } catch (Exception e) {
            drivers = null;
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Iterator driversIterator = ServiceLoader.load(Driver.class).iterator();
                while (driversIterator.hasNext()) {
                    try {
                        driversIterator.next();
                    } catch (Throwable th) {
                    }
                }
                return null;
            }
        });
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DriverManager.initialize: jdbc.drivers = ");
        stringBuilder.append(drivers);
        println(stringBuilder.toString());
        if (drivers != null && !drivers.equals("")) {
            String[] driversList = drivers.split(":");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("number of Drivers:");
            stringBuilder2.append(driversList.length);
            println(stringBuilder2.toString());
            for (String aDriver : driversList) {
                try {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("DriverManager.Initialize: loading ");
                    stringBuilder3.append(aDriver);
                    println(stringBuilder3.toString());
                    Class.forName(aDriver, true, ClassLoader.getSystemClassLoader());
                } catch (Exception ex) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("DriverManager.Initialize: load failed: ");
                    stringBuilder4.append(ex);
                    println(stringBuilder4.toString());
                }
            }
        }
    }

    private static Connection getConnection(String url, Properties info, ClassLoader callerCL) throws SQLException {
        synchronized (DriverManager.class) {
            if (callerCL == null) {
                try {
                    callerCL = Thread.currentThread().getContextClassLoader();
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            }
        }
        if (url != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DriverManager.getConnection(\"");
            stringBuilder.append(url);
            stringBuilder.append("\")");
            println(stringBuilder.toString());
            Object reason = null;
            Iterator it = registeredDrivers.iterator();
            while (it.hasNext()) {
                DriverInfo aDriver = (DriverInfo) it.next();
                StringBuilder stringBuilder2;
                if (isDriverAllowed(aDriver.driver, callerCL)) {
                    try {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("    trying ");
                        stringBuilder2.append(aDriver.driver.getClass().getName());
                        println(stringBuilder2.toString());
                        Connection con = aDriver.driver.connect(url, info);
                        if (con != null) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("getConnection returning ");
                            stringBuilder3.append(aDriver.driver.getClass().getName());
                            println(stringBuilder3.toString());
                            return con;
                        }
                    } catch (SQLException ex) {
                        if (reason == null) {
                            reason = ex;
                        }
                    }
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    skipping: ");
                    stringBuilder2.append(aDriver.getClass().getName());
                    println(stringBuilder2.toString());
                }
            }
            StringBuilder stringBuilder4;
            if (reason != null) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("getConnection failed: ");
                stringBuilder4.append(reason);
                println(stringBuilder4.toString());
                throw reason;
            }
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("getConnection: no suitable driver found for ");
            stringBuilder4.append(url);
            println(stringBuilder4.toString());
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("No suitable driver found for ");
            stringBuilder5.append(url);
            throw new SQLException(stringBuilder5.toString(), "08001");
        }
        throw new SQLException("The url cannot be null", "08001");
    }
}
