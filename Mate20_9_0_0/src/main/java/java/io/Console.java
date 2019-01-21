package java.io;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Formatter;
import sun.nio.cs.StreamDecoder;
import sun.nio.cs.StreamEncoder;

public final class Console implements Flushable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static Console cons;
    private static boolean echoOff;
    private Charset cs;
    private Formatter formatter;
    private Writer out;
    private PrintWriter pw;
    private char[] rcb;
    private Object readLock;
    private Reader reader;
    private Object writeLock;

    class LineReader extends Reader {
        private char[] cb = new char[1024];
        private Reader in;
        boolean leftoverLF = false;
        private int nChars = 0;
        private int nextChar = 0;

        LineReader(Reader in) {
            this.in = in;
        }

        public void close() {
        }

        public boolean ready() throws IOException {
            return this.in.ready();
        }

        public int read(char[] cbuf, int offset, int length) throws IOException {
            int off = offset;
            int end = offset + length;
            if (offset < 0 || offset > cbuf.length || length < 0 || end < 0 || end > cbuf.length) {
                throw new IndexOutOfBoundsException();
            }
            synchronized (Console.this.readLock) {
                int i;
                boolean eof = false;
                int end2 = end;
                char[] cbuf2 = cbuf;
                char c = 0;
                do {
                    try {
                        int n;
                        if (this.nextChar >= this.nChars) {
                            n = 0;
                            do {
                                n = this.in.read(this.cb, 0, this.cb.length);
                            } while (n == 0);
                            if (n > 0) {
                                this.nChars = n;
                                this.nextChar = 0;
                                if (!(n >= this.cb.length || this.cb[n - 1] == 10 || this.cb[n - 1] == 13)) {
                                    eof = true;
                                }
                            } else if (off - offset == 0) {
                                return -1;
                            } else {
                                i = off - offset;
                                return i;
                            }
                        }
                        if (this.leftoverLF && cbuf2 == Console.this.rcb && this.cb[this.nextChar] == 10) {
                            this.nextChar++;
                        }
                        this.leftoverLF = false;
                        while (this.nextChar < this.nChars) {
                            n = off + 1;
                            try {
                                char c2 = this.cb[this.nextChar];
                                cbuf2[off] = c2;
                                c = c2;
                                char[] cArr = this.cb;
                                int i2 = this.nextChar;
                                this.nextChar = i2 + 1;
                                cArr[i2] = 0;
                                if (c == 10) {
                                    off = n - offset;
                                    return off;
                                } else if (c == 13) {
                                    if (n == end2) {
                                        if (cbuf2 == Console.this.rcb) {
                                            cbuf2 = Console.this.grow();
                                            end2 = cbuf2.length;
                                        } else {
                                            this.leftoverLF = true;
                                            off = n - offset;
                                            return off;
                                        }
                                    }
                                    if (this.nextChar == this.nChars && this.in.ready()) {
                                        this.nChars = this.in.read(this.cb, 0, this.cb.length);
                                        this.nextChar = 0;
                                    }
                                    if (this.nextChar >= this.nChars || this.cb[this.nextChar] != 10) {
                                        off = n;
                                    } else {
                                        off = n + 1;
                                        cbuf2[n] = 10;
                                        this.nextChar++;
                                    }
                                    i = off - offset;
                                    return i;
                                } else {
                                    if (n == end2) {
                                        if (cbuf2 == Console.this.rcb) {
                                            cbuf2 = Console.this.grow();
                                            end2 = cbuf2.length;
                                        } else {
                                            off = n - offset;
                                            return off;
                                        }
                                    }
                                    off = n;
                                }
                            } catch (Throwable th) {
                                cbuf = th;
                                off = n;
                                throw cbuf;
                            }
                        }
                    } catch (Throwable th2) {
                        cbuf = th2;
                        throw cbuf;
                    }
                } while (!eof);
                i = off - offset;
                return i;
            }
        }
    }

    private static native boolean echo(boolean z) throws IOException;

    private static native String encoding();

    private static native boolean istty();

    public PrintWriter writer() {
        return this.pw;
    }

    public Reader reader() {
        return this.reader;
    }

    public Console format(String fmt, Object... args) {
        this.formatter.format(fmt, args).flush();
        return this;
    }

    public Console printf(String format, Object... args) {
        return format(format, args);
    }

    public String readLine(String fmt, Object... args) {
        String line = null;
        synchronized (this.writeLock) {
            synchronized (this.readLock) {
                if (fmt.length() != 0) {
                    this.pw.format(fmt, args);
                }
                try {
                    char[] ca = readline(null);
                    if (ca != null) {
                        line = new String(ca);
                    }
                } catch (IOException x) {
                    throw new IOError(x);
                }
            }
        }
        return line;
    }

    public String readLine() {
        return readLine("", new Object[0]);
    }

    public char[] readPassword(String fmt, Object... args) {
        IOError ioe;
        char[] passwd;
        synchronized (this.writeLock) {
            synchronized (this.readLock) {
                try {
                    echoOff = echo(false);
                    ioe = null;
                    try {
                        if (fmt.length() != 0) {
                            this.pw.format(fmt, args);
                        }
                        passwd = readline(true);
                        echoOff = echo(true);
                    } catch (IOException x) {
                        ioe = new IOError(x);
                        try {
                            echoOff = echo(true);
                        } catch (IOException x2) {
                            ioe.addSuppressed(x2);
                        }
                    }
                } catch (IOException x3) {
                    throw new IOError(x3);
                } catch (IOException x22) {
                    if (ioe == null) {
                        ioe = new IOError(x22);
                    } else {
                        ioe.addSuppressed(x22);
                    }
                } catch (Throwable th) {
                }
                if (ioe == null) {
                    this.pw.println();
                }
                throw ioe;
            }
        }
        return passwd;
    }

    public char[] readPassword() {
        return readPassword("", new Object[0]);
    }

    public void flush() {
        this.pw.flush();
    }

    private char[] readline(boolean zeroOut) throws IOException {
        int len = this.reader.read(this.rcb, 0, this.rcb.length);
        if (len < 0) {
            return null;
        }
        if (this.rcb[len - 1] == 13) {
            len--;
        } else if (this.rcb[len - 1] == 10) {
            len--;
            if (len > 0 && this.rcb[len - 1] == 13) {
                len--;
            }
        }
        Object b = new char[len];
        if (len > 0) {
            System.arraycopy(this.rcb, 0, b, 0, len);
            if (zeroOut) {
                Arrays.fill(this.rcb, 0, len, ' ');
            }
        }
        return b;
    }

    private char[] grow() {
        Object t = new char[(this.rcb.length * 2)];
        System.arraycopy(this.rcb, 0, t, 0, this.rcb.length);
        this.rcb = t;
        return this.rcb;
    }

    public static Console console() {
        if (!istty()) {
            return null;
        }
        if (cons == null) {
            cons = new Console();
        }
        return cons;
    }

    private Console() {
        this(new FileInputStream(FileDescriptor.in), new FileOutputStream(FileDescriptor.out));
    }

    private Console(InputStream inStream, OutputStream outStream) {
        this.readLock = new Object();
        this.writeLock = new Object();
        String csname = encoding();
        if (csname != null) {
            try {
                this.cs = Charset.forName(csname);
            } catch (Exception e) {
            }
        }
        if (this.cs == null) {
            this.cs = Charset.defaultCharset();
        }
        this.out = StreamEncoder.forOutputStreamWriter(outStream, this.writeLock, this.cs);
        this.pw = new PrintWriter(this.out, true) {
            public void close() {
            }
        };
        this.formatter = new Formatter(this.out);
        this.reader = new LineReader(StreamDecoder.forInputStreamReader(inStream, this.readLock, this.cs));
        this.rcb = new char[1024];
    }
}
