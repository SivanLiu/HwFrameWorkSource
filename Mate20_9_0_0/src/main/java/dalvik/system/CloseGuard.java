package dalvik.system;

public final class CloseGuard {
    private static volatile Tracker currentTracker = null;
    private static volatile Reporter reporter = new DefaultReporter();
    private static volatile boolean stackAndTrackingEnabled = true;
    private Object closerNameOrAllocationInfo;

    public interface Reporter {
        void report(String str, Throwable th);
    }

    public interface Tracker {
        void close(Throwable th);

        void open(Throwable th);
    }

    private static final class DefaultReporter implements Reporter {
        private DefaultReporter() {
        }

        public void report(String message, Throwable allocationSite) {
            System.logW(message, allocationSite);
        }
    }

    public static CloseGuard get() {
        return new CloseGuard();
    }

    public static void setEnabled(boolean enabled) {
        stackAndTrackingEnabled = enabled;
    }

    public static boolean isEnabled() {
        return stackAndTrackingEnabled;
    }

    public static void setReporter(Reporter rep) {
        if (rep != null) {
            reporter = rep;
            return;
        }
        throw new NullPointerException("reporter == null");
    }

    public static Reporter getReporter() {
        return reporter;
    }

    public static void setTracker(Tracker tracker) {
        currentTracker = tracker;
    }

    public static Tracker getTracker() {
        return currentTracker;
    }

    private CloseGuard() {
    }

    public void open(String closer) {
        if (closer == null) {
            throw new NullPointerException("closer == null");
        } else if (stackAndTrackingEnabled) {
            String message = new StringBuilder();
            message.append("Explicit termination method '");
            message.append(closer);
            message.append("' not called");
            Throwable stack = new Throwable(message.toString());
            this.closerNameOrAllocationInfo = stack;
            Tracker tracker = currentTracker;
            if (tracker != null) {
                tracker.open(stack);
            }
        } else {
            this.closerNameOrAllocationInfo = closer;
        }
    }

    public void close() {
        Tracker tracker = currentTracker;
        if (tracker != null && (this.closerNameOrAllocationInfo instanceof Throwable)) {
            tracker.close((Throwable) this.closerNameOrAllocationInfo);
        }
        this.closerNameOrAllocationInfo = null;
    }

    public void warnIfOpen() {
        if (this.closerNameOrAllocationInfo == null) {
            return;
        }
        if (this.closerNameOrAllocationInfo instanceof String) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("A resource failed to call ");
            stringBuilder.append((String) this.closerNameOrAllocationInfo);
            stringBuilder.append(". ");
            System.logW(stringBuilder.toString());
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("A resource was acquired at attached stack trace but never released. ");
        stringBuilder2.append("See java.io.Closeable for information on avoiding resource leaks.");
        reporter.report(stringBuilder2.toString(), this.closerNameOrAllocationInfo);
    }
}
