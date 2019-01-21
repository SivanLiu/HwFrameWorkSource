package java.util;

import java.lang.reflect.Array;

class TimSort<T> {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
    private static final int MIN_GALLOP = 7;
    private static final int MIN_MERGE = 32;
    private final T[] a;
    private final Comparator<? super T> c;
    private int minGallop = 7;
    private final int[] runBase;
    private final int[] runLen;
    private int stackSize = 0;
    private T[] tmp;
    private int tmpBase;
    private int tmpLen;

    private TimSort(T[] a, Comparator<? super T> c, T[] work, int workBase, int workLen) {
        this.a = a;
        this.c = c;
        int len = a.length;
        int tlen = len < 512 ? len >>> 1 : 256;
        if (work == null || workLen < tlen || workBase + tlen > work.length) {
            this.tmp = (Object[]) Array.newInstance(a.getClass().getComponentType(), tlen);
            this.tmpBase = 0;
            this.tmpLen = tlen;
        } else {
            this.tmp = work;
            this.tmpBase = workBase;
            this.tmpLen = workLen;
        }
        int stackLen = len < 120 ? 5 : len < 1542 ? 10 : len < 119151 ? 24 : 49;
        this.runBase = new int[stackLen];
        this.runLen = new int[stackLen];
    }

    static <T> void sort(T[] a, int lo, int hi, Comparator<? super T> c, T[] work, int workBase, int workLen) {
        int nRemaining = hi - lo;
        if (nRemaining >= 2) {
            if (nRemaining < 32) {
                binarySort(a, lo, hi, lo + countRunAndMakeAscending(a, lo, hi, c), c);
                return;
            }
            TimSort<T> timSort = new TimSort(a, c, work, workBase, workLen);
            int minRun = minRunLength(nRemaining);
            do {
                int runLen = countRunAndMakeAscending(a, lo, hi, c);
                if (runLen < minRun) {
                    int force = nRemaining <= minRun ? nRemaining : minRun;
                    binarySort(a, lo, lo + force, lo + runLen, c);
                    runLen = force;
                }
                timSort.pushRun(lo, runLen);
                timSort.mergeCollapse();
                lo += runLen;
                nRemaining -= runLen;
            } while (nRemaining != 0);
            timSort.mergeForceCollapse();
        }
    }

    private static <T> void binarySort(T[] a, int lo, int hi, int start, Comparator<? super T> c) {
        if (start == lo) {
            start++;
        }
        while (start < hi) {
            int left;
            T pivot = a[start];
            int left2 = lo;
            int right = start;
            while (left2 < right) {
                left = (left2 + right) >>> 1;
                if (c.compare(pivot, a[left]) < 0) {
                    right = left;
                } else {
                    left2 = left + 1;
                }
            }
            left = start - left2;
            switch (left) {
                case 1:
                    break;
                case 2:
                    a[left2 + 2] = a[left2 + 1];
                    break;
                default:
                    System.arraycopy((Object) a, left2, (Object) a, left2 + 1, left);
                    continue;
            }
            a[left2 + 1] = a[left2];
            a[left2] = pivot;
            start++;
        }
    }

    private static <T> int countRunAndMakeAscending(T[] a, int lo, int hi, Comparator<? super T> c) {
        int runHi = lo + 1;
        if (runHi == hi) {
            return 1;
        }
        int runHi2 = runHi + 1;
        if (c.compare(a[runHi], a[lo]) < 0) {
            while (runHi2 < hi && c.compare(a[runHi2], a[runHi2 - 1]) < 0) {
                runHi2++;
            }
            reverseRange(a, lo, runHi2);
        } else {
            while (runHi2 < hi && c.compare(a[runHi2], a[runHi2 - 1]) >= 0) {
                runHi2++;
            }
        }
        return runHi2 - lo;
    }

    private static void reverseRange(Object[] a, int lo, int hi) {
        hi--;
        while (lo < hi) {
            Object t = a[lo];
            int lo2 = lo + 1;
            a[lo] = a[hi];
            lo = hi - 1;
            a[hi] = t;
            hi = lo;
            lo = lo2;
        }
    }

    private static int minRunLength(int n) {
        int r = 0;
        while (n >= 32) {
            r |= n & 1;
            n >>= 1;
        }
        return n + r;
    }

    private void pushRun(int runBase, int runLen) {
        this.runBase[this.stackSize] = runBase;
        this.runLen[this.stackSize] = runLen;
        this.stackSize++;
    }

    private void mergeCollapse() {
        while (this.stackSize > 1) {
            int n = this.stackSize - 2;
            if (n > 0 && this.runLen[n - 1] <= this.runLen[n] + this.runLen[n + 1]) {
                if (this.runLen[n - 1] < this.runLen[n + 1]) {
                    n--;
                }
                mergeAt(n);
            } else if (this.runLen[n] <= this.runLen[n + 1]) {
                mergeAt(n);
            } else {
                return;
            }
        }
    }

    private void mergeForceCollapse() {
        while (this.stackSize > 1) {
            int n = this.stackSize - 2;
            if (n > 0 && this.runLen[n - 1] < this.runLen[n + 1]) {
                n--;
            }
            mergeAt(n);
        }
    }

    private void mergeAt(int i) {
        int i2 = i;
        int base1 = this.runBase[i2];
        int len1 = this.runLen[i2];
        int base2 = this.runBase[i2 + 1];
        int len2 = this.runLen[i2 + 1];
        this.runLen[i2] = len1 + len2;
        if (i2 == this.stackSize - 3) {
            this.runBase[i2 + 1] = this.runBase[i2 + 2];
            this.runLen[i2 + 1] = this.runLen[i2 + 2];
        }
        this.stackSize--;
        int k = gallopRight(this.a[base2], this.a, base1, len1, 0, this.c);
        base1 += k;
        len1 -= k;
        if (len1 != 0) {
            int base22 = base2;
            int len22 = gallopLeft(this.a[(base1 + len1) - 1], this.a, base2, len2, len2 - 1, this.c);
            if (len22 != 0) {
                if (len1 <= len22) {
                    mergeLo(base1, len1, base22, len22);
                } else {
                    mergeHi(base1, len1, base22, len22);
                }
            }
        }
    }

    private static <T> int gallopLeft(T key, T[] a, int base, int len, int hint, Comparator<? super T> c) {
        int maxOfs;
        int lastOfs = 0;
        int ofs = 1;
        if (c.compare(key, a[base + hint]) > 0) {
            maxOfs = len - hint;
            while (ofs < maxOfs && c.compare(key, a[(base + hint) + ofs]) > 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }
            if (ofs > maxOfs) {
                ofs = maxOfs;
            }
            lastOfs += hint;
            ofs += hint;
        } else {
            maxOfs = hint + 1;
            while (ofs < maxOfs && c.compare(key, a[(base + hint) - ofs]) <= 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }
            if (ofs > maxOfs) {
                ofs = maxOfs;
            }
            int tmp = lastOfs;
            lastOfs = hint - ofs;
            ofs = hint - tmp;
        }
        lastOfs++;
        while (lastOfs < ofs) {
            maxOfs = ((ofs - lastOfs) >>> 1) + lastOfs;
            if (c.compare(key, a[base + maxOfs]) > 0) {
                lastOfs = maxOfs + 1;
            } else {
                ofs = maxOfs;
            }
        }
        return ofs;
    }

    private static <T> int gallopRight(T key, T[] a, int base, int len, int hint, Comparator<? super T> c) {
        int maxOfs;
        int ofs = 1;
        int lastOfs = 0;
        if (c.compare(key, a[base + hint]) < 0) {
            maxOfs = hint + 1;
            while (ofs < maxOfs && c.compare(key, a[(base + hint) - ofs]) < 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }
            if (ofs > maxOfs) {
                ofs = maxOfs;
            }
            int tmp = lastOfs;
            lastOfs = hint - ofs;
            ofs = hint - tmp;
        } else {
            maxOfs = len - hint;
            while (ofs < maxOfs && c.compare(key, a[(base + hint) + ofs]) >= 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }
            if (ofs > maxOfs) {
                ofs = maxOfs;
            }
            lastOfs += hint;
            ofs += hint;
        }
        lastOfs++;
        while (lastOfs < ofs) {
            maxOfs = ((ofs - lastOfs) >>> 1) + lastOfs;
            if (c.compare(key, a[base + maxOfs]) < 0) {
                ofs = maxOfs;
            } else {
                lastOfs = maxOfs + 1;
            }
        }
        return ofs;
    }

    /* JADX WARNING: Missing block: B:22:0x007f, code skipped:
            r18 = r1;
            r13 = r3;
            r17 = r4;
            r15 = r6;
            r6 = r12;
            r16 = r14;
            r12 = r2;
            r14 = r5;
     */
    /* JADX WARNING: Missing block: B:23:0x008a, code skipped:
            r10 = r6;
            r6 = gallopRight(r7[r15], r8, r14, r12, 0, r11);
     */
    /* JADX WARNING: Missing block: B:24:0x0096, code skipped:
            if (r6 == 0) goto L_0x00b1;
     */
    /* JADX WARNING: Missing block: B:25:0x0098, code skipped:
            java.lang.System.arraycopy(r8, r14, r7, r10, r6);
            r1 = r10 + r6;
            r5 = r14 + r6;
            r2 = r12 - r6;
     */
    /* JADX WARNING: Missing block: B:26:0x00a2, code skipped:
            if (r2 > 1) goto L_0x00ae;
     */
    /* JADX WARNING: Missing block: B:27:0x00a4, code skipped:
            r12 = r2;
            r14 = r5;
            r3 = r13;
            r10 = r17;
            r9 = 1;
            r13 = r1;
     */
    /* JADX WARNING: Missing block: B:28:0x00ae, code skipped:
            r10 = r1;
            r12 = r2;
            r14 = r5;
     */
    /* JADX WARNING: Missing block: B:29:0x00b1, code skipped:
            r5 = r10 + 1;
            r4 = r15 + 1;
            r7[r10] = r7[r15];
            r10 = r13 - 1;
     */
    /* JADX WARNING: Missing block: B:30:0x00bb, code skipped:
            if (r10 != 0) goto L_0x00c5;
     */
    /* JADX WARNING: Missing block: B:31:0x00bd, code skipped:
            r15 = r4;
            r13 = r5;
            r3 = r10;
     */
    /* JADX WARNING: Missing block: B:33:0x00c5, code skipped:
            r15 = r4;
            r9 = r5;
            r13 = r6;
            r1 = gallopLeft(r8[r14], r7, r4, r10, 0, r11);
     */
    /* JADX WARNING: Missing block: B:34:0x00d4, code skipped:
            if (r1 == 0) goto L_0x00e8;
     */
    /* JADX WARNING: Missing block: B:35:0x00d6, code skipped:
            java.lang.System.arraycopy(r7, r15, r7, r9, r1);
            r2 = r9 + r1;
            r6 = r15 + r1;
            r3 = r10 - r1;
     */
    /* JADX WARNING: Missing block: B:36:0x00df, code skipped:
            if (r3 != 0) goto L_0x00e5;
     */
    /* JADX WARNING: Missing block: B:37:0x00e1, code skipped:
            r13 = r2;
            r15 = r6;
     */
    /* JADX WARNING: Missing block: B:38:0x00e5, code skipped:
            r9 = r2;
            r10 = r3;
            r15 = r6;
     */
    /* JADX WARNING: Missing block: B:39:0x00e8, code skipped:
            r6 = r9 + 1;
            r2 = r14 + 1;
            r7[r9] = r8[r14];
            r12 = r12 - 1;
            r9 = 1;
     */
    /* JADX WARNING: Missing block: B:40:0x00f3, code skipped:
            if (r12 != 1) goto L_0x011f;
     */
    /* JADX WARNING: Missing block: B:41:0x00f5, code skipped:
            r14 = r2;
            r13 = r6;
            r3 = r10;
            r10 = r17;
     */
    /* JADX WARNING: Missing block: B:53:0x011f, code skipped:
            r17 = r17 - 1;
     */
    /* JADX WARNING: Missing block: B:54:0x0122, code skipped:
            if (r13 < 7) goto L_0x0126;
     */
    /* JADX WARNING: Missing block: B:55:0x0124, code skipped:
            r4 = 1;
     */
    /* JADX WARNING: Missing block: B:56:0x0126, code skipped:
            r4 = 0;
     */
    /* JADX WARNING: Missing block: B:57:0x0127, code skipped:
            if (r1 < 7) goto L_0x012b;
     */
    /* JADX WARNING: Missing block: B:58:0x0129, code skipped:
            r3 = 1;
     */
    /* JADX WARNING: Missing block: B:59:0x012b, code skipped:
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:61:0x012d, code skipped:
            if ((r3 | r4) != 0) goto L_0x013f;
     */
    /* JADX WARNING: Missing block: B:62:0x012f, code skipped:
            if (r17 >= 0) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:63:0x0131, code skipped:
            r17 = 0;
     */
    /* JADX WARNING: Missing block: B:65:0x013f, code skipped:
            r18 = r1;
            r14 = r2;
            r16 = r13;
            r13 = r10;
            r10 = 1;
            r9 = r21;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void mergeLo(int base1, int len1, int base2, int len2) {
        int len12 = len1;
        Object a = this.a;
        Object tmp = ensureCapacity(len12);
        int cursor1 = this.tmpBase;
        int cursor2 = base2;
        int dest = base1;
        System.arraycopy(a, base1, tmp, cursor1, len12);
        int dest2 = dest + 1;
        int cursor22 = cursor2 + 1;
        a[dest] = a[cursor2];
        cursor2 = len2 - 1;
        if (cursor2 == 0) {
            System.arraycopy(tmp, cursor1, a, dest2, len12);
            return;
        }
        int len22 = 1;
        if (len12 == 1) {
            System.arraycopy(a, cursor22, a, dest2, cursor2);
            a[dest2 + cursor2] = tmp[cursor1];
            return;
        }
        int count1;
        int dest3;
        int count12;
        int cursor23;
        int i;
        Comparator<? super T> c = this.c;
        dest = this.minGallop;
        loop0:
        while (true) {
            count1 = 0;
            dest3 = dest2;
            dest2 = cursor1;
            cursor1 = len12;
            len12 = 0;
            while (true) {
                if (c.compare(a[cursor22], tmp[dest2]) < 0) {
                    count12 = dest3 + 1;
                    cursor23 = cursor22 + 1;
                    a[dest3] = a[cursor22];
                    len12++;
                    cursor2--;
                    if (cursor2 == 0) {
                        dest3 = cursor1;
                        count1 = dest2;
                        i = len22;
                        len22 = dest;
                        break loop0;
                    }
                    count1 = 0;
                    dest3 = count12;
                    cursor22 = cursor23;
                } else {
                    count12 = dest3 + 1;
                    cursor23 = dest2 + 1;
                    a[dest3] = tmp[dest2];
                    count1++;
                    len12 = 0;
                    cursor1--;
                    if (cursor1 == len22) {
                        dest3 = cursor1;
                        i = len22;
                        count1 = cursor23;
                        len22 = dest;
                        cursor23 = cursor22;
                        break loop0;
                    }
                    dest3 = count12;
                    dest2 = cursor23;
                }
                if ((count1 | len12) >= dest) {
                    break;
                }
                i = base1;
            }
            dest = minGallop + 2;
            dest2 = cursor22;
            cursor2 = len22;
            len12 = dest3;
            cursor22 = cursor23;
            len22 = 1;
            i = base1;
        }
        len22 = minGallop;
        i = 1;
        this.minGallop = len22 < i ? i : len22;
        if (dest3 == i) {
            System.arraycopy(a, cursor23, a, count12, cursor2);
            a[count12 + cursor2] = tmp[count1];
        } else if (dest3 != 0) {
            System.arraycopy(tmp, count1, a, count12, dest3);
        } else {
            throw new IllegalArgumentException("Comparison method violates its general contract!");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x016d A:{LOOP_END, LOOP:1: B:9:0x0049->B:69:0x016d} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x008e A:{SYNTHETIC, EDGE_INSN: B:78:0x008e->B:22:0x008e ?: BREAK  } */
    /* JADX WARNING: Missing block: B:14:0x0068, code skipped:
            r20 = r12;
            r12 = r4;
            r4 = r20;
     */
    /* JADX WARNING: Missing block: B:22:0x008e, code skipped:
            r19 = r1;
            r1 = r3;
            r18 = r4;
            r12 = r5;
            r14 = r6;
            r17 = r7;
            r16 = r8;
     */
    /* JADX WARNING: Missing block: B:23:0x0099, code skipped:
            r8 = r1 - gallopRight(r10[r14], r9, r22, r1, r1 - 1, r13);
     */
    /* JADX WARNING: Missing block: B:24:0x00a8, code skipped:
            if (r8 == 0) goto L_0x00c3;
     */
    /* JADX WARNING: Missing block: B:25:0x00aa, code skipped:
            r3 = r17 - r8;
            r4 = r16 - r8;
            r1 = r1 - r8;
            java.lang.System.arraycopy(r9, r4 + 1, r9, r3 + 1, r8);
     */
    /* JADX WARNING: Missing block: B:26:0x00b6, code skipped:
            if (r1 != 0) goto L_0x00c0;
     */
    /* JADX WARNING: Missing block: B:27:0x00b8, code skipped:
            r16 = r4;
            r5 = r12;
            r12 = r18;
            r4 = r3;
     */
    /* JADX WARNING: Missing block: B:28:0x00c0, code skipped:
            r16 = r4;
     */
    /* JADX WARNING: Missing block: B:29:0x00c3, code skipped:
            r3 = r17;
     */
    /* JADX WARNING: Missing block: B:30:0x00c5, code skipped:
            r15 = r3 - 1;
            r17 = r14 - 1;
            r9[r3] = r10[r14];
            r12 = r12 - 1;
     */
    /* JADX WARNING: Missing block: B:31:0x00d0, code skipped:
            if (r12 != 1) goto L_0x00da;
     */
    /* JADX WARNING: Missing block: B:32:0x00d2, code skipped:
            r5 = r12;
            r4 = r15;
     */
    /* JADX WARNING: Missing block: B:33:0x00d5, code skipped:
            r14 = r17;
     */
    /* JADX WARNING: Missing block: B:35:0x00da, code skipped:
            r14 = r8;
            r3 = r12 - gallopLeft(r9[r16], r10, r11, r12, r12 - 1, r13);
     */
    /* JADX WARNING: Missing block: B:36:0x00e9, code skipped:
            if (r3 == 0) goto L_0x0102;
     */
    /* JADX WARNING: Missing block: B:37:0x00eb, code skipped:
            r4 = r15 - r3;
            r6 = r17 - r3;
            r5 = r12 - r3;
            java.lang.System.arraycopy(r10, r6 + 1, r9, r4 + 1, r3);
     */
    /* JADX WARNING: Missing block: B:38:0x00f9, code skipped:
            if (r5 > 1) goto L_0x00fe;
     */
    /* JADX WARNING: Missing block: B:39:0x00fb, code skipped:
            r14 = r6;
     */
    /* JADX WARNING: Missing block: B:40:0x00fe, code skipped:
            r15 = r4;
            r12 = r5;
            r17 = r6;
     */
    /* JADX WARNING: Missing block: B:41:0x0102, code skipped:
            r4 = r15 - 1;
            r5 = r16 - 1;
            r9[r15] = r9[r16];
            r1 = r1 - 1;
     */
    /* JADX WARNING: Missing block: B:42:0x010c, code skipped:
            if (r1 != 0) goto L_0x0141;
     */
    /* JADX WARNING: Missing block: B:43:0x010e, code skipped:
            r16 = r5;
            r5 = r12;
     */
    /* JADX WARNING: Missing block: B:56:0x0141, code skipped:
            r18 = r18 - 1;
     */
    /* JADX WARNING: Missing block: B:57:0x0145, code skipped:
            if (r14 < 7) goto L_0x0149;
     */
    /* JADX WARNING: Missing block: B:58:0x0147, code skipped:
            r8 = 1;
     */
    /* JADX WARNING: Missing block: B:59:0x0149, code skipped:
            r8 = 0;
     */
    /* JADX WARNING: Missing block: B:60:0x014a, code skipped:
            if (r3 < 7) goto L_0x014e;
     */
    /* JADX WARNING: Missing block: B:61:0x014c, code skipped:
            r7 = 1;
     */
    /* JADX WARNING: Missing block: B:62:0x014e, code skipped:
            r7 = 0;
     */
    /* JADX WARNING: Missing block: B:64:0x0150, code skipped:
            if ((r7 | r8) != 0) goto L_0x0162;
     */
    /* JADX WARNING: Missing block: B:65:0x0152, code skipped:
            if (r18 >= 0) goto L_0x0156;
     */
    /* JADX WARNING: Missing block: B:66:0x0154, code skipped:
            r18 = 0;
     */
    /* JADX WARNING: Missing block: B:68:0x0162, code skipped:
            r19 = r3;
            r16 = r5;
            r15 = r14;
            r14 = r17;
            r17 = r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void mergeHi(int base1, int len1, int base2, int len2) {
        int i = base2;
        int len22 = len2;
        Object a = this.a;
        Object tmp = ensureCapacity(len22);
        int tmpBase = this.tmpBase;
        System.arraycopy(a, i, tmp, tmpBase, len22);
        int cursor1 = (base1 + len1) - 1;
        int cursor2 = (tmpBase + len22) - 1;
        int dest = (i + len22) - 1;
        int dest2 = dest - 1;
        int cursor12 = cursor1 - 1;
        a[dest] = a[cursor1];
        int len12 = len1 - 1;
        if (len12 == 0) {
            System.arraycopy(tmp, tmpBase, a, dest2 - (len22 - 1), len22);
        } else if (len22 == 1) {
            dest2 -= len12;
            System.arraycopy(a, (cursor12 - len12) + 1, a, dest2 + 1, len12);
            a[dest2] = tmp[cursor2];
        } else {
            int dest3;
            int cursor13;
            int cursor14;
            Comparator<? super T> c = this.c;
            cursor1 = this.minGallop;
            loop0:
            while (true) {
                int count1 = 0;
                dest = cursor2;
                cursor2 = len22;
                len22 = len12;
                len12 = 0;
                while (true) {
                    if (c.compare(tmp[dest], a[cursor12]) < 0) {
                        dest3 = dest2 - 1;
                        cursor13 = cursor12 - 1;
                        a[dest2] = a[cursor12];
                        count1++;
                        len12 = 0;
                        len22--;
                        if (len22 == 0) {
                            len12 = len22;
                            cursor14 = cursor13;
                            cursor13 = dest;
                            break loop0;
                        }
                        dest2 = dest3;
                        cursor12 = cursor13;
                        if ((count1 | len12) < cursor1) {
                            break;
                        }
                    } else {
                        dest3 = dest2 - 1;
                        cursor13 = dest - 1;
                        a[dest2] = tmp[dest];
                        len12++;
                        cursor2--;
                        if (cursor2 == 1) {
                            len12 = len22;
                            cursor14 = cursor12;
                            break loop0;
                        }
                        count1 = 0;
                        dest2 = dest3;
                        dest = cursor13;
                        if ((count1 | len12) < cursor1) {
                        }
                    }
                }
                dest2 = cursor1;
                cursor12 = cursor2;
                cursor2 = cursor2;
                cursor1 = minGallop + 2;
                len22 = dest3;
                dest3 = 1;
            }
            dest3 = minGallop;
            this.minGallop = dest3 < 1 ? 1 : dest3;
            if (cursor2 == 1) {
                cursor1 -= len12;
                System.arraycopy(a, (cursor14 - len12) + 1, a, cursor1 + 1, len12);
                a[cursor1] = tmp[cursor13];
            } else if (cursor2 != 0) {
                System.arraycopy(tmp, tmpBase, a, cursor1 - (cursor2 - 1), cursor2);
            } else {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
        }
    }

    private T[] ensureCapacity(int minCapacity) {
        if (this.tmpLen < minCapacity) {
            int newSize = minCapacity;
            newSize |= newSize >> 1;
            newSize |= newSize >> 2;
            newSize |= newSize >> 4;
            newSize |= newSize >> 8;
            newSize = (newSize | (newSize >> 16)) + 1;
            if (newSize < 0) {
                newSize = minCapacity;
            } else {
                newSize = Math.min(newSize, this.a.length >>> 1);
            }
            this.tmp = (Object[]) Array.newInstance(this.a.getClass().getComponentType(), newSize);
            this.tmpLen = newSize;
            this.tmpBase = 0;
        }
        return this.tmp;
    }
}
