package android.text.method;

import android.icu.lang.UCharacter;
import android.icu.text.DecimalFormatSymbols;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

public class DigitsKeyListener extends NumberKeyListener {
    private static final char[][] COMPATIBILITY_CHARACTERS = new char[][]{new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}, new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', HYPHEN_MINUS, '+'}, new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'}, new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', HYPHEN_MINUS, '+', '.'}};
    private static final int DECIMAL = 2;
    private static final String DEFAULT_DECIMAL_POINT_CHARS = ".";
    private static final String DEFAULT_SIGN_CHARS = "-+";
    private static final char EN_DASH = '–';
    private static final char HYPHEN_MINUS = '-';
    private static final char MINUS_SIGN = '−';
    private static final int SIGN = 1;
    private static final Object sLocaleCacheLock = new Object();
    @GuardedBy("sLocaleCacheLock")
    private static final HashMap<Locale, DigitsKeyListener[]> sLocaleInstanceCache = new HashMap();
    private static final Object sStringCacheLock = new Object();
    @GuardedBy("sStringCacheLock")
    private static final HashMap<String, DigitsKeyListener> sStringInstanceCache = new HashMap();
    private char[] mAccepted;
    private final boolean mDecimal;
    private String mDecimalPointChars;
    private final Locale mLocale;
    private boolean mNeedsAdvancedInput;
    private final boolean mSign;
    private String mSignChars;
    private final boolean mStringMode;

    protected char[] getAcceptedChars() {
        return this.mAccepted;
    }

    private boolean isSignChar(char c) {
        return this.mSignChars.indexOf(c) != -1;
    }

    private boolean isDecimalPointChar(char c) {
        return this.mDecimalPointChars.indexOf(c) != -1;
    }

    @Deprecated
    public DigitsKeyListener() {
        this(null, false, false);
    }

    @Deprecated
    public DigitsKeyListener(boolean sign, boolean decimal) {
        this(null, sign, decimal);
    }

    public DigitsKeyListener(Locale locale) {
        this(locale, false, false);
    }

    private void setToCompat() {
        this.mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
        this.mSignChars = DEFAULT_SIGN_CHARS;
        this.mAccepted = COMPATIBILITY_CHARACTERS[this.mSign | (this.mDecimal ? 2 : 0)];
        this.mNeedsAdvancedInput = false;
    }

    private void calculateNeedForAdvancedInput() {
        this.mNeedsAdvancedInput = ArrayUtils.containsAll(COMPATIBILITY_CHARACTERS[this.mSign | (this.mDecimal ? 2 : 0)], this.mAccepted) ^ 1;
    }

    private static String stripBidiControls(String sign) {
        String result = "";
        for (int i = 0; i < sign.length(); i++) {
            char c = sign.charAt(i);
            if (!UCharacter.hasBinaryProperty(c, 2)) {
                if (result.isEmpty()) {
                    result = String.valueOf(c);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(result);
                    stringBuilder.append(c);
                    result = stringBuilder.toString();
                }
            }
        }
        return result;
    }

    public DigitsKeyListener(Locale locale, boolean sign, boolean decimal) {
        this.mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
        this.mSignChars = DEFAULT_SIGN_CHARS;
        this.mSign = sign;
        this.mDecimal = decimal;
        this.mStringMode = false;
        this.mLocale = locale;
        if (locale == null) {
            setToCompat();
            return;
        }
        LinkedHashSet<Character> chars = new LinkedHashSet();
        if (NumberKeyListener.addDigits(chars, locale)) {
            if (sign || decimal) {
                String minusString;
                DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
                if (sign) {
                    minusString = stripBidiControls(symbols.getMinusSignString());
                    String plusString = stripBidiControls(symbols.getPlusSignString());
                    if (minusString.length() > 1 || plusString.length() > 1) {
                        setToCompat();
                        return;
                    }
                    char minus = minusString.charAt(0);
                    char plus = plusString.charAt(0);
                    chars.add(Character.valueOf(minus));
                    chars.add(Character.valueOf(plus));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(minus);
                    stringBuilder.append(plus);
                    this.mSignChars = stringBuilder.toString();
                    if (minus == MINUS_SIGN || minus == EN_DASH) {
                        chars.add(Character.valueOf(HYPHEN_MINUS));
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.mSignChars);
                        stringBuilder2.append(HYPHEN_MINUS);
                        this.mSignChars = stringBuilder2.toString();
                    }
                }
                if (decimal) {
                    minusString = symbols.getDecimalSeparatorString();
                    if (minusString.length() > 1) {
                        setToCompat();
                        return;
                    }
                    Character separatorChar = Character.valueOf(minusString.charAt(0));
                    chars.add(separatorChar);
                    this.mDecimalPointChars = separatorChar.toString();
                }
            }
            this.mAccepted = NumberKeyListener.collectionToArray(chars);
            calculateNeedForAdvancedInput();
            return;
        }
        setToCompat();
    }

    private DigitsKeyListener(String accepted) {
        this.mDecimalPointChars = DEFAULT_DECIMAL_POINT_CHARS;
        this.mSignChars = DEFAULT_SIGN_CHARS;
        this.mSign = false;
        this.mDecimal = false;
        this.mStringMode = true;
        this.mLocale = null;
        this.mAccepted = new char[accepted.length()];
        accepted.getChars(0, accepted.length(), this.mAccepted, 0);
        this.mNeedsAdvancedInput = false;
    }

    @Deprecated
    public static DigitsKeyListener getInstance() {
        return getInstance(false, false);
    }

    @Deprecated
    public static DigitsKeyListener getInstance(boolean sign, boolean decimal) {
        return getInstance(null, sign, decimal);
    }

    public static DigitsKeyListener getInstance(Locale locale) {
        return getInstance(locale, false, false);
    }

    public static DigitsKeyListener getInstance(Locale locale, boolean sign, boolean decimal) {
        int kind = (decimal ? 2 : 0) | sign;
        synchronized (sLocaleCacheLock) {
            DigitsKeyListener[] cachedValue = (DigitsKeyListener[]) sLocaleInstanceCache.get(locale);
            DigitsKeyListener digitsKeyListener;
            if (cachedValue == null || cachedValue[kind] == null) {
                if (cachedValue == null) {
                    cachedValue = new DigitsKeyListener[4];
                    sLocaleInstanceCache.put(locale, cachedValue);
                }
                digitsKeyListener = new DigitsKeyListener(locale, sign, decimal);
                cachedValue[kind] = digitsKeyListener;
                return digitsKeyListener;
            }
            digitsKeyListener = cachedValue[kind];
            return digitsKeyListener;
        }
    }

    public static DigitsKeyListener getInstance(String accepted) {
        DigitsKeyListener result;
        synchronized (sStringCacheLock) {
            result = (DigitsKeyListener) sStringInstanceCache.get(accepted);
            if (result == null) {
                result = new DigitsKeyListener(accepted);
                sStringInstanceCache.put(accepted, result);
            }
        }
        return result;
    }

    public static DigitsKeyListener getInstance(Locale locale, DigitsKeyListener listener) {
        if (listener.mStringMode) {
            return listener;
        }
        return getInstance(locale, listener.mSign, listener.mDecimal);
    }

    public int getInputType() {
        if (this.mNeedsAdvancedInput) {
            return 1;
        }
        int contentType = 2;
        if (this.mSign) {
            contentType = 2 | 4096;
        }
        if (this.mDecimal) {
            return contentType | 8192;
        }
        return contentType;
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        Spanned spanned = dest;
        int i = dstart;
        CharSequence out = super.filter(source, start, end, dest, dstart, dend);
        if (!this.mSign && !this.mDecimal) {
            return out;
        }
        CharSequence source2;
        int start2;
        int end2;
        int i2;
        char c;
        if (out != null) {
            source2 = out;
            start2 = 0;
            end2 = out.length();
        } else {
            source2 = source;
            start2 = start;
            end2 = end;
        }
        int sign = -1;
        int decimal = -1;
        int dlen = dest.length();
        for (i2 = 0; i2 < i; i2++) {
            c = spanned.charAt(i2);
            if (isSignChar(c)) {
                sign = i2;
            } else if (isDecimalPointChar(c)) {
                decimal = i2;
            }
        }
        i2 = decimal;
        for (decimal = dend; decimal < dlen; decimal++) {
            c = spanned.charAt(decimal);
            if (isSignChar(c)) {
                return "";
            }
            if (isDecimalPointChar(c)) {
                i2 = decimal;
            }
        }
        SpannableStringBuilder stripped = null;
        for (int i3 = end2 - 1; i3 >= start2; i3--) {
            char c2 = source2.charAt(i3);
            boolean strip = false;
            if (isSignChar(c2)) {
                if (i3 != start2 || i != 0) {
                    strip = true;
                } else if (sign >= 0) {
                    strip = true;
                } else {
                    sign = i3;
                }
            } else if (isDecimalPointChar(c2)) {
                if (i2 >= 0) {
                    strip = true;
                } else {
                    i2 = i3;
                }
            }
            if (strip) {
                if (end2 == start2 + 1) {
                    return "";
                }
                if (stripped == null) {
                    stripped = new SpannableStringBuilder(source2, start2, end2);
                }
                stripped.delete(i3 - start2, (i3 + 1) - start2);
            }
        }
        if (stripped != null) {
            return stripped;
        }
        if (out != null) {
            return out;
        }
        return null;
    }
}
