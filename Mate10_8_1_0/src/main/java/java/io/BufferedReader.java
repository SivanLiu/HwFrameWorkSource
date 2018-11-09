package java.io;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BufferedReader extends Reader {
    private static final int INVALIDATED = -2;
    private static final int UNMARKED = -1;
    private static int defaultCharBufferSize = 8192;
    private static int defaultExpectedLineLength = 80;
    private char[] cb;
    private Reader in;
    private int markedChar;
    private boolean markedSkipLF;
    private int nChars;
    private int nextChar;
    private int readAheadLimit;
    private boolean skipLF;

    public BufferedReader(Reader in, int sz) {
        super(in);
        this.markedChar = -1;
        this.readAheadLimit = 0;
        this.skipLF = false;
        this.markedSkipLF = false;
        if (sz <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.in = in;
        this.cb = new char[sz];
        this.nChars = 0;
        this.nextChar = 0;
    }

    public BufferedReader(Reader in) {
        this(in, defaultCharBufferSize);
    }

    private void ensureOpen() throws IOException {
        if (this.in == null) {
            throw new IOException("Stream closed");
        }
    }

    private void fill() throws IOException {
        int dst;
        int n;
        if (this.markedChar <= -1) {
            dst = 0;
        } else {
            int delta = this.nextChar - this.markedChar;
            if (delta >= this.readAheadLimit) {
                this.markedChar = -2;
                this.readAheadLimit = 0;
                dst = 0;
            } else {
                if (this.readAheadLimit <= this.cb.length) {
                    System.arraycopy(this.cb, this.markedChar, this.cb, 0, delta);
                    this.markedChar = 0;
                    dst = delta;
                } else {
                    int nlength = this.cb.length * 2;
                    if (nlength > this.readAheadLimit) {
                        nlength = this.readAheadLimit;
                    }
                    char[] ncb = new char[nlength];
                    System.arraycopy(this.cb, this.markedChar, ncb, 0, delta);
                    this.cb = ncb;
                    this.markedChar = 0;
                    dst = delta;
                }
                this.nChars = delta;
                this.nextChar = delta;
            }
        }
        do {
            n = this.in.read(this.cb, dst, this.cb.length - dst);
        } while (n == 0);
        if (n > 0) {
            this.nChars = dst + n;
            this.nextChar = dst;
        }
    }

    public int read() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            while (true) {
                if (this.nextChar >= this.nChars) {
                    fill();
                    if (this.nextChar >= this.nChars) {
                        return -1;
                    }
                }
                if (this.skipLF) {
                    this.skipLF = false;
                    if (this.cb[this.nextChar] == '\n') {
                        this.nextChar++;
                    }
                }
                break;
            }
            char[] cArr = this.cb;
            int i = this.nextChar;
            this.nextChar = i + 1;
            char c = cArr[i];
            return c;
        }
    }

    private int read1(char[] cbuf, int off, int len) throws IOException {
        if (this.nextChar >= this.nChars) {
            if (len >= this.cb.length && this.markedChar <= -1 && (this.skipLF ^ 1) != 0) {
                return this.in.read(cbuf, off, len);
            }
            fill();
        }
        if (this.nextChar >= this.nChars) {
            return -1;
        }
        if (this.skipLF) {
            this.skipLF = false;
            if (this.cb[this.nextChar] == '\n') {
                this.nextChar++;
                if (this.nextChar >= this.nChars) {
                    fill();
                }
                if (this.nextChar >= this.nChars) {
                    return -1;
                }
            }
        }
        int n = Math.min(len, this.nChars - this.nextChar);
        System.arraycopy(this.cb, this.nextChar, cbuf, off, n);
        this.nextChar += n;
        return n;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (off >= 0 && off <= cbuf.length && len >= 0) {
                if (off + len <= cbuf.length && off + len >= 0) {
                    if (len != 0) {
                        int n = read1(cbuf, off, len);
                        if (n > 0) {
                            while (n < len) {
                                if (!this.in.ready()) {
                                    break;
                                }
                                int n1 = read1(cbuf, off + n, len - n);
                                if (n1 <= 0) {
                                    break;
                                }
                                n += n1;
                            }
                        } else {
                            return n;
                        }
                    }
                    return 0;
                }
            }
            throw new IndexOutOfBoundsException();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    String readLine(boolean ignoreLF) throws IOException {
        Throwable th;
        synchronized (this.lock) {
            try {
                boolean omitLF;
                StringBuffer s;
                ensureOpen();
                if (ignoreLF) {
                    omitLF = true;
                    s = null;
                } else {
                    omitLF = this.skipLF;
                    s = null;
                }
                while (true) {
                    StringBuffer s2;
                    try {
                        if (this.nextChar >= this.nChars) {
                            fill();
                        }
                        if (this.nextChar >= this.nChars) {
                            break;
                        }
                        boolean eol = false;
                        char c = '\u0000';
                        if (omitLF) {
                            if (this.cb[this.nextChar] == '\n') {
                                this.nextChar++;
                            }
                        }
                        this.skipLF = false;
                        omitLF = false;
                        int i = this.nextChar;
                        while (i < this.nChars) {
                            c = this.cb[i];
                            if (c == '\n' || c == '\r') {
                                eol = true;
                                break;
                            }
                            i++;
                        }
                        int startChar = this.nextChar;
                        this.nextChar = i;
                        if (eol) {
                            break;
                        }
                        if (s == null) {
                            s2 = new StringBuffer(defaultExpectedLineLength);
                        } else {
                            s2 = s;
                        }
                        s2.append(this.cb, startChar, i - startChar);
                        s = s2;
                    } catch (Throwable th2) {
                        th = th2;
                        s2 = s;
                    }
                }
                if (s == null || s.length() <= 0) {
                } else {
                    String stringBuffer = s.toString();
                    return stringBuffer;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    public String readLine() throws IOException {
        return readLine(false);
    }

    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("skip value is negative");
        }
        long j;
        synchronized (this.lock) {
            ensureOpen();
            long r = n;
            while (r > 0) {
                if (this.nextChar >= this.nChars) {
                    fill();
                }
                if (this.nextChar >= this.nChars) {
                    break;
                }
                if (this.skipLF) {
                    this.skipLF = false;
                    if (this.cb[this.nextChar] == '\n') {
                        this.nextChar++;
                    }
                }
                long d = (long) (this.nChars - this.nextChar);
                if (r <= d) {
                    this.nextChar = (int) (((long) this.nextChar) + r);
                    r = 0;
                    break;
                }
                r -= d;
                this.nextChar = this.nChars;
            }
            j = n - r;
        }
        return j;
    }

    public boolean ready() throws IOException {
        boolean ready;
        synchronized (this.lock) {
            ensureOpen();
            if (this.skipLF) {
                if (this.nextChar >= this.nChars && this.in.ready()) {
                    fill();
                }
                if (this.nextChar < this.nChars) {
                    if (this.cb[this.nextChar] == '\n') {
                        this.nextChar++;
                    }
                    this.skipLF = false;
                }
            }
            ready = this.nextChar >= this.nChars ? this.in.ready() : true;
        }
        return ready;
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit < 0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        synchronized (this.lock) {
            ensureOpen();
            this.readAheadLimit = readAheadLimit;
            this.markedChar = this.nextChar;
            this.markedSkipLF = this.skipLF;
        }
    }

    public void reset() throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (this.markedChar < 0) {
                String str;
                if (this.markedChar == -2) {
                    str = "Mark invalid";
                } else {
                    str = "Stream not marked";
                }
                throw new IOException(str);
            }
            this.nextChar = this.markedChar;
            this.skipLF = this.markedSkipLF;
        }
    }

    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.in == null) {
                return;
            }
            try {
                this.in.close();
                this.in = null;
                this.cb = null;
            } catch (Throwable th) {
                this.in = null;
                this.cb = null;
            }
        }
    }

    public Stream<String> lines() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<String>() {
            String nextLine = null;

            public boolean hasNext() {
                boolean z = true;
                if (this.nextLine != null) {
                    return true;
                }
                try {
                    this.nextLine = BufferedReader.this.readLine();
                    if (this.nextLine == null) {
                        z = false;
                    }
                    return z;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            public String next() {
                if (this.nextLine != null || hasNext()) {
                    String line = this.nextLine;
                    this.nextLine = null;
                    return line;
                }
                throw new NoSuchElementException();
            }
        }, 272), false);
    }
}
