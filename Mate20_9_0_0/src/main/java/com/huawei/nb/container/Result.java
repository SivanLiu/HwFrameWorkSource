package com.huawei.nb.container;

public abstract class Result<T> implements Container<T> {
    public abstract boolean exist();

    public abstract <T> T get();
}
