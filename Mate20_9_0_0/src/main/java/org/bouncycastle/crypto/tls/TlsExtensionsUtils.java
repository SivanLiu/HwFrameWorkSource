package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import org.bouncycastle.util.Integers;

public class TlsExtensionsUtils {
    public static final Integer EXT_encrypt_then_mac = Integers.valueOf(22);
    public static final Integer EXT_extended_master_secret = Integers.valueOf(23);
    public static final Integer EXT_heartbeat = Integers.valueOf(15);
    public static final Integer EXT_max_fragment_length = Integers.valueOf(1);
    public static final Integer EXT_padding = Integers.valueOf(21);
    public static final Integer EXT_server_name = Integers.valueOf(0);
    public static final Integer EXT_status_request = Integers.valueOf(5);
    public static final Integer EXT_truncated_hmac = Integers.valueOf(4);

    public static void addEncryptThenMACExtension(Hashtable hashtable) {
        hashtable.put(EXT_encrypt_then_mac, createEncryptThenMACExtension());
    }

    public static void addExtendedMasterSecretExtension(Hashtable hashtable) {
        hashtable.put(EXT_extended_master_secret, createExtendedMasterSecretExtension());
    }

    public static void addHeartbeatExtension(Hashtable hashtable, HeartbeatExtension heartbeatExtension) throws IOException {
        hashtable.put(EXT_heartbeat, createHeartbeatExtension(heartbeatExtension));
    }

    public static void addMaxFragmentLengthExtension(Hashtable hashtable, short s) throws IOException {
        hashtable.put(EXT_max_fragment_length, createMaxFragmentLengthExtension(s));
    }

    public static void addPaddingExtension(Hashtable hashtable, int i) throws IOException {
        hashtable.put(EXT_padding, createPaddingExtension(i));
    }

    public static void addServerNameExtension(Hashtable hashtable, ServerNameList serverNameList) throws IOException {
        hashtable.put(EXT_server_name, createServerNameExtension(serverNameList));
    }

    public static void addStatusRequestExtension(Hashtable hashtable, CertificateStatusRequest certificateStatusRequest) throws IOException {
        hashtable.put(EXT_status_request, createStatusRequestExtension(certificateStatusRequest));
    }

    public static void addTruncatedHMacExtension(Hashtable hashtable) {
        hashtable.put(EXT_truncated_hmac, createTruncatedHMacExtension());
    }

    public static byte[] createEmptyExtensionData() {
        return TlsUtils.EMPTY_BYTES;
    }

    public static byte[] createEncryptThenMACExtension() {
        return createEmptyExtensionData();
    }

    public static byte[] createExtendedMasterSecretExtension() {
        return createEmptyExtensionData();
    }

    public static byte[] createHeartbeatExtension(HeartbeatExtension heartbeatExtension) throws IOException {
        if (heartbeatExtension != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            heartbeatExtension.encode(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
        throw new TlsFatalAlert((short) 80);
    }

    public static byte[] createMaxFragmentLengthExtension(short s) throws IOException {
        TlsUtils.checkUint8(s);
        byte[] bArr = new byte[1];
        TlsUtils.writeUint8(s, bArr, 0);
        return bArr;
    }

    public static byte[] createPaddingExtension(int i) throws IOException {
        TlsUtils.checkUint16(i);
        return new byte[i];
    }

    public static byte[] createServerNameExtension(ServerNameList serverNameList) throws IOException {
        if (serverNameList != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            serverNameList.encode(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
        throw new TlsFatalAlert((short) 80);
    }

    public static byte[] createStatusRequestExtension(CertificateStatusRequest certificateStatusRequest) throws IOException {
        if (certificateStatusRequest != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            certificateStatusRequest.encode(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
        throw new TlsFatalAlert((short) 80);
    }

    public static byte[] createTruncatedHMacExtension() {
        return createEmptyExtensionData();
    }

    public static Hashtable ensureExtensionsInitialised(Hashtable hashtable) {
        return hashtable == null ? new Hashtable() : hashtable;
    }

    public static HeartbeatExtension getHeartbeatExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_heartbeat);
        return extensionData == null ? null : readHeartbeatExtension(extensionData);
    }

    public static short getMaxFragmentLengthExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_max_fragment_length);
        return extensionData == null ? (short) -1 : readMaxFragmentLengthExtension(extensionData);
    }

    public static int getPaddingExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_padding);
        return extensionData == null ? -1 : readPaddingExtension(extensionData);
    }

    public static ServerNameList getServerNameExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_server_name);
        return extensionData == null ? null : readServerNameExtension(extensionData);
    }

    public static CertificateStatusRequest getStatusRequestExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_status_request);
        return extensionData == null ? null : readStatusRequestExtension(extensionData);
    }

    public static boolean hasEncryptThenMACExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_encrypt_then_mac);
        return extensionData == null ? false : readEncryptThenMACExtension(extensionData);
    }

    public static boolean hasExtendedMasterSecretExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_extended_master_secret);
        return extensionData == null ? false : readExtendedMasterSecretExtension(extensionData);
    }

    public static boolean hasTruncatedHMacExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = TlsUtils.getExtensionData(hashtable, EXT_truncated_hmac);
        return extensionData == null ? false : readTruncatedHMacExtension(extensionData);
    }

    private static boolean readEmptyExtensionData(byte[] bArr) throws IOException {
        if (bArr == null) {
            throw new IllegalArgumentException("'extensionData' cannot be null");
        } else if (bArr.length == 0) {
            return true;
        } else {
            throw new TlsFatalAlert((short) 47);
        }
    }

    public static boolean readEncryptThenMACExtension(byte[] bArr) throws IOException {
        return readEmptyExtensionData(bArr);
    }

    public static boolean readExtendedMasterSecretExtension(byte[] bArr) throws IOException {
        return readEmptyExtensionData(bArr);
    }

    public static HeartbeatExtension readHeartbeatExtension(byte[] bArr) throws IOException {
        if (bArr != null) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            HeartbeatExtension parse = HeartbeatExtension.parse(byteArrayInputStream);
            TlsProtocol.assertEmpty(byteArrayInputStream);
            return parse;
        }
        throw new IllegalArgumentException("'extensionData' cannot be null");
    }

    public static short readMaxFragmentLengthExtension(byte[] bArr) throws IOException {
        if (bArr == null) {
            throw new IllegalArgumentException("'extensionData' cannot be null");
        } else if (bArr.length == 1) {
            return TlsUtils.readUint8(bArr, 0);
        } else {
            throw new TlsFatalAlert((short) 50);
        }
    }

    public static int readPaddingExtension(byte[] bArr) throws IOException {
        if (bArr != null) {
            int i = 0;
            while (i < bArr.length) {
                if (bArr[i] == (byte) 0) {
                    i++;
                } else {
                    throw new TlsFatalAlert((short) 47);
                }
            }
            return bArr.length;
        }
        throw new IllegalArgumentException("'extensionData' cannot be null");
    }

    public static ServerNameList readServerNameExtension(byte[] bArr) throws IOException {
        if (bArr != null) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            ServerNameList parse = ServerNameList.parse(byteArrayInputStream);
            TlsProtocol.assertEmpty(byteArrayInputStream);
            return parse;
        }
        throw new IllegalArgumentException("'extensionData' cannot be null");
    }

    public static CertificateStatusRequest readStatusRequestExtension(byte[] bArr) throws IOException {
        if (bArr != null) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            CertificateStatusRequest parse = CertificateStatusRequest.parse(byteArrayInputStream);
            TlsProtocol.assertEmpty(byteArrayInputStream);
            return parse;
        }
        throw new IllegalArgumentException("'extensionData' cannot be null");
    }

    public static boolean readTruncatedHMacExtension(byte[] bArr) throws IOException {
        return readEmptyExtensionData(bArr);
    }
}
