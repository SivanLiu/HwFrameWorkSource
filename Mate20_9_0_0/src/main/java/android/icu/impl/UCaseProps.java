package android.icu.impl;

import android.icu.impl.ICUBinary.Authenticate;
import android.icu.impl.Trie2.Range;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Locale;

public final class UCaseProps {
    private static final int ABOVE = 64;
    private static final int CLOSURE_MAX_LENGTH = 15;
    private static final String DATA_FILE_NAME = "ucase.icu";
    private static final String DATA_NAME = "ucase";
    private static final String DATA_TYPE = "icu";
    private static final int DELTA_SHIFT = 7;
    private static final int DOT_MASK = 96;
    private static final int EXCEPTION = 16;
    private static final int EXC_CLOSURE = 6;
    private static final int EXC_CONDITIONAL_FOLD = 32768;
    private static final int EXC_CONDITIONAL_SPECIAL = 16384;
    private static final int EXC_DOT_SHIFT = 7;
    private static final int EXC_DOUBLE_SLOTS = 256;
    private static final int EXC_FOLD = 1;
    private static final int EXC_FULL_MAPPINGS = 7;
    private static final int EXC_LOWER = 0;
    private static final int EXC_SHIFT = 5;
    private static final int EXC_TITLE = 3;
    private static final int EXC_UPPER = 2;
    private static final int FMT = 1665225541;
    private static final int FOLD_CASE_OPTIONS_MASK = 7;
    private static final int FULL_LOWER = 15;
    static final int IGNORABLE = 4;
    public static final UCaseProps INSTANCE;
    private static final int IX_EXC_LENGTH = 3;
    private static final int IX_TOP = 16;
    private static final int IX_TRIE_SIZE = 2;
    private static final int IX_UNFOLD_LENGTH = 4;
    public static final int LOC_DUTCH = 5;
    static final int LOC_GREEK = 4;
    private static final int LOC_LITHUANIAN = 3;
    public static final int LOC_ROOT = 1;
    private static final int LOC_TURKISH = 2;
    public static final int LOWER = 1;
    public static final int MAX_STRING_LENGTH = 31;
    public static final int NONE = 0;
    private static final int OTHER_ACCENT = 96;
    private static final int SENSITIVE = 8;
    private static final int SOFT_DOTTED = 32;
    public static final int TITLE = 3;
    public static final int TYPE_MASK = 3;
    private static final int UNFOLD_ROWS = 0;
    private static final int UNFOLD_ROW_WIDTH = 1;
    private static final int UNFOLD_STRING_WIDTH = 2;
    public static final int UPPER = 2;
    public static final StringBuilder dummyStringBuilder = new StringBuilder();
    private static final byte[] flagsOffset = new byte[]{(byte) 0, (byte) 1, (byte) 1, (byte) 2, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 5, (byte) 6, (byte) 6, (byte) 7, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 5, (byte) 6, (byte) 6, (byte) 7, (byte) 2, (byte) 3, (byte) 3, (byte) 4, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 5, (byte) 6, (byte) 6, (byte) 7, (byte) 3, (byte) 4, (byte) 4, (byte) 5, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 5, (byte) 6, (byte) 6, (byte) 7, (byte) 4, (byte) 5, (byte) 5, (byte) 6, (byte) 5, (byte) 6, (byte) 6, (byte) 7, (byte) 5, (byte) 6, (byte) 6, (byte) 7, (byte) 6, (byte) 7, (byte) 7, (byte) 8};
    private static final String iDot = "i̇";
    private static final String iDotAcute = "i̇́";
    private static final String iDotGrave = "i̇̀";
    private static final String iDotTilde = "i̇̃";
    private static final String iOgonekDot = "į̇";
    private static final String jDot = "j̇";
    private String exceptions;
    private int[] indexes;
    private Trie2_16 trie;
    private char[] unfold;

    public interface ContextIterator {
        int next();

        void reset(int i);
    }

    private static final class IsAcceptable implements Authenticate {
        private IsAcceptable() {
        }

        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == (byte) 3;
        }
    }

    private UCaseProps() throws IOException {
        readData(ICUBinary.getRequiredData(DATA_FILE_NAME));
    }

    private final void readData(ByteBuffer bytes) throws IOException {
        ICUBinary.readHeader(bytes, FMT, new IsAcceptable());
        int count = bytes.getInt();
        if (count >= 16) {
            int i;
            this.indexes = new int[count];
            this.indexes[0] = count;
            for (i = 1; i < count; i++) {
                this.indexes[i] = bytes.getInt();
            }
            this.trie = Trie2_16.createFromSerialized(bytes);
            i = this.indexes[2];
            int trieLength = this.trie.getSerializedLength();
            if (trieLength <= i) {
                ICUBinary.skipBytes(bytes, i - trieLength);
                count = this.indexes[3];
                if (count > 0) {
                    this.exceptions = ICUBinary.getString(bytes, count, 0);
                }
                count = this.indexes[4];
                if (count > 0) {
                    this.unfold = ICUBinary.getChars(bytes, count, 0);
                    return;
                }
                return;
            }
            throw new IOException("ucase.icu: not enough bytes for the trie");
        }
        throw new IOException("indexes[0] too small in ucase.icu");
    }

    public final void addPropertyStarts(UnicodeSet set) {
        Iterator<Range> trieIterator = this.trie.iterator();
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

    private static final int getExceptionsOffset(int props) {
        return props >> 5;
    }

    private static final boolean propsHasException(int props) {
        return (props & 16) != 0;
    }

    static {
        try {
            INSTANCE = new UCaseProps();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static final boolean hasSlot(int flags, int index) {
        return ((1 << index) & flags) != 0;
    }

    private static final byte slotOffset(int flags, int index) {
        return flagsOffset[flags & ((1 << index) - 1)];
    }

    private final long getSlotValueAndOffset(int excWord, int index, int excOffset) {
        long value;
        if ((excWord & 256) == 0) {
            excOffset += slotOffset(excWord, index);
            value = (long) this.exceptions.charAt(excOffset);
        } else {
            excOffset += 2 * slotOffset(excWord, index);
            int excOffset2 = excOffset + 1;
            excOffset = excOffset2;
            value = (((long) this.exceptions.charAt(excOffset)) << 16) | ((long) this.exceptions.charAt(excOffset2));
        }
        return (((long) excOffset) << 32) | value;
    }

    private final int getSlotValue(int excWord, int index, int excOffset) {
        if ((excWord & 256) == 0) {
            return this.exceptions.charAt(excOffset + slotOffset(excWord, index));
        }
        excOffset += 2 * slotOffset(excWord, index);
        int excOffset2 = excOffset + 1;
        int value = (this.exceptions.charAt(excOffset) << 16) | this.exceptions.charAt(excOffset2);
        return value;
    }

    public final int tolower(int c) {
        int props = this.trie.get(c);
        if (propsHasException(props)) {
            int excWord = getExceptionsOffset(props);
            int excOffset = excWord + 1;
            excWord = this.exceptions.charAt(excWord);
            if (hasSlot(excWord, 0)) {
                return getSlotValue(excWord, 0, excOffset);
            }
            return c;
        } else if (getTypeFromProps(props) >= 2) {
            return c + getDelta(props);
        } else {
            return c;
        }
    }

    public final int toupper(int c) {
        int props = this.trie.get(c);
        if (propsHasException(props)) {
            int excWord = getExceptionsOffset(props);
            int excOffset = excWord + 1;
            excWord = this.exceptions.charAt(excWord);
            if (hasSlot(excWord, 2)) {
                return getSlotValue(excWord, 2, excOffset);
            }
            return c;
        } else if (getTypeFromProps(props) == 1) {
            return c + getDelta(props);
        } else {
            return c;
        }
    }

    public final int totitle(int c) {
        int props = this.trie.get(c);
        if (propsHasException(props)) {
            int index;
            int excWord = getExceptionsOffset(props);
            int excOffset = excWord + 1;
            excWord = this.exceptions.charAt(excWord);
            if (hasSlot(excWord, 3)) {
                index = 3;
            } else if (!hasSlot(excWord, 2)) {
                return c;
            } else {
                index = 2;
            }
            c = getSlotValue(excWord, index, excOffset);
        } else if (getTypeFromProps(props) == 1) {
            c += getDelta(props);
        }
        return c;
    }

    public final void addCaseClosure(int c, UnicodeSet set) {
        int i = c;
        UnicodeSet unicodeSet = set;
        if (i == 73) {
            unicodeSet.add(105);
        } else if (i != 105) {
            switch (i) {
                case 304:
                    unicodeSet.add(iDot);
                    return;
                case 305:
                    return;
                default:
                    int props = this.trie.get(i);
                    int excWord;
                    if (propsHasException(props)) {
                        int closureLength;
                        excWord = getExceptionsOffset(props);
                        int excOffset = excWord + 1;
                        excWord = this.exceptions.charAt(excWord);
                        int excOffset0 = excOffset;
                        int closureOffset = 0;
                        int c2 = i;
                        for (i = 0; i <= 3; i++) {
                            if (hasSlot(excWord, i)) {
                                excOffset = excOffset0;
                                c2 = getSlotValue(excWord, i, excOffset);
                                unicodeSet.add(c2);
                            }
                        }
                        if (hasSlot(excWord, 6)) {
                            excOffset = excOffset0;
                            long value = getSlotValueAndOffset(excWord, 6, excOffset);
                            int i2 = ((int) (value >> 32)) + 1;
                            closureLength = ((int) value) & 15;
                            closureOffset = i2;
                        } else {
                            closureLength = 0;
                        }
                        if (hasSlot(excWord, 7)) {
                            long value2 = getSlotValueAndOffset(excWord, 7, excOffset0);
                            excOffset = DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH & ((int) value2);
                            int excOffset2 = (((int) (value2 >> 32)) + 1) + (excOffset & 15);
                            excOffset >>= 4;
                            int length = excOffset & 15;
                            if (length != 0) {
                                unicodeSet.add(this.exceptions.substring(excOffset2, excOffset2 + length));
                                excOffset2 += length;
                            }
                            excOffset >>= 4;
                            closureOffset = (excOffset2 + (excOffset & 15)) + (excOffset >> 4);
                        }
                        excOffset = closureOffset + closureLength;
                        i = closureOffset;
                        while (i < excOffset) {
                            c2 = this.exceptions.codePointAt(i);
                            unicodeSet.add(c2);
                            i += UTF16.getCharCount(c2);
                        }
                    } else if (getTypeFromProps(props) != 0) {
                        excWord = getDelta(props);
                        if (excWord != 0) {
                            unicodeSet.add(i + excWord);
                        }
                    }
                    return;
            }
        } else {
            unicodeSet.add(73);
        }
    }

    private final int strcmpMax(String s, int unfoldOffset, int max) {
        int length = s.length();
        max -= length;
        int length2 = length;
        length = unfoldOffset;
        unfoldOffset = 0;
        while (true) {
            int i1 = unfoldOffset + 1;
            unfoldOffset = s.charAt(unfoldOffset);
            int unfoldOffset2 = length + 1;
            length = this.unfold[length];
            if (length == 0) {
                return 1;
            }
            unfoldOffset -= length;
            if (unfoldOffset != 0) {
                return unfoldOffset;
            }
            length2--;
            if (length2 > 0) {
                unfoldOffset = i1;
                length = unfoldOffset2;
            } else if (max == 0 || this.unfold[unfoldOffset2] == 0) {
                return 0;
            } else {
                return -max;
            }
        }
    }

    public final boolean addStringCaseClosure(String s, UnicodeSet set) {
        if (this.unfold == null || s == null) {
            return false;
        }
        int length = s.length();
        if (length <= 1) {
            return false;
        }
        int unfoldRows = this.unfold[0];
        int unfoldRowWidth = this.unfold[1];
        int unfoldStringWidth = this.unfold[2];
        if (length > unfoldStringWidth) {
            return false;
        }
        int start = 0;
        int limit = unfoldRows;
        while (start < limit) {
            int i = (start + limit) / 2;
            int unfoldOffset = (i + 1) * unfoldRowWidth;
            int result = strcmpMax(s, unfoldOffset, unfoldStringWidth);
            if (result == 0) {
                int i2 = unfoldStringWidth;
                while (i2 < unfoldRowWidth && this.unfold[unfoldOffset + i2] != 0) {
                    int c = UTF16.charAt(this.unfold, unfoldOffset, this.unfold.length, i2);
                    set.add(c);
                    addCaseClosure(c, set);
                    i2 += UTF16.getCharCount(c);
                }
                return true;
            } else if (result < 0) {
                limit = i;
            } else {
                start = i + 1;
            }
        }
        return false;
    }

    public final int getType(int c) {
        return getTypeFromProps(this.trie.get(c));
    }

    public final int getTypeOrIgnorable(int c) {
        return getTypeAndIgnorableFromProps(this.trie.get(c));
    }

    public final int getDotType(int c) {
        int props = this.trie.get(c);
        if (propsHasException(props)) {
            return (this.exceptions.charAt(getExceptionsOffset(props)) >> 7) & 96;
        }
        return props & 96;
    }

    public final boolean isSoftDotted(int c) {
        return getDotType(c) == 32;
    }

    public final boolean isCaseSensitive(int c) {
        return (this.trie.get(c) & 8) != 0;
    }

    public static final int getCaseLocale(Locale locale) {
        return getCaseLocale(locale.getLanguage());
    }

    public static final int getCaseLocale(ULocale locale) {
        return getCaseLocale(locale.getLanguage());
    }

    private static final int getCaseLocale(String language) {
        if (language.length() == 2) {
            if (language.equals("en") || language.charAt(0) > 't') {
                return 1;
            }
            if (language.equals("tr") || language.equals("az")) {
                return 2;
            }
            if (language.equals("el")) {
                return 4;
            }
            if (language.equals("lt")) {
                return 3;
            }
            if (language.equals("nl")) {
                return 5;
            }
        } else if (language.length() == 3) {
            if (language.equals("tur") || language.equals("aze")) {
                return 2;
            }
            if (language.equals("ell")) {
                return 4;
            }
            if (language.equals("lit")) {
                return 3;
            }
            if (language.equals("nld")) {
                return 5;
            }
        }
        return 1;
    }

    private final boolean isFollowedByCasedLetter(ContextIterator iter, int dir) {
        if (iter == null) {
            return false;
        }
        iter.reset(dir);
        while (true) {
            int next = iter.next();
            int c = next;
            if (next < 0) {
                return false;
            }
            next = getTypeOrIgnorable(c);
            if ((next & 4) == 0) {
                if (next != 0) {
                    return true;
                }
                return false;
            }
        }
    }

    private final boolean isPrecededBySoftDotted(ContextIterator iter) {
        if (iter == null) {
            return false;
        }
        iter.reset(-1);
        int dotType;
        do {
            dotType = iter.next();
            int c = dotType;
            if (dotType < 0) {
                return false;
            }
            dotType = getDotType(c);
            if (dotType == 32) {
                return true;
            }
        } while (dotType == 96);
        return false;
    }

    private final boolean isPrecededBy_I(ContextIterator iter) {
        if (iter == null) {
            return false;
        }
        iter.reset(-1);
        int c;
        do {
            int next = iter.next();
            c = next;
            if (next < 0) {
                return false;
            }
            if (c == 73) {
                return true;
            }
        } while (getDotType(c) == 96);
        return false;
    }

    private final boolean isFollowedByMoreAbove(ContextIterator iter) {
        if (iter == null) {
            return false;
        }
        iter.reset(1);
        int dotType;
        do {
            dotType = iter.next();
            int c = dotType;
            if (dotType < 0) {
                return false;
            }
            dotType = getDotType(c);
            if (dotType == 64) {
                return true;
            }
        } while (dotType == 96);
        return false;
    }

    private final boolean isFollowedByDotAbove(ContextIterator iter) {
        if (iter == null) {
            return false;
        }
        iter.reset(1);
        int c;
        do {
            int next = iter.next();
            c = next;
            if (next < 0) {
                return false;
            }
            if (c == 775) {
                return true;
            }
        } while (getDotType(c) == 96);
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:92:0x0111  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x010f  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x010f  */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x0111  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final int toFullLower(int c, ContextIterator iter, Appendable out, int caseLocale) {
        int result;
        int i = c;
        ContextIterator contextIterator = iter;
        Appendable appendable = out;
        int i2 = caseLocale;
        int result2 = i;
        int props = this.trie.get(i);
        if (propsHasException(props)) {
            int excOffset = getExceptionsOffset(props);
            int excOffset2 = excOffset + 1;
            int excWord = this.exceptions.charAt(excOffset);
            int excOffset22 = excOffset2;
            if ((excWord & 16384) == 0) {
                if (hasSlot(excWord, 7)) {
                    long value = getSlotValueAndOffset(excWord, 7, excOffset2);
                    int full = ((int) value) & 15;
                    if (full != 0) {
                        i2 = ((int) (value >> 32)) + 1;
                        try {
                            appendable.append(this.exceptions, i2, i2 + full);
                            return full;
                        } catch (IOException e) {
                            throw new ICUUncheckedIOException(e);
                        }
                    }
                }
                result = result2;
            } else if (i2 == 3 && (((i == 73 || i == 74 || i == 302) && isFollowedByMoreAbove(contextIterator)) || i == 204 || i == 205 || i == 296)) {
                switch (i) {
                    case 73:
                        appendable.append(iDot);
                        return 2;
                    case 74:
                        appendable.append(jDot);
                        return 2;
                    case 204:
                        appendable.append(iDotGrave);
                        return 3;
                    case 205:
                        appendable.append(iDotAcute);
                        return 3;
                    case 296:
                        appendable.append(iDotTilde);
                        return 3;
                    case 302:
                        try {
                            appendable.append(iOgonekDot);
                            return 2;
                        } catch (IOException e2) {
                            throw new ICUUncheckedIOException(e2);
                        }
                    default:
                        return 0;
                }
            } else if (i2 == 2 && i == 304) {
                return 105;
            } else {
                if (i2 == 2 && i == 775 && isPrecededBy_I(contextIterator)) {
                    return 0;
                }
                if (i2 == 2 && i == 73 && !isFollowedByDotAbove(contextIterator)) {
                    return 305;
                }
                if (i == 304) {
                    try {
                        appendable.append(iDot);
                        return 2;
                    } catch (IOException e22) {
                        throw new ICUUncheckedIOException(e22);
                    }
                } else if (i == 931 && !isFollowedByCasedLetter(contextIterator, 1) && isFollowedByCasedLetter(contextIterator, -1)) {
                    return 962;
                } else {
                    result = result2;
                }
            }
            if (hasSlot(excWord, 0)) {
                result2 = getSlotValue(excWord, 0, excOffset22);
                if (result2 == i) {
                }
                return result2 == i ? ~result2 : result2;
            }
        } else if (getTypeFromProps(props) >= 2) {
            result2 = i + getDelta(props);
            return result2 == i ? ~result2 : result2;
        } else {
            result = result2;
        }
        result2 = result;
        if (result2 == i) {
        }
        return result2 == i ? ~result2 : result2;
    }

    private final int toUpperOrTitle(int c, ContextIterator iter, Appendable out, int loc, boolean upperNotTitle) {
        Throwable e;
        Appendable appendable;
        int i = c;
        int i2 = loc;
        int result = i;
        int props = this.trie.get(i);
        if (propsHasException(props)) {
            int excOffset = getExceptionsOffset(props);
            int excOffset2 = excOffset + 1;
            int excWord = this.exceptions.charAt(excOffset);
            int excOffset22 = excOffset2;
            if ((excWord & 16384) == 0) {
                ContextIterator contextIterator = iter;
                if (hasSlot(excWord, 7)) {
                    long value = getSlotValueAndOffset(excWord, 7, excOffset2);
                    excOffset = ((int) value) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    excOffset >>= 4;
                    int excOffset3 = ((((int) (value >> 32)) + 1) + (excOffset & 15)) + (excOffset & 15);
                    excOffset >>= 4;
                    if (upperNotTitle) {
                        excOffset &= 15;
                    } else {
                        excOffset3 += excOffset & 15;
                        excOffset = (excOffset >> 4) & 15;
                    }
                    int full = excOffset;
                    excOffset2 = excOffset3;
                    if (full != 0) {
                        try {
                            try {
                                out.append(this.exceptions, excOffset2, excOffset2 + full);
                                return full;
                            } catch (IOException e2) {
                                e = e2;
                                throw new ICUUncheckedIOException(e);
                            }
                        } catch (IOException e3) {
                            e = e3;
                            appendable = out;
                            throw new ICUUncheckedIOException(e);
                        }
                    }
                }
            } else if (i2 == 2 && i == 105) {
                return 304;
            } else {
                if (i2 == 3 && i == 775 && isPrecededBySoftDotted(iter)) {
                    return 0;
                }
            }
            appendable = out;
            if (!upperNotTitle && hasSlot(excWord, 3)) {
                excOffset = 3;
            } else if (!hasSlot(excWord, 2)) {
                return ~i;
            } else {
                excOffset = 2;
            }
            result = getSlotValue(excWord, excOffset, excOffset22);
        } else {
            if (getTypeFromProps(props) == 1) {
                result = i + getDelta(props);
            }
            appendable = out;
        }
        return result == i ? ~result : result;
    }

    public final int toFullUpper(int c, ContextIterator iter, Appendable out, int caseLocale) {
        return toUpperOrTitle(c, iter, out, caseLocale, true);
    }

    public final int toFullTitle(int c, ContextIterator iter, Appendable out, int caseLocale) {
        return toUpperOrTitle(c, iter, out, caseLocale, false);
    }

    public final int fold(int c, int options) {
        int props = this.trie.get(c);
        if (propsHasException(props)) {
            int index;
            int excWord = getExceptionsOffset(props);
            int excOffset = excWord + 1;
            excWord = this.exceptions.charAt(excWord);
            if ((32768 & excWord) != 0) {
                if ((options & 7) == 0) {
                    if (c == 73) {
                        return 105;
                    }
                    if (c == 304) {
                        return c;
                    }
                } else if (c == 73) {
                    return 305;
                } else {
                    if (c == 304) {
                        return 105;
                    }
                }
            }
            if (hasSlot(excWord, 1)) {
                index = 1;
            } else if (!hasSlot(excWord, 0)) {
                return c;
            } else {
                index = 0;
            }
            c = getSlotValue(excWord, index, excOffset);
        } else if (getTypeFromProps(props) >= 2) {
            c += getDelta(props);
        }
        return c;
    }

    public final int toFullFolding(int c, Appendable out, int options) {
        int result = c;
        int props = this.trie.get(c);
        if (propsHasException(props)) {
            int full;
            int excWord = getExceptionsOffset(props);
            int excOffset = excWord + 1;
            excWord = this.exceptions.charAt(excWord);
            int excOffset2 = excOffset;
            if ((32768 & excWord) != 0) {
                if ((options & 7) == 0) {
                    if (c == 73) {
                        return 105;
                    }
                    if (c == 304) {
                        try {
                            out.append(iDot);
                            return 2;
                        } catch (IOException e) {
                            throw new ICUUncheckedIOException(e);
                        }
                    }
                } else if (c == 73) {
                    return 305;
                } else {
                    if (c == 304) {
                        return 105;
                    }
                }
            } else if (hasSlot(excWord, 7)) {
                long value = getSlotValueAndOffset(excWord, 7, excOffset);
                full = ((int) value) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                excOffset = (full & 15) + (((int) (value >> 32)) + 1);
                full = (full >> 4) & 15;
                if (full != 0) {
                    try {
                        out.append(this.exceptions, excOffset, excOffset + full);
                        return full;
                    } catch (IOException e2) {
                        throw new ICUUncheckedIOException(e2);
                    }
                }
            }
            if (hasSlot(excWord, 1)) {
                full = 1;
            } else if (!hasSlot(excWord, 0)) {
                return ~c;
            } else {
                full = 0;
            }
            result = getSlotValue(excWord, full, excOffset2);
        } else if (getTypeFromProps(props) >= 2) {
            result = c + getDelta(props);
        }
        return result == c ? ~result : result;
    }

    public final boolean hasBinaryProperty(int c, int which) {
        boolean z = false;
        if (which == 22) {
            if (1 == getType(c)) {
                z = true;
            }
            return z;
        } else if (which == 27) {
            return isSoftDotted(c);
        } else {
            if (which == 30) {
                if (2 == getType(c)) {
                    z = true;
                }
                return z;
            } else if (which == 34) {
                return isCaseSensitive(c);
            } else {
                if (which != 55) {
                    switch (which) {
                        case 49:
                            if (getType(c) != 0) {
                                z = true;
                            }
                            return z;
                        case 50:
                            if ((getTypeOrIgnorable(c) >> 2) != 0) {
                                z = true;
                            }
                            return z;
                        case 51:
                            dummyStringBuilder.setLength(0);
                            if (toFullLower(c, null, dummyStringBuilder, 1) >= 0) {
                                z = true;
                            }
                            return z;
                        case 52:
                            dummyStringBuilder.setLength(0);
                            if (toFullUpper(c, null, dummyStringBuilder, 1) >= 0) {
                                z = true;
                            }
                            return z;
                        case 53:
                            dummyStringBuilder.setLength(0);
                            if (toFullTitle(c, null, dummyStringBuilder, 1) >= 0) {
                                z = true;
                            }
                            return z;
                        default:
                            return false;
                    }
                }
                dummyStringBuilder.setLength(0);
                if (toFullLower(c, null, dummyStringBuilder, 1) >= 0 || toFullUpper(c, null, dummyStringBuilder, 1) >= 0 || toFullTitle(c, null, dummyStringBuilder, 1) >= 0) {
                    z = true;
                }
                return z;
            }
        }
    }

    private static final int getTypeFromProps(int props) {
        return props & 3;
    }

    private static final int getTypeAndIgnorableFromProps(int props) {
        return props & 7;
    }

    private static final int getDelta(int props) {
        return ((short) props) >> 7;
    }
}
