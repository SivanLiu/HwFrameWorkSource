package java.util;

final class DualPivotQuicksort {
    private static final int COUNTING_SORT_THRESHOLD_FOR_BYTE = 29;
    private static final int COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR = 3200;
    private static final int INSERTION_SORT_THRESHOLD = 47;
    private static final int MAX_RUN_COUNT = 67;
    private static final int MAX_RUN_LENGTH = 33;
    private static final int NUM_BYTE_VALUES = 256;
    private static final int NUM_CHAR_VALUES = 65536;
    private static final int NUM_SHORT_VALUES = 65536;
    private static final int QUICKSORT_THRESHOLD = 286;

    private DualPivotQuicksort() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00b7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00ae  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00bd  */
    /* JADX WARNING: Missing block: B:70:0x00f2, code skipped:
            if (r0[r3 + r14] <= r0[r6 + r14]) goto L_0x0105;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void sort(int[] a, int left, int right, int[] work, int workBase, int workLen) {
        int[] a2 = a;
        int i = left;
        int i2 = right;
        int[] work2 = work;
        boolean z = true;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(a2, i, i2, true);
            return;
        }
        int m;
        int lo;
        int t;
        int[] work3;
        byte odd;
        int[] run = new int[68];
        boolean z2 = false;
        run[0] = i;
        boolean count = false;
        int k = i;
        while (k < i2) {
            if (a2[k] >= a2[k + 1]) {
                if (a2[k] <= a2[k + 1]) {
                    m = MAX_RUN_LENGTH;
                    while (true) {
                        k++;
                        if (k > i2 || a2[k - 1] != a2[k]) {
                            break;
                        }
                        m--;
                        if (m == 0) {
                            sort(a2, i, i2, true);
                            return;
                        }
                    }
                }
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] < a2[k]) {
                        lo = run[count] - 1;
                        m = k;
                    }
                }
                lo = run[count] - 1;
                m = k;
                while (true) {
                    lo++;
                    m--;
                    if (lo >= m) {
                        break;
                    }
                    t = a2[lo];
                    a2[lo] = a2[m];
                    a2[m] = t;
                }
            } else {
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] > a2[k]) {
                        break;
                    }
                }
            }
            count++;
            if (count == MAX_RUN_COUNT) {
                sort(a2, i, i2, true);
                return;
            }
            run[count] = k;
        }
        m = i2 + 1;
        if (run[count] == i2) {
            count++;
            run[count] = m;
        } else if (count) {
            return;
        }
        byte odd2 = (byte) 0;
        i2 = 1;
        while (true) {
            boolean z3 = i2 << 1;
            boolean n = z3;
            if (z3 >= count) {
                break;
            }
            odd2 = (byte) (odd2 ^ 1);
        }
        i2 = m - i;
        if (work2 == null) {
            lo = workLen;
        } else if (workLen >= i2 && workBase + i2 <= work2.length) {
            int[] b;
            int bo;
            int ao;
            t = workBase;
            if (odd2 != (byte) 0) {
                System.arraycopy((Object) a2, i, (Object) work2, t, i2);
                b = a2;
                bo = 0;
                a2 = work2;
                ao = t - i;
            } else {
                b = work2;
                ao = 0;
                bo = t - i;
            }
            while (count > z) {
                int blen;
                boolean last = z2;
                boolean k2 = true;
                while (true) {
                    z = k2;
                    if (z > count) {
                        break;
                    }
                    int hi = run[z];
                    i = run[z - 1];
                    int i3 = run[z - 2];
                    blen = i2;
                    work3 = work2;
                    i2 = i3;
                    int p = i3;
                    i3 = i;
                    while (true) {
                        int q = i3;
                        if (i2 >= hi) {
                            break;
                        }
                        int mi;
                        odd = odd2;
                        k = q;
                        if (k < hi) {
                            if (p < i) {
                                mi = i;
                            } else {
                                mi = i;
                            }
                            q = k + 1;
                            b[i2 + bo] = a2[k + ao];
                            i3 = q;
                            i2++;
                            odd2 = odd;
                            i = mi;
                            lo = workLen;
                        } else {
                            mi = i;
                        }
                        lo = p + 1;
                        b[i2 + bo] = a2[p + ao];
                        i3 = k;
                        p = lo;
                        i2++;
                        odd2 = odd;
                        i = mi;
                        lo = workLen;
                    }
                    odd = odd2;
                    last++;
                    run[last] = hi;
                    k2 = z + 2;
                    i2 = blen;
                    work2 = work3;
                    i = left;
                    lo = workLen;
                }
                blen = i2;
                work3 = work2;
                odd = odd2;
                if ((count & 1) != 0) {
                    i = m;
                    i2 = run[count - 1];
                    while (true) {
                        i--;
                        if (i < i2) {
                            break;
                        }
                        b[i + bo] = a2[i + ao];
                    }
                    last++;
                    run[last] = m;
                }
                int[] t2 = a2;
                a2 = b;
                b = t2;
                i2 = ao;
                ao = bo;
                bo = i2;
                count = last;
                i2 = blen;
                work2 = work3;
                odd2 = odd;
                i = left;
                z = true;
                z2 = false;
                lo = workLen;
            }
            work3 = work2;
            odd = odd2;
        }
        work2 = new int[i2];
        t = 0;
        if (odd2 != (byte) 0) {
        }
        while (count > z) {
        }
        work3 = work2;
        odd = odd2;
    }

    private static void sort(int[] a, int left, int right, boolean leftmost) {
        int[] iArr = a;
        int k = left;
        int right2 = right;
        boolean z = leftmost;
        int length = (right2 - k) + 1;
        int j;
        int i;
        int ai;
        int j2;
        if (length < INSERTION_SORT_THRESHOLD) {
            if (z) {
                j = k;
                i = j;
                while (i < right2) {
                    ai = iArr[i + 1];
                    while (ai < iArr[j]) {
                        iArr[j + 1] = iArr[j];
                        j2 = j - 1;
                        if (j == k) {
                            j = j2;
                            break;
                        }
                        j = j2;
                    }
                    iArr[j + 1] = ai;
                    i++;
                    j = i;
                }
            } else {
                while (k < right2) {
                    k++;
                    if (iArr[k] < iArr[k - 1]) {
                        j = k;
                        while (true) {
                            j++;
                            if (j > right2) {
                                break;
                            }
                            i = iArr[k];
                            ai = iArr[j];
                            if (i < ai) {
                                ai = i;
                                i = iArr[j];
                            }
                            while (true) {
                                k--;
                                if (i >= iArr[k]) {
                                    break;
                                }
                                iArr[k + 2] = iArr[k];
                            }
                            k++;
                            iArr[k + 1] = i;
                            while (true) {
                                k--;
                                if (ai >= iArr[k]) {
                                    break;
                                }
                                iArr[k + 1] = iArr[k];
                            }
                            iArr[k + 1] = ai;
                            j++;
                            k = j;
                        }
                        k = iArr[right2];
                        while (true) {
                            right2--;
                            if (k >= iArr[right2]) {
                                break;
                            }
                            iArr[right2 + 1] = iArr[right2];
                        }
                        iArr[right2 + 1] = k;
                    }
                }
                return;
            }
            return;
        }
        int t;
        j = ((length >> 3) + (length >> 6)) + 1;
        i = (k + right2) >>> 1;
        ai = i - j;
        j2 = ai - j;
        int e4 = i + j;
        int e5 = e4 + j;
        if (iArr[ai] < iArr[j2]) {
            t = iArr[ai];
            iArr[ai] = iArr[j2];
            iArr[j2] = t;
        }
        if (iArr[i] < iArr[ai]) {
            t = iArr[i];
            iArr[i] = iArr[ai];
            iArr[ai] = t;
            if (t < iArr[j2]) {
                iArr[ai] = iArr[j2];
                iArr[j2] = t;
            }
        }
        if (iArr[e4] < iArr[i]) {
            t = iArr[e4];
            iArr[e4] = iArr[i];
            iArr[i] = t;
            if (t < iArr[ai]) {
                iArr[i] = iArr[ai];
                iArr[ai] = t;
                if (t < iArr[j2]) {
                    iArr[ai] = iArr[j2];
                    iArr[j2] = t;
                }
            }
        }
        if (iArr[e5] < iArr[e4]) {
            t = iArr[e5];
            iArr[e5] = iArr[e4];
            iArr[e4] = t;
            if (t < iArr[i]) {
                iArr[e4] = iArr[i];
                iArr[i] = t;
                if (t < iArr[ai]) {
                    iArr[i] = iArr[ai];
                    iArr[ai] = t;
                    if (t < iArr[j2]) {
                        iArr[ai] = iArr[j2];
                        iArr[j2] = t;
                    }
                }
            }
        }
        t = k;
        int great = right2;
        int i2;
        int ak;
        if (iArr[j2] == iArr[ai] || iArr[ai] == iArr[i] || iArr[i] == iArr[e4] || iArr[e4] == iArr[e5]) {
            i2 = j;
            length = iArr[i];
            j = t;
            while (t <= great) {
                if (iArr[t] != length) {
                    ak = iArr[t];
                    if (ak < length) {
                        iArr[t] = iArr[j];
                        iArr[j] = ak;
                        j++;
                    } else {
                        while (iArr[great] > length) {
                            great--;
                        }
                        if (iArr[great] < length) {
                            iArr[t] = iArr[j];
                            iArr[j] = iArr[great];
                            j++;
                        } else {
                            iArr[t] = length;
                        }
                        iArr[great] = ak;
                        great--;
                    }
                }
                t++;
            }
            sort(iArr, k, j - 1, z);
            sort(iArr, great + 1, right2, false);
        } else {
            ak = iArr[ai];
            int pivot2 = iArr[e4];
            iArr[ai] = iArr[k];
            iArr[e4] = iArr[right2];
            while (true) {
                t++;
                if (iArr[t] >= ak) {
                    break;
                }
            }
            while (true) {
                great--;
                if (iArr[great] <= pivot2) {
                    break;
                }
            }
            int k2 = t - 1;
            loop9:
            while (true) {
                k2++;
                if (k2 > great) {
                    i2 = j;
                    break;
                }
                int length2 = length;
                length = iArr[k2];
                if (length < ak) {
                    iArr[k2] = iArr[t];
                    iArr[t] = length;
                    t++;
                    i2 = j;
                } else if (length > pivot2) {
                    while (true) {
                        i2 = j;
                        if (iArr[great] > pivot2) {
                            j = great - 1;
                            if (great == k2) {
                                great = j;
                                break loop9;
                            } else {
                                great = j;
                                j = i2;
                            }
                        } else {
                            if (iArr[great] < ak) {
                                iArr[k2] = iArr[t];
                                iArr[t] = iArr[great];
                                t++;
                            } else {
                                iArr[k2] = iArr[great];
                            }
                            iArr[great] = length;
                            great--;
                        }
                    }
                } else {
                    i2 = j;
                }
                length = length2;
                j = i2;
            }
            iArr[k] = iArr[t - 1];
            iArr[t - 1] = ak;
            iArr[right2] = iArr[great + 1];
            iArr[great + 1] = pivot2;
            sort(iArr, k, t - 2, z);
            sort(iArr, great + 2, right2, false);
            if (t < j2 && e5 < great) {
                while (iArr[t] == ak) {
                    t++;
                }
                while (iArr[great] == pivot2) {
                    great--;
                }
                length = t - 1;
                loop13:
                while (true) {
                    length++;
                    if (length > great) {
                        break;
                    }
                    j = iArr[length];
                    if (j == ak) {
                        iArr[length] = iArr[t];
                        iArr[t] = j;
                        t++;
                    } else if (j == pivot2) {
                        while (iArr[great] == pivot2) {
                            k2 = great - 1;
                            if (great == length) {
                                great = k2;
                                break loop13;
                            }
                            great = k2;
                        }
                        if (iArr[great] == ak) {
                            iArr[length] = iArr[t];
                            iArr[t] = ak;
                            t++;
                        } else {
                            iArr[length] = iArr[great];
                        }
                        iArr[great] = j;
                        great--;
                    } else {
                        continue;
                    }
                }
            }
            sort(iArr, t, great, (boolean) 0);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00c7  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void sort(long[] a, int left, int right, long[] work, int workBase, int workLen) {
        long[] a2 = a;
        int i = left;
        int i2 = right;
        long[] work2 = work;
        boolean z = true;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(a2, i, i2, true);
            return;
        }
        int m;
        int lo;
        int workBase2;
        long[] work3;
        byte odd;
        int[] run = new int[68];
        boolean z2 = false;
        run[0] = i;
        boolean count = false;
        int k = i;
        while (k < i2) {
            if (a2[k] >= a2[k + 1]) {
                if (a2[k] <= a2[k + 1]) {
                    m = MAX_RUN_LENGTH;
                    while (true) {
                        k++;
                        if (k > i2 || a2[k - 1] != a2[k]) {
                            break;
                        }
                        m--;
                        if (m == 0) {
                            sort(a2, i, i2, true);
                            return;
                        }
                    }
                }
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] < a2[k]) {
                        lo = run[count] - 1;
                        m = k;
                    }
                }
                lo = run[count] - 1;
                m = k;
                while (true) {
                    lo++;
                    m--;
                    if (lo >= m) {
                        break;
                    }
                    long t = a2[lo];
                    a2[lo] = a2[m];
                    a2[m] = t;
                }
            } else {
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] > a2[k]) {
                        break;
                    }
                }
            }
            count++;
            if (count == MAX_RUN_COUNT) {
                sort(a2, i, i2, true);
                return;
            }
            run[count] = k;
        }
        m = i2 + 1;
        if (run[count] == i2) {
            count++;
            run[count] = m;
        } else if (count) {
            return;
        }
        byte odd2 = (byte) 0;
        i2 = 1;
        while (true) {
            boolean z3 = i2 << 1;
            boolean n = z3;
            if (z3 >= count) {
                break;
            }
            odd2 = (byte) (odd2 ^ 1);
        }
        i2 = m - i;
        if (work2 == null) {
            lo = workLen;
        } else if (workLen >= i2 && workBase + i2 <= work2.length) {
            long[] b;
            int bo;
            int ao;
            workBase2 = workBase;
            if (odd2 != (byte) 0) {
                System.arraycopy((Object) a2, i, (Object) work2, workBase2, i2);
                b = a2;
                bo = 0;
                a2 = work2;
                ao = workBase2 - i;
            } else {
                b = work2;
                ao = 0;
                bo = workBase2 - i;
            }
            while (count > z) {
                int blen;
                boolean last = z2;
                boolean k2 = true;
                while (true) {
                    z = k2;
                    if (z > count) {
                        break;
                    }
                    byte hi = run[z];
                    byte mi = run[z - 1];
                    byte i3 = run[z - 2];
                    blen = i2;
                    work3 = work2;
                    byte blen2 = i3;
                    int p = i3;
                    i3 = mi;
                    while (true) {
                        byte q = i3;
                        if (blen2 >= hi) {
                            break;
                        }
                        odd = odd2;
                        odd2 = q;
                        if (odd2 >= hi || (p < mi && a2[p + ao] <= a2[odd2 + ao])) {
                            int p2 = p + 1;
                            b[blen2 + bo] = a2[p + ao];
                            i3 = odd2;
                            p = p2;
                        } else {
                            q = odd2 + 1;
                            b[blen2 + bo] = a2[odd2 + ao];
                            i3 = q;
                        }
                        blen2++;
                        odd2 = odd;
                    }
                    last++;
                    run[last] = hi;
                    k2 = z + 2;
                    i2 = blen;
                    work2 = work3;
                    i = left;
                }
                blen = i2;
                work3 = work2;
                odd = odd2;
                if ((count & 1) != 0) {
                    i = m;
                    i2 = run[count - 1];
                    while (true) {
                        i--;
                        if (i < i2) {
                            break;
                        }
                        b[i + bo] = a2[i + ao];
                    }
                    last++;
                    run[last] = m;
                }
                long[] t2 = a2;
                a2 = b;
                b = t2;
                i2 = ao;
                ao = bo;
                bo = i2;
                count = last;
                i2 = blen;
                work2 = work3;
                odd2 = odd;
                i = left;
                z = true;
                z2 = false;
            }
            work3 = work2;
            odd = odd2;
        }
        work2 = new long[i2];
        workBase2 = 0;
        if (odd2 != (byte) 0) {
        }
        while (count > z) {
        }
        work3 = work2;
        odd = odd2;
    }

    private static void sort(long[] a, int left, int right, boolean leftmost) {
        long[] jArr = a;
        int k = left;
        int right2 = right;
        boolean z = leftmost;
        int length = (right2 - k) + 1;
        int j;
        int i;
        int j2;
        if (length < INSERTION_SORT_THRESHOLD) {
            if (z) {
                j = k;
                i = j;
                while (i < right2) {
                    long ai = jArr[i + 1];
                    while (ai < jArr[j]) {
                        jArr[j + 1] = jArr[j];
                        j2 = j - 1;
                        if (j == k) {
                            j = j2;
                            break;
                        }
                        j = j2;
                    }
                    jArr[j + 1] = ai;
                    i++;
                    j = i;
                }
            } else {
                while (k < right2) {
                    k++;
                    if (jArr[k] < jArr[k - 1]) {
                        long a1;
                        j = k;
                        while (true) {
                            j++;
                            if (j > right2) {
                                break;
                            }
                            a1 = jArr[k];
                            long a2 = jArr[j];
                            if (a1 < a2) {
                                a2 = a1;
                                a1 = jArr[j];
                            }
                            while (true) {
                                k--;
                                if (a1 >= jArr[k]) {
                                    break;
                                }
                                jArr[k + 2] = jArr[k];
                            }
                            k++;
                            jArr[k + 1] = a1;
                            while (true) {
                                k--;
                                if (a2 >= jArr[k]) {
                                    break;
                                }
                                jArr[k + 1] = jArr[k];
                            }
                            jArr[k + 1] = a2;
                            j++;
                            k = j;
                        }
                        a1 = jArr[right2];
                        while (true) {
                            right2--;
                            if (a1 >= jArr[right2]) {
                                break;
                            }
                            jArr[right2 + 1] = jArr[right2];
                        }
                        jArr[right2 + 1] = a1;
                    }
                }
                return;
            }
            return;
        }
        long t;
        j = ((length >> 3) + (length >> 6)) + 1;
        i = (k + right2) >>> 1;
        int e2 = i - j;
        int e1 = e2 - j;
        j2 = i + j;
        int e5 = j2 + j;
        if (jArr[e2] < jArr[e1]) {
            t = jArr[e2];
            jArr[e2] = jArr[e1];
            jArr[e1] = t;
        }
        if (jArr[i] < jArr[e2]) {
            t = jArr[i];
            jArr[i] = jArr[e2];
            jArr[e2] = t;
            if (t < jArr[e1]) {
                jArr[e2] = jArr[e1];
                jArr[e1] = t;
            }
        }
        if (jArr[j2] < jArr[i]) {
            t = jArr[j2];
            jArr[j2] = jArr[i];
            jArr[i] = t;
            if (t < jArr[e2]) {
                jArr[i] = jArr[e2];
                jArr[e2] = t;
                if (t < jArr[e1]) {
                    jArr[e2] = jArr[e1];
                    jArr[e1] = t;
                }
            }
        }
        if (jArr[e5] < jArr[j2]) {
            t = jArr[e5];
            jArr[e5] = jArr[j2];
            jArr[j2] = t;
            if (t < jArr[i]) {
                jArr[j2] = jArr[i];
                jArr[i] = t;
                if (t < jArr[e2]) {
                    jArr[i] = jArr[e2];
                    jArr[e2] = t;
                    if (t < jArr[e1]) {
                        jArr[e2] = jArr[e1];
                        jArr[e1] = t;
                    }
                }
            }
        }
        int k2 = k;
        int great = right2;
        long pivot;
        int less;
        long ak;
        if (jArr[e1] == jArr[e2] || jArr[e2] == jArr[i] || jArr[i] == jArr[j2] || jArr[j2] == jArr[e5]) {
            pivot = jArr[i];
            less = k2;
            while (k2 <= great) {
                if (jArr[k2] != pivot) {
                    ak = jArr[k2];
                    if (ak < pivot) {
                        jArr[k2] = jArr[less];
                        jArr[less] = ak;
                        less++;
                    } else {
                        while (jArr[great] > pivot) {
                            great--;
                        }
                        if (jArr[great] < pivot) {
                            jArr[k2] = jArr[less];
                            jArr[less] = jArr[great];
                            less++;
                        } else {
                            jArr[k2] = pivot;
                        }
                        jArr[great] = ak;
                        great--;
                    }
                }
                k2++;
            }
            sort(jArr, k, less - 1, z);
            sort(jArr, great + 1, right2, false);
        } else {
            long ak2;
            int great2;
            pivot = jArr[e2];
            ak = jArr[j2];
            jArr[e2] = jArr[k];
            jArr[j2] = jArr[right2];
            while (true) {
                k2++;
                if (jArr[k2] >= pivot) {
                    break;
                }
            }
            while (true) {
                great--;
                if (jArr[great] <= ak) {
                    break;
                }
            }
            less = k2 - 1;
            loop9:
            while (true) {
                less++;
                if (less > great) {
                    break;
                }
                ak2 = jArr[less];
                if (ak2 < pivot) {
                    jArr[less] = jArr[k2];
                    jArr[k2] = ak2;
                    k2++;
                } else if (ak2 > ak) {
                    while (jArr[great] > ak) {
                        great2 = great - 1;
                        if (great == less) {
                            great = great2;
                            break loop9;
                        }
                        great = great2;
                    }
                    if (jArr[great] < pivot) {
                        jArr[less] = jArr[k2];
                        jArr[k2] = jArr[great];
                        k2++;
                    } else {
                        jArr[less] = jArr[great];
                    }
                    jArr[great] = ak2;
                    great--;
                } else {
                    continue;
                }
            }
            jArr[k] = jArr[k2 - 1];
            jArr[k2 - 1] = pivot;
            jArr[right2] = jArr[great + 1];
            jArr[great + 1] = ak;
            sort(jArr, k, k2 - 2, z);
            sort(jArr, great + 2, right2, false);
            if (k2 < e1 && e5 < great) {
                while (jArr[k2] == pivot) {
                    k2++;
                }
                while (jArr[great] == ak) {
                    great--;
                }
                less = k2 - 1;
                loop13:
                while (true) {
                    less++;
                    if (less > great) {
                        break;
                    }
                    ak2 = jArr[less];
                    if (ak2 == pivot) {
                        jArr[less] = jArr[k2];
                        jArr[k2] = ak2;
                        k2++;
                    } else if (ak2 == ak) {
                        while (jArr[great] == ak) {
                            great2 = great - 1;
                            if (great == less) {
                                great = great2;
                                break loop13;
                            }
                            great = great2;
                        }
                        if (jArr[great] == pivot) {
                            jArr[less] = jArr[k2];
                            jArr[k2] = pivot;
                            k2++;
                        } else {
                            jArr[less] = jArr[great];
                        }
                        jArr[great] = ak2;
                        great--;
                    } else {
                        continue;
                    }
                }
            }
            sort(jArr, k2, great, false);
        }
    }

    static void sort(short[] a, int left, int right, short[] work, int workBase, int workLen) {
        if (right - left > COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR) {
            int i;
            int[] count = new int[65536];
            int i2 = left - 1;
            while (true) {
                i2++;
                if (i2 > right) {
                    break;
                }
                i = a[i2] - -32768;
                count[i] = count[i] + 1;
            }
            i2 = 65536;
            i = right + 1;
            while (i > left) {
                while (true) {
                    i2--;
                    if (count[i2] != 0) {
                        break;
                    }
                }
                short value = (short) (i2 - 32768);
                int s = count[i2];
                do {
                    i--;
                    a[i] = value;
                    s--;
                } while (s > 0);
            }
            return;
        }
        doSort(a, left, right, work, workBase, workLen);
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00b7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00ae  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00bd  */
    /* JADX WARNING: Missing block: B:70:0x00f2, code skipped:
            if (r0[r3 + r14] <= r0[r6 + r14]) goto L_0x0105;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void doSort(short[] a, int left, int right, short[] work, int workBase, int workLen) {
        short[] a2 = a;
        int i = left;
        int i2 = right;
        short[] work2 = work;
        boolean z = true;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(a2, i, i2, true);
            return;
        }
        int m;
        int lo;
        int workBase2;
        short[] work3;
        byte odd;
        int[] run = new int[68];
        boolean z2 = false;
        run[0] = i;
        boolean count = false;
        int k = i;
        while (k < i2) {
            if (a2[k] >= a2[k + 1]) {
                if (a2[k] <= a2[k + 1]) {
                    m = MAX_RUN_LENGTH;
                    while (true) {
                        k++;
                        if (k > i2 || a2[k - 1] != a2[k]) {
                            break;
                        }
                        m--;
                        if (m == 0) {
                            sort(a2, i, i2, true);
                            return;
                        }
                    }
                }
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] < a2[k]) {
                        lo = run[count] - 1;
                        m = k;
                    }
                }
                lo = run[count] - 1;
                m = k;
                while (true) {
                    lo++;
                    m--;
                    if (lo >= m) {
                        break;
                    }
                    short t = a2[lo];
                    a2[lo] = a2[m];
                    a2[m] = t;
                }
            } else {
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] > a2[k]) {
                        break;
                    }
                }
            }
            count++;
            if (count == MAX_RUN_COUNT) {
                sort(a2, i, i2, true);
                return;
            }
            run[count] = k;
        }
        m = i2 + 1;
        if (run[count] == i2) {
            count++;
            run[count] = m;
        } else if (count) {
            return;
        }
        byte odd2 = (byte) 0;
        i2 = 1;
        while (true) {
            boolean z3 = i2 << 1;
            boolean n = z3;
            if (z3 >= count) {
                break;
            }
            odd2 = (byte) (odd2 ^ 1);
        }
        i2 = m - i;
        if (work2 == null) {
            lo = workLen;
        } else if (workLen >= i2 && workBase + i2 <= work2.length) {
            short[] b;
            int bo;
            int ao;
            workBase2 = workBase;
            if (odd2 != (byte) 0) {
                System.arraycopy((Object) a2, i, (Object) work2, workBase2, i2);
                b = a2;
                bo = 0;
                a2 = work2;
                ao = workBase2 - i;
            } else {
                b = work2;
                ao = 0;
                bo = workBase2 - i;
            }
            while (count > z) {
                int blen;
                boolean last = z2;
                boolean k2 = true;
                while (true) {
                    z = k2;
                    if (z > count) {
                        break;
                    }
                    int hi = run[z];
                    i = run[z - 1];
                    int i3 = run[z - 2];
                    blen = i2;
                    work3 = work2;
                    i2 = i3;
                    int p = i3;
                    i3 = i;
                    while (true) {
                        int q = i3;
                        if (i2 >= hi) {
                            break;
                        }
                        int mi;
                        odd = odd2;
                        k = q;
                        if (k < hi) {
                            if (p < i) {
                                mi = i;
                            } else {
                                mi = i;
                            }
                            q = k + 1;
                            b[i2 + bo] = a2[k + ao];
                            i3 = q;
                            i2++;
                            odd2 = odd;
                            i = mi;
                            lo = workLen;
                        } else {
                            mi = i;
                        }
                        lo = p + 1;
                        b[i2 + bo] = a2[p + ao];
                        i3 = k;
                        p = lo;
                        i2++;
                        odd2 = odd;
                        i = mi;
                        lo = workLen;
                    }
                    odd = odd2;
                    last++;
                    run[last] = hi;
                    k2 = z + 2;
                    i2 = blen;
                    work2 = work3;
                    i = left;
                    lo = workLen;
                }
                blen = i2;
                work3 = work2;
                odd = odd2;
                if ((count & 1) != 0) {
                    i = m;
                    i2 = run[count - 1];
                    while (true) {
                        i--;
                        if (i < i2) {
                            break;
                        }
                        b[i + bo] = a2[i + ao];
                    }
                    last++;
                    run[last] = m;
                }
                short[] t2 = a2;
                a2 = b;
                b = t2;
                i2 = ao;
                ao = bo;
                bo = i2;
                count = last;
                i2 = blen;
                work2 = work3;
                odd2 = odd;
                i = left;
                z = true;
                z2 = false;
                lo = workLen;
            }
            work3 = work2;
            odd = odd2;
        }
        work2 = new short[i2];
        workBase2 = 0;
        if (odd2 != (byte) 0) {
        }
        while (count > z) {
        }
        work3 = work2;
        odd = odd2;
    }

    private static void sort(short[] a, int left, int right, boolean leftmost) {
        short[] sArr = a;
        int k = left;
        int right2 = right;
        boolean z = leftmost;
        int length = (right2 - k) + 1;
        int j;
        int i;
        int j2;
        if (length < INSERTION_SORT_THRESHOLD) {
            short ai;
            if (z) {
                j = k;
                i = j;
                while (i < right2) {
                    ai = sArr[i + 1];
                    while (ai < sArr[j]) {
                        sArr[j + 1] = sArr[j];
                        j2 = j - 1;
                        if (j == k) {
                            j = j2;
                            break;
                        }
                        j = j2;
                    }
                    sArr[j + 1] = ai;
                    i++;
                    j = i;
                }
            } else {
                while (k < right2) {
                    k++;
                    if (sArr[k] < sArr[k - 1]) {
                        j = k;
                        while (true) {
                            j++;
                            if (j > right2) {
                                break;
                            }
                            short a1 = sArr[k];
                            ai = sArr[j];
                            if (a1 < ai) {
                                ai = a1;
                                a1 = sArr[j];
                            }
                            while (true) {
                                k--;
                                if (a1 >= sArr[k]) {
                                    break;
                                }
                                sArr[k + 2] = sArr[k];
                            }
                            k++;
                            sArr[k + 1] = a1;
                            while (true) {
                                k--;
                                if (ai >= sArr[k]) {
                                    break;
                                }
                                sArr[k + 1] = sArr[k];
                            }
                            sArr[k + 1] = ai;
                            j++;
                            k = j;
                        }
                        short last = sArr[right2];
                        while (true) {
                            right2--;
                            if (last >= sArr[right2]) {
                                break;
                            }
                            sArr[right2 + 1] = sArr[right2];
                        }
                        sArr[right2 + 1] = last;
                    }
                }
                return;
            }
            return;
        }
        short t;
        j = ((length >> 3) + (length >> 6)) + 1;
        i = (k + right2) >>> 1;
        int e2 = i - j;
        j2 = e2 - j;
        int e4 = i + j;
        int e5 = e4 + j;
        if (sArr[e2] < sArr[j2]) {
            t = sArr[e2];
            sArr[e2] = sArr[j2];
            sArr[j2] = t;
        }
        if (sArr[i] < sArr[e2]) {
            t = sArr[i];
            sArr[i] = sArr[e2];
            sArr[e2] = t;
            if (t < sArr[j2]) {
                sArr[e2] = sArr[j2];
                sArr[j2] = t;
            }
        }
        if (sArr[e4] < sArr[i]) {
            t = sArr[e4];
            sArr[e4] = sArr[i];
            sArr[i] = t;
            if (t < sArr[e2]) {
                sArr[i] = sArr[e2];
                sArr[e2] = t;
                if (t < sArr[j2]) {
                    sArr[e2] = sArr[j2];
                    sArr[j2] = t;
                }
            }
        }
        if (sArr[e5] < sArr[e4]) {
            t = sArr[e5];
            sArr[e5] = sArr[e4];
            sArr[e4] = t;
            if (t < sArr[i]) {
                sArr[e4] = sArr[i];
                sArr[i] = t;
                if (t < sArr[e2]) {
                    sArr[i] = sArr[e2];
                    sArr[e2] = t;
                    if (t < sArr[j2]) {
                        sArr[e2] = sArr[j2];
                        sArr[j2] = t;
                    }
                }
            }
        }
        int k2 = k;
        int great = right2;
        int i2;
        short pivot;
        short ak;
        if (sArr[j2] == sArr[e2] || sArr[e2] == sArr[i] || sArr[i] == sArr[e4] || sArr[e4] == sArr[e5]) {
            i2 = j;
            pivot = sArr[i];
            j = k2;
            while (k2 <= great) {
                if (sArr[k2] != pivot) {
                    ak = sArr[k2];
                    if (ak < pivot) {
                        sArr[k2] = sArr[j];
                        sArr[j] = ak;
                        j++;
                    } else {
                        while (sArr[great] > pivot) {
                            great--;
                        }
                        if (sArr[great] < pivot) {
                            sArr[k2] = sArr[j];
                            sArr[j] = sArr[great];
                            j++;
                        } else {
                            sArr[k2] = pivot;
                        }
                        sArr[great] = ak;
                        great--;
                    }
                }
                k2++;
            }
            sort(sArr, k, j - 1, z);
            sort(sArr, great + 1, right2, false);
        } else {
            ak = sArr[e2];
            short pivot2 = sArr[e4];
            sArr[e2] = sArr[k];
            sArr[e4] = sArr[right2];
            while (true) {
                k2++;
                if (sArr[k2] >= ak) {
                    break;
                }
            }
            while (true) {
                great--;
                if (sArr[great] <= pivot2) {
                    break;
                }
            }
            int k3 = k2 - 1;
            loop9:
            while (true) {
                k3++;
                if (k3 > great) {
                    i2 = j;
                    break;
                }
                int length2 = length;
                pivot = sArr[k3];
                if (pivot < ak) {
                    sArr[k3] = sArr[k2];
                    sArr[k2] = pivot;
                    k2++;
                    i2 = j;
                } else if (pivot > pivot2) {
                    while (true) {
                        i2 = j;
                        if (sArr[great] > pivot2) {
                            j = great - 1;
                            if (great == k3) {
                                great = j;
                                break loop9;
                            } else {
                                great = j;
                                j = i2;
                            }
                        } else {
                            if (sArr[great] < ak) {
                                sArr[k3] = sArr[k2];
                                sArr[k2] = sArr[great];
                                k2++;
                            } else {
                                sArr[k3] = sArr[great];
                            }
                            sArr[great] = pivot;
                            great--;
                        }
                    }
                } else {
                    i2 = j;
                }
                length = length2;
                j = i2;
            }
            sArr[k] = sArr[k2 - 1];
            sArr[k2 - 1] = ak;
            sArr[right2] = sArr[great + 1];
            sArr[great + 1] = pivot2;
            sort(sArr, k, k2 - 2, z);
            sort(sArr, great + 2, right2, false);
            if (k2 < j2 && e5 < great) {
                while (sArr[k2] == ak) {
                    k2++;
                }
                while (sArr[great] == pivot2) {
                    great--;
                }
                length = k2 - 1;
                loop13:
                while (true) {
                    length++;
                    if (length > great) {
                        break;
                    }
                    short seventh = sArr[length];
                    if (seventh == ak) {
                        sArr[length] = sArr[k2];
                        sArr[k2] = seventh;
                        k2++;
                    } else if (seventh == pivot2) {
                        while (sArr[great] == pivot2) {
                            k3 = great - 1;
                            if (great == length) {
                                great = k3;
                                break loop13;
                            }
                            great = k3;
                        }
                        if (sArr[great] == ak) {
                            sArr[length] = sArr[k2];
                            sArr[k2] = ak;
                            k2++;
                        } else {
                            sArr[length] = sArr[great];
                        }
                        sArr[great] = seventh;
                        great--;
                    } else {
                        continue;
                    }
                }
            }
            sort(sArr, k2, great, (boolean) 0);
        }
    }

    static void sort(char[] a, int left, int right, char[] work, int workBase, int workLen) {
        if (right - left > COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR) {
            int[] count = new int[65536];
            int i = left - 1;
            while (true) {
                i++;
                if (i > right) {
                    break;
                }
                char c = a[i];
                count[c] = count[c] + 1;
            }
            i = 65536;
            int k = right + 1;
            while (k > left) {
                while (true) {
                    i--;
                    if (count[i] != 0) {
                        break;
                    }
                }
                char value = (char) i;
                int s = count[i];
                do {
                    k--;
                    a[k] = value;
                    s--;
                } while (s > 0);
            }
            return;
        }
        doSort(a, left, right, work, workBase, workLen);
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00b7  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00ae  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00bd  */
    /* JADX WARNING: Missing block: B:70:0x00f2, code skipped:
            if (r0[r3 + r14] <= r0[r6 + r14]) goto L_0x0105;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void doSort(char[] a, int left, int right, char[] work, int workBase, int workLen) {
        char[] a2 = a;
        int i = left;
        int i2 = right;
        char[] work2 = work;
        boolean z = true;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(a2, i, i2, true);
            return;
        }
        int m;
        int lo;
        int workBase2;
        char[] work3;
        byte odd;
        int[] run = new int[68];
        boolean z2 = false;
        run[0] = i;
        boolean count = false;
        int k = i;
        while (k < i2) {
            if (a2[k] >= a2[k + 1]) {
                if (a2[k] <= a2[k + 1]) {
                    m = MAX_RUN_LENGTH;
                    while (true) {
                        k++;
                        if (k > i2 || a2[k - 1] != a2[k]) {
                            break;
                        }
                        m--;
                        if (m == 0) {
                            sort(a2, i, i2, true);
                            return;
                        }
                    }
                }
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] < a2[k]) {
                        lo = run[count] - 1;
                        m = k;
                    }
                }
                lo = run[count] - 1;
                m = k;
                while (true) {
                    lo++;
                    m--;
                    if (lo >= m) {
                        break;
                    }
                    char t = a2[lo];
                    a2[lo] = a2[m];
                    a2[m] = t;
                }
            } else {
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] > a2[k]) {
                        break;
                    }
                }
            }
            count++;
            if (count == MAX_RUN_COUNT) {
                sort(a2, i, i2, true);
                return;
            }
            run[count] = k;
        }
        m = i2 + 1;
        if (run[count] == i2) {
            count++;
            run[count] = m;
        } else if (count) {
            return;
        }
        byte odd2 = (byte) 0;
        i2 = 1;
        while (true) {
            boolean z3 = i2 << 1;
            boolean n = z3;
            if (z3 >= count) {
                break;
            }
            odd2 = (byte) (odd2 ^ 1);
        }
        i2 = m - i;
        if (work2 == null) {
            lo = workLen;
        } else if (workLen >= i2 && workBase + i2 <= work2.length) {
            char[] b;
            int bo;
            int ao;
            workBase2 = workBase;
            if (odd2 != (byte) 0) {
                System.arraycopy((Object) a2, i, (Object) work2, workBase2, i2);
                b = a2;
                bo = 0;
                a2 = work2;
                ao = workBase2 - i;
            } else {
                b = work2;
                ao = 0;
                bo = workBase2 - i;
            }
            while (count > z) {
                int blen;
                boolean last = z2;
                boolean k2 = true;
                while (true) {
                    z = k2;
                    if (z > count) {
                        break;
                    }
                    int hi = run[z];
                    i = run[z - 1];
                    int i3 = run[z - 2];
                    blen = i2;
                    work3 = work2;
                    i2 = i3;
                    int p = i3;
                    i3 = i;
                    while (true) {
                        int q = i3;
                        if (i2 >= hi) {
                            break;
                        }
                        int mi;
                        odd = odd2;
                        k = q;
                        if (k < hi) {
                            if (p < i) {
                                mi = i;
                            } else {
                                mi = i;
                            }
                            q = k + 1;
                            b[i2 + bo] = a2[k + ao];
                            i3 = q;
                            i2++;
                            odd2 = odd;
                            i = mi;
                            lo = workLen;
                        } else {
                            mi = i;
                        }
                        lo = p + 1;
                        b[i2 + bo] = a2[p + ao];
                        i3 = k;
                        p = lo;
                        i2++;
                        odd2 = odd;
                        i = mi;
                        lo = workLen;
                    }
                    odd = odd2;
                    last++;
                    run[last] = hi;
                    k2 = z + 2;
                    i2 = blen;
                    work2 = work3;
                    i = left;
                    lo = workLen;
                }
                blen = i2;
                work3 = work2;
                odd = odd2;
                if ((count & 1) != 0) {
                    i = m;
                    i2 = run[count - 1];
                    while (true) {
                        i--;
                        if (i < i2) {
                            break;
                        }
                        b[i + bo] = a2[i + ao];
                    }
                    last++;
                    run[last] = m;
                }
                char[] t2 = a2;
                a2 = b;
                b = t2;
                i2 = ao;
                ao = bo;
                bo = i2;
                count = last;
                i2 = blen;
                work2 = work3;
                odd2 = odd;
                i = left;
                z = true;
                z2 = false;
                lo = workLen;
            }
            work3 = work2;
            odd = odd2;
        }
        work2 = new char[i2];
        workBase2 = 0;
        if (odd2 != (byte) 0) {
        }
        while (count > z) {
        }
        work3 = work2;
        odd = odd2;
    }

    private static void sort(char[] a, int left, int right, boolean leftmost) {
        char[] cArr = a;
        int k = left;
        int right2 = right;
        boolean z = leftmost;
        int length = (right2 - k) + 1;
        int j;
        int i;
        int j2;
        if (length < INSERTION_SORT_THRESHOLD) {
            char ai;
            if (z) {
                j = k;
                i = j;
                while (i < right2) {
                    ai = cArr[i + 1];
                    while (ai < cArr[j]) {
                        cArr[j + 1] = cArr[j];
                        j2 = j - 1;
                        if (j == k) {
                            j = j2;
                            break;
                        }
                        j = j2;
                    }
                    cArr[j + 1] = ai;
                    i++;
                    j = i;
                }
            } else {
                while (k < right2) {
                    k++;
                    if (cArr[k] < cArr[k - 1]) {
                        j = k;
                        while (true) {
                            j++;
                            if (j > right2) {
                                break;
                            }
                            char a1 = cArr[k];
                            ai = cArr[j];
                            if (a1 < ai) {
                                ai = a1;
                                a1 = cArr[j];
                            }
                            while (true) {
                                k--;
                                if (a1 >= cArr[k]) {
                                    break;
                                }
                                cArr[k + 2] = cArr[k];
                            }
                            k++;
                            cArr[k + 1] = a1;
                            while (true) {
                                k--;
                                if (ai >= cArr[k]) {
                                    break;
                                }
                                cArr[k + 1] = cArr[k];
                            }
                            cArr[k + 1] = ai;
                            j++;
                            k = j;
                        }
                        char last = cArr[right2];
                        while (true) {
                            right2--;
                            if (last >= cArr[right2]) {
                                break;
                            }
                            cArr[right2 + 1] = cArr[right2];
                        }
                        cArr[right2 + 1] = last;
                    }
                }
                return;
            }
            return;
        }
        char t;
        j = ((length >> 3) + (length >> 6)) + 1;
        i = (k + right2) >>> 1;
        int e2 = i - j;
        j2 = e2 - j;
        int e4 = i + j;
        int e5 = e4 + j;
        if (cArr[e2] < cArr[j2]) {
            t = cArr[e2];
            cArr[e2] = cArr[j2];
            cArr[j2] = t;
        }
        if (cArr[i] < cArr[e2]) {
            t = cArr[i];
            cArr[i] = cArr[e2];
            cArr[e2] = t;
            if (t < cArr[j2]) {
                cArr[e2] = cArr[j2];
                cArr[j2] = t;
            }
        }
        if (cArr[e4] < cArr[i]) {
            t = cArr[e4];
            cArr[e4] = cArr[i];
            cArr[i] = t;
            if (t < cArr[e2]) {
                cArr[i] = cArr[e2];
                cArr[e2] = t;
                if (t < cArr[j2]) {
                    cArr[e2] = cArr[j2];
                    cArr[j2] = t;
                }
            }
        }
        if (cArr[e5] < cArr[e4]) {
            t = cArr[e5];
            cArr[e5] = cArr[e4];
            cArr[e4] = t;
            if (t < cArr[i]) {
                cArr[e4] = cArr[i];
                cArr[i] = t;
                if (t < cArr[e2]) {
                    cArr[i] = cArr[e2];
                    cArr[e2] = t;
                    if (t < cArr[j2]) {
                        cArr[e2] = cArr[j2];
                        cArr[j2] = t;
                    }
                }
            }
        }
        int k2 = k;
        int great = right2;
        int i2;
        char pivot;
        char ak;
        if (cArr[j2] == cArr[e2] || cArr[e2] == cArr[i] || cArr[i] == cArr[e4] || cArr[e4] == cArr[e5]) {
            i2 = j;
            pivot = cArr[i];
            j = k2;
            while (k2 <= great) {
                if (cArr[k2] != pivot) {
                    ak = cArr[k2];
                    if (ak < pivot) {
                        cArr[k2] = cArr[j];
                        cArr[j] = ak;
                        j++;
                    } else {
                        while (cArr[great] > pivot) {
                            great--;
                        }
                        if (cArr[great] < pivot) {
                            cArr[k2] = cArr[j];
                            cArr[j] = cArr[great];
                            j++;
                        } else {
                            cArr[k2] = pivot;
                        }
                        cArr[great] = ak;
                        great--;
                    }
                }
                k2++;
            }
            sort(cArr, k, j - 1, z);
            sort(cArr, great + 1, right2, false);
        } else {
            ak = cArr[e2];
            char pivot2 = cArr[e4];
            cArr[e2] = cArr[k];
            cArr[e4] = cArr[right2];
            while (true) {
                k2++;
                if (cArr[k2] >= ak) {
                    break;
                }
            }
            while (true) {
                great--;
                if (cArr[great] <= pivot2) {
                    break;
                }
            }
            int k3 = k2 - 1;
            loop9:
            while (true) {
                k3++;
                if (k3 > great) {
                    i2 = j;
                    break;
                }
                int length2 = length;
                pivot = cArr[k3];
                if (pivot < ak) {
                    cArr[k3] = cArr[k2];
                    cArr[k2] = pivot;
                    k2++;
                    i2 = j;
                } else if (pivot > pivot2) {
                    while (true) {
                        i2 = j;
                        if (cArr[great] > pivot2) {
                            j = great - 1;
                            if (great == k3) {
                                great = j;
                                break loop9;
                            } else {
                                great = j;
                                j = i2;
                            }
                        } else {
                            if (cArr[great] < ak) {
                                cArr[k3] = cArr[k2];
                                cArr[k2] = cArr[great];
                                k2++;
                            } else {
                                cArr[k3] = cArr[great];
                            }
                            cArr[great] = pivot;
                            great--;
                        }
                    }
                } else {
                    i2 = j;
                }
                length = length2;
                j = i2;
            }
            cArr[k] = cArr[k2 - 1];
            cArr[k2 - 1] = ak;
            cArr[right2] = cArr[great + 1];
            cArr[great + 1] = pivot2;
            sort(cArr, k, k2 - 2, z);
            sort(cArr, great + 2, right2, false);
            if (k2 < j2 && e5 < great) {
                while (cArr[k2] == ak) {
                    k2++;
                }
                while (cArr[great] == pivot2) {
                    great--;
                }
                length = k2 - 1;
                loop13:
                while (true) {
                    length++;
                    if (length > great) {
                        break;
                    }
                    char seventh = cArr[length];
                    if (seventh == ak) {
                        cArr[length] = cArr[k2];
                        cArr[k2] = seventh;
                        k2++;
                    } else if (seventh == pivot2) {
                        while (cArr[great] == pivot2) {
                            k3 = great - 1;
                            if (great == length) {
                                great = k3;
                                break loop13;
                            }
                            great = k3;
                        }
                        if (cArr[great] == ak) {
                            cArr[length] = cArr[k2];
                            cArr[k2] = ak;
                            k2++;
                        } else {
                            cArr[length] = cArr[great];
                        }
                        cArr[great] = seventh;
                        great--;
                    } else {
                        continue;
                    }
                }
            }
            sort(cArr, k2, great, (boolean) 0);
        }
    }

    static void sort(byte[] a, int left, int right) {
        int i;
        if (right - left > COUNTING_SORT_THRESHOLD_FOR_BYTE) {
            int i2;
            int[] count = new int[256];
            i = left - 1;
            while (true) {
                i++;
                if (i > right) {
                    break;
                }
                i2 = a[i] + 128;
                count[i2] = count[i2] + 1;
            }
            i = 256;
            i2 = right + 1;
            while (i2 > left) {
                while (true) {
                    i--;
                    if (count[i] != 0) {
                        break;
                    }
                }
                byte value = (byte) (i - 128);
                int s = count[i];
                do {
                    i2--;
                    a[i2] = value;
                    s--;
                } while (s > 0);
            }
            return;
        }
        int j = left;
        i = j;
        while (i < right) {
            byte ai = a[i + 1];
            while (ai < a[j]) {
                a[j + 1] = a[j];
                int j2 = j - 1;
                if (j == left) {
                    j = j2;
                    break;
                }
                j = j2;
            }
            a[j + 1] = ai;
            i++;
            j = i;
        }
    }

    static void sort(float[] a, int left, int k, float[] work, int workBase, int workLen) {
        int middle;
        while (left <= k && Float.isNaN(a[k])) {
            k--;
        }
        int right = k;
        while (true) {
            k--;
            if (k < left) {
                break;
            }
            float ak = a[k];
            if (ak != ak) {
                a[k] = a[right];
                a[right] = ak;
                right--;
            }
        }
        doSort(a, left, right, work, workBase, workLen);
        k = left;
        left = right;
        while (k < left) {
            middle = (k + left) >>> 1;
            if (a[middle] < 0.0f) {
                k = middle + 1;
            } else {
                left = middle;
            }
        }
        while (k <= right && Float.floatToRawIntBits(a[k]) < 0) {
            k++;
        }
        middle = k;
        int p = k - 1;
        while (true) {
            middle++;
            if (middle <= right) {
                float ak2 = a[middle];
                if (ak2 == 0.0f) {
                    if (Float.floatToRawIntBits(ak2) < 0) {
                        a[middle] = 0.0f;
                        p++;
                        a[p] = -0.0f;
                    }
                } else {
                    return;
                }
            }
            return;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00c7  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void doSort(float[] a, int left, int right, float[] work, int workBase, int workLen) {
        float[] a2 = a;
        int i = left;
        int i2 = right;
        float[] work2 = work;
        boolean z = true;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(a2, i, i2, true);
            return;
        }
        int m;
        int lo;
        int workBase2;
        float[] work3;
        byte odd;
        int[] run = new int[68];
        boolean z2 = false;
        run[0] = i;
        boolean count = false;
        int k = i;
        while (k < i2) {
            if (a2[k] >= a2[k + 1]) {
                if (a2[k] <= a2[k + 1]) {
                    m = MAX_RUN_LENGTH;
                    while (true) {
                        k++;
                        if (k > i2 || a2[k - 1] != a2[k]) {
                            break;
                        }
                        m--;
                        if (m == 0) {
                            sort(a2, i, i2, true);
                            return;
                        }
                    }
                }
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] < a2[k]) {
                        lo = run[count] - 1;
                        m = k;
                    }
                }
                lo = run[count] - 1;
                m = k;
                while (true) {
                    lo++;
                    m--;
                    if (lo >= m) {
                        break;
                    }
                    float t = a2[lo];
                    a2[lo] = a2[m];
                    a2[m] = t;
                }
            } else {
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] > a2[k]) {
                        break;
                    }
                }
            }
            count++;
            if (count == MAX_RUN_COUNT) {
                sort(a2, i, i2, true);
                return;
            }
            run[count] = k;
        }
        m = i2 + 1;
        if (run[count] == i2) {
            count++;
            run[count] = m;
        } else if (count) {
            return;
        }
        byte odd2 = (byte) 0;
        i2 = 1;
        while (true) {
            boolean z3 = i2 << 1;
            boolean n = z3;
            if (z3 >= count) {
                break;
            }
            odd2 = (byte) (odd2 ^ 1);
        }
        i2 = m - i;
        if (work2 == null) {
            lo = workLen;
        } else if (workLen >= i2 && workBase + i2 <= work2.length) {
            float[] b;
            int bo;
            int ao;
            workBase2 = workBase;
            if (odd2 != (byte) 0) {
                System.arraycopy((Object) a2, i, (Object) work2, workBase2, i2);
                b = a2;
                bo = 0;
                a2 = work2;
                ao = workBase2 - i;
            } else {
                b = work2;
                ao = 0;
                bo = workBase2 - i;
            }
            while (count > z) {
                int blen;
                boolean last = z2;
                boolean k2 = true;
                while (true) {
                    z = k2;
                    if (z > count) {
                        break;
                    }
                    byte hi = run[z];
                    byte mi = run[z - 1];
                    byte i3 = run[z - 2];
                    blen = i2;
                    work3 = work2;
                    byte blen2 = i3;
                    int p = i3;
                    i3 = mi;
                    while (true) {
                        byte q = i3;
                        if (blen2 >= hi) {
                            break;
                        }
                        odd = odd2;
                        odd2 = q;
                        if (odd2 >= hi || (p < mi && a2[p + ao] <= a2[odd2 + ao])) {
                            int p2 = p + 1;
                            b[blen2 + bo] = a2[p + ao];
                            i3 = odd2;
                            p = p2;
                        } else {
                            q = odd2 + 1;
                            b[blen2 + bo] = a2[odd2 + ao];
                            i3 = q;
                        }
                        blen2++;
                        odd2 = odd;
                    }
                    last++;
                    run[last] = hi;
                    k2 = z + 2;
                    i2 = blen;
                    work2 = work3;
                    i = left;
                }
                blen = i2;
                work3 = work2;
                odd = odd2;
                if ((count & 1) != 0) {
                    i = m;
                    i2 = run[count - 1];
                    while (true) {
                        i--;
                        if (i < i2) {
                            break;
                        }
                        b[i + bo] = a2[i + ao];
                    }
                    last++;
                    run[last] = m;
                }
                float[] t2 = a2;
                a2 = b;
                b = t2;
                i2 = ao;
                ao = bo;
                bo = i2;
                count = last;
                i2 = blen;
                work2 = work3;
                odd2 = odd;
                i = left;
                z = true;
                z2 = false;
            }
            work3 = work2;
            odd = odd2;
        }
        work2 = new float[i2];
        workBase2 = 0;
        if (odd2 != (byte) 0) {
        }
        while (count > z) {
        }
        work3 = work2;
        odd = odd2;
    }

    private static void sort(float[] a, int left, int right, boolean leftmost) {
        float[] fArr = a;
        int k = left;
        int right2 = right;
        boolean z = leftmost;
        int length = (right2 - k) + 1;
        int j;
        int i;
        int j2;
        if (length < INSERTION_SORT_THRESHOLD) {
            float ai;
            if (z) {
                j = k;
                i = j;
                while (i < right2) {
                    ai = fArr[i + 1];
                    while (ai < fArr[j]) {
                        fArr[j + 1] = fArr[j];
                        j2 = j - 1;
                        if (j == k) {
                            j = j2;
                            break;
                        }
                        j = j2;
                    }
                    fArr[j + 1] = ai;
                    i++;
                    j = i;
                }
            } else {
                while (k < right2) {
                    k++;
                    if (fArr[k] < fArr[k - 1]) {
                        j = k;
                        while (true) {
                            j++;
                            if (j > right2) {
                                break;
                            }
                            float a1 = fArr[k];
                            ai = fArr[j];
                            if (a1 < ai) {
                                ai = a1;
                                a1 = fArr[j];
                            }
                            while (true) {
                                k--;
                                if (a1 >= fArr[k]) {
                                    break;
                                }
                                fArr[k + 2] = fArr[k];
                            }
                            k++;
                            fArr[k + 1] = a1;
                            while (true) {
                                k--;
                                if (ai >= fArr[k]) {
                                    break;
                                }
                                fArr[k + 1] = fArr[k];
                            }
                            fArr[k + 1] = ai;
                            j++;
                            k = j;
                        }
                        float last = fArr[right2];
                        while (true) {
                            right2--;
                            if (last >= fArr[right2]) {
                                break;
                            }
                            fArr[right2 + 1] = fArr[right2];
                        }
                        fArr[right2 + 1] = last;
                    }
                }
                return;
            }
            return;
        }
        float t;
        j = ((length >> 3) + (length >> 6)) + 1;
        i = (k + right2) >>> 1;
        int e2 = i - j;
        j2 = e2 - j;
        int e4 = i + j;
        int e5 = e4 + j;
        if (fArr[e2] < fArr[j2]) {
            t = fArr[e2];
            fArr[e2] = fArr[j2];
            fArr[j2] = t;
        }
        if (fArr[i] < fArr[e2]) {
            t = fArr[i];
            fArr[i] = fArr[e2];
            fArr[e2] = t;
            if (t < fArr[j2]) {
                fArr[e2] = fArr[j2];
                fArr[j2] = t;
            }
        }
        if (fArr[e4] < fArr[i]) {
            t = fArr[e4];
            fArr[e4] = fArr[i];
            fArr[i] = t;
            if (t < fArr[e2]) {
                fArr[i] = fArr[e2];
                fArr[e2] = t;
                if (t < fArr[j2]) {
                    fArr[e2] = fArr[j2];
                    fArr[j2] = t;
                }
            }
        }
        if (fArr[e5] < fArr[e4]) {
            t = fArr[e5];
            fArr[e5] = fArr[e4];
            fArr[e4] = t;
            if (t < fArr[i]) {
                fArr[e4] = fArr[i];
                fArr[i] = t;
                if (t < fArr[e2]) {
                    fArr[i] = fArr[e2];
                    fArr[e2] = t;
                    if (t < fArr[j2]) {
                        fArr[e2] = fArr[j2];
                        fArr[j2] = t;
                    }
                }
            }
        }
        int k2 = k;
        int great = right2;
        float ak;
        if (fArr[j2] == fArr[e2] || fArr[e2] == fArr[i] || fArr[i] == fArr[e4] || fArr[e4] == fArr[e5]) {
            float pivot = fArr[i];
            int less = k2;
            while (k2 <= great) {
                if (fArr[k2] != pivot) {
                    ak = fArr[k2];
                    if (ak < pivot) {
                        fArr[k2] = fArr[less];
                        fArr[less] = ak;
                        less++;
                    } else {
                        while (fArr[great] > pivot) {
                            great--;
                        }
                        if (fArr[great] < pivot) {
                            fArr[k2] = fArr[less];
                            fArr[less] = fArr[great];
                            less++;
                        } else {
                            fArr[k2] = fArr[great];
                        }
                        fArr[great] = ak;
                        great--;
                    }
                }
                k2++;
            }
            sort(fArr, k, less - 1, z);
            sort(fArr, great + 1, right2, false);
        } else {
            float pivot1 = fArr[e2];
            float pivot2 = fArr[e4];
            fArr[e2] = fArr[k];
            fArr[e4] = fArr[right2];
            while (true) {
                k2++;
                if (fArr[k2] >= pivot1) {
                    break;
                }
            }
            while (true) {
                great--;
                if (fArr[great] <= pivot2) {
                    break;
                }
            }
            int k3 = k2 - 1;
            loop9:
            while (true) {
                int k4 = k3 + 1;
                if (k4 > great) {
                    break;
                }
                float ak2 = fArr[k4];
                if (ak2 < pivot1) {
                    fArr[k4] = fArr[k2];
                    fArr[k2] = ak2;
                    k2++;
                } else if (ak2 > pivot2) {
                    while (fArr[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k4) {
                            great = great2;
                            break loop9;
                        }
                        great = great2;
                    }
                    if (fArr[great] < pivot1) {
                        fArr[k4] = fArr[k2];
                        fArr[k2] = fArr[great];
                        k2++;
                    } else {
                        fArr[k4] = fArr[great];
                    }
                    fArr[great] = ak2;
                    great--;
                } else {
                    continue;
                }
                k3 = k4;
            }
            fArr[k] = fArr[k2 - 1];
            fArr[k2 - 1] = pivot1;
            fArr[right2] = fArr[great + 1];
            fArr[great + 1] = pivot2;
            sort(fArr, k, k2 - 2, z);
            sort(fArr, great + 2, right2, (boolean) 0);
            if (k2 < j2 && e5 < great) {
                while (fArr[k2] == pivot1) {
                    k2++;
                }
                while (fArr[great] == pivot2) {
                    great--;
                }
                length = k2 - 1;
                loop13:
                while (true) {
                    length++;
                    if (length > great) {
                        break;
                    }
                    ak = fArr[length];
                    if (ak == pivot1) {
                        fArr[length] = fArr[k2];
                        fArr[k2] = ak;
                        k2++;
                    } else if (ak == pivot2) {
                        while (fArr[great] == pivot2) {
                            k3 = great - 1;
                            if (great == length) {
                                great = k3;
                                break loop13;
                            }
                            great = k3;
                        }
                        if (fArr[great] == pivot1) {
                            fArr[length] = fArr[k2];
                            fArr[k2] = fArr[great];
                            k2++;
                        } else {
                            fArr[length] = fArr[great];
                        }
                        fArr[great] = ak;
                        great--;
                    } else {
                        continue;
                    }
                }
            }
            sort(fArr, k2, great, (boolean) 0);
        }
    }

    static void sort(double[] a, int left, int right, double[] work, int workBase, int workLen) {
        int left2 = left;
        int k = right;
        while (left2 <= k && Double.isNaN(a[k])) {
            k--;
        }
        int right2 = k;
        while (true) {
            k--;
            if (k < left2) {
                break;
            }
            double ak = a[k];
            if (ak != ak) {
                a[k] = a[right2];
                a[right2] = ak;
                right2--;
            }
        }
        doSort(a, left2, right2, work, workBase, workLen);
        k = right2;
        while (left2 < k) {
            int middle = (left2 + k) >>> 1;
            if (a[middle] < 0.0d) {
                left2 = middle + 1;
            } else {
                k = middle;
            }
        }
        while (left2 <= right2 && Double.doubleToRawLongBits(a[left2]) < 0) {
            left2++;
        }
        int k2 = left2;
        int p = left2 - 1;
        while (true) {
            k2++;
            if (k2 <= right2) {
                double ak2 = a[k2];
                if (ak2 == 0.0d) {
                    if (Double.doubleToRawLongBits(ak2) < 0) {
                        a[k2] = 0.0d;
                        p++;
                        a[p] = -0.0d;
                    }
                } else {
                    return;
                }
            }
            return;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x00c7  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void doSort(double[] a, int left, int right, double[] work, int workBase, int workLen) {
        double[] a2 = a;
        int i = left;
        int i2 = right;
        double[] work2 = work;
        boolean z = true;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(a2, i, i2, true);
            return;
        }
        int m;
        int lo;
        int workBase2;
        double[] work3;
        byte odd;
        int[] run = new int[68];
        boolean z2 = false;
        run[0] = i;
        boolean count = false;
        int k = i;
        while (k < i2) {
            if (a2[k] >= a2[k + 1]) {
                if (a2[k] <= a2[k + 1]) {
                    m = MAX_RUN_LENGTH;
                    while (true) {
                        k++;
                        if (k > i2 || a2[k - 1] != a2[k]) {
                            break;
                        }
                        m--;
                        if (m == 0) {
                            sort(a2, i, i2, true);
                            return;
                        }
                    }
                }
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] < a2[k]) {
                        lo = run[count] - 1;
                        m = k;
                    }
                }
                lo = run[count] - 1;
                m = k;
                while (true) {
                    lo++;
                    m--;
                    if (lo >= m) {
                        break;
                    }
                    double t = a2[lo];
                    a2[lo] = a2[m];
                    a2[m] = t;
                }
            } else {
                while (true) {
                    k++;
                    if (k > i2 || a2[k - 1] > a2[k]) {
                        break;
                    }
                }
            }
            count++;
            if (count == MAX_RUN_COUNT) {
                sort(a2, i, i2, true);
                return;
            }
            run[count] = k;
        }
        m = i2 + 1;
        if (run[count] == i2) {
            count++;
            run[count] = m;
        } else if (count) {
            return;
        }
        byte odd2 = (byte) 0;
        i2 = 1;
        while (true) {
            boolean z3 = i2 << 1;
            boolean n = z3;
            if (z3 >= count) {
                break;
            }
            odd2 = (byte) (odd2 ^ 1);
        }
        i2 = m - i;
        if (work2 == null) {
            lo = workLen;
        } else if (workLen >= i2 && workBase + i2 <= work2.length) {
            double[] b;
            int bo;
            int ao;
            workBase2 = workBase;
            if (odd2 != (byte) 0) {
                System.arraycopy((Object) a2, i, (Object) work2, workBase2, i2);
                b = a2;
                bo = 0;
                a2 = work2;
                ao = workBase2 - i;
            } else {
                b = work2;
                ao = 0;
                bo = workBase2 - i;
            }
            while (count > z) {
                int blen;
                boolean last = z2;
                boolean k2 = true;
                while (true) {
                    z = k2;
                    if (z > count) {
                        break;
                    }
                    byte hi = run[z];
                    byte mi = run[z - 1];
                    byte i3 = run[z - 2];
                    blen = i2;
                    work3 = work2;
                    byte blen2 = i3;
                    int p = i3;
                    i3 = mi;
                    while (true) {
                        byte q = i3;
                        if (blen2 >= hi) {
                            break;
                        }
                        odd = odd2;
                        odd2 = q;
                        if (odd2 >= hi || (p < mi && a2[p + ao] <= a2[odd2 + ao])) {
                            int p2 = p + 1;
                            b[blen2 + bo] = a2[p + ao];
                            i3 = odd2;
                            p = p2;
                        } else {
                            q = odd2 + 1;
                            b[blen2 + bo] = a2[odd2 + ao];
                            i3 = q;
                        }
                        blen2++;
                        odd2 = odd;
                    }
                    last++;
                    run[last] = hi;
                    k2 = z + 2;
                    i2 = blen;
                    work2 = work3;
                    i = left;
                }
                blen = i2;
                work3 = work2;
                odd = odd2;
                if ((count & 1) != 0) {
                    i = m;
                    i2 = run[count - 1];
                    while (true) {
                        i--;
                        if (i < i2) {
                            break;
                        }
                        b[i + bo] = a2[i + ao];
                    }
                    last++;
                    run[last] = m;
                }
                double[] t2 = a2;
                a2 = b;
                b = t2;
                i2 = ao;
                ao = bo;
                bo = i2;
                count = last;
                i2 = blen;
                work2 = work3;
                odd2 = odd;
                i = left;
                z = true;
                z2 = false;
            }
            work3 = work2;
            odd = odd2;
        }
        work2 = new double[i2];
        workBase2 = 0;
        if (odd2 != (byte) 0) {
        }
        while (count > z) {
        }
        work3 = work2;
        odd = odd2;
    }

    private static void sort(double[] a, int left, int right, boolean leftmost) {
        double[] dArr = a;
        int k = left;
        int right2 = right;
        boolean z = leftmost;
        int length = (right2 - k) + 1;
        int j;
        int i;
        int j2;
        if (length < INSERTION_SORT_THRESHOLD) {
            if (z) {
                j = k;
                i = j;
                while (i < right2) {
                    double ai = dArr[i + 1];
                    while (ai < dArr[j]) {
                        dArr[j + 1] = dArr[j];
                        j2 = j - 1;
                        if (j == k) {
                            j = j2;
                            break;
                        }
                        j = j2;
                    }
                    dArr[j + 1] = ai;
                    i++;
                    j = i;
                }
            } else {
                while (k < right2) {
                    k++;
                    if (dArr[k] < dArr[k - 1]) {
                        double a1;
                        j = k;
                        while (true) {
                            j++;
                            if (j > right2) {
                                break;
                            }
                            a1 = dArr[k];
                            double a2 = dArr[j];
                            if (a1 < a2) {
                                a2 = a1;
                                a1 = dArr[j];
                            }
                            while (true) {
                                k--;
                                if (a1 >= dArr[k]) {
                                    break;
                                }
                                dArr[k + 2] = dArr[k];
                            }
                            k++;
                            dArr[k + 1] = a1;
                            while (true) {
                                k--;
                                if (a2 >= dArr[k]) {
                                    break;
                                }
                                dArr[k + 1] = dArr[k];
                            }
                            dArr[k + 1] = a2;
                            j++;
                            k = j;
                        }
                        a1 = dArr[right2];
                        while (true) {
                            right2--;
                            if (a1 >= dArr[right2]) {
                                break;
                            }
                            dArr[right2 + 1] = dArr[right2];
                        }
                        dArr[right2 + 1] = a1;
                    }
                }
                return;
            }
            return;
        }
        double t;
        j = ((length >> 3) + (length >> 6)) + 1;
        i = (k + right2) >>> 1;
        int e2 = i - j;
        int e1 = e2 - j;
        j2 = i + j;
        int e5 = j2 + j;
        if (dArr[e2] < dArr[e1]) {
            t = dArr[e2];
            dArr[e2] = dArr[e1];
            dArr[e1] = t;
        }
        if (dArr[i] < dArr[e2]) {
            t = dArr[i];
            dArr[i] = dArr[e2];
            dArr[e2] = t;
            if (t < dArr[e1]) {
                dArr[e2] = dArr[e1];
                dArr[e1] = t;
            }
        }
        if (dArr[j2] < dArr[i]) {
            t = dArr[j2];
            dArr[j2] = dArr[i];
            dArr[i] = t;
            if (t < dArr[e2]) {
                dArr[i] = dArr[e2];
                dArr[e2] = t;
                if (t < dArr[e1]) {
                    dArr[e2] = dArr[e1];
                    dArr[e1] = t;
                }
            }
        }
        if (dArr[e5] < dArr[j2]) {
            t = dArr[e5];
            dArr[e5] = dArr[j2];
            dArr[j2] = t;
            if (t < dArr[i]) {
                dArr[j2] = dArr[i];
                dArr[i] = t;
                if (t < dArr[e2]) {
                    dArr[i] = dArr[e2];
                    dArr[e2] = t;
                    if (t < dArr[e1]) {
                        dArr[e2] = dArr[e1];
                        dArr[e1] = t;
                    }
                }
            }
        }
        int k2 = k;
        int great = right2;
        double pivot;
        int less;
        double ak;
        if (dArr[e1] == dArr[e2] || dArr[e2] == dArr[i] || dArr[i] == dArr[j2] || dArr[j2] == dArr[e5]) {
            pivot = dArr[i];
            less = k2;
            while (k2 <= great) {
                if (dArr[k2] != pivot) {
                    ak = dArr[k2];
                    if (ak < pivot) {
                        dArr[k2] = dArr[less];
                        dArr[less] = ak;
                        less++;
                    } else {
                        while (dArr[great] > pivot) {
                            great--;
                        }
                        if (dArr[great] < pivot) {
                            dArr[k2] = dArr[less];
                            dArr[less] = dArr[great];
                            less++;
                        } else {
                            dArr[k2] = dArr[great];
                        }
                        dArr[great] = ak;
                        great--;
                    }
                }
                k2++;
            }
            sort(dArr, k, less - 1, z);
            sort(dArr, great + 1, right2, false);
        } else {
            double ak2;
            int great2;
            pivot = dArr[e2];
            ak = dArr[j2];
            dArr[e2] = dArr[k];
            dArr[j2] = dArr[right2];
            while (true) {
                k2++;
                if (dArr[k2] >= pivot) {
                    break;
                }
            }
            while (true) {
                great--;
                if (dArr[great] <= ak) {
                    break;
                }
            }
            less = k2 - 1;
            loop9:
            while (true) {
                less++;
                if (less > great) {
                    break;
                }
                ak2 = dArr[less];
                if (ak2 < pivot) {
                    dArr[less] = dArr[k2];
                    dArr[k2] = ak2;
                    k2++;
                } else if (ak2 > ak) {
                    while (dArr[great] > ak) {
                        great2 = great - 1;
                        if (great == less) {
                            great = great2;
                            break loop9;
                        }
                        great = great2;
                    }
                    if (dArr[great] < pivot) {
                        dArr[less] = dArr[k2];
                        dArr[k2] = dArr[great];
                        k2++;
                    } else {
                        dArr[less] = dArr[great];
                    }
                    dArr[great] = ak2;
                    great--;
                } else {
                    continue;
                }
            }
            dArr[k] = dArr[k2 - 1];
            dArr[k2 - 1] = pivot;
            dArr[right2] = dArr[great + 1];
            dArr[great + 1] = ak;
            sort(dArr, k, k2 - 2, z);
            sort(dArr, great + 2, right2, false);
            if (k2 < e1 && e5 < great) {
                while (dArr[k2] == pivot) {
                    k2++;
                }
                while (dArr[great] == ak) {
                    great--;
                }
                less = k2 - 1;
                loop13:
                while (true) {
                    less++;
                    if (less > great) {
                        break;
                    }
                    ak2 = dArr[less];
                    if (ak2 == pivot) {
                        dArr[less] = dArr[k2];
                        dArr[k2] = ak2;
                        k2++;
                    } else if (ak2 == ak) {
                        while (dArr[great] == ak) {
                            great2 = great - 1;
                            if (great == less) {
                                great = great2;
                                break loop13;
                            }
                            great = great2;
                        }
                        if (dArr[great] == pivot) {
                            dArr[less] = dArr[k2];
                            dArr[k2] = dArr[great];
                            k2++;
                        } else {
                            dArr[less] = dArr[great];
                        }
                        dArr[great] = ak2;
                        great--;
                    } else {
                        continue;
                    }
                }
            }
            sort(dArr, k2, great, false);
        }
    }
}
