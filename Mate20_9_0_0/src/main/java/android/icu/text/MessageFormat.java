package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.text.MessagePattern.ApostropheMode;
import android.icu.text.MessagePattern.ArgType;
import android.icu.text.MessagePattern.Part;
import android.icu.text.MessagePattern.Part.Type;
import android.icu.text.PluralRules.PluralType;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MessageFormat extends UFormat {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final char CURLY_BRACE_LEFT = '{';
    private static final char CURLY_BRACE_RIGHT = '}';
    private static final int DATE_MODIFIER_EMPTY = 0;
    private static final int DATE_MODIFIER_FULL = 4;
    private static final int DATE_MODIFIER_LONG = 3;
    private static final int DATE_MODIFIER_MEDIUM = 2;
    private static final int DATE_MODIFIER_SHORT = 1;
    private static final int MODIFIER_CURRENCY = 1;
    private static final int MODIFIER_EMPTY = 0;
    private static final int MODIFIER_INTEGER = 3;
    private static final int MODIFIER_PERCENT = 2;
    private static final char SINGLE_QUOTE = '\'';
    private static final int STATE_INITIAL = 0;
    private static final int STATE_IN_QUOTE = 2;
    private static final int STATE_MSG_ELEMENT = 3;
    private static final int STATE_SINGLE_QUOTE = 1;
    private static final int TYPE_DATE = 1;
    private static final int TYPE_DURATION = 5;
    private static final int TYPE_NUMBER = 0;
    private static final int TYPE_ORDINAL = 4;
    private static final int TYPE_SPELLOUT = 3;
    private static final int TYPE_TIME = 2;
    private static final String[] dateModifierList = new String[]{"", "short", "medium", "long", "full"};
    private static final String[] modifierList = new String[]{"", "currency", "percent", "integer"};
    private static final Locale rootLocale = new Locale("");
    static final long serialVersionUID = 7136212545847378652L;
    private static final String[] typeList = new String[]{"number", "date", "time", "spellout", "ordinal", "duration"};
    private transient Map<Integer, Format> cachedFormatters;
    private transient Set<Integer> customFormatArgStarts;
    private transient MessagePattern msgPattern;
    private transient PluralSelectorProvider ordinalProvider;
    private transient PluralSelectorProvider pluralProvider;
    private transient DateFormat stockDateFormatter;
    private transient NumberFormat stockNumberFormatter;
    private transient ULocale ulocale;

    private static final class AppendableWrapper {
        private Appendable app;
        private List<AttributeAndPosition> attributes = null;
        private int length;

        public AppendableWrapper(StringBuilder sb) {
            this.app = sb;
            this.length = sb.length();
        }

        public AppendableWrapper(StringBuffer sb) {
            this.app = sb;
            this.length = sb.length();
        }

        public void useAttributes() {
            this.attributes = new ArrayList();
        }

        public void append(CharSequence s) {
            try {
                this.app.append(s);
                this.length += s.length();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharSequence s, int start, int limit) {
            try {
                this.app.append(s, start, limit);
                this.length += limit - start;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void append(CharacterIterator iterator) {
            this.length += append(this.app, iterator);
        }

        public static int append(Appendable result, CharacterIterator iterator) {
            try {
                int start = iterator.getBeginIndex();
                int limit = iterator.getEndIndex();
                int length = limit - start;
                if (start < limit) {
                    result.append(iterator.first());
                    while (true) {
                        start++;
                        if (start >= limit) {
                            break;
                        }
                        result.append(iterator.next());
                    }
                }
                return length;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void formatAndAppend(Format formatter, Object arg) {
            if (this.attributes == null) {
                append(formatter.format(arg));
                return;
            }
            CharacterIterator formattedArg = formatter.formatToCharacterIterator(arg);
            int prevLength = this.length;
            append(formattedArg);
            formattedArg.first();
            int start = formattedArg.getIndex();
            int limit = formattedArg.getEndIndex();
            int offset = prevLength - start;
            while (start < limit) {
                Map<Attribute, Object> map = formattedArg.getAttributes();
                int runLimit = formattedArg.getRunLimit();
                if (map.size() != 0) {
                    for (Entry<Attribute, Object> entry : map.entrySet()) {
                        this.attributes.add(new AttributeAndPosition((Attribute) entry.getKey(), entry.getValue(), offset + start, offset + runLimit));
                    }
                }
                start = runLimit;
                formattedArg.setIndex(start);
            }
        }

        public void formatAndAppend(Format formatter, Object arg, String argString) {
            if (this.attributes != null || argString == null) {
                formatAndAppend(formatter, arg);
            } else {
                append((CharSequence) argString);
            }
        }
    }

    private static final class AttributeAndPosition {
        private Attribute key;
        private int limit;
        private int start;
        private Object value;

        public AttributeAndPosition(Object fieldValue, int startIndex, int limitIndex) {
            init(Field.ARGUMENT, fieldValue, startIndex, limitIndex);
        }

        public AttributeAndPosition(Attribute field, Object fieldValue, int startIndex, int limitIndex) {
            init(field, fieldValue, startIndex, limitIndex);
        }

        public void init(Attribute field, Object fieldValue, int startIndex, int limitIndex) {
            this.key = field;
            this.value = fieldValue;
            this.start = startIndex;
            this.limit = limitIndex;
        }
    }

    public static class Field extends java.text.Format.Field {
        public static final Field ARGUMENT = new Field("message argument field");
        private static final long serialVersionUID = 7510380454602616157L;

        protected Field(String name) {
            super(name);
        }

        protected Object readResolve() throws InvalidObjectException {
            if (getClass() != Field.class) {
                throw new InvalidObjectException("A subclass of MessageFormat.Field must implement readResolve.");
            } else if (getName().equals(ARGUMENT.getName())) {
                return ARGUMENT;
            } else {
                throw new InvalidObjectException("Unknown attribute name.");
            }
        }
    }

    private static final class PluralSelectorContext {
        String argName;
        boolean forReplaceNumber;
        Format formatter;
        Number number;
        int numberArgIndex;
        String numberString;
        double offset;
        int startIndex;

        private PluralSelectorContext(int start, String name, Number num, double off) {
            this.startIndex = start;
            this.argName = name;
            if (off == 0.0d) {
                this.number = num;
            } else {
                this.number = Double.valueOf(num.doubleValue() - off);
            }
            this.offset = off;
        }

        public String toString() {
            throw new AssertionError("PluralSelectorContext being formatted, rather than its number");
        }
    }

    private static final class PluralSelectorProvider implements PluralSelector {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private MessageFormat msgFormat;
        private PluralRules rules;
        private PluralType type;

        static {
            Class cls = MessageFormat.class;
        }

        public PluralSelectorProvider(MessageFormat mf, PluralType type) {
            this.msgFormat = mf;
            this.type = type;
        }

        public String select(Object ctx, double number) {
            if (this.rules == null) {
                this.rules = PluralRules.forLocale(this.msgFormat.ulocale, this.type);
            }
            PluralSelectorContext context = (PluralSelectorContext) ctx;
            context.numberArgIndex = this.msgFormat.findFirstPluralNumberArg(this.msgFormat.findOtherSubMessage(context.startIndex), context.argName);
            if (context.numberArgIndex > 0 && this.msgFormat.cachedFormatters != null) {
                context.formatter = (Format) this.msgFormat.cachedFormatters.get(Integer.valueOf(context.numberArgIndex));
            }
            if (context.formatter == null) {
                context.formatter = this.msgFormat.getStockNumberFormatter();
                context.forReplaceNumber = true;
            }
            context.numberString = context.formatter.format(context.number);
            if (!(context.formatter instanceof DecimalFormat)) {
                return this.rules.select(number);
            }
            return this.rules.select(((DecimalFormat) context.formatter).getFixedDecimal(number));
        }
    }

    public MessageFormat(String pattern) {
        this.ulocale = ULocale.getDefault(Category.FORMAT);
        applyPattern(pattern);
    }

    public MessageFormat(String pattern, Locale locale) {
        this(pattern, ULocale.forLocale(locale));
    }

    public MessageFormat(String pattern, ULocale locale) {
        this.ulocale = locale;
        applyPattern(pattern);
    }

    public void setLocale(Locale locale) {
        setLocale(ULocale.forLocale(locale));
    }

    public void setLocale(ULocale locale) {
        String existingPattern = toPattern();
        this.ulocale = locale;
        this.stockDateFormatter = null;
        this.stockNumberFormatter = null;
        this.pluralProvider = null;
        this.ordinalProvider = null;
        applyPattern(existingPattern);
    }

    public Locale getLocale() {
        return this.ulocale.toLocale();
    }

    public ULocale getULocale() {
        return this.ulocale;
    }

    public void applyPattern(String pttrn) {
        try {
            if (this.msgPattern == null) {
                this.msgPattern = new MessagePattern(pttrn);
            } else {
                this.msgPattern.parse(pttrn);
            }
            cacheExplicitFormats();
        } catch (RuntimeException e) {
            resetPattern();
            throw e;
        }
    }

    public void applyPattern(String pattern, ApostropheMode aposMode) {
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern(aposMode);
        } else if (aposMode != this.msgPattern.getApostropheMode()) {
            this.msgPattern.clearPatternAndSetApostropheMode(aposMode);
        }
        applyPattern(pattern);
    }

    public ApostropheMode getApostropheMode() {
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        return this.msgPattern.getApostropheMode();
    }

    public String toPattern() {
        if (this.customFormatArgStarts != null) {
            throw new IllegalStateException("toPattern() is not supported after custom Format objects have been set via setFormat() or similar APIs");
        } else if (this.msgPattern == null) {
            return "";
        } else {
            String originalPattern = this.msgPattern.getPatternString();
            return originalPattern == null ? "" : originalPattern;
        }
    }

    private int nextTopLevelArgStart(int partIndex) {
        if (partIndex != 0) {
            partIndex = this.msgPattern.getLimitPartIndex(partIndex);
        }
        while (true) {
            partIndex++;
            Type type = this.msgPattern.getPartType(partIndex);
            if (type == Type.ARG_START) {
                return partIndex;
            }
            if (type == Type.MSG_LIMIT) {
                return -1;
            }
        }
    }

    private boolean argNameMatches(int partIndex, String argName, int argNumber) {
        Part part = this.msgPattern.getPart(partIndex);
        if (part.getType() == Type.ARG_NAME) {
            return this.msgPattern.partSubstringMatches(part, argName);
        }
        return part.getValue() == argNumber;
    }

    private String getArgName(int partIndex) {
        Part part = this.msgPattern.getPart(partIndex);
        if (part.getType() == Type.ARG_NAME) {
            return this.msgPattern.getSubstring(part);
        }
        return Integer.toString(part.getValue());
    }

    public void setFormatsByArgumentIndex(Format[] newFormats) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart >= 0) {
                nextTopLevelArgStart = this.msgPattern.getPart(partIndex + 1).getValue();
                if (nextTopLevelArgStart < newFormats.length) {
                    setCustomArgStartFormat(partIndex, newFormats[nextTopLevelArgStart]);
                }
            } else {
                return;
            }
        }
    }

    public void setFormatsByArgumentName(Map<String, Format> newFormats) {
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart >= 0) {
                String key = getArgName(partIndex + 1);
                if (newFormats.containsKey(key)) {
                    setCustomArgStartFormat(partIndex, (Format) newFormats.get(key));
                }
            } else {
                return;
            }
        }
    }

    public void setFormats(Format[] newFormats) {
        int formatNumber = 0;
        int partIndex = 0;
        while (formatNumber < newFormats.length) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart >= 0) {
                setCustomArgStartFormat(partIndex, newFormats[formatNumber]);
                formatNumber++;
            } else {
                return;
            }
        }
    }

    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart < 0) {
                return;
            }
            if (this.msgPattern.getPart(partIndex + 1).getValue() == argumentIndex) {
                setCustomArgStartFormat(partIndex, newFormat);
            }
        }
    }

    public void setFormatByArgumentName(String argumentName, Format newFormat) {
        int argNumber = MessagePattern.validateArgumentName(argumentName);
        if (argNumber >= -1) {
            int partIndex = 0;
            while (true) {
                int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
                partIndex = nextTopLevelArgStart;
                if (nextTopLevelArgStart < 0) {
                    return;
                }
                if (argNameMatches(partIndex + 1, argumentName, argNumber)) {
                    setCustomArgStartFormat(partIndex, newFormat);
                }
            }
        }
    }

    public void setFormat(int formatElementIndex, Format newFormat) {
        int formatNumber = 0;
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart < 0) {
                throw new ArrayIndexOutOfBoundsException(formatElementIndex);
            } else if (formatNumber == formatElementIndex) {
                setCustomArgStartFormat(partIndex, newFormat);
                return;
            } else {
                formatNumber++;
            }
        }
    }

    public Format[] getFormatsByArgumentIndex() {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
        }
        ArrayList<Format> list = new ArrayList();
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart < 0) {
                return (Format[]) list.toArray(new Format[list.size()]);
            }
            Object obj;
            nextTopLevelArgStart = this.msgPattern.getPart(partIndex + 1).getValue();
            while (true) {
                obj = null;
                if (nextTopLevelArgStart < list.size()) {
                    break;
                }
                list.add(null);
            }
            if (this.cachedFormatters != null) {
                obj = (Format) this.cachedFormatters.get(Integer.valueOf(partIndex));
            }
            list.set(nextTopLevelArgStart, obj);
        }
    }

    public Format[] getFormats() {
        ArrayList<Format> list = new ArrayList();
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart < 0) {
                return (Format[]) list.toArray(new Format[list.size()]);
            }
            list.add(this.cachedFormatters == null ? null : (Format) this.cachedFormatters.get(Integer.valueOf(partIndex)));
        }
    }

    public Set<String> getArgumentNames() {
        Set<String> result = new HashSet();
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart < 0) {
                return result;
            }
            result.add(getArgName(partIndex + 1));
        }
    }

    public Format getFormatByArgumentName(String argumentName) {
        if (this.cachedFormatters == null) {
            return null;
        }
        int argNumber = MessagePattern.validateArgumentName(argumentName);
        if (argNumber < -1) {
            return null;
        }
        int partIndex = 0;
        do {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart < 0) {
                return null;
            }
        } while (!argNameMatches(partIndex + 1, argumentName, argNumber));
        return (Format) this.cachedFormatters.get(Integer.valueOf(partIndex));
    }

    public final StringBuffer format(Object[] arguments, StringBuffer result, FieldPosition pos) {
        format(arguments, null, new AppendableWrapper(result), pos);
        return result;
    }

    public final StringBuffer format(Map<String, Object> arguments, StringBuffer result, FieldPosition pos) {
        format(null, arguments, new AppendableWrapper(result), pos);
        return result;
    }

    public static String format(String pattern, Object... arguments) {
        return new MessageFormat(pattern).format(arguments);
    }

    public static String format(String pattern, Map<String, Object> arguments) {
        return new MessageFormat(pattern).format(arguments);
    }

    public boolean usesNamedArguments() {
        return this.msgPattern.hasNamedArguments();
    }

    public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
        format(arguments, new AppendableWrapper(result), pos);
        return result;
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        if (arguments != null) {
            StringBuilder result = new StringBuilder();
            AppendableWrapper wrapper = new AppendableWrapper(result);
            wrapper.useAttributes();
            format(arguments, wrapper, null);
            AttributedString as = new AttributedString(result.toString());
            for (AttributeAndPosition a : wrapper.attributes) {
                as.addAttribute(a.key, a.value, a.start, a.limit);
            }
            return as.getIterator();
        }
        throw new NullPointerException("formatToCharacterIterator must be passed non-null object");
    }

    public Object[] parse(String source, ParsePosition pos) {
        if (this.msgPattern.hasNamedArguments()) {
            throw new IllegalArgumentException("This method is not available in MessageFormat objects that use named argument.");
        }
        int maxArgId = -1;
        int partIndex = 0;
        while (true) {
            int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
            partIndex = nextTopLevelArgStart;
            if (nextTopLevelArgStart < 0) {
                break;
            }
            nextTopLevelArgStart = this.msgPattern.getPart(partIndex + 1).getValue();
            if (nextTopLevelArgStart > maxArgId) {
                maxArgId = nextTopLevelArgStart;
            }
        }
        Object[] resultArray = new Object[(maxArgId + 1)];
        int backupStartPos = pos.getIndex();
        parse(0, source, pos, resultArray, null);
        if (pos.getIndex() == backupStartPos) {
            return null;
        }
        return resultArray;
    }

    public Map<String, Object> parseToMap(String source, ParsePosition pos) {
        Map<String, Object> result = new HashMap();
        int backupStartPos = pos.getIndex();
        parse(0, source, pos, null, result);
        if (pos.getIndex() == backupStartPos) {
            return null;
        }
        return result;
    }

    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.getIndex() != 0) {
            return result;
        }
        throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());
    }

    /* JADX WARNING: Removed duplicated region for block: B:75:0x01b5  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x01b5  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parse(int msgStart, String source, ParsePosition pos, Object[] args, Map<String, Object> argsMap) {
        int i = msgStart;
        String str = source;
        ParsePosition parsePosition = pos;
        Map<String, Object> map = argsMap;
        if (str != null) {
            String msgString = this.msgPattern.getPatternString();
            int prevIndex = this.msgPattern.getPart(i).getLimit();
            int sourceOffset = pos.getIndex();
            ParsePosition tempStatus = new ParsePosition(0);
            int i2 = i + 1;
            while (true) {
                Part part = this.msgPattern.getPart(i2);
                Type type = part.getType();
                int index = part.getIndex();
                int len = index - prevIndex;
                if (len == 0 || msgString.regionMatches(prevIndex, str, sourceOffset, len)) {
                    sourceOffset += len;
                    prevIndex += len;
                    if (type == Type.MSG_LIMIT) {
                        parsePosition.setIndex(sourceOffset);
                        return;
                    }
                    String msgString2;
                    int i3;
                    Type type2;
                    int i4;
                    if (type == Type.SKIP_SYNTAX) {
                        msgString2 = msgString;
                        i3 = prevIndex;
                        type2 = type;
                        i4 = index;
                    } else if (type == Type.INSERT_CHAR) {
                        msgString2 = msgString;
                        i3 = prevIndex;
                        type2 = type;
                        i4 = index;
                    } else {
                        Format formatter;
                        ArgType argType;
                        boolean haveArgResult;
                        int argLimit = this.msgPattern.getLimitPartIndex(i2);
                        ArgType argType2 = part.getArgType();
                        msgString2 = msgString;
                        i2++;
                        msgString = this.msgPattern.getPart(i2);
                        int argNumber = 0;
                        String key = null;
                        if (args != null) {
                            prevIndex = msgString.getValue();
                            part = Integer.valueOf(prevIndex);
                            argNumber = prevIndex;
                        } else {
                            Part argId = null;
                            if (msgString.getType() == Type.ARG_NAME) {
                                prevIndex = this.msgPattern.getSubstring(msgString);
                            } else {
                                prevIndex = Integer.toString(msgString.getValue());
                            }
                            key = prevIndex;
                            Object part2 = key;
                        }
                        prevIndex = key;
                        i2++;
                        boolean haveArgResult2 = false;
                        String argResult = null;
                        String part3 = msgString;
                        if (this.cachedFormatters != null) {
                            Format msgString3 = (Format) this.cachedFormatters.get(Integer.valueOf(i2 - 2));
                            formatter = msgString3;
                            if (msgString3 != null) {
                                tempStatus.setIndex(sourceOffset);
                                String argResult2 = formatter.parseObject(str, tempStatus);
                                if (tempStatus.getIndex() == sourceOffset) {
                                    parsePosition.setErrorIndex(sourceOffset);
                                    return;
                                }
                                sourceOffset = tempStatus.getIndex();
                                argType = argType2;
                                haveArgResult = true;
                                Format format = formatter;
                                i4 = index;
                                msgString = argResult2;
                                if (haveArgResult) {
                                    if (args != null) {
                                        args[argNumber] = msgString;
                                    } else if (map != null) {
                                        map.put(prevIndex, msgString);
                                    }
                                }
                                i = this.msgPattern.getPart(argLimit).getLimit();
                                i2 = argLimit;
                                prevIndex = i;
                                i2++;
                                msgString = msgString2;
                                i = msgStart;
                                str = source;
                            }
                        } else {
                            formatter = null;
                        }
                        if (argType2 != ArgType.NONE) {
                            if (this.cachedFormatters != null) {
                                if (this.cachedFormatters.containsKey(Integer.valueOf(i2 - 2)) != null) {
                                    i4 = index;
                                }
                            }
                            if (argType2 == ArgType.CHOICE) {
                                tempStatus.setIndex(sourceOffset);
                                formatter = parseChoiceArgument(this.msgPattern, i2, str, tempStatus);
                                if (tempStatus.getIndex() == sourceOffset) {
                                    parsePosition.setErrorIndex(sourceOffset);
                                    return;
                                }
                                msgString = Double.valueOf(formatter);
                                haveArgResult = true;
                                sourceOffset = tempStatus.getIndex();
                                argType = argType2;
                                if (haveArgResult) {
                                }
                                i = this.msgPattern.getPart(argLimit).getLimit();
                                i2 = argLimit;
                                prevIndex = i;
                                i2++;
                                msgString = msgString2;
                                i = msgStart;
                                str = source;
                            } else {
                                if (argType2.hasPluralStyle() != null || argType2 == ArgType.SELECT) {
                                    throw new UnsupportedOperationException("Parsing of plural/select/selectordinal argument is not supported.");
                                }
                                formatter = new StringBuilder();
                                formatter.append("unexpected argType ");
                                formatter.append(argType2);
                                throw new IllegalStateException(formatter.toString());
                            }
                        }
                        i4 = index;
                        msgString = getLiteralStringUntilNextArgument(argLimit);
                        if (msgString.length() != 0) {
                            type = str.indexOf(msgString, sourceOffset);
                        } else {
                            type = source.length();
                        }
                        if (type < null) {
                            parsePosition.setErrorIndex(sourceOffset);
                            return;
                        }
                        index = str.substring(sourceOffset, type);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("{");
                        stringBuilder.append(part.toString());
                        stringBuilder.append("}");
                        if (!index.equals(stringBuilder.toString())) {
                            haveArgResult2 = true;
                            argResult = index;
                        }
                        sourceOffset = type;
                        haveArgResult = haveArgResult2;
                        msgString = argResult;
                        if (haveArgResult) {
                        }
                        i = this.msgPattern.getPart(argLimit).getLimit();
                        i2 = argLimit;
                        prevIndex = i;
                        i2++;
                        msgString = msgString2;
                        i = msgStart;
                        str = source;
                    }
                    i = part.getLimit();
                    prevIndex = i;
                    i2++;
                    msgString = msgString2;
                    i = msgStart;
                    str = source;
                } else {
                    parsePosition.setErrorIndex(sourceOffset);
                    return;
                }
            }
        }
    }

    public Map<String, Object> parseToMap(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Map<String, Object> result = new HashMap();
        parse(0, source, pos, null, result);
        if (pos.getIndex() != 0) {
            return result;
        }
        throw new ParseException("MessageFormat parse error!", pos.getErrorIndex());
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (this.msgPattern.hasNamedArguments()) {
            return parseToMap(source, pos);
        }
        return parse(source, pos);
    }

    public Object clone() {
        MessageFormat other = (MessageFormat) super.clone();
        if (this.customFormatArgStarts != null) {
            other.customFormatArgStarts = new HashSet();
            for (Integer key : this.customFormatArgStarts) {
                other.customFormatArgStarts.add(key);
            }
        } else {
            other.customFormatArgStarts = null;
        }
        if (this.cachedFormatters != null) {
            other.cachedFormatters = new HashMap();
            for (Entry<Integer, Format> entry : this.cachedFormatters.entrySet()) {
                other.cachedFormatters.put((Integer) entry.getKey(), (Format) entry.getValue());
            }
        } else {
            other.cachedFormatters = null;
        }
        other.msgPattern = this.msgPattern == null ? null : (MessagePattern) this.msgPattern.clone();
        other.stockDateFormatter = this.stockDateFormatter == null ? null : (DateFormat) this.stockDateFormatter.clone();
        other.stockNumberFormatter = this.stockNumberFormatter == null ? null : (NumberFormat) this.stockNumberFormatter.clone();
        other.pluralProvider = null;
        other.ordinalProvider = null;
        return other;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageFormat other = (MessageFormat) obj;
        if (!(Utility.objectEquals(this.ulocale, other.ulocale) && Utility.objectEquals(this.msgPattern, other.msgPattern) && Utility.objectEquals(this.cachedFormatters, other.cachedFormatters) && Utility.objectEquals(this.customFormatArgStarts, other.customFormatArgStarts))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return this.msgPattern.getPatternString().hashCode();
    }

    private DateFormat getStockDateFormatter() {
        if (this.stockDateFormatter == null) {
            this.stockDateFormatter = DateFormat.getDateTimeInstance(3, 3, this.ulocale);
        }
        return this.stockDateFormatter;
    }

    private NumberFormat getStockNumberFormatter() {
        if (this.stockNumberFormatter == null) {
            this.stockNumberFormatter = NumberFormat.getInstance(this.ulocale);
        }
        return this.stockNumberFormatter;
    }

    private void format(int msgStart, PluralSelectorContext pluralNumber, Object[] args, Map<String, Object> argsMap, AppendableWrapper dest, FieldPosition fp) {
        int i = msgStart;
        PluralSelectorContext pluralSelectorContext = pluralNumber;
        Object[] objArr = args;
        Map map = argsMap;
        AppendableWrapper appendableWrapper = dest;
        String msgString = this.msgPattern.getPatternString();
        int prevIndex = this.msgPattern.getPart(i).getLimit();
        int i2 = i + 1;
        FieldPosition fp2 = fp;
        while (true) {
            Part part = this.msgPattern.getPart(i2);
            Type type = part.getType();
            int index = part.getIndex();
            appendableWrapper.append(msgString, prevIndex, index);
            if (type != Type.MSG_LIMIT) {
                String msgString2;
                AppendableWrapper appendableWrapper2;
                Map<String, Object> map2;
                int prevIndex2 = part.getLimit();
                if (type == Type.REPLACE_NUMBER) {
                    if (pluralSelectorContext.forReplaceNumber) {
                        appendableWrapper.formatAndAppend(pluralSelectorContext.formatter, pluralSelectorContext.number, pluralSelectorContext.numberString);
                    } else {
                        appendableWrapper.formatAndAppend(getStockNumberFormatter(), pluralSelectorContext.number);
                    }
                } else if (type == Type.ARG_START) {
                    boolean noArg;
                    int prevDestLength;
                    Object argId;
                    Object argId2;
                    Type type2;
                    FieldPosition fp3;
                    int prevDestLength2;
                    int i3;
                    Format format;
                    int argLimit;
                    Format format2;
                    ArgType part2;
                    FieldPosition fp4;
                    int i4;
                    prevIndex = this.msgPattern.getLimitPartIndex(i2);
                    ArgType argType = part.getArgType();
                    i2++;
                    Part part3 = this.msgPattern.getPart(i2);
                    boolean noArg2 = false;
                    Object argId3 = null;
                    int argLimit2 = prevIndex;
                    String argName = this.msgPattern.getSubstring(part3);
                    if (objArr != null) {
                        msgString2 = msgString;
                        msgString = part3.getValue();
                        if (dest.attributes != null) {
                            argId3 = Integer.valueOf(msgString);
                        }
                        if (msgString < null || msgString >= objArr.length) {
                            noArg = false;
                            noArg2 = true;
                        } else {
                            noArg = objArr[msgString];
                        }
                    } else {
                        msgString2 = msgString;
                        argId3 = argName;
                        if (map2 == null || !map2.containsKey(argName)) {
                            noArg = false;
                            noArg2 = true;
                        } else {
                            noArg = map2.get(argName);
                        }
                    }
                    Format arg = noArg;
                    noArg = noArg2;
                    i2++;
                    int prevDestLength3 = dest.length;
                    if (noArg) {
                        StringBuilder stringBuilder = new StringBuilder();
                        prevDestLength = prevDestLength3;
                        stringBuilder.append("{");
                        stringBuilder.append(argName);
                        stringBuilder.append("}");
                        appendableWrapper.append(stringBuilder.toString());
                    } else {
                        prevDestLength = prevDestLength3;
                        if (arg == null) {
                            appendableWrapper.append((CharSequence) "null");
                        } else if (pluralSelectorContext == null || pluralSelectorContext.numberArgIndex != i2 - 2) {
                            String argName2;
                            ArgType argType2;
                            Object arg2;
                            argId = argId3;
                            if (this.cachedFormatters != null) {
                                Format format3 = (Format) this.cachedFormatters.get(Integer.valueOf(i2 - 2));
                                Format formatter = format3;
                                if (format3 != null) {
                                    Format formatter2;
                                    if ((formatter instanceof ChoiceFormat) || (formatter instanceof PluralFormat) || (formatter instanceof SelectFormat)) {
                                        String subMsgString = formatter.format(arg);
                                        if (subMsgString.indexOf(123) >= 0 || (subMsgString.indexOf(39) >= 0 && !this.msgPattern.jdkAposMode())) {
                                            argName2 = argName;
                                            MessageFormat subMsgFormat = new MessageFormat(subMsgString, this.ulocale);
                                            argName = prevDestLength;
                                            formatter2 = formatter;
                                            argId2 = argId;
                                            prevDestLength = part3;
                                            argType2 = argType;
                                            argId = index;
                                            type2 = type;
                                            fp3 = fp2;
                                            subMsgFormat.format(0, null, objArr, map2, appendableWrapper, null);
                                        } else {
                                            if (dest.attributes == null) {
                                                appendableWrapper.append((CharSequence) subMsgString);
                                            } else {
                                                appendableWrapper.formatAndAppend(formatter, arg);
                                            }
                                            argName2 = argName;
                                            formatter2 = formatter;
                                            argType2 = argType;
                                            fp3 = fp2;
                                            argName = prevDestLength;
                                            argId2 = argId;
                                            prevDestLength = part3;
                                            argId = index;
                                        }
                                    } else {
                                        appendableWrapper.formatAndAppend(formatter, arg);
                                        argName2 = argName;
                                        formatter2 = formatter;
                                        argType2 = argType;
                                        fp3 = fp2;
                                        argName = prevDestLength;
                                        argId2 = argId;
                                    }
                                    prevDestLength2 = argName;
                                    i3 = i2;
                                    format = arg;
                                    appendableWrapper2 = appendableWrapper;
                                    argLimit = argLimit2;
                                    format2 = formatter2;
                                    argLimit2 = argName2;
                                    part2 = argType2;
                                    fp4 = updateMetaData(appendableWrapper2, prevDestLength2, fp3, argId2);
                                    prevIndex = argLimit;
                                    fp2 = fp4;
                                    prevIndex2 = this.msgPattern.getPart(argLimit).getLimit();
                                    i2 = prevIndex + 1;
                                    objArr = args;
                                    map2 = argsMap;
                                    appendableWrapper = appendableWrapper2;
                                    prevIndex = prevIndex2;
                                    msgString = msgString2;
                                    i = msgStart;
                                    pluralSelectorContext = pluralNumber;
                                } else {
                                    argName2 = argName;
                                    argType2 = argType;
                                    type2 = type;
                                    fp3 = fp2;
                                    prevIndex = prevDestLength;
                                    argId2 = argId;
                                    prevDestLength = part3;
                                    argId = index;
                                    format = formatter;
                                }
                            } else {
                                argName2 = argName;
                                argType2 = argType;
                                type2 = type;
                                fp3 = fp2;
                                prevIndex = prevDestLength;
                                argId2 = argId;
                                format = null;
                            }
                            part2 = argType2;
                            if (part2 == ArgType.NONE) {
                                prevDestLength2 = prevIndex;
                                i3 = i2;
                                appendableWrapper2 = appendableWrapper;
                                format2 = format;
                                argLimit = argLimit2;
                                arg2 = arg;
                            } else if (this.cachedFormatters == null || !this.cachedFormatters.containsKey(Integer.valueOf(i2 - 2))) {
                                StringBuilder stringBuilder2;
                                if (part2 != ArgType.CHOICE) {
                                    prevDestLength2 = prevIndex;
                                    i3 = i2;
                                    appendableWrapper2 = appendableWrapper;
                                    format2 = format;
                                    argLimit = argLimit2;
                                    argLimit2 = argName2;
                                    format = arg;
                                    if (part2.hasPluralStyle()) {
                                        if (format instanceof Number) {
                                            PluralSelectorProvider pluralSelectorProvider;
                                            if (part2 == ArgType.PLURAL) {
                                                if (this.pluralProvider == null) {
                                                    this.pluralProvider = new PluralSelectorProvider(this, PluralType.CARDINAL);
                                                }
                                                pluralSelectorProvider = this.pluralProvider;
                                            } else {
                                                if (this.ordinalProvider == null) {
                                                    this.ordinalProvider = new PluralSelectorProvider(this, PluralType.ORDINAL);
                                                }
                                                pluralSelectorProvider = this.ordinalProvider;
                                            }
                                            PluralSelectorProvider selector = pluralSelectorProvider;
                                            Number number = (Number) format;
                                            PluralSelectorContext pluralSelectorContext2 = new PluralSelectorContext(i3, argLimit2, number, this.msgPattern.getPluralOffset(i3));
                                            formatComplexSubMessage(PluralFormat.findSubMessage(this.msgPattern, i3, selector, pluralSelectorContext2, number.doubleValue()), pluralSelectorContext2, args, argsMap, appendableWrapper2);
                                        } else {
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("'");
                                            stringBuilder2.append(format);
                                            stringBuilder2.append("' is not a Number");
                                            throw new IllegalArgumentException(stringBuilder2.toString());
                                        }
                                    } else if (part2 == ArgType.SELECT) {
                                        formatComplexSubMessage(SelectFormat.findSubMessage(this.msgPattern, i3, format.toString()), null, args, argsMap, appendableWrapper2);
                                    } else {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("unexpected argType ");
                                        stringBuilder2.append(part2);
                                        throw new IllegalStateException(stringBuilder2.toString());
                                    }
                                } else if (arg instanceof Number) {
                                    prevDestLength2 = prevIndex;
                                    argLimit = argLimit2;
                                    argLimit2 = argName2;
                                    format = arg;
                                    appendableWrapper2 = appendableWrapper;
                                    formatComplexSubMessage(findChoiceSubMessage(this.msgPattern, i2, ((Number) arg).doubleValue()), null, objArr, argsMap, appendableWrapper2);
                                } else {
                                    i3 = i2;
                                    appendableWrapper2 = appendableWrapper;
                                    format2 = format;
                                    argLimit = argLimit2;
                                    argLimit2 = argName2;
                                    format = arg;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("'");
                                    stringBuilder2.append(format);
                                    stringBuilder2.append("' is not a Number");
                                    throw new IllegalArgumentException(stringBuilder2.toString());
                                }
                                fp4 = updateMetaData(appendableWrapper2, prevDestLength2, fp3, argId2);
                                prevIndex = argLimit;
                                fp2 = fp4;
                                prevIndex2 = this.msgPattern.getPart(argLimit).getLimit();
                                i2 = prevIndex + 1;
                                objArr = args;
                                map2 = argsMap;
                                appendableWrapper = appendableWrapper2;
                                prevIndex = prevIndex2;
                                msgString = msgString2;
                                i = msgStart;
                                pluralSelectorContext = pluralNumber;
                            } else {
                                prevDestLength2 = prevIndex;
                                i3 = i2;
                                appendableWrapper2 = appendableWrapper;
                                format2 = format;
                                argLimit = argLimit2;
                                argLimit2 = argName2;
                                arg2 = arg;
                            }
                            if (arg2 instanceof Number) {
                                appendableWrapper2.formatAndAppend(getStockNumberFormatter(), arg2);
                            } else if (arg2 instanceof Date) {
                                appendableWrapper2.formatAndAppend(getStockDateFormatter(), arg2);
                            } else {
                                appendableWrapper2.append(arg2.toString());
                            }
                            fp4 = updateMetaData(appendableWrapper2, prevDestLength2, fp3, argId2);
                            prevIndex = argLimit;
                            fp2 = fp4;
                            prevIndex2 = this.msgPattern.getPart(argLimit).getLimit();
                            i2 = prevIndex + 1;
                            objArr = args;
                            map2 = argsMap;
                            appendableWrapper = appendableWrapper2;
                            prevIndex = prevIndex2;
                            msgString = msgString2;
                            i = msgStart;
                            pluralSelectorContext = pluralNumber;
                        } else {
                            argId = argId3;
                            if (pluralSelectorContext.offset == 0.0d) {
                                appendableWrapper.formatAndAppend(pluralSelectorContext.formatter, pluralSelectorContext.number, pluralSelectorContext.numberString);
                            } else {
                                appendableWrapper.formatAndAppend(pluralSelectorContext.formatter, arg);
                            }
                            i3 = i2;
                            format = arg;
                            type2 = type;
                            fp3 = fp2;
                            argLimit = argLimit2;
                            format2 = null;
                            prevDestLength2 = prevDestLength;
                            argId2 = argId;
                            argLimit2 = argName;
                            appendableWrapper2 = appendableWrapper;
                            prevDestLength = part3;
                            part2 = argType;
                            i4 = index;
                            fp4 = updateMetaData(appendableWrapper2, prevDestLength2, fp3, argId2);
                            prevIndex = argLimit;
                            fp2 = fp4;
                            prevIndex2 = this.msgPattern.getPart(argLimit).getLimit();
                            i2 = prevIndex + 1;
                            objArr = args;
                            map2 = argsMap;
                            appendableWrapper = appendableWrapper2;
                            prevIndex = prevIndex2;
                            msgString = msgString2;
                            i = msgStart;
                            pluralSelectorContext = pluralNumber;
                        }
                    }
                    argId = argId3;
                    i3 = i2;
                    format = arg;
                    type2 = type;
                    fp3 = fp2;
                    argLimit = argLimit2;
                    format2 = null;
                    prevDestLength2 = prevDestLength;
                    argId2 = argId;
                    argLimit2 = argName;
                    appendableWrapper2 = appendableWrapper;
                    prevDestLength = part3;
                    part2 = argType;
                    i4 = index;
                    fp4 = updateMetaData(appendableWrapper2, prevDestLength2, fp3, argId2);
                    prevIndex = argLimit;
                    fp2 = fp4;
                    prevIndex2 = this.msgPattern.getPart(argLimit).getLimit();
                    i2 = prevIndex + 1;
                    objArr = args;
                    map2 = argsMap;
                    appendableWrapper = appendableWrapper2;
                    prevIndex = prevIndex2;
                    msgString = msgString2;
                    i = msgStart;
                    pluralSelectorContext = pluralNumber;
                }
                prevIndex = i2;
                msgString2 = msgString;
                appendableWrapper2 = appendableWrapper;
                i2 = prevIndex + 1;
                objArr = args;
                map2 = argsMap;
                appendableWrapper = appendableWrapper2;
                prevIndex = prevIndex2;
                msgString = msgString2;
                i = msgStart;
                pluralSelectorContext = pluralNumber;
            } else {
                return;
            }
        }
    }

    private void formatComplexSubMessage(int msgStart, PluralSelectorContext pluralNumber, Object[] args, Map<String, Object> argsMap, AppendableWrapper dest) {
        PluralSelectorContext pluralSelectorContext = pluralNumber;
        if (this.msgPattern.jdkAposMode()) {
            Part part;
            int index;
            AppendableWrapper appendableWrapper;
            String subMsgString;
            String msgString = this.msgPattern.getPatternString();
            int i = msgStart;
            int prevIndex = this.msgPattern.getPart(i).getLimit();
            StringBuilder sb = null;
            int i2 = i;
            while (true) {
                i2++;
                part = this.msgPattern.getPart(i2);
                Type type = part.getType();
                index = part.getIndex();
                if (type == Type.MSG_LIMIT) {
                    break;
                }
                appendableWrapper = dest;
                if (type == Type.REPLACE_NUMBER || type == Type.SKIP_SYNTAX) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(msgString, prevIndex, index);
                    if (type == Type.REPLACE_NUMBER) {
                        if (pluralSelectorContext.forReplaceNumber) {
                            sb.append(pluralSelectorContext.numberString);
                        } else {
                            sb.append(getStockNumberFormatter().format(pluralSelectorContext.number));
                        }
                    }
                    prevIndex = part.getLimit();
                } else if (type == Type.ARG_START) {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    sb.append(msgString, prevIndex, index);
                    prevIndex = index;
                    i2 = this.msgPattern.getLimitPartIndex(i2);
                    index = this.msgPattern.getPart(i2).getLimit();
                    MessagePattern.appendReducedApostrophes(msgString, prevIndex, index, sb);
                    prevIndex = index;
                }
            }
            if (sb == null) {
                subMsgString = msgString.substring(prevIndex, index);
            } else {
                sb.append(msgString, prevIndex, index);
                subMsgString = sb.toString();
            }
            CharSequence i3 = subMsgString;
            if (i3.indexOf(123) >= 0) {
                part = new MessageFormat("", this.ulocale);
                part.applyPattern(i3, ApostropheMode.DOUBLE_REQUIRED);
                part.format(0, null, args, argsMap, dest, null);
                appendableWrapper = dest;
            } else {
                dest.append(i3);
            }
            return;
        }
        format(msgStart, pluralSelectorContext, args, argsMap, dest, null);
    }

    private String getLiteralStringUntilNextArgument(int from) {
        StringBuilder b = new StringBuilder();
        String msgString = this.msgPattern.getPatternString();
        int prevIndex = this.msgPattern.getPart(from).getLimit();
        int i = from + 1;
        while (true) {
            Part part = this.msgPattern.getPart(i);
            Type type = part.getType();
            b.append(msgString, prevIndex, part.getIndex());
            if (type != Type.ARG_START && type != Type.MSG_LIMIT) {
                prevIndex = part.getLimit();
                i++;
            }
        }
        return b.toString();
    }

    private FieldPosition updateMetaData(AppendableWrapper dest, int prevLength, FieldPosition fp, Object argId) {
        if (dest.attributes != null && prevLength < dest.length) {
            dest.attributes.add(new AttributeAndPosition(argId, prevLength, dest.length));
        }
        if (fp == null || !Field.ARGUMENT.equals(fp.getFieldAttribute())) {
            return fp;
        }
        fp.setBeginIndex(prevLength);
        fp.setEndIndex(dest.length);
        return null;
    }

    private static int findChoiceSubMessage(MessagePattern pattern, int partIndex, double number) {
        int msgStart;
        int count = pattern.countParts();
        partIndex += 2;
        while (true) {
            msgStart = partIndex;
            Part part = pattern.getLimitPartIndex(partIndex) + 1;
            if (part >= count) {
                break;
            }
            int selectorIndex = part + 1;
            part = pattern.getPart(part);
            if (part.getType() == Type.ARG_LIMIT) {
                part = selectorIndex;
                break;
            }
            double boundary = pattern.getNumericValue(part);
            int partIndex2 = selectorIndex + 1;
            if (pattern.getPatternString().charAt(pattern.getPatternIndex(selectorIndex)) == '<') {
                if (number <= boundary) {
                    break;
                }
                partIndex = partIndex2;
            } else if (number < boundary) {
                break;
            } else {
                partIndex = partIndex2;
            }
        }
        return msgStart;
    }

    private static double parseChoiceArgument(MessagePattern pattern, int partIndex, String source, ParsePosition pos) {
        int start = pos.getIndex();
        int furthest = start;
        double bestNumber = Double.NaN;
        while (pattern.getPartType(partIndex) != Type.ARG_LIMIT) {
            double tempNumber = pattern.getNumericValue(pattern.getPart(partIndex));
            partIndex += 2;
            int msgLimit = pattern.getLimitPartIndex(partIndex);
            int len = matchStringUntilLimitPart(pattern, partIndex, msgLimit, source, start);
            if (len >= 0) {
                int newIndex = start + len;
                if (newIndex > furthest) {
                    furthest = newIndex;
                    bestNumber = tempNumber;
                    if (furthest == source.length()) {
                        break;
                    }
                } else {
                    continue;
                }
            }
            partIndex = msgLimit + 1;
        }
        if (furthest == start) {
            pos.setErrorIndex(start);
        } else {
            pos.setIndex(furthest);
        }
        return bestNumber;
    }

    private static int matchStringUntilLimitPart(MessagePattern pattern, int partIndex, int limitPartIndex, String source, int sourceOffset) {
        int matchingSourceLength = 0;
        String msgString = pattern.getPatternString();
        int prevIndex = pattern.getPart(partIndex).getLimit();
        while (true) {
            partIndex++;
            Part part = pattern.getPart(partIndex);
            if (partIndex == limitPartIndex || part.getType() == Type.SKIP_SYNTAX) {
                int length = part.getIndex() - prevIndex;
                if (length != 0 && !source.regionMatches(sourceOffset, msgString, prevIndex, length)) {
                    return -1;
                }
                matchingSourceLength += length;
                if (partIndex == limitPartIndex) {
                    return matchingSourceLength;
                }
                prevIndex = part.getLimit();
            }
        }
    }

    private int findOtherSubMessage(int partIndex) {
        int count = this.msgPattern.countParts();
        if (this.msgPattern.getPart(partIndex).getType().hasNumericValue()) {
            partIndex++;
        }
        do {
            int partIndex2 = partIndex + 1;
            Part part = this.msgPattern.getPart(partIndex);
            if (part.getType() == Type.ARG_LIMIT) {
                Type type = partIndex2;
                break;
            } else if (this.msgPattern.partSubstringMatches(part, PluralRules.KEYWORD_OTHER)) {
                return partIndex2;
            } else {
                if (this.msgPattern.getPartType(partIndex2).hasNumericValue()) {
                    partIndex2++;
                }
                partIndex = this.msgPattern.getLimitPartIndex(partIndex2) + 1;
            }
        } while (partIndex < count);
        return 0;
    }

    private int findFirstPluralNumberArg(int msgStart, String argName) {
        int i = msgStart + 1;
        while (true) {
            Part part = this.msgPattern.getPart(i);
            Type type = part.getType();
            if (type == Type.MSG_LIMIT) {
                return 0;
            }
            if (type == Type.REPLACE_NUMBER) {
                return -1;
            }
            if (type == Type.ARG_START) {
                ArgType argType = part.getArgType();
                if (argName.length() != 0 && (argType == ArgType.NONE || argType == ArgType.SIMPLE)) {
                    if (this.msgPattern.partSubstringMatches(this.msgPattern.getPart(i + 1), argName)) {
                        return i;
                    }
                }
                i = this.msgPattern.getLimitPartIndex(i);
            }
            i++;
        }
    }

    private void format(Object arguments, AppendableWrapper result, FieldPosition fp) {
        if (arguments == null || (arguments instanceof Map)) {
            format(null, (Map) arguments, result, fp);
        } else {
            format((Object[]) arguments, null, result, fp);
        }
    }

    private void format(Object[] arguments, Map<String, Object> argsMap, AppendableWrapper dest, FieldPosition fp) {
        if (arguments == null || !this.msgPattern.hasNamedArguments()) {
            format(0, null, arguments, argsMap, dest, fp);
            return;
        }
        throw new IllegalArgumentException("This method is not available in MessageFormat objects that use alphanumeric argument names.");
    }

    private void resetPattern() {
        if (this.msgPattern != null) {
            this.msgPattern.clear();
        }
        if (this.cachedFormatters != null) {
            this.cachedFormatters.clear();
        }
        this.customFormatArgStarts = null;
    }

    private Format createAppropriateFormat(String type, String style) {
        Format rbnf;
        String ruleset;
        switch (findKeyword(type, typeList)) {
            case 0:
                switch (findKeyword(style, modifierList)) {
                    case 0:
                        return NumberFormat.getInstance(this.ulocale);
                    case 1:
                        return NumberFormat.getCurrencyInstance(this.ulocale);
                    case 2:
                        return NumberFormat.getPercentInstance(this.ulocale);
                    case 3:
                        return NumberFormat.getIntegerInstance(this.ulocale);
                    default:
                        return new DecimalFormat(style, new DecimalFormatSymbols(this.ulocale));
                }
            case 1:
                switch (findKeyword(style, dateModifierList)) {
                    case 0:
                        return DateFormat.getDateInstance(2, this.ulocale);
                    case 1:
                        return DateFormat.getDateInstance(3, this.ulocale);
                    case 2:
                        return DateFormat.getDateInstance(2, this.ulocale);
                    case 3:
                        return DateFormat.getDateInstance(1, this.ulocale);
                    case 4:
                        return DateFormat.getDateInstance(0, this.ulocale);
                    default:
                        return new SimpleDateFormat(style, this.ulocale);
                }
            case 2:
                switch (findKeyword(style, dateModifierList)) {
                    case 0:
                        return DateFormat.getTimeInstance(2, this.ulocale);
                    case 1:
                        return DateFormat.getTimeInstance(3, this.ulocale);
                    case 2:
                        return DateFormat.getTimeInstance(2, this.ulocale);
                    case 3:
                        return DateFormat.getTimeInstance(1, this.ulocale);
                    case 4:
                        return DateFormat.getTimeInstance(0, this.ulocale);
                    default:
                        return new SimpleDateFormat(style, this.ulocale);
                }
            case 3:
                rbnf = new RuleBasedNumberFormat(this.ulocale, 1);
                ruleset = style.trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    } catch (Exception e) {
                    }
                }
                return rbnf;
            case 4:
                rbnf = new RuleBasedNumberFormat(this.ulocale, 2);
                ruleset = style.trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    } catch (Exception e2) {
                    }
                }
                return rbnf;
            case 5:
                rbnf = new RuleBasedNumberFormat(this.ulocale, 3);
                ruleset = style.trim();
                if (ruleset.length() != 0) {
                    try {
                        rbnf.setDefaultRuleSet(ruleset);
                    } catch (Exception e3) {
                    }
                }
                return rbnf;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown format type \"");
                stringBuilder.append(type);
                stringBuilder.append("\"");
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static final int findKeyword(String s, String[] list) {
        s = PatternProps.trimWhiteSpace(s).toLowerCase(rootLocale);
        for (int i = 0; i < list.length; i++) {
            if (s.equals(list[i])) {
                return i;
            }
        }
        return -1;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.ulocale.toLanguageTag());
        if (this.msgPattern == null) {
            this.msgPattern = new MessagePattern();
        }
        out.writeObject(this.msgPattern.getApostropheMode());
        out.writeObject(this.msgPattern.getPatternString());
        if (this.customFormatArgStarts != null && !this.customFormatArgStarts.isEmpty()) {
            out.writeInt(this.customFormatArgStarts.size());
            int formatIndex = 0;
            int partIndex = 0;
            while (true) {
                int nextTopLevelArgStart = nextTopLevelArgStart(partIndex);
                partIndex = nextTopLevelArgStart;
                if (nextTopLevelArgStart < 0) {
                    break;
                }
                if (this.customFormatArgStarts.contains(Integer.valueOf(partIndex))) {
                    out.writeInt(formatIndex);
                    out.writeObject(this.cachedFormatters.get(Integer.valueOf(partIndex)));
                }
                formatIndex++;
            }
        } else {
            out.writeInt(0);
        }
        out.writeInt(0);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int numFormatters;
        in.defaultReadObject();
        this.ulocale = ULocale.forLanguageTag((String) in.readObject());
        ApostropheMode aposMode = (ApostropheMode) in.readObject();
        if (this.msgPattern == null || aposMode != this.msgPattern.getApostropheMode()) {
            this.msgPattern = new MessagePattern(aposMode);
        }
        String msg = (String) in.readObject();
        if (msg != null) {
            applyPattern(msg);
        }
        for (numFormatters = in.readInt(); numFormatters > 0; numFormatters--) {
            setFormat(in.readInt(), (Format) in.readObject());
        }
        for (numFormatters = in.readInt(); numFormatters > 0; numFormatters--) {
            in.readInt();
            in.readObject();
        }
    }

    private void cacheExplicitFormats() {
        if (this.cachedFormatters != null) {
            this.cachedFormatters.clear();
        }
        this.customFormatArgStarts = null;
        int limit = this.msgPattern.countParts() - 2;
        int i = 1;
        while (i < limit) {
            Part part = this.msgPattern.getPart(i);
            if (part.getType() == Type.ARG_START && part.getArgType() == ArgType.SIMPLE) {
                int index = i;
                i += 2;
                int i2 = i + 1;
                String explicitType = this.msgPattern.getSubstring(this.msgPattern.getPart(i));
                String style = "";
                Part part2 = this.msgPattern.getPart(i2);
                part = part2;
                if (part2.getType() == Type.ARG_STYLE) {
                    style = this.msgPattern.getSubstring(part);
                    i2++;
                }
                setArgStartFormat(index, createAppropriateFormat(explicitType, style));
                i = i2;
            }
            i++;
        }
    }

    private void setArgStartFormat(int argStart, Format formatter) {
        if (this.cachedFormatters == null) {
            this.cachedFormatters = new HashMap();
        }
        this.cachedFormatters.put(Integer.valueOf(argStart), formatter);
    }

    private void setCustomArgStartFormat(int argStart, Format formatter) {
        setArgStartFormat(argStart, formatter);
        if (this.customFormatArgStarts == null) {
            this.customFormatArgStarts = new HashSet();
        }
        this.customFormatArgStarts.add(Integer.valueOf(argStart));
    }

    public static String autoQuoteApostrophe(String pattern) {
        StringBuilder buf = new StringBuilder(pattern.length() * 2);
        int state = 0;
        int braceCount = 0;
        int j = pattern.length();
        for (int i = 0; i < j; i++) {
            char c = pattern.charAt(i);
            switch (state) {
                case 0:
                    if (c != '\'') {
                        if (c == CURLY_BRACE_LEFT) {
                            state = 3;
                            braceCount++;
                            break;
                        }
                        break;
                    }
                    state = 1;
                    break;
                case 1:
                    if (c != '\'') {
                        if (c != CURLY_BRACE_LEFT && c != CURLY_BRACE_RIGHT) {
                            buf.append('\'');
                            state = 0;
                            break;
                        }
                        state = 2;
                        break;
                    }
                    state = 0;
                    break;
                case 2:
                    if (c == '\'') {
                        state = 0;
                        break;
                    }
                    break;
                case 3:
                    if (c != CURLY_BRACE_LEFT) {
                        if (c == CURLY_BRACE_RIGHT) {
                            braceCount--;
                            if (braceCount != 0) {
                                break;
                            }
                            state = 0;
                            break;
                        }
                        break;
                    }
                    braceCount++;
                    break;
                default:
                    break;
            }
            buf.append(c);
        }
        if (state == 1 || state == 2) {
            buf.append('\'');
        }
        return new String(buf);
    }
}
