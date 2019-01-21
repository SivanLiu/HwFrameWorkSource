package sun.nio.cs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import sun.nio.ch.ChannelInputStream;

public class StreamDecoder extends Reader {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    private static final int MIN_BYTE_BUFFER_SIZE = 32;
    private static volatile boolean channelsAvailable = true;
    private ByteBuffer bb;
    private ReadableByteChannel ch;
    private Charset cs;
    private CharsetDecoder decoder;
    private boolean haveLeftoverChar;
    private InputStream in;
    private volatile boolean isOpen;
    private char leftoverChar;
    private boolean needsFlush;

    private void ensureOpen() throws IOException {
        if (!this.isOpen) {
            throw new IOException("Stream closed");
        }
    }

    public static StreamDecoder forInputStreamReader(InputStream in, Object lock, String charsetName) throws UnsupportedEncodingException {
        String csn = charsetName;
        if (csn == null) {
            csn = Charset.defaultCharset().name();
        }
        try {
            if (Charset.isSupported(csn)) {
                return new StreamDecoder(in, lock, Charset.forName(csn));
            }
        } catch (IllegalCharsetNameException e) {
        }
        throw new UnsupportedEncodingException(csn);
    }

    public static StreamDecoder forInputStreamReader(InputStream in, Object lock, Charset cs) {
        return new StreamDecoder(in, lock, cs);
    }

    public static StreamDecoder forInputStreamReader(InputStream in, Object lock, CharsetDecoder dec) {
        return new StreamDecoder(in, lock, dec);
    }

    public static StreamDecoder forDecoder(ReadableByteChannel ch, CharsetDecoder dec, int minBufferCap) {
        return new StreamDecoder(ch, dec, minBufferCap);
    }

    public String getEncoding() {
        if (isOpen()) {
            return encodingName();
        }
        return null;
    }

    public int read() throws IOException {
        return read0();
    }

    private int read0() throws IOException {
        synchronized (this.lock) {
            if (this.haveLeftoverChar) {
                this.haveLeftoverChar = $assertionsDisabled;
                char c = this.leftoverChar;
                return c;
            }
            char[] cb = new char[2];
            int n = read(cb, 0, 2);
            if (n != -1) {
                switch (n) {
                    case 1:
                        break;
                    case 2:
                        this.leftoverChar = cb[1];
                        this.haveLeftoverChar = true;
                        break;
                    default:
                        return -1;
                }
                char c2 = cb[0];
                return c2;
            }
            return -1;
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0036, code skipped:
            return 1;
     */
    /* JADX WARNING: Missing block: B:32:0x0046, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int read(char[] cbuf, int offset, int length) throws IOException {
        int off = offset;
        int len = length;
        synchronized (this.lock) {
            ensureOpen();
            if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            } else {
                int n = 0;
                if (this.haveLeftoverChar) {
                    cbuf[off] = this.leftoverChar;
                    off++;
                    len--;
                    this.haveLeftoverChar = $assertionsDisabled;
                    n = 1;
                    if (len == 0 || !implReady()) {
                    }
                }
                int c;
                if (len == 1) {
                    c = read0();
                    int i = -1;
                    if (c != -1) {
                        cbuf[off] = (char) c;
                        i = n + 1;
                        return i;
                    } else if (n != 0) {
                        i = n;
                    }
                } else {
                    c = implRead(cbuf, off, off + len) + n;
                    return c;
                }
            }
        }
    }

    public boolean ready() throws IOException {
        boolean z;
        synchronized (this.lock) {
            ensureOpen();
            if (!this.haveLeftoverChar) {
                if (!implReady()) {
                    z = $assertionsDisabled;
                }
            }
            z = true;
        }
        return z;
    }

    public void close() throws IOException {
        synchronized (this.lock) {
            if (this.isOpen) {
                implClose();
                this.isOpen = $assertionsDisabled;
                return;
            }
        }
    }

    private boolean isOpen() {
        return this.isOpen;
    }

    private static FileChannel getChannel(FileInputStream in) {
        if (!channelsAvailable) {
            return null;
        }
        try {
            return in.getChannel();
        } catch (UnsatisfiedLinkError e) {
            channelsAvailable = $assertionsDisabled;
            return null;
        }
    }

    StreamDecoder(InputStream in, Object lock, Charset cs) {
        this(in, lock, cs.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    StreamDecoder(InputStream in, Object lock, CharsetDecoder dec) {
        super(lock);
        this.isOpen = true;
        this.haveLeftoverChar = $assertionsDisabled;
        this.needsFlush = $assertionsDisabled;
        this.cs = dec.charset();
        this.decoder = dec;
        if (this.ch == null) {
            this.in = in;
            this.ch = null;
            this.bb = ByteBuffer.allocate(8192);
        }
        this.bb.flip();
    }

    StreamDecoder(ReadableByteChannel ch, CharsetDecoder dec, int mbc) {
        this.isOpen = true;
        this.haveLeftoverChar = $assertionsDisabled;
        this.needsFlush = $assertionsDisabled;
        this.in = null;
        this.ch = ch;
        this.decoder = dec;
        this.cs = dec.charset();
        int i = 32;
        if (mbc < 0) {
            i = 8192;
        } else if (mbc >= 32) {
            i = mbc;
        }
        this.bb = ByteBuffer.allocate(i);
        this.bb.flip();
    }

    private int readBytes() throws IOException {
        this.bb.compact();
        try {
            int n;
            if (this.ch != null) {
                n = ChannelInputStream.read(this.ch, this.bb);
                if (n < 0) {
                    return n;
                }
            }
            n = this.bb.limit();
            int pos = this.bb.position();
            int n2 = this.in.read(this.bb.array(), this.bb.arrayOffset() + pos, pos <= n ? n - pos : 0);
            if (n2 < 0) {
                this.bb.flip();
                return n2;
            } else if (n2 != 0) {
                this.bb.position(pos + n2);
            } else {
                throw new IOException("Underlying input stream returned zero bytes");
            }
            this.bb.flip();
            return this.bb.remaining();
        } finally {
            this.bb.flip();
        }
    }

    int implRead(char[] cbuf, int off, int end) throws IOException {
        CoderResult cr;
        CharBuffer cb = CharBuffer.wrap(cbuf, off, end - off);
        if (cb.position() != 0) {
            cb = cb.slice();
        }
        if (this.needsFlush) {
            CoderResult cr2 = this.decoder.flush(cb);
            if (cr2.isOverflow()) {
                return cb.position();
            }
            if (!cr2.isUnderflow()) {
                cr2.throwException();
            } else if (cb.position() == 0) {
                return -1;
            } else {
                return cb.position();
            }
        }
        boolean eof = $assertionsDisabled;
        while (true) {
            cr = this.decoder.decode(this.bb, cb, eof);
            if (cr.isUnderflow()) {
                if (eof || !cb.hasRemaining() || (cb.position() > 0 && !inReady())) {
                    break;
                } else if (readBytes() < 0) {
                    eof = true;
                }
            } else if (cr.isOverflow()) {
                break;
            } else {
                cr.throwException();
            }
        }
        if (eof) {
            cr = this.decoder.flush(cb);
            if (cr.isOverflow()) {
                this.needsFlush = true;
                return cb.position();
            }
            this.decoder.reset();
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
        }
        if (cb.position() == 0 && eof) {
            return -1;
        }
        return cb.position();
    }

    String encodingName() {
        if (this.cs instanceof HistoricallyNamedCharset) {
            return ((HistoricallyNamedCharset) this.cs).historicalName();
        }
        return this.cs.name();
    }

    private boolean inReady() {
        boolean z = $assertionsDisabled;
        try {
            if ((this.in != null && this.in.available() > 0) || (this.ch instanceof FileChannel)) {
                z = true;
            }
            return z;
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    boolean implReady() {
        return (this.bb.hasRemaining() || inReady()) ? true : $assertionsDisabled;
    }

    void implClose() throws IOException {
        if (this.ch != null) {
            this.ch.close();
        } else {
            this.in.close();
        }
    }
}
