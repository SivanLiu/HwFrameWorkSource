package com.android.internal.util;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

public class IndentingPrintWriter extends PrintWriter {
    private char[] mCurrentIndent;
    private int mCurrentLength;
    private boolean mEmptyLine;
    private StringBuilder mIndentBuilder;
    private char[] mSingleChar;
    private final String mSingleIndent;
    private final int mWrapLength;

    public IndentingPrintWriter(Writer writer, String singleIndent) {
        this(writer, singleIndent, -1);
    }

    public IndentingPrintWriter(Writer writer, String singleIndent, int wrapLength) {
        super(writer);
        this.mIndentBuilder = new StringBuilder();
        this.mEmptyLine = true;
        this.mSingleChar = new char[1];
        this.mSingleIndent = singleIndent;
        this.mWrapLength = wrapLength;
    }

    public IndentingPrintWriter setIndent(String indent) {
        this.mIndentBuilder.setLength(0);
        this.mIndentBuilder.append(indent);
        this.mCurrentIndent = null;
        return this;
    }

    public IndentingPrintWriter setIndent(int indent) {
        int i = 0;
        this.mIndentBuilder.setLength(0);
        while (true) {
            int i2 = i;
            if (i2 >= indent) {
                return this;
            }
            increaseIndent();
            i = i2 + 1;
        }
    }

    public IndentingPrintWriter increaseIndent() {
        this.mIndentBuilder.append(this.mSingleIndent);
        this.mCurrentIndent = null;
        return this;
    }

    public IndentingPrintWriter decreaseIndent() {
        this.mIndentBuilder.delete(0, this.mSingleIndent.length());
        this.mCurrentIndent = null;
        return this;
    }

    public IndentingPrintWriter printPair(String key, Object value) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(key);
        stringBuilder.append("=");
        stringBuilder.append(String.valueOf(value));
        stringBuilder.append(" ");
        print(stringBuilder.toString());
        return this;
    }

    public IndentingPrintWriter printPair(String key, Object[] value) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(key);
        stringBuilder.append("=");
        stringBuilder.append(Arrays.toString(value));
        stringBuilder.append(" ");
        print(stringBuilder.toString());
        return this;
    }

    public IndentingPrintWriter printHexPair(String key, int value) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(key);
        stringBuilder.append("=0x");
        stringBuilder.append(Integer.toHexString(value));
        stringBuilder.append(" ");
        print(stringBuilder.toString());
        return this;
    }

    public void println() {
        write(10);
    }

    public void write(int c) {
        this.mSingleChar[0] = (char) c;
        write(this.mSingleChar, 0, 1);
    }

    public void write(String s, int off, int len) {
        char[] buf = new char[len];
        s.getChars(off, len - off, buf, 0);
        write(buf, 0, len);
    }

    public void write(char[] buf, int offset, int count) {
        int indentLength = this.mIndentBuilder.length();
        int bufferEnd = offset + count;
        int lineEnd = offset;
        int lineStart = lineEnd;
        while (lineEnd < bufferEnd) {
            int lineEnd2 = lineEnd + 1;
            lineEnd = buf[lineEnd];
            this.mCurrentLength++;
            if (lineEnd == 10) {
                maybeWriteIndent();
                super.write(buf, lineStart, lineEnd2 - lineStart);
                lineStart = lineEnd2;
                this.mEmptyLine = true;
                this.mCurrentLength = 0;
            }
            if (this.mWrapLength > 0 && this.mCurrentLength >= this.mWrapLength - indentLength) {
                if (this.mEmptyLine) {
                    maybeWriteIndent();
                    super.write(buf, lineStart, lineEnd2 - lineStart);
                    super.write(10);
                    this.mEmptyLine = true;
                    lineStart = lineEnd2;
                    this.mCurrentLength = 0;
                } else {
                    super.write(10);
                    this.mEmptyLine = true;
                    this.mCurrentLength = lineEnd2 - lineStart;
                }
            }
            lineEnd = lineEnd2;
        }
        if (lineStart != lineEnd) {
            maybeWriteIndent();
            super.write(buf, lineStart, lineEnd - lineStart);
        }
    }

    private void maybeWriteIndent() {
        if (this.mEmptyLine) {
            this.mEmptyLine = false;
            if (this.mIndentBuilder.length() != 0) {
                if (this.mCurrentIndent == null) {
                    this.mCurrentIndent = this.mIndentBuilder.toString().toCharArray();
                }
                super.write(this.mCurrentIndent, 0, this.mCurrentIndent.length);
            }
        }
    }
}
