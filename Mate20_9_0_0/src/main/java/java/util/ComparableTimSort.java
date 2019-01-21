package java.util;

class ComparableTimSort {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
    private static final int MIN_GALLOP = 7;
    private static final int MIN_MERGE = 32;
    private final Object[] a;
    private int minGallop = 7;
    private final int[] runBase;
    private final int[] runLen;
    private int stackSize = 0;
    private Object[] tmp;
    private int tmpBase;
    private int tmpLen;

    private ComparableTimSort(Object[] a, Object[] work, int workBase, int workLen) {
        this.a = a;
        int len = a.length;
        int tlen = len < 512 ? len >>> 1 : 256;
        if (work == null || workLen < tlen || workBase + tlen > work.length) {
            this.tmp = new Object[tlen];
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

    static void sort(Object[] a, int lo, int hi, Object[] work, int workBase, int workLen) {
        int nRemaining = hi - lo;
        if (nRemaining >= 2) {
            if (nRemaining < 32) {
                binarySort(a, lo, hi, lo + countRunAndMakeAscending(a, lo, hi));
                return;
            }
            ComparableTimSort ts = new ComparableTimSort(a, work, workBase, workLen);
            int minRun = minRunLength(nRemaining);
            do {
                int runLen = countRunAndMakeAscending(a, lo, hi);
                if (runLen < minRun) {
                    int force = nRemaining <= minRun ? nRemaining : minRun;
                    binarySort(a, lo, lo + force, lo + runLen);
                    runLen = force;
                }
                ts.pushRun(lo, runLen);
                ts.mergeCollapse();
                lo += runLen;
                nRemaining -= runLen;
            } while (nRemaining != 0);
            ts.mergeForceCollapse();
        }
    }

    private static void binarySort(Object[] a, int lo, int hi, int start) {
        if (start == lo) {
            start++;
        }
        while (start < hi) {
            int left;
            Comparable pivot = a[start];
            int left2 = lo;
            int right = start;
            while (left2 < right) {
                left = (left2 + right) >>> 1;
                if (pivot.compareTo(a[left]) < 0) {
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

    private static int countRunAndMakeAscending(Object[] a, int lo, int hi) {
        int runHi = lo + 1;
        if (runHi == hi) {
            return 1;
        }
        int runHi2 = runHi + 1;
        if (((Comparable) a[runHi]).compareTo(a[lo]) < 0) {
            while (runHi2 < hi && ((Comparable) a[runHi2]).compareTo(a[runHi2 - 1]) < 0) {
                runHi2++;
            }
            reverseRange(a, lo, runHi2);
        } else {
            while (runHi2 < hi && ((Comparable) a[runHi2]).compareTo(a[runHi2 - 1]) >= 0) {
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
        int base1 = this.runBase[i];
        int len1 = this.runLen[i];
        int base2 = this.runBase[i + 1];
        int len2 = this.runLen[i + 1];
        this.runLen[i] = len1 + len2;
        if (i == this.stackSize - 3) {
            this.runBase[i + 1] = this.runBase[i + 2];
            this.runLen[i + 1] = this.runLen[i + 2];
        }
        this.stackSize--;
        int k = gallopRight((Comparable) this.a[base2], this.a, base1, len1, 0);
        base1 += k;
        len1 -= k;
        if (len1 != 0) {
            len2 = gallopLeft((Comparable) this.a[(base1 + len1) - 1], this.a, base2, len2, len2 - 1);
            if (len2 != 0) {
                if (len1 <= len2) {
                    mergeLo(base1, len1, base2, len2);
                } else {
                    mergeHi(base1, len1, base2, len2);
                }
            }
        }
    }

    private static int gallopLeft(Comparable<Object> key, Object[] a, int base, int len, int hint) {
        int maxOfs;
        int lastOfs = 0;
        int ofs = 1;
        if (key.compareTo(a[base + hint]) > 0) {
            maxOfs = len - hint;
            while (ofs < maxOfs && key.compareTo(a[(base + hint) + ofs]) > 0) {
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
            while (ofs < maxOfs && key.compareTo(a[(base + hint) - ofs]) <= 0) {
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
            if (key.compareTo(a[base + maxOfs]) > 0) {
                lastOfs = maxOfs + 1;
            } else {
                ofs = maxOfs;
            }
        }
        return ofs;
    }

    private static int gallopRight(Comparable<Object> key, Object[] a, int base, int len, int hint) {
        int maxOfs;
        int ofs = 1;
        int lastOfs = 0;
        if (key.compareTo(a[base + hint]) < 0) {
            maxOfs = hint + 1;
            while (ofs < maxOfs && key.compareTo(a[(base + hint) - ofs]) < 0) {
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
            while (ofs < maxOfs && key.compareTo(a[(base + hint) + ofs]) >= 0) {
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
            if (key.compareTo(a[base + maxOfs]) < 0) {
                ofs = maxOfs;
            } else {
                lastOfs = maxOfs + 1;
            }
        }
        return ofs;
    }

    /* JADX WARNING: Missing block: B:14:0x0059, code skipped:
            r9 = r15;
     */
    /* JADX WARNING: Missing block: B:57:0x00f3, code skipped:
            if (r10 >= 0) goto L_0x00f6;
     */
    /* JADX WARNING: Missing block: B:58:0x00f5, code skipped:
            r10 = 0;
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
        } else if (len12 == 1) {
            System.arraycopy(a, cursor22, a, dest2, cursor2);
            a[dest2 + cursor2] = tmp[cursor1];
        } else {
            int dest3;
            int cursor23;
            int minGallop = this.minGallop;
            loop0:
            while (true) {
                int count1 = 0;
                int dest4 = dest2;
                dest2 = cursor1;
                cursor1 = len12;
                len12 = 0;
                do {
                    if (((Comparable) a[cursor22]).compareTo(tmp[dest2]) < 0) {
                        dest3 = dest4 + 1;
                        cursor23 = cursor22 + 1;
                        a[dest4] = a[cursor22];
                        len12++;
                        cursor2--;
                        if (cursor2 == 0) {
                            break loop0;
                        }
                        count1 = 0;
                        dest4 = dest3;
                        cursor22 = cursor23;
                    } else {
                        dest3 = dest4 + 1;
                        cursor23 = dest2 + 1;
                        a[dest4] = tmp[dest2];
                        count1++;
                        len12 = 0;
                        cursor1--;
                        if (cursor1 == 1) {
                            break loop0;
                        }
                        dest4 = dest3;
                        dest2 = cursor23;
                    }
                } while ((count1 | len12) < minGallop);
                while (true) {
                    count1 = gallopRight((Comparable) a[cursor22], tmp, dest2, cursor1, 0);
                    if (count1 != 0) {
                        System.arraycopy(tmp, dest2, a, dest4, count1);
                        dest3 = dest4 + count1;
                        dest2 += count1;
                        cursor1 -= count1;
                        if (cursor1 <= 1) {
                            break loop0;
                        }
                        dest4 = dest3;
                    }
                    dest3 = dest4 + 1;
                    cursor23 = cursor22 + 1;
                    a[dest4] = a[cursor22];
                    cursor2--;
                    if (cursor2 == 0) {
                        break loop0;
                    }
                    len12 = gallopLeft((Comparable) tmp[dest2], a, cursor23, cursor2, 0);
                    if (len12 != 0) {
                        System.arraycopy(a, cursor23, a, dest3, len12);
                        dest3 += len12;
                        cursor22 = cursor23 + len12;
                        cursor2 -= len12;
                        if (cursor2 == 0) {
                            break loop0;
                        }
                    }
                    cursor22 = cursor23;
                    dest4 = dest3 + 1;
                    cursor23 = dest2 + 1;
                    a[dest3] = tmp[dest2];
                    cursor1--;
                    if (cursor1 == 1) {
                        dest3 = dest4;
                        break loop0;
                    }
                    minGallop--;
                    if (((len12 >= 7 ? 1 : 0) | (count1 >= 7 ? 1 : 0)) == 0) {
                        break;
                    }
                    dest2 = cursor23;
                }
                minGallop += 2;
                len12 = cursor1;
                dest2 = dest4;
                cursor1 = cursor23;
            }
            dest2 = cursor23;
            this.minGallop = minGallop < 1 ? 1 : minGallop;
            if (cursor1 == 1) {
                System.arraycopy(a, cursor22, a, dest3, cursor2);
                a[dest3 + cursor2] = tmp[dest2];
            } else if (cursor1 != 0) {
                System.arraycopy(tmp, dest2, a, dest3, cursor1);
            } else {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:64:0x013b A:{LOOP_END, LOOP:1: B:9:0x0049->B:64:0x013b} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x008b A:{SYNTHETIC, EDGE_INSN: B:73:0x008b->B:22:0x008b ?: BREAK  } */
    /* JADX WARNING: Missing block: B:22:0x008b, code skipped:
            r15 = r4 - gallopRight((java.lang.Comparable) r6[r11], r5, r1, r4, r4 - 1);
     */
    /* JADX WARNING: Missing block: B:23:0x0098, code skipped:
            if (r15 == 0) goto L_0x00aa;
     */
    /* JADX WARNING: Missing block: B:24:0x009a, code skipped:
            r9 = r12 - r15;
            r13 = r13 - r15;
            r4 = r4 - r15;
            java.lang.System.arraycopy(r5, r13 + 1, r5, r9 + 1, r15);
     */
    /* JADX WARNING: Missing block: B:25:0x00a5, code skipped:
            if (r4 != 0) goto L_0x00ab;
     */
    /* JADX WARNING: Missing block: B:26:0x00a8, code skipped:
            r12 = r9;
     */
    /* JADX WARNING: Missing block: B:27:0x00aa, code skipped:
            r9 = r12;
     */
    /* JADX WARNING: Missing block: B:28:0x00ab, code skipped:
            r12 = r9 - 1;
            r14 = r11 - 1;
            r5[r9] = r6[r11];
            r10 = r10 - 1;
     */
    /* JADX WARNING: Missing block: B:29:0x00b6, code skipped:
            if (r10 != 1) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:30:0x00b8, code skipped:
            r9 = r8;
            r11 = r14;
     */
    /* JADX WARNING: Missing block: B:31:0x00bc, code skipped:
            r2 = r10 - gallopLeft((java.lang.Comparable) r5[r13], r6, r7, r10, r10 - 1);
     */
    /* JADX WARNING: Missing block: B:32:0x00c8, code skipped:
            if (r2 == 0) goto L_0x00da;
     */
    /* JADX WARNING: Missing block: B:33:0x00ca, code skipped:
            r9 = r12 - r2;
            r11 = r14 - r2;
            r10 = r10 - r2;
            java.lang.System.arraycopy(r6, r11 + 1, r5, r9 + 1, r2);
     */
    /* JADX WARNING: Missing block: B:34:0x00d7, code skipped:
            if (r10 > 1) goto L_0x00dc;
     */
    /* JADX WARNING: Missing block: B:35:0x00da, code skipped:
            r9 = r12;
            r11 = r14;
     */
    /* JADX WARNING: Missing block: B:36:0x00dc, code skipped:
            r12 = r9 - 1;
            r14 = r13 - 1;
            r5[r9] = r5[r13];
            r4 = r4 - 1;
     */
    /* JADX WARNING: Missing block: B:37:0x00e6, code skipped:
            if (r4 != 0) goto L_0x0118;
     */
    /* JADX WARNING: Missing block: B:38:0x00e8, code skipped:
            r9 = r8;
            r13 = r14;
     */
    /* JADX WARNING: Missing block: B:51:0x0118, code skipped:
            r8 = r8 - 1;
     */
    /* JADX WARNING: Missing block: B:52:0x011c, code skipped:
            if (r15 < 7) goto L_0x0121;
     */
    /* JADX WARNING: Missing block: B:53:0x011e, code skipped:
            r16 = 1;
     */
    /* JADX WARNING: Missing block: B:54:0x0121, code skipped:
            r16 = 0;
     */
    /* JADX WARNING: Missing block: B:55:0x0123, code skipped:
            if (r2 < 7) goto L_0x0127;
     */
    /* JADX WARNING: Missing block: B:56:0x0125, code skipped:
            r13 = 1;
     */
    /* JADX WARNING: Missing block: B:57:0x0127, code skipped:
            r13 = 0;
     */
    /* JADX WARNING: Missing block: B:59:0x012a, code skipped:
            if ((r16 | r13) != 0) goto L_0x0138;
     */
    /* JADX WARNING: Missing block: B:60:0x012c, code skipped:
            if (r8 >= 0) goto L_0x012f;
     */
    /* JADX WARNING: Missing block: B:61:0x012e, code skipped:
            r8 = 0;
     */
    /* JADX WARNING: Missing block: B:63:0x0138, code skipped:
            r13 = r14;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void mergeHi(int base1, int len1, int base2, int len2) {
        int i = base1;
        int i2 = base2;
        int len22 = len2;
        Object a = this.a;
        Object tmp = ensureCapacity(len22);
        int tmpBase = this.tmpBase;
        System.arraycopy(a, i2, tmp, tmpBase, len22);
        int cursor1 = (i + len1) - 1;
        int cursor2 = (tmpBase + len22) - 1;
        int dest = (i2 + len22) - 1;
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
            cursor1 = this.minGallop;
            loop0:
            while (true) {
                int cursor13;
                int count1 = 0;
                dest = cursor2;
                cursor2 = len22;
                len22 = len12;
                len12 = 0;
                while (true) {
                    if (((Comparable) tmp[dest]).compareTo(a[cursor12]) < 0) {
                        dest3 = dest2 - 1;
                        cursor13 = cursor12 - 1;
                        a[dest2] = a[cursor12];
                        count1++;
                        len12 = 0;
                        len22--;
                        if (len22 == 0) {
                            dest2 = dest3;
                            cursor12 = cursor13;
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
                            dest2 = dest3;
                            dest = cursor13;
                            break loop0;
                        }
                        dest2 = dest3;
                        count1 = 0;
                        dest = cursor13;
                        if ((count1 | len12) < cursor1) {
                        }
                    }
                }
                cursor1 += 2;
                len12 = len22;
                len22 = cursor2;
                cursor2 = dest;
                cursor12 = cursor13;
            }
            dest3 = cursor1;
            this.minGallop = dest3 < 1 ? 1 : dest3;
            if (cursor2 == 1) {
                dest2 -= len22;
                System.arraycopy(a, (cursor12 - len22) + 1, a, dest2 + 1, len22);
                a[dest2] = tmp[dest];
            } else if (cursor2 != 0) {
                System.arraycopy(tmp, tmpBase, a, dest2 - (cursor2 - 1), cursor2);
            } else {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
        }
    }

    private Object[] ensureCapacity(int minCapacity) {
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
            this.tmp = new Object[newSize];
            this.tmpLen = newSize;
            this.tmpBase = 0;
        }
        return this.tmp;
    }
}
