package android.util.apk;

import android.os.Build.VERSION;
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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ApkSignatureSchemeV3Verifier {
    private static final int APK_SIGNATURE_SCHEME_V3_BLOCK_ID = -262969152;
    private static final int PROOF_OF_ROTATION_ATTR_ID = 1000370060;
    public static final int SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID = 3;

    private static class PlatformNotSupportedException extends Exception {
        PlatformNotSupportedException(String s) {
            super(s);
        }
    }

    public static class VerifiedProofOfRotation {
        public final List<X509Certificate> certs;
        public final List<Integer> flagsList;

        public VerifiedProofOfRotation(List<X509Certificate> certs, List<Integer> flagsList) {
            this.certs = certs;
            this.flagsList = flagsList;
        }
    }

    public static class VerifiedSigner {
        public final X509Certificate[] certs;
        public final VerifiedProofOfRotation por;
        public byte[] verityRootHash;

        public VerifiedSigner(X509Certificate[] certs, VerifiedProofOfRotation por) {
            this.certs = certs;
            this.por = por;
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

    public static VerifiedSigner verify(String apkFile) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(apkFile, true);
    }

    public static VerifiedSigner plsCertsNoVerifyOnlyCerts(String apkFile) throws SignatureNotFoundException, SecurityException, IOException {
        return verify(apkFile, false);
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
        return ApkSigningBlockUtils.findSignature(apk, APK_SIGNATURE_SCHEME_V3_BLOCK_ID);
    }

    private static VerifiedSigner verify(RandomAccessFile apk, SignatureInfo signatureInfo, boolean doVerifyIntegrity) throws SecurityException, IOException {
        int signerCount = 0;
        Map<Integer, byte[]> contentDigests = new ArrayMap();
        VerifiedSigner result = null;
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            try {
                ByteBuffer signers = ApkSigningBlockUtils.getLengthPrefixedSlice(signatureInfo.signatureBlock);
                while (signers.hasRemaining()) {
                    try {
                        result = verifySigner(ApkSigningBlockUtils.getLengthPrefixedSlice(signers), contentDigests, certFactory);
                        signerCount++;
                    } catch (PlatformNotSupportedException e) {
                    } catch (IOException | SecurityException | BufferUnderflowException e2) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to parse/verify signer #");
                        stringBuilder.append(signerCount);
                        stringBuilder.append(" block");
                        throw new SecurityException(stringBuilder.toString(), e2);
                    }
                }
                if (signerCount < 1 || result == null) {
                    throw new SecurityException("No signers found");
                } else if (signerCount != 1) {
                    throw new SecurityException("APK Signature Scheme V3 only supports one signer: multiple signers found.");
                } else if (contentDigests.isEmpty()) {
                    throw new SecurityException("No content digests found");
                } else {
                    if (doVerifyIntegrity) {
                        ApkSigningBlockUtils.verifyIntegrity(contentDigests, apk, signatureInfo);
                    }
                    if (contentDigests.containsKey(Integer.valueOf(3))) {
                        result.verityRootHash = ApkSigningBlockUtils.parseVerityDigestAndVerifySourceLength((byte[]) contentDigests.get(Integer.valueOf(3)), apk.length(), signatureInfo);
                    }
                    return result;
                }
            } catch (IOException e3) {
                throw new SecurityException("Failed to read list of signers", e3);
            }
        } catch (CertificateException e4) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e4);
        }
    }

    private static VerifiedSigner verifySigner(ByteBuffer signerBlock, Map<Integer, byte[]> contentDigests, CertificateFactory certFactory) throws SecurityException, IOException, PlatformNotSupportedException {
        Exception e;
        GeneralSecurityException e2;
        ByteBuffer byteBuffer;
        int i;
        List<Integer> list;
        Pair<String, ? extends AlgorithmParameterSpec> pair;
        StringBuilder stringBuilder;
        byte[] encodedCert;
        Object obj;
        CertificateFactory certificateFactory = certFactory;
        ByteBuffer signedData = ApkSigningBlockUtils.getLengthPrefixedSlice(signerBlock);
        int minSdkVersion = signerBlock.getInt();
        int maxSdkVersion = signerBlock.getInt();
        Map<Integer, byte[]> map;
        StringBuilder stringBuilder2;
        if (VERSION.SDK_INT < minSdkVersion || VERSION.SDK_INT > maxSdkVersion) {
            map = contentDigests;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Signer not supported by this platform version. This platform: ");
            stringBuilder2.append(VERSION.SDK_INT);
            stringBuilder2.append(", signer minSdkVersion: ");
            stringBuilder2.append(minSdkVersion);
            stringBuilder2.append(", maxSdkVersion: ");
            stringBuilder2.append(maxSdkVersion);
            throw new PlatformNotSupportedException(stringBuilder2.toString());
        }
        ByteBuffer signature;
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
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failed to parse signature record #");
                stringBuilder3.append(signatureCount);
                throw new SecurityException(stringBuilder3.toString(), e3);
            }
        }
        if (bestSigAlgorithm != -1) {
            String keyAlgorithm = ApkSigningBlockUtils.getSignatureAlgorithmJcaKeyAlgorithm(bestSigAlgorithm);
            Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmParams = ApkSigningBlockUtils.getSignatureAlgorithmJcaSignatureAlgorithm(bestSigAlgorithm);
            String jcaSignatureAlgorithm = signatureAlgorithmParams.first;
            AlgorithmParameterSpec jcaSignatureAlgorithmParams = (AlgorithmParameterSpec) signatureAlgorithmParams.second;
            int i2;
            byte[] bArr;
            String str;
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
                        pair = signatureAlgorithmParams;
                        bestSigAlgorithmSignatureBytes = contentDigests;
                    }
                }
                sig.update(signedData);
                boolean sigVerified = sig.verify(bestSigAlgorithmSignatureBytes);
                boolean sigVerified2;
                if (sigVerified) {
                    ByteBuffer digests;
                    signedData.clear();
                    signatures = ApkSigningBlockUtils.getLengthPrefixedSlice(signedData);
                    byte[] contentDigest = null;
                    List<Integer> digestsSigAlgorithms = new ArrayList();
                    signatureCount = contentDigest;
                    int digestCount = 0;
                    while (signatures.hasRemaining()) {
                        bArr = bestSigAlgorithmSignatureBytes;
                        bestSigAlgorithmSignatureBytes = digestCount + 1;
                        try {
                            digests = signatures;
                            signature = ApkSigningBlockUtils.getLengthPrefixedSlice(signatures);
                            try {
                                str = keyAlgorithm;
                                if (signature.remaining() >= 8) {
                                    try {
                                        signatures = signature.getInt();
                                        sigVerified2 = sigVerified;
                                        sigVerified = digestsSigAlgorithms;
                                    } catch (IOException | BufferUnderflowException e5) {
                                        e3 = e5;
                                        sigVerified2 = sigVerified;
                                        sigVerified = digestsSigAlgorithms;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Failed to parse digest record #");
                                        stringBuilder.append(bestSigAlgorithmSignatureBytes);
                                        throw new IOException(stringBuilder.toString(), e3);
                                    }
                                    try {
                                        sigVerified.add(Integer.valueOf(signatures));
                                        if (signatures == bestSigAlgorithm) {
                                            signatureCount = ApkSigningBlockUtils.readLengthPrefixedByteArray(signature);
                                        }
                                        digestCount = bestSigAlgorithmSignatureBytes;
                                        digestsSigAlgorithms = sigVerified;
                                        bestSigAlgorithmSignatureBytes = bArr;
                                        signatures = digests;
                                        keyAlgorithm = str;
                                        sigVerified = sigVerified2;
                                    } catch (IOException | BufferUnderflowException e6) {
                                        e3 = e6;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Failed to parse digest record #");
                                        stringBuilder.append(bestSigAlgorithmSignatureBytes);
                                        throw new IOException(stringBuilder.toString(), e3);
                                    }
                                }
                                sigVerified = digestsSigAlgorithms;
                                throw new IOException("Record too short");
                            } catch (IOException | BufferUnderflowException e7) {
                                e3 = e7;
                                str = keyAlgorithm;
                                sigVerified2 = sigVerified;
                                sigVerified = digestsSigAlgorithms;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to parse digest record #");
                                stringBuilder.append(bestSigAlgorithmSignatureBytes);
                                throw new IOException(stringBuilder.toString(), e3);
                            }
                        } catch (IOException | BufferUnderflowException e8) {
                            e3 = e8;
                            digests = signatures;
                            str = keyAlgorithm;
                            sigVerified2 = sigVerified;
                            sigVerified = digestsSigAlgorithms;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to parse digest record #");
                            stringBuilder.append(bestSigAlgorithmSignatureBytes);
                            throw new IOException(stringBuilder.toString(), e3);
                        }
                    }
                    digests = signatures;
                    bArr = bestSigAlgorithmSignatureBytes;
                    str = keyAlgorithm;
                    sigVerified2 = sigVerified;
                    pair = signatureAlgorithmParams;
                    Object obj2;
                    if (signaturesSigAlgorithms.equals(digestsSigAlgorithms)) {
                        int digestAlgorithm;
                        signatures = ApkSigningBlockUtils.getSignatureAlgorithmContentDigestAlgorithm(bestSigAlgorithm);
                        byte[] keyAlgorithm2 = (byte[]) contentDigests.put(Integer.valueOf(signatures), signatureCount);
                        byte[] contentDigest2;
                        if (keyAlgorithm2 == null) {
                            contentDigest2 = signatureCount;
                        } else if (MessageDigest.isEqual(keyAlgorithm2, signatureCount)) {
                            obj2 = signatureCount;
                        } else {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            contentDigest2 = signatureCount;
                            stringBuilder4.append(ApkSigningBlockUtils.getContentDigestAlgorithmJcaDigestAlgorithm(signatures));
                            stringBuilder4.append(" contents digest does not match the digest specified by a preceding signer");
                            throw new SecurityException(stringBuilder4.toString());
                        }
                        signatureCount = ApkSigningBlockUtils.getLengthPrefixedSlice(signedData);
                        ArrayList signatureAlgorithmParams2 = new ArrayList();
                        int certificateCount = 0;
                        while (signatureCount.hasRemaining()) {
                            digestAlgorithm = signatures;
                            signatures = certificateCount + 1;
                            byte[] encodedCert2 = ApkSigningBlockUtils.readLengthPrefixedByteArray(signatureCount);
                            try {
                                ByteBuffer certificates = signatureCount;
                                signatureCount = encodedCert2;
                                try {
                                    i2 = bestSigAlgorithm;
                                    signatureAlgorithmParams2.add(new VerbatimX509Certificate((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(signatureCount)), signatureCount));
                                    certificateCount = signatures;
                                    signatures = digestAlgorithm;
                                    signatureCount = certificates;
                                    bestSigAlgorithm = i2;
                                } catch (CertificateException e9) {
                                    certificateCount = e9;
                                    i2 = bestSigAlgorithm;
                                    encodedCert = signatureCount;
                                    signatureCount = new StringBuilder();
                                    signatureCount.append("Failed to decode certificate #");
                                    signatureCount.append(signatures);
                                    throw new SecurityException(signatureCount.toString(), certificateCount);
                                }
                            } catch (CertificateException e10) {
                                certificateCount = e10;
                                obj = signatureCount;
                                i2 = bestSigAlgorithm;
                                signatureCount = encodedCert2;
                                encodedCert = signatureCount;
                                signatureCount = new StringBuilder();
                                signatureCount.append("Failed to decode certificate #");
                                signatureCount.append(signatures);
                                throw new SecurityException(signatureCount.toString(), certificateCount);
                            }
                        }
                        digestAlgorithm = signatures;
                        obj = signatureCount;
                        i2 = bestSigAlgorithm;
                        list = signaturesSigAlgorithms;
                        if (signatureAlgorithmParams2.isEmpty() != null) {
                            throw new SecurityException("No certificates listed");
                        } else if (!Arrays.equals(publicKeyBytes, ((X509Certificate) signatureAlgorithmParams2.get(null)).getPublicKey().getEncoded())) {
                            throw new SecurityException("Public key mismatch between certificate and signature record");
                        } else if (signedData.getInt() != minSdkVersion) {
                            throw new SecurityException("minSdkVersion mismatch between signed and unsigned in v3 signer block.");
                        } else if (signedData.getInt() == maxSdkVersion) {
                            return verifyAdditionalAttributes(ApkSigningBlockUtils.getLengthPrefixedSlice(signedData), signatureAlgorithmParams2, certificateFactory);
                        } else {
                            throw new SecurityException("maxSdkVersion mismatch between signed and unsigned in v3 signer block.");
                        }
                    }
                    map = contentDigests;
                    obj2 = signatureCount;
                    i2 = bestSigAlgorithm;
                    list = signaturesSigAlgorithms;
                    throw new SecurityException("Signature algorithms don't match between digests and signatures records");
                }
                i = signatureCount;
                i2 = bestSigAlgorithm;
                list = signaturesSigAlgorithms;
                bArr = bestSigAlgorithmSignatureBytes;
                str = keyAlgorithm;
                sigVerified2 = sigVerified;
                pair = signatureAlgorithmParams;
                bestSigAlgorithmSignatureBytes = contentDigests;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(jcaSignatureAlgorithm);
                stringBuilder2.append(" signature did not verify");
                throw new SecurityException(stringBuilder2.toString());
            } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e11) {
                e2 = e11;
                byteBuffer = signatures;
                i = signatureCount;
                i2 = bestSigAlgorithm;
                list = signaturesSigAlgorithms;
                bArr = bestSigAlgorithmSignatureBytes;
                str = keyAlgorithm;
                pair = signatureAlgorithmParams;
                bestSigAlgorithmSignatureBytes = contentDigests;
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

    private static VerifiedSigner verifyAdditionalAttributes(ByteBuffer attrs, List<X509Certificate> certs, CertificateFactory certFactory) throws IOException {
        X509Certificate[] certChain = (X509Certificate[]) certs.toArray(new X509Certificate[certs.size()]);
        VerifiedProofOfRotation por = null;
        while (attrs.hasRemaining()) {
            ByteBuffer attr = ApkSigningBlockUtils.getLengthPrefixedSlice(attrs);
            if (attr.remaining() < 4) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Remaining buffer too short to contain additional attribute ID. Remaining: ");
                stringBuilder.append(attr.remaining());
                throw new IOException(stringBuilder.toString());
            } else if (attr.getInt() == PROOF_OF_ROTATION_ATTR_ID) {
                if (por == null) {
                    por = verifyProofOfRotationStruct(attr, certFactory);
                    try {
                        if (por.certs.size() <= 0) {
                            continue;
                        } else if (!Arrays.equals(((X509Certificate) por.certs.get(por.certs.size() - 1)).getEncoded(), certChain[0].getEncoded())) {
                            throw new SecurityException("Terminal certificate in Proof-of-rotation record does not match APK signing certificate");
                        }
                    } catch (CertificateEncodingException e) {
                        throw new SecurityException("Failed to encode certificate when comparing Proof-of-rotation record and signing certificate", e);
                    }
                }
                throw new SecurityException("Encountered multiple Proof-of-rotation records when verifying APK Signature Scheme v3 signature");
            }
        }
        return new VerifiedSigner(certChain, por);
    }

    private static VerifiedProofOfRotation verifyProofOfRotationStruct(ByteBuffer porBuf, CertificateFactory certFactory) throws SecurityException, IOException {
        Exception e;
        GeneralSecurityException e2;
        StringBuilder stringBuilder;
        CertificateException e3;
        int levelCount = 0;
        int lastSigAlgorithm = -1;
        X509Certificate lastCert = null;
        ArrayList certs = new ArrayList();
        ArrayList flagsList = new ArrayList();
        CertificateFactory certificateFactory;
        try {
            porBuf.getInt();
            HashSet<X509Certificate> certHistorySet = new HashSet();
            while (porBuf.hasRemaining()) {
                levelCount++;
                ByteBuffer level = ApkSigningBlockUtils.getLengthPrefixedSlice(porBuf);
                ByteBuffer signedData = ApkSigningBlockUtils.getLengthPrefixedSlice(level);
                int flags = level.getInt();
                int sigAlgorithm = level.getInt();
                byte[] signature = ApkSigningBlockUtils.readLengthPrefixedByteArray(level);
                if (lastCert != null) {
                    Pair<String, ? extends AlgorithmParameterSpec> sigAlgParams = ApkSigningBlockUtils.getSignatureAlgorithmJcaSignatureAlgorithm(lastSigAlgorithm);
                    PublicKey publicKey = lastCert.getPublicKey();
                    Signature sig = Signature.getInstance((String) sigAlgParams.first);
                    sig.initVerify(publicKey);
                    if (sigAlgParams.second != null) {
                        sig.setParameter((AlgorithmParameterSpec) sigAlgParams.second);
                    }
                    sig.update(signedData);
                    if (sig.verify(signature)) {
                        ByteBuffer byteBuffer = level;
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unable to verify signature of certificate #");
                        stringBuilder2.append(levelCount);
                        stringBuilder2.append(" using ");
                        stringBuilder2.append((String) sigAlgParams.first);
                        stringBuilder2.append(" when verifying Proof-of-rotation record");
                        throw new SecurityException(stringBuilder2.toString());
                    }
                }
                signedData.rewind();
                byte[] encodedCert = ApkSigningBlockUtils.readLengthPrefixedByteArray(signedData);
                int signedSigAlgorithm = signedData.getInt();
                if (lastCert != null) {
                    if (lastSigAlgorithm != signedSigAlgorithm) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Signing algorithm ID mismatch for certificate #");
                        stringBuilder3.append(levelCount);
                        stringBuilder3.append(" when verifying Proof-of-rotation record");
                        throw new SecurityException(stringBuilder3.toString());
                    }
                }
                try {
                    lastCert = new VerbatimX509Certificate((X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(encodedCert)), encodedCert);
                    lastSigAlgorithm = sigAlgorithm;
                    if (certHistorySet.contains(lastCert)) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Encountered duplicate entries in Proof-of-rotation record at certificate #");
                        stringBuilder4.append(levelCount);
                        stringBuilder4.append(".  All signing certificates should be unique");
                        throw new SecurityException(stringBuilder4.toString());
                    }
                    certHistorySet.add(lastCert);
                    certs.add(lastCert);
                    flagsList.add(Integer.valueOf(flags));
                } catch (IOException | BufferUnderflowException e4) {
                    e = e4;
                    throw new IOException("Failed to parse Proof-of-rotation record", e);
                } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException e5) {
                    e2 = e5;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to verify signature over signed data for certificate #");
                    stringBuilder.append(levelCount);
                    stringBuilder.append(" when verifying Proof-of-rotation record");
                    throw new SecurityException(stringBuilder.toString(), e2);
                } catch (CertificateException e6) {
                    e3 = e6;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to decode certificate #");
                    stringBuilder.append(levelCount);
                    stringBuilder.append(" when verifying Proof-of-rotation record");
                    throw new SecurityException(stringBuilder.toString(), e3);
                }
            }
            certificateFactory = certFactory;
            return new VerifiedProofOfRotation(certs, flagsList);
        } catch (IOException | BufferUnderflowException e7) {
            e = e7;
            certificateFactory = certFactory;
            throw new IOException("Failed to parse Proof-of-rotation record", e);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | SignatureException e8) {
            e2 = e8;
            certificateFactory = certFactory;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to verify signature over signed data for certificate #");
            stringBuilder.append(levelCount);
            stringBuilder.append(" when verifying Proof-of-rotation record");
            throw new SecurityException(stringBuilder.toString(), e2);
        } catch (CertificateException e9) {
            e3 = e9;
            certificateFactory = certFactory;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to decode certificate #");
            stringBuilder.append(levelCount);
            stringBuilder.append(" when verifying Proof-of-rotation record");
            throw new SecurityException(stringBuilder.toString(), e3);
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
    static byte[] generateFsverityRootHash(String apkPath) throws NoSuchAlgorithmException, DigestException, IOException, SignatureNotFoundException {
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
