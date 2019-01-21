package com.huawei.okhttp3.internal.http2;

import com.huawei.okio.AsyncTimeout;
import com.huawei.okio.Buffer;
import com.huawei.okio.BufferedSource;
import com.huawei.okio.Sink;
import com.huawei.okio.Source;
import com.huawei.okio.Timeout;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public final class Http2Stream {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    long bytesLeftInWriteWindow;
    final Http2Connection connection;
    ErrorCode errorCode = null;
    final int id;
    final StreamTimeout readTimeout = new StreamTimeout();
    private final List<Header> requestHeaders;
    private List<Header> responseHeaders;
    final FramedDataSink sink;
    private final FramedDataSource source;
    long unacknowledgedBytesRead = 0;
    final StreamTimeout writeTimeout = new StreamTimeout();

    final class FramedDataSink implements Sink {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final long EMIT_BUFFER_SIZE = 16384;
        boolean closed;
        boolean finished;
        private final Buffer sendBuffer = new Buffer();

        static {
            Class cls = Http2Stream.class;
        }

        FramedDataSink() {
        }

        public void write(Buffer source, long byteCount) throws IOException {
            this.sendBuffer.write(source, byteCount);
            while (this.sendBuffer.size() >= EMIT_BUFFER_SIZE) {
                emitDataFrame(false);
            }
        }

        private void emitDataFrame(boolean outFinished) throws IOException {
            long toWrite;
            synchronized (Http2Stream.this) {
                Http2Stream.this.writeTimeout.enter();
                while (Http2Stream.this.bytesLeftInWriteWindow <= 0 && !this.finished && !this.closed && Http2Stream.this.errorCode == null) {
                    try {
                        Http2Stream.this.waitForIo();
                    } finally {
                        Http2Stream.this.writeTimeout.exitAndThrowIfTimedOut();
                    }
                }
                Http2Stream.this.checkOutNotClosed();
                toWrite = Math.min(Http2Stream.this.bytesLeftInWriteWindow, this.sendBuffer.size());
                Http2Stream http2Stream = Http2Stream.this;
                http2Stream.bytesLeftInWriteWindow -= toWrite;
            }
            Http2Stream.this.writeTimeout.enter();
            try {
                Http2Connection http2Connection = Http2Stream.this.connection;
                int i = Http2Stream.this.id;
                boolean z = outFinished && toWrite == this.sendBuffer.size();
                http2Connection.writeData(i, z, this.sendBuffer, toWrite);
            } finally {
                Http2Stream.this.writeTimeout.exitAndThrowIfTimedOut();
            }
        }

        public void flush() throws IOException {
            synchronized (Http2Stream.this) {
                Http2Stream.this.checkOutNotClosed();
            }
            while (this.sendBuffer.size() > 0) {
                emitDataFrame(false);
                Http2Stream.this.connection.flush();
            }
        }

        public Timeout timeout() {
            return Http2Stream.this.writeTimeout;
        }

        /* JADX WARNING: Missing block: B:9:0x0012, code skipped:
            if (r8.this$0.sink.finished != false) goto L_0x003d;
     */
        /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            if (r8.sendBuffer.size() <= 0) goto L_0x002e;
     */
        /* JADX WARNING: Missing block: B:13:0x0028, code skipped:
            if (r8.sendBuffer.size() <= 0) goto L_0x003d;
     */
        /* JADX WARNING: Missing block: B:14:0x002a, code skipped:
            emitDataFrame(true);
     */
        /* JADX WARNING: Missing block: B:15:0x002e, code skipped:
            r8.this$0.connection.writeData(r8.this$0.id, true, null, 0);
     */
        /* JADX WARNING: Missing block: B:16:0x003d, code skipped:
            r2 = r8.this$0;
     */
        /* JADX WARNING: Missing block: B:17:0x003f, code skipped:
            monitor-enter(r2);
     */
        /* JADX WARNING: Missing block: B:19:?, code skipped:
            r8.closed = true;
     */
        /* JADX WARNING: Missing block: B:20:0x0042, code skipped:
            monitor-exit(r2);
     */
        /* JADX WARNING: Missing block: B:21:0x0043, code skipped:
            r8.this$0.connection.flush();
            r8.this$0.cancelStreamIfNecessary();
     */
        /* JADX WARNING: Missing block: B:22:0x004f, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void close() throws IOException {
            synchronized (Http2Stream.this) {
                if (this.closed) {
                }
            }
        }
    }

    private final class FramedDataSource implements Source {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        boolean closed;
        boolean finished;
        private final long maxByteCount;
        private final Buffer readBuffer = new Buffer();
        private final Buffer receiveBuffer = new Buffer();

        static {
            Class cls = Http2Stream.class;
        }

        FramedDataSource(long maxByteCount) {
            this.maxByteCount = maxByteCount;
        }

        /* JADX WARNING: Missing block: B:14:0x005d, code skipped:
            r5 = r10.this$0.connection;
     */
        /* JADX WARNING: Missing block: B:15:0x0061, code skipped:
            monitor-enter(r5);
     */
        /* JADX WARNING: Missing block: B:17:?, code skipped:
            r2 = r10.this$0.connection;
            r2.unacknowledgedBytesRead += r3;
     */
        /* JADX WARNING: Missing block: B:18:0x0080, code skipped:
            if (r10.this$0.connection.unacknowledgedBytesRead < ((long) (r10.this$0.connection.okHttpSettings.getInitialWindowSize() / 2))) goto L_0x0096;
     */
        /* JADX WARNING: Missing block: B:19:0x0082, code skipped:
            r10.this$0.connection.writeWindowUpdateLater(0, r10.this$0.connection.unacknowledgedBytesRead);
            r10.this$0.connection.unacknowledgedBytesRead = 0;
     */
        /* JADX WARNING: Missing block: B:20:0x0096, code skipped:
            monitor-exit(r5);
     */
        /* JADX WARNING: Missing block: B:21:0x0097, code skipped:
            return r3;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long read(Buffer sink, long byteCount) throws IOException {
            if (byteCount >= 0) {
                synchronized (Http2Stream.this) {
                    waitUntilReadable();
                    checkNotClosed();
                    if (this.readBuffer.size() == 0) {
                        return -1;
                    }
                    long read = this.readBuffer.read(sink, Math.min(byteCount, this.readBuffer.size()));
                    Http2Stream http2Stream = Http2Stream.this;
                    http2Stream.unacknowledgedBytesRead += read;
                    if (Http2Stream.this.unacknowledgedBytesRead >= ((long) (Http2Stream.this.connection.okHttpSettings.getInitialWindowSize() / 2))) {
                        Http2Stream.this.connection.writeWindowUpdateLater(Http2Stream.this.id, Http2Stream.this.unacknowledgedBytesRead);
                        Http2Stream.this.unacknowledgedBytesRead = 0;
                    }
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("byteCount < 0: ");
                stringBuilder.append(byteCount);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        private void waitUntilReadable() throws IOException {
            Http2Stream.this.readTimeout.enter();
            while (this.readBuffer.size() == 0 && !this.finished && !this.closed && Http2Stream.this.errorCode == null) {
                try {
                    Http2Stream.this.waitForIo();
                } catch (Throwable th) {
                    Http2Stream.this.readTimeout.exitAndThrowIfTimedOut();
                }
            }
            Http2Stream.this.readTimeout.exitAndThrowIfTimedOut();
        }

        void receive(BufferedSource in, long byteCount) throws IOException {
            while (byteCount > 0) {
                boolean finished;
                boolean z;
                boolean flowControlError;
                synchronized (Http2Stream.this) {
                    finished = this.finished;
                    z = false;
                    flowControlError = this.readBuffer.size() + byteCount > this.maxByteCount;
                }
                if (flowControlError) {
                    in.skip(byteCount);
                    Http2Stream.this.closeLater(ErrorCode.FLOW_CONTROL_ERROR);
                    return;
                } else if (finished) {
                    in.skip(byteCount);
                    return;
                } else {
                    long read = in.read(this.receiveBuffer, byteCount);
                    if (read != -1) {
                        long byteCount2 = byteCount - read;
                        synchronized (Http2Stream.this) {
                            if (this.readBuffer.size() == 0) {
                                z = true;
                            }
                            boolean wasEmpty = z;
                            this.readBuffer.writeAll(this.receiveBuffer);
                            if (wasEmpty) {
                                Http2Stream.this.notifyAll();
                            }
                        }
                        byteCount = byteCount2;
                    } else {
                        throw new EOFException();
                    }
                }
            }
        }

        public Timeout timeout() {
            return Http2Stream.this.readTimeout;
        }

        public void close() throws IOException {
            synchronized (Http2Stream.this) {
                this.closed = true;
                this.readBuffer.clear();
                Http2Stream.this.notifyAll();
            }
            Http2Stream.this.cancelStreamIfNecessary();
        }

        private void checkNotClosed() throws IOException {
            if (this.closed) {
                throw new IOException("stream closed");
            } else if (Http2Stream.this.errorCode != null) {
                throw new StreamResetException(Http2Stream.this.errorCode);
            }
        }
    }

    class StreamTimeout extends AsyncTimeout {
        StreamTimeout() {
        }

        protected void timedOut() {
            Http2Stream.this.closeLater(ErrorCode.CANCEL);
        }

        protected IOException newTimeoutException(IOException cause) {
            SocketTimeoutException socketTimeoutException = new SocketTimeoutException("timeout");
            if (cause != null) {
                socketTimeoutException.initCause(cause);
            }
            return socketTimeoutException;
        }

        public void exitAndThrowIfTimedOut() throws IOException {
            if (exit()) {
                throw newTimeoutException(null);
            }
        }
    }

    Http2Stream(int id, Http2Connection connection, boolean outFinished, boolean inFinished, List<Header> requestHeaders) {
        if (connection == null) {
            throw new NullPointerException("connection == null");
        } else if (requestHeaders != null) {
            this.id = id;
            this.connection = connection;
            this.bytesLeftInWriteWindow = (long) connection.peerSettings.getInitialWindowSize();
            this.source = new FramedDataSource((long) connection.okHttpSettings.getInitialWindowSize());
            this.sink = new FramedDataSink();
            this.source.finished = inFinished;
            this.sink.finished = outFinished;
            this.requestHeaders = requestHeaders;
        } else {
            throw new NullPointerException("requestHeaders == null");
        }
    }

    public int getId() {
        return this.id;
    }

    public synchronized boolean isOpen() {
        if (this.errorCode != null) {
            return false;
        }
        if ((this.source.finished || this.source.closed) && ((this.sink.finished || this.sink.closed) && this.responseHeaders != null)) {
            return false;
        }
        return true;
    }

    public boolean isLocallyInitiated() {
        if (this.connection.client == ((this.id & 1) == 1)) {
            return true;
        }
        return false;
    }

    public Http2Connection getConnection() {
        return this.connection;
    }

    public List<Header> getRequestHeaders() {
        return this.requestHeaders;
    }

    public synchronized List<Header> getResponseHeaders() throws IOException {
        this.readTimeout.enter();
        while (this.responseHeaders == null && this.errorCode == null) {
            try {
                waitForIo();
            } finally {
                this.readTimeout.exitAndThrowIfTimedOut();
            }
        }
        if (this.responseHeaders != null) {
        } else {
            throw new StreamResetException(this.errorCode);
        }
        return this.responseHeaders;
    }

    public synchronized ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public void reply(List<Header> responseHeaders, boolean out) throws IOException {
        boolean outFinished = false;
        synchronized (this) {
            if (responseHeaders == null) {
                throw new NullPointerException("responseHeaders == null");
            } else if (this.responseHeaders == null) {
                this.responseHeaders = responseHeaders;
                if (!out) {
                    this.sink.finished = true;
                    outFinished = true;
                }
            } else {
                throw new IllegalStateException("reply already sent");
            }
        }
        this.connection.writeSynReply(this.id, outFinished, responseHeaders);
        if (outFinished) {
            this.connection.flush();
        }
    }

    public Timeout readTimeout() {
        return this.readTimeout;
    }

    public Timeout writeTimeout() {
        return this.writeTimeout;
    }

    public Source getSource() {
        return this.source;
    }

    public Sink getSink() {
        synchronized (this) {
            if (this.responseHeaders == null) {
                if (!isLocallyInitiated()) {
                    throw new IllegalStateException("reply before requesting the sink");
                }
            }
        }
        return this.sink;
    }

    public void close(ErrorCode rstStatusCode) throws IOException {
        if (closeInternal(rstStatusCode)) {
            this.connection.writeSynReset(this.id, rstStatusCode);
        }
    }

    public void closeLater(ErrorCode errorCode) {
        if (closeInternal(errorCode)) {
            this.connection.writeSynResetLater(this.id, errorCode);
        }
    }

    private boolean closeInternal(ErrorCode errorCode) {
        synchronized (this) {
            if (this.errorCode != null) {
                return false;
            } else if (this.source.finished && this.sink.finished) {
                return false;
            } else {
                this.errorCode = errorCode;
                notifyAll();
                this.connection.removeStream(this.id);
                return true;
            }
        }
    }

    void receiveHeaders(List<Header> headers) {
        boolean open = true;
        synchronized (this) {
            if (this.responseHeaders == null) {
                this.responseHeaders = headers;
                open = isOpen();
                notifyAll();
            } else {
                List<Header> newHeaders = new ArrayList();
                newHeaders.addAll(this.responseHeaders);
                newHeaders.addAll(headers);
                this.responseHeaders = newHeaders;
            }
        }
        if (!open) {
            this.connection.removeStream(this.id);
        }
    }

    void receiveData(BufferedSource in, int length) throws IOException {
        this.source.receive(in, (long) length);
    }

    void receiveFin() {
        boolean open;
        synchronized (this) {
            this.source.finished = true;
            open = isOpen();
            notifyAll();
        }
        if (!open) {
            this.connection.removeStream(this.id);
        }
    }

    synchronized void receiveRstStream(ErrorCode errorCode) {
        if (this.errorCode == null) {
            this.errorCode = errorCode;
            notifyAll();
        }
    }

    void cancelStreamIfNecessary() throws IOException {
        boolean cancel;
        boolean open;
        synchronized (this) {
            cancel = !this.source.finished && this.source.closed && (this.sink.finished || this.sink.closed);
            open = isOpen();
        }
        if (cancel) {
            close(ErrorCode.CANCEL);
        } else if (!open) {
            this.connection.removeStream(this.id);
        }
    }

    void addBytesToWriteWindow(long delta) {
        this.bytesLeftInWriteWindow += delta;
        if (delta > 0) {
            notifyAll();
        }
    }

    void checkOutNotClosed() throws IOException {
        if (this.sink.closed) {
            throw new IOException("stream closed");
        } else if (this.sink.finished) {
            throw new IOException("stream finished");
        } else if (this.errorCode != null) {
            throw new StreamResetException(this.errorCode);
        }
    }

    void waitForIo() throws InterruptedIOException {
        try {
            wait();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }
}
