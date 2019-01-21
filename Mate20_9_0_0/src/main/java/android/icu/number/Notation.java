package android.icu.number;

import android.icu.number.NumberFormatter.SignDisplay;
import android.icu.text.CompactDecimalFormat.CompactStyle;

public class Notation {
    private static final CompactNotation COMPACT_LONG = new CompactNotation(CompactStyle.LONG);
    private static final CompactNotation COMPACT_SHORT = new CompactNotation(CompactStyle.SHORT);
    private static final ScientificNotation ENGINEERING = new ScientificNotation(3, false, 1, SignDisplay.AUTO);
    private static final ScientificNotation SCIENTIFIC = new ScientificNotation(1, false, 1, SignDisplay.AUTO);
    private static final SimpleNotation SIMPLE = new SimpleNotation();

    Notation() {
    }

    public static ScientificNotation scientific() {
        return SCIENTIFIC;
    }

    public static ScientificNotation engineering() {
        return ENGINEERING;
    }

    public static CompactNotation compactShort() {
        return COMPACT_SHORT;
    }

    public static CompactNotation compactLong() {
        return COMPACT_LONG;
    }

    public static SimpleNotation simple() {
        return SIMPLE;
    }
}
