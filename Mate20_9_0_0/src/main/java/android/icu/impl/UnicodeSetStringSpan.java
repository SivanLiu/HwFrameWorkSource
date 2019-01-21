package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.text.UnicodeSet.SpanCondition;
import android.icu.util.OutputInt;
import java.util.ArrayList;

public class UnicodeSetStringSpan {
    public static final int ALL = 127;
    static final short ALL_CP_CONTAINED = (short) 255;
    public static final int BACK = 16;
    public static final int BACK_UTF16_CONTAINED = 18;
    public static final int BACK_UTF16_NOT_CONTAINED = 17;
    public static final int CONTAINED = 2;
    public static final int FWD = 32;
    public static final int FWD_UTF16_CONTAINED = 34;
    public static final int FWD_UTF16_NOT_CONTAINED = 33;
    static final short LONG_SPAN = (short) 254;
    public static final int NOT_CONTAINED = 1;
    public static final int WITH_COUNT = 64;
    private boolean all;
    private final int maxLength16;
    private OffsetList offsets;
    private boolean someRelevant;
    private short[] spanLengths;
    private UnicodeSet spanNotSet;
    private UnicodeSet spanSet;
    private ArrayList<String> strings;

    private static final class OffsetList {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private int length;
        private int[] list = new int[16];
        private int start;

        static {
            Class cls = UnicodeSetStringSpan.class;
        }

        public void setMaxLength(int maxLength) {
            if (maxLength > this.list.length) {
                this.list = new int[maxLength];
            }
            clear();
        }

        public void clear() {
            int i = this.list.length;
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    this.list[i2] = 0;
                    i = i2;
                } else {
                    this.length = 0;
                    this.start = 0;
                    return;
                }
            }
        }

        public boolean isEmpty() {
            return this.length == 0;
        }

        public void shift(int delta) {
            int i = this.start + delta;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            if (this.list[i] != 0) {
                this.list[i] = 0;
                this.length--;
            }
            this.start = i;
        }

        public void addOffset(int offset) {
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            this.list[i] = 1;
            this.length++;
        }

        public void addOffsetAndCount(int offset, int count) {
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            if (this.list[i] == 0) {
                this.list[i] = count;
                this.length++;
            } else if (count < this.list[i]) {
                this.list[i] = count;
            }
        }

        public boolean containsOffset(int offset) {
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            return this.list[i] != 0;
        }

        public boolean hasCountAtOffset(int offset, int count) {
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            int oldCount = this.list[i];
            return oldCount != 0 && oldCount <= count;
        }

        public int popMinimum(OutputInt outCount) {
            int i = this.start;
            while (true) {
                i++;
                int count;
                if (i < this.list.length) {
                    count = this.list[i];
                    if (count != 0) {
                        this.list[i] = 0;
                        this.length--;
                        int result = i - this.start;
                        this.start = i;
                        if (outCount != null) {
                            outCount.value = count;
                        }
                        return result;
                    }
                } else {
                    int count2;
                    count = this.list.length - this.start;
                    i = 0;
                    while (true) {
                        int i2 = this.list[i];
                        count2 = i2;
                        if (i2 != 0) {
                            break;
                        }
                        i++;
                    }
                    this.list[i] = 0;
                    this.length--;
                    this.start = i;
                    if (outCount != null) {
                        outCount.value = count2;
                    }
                    return count + i;
                }
            }
        }
    }

    public UnicodeSetStringSpan(UnicodeSet set, ArrayList<String> setStrings, int which) {
        int i;
        int i2 = which;
        this.spanSet = new UnicodeSet(0, 1114111);
        this.strings = setStrings;
        this.all = i2 == 127;
        this.spanSet.retainAll(set);
        if ((i2 & 1) != 0) {
            this.spanNotSet = this.spanSet;
        }
        this.offsets = new OffsetList();
        int stringsLength = this.strings.size();
        this.someRelevant = false;
        int maxLength16 = 0;
        for (i = 0; i < stringsLength; i++) {
            String string = (String) this.strings.get(i);
            int length16 = string.length();
            if (this.spanSet.span(string, SpanCondition.CONTAINED) < length16) {
                this.someRelevant = true;
            }
            if (length16 > maxLength16) {
                maxLength16 = length16;
            }
        }
        this.maxLength16 = maxLength16;
        if (this.someRelevant || (i2 & 64) != 0) {
            int allocSize;
            int spanBackLengthsOffset;
            if (this.all) {
                this.spanSet.freeze();
            }
            if (this.all) {
                allocSize = stringsLength * 2;
            } else {
                allocSize = stringsLength;
            }
            this.spanLengths = new short[allocSize];
            if (this.all) {
                spanBackLengthsOffset = stringsLength;
            } else {
                spanBackLengthsOffset = 0;
            }
            for (i = 0; i < stringsLength; i++) {
                String string2 = (String) this.strings.get(i);
                int length162 = string2.length();
                int spanLength = this.spanSet.span(string2, SpanCondition.CONTAINED);
                short[] sArr;
                if (spanLength < length162) {
                    if ((i2 & 2) != 0) {
                        if ((i2 & 32) != 0) {
                            this.spanLengths[i] = makeSpanLengthByte(spanLength);
                        }
                        if ((i2 & 16) != 0) {
                            this.spanLengths[spanBackLengthsOffset + i] = makeSpanLengthByte(length162 - this.spanSet.spanBack(string2, length162, SpanCondition.CONTAINED));
                        }
                    } else {
                        sArr = this.spanLengths;
                        this.spanLengths[spanBackLengthsOffset + i] = (short) 0;
                        sArr[i] = (short) 0;
                    }
                    if ((i2 & 1) != 0) {
                        if ((i2 & 32) != 0) {
                            addToSpanNotSet(string2.codePointAt(0));
                        }
                        if ((i2 & 16) != 0) {
                            addToSpanNotSet(string2.codePointBefore(length162));
                        }
                    }
                } else if (this.all) {
                    sArr = this.spanLengths;
                    this.spanLengths[spanBackLengthsOffset + i] = ALL_CP_CONTAINED;
                    sArr[i] = ALL_CP_CONTAINED;
                } else {
                    this.spanLengths[i] = ALL_CP_CONTAINED;
                }
            }
            if (this.all) {
                this.spanNotSet.freeze();
            }
        }
    }

    public UnicodeSetStringSpan(UnicodeSetStringSpan otherStringSpan, ArrayList<String> newParentSetStrings) {
        this.spanSet = otherStringSpan.spanSet;
        this.strings = newParentSetStrings;
        this.maxLength16 = otherStringSpan.maxLength16;
        this.someRelevant = otherStringSpan.someRelevant;
        this.all = true;
        if (Utility.sameObjects(otherStringSpan.spanNotSet, otherStringSpan.spanSet)) {
            this.spanNotSet = this.spanSet;
        } else {
            this.spanNotSet = (UnicodeSet) otherStringSpan.spanNotSet.clone();
        }
        this.offsets = new OffsetList();
        this.spanLengths = (short[]) otherStringSpan.spanLengths.clone();
    }

    public boolean needsStringSpanUTF16() {
        return this.someRelevant;
    }

    public boolean contains(int c) {
        return this.spanSet.contains(c);
    }

    private void addToSpanNotSet(int c) {
        if (Utility.sameObjects(this.spanNotSet, null) || Utility.sameObjects(this.spanNotSet, this.spanSet)) {
            if (!this.spanSet.contains(c)) {
                this.spanNotSet = this.spanSet.cloneAsThawed();
            } else {
                return;
            }
        }
        this.spanNotSet.add(c);
    }

    public int span(CharSequence s, int start, SpanCondition spanCondition) {
        if (spanCondition == SpanCondition.NOT_CONTAINED) {
            return spanNot(s, start, null);
        }
        int spanLimit = this.spanSet.span(s, start, SpanCondition.CONTAINED);
        if (spanLimit == s.length()) {
            return spanLimit;
        }
        return spanWithStrings(s, start, spanLimit, spanCondition);
    }

    /* JADX WARNING: Missing block: B:78:0x010b, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized int spanWithStrings(CharSequence s, int start, int spanLimit, SpanCondition spanCondition) {
        CharSequence charSequence = s;
        SpanCondition spanCondition2 = spanCondition;
        synchronized (this) {
            int initSize = 0;
            if (spanCondition2 == SpanCondition.CONTAINED) {
                initSize = this.maxLength16;
            }
            this.offsets.setMaxLength(initSize);
            int length = s.length();
            int pos = spanLimit;
            int rest = length - spanLimit;
            int spanLength = spanLimit - start;
            int stringsLength = this.strings.size();
            int spanLimit2 = spanLimit;
            while (true) {
                int spanLimit3;
                int initSize2;
                int i = 254;
                int i2 = 0;
                int i3;
                String string;
                int length16;
                if (spanCondition2 == SpanCondition.CONTAINED) {
                    while (true) {
                        i3 = i2;
                        if (i3 >= stringsLength) {
                            spanLimit3 = spanLimit2;
                            initSize2 = initSize;
                            break;
                        }
                        i2 = this.spanLengths[i3];
                        if (i2 != 255) {
                            string = (String) this.strings.get(i3);
                            length16 = string.length();
                            if (i2 >= i) {
                                i2 = string.offsetByCodePoints(length16, -1);
                            }
                            if (i2 > spanLength) {
                                i2 = spanLength;
                            }
                            i = length16 - i2;
                            while (i <= rest) {
                                spanLimit3 = spanLimit2;
                                if (!this.offsets.containsOffset(i) && matches16CPB(charSequence, pos - i2, length, string, length16)) {
                                    if (i == rest) {
                                        return length;
                                    }
                                    this.offsets.addOffset(i);
                                }
                                if (i2 == 0) {
                                    break;
                                }
                                i2--;
                                i++;
                                spanLimit2 = spanLimit3;
                            }
                        }
                        spanLimit3 = spanLimit2;
                        i2 = i3 + 1;
                        spanLimit2 = spanLimit3;
                        i = 254;
                    }
                } else {
                    spanLimit3 = spanLimit2;
                    spanLimit2 = 0;
                    i3 = 0;
                    while (true) {
                        i = i2;
                        if (i >= stringsLength) {
                            break;
                        }
                        i2 = this.spanLengths[i];
                        string = (String) this.strings.get(i);
                        length16 = string.length();
                        if (i2 >= 254) {
                            i2 = length16;
                        }
                        if (i2 > spanLength) {
                            i2 = spanLength;
                        }
                        int inc = length16 - i2;
                        while (true) {
                            int inc2 = inc;
                            if (inc2 > rest) {
                                initSize2 = initSize;
                                break;
                            } else if (i2 < i3) {
                                initSize2 = initSize;
                                break;
                            } else {
                                if (i2 <= i3) {
                                    if (inc2 <= spanLimit2) {
                                        initSize2 = initSize;
                                        i2--;
                                        inc = inc2 + 1;
                                        initSize = initSize2;
                                    }
                                }
                                initSize2 = initSize;
                                if (matches16CPB(charSequence, pos - i2, length, string, length16) != 0) {
                                    spanLimit2 = inc2;
                                    i3 = i2;
                                    break;
                                }
                                i2--;
                                inc = inc2 + 1;
                                initSize = initSize2;
                            }
                        }
                        i2 = i + 1;
                        initSize = initSize2;
                        spanCondition2 = spanCondition;
                    }
                    initSize2 = initSize;
                    if (spanLimit2 == 0) {
                        if (i3 == 0) {
                            i3 = i;
                        }
                    }
                    pos += spanLimit2;
                    rest -= spanLimit2;
                    if (rest == 0) {
                        return length;
                    }
                    spanLength = 0;
                    spanLimit2 = spanLimit3;
                    initSize = initSize2;
                    spanCondition2 = spanCondition;
                }
                if (spanLength == 0) {
                    if (pos != 0) {
                        if (this.offsets.isEmpty() != 0) {
                            spanLimit2 = this.spanSet.span(charSequence, pos, SpanCondition.CONTAINED);
                            spanLength = spanLimit2 - pos;
                            if (spanLength == rest || spanLength == 0) {
                            } else {
                                pos += spanLength;
                                rest -= spanLength;
                                initSize = initSize2;
                                spanCondition2 = spanCondition;
                            }
                        } else {
                            spanLength = spanOne(this.spanSet, charSequence, pos, rest);
                            if (spanLength > 0) {
                                if (spanLength == rest) {
                                    return length;
                                }
                                pos += spanLength;
                                rest -= spanLength;
                                this.offsets.shift(spanLength);
                                spanLength = 0;
                                spanLimit2 = spanLimit3;
                                initSize = initSize2;
                                spanCondition2 = spanCondition;
                            }
                            spanLimit2 = this.offsets.popMinimum(null);
                            pos += spanLimit2;
                            rest -= spanLimit2;
                            spanLength = 0;
                            spanLimit2 = spanLimit3;
                            initSize = initSize2;
                            spanCondition2 = spanCondition;
                        }
                    }
                }
                if (this.offsets.isEmpty() != 0) {
                    return pos;
                }
                spanLimit2 = this.offsets.popMinimum(null);
                pos += spanLimit2;
                rest -= spanLimit2;
                spanLength = 0;
                spanLimit2 = spanLimit3;
                initSize = initSize2;
                spanCondition2 = spanCondition;
            }
        }
    }

    public int spanAndCount(CharSequence s, int start, SpanCondition spanCondition, OutputInt outCount) {
        CharSequence charSequence = s;
        int i = start;
        SpanCondition spanCondition2 = spanCondition;
        OutputInt outputInt = outCount;
        if (spanCondition2 == SpanCondition.NOT_CONTAINED) {
            return spanNot(charSequence, i, outputInt);
        }
        if (spanCondition2 == SpanCondition.CONTAINED) {
            return spanContainedAndCount(charSequence, i, outputInt);
        }
        int stringsLength = this.strings.size();
        int length = s.length();
        int rest = length - i;
        int pos = i;
        int count = 0;
        while (rest != 0) {
            int cpLength = spanOne(this.spanSet, charSequence, pos, rest);
            int maxInc = cpLength > 0 ? cpLength : 0;
            for (int i2 = 0; i2 < stringsLength; i2++) {
                String string = (String) this.strings.get(i2);
                int length16 = string.length();
                if (maxInc < length16 && length16 <= rest && matches16CPB(charSequence, pos, length, string, length16)) {
                    maxInc = length16;
                }
            }
            if (maxInc == 0) {
                outputInt.value = count;
                return pos;
            }
            count++;
            pos += maxInc;
            rest -= maxInc;
        }
        outputInt.value = count;
        return pos;
    }

    private synchronized int spanContainedAndCount(CharSequence s, int start, OutputInt outCount) {
        this.offsets.setMaxLength(this.maxLength16);
        int stringsLength = this.strings.size();
        int length = s.length();
        int rest = length - start;
        int pos = start;
        int count = 0;
        while (rest != 0) {
            int i;
            int cpLength = spanOne(this.spanSet, s, pos, rest);
            if (cpLength > 0) {
                this.offsets.addOffsetAndCount(cpLength, count + 1);
            }
            for (i = 0; i < stringsLength; i++) {
                String string = (String) this.strings.get(i);
                int length16 = string.length();
                if (length16 <= rest && !this.offsets.hasCountAtOffset(length16, count + 1) && matches16CPB(s, pos, length, string, length16)) {
                    this.offsets.addOffsetAndCount(length16, count + 1);
                }
            }
            if (this.offsets.isEmpty()) {
                outCount.value = count;
                return pos;
            }
            i = this.offsets.popMinimum(outCount);
            count = outCount.value;
            pos += i;
            rest -= i;
        }
        outCount.value = count;
        return pos;
    }

    /* JADX WARNING: Missing block: B:82:0x0103, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized int spanBack(CharSequence s, int length, SpanCondition spanCondition) {
        CharSequence charSequence = s;
        int i = length;
        SpanCondition spanCondition2 = spanCondition;
        synchronized (this) {
            int spanNotBack;
            if (spanCondition2 == SpanCondition.NOT_CONTAINED) {
                spanNotBack = spanNotBack(s, length);
                return spanNotBack;
            }
            spanNotBack = this.spanSet.spanBack(charSequence, i, SpanCondition.CONTAINED);
            int i2 = 0;
            if (spanNotBack == 0) {
                return 0;
            }
            int spanLength = i - spanNotBack;
            int initSize = 0;
            if (spanCondition2 == SpanCondition.CONTAINED) {
                initSize = this.maxLength16;
            }
            this.offsets.setMaxLength(initSize);
            int stringsLength = this.strings.size();
            int spanBackLengthsOffset = 0;
            if (this.all) {
                spanBackLengthsOffset = stringsLength;
            }
            while (true) {
                int i3 = 254;
                int i4;
                int overlap;
                if (spanCondition2 == SpanCondition.CONTAINED) {
                    i4 = i2;
                    while (i4 < stringsLength) {
                        overlap = this.spanLengths[spanBackLengthsOffset + i4];
                        if (overlap != 255) {
                            String string = (String) this.strings.get(i4);
                            int length16 = string.length();
                            if (overlap >= i3) {
                                overlap = length16 - string.offsetByCodePoints(i2, 1);
                            }
                            if (overlap > spanLength) {
                                overlap = spanLength;
                            }
                            i3 = length16 - overlap;
                            while (i3 <= spanNotBack) {
                                if (!this.offsets.containsOffset(i3) && matches16CPB(charSequence, spanNotBack - i3, i, string, length16)) {
                                    if (i3 == spanNotBack) {
                                        return i2;
                                    }
                                    this.offsets.addOffset(i3);
                                }
                                if (overlap == 0) {
                                    break;
                                }
                                overlap--;
                                i3++;
                            }
                        }
                        i4++;
                        i3 = 254;
                    }
                } else {
                    overlap = 0;
                    i3 = 0;
                    i4 = i2;
                    while (i4 < stringsLength) {
                        int overlap2 = this.spanLengths[spanBackLengthsOffset + i4];
                        String string2 = (String) this.strings.get(i4);
                        int length162 = string2.length();
                        if (overlap2 >= 254) {
                            overlap2 = length162;
                        }
                        if (overlap2 > spanLength) {
                            overlap2 = spanLength;
                        }
                        int dec = length162 - overlap2;
                        while (true) {
                            i2 = dec;
                            if (i2 > spanNotBack) {
                                break;
                            } else if (overlap2 < overlap) {
                                break;
                            } else if ((overlap2 > overlap || i2 > i3) && matches16CPB(charSequence, spanNotBack - i2, i, string2, length162)) {
                                overlap = overlap2;
                                i3 = i2;
                                break;
                            } else {
                                overlap2--;
                                dec = i2 + 1;
                                spanCondition2 = spanCondition;
                            }
                        }
                        i4++;
                        spanCondition2 = spanCondition;
                    }
                    if (i3 == 0) {
                        if (overlap != 0) {
                        }
                    }
                    spanNotBack -= i3;
                    if (spanNotBack == 0) {
                        return 0;
                    }
                    spanLength = 0;
                    i2 = 0;
                    spanCondition2 = spanCondition;
                }
                if (spanLength == 0) {
                    if (spanNotBack != i) {
                        if (this.offsets.isEmpty()) {
                            int oldPos = spanNotBack;
                            spanNotBack = this.spanSet.spanBack(charSequence, oldPos, SpanCondition.CONTAINED);
                            spanLength = oldPos - spanNotBack;
                            if (spanNotBack == 0 || spanLength == 0) {
                            }
                        } else {
                            spanLength = spanOneBack(this.spanSet, charSequence, spanNotBack);
                            if (spanLength > 0) {
                                if (spanLength == spanNotBack) {
                                    return 0;
                                }
                                spanNotBack -= spanLength;
                                this.offsets.shift(spanLength);
                                spanLength = 0;
                            }
                            spanNotBack -= this.offsets.popMinimum(null);
                            spanLength = 0;
                        }
                        spanCondition2 = spanCondition;
                        i2 = 0;
                    }
                }
                if (this.offsets.isEmpty()) {
                    return spanNotBack;
                }
                spanNotBack -= this.offsets.popMinimum(null);
                spanLength = 0;
                spanCondition2 = spanCondition;
                i2 = 0;
            }
        }
    }

    private int spanNot(CharSequence s, int start, OutputInt outCount) {
        int length = s.length();
        int pos = start;
        int rest = length - start;
        int stringsLength = this.strings.size();
        rest = pos;
        pos = 0;
        int rest2;
        int cpLength;
        do {
            int spanLimit;
            if (outCount == null) {
                spanLimit = this.spanNotSet.span(s, rest, SpanCondition.NOT_CONTAINED);
            } else {
                spanLimit = this.spanNotSet.spanAndCount(s, rest, SpanCondition.NOT_CONTAINED, outCount);
                cpLength = outCount.value + pos;
                pos = cpLength;
                outCount.value = cpLength;
            }
            if (spanLimit == length) {
                return length;
            }
            rest = spanLimit;
            rest2 = length - spanLimit;
            cpLength = spanOne(this.spanSet, s, rest, rest2);
            if (cpLength > 0) {
                return rest;
            }
            for (int i = 0; i < stringsLength; i++) {
                if (this.spanLengths[i] != ALL_CP_CONTAINED) {
                    String string = (String) this.strings.get(i);
                    int length16 = string.length();
                    if (length16 <= rest2 && matches16CPB(s, rest, length, string, length16)) {
                        return rest;
                    }
                }
            }
            rest -= cpLength;
            pos++;
        } while (rest2 + cpLength != 0);
        if (outCount != null) {
            outCount.value = pos;
        }
        return length;
    }

    private int spanNotBack(CharSequence s, int length) {
        int pos = length;
        int stringsLength = this.strings.size();
        do {
            pos = this.spanNotSet.spanBack(s, pos, SpanCondition.NOT_CONTAINED);
            if (pos == 0) {
                return 0;
            }
            int cpLength = spanOneBack(this.spanSet, s, pos);
            if (cpLength > 0) {
                return pos;
            }
            for (int i = 0; i < stringsLength; i++) {
                if (this.spanLengths[i] != ALL_CP_CONTAINED) {
                    String string = (String) this.strings.get(i);
                    int length16 = string.length();
                    if (length16 <= pos && matches16CPB(s, pos - length16, length, string, length16)) {
                        return pos;
                    }
                }
            }
            pos += cpLength;
        } while (pos != 0);
        return 0;
    }

    static short makeSpanLengthByte(int spanLength) {
        return spanLength < 254 ? (short) spanLength : LONG_SPAN;
    }

    private static boolean matches16(CharSequence s, int start, String t, int length) {
        int end = start + length;
        while (true) {
            int length2 = length - 1;
            if (length <= 0) {
                return true;
            }
            end--;
            if (s.charAt(end) != t.charAt(length2)) {
                return false;
            }
            length = length2;
        }
    }

    static boolean matches16CPB(CharSequence s, int start, int limit, String t, int tlength) {
        if (!matches16(s, start, t, tlength) || ((start > 0 && Character.isHighSurrogate(s.charAt(start - 1)) && Character.isLowSurrogate(s.charAt(start))) || (start + tlength < limit && Character.isHighSurrogate(s.charAt((start + tlength) - 1)) && Character.isLowSurrogate(s.charAt(start + tlength))))) {
            return false;
        }
        return true;
    }

    static int spanOne(UnicodeSet set, CharSequence s, int start, int length) {
        int c = s.charAt(start);
        if (c >= 55296 && c <= UCharacter.MAX_HIGH_SURROGATE) {
            int i = 2;
            if (length >= 2) {
                char c2 = s.charAt(start + 1);
                if (UTF16.isTrailSurrogate(c2)) {
                    if (!set.contains(Character.toCodePoint(c, c2))) {
                        i = -2;
                    }
                    return i;
                }
            }
        }
        return set.contains(c) ? 1 : -1;
    }

    static int spanOneBack(UnicodeSet set, CharSequence s, int length) {
        int c = s.charAt(length - 1);
        if (c >= UCharacter.MIN_LOW_SURROGATE && c <= 57343) {
            int i = 2;
            if (length >= 2) {
                char c2 = s.charAt(length - 2);
                if (UTF16.isLeadSurrogate(c2)) {
                    if (!set.contains(Character.toCodePoint(c2, c))) {
                        i = -2;
                    }
                    return i;
                }
            }
        }
        return set.contains(c) ? 1 : -1;
    }
}
