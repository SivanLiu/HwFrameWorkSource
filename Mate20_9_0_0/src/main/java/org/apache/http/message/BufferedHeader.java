package org.apache.http.message;

import org.apache.http.FormattedHeader;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class BufferedHeader implements FormattedHeader, Cloneable {
    private final CharArrayBuffer buffer;
    private final String name;
    private final int valuePos;

    public BufferedHeader(CharArrayBuffer buffer) throws ParseException {
        if (buffer != null) {
            int colon = buffer.indexOf(58);
            if (colon != -1) {
                String s = buffer.substringTrimmed(null, colon);
                if (s.length() != 0) {
                    this.buffer = buffer;
                    this.name = s;
                    this.valuePos = colon + 1;
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid header: ");
                stringBuilder.append(buffer.toString());
                throw new ParseException(stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid header: ");
            stringBuilder2.append(buffer.toString());
            throw new ParseException(stringBuilder2.toString());
        }
        throw new IllegalArgumentException("Char array buffer may not be null");
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.buffer.substringTrimmed(this.valuePos, this.buffer.length());
    }

    public HeaderElement[] getElements() throws ParseException {
        ParserCursor cursor = new ParserCursor(0, this.buffer.length());
        cursor.updatePos(this.valuePos);
        return BasicHeaderValueParser.DEFAULT.parseElements(this.buffer, cursor);
    }

    public int getValuePos() {
        return this.valuePos;
    }

    public CharArrayBuffer getBuffer() {
        return this.buffer;
    }

    public String toString() {
        return this.buffer.toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
