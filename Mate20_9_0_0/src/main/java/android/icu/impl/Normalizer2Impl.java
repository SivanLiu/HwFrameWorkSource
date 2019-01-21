package android.icu.impl;

import android.icu.impl.ICUBinary.Authenticate;
import android.icu.impl.Trie2.Range;
import android.icu.impl.Trie2.ValueMapper;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.VersionInfo;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

public final class Normalizer2Impl {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int CANON_HAS_COMPOSITIONS = 1073741824;
    private static final int CANON_HAS_SET = 2097152;
    private static final int CANON_NOT_SEGMENT_STARTER = Integer.MIN_VALUE;
    private static final int CANON_VALUE_MASK = 2097151;
    public static final int COMP_1_LAST_TUPLE = 32768;
    public static final int COMP_1_TRAIL_LIMIT = 13312;
    public static final int COMP_1_TRAIL_MASK = 32766;
    public static final int COMP_1_TRAIL_SHIFT = 9;
    public static final int COMP_1_TRIPLE = 1;
    public static final int COMP_2_TRAIL_MASK = 65472;
    public static final int COMP_2_TRAIL_SHIFT = 6;
    private static final int DATA_FORMAT = 1316121906;
    public static final int DELTA_SHIFT = 3;
    public static final int DELTA_TCCC_0 = 0;
    public static final int DELTA_TCCC_1 = 2;
    public static final int DELTA_TCCC_GT_1 = 4;
    public static final int DELTA_TCCC_MASK = 6;
    public static final int HAS_COMP_BOUNDARY_AFTER = 1;
    public static final int INERT = 1;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
    public static final int IX_COUNT = 20;
    public static final int IX_EXTRA_DATA_OFFSET = 1;
    public static final int IX_LIMIT_NO_NO = 12;
    public static final int IX_MIN_COMP_NO_MAYBE_CP = 9;
    public static final int IX_MIN_DECOMP_NO_CP = 8;
    public static final int IX_MIN_LCCC_CP = 18;
    public static final int IX_MIN_MAYBE_YES = 13;
    public static final int IX_MIN_NO_NO = 11;
    public static final int IX_MIN_NO_NO_COMP_BOUNDARY_BEFORE = 15;
    public static final int IX_MIN_NO_NO_COMP_NO_MAYBE_CC = 16;
    public static final int IX_MIN_NO_NO_EMPTY = 17;
    public static final int IX_MIN_YES_NO = 10;
    public static final int IX_MIN_YES_NO_MAPPINGS_ONLY = 14;
    public static final int IX_NORM_TRIE_OFFSET = 0;
    public static final int IX_RESERVED3_OFFSET = 3;
    public static final int IX_SMALL_FCD_OFFSET = 2;
    public static final int IX_TOTAL_SIZE = 7;
    public static final int JAMO_L = 2;
    public static final int JAMO_VT = 65024;
    public static final int MAPPING_HAS_CCC_LCCC_WORD = 128;
    public static final int MAPPING_HAS_RAW_MAPPING = 64;
    public static final int MAPPING_LENGTH_MASK = 31;
    public static final int MAX_DELTA = 64;
    public static final int MIN_NORMAL_MAYBE_YES = 64512;
    public static final int MIN_YES_YES_WITH_CC = 65026;
    public static final int OFFSET_SHIFT = 1;
    private static final ValueMapper segmentStarterMapper = new ValueMapper() {
        public int map(int in) {
            return Integer.MIN_VALUE & in;
        }
    };
    private Trie2_32 canonIterData;
    private ArrayList<UnicodeSet> canonStartSets;
    private int centerNoNoDelta;
    private VersionInfo dataVersion;
    private String extraData;
    private int limitNoNo;
    private String maybeYesCompositions;
    private int minCompNoMaybeCP;
    private int minDecompNoCP;
    private int minLcccCP;
    private int minMaybeYes;
    private int minNoNo;
    private int minNoNoCompBoundaryBefore;
    private int minNoNoCompNoMaybeCC;
    private int minNoNoEmpty;
    private int minYesNo;
    private int minYesNoMappingsOnly;
    private Trie2_16 normTrie;
    private byte[] smallFCD;

    public static final class Hangul {
        public static final int HANGUL_BASE = 44032;
        public static final int HANGUL_COUNT = 11172;
        public static final int HANGUL_END = 55203;
        public static final int HANGUL_LIMIT = 55204;
        public static final int JAMO_L_BASE = 4352;
        public static final int JAMO_L_COUNT = 19;
        public static final int JAMO_L_END = 4370;
        public static final int JAMO_L_LIMIT = 4371;
        public static final int JAMO_T_BASE = 4519;
        public static final int JAMO_T_COUNT = 28;
        public static final int JAMO_T_END = 4546;
        public static final int JAMO_VT_COUNT = 588;
        public static final int JAMO_V_BASE = 4449;
        public static final int JAMO_V_COUNT = 21;
        public static final int JAMO_V_END = 4469;
        public static final int JAMO_V_LIMIT = 4470;

        public static boolean isHangul(int c) {
            return HANGUL_BASE <= c && c < HANGUL_LIMIT;
        }

        public static boolean isHangulLV(int c) {
            c -= HANGUL_BASE;
            return c >= 0 && c < HANGUL_COUNT && c % 28 == 0;
        }

        public static boolean isJamoL(int c) {
            return JAMO_L_BASE <= c && c < JAMO_L_LIMIT;
        }

        public static boolean isJamoV(int c) {
            return JAMO_V_BASE <= c && c < JAMO_V_LIMIT;
        }

        public static boolean isJamoT(int c) {
            int t = c - 4519;
            return t > 0 && t < 28;
        }

        public static boolean isJamo(int c) {
            return JAMO_L_BASE <= c && c <= JAMO_T_END && (c <= JAMO_L_END || ((JAMO_V_BASE <= c && c <= JAMO_V_END) || JAMO_T_BASE < c));
        }

        public static int decompose(int c, Appendable buffer) {
            c -= HANGUL_BASE;
            try {
                int c2 = c % 28;
                c /= 28;
                buffer.append((char) (JAMO_L_BASE + (c / 21)));
                buffer.append((char) (JAMO_V_BASE + (c % 21)));
                if (c2 == 0) {
                    return 2;
                }
                buffer.append((char) (JAMO_T_BASE + c2));
                return 3;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public static void getRawDecomposition(int c, Appendable buffer) {
            int orig = c;
            c -= HANGUL_BASE;
            try {
                int c2 = c % 28;
                if (c2 == 0) {
                    c /= 28;
                    buffer.append((char) (JAMO_L_BASE + (c / 21)));
                    buffer.append((char) (JAMO_V_BASE + (c % 21)));
                    return;
                }
                buffer.append((char) (orig - c2));
                buffer.append((char) (JAMO_T_BASE + c2));
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
    }

    public static final class ReorderingBuffer implements Appendable {
        private final Appendable app;
        private final boolean appIsStringBuilder;
        private int codePointLimit;
        private int codePointStart;
        private final Normalizer2Impl impl;
        private int lastCC;
        private int reorderStart;
        private final StringBuilder str;

        public ReorderingBuffer(Normalizer2Impl ni, Appendable dest, int destCapacity) {
            this.impl = ni;
            this.app = dest;
            if (this.app instanceof StringBuilder) {
                this.appIsStringBuilder = true;
                this.str = (StringBuilder) dest;
                this.str.ensureCapacity(destCapacity);
                this.reorderStart = 0;
                if (this.str.length() == 0) {
                    this.lastCC = 0;
                    return;
                }
                setIterator();
                this.lastCC = previousCC();
                if (this.lastCC > 1) {
                    while (previousCC() > 1) {
                    }
                }
                this.reorderStart = this.codePointLimit;
                return;
            }
            this.appIsStringBuilder = false;
            this.str = new StringBuilder();
            this.reorderStart = 0;
            this.lastCC = 0;
        }

        public boolean isEmpty() {
            return this.str.length() == 0;
        }

        public int length() {
            return this.str.length();
        }

        public int getLastCC() {
            return this.lastCC;
        }

        public StringBuilder getStringBuilder() {
            return this.str;
        }

        public boolean equals(CharSequence s, int start, int limit) {
            return UTF16Plus.equal(this.str, 0, this.str.length(), s, start, limit);
        }

        public void append(int c, int cc) {
            if (this.lastCC <= cc || cc == 0) {
                this.str.appendCodePoint(c);
                this.lastCC = cc;
                if (cc <= 1) {
                    this.reorderStart = this.str.length();
                    return;
                }
                return;
            }
            insert(c, cc);
        }

        public void append(CharSequence s, int start, int limit, int leadCC, int trailCC) {
            if (start != limit) {
                if (this.lastCC <= leadCC || leadCC == 0) {
                    if (trailCC <= 1) {
                        this.reorderStart = this.str.length() + (limit - start);
                    } else if (leadCC <= 1) {
                        this.reorderStart = this.str.length() + 1;
                    }
                    this.str.append(s, start, limit);
                    this.lastCC = trailCC;
                } else {
                    int c = Character.codePointAt(s, start);
                    start += Character.charCount(c);
                    insert(c, leadCC);
                    while (start < limit) {
                        c = Character.codePointAt(s, start);
                        start += Character.charCount(c);
                        if (start < limit) {
                            leadCC = Normalizer2Impl.getCCFromYesOrMaybe(this.impl.getNorm16(c));
                        } else {
                            leadCC = trailCC;
                        }
                        append(c, leadCC);
                    }
                }
            }
        }

        public ReorderingBuffer append(char c) {
            this.str.append(c);
            this.lastCC = 0;
            this.reorderStart = this.str.length();
            return this;
        }

        public void appendZeroCC(int c) {
            this.str.appendCodePoint(c);
            this.lastCC = 0;
            this.reorderStart = this.str.length();
        }

        public ReorderingBuffer append(CharSequence s) {
            if (s.length() != 0) {
                this.str.append(s);
                this.lastCC = 0;
                this.reorderStart = this.str.length();
            }
            return this;
        }

        public ReorderingBuffer append(CharSequence s, int start, int limit) {
            if (start != limit) {
                this.str.append(s, start, limit);
                this.lastCC = 0;
                this.reorderStart = this.str.length();
            }
            return this;
        }

        public void flush() {
            if (this.appIsStringBuilder) {
                this.reorderStart = this.str.length();
            } else {
                try {
                    this.app.append(this.str);
                    this.str.setLength(0);
                    this.reorderStart = 0;
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
            this.lastCC = 0;
        }

        public ReorderingBuffer flushAndAppendZeroCC(CharSequence s, int start, int limit) {
            if (this.appIsStringBuilder) {
                this.str.append(s, start, limit);
                this.reorderStart = this.str.length();
            } else {
                try {
                    this.app.append(this.str).append(s, start, limit);
                    this.str.setLength(0);
                    this.reorderStart = 0;
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
            this.lastCC = 0;
            return this;
        }

        public void remove() {
            this.str.setLength(0);
            this.lastCC = 0;
            this.reorderStart = 0;
        }

        public void removeSuffix(int suffixLength) {
            int oldLength = this.str.length();
            this.str.delete(oldLength - suffixLength, oldLength);
            this.lastCC = 0;
            this.reorderStart = this.str.length();
        }

        private void insert(int c, int cc) {
            setIterator();
            skipPrevious();
            while (previousCC() > cc) {
            }
            if (c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                this.str.insert(this.codePointLimit, (char) c);
                if (cc <= 1) {
                    this.reorderStart = this.codePointLimit + 1;
                    return;
                }
                return;
            }
            this.str.insert(this.codePointLimit, Character.toChars(c));
            if (cc <= 1) {
                this.reorderStart = this.codePointLimit + 2;
            }
        }

        private void setIterator() {
            this.codePointStart = this.str.length();
        }

        private void skipPrevious() {
            this.codePointLimit = this.codePointStart;
            this.codePointStart = this.str.offsetByCodePoints(this.codePointStart, -1);
        }

        private int previousCC() {
            this.codePointLimit = this.codePointStart;
            if (this.reorderStart >= this.codePointStart) {
                return 0;
            }
            int c = this.str.codePointBefore(this.codePointStart);
            this.codePointStart -= Character.charCount(c);
            return this.impl.getCCFromYesOrMaybeCP(c);
        }
    }

    public static final class UTF16Plus {
        public static boolean isSurrogateLead(int c) {
            return (c & 1024) == 0;
        }

        public static boolean equal(CharSequence s1, CharSequence s2) {
            if (s1 == s2) {
                return true;
            }
            int length = s1.length();
            if (length != s2.length()) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (s1.charAt(i) != s2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        public static boolean equal(CharSequence s1, int start1, int limit1, CharSequence s2, int start2, int limit2) {
            if (limit1 - start1 != limit2 - start2) {
                return false;
            }
            if (s1 == s2 && start1 == start2) {
                return true;
            }
            while (start1 < limit1) {
                int start12 = start1 + 1;
                int start22 = start2 + 1;
                if (s1.charAt(start1) != s2.charAt(start2)) {
                    return false;
                }
                start1 = start12;
                start2 = start22;
            }
            return true;
        }
    }

    private static final class IsAcceptable implements Authenticate {
        private IsAcceptable() {
        }

        /* synthetic */ IsAcceptable(AnonymousClass1 x0) {
            this();
        }

        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == (byte) 3;
        }
    }

    public Normalizer2Impl load(ByteBuffer bytes) {
        try {
            this.dataVersion = ICUBinary.readHeaderAndDataVersion(bytes, DATA_FORMAT, IS_ACCEPTABLE);
            int indexesLength = bytes.getInt() / 4;
            if (indexesLength > 18) {
                int i;
                int[] inIndexes = new int[indexesLength];
                inIndexes[0] = indexesLength * 4;
                for (i = 1; i < indexesLength; i++) {
                    inIndexes[i] = bytes.getInt();
                }
                this.minDecompNoCP = inIndexes[8];
                this.minCompNoMaybeCP = inIndexes[9];
                this.minLcccCP = inIndexes[18];
                this.minYesNo = inIndexes[10];
                this.minYesNoMappingsOnly = inIndexes[14];
                this.minNoNo = inIndexes[11];
                this.minNoNoCompBoundaryBefore = inIndexes[15];
                this.minNoNoCompNoMaybeCC = inIndexes[16];
                this.minNoNoEmpty = inIndexes[17];
                this.limitNoNo = inIndexes[12];
                this.minMaybeYes = inIndexes[13];
                this.centerNoNoDelta = ((this.minMaybeYes >> 3) - 64) - 1;
                int offset = inIndexes[0];
                i = inIndexes[1];
                this.normTrie = Trie2_16.createFromSerialized(bytes);
                int trieLength = this.normTrie.getSerializedLength();
                if (trieLength <= i - offset) {
                    ICUBinary.skipBytes(bytes, (i - offset) - trieLength);
                    offset = i;
                    i = inIndexes[2];
                    int numChars = (i - offset) / 2;
                    if (numChars != 0) {
                        this.maybeYesCompositions = ICUBinary.getString(bytes, numChars, 0);
                        this.extraData = this.maybeYesCompositions.substring((MIN_NORMAL_MAYBE_YES - this.minMaybeYes) >> 1);
                    }
                    offset = i;
                    this.smallFCD = new byte[256];
                    bytes.get(this.smallFCD);
                    return this;
                }
                throw new ICUUncheckedIOException("Normalizer2 data: not enough bytes for normTrie");
            }
            throw new ICUUncheckedIOException("Normalizer2 data: not enough indexes");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public Normalizer2Impl load(String name) {
        return load(ICUBinary.getRequiredData(name));
    }

    private void enumLcccRange(int start, int end, int norm16, UnicodeSet set) {
        if (norm16 > MIN_NORMAL_MAYBE_YES && norm16 != JAMO_VT) {
            set.add(start, end);
        } else if (this.minNoNoCompNoMaybeCC <= norm16 && norm16 < this.limitNoNo && getFCD16(start) > 255) {
            set.add(start, end);
        }
    }

    private void enumNorm16PropertyStartsRange(int start, int end, int value, UnicodeSet set) {
        set.add(start);
        if (start != end && isAlgorithmicNoNo(value) && (value & 6) > 2) {
            int prevFCD16 = getFCD16(start);
            while (true) {
                start++;
                if (start <= end) {
                    int fcd16 = getFCD16(start);
                    if (fcd16 != prevFCD16) {
                        set.add(start);
                        prevFCD16 = fcd16;
                    }
                } else {
                    return;
                }
            }
        }
    }

    public void addLcccChars(UnicodeSet set) {
        Iterator<Range> trieIterator = this.normTrie.iterator();
        while (trieIterator.hasNext()) {
            Range range = (Range) trieIterator.next();
            Range range2 = range;
            if (!range.leadSurrogate) {
                enumLcccRange(range2.startCodePoint, range2.endCodePoint, range2.value, set);
            } else {
                return;
            }
        }
    }

    public void addPropertyStarts(UnicodeSet set) {
        Iterator<Range> trieIterator = this.normTrie.iterator();
        while (trieIterator.hasNext()) {
            Range range = (Range) trieIterator.next();
            Range range2 = range;
            if (range.leadSurrogate) {
                break;
            }
            enumNorm16PropertyStartsRange(range2.startCodePoint, range2.endCodePoint, range2.value, set);
        }
        for (int c = Hangul.HANGUL_BASE; c < Hangul.HANGUL_LIMIT; c += 28) {
            set.add(c);
            set.add(c + 1);
        }
        set.add((int) Hangul.HANGUL_LIMIT);
    }

    public void addCanonIterPropertyStarts(UnicodeSet set) {
        ensureCanonIterData();
        Iterator<Range> trieIterator = this.canonIterData.iterator(segmentStarterMapper);
        while (trieIterator.hasNext()) {
            Range range = (Range) trieIterator.next();
            Range range2 = range;
            if (!range.leadSurrogate) {
                set.add(range2.startCodePoint);
            } else {
                return;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:71:0x00df A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00dc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized Normalizer2Impl ensureCanonIterData() {
        synchronized (this) {
            if (this.canonIterData == null) {
                Trie2Writable newData = new Trie2Writable(0, 0);
                this.canonStartSets = new ArrayList();
                Iterator<Range> trieIterator = this.normTrie.iterator();
                while (trieIterator.hasNext()) {
                    Range range = (Range) trieIterator.next();
                    Range range2 = range;
                    if (range.leadSurrogate) {
                        break;
                    }
                    Iterator<Range> trieIterator2;
                    int norm16 = range2.value;
                    if (isInert(norm16)) {
                        trieIterator2 = trieIterator;
                    } else if (this.minYesNo > norm16 || norm16 >= this.minNoNo) {
                        int c = range2.startCodePoint;
                        while (c <= range2.endCodePoint) {
                            int oldValue = newData.get(c);
                            int newValue = oldValue;
                            if (isMaybeOrNonZeroCC(norm16)) {
                                newValue |= Integer.MIN_VALUE;
                                if (norm16 < MIN_NORMAL_MAYBE_YES) {
                                    newValue |= 1073741824;
                                }
                            } else if (norm16 < this.minYesNo) {
                                newValue |= 1073741824;
                            } else {
                                int c2 = c;
                                int norm16_2 = norm16;
                                if (isDecompNoAlgorithmic(norm16_2)) {
                                    c2 = mapAlgorithmic(c2, norm16_2);
                                    norm16_2 = getNorm16(c2);
                                }
                                if (norm16_2 > this.minYesNo) {
                                    int mapping = norm16_2 >> 1;
                                    int firstUnit = this.extraData.charAt(mapping);
                                    int length = firstUnit & 31;
                                    if (!((firstUnit & 128) == 0 || c != c2 || (this.extraData.charAt(mapping - 1) & 255) == 0)) {
                                        newValue |= Integer.MIN_VALUE;
                                    }
                                    if (length != 0) {
                                        mapping++;
                                        int limit = mapping + length;
                                        c2 = this.extraData.codePointAt(mapping);
                                        addToStartSet(newData, c, c2);
                                        if (norm16_2 >= this.minNoNo) {
                                            while (true) {
                                                int charCount = Character.charCount(c2) + mapping;
                                                mapping = charCount;
                                                if (charCount >= limit) {
                                                    break;
                                                }
                                                c2 = this.extraData.codePointAt(mapping);
                                                charCount = newData.get(c2);
                                                if ((charCount & Integer.MIN_VALUE) == 0) {
                                                    trieIterator2 = trieIterator;
                                                    newData.set(c2, charCount | -2147483648);
                                                } else {
                                                    trieIterator2 = trieIterator;
                                                }
                                                trieIterator = trieIterator2;
                                            }
                                        }
                                    }
                                    trieIterator2 = trieIterator;
                                } else {
                                    trieIterator2 = trieIterator;
                                    addToStartSet(newData, c, c2);
                                }
                                if (newValue == oldValue) {
                                    newData.set(c, newValue);
                                }
                                c++;
                                trieIterator = trieIterator2;
                            }
                            trieIterator2 = trieIterator;
                            if (newValue == oldValue) {
                            }
                            c++;
                            trieIterator = trieIterator2;
                        }
                        trieIterator2 = trieIterator;
                    } else {
                        trieIterator2 = trieIterator;
                    }
                    trieIterator = trieIterator2;
                }
                this.canonIterData = newData.toTrie2_32();
            }
        }
        return this;
    }

    public int getNorm16(int c) {
        return this.normTrie.get(c);
    }

    public int getCompQuickCheck(int norm16) {
        if (norm16 < this.minNoNo || MIN_YES_YES_WITH_CC <= norm16) {
            return 1;
        }
        if (this.minMaybeYes <= norm16) {
            return 2;
        }
        return 0;
    }

    public boolean isAlgorithmicNoNo(int norm16) {
        return this.limitNoNo <= norm16 && norm16 < this.minMaybeYes;
    }

    public boolean isCompNo(int norm16) {
        return this.minNoNo <= norm16 && norm16 < this.minMaybeYes;
    }

    public boolean isDecompYes(int norm16) {
        return norm16 < this.minYesNo || this.minMaybeYes <= norm16;
    }

    public int getCC(int norm16) {
        if (norm16 >= MIN_NORMAL_MAYBE_YES) {
            return getCCFromNormalYesOrMaybe(norm16);
        }
        if (norm16 < this.minNoNo || this.limitNoNo <= norm16) {
            return 0;
        }
        return getCCFromNoNo(norm16);
    }

    public static int getCCFromNormalYesOrMaybe(int norm16) {
        return (norm16 >> 1) & 255;
    }

    public static int getCCFromYesOrMaybe(int norm16) {
        return norm16 >= MIN_NORMAL_MAYBE_YES ? getCCFromNormalYesOrMaybe(norm16) : 0;
    }

    public int getCCFromYesOrMaybeCP(int c) {
        if (c < this.minCompNoMaybeCP) {
            return 0;
        }
        return getCCFromYesOrMaybe(getNorm16(c));
    }

    public int getFCD16(int c) {
        if (c < this.minDecompNoCP) {
            return 0;
        }
        if (c > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH || singleLeadMightHaveNonZeroFCD16(c)) {
            return getFCD16FromNormData(c);
        }
        return 0;
    }

    public boolean singleLeadMightHaveNonZeroFCD16(int lead) {
        byte bits = this.smallFCD[lead >> 8];
        boolean z = false;
        if (bits == (byte) 0) {
            return false;
        }
        if (((bits >> ((lead >> 5) & 7)) & 1) != 0) {
            z = true;
        }
        return z;
    }

    public int getFCD16FromNormData(int c) {
        int deltaTrailCC;
        int norm16 = getNorm16(c);
        if (norm16 >= this.limitNoNo) {
            if (norm16 >= MIN_NORMAL_MAYBE_YES) {
                norm16 = getCCFromNormalYesOrMaybe(norm16);
                return (norm16 << 8) | norm16;
            } else if (norm16 >= this.minMaybeYes) {
                return 0;
            } else {
                deltaTrailCC = norm16 & 6;
                if (deltaTrailCC <= 2) {
                    return deltaTrailCC >> 1;
                }
                norm16 = getNorm16(mapAlgorithmic(c, norm16));
            }
        }
        if (norm16 <= this.minYesNo || isHangulLVT(norm16)) {
            return 0;
        }
        deltaTrailCC = norm16 >> 1;
        int firstUnit = this.extraData.charAt(deltaTrailCC);
        int fcd16 = firstUnit >> 8;
        if ((firstUnit & 128) != 0) {
            fcd16 |= this.extraData.charAt(deltaTrailCC - 1) & 65280;
        }
        return fcd16;
    }

    public String getDecomposition(int c) {
        if (c >= this.minDecompNoCP) {
            int norm16 = getNorm16(c);
            int norm162 = norm16;
            if (!isMaybeOrNonZeroCC(norm16)) {
                norm16 = -1;
                if (isDecompNoAlgorithmic(norm162)) {
                    int mapAlgorithmic = mapAlgorithmic(c, norm162);
                    c = mapAlgorithmic;
                    norm16 = mapAlgorithmic;
                    norm162 = getNorm16(c);
                }
                if (norm162 < this.minYesNo) {
                    if (norm16 < 0) {
                        return null;
                    }
                    return UTF16.valueOf(norm16);
                } else if (isHangulLV(norm162) || isHangulLVT(norm162)) {
                    StringBuilder buffer = new StringBuilder();
                    Hangul.decompose(c, buffer);
                    return buffer.toString();
                } else {
                    int mapping = norm162 >> 1;
                    int mapping2 = mapping + 1;
                    return this.extraData.substring(mapping2, mapping2 + (this.extraData.charAt(mapping) & 31));
                }
            }
        }
        return null;
    }

    public String getRawDecomposition(int c) {
        if (c >= this.minDecompNoCP) {
            int norm16 = getNorm16(c);
            int norm162 = norm16;
            if (!isDecompYes(norm16)) {
                if (isHangulLV(norm162) || isHangulLVT(norm162)) {
                    StringBuilder buffer = new StringBuilder();
                    Hangul.getRawDecomposition(c, buffer);
                    return buffer.toString();
                } else if (isDecompNoAlgorithmic(norm162)) {
                    return UTF16.valueOf(mapAlgorithmic(c, norm162));
                } else {
                    norm16 = norm162 >> 1;
                    int firstUnit = this.extraData.charAt(norm16);
                    int mLength = firstUnit & 31;
                    if ((firstUnit & 64) != 0) {
                        int rawMapping = (norm16 - ((firstUnit >> 7) & 1)) - 1;
                        char rm0 = this.extraData.charAt(rawMapping);
                        if (rm0 <= 31) {
                            return this.extraData.substring(rawMapping - rm0, rawMapping);
                        }
                        StringBuilder buffer2 = new StringBuilder(mLength - 1).append(rm0);
                        norm16 += 3;
                        buffer2.append(this.extraData, norm16, (norm16 + mLength) - 2);
                        return buffer2.toString();
                    }
                    norm16++;
                    return this.extraData.substring(norm16, norm16 + mLength);
                }
            }
        }
        return null;
    }

    public boolean isCanonSegmentStarter(int c) {
        return this.canonIterData.get(c) >= 0;
    }

    public boolean getCanonStartSet(int c, UnicodeSet set) {
        int canonValue = this.canonIterData.get(c) & Integer.MAX_VALUE;
        if (canonValue == 0) {
            return false;
        }
        set.clear();
        int value = 2097151 & canonValue;
        if ((2097152 & canonValue) != 0) {
            set.addAll((UnicodeSet) this.canonStartSets.get(value));
        } else if (value != 0) {
            set.add(value);
        }
        if ((1073741824 & canonValue) != 0) {
            int norm16 = getNorm16(c);
            if (norm16 == 2) {
                int syllable = Hangul.HANGUL_BASE + ((c - 4352) * Hangul.JAMO_VT_COUNT);
                set.add(syllable, (syllable + Hangul.JAMO_VT_COUNT) - 1);
            } else {
                addComposites(getCompositionsList(norm16), set);
            }
        }
        return true;
    }

    public Appendable decompose(CharSequence s, StringBuilder dest) {
        decompose(s, 0, s.length(), dest, s.length());
        return dest;
    }

    public void decompose(CharSequence s, int src, int limit, StringBuilder dest, int destLengthEstimate) {
        if (destLengthEstimate < 0) {
            destLengthEstimate = limit - src;
        }
        dest.setLength(0);
        decompose(s, src, limit, new ReorderingBuffer(this, dest, destLengthEstimate));
    }

    public int decompose(CharSequence s, int prevSrc, int limit, ReorderingBuffer buffer) {
        char minNoCP = this.minDecompNoCP;
        int c = 0;
        int norm16 = 0;
        int prevBoundary = prevSrc;
        int prevCC = 0;
        while (true) {
            int fromU16SingleLead;
            int norm162 = norm16;
            norm16 = c;
            c = prevSrc;
            while (c != limit) {
                char charAt = s.charAt(c);
                norm16 = charAt;
                if (charAt >= minNoCP) {
                    fromU16SingleLead = this.normTrie.getFromU16SingleLead((char) norm16);
                    norm162 = fromU16SingleLead;
                    if (!isMostDecompYesAndZeroCC(fromU16SingleLead)) {
                        if (!UTF16.isSurrogate((char) norm16)) {
                            break;
                        }
                        char c2;
                        if (UTF16Plus.isSurrogateLead(norm16)) {
                            if (c + 1 != limit) {
                                charAt = s.charAt(c + 1);
                                c2 = charAt;
                                if (Character.isLowSurrogate(charAt)) {
                                    norm16 = Character.toCodePoint((char) norm16, c2);
                                }
                            }
                        } else if (prevSrc < c) {
                            charAt = s.charAt(c - 1);
                            c2 = charAt;
                            if (Character.isHighSurrogate(charAt)) {
                                c--;
                                norm16 = Character.toCodePoint(c2, (char) norm16);
                            }
                        }
                        fromU16SingleLead = getNorm16(norm16);
                        norm162 = fromU16SingleLead;
                        if (!isMostDecompYesAndZeroCC(fromU16SingleLead)) {
                            break;
                        }
                        c += Character.charCount(norm16);
                    }
                }
                c++;
            }
            if (c != prevSrc) {
                if (buffer != null) {
                    buffer.flushAndAppendZeroCC(s, prevSrc, c);
                } else {
                    prevCC = 0;
                    prevBoundary = c;
                }
            }
            if (c == limit) {
                return c;
            }
            c += Character.charCount(norm16);
            if (buffer == null) {
                if (!isDecompYes(norm162)) {
                    break;
                }
                fromU16SingleLead = getCCFromYesOrMaybe(norm162);
                if (prevCC > fromU16SingleLead && fromU16SingleLead != 0) {
                    break;
                }
                prevCC = fromU16SingleLead;
                if (fromU16SingleLead <= 1) {
                    prevBoundary = c;
                }
            } else {
                decompose(norm16, norm162, buffer);
            }
            prevSrc = c;
            c = norm16;
            norm16 = norm162;
        }
        return prevBoundary;
    }

    public void decomposeAndAppend(CharSequence s, boolean doDecompose, ReorderingBuffer buffer) {
        int limit = s.length();
        if (limit != 0) {
            if (doDecompose) {
                decompose(s, 0, limit, buffer);
                return;
            }
            int c = Character.codePointAt(s, 0);
            int src = 0;
            int cc = getCC(getNorm16(c));
            int cc2 = cc;
            int prevCC = cc;
            int firstCC = cc;
            while (cc2 != 0) {
                prevCC = cc2;
                src += Character.charCount(c);
                if (src >= limit) {
                    break;
                }
                c = Character.codePointAt(s, src);
                cc2 = getCC(getNorm16(c));
            }
            buffer.append(s, 0, src, firstCC, prevCC);
            buffer.append(s, src, limit);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:124:0x01d1  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0202  */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x01d1  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0202  */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x01d1  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0202  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0129  */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x01d1  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0202  */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x01d1  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0202  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean compose(CharSequence s, int src, int limit, boolean onlyContiguous, boolean doCompose, ReorderingBuffer buffer) {
        CharSequence charSequence = s;
        int i = limit;
        boolean z = onlyContiguous;
        ReorderingBuffer reorderingBuffer = buffer;
        int src2 = src;
        char minNoMaybeCP = this.minCompNoMaybeCP;
        int prevBoundary = src2;
        while (true) {
            boolean z2;
            int fromU16SingleLead;
            char c2;
            int norm16;
            char c = minNoMaybeCP;
            int i2 = 0;
            int c3 = 0;
            int prevSrc = src2;
            src2 = 0;
            while (true) {
                z2 = true;
                if (prevSrc == i) {
                    if (prevBoundary != i && doCompose) {
                        reorderingBuffer.append(charSequence, prevBoundary, i);
                    }
                    return true;
                }
                char charAt = charSequence.charAt(prevSrc);
                c3 = charAt;
                if (charAt >= c) {
                    fromU16SingleLead = this.normTrie.getFromU16SingleLead((char) c3);
                    src2 = fromU16SingleLead;
                    if (!isCompYesAndZeroCC(fromU16SingleLead)) {
                        fromU16SingleLead = prevSrc + 1;
                        if (!UTF16.isSurrogate((char) c3)) {
                            break;
                        }
                        char charAt2;
                        if (UTF16Plus.isSurrogateLead(c3)) {
                            if (fromU16SingleLead != i) {
                                charAt2 = charSequence.charAt(fromU16SingleLead);
                                c2 = charAt2;
                                if (Character.isLowSurrogate(charAt2)) {
                                    fromU16SingleLead++;
                                    c3 = Character.toCodePoint((char) c3, c2);
                                }
                            }
                        } else if (prevBoundary < prevSrc) {
                            charAt2 = charSequence.charAt(prevSrc - 1);
                            c2 = charAt2;
                            if (Character.isHighSurrogate(charAt2)) {
                                prevSrc--;
                                c3 = Character.toCodePoint(c2, (char) c3);
                            }
                        }
                        norm16 = getNorm16(c3);
                        src2 = norm16;
                        if (!isCompYesAndZeroCC(norm16)) {
                            break;
                        }
                        prevSrc = fromU16SingleLead;
                    }
                }
                prevSrc++;
                i2 = i2;
                prevBoundary = prevBoundary;
            }
            int prevSrc2;
            CharSequence charSequence2;
            int recomposeStartIndex;
            boolean z3;
            ReorderingBuffer reorderingBuffer2;
            if (isMaybeOrNonZeroCC(src2)) {
                if (!isJamoVT(src2) || prevBoundary == prevSrc) {
                    if (src2 > JAMO_VT) {
                        norm16 = getCCFromNormalYesOrMaybe(src2);
                        if (!z || getPreviousTrailCC(charSequence, prevBoundary, prevSrc) <= norm16) {
                            while (fromU16SingleLead != i) {
                                prevSrc2 = norm16;
                                c3 = Character.codePointAt(charSequence, fromU16SingleLead);
                                i2 = this.normTrie.get(c3);
                                if (i2 >= MIN_YES_YES_WITH_CC) {
                                    norm16 = getCCFromNormalYesOrMaybe(i2);
                                    if (prevSrc2 <= norm16) {
                                        fromU16SingleLead += Character.charCount(c3);
                                        z2 = true;
                                    } else if (!doCompose) {
                                        return false;
                                    }
                                }
                                if (norm16HasCompBoundaryBefore(i2)) {
                                    if (isCompYesAndZeroCC(i2)) {
                                        src2 = Character.charCount(c3) + fromU16SingleLead;
                                        minNoMaybeCP = c;
                                    }
                                }
                            }
                            if (doCompose) {
                                reorderingBuffer.append(charSequence, prevBoundary, i);
                            }
                            return z2;
                        } else if (!doCompose) {
                            return false;
                        }
                    }
                    i2 = fromU16SingleLead;
                    c3 = Character.codePointBefore(charSequence, prevSrc);
                    src2 = this.normTrie.get(c3);
                    if (!norm16HasCompBoundaryAfter(src2, z)) {
                    }
                    prevSrc2 = prevSrc;
                    reorderingBuffer.append(charSequence, prevBoundary, prevSrc2);
                    charSequence2 = charSequence;
                    recomposeStartIndex = buffer.length();
                    z3 = z;
                    prevBoundary = prevSrc2;
                    reorderingBuffer2 = reorderingBuffer;
                    decomposeShort(charSequence2, prevSrc2, i2, false, z3, reorderingBuffer2);
                    src2 = decomposeShort(charSequence2, i2, i, true, z3, reorderingBuffer2);
                    recompose(reorderingBuffer, recomposeStartIndex, z);
                    if (!doCompose) {
                    }
                    prevBoundary = src2;
                    minNoMaybeCP = c;
                } else {
                    char prev = charSequence.charAt(prevSrc - 1);
                    if (c3 < Hangul.JAMO_T_BASE) {
                        c2 = (char) (prev - 4352);
                        if (c2 < 19) {
                            if (!doCompose) {
                                return i2;
                            }
                            if (fromU16SingleLead != i) {
                                i2 = charSequence.charAt(fromU16SingleLead) - Hangul.JAMO_T_BASE;
                                norm16 = i2;
                                if (i2 > 0 && norm16 < 28) {
                                    fromU16SingleLead++;
                                    if (norm16 >= 0) {
                                        i2 = (Hangul.HANGUL_BASE + (((c2 * 21) + (c3 - 4449)) * 28)) + norm16;
                                        prevSrc--;
                                        if (prevBoundary != prevSrc) {
                                            reorderingBuffer.append(charSequence, prevBoundary, prevSrc);
                                        }
                                        reorderingBuffer.append((char) i2);
                                        prevBoundary = fromU16SingleLead;
                                    }
                                }
                            }
                            if (hasCompBoundaryBefore(charSequence, fromU16SingleLead, i)) {
                                norm16 = 0;
                            } else {
                                norm16 = -1;
                            }
                            if (norm16 >= 0) {
                            }
                        }
                        i2 = fromU16SingleLead;
                        c3 = Character.codePointBefore(charSequence, prevSrc);
                        src2 = this.normTrie.get(c3);
                        if (norm16HasCompBoundaryAfter(src2, z)) {
                        }
                        prevSrc2 = prevSrc;
                        reorderingBuffer.append(charSequence, prevBoundary, prevSrc2);
                        charSequence2 = charSequence;
                        recomposeStartIndex = buffer.length();
                        z3 = z;
                        prevBoundary = prevSrc2;
                        reorderingBuffer2 = reorderingBuffer;
                        decomposeShort(charSequence2, prevSrc2, i2, false, z3, reorderingBuffer2);
                        src2 = decomposeShort(charSequence2, i2, i, true, z3, reorderingBuffer2);
                        recompose(reorderingBuffer, recomposeStartIndex, z);
                        if (doCompose) {
                        }
                        prevBoundary = src2;
                        minNoMaybeCP = c;
                    } else {
                        if (Hangul.isHangulLV(prev)) {
                            if (!doCompose) {
                                return false;
                            }
                            norm16 = (prev + c3) - Hangul.JAMO_T_BASE;
                            prevSrc--;
                            if (prevBoundary != prevSrc) {
                                reorderingBuffer.append(charSequence, prevBoundary, prevSrc);
                            }
                            reorderingBuffer.append((char) norm16);
                            prevBoundary = fromU16SingleLead;
                        }
                        i2 = fromU16SingleLead;
                        c3 = Character.codePointBefore(charSequence, prevSrc);
                        src2 = this.normTrie.get(c3);
                        if (norm16HasCompBoundaryAfter(src2, z)) {
                        }
                        prevSrc2 = prevSrc;
                        reorderingBuffer.append(charSequence, prevBoundary, prevSrc2);
                        charSequence2 = charSequence;
                        recomposeStartIndex = buffer.length();
                        z3 = z;
                        prevBoundary = prevSrc2;
                        reorderingBuffer2 = reorderingBuffer;
                        decomposeShort(charSequence2, prevSrc2, i2, false, z3, reorderingBuffer2);
                        src2 = decomposeShort(charSequence2, i2, i, true, z3, reorderingBuffer2);
                        recompose(reorderingBuffer, recomposeStartIndex, z);
                        if (doCompose) {
                        }
                        prevBoundary = src2;
                        minNoMaybeCP = c;
                    }
                }
            } else if (!doCompose) {
                return i2;
            } else {
                if (isDecompNoAlgorithmic(src2)) {
                    if (norm16HasCompBoundaryAfter(src2, z) || hasCompBoundaryBefore(charSequence, fromU16SingleLead, i)) {
                        if (prevBoundary != prevSrc) {
                            reorderingBuffer.append(charSequence, prevBoundary, prevSrc);
                        }
                        reorderingBuffer.append(mapAlgorithmic(c3, src2), i2);
                        prevBoundary = fromU16SingleLead;
                    }
                    i2 = fromU16SingleLead;
                    if (!(prevBoundary == prevSrc || norm16HasCompBoundaryBefore(src2))) {
                        c3 = Character.codePointBefore(charSequence, prevSrc);
                        src2 = this.normTrie.get(c3);
                        if (norm16HasCompBoundaryAfter(src2, z)) {
                            prevSrc -= Character.charCount(c3);
                        }
                    }
                    prevSrc2 = prevSrc;
                    if (doCompose && prevBoundary != prevSrc2) {
                        reorderingBuffer.append(charSequence, prevBoundary, prevSrc2);
                    }
                    charSequence2 = charSequence;
                    recomposeStartIndex = buffer.length();
                    z3 = z;
                    prevBoundary = prevSrc2;
                    reorderingBuffer2 = reorderingBuffer;
                    decomposeShort(charSequence2, prevSrc2, i2, false, z3, reorderingBuffer2);
                    src2 = decomposeShort(charSequence2, i2, i, true, z3, reorderingBuffer2);
                    recompose(reorderingBuffer, recomposeStartIndex, z);
                    if (doCompose) {
                        if (!reorderingBuffer.equals(charSequence, prevBoundary, src2)) {
                            return false;
                        }
                        buffer.remove();
                    }
                    prevBoundary = src2;
                    minNoMaybeCP = c;
                } else if (src2 < this.minNoNoCompBoundaryBefore) {
                    if (norm16HasCompBoundaryAfter(src2, z) || hasCompBoundaryBefore(charSequence, fromU16SingleLead, i)) {
                        if (prevBoundary != prevSrc) {
                            reorderingBuffer.append(charSequence, prevBoundary, prevSrc);
                        }
                        int mapping = src2 >> 1;
                        prevSrc2 = mapping + 1;
                        reorderingBuffer.append(this.extraData, prevSrc2, prevSrc2 + (this.extraData.charAt(mapping) & 31));
                        prevBoundary = fromU16SingleLead;
                    }
                    i2 = fromU16SingleLead;
                    c3 = Character.codePointBefore(charSequence, prevSrc);
                    src2 = this.normTrie.get(c3);
                    if (norm16HasCompBoundaryAfter(src2, z)) {
                    }
                    prevSrc2 = prevSrc;
                    reorderingBuffer.append(charSequence, prevBoundary, prevSrc2);
                    charSequence2 = charSequence;
                    recomposeStartIndex = buffer.length();
                    z3 = z;
                    prevBoundary = prevSrc2;
                    reorderingBuffer2 = reorderingBuffer;
                    decomposeShort(charSequence2, prevSrc2, i2, false, z3, reorderingBuffer2);
                    src2 = decomposeShort(charSequence2, i2, i, true, z3, reorderingBuffer2);
                    recompose(reorderingBuffer, recomposeStartIndex, z);
                    if (doCompose) {
                    }
                    prevBoundary = src2;
                    minNoMaybeCP = c;
                } else {
                    if (src2 >= this.minNoNoEmpty && (hasCompBoundaryBefore(charSequence, fromU16SingleLead, i) || hasCompBoundaryAfter(charSequence, prevBoundary, prevSrc, z))) {
                        if (prevBoundary != prevSrc) {
                            reorderingBuffer.append(charSequence, prevBoundary, prevSrc);
                        }
                        prevBoundary = fromU16SingleLead;
                    }
                    i2 = fromU16SingleLead;
                    c3 = Character.codePointBefore(charSequence, prevSrc);
                    src2 = this.normTrie.get(c3);
                    if (norm16HasCompBoundaryAfter(src2, z)) {
                    }
                    prevSrc2 = prevSrc;
                    reorderingBuffer.append(charSequence, prevBoundary, prevSrc2);
                    charSequence2 = charSequence;
                    recomposeStartIndex = buffer.length();
                    z3 = z;
                    prevBoundary = prevSrc2;
                    reorderingBuffer2 = reorderingBuffer;
                    decomposeShort(charSequence2, prevSrc2, i2, false, z3, reorderingBuffer2);
                    src2 = decomposeShort(charSequence2, i2, i, true, z3, reorderingBuffer2);
                    recompose(reorderingBuffer, recomposeStartIndex, z);
                    if (doCompose) {
                    }
                    prevBoundary = src2;
                    minNoMaybeCP = c;
                }
            }
            src2 = fromU16SingleLead;
            minNoMaybeCP = c;
        }
    }

    public int composeQuickCheck(CharSequence s, int prevSrc, int limit, boolean onlyContiguous, boolean doSpan) {
        int qcResult = 0;
        int prevBoundary = prevSrc;
        char minNoMaybeCP = this.minCompNoMaybeCP;
        while (true) {
            while (prevSrc != limit) {
                char charAt = s.charAt(prevSrc);
                int c = charAt;
                if (charAt >= minNoMaybeCP) {
                    int fromU16SingleLead = this.normTrie.getFromU16SingleLead((char) c);
                    int norm16 = fromU16SingleLead;
                    if (!isCompYesAndZeroCC(fromU16SingleLead)) {
                        int norm162;
                        int n16;
                        fromU16SingleLead = prevSrc + 1;
                        if (UTF16.isSurrogate((char) c)) {
                            char charAt2;
                            char c2;
                            if (UTF16Plus.isSurrogateLead(c)) {
                                if (fromU16SingleLead != limit) {
                                    charAt2 = s.charAt(fromU16SingleLead);
                                    c2 = charAt2;
                                    if (Character.isLowSurrogate(charAt2)) {
                                        fromU16SingleLead++;
                                        c = Character.toCodePoint((char) c, c2);
                                    }
                                }
                            } else if (prevBoundary < prevSrc) {
                                charAt2 = s.charAt(prevSrc - 1);
                                c2 = charAt2;
                                if (Character.isHighSurrogate(charAt2)) {
                                    prevSrc--;
                                    c = Character.toCodePoint(c2, (char) c);
                                }
                            }
                            norm162 = getNorm16(c);
                            norm16 = norm162;
                            if (isCompYesAndZeroCC(norm162)) {
                                prevSrc = fromU16SingleLead;
                            }
                        }
                        norm162 = 1;
                        if (prevBoundary != prevSrc) {
                            prevBoundary = prevSrc;
                            if (!norm16HasCompBoundaryBefore(norm16)) {
                                c = Character.codePointBefore(s, prevSrc);
                                n16 = getNorm16(c);
                                if (!norm16HasCompBoundaryAfter(n16, onlyContiguous)) {
                                    prevBoundary -= Character.charCount(c);
                                    norm162 = n16;
                                }
                            }
                        }
                        if (!isMaybeOrNonZeroCC(norm16)) {
                            break;
                        }
                        n16 = getCCFromYesOrMaybe(norm16);
                        if (onlyContiguous && n16 != 0 && getTrailCCFromCompYesAndZeroCC(prevNorm16) > n16) {
                            break;
                        }
                        while (true) {
                            if (norm16 < MIN_YES_YES_WITH_CC) {
                                if (doSpan) {
                                    return prevBoundary << 1;
                                }
                                qcResult = 1;
                            }
                            if (fromU16SingleLead != limit) {
                                int prevCC = n16;
                                c = Character.codePointAt(s, fromU16SingleLead);
                                norm16 = getNorm16(c);
                                if (!isMaybeOrNonZeroCC(norm16)) {
                                    break;
                                }
                                n16 = getCCFromYesOrMaybe(norm16);
                                if (prevCC > n16 && n16 != 0) {
                                    break;
                                }
                                fromU16SingleLead += Character.charCount(c);
                            } else {
                                return (fromU16SingleLead << 1) | qcResult;
                            }
                        }
                        if (!isCompYesAndZeroCC(norm16)) {
                            break;
                        }
                        prevBoundary = fromU16SingleLead;
                        prevSrc = fromU16SingleLead + Character.charCount(c);
                    }
                }
                prevSrc++;
            }
            return (prevSrc << 1) | qcResult;
        }
        return prevBoundary << 1;
    }

    public void composeAndAppend(CharSequence s, boolean doCompose, boolean onlyContiguous, ReorderingBuffer buffer) {
        CharSequence charSequence = s;
        boolean z = onlyContiguous;
        ReorderingBuffer reorderingBuffer = buffer;
        int src = 0;
        int limit = s.length();
        if (!buffer.isEmpty()) {
            int firstStarterInSrc = findNextCompBoundary(charSequence, 0, limit, z);
            if (firstStarterInSrc != 0) {
                int lastStarterInDest = findPreviousCompBoundary(buffer.getStringBuilder(), buffer.length(), z);
                StringBuilder middle = new StringBuilder(((buffer.length() - lastStarterInDest) + firstStarterInSrc) + 16);
                middle.append(buffer.getStringBuilder(), lastStarterInDest, buffer.length());
                reorderingBuffer.removeSuffix(buffer.length() - lastStarterInDest);
                middle.append(charSequence, 0, firstStarterInSrc);
                compose(middle, 0, middle.length(), z, true, reorderingBuffer);
                src = firstStarterInSrc;
            }
        }
        if (doCompose) {
            compose(charSequence, src, limit, z, true, reorderingBuffer);
        } else {
            reorderingBuffer.append(charSequence, src, limit);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:60:0x00f5  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00e3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int makeFCD(CharSequence s, int src, int limit, ReorderingBuffer buffer) {
        CharSequence charSequence = s;
        int i = limit;
        ReorderingBuffer reorderingBuffer = buffer;
        int prevSrc = src;
        int fcd16 = 0;
        int prevFCD16 = 0;
        int c = 0;
        int prevBoundary = prevSrc;
        while (true) {
            int fcd162 = fcd16;
            fcd16 = c;
            c = prevSrc;
            while (c != i) {
                char charAt = charSequence.charAt(c);
                fcd16 = charAt;
                if (charAt >= this.minLcccCP) {
                    if (singleLeadMightHaveNonZeroFCD16(fcd16)) {
                        if (UTF16.isSurrogate((char) fcd16)) {
                            char c2;
                            if (UTF16Plus.isSurrogateLead(fcd16)) {
                                if (c + 1 != i) {
                                    charAt = charSequence.charAt(c + 1);
                                    c2 = charAt;
                                    if (Character.isLowSurrogate(charAt)) {
                                        fcd16 = Character.toCodePoint((char) fcd16, c2);
                                    }
                                }
                            } else if (prevSrc < c) {
                                charAt = charSequence.charAt(c - 1);
                                c2 = charAt;
                                if (Character.isHighSurrogate(charAt)) {
                                    c--;
                                    fcd16 = Character.toCodePoint(c2, (char) fcd16);
                                }
                            }
                        }
                        int fCD16FromNormData = getFCD16FromNormData(fcd16);
                        fcd162 = fCD16FromNormData;
                        if (fCD16FromNormData > 255) {
                            break;
                        }
                        prevFCD16 = fcd162;
                        c += Character.charCount(fcd16);
                    } else {
                        prevFCD16 = 0;
                        c++;
                    }
                } else {
                    prevFCD16 = ~fcd16;
                    c++;
                }
            }
            int c3 = fcd16;
            int fcd163 = fcd162;
            int prevSrc2;
            int prevBoundary2;
            if (c == prevSrc) {
                if (c == i) {
                    break;
                }
                prevSrc2 = prevSrc;
                prevBoundary2 = prevBoundary;
                prevSrc = Character.charCount(c3) + c;
                if ((prevFCD16 & 255) > (fcd163 >> 8)) {
                }
            } else if (c != i) {
                prevBoundary = c;
                if (prevFCD16 < 0) {
                    fcd162 = ~prevFCD16;
                    if (fcd162 < this.minDecompNoCP) {
                        prevFCD16 = 0;
                    } else {
                        prevFCD16 = getFCD16FromNormData(fcd162);
                        if (prevFCD16 > 1) {
                            prevBoundary--;
                        }
                    }
                } else {
                    fcd162 = c - 1;
                    if (Character.isLowSurrogate(charSequence.charAt(fcd162)) && prevSrc < fcd162 && Character.isHighSurrogate(charSequence.charAt(fcd162 - 1))) {
                        fcd162--;
                        prevFCD16 = getFCD16FromNormData(Character.toCodePoint(charSequence.charAt(fcd162), charSequence.charAt(fcd162 + 1)));
                    }
                    if (prevFCD16 > 1) {
                        prevBoundary = fcd162;
                    }
                }
                if (reorderingBuffer != null) {
                    reorderingBuffer.flushAndAppendZeroCC(charSequence, prevSrc, prevBoundary);
                    reorderingBuffer.append(charSequence, prevBoundary, c);
                }
                prevSrc = c;
                prevSrc2 = prevSrc;
                prevBoundary2 = prevBoundary;
                prevSrc = Character.charCount(c3) + c;
                if ((prevFCD16 & 255) > (fcd163 >> 8)) {
                    if ((fcd163 & 255) <= 1) {
                        prevBoundary = prevSrc;
                    } else {
                        prevBoundary = prevBoundary2;
                    }
                    if (reorderingBuffer != null) {
                        reorderingBuffer.appendZeroCC(c3);
                    }
                    prevFCD16 = fcd163;
                    c = c3;
                    fcd16 = fcd163;
                } else if (reorderingBuffer == null) {
                    return prevBoundary2;
                } else {
                    reorderingBuffer.removeSuffix(prevSrc2 - prevBoundary2);
                    int src2 = findNextFCDBoundary(charSequence, prevSrc, i);
                    decomposeShort(charSequence, prevBoundary2, src2, false, false, reorderingBuffer);
                    prevBoundary = src2;
                    prevFCD16 = 0;
                    c = c3;
                    fcd16 = fcd163;
                    prevSrc = src2;
                }
            } else if (reorderingBuffer != null) {
                reorderingBuffer.flushAndAppendZeroCC(charSequence, prevSrc, c);
            }
        }
        return c;
    }

    public void makeFCDAndAppend(CharSequence s, boolean doMakeFCD, ReorderingBuffer buffer) {
        int src = 0;
        int limit = s.length();
        if (!buffer.isEmpty()) {
            int firstBoundaryInSrc = findNextFCDBoundary(s, 0, limit);
            if (firstBoundaryInSrc != 0) {
                int lastBoundaryInDest = findPreviousFCDBoundary(buffer.getStringBuilder(), buffer.length());
                StringBuilder middle = new StringBuilder(((buffer.length() - lastBoundaryInDest) + firstBoundaryInSrc) + 16);
                middle.append(buffer.getStringBuilder(), lastBoundaryInDest, buffer.length());
                buffer.removeSuffix(buffer.length() - lastBoundaryInDest);
                middle.append(s, 0, firstBoundaryInSrc);
                makeFCD(middle, 0, middle.length(), buffer);
                src = firstBoundaryInSrc;
            }
        }
        if (doMakeFCD) {
            makeFCD(s, src, limit, buffer);
        } else {
            buffer.append(s, src, limit);
        }
    }

    public boolean hasDecompBoundaryBefore(int c) {
        return c < this.minLcccCP || ((c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH && !singleLeadMightHaveNonZeroFCD16(c)) || norm16HasDecompBoundaryBefore(getNorm16(c)));
    }

    public boolean norm16HasDecompBoundaryBefore(int norm16) {
        boolean z = true;
        if (norm16 < this.minNoNoCompNoMaybeCC) {
            return true;
        }
        if (norm16 >= this.limitNoNo) {
            if (norm16 > MIN_NORMAL_MAYBE_YES && norm16 != JAMO_VT) {
                z = false;
            }
            return z;
        }
        int mapping = norm16 >> 1;
        if (!((this.extraData.charAt(mapping) & 128) == 0 || (this.extraData.charAt(mapping - 1) & 65280) == 0)) {
            z = false;
        }
        return z;
    }

    public boolean hasDecompBoundaryAfter(int c) {
        if (c < this.minDecompNoCP) {
            return true;
        }
        if (c > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH || singleLeadMightHaveNonZeroFCD16(c)) {
            return norm16HasDecompBoundaryAfter(getNorm16(c));
        }
        return true;
    }

    public boolean norm16HasDecompBoundaryAfter(int norm16) {
        boolean z = true;
        if (norm16 <= this.minYesNo || isHangulLVT(norm16)) {
            return true;
        }
        if (norm16 < this.limitNoNo) {
            int mapping = norm16 >> 1;
            int firstUnit = this.extraData.charAt(mapping);
            if (firstUnit > Opcodes.OP_CHECK_CAST_JUMBO) {
                return false;
            }
            if (firstUnit <= 255) {
                return true;
            }
            if (!((firstUnit & 128) == 0 || (this.extraData.charAt(mapping - 1) & 65280) == 0)) {
                z = false;
            }
            return z;
        } else if (isMaybeOrNonZeroCC(norm16)) {
            if (norm16 > MIN_NORMAL_MAYBE_YES && norm16 != JAMO_VT) {
                z = false;
            }
            return z;
        } else {
            if ((norm16 & 6) > 2) {
                z = false;
            }
            return z;
        }
    }

    public boolean isDecompInert(int c) {
        return isDecompYesAndZeroCC(getNorm16(c));
    }

    public boolean hasCompBoundaryBefore(int c) {
        return c < this.minCompNoMaybeCP || norm16HasCompBoundaryBefore(getNorm16(c));
    }

    public boolean hasCompBoundaryAfter(int c, boolean onlyContiguous) {
        return norm16HasCompBoundaryAfter(getNorm16(c), onlyContiguous);
    }

    public boolean isCompInert(int c, boolean onlyContiguous) {
        int norm16 = getNorm16(c);
        if (!isCompYesAndZeroCC(norm16) || (norm16 & 1) == 0 || (onlyContiguous && !isInert(norm16) && this.extraData.charAt(norm16 >> 1) > 511)) {
            return false;
        }
        return true;
    }

    public boolean hasFCDBoundaryBefore(int c) {
        return hasDecompBoundaryBefore(c);
    }

    public boolean hasFCDBoundaryAfter(int c) {
        return hasDecompBoundaryAfter(c);
    }

    public boolean isFCDInert(int c) {
        return getFCD16(c) <= 1;
    }

    private boolean isMaybe(int norm16) {
        return this.minMaybeYes <= norm16 && norm16 <= JAMO_VT;
    }

    private boolean isMaybeOrNonZeroCC(int norm16) {
        return norm16 >= this.minMaybeYes;
    }

    private static boolean isInert(int norm16) {
        return norm16 == 1;
    }

    private static boolean isJamoL(int norm16) {
        return norm16 == 2;
    }

    private static boolean isJamoVT(int norm16) {
        return norm16 == JAMO_VT;
    }

    private int hangulLVT() {
        return this.minYesNoMappingsOnly | 1;
    }

    private boolean isHangulLV(int norm16) {
        return norm16 == this.minYesNo;
    }

    private boolean isHangulLVT(int norm16) {
        return norm16 == hangulLVT();
    }

    private boolean isCompYesAndZeroCC(int norm16) {
        return norm16 < this.minNoNo;
    }

    private boolean isDecompYesAndZeroCC(int norm16) {
        return norm16 < this.minYesNo || norm16 == JAMO_VT || (this.minMaybeYes <= norm16 && norm16 <= MIN_NORMAL_MAYBE_YES);
    }

    private boolean isMostDecompYesAndZeroCC(int norm16) {
        return norm16 < this.minYesNo || norm16 == MIN_NORMAL_MAYBE_YES || norm16 == JAMO_VT;
    }

    private boolean isDecompNoAlgorithmic(int norm16) {
        return norm16 >= this.limitNoNo;
    }

    private int getCCFromNoNo(int norm16) {
        int mapping = norm16 >> 1;
        if ((this.extraData.charAt(mapping) & 128) != 0) {
            return this.extraData.charAt(mapping - 1) & 255;
        }
        return 0;
    }

    int getTrailCCFromCompYesAndZeroCC(int norm16) {
        if (norm16 <= this.minYesNo) {
            return 0;
        }
        return this.extraData.charAt(norm16 >> 1) >> 8;
    }

    private int mapAlgorithmic(int c, int norm16) {
        return ((norm16 >> 3) + c) - this.centerNoNoDelta;
    }

    private int getCompositionsListForDecompYes(int norm16) {
        if (norm16 < 2 || MIN_NORMAL_MAYBE_YES <= norm16) {
            return -1;
        }
        int i = norm16 - this.minMaybeYes;
        norm16 = i;
        if (i < 0) {
            norm16 += MIN_NORMAL_MAYBE_YES;
        }
        return norm16 >> 1;
    }

    private int getCompositionsListForComposite(int norm16) {
        int list = ((MIN_NORMAL_MAYBE_YES - this.minMaybeYes) + norm16) >> 1;
        return (list + 1) + (this.maybeYesCompositions.charAt(list) & 31);
    }

    private int getCompositionsListForMaybe(int norm16) {
        return (norm16 - this.minMaybeYes) >> 1;
    }

    private int getCompositionsList(int norm16) {
        if (isDecompYes(norm16)) {
            return getCompositionsListForDecompYes(norm16);
        }
        return getCompositionsListForComposite(norm16);
    }

    private int decomposeShort(CharSequence s, int src, int limit, boolean stopAtCompBoundary, boolean onlyContiguous, ReorderingBuffer buffer) {
        while (src < limit) {
            int c = Character.codePointAt(s, src);
            if (stopAtCompBoundary && c < this.minCompNoMaybeCP) {
                return src;
            }
            int norm16 = getNorm16(c);
            if (stopAtCompBoundary && norm16HasCompBoundaryBefore(norm16)) {
                return src;
            }
            src += Character.charCount(c);
            decompose(c, norm16, buffer);
            if (stopAtCompBoundary && norm16HasCompBoundaryAfter(norm16, onlyContiguous)) {
                return src;
            }
        }
        return src;
    }

    private void decompose(int c, int norm16, ReorderingBuffer buffer) {
        if (norm16 >= this.limitNoNo) {
            if (isMaybeOrNonZeroCC(norm16)) {
                buffer.append(c, getCCFromYesOrMaybe(norm16));
                return;
            } else {
                c = mapAlgorithmic(c, norm16);
                norm16 = getNorm16(c);
            }
        }
        int i = 0;
        if (norm16 < this.minYesNo) {
            buffer.append(c, 0);
        } else if (isHangulLV(norm16) || isHangulLVT(norm16)) {
            Hangul.decompose(c, buffer);
        } else {
            int mapping = norm16 >> 1;
            int firstUnit = this.extraData.charAt(mapping);
            int length = firstUnit & 31;
            int trailCC = firstUnit >> 8;
            if ((firstUnit & 128) != 0) {
                i = this.extraData.charAt(mapping - 1) >> 8;
            }
            mapping++;
            buffer.append(this.extraData, mapping, mapping + length, i, trailCC);
        }
    }

    private static int combine(String compositions, int list, int trail) {
        char key1;
        char charAt;
        char firstUnit;
        if (trail < COMP_1_TRAIL_LIMIT) {
            key1 = trail << 1;
            while (true) {
                charAt = compositions.charAt(list);
                firstUnit = charAt;
                if (key1 <= charAt) {
                    break;
                }
                list += (firstUnit & 1) + 2;
            }
            if (key1 == (firstUnit & COMP_1_TRAIL_MASK)) {
                if ((firstUnit & 1) != 0) {
                    return (compositions.charAt(list + 1) << 16) | compositions.charAt(list + 2);
                }
                return compositions.charAt(list + 1);
            }
        }
        char firstUnit2;
        key1 = COMP_1_TRAIL_LIMIT + ((trail >> 9) & -2);
        charAt = (trail << 6) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
        while (true) {
            firstUnit = compositions.charAt(list);
            firstUnit2 = firstUnit;
            if (key1 <= firstUnit) {
                if (key1 != (firstUnit2 & COMP_1_TRAIL_MASK)) {
                    break;
                }
                firstUnit = compositions.charAt(list + 1);
                char secondUnit = firstUnit;
                if (charAt > firstUnit) {
                    if ((32768 & firstUnit2) != 0) {
                        break;
                    }
                    list += 3;
                } else if (charAt == (COMP_2_TRAIL_MASK & secondUnit)) {
                    return ((-65473 & secondUnit) << 16) | compositions.charAt(list + 2);
                }
            } else {
                list += (firstUnit2 & 1) + 2;
            }
        }
        firstUnit = firstUnit2;
        return -1;
    }

    private void addComposites(int list, UnicodeSet set) {
        int firstUnit;
        do {
            int compositeAndFwd;
            firstUnit = this.maybeYesCompositions.charAt(list);
            if ((firstUnit & 1) == 0) {
                compositeAndFwd = this.maybeYesCompositions.charAt(list + 1);
                list += 2;
            } else {
                compositeAndFwd = ((this.maybeYesCompositions.charAt(list + 1) & -65473) << 16) | this.maybeYesCompositions.charAt(list + 2);
                list += 3;
            }
            int composite = compositeAndFwd >> 1;
            if ((compositeAndFwd & 1) != 0) {
                addComposites(getCompositionsListForComposite(getNorm16(composite)), set);
            }
            set.add(composite);
        } while ((32768 & firstUnit) == 0);
    }

    private void recompose(ReorderingBuffer buffer, int recomposeStartIndex, boolean onlyContiguous) {
        StringBuilder sb = buffer.getStringBuilder();
        int p = recomposeStartIndex;
        if (p != sb.length()) {
            int compositionsList = -1;
            int starter = -1;
            boolean starterIsSupplementary = false;
            int prevCC = 0;
            while (true) {
                int combine;
                int c = sb.codePointAt(p);
                p += Character.charCount(c);
                int norm16 = getNorm16(c);
                int cc = getCCFromYesOrMaybe(norm16);
                if (isMaybe(norm16) && compositionsList >= 0 && (prevCC < cc || prevCC == 0)) {
                    int pRemove;
                    if (isJamoVT(norm16)) {
                        if (c < Hangul.JAMO_T_BASE) {
                            char prev = (char) (sb.charAt(starter) - 4352);
                            if (prev < 19) {
                                pRemove = p - 1;
                                char syllable = (char) (44032 + (((prev * 21) + (c - 4449)) * 28));
                                if (p != sb.length()) {
                                    char charAt = (char) (sb.charAt(p) - Hangul.JAMO_T_BASE);
                                    char t = charAt;
                                    if (charAt < 28) {
                                        p++;
                                        syllable = (char) (syllable + t);
                                    }
                                }
                                sb.setCharAt(starter, syllable);
                                sb.delete(pRemove, p);
                                p = pRemove;
                            }
                        }
                        if (p == sb.length()) {
                            break;
                        }
                        compositionsList = -1;
                    } else {
                        combine = combine(this.maybeYesCompositions, compositionsList, c);
                        pRemove = combine;
                        if (combine >= 0) {
                            combine = pRemove >> 1;
                            int pRemove2 = p - Character.charCount(c);
                            sb.delete(pRemove2, p);
                            p = pRemove2;
                            if (starterIsSupplementary) {
                                if (combine > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                                    sb.setCharAt(starter, UTF16.getLeadSurrogate(combine));
                                    sb.setCharAt(starter + 1, UTF16.getTrailSurrogate(combine));
                                } else {
                                    sb.setCharAt(starter, (char) c);
                                    sb.deleteCharAt(starter + 1);
                                    starterIsSupplementary = false;
                                    p--;
                                }
                            } else if (combine > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                                starterIsSupplementary = true;
                                sb.setCharAt(starter, UTF16.getLeadSurrogate(combine));
                                sb.insert(starter + 1, UTF16.getTrailSurrogate(combine));
                                p++;
                            } else {
                                sb.setCharAt(starter, (char) combine);
                            }
                            if (p == sb.length()) {
                                break;
                            }
                            compositionsList = (pRemove & 1) != 0 ? getCompositionsListForComposite(getNorm16(combine)) : -1;
                        }
                    }
                }
                prevCC = cc;
                if (p == sb.length()) {
                    break;
                } else if (cc == 0) {
                    combine = getCompositionsListForDecompYes(norm16);
                    compositionsList = combine;
                    if (combine >= 0) {
                        if (c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                            starterIsSupplementary = false;
                            starter = p - 1;
                        } else {
                            starterIsSupplementary = true;
                            starter = p - 2;
                        }
                    }
                } else if (onlyContiguous) {
                    compositionsList = -1;
                }
            }
            buffer.flush();
        }
    }

    public int composePair(int a, int b) {
        int norm16 = getNorm16(a);
        if (isInert(norm16)) {
            return -1;
        }
        int list;
        if (norm16 < this.minYesNoMappingsOnly) {
            if (isJamoL(norm16)) {
                b -= 4449;
                if (b < 0 || b >= 21) {
                    return -1;
                }
                return Hangul.HANGUL_BASE + ((((a - 4352) * 21) + b) * 28);
            } else if (isHangulLV(norm16)) {
                b -= 4519;
                if (b <= 0 || b >= 28) {
                    return -1;
                }
                return a + b;
            } else {
                list = ((MIN_NORMAL_MAYBE_YES - this.minMaybeYes) + norm16) >> 1;
                if (norm16 > this.minYesNo) {
                    list += (this.maybeYesCompositions.charAt(list) & 31) + 1;
                }
            }
        } else if (norm16 < this.minMaybeYes || MIN_NORMAL_MAYBE_YES <= norm16) {
            return -1;
        } else {
            list = getCompositionsListForMaybe(norm16);
        }
        if (b < 0 || 1114111 < b) {
            return -1;
        }
        return combine(this.maybeYesCompositions, list, b) >> 1;
    }

    private boolean hasCompBoundaryBefore(int c, int norm16) {
        return c < this.minCompNoMaybeCP || norm16HasCompBoundaryBefore(norm16);
    }

    private boolean norm16HasCompBoundaryBefore(int norm16) {
        return norm16 < this.minNoNoCompNoMaybeCC || isAlgorithmicNoNo(norm16);
    }

    private boolean hasCompBoundaryBefore(CharSequence s, int src, int limit) {
        return src == limit || hasCompBoundaryBefore(Character.codePointAt(s, src));
    }

    private boolean norm16HasCompBoundaryAfter(int norm16, boolean onlyContiguous) {
        if ((norm16 & 1) == 0 || (onlyContiguous && !isTrailCC01ForCompBoundaryAfter(norm16))) {
            return false;
        }
        return true;
    }

    private boolean hasCompBoundaryAfter(CharSequence s, int start, int p, boolean onlyContiguous) {
        return start == p || hasCompBoundaryAfter(Character.codePointBefore(s, p), onlyContiguous);
    }

    private boolean isTrailCC01ForCompBoundaryAfter(int norm16) {
        if (isInert(norm16) || (isDecompNoAlgorithmic(norm16) ? (norm16 & 6) > 2 : this.extraData.charAt(norm16 >> 1) > 511)) {
            return true;
        }
        return false;
    }

    private int findPreviousCompBoundary(CharSequence s, int p, boolean onlyContiguous) {
        while (p > 0) {
            int c = Character.codePointBefore(s, p);
            int norm16 = getNorm16(c);
            if (norm16HasCompBoundaryAfter(norm16, onlyContiguous)) {
                break;
            }
            p -= Character.charCount(c);
            if (hasCompBoundaryBefore(c, norm16)) {
                break;
            }
        }
        return p;
    }

    private int findNextCompBoundary(CharSequence s, int p, int limit, boolean onlyContiguous) {
        while (p < limit) {
            int c = Character.codePointAt(s, p);
            int norm16 = this.normTrie.get(c);
            if (hasCompBoundaryBefore(c, norm16)) {
                break;
            }
            p += Character.charCount(c);
            if (norm16HasCompBoundaryAfter(norm16, onlyContiguous)) {
                break;
            }
        }
        return p;
    }

    private int findPreviousFCDBoundary(CharSequence s, int p) {
        while (p > 0) {
            int c = Character.codePointBefore(s, p);
            if (c < this.minDecompNoCP) {
                break;
            }
            int norm16 = getNorm16(c);
            int norm162 = norm16;
            if (norm16HasDecompBoundaryAfter(norm16)) {
                break;
            }
            p -= Character.charCount(c);
            if (norm16HasDecompBoundaryBefore(norm162)) {
                break;
            }
        }
        return p;
    }

    private int findNextFCDBoundary(CharSequence s, int p, int limit) {
        while (p < limit) {
            int c = Character.codePointAt(s, p);
            if (c < this.minLcccCP) {
                break;
            }
            int norm16 = getNorm16(c);
            int norm162 = norm16;
            if (norm16HasDecompBoundaryBefore(norm16)) {
                break;
            }
            p += Character.charCount(c);
            if (norm16HasDecompBoundaryAfter(norm162)) {
                break;
            }
        }
        return p;
    }

    private int getPreviousTrailCC(CharSequence s, int start, int p) {
        if (start == p) {
            return 0;
        }
        return getFCD16(Character.codePointBefore(s, p));
    }

    private void addToStartSet(Trie2Writable newData, int origin, int decompLead) {
        int canonValue = newData.get(decompLead);
        if ((4194303 & canonValue) != 0 || origin == 0) {
            UnicodeSet set;
            if ((canonValue & 2097152) == 0) {
                int firstOrigin = canonValue & 2097151;
                newData.set(decompLead, (2097152 | (-2097152 & canonValue)) | this.canonStartSets.size());
                ArrayList arrayList = this.canonStartSets;
                UnicodeSet unicodeSet = new UnicodeSet();
                set = unicodeSet;
                arrayList.add(unicodeSet);
                if (firstOrigin != 0) {
                    set.add(firstOrigin);
                }
            } else {
                set = (UnicodeSet) this.canonStartSets.get(canonValue & 2097151);
            }
            set.add(origin);
            return;
        }
        newData.set(decompLead, canonValue | origin);
    }
}
