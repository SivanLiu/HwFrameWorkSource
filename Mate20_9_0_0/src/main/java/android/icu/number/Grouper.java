package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.PatternStringParser.ParsedPatternInfo;

@Deprecated
public class Grouper {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final byte B2 = (byte) 2;
    private static final byte B3 = (byte) 3;
    private static final Grouper DEFAULTS = new Grouper(N2, N2, false);
    private static final Grouper GROUPING_3 = new Grouper((byte) 3, (byte) 3, false);
    private static final Grouper GROUPING_3_2 = new Grouper((byte) 3, (byte) 2, false);
    private static final Grouper GROUPING_3_2_MIN2 = new Grouper((byte) 3, (byte) 2, true);
    private static final Grouper GROUPING_3_MIN2 = new Grouper((byte) 3, (byte) 3, true);
    private static final Grouper MIN2 = new Grouper(N2, N2, true);
    private static final byte N1 = (byte) -1;
    private static final byte N2 = (byte) -2;
    private static final Grouper NONE = new Grouper((byte) -1, (byte) -1, false);
    private final byte grouping1;
    private final byte grouping2;
    private final boolean min2;

    private Grouper(byte grouping1, byte grouping2, boolean min2) {
        this.grouping1 = grouping1;
        this.grouping2 = grouping2;
        this.min2 = min2;
    }

    @Deprecated
    public static Grouper defaults() {
        return DEFAULTS;
    }

    @Deprecated
    public static Grouper minTwoDigits() {
        return MIN2;
    }

    @Deprecated
    public static Grouper none() {
        return NONE;
    }

    static Grouper getInstance(byte grouping1, byte grouping2, boolean min2) {
        if (grouping1 == (byte) -1) {
            return NONE;
        }
        if (!min2 && grouping1 == (byte) 3 && grouping2 == (byte) 3) {
            return GROUPING_3;
        }
        if (!min2 && grouping1 == (byte) 3 && grouping2 == (byte) 2) {
            return GROUPING_3_2;
        }
        if (min2 && grouping1 == (byte) 3 && grouping2 == (byte) 3) {
            return GROUPING_3_MIN2;
        }
        if (min2 && grouping1 == (byte) 3 && grouping2 == (byte) 2) {
            return GROUPING_3_2_MIN2;
        }
        return new Grouper(grouping1, grouping2, min2);
    }

    Grouper withLocaleData(ParsedPatternInfo patternInfo) {
        if (this.grouping1 != N2) {
            return this;
        }
        byte grouping1 = (byte) ((int) (patternInfo.positive.groupingSizes & 65535));
        byte grouping2 = (byte) ((int) ((patternInfo.positive.groupingSizes >>> 16) & 65535));
        byte grouping3 = (byte) ((int) (65535 & (patternInfo.positive.groupingSizes >>> 32)));
        if (grouping2 == (byte) -1) {
            grouping1 = (byte) -1;
        }
        if (grouping3 == (byte) -1) {
            grouping2 = grouping1;
        }
        return getInstance(grouping1, grouping2, this.min2);
    }

    /* JADX WARNING: Missing block: B:12:0x0027, code skipped:
            if (((r6.getUpperDisplayMagnitude() - r4.grouping1) + 1) >= (r4.min2 ? true : true)) goto L_0x002b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean groupAtPosition(int position, DecimalQuantity value) {
        if (this.grouping1 == (byte) -1 || this.grouping1 == (byte) 0) {
            return false;
        }
        position -= this.grouping1;
        boolean z = true;
        if (position >= 0 && position % this.grouping2 == 0) {
        }
        z = false;
        return z;
    }
}
