package android.util;

import android.telephony.PhoneNumberUtils;
import android.text.format.DateFormat;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import libcore.internal.StringPool;

public final class JsonReader implements Closeable {
    private static final String FALSE = "false";
    private static final String TRUE = "true";
    private final char[] buffer = new char[1024];
    private int bufferStartColumn = 1;
    private int bufferStartLine = 1;
    private final Reader in;
    private boolean lenient = false;
    private int limit = 0;
    private String name;
    private int pos = 0;
    private boolean skipping;
    private final List<JsonScope> stack = new ArrayList();
    private final StringPool stringPool = new StringPool();
    private JsonToken token;
    private String value;
    private int valueLength;
    private int valuePos;

    public JsonReader(Reader in) {
        push(JsonScope.EMPTY_DOCUMENT);
        this.skipping = false;
        if (in != null) {
            this.in = in;
            return;
        }
        throw new NullPointerException("in == null");
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public boolean isLenient() {
        return this.lenient;
    }

    public void beginArray() throws IOException {
        expect(JsonToken.BEGIN_ARRAY);
    }

    public void endArray() throws IOException {
        expect(JsonToken.END_ARRAY);
    }

    public void beginObject() throws IOException {
        expect(JsonToken.BEGIN_OBJECT);
    }

    public void endObject() throws IOException {
        expect(JsonToken.END_OBJECT);
    }

    private void expect(JsonToken expected) throws IOException {
        peek();
        if (this.token == expected) {
            advance();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected ");
        stringBuilder.append(expected);
        stringBuilder.append(" but was ");
        stringBuilder.append(peek());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public boolean hasNext() throws IOException {
        peek();
        return (this.token == JsonToken.END_OBJECT || this.token == JsonToken.END_ARRAY) ? false : true;
    }

    public JsonToken peek() throws IOException {
        if (this.token != null) {
            return this.token;
        }
        JsonToken firstToken;
        switch (peekStack()) {
            case EMPTY_DOCUMENT:
                replaceTop(JsonScope.NONEMPTY_DOCUMENT);
                firstToken = nextValue();
                if (this.lenient || this.token == JsonToken.BEGIN_ARRAY || this.token == JsonToken.BEGIN_OBJECT) {
                    return firstToken;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Expected JSON document to start with '[' or '{' but was ");
                stringBuilder.append(this.token);
                throw new IOException(stringBuilder.toString());
            case EMPTY_ARRAY:
                return nextInArray(true);
            case NONEMPTY_ARRAY:
                return nextInArray(false);
            case EMPTY_OBJECT:
                return nextInObject(true);
            case DANGLING_NAME:
                return objectValue();
            case NONEMPTY_OBJECT:
                return nextInObject(false);
            case NONEMPTY_DOCUMENT:
                try {
                    firstToken = nextValue();
                    if (this.lenient) {
                        return firstToken;
                    }
                    throw syntaxError("Expected EOF");
                } catch (EOFException e) {
                    JsonToken jsonToken = JsonToken.END_DOCUMENT;
                    this.token = jsonToken;
                    return jsonToken;
                }
            case CLOSED:
                throw new IllegalStateException("JsonReader is closed");
            default:
                throw new AssertionError();
        }
    }

    private JsonToken advance() throws IOException {
        peek();
        JsonToken result = this.token;
        this.token = null;
        this.value = null;
        this.name = null;
        return result;
    }

    public String nextName() throws IOException {
        peek();
        if (this.token == JsonToken.NAME) {
            String result = this.name;
            advance();
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected a name but was ");
        stringBuilder.append(peek());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public String nextString() throws IOException {
        peek();
        if (this.token == JsonToken.STRING || this.token == JsonToken.NUMBER) {
            String result = this.value;
            advance();
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected a string but was ");
        stringBuilder.append(peek());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public boolean nextBoolean() throws IOException {
        peek();
        if (this.token == JsonToken.BOOLEAN) {
            boolean result = this.value == TRUE;
            advance();
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected a boolean but was ");
        stringBuilder.append(this.token);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void nextNull() throws IOException {
        peek();
        if (this.token == JsonToken.NULL) {
            advance();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected null but was ");
        stringBuilder.append(this.token);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public double nextDouble() throws IOException {
        peek();
        if (this.token == JsonToken.STRING || this.token == JsonToken.NUMBER) {
            double result = Double.parseDouble(this.value);
            advance();
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected a double but was ");
        stringBuilder.append(this.token);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public long nextLong() throws IOException {
        peek();
        if (this.token == JsonToken.STRING || this.token == JsonToken.NUMBER) {
            NumberFormatException ignored;
            try {
                ignored = Long.parseLong(this.value);
            } catch (NumberFormatException e) {
                double asDouble = Double.parseDouble(this.value);
                long result = (long) asDouble;
                if (((double) result) == asDouble) {
                    ignored = result;
                } else {
                    throw new NumberFormatException(this.value);
                }
            }
            advance();
            return ignored;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected a long but was ");
        stringBuilder.append(this.token);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public int nextInt() throws IOException {
        peek();
        if (this.token == JsonToken.STRING || this.token == JsonToken.NUMBER) {
            NumberFormatException ignored;
            try {
                ignored = Integer.parseInt(this.value);
            } catch (NumberFormatException e) {
                double asDouble = Double.parseDouble(this.value);
                int result = (int) asDouble;
                if (((double) result) == asDouble) {
                    ignored = result;
                } else {
                    throw new NumberFormatException(this.value);
                }
            }
            advance();
            return ignored;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected an int but was ");
        stringBuilder.append(this.token);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void close() throws IOException {
        this.value = null;
        this.token = null;
        this.stack.clear();
        this.stack.add(JsonScope.CLOSED);
        this.in.close();
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x002f A:{SYNTHETIC, EDGE_INSN: B:25:0x002f->B:29:0x002f ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x002f A:{SYNTHETIC, EDGE_INSN: B:25:0x002f->B:29:0x002f ?: BREAK  } */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void skipValue() throws IOException {
        this.skipping = true;
        try {
            if (!hasNext() || peek() == JsonToken.END_DOCUMENT) {
                throw new IllegalStateException("No element left to skip");
            }
            int count = 0;
            while (true) {
                JsonToken token = advance();
                if (token != JsonToken.BEGIN_ARRAY) {
                    if (token != JsonToken.BEGIN_OBJECT) {
                        if (token == JsonToken.END_ARRAY || token == JsonToken.END_OBJECT) {
                            count--;
                            continue;
                            if (count == 0) {
                                break;
                            }
                        } else if (count == 0) {
                        }
                    }
                }
                count++;
                continue;
                if (count == 0) {
                }
            }
        } finally {
            this.skipping = false;
        }
    }

    private JsonScope peekStack() {
        return (JsonScope) this.stack.get(this.stack.size() - 1);
    }

    private JsonScope pop() {
        return (JsonScope) this.stack.remove(this.stack.size() - 1);
    }

    private void push(JsonScope newTop) {
        this.stack.add(newTop);
    }

    private void replaceTop(JsonScope newTop) {
        this.stack.set(this.stack.size() - 1, newTop);
    }

    private JsonToken nextInArray(boolean firstElement) throws IOException {
        int nextNonWhitespace;
        JsonToken jsonToken;
        if (firstElement) {
            replaceTop(JsonScope.NONEMPTY_ARRAY);
        } else {
            nextNonWhitespace = nextNonWhitespace();
            if (nextNonWhitespace != 44) {
                if (nextNonWhitespace == 59) {
                    checkLenient();
                } else if (nextNonWhitespace == 93) {
                    pop();
                    jsonToken = JsonToken.END_ARRAY;
                    this.token = jsonToken;
                    return jsonToken;
                } else {
                    throw syntaxError("Unterminated array");
                }
            }
        }
        nextNonWhitespace = nextNonWhitespace();
        if (!(nextNonWhitespace == 44 || nextNonWhitespace == 59)) {
            if (nextNonWhitespace != 93) {
                this.pos--;
                return nextValue();
            } else if (firstElement) {
                pop();
                jsonToken = JsonToken.END_ARRAY;
                this.token = jsonToken;
                return jsonToken;
            }
        }
        checkLenient();
        this.pos--;
        this.value = "null";
        jsonToken = JsonToken.NULL;
        this.token = jsonToken;
        return jsonToken;
    }

    private JsonToken nextInObject(boolean firstElement) throws IOException {
        JsonToken jsonToken;
        JsonToken jsonToken2;
        if (!firstElement) {
            int nextNonWhitespace = nextNonWhitespace();
            if (!(nextNonWhitespace == 44 || nextNonWhitespace == 59)) {
                if (nextNonWhitespace == 125) {
                    pop();
                    jsonToken2 = JsonToken.END_OBJECT;
                    this.token = jsonToken2;
                    return jsonToken2;
                }
                throw syntaxError("Unterminated object");
            }
        } else if (nextNonWhitespace() != 125) {
            this.pos--;
        } else {
            pop();
            jsonToken2 = JsonToken.END_OBJECT;
            this.token = jsonToken2;
            return jsonToken2;
        }
        int quote = nextNonWhitespace();
        if (quote != 34) {
            if (quote != 39) {
                checkLenient();
                this.pos--;
                this.name = nextLiteral(false);
                if (this.name.isEmpty()) {
                    throw syntaxError("Expected name");
                }
                replaceTop(JsonScope.DANGLING_NAME);
                jsonToken = JsonToken.NAME;
                this.token = jsonToken;
                return jsonToken;
            }
            checkLenient();
        }
        this.name = nextString((char) quote);
        replaceTop(JsonScope.DANGLING_NAME);
        jsonToken = JsonToken.NAME;
        this.token = jsonToken;
        return jsonToken;
    }

    private JsonToken objectValue() throws IOException {
        int nextNonWhitespace = nextNonWhitespace();
        if (nextNonWhitespace != 58) {
            if (nextNonWhitespace == 61) {
                checkLenient();
                if ((this.pos < this.limit || fillBuffer(1)) && this.buffer[this.pos] == '>') {
                    this.pos++;
                }
            } else {
                throw syntaxError("Expected ':'");
            }
        }
        replaceTop(JsonScope.NONEMPTY_OBJECT);
        return nextValue();
    }

    private JsonToken nextValue() throws IOException {
        JsonToken jsonToken;
        int c = nextNonWhitespace();
        if (c != 34) {
            if (c == 39) {
                checkLenient();
            } else if (c == 91) {
                push(JsonScope.EMPTY_ARRAY);
                jsonToken = JsonToken.BEGIN_ARRAY;
                this.token = jsonToken;
                return jsonToken;
            } else if (c != 123) {
                this.pos--;
                return readLiteral();
            } else {
                push(JsonScope.EMPTY_OBJECT);
                jsonToken = JsonToken.BEGIN_OBJECT;
                this.token = jsonToken;
                return jsonToken;
            }
        }
        this.value = nextString((char) c);
        jsonToken = JsonToken.STRING;
        this.token = jsonToken;
        return jsonToken;
    }

    private boolean fillBuffer(int minimum) throws IOException {
        int i;
        for (i = 0; i < this.pos; i++) {
            if (this.buffer[i] == 10) {
                this.bufferStartLine++;
                this.bufferStartColumn = 1;
            } else {
                this.bufferStartColumn++;
            }
        }
        if (this.limit != this.pos) {
            this.limit -= this.pos;
            System.arraycopy(this.buffer, this.pos, this.buffer, 0, this.limit);
        } else {
            this.limit = 0;
        }
        this.pos = 0;
        do {
            i = this.in.read(this.buffer, this.limit, this.buffer.length - this.limit);
            int total = i;
            if (i == -1) {
                return false;
            }
            this.limit += total;
            if (this.bufferStartLine == 1 && this.bufferStartColumn == 1 && this.limit > 0 && this.buffer[0] == 65279) {
                this.pos++;
                this.bufferStartColumn--;
            }
        } while (this.limit < minimum);
        return true;
    }

    private int getLineNumber() {
        int result = this.bufferStartLine;
        for (int i = 0; i < this.pos; i++) {
            if (this.buffer[i] == 10) {
                result++;
            }
        }
        return result;
    }

    private int getColumnNumber() {
        int result = this.bufferStartColumn;
        for (int i = 0; i < this.pos; i++) {
            if (this.buffer[i] == 10) {
                result = 1;
            } else {
                result++;
            }
        }
        return result;
    }

    private int nextNonWhitespace() throws IOException {
        while (true) {
            if (this.pos < this.limit || fillBuffer(1)) {
                int c = this.buffer;
                int i = this.pos;
                this.pos = i + 1;
                c = c[i];
                if (!(c == 13 || c == 32)) {
                    if (c == 35) {
                        checkLenient();
                        skipToEndOfLine();
                    } else if (c != 47) {
                        switch (c) {
                            case 9:
                            case 10:
                                break;
                            default:
                                return c;
                        }
                    } else if (this.pos == this.limit && !fillBuffer(1)) {
                        return c;
                    } else {
                        checkLenient();
                        char peek = this.buffer[this.pos];
                        if (peek == '*') {
                            this.pos++;
                            if (skipTo("*/")) {
                                this.pos += 2;
                            } else {
                                throw syntaxError("Unterminated comment");
                            }
                        } else if (peek != '/') {
                            return c;
                        } else {
                            this.pos++;
                            skipToEndOfLine();
                        }
                    }
                }
            } else {
                throw new EOFException("End of input");
            }
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
                if (c == 13 || c == 10) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private boolean skipTo(String toFind) throws IOException {
        while (true) {
            int c = 0;
            if (this.pos + toFind.length() > this.limit && !fillBuffer(toFind.length())) {
                return false;
            }
            while (true) {
                int c2 = c;
                if (c2 >= toFind.length()) {
                    return true;
                }
                if (this.buffer[this.pos + c2] != toFind.charAt(c2)) {
                    break;
                }
                c = c2 + 1;
            }
            this.pos++;
        }
    }

    private String nextString(char quote) throws IOException {
        StringBuilder builder = null;
        while (true) {
            int start = this.pos;
            while (this.pos < this.limit) {
                int c = this.buffer;
                int i = this.pos;
                this.pos = i + 1;
                char c2 = c[i];
                if (c2 == quote) {
                    if (this.skipping) {
                        return "skipped!";
                    }
                    if (builder == null) {
                        return this.stringPool.get(this.buffer, start, (this.pos - start) - 1);
                    }
                    builder.append(this.buffer, start, (this.pos - start) - 1);
                    return builder.toString();
                } else if (c2 == '\\') {
                    if (builder == null) {
                        builder = new StringBuilder();
                    }
                    builder.append(this.buffer, start, (this.pos - start) - 1);
                    builder.append(readEscapeCharacter());
                    start = this.pos;
                }
            }
            if (builder == null) {
                builder = new StringBuilder();
            }
            builder.append(this.buffer, start, this.pos - start);
            if (!fillBuffer(1)) {
                throw syntaxError("Unterminated string");
            }
        }
    }

    private String nextLiteral(boolean assignOffsetsOnly) throws IOException {
        String result;
        this.valuePos = -1;
        this.valueLength = 0;
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
                    case ',':
                    case ':':
                    case '[':
                    case ']':
                    case '{':
                    case '}':
                        break;
                    case '#':
                    case '/':
                    case ';':
                    case '=':
                    case '\\':
                        checkLenient();
                        break;
                    default:
                        i++;
                        continue;
                }
            } else if (i >= this.buffer.length) {
                if (builder == null) {
                    builder = new StringBuilder();
                }
                builder.append(this.buffer, this.pos, i);
                this.valueLength += i;
                this.pos += i;
                i = 0;
                if (fillBuffer(1)) {
                }
            } else if (!fillBuffer(i + 1)) {
                this.buffer[this.limit] = 0;
            }
        }
        if (assignOffsetsOnly && builder == null) {
            this.valuePos = this.pos;
            result = null;
        } else if (this.skipping) {
            result = "skipped!";
        } else if (builder == null) {
            result = this.stringPool.get(this.buffer, this.pos, i);
        } else {
            builder.append(this.buffer, this.pos, i);
            result = builder.toString();
        }
        this.valueLength += i;
        this.pos += i;
        return result;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getSimpleName());
        stringBuilder.append(" near ");
        stringBuilder.append(getSnippet());
        return stringBuilder.toString();
    }

    private char readEscapeCharacter() throws IOException {
        if (this.pos != this.limit || fillBuffer(1)) {
            char escaped = this.buffer;
            int i = this.pos;
            this.pos = i + 1;
            escaped = escaped[i];
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
                case 'u':
                    if (this.pos + 4 <= this.limit || fillBuffer(4)) {
                        String hex = this.stringPool.get(this.buffer, this.pos, 4);
                        this.pos += 4;
                        return (char) Integer.parseInt(hex, 16);
                    }
                    throw syntaxError("Unterminated escape sequence");
                default:
                    return escaped;
            }
        }
        throw syntaxError("Unterminated escape sequence");
    }

    private JsonToken readLiteral() throws IOException {
        this.value = nextLiteral(true);
        if (this.valueLength != 0) {
            this.token = decodeLiteral();
            if (this.token == JsonToken.STRING) {
                checkLenient();
            }
            return this.token;
        }
        throw syntaxError("Expected literal value");
    }

    private JsonToken decodeLiteral() throws IOException {
        if (this.valuePos == -1) {
            return JsonToken.STRING;
        }
        if (this.valueLength == 4 && (('n' == this.buffer[this.valuePos] || PhoneNumberUtils.WILD == this.buffer[this.valuePos]) && (('u' == this.buffer[this.valuePos + 1] || 'U' == this.buffer[this.valuePos + 1]) && (('l' == this.buffer[this.valuePos + 2] || DateFormat.STANDALONE_MONTH == this.buffer[this.valuePos + 2]) && ('l' == this.buffer[this.valuePos + 3] || DateFormat.STANDALONE_MONTH == this.buffer[this.valuePos + 3]))))) {
            this.value = "null";
            return JsonToken.NULL;
        } else if (this.valueLength == 4 && (('t' == this.buffer[this.valuePos] || 'T' == this.buffer[this.valuePos]) && (('r' == this.buffer[this.valuePos + 1] || 'R' == this.buffer[this.valuePos + 1]) && (('u' == this.buffer[this.valuePos + 2] || 'U' == this.buffer[this.valuePos + 2]) && ('e' == this.buffer[this.valuePos + 3] || DateFormat.DAY == this.buffer[this.valuePos + 3]))))) {
            this.value = TRUE;
            return JsonToken.BOOLEAN;
        } else if (this.valueLength == 5 && (('f' == this.buffer[this.valuePos] || 'F' == this.buffer[this.valuePos]) && ((DateFormat.AM_PM == this.buffer[this.valuePos + 1] || DateFormat.CAPITAL_AM_PM == this.buffer[this.valuePos + 1]) && (('l' == this.buffer[this.valuePos + 2] || DateFormat.STANDALONE_MONTH == this.buffer[this.valuePos + 2]) && ((DateFormat.SECONDS == this.buffer[this.valuePos + 3] || 'S' == this.buffer[this.valuePos + 3]) && ('e' == this.buffer[this.valuePos + 4] || DateFormat.DAY == this.buffer[this.valuePos + 4])))))) {
            this.value = FALSE;
            return JsonToken.BOOLEAN;
        } else {
            this.value = this.stringPool.get(this.buffer, this.valuePos, this.valueLength);
            return decodeNumber(this.buffer, this.valuePos, this.valueLength);
        }
    }

    private JsonToken decodeNumber(char[] chars, int offset, int length) {
        int i = offset;
        int c = chars[i];
        if (c == 45) {
            i++;
            c = chars[i];
        }
        if (c == 48) {
            i++;
            c = chars[i];
        } else if (c < 49 || c > 57) {
            return JsonToken.STRING;
        } else {
            i++;
            c = chars[i];
            while (c >= 48 && c <= 57) {
                i++;
                c = chars[i];
            }
        }
        if (c == 46) {
            i++;
            c = chars[i];
            while (c >= 48 && c <= 57) {
                i++;
                c = chars[i];
            }
        }
        if (c == 101 || c == 69) {
            i++;
            c = chars[i];
            if (c == 43 || c == 45) {
                i++;
                c = chars[i];
            }
            if (c < 48 || c > 57) {
                return JsonToken.STRING;
            }
            i++;
            c = chars[i];
            while (c >= 48 && c <= 57) {
                i++;
                c = chars[i];
            }
        }
        if (i == offset + length) {
            return JsonToken.NUMBER;
        }
        return JsonToken.STRING;
    }

    private IOException syntaxError(String message) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(" at line ");
        stringBuilder.append(getLineNumber());
        stringBuilder.append(" column ");
        stringBuilder.append(getColumnNumber());
        throw new MalformedJsonException(stringBuilder.toString());
    }

    private CharSequence getSnippet() {
        StringBuilder snippet = new StringBuilder();
        int beforePos = Math.min(this.pos, 20);
        snippet.append(this.buffer, this.pos - beforePos, beforePos);
        snippet.append(this.buffer, this.pos, Math.min(this.limit - this.pos, 20));
        return snippet;
    }
}
