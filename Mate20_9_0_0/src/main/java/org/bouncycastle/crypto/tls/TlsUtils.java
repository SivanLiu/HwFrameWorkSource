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
    static final byte[] SSL_CLIENT = new byte[]{(byte) 67, (byte) 76, (byte) 78, (byte) 84};
    static final byte[] SSL_SERVER = new byte[]{(byte) 83, (byte) 82, (byte) 86, (byte) 82};

    public static byte[] PRF(TlsContext tlsContext, byte[] bArr, String str, byte[] bArr2, int i) {
        if (tlsContext.getServerVersion().isSSL()) {
            throw new IllegalStateException("No PRF available for SSLv3 session");
        }
        byte[] toByteArray = Strings.toByteArray(str);
        bArr2 = concat(toByteArray, bArr2);
        int prfAlgorithm = tlsContext.getSecurityParameters().getPrfAlgorithm();
        if (prfAlgorithm == 0) {
            return PRF_legacy(bArr, toByteArray, bArr2, i);
        }
        toByteArray = new byte[i];
        hmac_hash(createPRFHash(prfAlgorithm), bArr, bArr2, toByteArray);
        return toByteArray;
    }

    public static byte[] PRF_legacy(byte[] bArr, String str, byte[] bArr2, int i) {
        byte[] toByteArray = Strings.toByteArray(str);
        return PRF_legacy(bArr, toByteArray, concat(toByteArray, bArr2), i);
    }

    static byte[] PRF_legacy(byte[] bArr, byte[] bArr2, byte[] bArr3, int i) {
        int length = (bArr.length + 1) / 2;
        byte[] bArr4 = new byte[length];
        byte[] bArr5 = new byte[length];
        int i2 = 0;
        System.arraycopy(bArr, 0, bArr4, 0, length);
        System.arraycopy(bArr, bArr.length - length, bArr5, 0, length);
        bArr = new byte[i];
        bArr2 = new byte[i];
        hmac_hash(createHash((short) 1), bArr4, bArr3, bArr);
        hmac_hash(createHash((short) 2), bArr5, bArr3, bArr2);
        while (i2 < i) {
            bArr[i2] = (byte) (bArr[i2] ^ bArr2[i2]);
            i2++;
        }
        return bArr;
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
        int i3 = i2;
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
        byte[] sessionHash = securityParameters.extendedMasterSecret ? securityParameters.getSessionHash() : concat(securityParameters.getClientRandom(), securityParameters.getServerRandom());
        if (isSSL(tlsContext)) {
            return calculateMasterSecret_SSL(bArr, sessionHash);
        }
        return PRF(tlsContext, bArr, securityParameters.extendedMasterSecret ? ExporterLabel.extended_master_secret : "master secret", sessionHash, 48);
    }

    static byte[] calculateMasterSecret_SSL(byte[] bArr, byte[] bArr2) {
        Digest createHash = createHash((short) 1);
        Digest createHash2 = createHash((short) 2);
        int digestSize = createHash.getDigestSize();
        byte[] bArr3 = new byte[createHash2.getDigestSize()];
        byte[] bArr4 = new byte[(digestSize * 3)];
        int i = 0;
        int i2 = i;
        while (i < 3) {
            byte[] bArr5 = SSL3_CONST[i];
            createHash2.update(bArr5, 0, bArr5.length);
            createHash2.update(bArr, 0, bArr.length);
            createHash2.update(bArr2, 0, bArr2.length);
            createHash2.doFinal(bArr3, 0);
            createHash.update(bArr, 0, bArr.length);
            createHash.update(bArr3, 0, bArr3.length);
            createHash.doFinal(bArr4, i2);
            i2 += digestSize;
            i++;
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
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint16(long j) throws IOException {
        if (!isValidUint16(j)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint24(int i) throws IOException {
        if (!isValidUint24(i)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint24(long j) throws IOException {
        if (!isValidUint24(j)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint32(long j) throws IOException {
        if (!isValidUint32(j)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint48(long j) throws IOException {
        if (!isValidUint48(j)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint64(long j) throws IOException {
        if (!isValidUint64(j)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint8(int i) throws IOException {
        if (!isValidUint8(i)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint8(long j) throws IOException {
        if (!isValidUint8(j)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static void checkUint8(short s) throws IOException {
        if (!isValidUint8(s)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public static Digest cloneHash(short s, Digest digest) {
        switch (s) {
            case (short) 1:
                return new MD5Digest((MD5Digest) digest);
            case (short) 2:
                return new SHA1Digest((SHA1Digest) digest);
            case (short) 3:
                return new SHA224Digest((SHA224Digest) digest);
            case (short) 4:
                return new SHA256Digest((SHA256Digest) digest);
            case (short) 5:
                return new SHA384Digest((SHA384Digest) digest);
            case (short) 6:
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
            case (short) 1:
                return new MD5Digest();
            case (short) 2:
                return new SHA1Digest();
            case (short) 3:
                return new SHA224Digest();
            case (short) 4:
                return new SHA256Digest();
            case (short) 5:
                return new SHA384Digest();
            case (short) 6:
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
        if (s == (short) 64) {
            return new TlsECDSASigner();
        }
        switch (s) {
            case (short) 1:
                return new TlsRSASigner();
            case (short) 2:
                return new TlsDSSSigner();
            default:
                throw new IllegalArgumentException("'clientCertificateType' is not a type with signing capability");
        }
    }

    public static byte[] encodeOpaque8(byte[] bArr) throws IOException {
        checkUint8(bArr.length);
        return Arrays.prepend(bArr, (byte) bArr.length);
    }

    public static void encodeSupportedSignatureAlgorithms(Vector vector, boolean z, OutputStream outputStream) throws IOException {
        if (vector == null || vector.size() < 1 || vector.size() >= 32768) {
            throw new IllegalArgumentException("'supportedSignatureAlgorithms' must have length from 1 to (2^15 - 1)");
        }
        int size = 2 * vector.size();
        checkUint16(size);
        writeUint16(size, outputStream);
        size = 0;
        while (size < vector.size()) {
            SignatureAndHashAlgorithm signatureAndHashAlgorithm = (SignatureAndHashAlgorithm) vector.elementAt(size);
            if (z || signatureAndHashAlgorithm.getSignature() != (short) 0) {
                signatureAndHashAlgorithm.encode(outputStream);
                size++;
            } else {
                throw new IllegalArgumentException("SignatureAlgorithm.anonymous MUST NOT appear in the signature_algorithms extension");
            }
        }
    }

    public static byte[] encodeUint16ArrayWithUint16Length(int[] iArr) throws IOException {
        byte[] bArr = new byte[(2 + (iArr.length * 2))];
        writeUint16ArrayWithUint16Length(iArr, bArr, 0);
        return bArr;
    }

    public static byte[] encodeUint8ArrayWithUint8Length(short[] sArr) throws IOException {
        byte[] bArr = new byte[(1 + sArr.length)];
        writeUint8ArrayWithUint8Length(sArr, bArr, 0);
        return bArr;
    }

    private static byte[][] genSSL3Const() {
        byte[][] bArr = new byte[10][];
        int i = 0;
        while (i < 10) {
            int i2 = i + 1;
            byte[] bArr2 = new byte[i2];
            Arrays.fill(bArr2, (byte) (65 + i));
            bArr[i] = bArr2;
            i = i2;
        }
        return bArr;
    }

    public static Vector getAllSignatureAlgorithms() {
        Vector vector = new Vector(4);
        vector.addElement(Shorts.valueOf((short) 0));
        vector.addElement(Shorts.valueOf((short) 1));
        vector.addElement(Shorts.valueOf((short) 2));
        vector.addElement(Shorts.valueOf((short) 3));
        return vector;
    }

    public static int getCipherType(int i) throws IOException {
        i = getEncryptionAlgorithm(i);
        switch (i) {
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
                break;
            default:
                switch (i) {
                    case 103:
                    case 104:
                        break;
                    default:
                        throw new TlsFatalAlert((short) 80);
                }
        }
        return 2;
    }

    static short getClientCertificateType(Certificate certificate, Certificate certificate2) throws IOException {
        if (certificate.isEmpty()) {
            return (short) -1;
        }
        Certificate certificateAt = certificate.getCertificateAt(0);
        try {
            AsymmetricKeyParameter createKey = PublicKeyFactory.createKey(certificateAt.getSubjectPublicKeyInfo());
            if (createKey.isPrivate()) {
                throw new TlsFatalAlert((short) 80);
            } else if (createKey instanceof RSAKeyParameters) {
                validateKeyUsage(certificateAt, 128);
                return (short) 1;
            } else if (createKey instanceof DSAPublicKeyParameters) {
                validateKeyUsage(certificateAt, 128);
                return (short) 2;
            } else if (createKey instanceof ECPublicKeyParameters) {
                validateKeyUsage(certificateAt, 128);
                return (short) 64;
            } else {
                throw new TlsFatalAlert((short) 43);
            }
        } catch (Exception e) {
            throw new TlsFatalAlert((short) 43, e);
        }
    }

    public static Vector getDefaultDSSSignatureAlgorithms() {
        return vectorOfOne(new SignatureAndHashAlgorithm((short) 2, (short) 2));
    }

    public static Vector getDefaultECDSASignatureAlgorithms() {
        return vectorOfOne(new SignatureAndHashAlgorithm((short) 2, (short) 3));
    }

    public static Vector getDefaultRSASignatureAlgorithms() {
        return vectorOfOne(new SignatureAndHashAlgorithm((short) 2, (short) 1));
    }

    public static Vector getDefaultSupportedSignatureAlgorithms() {
        short[] sArr = new short[]{(short) 2, (short) 3, (short) 4, (short) 5, (short) 6};
        short[] sArr2 = new short[]{(short) 1, (short) 2, (short) 3};
        Vector vector = new Vector();
        for (short signatureAndHashAlgorithm : sArr2) {
            for (short signatureAndHashAlgorithm2 : sArr) {
                vector.addElement(new SignatureAndHashAlgorithm(signatureAndHashAlgorithm2, signatureAndHashAlgorithm));
            }
        }
        return vector;
    }

    /* JADX WARNING: Missing block: B:16:0x002d, code skipped:
            return 104;
     */
    /* JADX WARNING: Missing block: B:18:0x0030, code skipped:
            return 103;
     */
    /* JADX WARNING: Missing block: B:33:0x0046, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:35:0x0049, code skipped:
            return 11;
     */
    /* JADX WARNING: Missing block: B:37:0x004c, code skipped:
            return 10;
     */
    /* JADX WARNING: Missing block: B:41:0x0051, code skipped:
            return 7;
     */
    /* JADX WARNING: Missing block: B:43:0x0054, code skipped:
            return 13;
     */
    /* JADX WARNING: Missing block: B:45:0x0057, code skipped:
            return 12;
     */
    /* JADX WARNING: Missing block: B:46:0x0058, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:48:0x005b, code skipped:
            return 9;
     */
    /* JADX WARNING: Missing block: B:50:0x005e, code skipped:
            return 8;
     */
    /* JADX WARNING: Missing block: B:51:0x005f, code skipped:
            return 2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int getEncryptionAlgorithm(int i) throws IOException {
        switch (i) {
            case 1:
            case 2:
                break;
            default:
                switch (i) {
                    case 4:
                    case 5:
                        break;
                    default:
                        switch (i) {
                            case 44:
                            case 45:
                            case 46:
                                break;
                            case 47:
                            case 48:
                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_CBC_SHA /*49*/:
                            case 50:
                            case 51:
                            case 52:
                            case 60:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_CBC_SHA256 /*62*/:
                            case 63:
                            case 64:
                                break;
                            case 53:
                            case 54:
                            case 55:
                            case 56:
                            case 57:
                            case 58:
                            case 61:
                                break;
                            case 59:
                                break;
                            case 65:
                            case 66:
                            case 67:
                            case 68:
                            case 69:
                            case 70:
                                break;
                            default:
                                switch (i) {
                                    case 103:
                                    case CipherSuite.TLS_DH_anon_WITH_AES_128_CBC_SHA256 /*108*/:
                                        break;
                                    case 104:
                                    case CipherSuite.TLS_DH_RSA_WITH_AES_256_CBC_SHA256 /*105*/:
                                    case CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 /*106*/:
                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 /*107*/:
                                    case CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256 /*109*/:
                                        break;
                                    default:
                                        switch (i) {
                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_CBC_SHA /*132*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA /*133*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA /*134*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA /*135*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA /*136*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA /*137*/:
                                            case 192:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*193*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*194*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*195*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*196*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256 /*197*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_RC4_128_SHA /*138*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_RC4_128_SHA /*142*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_RC4_128_SHA /*146*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_3DES_EDE_CBC_SHA /*139*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_3DES_EDE_CBC_SHA /*143*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_3DES_EDE_CBC_SHA /*147*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA /*140*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA /*144*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA /*148*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256 /*174*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA256 /*178*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA256 /*182*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA /*141*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA /*145*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA /*149*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA384 /*175*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384 /*179*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA384 /*183*/:
                                                break;
                                            case CipherSuite.TLS_RSA_WITH_SEED_CBC_SHA /*150*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA /*151*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_SEED_CBC_SHA /*152*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_SEED_CBC_SHA /*153*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_SEED_CBC_SHA /*154*/:
                                            case CipherSuite.TLS_DH_anon_WITH_SEED_CBC_SHA /*155*/:
                                                return 14;
                                            case CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256 /*156*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 /*158*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256 /*160*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 /*162*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_GCM_SHA256 /*164*/:
                                            case CipherSuite.TLS_DH_anon_WITH_AES_128_GCM_SHA256 /*166*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_128_GCM_SHA256 /*168*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_GCM_SHA256 /*170*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_GCM_SHA256 /*172*/:
                                                break;
                                            case CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384 /*157*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 /*159*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_AES_256_GCM_SHA384 /*161*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 /*163*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_AES_256_GCM_SHA384 /*165*/:
                                            case CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384 /*167*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_256_GCM_SHA384 /*169*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384 /*171*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384 /*173*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_NULL_SHA256 /*176*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256 /*180*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA256 /*184*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_NULL_SHA384 /*177*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA384 /*181*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA384 /*185*/:
                                                break;
                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*186*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*187*/:
                                            case 188:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*189*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*190*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256 /*191*/:
                                                break;
                                            default:
                                                switch (i) {
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_NULL_SHA /*49153*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_NULL_SHA /*49158*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_NULL_SHA /*49163*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_NULL_SHA /*49168*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_NULL_SHA /*49173*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA /*49209*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_RC4_128_SHA /*49154*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA /*49159*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_RC4_128_SHA /*49164*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA /*49169*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_RC4_128_SHA /*49174*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_RC4_128_SHA /*49203*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA /*49155*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA /*49160*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA /*49165*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA /*49170*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA /*49175*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA /*49178*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA /*49179*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA /*49180*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA /*49204*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA /*49156*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA /*49161*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA /*49166*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA /*49171*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_AES_128_CBC_SHA /*49176*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_AES_128_CBC_SHA /*49181*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA /*49182*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA /*49183*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 /*49187*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256 /*49189*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 /*49191*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256 /*49193*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA /*49205*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256 /*49207*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA /*49157*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA /*49162*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA /*49167*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA /*49172*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_AES_256_CBC_SHA /*49177*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_AES_256_CBC_SHA /*49184*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA /*49185*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA /*49186*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 /*49188*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384 /*49190*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 /*49192*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384 /*49194*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA /*49206*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384 /*49208*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 /*49195*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256 /*49197*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 /*49199*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256 /*49201*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 /*49196*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384 /*49198*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 /*49200*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384 /*49202*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA256 /*49210*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA384 /*49211*/:
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49266*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49268*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49270*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49272*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49300*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49302*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49304*/:
                                                            case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49306*/:
                                                                break;
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49267*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49269*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49271*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49273*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49301*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49303*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49305*/:
                                                            case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49307*/:
                                                                break;
                                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49274*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49276*/:
                                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49278*/:
                                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49280*/:
                                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49282*/:
                                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256 /*49284*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49286*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49288*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49290*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49292*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49294*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49296*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49298*/:
                                                                return 19;
                                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49275*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49277*/:
                                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49279*/:
                                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49281*/:
                                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49283*/:
                                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384 /*49285*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49287*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49289*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49291*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49293*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49295*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49297*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49299*/:
                                                                return 20;
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM /*49308*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM /*49310*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM /*49316*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM /*49318*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM /*49324*/:
                                                                return 15;
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM /*49309*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM /*49311*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM /*49317*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM /*49319*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM /*49325*/:
                                                                return 17;
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM_8 /*49312*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM_8 /*49314*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM_8 /*49320*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8 /*49322*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8 /*49326*/:
                                                                return 16;
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM_8 /*49313*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM_8 /*49315*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM_8 /*49321*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8 /*49323*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8 /*49327*/:
                                                                return 18;
                                                            default:
                                                                switch (i) {
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52392*/:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256 /*52393*/:
                                                                    case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52394*/:
                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52395*/:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52396*/:
                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52397*/:
                                                                    case CipherSuite.DRAFT_TLS_RSA_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52398*/:
                                                                        return 21;
                                                                    default:
                                                                        switch (i) {
                                                                            case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB /*65280*/:
                                                                            case 65282:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_128_OCB /*65284*/:
                                                                                break;
                                                                            case 65281:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_AES_256_OCB /*65283*/:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_256_OCB /*65285*/:
                                                                                break;
                                                                            default:
                                                                                switch (i) {
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_128_OCB /*65296*/:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_128_OCB /*65298*/:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_128_OCB /*65300*/:
                                                                                        break;
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_256_OCB /*65297*/:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_256_OCB /*65299*/:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_256_OCB /*65301*/:
                                                                                        break;
                                                                                    default:
                                                                                        switch (i) {
                                                                                            case 10:
                                                                                            case 13:
                                                                                            case 16:
                                                                                            case 19:
                                                                                            case 22:
                                                                                            case 27:
                                                                                                break;
                                                                                            case 24:
                                                                                                break;
                                                                                            default:
                                                                                                throw new TlsFatalAlert((short) 80);
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
        }
        return 0;
    }

    public static byte[] getExtensionData(Hashtable hashtable, Integer num) {
        return hashtable == null ? null : (byte[]) hashtable.get(num);
    }

    public static short getHashAlgorithmForPRFAlgorithm(int i) {
        switch (i) {
            case 0:
                throw new IllegalArgumentException("legacy PRF not a valid algorithm");
            case 1:
                return (short) 4;
            case 2:
                return (short) 5;
            default:
                throw new IllegalArgumentException("unknown PRFAlgorithm");
        }
    }

    /* JADX WARNING: Missing block: B:14:0x002b, code skipped:
            return 24;
     */
    /* JADX WARNING: Missing block: B:24:0x003a, code skipped:
            return 19;
     */
    /* JADX WARNING: Missing block: B:26:0x003d, code skipped:
            return 18;
     */
    /* JADX WARNING: Missing block: B:28:0x0040, code skipped:
            return 17;
     */
    /* JADX WARNING: Missing block: B:30:0x0043, code skipped:
            return 16;
     */
    /* JADX WARNING: Missing block: B:32:0x0046, code skipped:
            return 11;
     */
    /* JADX WARNING: Missing block: B:34:0x0048, code skipped:
            return 5;
     */
    /* JADX WARNING: Missing block: B:36:0x004a, code skipped:
            return 3;
     */
    /* JADX WARNING: Missing block: B:38:0x004d, code skipped:
            return 9;
     */
    /* JADX WARNING: Missing block: B:40:0x004f, code skipped:
            return 7;
     */
    /* JADX WARNING: Missing block: B:42:0x0052, code skipped:
            return 15;
     */
    /* JADX WARNING: Missing block: B:44:0x0055, code skipped:
            return 14;
     */
    /* JADX WARNING: Missing block: B:46:0x0058, code skipped:
            return 13;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int getKeyExchangeAlgorithm(int i) throws IOException {
        switch (i) {
            case 1:
            case 2:
                break;
            default:
                switch (i) {
                    case 4:
                    case 5:
                        break;
                    default:
                        switch (i) {
                            case 44:
                                break;
                            case 45:
                                break;
                            case 46:
                                break;
                            case 47:
                            case 53:
                            case 59:
                            case 60:
                            case 61:
                            case 65:
                                break;
                            case 48:
                            case 54:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_CBC_SHA256 /*62*/:
                            case 66:
                                break;
                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_CBC_SHA /*49*/:
                            case 55:
                            case 63:
                            case 67:
                                break;
                            case 50:
                            case 56:
                            case 64:
                            case 68:
                                break;
                            case 51:
                            case 57:
                            case 69:
                                break;
                            case 52:
                            case 58:
                            case 70:
                                break;
                            default:
                                switch (i) {
                                    case 103:
                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 /*107*/:
                                        break;
                                    case 104:
                                        break;
                                    case CipherSuite.TLS_DH_RSA_WITH_AES_256_CBC_SHA256 /*105*/:
                                        break;
                                    case CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 /*106*/:
                                        break;
                                    case CipherSuite.TLS_DH_anon_WITH_AES_128_CBC_SHA256 /*108*/:
                                    case CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256 /*109*/:
                                        break;
                                    default:
                                        switch (i) {
                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_CBC_SHA /*132*/:
                                            case CipherSuite.TLS_RSA_WITH_SEED_CBC_SHA /*150*/:
                                            case CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256 /*156*/:
                                            case CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384 /*157*/:
                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*186*/:
                                            case 192:
                                                break;
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA /*133*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA /*151*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_GCM_SHA256 /*164*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_AES_256_GCM_SHA384 /*165*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*187*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*193*/:
                                                break;
                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA /*134*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_SEED_CBC_SHA /*152*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256 /*160*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_AES_256_GCM_SHA384 /*161*/:
                                            case 188:
                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*194*/:
                                                break;
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA /*135*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_SEED_CBC_SHA /*153*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 /*162*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 /*163*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*189*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*195*/:
                                                break;
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA /*136*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_SEED_CBC_SHA /*154*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 /*158*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 /*159*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*190*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*196*/:
                                                break;
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA /*137*/:
                                            case CipherSuite.TLS_DH_anon_WITH_SEED_CBC_SHA /*155*/:
                                            case CipherSuite.TLS_DH_anon_WITH_AES_128_GCM_SHA256 /*166*/:
                                            case CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384 /*167*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256 /*191*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256 /*197*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_RC4_128_SHA /*138*/:
                                            case CipherSuite.TLS_PSK_WITH_3DES_EDE_CBC_SHA /*139*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA /*140*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA /*141*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_128_GCM_SHA256 /*168*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_256_GCM_SHA384 /*169*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256 /*174*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA384 /*175*/:
                                            case CipherSuite.TLS_PSK_WITH_NULL_SHA256 /*176*/:
                                            case CipherSuite.TLS_PSK_WITH_NULL_SHA384 /*177*/:
                                                break;
                                            case CipherSuite.TLS_DHE_PSK_WITH_RC4_128_SHA /*142*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_3DES_EDE_CBC_SHA /*143*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA /*144*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA /*145*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_GCM_SHA256 /*170*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384 /*171*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA256 /*178*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384 /*179*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256 /*180*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA384 /*181*/:
                                                break;
                                            case CipherSuite.TLS_RSA_PSK_WITH_RC4_128_SHA /*146*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_3DES_EDE_CBC_SHA /*147*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA /*148*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA /*149*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_GCM_SHA256 /*172*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384 /*173*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA256 /*182*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA384 /*183*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA256 /*184*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA384 /*185*/:
                                                break;
                                            default:
                                                switch (i) {
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_NULL_SHA /*49153*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_RC4_128_SHA /*49154*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA /*49155*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA /*49156*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA /*49157*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256 /*49189*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384 /*49190*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256 /*49197*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384 /*49198*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_NULL_SHA /*49158*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA /*49159*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA /*49160*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA /*49161*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA /*49162*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 /*49187*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 /*49188*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 /*49195*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 /*49196*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_NULL_SHA /*49163*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_RC4_128_SHA /*49164*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA /*49165*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA /*49166*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA /*49167*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256 /*49193*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384 /*49194*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256 /*49201*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384 /*49202*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_NULL_SHA /*49168*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA /*49169*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA /*49170*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA /*49171*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA /*49172*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 /*49191*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 /*49192*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 /*49199*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 /*49200*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDH_anon_WITH_NULL_SHA /*49173*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_RC4_128_SHA /*49174*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA /*49175*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_AES_128_CBC_SHA /*49176*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_AES_256_CBC_SHA /*49177*/:
                                                        return 20;
                                                    case CipherSuite.TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA /*49178*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_AES_128_CBC_SHA /*49181*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_AES_256_CBC_SHA /*49184*/:
                                                        return 21;
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA /*49179*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA /*49182*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA /*49185*/:
                                                        return 23;
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA /*49180*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA /*49183*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA /*49186*/:
                                                        return 22;
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_RC4_128_SHA /*49203*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA /*49204*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA /*49205*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA /*49206*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256 /*49207*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384 /*49208*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA /*49209*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA256 /*49210*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA384 /*49211*/:
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49266*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49267*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49286*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49287*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM /*49324*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM /*49325*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8 /*49326*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8 /*49327*/:
                                                                break;
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49268*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49269*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49288*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49289*/:
                                                                break;
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49270*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49271*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49290*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49291*/:
                                                                break;
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49272*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49273*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49292*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49293*/:
                                                                break;
                                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49274*/:
                                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49275*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM /*49308*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM /*49309*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM_8 /*49312*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM_8 /*49313*/:
                                                                break;
                                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49276*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49277*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM /*49310*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM /*49311*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM_8 /*49314*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM_8 /*49315*/:
                                                                break;
                                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49278*/:
                                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49279*/:
                                                                break;
                                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49280*/:
                                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49281*/:
                                                                break;
                                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49282*/:
                                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49283*/:
                                                                break;
                                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256 /*49284*/:
                                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384 /*49285*/:
                                                                break;
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49294*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49295*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49300*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49301*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM /*49316*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM /*49317*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM_8 /*49320*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM_8 /*49321*/:
                                                                break;
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49296*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49297*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49302*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49303*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM /*49318*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM /*49319*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8 /*49322*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8 /*49323*/:
                                                                break;
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49298*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49299*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49304*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49305*/:
                                                                break;
                                                            case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49306*/:
                                                            case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49307*/:
                                                                break;
                                                            default:
                                                                switch (i) {
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52392*/:
                                                                        break;
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256 /*52393*/:
                                                                        break;
                                                                    case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52394*/:
                                                                        break;
                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52395*/:
                                                                        break;
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52396*/:
                                                                        break;
                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52397*/:
                                                                        break;
                                                                    case CipherSuite.DRAFT_TLS_RSA_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52398*/:
                                                                        break;
                                                                    default:
                                                                        switch (i) {
                                                                            case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB /*65280*/:
                                                                            case 65281:
                                                                                break;
                                                                            case 65282:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_AES_256_OCB /*65283*/:
                                                                                break;
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_128_OCB /*65284*/:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_256_OCB /*65285*/:
                                                                                break;
                                                                            default:
                                                                                switch (i) {
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_128_OCB /*65296*/:
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_256_OCB /*65297*/:
                                                                                        break;
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_128_OCB /*65298*/:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_256_OCB /*65299*/:
                                                                                        break;
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_128_OCB /*65300*/:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_256_OCB /*65301*/:
                                                                                        break;
                                                                                    default:
                                                                                        switch (i) {
                                                                                            case 10:
                                                                                                break;
                                                                                            case 13:
                                                                                                break;
                                                                                            case 16:
                                                                                                break;
                                                                                            case 19:
                                                                                                break;
                                                                                            case 22:
                                                                                                break;
                                                                                            case 24:
                                                                                            case 27:
                                                                                                break;
                                                                                            default:
                                                                                                throw new TlsFatalAlert((short) 80);
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
        }
        return 1;
    }

    /* JADX WARNING: Missing block: B:14:0x002a, code skipped:
            return 4;
     */
    /* JADX WARNING: Missing block: B:16:0x002c, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:18:0x002e, code skipped:
            return 3;
     */
    /* JADX WARNING: Missing block: B:20:0x0030, code skipped:
            return 2;
     */
    /* JADX WARNING: Missing block: B:22:0x0032, code skipped:
            return 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int getMACAlgorithm(int i) throws IOException {
        switch (i) {
            case 1:
                break;
            case 2:
                break;
            default:
                switch (i) {
                    case 4:
                        break;
                    case 5:
                        break;
                    default:
                        switch (i) {
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                            case 48:
                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_CBC_SHA /*49*/:
                            case 50:
                            case 51:
                            case 52:
                            case 53:
                            case 54:
                            case 55:
                            case 56:
                            case 57:
                            case 58:
                            case 65:
                            case 66:
                            case 67:
                            case 68:
                            case 69:
                            case 70:
                                break;
                            case 59:
                            case 60:
                            case 61:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_CBC_SHA256 /*62*/:
                            case 63:
                            case 64:
                                break;
                            default:
                                switch (i) {
                                    case 103:
                                    case 104:
                                    case CipherSuite.TLS_DH_RSA_WITH_AES_256_CBC_SHA256 /*105*/:
                                    case CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 /*106*/:
                                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 /*107*/:
                                    case CipherSuite.TLS_DH_anon_WITH_AES_128_CBC_SHA256 /*108*/:
                                    case CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256 /*109*/:
                                        break;
                                    default:
                                        switch (i) {
                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_CBC_SHA /*132*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA /*133*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA /*134*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA /*135*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA /*136*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA /*137*/:
                                            case CipherSuite.TLS_PSK_WITH_RC4_128_SHA /*138*/:
                                            case CipherSuite.TLS_PSK_WITH_3DES_EDE_CBC_SHA /*139*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA /*140*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA /*141*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_RC4_128_SHA /*142*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_3DES_EDE_CBC_SHA /*143*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA /*144*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA /*145*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_RC4_128_SHA /*146*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_3DES_EDE_CBC_SHA /*147*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA /*148*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA /*149*/:
                                            case CipherSuite.TLS_RSA_WITH_SEED_CBC_SHA /*150*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_SEED_CBC_SHA /*151*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_SEED_CBC_SHA /*152*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_SEED_CBC_SHA /*153*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_SEED_CBC_SHA /*154*/:
                                            case CipherSuite.TLS_DH_anon_WITH_SEED_CBC_SHA /*155*/:
                                                break;
                                            case CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256 /*156*/:
                                            case CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384 /*157*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 /*158*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 /*159*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256 /*160*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_AES_256_GCM_SHA384 /*161*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 /*162*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 /*163*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_GCM_SHA256 /*164*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_AES_256_GCM_SHA384 /*165*/:
                                            case CipherSuite.TLS_DH_anon_WITH_AES_128_GCM_SHA256 /*166*/:
                                            case CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384 /*167*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_128_GCM_SHA256 /*168*/:
                                            case CipherSuite.TLS_PSK_WITH_AES_256_GCM_SHA384 /*169*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_GCM_SHA256 /*170*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384 /*171*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_GCM_SHA256 /*172*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384 /*173*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256 /*174*/:
                                            case CipherSuite.TLS_PSK_WITH_NULL_SHA256 /*176*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CBC_SHA256 /*178*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA256 /*180*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_CBC_SHA256 /*182*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA256 /*184*/:
                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*186*/:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*187*/:
                                            case 188:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*189*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*190*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256 /*191*/:
                                            case 192:
                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*193*/:
                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*194*/:
                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*195*/:
                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*196*/:
                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256 /*197*/:
                                                break;
                                            case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA384 /*175*/:
                                            case CipherSuite.TLS_PSK_WITH_NULL_SHA384 /*177*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384 /*179*/:
                                            case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA384 /*181*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA384 /*183*/:
                                            case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA384 /*185*/:
                                                break;
                                            default:
                                                switch (i) {
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_NULL_SHA /*49153*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_RC4_128_SHA /*49154*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA /*49155*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA /*49156*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA /*49157*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_NULL_SHA /*49158*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA /*49159*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA /*49160*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA /*49161*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA /*49162*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_NULL_SHA /*49163*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_RC4_128_SHA /*49164*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA /*49165*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA /*49166*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA /*49167*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_NULL_SHA /*49168*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA /*49169*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA /*49170*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA /*49171*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA /*49172*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_NULL_SHA /*49173*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_RC4_128_SHA /*49174*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA /*49175*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_AES_128_CBC_SHA /*49176*/:
                                                    case CipherSuite.TLS_ECDH_anon_WITH_AES_256_CBC_SHA /*49177*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA /*49178*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA /*49179*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA /*49180*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_AES_128_CBC_SHA /*49181*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA /*49182*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA /*49183*/:
                                                    case CipherSuite.TLS_SRP_SHA_WITH_AES_256_CBC_SHA /*49184*/:
                                                    case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA /*49185*/:
                                                    case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA /*49186*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_RC4_128_SHA /*49203*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA /*49204*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA /*49205*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA /*49206*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA /*49209*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 /*49187*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256 /*49189*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 /*49191*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256 /*49193*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256 /*49207*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA256 /*49210*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 /*49188*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384 /*49190*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 /*49192*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384 /*49194*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384 /*49208*/:
                                                    case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA384 /*49211*/:
                                                        break;
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 /*49195*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 /*49196*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256 /*49197*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384 /*49198*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 /*49199*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 /*49200*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256 /*49201*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384 /*49202*/:
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49266*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49268*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49270*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49272*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49300*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49302*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49304*/:
                                                            case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_128_CBC_SHA256 /*49306*/:
                                                                break;
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49267*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49269*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49271*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49273*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49301*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49303*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49305*/:
                                                            case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_256_CBC_SHA384 /*49307*/:
                                                                break;
                                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49274*/:
                                                            case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49275*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49276*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49277*/:
                                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49278*/:
                                                            case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49279*/:
                                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49280*/:
                                                            case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49281*/:
                                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49282*/:
                                                            case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49283*/:
                                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256 /*49284*/:
                                                            case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384 /*49285*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49286*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49287*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49288*/:
                                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49289*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49290*/:
                                                            case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49291*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49292*/:
                                                            case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49293*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49294*/:
                                                            case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49295*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49296*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49297*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49298*/:
                                                            case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49299*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM /*49308*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM /*49309*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM /*49310*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM /*49311*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM_8 /*49312*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM_8 /*49313*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM_8 /*49314*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM_8 /*49315*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM /*49316*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM /*49317*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM /*49318*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM /*49319*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM_8 /*49320*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM_8 /*49321*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8 /*49322*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8 /*49323*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM /*49324*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM /*49325*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8 /*49326*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8 /*49327*/:
                                                                break;
                                                            default:
                                                                switch (i) {
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52392*/:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256 /*52393*/:
                                                                    case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52394*/:
                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52395*/:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52396*/:
                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52397*/:
                                                                    case CipherSuite.DRAFT_TLS_RSA_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52398*/:
                                                                        break;
                                                                    default:
                                                                        switch (i) {
                                                                            case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB /*65280*/:
                                                                            case 65281:
                                                                            case 65282:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_AES_256_OCB /*65283*/:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_128_OCB /*65284*/:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_256_OCB /*65285*/:
                                                                                break;
                                                                            default:
                                                                                switch (i) {
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_128_OCB /*65296*/:
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_256_OCB /*65297*/:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_128_OCB /*65298*/:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_256_OCB /*65299*/:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_128_OCB /*65300*/:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_256_OCB /*65301*/:
                                                                                        break;
                                                                                    default:
                                                                                        switch (i) {
                                                                                            case 10:
                                                                                            case 13:
                                                                                            case 16:
                                                                                            case 19:
                                                                                            case 22:
                                                                                            case 27:
                                                                                                break;
                                                                                            case 24:
                                                                                                break;
                                                                                            default:
                                                                                                throw new TlsFatalAlert((short) 80);
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
        }
    }

    public static ProtocolVersion getMinimumVersion(int i) {
        switch (i) {
            case 59:
            case 60:
            case 61:
            case CipherSuite.TLS_DH_DSS_WITH_AES_128_CBC_SHA256 /*62*/:
            case 63:
            case 64:
                break;
            default:
                switch (i) {
                    case 103:
                    case 104:
                    case CipherSuite.TLS_DH_RSA_WITH_AES_256_CBC_SHA256 /*105*/:
                    case CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 /*106*/:
                    case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256 /*107*/:
                    case CipherSuite.TLS_DH_anon_WITH_AES_128_CBC_SHA256 /*108*/:
                    case CipherSuite.TLS_DH_anon_WITH_AES_256_CBC_SHA256 /*109*/:
                        break;
                    default:
                        switch (i) {
                            case CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256 /*156*/:
                            case CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384 /*157*/:
                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256 /*158*/:
                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384 /*159*/:
                            case CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256 /*160*/:
                            case CipherSuite.TLS_DH_RSA_WITH_AES_256_GCM_SHA384 /*161*/:
                            case CipherSuite.TLS_DHE_DSS_WITH_AES_128_GCM_SHA256 /*162*/:
                            case CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 /*163*/:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_128_GCM_SHA256 /*164*/:
                            case CipherSuite.TLS_DH_DSS_WITH_AES_256_GCM_SHA384 /*165*/:
                            case CipherSuite.TLS_DH_anon_WITH_AES_128_GCM_SHA256 /*166*/:
                            case CipherSuite.TLS_DH_anon_WITH_AES_256_GCM_SHA384 /*167*/:
                            case CipherSuite.TLS_PSK_WITH_AES_128_GCM_SHA256 /*168*/:
                            case CipherSuite.TLS_PSK_WITH_AES_256_GCM_SHA384 /*169*/:
                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_GCM_SHA256 /*170*/:
                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384 /*171*/:
                            case CipherSuite.TLS_RSA_PSK_WITH_AES_128_GCM_SHA256 /*172*/:
                            case CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384 /*173*/:
                                break;
                            default:
                                switch (i) {
                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*186*/:
                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*187*/:
                                    case 188:
                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256 /*189*/:
                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*190*/:
                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256 /*191*/:
                                    case 192:
                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*193*/:
                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*194*/:
                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256 /*195*/:
                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256 /*196*/:
                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256 /*197*/:
                                        break;
                                    default:
                                        switch (i) {
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 /*49187*/:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 /*49188*/:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256 /*49189*/:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384 /*49190*/:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 /*49191*/:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 /*49192*/:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256 /*49193*/:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384 /*49194*/:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 /*49195*/:
                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 /*49196*/:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256 /*49197*/:
                                            case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384 /*49198*/:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 /*49199*/:
                                            case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 /*49200*/:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256 /*49201*/:
                                            case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384 /*49202*/:
                                                break;
                                            default:
                                                switch (i) {
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49266*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49267*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256 /*49268*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384 /*49269*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49270*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49271*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256 /*49272*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384 /*49273*/:
                                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49274*/:
                                                    case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49275*/:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49276*/:
                                                    case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49277*/:
                                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49278*/:
                                                    case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49279*/:
                                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49280*/:
                                                    case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49281*/:
                                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256 /*49282*/:
                                                    case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384 /*49283*/:
                                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256 /*49284*/:
                                                    case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384 /*49285*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49286*/:
                                                    case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49287*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256 /*49288*/:
                                                    case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384 /*49289*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49290*/:
                                                    case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49291*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256 /*49292*/:
                                                    case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384 /*49293*/:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49294*/:
                                                    case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49295*/:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49296*/:
                                                    case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49297*/:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256 /*49298*/:
                                                    case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384 /*49299*/:
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM /*49308*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM /*49309*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM /*49310*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM /*49311*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_128_CCM_8 /*49312*/:
                                                            case CipherSuite.TLS_RSA_WITH_AES_256_CCM_8 /*49313*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM_8 /*49314*/:
                                                            case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM_8 /*49315*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM /*49316*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM /*49317*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM /*49318*/:
                                                            case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM /*49319*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_128_CCM_8 /*49320*/:
                                                            case CipherSuite.TLS_PSK_WITH_AES_256_CCM_8 /*49321*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8 /*49322*/:
                                                            case CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8 /*49323*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM /*49324*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM /*49325*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8 /*49326*/:
                                                            case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8 /*49327*/:
                                                                break;
                                                            default:
                                                                switch (i) {
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52392*/:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256 /*52393*/:
                                                                    case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256 /*52394*/:
                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52395*/:
                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52396*/:
                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52397*/:
                                                                    case CipherSuite.DRAFT_TLS_RSA_PSK_WITH_CHACHA20_POLY1305_SHA256 /*52398*/:
                                                                        break;
                                                                    default:
                                                                        switch (i) {
                                                                            case CipherSuite.DRAFT_TLS_DHE_RSA_WITH_AES_128_OCB /*65280*/:
                                                                            case 65281:
                                                                            case 65282:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_RSA_WITH_AES_256_OCB /*65283*/:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_128_OCB /*65284*/:
                                                                            case CipherSuite.DRAFT_TLS_ECDHE_ECDSA_WITH_AES_256_OCB /*65285*/:
                                                                                break;
                                                                            default:
                                                                                switch (i) {
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_128_OCB /*65296*/:
                                                                                    case CipherSuite.DRAFT_TLS_PSK_WITH_AES_256_OCB /*65297*/:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_128_OCB /*65298*/:
                                                                                    case CipherSuite.DRAFT_TLS_DHE_PSK_WITH_AES_256_OCB /*65299*/:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_128_OCB /*65300*/:
                                                                                    case CipherSuite.DRAFT_TLS_ECDHE_PSK_WITH_AES_256_OCB /*65301*/:
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
            case (short) 1:
                return PKCSObjectIdentifiers.md5;
            case (short) 2:
                return X509ObjectIdentifiers.id_SHA1;
            case (short) 3:
                return NISTObjectIdentifiers.id_sha224;
            case (short) 4:
                return NISTObjectIdentifiers.id_sha256;
            case (short) 5:
                return NISTObjectIdentifiers.id_sha384;
            case (short) 6:
                return NISTObjectIdentifiers.id_sha512;
            default:
                throw new IllegalArgumentException("unknown HashAlgorithm");
        }
    }

    public static Vector getSignatureAlgorithmsExtension(Hashtable hashtable) throws IOException {
        byte[] extensionData = getExtensionData(hashtable, EXT_signature_algorithms);
        return extensionData == null ? null : readSignatureAlgorithmsExtension(extensionData);
    }

    public static SignatureAndHashAlgorithm getSignatureAndHashAlgorithm(TlsContext tlsContext, TlsSignerCredentials tlsSignerCredentials) throws IOException {
        if (!isTLSv12(tlsContext)) {
            return null;
        }
        SignatureAndHashAlgorithm signatureAndHashAlgorithm = tlsSignerCredentials.getSignatureAndHashAlgorithm();
        if (signatureAndHashAlgorithm != null) {
            return signatureAndHashAlgorithm;
        }
        throw new TlsFatalAlert((short) 80);
    }

    public static Vector getUsableSignatureAlgorithms(Vector vector) {
        if (vector == null) {
            return getAllSignatureAlgorithms();
        }
        Vector vector2 = new Vector(4);
        int i = 0;
        vector2.addElement(Shorts.valueOf((short) 0));
        while (i < vector.size()) {
            Short valueOf = Shorts.valueOf(((SignatureAndHashAlgorithm) vector.elementAt(i)).getSignature());
            if (!vector2.contains(valueOf)) {
                vector2.addElement(valueOf);
            }
            i++;
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
        if (s != (short) 64) {
            switch (s) {
                case (short) 1:
                case (short) 2:
                    break;
                default:
                    return false;
            }
        }
        return true;
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
        try {
            short s;
            Object valueOf;
            switch (getKeyExchangeAlgorithm(i)) {
                case 3:
                case 4:
                case 22:
                    s = (short) 2;
                    break;
                case 5:
                case 6:
                case 19:
                case 23:
                    valueOf = Shorts.valueOf((short) 1);
                    break;
                case 11:
                case 12:
                case 20:
                    s = (short) 0;
                    break;
                case 17:
                    s = (short) 3;
                    break;
                default:
                    return true;
            }
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
            throw new TlsFatalAlert((short) 50);
        }
        readUint16 /= 2;
        Vector vector = new Vector(readUint16);
        int i = 0;
        while (i < readUint16) {
            SignatureAndHashAlgorithm parse = SignatureAndHashAlgorithm.parse(inputStream);
            if (z || parse.getSignature() != (short) 0) {
                vector.addElement(parse);
                i++;
            } else {
                throw new TlsFatalAlert((short) 47);
            }
        }
        return vector;
    }

    public static ASN1Primitive readASN1Object(byte[] bArr) throws IOException {
        ASN1InputStream aSN1InputStream = new ASN1InputStream(bArr);
        ASN1Primitive readObject = aSN1InputStream.readObject();
        if (readObject == null) {
            throw new TlsFatalAlert((short) 50);
        } else if (aSN1InputStream.readObject() == null) {
            return readObject;
        } else {
            throw new TlsFatalAlert((short) 50);
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
        throw new TlsFatalAlert((short) 50);
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
            return read3 | ((read << 16) | (read2 << 8));
        }
        throw new EOFException();
    }

    public static int readUint24(byte[] bArr, int i) {
        i++;
        return (bArr[i + 1] & 255) | (((bArr[i] & 255) << 16) | ((bArr[i] & 255) << 8));
    }

    public static long readUint32(InputStream inputStream) throws IOException {
        int read = inputStream.read();
        int read2 = inputStream.read();
        int read3 = inputStream.read();
        int read4 = inputStream.read();
        if (read4 >= 0) {
            return ((long) (read4 | (((read << 24) | (read2 << 16)) | (read3 << 8)))) & BodyPartID.bodyIdMax;
        }
        throw new EOFException();
    }

    public static long readUint32(byte[] bArr, int i) {
        i++;
        i++;
        return ((long) ((bArr[i + 1] & 255) | ((((bArr[i] & 255) << 24) | ((bArr[i] & 255) << 16)) | ((bArr[i] & 255) << 8)))) & BodyPartID.bodyIdMax;
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
                if (!HashAlgorithm.isPrivate(hash)) {
                    tlsHandshakeHash.trackHashAlgorithm(hash);
                }
            }
        }
    }

    static void validateKeyUsage(Certificate certificate, int i) throws IOException {
        Extensions extensions = certificate.getTBSCertificate().getExtensions();
        if (extensions != null) {
            KeyUsage fromExtensions = KeyUsage.fromExtensions(extensions);
            if (fromExtensions != null && ((fromExtensions.getBytes()[0] & 255) & i) != i) {
                throw new TlsFatalAlert((short) 46);
            }
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
            if (signatureAndHashAlgorithm.getSignature() != (short) 0) {
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
            throw new TlsFatalAlert((short) 47);
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
        for (int writeUint16 : iArr) {
            writeUint16(writeUint16, outputStream);
        }
    }

    public static void writeUint16Array(int[] iArr, byte[] bArr, int i) throws IOException {
        for (int writeUint16 : iArr) {
            writeUint16(writeUint16, bArr, i);
            i += 2;
        }
    }

    public static void writeUint16ArrayWithUint16Length(int[] iArr, OutputStream outputStream) throws IOException {
        int length = 2 * iArr.length;
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
        for (short writeUint8 : sArr) {
            writeUint8(writeUint8, outputStream);
        }
    }

    public static void writeUint8Array(short[] sArr, byte[] bArr, int i) throws IOException {
        for (short writeUint8 : sArr) {
            writeUint8(writeUint8, bArr, i);
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
