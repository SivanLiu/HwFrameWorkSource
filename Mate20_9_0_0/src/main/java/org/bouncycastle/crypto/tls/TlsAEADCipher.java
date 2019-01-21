package org.bouncycastle.crypto.tls;

import java.io.IOException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Arrays;

public class TlsAEADCipher implements TlsCipher {
    static final int NONCE_DRAFT_CHACHA20_POLY1305 = 2;
    public static final int NONCE_RFC5288 = 1;
    protected TlsContext context;
    protected AEADBlockCipher decryptCipher;
    protected byte[] decryptImplicitNonce;
    protected AEADBlockCipher encryptCipher;
    protected byte[] encryptImplicitNonce;
    protected int macSize;
    protected int nonceMode;
    protected int record_iv_length;

    public TlsAEADCipher(TlsContext tlsContext, AEADBlockCipher aEADBlockCipher, AEADBlockCipher aEADBlockCipher2, int i, int i2) throws IOException {
        this(tlsContext, aEADBlockCipher, aEADBlockCipher2, i, i2, 1);
    }

    TlsAEADCipher(TlsContext tlsContext, AEADBlockCipher aEADBlockCipher, AEADBlockCipher aEADBlockCipher2, int i, int i2, int i3) throws IOException {
        TlsContext tlsContext2 = tlsContext;
        AEADBlockCipher aEADBlockCipher3 = aEADBlockCipher;
        AEADBlockCipher aEADBlockCipher4 = aEADBlockCipher2;
        int i4 = i;
        int i5 = i2;
        int i6 = i3;
        if (TlsUtils.isTLSv12(tlsContext)) {
            this.nonceMode = i6;
            switch (i6) {
                case 1:
                    i6 = 4;
                    this.record_iv_length = 8;
                    break;
                case 2:
                    i6 = 12;
                    this.record_iv_length = 0;
                    break;
                default:
                    throw new TlsFatalAlert((short) 80);
            }
            this.context = tlsContext2;
            this.macSize = i5;
            int i7 = (2 * i4) + (2 * i6);
            byte[] calculateKeyBlock = TlsUtils.calculateKeyBlock(tlsContext2, i7);
            KeyParameter keyParameter = new KeyParameter(calculateKeyBlock, 0, i4);
            int i8 = 0 + i4;
            KeyParameter keyParameter2 = new KeyParameter(calculateKeyBlock, i8, i4);
            i8 += i4;
            i4 = i8 + i6;
            byte[] copyOfRange = Arrays.copyOfRange(calculateKeyBlock, i8, i4);
            int i9 = i4 + i6;
            byte[] copyOfRange2 = Arrays.copyOfRange(calculateKeyBlock, i4, i9);
            if (i9 == i7) {
                if (tlsContext.isServer()) {
                    this.encryptCipher = aEADBlockCipher4;
                    this.decryptCipher = aEADBlockCipher3;
                    this.encryptImplicitNonce = copyOfRange2;
                    this.decryptImplicitNonce = copyOfRange;
                    KeyParameter keyParameter3 = keyParameter2;
                    keyParameter2 = keyParameter;
                    keyParameter = keyParameter3;
                } else {
                    this.encryptCipher = aEADBlockCipher3;
                    this.decryptCipher = aEADBlockCipher4;
                    this.encryptImplicitNonce = copyOfRange;
                    this.decryptImplicitNonce = copyOfRange2;
                }
                byte[] bArr = new byte[(i6 + this.record_iv_length)];
                int i10 = 8 * i5;
                this.encryptCipher.init(true, new AEADParameters(keyParameter, i10, bArr));
                this.decryptCipher.init(false, new AEADParameters(keyParameter2, i10, bArr));
                return;
            }
            throw new TlsFatalAlert((short) 80);
        }
        throw new TlsFatalAlert((short) 80);
    }

    public byte[] decodeCiphertext(long j, short s, byte[] bArr, int i, int i2) throws IOException {
        long j2 = j;
        int i3 = i;
        int i4 = i2;
        if (getPlaintextLimit(i4) >= 0) {
            byte[] bArr2;
            byte[] bArr3 = new byte[(this.decryptImplicitNonce.length + this.record_iv_length)];
            switch (this.nonceMode) {
                case 1:
                    System.arraycopy(this.decryptImplicitNonce, 0, bArr3, 0, this.decryptImplicitNonce.length);
                    bArr2 = bArr;
                    System.arraycopy(bArr2, i3, bArr3, bArr3.length - this.record_iv_length, this.record_iv_length);
                    break;
                case 2:
                    TlsUtils.writeUint64(j2, bArr3, bArr3.length - 8);
                    for (int i5 = 0; i5 < this.decryptImplicitNonce.length; i5++) {
                        bArr3[i5] = (byte) (bArr3[i5] ^ this.decryptImplicitNonce[i5]);
                    }
                    bArr2 = bArr;
                    break;
                default:
                    throw new TlsFatalAlert((short) 80);
            }
            int i6 = i3 + this.record_iv_length;
            int i7 = i4 - this.record_iv_length;
            i3 = this.decryptCipher.getOutputSize(i7);
            byte[] bArr4 = new byte[i3];
            try {
                this.decryptCipher.init(false, new AEADParameters(null, 8 * this.macSize, bArr3, getAdditionalData(j2, s, i3)));
                int processBytes = 0 + this.decryptCipher.processBytes(bArr2, i6, i7, bArr4, 0);
                if (processBytes + this.decryptCipher.doFinal(bArr4, processBytes) == bArr4.length) {
                    return bArr4;
                }
                throw new TlsFatalAlert((short) 80);
            } catch (Exception e) {
                throw new TlsFatalAlert((short) 20, e);
            }
        }
        throw new TlsFatalAlert((short) 50);
    }

    public byte[] encodePlaintext(long j, short s, byte[] bArr, int i, int i2) throws IOException {
        long j2 = j;
        int i3 = i2;
        byte[] bArr2 = new byte[(this.encryptImplicitNonce.length + this.record_iv_length)];
        switch (this.nonceMode) {
            case 1:
                System.arraycopy(this.encryptImplicitNonce, 0, bArr2, 0, this.encryptImplicitNonce.length);
                TlsUtils.writeUint64(j2, bArr2, this.encryptImplicitNonce.length);
                break;
            case 2:
                TlsUtils.writeUint64(j2, bArr2, bArr2.length - 8);
                for (int i4 = 0; i4 < this.encryptImplicitNonce.length; i4++) {
                    bArr2[i4] = (byte) (bArr2[i4] ^ this.encryptImplicitNonce[i4]);
                }
                break;
            default:
                throw new TlsFatalAlert((short) 80);
        }
        byte[] bArr3 = new byte[(this.record_iv_length + this.encryptCipher.getOutputSize(i3))];
        if (this.record_iv_length != 0) {
            System.arraycopy(bArr2, bArr2.length - this.record_iv_length, bArr3, 0, this.record_iv_length);
        }
        int i5 = this.record_iv_length;
        try {
            this.encryptCipher.init(true, new AEADParameters(null, 8 * this.macSize, bArr2, getAdditionalData(j2, s, i3)));
            i5 += this.encryptCipher.processBytes(bArr, i, i3, bArr3, i5);
            if (i5 + this.encryptCipher.doFinal(bArr3, i5) == bArr3.length) {
                return bArr3;
            }
            throw new TlsFatalAlert((short) 80);
        } catch (Exception e) {
            throw new TlsFatalAlert((short) 80, e);
        }
    }

    protected byte[] getAdditionalData(long j, short s, int i) throws IOException {
        byte[] bArr = new byte[13];
        TlsUtils.writeUint64(j, bArr, 0);
        TlsUtils.writeUint8(s, bArr, 8);
        TlsUtils.writeVersion(this.context.getServerVersion(), bArr, 9);
        TlsUtils.writeUint16(i, bArr, 11);
        return bArr;
    }

    public int getPlaintextLimit(int i) {
        return (i - this.macSize) - this.record_iv_length;
    }
}
