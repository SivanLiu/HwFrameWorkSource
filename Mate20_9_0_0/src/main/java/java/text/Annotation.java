package java.text;

public class Annotation {
    private Object value;

    public Annotation(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[value=");
        stringBuilder.append(this.value);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
