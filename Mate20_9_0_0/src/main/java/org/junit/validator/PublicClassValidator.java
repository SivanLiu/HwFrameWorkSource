package org.junit.validator;

import java.util.Collections;
import java.util.List;
import org.junit.runners.model.TestClass;

public class PublicClassValidator implements TestClassValidator {
    private static final List<Exception> NO_VALIDATION_ERRORS = Collections.emptyList();

    public List<Exception> validateTestClass(TestClass testClass) {
        if (testClass.isPublic()) {
            return NO_VALIDATION_ERRORS;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The class ");
        stringBuilder.append(testClass.getName());
        stringBuilder.append(" is not public.");
        return Collections.singletonList(new Exception(stringBuilder.toString()));
    }
}
