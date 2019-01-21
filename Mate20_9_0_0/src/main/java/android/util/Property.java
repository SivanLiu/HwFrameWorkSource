package android.util;

public abstract class Property<T, V> {
    private final String mName;
    private final Class<V> mType;

    public abstract V get(T t);

    public static <T, V> Property<T, V> of(Class<T> hostType, Class<V> valueType, String name) {
        return new ReflectiveProperty(hostType, valueType, name);
    }

    public Property(Class<V> type, String name) {
        this.mName = name;
        this.mType = type;
    }

    public boolean isReadOnly() {
        return false;
    }

    public void set(T t, V v) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Property ");
        stringBuilder.append(getName());
        stringBuilder.append(" is read-only");
        throw new UnsupportedOperationException(stringBuilder.toString());
    }

    public String getName() {
        return this.mName;
    }

    public Class<V> getType() {
        return this.mType;
    }
}
