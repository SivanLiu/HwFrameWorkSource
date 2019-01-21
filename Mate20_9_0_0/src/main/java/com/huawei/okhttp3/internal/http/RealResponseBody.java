package com.huawei.okhttp3.internal.http;

import com.huawei.okhttp3.Headers;
import com.huawei.okhttp3.MediaType;
import com.huawei.okhttp3.ResponseBody;
import com.huawei.okio.BufferedSource;

public final class RealResponseBody extends ResponseBody {
    private final Headers headers;
    private final BufferedSource source;

    public RealResponseBody(Headers headers, BufferedSource source) {
        this.headers = headers;
        this.source = source;
    }

    public MediaType contentType() {
        String contentType = this.headers.get("Content-Type");
        return contentType != null ? MediaType.parse(contentType) : null;
    }

    public long contentLength() {
        return HttpHeaders.contentLength(this.headers);
    }

    public BufferedSource source() {
        return this.source;
    }
}
