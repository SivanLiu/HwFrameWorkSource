package sun.net;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TelnetInputStream extends FilterInputStream {
    public boolean binaryMode = false;
    boolean seenCR = false;
    boolean stickyCRLF = false;

    public TelnetInputStream(InputStream fd, boolean binary) {
        super(fd);
        this.binaryMode = binary;
    }

    public void setStickyCRLF(boolean on) {
        this.stickyCRLF = on;
    }

    public int read() throws IOException {
        if (this.binaryMode) {
            return super.read();
        }
        if (this.seenCR) {
            this.seenCR = false;
            return 10;
        }
        int read = super.read();
        int c = read;
        if (read != 13) {
            return c;
        }
        read = super.read();
        c = read;
        if (read == 0) {
            return 13;
        }
        if (read != 10) {
            throw new TelnetProtocolException("misplaced CR in input");
        } else if (!this.stickyCRLF) {
            return 10;
        } else {
            this.seenCR = true;
            return 13;
        }
    }

    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    public int read(byte[] bytes, int offStart, int length) throws IOException {
        if (this.binaryMode) {
            return super.read(bytes, offStart, length);
        }
        int i;
        int off = offStart;
        while (true) {
            i = -1;
            length--;
            if (length < 0) {
                break;
            }
            int c = read();
            if (c == -1) {
                break;
            }
            i = off + 1;
            bytes[off] = (byte) c;
            off = i;
        }
        if (off > offStart) {
            i = off - offStart;
        }
        return i;
    }
}
