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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;

abstract class NativeSslSession {
    private static final Logger logger = Logger.getLogger(NativeSslSession.class.getName());

    private static final class Impl extends NativeSslSession {
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

        void offerToResume(NativeSsl ssl) throws SSLException {
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
                    byte[] data2 = cert.getEncoded();
                    daos.writeInt(data2.length);
                    daos.write(data2);
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
                NativeSslSession.logger.log(Level.WARNING, "Failed to convert saved SSL Session: ", e);
                return null;
            } catch (CertificateEncodingException e2) {
                NativeSslSession.log(e2);
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

    abstract void offerToResume(NativeSsl nativeSsl) throws SSLException;

    abstract byte[] toBytes();

    abstract SSLSession toSSLSession();

    NativeSslSession() {
    }

    static NativeSslSession newInstance(SSL_SESSION ref, ConscryptSession session) throws SSLPeerUnverifiedException {
        AbstractSessionContext context = (AbstractSessionContext) session.getSessionContext();
        if (!(context instanceof ClientSessionContext)) {
            return new Impl(context, ref, null, -1, null, null, null);
        }
        return new Impl(context, ref, session.getPeerHost(), session.getPeerPort(), session.getPeerCertificates(), getOcspResponse(session), session.getPeerSignedCertificateTimestamp());
    }

    private static byte[] getOcspResponse(ConscryptSession session) {
        List<byte[]> ocspResponseList = session.getStatusResponses();
        if (ocspResponseList.size() >= 1) {
            return (byte[]) ocspResponseList.get(0);
        }
        return null;
    }

    static NativeSslSession newInstance(AbstractSessionContext context, byte[] data, String host, int port) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        int count;
        int i;
        try {
            int type = buf.getInt();
            if (SessionType.isSupportedType(type)) {
                int length;
                int length2 = buf.getInt();
                checkRemaining(buf, length2);
                byte[] sessionData = new byte[length2];
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
                    length2 = length;
                }
                byte[] ocspData = null;
                if (type >= SessionType.OPEN_SSL_WITH_OCSP.value) {
                    length = buf.getInt();
                    checkRemaining(buf, length);
                    int i2 = 1;
                    if (length >= 1) {
                        int ocspLength = buf.getInt();
                        checkRemaining(buf, ocspLength);
                        ocspData = new byte[ocspLength];
                        buf.get(ocspData);
                        while (i2 < length) {
                            ocspLength = buf.getInt();
                            checkRemaining(buf, ocspLength);
                            buf.position(buf.position() + ocspLength);
                            i2++;
                        }
                    }
                }
                byte[] ocspData2 = ocspData;
                ocspData = null;
                if (type == SessionType.OPEN_SSL_WITH_TLS_SCT.value) {
                    length = buf.getInt();
                    checkRemaining(buf, length);
                    if (length > 0) {
                        ocspData = new byte[length];
                        buf.get(ocspData);
                    }
                }
                byte[] tlsSctData = ocspData;
                if (buf.remaining() != 0) {
                    log(new AssertionError("Read entire session, but data still remains; rejecting"));
                    return null;
                }
                return new Impl(context, new SSL_SESSION(NativeCrypto.d2i_SSL_SESSION(sessionData)), host, port, peerCerts, ocspData2, tlsSctData);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected type ID: ");
            stringBuilder.append(type);
            throw new IOException(stringBuilder.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Can not read certificate ");
            stringBuilder2.append(i);
            stringBuilder2.append("/");
            stringBuilder2.append(count);
            throw new IOException(stringBuilder2.toString());
        } catch (IOException e2) {
            log(e2);
            return null;
        } catch (BufferUnderflowException e3) {
            log(e3);
            return null;
        }
    }

    private static void log(Throwable t) {
        logger.log(Level.INFO, "Error inflating SSL session: {0}", t.getMessage() != null ? t.getMessage() : t.getClass().getName());
    }

    private static void checkRemaining(ByteBuffer buf, int length) throws IOException {
        StringBuilder stringBuilder;
        if (length < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Length is negative: ");
            stringBuilder.append(length);
            throw new IOException(stringBuilder.toString());
        } else if (length > buf.remaining()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Length of blob is longer than available: ");
            stringBuilder.append(length);
            stringBuilder.append(" > ");
            stringBuilder.append(buf.remaining());
            throw new IOException(stringBuilder.toString());
        }
    }
}
