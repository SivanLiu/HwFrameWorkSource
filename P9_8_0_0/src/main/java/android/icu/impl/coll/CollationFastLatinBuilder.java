package android.icu.impl.coll;

import android.icu.util.CharsTrie;
import android.icu.util.CharsTrie.Entry;
import android.icu.util.CharsTrie.Iterator;
import dalvik.bytecode.Opcodes;
import java.lang.reflect.Array;

final class CollationFastLatinBuilder {
    static final /* synthetic */ boolean -assertionsDisabled = (CollationFastLatinBuilder.class.desiredAssertionStatus() ^ 1);
    private static final long CONTRACTION_FLAG = 2147483648L;
    private static final int NUM_SPECIAL_GROUPS = 4;
    private long ce0 = 0;
    private long ce1 = 0;
    private long[][] charCEs = ((long[][]) Array.newInstance(Long.TYPE, new int[]{448, 2}));
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

    private void addUniqueCE(long r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.icu.impl.coll.CollationFastLatinBuilder.addUniqueCE(long):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.impl.coll.CollationFastLatinBuilder.addUniqueCE(long):void");
    }

    private static final int binarySearch(long[] r1, int r2, long r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.icu.impl.coll.CollationFastLatinBuilder.binarySearch(long[], int, long):int
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.impl.coll.CollationFastLatinBuilder.binarySearch(long[], int, long):int");
    }

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
            this.lastSpecialPrimaries[i] = data.getLastPrimaryForGroup(i + 4096);
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
        if (p >= this.firstShortPrimary) {
            return q >= this.firstShortPrimary;
        } else if (q >= this.firstShortPrimary) {
            return false;
        } else {
            long lastVariablePrimary = this.lastSpecialPrimaries[3];
            if (p > lastVariablePrimary) {
                return q > lastVariablePrimary;
            } else if (q > lastVariablePrimary) {
                return false;
            } else {
                if (-assertionsDisabled || !(p == 0 || q == 0)) {
                    long lastPrimary;
                    int i = 0;
                    while (true) {
                        lastPrimary = this.lastSpecialPrimaries[i];
                        if (p <= lastPrimary) {
                            break;
                        } else if (q <= lastPrimary) {
                            return false;
                        } else {
                            i++;
                        }
                    }
                    return q <= lastPrimary;
                }
                throw new AssertionError();
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
        int i = 0;
        int i2 = 0;
        while (true) {
            CollationData d;
            if (i2 == CollationFastLatin.LATIN_LIMIT) {
                i2 = 8192;
            } else if (i2 == 8256) {
                this.contractionCEs.addElement(511);
                return;
            }
            int ce32 = data.getCE32(i2);
            if (ce32 == 192) {
                d = data.base;
                ce32 = d.getCE32(i2);
            } else {
                d = data;
            }
            if (getCEsFromCE32(d, i2, ce32)) {
                this.charCEs[i][0] = this.ce0;
                this.charCEs[i][1] = this.ce1;
                addUniqueCE(this.ce0);
                addUniqueCE(this.ce1);
            } else {
                long[] jArr = this.charCEs[i];
                this.ce0 = Collation.NO_CE;
                jArr[0] = 4311744768L;
                jArr = this.charCEs[i];
                this.ce1 = 0;
                jArr[1] = 0;
            }
            if (i2 == 0 && (isContractionCharCE(this.ce0) ^ 1) != 0) {
                if (-assertionsDisabled || this.contractionCEs.isEmpty()) {
                    addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
                    this.charCEs[0][0] = 6442450944L;
                    this.charCEs[0][1] = 0;
                } else {
                    throw new AssertionError();
                }
            }
            i++;
            i2 = (char) (i2 + 1);
        }
    }

    private boolean getCEsFromCE32(CollationData data, int c, int ce32) {
        ce32 = data.getFinalCE32(ce32);
        this.ce1 = 0;
        if (Collation.isSimpleOrLongCE32(ce32)) {
            this.ce0 = Collation.ceFromCE32(ce32);
        } else {
            int index;
            int length;
            switch (Collation.tagFromCE32(ce32)) {
                case 4:
                    this.ce0 = Collation.latinCE0FromCE32(ce32);
                    this.ce1 = Collation.latinCE1FromCE32(ce32);
                    break;
                case 5:
                    index = Collation.indexFromCE32(ce32);
                    length = Collation.lengthFromCE32(ce32);
                    if (length <= 2) {
                        this.ce0 = Collation.ceFromCE32(data.ce32s[index]);
                        if (length == 2) {
                            this.ce1 = Collation.ceFromCE32(data.ce32s[index + 1]);
                            break;
                        }
                    }
                    return false;
                    break;
                case 6:
                    index = Collation.indexFromCE32(ce32);
                    length = Collation.lengthFromCE32(ce32);
                    if (length <= 2) {
                        this.ce0 = data.ces[index];
                        if (length == 2) {
                            this.ce1 = data.ces[index + 1];
                            break;
                        }
                    }
                    return false;
                    break;
                case 9:
                    if (-assertionsDisabled || c >= 0) {
                        return getCEsFromContractionCE32(data, ce32);
                    }
                    throw new AssertionError();
                case 14:
                    if (-assertionsDisabled || c >= 0) {
                        this.ce0 = data.getCEFromOffsetCE32(c, ce32);
                        break;
                    }
                    throw new AssertionError();
                default:
                    return false;
            }
        }
        if (this.ce0 == 0) {
            boolean z;
            if (this.ce1 == 0) {
                z = true;
            } else {
                z = false;
            }
            return z;
        }
        long p0 = this.ce0 >>> 32;
        if (p0 == 0) {
            return false;
        }
        if (p0 > this.lastLatinPrimary) {
            return false;
        }
        int lower32_0 = (int) this.ce0;
        if (p0 < this.firstShortPrimary && (lower32_0 & -16384) != 83886080) {
            return false;
        }
        if ((lower32_0 & Collation.ONLY_TERTIARY_MASK) < Collation.COMMON_WEIGHT16) {
            return false;
        }
        if (this.ce1 != 0) {
            long p1 = this.ce1 >>> 32;
            if (p1 != 0 ? (inSameGroup(p0, p1) ^ 1) != 0 : p0 < this.firstShortPrimary) {
                return false;
            }
            int lower32_1 = (int) this.ce1;
            if ((lower32_1 >>> 16) == 0) {
                return false;
            }
            if (p1 != 0 && p1 < this.firstShortPrimary && (lower32_1 & -16384) != 83886080) {
                return false;
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

    private boolean getCEsFromContractionCE32(CollationData data, int ce32) {
        int trieIndex = Collation.indexFromCE32(ce32);
        ce32 = data.getCE32FromContexts(trieIndex);
        if (-assertionsDisabled || !Collation.isContractionCE32(ce32)) {
            int contractionIndex = this.contractionCEs.size();
            if (getCEsFromCE32(data, -1, ce32)) {
                addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
            } else {
                addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, Collation.NO_CE, 0);
            }
            int prevX = -1;
            boolean addContraction = false;
            Iterator suffixes = CharsTrie.iterator(data.contexts, trieIndex + 2, 0);
            while (suffixes.hasNext()) {
                Entry entry = suffixes.next();
                CharSequence suffix = entry.chars;
                int x = CollationFastLatin.getCharIndex(suffix.charAt(0));
                if (x >= 0) {
                    if (x != prevX) {
                        if (addContraction) {
                            addContractionEntry(prevX, this.ce0, this.ce1);
                        }
                        ce32 = entry.value;
                        if (suffix.length() == 1 && getCEsFromCE32(data, -1, ce32)) {
                            addContraction = true;
                        } else {
                            addContractionEntry(x, Collation.NO_CE, 0);
                            addContraction = false;
                        }
                        prevX = x;
                    } else if (addContraction) {
                        addContractionEntry(x, Collation.NO_CE, 0);
                        addContraction = false;
                    }
                }
            }
            if (addContraction) {
                addContractionEntry(prevX, this.ce0, this.ce1);
            }
            this.ce0 = ((long) contractionIndex) | 6442450944L;
            this.ce1 = 0;
            return true;
        }
        throw new AssertionError();
    }

    private void addContractionEntry(int x, long cce0, long cce1) {
        this.contractionCEs.addElement((long) x);
        this.contractionCEs.addElement(cce0);
        this.contractionCEs.addElement(cce1);
        addUniqueCE(cce0);
        addUniqueCE(cce1);
    }

    private int getMiniCE(long ce) {
        int index = binarySearch(this.uniqueCEs.getBuffer(), this.uniqueCEs.size(), ce & -49153);
        if (-assertionsDisabled || index >= 0) {
            return this.miniCEs[index];
        }
        throw new AssertionError();
    }

    private void encodeUniqueCEs() {
        this.miniCEs = new char[this.uniqueCEs.size()];
        int group = 0;
        long lastGroupPrimary = this.lastSpecialPrimaries[0];
        if (-assertionsDisabled || (((int) this.uniqueCEs.elementAti(0)) >>> 16) != 0) {
            long prevPrimary = 0;
            int prevSecondary = 0;
            int pri = 0;
            int sec = 0;
            int ter = 0;
            int i = 0;
            while (i < this.uniqueCEs.size()) {
                long ce = this.uniqueCEs.elementAti(i);
                long p = ce >>> 32;
                if (p != prevPrimary) {
                    while (p > lastGroupPrimary) {
                        if (-assertionsDisabled || pri <= 4088) {
                            this.result.setCharAt(group + 1, (char) pri);
                            group++;
                            if (group >= 4) {
                                lastGroupPrimary = 4294967295L;
                                break;
                            }
                            lastGroupPrimary = this.lastSpecialPrimaries[group];
                        } else {
                            throw new AssertionError();
                        }
                    }
                    if (p < this.firstShortPrimary) {
                        if (pri == 0) {
                            pri = 3072;
                        } else if (pri < 4088) {
                            pri += 8;
                        } else {
                            this.miniCEs[i] = '\u0001';
                            i++;
                        }
                    } else if (pri < 4096) {
                        pri = 4096;
                    } else if (pri < 63488) {
                        pri += 1024;
                    } else {
                        this.shortPrimaryOverflow = true;
                        this.miniCEs[i] = '\u0001';
                        i++;
                    }
                    prevPrimary = p;
                    prevSecondary = Collation.COMMON_WEIGHT16;
                    sec = 160;
                    ter = 0;
                }
                int lower32 = (int) ce;
                int s = lower32 >>> 16;
                if (s != prevSecondary) {
                    if (pri == 0) {
                        if (sec == 0) {
                            sec = CollationFastLatin.LATIN_LIMIT;
                        } else if (sec < 992) {
                            sec += 32;
                        } else {
                            this.miniCEs[i] = '\u0001';
                            i++;
                        }
                        prevSecondary = s;
                    } else if (s < 1280) {
                        if (sec == 160) {
                            sec = 0;
                        } else if (sec < 128) {
                            sec += 32;
                        } else {
                            this.miniCEs[i] = '\u0001';
                            i++;
                        }
                    } else if (s == 1280) {
                        sec = 160;
                    } else if (sec < 192) {
                        sec = 192;
                    } else if (sec < 352) {
                        sec += 32;
                    } else {
                        this.miniCEs[i] = '\u0001';
                        i++;
                    }
                    prevSecondary = s;
                    ter = 0;
                }
                if (-assertionsDisabled || (Collation.CASE_MASK & lower32) == 0) {
                    if ((lower32 & Collation.ONLY_TERTIARY_MASK) > 1280) {
                        if (ter < 7) {
                            ter++;
                        } else {
                            this.miniCEs[i] = '\u0001';
                            i++;
                        }
                    }
                    if (3072 > pri || pri > 4088) {
                        this.miniCEs[i] = (char) ((pri | sec) | ter);
                        i++;
                    } else if (-assertionsDisabled || sec == 160) {
                        this.miniCEs[i] = (char) (pri | ter);
                        i++;
                    } else {
                        throw new AssertionError();
                    }
                }
                throw new AssertionError();
            }
            return;
        }
        throw new AssertionError();
    }

    private void encodeCharCEs() {
        int i;
        int miniCEsStart = this.result.length();
        for (i = 0; i < 448; i++) {
            this.result.append(0);
        }
        int indexBase = this.result.length();
        for (i = 0; i < 448; i++) {
            long ce = this.charCEs[i][0];
            if (!isContractionCharCE(ce)) {
                int miniCE = encodeTwoCEs(ce, this.charCEs[i][1]);
                if ((miniCE >>> 16) > 0) {
                    int expansionIndex = this.result.length() - indexBase;
                    if (expansionIndex > Opcodes.OP_NEW_INSTANCE_JUMBO) {
                        miniCE = 1;
                    } else {
                        this.result.append((char) (miniCE >> 16)).append((char) miniCE);
                        miniCE = expansionIndex | 2048;
                    }
                }
                this.result.setCharAt(miniCEsStart + i, (char) miniCE);
            }
        }
    }

    private void encodeContractions() {
        int indexBase = this.headerLength + 448;
        int firstContractionIndex = this.result.length();
        for (int i = 0; i < 448; i++) {
            long ce = this.charCEs[i][0];
            if (isContractionCharCE(ce)) {
                int contractionIndex = this.result.length() - indexBase;
                if (contractionIndex > Opcodes.OP_NEW_INSTANCE_JUMBO) {
                    this.result.setCharAt(this.headerLength + i, '\u0001');
                } else {
                    boolean firstTriple = true;
                    int index = ((int) ce) & Integer.MAX_VALUE;
                    while (true) {
                        long x = this.contractionCEs.elementAti(index);
                        if (x == 511 && (firstTriple ^ 1) != 0) {
                            break;
                        }
                        int miniCE = encodeTwoCEs(this.contractionCEs.elementAti(index + 1), this.contractionCEs.elementAti(index + 2));
                        if (miniCE == 1) {
                            this.result.append((char) ((int) (512 | x)));
                        } else if ((miniCE >>> 16) == 0) {
                            this.result.append((char) ((int) (1024 | x)));
                            this.result.append((char) miniCE);
                        } else {
                            this.result.append((char) ((int) (1536 | x)));
                            this.result.append((char) (miniCE >> 16)).append((char) miniCE);
                        }
                        firstTriple = false;
                        index += 3;
                    }
                    this.result.setCharAt(this.headerLength + i, (char) (contractionIndex | 1024));
                }
            }
        }
        if (this.result.length() > firstContractionIndex) {
            this.result.append('ǿ');
        }
    }

    private int encodeTwoCEs(long first, long second) {
        if (first == 0) {
            return 0;
        }
        if (first == Collation.NO_CE) {
            return 1;
        }
        if (-assertionsDisabled || (first >>> 32) != 1) {
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
        throw new AssertionError();
    }

    private static boolean isContractionCharCE(long ce) {
        return (ce >>> 32) == 1 && ce != Collation.NO_CE;
    }
}
