package java.lang;

import android.icu.text.Transliterator;
import java.util.Locale;
import libcore.icu.ICU;

class CaseMapper {
    private static final ThreadLocal<Transliterator> EL_UPPER = new ThreadLocal<Transliterator>() {
        protected Transliterator initialValue() {
            return Transliterator.getInstance("el-Upper");
        }
    };
    private static final char GREEK_CAPITAL_SIGMA = 'Σ';
    private static final char GREEK_SMALL_FINAL_SIGMA = 'ς';
    private static final char LATIN_CAPITAL_I_WITH_DOT = 'İ';
    private static final char[] upperValues = "SS\u0000ʼN\u0000J̌\u0000Ϊ́Ϋ́ԵՒ\u0000H̱\u0000T̈\u0000W̊\u0000Y̊\u0000Aʾ\u0000Υ̓\u0000Υ̓̀Υ̓́Υ̓͂ἈΙ\u0000ἉΙ\u0000ἊΙ\u0000ἋΙ\u0000ἌΙ\u0000ἍΙ\u0000ἎΙ\u0000ἏΙ\u0000ἈΙ\u0000ἉΙ\u0000ἊΙ\u0000ἋΙ\u0000ἌΙ\u0000ἍΙ\u0000ἎΙ\u0000ἏΙ\u0000ἨΙ\u0000ἩΙ\u0000ἪΙ\u0000ἫΙ\u0000ἬΙ\u0000ἭΙ\u0000ἮΙ\u0000ἯΙ\u0000ἨΙ\u0000ἩΙ\u0000ἪΙ\u0000ἫΙ\u0000ἬΙ\u0000ἭΙ\u0000ἮΙ\u0000ἯΙ\u0000ὨΙ\u0000ὩΙ\u0000ὪΙ\u0000ὫΙ\u0000ὬΙ\u0000ὭΙ\u0000ὮΙ\u0000ὯΙ\u0000ὨΙ\u0000ὩΙ\u0000ὪΙ\u0000ὫΙ\u0000ὬΙ\u0000ὭΙ\u0000ὮΙ\u0000ὯΙ\u0000ᾺΙ\u0000ΑΙ\u0000ΆΙ\u0000Α͂\u0000Α͂ΙΑΙ\u0000ῊΙ\u0000ΗΙ\u0000ΉΙ\u0000Η͂\u0000Η͂ΙΗΙ\u0000Ϊ̀Ϊ́Ι͂\u0000Ϊ͂Ϋ̀Ϋ́Ρ̓\u0000Υ͂\u0000Ϋ͂ῺΙ\u0000ΩΙ\u0000ΏΙ\u0000Ω͂\u0000Ω͂ΙΩΙ\u0000FF\u0000FI\u0000FL\u0000FFIFFLST\u0000ST\u0000ՄՆ\u0000ՄԵ\u0000ՄԻ\u0000ՎՆ\u0000ՄԽ\u0000".toCharArray();
    private static final char[] upperValues2 = "\u000b\u0000\f\u0000\r\u0000\u000e\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\"#$%&'()*+,-./0123456789:;<=>\u0000\u0000?@A\u0000BC\u0000\u0000\u0000\u0000D\u0000\u0000\u0000\u0000\u0000EFG\u0000HI\u0000\u0000\u0000\u0000J\u0000\u0000\u0000\u0000\u0000KL\u0000\u0000MN\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000OPQ\u0000RS\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000TUV\u0000WX\u0000\u0000\u0000\u0000Y".toCharArray();

    private CaseMapper() {
    }

    public static String toLowerCase(Locale locale, String s) {
        String languageCode = locale.getLanguage();
        if (languageCode.equals("tr") || languageCode.equals("az") || languageCode.equals("lt")) {
            return ICU.toLowerCase(s, locale);
        }
        char[] newValue = null;
        int i = 0;
        int end = s.length();
        while (i < end) {
            char ch = s.charAt(i);
            if (ch == 'İ' || Character.isHighSurrogate(ch)) {
                return ICU.toLowerCase(s, locale);
            }
            char c;
            if (ch == GREEK_CAPITAL_SIGMA && isFinalSigma(s, i)) {
                c = GREEK_SMALL_FINAL_SIGMA;
            } else {
                c = Character.toLowerCase(ch);
            }
            if (ch != c) {
                if (newValue == null) {
                    newValue = new char[end];
                    s.getCharsNoCheck(0, end, newValue, 0);
                }
                newValue[i] = c;
            }
            i++;
        }
        if (newValue != null) {
            s = new String(newValue);
        }
        return s;
    }

    private static boolean isFinalSigma(String s, int index) {
        if (index <= 0) {
            return false;
        }
        boolean z;
        char previous = s.charAt(index - 1);
        if (Character.isLowerCase(previous) || Character.isUpperCase(previous)) {
            z = true;
        } else {
            z = Character.isTitleCase(previous);
        }
        if (!z) {
            return false;
        }
        if (index + 1 >= s.length()) {
            return true;
        }
        char next = s.charAt(index + 1);
        return (Character.isLowerCase(next) || Character.isUpperCase(next) || Character.isTitleCase(next)) ? false : true;
    }

    private static int upperIndex(int ch) {
        int index = -1;
        if (ch >= 223) {
            if (ch <= 1415) {
                switch (ch) {
                    case 223:
                        return 0;
                    case 329:
                        return 1;
                    case 496:
                        return 2;
                    case 912:
                        return 3;
                    case 944:
                        return 4;
                    case 1415:
                        return 5;
                }
            } else if (ch >= 7830) {
                if (ch <= 7834) {
                    index = (ch + 6) - 7830;
                } else if (ch >= 8016 && ch <= 8188) {
                    index = upperValues2[ch - 8016];
                    if (index == 0) {
                        index = -1;
                    }
                } else if (ch >= 64256) {
                    if (ch <= 64262) {
                        index = (ch + 90) - 64256;
                    } else if (ch >= 64275 && ch <= 64279) {
                        index = (ch + 97) - 64275;
                    }
                }
            }
        }
        return index;
    }

    public static String toUpperCase(Locale locale, String s, int count) {
        String languageCode = locale.getLanguage();
        if (languageCode.equals("tr") || languageCode.equals("az") || languageCode.equals("lt")) {
            return ICU.toUpperCase(s, locale);
        }
        if (languageCode.equals("el")) {
            return ((Transliterator) EL_UPPER.get()).transliterate(s);
        }
        char[] output = null;
        int o = 0;
        int i = 0;
        while (o < count) {
            char ch = s.charAt(o);
            if (Character.isHighSurrogate(ch)) {
                return ICU.toUpperCase(s, locale);
            }
            int i2;
            int index = upperIndex(ch);
            char[] newoutput;
            if (index == -1) {
                if (output != null && i >= output.length) {
                    newoutput = new char[((output.length + (count / 6)) + 2)];
                    System.arraycopy(output, 0, newoutput, 0, output.length);
                    output = newoutput;
                }
                char upch = Character.toUpperCase(ch);
                if (output != null) {
                    i2 = i + 1;
                    output[i] = upch;
                } else if (ch != upch) {
                    output = new char[count];
                    i2 = o;
                    s.getCharsNoCheck(0, o, output, 0);
                    i = i2 + 1;
                    output[i2] = upch;
                    i2 = i;
                } else {
                    i2 = i;
                }
            } else {
                int target = index * 3;
                char val3 = upperValues[target + 2];
                if (output == null) {
                    output = new char[(((count / 6) + count) + 2)];
                    i2 = o;
                    s.getCharsNoCheck(0, o, output, 0);
                } else {
                    if ((val3 == '\u0000' ? 1 : 2) + i >= output.length) {
                        newoutput = new char[((output.length + (count / 6)) + 3)];
                        System.arraycopy(output, 0, newoutput, 0, output.length);
                        output = newoutput;
                        i2 = i;
                    } else {
                        i2 = i;
                    }
                }
                i = i2 + 1;
                output[i2] = upperValues[target];
                i2 = i + 1;
                output[i] = upperValues[target + 1];
                if (val3 != '\u0000') {
                    i = i2 + 1;
                    output[i2] = val3;
                    i2 = i;
                }
            }
            o++;
            i = i2;
        }
        if (output == null) {
            return s;
        }
        String str = (output.length == i || output.length - i < 8) ? new String(0, i, output) : new String(output, 0, i);
        return str;
    }
}
