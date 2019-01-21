package com.huawei.okhttp3.internal.ws;

import com.huawei.android.util.HwPCUtilsEx;
import com.huawei.okio.Buffer;
import com.huawei.okio.BufferedSource;
import com.huawei.okio.ByteString;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;

final class WebSocketReader {
    boolean closed;
    long frameBytesRead;
    final FrameCallback frameCallback;
    long frameLength;
    final boolean isClient;
    boolean isControlFrame;
    boolean isFinalFrame;
    boolean isMasked;
    final byte[] maskBuffer = new byte[8192];
    final byte[] maskKey = new byte[4];
    int opcode;
    final BufferedSource source;

    public interface FrameCallback {
        void onReadClose(int i, String str);

        void onReadMessage(ByteString byteString) throws IOException;

        void onReadMessage(String str) throws IOException;

        void onReadPing(ByteString byteString);

        void onReadPong(ByteString byteString);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: SSATransform
        jadx.core.utils.exceptions.JadxRuntimeException: Not initialized variable reg: 5, insn: 0x0036: MOVE  (r3 ?[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r5 ?[int, float, boolean, short, byte, char, OBJECT, ARRAY]), block:B:7:0x0036, method: com.huawei.okhttp3.internal.ws.WebSocketReader.readHeader():void
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:162)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVar(SSATransform.java:184)
        	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:133)
        	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
        	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void readHeader() throws java.io.IOException {
        /*
        r13 = this;
        r0 = r13.closed;
        if (r0 != 0) goto L_0x0124;
        r0 = r13.source;
        r0 = r0.timeout();
        r0 = r0.timeoutNanos();
        r2 = r13.source;
        r2 = r2.timeout();
        r2.clearTimeout();
        r2 = r13.source;	 Catch:{ all -> 0x0117 }
        r2 = r2.readByte();	 Catch:{ all -> 0x0117 }
        r2 = r2 & 255;
        r3 = r13.source;
        r3 = r3.timeout();
        r4 = java.util.concurrent.TimeUnit.NANOSECONDS;
        r3.timeout(r0, r4);
        r3 = r2 & 15;
        r13.opcode = r3;
        r3 = r2 & 128;
        r4 = 0;
        r5 = 1;
        if (r3 == 0) goto L_0x0038;
        r3 = r5;
        goto L_0x0039;
        r3 = r4;
        r13.isFinalFrame = r3;
        r3 = r2 & 8;
        if (r3 == 0) goto L_0x0041;
        r3 = r5;
        goto L_0x0042;
        r3 = r4;
        r13.isControlFrame = r3;
        r3 = r13.isControlFrame;
        if (r3 == 0) goto L_0x0055;
        r3 = r13.isFinalFrame;
        if (r3 == 0) goto L_0x004d;
        goto L_0x0055;
        r3 = new java.net.ProtocolException;
        r4 = "Control frames must be final.";
        r3.<init>(r4);
        throw r3;
        r3 = r2 & 64;
        if (r3 == 0) goto L_0x005b;
        r3 = r5;
        goto L_0x005c;
        r3 = r4;
        r6 = r2 & 32;
        if (r6 == 0) goto L_0x0062;
        r6 = r5;
        goto L_0x0063;
        r6 = r4;
        r7 = r2 & 16;
        if (r7 == 0) goto L_0x0069;
        r7 = r5;
        goto L_0x006a;
        r7 = r4;
        if (r3 != 0) goto L_0x010f;
        if (r6 != 0) goto L_0x010f;
        if (r7 != 0) goto L_0x010f;
        r8 = r13.source;
        r8 = r8.readByte();
        r8 = r8 & 255;
        r9 = r8 & 128;
        if (r9 == 0) goto L_0x007e;
        r4 = r5;
        r13.isMasked = r4;
        r4 = r13.isMasked;
        r5 = r13.isClient;
        if (r4 != r5) goto L_0x0096;
        r4 = new java.net.ProtocolException;
        r5 = r13.isClient;
        if (r5 == 0) goto L_0x0090;
        r5 = "Server-sent frames must not be masked.";
        goto L_0x0092;
        r5 = "Client-sent frames must be masked.";
        r4.<init>(r5);
        throw r4;
        r4 = r8 & 127;
        r4 = (long) r4;
        r13.frameLength = r4;
        r4 = r13.frameLength;
        r9 = 126; // 0x7e float:1.77E-43 double:6.23E-322;
        r4 = (r4 > r9 ? 1 : (r4 == r9 ? 0 : -1));
        r9 = 0;
        if (r4 != 0) goto L_0x00b3;
        r4 = r13.source;
        r4 = r4.readShort();
        r4 = (long) r4;
        r11 = 65535; // 0xffff float:9.1834E-41 double:3.23786E-319;
        r4 = r4 & r11;
        r13.frameLength = r4;
        goto L_0x00ec;
        r4 = r13.frameLength;
        r11 = 127; // 0x7f float:1.78E-43 double:6.27E-322;
        r4 = (r4 > r11 ? 1 : (r4 == r11 ? 0 : -1));
        if (r4 != 0) goto L_0x00ec;
        r4 = r13.source;
        r4 = r4.readLong();
        r13.frameLength = r4;
        r4 = r13.frameLength;
        r4 = (r4 > r9 ? 1 : (r4 == r9 ? 0 : -1));
        if (r4 < 0) goto L_0x00ca;
        goto L_0x00ec;
        r4 = new java.net.ProtocolException;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r9 = "Frame length 0x";
        r5.append(r9);
        r9 = r13.frameLength;
        r9 = java.lang.Long.toHexString(r9);
        r5.append(r9);
        r9 = " > 0x7FFFFFFFFFFFFFFF";
        r5.append(r9);
        r5 = r5.toString();
        r4.<init>(r5);
        throw r4;
        r13.frameBytesRead = r9;
        r4 = r13.isControlFrame;
        if (r4 == 0) goto L_0x0103;
        r4 = r13.frameLength;
        r9 = 125; // 0x7d float:1.75E-43 double:6.2E-322;
        r4 = (r4 > r9 ? 1 : (r4 == r9 ? 0 : -1));
        if (r4 > 0) goto L_0x00fb;
        goto L_0x0103;
        r4 = new java.net.ProtocolException;
        r5 = "Control frame must be less than 125B.";
        r4.<init>(r5);
        throw r4;
        r4 = r13.isMasked;
        if (r4 == 0) goto L_?;
        r4 = r13.source;
        r5 = r13.maskKey;
        r4.readFully(r5);
        return;
        r4 = new java.net.ProtocolException;
        r5 = "Reserved flags are unsupported.";
        r4.<init>(r5);
        throw r4;
        r2 = move-exception;
        r3 = r13.source;
        r3 = r3.timeout();
        r4 = java.util.concurrent.TimeUnit.NANOSECONDS;
        r3.timeout(r0, r4);
        throw r2;
        r0 = new java.io.IOException;
        r1 = "closed";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.okhttp3.internal.ws.WebSocketReader.readHeader():void");
    }

    WebSocketReader(boolean isClient, BufferedSource source, FrameCallback frameCallback) {
        if (source == null) {
            throw new NullPointerException("source == null");
        } else if (frameCallback != null) {
            this.isClient = isClient;
            this.source = source;
            this.frameCallback = frameCallback;
        } else {
            throw new NullPointerException("frameCallback == null");
        }
    }

    void processNextFrame() throws IOException {
        readHeader();
        if (this.isControlFrame) {
            readControlFrame();
        } else {
            readMessageFrame();
        }
    }

    private void readControlFrame() throws IOException {
        Buffer buffer = new Buffer();
        if (this.frameBytesRead < this.frameLength) {
            if (this.isClient) {
                this.source.readFully(buffer, this.frameLength);
            } else {
                while (this.frameBytesRead < this.frameLength) {
                    int read = this.source.read(this.maskBuffer, 0, (int) Math.min(this.frameLength - this.frameBytesRead, (long) this.maskBuffer.length));
                    if (read != -1) {
                        WebSocketProtocol.toggleMask(this.maskBuffer, (long) read, this.maskKey, this.frameBytesRead);
                        buffer.write(this.maskBuffer, 0, read);
                        this.frameBytesRead += (long) read;
                    } else {
                        throw new EOFException();
                    }
                }
            }
        }
        switch (this.opcode) {
            case 8:
                int code = HwPCUtilsEx.FORCED_PC_DISPLAY_SIZE_GET_OVERSCAN_MODE;
                String reason = "";
                long bufferSize = buffer.size();
                if (bufferSize != 1) {
                    if (bufferSize != 0) {
                        code = buffer.readShort();
                        reason = buffer.readUtf8();
                        String codeExceptionMessage = WebSocketProtocol.closeCodeExceptionMessage(code);
                        if (codeExceptionMessage != null) {
                            throw new ProtocolException(codeExceptionMessage);
                        }
                    }
                    this.frameCallback.onReadClose(code, reason);
                    this.closed = true;
                    return;
                }
                throw new ProtocolException("Malformed close payload length of 1.");
            case 9:
                this.frameCallback.onReadPing(buffer.readByteString());
                return;
            case 10:
                this.frameCallback.onReadPong(buffer.readByteString());
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown control opcode: ");
                stringBuilder.append(Integer.toHexString(this.opcode));
                throw new ProtocolException(stringBuilder.toString());
        }
    }

    private void readMessageFrame() throws IOException {
        int opcode = this.opcode;
        if (opcode == 1 || opcode == 2) {
            Buffer message = new Buffer();
            readMessage(message);
            if (opcode == 1) {
                this.frameCallback.onReadMessage(message.readUtf8());
                return;
            } else {
                this.frameCallback.onReadMessage(message.readByteString());
                return;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown opcode: ");
        stringBuilder.append(Integer.toHexString(opcode));
        throw new ProtocolException(stringBuilder.toString());
    }

    void readUntilNonControlFrame() throws IOException {
        while (!this.closed) {
            readHeader();
            if (this.isControlFrame) {
                readControlFrame();
            } else {
                return;
            }
        }
    }

    private void readMessage(Buffer sink) throws IOException {
        while (!this.closed) {
            long read;
            if (this.frameBytesRead == this.frameLength) {
                if (!this.isFinalFrame) {
                    readUntilNonControlFrame();
                    if (this.opcode != 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected continuation opcode. Got: ");
                        stringBuilder.append(Integer.toHexString(this.opcode));
                        throw new ProtocolException(stringBuilder.toString());
                    } else if (this.isFinalFrame && this.frameLength == 0) {
                        return;
                    }
                }
                return;
            }
            long toRead = this.frameLength - this.frameBytesRead;
            if (this.isMasked) {
                read = (long) this.source.read(this.maskBuffer, 0, (int) Math.min(toRead, (long) this.maskBuffer.length));
                if (read != -1) {
                    WebSocketProtocol.toggleMask(this.maskBuffer, read, this.maskKey, this.frameBytesRead);
                    sink.write(this.maskBuffer, 0, (int) read);
                } else {
                    throw new EOFException();
                }
            }
            read = this.source.read(sink, toRead);
            if (read == -1) {
                throw new EOFException();
            }
            this.frameBytesRead += read;
        }
        throw new IOException("closed");
    }
}
