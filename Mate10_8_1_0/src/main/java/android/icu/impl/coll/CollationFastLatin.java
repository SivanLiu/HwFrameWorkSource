package android.icu.impl.coll;

import android.icu.text.DateTimePatternGenerator;
import dalvik.bytecode.Opcodes;

public final class CollationFastLatin {
    static final /* synthetic */ boolean -assertionsDisabled = (CollationFastLatin.class.desiredAssertionStatus() ^ 1);
    static final int BAIL_OUT = 1;
    public static final int BAIL_OUT_RESULT = -2;
    static final int CASE_AND_TERTIARY_MASK = 31;
    static final int CASE_MASK = 24;
    static final int COMMON_SEC = 160;
    static final int COMMON_SEC_PLUS_OFFSET = 192;
    static final int COMMON_TER = 0;
    static final int COMMON_TER_PLUS_OFFSET = 32;
    static final int CONTRACTION = 1024;
    static final int CONTR_CHAR_MASK = 511;
    static final int CONTR_LENGTH_SHIFT = 9;
    static final int EOS = 2;
    static final int EXPANSION = 2048;
    static final int INDEX_MASK = 1023;
    public static final int LATIN_LIMIT = 384;
    public static final int LATIN_MAX = 383;
    static final int LATIN_MAX_UTF8_LEAD = 197;
    static final int LONG_INC = 8;
    static final int LONG_PRIMARY_MASK = 65528;
    static final int LOWER_CASE = 8;
    static final int MAX_LONG = 4088;
    static final int MAX_SEC_AFTER = 352;
    static final int MAX_SEC_BEFORE = 128;
    static final int MAX_SEC_HIGH = 992;
    static final int MAX_SHORT = 64512;
    static final int MAX_TER_AFTER = 7;
    static final int MERGE_WEIGHT = 3;
    static final int MIN_LONG = 3072;
    static final int MIN_SEC_AFTER = 192;
    static final int MIN_SEC_BEFORE = 0;
    static final int MIN_SEC_HIGH = 384;
    static final int MIN_SHORT = 4096;
    static final int NUM_FAST_CHARS = 448;
    static final int PUNCT_LIMIT = 8256;
    static final int PUNCT_START = 8192;
    static final int SECONDARY_MASK = 992;
    static final int SEC_INC = 32;
    static final int SEC_OFFSET = 32;
    static final int SHORT_INC = 1024;
    static final int SHORT_PRIMARY_MASK = 64512;
    static final int TERTIARY_MASK = 7;
    static final int TER_OFFSET = 32;
    static final int TWO_CASES_MASK = 1572888;
    static final int TWO_COMMON_SEC_PLUS_OFFSET = 12583104;
    static final int TWO_COMMON_TER_PLUS_OFFSET = 2097184;
    static final int TWO_LONG_PRIMARIES_MASK = -458760;
    static final int TWO_LOWER_CASES = 524296;
    static final int TWO_SECONDARIES_MASK = 65012704;
    static final int TWO_SEC_OFFSETS = 2097184;
    static final int TWO_SHORT_PRIMARIES_MASK = -67044352;
    static final int TWO_TERTIARIES_MASK = 458759;
    static final int TWO_TER_OFFSETS = 2097184;
    public static final int VERSION = 2;

    public static int compareUTF16(char[] r1, char[] r2, int r3, java.lang.CharSequence r4, java.lang.CharSequence r5, int r6) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.icu.impl.coll.CollationFastLatin.compareUTF16(char[], char[], int, java.lang.CharSequence, java.lang.CharSequence, int):int
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-long
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.impl.coll.CollationFastLatin.compareUTF16(char[], char[], int, java.lang.CharSequence, java.lang.CharSequence, int):int");
    }

    private static long nextPair(char[] r1, int r2, int r3, java.lang.CharSequence r4, int r5) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.icu.impl.coll.CollationFastLatin.nextPair(char[], int, int, java.lang.CharSequence, int):long
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-long
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.impl.coll.CollationFastLatin.nextPair(char[], int, int, java.lang.CharSequence, int):long");
    }

    static int getCharIndex(char c) {
        if (c <= 'ſ') {
            return c;
        }
        if (' ' > c || c >= '⁀') {
            return -1;
        }
        return c - 7808;
    }

    public static int getOptions(CollationData data, CollationSettings settings, char[] primaries) {
        char[] header = data.fastLatinTableHeader;
        if (header == null) {
            return -1;
        }
        if (!-assertionsDisabled && (header[0] >> 8) != 2) {
            throw new AssertionError();
        } else if (primaries.length == 384) {
            int miniVarTop;
            int c;
            if ((settings.options & 12) == 0) {
                miniVarTop = Opcodes.OP_IGET_CHAR_JUMBO;
            } else {
                int i = settings.getMaxVariable() + 1;
                if (i >= (header[0] & 255)) {
                    return -1;
                }
                miniVarTop = header[i];
            }
            boolean digitsAreReordered = false;
            if (settings.hasReordering()) {
                long prevStart = 0;
                long beforeDigitStart = 0;
                long digitStart = 0;
                long afterDigitStart = 0;
                for (int group = 4096; group < 4104; group++) {
                    long start = settings.reorder(data.getFirstPrimaryForGroup(group));
                    if (group == 4100) {
                        beforeDigitStart = prevStart;
                        digitStart = start;
                    } else if (start == 0) {
                        continue;
                    } else if (start < prevStart) {
                        return -1;
                    } else {
                        if (digitStart != 0 && afterDigitStart == 0 && prevStart == beforeDigitStart) {
                            afterDigitStart = start;
                        }
                        prevStart = start;
                    }
                }
                long latinStart = settings.reorder(data.getFirstPrimaryForGroup(25));
                if (latinStart < prevStart) {
                    return -1;
                }
                if (afterDigitStart == 0) {
                    afterDigitStart = latinStart;
                }
                if (beforeDigitStart >= digitStart || digitStart >= afterDigitStart) {
                    digitsAreReordered = true;
                }
            }
            char[] table = data.fastLatinTable;
            for (c = 0; c < 384; c++) {
                int p = table[c];
                if (p >= 4096) {
                    p &= 64512;
                } else if (p > miniVarTop) {
                    p &= LONG_PRIMARY_MASK;
                } else {
                    p = 0;
                }
                primaries[c] = (char) p;
            }
            if (digitsAreReordered || (settings.options & 2) != 0) {
                for (c = 48; c <= 57; c++) {
                    primaries[c] = '\u0000';
                }
            }
            return (miniVarTop << 16) | settings.options;
        } else if (-assertionsDisabled) {
            return -1;
        } else {
            throw new AssertionError();
        }
    }

    private static int lookup(char[] table, int c) {
        if (!-assertionsDisabled && c <= LATIN_MAX) {
            throw new AssertionError();
        } else if (8192 <= c && c < PUNCT_LIMIT) {
            return table[(c - 8192) + 384];
        } else {
            if (c == 65534) {
                return 3;
            }
            if (c == DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                return 64680;
            }
            return 1;
        }
    }

    private static int getPrimaries(int variableTop, int pair) {
        int ce = pair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
        if (ce >= 4096) {
            return TWO_SHORT_PRIMARIES_MASK & pair;
        }
        if (ce > variableTop) {
            return TWO_LONG_PRIMARIES_MASK & pair;
        }
        if (ce >= MIN_LONG) {
            return 0;
        }
        return pair;
    }

    private static int getSecondariesFromOneShortCE(int ce) {
        ce &= 992;
        if (ce < 384) {
            return ce + 32;
        }
        return ((ce + 32) << 16) | 192;
    }

    private static int getSecondaries(int variableTop, int pair) {
        if (pair > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            int ce = pair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
            if (ce >= 4096) {
                return (TWO_SECONDARIES_MASK & pair) + 2097184;
            }
            if (ce > variableTop) {
                return TWO_COMMON_SEC_PLUS_OFFSET;
            }
            if (-assertionsDisabled || ce >= MIN_LONG) {
                return 0;
            }
            throw new AssertionError();
        } else if (pair >= 4096) {
            return getSecondariesFromOneShortCE(pair);
        } else {
            if (pair > variableTop) {
                return 192;
            }
            if (pair >= MIN_LONG) {
                return 0;
            }
            return pair;
        }
    }

    private static int getCases(int variableTop, boolean strengthIsPrimary, int pair) {
        int ce;
        if (pair > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            ce = pair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
            if (ce >= 4096) {
                if (strengthIsPrimary && (-67108864 & pair) == 0) {
                    return pair & 24;
                }
                return pair & TWO_CASES_MASK;
            } else if (ce > variableTop) {
                return TWO_LOWER_CASES;
            } else {
                if (-assertionsDisabled || ce >= MIN_LONG) {
                    return 0;
                }
                throw new AssertionError();
            }
        } else if (pair >= 4096) {
            ce = pair;
            pair &= 24;
            return (strengthIsPrimary || (ce & 992) < 384) ? pair : pair | 524288;
        } else if (pair > variableTop) {
            return 8;
        } else {
            if (pair >= MIN_LONG) {
                return 0;
            }
            return pair;
        }
    }

    private static int getTertiaries(int variableTop, boolean withCaseBits, int pair) {
        int ce;
        if (pair > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            ce = pair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
            if (ce >= 4096) {
                if (withCaseBits) {
                    pair &= 2031647;
                } else {
                    pair &= TWO_TERTIARIES_MASK;
                }
                return pair + 2097184;
            } else if (ce > variableTop) {
                pair = (pair & TWO_TERTIARIES_MASK) + 2097184;
                if (withCaseBits) {
                    return pair | TWO_LOWER_CASES;
                }
                return pair;
            } else if (-assertionsDisabled || ce >= MIN_LONG) {
                return 0;
            } else {
                throw new AssertionError();
            }
        } else if (pair >= 4096) {
            ce = pair;
            if (withCaseBits) {
                pair = (pair & 31) + 32;
                return (ce & 992) >= 384 ? pair | 2621440 : pair;
            } else {
                pair = (pair & 7) + 32;
                if ((ce & 992) >= 384) {
                    return pair | 2097152;
                }
                return pair;
            }
        } else if (pair > variableTop) {
            pair = (pair & 7) + 32;
            if (withCaseBits) {
                return pair | 8;
            }
            return pair;
        } else if (pair >= MIN_LONG) {
            return 0;
        } else {
            return pair;
        }
    }

    private static int getQuaternaries(int variableTop, int pair) {
        if (pair > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            int ce = pair & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
            if (ce > variableTop) {
                return TWO_SHORT_PRIMARIES_MASK;
            }
            if (-assertionsDisabled || ce >= MIN_LONG) {
                return pair & TWO_LONG_PRIMARIES_MASK;
            }
            throw new AssertionError();
        } else if (pair >= 4096) {
            if ((pair & 992) >= 384) {
                return TWO_SHORT_PRIMARIES_MASK;
            }
            return 64512;
        } else if (pair > variableTop) {
            return 64512;
        } else {
            if (pair >= MIN_LONG) {
                return pair & LONG_PRIMARY_MASK;
            }
            return pair;
        }
    }

    private CollationFastLatin() {
    }
}
