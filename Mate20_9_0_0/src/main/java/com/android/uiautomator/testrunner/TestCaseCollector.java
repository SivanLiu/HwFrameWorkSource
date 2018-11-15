package com.android.uiautomator.testrunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

public class TestCaseCollector {
    private ClassLoader mClassLoader;
    private TestCaseFilter mFilter;
    private List<TestCase> mTestCases = new ArrayList();

    public interface TestCaseFilter {
        boolean accept(Class<?> cls);

        boolean accept(Method method);
    }

    public TestCaseCollector(ClassLoader classLoader, TestCaseFilter filter) {
        this.mClassLoader = classLoader;
        this.mFilter = filter;
    }

    public void addTestClasses(List<String> classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            addTestClass(className);
        }
    }

    public void addTestClass(String className) throws ClassNotFoundException {
        int hashPos = className.indexOf(35);
        String methodName = null;
        if (hashPos != -1) {
            methodName = className.substring(hashPos + 1);
            className = className.substring(0, hashPos);
        }
        addTestClass(className, methodName);
    }

    public void addTestClass(String className, String methodName) throws ClassNotFoundException {
        Class<?> clazz = this.mClassLoader.loadClass(className);
        if (methodName != null) {
            addSingleTestMethod(clazz, methodName);
            return;
        }
        for (Method method : clazz.getMethods()) {
            if (this.mFilter.accept(method)) {
                addSingleTestMethod(clazz, method.getName());
            }
        }
    }

    public List<TestCase> getTestCases() {
        return Collections.unmodifiableList(this.mTestCases);
    }

    protected void addSingleTestMethod(Class<?> clazz, String method) {
        List list;
        StringBuilder stringBuilder;
        if (this.mFilter.accept((Class) clazz)) {
            try {
                TestCase testCase = (TestCase) clazz.newInstance();
                testCase.setName(method);
                this.mTestCases.add(testCase);
                return;
            } catch (InstantiationException e) {
                list = this.mTestCases;
                stringBuilder = new StringBuilder();
                stringBuilder.append("InstantiationException: could not instantiate test class. Class: ");
                stringBuilder.append(clazz.getName());
                list.add(error(clazz, stringBuilder.toString()));
                return;
            } catch (IllegalAccessException e2) {
                list = this.mTestCases;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IllegalAccessException: could not instantiate test class. Class: ");
                stringBuilder.append(clazz.getName());
                list.add(error(clazz, stringBuilder.toString()));
                return;
            }
        }
        throw new RuntimeException("Test class must be derived from UiAutomatorTestCase");
    }

    private UiAutomatorTestCase error(Class<?> clazz, final String message) {
        UiAutomatorTestCase warning = new UiAutomatorTestCase() {
            protected void runTest() {
                TestCase.fail(message);
            }
        };
        warning.setName(clazz.getName());
        return warning;
    }
}
