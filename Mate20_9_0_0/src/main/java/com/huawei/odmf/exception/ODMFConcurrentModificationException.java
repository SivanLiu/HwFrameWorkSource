package com.huawei.odmf.exception;

public class ODMFConcurrentModificationException extends ODMFException {
    public ODMFConcurrentModificationException(String message) {
        super(message);
    }

    public ODMFConcurrentModificationException(Throwable cause) {
        super(cause);
    }
}
