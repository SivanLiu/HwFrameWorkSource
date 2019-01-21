package java.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Category;
import sun.security.x509.InvalidityDateExtension;

public class MessageFormat extends Format {
    private static final int[] DATE_TIME_MODIFIERS = new int[]{2, 3, 2, 1, 0};
    private static final String[] DATE_TIME_MODIFIER_KEYWORDS = new String[]{"", "short", "medium", "long", "full"};
    private static final int INITIAL_FORMATS = 10;
    private static final int MODIFIER_CURRENCY = 1;
    private static final int MODIFIER_DEFAULT = 0;
    private static final int MODIFIER_FULL = 4;
    private static final int MODIFIER_INTEGER = 3;
    private static final int MODIFIER_LONG = 3;
    private static final int MODIFIER_MEDIUM = 2;
    private static final int MODIFIER_PERCENT = 2;
    private static final int MODIFIER_SHORT = 1;
    private static final String[] NUMBER_MODIFIER_KEYWORDS = new String[]{"", "currency", "percent", "integer"};
    private static final int SEG_INDEX = 1;
    private static final int SEG_MODIFIER = 3;
    private static final int SEG_RAW = 0;
    private static final int SEG_TYPE = 2;
    private static final int TYPE_CHOICE = 4;
    private static final int TYPE_DATE = 2;
    private static final String[] TYPE_KEYWORDS = new String[]{"", "number", InvalidityDateExtension.DATE, "time", "choice"};
    private static final int TYPE_NULL = 0;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_TIME = 3;
    private static final long serialVersionUID = 6479157306784022952L;
    private int[] argumentNumbers;
    private Format[] formats;
    private Locale locale;
    private int maxOffset;
    private int[] offsets;
    private String pattern;

    public static class Field extends java.text.Format.Field {
        public static final Field ARGUMENT = new Field("message argument field");
        private static final long serialVersionUID = 7899943957617360810L;

        protected Field(String name) {
            super(name);
        }

        protected Object readResolve() throws InvalidObjectException {
            if (getClass() == Field.class) {
                return ARGUMENT;
            }
            throw new InvalidObjectException("subclass didn't correctly implement readResolve");
        }
    }

    public MessageFormat(String pattern) {
        this.pattern = "";
        this.formats = new Format[10];
        this.offsets = new int[10];
        this.argumentNumbers = new int[10];
        this.maxOffset = -1;
        this.locale = Locale.getDefault(Category.FORMAT);
        applyPattern(pattern);
    }

    public MessageFormat(String pattern, Locale locale) {
        this.pattern = "";
        this.formats = new Format[10];
        this.offsets = new int[10];
        this.argumentNumbers = new int[10];
        this.maxOffset = -1;
        this.locale = locale;
        applyPattern(pattern);
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void applyPattern(String pattern) {
        StringBuilder[] segments = new StringBuilder[4];
        segments[0] = new StringBuilder();
        boolean inQuote = false;
        int braceStack = 0;
        this.maxOffset = -1;
        int formatNumber = 0;
        int part = 0;
        int i = 0;
        while (i < pattern.length()) {
            char ch = pattern.charAt(i);
            if (part == 0) {
                if (ch == '\'') {
                    if (i + 1 >= pattern.length() || pattern.charAt(i + 1) != '\'') {
                        inQuote = !inQuote;
                    } else {
                        segments[part].append(ch);
                        i++;
                    }
                } else if (ch != '{' || inQuote) {
                    segments[part].append(ch);
                } else {
                    part = 1;
                    if (segments[1] == null) {
                        segments[1] = new StringBuilder();
                    }
                }
            } else if (inQuote) {
                segments[part].append(ch);
                if (ch == '\'') {
                    inQuote = false;
                }
            } else if (ch != ' ') {
                if (ch == '\'') {
                    inQuote = true;
                } else if (ch != ',') {
                    if (ch == '{') {
                        braceStack++;
                        segments[part].append(ch);
                    } else if (ch == '}') {
                        if (braceStack == 0) {
                            part = 0;
                            makeFormat(i, formatNumber, segments);
                            formatNumber++;
                            segments[1] = null;
                            segments[2] = null;
                            segments[3] = null;
                        } else {
                            braceStack--;
                            segments[part].append(ch);
                        }
                    }
                } else if (part < 3) {
                    part++;
                    if (segments[part] == null) {
                        segments[part] = new StringBuilder();
                    }
                } else {
                    segments[part].append(ch);
                }
                segments[part].append(ch);
            } else if (part != 2 || segments[2].length() > 0) {
                segments[part].append(ch);
            }
            i++;
        }
        if (braceStack != 0 || part == 0) {
            this.pattern = segments[0].toString();
        } else {
            this.maxOffset = -1;
            throw new IllegalArgumentException("Unmatched braces in the pattern.");
        }
    }

    public String toPattern() {
        StringBuilder result = new StringBuilder();
        int lastOffset = 0;
        for (int i = 0; i <= this.maxOffset; i++) {
            copyAndFixQuotes(this.pattern, lastOffset, this.offsets[i], result);
            lastOffset = this.offsets[i];
            result.append('{');
            result.append(this.argumentNumbers[i]);
            Format fmt = this.formats[i];
            if (fmt != null) {
                if (fmt instanceof NumberFormat) {
                    if (fmt.equals(NumberFormat.getInstance(this.locale))) {
                        result.append(",number");
                    } else if (fmt.equals(NumberFormat.getCurrencyInstance(this.locale))) {
                        result.append(",number,currency");
                    } else if (fmt.equals(NumberFormat.getPercentInstance(this.locale))) {
                        result.append(",number,percent");
                    } else if (fmt.equals(NumberFormat.getIntegerInstance(this.locale))) {
                        result.append(",number,integer");
                    } else if (fmt instanceof DecimalFormat) {
                        result.append(",number,");
                        result.append(((DecimalFormat) fmt).toPattern());
                    } else if (fmt instanceof ChoiceFormat) {
                        result.append(",choice,");
                        result.append(((ChoiceFormat) fmt).toPattern());
                    }
                } else if (fmt instanceof DateFormat) {
                    int index = 0;
                    while (index < DATE_TIME_MODIFIERS.length) {
                        if (fmt.equals(DateFormat.getDateInstance(DATE_TIME_MODIFIERS[index], this.locale))) {
                            result.append(",date");
                            break;
                        } else if (fmt.equals(DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[index], this.locale))) {
                            result.append(",time");
                            break;
                        } else {
                            index++;
                        }
                    }
                    if (index >= DATE_TIME_MODIFIERS.length) {
                        if (fmt instanceof SimpleDateFormat) {
                            result.append(",date,");
                            result.append(((SimpleDateFormat) fmt).toPattern());
                        }
                    } else if (index != 0) {
                        result.append(',');
                        result.append(DATE_TIME_MODIFIER_KEYWORDS[index]);
                    }
                }
            }
            result.append('}');
        }
        copyAndFixQuotes(this.pattern, lastOffset, this.pattern.length(), result);
        return result.toString();
    }

    public void setFormatsByArgumentIndex(Format[] newFormats) {
        for (int i = 0; i <= this.maxOffset; i++) {
            int j = this.argumentNumbers[i];
            if (j < newFormats.length) {
                this.formats[i] = newFormats[j];
            }
        }
    }

    public void setFormats(Format[] newFormats) {
        int runsToCopy = newFormats.length;
        if (runsToCopy > this.maxOffset + 1) {
            runsToCopy = this.maxOffset + 1;
        }
        for (int i = 0; i < runsToCopy; i++) {
            this.formats[i] = newFormats[i];
        }
    }

    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        for (int j = 0; j <= this.maxOffset; j++) {
            if (this.argumentNumbers[j] == argumentIndex) {
                this.formats[j] = newFormat;
            }
        }
    }

    public void setFormat(int formatElementIndex, Format newFormat) {
        if (formatElementIndex <= this.maxOffset) {
            this.formats[formatElementIndex] = newFormat;
            return;
        }
        throw new ArrayIndexOutOfBoundsException(this.maxOffset, formatElementIndex);
    }

    public Format[] getFormatsByArgumentIndex() {
        int i = 0;
        int maximumArgumentNumber = -1;
        for (int i2 = 0; i2 <= this.maxOffset; i2++) {
            if (this.argumentNumbers[i2] > maximumArgumentNumber) {
                maximumArgumentNumber = this.argumentNumbers[i2];
            }
        }
        Format[] resultArray = new Format[(maximumArgumentNumber + 1)];
        while (i <= this.maxOffset) {
            resultArray[this.argumentNumbers[i]] = this.formats[i];
            i++;
        }
        return resultArray;
    }

    public Format[] getFormats() {
        Object resultArray = new Format[(this.maxOffset + 1)];
        System.arraycopy(this.formats, 0, resultArray, 0, this.maxOffset + 1);
        return resultArray;
    }

    public final StringBuffer format(Object[] arguments, StringBuffer result, FieldPosition pos) {
        return subformat(arguments, result, pos, null);
    }

    public static String format(String pattern, Object... arguments) {
        return new MessageFormat(pattern).format(arguments);
    }

    public final StringBuffer format(Object arguments, StringBuffer result, FieldPosition pos) {
        return subformat((Object[]) arguments, result, pos, null);
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object arguments) {
        StringBuffer result = new StringBuffer();
        ArrayList<AttributedCharacterIterator> iterators = new ArrayList();
        if (arguments != null) {
            subformat((Object[]) arguments, result, null, iterators);
            if (iterators.size() == 0) {
                return createAttributedCharacterIterator("");
            }
            return createAttributedCharacterIterator((AttributedCharacterIterator[]) iterators.toArray(new AttributedCharacterIterator[iterators.size()]));
        }
        throw new NullPointerException("formatToCharacterIterator must be passed non-null object");
    }

    public Object[] parse(String source, ParsePosition pos) {
        int i = 0;
        if (source == null) {
            return new Object[0];
        }
        int maximumArgumentNumber = -1;
        for (int i2 = 0; i2 <= this.maxOffset; i2++) {
            if (this.argumentNumbers[i2] > maximumArgumentNumber) {
                maximumArgumentNumber = this.argumentNumbers[i2];
            }
        }
        Object[] resultArray = new Object[(maximumArgumentNumber + 1)];
        int patternOffset = 0;
        int sourceOffset = pos.index;
        ParsePosition tempStatus = new ParsePosition(0);
        while (i <= this.maxOffset) {
            int len = this.offsets[i] - patternOffset;
            if (len == 0 || this.pattern.regionMatches(patternOffset, source, sourceOffset, len)) {
                sourceOffset += len;
                patternOffset += len;
                if (this.formats[i] == null) {
                    int next;
                    int tempLength = i != this.maxOffset ? this.offsets[i + 1] : this.pattern.length();
                    if (patternOffset >= tempLength) {
                        next = source.length();
                    } else {
                        next = source.indexOf(this.pattern.substring(patternOffset, tempLength), sourceOffset);
                    }
                    if (next < 0) {
                        pos.errorIndex = sourceOffset;
                        return null;
                    }
                    String strValue = source.substring(sourceOffset, next);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("{");
                    stringBuilder.append(this.argumentNumbers[i]);
                    stringBuilder.append("}");
                    if (!strValue.equals(stringBuilder.toString())) {
                        resultArray[this.argumentNumbers[i]] = source.substring(sourceOffset, next);
                    }
                    sourceOffset = next;
                } else {
                    tempStatus.index = sourceOffset;
                    resultArray[this.argumentNumbers[i]] = this.formats[i].parseObject(source, tempStatus);
                    if (tempStatus.index == sourceOffset) {
                        pos.errorIndex = sourceOffset;
                        return null;
                    }
                    sourceOffset = tempStatus.index;
                }
                i++;
            } else {
                pos.errorIndex = sourceOffset;
                return null;
            }
        }
        i = this.pattern.length() - patternOffset;
        if (i == 0 || this.pattern.regionMatches(patternOffset, source, sourceOffset, i)) {
            pos.index = sourceOffset + i;
            return resultArray;
        }
        pos.errorIndex = sourceOffset;
        return null;
    }

    public Object[] parse(String source) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Object[] result = parse(source, pos);
        if (pos.index != 0) {
            return result;
        }
        throw new ParseException("MessageFormat parse error!", pos.errorIndex);
    }

    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    public Object clone() {
        MessageFormat other = (MessageFormat) super.clone();
        other.formats = (Format[]) this.formats.clone();
        for (int i = 0; i < this.formats.length; i++) {
            if (this.formats[i] != null) {
                other.formats[i] = (Format) this.formats[i].clone();
            }
        }
        other.offsets = (int[]) this.offsets.clone();
        other.argumentNumbers = (int[]) this.argumentNumbers.clone();
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
        if (!(this.maxOffset == other.maxOffset && this.pattern.equals(other.pattern) && (((this.locale != null && this.locale.equals(other.locale)) || (this.locale == null && other.locale == null)) && Arrays.equals(this.offsets, other.offsets) && Arrays.equals(this.argumentNumbers, other.argumentNumbers) && Arrays.equals(this.formats, other.formats)))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return this.pattern.hashCode();
    }

    private StringBuffer subformat(Object[] arguments, StringBuffer result, FieldPosition fp, List<AttributedCharacterIterator> characterIterators) {
        int lastOffset = 0;
        int last = result.length();
        for (int i = 0; i <= this.maxOffset; i++) {
            result.append(this.pattern.substring(lastOffset, this.offsets[i]));
            lastOffset = this.offsets[i];
            int argumentNumber = this.argumentNumbers[i];
            if (arguments == null || argumentNumber >= arguments.length) {
                result.append('{');
                result.append(argumentNumber);
                result.append('}');
            } else {
                Object obj = arguments[argumentNumber];
                String arg = null;
                Format subFormatter = null;
                if (obj == null) {
                    arg = "null";
                } else if (this.formats[i] != null) {
                    subFormatter = this.formats[i];
                    if (subFormatter instanceof ChoiceFormat) {
                        arg = this.formats[i].format(obj);
                        if (arg.indexOf(123) >= 0) {
                            subFormatter = new MessageFormat(arg, this.locale);
                            obj = arguments;
                            arg = null;
                        }
                    }
                } else if (obj instanceof Number) {
                    subFormatter = NumberFormat.getInstance(this.locale);
                } else if (obj instanceof Date) {
                    subFormatter = DateFormat.getDateTimeInstance(3, 3, this.locale);
                } else if (obj instanceof String) {
                    arg = (String) obj;
                } else {
                    arg = obj.toString();
                    if (arg == null) {
                        arg = "null";
                    }
                }
                if (characterIterators != null) {
                    if (last != result.length()) {
                        characterIterators.add(createAttributedCharacterIterator(result.substring(last)));
                        last = result.length();
                    }
                    if (subFormatter != null) {
                        AttributedCharacterIterator subIterator = subFormatter.formatToCharacterIterator(obj);
                        append(result, subIterator);
                        if (last != result.length()) {
                            characterIterators.add(createAttributedCharacterIterator(subIterator, (Attribute) Field.ARGUMENT, (Object) Integer.valueOf(argumentNumber)));
                            last = result.length();
                        }
                        arg = null;
                    }
                    if (arg != null && arg.length() > 0) {
                        result.append(arg);
                        characterIterators.add(createAttributedCharacterIterator(arg, (Attribute) Field.ARGUMENT, (Object) Integer.valueOf(argumentNumber)));
                        last = result.length();
                    }
                } else {
                    if (subFormatter != null) {
                        arg = subFormatter.format(obj);
                    }
                    last = result.length();
                    result.append(arg);
                    if (i == 0 && fp != null && Field.ARGUMENT.equals(fp.getFieldAttribute())) {
                        fp.setBeginIndex(last);
                        fp.setEndIndex(result.length());
                    }
                    last = result.length();
                }
            }
        }
        result.append(this.pattern.substring(lastOffset, this.pattern.length()));
        if (!(characterIterators == null || last == result.length())) {
            characterIterators.add(createAttributedCharacterIterator(result.substring(last)));
        }
        return result;
    }

    private void append(StringBuffer result, CharacterIterator iterator) {
        if (iterator.first() != 65535) {
            result.append(iterator.first());
            while (true) {
                char next = iterator.next();
                char aChar = next;
                if (next != 65535) {
                    result.append(aChar);
                } else {
                    return;
                }
            }
        }
    }

    private void makeFormat(int position, int offsetNumber, StringBuilder[] textSegments) {
        int i;
        String[] segments = new String[textSegments.length];
        for (i = 0; i < textSegments.length; i++) {
            StringBuilder oneseg = textSegments[i];
            segments[i] = oneseg != null ? oneseg.toString() : "";
        }
        try {
            int argumentNumber = Integer.parseInt(segments[1]);
            if (argumentNumber >= 0) {
                int newLength;
                if (offsetNumber >= this.formats.length) {
                    newLength = this.formats.length * 2;
                    Object newFormats = new Format[newLength];
                    Object newOffsets = new int[newLength];
                    Object newArgumentNumbers = new int[newLength];
                    System.arraycopy(this.formats, 0, newFormats, 0, this.maxOffset + 1);
                    System.arraycopy(this.offsets, 0, newOffsets, 0, this.maxOffset + 1);
                    System.arraycopy(this.argumentNumbers, 0, newArgumentNumbers, 0, this.maxOffset + 1);
                    this.formats = newFormats;
                    this.offsets = newOffsets;
                    this.argumentNumbers = newArgumentNumbers;
                }
                i = this.maxOffset;
                this.maxOffset = offsetNumber;
                this.offsets[offsetNumber] = segments[0].length();
                this.argumentNumbers[offsetNumber] = argumentNumber;
                Format newFormat = null;
                if (segments[2].length() != 0) {
                    newLength = findKeyword(segments[2], TYPE_KEYWORDS);
                    switch (newLength) {
                        case 0:
                            break;
                        case 1:
                            switch (findKeyword(segments[3], NUMBER_MODIFIER_KEYWORDS)) {
                                case 0:
                                    newFormat = NumberFormat.getInstance(this.locale);
                                    break;
                                case 1:
                                    newFormat = NumberFormat.getCurrencyInstance(this.locale);
                                    break;
                                case 2:
                                    newFormat = NumberFormat.getPercentInstance(this.locale);
                                    break;
                                case 3:
                                    newFormat = NumberFormat.getIntegerInstance(this.locale);
                                    break;
                                default:
                                    try {
                                        newFormat = new DecimalFormat(segments[3], DecimalFormatSymbols.getInstance(this.locale));
                                        break;
                                    } catch (IllegalArgumentException e) {
                                        this.maxOffset = i;
                                        throw e;
                                    }
                            }
                        case 2:
                        case 3:
                            int mod = findKeyword(segments[3], DATE_TIME_MODIFIER_KEYWORDS);
                            if (mod >= 0 && mod < DATE_TIME_MODIFIER_KEYWORDS.length) {
                                if (newLength != 2) {
                                    newFormat = DateFormat.getTimeInstance(DATE_TIME_MODIFIERS[mod], this.locale);
                                    break;
                                } else {
                                    newFormat = DateFormat.getDateInstance(DATE_TIME_MODIFIERS[mod], this.locale);
                                    break;
                                }
                            }
                            try {
                                newFormat = new SimpleDateFormat(segments[3], this.locale);
                                break;
                            } catch (IllegalArgumentException e2) {
                                this.maxOffset = i;
                                throw e2;
                            }
                            break;
                        case 4:
                            try {
                                newFormat = new ChoiceFormat(segments[3]);
                                break;
                            } catch (Exception e3) {
                                this.maxOffset = i;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Choice Pattern incorrect: ");
                                stringBuilder.append(segments[3]);
                                throw new IllegalArgumentException(stringBuilder.toString(), e3);
                            }
                        default:
                            this.maxOffset = i;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("unknown format type: ");
                            stringBuilder2.append(segments[2]);
                            throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                }
                this.formats[offsetNumber] = newFormat;
                return;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("negative argument number: ");
            stringBuilder3.append(argumentNumber);
            throw new IllegalArgumentException(stringBuilder3.toString());
        } catch (NumberFormatException e4) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("can't parse argument number: ");
            stringBuilder4.append(segments[1]);
            throw new IllegalArgumentException(stringBuilder4.toString(), e4);
        }
    }

    private static final int findKeyword(String s, String[] list) {
        int i = 0;
        for (int i2 = 0; i2 < list.length; i2++) {
            if (s.equals(list[i2])) {
                return i2;
            }
        }
        String ls = s.trim().toLowerCase(Locale.ROOT);
        if (ls != s) {
            while (i < list.length) {
                if (ls.equals(list[i])) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    private static final void copyAndFixQuotes(String source, int start, int end, StringBuilder target) {
        boolean quoted = false;
        for (int i = start; i < end; i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                if (!quoted) {
                    target.append('\'');
                    quoted = true;
                }
                target.append(ch);
            } else if (ch == '\'') {
                target.append("''");
            } else {
                if (quoted) {
                    target.append('\'');
                    quoted = false;
                }
                target.append(ch);
            }
        }
        if (quoted) {
            target.append('\'');
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        boolean isValid = this.maxOffset >= -1 && this.formats.length > this.maxOffset && this.offsets.length > this.maxOffset && this.argumentNumbers.length > this.maxOffset;
        if (isValid) {
            int lastOffset = this.pattern.length() + 1;
            int i = this.maxOffset;
            while (i >= 0) {
                if (this.offsets[i] < 0 || this.offsets[i] > lastOffset) {
                    isValid = false;
                    break;
                } else {
                    lastOffset = this.offsets[i];
                    i--;
                }
            }
        }
        if (!isValid) {
            throw new InvalidObjectException("Could not reconstruct MessageFormat from corrupt stream.");
        }
    }
}
