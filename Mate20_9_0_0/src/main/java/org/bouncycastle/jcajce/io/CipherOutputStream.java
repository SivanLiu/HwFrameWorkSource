package org.bouncycastle.jcajce.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import org.bouncycastle.crypto.io.InvalidCipherTextIOException;

public class CipherOutputStream extends FilterOutputStream {
    private final Cipher cipher;
    private final byte[] oneByte = new byte[1];

    public CipherOutputStream(OutputStream outputStream, Cipher cipher) {
        super(outputStream);
        this.cipher = cipher;
    }

    /* JADX WARNING: Missing block: B:12:0x003a, code skipped:
            if (r1 != null) goto L_0x003d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close() throws IOException {
        IOException iOException;
        IOException e;
        try {
            byte[] doFinal = this.cipher.doFinal();
            if (doFinal != null) {
                this.out.write(doFinal);
            }
            iOException = null;
        } catch (GeneralSecurityException e2) {
            iOException = new InvalidCipherTextIOException("Error during cipher finalisation", e2);
        } catch (Exception e3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error closing stream: ");
            stringBuilder.append(e3);
            iOException = new IOException(stringBuilder.toString());
        }
        try {
            flush();
            this.out.close();
        } catch (IOException e4) {
            e = e4;
        }
        e = iOException;
        if (e != null) {
            throw e;
        }
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void write(int i) throws IOException {
        this.oneByte[0] = (byte) i;
        write(this.oneByte, 0, 1);
    }

    public void write(byte[] bArr, int i, int i2) throws IOException {
        bArr = this.cipher.update(bArr, i, i2);
        if (bArr != null) {
            this.out.write(bArr);
        }
    }
}
