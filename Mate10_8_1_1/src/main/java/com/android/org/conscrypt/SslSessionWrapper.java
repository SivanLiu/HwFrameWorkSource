package com.android.org.conscrypt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;

abstract class SslSessionWrapper {

    private static final class Impl extends SslSessionWrapper {
        private final String cipherSuite;
        private final AbstractSessionContext context;
        private final String host;
        private final X509Certificate[] peerCertificates;
        private final byte[] peerOcspStapledResponse;
        private final byte[] peerSignedCertificateTimestamp;
        private final int port;
        private final String protocol;
        private final SSL_SESSION ref;

        private Impl(AbstractSessionContext context, SSL_SESSION ref, String host, int port, X509Certificate[] peerCertificates, byte[] peerOcspStapledResponse, byte[] peerSignedCertificateTimestamp) {
            this.context = context;
            this.host = host;
            this.port = port;
            this.peerCertificates = peerCertificates;
            this.peerOcspStapledResponse = peerOcspStapledResponse;
            this.peerSignedCertificateTimestamp = peerSignedCertificateTimestamp;
            this.protocol = NativeCrypto.SSL_SESSION_get_version(ref.context);
            this.cipherSuite = NativeCrypto.cipherSuiteToJava(NativeCrypto.SSL_SESSION_cipher(ref.context));
            this.ref = ref;
        }

        byte[] getId() {
            return NativeCrypto.SSL_SESSION_session_id(this.ref.context);
        }

        private long getCreationTime() {
            return NativeCrypto.SSL_SESSION_get_time(this.ref.context);
        }

        boolean isValid() {
            return System.currentTimeMillis() - (Math.max(0, Math.min((long) this.context.getSessionTimeout(), NativeCrypto.SSL_SESSION_get_timeout(this.ref.context))) * 1000) < getCreationTime();
        }

        void offerToResume(SslWrapper ssl) throws SSLException {
            ssl.offerToResumeSession(this.ref.context);
        }

        String getCipherSuite() {
            return this.cipherSuite;
        }

        String getProtocol() {
            return this.protocol;
        }

        String getPeerHost() {
            return this.host;
        }

        int getPeerPort() {
            return this.port;
        }

        byte[] getPeerOcspStapledResponse() {
            return this.peerOcspStapledResponse;
        }

        byte[] getPeerSignedCertificateTimestamp() {
            return this.peerSignedCertificateTimestamp;
        }

        byte[] toBytes() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream daos = new DataOutputStream(baos);
                daos.writeInt(SessionType.OPEN_SSL_WITH_TLS_SCT.value);
                byte[] data = NativeCrypto.i2d_SSL_SESSION(this.ref.context);
                daos.writeInt(data.length);
                daos.write(data);
                daos.writeInt(this.peerCertificates.length);
                for (Certificate cert : this.peerCertificates) {
                    data = cert.getEncoded();
                    daos.writeInt(data.length);
                    daos.write(data);
                }
                if (this.peerOcspStapledResponse != null) {
                    daos.writeInt(1);
                    daos.writeInt(this.peerOcspStapledResponse.length);
                    daos.write(this.peerOcspStapledResponse);
                } else {
                    daos.writeInt(0);
                }
                if (this.peerSignedCertificateTimestamp != null) {
                    daos.writeInt(this.peerSignedCertificateTimestamp.length);
                    daos.write(this.peerSignedCertificateTimestamp);
                } else {
                    daos.writeInt(0);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                System.err.println("Failed to convert saved SSL Session: " + e.getMessage());
                return null;
            } catch (CertificateEncodingException e2) {
                SslSessionWrapper.log(e2);
                return null;
            }
        }

        SSLSession toSSLSession() {
            return new SSLSession() {
                public byte[] getId() {
                    return Impl.this.getId();
                }

                public String getCipherSuite() {
                    return Impl.this.getCipherSuite();
                }

                public String getProtocol() {
                    return Impl.this.getProtocol();
                }

                public String getPeerHost() {
                    return Impl.this.getPeerHost();
                }

                public int getPeerPort() {
                    return Impl.this.getPeerPort();
                }

                public long getCreationTime() {
                    return Impl.this.getCreationTime();
                }

                public boolean isValid() {
                    return Impl.this.isValid();
                }

                public SSLSessionContext getSessionContext() {
                    throw new UnsupportedOperationException();
                }

                public long getLastAccessedTime() {
                    throw new UnsupportedOperationException();
                }

                public void invalidate() {
                    throw new UnsupportedOperationException();
                }

                public void putValue(String s, Object o) {
                    throw new UnsupportedOperationException();
                }

                public Object getValue(String s) {
                    throw new UnsupportedOperationException();
                }

                public void removeValue(String s) {
                    throw new UnsupportedOperationException();
                }

                public String[] getValueNames() {
                    throw new UnsupportedOperationException();
                }

                public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
                    throw new UnsupportedOperationException();
                }

                public Certificate[] getLocalCertificates() {
                    throw new UnsupportedOperationException();
                }

                public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
                    throw new UnsupportedOperationException();
                }

                public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
                    throw new UnsupportedOperationException();
                }

                public Principal getLocalPrincipal() {
                    throw new UnsupportedOperationException();
                }

                public int getPacketBufferSize() {
                    throw new UnsupportedOperationException();
                }

                public int getApplicationBufferSize() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    abstract String getCipherSuite();

    abstract byte[] getId();

    abstract String getPeerHost();

    abstract byte[] getPeerOcspStapledResponse();

    abstract int getPeerPort();

    abstract byte[] getPeerSignedCertificateTimestamp();

    abstract String getProtocol();

    abstract boolean isValid();

    abstract void offerToResume(SslWrapper sslWrapper) throws SSLException;

    abstract byte[] toBytes();

    abstract SSLSession toSSLSession();

    SslSessionWrapper() {
    }

    static SslSessionWrapper newInstance(SSL_SESSION ref, ActiveSession activeSession) throws SSLPeerUnverifiedException {
        AbstractSessionContext context = (AbstractSessionContext) activeSession.getSessionContext();
        if (!(context instanceof ClientSessionContext)) {
            return new Impl(context, ref, null, -1, null, null, null);
        }
        return new Impl(context, ref, activeSession.getPeerHost(), activeSession.getPeerPort(), activeSession.getPeerCertificates(), getOcspResponse(activeSession), activeSession.getPeerSignedCertificateTimestamp());
    }

    private static byte[] getOcspResponse(ActiveSession activeSession) {
        List<byte[]> ocspResponseList = activeSession.getStatusResponses();
        if (ocspResponseList.size() >= 1) {
            return (byte[]) ocspResponseList.get(0);
        }
        return null;
    }

    static SslSessionWrapper newInstance(AbstractSessionContext context, byte[] data, String host, int port) {
        int i;
        ByteBuffer buf = ByteBuffer.wrap(data);
        int count;
        try {
            int type = buf.getInt();
            if (SessionType.isSupportedType(type)) {
                int length = buf.getInt();
                checkRemaining(buf, length);
                byte[] sessionData = new byte[length];
                buf.get(sessionData);
                count = buf.getInt();
                checkRemaining(buf, count);
                X509Certificate[] peerCerts = new X509Certificate[count];
                i = 0;
                while (i < count) {
                    length = buf.getInt();
                    checkRemaining(buf, length);
                    byte[] certData = new byte[length];
                    buf.get(certData);
                    peerCerts[i] = OpenSSLX509Certificate.fromX509Der(certData);
                    i++;
                }
                byte[] bArr = null;
                if (type >= SessionType.OPEN_SSL_WITH_OCSP.value) {
                    int countOcspResponses = buf.getInt();
                    checkRemaining(buf, countOcspResponses);
                    if (countOcspResponses >= 1) {
                        int ocspLength = buf.getInt();
                        checkRemaining(buf, ocspLength);
                        bArr = new byte[ocspLength];
                        buf.get(bArr);
                        for (i = 1; i < countOcspResponses; i++) {
                            ocspLength = buf.getInt();
                            checkRemaining(buf, ocspLength);
                            buf.position(buf.position() + ocspLength);
                        }
                    }
                }
                byte[] bArr2 = null;
                if (type == SessionType.OPEN_SSL_WITH_TLS_SCT.value) {
                    int tlsSctDataLength = buf.getInt();
                    checkRemaining(buf, tlsSctDataLength);
                    if (tlsSctDataLength > 0) {
                        bArr2 = new byte[tlsSctDataLength];
                        buf.get(bArr2);
                    }
                }
                if (buf.remaining() != 0) {
                    log(new AssertionError("Read entire session, but data still remains; rejecting"));
                    return null;
                }
                return new Impl(context, new SSL_SESSION(NativeCrypto.d2i_SSL_SESSION(sessionData)), host, port, peerCerts, bArr, bArr2);
            }
            throw new IOException("Unexpected type ID: " + type);
        } catch (Exception e) {
            throw new IOException("Can not read certificate " + i + "/" + count);
        } catch (IOException e2) {
            log(e2);
            return null;
        } catch (BufferUnderflowException e3) {
            log(e3);
            return null;
        }
    }

    private static void log(Throwable t) {
        System.out.println("Error inflating SSL session: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getName()));
    }

    private static void checkRemaining(ByteBuffer buf, int length) throws IOException {
        if (length < 0) {
            throw new IOException("Length is negative: " + length);
        } else if (length > buf.remaining()) {
            throw new IOException("Length of blob is longer than available: " + length + " > " + buf.remaining());
        }
    }
}
