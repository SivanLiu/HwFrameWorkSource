package android.icu.impl.number;

public class Padder {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final String FALLBACK_PADDING_STRING = " ";
    public static final Padder NONE = new Padder(null, -1, null);
    String paddingString;
    PadPosition position;
    int targetWidth;

    public enum PadPosition {
        BEFORE_PREFIX,
        AFTER_PREFIX,
        BEFORE_SUFFIX,
        AFTER_SUFFIX;

        public static PadPosition fromOld(int old) {
            switch (old) {
                case 0:
                    return BEFORE_PREFIX;
                case 1:
                    return AFTER_PREFIX;
                case 2:
                    return BEFORE_SUFFIX;
                case 3:
                    return AFTER_SUFFIX;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Don't know how to map ");
                    stringBuilder.append(old);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public int toOld() {
            switch (this) {
                case BEFORE_PREFIX:
                    return 0;
                case AFTER_PREFIX:
                    return 1;
                case BEFORE_SUFFIX:
                    return 2;
                case AFTER_SUFFIX:
                    return 3;
                default:
                    return -1;
            }
        }
    }

    public Padder(String paddingString, int targetWidth, PadPosition position) {
        this.paddingString = paddingString == null ? FALLBACK_PADDING_STRING : paddingString;
        this.targetWidth = targetWidth;
        this.position = position == null ? PadPosition.BEFORE_PREFIX : position;
    }

    public static Padder none() {
        return NONE;
    }

    public static Padder codePoints(int cp, int targetWidth, PadPosition position) {
        if (targetWidth >= 0) {
            return new Padder(String.valueOf(Character.toChars(cp)), targetWidth, position);
        }
        throw new IllegalArgumentException("Padding width must not be negative");
    }

    public boolean isValid() {
        return this.targetWidth > 0;
    }

    public int padAndApply(Modifier mod1, Modifier mod2, NumberStringBuilder string, int leftIndex, int rightIndex) {
        int requiredPadding = (this.targetWidth - (mod1.getCodePointCount() + mod2.getCodePointCount())) - string.codePointCount();
        int length = 0;
        if (requiredPadding <= 0) {
            length = 0 + mod1.apply(string, leftIndex, rightIndex);
            return length + mod2.apply(string, leftIndex, rightIndex + length);
        }
        if (this.position == PadPosition.AFTER_PREFIX) {
            length = 0 + addPaddingHelper(this.paddingString, requiredPadding, string, leftIndex);
        } else if (this.position == PadPosition.BEFORE_SUFFIX) {
            length = 0 + addPaddingHelper(this.paddingString, requiredPadding, string, rightIndex + 0);
        }
        length += mod1.apply(string, leftIndex, rightIndex + length);
        length += mod2.apply(string, leftIndex, rightIndex + length);
        if (this.position == PadPosition.BEFORE_PREFIX) {
            length += addPaddingHelper(this.paddingString, requiredPadding, string, leftIndex);
        } else if (this.position == PadPosition.AFTER_SUFFIX) {
            length += addPaddingHelper(this.paddingString, requiredPadding, string, rightIndex + length);
        }
        return length;
    }

    private static int addPaddingHelper(String paddingString, int requiredPadding, NumberStringBuilder string, int index) {
        for (int i = 0; i < requiredPadding; i++) {
            string.insert(index, (CharSequence) paddingString, null);
        }
        return paddingString.length() * requiredPadding;
    }
}
