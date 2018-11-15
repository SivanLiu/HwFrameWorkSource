package org.bouncycastle.crypto.tls;

import java.io.IOException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.util.Arrays;

public class TlsNullCipher implements TlsCipher {
    protected TlsContext context;
    protected TlsMac readMac;
    protected TlsMac writeMac;

    public TlsNullCipher(TlsContext tlsContext) {
        this.context = tlsContext;
        this.writeMac = null;
        this.readMac = null;
    }

    public TlsNullCipher(TlsContext tlsContext, Digest digest, Digest digest2) throws IOException {
        int i = 1;
        int i2 = digest == null ? 1 : 0;
        if (digest2 != null) {
            i = 0;
        }
        if (i2 == i) {
            TlsMac tlsMac;
            this.context = tlsContext;
            TlsMac tlsMac2 = null;
            if (digest != null) {
                i = digest.getDigestSize() + digest2.getDigestSize();
                TlsContext tlsContext2 = tlsContext;
                byte[] calculateKeyBlock = TlsUtils.calculateKeyBlock(tlsContext, i);
                TlsMac tlsMac3 = new TlsMac(tlsContext2, digest, calculateKeyBlock, 0, digest.getDigestSize());
                int digestSize = digest.getDigestSize() + 0;
                tlsMac3 = new TlsMac(tlsContext2, digest2, calculateKeyBlock, digestSize, digest2.getDigestSize());
                if (digestSize + digest2.getDigestSize() == i) {
                    tlsMac2 = tlsMac3;
                } else {
                    throw new TlsFatalAlert((short) 80);
                }
            }
            tlsMac = null;
            if (tlsContext.isServer()) {
                this.writeMac = tlsMac2;
                this.readMac = tlsMac;
                return;
            }
            this.writeMac = tlsMac;
            this.readMac = tlsMac2;
            return;
        }
        throw new TlsFatalAlert((short) 80);
    }

    public byte[] decodeCiphertext(long j, short s, byte[] bArr, int i, int i2) throws IOException {
        if (this.readMac == null) {
            return Arrays.copyOfRange(bArr, i, i2 + i);
        }
        int size = this.readMac.getSize();
        if (i2 >= size) {
            int i3 = i2 - size;
            size = i + i3;
            if (Arrays.constantTimeAreEqual(Arrays.copyOfRange(bArr, size, i2 + i), this.readMac.calculateMac(j, s, bArr, i, i3))) {
                return Arrays.copyOfRange(bArr, i, size);
            }
            throw new TlsFatalAlert((short) 20);
        }
        throw new TlsFatalAlert((short) 50);
    }

    public byte[] encodePlaintext(long j, short s, byte[] bArr, int i, int i2) throws IOException {
        if (this.writeMac == null) {
            return Arrays.copyOfRange(bArr, i, i2 + i);
        }
        Object calculateMac = this.writeMac.calculateMac(j, s, bArr, i, i2);
        Object obj = new byte[(calculateMac.length + i2)];
        System.arraycopy(bArr, i, obj, 0, i2);
        System.arraycopy(calculateMac, 0, obj, i2, calculateMac.length);
        return obj;
    }

    public int getPlaintextLimit(int i) {
        return this.writeMac != null ? i - this.writeMac.getSize() : i;
    }
}
