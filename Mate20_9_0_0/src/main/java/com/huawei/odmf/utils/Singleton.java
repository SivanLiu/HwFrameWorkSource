package com.huawei.odmf.utils;

public abstract class Singleton<T> {
    private volatile T mInstance;

    protected abstract T create();

    public final T get() {
        if (this.mInstance != null) {
            return this.mInstance;
        }
        T t;
        synchronized (this) {
            if (this.mInstance == null) {
                this.mInstance = create();
            }
            t = this.mInstance;
        }
        return t;
    }
}
