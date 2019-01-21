package android.icu.impl.coll;

import android.icu.impl.IllegalIcuArgumentException;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.Normalizer2;
import android.icu.text.PluralRules;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Builder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

public final class CollationRuleParser {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String BEFORE = "[before";
    private static final int OFFSET_SHIFT = 8;
    static final Position[] POSITION_VALUES = Position.values();
    static final char POS_BASE = '⠀';
    static final char POS_LEAD = '￾';
    private static final int STARRED_FLAG = 16;
    private static final int STRENGTH_MASK = 15;
    private static final int UCOL_DEFAULT = -1;
    private static final int UCOL_OFF = 0;
    private static final int UCOL_ON = 1;
    private static final int U_PARSE_CONTEXT_LEN = 16;
    private static final String[] gSpecialReorderCodes = new String[]{"space", "punct", "symbol", "currency", "digit"};
    private static final String[] positions = new String[]{"first tertiary ignorable", "last tertiary ignorable", "first secondary ignorable", "last secondary ignorable", "first primary ignorable", "last primary ignorable", "first variable", "last variable", "first regular", "last regular", "first implicit", "last implicit", "first trailing", "last trailing"};
    private final CollationData baseData;
    private Importer importer;
    private Normalizer2 nfc = Normalizer2.getNFCInstance();
    private Normalizer2 nfd = Normalizer2.getNFDInstance();
    private final StringBuilder rawBuilder = new StringBuilder();
    private int ruleIndex;
    private String rules;
    private CollationSettings settings;
    private Sink sink;

    interface Importer {
        String getRules(String str, String str2);
    }

    enum Position {
        FIRST_TERTIARY_IGNORABLE,
        LAST_TERTIARY_IGNORABLE,
        FIRST_SECONDARY_IGNORABLE,
        LAST_SECONDARY_IGNORABLE,
        FIRST_PRIMARY_IGNORABLE,
        LAST_PRIMARY_IGNORABLE,
        FIRST_VARIABLE,
        LAST_VARIABLE,
        FIRST_REGULAR,
        LAST_REGULAR,
        FIRST_IMPLICIT,
        LAST_IMPLICIT,
        FIRST_TRAILING,
        LAST_TRAILING
    }

    static abstract class Sink {
        abstract void addRelation(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3);

        abstract void addReset(int i, CharSequence charSequence);

        Sink() {
        }

        void suppressContractions(UnicodeSet set) {
        }

        void optimize(UnicodeSet set) {
        }
    }

    CollationRuleParser(CollationData base) {
        this.baseData = base;
    }

    void setSink(Sink sinkAlias) {
        this.sink = sinkAlias;
    }

    void setImporter(Importer importerAlias) {
        this.importer = importerAlias;
    }

    void parse(String ruleString, CollationSettings outSettings) throws ParseException {
        this.settings = outSettings;
        parse(ruleString);
    }

    private void parse(String ruleString) throws ParseException {
        this.rules = ruleString;
        this.ruleIndex = 0;
        while (this.ruleIndex < this.rules.length()) {
            char c = this.rules.charAt(this.ruleIndex);
            if (PatternProps.isWhiteSpace(c)) {
                this.ruleIndex++;
            } else if (c == '!') {
                this.ruleIndex++;
            } else if (c == '#') {
                this.ruleIndex = skipComment(this.ruleIndex + 1);
            } else if (c == '&') {
                parseRuleChain();
            } else if (c == '@') {
                this.settings.setFlag(2048, true);
                this.ruleIndex++;
            } else if (c != '[') {
                setParseError("expected a reset or setting or comment");
            } else {
                parseSetting();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x002e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseRuleChain() throws ParseException {
        int resetStrength = parseResetAndPosition();
        boolean isFirstRelation = true;
        while (true) {
            int result = parseRelationOperator();
            if (result >= 0) {
                int strength = result & 15;
                if (resetStrength < 15) {
                    if (isFirstRelation) {
                        if (strength != resetStrength) {
                            setParseError("reset-before strength differs from its first relation");
                            return;
                        }
                    } else if (strength < resetStrength) {
                        setParseError("reset-before strength followed by a stronger relation");
                        return;
                    }
                }
                int i = this.ruleIndex + (result >> 8);
                if ((result & 16) == 0) {
                    parseRelationStrings(strength, i);
                } else {
                    parseStarredCharacters(strength, i);
                }
                isFirstRelation = false;
            } else if (this.ruleIndex < this.rules.length() && this.rules.charAt(this.ruleIndex) == '#') {
                this.ruleIndex = skipComment(this.ruleIndex + 1);
            } else if (isFirstRelation) {
                setParseError("reset not followed by a relation");
            }
        }
        if (isFirstRelation) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x007d  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0077  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int parseResetAndPosition() throws ParseException {
        int length;
        int resetStrength;
        int i = skipWhiteSpace(this.ruleIndex + 1);
        if (this.rules.regionMatches(i, BEFORE, 0, BEFORE.length())) {
            length = BEFORE.length() + i;
            int j = length;
            if (length < this.rules.length() && PatternProps.isWhiteSpace(this.rules.charAt(j))) {
                length = skipWhiteSpace(j + 1);
                j = length;
                if (length + 1 < this.rules.length()) {
                    char charAt = this.rules.charAt(j);
                    char c = charAt;
                    if ('1' <= charAt && c <= '3' && this.rules.charAt(j + 1) == ']') {
                        resetStrength = 0 + (c - 49);
                        i = skipWhiteSpace(j + 2);
                        length = resetStrength;
                        if (i < this.rules.length()) {
                            setParseError("reset without position");
                            return -1;
                        }
                        if (this.rules.charAt(i) == '[') {
                            i = parseSpecialPosition(i, this.rawBuilder);
                        } else {
                            i = parseTailoringString(i, this.rawBuilder);
                        }
                        try {
                            this.sink.addReset(length, this.rawBuilder);
                            this.ruleIndex = i;
                            return length;
                        } catch (Exception e) {
                            setParseError("adding reset failed", e);
                            return -1;
                        }
                    }
                }
            }
        }
        resetStrength = 15;
        length = resetStrength;
        if (i < this.rules.length()) {
        }
    }

    private int parseRelationOperator() {
        this.ruleIndex = skipWhiteSpace(this.ruleIndex);
        if (this.ruleIndex >= this.rules.length()) {
            return -1;
        }
        int strength;
        char c = this.ruleIndex;
        int i = c + 1;
        c = this.rules.charAt(c);
        if (c != ',') {
            switch (c) {
                case ';':
                    strength = 1;
                    break;
                case '<':
                    if (i >= this.rules.length() || this.rules.charAt(i) != '<') {
                        strength = 0;
                    } else {
                        i++;
                        if (i >= this.rules.length() || this.rules.charAt(i) != '<') {
                            strength = 1;
                        } else {
                            i++;
                            if (i >= this.rules.length() || this.rules.charAt(i) != '<') {
                                strength = 2;
                            } else {
                                i++;
                                strength = 3;
                            }
                        }
                    }
                    if (i < this.rules.length() && this.rules.charAt(i) == '*') {
                        i++;
                        strength |= 16;
                        break;
                    }
                    break;
                case '=':
                    strength = 15;
                    if (i < this.rules.length() && this.rules.charAt(i) == '*') {
                        i++;
                        strength = 15 | 16;
                        break;
                    }
                default:
                    return -1;
            }
        }
        strength = 2;
        return ((i - this.ruleIndex) << 8) | strength;
    }

    private void parseRelationStrings(int strength, int i) throws ParseException {
        String prefix = "";
        CharSequence extension = "";
        i = parseTailoringString(i, this.rawBuilder);
        char next = i < this.rules.length() ? this.rules.charAt(i) : 0;
        if (next == '|') {
            prefix = this.rawBuilder.toString();
            i = parseTailoringString(i + 1, this.rawBuilder);
            next = i < this.rules.length() ? this.rules.charAt(i) : 0;
        }
        if (next == '/') {
            StringBuilder extBuilder = new StringBuilder();
            i = parseTailoringString(i + 1, extBuilder);
            extension = extBuilder;
        }
        if (prefix.length() != 0) {
            int prefix0 = prefix.codePointAt(0);
            int c = this.rawBuilder.codePointAt(0);
            if (!(this.nfc.hasBoundaryBefore(prefix0) && this.nfc.hasBoundaryBefore(c))) {
                setParseError("in 'prefix|str', prefix and str must each start with an NFC boundary");
                return;
            }
        }
        try {
            this.sink.addRelation(strength, prefix, this.rawBuilder, extension);
            this.ruleIndex = i;
        } catch (Exception e) {
            setParseError("adding relation failed", e);
        }
    }

    private void parseStarredCharacters(int strength, int i) throws ParseException {
        String empty = "";
        i = parseString(skipWhiteSpace(i), this.rawBuilder);
        if (this.rawBuilder.length() == 0) {
            setParseError("missing starred-relation string");
            return;
        }
        int prev = -1;
        int i2 = i;
        i = 0;
        while (true) {
            int c;
            if (i >= this.rawBuilder.length()) {
                if (i2 < this.rules.length() && this.rules.charAt(i2) == '-') {
                    if (prev >= 0) {
                        i2 = parseString(i2 + 1, this.rawBuilder);
                        if (this.rawBuilder.length() != 0) {
                            c = this.rawBuilder.codePointAt(0);
                            if (c >= prev) {
                                while (true) {
                                    prev++;
                                    if (prev > c) {
                                        prev = -1;
                                        i = Character.charCount(c);
                                        break;
                                    } else if (!this.nfd.isInert(prev)) {
                                        setParseError("starred-relation string range is not all NFD-inert");
                                        return;
                                    } else if (isSurrogate(prev)) {
                                        setParseError("starred-relation string range contains a surrogate");
                                        return;
                                    } else if (UCharacter.REPLACEMENT_CHAR > prev || prev > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                                        try {
                                            this.sink.addRelation(strength, empty, UTF16.valueOf(prev), empty);
                                        } catch (Exception e) {
                                            setParseError("adding relation failed", e);
                                            return;
                                        }
                                    } else {
                                        setParseError("starred-relation string range contains U+FFFD, U+FFFE or U+FFFF");
                                        return;
                                    }
                                }
                            }
                            setParseError("range start greater than end in starred-relation string");
                            return;
                        }
                        setParseError("range without end in starred-relation string");
                        return;
                    }
                    setParseError("range without start in starred-relation string");
                    return;
                }
                this.ruleIndex = skipWhiteSpace(i2);
            } else {
                c = this.rawBuilder.codePointAt(i);
                if (this.nfd.isInert(c)) {
                    try {
                        this.sink.addRelation(strength, empty, UTF16.valueOf(c), empty);
                        i += Character.charCount(c);
                        prev = c;
                    } catch (Exception e2) {
                        setParseError("adding relation failed", e2);
                        return;
                    }
                }
                setParseError("starred-relation string is not all NFD-inert");
                return;
            }
        }
        this.ruleIndex = skipWhiteSpace(i2);
    }

    private int parseTailoringString(int i, StringBuilder raw) throws ParseException {
        i = parseString(skipWhiteSpace(i), raw);
        if (raw.length() == 0) {
            setParseError("missing relation string");
        }
        return skipWhiteSpace(i);
    }

    private int parseString(int i, StringBuilder raw) throws ParseException {
        int i2;
        int j = 0;
        raw.setLength(0);
        while (i < this.rules.length()) {
            int i3 = i + 1;
            char c = this.rules.charAt(i);
            if (isSyntaxChar(c)) {
                if (c != PatternTokenizer.SINGLE_QUOTE) {
                    if (c != PatternTokenizer.BACK_SLASH) {
                        i2 = i3 - 1;
                        break;
                    } else if (i3 == this.rules.length()) {
                        setParseError("backslash escape at the end of the rule string");
                        return i3;
                    } else {
                        i2 = this.rules.codePointAt(i3);
                        raw.appendCodePoint(i2);
                        i3 += Character.charCount(i2);
                    }
                } else if (i3 >= this.rules.length() || this.rules.charAt(i3) != PatternTokenizer.SINGLE_QUOTE) {
                    while (i3 != this.rules.length()) {
                        int i4 = i3 + 1;
                        c = this.rules.charAt(i3);
                        if (c == PatternTokenizer.SINGLE_QUOTE) {
                            if (i4 >= this.rules.length() || this.rules.charAt(i4) != PatternTokenizer.SINGLE_QUOTE) {
                                i = i4;
                            } else {
                                i4++;
                            }
                        }
                        i3 = i4;
                        raw.append(c);
                    }
                    setParseError("quoted literal text missing terminating apostrophe");
                    return i3;
                } else {
                    raw.append(PatternTokenizer.SINGLE_QUOTE);
                    i = i3 + 1;
                }
            } else if (PatternProps.isWhiteSpace(c)) {
                i2 = i3 - 1;
                break;
            } else {
                raw.append(c);
            }
            i = i3;
        }
        i2 = i;
        while (true) {
            i = j;
            if (i >= raw.length()) {
                return i2;
            }
            j = raw.codePointAt(i);
            if (isSurrogate(j)) {
                setParseError("string contains an unpaired surrogate");
                return i2;
            } else if (UCharacter.REPLACEMENT_CHAR > j || j > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                j = i + Character.charCount(j);
            } else {
                setParseError("string contains U+FFFD, U+FFFE or U+FFFF");
                return i2;
            }
        }
    }

    private static final boolean isSurrogate(int c) {
        return (c & -2048) == 55296;
    }

    private int parseSpecialPosition(int i, StringBuilder str) throws ParseException {
        int j = readWords(i + 1, this.rawBuilder);
        if (j > i && this.rules.charAt(j) == ']' && this.rawBuilder.length() != 0) {
            j++;
            String raw = this.rawBuilder.toString();
            int pos = 0;
            str.setLength(0);
            while (pos < positions.length) {
                if (raw.equals(positions[pos])) {
                    str.append(POS_LEAD);
                    str.append((char) (10240 + pos));
                    return j;
                }
                pos++;
            }
            if (raw.equals("top")) {
                str.append(POS_LEAD);
                str.append((char) (10240 + Position.LAST_REGULAR.ordinal()));
                return j;
            } else if (raw.equals("variable top")) {
                str.append(POS_LEAD);
                str.append((char) (10240 + Position.LAST_VARIABLE.ordinal()));
                return j;
            }
        }
        setParseError("not a valid special reset position");
        return i;
    }

    private void parseSetting() throws ParseException {
        boolean z = true;
        int i = this.ruleIndex + 1;
        int j = readWords(i, this.rawBuilder);
        if (j <= i || this.rawBuilder.length() == 0) {
            setParseError("expected a setting/option at '['");
        }
        String raw = this.rawBuilder.toString();
        if (this.rules.charAt(j) == ']') {
            j++;
            if (raw.startsWith("reorder") && (raw.length() == 7 || raw.charAt(7) == ' ')) {
                parseReordering(raw);
                this.ruleIndex = j;
                return;
            } else if (raw.equals("backwards 2")) {
                this.settings.setFlag(2048, true);
                this.ruleIndex = j;
                return;
            } else {
                String v;
                int valueIndex = raw.lastIndexOf(32);
                boolean z2 = false;
                if (valueIndex >= 0) {
                    v = raw.substring(valueIndex + 1);
                    raw = raw.substring(0, valueIndex);
                } else {
                    v = "";
                }
                int value;
                int value2;
                CollationSettings collationSettings;
                if (raw.equals("strength") && v.length() == 1) {
                    value = -1;
                    char c = v.charAt(0);
                    if ('1' <= c && c <= '4') {
                        value = 0 + (c - 49);
                    } else if (c == 'I') {
                        value = 15;
                    }
                    if (value != -1) {
                        this.settings.setStrength(value);
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("alternate")) {
                    value2 = -1;
                    if (v.equals("non-ignorable")) {
                        value2 = 0;
                    } else if (v.equals("shifted")) {
                        value2 = 1;
                    }
                    if (value2 != -1) {
                        collationSettings = this.settings;
                        if (value2 <= 0) {
                            z = false;
                        }
                        collationSettings.setAlternateHandlingShifted(z);
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("maxVariable")) {
                    value = -1;
                    if (v.equals("space")) {
                        value = 0;
                    } else if (v.equals("punct")) {
                        value = 1;
                    } else if (v.equals("symbol")) {
                        value = 2;
                    } else if (v.equals("currency")) {
                        value = 3;
                    }
                    if (value != -1) {
                        this.settings.setMaxVariable(value, 0);
                        this.settings.variableTop = this.baseData.getLastPrimaryForGroup(4096 + value);
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("caseFirst")) {
                    value = -1;
                    if (v.equals("off")) {
                        value = 0;
                    } else if (v.equals("lower")) {
                        value = 512;
                    } else if (v.equals("upper")) {
                        value = CollationSettings.CASE_FIRST_AND_UPPER_MASK;
                    }
                    if (value != -1) {
                        this.settings.setCaseFirst(value);
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("caseLevel")) {
                    value2 = getOnOffValue(v);
                    if (value2 != -1) {
                        collationSettings = this.settings;
                        if (value2 <= 0) {
                            z = false;
                        }
                        collationSettings.setFlag(1024, z);
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("normalization")) {
                    value2 = getOnOffValue(v);
                    if (value2 != -1) {
                        collationSettings = this.settings;
                        if (value2 > 0) {
                            z2 = true;
                        }
                        collationSettings.setFlag(1, z2);
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("numericOrdering")) {
                    value2 = getOnOffValue(v);
                    if (value2 != -1) {
                        collationSettings = this.settings;
                        if (value2 <= 0) {
                            z = false;
                        }
                        collationSettings.setFlag(2, z);
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("hiraganaQ")) {
                    int value3 = getOnOffValue(v);
                    if (value3 != -1) {
                        if (value3 == 1) {
                            setParseError("[hiraganaQ on] is not supported");
                        }
                        this.ruleIndex = j;
                        return;
                    }
                } else if (raw.equals("import")) {
                    try {
                        ULocale localeID = new Builder().setLanguageTag(v).build();
                        String baseID = localeID.getBaseName();
                        String collationType = localeID.getKeywordValue("collation");
                        if (this.importer == null) {
                            setParseError("[import langTag] is not supported");
                        } else {
                            try {
                                String str;
                                String importedRules = this.importer;
                                if (collationType != null) {
                                    str = collationType;
                                } else {
                                    str = "standard";
                                }
                                importedRules = importedRules.getRules(baseID, str);
                                str = this.rules;
                                int outerRuleIndex = this.ruleIndex;
                                try {
                                    parse(importedRules);
                                } catch (Exception e) {
                                    this.ruleIndex = outerRuleIndex;
                                    setParseError("parsing imported rules failed", e);
                                }
                                this.rules = str;
                                this.ruleIndex = j;
                            } catch (Exception e2) {
                                setParseError("[import langTag] failed", e2);
                                return;
                            }
                        }
                        return;
                    } catch (Exception e3) {
                        setParseError("expected language tag in [import langTag]", e3);
                        return;
                    }
                }
            }
        } else if (this.rules.charAt(j) == '[') {
            UnicodeSet set = new UnicodeSet();
            j = parseUnicodeSet(j, set);
            if (raw.equals("optimize")) {
                try {
                    this.sink.optimize(set);
                } catch (Exception e4) {
                    setParseError("[optimize set] failed", e4);
                }
                this.ruleIndex = j;
                return;
            } else if (raw.equals("suppressContractions")) {
                try {
                    this.sink.suppressContractions(set);
                } catch (Exception e42) {
                    setParseError("[suppressContractions set] failed", e42);
                }
                this.ruleIndex = j;
                return;
            }
        }
        setParseError("not a valid setting/option");
    }

    private void parseReordering(CharSequence raw) throws ParseException {
        int i = 7;
        if (7 == raw.length()) {
            this.settings.resetReordering();
            return;
        }
        ArrayList<Integer> reorderCodes = new ArrayList();
        while (i < raw.length()) {
            i++;
            int limit = i;
            while (limit < raw.length() && raw.charAt(limit) != ' ') {
                limit++;
            }
            int code = getReorderCode(raw.subSequence(i, limit).toString());
            if (code < 0) {
                setParseError("unknown script or reorder code");
                return;
            } else {
                reorderCodes.add(Integer.valueOf(code));
                i = limit;
            }
        }
        if (reorderCodes.isEmpty()) {
            this.settings.resetReordering();
        } else {
            int[] codes = new int[reorderCodes.size()];
            int j = 0;
            Iterator it = reorderCodes.iterator();
            while (it.hasNext()) {
                int j2 = j + 1;
                codes[j] = ((Integer) it.next()).intValue();
                j = j2;
            }
            this.settings.setReordering(this.baseData, codes);
        }
    }

    public static int getReorderCode(String word) {
        int i;
        for (i = 0; i < gSpecialReorderCodes.length; i++) {
            if (word.equalsIgnoreCase(gSpecialReorderCodes[i])) {
                return 4096 + i;
            }
        }
        try {
            i = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, word);
            if (i >= 0) {
                return i;
            }
        } catch (IllegalIcuArgumentException e) {
        }
        if (word.equalsIgnoreCase("others")) {
            return 103;
        }
        return -1;
    }

    private static int getOnOffValue(String s) {
        if (s.equals("on")) {
            return 1;
        }
        if (s.equals("off")) {
            return 0;
        }
        return -1;
    }

    private int parseUnicodeSet(int i, UnicodeSet set) throws ParseException {
        int level = 0;
        char c = i;
        while (c != this.rules.length()) {
            char j = c + 1;
            c = this.rules.charAt(c);
            if (c == '[') {
                level++;
            } else if (c == ']') {
                level--;
                if (level == 0) {
                    try {
                        set.applyPattern(this.rules.substring(i, j));
                    } catch (Exception e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("not a valid UnicodeSet pattern: ");
                        stringBuilder.append(e.getMessage());
                        setParseError(stringBuilder.toString());
                    }
                    int j2 = skipWhiteSpace(j);
                    if (j2 != this.rules.length() && this.rules.charAt(j2) == ']') {
                        return j2 + 1;
                    }
                    setParseError("missing option-terminating ']' after UnicodeSet pattern");
                    return j2;
                }
            } else {
                continue;
            }
            c = j;
        }
        setParseError("unbalanced UnicodeSet pattern brackets");
        return c;
    }

    private int readWords(int i, StringBuilder raw) {
        raw.setLength(0);
        i = skipWhiteSpace(i);
        while (i < this.rules.length()) {
            char c = this.rules.charAt(i);
            if (!isSyntaxChar(c) || c == '-' || c == '_') {
                if (PatternProps.isWhiteSpace(c)) {
                    raw.append(' ');
                    i = skipWhiteSpace(i + 1);
                } else {
                    raw.append(c);
                    i++;
                }
            } else if (raw.length() == 0) {
                return i;
            } else {
                int lastIndex = raw.length() - 1;
                if (raw.charAt(lastIndex) == ' ') {
                    raw.setLength(lastIndex);
                }
                return i;
            }
        }
        return 0;
    }

    private int skipComment(int i) {
        while (i < this.rules.length()) {
            int i2 = i + 1;
            char c = this.rules.charAt(i);
            if (c == 10 || c == 12 || c == 13 || c == 133 || c == 8232 || c == 8233) {
                return i2;
            }
            i = i2;
        }
        return i;
    }

    private void setParseError(String reason) throws ParseException {
        throw makeParseException(reason);
    }

    private void setParseError(String reason, Exception e) throws ParseException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(reason);
        stringBuilder.append(PluralRules.KEYWORD_RULE_SEPARATOR);
        stringBuilder.append(e.getMessage());
        ParseException newExc = makeParseException(stringBuilder.toString());
        newExc.initCause(e);
        throw newExc;
    }

    private ParseException makeParseException(String reason) {
        return new ParseException(appendErrorContext(reason), this.ruleIndex);
    }

    private String appendErrorContext(String reason) {
        StringBuilder msg = new StringBuilder(reason);
        msg.append(" at index ");
        msg.append(this.ruleIndex);
        msg.append(" near \"");
        int start = this.ruleIndex - 15;
        if (start < 0) {
            start = 0;
        } else if (start > 0 && Character.isLowSurrogate(this.rules.charAt(start))) {
            start++;
        }
        msg.append(this.rules, start, this.ruleIndex);
        msg.append('!');
        int length = this.rules.length() - this.ruleIndex;
        if (length >= 16) {
            length = 15;
            if (Character.isHighSurrogate(this.rules.charAt((this.ruleIndex + 15) - 1))) {
                length = 15 - 1;
            }
        }
        msg.append(this.rules, this.ruleIndex, this.ruleIndex + length);
        msg.append('\"');
        return msg.toString();
    }

    private static boolean isSyntaxChar(int c) {
        return 33 <= c && c <= 126 && (c <= 47 || ((58 <= c && c <= 64) || ((91 <= c && c <= 96) || 123 <= c)));
    }

    private int skipWhiteSpace(int i) {
        while (i < this.rules.length() && PatternProps.isWhiteSpace(this.rules.charAt(i))) {
            i++;
        }
        return i;
    }
}
