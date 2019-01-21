package android.icu.impl.duration.impl;

import android.icu.impl.duration.TimeUnit;
import android.icu.impl.duration.impl.DataRecord.ScopeData;
import android.icu.impl.duration.impl.Utils.ChineseDigits;
import android.icu.text.BreakIterator;
import java.io.PrintStream;
import java.util.Arrays;

public class PeriodFormatterData {
    private static final int FORM_DUAL = 2;
    private static final int FORM_HALF_SPELLED = 6;
    private static final int FORM_PAUCAL = 3;
    private static final int FORM_PLURAL = 0;
    private static final int FORM_SINGULAR = 1;
    private static final int FORM_SINGULAR_NO_OMIT = 5;
    private static final int FORM_SINGULAR_SPELLED = 4;
    public static boolean trace = false;
    final DataRecord dr;
    String localeName;

    public PeriodFormatterData(String localeName, DataRecord dr) {
        this.dr = dr;
        this.localeName = localeName;
        if (localeName == null) {
            throw new NullPointerException("localename is null");
        } else if (dr == null) {
            throw new NullPointerException("data record is null");
        }
    }

    public int pluralization() {
        return this.dr.pl;
    }

    public boolean allowZero() {
        return this.dr.allowZero;
    }

    public boolean weeksAloneOnly() {
        return this.dr.weeksAloneOnly;
    }

    public int useMilliseconds() {
        return this.dr.useMilliseconds;
    }

    public boolean appendPrefix(int tl, int td, StringBuffer sb) {
        if (this.dr.scopeData != null) {
            ScopeData sd = this.dr.scopeData[(tl * 3) + td];
            if (sd != null) {
                String prefix = sd.prefix;
                if (prefix != null) {
                    sb.append(prefix);
                    return sd.requiresDigitPrefix;
                }
            }
        }
        return false;
    }

    public void appendSuffix(int tl, int td, StringBuffer sb) {
        if (this.dr.scopeData != null) {
            ScopeData sd = this.dr.scopeData[(tl * 3) + td];
            if (sd != null) {
                String suffix = sd.suffix;
                if (suffix != null) {
                    if (trace) {
                        PrintStream printStream = System.out;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("appendSuffix '");
                        stringBuilder.append(suffix);
                        stringBuilder.append("'");
                        printStream.println(stringBuilder.toString());
                    }
                    sb.append(suffix);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:39:0x008d, code skipped:
            if (r0 > 1000) goto L_0x0097;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean appendUnit(TimeUnit unit, int count, int cv, int uv, boolean useCountSep, boolean useDigitPrefix, boolean multiple, boolean last, boolean wasSkipped, StringBuffer sb) {
        String name;
        int i = count;
        int i2 = uv;
        StringBuffer stringBuffer = sb;
        int px = unit.ordinal();
        boolean willRequireSkipMarker = false;
        if (!(this.dr.requiresSkipMarker == null || !this.dr.requiresSkipMarker[px] || this.dr.skippedUnitMarker == null)) {
            if (!wasSkipped && last) {
                stringBuffer.append(this.dr.skippedUnitMarker);
            }
            willRequireSkipMarker = true;
        }
        boolean willRequireSkipMarker2 = willRequireSkipMarker;
        if (i2 != 0) {
            boolean useMedium = i2 == 1;
            String[] names = useMedium ? this.dr.mediumNames : this.dr.shortNames;
            if (names == null || names[px] == null) {
                names = useMedium ? this.dr.shortNames : this.dr.mediumNames;
            }
            String[] names2 = names;
            if (!(names2 == null || names2[px] == null)) {
                appendCount(unit, false, false, i, cv, useCountSep, names2[px], last, stringBuffer);
                return false;
            }
        }
        int i3 = cv;
        if (i3 == 2 && this.dr.halfSupport != null) {
            switch (this.dr.halfSupport[px]) {
                case (byte) 2:
                    break;
                case (byte) 1:
                    i = (i / BreakIterator.WORD_IDEO_LIMIT) * BreakIterator.WORD_IDEO_LIMIT;
                    i3 = 3;
                    break;
            }
        }
        int count2 = i;
        int cv2 = i3;
        boolean z = multiple && last;
        TimeUnit timeUnit = unit;
        int form = computeForm(timeUnit, count2, cv2, z);
        if (form == 4) {
            if (this.dr.singularNames == null) {
                form = 1;
                name = this.dr.pluralNames[px][1];
            } else {
                name = this.dr.singularNames[px];
            }
        } else if (form == 5) {
            name = this.dr.pluralNames[px][1];
        } else if (form == 6) {
            name = this.dr.halfNames[px];
        } else {
            try {
                name = this.dr.pluralNames[px][form];
            } catch (NullPointerException e) {
                int i4 = cv2;
                int i5 = count2;
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Null Pointer in PeriodFormatterData[");
                stringBuilder.append(this.localeName);
                stringBuilder.append("].au px: ");
                stringBuilder.append(px);
                stringBuilder.append(" form: ");
                stringBuilder.append(form);
                stringBuilder.append(" pn: ");
                stringBuilder.append(Arrays.toString(this.dr.pluralNames));
                printStream.println(stringBuilder.toString());
                throw e;
            }
        }
        if (name == null) {
            form = 0;
            name = this.dr.pluralNames[px][0];
        }
        String name2 = name;
        int form2 = form;
        name = (form2 == 4 || form2 == 6 || ((this.dr.omitSingularCount && form2 == 1) || (this.dr.omitDualCount && form2 == 2))) ? 1 : null;
        int suffixIndex = appendCount(timeUnit, name, useDigitPrefix, count2, cv2, useCountSep, name2, last, stringBuffer);
        if (last && suffixIndex >= 0) {
            String suffix = null;
            if (this.dr.rqdSuffixes != null && suffixIndex < this.dr.rqdSuffixes.length) {
                suffix = this.dr.rqdSuffixes[suffixIndex];
            }
            if (suffix == null && this.dr.optSuffixes != null && suffixIndex < this.dr.optSuffixes.length) {
                suffix = this.dr.optSuffixes[suffixIndex];
            }
            if (suffix != null) {
                stringBuffer.append(suffix);
            }
        }
        return willRequireSkipMarker2;
    }

    public int appendCount(TimeUnit unit, boolean omitCount, boolean useDigitPrefix, int count, int cv, boolean useSep, String name, boolean last, StringBuffer sb) {
        int i;
        int i2 = count;
        String name2 = name;
        StringBuffer stringBuffer = sb;
        int cv2 = cv;
        if (cv2 == 2 && this.dr.halves == null) {
            cv2 = 0;
        }
        if (!(omitCount || !useDigitPrefix || this.dr.digitPrefix == null)) {
            stringBuffer.append(this.dr.digitPrefix);
        }
        int index = unit.ordinal();
        TimeUnit timeUnit;
        int val;
        switch (cv2) {
            case 0:
                timeUnit = unit;
                if (!omitCount) {
                    appendInteger(i2 / 1000, 1, 10, stringBuffer);
                    break;
                }
                break;
            case 1:
                val = i2 / 1000;
                if (unit == TimeUnit.MINUTE && !((this.dr.fiveMinutes == null && this.dr.fifteenMinutes == null) || val == 0 || val % 5 != 0)) {
                    if (this.dr.fifteenMinutes == null || (val != 15 && val != 45)) {
                        if (this.dr.fiveMinutes != null) {
                            val /= 5;
                            if (!omitCount) {
                                appendInteger(val, 1, 10, stringBuffer);
                            }
                            name2 = this.dr.fiveMinutes;
                            index = 9;
                            break;
                        }
                    }
                    val = val == 15 ? 1 : 3;
                    if (!omitCount) {
                        appendInteger(val, 1, 10, stringBuffer);
                    }
                    name2 = this.dr.fifteenMinutes;
                    index = 8;
                    break;
                }
                if (!omitCount) {
                    appendInteger(val, 1, 10, stringBuffer);
                    break;
                }
                break;
            case 2:
                int v = i2 / BreakIterator.WORD_IDEO_LIMIT;
                if (!(v == 1 || omitCount)) {
                    appendCountValue(i2, 1, 0, stringBuffer);
                }
                if ((v & 1) == 1) {
                    if (v != 1 || this.dr.halfNames == null || this.dr.halfNames[index] == null) {
                        int solox = v == 1 ? 0 : 1;
                        if (this.dr.genders != null && this.dr.halves.length > 2 && this.dr.genders[index] == (byte) 1) {
                            solox += 2;
                        }
                        byte hp = this.dr.halfPlacements == null ? (byte) 0 : this.dr.halfPlacements[solox & 1];
                        String half = this.dr.halves[solox];
                        String measure = this.dr.measures == null ? null : this.dr.measures[index];
                        switch (hp) {
                            case (byte) 0:
                                stringBuffer.append(half);
                                break;
                            case (byte) 1:
                                if (measure != null) {
                                    stringBuffer.append(measure);
                                    stringBuffer.append(half);
                                    if (useSep && !omitCount) {
                                        stringBuffer.append(this.dr.countSep);
                                    }
                                    stringBuffer.append(name2);
                                    return -1;
                                }
                                i = -1;
                                stringBuffer.append(name2);
                                stringBuffer.append(half);
                                if (last) {
                                    i = index;
                                }
                                return i;
                            case (byte) 2:
                                if (measure != null) {
                                    stringBuffer.append(measure);
                                }
                                if (useSep && !omitCount) {
                                    stringBuffer.append(this.dr.countSep);
                                }
                                stringBuffer.append(name2);
                                stringBuffer.append(half);
                                return last ? index : -1;
                        }
                    }
                    stringBuffer.append(name2);
                    return last ? index : -1;
                }
                timeUnit = unit;
                break;
            default:
                timeUnit = unit;
                val = 1;
                switch (cv2) {
                    case 4:
                        val = 2;
                        break;
                    case 5:
                        val = 3;
                        break;
                }
                if (!omitCount) {
                    appendCountValue(i2, 1, val, stringBuffer);
                    break;
                }
                break;
        }
        i = index;
        if (!omitCount && useSep) {
            stringBuffer.append(this.dr.countSep);
        }
        if (!(omitCount || this.dr.measures == null || i >= this.dr.measures.length)) {
            String measure2 = this.dr.measures[i];
            if (measure2 != null) {
                stringBuffer.append(measure2);
            }
        }
        stringBuffer.append(name2);
        return last ? i : -1;
    }

    public void appendCountValue(int count, int integralDigits, int decimalDigits, StringBuffer sb) {
        int ival = count / 1000;
        if (decimalDigits == 0) {
            appendInteger(ival, integralDigits, 10, sb);
            return;
        }
        if (this.dr.requiresDigitSeparator && sb.length() > 0) {
            sb.append(' ');
        }
        appendDigits((long) ival, integralDigits, 10, sb);
        int dval = count % 1000;
        if (decimalDigits == 1) {
            dval /= 100;
        } else if (decimalDigits == 2) {
            dval /= 10;
        }
        sb.append(this.dr.decimalSep);
        appendDigits((long) dval, decimalDigits, decimalDigits, sb);
        if (this.dr.requiresDigitSeparator) {
            sb.append(' ');
        }
    }

    public void appendInteger(int num, int mindigits, int maxdigits, StringBuffer sb) {
        if (this.dr.numberNames != null && num < this.dr.numberNames.length) {
            String name = this.dr.numberNames[num];
            if (name != null) {
                sb.append(name);
                return;
            }
        }
        if (this.dr.requiresDigitSeparator && sb.length() > 0) {
            sb.append(' ');
        }
        switch (this.dr.numberSystem) {
            case (byte) 0:
                appendDigits((long) num, mindigits, maxdigits, sb);
                break;
            case (byte) 1:
                sb.append(Utils.chineseNumber((long) num, ChineseDigits.TRADITIONAL));
                break;
            case (byte) 2:
                sb.append(Utils.chineseNumber((long) num, ChineseDigits.SIMPLIFIED));
                break;
            case (byte) 3:
                sb.append(Utils.chineseNumber((long) num, ChineseDigits.KOREAN));
                break;
        }
        if (this.dr.requiresDigitSeparator) {
            sb.append(' ');
        }
    }

    public void appendDigits(long num, int mindigits, int maxdigits, StringBuffer sb) {
        char[] buf = new char[maxdigits];
        long num2 = num;
        int ix = maxdigits;
        while (ix > 0 && num2 > 0) {
            ix--;
            buf[ix] = (char) ((int) (((long) this.dr.zero) + (num2 % 10)));
            num2 /= 10;
        }
        int e = maxdigits - mindigits;
        while (ix > e) {
            ix--;
            buf[ix] = this.dr.zero;
        }
        sb.append(buf, ix, maxdigits - ix);
    }

    public void appendSkippedUnit(StringBuffer sb) {
        if (this.dr.skippedUnitMarker != null) {
            sb.append(this.dr.skippedUnitMarker);
        }
    }

    public boolean appendUnitSeparator(TimeUnit unit, boolean longSep, boolean afterFirst, boolean beforeLast, StringBuffer sb) {
        boolean z = false;
        if ((longSep && this.dr.unitSep != null) || this.dr.shortUnitSep != null) {
            if (!longSep || this.dr.unitSep == null) {
                sb.append(this.dr.shortUnitSep);
            } else {
                int ix = (afterFirst ? 2 : 0) + beforeLast;
                sb.append(this.dr.unitSep[ix]);
                if (this.dr.unitSepRequiresDP != null && this.dr.unitSepRequiresDP[ix]) {
                    z = true;
                }
                return z;
            }
        }
        return false;
    }

    private int computeForm(TimeUnit unit, int count, int cv, boolean lastOfMultiple) {
        if (trace) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pfd.cf unit: ");
            stringBuilder.append(unit);
            stringBuilder.append(" count: ");
            stringBuilder.append(count);
            stringBuilder.append(" cv: ");
            stringBuilder.append(cv);
            stringBuilder.append(" dr.pl: ");
            stringBuilder.append(this.dr.pl);
            printStream.println(stringBuilder.toString());
            Thread.dumpStack();
        }
        if (this.dr.pl == (byte) 0) {
            return 0;
        }
        int v;
        StringBuilder stringBuilder2;
        int val = count / 1000;
        switch (cv) {
            case 0:
            case 1:
                break;
            case 2:
                switch (this.dr.fractionHandling) {
                    case (byte) 0:
                        return 0;
                    case (byte) 1:
                    case (byte) 2:
                        int v2 = count / BreakIterator.WORD_IDEO_LIMIT;
                        if (v2 == 1) {
                            if (this.dr.halfNames == null || this.dr.halfNames[unit.ordinal()] == null) {
                                return 5;
                            }
                            return 6;
                        } else if ((v2 & 1) == 1) {
                            if (this.dr.pl == (byte) 5 && v2 > 21) {
                                return 5;
                            }
                            if (v2 == 3 && this.dr.pl == (byte) 1 && this.dr.fractionHandling != (byte) 2) {
                                return 0;
                            }
                        }
                        break;
                    case (byte) 3:
                        v = count / BreakIterator.WORD_IDEO_LIMIT;
                        if (v == 1 || v == 3) {
                            return 3;
                        }
                    default:
                        throw new IllegalStateException();
                }
                break;
            default:
                switch (this.dr.decimalHandling) {
                    case (byte) 1:
                        return 5;
                    case (byte) 2:
                        if (count < 1000) {
                            return 5;
                        }
                        break;
                    case (byte) 3:
                        if (this.dr.pl == (byte) 3) {
                            return 3;
                        }
                        break;
                }
                return 0;
        }
        if (trace && count == 0) {
            PrintStream printStream2 = System.err;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("EZeroHandling = ");
            stringBuilder2.append(this.dr.zeroHandling);
            printStream2.println(stringBuilder2.toString());
        }
        if (count == 0 && this.dr.zeroHandling == (byte) 1) {
            return 4;
        }
        v = 0;
        switch (this.dr.pl) {
            case (byte) 0:
                break;
            case (byte) 1:
                if (val == 1) {
                    v = 4;
                    break;
                }
                break;
            case (byte) 2:
                if (val != 2) {
                    if (val == 1) {
                        v = 1;
                        break;
                    }
                }
                v = 2;
                break;
                break;
            case (byte) 3:
                int v3 = val % 100;
                if (v3 > 20) {
                    v3 %= 10;
                }
                if (v3 != 1) {
                    if (v3 > 1 && v3 < 5) {
                        v = 3;
                        break;
                    }
                }
                v = 1;
                break;
            case (byte) 4:
                if (val != 2) {
                    if (val != 1) {
                        if (unit == TimeUnit.YEAR && val > 11) {
                            v = 5;
                            break;
                        }
                    } else if (!lastOfMultiple) {
                        v = 1;
                        break;
                    } else {
                        v = 4;
                        break;
                    }
                }
                v = 2;
                break;
            case (byte) 5:
                if (val != 2) {
                    if (val != 1) {
                        if (val > 10) {
                            v = 5;
                            break;
                        }
                    }
                    v = 1;
                    break;
                }
                v = 2;
                break;
                break;
            default:
                PrintStream printStream3 = System.err;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("dr.pl is ");
                stringBuilder2.append(this.dr.pl);
                printStream3.println(stringBuilder2.toString());
                throw new IllegalStateException();
        }
        return v;
    }
}
