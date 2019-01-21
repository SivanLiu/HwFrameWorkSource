package android.util.apk;

import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.PackageParser.SigningDetails.SignatureSchemeVersion;
import android.content.pm.Signature;
import android.os.Trace;
import android.security.keymaster.KeymasterDefs;
import android.util.apk.ApkSignatureSchemeV3Verifier.VerifiedSigner;
import android.util.jar.StrictJarFile;
import com.android.internal.util.ArrayUtils;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;

public class ApkSignatureVerifier {
    private static final AtomicReference<byte[]> sBuffer = new AtomicReference();

    public static class Result {
        public final Certificate[][] certs;
        public final int signatureSchemeVersion;
        public final Signature[] sigs;

        public Result(Certificate[][] certs, Signature[] sigs, int signingVersion) {
            this.certs = certs;
            this.sigs = sigs;
            this.signatureSchemeVersion = signingVersion;
        }
    }

    public static SigningDetails verify(String apkPath, @SignatureSchemeVersion int minSignatureSchemeVersion) throws PackageParserException {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (minSignatureSchemeVersion <= 3) {
            Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "verifyV3");
            try {
                VerifiedSigner vSigner = ApkSignatureSchemeV3Verifier.verify(apkPath);
                Certificate[][] signerCerts = new Certificate[1][];
                int i = 0;
                signerCerts[0] = vSigner.certs;
                Signature[] signerSigs = convertToSignatures(signerCerts);
                Signature[] pastSignerSigs = null;
                int[] pastSignerSigsFlags = null;
                if (vSigner.por != null) {
                    pastSignerSigs = new Signature[vSigner.por.certs.size()];
                    pastSignerSigsFlags = new int[vSigner.por.flagsList.size()];
                    while (i < pastSignerSigs.length) {
                        pastSignerSigs[i] = new Signature(((X509Certificate) vSigner.por.certs.get(i)).getEncoded());
                        pastSignerSigsFlags[i] = ((Integer) vSigner.por.flagsList.get(i)).intValue();
                        i++;
                    }
                }
                SigningDetails signingDetails = new SigningDetails(signerSigs, 3, pastSignerSigs, pastSignerSigsFlags);
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                return signingDetails;
            } catch (SignatureNotFoundException e) {
                if (minSignatureSchemeVersion < 3) {
                    Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                    if (minSignatureSchemeVersion <= 2) {
                        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "verifyV2");
                        try {
                            SigningDetails signingDetails2 = new SigningDetails(convertToSignatures(ApkSignatureSchemeV2Verifier.verify(apkPath)), 2);
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            return signingDetails2;
                        } catch (SignatureNotFoundException e2) {
                            if (minSignatureSchemeVersion < 2) {
                                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                                if (minSignatureSchemeVersion <= 1) {
                                    return verifyV1Signature(apkPath, true);
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("No signature found in package of version ");
                                stringBuilder.append(minSignatureSchemeVersion);
                                stringBuilder.append(" or newer for package ");
                                stringBuilder.append(apkPath);
                                throw new PackageParserException(-103, stringBuilder.toString());
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("No APK Signature Scheme v2 signature in package ");
                            stringBuilder.append(apkPath);
                            throw new PackageParserException(-103, stringBuilder.toString(), e2);
                        } catch (Exception e3) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failed to collect certificates from ");
                            stringBuilder2.append(apkPath);
                            stringBuilder2.append(" using APK Signature Scheme v2");
                            throw new PackageParserException(-103, stringBuilder2.toString(), e3);
                        } catch (Throwable th) {
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                        }
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("No signature found in package of version ");
                        stringBuilder.append(minSignatureSchemeVersion);
                        stringBuilder.append(" or newer for package ");
                        stringBuilder.append(apkPath);
                        throw new PackageParserException(-103, stringBuilder.toString());
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("No APK Signature Scheme v3 signature in package ");
                stringBuilder.append(apkPath);
                throw new PackageParserException(-103, stringBuilder.toString(), e2);
            } catch (Exception e32) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to collect certificates from ");
                stringBuilder2.append(apkPath);
                stringBuilder2.append(" using APK Signature Scheme v3");
                throw new PackageParserException(-103, stringBuilder2.toString(), e32);
            } catch (Throwable th2) {
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No signature found in package of version ");
            stringBuilder.append(minSignatureSchemeVersion);
            stringBuilder.append(" or newer for package ");
            stringBuilder.append(apkPath);
            throw new PackageParserException(-103, stringBuilder.toString());
        }
    }

    private static SigningDetails verifyV1Signature(String apkPath, boolean verifyFull) throws PackageParserException {
        String str = apkPath;
        boolean z = verifyFull;
        StrictJarFile jarFile = null;
        StringBuilder stringBuilder;
        try {
            Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "strictJarFileCtor");
            jarFile = new StrictJarFile(str, true, z);
            List<ZipEntry> toVerify = new ArrayList();
            ZipEntry manifestEntry = jarFile.findEntry("AndroidManifest.xml");
            if (manifestEntry != null) {
                Certificate[][] lastCerts = loadCertificates(jarFile, manifestEntry);
                if (ArrayUtils.isEmpty(lastCerts)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Package ");
                    stringBuilder.append(str);
                    stringBuilder.append(" has no certificates at entry ");
                    stringBuilder.append("AndroidManifest.xml");
                    throw new PackageParserException(-103, stringBuilder.toString());
                }
                Signature[] lastSigs = convertToSignatures(lastCerts);
                if (z) {
                    Iterator<ZipEntry> i = jarFile.iterator();
                    while (i.hasNext()) {
                        ZipEntry entry = (ZipEntry) i.next();
                        if (!entry.isDirectory()) {
                            String entryName = entry.getName();
                            if (!entryName.startsWith("META-INF/")) {
                                if (!entryName.equals("AndroidManifest.xml")) {
                                    toVerify.add(entry);
                                }
                            }
                        }
                    }
                    for (ZipEntry entry2 : toVerify) {
                        Certificate[][] entryCerts = loadCertificates(jarFile, entry2);
                        if (ArrayUtils.isEmpty(entryCerts)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Package ");
                            stringBuilder.append(str);
                            stringBuilder.append(" has no certificates at entry ");
                            stringBuilder.append(entry2.getName());
                            throw new PackageParserException(-103, stringBuilder.toString());
                        } else if (!Signature.areExactMatch(lastSigs, convertToSignatures(entryCerts))) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Package ");
                            stringBuilder2.append(str);
                            stringBuilder2.append(" has mismatched certificates at entry ");
                            stringBuilder2.append(entry2.getName());
                            throw new PackageParserException(-104, stringBuilder2.toString());
                        }
                    }
                }
                SigningDetails signingDetails = new SigningDetails(lastSigs, 1);
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                closeQuietly(jarFile);
                return signingDetails;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Package ");
            stringBuilder3.append(str);
            stringBuilder3.append(" has no manifest");
            throw new PackageParserException(KeymasterDefs.KM_ERROR_VERSION_MISMATCH, stringBuilder3.toString());
        } catch (GeneralSecurityException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to collect certificates from ");
            stringBuilder.append(str);
            throw new PackageParserException(-105, stringBuilder.toString(), e);
        } catch (IOException | RuntimeException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to collect certificates from ");
            stringBuilder.append(str);
            throw new PackageParserException(-103, stringBuilder.toString(), e2);
        } catch (Throwable th) {
            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
            closeQuietly(jarFile);
        }
    }

    private static Certificate[][] loadCertificates(StrictJarFile jarFile, ZipEntry entry) throws PackageParserException {
        InputStream is = null;
        try {
            is = jarFile.getInputStream(entry);
            readFullyIgnoringContents(is);
            Certificate[][] certificateChains = jarFile.getCertificateChains(entry);
            IoUtils.closeQuietly(is);
            return certificateChains;
        } catch (IOException | RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed reading ");
            stringBuilder.append(entry.getName());
            stringBuilder.append(" in ");
            stringBuilder.append(jarFile);
            throw new PackageParserException(-102, stringBuilder.toString(), e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(is);
        }
    }

    private static void readFullyIgnoringContents(InputStream in) throws IOException {
        byte[] buffer = (byte[]) sBuffer.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[4096];
        }
        int n = 0;
        int count = 0;
        while (true) {
            int read = in.read(buffer, 0, buffer.length);
            n = read;
            if (read != -1) {
                count += n;
            } else {
                sBuffer.set(buffer);
                return;
            }
        }
    }

    public static Signature[] convertToSignatures(Certificate[][] certs) throws CertificateEncodingException {
        Signature[] res = new Signature[certs.length];
        for (int i = 0; i < certs.length; i++) {
            res[i] = new Signature(certs[i]);
        }
        return res;
    }

    private static void closeQuietly(StrictJarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (Exception e) {
            }
        }
    }

    public static SigningDetails plsCertsNoVerifyOnlyCerts(String apkPath, int minSignatureSchemeVersion) throws PackageParserException {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (minSignatureSchemeVersion <= 3) {
            Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "certsOnlyV3");
            try {
                VerifiedSigner vSigner = ApkSignatureSchemeV3Verifier.plsCertsNoVerifyOnlyCerts(apkPath);
                Signature[] signerSigs = convertToSignatures(new Certificate[][]{vSigner.certs});
                Signature[] pastSignerSigs = null;
                int[] pastSignerSigsFlags = null;
                if (vSigner.por != null) {
                    pastSignerSigs = new Signature[vSigner.por.certs.size()];
                    pastSignerSigsFlags = new int[vSigner.por.flagsList.size()];
                    for (int i = 0; i < pastSignerSigs.length; i++) {
                        pastSignerSigs[i] = new Signature(((X509Certificate) vSigner.por.certs.get(i)).getEncoded());
                        pastSignerSigsFlags[i] = ((Integer) vSigner.por.flagsList.get(i)).intValue();
                    }
                }
                SigningDetails signingDetails = new SigningDetails(signerSigs, 3, pastSignerSigs, pastSignerSigsFlags);
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                return signingDetails;
            } catch (SignatureNotFoundException e) {
                if (minSignatureSchemeVersion < 3) {
                    Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                    if (minSignatureSchemeVersion <= 2) {
                        Trace.traceBegin(Trace.TRACE_TAG_PACKAGE_MANAGER, "certsOnlyV2");
                        try {
                            SigningDetails signingDetails2 = new SigningDetails(convertToSignatures(ApkSignatureSchemeV2Verifier.plsCertsNoVerifyOnlyCerts(apkPath)), 2);
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                            return signingDetails2;
                        } catch (SignatureNotFoundException e2) {
                            if (minSignatureSchemeVersion < 2) {
                                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                                if (minSignatureSchemeVersion <= 1) {
                                    return verifyV1Signature(apkPath, false);
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("No signature found in package of version ");
                                stringBuilder.append(minSignatureSchemeVersion);
                                stringBuilder.append(" or newer for package ");
                                stringBuilder.append(apkPath);
                                throw new PackageParserException(-103, stringBuilder.toString());
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("No APK Signature Scheme v2 signature in package ");
                            stringBuilder.append(apkPath);
                            throw new PackageParserException(-103, stringBuilder.toString(), e2);
                        } catch (Exception e3) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failed to collect certificates from ");
                            stringBuilder2.append(apkPath);
                            stringBuilder2.append(" using APK Signature Scheme v2");
                            throw new PackageParserException(-103, stringBuilder2.toString(), e3);
                        } catch (Throwable th) {
                            Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
                        }
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("No signature found in package of version ");
                        stringBuilder.append(minSignatureSchemeVersion);
                        stringBuilder.append(" or newer for package ");
                        stringBuilder.append(apkPath);
                        throw new PackageParserException(-103, stringBuilder.toString());
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("No APK Signature Scheme v3 signature in package ");
                stringBuilder.append(apkPath);
                throw new PackageParserException(-103, stringBuilder.toString(), e2);
            } catch (Exception e32) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to collect certificates from ");
                stringBuilder2.append(apkPath);
                stringBuilder2.append(" using APK Signature Scheme v3");
                throw new PackageParserException(-103, stringBuilder2.toString(), e32);
            } catch (Throwable th2) {
                Trace.traceEnd(Trace.TRACE_TAG_PACKAGE_MANAGER);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No signature found in package of version ");
            stringBuilder.append(minSignatureSchemeVersion);
            stringBuilder.append(" or newer for package ");
            stringBuilder.append(apkPath);
            throw new PackageParserException(-103, stringBuilder.toString());
        }
    }

    public static byte[] getVerityRootHash(String apkPath) throws IOException, SignatureNotFoundException, SecurityException {
        try {
            return ApkSignatureSchemeV3Verifier.getVerityRootHash(apkPath);
        } catch (SignatureNotFoundException e) {
            return ApkSignatureSchemeV2Verifier.getVerityRootHash(apkPath);
        }
    }

    public static byte[] generateApkVerity(String apkPath, ByteBufferFactory bufferFactory) throws IOException, SignatureNotFoundException, SecurityException, DigestException, NoSuchAlgorithmException {
        try {
            return ApkSignatureSchemeV3Verifier.generateApkVerity(apkPath, bufferFactory);
        } catch (SignatureNotFoundException e) {
            return ApkSignatureSchemeV2Verifier.generateApkVerity(apkPath, bufferFactory);
        }
    }

    public static byte[] generateFsverityRootHash(String apkPath) throws NoSuchAlgorithmException, DigestException, IOException {
        try {
            return ApkSignatureSchemeV3Verifier.generateFsverityRootHash(apkPath);
        } catch (SignatureNotFoundException e) {
            try {
                return ApkSignatureSchemeV2Verifier.generateFsverityRootHash(apkPath);
            } catch (SignatureNotFoundException e2) {
                return null;
            }
        }
    }
}
