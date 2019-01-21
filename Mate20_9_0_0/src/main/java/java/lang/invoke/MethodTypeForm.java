package java.lang.invoke;

import sun.invoke.util.Wrapper;

final class MethodTypeForm {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int ERASE = 1;
    public static final int INTS = 4;
    public static final int LONGS = 5;
    public static final int NO_CHANGE = 0;
    public static final int RAW_RETURN = 6;
    public static final int UNWRAP = 3;
    public static final int WRAP = 2;
    final long argCounts;
    final int[] argToSlotTable;
    final MethodType basicType;
    final MethodType erasedType;
    final long primCounts;
    final int[] slotToArgTable;

    public MethodType erasedType() {
        return this.erasedType;
    }

    public MethodType basicType() {
        return this.basicType;
    }

    private boolean assertIsBasicType() {
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x0094  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x008a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected MethodTypeForm(MethodType erasedType) {
        Class<?> pt;
        int rtypeCount;
        int rtypeCount2;
        MethodType methodType = erasedType;
        this.erasedType = methodType;
        Class<?>[] ptypes = erasedType.ptypes();
        int ptypeCount = ptypes.length;
        int pslotCount = ptypeCount;
        int rtypeCount3 = 1;
        int lac = 0;
        int prc = 0;
        int lrc = 0;
        Class<?>[] epts = ptypes;
        Class<?>[] bpts = epts;
        int pac = 0;
        int i = 0;
        while (true) {
            Class<?>[] ptypes2 = ptypes;
            if (i >= epts.length) {
                break;
            }
            pt = epts[i];
            rtypeCount = rtypeCount3;
            if (pt != Object.class) {
                pac++;
                rtypeCount3 = Wrapper.forPrimitiveType(pt);
                if (rtypeCount3.isDoubleWord()) {
                    lac++;
                }
                if (rtypeCount3.isSubwordOrInt()) {
                    Wrapper w = rtypeCount3;
                    if (pt != Integer.TYPE) {
                        if (bpts == epts) {
                            bpts = (Class[]) bpts.clone();
                        }
                        bpts[i] = Integer.TYPE;
                    }
                }
            }
            i++;
            ptypes = ptypes2;
            rtypeCount3 = rtypeCount;
        }
        rtypeCount = rtypeCount3;
        pslotCount += lac;
        pt = erasedType.returnType();
        Class<?> bt = pt;
        Class<?> bt2;
        if (pt != Object.class) {
            Class<?> cls;
            prc = 0 + 1;
            Wrapper w2 = Wrapper.forPrimitiveType(pt);
            if (w2.isDoubleWord()) {
                lrc = 0 + 1;
            }
            if (w2.isSubwordOrInt()) {
                bt2 = bt;
                if (pt != Integer.TYPE) {
                    bt = Integer.TYPE;
                    cls = bt;
                    if (pt != Void.TYPE) {
                        rtypeCount2 = 0;
                        rtypeCount = 0;
                    } else {
                        rtypeCount2 = 1 + lrc;
                    }
                    i = rtypeCount2;
                    rtypeCount2 = rtypeCount;
                    bt = cls;
                }
            } else {
                bt2 = bt;
            }
            bt = bt2;
            cls = bt;
            if (pt != Void.TYPE) {
            }
            i = rtypeCount2;
            rtypeCount2 = rtypeCount;
            bt = cls;
        } else {
            bt2 = bt;
            i = 1;
            rtypeCount2 = rtypeCount;
        }
        int[] argToSlotTab = null;
        Class<?>[] epts2;
        if (epts == bpts && bt == pt) {
            int[] slotToArgTab;
            int[] argToSlotTab2;
            this.basicType = methodType;
            int[] slotToArgTab2;
            if (lac != 0) {
                rtypeCount = ptypeCount + lac;
                slotToArgTab = new int[(rtypeCount + 1)];
                argToSlotTab2 = new int[(1 + ptypeCount)];
                argToSlotTab2[0] = rtypeCount;
                int i2 = 0;
                while (true) {
                    slotToArgTab2 = i2;
                    Class<?> rt = pt;
                    if (slotToArgTab2 >= epts.length) {
                        break;
                    }
                    epts2 = epts;
                    if (Wrapper.forBasicType(epts[slotToArgTab2]).isDoubleWord()) {
                        rtypeCount--;
                    }
                    rtypeCount--;
                    slotToArgTab[rtypeCount] = slotToArgTab2 + 1;
                    argToSlotTab2[1 + slotToArgTab2] = rtypeCount;
                    i2 = slotToArgTab2 + 1;
                    pt = rt;
                    epts = epts2;
                }
            } else {
                epts2 = epts;
                if (pac != 0) {
                    argToSlotTab2 = MethodType.genericMethodType(ptypeCount).form();
                    slotToArgTab = argToSlotTab2.slotToArgTable;
                    argToSlotTab2 = argToSlotTab2.argToSlotTable;
                } else {
                    int slot = ptypeCount;
                    slotToArgTab = new int[(slot + 1)];
                    pt = new int[(1 + ptypeCount)];
                    slotToArgTab2 = null;
                    pt[0] = slot;
                    while (slotToArgTab2 < ptypeCount) {
                        slot--;
                        slotToArgTab[slot] = slotToArgTab2 + 1;
                        pt[1 + slotToArgTab2] = slot;
                        slotToArgTab2++;
                    }
                    argToSlotTab2 = pt;
                }
            }
            int[] slotToArgTab3 = slotToArgTab;
            this.primCounts = pack(lrc, prc, lac, pac);
            this.argCounts = pack(i, rtypeCount2, pslotCount, ptypeCount);
            this.argToSlotTable = argToSlotTab2;
            this.slotToArgTable = slotToArgTab3;
            if (pslotCount >= 256) {
                throw MethodHandleStatics.newIllegalArgumentException("too many arguments");
            }
            return;
        }
        epts2 = epts;
        this.basicType = MethodType.makeImpl(bt, bpts, true);
        MethodTypeForm that = this.basicType.form();
        this.primCounts = that.primCounts;
        this.argCounts = that.argCounts;
        this.argToSlotTable = that.argToSlotTable;
        this.slotToArgTable = that.slotToArgTable;
    }

    private static long pack(int a, int b, int c, int d) {
        return (((long) ((a << 16) | b)) << 32) | ((long) ((c << 16) | d));
    }

    private static char unpack(long packed, int word) {
        return (char) ((int) (packed >> ((3 - word) * 16)));
    }

    public int parameterCount() {
        return unpack(this.argCounts, 3);
    }

    public int parameterSlotCount() {
        return unpack(this.argCounts, 2);
    }

    public int returnCount() {
        return unpack(this.argCounts, 1);
    }

    public int returnSlotCount() {
        return unpack(this.argCounts, 0);
    }

    public int primitiveParameterCount() {
        return unpack(this.primCounts, 3);
    }

    public int longPrimitiveParameterCount() {
        return unpack(this.primCounts, 2);
    }

    public int primitiveReturnCount() {
        return unpack(this.primCounts, 1);
    }

    public int longPrimitiveReturnCount() {
        return unpack(this.primCounts, 0);
    }

    public boolean hasPrimitives() {
        return this.primCounts != 0 ? true : $assertionsDisabled;
    }

    public boolean hasNonVoidPrimitives() {
        int i = (this.primCounts > 0 ? 1 : (this.primCounts == 0 ? 0 : -1));
        boolean z = $assertionsDisabled;
        if (i == 0) {
            return $assertionsDisabled;
        }
        if (primitiveParameterCount() != 0) {
            return true;
        }
        if (!(primitiveReturnCount() == 0 || returnCount() == 0)) {
            z = true;
        }
        return z;
    }

    public boolean hasLongPrimitives() {
        return (longPrimitiveParameterCount() | longPrimitiveReturnCount()) != 0 ? true : $assertionsDisabled;
    }

    public int parameterToArgSlot(int i) {
        return this.argToSlotTable[1 + i];
    }

    public int argSlotToParameter(int argSlot) {
        return this.slotToArgTable[argSlot] - 1;
    }

    static MethodTypeForm findForm(MethodType mt) {
        MethodType erased = canonicalize(mt, 1, 1);
        if (erased == null) {
            return new MethodTypeForm(mt);
        }
        return erased.form();
    }

    public static MethodType canonicalize(MethodType mt, int howRet, int howArgs) {
        Class<?>[] ptypes = mt.ptypes();
        Class<?>[] ptc = canonicalizeAll(ptypes, howArgs);
        Class<?> rtype = mt.returnType();
        Class<?> rtc = canonicalize(rtype, howRet);
        if (ptc == null && rtc == null) {
            return null;
        }
        if (rtc == null) {
            rtc = rtype;
        }
        if (ptc == null) {
            ptc = ptypes;
        }
        return MethodType.makeImpl(rtc, ptc, true);
    }

    /* JADX WARNING: Missing block: B:8:0x0013, code skipped:
            if (r5 != 6) goto L_0x0035;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static Class<?> canonicalize(Class<?> t, int how) {
        if (t != Object.class) {
            if (!t.isPrimitive()) {
                if (how != 1) {
                    if (how == 3) {
                        Class<?> ct = Wrapper.asPrimitiveType(t);
                        if (ct != t) {
                            return ct;
                        }
                    }
                }
                return Object.class;
            } else if (t == Void.TYPE) {
                if (how == 2) {
                    return Void.class;
                }
                if (how == 6) {
                    return Integer.TYPE;
                }
            } else if (how == 2) {
                return Wrapper.asWrapperType(t);
            } else {
                switch (how) {
                    case 4:
                        if (t == Integer.TYPE || t == Long.TYPE) {
                            return null;
                        }
                        if (t == Double.TYPE) {
                            return Long.TYPE;
                        }
                        return Integer.TYPE;
                    case 5:
                        if (t == Long.TYPE) {
                            return null;
                        }
                        return Long.TYPE;
                    case 6:
                        if (t == Integer.TYPE || t == Long.TYPE || t == Float.TYPE || t == Double.TYPE) {
                            return null;
                        }
                        return Integer.TYPE;
                }
            }
        }
        return null;
    }

    static Class<?>[] canonicalizeAll(Class<?>[] ts, int how) {
        Class<?>[] cs = null;
        int imax = ts.length;
        for (int i = 0; i < imax; i++) {
            Class<?> c = canonicalize(ts[i], how);
            if (c == Void.TYPE) {
                c = null;
            }
            if (c != null) {
                if (cs == null) {
                    cs = (Class[]) ts.clone();
                }
                cs[i] = c;
            }
        }
        return cs;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Form");
        stringBuilder.append(this.erasedType);
        return stringBuilder.toString();
    }
}
