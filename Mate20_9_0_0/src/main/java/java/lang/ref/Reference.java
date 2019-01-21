package java.lang.ref;

public abstract class Reference<T> {
    private static boolean disableIntrinsic = false;
    private static boolean slowPathEnabled = false;
    Reference<?> pendingNext;
    final ReferenceQueue<? super T> queue;
    Reference queueNext;
    volatile T referent;

    private static class SinkHolder {
        private static volatile int finalize_count = 0;
        static volatile Object sink;
        private static Object sinkUser = new Object() {
            protected void finalize() {
                if (SinkHolder.sink != null || SinkHolder.finalize_count <= 0) {
                    SinkHolder.access$008();
                    return;
                }
                throw new AssertionError((Object) "Can't get here");
            }
        };

        private SinkHolder() {
        }

        static /* synthetic */ int access$008() {
            int i = finalize_count;
            finalize_count = i + 1;
            return i;
        }
    }

    private final native T getReferent();

    native void clearReferent();

    public T get() {
        return getReferent();
    }

    public void clear() {
        clearReferent();
    }

    public boolean isEnqueued() {
        return this.queue != null && this.queue.isEnqueued(this);
    }

    public boolean enqueue() {
        return this.queue != null && this.queue.enqueue(this);
    }

    Reference(T referent) {
        this(referent, null);
    }

    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
        this.queue = queue;
    }

    public static void reachabilityFence(Object ref) {
        SinkHolder.sink = ref;
        if (SinkHolder.finalize_count == 0) {
            SinkHolder.sink = null;
        }
    }
}
