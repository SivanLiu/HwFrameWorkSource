package huawei.android.security.securityprofile;

import android.os.SystemProperties;
import android.util.Pair;
import android.util.Slog;
import android.util.jar.StrictJarFile;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import libcore.io.Streams;

public class DigestMatcher {
    private static final int APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 1896449818;
    private static final long APK_SIG_BLOCK_MAGIC_HI = 3617552046287187010L;
    private static final long APK_SIG_BLOCK_MAGIC_LO = 2334950737559900225L;
    private static final int APK_SIG_BLOCK_MIN_SIZE = 32;
    public static final boolean CALCULATE_APKDIGEST = "true".equalsIgnoreCase(SystemProperties.get("ro.config.iseapp_calculate_apkdigest", "true"));
    public static final int CONTENT_DIGEST_CHUNKED_SHA256 = 1;
    public static final int CONTENT_DIGEST_CHUNKED_SHA512 = 2;
    private static final int SIGNATURE_DSA_WITH_SHA256 = 769;
    private static final int SIGNATURE_ECDSA_WITH_SHA256 = 513;
    private static final int SIGNATURE_ECDSA_WITH_SHA512 = 514;
    private static final int SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 259;
    private static final int SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA512 = 260;
    private static final int SIGNATURE_RSA_PSS_WITH_SHA256 = 257;
    private static final int SIGNATURE_RSA_PSS_WITH_SHA512 = 258;
    private static final String TAG = "SecurityProfileDigestMatcher";

    public static class SignatureNotFoundException extends Exception {
        private static final long serialVersionUID = 1;

        public SignatureNotFoundException(String message) {
            super(message);
        }

        public SignatureNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static byte[] calculateManifestDigest(String apkPath, String digestAlgorithm) {
        StrictJarFile jarFile = null;
        try {
            jarFile = new StrictJarFile(apkPath, false, false);
            ZipEntry ze = jarFile.findEntry("META-INF/MANIFEST.MF");
            if (ze != null) {
                byte[] b = Streams.readFully(jarFile.getInputStream(ze));
                MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
                md.update(b);
                byte[] digest = md.digest();
                try {
                    jarFile.close();
                } catch (IOException e) {
                    Slog.w(TAG, "close jar file counter exception!");
                }
                return digest;
            }
            Slog.d(TAG, "ZipEntry is null");
            try {
                jarFile.close();
            } catch (IOException e2) {
                Slog.w(TAG, "close jar file counter exception!");
            }
            return null;
        } catch (NoSuchAlgorithmException e3) {
            Slog.e(TAG, e3.getMessage());
            if (jarFile != null) {
                jarFile.close();
            }
        } catch (IOException e4) {
            Slog.e(TAG, e4.getMessage());
            if (jarFile != null) {
                jarFile.close();
            }
        } catch (Throwable th) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e5) {
                    Slog.w(TAG, "close jar file counter exception!");
                }
            }
        }
    }

    public static boolean packageMatchesDigest(String apkPath, ApkDigest apkDigest) {
        byte[] calculatedDigest = apkDigest.apkSignatureScheme.equals("v1_manifest") ? calculateManifestDigest(apkPath, apkDigest.digestAlgorithm) : findDigest(apkPath, apkDigest.digestAlgorithm);
        if (calculatedDigest == null) {
            return false;
        }
        return apkDigest.base64Digest.equals(Base64.getEncoder().encodeToString(calculatedDigest));
    }

    public static byte[] findDigest(String apkFile, String digestAlgorithm) {
        Throwable th;
        Throwable th2;
        try {
            RandomAccessFile apk = new RandomAccessFile(apkFile, "r");
            try {
                byte[] findDigest = findDigest(apk, digestAlgorithm);
                $closeResource(null, apk);
                return findDigest;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            $closeResource(th22, apk);
            throw th;
        } catch (SignatureNotFoundException e) {
            return null;
        } catch (IOException e2) {
            return null;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private static byte[] findDigest(RandomAccessFile apk, String digestAlgorithm) throws IOException, SignatureNotFoundException {
        return findDigest(findSignatureBlock(apk), digestAlgorithm);
    }

    private static ByteBuffer findSignatureBlock(RandomAccessFile apk) throws IOException, SignatureNotFoundException {
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = getEocd(apk);
        ByteBuffer eocd = eocdAndOffsetInFile.first;
        long eocdOffset = ((Long) eocdAndOffsetInFile.second).longValue();
        if (!ZipUtils.isZip64EndOfCentralDirectoryLocatorPresent(apk, eocdOffset)) {
            return findApkSignatureSchemeV2Block(findApkSigningBlock(apk, getCentralDirOffset(eocd, eocdOffset)).first);
        }
        throw new SignatureNotFoundException("ZIP64 APK not supported");
    }

    private static byte[] findDigest(ByteBuffer signatureBlock, String jcaDigestAlgorithm) throws SecurityException {
        int signerCount = 0;
        try {
            ByteBuffer signers = getLengthPrefixedSlice(signatureBlock);
            while (signers.hasRemaining()) {
                signerCount++;
                try {
                    byte[] contentDigest = findMatchingContentDigest(getLengthPrefixedSlice(signers), jcaDigestAlgorithm);
                    if (contentDigest != null) {
                        return contentDigest;
                    }
                } catch (IOException | SecurityException | BufferUnderflowException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parse/verify signer #");
                    stringBuilder.append(signerCount);
                    stringBuilder.append(" block");
                    throw new SecurityException(stringBuilder.toString(), e);
                }
            }
            return null;
        } catch (IOException e2) {
            throw new SecurityException("Failed to read list of signers", e2);
        }
    }

    private static byte[] findMatchingContentDigest(ByteBuffer signerBlock, String jcaDigestAlgorithm) throws SecurityException, IOException {
        ByteBuffer digests = getLengthPrefixedSlice(getLengthPrefixedSlice(signerBlock));
        while (digests.hasRemaining()) {
            try {
                ByteBuffer digest = getLengthPrefixedSlice(digests);
                if (digest.remaining() < 8) {
                    throw new IOException("Record too short");
                } else if (getContentDigestAlgorithmJcaDigestAlgorithm(getSignatureAlgorithmContentDigestAlgorithm(digest.getInt())).equals(jcaDigestAlgorithm)) {
                    return readLengthPrefixedByteArray(digest);
                }
            } catch (IOException | BufferUnderflowException e) {
                throw new IOException("Failed to parse digest record ", e);
            }
        }
        return null;
    }

    private static Pair<ByteBuffer, Long> getEocd(RandomAccessFile apk) throws IOException, SignatureNotFoundException {
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = ZipUtils.findZipEndOfCentralDirectoryRecord(apk);
        if (eocdAndOffsetInFile != null) {
            return eocdAndOffsetInFile;
        }
        throw new SignatureNotFoundException("Not an APK file: ZIP End of Central Directory record not found");
    }

    private static long getCentralDirOffset(ByteBuffer eocd, long eocdOffset) throws SignatureNotFoundException {
        long centralDirOffset = ZipUtils.getZipEocdCentralDirectoryOffset(eocd);
        if (centralDirOffset > eocdOffset) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ZIP Central Directory offset out of range: ");
            stringBuilder.append(centralDirOffset);
            stringBuilder.append(". ZIP End of Central Directory offset: ");
            stringBuilder.append(eocdOffset);
            throw new SignatureNotFoundException(stringBuilder.toString());
        } else if (centralDirOffset + ZipUtils.getZipEocdCentralDirectorySizeBytes(eocd) == eocdOffset) {
            return centralDirOffset;
        } else {
            throw new SignatureNotFoundException("ZIP Central Directory is not immediately followed by End of Central Directory");
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0026, code skipped:
            return 2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getSignatureAlgorithmContentDigestAlgorithm(int sigAlgorithm) {
        if (sigAlgorithm != SIGNATURE_DSA_WITH_SHA256) {
            switch (sigAlgorithm) {
                case SIGNATURE_RSA_PSS_WITH_SHA256 /*257*/:
                case SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 /*259*/:
                    break;
                case SIGNATURE_RSA_PSS_WITH_SHA512 /*258*/:
                case SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA512 /*260*/:
                    break;
                default:
                    switch (sigAlgorithm) {
                        case SIGNATURE_ECDSA_WITH_SHA256 /*513*/:
                            break;
                        case SIGNATURE_ECDSA_WITH_SHA512 /*514*/:
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown signature algorithm: 0x");
                            stringBuilder.append(Integer.toHexString(sigAlgorithm));
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
            }
        }
        return 1;
    }

    private static String getContentDigestAlgorithmJcaDigestAlgorithm(int digestAlgorithm) {
        switch (digestAlgorithm) {
            case 1:
                return "SHA-256";
            case 2:
                return "SHA-512";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown content digest algorthm: ");
                stringBuilder.append(digestAlgorithm);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static ByteBuffer sliceFromTo(ByteBuffer source, int start, int end) {
        StringBuilder stringBuilder;
        if (start < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("start: ");
            stringBuilder.append(start);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (end >= start) {
            int capacity = source.capacity();
            if (end <= source.capacity()) {
                int originalLimit = source.limit();
                int originalPosition = source.position();
                try {
                    source.position(0);
                    source.limit(end);
                    source.position(start);
                    ByteBuffer result = source.slice();
                    result.order(source.order());
                    return result;
                } finally {
                    source.position(0);
                    source.limit(originalLimit);
                    source.position(originalPosition);
                }
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("end > capacity: ");
                stringBuilder2.append(end);
                stringBuilder2.append(" > ");
                stringBuilder2.append(capacity);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("end < start: ");
            stringBuilder.append(end);
            stringBuilder.append(" < ");
            stringBuilder.append(start);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static ByteBuffer getByteBuffer(ByteBuffer source, int size) throws BufferUnderflowException {
        if (size >= 0) {
            int originalLimit = source.limit();
            int position = source.position();
            int limit = position + size;
            if (limit < position || limit > originalLimit) {
                throw new BufferUnderflowException();
            }
            source.limit(limit);
            try {
                ByteBuffer result = source.slice();
                result.order(source.order());
                source.position(limit);
                return result;
            } finally {
                source.limit(originalLimit);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("size: ");
            stringBuilder.append(size);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static ByteBuffer getLengthPrefixedSlice(ByteBuffer source) throws IOException {
        if (source.remaining() >= 4) {
            int len = source.getInt();
            if (len < 0) {
                throw new IllegalArgumentException("Negative length");
            } else if (len <= source.remaining()) {
                return getByteBuffer(source, len);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Length-prefixed field longer than remaining buffer. Field length: ");
                stringBuilder.append(len);
                stringBuilder.append(", remaining: ");
                stringBuilder.append(source.remaining());
                throw new IOException(stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Remaining buffer too short to contain length of length-prefixed field. Remaining: ");
        stringBuilder2.append(source.remaining());
        throw new IOException(stringBuilder2.toString());
    }

    private static byte[] readLengthPrefixedByteArray(ByteBuffer buf) throws IOException {
        int len = buf.getInt();
        if (len < 0) {
            throw new IOException("Negative length");
        } else if (len <= buf.remaining()) {
            byte[] result = new byte[len];
            buf.get(result);
            return result;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Underflow while reading length-prefixed value. Length: ");
            stringBuilder.append(len);
            stringBuilder.append(", available: ");
            stringBuilder.append(buf.remaining());
            throw new IOException(stringBuilder.toString());
        }
    }

    private static Pair<ByteBuffer, Long> findApkSigningBlock(RandomAccessFile apk, long centralDirOffset) throws IOException, SignatureNotFoundException {
        if (centralDirOffset >= 32) {
            ByteBuffer footer = ByteBuffer.allocate(24);
            footer.order(ByteOrder.LITTLE_ENDIAN);
            apk.seek(centralDirOffset - ((long) footer.capacity()));
            apk.readFully(footer.array(), footer.arrayOffset(), footer.capacity());
            if (footer.getLong(8) == APK_SIG_BLOCK_MAGIC_LO && footer.getLong(16) == APK_SIG_BLOCK_MAGIC_HI) {
                long apkSigBlockSizeInFooter = footer.getLong(0);
                if (apkSigBlockSizeInFooter < ((long) footer.capacity()) || apkSigBlockSizeInFooter > 2147483639) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("APK Signing Block size out of range: ");
                    stringBuilder.append(apkSigBlockSizeInFooter);
                    throw new SignatureNotFoundException(stringBuilder.toString());
                }
                int totalSize = (int) (8 + apkSigBlockSizeInFooter);
                long apkSigBlockOffset = centralDirOffset - ((long) totalSize);
                if (apkSigBlockOffset >= 0) {
                    ByteBuffer apkSigBlock = ByteBuffer.allocate(totalSize);
                    apkSigBlock.order(ByteOrder.LITTLE_ENDIAN);
                    apk.seek(apkSigBlockOffset);
                    apk.readFully(apkSigBlock.array(), apkSigBlock.arrayOffset(), apkSigBlock.capacity());
                    long apkSigBlockSizeInHeader = apkSigBlock.getLong(0);
                    if (apkSigBlockSizeInHeader == apkSigBlockSizeInFooter) {
                        return Pair.create(apkSigBlock, Long.valueOf(apkSigBlockOffset));
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("APK Signing Block sizes in header and footer do not match: ");
                    stringBuilder2.append(apkSigBlockSizeInHeader);
                    stringBuilder2.append(" vs ");
                    stringBuilder2.append(apkSigBlockSizeInFooter);
                    throw new SignatureNotFoundException(stringBuilder2.toString());
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("APK Signing Block offset out of range: ");
                stringBuilder3.append(apkSigBlockOffset);
                throw new SignatureNotFoundException(stringBuilder3.toString());
            }
            throw new SignatureNotFoundException("No APK Signing Block before ZIP Central Directory");
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("APK too small for APK Signing Block. ZIP Central Directory offset: ");
        stringBuilder4.append(centralDirOffset);
        throw new SignatureNotFoundException(stringBuilder4.toString());
    }

    private static ByteBuffer findApkSignatureSchemeV2Block(ByteBuffer apkSigningBlock) throws SignatureNotFoundException {
        checkByteOrderLittleEndian(apkSigningBlock);
        ByteBuffer pairs = sliceFromTo(apkSigningBlock, 8, apkSigningBlock.capacity() - 24);
        int entryCount = 0;
        while (pairs.hasRemaining()) {
            entryCount++;
            if (pairs.remaining() >= 8) {
                long lenLong = pairs.getLong();
                if (lenLong < 4 || lenLong > 2147483647L) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("APK Signing Block entry #");
                    stringBuilder.append(entryCount);
                    stringBuilder.append(" size out of range: ");
                    stringBuilder.append(lenLong);
                    throw new SignatureNotFoundException(stringBuilder.toString());
                }
                int len = (int) lenLong;
                int nextEntryPos = pairs.position() + len;
                if (len > pairs.remaining()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("APK Signing Block entry #");
                    stringBuilder2.append(entryCount);
                    stringBuilder2.append(" size out of range: ");
                    stringBuilder2.append(len);
                    stringBuilder2.append(", available: ");
                    stringBuilder2.append(pairs.remaining());
                    throw new SignatureNotFoundException(stringBuilder2.toString());
                } else if (pairs.getInt() == APK_SIGNATURE_SCHEME_V2_BLOCK_ID) {
                    return getByteBuffer(pairs, len - 4);
                } else {
                    pairs.position(nextEntryPos);
                }
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Insufficient data to read size of APK Signing Block entry #");
                stringBuilder3.append(entryCount);
                throw new SignatureNotFoundException(stringBuilder3.toString());
            }
        }
        throw new SignatureNotFoundException("No APK Signature Scheme v2 block in APK Signing Block");
    }

    private static void checkByteOrderLittleEndian(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }

    public static ApkDigest getApkDigest(String apkPath) {
        ApkDigest digest = calculateV2ApkDigest(apkPath);
        if (digest == null) {
            return calculateV1ApkDigest(apkPath);
        }
        return digest;
    }

    public static ApkDigest calculateV1ApkDigest(String apkPath) {
        String digestAlgorithm = "SHA-256";
        return new ApkDigest("v1_manifest", digestAlgorithm, Base64.getEncoder().encodeToString(calculateManifestDigest(apkPath, digestAlgorithm)));
    }

    public static ApkDigest calculateV2ApkDigest(String apkPath) {
        Throwable th;
        Throwable th2;
        String str;
        StringBuilder stringBuilder;
        try {
            RandomAccessFile apk = new RandomAccessFile(apkPath, "r");
            try {
                ApkDigest apkDigest = findDigestInline(findSignatureBlock(apk));
                $closeResource(null, apk);
                return apkDigest;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            $closeResource(th22, apk);
            throw th;
        } catch (SignatureNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("calculateV2ApkDigest SignatureNotFound : ");
            stringBuilder.append(e.getMessage());
            Slog.w(str, stringBuilder.toString());
            return null;
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("calculateV2ApkDigest IOException : ");
            stringBuilder.append(e2.getMessage());
            Slog.e(str, stringBuilder.toString());
            return null;
        } catch (Exception e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("calculateV2ApkDigest Exception : ");
            stringBuilder.append(e3.getMessage());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    private static ApkDigest findDigestInline(ByteBuffer signatureBlock) {
        int signerCount = 0;
        try {
            ByteBuffer signers = getLengthPrefixedSlice(signatureBlock);
            while (signers.hasRemaining()) {
                signerCount++;
                try {
                    String signatureScheme = "v2";
                    String jcaDigestAlgorithm = "";
                    ByteBuffer digests = getLengthPrefixedSlice(getLengthPrefixedSlice(getLengthPrefixedSlice(signers)));
                    while (digests.hasRemaining()) {
                        ByteBuffer digest = getLengthPrefixedSlice(digests);
                        if (digest.remaining() >= 8) {
                            jcaDigestAlgorithm = getContentDigestAlgorithmJcaDigestAlgorithm(getSignatureAlgorithmContentDigestAlgorithm(digest.getInt()));
                            byte[] digestData = readLengthPrefixedByteArray(digest);
                            if (digestData != null) {
                                return new ApkDigest(signatureScheme, jcaDigestAlgorithm, Base64.getEncoder().encodeToString(digestData));
                            }
                        } else {
                            throw new IOException("Record too short");
                        }
                    }
                } catch (IOException | SecurityException | BufferUnderflowException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parse/verify signer #");
                    stringBuilder.append(signerCount);
                    stringBuilder.append(" block");
                    throw new SecurityException(stringBuilder.toString(), e);
                }
            }
            return null;
        } catch (IOException e2) {
            throw new SecurityException("Failed to read list of signers", e2);
        }
    }
}
