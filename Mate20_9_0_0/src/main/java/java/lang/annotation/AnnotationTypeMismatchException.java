package java.lang.annotation;

import java.lang.reflect.Method;

public class AnnotationTypeMismatchException extends RuntimeException {
    private static final long serialVersionUID = 8125925355765570191L;
    private final Method element;
    private final String foundType;

    public AnnotationTypeMismatchException(Method element, String foundType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Incorrectly typed data found for annotation element ");
        stringBuilder.append((Object) element);
        stringBuilder.append(" (Found data of type ");
        stringBuilder.append(foundType);
        stringBuilder.append(")");
        super(stringBuilder.toString());
        this.element = element;
        this.foundType = foundType;
    }

    public Method element() {
        return this.element;
    }

    public String foundType() {
        return this.foundType;
    }
}
