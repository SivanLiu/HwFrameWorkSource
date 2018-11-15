package junit.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.Properties;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestSuite;

public abstract class BaseTestRunner implements TestListener {
    public static final String SUITE_METHODNAME = "suite";
    private static Properties fPreferences;
    static boolean fgFilterStack = true;
    static int fgMaxMessageLength;
    boolean fLoading = true;

    protected abstract void runFailed(String str);

    public abstract void testEnded(String str);

    public abstract void testFailed(int i, Test test, Throwable th);

    public abstract void testStarted(String str);

    static {
        fgMaxMessageLength = 500;
        fgMaxMessageLength = getPreference("maxmessage", fgMaxMessageLength);
    }

    public synchronized void startTest(Test test) {
        testStarted(test.toString());
    }

    protected static void setPreferences(Properties preferences) {
        fPreferences = preferences;
    }

    protected static Properties getPreferences() {
        if (fPreferences == null) {
            fPreferences = new Properties();
            fPreferences.put("loading", "true");
            fPreferences.put("filterstack", "true");
            readPreferences();
        }
        return fPreferences;
    }

    public static void savePreferences() throws IOException {
        FileOutputStream fos = new FileOutputStream(getPreferencesFile());
        try {
            getPreferences().store(fos, "");
        } finally {
            fos.close();
        }
    }

    public static void setPreference(String key, String value) {
        getPreferences().put(key, value);
    }

    public synchronized void endTest(Test test) {
        testEnded(test.toString());
    }

    public synchronized void addError(Test test, Throwable e) {
        testFailed(1, test, e);
    }

    public synchronized void addFailure(Test test, AssertionFailedError e) {
        testFailed(2, test, e);
    }

    public Test getTest(String suiteClassName) {
        StringBuilder stringBuilder;
        if (suiteClassName.length() <= 0) {
            clearStatus();
            return null;
        }
        Class<?> testClass = null;
        try {
            Class testClass2 = loadSuiteClass(suiteClassName);
            Method suiteMethod = null;
            try {
                suiteMethod = testClass2.getMethod(SUITE_METHODNAME, new Class[0]);
                if (Modifier.isStatic(suiteMethod.getModifiers())) {
                    Test test = null;
                    try {
                        Test test2 = (Test) suiteMethod.invoke(null, new Object[0]);
                        if (test2 == null) {
                            return test2;
                        }
                        clearStatus();
                        return test2;
                    } catch (InvocationTargetException e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to invoke suite():");
                        stringBuilder.append(e.getTargetException().toString());
                        runFailed(stringBuilder.toString());
                        return null;
                    } catch (IllegalAccessException e2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to invoke suite():");
                        stringBuilder.append(e2.toString());
                        runFailed(stringBuilder.toString());
                        return null;
                    }
                }
                runFailed("Suite() method must be static");
                return null;
            } catch (Exception e3) {
                clearStatus();
                return new TestSuite(testClass2);
            }
        } catch (ClassNotFoundException e4) {
            String clazz = e4.getMessage();
            if (clazz == null) {
                clazz = suiteClassName;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Class not found \"");
            stringBuilder2.append(clazz);
            stringBuilder2.append("\"");
            runFailed(stringBuilder2.toString());
            return null;
        } catch (Exception e5) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Error: ");
            stringBuilder3.append(e5.toString());
            runFailed(stringBuilder3.toString());
            return null;
        }
    }

    public String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format(((double) runTime) / 1000.0d);
    }

    protected String processArguments(String[] args) {
        String suiteName = null;
        int i = 0;
        while (i < args.length) {
            if (args[i].equals("-noloading")) {
                setLoading(false);
            } else if (args[i].equals("-nofilterstack")) {
                fgFilterStack = false;
            } else if (args[i].equals("-c")) {
                if (args.length > i + 1) {
                    suiteName = extractClassName(args[i + 1]);
                } else {
                    System.out.println("Missing Test class name");
                }
                i++;
            } else {
                suiteName = args[i];
            }
            i++;
        }
        return suiteName;
    }

    public void setLoading(boolean enable) {
        this.fLoading = enable;
    }

    public String extractClassName(String className) {
        if (className.startsWith("Default package for")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }

    public static String truncate(String s) {
        if (fgMaxMessageLength == -1 || s.length() <= fgMaxMessageLength) {
            return s;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(s.substring(0, fgMaxMessageLength));
        stringBuilder.append("...");
        return stringBuilder.toString();
    }

    protected Class<?> loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
        return Class.forName(suiteClassName);
    }

    protected void clearStatus() {
    }

    protected boolean useReloadingTestSuiteLoader() {
        return getPreference("loading").equals("true") && this.fLoading;
    }

    private static File getPreferencesFile() {
        return new File(System.getProperty("user.home"), "junit.properties");
    }

    private static void readPreferences() {
        InputStream is = null;
        try {
            is = new FileInputStream(getPreferencesFile());
            setPreferences(new Properties(getPreferences()));
            getPreferences().load(is);
            try {
                is.close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            if (is != null) {
                is.close();
            }
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e3) {
                }
            }
        }
    }

    public static String getPreference(String key) {
        return getPreferences().getProperty(key);
    }

    public static int getPreference(String key, int dflt) {
        String value = getPreference(key);
        int intValue = dflt;
        if (value == null) {
            return intValue;
        }
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }
        return intValue;
    }

    public static String getFilteredTrace(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return getFilteredTrace(stringWriter.toString());
    }

    public static String getFilteredTrace(String stack) {
        if (showStackRaw()) {
            return stack;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        BufferedReader br = new BufferedReader(new StringReader(stack));
        while (true) {
            try {
                String readLine = br.readLine();
                String line = readLine;
                if (readLine == null) {
                    return sw.toString();
                }
                if (!filterLine(line)) {
                    pw.println(line);
                }
            } catch (Exception e) {
                return stack;
            }
        }
    }

    protected static boolean showStackRaw() {
        return (getPreference("filterstack").equals("true") && fgFilterStack) ? false : true;
    }

    static boolean filterLine(String line) {
        String[] patterns = new String[]{"junit.framework.TestCase", "junit.framework.TestResult", "junit.framework.TestSuite", "junit.framework.Assert.", "junit.swingui.TestRunner", "junit.awtui.TestRunner", "junit.textui.TestRunner", "java.lang.reflect.Method.invoke("};
        for (String indexOf : patterns) {
            if (line.indexOf(indexOf) > 0) {
                return true;
            }
        }
        return false;
    }
}
