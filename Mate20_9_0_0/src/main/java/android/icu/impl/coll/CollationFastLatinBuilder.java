package android.icu.impl.coll;

import android.icu.util.CharsTrie;
import android.icu.util.CharsTrie.Entry;
import android.icu.util.CharsTrie.Iterator;
import dalvik.bytecode.Opcodes;
import java.lang.reflect.Array;

final class CollationFastLatinBuilder {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final long CONTRACTION_FLAG = 2147483648L;
    private static final int NUM_SPECIAL_GROUPS = 4;
    private long ce0 = 0;
    private long ce1 = 0;
    private long[][] charCEs = ((long[][]) Array.newInstance(long.class, new int[]{448, 2}));
    private UVector64 contractionCEs = new UVector64();
    private long firstDigitPrimary = 0;
    private long firstLatinPrimary = 0;
    private long firstShortPrimary = 0;
    private int headerLength = 0;
    private long lastLatinPrimary = 0;
    long[] lastSpecialPrimaries = new long[4];
    private char[] miniCEs = null;
    private StringBuilder result = new StringBuilder();
    private boolean shortPrimaryOverflow = false;
    private UVector64 uniqueCEs = new UVector64();

    private static final int compareInt64AsUnsigned(long a, long b) {
        a -= Long.MIN_VALUE;
        b -= Long.MIN_VALUE;
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return 0;
    }

    private static final int binarySearch(long[] list, int limit, long ce) {
        if (limit == 0) {
            return -1;
        }
        int start = 0;
        while (true) {
            int i = (int) ((((long) start) + ((long) limit)) / 2);
            int cmp = compareInt64AsUnsigned(ce, list[i]);
            if (cmp == 0) {
                return i;
            }
            if (cmp < 0) {
                if (i == start) {
                    return ~start;
                }
                limit = i;
            } else if (i == start) {
                return ~(start + 1);
            } else {
                start = i;
            }
        }
    }

    CollationFastLatinBuilder() {
    }

    boolean forData(CollationData data) {
        if (this.result.length() != 0) {
            throw new IllegalStateException("attempt to reuse a CollationFastLatinBuilder");
        } else if (!loadGroups(data)) {
            return false;
        } else {
            this.firstShortPrimary = this.firstDigitPrimary;
            getCEs(data);
            encodeUniqueCEs();
            if (this.shortPrimaryOverflow) {
                this.firstShortPrimary = this.firstLatinPrimary;
                resetCEs();
                getCEs(data);
                encodeUniqueCEs();
            }
            boolean ok = this.shortPrimaryOverflow ^ 1;
            if (ok) {
                encodeCharCEs();
                encodeContractions();
            }
            this.contractionCEs.removeAllElements();
            this.uniqueCEs.removeAllElements();
            return ok;
        }
    }

    char[] getHeader() {
        char[] resultArray = new char[this.headerLength];
        this.result.getChars(0, this.headerLength, resultArray, 0);
        return resultArray;
    }

    char[] getTable() {
        char[] resultArray = new char[(this.result.length() - this.headerLength)];
        this.result.getChars(this.headerLength, this.result.length(), resultArray, 0);
        return resultArray;
    }

    private boolean loadGroups(CollationData data) {
        this.headerLength = 5;
        this.result.append((char) (this.headerLength | 512));
        for (int i = 0; i < 4; i++) {
            this.lastSpecialPrimaries[i] = data.getLastPrimaryForGroup(4096 + i);
            if (this.lastSpecialPrimaries[i] == 0) {
                return false;
            }
            this.result.append(0);
        }
        this.firstDigitPrimary = data.getFirstPrimaryForGroup(4100);
        this.firstLatinPrimary = data.getFirstPrimaryForGroup(25);
        this.lastLatinPrimary = data.getLastPrimaryForGroup(25);
        if (this.firstDigitPrimary == 0 || this.firstLatinPrimary == 0) {
            return false;
        }
        return true;
    }

    private boolean inSameGroup(long p, long q) {
        boolean z = true;
        if (p >= this.firstShortPrimary) {
            if (q < this.firstShortPrimary) {
                z = false;
            }
            return z;
        } else if (q >= this.firstShortPrimary) {
            return false;
        } else {
            long lastVariablePrimary = this.lastSpecialPrimaries[3];
            if (p > lastVariablePrimary) {
                if (q <= lastVariablePrimary) {
                    z = false;
                }
                return z;
            } else if (q > lastVariablePrimary) {
                return false;
            } else {
                int i = 0;
                while (true) {
                    long lastPrimary = this.lastSpecialPrimaries[i];
                    if (p <= lastPrimary) {
                        if (q > lastPrimary) {
                            z = false;
                        }
                        return z;
                    } else if (q <= lastPrimary) {
                        return false;
                    } else {
                        i++;
                    }
                }
            }
        }
    }

    private void resetCEs() {
        this.contractionCEs.removeAllElements();
        this.uniqueCEs.removeAllElements();
        this.shortPrimaryOverflow = false;
        this.result.setLength(this.headerLength);
    }

    private void getCEs(CollationData data) {
        CollationData collationData = data;
        int i = 0;
        char c = 0;
        while (true) {
            CollationData d;
            int ce32;
            if (c == 384) {
                c = 8192;
            } else if (c == 8256) {
                this.contractionCEs.addElement(511);
                return;
            }
            char c2 = c;
            int ce322 = collationData.getCE32(c2);
            if (ce322 == 192) {
                d = collationData.base;
                ce32 = d.getCE32(c2);
            } else {
                ce32 = ce322;
                d = collationData;
            }
            if (getCEsFromCE32(d, c2, ce32)) {
                this.charCEs[i][0] = this.ce0;
                this.charCEs[i][1] = this.ce1;
                addUniqueCE(this.ce0);
                addUniqueCE(this.ce1);
            } else {
                long[] jArr = this.charCEs[i];
                this.ce0 = Collation.NO_CE;
                jArr[0] = Collation.NO_CE;
                jArr = this.charCEs[i];
                this.ce1 = 0;
                jArr[1] = 0;
            }
            if (c2 == 0 && !isContractionCharCE(this.ce0)) {
                addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
                this.charCEs[0][0] = 6442450944L;
                this.charCEs[0][1] = 0;
            }
            i++;
            c = (char) (c2 + 1);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0095  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x008c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getCEsFromCE32(CollationData data, int c, int ce32) {
        int tagFromCE32;
        CollationData collationData = data;
        int ce322 = collationData.getFinalCE32(ce32);
        this.ce1 = 0;
        boolean z = false;
        if (Collation.isSimpleOrLongCE32(ce322)) {
            this.ce0 = Collation.ceFromCE32(ce322);
        } else {
            tagFromCE32 = Collation.tagFromCE32(ce322);
            if (tagFromCE32 == 9) {
                tagFromCE32 = c;
                return getCEsFromContractionCE32(collationData, ce322);
            } else if (tagFromCE32 != 14) {
                int length;
                switch (tagFromCE32) {
                    case 4:
                        this.ce0 = Collation.latinCE0FromCE32(ce322);
                        this.ce1 = Collation.latinCE1FromCE32(ce322);
                        break;
                    case 5:
                        tagFromCE32 = Collation.indexFromCE32(ce322);
                        length = Collation.lengthFromCE32(ce322);
                        if (length <= 2) {
                            this.ce0 = Collation.ceFromCE32(collationData.ce32s[tagFromCE32]);
                            if (length == 2) {
                                this.ce1 = Collation.ceFromCE32(collationData.ce32s[tagFromCE32 + 1]);
                                break;
                            }
                        }
                        return false;
                        break;
                    case 6:
                        tagFromCE32 = Collation.indexFromCE32(ce322);
                        length = Collation.lengthFromCE32(ce322);
                        if (length <= 2) {
                            this.ce0 = collationData.ces[tagFromCE32];
                            if (length == 2) {
                                this.ce1 = collationData.ces[tagFromCE32 + 1];
                                break;
                            }
                        }
                        return false;
                        break;
                    default:
                        return false;
                }
            } else {
                this.ce0 = collationData.getCEFromOffsetCE32(c, ce322);
                if (this.ce0 != 0) {
                    if (this.ce1 == 0) {
                        z = true;
                    }
                    return z;
                }
                long p0 = this.ce0 >>> 32;
                if (p0 == 0 || p0 > this.lastLatinPrimary) {
                    return false;
                }
                int lower32_0 = (int) this.ce0;
                if ((p0 < this.firstShortPrimary && (lower32_0 & -16384) != 83886080) || (lower32_0 & Collation.ONLY_TERTIARY_MASK) < Collation.COMMON_WEIGHT16) {
                    return false;
                }
                if (this.ce1 != 0) {
                    long p1 = this.ce1 >>> 32;
                    if (!p1 != 0 ? p0 >= this.firstShortPrimary : inSameGroup(p0, p1)) {
                        return false;
                    }
                    int lower32_1 = (int) this.ce1;
                    if ((lower32_1 >>> 16) == 0) {
                        return false;
                    }
                    if (p1 != 0) {
                        if (p1 < this.firstShortPrimary && (lower32_1 & -16384) != 83886080) {
                            return false;
                        }
                    }
                    if ((lower32_0 & Collation.ONLY_TERTIARY_MASK) < Collation.COMMON_WEIGHT16) {
                        return false;
                    }
                }
                if (((this.ce0 | this.ce1) & 192) != 0) {
                    return false;
                }
                return true;
            }
        }
        tagFromCE32 = c;
        if (this.ce0 != 0) {
        }
    }

    private boolean getCEsFromContractionCE32(CollationData data, int ce32) {
        CollationData collationData = data;
        int trieIndex = Collation.indexFromCE32(ce32);
        int ce322 = collationData.getCE32FromContexts(trieIndex);
        int contractionIndex = this.contractionCEs.size();
        if (getCEsFromCE32(collationData, -1, ce322)) {
            addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
        } else {
            addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, Collation.NO_CE, 0);
        }
        int i = 0;
        Iterator suffixes = CharsTrie.iterator(collationData.contexts, trieIndex + 2, 0);
        int prevX = -1;
        boolean addContraction = false;
        while (true) {
            Iterator suffixes2 = suffixes;
            if (!suffixes2.hasNext()) {
                break;
            }
            Entry entry = suffixes2.next();
            CharSequence suffix = entry.chars;
            int x = CollationFastLatin.getCharIndex(suffix.charAt(i));
            if (x >= 0) {
                if (x != prevX) {
                    int trieIndex2;
                    boolean addContraction2;
                    int i2;
                    CharSequence suffix2 = suffix;
                    int x2 = x;
                    Entry entry2 = entry;
                    if (addContraction) {
                        trieIndex2 = trieIndex;
                        trieIndex = 1;
                        addContractionEntry(prevX, this.ce0, this.ce1);
                    } else {
                        trieIndex2 = trieIndex;
                        trieIndex = 1;
                    }
                    int ce323 = entry2.value;
                    if (suffix2.length() == trieIndex) {
                        trieIndex = -1;
                        if (getCEsFromCE32(collationData, -1, ce323)) {
                            addContraction2 = true;
                            addContraction = addContraction2;
                            prevX = x2;
                            i2 = trieIndex;
                            suffixes = suffixes2;
                            trieIndex = trieIndex2;
                            i = 0;
                        }
                    } else {
                        trieIndex = -1;
                    }
                    addContractionEntry(x2, Collation.NO_CE, 0);
                    addContraction2 = false;
                    addContraction = addContraction2;
                    prevX = x2;
                    i2 = trieIndex;
                    suffixes = suffixes2;
                    trieIndex = trieIndex2;
                    i = 0;
                } else if (addContraction) {
                    addContractionEntry(x, 4311744768L, 0);
                    addContraction = false;
                }
            }
            suffixes = suffixes2;
            i = 0;
        }
        boolean z = true;
        if (addContraction) {
            addContractionEntry(prevX, this.ce0, this.ce1);
        }
        this.ce0 = 6442450944L | ((long) contractionIndex);
        this.ce1 = 0;
        return z;
    }

    private void addContractionEntry(int x, long cce0, long cce1) {
        this.contractionCEs.addElement((long) x);
        this.contractionCEs.addElement(cce0);
        this.contractionCEs.addElement(cce1);
        addUniqueCE(cce0);
        addUniqueCE(cce1);
    }

    private void addUniqueCE(long ce) {
        if (ce != 0 && (ce >>> 32) != 1) {
            ce &= -49153;
            int i = binarySearch(this.uniqueCEs.getBuffer(), this.uniqueCEs.size(), ce);
            if (i < 0) {
                this.uniqueCEs.insertElementAt(ce, ~i);
            }
        }
    }

    private int getMiniCE(long ce) {
        return this.miniCEs[binarySearch(this.uniqueCEs.getBuffer(), this.uniqueCEs.size(), ce & -49153)];
    }

    private void encodeUniqueCEs() {
        int group;
        this.miniCEs = new char[this.uniqueCEs.size()];
        int group2 = 0;
        long lastGroupPrimary = this.lastSpecialPrimaries[0];
        long prevPrimary = 0;
        int prevSecondary = 0;
        int pri = 0;
        int sec = 0;
        int ter = 0;
        for (int i = 0; i < this.uniqueCEs.size(); i++) {
            long ce = this.uniqueCEs.elementAti(i);
            long p = ce >>> 32;
            group = group2;
            if (p != prevPrimary) {
                while (p > lastGroupPrimary) {
                    this.result.setCharAt(1 + group, (char) pri);
                    lastGroupPrimary = group + 1;
                    if (lastGroupPrimary >= 4) {
                        group = lastGroupPrimary;
                        lastGroupPrimary = 4294967295L;
                        break;
                    }
                    group = lastGroupPrimary;
                    lastGroupPrimary = this.lastSpecialPrimaries[lastGroupPrimary];
                }
                long j = lastGroupPrimary;
                long lastGroupPrimary2 = lastGroupPrimary;
                if (p < this.firstShortPrimary) {
                    if (pri == 0) {
                        group2 = 3072;
                    } else if (pri < 4088) {
                        pri += 8;
                        prevPrimary = p;
                        prevSecondary = Collation.COMMON_WEIGHT16;
                        sec = 160;
                        ter = 0;
                        lastGroupPrimary = lastGroupPrimary2;
                    } else {
                        this.miniCEs[i] = 1;
                        group2 = group;
                        lastGroupPrimary = lastGroupPrimary2;
                    }
                } else if (pri < 4096) {
                    group2 = 4096;
                } else if (pri < 63488) {
                    pri += 1024;
                    prevPrimary = p;
                    prevSecondary = Collation.COMMON_WEIGHT16;
                    sec = 160;
                    ter = 0;
                    lastGroupPrimary = lastGroupPrimary2;
                } else {
                    this.shortPrimaryOverflow = true;
                    this.miniCEs[i] = 1;
                    group2 = group;
                    lastGroupPrimary = lastGroupPrimary2;
                }
                pri = group2;
                prevPrimary = p;
                prevSecondary = Collation.COMMON_WEIGHT16;
                sec = 160;
                ter = 0;
                lastGroupPrimary = lastGroupPrimary2;
            }
            group2 = (int) ce;
            int s = group2 >>> 16;
            long j2 = lastGroupPrimary;
            if (s != prevSecondary) {
                int sec2;
                if (pri == 0) {
                    if (sec == 0) {
                        sec2 = CollationFastLatin.LATIN_LIMIT;
                    } else if (sec < 992) {
                        sec2 = sec + 32;
                    } else {
                        this.miniCEs[i] = 1;
                        group2 = group;
                        lastGroupPrimary = j2;
                    }
                    prevSecondary = s;
                } else if (s < Collation.COMMON_WEIGHT16) {
                    if (sec == 160) {
                        sec2 = 0;
                    } else if (sec < 128) {
                        sec += 32;
                        prevSecondary = s;
                        ter = 0;
                    } else {
                        this.miniCEs[i] = 1;
                        group2 = group;
                        lastGroupPrimary = j2;
                    }
                } else if (s == Collation.COMMON_WEIGHT16) {
                    sec2 = 160;
                } else if (sec < 192) {
                    sec2 = 192;
                } else if (sec < 352) {
                    sec += 32;
                    prevSecondary = s;
                    ter = 0;
                } else {
                    this.miniCEs[i] = 1;
                    group2 = group;
                    lastGroupPrimary = j2;
                }
                sec = sec2;
                prevSecondary = s;
                ter = 0;
            }
            if ((group2 & Collation.ONLY_TERTIARY_MASK) > Collation.COMMON_WEIGHT16) {
                if (ter < 7) {
                    ter++;
                } else {
                    this.miniCEs[i] = 1;
                    group2 = group;
                    lastGroupPrimary = j2;
                }
            }
            if (3072 > pri || pri > 4088) {
                this.miniCEs[i] = (char) ((pri | sec) | ter);
            } else {
                this.miniCEs[i] = (char) (pri | ter);
            }
            group2 = group;
            lastGroupPrimary = j2;
        }
        group = group2;
    }

    private void encodeCharCEs() {
        int i;
        int miniCEsStart = this.result.length();
        for (i = 0; i < 448; i++) {
            this.result.append(0);
        }
        i = this.result.length();
        for (int i2 = 0; i2 < 448; i2++) {
            long ce = this.charCEs[i2][0];
            if (!isContractionCharCE(ce)) {
                int miniCE = encodeTwoCEs(ce, this.charCEs[i2][1]);
                if ((miniCE >>> 16) > 0) {
                    int expansionIndex = this.result.length() - i;
                    if (expansionIndex > Opcodes.OP_NEW_INSTANCE_JUMBO) {
                        miniCE = 1;
                    } else {
                        StringBuilder stringBuilder = this.result;
                        stringBuilder.append((char) (miniCE >> 16));
                        stringBuilder.append((char) miniCE);
                        miniCE = 2048 | expansionIndex;
                    }
                }
                this.result.setCharAt(miniCEsStart + i2, (char) miniCE);
            }
        }
    }

    private void encodeContractions() {
        int i = 448;
        int indexBase = this.headerLength + 448;
        int firstContractionIndex = this.result.length();
        int i2 = 0;
        int i3 = 0;
        while (i3 < i) {
            long ce = this.charCEs[i3][i2];
            if (isContractionCharCE(ce)) {
                int contractionIndex = this.result.length() - indexBase;
                if (contractionIndex > Opcodes.OP_NEW_INSTANCE_JUMBO) {
                    this.result.setCharAt(this.headerLength + i3, 1);
                } else {
                    boolean firstTriple = true;
                    int index = ((int) ce) & Integer.MAX_VALUE;
                    while (true) {
                        long x = this.contractionCEs.elementAti(index);
                        if (x == 511 && !firstTriple) {
                            break;
                        }
                        int i4;
                        long ce2;
                        int index2 = index;
                        i = encodeTwoCEs(this.contractionCEs.elementAti(index + 1), this.contractionCEs.elementAti(index + 2));
                        if (i == 1) {
                            i4 = i3;
                            ce2 = ce;
                            this.result.append((char) ((int) (x | 512)));
                        } else {
                            i4 = i3;
                            ce2 = ce;
                            if ((i >>> 16) == 0) {
                                this.result.append((char) ((int) (1024 | x)));
                                this.result.append((char) i);
                            } else {
                                this.result.append((char) ((int) (1536 | x)));
                                StringBuilder stringBuilder = this.result;
                                stringBuilder.append((char) (i >> 16));
                                stringBuilder.append((char) i);
                            }
                        }
                        firstTriple = false;
                        index = index2 + 3;
                        i3 = i4;
                        ce = ce2;
                        i = 448;
                        i2 = 0;
                    }
                    this.result.setCharAt(this.headerLength + i3, (char) (1024 | contractionIndex));
                }
            }
            i3++;
        }
        if (this.result.length() > firstContractionIndex) {
            this.result.append(511);
        }
    }

    private int encodeTwoCEs(long first, long second) {
        if (first == 0) {
            return 0;
        }
        if (first == Collation.NO_CE) {
            return 1;
        }
        int miniCE = getMiniCE(first);
        if (miniCE == 1) {
            return miniCE;
        }
        if (miniCE >= 4096) {
            miniCE |= ((((int) first) & Collation.CASE_MASK) >> 11) + 8;
        }
        if (second == 0) {
            return miniCE;
        }
        int miniCE1 = getMiniCE(second);
        if (miniCE1 == 1) {
            return miniCE1;
        }
        int case1 = ((int) second) & Collation.CASE_MASK;
        if (miniCE >= 4096 && (miniCE & 992) == 160) {
            int sec1 = miniCE1 & 992;
            int ter1 = miniCE1 & 7;
            if (sec1 >= CollationFastLatin.LATIN_LIMIT && case1 == 0 && ter1 == 0) {
                return (miniCE & -993) | sec1;
            }
        }
        if (miniCE1 <= 992 || 4096 <= miniCE1) {
            miniCE1 |= (case1 >> 11) + 8;
        }
        return (miniCE << 16) | miniCE1;
    }

    private static boolean isContractionCharCE(long ce) {
        return (ce >>> 32) == 1 && ce != Collation.NO_CE;
    }
}
