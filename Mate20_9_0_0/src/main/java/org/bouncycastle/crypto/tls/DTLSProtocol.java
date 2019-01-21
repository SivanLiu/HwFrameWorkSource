package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.util.Arrays;

public abstract class DTLSProtocol {
    protected final SecureRandom secureRandom;

    protected DTLSProtocol(SecureRandom secureRandom) {
        if (secureRandom != null) {
            this.secureRandom = secureRandom;
            return;
        }
        throw new IllegalArgumentException("'secureRandom' cannot be null");
    }

    protected static void applyMaxFragmentLengthExtension(DTLSRecordLayer dTLSRecordLayer, short s) throws IOException {
        if (s < (short) 0) {
            return;
        }
        if (MaxFragmentLength.isValid(s)) {
            dTLSRecordLayer.setPlaintextLimit(1 << (8 + s));
            return;
        }
        throw new TlsFatalAlert((short) 80);
    }

    /* JADX WARNING: Missing block: B:10:0x001b, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected static short evaluateMaxFragmentLengthExtension(boolean z, Hashtable hashtable, Hashtable hashtable2, short s) throws IOException {
        short maxFragmentLengthExtension = TlsExtensionsUtils.getMaxFragmentLengthExtension(hashtable2);
        if (maxFragmentLengthExtension < (short) 0 || (MaxFragmentLength.isValid(maxFragmentLengthExtension) && (z || maxFragmentLengthExtension == TlsExtensionsUtils.getMaxFragmentLengthExtension(hashtable)))) {
            return maxFragmentLengthExtension;
        }
        throw new TlsFatalAlert(s);
    }

    protected static byte[] generateCertificate(Certificate certificate) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        certificate.encode(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    protected static byte[] generateSupplementalData(Vector vector) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        TlsProtocol.writeSupplementalData(byteArrayOutputStream, vector);
        return byteArrayOutputStream.toByteArray();
    }

    protected static void validateSelectedCipherSuite(int i, short s) throws IOException {
        switch (TlsUtils.getEncryptionAlgorithm(i)) {
            case 1:
            case 2:
                throw new TlsFatalAlert(s);
            default:
                return;
        }
    }

    protected void processFinished(byte[] bArr, byte[] bArr2) throws IOException {
        InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        bArr = TlsUtils.readFully(bArr2.length, byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
        if (!Arrays.constantTimeAreEqual(bArr2, bArr)) {
            throw new TlsFatalAlert((short) 40);
        }
    }
}
