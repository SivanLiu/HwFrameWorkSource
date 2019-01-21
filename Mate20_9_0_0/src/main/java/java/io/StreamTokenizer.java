package java.io;

import java.util.Arrays;

public class StreamTokenizer {
    private static final byte CT_ALPHA = (byte) 4;
    private static final byte CT_COMMENT = (byte) 16;
    private static final byte CT_DIGIT = (byte) 2;
    private static final byte CT_QUOTE = (byte) 8;
    private static final byte CT_WHITESPACE = (byte) 1;
    private static final int NEED_CHAR = Integer.MAX_VALUE;
    private static final int SKIP_LF = 2147483646;
    public static final int TT_EOF = -1;
    public static final int TT_EOL = 10;
    private static final int TT_NOTHING = -4;
    public static final int TT_NUMBER = -2;
    public static final int TT_WORD = -3;
    private int LINENO;
    private char[] buf;
    private byte[] ctype;
    private boolean eolIsSignificantP;
    private boolean forceLower;
    private InputStream input;
    public double nval;
    private int peekc;
    private boolean pushedBack;
    private Reader reader;
    private boolean slashSlashCommentsP;
    private boolean slashStarCommentsP;
    public String sval;
    public int ttype;

    private StreamTokenizer() {
        this.reader = null;
        this.input = null;
        this.buf = new char[20];
        this.peekc = Integer.MAX_VALUE;
        this.LINENO = 1;
        this.eolIsSignificantP = false;
        this.slashSlashCommentsP = false;
        this.slashStarCommentsP = false;
        this.ctype = new byte[256];
        this.ttype = -4;
        wordChars(97, 122);
        wordChars(65, 90);
        wordChars(160, 255);
        whitespaceChars(0, 32);
        commentChar(47);
        quoteChar(34);
        quoteChar(39);
        parseNumbers();
    }

    @Deprecated
    public StreamTokenizer(InputStream is) {
        this();
        if (is != null) {
            this.input = is;
            return;
        }
        throw new NullPointerException();
    }

    public StreamTokenizer(Reader r) {
        this();
        if (r != null) {
            this.reader = r;
            return;
        }
        throw new NullPointerException();
    }

    public void resetSyntax() {
        int i = this.ctype.length;
        while (true) {
            i--;
            if (i >= 0) {
                this.ctype[i] = (byte) 0;
            } else {
                return;
            }
        }
    }

    public void wordChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi >= this.ctype.length) {
            hi = this.ctype.length - 1;
        }
        while (low <= hi) {
            byte[] bArr = this.ctype;
            int low2 = low + 1;
            bArr[low] = (byte) (bArr[low] | 4);
            low = low2;
        }
    }

    public void whitespaceChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi >= this.ctype.length) {
            hi = this.ctype.length - 1;
        }
        while (low <= hi) {
            int low2 = low + 1;
            this.ctype[low] = (byte) 1;
            low = low2;
        }
    }

    public void ordinaryChars(int low, int hi) {
        if (low < 0) {
            low = 0;
        }
        if (hi >= this.ctype.length) {
            hi = this.ctype.length - 1;
        }
        while (low <= hi) {
            int low2 = low + 1;
            this.ctype[low] = (byte) 0;
            low = low2;
        }
    }

    public void ordinaryChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = (byte) 0;
        }
    }

    public void commentChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = (byte) 16;
        }
    }

    public void quoteChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = (byte) 8;
        }
    }

    public void parseNumbers() {
        for (int i = 48; i <= 57; i++) {
            byte[] bArr = this.ctype;
            bArr[i] = (byte) (bArr[i] | 2);
        }
        byte[] bArr2 = this.ctype;
        bArr2[46] = (byte) (bArr2[46] | 2);
        bArr2 = this.ctype;
        bArr2[45] = (byte) (bArr2[45] | 2);
    }

    public void eolIsSignificant(boolean flag) {
        this.eolIsSignificantP = flag;
    }

    public void slashStarComments(boolean flag) {
        this.slashStarCommentsP = flag;
    }

    public void slashSlashComments(boolean flag) {
        this.slashSlashCommentsP = flag;
    }

    public void lowerCaseMode(boolean fl) {
        this.forceLower = fl;
    }

    private int read() throws IOException {
        if (this.reader != null) {
            return this.reader.read();
        }
        if (this.input != null) {
            return this.input.read();
        }
        throw new IllegalStateException();
    }

    public int nextToken() throws IOException {
        int seendot = 0;
        if (this.pushedBack) {
            this.pushedBack = false;
            return this.ttype;
        }
        byte[] ct = this.ctype;
        this.sval = null;
        int c = this.peekc;
        if (c < 0) {
            c = Integer.MAX_VALUE;
        }
        if (c == SKIP_LF) {
            c = read();
            if (c < 0) {
                this.ttype = -1;
                return -1;
            } else if (c == 10) {
                c = Integer.MAX_VALUE;
            }
        }
        int i = Integer.MAX_VALUE;
        if (c == Integer.MAX_VALUE) {
            c = read();
            if (c < 0) {
                this.ttype = -1;
                return -1;
            }
        }
        this.ttype = c;
        this.peekc = Integer.MAX_VALUE;
        int ctype = c < 256 ? ct[c] : 4;
        while ((ctype & 1) != 0) {
            if (c == 13) {
                this.LINENO++;
                if (this.eolIsSignificantP) {
                    this.peekc = SKIP_LF;
                    this.ttype = 10;
                    return 10;
                }
                c = read();
                if (c == 10) {
                    c = read();
                }
            } else {
                if (c == 10) {
                    this.LINENO++;
                    if (this.eolIsSignificantP) {
                        this.ttype = 10;
                        return 10;
                    }
                }
                c = read();
            }
            if (c < 0) {
                this.ttype = -1;
                return -1;
            }
            byte ctype2 = c < 256 ? ct[c] : (byte) 4;
        }
        int decexp;
        int c2;
        if ((ctype2 & 2) != 0) {
            boolean neg = false;
            if (c == 45) {
                c = read();
                if (c == 46 || (c >= 48 && c <= 57)) {
                    neg = true;
                } else {
                    this.peekc = c;
                    this.ttype = 45;
                    return 45;
                }
            }
            double v = 0.0d;
            decexp = 0;
            while (true) {
                if (c == 46 && seendot == 0) {
                    seendot = 1;
                } else if (48 > c || c > 57) {
                    this.peekc = c;
                } else {
                    decexp += seendot;
                    v = (10.0d * v) + ((double) (c - 48));
                }
                c = read();
            }
            this.peekc = c;
            if (decexp != 0) {
                double denom = 10.0d;
                for (decexp--; decexp > 0; decexp--) {
                    denom *= 10.0d;
                }
                v /= denom;
            }
            this.nval = neg ? -v : v;
            this.ttype = -2;
            return -2;
        } else if ((ctype2 & 4) != 0) {
            int i2;
            c2 = c;
            c = 0;
            while (true) {
                if (c >= this.buf.length) {
                    this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                }
                i2 = c + 1;
                this.buf[c] = (char) c2;
                c2 = read();
                byte b = c2 < 0 ? (byte) 1 : c2 < 256 ? ct[c2] : (byte) 4;
                if ((b & 6) == 0) {
                    break;
                }
                c = i2;
            }
            this.peekc = c2;
            this.sval = String.copyValueOf(this.buf, 0, i2);
            if (this.forceLower) {
                this.sval = this.sval.toLowerCase();
            }
            this.ttype = -3;
            return -3;
        } else if ((ctype2 & 8) != 0) {
            this.ttype = c;
            c2 = 0;
            int d = read();
            while (d >= 0 && d != this.ttype && d != 10 && d != 13) {
                if (d == 92) {
                    c = read();
                    decexp = c;
                    if (c < 48 || c > 55) {
                        if (c == 102) {
                            c = 12;
                        } else if (c == 110) {
                            c = 10;
                        } else if (c == 114) {
                            c = 13;
                        } else if (c == 116) {
                            c = 9;
                        } else if (c != 118) {
                            switch (c) {
                                case 97:
                                    c = 7;
                                    break;
                                case 98:
                                    c = 8;
                                    break;
                            }
                        } else {
                            c = 11;
                        }
                        d = read();
                    } else {
                        c -= 48;
                        int c22 = read();
                        if (48 > c22 || c22 > 55) {
                            d = c22;
                        } else {
                            c = (c << 3) + (c22 - 48);
                            c22 = read();
                            if (48 > c22 || c22 > 55 || decexp > 51) {
                                d = c22;
                            } else {
                                c = (c << 3) + (c22 - 48);
                                d = read();
                            }
                        }
                    }
                } else {
                    c = d;
                    d = read();
                }
                if (c2 >= this.buf.length) {
                    this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                }
                int i3 = c2 + 1;
                this.buf[c2] = (char) c;
                c2 = i3;
            }
            if (d != this.ttype) {
                i = d;
            }
            this.peekc = i;
            this.sval = String.copyValueOf(this.buf, 0, c2);
            return this.ttype;
        } else if (c == 47 && (this.slashSlashCommentsP || this.slashStarCommentsP)) {
            c = read();
            if (c == 42 && this.slashStarCommentsP) {
                while (true) {
                    i = read();
                    c = i;
                    if (i == 47 && seendot == 42) {
                        return nextToken();
                    }
                    if (c == 13) {
                        this.LINENO++;
                        c = read();
                        if (c == 10) {
                            c = read();
                        }
                    } else if (c == 10) {
                        this.LINENO++;
                        c = read();
                    }
                    if (c < 0) {
                        this.ttype = -1;
                        return -1;
                    }
                    seendot = c;
                }
            } else if (c == 47 && this.slashSlashCommentsP) {
                while (true) {
                    seendot = read();
                    c = seendot;
                    if (seendot == 10 || c == 13 || c < 0) {
                        this.peekc = c;
                    }
                }
                this.peekc = c;
                return nextToken();
            } else if ((ct[47] & 16) != 0) {
                while (true) {
                    seendot = read();
                    c = seendot;
                    if (seendot == 10 || c == 13 || c < 0) {
                        this.peekc = c;
                    }
                }
                this.peekc = c;
                return nextToken();
            } else {
                this.peekc = c;
                this.ttype = 47;
                return 47;
            }
        } else if ((ctype2 & 16) != 0) {
            while (true) {
                seendot = read();
                c = seendot;
                if (seendot == 10 || c == 13 || c < 0) {
                    this.peekc = c;
                }
            }
            this.peekc = c;
            return nextToken();
        } else {
            this.ttype = c;
            return c;
        }
    }

    public void pushBack() {
        if (this.ttype != -4) {
            this.pushedBack = true;
        }
    }

    public int lineno() {
        return this.LINENO;
    }

    public String toString() {
        String ret;
        int i = this.ttype;
        if (i != 10) {
            switch (i) {
                case -4:
                    ret = "NOTHING";
                    break;
                case -3:
                    ret = this.sval;
                    break;
                case -2:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("n=");
                    stringBuilder.append(this.nval);
                    ret = stringBuilder.toString();
                    break;
                case -1:
                    ret = "EOF";
                    break;
                default:
                    if (this.ttype < 256 && (this.ctype[this.ttype] & 8) != 0) {
                        ret = this.sval;
                        break;
                    }
                    ret = new String(new char[]{'\'', '\'', (char) this.ttype});
                    break;
            }
        }
        ret = "EOL";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Token[");
        stringBuilder2.append(ret);
        stringBuilder2.append("], line ");
        stringBuilder2.append(this.LINENO);
        return stringBuilder2.toString();
    }
}
