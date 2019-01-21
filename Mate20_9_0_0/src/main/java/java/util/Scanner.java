package java.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale.Category;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.misc.LRUCache;
import sun.util.locale.LanguageTag;

public final class Scanner implements Iterator<String>, Closeable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String BOOLEAN_PATTERN = "true|false";
    private static final int BUFFER_SIZE = 1024;
    private static Pattern FIND_ANY_PATTERN = Pattern.compile("(?s).*");
    private static final String LINE_PATTERN = ".*(\r\n|[\n\r  ])|.+$";
    private static final String LINE_SEPARATOR_PATTERN = "\r\n|[\n\r  ]";
    private static Pattern NON_ASCII_DIGIT = Pattern.compile("[\\p{javaDigit}&&[^0-9]]");
    private static Pattern WHITESPACE_PATTERN = Pattern.compile("\\p{javaWhitespace}+");
    private static volatile Pattern boolPattern;
    private static volatile Pattern linePattern;
    private static volatile Pattern separatorPattern;
    private int SIMPLE_GROUP_INDEX;
    private CharBuffer buf;
    private boolean closed;
    private Pattern decimalPattern;
    private String decimalSeparator;
    private int defaultRadix;
    private Pattern delimPattern;
    private String digits;
    private Pattern floatPattern;
    private String groupSeparator;
    private Pattern hasNextPattern;
    private int hasNextPosition;
    private String hasNextResult;
    private String infinityString;
    private Pattern integerPattern;
    private IOException lastException;
    private Locale locale;
    private boolean matchValid;
    private Matcher matcher;
    private String nanString;
    private boolean needInput;
    private String negativePrefix;
    private String negativeSuffix;
    private String non0Digit;
    private LRUCache<String, Pattern> patternCache;
    private int position;
    private String positivePrefix;
    private String positiveSuffix;
    private int radix;
    private int savedScannerPosition;
    private boolean skipped;
    private Readable source;
    private boolean sourceClosed;
    private Object typeCache;

    private static Pattern boolPattern() {
        Pattern bp = boolPattern;
        if (bp != null) {
            return bp;
        }
        Pattern compile = Pattern.compile(BOOLEAN_PATTERN, 2);
        bp = compile;
        boolPattern = compile;
        return bp;
    }

    private String buildIntegerPatternString() {
        String radixDigits = this.digits.substring(0, this.radix);
        String nonZeroRadixDigits = new StringBuilder();
        nonZeroRadixDigits.append("((?i)[");
        nonZeroRadixDigits.append(this.digits.substring(1, this.radix));
        nonZeroRadixDigits.append("]|(");
        nonZeroRadixDigits.append(this.non0Digit);
        nonZeroRadixDigits.append("))");
        nonZeroRadixDigits = nonZeroRadixDigits.toString();
        String digit = new StringBuilder();
        digit.append("((?i)[");
        digit.append(radixDigits);
        digit.append("]|\\p{javaDigit})");
        digit = digit.toString();
        String groupedNumeral = new StringBuilder();
        groupedNumeral.append("(");
        groupedNumeral.append(nonZeroRadixDigits);
        groupedNumeral.append(digit);
        groupedNumeral.append("?");
        groupedNumeral.append(digit);
        groupedNumeral.append("?(");
        groupedNumeral.append(this.groupSeparator);
        groupedNumeral.append(digit);
        groupedNumeral.append(digit);
        groupedNumeral.append(digit);
        groupedNumeral.append(")+)");
        groupedNumeral = groupedNumeral.toString();
        String numeral = new StringBuilder();
        numeral.append("((");
        numeral.append(digit);
        numeral.append("++)|");
        numeral.append(groupedNumeral);
        numeral.append(")");
        numeral = numeral.toString();
        String javaStyleInteger = new StringBuilder();
        javaStyleInteger.append("([-+]?(");
        javaStyleInteger.append(numeral);
        javaStyleInteger.append("))");
        javaStyleInteger = javaStyleInteger.toString();
        String negativeInteger = new StringBuilder();
        negativeInteger.append(this.negativePrefix);
        negativeInteger.append(numeral);
        negativeInteger.append(this.negativeSuffix);
        negativeInteger = negativeInteger.toString();
        String positiveInteger = new StringBuilder();
        positiveInteger.append(this.positivePrefix);
        positiveInteger.append(numeral);
        positiveInteger.append(this.positiveSuffix);
        positiveInteger = positiveInteger.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        stringBuilder.append(javaStyleInteger);
        stringBuilder.append(")|(");
        stringBuilder.append(positiveInteger);
        stringBuilder.append(")|(");
        stringBuilder.append(negativeInteger);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private Pattern integerPattern() {
        if (this.integerPattern == null) {
            this.integerPattern = (Pattern) this.patternCache.forName(buildIntegerPatternString());
        }
        return this.integerPattern;
    }

    private static Pattern separatorPattern() {
        Pattern sp = separatorPattern;
        if (sp != null) {
            return sp;
        }
        Pattern compile = Pattern.compile(LINE_SEPARATOR_PATTERN);
        sp = compile;
        separatorPattern = compile;
        return sp;
    }

    private static Pattern linePattern() {
        Pattern lp = linePattern;
        if (lp != null) {
            return lp;
        }
        Pattern compile = Pattern.compile(LINE_PATTERN);
        lp = compile;
        linePattern = compile;
        return lp;
    }

    private void buildFloatAndDecimalPattern() {
        String digit = "([0-9]|(\\p{javaDigit}))";
        String exponent = new StringBuilder();
        exponent.append("([eE][+-]?");
        exponent.append(digit);
        exponent.append("+)?");
        exponent = exponent.toString();
        String groupedNumeral = new StringBuilder();
        groupedNumeral.append("(");
        groupedNumeral.append(this.non0Digit);
        groupedNumeral.append(digit);
        groupedNumeral.append("?");
        groupedNumeral.append(digit);
        groupedNumeral.append("?(");
        groupedNumeral.append(this.groupSeparator);
        groupedNumeral.append(digit);
        groupedNumeral.append(digit);
        groupedNumeral.append(digit);
        groupedNumeral.append(")+)");
        groupedNumeral = groupedNumeral.toString();
        String numeral = new StringBuilder();
        numeral.append("((");
        numeral.append(digit);
        numeral.append("++)|");
        numeral.append(groupedNumeral);
        numeral.append(")");
        numeral = numeral.toString();
        String decimalNumeral = new StringBuilder();
        decimalNumeral.append("(");
        decimalNumeral.append(numeral);
        decimalNumeral.append("|");
        decimalNumeral.append(numeral);
        decimalNumeral.append(this.decimalSeparator);
        decimalNumeral.append(digit);
        decimalNumeral.append("*+|");
        decimalNumeral.append(this.decimalSeparator);
        decimalNumeral.append(digit);
        decimalNumeral.append("++)");
        decimalNumeral = decimalNumeral.toString();
        String nonNumber = new StringBuilder();
        nonNumber.append("(NaN|");
        nonNumber.append(this.nanString);
        nonNumber.append("|Infinity|");
        nonNumber.append(this.infinityString);
        nonNumber.append(")");
        nonNumber = nonNumber.toString();
        String positiveFloat = new StringBuilder();
        positiveFloat.append("(");
        positiveFloat.append(this.positivePrefix);
        positiveFloat.append(decimalNumeral);
        positiveFloat.append(this.positiveSuffix);
        positiveFloat.append(exponent);
        positiveFloat.append(")");
        positiveFloat = positiveFloat.toString();
        String negativeFloat = new StringBuilder();
        negativeFloat.append("(");
        negativeFloat.append(this.negativePrefix);
        negativeFloat.append(decimalNumeral);
        negativeFloat.append(this.negativeSuffix);
        negativeFloat.append(exponent);
        negativeFloat.append(")");
        negativeFloat = negativeFloat.toString();
        String decimal = new StringBuilder();
        decimal.append("(([-+]?");
        decimal.append(decimalNumeral);
        decimal.append(exponent);
        decimal.append(")|");
        decimal.append(positiveFloat);
        decimal.append("|");
        decimal.append(negativeFloat);
        decimal.append(")");
        decimal = decimal.toString();
        String positiveNonNumber = new StringBuilder();
        positiveNonNumber.append("(");
        positiveNonNumber.append(this.positivePrefix);
        positiveNonNumber.append(nonNumber);
        positiveNonNumber.append(this.positiveSuffix);
        positiveNonNumber.append(")");
        positiveNonNumber = positiveNonNumber.toString();
        String negativeNonNumber = new StringBuilder();
        negativeNonNumber.append("(");
        negativeNonNumber.append(this.negativePrefix);
        negativeNonNumber.append(nonNumber);
        negativeNonNumber.append(this.negativeSuffix);
        negativeNonNumber.append(")");
        negativeNonNumber = negativeNonNumber.toString();
        String signedNonNumber = new StringBuilder();
        signedNonNumber.append("(([-+]?");
        signedNonNumber.append(nonNumber);
        signedNonNumber.append(")|");
        signedNonNumber.append(positiveNonNumber);
        signedNonNumber.append("|");
        signedNonNumber.append(negativeNonNumber);
        signedNonNumber.append(")");
        signedNonNumber = signedNonNumber.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(decimal);
        stringBuilder.append("|");
        stringBuilder.append("[-+]?0[xX][0-9a-fA-F]*\\.[0-9a-fA-F]+([pP][-+]?[0-9]+)?");
        stringBuilder.append("|");
        stringBuilder.append(signedNonNumber);
        this.floatPattern = Pattern.compile(stringBuilder.toString());
        this.decimalPattern = Pattern.compile(decimal);
    }

    private Pattern floatPattern() {
        if (this.floatPattern == null) {
            buildFloatAndDecimalPattern();
        }
        return this.floatPattern;
    }

    private Pattern decimalPattern() {
        if (this.decimalPattern == null) {
            buildFloatAndDecimalPattern();
        }
        return this.decimalPattern;
    }

    private Scanner(Readable source, Pattern pattern) {
        this.sourceClosed = $assertionsDisabled;
        this.needInput = $assertionsDisabled;
        this.skipped = $assertionsDisabled;
        this.savedScannerPosition = -1;
        this.typeCache = null;
        this.matchValid = $assertionsDisabled;
        this.closed = $assertionsDisabled;
        this.radix = 10;
        this.defaultRadix = 10;
        this.locale = null;
        this.patternCache = new LRUCache<String, Pattern>(7) {
            protected Pattern create(String s) {
                return Pattern.compile(s);
            }

            protected boolean hasName(Pattern p, String s) {
                return p.pattern().equals(s);
            }
        };
        this.groupSeparator = "\\,";
        this.decimalSeparator = "\\.";
        this.nanString = "NaN";
        this.infinityString = "Infinity";
        this.positivePrefix = "";
        this.negativePrefix = "\\-";
        this.positiveSuffix = "";
        this.negativeSuffix = "";
        this.digits = "0123456789abcdefghijklmnopqrstuvwxyz";
        this.non0Digit = "[\\p{javaDigit}&&[^0]]";
        this.SIMPLE_GROUP_INDEX = 5;
        this.source = source;
        this.delimPattern = pattern;
        this.buf = CharBuffer.allocate(1024);
        this.buf.limit(0);
        this.matcher = this.delimPattern.matcher(this.buf);
        this.matcher.useTransparentBounds(true);
        this.matcher.useAnchoringBounds($assertionsDisabled);
        useLocale(Locale.getDefault(Category.FORMAT));
    }

    public Scanner(Readable source) {
        this((Readable) Objects.requireNonNull((Object) source, "source"), WHITESPACE_PATTERN);
    }

    public Scanner(InputStream source) {
        this(new InputStreamReader(source), WHITESPACE_PATTERN);
    }

    public Scanner(InputStream source, String charsetName) {
        this(makeReadable((InputStream) Objects.requireNonNull((Object) source, "source"), toCharset(charsetName)), WHITESPACE_PATTERN);
    }

    private static Charset toCharset(String csn) {
        Objects.requireNonNull((Object) csn, "charsetName");
        try {
            return Charset.forName(csn);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Readable makeReadable(InputStream source, Charset charset) {
        return new InputStreamReader(source, charset);
    }

    public Scanner(File source) throws FileNotFoundException {
        this(new FileInputStream(source).getChannel());
    }

    public Scanner(File source, String charsetName) throws FileNotFoundException {
        this((File) Objects.requireNonNull(source), toDecoder(charsetName));
    }

    private Scanner(File source, CharsetDecoder dec) throws FileNotFoundException {
        this(makeReadable(new FileInputStream(source).getChannel(), dec));
    }

    private static CharsetDecoder toDecoder(String charsetName) {
        if (charsetName != null) {
            try {
                return Charset.forName(charsetName).newDecoder();
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                throw new IllegalArgumentException(charsetName);
            }
        }
        throw new IllegalArgumentException("charsetName == null");
    }

    private static Readable makeReadable(ReadableByteChannel source, CharsetDecoder dec) {
        return Channels.newReader(source, dec, -1);
    }

    public Scanner(Path source) throws IOException {
        this(Files.newInputStream(source, new OpenOption[0]));
    }

    public Scanner(Path source, String charsetName) throws IOException {
        this((Path) Objects.requireNonNull(source), toCharset(charsetName));
    }

    private Scanner(Path source, Charset charset) throws IOException {
        this(makeReadable(Files.newInputStream(source, new OpenOption[0]), charset));
    }

    public Scanner(String source) {
        this(new StringReader(source), WHITESPACE_PATTERN);
    }

    public Scanner(ReadableByteChannel source) {
        this(makeReadable((ReadableByteChannel) Objects.requireNonNull((Object) source, "source")), WHITESPACE_PATTERN);
    }

    private static Readable makeReadable(ReadableByteChannel source) {
        return makeReadable(source, Charset.defaultCharset().newDecoder());
    }

    public Scanner(ReadableByteChannel source, String charsetName) {
        this(makeReadable((ReadableByteChannel) Objects.requireNonNull((Object) source, "source"), toDecoder(charsetName)), WHITESPACE_PATTERN);
    }

    private void saveState() {
        this.savedScannerPosition = this.position;
    }

    private void revertState() {
        this.position = this.savedScannerPosition;
        this.savedScannerPosition = -1;
        this.skipped = $assertionsDisabled;
    }

    private boolean revertState(boolean b) {
        this.position = this.savedScannerPosition;
        this.savedScannerPosition = -1;
        this.skipped = $assertionsDisabled;
        return b;
    }

    private void cacheResult() {
        this.hasNextResult = this.matcher.group();
        this.hasNextPosition = this.matcher.end();
        this.hasNextPattern = this.matcher.pattern();
    }

    private void cacheResult(String result) {
        this.hasNextResult = result;
        this.hasNextPosition = this.matcher.end();
        this.hasNextPattern = this.matcher.pattern();
    }

    private void clearCaches() {
        this.hasNextPattern = null;
        this.typeCache = null;
    }

    private String getCachedResult() {
        this.position = this.hasNextPosition;
        this.hasNextPattern = null;
        this.typeCache = null;
        return this.hasNextResult;
    }

    private void useTypeCache() {
        if (this.closed) {
            throw new IllegalStateException("Scanner closed");
        }
        this.position = this.hasNextPosition;
        this.hasNextPattern = null;
        this.typeCache = null;
    }

    private void readInput() {
        if (this.buf.limit() == this.buf.capacity()) {
            makeSpace();
        }
        int p = this.buf.position();
        this.buf.position(this.buf.limit());
        this.buf.limit(this.buf.capacity());
        int n = 0;
        try {
            n = this.source.read(this.buf);
        } catch (IOException ioe) {
            this.lastException = ioe;
            n = -1;
        }
        if (n == -1) {
            this.sourceClosed = true;
            this.needInput = $assertionsDisabled;
        }
        if (n > 0) {
            this.needInput = $assertionsDisabled;
        }
        this.buf.limit(this.buf.position());
        this.buf.position(p);
        this.matcher.reset(this.buf);
    }

    private boolean makeSpace() {
        clearCaches();
        int offset = this.savedScannerPosition == -1 ? this.position : this.savedScannerPosition;
        this.buf.position(offset);
        if (offset > 0) {
            this.buf.compact();
            translateSavedIndexes(offset);
            this.position -= offset;
            this.buf.flip();
            return true;
        }
        CharBuffer newBuf = CharBuffer.allocate(this.buf.capacity() * 2);
        newBuf.put(this.buf);
        newBuf.flip();
        translateSavedIndexes(offset);
        this.position -= offset;
        this.buf = newBuf;
        this.matcher.reset(this.buf);
        return true;
    }

    private void translateSavedIndexes(int offset) {
        if (this.savedScannerPosition != -1) {
            this.savedScannerPosition -= offset;
        }
    }

    private void throwFor() {
        this.skipped = $assertionsDisabled;
        if (this.sourceClosed && this.position == this.buf.limit()) {
            throw new NoSuchElementException();
        }
        throw new InputMismatchException();
    }

    private boolean hasTokenInBuffer() {
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(this.delimPattern);
        this.matcher.region(this.position, this.buf.limit());
        if (this.matcher.lookingAt()) {
            this.position = this.matcher.end();
        }
        if (this.position == this.buf.limit()) {
            return $assertionsDisabled;
        }
        return true;
    }

    private String getCompleteTokenInBuffer(Pattern pattern) {
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(this.delimPattern);
        if (!this.skipped) {
            this.matcher.region(this.position, this.buf.limit());
            if (this.matcher.lookingAt()) {
                if (!this.matcher.hitEnd() || this.sourceClosed) {
                    this.skipped = true;
                    this.position = this.matcher.end();
                } else {
                    this.needInput = true;
                    return null;
                }
            }
        }
        if (this.position != this.buf.limit()) {
            this.matcher.region(this.position, this.buf.limit());
            boolean foundNextDelim = this.matcher.find();
            if (foundNextDelim && this.matcher.end() == this.position) {
                foundNextDelim = this.matcher.find();
            }
            if (foundNextDelim) {
                if (!this.matcher.requireEnd() || this.sourceClosed) {
                    int tokenEnd = this.matcher.start();
                    if (pattern == null) {
                        pattern = FIND_ANY_PATTERN;
                    }
                    this.matcher.usePattern(pattern);
                    this.matcher.region(this.position, tokenEnd);
                    if (!this.matcher.matches()) {
                        return null;
                    }
                    String s = this.matcher.group();
                    this.position = this.matcher.end();
                    return s;
                }
                this.needInput = true;
                return null;
            } else if (this.sourceClosed) {
                if (pattern == null) {
                    pattern = FIND_ANY_PATTERN;
                }
                this.matcher.usePattern(pattern);
                this.matcher.region(this.position, this.buf.limit());
                if (!this.matcher.matches()) {
                    return null;
                }
                String s2 = this.matcher.group();
                this.position = this.matcher.end();
                return s2;
            } else {
                this.needInput = true;
                return null;
            }
        } else if (this.sourceClosed) {
            return null;
        } else {
            this.needInput = true;
            return null;
        }
    }

    private String findPatternInBuffer(Pattern pattern, int horizon) {
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(pattern);
        int bufferLimit = this.buf.limit();
        int horizonLimit = -1;
        int searchLimit = bufferLimit;
        if (horizon > 0) {
            horizonLimit = this.position + horizon;
            if (horizonLimit < bufferLimit) {
                searchLimit = horizonLimit;
            }
        }
        this.matcher.region(this.position, searchLimit);
        if (this.matcher.find()) {
            if (this.matcher.hitEnd() && !this.sourceClosed) {
                if (searchLimit != horizonLimit) {
                    this.needInput = true;
                    return null;
                } else if (searchLimit == horizonLimit && this.matcher.requireEnd()) {
                    this.needInput = true;
                    return null;
                }
            }
            this.position = this.matcher.end();
            return this.matcher.group();
        } else if (this.sourceClosed) {
            return null;
        } else {
            if (horizon == 0 || searchLimit != horizonLimit) {
                this.needInput = true;
            }
            return null;
        }
    }

    private String matchPatternInBuffer(Pattern pattern) {
        this.matchValid = $assertionsDisabled;
        this.matcher.usePattern(pattern);
        this.matcher.region(this.position, this.buf.limit());
        if (this.matcher.lookingAt()) {
            if (!this.matcher.hitEnd() || this.sourceClosed) {
                this.position = this.matcher.end();
                return this.matcher.group();
            }
            this.needInput = true;
            return null;
        } else if (this.sourceClosed) {
            return null;
        } else {
            this.needInput = true;
            return null;
        }
    }

    private void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("Scanner closed");
        }
    }

    public void close() {
        if (!this.closed) {
            if (this.source instanceof Closeable) {
                try {
                    ((Closeable) this.source).close();
                } catch (IOException ioe) {
                    this.lastException = ioe;
                }
            }
            this.sourceClosed = true;
            this.source = null;
            this.closed = true;
        }
    }

    public IOException ioException() {
        return this.lastException;
    }

    public Pattern delimiter() {
        return this.delimPattern;
    }

    public Scanner useDelimiter(Pattern pattern) {
        this.delimPattern = pattern;
        return this;
    }

    public Scanner useDelimiter(String pattern) {
        this.delimPattern = (Pattern) this.patternCache.forName(pattern);
        return this;
    }

    public Locale locale() {
        return this.locale;
    }

    public Scanner useLocale(Locale locale) {
        if (locale.equals(this.locale)) {
            return this;
        }
        this.locale = locale;
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(locale);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\\");
        stringBuilder.append(dfs.getGroupingSeparator());
        this.groupSeparator = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("\\");
        stringBuilder.append(dfs.getDecimalSeparator());
        this.decimalSeparator = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("\\Q");
        stringBuilder.append(dfs.getNaN());
        stringBuilder.append("\\E");
        this.nanString = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("\\Q");
        stringBuilder.append(dfs.getInfinity());
        stringBuilder.append("\\E");
        this.infinityString = stringBuilder.toString();
        this.positivePrefix = df.getPositivePrefix();
        if (this.positivePrefix.length() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("\\Q");
            stringBuilder.append(this.positivePrefix);
            stringBuilder.append("\\E");
            this.positivePrefix = stringBuilder.toString();
        }
        this.negativePrefix = df.getNegativePrefix();
        if (this.negativePrefix.length() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("\\Q");
            stringBuilder.append(this.negativePrefix);
            stringBuilder.append("\\E");
            this.negativePrefix = stringBuilder.toString();
        }
        this.positiveSuffix = df.getPositiveSuffix();
        if (this.positiveSuffix.length() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("\\Q");
            stringBuilder.append(this.positiveSuffix);
            stringBuilder.append("\\E");
            this.positiveSuffix = stringBuilder.toString();
        }
        this.negativeSuffix = df.getNegativeSuffix();
        if (this.negativeSuffix.length() > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("\\Q");
            stringBuilder.append(this.negativeSuffix);
            stringBuilder.append("\\E");
            this.negativeSuffix = stringBuilder.toString();
        }
        this.integerPattern = null;
        this.floatPattern = null;
        return this;
    }

    public int radix() {
        return this.defaultRadix;
    }

    public Scanner useRadix(int radix) {
        if (radix < 2 || radix > 36) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("radix:");
            stringBuilder.append(radix);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.defaultRadix == radix) {
            return this;
        } else {
            this.defaultRadix = radix;
            this.integerPattern = null;
            return this;
        }
    }

    private void setRadix(int radix) {
        if (radix > 36) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("radix == ");
            stringBuilder.append(radix);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.radix != radix) {
            this.integerPattern = null;
            this.radix = radix;
        }
    }

    public MatchResult match() {
        if (this.matchValid) {
            return this.matcher.toMatchResult();
        }
        throw new IllegalStateException("No match result available");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.Scanner");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[delimiters=");
        stringBuilder.append(this.delimPattern);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[position=");
        stringBuilder.append(this.position);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[match valid=");
        stringBuilder.append(this.matchValid);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[need input=");
        stringBuilder.append(this.needInput);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[source closed=");
        stringBuilder.append(this.sourceClosed);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[skipped=");
        stringBuilder.append(this.skipped);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[group separator=");
        stringBuilder.append(this.groupSeparator);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[decimal separator=");
        stringBuilder.append(this.decimalSeparator);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[positive prefix=");
        stringBuilder.append(this.positivePrefix);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[negative prefix=");
        stringBuilder.append(this.negativePrefix);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[positive suffix=");
        stringBuilder.append(this.positiveSuffix);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[negative suffix=");
        stringBuilder.append(this.negativeSuffix);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[NaN string=");
        stringBuilder.append(this.nanString);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[infinity string=");
        stringBuilder.append(this.infinityString);
        stringBuilder.append("]");
        sb.append(stringBuilder.toString());
        return sb.toString();
    }

    public boolean hasNext() {
        ensureOpen();
        saveState();
        while (!this.sourceClosed) {
            if (hasTokenInBuffer()) {
                return revertState(true);
            }
            readInput();
        }
        return revertState(hasTokenInBuffer());
    }

    public String next() {
        ensureOpen();
        clearCaches();
        while (true) {
            String token = getCompleteTokenInBuffer(null);
            if (token != null) {
                this.matchValid = true;
                this.skipped = $assertionsDisabled;
                return token;
            } else if (this.needInput) {
                readInput();
            } else {
                throwFor();
            }
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext(String pattern) {
        return hasNext((Pattern) this.patternCache.forName(pattern));
    }

    public String next(String pattern) {
        return next((Pattern) this.patternCache.forName(pattern));
    }

    public boolean hasNext(Pattern pattern) {
        ensureOpen();
        if (pattern != null) {
            this.hasNextPattern = null;
            saveState();
            while (getCompleteTokenInBuffer(pattern) == null) {
                if (!this.needInput) {
                    return revertState($assertionsDisabled);
                }
                readInput();
            }
            this.matchValid = true;
            cacheResult();
            return revertState(true);
        }
        throw new NullPointerException();
    }

    public String next(Pattern pattern) {
        ensureOpen();
        if (pattern == null) {
            throw new NullPointerException();
        } else if (this.hasNextPattern == pattern) {
            return getCachedResult();
        } else {
            clearCaches();
            while (true) {
                String token = getCompleteTokenInBuffer(pattern);
                if (token != null) {
                    this.matchValid = true;
                    this.skipped = $assertionsDisabled;
                    return token;
                } else if (this.needInput) {
                    readInput();
                } else {
                    throwFor();
                }
            }
        }
    }

    public boolean hasNextLine() {
        saveState();
        String result = findWithinHorizon(linePattern(), 0);
        if (result != null) {
            String lineSep = match().group(1);
            if (lineSep != null) {
                result = result.substring(0, result.length() - lineSep.length());
                cacheResult(result);
            } else {
                cacheResult();
            }
        }
        revertState();
        if (result != null) {
            return true;
        }
        return $assertionsDisabled;
    }

    public String nextLine() {
        if (this.hasNextPattern == linePattern()) {
            return getCachedResult();
        }
        clearCaches();
        String result = findWithinHorizon(linePattern, 0);
        if (result != null) {
            String lineSep = match().group(1);
            if (lineSep != null) {
                result = result.substring(0, result.length() - lineSep.length());
            }
            if (result != null) {
                return result;
            }
            throw new NoSuchElementException();
        }
        throw new NoSuchElementException("No line found");
    }

    public String findInLine(String pattern) {
        return findInLine((Pattern) this.patternCache.forName(pattern));
    }

    public String findInLine(Pattern pattern) {
        ensureOpen();
        if (pattern != null) {
            int endPosition;
            clearCaches();
            saveState();
            while (findPatternInBuffer(separatorPattern(), 0) == null) {
                if (!this.needInput) {
                    endPosition = this.buf.limit();
                    break;
                }
                readInput();
            }
            endPosition = this.matcher.start();
            revertState();
            int horizonForLine = endPosition - this.position;
            if (horizonForLine == 0) {
                return null;
            }
            return findWithinHorizon(pattern, horizonForLine);
        }
        throw new NullPointerException();
    }

    public String findWithinHorizon(String pattern, int horizon) {
        return findWithinHorizon((Pattern) this.patternCache.forName(pattern), horizon);
    }

    public String findWithinHorizon(Pattern pattern, int horizon) {
        ensureOpen();
        if (pattern == null) {
            throw new NullPointerException();
        } else if (horizon >= 0) {
            clearCaches();
            while (true) {
                String token = findPatternInBuffer(pattern, horizon);
                if (token != null) {
                    this.matchValid = true;
                    return token;
                } else if (!this.needInput) {
                    return null;
                } else {
                    readInput();
                }
            }
        } else {
            throw new IllegalArgumentException("horizon < 0");
        }
    }

    public Scanner skip(Pattern pattern) {
        ensureOpen();
        if (pattern != null) {
            clearCaches();
            while (matchPatternInBuffer(pattern) == null) {
                if (this.needInput) {
                    readInput();
                } else {
                    throw new NoSuchElementException();
                }
            }
            this.matchValid = true;
            this.position = this.matcher.end();
            return this;
        }
        throw new NullPointerException();
    }

    public Scanner skip(String pattern) {
        return skip((Pattern) this.patternCache.forName(pattern));
    }

    public boolean hasNextBoolean() {
        return hasNext(boolPattern());
    }

    public boolean nextBoolean() {
        clearCaches();
        return Boolean.parseBoolean(next(boolPattern()));
    }

    public boolean hasNextByte() {
        return hasNextByte(this.defaultRadix);
    }

    public boolean hasNextByte(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (!result) {
            return result;
        }
        try {
            String s;
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(this.hasNextResult);
            } else {
                s = this.hasNextResult;
            }
            this.typeCache = Byte.valueOf(Byte.parseByte(s, radix));
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    public byte nextByte() {
        return nextByte(this.defaultRadix);
    }

    public byte nextByte(int radix) {
        if (this.typeCache != null && (this.typeCache instanceof Byte) && this.radix == radix) {
            byte val = ((Byte) this.typeCache).byteValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        try {
            String s = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(s);
            }
            return Byte.parseByte(s, radix);
        } catch (NumberFormatException nfe) {
            this.position = this.matcher.start();
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    public boolean hasNextShort() {
        return hasNextShort(this.defaultRadix);
    }

    public boolean hasNextShort(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (!result) {
            return result;
        }
        try {
            String s;
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(this.hasNextResult);
            } else {
                s = this.hasNextResult;
            }
            this.typeCache = Short.valueOf(Short.parseShort(s, radix));
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    public short nextShort() {
        return nextShort(this.defaultRadix);
    }

    public short nextShort(int radix) {
        if (this.typeCache != null && (this.typeCache instanceof Short) && this.radix == radix) {
            short val = ((Short) this.typeCache).shortValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        try {
            String s = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(s);
            }
            return Short.parseShort(s, radix);
        } catch (NumberFormatException nfe) {
            this.position = this.matcher.start();
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    public boolean hasNextInt() {
        return hasNextInt(this.defaultRadix);
    }

    public boolean hasNextInt(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (!result) {
            return result;
        }
        try {
            String s;
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(this.hasNextResult);
            } else {
                s = this.hasNextResult;
            }
            this.typeCache = Integer.valueOf(Integer.parseInt(s, radix));
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    private String processIntegerToken(String token) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.groupSeparator);
        String result = token.replaceAll(stringBuilder.toString(), "");
        boolean isNegative = $assertionsDisabled;
        int preLen = this.negativePrefix.length();
        if (preLen > 0 && result.startsWith(this.negativePrefix)) {
            isNegative = true;
            result = result.substring(preLen);
        }
        int sufLen = this.negativeSuffix.length();
        if (sufLen > 0 && result.endsWith(this.negativeSuffix)) {
            isNegative = true;
            result = result.substring(result.length() - sufLen, result.length());
        }
        if (!isNegative) {
            return result;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(LanguageTag.SEP);
        stringBuilder2.append(result);
        return stringBuilder2.toString();
    }

    public int nextInt() {
        return nextInt(this.defaultRadix);
    }

    public int nextInt(int radix) {
        if (this.typeCache != null && (this.typeCache instanceof Integer) && this.radix == radix) {
            int val = ((Integer) this.typeCache).intValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        try {
            String s = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(s);
            }
            return Integer.parseInt(s, radix);
        } catch (NumberFormatException nfe) {
            this.position = this.matcher.start();
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    public boolean hasNextLong() {
        return hasNextLong(this.defaultRadix);
    }

    public boolean hasNextLong(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (!result) {
            return result;
        }
        try {
            String s;
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(this.hasNextResult);
            } else {
                s = this.hasNextResult;
            }
            this.typeCache = Long.valueOf(Long.parseLong(s, radix));
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    public long nextLong() {
        return nextLong(this.defaultRadix);
    }

    public long nextLong(int radix) {
        if (this.typeCache != null && (this.typeCache instanceof Long) && this.radix == radix) {
            long val = ((Long) this.typeCache).longValue();
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        try {
            String s = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(s);
            }
            return Long.parseLong(s, radix);
        } catch (NumberFormatException nfe) {
            this.position = this.matcher.start();
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    private String processFloatToken(String token) {
        String result = token.replaceAll(this.groupSeparator, "");
        if (!this.decimalSeparator.equals("\\.")) {
            result = result.replaceAll(this.decimalSeparator, ".");
        }
        boolean isNegative = $assertionsDisabled;
        int preLen = this.negativePrefix.length();
        if (preLen > 0 && result.startsWith(this.negativePrefix)) {
            isNegative = true;
            result = result.substring(preLen);
        }
        int sufLen = this.negativeSuffix.length();
        if (sufLen > 0 && result.endsWith(this.negativeSuffix)) {
            isNegative = true;
            result = result.substring(result.length() - sufLen, result.length());
        }
        if (result.equals(this.nanString)) {
            result = "NaN";
        }
        if (result.equals(this.infinityString)) {
            result = "Infinity";
        }
        if (result.equals("∞")) {
            result = "Infinity";
        }
        if (isNegative) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(LanguageTag.SEP);
            stringBuilder.append(result);
            result = stringBuilder.toString();
        }
        if (!NON_ASCII_DIGIT.matcher(result).find()) {
            return result;
        }
        StringBuilder inASCII = new StringBuilder();
        for (int i = 0; i < result.length(); i++) {
            char nextChar = result.charAt(i);
            if (Character.isDigit(nextChar)) {
                int d = Character.digit(nextChar, 10);
                if (d != -1) {
                    inASCII.append(d);
                } else {
                    inASCII.append(nextChar);
                }
            } else {
                inASCII.append(nextChar);
            }
        }
        return inASCII.toString();
    }

    public boolean hasNextFloat() {
        setRadix(10);
        boolean result = hasNext(floatPattern());
        if (!result) {
            return result;
        }
        try {
            this.typeCache = Float.valueOf(Float.parseFloat(processFloatToken(this.hasNextResult)));
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    public float nextFloat() {
        if (this.typeCache == null || !(this.typeCache instanceof Float)) {
            setRadix(10);
            clearCaches();
            try {
                return Float.parseFloat(processFloatToken(next(floatPattern())));
            } catch (NumberFormatException nfe) {
                this.position = this.matcher.start();
                throw new InputMismatchException(nfe.getMessage());
            }
        }
        float val = ((Float) this.typeCache).floatValue();
        useTypeCache();
        return val;
    }

    public boolean hasNextDouble() {
        setRadix(10);
        boolean result = hasNext(floatPattern());
        if (!result) {
            return result;
        }
        try {
            this.typeCache = Double.valueOf(Double.parseDouble(processFloatToken(this.hasNextResult)));
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    public double nextDouble() {
        if (this.typeCache == null || !(this.typeCache instanceof Double)) {
            setRadix(10);
            clearCaches();
            try {
                return Double.parseDouble(processFloatToken(next(floatPattern())));
            } catch (NumberFormatException nfe) {
                this.position = this.matcher.start();
                throw new InputMismatchException(nfe.getMessage());
            }
        }
        double val = ((Double) this.typeCache).doubleValue();
        useTypeCache();
        return val;
    }

    public boolean hasNextBigInteger() {
        return hasNextBigInteger(this.defaultRadix);
    }

    public boolean hasNextBigInteger(int radix) {
        setRadix(radix);
        boolean result = hasNext(integerPattern());
        if (!result) {
            return result;
        }
        try {
            String s;
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(this.hasNextResult);
            } else {
                s = this.hasNextResult;
            }
            this.typeCache = new BigInteger(s, radix);
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    public BigInteger nextBigInteger() {
        return nextBigInteger(this.defaultRadix);
    }

    public BigInteger nextBigInteger(int radix) {
        if (this.typeCache != null && (this.typeCache instanceof BigInteger) && this.radix == radix) {
            BigInteger val = this.typeCache;
            useTypeCache();
            return val;
        }
        setRadix(radix);
        clearCaches();
        try {
            String s = next(integerPattern());
            if (this.matcher.group(this.SIMPLE_GROUP_INDEX) == null) {
                s = processIntegerToken(s);
            }
            return new BigInteger(s, radix);
        } catch (NumberFormatException nfe) {
            this.position = this.matcher.start();
            throw new InputMismatchException(nfe.getMessage());
        }
    }

    public boolean hasNextBigDecimal() {
        setRadix(10);
        boolean result = hasNext(decimalPattern());
        if (!result) {
            return result;
        }
        try {
            this.typeCache = new BigDecimal(processFloatToken(this.hasNextResult));
            return result;
        } catch (NumberFormatException e) {
            return $assertionsDisabled;
        }
    }

    public BigDecimal nextBigDecimal() {
        if (this.typeCache == null || !(this.typeCache instanceof BigDecimal)) {
            setRadix(10);
            clearCaches();
            try {
                return new BigDecimal(processFloatToken(next(decimalPattern())));
            } catch (NumberFormatException nfe) {
                this.position = this.matcher.start();
                throw new InputMismatchException(nfe.getMessage());
            }
        }
        BigDecimal val = this.typeCache;
        useTypeCache();
        return val;
    }

    public Scanner reset() {
        this.delimPattern = WHITESPACE_PATTERN;
        useLocale(Locale.getDefault(Category.FORMAT));
        useRadix(10);
        clearCaches();
        return this;
    }
}
