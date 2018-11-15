package android.test;

import android.app.Instrumentation;
import android.content.Context;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.runner.BaseTestRunner;

@Deprecated
public class AndroidTestRunner extends BaseTestRunner {
    private Context mContext;
    private Instrumentation mInstrumentation;
    private boolean mSkipExecution = false;
    private List<TestCase> mTestCases;
    private String mTestClassName;
    private List<TestListener> mTestListeners = new ArrayList();
    private TestResult mTestResult;

    public void setTestClassName(String testClassName, String testMethodName) {
        Class testClass = loadTestClass(testClassName);
        if (shouldRunSingleTestMethod(testMethodName, testClass)) {
            TestCase testCase = buildSingleTestMethod(testClass, testMethodName);
            this.mTestCases = new ArrayList();
            this.mTestCases.add(testCase);
            this.mTestClassName = testClass.getSimpleName();
            return;
        }
        setTest(getTest(testClass), testClass);
    }

    public void setTest(Test test) {
        setTest(test, test.getClass());
    }

    private void setTest(Test test, Class<? extends Test> testClass) {
        this.mTestCases = TestCaseUtil.getTests(test, true);
        if (TestSuite.class.isAssignableFrom(testClass)) {
            this.mTestClassName = TestCaseUtil.getTestName(test);
        } else {
            this.mTestClassName = testClass.getSimpleName();
        }
    }

    public void clearTestListeners() {
        this.mTestListeners.clear();
    }

    public void addTestListener(TestListener testListener) {
        if (testListener != null) {
            this.mTestListeners.add(testListener);
        }
    }

    private Class<? extends Test> loadTestClass(String testClassName) {
        try {
            return this.mContext.getClassLoader().loadClass(testClassName);
        } catch (ClassNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not find test class. Class: ");
            stringBuilder.append(testClassName);
            runFailed(stringBuilder.toString());
            return null;
        }
    }

    private TestCase buildSingleTestMethod(Class testClass, String testMethodName) {
        try {
            return newSingleTestMethod(testClass, testMethodName, testClass.getConstructor(new Class[0]), new Object[0]);
        } catch (NoSuchMethodException e) {
            try {
                return newSingleTestMethod(testClass, testMethodName, testClass.getConstructor(new Class[]{String.class}), testMethodName);
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }

    private TestCase newSingleTestMethod(Class testClass, String testMethodName, Constructor constructor, Object... args) {
        StringBuilder stringBuilder;
        try {
            TestCase testCase = (TestCase) constructor.newInstance(args);
            testCase.setName(testMethodName);
            return testCase;
        } catch (IllegalAccessException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not access test class. Class: ");
            stringBuilder.append(testClass.getName());
            runFailed(stringBuilder.toString());
            return null;
        } catch (InstantiationException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not instantiate test class. Class: ");
            stringBuilder.append(testClass.getName());
            runFailed(stringBuilder.toString());
            return null;
        } catch (IllegalArgumentException e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal argument passed to constructor. Class: ");
            stringBuilder.append(testClass.getName());
            runFailed(stringBuilder.toString());
            return null;
        } catch (InvocationTargetException e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Constructor thew an exception. Class: ");
            stringBuilder.append(testClass.getName());
            runFailed(stringBuilder.toString());
            return null;
        }
    }

    private boolean shouldRunSingleTestMethod(String testMethodName, Class<? extends Test> testClass) {
        return testMethodName != null && TestCase.class.isAssignableFrom(testClass);
    }

    private Test getTest(Class clazz) {
        StringBuilder stringBuilder;
        if (TestSuiteProvider.class.isAssignableFrom(clazz)) {
            try {
                return ((TestSuiteProvider) clazz.getConstructor(new Class[0]).newInstance(new Object[0])).getTestSuite();
            } catch (InstantiationException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not instantiate test suite provider. Class: ");
                stringBuilder.append(clazz.getName());
                runFailed(stringBuilder.toString());
            } catch (IllegalAccessException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal access of test suite provider. Class: ");
                stringBuilder.append(clazz.getName());
                runFailed(stringBuilder.toString());
            } catch (InvocationTargetException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invocation exception test suite provider. Class: ");
                stringBuilder.append(clazz.getName());
                runFailed(stringBuilder.toString());
            } catch (NoSuchMethodException e4) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("No such method on test suite provider. Class: ");
                stringBuilder.append(clazz.getName());
                runFailed(stringBuilder.toString());
            }
        }
        return getTest(clazz.getName());
    }

    protected TestResult createTestResult() {
        if (this.mSkipExecution) {
            return new NoExecTestResult();
        }
        return new TestResult();
    }

    void setSkipExecution(boolean skip) {
        this.mSkipExecution = skip;
    }

    public List<TestCase> getTestCases() {
        return this.mTestCases;
    }

    public String getTestClassName() {
        return this.mTestClassName;
    }

    public TestResult getTestResult() {
        return this.mTestResult;
    }

    public void runTest() {
        runTest(createTestResult());
    }

    public void runTest(TestResult testResult) {
        this.mTestResult = testResult;
        for (TestListener testListener : this.mTestListeners) {
            this.mTestResult.addListener(testListener);
        }
        Context testContext = this.mInstrumentation == null ? this.mContext : this.mInstrumentation.getContext();
        for (TestCase testCase : this.mTestCases) {
            setContextIfAndroidTestCase(testCase, this.mContext, testContext);
            setInstrumentationIfInstrumentationTestCase(testCase, this.mInstrumentation);
            testCase.run(this.mTestResult);
        }
    }

    private void setContextIfAndroidTestCase(Test test, Context context, Context testContext) {
        if (AndroidTestCase.class.isAssignableFrom(test.getClass())) {
            ((AndroidTestCase) test).setContext(context);
            ((AndroidTestCase) test).setTestContext(testContext);
        }
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    private void setInstrumentationIfInstrumentationTestCase(Test test, Instrumentation instrumentation) {
        if (InstrumentationTestCase.class.isAssignableFrom(test.getClass())) {
            ((InstrumentationTestCase) test).injectInstrumentation(instrumentation);
        }
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.mInstrumentation = instrumentation;
    }

    @Deprecated
    public void setInstrumentaiton(Instrumentation instrumentation) {
        setInstrumentation(instrumentation);
    }

    protected Class loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
        return this.mContext.getClassLoader().loadClass(suiteClassName);
    }

    public void testStarted(String testName) {
    }

    public void testEnded(String testName) {
    }

    public void testFailed(int status, Test test, Throwable t) {
    }

    protected void runFailed(String message) {
        throw new RuntimeException(message);
    }
}
