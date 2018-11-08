package com.android.org.conscrypt;

import java.nio.ByteBuffer;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.CertificateException;

final class SSLUtils {
    private static final String KEY_TYPE_EC = "EC";
    private static final String KEY_TYPE_RSA = "RSA";
    private static final int MAX_ENCRYPTION_OVERHEAD_DIFF = 2147483562;
    private static final int MAX_ENCRYPTION_OVERHEAD_LENGTH = 85;
    private static final int MAX_PROTOCOL_LENGTH = 255;
    static final boolean USE_ENGINE_SOCKET_BY_DEFAULT = Boolean.parseBoolean(System.getProperty("com.android.org.conscrypt.useEngineSocketByDefault", "false"));

    static final class EngineStates {
        static final int STATE_CLOSED = 8;
        static final int STATE_CLOSED_INBOUND = 6;
        static final int STATE_CLOSED_OUTBOUND = 7;
        static final int STATE_HANDSHAKE_COMPLETED = 3;
        static final int STATE_HANDSHAKE_STARTED = 2;
        static final int STATE_MODE_SET = 1;
        static final int STATE_NEW = 0;
        static final int STATE_READY = 5;
        static final int STATE_READY_HANDSHAKE_CUT_THROUGH = 4;

        private EngineStates() {
        }
    }

    enum SessionType {
        OPEN_SSL(1),
        OPEN_SSL_WITH_OCSP(2),
        OPEN_SSL_WITH_TLS_SCT(3);
        
        final int value;

        private SessionType(int value) {
            this.value = value;
        }

        static final boolean isSupportedType(int type) {
            if (type == OPEN_SSL.value || type == OPEN_SSL_WITH_OCSP.value || type == OPEN_SSL_WITH_TLS_SCT.value) {
                return true;
            }
            return false;
        }
    }

    static String getServerX509KeyType(long sslCipherNative) throws SSLException {
        String kx_name = NativeCrypto.SSL_CIPHER_get_kx_name(sslCipherNative);
        if (kx_name.equals(KEY_TYPE_RSA) || kx_name.equals("DHE_RSA") || kx_name.equals("ECDHE_RSA")) {
            return KEY_TYPE_RSA;
        }
        if (kx_name.equals("ECDHE_ECDSA")) {
            return KEY_TYPE_EC;
        }
        return null;
    }

    static String getClientKeyType(byte clientCertificateType) {
        switch (clientCertificateType) {
            case (byte) 1:
                return KEY_TYPE_RSA;
            case (byte) 64:
                return KEY_TYPE_EC;
            default:
                return null;
        }
    }

    static Set<String> getSupportedClientKeyTypes(byte[] clientCertificateTypes) {
        Set<String> result = new HashSet(clientCertificateTypes.length);
        for (byte keyTypeCode : clientCertificateTypes) {
            String keyType = getClientKeyType(keyTypeCode);
            if (keyType != null) {
                result.add(keyType);
            }
        }
        return result;
    }

    static byte[][] encodeIssuerX509Principals(X509Certificate[] certificates) throws CertificateEncodingException {
        byte[][] principalBytes = new byte[certificates.length][];
        for (int i = 0; i < certificates.length; i++) {
            principalBytes[i] = certificates[i].getIssuerX500Principal().getEncoded();
        }
        return principalBytes;
    }

    static javax.security.cert.X509Certificate[] toCertificateChain(X509Certificate[] certificates) throws SSLPeerUnverifiedException {
        SSLPeerUnverifiedException exception;
        try {
            javax.security.cert.X509Certificate[] chain = new javax.security.cert.X509Certificate[certificates.length];
            for (int i = 0; i < certificates.length; i++) {
                chain[i] = javax.security.cert.X509Certificate.getInstance(certificates[i].getEncoded());
            }
            return chain;
        } catch (CertificateEncodingException e) {
            exception = new SSLPeerUnverifiedException(e.getMessage());
            exception.initCause(exception);
            throw exception;
        } catch (CertificateException e2) {
            exception = new SSLPeerUnverifiedException(e2.getMessage());
            exception.initCause(exception);
            throw exception;
        }
    }

    static int calculateOutNetBufSize(int pendingBytes) {
        return Math.min(16709, Math.min(MAX_ENCRYPTION_OVERHEAD_DIFF, pendingBytes) + MAX_ENCRYPTION_OVERHEAD_LENGTH);
    }

    static SSLHandshakeException toSSLHandshakeException(Throwable e) {
        if (e instanceof SSLHandshakeException) {
            return (SSLHandshakeException) e;
        }
        return (SSLHandshakeException) new SSLHandshakeException(e.getMessage()).initCause(e);
    }

    static SSLException toSSLException(Throwable e) {
        if (e instanceof SSLException) {
            return (SSLException) e;
        }
        return new SSLException(e);
    }

    static int getEncryptedPacketLength(ByteBuffer[] buffers, int offset) {
        ByteBuffer buffer = buffers[offset];
        if (buffer.remaining() >= 5) {
            return getEncryptedPacketLength(buffer);
        }
        ByteBuffer tmp = ByteBuffer.allocate(5);
        while (true) {
            int offset2 = offset + 1;
            buffer = buffers[offset];
            int pos = buffer.position();
            int limit = buffer.limit();
            if (buffer.remaining() > tmp.remaining()) {
                buffer.limit(tmp.remaining() + pos);
            }
            try {
                tmp.put(buffer);
                if (tmp.hasRemaining()) {
                    offset = offset2;
                } else {
                    tmp.flip();
                    return getEncryptedPacketLength(tmp);
                }
            } finally {
                buffer.limit(limit);
                buffer.position(pos);
            }
        }
    }

    static byte[] toLengthPrefixedList(String... protocols) {
        int i;
        int length = 0;
        for (i = 0; i < protocols.length; i++) {
            int protocolLength = protocols[i].length();
            if (protocolLength == 0 || protocolLength > MAX_PROTOCOL_LENGTH) {
                throw new IllegalArgumentException("Protocol has invalid length (" + protocolLength + "): " + protocols[i]);
            }
            length += protocolLength + 1;
        }
        byte[] data = new byte[length];
        int dataIndex = 0;
        i = 0;
        while (i < protocols.length) {
            String protocol = protocols[i];
            protocolLength = protocol.length();
            int dataIndex2 = dataIndex + 1;
            data[dataIndex] = (byte) protocolLength;
            int ci = 0;
            while (ci < protocolLength) {
                char c = protocol.charAt(ci);
                if (c > '') {
                    throw new IllegalArgumentException("Protocol contains invalid character: " + c + "(protocol=" + protocol + ")");
                }
                dataIndex = dataIndex2 + 1;
                data[dataIndex2] = (byte) c;
                ci++;
                dataIndex2 = dataIndex;
            }
            i++;
            dataIndex = dataIndex2;
        }
        return data;
    }

    private static int getEncryptedPacketLength(ByteBuffer buffer) {
        int pos = buffer.position();
        switch (unsignedByte(buffer.get(pos))) {
            case (short) 20:
            case (short) 21:
            case (short) 22:
            case (short) 23:
                if (unsignedByte(buffer.get(pos + 1)) != 3) {
                    return -1;
                }
                int packetLength = unsignedShort(buffer.getShort(pos + 3)) + 5;
                if (packetLength <= 5) {
                    return -1;
                }
                return packetLength;
            default:
                return -1;
        }
    }

    private static short unsignedByte(byte b) {
        return (short) (b & MAX_PROTOCOL_LENGTH);
    }

    private static int unsignedShort(short s) {
        return 65535 & s;
    }

    private SSLUtils() {
    }
}
