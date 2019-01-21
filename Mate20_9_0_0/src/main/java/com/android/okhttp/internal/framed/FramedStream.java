package com.android.okhttp.internal.framed;

import com.android.okhttp.okio.AsyncTimeout;
import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.Sink;
import com.android.okhttp.okio.Source;
import com.android.okhttp.okio.Timeout;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public final class FramedStream {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    long bytesLeftInWriteWindow;
    private final FramedConnection connection;
    private ErrorCode errorCode = null;
    private final int id;
    private final StreamTimeout readTimeout = new StreamTimeout();
    private final List<Header> requestHeaders;
    private List<Header> responseHeaders;
    final FramedDataSink sink;
    private final FramedDataSource source;
    long unacknowledgedBytesRead = 0;
    private final StreamTimeout writeTimeout = new StreamTimeout();

    final class FramedDataSink implements Sink {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final long EMIT_BUFFER_SIZE = 16384;
        private boolean closed;
        private boolean finished;
        private final Buffer sendBuffer = new Buffer();

        static {
            Class cls = FramedStream.class;
        }

        FramedDataSink() {
        }

        public void write(Buffer source, long byteCount) throws IOException {
            this.sendBuffer.write(source, byteCount);
            while (this.sendBuffer.size() >= EMIT_BUFFER_SIZE) {
                emitDataFrame($assertionsDisabled);
            }
        }

        private void emitDataFrame(boolean outFinished) throws IOException {
            long toWrite;
            synchronized (FramedStream.this) {
                FramedStream.this.writeTimeout.enter();
                while (FramedStream.this.bytesLeftInWriteWindow <= 0 && !this.finished && !this.closed && FramedStream.this.errorCode == null) {
                    try {
                        FramedStream.this.waitForIo();
                    } finally {
                        FramedStream.this.writeTimeout.exitAndThrowIfTimedOut();
                    }
                }
                FramedStream.this.checkOutNotClosed();
                toWrite = Math.min(FramedStream.this.bytesLeftInWriteWindow, this.sendBuffer.size());
                FramedStream framedStream = FramedStream.this;
                framedStream.bytesLeftInWriteWindow -= toWrite;
            }
            FramedStream.this.writeTimeout.enter();
            try {
                FramedConnection access$500 = FramedStream.this.connection;
                int access$600 = FramedStream.this.id;
                boolean z = (outFinished && toWrite == this.sendBuffer.size()) ? true : $assertionsDisabled;
                access$500.writeData(access$600, z, this.sendBuffer, toWrite);
            } finally {
                FramedStream.this.writeTimeout.exitAndThrowIfTimedOut();
            }
        }

        public void flush() throws IOException {
            synchronized (FramedStream.this) {
                FramedStream.this.checkOutNotClosed();
            }
            while (this.sendBuffer.size() > 0) {
                emitDataFrame($assertionsDisabled);
                FramedStream.this.connection.flush();
            }
        }

        public Timeout timeout() {
            return FramedStream.this.writeTimeout;
        }

        /* JADX WARNING: Missing block: B:9:0x0012, code skipped:
            if (r8.this$0.sink.finished != false) goto L_0x0041;
     */
        /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            if (r8.sendBuffer.size() <= 0) goto L_0x002e;
     */
        /* JADX WARNING: Missing block: B:13:0x0028, code skipped:
            if (r8.sendBuffer.size() <= 0) goto L_0x0041;
     */
        /* JADX WARNING: Missing block: B:14:0x002a, code skipped:
            emitDataFrame(true);
     */
        /* JADX WARNING: Missing block: B:15:0x002e, code skipped:
            com.android.okhttp.internal.framed.FramedStream.access$500(r8.this$0).writeData(com.android.okhttp.internal.framed.FramedStream.access$600(r8.this$0), true, null, 0);
     */
        /* JADX WARNING: Missing block: B:16:0x0041, code skipped:
            r2 = r8.this$0;
     */
        /* JADX WARNING: Missing block: B:17:0x0043, code skipped:
            monitor-enter(r2);
     */
        /* JADX WARNING: Missing block: B:19:?, code skipped:
            r8.closed = true;
     */
        /* JADX WARNING: Missing block: B:20:0x0046, code skipped:
            monitor-exit(r2);
     */
        /* JADX WARNING: Missing block: B:21:0x0047, code skipped:
            com.android.okhttp.internal.framed.FramedStream.access$500(r8.this$0).flush();
            com.android.okhttp.internal.framed.FramedStream.access$1000(r8.this$0);
     */
        /* JADX WARNING: Missing block: B:22:0x0055, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void close() throws IOException {
            synchronized (FramedStream.this) {
                if (this.closed) {
                }
            }
        }
    }

    private final class FramedDataSource implements Source {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private boolean closed;
        private boolean finished;
        private final long maxByteCount;
        private final Buffer readBuffer;
        private final Buffer receiveBuffer;

        static {
            Class cls = FramedStream.class;
        }

        private FramedDataSource(long maxByteCount) {
            this.receiveBuffer = new Buffer();
            this.readBuffer = new Buffer();
            this.maxByteCount = maxByteCount;
        }

        /* JADX WARNING: Missing block: B:14:0x0065, code skipped:
            r5 = com.android.okhttp.internal.framed.FramedStream.access$500(r11.this$0);
     */
        /* JADX WARNING: Missing block: B:15:0x006b, code skipped:
            monitor-enter(r5);
     */
        /* JADX WARNING: Missing block: B:17:?, code skipped:
            r2 = com.android.okhttp.internal.framed.FramedStream.access$500(r11.this$0);
            r2.unacknowledgedBytesRead += r3;
     */
        /* JADX WARNING: Missing block: B:18:0x0090, code skipped:
            if (com.android.okhttp.internal.framed.FramedStream.access$500(r11.this$0).unacknowledgedBytesRead < ((long) (com.android.okhttp.internal.framed.FramedStream.access$500(r11.this$0).okHttpSettings.getInitialWindowSize(65536) / 2))) goto L_0x00ac;
     */
        /* JADX WARNING: Missing block: B:19:0x0092, code skipped:
            com.android.okhttp.internal.framed.FramedStream.access$500(r11.this$0).writeWindowUpdateLater(0, com.android.okhttp.internal.framed.FramedStream.access$500(r11.this$0).unacknowledgedBytesRead);
            com.android.okhttp.internal.framed.FramedStream.access$500(r11.this$0).unacknowledgedBytesRead = 0;
     */
        /* JADX WARNING: Missing block: B:20:0x00ac, code skipped:
            monitor-exit(r5);
     */
        /* JADX WARNING: Missing block: B:21:0x00ad, code skipped:
            return r3;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long read(Buffer sink, long byteCount) throws IOException {
            if (byteCount >= 0) {
                synchronized (FramedStream.this) {
                    waitUntilReadable();
                    checkNotClosed();
                    if (this.readBuffer.size() == 0) {
                        return -1;
                    }
                    long read = this.readBuffer.read(sink, Math.min(byteCount, this.readBuffer.size()));
                    FramedStream framedStream = FramedStream.this;
                    framedStream.unacknowledgedBytesRead += read;
                    if (FramedStream.this.unacknowledgedBytesRead >= ((long) (FramedStream.this.connection.okHttpSettings.getInitialWindowSize(65536) / 2))) {
                        FramedStream.this.connection.writeWindowUpdateLater(FramedStream.this.id, FramedStream.this.unacknowledgedBytesRead);
                        FramedStream.this.unacknowledgedBytesRead = 0;
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
            FramedStream.this.readTimeout.enter();
            while (this.readBuffer.size() == 0 && !this.finished && !this.closed && FramedStream.this.errorCode == null) {
                try {
                    FramedStream.this.waitForIo();
                } catch (Throwable th) {
                    FramedStream.this.readTimeout.exitAndThrowIfTimedOut();
                }
            }
            FramedStream.this.readTimeout.exitAndThrowIfTimedOut();
        }

        void receive(BufferedSource in, long byteCount) throws IOException {
            while (byteCount > 0) {
                boolean finished;
                boolean z;
                boolean flowControlError;
                synchronized (FramedStream.this) {
                    finished = this.finished;
                    z = false;
                    flowControlError = this.readBuffer.size() + byteCount > this.maxByteCount;
                }
                if (flowControlError) {
                    in.skip(byteCount);
                    FramedStream.this.closeLater(ErrorCode.FLOW_CONTROL_ERROR);
                    return;
                } else if (finished) {
                    in.skip(byteCount);
                    return;
                } else {
                    long read = in.read(this.receiveBuffer, byteCount);
                    if (read != -1) {
                        long byteCount2 = byteCount - read;
                        synchronized (FramedStream.this) {
                            if (this.readBuffer.size() == 0) {
                                z = true;
                            }
                            boolean wasEmpty = z;
                            this.readBuffer.writeAll(this.receiveBuffer);
                            if (wasEmpty) {
                                FramedStream.this.notifyAll();
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
            return FramedStream.this.readTimeout;
        }

        public void close() throws IOException {
            synchronized (FramedStream.this) {
                this.closed = true;
                this.readBuffer.clear();
                FramedStream.this.notifyAll();
            }
            FramedStream.this.cancelStreamIfNecessary();
        }

        private void checkNotClosed() throws IOException {
            if (this.closed) {
                throw new IOException("stream closed");
            } else if (FramedStream.this.errorCode != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stream was reset: ");
                stringBuilder.append(FramedStream.this.errorCode);
                throw new IOException(stringBuilder.toString());
            }
        }
    }

    class StreamTimeout extends AsyncTimeout {
        StreamTimeout() {
        }

        protected void timedOut() {
            FramedStream.this.closeLater(ErrorCode.CANCEL);
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

    FramedStream(int id, FramedConnection connection, boolean outFinished, boolean inFinished, List<Header> requestHeaders) {
        if (connection == null) {
            throw new NullPointerException("connection == null");
        } else if (requestHeaders != null) {
            this.id = id;
            this.connection = connection;
            this.bytesLeftInWriteWindow = (long) connection.peerSettings.getInitialWindowSize(65536);
            this.source = new FramedDataSource((long) connection.okHttpSettings.getInitialWindowSize(65536));
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

    public FramedConnection getConnection() {
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stream was reset: ");
            stringBuilder.append(this.errorCode);
            throw new IOException(stringBuilder.toString());
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

    void receiveHeaders(List<Header> headers, HeadersMode headersMode) {
        ErrorCode errorCode = null;
        boolean open = true;
        synchronized (this) {
            if (this.responseHeaders == null) {
                if (headersMode.failIfHeadersAbsent()) {
                    errorCode = ErrorCode.PROTOCOL_ERROR;
                } else {
                    this.responseHeaders = headers;
                    open = isOpen();
                    notifyAll();
                }
            } else if (headersMode.failIfHeadersPresent()) {
                errorCode = ErrorCode.STREAM_IN_USE;
            } else {
                List<Header> newHeaders = new ArrayList();
                newHeaders.addAll(this.responseHeaders);
                newHeaders.addAll(headers);
                this.responseHeaders = newHeaders;
            }
        }
        if (errorCode != null) {
            closeLater(errorCode);
        } else if (!open) {
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

    private void cancelStreamIfNecessary() throws IOException {
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

    private void checkOutNotClosed() throws IOException {
        if (this.sink.closed) {
            throw new IOException("stream closed");
        } else if (this.sink.finished) {
            throw new IOException("stream finished");
        } else if (this.errorCode != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stream was reset: ");
            stringBuilder.append(this.errorCode);
            throw new IOException(stringBuilder.toString());
        }
    }

    private void waitForIo() throws InterruptedIOException {
        try {
            wait();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }
}
