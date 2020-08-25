package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA224Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.math.ec.Tnaf;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;
import org.bouncycastle.util.Shorts;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.io.Streams;

public class TlsUtils {
    public static final byte[] EMPTY_BYTES = new byte[0];
    public static final int[] EMPTY_INTS = new int[0];
    public static final long[] EMPTY_LONGS = new long[0];
    public static final short[] EMPTY_SHORTS = new short[0];
    public static final Integer EXT_signature_algorithms = Integers.valueOf(13);
    static final byte[][] SSL3_CONST = genSSL3Const();
    static final byte[] SSL_CLIENT = {67, 76, 78, 84};
    static final byte[] SSL_SERVER = {83, 82, 86, 82};

    public static byte[] PRF(TlsContext tlsContext, byte[] bArr, String str, byte[] bArr2, int i) {
        if (!tlsContext.getServerVersion().isSSL()) {
            byte[] byteArray = Strings.toByteArray(str);
            byte[] concat = concat(byteArray, bArr2);
            int prfAlgorithm = tlsContext.getSecurityParameters().getPrfAlgorithm();
            if (prfAlgorithm == 0) {
                return PRF_legacy(bArr, byteArray, concat, i);
            }
            byte[] bArr3 = new byte[i];
            hmac_hash(createPRFHash(prfAlgorithm), bArr, concat, bArr3);
            return bArr3;
        }
        throw new IllegalStateException("No PRF available for SSLv3 session");
    }

    public static byte[] PRF_legacy(byte[] bArr, String str, byte[] bArr2, int i) {
        byte[] byteArray = Strings.toByteArray(str);
        return PRF_legacy(bArr, byteArray, concat(byteArray, bArr2), i);
    }

    static byte[] PRF_legacy(byte[] bArr, byte[] bArr2, byte[] bArr3, int i) {
        int length = (bArr.length + 1) / 2;
        byte[] bArr4 = new byte[length];
        byte[] bArr5 = new byte[length];
        System.arraycopy(bArr, 0, bArr4, 0, length);
        System.arraycopy(bArr, bArr.length - length, bArr5, 0, length);
        byte[] bArr6 = new byte[i];
        byte[] bArr7 = new byte[i];
        hmac_hash(createHash((short) 1), bArr4, bArr3, bArr6);
        hmac_hash(createHash((short) 2), bArr5, bArr3, bArr7);
        for (int i2 = 0; i2 < i; i2++) {
            bArr6[i2] = (byte) (bArr6[i2] ^ bArr7[i2]);
        }
        return bArr6;
    }

    public static void addSignatureAlgorithmsExtension(Hashtable hashtable, Vector vector) throws IOException {
        hashtable.put(EXT_signature_algorithms, createSignatureAlgorithmsExtension(vector));
    }

    static byte[] calculateKeyBlock(TlsContext tlsContext, int i) {
        SecurityParameters securityParameters = tlsContext.getSecurityParameters();
        byte[] masterSecret = securityParameters.getMasterSecret();
        byte[] concat = concat(securityParameters.getServerRandom(), securityParameters.getClientRandom());
        return isSSL(tlsContext) ? calculateKeyBlock_SSL(masterSecret, concat, i) : PRF(tlsContext, masterSecret, "key expansion", concat, i);
    }

    static byte[] calculateKeyBlock_SSL(byte[] bArr, byte[] bArr2, int i) {
        Digest createHash = createHash((short) 1);
        Digest createHash2 = createHash((short) 2);
        int digestSize = createHash.getDigestSize();
        byte[] bArr3 = new byte[createHash2.getDigestSize()];
        byte[] bArr4 = new byte[(i + digestSize)];
        int i2 = 0;
        int i3 = 0;
        while (i2 < i) {
            byte[] bArr5 = SSL3_CONST[i3];
            createHash2.update(bArr5, 0, bArr5.length);
            createHash2.update(bArr, 0, bArr.length);
            createHash2.update(bArr2, 0, bArr2.length);
            createHash2.doFinal(bArr3, 0);
            createHash.update(bArr, 0, bArr.length);
            createHash.update(bArr3, 0, bArr3.length);
            createHash.doFinal(bArr4, i2);
            i2 += digestSize;
            i3++;
        }
        return Arrays.copyOfRange(bArr4, 0, i);
    }

    static byte[] calculateMasterSecret(TlsContext tlsContext, byte[] bArr) {
        SecurityParameters securityParameters = tlsContext.getSecurityParameters();
        byte[] sessionHash = securityParameters.isExtendedMasterSecret() ? securityParameters.getSessionHash() : concat(securityParameters.getClientRandom(), securityParameters.getServerRandom());
        if (isSSL(tlsContext)) {
            return calculateMasterSecret_SSL(bArr, sessionHash);
        }
        return PRF(tlsContext, bArr, securityParameters.isExtendedMasterSecret() ? ExporterLabel.extended_master_secret : "master secret", sessionHash, 48);
    }

    static byte[] calculateMasterSecret_SSL(byte[] bArr, byte[] bArr2) {
        Digest createHash = createHash((short) 1);
        Digest createHash2 = createHash((short) 2);
        int digestSize = createHash.getDigestSize();
        byte[] bArr3 = new byte[createHash2.getDigestSize()];
        byte[] bArr4 = new byte[(digestSize * 3)];
        int i = 0;
        for (int i2 = 0; i2 < 3; i2++) {
            byte[] bArr5 = SSL3_CONST[i2];
            createHash2.update(bArr5, 0, bArr5.length);
            createHash2.update(bArr, 0, bArr.length);
            createHash2.update(bArr2, 0, bArr2.length);
            createHash2.doFinal(bArr3, 0);
            createHash.update(bArr, 0, bArr.length);
            createHash.update(bArr3, 0, bArr3.length);
            createHash.doFinal(bArr4, i);
            i += digestSize;
        }
        return bArr4;
    }

    static byte[] calculateVerifyData(TlsContext tlsContext, String str, byte[] bArr) {
        if (isSSL(tlsContext)) {
            return bArr;
        }
        SecurityParameters securityParameters = tlsContext.getSecurityParameters();
        return PRF(tlsContext, securityParameters.getMasterSecret(), str, bArr, securityParameters.getVerifyDataLength());
    }

    public static void checkUint16(int i) throws IOException {
        if (!isValidUint16(i)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint16(long j) throws IOException {
        if (!isValidUint16(j)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint24(int i) throws IOException {
        if (!isValidUint24(i)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint24(long j) throws IOException {
        if (!isValidUint24(j)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint32(long j) throws IOException {
        if (!isValidUint32(j)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint48(long j) throws IOException {
        if (!isValidUint48(j)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint64(long j) throws IOException {
        if (!isValidUint64(j)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint8(int i) throws IOException {
        if (!isValidUint8(i)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint8(long j) throws IOException {
        if (!isValidUint8(j)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static void checkUint8(short s) throws IOException {
        if (!isValidUint8(s)) {
            throw new TlsFatalAlert(80);
        }
    }

    public static Digest cloneHash(short s, Digest digest) {
        switch (s) {
            case 1:
                return new MD5Digest((MD5Digest) digest);
            case 2:
                return new SHA1Digest((SHA1Digest) digest);
            case 3:
                return new SHA224Digest((SHA224Digest) digest);
            case 4:
                return new SHA256Digest((SHA256Digest) digest);
            case 5:
                return new SHA384Digest((SHA384Digest) digest);
            case 6:
                return new SHA512Digest((SHA512Digest) digest);
            default:
                throw new IllegalArgumentException("unknown HashAlgorithm");
        }
    }

    public static Digest clonePRFHash(int i, Digest digest) {
        return i != 0 ? cloneHash(getHashAlgorithmForPRFAlgorithm(i), digest) : new CombinedHash((CombinedHash) digest);
    }

    static byte[] concat(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = new byte[(bArr.length + bArr2.length)];
        System.arraycopy(bArr, 0, bArr3, 0, bArr.length);
        System.arraycopy(bArr2, 0, bArr3, bArr.length, bArr2.length);
        return bArr3;
    }

    public static Digest createHash(SignatureAndHashAlgorithm signatureAndHashAlgorithm) {
        return signatureAndHashAlgorithm == null ? new CombinedHash() : createHash(signatureAndHashAlgorithm.getHash());
    }

    public static Digest createHash(short s) {
        switch (s) {
            case 1:
                return new MD5Digest();
            case 2:
                return new SHA1Digest();
            case 3:
                return new SHA224Digest();
            case 4:
                return new SHA256Digest();
            case 5:
                return new SHA384Digest();
            case 6:
                return new SHA512Digest();
            default:
                throw new IllegalArgumentException("unknown HashAlgorithm");
        }
    }

    public static Digest createPRFHash(int i) {
        return i != 0 ? createHash(getHashAlgorithmForPRFAlgorithm(i)) : new CombinedHash();
    }

    public static byte[] createSignatureAlgorithmsExtension(Vector vector) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        encodeSupportedSignatureAlgorithms(vector, false, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static TlsSigner createTlsSigner(short s) {
        if (s == 1) {
            return new TlsRSASigner();
        }
        if (s == 2) {
            return new TlsDSSSigner();
        }
        if (s == 64) {
            return new TlsECDSASigner();
        }
        throw new IllegalArgumentException("'clientCertificateType' is not a type with signing capability");
    }

    public static byte[] encodeOpaque8(byte[] bArr) throws IOException {
        checkUint8(bArr.length);
        return Arrays.prepend(bArr, (byte) bArr.length);
    }

    public static void encodeSupportedSignatureAlgorithms(Vector vector, boolean z, OutputStream outputStream) throws IOException {
        if (vector == null || vector.size() < 1 || vector.size() >= 32768) {
            throw new IllegalArgumentException("'supportedSignatureAlgorithms' must have length from 1 to (2^15 - 1)");
        }
        int size = vector.size() * 2;
        checkUint16(size);
        writeUint16(size, outputStream);
        int i = 0;
        while (i < vector.size()) {
            SignatureAndHashAlgorithm signatureAndHashAlgorithm = (SignatureAndHashAlgorithm) vector.elementAt(i);
            if (z || signatureAndHashAlgorithm.getSignature() != 0) {
                signatureAndHashAlgorithm.encode(outputStream);
                i++;
            } else {
                throw new IllegalArgumentException("SignatureAlgorithm.anonymous MUST NOT appear in the signature_algorithms extension");
            }
        }
    }

    public static byte[] encodeUint16ArrayWithUint16Length(int[] iArr) throws IOException {
        byte[] bArr = new byte[((iArr.length * 2) + 2)];
        writeUint16ArrayWithUint16Length(iArr, bArr, 0);
        return bArr;
    }

    public static byte[] encodeUint8ArrayWithUint8Length(short[] sArr) throws IOException {
        byte[] bArr = new byte[(sArr.length + 1)];
        writeUint8ArrayWithUint8Length(sArr, bArr, 0);
        return bArr;
    }

    private static byte[][] genSSL3Const() {
        byte[][] bArr = new byte[10][];
        int i = 0;
        while (i < 10) {
            int i2 = i + 1;
            byte[] bArr2 = new byte[i2];
            Arrays.fill(bArr2, (byte) (i + 65));
            bArr[i] = bArr2;
            i = i2;
        }
        return bArr;
    }

    public static Vector getAllSignatureAlgorithms() {
        Vector vector = new Vector(4);
        vector.addElement(Shorts.valueOf(0));
        vector.addElement(Shorts.valueOf(1));
        vector.addElement(Shorts.valueOf(2));
        vector.addElement(Shorts.valueOf(3));
        return vector;
    }

    public static int getCipherType(int i) throws IOException {
        int encryptionAlgorithm = getEncryptionAlgorithm(i);
        if (encryptionAlgorithm == 103 || encryptionAlgorithm == 104) {
            return 2;
        }
        switch (encryptionAlgorithm) {
            case 0:
            case 1:
            case 2:
                return 0;
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 12:
            case 13:
            case 14:
                return 1;
            case 10:
            case 11:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
                return 2;
            default:
                throw new TlsFatalAlert(80);
        }
    }

    static short getClientCertificateType(Certificate certificate, Certificate certificate2) throws IOException {
        if (certificate.isEmpty()) {
            return -1;
        }
        Certificate certificateAt = certificate.getCertificateAt(0);
        try {
            AsymmetricKeyParameter createKey = PublicKeyFactory.createKey(certificateAt.getSubjectPublicKeyInfo());
            if (createKey.isPrivate()) {
                throw new TlsFatalAlert(80);
            } else if (createKey instanceof RSAKeyParameters) {
                validateKeyUsage(certificateAt, 128);
                return 1;
            } else if (createKey instanceof DSAPublicKeyParameters) {
                validateKeyUsage(certificateAt, 128);
                return 2;
            } else if (createKey instanceof ECPublicKeyParameters) {
                validateKeyUsage(certificateAt, 128);
                return 64;
            } else {
                throw new TlsFatalAlert(43);
            }
        } catch (Exception e) {
            throw new TlsFatalAlert(43, e);
        }
    }

    public static Vector getDefaultDSSSignatureAlgorithms() {
        return vectorOfOne(new SignatureAndHashAlgorithm(2, 2));
    }

    public static Vector getDefaultECDSASignatureAlgorithms() {
        return vectorOfOne(new SignatureAndHashAlgorithm(2, 3));
    }

    public static Vector getDefaultRSASignatureAlgorithms() {
        return vectorOfOne(new SignatureAndHashAlgorithm(2, 1));
    }

    public static Vector getDefaultSupportedSignatureAlgorithms() {
        short[] sArr = {2, 3, 4, 5, 6};
        short[] sArr2 = {1, 2, 3};
        Vector vector = new Vector();
        for (int i = 0; i < sArr2.length; i++) {
            for (short s : sArr) {
                vector.addElement(new SignatureAndHashAlgorithm(s, sArr2[i]));
            }
        }
        return vector;
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x004b A[RETURN] */
    public static int getEncryptionAlgorithm(int i) throws IOException {
        if (!(i == 1 || i == 2)) {
            if (!(i == 4 || i == 5)) {
                switch (i) {
                    case 10:
                    case 13:
                    case 16:
                    case 19:
                    case 22:
                    case 27:
                    case CipherSuite.TLS_PSK_WITH_3DES_EDE_CBC_SHA:
                    case CipherSuite.TLS_DHE_PSK_WITH_3DES_EDE_CBC_SHA:
                    case CipherSuite.TLS_RSA_PSK_WITH_3DES_EDE_CBC_SHA:
                        return 7;
                    case 24:
                    case CipherSuite.TLS_PSK_WITH_RC4_128_SHA:
                    case CipherSuite.TLS_DHE_PSK_WITH_RC4_128_SHA:
                    case CipherSuite.TLS_RSA_PSK_WITH_RC4_128_SHA:
                        break;
                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_CBC_SHA:
                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA:
                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA:
                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA:
                    case 136:
                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA:
                    case 192:
                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256:
                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256:
                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256:
                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256:
                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256:
                        return 13;
                    case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA:
                    case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA:
                    case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA:
                    case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256:
                    case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA256:
                    case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA256:
                        return 8;
                    case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA:
                    case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA:
                    case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA:
                    case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA384:
                    case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384:
                    case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA384:
                        return 9;
                    case CipherSuite.TLS_RSA_WITH_SEED_CBC_SHA:
                    case CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA:
                    case CipherSuite.TLS_DH_RSA_WITH_SEED_CBC_SHA:
                    case CipherSuite.TLS_DHE_DSS_WITH_SEED_CBC_SHA:
                    case CipherSuite.TLS_DHE_RSA_WITH_SEED_CBC_SHA:
                    case CipherSuite.TLS_DH_anon_WITH_SEED_CBC_SHA:
                        return 14;
                    case CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256:
                    case CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256:
                    case CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256:
                    case CipherSuite.TLS_DHE_DSS_WITH_AES_128_GCM_SHA256:
                    case CipherSuite.TLS_DH_DSS_WITH_AES_128_GCM_SHA256:
                    case CipherSuite.TLS_DH_anon_WITH_AES_128_GCM_SHA256:
                    case 168:
                    case CipherSuite.TLS_DHE_PSK_WITH_AES_128_GCM_SHA256:
                    case CipherSuite.TLS_RSA_PSK_WITH_AES_128_GCM_SHA256:
                        return 10;
                    case CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_DH_RSA_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_DH_DSS_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_PSK_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384:
                    case CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384:
                        return 11;
                    case CipherSuite.TLS_PSK_WITH_NULL_SHA256:
                    case CipherSuite.TLS_PSK_WITH_NULL_SHA384:
                    case 180:
                    case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA384:
                    case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA256:
                    case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA384:
                        return 0;
                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256:
                    case 188:
                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256:
                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256:
                        return 12;
                    default:
                        switch (i) {
                            case 44:
                            case 45:
                            case 46:
                                break;
                            case 47:
                            case 48:
                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_CBC_SHA:
                            case 50:
                            case 51:
                            case 52:
                            case 60:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_CBC_SHA256:
                            case 63:
                            case 64:
                                return 8;
                            case 53:
                            case 54:
                            case 55:
                            case 56:
                            case 57:
                            case 58:
                            case 61:
                                return 9;
                            case 59:
                                break;
                            case 65:
                            case 66:
                            case 67:
                            case 68:
                            case 69:
                            case 70:
                                return 12;
                            default:
                                switch (i) {
                                    case 103:
                                    case 108:
                                        return 8;
                                    case 104:
                                    case CipherSuite.TLS_DH_RSA_WITH_AES_256_CBC_SHA256:
                                    case CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256:
                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256:
                                    case CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256:
                                        return 9;
                                    default:
                                        switch (i) {
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_NULL_SHA:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_NULL_SHA:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_NULL_SHA:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_NULL_SHA:
                                            case CipherSuite.TLS_ECDH_anon_WITH_NULL_SHA:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA:
                                                break;
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_RC4_128_SHA:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_RC4_128_SHA:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA:
                                            case CipherSuite.TLS_ECDH_anon_WITH_RC4_128_SHA:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_RC4_128_SHA:
                                                break;
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA:
                                                return 7;
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_ECDH_anon_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256:
                                                return 8;
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_ECDH_anon_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384:
                                                return 9;
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256:
                                                return 10;
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384:
                                                return 11;
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA256:
                                            case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA384:
                                                break;
                                            default:
                                                switch (i) {
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_128_CBC_SHA256:
                                                        return 12;
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_256_CBC_SHA384:
                                                        return 13;
                                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256:
                                                        return 19;
                                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384:
                                                        return 20;
                                                    case CipherSuite.TLS_RSA_WITH_AES_128_CCM:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM:
                                                    case CipherSuite.TLS_PSK_WITH_AES_128_CCM:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM:
                                                        return 15;
                                                    case CipherSuite.TLS_RSA_WITH_AES_256_CCM:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM:
                                                    case CipherSuite.TLS_PSK_WITH_AES_256_CCM:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM:
                                                        return 17;
                                                    case CipherSuite.TLS_RSA_WITH_AES_128_CCM_8:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM_8:
                                                    case CipherSuite.TLS_PSK_WITH_AES_128_CCM_8:
                                                    case CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8:
                                                        return 16;
                                                    case CipherSuite.TLS_RSA_WITH_AES_256_CCM_8:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM_8:
                                                    case CipherSuite.TLS_PSK_WITH_AES_256_CCM_8:
                                                    case CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8:
                                                        return 18;
                                                    default:
                                                        switch (i) {
                                                            case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:
                                                            case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
                                                            case CipherSuite.DRAFT_TLS_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                            case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                            case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                            case CipherSuite.DRAFT_TLS_RSA_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                                return 21;
                                                            default:
                                                                switch (i) {
                                                                    case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB:
                                                                    case 65282:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_128_OCB:
                                                                        return 103;
                                                                    case 65281:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_AES_256_OCB:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_256_OCB:
                                                                        return 104;
                                                                    default:
                                                                        switch (i) {
                                                                            case CipherSuite.DRAFT_TLS_PSK_WITH_AES_128_OCB:
                                                                            case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_128_OCB:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_128_OCB:
                                                                                return 103;
                                                                            case CipherSuite.DRAFT_TLS_PSK_WITH_AES_256_OCB:
                                                                            case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_256_OCB:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_256_OCB:
                                                                                return 104;
                                                                            default:
                                                                                throw new TlsFatalAlert(80);
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
            }
            return 2;
        }
        return 0;
    }

    public static byte[] getExtensionData(Hashtable hashtable, Integer num) {
        if (hashtable == null) {
            return null;
        }
        return (byte[]) hashtable.get(num);
    }

    public static short getHashAlgorithmForPRFAlgorithm(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("legacy PRF not a valid algorithm");
        } else if (i == 1) {
            return 4;
        } else {
            if (i == 2) {
                return 5;
            }
            throw new IllegalArgumentException("unknown PRFAlgorithm");
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Failed to find switch 'out' block
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:786)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:50)
        */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0053 A[RETURN] */
    public static int getKeyExchangeAlgorithm(int r2) throws IOException {
        /*
            r0 = 1
            if (r2 == r0) goto L_0x005b
            r1 = 2
            if (r2 == r1) goto L_0x005b
            r1 = 4
            if (r2 == r1) goto L_0x005b
            r1 = 5
            if (r2 == r1) goto L_0x005b
            switch(r2) {
                case 10: goto L_0x005b;
                case 13: goto L_0x0059;
                case 16: goto L_0x0056;
                case 19: goto L_0x0054;
                case 22: goto L_0x0053;
                case 24: goto L_0x0050;
                case 27: goto L_0x0050;
                case 132: goto L_0x005b;
                case 133: goto L_0x0059;
                case 134: goto L_0x0056;
                case 135: goto L_0x0054;
                case 136: goto L_0x0053;
                case 137: goto L_0x0050;
                case 138: goto L_0x004d;
                case 139: goto L_0x004d;
                case 140: goto L_0x004d;
                case 141: goto L_0x004d;
                case 142: goto L_0x004a;
                case 143: goto L_0x004a;
                case 144: goto L_0x004a;
                case 145: goto L_0x004a;
                case 146: goto L_0x0047;
                case 147: goto L_0x0047;
                case 148: goto L_0x0047;
                case 149: goto L_0x0047;
                case 150: goto L_0x005b;
                case 151: goto L_0x0059;
                case 152: goto L_0x0056;
                case 153: goto L_0x0054;
                case 154: goto L_0x0053;
                case 155: goto L_0x0050;
                case 156: goto L_0x005b;
                case 157: goto L_0x005b;
                case 158: goto L_0x0053;
                case 159: goto L_0x0053;
                case 160: goto L_0x0056;
                case 161: goto L_0x0056;
                case 162: goto L_0x0054;
                case 163: goto L_0x0054;
                case 164: goto L_0x0059;
                case 165: goto L_0x0059;
                case 166: goto L_0x0050;
                case 167: goto L_0x0050;
                case 168: goto L_0x004d;
                case 169: goto L_0x004d;
                case 170: goto L_0x004a;
                case 171: goto L_0x004a;
                case 172: goto L_0x0047;
                case 173: goto L_0x0047;
                case 174: goto L_0x004d;
                case 175: goto L_0x004d;
                case 176: goto L_0x004d;
                case 177: goto L_0x004d;
                case 178: goto L_0x004a;
                case 179: goto L_0x004a;
                case 180: goto L_0x004a;
                case 181: goto L_0x004a;
                case 182: goto L_0x0047;
                case 183: goto L_0x0047;
                case 184: goto L_0x0047;
                case 185: goto L_0x0047;
                case 186: goto L_0x005b;
                case 187: goto L_0x0059;
                case 188: goto L_0x0056;
                case 189: goto L_0x0054;
                case 190: goto L_0x0053;
                case 191: goto L_0x0050;
                case 192: goto L_0x005b;
                case 193: goto L_0x0059;
                case 194: goto L_0x0056;
                case 195: goto L_0x0054;
                case 196: goto L_0x0053;
                case 197: goto L_0x0050;
                default: goto L_0x000f;
            }
        L_0x000f:
            switch(r2) {
                case 44: goto L_0x004d;
                case 45: goto L_0x004a;
                case 46: goto L_0x0047;
                case 47: goto L_0x005b;
                case 48: goto L_0x0059;
                case 49: goto L_0x0056;
                case 50: goto L_0x0054;
                case 51: goto L_0x0053;
                case 52: goto L_0x0050;
                case 53: goto L_0x005b;
                case 54: goto L_0x0059;
                case 55: goto L_0x0056;
                case 56: goto L_0x0054;
                case 57: goto L_0x0053;
                case 58: goto L_0x0050;
                case 59: goto L_0x005b;
                case 60: goto L_0x005b;
                case 61: goto L_0x005b;
                case 62: goto L_0x0059;
                case 63: goto L_0x0056;
                case 64: goto L_0x0054;
                case 65: goto L_0x005b;
                case 66: goto L_0x0059;
                case 67: goto L_0x0056;
                case 68: goto L_0x0054;
                case 69: goto L_0x0053;
                case 70: goto L_0x0050;
                default: goto L_0x0012;
            }
        L_0x0012:
            switch(r2) {
                case 103: goto L_0x0053;
                case 104: goto L_0x0059;
                case 105: goto L_0x0056;
                case 106: goto L_0x0054;
                case 107: goto L_0x0053;
                case 108: goto L_0x0050;
                case 109: goto L_0x0050;
                default: goto L_0x0015;
            }
        L_0x0015:
            switch(r2) {
                case 49153: goto L_0x0044;
                case 49154: goto L_0x0044;
                case 49155: goto L_0x0044;
                case 49156: goto L_0x0044;
                case 49157: goto L_0x0044;
                case 49158: goto L_0x0041;
                case 49159: goto L_0x0041;
                case 49160: goto L_0x0041;
                case 49161: goto L_0x0041;
                case 49162: goto L_0x0041;
                case 49163: goto L_0x003e;
                case 49164: goto L_0x003e;
                case 49165: goto L_0x003e;
                case 49166: goto L_0x003e;
                case 49167: goto L_0x003e;
                case 49168: goto L_0x003b;
                case 49169: goto L_0x003b;
                case 49170: goto L_0x003b;
                case 49171: goto L_0x003b;
                case 49172: goto L_0x003b;
                case 49173: goto L_0x0038;
                case 49174: goto L_0x0038;
                case 49175: goto L_0x0038;
                case 49176: goto L_0x0038;
                case 49177: goto L_0x0038;
                case 49178: goto L_0x0035;
                case 49179: goto L_0x0032;
                case 49180: goto L_0x002f;
                case 49181: goto L_0x0035;
                case 49182: goto L_0x0032;
                case 49183: goto L_0x002f;
                case 49184: goto L_0x0035;
                case 49185: goto L_0x0032;
                case 49186: goto L_0x002f;
                case 49187: goto L_0x0041;
                case 49188: goto L_0x0041;
                case 49189: goto L_0x0044;
                case 49190: goto L_0x0044;
                case 49191: goto L_0x003b;
                case 49192: goto L_0x003b;
                case 49193: goto L_0x003e;
                case 49194: goto L_0x003e;
                case 49195: goto L_0x0041;
                case 49196: goto L_0x0041;
                case 49197: goto L_0x0044;
                case 49198: goto L_0x0044;
                case 49199: goto L_0x003b;
                case 49200: goto L_0x003b;
                case 49201: goto L_0x003e;
                case 49202: goto L_0x003e;
                case 49203: goto L_0x002c;
                case 49204: goto L_0x002c;
                case 49205: goto L_0x002c;
                case 49206: goto L_0x002c;
                case 49207: goto L_0x002c;
                case 49208: goto L_0x002c;
                case 49209: goto L_0x002c;
                case 49210: goto L_0x002c;
                case 49211: goto L_0x002c;
                default: goto L_0x0018;
            }
        L_0x0018:
            switch(r2) {
                case 49266: goto L_0x0041;
                case 49267: goto L_0x0041;
                case 49268: goto L_0x0044;
                case 49269: goto L_0x0044;
                case 49270: goto L_0x003b;
                case 49271: goto L_0x003b;
                case 49272: goto L_0x003e;
                case 49273: goto L_0x003e;
                case 49274: goto L_0x005b;
                case 49275: goto L_0x005b;
                case 49276: goto L_0x0053;
                case 49277: goto L_0x0053;
                case 49278: goto L_0x0056;
                case 49279: goto L_0x0056;
                case 49280: goto L_0x0054;
                case 49281: goto L_0x0054;
                case 49282: goto L_0x0059;
                case 49283: goto L_0x0059;
                case 49284: goto L_0x0050;
                case 49285: goto L_0x0050;
                case 49286: goto L_0x0041;
                case 49287: goto L_0x0041;
                case 49288: goto L_0x0044;
                case 49289: goto L_0x0044;
                case 49290: goto L_0x003b;
                case 49291: goto L_0x003b;
                case 49292: goto L_0x003e;
                case 49293: goto L_0x003e;
                case 49294: goto L_0x004d;
                case 49295: goto L_0x004d;
                case 49296: goto L_0x004a;
                case 49297: goto L_0x004a;
                case 49298: goto L_0x0047;
                case 49299: goto L_0x0047;
                case 49300: goto L_0x004d;
                case 49301: goto L_0x004d;
                case 49302: goto L_0x004a;
                case 49303: goto L_0x004a;
                case 49304: goto L_0x0047;
                case 49305: goto L_0x0047;
                case 49306: goto L_0x002c;
                case 49307: goto L_0x002c;
                case 49308: goto L_0x005b;
                case 49309: goto L_0x005b;
                case 49310: goto L_0x0053;
                case 49311: goto L_0x0053;
                case 49312: goto L_0x005b;
                case 49313: goto L_0x005b;
                case 49314: goto L_0x0053;
                case 49315: goto L_0x0053;
                case 49316: goto L_0x004d;
                case 49317: goto L_0x004d;
                case 49318: goto L_0x004a;
                case 49319: goto L_0x004a;
                case 49320: goto L_0x004d;
                case 49321: goto L_0x004d;
                case 49322: goto L_0x004a;
                case 49323: goto L_0x004a;
                case 49324: goto L_0x0041;
                case 49325: goto L_0x0041;
                case 49326: goto L_0x0041;
                case 49327: goto L_0x0041;
                default: goto L_0x001b;
            }
        L_0x001b:
            switch(r2) {
                case 52392: goto L_0x003b;
                case 52393: goto L_0x0041;
                case 52394: goto L_0x0053;
                case 52395: goto L_0x004d;
                case 52396: goto L_0x002c;
                case 52397: goto L_0x004a;
                case 52398: goto L_0x0047;
                default: goto L_0x001e;
            }
        L_0x001e:
            switch(r2) {
                case 65280: goto L_0x0053;
                case 65281: goto L_0x0053;
                case 65282: goto L_0x003b;
                case 65283: goto L_0x003b;
                case 65284: goto L_0x0041;
                case 65285: goto L_0x0041;
                default: goto L_0x0021;
            }
        L_0x0021:
            switch(r2) {
                case 65296: goto L_0x004d;
                case 65297: goto L_0x004d;
                case 65298: goto L_0x004a;
                case 65299: goto L_0x004a;
                case 65300: goto L_0x002c;
                case 65301: goto L_0x002c;
                default: goto L_0x0024;
            }
        L_0x0024:
            org.bouncycastle.crypto.tls.TlsFatalAlert r2 = new org.bouncycastle.crypto.tls.TlsFatalAlert
            r0 = 80
            r2.<init>(r0)
            throw r2
        L_0x002c:
            r2 = 24
            return r2
        L_0x002f:
            r2 = 22
            return r2
        L_0x0032:
            r2 = 23
            return r2
        L_0x0035:
            r2 = 21
            return r2
        L_0x0038:
            r2 = 20
            return r2
        L_0x003b:
            r2 = 19
            return r2
        L_0x003e:
            r2 = 18
            return r2
        L_0x0041:
            r2 = 17
            return r2
        L_0x0044:
            r2 = 16
            return r2
        L_0x0047:
            r2 = 15
            return r2
        L_0x004a:
            r2 = 14
            return r2
        L_0x004d:
            r2 = 13
            return r2
        L_0x0050:
            r2 = 11
            return r2
        L_0x0053:
            return r1
        L_0x0054:
            r2 = 3
            return r2
        L_0x0056:
            r2 = 9
            return r2
        L_0x0059:
            r2 = 7
            return r2
        L_0x005b:
            return r0
            switch-data {10->0x005b, 13->0x0059, 16->0x0056, 19->0x0054, 22->0x0053, 24->0x0050, 27->0x0050, 132->0x005b, 133->0x0059, 134->0x0056, 135->0x0054, 136->0x0053, 137->0x0050, 138->0x004d, 139->0x004d, 140->0x004d, 141->0x004d, 142->0x004a, 143->0x004a, 144->0x004a, 145->0x004a, 146->0x0047, 147->0x0047, 148->0x0047, 149->0x0047, 150->0x005b, 151->0x0059, 152->0x0056, 153->0x0054, 154->0x0053, 155->0x0050, 156->0x005b, 157->0x005b, 158->0x0053, 159->0x0053, 160->0x0056, 161->0x0056, 162->0x0054, 163->0x0054, 164->0x0059, 165->0x0059, 166->0x0050, 167->0x0050, 168->0x004d, 169->0x004d, 170->0x004a, 171->0x004a, 172->0x0047, 173->0x0047, 174->0x004d, 175->0x004d, 176->0x004d, 177->0x004d, 178->0x004a, 179->0x004a, 180->0x004a, 181->0x004a, 182->0x0047, 183->0x0047, 184->0x0047, 185->0x0047, 186->0x005b, 187->0x0059, 188->0x0056, 189->0x0054, 190->0x0053, 191->0x0050, 192->0x005b, 193->0x0059, 194->0x0056, 195->0x0054, 196->0x0053, 197->0x0050, }
            switch-data {44->0x004d, 45->0x004a, 46->0x0047, 47->0x005b, 48->0x0059, 49->0x0056, 50->0x0054, 51->0x0053, 52->0x0050, 53->0x005b, 54->0x0059, 55->0x0056, 56->0x0054, 57->0x0053, 58->0x0050, 59->0x005b, 60->0x005b, 61->0x005b, 62->0x0059, 63->0x0056, 64->0x0054, 65->0x005b, 66->0x0059, 67->0x0056, 68->0x0054, 69->0x0053, 70->0x0050, }
            switch-data {103->0x0053, 104->0x0059, 105->0x0056, 106->0x0054, 107->0x0053, 108->0x0050, 109->0x0050, }
            switch-data {49153->0x0044, 49154->0x0044, 49155->0x0044, 49156->0x0044, 49157->0x0044, 49158->0x0041, 49159->0x0041, 49160->0x0041, 49161->0x0041, 49162->0x0041, 49163->0x003e, 49164->0x003e, 49165->0x003e, 49166->0x003e, 49167->0x003e, 49168->0x003b, 49169->0x003b, 49170->0x003b, 49171->0x003b, 49172->0x003b, 49173->0x0038, 49174->0x0038, 49175->0x0038, 49176->0x0038, 49177->0x0038, 49178->0x0035, 49179->0x0032, 49180->0x002f, 49181->0x0035, 49182->0x0032, 49183->0x002f, 49184->0x0035, 49185->0x0032, 49186->0x002f, 49187->0x0041, 49188->0x0041, 49189->0x0044, 49190->0x0044, 49191->0x003b, 49192->0x003b, 49193->0x003e, 49194->0x003e, 49195->0x0041, 49196->0x0041, 49197->0x0044, 49198->0x0044, 49199->0x003b, 49200->0x003b, 49201->0x003e, 49202->0x003e, 49203->0x002c, 49204->0x002c, 49205->0x002c, 49206->0x002c, 49207->0x002c, 49208->0x002c, 49209->0x002c, 49210->0x002c, 49211->0x002c, }
            switch-data {49266->0x0041, 49267->0x0041, 49268->0x0044, 49269->0x0044, 49270->0x003b, 49271->0x003b, 49272->0x003e, 49273->0x003e, 49274->0x005b, 49275->0x005b, 49276->0x0053, 49277->0x0053, 49278->0x0056, 49279->0x0056, 49280->0x0054, 49281->0x0054, 49282->0x0059, 49283->0x0059, 49284->0x0050, 49285->0x0050, 49286->0x0041, 49287->0x0041, 49288->0x0044, 49289->0x0044, 49290->0x003b, 49291->0x003b, 49292->0x003e, 49293->0x003e, 49294->0x004d, 49295->0x004d, 49296->0x004a, 49297->0x004a, 49298->0x0047, 49299->0x0047, 49300->0x004d, 49301->0x004d, 49302->0x004a, 49303->0x004a, 49304->0x0047, 49305->0x0047, 49306->0x002c, 49307->0x002c, 49308->0x005b, 49309->0x005b, 49310->0x0053, 49311->0x0053, 49312->0x005b, 49313->0x005b, 49314->0x0053, 49315->0x0053, 49316->0x004d, 49317->0x004d, 49318->0x004a, 49319->0x004a, 49320->0x004d, 49321->0x004d, 49322->0x004a, 49323->0x004a, 49324->0x0041, 49325->0x0041, 49326->0x0041, 49327->0x0041, }
            switch-data {52392->0x003b, 52393->0x0041, 52394->0x0053, 52395->0x004d, 52396->0x002c, 52397->0x004a, 52398->0x0047, }
            switch-data {65280->0x0053, 65281->0x0053, 65282->0x003b, 65283->0x003b, 65284->0x0041, 65285->0x0041, }
            switch-data {65296->0x004d, 65297->0x004d, 65298->0x004a, 65299->0x004a, 65300->0x002c, 65301->0x002c, }
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.tls.TlsUtils.getKeyExchangeAlgorithm(int):int");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Failed to find switch 'out' block
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:786)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processSwitch(RegionMaker.java:825)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:130)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMaker.processIf(RegionMaker.java:696)
        	at jadx.core.dex.visitors.regions.RegionMaker.traverse(RegionMaker.java:125)
        	at jadx.core.dex.visitors.regions.RegionMaker.makeRegion(RegionMaker.java:88)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:50)
        */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x002c A[RETURN] */
    public static int getMACAlgorithm(int r4) throws IOException {
        /*
            r0 = 1
            if (r4 == r0) goto L_0x0032
            r1 = 2
            if (r4 == r1) goto L_0x0031
            r2 = 4
            if (r4 == r2) goto L_0x0032
            r3 = 5
            if (r4 == r3) goto L_0x0031
            switch(r4) {
                case 10: goto L_0x0031;
                case 13: goto L_0x0031;
                case 16: goto L_0x0031;
                case 19: goto L_0x0031;
                case 22: goto L_0x0031;
                case 24: goto L_0x0032;
                case 27: goto L_0x0031;
                case 132: goto L_0x0031;
                case 133: goto L_0x0031;
                case 134: goto L_0x0031;
                case 135: goto L_0x0031;
                case 136: goto L_0x0031;
                case 137: goto L_0x0031;
                case 138: goto L_0x0031;
                case 139: goto L_0x0031;
                case 140: goto L_0x0031;
                case 141: goto L_0x0031;
                case 142: goto L_0x0031;
                case 143: goto L_0x0031;
                case 144: goto L_0x0031;
                case 145: goto L_0x0031;
                case 146: goto L_0x0031;
                case 147: goto L_0x0031;
                case 148: goto L_0x0031;
                case 149: goto L_0x0031;
                case 150: goto L_0x0031;
                case 151: goto L_0x0031;
                case 152: goto L_0x0031;
                case 153: goto L_0x0031;
                case 154: goto L_0x0031;
                case 155: goto L_0x0031;
                case 156: goto L_0x002f;
                case 157: goto L_0x002f;
                case 158: goto L_0x002f;
                case 159: goto L_0x002f;
                case 160: goto L_0x002f;
                case 161: goto L_0x002f;
                case 162: goto L_0x002f;
                case 163: goto L_0x002f;
                case 164: goto L_0x002f;
                case 165: goto L_0x002f;
                case 166: goto L_0x002f;
                case 167: goto L_0x002f;
                case 168: goto L_0x002f;
                case 169: goto L_0x002f;
                case 170: goto L_0x002f;
                case 171: goto L_0x002f;
                case 172: goto L_0x002f;
                case 173: goto L_0x002f;
                case 174: goto L_0x002d;
                case 175: goto L_0x002c;
                case 176: goto L_0x002d;
                case 177: goto L_0x002c;
                case 178: goto L_0x002d;
                case 179: goto L_0x002c;
                case 180: goto L_0x002d;
                case 181: goto L_0x002c;
                case 182: goto L_0x002d;
                case 183: goto L_0x002c;
                case 184: goto L_0x002d;
                case 185: goto L_0x002c;
                case 186: goto L_0x002d;
                case 187: goto L_0x002d;
                case 188: goto L_0x002d;
                case 189: goto L_0x002d;
                case 190: goto L_0x002d;
                case 191: goto L_0x002d;
                case 192: goto L_0x002d;
                case 193: goto L_0x002d;
                case 194: goto L_0x002d;
                case 195: goto L_0x002d;
                case 196: goto L_0x002d;
                case 197: goto L_0x002d;
                default: goto L_0x000f;
            }
        L_0x000f:
            switch(r4) {
                case 44: goto L_0x0031;
                case 45: goto L_0x0031;
                case 46: goto L_0x0031;
                case 47: goto L_0x0031;
                case 48: goto L_0x0031;
                case 49: goto L_0x0031;
                case 50: goto L_0x0031;
                case 51: goto L_0x0031;
                case 52: goto L_0x0031;
                case 53: goto L_0x0031;
                case 54: goto L_0x0031;
                case 55: goto L_0x0031;
                case 56: goto L_0x0031;
                case 57: goto L_0x0031;
                case 58: goto L_0x0031;
                case 59: goto L_0x002d;
                case 60: goto L_0x002d;
                case 61: goto L_0x002d;
                case 62: goto L_0x002d;
                case 63: goto L_0x002d;
                case 64: goto L_0x002d;
                case 65: goto L_0x0031;
                case 66: goto L_0x0031;
                case 67: goto L_0x0031;
                case 68: goto L_0x0031;
                case 69: goto L_0x0031;
                case 70: goto L_0x0031;
                default: goto L_0x0012;
            }
        L_0x0012:
            switch(r4) {
                case 103: goto L_0x002d;
                case 104: goto L_0x002d;
                case 105: goto L_0x002d;
                case 106: goto L_0x002d;
                case 107: goto L_0x002d;
                case 108: goto L_0x002d;
                case 109: goto L_0x002d;
                default: goto L_0x0015;
            }
        L_0x0015:
            switch(r4) {
                case 49153: goto L_0x0031;
                case 49154: goto L_0x0031;
                case 49155: goto L_0x0031;
                case 49156: goto L_0x0031;
                case 49157: goto L_0x0031;
                case 49158: goto L_0x0031;
                case 49159: goto L_0x0031;
                case 49160: goto L_0x0031;
                case 49161: goto L_0x0031;
                case 49162: goto L_0x0031;
                case 49163: goto L_0x0031;
                case 49164: goto L_0x0031;
                case 49165: goto L_0x0031;
                case 49166: goto L_0x0031;
                case 49167: goto L_0x0031;
                case 49168: goto L_0x0031;
                case 49169: goto L_0x0031;
                case 49170: goto L_0x0031;
                case 49171: goto L_0x0031;
                case 49172: goto L_0x0031;
                case 49173: goto L_0x0031;
                case 49174: goto L_0x0031;
                case 49175: goto L_0x0031;
                case 49176: goto L_0x0031;
                case 49177: goto L_0x0031;
                case 49178: goto L_0x0031;
                case 49179: goto L_0x0031;
                case 49180: goto L_0x0031;
                case 49181: goto L_0x0031;
                case 49182: goto L_0x0031;
                case 49183: goto L_0x0031;
                case 49184: goto L_0x0031;
                case 49185: goto L_0x0031;
                case 49186: goto L_0x0031;
                case 49187: goto L_0x002d;
                case 49188: goto L_0x002c;
                case 49189: goto L_0x002d;
                case 49190: goto L_0x002c;
                case 49191: goto L_0x002d;
                case 49192: goto L_0x002c;
                case 49193: goto L_0x002d;
                case 49194: goto L_0x002c;
                case 49195: goto L_0x002f;
                case 49196: goto L_0x002f;
                case 49197: goto L_0x002f;
                case 49198: goto L_0x002f;
                case 49199: goto L_0x002f;
                case 49200: goto L_0x002f;
                case 49201: goto L_0x002f;
                case 49202: goto L_0x002f;
                case 49203: goto L_0x0031;
                case 49204: goto L_0x0031;
                case 49205: goto L_0x0031;
                case 49206: goto L_0x0031;
                case 49207: goto L_0x002d;
                case 49208: goto L_0x002c;
                case 49209: goto L_0x0031;
                case 49210: goto L_0x002d;
                case 49211: goto L_0x002c;
                default: goto L_0x0018;
            }
        L_0x0018:
            switch(r4) {
                case 49266: goto L_0x002d;
                case 49267: goto L_0x002c;
                case 49268: goto L_0x002d;
                case 49269: goto L_0x002c;
                case 49270: goto L_0x002d;
                case 49271: goto L_0x002c;
                case 49272: goto L_0x002d;
                case 49273: goto L_0x002c;
                case 49274: goto L_0x002f;
                case 49275: goto L_0x002f;
                case 49276: goto L_0x002f;
                case 49277: goto L_0x002f;
                case 49278: goto L_0x002f;
                case 49279: goto L_0x002f;
                case 49280: goto L_0x002f;
                case 49281: goto L_0x002f;
                case 49282: goto L_0x002f;
                case 49283: goto L_0x002f;
                case 49284: goto L_0x002f;
                case 49285: goto L_0x002f;
                case 49286: goto L_0x002f;
                case 49287: goto L_0x002f;
                case 49288: goto L_0x002f;
                case 49289: goto L_0x002f;
                case 49290: goto L_0x002f;
                case 49291: goto L_0x002f;
                case 49292: goto L_0x002f;
                case 49293: goto L_0x002f;
                case 49294: goto L_0x002f;
                case 49295: goto L_0x002f;
                case 49296: goto L_0x002f;
                case 49297: goto L_0x002f;
                case 49298: goto L_0x002f;
                case 49299: goto L_0x002f;
                case 49300: goto L_0x002d;
                case 49301: goto L_0x002c;
                case 49302: goto L_0x002d;
                case 49303: goto L_0x002c;
                case 49304: goto L_0x002d;
                case 49305: goto L_0x002c;
                case 49306: goto L_0x002d;
                case 49307: goto L_0x002c;
                case 49308: goto L_0x002f;
                case 49309: goto L_0x002f;
                case 49310: goto L_0x002f;
                case 49311: goto L_0x002f;
                case 49312: goto L_0x002f;
                case 49313: goto L_0x002f;
                case 49314: goto L_0x002f;
                case 49315: goto L_0x002f;
                case 49316: goto L_0x002f;
                case 49317: goto L_0x002f;
                case 49318: goto L_0x002f;
                case 49319: goto L_0x002f;
                case 49320: goto L_0x002f;
                case 49321: goto L_0x002f;
                case 49322: goto L_0x002f;
                case 49323: goto L_0x002f;
                case 49324: goto L_0x002f;
                case 49325: goto L_0x002f;
                case 49326: goto L_0x002f;
                case 49327: goto L_0x002f;
                default: goto L_0x001b;
            }
        L_0x001b:
            switch(r4) {
                case 52392: goto L_0x002f;
                case 52393: goto L_0x002f;
                case 52394: goto L_0x002f;
                case 52395: goto L_0x002f;
                case 52396: goto L_0x002f;
                case 52397: goto L_0x002f;
                case 52398: goto L_0x002f;
                default: goto L_0x001e;
            }
        L_0x001e:
            switch(r4) {
                case 65280: goto L_0x002f;
                case 65281: goto L_0x002f;
                case 65282: goto L_0x002f;
                case 65283: goto L_0x002f;
                case 65284: goto L_0x002f;
                case 65285: goto L_0x002f;
                default: goto L_0x0021;
            }
        L_0x0021:
            switch(r4) {
                case 65296: goto L_0x002f;
                case 65297: goto L_0x002f;
                case 65298: goto L_0x002f;
                case 65299: goto L_0x002f;
                case 65300: goto L_0x002f;
                case 65301: goto L_0x002f;
                default: goto L_0x0024;
            }
        L_0x0024:
            org.bouncycastle.crypto.tls.TlsFatalAlert r4 = new org.bouncycastle.crypto.tls.TlsFatalAlert
            r0 = 80
            r4.<init>(r0)
            throw r4
        L_0x002c:
            return r2
        L_0x002d:
            r4 = 3
            return r4
        L_0x002f:
            r4 = 0
            return r4
        L_0x0031:
            return r1
        L_0x0032:
            return r0
            switch-data {10->0x0031, 13->0x0031, 16->0x0031, 19->0x0031, 22->0x0031, 24->0x0032, 27->0x0031, 132->0x0031, 133->0x0031, 134->0x0031, 135->0x0031, 136->0x0031, 137->0x0031, 138->0x0031, 139->0x0031, 140->0x0031, 141->0x0031, 142->0x0031, 143->0x0031, 144->0x0031, 145->0x0031, 146->0x0031, 147->0x0031, 148->0x0031, 149->0x0031, 150->0x0031, 151->0x0031, 152->0x0031, 153->0x0031, 154->0x0031, 155->0x0031, 156->0x002f, 157->0x002f, 158->0x002f, 159->0x002f, 160->0x002f, 161->0x002f, 162->0x002f, 163->0x002f, 164->0x002f, 165->0x002f, 166->0x002f, 167->0x002f, 168->0x002f, 169->0x002f, 170->0x002f, 171->0x002f, 172->0x002f, 173->0x002f, 174->0x002d, 175->0x002c, 176->0x002d, 177->0x002c, 178->0x002d, 179->0x002c, 180->0x002d, 181->0x002c, 182->0x002d, 183->0x002c, 184->0x002d, 185->0x002c, 186->0x002d, 187->0x002d, 188->0x002d, 189->0x002d, 190->0x002d, 191->0x002d, 192->0x002d, 193->0x002d, 194->0x002d, 195->0x002d, 196->0x002d, 197->0x002d, }
            switch-data {44->0x0031, 45->0x0031, 46->0x0031, 47->0x0031, 48->0x0031, 49->0x0031, 50->0x0031, 51->0x0031, 52->0x0031, 53->0x0031, 54->0x0031, 55->0x0031, 56->0x0031, 57->0x0031, 58->0x0031, 59->0x002d, 60->0x002d, 61->0x002d, 62->0x002d, 63->0x002d, 64->0x002d, 65->0x0031, 66->0x0031, 67->0x0031, 68->0x0031, 69->0x0031, 70->0x0031, }
            switch-data {103->0x002d, 104->0x002d, 105->0x002d, 106->0x002d, 107->0x002d, 108->0x002d, 109->0x002d, }
            switch-data {49153->0x0031, 49154->0x0031, 49155->0x0031, 49156->0x0031, 49157->0x0031, 49158->0x0031, 49159->0x0031, 49160->0x0031, 49161->0x0031, 49162->0x0031, 49163->0x0031, 49164->0x0031, 49165->0x0031, 49166->0x0031, 49167->0x0031, 49168->0x0031, 49169->0x0031, 49170->0x0031, 49171->0x0031, 49172->0x0031, 49173->0x0031, 49174->0x0031, 49175->0x0031, 49176->0x0031, 49177->0x0031, 49178->0x0031, 49179->0x0031, 49180->0x0031, 49181->0x0031, 49182->0x0031, 49183->0x0031, 49184->0x0031, 49185->0x0031, 49186->0x0031, 49187->0x002d, 49188->0x002c, 49189->0x002d, 49190->0x002c, 49191->0x002d, 49192->0x002c, 49193->0x002d, 49194->0x002c, 49195->0x002f, 49196->0x002f, 49197->0x002f, 49198->0x002f, 49199->0x002f, 49200->0x002f, 49201->0x002f, 49202->0x002f, 49203->0x0031, 49204->0x0031, 49205->0x0031, 49206->0x0031, 49207->0x002d, 49208->0x002c, 49209->0x0031, 49210->0x002d, 49211->0x002c, }
            switch-data {49266->0x002d, 49267->0x002c, 49268->0x002d, 49269->0x002c, 49270->0x002d, 49271->0x002c, 49272->0x002d, 49273->0x002c, 49274->0x002f, 49275->0x002f, 49276->0x002f, 49277->0x002f, 49278->0x002f, 49279->0x002f, 49280->0x002f, 49281->0x002f, 49282->0x002f, 49283->0x002f, 49284->0x002f, 49285->0x002f, 49286->0x002f, 49287->0x002f, 49288->0x002f, 49289->0x002f, 49290->0x002f, 49291->0x002f, 49292->0x002f, 49293->0x002f, 49294->0x002f, 49295->0x002f, 49296->0x002f, 49297->0x002f, 49298->0x002f, 49299->0x002f, 49300->0x002d, 49301->0x002c, 49302->0x002d, 49303->0x002c, 49304->0x002d, 49305->0x002c, 49306->0x002d, 49307->0x002c, 49308->0x002f, 49309->0x002f, 49310->0x002f, 49311->0x002f, 49312->0x002f, 49313->0x002f, 49314->0x002f, 49315->0x002f, 49316->0x002f, 49317->0x002f, 49318->0x002f, 49319->0x002f, 49320->0x002f, 49321->0x002f, 49322->0x002f, 49323->0x002f, 49324->0x002f, 49325->0x002f, 49326->0x002f, 49327->0x002f, }
            switch-data {52392->0x002f, 52393->0x002f, 52394->0x002f, 52395->0x002f, 52396->0x002f, 52397->0x002f, 52398->0x002f, }
            switch-data {65280->0x002f, 65281->0x002f, 65282->0x002f, 65283->0x002f, 65284->0x002f, 65285->0x002f, }
            switch-data {65296->0x002f, 65297->0x002f, 65298->0x002f, 65299->0x002f, 65300->0x002f, 65301->0x002f, }
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.tls.TlsUtils.getMACAlgorithm(int):int");
    }

    public static ProtocolVersion getMinimumVersion(int i) {
        switch (i) {
            case 59:
            case 60:
            case 61:
            case CipherSuite.TLS_DH_DSS_WITH_AES_128_CBC_SHA256:
            case 63:
            case 64:
                break;
            default:
                switch (i) {
                    case 103:
                    case 104:
                    case CipherSuite.TLS_DH_RSA_WITH_AES_256_CBC_SHA256:
                    case CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256:
                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256:
                    case 108:
                    case CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256:
                        break;
                    default:
                        switch (i) {
                            case CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384:
                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384:
                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_DH_RSA_WITH_AES_256_GCM_SHA384:
                            case CipherSuite.TLS_DHE_DSS_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_256_GCM_SHA384:
                            case CipherSuite.TLS_DH_anon_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384:
                            case 168:
                            case CipherSuite.TLS_PSK_WITH_AES_256_GCM_SHA384:
                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384:
                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_GCM_SHA256:
                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384:
                                break;
                            default:
                                switch (i) {
                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256:
                                    case 188:
                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256:
                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256:
                                    case 192:
                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256:
                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256:
                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256:
                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256:
                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256:
                                        break;
                                    default:
                                        switch (i) {
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384:
                                                break;
                                            default:
                                                switch (i) {
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384:
                                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384:
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM:
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM_8:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM_8:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM_8:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM_8:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM_8:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM_8:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8:
                                                                break;
                                                            default:
                                                                switch (i) {
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:
                                                                    case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                                    case CipherSuite.DRAFT_TLS_RSA_PSK_WITH_CHACHA20_POLY1305_SHA256:
                                                                        break;
                                                                    default:
                                                                        switch (i) {
                                                                            case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB:
                                                                            case 65281:
                                                                            case 65282:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_AES_256_OCB:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_128_OCB:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_256_OCB:
                                                                                break;
                                                                            default:
                                                                                switch (i) {
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_128_OCB:
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_256_OCB:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_128_OCB:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_256_OCB:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_128_OCB:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_256_OCB:
                                                                                        break;
                                                                                    default:
                                                                                        return ProtocolVersion.SSLv3;
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
        return ProtocolVersion.TLSv12;
    }

    public static ASN1ObjectIdentifier getOIDForHashAlgorithm(short s) {
        switch (s) {
            case 1:
                return PKCSObjectIdentifiers.md5;
            case 2:
                return X509ObjectIdentifiers.id_SHA1;
            case 3:
                return NISTObjectIdentifiers.id_sha224;
            case 4:
                return NISTObjectIdentifiers.id_sha256;
            case 5:
                return NISTObjectIdentifiers.id_sha384;
            case 6:
                return NISTObjectIdentifiers.id_sha512;
            default:
                throw new IllegalArgumentException("unknown HashAlgorithm");
        }
    }

    public static Vector getSignatureAlgorithmsExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = getExtensionData(hashtable, EXT_signature_algorithms);
        if (extensionData == null) {
            return null;
        }
        return readSignatureAlgorithmsExtension(extensionData);
    }

    public static SignatureAndHashAlgorithm getSignatureAndHashAlgorithm(TlsContext tlsContext, TlsSignerCredentials tlsSignerCredentials) throws IOException {
        if (!isTLSv12(tlsContext)) {
            return null;
        }
        SignatureAndHashAlgorithm signatureAndHashAlgorithm = tlsSignerCredentials.getSignatureAndHashAlgorithm();
        if (signatureAndHashAlgorithm != null) {
            return signatureAndHashAlgorithm;
        }
        throw new TlsFatalAlert(80);
    }

    public static Vector getUsableSignatureAlgorithms(Vector vector) {
        if (vector == null) {
            return getAllSignatureAlgorithms();
        }
        Vector vector2 = new Vector(4);
        vector2.addElement(Shorts.valueOf(0));
        for (int i = 0; i < vector.size(); i++) {
            Short valueOf = Shorts.valueOf(((SignatureAndHashAlgorithm) vector.elementAt(i)).getSignature());
            if (!vector2.contains(valueOf)) {
                vector2.addElement(valueOf);
            }
        }
        return vector2;
    }

    public static boolean hasExpectedEmptyExtensionData(Hashtable hashtable, Integer num, short s) throws IOException {
        byte[] extensionData = getExtensionData(hashtable, num);
        if (extensionData == null) {
            return false;
        }
        if (extensionData.length == 0) {
            return true;
        }
        throw new TlsFatalAlert(s);
    }

    public static boolean hasSigningCapability(short s) {
        return s == 1 || s == 2 || s == 64;
    }

    static void hmac_hash(Digest digest, byte[] bArr, byte[] bArr2, byte[] bArr3) {
        HMac hMac = new HMac(digest);
        hMac.init(new KeyParameter(bArr));
        int digestSize = digest.getDigestSize();
        int length = ((bArr3.length + digestSize) - 1) / digestSize;
        byte[] bArr4 = new byte[hMac.getMacSize()];
        byte[] bArr5 = new byte[hMac.getMacSize()];
        byte[] bArr6 = bArr2;
        int i = 0;
        while (i < length) {
            hMac.update(bArr6, 0, bArr6.length);
            hMac.doFinal(bArr4, 0);
            hMac.update(bArr4, 0, bArr4.length);
            hMac.update(bArr2, 0, bArr2.length);
            hMac.doFinal(bArr5, 0);
            int i2 = digestSize * i;
            System.arraycopy(bArr5, 0, bArr3, i2, Math.min(digestSize, bArr3.length - i2));
            i++;
            bArr6 = bArr4;
        }
    }

    public static TlsSession importSession(byte[] bArr, SessionParameters sessionParameters) {
        return new TlsSessionImpl(bArr, sessionParameters);
    }

    public static boolean isAEADCipherSuite(int i) throws IOException {
        return 2 == getCipherType(i);
    }

    public static boolean isBlockCipherSuite(int i) throws IOException {
        return 1 == getCipherType(i);
    }

    public static boolean isSSL(TlsContext tlsContext) {
        return tlsContext.getServerVersion().isSSL();
    }

    public static boolean isSignatureAlgorithmsExtensionAllowed(ProtocolVersion protocolVersion) {
        return ProtocolVersion.TLSv12.isEqualOrEarlierVersionOf(protocolVersion.getEquivalentTLSVersion());
    }

    public static boolean isStreamCipherSuite(int i) throws IOException {
        return getCipherType(i) == 0;
    }

    public static boolean isTLSv11(ProtocolVersion protocolVersion) {
        return ProtocolVersion.TLSv11.isEqualOrEarlierVersionOf(protocolVersion.getEquivalentTLSVersion());
    }

    public static boolean isTLSv11(TlsContext tlsContext) {
        return isTLSv11(tlsContext.getServerVersion());
    }

    public static boolean isTLSv12(ProtocolVersion protocolVersion) {
        return ProtocolVersion.TLSv12.isEqualOrEarlierVersionOf(protocolVersion.getEquivalentTLSVersion());
    }

    public static boolean isTLSv12(TlsContext tlsContext) {
        return isTLSv12(tlsContext.getServerVersion());
    }

    public static boolean isValidCipherSuiteForSignatureAlgorithms(int i, Vector vector) {
        short s;
        Short valueOf;
        try {
            int keyExchangeAlgorithm = getKeyExchangeAlgorithm(i);
            if (!(keyExchangeAlgorithm == 3 || keyExchangeAlgorithm == 4)) {
                if (!(keyExchangeAlgorithm == 5 || keyExchangeAlgorithm == 6)) {
                    if (!(keyExchangeAlgorithm == 11 || keyExchangeAlgorithm == 12)) {
                        if (keyExchangeAlgorithm == 17) {
                            valueOf = Shorts.valueOf(3);
                            return vector.contains(valueOf);
                        } else if (keyExchangeAlgorithm != 19) {
                            if (keyExchangeAlgorithm != 20) {
                                if (keyExchangeAlgorithm != 22) {
                                    if (keyExchangeAlgorithm != 23) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    s = 0;
                    valueOf = Shorts.valueOf(s);
                    return vector.contains(valueOf);
                }
                valueOf = Shorts.valueOf(1);
                return vector.contains(valueOf);
            }
            s = 2;
            valueOf = Shorts.valueOf(s);
            return vector.contains(valueOf);
        } catch (IOException e) {
            return true;
        }
    }

    public static boolean isValidCipherSuiteForVersion(int i, ProtocolVersion protocolVersion) {
        return getMinimumVersion(i).isEqualOrEarlierVersionOf(protocolVersion.getEquivalentTLSVersion());
    }

    public static boolean isValidUint16(int i) {
        return (65535 & i) == i;
    }

    public static boolean isValidUint16(long j) {
        return (65535 & j) == j;
    }

    public static boolean isValidUint24(int i) {
        return (16777215 & i) == i;
    }

    public static boolean isValidUint24(long j) {
        return (16777215 & j) == j;
    }

    public static boolean isValidUint32(long j) {
        return (BodyPartID.bodyIdMax & j) == j;
    }

    public static boolean isValidUint48(long j) {
        return (281474976710655L & j) == j;
    }

    public static boolean isValidUint64(long j) {
        return true;
    }

    public static boolean isValidUint8(int i) {
        return (i & 255) == i;
    }

    public static boolean isValidUint8(long j) {
        return (255 & j) == j;
    }

    public static boolean isValidUint8(short s) {
        return (s & 255) == s;
    }

    public static Vector parseSupportedSignatureAlgorithms(boolean z, InputStream inputStream) throws IOException {
        int readUint16 = readUint16(inputStream);
        if (readUint16 < 2 || (readUint16 & 1) != 0) {
            throw new TlsFatalAlert(50);
        }
        int i = readUint16 / 2;
        Vector vector = new Vector(i);
        int i2 = 0;
        while (i2 < i) {
            SignatureAndHashAlgorithm parse = SignatureAndHashAlgorithm.parse(inputStream);
            if (z || parse.getSignature() != 0) {
                vector.addElement(parse);
                i2++;
            } else {
                throw new TlsFatalAlert(47);
            }
        }
        return vector;
    }

    public static ASN1Primitive readASN1Object(byte[] bArr) throws IOException {
        ASN1InputStream aSN1InputStream = new ASN1InputStream(bArr);
        ASN1Primitive readObject = aSN1InputStream.readObject();
        if (readObject == null) {
            throw new TlsFatalAlert(50);
        } else if (aSN1InputStream.readObject() == null) {
            return readObject;
        } else {
            throw new TlsFatalAlert(50);
        }
    }

    public static byte[] readAllOrNothing(int i, InputStream inputStream) throws IOException {
        if (i < 1) {
            return EMPTY_BYTES;
        }
        byte[] bArr = new byte[i];
        int readFully = Streams.readFully(inputStream, bArr);
        if (readFully == 0) {
            return null;
        }
        if (readFully == i) {
            return bArr;
        }
        throw new EOFException();
    }

    public static ASN1Primitive readDERObject(byte[] bArr) throws IOException {
        ASN1Primitive readASN1Object = readASN1Object(bArr);
        if (Arrays.areEqual(readASN1Object.getEncoded(ASN1Encoding.DER), bArr)) {
            return readASN1Object;
        }
        throw new TlsFatalAlert(50);
    }

    public static void readFully(byte[] bArr, InputStream inputStream) throws IOException {
        int length = bArr.length;
        if (length > 0 && length != Streams.readFully(inputStream, bArr)) {
            throw new EOFException();
        }
    }

    public static byte[] readFully(int i, InputStream inputStream) throws IOException {
        if (i < 1) {
            return EMPTY_BYTES;
        }
        byte[] bArr = new byte[i];
        if (i == Streams.readFully(inputStream, bArr)) {
            return bArr;
        }
        throw new EOFException();
    }

    public static byte[] readOpaque16(InputStream inputStream) throws IOException {
        return readFully(readUint16(inputStream), inputStream);
    }

    public static byte[] readOpaque24(InputStream inputStream) throws IOException {
        return readFully(readUint24(inputStream), inputStream);
    }

    public static byte[] readOpaque8(InputStream inputStream) throws IOException {
        return readFully(readUint8(inputStream), inputStream);
    }

    public static Vector readSignatureAlgorithmsExtension(byte[] bArr) throws IOException {
        if (bArr != null) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            Vector parseSupportedSignatureAlgorithms = parseSupportedSignatureAlgorithms(false, byteArrayInputStream);
            TlsProtocol.assertEmpty(byteArrayInputStream);
            return parseSupportedSignatureAlgorithms;
        }
        throw new IllegalArgumentException("'extensionData' cannot be null");
    }

    public static int readUint16(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        int read2 = inputStream.read();
        if (read2 >= 0) {
            return read2 | (read << 8);
        }
        throw new EOFException();
    }

    public static int readUint16(byte[] bArr, int i) {
        return (bArr[i + 1] & 255) | ((bArr[i] & 255) << 8);
    }

    public static int[] readUint16Array(int i, InputStream inputStream) throws IOException {
        int[] iArr = new int[i];
        for (int i2 = 0; i2 < i; i2++) {
            iArr[i2] = readUint16(inputStream);
        }
        return iArr;
    }

    public static int readUint24(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        int read2 = inputStream.read();
        int read3 = inputStream.read();
        if (read3 >= 0) {
            return read3 | (read << 16) | (read2 << 8);
        }
        throw new EOFException();
    }

    public static int readUint24(byte[] bArr, int i) {
        int i2 = i + 1;
        return (bArr[i2 + 1] & 255) | ((bArr[i] & 255) << Tnaf.POW_2_WIDTH) | ((bArr[i2] & 255) << 8);
    }

    public static long readUint32(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        int read2 = inputStream.read();
        int read3 = inputStream.read();
        int read4 = inputStream.read();
        if (read4 >= 0) {
            return ((long) (read4 | (read << 24) | (read2 << 16) | (read3 << 8))) & BodyPartID.bodyIdMax;
        }
        throw new EOFException();
    }

    public static long readUint32(byte[] bArr, int i) {
        int i2 = i + 1;
        int i3 = i2 + 1;
        return ((long) ((bArr[i3 + 1] & 255) | ((bArr[i] & 255) << 24) | ((bArr[i2] & 255) << Tnaf.POW_2_WIDTH) | ((bArr[i3] & 255) << 8))) & BodyPartID.bodyIdMax;
    }

    public static long readUint48(InputStream inputStream) throws IOException {
        return ((((long) readUint24(inputStream)) & BodyPartID.bodyIdMax) << 24) | (BodyPartID.bodyIdMax & ((long) readUint24(inputStream)));
    }

    public static long readUint48(byte[] bArr, int i) {
        int readUint24 = readUint24(bArr, i);
        return (((long) readUint24(bArr, i + 3)) & BodyPartID.bodyIdMax) | ((((long) readUint24) & BodyPartID.bodyIdMax) << 24);
    }

    public static short readUint8(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        if (read >= 0) {
            return (short) read;
        }
        throw new EOFException();
    }

    public static short readUint8(byte[] bArr, int i) {
        return (short) (bArr[i] & 255);
    }

    public static short[] readUint8Array(int i, InputStream inputStream) throws IOException {
        short[] sArr = new short[i];
        for (int i2 = 0; i2 < i; i2++) {
            sArr[i2] = readUint8(inputStream);
        }
        return sArr;
    }

    public static ProtocolVersion readVersion(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        int read2 = inputStream.read();
        if (read2 >= 0) {
            return ProtocolVersion.get(read, read2);
        }
        throw new EOFException();
    }

    public static ProtocolVersion readVersion(byte[] bArr, int i) throws IOException {
        return ProtocolVersion.get(bArr[i] & 255, bArr[i + 1] & 255);
    }

    public static int readVersionRaw(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        int read2 = inputStream.read();
        if (read2 >= 0) {
            return read2 | (read << 8);
        }
        throw new EOFException();
    }

    public static int readVersionRaw(byte[] bArr, int i) throws IOException {
        return bArr[i + 1] | (bArr[i] << 8);
    }

    static void trackHashAlgorithms(TlsHandshakeHash tlsHandshakeHash, Vector vector) {
        if (vector != null) {
            for (int i = 0; i < vector.size(); i++) {
                short hash = ((SignatureAndHashAlgorithm) vector.elementAt(i)).getHash();
                if (HashAlgorithm.isRecognized(hash)) {
                    tlsHandshakeHash.trackHashAlgorithm(hash);
                }
            }
        }
    }

    static void validateKeyUsage(Certificate certificate, int i) throws IOException {
        KeyUsage fromExtensions;
        Extensions extensions = certificate.getTBSCertificate().getExtensions();
        if (extensions != null && (fromExtensions = KeyUsage.fromExtensions(extensions)) != null && (fromExtensions.getBytes()[0] & 255 & i) != i) {
            throw new TlsFatalAlert(46);
        }
    }

    private static Vector vectorOfOne(Object obj) {
        Vector vector = new Vector(1);
        vector.addElement(obj);
        return vector;
    }

    public static void verifySupportedSignatureAlgorithm(Vector vector, SignatureAndHashAlgorithm signatureAndHashAlgorithm) throws IOException {
        if (vector == null || vector.size() < 1 || vector.size() >= 32768) {
            throw new IllegalArgumentException("'supportedSignatureAlgorithms' must have length from 1 to (2^15 - 1)");
        } else if (signatureAndHashAlgorithm != null) {
            if (signatureAndHashAlgorithm.getSignature() != 0) {
                int i = 0;
                while (i < vector.size()) {
                    SignatureAndHashAlgorithm signatureAndHashAlgorithm2 = (SignatureAndHashAlgorithm) vector.elementAt(i);
                    if (signatureAndHashAlgorithm2.getHash() != signatureAndHashAlgorithm.getHash() || signatureAndHashAlgorithm2.getSignature() != signatureAndHashAlgorithm.getSignature()) {
                        i++;
                    } else {
                        return;
                    }
                }
            }
            throw new TlsFatalAlert(47);
        } else {
            throw new IllegalArgumentException("'signatureAlgorithm' cannot be null");
        }
    }

    public static void writeGMTUnixTime(byte[] bArr, int i) {
        int currentTimeMillis = (int) (System.currentTimeMillis() / 1000);
        bArr[i] = (byte) (currentTimeMillis >>> 24);
        bArr[i + 1] = (byte) (currentTimeMillis >>> 16);
        bArr[i + 2] = (byte) (currentTimeMillis >>> 8);
        bArr[i + 3] = (byte) currentTimeMillis;
    }

    public static void writeOpaque16(byte[] bArr, OutputStream outputStream) throws IOException {
        checkUint16(bArr.length);
        writeUint16(bArr.length, outputStream);
        outputStream.write(bArr);
    }

    public static void writeOpaque24(byte[] bArr, OutputStream outputStream) throws IOException {
        checkUint24(bArr.length);
        writeUint24(bArr.length, outputStream);
        outputStream.write(bArr);
    }

    public static void writeOpaque8(byte[] bArr, OutputStream outputStream) throws IOException {
        checkUint8(bArr.length);
        writeUint8(bArr.length, outputStream);
        outputStream.write(bArr);
    }

    public static void writeUint16(int i, OutputStream outputStream) throws IOException {
        outputStream.write(i >>> 8);
        outputStream.write(i);
    }

    public static void writeUint16(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) (i >>> 8);
        bArr[i2 + 1] = (byte) i;
    }

    public static void writeUint16Array(int[] iArr, OutputStream outputStream) throws IOException {
        for (int i : iArr) {
            writeUint16(i, outputStream);
        }
    }

    public static void writeUint16Array(int[] iArr, byte[] bArr, int i) throws IOException {
        for (int i2 : iArr) {
            writeUint16(i2, bArr, i);
            i += 2;
        }
    }

    public static void writeUint16ArrayWithUint16Length(int[] iArr, OutputStream outputStream) throws IOException {
        int length = iArr.length * 2;
        checkUint16(length);
        writeUint16(length, outputStream);
        writeUint16Array(iArr, outputStream);
    }

    public static void writeUint16ArrayWithUint16Length(int[] iArr, byte[] bArr, int i) throws IOException {
        int length = iArr.length * 2;
        checkUint16(length);
        writeUint16(length, bArr, i);
        writeUint16Array(iArr, bArr, i + 2);
    }

    public static void writeUint24(int i, OutputStream outputStream) throws IOException {
        outputStream.write((byte) (i >>> 16));
        outputStream.write((byte) (i >>> 8));
        outputStream.write((byte) i);
    }

    public static void writeUint24(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) (i >>> 16);
        bArr[i2 + 1] = (byte) (i >>> 8);
        bArr[i2 + 2] = (byte) i;
    }

    public static void writeUint32(long j, OutputStream outputStream) throws IOException {
        outputStream.write((byte) ((int) (j >>> 24)));
        outputStream.write((byte) ((int) (j >>> 16)));
        outputStream.write((byte) ((int) (j >>> 8)));
        outputStream.write((byte) ((int) j));
    }

    public static void writeUint32(long j, byte[] bArr, int i) {
        bArr[i] = (byte) ((int) (j >>> 24));
        bArr[i + 1] = (byte) ((int) (j >>> 16));
        bArr[i + 2] = (byte) ((int) (j >>> 8));
        bArr[i + 3] = (byte) ((int) j);
    }

    public static void writeUint48(long j, OutputStream outputStream) throws IOException {
        outputStream.write((byte) ((int) (j >>> 40)));
        outputStream.write((byte) ((int) (j >>> 32)));
        outputStream.write((byte) ((int) (j >>> 24)));
        outputStream.write((byte) ((int) (j >>> 16)));
        outputStream.write((byte) ((int) (j >>> 8)));
        outputStream.write((byte) ((int) j));
    }

    public static void writeUint48(long j, byte[] bArr, int i) {
        bArr[i] = (byte) ((int) (j >>> 40));
        bArr[i + 1] = (byte) ((int) (j >>> 32));
        bArr[i + 2] = (byte) ((int) (j >>> 24));
        bArr[i + 3] = (byte) ((int) (j >>> 16));
        bArr[i + 4] = (byte) ((int) (j >>> 8));
        bArr[i + 5] = (byte) ((int) j);
    }

    public static void writeUint64(long j, OutputStream outputStream) throws IOException {
        outputStream.write((byte) ((int) (j >>> 56)));
        outputStream.write((byte) ((int) (j >>> 48)));
        outputStream.write((byte) ((int) (j >>> 40)));
        outputStream.write((byte) ((int) (j >>> 32)));
        outputStream.write((byte) ((int) (j >>> 24)));
        outputStream.write((byte) ((int) (j >>> 16)));
        outputStream.write((byte) ((int) (j >>> 8)));
        outputStream.write((byte) ((int) j));
    }

    public static void writeUint64(long j, byte[] bArr, int i) {
        bArr[i] = (byte) ((int) (j >>> 56));
        bArr[i + 1] = (byte) ((int) (j >>> 48));
        bArr[i + 2] = (byte) ((int) (j >>> 40));
        bArr[i + 3] = (byte) ((int) (j >>> 32));
        bArr[i + 4] = (byte) ((int) (j >>> 24));
        bArr[i + 5] = (byte) ((int) (j >>> 16));
        bArr[i + 6] = (byte) ((int) (j >>> 8));
        bArr[i + 7] = (byte) ((int) j);
    }

    public static void writeUint8(int i, OutputStream outputStream) throws IOException {
        outputStream.write(i);
    }

    public static void writeUint8(int i, byte[] bArr, int i2) {
        bArr[i2] = (byte) i;
    }

    public static void writeUint8(short s, OutputStream outputStream) throws IOException {
        outputStream.write(s);
    }

    public static void writeUint8(short s, byte[] bArr, int i) {
        bArr[i] = (byte) s;
    }

    public static void writeUint8Array(short[] sArr, OutputStream outputStream) throws IOException {
        for (short s : sArr) {
            writeUint8(s, outputStream);
        }
    }

    public static void writeUint8Array(short[] sArr, byte[] bArr, int i) throws IOException {
        for (short s : sArr) {
            writeUint8(s, bArr, i);
            i++;
        }
    }

    public static void writeUint8ArrayWithUint8Length(short[] sArr, OutputStream outputStream) throws IOException {
        checkUint8(sArr.length);
        writeUint8(sArr.length, outputStream);
        writeUint8Array(sArr, outputStream);
    }

    public static void writeUint8ArrayWithUint8Length(short[] sArr, byte[] bArr, int i) throws IOException {
        checkUint8(sArr.length);
        writeUint8(sArr.length, bArr, i);
        writeUint8Array(sArr, bArr, i + 1);
    }

    public static void writeVersion(ProtocolVersion protocolVersion, OutputStream outputStream) throws IOException {
        outputStream.write(protocolVersion.getMajorVersion());
        outputStream.write(protocolVersion.getMinorVersion());
    }

    public static void writeVersion(ProtocolVersion protocolVersion, byte[] bArr, int i) {
        bArr[i] = (byte) protocolVersion.getMajorVersion();
        bArr[i + 1] = (byte) protocolVersion.getMinorVersion();
    }
}
