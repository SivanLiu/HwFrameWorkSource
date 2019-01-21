package android.util.jar;

import android.security.keystore.KeyProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import sun.security.jca.Providers;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

class StrictJarVerifier {
    private static final String[] DIGEST_ALGORITHMS = new String[]{KeyProperties.DIGEST_SHA512, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA256, "SHA1"};
    private static final String SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME = "X-Android-APK-Signed";
    private final Hashtable<String, Certificate[]> certificates = new Hashtable(5);
    private final String jarName;
    private final int mainAttributesEnd;
    private final StrictJarManifest manifest;
    private final HashMap<String, byte[]> metaEntries;
    private final boolean signatureSchemeRollbackProtectionsEnforced;
    private final Hashtable<String, HashMap<String, Attributes>> signatures = new Hashtable(5);
    private final Hashtable<String, Certificate[][]> verifiedEntries = new Hashtable();

    static class VerifierEntry extends OutputStream {
        private final Certificate[][] certChains;
        private final MessageDigest digest;
        private final byte[] hash;
        private final String name;
        private final Hashtable<String, Certificate[][]> verifiedEntries;

        VerifierEntry(String name, MessageDigest digest, byte[] hash, Certificate[][] certChains, Hashtable<String, Certificate[][]> verifedEntries) {
            this.name = name;
            this.digest = digest;
            this.hash = hash;
            this.certChains = certChains;
            this.verifiedEntries = verifedEntries;
        }

        public void write(int value) {
            this.digest.update((byte) value);
        }

        public void write(byte[] buf, int off, int nbytes) {
            this.digest.update(buf, off, nbytes);
        }

        void verify() {
            if (StrictJarVerifier.verifyMessageDigest(this.digest.digest(), this.hash)) {
                this.verifiedEntries.put(this.name, this.certChains);
            } else {
                throw StrictJarVerifier.invalidDigest("META-INF/MANIFEST.MF", this.name, this.name);
            }
        }
    }

    private static SecurityException invalidDigest(String signatureFile, String name, String jarName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(signatureFile);
        stringBuilder.append(" has invalid digest for ");
        stringBuilder.append(name);
        stringBuilder.append(" in ");
        stringBuilder.append(jarName);
        throw new SecurityException(stringBuilder.toString());
    }

    private static SecurityException failedVerification(String jarName, String signatureFile) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(jarName);
        stringBuilder.append(" failed verification of ");
        stringBuilder.append(signatureFile);
        throw new SecurityException(stringBuilder.toString());
    }

    private static SecurityException failedVerification(String jarName, String signatureFile, Throwable e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(jarName);
        stringBuilder.append(" failed verification of ");
        stringBuilder.append(signatureFile);
        throw new SecurityException(stringBuilder.toString(), e);
    }

    StrictJarVerifier(String name, StrictJarManifest manifest, HashMap<String, byte[]> metaEntries, boolean signatureSchemeRollbackProtectionsEnforced) {
        this.jarName = name;
        this.manifest = manifest;
        this.metaEntries = metaEntries;
        this.mainAttributesEnd = manifest.getMainAttributesEnd();
        this.signatureSchemeRollbackProtectionsEnforced = signatureSchemeRollbackProtectionsEnforced;
    }

    VerifierEntry initEntry(String name) {
        String str = name;
        if (this.manifest == null || this.signatures.isEmpty()) {
            return null;
        }
        Attributes attributes = this.manifest.getAttributes(str);
        if (attributes == null) {
            return null;
        }
        ArrayList<Certificate[]> certChains = new ArrayList();
        Iterator<Entry<String, HashMap<String, Attributes>>> it = this.signatures.entrySet().iterator();
        while (true) {
            Iterator<Entry<String, HashMap<String, Attributes>>> it2 = it;
            if (!it2.hasNext()) {
                break;
            }
            Entry<String, HashMap<String, Attributes>> entry = (Entry) it2.next();
            if (((HashMap) entry.getValue()).get(str) != null) {
                Certificate[] certChain = (Certificate[]) this.certificates.get((String) entry.getKey());
                if (certChain != null) {
                    certChains.add(certChain);
                }
            }
            it = it2;
        }
        if (certChains.isEmpty()) {
            return null;
        }
        Certificate[][] certChainsArray = (Certificate[][]) certChains.toArray(new Certificate[certChains.size()][]);
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= DIGEST_ALGORITHMS.length) {
                return null;
            }
            String algorithm = DIGEST_ALGORITHMS[i2];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(algorithm);
            stringBuilder.append("-Digest");
            String hash = attributes.getValue(stringBuilder.toString());
            if (hash != null) {
                byte[] hashBytes = hash.getBytes(StandardCharsets.ISO_8859_1);
                try {
                    VerifierEntry verifierEntry = verifierEntry;
                    try {
                        return new VerifierEntry(str, MessageDigest.getInstance(algorithm), hashBytes, certChainsArray, this.verifiedEntries);
                    } catch (NoSuchAlgorithmException e) {
                    }
                } catch (NoSuchAlgorithmException e2) {
                    String str2 = hash;
                }
            }
            i = i2 + 1;
        }
    }

    void addMetaEntry(String name, byte[] buf) {
        this.metaEntries.put(name.toUpperCase(Locale.US), buf);
    }

    synchronized boolean readCertificates() {
        if (this.metaEntries.isEmpty()) {
            return false;
        }
        Iterator<String> it = this.metaEntries.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (key.endsWith(".DSA") || key.endsWith(".RSA") || key.endsWith(".EC")) {
                verifyCertificate(key);
                it.remove();
            }
        }
        return true;
    }

    static Certificate[] verifyBytes(byte[] blockBytes, byte[] sfBytes) throws GeneralSecurityException {
        Object obj = null;
        try {
            obj = Providers.startJarVerification();
            PKCS7 block = new PKCS7(blockBytes);
            SignerInfo[] verifiedSignerInfos = block.verify(sfBytes);
            if (verifiedSignerInfos == null || verifiedSignerInfos.length == 0) {
                throw new GeneralSecurityException("Failed to verify signature: no verified SignerInfos");
            }
            List<X509Certificate> verifiedSignerCertChain = verifiedSignerInfos[null].getCertificateChain(block);
            if (verifiedSignerCertChain == null) {
                throw new GeneralSecurityException("Failed to find verified SignerInfo certificate chain");
            } else if (verifiedSignerCertChain.isEmpty()) {
                throw new GeneralSecurityException("Verified SignerInfo certificate chain is emtpy");
            } else {
                Certificate[] certificateArr = (Certificate[]) verifiedSignerCertChain.toArray(new X509Certificate[verifiedSignerCertChain.size()]);
                Providers.stopJarVerification(obj);
                return certificateArr;
            }
        } catch (IOException e) {
            throw new GeneralSecurityException("IO exception verifying jar cert", e);
        } catch (Throwable th) {
            Providers.stopJarVerification(obj);
        }
    }

    private void verifyCertificate(String certFile) {
        GeneralSecurityException e;
        String str = certFile;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str.substring(0, str.lastIndexOf(46)));
        stringBuilder.append(".SF");
        String signatureFile = stringBuilder.toString();
        byte[] sfBytes = (byte[]) this.metaEntries.get(signatureFile);
        if (sfBytes != null) {
            byte[] manifestBytes = (byte[]) this.metaEntries.get("META-INF/MANIFEST.MF");
            if (manifestBytes != null) {
                byte[] sBlockBytes = (byte[]) this.metaEntries.get(str);
                byte[] bArr;
                byte[] bArr2;
                try {
                    Certificate[] signerCertChain = verifyBytes(sBlockBytes, sfBytes);
                    if (signerCertChain != null) {
                        try {
                            this.certificates.put(signatureFile, signerCertChain);
                        } catch (GeneralSecurityException e2) {
                            e = e2;
                            bArr = sBlockBytes;
                            bArr2 = manifestBytes;
                        }
                    }
                    Attributes attributes = new Attributes();
                    HashMap<String, Attributes> entries = new HashMap();
                    HashMap<String, Attributes> entries2;
                    Attributes attributes2;
                    try {
                        String idText;
                        new StrictJarManifestReader(sfBytes, attributes).readEntries(entries, null);
                        if (this.signatureSchemeRollbackProtectionsEnforced) {
                            String apkSignatureSchemeIdList = attributes.getValue(SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME);
                            if (apkSignatureSchemeIdList != null) {
                                boolean v2SignatureGenerated = false;
                                boolean v3SignatureGenerated = false;
                                StringTokenizer tokenizer = new StringTokenizer(apkSignatureSchemeIdList, ",");
                                while (true) {
                                    StringTokenizer tokenizer2 = tokenizer;
                                    if (!tokenizer2.hasMoreTokens()) {
                                        break;
                                    }
                                    idText = tokenizer2.nextToken().trim();
                                    if (!idText.isEmpty()) {
                                        try {
                                            int id = Integer.parseInt(idText);
                                            if (id == 2) {
                                                v2SignatureGenerated = true;
                                                break;
                                            } else if (id == 3) {
                                                v3SignatureGenerated = true;
                                                break;
                                            }
                                        } catch (Exception ignored) {
                                            Exception exception = ignored;
                                        }
                                    }
                                    tokenizer = tokenizer2;
                                }
                                StringBuilder stringBuilder2;
                                if (v2SignatureGenerated) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(signatureFile);
                                    stringBuilder2.append(" indicates ");
                                    stringBuilder2.append(this.jarName);
                                    stringBuilder2.append(" is signed using APK Signature Scheme v2, but no such signature was found. Signature stripped?");
                                    throw new SecurityException(stringBuilder2.toString());
                                } else if (v3SignatureGenerated) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(signatureFile);
                                    stringBuilder2.append(" indicates ");
                                    stringBuilder2.append(this.jarName);
                                    stringBuilder2.append(" is signed using APK Signature Scheme v3, but no such signature was found. Signature stripped?");
                                    throw new SecurityException(stringBuilder2.toString());
                                }
                            }
                        }
                        if (attributes.get(Name.SIGNATURE_VERSION) != null) {
                            boolean createdBySigntool = false;
                            idText = attributes.getValue("Created-By");
                            if (idText != null) {
                                createdBySigntool = idText.indexOf("signtool") != -1;
                            }
                            if (this.mainAttributesEnd <= 0 || createdBySigntool) {
                                entries2 = entries;
                            } else {
                                entries2 = entries;
                                if (!verify(attributes, "-Digest-Manifest-Main-Attributes", manifestBytes, 0, this.mainAttributesEnd, false, 1)) {
                                    throw failedVerification(this.jarName, signatureFile);
                                }
                            }
                            if (!verify(attributes, createdBySigntool ? "-Digest" : "-Digest-Manifest", manifestBytes, 0, manifestBytes.length, false, false)) {
                                for (Entry<String, Attributes> entry : entries2.entrySet()) {
                                    Chunk chunk = this.manifest.getChunk((String) entry.getKey());
                                    if (chunk != null) {
                                        attributes2 = attributes;
                                        bArr = sBlockBytes;
                                        bArr2 = manifestBytes;
                                        if (verify((Attributes) entry.getValue(), "-Digest", manifestBytes, chunk.start, chunk.end, createdBySigntool, null)) {
                                            sBlockBytes = bArr;
                                            attributes = attributes2;
                                            manifestBytes = bArr2;
                                        } else {
                                            throw invalidDigest(signatureFile, (String) entry.getKey(), this.jarName);
                                        }
                                    }
                                    return;
                                }
                            }
                            bArr = sBlockBytes;
                            bArr2 = manifestBytes;
                            this.metaEntries.put(signatureFile, null);
                            this.signatures.put(signatureFile, entries2);
                        }
                    } catch (IOException e3) {
                        entries2 = entries;
                        attributes2 = attributes;
                        bArr = sBlockBytes;
                        bArr2 = manifestBytes;
                    }
                } catch (GeneralSecurityException e4) {
                    e = e4;
                    bArr = sBlockBytes;
                    bArr2 = manifestBytes;
                    throw failedVerification(this.jarName, signatureFile, e);
                }
            }
        }
    }

    boolean isSignedJar() {
        return this.certificates.size() > 0;
    }

    private boolean verify(Attributes attributes, String entry, byte[] data, int start, int end, boolean ignoreSecondEndline, boolean ignorable) {
        for (String algorithm : DIGEST_ALGORITHMS) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(algorithm);
            stringBuilder.append(entry);
            String hash = attributes.getValue(stringBuilder.toString());
            if (hash != null) {
                try {
                    MessageDigest md = MessageDigest.getInstance(algorithm);
                    if (ignoreSecondEndline && data[end - 1] == (byte) 10 && data[end - 2] == (byte) 10) {
                        md.update(data, start, (end - 1) - start);
                    } else {
                        md.update(data, start, end - start);
                    }
                    return verifyMessageDigest(md.digest(), hash.getBytes(StandardCharsets.ISO_8859_1));
                } catch (NoSuchAlgorithmException e) {
                }
            }
        }
        return ignorable;
    }

    private static boolean verifyMessageDigest(byte[] expected, byte[] encodedActual) {
        try {
            return MessageDigest.isEqual(expected, Base64.getDecoder().decode(encodedActual));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    Certificate[][] getCertificateChains(String name) {
        return (Certificate[][]) this.verifiedEntries.get(name);
    }

    void removeMetaEntries() {
        this.metaEntries.clear();
    }
}
