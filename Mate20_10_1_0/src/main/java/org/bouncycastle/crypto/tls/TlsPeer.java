package org.bouncycastle.crypto.tls;

import java.io.IOException;

public interface TlsPeer {
    void cancel() throws IOException;

    TlsCipher getCipher() throws IOException;

    TlsCompression getCompression() throws IOException;

    void notifyAlertRaised(short s, short s2, String str, Throwable th);

    void notifyAlertReceived(short s, short s2);

    void notifyCloseHandle(TlsCloseable tlsCloseable);

    void notifyHandshakeComplete() throws IOException;

    void notifySecureRenegotiation(boolean z) throws IOException;

    boolean requiresExtendedMasterSecret();

    boolean shouldUseGMTUnixTime();
}
