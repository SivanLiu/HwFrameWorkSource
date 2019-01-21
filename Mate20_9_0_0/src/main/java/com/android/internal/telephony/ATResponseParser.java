package com.android.internal.telephony;

public class ATResponseParser {
    private String mLine;
    private int mNext = 0;
    private int mTokEnd;
    private int mTokStart;

    public ATResponseParser(String line) {
        this.mLine = line;
    }

    public boolean nextBoolean() {
        nextTok();
        if (this.mTokEnd - this.mTokStart <= 1) {
            char c = this.mLine.charAt(this.mTokStart);
            if (c == '0') {
                return false;
            }
            if (c == '1') {
                return true;
            }
            throw new ATParseEx();
        }
        throw new ATParseEx();
    }

    public int nextInt() {
        int ret = 0;
        nextTok();
        for (int i = this.mTokStart; i < this.mTokEnd; i++) {
            char c = this.mLine.charAt(i);
            if (c < '0' || c > '9') {
                throw new ATParseEx();
            }
            ret = (ret * 10) + (c - 48);
        }
        return ret;
    }

    public String nextString() {
        nextTok();
        return this.mLine.substring(this.mTokStart, this.mTokEnd);
    }

    public boolean hasMore() {
        return this.mNext < this.mLine.length();
    }

    private void nextTok() {
        int len = this.mLine.length();
        if (this.mNext == 0) {
            skipPrefix();
        }
        if (this.mNext < len) {
            try {
                char c = this.mLine;
                int i = this.mNext;
                this.mNext = i + 1;
                c = skipWhiteSpace(c.charAt(i));
                String str;
                int i2;
                if (c != '\"') {
                    this.mTokStart = this.mNext - 1;
                    this.mTokEnd = this.mTokStart;
                    while (c != ',') {
                        if (!Character.isWhitespace(c)) {
                            this.mTokEnd = this.mNext;
                        }
                        if (this.mNext != len) {
                            str = this.mLine;
                            i2 = this.mNext;
                            this.mNext = i2 + 1;
                            c = str.charAt(i2);
                        } else {
                            return;
                        }
                    }
                    return;
                } else if (this.mNext < len) {
                    String str2 = this.mLine;
                    int i3 = this.mNext;
                    this.mNext = i3 + 1;
                    c = str2.charAt(i3);
                    this.mTokStart = this.mNext - 1;
                    while (c != '\"' && this.mNext < len) {
                        str2 = this.mLine;
                        i3 = this.mNext;
                        this.mNext = i3 + 1;
                        c = str2.charAt(i3);
                    }
                    if (c == '\"') {
                        this.mTokEnd = this.mNext - 1;
                        if (this.mNext < len) {
                            str = this.mLine;
                            i2 = this.mNext;
                            this.mNext = i2 + 1;
                            if (str.charAt(i2) != ',') {
                                throw new ATParseEx();
                            }
                            return;
                        }
                        return;
                    }
                    throw new ATParseEx();
                } else {
                    throw new ATParseEx();
                }
            } catch (StringIndexOutOfBoundsException e) {
                throw new ATParseEx();
            }
        }
        throw new ATParseEx();
    }

    private char skipWhiteSpace(char c) {
        int len = this.mLine.length();
        while (this.mNext < len && Character.isWhitespace(c)) {
            String str = this.mLine;
            int i = this.mNext;
            this.mNext = i + 1;
            c = str.charAt(i);
        }
        if (!Character.isWhitespace(c)) {
            return c;
        }
        throw new ATParseEx();
    }

    private void skipPrefix() {
        this.mNext = 0;
        int s = this.mLine.length();
        while (this.mNext < s) {
            char c = this.mLine;
            int i = this.mNext;
            this.mNext = i + 1;
            if (c.charAt(i) == ':') {
                return;
            }
        }
        throw new ATParseEx("missing prefix");
    }
}
