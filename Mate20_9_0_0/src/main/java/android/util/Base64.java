package android.util;

import java.io.UnsupportedEncodingException;

public class Base64 {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int CRLF = 4;
    public static final int DEFAULT = 0;
    public static final int NO_CLOSE = 16;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int URL_SAFE = 8;

    static abstract class Coder {
        public int op;
        public byte[] output;

        public abstract int maxOutputSize(int i);

        public abstract boolean process(byte[] bArr, int i, int i2, boolean z);

        Coder() {
        }
    }

    static class Decoder extends Coder {
        private static final int[] DECODE = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        private static final int[] DECODE_WEBSAFE = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, 63, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        private static final int EQUALS = -2;
        private static final int SKIP = -1;
        private final int[] alphabet;
        private int state;
        private int value;

        public Decoder(int flags, byte[] output) {
            this.output = output;
            this.alphabet = (flags & 8) == 0 ? DECODE : DECODE_WEBSAFE;
            this.state = 0;
            this.value = 0;
        }

        public int maxOutputSize(int len) {
            return ((len * 3) / 4) + 10;
        }

        /* JADX WARNING: Removed duplicated region for block: B:63:0x00e5 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:51:0x00ef  */
        /* JADX WARNING: Removed duplicated region for block: B:49:0x00e8  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean process(byte[] input, int offset, int len, boolean finish) {
            if (this.state == 6) {
                return false;
            }
            int p = offset;
            len += offset;
            int state = this.state;
            int value = this.value;
            int op = 0;
            byte[] output = this.output;
            int[] alphabet = this.alphabet;
            while (p < len) {
                int i;
                if (state == 0) {
                    while (p + 4 <= len) {
                        i = (((alphabet[input[p] & 255] << 18) | (alphabet[input[p + 1] & 255] << 12)) | (alphabet[input[p + 2] & 255] << 6)) | alphabet[input[p + 3] & 255];
                        value = i;
                        if (i >= 0) {
                            output[op + 2] = (byte) value;
                            output[op + 1] = (byte) (value >> 8);
                            output[op] = (byte) (value >> 16);
                            op += 3;
                            p += 4;
                        } else if (p >= len) {
                            if (finish) {
                                this.state = state;
                                this.value = value;
                                this.op = op;
                                return true;
                            }
                            int op2;
                            switch (state) {
                                case 1:
                                    this.state = 6;
                                    return false;
                                case 2:
                                    op2 = op + 1;
                                    output[op] = (byte) (value >> 4);
                                    op = op2;
                                    break;
                                case 3:
                                    op2 = op + 1;
                                    output[op] = (byte) (value >> 10);
                                    op = op2 + 1;
                                    output[op2] = (byte) (value >> 2);
                                    break;
                                case 4:
                                    this.state = 6;
                                    return false;
                            }
                            this.state = state;
                            this.op = op;
                            return true;
                        }
                    }
                    if (p >= len) {
                    }
                }
                i = p + 1;
                p = alphabet[input[p] & 255];
                switch (state) {
                    case 0:
                        if (p < 0) {
                            if (p == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        value = p;
                        state++;
                        break;
                    case 1:
                        if (p < 0) {
                            if (p == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        value = (value << 6) | p;
                        state++;
                        break;
                    case 2:
                        if (p < 0) {
                            if (p != -2) {
                                if (p == -1) {
                                    break;
                                }
                                this.state = 6;
                                return false;
                            }
                            int op3 = op + 1;
                            output[op] = (byte) (value >> 4);
                            state = 4;
                            op = op3;
                            break;
                        }
                        value = (value << 6) | p;
                        state++;
                        break;
                    case 3:
                        if (p < 0) {
                            if (p != -2) {
                                if (p == -1) {
                                    break;
                                }
                                this.state = 6;
                                return false;
                            }
                            output[op + 1] = (byte) (value >> 2);
                            output[op] = (byte) (value >> 10);
                            op += 2;
                            state = 5;
                            break;
                        }
                        value = (value << 6) | p;
                        output[op + 2] = (byte) value;
                        output[op + 1] = (byte) (value >> 8);
                        output[op] = (byte) (value >> 16);
                        op += 3;
                        state = 0;
                        break;
                    case 4:
                        if (p != -2) {
                            if (p == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        state++;
                        break;
                    case 5:
                        if (p == -1) {
                            break;
                        }
                        this.state = 6;
                        return false;
                    default:
                        break;
                }
                p = i;
            }
            if (finish) {
            }
        }
    }

    static class Encoder extends Coder {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final byte[] ENCODE = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 43, (byte) 47};
        private static final byte[] ENCODE_WEBSAFE = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 45, (byte) 95};
        public static final int LINE_GROUPS = 19;
        private final byte[] alphabet;
        private int count;
        public final boolean do_cr;
        public final boolean do_newline;
        public final boolean do_padding;
        private final byte[] tail;
        int tailLen;

        static {
            Class cls = Base64.class;
        }

        public Encoder(int flags, byte[] output) {
            this.output = output;
            boolean z = true;
            this.do_padding = (flags & 1) == 0;
            this.do_newline = (flags & 2) == 0;
            if ((flags & 4) == 0) {
                z = false;
            }
            this.do_cr = z;
            this.alphabet = (flags & 8) == 0 ? ENCODE : ENCODE_WEBSAFE;
            this.tail = new byte[2];
            this.tailLen = 0;
            this.count = this.do_newline ? 19 : -1;
        }

        public int maxOutputSize(int len) {
            return ((len * 8) / 5) + 10;
        }

        /* JADX WARNING: Removed duplicated region for block: B:20:0x009e  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean process(byte[] input, int offset, int len, boolean finish) {
            int p;
            byte[] alphabet = this.alphabet;
            byte[] output = this.output;
            int op = 0;
            int count = this.count;
            int p2 = offset;
            int len2 = len + offset;
            int v = -1;
            int p3;
            switch (this.tailLen) {
                case 1:
                    if (p2 + 2 <= len2) {
                        p3 = p2 + 1;
                        p2 = ((input[p2] & 255) << 8) | ((this.tail[0] & 255) << 16);
                        p = p3 + 1;
                        v = p2 | (input[p3] & 255);
                        this.tailLen = 0;
                        break;
                    }
                case 2:
                    if (p2 + 1 <= len2) {
                        p3 = p2 + 1;
                        v = (((this.tail[0] & 255) << 16) | ((this.tail[1] & 255) << 8)) | (input[p2] & 255);
                        this.tailLen = 0;
                        p = p3;
                        break;
                    }
                default:
                    p = p2;
                    break;
            }
            if (v != -1) {
                p2 = 0 + 1;
                output[0] = alphabet[(v >> 18) & 63];
                op = p2 + 1;
                output[p2] = alphabet[(v >> 12) & 63];
                p2 = op + 1;
                output[op] = alphabet[(v >> 6) & 63];
                op = p2 + 1;
                output[p2] = alphabet[v & 63];
                count--;
                if (count == 0) {
                    if (this.do_cr) {
                        p2 = op + 1;
                        output[op] = (byte) 13;
                        op = p2;
                    }
                    p2 = op + 1;
                    output[op] = (byte) 10;
                    count = 19;
                    op = p2;
                }
            }
            while (p + 3 <= len2) {
                v = (((input[p] & 255) << 16) | ((input[p + 1] & 255) << 8)) | (input[p + 2] & 255);
                output[op] = alphabet[(v >> 18) & 63];
                output[op + 1] = alphabet[(v >> 12) & 63];
                output[op + 2] = alphabet[(v >> 6) & 63];
                output[op + 3] = alphabet[v & 63];
                p += 3;
                op += 4;
                count--;
                if (count == 0) {
                    if (this.do_cr) {
                        p2 = op + 1;
                        output[op] = (byte) 13;
                        op = p2;
                    }
                    p2 = op + 1;
                    output[op] = (byte) 10;
                    count = 19;
                    op = p2;
                    while (p + 3 <= len2) {
                    }
                }
            }
            int p4;
            byte[] bArr;
            if (finish) {
                int t;
                int t2;
                int p5;
                if (p - this.tailLen == len2 - 1) {
                    t = 0;
                    if (this.tailLen > 0) {
                        t2 = 0 + 1;
                        p5 = p;
                        p = this.tail[0];
                        t = t2;
                    } else {
                        p5 = p + 1;
                        p = input[p];
                    }
                    v = (p & 255) << 4;
                    this.tailLen -= t;
                    p = op + 1;
                    output[op] = alphabet[(v >> 6) & 63];
                    op = p + 1;
                    output[p] = alphabet[v & 63];
                    if (this.do_padding) {
                        p = op + 1;
                        output[op] = (byte) 61;
                        op = p + 1;
                        output[p] = (byte) 61;
                    }
                    if (this.do_newline) {
                        if (this.do_cr) {
                            p = op + 1;
                            output[op] = (byte) 13;
                        } else {
                            p = op;
                        }
                        op = p + 1;
                        output[p] = (byte) 10;
                    }
                } else if (p - this.tailLen == len2 - 2) {
                    t = 0;
                    if (this.tailLen > 1) {
                        t2 = 0 + 1;
                        p5 = p;
                        p = this.tail[0];
                        t = t2;
                    } else {
                        p5 = p + 1;
                        p = input[p];
                    }
                    p = (p & 255) << 10;
                    if (this.tailLen > 0) {
                        t2 = t + 1;
                        p4 = this.tail[t];
                        t = t2;
                    } else {
                        p4 = p5 + 1;
                        int i = input[p5];
                        p5 = p4;
                        p4 = i;
                    }
                    v = p | ((p4 & 255) << 2);
                    this.tailLen -= t;
                    p = op + 1;
                    output[op] = alphabet[(v >> 12) & 63];
                    op = p + 1;
                    output[p] = alphabet[(v >> 6) & 63];
                    p = op + 1;
                    output[op] = alphabet[v & 63];
                    if (this.do_padding) {
                        op = p + 1;
                        output[p] = (byte) 61;
                        p = op;
                    }
                    if (this.do_newline) {
                        if (this.do_cr) {
                            op = p + 1;
                            output[p] = (byte) 13;
                            p = op;
                        }
                        op = p + 1;
                        output[p] = (byte) 10;
                        p = op;
                    }
                    op = p;
                } else if (this.do_newline && op > 0 && count != 19) {
                    if (this.do_cr) {
                        p4 = op + 1;
                        output[op] = (byte) 13;
                    } else {
                        p4 = op;
                    }
                    op = p4 + 1;
                    output[p4] = (byte) 10;
                }
            } else if (p == len2 - 1) {
                bArr = this.tail;
                p4 = this.tailLen;
                this.tailLen = p4 + 1;
                bArr[p4] = input[p];
            } else if (p == len2 - 2) {
                bArr = this.tail;
                p4 = this.tailLen;
                this.tailLen = p4 + 1;
                bArr[p4] = input[p];
                bArr = this.tail;
                p4 = this.tailLen;
                this.tailLen = p4 + 1;
                bArr[p4] = input[p + 1];
            }
            this.op = op;
            this.count = count;
            return true;
        }
    }

    public static byte[] decode(String str, int flags) {
        return decode(str.getBytes(), flags);
    }

    public static byte[] decode(byte[] input, int flags) {
        return decode(input, 0, input.length, flags);
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        Decoder decoder = new Decoder(flags, new byte[((len * 3) / 4)]);
        if (!decoder.process(input, offset, len, true)) {
            throw new IllegalArgumentException("bad base-64");
        } else if (decoder.op == decoder.output.length) {
            return decoder.output;
        } else {
            byte[] temp = new byte[decoder.op];
            System.arraycopy(decoder.output, 0, temp, 0, decoder.op);
            return temp;
        }
    }

    public static String encodeToString(byte[] input, int flags) {
        try {
            return new String(encode(input, flags), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        try {
            return new String(encode(input, offset, len, flags), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] encode(byte[] input, int flags) {
        return encode(input, 0, input.length, flags);
    }

    public static byte[] encode(byte[] input, int offset, int len, int flags) {
        Encoder encoder = new Encoder(flags, null);
        int output_len = (len / 3) * 4;
        if (!encoder.do_padding) {
            switch (len % 3) {
                case 1:
                    output_len += 2;
                    break;
                case 2:
                    output_len += 3;
                    break;
            }
        } else if (len % 3 > 0) {
            output_len += 4;
        }
        if (encoder.do_newline && len > 0) {
            output_len += (((len - 1) / 57) + 1) * (encoder.do_cr ? 2 : 1);
        }
        encoder.output = new byte[output_len];
        encoder.process(input, offset, len, true);
        return encoder.output;
    }

    private Base64() {
    }
}
