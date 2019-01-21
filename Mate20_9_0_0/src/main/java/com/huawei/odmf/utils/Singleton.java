package com.huawei.odmf.utils;

public abstract class Singleton<T> {
    private volatile T mInstance;

    protected abstract T create();

    public final T get() {
        if (this.mInstance != null) {
            return this.mInstance;
        }
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
