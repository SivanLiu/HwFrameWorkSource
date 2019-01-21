package android.util;

public abstract class Singleton<T> {
    private T mInstance;

    protected abstract T create();

    public final T get() {
        Object obj;
        synchronized (this) {
            if (this.mInstance == null) {
                this.mInstance = create();
            }
            obj = this.mInstance;
        }
        return obj;
    }
}
