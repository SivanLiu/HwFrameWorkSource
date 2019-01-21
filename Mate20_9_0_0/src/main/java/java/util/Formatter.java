package java.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.Types;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale.Category;
import libcore.icu.LocaleData;
import sun.misc.FormattedFloatingDecimal;
import sun.misc.FormattedFloatingDecimal.Form;

public final class Formatter implements Closeable, Flushable {
    private static final int MAX_FD_CHARS = 30;
    private static double scaleUp;
    private Appendable a;
    private final Locale l;
    private IOException lastException;
    private final char zero;

    private static class Conversion {
        static final char BOOLEAN = 'b';
        static final char BOOLEAN_UPPER = 'B';
        static final char CHARACTER = 'c';
        static final char CHARACTER_UPPER = 'C';
        static final char DATE_TIME = 't';
        static final char DATE_TIME_UPPER = 'T';
        static final char DECIMAL_FLOAT = 'f';
        static final char DECIMAL_INTEGER = 'd';
        static final char GENERAL = 'g';
        static final char GENERAL_UPPER = 'G';
        static final char HASHCODE = 'h';
        static final char HASHCODE_UPPER = 'H';
        static final char HEXADECIMAL_FLOAT = 'a';
        static final char HEXADECIMAL_FLOAT_UPPER = 'A';
        static final char HEXADECIMAL_INTEGER = 'x';
        static final char HEXADECIMAL_INTEGER_UPPER = 'X';
        static final char LINE_SEPARATOR = 'n';
        static final char OCTAL_INTEGER = 'o';
        static final char PERCENT_SIGN = '%';
        static final char SCIENTIFIC = 'e';
        static final char SCIENTIFIC_UPPER = 'E';
        static final char STRING = 's';
        static final char STRING_UPPER = 'S';

        private Conversion() {
        }

        static boolean isValid(char c) {
            return isGeneral(c) || isInteger(c) || isFloat(c) || isText(c) || c == DATE_TIME || isCharacter(c);
        }

        static boolean isGeneral(char c) {
            if (c == BOOLEAN_UPPER || c == HASHCODE_UPPER || c == STRING_UPPER || c == BOOLEAN || c == HASHCODE || c == STRING) {
                return true;
            }
            return false;
        }

        static boolean isCharacter(char c) {
            if (c == CHARACTER_UPPER || c == CHARACTER) {
                return true;
            }
            return false;
        }

        static boolean isInteger(char c) {
            if (c == HEXADECIMAL_INTEGER_UPPER || c == DECIMAL_INTEGER || c == OCTAL_INTEGER || c == 'x') {
                return true;
            }
            return false;
        }

        static boolean isFloat(char c) {
            if (!(c == HEXADECIMAL_FLOAT_UPPER || c == SCIENTIFIC_UPPER || c == GENERAL_UPPER || c == HEXADECIMAL_FLOAT)) {
                switch (c) {
                    case 'e':
                    case 'f':
                    case 'g':
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

        static boolean isText(char c) {
            if (c == PERCENT_SIGN || c == LINE_SEPARATOR) {
                return true;
            }
            return false;
        }
    }

    private static class DateTime {
        static final char AM_PM = 'p';
        static final char CENTURY = 'C';
        static final char DATE = 'D';
        static final char DATE_TIME = 'c';
        static final char DAY_OF_MONTH = 'e';
        static final char DAY_OF_MONTH_0 = 'd';
        static final char DAY_OF_YEAR = 'j';
        static final char HOUR = 'l';
        static final char HOUR_0 = 'I';
        static final char HOUR_OF_DAY = 'k';
        static final char HOUR_OF_DAY_0 = 'H';
        static final char ISO_STANDARD_DATE = 'F';
        static final char MILLISECOND = 'L';
        static final char MILLISECOND_SINCE_EPOCH = 'Q';
        static final char MINUTE = 'M';
        static final char MONTH = 'm';
        static final char NAME_OF_DAY = 'A';
        static final char NAME_OF_DAY_ABBREV = 'a';
        static final char NAME_OF_MONTH = 'B';
        static final char NAME_OF_MONTH_ABBREV = 'b';
        static final char NAME_OF_MONTH_ABBREV_X = 'h';
        static final char NANOSECOND = 'N';
        static final char SECOND = 'S';
        static final char SECONDS_SINCE_EPOCH = 's';
        static final char TIME = 'T';
        static final char TIME_12_HOUR = 'r';
        static final char TIME_24_HOUR = 'R';
        static final char YEAR_2 = 'y';
        static final char YEAR_4 = 'Y';
        static final char ZONE = 'Z';
        static final char ZONE_NUMERIC = 'z';

        private DateTime() {
        }

        static boolean isValid(char c) {
            switch (c) {
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case Types.DATALINK /*70*/:
                case 'H':
                case 'I':
                case 'L':
                case 'M':
                case 'N':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'Y':
                case 'Z':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'h':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'p':
                case 'r':
                case 's':
                case 'y':
                case 'z':
                    return true;
                default:
                    return false;
            }
        }
    }

    private static class Flags {
        static final Flags ALTERNATE = new Flags(4);
        static final Flags GROUP = new Flags(64);
        static final Flags LEADING_SPACE = new Flags(16);
        static final Flags LEFT_JUSTIFY = new Flags(1);
        static final Flags NONE = new Flags(0);
        static final Flags PARENTHESES = new Flags(128);
        static final Flags PLUS = new Flags(8);
        static final Flags PREVIOUS = new Flags(256);
        static final Flags UPPERCASE = new Flags(2);
        static final Flags ZERO_PAD = new Flags(32);
        private int flags;

        private Flags(int f) {
            this.flags = f;
        }

        public int valueOf() {
            return this.flags;
        }

        public boolean contains(Flags f) {
            return (this.flags & f.valueOf()) == f.valueOf();
        }

        public Flags dup() {
            return new Flags(this.flags);
        }

        private Flags add(Flags f) {
            this.flags |= f.valueOf();
            return this;
        }

        public Flags remove(Flags f) {
            this.flags &= ~f.valueOf();
            return this;
        }

        public static Flags parse(String s) {
            char[] ca = s.toCharArray();
            int i = 0;
            Flags f = new Flags(0);
            while (i < ca.length) {
                Flags v = parse(ca[i]);
                if (f.contains(v)) {
                    throw new DuplicateFormatFlagsException(v.toString());
                }
                f.add(v);
                i++;
            }
            return f;
        }

        private static Flags parse(char c) {
            if (c == ' ') {
                return LEADING_SPACE;
            }
            if (c == '#') {
                return ALTERNATE;
            }
            if (c == '(') {
                return PARENTHESES;
            }
            if (c == '0') {
                return ZERO_PAD;
            }
            if (c == '<') {
                return PREVIOUS;
            }
            switch (c) {
                case '+':
                    return PLUS;
                case ',':
                    return GROUP;
                case '-':
                    return LEFT_JUSTIFY;
                default:
                    throw new UnknownFormatFlagsException(String.valueOf(c));
            }
        }

        public static String toString(Flags f) {
            return f.toString();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (contains(LEFT_JUSTIFY)) {
                sb.append('-');
            }
            if (contains(UPPERCASE)) {
                sb.append('^');
            }
            if (contains(ALTERNATE)) {
                sb.append('#');
            }
            if (contains(PLUS)) {
                sb.append('+');
            }
            if (contains(LEADING_SPACE)) {
                sb.append(' ');
            }
            if (contains(ZERO_PAD)) {
                sb.append('0');
            }
            if (contains(GROUP)) {
                sb.append(',');
            }
            if (contains(PARENTHESES)) {
                sb.append('(');
            }
            if (contains(PREVIOUS)) {
                sb.append('<');
            }
            return sb.toString();
        }
    }

    private class FormatSpecifierParser {
        private static final String FLAGS = ",-(+# 0<";
        private String conv;
        private int cursor;
        private String flags;
        private final String format;
        private FormatSpecifier fs;
        private String index;
        private String precision;
        private String tT;
        private String width;

        public FormatSpecifierParser(String format, int startIdx) {
            this.format = format;
            this.cursor = startIdx;
            if (nextIsInt()) {
                String nint = nextInt();
                if (peek() == '$') {
                    this.index = nint;
                    advance();
                } else if (nint.charAt(0) == '0') {
                    back(nint.length());
                } else {
                    this.width = nint;
                }
            }
            this.flags = "";
            while (this.width == null && FLAGS.indexOf(peek()) >= 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.flags);
                stringBuilder.append(advance());
                this.flags = stringBuilder.toString();
            }
            if (this.width == null && nextIsInt()) {
                this.width = nextInt();
            }
            if (peek() == '.') {
                advance();
                if (nextIsInt()) {
                    this.precision = nextInt();
                } else {
                    throw new IllegalFormatPrecisionException(peek());
                }
            }
            if (peek() == 't' || peek() == 'T') {
                this.tT = String.valueOf(advance());
            }
            this.conv = String.valueOf(advance());
            this.fs = new FormatSpecifier(this.index, this.flags, this.width, this.precision, this.tT, this.conv);
        }

        private String nextInt() {
            int strBegin = this.cursor;
            while (nextIsInt()) {
                advance();
            }
            return this.format.substring(strBegin, this.cursor);
        }

        private boolean nextIsInt() {
            return !isEnd() && Character.isDigit(peek());
        }

        private char peek() {
            if (!isEnd()) {
                return this.format.charAt(this.cursor);
            }
            throw new UnknownFormatConversionException("End of String");
        }

        private char advance() {
            if (isEnd()) {
                throw new UnknownFormatConversionException("End of String");
            }
            String str = this.format;
            int i = this.cursor;
            this.cursor = i + 1;
            return str.charAt(i);
        }

        private void back(int len) {
            this.cursor -= len;
        }

        private boolean isEnd() {
            return this.cursor == this.format.length();
        }

        public FormatSpecifier getFormatSpecifier() {
            return this.fs;
        }

        public int getEndIdx() {
            return this.cursor;
        }
    }

    private interface FormatString {
        int index();

        void print(Object obj, Locale locale) throws IOException;

        String toString();
    }

    private class FixedString implements FormatString {
        private String s;

        FixedString(String s) {
            this.s = s;
        }

        public int index() {
            return -2;
        }

        public void print(Object arg, Locale l) throws IOException {
            Formatter.this.a.append(this.s);
        }

        public String toString() {
            return this.s;
        }
    }

    private class FormatSpecifier implements FormatString {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private char c;
        private boolean dt = false;
        private Flags f = Flags.NONE;
        private int index = -1;
        private int precision;
        private int width;

        private class BigDecimalLayout {
            private boolean dot = false;
            private StringBuilder exp;
            private StringBuilder mant;
            private int scale;

            public BigDecimalLayout(BigInteger intVal, int scale, BigDecimalLayoutForm form) {
                layout(intVal, scale, form);
            }

            public boolean hasDot() {
                return this.dot;
            }

            public int scale() {
                return this.scale;
            }

            public char[] layoutChars() {
                StringBuilder sb = new StringBuilder(this.mant);
                if (this.exp != null) {
                    sb.append('E');
                    sb.append(this.exp);
                }
                return toCharArray(sb);
            }

            public char[] mantissa() {
                return toCharArray(this.mant);
            }

            public char[] exponent() {
                return toCharArray(this.exp);
            }

            private char[] toCharArray(StringBuilder sb) {
                if (sb == null) {
                    return null;
                }
                char[] result = new char[sb.length()];
                sb.getChars(0, result.length, result, 0);
                return result;
            }

            private void layout(BigInteger intVal, int scale, BigDecimalLayoutForm form) {
                char[] coeff = intVal.toString().toCharArray();
                this.scale = scale;
                this.mant = new StringBuilder(coeff.length + 14);
                if (scale == 0) {
                    int len = coeff.length;
                    if (len > 1) {
                        this.mant.append(coeff[0]);
                        if (form == BigDecimalLayoutForm.SCIENTIFIC) {
                            this.mant.append('.');
                            this.dot = true;
                            this.mant.append(coeff, 1, len - 1);
                            this.exp = new StringBuilder("+");
                            if (len < 10) {
                                StringBuilder stringBuilder = this.exp;
                                stringBuilder.append("0");
                                stringBuilder.append(len - 1);
                            } else {
                                this.exp.append(len - 1);
                            }
                        } else {
                            this.mant.append(coeff, 1, len - 1);
                        }
                    } else {
                        this.mant.append(coeff);
                        if (form == BigDecimalLayoutForm.SCIENTIFIC) {
                            this.exp = new StringBuilder("+00");
                        }
                    }
                    return;
                }
                long adjusted = (-((long) scale)) + ((long) (coeff.length - 1));
                if (form == BigDecimalLayoutForm.DECIMAL_FLOAT) {
                    int pad = scale - coeff.length;
                    if (pad >= 0) {
                        this.mant.append("0.");
                        this.dot = true;
                        while (pad > 0) {
                            this.mant.append('0');
                            pad--;
                        }
                        this.mant.append(coeff);
                    } else if ((-pad) < coeff.length) {
                        this.mant.append(coeff, 0, -pad);
                        this.mant.append('.');
                        this.dot = true;
                        this.mant.append(coeff, -pad, scale);
                    } else {
                        this.mant.append(coeff, 0, coeff.length);
                        for (int i = 0; i < (-scale); i++) {
                            this.mant.append('0');
                        }
                        this.scale = 0;
                    }
                } else {
                    this.mant.append(coeff[0]);
                    if (coeff.length > 1) {
                        this.mant.append('.');
                        this.dot = true;
                        this.mant.append(coeff, 1, coeff.length - 1);
                    }
                    this.exp = new StringBuilder();
                    if (adjusted != 0) {
                        long abs = Math.abs(adjusted);
                        this.exp.append(adjusted < 0 ? '-' : '+');
                        if (abs < 10) {
                            this.exp.append('0');
                        }
                        this.exp.append(abs);
                    } else {
                        this.exp.append("+00");
                    }
                }
            }
        }

        static {
            Class cls = Formatter.class;
        }

        private int index(String s) {
            if (s != null) {
                try {
                    this.index = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                }
            } else {
                this.index = 0;
            }
            return this.index;
        }

        public int index() {
            return this.index;
        }

        private Flags flags(String s) {
            this.f = Flags.parse(s);
            if (this.f.contains(Flags.PREVIOUS)) {
                this.index = -1;
            }
            return this.f;
        }

        Flags flags() {
            return this.f;
        }

        private int width(String s) {
            this.width = -1;
            if (s != null) {
                try {
                    this.width = Integer.parseInt(s);
                    if (this.width < 0) {
                        throw new IllegalFormatWidthException(this.width);
                    }
                } catch (NumberFormatException e) {
                }
            }
            return this.width;
        }

        int width() {
            return this.width;
        }

        private int precision(String s) {
            this.precision = -1;
            if (s != null) {
                try {
                    this.precision = Integer.parseInt(s);
                    if (this.precision < 0) {
                        throw new IllegalFormatPrecisionException(this.precision);
                    }
                } catch (NumberFormatException e) {
                }
            }
            return this.precision;
        }

        int precision() {
            return this.precision;
        }

        private char conversion(String s) {
            this.c = s.charAt(0);
            if (!this.dt) {
                if (Conversion.isValid(this.c)) {
                    if (Character.isUpperCase(this.c)) {
                        this.f.add(Flags.UPPERCASE);
                    }
                    this.c = Character.toLowerCase(this.c);
                    if (Conversion.isText(this.c)) {
                        this.index = -2;
                    }
                } else {
                    throw new UnknownFormatConversionException(String.valueOf(this.c));
                }
            }
            return this.c;
        }

        private char conversion() {
            return this.c;
        }

        FormatSpecifier(String indexStr, String flagsStr, String widthStr, String precisionStr, String tTStr, String convStr) {
            index(indexStr);
            flags(flagsStr);
            width(widthStr);
            precision(precisionStr);
            if (tTStr != null) {
                this.dt = true;
                if (tTStr.equals("T")) {
                    this.f.add(Flags.UPPERCASE);
                }
            }
            conversion(convStr);
            if (this.dt) {
                checkDateTime();
            } else if (Conversion.isGeneral(this.c)) {
                checkGeneral();
            } else if (Conversion.isCharacter(this.c)) {
                checkCharacter();
            } else if (Conversion.isInteger(this.c)) {
                checkInteger();
            } else if (Conversion.isFloat(this.c)) {
                checkFloat();
            } else if (Conversion.isText(this.c)) {
                checkText();
            } else {
                throw new UnknownFormatConversionException(String.valueOf(this.c));
            }
        }

        public void print(Object arg, Locale l) throws IOException {
            if (this.dt) {
                printDateTime(arg, l);
                return;
            }
            char c = this.c;
            if (c != '%') {
                if (c != 'C') {
                    if (c != 's') {
                        if (c != Locale.PRIVATE_USE_EXTENSION) {
                            switch (c) {
                                case 'a':
                                case 'e':
                                case 'f':
                                case 'g':
                                    printFloat(arg, l);
                                    break;
                                case 'b':
                                    printBoolean(arg);
                                    break;
                                case 'c':
                                    break;
                                case 'd':
                                    break;
                                case 'h':
                                    printHashCode(arg);
                                    break;
                                default:
                                    switch (c) {
                                        case 'n':
                                            Formatter.this.a.append(System.lineSeparator());
                                            break;
                                        case 'o':
                                            break;
                                    }
                                    break;
                            }
                        }
                        printInteger(arg, l);
                    } else {
                        printString(arg, l);
                    }
                }
                printCharacter(arg);
            } else {
                Formatter.this.a.append('%');
            }
        }

        private void printInteger(Object arg, Locale l) throws IOException {
            if (arg == null) {
                print("null");
            } else if (arg instanceof Byte) {
                print(((Byte) arg).byteValue(), l);
            } else if (arg instanceof Short) {
                print(((Short) arg).shortValue(), l);
            } else if (arg instanceof Integer) {
                print(((Integer) arg).intValue(), l);
            } else if (arg instanceof Long) {
                print(((Long) arg).longValue(), l);
            } else if (arg instanceof BigInteger) {
                print((BigInteger) arg, l);
            } else {
                failConversion(this.c, arg);
            }
        }

        private void printFloat(Object arg, Locale l) throws IOException {
            if (arg == null) {
                print("null");
            } else if (arg instanceof Float) {
                print(((Float) arg).floatValue(), l);
            } else if (arg instanceof Double) {
                print(((Double) arg).doubleValue(), l);
            } else if (arg instanceof BigDecimal) {
                print((BigDecimal) arg, l);
            } else {
                failConversion(this.c, arg);
            }
        }

        private void printDateTime(Object arg, Locale l) throws IOException {
            if (arg == null) {
                print("null");
                return;
            }
            Calendar cal = null;
            if (arg instanceof Long) {
                cal = Calendar.getInstance(l == null ? Locale.US : l);
                cal.setTimeInMillis(((Long) arg).longValue());
            } else if (arg instanceof Date) {
                cal = Calendar.getInstance(l == null ? Locale.US : l);
                cal.setTime((Date) arg);
            } else if (arg instanceof Calendar) {
                cal = (Calendar) ((Calendar) arg).clone();
                cal.setLenient(true);
            } else if (arg instanceof TemporalAccessor) {
                print((TemporalAccessor) arg, this.c, l);
                return;
            } else {
                failConversion(this.c, arg);
            }
            print(cal, this.c, l);
        }

        private void printCharacter(Object arg) throws IOException {
            if (arg == null) {
                print("null");
                return;
            }
            String s = null;
            if (arg instanceof Character) {
                s = ((Character) arg).toString();
            } else if (arg instanceof Byte) {
                byte i = ((Byte) arg).byteValue();
                if (Character.isValidCodePoint(i)) {
                    s = new String(Character.toChars(i));
                } else {
                    throw new IllegalFormatCodePointException(i);
                }
            } else if (arg instanceof Short) {
                short i2 = ((Short) arg).shortValue();
                if (Character.isValidCodePoint(i2)) {
                    s = new String(Character.toChars(i2));
                } else {
                    throw new IllegalFormatCodePointException(i2);
                }
            } else if (arg instanceof Integer) {
                int i3 = ((Integer) arg).intValue();
                if (Character.isValidCodePoint(i3)) {
                    s = new String(Character.toChars(i3));
                } else {
                    throw new IllegalFormatCodePointException(i3);
                }
            } else {
                failConversion(this.c, arg);
            }
            print(s);
        }

        private void printString(Object arg, Locale l) throws IOException {
            if (arg instanceof Formattable) {
                Formatter fmt = Formatter.this;
                if (fmt.locale() != l) {
                    fmt = new Formatter(fmt.out(), l);
                }
                ((Formattable) arg).formatTo(fmt, this.f.valueOf(), this.width, this.precision);
                return;
            }
            if (this.f.contains(Flags.ALTERNATE)) {
                failMismatch(Flags.ALTERNATE, 's');
            }
            if (arg == null) {
                print("null");
            } else {
                print(arg.toString());
            }
        }

        private void printBoolean(Object arg) throws IOException {
            String s;
            if (arg == null) {
                s = Boolean.toString(false);
            } else if (arg instanceof Boolean) {
                s = ((Boolean) arg).toString();
            } else {
                s = Boolean.toString(true);
            }
            print(s);
        }

        private void printHashCode(Object arg) throws IOException {
            String s;
            if (arg == null) {
                s = "null";
            } else {
                s = Integer.toHexString(arg.hashCode());
            }
            print(s);
        }

        private void print(String s) throws IOException {
            if (this.precision != -1 && this.precision < s.length()) {
                s = s.substring(0, this.precision);
            }
            if (this.f.contains(Flags.UPPERCASE)) {
                s = s.toUpperCase(Formatter.this.l != null ? Formatter.this.l : Locale.getDefault());
            }
            Formatter.this.a.append(justify(s));
        }

        private String justify(String s) {
            if (this.width == -1) {
                return s;
            }
            StringBuilder sb = new StringBuilder();
            boolean pad = this.f.contains(Flags.LEFT_JUSTIFY);
            int sp = this.width - s.length();
            int i = 0;
            if (!pad) {
                for (int i2 = 0; i2 < sp; i2++) {
                    sb.append(' ');
                }
            }
            sb.append(s);
            if (pad) {
                while (i < sp) {
                    sb.append(' ');
                    i++;
                }
            }
            return sb.toString();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("%");
            sb.append(this.f.dup().remove(Flags.UPPERCASE).toString());
            if (this.index > 0) {
                sb.append(this.index);
                sb.append('$');
            }
            if (this.width != -1) {
                sb.append(this.width);
            }
            if (this.precision != -1) {
                sb.append('.');
                sb.append(this.precision);
            }
            if (this.dt) {
                sb.append(this.f.contains(Flags.UPPERCASE) ? 'T' : 't');
            }
            sb.append(this.f.contains(Flags.UPPERCASE) ? Character.toUpperCase(this.c) : this.c);
            return sb.toString();
        }

        private void checkGeneral() {
            if ((this.c == 'b' || this.c == 'h') && this.f.contains(Flags.ALTERNATE)) {
                failMismatch(Flags.ALTERNATE, this.c);
            }
            if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                throw new MissingFormatWidthException(toString());
            }
            checkBadFlags(Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
        }

        private void checkDateTime() {
            if (this.precision != -1) {
                throw new IllegalFormatPrecisionException(this.precision);
            } else if (DateTime.isValid(this.c)) {
                checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
                if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                    throw new MissingFormatWidthException(toString());
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("t");
                stringBuilder.append(this.c);
                throw new UnknownFormatConversionException(stringBuilder.toString());
            }
        }

        private void checkCharacter() {
            if (this.precision == -1) {
                checkBadFlags(Flags.ALTERNATE, Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
                if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                    throw new MissingFormatWidthException(toString());
                }
                return;
            }
            throw new IllegalFormatPrecisionException(this.precision);
        }

        private void checkInteger() {
            checkNumeric();
            if (this.precision != -1) {
                throw new IllegalFormatPrecisionException(this.precision);
            } else if (this.c == 'd') {
                checkBadFlags(Flags.ALTERNATE);
            } else if (this.c == 'o') {
                checkBadFlags(Flags.GROUP);
            } else {
                checkBadFlags(Flags.GROUP);
            }
        }

        private void checkBadFlags(Flags... badFlags) {
            for (int i = 0; i < badFlags.length; i++) {
                if (this.f.contains(badFlags[i])) {
                    failMismatch(badFlags[i], this.c);
                }
            }
        }

        private void checkFloat() {
            checkNumeric();
            if (this.c != 'f') {
                if (this.c == 'a') {
                    checkBadFlags(Flags.PARENTHESES, Flags.GROUP);
                } else if (this.c == 'e') {
                    checkBadFlags(Flags.GROUP);
                } else if (this.c == 'g') {
                    checkBadFlags(Flags.ALTERNATE);
                }
            }
        }

        private void checkNumeric() {
            if (this.width != -1 && this.width < 0) {
                throw new IllegalFormatWidthException(this.width);
            } else if (this.precision != -1 && this.precision < 0) {
                throw new IllegalFormatPrecisionException(this.precision);
            } else if (this.width == -1 && (this.f.contains(Flags.LEFT_JUSTIFY) || this.f.contains(Flags.ZERO_PAD))) {
                throw new MissingFormatWidthException(toString());
            } else if ((this.f.contains(Flags.PLUS) && this.f.contains(Flags.LEADING_SPACE)) || (this.f.contains(Flags.LEFT_JUSTIFY) && this.f.contains(Flags.ZERO_PAD))) {
                throw new IllegalFormatFlagsException(this.f.toString());
            }
        }

        private void checkText() {
            if (this.precision == -1) {
                char c = this.c;
                if (c != '%') {
                    if (c == 'n') {
                        if (this.width != -1) {
                            throw new IllegalFormatWidthException(this.width);
                        } else if (this.f.valueOf() != Flags.NONE.valueOf()) {
                            throw new IllegalFormatFlagsException(this.f.toString());
                        } else {
                            return;
                        }
                    }
                    return;
                } else if (this.f.valueOf() != Flags.LEFT_JUSTIFY.valueOf() && this.f.valueOf() != Flags.NONE.valueOf()) {
                    throw new IllegalFormatFlagsException(this.f.toString());
                } else if (this.width == -1 && this.f.contains(Flags.LEFT_JUSTIFY)) {
                    throw new MissingFormatWidthException(toString());
                } else {
                    return;
                }
            }
            throw new IllegalFormatPrecisionException(this.precision);
        }

        private void print(byte value, Locale l) throws IOException {
            long v = (long) value;
            if (value < (byte) 0 && (this.c == 'o' || this.c == Locale.PRIVATE_USE_EXTENSION)) {
                v += 256;
            }
            print(v, l);
        }

        private void print(short value, Locale l) throws IOException {
            long v = (long) value;
            if (value < (short) 0 && (this.c == 'o' || this.c == Locale.PRIVATE_USE_EXTENSION)) {
                v += 65536;
            }
            print(v, l);
        }

        private void print(int value, Locale l) throws IOException {
            long v = (long) value;
            if (value < 0 && (this.c == 'o' || this.c == Locale.PRIVATE_USE_EXTENSION)) {
                v += 4294967296L;
            }
            print(v, l);
        }

        private void print(long value, Locale l) throws IOException {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            String s;
            int len;
            if (this.c == 'd') {
                boolean i2;
                char[] toCharArray;
                if (value < 0) {
                    i2 = true;
                }
                boolean neg = i2;
                if (value < 0) {
                    toCharArray = Long.toString(value, 10).substring(1).toCharArray();
                } else {
                    toCharArray = Long.toString(value, 10).toCharArray();
                }
                char[] va = toCharArray;
                leadingSign(sb, neg);
                localizedMagnitude(sb, va, this.f, adjustWidth(this.width, this.f, neg), l);
                trailingSign(sb, neg);
            } else if (this.c == 'o') {
                int length;
                checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
                s = Long.toOctalString(value);
                if (this.f.contains(Flags.ALTERNATE)) {
                    length = s.length() + 1;
                } else {
                    length = s.length();
                }
                len = length;
                if (this.f.contains(Flags.ALTERNATE)) {
                    sb.append('0');
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    while (i < this.width - len) {
                        sb.append('0');
                        i++;
                    }
                }
                sb.append(s);
            } else if (this.c == Locale.PRIVATE_USE_EXTENSION) {
                checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
                s = Long.toHexString(value);
                if (this.f.contains(Flags.ALTERNATE)) {
                    len = s.length() + 2;
                } else {
                    len = s.length();
                }
                if (this.f.contains(Flags.ALTERNATE)) {
                    sb.append(this.f.contains(Flags.UPPERCASE) ? "0X" : "0x");
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    while (i < this.width - len) {
                        sb.append('0');
                        i++;
                    }
                }
                if (this.f.contains(Flags.UPPERCASE)) {
                    s = s.toUpperCase();
                }
                sb.append(s);
            }
            Formatter.this.a.append(justify(sb.toString()));
        }

        private StringBuilder leadingSign(StringBuilder sb, boolean neg) {
            if (neg) {
                if (this.f.contains(Flags.PARENTHESES)) {
                    sb.append('(');
                } else {
                    sb.append('-');
                }
            } else if (this.f.contains(Flags.PLUS)) {
                sb.append('+');
            } else if (this.f.contains(Flags.LEADING_SPACE)) {
                sb.append(' ');
            }
            return sb;
        }

        private StringBuilder trailingSign(StringBuilder sb, boolean neg) {
            if (neg && this.f.contains(Flags.PARENTHESES)) {
                sb.append(')');
            }
            return sb;
        }

        private void print(BigInteger value, Locale l) throws IOException {
            StringBuilder sb = new StringBuilder();
            boolean z = false;
            boolean neg = value.signum() == -1;
            BigInteger v = value.abs();
            leadingSign(sb, neg);
            String s;
            int len;
            int i;
            if (this.c == 'd') {
                localizedMagnitude(sb, v.toString().toCharArray(), this.f, adjustWidth(this.width, this.f, neg), l);
            } else if (this.c == 'o') {
                s = v.toString(8);
                len = s.length() + sb.length();
                if (neg && this.f.contains(Flags.PARENTHESES)) {
                    len++;
                }
                if (this.f.contains(Flags.ALTERNATE)) {
                    len++;
                    sb.append('0');
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    for (i = 0; i < this.width - len; i++) {
                        sb.append('0');
                    }
                }
                sb.append(s);
            } else if (this.c == Locale.PRIVATE_USE_EXTENSION) {
                s = v.toString(16);
                len = s.length() + sb.length();
                if (neg && this.f.contains(Flags.PARENTHESES)) {
                    len++;
                }
                if (this.f.contains(Flags.ALTERNATE)) {
                    len += 2;
                    sb.append(this.f.contains(Flags.UPPERCASE) ? "0X" : "0x");
                }
                if (this.f.contains(Flags.ZERO_PAD)) {
                    for (i = 0; i < this.width - len; i++) {
                        sb.append('0');
                    }
                }
                if (this.f.contains(Flags.UPPERCASE)) {
                    s = s.toUpperCase();
                }
                sb.append(s);
            }
            if (value.signum() == -1) {
                z = true;
            }
            trailingSign(sb, z);
            Formatter.this.a.append(justify(sb.toString()));
        }

        private void print(float value, Locale l) throws IOException {
            print((double) value, l);
        }

        private void print(double value, Locale l) throws IOException {
            StringBuilder sb = new StringBuilder();
            boolean neg = Double.compare(value, 0.0d) == -1;
            if (Double.isNaN(value)) {
                sb.append(this.f.contains(Flags.UPPERCASE) ? "NAN" : "NaN");
            } else {
                double v = Math.abs(value);
                leadingSign(sb, neg);
                if (Double.isInfinite(v)) {
                    sb.append(this.f.contains(Flags.UPPERCASE) ? "INFINITY" : "Infinity");
                } else {
                    print(sb, v, l, this.f, this.c, this.precision, neg);
                }
                trailingSign(sb, neg);
            }
            Formatter.this.a.append(justify(sb.toString()));
        }

        private void print(StringBuilder sb, double value, Locale l, Flags f, char c, int precision, boolean neg) throws IOException {
            StringBuilder stringBuilder = sb;
            double d = value;
            Flags flags = f;
            char c2 = c;
            int i = precision;
            boolean z = neg;
            int i2 = 6;
            int prec;
            int newW;
            Object exp;
            int i3;
            Flags flags2;
            char sign;
            if (c2 == 'e') {
                String toUpperCase;
                if (i != -1) {
                    i2 = i;
                }
                prec = i2;
                FormattedFloatingDecimal fd = FormattedFloatingDecimal.valueOf(d, prec, Form.SCIENTIFIC);
                char[] mant = addZeros(fd.getMantissa(), prec);
                if (flags.contains(Flags.ALTERNATE) && prec == 0) {
                    mant = addDot(mant);
                }
                char[] mant2 = mant;
                Object exp2 = d == 0.0d ? new char[]{'+', '0', '0'} : fd.getExponent();
                newW = this.width;
                if (this.width != -1) {
                    newW = adjustWidth((this.width - exp2.length) - 1, flags, z);
                }
                exp = exp2;
                i3 = 1;
                localizedMagnitude(stringBuilder, mant2, flags, newW, l);
                Locale separatorLocale = l != null ? l : Locale.getDefault();
                LocaleData localeData = LocaleData.get(separatorLocale);
                if (flags.contains(Flags.UPPERCASE)) {
                    toUpperCase = localeData.exponentSeparator.toUpperCase(separatorLocale);
                } else {
                    toUpperCase = localeData.exponentSeparator.toLowerCase(separatorLocale);
                }
                stringBuilder.append(toUpperCase);
                flags2 = f.dup().remove(Flags.GROUP);
                sign = exp[0];
                stringBuilder.append(sign);
                mant = new char[(exp.length - i3)];
                System.arraycopy(exp, i3, (Object) mant, 0, exp.length - i3);
                Object tmp = mant;
                stringBuilder.append(localizedMagnitude(null, mant, flags2, -1, l));
                return;
            }
            i3 = 1;
            char[] mant3;
            int prec2;
            if (c2 == 'f') {
                if (i != -1) {
                    i2 = i;
                }
                prec = i2;
                mant3 = addZeros(FormattedFloatingDecimal.valueOf(d, prec, Form.DECIMAL_FLOAT).getMantissa(), prec);
                if (flags.contains(Flags.ALTERNATE) && prec == 0) {
                    mant3 = addDot(mant3);
                }
                char[] mant4 = mant3;
                newW = this.width;
                if (this.width != -1) {
                    newW = adjustWidth(this.width, flags, z);
                }
                localizedMagnitude(stringBuilder, mant4, flags, newW, l);
            } else if (c2 == 'g') {
                char[] mant5;
                Object exp3;
                i2 = i;
                if (i == -1) {
                    i2 = 6;
                } else if (i == 0) {
                    i2 = 1;
                }
                if (d == 0.0d) {
                    mant5 = new char[i3];
                    mant5[0] = '0';
                    exp3 = null;
                    newW = 0;
                } else {
                    FormattedFloatingDecimal fd2 = FormattedFloatingDecimal.valueOf(d, i2, Form.GENERAL);
                    Object exp4 = fd2.getExponent();
                    char[] mant6 = fd2.getMantissa();
                    newW = fd2.getExponentRounded();
                    exp3 = exp4;
                    mant5 = mant6;
                }
                int expRounded = newW;
                if (exp3 != null) {
                    i2--;
                } else {
                    i2 -= expRounded + 1;
                }
                prec2 = i2;
                mant3 = addZeros(mant5, prec2);
                if (flags.contains(Flags.ALTERNATE) && prec2 == 0) {
                    mant3 = addDot(mant3);
                }
                char[] mant7 = mant3;
                newW = this.width;
                if (this.width != -1) {
                    if (exp3 != null) {
                        newW = adjustWidth((this.width - exp3.length) - i3, flags, z);
                    } else {
                        newW = adjustWidth(this.width, flags, z);
                    }
                }
                exp = exp3;
                localizedMagnitude(stringBuilder, mant7, flags, newW, l);
                if (exp != null) {
                    stringBuilder.append(flags.contains(Flags.UPPERCASE) ? 'E' : 'e');
                    flags2 = f.dup().remove(Flags.GROUP);
                    char sign2 = exp[0];
                    stringBuilder.append(sign2);
                    Object tmp2 = new char[(exp.length - i3)];
                    System.arraycopy(exp, i3, tmp2, 0, exp.length - i3);
                    stringBuilder.append(localizedMagnitude(null, (char[]) tmp2, flags2, -1, l));
                }
            } else if (c2 == 'a') {
                newW = i;
                if (i == -1) {
                    newW = 0;
                } else if (i == 0) {
                    newW = 1;
                }
                String s = hexDouble(d, newW);
                boolean upper = flags.contains(Flags.UPPERCASE);
                stringBuilder.append(upper ? "0X" : "0x");
                if (flags.contains(Flags.ZERO_PAD)) {
                    for (int i4 = 0; i4 < (this.width - s.length()) - 2; i4++) {
                        stringBuilder.append('0');
                    }
                }
                sign = 'p';
                prec2 = s.indexOf(112);
                char[] va = s.substring(0, prec2).toCharArray();
                if (upper) {
                    va = new String(va).toUpperCase(Locale.US).toCharArray();
                }
                stringBuilder.append(newW != 0 ? addZeros(va, newW) : va);
                if (upper) {
                    sign = 'P';
                }
                stringBuilder.append(sign);
                stringBuilder.append(s.substring(prec2 + 1));
            }
        }

        private char[] addZeros(char[] v, int prec) {
            int i = 0;
            while (i < v.length && v[i] != '.') {
                i++;
            }
            boolean needDot = false;
            if (i == v.length) {
                needDot = true;
            }
            int i2 = 1;
            int outPrec = (v.length - i) - (needDot ? 0 : 1);
            if (outPrec == prec) {
                return v;
            }
            int length = (v.length + prec) - outPrec;
            if (!needDot) {
                i2 = 0;
            }
            Object tmp = new char[(length + i2)];
            System.arraycopy((Object) v, 0, tmp, 0, v.length);
            int start = v.length;
            if (needDot) {
                tmp[v.length] = 46;
                start++;
            }
            for (int j = start; j < tmp.length; j++) {
                tmp[j] = '0';
            }
            return tmp;
        }

        private String hexDouble(double d, int prec) {
            int i = prec;
            if (!Double.isFinite(d) || d == 0.0d || i == 0 || i >= 13) {
                return Double.toHexString(d).substring(2);
            }
            double d2;
            int exponent = Math.getExponent(d);
            boolean subnormal = exponent == -1023;
            if (subnormal) {
                Formatter.scaleUp = Math.scalb(1.0d, 54);
                d2 = d * Formatter.scaleUp;
                exponent = Math.getExponent(d2);
            } else {
                d2 = d;
            }
            int precision = (i * 4) + 1;
            int shiftDistance = 53 - precision;
            long doppel = Double.doubleToLongBits(d2);
            long newSignif = (Long.MAX_VALUE & doppel) >> shiftDistance;
            long roundingBits = (~(-1 << shiftDistance)) & doppel;
            boolean leastZero = (newSignif & 1) == 0;
            boolean round = ((1 << (shiftDistance + -1)) & roundingBits) != 0;
            boolean z = shiftDistance > 1 && ((~(1 << (shiftDistance - 1))) & roundingBits) != 0;
            boolean sticky = z;
            if ((leastZero && round && sticky) || (!leastZero && round)) {
                newSignif++;
            }
            boolean subnormal2 = subnormal;
            double result = Double.longBitsToDouble((Long.MIN_VALUE & doppel) | (newSignif << shiftDistance));
            if (Double.isInfinite(result)) {
                return "1.0p1024";
            }
            String res = Double.toHexString(result).substring(2);
            if (!subnormal2) {
                return res;
            }
            int idx = res.indexOf(112);
            if (idx == -1) {
                return null;
            }
            i = Integer.parseInt(res.substring(idx + 1)) - 54;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(res.substring(0.0d, idx));
            stringBuilder.append("p");
            stringBuilder.append(Integer.toString(i));
            return stringBuilder.toString();
        }

        private void print(BigDecimal value, Locale l) throws IOException {
            if (this.c == 'a') {
                failConversion(this.c, value);
            }
            StringBuilder sb = new StringBuilder();
            boolean neg = value.signum() == -1;
            BigDecimal v = value.abs();
            leadingSign(sb, neg);
            print(sb, v, l, this.f, this.c, this.precision, neg);
            trailingSign(sb, neg);
            Formatter.this.a.append(justify(sb.toString()));
        }

        private void print(StringBuilder sb, BigDecimal value, Locale l, Flags f, char c, int precision, boolean neg) throws IOException {
            StringBuilder stringBuilder = sb;
            BigDecimal value2 = value;
            Flags flags = f;
            char c2 = c;
            int i = precision;
            boolean z = neg;
            int i2 = 6;
            int prec;
            BigDecimal v;
            BigDecimalLayout bdl;
            int newW;
            if (c2 == 'e') {
                int compPrec;
                int newW2;
                if (i != -1) {
                    i2 = i;
                }
                prec = i2;
                int scale = value.scale();
                int origPrec = value.precision();
                i2 = 0;
                if (prec > origPrec - 1) {
                    compPrec = origPrec;
                    i2 = prec - (origPrec - 1);
                } else {
                    compPrec = prec + 1;
                }
                MathContext mc = new MathContext(compPrec);
                v = new BigDecimal(value.unscaledValue(), scale, mc);
                bdl = new BigDecimalLayout(v.unscaledValue(), v.scale(), BigDecimalLayoutForm.SCIENTIFIC);
                char[] mant = bdl.mantissa();
                if ((origPrec == 1 || !bdl.hasDot()) && (i2 > 0 || flags.contains(Flags.ALTERNATE))) {
                    mant = addDot(mant);
                }
                char[] mant2 = trailingZeros(mant, i2);
                Object exp = bdl.exponent();
                newW = this.width;
                if (this.width != -1) {
                    newW2 = adjustWidth((this.width - exp.length) - 1, flags, z);
                } else {
                    newW2 = newW;
                }
                Object exp2 = exp;
                localizedMagnitude(stringBuilder, mant2, flags, newW2, l);
                stringBuilder.append(flags.contains(Flags.UPPERCASE) ? 'E' : 'e');
                Flags flags2 = f.dup().remove(Flags.GROUP);
                char sign = exp2[0];
                stringBuilder.append(exp2[0]);
                Object tmp = new char[(exp2.length - 1)];
                System.arraycopy(exp2, 1, tmp, 0, exp2.length - 1);
                stringBuilder.append(localizedMagnitude(null, (char[]) tmp, flags2, -1, l));
                return;
            }
            newW = 0;
            if (c2 == 'f') {
                if (i != -1) {
                    i2 = i;
                }
                int prec2 = i2;
                int scale2 = value.scale();
                if (scale2 > prec2) {
                    i2 = value.precision();
                    if (i2 <= scale2) {
                        value2 = value2.setScale(prec2, RoundingMode.HALF_UP);
                    } else {
                        value2 = new BigDecimal(value.unscaledValue(), scale2, new MathContext(i2 - (scale2 - prec2)));
                    }
                }
                bdl = new BigDecimalLayout(value2.unscaledValue(), value2.scale(), BigDecimalLayoutForm.DECIMAL_FLOAT);
                char[] mant3 = bdl.mantissa();
                if (bdl.scale() < prec2) {
                    newW = prec2 - bdl.scale();
                }
                prec = newW;
                if (bdl.scale() == 0 && (flags.contains(Flags.ALTERNATE) || prec > 0)) {
                    mant3 = addDot(bdl.mantissa());
                }
                localizedMagnitude(stringBuilder, trailingZeros(mant3, prec), flags, adjustWidth(this.width, flags, z), l);
            } else if (c2 == 'g') {
                i2 = i;
                if (i == -1) {
                    i2 = 6;
                } else if (i == 0) {
                    i2 = 1;
                }
                int prec3 = i2;
                v = BigDecimal.valueOf(1, 4);
                BigDecimal tenToThePrec = BigDecimal.valueOf(1, -prec3);
                BigDecimal bigDecimal;
                BigDecimal bigDecimal2;
                if (value2.equals(BigDecimal.ZERO)) {
                    bigDecimal = tenToThePrec;
                    bigDecimal2 = v;
                } else if (value2.compareTo(v) == -1 || value2.compareTo(tenToThePrec) != -1) {
                    print(stringBuilder, value2, l, flags, 'e', prec3 - 1, z);
                    return;
                } else {
                    bigDecimal = tenToThePrec;
                    bigDecimal2 = v;
                }
                print(stringBuilder, value2, l, flags, 'f', (prec3 - ((-value.scale()) + (value.unscaledValue().toString().length() - 1))) - 1, z);
            } else {
                if (c2 == 'a') {
                }
            }
        }

        private int adjustWidth(int width, Flags f, boolean neg) {
            int newW = width;
            if (newW != -1 && neg && f.contains(Flags.PARENTHESES)) {
                return newW - 1;
            }
            return newW;
        }

        private char[] addDot(char[] mant) {
            char[] tmp = mant;
            Object tmp2 = new char[(mant.length + 1)];
            System.arraycopy((Object) mant, 0, tmp2, 0, mant.length);
            tmp2[tmp2.length - 1] = '.';
            return tmp2;
        }

        private char[] trailingZeros(char[] mant, int nzeros) {
            char[] tmp = mant;
            if (nzeros > 0) {
                tmp = new char[(mant.length + nzeros)];
                System.arraycopy((Object) mant, 0, (Object) tmp, 0, mant.length);
                for (int i = mant.length; i < tmp.length; i++) {
                    tmp[i] = '0';
                }
            }
            return tmp;
        }

        private void print(Calendar t, char c, Locale l) throws IOException {
            StringBuilder sb = new StringBuilder();
            print(sb, t, c, l);
            CharSequence s = justify(sb.toString());
            if (this.f.contains(Flags.UPPERCASE)) {
                s = s.toUpperCase();
            }
            Formatter.this.a.append(s);
        }

        private Appendable print(StringBuilder sb, Calendar t, char c, Locale l) throws IOException {
            StringBuilder sb2;
            Calendar calendar = t;
            char c2 = c;
            Locale locale = l;
            if (sb == null) {
                sb2 = new StringBuilder();
            } else {
                sb2 = sb;
            }
            boolean z = false;
            boolean i = true;
            int i2;
            DateFormatSymbols dfs;
            int size;
            int i3;
            Flags flags;
            switch (c2) {
                case 'A':
                case 'a':
                    i2 = calendar.get(7);
                    dfs = DateFormatSymbols.getInstance(locale == null ? Locale.US : locale);
                    if (c2 != 'A') {
                        sb2.append(dfs.getShortWeekdays()[i2]);
                        break;
                    }
                    sb2.append(dfs.getWeekdays()[i2]);
                    break;
                case 'B':
                case 'b':
                case 'h':
                    i2 = calendar.get(2);
                    dfs = DateFormatSymbols.getInstance(locale == null ? Locale.US : locale);
                    if (c2 != 'B') {
                        sb2.append(dfs.getShortMonths()[i2]);
                        break;
                    }
                    sb2.append(dfs.getMonths()[i2]);
                    break;
                case 'C':
                case 'Y':
                case 'y':
                    i2 = calendar.get(1);
                    size = 2;
                    if (c2 == 'C') {
                        i2 /= 100;
                    } else if (c2 == 'Y') {
                        size = 4;
                    } else if (c2 == 'y') {
                        i2 %= 100;
                    }
                    sb2.append(localizedMagnitude(null, (long) i2, Flags.ZERO_PAD, size, locale));
                    break;
                case 'D':
                    print(sb2, calendar, 'm', locale).append('/');
                    print(sb2, calendar, 'd', locale).append('/');
                    print(sb2, calendar, 'y', locale);
                    break;
                case Types.DATALINK /*70*/:
                    print(sb2, calendar, 'Y', locale).append('-');
                    print(sb2, calendar, 'm', locale).append('-');
                    print(sb2, calendar, 'd', locale);
                    break;
                case 'H':
                case 'I':
                case 'k':
                case 'l':
                    i2 = calendar.get(11);
                    if (c2 == 'I' || c2 == 'l') {
                        if (i2 != 0) {
                            size = 12;
                            if (i2 != 12) {
                                size = i2 % 12;
                            }
                        } else {
                            size = 12;
                        }
                        i2 = size;
                    }
                    i3 = i2;
                    if (c2 == 'H' || c2 == 'I') {
                        flags = Flags.ZERO_PAD;
                    } else {
                        flags = Flags.NONE;
                    }
                    sb2.append(localizedMagnitude(null, (long) i3, flags, 2, locale));
                    break;
                case 'L':
                    sb2.append(localizedMagnitude(null, (long) calendar.get(14), (Flags) Flags.ZERO_PAD, 3, locale));
                    break;
                case 'M':
                    sb2.append(localizedMagnitude(null, (long) calendar.get(12), (Flags) Flags.ZERO_PAD, 2, locale));
                    break;
                case 'N':
                    sb2.append(localizedMagnitude(null, (long) (calendar.get(14) * 1000000), (Flags) Flags.ZERO_PAD, 9, locale));
                    break;
                case 'Q':
                    sb2.append(localizedMagnitude(null, t.getTimeInMillis(), Flags.NONE, this.width, locale));
                    break;
                case 'R':
                case 'T':
                    print(sb2, calendar, 'H', locale).append(':');
                    print(sb2, calendar, 'M', locale);
                    if (c2 == 'T') {
                        sb2.append(':');
                        print(sb2, calendar, 'S', locale);
                        break;
                    }
                    break;
                case 'S':
                    sb2.append(localizedMagnitude(null, (long) calendar.get(13), (Flags) Flags.ZERO_PAD, 2, locale));
                    break;
                case 'Z':
                    Locale locale2;
                    TimeZone tz = t.getTimeZone();
                    if (calendar.get(16) == 0) {
                        i = false;
                    }
                    if (locale == null) {
                        locale2 = Locale.US;
                    } else {
                        locale2 = locale;
                    }
                    sb2.append(tz.getDisplayName(i, 0, locale2));
                    break;
                case 'c':
                    print(sb2, calendar, 'a', locale).append(' ');
                    print(sb2, calendar, 'b', locale).append(' ');
                    print(sb2, calendar, 'd', locale).append(' ');
                    print(sb2, calendar, 'T', locale).append(' ');
                    print(sb2, calendar, 'Z', locale).append(' ');
                    print(sb2, calendar, 'Y', locale);
                    break;
                case 'd':
                case 'e':
                    i3 = calendar.get(5);
                    if (c2 == 'd') {
                        flags = Flags.ZERO_PAD;
                    } else {
                        flags = Flags.NONE;
                    }
                    sb2.append(localizedMagnitude(null, (long) i3, flags, 2, locale));
                    break;
                case 'j':
                    sb2.append(localizedMagnitude(null, (long) calendar.get(6), (Flags) Flags.ZERO_PAD, 3, locale));
                    break;
                case 'm':
                    sb2.append(localizedMagnitude(null, (long) (calendar.get(2) + 1), (Flags) Flags.ZERO_PAD, 2, locale));
                    break;
                case 'p':
                    String[] ampm = new String[]{"AM", "PM"};
                    if (!(locale == null || locale == Locale.US)) {
                        ampm = DateFormatSymbols.getInstance(l).getAmPmStrings();
                    }
                    sb2.append(ampm[calendar.get(9)].toLowerCase(locale != null ? locale : Locale.US));
                    break;
                case 'r':
                    print(sb2, calendar, 'I', locale).append(':');
                    print(sb2, calendar, 'M', locale).append(':');
                    print(sb2, calendar, 'S', locale).append(' ');
                    StringBuilder tsb = new StringBuilder();
                    print(tsb, calendar, 'p', locale);
                    sb2.append(tsb.toString().toUpperCase(locale != null ? locale : Locale.US));
                    break;
                case 's':
                    sb2.append(localizedMagnitude(null, t.getTimeInMillis() / 1000, Flags.NONE, this.width, locale));
                    break;
                case 'z':
                    int i4 = calendar.get(15) + calendar.get(16);
                    if (i4 < 0) {
                        z = true;
                    }
                    boolean neg = z;
                    sb2.append(neg ? '-' : '+');
                    if (neg) {
                        i4 = -i4;
                    }
                    int min = i4 / 60000;
                    sb2.append(localizedMagnitude(null, (long) (((min / 60) * 100) + (min % 60)), Flags.ZERO_PAD, 4, locale));
                    break;
            }
            return sb2;
        }

        private void print(TemporalAccessor t, char c, Locale l) throws IOException {
            StringBuilder sb = new StringBuilder();
            print(sb, t, c, l);
            CharSequence s = justify(sb.toString());
            if (this.f.contains(Flags.UPPERCASE)) {
                s = s.toUpperCase();
            }
            Formatter.this.a.append(s);
        }

        private Appendable print(StringBuilder sb, TemporalAccessor t, char c, Locale l) throws IOException {
            if (sb == null) {
                sb = new StringBuilder();
            }
            boolean z = false;
            int i;
            DateFormatSymbols dfs;
            int size;
            switch (c) {
                case 'A':
                case 'a':
                    i = (t.get(ChronoField.DAY_OF_WEEK) % 7) + 1;
                    dfs = DateFormatSymbols.getInstance(l == null ? Locale.US : l);
                    if (c != 'A') {
                        sb.append(dfs.getShortWeekdays()[i]);
                        break;
                    }
                    sb.append(dfs.getWeekdays()[i]);
                    break;
                case 'B':
                case 'b':
                case 'h':
                    i = t.get(ChronoField.MONTH_OF_YEAR) - 1;
                    dfs = DateFormatSymbols.getInstance(l == null ? Locale.US : l);
                    if (c != 'B') {
                        sb.append(dfs.getShortMonths()[i]);
                        break;
                    }
                    sb.append(dfs.getMonths()[i]);
                    break;
                case 'C':
                case 'Y':
                case 'y':
                    int i2 = t.get(ChronoField.YEAR_OF_ERA);
                    size = 2;
                    if (c == 'C') {
                        i2 /= 100;
                    } else if (c == 'Y') {
                        size = 4;
                    } else if (c == 'y') {
                        i2 %= 100;
                    }
                    sb.append(localizedMagnitude(null, (long) i2, Flags.ZERO_PAD, size, l));
                    break;
                case 'D':
                    print(sb, t, 'm', l).append('/');
                    print(sb, t, 'd', l).append('/');
                    print(sb, t, 'y', l);
                    break;
                case Types.DATALINK /*70*/:
                    print(sb, t, 'Y', l).append('-');
                    print(sb, t, 'm', l).append('-');
                    print(sb, t, 'd', l);
                    break;
                case 'H':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.HOUR_OF_DAY), Flags.ZERO_PAD, 2, l));
                    break;
                case 'I':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.CLOCK_HOUR_OF_AMPM), Flags.ZERO_PAD, 2, l));
                    break;
                case 'L':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.MILLI_OF_SECOND), Flags.ZERO_PAD, 3, l));
                    break;
                case 'M':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.MINUTE_OF_HOUR), Flags.ZERO_PAD, 2, l));
                    break;
                case 'N':
                    sb.append(localizedMagnitude(null, (long) (t.get(ChronoField.MILLI_OF_SECOND) * 1000000), Flags.ZERO_PAD, 9, l));
                    break;
                case 'Q':
                    sb.append(localizedMagnitude(null, (t.getLong(ChronoField.INSTANT_SECONDS) * 1000) + t.getLong(ChronoField.MILLI_OF_SECOND), Flags.NONE, this.width, l));
                    break;
                case 'R':
                case 'T':
                    print(sb, t, 'H', l).append(':');
                    print(sb, t, 'M', l);
                    if (c == 'T') {
                        sb.append(':');
                        print(sb, t, 'S', l);
                        break;
                    }
                    break;
                case 'S':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.SECOND_OF_MINUTE), Flags.ZERO_PAD, 2, l));
                    break;
                case 'Z':
                    ZoneId zid = (ZoneId) t.query(TemporalQueries.zone());
                    if (zid != null) {
                        if (!(zid instanceof ZoneOffset) && t.isSupported(ChronoField.INSTANT_SECONDS)) {
                            Locale locale;
                            Instant instant = Instant.from(t);
                            TimeZone timeZone = TimeZone.getTimeZone(zid.getId());
                            boolean isDaylightSavings = zid.getRules().isDaylightSavings(instant);
                            if (l == null) {
                                locale = Locale.US;
                            } else {
                                locale = l;
                            }
                            sb.append(timeZone.getDisplayName(isDaylightSavings, 0, locale));
                            break;
                        }
                        sb.append(zid.getId());
                        break;
                    }
                    throw new IllegalFormatConversionException(c, t.getClass());
                    break;
                case 'c':
                    print(sb, t, 'a', l).append(' ');
                    print(sb, t, 'b', l).append(' ');
                    print(sb, t, 'd', l).append(' ');
                    print(sb, t, 'T', l).append(' ');
                    print(sb, t, 'Z', l).append(' ');
                    print(sb, t, 'Y', l);
                    break;
                case 'd':
                case 'e':
                    Flags flags;
                    i = t.get(ChronoField.DAY_OF_MONTH);
                    if (c == 'd') {
                        flags = Flags.ZERO_PAD;
                    } else {
                        flags = Flags.NONE;
                    }
                    sb.append(localizedMagnitude(null, (long) i, flags, 2, l));
                    break;
                case 'j':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.DAY_OF_YEAR), Flags.ZERO_PAD, 3, l));
                    break;
                case 'k':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.HOUR_OF_DAY), Flags.NONE, 2, l));
                    break;
                case 'l':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.CLOCK_HOUR_OF_AMPM), Flags.NONE, 2, l));
                    break;
                case 'm':
                    sb.append(localizedMagnitude(null, (long) t.get(ChronoField.MONTH_OF_YEAR), Flags.ZERO_PAD, 2, l));
                    break;
                case 'p':
                    String[] ampm = new String[]{"AM", "PM"};
                    if (!(l == null || l == Locale.US)) {
                        ampm = DateFormatSymbols.getInstance(l).getAmPmStrings();
                    }
                    sb.append(ampm[t.get(ChronoField.AMPM_OF_DAY)].toLowerCase(l != null ? l : Locale.US));
                    break;
                case 'r':
                    print(sb, t, 'I', l).append(':');
                    print(sb, t, 'M', l).append(':');
                    print(sb, t, 'S', l).append(' ');
                    StringBuilder tsb = new StringBuilder();
                    print(tsb, t, 'p', l);
                    sb.append(tsb.toString().toUpperCase(l != null ? l : Locale.US));
                    break;
                case 's':
                    sb.append(localizedMagnitude(null, t.getLong(ChronoField.INSTANT_SECONDS), Flags.NONE, this.width, l));
                    break;
                case 'z':
                    try {
                        i = t.get(ChronoField.OFFSET_SECONDS);
                        if (i < 0) {
                            z = true;
                        }
                        boolean neg = z;
                        sb.append(neg ? '-' : '+');
                        if (neg) {
                            i = -i;
                        }
                        size = i / 60;
                        sb.append(localizedMagnitude(null, (long) (((size / 60) * 100) + (size % 60)), Flags.ZERO_PAD, 4, l));
                        break;
                    } catch (DateTimeException e) {
                        throw new IllegalFormatConversionException(c, t.getClass());
                    }
            }
            return sb;
        }

        private void failMismatch(Flags f, char c) {
            throw new FormatFlagsConversionMismatchException(f.toString(), c);
        }

        private void failConversion(char c, Object arg) {
            throw new IllegalFormatConversionException(c, arg.getClass());
        }

        private char getZero(Locale l) {
            if (l == null || l.equals(Formatter.this.locale())) {
                return Formatter.this.zero;
            }
            return DecimalFormatSymbols.getInstance(l).getZeroDigit();
        }

        private StringBuilder localizedMagnitude(StringBuilder sb, long value, Flags f, int width, Locale l) {
            return localizedMagnitude(sb, Long.toString(value, 10).toCharArray(), f, width, l);
        }

        private StringBuilder localizedMagnitude(StringBuilder sb, char[] value, Flags f, int width, Locale l) {
            StringBuilder sb2;
            char[] cArr = value;
            Flags flags = f;
            int i = width;
            Locale locale = l;
            if (sb == null) {
                sb2 = new StringBuilder();
            } else {
                sb2 = sb;
            }
            int begin = sb2.length();
            char zero = getZero(locale);
            char grpSep = 0;
            int grpSize = -1;
            char decSep = 0;
            int len = cArr.length;
            int dot = len;
            for (int j = 0; j < len; j++) {
                if (cArr[j] == '.') {
                    dot = j;
                    break;
                }
            }
            if (dot < len) {
                if (locale == null || locale.equals(Locale.US)) {
                    decSep = '.';
                } else {
                    decSep = DecimalFormatSymbols.getInstance(l).getDecimalSeparator();
                }
            }
            if (flags.contains(Flags.GROUP)) {
                if (locale == null || locale.equals(Locale.US)) {
                    grpSep = ',';
                    grpSize = 3;
                } else {
                    grpSep = DecimalFormatSymbols.getInstance(l).getGroupingSeparator();
                    DecimalFormat df = (DecimalFormat) NumberFormat.getIntegerInstance(l);
                    grpSize = df.getGroupingSize();
                    if (!df.isGroupingUsed() || df.getGroupingSize() == 0) {
                        grpSep = 0;
                    }
                }
            }
            char grpSep2 = grpSep;
            int j2 = 0;
            while (j2 < len) {
                if (j2 == dot) {
                    sb2.append(decSep);
                    grpSep2 = 0;
                } else {
                    sb2.append((char) ((cArr[j2] - 48) + zero));
                    if (!(grpSep2 == 0 || j2 == dot - 1 || (dot - j2) % grpSize != 1)) {
                        sb2.append(grpSep2);
                    }
                }
                j2++;
                cArr = value;
            }
            int len2 = sb2.length();
            if (i != -1 && flags.contains(Flags.ZERO_PAD)) {
                int k = 0;
                while (true) {
                    j2 = k;
                    if (j2 >= i - len2) {
                        break;
                    }
                    sb2.insert(begin, zero);
                    k = j2 + 1;
                }
            }
            return sb2;
        }
    }

    public enum BigDecimalLayoutForm {
        SCIENTIFIC,
        DECIMAL_FLOAT
    }

    private static Charset toCharset(String csn) throws UnsupportedEncodingException {
        Objects.requireNonNull((Object) csn, "charsetName");
        try {
            return Charset.forName(csn);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(csn);
        }
    }

    private static final Appendable nonNullAppendable(Appendable a) {
        if (a == null) {
            return new StringBuilder();
        }
        return a;
    }

    private Formatter(Locale l, Appendable a) {
        this.a = a;
        this.l = l;
        this.zero = getZero(l);
    }

    private Formatter(Charset charset, Locale l, File file) throws FileNotFoundException {
        this(l, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset)));
    }

    public Formatter() {
        this(Locale.getDefault(Category.FORMAT), new StringBuilder());
    }

    public Formatter(Appendable a) {
        this(Locale.getDefault(Category.FORMAT), nonNullAppendable(a));
    }

    public Formatter(Locale l) {
        this(l, new StringBuilder());
    }

    public Formatter(Appendable a, Locale l) {
        this(l, nonNullAppendable(a));
    }

    public Formatter(String fileName) throws FileNotFoundException {
        this(Locale.getDefault(Category.FORMAT), new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))));
    }

    public Formatter(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        this(fileName, csn, Locale.getDefault(Category.FORMAT));
    }

    public Formatter(String fileName, String csn, Locale l) throws FileNotFoundException, UnsupportedEncodingException {
        this(toCharset(csn), l, new File(fileName));
    }

    public Formatter(File file) throws FileNotFoundException {
        this(Locale.getDefault(Category.FORMAT), new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))));
    }

    public Formatter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        this(file, csn, Locale.getDefault(Category.FORMAT));
    }

    public Formatter(File file, String csn, Locale l) throws FileNotFoundException, UnsupportedEncodingException {
        this(toCharset(csn), l, file);
    }

    public Formatter(PrintStream ps) {
        this(Locale.getDefault(Category.FORMAT), (Appendable) Objects.requireNonNull(ps));
    }

    public Formatter(OutputStream os) {
        this(Locale.getDefault(Category.FORMAT), new BufferedWriter(new OutputStreamWriter(os)));
    }

    public Formatter(OutputStream os, String csn) throws UnsupportedEncodingException {
        this(os, csn, Locale.getDefault(Category.FORMAT));
    }

    public Formatter(OutputStream os, String csn, Locale l) throws UnsupportedEncodingException {
        this(l, new BufferedWriter(new OutputStreamWriter(os, csn)));
    }

    private static char getZero(Locale l) {
        if (l == null || l.equals(Locale.US)) {
            return '0';
        }
        return DecimalFormatSymbols.getInstance(l).getZeroDigit();
    }

    public Locale locale() {
        ensureOpen();
        return this.l;
    }

    public Appendable out() {
        ensureOpen();
        return this.a;
    }

    public String toString() {
        ensureOpen();
        return this.a.toString();
    }

    public void flush() {
        ensureOpen();
        if (this.a instanceof Flushable) {
            try {
                ((Flushable) this.a).flush();
            } catch (IOException ioe) {
                this.lastException = ioe;
            }
        }
    }

    public void close() {
        if (this.a != null) {
            try {
                if (this.a instanceof Closeable) {
                    ((Closeable) this.a).close();
                }
            } catch (IOException ioe) {
                this.lastException = ioe;
            } catch (Throwable th) {
                this.a = null;
            }
            this.a = null;
        }
    }

    private void ensureOpen() {
        if (this.a == null) {
            throw new FormatterClosedException();
        }
    }

    public IOException ioException() {
        return this.lastException;
    }

    public Formatter format(String format, Object... args) {
        return format(this.l, format, args);
    }

    public Formatter format(Locale l, String format, Object... args) {
        ensureOpen();
        int last = -1;
        int lasto = -1;
        FormatString[] fsa = parse(format);
        for (FormatString fs : fsa) {
            int index = fs.index();
            IOException x = null;
            Object x2;
            switch (index) {
                case -2:
                    fs.print(null, l);
                    break;
                case -1:
                    if (last >= 0 && (args == null || last <= args.length - 1)) {
                        if (args != null) {
                            x2 = args[last];
                        }
                        fs.print(x2, l);
                        break;
                    }
                    throw new MissingFormatArgumentException(fs.toString());
                case 0:
                    lasto++;
                    last = lasto;
                    if (args != null) {
                        if (lasto > args.length - 1) {
                            throw new MissingFormatArgumentException(fs.toString());
                        }
                    }
                    if (args != null) {
                        x2 = args[lasto];
                    }
                    fs.print(x2, l);
                    break;
                default:
                    last = index - 1;
                    if (args != null) {
                        try {
                            if (last > args.length - 1) {
                                throw new MissingFormatArgumentException(fs.toString());
                            }
                        } catch (IOException x3) {
                            this.lastException = x3;
                            break;
                        }
                    }
                    if (args != null) {
                        x3 = args[last];
                    }
                    fs.print(x3, l);
                    break;
            }
        }
        return this;
    }

    private FormatString[] parse(String s) {
        ArrayList<FormatString> al = new ArrayList();
        int i = 0;
        int len = s.length();
        while (i < len) {
            int nextPercent = s.indexOf(37, i);
            if (s.charAt(i) != '%') {
                int plainTextStart = i;
                int plainTextEnd = nextPercent == -1 ? len : nextPercent;
                al.add(new FixedString(s.substring(plainTextStart, plainTextEnd)));
                i = plainTextEnd;
            } else {
                FormatSpecifierParser fsp = new FormatSpecifierParser(s, i + 1);
                al.add(fsp.getFormatSpecifier());
                i = fsp.getEndIdx();
            }
        }
        return (FormatString[]) al.toArray(new FormatString[al.size()]);
    }
}
