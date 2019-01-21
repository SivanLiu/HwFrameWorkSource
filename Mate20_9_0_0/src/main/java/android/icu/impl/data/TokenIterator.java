package android.icu.impl.data;

import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.Utility;
import android.icu.text.UTF16;
import java.io.IOException;

public class TokenIterator {
    private StringBuffer buf = new StringBuffer();
    private boolean done = false;
    private int lastpos = -1;
    private String line = null;
    private int pos = -1;
    private ResourceReader reader;

    public TokenIterator(ResourceReader r) {
        this.reader = r;
    }

    public String next() throws IOException {
        if (this.done) {
            return null;
        }
        while (true) {
            if (this.line == null) {
                this.line = this.reader.readLineSkippingComments();
                if (this.line == null) {
                    this.done = true;
                    return null;
                }
                this.pos = 0;
            }
            this.buf.setLength(0);
            this.lastpos = this.pos;
            this.pos = nextToken(this.pos);
            if (this.pos >= 0) {
                return this.buf.toString();
            }
            this.line = null;
        }
    }

    public int getLineNumber() {
        return this.reader.getLineNumber();
    }

    public String describePosition() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.reader.describePosition());
        stringBuilder.append(':');
        stringBuilder.append(this.lastpos + 1);
        return stringBuilder.toString();
    }

    private int nextToken(int position) {
        char c = PatternProps.skipWhiteSpace(this.line, position);
        if (c == this.line.length()) {
            return -1;
        }
        char startpos = c;
        int position2 = c + 1;
        c = this.line.charAt(c);
        char quote = 0;
        if (c != PatternTokenizer.SINGLE_QUOTE) {
            switch (c) {
                case '\"':
                    break;
                case '#':
                    return -1;
                default:
                    this.buf.append(c);
                    break;
            }
        }
        quote = c;
        int[] posref = null;
        while (position2 < this.line.length()) {
            c = this.line.charAt(position2);
            if (c == PatternTokenizer.BACK_SLASH) {
                if (posref == null) {
                    posref = new int[1];
                }
                posref[0] = position2 + 1;
                int c32 = Utility.unescapeAt(this.line, posref);
                if (c32 >= 0) {
                    UTF16.append(this.buf, c32);
                    position2 = posref[0];
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid escape at ");
                    stringBuilder.append(this.reader.describePosition());
                    stringBuilder.append(':');
                    stringBuilder.append(position2);
                    throw new RuntimeException(stringBuilder.toString());
                }
            } else if ((quote != 0 && c == quote) || (quote == 0 && PatternProps.isWhiteSpace(c))) {
                return position2 + 1;
            } else {
                if (quote == 0 && c == '#') {
                    return position2;
                }
                this.buf.append(c);
                position2++;
            }
        }
        if (quote == 0) {
            return position2;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unterminated quote at ");
        stringBuilder2.append(this.reader.describePosition());
        stringBuilder2.append(':');
        stringBuilder2.append(startpos);
        throw new RuntimeException(stringBuilder2.toString());
    }
}
