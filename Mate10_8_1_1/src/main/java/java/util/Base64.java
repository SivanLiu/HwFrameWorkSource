package java.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Base64 {

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

        public int read(byte[] r11, int r12, int r13) throws java.io.IOException {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxOverflowException: Regions stack size limit reached
	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:37)
	at jadx.core.utils.ErrorsCounter.methodError(ErrorsCounter.java:61)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r10 = this;
            r9 = 12;
            r8 = 1;
            r7 = 18;
            r6 = -1;
            r5 = 0;
            r3 = r10.closed;
            if (r3 == 0) goto L_0x0014;
        L_0x000b:
            r3 = new java.io.IOException;
            r4 = "Stream is closed";
            r3.<init>(r4);
            throw r3;
        L_0x0014:
            r3 = r10.eof;
            if (r3 == 0) goto L_0x001d;
        L_0x0018:
            r3 = r10.nextout;
            if (r3 >= 0) goto L_0x001d;
        L_0x001c:
            return r6;
        L_0x001d:
            if (r12 < 0) goto L_0x0021;
        L_0x001f:
            if (r13 >= 0) goto L_0x0027;
        L_0x0021:
            r3 = new java.lang.IndexOutOfBoundsException;
            r3.<init>();
            throw r3;
        L_0x0027:
            r3 = r11.length;
            r3 = r3 - r12;
            if (r13 > r3) goto L_0x0021;
        L_0x002b:
            r1 = r12;
            r3 = r10.nextout;
            if (r3 < 0) goto L_0x0157;
        L_0x0030:
            r0 = r12;
            if (r13 != 0) goto L_0x0036;
        L_0x0033:
            r3 = r0 - r1;
            return r3;
        L_0x0036:
            r12 = r0 + 1;
            r3 = r10.bits;
            r4 = r10.nextout;
            r3 = r3 >> r4;
            r3 = (byte) r3;
            r11[r0] = r3;
            r13 = r13 + -1;
            r3 = r10.nextout;
            r3 = r3 + -8;
            r10.nextout = r3;
            r3 = r10.nextout;
            if (r3 >= 0) goto L_0x0030;
        L_0x004c:
            r10.bits = r5;
            r0 = r12;
        L_0x004f:
            if (r13 <= 0) goto L_0x0165;
        L_0x0051:
            r3 = r10.is;
            r2 = r3.read();
            if (r2 != r6) goto L_0x0096;
        L_0x0059:
            r10.eof = r8;
            r3 = r10.nextin;
            if (r3 == r7) goto L_0x0162;
        L_0x005f:
            r3 = r10.nextin;
            if (r3 != r9) goto L_0x006c;
        L_0x0063:
            r3 = new java.io.IOException;
            r4 = "Base64 stream has one un-decoded dangling byte.";
            r3.<init>(r4);
            throw r3;
        L_0x006c:
            r12 = r0 + 1;
            r3 = r10.bits;
            r3 = r3 >> 16;
            r3 = (byte) r3;
            r11[r0] = r3;
            r13 = r13 + -1;
            r3 = r10.nextin;
            if (r3 != 0) goto L_0x0085;
        L_0x007b:
            if (r13 != 0) goto L_0x0088;
        L_0x007d:
            r3 = r10.bits;
            r3 = r3 >> 8;
            r10.bits = r3;
            r10.nextout = r5;
        L_0x0085:
            if (r12 != r1) goto L_0x0093;
        L_0x0087:
            return r6;
        L_0x0088:
            r0 = r12 + 1;
            r3 = r10.bits;
            r3 = r3 >> 8;
            r3 = (byte) r3;
            r11[r12] = r3;
            r12 = r0;
            goto L_0x0085;
        L_0x0093:
            r3 = r12 - r1;
            return r3;
        L_0x0096:
            r3 = 61;
            if (r2 != r3) goto L_0x00f6;
        L_0x009a:
            r3 = r10.nextin;
            if (r3 == r7) goto L_0x00a2;
        L_0x009e:
            r3 = r10.nextin;
            if (r3 != r9) goto L_0x00be;
        L_0x00a2:
            r3 = new java.io.IOException;
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r5 = "Illegal base64 ending sequence:";
            r4 = r4.append(r5);
            r5 = r10.nextin;
            r4 = r4.append(r5);
            r4 = r4.toString();
            r3.<init>(r4);
            throw r3;
        L_0x00be:
            r3 = r10.nextin;
            r4 = 6;
            if (r3 != r4) goto L_0x00cd;
        L_0x00c3:
            r3 = r10.is;
            r3 = r3.read();
            r4 = 61;
            if (r3 != r4) goto L_0x00a2;
        L_0x00cd:
            r12 = r0 + 1;
            r3 = r10.bits;
            r3 = r3 >> 16;
            r3 = (byte) r3;
            r11[r0] = r3;
            r13 = r13 + -1;
            r3 = r10.nextin;
            if (r3 != 0) goto L_0x00e6;
        L_0x00dc:
            if (r13 != 0) goto L_0x00eb;
        L_0x00de:
            r3 = r10.bits;
            r3 = r3 >> 8;
            r10.bits = r3;
            r10.nextout = r5;
        L_0x00e6:
            r10.eof = r8;
        L_0x00e8:
            r3 = r12 - r1;
            return r3;
        L_0x00eb:
            r0 = r12 + 1;
            r3 = r10.bits;
            r3 = r3 >> 8;
            r3 = (byte) r3;
            r11[r12] = r3;
            r12 = r0;
            goto L_0x00e6;
        L_0x00f6:
            r3 = r10.base64;
            r2 = r3[r2];
            if (r2 != r6) goto L_0x0120;
        L_0x00fc:
            r3 = r10.isMIME;
            if (r3 != 0) goto L_0x004f;
        L_0x0100:
            r3 = new java.io.IOException;
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r5 = "Illegal base64 character ";
            r4 = r4.append(r5);
            r5 = 16;
            r5 = java.lang.Integer.toString(r2, r5);
            r4 = r4.append(r5);
            r4 = r4.toString();
            r3.<init>(r4);
            throw r3;
        L_0x0120:
            r3 = r10.bits;
            r4 = r10.nextin;
            r4 = r2 << r4;
            r3 = r3 | r4;
            r10.bits = r3;
            r3 = r10.nextin;
            if (r3 != 0) goto L_0x015a;
        L_0x012d:
            r10.nextin = r7;
            r3 = 16;
            r10.nextout = r3;
            r12 = r0;
        L_0x0134:
            r3 = r10.nextout;
            if (r3 < 0) goto L_0x0155;
        L_0x0138:
            r0 = r12 + 1;
            r3 = r10.bits;
            r4 = r10.nextout;
            r3 = r3 >> r4;
            r3 = (byte) r3;
            r11[r12] = r3;
            r13 = r13 + -1;
            r3 = r10.nextout;
            r3 = r3 + -8;
            r10.nextout = r3;
            if (r13 != 0) goto L_0x0153;
        L_0x014c:
            r3 = r10.nextout;
            if (r3 < 0) goto L_0x0153;
        L_0x0150:
            r3 = r0 - r1;
            return r3;
        L_0x0153:
            r12 = r0;
            goto L_0x0134;
        L_0x0155:
            r10.bits = r5;
        L_0x0157:
            r0 = r12;
            goto L_0x004f;
        L_0x015a:
            r3 = r10.nextin;
            r3 = r3 + -6;
            r10.nextin = r3;
            r12 = r0;
            goto L_0x0157;
        L_0x0162:
            r12 = r0;
            goto L_0x0085;
        L_0x0165:
            r12 = r0;
            goto L_0x00e8;
            */
            throw new UnsupportedOperationException("Method not decompiled: java.util.Base64.DecInputStream.read(byte[], int, int):int");
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
            int i;
            Arrays.fill(fromBase64, -1);
            for (i = 0; i < Encoder.toBase64.length; i++) {
                fromBase64[Encoder.toBase64[i]] = i;
            }
            fromBase64[61] = -2;
            Arrays.fill(fromBase64URL, -1);
            for (i = 0; i < Encoder.toBase64URL.length; i++) {
                fromBase64URL[Encoder.toBase64URL[i]] = i;
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
            if (len == 0) {
                return 0;
            }
            if (len >= 2) {
                if (this.isMIME) {
                    int n = 0;
                    int sp2 = sp;
                    while (sp2 < sl) {
                        sp = sp2 + 1;
                        int b = src[sp2] & 255;
                        if (b == 61) {
                            len -= (sl - sp) + 1;
                            break;
                        }
                        if (base64[b] == -1) {
                            n++;
                        }
                        sp2 = sp;
                    }
                    len -= n;
                } else if (src[sl - 1] == (byte) 61) {
                    paddings = 1;
                    if (src[sl - 2] == (byte) 61) {
                        paddings = 1 + 1;
                    }
                }
                if (paddings == 0 && (len & 3) != 0) {
                    paddings = 4 - (len & 3);
                }
                return (((len + 3) / 4) * 3) - paddings;
            } else if (this.isMIME && base64[0] == -1) {
                return 0;
            } else {
                throw new IllegalArgumentException("Input byte[] should at least have 2 bytes for base64 bytes");
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int decode0(byte[] src, int sp, int sl, byte[] dst) {
            int i;
            int[] base64 = this.isURL ? fromBase64URL : fromBase64;
            int bits = 0;
            int shiftto = 18;
            int dp = 0;
            int sp2 = sp;
            while (sp2 < sl) {
                sp = sp2 + 1;
                int b = base64[src[sp2] & 255];
                if (b >= 0) {
                    bits |= b << shiftto;
                    shiftto -= 6;
                    if (shiftto < 0) {
                        i = dp + 1;
                        dst[dp] = (byte) (bits >> 16);
                        dp = i + 1;
                        dst[i] = (byte) (bits >> 8);
                        i = dp + 1;
                        dst[dp] = (byte) bits;
                        shiftto = 18;
                        bits = 0;
                    } else {
                        i = dp;
                    }
                    dp = i;
                    sp2 = sp;
                } else if (b == -2) {
                    if (shiftto == 6) {
                        if (sp != sl) {
                            sp2 = sp + 1;
                            if (src[sp] != (byte) 61) {
                                sp = sp2;
                            } else {
                                sp = sp2;
                            }
                        }
                        throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                    }
                } else if (this.isMIME) {
                    sp2 = sp;
                } else {
                    throw new IllegalArgumentException("Illegal base64 character " + Integer.toString(src[sp - 1], 16));
                }
            }
            sp = sp2;
            if (shiftto == 6) {
                i = dp + 1;
                dst[dp] = (byte) (bits >> 16);
                sp2 = sp;
            } else if (shiftto == 0) {
                i = dp + 1;
                dst[dp] = (byte) (bits >> 16);
                dp = i + 1;
                dst[i] = (byte) (bits >> 8);
                i = dp;
                sp2 = sp;
            } else if (shiftto == 12) {
                throw new IllegalArgumentException("Last unit does not have enough valid bits");
            } else {
                i = dp;
                sp2 = sp;
            }
            while (sp2 < sl) {
                if (this.isMIME) {
                    sp = sp2 + 1;
                    if (base64[src[sp2]] < 0) {
                        sp2 = sp;
                    }
                } else {
                    sp = sp2;
                }
                throw new IllegalArgumentException("Input byte array has incorrect ending byte at " + sp);
            }
            return i;
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
                int nBits24 = len / 3;
                this.leftover = len - (nBits24 * 3);
                int nBits242 = nBits24;
                off2 = off;
                while (true) {
                    nBits24 = nBits242 - 1;
                    if (nBits242 <= 0) {
                        break;
                    }
                    checkNewline();
                    off = off2 + 1;
                    off2 = off + 1;
                    off = off2 + 1;
                    int bits = (((b[off2] & 255) << 16) | ((b[off] & 255) << 8)) | (b[off2] & 255);
                    this.out.write(this.base64[(bits >>> 18) & 63]);
                    this.out.write(this.base64[(bits >>> 12) & 63]);
                    this.out.write(this.base64[(bits >>> 6) & 63]);
                    this.out.write(this.base64[bits & 63]);
                    this.linepos += 4;
                    nBits242 = nBits24;
                    off2 = off;
                }
                if (this.leftover == 1) {
                    off = off2 + 1;
                    this.b0 = b[off2] & 255;
                } else if (this.leftover == 2) {
                    off = off2 + 1;
                    this.b0 = b[off2] & 255;
                    off2 = off + 1;
                    this.b1 = b[off] & 255;
                    off = off2;
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
            int i = 0;
            if (this.doPadding) {
                len = ((srclen + 2) / 3) * 4;
            } else {
                int n = srclen % 3;
                int i2 = (srclen / 3) * 4;
                if (n != 0) {
                    i = n + 1;
                }
                len = i2 + i;
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
            char[] base64 = this.isURL ? toBase64URL : toBase64;
            int sp = off;
            int slen = ((end - off) / 3) * 3;
            int sl = off + slen;
            if (this.linemax > 0 && slen > (this.linemax / 4) * 3) {
                slen = (this.linemax / 4) * 3;
            }
            int dp = 0;
            int sp2 = sp;
            while (sp2 < sl) {
                int sl0 = Math.min(sp2 + slen, sl);
                int dp0 = dp;
                int sp0 = sp2;
                while (sp0 < sl0) {
                    int sp02 = sp0 + 1;
                    sp0 = sp02 + 1;
                    sp02 = sp0 + 1;
                    int bits = (((src[sp0] & 255) << 16) | ((src[sp02] & 255) << 8)) | (src[sp0] & 255);
                    int i = dp0 + 1;
                    dst[dp0] = (byte) base64[(bits >>> 18) & 63];
                    dp0 = i + 1;
                    dst[i] = (byte) base64[(bits >>> 12) & 63];
                    i = dp0 + 1;
                    dst[dp0] = (byte) base64[(bits >>> 6) & 63];
                    dp0 = i + 1;
                    dst[i] = (byte) base64[bits & 63];
                    sp0 = sp02;
                }
                int dlen = ((sl0 - sp2) / 3) * 4;
                int dp2 = dp + dlen;
                sp = sl0;
                if (dlen == this.linemax && sl0 < end) {
                    byte[] bArr = this.newline;
                    int i2 = 0;
                    int length = bArr.length;
                    dp = dp2;
                    while (i2 < length) {
                        dp2 = dp + 1;
                        dst[dp] = bArr[i2];
                        i2++;
                        dp = dp2;
                    }
                    dp2 = dp;
                }
                dp = dp2;
                sp2 = sp;
            }
            if (sp2 < end) {
                sp = sp2 + 1;
                int b0 = src[sp2] & 255;
                dp2 = dp + 1;
                dst[dp] = (byte) base64[b0 >> 2];
                if (sp == end) {
                    dp = dp2 + 1;
                    dst[dp2] = (byte) base64[(b0 << 4) & 63];
                    if (!this.doPadding) {
                        return dp;
                    }
                    dp2 = dp + 1;
                    dst[dp] = (byte) 61;
                    dp = dp2 + 1;
                    dst[dp2] = (byte) 61;
                    return dp;
                }
                sp2 = sp + 1;
                int b1 = src[sp] & 255;
                dp = dp2 + 1;
                dst[dp2] = (byte) base64[((b0 << 4) & 63) | (b1 >> 4)];
                dp2 = dp + 1;
                dst[dp] = (byte) base64[(b1 << 2) & 63];
                if (this.doPadding) {
                    dp = dp2 + 1;
                    dst[dp2] = (byte) 61;
                    sp = sp2;
                    return dp;
                }
                return dp2;
            }
            sp = sp2;
            return dp;
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
        for (byte b : lineSeparator) {
            if (base64[b & 255] != -1) {
                throw new IllegalArgumentException("Illegal base64 line separator character 0x" + Integer.toString(b, 16));
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
