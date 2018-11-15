package org.junit.runners.parameterized;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class BlockJUnit4ClassRunnerWithParameters extends BlockJUnit4ClassRunner {
    private final String name;
    private final Object[] parameters;

    public BlockJUnit4ClassRunnerWithParameters(TestWithParameters test) throws InitializationError {
        super(test.getTestClass().getJavaClass());
        this.parameters = test.getParameters().toArray(new Object[test.getParameters().size()]);
        this.name = test.getName();
    }

    public Object createTest() throws Exception {
        if (fieldsAreAnnotated()) {
            return createTestUsingFieldInjection();
        }
        return createTestUsingConstructorInjection();
    }

    private Object createTestUsingConstructorInjection() throws Exception {
        return getTestClass().getOnlyConstructor().newInstance(this.parameters);
    }

    private Object createTestUsingFieldInjection() throws Exception {
        List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
        if (annotatedFieldsByParameter.size() == this.parameters.length) {
            Object testClassInstance = getTestClass().getJavaClass().newInstance();
            for (FrameworkField each : annotatedFieldsByParameter) {
                Field field = each.getField();
                int index = ((Parameter) field.getAnnotation(Parameter.class)).value();
                try {
                    field.set(testClassInstance, this.parameters[index]);
                } catch (IllegalArgumentException iare) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(getTestClass().getName());
                    stringBuilder.append(": Trying to set ");
                    stringBuilder.append(field.getName());
                    stringBuilder.append(" with the value ");
                    stringBuilder.append(this.parameters[index]);
                    stringBuilder.append(" that is not the right type (");
                    stringBuilder.append(this.parameters[index].getClass().getSimpleName());
                    stringBuilder.append(" instead of ");
                    stringBuilder.append(field.getType().getSimpleName());
                    stringBuilder.append(").");
                    throw new Exception(stringBuilder.toString(), iare);
                }
            }
            return testClassInstance;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Wrong number of parameters and @Parameter fields. @Parameter fields counted: ");
        stringBuilder2.append(annotatedFieldsByParameter.size());
        stringBuilder2.append(", available parameters: ");
        stringBuilder2.append(this.parameters.length);
        stringBuilder2.append(".");
        throw new Exception(stringBuilder2.toString());
    }

    protected String getName() {
        return this.name;
    }

    protected String testName(FrameworkMethod method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(method.getName());
        stringBuilder.append(getName());
        return stringBuilder.toString();
    }

    protected void validateConstructor(List<Throwable> errors) {
        validateOnlyOneConstructor(errors);
        if (fieldsAreAnnotated()) {
            validateZeroArgConstructor(errors);
        }
    }

    protected void validateFields(List<Throwable> errors) {
        super.validateFields(errors);
        if (fieldsAreAnnotated()) {
            List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
            int[] usedIndices = new int[annotatedFieldsByParameter.size()];
            for (FrameworkField each : annotatedFieldsByParameter) {
                int index = ((Parameter) each.getField().getAnnotation(Parameter.class)).value();
                if (index < 0 || index > annotatedFieldsByParameter.size() - 1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid @Parameter value: ");
                    stringBuilder.append(index);
                    stringBuilder.append(". @Parameter fields counted: ");
                    stringBuilder.append(annotatedFieldsByParameter.size());
                    stringBuilder.append(". Please use an index between 0 and ");
                    stringBuilder.append(annotatedFieldsByParameter.size() - 1);
                    stringBuilder.append(".");
                    errors.add(new Exception(stringBuilder.toString()));
                } else {
                    usedIndices[index] = usedIndices[index] + 1;
                }
            }
            for (int index2 = 0; index2 < usedIndices.length; index2++) {
                int numberOfUse = usedIndices[index2];
                StringBuilder stringBuilder2;
                if (numberOfUse == 0) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("@Parameter(");
                    stringBuilder2.append(index2);
                    stringBuilder2.append(") is never used.");
                    errors.add(new Exception(stringBuilder2.toString()));
                } else if (numberOfUse > 1) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("@Parameter(");
                    stringBuilder2.append(index2);
                    stringBuilder2.append(") is used more than once (");
                    stringBuilder2.append(numberOfUse);
                    stringBuilder2.append(").");
                    errors.add(new Exception(stringBuilder2.toString()));
                }
            }
        }
    }

    protected Statement classBlock(RunNotifier notifier) {
        return childrenInvoker(notifier);
    }

    protected Annotation[] getRunnerAnnotations() {
        return new Annotation[0];
    }

    private List<FrameworkField> getAnnotatedFieldsByParameter() {
        return getTestClass().getAnnotatedFields(Parameter.class);
    }

    private boolean fieldsAreAnnotated() {
        return getAnnotatedFieldsByParameter().isEmpty() ^ 1;
    }
}
