package android.icu.text;

import android.icu.impl.UBiDiProps;

public final class ArabicShaping {
    private static final int ALEFTYPE = 32;
    private static final int DESHAPE_MODE = 1;
    public static final int DIGITS_AN2EN = 64;
    public static final int DIGITS_EN2AN = 32;
    public static final int DIGITS_EN2AN_INIT_AL = 128;
    public static final int DIGITS_EN2AN_INIT_LR = 96;
    public static final int DIGITS_MASK = 224;
    public static final int DIGITS_NOOP = 0;
    public static final int DIGIT_TYPE_AN = 0;
    public static final int DIGIT_TYPE_AN_EXTENDED = 256;
    public static final int DIGIT_TYPE_MASK = 256;
    private static final char HAMZA06_CHAR = 'ء';
    private static final char HAMZAFE_CHAR = 'ﺀ';
    private static final int IRRELEVANT = 4;
    public static final int LAMALEF_AUTO = 65536;
    public static final int LAMALEF_BEGIN = 3;
    public static final int LAMALEF_END = 2;
    public static final int LAMALEF_MASK = 65539;
    public static final int LAMALEF_NEAR = 1;
    public static final int LAMALEF_RESIZE = 0;
    private static final char LAMALEF_SPACE_SUB = '￿';
    private static final int LAMTYPE = 16;
    private static final char LAM_CHAR = 'ل';
    public static final int LENGTH_FIXED_SPACES_AT_BEGINNING = 3;
    public static final int LENGTH_FIXED_SPACES_AT_END = 2;
    public static final int LENGTH_FIXED_SPACES_NEAR = 1;
    public static final int LENGTH_GROW_SHRINK = 0;
    public static final int LENGTH_MASK = 65539;
    public static final int LETTERS_MASK = 24;
    public static final int LETTERS_NOOP = 0;
    public static final int LETTERS_SHAPE = 8;
    public static final int LETTERS_SHAPE_TASHKEEL_ISOLATED = 24;
    public static final int LETTERS_UNSHAPE = 16;
    private static final int LINKL = 2;
    private static final int LINKR = 1;
    private static final int LINK_MASK = 3;
    private static final char NEW_TAIL_CHAR = 'ﹳ';
    private static final char OLD_TAIL_CHAR = '​';
    public static final int SEEN_MASK = 7340032;
    public static final int SEEN_TWOCELL_NEAR = 2097152;
    private static final char SHADDA06_CHAR = 'ّ';
    private static final char SHADDA_CHAR = 'ﹼ';
    private static final char SHADDA_TATWEEL_CHAR = 'ﹽ';
    private static final int SHAPE_MODE = 0;
    public static final int SHAPE_TAIL_NEW_UNICODE = 134217728;
    public static final int SHAPE_TAIL_TYPE_MASK = 134217728;
    public static final int SPACES_RELATIVE_TO_TEXT_BEGIN_END = 67108864;
    public static final int SPACES_RELATIVE_TO_TEXT_MASK = 67108864;
    private static final char SPACE_CHAR = ' ';
    public static final int TASHKEEL_BEGIN = 262144;
    public static final int TASHKEEL_END = 393216;
    public static final int TASHKEEL_MASK = 917504;
    public static final int TASHKEEL_REPLACE_BY_TATWEEL = 786432;
    public static final int TASHKEEL_RESIZE = 524288;
    private static final char TASHKEEL_SPACE_SUB = '￾';
    private static final char TATWEEL_CHAR = 'ـ';
    public static final int TEXT_DIRECTION_LOGICAL = 0;
    public static final int TEXT_DIRECTION_MASK = 4;
    public static final int TEXT_DIRECTION_VISUAL_LTR = 4;
    public static final int TEXT_DIRECTION_VISUAL_RTL = 0;
    public static final int YEHHAMZA_MASK = 58720256;
    public static final int YEHHAMZA_TWOCELL_NEAR = 16777216;
    private static final char YEH_HAMZAFE_CHAR = 'ﺉ';
    private static final char YEH_HAMZA_CHAR = 'ئ';
    private static final int[] araLink = new int[]{4385, 4897, 5377, 5921, 6403, 7457, 7939, 8961, 9475, 10499, 11523, 12547, 13571, 14593, 15105, 15617, 16129, 16643, 17667, 18691, 19715, 20739, 21763, 22787, 23811, 0, 0, 0, 0, 0, 3, 24835, 25859, 26883, 27923, 28931, 29955, 30979, 32001, 32513, 33027, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 0, 0, 0, 0, 0, 34049, 34561, 35073, 35585, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 33, 33, 0, 33, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 1, 3, 3, 3, 3, 1, 1};
    private static int[] convertFEto06 = new int[]{1611, 1611, 1612, 1612, 1613, 1613, 1614, 1614, 1615, 1615, 1616, 1616, 1617, 1617, 1618, 1618, 1569, 1570, 1570, 1571, 1571, 1572, 1572, 1573, 1573, 1574, 1574, 1574, 1574, 1575, 1575, 1576, 1576, 1576, 1576, 1577, 1577, 1578, 1578, 1578, 1578, 1579, 1579, 1579, 1579, 1580, 1580, 1580, 1580, 1581, 1581, 1581, 1581, 1582, 1582, 1582, 1582, 1583, 1583, 1584, 1584, 1585, 1585, 1586, 1586, 1587, 1587, 1587, 1587, 1588, 1588, 1588, 1588, 1589, 1589, 1589, 1589, 1590, 1590, 1590, 1590, 1591, 1591, 1591, 1591, 1592, 1592, 1592, 1592, 1593, 1593, 1593, 1593, 1594, 1594, 1594, 1594, 1601, 1601, 1601, 1601, 1602, 1602, 1602, 1602, 1603, 1603, 1603, 1603, 1604, 1604, 1604, 1604, 1605, 1605, 1605, 1605, 1606, 1606, 1606, 1606, 1607, 1607, 1607, 1607, 1608, 1608, 1609, 1609, 1610, 1610, 1610, 1610, 1628, 1628, 1629, 1629, 1630, 1630, 1631, 1631};
    private static final char[] convertNormalizedLamAlef = new char[]{1570, 1571, 1573, 1575};
    private static final int[] irrelevantPos = new int[]{0, 2, 4, 6, 8, 10, 12, 14};
    private static final int[] presLink = new int[]{3, 3, 3, 0, 3, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 0, 32, 33, 32, 33, 0, 1, 32, 33, 0, 2, 3, 1, 32, 33, 0, 2, 3, 1, 0, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 16, 18, 19, 17, 0, 2, 3, 1, 0, 2, 3, 1, 0, 2, 3, 1, 0, 1, 0, 1, 0, 2, 3, 1, 0, 1, 0, 1, 0, 1, 0, 1};
    private static final int[][][] shapeTable;
    private static final int[] tailFamilyIsolatedFinal = new int[]{1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1};
    private static final int[] tashkeelMedial = new int[]{0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1};
    private static final char[] yehHamzaToYeh = new char[]{65263, 65264};
    private boolean isLogical;
    private final int options;
    private boolean spacesRelativeToTextBeginEnd;
    private char tailChar;

    public int shape(char[] source, int sourceStart, int sourceLength, char[] dest, int destStart, int destSize) throws ArabicShapingException {
        StringBuilder stringBuilder;
        if (source == null) {
            throw new IllegalArgumentException("source can not be null");
        } else if (sourceStart < 0 || sourceLength < 0 || sourceStart + sourceLength > source.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad source start (");
            stringBuilder.append(sourceStart);
            stringBuilder.append(") or length (");
            stringBuilder.append(sourceLength);
            stringBuilder.append(") for buffer of length ");
            stringBuilder.append(source.length);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (dest == null && destSize != 0) {
            throw new IllegalArgumentException("null dest requires destSize == 0");
        } else if (destSize != 0 && (destStart < 0 || destSize < 0 || destStart + destSize > dest.length)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad dest start (");
            stringBuilder.append(destStart);
            stringBuilder.append(") or size (");
            stringBuilder.append(destSize);
            stringBuilder.append(") for buffer of length ");
            stringBuilder.append(dest.length);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if ((this.options & TASHKEEL_MASK) != 0 && (this.options & TASHKEEL_MASK) != 262144 && (this.options & TASHKEEL_MASK) != TASHKEEL_END && (this.options & TASHKEEL_MASK) != 524288 && (this.options & TASHKEEL_MASK) != TASHKEEL_REPLACE_BY_TATWEEL) {
            throw new IllegalArgumentException("Wrong Tashkeel argument");
        } else if ((this.options & 65539) != 0 && (this.options & 65539) != 3 && (this.options & 65539) != 2 && (this.options & 65539) != 0 && (this.options & 65539) != 65536 && (this.options & 65539) != 1) {
            throw new IllegalArgumentException("Wrong Lam Alef argument");
        } else if ((this.options & TASHKEEL_MASK) == 0 || (this.options & 24) != 16) {
            return internalShape(source, sourceStart, sourceLength, dest, destStart, destSize);
        } else {
            throw new IllegalArgumentException("Tashkeel replacement should not be enabled in deshaping mode ");
        }
    }

    public void shape(char[] source, int start, int length) throws ArabicShapingException {
        if ((this.options & 65539) != 0) {
            shape(source, start, length, source, start, length);
            return;
        }
        throw new ArabicShapingException("Cannot shape in place with length option resize.");
    }

    public String shape(String text) throws ArabicShapingException {
        char[] src = text.toCharArray();
        char[] dest = src;
        if ((this.options & 65539) == 0 && (this.options & 24) == 16) {
            dest = new char[(src.length * 2)];
        }
        char[] dest2 = dest;
        return new String(dest2, 0, shape(src, 0, src.length, dest2, 0, dest2.length));
    }

    public ArabicShaping(int options) {
        this.options = options;
        if ((options & 224) <= 128) {
            boolean z = false;
            this.isLogical = (options & 4) == 0;
            if ((options & 67108864) == 67108864) {
                z = true;
            }
            this.spacesRelativeToTextBeginEnd = z;
            if ((options & 134217728) == 134217728) {
                this.tailChar = NEW_TAIL_CHAR;
                return;
            } else {
                this.tailChar = OLD_TAIL_CHAR;
                return;
            }
        }
        throw new IllegalArgumentException("bad DIGITS options");
    }

    public boolean equals(Object rhs) {
        return rhs != null && rhs.getClass() == ArabicShaping.class && this.options == ((ArabicShaping) rhs).options;
    }

    public int hashCode() {
        return this.options;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.append('[');
        int i = this.options & 65539;
        if (i != 65536) {
            switch (i) {
                case 0:
                    buf.append("LamAlef resize");
                    break;
                case 1:
                    buf.append("LamAlef spaces at near");
                    break;
                case 2:
                    buf.append("LamAlef spaces at end");
                    break;
                case 3:
                    buf.append("LamAlef spaces at begin");
                    break;
            }
        }
        buf.append("lamAlef auto");
        i = this.options & 4;
        if (i == 0) {
            buf.append(", logical");
        } else if (i == 4) {
            buf.append(", visual");
        }
        i = this.options & 24;
        if (i == 0) {
            buf.append(", no letter shaping");
        } else if (i == 8) {
            buf.append(", shape letters");
        } else if (i == 16) {
            buf.append(", unshape letters");
        } else if (i == 24) {
            buf.append(", shape letters tashkeel isolated");
        }
        if ((this.options & SEEN_MASK) == 2097152) {
            buf.append(", Seen at near");
        }
        if ((this.options & YEHHAMZA_MASK) == 16777216) {
            buf.append(", Yeh Hamza at near");
        }
        i = this.options & TASHKEEL_MASK;
        if (i == 262144) {
            buf.append(", Tashkeel at begin");
        } else if (i == TASHKEEL_END) {
            buf.append(", Tashkeel at end");
        } else if (i == 524288) {
            buf.append(", Tashkeel resize");
        } else if (i == TASHKEEL_REPLACE_BY_TATWEEL) {
            buf.append(", Tashkeel replace with tatweel");
        }
        i = this.options & 224;
        if (i == 0) {
            buf.append(", no digit shaping");
        } else if (i == 32) {
            buf.append(", shape digits to AN");
        } else if (i == 64) {
            buf.append(", shape digits to EN");
        } else if (i == 96) {
            buf.append(", shape digits to AN contextually: default EN");
        } else if (i == 128) {
            buf.append(", shape digits to AN contextually: default AL");
        }
        i = this.options & 256;
        if (i == 0) {
            buf.append(", standard Arabic-Indic digits");
        } else if (i == 256) {
            buf.append(", extended Arabic-Indic digits");
        }
        buf.append("]");
        return buf.toString();
    }

    static {
        r2 = new int[4][][];
        r2[0] = new int[][]{new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}, new int[]{0, 1, 0, 3}, new int[]{0, 1, 0, 1}};
        r2[1] = new int[][]{new int[]{0, 0, 2, 2}, new int[]{0, 0, 1, 2}, new int[]{0, 1, 1, 2}, new int[]{0, 1, 1, 3}};
        r2[2] = new int[][]{new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 0}, new int[]{0, 1, 0, 3}, new int[]{0, 1, 0, 3}};
        r2[3] = new int[][]{new int[]{0, 0, 1, 2}, new int[]{0, 0, 1, 2}, new int[]{0, 1, 1, 2}, new int[]{0, 1, 1, 3}};
        shapeTable = r2;
    }

    private void shapeToArabicDigitsWithContext(char[] dest, int start, int length, char digitBase, boolean lastStrongWasAL) {
        UBiDiProps bdp = UBiDiProps.INSTANCE;
        digitBase = (char) (digitBase - 48);
        int i = start + length;
        while (true) {
            i--;
            if (i >= start) {
                char ch = dest[i];
                int i2 = bdp.getClass(ch);
                if (i2 != 13) {
                    switch (i2) {
                        case 0:
                        case 1:
                            lastStrongWasAL = false;
                            break;
                        case 2:
                            if (lastStrongWasAL && ch <= '9') {
                                dest[i] = (char) (ch + digitBase);
                                break;
                            }
                        default:
                            break;
                    }
                }
                lastStrongWasAL = true;
            } else {
                return;
            }
        }
    }

    private static void invertBuffer(char[] buffer, int start, int length) {
        int i = start;
        for (int j = (start + length) - 1; i < j; j--) {
            char temp = buffer[i];
            buffer[i] = buffer[j];
            buffer[j] = temp;
            i++;
        }
    }

    private static char changeLamAlef(char ch) {
        switch (ch) {
            case 1570:
                return 1628;
            case 1571:
                return 1629;
            case 1573:
                return 1630;
            case 1575:
                return 1631;
            default:
                return 0;
        }
    }

    private static int specialChar(char ch) {
        if ((ch > HAMZA06_CHAR && ch < YEH_HAMZA_CHAR) || ch == 1575 || ((ch > 1582 && ch < 1587) || ((ch > 1607 && ch < 1610) || ch == 1577))) {
            return 1;
        }
        if (ch >= 1611 && ch <= 1618) {
            return 2;
        }
        if ((ch < 1619 || ch > 1621) && ch != 1648 && (ch < 65136 || ch > 65151)) {
            return 0;
        }
        return 3;
    }

    private static int getLink(char ch) {
        if (ch >= 1570 && ch <= 1747) {
            return araLink[ch - 1570];
        }
        if (ch == 8205) {
            return 3;
        }
        if (ch >= 8301 && ch <= 8303) {
            return 4;
        }
        if (ch < 65136 || ch > 65276) {
            return 0;
        }
        return presLink[ch - 65136];
    }

    private static int countSpacesLeft(char[] dest, int start, int count) {
        int e = start + count;
        for (int i = start; i < e; i++) {
            if (dest[i] != SPACE_CHAR) {
                return i - start;
            }
        }
        return count;
    }

    private static int countSpacesRight(char[] dest, int start, int count) {
        int i = start + count;
        do {
            i--;
            if (i < start) {
                return count;
            }
        } while (dest[i] == SPACE_CHAR);
        return ((start + count) - 1) - i;
    }

    private static boolean isTashkeelChar(char ch) {
        return ch >= 1611 && ch <= 1618;
    }

    private static int isSeenTailFamilyChar(char ch) {
        if (ch < 65201 || ch >= 65215) {
            return 0;
        }
        return tailFamilyIsolatedFinal[ch - 65201];
    }

    private static int isSeenFamilyChar(char ch) {
        if (ch < 1587 || ch > 1590) {
            return 0;
        }
        return 1;
    }

    private static boolean isTailChar(char ch) {
        if (ch == OLD_TAIL_CHAR || ch == NEW_TAIL_CHAR) {
            return true;
        }
        return false;
    }

    private static boolean isAlefMaksouraChar(char ch) {
        return ch == 65263 || ch == 65264 || ch == 1609;
    }

    private static boolean isYehHamzaChar(char ch) {
        if (ch == YEH_HAMZAFE_CHAR || ch == 65162) {
            return true;
        }
        return false;
    }

    private static boolean isTashkeelCharFE(char ch) {
        return ch != 65141 && ch >= 65136 && ch <= 65151;
    }

    private static int isTashkeelOnTatweelChar(char ch) {
        if (ch >= 65136 && ch <= 65151 && ch != NEW_TAIL_CHAR && ch != 65141 && ch != SHADDA_TATWEEL_CHAR) {
            return tashkeelMedial[ch - 65136];
        }
        if ((ch < 64754 || ch > 64756) && ch != SHADDA_TATWEEL_CHAR) {
            return 0;
        }
        return 2;
    }

    private static int isIsolatedTashkeelChar(char ch) {
        if (ch >= 65136 && ch <= 65151 && ch != NEW_TAIL_CHAR && ch != 65141) {
            return 1 - tashkeelMedial[ch - 65136];
        }
        if (ch < 64606 || ch > 64611) {
            return 0;
        }
        return 1;
    }

    private static boolean isAlefChar(char ch) {
        return ch == 1570 || ch == 1571 || ch == 1573 || ch == 1575;
    }

    private static boolean isLamAlefChar(char ch) {
        return ch >= 65269 && ch <= 65276;
    }

    private static boolean isNormalizedLamAlefChar(char ch) {
        return ch >= 1628 && ch <= 1631;
    }

    /* JADX WARNING: Missing block: B:4:0x000e, code skipped:
            if (r1 != 24) goto L_0x006e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int calculateSize(char[] source, int sourceStart, int sourceLength) {
        int destSize = sourceLength;
        int i = this.options & 24;
        if (i != 8) {
            if (i == 16) {
                int e = sourceStart + sourceLength;
                for (i = sourceStart; i < e; i++) {
                    if (isLamAlefChar(source[i])) {
                        destSize++;
                    }
                }
            }
            return destSize;
        }
        int e2;
        if (this.isLogical) {
            i = sourceStart;
            e2 = (sourceStart + sourceLength) - 1;
            while (i < e2) {
                if ((source[i] == LAM_CHAR && isAlefChar(source[i + 1])) || isTashkeelCharFE(source[i])) {
                    destSize--;
                }
                i++;
            }
        } else {
            i = sourceStart + 1;
            e2 = sourceStart + sourceLength;
            while (i < e2) {
                if ((source[i] == LAM_CHAR && isAlefChar(source[i - 1])) || isTashkeelCharFE(source[i])) {
                    destSize--;
                }
                i++;
            }
        }
        return destSize;
    }

    private static int countSpaceSub(char[] dest, int length, char subChar) {
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (dest[i] == subChar) {
                count++;
            }
        }
        return count;
    }

    private static void shiftArray(char[] dest, int start, int e, char subChar) {
        int r = e;
        int w = r;
        while (true) {
            r--;
            if (r >= start) {
                char ch = dest[r];
                if (ch != subChar) {
                    w--;
                    if (w != r) {
                        dest[w] = ch;
                    }
                }
            } else {
                return;
            }
        }
    }

    private static int flipArray(char[] dest, int start, int e, int w) {
        if (w <= start) {
            return e;
        }
        int r = w;
        w = start;
        while (r < e) {
            int w2 = w + 1;
            int r2 = r + 1;
            dest[w] = dest[r];
            w = w2;
            r = r2;
        }
        return w;
    }

    private static int handleTashkeelWithTatweel(char[] dest, int sourceLength) {
        int i = 0;
        while (i < sourceLength) {
            if (isTashkeelOnTatweelChar(dest[i]) == 1) {
                dest[i] = TATWEEL_CHAR;
            } else if (isTashkeelOnTatweelChar(dest[i]) == 2) {
                dest[i] = SHADDA_TATWEEL_CHAR;
            } else if (isIsolatedTashkeelChar(dest[i]) == 1 && dest[i] != SHADDA_CHAR) {
                dest[i] = SPACE_CHAR;
            }
            i++;
        }
        return sourceLength;
    }

    private int handleGeneratedSpaces(char[] dest, int start, int length) {
        char[] cArr = dest;
        int i = start;
        int length2 = length;
        int lenOptionsLamAlef = this.options & 65539;
        int lenOptionsTashkeel = this.options & TASHKEEL_MASK;
        boolean lamAlefOn = false;
        boolean tashkeelOn = false;
        if (((this.isLogical ^ 1) & (this.spacesRelativeToTextBeginEnd ^ 1)) != 0) {
            switch (lenOptionsLamAlef) {
                case 2:
                    lenOptionsLamAlef = 3;
                    break;
                case 3:
                    lenOptionsLamAlef = 2;
                    break;
            }
            if (lenOptionsTashkeel == 262144) {
                lenOptionsTashkeel = TASHKEEL_END;
            } else if (lenOptionsTashkeel == TASHKEEL_END) {
                lenOptionsTashkeel = 262144;
            }
        }
        int i2;
        int e;
        if (lenOptionsLamAlef == 1) {
            i2 = i;
            e = i2 + length2;
            while (i2 < e) {
                if (cArr[i2] == 65535) {
                    cArr[i2] = SPACE_CHAR;
                }
                i2++;
            }
        } else {
            i2 = i + length2;
            int wL = countSpaceSub(cArr, length2, 65535);
            int wT = countSpaceSub(cArr, length2, TASHKEEL_SPACE_SUB);
            if (lenOptionsLamAlef == 2) {
                lamAlefOn = true;
            }
            if (lenOptionsTashkeel == TASHKEEL_END) {
                tashkeelOn = true;
            }
            if (lamAlefOn && lenOptionsLamAlef == 2) {
                shiftArray(cArr, i, i2, 65535);
                while (wL > i) {
                    wL--;
                    cArr[wL] = SPACE_CHAR;
                }
            }
            if (tashkeelOn && lenOptionsTashkeel == TASHKEEL_END) {
                shiftArray(cArr, i, i2, TASHKEEL_SPACE_SUB);
                while (wT > i) {
                    wT--;
                    cArr[wT] = SPACE_CHAR;
                }
            }
            lamAlefOn = false;
            tashkeelOn = false;
            if (lenOptionsLamAlef == 0) {
                lamAlefOn = true;
            }
            if (lenOptionsTashkeel == 524288) {
                tashkeelOn = true;
            }
            if (lamAlefOn && lenOptionsLamAlef == 0) {
                shiftArray(cArr, i, i2, 65535);
                wL = flipArray(cArr, i, i2, wL);
                length2 = wL - i;
            }
            if (tashkeelOn && lenOptionsTashkeel == 524288) {
                shiftArray(cArr, i, i2, TASHKEEL_SPACE_SUB);
                wT = flipArray(cArr, i, i2, wT);
                length2 = wT - i;
            }
            lamAlefOn = false;
            tashkeelOn = false;
            if (lenOptionsLamAlef == 3 || lenOptionsLamAlef == 65536) {
                lamAlefOn = true;
            }
            if (lenOptionsTashkeel == 262144) {
                tashkeelOn = true;
            }
            if (lamAlefOn && (lenOptionsLamAlef == 3 || lenOptionsLamAlef == 65536)) {
                shiftArray(cArr, i, i2, 65535);
                e = flipArray(cArr, i, i2, wL);
                while (true) {
                    wL = e;
                    if (wL < i2) {
                        e = wL + 1;
                        cArr[wL] = SPACE_CHAR;
                    }
                }
            }
            if (tashkeelOn && lenOptionsTashkeel == 262144) {
                shiftArray(cArr, i, i2, TASHKEEL_SPACE_SUB);
                e = flipArray(cArr, i, i2, wT);
                while (e < i2) {
                    int wT2 = e + 1;
                    cArr[e] = SPACE_CHAR;
                    e = wT2;
                }
            }
        }
        return length2;
    }

    private boolean expandCompositCharAtBegin(char[] dest, int start, int length, int lacount) {
        if (lacount > countSpacesRight(dest, start, length)) {
            return true;
        }
        int r = (start + length) - lacount;
        int w = start + length;
        while (true) {
            r--;
            if (r < start) {
                return false;
            }
            char ch = dest[r];
            if (isNormalizedLamAlefChar(ch)) {
                w--;
                dest[w] = LAM_CHAR;
                w--;
                dest[w] = convertNormalizedLamAlef[ch - 1628];
            } else {
                w--;
                dest[w] = ch;
            }
        }
    }

    private boolean expandCompositCharAtEnd(char[] dest, int start, int length, int lacount) {
        if (lacount > countSpacesLeft(dest, start, length)) {
            return true;
        }
        int w = start;
        int e = start + length;
        for (int r = start + lacount; r < e; r++) {
            char ch = dest[r];
            int w2;
            if (isNormalizedLamAlefChar(ch)) {
                w2 = w + 1;
                dest[w] = convertNormalizedLamAlef[ch - 1628];
                w = w2 + 1;
                dest[w2] = LAM_CHAR;
            } else {
                w2 = w + 1;
                dest[w] = ch;
                w = w2;
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:16:0x0036, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:34:0x0074, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean expandCompositCharAtNear(char[] dest, int start, int length, int yehHamzaOption, int seenTailOption, int lamAlefOption) {
        if (isNormalizedLamAlefChar(dest[start])) {
            return true;
        }
        int i = start + length;
        while (true) {
            i--;
            if (i < start) {
                return false;
            }
            char ch = dest[i];
            if (lamAlefOption == 1 && isNormalizedLamAlefChar(ch)) {
                if (i > start && dest[i - 1] == SPACE_CHAR) {
                    dest[i] = LAM_CHAR;
                    i--;
                    dest[i] = convertNormalizedLamAlef[ch - 1628];
                }
            } else if (seenTailOption == 1 && isSeenTailFamilyChar(ch) == 1) {
                if (i > start && dest[i - 1] == SPACE_CHAR) {
                    dest[i - 1] = this.tailChar;
                }
            } else if (yehHamzaOption == 1 && isYehHamzaChar(ch)) {
                if (i > start && dest[i - 1] == SPACE_CHAR) {
                    dest[i] = yehHamzaToYeh[ch - 65161];
                    dest[i - 1] = HAMZAFE_CHAR;
                }
            }
        }
        return true;
    }

    private int expandCompositChar(char[] dest, int start, int length, int lacount, int shapingMode) throws ArabicShapingException {
        int i = start;
        int lenOptionsLamAlef = this.options & 65539;
        int lenOptionsSeen = this.options & SEEN_MASK;
        int lenOptionsYehHamza = this.options & YEHHAMZA_MASK;
        if (!(this.isLogical || this.spacesRelativeToTextBeginEnd)) {
            switch (lenOptionsLamAlef) {
                case 2:
                    lenOptionsLamAlef = 3;
                    break;
                case 3:
                    lenOptionsLamAlef = 2;
                    break;
            }
        }
        int lenOptionsLamAlef2 = lenOptionsLamAlef;
        if (shapingMode == 1) {
            if (lenOptionsLamAlef2 == 65536) {
                boolean spaceNotFound;
                boolean spaceNotFound2;
                if (this.isLogical) {
                    spaceNotFound = expandCompositCharAtEnd(dest, start, length, lacount);
                    if (spaceNotFound) {
                        spaceNotFound = expandCompositCharAtBegin(dest, start, length, lacount);
                    }
                    spaceNotFound2 = spaceNotFound;
                    if (spaceNotFound2) {
                        spaceNotFound2 = expandCompositCharAtNear(dest, i, length, 0, 0, 1);
                    }
                    if (spaceNotFound2) {
                        throw new ArabicShapingException("No spacefor lamalef");
                    }
                }
                spaceNotFound = expandCompositCharAtBegin(dest, start, length, lacount);
                if (spaceNotFound) {
                    spaceNotFound = expandCompositCharAtEnd(dest, start, length, lacount);
                }
                spaceNotFound2 = spaceNotFound;
                if (spaceNotFound2) {
                    spaceNotFound2 = expandCompositCharAtNear(dest, i, length, 0, 0, 1);
                }
                if (spaceNotFound2) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
            } else if (lenOptionsLamAlef2 == 2) {
                if (expandCompositCharAtEnd(dest, start, length, lacount)) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
            } else if (lenOptionsLamAlef2 == 3) {
                if (expandCompositCharAtBegin(dest, start, length, lacount)) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
            } else if (lenOptionsLamAlef2 == 1) {
                if (expandCompositCharAtNear(dest, i, length, 0, 0, 1)) {
                    throw new ArabicShapingException("No spacefor lamalef");
                }
            } else if (lenOptionsLamAlef2 == 0) {
                lenOptionsLamAlef = i + length;
                int w = lenOptionsLamAlef + lacount;
                while (true) {
                    lenOptionsLamAlef--;
                    if (lenOptionsLamAlef < i) {
                        return length + lacount;
                    }
                    char ch = dest[lenOptionsLamAlef];
                    if (isNormalizedLamAlefChar(ch)) {
                        w--;
                        dest[w] = LAM_CHAR;
                        w--;
                        dest[w] = convertNormalizedLamAlef[ch - 1628];
                    } else {
                        w--;
                        dest[w] = ch;
                    }
                }
            }
        } else if (lenOptionsSeen == 2097152 && expandCompositCharAtNear(dest, i, length, 0, 1, 0)) {
            throw new ArabicShapingException("No space for Seen tail expansion");
        } else if (lenOptionsYehHamza == 16777216 && expandCompositCharAtNear(dest, i, length, 1, 0, 0)) {
            throw new ArabicShapingException("No space for YehHamza expansion");
        }
        return length;
    }

    private int normalize(char[] dest, int start, int length) {
        int lacount = 0;
        int i = start;
        int e = i + length;
        while (i < e) {
            char ch = dest[i];
            if (ch >= 65136 && ch <= 65276) {
                if (isLamAlefChar(ch)) {
                    lacount++;
                }
                dest[i] = (char) convertFEto06[ch - 65136];
            }
            i++;
        }
        return lacount;
    }

    private int deshapeNormalize(char[] dest, int start, int length) {
        int lacount = 0;
        int i = 0;
        int yehHamzaComposeEnabled = (this.options & YEHHAMZA_MASK) == 16777216 ? 1 : 0;
        if ((this.options & SEEN_MASK) == 2097152) {
            i = 1;
        }
        int seenComposeEnabled = i;
        int i2 = start;
        i = i2 + length;
        while (i2 < i) {
            char ch = dest[i2];
            if (yehHamzaComposeEnabled == 1 && ((ch == HAMZA06_CHAR || ch == HAMZAFE_CHAR) && i2 < length - 1 && isAlefMaksouraChar(dest[i2 + 1]))) {
                dest[i2] = SPACE_CHAR;
                dest[i2 + 1] = YEH_HAMZA_CHAR;
            } else if (seenComposeEnabled == 1 && isTailChar(ch) && i2 < length - 1 && isSeenTailFamilyChar(dest[i2 + 1]) == 1) {
                dest[i2] = SPACE_CHAR;
            } else if (ch >= 65136 && ch <= 65276) {
                if (isLamAlefChar(ch)) {
                    lacount++;
                }
                dest[i2] = (char) convertFEto06[ch - 65136];
            }
            i2++;
        }
        return lacount;
    }

    private int shapeUnicode(char[] dest, int start, int length, int destSize, int tashkeelFlag) throws ArabicShapingException {
        int nw;
        int i = tashkeelFlag;
        int lamalef_count = normalize(dest, start, length);
        int i2 = 1;
        int i3 = (start + length) - 1;
        int nx = -2;
        boolean yehhamza_found = false;
        int nextLink = 0;
        int prevLink = 0;
        int lastLink = 0;
        int lastPos = i3;
        boolean lamalef_found = false;
        boolean seenfam_found = false;
        boolean tashkeel_found = false;
        int currLink = getLink(dest[i3]);
        int i4 = i3;
        while (i4 >= 0) {
            int nx2;
            if ((65280 & currLink) != 0 || isTashkeelChar(dest[i4])) {
                int i5;
                nw = i4 - 1;
                nx2 = -2;
                while (nx2 < 0) {
                    if (nw == -1) {
                        nextLink = 0;
                        nx2 = Integer.MAX_VALUE;
                    } else {
                        nextLink = getLink(dest[nw]);
                        if ((nextLink & 4) == 0) {
                            nx2 = nw;
                        } else {
                            nw--;
                        }
                    }
                }
                if ((currLink & 32) > 0 && (lastLink & 16) > 0) {
                    lamalef_found = true;
                    char wLamalef = changeLamAlef(dest[i4]);
                    if (wLamalef != 0) {
                        dest[i4] = 65535;
                        dest[lastPos] = wLamalef;
                        i4 = lastPos;
                    }
                    lastLink = prevLink;
                    currLink = getLink(wLamalef);
                }
                if (i4 <= 0 || dest[i4 - 1] != SPACE_CHAR) {
                    if (i4 == 0) {
                        if (isSeenFamilyChar(dest[i4]) == i2) {
                            seenfam_found = true;
                        } else if (dest[i4] == YEH_HAMZA_CHAR) {
                            yehhamza_found = true;
                        }
                    }
                } else if (isSeenFamilyChar(dest[i4]) == i2) {
                    seenfam_found = true;
                } else if (dest[i4] == YEH_HAMZA_CHAR) {
                    yehhamza_found = true;
                }
                int flag = specialChar(dest[i4]);
                int shape = shapeTable[nextLink & 3][lastLink & 3][currLink & 3];
                if (flag == i2) {
                    shape &= 1;
                } else if (flag == 2) {
                    if (i == 0 && (lastLink & 2) != 0 && (nextLink & 1) != 0 && dest[i4] != 1612 && dest[i4] != 1613 && ((nextLink & 32) != 32 || (lastLink & 16) != 16)) {
                        shape = 1;
                    } else if (i == 2 && dest[i4] == SHADDA06_CHAR) {
                        shape = 1;
                    } else {
                        shape = 0;
                    }
                }
                if (flag != 2) {
                    dest[i4] = (char) (((currLink >> 8) + 65136) + shape);
                } else if (i != 2 || dest[i4] == SHADDA06_CHAR) {
                    dest[i4] = (char) ((irrelevantPos[dest[i4] - 1611] + 65136) + shape);
                } else {
                    dest[i4] = TASHKEEL_SPACE_SUB;
                    i5 = nw;
                    tashkeel_found = true;
                }
                i5 = nw;
            } else {
                nx2 = nx;
            }
            if ((currLink & 4) == 0) {
                prevLink = lastLink;
                lastLink = currLink;
                lastPos = i4;
            }
            i4--;
            if (i4 == nx2) {
                currLink = nextLink;
                nx = -2;
            } else {
                if (i4 != -1) {
                    currLink = getLink(dest[i4]);
                }
                nx = nx2;
            }
            i2 = 1;
        }
        nw = length;
        if (lamalef_found || tashkeel_found) {
            nw = handleGeneratedSpaces(dest, start, length);
        }
        int destSize2 = nw;
        if (seenfam_found || yehhamza_found) {
            return expandCompositChar(dest, start, destSize2, lamalef_count, 0);
        }
        return destSize2;
    }

    private int deShapeUnicode(char[] dest, int start, int length, int destSize) throws ArabicShapingException {
        int lamalef_count = deshapeNormalize(dest, start, length);
        if (lamalef_count != 0) {
            return expandCompositChar(dest, start, length, lamalef_count, 1);
        }
        return length;
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x0104  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0083  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int internalShape(char[] source, int sourceStart, int sourceLength, char[] dest, int destStart, int destSize) throws ArabicShapingException {
        int i = sourceLength;
        int i2 = destSize;
        if (i == 0) {
            return 0;
        }
        if (i2 != 0) {
            int outputSize;
            char[] temp = new char[(i * 2)];
            System.arraycopy(source, sourceStart, temp, 0, i);
            if (this.isLogical) {
                invertBuffer(temp, 0, i);
            }
            int outputSize2 = i;
            int i3 = this.options & 24;
            if (i3 != 8) {
                if (i3 != 16) {
                    if (i3 == 24) {
                        outputSize = shapeUnicode(temp, 0, i, i2, 1);
                    }
                    if (outputSize2 > i2) {
                        if ((this.options & 224) != 0) {
                            char digitBase = '0';
                            i3 = this.options & 256;
                            if (i3 == 0) {
                                digitBase = 1632;
                            } else if (i3 == 256) {
                                digitBase = 1776;
                            }
                            char digitBase2 = digitBase;
                            outputSize = this.options & 224;
                            char ch;
                            if (outputSize == 32) {
                                outputSize = digitBase2 - 48;
                                for (i3 = 0; i3 < outputSize2; i3++) {
                                    ch = temp[i3];
                                    if (ch <= '9' && ch >= '0') {
                                        temp[i3] = (char) (temp[i3] + outputSize);
                                    }
                                }
                            } else if (outputSize == 64) {
                                digitBase = (char) (digitBase2 + 9);
                                int digitDelta = 48 - digitBase2;
                                for (i3 = 0; i3 < outputSize2; i3++) {
                                    ch = temp[i3];
                                    if (ch <= digitBase && ch >= digitBase2) {
                                        temp[i3] = (char) (temp[i3] + digitDelta);
                                    }
                                }
                            } else if (outputSize == 96) {
                                shapeToArabicDigitsWithContext(temp, 0, outputSize2, digitBase2, false);
                            } else if (outputSize == 128) {
                                shapeToArabicDigitsWithContext(temp, 0, outputSize2, digitBase2, true);
                            }
                        }
                        if (this.isLogical) {
                            invertBuffer(temp, 0, outputSize2);
                        }
                        System.arraycopy(temp, 0, dest, destStart, outputSize2);
                        return outputSize2;
                    }
                    char[] cArr = dest;
                    i3 = destStart;
                    throw new ArabicShapingException("not enough room for result data");
                }
                outputSize = deShapeUnicode(temp, 0, i, i2);
            } else if ((this.options & TASHKEEL_MASK) == 0 || (this.options & TASHKEEL_MASK) == TASHKEEL_REPLACE_BY_TATWEEL) {
                outputSize = shapeUnicode(temp, 0, i, i2, 0);
                if ((this.options & TASHKEEL_MASK) == TASHKEEL_REPLACE_BY_TATWEEL) {
                    outputSize = handleTashkeelWithTatweel(temp, i);
                }
            } else {
                outputSize = shapeUnicode(temp, 0, i, i2, 2);
            }
            outputSize2 = outputSize;
            if (outputSize2 > i2) {
            }
        } else if ((24 & this.options) == 0 || (this.options & 65539) != 0) {
            return i;
        } else {
            return calculateSize(source, sourceStart, sourceLength);
        }
    }
}
