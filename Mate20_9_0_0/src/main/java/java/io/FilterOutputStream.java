package java.io;

public class FilterOutputStream extends OutputStream {
    protected OutputStream out;

    public FilterOutputStream(OutputStream out) {
        this.out = out;
    }

    public void write(int b) throws IOException {
        this.out.write(b);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if ((((off | len) | (b.length - (len + off))) | (off + len)) >= 0) {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
            return;
        }
        throw new IndexOutOfBoundsException();
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void close() throws IOException {
        Throwable th;
        Throwable th2;
        OutputStream ostream = this.out;
        try {
            flush();
            if (ostream != null) {
                ostream.close();
                return;
            }
            return;
        } catch (Throwable th3) {
            th = th3;
        }
        if (ostream != null) {
            if (th2 != null) {
                try {
                    ostream.close();
                } catch (Throwable th4) {
                    th2.addSuppressed(th4);
                }
            } else {
                ostream.close();
            }
        }
        throw th;
        throw th;
    }
}
