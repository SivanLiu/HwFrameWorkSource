package android.icu.number;

public class IntegerWidth {
    static final IntegerWidth DEFAULT = new IntegerWidth(1, -1);
    final int maxInt;
    final int minInt;

    private IntegerWidth(int minInt, int maxInt) {
        this.minInt = minInt;
        this.maxInt = maxInt;
    }

    public static IntegerWidth zeroFillTo(int minInt) {
        if (minInt == 1) {
            return DEFAULT;
        }
        if (minInt >= 0 && minInt < 100) {
            return new IntegerWidth(minInt, -1);
        }
        throw new IllegalArgumentException("Integer digits must be between 0 and 100");
    }

    public IntegerWidth truncateAt(int maxInt) {
        if (maxInt == this.maxInt) {
            return this;
        }
        if (maxInt >= 0 && maxInt < 100) {
            return new IntegerWidth(this.minInt, maxInt);
        }
        if (maxInt == -1) {
            return new IntegerWidth(this.minInt, maxInt);
        }
        throw new IllegalArgumentException("Integer digits must be between 0 and 100");
    }
}
