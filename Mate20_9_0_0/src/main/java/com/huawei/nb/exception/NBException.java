package com.huawei.nb.exception;

public class NBException extends RuntimeException {
    public NBException(String message) {
        super(message);
    }

    public NBException(Throwable cause) {
        super(cause);
    }
}
