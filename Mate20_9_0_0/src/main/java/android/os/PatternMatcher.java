package android.os;

import android.os.Parcelable.Creator;
import android.util.proto.ProtoOutputStream;
import java.util.Arrays;

public class PatternMatcher implements Parcelable {
    public static final Creator<PatternMatcher> CREATOR = new Creator<PatternMatcher>() {
        public PatternMatcher createFromParcel(Parcel source) {
            return new PatternMatcher(source);
        }

        public PatternMatcher[] newArray(int size) {
            return new PatternMatcher[size];
        }
    };
    private static final int MAX_PATTERN_STORAGE = 2048;
    private static final int NO_MATCH = -1;
    private static final int PARSED_MODIFIER_ONE_OR_MORE = -8;
    private static final int PARSED_MODIFIER_RANGE_START = -5;
    private static final int PARSED_MODIFIER_RANGE_STOP = -6;
    private static final int PARSED_MODIFIER_ZERO_OR_MORE = -7;
    private static final int PARSED_TOKEN_CHAR_ANY = -4;
    private static final int PARSED_TOKEN_CHAR_SET_INVERSE_START = -2;
    private static final int PARSED_TOKEN_CHAR_SET_START = -1;
    private static final int PARSED_TOKEN_CHAR_SET_STOP = -3;
    public static final int PATTERN_ADVANCED_GLOB = 3;
    public static final int PATTERN_LITERAL = 0;
    public static final int PATTERN_PREFIX = 1;
    public static final int PATTERN_SIMPLE_GLOB = 2;
    private static final String TAG = "PatternMatcher";
    private static final int TOKEN_TYPE_ANY = 1;
    private static final int TOKEN_TYPE_INVERSE_SET = 3;
    private static final int TOKEN_TYPE_LITERAL = 0;
    private static final int TOKEN_TYPE_SET = 2;
    private static final int[] sParsedPatternScratch = new int[2048];
    private final int[] mParsedPattern;
    private final String mPattern;
    private final int mType;

    public PatternMatcher(String pattern, int type) {
        this.mPattern = pattern;
        this.mType = type;
        if (this.mType == 3) {
            this.mParsedPattern = parseAndVerifyAdvancedPattern(pattern);
        } else {
            this.mParsedPattern = null;
        }
    }

    public final String getPath() {
        return this.mPattern;
    }

    public final int getType() {
        return this.mType;
    }

    public boolean match(String str) {
        return matchPattern(str, this.mPattern, this.mParsedPattern, this.mType);
    }

    public String toString() {
        String type = "? ";
        switch (this.mType) {
            case 0:
                type = "LITERAL: ";
                break;
            case 1:
                type = "PREFIX: ";
                break;
            case 2:
                type = "GLOB: ";
                break;
            case 3:
                type = "ADVANCED: ";
                break;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PatternMatcher{");
        stringBuilder.append(type);
        stringBuilder.append(this.mPattern);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, this.mPattern);
        proto.write(1159641169922L, this.mType);
        proto.end(token);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPattern);
        dest.writeInt(this.mType);
        dest.writeIntArray(this.mParsedPattern);
    }

    public PatternMatcher(Parcel src) {
        this.mPattern = src.readString();
        this.mType = src.readInt();
        this.mParsedPattern = src.createIntArray();
    }

    static boolean matchPattern(String match, String pattern, int[] parsedPattern, int type) {
        if (match == null) {
            return false;
        }
        if (type == 0) {
            return pattern.equals(match);
        }
        if (type == 1) {
            return match.startsWith(pattern);
        }
        if (type == 2) {
            return matchGlobPattern(pattern, match);
        }
        if (type == 3) {
            return matchAdvancedPattern(parsedPattern, match);
        }
        return false;
    }

    static boolean matchGlobPattern(String pattern, String match) {
        int NP = pattern.length();
        boolean z = false;
        if (NP <= 0) {
            if (match.length() <= 0) {
                z = true;
            }
            return z;
        }
        int NM = match.length();
        int ip = 0;
        int im = 0;
        char nextChar = pattern.charAt(0);
        while (ip < NP && im < NM) {
            char c = nextChar;
            ip++;
            nextChar = ip < NP ? pattern.charAt(ip) : 0;
            boolean escaped = c == '\\';
            if (escaped) {
                c = nextChar;
                ip++;
                nextChar = ip < NP ? pattern.charAt(ip) : 0;
            }
            if (nextChar == '*') {
                if (escaped || c != '.') {
                    while (match.charAt(im) == c) {
                        im++;
                        if (im >= NM) {
                            break;
                        }
                    }
                    ip++;
                    nextChar = ip < NP ? pattern.charAt(ip) : 0;
                } else if (ip >= NP - 1) {
                    return true;
                } else {
                    ip++;
                    nextChar = pattern.charAt(ip);
                    if (nextChar == '\\') {
                        ip++;
                        nextChar = ip < NP ? pattern.charAt(ip) : 0;
                    }
                    while (match.charAt(im) != nextChar) {
                        im++;
                        if (im >= NM) {
                            break;
                        }
                    }
                    if (im == NM) {
                        return false;
                    }
                    ip++;
                    nextChar = ip < NP ? pattern.charAt(ip) : 0;
                    im++;
                }
            } else if (c != '.' && match.charAt(im) != c) {
                return false;
            } else {
                im++;
            }
        }
        if (ip < NP || im < NM) {
            return ip == NP + -2 && pattern.charAt(ip) == '.' && pattern.charAt(ip + 1) == '*';
        } else {
            return true;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:76:0x014e  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x014e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static synchronized int[] parseAndVerifyAdvancedPattern(String pattern) {
        NumberFormatException e;
        int[] copyOf;
        String str = pattern;
        synchronized (PatternMatcher.class) {
            boolean LP = pattern.length();
            boolean inRange = false;
            boolean inSet = false;
            int it = 0;
            boolean ip = false;
            boolean inCharClass = false;
            while (ip < LP) {
                if (it <= 2045) {
                    int it2;
                    int it3;
                    char c = str.charAt(ip);
                    boolean addToParsedPattern = false;
                    if (c != '.') {
                        int it4;
                        int parsedToken;
                        boolean ip2;
                        if (c != '{') {
                            if (c != '}') {
                                switch (c) {
                                    case '*':
                                        if (!inSet) {
                                            if (it != 0 && !isParsedModifier(sParsedPatternScratch[it - 1])) {
                                                it2 = it + 1;
                                                sParsedPatternScratch[it] = -7;
                                                break;
                                            }
                                            throw new IllegalArgumentException("Modifier must follow a token.");
                                        }
                                        break;
                                    case '+':
                                        if (!inSet) {
                                            if (it != 0 && !isParsedModifier(sParsedPatternScratch[it - 1])) {
                                                it2 = it + 1;
                                                sParsedPatternScratch[it] = -8;
                                                break;
                                            }
                                            throw new IllegalArgumentException("Modifier must follow a token.");
                                        }
                                        break;
                                    default:
                                        switch (c) {
                                            case '[':
                                                if (inSet) {
                                                    addToParsedPattern = true;
                                                } else {
                                                    int ip3;
                                                    if (str.charAt(ip + 1) == '^') {
                                                        it2 = it + 1;
                                                        sParsedPatternScratch[it] = -2;
                                                        ip3 = ip + 1;
                                                        it = it2;
                                                    } else {
                                                        it4 = it + 1;
                                                        sParsedPatternScratch[it] = -1;
                                                        it = it4;
                                                    }
                                                    ip = ip3 + 1;
                                                    inSet = true;
                                                    continue;
                                                    continue;
                                                }
                                            case '\\':
                                                if (ip + 1 < LP) {
                                                    ip++;
                                                    c = str.charAt(ip);
                                                    addToParsedPattern = true;
                                                } else {
                                                    throw new IllegalArgumentException("Escape found at end of pattern!");
                                                }
                                            case ']':
                                                if (inSet) {
                                                    parsedToken = sParsedPatternScratch[it - 1];
                                                    if (parsedToken != -1 && parsedToken != -2) {
                                                        it2 = it + 1;
                                                        sParsedPatternScratch[it] = -3;
                                                        inCharClass = false;
                                                        inSet = false;
                                                        break;
                                                    }
                                                    throw new IllegalArgumentException("You must define characters in a set.");
                                                }
                                                addToParsedPattern = true;
                                                break;
                                            default:
                                                addToParsedPattern = true;
                                        }
                                        break;
                                }
                            } else if (inRange) {
                                it2 = it + 1;
                                sParsedPatternScratch[it] = -6;
                                ip2 = false;
                            }
                        } else if (!inSet) {
                            if (it == 0 || isParsedModifier(sParsedPatternScratch[it - 1])) {
                                throw new IllegalArgumentException("Modifier must follow a token.");
                            }
                            it2 = it + 1;
                            sParsedPatternScratch[it] = -5;
                            ip++;
                            ip2 = true;
                        }
                        inRange = ip2;
                        it = ip;
                        ip = inCharClass;
                        if (inSet) {
                            if (ip) {
                                it3 = it2 + 1;
                                sParsedPatternScratch[it2] = c;
                                inCharClass = false;
                            } else if (it + 2 >= LP || str.charAt(it + 1) != '-' || str.charAt(it + 2) == ']') {
                                it3 = it2 + 1;
                                sParsedPatternScratch[it2] = c;
                                it4 = it3 + 1;
                                sParsedPatternScratch[it3] = c;
                                inCharClass = ip;
                                it3 = it4;
                            } else {
                                inCharClass = true;
                                it3 = it2 + 1;
                                sParsedPatternScratch[it2] = c;
                                it++;
                            }
                        } else if (inRange) {
                            boolean endOfSet = str.indexOf(125, it);
                            if (endOfSet < false) {
                                int rangeMin;
                                int rangeMax;
                                String rangeString = str.substring(it, endOfSet);
                                parsedToken = rangeString.indexOf(44);
                                if (parsedToken < 0) {
                                    try {
                                        rangeMin = Integer.parseInt(rangeString);
                                        rangeMax = rangeMin;
                                    } catch (NumberFormatException e2) {
                                        e = e2;
                                        throw new IllegalArgumentException("Range number format incorrect", e);
                                    }
                                }
                                rangeMin = Integer.parseInt(rangeString.substring(0, parsedToken));
                                if (parsedToken == rangeString.length() - 1) {
                                    rangeMax = Integer.MAX_VALUE;
                                } else {
                                    rangeMax = Integer.parseInt(rangeString.substring(parsedToken + 1));
                                }
                                if (rangeMin <= rangeMax) {
                                    int it5 = it2 + 1;
                                    try {
                                        sParsedPatternScratch[it2] = rangeMin;
                                        int it6 = it5 + 1;
                                        try {
                                            sParsedPatternScratch[it5] = rangeMax;
                                            it = it6;
                                            boolean z = ip;
                                            ip = endOfSet;
                                            inCharClass = z;
                                        } catch (NumberFormatException e3) {
                                            e = e3;
                                            it2 = it6;
                                            throw new IllegalArgumentException("Range number format incorrect", e);
                                        }
                                    } catch (NumberFormatException e4) {
                                        e = e4;
                                        it2 = it5;
                                        throw new IllegalArgumentException("Range number format incorrect", e);
                                    }
                                }
                                throw new IllegalArgumentException("Range quantifier minimum is greater than maximum");
                            }
                            throw new IllegalArgumentException("Range not ended with '}'");
                        } else if (addToParsedPattern) {
                            it3 = it2 + 1;
                            sParsedPatternScratch[it2] = c;
                            inCharClass = ip;
                        } else {
                            inCharClass = ip;
                            it3 = it2;
                        }
                        ip = it + 1;
                        it = it3;
                    } else if (!inSet) {
                        it2 = it + 1;
                        sParsedPatternScratch[it] = -4;
                        it = ip;
                        ip = inCharClass;
                        if (inSet) {
                        }
                        ip = it + 1;
                        it = it3;
                    }
                    it2 = it;
                    it = ip;
                    ip = inCharClass;
                    if (inSet) {
                    }
                    ip = it + 1;
                    it = it3;
                } else {
                    throw new IllegalArgumentException("Pattern is too large!");
                }
            }
            if (inSet) {
                throw new IllegalArgumentException("Set was not terminated!");
            }
            copyOf = Arrays.copyOf(sParsedPatternScratch, it);
        }
        return copyOf;
    }

    private static boolean isParsedModifier(int parsedChar) {
        return parsedChar == -8 || parsedChar == -7 || parsedChar == -6 || parsedChar == -5;
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x0072  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0071 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static boolean matchAdvancedPattern(int[] parsedPattern, String match) {
        int[] iArr = parsedPattern;
        int LP = iArr.length;
        int LM = match.length();
        int charSetStart = 0;
        boolean z = false;
        int im = 0;
        int ip = 0;
        int charSetEnd = 0;
        while (true) {
            int minRepetition = 1;
            if (ip < LP) {
                int tokenType;
                int ip2;
                int maxRepetition;
                int minRepetition2;
                int patternChar = iArr[ip];
                if (patternChar != -4) {
                    switch (patternChar) {
                        case -2:
                        case -1:
                            tokenType = patternChar == -1 ? 2 : 3;
                            charSetStart = ip + 1;
                            while (true) {
                                ip++;
                                if (ip >= LP || iArr[ip] == -3) {
                                    charSetEnd = ip - 1;
                                    ip++;
                                    break;
                                }
                            }
                            charSetEnd = ip - 1;
                            ip++;
                            break;
                        default:
                            charSetStart = ip;
                            tokenType = 0;
                            ip++;
                            break;
                    }
                }
                tokenType = 1;
                ip++;
                int charSetEnd2 = charSetEnd;
                int charSetStart2 = charSetStart;
                int tokenType2 = tokenType;
                if (ip >= LP) {
                    charSetEnd = 1;
                } else {
                    patternChar = iArr[ip];
                    if (patternChar != -5) {
                        switch (patternChar) {
                            case -8:
                                minRepetition = 1;
                                charSetEnd = Integer.MAX_VALUE;
                                ip++;
                                break;
                            case -7:
                                minRepetition = 0;
                                charSetEnd = Integer.MAX_VALUE;
                                ip++;
                                break;
                            default:
                                charSetEnd = 1;
                                break;
                        }
                    }
                    ip++;
                    charSetEnd = iArr[ip];
                    ip++;
                    minRepetition = charSetEnd;
                    ip2 = ip + 2;
                    maxRepetition = iArr[ip];
                    minRepetition2 = minRepetition;
                    if (minRepetition2 <= maxRepetition) {
                        return false;
                    }
                    charSetEnd = matchChars(match, im, LM, tokenType2, minRepetition2, maxRepetition, iArr, charSetStart2, charSetEnd2);
                    if (charSetEnd == -1) {
                        return false;
                    }
                    im += charSetEnd;
                    charSetStart = charSetStart2;
                    charSetEnd = charSetEnd2;
                    ip = ip2;
                }
                maxRepetition = charSetEnd;
                ip2 = ip;
                minRepetition2 = minRepetition;
                if (minRepetition2 <= maxRepetition) {
                }
            } else {
                if (ip >= LP && im >= LM) {
                    z = true;
                }
                return z;
            }
        }
    }

    private static int matchChars(String match, int im, int lm, int tokenType, int minRepetition, int maxRepetition, int[] parsedPattern, int tokenStart, int tokenEnd) {
        int matched = 0;
        while (matched < maxRepetition) {
            if (!matchChar(match, im + matched, lm, tokenType, parsedPattern, tokenStart, tokenEnd)) {
                break;
            }
            matched++;
        }
        return matched < minRepetition ? -1 : matched;
    }

    private static boolean matchChar(String match, int im, int lm, int tokenType, int[] parsedPattern, int tokenStart, int tokenEnd) {
        boolean z = false;
        if (im >= lm) {
            return false;
        }
        int i;
        char matchChar;
        switch (tokenType) {
            case 0:
                if (match.charAt(im) == parsedPattern[tokenStart]) {
                    z = true;
                }
                return z;
            case 1:
                return true;
            case 2:
                i = tokenStart;
                while (i < tokenEnd) {
                    matchChar = match.charAt(im);
                    if (matchChar >= parsedPattern[i] && matchChar <= parsedPattern[i + 1]) {
                        return true;
                    }
                    i += 2;
                }
                return false;
            case 3:
                i = tokenStart;
                while (i < tokenEnd) {
                    matchChar = match.charAt(im);
                    if (matchChar >= parsedPattern[i] && matchChar <= parsedPattern[i + 1]) {
                        return false;
                    }
                    i += 2;
                }
                return true;
            default:
                return false;
        }
    }
}
