package android.util;

import java.io.UnsupportedEncodingException;

public class Base64 {
    static final /* synthetic */ boolean -assertionsDisabled = (Base64.class.desiredAssertionStatus() ^ 1);
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

        public boolean process(byte[] input, int offset, int len, boolean finish) {
            if (this.state == 6) {
                return false;
            }
            int op;
            int p = offset;
            len += offset;
            int state = this.state;
            int value = this.value;
            int op2 = 0;
            byte[] output = this.output;
            int[] alphabet = this.alphabet;
            while (p < len) {
                if (state == 0) {
                    while (p + 4 <= len) {
                        value = (((alphabet[input[p] & 255] << 18) | (alphabet[input[p + 1] & 255] << 12)) | (alphabet[input[p + 2] & 255] << 6)) | alphabet[input[p + 3] & 255];
                        if (value >= 0) {
                            output[op2 + 2] = (byte) value;
                            output[op2 + 1] = (byte) (value >> 8);
                            output[op2] = (byte) (value >> 16);
                            op2 += 3;
                            p += 4;
                        } else if (p >= len) {
                            op = op2;
                            if (finish) {
                                switch (state) {
                                    case 0:
                                        op2 = op;
                                        break;
                                    case 1:
                                        this.state = 6;
                                        return false;
                                    case 2:
                                        op2 = op + 1;
                                        output[op] = (byte) (value >> 4);
                                        break;
                                    case 3:
                                        op2 = op + 1;
                                        output[op] = (byte) (value >> 10);
                                        op = op2 + 1;
                                        output[op2] = (byte) (value >> 2);
                                        op2 = op;
                                        break;
                                    case 4:
                                        this.state = 6;
                                        return false;
                                    case 5:
                                        op2 = op;
                                        break;
                                    default:
                                        op2 = op;
                                        break;
                                }
                                this.state = state;
                                this.op = op2;
                                return true;
                            }
                            this.state = state;
                            this.value = value;
                            this.op = op;
                            return true;
                        }
                    }
                    if (p >= len) {
                        op = op2;
                        if (finish) {
                            switch (state) {
                                case 0:
                                    op2 = op;
                                    break;
                                case 1:
                                    this.state = 6;
                                    return false;
                                case 2:
                                    op2 = op + 1;
                                    output[op] = (byte) (value >> 4);
                                    break;
                                case 3:
                                    op2 = op + 1;
                                    output[op] = (byte) (value >> 10);
                                    op = op2 + 1;
                                    output[op2] = (byte) (value >> 2);
                                    op2 = op;
                                    break;
                                case 4:
                                    this.state = 6;
                                    return false;
                                case 5:
                                    op2 = op;
                                    break;
                                default:
                                    op2 = op;
                                    break;
                            }
                            this.state = state;
                            this.op = op2;
                            return true;
                        }
                        this.state = state;
                        this.value = value;
                        this.op = op;
                        return true;
                    }
                }
                int p2 = p + 1;
                int d = alphabet[input[p] & 255];
                switch (state) {
                    case 0:
                        if (d < 0) {
                            if (d == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        value = d;
                        state++;
                        break;
                    case 1:
                        if (d < 0) {
                            if (d == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        value = (value << 6) | d;
                        state++;
                        break;
                    case 2:
                        if (d < 0) {
                            if (d != -2) {
                                if (d == -1) {
                                    break;
                                }
                                this.state = 6;
                                return false;
                            }
                            op = op2 + 1;
                            output[op2] = (byte) (value >> 4);
                            state = 4;
                            op2 = op;
                            break;
                        }
                        value = (value << 6) | d;
                        state++;
                        break;
                    case 3:
                        if (d < 0) {
                            if (d != -2) {
                                if (d == -1) {
                                    break;
                                }
                                this.state = 6;
                                return false;
                            }
                            output[op2 + 1] = (byte) (value >> 2);
                            output[op2] = (byte) (value >> 10);
                            op2 += 2;
                            state = 5;
                            break;
                        }
                        value = (value << 6) | d;
                        output[op2 + 2] = (byte) value;
                        output[op2 + 1] = (byte) (value >> 8);
                        output[op2] = (byte) (value >> 16);
                        op2 += 3;
                        state = 0;
                        break;
                    case 4:
                        if (d != -2) {
                            if (d == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        state++;
                        break;
                    case 5:
                        if (d == -1) {
                            break;
                        }
                        this.state = 6;
                        return false;
                    default:
                        break;
                }
                p = p2;
            }
            op = op2;
            if (finish) {
                this.state = state;
                this.value = value;
                this.op = op;
                return true;
            }
            switch (state) {
                case 0:
                    op2 = op;
                    break;
                case 1:
                    this.state = 6;
                    return false;
                case 2:
                    op2 = op + 1;
                    output[op] = (byte) (value >> 4);
                    break;
                case 3:
                    op2 = op + 1;
                    output[op] = (byte) (value >> 10);
                    op = op2 + 1;
                    output[op2] = (byte) (value >> 2);
                    op2 = op;
                    break;
                case 4:
                    this.state = 6;
                    return false;
                case 5:
                    op2 = op;
                    break;
                default:
                    op2 = op;
                    break;
            }
            this.state = state;
            this.op = op2;
            return true;
        }
    }

    static class Encoder extends Coder {
        static final /* synthetic */ boolean -assertionsDisabled = (Encoder.class.desiredAssertionStatus() ^ 1);
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

        public Encoder(int flags, byte[] output) {
            boolean z;
            boolean z2 = true;
            this.output = output;
            this.do_padding = (flags & 1) == 0;
            if ((flags & 2) == 0) {
                z = true;
            } else {
                z = false;
            }
            this.do_newline = z;
            if ((flags & 4) == 0) {
                z2 = false;
            }
            this.do_cr = z2;
            this.alphabet = (flags & 8) == 0 ? ENCODE : ENCODE_WEBSAFE;
            this.tail = new byte[2];
            this.tailLen = 0;
            this.count = this.do_newline ? 19 : -1;
        }

        public int maxOutputSize(int len) {
            return ((len * 8) / 5) + 10;
        }

        public boolean process(byte[] r15, int r16, int r17, boolean r18) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxOverflowException: Regions stack size limit reached
	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:37)
	at jadx.core.utils.ErrorsCounter.methodError(ErrorsCounter.java:61)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r14 = this;
            r1 = r14.alphabet;
            r5 = r14.output;
            r3 = 0;
            r2 = r14.count;
            r6 = r16;
            r17 = r17 + r16;
            r10 = -1;
            r11 = r14.tailLen;
            switch(r11) {
                case 0: goto L_0x0011;
                case 1: goto L_0x00b1;
                case 2: goto L_0x00d7;
                default: goto L_0x0011;
            };
        L_0x0011:
            r11 = -1;
            if (r10 == r11) goto L_0x00ae;
        L_0x0014:
            r11 = 0;
            r3 = 1;
            r12 = r10 >> 18;
            r12 = r12 & 63;
            r12 = r1[r12];
            r5[r11] = r12;
            r4 = r3 + 1;
            r11 = r10 >> 12;
            r11 = r11 & 63;
            r11 = r1[r11];
            r5[r3] = r11;
            r3 = r4 + 1;
            r11 = r10 >> 6;
            r11 = r11 & 63;
            r11 = r1[r11];
            r5[r4] = r11;
            r4 = r3 + 1;
            r11 = r10 & 63;
            r11 = r1[r11];
            r5[r3] = r11;
            r2 = r2 + -1;
            if (r2 != 0) goto L_0x0254;
        L_0x003e:
            r11 = r14.do_cr;
            if (r11 == 0) goto L_0x0251;
        L_0x0042:
            r3 = r4 + 1;
            r11 = 13;
            r5[r4] = r11;
        L_0x0048:
            r4 = r3 + 1;
            r11 = 10;
            r5[r3] = r11;
            r2 = 19;
            r7 = r6;
        L_0x0051:
            r11 = r7 + 3;
            r0 = r17;
            if (r11 > r0) goto L_0x00fd;
        L_0x0057:
            r11 = r15[r7];
            r11 = r11 & 255;
            r11 = r11 << 16;
            r12 = r7 + 1;
            r12 = r15[r12];
            r12 = r12 & 255;
            r12 = r12 << 8;
            r11 = r11 | r12;
            r12 = r7 + 2;
            r12 = r15[r12];
            r12 = r12 & 255;
            r10 = r11 | r12;
            r11 = r10 >> 18;
            r11 = r11 & 63;
            r11 = r1[r11];
            r5[r4] = r11;
            r11 = r4 + 1;
            r12 = r10 >> 12;
            r12 = r12 & 63;
            r12 = r1[r12];
            r5[r11] = r12;
            r11 = r4 + 2;
            r12 = r10 >> 6;
            r12 = r12 & 63;
            r12 = r1[r12];
            r5[r11] = r12;
            r11 = r4 + 3;
            r12 = r10 & 63;
            r12 = r1[r12];
            r5[r11] = r12;
            r6 = r7 + 3;
            r3 = r4 + 4;
            r2 = r2 + -1;
            if (r2 != 0) goto L_0x00ae;
        L_0x009a:
            r11 = r14.do_cr;
            if (r11 == 0) goto L_0x00a5;
        L_0x009e:
            r4 = r3 + 1;
            r11 = 13;
            r5[r3] = r11;
            r3 = r4;
        L_0x00a5:
            r4 = r3 + 1;
            r11 = 10;
            r5[r3] = r11;
            r2 = 19;
            r3 = r4;
        L_0x00ae:
            r7 = r6;
            r4 = r3;
            goto L_0x0051;
        L_0x00b1:
            r11 = r16 + 2;
            r0 = r17;
            if (r11 > r0) goto L_0x0011;
        L_0x00b7:
            r11 = r14.tail;
            r12 = 0;
            r11 = r11[r12];
            r11 = r11 & 255;
            r11 = r11 << 16;
            r6 = r16 + 1;
            r12 = r15[r16];
            r12 = r12 & 255;
            r12 = r12 << 8;
            r11 = r11 | r12;
            r7 = r6 + 1;
            r12 = r15[r6];
            r12 = r12 & 255;
            r10 = r11 | r12;
            r11 = 0;
            r14.tailLen = r11;
            r6 = r7;
            goto L_0x0011;
        L_0x00d7:
            r11 = r16 + 1;
            r0 = r17;
            if (r11 > r0) goto L_0x0011;
        L_0x00dd:
            r11 = r14.tail;
            r12 = 0;
            r11 = r11[r12];
            r11 = r11 & 255;
            r11 = r11 << 16;
            r12 = r14.tail;
            r13 = 1;
            r12 = r12[r13];
            r12 = r12 & 255;
            r12 = r12 << 8;
            r11 = r11 | r12;
            r6 = r16 + 1;
            r12 = r15[r16];
            r12 = r12 & 255;
            r10 = r11 | r12;
            r11 = 0;
            r14.tailLen = r11;
            goto L_0x0011;
        L_0x00fd:
            if (r18 == 0) goto L_0x0210;
        L_0x00ff:
            r11 = r14.tailLen;
            r11 = r7 - r11;
            r12 = r17 + -1;
            if (r11 != r12) goto L_0x0168;
        L_0x0107:
            r8 = 0;
            r11 = r14.tailLen;
            if (r11 <= 0) goto L_0x0163;
        L_0x010c:
            r11 = r14.tail;
            r12 = 0;
            r8 = 1;
            r11 = r11[r12];
            r6 = r7;
        L_0x0113:
            r11 = r11 & 255;
            r10 = r11 << 4;
            r11 = r14.tailLen;
            r11 = r11 - r8;
            r14.tailLen = r11;
            r3 = r4 + 1;
            r11 = r10 >> 6;
            r11 = r11 & 63;
            r11 = r1[r11];
            r5[r4] = r11;
            r4 = r3 + 1;
            r11 = r10 & 63;
            r11 = r1[r11];
            r5[r3] = r11;
            r11 = r14.do_padding;
            if (r11 == 0) goto L_0x024e;
        L_0x0132:
            r3 = r4 + 1;
            r11 = 61;
            r5[r4] = r11;
            r4 = r3 + 1;
            r11 = 61;
            r5[r3] = r11;
            r3 = r4;
        L_0x013f:
            r11 = r14.do_newline;
            if (r11 == 0) goto L_0x0155;
        L_0x0143:
            r11 = r14.do_cr;
            if (r11 == 0) goto L_0x014e;
        L_0x0147:
            r4 = r3 + 1;
            r11 = 13;
            r5[r3] = r11;
            r3 = r4;
        L_0x014e:
            r4 = r3 + 1;
            r11 = 10;
            r5[r3] = r11;
            r3 = r4;
        L_0x0155:
            r11 = -assertionsDisabled;
            if (r11 != 0) goto L_0x0202;
        L_0x0159:
            r11 = r14.tailLen;
            if (r11 == 0) goto L_0x0202;
        L_0x015d:
            r11 = new java.lang.AssertionError;
            r11.<init>();
            throw r11;
        L_0x0163:
            r6 = r7 + 1;
            r11 = r15[r7];
            goto L_0x0113;
        L_0x0168:
            r11 = r14.tailLen;
            r11 = r7 - r11;
            r12 = r17 + -2;
            if (r11 != r12) goto L_0x01e0;
        L_0x0170:
            r8 = 0;
            r11 = r14.tailLen;
            r12 = 1;
            if (r11 <= r12) goto L_0x01d5;
        L_0x0176:
            r11 = r14.tail;
            r12 = 0;
            r8 = 1;
            r11 = r11[r12];
            r6 = r7;
        L_0x017d:
            r11 = r11 & 255;
            r12 = r11 << 10;
            r11 = r14.tailLen;
            if (r11 <= 0) goto L_0x01da;
        L_0x0185:
            r11 = r14.tail;
            r9 = r8 + 1;
            r11 = r11[r8];
            r8 = r9;
        L_0x018c:
            r11 = r11 & 255;
            r11 = r11 << 2;
            r10 = r12 | r11;
            r11 = r14.tailLen;
            r11 = r11 - r8;
            r14.tailLen = r11;
            r3 = r4 + 1;
            r11 = r10 >> 12;
            r11 = r11 & 63;
            r11 = r1[r11];
            r5[r4] = r11;
            r4 = r3 + 1;
            r11 = r10 >> 6;
            r11 = r11 & 63;
            r11 = r1[r11];
            r5[r3] = r11;
            r3 = r4 + 1;
            r11 = r10 & 63;
            r11 = r1[r11];
            r5[r4] = r11;
            r11 = r14.do_padding;
            if (r11 == 0) goto L_0x01be;
        L_0x01b7:
            r4 = r3 + 1;
            r11 = 61;
            r5[r3] = r11;
            r3 = r4;
        L_0x01be:
            r11 = r14.do_newline;
            if (r11 == 0) goto L_0x0155;
        L_0x01c2:
            r11 = r14.do_cr;
            if (r11 == 0) goto L_0x01cd;
        L_0x01c6:
            r4 = r3 + 1;
            r11 = 13;
            r5[r3] = r11;
            r3 = r4;
        L_0x01cd:
            r4 = r3 + 1;
            r11 = 10;
            r5[r3] = r11;
            r3 = r4;
            goto L_0x0155;
        L_0x01d5:
            r6 = r7 + 1;
            r11 = r15[r7];
            goto L_0x017d;
        L_0x01da:
            r7 = r6 + 1;
            r11 = r15[r6];
            r6 = r7;
            goto L_0x018c;
        L_0x01e0:
            r11 = r14.do_newline;
            if (r11 == 0) goto L_0x01fe;
        L_0x01e4:
            if (r4 <= 0) goto L_0x01fe;
        L_0x01e6:
            r11 = 19;
            if (r2 == r11) goto L_0x01fe;
        L_0x01ea:
            r11 = r14.do_cr;
            if (r11 == 0) goto L_0x024c;
        L_0x01ee:
            r3 = r4 + 1;
            r11 = 13;
            r5[r4] = r11;
        L_0x01f4:
            r4 = r3 + 1;
            r11 = 10;
            r5[r3] = r11;
            r6 = r7;
            r3 = r4;
            goto L_0x0155;
        L_0x01fe:
            r6 = r7;
            r3 = r4;
            goto L_0x0155;
        L_0x0202:
            r11 = -assertionsDisabled;
            if (r11 != 0) goto L_0x0222;
        L_0x0206:
            r0 = r17;
            if (r6 == r0) goto L_0x0222;
        L_0x020a:
            r11 = new java.lang.AssertionError;
            r11.<init>();
            throw r11;
        L_0x0210:
            r11 = r17 + -1;
            if (r7 != r11) goto L_0x0228;
        L_0x0214:
            r11 = r14.tail;
            r12 = r14.tailLen;
            r13 = r12 + 1;
            r14.tailLen = r13;
            r13 = r15[r7];
            r11[r12] = r13;
            r6 = r7;
            r3 = r4;
        L_0x0222:
            r14.op = r3;
            r14.count = r2;
            r11 = 1;
            return r11;
        L_0x0228:
            r11 = r17 + -2;
            if (r7 != r11) goto L_0x0249;
        L_0x022c:
            r11 = r14.tail;
            r12 = r14.tailLen;
            r13 = r12 + 1;
            r14.tailLen = r13;
            r13 = r15[r7];
            r11[r12] = r13;
            r11 = r14.tail;
            r12 = r14.tailLen;
            r13 = r12 + 1;
            r14.tailLen = r13;
            r13 = r7 + 1;
            r13 = r15[r13];
            r11[r12] = r13;
            r6 = r7;
            r3 = r4;
            goto L_0x0222;
        L_0x0249:
            r6 = r7;
            r3 = r4;
            goto L_0x0222;
        L_0x024c:
            r3 = r4;
            goto L_0x01f4;
        L_0x024e:
            r3 = r4;
            goto L_0x013f;
        L_0x0251:
            r3 = r4;
            goto L_0x0048;
        L_0x0254:
            r7 = r6;
            goto L_0x0051;
            */
            throw new UnsupportedOperationException("Method not decompiled: android.util.Base64.Encoder.process(byte[], int, int, boolean):boolean");
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
                case 0:
                    break;
                case 1:
                    output_len += 2;
                    break;
                case 2:
                    output_len += 3;
                    break;
                default:
                    break;
            }
        } else if (len % 3 > 0) {
            output_len += 4;
        }
        if (encoder.do_newline && len > 0) {
            int i;
            int i2 = ((len - 1) / 57) + 1;
            if (encoder.do_cr) {
                i = 2;
            } else {
                i = 1;
            }
            output_len += i * i2;
        }
        encoder.output = new byte[output_len];
        encoder.process(input, offset, len, true);
        if (-assertionsDisabled || encoder.op == output_len) {
            return encoder.output;
        }
        throw new AssertionError();
    }

    private Base64() {
    }
}
