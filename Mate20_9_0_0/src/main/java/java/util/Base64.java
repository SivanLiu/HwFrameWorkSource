package java.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Base64 {

    public static class Decoder {
        static final Decoder RFC2045 = new Decoder(false, true);
        static final Decoder RFC4648 = new Decoder(false, false);
        static final Decoder RFC4648_URLSAFE = new Decoder(true, false);
        private static final int[] fromBase64 = new int[256];
        private static final int[] fromBase64URL = new int[256];
        private final boolean isMIME;
        private final boolean isURL;

        private Decoder(boolean isURL, boolean isMIME) {
            this.isURL = isURL;
            this.isMIME = isMIME;
        }

        static {
            Arrays.fill(fromBase64, -1);
            for (int i = 0; i < Encoder.toBase64.length; i++) {
                fromBase64[Encoder.toBase64[i]] = i;
            }
            fromBase64[61] = -2;
            Arrays.fill(fromBase64URL, -1);
            for (int i2 = 0; i2 < Encoder.toBase64URL.length; i2++) {
                fromBase64URL[Encoder.toBase64URL[i2]] = i2;
            }
            fromBase64URL[61] = -2;
        }

        public byte[] decode(byte[] src) {
            byte[] dst = new byte[outLength(src, 0, src.length)];
            int ret = decode0(src, 0, src.length, dst);
            if (ret != dst.length) {
                return Arrays.copyOf(dst, ret);
            }
            return dst;
        }

        public byte[] decode(String src) {
            return decode(src.getBytes(StandardCharsets.ISO_8859_1));
        }

        public int decode(byte[] src, byte[] dst) {
            if (dst.length >= outLength(src, 0, src.length)) {
                return decode0(src, 0, src.length, dst);
            }
            throw new IllegalArgumentException("Output byte array is too small for decoding all input bytes");
        }

        public ByteBuffer decode(ByteBuffer buffer) {
            int pos0 = buffer.position();
            try {
                byte[] src;
                int sp;
                int sl;
                if (buffer.hasArray()) {
                    src = buffer.array();
                    sp = buffer.arrayOffset() + buffer.position();
                    sl = buffer.arrayOffset() + buffer.limit();
                    buffer.position(buffer.limit());
                } else {
                    src = new byte[buffer.remaining()];
                    buffer.get(src);
                    sp = 0;
                    sl = src.length;
                }
                byte[] dst = new byte[outLength(src, sp, sl)];
                return ByteBuffer.wrap(dst, 0, decode0(src, sp, sl, dst));
            } catch (IllegalArgumentException iae) {
                buffer.position(pos0);
                throw iae;
            }
        }

        public InputStream wrap(InputStream is) {
            Objects.requireNonNull(is);
            return new DecInputStream(is, this.isURL ? fromBase64URL : fromBase64, this.isMIME);
        }

        private int outLength(byte[] src, int sp, int sl) {
            int[] base64 = this.isURL ? fromBase64URL : fromBase64;
            int paddings = 0;
            int len = sl - sp;
            int n = 0;
            if (len == 0) {
                return 0;
            }
            if (len >= 2) {
                if (this.isMIME) {
                    while (sp < sl) {
                        int sp2 = sp + 1;
                        sp = src[sp] & 255;
                        if (sp == 61) {
                            len -= (sl - sp2) + 1;
                            sp = sp2;
                            break;
                        }
                        int i = base64[sp];
                        sp = i;
                        if (i == -1) {
                            n++;
                        }
                        sp = sp2;
                    }
                    len -= n;
                } else if (src[sl - 1] == (byte) 61) {
                    paddings = 0 + 1;
                    if (src[sl - 2] == (byte) 61) {
                        paddings++;
                    }
                }
                if (paddings == 0 && (len & 3) != 0) {
                    paddings = 4 - (len & 3);
                }
                return (3 * ((len + 3) / 4)) - paddings;
            } else if (this.isMIME && base64[0] == -1) {
                return 0;
            } else {
                throw new IllegalArgumentException("Input byte[] should at least have 2 bytes for base64 bytes");
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:31:0x008f  */
        /* JADX WARNING: Removed duplicated region for block: B:30:0x0086  */
        /* JADX WARNING: Removed duplicated region for block: B:36:0x00a6  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int decode0(byte[] src, int sp, int sl, byte[] dst) {
            int[] base64 = this.isURL ? fromBase64URL : fromBase64;
            int bits = 0;
            int dp = 0;
            int sp2 = sp;
            sp = 18;
            while (sp2 < sl) {
                int sp3 = sp2 + 1;
                int i = base64[src[sp2] & 255];
                sp2 = i;
                StringBuilder stringBuilder;
                if (i >= 0) {
                    bits |= sp2 << sp;
                    sp -= 6;
                    if (sp < 0) {
                        int dp2 = dp + 1;
                        dst[dp] = (byte) (bits >> 16);
                        dp = dp2 + 1;
                        dst[dp2] = (byte) (bits >> 8);
                        dp2 = dp + 1;
                        dst[dp] = (byte) bits;
                        sp = 18;
                        bits = 0;
                        dp = dp2;
                    }
                } else if (sp2 == -2) {
                    if (sp == 6) {
                        if (sp3 != sl) {
                            i = sp3 + 1;
                            if (src[sp3] == (byte) 61) {
                                sp3 = i;
                            } else {
                                sp3 = i;
                            }
                        }
                        throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                    }
                    if (sp != 18) {
                        int dp3;
                        sp2 = sp3;
                        if (sp != 6) {
                            dp3 = dp + 1;
                            dst[dp] = (byte) (bits >> 16);
                            dp = dp3;
                        } else if (sp == 0) {
                            dp3 = dp + 1;
                            dst[dp] = (byte) (bits >> 16);
                            dp = dp3 + 1;
                            dst[dp3] = (byte) (bits >> 8);
                        } else if (sp == 12) {
                            throw new IllegalArgumentException("Last unit does not have enough valid bits");
                        }
                        while (sp2 < sl) {
                            if (this.isMIME) {
                                dp3 = sp2 + 1;
                                if (base64[src[sp2]] < 0) {
                                    sp2 = dp3;
                                } else {
                                    sp2 = dp3;
                                }
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Input byte array has incorrect ending byte at ");
                            stringBuilder.append(sp2);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                        return dp;
                    }
                    throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                } else if (!this.isMIME) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal base64 character ");
                    stringBuilder.append(Integer.toString(src[sp3 - 1], 16));
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                sp2 = sp3;
            }
            if (sp != 6) {
            }
            while (sp2 < sl) {
            }
            return dp;
        }
    }

    public static class Encoder {
        private static final byte[] CRLF = new byte[]{(byte) 13, (byte) 10};
        private static final int MIMELINEMAX = 76;
        static final Encoder RFC2045 = new Encoder(false, CRLF, MIMELINEMAX, true);
        static final Encoder RFC4648 = new Encoder(false, null, -1, true);
        static final Encoder RFC4648_URLSAFE = new Encoder(true, null, -1, true);
        private static final char[] toBase64 = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
        private static final char[] toBase64URL = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};
        private final boolean doPadding;
        private final boolean isURL;
        private final int linemax;
        private final byte[] newline;

        private Encoder(boolean isURL, byte[] newline, int linemax, boolean doPadding) {
            this.isURL = isURL;
            this.newline = newline;
            this.linemax = linemax;
            this.doPadding = doPadding;
        }

        private final int outLength(int srclen) {
            int len;
            if (this.doPadding) {
                len = 4 * ((srclen + 2) / 3);
            } else {
                int n = srclen % 3;
                len = (4 * (srclen / 3)) + (n == 0 ? 0 : n + 1);
            }
            if (this.linemax > 0) {
                return len + (((len - 1) / this.linemax) * this.newline.length);
            }
            return len;
        }

        public byte[] encode(byte[] src) {
            byte[] dst = new byte[outLength(src.length)];
            int ret = encode0(src, 0, src.length, dst);
            if (ret != dst.length) {
                return Arrays.copyOf(dst, ret);
            }
            return dst;
        }

        public int encode(byte[] src, byte[] dst) {
            if (dst.length >= outLength(src.length)) {
                return encode0(src, 0, src.length, dst);
            }
            throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
        }

        public String encodeToString(byte[] src) {
            byte[] encoded = encode(src);
            return new String(encoded, 0, 0, encoded.length);
        }

        public ByteBuffer encode(ByteBuffer buffer) {
            int ret;
            byte[] dst = new byte[outLength(buffer.remaining())];
            if (buffer.hasArray()) {
                ret = encode0(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.arrayOffset() + buffer.limit(), dst);
                buffer.position(buffer.limit());
            } else {
                byte[] src = new byte[buffer.remaining()];
                buffer.get(src);
                ret = encode0(src, 0, src.length, dst);
            }
            if (ret != dst.length) {
                dst = Arrays.copyOf(dst, ret);
            }
            return ByteBuffer.wrap(dst);
        }

        public OutputStream wrap(OutputStream os) {
            Objects.requireNonNull(os);
            return new EncOutputStream(os, this.isURL ? toBase64URL : toBase64, this.newline, this.linemax, this.doPadding);
        }

        public Encoder withoutPadding() {
            if (this.doPadding) {
                return new Encoder(this.isURL, this.newline, this.linemax, false);
            }
            return this;
        }

        private int encode0(byte[] src, int off, int end, byte[] dst) {
            int sl0;
            int sp0;
            int dp0;
            int dp02;
            int i = end;
            char[] base64 = this.isURL ? toBase64URL : toBase64;
            int sp = off;
            int slen = ((i - off) / 3) * 3;
            int sl = off + slen;
            if (this.linemax > 0 && slen > (this.linemax / 4) * 3) {
                slen = (this.linemax / 4) * 3;
            }
            int sp2 = sp;
            sp = 0;
            while (sp2 < sl) {
                int sp02;
                sl0 = Math.min(sp2 + slen, sl);
                sp0 = sp2;
                dp0 = sp;
                while (sp0 < sl0) {
                    sp02 = sp0 + 1;
                    int sp03 = sp02 + 1;
                    sp0 = ((src[sp0] & 255) << 16) | ((src[sp02] & 255) << 8);
                    sp02 = sp03 + 1;
                    sp0 |= src[sp03] & 255;
                    sp03 = dp0 + 1;
                    dst[dp0] = (byte) base64[(sp0 >>> 18) & 63];
                    dp02 = sp03 + 1;
                    dst[sp03] = (byte) base64[(sp0 >>> 12) & 63];
                    dp0 = dp02 + 1;
                    dst[dp02] = (byte) base64[(sp0 >>> 6) & 63];
                    dp02 = dp0 + 1;
                    dst[dp0] = (byte) base64[sp0 & 63];
                    dp0 = dp02;
                    sp0 = sp02;
                }
                dp02 = ((sl0 - sp2) / 3) * 4;
                sp += dp02;
                sp2 = sl0;
                if (dp02 == this.linemax && sp2 < i) {
                    byte[] bArr = this.newline;
                    sp0 = bArr.length;
                    sp02 = sp;
                    sp = 0;
                    while (sp < sp0) {
                        int dp = sp02 + 1;
                        dst[sp02] = bArr[sp];
                        sp++;
                        sp02 = dp;
                    }
                    sp = sp02;
                }
            }
            if (sp2 < i) {
                dp02 = sp2 + 1;
                sp2 = src[sp2] & 255;
                sl0 = sp + 1;
                dst[sp] = (byte) base64[sp2 >> 2];
                if (dp02 == i) {
                    dp0 = sl0 + 1;
                    dst[sl0] = (byte) base64[(sp2 << 4) & 63];
                    if (!this.doPadding) {
                        return dp0;
                    }
                    sl0 = dp0 + 1;
                    dst[dp0] = (byte) 61;
                    dp0 = sl0 + 1;
                    dst[sl0] = (byte) 61;
                    return dp0;
                }
                dp0 = dp02 + 1;
                dp02 = src[dp02] & 255;
                sp0 = sl0 + 1;
                dst[sl0] = (byte) base64[((sp2 << 4) & 63) | (dp02 >> 4)];
                sl0 = sp0 + 1;
                dst[sp0] = (byte) base64[(dp02 << 2) & 63];
                if (this.doPadding) {
                    sp0 = sl0 + 1;
                    dst[sl0] = (byte) 61;
                    dp02 = dp0;
                    return sp0;
                }
                dp02 = dp0;
                return sl0;
            }
            return sp;
        }
    }

    private static class DecInputStream extends InputStream {
        private final int[] base64;
        private int bits = 0;
        private boolean closed = false;
        private boolean eof = false;
        private final InputStream is;
        private final boolean isMIME;
        private int nextin = 18;
        private int nextout = -8;
        private byte[] sbBuf = new byte[1];

        DecInputStream(InputStream is, int[] base64, boolean isMIME) {
            this.is = is;
            this.base64 = base64;
            this.isMIME = isMIME;
        }

        public int read() throws IOException {
            return read(this.sbBuf, 0, 1) == -1 ? -1 : this.sbBuf[0] & 255;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (this.closed) {
                throw new IOException("Stream is closed");
            } else if (this.eof && this.nextout < 0) {
                return -1;
            } else {
                if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                }
                int off2;
                int oldOff = off;
                if (this.nextout >= 0) {
                    while (len != 0) {
                        off2 = off + 1;
                        b[off] = (byte) (this.bits >> this.nextout);
                        len--;
                        this.nextout -= 8;
                        if (this.nextout < 0) {
                            this.bits = 0;
                            off = off2;
                        } else {
                            off = off2;
                        }
                    }
                    return off - oldOff;
                }
                while (len > 0) {
                    off2 = this.is.read();
                    int off3;
                    StringBuilder stringBuilder;
                    if (off2 == -1) {
                        this.eof = true;
                        if (this.nextin != 18) {
                            if (this.nextin != 12) {
                                off3 = off + 1;
                                b[off] = (byte) (this.bits >> 16);
                                len--;
                                if (this.nextin == 0) {
                                    if (len == 0) {
                                        this.bits >>= 8;
                                        this.nextout = 0;
                                    } else {
                                        off = off3 + 1;
                                        b[off3] = (byte) (this.bits >> 8);
                                    }
                                }
                                off = off3;
                            } else {
                                throw new IOException("Base64 stream has one un-decoded dangling byte.");
                            }
                        }
                        if (off == oldOff) {
                            return -1;
                        }
                        return off - oldOff;
                    } else if (off2 != 61) {
                        off3 = this.base64[off2];
                        off2 = off3;
                        if (off3 != -1) {
                            this.bits |= off2 << this.nextin;
                            if (this.nextin == 0) {
                                this.nextin = 18;
                                this.nextout = 16;
                                while (this.nextout >= 0) {
                                    off3 = off + 1;
                                    b[off] = (byte) (this.bits >> this.nextout);
                                    len--;
                                    this.nextout -= 8;
                                    if (len == 0 && this.nextout >= 0) {
                                        return off3 - oldOff;
                                    }
                                    off = off3;
                                }
                                this.bits = 0;
                            } else {
                                this.nextin -= 6;
                            }
                        } else if (!this.isMIME) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Illegal base64 character ");
                            stringBuilder.append(Integer.toString(off2, 16));
                            throw new IOException(stringBuilder.toString());
                        }
                    } else if (this.nextin == 18 || this.nextin == 12 || (this.nextin == 6 && this.is.read() != 61)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Illegal base64 ending sequence:");
                        stringBuilder.append(this.nextin);
                        throw new IOException(stringBuilder.toString());
                    } else {
                        int off4 = off + 1;
                        b[off] = (byte) (this.bits >> 16);
                        len--;
                        if (this.nextin == 0) {
                            if (len == 0) {
                                this.bits >>= 8;
                                this.nextout = 0;
                            } else {
                                off = off4 + 1;
                                b[off4] = (byte) (this.bits >> 8);
                                this.eof = true;
                                return off - oldOff;
                            }
                        }
                        off = off4;
                        this.eof = true;
                        return off - oldOff;
                    }
                }
                return off - oldOff;
            }
        }

        public int available() throws IOException {
            if (!this.closed) {
                return this.is.available();
            }
            throw new IOException("Stream is closed");
        }

        public void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                this.is.close();
            }
        }
    }

    private static class EncOutputStream extends FilterOutputStream {
        private int b0;
        private int b1;
        private int b2;
        private final char[] base64;
        private boolean closed = false;
        private final boolean doPadding;
        private int leftover = 0;
        private final int linemax;
        private int linepos = 0;
        private final byte[] newline;

        EncOutputStream(OutputStream os, char[] base64, byte[] newline, int linemax, boolean doPadding) {
            super(os);
            this.base64 = base64;
            this.newline = newline;
            this.linemax = linemax;
            this.doPadding = doPadding;
        }

        public void write(int b) throws IOException {
            write(new byte[]{(byte) (b & 255)}, 0, 1);
        }

        private void checkNewline() throws IOException {
            if (this.linepos == this.linemax) {
                this.out.write(this.newline);
                this.linepos = 0;
            }
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (this.closed) {
                throw new IOException("Stream is closed");
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new ArrayIndexOutOfBoundsException();
            } else if (len != 0) {
                int off2;
                if (this.leftover != 0) {
                    if (this.leftover == 1) {
                        off2 = off + 1;
                        this.b1 = b[off] & 255;
                        len--;
                        if (len == 0) {
                            this.leftover++;
                            return;
                        }
                        off = off2;
                    }
                    off2 = off + 1;
                    this.b2 = b[off] & 255;
                    len--;
                    checkNewline();
                    this.out.write(this.base64[this.b0 >> 2]);
                    this.out.write(this.base64[((this.b0 << 4) & 63) | (this.b1 >> 4)]);
                    this.out.write(this.base64[((this.b1 << 2) & 63) | (this.b2 >> 6)]);
                    this.out.write(this.base64[this.b2 & 63]);
                    this.linepos += 4;
                    off = off2;
                }
                off2 = len / 3;
                this.leftover = len - (off2 * 3);
                while (true) {
                    int nBits24 = off2 - 1;
                    if (off2 <= 0) {
                        break;
                    }
                    checkNewline();
                    off2 = off + 1;
                    int off3 = off2 + 1;
                    off = ((b[off] & 255) << 16) | ((b[off2] & 255) << 8);
                    off2 = off3 + 1;
                    off |= b[off3] & 255;
                    this.out.write(this.base64[(off >>> 18) & 63]);
                    this.out.write(this.base64[(off >>> 12) & 63]);
                    this.out.write(this.base64[(off >>> 6) & 63]);
                    this.out.write(this.base64[off & 63]);
                    this.linepos += 4;
                    off = off2;
                    off2 = nBits24;
                }
                if (this.leftover == 1) {
                    off2 = off + 1;
                    this.b0 = b[off] & 255;
                    off = off2;
                } else if (this.leftover == 2) {
                    off2 = off + 1;
                    this.b0 = b[off] & 255;
                    off = off2 + 1;
                    this.b1 = b[off2] & 255;
                }
            }
        }

        public void close() throws IOException {
            if (!this.closed) {
                this.closed = true;
                if (this.leftover == 1) {
                    checkNewline();
                    this.out.write(this.base64[this.b0 >> 2]);
                    this.out.write(this.base64[(this.b0 << 4) & 63]);
                    if (this.doPadding) {
                        this.out.write(61);
                        this.out.write(61);
                    }
                } else if (this.leftover == 2) {
                    checkNewline();
                    this.out.write(this.base64[this.b0 >> 2]);
                    this.out.write(this.base64[((this.b0 << 4) & 63) | (this.b1 >> 4)]);
                    this.out.write(this.base64[(this.b1 << 2) & 63]);
                    if (this.doPadding) {
                        this.out.write(61);
                    }
                }
                this.leftover = 0;
                this.out.close();
            }
        }
    }

    private Base64() {
    }

    public static Encoder getEncoder() {
        return Encoder.RFC4648;
    }

    public static Encoder getUrlEncoder() {
        return Encoder.RFC4648_URLSAFE;
    }

    public static Encoder getMimeEncoder() {
        return Encoder.RFC2045;
    }

    public static Encoder getMimeEncoder(int lineLength, byte[] lineSeparator) {
        Objects.requireNonNull(lineSeparator);
        int[] base64 = Decoder.fromBase64;
        int length = lineSeparator.length;
        int i = 0;
        while (i < length) {
            byte b = lineSeparator[i];
            if (base64[b & 255] == -1) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal base64 line separator character 0x");
                stringBuilder.append(Integer.toString(b, 16));
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (lineLength <= 0) {
            return Encoder.RFC4648;
        }
        return new Encoder(false, lineSeparator, (lineLength >> 2) << 2, true);
    }

    public static Decoder getDecoder() {
        return Decoder.RFC4648;
    }

    public static Decoder getUrlDecoder() {
        return Decoder.RFC4648_URLSAFE;
    }

    public static Decoder getMimeDecoder() {
        return Decoder.RFC2045;
    }
}
