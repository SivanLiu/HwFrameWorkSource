package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2_32;
import android.icu.text.UnicodeSet;

public final class CollationData {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    static final int JAMO_CE32S_LENGTH = 67;
    static final int MAX_NUM_SPECIAL_REORDER_CODES = 8;
    static final int REORDER_RESERVED_AFTER_LATIN = 4111;
    static final int REORDER_RESERVED_BEFORE_LATIN = 4110;
    public CollationData base;
    int[] ce32s;
    long[] ces;
    public boolean[] compressibleBytes;
    String contexts;
    public char[] fastLatinTable;
    char[] fastLatinTableHeader;
    int[] jamoCE32s = new int[67];
    public Normalizer2Impl nfcImpl;
    int numScripts;
    long numericPrimary = 301989888;
    public long[] rootElements;
    char[] scriptStarts;
    char[] scriptsIndex;
    Trie2_32 trie;
    UnicodeSet unsafeBackwardSet;

    CollationData(Normalizer2Impl nfc) {
        this.nfcImpl = nfc;
    }

    public int getCE32(int c) {
        return this.trie.get(c);
    }

    int getCE32FromSupplementary(int c) {
        return this.trie.get(c);
    }

    boolean isDigit(int c) {
        if (c < 1632) {
            return c <= 57 && 48 <= c;
        } else {
            return Collation.hasCE32Tag(getCE32(c), 10);
        }
    }

    public boolean isUnsafeBackward(int c, boolean numeric) {
        return this.unsafeBackwardSet.contains(c) || (numeric && isDigit(c));
    }

    public boolean isCompressibleLeadByte(int b) {
        return this.compressibleBytes[b];
    }

    public boolean isCompressiblePrimary(long p) {
        return isCompressibleLeadByte(((int) p) >>> 24);
    }

    int getCE32FromContexts(int index) {
        return (this.contexts.charAt(index) << 16) | this.contexts.charAt(index + 1);
    }

    int getIndirectCE32(int ce32) {
        int tag = Collation.tagFromCE32(ce32);
        if (tag == 10) {
            return this.ce32s[Collation.indexFromCE32(ce32)];
        }
        if (tag == 13) {
            return -1;
        }
        if (tag == 11) {
            return this.ce32s[0];
        }
        return ce32;
    }

    int getFinalCE32(int ce32) {
        if (Collation.isSpecialCE32(ce32)) {
            return getIndirectCE32(ce32);
        }
        return ce32;
    }

    long getCEFromOffsetCE32(int c, int ce32) {
        return Collation.makeCE(Collation.getThreeBytePrimaryForOffsetData(c, this.ces[Collation.indexFromCE32(ce32)]));
    }

    long getSingleCE(int c) {
        CollationData d;
        int ce32 = getCE32(c);
        if (ce32 == 192) {
            d = this.base;
            ce32 = this.base.getCE32(c);
        } else {
            d = this;
        }
        while (Collation.isSpecialCE32(ce32)) {
            switch (Collation.tagFromCE32(ce32)) {
                case 0:
                case 3:
                    throw new AssertionError(String.format("unexpected CE32 tag for U+%04X (CE32 0x%08x)", new Object[]{Integer.valueOf(c), Integer.valueOf(ce32)}));
                case 1:
                    return Collation.ceFromLongPrimaryCE32(ce32);
                case 2:
                    return Collation.ceFromLongSecondaryCE32(ce32);
                case 4:
                case 7:
                case 8:
                case 9:
                case 12:
                case 13:
                    throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", new Object[]{Integer.valueOf(c), Integer.valueOf(ce32)}));
                case 5:
                    if (Collation.lengthFromCE32(ce32) == 1) {
                        ce32 = d.ce32s[Collation.indexFromCE32(ce32)];
                        break;
                    }
                    throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", new Object[]{Integer.valueOf(c), Integer.valueOf(ce32)}));
                case 6:
                    if (Collation.lengthFromCE32(ce32) == 1) {
                        return d.ces[Collation.indexFromCE32(ce32)];
                    }
                    throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", new Object[]{Integer.valueOf(c), Integer.valueOf(ce32)}));
                case 10:
                    ce32 = d.ce32s[Collation.indexFromCE32(ce32)];
                    break;
                case 11:
                    ce32 = d.ce32s[0];
                    break;
                case 14:
                    return d.getCEFromOffsetCE32(c, ce32);
                case 15:
                    return Collation.unassignedCEFromCodePoint(c);
                default:
                    break;
            }
        }
        return Collation.ceFromSimpleCE32(ce32);
    }

    int getFCD16(int c) {
        return this.nfcImpl.getFCD16(c);
    }

    long getFirstPrimaryForGroup(int script) {
        int index = getScriptIndex(script);
        return index == 0 ? 0 : ((long) this.scriptStarts[index]) << 16;
    }

    public long getLastPrimaryForGroup(int script) {
        int index = getScriptIndex(script);
        if (index == 0) {
            return 0;
        }
        return (((long) this.scriptStarts[index + 1]) << 16) - 1;
    }

    public int getGroupForPrimary(long p) {
        p >>= 16;
        char index = 1;
        if (p < ((long) this.scriptStarts[1]) || ((long) this.scriptStarts[this.scriptStarts.length - 1]) <= p) {
            return -1;
        }
        char index2;
        while (true) {
            index2 = index;
            if (p < ((long) this.scriptStarts[index2 + 1])) {
                break;
            }
            index = index2 + 1;
        }
        int i = 0;
        for (int i2 = 0; i2 < this.numScripts; i2++) {
            if (this.scriptsIndex[i2] == index2) {
                return i2;
            }
        }
        while (i < 8) {
            if (this.scriptsIndex[this.numScripts + i] == index2) {
                return 4096 + i;
            }
            i++;
        }
        return -1;
    }

    private int getScriptIndex(int script) {
        if (script < 0) {
            return 0;
        }
        if (script < this.numScripts) {
            return this.scriptsIndex[script];
        }
        if (script < 4096) {
            return 0;
        }
        script -= 4096;
        if (script < 8) {
            return this.scriptsIndex[this.numScripts + script];
        }
        return 0;
    }

    public int[] getEquivalentScripts(int script) {
        char index = getScriptIndex(script);
        if (index == 0) {
            return EMPTY_INT_ARRAY;
        }
        int i = 0;
        if (script >= 4096) {
            return new int[]{script};
        }
        int length = 0;
        for (int i2 = 0; i2 < this.numScripts; i2++) {
            if (this.scriptsIndex[i2] == index) {
                length++;
            }
        }
        int[] dest = new int[length];
        if (length == 1) {
            dest[0] = script;
            return dest;
        }
        int length2 = 0;
        while (i < this.numScripts) {
            if (this.scriptsIndex[i] == index) {
                length = length2 + 1;
                dest[length2] = i;
                length2 = length;
            }
            i++;
        }
        return dest;
    }

    void makeReorderRanges(int[] reorder, UVector32 ranges) {
        makeReorderRanges(reorder, false, ranges);
    }

    private void makeReorderRanges(int[] reorder, boolean latinMustMove, UVector32 ranges) {
        int[] iArr = reorder;
        UVector32 uVector32 = ranges;
        ranges.removeAllElements();
        int length;
        int i;
        if (length != 0) {
            int index = 103;
            int i2 = 1;
            if (length == 1 && iArr[0] == 103) {
                i = length;
            } else {
                int reorderCode;
                int start;
                int i3;
                short[] table = new short[(this.scriptStarts.length - 1)];
                int index2 = this.scriptsIndex[(this.numScripts + 4110) - 4096];
                if (index2 != 0) {
                    table[index2] = (short) 255;
                }
                index2 = this.scriptsIndex[(this.numScripts + 4111) - 4096];
                if (index2 != 0) {
                    table[index2] = (short) 255;
                }
                index2 = this.scriptStarts[1];
                int highLimit = this.scriptStarts[this.scriptStarts.length - 1];
                int specials = 0;
                for (int reorderCode2 : iArr) {
                    reorderCode2 = reorderCode2 - 4096;
                    if (reorderCode2 >= 0 && reorderCode2 < 8) {
                        specials |= 1 << reorderCode2;
                    }
                }
                int i4 = index2;
                index2 = 0;
                while (index2 < 8) {
                    reorderCode2 = this.scriptsIndex[this.numScripts + index2];
                    if (reorderCode2 != 0 && ((1 << index2) & specials) == 0) {
                        i4 = addLowScriptRange(table, reorderCode2, i4);
                    }
                    index2++;
                }
                index2 = 0;
                if (specials == 0 && iArr[0] == 25 && !latinMustMove) {
                    start = this.scriptStarts[this.scriptsIndex[25]];
                    index2 = start - i4;
                    i4 = start;
                }
                boolean hasReorderToEnd = false;
                int i5 = 0;
                while (true) {
                    start = i5;
                    if (start >= length) {
                        break;
                    }
                    i3 = start + 1;
                    start = iArr[start];
                    StringBuilder stringBuilder;
                    if (start == index) {
                        hasReorderToEnd = true;
                        while (i3 < length) {
                            length--;
                            start = iArr[length];
                            if (start == index) {
                                throw new IllegalArgumentException("setReorderCodes(): duplicate UScript.UNKNOWN");
                            } else if (start != -1) {
                                index = getScriptIndex(start);
                                if (index != 0) {
                                    if (table[index] == (short) 0) {
                                        highLimit = addHighScriptRange(table, index, highLimit);
                                    } else {
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("setReorderCodes(): duplicate or equivalent script ");
                                        stringBuilder.append(scriptCodeString(start));
                                        throw new IllegalArgumentException(stringBuilder.toString());
                                    }
                                }
                                index = 103;
                            } else {
                                throw new IllegalArgumentException("setReorderCodes(): UScript.DEFAULT together with other scripts");
                            }
                        }
                    } else if (start != -1) {
                        index = getScriptIndex(start);
                        if (index != 0) {
                            if (table[index] == (short) 0) {
                                i4 = addLowScriptRange(table, index, i4);
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("setReorderCodes(): duplicate or equivalent script ");
                                stringBuilder.append(scriptCodeString(start));
                                throw new IllegalArgumentException(stringBuilder.toString());
                            }
                        }
                        i5 = i3;
                        index = 103;
                    } else {
                        throw new IllegalArgumentException("setReorderCodes(): UScript.DEFAULT together with other scripts");
                    }
                }
                for (index = 1; index < this.scriptStarts.length - 1; index++) {
                    if (table[index] == 0) {
                        i3 = this.scriptStarts[index];
                        if (!hasReorderToEnd && i3 > i4) {
                            i4 = i3;
                        }
                        i4 = addLowScriptRange(table, index, i4);
                    }
                }
                if (i4 <= highLimit) {
                    int offset = 0;
                    index = 1;
                    while (true) {
                        i3 = index;
                        index = offset;
                        for (i2 = 
/*
Method generation error in method: android.icu.impl.coll.CollationData.makeReorderRanges(int[], boolean, android.icu.impl.coll.UVector32):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r6_4 'i2' int) = (r6_0 'i2' int), (r6_21 'i2' int) binds: {(r6_0 'i2' int)=B:87:0x0166, (r6_21 'i2' int)=B:105:0x01ad} in method: android.icu.impl.coll.CollationData.makeReorderRanges(int[], boolean, android.icu.impl.coll.UVector32):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:175)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:130)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 37 more

*/

    private int addLowScriptRange(short[] table, int index, int lowStart) {
        int start = this.scriptStarts[index];
        if ((start & 255) < (lowStart & 255)) {
            lowStart += 256;
        }
        table[index] = (short) (lowStart >> 8);
        int limit = this.scriptStarts[index + 1];
        return ((lowStart & 65280) + ((limit & 65280) - (65280 & start))) | (limit & 255);
    }

    private int addHighScriptRange(short[] table, int index, int highLimit) {
        int limit = this.scriptStarts[index + 1];
        if ((limit & 255) > (highLimit & 255)) {
            highLimit -= 256;
        }
        int start = this.scriptStarts[index];
        highLimit = ((highLimit & 65280) - ((limit & 65280) - (65280 & start))) | (start & 255);
        table[index] = (short) (highLimit >> 8);
        return highLimit;
    }

    private static String scriptCodeString(int script) {
        if (script < 4096) {
            return Integer.toString(script);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(script));
        return stringBuilder.toString();
    }
}
