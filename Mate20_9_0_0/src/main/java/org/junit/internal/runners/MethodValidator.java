package org.junit.internal.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Deprecated
public class MethodValidator {
    private final List<Throwable> errors = new ArrayList();
    private TestClass testClass;

    public MethodValidator(TestClass testClass) {
        this.testClass = testClass;
    }

    public void validateInstanceMethods() {
        validateTestMethods(After.class, false);
        validateTestMethods(Before.class, false);
        validateTestMethods(Test.class, false);
        if (this.testClass.getAnnotatedMethods(Test.class).size() == 0) {
            this.errors.add(new Exception("No runnable methods"));
        }
    }

    public void validateStaticMethods() {
        validateTestMethods(BeforeClass.class, true);
        validateTestMethods(AfterClass.class, true);
    }

    public List<Throwable> validateMethodsForDefaultRunner() {
        validateNoArgConstructor();
        validateStaticMethods();
        validateInstanceMethods();
        return this.errors;
    }

    public void assertValid() throws InitializationError {
        if (!this.errors.isEmpty()) {
            throw new InitializationError(this.errors);
        }
    }

    public void validateNoArgConstructor() {
        try {
            this.testClass.getConstructor();
        } catch (Exception e) {
            this.errors.add(new Exception("Test class should have public zero-argument constructor", e));
        }
    }

    private void validateTestMethods(Class<? extends Annotation> annotation, boolean isStatic) {
        for (Method each : this.testClass.getAnnotatedMethods(annotation)) {
            List list;
            StringBuilder stringBuilder;
            if (Modifier.isStatic(each.getModifiers()) != isStatic) {
                String state = isStatic ? "should" : "should not";
                List list2 = this.errors;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Method ");
                stringBuilder2.append(each.getName());
                stringBuilder2.append("() ");
                stringBuilder2.append(state);
                stringBuilder2.append(" be static");
                list2.add(new Exception(stringBuilder2.toString()));
            }
            if (!Modifier.isPublic(each.getDeclaringClass().getModifiers())) {
                list = this.errors;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Class ");
                stringBuilder.append(each.getDeclaringClass().getName());
                stringBuilder.append(" should be public");
                list.add(new Exception(stringBuilder.toString()));
            }
            if (!Modifier.isPublic(each.getModifiers())) {
                list = this.errors;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Method ");
                stringBuilder.append(each.getName());
                stringBuilder.append(" should be public");
                list.add(new Exception(stringBuilder.toString()));
            }
            if (each.getReturnType() != Void.TYPE) {
                list = this.errors;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Method ");
                stringBuilder.append(each.getName());
                stringBuilder.append(" should be void");
                list.add(new Exception(stringBuilder.toString()));
            }
            if (each.getParameterTypes().length != 0) {
                list = this.errors;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Method ");
                stringBuilder.append(each.getName());
                stringBuilder.append(" should have no parameters");
                list.add(new Exception(stringBuilder.toString()));
            }
        }
    }
}
