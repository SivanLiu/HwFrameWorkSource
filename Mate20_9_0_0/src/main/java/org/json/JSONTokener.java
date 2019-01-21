package org.json;

public class JSONTokener {
    private final String in;
    private int pos;

    public JSONTokener(String in) {
        if (in != null && in.startsWith("﻿")) {
            in = in.substring(1);
        }
        this.in = in;
    }

    public Object nextValue() throws JSONException {
        int c = nextCleanInternal();
        if (c == -1) {
            throw syntaxError("End of input");
        } else if (c == 34 || c == 39) {
            return nextString((char) c);
        } else {
            if (c == 91) {
                return readArray();
            }
            if (c == 123) {
                return readObject();
            }
            this.pos--;
            return readLiteral();
        }
    }

    private int nextCleanInternal() throws JSONException {
        while (this.pos < this.in.length()) {
            int c = this.in;
            int i = this.pos;
            this.pos = i + 1;
            c = c.charAt(i);
            if (!(c == 13 || c == 32)) {
                if (c == 35) {
                    skipToEndOfLine();
                } else if (c != 47) {
                    switch (c) {
                        case 9:
                        case 10:
                            break;
                        default:
                            return c;
                    }
                } else if (this.pos == this.in.length()) {
                    return c;
                } else {
                    char peek = this.in.charAt(this.pos);
                    if (peek == '*') {
                        this.pos++;
                        i = this.in.indexOf("*/", this.pos);
                        if (i != -1) {
                            this.pos = i + 2;
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
        }
        return -1;
    }

    private void skipToEndOfLine() {
        while (this.pos < this.in.length()) {
            char c = this.in.charAt(this.pos);
            if (c == 13 || c == 10) {
                this.pos++;
                return;
            }
            this.pos++;
        }
    }

    public String nextString(char quote) throws JSONException {
        StringBuilder builder = null;
        int start = this.pos;
        while (this.pos < this.in.length()) {
            int c = this.in;
            int i = this.pos;
            this.pos = i + 1;
            char c2 = c.charAt(i);
            if (c2 == quote) {
                if (builder == null) {
                    return new String(this.in.substring(start, this.pos - 1));
                }
                builder.append(this.in, start, this.pos - 1);
                return builder.toString();
            } else if (c2 == '\\') {
                if (this.pos != this.in.length()) {
                    if (builder == null) {
                        builder = new StringBuilder();
                    }
                    builder.append(this.in, start, this.pos - 1);
                    builder.append(readEscapeCharacter());
                    start = this.pos;
                } else {
                    throw syntaxError("Unterminated escape sequence");
                }
            }
        }
        throw syntaxError("Unterminated string");
    }

    private char readEscapeCharacter() throws JSONException {
        char escaped = this.in;
        int i = this.pos;
        this.pos = i + 1;
        escaped = escaped.charAt(i);
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
                if (this.pos + 4 <= this.in.length()) {
                    String hex = this.in.substring(this.pos, this.pos + 4);
                    this.pos += 4;
                    try {
                        return (char) Integer.parseInt(hex, 16);
                    } catch (NumberFormatException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid escape sequence: ");
                        stringBuilder.append(hex);
                        throw syntaxError(stringBuilder.toString());
                    }
                }
                throw syntaxError("Unterminated escape sequence");
            default:
                return escaped;
        }
    }

    private Object readLiteral() throws JSONException {
        String literal = nextToInternal("{}[]/\\:,=;# \t\f");
        if (literal.length() == 0) {
            throw syntaxError("Expected literal value");
        } else if ("null".equalsIgnoreCase(literal)) {
            return JSONObject.NULL;
        } else {
            if ("true".equalsIgnoreCase(literal)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(literal)) {
                return Boolean.FALSE;
            }
            if (literal.indexOf(46) == -1) {
                int base = 10;
                String number = literal;
                if (number.startsWith("0x") || number.startsWith("0X")) {
                    number = number.substring(2);
                    base = 16;
                } else if (number.startsWith(AndroidHardcodedSystemProperties.JAVA_VERSION) && number.length() > 1) {
                    number = number.substring(1);
                    base = 8;
                }
                try {
                    long longValue = Long.parseLong(number, base);
                    if (longValue > 2147483647L || longValue < -2147483648L) {
                        return Long.valueOf(longValue);
                    }
                    return Integer.valueOf((int) longValue);
                } catch (NumberFormatException e) {
                }
            }
            try {
                return Double.valueOf(literal);
            } catch (NumberFormatException e2) {
                return new String(literal);
            }
        }
    }

    private String nextToInternal(String excluded) {
        int start = this.pos;
        while (this.pos < this.in.length()) {
            char c = this.in.charAt(this.pos);
            if (c == 13 || c == 10 || excluded.indexOf(c) != -1) {
                return this.in.substring(start, this.pos);
            }
            this.pos++;
        }
        return this.in.substring(start);
    }

    private JSONObject readObject() throws JSONException {
        JSONObject result = new JSONObject();
        int first = nextCleanInternal();
        if (first == 125) {
            return result;
        }
        if (first != -1) {
            this.pos--;
        }
        while (true) {
            Object name = nextValue();
            StringBuilder stringBuilder;
            if (name instanceof String) {
                int separator = nextCleanInternal();
                if (separator == 58 || separator == 61) {
                    if (this.pos < this.in.length() && this.in.charAt(this.pos) == '>') {
                        this.pos++;
                    }
                    result.put((String) name, nextValue());
                    int nextCleanInternal = nextCleanInternal();
                    if (nextCleanInternal != 44 && nextCleanInternal != 59) {
                        if (nextCleanInternal == 125) {
                            return result;
                        }
                        throw syntaxError("Unterminated object");
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected ':' after ");
                    stringBuilder.append(name);
                    throw syntaxError(stringBuilder.toString());
                }
            } else if (name == null) {
                throw syntaxError("Names cannot be null");
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Names must be strings, but ");
                stringBuilder.append(name);
                stringBuilder.append(" is of type ");
                stringBuilder.append(name.getClass().getName());
                throw syntaxError(stringBuilder.toString());
            }
        }
    }

    private JSONArray readArray() throws JSONException {
        JSONArray result = new JSONArray();
        boolean hasTrailingSeparator = false;
        while (true) {
            int nextCleanInternal = nextCleanInternal();
            if (nextCleanInternal == -1) {
                throw syntaxError("Unterminated array");
            } else if (nextCleanInternal == 44 || nextCleanInternal == 59) {
                result.put(null);
                hasTrailingSeparator = true;
            } else if (nextCleanInternal != 93) {
                this.pos--;
                result.put(nextValue());
                nextCleanInternal = nextCleanInternal();
                if (nextCleanInternal == 44 || nextCleanInternal == 59) {
                    hasTrailingSeparator = true;
                } else if (nextCleanInternal == 93) {
                    return result;
                } else {
                    throw syntaxError("Unterminated array");
                }
            } else {
                if (hasTrailingSeparator) {
                    result.put(null);
                }
                return result;
            }
        }
    }

    public JSONException syntaxError(String message) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(this);
        return new JSONException(stringBuilder.toString());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" at character ");
        stringBuilder.append(this.pos);
        stringBuilder.append(" of ");
        stringBuilder.append(this.in);
        return stringBuilder.toString();
    }

    public boolean more() {
        return this.pos < this.in.length();
    }

    public char next() {
        if (this.pos >= this.in.length()) {
            return 0;
        }
        String str = this.in;
        int i = this.pos;
        this.pos = i + 1;
        return str.charAt(i);
    }

    public char next(char c) throws JSONException {
        char result = next();
        if (result == c) {
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected ");
        stringBuilder.append(c);
        stringBuilder.append(" but was ");
        stringBuilder.append(result);
        throw syntaxError(stringBuilder.toString());
    }

    public char nextClean() throws JSONException {
        int nextCleanInt = nextCleanInternal();
        return nextCleanInt == -1 ? 0 : (char) nextCleanInt;
    }

    public String next(int length) throws JSONException {
        if (this.pos + length <= this.in.length()) {
            String result = this.in.substring(this.pos, this.pos + length);
            this.pos += length;
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(length);
        stringBuilder.append(" is out of bounds");
        throw syntaxError(stringBuilder.toString());
    }

    public String nextTo(String excluded) {
        if (excluded != null) {
            return nextToInternal(excluded).trim();
        }
        throw new NullPointerException("excluded == null");
    }

    public String nextTo(char excluded) {
        return nextToInternal(String.valueOf(excluded)).trim();
    }

    public void skipPast(String thru) {
        int thruStart = this.in.indexOf(thru, this.pos);
        this.pos = thruStart == -1 ? this.in.length() : thru.length() + thruStart;
    }

    public char skipTo(char to) {
        int index = this.in.indexOf(to, this.pos);
        if (index == -1) {
            return 0;
        }
        this.pos = index;
        return to;
    }

    public void back() {
        int i = this.pos - 1;
        this.pos = i;
        if (i == -1) {
            this.pos = 0;
        }
    }

    public static int dehexchar(char hex) {
        if (hex >= '0' && hex <= '9') {
            return hex - 48;
        }
        if (hex >= 'A' && hex <= 'F') {
            return (hex - 65) + 10;
        }
        if (hex < 'a' || hex > 'f') {
            return -1;
        }
        return (hex - 97) + 10;
    }
}
