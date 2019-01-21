package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UnicodeSet.SpanCondition;
import android.icu.util.OutputInt;
import dalvik.bytecode.Opcodes;

public final class BMPSet {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static int U16_SURROGATE_OFFSET = 56613888;
    private int[] bmpBlockBits;
    private boolean[] latin1Contains;
    private final int[] list;
    private int[] list4kStarts;
    private final int listLength;
    private int[] table7FF;

    public BMPSet(int[] parentList, int parentListLength) {
        this.list = parentList;
        this.listLength = parentListLength;
        this.latin1Contains = new boolean[256];
        this.table7FF = new int[64];
        this.bmpBlockBits = new int[64];
        this.list4kStarts = new int[18];
        this.list4kStarts[0] = findCodePoint(2048, 0, this.listLength - 1);
        for (int i = 1; i <= 16; i++) {
            this.list4kStarts[i] = findCodePoint(i << 12, this.list4kStarts[i - 1], this.listLength - 1);
        }
        this.list4kStarts[17] = this.listLength - 1;
        initBits();
    }

    public BMPSet(BMPSet otherBMPSet, int[] newParentList, int newParentListLength) {
        this.list = newParentList;
        this.listLength = newParentListLength;
        this.latin1Contains = (boolean[]) otherBMPSet.latin1Contains.clone();
        this.table7FF = (int[]) otherBMPSet.table7FF.clone();
        this.bmpBlockBits = (int[]) otherBMPSet.bmpBlockBits.clone();
        this.list4kStarts = (int[]) otherBMPSet.list4kStarts.clone();
    }

    public boolean contains(int c) {
        if (c <= 255) {
            return this.latin1Contains[c];
        }
        boolean z = false;
        if (c <= Opcodes.OP_IGET_WIDE_JUMBO) {
            if ((this.table7FF[c & 63] & (1 << (c >> 6))) != 0) {
                z = true;
            }
            return z;
        } else if (c < 55296 || (c >= 57344 && c <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH)) {
            int lead = c >> 12;
            int twoBits = (this.bmpBlockBits[(c >> 6) & 63] >> lead) & 65537;
            if (twoBits > 1) {
                return containsSlow(c, this.list4kStarts[lead], this.list4kStarts[lead + 1]);
            }
            if (twoBits != 0) {
                z = true;
            }
            return z;
        } else if (c <= 1114111) {
            return containsSlow(c, this.list4kStarts[13], this.list4kStarts[17]);
        } else {
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:52:0x012b  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0128  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final int span(CharSequence s, int start, SpanCondition spanCondition, OutputInt outCount) {
        CharSequence charSequence = s;
        OutputInt outputInt = outCount;
        int i = start;
        int limit = s.length();
        int numSupplementary = 0;
        SpanCondition spanCondition2 = SpanCondition.NOT_CONTAINED;
        int i2 = 17;
        int i3 = 16;
        char c = 57344;
        char c2 = 55296;
        char c3 = 255;
        char c4 = UCharacter.MIN_LOW_SURROGATE;
        char c5;
        int twoBits;
        if (spanCondition2 != spanCondition) {
            while (i < limit) {
                c5 = charSequence.charAt(i);
                if (c5 <= c3) {
                    if (!this.latin1Contains[c5]) {
                        break;
                    }
                } else if (c5 > 2047) {
                    if (c5 >= 55296 && c5 < c4 && i + 1 != limit) {
                        c3 = charSequence.charAt(i + 1);
                        char c22 = c3;
                        if (c3 >= c4) {
                            c3 = c22;
                            if (c3 < c) {
                                if (!containsSlow(Character.toCodePoint(c5, c3), this.list4kStarts[i3], this.list4kStarts[17])) {
                                    break;
                                }
                                numSupplementary++;
                                i++;
                            }
                        }
                    }
                    i3 = c5 >> 12;
                    twoBits = (this.bmpBlockBits[(c5 >> 6) & 63] >> i3) & 65537;
                    if (twoBits <= 1) {
                        if (twoBits == 0) {
                            break;
                        }
                    } else if (!containsSlow(c5, this.list4kStarts[i3], this.list4kStarts[i3 + 1])) {
                        break;
                    }
                } else if ((this.table7FF[c5 & 63] & (1 << (c5 >> 6))) == 0) {
                    break;
                }
                i++;
                i3 = 16;
                c = 57344;
                c3 = 255;
                c4 = UCharacter.MIN_LOW_SURROGATE;
            }
        } else {
            while (i < limit) {
                c5 = charSequence.charAt(i);
                if (c5 <= 255) {
                    if (this.latin1Contains[c5]) {
                        break;
                    }
                } else if (c5 <= 2047) {
                    if ((this.table7FF[c5 & 63] & (1 << (c5 >> 6))) != 0) {
                        break;
                    }
                } else {
                    if (c5 >= c2 && c5 < UCharacter.MIN_LOW_SURROGATE && i + 1 != limit) {
                        c3 = charSequence.charAt(i + 1);
                        c4 = c3;
                        if (c3 >= UCharacter.MIN_LOW_SURROGATE) {
                            if (c4 < 57344) {
                                if (containsSlow(Character.toCodePoint(c5, c4), this.list4kStarts[16], this.list4kStarts[i2])) {
                                    break;
                                }
                                numSupplementary++;
                                i++;
                                i++;
                                i2 = 17;
                                c2 = 55296;
                            }
                            i3 = c5 >> 12;
                            twoBits = (this.bmpBlockBits[(c5 >> 6) & 63] >> i3) & 65537;
                            if (twoBits > 1) {
                                if (twoBits != 0) {
                                    break;
                                }
                                i++;
                                i2 = 17;
                                c2 = 55296;
                            } else if (containsSlow(c5, this.list4kStarts[i3], this.list4kStarts[i3 + 1])) {
                                break;
                            } else {
                                i++;
                                i2 = 17;
                                c2 = 55296;
                            }
                        }
                    }
                    i3 = c5 >> 12;
                    twoBits = (this.bmpBlockBits[(c5 >> 6) & 63] >> i3) & 65537;
                    if (twoBits > 1) {
                    }
                }
                i++;
                i2 = 17;
                c2 = 55296;
            }
        }
        if (outputInt != null) {
            outputInt.value = (i - start) - numSupplementary;
        }
        return i;
    }

    public final int spanBack(CharSequence s, int limit, SpanCondition spanCondition) {
        int limit2;
        CharSequence charSequence = s;
        SpanCondition spanCondition2 = SpanCondition.NOT_CONTAINED;
        int i = 16;
        char c = UCharacter.MIN_LOW_SURROGATE;
        char c2;
        char charAt;
        int twoBits;
        if (spanCondition2 != spanCondition) {
            limit2 = limit;
            while (true) {
                limit2--;
                c2 = charSequence.charAt(limit2);
                if (c2 > 255) {
                    if (c2 <= 2047) {
                        if ((this.table7FF[c2 & 63] & (1 << (c2 >> 6))) == 0) {
                            break;
                        }
                    }
                    if (c2 >= 55296 && c2 >= c && limit2 != 0) {
                        charAt = charSequence.charAt(limit2 - 1);
                        char c22 = charAt;
                        if (charAt >= 55296 && c22 < c) {
                            if (!containsSlow(Character.toCodePoint(c22, c2), this.list4kStarts[i], this.list4kStarts[17])) {
                                break;
                            }
                            limit2--;
                        }
                    }
                    i = c2 >> 12;
                    twoBits = (this.bmpBlockBits[(c2 >> 6) & 63] >> i) & 65537;
                    if (twoBits > 1) {
                        if (!containsSlow(c2, this.list4kStarts[i], this.list4kStarts[i + 1])) {
                            break;
                        }
                    } else if (twoBits == 0) {
                        break;
                    }
                } else if (!this.latin1Contains[c2]) {
                    break;
                }
                if (limit2 == 0) {
                    return 0;
                }
                i = 16;
                c = UCharacter.MIN_LOW_SURROGATE;
            }
        } else {
            limit2 = limit;
            do {
                limit2--;
                c2 = charSequence.charAt(limit2);
                if (c2 <= 255) {
                    if (this.latin1Contains[c2]) {
                    }
                } else if (c2 > 2047) {
                    if (c2 >= 55296 && c2 >= UCharacter.MIN_LOW_SURROGATE && limit2 != 0) {
                        c = charSequence.charAt(limit2 - 1);
                        charAt = c;
                        if (c >= 55296 && charAt < UCharacter.MIN_LOW_SURROGATE) {
                            if (!containsSlow(Character.toCodePoint(charAt, c2), this.list4kStarts[16], this.list4kStarts[17])) {
                                limit2--;
                                continue;
                            }
                        }
                    }
                    i = c2 >> 12;
                    twoBits = (this.bmpBlockBits[(c2 >> 6) & 63] >> i) & 65537;
                    if (twoBits <= 1) {
                        if (twoBits != 0) {
                        }
                    } else if (containsSlow(c2, this.list4kStarts[i], this.list4kStarts[i + 1])) {
                    }
                } else if ((this.table7FF[c2 & 63] & (1 << (c2 >> 6))) != 0) {
                }
                continue;
            } while (limit2 != 0);
            return 0;
        }
        return limit2 + 1;
    }

    private static void set32x64Bits(int[] table, int start, int limit) {
        int lead = start >> 6;
        int trail = start & 63;
        int bits = 1 << lead;
        if (start + 1 == limit) {
            table[trail] = table[trail] | bits;
            return;
        }
        int limitLead = limit >> 6;
        int limitTrail = limit & 63;
        if (lead == limitLead) {
            while (trail < limitTrail) {
                int trail2 = trail + 1;
                table[trail] = table[trail] | bits;
                trail = trail2;
            }
        } else {
            if (trail > 0) {
                int trail3;
                while (true) {
                    trail3 = trail + 1;
                    table[trail] = table[trail] | bits;
                    if (trail3 >= 64) {
                        break;
                    }
                    trail = trail3;
                }
                lead++;
                trail = trail3;
            }
            if (lead < limitLead) {
                bits = ~((1 << lead) - 1);
                if (limitLead < 32) {
                    bits &= (1 << limitLead) - 1;
                }
                for (trail = 0; trail < 64; trail++) {
                    table[trail] = table[trail] | bits;
                }
            }
            bits = 1 << limitLead;
            for (trail = 0; trail < limitTrail; trail++) {
                table[trail] = table[trail] | bits;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x00bd A:{LOOP_END, LOOP:0: B:1:0x0001->B:48:0x00bd} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x002a A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void initBits() {
        int listIndex;
        int listIndex2;
        int limit;
        int start;
        int listIndex3;
        int start2 = 0;
        while (true) {
            listIndex = start2 + 1;
            start2 = this.list[start2];
            if (listIndex < this.listLength) {
                listIndex2 = listIndex + 1;
                limit = this.list[listIndex];
                listIndex = listIndex2;
            } else {
                limit = 1114112;
            }
            if (start2 >= 256) {
                break;
            }
            while (true) {
                start = start2 + 1;
                this.latin1Contains[start2] = true;
                if (start < limit && start < 256) {
                    start2 = start;
                } else if (limit <= 256) {
                    start2 = start;
                    break;
                } else {
                    start2 = listIndex;
                }
            }
            if (limit <= 256) {
            }
        }
        while (true) {
            listIndex2 = 2048;
            if (start2 >= 2048) {
                break;
            }
            set32x64Bits(this.table7FF, start2, limit <= 2048 ? limit : 2048);
            if (limit > 2048) {
                start2 = 2048;
                break;
            }
            listIndex3 = listIndex + 1;
            start2 = this.list[listIndex];
            if (listIndex3 < this.listLength) {
                listIndex2 = listIndex3 + 1;
                limit = this.list[listIndex3];
                listIndex = listIndex2;
            } else {
                limit = 1114112;
                listIndex = listIndex3;
            }
        }
        while (start2 < 65536) {
            if (limit > 65536) {
                limit = 65536;
            }
            if (start2 < listIndex2) {
                start2 = listIndex2;
            }
            if (start2 < limit) {
                int[] iArr;
                int i;
                if ((start2 & 63) != 0) {
                    start2 >>= 6;
                    iArr = this.bmpBlockBits;
                    i = start2 & 63;
                    iArr[i] = iArr[i] | (65537 << (start2 >> 6));
                    start2 = (start2 + 1) << 6;
                    listIndex2 = start2;
                }
                if (start2 < limit) {
                    if (start2 < (limit & -64)) {
                        set32x64Bits(this.bmpBlockBits, start2 >> 6, limit >> 6);
                    }
                    if ((limit & 63) != 0) {
                        limit >>= 6;
                        iArr = this.bmpBlockBits;
                        i = limit & 63;
                        iArr[i] = (65537 << (limit >> 6)) | iArr[i];
                        limit = (limit + 1) << 6;
                        listIndex2 = limit;
                    }
                }
            }
            if (limit != 65536) {
                start = listIndex + 1;
                start2 = this.list[listIndex];
                if (start < this.listLength) {
                    listIndex3 = start + 1;
                    limit = this.list[start];
                    listIndex = listIndex3;
                } else {
                    limit = 1114112;
                    listIndex = start;
                }
            } else {
                return;
            }
        }
    }

    private int findCodePoint(int c, int lo, int hi) {
        if (c < this.list[lo]) {
            return lo;
        }
        if (lo >= hi || c >= this.list[hi - 1]) {
            return hi;
        }
        while (true) {
            int i = (lo + hi) >>> 1;
            if (i == lo) {
                return hi;
            }
            if (c < this.list[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
    }

    private final boolean containsSlow(int c, int lo, int hi) {
        return (findCodePoint(c, lo, hi) & 1) != 0;
    }
}
