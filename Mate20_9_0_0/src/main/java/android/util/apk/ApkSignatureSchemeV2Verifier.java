package android.util.apk;

import android.os.Process;
import android.util.ArrayMap;
import android.util.Pair;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ApkSignatureSchemeV2Verifier {
    private static final int APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 1896449818;
    public static final int SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID = 2;
    private static final int STRIPPING_PROTECTION_ATTR_ID = -1091571699;

    public static class VerifiedSigner {
        public final X509Certificate[][] certs;
        public final byte[] verityRootHash;

        public VerifiedSigner(X509Certificate[][] certs, byte[] verityRootHash) {
            this.certs = certs;
            this.verityRootHash = verityRootHash;
        }
    }

    public static boolean hasSignature(String apkFile) throws IOException {
        RandomAccessFile apk;
        try {
            apk = new RandomAccessFile(apkFile, "r");
            findSignature(apk);
            $closeResource(null, apk);
            return true;
        } catch (SignatureNotFoundException e) {
            return false;
        } catch (Throwable th) {
            $closeResource(r1, apk);
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

    public static X509Certificate[][] verify(String apkFile) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(apkFile, true).certs;
    }

    public static X509Certificate[][] plsCertsNoVerifyOnlyCerts(String apkFile) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(apkFile, (boolean) null).certs;
    }

    /* JADX WARNING: Missing block: B:9:0x0015, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static VerifiedSigner verify(String apkFile, boolean verifyIntegrity) throws SignatureNotFoundException, SecurityException, IOException {
        RandomAccessFile apk = new RandomAccessFile(apkFile, "r");
        VerifiedSigner verify = verify(apk, verifyIntegrity);
        $closeResource(null, apk);
        return verify;
    }

    private static VerifiedSigner verify(RandomAccessFile apk, boolean verifyIntegrity) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(apk, findSignature(apk), verifyIntegrity);
    }

    private static SignatureInfo findSignature(RandomAccessFile apk) throws IOException, SignatureNotFoundException {
        return ApkSigningBlockUtils.findSignature(apk, APK_SIGNATURE_SCHEME_V2_BLOCK_ID);
    }

    private static VerifiedSigner verify(RandomAccessFile apk, SignatureInfo signatureInfo, boolean doVerifyIntegrity) throws SecurityException, IOException {
        int signerCount = 0;
        Map<Integer, byte[]> contentDigests = new ArrayMap();
        List<X509Certificate[]> signerCerts = new ArrayList();
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            try {
                ByteBuffer signers = ApkSigningBlockUtils.getLengthPrefixedSlice(signatureInfo.signatureBlock);
                while (signers.hasRemaining()) {
                    signerCount++;
                    try {
                        signerCerts.add(verifySigner(ApkSigningBlockUtils.getLengthPrefixedSlice(signers), contentDigests, certFactory));
                    } catch (IOException | SecurityException | BufferUnderflowException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to parse/verify signer #");
                        stringBuilder.append(signerCount);
                        stringBuilder.append(" block");
                        throw new SecurityException(stringBuilder.toString(), e);
                    }
                }
                if (signerCount < 1) {
                    throw new SecurityException("No signers found");
                } else if (contentDigests.isEmpty()) {
                    throw new SecurityException("No content digests found");
                } else {
                    if (doVerifyIntegrity) {
                        ApkSigningBlockUtils.verifyIntegrity(contentDigests, apk, signatureInfo);
                    }
                    byte[] verityRootHash = null;
                    if (contentDigests.containsKey(Integer.valueOf(3))) {
                        verityRootHash = ApkSigningBlockUtils.parseVerityDigestAndVerifySourceLength((byte[]) contentDigests.get(Integer.valueOf(3)), apk.length(), signatureInfo);
                    }
                    return new VerifiedSigner((X509Certificate[][]) signerCerts.toArray(new X509Certificate[signerCerts.size()][]), verityRootHash);
                }
            } catch (IOException e2) {
                throw new SecurityException("Failed to read list of signers", e2);
            }
        } catch (CertificateException e3) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e3);
        }
    }

    private static X509Certificate[] verifySigner(ByteBuffer signerBlock, Map<Integer, byte[]> contentDigests, CertificateFactory certFactory) throws SecurityException, IOException {
        ByteBuffer signature;
        Exception e;
        GeneralSecurityException e2;
        List<Integer> list;
        String str;
        StringBuilder stringBuilder;
        ByteBuffer signedData = ApkSigningBlockUtils.getLengthPrefixedSlice(signerBlock);
        ByteBuffer signatures = ApkSigningBlockUtils.getLengthPrefixedSlice(signerBlock);
        byte[] publicKeyBytes = ApkSigningBlockUtils.readLengthPrefixedByteArray(signerBlock);
        List<Integer> signaturesSigAlgorithms = new ArrayList();
        byte[] bestSigAlgorithmSignatureBytes = null;
        int bestSigAlgorithm = -1;
        int signatureCount = 0;
        while (signatures.hasRemaining()) {
            signatureCount++;
            try {
                signature = ApkSigningBlockUtils.getLengthPrefixedSlice(signatures);
                if (signature.remaining() >= 8) {
                    int sigAlgorithm = signature.getInt();
                    signaturesSigAlgorithms.add(Integer.valueOf(sigAlgorithm));
                    if (isSupportedSignatureAlgorithm(sigAlgorithm)) {
                        if (bestSigAlgorithm == -1 || ApkSigningBlockUtils.compareSignatureAlgorithm(sigAlgorithm, bestSigAlgorithm) > 0) {
                            bestSigAlgorithm = sigAlgorithm;
                            bestSigAlgorithmSignatureBytes = ApkSigningBlockUtils.readLengthPrefixedByteArray(signature);
                        }
                    }
                } else {
                    throw new SecurityException("Signature record too short");
                }
            } catch (IOException | BufferUnderflowException e3) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to parse signature record #");
                stringBuilder2.append(signatureCount);
                throw new SecurityException(stringBuilder2.toString(), e3);
            }
        }
        if (bestSigAlgorithm != -1) {
            String keyAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmJcaKeyAlgorithm(bestSigAlgorithm);
            Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmParams = ApkSigningBlockUtils.getSignatureAlgorithmJcaSignatureAlgorithm(bestSigAlgorithm);
            String jcaSignatureAlgorithm = signatureAlgorithmParams.first;
            AlgorithmParameterSpec jcaSignatureAlgorithmParams = (AlgorithmParameterSpec) signatureAlgorithmParams.second;
            ByteBuffer byteBuffer;
            int i;
            int i2;
            byte[] bArr;
            try {
                PublicKey publicKey = KeyFactory.getInstance(keyAlgorithm).generatePublic(new X509EncodedKeySpec(publicKeyBytes));
                Signature sig = Signature.getInstance(jcaSignatureAlgorithm);
                sig.initVerify(publicKey);
                if (jcaSignatureAlgorithmParams != null) {
                    try {
                        sig.setParameter(jcaSignatureAlgorithmParams);
                    } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e4) {
                        e2 = e4;
                        byteBuffer = signatures;
                        i = signatureCount;
                        i2 = bestSigAlgorithm;
                        list = signaturesSigAlgorithms;
                        bArr = bestSigAlgorithmSignatureBytes;
                        str = keyAlgorithm;
                    }
                }
                sig.update(signedData);
                if (sig.verify(bestSigAlgorithmSignatureBytes)) {
                    signedData.clear();
                    ByteBuffer digests = ApkSigningBlockUtils.getLengthPrefixedSlice(signedData);
                    List<Integer> digestsSigAlgorithms = new ArrayList();
                    byte[] contentDigest = null;
                    int digestCount = 0;
                    while (true) {
                        int digestCount2 = digestCount;
                        if (digests.hasRemaining()) {
                            byteBuffer = signatures;
                            signatures = digestCount2 + 1;
                            try {
                                signature = ApkSigningBlockUtils.getLengthPrefixedSlice(digests);
                                i = signatureCount;
                                try {
                                    bArr = bestSigAlgorithmSignatureBytes;
                                    if (signature.remaining() >= 8) {
                                        try {
                                            signatureCount = signature.getInt();
                                            digestsSigAlgorithms.add(Integer.valueOf(signatureCount));
                                            if (signatureCount == bestSigAlgorithm) {
                                                contentDigest = ApkSigningBlockUtils.readLengthPrefixedByteArray(signature);
                                            }
                                            digestCount = signatures;
                                            signatures = byteBuffer;
                                            signatureCount = i;
                                            bestSigAlgorithmSignatureBytes = bArr;
                                        } catch (IOException | BufferUnderflowException e5) {
                                            e3 = e5;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Failed to parse digest record #");
                                            stringBuilder.append(signatures);
                                            throw new IOException(stringBuilder.toString(), e3);
                                        }
                                    }
                                    throw new IOException("Record too short");
                                } catch (IOException | BufferUnderflowException e6) {
                                    e3 = e6;
                                    bArr = bestSigAlgorithmSignatureBytes;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to parse digest record #");
                                    stringBuilder.append(signatures);
                                    throw new IOException(stringBuilder.toString(), e3);
                                }
                            } catch (IOException | BufferUnderflowException e7) {
                                e3 = e7;
                                i = signatureCount;
                                bArr = bestSigAlgorithmSignatureBytes;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to parse digest record #");
                                stringBuilder.append(signatures);
                                throw new IOException(stringBuilder.toString(), e3);
                            }
                        }
                        i = signatureCount;
                        bArr = bestSigAlgorithmSignatureBytes;
                        str = keyAlgorithm;
                        if (signaturesSigAlgorithms.equals(digestsSigAlgorithms)) {
                            signatures = ApkSigningBlockUtils.getSignatureAlgorithmContentDigestAlgorithm(bestSigAlgorithm);
                            bestSigAlgorithmSignatureBytes = (byte[]) contentDigests.put(Integer.valueOf(signatures), contentDigest);
                            if (bestSigAlgorithmSignatureBytes == null || MessageDigest.isEqual(bestSigAlgorithmSignatureBytes, contentDigest)) {
                                int digestAlgorithm;
                                ByteBuffer certificates;
                                ByteBuffer certificates2 = ApkSigningBlockUtils.getLengthPrefixedSlice(signedData);
                                ArrayList keyAlgorithm2 = new ArrayList();
                                digestCount = 0;
                                while (certificates2.hasRemaining()) {
                                    digestAlgorithm = signatures;
                                    signatures = digestCount + 1;
                                    byte[] encodedCert = ApkSigningBlockUtils.readLengthPrefixedByteArray(certificates2);
                                    byte[] encodedCert2;
                                    try {
                                        certificates = certificates2;
                                        encodedCert2 = encodedCert;
                                        try {
                                            i2 = bestSigAlgorithm;
                                            try {
                                                keyAlgorithm2.add(new VerbatimX509Certificate((X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(encodedCert2)), encodedCert2));
                                                digestCount = signatures;
                                                signatures = digestAlgorithm;
                                                certificates2 = certificates;
                                                bestSigAlgorithm = i2;
                                            } catch (CertificateException e8) {
                                                digestCount = e8;
                                                certificates2 = new StringBuilder();
                                                certificates2.append("Failed to decode certificate #");
                                                certificates2.append(signatures);
                                                throw new SecurityException(certificates2.toString(), digestCount);
                                            }
                                        } catch (CertificateException e9) {
                                            digestCount = e9;
                                            i2 = bestSigAlgorithm;
                                            certificates2 = new StringBuilder();
                                            certificates2.append("Failed to decode certificate #");
                                            certificates2.append(signatures);
                                            throw new SecurityException(certificates2.toString(), digestCount);
                                        }
                                    } catch (CertificateException e10) {
                                        digestCount = e10;
                                        certificates = certificates2;
                                        i2 = bestSigAlgorithm;
                                        encodedCert2 = encodedCert;
                                        certificates2 = new StringBuilder();
                                        certificates2.append("Failed to decode certificate #");
                                        certificates2.append(signatures);
                                        throw new SecurityException(certificates2.toString(), digestCount);
                                    }
                                }
                                digestAlgorithm = signatures;
                                certificates = certificates2;
                                i2 = bestSigAlgorithm;
                                list = signaturesSigAlgorithms;
                                if (keyAlgorithm2.isEmpty() != null) {
                                    throw new SecurityException("No certificates listed");
                                } else if (Arrays.equals(publicKeyBytes, ((X509Certificate) keyAlgorithm2.get(null)).getPublicKey().getEncoded()) != 0) {
                                    verifyAdditionalAttributes(ApkSigningBlockUtils.getLengthPrefixedSlice(signedData));
                                    return (X509Certificate[]) keyAlgorithm2.toArray(new X509Certificate[keyAlgorithm2.size()]);
                                } else {
                                    throw new SecurityException("Public key mismatch between certificate and signature record");
                                }
                            }
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(ApkSigningBlockUtils.getContentDigestAlgorithmJcaDigestAlgorithm(signatures));
                            stringBuilder3.append(" contents digest does not match the digest specified by a preceding signer");
                            throw new SecurityException(stringBuilder3.toString());
                        }
                        list = signaturesSigAlgorithms;
                        throw new SecurityException("Signature algorithms don't match between digests and signatures records");
                    }
                }
                i = signatureCount;
                i2 = bestSigAlgorithm;
                list = signaturesSigAlgorithms;
                bArr = bestSigAlgorithmSignatureBytes;
                str = keyAlgorithm;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(jcaSignatureAlgorithm);
                stringBuilder4.append(" signature did not verify");
                throw new SecurityException(stringBuilder4.toString());
            } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e11) {
                e2 = e11;
                byteBuffer = signatures;
                i = signatureCount;
                i2 = bestSigAlgorithm;
                list = signaturesSigAlgorithms;
                bArr = bestSigAlgorithmSignatureBytes;
                str = keyAlgorithm;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Failed to verify ");
                stringBuilder5.append(jcaSignatureAlgorithm);
                stringBuilder5.append(" signature");
                throw new SecurityException(stringBuilder5.toString(), e2);
            }
        } else if (signatureCount == 0) {
            throw new SecurityException("No signatures found");
        } else {
            throw new SecurityException("No supported signatures found");
        }
    }

    private static void verifyAdditionalAttributes(ByteBuffer attrs) throws SecurityException, IOException {
        while (attrs.hasRemaining()) {
            ByteBuffer attr = ApkSigningBlockUtils.getLengthPrefixedSlice(attrs);
            if (attr.remaining() < 4) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Remaining buffer too short to contain additional attribute ID. Remaining: ");
                stringBuilder.append(attr.remaining());
                throw new IOException(stringBuilder.toString());
            } else if (attr.getInt() == STRIPPING_PROTECTION_ATTR_ID) {
                if (attr.remaining() < 4) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("V2 Signature Scheme Stripping Protection Attribute  value too small.  Expected 4 bytes, but found ");
                    stringBuilder2.append(attr.remaining());
                    throw new IOException(stringBuilder2.toString());
                } else if (attr.getInt() == 3) {
                    throw new SecurityException("V2 signature indicates APK is signed using APK Signature Scheme v3, but none was found. Signature stripped?");
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x001c, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static byte[] getVerityRootHash(String apkPath) throws IOException, SignatureNotFoundException, SecurityException {
        RandomAccessFile apk = new RandomAccessFile(apkPath, "r");
        SignatureInfo signatureInfo = findSignature(apk);
        byte[] bArr = verify(apk, (boolean) null).verityRootHash;
        $closeResource(null, apk);
        return bArr;
    }

    /* JADX WARNING: Missing block: B:9:0x0019, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static byte[] generateApkVerity(String apkPath, ByteBufferFactory bufferFactory) throws IOException, SignatureNotFoundException, SecurityException, DigestException, NoSuchAlgorithmException {
        RandomAccessFile apk = new RandomAccessFile(apkPath, "r");
        byte[] generateApkVerity = ApkSigningBlockUtils.generateApkVerity(apkPath, bufferFactory, findSignature(apk));
        $closeResource(null, apk);
        return generateApkVerity;
    }

    /* JADX WARNING: Missing block: B:14:0x002d, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static byte[] generateFsverityRootHash(String apkPath) throws IOException, SignatureNotFoundException, DigestException, NoSuchAlgorithmException {
        RandomAccessFile apk = new RandomAccessFile(apkPath, "r");
        SignatureInfo signatureInfo = findSignature(apk);
        VerifiedSigner vSigner = verify(apk, (boolean) null);
        if (vSigner.verityRootHash == null) {
            $closeResource(null, apk);
            return null;
        }
        byte[] generateFsverityRootHash = ApkVerityBuilder.generateFsverityRootHash(apk, ByteBuffer.wrap(vSigner.verityRootHash), signatureInfo);
        $closeResource(null, apk);
        return generateFsverityRootHash;
    }

    private static boolean isSupportedSignatureAlgorithm(int sigAlgorithm) {
        if (!(sigAlgorithm == 769 || sigAlgorithm == 1057 || sigAlgorithm == 1059 || sigAlgorithm == Process.OTA_UPDATE_UID)) {
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
                            return false;
                    }
            }
        }
        return true;
    }
}
