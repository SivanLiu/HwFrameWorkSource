package com.android.uiautomator.testrunner;

import android.test.InstrumentationTestRunner;
import com.android.uiautomator.testrunner.TestCaseCollector.TestCaseFilter;
import java.lang.reflect.Method;

public class UiAutomatorTestCaseFilter implements TestCaseFilter {
    public boolean accept(Method method) {
        if (method.getParameterTypes().length == 0 && method.getName().startsWith(InstrumentationTestRunner.REPORT_KEY_NAME_TEST)) {
            return method.getReturnType().getSimpleName().equals("void");
        }
        return false;
    }

    public boolean accept(Class<?> clazz) {
        return UiAutomatorTestCase.class.isAssignableFrom(clazz);
    }
}
