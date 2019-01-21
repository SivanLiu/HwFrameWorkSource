package com.huawei.okhttp3.internal.http2;

import java.io.IOException;

public final class StreamResetException extends IOException {
    public final ErrorCode errorCode;

    public StreamResetException(ErrorCode errorCode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stream was reset: ");
        stringBuilder.append(errorCode);
        super(stringBuilder.toString());
        this.errorCode = errorCode;
    }
}
