package com.huawei.okhttp3;

import com.huawei.okhttp3.internal.Util;
import com.huawei.okio.Buffer;
import com.huawei.okio.BufferedSink;
import com.huawei.okio.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MultipartBody extends RequestBody {
    public static final MediaType ALTERNATIVE = MediaType.parse("multipart/alternative");
    private static final byte[] COLONSPACE = new byte[]{(byte) 58, (byte) 32};
    private static final byte[] CRLF = new byte[]{(byte) 13, (byte) 10};
    private static final byte[] DASHDASH = new byte[]{(byte) 45, (byte) 45};
    public static final MediaType DIGEST = MediaType.parse("multipart/digest");
    public static final MediaType FORM = MediaType.parse("multipart/form-data");
    public static final MediaType MIXED = MediaType.parse("multipart/mixed");
    public static final MediaType PARALLEL = MediaType.parse("multipart/parallel");
    private final ByteString boundary;
    private long contentLength = -1;
    private final MediaType contentType;
    private final MediaType originalType;
    private final List<Part> parts;

    public static final class Builder {
        private final ByteString boundary;
        private final List<Part> parts;
        private MediaType type;

        public Builder() {
            this(UUID.randomUUID().toString());
        }

        public Builder(String boundary) {
            this.type = MultipartBody.MIXED;
            this.parts = new ArrayList();
            this.boundary = ByteString.encodeUtf8(boundary);
        }

        public Builder setType(MediaType type) {
            if (type == null) {
                throw new NullPointerException("type == null");
            } else if (type.type().equals("multipart")) {
                this.type = type;
                return this;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("multipart != ");
                stringBuilder.append(type);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public Builder addPart(RequestBody body) {
            return addPart(Part.create(body));
        }

        public Builder addPart(Headers headers, RequestBody body) {
            return addPart(Part.create(headers, body));
        }

        public Builder addFormDataPart(String name, String value) {
            return addPart(Part.createFormData(name, value));
        }

        public Builder addFormDataPart(String name, String filename, RequestBody body) {
            return addPart(Part.createFormData(name, filename, body));
        }

        public Builder addPart(Part part) {
            if (part != null) {
                this.parts.add(part);
                return this;
            }
            throw new NullPointerException("part == null");
        }

        public MultipartBody build() {
            if (!this.parts.isEmpty()) {
                return new MultipartBody(this.boundary, this.type, this.parts);
            }
            throw new IllegalStateException("Multipart body must have at least one part.");
        }
    }

    public static final class Part {
        final RequestBody body;
        final Headers headers;

        public static Part create(RequestBody body) {
            return create(null, body);
        }

        public static Part create(Headers headers, RequestBody body) {
            if (body == null) {
                throw new NullPointerException("body == null");
            } else if (headers != null && headers.get("Content-Type") != null) {
                throw new IllegalArgumentException("Unexpected header: Content-Type");
            } else if (headers == null || headers.get("Content-Length") == null) {
                return new Part(headers, body);
            } else {
                throw new IllegalArgumentException("Unexpected header: Content-Length");
            }
        }

        public static Part createFormData(String name, String value) {
            return createFormData(name, null, RequestBody.create(null, value));
        }

        public static Part createFormData(String name, String filename, RequestBody body) {
            if (name != null) {
                StringBuilder disposition = new StringBuilder("form-data; name=");
                MultipartBody.appendQuotedString(disposition, name);
                if (filename != null) {
                    disposition.append("; filename=");
                    MultipartBody.appendQuotedString(disposition, filename);
                }
                return create(Headers.of("Content-Disposition", disposition.toString()), body);
            }
            throw new NullPointerException("name == null");
        }

        private Part(Headers headers, RequestBody body) {
            this.headers = headers;
            this.body = body;
        }

        public Headers headers() {
            return this.headers;
        }

        public RequestBody body() {
            return this.body;
        }
    }

    MultipartBody(ByteString boundary, MediaType type, List<Part> parts) {
        this.boundary = boundary;
        this.originalType = type;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        stringBuilder.append("; boundary=");
        stringBuilder.append(boundary.utf8());
        this.contentType = MediaType.parse(stringBuilder.toString());
        this.parts = Util.immutableList((List) parts);
    }

    public MediaType type() {
        return this.originalType;
    }

    public String boundary() {
        return this.boundary.utf8();
    }

    public int size() {
        return this.parts.size();
    }

    public List<Part> parts() {
        return this.parts;
    }

    public Part part(int index) {
        return (Part) this.parts.get(index);
    }

    public MediaType contentType() {
        return this.contentType;
    }

    public long contentLength() throws IOException {
        long result = this.contentLength;
        if (result != -1) {
            return result;
        }
        long writeOrCountBytes = writeOrCountBytes(null, true);
        this.contentLength = writeOrCountBytes;
        return writeOrCountBytes;
    }

    public void writeTo(BufferedSink sink) throws IOException {
        writeOrCountBytes(sink, false);
    }

    private long writeOrCountBytes(BufferedSink sink, boolean countBytes) throws IOException {
        BufferedSink sink2;
        long byteCount = 0;
        Buffer byteCountBuffer = null;
        if (countBytes) {
            sink2 = new Buffer();
            byteCountBuffer = sink2;
        } else {
            sink2 = sink;
        }
        int partCount = this.parts.size();
        for (int p = 0; p < partCount; p++) {
            Part part = (Part) this.parts.get(p);
            Headers headers = part.headers;
            RequestBody body = part.body;
            sink2.write(DASHDASH);
            sink2.write(this.boundary);
            sink2.write(CRLF);
            if (headers != null) {
                int headerCount = headers.size();
                for (int h = 0; h < headerCount; h++) {
                    sink2.writeUtf8(headers.name(h)).write(COLONSPACE).writeUtf8(headers.value(h)).write(CRLF);
                }
            }
            MediaType contentType = body.contentType();
            if (contentType != null) {
                sink2.writeUtf8("Content-Type: ").writeUtf8(contentType.toString()).write(CRLF);
            }
            long contentLength = body.contentLength();
            if (contentLength != -1) {
                sink2.writeUtf8("Content-Length: ").writeDecimalLong(contentLength).write(CRLF);
            } else if (countBytes) {
                byteCountBuffer.clear();
                return -1;
            }
            sink2.write(CRLF);
            if (countBytes) {
                byteCount += contentLength;
            } else {
                body.writeTo(sink2);
            }
            sink2.write(CRLF);
        }
        sink2.write(DASHDASH);
        sink2.write(this.boundary);
        sink2.write(DASHDASH);
        sink2.write(CRLF);
        if (countBytes) {
            byteCount += byteCountBuffer.size();
            byteCountBuffer.clear();
        }
        return byteCount;
    }

    static StringBuilder appendQuotedString(StringBuilder target, String key) {
        target.append('\"');
        int len = key.length();
        for (int i = 0; i < len; i++) {
            char ch = key.charAt(i);
            if (ch == 10) {
                target.append("%0A");
            } else if (ch == 13) {
                target.append("%0D");
            } else if (ch != '\"') {
                target.append(ch);
            } else {
                target.append("%22");
            }
        }
        target.append('\"');
        return target;
    }
}
