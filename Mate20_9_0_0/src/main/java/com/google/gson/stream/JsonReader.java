package com.google.gson.stream;

import com.android.server.hidata.wavemapping.cons.WMStateCons;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.internal.bind.JsonTreeReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.TagID;

public class JsonReader implements Closeable {
    private static final long MIN_INCOMPLETE_INTEGER = -922337203685477580L;
    private static final char[] NON_EXECUTE_PREFIX = ")]}'\n".toCharArray();
    private static final int NUMBER_CHAR_DECIMAL = 3;
    private static final int NUMBER_CHAR_DIGIT = 2;
    private static final int NUMBER_CHAR_EXP_DIGIT = 7;
    private static final int NUMBER_CHAR_EXP_E = 5;
    private static final int NUMBER_CHAR_EXP_SIGN = 6;
    private static final int NUMBER_CHAR_FRACTION_DIGIT = 4;
    private static final int NUMBER_CHAR_NONE = 0;
    private static final int NUMBER_CHAR_SIGN = 1;
    private static final int PEEKED_BEGIN_ARRAY = 3;
    private static final int PEEKED_BEGIN_OBJECT = 1;
    private static final int PEEKED_BUFFERED = 11;
    private static final int PEEKED_DOUBLE_QUOTED = 9;
    private static final int PEEKED_DOUBLE_QUOTED_NAME = 13;
    private static final int PEEKED_END_ARRAY = 4;
    private static final int PEEKED_END_OBJECT = 2;
    private static final int PEEKED_EOF = 17;
    private static final int PEEKED_FALSE = 6;
    private static final int PEEKED_LONG = 15;
    private static final int PEEKED_NONE = 0;
    private static final int PEEKED_NULL = 7;
    private static final int PEEKED_NUMBER = 16;
    private static final int PEEKED_SINGLE_QUOTED = 8;
    private static final int PEEKED_SINGLE_QUOTED_NAME = 12;
    private static final int PEEKED_TRUE = 5;
    private static final int PEEKED_UNQUOTED = 10;
    private static final int PEEKED_UNQUOTED_NAME = 14;
    private final char[] buffer = new char[1024];
    private final Reader in;
    private boolean lenient = false;
    private int limit = 0;
    private int lineNumber = 0;
    private int lineStart = 0;
    private int[] pathIndices;
    private String[] pathNames;
    int peeked = 0;
    private long peekedLong;
    private int peekedNumberLength;
    private String peekedString;
    private int pos = 0;
    private int[] stack = new int[32];
    private int stackSize = 0;

    static {
        JsonReaderInternalAccess.INSTANCE = new JsonReaderInternalAccess() {
            public void promoteNameToValue(JsonReader reader) throws IOException {
                if (reader instanceof JsonTreeReader) {
                    ((JsonTreeReader) reader).promoteNameToValue();
                    return;
                }
                int p = reader.peeked;
                if (p == 0) {
                    p = reader.doPeek();
                }
                if (p == 13) {
                    reader.peeked = 9;
                } else if (p == 12) {
                    reader.peeked = 8;
                } else if (p == 14) {
                    reader.peeked = 10;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected a name but was ");
                    stringBuilder.append(reader.peek());
                    stringBuilder.append(reader.locationString());
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
        };
    }

    public JsonReader(Reader in) {
        int[] iArr = this.stack;
        int i = this.stackSize;
        this.stackSize = i + 1;
        iArr[i] = 6;
        this.pathNames = new String[32];
        this.pathIndices = new int[32];
        if (in != null) {
            this.in = in;
            return;
        }
        throw new NullPointerException("in == null");
    }

    public final void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public final boolean isLenient() {
        return this.lenient;
    }

    public void beginArray() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        if (p == 3) {
            push(1);
            this.pathIndices[this.stackSize - 1] = 0;
            this.peeked = 0;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected BEGIN_ARRAY but was ");
        stringBuilder.append(peek());
        stringBuilder.append(locationString());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void endArray() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        if (p == 4) {
            this.stackSize--;
            int[] iArr = this.pathIndices;
            int i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            this.peeked = 0;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected END_ARRAY but was ");
        stringBuilder.append(peek());
        stringBuilder.append(locationString());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void beginObject() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        if (p == 1) {
            push(3);
            this.peeked = 0;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected BEGIN_OBJECT but was ");
        stringBuilder.append(peek());
        stringBuilder.append(locationString());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void endObject() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        if (p == 2) {
            this.stackSize--;
            this.pathNames[this.stackSize] = null;
            int[] iArr = this.pathIndices;
            int i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            this.peeked = 0;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected END_OBJECT but was ");
        stringBuilder.append(peek());
        stringBuilder.append(locationString());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public boolean hasNext() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        return (p == 2 || p == 4) ? false : true;
    }

    public JsonToken peek() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        switch (p) {
            case 1:
                return JsonToken.BEGIN_OBJECT;
            case 2:
                return JsonToken.END_OBJECT;
            case 3:
                return JsonToken.BEGIN_ARRAY;
            case 4:
                return JsonToken.END_ARRAY;
            case 5:
            case 6:
                return JsonToken.BOOLEAN;
            case 7:
                return JsonToken.NULL;
            case 8:
            case 9:
            case 10:
            case 11:
                return JsonToken.STRING;
            case 12:
            case 13:
            case 14:
                return JsonToken.NAME;
            case 15:
            case 16:
                return JsonToken.NUMBER;
            case 17:
                return JsonToken.END_DOCUMENT;
            default:
                throw new AssertionError();
        }
    }

    int doPeek() throws IOException {
        int c;
        int peekStack = this.stack[this.stackSize - 1];
        if (peekStack == 1) {
            this.stack[this.stackSize - 1] = 2;
        } else if (peekStack == 2) {
            c = nextNonWhitespace(true);
            if (c != 44) {
                if (c == 59) {
                    checkLenient();
                } else if (c == 93) {
                    this.peeked = 4;
                    return 4;
                } else {
                    throw syntaxError("Unterminated array");
                }
            }
        } else if (peekStack == 3 || peekStack == 5) {
            int c2;
            this.stack[this.stackSize - 1] = 4;
            if (peekStack == 5) {
                c2 = nextNonWhitespace(true);
                if (c2 != 44) {
                    if (c2 == 59) {
                        checkLenient();
                    } else if (c2 == CPUFeature.MSG_SET_CPUSETCONFIG_VR) {
                        this.peeked = 2;
                        return 2;
                    } else {
                        throw syntaxError("Unterminated object");
                    }
                }
            }
            c2 = nextNonWhitespace(true);
            if (c2 == 34) {
                this.peeked = 13;
                return 13;
            } else if (c2 == 39) {
                checkLenient();
                this.peeked = 12;
                return 12;
            } else if (c2 != CPUFeature.MSG_SET_CPUSETCONFIG_VR) {
                checkLenient();
                this.pos--;
                if (isLiteral((char) c2)) {
                    this.peeked = 14;
                    return 14;
                }
                throw syntaxError("Expected name");
            } else if (peekStack != 5) {
                this.peeked = 2;
                return 2;
            } else {
                throw syntaxError("Expected name");
            }
        } else if (peekStack == 4) {
            this.stack[this.stackSize - 1] = 5;
            c = nextNonWhitespace(true);
            if (c != 58) {
                if (c == 61) {
                    checkLenient();
                    if ((this.pos < this.limit || fillBuffer(1)) && this.buffer[this.pos] == '>') {
                        this.pos++;
                    }
                } else {
                    throw syntaxError("Expected ':'");
                }
            }
        } else if (peekStack == 6) {
            if (this.lenient) {
                consumeNonExecutePrefix();
            }
            this.stack[this.stackSize - 1] = 7;
        } else if (peekStack == 7) {
            if (nextNonWhitespace(false) == -1) {
                this.peeked = 17;
                return 17;
            }
            checkLenient();
            this.pos--;
        } else if (peekStack == 8) {
            throw new IllegalStateException("JsonReader is closed");
        }
        c = nextNonWhitespace(true);
        if (c == 34) {
            this.peeked = 9;
            return 9;
        } else if (c != 39) {
            if (!(c == 44 || c == 59)) {
                if (c == 91) {
                    this.peeked = 3;
                    return 3;
                } else if (c != 93) {
                    if (c != 123) {
                        this.pos--;
                        int result = peekKeyword();
                        if (result != 0) {
                            return result;
                        }
                        result = peekNumber();
                        if (result != 0) {
                            return result;
                        }
                        if (isLiteral(this.buffer[this.pos])) {
                            checkLenient();
                            this.peeked = 10;
                            return 10;
                        }
                        throw syntaxError("Expected value");
                    }
                    this.peeked = 1;
                    return 1;
                } else if (peekStack == 1) {
                    this.peeked = 4;
                    return 4;
                }
            }
            if (peekStack == 1 || peekStack == 2) {
                checkLenient();
                this.pos--;
                this.peeked = 7;
                return 7;
            }
            throw syntaxError("Unexpected value");
        } else {
            checkLenient();
            this.peeked = 8;
            return 8;
        }
    }

    private int peekKeyword() throws IOException {
        String keyword;
        int peeking;
        char c = this.buffer[this.pos];
        String keywordUpper;
        if (c == 't' || c == 'T') {
            keyword = "true";
            keywordUpper = "TRUE";
            peeking = 5;
        } else if (c == 'f' || c == 'F') {
            keyword = "false";
            keywordUpper = "FALSE";
            peeking = 6;
        } else if (c != 'n' && c != 'N') {
            return 0;
        } else {
            keyword = "null";
            keywordUpper = "NULL";
            peeking = 7;
        }
        int length = keyword.length();
        int i = 1;
        while (i < length) {
            if (this.pos + i >= this.limit && !fillBuffer(i + 1)) {
                return 0;
            }
            c = this.buffer[this.pos + i];
            if (c != keyword.charAt(i) && c != keywordUpper.charAt(i)) {
                return 0;
            }
            i++;
        }
        if ((this.pos + length < this.limit || fillBuffer(length + 1)) && isLiteral(this.buffer[this.pos + length])) {
            return 0;
        }
        this.pos += length;
        this.peeked = peeking;
        return peeking;
    }

    /* JADX WARNING: Missing block: B:82:0x00ff, code:
            r8 = r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int peekNumber() throws IOException {
        char c;
        char[] buffer = this.buffer;
        int p = this.pos;
        boolean negative = false;
        boolean fitsInLong = true;
        int last = 0;
        int i = 0;
        long value = 0;
        int l = this.limit;
        int p2 = p;
        p = 0;
        while (true) {
            int i2;
            int i3;
            if (p2 + p == l) {
                if (p == buffer.length) {
                    return i;
                }
                if (fillBuffer(p + 1)) {
                    p2 = this.pos;
                    l = this.limit;
                } else {
                    i2 = p2;
                    i3 = l;
                }
            }
            c = buffer[p2 + p];
            if (c != '+') {
                if (c != 'E' && c != 'e') {
                    switch (c) {
                        case '-':
                            i2 = p2;
                            i3 = l;
                            if (last == 0) {
                                l = 1;
                                negative = true;
                                break;
                            } else if (last == 5) {
                                p2 = 6;
                                break;
                            } else {
                                return 0;
                            }
                        case TagID.TAG_S3_SIMILARIT_COEFF /*46*/:
                            i2 = p2;
                            i3 = l;
                            if (last == 2) {
                                l = 3;
                                break;
                            }
                            return 0;
                        default:
                            if (c >= '0') {
                                if (c <= '9') {
                                    int i4 = 1;
                                    if (last != 1) {
                                        if (last != 0) {
                                            if (last == 2) {
                                                if (value != 0) {
                                                    i2 = p2;
                                                    i3 = l;
                                                    long newValue = (10 * value) - ((long) (c - 48));
                                                    if (value <= MIN_INCOMPLETE_INTEGER && (value != MIN_INCOMPLETE_INTEGER || newValue >= value)) {
                                                        i4 = 0;
                                                    }
                                                    fitsInLong &= i4;
                                                    value = newValue;
                                                    break;
                                                }
                                                return 0;
                                            }
                                            i2 = p2;
                                            i3 = l;
                                            if (last != 3) {
                                                if (last != 5 && last != 6) {
                                                    break;
                                                }
                                                p2 = 7;
                                                break;
                                            }
                                            p2 = 4;
                                            break;
                                        }
                                        i2 = p2;
                                        i3 = l;
                                    } else {
                                        i2 = p2;
                                        i3 = l;
                                    }
                                    last = 2;
                                    value = (long) (-(c - 48));
                                    break;
                                }
                                i2 = p2;
                                i3 = l;
                                break;
                            }
                            i3 = l;
                            break;
                            break;
                    }
                }
                i2 = p2;
                i3 = l;
                if (last != 2 && last != 4) {
                    return 0;
                }
                p2 = 5;
            } else {
                i2 = p2;
                i3 = l;
                if (last != 5) {
                    return 0;
                }
                p2 = 6;
            }
            last = p2;
            p++;
            p2 = i2;
            l = i3;
            i = 0;
        }
        if (isLiteral(c) != 0) {
            return 0;
        }
        if (last == 2 && fitsInLong && ((value != 0 || negative) && (value != 0 || !negative))) {
            this.peekedLong = negative ? value : -value;
            this.pos += p;
            this.peeked = 15;
            return 15;
        } else if (last != 2 && last != 4 && last != 7) {
            return 0;
        } else {
            this.peekedNumberLength = p;
            this.peeked = 16;
            return 16;
        }
    }

    private boolean isLiteral(char c) throws IOException {
        switch (c) {
            case 9:
            case 10:
            case 12:
            case 13:
            case ' ':
            case TagID.TAG_S3_WHITE_SIGMA45 /*44*/:
            case ':':
            case WMStateCons.MSG_CONNECTIVITY_CHANGE /*91*/:
            case WMStateCons.MSG_CELL_CHANGE /*93*/:
            case '{':
            case CPUFeature.MSG_SET_CPUSETCONFIG_VR /*125*/:
                break;
            case '#':
            case '/':
            case ';':
            case WMStateCons.MSG_WIFI_UPDATE_SCAN_RESULT /*61*/:
            case WMStateCons.MSG_SUPPLICANT_COMPLETE /*92*/:
                checkLenient();
                break;
            default:
                return true;
        }
        return false;
    }

    public String nextName() throws IOException {
        String result;
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        if (p == 14) {
            result = nextUnquotedValue();
        } else if (p == 12) {
            result = nextQuotedValue('\'');
        } else if (p == 13) {
            result = nextQuotedValue('\"');
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected a name but was ");
            stringBuilder.append(peek());
            stringBuilder.append(locationString());
            throw new IllegalStateException(stringBuilder.toString());
        }
        this.peeked = 0;
        this.pathNames[this.stackSize - 1] = result;
        return result;
    }

    public String nextString() throws IOException {
        String result;
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        if (p == 10) {
            result = nextUnquotedValue();
        } else if (p == 8) {
            result = nextQuotedValue('\'');
        } else if (p == 9) {
            result = nextQuotedValue('\"');
        } else if (p == 11) {
            result = this.peekedString;
            this.peekedString = null;
        } else if (p == 15) {
            result = Long.toString(this.peekedLong);
        } else if (p == 16) {
            result = new String(this.buffer, this.pos, this.peekedNumberLength);
            this.pos += this.peekedNumberLength;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected a string but was ");
            stringBuilder.append(peek());
            stringBuilder.append(locationString());
            throw new IllegalStateException(stringBuilder.toString());
        }
        this.peeked = 0;
        int[] iArr = this.pathIndices;
        int i = this.stackSize - 1;
        iArr[i] = iArr[i] + 1;
        return result;
    }

    public boolean nextBoolean() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        int[] iArr;
        if (p == 5) {
            this.peeked = 0;
            iArr = this.pathIndices;
            int i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            return true;
        } else if (p == 6) {
            this.peeked = 0;
            iArr = this.pathIndices;
            int i2 = this.stackSize - 1;
            iArr[i2] = iArr[i2] + 1;
            return false;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected a boolean but was ");
            stringBuilder.append(peek());
            stringBuilder.append(locationString());
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public void nextNull() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        if (p == 7) {
            this.peeked = 0;
            int[] iArr = this.pathIndices;
            int i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected null but was ");
        stringBuilder.append(peek());
        stringBuilder.append(locationString());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public double nextDouble() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        int[] iArr;
        int i;
        if (p == 15) {
            this.peeked = 0;
            iArr = this.pathIndices;
            i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            return (double) this.peekedLong;
        }
        StringBuilder stringBuilder;
        if (p == 16) {
            this.peekedString = new String(this.buffer, this.pos, this.peekedNumberLength);
            this.pos += this.peekedNumberLength;
        } else if (p == 8 || p == 9) {
            this.peekedString = nextQuotedValue(p == 8 ? '\'' : '\"');
        } else if (p == 10) {
            this.peekedString = nextUnquotedValue();
        } else if (p != 11) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Expected a double but was ");
            stringBuilder.append(peek());
            stringBuilder.append(locationString());
            throw new IllegalStateException(stringBuilder.toString());
        }
        this.peeked = 11;
        double result = Double.parseDouble(this.peekedString);
        if (this.lenient || !(Double.isNaN(result) || Double.isInfinite(result))) {
            this.peekedString = null;
            this.peeked = 0;
            iArr = this.pathIndices;
            i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            return result;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("JSON forbids NaN and infinities: ");
        stringBuilder.append(result);
        stringBuilder.append(locationString());
        throw new MalformedJsonException(stringBuilder.toString());
    }

    public long nextLong() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        int[] iArr;
        int i;
        if (p == 15) {
            this.peeked = 0;
            iArr = this.pathIndices;
            i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            return this.peekedLong;
        }
        StringBuilder stringBuilder;
        if (p == 16) {
            this.peekedString = new String(this.buffer, this.pos, this.peekedNumberLength);
            this.pos += this.peekedNumberLength;
        } else if (p == 8 || p == 9 || p == 10) {
            if (p == 10) {
                this.peekedString = nextUnquotedValue();
            } else {
                this.peekedString = nextQuotedValue(p == 8 ? '\'' : '\"');
            }
            try {
                long result = Long.parseLong(this.peekedString);
                this.peeked = 0;
                iArr = this.pathIndices;
                int i2 = this.stackSize - 1;
                iArr[i2] = iArr[i2] + 1;
                return result;
            } catch (NumberFormatException e) {
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Expected a long but was ");
            stringBuilder.append(peek());
            stringBuilder.append(locationString());
            throw new IllegalStateException(stringBuilder.toString());
        }
        this.peeked = 11;
        double asDouble = Double.parseDouble(this.peekedString);
        long result2 = (long) asDouble;
        if (((double) result2) == asDouble) {
            this.peekedString = null;
            this.peeked = 0;
            iArr = this.pathIndices;
            i = this.stackSize - 1;
            iArr[i] = iArr[i] + 1;
            return result2;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Expected a long but was ");
        stringBuilder.append(this.peekedString);
        stringBuilder.append(locationString());
        throw new NumberFormatException(stringBuilder.toString());
    }

    private String nextQuotedValue(char quote) throws IOException {
        char[] buffer = this.buffer;
        StringBuilder builder = null;
        while (true) {
            int start = this.pos;
            int l = this.limit;
            StringBuilder builder2 = builder;
            int p = start;
            while (p < l) {
                int p2 = p + 1;
                char c = buffer[p];
                int len;
                if (c == quote) {
                    this.pos = p2;
                    len = (p2 - start) - 1;
                    if (builder2 == null) {
                        return new String(buffer, start, len);
                    }
                    builder2.append(buffer, start, len);
                    return builder2.toString();
                } else if (c == '\\') {
                    this.pos = p2;
                    int len2 = (p2 - start) - 1;
                    if (builder2 == null) {
                        builder2 = new StringBuilder(Math.max((len2 + 1) * 2, 16));
                    }
                    builder2.append(buffer, start, len2);
                    builder2.append(readEscapeCharacter());
                    len = this.pos;
                    l = this.limit;
                    start = len;
                    p = len;
                } else {
                    if (c == 10) {
                        this.lineNumber++;
                        this.lineStart = p2;
                    }
                    p = p2;
                }
            }
            if (builder2 == null) {
                builder2 = new StringBuilder(Math.max((p - start) * 2, 16));
            }
            builder2.append(buffer, start, p - start);
            this.pos = p;
            if (fillBuffer(1)) {
                builder = builder2;
            } else {
                throw syntaxError("Unterminated string");
            }
        }
    }

    private String nextUnquotedValue() throws IOException {
        String result;
        StringBuilder builder = null;
        int i = 0;
        while (true) {
            if (this.pos + i < this.limit) {
                switch (this.buffer[this.pos + i]) {
                    case 9:
                    case 10:
                    case 12:
                    case 13:
                    case ' ':
                    case TagID.TAG_S3_WHITE_SIGMA45 /*44*/:
                    case ':':
                    case WMStateCons.MSG_CONNECTIVITY_CHANGE /*91*/:
                    case WMStateCons.MSG_CELL_CHANGE /*93*/:
                    case '{':
                    case CPUFeature.MSG_SET_CPUSETCONFIG_VR /*125*/:
                        break;
                    case '#':
                    case '/':
                    case ';':
                    case WMStateCons.MSG_WIFI_UPDATE_SCAN_RESULT /*61*/:
                    case WMStateCons.MSG_SUPPLICANT_COMPLETE /*92*/:
                        checkLenient();
                        break;
                    default:
                        i++;
                        continue;
                }
            } else if (i >= this.buffer.length) {
                if (builder == null) {
                    builder = new StringBuilder(Math.max(i, 16));
                }
                builder.append(this.buffer, this.pos, i);
                this.pos += i;
                i = 0;
                if (fillBuffer(1)) {
                }
            } else if (fillBuffer(i + 1)) {
            }
        }
        if (builder == null) {
            result = new String(this.buffer, this.pos, i);
        } else {
            builder.append(this.buffer, this.pos, i);
            result = builder.toString();
        }
        this.pos += i;
        return result;
    }

    private void skipQuotedValue(char quote) throws IOException {
        char[] buffer = this.buffer;
        while (true) {
            int p = this.pos;
            int l = this.limit;
            while (p < l) {
                int p2 = p + 1;
                char c = buffer[p];
                if (c == quote) {
                    this.pos = p2;
                    return;
                } else if (c == '\\') {
                    this.pos = p2;
                    readEscapeCharacter();
                    int p3 = this.pos;
                    l = this.limit;
                    p = p3;
                } else {
                    if (c == 10) {
                        this.lineNumber++;
                        this.lineStart = p2;
                    }
                    p = p2;
                }
            }
            this.pos = p;
            if (!fillBuffer(1)) {
                throw syntaxError("Unterminated string");
            }
        }
    }

    private void skipUnquotedValue() throws IOException {
        do {
            int i = 0;
            while (this.pos + i < this.limit) {
                switch (this.buffer[this.pos + i]) {
                    case 9:
                    case 10:
                    case 12:
                    case 13:
                    case ' ':
                    case TagID.TAG_S3_WHITE_SIGMA45 /*44*/:
                    case ':':
                    case WMStateCons.MSG_CONNECTIVITY_CHANGE /*91*/:
                    case WMStateCons.MSG_CELL_CHANGE /*93*/:
                    case '{':
                    case CPUFeature.MSG_SET_CPUSETCONFIG_VR /*125*/:
                        break;
                    case '#':
                    case '/':
                    case ';':
                    case WMStateCons.MSG_WIFI_UPDATE_SCAN_RESULT /*61*/:
                    case WMStateCons.MSG_SUPPLICANT_COMPLETE /*92*/:
                        checkLenient();
                        break;
                    default:
                        i++;
                }
                this.pos += i;
                return;
            }
            this.pos += i;
        } while (fillBuffer(1));
    }

    public int nextInt() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = doPeek();
        }
        int result;
        int[] iArr;
        if (p == 15) {
            result = (int) this.peekedLong;
            if (this.peekedLong == ((long) result)) {
                this.peeked = 0;
                iArr = this.pathIndices;
                int i = this.stackSize - 1;
                iArr[i] = iArr[i] + 1;
                return result;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected an int but was ");
            stringBuilder.append(this.peekedLong);
            stringBuilder.append(locationString());
            throw new NumberFormatException(stringBuilder.toString());
        }
        if (p == 16) {
            this.peekedString = new String(this.buffer, this.pos, this.peekedNumberLength);
            this.pos += this.peekedNumberLength;
        } else if (p == 8 || p == 9 || p == 10) {
            if (p == 10) {
                this.peekedString = nextUnquotedValue();
            } else {
                this.peekedString = nextQuotedValue(p == 8 ? '\'' : '\"');
            }
            try {
                result = Integer.parseInt(this.peekedString);
                this.peeked = 0;
                int[] iArr2 = this.pathIndices;
                int i2 = this.stackSize - 1;
                iArr2[i2] = iArr2[i2] + 1;
                return result;
            } catch (NumberFormatException e) {
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Expected an int but was ");
            stringBuilder2.append(peek());
            stringBuilder2.append(locationString());
            throw new IllegalStateException(stringBuilder2.toString());
        }
        this.peeked = 11;
        double asDouble = Double.parseDouble(this.peekedString);
        result = (int) asDouble;
        if (((double) result) == asDouble) {
            this.peekedString = null;
            this.peeked = 0;
            iArr = this.pathIndices;
            int i3 = this.stackSize - 1;
            iArr[i3] = iArr[i3] + 1;
            return result;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Expected an int but was ");
        stringBuilder3.append(this.peekedString);
        stringBuilder3.append(locationString());
        throw new NumberFormatException(stringBuilder3.toString());
    }

    public void close() throws IOException {
        this.peeked = 0;
        this.stack[0] = 8;
        this.stackSize = 1;
        this.in.close();
    }

    public void skipValue() throws IOException {
        int p;
        int count = 0;
        do {
            p = this.peeked;
            if (p == 0) {
                p = doPeek();
            }
            if (p == 3) {
                push(1);
                count++;
            } else if (p == 1) {
                push(3);
                count++;
            } else if (p == 4) {
                this.stackSize--;
                count--;
            } else if (p == 2) {
                this.stackSize--;
                count--;
            } else if (p == 14 || p == 10) {
                skipUnquotedValue();
            } else if (p == 8 || p == 12) {
                skipQuotedValue('\'');
            } else if (p == 9 || p == 13) {
                skipQuotedValue('\"');
            } else if (p == 16) {
                this.pos += this.peekedNumberLength;
            }
            this.peeked = 0;
        } while (count != 0);
        int[] iArr = this.pathIndices;
        p = this.stackSize - 1;
        iArr[p] = iArr[p] + 1;
        this.pathNames[this.stackSize - 1] = "null";
    }

    private void push(int newTop) {
        int[] newStack;
        if (this.stackSize == this.stack.length) {
            newStack = new int[(this.stackSize * 2)];
            int[] newPathIndices = new int[(this.stackSize * 2)];
            String[] newPathNames = new String[(this.stackSize * 2)];
            System.arraycopy(this.stack, 0, newStack, 0, this.stackSize);
            System.arraycopy(this.pathIndices, 0, newPathIndices, 0, this.stackSize);
            System.arraycopy(this.pathNames, 0, newPathNames, 0, this.stackSize);
            this.stack = newStack;
            this.pathIndices = newPathIndices;
            this.pathNames = newPathNames;
        }
        newStack = this.stack;
        int i = this.stackSize;
        this.stackSize = i + 1;
        newStack[i] = newTop;
    }

    private boolean fillBuffer(int minimum) throws IOException {
        char[] buffer = this.buffer;
        this.lineStart -= this.pos;
        if (this.limit != this.pos) {
            this.limit -= this.pos;
            System.arraycopy(buffer, this.pos, buffer, 0, this.limit);
        } else {
            this.limit = 0;
        }
        this.pos = 0;
        do {
            int read = this.in.read(buffer, this.limit, buffer.length - this.limit);
            int total = read;
            if (read == -1) {
                return false;
            }
            this.limit += total;
            if (this.lineNumber == 0 && this.lineStart == 0 && this.limit > 0 && buffer[0] == 65279) {
                this.pos++;
                this.lineStart++;
                minimum++;
            }
        } while (this.limit < minimum);
        return true;
    }

    private int nextNonWhitespace(boolean throwOnEof) throws IOException {
        char[] buffer = this.buffer;
        int c = this.pos;
        int l = this.limit;
        while (true) {
            if (c == l) {
                this.pos = c;
                if (fillBuffer(1)) {
                    c = this.pos;
                    l = this.limit;
                } else if (!throwOnEof) {
                    return -1;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("End of input");
                    stringBuilder.append(locationString());
                    throw new EOFException(stringBuilder.toString());
                }
            }
            int p = c + 1;
            c = buffer[c];
            if (c == 10) {
                this.lineNumber++;
                this.lineStart = p;
            } else if (!(c == 32 || c == 13 || c == 9)) {
                int p2;
                if (c == 47) {
                    this.pos = p;
                    if (p == l) {
                        this.pos--;
                        boolean charsLoaded = fillBuffer(2);
                        this.pos++;
                        if (!charsLoaded) {
                            return c;
                        }
                    }
                    checkLenient();
                    char peek = buffer[this.pos];
                    if (peek == '*') {
                        this.pos++;
                        if (skipTo("*/")) {
                            p2 = this.pos + 2;
                            l = this.limit;
                        } else {
                            throw syntaxError("Unterminated comment");
                        }
                    } else if (peek != '/') {
                        return c;
                    } else {
                        this.pos++;
                        skipToEndOfLine();
                        p2 = this.pos;
                        l = this.limit;
                    }
                } else if (c == 35) {
                    this.pos = p;
                    checkLenient();
                    skipToEndOfLine();
                    p2 = this.pos;
                    l = this.limit;
                } else {
                    this.pos = p;
                    return c;
                }
                c = p2;
            }
            c = p;
        }
    }

    private void checkLenient() throws IOException {
        if (!this.lenient) {
            throw syntaxError("Use JsonReader.setLenient(true) to accept malformed JSON");
        }
    }

    private void skipToEndOfLine() throws IOException {
        while (true) {
            if (this.pos < this.limit || fillBuffer(1)) {
                char c = this.buffer;
                int i = this.pos;
                this.pos = i + 1;
                c = c[i];
                if (c == 10) {
                    this.lineNumber++;
                    this.lineStart = this.pos;
                    return;
                } else if (c == 13) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private boolean skipTo(String toFind) throws IOException {
        int length = toFind.length();
        while (true) {
            int c = 0;
            if (this.pos + length > this.limit && !fillBuffer(length)) {
                return false;
            }
            if (this.buffer[this.pos] == 10) {
                this.lineNumber++;
                this.lineStart = this.pos + 1;
            } else {
                while (true) {
                    int c2 = c;
                    if (c2 >= length) {
                        return true;
                    }
                    if (this.buffer[this.pos + c2] != toFind.charAt(c2)) {
                        break;
                    }
                    c = c2 + 1;
                }
            }
            this.pos++;
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getSimpleName());
        stringBuilder.append(locationString());
        return stringBuilder.toString();
    }

    String locationString() {
        int line = this.lineNumber + 1;
        int column = (this.pos - this.lineStart) + 1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" at line ");
        stringBuilder.append(line);
        stringBuilder.append(" column ");
        stringBuilder.append(column);
        stringBuilder.append(" path ");
        stringBuilder.append(getPath());
        return stringBuilder.toString();
    }

    public String getPath() {
        StringBuilder result = new StringBuilder().append('$');
        int size = this.stackSize;
        for (int i = 0; i < size; i++) {
            switch (this.stack[i]) {
                case 1:
                case 2:
                    result.append('[');
                    result.append(this.pathIndices[i]);
                    result.append(']');
                    break;
                case 3:
                case 4:
                case 5:
                    result.append('.');
                    if (this.pathNames[i] == null) {
                        break;
                    }
                    result.append(this.pathNames[i]);
                    break;
                default:
                    break;
            }
        }
        return result.toString();
    }

    private char readEscapeCharacter() throws IOException {
        if (this.pos != this.limit || fillBuffer(1)) {
            char escaped = this.buffer;
            int i = this.pos;
            this.pos = i + 1;
            escaped = escaped[i];
            if (escaped == 10) {
                this.lineNumber++;
                this.lineStart = this.pos;
            } else if (!(escaped == '\"' || escaped == '\'' || escaped == '/' || escaped == '\\')) {
                if (escaped == 'b') {
                    return 8;
                }
                if (escaped == 'f') {
                    return 12;
                }
                if (escaped == 'n') {
                    return 10;
                }
                if (escaped == 'r') {
                    return 13;
                }
                switch (escaped) {
                    case 't':
                        return 9;
                    case CPUFeature.MSG_RESET_TOP_APP_CPUSET /*117*/:
                        if (this.pos + 4 <= this.limit || fillBuffer(4)) {
                            char result = 0;
                            int i2 = this.pos;
                            int end = i2 + 4;
                            while (i2 < end) {
                                int i3;
                                char c = this.buffer[i2];
                                result = (char) (result << 4);
                                if (c >= '0' && c <= '9') {
                                    i3 = c - 48;
                                } else if (c >= 'a' && c <= 'f') {
                                    i3 = (c - 97) + 10;
                                } else if (c < 'A' || c > 'F') {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("\\u");
                                    stringBuilder.append(new String(this.buffer, this.pos, 4));
                                    throw new NumberFormatException(stringBuilder.toString());
                                } else {
                                    i3 = (c - 65) + 10;
                                }
                                result = (char) (i3 + result);
                                i2++;
                            }
                            this.pos += 4;
                            return result;
                        }
                        throw syntaxError("Unterminated escape sequence");
                    default:
                        throw syntaxError("Invalid escape sequence");
                }
            }
            return escaped;
        }
        throw syntaxError("Unterminated escape sequence");
    }

    private IOException syntaxError(String message) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(locationString());
        throw new MalformedJsonException(stringBuilder.toString());
    }

    private void consumeNonExecutePrefix() throws IOException {
        nextNonWhitespace(true);
        this.pos--;
        if (this.pos + NON_EXECUTE_PREFIX.length <= this.limit || fillBuffer(NON_EXECUTE_PREFIX.length)) {
            int i = 0;
            while (i < NON_EXECUTE_PREFIX.length) {
                if (this.buffer[this.pos + i] == NON_EXECUTE_PREFIX[i]) {
                    i++;
                } else {
                    return;
                }
            }
            this.pos += NON_EXECUTE_PREFIX.length;
        }
    }
}
