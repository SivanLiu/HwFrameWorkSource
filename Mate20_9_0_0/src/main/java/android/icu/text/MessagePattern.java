package android.icu.text;

import android.icu.impl.ICUConfig;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.UCharacterProperty;
import android.icu.util.Freezable;
import android.icu.util.ICUCloneNotSupportedException;
import java.util.ArrayList;
import java.util.Locale;

public final class MessagePattern implements Cloneable, Freezable<MessagePattern> {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final int ARG_NAME_NOT_NUMBER = -1;
    public static final int ARG_NAME_NOT_VALID = -2;
    private static final int MAX_PREFIX_LENGTH = 24;
    public static final double NO_NUMERIC_VALUE = -1.23456789E8d;
    private static final ArgType[] argTypes = ArgType.values();
    private static final ApostropheMode defaultAposMode = ApostropheMode.valueOf(ICUConfig.get("android.icu.text.MessagePattern.ApostropheMode", "DOUBLE_OPTIONAL"));
    private ApostropheMode aposMode;
    private volatile boolean frozen;
    private boolean hasArgNames;
    private boolean hasArgNumbers;
    private String msg;
    private boolean needsAutoQuoting;
    private ArrayList<Double> numericValues;
    private ArrayList<Part> parts;

    public enum ApostropheMode {
        DOUBLE_OPTIONAL,
        DOUBLE_REQUIRED
    }

    public enum ArgType {
        NONE,
        SIMPLE,
        CHOICE,
        PLURAL,
        SELECT,
        SELECTORDINAL;

        public boolean hasPluralStyle() {
            return this == PLURAL || this == SELECTORDINAL;
        }
    }

    public static final class Part {
        private static final int MAX_LENGTH = 65535;
        private static final int MAX_VALUE = 32767;
        private final int index;
        private final char length;
        private int limitPartIndex;
        private final Type type;
        private short value;

        public enum Type {
            MSG_START,
            MSG_LIMIT,
            SKIP_SYNTAX,
            INSERT_CHAR,
            REPLACE_NUMBER,
            ARG_START,
            ARG_LIMIT,
            ARG_NUMBER,
            ARG_NAME,
            ARG_TYPE,
            ARG_STYLE,
            ARG_SELECTOR,
            ARG_INT,
            ARG_DOUBLE;

            public boolean hasNumericValue() {
                return this == ARG_INT || this == ARG_DOUBLE;
            }
        }

        private Part(Type t, int i, int l, int v) {
            this.type = t;
            this.index = i;
            this.length = (char) l;
            this.value = (short) v;
        }

        public Type getType() {
            return this.type;
        }

        public int getIndex() {
            return this.index;
        }

        public int getLength() {
            return this.length;
        }

        public int getLimit() {
            return this.index + this.length;
        }

        public int getValue() {
            return this.value;
        }

        public ArgType getArgType() {
            Type type = getType();
            if (type == Type.ARG_START || type == Type.ARG_LIMIT) {
                return MessagePattern.argTypes[this.value];
            }
            return ArgType.NONE;
        }

        public String toString() {
            String valueString = (this.type == Type.ARG_START || this.type == Type.ARG_LIMIT) ? getArgType().name() : Integer.toString(this.value);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.type.name());
            stringBuilder.append("(");
            stringBuilder.append(valueString);
            stringBuilder.append(")@");
            stringBuilder.append(this.index);
            return stringBuilder.toString();
        }

        public boolean equals(Object other) {
            boolean z = true;
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Part o = (Part) other;
            if (!(this.type.equals(o.type) && this.index == o.index && this.length == o.length && this.value == o.value && this.limitPartIndex == o.limitPartIndex)) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (((((this.type.hashCode() * 37) + this.index) * 37) + this.length) * 37) + this.value;
        }
    }

    public MessagePattern() {
        this.parts = new ArrayList();
        this.aposMode = defaultAposMode;
    }

    public MessagePattern(ApostropheMode mode) {
        this.parts = new ArrayList();
        this.aposMode = mode;
    }

    public MessagePattern(String pattern) {
        this.parts = new ArrayList();
        this.aposMode = defaultAposMode;
        parse(pattern);
    }

    public MessagePattern parse(String pattern) {
        preParse(pattern);
        parseMessage(0, 0, 0, ArgType.NONE);
        postParse();
        return this;
    }

    public MessagePattern parseChoiceStyle(String pattern) {
        preParse(pattern);
        parseChoiceStyle(0, 0);
        postParse();
        return this;
    }

    public MessagePattern parsePluralStyle(String pattern) {
        preParse(pattern);
        parsePluralOrSelectStyle(ArgType.PLURAL, 0, 0);
        postParse();
        return this;
    }

    public MessagePattern parseSelectStyle(String pattern) {
        preParse(pattern);
        parsePluralOrSelectStyle(ArgType.SELECT, 0, 0);
        postParse();
        return this;
    }

    public void clear() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to clear() a frozen MessagePattern instance.");
        }
        this.msg = null;
        this.hasArgNumbers = false;
        this.hasArgNames = false;
        this.needsAutoQuoting = false;
        this.parts.clear();
        if (this.numericValues != null) {
            this.numericValues.clear();
        }
    }

    public void clearPatternAndSetApostropheMode(ApostropheMode mode) {
        clear();
        this.aposMode = mode;
    }

    public boolean equals(Object other) {
        boolean z = true;
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        MessagePattern o = (MessagePattern) other;
        if (!(this.aposMode.equals(o.aposMode) && (!this.msg != null ? o.msg != null : !this.msg.equals(o.msg)) && this.parts.equals(o.parts))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (((this.aposMode.hashCode() * 37) + (this.msg != null ? this.msg.hashCode() : 0)) * 37) + this.parts.hashCode();
    }

    public ApostropheMode getApostropheMode() {
        return this.aposMode;
    }

    boolean jdkAposMode() {
        return this.aposMode == ApostropheMode.DOUBLE_REQUIRED;
    }

    public String getPatternString() {
        return this.msg;
    }

    public boolean hasNamedArguments() {
        return this.hasArgNames;
    }

    public boolean hasNumberedArguments() {
        return this.hasArgNumbers;
    }

    public String toString() {
        return this.msg;
    }

    public static int validateArgumentName(String name) {
        if (PatternProps.isIdentifier(name)) {
            return parseArgNumber(name, 0, name.length());
        }
        return -2;
    }

    public String autoQuoteApostropheDeep() {
        if (!this.needsAutoQuoting) {
            return this.msg;
        }
        StringBuilder modified = null;
        int i = countParts();
        while (i > 0) {
            i--;
            Part part = getPart(i);
            Part part2 = part;
            if (part.getType() == Type.INSERT_CHAR) {
                if (modified == null) {
                    modified = new StringBuilder(this.msg.length() + 10).append(this.msg);
                }
                modified.insert(part2.index, (char) part2.value);
            }
        }
        if (modified == null) {
            return this.msg;
        }
        return modified.toString();
    }

    public int countParts() {
        return this.parts.size();
    }

    public Part getPart(int i) {
        return (Part) this.parts.get(i);
    }

    public Type getPartType(int i) {
        return ((Part) this.parts.get(i)).type;
    }

    public int getPatternIndex(int partIndex) {
        return ((Part) this.parts.get(partIndex)).index;
    }

    public String getSubstring(Part part) {
        int index = part.index;
        return this.msg.substring(index, part.length + index);
    }

    public boolean partSubstringMatches(Part part, String s) {
        return part.length == s.length() && this.msg.regionMatches(part.index, s, 0, part.length);
    }

    public double getNumericValue(Part part) {
        Type type = part.type;
        if (type == Type.ARG_INT) {
            return (double) part.value;
        }
        if (type == Type.ARG_DOUBLE) {
            return ((Double) this.numericValues.get(part.value)).doubleValue();
        }
        return -1.23456789E8d;
    }

    public double getPluralOffset(int pluralStart) {
        Part part = (Part) this.parts.get(pluralStart);
        if (part.type.hasNumericValue()) {
            return getNumericValue(part);
        }
        return 0.0d;
    }

    public int getLimitPartIndex(int start) {
        int limit = ((Part) this.parts.get(start)).limitPartIndex;
        if (limit < start) {
            return start;
        }
        return limit;
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    public MessagePattern cloneAsThawed() {
        try {
            MessagePattern newMsg = (MessagePattern) super.clone();
            newMsg.parts = (ArrayList) this.parts.clone();
            if (this.numericValues != null) {
                newMsg.numericValues = (ArrayList) this.numericValues.clone();
            }
            newMsg.frozen = false;
            return newMsg;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    public MessagePattern freeze() {
        this.frozen = true;
        return this;
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    private void preParse(String pattern) {
        if (isFrozen()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempt to parse(");
            stringBuilder.append(prefix(pattern));
            stringBuilder.append(") on frozen MessagePattern instance.");
            throw new UnsupportedOperationException(stringBuilder.toString());
        }
        this.msg = pattern;
        this.hasArgNumbers = false;
        this.hasArgNames = false;
        this.needsAutoQuoting = false;
        this.parts.clear();
        if (this.numericValues != null) {
            this.numericValues.clear();
        }
    }

    private void postParse() {
    }

    private int parseMessage(int index, int msgStartLength, int nestingLevel, ArgType parentType) {
        if (nestingLevel <= 32767) {
            int msgStart = this.parts.size();
            addPart(Type.MSG_START, index, msgStartLength, nestingLevel);
            index += msgStartLength;
            while (index < this.msg.length()) {
                int index2;
                int index3 = index + 1;
                char c = this.msg.charAt(index);
                if (c == PatternTokenizer.SINGLE_QUOTE) {
                    if (index3 == this.msg.length()) {
                        addPart(Type.INSERT_CHAR, index3, 0, 39);
                        this.needsAutoQuoting = true;
                    } else {
                        c = this.msg.charAt(index3);
                        if (c == PatternTokenizer.SINGLE_QUOTE) {
                            int index4 = index3 + 1;
                            addPart(Type.SKIP_SYNTAX, index3, 1, 0);
                            index = index4;
                        } else if (this.aposMode == ApostropheMode.DOUBLE_REQUIRED || c == '{' || c == '}' || ((parentType == ArgType.CHOICE && c == '|') || (parentType.hasPluralStyle() && c == '#'))) {
                            int index5;
                            addPart(Type.SKIP_SYNTAX, index3 - 1, 1, 0);
                            while (true) {
                                index2 = this.msg.indexOf(39, index3 + 1);
                                if (index2 < 0) {
                                    index2 = this.msg.length();
                                    addPart(Type.INSERT_CHAR, index2, 0, 39);
                                    this.needsAutoQuoting = true;
                                    break;
                                } else if (index2 + 1 >= this.msg.length() || this.msg.charAt(index2 + 1) != PatternTokenizer.SINGLE_QUOTE) {
                                    index5 = index2 + 1;
                                    addPart(Type.SKIP_SYNTAX, index2, 1, 0);
                                    index = index5;
                                } else {
                                    index3 = index2 + 1;
                                    addPart(Type.SKIP_SYNTAX, index3, 1, 0);
                                }
                            }
                            index5 = index2 + 1;
                            addPart(Type.SKIP_SYNTAX, index2, 1, 0);
                            index = index5;
                        } else {
                            addPart(Type.INSERT_CHAR, index3, 0, 39);
                            this.needsAutoQuoting = true;
                        }
                    }
                    index = index3;
                } else if (parentType.hasPluralStyle() && c == '#') {
                    addPart(Type.REPLACE_NUMBER, index3 - 1, 1, 0);
                    index = index3;
                } else if (c == '{') {
                    index2 = parseArg(index3 - 1, 1, nestingLevel);
                } else {
                    if ((nestingLevel > 0 && c == '}') || (parentType == ArgType.CHOICE && c == '|')) {
                        int limitLength = (parentType == ArgType.CHOICE && c == '}') ? 0 : 1;
                        addLimitPart(msgStart, Type.MSG_LIMIT, index3 - 1, limitLength, nestingLevel);
                        if (parentType == ArgType.CHOICE) {
                            return index3 - 1;
                        }
                        return index3;
                    }
                    index = index3;
                }
                index = index2;
            }
            if (nestingLevel <= 0 || inTopLevelChoiceMessage(nestingLevel, parentType)) {
                addLimitPart(msgStart, Type.MSG_LIMIT, index, 0, nestingLevel);
                return index;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unmatched '{' braces in message ");
            stringBuilder.append(prefix());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        throw new IndexOutOfBoundsException();
    }

    private int parseArg(int index, int argStartLength, int nestingLevel) {
        int argStart = this.parts.size();
        ArgType argType = ArgType.NONE;
        addPart(Type.ARG_START, index, argStartLength, argType.ordinal());
        int skipWhiteSpace = skipWhiteSpace(index + argStartLength);
        index = skipWhiteSpace;
        int nameIndex = skipWhiteSpace;
        StringBuilder stringBuilder;
        if (index != this.msg.length()) {
            index = skipIdentifier(index);
            int number = parseArgNumber(nameIndex, index);
            int length;
            if (number >= 0) {
                length = index - nameIndex;
                if (length > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH || number > 32767) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Argument number too large: ");
                    stringBuilder.append(prefix(nameIndex));
                    throw new IndexOutOfBoundsException(stringBuilder.toString());
                }
                this.hasArgNumbers = true;
                addPart(Type.ARG_NUMBER, nameIndex, length, number);
            } else if (number == -1) {
                length = index - nameIndex;
                if (length <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                    this.hasArgNames = true;
                    addPart(Type.ARG_NAME, nameIndex, length, 0);
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Argument name too long: ");
                    stringBuilder.append(prefix(nameIndex));
                    throw new IndexOutOfBoundsException(stringBuilder.toString());
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad argument syntax: ");
                stringBuilder.append(prefix(nameIndex));
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            index = skipWhiteSpace(index);
            if (index != this.msg.length()) {
                int typeIndex;
                char c = this.msg.charAt(index);
                if (c != '}') {
                    if (c == ',') {
                        typeIndex = skipWhiteSpace(index + 1);
                        index = typeIndex;
                        while (index < this.msg.length() && isArgTypeChar(this.msg.charAt(index))) {
                            index++;
                        }
                        int length2 = index - typeIndex;
                        index = skipWhiteSpace(index);
                        if (index != this.msg.length()) {
                            if (length2 != 0) {
                                char charAt = this.msg.charAt(index);
                                c = charAt;
                                if (charAt == ',' || c == '}') {
                                    if (length2 <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                                        argType = ArgType.SIMPLE;
                                        if (length2 == 6) {
                                            if (isChoice(typeIndex)) {
                                                argType = ArgType.CHOICE;
                                            } else if (isPlural(typeIndex)) {
                                                argType = ArgType.PLURAL;
                                            } else if (isSelect(typeIndex)) {
                                                argType = ArgType.SELECT;
                                            }
                                        } else if (length2 == 13 && isSelect(typeIndex) && isOrdinal(typeIndex + 6)) {
                                            argType = ArgType.SELECTORDINAL;
                                        }
                                        ((Part) this.parts.get(argStart)).value = (short) argType.ordinal();
                                        if (argType == ArgType.SIMPLE) {
                                            addPart(Type.ARG_TYPE, typeIndex, length2, 0);
                                        }
                                        if (c != '}') {
                                            index++;
                                            if (argType == ArgType.SIMPLE) {
                                                index = parseSimpleStyle(index);
                                            } else if (argType == ArgType.CHOICE) {
                                                index = parseChoiceStyle(index, nestingLevel);
                                            } else {
                                                index = parsePluralOrSelectStyle(argType, index, nestingLevel);
                                            }
                                        } else if (argType != ArgType.SIMPLE) {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("No style field for complex argument: ");
                                            stringBuilder.append(prefix(nameIndex));
                                            throw new IllegalArgumentException(stringBuilder.toString());
                                        }
                                    }
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Argument type name too long: ");
                                    stringBuilder.append(prefix(nameIndex));
                                    throw new IndexOutOfBoundsException(stringBuilder.toString());
                                }
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Bad argument syntax: ");
                            stringBuilder.append(prefix(nameIndex));
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unmatched '{' braces in message ");
                        stringBuilder.append(prefix());
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad argument syntax: ");
                    stringBuilder.append(prefix(nameIndex));
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                typeIndex = argType;
                addLimitPart(argStart, Type.ARG_LIMIT, index, 1, typeIndex.ordinal());
                return index + 1;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unmatched '{' braces in message ");
            stringBuilder.append(prefix());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unmatched '{' braces in message ");
        stringBuilder.append(prefix());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private int parseSimpleStyle(int index) {
        int start = index;
        int index2 = index;
        index = 0;
        while (index2 < this.msg.length()) {
            int index3 = index2 + 1;
            index2 = this.msg.charAt(index2);
            int index4;
            if (index2 == 39) {
                index4 = this.msg.indexOf(39, index3);
                if (index4 >= 0) {
                    index2 = index4 + 1;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Quoted literal argument style text reaches to the end of the message: ");
                    stringBuilder.append(prefix(start));
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (index2 == 123) {
                index++;
            } else if (index2 == 125) {
                if (index > 0) {
                    index--;
                } else {
                    index3--;
                    index4 = index3 - start;
                    if (index4 <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                        addPart(Type.ARG_STYLE, start, index4, 0);
                        return index3;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Argument style text too long: ");
                    stringBuilder2.append(prefix(start));
                    throw new IndexOutOfBoundsException(stringBuilder2.toString());
                }
            }
            index2 = index3;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Unmatched '{' braces in message ");
        stringBuilder3.append(prefix());
        throw new IllegalArgumentException(stringBuilder3.toString());
    }

    private int parseChoiceStyle(int index, int nestingLevel) {
        int start = index;
        index = skipWhiteSpace(index);
        if (index == this.msg.length() || this.msg.charAt(index) == '}') {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Missing choice argument pattern in ");
            stringBuilder.append(prefix());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        while (true) {
            int numberIndex = index;
            index = skipDouble(index);
            int length = index - numberIndex;
            StringBuilder stringBuilder2;
            if (length == 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Bad choice pattern syntax: ");
                stringBuilder2.append(prefix(start));
                throw new IllegalArgumentException(stringBuilder2.toString());
            } else if (length <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                parseDouble(numberIndex, index, true);
                index = skipWhiteSpace(index);
                if (index != this.msg.length()) {
                    char c = this.msg.charAt(index);
                    if (c == '#' || c == '<' || c == 8804) {
                        addPart(Type.ARG_SELECTOR, index, 1, 0);
                        index = parseMessage(index + 1, 0, nestingLevel + 1, ArgType.CHOICE);
                        if (index == this.msg.length()) {
                            return index;
                        }
                        if (this.msg.charAt(index) != '}') {
                            index = skipWhiteSpace(index + 1);
                        } else if (inMessageFormatPattern(nestingLevel)) {
                            return index;
                        } else {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Bad choice pattern syntax: ");
                            stringBuilder2.append(prefix(start));
                            throw new IllegalArgumentException(stringBuilder2.toString());
                        }
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Expected choice separator (#<≤) instead of '");
                    stringBuilder2.append(c);
                    stringBuilder2.append("' in choice pattern ");
                    stringBuilder2.append(prefix(start));
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Bad choice pattern syntax: ");
                stringBuilder2.append(prefix(start));
                throw new IllegalArgumentException(stringBuilder2.toString());
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Choice number too long: ");
                stringBuilder2.append(prefix(numberIndex));
                throw new IndexOutOfBoundsException(stringBuilder2.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:52:0x0182, code skipped:
            throw new java.lang.IllegalArgumentException(r5.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int parsePluralOrSelectStyle(ArgType argType, int index, int nestingLevel) {
        boolean eos;
        StringBuilder stringBuilder;
        int start = index;
        boolean isEmpty = true;
        int index2 = index;
        boolean hasOther = false;
        while (true) {
            index2 = skipWhiteSpace(index2);
            eos = index2 == this.msg.length();
            if (!eos && this.msg.charAt(index2) != '}') {
                int selectorIndex = index2;
                int length;
                if (argType.hasPluralStyle() && this.msg.charAt(selectorIndex) == '=') {
                    index2 = skipDouble(index2 + 1);
                    length = index2 - selectorIndex;
                    if (length == 1) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad ");
                        stringBuilder.append(argType.toString().toLowerCase(Locale.ENGLISH));
                        stringBuilder.append(" pattern syntax: ");
                        stringBuilder.append(prefix(start));
                        throw new IllegalArgumentException(stringBuilder.toString());
                    } else if (length <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                        addPart(Type.ARG_SELECTOR, selectorIndex, length, 0);
                        parseDouble(selectorIndex + 1, index2, false);
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Argument selector too long: ");
                        stringBuilder.append(prefix(selectorIndex));
                        throw new IndexOutOfBoundsException(stringBuilder.toString());
                    }
                }
                index2 = skipIdentifier(index2);
                length = index2 - selectorIndex;
                if (length == 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad ");
                    stringBuilder.append(argType.toString().toLowerCase(Locale.ENGLISH));
                    stringBuilder.append(" pattern syntax: ");
                    stringBuilder.append(prefix(start));
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (argType.hasPluralStyle() && length == 6 && index2 < this.msg.length() && this.msg.regionMatches(selectorIndex, "offset:", 0, 7)) {
                    if (isEmpty) {
                        int valueIndex = skipWhiteSpace(index2 + 1);
                        index2 = skipDouble(valueIndex);
                        StringBuilder stringBuilder2;
                        if (index2 == valueIndex) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Missing value for plural 'offset:' ");
                            stringBuilder2.append(prefix(start));
                            throw new IllegalArgumentException(stringBuilder2.toString());
                        } else if (index2 - valueIndex <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                            parseDouble(valueIndex, index2, false);
                            isEmpty = false;
                        } else {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Plural offset value too long: ");
                            stringBuilder2.append(prefix(valueIndex));
                            throw new IndexOutOfBoundsException(stringBuilder2.toString());
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Plural argument 'offset:' (if present) must precede key-message pairs: ");
                    stringBuilder.append(prefix(start));
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (length <= DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
                    addPart(Type.ARG_SELECTOR, selectorIndex, length, 0);
                    if (this.msg.regionMatches(selectorIndex, PluralRules.KEYWORD_OTHER, 0, length)) {
                        hasOther = true;
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Argument selector too long: ");
                    stringBuilder.append(prefix(selectorIndex));
                    throw new IndexOutOfBoundsException(stringBuilder.toString());
                }
                index2 = skipWhiteSpace(index2);
                if (index2 == this.msg.length() || this.msg.charAt(index2) != '{') {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No message fragment after ");
                    stringBuilder.append(argType.toString().toLowerCase(Locale.ENGLISH));
                    stringBuilder.append(" selector: ");
                    stringBuilder.append(prefix(selectorIndex));
                } else {
                    index2 = parseMessage(index2, 1, nestingLevel + 1, argType);
                    isEmpty = false;
                }
            }
        }
        if (eos == inMessageFormatPattern(nestingLevel)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad ");
            stringBuilder.append(argType.toString().toLowerCase(Locale.ENGLISH));
            stringBuilder.append(" pattern syntax: ");
            stringBuilder.append(prefix(start));
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (hasOther) {
            return index2;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Missing 'other' keyword in ");
            stringBuilder.append(argType.toString().toLowerCase(Locale.ENGLISH));
            stringBuilder.append(" pattern in ");
            stringBuilder.append(prefix());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int parseArgNumber(CharSequence s, int c, int limit) {
        if (c >= limit) {
            return -2;
        }
        int number;
        boolean badNumber;
        int start = c + 1;
        char c2 = s.charAt(c);
        if (c2 == '0') {
            if (start == limit) {
                return 0;
            }
            number = 0;
            badNumber = true;
        } else if ('1' > c2 || c2 > '9') {
            return -1;
        } else {
            number = c2 - 48;
            badNumber = false;
        }
        while (start < limit) {
            int start2 = start + 1;
            c2 = s.charAt(start);
            if ('0' > c2 || c2 > '9') {
                return -1;
            }
            if (number >= 214748364) {
                badNumber = true;
            }
            number = (number * 10) + (c2 - 48);
            start = start2;
        }
        if (badNumber) {
            return -2;
        }
        return number;
    }

    private int parseArgNumber(int start, int limit) {
        return parseArgNumber(this.msg, start, limit);
    }

    private void parseDouble(int start, int limit, boolean allowInfinity) {
        int index;
        StringBuilder stringBuilder;
        int value = 0;
        int isNegative = 0;
        char c = start;
        int index2 = c + 1;
        c = this.msg.charAt(c);
        if (c == '-') {
            isNegative = 1;
            if (index2 != limit) {
                index = index2 + 1;
                c = this.msg.charAt(index2);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad syntax for numeric value: ");
            stringBuilder.append(this.msg.substring(start, limit));
            throw new NumberFormatException(stringBuilder.toString());
        } else if (c == '+') {
            if (index2 != limit) {
                index = index2 + 1;
                c = this.msg.charAt(index2);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad syntax for numeric value: ");
            stringBuilder.append(this.msg.substring(start, limit));
            throw new NumberFormatException(stringBuilder.toString());
        } else {
            index = index2;
        }
        if (c == 8734) {
            if (allowInfinity && index == limit) {
                addArgDoublePart(isNegative != 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY, start, limit - start);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad syntax for numeric value: ");
            stringBuilder.append(this.msg.substring(start, limit));
            throw new NumberFormatException(stringBuilder.toString());
        }
        while ('0' <= c && c <= '9') {
            value = (value * 10) + (c - 48);
            if (value > 32767 + isNegative) {
                break;
            } else if (index == limit) {
                addPart(Type.ARG_INT, start, limit - start, isNegative != 0 ? -value : value);
                return;
            } else {
                index2 = index + 1;
                c = this.msg.charAt(index);
                index = index2;
            }
        }
        addArgDoublePart(Double.parseDouble(this.msg.substring(start, limit)), start, limit - start);
    }

    static void appendReducedApostrophes(String s, int start, int limit, StringBuilder sb) {
        int doubleApos = -1;
        while (true) {
            int i = s.indexOf(39, start);
            if (i < 0 || i >= limit) {
                sb.append(s, start, limit);
            } else if (i == doubleApos) {
                sb.append(PatternTokenizer.SINGLE_QUOTE);
                start++;
                doubleApos = -1;
            } else {
                sb.append(s, start, i);
                int i2 = i + 1;
                start = i2;
                doubleApos = i2;
            }
        }
        sb.append(s, start, limit);
    }

    private int skipWhiteSpace(int index) {
        return PatternProps.skipWhiteSpace(this.msg, index);
    }

    private int skipIdentifier(int index) {
        return PatternProps.skipIdentifier(this.msg, index);
    }

    private int skipDouble(int index) {
        while (index < this.msg.length()) {
            char c = this.msg.charAt(index);
            if ((c < '0' && "+-.".indexOf(c) < 0) || (c > '9' && c != 'e' && c != 'E' && c != 8734)) {
                break;
            }
            index++;
        }
        return index;
    }

    private static boolean isArgTypeChar(int c) {
        return (97 <= c && c <= 122) || (65 <= c && c <= 90);
    }

    private boolean isChoice(int index) {
        int index2 = index + 1;
        char charAt = this.msg.charAt(index);
        char c = charAt;
        if (charAt == 'c' || c == 'C') {
            int index3 = index2 + 1;
            charAt = this.msg.charAt(index2);
            c = charAt;
            if (charAt == 'h' || c == 'H') {
                index2 = index3 + 1;
                charAt = this.msg.charAt(index3);
                c = charAt;
                if (charAt == 'o' || c == 'O') {
                    index3 = index2 + 1;
                    charAt = this.msg.charAt(index2);
                    c = charAt;
                    if (charAt == UCharacterProperty.LATIN_SMALL_LETTER_I_ || c == 'I') {
                        index2 = index3 + 1;
                        charAt = this.msg.charAt(index3);
                        c = charAt;
                        if (charAt == 'c' || c == 'C') {
                            charAt = this.msg.charAt(index2);
                            c = charAt;
                            if (charAt == 'e' || c == 'E') {
                                return true;
                            }
                        }
                    }
                }
            }
            index2 = index3;
        }
        return false;
    }

    private boolean isPlural(int index) {
        int index2 = index + 1;
        char charAt = this.msg.charAt(index);
        char c = charAt;
        int index3;
        if (charAt == 'p' || c == 'P') {
            int index4 = index2 + 1;
            charAt = this.msg.charAt(index2);
            c = charAt;
            if (charAt == 'l' || c == 'L') {
                index3 = index4 + 1;
                charAt = this.msg.charAt(index4);
                c = charAt;
                if (charAt == 'u' || c == 'U') {
                    index4 = index3 + 1;
                    charAt = this.msg.charAt(index3);
                    c = charAt;
                    if (charAt == 'r' || c == 'R') {
                        index3 = index4 + 1;
                        charAt = this.msg.charAt(index4);
                        c = charAt;
                        if (charAt == 'a' || c == 'A') {
                            charAt = this.msg.charAt(index3);
                            c = charAt;
                            if (charAt == 'l' || c == 'L') {
                                return true;
                            }
                        }
                    }
                }
            }
            index3 = index4;
        } else {
            index3 = index2;
        }
        return false;
    }

    private boolean isSelect(int index) {
        int index2 = index + 1;
        char charAt = this.msg.charAt(index);
        char c = charAt;
        if (charAt == 's' || c == 'S') {
            int index3 = index2 + 1;
            charAt = this.msg.charAt(index2);
            c = charAt;
            if (charAt == 'e' || c == 'E') {
                int index4 = index3 + 1;
                charAt = this.msg.charAt(index3);
                c = charAt;
                if (charAt == 'l' || c == 'L') {
                    index3 = index4 + 1;
                    charAt = this.msg.charAt(index4);
                    c = charAt;
                    if (charAt == 'e' || c == 'E') {
                        index2 = index3 + 1;
                        charAt = this.msg.charAt(index3);
                        c = charAt;
                        if (charAt == 'c' || c == 'C') {
                            charAt = this.msg.charAt(index2);
                            c = charAt;
                            if (charAt == 't' || c == 'T') {
                                return true;
                            }
                        }
                    }
                }
                index2 = index4;
            }
            index2 = index3;
        }
        return false;
    }

    private boolean isOrdinal(int index) {
        int index2;
        int index3 = index + 1;
        char charAt = this.msg.charAt(index);
        char c = charAt;
        if (charAt == 'o' || c == 'O') {
            index2 = index3 + 1;
            charAt = this.msg.charAt(index3);
            c = charAt;
            if (charAt == 'r' || c == 'R') {
                index3 = index2 + 1;
                charAt = this.msg.charAt(index2);
                c = charAt;
                if (charAt == 'd' || c == 'D') {
                    index2 = index3 + 1;
                    charAt = this.msg.charAt(index3);
                    c = charAt;
                    if (charAt == UCharacterProperty.LATIN_SMALL_LETTER_I_ || c == 'I') {
                        index3 = index2 + 1;
                        charAt = this.msg.charAt(index2);
                        c = charAt;
                        if (charAt == 'n' || c == 'N') {
                            index2 = index3 + 1;
                            charAt = this.msg.charAt(index3);
                            c = charAt;
                            if (charAt == 'a' || c == 'A') {
                                charAt = this.msg.charAt(index2);
                                c = charAt;
                                if (charAt == 'l' || c == 'L') {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
        index2 = index3;
        return false;
    }

    private boolean inMessageFormatPattern(int nestingLevel) {
        return nestingLevel > 0 || ((Part) this.parts.get(0)).type == Type.MSG_START;
    }

    private boolean inTopLevelChoiceMessage(int nestingLevel, ArgType parentType) {
        return nestingLevel == 1 && parentType == ArgType.CHOICE && ((Part) this.parts.get(0)).type != Type.MSG_START;
    }

    private void addPart(Type type, int index, int length, int value) {
        this.parts.add(new Part(type, index, length, value));
    }

    private void addLimitPart(int start, Type type, int index, int length, int value) {
        ((Part) this.parts.get(start)).limitPartIndex = this.parts.size();
        addPart(type, index, length, value);
    }

    private void addArgDoublePart(double numericValue, int start, int length) {
        int numericIndex;
        if (this.numericValues == null) {
            this.numericValues = new ArrayList();
            numericIndex = 0;
        } else {
            numericIndex = this.numericValues.size();
            if (numericIndex > 32767) {
                throw new IndexOutOfBoundsException("Too many numeric values");
            }
        }
        this.numericValues.add(Double.valueOf(numericValue));
        addPart(Type.ARG_DOUBLE, start, length, numericIndex);
    }

    private static String prefix(String s, int start) {
        StringBuilder prefix = new StringBuilder(44);
        if (start == 0) {
            prefix.append("\"");
        } else {
            prefix.append("[at pattern index ");
            prefix.append(start);
            prefix.append("] \"");
        }
        if (s.length() - start <= 24) {
            prefix.append(start == 0 ? s : s.substring(start));
        } else {
            int limit = (start + 24) - 4;
            if (Character.isHighSurrogate(s.charAt(limit - 1))) {
                limit--;
            }
            prefix.append(s, start, limit);
            prefix.append(" ...");
        }
        prefix.append("\"");
        return prefix.toString();
    }

    private static String prefix(String s) {
        return prefix(s, 0);
    }

    private String prefix(int start) {
        return prefix(this.msg, start);
    }

    private String prefix() {
        return prefix(this.msg, 0);
    }
}
