package com.android.org.conscrypt;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

final class SSLUtils {
    private static final String KEY_TYPE_EC = "EC";
    private static final String KEY_TYPE_RSA = "RSA";
    private static final int MAX_ENCRYPTION_OVERHEAD_DIFF = 2147483562;
    private static final int MAX_ENCRYPTION_OVERHEAD_LENGTH = 85;
    private static final int MAX_PROTOCOL_LENGTH = 255;
    static final boolean USE_ENGINE_SOCKET_BY_DEFAULT = Boolean.parseBoolean(System.getProperty("com.android.org.conscrypt.useEngineSocketByDefault", "false"));
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

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

        static boolean isSupportedType(int type) {
            return type == OPEN_SSL.value || type == OPEN_SSL_WITH_OCSP.value || type == OPEN_SSL_WITH_TLS_SCT.value;
        }
    }

    static SSLSession unwrapSession(SSLSession session) {
        while (session instanceof SessionDecorator) {
            session = ((SessionDecorator) session).getDelegate();
        }
        return session;
    }

    static X509Certificate[] decodeX509CertificateChain(byte[][] certChain) throws CertificateException {
        CertificateFactory certificateFactory = getCertificateFactory();
        int numCerts = certChain.length;
        X509Certificate[] decodedCerts = new X509Certificate[numCerts];
        for (int i = 0; i < numCerts; i++) {
            decodedCerts[i] = decodeX509Certificate(certificateFactory, certChain[i]);
        }
        return decodedCerts;
    }

    private static CertificateFactory getCertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            return null;
        }
    }

    private static X509Certificate decodeX509Certificate(CertificateFactory certificateFactory, byte[] bytes) throws CertificateException {
        if (certificateFactory != null) {
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(bytes));
        }
        return OpenSSLX509Certificate.fromX509Der(bytes);
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
        if (clientCertificateType == (byte) 1) {
            return KEY_TYPE_RSA;
        }
        if (clientCertificateType != (byte) 64) {
            return null;
        }
        return KEY_TYPE_EC;
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
        } catch (javax.security.cert.CertificateException e2) {
            exception = new SSLPeerUnverifiedException(e2.getMessage());
            exception.initCause(exception);
            throw exception;
        }
    }

    static int calculateOutNetBufSize(int pendingBytes) {
        return Math.min(16709, MAX_ENCRYPTION_OVERHEAD_LENGTH + Math.min(MAX_ENCRYPTION_OVERHEAD_DIFF, pendingBytes));
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

    static String toProtocolString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, US_ASCII);
    }

    static byte[] toProtocolBytes(String protocol) {
        if (protocol == null) {
            return null;
        }
        return protocol.getBytes(US_ASCII);
    }

    static String[] decodeProtocols(byte[] protocols) {
        if (protocols.length == 0) {
            return EmptyArray.STRING;
        }
        int protocolLength;
        int d = 0;
        int numProtocols = 0;
        int i = 0;
        while (i < protocols.length) {
            protocolLength = protocols[i];
            if (protocolLength < 0 || protocolLength > protocols.length - i) {
                String arrays;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Protocol has invalid length (");
                stringBuilder.append(protocolLength);
                stringBuilder.append(" at position ");
                stringBuilder.append(i);
                stringBuilder.append("): ");
                if (protocols.length < 50) {
                    arrays = Arrays.toString(protocols);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(protocols.length);
                    stringBuilder2.append(" byte array");
                    arrays = stringBuilder2.toString();
                }
                stringBuilder.append(arrays);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            numProtocols++;
            i += 1 + protocolLength;
        }
        String[] decoded = new String[numProtocols];
        protocolLength = 0;
        while (protocolLength < protocols.length) {
            String str;
            int protocolLength2 = protocols[protocolLength];
            int d2 = d + 1;
            if (protocolLength2 > 0) {
                str = new String(protocols, protocolLength + 1, protocolLength2, US_ASCII);
            } else {
                str = "";
            }
            decoded[d] = str;
            protocolLength += 1 + protocolLength2;
            d = d2;
        }
        return decoded;
    }

    static byte[] encodeProtocols(String[] protocols) {
        if (protocols == null) {
            throw new IllegalArgumentException("protocols array must be non-null");
        } else if (protocols.length == 0) {
            return EmptyArray.BYTE;
        } else {
            int protocolLength;
            int length = 0;
            int i = 0;
            while (i < protocols.length) {
                if (protocols[i] != null) {
                    protocolLength = protocols[i].length();
                    if (protocolLength == 0 || protocolLength > MAX_PROTOCOL_LENGTH) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("protocol[");
                        stringBuilder.append(i);
                        stringBuilder.append("] has invalid length: ");
                        stringBuilder.append(protocolLength);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    length += 1 + protocolLength;
                    i++;
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("protocol[");
                    stringBuilder2.append(i);
                    stringBuilder2.append("] is null");
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            }
            byte[] data = new byte[length];
            protocolLength = 0;
            int i2 = 0;
            while (i2 < protocols.length) {
                String protocol = protocols[i2];
                int protocolLength2 = protocol.length();
                int dataIndex = protocolLength + 1;
                data[protocolLength] = (byte) protocolLength2;
                protocolLength = 0;
                while (protocolLength < protocolLength2) {
                    char c = protocol.charAt(protocolLength);
                    if (c <= 127) {
                        int dataIndex2 = dataIndex + 1;
                        data[dataIndex] = (byte) c;
                        protocolLength++;
                        dataIndex = dataIndex2;
                    } else {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Protocol contains invalid character: ");
                        stringBuilder3.append(c);
                        stringBuilder3.append("(protocol=");
                        stringBuilder3.append(protocol);
                        stringBuilder3.append(")");
                        throw new IllegalArgumentException(stringBuilder3.toString());
                    }
                }
                i2++;
                protocolLength = dataIndex;
            }
            return data;
        }
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
            offset = buffer.position();
            int limit = buffer.limit();
            if (buffer.remaining() > tmp.remaining()) {
                buffer.limit(tmp.remaining() + offset);
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
                buffer.position(offset);
            }
        }
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
