package tmsdk.common.utils;

public class b {
    static final /* synthetic */ boolean bF;

    static abstract class a {
        public byte[] Lm;
        public int Ln;

        a() {
        }
    }

    static class b extends a {
        private static final int[] Lo = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        private static final int[] Lp = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, 63, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        private final int[] Lq;
        private int state;
        private int value;

        public b(int i, byte[] bArr) {
            this.Lm = bArr;
            this.Lq = (i & 8) != 0 ? Lp : Lo;
            this.state = 0;
            this.value = 0;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean a(byte[] bArr, int -l_5_I, int i, boolean z) {
            if (this.state == 6) {
                return false;
            }
            int -l_8_I;
            int i2 = i + -l_5_I;
            int -l_6_I = this.state;
            int -l_7_I = this.value;
            int -l_8_I2 = 0;
            Object -l_9_R = this.Lm;
            Object -l_10_R = this.Lq;
            while (-l_5_I < i2) {
                if (-l_6_I == 0) {
                    while (-l_5_I + 4 <= i2) {
                        -l_7_I = (((-l_10_R[bArr[-l_5_I] & 255] << 18) | (-l_10_R[bArr[-l_5_I + 1] & 255] << 12)) | (-l_10_R[bArr[-l_5_I + 2] & 255] << 6)) | -l_10_R[bArr[-l_5_I + 3] & 255];
                        if (-l_7_I >= 0) {
                            -l_9_R[-l_8_I2 + 2] = (byte) ((byte) -l_7_I);
                            -l_9_R[-l_8_I2 + 1] = (byte) ((byte) (-l_7_I >> 8));
                            -l_9_R[-l_8_I2] = (byte) ((byte) (-l_7_I >> 16));
                            -l_8_I2 += 3;
                            -l_5_I += 4;
                        } else if (-l_5_I < i2) {
                            -l_8_I = -l_8_I2;
                            if (z) {
                                switch (-l_6_I) {
                                    case 1:
                                        this.state = 6;
                                        return false;
                                    case 2:
                                        -l_8_I2 = -l_8_I + 1;
                                        -l_9_R[-l_8_I] = (byte) ((byte) (-l_7_I >> 4));
                                        break;
                                    case 3:
                                        -l_8_I2 = -l_8_I + 1;
                                        -l_9_R[-l_8_I] = (byte) ((byte) (-l_7_I >> 10));
                                        -l_8_I = -l_8_I2 + 1;
                                        -l_9_R[-l_8_I2] = (byte) ((byte) (-l_7_I >> 2));
                                        break;
                                    case 4:
                                        this.state = 6;
                                        return false;
                                }
                                -l_8_I2 = -l_8_I;
                                this.state = -l_6_I;
                                this.Ln = -l_8_I2;
                                return true;
                            }
                            this.state = -l_6_I;
                            this.value = -l_7_I;
                            this.Ln = -l_8_I;
                            return true;
                        }
                    }
                    if (-l_5_I < i2) {
                        -l_8_I = -l_8_I2;
                        if (z) {
                            this.state = -l_6_I;
                            this.value = -l_7_I;
                            this.Ln = -l_8_I;
                            return true;
                        }
                        switch (-l_6_I) {
                            case 1:
                                this.state = 6;
                                return false;
                            case 2:
                                -l_8_I2 = -l_8_I + 1;
                                -l_9_R[-l_8_I] = (byte) ((byte) (-l_7_I >> 4));
                                break;
                            case 3:
                                -l_8_I2 = -l_8_I + 1;
                                -l_9_R[-l_8_I] = (byte) ((byte) (-l_7_I >> 10));
                                -l_8_I = -l_8_I2 + 1;
                                -l_9_R[-l_8_I2] = (byte) ((byte) (-l_7_I >> 2));
                                break;
                            case 4:
                                this.state = 6;
                                return false;
                        }
                        -l_8_I2 = -l_8_I;
                        this.state = -l_6_I;
                        this.Ln = -l_8_I2;
                        return true;
                    }
                }
                int -l_5_I2 = -l_5_I + 1;
                int -l_11_I = -l_10_R[bArr[-l_5_I] & 255];
                switch (-l_6_I) {
                    case 0:
                        if (-l_11_I < 0) {
                            if (-l_11_I == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        -l_7_I = -l_11_I;
                        -l_6_I++;
                        break;
                    case 1:
                        if (-l_11_I < 0) {
                            if (-l_11_I == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                    case 2:
                        if (-l_11_I < 0) {
                            if (-l_11_I != -2) {
                                if (-l_11_I == -1) {
                                    break;
                                }
                                this.state = 6;
                                return false;
                            }
                            -l_8_I = -l_8_I2 + 1;
                            -l_9_R[-l_8_I2] = (byte) ((byte) (-l_7_I >> 4));
                            -l_6_I = 4;
                            -l_8_I2 = -l_8_I;
                            break;
                        }
                    case 3:
                        if (-l_11_I < 0) {
                            if (-l_11_I != -2) {
                                if (-l_11_I == -1) {
                                    break;
                                }
                                this.state = 6;
                                return false;
                            }
                            -l_9_R[-l_8_I2 + 1] = (byte) ((byte) (-l_7_I >> 2));
                            -l_9_R[-l_8_I2] = (byte) ((byte) (-l_7_I >> 10));
                            -l_8_I2 += 2;
                            -l_6_I = 5;
                            break;
                        }
                        -l_7_I = (-l_7_I << 6) | -l_11_I;
                        -l_9_R[-l_8_I2 + 2] = (byte) ((byte) -l_7_I);
                        -l_9_R[-l_8_I2 + 1] = (byte) ((byte) (-l_7_I >> 8));
                        -l_9_R[-l_8_I2] = (byte) ((byte) (-l_7_I >> 16));
                        -l_8_I2 += 3;
                        -l_6_I = 0;
                        break;
                    case 4:
                        if (-l_11_I != -2) {
                            if (-l_11_I == -1) {
                                break;
                            }
                            this.state = 6;
                            return false;
                        }
                        -l_6_I++;
                        break;
                    case 5:
                        if (-l_11_I == -1) {
                            break;
                        }
                        this.state = 6;
                        return false;
                    default:
                        break;
                }
                -l_5_I = -l_5_I2;
            }
            -l_8_I = -l_8_I2;
            if (z) {
                switch (-l_6_I) {
                    case 1:
                        this.state = 6;
                        return false;
                    case 2:
                        -l_8_I2 = -l_8_I + 1;
                        -l_9_R[-l_8_I] = (byte) ((byte) (-l_7_I >> 4));
                        break;
                    case 3:
                        -l_8_I2 = -l_8_I + 1;
                        -l_9_R[-l_8_I] = (byte) ((byte) (-l_7_I >> 10));
                        -l_8_I = -l_8_I2 + 1;
                        -l_9_R[-l_8_I2] = (byte) ((byte) (-l_7_I >> 2));
                        break;
                    case 4:
                        this.state = 6;
                        return false;
                }
                -l_8_I2 = -l_8_I;
                this.state = -l_6_I;
                this.Ln = -l_8_I2;
                return true;
            }
            this.state = -l_6_I;
            this.value = -l_7_I;
            this.Ln = -l_8_I;
            return true;
        }
    }

    static class c extends a {
        private static final byte[] Lr = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 43, (byte) 47};
        private static final byte[] Ls = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 45, (byte) 95};
        static final /* synthetic */ boolean bF = (!b.class.desiredAssertionStatus());
        private final byte[] Lt;
        int Lu;
        public final boolean Lv;
        public final boolean Lw;
        public final boolean Lx;
        private final byte[] Ly;
        private int count;

        public c(int i, byte[] bArr) {
            boolean z = true;
            this.Lm = bArr;
            this.Lv = (i & 1) == 0;
            this.Lw = (i & 2) == 0;
            if ((i & 4) == 0) {
                z = false;
            }
            this.Lx = z;
            this.Ly = (i & 8) != 0 ? Ls : Lr;
            this.Lt = new byte[2];
            this.Lu = 0;
            this.count = !this.Lw ? -1 : 19;
        }

        public boolean a(byte[] r14, int r15, int r16, boolean r17) {
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
            r13 = this;
            r3 = r13.Ly;
            r4 = r13.Lm;
            r5 = 0;
            r7 = r13.count;
            r10 = r16 + r15;
            r0 = -1;
            r9 = r13.Lu;
            switch(r9) {
                case 0: goto L_0x000f;
                case 1: goto L_0x002a;
                case 2: goto L_0x004c;
                default: goto L_0x000f;
            };
        L_0x000f:
            r9 = -1;
            if (r0 != r9) goto L_0x0070;
        L_0x0012:
            r8 = r15;
            r6 = r5;
        L_0x0014:
            r9 = r8 + 3;
            if (r9 <= r10) goto L_0x00b8;
        L_0x0018:
            if (r17 != 0) goto L_0x0112;
        L_0x001a:
            r9 = r10 + -1;
            if (r8 == r9) goto L_0x0228;
        L_0x001e:
            r9 = r10 + -2;
            if (r8 == r9) goto L_0x0237;
        L_0x0022:
            r15 = r8;
            r5 = r6;
        L_0x0024:
            r13.Ln = r5;
            r13.count = r7;
            r9 = 1;
            return r9;
        L_0x002a:
            r9 = r15 + 2;
            if (r9 > r10) goto L_0x000f;
        L_0x002e:
            r9 = r13.Lt;
            r11 = 0;
            r9 = r9[r11];
            r9 = r9 & 255;
            r9 = r9 << 16;
            r8 = r15 + 1;
            r11 = r14[r15];
            r11 = r11 & 255;
            r11 = r11 << 8;
            r9 = r9 | r11;
            r15 = r8 + 1;
            r11 = r14[r8];
            r11 = r11 & 255;
            r0 = r9 | r11;
            r9 = 0;
            r13.Lu = r9;
            goto L_0x000f;
        L_0x004c:
            r9 = r15 + 1;
            if (r9 > r10) goto L_0x000f;
        L_0x0050:
            r9 = r13.Lt;
            r11 = 0;
            r9 = r9[r11];
            r9 = r9 & 255;
            r9 = r9 << 16;
            r11 = r13.Lt;
            r12 = 1;
            r11 = r11[r12];
            r11 = r11 & 255;
            r11 = r11 << 8;
            r9 = r9 | r11;
            r8 = r15 + 1;
            r11 = r14[r15];
            r11 = r11 & 255;
            r0 = r9 | r11;
            r9 = 0;
            r13.Lu = r9;
            r15 = r8;
            goto L_0x000f;
        L_0x0070:
            r9 = 0;
            r5 = 1;
            r11 = r0 >> 18;
            r11 = r11 & 63;
            r11 = r3[r11];
            r11 = (byte) r11;
            r4[r9] = r11;
            r6 = r5 + 1;
            r9 = r0 >> 12;
            r9 = r9 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r5] = r9;
            r5 = r6 + 1;
            r9 = r0 >> 6;
            r9 = r9 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r6] = r9;
            r6 = r5 + 1;
            r9 = r0 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r5] = r9;
            r7 = r7 + -1;
            if (r7 == 0) goto L_0x00a1;
        L_0x009e:
            r8 = r15;
            goto L_0x0014;
        L_0x00a1:
            r9 = r13.Lx;
            if (r9 != 0) goto L_0x00b1;
        L_0x00a5:
            r5 = r6;
        L_0x00a6:
            r6 = r5 + 1;
            r9 = 10;
            r4[r5] = r9;
        L_0x00ac:
            r7 = 19;
            r8 = r15;
            goto L_0x0014;
        L_0x00b1:
            r5 = r6 + 1;
            r9 = 13;
            r4[r6] = r9;
            goto L_0x00a6;
        L_0x00b8:
            r9 = r14[r8];
            r9 = r9 & 255;
            r9 = r9 << 16;
            r11 = r8 + 1;
            r11 = r14[r11];
            r11 = r11 & 255;
            r11 = r11 << 8;
            r9 = r9 | r11;
            r11 = r8 + 2;
            r11 = r14[r11];
            r11 = r11 & 255;
            r0 = r9 | r11;
            r9 = r0 >> 18;
            r9 = r9 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r6] = r9;
            r9 = r6 + 1;
            r11 = r0 >> 12;
            r11 = r11 & 63;
            r11 = r3[r11];
            r11 = (byte) r11;
            r4[r9] = r11;
            r9 = r6 + 2;
            r11 = r0 >> 6;
            r11 = r11 & 63;
            r11 = r3[r11];
            r11 = (byte) r11;
            r4[r9] = r11;
            r9 = r6 + 3;
            r11 = r0 & 63;
            r11 = r3[r11];
            r11 = (byte) r11;
            r4[r9] = r11;
            r15 = r8 + 3;
            r5 = r6 + 4;
            r7 = r7 + -1;
            if (r7 != 0) goto L_0x0012;
        L_0x00ff:
            r9 = r13.Lx;
            if (r9 != 0) goto L_0x010a;
        L_0x0103:
            r6 = r5 + 1;
            r9 = 10;
            r4[r5] = r9;
            goto L_0x00ac;
        L_0x010a:
            r6 = r5 + 1;
            r9 = 13;
            r4[r5] = r9;
            r5 = r6;
            goto L_0x0103;
        L_0x0112:
            r9 = r13.Lu;
            r9 = r8 - r9;
            r11 = r10 + -1;
            if (r9 == r11) goto L_0x0138;
        L_0x011a:
            r9 = r13.Lu;
            r9 = r8 - r9;
            r11 = r10 + -2;
            if (r9 == r11) goto L_0x0190;
        L_0x0122:
            r9 = r13.Lw;
            if (r9 != 0) goto L_0x0204;
        L_0x0126:
            r15 = r8;
            r5 = r6;
        L_0x0128:
            r9 = bF;
            if (r9 == 0) goto L_0x021e;
        L_0x012c:
            r9 = bF;
            if (r9 != 0) goto L_0x0024;
        L_0x0130:
            if (r15 == r10) goto L_0x0024;
        L_0x0132:
            r9 = new java.lang.AssertionError;
            r9.<init>();
            throw r9;
        L_0x0138:
            r1 = 0;
            r9 = r13.Lu;
            if (r9 > 0) goto L_0x0173;
        L_0x013d:
            r15 = r8 + 1;
            r9 = r14[r8];
        L_0x0141:
            r9 = r9 & 255;
            r0 = r9 << 4;
            r9 = r13.Lu;
            r9 = r9 - r1;
            r13.Lu = r9;
            r5 = r6 + 1;
            r9 = r0 >> 6;
            r9 = r9 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r6] = r9;
            r6 = r5 + 1;
            r9 = r0 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r5] = r9;
            r9 = r13.Lv;
            if (r9 != 0) goto L_0x017b;
        L_0x0162:
            r5 = r6;
            r9 = r13.Lw;
            if (r9 == 0) goto L_0x0128;
        L_0x0167:
            r9 = r13.Lx;
            if (r9 != 0) goto L_0x0188;
        L_0x016b:
            r6 = r5 + 1;
            r9 = 10;
            r4[r5] = r9;
        L_0x0171:
            r5 = r6;
            goto L_0x0128;
        L_0x0173:
            r9 = r13.Lt;
            r11 = 0;
            r1 = 1;
            r9 = r9[r11];
            r15 = r8;
            goto L_0x0141;
        L_0x017b:
            r5 = r6 + 1;
            r9 = 61;
            r4[r6] = r9;
            r6 = r5 + 1;
            r9 = 61;
            r4[r5] = r9;
            goto L_0x0162;
        L_0x0188:
            r6 = r5 + 1;
            r9 = 13;
            r4[r5] = r9;
            r5 = r6;
            goto L_0x016b;
        L_0x0190:
            r1 = 0;
            r9 = r13.Lu;
            r11 = 1;
            if (r9 > r11) goto L_0x01e4;
        L_0x0196:
            r15 = r8 + 1;
            r9 = r14[r8];
        L_0x019a:
            r9 = r9 & 255;
            r11 = r9 << 10;
            r9 = r13.Lu;
            if (r9 > 0) goto L_0x01ec;
        L_0x01a2:
            r8 = r15 + 1;
            r9 = r14[r15];
            r15 = r8;
        L_0x01a7:
            r9 = r9 & 255;
            r9 = r9 << 2;
            r0 = r11 | r9;
            r9 = r13.Lu;
            r9 = r9 - r1;
            r13.Lu = r9;
            r5 = r6 + 1;
            r9 = r0 >> 12;
            r9 = r9 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r6] = r9;
            r6 = r5 + 1;
            r9 = r0 >> 6;
            r9 = r9 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r5] = r9;
            r5 = r6 + 1;
            r9 = r0 & 63;
            r9 = r3[r9];
            r9 = (byte) r9;
            r4[r6] = r9;
            r9 = r13.Lv;
            if (r9 != 0) goto L_0x01f4;
        L_0x01d5:
            r9 = r13.Lw;
            if (r9 == 0) goto L_0x0128;
        L_0x01d9:
            r9 = r13.Lx;
            if (r9 != 0) goto L_0x01fc;
        L_0x01dd:
            r6 = r5 + 1;
            r9 = 10;
            r4[r5] = r9;
            goto L_0x0171;
        L_0x01e4:
            r9 = r13.Lt;
            r11 = 0;
            r1 = 1;
            r9 = r9[r11];
            r15 = r8;
            goto L_0x019a;
        L_0x01ec:
            r9 = r13.Lt;
            r2 = r1 + 1;
            r9 = r9[r1];
            r1 = r2;
            goto L_0x01a7;
        L_0x01f4:
            r6 = r5 + 1;
            r9 = 61;
            r4[r5] = r9;
            r5 = r6;
            goto L_0x01d5;
        L_0x01fc:
            r6 = r5 + 1;
            r9 = 13;
            r4[r5] = r9;
            r5 = r6;
            goto L_0x01dd;
        L_0x0204:
            if (r6 <= 0) goto L_0x0126;
        L_0x0206:
            r9 = 19;
            if (r7 == r9) goto L_0x0126;
        L_0x020a:
            r9 = r13.Lx;
            if (r9 != 0) goto L_0x0217;
        L_0x020e:
            r5 = r6;
        L_0x020f:
            r6 = r5 + 1;
            r9 = 10;
            r4[r5] = r9;
            goto L_0x0126;
        L_0x0217:
            r5 = r6 + 1;
            r9 = 13;
            r4[r6] = r9;
            goto L_0x020f;
        L_0x021e:
            r9 = r13.Lu;
            if (r9 == 0) goto L_0x012c;
        L_0x0222:
            r9 = new java.lang.AssertionError;
            r9.<init>();
            throw r9;
        L_0x0228:
            r9 = r13.Lt;
            r10 = r13.Lu;
            r11 = r10 + 1;
            r13.Lu = r11;
            r11 = r14[r8];
            r11 = (byte) r11;
            r9[r10] = r11;
            goto L_0x0022;
        L_0x0237:
            r9 = r13.Lt;
            r10 = r13.Lu;
            r11 = r10 + 1;
            r13.Lu = r11;
            r11 = r14[r8];
            r11 = (byte) r11;
            r9[r10] = r11;
            r9 = r13.Lt;
            r10 = r13.Lu;
            r11 = r10 + 1;
            r13.Lu = r11;
            r11 = r8 + 1;
            r11 = r14[r11];
            r11 = (byte) r11;
            r9[r10] = r11;
            goto L_0x0022;
            */
            throw new UnsupportedOperationException("Method not decompiled: tmsdk.common.utils.b.c.a(byte[], int, int, boolean):boolean");
        }
    }

    static {
        boolean z = false;
        if (!b.class.desiredAssertionStatus()) {
            z = true;
        }
        bF = z;
    }

    private b() {
    }

    public static byte[] decode(String str, int i) {
        return decode(str.getBytes(), i);
    }

    public static byte[] decode(byte[] bArr, int i) {
        return decode(bArr, 0, bArr.length, i);
    }

    public static byte[] decode(byte[] bArr, int i, int i2, int i3) {
        Object -l_4_R = new b(i3, new byte[((i2 * 3) / 4)]);
        if (!-l_4_R.a(bArr, i, i2, true)) {
            throw new IllegalArgumentException("bad base-64");
        } else if (-l_4_R.Ln == -l_4_R.Lm.length) {
            return -l_4_R.Lm;
        } else {
            Object -l_5_R = new byte[-l_4_R.Ln];
            System.arraycopy(-l_4_R.Lm, 0, -l_5_R, 0, -l_4_R.Ln);
            return -l_5_R;
        }
    }

    public static byte[] encode(byte[] bArr, int i) {
        return encode(bArr, 0, bArr.length, i);
    }

    public static byte[] encode(byte[] bArr, int i, int i2, int i3) {
        Object -l_4_R = new c(i3, null);
        int -l_5_I = (i2 / 3) * 4;
        if (!-l_4_R.Lv) {
            switch (i2 % 3) {
                case 1:
                    -l_5_I += 2;
                    break;
                case 2:
                    -l_5_I += 3;
                    break;
            }
        } else if (i2 % 3 > 0) {
            -l_5_I += 4;
        }
        if (-l_4_R.Lw && i2 > 0) {
            -l_5_I += (!-l_4_R.Lx ? 1 : 2) * (((i2 - 1) / 57) + 1);
        }
        -l_4_R.Lm = new byte[-l_5_I];
        -l_4_R.a(bArr, i, i2, true);
        if (bF || -l_4_R.Ln == -l_5_I) {
            return -l_4_R.Lm;
        }
        throw new AssertionError();
    }

    public static String encodeToString(byte[] bArr, int i) {
        try {
            return new String(encode(bArr, i), "US-ASCII");
        } catch (Object -l_2_R) {
            throw new AssertionError(-l_2_R);
        }
    }
}
