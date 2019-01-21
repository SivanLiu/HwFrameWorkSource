package org.bouncycastle.est;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.bouncycastle.util.encoders.Base64;

class CTEBase64InputStream extends InputStream {
    protected final byte[] data = new byte[768];
    protected final OutputStream dataOutputStream;
    protected boolean end;
    protected final Long max;
    protected final byte[] rawBuf = new byte[1024];
    protected long read;
    protected int rp;
    protected final InputStream src;
    protected int wp;

    public CTEBase64InputStream(InputStream inputStream, Long l) {
        this.src = inputStream;
        this.dataOutputStream = new OutputStream() {
            public void write(int i) throws IOException {
                byte[] bArr = CTEBase64InputStream.this.data;
                CTEBase64InputStream cTEBase64InputStream = CTEBase64InputStream.this;
                int i2 = cTEBase64InputStream.wp;
                cTEBase64InputStream.wp = i2 + 1;
                bArr[i2] = (byte) i;
            }
        };
        this.max = l;
    }

    public void close() throws IOException {
        this.src.close();
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0056 A:{SYNTHETIC, Splitter:B:21:0x0056} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int pullFromSrc() throws IOException {
        if (this.read >= this.max.longValue()) {
            return -1;
        }
        int i = 0;
        do {
            int read = this.src.read();
            if (read >= 33 || read == 13 || read == 10) {
                if (i < this.rawBuf.length) {
                    int i2 = i + 1;
                    this.rawBuf[i] = (byte) read;
                    this.read++;
                    i = i2;
                } else {
                    throw new IOException("Content Transfer Encoding, base64 line length > 1024");
                }
            } else if (read >= 0) {
                this.read++;
            }
            if (read <= -1 || i >= this.rawBuf.length || read == 10) {
                if (i <= 0) {
                    try {
                        Base64.decode(this.rawBuf, 0, i, this.dataOutputStream);
                    } catch (Exception e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Decode Base64 Content-Transfer-Encoding: ");
                        stringBuilder.append(e);
                        throw new IOException(stringBuilder.toString());
                    }
                } else if (read == -1) {
                    return -1;
                }
            }
        } while (this.read < this.max.longValue());
        if (i <= 0) {
        }
        return this.wp;
    }

    public int read() throws IOException {
        if (this.rp == this.wp) {
            this.rp = 0;
            this.wp = 0;
            int pullFromSrc = pullFromSrc();
            if (pullFromSrc == -1) {
                return pullFromSrc;
            }
        }
        byte[] bArr = this.data;
        int i = this.rp;
        this.rp = i + 1;
        return bArr[i] & 255;
    }
}
