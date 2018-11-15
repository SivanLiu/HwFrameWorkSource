package org.junit.validator;

import java.util.concurrent.ConcurrentHashMap;

public class AnnotationValidatorFactory {
    private static final ConcurrentHashMap<ValidateWith, AnnotationValidator> VALIDATORS_FOR_ANNOTATION_TYPES = new ConcurrentHashMap();

    public AnnotationValidator createAnnotationValidator(ValidateWith validateWithAnnotation) {
        AnnotationValidator validator = (AnnotationValidator) VALIDATORS_FOR_ANNOTATION_TYPES.get(validateWithAnnotation);
        if (validator != null) {
            return validator;
        }
        Class<? extends AnnotationValidator> clazz = validateWithAnnotation.value();
        if (clazz != null) {
            try {
                VALIDATORS_FOR_ANNOTATION_TYPES.putIfAbsent(validateWithAnnotation, (AnnotationValidator) clazz.newInstance());
                return (AnnotationValidator) VALIDATORS_FOR_ANNOTATION_TYPES.get(validateWithAnnotation);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception received when creating AnnotationValidator class ");
                stringBuilder.append(clazz.getName());
                throw new RuntimeException(stringBuilder.toString(), e);
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Can't create validator, value is null in annotation ");
        stringBuilder2.append(validateWithAnnotation.getClass().getName());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }
}
