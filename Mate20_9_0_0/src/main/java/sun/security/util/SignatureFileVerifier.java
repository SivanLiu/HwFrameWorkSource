package sun.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.CodeSigner;
import java.security.CryptoPrimitive;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import sun.security.jca.Providers;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

public class SignatureFileVerifier {
    private static final String ATTR_DIGEST = "-DIGEST-Manifest-Main-Attributes".toUpperCase(Locale.ENGLISH);
    private static final Set<CryptoPrimitive> DIGEST_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.MESSAGE_DIGEST));
    private static final DisabledAlgorithmConstraints JAR_DISABLED_CHECK = new DisabledAlgorithmConstraints(DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS);
    private static final Debug debug = Debug.getInstance("jar");
    private static final char[] hexc = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private PKCS7 block;
    private CertificateFactory certificateFactory;
    private HashMap<String, MessageDigest> createdDigests;
    private ManifestDigester md;
    private String name;
    private byte[] sfBytes;
    private ArrayList<CodeSigner[]> signerCache;
    private boolean workaround = false;

    public SignatureFileVerifier(ArrayList<CodeSigner[]> signerCache, ManifestDigester md, String name, byte[] rawBytes) throws IOException, CertificateException {
        Object obj = null;
        this.certificateFactory = null;
        try {
            obj = Providers.startJarVerification();
            this.block = new PKCS7(rawBytes);
            this.sfBytes = this.block.getContentInfo().getData();
            this.certificateFactory = CertificateFactory.getInstance("X509");
            this.name = name.substring(0, name.lastIndexOf(".")).toUpperCase(Locale.ENGLISH);
            this.md = md;
            this.signerCache = signerCache;
        } finally {
            Providers.stopJarVerification(obj);
        }
    }

    public boolean needSignatureFileBytes() {
        return this.sfBytes == null;
    }

    public boolean needSignatureFile(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public void setSignatureFile(byte[] sfBytes) {
        this.sfBytes = sfBytes;
    }

    public static boolean isBlockOrSF(String s) {
        if (s.endsWith(".SF") || s.endsWith(".DSA") || s.endsWith(".RSA") || s.endsWith(".EC")) {
            return true;
        }
        return false;
    }

    public static boolean isSigningRelated(String name) {
        name = name.toUpperCase(Locale.ENGLISH);
        if (!name.startsWith("META-INF/")) {
            return false;
        }
        name = name.substring(9);
        if (name.indexOf(47) != -1) {
            return false;
        }
        if (isBlockOrSF(name) || name.equals("MANIFEST.MF")) {
            return true;
        }
        if (!name.startsWith("SIG-")) {
            return false;
        }
        int extIndex = name.lastIndexOf(46);
        if (extIndex != -1) {
            String ext = name.substring(extIndex + 1);
            if (ext.length() > 3 || ext.length() < 1) {
                return false;
            }
            for (int index = 0; index < ext.length(); index++) {
                char cc = ext.charAt(index);
                if ((cc < 'A' || cc > 'Z') && (cc < '0' || cc > '9')) {
                    return false;
                }
            }
        }
        return true;
    }

    private MessageDigest getDigest(String algorithm) throws SignatureException {
        if (JAR_DISABLED_CHECK.permits(DIGEST_PRIMITIVE_SET, algorithm, null)) {
            if (this.createdDigests == null) {
                this.createdDigests = new HashMap();
            }
            MessageDigest digest = (MessageDigest) this.createdDigests.get(algorithm);
            if (digest != null) {
                return digest;
            }
            try {
                digest = MessageDigest.getInstance(algorithm);
                this.createdDigests.put(algorithm, digest);
                return digest;
            } catch (NoSuchAlgorithmException e) {
                return digest;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SignatureFile check failed. Disabled algorithm used: ");
        stringBuilder.append(algorithm);
        throw new SignatureException(stringBuilder.toString());
    }

    public void process(Hashtable<String, CodeSigner[]> signers, List<Object> manifestDigests) throws IOException, SignatureException, NoSuchAlgorithmException, JarException, CertificateException {
        Object obj = null;
        try {
            obj = Providers.startJarVerification();
            processImpl(signers, manifestDigests);
        } finally {
            Providers.stopJarVerification(obj);
        }
    }

    private void processImpl(Hashtable<String, CodeSigner[]> signers, List<Object> manifestDigests) throws IOException, SignatureException, NoSuchAlgorithmException, JarException, CertificateException {
        Manifest sf = new Manifest();
        sf.read(new ByteArrayInputStream(this.sfBytes));
        String version = sf.getMainAttributes().getValue(Name.SIGNATURE_VERSION);
        if (version != null && version.equalsIgnoreCase("1.0")) {
            SignerInfo[] infos = this.block.verify(this.sfBytes);
            if (infos != null) {
                CodeSigner[] newSigners = getSigners(infos, this.block);
                if (newSigners != null) {
                    boolean manifestSigned = verifyManifestHash(sf, this.md, manifestDigests);
                    if (manifestSigned || verifyManifestMainAttrs(sf, this.md)) {
                        for (Entry<String, Attributes> e : sf.getEntries().entrySet()) {
                            String name = (String) e.getKey();
                            Debug debug;
                            StringBuilder stringBuilder;
                            if (manifestSigned || verifySection((Attributes) e.getValue(), name, this.md)) {
                                if (name.startsWith("./")) {
                                    name = name.substring(2);
                                }
                                if (name.startsWith("/")) {
                                    name = name.substring(1);
                                }
                                updateSigners(newSigners, signers, name);
                                if (debug != null) {
                                    debug = debug;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("processSignature signed name = ");
                                    stringBuilder.append(name);
                                    debug.println(stringBuilder.toString());
                                }
                            } else if (debug != null) {
                                debug = debug;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("processSignature unsigned name = ");
                                stringBuilder.append(name);
                                debug.println(stringBuilder.toString());
                            }
                        }
                        updateSigners(newSigners, signers, JarFile.MANIFEST_NAME);
                        return;
                    }
                    throw new SecurityException("Invalid signature file digest for Manifest main attributes");
                }
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("cannot verify signature block file ");
            stringBuilder2.append(this.name);
            throw new SecurityException(stringBuilder2.toString());
        }
    }

    private boolean verifyManifestHash(Manifest sf, ManifestDigester md, List<Object> manifestDigests) throws IOException, SignatureException {
        boolean manifestSigned = false;
        for (Entry<Object, Object> se : sf.getMainAttributes().entrySet()) {
            String key = se.getKey().toString();
            if (key.toUpperCase(Locale.ENGLISH).endsWith("-DIGEST-MANIFEST")) {
                String algorithm = key.substring(null, key.length() - 16);
                manifestDigests.add(key);
                manifestDigests.add(se.getValue());
                MessageDigest digest = getDigest(algorithm);
                if (digest != null) {
                    byte[] computedHash = md.manifestDigest(digest);
                    byte[] expectedHash = Base64.getMimeDecoder().decode((String) se.getValue());
                    if (debug != null) {
                        Debug debug = debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Signature File: Manifest digest ");
                        stringBuilder.append(digest.getAlgorithm());
                        debug.println(stringBuilder.toString());
                        debug = debug;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  sigfile  ");
                        stringBuilder.append(toHex(expectedHash));
                        debug.println(stringBuilder.toString());
                        debug = debug;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  computed ");
                        stringBuilder.append(toHex(computedHash));
                        debug.println(stringBuilder.toString());
                        debug.println();
                    }
                    if (MessageDigest.isEqual(computedHash, expectedHash)) {
                        manifestSigned = true;
                    }
                }
            }
        }
        return manifestSigned;
    }

    private boolean verifyManifestMainAttrs(Manifest sf, ManifestDigester md) throws IOException, SignatureException {
        boolean attrsVerified = true;
        for (Entry<Object, Object> se : sf.getMainAttributes().entrySet()) {
            String key = se.getKey().toString();
            if (key.toUpperCase(Locale.ENGLISH).endsWith(ATTR_DIGEST)) {
                MessageDigest digest = getDigest(key.substring(0, key.length() - ATTR_DIGEST.length()));
                if (digest != null) {
                    byte[] computedHash = md.get(ManifestDigester.MF_MAIN_ATTRS, false).digest(digest);
                    byte[] expectedHash = Base64.getMimeDecoder().decode((String) se.getValue());
                    if (debug != null) {
                        Debug debug = debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Signature File: Manifest Main Attributes digest ");
                        stringBuilder.append(digest.getAlgorithm());
                        debug.println(stringBuilder.toString());
                        debug = debug;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  sigfile  ");
                        stringBuilder.append(toHex(expectedHash));
                        debug.println(stringBuilder.toString());
                        debug = debug;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  computed ");
                        stringBuilder.append(toHex(computedHash));
                        debug.println(stringBuilder.toString());
                        debug.println();
                    }
                    if (!MessageDigest.isEqual(computedHash, expectedHash)) {
                        attrsVerified = false;
                        if (debug != null) {
                            debug.println("Verification of Manifest main attributes failed");
                            debug.println();
                        }
                        return attrsVerified;
                    }
                } else {
                    continue;
                }
            }
        }
        return attrsVerified;
    }

    private boolean verifySection(Attributes sfAttr, String name, ManifestDigester md) throws IOException, SignatureException {
        String str = name;
        boolean oneDigestVerified = false;
        ManifestDigester.Entry mde = md.get(str, this.block.isOldStyle());
        if (mde == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("no manifest section for signature file entry ");
            stringBuilder.append(str);
            throw new SecurityException(stringBuilder.toString());
        } else if (sfAttr == null) {
            return false;
        } else {
            for (Entry<Object, Object> se : sfAttr.entrySet()) {
                String key = se.getKey().toString();
                if (key.toUpperCase(Locale.ENGLISH).endsWith("-DIGEST")) {
                    MessageDigest digest = getDigest(key.substring(null, key.length() - 7));
                    if (digest != null) {
                        byte[] computed;
                        boolean oneDigestVerified2;
                        Debug debug;
                        StringBuilder stringBuilder2;
                        boolean ok = false;
                        byte[] expected = Base64.getMimeDecoder().decode((String) se.getValue());
                        if (this.workaround) {
                            computed = mde.digestWorkaround(digest);
                        } else {
                            computed = mde.digest(digest);
                        }
                        if (debug != null) {
                            Debug debug2 = debug;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            oneDigestVerified2 = oneDigestVerified;
                            stringBuilder3.append("Signature Block File: ");
                            stringBuilder3.append(str);
                            stringBuilder3.append(" digest=");
                            stringBuilder3.append(digest.getAlgorithm());
                            debug2.println(stringBuilder3.toString());
                            debug = debug;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("  expected ");
                            stringBuilder2.append(toHex(expected));
                            debug.println(stringBuilder2.toString());
                            debug = debug;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("  computed ");
                            stringBuilder2.append(toHex(computed));
                            debug.println(stringBuilder2.toString());
                            debug.println();
                        } else {
                            oneDigestVerified2 = oneDigestVerified;
                        }
                        if (MessageDigest.isEqual(computed, expected)) {
                            oneDigestVerified = true;
                            ok = true;
                        } else {
                            if (!this.workaround) {
                                computed = mde.digestWorkaround(digest);
                                if (MessageDigest.isEqual(computed, expected)) {
                                    if (debug != null) {
                                        debug = debug;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("  re-computed ");
                                        stringBuilder2.append(toHex(computed));
                                        debug.println(stringBuilder2.toString());
                                        debug.println();
                                    }
                                    this.workaround = true;
                                    oneDigestVerified = true;
                                    ok = true;
                                }
                            }
                            oneDigestVerified = oneDigestVerified2;
                        }
                        if (!ok) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("invalid ");
                            stringBuilder2.append(digest.getAlgorithm());
                            stringBuilder2.append(" signature file digest for ");
                            stringBuilder2.append(str);
                            throw new SecurityException(stringBuilder2.toString());
                        }
                    }
                }
                oneDigestVerified = oneDigestVerified;
            }
            return oneDigestVerified;
        }
    }

    private CodeSigner[] getSigners(SignerInfo[] infos, PKCS7 block) throws IOException, NoSuchAlgorithmException, SignatureException, CertificateException {
        ArrayList<CodeSigner> signers = null;
        for (SignerInfo info : infos) {
            List chain = info.getCertificateChain(block);
            CertPath certChain = this.certificateFactory.generateCertPath(chain);
            if (signers == null) {
                signers = new ArrayList();
            }
            signers.add(new CodeSigner(certChain, info.getTimestamp()));
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Signature Block Certificate: ");
                stringBuilder.append(chain.get(0));
                debug.println(stringBuilder.toString());
            }
        }
        if (signers != null) {
            return (CodeSigner[]) signers.toArray(new CodeSigner[signers.size()]);
        }
        return null;
    }

    static String toHex(byte[] data) {
        StringBuffer sb = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            sb.append(hexc[(data[i] >> 4) & 15]);
            sb.append(hexc[data[i] & 15]);
        }
        return sb.toString();
    }

    static boolean contains(CodeSigner[] set, CodeSigner signer) {
        for (CodeSigner equals : set) {
            if (equals.equals(signer)) {
                return true;
            }
        }
        return false;
    }

    static boolean isSubSet(CodeSigner[] subset, CodeSigner[] set) {
        if (set == subset) {
            return true;
        }
        for (CodeSigner contains : subset) {
            if (!contains(set, contains)) {
                return false;
            }
        }
        return true;
    }

    static boolean matches(CodeSigner[] signers, CodeSigner[] oldSigners, CodeSigner[] newSigners) {
        if (oldSigners == null && signers == newSigners) {
            return true;
        }
        if ((oldSigners != null && !isSubSet(oldSigners, signers)) || !isSubSet(newSigners, signers)) {
            return false;
        }
        for (int i = 0; i < signers.length; i++) {
            boolean found = (oldSigners != null && contains(oldSigners, signers[i])) || contains(newSigners, signers[i]);
            if (!found) {
                return false;
            }
        }
        return true;
    }

    void updateSigners(CodeSigner[] newSigners, Hashtable<String, CodeSigner[]> signers, String name) {
        Object oldSigners = (CodeSigner[]) signers.get(name);
        int i = this.signerCache.size();
        while (true) {
            i--;
            if (i != -1) {
                CodeSigner[] cachedSigners = (CodeSigner[]) this.signerCache.get(i);
                if (matches(cachedSigners, oldSigners, newSigners)) {
                    signers.put(name, cachedSigners);
                    return;
                }
            } else {
                CodeSigner[] cachedSigners2;
                if (oldSigners == null) {
                    cachedSigners2 = newSigners;
                } else {
                    cachedSigners2 = new CodeSigner[(oldSigners.length + newSigners.length)];
                    System.arraycopy(oldSigners, 0, (Object) cachedSigners2, 0, oldSigners.length);
                    System.arraycopy((Object) newSigners, 0, (Object) cachedSigners2, oldSigners.length, newSigners.length);
                }
                this.signerCache.add(cachedSigners2);
                signers.put(name, cachedSigners2);
                return;
            }
        }
    }
}
