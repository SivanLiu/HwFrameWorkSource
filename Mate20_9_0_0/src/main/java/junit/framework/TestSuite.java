package junit.framework;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.junit.internal.MethodSorter;

public class TestSuite implements Test {
    private String fName;
    private Vector<Test> fTests;

    public static Test createTest(Class<?> theClass, String name) {
        StringBuilder stringBuilder;
        try {
            Constructor<?> constructor = getTestConstructor(theClass);
            try {
                Object test;
                if (constructor.getParameterTypes().length == 0) {
                    test = constructor.newInstance(new Object[0]);
                    if (test instanceof TestCase) {
                        ((TestCase) test).setName(name);
                    }
                } else {
                    test = constructor.newInstance(new Object[]{name});
                }
                return (Test) test;
            } catch (InstantiationException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot instantiate test case: ");
                stringBuilder.append(name);
                stringBuilder.append(" (");
                stringBuilder.append(exceptionToString(e));
                stringBuilder.append(")");
                return warning(stringBuilder.toString());
            } catch (InvocationTargetException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in constructor: ");
                stringBuilder.append(name);
                stringBuilder.append(" (");
                stringBuilder.append(exceptionToString(e2.getTargetException()));
                stringBuilder.append(")");
                return warning(stringBuilder.toString());
            } catch (IllegalAccessException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot access test case: ");
                stringBuilder.append(name);
                stringBuilder.append(" (");
                stringBuilder.append(exceptionToString(e3));
                stringBuilder.append(")");
                return warning(stringBuilder.toString());
            }
        } catch (NoSuchMethodException e4) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Class ");
            stringBuilder2.append(theClass.getName());
            stringBuilder2.append(" has no public constructor TestCase(String name) or TestCase()");
            return warning(stringBuilder2.toString());
        }
    }

    public static Constructor<?> getTestConstructor(Class<?> theClass) throws NoSuchMethodException {
        try {
            return theClass.getConstructor(new Class[]{String.class});
        } catch (NoSuchMethodException e) {
            return theClass.getConstructor(new Class[0]);
        }
    }

    public static Test warning(final String message) {
        return new TestCase("warning") {
            protected void runTest() {
                TestCase.fail(message);
            }
        };
    }

    private static String exceptionToString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public TestSuite() {
        this.fTests = new Vector(10);
    }

    public TestSuite(Class<?> theClass) {
        this.fTests = new Vector(10);
        addTestsFromTestCase(theClass);
    }

    private void addTestsFromTestCase(Class<?> theClass) {
        this.fName = theClass.getName();
        try {
            getTestConstructor(theClass);
            if (Modifier.isPublic(theClass.getModifiers())) {
                List<String> names = new ArrayList();
                for (Class<?> superClass = theClass; Test.class.isAssignableFrom(superClass); superClass = superClass.getSuperclass()) {
                    for (Method each : MethodSorter.getDeclaredMethods(superClass)) {
                        addTestMethod(each, names, theClass);
                    }
                }
                if (this.fTests.size() == 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No tests found in ");
                    stringBuilder.append(theClass.getName());
                    addTest(warning(stringBuilder.toString()));
                }
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Class ");
            stringBuilder2.append(theClass.getName());
            stringBuilder2.append(" is not public");
            addTest(warning(stringBuilder2.toString()));
        } catch (NoSuchMethodException e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Class ");
            stringBuilder3.append(theClass.getName());
            stringBuilder3.append(" has no public constructor TestCase(String name) or TestCase()");
            addTest(warning(stringBuilder3.toString()));
        }
    }

    public TestSuite(Class<? extends TestCase> theClass, String name) {
        this((Class) theClass);
        setName(name);
    }

    public TestSuite(String name) {
        this.fTests = new Vector(10);
        setName(name);
    }

    public TestSuite(Class<?>... classes) {
        this.fTests = new Vector(10);
        for (Class<?> each : classes) {
            addTest(testCaseForClass(each));
        }
    }

    private Test testCaseForClass(Class<?> each) {
        if (TestCase.class.isAssignableFrom(each)) {
            return new TestSuite(each.asSubclass(TestCase.class));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(each.getCanonicalName());
        stringBuilder.append(" does not extend TestCase");
        return warning(stringBuilder.toString());
    }

    public TestSuite(Class<? extends TestCase>[] classes, String name) {
        this((Class[]) classes);
        setName(name);
    }

    public void addTest(Test test) {
        this.fTests.add(test);
    }

    public void addTestSuite(Class<? extends TestCase> testClass) {
        addTest(new TestSuite((Class) testClass));
    }

    public int countTestCases() {
        int count = 0;
        Iterator it = this.fTests.iterator();
        while (it.hasNext()) {
            count += ((Test) it.next()).countTestCases();
        }
        return count;
    }

    public String getName() {
        return this.fName;
    }

    public void run(TestResult result) {
        Iterator it = this.fTests.iterator();
        while (it.hasNext()) {
            Test each = (Test) it.next();
            if (!result.shouldStop()) {
                runTest(each, result);
            } else {
                return;
            }
        }
    }

    public void runTest(Test test, TestResult result) {
        test.run(result);
    }

    public void setName(String name) {
        this.fName = name;
    }

    public Test testAt(int index) {
        return (Test) this.fTests.get(index);
    }

    public int testCount() {
        return this.fTests.size();
    }

    public Enumeration<Test> tests() {
        return this.fTests.elements();
    }

    public String toString() {
        if (getName() != null) {
            return getName();
        }
        return super.toString();
    }

    private void addTestMethod(Method m, List<String> names, Class<?> theClass) {
        String name = m.getName();
        if (!names.contains(name)) {
            if (isPublicTestMethod(m)) {
                names.add(name);
                addTest(createTest(theClass, name));
                return;
            }
            if (isTestMethod(m)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Test method isn't public: ");
                stringBuilder.append(m.getName());
                stringBuilder.append("(");
                stringBuilder.append(theClass.getCanonicalName());
                stringBuilder.append(")");
                addTest(warning(stringBuilder.toString()));
            }
        }
    }

    private boolean isPublicTestMethod(Method m) {
        return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
    }

    private boolean isTestMethod(Method m) {
        return m.getParameterTypes().length == 0 && m.getName().startsWith("test") && m.getReturnType().equals(Void.TYPE);
    }
}
