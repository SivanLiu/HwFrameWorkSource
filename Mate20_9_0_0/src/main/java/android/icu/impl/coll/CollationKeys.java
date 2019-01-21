package android.icu.impl.coll;

import android.icu.text.DateTimePatternGenerator;

public final class CollationKeys {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int CASE_LOWER_FIRST_COMMON_HIGH = 13;
    private static final int CASE_LOWER_FIRST_COMMON_LOW = 1;
    private static final int CASE_LOWER_FIRST_COMMON_MAX_COUNT = 7;
    private static final int CASE_LOWER_FIRST_COMMON_MIDDLE = 7;
    private static final int CASE_UPPER_FIRST_COMMON_HIGH = 15;
    private static final int CASE_UPPER_FIRST_COMMON_LOW = 3;
    private static final int CASE_UPPER_FIRST_COMMON_MAX_COUNT = 13;
    private static final int QUAT_COMMON_HIGH = 252;
    private static final int QUAT_COMMON_LOW = 28;
    private static final int QUAT_COMMON_MAX_COUNT = 113;
    private static final int QUAT_COMMON_MIDDLE = 140;
    private static final int QUAT_SHIFTED_LIMIT_BYTE = 27;
    static final int SEC_COMMON_HIGH = 69;
    private static final int SEC_COMMON_LOW = 5;
    private static final int SEC_COMMON_MAX_COUNT = 33;
    private static final int SEC_COMMON_MIDDLE = 37;
    public static final LevelCallback SIMPLE_LEVEL_FALLBACK = new LevelCallback();
    private static final int TER_LOWER_FIRST_COMMON_HIGH = 69;
    private static final int TER_LOWER_FIRST_COMMON_LOW = 5;
    private static final int TER_LOWER_FIRST_COMMON_MAX_COUNT = 33;
    private static final int TER_LOWER_FIRST_COMMON_MIDDLE = 37;
    private static final int TER_ONLY_COMMON_HIGH = 197;
    private static final int TER_ONLY_COMMON_LOW = 5;
    private static final int TER_ONLY_COMMON_MAX_COUNT = 97;
    private static final int TER_ONLY_COMMON_MIDDLE = 101;
    private static final int TER_UPPER_FIRST_COMMON_HIGH = 197;
    private static final int TER_UPPER_FIRST_COMMON_LOW = 133;
    private static final int TER_UPPER_FIRST_COMMON_MAX_COUNT = 33;
    private static final int TER_UPPER_FIRST_COMMON_MIDDLE = 165;
    private static final int[] levelMasks = new int[]{2, 6, 22, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 54};

    public static class LevelCallback {
        boolean needToWrite(int level) {
            return true;
        }
    }

    public static abstract class SortKeyByteSink {
        private int appended_ = 0;
        protected byte[] buffer_;

        protected abstract void AppendBeyondCapacity(byte[] bArr, int i, int i2, int i3);

        protected abstract boolean Resize(int i, int i2);

        public SortKeyByteSink(byte[] dest) {
            this.buffer_ = dest;
        }

        public void setBufferAndAppended(byte[] dest, int app) {
            this.buffer_ = dest;
            this.appended_ = app;
        }

        public void Append(byte[] bytes, int n) {
            if (n > 0 && bytes != null) {
                int length = this.appended_;
                this.appended_ += n;
                if (n <= this.buffer_.length - length) {
                    System.arraycopy(bytes, 0, this.buffer_, length, n);
                } else {
                    AppendBeyondCapacity(bytes, 0, n, length);
                }
            }
        }

        public void Append(int b) {
            if (this.appended_ < this.buffer_.length || Resize(1, this.appended_)) {
                this.buffer_[this.appended_] = (byte) b;
            }
            this.appended_++;
        }

        public int NumberOfBytesAppended() {
            return this.appended_;
        }

        public int GetRemainingCapacity() {
            return this.buffer_.length - this.appended_;
        }

        public boolean Overflowed() {
            return this.appended_ > this.buffer_.length;
        }
    }

    private static final class SortKeyLevel {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final int INITIAL_CAPACITY = 40;
        byte[] buffer = new byte[40];
        int len = 0;

        static {
            Class cls = CollationKeys.class;
        }

        SortKeyLevel() {
        }

        boolean isEmpty() {
            return this.len == 0;
        }

        int length() {
            return this.len;
        }

        byte getAt(int index) {
            return this.buffer[index];
        }

        byte[] data() {
            return this.buffer;
        }

        void appendByte(int b) {
            if (this.len < this.buffer.length || ensureCapacity(1)) {
                byte[] bArr = this.buffer;
                int i = this.len;
                this.len = i + 1;
                bArr[i] = (byte) b;
            }
        }

        void appendWeight16(int w) {
            byte b0 = (byte) (w >>> 8);
            byte b1 = (byte) w;
            int appendLength = b1 == (byte) 0 ? 1 : 2;
            if (this.len + appendLength <= this.buffer.length || ensureCapacity(appendLength)) {
                byte[] bArr = this.buffer;
                int i = this.len;
                this.len = i + 1;
                bArr[i] = b0;
                if (b1 != (byte) 0) {
                    bArr = this.buffer;
                    i = this.len;
                    this.len = i + 1;
                    bArr[i] = b1;
                }
            }
        }

        void appendWeight32(long w) {
            int appendLength = 4;
            byte[] bytes = new byte[]{(byte) ((int) (w >>> 24)), (byte) ((int) (w >>> 16)), (byte) ((int) (w >>> 8)), (byte) ((int) w)};
            if (bytes[1] == (byte) 0) {
                appendLength = 1;
            } else if (bytes[2] == (byte) 0) {
                appendLength = 2;
            } else if (bytes[3] == (byte) 0) {
                appendLength = 3;
            }
            if (this.len + appendLength <= this.buffer.length || ensureCapacity(appendLength)) {
                byte[] bArr = this.buffer;
                int i = this.len;
                this.len = i + 1;
                bArr[i] = bytes[0];
                if (bytes[1] != (byte) 0) {
                    bArr = this.buffer;
                    int i2 = this.len;
                    this.len = i2 + 1;
                    bArr[i2] = bytes[1];
                    if (bytes[2] != (byte) 0) {
                        bArr = this.buffer;
                        i2 = this.len;
                        this.len = i2 + 1;
                        bArr[i2] = bytes[2];
                        if (bytes[3] != (byte) 0) {
                            bArr = this.buffer;
                            i2 = this.len;
                            this.len = i2 + 1;
                            bArr[i2] = bytes[3];
                        }
                    }
                }
            }
        }

        void appendReverseWeight16(int w) {
            byte b0 = (byte) (w >>> 8);
            byte b1 = (byte) w;
            int appendLength = b1 == (byte) 0 ? 1 : 2;
            if (this.len + appendLength > this.buffer.length && !ensureCapacity(appendLength)) {
                return;
            }
            if (b1 == (byte) 0) {
                byte[] bArr = this.buffer;
                int i = this.len;
                this.len = i + 1;
                bArr[i] = b0;
                return;
            }
            this.buffer[this.len] = b1;
            this.buffer[this.len + 1] = b0;
            this.len += 2;
        }

        void appendTo(SortKeyByteSink sink) {
            sink.Append(this.buffer, this.len - 1);
        }

        private boolean ensureCapacity(int appendCapacity) {
            int newCapacity = this.buffer.length * 2;
            int altCapacity = this.len + (2 * appendCapacity);
            if (newCapacity < altCapacity) {
                newCapacity = altCapacity;
            }
            if (newCapacity < 200) {
                newCapacity = 200;
            }
            byte[] newbuf = new byte[newCapacity];
            System.arraycopy(this.buffer, 0, newbuf, 0, this.len);
            this.buffer = newbuf;
            return true;
        }
    }

    private static SortKeyLevel getSortKeyLevel(int levels, int level) {
        return (levels & level) != 0 ? new SortKeyLevel() : null;
    }

    private CollationKeys() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:168:0x02b6  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x0280 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x039f  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x02e6  */
    /* JADX WARNING: Removed duplicated region for block: B:237:0x03a5  */
    /* JADX WARNING: Removed duplicated region for block: B:297:0x046b  */
    /* JADX WARNING: Removed duplicated region for block: B:300:0x03f8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x01d6  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x01a8  */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0276  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x0280 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x02b6  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x02e6  */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x039f  */
    /* JADX WARNING: Removed duplicated region for block: B:237:0x03a5  */
    /* JADX WARNING: Removed duplicated region for block: B:300:0x03f8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:297:0x046b  */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x0276  */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x02b6  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x0280 A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:234:0x039f  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x02e6  */
    /* JADX WARNING: Removed duplicated region for block: B:237:0x03a5  */
    /* JADX WARNING: Removed duplicated region for block: B:297:0x046b  */
    /* JADX WARNING: Removed duplicated region for block: B:300:0x03f8 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void writeSortKeyUpToQuaternary(CollationIterator iter, boolean[] compressibleBytes, CollationSettings settings, SortKeyByteSink sink, int minLevel, LevelCallback callback, boolean preflight) {
        CollationSettings collationSettings = settings;
        SortKeyByteSink sortKeyByteSink = sink;
        LevelCallback levelCallback = callback;
        int options = collationSettings.options;
        int levels = levelMasks[CollationSettings.getStrength(options)];
        if ((options & 1024) != 0) {
            levels |= 8;
        }
        levels &= ~((1 << minLevel) - 1);
        if (levels != 0) {
            long variableTop;
            if ((options & 12) == 0) {
                variableTop = 0;
            } else {
                variableTop = collationSettings.variableTop + 1;
            }
            int tertiaryMask = CollationSettings.getTertiaryMask(options);
            byte[] p234 = new byte[3];
            SortKeyLevel cases = getSortKeyLevel(levels, 8);
            SortKeyLevel secondaries = getSortKeyLevel(levels, 4);
            SortKeyLevel tertiaries = getSortKeyLevel(levels, 16);
            long j = 32;
            SortKeyLevel quaternaries = getSortKeyLevel(levels, 32);
            int commonCases = 0;
            int commonSecondaries = 0;
            int commonTertiaries = 0;
            int commonQuaternaries = 0;
            int secSegmentStart = 0;
            byte[] p2342 = p234;
            long prevReorderedPrimary = 0;
            int prevSecondary = 0;
            while (true) {
                SortKeyLevel tertiaries2;
                int options2;
                long ce;
                long p;
                byte[] p2343;
                int p1;
                byte p2;
                int i;
                iter.clearCEsIfNoneRemaining();
                long ce2 = iter.nextCE();
                long p3 = ce2 >>> j;
                if (p3 >= variableTop || p3 <= Collation.MERGE_SEPARATOR_PRIMARY) {
                    tertiaries2 = tertiaries;
                    Object obj = j;
                    options2 = options;
                    ce = ce2;
                    p = p3;
                } else {
                    int commonQuaternaries2;
                    int commonQuaternaries3;
                    if (commonQuaternaries != 0) {
                        commonQuaternaries2 = commonQuaternaries - 1;
                        while (commonQuaternaries2 >= 113) {
                            quaternaries.appendByte(140);
                            commonQuaternaries2 -= 113;
                        }
                        quaternaries.appendByte(28 + commonQuaternaries2);
                        commonQuaternaries = 0;
                    }
                    tertiaries2 = tertiaries;
                    commonQuaternaries2 = commonQuaternaries;
                    tertiaries = p3;
                    while (true) {
                        if ((levels & 32) != 0) {
                            if (settings.hasReordering()) {
                                tertiaries = collationSettings.reorder(tertiaries);
                            }
                            commonQuaternaries3 = commonQuaternaries2;
                            if ((((int) tertiaries) >>> 24) >= 27) {
                                quaternaries.appendByte(27);
                            }
                            quaternaries.appendWeight32(tertiaries);
                        } else {
                            commonQuaternaries3 = commonQuaternaries2;
                        }
                        do {
                            ce2 = iter.nextCE();
                            p = ce2 >>> 32;
                        } while (p == 0);
                        if (p >= variableTop || p <= Collation.MERGE_SEPARATOR_PRIMARY) {
                            options2 = options;
                            ce = ce2;
                            commonQuaternaries = commonQuaternaries3;
                        } else {
                            commonQuaternaries2 = commonQuaternaries3;
                            levelCallback = callback;
                        }
                    }
                    options2 = options;
                    ce = ce2;
                    commonQuaternaries = commonQuaternaries3;
                }
                long variableTop2 = variableTop;
                if (p <= 1 || (levels & 2) == 0) {
                    p2343 = p2342;
                } else {
                    boolean isCompressible = compressibleBytes[((int) p) >>> 24];
                    if (settings.hasReordering()) {
                        p = collationSettings.reorder(p);
                    }
                    p1 = ((int) p) >>> 24;
                    if (!(isCompressible && p1 == (((int) prevReorderedPrimary) >>> 24))) {
                        if (prevReorderedPrimary != 0) {
                            if (p >= prevReorderedPrimary) {
                                sortKeyByteSink.Append(255);
                            } else if (p1 > 2) {
                                sortKeyByteSink.Append(3);
                            }
                        }
                        sortKeyByteSink.Append(p1);
                        if (isCompressible) {
                            prevReorderedPrimary = p;
                        } else {
                            prevReorderedPrimary = 0;
                        }
                    }
                    p2 = (byte) ((int) (p >>> 16));
                    if (p2 != (byte) 0) {
                        p2342[0] = p2;
                        p2342[1] = (byte) ((int) (p >>> 8));
                        p2342[2] = (byte) ((int) p);
                        i = p2342[1] == (byte) 0 ? 1 : p2342[2] == (byte) 0 ? 2 : 3;
                        p2343 = p2342;
                        sortKeyByteSink = sink;
                        sortKeyByteSink.Append(p2343, i);
                    } else {
                        p2343 = p2342;
                    }
                    if (!preflight && sink.Overflowed()) {
                        return;
                    }
                }
                p1 = (int) ce;
                if (p1 == 0) {
                    p2342 = p2343;
                    tertiaries = tertiaries2;
                    options = options2;
                    variableTop = variableTop2;
                    collationSettings = settings;
                    ce = callback;
                    j = 32;
                } else {
                    int prevSecondary2;
                    int options3;
                    byte[] p2344;
                    long prevReorderedPrimary2;
                    if ((levels & 4) != 0) {
                        i = p1 >>> 16;
                        if (i == 0) {
                            long j2 = ce;
                            p2344 = p2343;
                            prevReorderedPrimary2 = prevReorderedPrimary;
                            prevSecondary2 = prevSecondary;
                            options3 = options2;
                        } else {
                            if (i == 1280) {
                                options3 = options2;
                                if ((options3 & 2048) == 0 || p != Collation.MERGE_SEPARATOR_PRIMARY) {
                                    commonSecondaries++;
                                    p2344 = p2343;
                                }
                                int b;
                                if ((options3 & 2048) != 0) {
                                    if (commonSecondaries != 0) {
                                        options = commonSecondaries - 1;
                                        while (true) {
                                            p2344 = p2343;
                                            if (options < 33) {
                                                break;
                                            }
                                            secondaries.appendByte(37);
                                            options -= 33;
                                            p2343 = p2344;
                                        }
                                        if (i < 1280) {
                                            b = 5 + options;
                                        } else {
                                            b = 69 - options;
                                        }
                                        secondaries.appendByte(b);
                                        commonSecondaries = 0;
                                    } else {
                                        p2344 = p2343;
                                    }
                                    secondaries.appendWeight16(i);
                                } else {
                                    int b2;
                                    p2344 = p2343;
                                    if (commonSecondaries != 0) {
                                        commonSecondaries--;
                                        options = commonSecondaries % 33;
                                        prevReorderedPrimary2 = prevReorderedPrimary;
                                        if (prevSecondary < 1280) {
                                            b2 = 5 + options;
                                        } else {
                                            b2 = 69 - options;
                                        }
                                        secondaries.appendByte(b2);
                                        commonSecondaries -= options;
                                        while (commonSecondaries > 0) {
                                            secondaries.appendByte(37);
                                            commonSecondaries -= 33;
                                        }
                                    } else {
                                        prevReorderedPrimary2 = prevReorderedPrimary;
                                        prevSecondary2 = prevSecondary;
                                    }
                                    int b3;
                                    if (0 >= p || p > Collation.MERGE_SEPARATOR_PRIMARY) {
                                        SortKeyLevel tertiaries3;
                                        secondaries.appendReverseWeight16(i);
                                        prevSecondary = i;
                                        if ((levels & 8) != 0 && (CollationSettings.getStrength(options3) != 0 ? (p1 >>> 16) != 0 : p != 0)) {
                                            i = (p1 >>> 8) & 255;
                                            if ((i & 192) == 0 || i <= 1) {
                                                if ((options3 & 256) != 0) {
                                                    if (commonCases != 0 && (i > 1 || !cases.isEmpty())) {
                                                        options = commonCases - 1;
                                                        while (options >= 7) {
                                                            cases.appendByte(112);
                                                            options -= 7;
                                                        }
                                                        if (i <= 1) {
                                                            b3 = 1 + options;
                                                        } else {
                                                            b3 = 13 - options;
                                                        }
                                                        cases.appendByte(b3 << 4);
                                                        commonCases = 0;
                                                    }
                                                    if (i > 1) {
                                                        i = (13 + (i >>> 6)) << 4;
                                                    }
                                                } else {
                                                    if (commonCases != 0) {
                                                        options = commonCases - 1;
                                                        while (options >= 13) {
                                                            cases.appendByte(48);
                                                            options -= 13;
                                                        }
                                                        cases.appendByte((3 + options) << 4);
                                                        commonCases = 0;
                                                    }
                                                    if (i > 1) {
                                                        i = (3 - (i >>> 6)) << 4;
                                                    }
                                                }
                                                cases.appendByte(i);
                                            } else {
                                                commonCases++;
                                            }
                                        }
                                        if ((levels & 16) == 0) {
                                            i = p1 & tertiaryMask;
                                            if (i == Collation.COMMON_WEIGHT16) {
                                                commonTertiaries++;
                                                tertiaries3 = tertiaries2;
                                            } else if ((32768 & tertiaryMask) == 0) {
                                                if (commonTertiaries != 0) {
                                                    prevSecondary2 = commonTertiaries - 1;
                                                    while (prevSecondary2 >= 97) {
                                                        tertiaries2.appendByte(101);
                                                        prevSecondary2 -= 97;
                                                    }
                                                    tertiaries3 = tertiaries2;
                                                    if (i < Collation.COMMON_WEIGHT16) {
                                                        b3 = 5 + prevSecondary2;
                                                    } else {
                                                        b3 = 197 - prevSecondary2;
                                                    }
                                                    tertiaries3.appendByte(b3);
                                                    commonTertiaries = 0;
                                                } else {
                                                    tertiaries3 = tertiaries2;
                                                }
                                                if (i > Collation.COMMON_WEIGHT16) {
                                                    i += Collation.CASE_MASK;
                                                }
                                                tertiaries3.appendWeight16(i);
                                            } else {
                                                tertiaries3 = tertiaries2;
                                                if ((options3 & 256) == 0) {
                                                    if (commonTertiaries != 0) {
                                                        prevSecondary2 = commonTertiaries - 1;
                                                        while (prevSecondary2 >= 33) {
                                                            tertiaries3.appendByte(37);
                                                            prevSecondary2 -= 33;
                                                        }
                                                        if (i < Collation.COMMON_WEIGHT16) {
                                                            b = 5 + prevSecondary2;
                                                        } else {
                                                            b = 69 - prevSecondary2;
                                                        }
                                                        tertiaries3.appendByte(b);
                                                        commonTertiaries = 0;
                                                    }
                                                    if (i > Collation.COMMON_WEIGHT16) {
                                                        i += 16384;
                                                    }
                                                    tertiaries3.appendWeight16(i);
                                                } else {
                                                    if (i > 256) {
                                                        if ((p1 >>> 16) != 0) {
                                                            i ^= Collation.CASE_MASK;
                                                            if (i < 50432) {
                                                                i -= 16384;
                                                            }
                                                        } else {
                                                            i += 16384;
                                                        }
                                                    }
                                                    if (commonTertiaries != 0) {
                                                        prevSecondary2 = commonTertiaries - 1;
                                                        while (prevSecondary2 >= 33) {
                                                            tertiaries3.appendByte(165);
                                                            prevSecondary2 -= 33;
                                                        }
                                                        if (i < 34048) {
                                                            b3 = 133 + prevSecondary2;
                                                        } else {
                                                            b3 = 197 - prevSecondary2;
                                                        }
                                                        tertiaries3.appendByte(b3);
                                                        commonTertiaries = 0;
                                                    }
                                                    tertiaries3.appendWeight16(i);
                                                }
                                            }
                                        } else {
                                            tertiaries3 = tertiaries2;
                                        }
                                        if ((levels & 32) != 0) {
                                            i = DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & p1;
                                            if ((i & 192) == 0 && i > 256) {
                                                commonQuaternaries++;
                                            } else if (i == 256 && (options3 & 12) == 0 && quaternaries.isEmpty()) {
                                                quaternaries.appendByte(1);
                                            } else {
                                                if (i == 256) {
                                                    i = 1;
                                                } else {
                                                    i = 252 + ((i >>> 6) & 3);
                                                }
                                                if (commonQuaternaries != 0) {
                                                    options = commonQuaternaries - 1;
                                                    while (options >= 113) {
                                                        quaternaries.appendByte(140);
                                                        options -= 113;
                                                    }
                                                    if (i < 28) {
                                                        prevSecondary2 = 28 + options;
                                                    } else {
                                                        prevSecondary2 = 252 - options;
                                                    }
                                                    quaternaries.appendByte(prevSecondary2);
                                                    commonQuaternaries = 0;
                                                }
                                                quaternaries.appendByte(i);
                                            }
                                        }
                                        int i2;
                                        if ((p1 >>> 24) != 1) {
                                            LevelCallback levelCallback2;
                                            if ((levels & 4) != 0) {
                                                levelCallback2 = callback;
                                                if (levelCallback2.needToWrite(2)) {
                                                    sortKeyByteSink.Append(1);
                                                    secondaries.appendTo(sortKeyByteSink);
                                                } else {
                                                    return;
                                                }
                                            }
                                            levelCallback2 = callback;
                                            if ((levels & 8) != 0) {
                                                if (levelCallback2.needToWrite(3)) {
                                                    sortKeyByteSink.Append(1);
                                                    int length = cases.length() - 1;
                                                    byte b4 = (byte) 0;
                                                    for (i2 = 0; i2 < length; i2++) {
                                                        p2 = cases.getAt(i2);
                                                        if (b4 == (byte) 0) {
                                                            b4 = p2;
                                                        } else {
                                                            sortKeyByteSink.Append(((p2 >> 4) & 15) | b4);
                                                            b4 = (byte) 0;
                                                        }
                                                    }
                                                    if (b4 != (byte) 0) {
                                                        sortKeyByteSink.Append(b4);
                                                    }
                                                } else {
                                                    return;
                                                }
                                            }
                                            if ((levels & 16) != 0) {
                                                if (levelCallback2.needToWrite(4)) {
                                                    sortKeyByteSink.Append(1);
                                                    tertiaries3.appendTo(sortKeyByteSink);
                                                } else {
                                                    return;
                                                }
                                            }
                                            if ((levels & 32) != 0 && levelCallback2.needToWrite(5)) {
                                                sortKeyByteSink.Append(1);
                                                quaternaries.appendTo(sortKeyByteSink);
                                                return;
                                            }
                                            return;
                                        }
                                        i2 = 1;
                                        options = options3;
                                        tertiaries = tertiaries3;
                                        variableTop = variableTop2;
                                        p2342 = p2344;
                                        prevReorderedPrimary = prevReorderedPrimary2;
                                        j = 32;
                                        options3 = callback;
                                        collationSettings = settings;
                                    } else {
                                        byte[] secs = secondaries.data();
                                        b3 = secondaries.length() - 1;
                                        b2 = secSegmentStart;
                                        while (b2 < b3) {
                                            byte b5 = secs[b2];
                                            int secSegmentStart2 = b2 + 1;
                                            secs[b2] = secs[b3];
                                            b2 = b3 - 1;
                                            secs[b3] = b5;
                                            b3 = b2;
                                            b2 = secSegmentStart2;
                                        }
                                        if (p == 1) {
                                            byte[] bArr = secs;
                                            secs = 1;
                                        } else {
                                            secs = 2;
                                        }
                                        secondaries.appendByte(secs);
                                        prevSecondary = 0;
                                        secSegmentStart = secondaries.length();
                                        i = (p1 >>> 8) & 255;
                                        if ((i & 192) == 0) {
                                        }
                                        if ((options3 & 256) != 0) {
                                        }
                                        cases.appendByte(i);
                                        if ((levels & 16) == 0) {
                                        }
                                        if ((levels & 32) != 0) {
                                        }
                                        if ((p1 >>> 24) != 1) {
                                        }
                                    }
                                }
                            } else {
                                options3 = options2;
                                if ((options3 & 2048) != 0) {
                                }
                            }
                            prevReorderedPrimary2 = prevReorderedPrimary;
                            i = (p1 >>> 8) & 255;
                            if ((i & 192) == 0) {
                            }
                            if ((options3 & 256) != 0) {
                            }
                            cases.appendByte(i);
                            if ((levels & 16) == 0) {
                            }
                            if ((levels & 32) != 0) {
                            }
                            if ((p1 >>> 24) != 1) {
                            }
                        }
                    } else {
                        p2344 = p2343;
                        prevReorderedPrimary2 = prevReorderedPrimary;
                        prevSecondary2 = prevSecondary;
                        options3 = options2;
                    }
                    prevSecondary = prevSecondary2;
                    i = (p1 >>> 8) & 255;
                    if ((i & 192) == 0) {
                    }
                    if ((options3 & 256) != 0) {
                    }
                    cases.appendByte(i);
                    if ((levels & 16) == 0) {
                    }
                    if ((levels & 32) != 0) {
                    }
                    if ((p1 >>> 24) != 1) {
                    }
                }
            }
        }
    }
}
