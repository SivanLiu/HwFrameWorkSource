package android.util.apk;

import android.os.Trace;
import android.security.keystore.KeyProperties;
import android.util.ArrayMap;
import android.util.Pair;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;
import java.util.Map;

final class ApkSigningBlockUtils {
    private static final long APK_SIG_BLOCK_MAGIC_HI = 3617552046287187010L;
    private static final long APK_SIG_BLOCK_MAGIC_LO = 2334950737559900225L;
    private static final int APK_SIG_BLOCK_MIN_SIZE = 32;
    private static final int CHUNK_SIZE_BYTES = 1048576;
    static final int CONTENT_DIGEST_CHUNKED_SHA256 = 1;
    static final int CONTENT_DIGEST_CHUNKED_SHA512 = 2;
    static final int CONTENT_DIGEST_VERITY_CHUNKED_SHA256 = 3;
    static final int SIGNATURE_DSA_WITH_SHA256 = 769;
    static final int SIGNATURE_ECDSA_WITH_SHA256 = 513;
    static final int SIGNATURE_ECDSA_WITH_SHA512 = 514;
    static final int SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 259;
    static final int SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA512 = 260;
    static final int SIGNATURE_RSA_PSS_WITH_SHA256 = 257;
    static final int SIGNATURE_RSA_PSS_WITH_SHA512 = 258;
    static final int SIGNATURE_VERITY_DSA_WITH_SHA256 = 1061;
    static final int SIGNATURE_VERITY_ECDSA_WITH_SHA256 = 1059;
    static final int SIGNATURE_VERITY_RSA_PKCS1_V1_5_WITH_SHA256 = 1057;

    private static class MultipleDigestDataDigester implements DataDigester {
        private final MessageDigest[] mMds;

        MultipleDigestDataDigester(MessageDigest[] mds) {
            this.mMds = mds;
        }

        public void consume(ByteBuffer buffer) {
            buffer = buffer.slice();
            for (MessageDigest md : this.mMds) {
                buffer.position(0);
                md.update(buffer);
            }
        }
    }

    private ApkSigningBlockUtils() {
    }

    static SignatureInfo findSignature(RandomAccessFile apk, int blockId) throws IOException, SignatureNotFoundException {
        RandomAccessFile randomAccessFile = apk;
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = getEocd(apk);
        ByteBuffer eocd = eocdAndOffsetInFile.first;
        long eocdOffset = ((Long) eocdAndOffsetInFile.second).longValue();
        if (ZipUtils.isZip64EndOfCentralDirectoryLocatorPresent(randomAccessFile, eocdOffset)) {
            throw new SignatureNotFoundException("ZIP64 APK not supported");
        }
        long centralDirOffset = getCentralDirOffset(eocd, eocdOffset);
        Pair<ByteBuffer, Long> apkSigningBlockAndOffsetInFile = findApkSigningBlock(randomAccessFile, centralDirOffset);
        ByteBuffer apkSigningBlock = apkSigningBlockAndOffsetInFile.first;
        return new SignatureInfo(findApkSignatureSchemeBlock(apkSigningBlock, blockId), ((Long) apkSigningBlockAndOffsetInFile.second).longValue(), centralDirOffset, eocdOffset, eocd);
    }

    static void verifyIntegrity(Map<Integer, byte[]> expectedDigests, RandomAccessFile apk, SignatureInfo signatureInfo) throws SecurityException {
        if (expectedDigests.isEmpty()) {
            throw new SecurityException("No digests provided");
        }
        boolean neverVerified = true;
        Map<Integer, byte[]> expected1MbChunkDigests = new ArrayMap();
        if (expectedDigests.containsKey(Integer.valueOf(1))) {
            expected1MbChunkDigests.put(Integer.valueOf(1), (byte[]) expectedDigests.get(Integer.valueOf(1)));
        }
        if (expectedDigests.containsKey(Integer.valueOf(2))) {
            expected1MbChunkDigests.put(Integer.valueOf(2), (byte[]) expectedDigests.get(Integer.valueOf(2)));
        }
        if (!expected1MbChunkDigests.isEmpty()) {
            try {
                verifyIntegrityFor1MbChunkBasedAlgorithm(expected1MbChunkDigests, apk.getFD(), signatureInfo);
                neverVerified = false;
            } catch (IOException e) {
                throw new SecurityException("Cannot get FD", e);
            }
        }
        if (expectedDigests.containsKey(Integer.valueOf(3))) {
            verifyIntegrityForVerityBasedAlgorithm((byte[]) expectedDigests.get(Integer.valueOf(3)), apk, signatureInfo);
            neverVerified = false;
        }
        if (neverVerified) {
            throw new SecurityException("No known digest exists for integrity check");
        }
    }

    private static void verifyIntegrityFor1MbChunkBasedAlgorithm(Map<Integer, byte[]> expectedDigests, FileDescriptor apkFileDescriptor, SignatureInfo signatureInfo) throws SecurityException {
        SignatureInfo signatureInfo2 = signatureInfo;
        MemoryMappedFileDataSource beforeApkSigningBlock = new MemoryMappedFileDataSource(apkFileDescriptor, 0, signatureInfo2.apkSigningBlockOffset);
        MemoryMappedFileDataSource centralDir = new MemoryMappedFileDataSource(apkFileDescriptor, signatureInfo2.centralDirOffset, signatureInfo2.eocdOffset - signatureInfo2.centralDirOffset);
        ByteBuffer eocdBuf = signatureInfo2.eocd.duplicate();
        eocdBuf.order(ByteOrder.LITTLE_ENDIAN);
        ZipUtils.setZipEocdCentralDirectoryOffset(eocdBuf, signatureInfo2.apkSigningBlockOffset);
        ByteBufferDataSource eocd = new ByteBufferDataSource(eocdBuf);
        int[] digestAlgorithms = new int[expectedDigests.size()];
        int digestAlgorithmCount = 0;
        for (Integer digestAlgorithm : expectedDigests.keySet()) {
            digestAlgorithms[digestAlgorithmCount] = digestAlgorithm.intValue();
            digestAlgorithmCount++;
        }
        Map<Integer, byte[]> map;
        try {
            actualDigests = new DataSource[3];
            int i = 0;
            actualDigests[0] = beforeApkSigningBlock;
            actualDigests[1] = centralDir;
            actualDigests[2] = eocd;
            actualDigests = computeContentDigestsPer1MbChunk(digestAlgorithms, actualDigests);
            while (i < digestAlgorithms.length) {
                int digestAlgorithm2 = digestAlgorithms[i];
                if (MessageDigest.isEqual((byte[]) expectedDigests.get(Integer.valueOf(digestAlgorithm2)), actualDigests[i])) {
                    i++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(getContentDigestAlgorithmJcaDigestAlgorithm(digestAlgorithm2));
                    stringBuilder.append(" digest of contents did not verify");
                    throw new SecurityException(stringBuilder.toString());
                }
            }
            map = expectedDigests;
        } catch (DigestException e) {
            map = expectedDigests;
            throw new SecurityException("Failed to compute digest(s) of contents", e);
        }
    }

    /* JADX WARNING: Missing block: B:43:0x016a, code skipped:
            r25 = r3;
            r30 = r5;
            r28 = r6;
            r31 = r8;
            r33 = r12;
            r32 = r13;
            r27 = r14;
            r6 = r21;
            r15 = r15 + 1;
            r0 = r0 + 1;
            r9 = 5;
            r6 = r28;
            r2 = r37;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static byte[][] computeContentDigestsPer1MbChunk(int[] digestAlgorithms, DataSource[] contents) throws DigestException {
        int totalChunkCountLong;
        StringBuilder stringBuilder;
        int[] iArr = digestAlgorithms;
        DataSource[] dataSourceArr = contents;
        long totalChunkCountLong2 = 0;
        for (DataSource input : dataSourceArr) {
            totalChunkCountLong2 += getChunkCount(input.size());
        }
        long totalChunkCountLong3;
        if (totalChunkCountLong2 < 2097151) {
            int i;
            byte[] chunkContentPrefix;
            DataDigester digester;
            MessageDigest[] mds;
            totalChunkCountLong = (int) totalChunkCountLong2;
            byte[][] digestsOfChunks = new byte[iArr.length][];
            for (i = 0; i < iArr.length; i++) {
                byte[] concatenationOfChunkCountAndChunkDigests = new byte[(5 + (totalChunkCountLong * getContentDigestAlgorithmOutputSizeBytes(iArr[i])))];
                concatenationOfChunkCountAndChunkDigests[0] = (byte) 90;
                setUnsignedInt32LittleEndian(totalChunkCountLong, concatenationOfChunkCountAndChunkDigests, 1);
                digestsOfChunks[i] = concatenationOfChunkCountAndChunkDigests;
            }
            byte[] chunkContentPrefix2 = new byte[5];
            chunkContentPrefix2[0] = (byte) -91;
            int chunkIndex = 0;
            MessageDigest[] mds2 = new MessageDigest[iArr.length];
            i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= iArr.length) {
                    break;
                }
                String jcaAlgorithmName = getContentDigestAlgorithmJcaDigestAlgorithm(iArr[i2]);
                try {
                    mds2[i2] = MessageDigest.getInstance(jcaAlgorithmName);
                    i = i2 + 1;
                } catch (NoSuchAlgorithmException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(jcaAlgorithmName);
                    stringBuilder.append(" digest not supported");
                    throw new RuntimeException(stringBuilder.toString(), e);
                }
            }
            DataDigester digester2 = new MultipleDigestDataDigester(mds2);
            int i3 = dataSourceArr.length;
            int dataSourceIndex = 0;
            i = 0;
            while (i < i3) {
                DataSource input2 = dataSourceArr[i];
                long inputRemaining = input2.size();
                long inputOffset = 0;
                while (true) {
                    long inputRemaining2 = inputRemaining;
                    if (inputRemaining2 <= 0) {
                        break;
                    }
                    int i4;
                    int totalChunkCount = totalChunkCountLong;
                    int chunkSize = (int) Math.min(inputRemaining2, Trace.TRACE_TAG_DATABASE);
                    setUnsignedInt32LittleEndian(chunkSize, chunkContentPrefix2, 1);
                    int i5 = 0;
                    while (true) {
                        i4 = i3;
                        i3 = i5;
                        if (i3 >= mds2.length) {
                            break;
                        }
                        mds2[i3].update(chunkContentPrefix2);
                        i5 = i3 + 1;
                        i3 = i4;
                    }
                    totalChunkCountLong3 = totalChunkCountLong2;
                    totalChunkCountLong2 = inputOffset;
                    DataSource input3;
                    try {
                        input2.feedIntoDataDigester(digester2, totalChunkCountLong2, chunkSize);
                        totalChunkCountLong = 0;
                        while (totalChunkCountLong < iArr.length) {
                            i3 = iArr[totalChunkCountLong];
                            input3 = input2;
                            input2 = digestsOfChunks[totalChunkCountLong];
                            chunkContentPrefix = chunkContentPrefix2;
                            int expectedDigestSizeBytes = getContentDigestAlgorithmOutputSizeBytes(i3);
                            digester = digester2;
                            digester2 = mds2[totalChunkCountLong];
                            mds = mds2;
                            mds2 = digester2.digest(input2, 5 + (chunkIndex * expectedDigestSizeBytes), expectedDigestSizeBytes);
                            if (mds2 == expectedDigestSizeBytes) {
                                totalChunkCountLong++;
                                input2 = input3;
                                chunkContentPrefix2 = chunkContentPrefix;
                                digester2 = digester;
                                mds2 = mds;
                            } else {
                                int i6 = totalChunkCountLong;
                                totalChunkCountLong = new StringBuilder();
                                byte[] concatenationOfChunkCountAndChunkDigests2 = input2;
                                totalChunkCountLong.append("Unexpected output size of ");
                                totalChunkCountLong.append(digester2.getAlgorithm());
                                totalChunkCountLong.append(" digest: ");
                                totalChunkCountLong.append(mds2);
                                throw new RuntimeException(totalChunkCountLong.toString());
                            }
                        }
                        chunkContentPrefix = chunkContentPrefix2;
                        inputOffset = totalChunkCountLong2 + ((long) chunkSize);
                        inputRemaining = inputRemaining2 - ((long) chunkSize);
                        chunkIndex++;
                        int i7 = 5;
                        totalChunkCountLong = totalChunkCount;
                        i3 = i4;
                        totalChunkCountLong2 = totalChunkCountLong3;
                        input2 = input2;
                        digester2 = digester2;
                        mds2 = mds2;
                        dataSourceArr = contents;
                    } catch (IOException e2) {
                        input3 = input2;
                        chunkContentPrefix = chunkContentPrefix2;
                        mds = mds2;
                        digester = digester2;
                        totalChunkCountLong = e2;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failed to digest chunk #");
                        stringBuilder2.append(chunkIndex);
                        stringBuilder2.append(" of section #");
                        stringBuilder2.append(dataSourceIndex);
                        throw new DigestException(stringBuilder2.toString(), e2);
                    }
                }
            }
            totalChunkCountLong3 = totalChunkCountLong2;
            chunkContentPrefix = chunkContentPrefix2;
            mds = mds2;
            digester = digester2;
            byte[][] result = new byte[iArr.length][];
            int i8 = 0;
            while (true) {
                totalChunkCountLong = i8;
                if (totalChunkCountLong >= iArr.length) {
                    return result;
                }
                int digestAlgorithm = iArr[totalChunkCountLong];
                totalChunkCountLong2 = digestsOfChunks[totalChunkCountLong];
                String jcaAlgorithmName2 = getContentDigestAlgorithmJcaDigestAlgorithm(digestAlgorithm);
                try {
                    result[totalChunkCountLong] = MessageDigest.getInstance(jcaAlgorithmName2).digest(totalChunkCountLong2);
                    i8 = totalChunkCountLong + 1;
                } catch (NoSuchAlgorithmException e3) {
                    NoSuchAlgorithmException noSuchAlgorithmException = e3;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(jcaAlgorithmName2);
                    stringBuilder.append(" digest not supported");
                    throw new RuntimeException(stringBuilder.toString(), e3);
                }
            }
        }
        totalChunkCountLong3 = totalChunkCountLong2;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Too many chunks: ");
        stringBuilder3.append(totalChunkCountLong3);
        throw new DigestException(stringBuilder3.toString());
    }

    static byte[] parseVerityDigestAndVerifySourceLength(byte[] data, long fileSize, SignatureInfo signatureInfo) throws SecurityException {
        if (data.length == 32 + 8) {
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            buffer.position(32);
            if (buffer.getLong() == fileSize - (signatureInfo.centralDirOffset - signatureInfo.apkSigningBlockOffset)) {
                return Arrays.copyOfRange(data, 0, 32);
            }
            throw new SecurityException("APK content size did not verify");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Verity digest size is wrong: ");
        stringBuilder.append(data.length);
        throw new SecurityException(stringBuilder.toString());
    }

    private static void verifyIntegrityForVerityBasedAlgorithm(byte[] expectedDigest, RandomAccessFile apk, SignatureInfo signatureInfo) throws SecurityException {
        try {
            if (!Arrays.equals(parseVerityDigestAndVerifySourceLength(expectedDigest, apk.length(), signatureInfo), ApkVerityBuilder.generateApkVerity(apk, signatureInfo, new ByteBufferFactory() {
                public ByteBuffer create(int capacity) {
                    return ByteBuffer.allocate(capacity);
                }
            }).rootHash)) {
                throw new SecurityException("APK verity digest of contents did not verify");
            }
        } catch (IOException | DigestException | NoSuchAlgorithmException e) {
            throw new SecurityException("Error during verification", e);
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0017, code skipped:
            if (r1 != null) goto L_0x0019;
     */
    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:12:0x001d, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            r1.addSuppressed(r3);
     */
    /* JADX WARNING: Missing block: B:14:0x0022, code skipped:
            r0.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] generateApkVerity(String apkPath, ByteBufferFactory bufferFactory, SignatureInfo signatureInfo) throws IOException, SignatureNotFoundException, SecurityException, DigestException, NoSuchAlgorithmException {
        RandomAccessFile apk = new RandomAccessFile(apkPath, "r");
        byte[] bArr = ApkVerityBuilder.generateApkVerity(apk, signatureInfo, bufferFactory).rootHash;
        apk.close();
        return bArr;
    }

    static Pair<ByteBuffer, Long> getEocd(RandomAccessFile apk) throws IOException, SignatureNotFoundException {
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = ZipUtils.findZipEndOfCentralDirectoryRecord(apk);
        if (eocdAndOffsetInFile != null) {
            return eocdAndOffsetInFile;
        }
        throw new SignatureNotFoundException("Not an APK file: ZIP End of Central Directory record not found");
    }

    static long getCentralDirOffset(ByteBuffer eocd, long eocdOffset) throws SignatureNotFoundException {
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

    private static long getChunkCount(long inputSizeBytes) {
        return ((inputSizeBytes + Trace.TRACE_TAG_DATABASE) - 1) / Trace.TRACE_TAG_DATABASE;
    }

    static int compareSignatureAlgorithm(int sigAlgorithm1, int sigAlgorithm2) {
        return compareContentDigestAlgorithm(getSignatureAlgorithmContentDigestAlgorithm(sigAlgorithm1), getSignatureAlgorithmContentDigestAlgorithm(sigAlgorithm2));
    }

    private static int compareContentDigestAlgorithm(int digestAlgorithm1, int digestAlgorithm2) {
        StringBuilder stringBuilder;
        switch (digestAlgorithm1) {
            case 1:
                switch (digestAlgorithm2) {
                    case 1:
                        return 0;
                    case 2:
                    case 3:
                        return -1;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown digestAlgorithm2: ");
                        stringBuilder.append(digestAlgorithm2);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            case 2:
                switch (digestAlgorithm2) {
                    case 1:
                    case 3:
                        return 1;
                    case 2:
                        return 0;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown digestAlgorithm2: ");
                        stringBuilder.append(digestAlgorithm2);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            case 3:
                switch (digestAlgorithm2) {
                    case 1:
                        return 1;
                    case 2:
                        return -1;
                    case 3:
                        return 0;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown digestAlgorithm2: ");
                        stringBuilder.append(digestAlgorithm2);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown digestAlgorithm1: ");
                stringBuilder.append(digestAlgorithm1);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0035, code skipped:
            return 2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int getSignatureAlgorithmContentDigestAlgorithm(int sigAlgorithm) {
        if (sigAlgorithm != 769) {
            if (sigAlgorithm == SIGNATURE_VERITY_RSA_PKCS1_V1_5_WITH_SHA256 || sigAlgorithm == SIGNATURE_VERITY_ECDSA_WITH_SHA256 || sigAlgorithm == 1061) {
                return 3;
            }
            switch (sigAlgorithm) {
                case 257:
                case 259:
                    break;
                case 258:
                case 260:
                    break;
                default:
                    switch (sigAlgorithm) {
                        case 513:
                            break;
                        case 514:
                            break;
                        default:
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown signature algorithm: 0x");
                            stringBuilder.append(Long.toHexString((long) (sigAlgorithm & -1)));
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
            }
        }
        return 1;
    }

    static String getContentDigestAlgorithmJcaDigestAlgorithm(int digestAlgorithm) {
        switch (digestAlgorithm) {
            case 1:
            case 3:
                return KeyProperties.DIGEST_SHA256;
            case 2:
                return KeyProperties.DIGEST_SHA512;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown content digest algorthm: ");
                stringBuilder.append(digestAlgorithm);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static int getContentDigestAlgorithmOutputSizeBytes(int digestAlgorithm) {
        switch (digestAlgorithm) {
            case 1:
            case 3:
                return 32;
            case 2:
                return 64;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown content digest algorthm: ");
                stringBuilder.append(digestAlgorithm);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    static String getSignatureAlgorithmJcaKeyAlgorithm(int sigAlgorithm) {
        if (sigAlgorithm != 769) {
            if (sigAlgorithm != SIGNATURE_VERITY_RSA_PKCS1_V1_5_WITH_SHA256) {
                if (sigAlgorithm != SIGNATURE_VERITY_ECDSA_WITH_SHA256) {
                    if (sigAlgorithm != 1061) {
                        switch (sigAlgorithm) {
                            case 257:
                            case 258:
                            case 259:
                            case 260:
                                break;
                            default:
                                switch (sigAlgorithm) {
                                    case 513:
                                    case 514:
                                        break;
                                    default:
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unknown signature algorithm: 0x");
                                        stringBuilder.append(Long.toHexString((long) (sigAlgorithm & -1)));
                                        throw new IllegalArgumentException(stringBuilder.toString());
                                }
                        }
                    }
                }
                return KeyProperties.KEY_ALGORITHM_EC;
            }
            return KeyProperties.KEY_ALGORITHM_RSA;
        }
        return "DSA";
    }

    static Pair<String, ? extends AlgorithmParameterSpec> getSignatureAlgorithmJcaSignatureAlgorithm(int sigAlgorithm) {
        if (sigAlgorithm != 769) {
            if (sigAlgorithm != SIGNATURE_VERITY_RSA_PKCS1_V1_5_WITH_SHA256) {
                if (sigAlgorithm != SIGNATURE_VERITY_ECDSA_WITH_SHA256) {
                    if (sigAlgorithm != 1061) {
                        switch (sigAlgorithm) {
                            case 257:
                                return Pair.create("SHA256withRSA/PSS", new PSSParameterSpec(KeyProperties.DIGEST_SHA256, "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
                            case 258:
                                return Pair.create("SHA512withRSA/PSS", new PSSParameterSpec(KeyProperties.DIGEST_SHA512, "MGF1", MGF1ParameterSpec.SHA512, 64, 1));
                            case 259:
                                break;
                            case 260:
                                return Pair.create("SHA512withRSA", null);
                            default:
                                switch (sigAlgorithm) {
                                    case 513:
                                        break;
                                    case 514:
                                        return Pair.create("SHA512withECDSA", null);
                                    default:
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Unknown signature algorithm: 0x");
                                        stringBuilder.append(Long.toHexString((long) (sigAlgorithm & -1)));
                                        throw new IllegalArgumentException(stringBuilder.toString());
                                }
                        }
                    }
                }
                return Pair.create("SHA256withECDSA", null);
            }
            return Pair.create("SHA256withRSA", null);
        }
        return Pair.create("SHA256withDSA", null);
    }

    static ByteBuffer sliceFromTo(ByteBuffer source, int start, int end) {
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

    static ByteBuffer getByteBuffer(ByteBuffer source, int size) throws BufferUnderflowException {
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

    static ByteBuffer getLengthPrefixedSlice(ByteBuffer source) throws IOException {
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

    static byte[] readLengthPrefixedByteArray(ByteBuffer buf) throws IOException {
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

    static void setUnsignedInt32LittleEndian(int value, byte[] result, int offset) {
        result[offset] = (byte) (value & 255);
        result[offset + 1] = (byte) ((value >>> 8) & 255);
        result[offset + 2] = (byte) ((value >>> 16) & 255);
        result[offset + 3] = (byte) ((value >>> 24) & 255);
    }

    static Pair<ByteBuffer, Long> findApkSigningBlock(RandomAccessFile apk, long centralDirOffset) throws IOException, SignatureNotFoundException {
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

    static ByteBuffer findApkSignatureSchemeBlock(ByteBuffer apkSigningBlock, int blockId) throws SignatureNotFoundException {
        StringBuilder stringBuilder;
        checkByteOrderLittleEndian(apkSigningBlock);
        ByteBuffer pairs = sliceFromTo(apkSigningBlock, 8, apkSigningBlock.capacity() - 24);
        int entryCount = 0;
        while (pairs.hasRemaining()) {
            entryCount++;
            if (pairs.remaining() >= 8) {
                long lenLong = pairs.getLong();
                if (lenLong < 4 || lenLong > 2147483647L) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("APK Signing Block entry #");
                    stringBuilder2.append(entryCount);
                    stringBuilder2.append(" size out of range: ");
                    stringBuilder2.append(lenLong);
                    throw new SignatureNotFoundException(stringBuilder2.toString());
                }
                int len = (int) lenLong;
                int nextEntryPos = pairs.position() + len;
                if (len > pairs.remaining()) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("APK Signing Block entry #");
                    stringBuilder3.append(entryCount);
                    stringBuilder3.append(" size out of range: ");
                    stringBuilder3.append(len);
                    stringBuilder3.append(", available: ");
                    stringBuilder3.append(pairs.remaining());
                    throw new SignatureNotFoundException(stringBuilder3.toString());
                } else if (pairs.getInt() == blockId) {
                    return getByteBuffer(pairs, len - 4);
                } else {
                    pairs.position(nextEntryPos);
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Insufficient data to read size of APK Signing Block entry #");
                stringBuilder.append(entryCount);
                throw new SignatureNotFoundException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("No block with ID ");
        stringBuilder.append(blockId);
        stringBuilder.append(" in APK Signing Block.");
        throw new SignatureNotFoundException(stringBuilder.toString());
    }

    private static void checkByteOrderLittleEndian(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }
}
