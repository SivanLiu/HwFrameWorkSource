package com.huawei.okhttp3.internal.ws;

import com.huawei.android.app.AppOpsManagerEx;
import com.huawei.android.hishow.AlarmInfoEx;
import com.huawei.android.util.JlogConstantsEx;
import com.huawei.okio.Buffer;
import com.huawei.okio.BufferedSink;
import com.huawei.okio.ByteString;
import com.huawei.okio.Sink;
import com.huawei.okio.Timeout;
import java.io.IOException;
import java.util.Random;

final class WebSocketWriter {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    boolean activeWriter;
    final Buffer buffer = new Buffer();
    final FrameSink frameSink = new FrameSink();
    final boolean isClient;
    final byte[] maskBuffer;
    final byte[] maskKey;
    final Random random;
    final BufferedSink sink;
    boolean writerClosed;

    final class FrameSink implements Sink {
        boolean closed;
        long contentLength;
        int formatOpcode;
        boolean isFirstFrame;

        FrameSink() {
        }

        public void write(Buffer source, long byteCount) throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            WebSocketWriter.this.buffer.write(source, byteCount);
            boolean deferWrite = this.isFirstFrame && this.contentLength != -1 && WebSocketWriter.this.buffer.size() > this.contentLength - 8192;
            long emitCount = WebSocketWriter.this.buffer.completeSegmentByteCount();
            if (emitCount > 0 && !deferWrite) {
                synchronized (WebSocketWriter.this) {
                    WebSocketWriter.this.writeMessageFrameSynchronized(this.formatOpcode, emitCount, this.isFirstFrame, false);
                }
                this.isFirstFrame = false;
            }
        }

        public void flush() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            synchronized (WebSocketWriter.this) {
                WebSocketWriter.this.writeMessageFrameSynchronized(this.formatOpcode, WebSocketWriter.this.buffer.size(), this.isFirstFrame, false);
            }
            this.isFirstFrame = false;
        }

        public Timeout timeout() {
            return WebSocketWriter.this.sink.timeout();
        }

        public void close() throws IOException {
            if (this.closed) {
                throw new IOException("closed");
            }
            synchronized (WebSocketWriter.this) {
                WebSocketWriter.this.writeMessageFrameSynchronized(this.formatOpcode, WebSocketWriter.this.buffer.size(), this.isFirstFrame, true);
            }
            this.closed = true;
            WebSocketWriter.this.activeWriter = false;
        }
    }

    WebSocketWriter(boolean isClient, BufferedSink sink, Random random) {
        if (sink == null) {
            throw new NullPointerException("sink == null");
        } else if (random != null) {
            this.isClient = isClient;
            this.sink = sink;
            this.random = random;
            byte[] bArr = null;
            this.maskKey = isClient ? new byte[4] : null;
            if (isClient) {
                bArr = new byte[8192];
            }
            this.maskBuffer = bArr;
        } else {
            throw new NullPointerException("random == null");
        }
    }

    void writePing(ByteString payload) throws IOException {
        synchronized (this) {
            writeControlFrameSynchronized(9, payload);
        }
    }

    void writePong(ByteString payload) throws IOException {
        synchronized (this) {
            writeControlFrameSynchronized(10, payload);
        }
    }

    void writeClose(int code, ByteString reason) throws IOException {
        ByteString payload = ByteString.EMPTY;
        if (!(code == 0 && reason == null)) {
            if (code != 0) {
                WebSocketProtocol.validateCloseCode(code);
            }
            Buffer buffer = new Buffer();
            buffer.writeShort(code);
            if (reason != null) {
                buffer.write(reason);
            }
            payload = buffer.readByteString();
        }
        synchronized (this) {
            try {
                writeControlFrameSynchronized(8, payload);
                this.writerClosed = true;
            } catch (Throwable th) {
            }
        }
    }

    private void writeControlFrameSynchronized(int opcode, ByteString payload) throws IOException {
        if (this.writerClosed) {
            throw new IOException("closed");
        }
        int length = payload.size();
        if (((long) length) <= 125) {
            this.sink.writeByte(AppOpsManagerEx.TYPE_MICROPHONE | opcode);
            int b1 = length;
            if (this.isClient) {
                this.sink.writeByte(b1 | AppOpsManagerEx.TYPE_MICROPHONE);
                this.random.nextBytes(this.maskKey);
                this.sink.write(this.maskKey);
                byte[] bytes = payload.toByteArray();
                WebSocketProtocol.toggleMask(bytes, (long) bytes.length, this.maskKey, 0);
                this.sink.write(bytes);
            } else {
                this.sink.writeByte(b1);
                this.sink.write(payload);
            }
            this.sink.flush();
            return;
        }
        throw new IllegalArgumentException("Payload size must be less than or equal to 125");
    }

    Sink newMessageSink(int formatOpcode, long contentLength) {
        if (this.activeWriter) {
            throw new IllegalStateException("Another message writer is active. Did you call close()?");
        }
        this.activeWriter = true;
        this.frameSink.formatOpcode = formatOpcode;
        this.frameSink.contentLength = contentLength;
        this.frameSink.isFirstFrame = true;
        this.frameSink.closed = false;
        return this.frameSink;
    }

    void writeMessageFrameSynchronized(int formatOpcode, long byteCount, boolean isFirstFrame, boolean isFinal) throws IOException {
        long j = byteCount;
        if (this.writerClosed) {
            throw new IOException("closed");
        }
        int i = 0;
        int b0 = isFirstFrame ? formatOpcode : 0;
        if (isFinal) {
            b0 |= AppOpsManagerEx.TYPE_MICROPHONE;
        }
        this.sink.writeByte(b0);
        int b1 = 0;
        if (this.isClient) {
            b1 = 0 | AppOpsManagerEx.TYPE_MICROPHONE;
        }
        if (j <= 125) {
            this.sink.writeByte(b1 | ((int) j));
        } else if (j <= 65535) {
            this.sink.writeByte(b1 | JlogConstantsEx.JLID_NEW_CONTACT_SELECT_ACCOUNT);
            this.sink.writeShort((int) j);
        } else {
            this.sink.writeByte(b1 | AlarmInfoEx.EVERYDAY_CODE);
            this.sink.writeLong(j);
        }
        if (this.isClient) {
            this.random.nextBytes(this.maskKey);
            this.sink.write(this.maskKey);
            long written = 0;
            while (written < j) {
                int toRead = (int) Math.min(j, (long) this.maskBuffer.length);
                int read = this.buffer.read(this.maskBuffer, i, toRead);
                if (read != -1) {
                    i = read;
                    WebSocketProtocol.toggleMask(this.maskBuffer, (long) read, this.maskKey, written);
                    this.sink.write(this.maskBuffer, 0, i);
                    written += (long) i;
                    i = 0;
                } else {
                    int i2 = toRead;
                    i = read;
                    throw new AssertionError();
                }
            }
        }
        this.sink.write(this.buffer, j);
        this.sink.emit();
    }
}
