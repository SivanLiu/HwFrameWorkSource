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
        if (sz > 0) {
            this.in = in;
            this.cb = new char[sz];
            this.nChars = 0;
            this.nextChar = 0;
            return;
        }
        throw new IllegalArgumentException("Buffer size <= 0");
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
        int dst2;
        if (this.markedChar <= -1) {
            dst = 0;
        } else {
            dst = this.nextChar - this.markedChar;
            if (dst >= this.readAheadLimit) {
                this.markedChar = -2;
                this.readAheadLimit = 0;
                dst2 = 0;
            } else {
                if (this.readAheadLimit <= this.cb.length) {
                    System.arraycopy(this.cb, this.markedChar, this.cb, 0, dst);
                    this.markedChar = 0;
                    dst2 = dst;
                } else {
                    dst2 = this.cb.length * 2;
                    if (dst2 > this.readAheadLimit) {
                        dst2 = this.readAheadLimit;
                    }
                    Object ncb = new char[dst2];
                    System.arraycopy(this.cb, this.markedChar, ncb, 0, dst);
                    this.cb = ncb;
                    this.markedChar = 0;
                    dst2 = dst;
                }
                this.nChars = dst;
                this.nextChar = dst;
            }
            dst = dst2;
        }
        do {
            dst2 = this.in.read(this.cb, dst, this.cb.length - dst);
        } while (dst2 == 0);
        if (dst2 > 0) {
            this.nChars = dst + dst2;
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
                if (!this.skipLF) {
                    break;
                }
                this.skipLF = false;
                if (this.cb[this.nextChar] != 10) {
                    break;
                }
                this.nextChar++;
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
            if (len >= this.cb.length && this.markedChar <= -1 && !this.skipLF) {
                return this.in.read(cbuf, off, len);
            }
            fill();
        }
        if (this.nextChar >= this.nChars) {
            return -1;
        }
        if (this.skipLF) {
            this.skipLF = false;
            if (this.cb[this.nextChar] == 10) {
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
        System.arraycopy(this.cb, this.nextChar, (Object) cbuf, off, n);
        this.nextChar += n;
        return n;
    }

    /* JADX WARNING: Missing block: B:28:0x003b, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized (this.lock) {
            ensureOpen();
            if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            } else {
                int n = read1(cbuf, off, len);
                if (n <= 0) {
                    return n;
                }
                while (n < len && this.in.ready()) {
                    int n1 = read1(cbuf, off + n, len - n);
                    if (n1 <= 0) {
                        break;
                    }
                    n += n1;
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0019  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0033  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x0022 A:{SYNTHETIC} */
    /* JADX WARNING: Missing block: B:47:0x0087, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    String readLine(boolean ignoreLF) throws IOException {
        StringBuffer s = null;
        synchronized (this.lock) {
            boolean omitLF;
            int i;
            int startChar;
            ensureOpen();
            if (!ignoreLF) {
                if (!this.skipLF) {
                    omitLF = false;
                    while (true) {
                        if (this.nextChar >= this.nChars) {
                            fill();
                        }
                        String str;
                        if (this.nextChar >= this.nChars) {
                            boolean eol = false;
                            char c = 0;
                            if (omitLF && this.cb[this.nextChar] == 10) {
                                this.nextChar++;
                            }
                            this.skipLF = false;
                            omitLF = false;
                            i = this.nextChar;
                            while (i < this.nChars) {
                                c = this.cb[i];
                                if (c != 10) {
                                    if (c != 13) {
                                        i++;
                                    }
                                }
                                eol = true;
                            }
                            startChar = this.nextChar;
                            this.nextChar = i;
                            if (eol) {
                                if (s == null) {
                                    str = new String(this.cb, startChar, i - startChar);
                                } else {
                                    s.append(this.cb, startChar, i - startChar);
                                    str = s.toString();
                                }
                                this.nextChar++;
                                if (c == 13) {
                                    this.skipLF = true;
                                }
                            } else {
                                if (s == null) {
                                    s = new StringBuffer(defaultExpectedLineLength);
                                }
                                s.append(this.cb, startChar, i - startChar);
                            }
                        } else if (s == null || s.length() <= 0) {
                            return null;
                        } else {
                            str = s.toString();
                            return str;
                        }
                    }
                }
            }
            omitLF = true;
            while (true) {
                if (this.nextChar >= this.nChars) {
                }
                if (this.nextChar >= this.nChars) {
                }
                s.append(this.cb, startChar, i - startChar);
            }
        }
    }

    public String readLine() throws IOException {
        return readLine(false);
    }

    public long skip(long n) throws IOException {
        if (n >= 0) {
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
                        if (this.cb[this.nextChar] == 10) {
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
        throw new IllegalArgumentException("skip value is negative");
    }

    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            ensureOpen();
            z = false;
            if (this.skipLF) {
                if (this.nextChar >= this.nChars && this.in.ready()) {
                    fill();
                }
                if (this.nextChar < this.nChars) {
                    if (this.cb[this.nextChar] == 10) {
                        this.nextChar++;
                    }
                    this.skipLF = false;
                }
            }
            if (this.nextChar >= this.nChars) {
                if (!this.in.ready()) {
                }
            }
            z = true;
        }
        return z;
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit >= 0) {
            synchronized (this.lock) {
                ensureOpen();
                this.readAheadLimit = readAheadLimit;
                this.markedChar = this.nextChar;
                this.markedSkipLF = this.skipLF;
            }
            return;
        }
        throw new IllegalArgumentException("Read-ahead limit < 0");
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
            } finally {
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
