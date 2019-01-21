package android.icu.impl.number;

import android.icu.impl.UCharacterProperty;
import android.icu.text.NumberFormat.Field;
import android.icu.text.SymbolTable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NumberStringBuilder implements CharSequence {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final NumberStringBuilder EMPTY = new NumberStringBuilder();
    private static final Map<Field, Character> fieldToDebugChar = new HashMap();
    private char[] chars;
    private Field[] fields;
    private int length;
    private int zero;

    static {
        fieldToDebugChar.put(Field.SIGN, Character.valueOf('-'));
        fieldToDebugChar.put(Field.INTEGER, Character.valueOf(UCharacterProperty.LATIN_SMALL_LETTER_I_));
        fieldToDebugChar.put(Field.FRACTION, Character.valueOf('f'));
        fieldToDebugChar.put(Field.EXPONENT, Character.valueOf('e'));
        fieldToDebugChar.put(Field.EXPONENT_SIGN, Character.valueOf('+'));
        fieldToDebugChar.put(Field.EXPONENT_SYMBOL, Character.valueOf('E'));
        fieldToDebugChar.put(Field.DECIMAL_SEPARATOR, Character.valueOf('.'));
        fieldToDebugChar.put(Field.GROUPING_SEPARATOR, Character.valueOf(','));
        fieldToDebugChar.put(Field.PERCENT, Character.valueOf('%'));
        fieldToDebugChar.put(Field.PERMILLE, Character.valueOf(8240));
        fieldToDebugChar.put(Field.CURRENCY, Character.valueOf(SymbolTable.SYMBOL_REF));
    }

    public NumberStringBuilder() {
        this(40);
    }

    public NumberStringBuilder(int capacity) {
        this.chars = new char[capacity];
        this.fields = new Field[capacity];
        this.zero = capacity / 2;
        this.length = 0;
    }

    public NumberStringBuilder(NumberStringBuilder source) {
        copyFrom(source);
    }

    public void copyFrom(NumberStringBuilder source) {
        this.chars = Arrays.copyOf(source.chars, source.chars.length);
        this.fields = (Field[]) Arrays.copyOf(source.fields, source.fields.length);
        this.zero = source.zero;
        this.length = source.length;
    }

    public int length() {
        return this.length;
    }

    public int codePointCount() {
        return Character.codePointCount(this, 0, length());
    }

    public char charAt(int index) {
        return this.chars[this.zero + index];
    }

    public Field fieldAt(int index) {
        return this.fields[this.zero + index];
    }

    public int getFirstCodePoint() {
        if (this.length == 0) {
            return -1;
        }
        return Character.codePointAt(this.chars, this.zero, this.zero + this.length);
    }

    public int getLastCodePoint() {
        if (this.length == 0) {
            return -1;
        }
        return Character.codePointBefore(this.chars, this.zero + this.length, this.zero);
    }

    public int codePointAt(int index) {
        return Character.codePointAt(this.chars, this.zero + index, this.zero + this.length);
    }

    public int codePointBefore(int index) {
        return Character.codePointBefore(this.chars, this.zero + index, this.zero);
    }

    public NumberStringBuilder clear() {
        this.zero = getCapacity() / 2;
        this.length = 0;
        return this;
    }

    public int appendCodePoint(int codePoint, Field field) {
        return insertCodePoint(this.length, codePoint, field);
    }

    public int insertCodePoint(int index, int codePoint, Field field) {
        int count = Character.charCount(codePoint);
        int position = prepareForInsert(index, count);
        Character.toChars(codePoint, this.chars, position);
        this.fields[position] = field;
        if (count == 2) {
            this.fields[position + 1] = field;
        }
        return count;
    }

    public int append(CharSequence sequence, Field field) {
        return insert(this.length, sequence, field);
    }

    public int insert(int index, CharSequence sequence, Field field) {
        if (sequence.length() == 0) {
            return 0;
        }
        if (sequence.length() == 1) {
            return insertCodePoint(index, sequence.charAt(0), field);
        }
        return insert(index, sequence, 0, sequence.length(), field);
    }

    public int insert(int index, CharSequence sequence, int start, int end, Field field) {
        int count = end - start;
        int position = prepareForInsert(index, count);
        for (int i = 0; i < count; i++) {
            this.chars[position + i] = sequence.charAt(start + i);
            this.fields[position + i] = field;
        }
        return count;
    }

    public int append(char[] chars, Field[] fields) {
        return insert(this.length, chars, fields);
    }

    public int insert(int index, char[] chars, Field[] fields) {
        int count = chars.length;
        int i = 0;
        if (count == 0) {
            return 0;
        }
        int position = prepareForInsert(index, count);
        while (i < count) {
            this.chars[position + i] = chars[i];
            this.fields[position + i] = fields == null ? null : fields[i];
            i++;
        }
        return count;
    }

    public int append(NumberStringBuilder other) {
        return insert(this.length, other);
    }

    public int insert(int index, NumberStringBuilder other) {
        if (this != other) {
            int count = other.length;
            int i = 0;
            if (count == 0) {
                return 0;
            }
            int position = prepareForInsert(index, count);
            while (i < count) {
                this.chars[position + i] = other.charAt(i);
                this.fields[position + i] = other.fieldAt(i);
                i++;
            }
            return count;
        }
        throw new IllegalArgumentException("Cannot call insert/append on myself");
    }

    private int prepareForInsert(int index, int count) {
        if (index == 0 && this.zero - count >= 0) {
            this.zero -= count;
            this.length += count;
            return this.zero;
        } else if (index != this.length || (this.zero + this.length) + count >= getCapacity()) {
            return prepareForInsertHelper(index, count);
        } else {
            this.length += count;
            return (this.zero + this.length) - count;
        }
    }

    private int prepareForInsertHelper(int index, int count) {
        int oldCapacity = getCapacity();
        int oldZero = this.zero;
        char[] oldChars = this.chars;
        Field[] oldFields = this.fields;
        int newCapacity;
        if (this.length + count > oldCapacity) {
            newCapacity = (this.length + count) * 2;
            int newZero = (newCapacity / 2) - ((this.length + count) / 2);
            char[] newChars = new char[newCapacity];
            Field[] newFields = new Field[newCapacity];
            System.arraycopy(oldChars, oldZero, newChars, newZero, index);
            System.arraycopy(oldChars, oldZero + index, newChars, (newZero + index) + count, this.length - index);
            System.arraycopy(oldFields, oldZero, newFields, newZero, index);
            System.arraycopy(oldFields, oldZero + index, newFields, (newZero + index) + count, this.length - index);
            this.chars = newChars;
            this.fields = newFields;
            this.zero = newZero;
            this.length += count;
        } else {
            newCapacity = (oldCapacity / 2) - ((this.length + count) / 2);
            System.arraycopy(oldChars, oldZero, oldChars, newCapacity, this.length);
            System.arraycopy(oldChars, newCapacity + index, oldChars, (newCapacity + index) + count, this.length - index);
            System.arraycopy(oldFields, oldZero, oldFields, newCapacity, this.length);
            System.arraycopy(oldFields, newCapacity + index, oldFields, (newCapacity + index) + count, this.length - index);
            this.zero = newCapacity;
            this.length += count;
        }
        return this.zero + index;
    }

    private int getCapacity() {
        return this.chars.length;
    }

    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end > this.length || end < start) {
            throw new IndexOutOfBoundsException();
        }
        NumberStringBuilder other = new NumberStringBuilder(this);
        other.zero = this.zero + start;
        other.length = end - start;
        return other;
    }

    public String toString() {
        return new String(this.chars, this.zero, this.length);
    }

    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<NumberStringBuilder [");
        sb.append(toString());
        sb.append("] [");
        for (int i = this.zero; i < this.zero + this.length; i++) {
            if (this.fields[i] == null) {
                sb.append('n');
            } else {
                sb.append(fieldToDebugChar.get(this.fields[i]));
            }
        }
        sb.append("]>");
        return sb.toString();
    }

    public char[] toCharArray() {
        return Arrays.copyOfRange(this.chars, this.zero, this.zero + this.length);
    }

    public Field[] toFieldArray() {
        return (Field[]) Arrays.copyOfRange(this.fields, this.zero, this.zero + this.length);
    }

    public boolean contentEquals(char[] chars, Field[] fields) {
        if (chars.length != this.length || fields.length != this.length) {
            return false;
        }
        int i = 0;
        while (i < this.length) {
            if (this.chars[this.zero + i] != chars[i] || this.fields[this.zero + i] != fields[i]) {
                return false;
            }
            i++;
        }
        return true;
    }

    public boolean contentEquals(NumberStringBuilder other) {
        if (this.length != other.length) {
            return false;
        }
        int i = 0;
        while (i < this.length) {
            if (charAt(i) != other.charAt(i) || fieldAt(i) != other.fieldAt(i)) {
                return false;
            }
            i++;
        }
        return true;
    }

    public int hashCode() {
        throw new UnsupportedOperationException("Don't call #hashCode() or #equals() on a mutable.");
    }

    public boolean equals(Object other) {
        throw new UnsupportedOperationException("Don't call #hashCode() or #equals() on a mutable.");
    }

    public void populateFieldPosition(FieldPosition fp, int offset) {
        Format.Field rawField = fp.getFieldAttribute();
        if (rawField == null) {
            if (fp.getField() == 0) {
                rawField = Field.INTEGER;
            } else if (fp.getField() == 1) {
                rawField = Field.FRACTION;
            } else {
                return;
            }
        }
        if (rawField instanceof Field) {
            Field field = (Field) rawField;
            boolean seenStart = false;
            int fractionStart = -1;
            int i = this.zero;
            while (i <= this.zero + this.length) {
                Field _field = i < this.zero + this.length ? this.fields[i] : null;
                if (!seenStart || field == _field) {
                    if (!seenStart && field == _field) {
                        fp.setBeginIndex((i - this.zero) + offset);
                        seenStart = true;
                    }
                    if (_field == Field.INTEGER || _field == Field.DECIMAL_SEPARATOR) {
                        fractionStart = (i - this.zero) + 1;
                    }
                } else if (field != Field.INTEGER || _field != Field.GROUPING_SEPARATOR) {
                    fp.setEndIndex((i - this.zero) + offset);
                    break;
                }
                i++;
            }
            if (field == Field.FRACTION && !seenStart) {
                fp.setBeginIndex(fractionStart + offset);
                fp.setEndIndex(fractionStart + offset);
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("You must pass an instance of android.icu.text.NumberFormat.Field as your FieldPosition attribute.  You passed: ");
        stringBuilder.append(rawField.getClass().toString());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public AttributedCharacterIterator getIterator() {
        AttributedString as = new AttributedString(toString());
        Field current = null;
        int currentStart = -1;
        for (int i = 0; i < this.length; i++) {
            Field field = this.fields[this.zero + i];
            if (current == Field.INTEGER && field == Field.GROUPING_SEPARATOR) {
                as.addAttribute(Field.GROUPING_SEPARATOR, Field.GROUPING_SEPARATOR, i, i + 1);
            } else if (current != field) {
                if (current != null) {
                    as.addAttribute(current, current, currentStart, i);
                }
                current = field;
                currentStart = i;
            }
        }
        if (current != null) {
            as.addAttribute(current, current, currentStart, this.length);
        }
        return as.getIterator();
    }
}
