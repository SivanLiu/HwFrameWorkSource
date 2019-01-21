package com.android.okhttp.okio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Buffer implements BufferedSource, BufferedSink, Cloneable {
    private static final byte[] DIGITS = new byte[]{(byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102};
    static final int REPLACEMENT_CHARACTER = 65533;
    Segment head;
    long size;

    public long size() {
        return this.size;
    }

    public Buffer buffer() {
        return this;
    }

    public OutputStream outputStream() {
        return new OutputStream() {
            public void write(int b) {
                Buffer.this.writeByte((byte) b);
            }

            public void write(byte[] data, int offset, int byteCount) {
                Buffer.this.write(data, offset, byteCount);
            }

            public void flush() {
            }

            public void close() {
            }

            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(".outputStream()");
                return stringBuilder.toString();
            }
        };
    }

    public Buffer emitCompleteSegments() {
        return this;
    }

    public BufferedSink emit() {
        return this;
    }

    public boolean exhausted() {
        return this.size == 0;
    }

    public void require(long byteCount) throws EOFException {
        if (this.size < byteCount) {
            throw new EOFException();
        }
    }

    public boolean request(long byteCount) {
        return this.size >= byteCount;
    }

    public InputStream inputStream() {
        return new InputStream() {
            public int read() {
                if (Buffer.this.size > 0) {
                    return Buffer.this.readByte() & 255;
                }
                return -1;
            }

            public int read(byte[] sink, int offset, int byteCount) {
                return Buffer.this.read(sink, offset, byteCount);
            }

            public int available() {
                return (int) Math.min(Buffer.this.size, 2147483647L);
            }

            public void close() {
            }

            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(Buffer.this);
                stringBuilder.append(".inputStream()");
                return stringBuilder.toString();
            }
        };
    }

    public Buffer copyTo(OutputStream out) throws IOException {
        return copyTo(out, 0, this.size);
    }

    public Buffer copyTo(OutputStream out, long offset, long byteCount) throws IOException {
        if (out != null) {
            Util.checkOffsetAndCount(this.size, offset, byteCount);
            if (byteCount == 0) {
                return this;
            }
            Segment s = this.head;
            while (offset >= ((long) (s.limit - s.pos))) {
                offset -= (long) (s.limit - s.pos);
                s = s.next;
            }
            while (byteCount > 0) {
                int pos = (int) (((long) s.pos) + offset);
                int toCopy = (int) Math.min((long) (s.limit - pos), byteCount);
                out.write(s.data, pos, toCopy);
                byteCount -= (long) toCopy;
                offset = 0;
                s = s.next;
            }
            return this;
        }
        throw new IllegalArgumentException("out == null");
    }

    public Buffer copyTo(Buffer out, long offset, long byteCount) {
        if (out != null) {
            Util.checkOffsetAndCount(this.size, offset, byteCount);
            if (byteCount == 0) {
                return this;
            }
            out.size += byteCount;
            Segment s = this.head;
            while (offset >= ((long) (s.limit - s.pos))) {
                offset -= (long) (s.limit - s.pos);
                s = s.next;
            }
            while (byteCount > 0) {
                Segment copy = new Segment(s);
                copy.pos = (int) (((long) copy.pos) + offset);
                copy.limit = Math.min(copy.pos + ((int) byteCount), copy.limit);
                if (out.head == null) {
                    copy.prev = copy;
                    copy.next = copy;
                    out.head = copy;
                } else {
                    out.head.prev.push(copy);
                }
                byteCount -= (long) (copy.limit - copy.pos);
                offset = 0;
                s = s.next;
            }
            return this;
        }
        throw new IllegalArgumentException("out == null");
    }

    public Buffer writeTo(OutputStream out) throws IOException {
        return writeTo(out, this.size);
    }

    public Buffer writeTo(OutputStream out, long byteCount) throws IOException {
        if (out != null) {
            Util.checkOffsetAndCount(this.size, 0, byteCount);
            Segment s = this.head;
            while (byteCount > 0) {
                int toCopy = (int) Math.min(byteCount, (long) (s.limit - s.pos));
                out.write(s.data, s.pos, toCopy);
                s.pos += toCopy;
                this.size -= (long) toCopy;
                byteCount -= (long) toCopy;
                if (s.pos == s.limit) {
                    Segment toRecycle = s;
                    Segment pop = toRecycle.pop();
                    s = pop;
                    this.head = pop;
                    SegmentPool.recycle(toRecycle);
                }
            }
            return this;
        }
        throw new IllegalArgumentException("out == null");
    }

    public Buffer readFrom(InputStream in) throws IOException {
        readFrom(in, Long.MAX_VALUE, true);
        return this;
    }

    public Buffer readFrom(InputStream in, long byteCount) throws IOException {
        if (byteCount >= 0) {
            readFrom(in, byteCount, false);
            return this;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("byteCount < 0: ");
        stringBuilder.append(byteCount);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void readFrom(InputStream in, long byteCount, boolean forever) throws IOException {
        if (in != null) {
            while (true) {
                if (byteCount > 0 || forever) {
                    Segment tail = writableSegment(1);
                    int bytesRead = in.read(tail.data, tail.limit, (int) Math.min(byteCount, (long) (8192 - tail.limit)));
                    if (bytesRead != -1) {
                        tail.limit += bytesRead;
                        this.size += (long) bytesRead;
                        byteCount -= (long) bytesRead;
                    } else if (!forever) {
                        throw new EOFException();
                    } else {
                        return;
                    }
                }
                return;
            }
        }
        throw new IllegalArgumentException("in == null");
    }

    public long completeSegmentByteCount() {
        long result = this.size;
        if (result == 0) {
            return 0;
        }
        Segment tail = this.head.prev;
        if (tail.limit < 8192 && tail.owner) {
            result -= (long) (tail.limit - tail.pos);
        }
        return result;
    }

    public byte readByte() {
        if (this.size != 0) {
            Segment segment = this.head;
            byte b = segment.pos;
            int limit = segment.limit;
            int pos = b + 1;
            b = segment.data[b];
            this.size--;
            if (pos == limit) {
                this.head = segment.pop();
                SegmentPool.recycle(segment);
            } else {
                segment.pos = pos;
            }
            return b;
        }
        throw new IllegalStateException("size == 0");
    }

    public byte getByte(long pos) {
        Util.checkOffsetAndCount(this.size, pos, 1);
        Segment s = this.head;
        while (true) {
            int segmentByteCount = s.limit - s.pos;
            if (pos < ((long) segmentByteCount)) {
                return s.data[s.pos + ((int) pos)];
            }
            pos -= (long) segmentByteCount;
            s = s.next;
        }
    }

    public short readShort() {
        if (this.size >= 2) {
            Segment segment = this.head;
            int pos = segment.pos;
            int limit = segment.limit;
            if (limit - pos < 2) {
                return (short) (((readByte() & 255) << 8) | (readByte() & 255));
            }
            byte[] data = segment.data;
            int pos2 = pos + 1;
            int pos3 = pos2 + 1;
            pos = ((data[pos] & 255) << 8) | (data[pos2] & 255);
            this.size -= 2;
            if (pos3 == limit) {
                this.head = segment.pop();
                SegmentPool.recycle(segment);
            } else {
                segment.pos = pos3;
            }
            return (short) pos;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("size < 2: ");
        stringBuilder.append(this.size);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public int readInt() {
        if (this.size >= 4) {
            Segment segment = this.head;
            int pos = segment.pos;
            int limit = segment.limit;
            if (limit - pos < 4) {
                return ((((readByte() & 255) << 24) | ((readByte() & 255) << 16)) | ((readByte() & 255) << 8)) | (readByte() & 255);
            }
            byte[] data = segment.data;
            int pos2 = pos + 1;
            int pos3 = pos2 + 1;
            pos = ((data[pos] & 255) << 24) | ((data[pos2] & 255) << 16);
            pos2 = pos3 + 1;
            pos |= (data[pos3] & 255) << 8;
            pos3 = pos2 + 1;
            pos |= data[pos2] & 255;
            this.size -= 4;
            if (pos3 == limit) {
                this.head = segment.pop();
                SegmentPool.recycle(segment);
            } else {
                segment.pos = pos3;
            }
            return pos;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("size < 4: ");
        stringBuilder.append(this.size);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public long readLong() {
        if (this.size >= 8) {
            Segment segment = this.head;
            int pos = segment.pos;
            int limit = segment.limit;
            if (limit - pos < 8) {
                return ((((long) readInt()) & 4294967295L) << 32) | (((long) readInt()) & 4294967295L);
            }
            byte[] data = segment.data;
            int pos2 = pos + 1;
            pos = pos2 + 1;
            int pos3 = pos + 1;
            pos = pos3 + 1;
            int pos4 = pos + 1;
            pos = pos4 + 1;
            pos4 = pos + 1;
            pos = pos4 + 1;
            long v = ((((((((((long) data[pos]) & 255) << 56) | ((((long) data[pos2]) & 255) << 48)) | ((((long) data[pos]) & 255) << 40)) | ((((long) data[pos3]) & 255) << 32)) | ((((long) data[pos]) & 255) << 24)) | ((((long) data[pos4]) & 255) << 16)) | ((((long) data[pos]) & 255) << 8)) | (((long) data[pos4]) & 255);
            this.size -= 8;
            if (pos == limit) {
                this.head = segment.pop();
                SegmentPool.recycle(segment);
            } else {
                segment.pos = pos;
            }
            return v;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("size < 8: ");
        stringBuilder.append(this.size);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public short readShortLe() {
        return Util.reverseBytesShort(readShort());
    }

    public int readIntLe() {
        return Util.reverseBytesInt(readInt());
    }

    public long readLongLe() {
        return Util.reverseBytesLong(readLong());
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x00cb  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00c1  */
    /* JADX WARNING: Missing block: B:41:0x00de, code skipped:
            if (r4 == false) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:56:?, code skipped:
            return -r1;
     */
    /* JADX WARNING: Missing block: B:57:?, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long readDecimalLong() {
        if (this.size != 0) {
            byte b;
            long value = 0;
            int seen = 0;
            boolean negative = false;
            boolean done = false;
            long overflowZone = -922337203685477580L;
            long overflowDigit = -7;
            loop0:
            while (true) {
                long j;
                byte[] data;
                Segment segment = this.head;
                byte[] data2 = segment.data;
                int pos = segment.pos;
                int limit = segment.limit;
                while (pos < limit) {
                    boolean done2;
                    b = data2[pos];
                    if (b >= (byte) 48 && b <= (byte) 57) {
                        int digit = 48 - b;
                        if (value < overflowZone) {
                            j = overflowZone;
                            break loop0;
                        }
                        if (value == overflowZone) {
                            done2 = done;
                            j = overflowZone;
                            if (((long) digit) < overflowDigit) {
                                break loop0;
                            }
                        }
                        done2 = done;
                        j = overflowZone;
                        value = (value * true) + ((long) digit);
                        data = data2;
                    } else {
                        done2 = done;
                        j = overflowZone;
                        data = data2;
                        if (b == (byte) 45 && seen == 0) {
                            negative = true;
                            overflowDigit--;
                        } else if (seen != 0) {
                            done = true;
                            if (pos != limit) {
                                this.head = segment.pop();
                                SegmentPool.recycle(segment);
                            } else {
                                segment.pos = pos;
                            }
                            if (!!done || this.head == null) {
                                this.size -= (long) seen;
                            } else {
                                overflowZone = j;
                            }
                        } else {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Expected leading [0-9] or '-' character but was 0x");
                            stringBuilder.append(Integer.toHexString(b));
                            throw new NumberFormatException(stringBuilder.toString());
                        }
                    }
                    pos++;
                    seen++;
                    overflowZone = j;
                    done = done2;
                    data2 = data;
                }
                j = overflowZone;
                data = data2;
                if (pos != limit) {
                }
                if (!done) {
                    break;
                }
                break;
            }
            done = new Buffer().writeDecimalLong(value).writeByte((int) b);
            if (!negative) {
                done.readByte();
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Number too large: ");
            stringBuilder2.append(done.readUtf8());
            throw new NumberFormatException(stringBuilder2.toString());
        }
        throw new IllegalStateException("size == 0");
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x00a1  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0097  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00a9 A:{SYNTHETIC, EDGE_INSN: B:40:0x00a9->B:35:0x00a9 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00a5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long readHexadecimalUnsignedLong() {
        if (this.size != 0) {
            long value = 0;
            int seen = 0;
            boolean done = false;
            do {
                Segment segment = this.head;
                byte[] data = segment.data;
                int pos = segment.pos;
                int limit = segment.limit;
                while (pos < limit) {
                    int digit;
                    int b = data[pos];
                    if (b >= (byte) 48 && b <= (byte) 57) {
                        digit = b - 48;
                    } else if (b >= (byte) 97 && b <= (byte) 102) {
                        digit = (b - 97) + 10;
                    } else if (b >= (byte) 65 && b <= (byte) 70) {
                        digit = (b - 65) + 10;
                    } else if (seen != 0) {
                        done = true;
                        if (pos != limit) {
                            this.head = segment.pop();
                            SegmentPool.recycle(segment);
                        } else {
                            segment.pos = pos;
                        }
                        if (!done) {
                            break;
                        }
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Expected leading [0-9a-fA-F] character but was 0x");
                        stringBuilder.append(Integer.toHexString(b));
                        throw new NumberFormatException(stringBuilder.toString());
                    }
                    if ((-1152921504606846976L & value) == 0) {
                        value = (value << 4) | ((long) digit);
                        pos++;
                        seen++;
                    } else {
                        Buffer buffer = new Buffer().writeHexadecimalUnsignedLong(value).writeByte(b);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Number too large: ");
                        stringBuilder2.append(buffer.readUtf8());
                        throw new NumberFormatException(stringBuilder2.toString());
                    }
                }
                if (pos != limit) {
                }
                if (!done) {
                }
            } while (this.head != null);
            this.size -= (long) seen;
            return value;
        }
        throw new IllegalStateException("size == 0");
    }

    public ByteString readByteString() {
        return new ByteString(readByteArray());
    }

    public ByteString readByteString(long byteCount) throws EOFException {
        return new ByteString(readByteArray(byteCount));
    }

    public void readFully(Buffer sink, long byteCount) throws EOFException {
        if (this.size >= byteCount) {
            sink.write(this, byteCount);
        } else {
            sink.write(this, this.size);
            throw new EOFException();
        }
    }

    public long readAll(Sink sink) throws IOException {
        long byteCount = this.size;
        if (byteCount > 0) {
            sink.write(this, byteCount);
        }
        return byteCount;
    }

    public String readUtf8() {
        try {
            return readString(this.size, Util.UTF_8);
        } catch (EOFException e) {
            throw new AssertionError(e);
        }
    }

    public String readUtf8(long byteCount) throws EOFException {
        return readString(byteCount, Util.UTF_8);
    }

    public String readString(Charset charset) {
        try {
            return readString(this.size, charset);
        } catch (EOFException e) {
            throw new AssertionError(e);
        }
    }

    public String readString(long byteCount, Charset charset) throws EOFException {
        Util.checkOffsetAndCount(this.size, 0, byteCount);
        if (charset == null) {
            throw new IllegalArgumentException("charset == null");
        } else if (byteCount > 2147483647L) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("byteCount > Integer.MAX_VALUE: ");
            stringBuilder.append(byteCount);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (byteCount == 0) {
            return "";
        } else {
            Segment s = this.head;
            if (((long) s.pos) + byteCount > ((long) s.limit)) {
                return new String(readByteArray(byteCount), charset);
            }
            String result = new String(s.data, s.pos, (int) byteCount, charset);
            s.pos = (int) (((long) s.pos) + byteCount);
            this.size -= byteCount;
            if (s.pos == s.limit) {
                this.head = s.pop();
                SegmentPool.recycle(s);
            }
            return result;
        }
    }

    public String readUtf8Line() throws EOFException {
        long newline = indexOf((byte) 10);
        if (newline != -1) {
            return readUtf8Line(newline);
        }
        return this.size != 0 ? readUtf8(this.size) : null;
    }

    public String readUtf8LineStrict() throws EOFException {
        long newline = indexOf((byte) 10);
        if (newline != -1) {
            return readUtf8Line(newline);
        }
        Buffer data = new Buffer();
        copyTo(data, 0, Math.min(32, this.size));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\\n not found: size=");
        stringBuilder.append(size());
        stringBuilder.append(" content=");
        stringBuilder.append(data.readByteString().hex());
        stringBuilder.append("...");
        throw new EOFException(stringBuilder.toString());
    }

    String readUtf8Line(long newline) throws EOFException {
        String result;
        if (newline <= 0 || getByte(newline - 1) != (byte) 13) {
            result = readUtf8(newline);
            skip(1);
            return result;
        }
        result = readUtf8(newline - 1);
        skip(2);
        return result;
    }

    public int readUtf8CodePoint() throws EOFException {
        if (this.size != 0) {
            int codePoint;
            int byteCount;
            byte b0 = getByte(0);
            int min;
            if ((b0 & 128) == 0) {
                codePoint = b0 & 127;
                byteCount = 1;
                min = 0;
            } else if ((b0 & 224) == 192) {
                codePoint = b0 & 31;
                byteCount = 2;
                min = 128;
            } else if ((b0 & 240) == 224) {
                codePoint = b0 & 15;
                byteCount = 3;
                min = 2048;
            } else if ((b0 & 248) == 240) {
                codePoint = b0 & 7;
                byteCount = 4;
                min = 65536;
            } else {
                skip(1);
                return REPLACEMENT_CHARACTER;
            }
            if (this.size >= ((long) byteCount)) {
                int i = 1;
                while (i < byteCount) {
                    byte b = getByte((long) i);
                    if ((b & 192) == 128) {
                        codePoint = (codePoint << 6) | (b & 63);
                        i++;
                    } else {
                        skip((long) i);
                        return REPLACEMENT_CHARACTER;
                    }
                }
                skip((long) byteCount);
                if (codePoint > 1114111) {
                    return REPLACEMENT_CHARACTER;
                }
                if ((codePoint < 55296 || codePoint > 57343) && codePoint >= min) {
                    return codePoint;
                }
                return REPLACEMENT_CHARACTER;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("size < ");
            stringBuilder.append(byteCount);
            stringBuilder.append(": ");
            stringBuilder.append(this.size);
            stringBuilder.append(" (to read code point prefixed 0x");
            stringBuilder.append(Integer.toHexString(b0));
            stringBuilder.append(")");
            throw new EOFException(stringBuilder.toString());
        }
        throw new EOFException();
    }

    public byte[] readByteArray() {
        try {
            return readByteArray(this.size);
        } catch (EOFException e) {
            throw new AssertionError(e);
        }
    }

    public byte[] readByteArray(long byteCount) throws EOFException {
        Util.checkOffsetAndCount(this.size, 0, byteCount);
        if (byteCount <= 2147483647L) {
            byte[] result = new byte[((int) byteCount)];
            readFully(result);
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("byteCount > Integer.MAX_VALUE: ");
        stringBuilder.append(byteCount);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int read(byte[] sink) {
        return read(sink, 0, sink.length);
    }

    public void readFully(byte[] sink) throws EOFException {
        int offset = 0;
        while (offset < sink.length) {
            int read = read(sink, offset, sink.length - offset);
            if (read != -1) {
                offset += read;
            } else {
                throw new EOFException();
            }
        }
    }

    public int read(byte[] sink, int offset, int byteCount) {
        Util.checkOffsetAndCount((long) sink.length, (long) offset, (long) byteCount);
        Segment s = this.head;
        if (s == null) {
            return -1;
        }
        int toCopy = Math.min(byteCount, s.limit - s.pos);
        System.arraycopy(s.data, s.pos, sink, offset, toCopy);
        s.pos += toCopy;
        this.size -= (long) toCopy;
        if (s.pos == s.limit) {
            this.head = s.pop();
            SegmentPool.recycle(s);
        }
        return toCopy;
    }

    public void clear() {
        try {
            skip(this.size);
        } catch (EOFException e) {
            throw new AssertionError(e);
        }
    }

    public void skip(long byteCount) throws EOFException {
        while (byteCount > 0) {
            if (this.head != null) {
                int toSkip = (int) Math.min(byteCount, (long) (this.head.limit - this.head.pos));
                this.size -= (long) toSkip;
                byteCount -= (long) toSkip;
                Segment segment = this.head;
                segment.pos += toSkip;
                if (this.head.pos == this.head.limit) {
                    segment = this.head;
                    this.head = segment.pop();
                    SegmentPool.recycle(segment);
                }
            } else {
                throw new EOFException();
            }
        }
    }

    public Buffer write(ByteString byteString) {
        if (byteString != null) {
            byteString.write(this);
            return this;
        }
        throw new IllegalArgumentException("byteString == null");
    }

    public Buffer writeUtf8(String string) {
        return writeUtf8(string, 0, string.length());
    }

    public Buffer writeUtf8(String string, int beginIndex, int endIndex) {
        StringBuilder stringBuilder;
        if (string == null) {
            throw new IllegalArgumentException("string == null");
        } else if (beginIndex < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("beginIndex < 0: ");
            stringBuilder.append(beginIndex);
            throw new IllegalAccessError(stringBuilder.toString());
        } else if (endIndex < beginIndex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("endIndex < beginIndex: ");
            stringBuilder.append(endIndex);
            stringBuilder.append(" < ");
            stringBuilder.append(beginIndex);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (endIndex <= string.length()) {
            int i = beginIndex;
            while (i < endIndex) {
                int c = string.charAt(i);
                int segmentOffset;
                if (c < 128) {
                    Segment tail = writableSegment(1);
                    byte[] data = tail.data;
                    segmentOffset = tail.limit - i;
                    int runLimit = Math.min(endIndex, 8192 - segmentOffset);
                    int i2 = i + 1;
                    data[i + segmentOffset] = (byte) c;
                    while (i2 < runLimit) {
                        c = string.charAt(i2);
                        if (c >= 128) {
                            break;
                        }
                        i = i2 + 1;
                        data[i2 + segmentOffset] = (byte) c;
                        i2 = i;
                    }
                    i = (i2 + segmentOffset) - tail.limit;
                    tail.limit += i;
                    this.size += (long) i;
                    i = i2;
                } else if (c < 2048) {
                    writeByte((c >> 6) | 192);
                    writeByte(128 | (c & 63));
                    i++;
                } else if (c < 55296 || c > 57343) {
                    writeByte((c >> 12) | 224);
                    writeByte(((c >> 6) & 63) | 128);
                    writeByte(128 | (c & 63));
                    i++;
                } else {
                    segmentOffset = i + 1 < endIndex ? string.charAt(i + 1) : 0;
                    if (c > 56319 || segmentOffset < 56320 || segmentOffset > 57343) {
                        writeByte(63);
                        i++;
                    } else {
                        int codePoint = 65536 + (((-55297 & c) << 10) | (-56321 & segmentOffset));
                        writeByte((codePoint >> 18) | 240);
                        writeByte(((codePoint >> 12) & 63) | 128);
                        writeByte((63 & (codePoint >> 6)) | 128);
                        writeByte(128 | (codePoint & 63));
                        i += 2;
                    }
                }
            }
            return this;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("endIndex > string.length: ");
            stringBuilder.append(endIndex);
            stringBuilder.append(" > ");
            stringBuilder.append(string.length());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public Buffer writeUtf8CodePoint(int codePoint) {
        StringBuilder stringBuilder;
        if (codePoint < 128) {
            writeByte(codePoint);
        } else if (codePoint < 2048) {
            writeByte((codePoint >> 6) | 192);
            writeByte(128 | (codePoint & 63));
        } else if (codePoint < 65536) {
            if (codePoint < 55296 || codePoint > 57343) {
                writeByte((codePoint >> 12) | 224);
                writeByte(((codePoint >> 6) & 63) | 128);
                writeByte(128 | (codePoint & 63));
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected code point: ");
                stringBuilder.append(Integer.toHexString(codePoint));
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } else if (codePoint <= 1114111) {
            writeByte((codePoint >> 18) | 240);
            writeByte(((codePoint >> 12) & 63) | 128);
            writeByte(((codePoint >> 6) & 63) | 128);
            writeByte(128 | (codePoint & 63));
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected code point: ");
            stringBuilder.append(Integer.toHexString(codePoint));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        return this;
    }

    public Buffer writeString(String string, Charset charset) {
        return writeString(string, 0, string.length(), charset);
    }

    public Buffer writeString(String string, int beginIndex, int endIndex, Charset charset) {
        StringBuilder stringBuilder;
        if (string == null) {
            throw new IllegalArgumentException("string == null");
        } else if (beginIndex < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("beginIndex < 0: ");
            stringBuilder.append(beginIndex);
            throw new IllegalAccessError(stringBuilder.toString());
        } else if (endIndex < beginIndex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("endIndex < beginIndex: ");
            stringBuilder.append(endIndex);
            stringBuilder.append(" < ");
            stringBuilder.append(beginIndex);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (endIndex > string.length()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("endIndex > string.length: ");
            stringBuilder.append(endIndex);
            stringBuilder.append(" > ");
            stringBuilder.append(string.length());
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (charset == null) {
            throw new IllegalArgumentException("charset == null");
        } else if (charset.equals(Util.UTF_8)) {
            return writeUtf8(string);
        } else {
            byte[] data = string.substring(beginIndex, endIndex).getBytes(charset);
            return write(data, 0, data.length);
        }
    }

    public Buffer write(byte[] source) {
        if (source != null) {
            return write(source, 0, source.length);
        }
        throw new IllegalArgumentException("source == null");
    }

    public Buffer write(byte[] source, int offset, int byteCount) {
        if (source != null) {
            Util.checkOffsetAndCount((long) source.length, (long) offset, (long) byteCount);
            int limit = offset + byteCount;
            while (offset < limit) {
                Segment tail = writableSegment(1);
                int toCopy = Math.min(limit - offset, 8192 - tail.limit);
                System.arraycopy(source, offset, tail.data, tail.limit, toCopy);
                offset += toCopy;
                tail.limit += toCopy;
            }
            this.size += (long) byteCount;
            return this;
        }
        throw new IllegalArgumentException("source == null");
    }

    public long writeAll(Source source) throws IOException {
        if (source != null) {
            long totalBytesRead = 0;
            while (true) {
                long read = source.read(this, 8192);
                long readCount = read;
                if (read == -1) {
                    return totalBytesRead;
                }
                totalBytesRead += readCount;
            }
        } else {
            throw new IllegalArgumentException("source == null");
        }
    }

    public BufferedSink write(Source source, long byteCount) throws IOException {
        while (byteCount > 0) {
            long read = source.read(this, byteCount);
            if (read != -1) {
                byteCount -= read;
            } else {
                throw new EOFException();
            }
        }
        return this;
    }

    public Buffer writeByte(int b) {
        Segment tail = writableSegment(1);
        byte[] bArr = tail.data;
        int i = tail.limit;
        tail.limit = i + 1;
        bArr[i] = (byte) b;
        this.size++;
        return this;
    }

    public Buffer writeShort(int s) {
        Segment tail = writableSegment(2);
        byte[] data = tail.data;
        int limit = tail.limit;
        int limit2 = limit + 1;
        data[limit] = (byte) ((s >>> 8) & 255);
        limit = limit2 + 1;
        data[limit2] = (byte) (s & 255);
        tail.limit = limit;
        this.size += 2;
        return this;
    }

    public Buffer writeShortLe(int s) {
        return writeShort(Util.reverseBytesShort((short) s));
    }

    public Buffer writeInt(int i) {
        Segment tail = writableSegment(4);
        byte[] data = tail.data;
        int limit = tail.limit;
        int limit2 = limit + 1;
        data[limit] = (byte) ((i >>> 24) & 255);
        limit = limit2 + 1;
        data[limit2] = (byte) ((i >>> 16) & 255);
        limit2 = limit + 1;
        data[limit] = (byte) ((i >>> 8) & 255);
        limit = limit2 + 1;
        data[limit2] = (byte) (i & 255);
        tail.limit = limit;
        this.size += 4;
        return this;
    }

    public Buffer writeIntLe(int i) {
        return writeInt(Util.reverseBytesInt(i));
    }

    public Buffer writeLong(long v) {
        Segment tail = writableSegment(8);
        byte[] data = tail.data;
        int limit = tail.limit;
        int limit2 = limit + 1;
        data[limit] = (byte) ((int) ((v >>> 56) & 255));
        limit = limit2 + 1;
        data[limit2] = (byte) ((int) ((v >>> 48) & 255));
        limit2 = limit + 1;
        data[limit] = (byte) ((int) ((v >>> 40) & 255));
        limit = limit2 + 1;
        data[limit2] = (byte) ((int) ((v >>> 32) & 255));
        limit2 = limit + 1;
        data[limit] = (byte) ((int) ((v >>> 24) & 255));
        limit = limit2 + 1;
        data[limit2] = (byte) ((int) ((v >>> 16) & 255));
        limit2 = limit + 1;
        data[limit] = (byte) ((int) ((v >>> 8) & 255));
        int limit3 = limit2 + 1;
        data[limit2] = (byte) ((int) (v & 255));
        tail.limit = limit3;
        this.size += 8;
        return this;
    }

    public Buffer writeLongLe(long v) {
        return writeLong(Util.reverseBytesLong(v));
    }

    public Buffer writeDecimalLong(long v) {
        if (v == 0) {
            return writeByte(48);
        }
        boolean negative = false;
        if (v < 0) {
            v = -v;
            if (v < 0) {
                return writeUtf8("-9223372036854775808");
            }
            negative = true;
        }
        int width = v < 100000000 ? v < 10000 ? v < 100 ? v < 10 ? 1 : 2 : v < 1000 ? 3 : 4 : v < 1000000 ? v < 100000 ? 5 : 6 : v < 10000000 ? 7 : 8 : v < 1000000000000L ? v < 10000000000L ? v < 1000000000 ? 9 : 10 : v < 100000000000L ? 11 : 12 : v < 1000000000000000L ? v < 10000000000000L ? 13 : v < 100000000000000L ? 14 : 15 : v < 100000000000000000L ? v < 10000000000000000L ? 16 : 17 : v < 1000000000000000000L ? 18 : 19;
        if (negative) {
            width++;
        }
        Segment tail = writableSegment(width);
        byte[] data = tail.data;
        int pos = tail.limit + width;
        while (v != 0) {
            pos--;
            data[pos] = DIGITS[(int) (v % 10)];
            v /= 10;
        }
        if (negative) {
            data[pos - 1] = (byte) 45;
        }
        tail.limit += width;
        this.size += (long) width;
        return this;
    }

    public Buffer writeHexadecimalUnsignedLong(long v) {
        if (v == 0) {
            return writeByte(48);
        }
        int width = (Long.numberOfTrailingZeros(Long.highestOneBit(v)) / 4) + 1;
        Segment tail = writableSegment(width);
        byte[] data = tail.data;
        int start = tail.limit;
        for (int pos = (tail.limit + width) - 1; pos >= start; pos--) {
            data[pos] = DIGITS[(int) (15 & v)];
            v >>>= 4;
        }
        tail.limit += width;
        this.size += (long) width;
        return this;
    }

    Segment writableSegment(int minimumCapacity) {
        Segment segment;
        if (minimumCapacity < 1 || minimumCapacity > 8192) {
            throw new IllegalArgumentException();
        } else if (this.head == null) {
            this.head = SegmentPool.take();
            Segment segment2 = this.head;
            segment = this.head;
            Segment segment3 = this.head;
            segment.prev = segment3;
            segment2.next = segment3;
            return segment3;
        } else {
            segment = this.head.prev;
            if (segment.limit + minimumCapacity > 8192 || !segment.owner) {
                segment = segment.push(SegmentPool.take());
            }
            return segment;
        }
    }

    public void write(Buffer source, long byteCount) {
        if (source == null) {
            throw new IllegalArgumentException("source == null");
        } else if (source != this) {
            Util.checkOffsetAndCount(source.size, 0, byteCount);
            while (byteCount > 0) {
                Segment tail;
                if (byteCount < ((long) (source.head.limit - source.head.pos))) {
                    tail = this.head != null ? this.head.prev : null;
                    if (tail != null && tail.owner) {
                        if ((((long) tail.limit) + byteCount) - ((long) (tail.shared ? 0 : tail.pos)) <= 8192) {
                            source.head.writeTo(tail, (int) byteCount);
                            source.size -= byteCount;
                            this.size += byteCount;
                            return;
                        }
                    }
                    source.head = source.head.split((int) byteCount);
                }
                tail = source.head;
                long movedByteCount = (long) (tail.limit - tail.pos);
                source.head = tail.pop();
                if (this.head == null) {
                    this.head = tail;
                    Segment segment = this.head;
                    Segment segment2 = this.head;
                    Segment segment3 = this.head;
                    segment2.prev = segment3;
                    segment.next = segment3;
                } else {
                    this.head.prev.push(tail).compact();
                }
                source.size -= movedByteCount;
                this.size += movedByteCount;
                byteCount -= movedByteCount;
            }
        } else {
            throw new IllegalArgumentException("source == this");
        }
    }

    public long read(Buffer sink, long byteCount) {
        if (sink == null) {
            throw new IllegalArgumentException("sink == null");
        } else if (byteCount < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("byteCount < 0: ");
            stringBuilder.append(byteCount);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.size == 0) {
            return -1;
        } else {
            if (byteCount > this.size) {
                byteCount = this.size;
            }
            sink.write(this, byteCount);
            return byteCount;
        }
    }

    public long indexOf(byte b) {
        return indexOf(b, 0);
    }

    public long indexOf(byte b, long fromIndex) {
        long offset = 0;
        if (fromIndex >= 0) {
            Segment s = this.head;
            if (s == null) {
                return -1;
            }
            do {
                int segmentByteCount = s.limit - s.pos;
                if (fromIndex >= ((long) segmentByteCount)) {
                    fromIndex -= (long) segmentByteCount;
                } else {
                    byte[] data = s.data;
                    int limit = s.limit;
                    for (int pos = (int) (((long) s.pos) + fromIndex); pos < limit; pos++) {
                        if (data[pos] == b) {
                            return (((long) pos) + offset) - ((long) s.pos);
                        }
                    }
                    fromIndex = 0;
                }
                offset += (long) segmentByteCount;
                s = s.next;
            } while (s != this.head);
            return -1;
        }
        throw new IllegalArgumentException("fromIndex < 0");
    }

    public long indexOf(ByteString bytes) throws IOException {
        return indexOf(bytes, 0);
    }

    public long indexOf(ByteString bytes, long fromIndex) throws IOException {
        if (bytes.size() != 0) {
            while (true) {
                fromIndex = indexOf(bytes.getByte(0), fromIndex);
                if (fromIndex == -1) {
                    return -1;
                }
                if (rangeEquals(fromIndex, bytes)) {
                    return fromIndex;
                }
                fromIndex++;
            }
        } else {
            throw new IllegalArgumentException("bytes is empty");
        }
    }

    public long indexOfElement(ByteString targetBytes) {
        return indexOfElement(targetBytes, 0);
    }

    public long indexOfElement(ByteString targetBytes, long fromIndex) {
        if (fromIndex >= 0) {
            Segment s = this.head;
            if (s == null) {
                return -1;
            }
            long offset = 0;
            byte[] toFind = targetBytes.toByteArray();
            long fromIndex2 = fromIndex;
            while (true) {
                byte[] toFind2;
                int segmentByteCount = s.limit - s.pos;
                if (fromIndex2 >= ((long) segmentByteCount)) {
                    fromIndex2 -= (long) segmentByteCount;
                    toFind2 = toFind;
                } else {
                    long fromIndex3;
                    byte[] data = s.data;
                    long pos = ((long) s.pos) + fromIndex2;
                    long limit = (long) s.limit;
                    while (pos < limit) {
                        byte b = data[(int) pos];
                        int length = toFind.length;
                        int i = 0;
                        while (i < length) {
                            fromIndex3 = fromIndex2;
                            byte targetByte = toFind[i];
                            if (b == targetByte) {
                                return (offset + pos) - ((long) s.pos);
                            }
                            i++;
                            fromIndex2 = fromIndex3;
                        }
                        pos++;
                        fromIndex2 = fromIndex2;
                        toFind = toFind;
                    }
                    toFind2 = toFind;
                    fromIndex3 = fromIndex2;
                    fromIndex2 = 0;
                }
                offset += (long) segmentByteCount;
                s = s.next;
                if (s == this.head) {
                    return -1;
                }
                toFind = toFind2;
            }
        } else {
            throw new IllegalArgumentException("fromIndex < 0");
        }
    }

    boolean rangeEquals(long offset, ByteString bytes) {
        int byteCount = bytes.size();
        if (this.size - offset < ((long) byteCount)) {
            return false;
        }
        for (int i = 0; i < byteCount; i++) {
            if (getByte(((long) i) + offset) != bytes.getByte(i)) {
                return false;
            }
        }
        return true;
    }

    public void flush() {
    }

    public void close() {
    }

    public Timeout timeout() {
        return Timeout.NONE;
    }

    List<Integer> segmentSizes() {
        if (this.head == null) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList();
        result.add(Integer.valueOf(this.head.limit - this.head.pos));
        Segment s = this.head;
        while (true) {
            s = s.next;
            if (s == this.head) {
                return result;
            }
            result.add(Integer.valueOf(s.limit - s.pos));
        }
    }

    public boolean equals(Object o) {
        Buffer buffer = o;
        if (this == buffer) {
            return true;
        }
        if (!(buffer instanceof Buffer)) {
            return false;
        }
        Buffer that = buffer;
        if (this.size != that.size) {
            return false;
        }
        long pos = 0;
        if (this.size == 0) {
            return true;
        }
        Segment sa = this.head;
        Segment sb = that.head;
        int posA = sa.pos;
        int posB = sb.pos;
        while (pos < this.size) {
            long count = (long) Math.min(sa.limit - posA, sb.limit - posB);
            int posB2 = posB;
            posB = posA;
            posA = 0;
            while (((long) posA) < count) {
                int posA2 = posB + 1;
                int posB3 = posB2 + 1;
                if (sa.data[posB] != sb.data[posB2]) {
                    return false;
                }
                posA++;
                posB = posA2;
                posB2 = posB3;
            }
            if (posB == sa.limit) {
                sa = sa.next;
                posA = sa.pos;
            } else {
                posA = posB;
            }
            if (posB2 == sb.limit) {
                sb = sb.next;
                posB = sb.pos;
            } else {
                posB = posB2;
            }
            pos += count;
        }
        return true;
    }

    public int hashCode() {
        Segment s = this.head;
        if (s == null) {
            return 0;
        }
        int result = 1;
        do {
            for (int pos = s.pos; pos < s.limit; pos++) {
                result = (31 * result) + s.data[pos];
            }
            s = s.next;
        } while (s != this.head);
        return result;
    }

    public String toString() {
        if (this.size == 0) {
            return "Buffer[size=0]";
        }
        if (this.size <= 16) {
            ByteString data = clone().readByteString();
            return String.format("Buffer[size=%s data=%s]", new Object[]{Long.valueOf(this.size), data.hex()});
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(this.head.data, this.head.pos, this.head.limit - this.head.pos);
            for (Segment s = this.head.next; s != this.head; s = s.next) {
                md5.update(s.data, s.pos, s.limit - s.pos);
            }
            return String.format("Buffer[size=%s md5=%s]", new Object[]{Long.valueOf(this.size), ByteString.of(md5.digest()).hex()});
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
    }

    public Buffer clone() {
        Buffer result = new Buffer();
        if (this.size == 0) {
            return result;
        }
        result.head = new Segment(this.head);
        Segment segment = result.head;
        Segment segment2 = result.head;
        Segment segment3 = result.head;
        segment2.prev = segment3;
        segment.next = segment3;
        segment = this.head;
        while (true) {
            segment = segment.next;
            if (segment != this.head) {
                result.head.prev.push(new Segment(segment));
            } else {
                result.size = this.size;
                return result;
            }
        }
    }

    public ByteString snapshot() {
        if (this.size <= 2147483647L) {
            return snapshot((int) this.size);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("size > Integer.MAX_VALUE: ");
        stringBuilder.append(this.size);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ByteString snapshot(int byteCount) {
        if (byteCount == 0) {
            return ByteString.EMPTY;
        }
        return new SegmentedByteString(this, byteCount);
    }
}
