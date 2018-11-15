package org.junit.internal.runners;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import junit.framework.Test;
import junit.runner.BaseTestRunner;

public class SuiteMethod extends JUnit38ClassRunner {
    public SuiteMethod(Class<?> klass) throws Throwable {
        super(testFromSuiteMethod(klass));
    }

    public static Test testFromSuiteMethod(Class<?> klass) throws Throwable {
        Test suite = null;
        try {
            Method suiteMethod = klass.getMethod(BaseTestRunner.SUITE_METHODNAME, new Class[0]);
            if (Modifier.isStatic(suiteMethod.getModifiers())) {
                return (Test) suiteMethod.invoke(null, new Object[0]);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(klass.getName());
            stringBuilder.append(".suite() must be static");
            throw new Exception(stringBuilder.toString());
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
