package org.bouncycastle.est;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.bouncycastle.util.Properties;
import org.bouncycastle.util.Strings;

public class ESTResponse {
    private static final Long ZERO = Long.valueOf(0);
    private String HttpVersion;
    private Long absoluteReadLimit;
    private Long contentLength;
    private final Headers headers;
    private InputStream inputStream;
    private final byte[] lineBuffer;
    private final ESTRequest originalRequest;
    private long read = 0;
    private final Source source;
    private int statusCode;
    private String statusMessage;

    private class PrintingInputStream extends InputStream {
        private final InputStream src;

        private PrintingInputStream(InputStream inputStream) {
            this.src = inputStream;
        }

        /* synthetic */ PrintingInputStream(ESTResponse eSTResponse, InputStream inputStream, AnonymousClass1 anonymousClass1) {
            this(inputStream);
        }

        public int available() throws IOException {
            return this.src.available();
        }

        public void close() throws IOException {
            this.src.close();
        }

        public int read() throws IOException {
            int read = this.src.read();
            System.out.print(String.valueOf((char) read));
            return read;
        }
    }

    public ESTResponse(ESTRequest eSTRequest, Source source) throws IOException {
        this.originalRequest = eSTRequest;
        this.source = source;
        if (source instanceof LimitedSource) {
            this.absoluteReadLimit = ((LimitedSource) source).getAbsoluteReadLimit();
        }
        Set asKeySet = Properties.asKeySet("org.bouncycastle.debug.est");
        InputStream printingInputStream = (asKeySet.contains("input") || asKeySet.contains("all")) ? new PrintingInputStream(this, source.getInputStream(), null) : source.getInputStream();
        this.inputStream = printingInputStream;
        this.headers = new Headers();
        this.lineBuffer = new byte[1024];
        process();
    }

    private void process() throws IOException {
        this.HttpVersion = readStringIncluding(' ');
        this.statusCode = Integer.parseInt(readStringIncluding(' '));
        this.statusMessage = readStringIncluding(10);
        while (true) {
            String readStringIncluding = readStringIncluding(10);
            if (readStringIncluding.length() <= 0) {
                break;
            }
            int indexOf = readStringIncluding.indexOf(58);
            if (indexOf > -1) {
                this.headers.add(Strings.toLowerCase(readStringIncluding.substring(0, indexOf).trim()), readStringIncluding.substring(indexOf + 1).trim());
            }
        }
        this.contentLength = getContentLength();
        if (this.statusCode == 204 || this.statusCode == 202) {
            if (this.contentLength == null) {
                this.contentLength = Long.valueOf(0);
            } else if (this.statusCode == 204 && this.contentLength.longValue() > 0) {
                throw new IOException("Got HTTP status 204 but Content-length > 0.");
            }
        }
        if (this.contentLength != null) {
            if (this.contentLength.equals(ZERO)) {
                this.inputStream = new InputStream() {
                    public int read() throws IOException {
                        return -1;
                    }
                };
            }
            if (this.contentLength != null) {
                StringBuilder stringBuilder;
                if (this.contentLength.longValue() < 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Server returned negative content length: ");
                    stringBuilder.append(this.absoluteReadLimit);
                    throw new IOException(stringBuilder.toString());
                } else if (this.absoluteReadLimit != null && this.contentLength.longValue() >= this.absoluteReadLimit.longValue()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Content length longer than absolute read limit: ");
                    stringBuilder.append(this.absoluteReadLimit);
                    stringBuilder.append(" Content-Length: ");
                    stringBuilder.append(this.contentLength);
                    throw new IOException(stringBuilder.toString());
                }
            }
            this.inputStream = wrapWithCounter(this.inputStream, this.absoluteReadLimit);
            if ("base64".equalsIgnoreCase(getHeader("content-transfer-encoding"))) {
                this.inputStream = new CTEBase64InputStream(this.inputStream, getContentLength());
                return;
            }
            return;
        }
        throw new IOException("No Content-length header.");
    }

    public void close() throws IOException {
        if (this.inputStream != null) {
            this.inputStream.close();
        }
        this.source.close();
    }

    public Long getContentLength() {
        String firstValue = this.headers.getFirstValue("Content-Length");
        if (firstValue == null) {
            return null;
        }
        try {
            return Long.valueOf(Long.parseLong(firstValue));
        } catch (RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Content Length: '");
            stringBuilder.append(firstValue);
            stringBuilder.append("' invalid. ");
            stringBuilder.append(e.getMessage());
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    public String getHeader(String str) {
        return this.headers.getFirstValue(str);
    }

    public Headers getHeaders() {
        return this.headers;
    }

    public String getHttpVersion() {
        return this.HttpVersion;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public ESTRequest getOriginalRequest() {
        return this.originalRequest;
    }

    public Source getSource() {
        return this.source;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x002a  */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x001e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected String readStringIncluding(char c) throws IOException {
        char read;
        int i = 0;
        while (true) {
            read = this.inputStream.read();
            int i2 = i + 1;
            this.lineBuffer[i] = (byte) read;
            if (i2 >= this.lineBuffer.length) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Server sent line > ");
                stringBuilder.append(this.lineBuffer.length);
                throw new IOException(stringBuilder.toString());
            } else if (read != c && read > 65535) {
                i = i2;
            } else if (read == 65535) {
                return new String(this.lineBuffer, 0, i2).trim();
            } else {
                throw new EOFException();
            }
        }
        if (read == 65535) {
        }
    }

    protected InputStream wrapWithCounter(final InputStream inputStream, final Long l) {
        return new InputStream() {
            public void close() throws IOException {
                if (ESTResponse.this.contentLength != null && ESTResponse.this.contentLength.longValue() - 1 > ESTResponse.this.read) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Stream closed before limit fully read, Read: ");
                    stringBuilder.append(ESTResponse.this.read);
                    stringBuilder.append(" ContentLength: ");
                    stringBuilder.append(ESTResponse.this.contentLength);
                    throw new IOException(stringBuilder.toString());
                } else if (inputStream.available() <= 0) {
                    inputStream.close();
                } else {
                    throw new IOException("Stream closed with extra content in pipe that exceeds content length.");
                }
            }

            public int read() throws IOException {
                int read = inputStream.read();
                if (read > -1) {
                    ESTResponse.this.read = 1 + ESTResponse.this.read;
                    if (l == null || ESTResponse.this.read < l.longValue()) {
                        return read;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Absolute Read Limit exceeded: ");
                    stringBuilder.append(l);
                    throw new IOException(stringBuilder.toString());
                }
                return read;
            }
        };
    }
}
