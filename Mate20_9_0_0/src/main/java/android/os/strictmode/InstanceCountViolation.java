package android.os.strictmode;

public class InstanceCountViolation extends Violation {
    private static final StackTraceElement[] FAKE_STACK = new StackTraceElement[]{new StackTraceElement("android.os.StrictMode", "setClassInstanceLimit", "StrictMode.java", 1)};
    private final long mInstances;

    public InstanceCountViolation(Class klass, long instances, int limit) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(klass.toString());
        stringBuilder.append("; instances=");
        stringBuilder.append(instances);
        stringBuilder.append("; limit=");
        stringBuilder.append(limit);
        super(stringBuilder.toString());
        setStackTrace(FAKE_STACK);
        this.mInstances = instances;
    }

    public long getNumberOfInstances() {
        return this.mInstances;
    }
}
