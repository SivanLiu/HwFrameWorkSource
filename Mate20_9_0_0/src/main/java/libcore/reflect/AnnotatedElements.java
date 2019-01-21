package libcore.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public final class AnnotatedElements {
    public static <T extends Annotation> T[] getDirectOrIndirectAnnotationsByType(AnnotatedElement element, Class<T> annotationClass) {
        if (annotationClass != null) {
            Annotation[] annotations = element.getDeclaredAnnotations();
            ArrayList<T> unfoldedAnnotations = new ArrayList();
            Class<? extends Annotation> repeatableAnnotationClass = getRepeatableAnnotationContainerClassFor(annotationClass);
            int i = 0;
            while (i < annotations.length) {
                if (annotationClass.isInstance(annotations[i])) {
                    unfoldedAnnotations.add(annotations[i]);
                } else if (repeatableAnnotationClass != null && repeatableAnnotationClass.isInstance(annotations[i])) {
                    insertAnnotationValues(annotations[i], annotationClass, unfoldedAnnotations);
                }
                i++;
            }
            return (Annotation[]) unfoldedAnnotations.toArray((Annotation[]) Array.newInstance(annotationClass, 0));
        }
        throw new NullPointerException("annotationClass");
    }

    private static <T extends Annotation> void insertAnnotationValues(Annotation annotation, Class<T> annotationClass, ArrayList<T> unfoldedAnnotations) {
        int i = 0;
        Class<T[]> annotationArrayClass = ((Annotation[]) Array.newInstance(annotationClass, 0)).getClass();
        StringBuilder stringBuilder;
        try {
            Method valuesMethod = annotation.getClass().getDeclaredMethod("value", new Class[0]);
            if (!valuesMethod.getReturnType().isArray()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("annotation container = ");
                stringBuilder.append(annotation);
                stringBuilder.append("annotation element class = ");
                stringBuilder.append(annotationClass);
                stringBuilder.append("; value() doesn't return array");
                throw new AssertionError(stringBuilder.toString());
            } else if (annotationClass.equals(valuesMethod.getReturnType().getComponentType())) {
                try {
                    Annotation[] nestedAnnotations = (Annotation[]) valuesMethod.invoke(annotation, new Object[0]);
                    while (i < nestedAnnotations.length) {
                        unfoldedAnnotations.add(nestedAnnotations[i]);
                        i++;
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new AssertionError(e);
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("annotation container = ");
                stringBuilder.append(annotation);
                stringBuilder.append("annotation element class = ");
                stringBuilder.append(annotationClass);
                stringBuilder.append("; value() returns incorrect type");
                throw new AssertionError(stringBuilder.toString());
            }
        } catch (NoSuchMethodException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("annotation container = ");
            stringBuilder.append(annotation);
            stringBuilder.append("annotation element class = ");
            stringBuilder.append(annotationClass);
            stringBuilder.append("; missing value() method");
            throw new AssertionError(stringBuilder.toString());
        } catch (SecurityException e3) {
            throw new IncompleteAnnotationException(annotation.getClass(), "value");
        }
    }

    private static <T extends Annotation> Class<? extends Annotation> getRepeatableAnnotationContainerClassFor(Class<T> annotationClass) {
        Repeatable repeatableAnnotation = (Repeatable) annotationClass.getDeclaredAnnotation(Repeatable.class);
        return repeatableAnnotation == null ? null : repeatableAnnotation.value();
    }

    private AnnotatedElements() {
    }
}
