package com.android.org.bouncycastle.jcajce.provider.keystore.pkcs12;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.BEROctetString;
import com.android.org.bouncycastle.asn1.BEROutputStream;
import com.android.org.bouncycastle.asn1.DERBMPString;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.DEROutputStream;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERSet;
import com.android.org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import com.android.org.bouncycastle.asn1.ntt.NTTObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.AuthenticatedSafe;
import com.android.org.bouncycastle.asn1.pkcs.CertBag;
import com.android.org.bouncycastle.asn1.pkcs.ContentInfo;
import com.android.org.bouncycastle.asn1.pkcs.EncryptedData;
import com.android.org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import com.android.org.bouncycastle.asn1.pkcs.MacData;
import com.android.org.bouncycastle.asn1.pkcs.PBES2Parameters;
import com.android.org.bouncycastle.asn1.pkcs.PBKDF2Params;
import com.android.org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.Pfx;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.pkcs.SafeBag;
import com.android.org.bouncycastle.asn1.util.ASN1Dump;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import com.android.org.bouncycastle.asn1.x509.DigestInfo;
import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import com.android.org.bouncycastle.jcajce.PKCS12Key;
import com.android.org.bouncycastle.jcajce.PKCS12StoreParameter;
import com.android.org.bouncycastle.jcajce.spec.PBKDF2KeySpec;
import com.android.org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.interfaces.BCKeyStore;
import com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.jce.provider.JDKPKCS12StoreParameter;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Integers;
import com.android.org.bouncycastle.util.Strings;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class PKCS12KeyStoreSpi extends KeyStoreSpi implements PKCSObjectIdentifiers, X509ObjectIdentifiers, BCKeyStore {
    static final int CERTIFICATE = 1;
    static final int KEY = 2;
    static final int KEY_PRIVATE = 0;
    static final int KEY_PUBLIC = 1;
    static final int KEY_SECRET = 2;
    private static final int MIN_ITERATIONS = 1024;
    static final int NULL = 0;
    private static final int SALT_SIZE = 20;
    static final int SEALED = 4;
    static final int SECRET = 3;
    private static final DefaultSecretKeyProvider keySizeProvider = new DefaultSecretKeyProvider();
    private ASN1ObjectIdentifier certAlgorithm;
    private CertificateFactory certFact;
    private IgnoresCaseHashtable certs = new IgnoresCaseHashtable();
    private Hashtable chainCerts = new Hashtable();
    private final JcaJceHelper helper = new DefaultJcaJceHelper();
    private ASN1ObjectIdentifier keyAlgorithm;
    private Hashtable keyCerts = new Hashtable();
    private IgnoresCaseHashtable keys = new IgnoresCaseHashtable();
    private Hashtable localIds = new Hashtable();
    protected SecureRandom random = new SecureRandom();

    private class CertId {
        byte[] id;

        CertId(PublicKey key) {
            this.id = PKCS12KeyStoreSpi.this.createSubjectKeyId(key).getKeyIdentifier();
        }

        CertId(byte[] id) {
            this.id = id;
        }

        public int hashCode() {
            return Arrays.hashCode(this.id);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof CertId)) {
                return false;
            }
            return Arrays.areEqual(this.id, ((CertId) o).id);
        }
    }

    private static class DefaultSecretKeyProvider {
        private final Map KEY_SIZES;

        DefaultSecretKeyProvider() {
            Map keySizes = new HashMap();
            keySizes.put(new ASN1ObjectIdentifier("1.2.840.113533.7.66.10"), Integers.valueOf(128));
            keySizes.put(PKCSObjectIdentifiers.des_EDE3_CBC, Integers.valueOf(192));
            keySizes.put(NISTObjectIdentifiers.id_aes128_CBC, Integers.valueOf(128));
            keySizes.put(NISTObjectIdentifiers.id_aes192_CBC, Integers.valueOf(192));
            keySizes.put(NISTObjectIdentifiers.id_aes256_CBC, Integers.valueOf(256));
            keySizes.put(NTTObjectIdentifiers.id_camellia128_cbc, Integers.valueOf(128));
            keySizes.put(NTTObjectIdentifiers.id_camellia192_cbc, Integers.valueOf(192));
            keySizes.put(NTTObjectIdentifiers.id_camellia256_cbc, Integers.valueOf(256));
            this.KEY_SIZES = Collections.unmodifiableMap(keySizes);
        }

        public int getKeySize(AlgorithmIdentifier algorithmIdentifier) {
            Integer keySize = (Integer) this.KEY_SIZES.get(algorithmIdentifier.getAlgorithm());
            if (keySize != null) {
                return keySize.intValue();
            }
            return -1;
        }
    }

    private static class IgnoresCaseHashtable {
        private Hashtable keys;
        private Hashtable orig;

        private IgnoresCaseHashtable() {
            this.orig = new Hashtable();
            this.keys = new Hashtable();
        }

        public void put(String key, Object value) {
            String lower = key == null ? null : Strings.toLowerCase(key);
            String k = (String) this.keys.get(lower);
            if (k != null) {
                this.orig.remove(k);
            }
            this.keys.put(lower, key);
            this.orig.put(key, value);
        }

        public Enumeration keys() {
            return this.orig.keys();
        }

        public Object remove(String alias) {
            String k = (String) this.keys.remove(alias == null ? null : Strings.toLowerCase(alias));
            if (k == null) {
                return null;
            }
            return this.orig.remove(k);
        }

        public Object get(String alias) {
            String k = (String) this.keys.get(alias == null ? null : Strings.toLowerCase(alias));
            if (k == null) {
                return null;
            }
            return this.orig.get(k);
        }

        public Enumeration elements() {
            return this.orig.elements();
        }
    }

    public static class BCPKCS12KeyStore extends PKCS12KeyStoreSpi {
        public BCPKCS12KeyStore() {
            super(new BouncyCastleProvider(), pbeWithSHAAnd3_KeyTripleDES_CBC, pbeWithSHAAnd40BitRC2_CBC);
        }
    }

    public PKCS12KeyStoreSpi(Provider provider, ASN1ObjectIdentifier keyAlgorithm, ASN1ObjectIdentifier certAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
        this.certAlgorithm = certAlgorithm;
        if (provider != null) {
            try {
                this.certFact = CertificateFactory.getInstance("X.509", provider);
                return;
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("can't create cert factory - ");
                stringBuilder.append(e.toString());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        this.certFact = CertificateFactory.getInstance("X.509");
    }

    private SubjectKeyIdentifier createSubjectKeyId(PublicKey pubKey) {
        try {
            return new SubjectKeyIdentifier(getDigest(SubjectPublicKeyInfo.getInstance(pubKey.getEncoded())));
        } catch (Exception e) {
            throw new RuntimeException("error creating key");
        }
    }

    private static byte[] getDigest(SubjectPublicKeyInfo spki) {
        Digest digest = AndroidDigestFactory.getSHA1();
        byte[] resBuf = new byte[digest.getDigestSize()];
        byte[] bytes = spki.getPublicKeyData().getBytes();
        digest.update(bytes, 0, bytes.length);
        digest.doFinal(resBuf, 0);
        return resBuf;
    }

    public void setRandom(SecureRandom rand) {
        this.random = rand;
    }

    public Enumeration engineAliases() {
        Hashtable tab = new Hashtable();
        Enumeration e = this.certs.keys();
        while (e.hasMoreElements()) {
            tab.put(e.nextElement(), "cert");
        }
        e = this.keys.keys();
        while (e.hasMoreElements()) {
            String a = (String) e.nextElement();
            if (tab.get(a) == null) {
                tab.put(a, "key");
            }
        }
        return tab.keys();
    }

    public boolean engineContainsAlias(String alias) {
        return (this.certs.get(alias) == null && this.keys.get(alias) == null) ? false : true;
    }

    public void engineDeleteEntry(String alias) throws KeyStoreException {
        Key k = (Key) this.keys.remove(alias);
        Certificate c = (Certificate) this.certs.remove(alias);
        if (c != null) {
            this.chainCerts.remove(new CertId(c.getPublicKey()));
        }
        if (k != null) {
            String id = (String) this.localIds.remove(alias);
            if (id != null) {
                c = (Certificate) this.keyCerts.remove(id);
            }
            if (c != null) {
                this.chainCerts.remove(new CertId(c.getPublicKey()));
            }
        }
    }

    public Certificate engineGetCertificate(String alias) {
        if (alias != null) {
            Certificate c = (Certificate) this.certs.get(alias);
            if (c != null) {
                return c;
            }
            String id = (String) this.localIds.get(alias);
            if (id != null) {
                return (Certificate) this.keyCerts.get(id);
            }
            return (Certificate) this.keyCerts.get(alias);
        }
        throw new IllegalArgumentException("null alias passed to getCertificate.");
    }

    public String engineGetCertificateAlias(Certificate cert) {
        String ta;
        Enumeration c = this.certs.elements();
        Enumeration k = this.certs.keys();
        while (c.hasMoreElements()) {
            ta = (String) k.nextElement();
            if (((Certificate) c.nextElement()).equals(cert)) {
                return ta;
            }
        }
        c = this.keyCerts.elements();
        k = this.keyCerts.keys();
        while (c.hasMoreElements()) {
            ta = (String) k.nextElement();
            if (((Certificate) c.nextElement()).equals(cert)) {
                return ta;
            }
        }
        return null;
    }

    public Certificate[] engineGetCertificateChain(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException("null alias passed to getCertificateChain.");
        } else if (!engineIsKeyEntry(alias)) {
            return null;
        } else {
            Certificate c = engineGetCertificate(alias);
            if (c == null) {
                return null;
            }
            Vector cs = new Vector();
            while (c != null) {
                X509Certificate x509c = (X509Certificate) c;
                Certificate nextC = null;
                byte[] bytes = x509c.getExtensionValue(Extension.authorityKeyIdentifier.getId());
                if (bytes != null) {
                    try {
                        AuthorityKeyIdentifier id = AuthorityKeyIdentifier.getInstance(new ASN1InputStream(((ASN1OctetString) new ASN1InputStream(bytes).readObject()).getOctets()).readObject());
                        if (id.getKeyIdentifier() != null) {
                            nextC = (Certificate) this.chainCerts.get(new CertId(id.getKeyIdentifier()));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e.toString());
                    }
                }
                if (nextC == null) {
                    Principal i = x509c.getIssuerDN();
                    if (!i.equals(x509c.getSubjectDN())) {
                        Enumeration e2 = this.chainCerts.keys();
                        while (e2.hasMoreElements()) {
                            Certificate crt = (X509Certificate) this.chainCerts.get(e2.nextElement());
                            if (crt.getSubjectDN().equals(i)) {
                                try {
                                    x509c.verify(crt.getPublicKey());
                                    nextC = crt;
                                    break;
                                } catch (Exception e3) {
                                }
                            }
                        }
                    }
                }
                if (cs.contains(c)) {
                    c = null;
                } else {
                    cs.addElement(c);
                    if (nextC != c) {
                        c = nextC;
                    } else {
                        c = null;
                    }
                }
            }
            Certificate[] certChain = new Certificate[cs.size()];
            for (int i2 = 0; i2 != certChain.length; i2++) {
                certChain[i2] = (Certificate) cs.elementAt(i2);
            }
            return certChain;
        }
    }

    public Date engineGetCreationDate(String alias) {
        if (alias == null) {
            throw new NullPointerException("alias == null");
        } else if (this.keys.get(alias) == null && this.certs.get(alias) == null) {
            return null;
        } else {
            return new Date();
        }
    }

    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (alias != null) {
            return (Key) this.keys.get(alias);
        }
        throw new IllegalArgumentException("null alias passed to getKey.");
    }

    public boolean engineIsCertificateEntry(String alias) {
        return this.certs.get(alias) != null && this.keys.get(alias) == null;
    }

    public boolean engineIsKeyEntry(String alias) {
        return this.keys.get(alias) != null;
    }

    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        if (this.keys.get(alias) == null) {
            this.certs.put(alias, cert);
            this.chainCerts.put(new CertId(cert.getPublicKey()), cert);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("There is a key entry with the name ");
        stringBuilder.append(alias);
        stringBuilder.append(".");
        throw new KeyStoreException(stringBuilder.toString());
    }

    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
        throw new RuntimeException("operation not supported");
    }

    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
        if (!(key instanceof PrivateKey)) {
            throw new KeyStoreException("PKCS12 does not support non-PrivateKeys");
        } else if ((key instanceof PrivateKey) && chain == null) {
            throw new KeyStoreException("no certificate chain for private key");
        } else {
            if (this.keys.get(alias) != null) {
                engineDeleteEntry(alias);
            }
            this.keys.put(alias, key);
            if (chain != null) {
                int i = 0;
                this.certs.put(alias, chain[0]);
                while (true) {
                    int i2 = i;
                    if (i2 != chain.length) {
                        this.chainCerts.put(new CertId(chain[i2].getPublicKey()), chain[i2]);
                        i = i2 + 1;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public int engineSize() {
        Hashtable tab = new Hashtable();
        Enumeration e = this.certs.keys();
        while (e.hasMoreElements()) {
            tab.put(e.nextElement(), "cert");
        }
        e = this.keys.keys();
        while (e.hasMoreElements()) {
            String a = (String) e.nextElement();
            if (tab.get(a) == null) {
                tab.put(a, "key");
            }
        }
        return tab.size();
    }

    protected PrivateKey unwrapKey(AlgorithmIdentifier algId, byte[] data, char[] password, boolean wrongPKCS12Zero) throws IOException {
        ASN1ObjectIdentifier algorithm = algId.getAlgorithm();
        try {
            if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
                PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algId.getParameters());
                PBEParameterSpec defParams = new PBEParameterSpec(pbeParams.getIV(), pbeParams.getIterations().intValue());
                Cipher cipher = this.helper.createCipher(algorithm.getId());
                cipher.init(4, new PKCS12Key(password, wrongPKCS12Zero), defParams);
                return (PrivateKey) cipher.unwrap(data, "", 2);
            } else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
                return (PrivateKey) createCipher(4, password, algId).unwrap(data, "", 2);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception unwrapping private key - cannot recognise: ");
                stringBuilder.append(algorithm);
                throw new IOException(stringBuilder.toString());
            }
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("exception unwrapping private key - ");
            stringBuilder2.append(e.toString());
            throw new IOException(stringBuilder2.toString());
        }
    }

    protected byte[] wrapKey(String algorithm, Key key, PKCS12PBEParams pbeParams, char[] password) throws IOException {
        PBEKeySpec pbeSpec = new PBEKeySpec(password);
        try {
            SecretKeyFactory keyFact = this.helper.createSecretKeyFactory(algorithm);
            PBEParameterSpec defParams = new PBEParameterSpec(pbeParams.getIV(), pbeParams.getIterations().intValue());
            Cipher cipher = this.helper.createCipher(algorithm);
            cipher.init(3, keyFact.generateSecret(pbeSpec), defParams);
            return cipher.wrap(key);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception encrypting data - ");
            stringBuilder.append(e.toString());
            throw new IOException(stringBuilder.toString());
        }
    }

    protected byte[] cryptData(boolean forEncryption, AlgorithmIdentifier algId, char[] password, boolean wrongPKCS12Zero, byte[] data) throws IOException {
        ASN1ObjectIdentifier algorithm = algId.getAlgorithm();
        int mode = forEncryption ? 1 : 2;
        if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
            PKCS12PBEParams pbeParams = PKCS12PBEParams.getInstance(algId.getParameters());
            PBEKeySpec pbeSpec = new PBEKeySpec(password);
            try {
                PBEParameterSpec defParams = new PBEParameterSpec(pbeParams.getIV(), pbeParams.getIterations().intValue());
                PKCS12Key key = new PKCS12Key(password, wrongPKCS12Zero);
                Cipher cipher = this.helper.createCipher(algorithm.getId());
                cipher.init(mode, key, defParams);
                return cipher.doFinal(data);
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception decrypting data - ");
                stringBuilder.append(e.toString());
                throw new IOException(stringBuilder.toString());
            }
        } else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
            try {
                return createCipher(mode, password, algId).doFinal(data);
            } catch (Exception e2) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("exception decrypting data - ");
                stringBuilder2.append(e2.toString());
                throw new IOException(stringBuilder2.toString());
            }
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("unknown PBE algorithm: ");
            stringBuilder3.append(algorithm);
            throw new IOException(stringBuilder3.toString());
        }
    }

    private Cipher createCipher(int mode, char[] password, AlgorithmIdentifier algId) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchProviderException {
        SecretKey key;
        PBES2Parameters alg = PBES2Parameters.getInstance(algId.getParameters());
        PBKDF2Params func = PBKDF2Params.getInstance(alg.getKeyDerivationFunc().getParameters());
        AlgorithmIdentifier encScheme = AlgorithmIdentifier.getInstance(alg.getEncryptionScheme());
        SecretKeyFactory keyFact = this.helper.createSecretKeyFactory(alg.getKeyDerivationFunc().getAlgorithm().getId());
        if (func.isDefaultPrf()) {
            key = keyFact.generateSecret(new PBEKeySpec(password, func.getSalt(), func.getIterationCount().intValue(), keySizeProvider.getKeySize(encScheme)));
        } else {
            key = keyFact.generateSecret(new PBKDF2KeySpec(password, func.getSalt(), func.getIterationCount().intValue(), keySizeProvider.getKeySize(encScheme), func.getPrf()));
        }
        Cipher cipher = Cipher.getInstance(alg.getEncryptionScheme().getAlgorithm().getId());
        AlgorithmIdentifier encryptionAlg = AlgorithmIdentifier.getInstance(alg.getEncryptionScheme());
        ASN1Encodable encParams = alg.getEncryptionScheme().getParameters();
        if (encParams instanceof ASN1OctetString) {
            cipher.init(mode, key, new IvParameterSpec(ASN1OctetString.getInstance(encParams).getOctets()));
        }
        return cipher;
    }

    public void engineLoad(InputStream stream, char[] password) throws IOException {
        IOException e;
        Exception e2;
        StringBuilder stringBuilder;
        Pfx pfx;
        DigestInfo digestInfo;
        MacData macData;
        InputStream inputStream = stream;
        char[] cArr = password;
        if (inputStream != null) {
            if (cArr != null) {
                InputStream bufIn = new BufferedInputStream(inputStream);
                bufIn.mark(10);
                int head = bufIn.read();
                InputStream inputStream2;
                if (head == 48) {
                    Vector chain;
                    ContentInfo info;
                    ASN1InputStream bIn;
                    char[] info2;
                    boolean wrongPKCS12Zero;
                    int i;
                    Vector chain2;
                    String alias;
                    String alias2;
                    bufIn.reset();
                    ASN1InputStream bIn2 = new ASN1InputStream(bufIn);
                    ASN1Object obj = (ASN1Sequence) bIn2.readObject();
                    Pfx bag = Pfx.getInstance(obj);
                    ContentInfo info3 = bag.getAuthSafe();
                    Vector chain3 = new Vector();
                    boolean unmarkedKey = false;
                    boolean wrongPKCS12Zero2 = false;
                    ASN1Object algId;
                    int i2;
                    if (bag.getMacData() != null) {
                        MacData mData = bag.getMacData();
                        DigestInfo dInfo = mData.getMac();
                        ASN1Object algId2 = dInfo.getAlgorithmId();
                        byte[] salt = mData.getSalt();
                        int itCount = mData.getIterationCount().intValue();
                        byte[] data = ((ASN1OctetString) info3.getContent()).getOctets();
                        ASN1Object aSN1Object;
                        try {
                            algId = algId2;
                            DigestInfo dInfo2 = dInfo;
                            chain = chain3;
                            info = info3;
                            try {
                                byte[] res = calculatePbeMac(algId2.getAlgorithm(), salt, itCount, cArr, null, data);
                                try {
                                    AlgorithmIdentifier algId3 = dInfo2.getDigest();
                                    if (Arrays.constantTimeAreEqual(res, algId3)) {
                                        bIn = bIn2;
                                        i2 = head;
                                        inputStream2 = bufIn;
                                        info2 = cArr;
                                        aSN1Object = algId;
                                        algId = obj;
                                    } else if (cArr.length <= 0) {
                                        try {
                                            ASN1ObjectIdentifier algorithm = algId.getAlgorithm();
                                            ASN1ObjectIdentifier aSN1ObjectIdentifier = algorithm;
                                            bIn = bIn2;
                                            info2 = cArr;
                                            try {
                                                if (Arrays.constantTimeAreEqual(calculatePbeMac(aSN1ObjectIdentifier, salt, itCount, cArr, true, data), algId3)) {
                                                    wrongPKCS12Zero2 = true;
                                                } else {
                                                    throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                                                }
                                            } catch (IOException e3) {
                                                e = e3;
                                                throw e;
                                            } catch (Exception e4) {
                                                e2 = e4;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("error constructing MAC: ");
                                                stringBuilder.append(e2.toString());
                                                throw new IOException(stringBuilder.toString());
                                            }
                                        } catch (IOException e5) {
                                            e = e5;
                                            pfx = bag;
                                            algId = obj;
                                            bIn = bIn2;
                                            i2 = head;
                                            inputStream2 = bufIn;
                                            info2 = cArr;
                                            throw e;
                                        } catch (Exception e6) {
                                            e2 = e6;
                                            pfx = bag;
                                            ASN1Sequence aSN1Sequence = obj;
                                            bIn = bIn2;
                                            i2 = head;
                                            inputStream2 = bufIn;
                                            info2 = cArr;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("error constructing MAC: ");
                                            stringBuilder.append(e2.toString());
                                            throw new IOException(stringBuilder.toString());
                                        }
                                    } else {
                                        bIn = bIn2;
                                        i2 = head;
                                        inputStream2 = bufIn;
                                        info2 = cArr;
                                        aSN1Object = algId;
                                        algId = obj;
                                        throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                                    }
                                    wrongPKCS12Zero = wrongPKCS12Zero2;
                                } catch (IOException e7) {
                                    e = e7;
                                    pfx = bag;
                                    bIn = bIn2;
                                    dInfo2 = head;
                                    inputStream2 = bufIn;
                                    info2 = cArr;
                                    aSN1Object = algId;
                                    algId = obj;
                                    throw e;
                                } catch (Exception e8) {
                                    e2 = e8;
                                    pfx = bag;
                                    bIn = bIn2;
                                    i2 = head;
                                    inputStream2 = bufIn;
                                    info2 = cArr;
                                    aSN1Object = algId;
                                    algId = obj;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("error constructing MAC: ");
                                    stringBuilder.append(e2.toString());
                                    throw new IOException(stringBuilder.toString());
                                }
                            } catch (IOException e9) {
                                e = e9;
                                pfx = bag;
                                bIn = bIn2;
                                inputStream2 = bufIn;
                                info2 = cArr;
                                aSN1Object = algId;
                                digestInfo = dInfo2;
                                throw e;
                            } catch (Exception e10) {
                                e2 = e10;
                                pfx = bag;
                                bIn = bIn2;
                                inputStream2 = bufIn;
                                info2 = cArr;
                                aSN1Object = algId;
                                digestInfo = dInfo2;
                                algId = obj;
                                dInfo2 = head;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("error constructing MAC: ");
                                stringBuilder.append(e2.toString());
                                throw new IOException(stringBuilder.toString());
                            }
                        } catch (IOException e11) {
                            e = e11;
                            digestInfo = dInfo;
                            macData = mData;
                            chain = chain3;
                            info = info3;
                            pfx = bag;
                            algId = obj;
                            i2 = head;
                            inputStream2 = bufIn;
                            info3 = cArr;
                            throw e;
                        } catch (Exception e12) {
                            e2 = e12;
                            digestInfo = dInfo;
                            macData = mData;
                            chain = chain3;
                            info = info3;
                            pfx = bag;
                            algId = obj;
                            bIn = bIn2;
                            i2 = head;
                            inputStream2 = bufIn;
                            info3 = cArr;
                            aSN1Object = algId2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("error constructing MAC: ");
                            stringBuilder.append(e2.toString());
                            throw new IOException(stringBuilder.toString());
                        }
                    }
                    chain = chain3;
                    info = info3;
                    pfx = bag;
                    algId = obj;
                    bIn = bIn2;
                    i2 = head;
                    inputStream2 = bufIn;
                    info2 = cArr;
                    wrongPKCS12Zero = false;
                    this.keys = new IgnoresCaseHashtable();
                    this.localIds = new Hashtable();
                    ContentInfo info4 = info;
                    boolean z;
                    ContentInfo contentInfo;
                    ASN1InputStream bIn3;
                    if (info4.getContentType().equals(data)) {
                        ASN1InputStream bIn4 = new ASN1InputStream(((ASN1OctetString) info4.getContent()).getOctets());
                        ContentInfo[] c = AuthenticatedSafe.getInstance(bIn4.readObject()).getContentInfo();
                        i = 0;
                        while (true) {
                            int i3 = i;
                            if (i3 == c.length) {
                                head = info2;
                                z = wrongPKCS12Zero;
                                contentInfo = info4;
                                chain2 = chain;
                                break;
                            }
                            ASN1Sequence seq;
                            int j;
                            EncryptedPrivateKeyInfo eIn;
                            PKCS12BagAttributeCarrier bagAttr;
                            ASN1Sequence bIn5;
                            StringBuilder stringBuilder2;
                            if (c[i3].getContentType().equals(data)) {
                                ASN1InputStream dIn = new ASN1InputStream(((ASN1OctetString) c[i3].getContent()).getOctets());
                                seq = (ASN1Sequence) dIn.readObject();
                                j = 0;
                                while (j != seq.size()) {
                                    ASN1InputStream dIn2;
                                    ASN1Sequence seq2;
                                    SafeBag b = SafeBag.getInstance(seq.getObjectAt(j));
                                    if (b.getBagId().equals(pkcs8ShroudedKeyBag)) {
                                        EncryptedPrivateKeyInfo eIn2;
                                        eIn = EncryptedPrivateKeyInfo.getInstance(b.getBagValue());
                                        PrivateKey privKey = unwrapKey(eIn.getEncryptionAlgorithm(), eIn.getEncryptedData(), info2, wrongPKCS12Zero);
                                        bagAttr = (PKCS12BagAttributeCarrier) privKey;
                                        String alias3 = null;
                                        ASN1OctetString localId = null;
                                        if (b.getBagAttributes() != null) {
                                            head = b.getBagAttributes().getObjects();
                                            while (head.hasMoreElements()) {
                                                bIn3 = bIn4;
                                                bIn5 = (ASN1Sequence) head.nextElement();
                                                dIn2 = dIn;
                                                ASN1ObjectIdentifier aOid = (ASN1ObjectIdentifier) bIn5.getObjectAt(null);
                                                seq2 = seq;
                                                ASN1Set attrSet = (ASN1Set) bIn5.getObjectAt(1);
                                                ASN1Sequence sq;
                                                if (attrSet.size() > 0) {
                                                    sq = bIn5;
                                                    bIn4 = (ASN1Primitive) attrSet.getObjectAt(null);
                                                    attrSet = bagAttr.getBagAttribute(aOid);
                                                    ASN1Set existing;
                                                    if (attrSet != null) {
                                                        eIn2 = eIn;
                                                        if (!attrSet.toASN1Primitive().equals(bIn4)) {
                                                            existing = attrSet;
                                                            throw new IOException("attempt to add existing attribute with different value");
                                                        }
                                                    }
                                                    existing = attrSet;
                                                    eIn2 = eIn;
                                                    bagAttr.setBagAttribute(aOid, bIn4);
                                                } else {
                                                    sq = bIn5;
                                                    ASN1Set aSN1Set = attrSet;
                                                    eIn2 = eIn;
                                                    bIn4 = null;
                                                }
                                                if (aOid.equals(pkcs_9_at_friendlyName)) {
                                                    alias = ((DERBMPString) bIn4).getString();
                                                    this.keys.put(alias, privKey);
                                                    alias3 = alias;
                                                } else if (aOid.equals(pkcs_9_at_localKeyId)) {
                                                    localId = (ASN1OctetString) bIn4;
                                                }
                                                bIn4 = bIn3;
                                                dIn = dIn2;
                                                seq = seq2;
                                                eIn = eIn2;
                                            }
                                        }
                                        bIn3 = bIn4;
                                        dIn2 = dIn;
                                        seq2 = seq;
                                        eIn2 = eIn;
                                        String dIn3 = alias3;
                                        ASN1OctetString bIn6 = localId;
                                        if (bIn6 != null) {
                                            alias = new String(Hex.encode(bIn6.getOctets()));
                                            if (dIn3 == null) {
                                                this.keys.put(alias, privKey);
                                            } else {
                                                this.localIds.put(dIn3, alias);
                                            }
                                        } else {
                                            unmarkedKey = true;
                                            this.keys.put("unmarked", privKey);
                                        }
                                        chain2 = chain;
                                    } else {
                                        bIn3 = bIn4;
                                        dIn2 = dIn;
                                        seq2 = seq;
                                        if (b.getBagId().equals(certBag) != null) {
                                            chain2 = chain;
                                            chain2.addElement(b);
                                        } else {
                                            chain2 = chain;
                                            bIn4 = System.out;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("extra in data ");
                                            stringBuilder2.append(b.getBagId());
                                            bIn4.println(stringBuilder2.toString());
                                            System.out.println(ASN1Dump.dumpAsString(b));
                                        }
                                    }
                                    j++;
                                    chain = chain2;
                                    bIn4 = bIn3;
                                    dIn = dIn2;
                                    seq = seq2;
                                }
                                bIn3 = bIn4;
                                chain2 = chain;
                                head = info2;
                                z = wrongPKCS12Zero;
                                contentInfo = info4;
                            } else {
                                bIn3 = bIn4;
                                chain2 = chain;
                                if (c[i3].getContentType().equals(encryptedData) != null) {
                                    bIn4 = EncryptedData.getInstance(c[i3].getContent());
                                    head = info2;
                                    byte[] octets = cryptData(false, bIn4.getEncryptionAlgorithm(), info2, wrongPKCS12Zero, bIn4.getContent().getOctets());
                                    seq = (ASN1Sequence) ASN1Primitive.fromByteArray(octets);
                                    j = 0;
                                    while (j != seq.size()) {
                                        EncryptedData d;
                                        byte[] octets2;
                                        ASN1Sequence seq3;
                                        bIn = SafeBag.getInstance(seq.getObjectAt(j));
                                        if (bIn.getBagId().equals(certBag)) {
                                            chain2.addElement(bIn);
                                            d = bIn4;
                                            octets2 = octets;
                                            seq3 = seq;
                                            z = wrongPKCS12Zero;
                                            contentInfo = info4;
                                        } else if (bIn.getBagId().equals(pkcs8ShroudedKeyBag)) {
                                            ASN1InputStream e13;
                                            EncryptedPrivateKeyInfo eIn3;
                                            eIn = EncryptedPrivateKeyInfo.getInstance(bIn.getBagValue());
                                            PrivateKey privKey2 = unwrapKey(eIn.getEncryptionAlgorithm(), eIn.getEncryptedData(), head, wrongPKCS12Zero);
                                            bagAttr = (PKCS12BagAttributeCarrier) privKey2;
                                            d = bIn4;
                                            bIn4 = bIn.getBagAttributes().getObjects();
                                            octets2 = octets;
                                            seq3 = seq;
                                            seq = null;
                                            octets = null;
                                            while (bIn4.hasMoreElements()) {
                                                e13 = bIn4;
                                                bIn5 = (ASN1Sequence) bIn4.nextElement();
                                                eIn3 = eIn;
                                                ASN1ObjectIdentifier aOid2 = (ASN1ObjectIdentifier) bIn5.getObjectAt(null);
                                                z = wrongPKCS12Zero;
                                                ASN1Set wrongPKCS12Zero3 = (ASN1Set) bIn5.getObjectAt(true);
                                                ASN1Sequence sq2;
                                                ASN1Set attrSet2;
                                                if (wrongPKCS12Zero3.size() > 0) {
                                                    sq2 = bIn5;
                                                    bIn4 = (ASN1Primitive) wrongPKCS12Zero3.getObjectAt(null);
                                                    attrSet2 = wrongPKCS12Zero3;
                                                    wrongPKCS12Zero = bagAttr.getBagAttribute(aOid2);
                                                    ASN1Encodable existing2;
                                                    if (wrongPKCS12Zero) {
                                                        contentInfo = info4;
                                                        if (!wrongPKCS12Zero.toASN1Primitive().equals(bIn4)) {
                                                            existing2 = wrongPKCS12Zero;
                                                            throw new IOException("attempt to add existing attribute with different value");
                                                        }
                                                    }
                                                    existing2 = wrongPKCS12Zero;
                                                    contentInfo = info4;
                                                    bagAttr.setBagAttribute(aOid2, bIn4);
                                                } else {
                                                    sq2 = bIn5;
                                                    attrSet2 = wrongPKCS12Zero3;
                                                    contentInfo = info4;
                                                    bIn4 = null;
                                                }
                                                if (aOid2.equals(pkcs_9_at_friendlyName)) {
                                                    seq = ((DERBMPString) bIn4).getString();
                                                    this.keys.put(seq, privKey2);
                                                } else if (aOid2.equals(pkcs_9_at_localKeyId)) {
                                                    octets = (ASN1OctetString) bIn4;
                                                }
                                                bIn4 = e13;
                                                eIn = eIn3;
                                                wrongPKCS12Zero = z;
                                                info4 = contentInfo;
                                            }
                                            e13 = bIn4;
                                            eIn3 = eIn;
                                            z = wrongPKCS12Zero;
                                            contentInfo = info4;
                                            bIn4 = new String(Hex.encode(octets.getOctets()));
                                            if (seq == null) {
                                                this.keys.put(bIn4, privKey2);
                                            } else {
                                                this.localIds.put(seq, bIn4);
                                            }
                                        } else {
                                            d = bIn4;
                                            octets2 = octets;
                                            seq3 = seq;
                                            z = wrongPKCS12Zero;
                                            contentInfo = info4;
                                            if (bIn.getBagId().equals(keyBag) != null) {
                                                ASN1InputStream kInfo;
                                                bIn4 = PrivateKeyInfo.getInstance(bIn.getBagValue());
                                                PrivateKey privKey3 = BouncyCastleProvider.getPrivateKey(bIn4);
                                                PKCS12BagAttributeCarrier bagAttr2 = (PKCS12BagAttributeCarrier) privKey3;
                                                alias2 = null;
                                                ASN1OctetString localId2 = null;
                                                wrongPKCS12Zero = bIn.getBagAttributes().getObjects();
                                                while (wrongPKCS12Zero.hasMoreElements()) {
                                                    ASN1Sequence sq3 = ASN1Sequence.getInstance(wrongPKCS12Zero.nextElement());
                                                    kInfo = bIn4;
                                                    bIn4 = ASN1ObjectIdentifier.getInstance(sq3.getObjectAt(0));
                                                    Enumeration e14 = wrongPKCS12Zero;
                                                    wrongPKCS12Zero = ASN1Set.getInstance(sq3.getObjectAt(1));
                                                    if (wrongPKCS12Zero.size() > 0) {
                                                        ASN1Primitive attr = (ASN1Primitive) wrongPKCS12Zero.getObjectAt(null);
                                                        ASN1Encodable existing3 = bagAttr2.getBagAttribute(bIn4);
                                                        ASN1Set attrSet3;
                                                        ASN1Encodable aSN1Encodable;
                                                        if (existing3 != null) {
                                                            attrSet3 = wrongPKCS12Zero;
                                                            if (existing3.toASN1Primitive().equals(attr)) {
                                                                aSN1Encodable = existing3;
                                                            } else {
                                                                throw new IOException("attempt to add existing attribute with different value");
                                                            }
                                                        }
                                                        attrSet3 = wrongPKCS12Zero;
                                                        aSN1Encodable = existing3;
                                                        bagAttr2.setBagAttribute(bIn4, attr);
                                                        if (bIn4.equals(pkcs_9_at_friendlyName)) {
                                                            alias2 = ((DERBMPString) attr).getString();
                                                            this.keys.put(alias2, privKey3);
                                                        } else if (bIn4.equals(pkcs_9_at_localKeyId)) {
                                                            localId2 = (ASN1OctetString) attr;
                                                        }
                                                    }
                                                    bIn4 = kInfo;
                                                    wrongPKCS12Zero = e14;
                                                }
                                                kInfo = bIn4;
                                                Object obj2 = wrongPKCS12Zero;
                                                bIn4 = new String(Hex.encode(localId2.getOctets()));
                                                if (alias2 == null) {
                                                    this.keys.put(bIn4, privKey3);
                                                } else {
                                                    this.localIds.put(alias2, bIn4);
                                                }
                                            } else {
                                                bIn4 = System.out;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("extra in encryptedData ");
                                                stringBuilder2.append(bIn.getBagId());
                                                bIn4.println(stringBuilder2.toString());
                                                System.out.println(ASN1Dump.dumpAsString(bIn));
                                            }
                                        }
                                        j++;
                                        Object bIn7 = d;
                                        octets = octets2;
                                        seq = seq3;
                                        wrongPKCS12Zero = z;
                                        info4 = contentInfo;
                                    }
                                    z = wrongPKCS12Zero;
                                    contentInfo = info4;
                                } else {
                                    head = info2;
                                    z = wrongPKCS12Zero;
                                    contentInfo = info4;
                                    bIn4 = System.out;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("extra ");
                                    stringBuilder2.append(c[i3].getContentType().getId());
                                    bIn4.println(stringBuilder2.toString());
                                    bIn4 = System.out;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("extra ");
                                    stringBuilder2.append(ASN1Dump.dumpAsString(c[i3].getContent()));
                                    bIn4.println(stringBuilder2.toString());
                                }
                            }
                            i = i3 + 1;
                            chain = chain2;
                            info2 = head;
                            bIn4 = bIn3;
                            wrongPKCS12Zero = z;
                            info4 = contentInfo;
                        }
                    } else {
                        head = info2;
                        z = wrongPKCS12Zero;
                        contentInfo = info4;
                        chain2 = chain;
                        bIn3 = bIn;
                    }
                    this.certs = new IgnoresCaseHashtable();
                    this.chainCerts = new Hashtable();
                    this.keyCerts = new Hashtable();
                    int i4 = 0;
                    while (true) {
                        i = i4;
                        if (i != chain2.size()) {
                            SafeBag b2 = (SafeBag) chain2.elementAt(i);
                            CertBag cb = CertBag.getInstance(b2.getBagValue());
                            if (cb.getCertId().equals(x509Certificate)) {
                                SafeBag safeBag;
                                try {
                                    Certificate cert = this.certFact.generateCertificate(new ByteArrayInputStream(((ASN1OctetString) cb.getCertValue()).getOctets()));
                                    ASN1OctetString localId3 = null;
                                    alias2 = null;
                                    if (b2.getBagAttributes() != null) {
                                        ASN1OctetString localId4;
                                        Enumeration e15 = b2.getBagAttributes().getObjects();
                                        while (e15.hasMoreElements()) {
                                            ASN1Sequence sq4 = ASN1Sequence.getInstance(e15.nextElement());
                                            ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(sq4.getObjectAt(0));
                                            ASN1Set attrSet4 = ASN1Set.getInstance(sq4.getObjectAt(1));
                                            if (attrSet4.size() > 0) {
                                                ASN1Primitive attr2 = (ASN1Primitive) attrSet4.getObjectAt(0);
                                                if (cert instanceof PKCS12BagAttributeCarrier) {
                                                    PKCS12BagAttributeCarrier bagAttr3 = (PKCS12BagAttributeCarrier) cert;
                                                    BufferedInputStream bufIn2 = bagAttr3.getBagAttribute(oid);
                                                    if (bufIn2 != null) {
                                                        safeBag = b2;
                                                        if (bufIn2.toASN1Primitive().equals(attr2)) {
                                                            localId4 = localId3;
                                                        } else {
                                                            throw new IOException("attempt to add existing attribute with different value");
                                                        }
                                                    }
                                                    safeBag = b2;
                                                    localId4 = localId3;
                                                    bagAttr3.setBagAttribute(oid, attr2);
                                                    PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier = bagAttr3;
                                                } else {
                                                    safeBag = b2;
                                                    localId4 = localId3;
                                                }
                                                if (oid.equals(pkcs_9_at_friendlyName)) {
                                                    alias2 = ((DERBMPString) attr2).getString();
                                                } else if (oid.equals(pkcs_9_at_localKeyId)) {
                                                    localId3 = (ASN1OctetString) attr2;
                                                    b2 = safeBag;
                                                }
                                            } else {
                                                safeBag = b2;
                                                localId4 = localId3;
                                            }
                                            localId3 = localId4;
                                            b2 = safeBag;
                                        }
                                        localId4 = localId3;
                                    }
                                    this.chainCerts.put(new CertId(cert.getPublicKey()), cert);
                                    if (!unmarkedKey) {
                                        if (localId3 != null) {
                                            this.keyCerts.put(new String(Hex.encode(localId3.getOctets())), cert);
                                        }
                                        if (alias2 != null) {
                                            this.certs.put(alias2, cert);
                                        }
                                    } else if (this.keyCerts.isEmpty()) {
                                        alias = new String(Hex.encode(createSubjectKeyId(cert.getPublicKey()).getKeyIdentifier()));
                                        this.keyCerts.put(alias, cert);
                                        this.keys.put(alias, this.keys.remove("unmarked"));
                                    }
                                    i4 = i + 1;
                                } catch (Exception e22) {
                                    safeBag = b2;
                                    throw new RuntimeException(e22.toString());
                                }
                            }
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Unsupported certificate type: ");
                            stringBuilder3.append(cb.getCertId());
                            throw new RuntimeException(stringBuilder3.toString());
                        }
                        return;
                    }
                }
                inputStream2 = bufIn;
                head = cArr;
                throw new IOException("stream does not represent a PKCS12 key store");
            }
            char[] cArr2 = cArr;
            throw new NullPointerException("No password supplied for PKCS#12 KeyStore.");
        }
    }

    public void engineStore(LoadStoreParameter param) throws IOException, NoSuchAlgorithmException, CertificateException {
        if (param == null) {
            throw new IllegalArgumentException("'param' arg cannot be null");
        } else if ((param instanceof PKCS12StoreParameter) || (param instanceof JDKPKCS12StoreParameter)) {
            PKCS12StoreParameter bcParam;
            char[] password;
            if (param instanceof PKCS12StoreParameter) {
                bcParam = (PKCS12StoreParameter) param;
            } else {
                bcParam = new PKCS12StoreParameter(((JDKPKCS12StoreParameter) param).getOutputStream(), param.getProtectionParameter(), ((JDKPKCS12StoreParameter) param).isUseDEREncoding());
            }
            ProtectionParameter protParam = param.getProtectionParameter();
            if (protParam == null) {
                password = null;
            } else if (protParam instanceof PasswordProtection) {
                password = ((PasswordProtection) protParam).getPassword();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No support for protection parameter of type ");
                stringBuilder.append(protParam.getClass().getName());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            doStore(bcParam.getOutputStream(), password, bcParam.isForDEREncoding());
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No support for 'param' of type ");
            stringBuilder2.append(param.getClass().getName());
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    public void engineStore(OutputStream stream, char[] password) throws IOException {
        doStore(stream, password, false);
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x00a6  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x00b2 A:{LOOP_END, LOOP:1: B:19:0x00ac->B:21:0x00b2} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x01f6 A:{SYNTHETIC, Splitter:B:56:0x01f6} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x020d A:{Catch:{ CertificateEncodingException -> 0x0248 }} */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x0393 A:{Catch:{ CertificateEncodingException -> 0x03f7 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void doStore(OutputStream stream, char[] password, boolean useDEREncoding) throws IOException {
        CertificateEncodingException e;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        Exception e2;
        OutputStream outputStream = stream;
        char[] cArr = password;
        if (cArr != null) {
            Enumeration ks;
            ASN1EncodableVector privKey;
            ASN1EncodableVector kSeq;
            Certificate ct;
            Enumeration cs;
            String name;
            AlgorithmIdentifier cAlgId;
            PKCS12PBEParams pKCS12PBEParams;
            byte[] bArr;
            ASN1EncodableVector aSN1EncodableVector;
            Enumeration enumeration;
            byte[] bArr2;
            PKCS12BagAttributeCarrier bagAttrs;
            Enumeration e3;
            ASN1EncodableVector fSeq;
            ASN1ObjectIdentifier oid;
            Hashtable doneCerts;
            DEROutputStream asn1Out;
            ASN1EncodableVector keyS = new ASN1EncodableVector();
            Enumeration ks2 = this.keys.keys();
            while (true) {
                ks = ks2;
                if (!ks.hasMoreElements()) {
                    break;
                }
                byte[] kSalt = new byte[20];
                this.random.nextBytes(kSalt);
                String name2 = (String) ks.nextElement();
                PrivateKey privKey2 = (PrivateKey) this.keys.get(name2);
                PKCS12PBEParams kParams = new PKCS12PBEParams(kSalt, MIN_ITERATIONS);
                byte[] kBytes = wrapKey(this.keyAlgorithm.getId(), privKey2, kParams, cArr);
                EncryptedPrivateKeyInfo kInfo = new EncryptedPrivateKeyInfo(new AlgorithmIdentifier(this.keyAlgorithm, kParams.toASN1Primitive()), kBytes);
                boolean attrSet = false;
                ASN1EncodableVector kName = new ASN1EncodableVector();
                byte[] kBytes2;
                if (privKey2 instanceof PKCS12BagAttributeCarrier) {
                    DERBMPString dERBMPString;
                    PKCS12BagAttributeCarrier bagAttrs2 = (PKCS12BagAttributeCarrier) privKey2;
                    DERBMPString kSalt2 = (DERBMPString) bagAttrs2.getBagAttribute(pkcs_9_at_friendlyName);
                    if (kSalt2 != null) {
                        if (kSalt2.getString().equals(name2)) {
                            dERBMPString = kSalt2;
                            if (bagAttrs2.getBagAttribute(pkcs_9_at_localKeyId) != null) {
                                bagAttrs2.setBagAttribute(pkcs_9_at_localKeyId, createSubjectKeyId(engineGetCertificate(name2).getPublicKey()));
                            }
                            kSalt = bagAttrs2.getBagAttributeKeys();
                            while (kSalt.hasMoreElements()) {
                                ASN1ObjectIdentifier kParams2 = (ASN1ObjectIdentifier) kSalt.nextElement();
                                privKey = new ASN1EncodableVector();
                                privKey.add(kParams2);
                                Enumeration e4 = kSalt;
                                kBytes2 = kBytes;
                                privKey.add(new DERSet(bagAttrs2.getBagAttribute(kParams2)));
                                attrSet = true;
                                kName.add(new DERSequence(privKey));
                                kSalt = e4;
                                kBytes = kBytes2;
                            }
                        }
                    }
                    dERBMPString = kSalt2;
                    bagAttrs2.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(name2));
                    if (bagAttrs2.getBagAttribute(pkcs_9_at_localKeyId) != null) {
                    }
                    kSalt = bagAttrs2.getBagAttributeKeys();
                    while (kSalt.hasMoreElements()) {
                    }
                } else {
                    PKCS12PBEParams pKCS12PBEParams2 = kParams;
                    PrivateKey privateKey = privKey2;
                    kBytes2 = kBytes;
                }
                if (!attrSet) {
                    kSeq = new ASN1EncodableVector();
                    ct = engineGetCertificate(name2);
                    kSeq.add(pkcs_9_at_localKeyId);
                    kSeq.add(new DERSet(createSubjectKeyId(ct.getPublicKey())));
                    kName.add(new DERSequence(kSeq));
                    kSeq = new ASN1EncodableVector();
                    kSeq.add(pkcs_9_at_friendlyName);
                    kSeq.add(new DERSet(new DERBMPString(name2)));
                    kName.add(new DERSequence(kSeq));
                }
                keyS.add(new SafeBag(pkcs8ShroudedKeyBag, kInfo.toASN1Primitive(), new DERSet(kName)));
                ks2 = ks;
            }
            byte[] keySEncoded = new DERSequence(keyS).getEncoded(ASN1Encoding.DER);
            BEROctetString keyString = new BEROctetString(keySEncoded);
            byte[] cSalt = new byte[20];
            this.random.nextBytes(cSalt);
            ASN1EncodableVector certSeq = new ASN1EncodableVector();
            PKCS12PBEParams cParams = new PKCS12PBEParams(cSalt, MIN_ITERATIONS);
            AlgorithmIdentifier cAlgId2 = new AlgorithmIdentifier(this.certAlgorithm, cParams.toASN1Primitive());
            Hashtable doneCerts2 = new Hashtable();
            ks2 = this.keys.keys();
            while (true) {
                cs = ks2;
                if (!cs.hasMoreElements()) {
                    break;
                }
                Enumeration cs2;
                try {
                    name = (String) cs.nextElement();
                    Certificate cert = engineGetCertificate(name);
                    boolean cAttrSet = false;
                    cs2 = cs;
                    try {
                        cAlgId = cAlgId2;
                    } catch (CertificateEncodingException e5) {
                        e = e5;
                        cAlgId = cAlgId2;
                        pKCS12PBEParams = cParams;
                        bArr = cSalt;
                        aSN1EncodableVector = keyS;
                        enumeration = ks;
                        bArr2 = keySEncoded;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error encoding certificate: ");
                        stringBuilder.append(e.toString());
                        throw new IOException(stringBuilder.toString());
                    }
                    try {
                        pKCS12PBEParams = cParams;
                        bArr = cSalt;
                        Certificate cert2 = cert;
                        try {
                            CertBag cs3 = new CertBag(x509Certificate, new DEROctetString(cert2.getEncoded()));
                            privKey = new ASN1EncodableVector();
                            if (cert2 instanceof PKCS12BagAttributeCarrier) {
                                bagAttrs = (PKCS12BagAttributeCarrier) cert2;
                                DERBMPString nm = (DERBMPString) bagAttrs.getBagAttribute(pkcs_9_at_friendlyName);
                                if (nm != null) {
                                    aSN1EncodableVector = keyS;
                                    try {
                                        if (nm.getString().equals(name) != null) {
                                            DERBMPString dERBMPString2 = nm;
                                            if (bagAttrs.getBagAttribute(pkcs_9_at_localKeyId) == null) {
                                                bagAttrs.setBagAttribute(pkcs_9_at_localKeyId, createSubjectKeyId(cert2.getPublicKey()));
                                            }
                                            e3 = bagAttrs.getBagAttributeKeys();
                                            while (e3.hasMoreElements() != null) {
                                                ASN1ObjectIdentifier keyS2 = (ASN1ObjectIdentifier) e3.nextElement();
                                                Enumeration e6 = e3;
                                                fSeq = new ASN1EncodableVector();
                                                fSeq.add(keyS2);
                                                enumeration = ks;
                                                try {
                                                    bArr2 = keySEncoded;
                                                } catch (CertificateEncodingException e7) {
                                                    e = e7;
                                                    bArr2 = keySEncoded;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Error encoding certificate: ");
                                                    stringBuilder.append(e.toString());
                                                    throw new IOException(stringBuilder.toString());
                                                }
                                                try {
                                                    fSeq.add(new DERSet(bagAttrs.getBagAttribute(keyS2)));
                                                    privKey.add(new DERSequence(fSeq));
                                                    cAttrSet = true;
                                                    e3 = e6;
                                                    ks = enumeration;
                                                    keySEncoded = bArr2;
                                                } catch (CertificateEncodingException e8) {
                                                    e = e8;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("Error encoding certificate: ");
                                                    stringBuilder.append(e.toString());
                                                    throw new IOException(stringBuilder.toString());
                                                }
                                            }
                                            enumeration = ks;
                                            bArr2 = keySEncoded;
                                        }
                                    } catch (CertificateEncodingException e9) {
                                        e = e9;
                                        enumeration = ks;
                                        bArr2 = keySEncoded;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error encoding certificate: ");
                                        stringBuilder.append(e.toString());
                                        throw new IOException(stringBuilder.toString());
                                    }
                                }
                                aSN1EncodableVector = keyS;
                                try {
                                    bagAttrs.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(name));
                                    if (bagAttrs.getBagAttribute(pkcs_9_at_localKeyId) == null) {
                                    }
                                    e3 = bagAttrs.getBagAttributeKeys();
                                    while (e3.hasMoreElements() != null) {
                                    }
                                    enumeration = ks;
                                    bArr2 = keySEncoded;
                                } catch (CertificateEncodingException e10) {
                                    e = e10;
                                    enumeration = ks;
                                    bArr2 = keySEncoded;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error encoding certificate: ");
                                    stringBuilder.append(e.toString());
                                    throw new IOException(stringBuilder.toString());
                                }
                            }
                            aSN1EncodableVector = keyS;
                            enumeration = ks;
                            bArr2 = keySEncoded;
                            if (!cAttrSet) {
                                ASN1EncodableVector fSeq2 = new ASN1EncodableVector();
                                fSeq2.add(pkcs_9_at_localKeyId);
                                fSeq2.add(new DERSet(createSubjectKeyId(cert2.getPublicKey())));
                                privKey.add(new DERSequence(fSeq2));
                                fSeq2 = new ASN1EncodableVector();
                                fSeq2.add(pkcs_9_at_friendlyName);
                                fSeq2.add(new DERSet(new DERBMPString(name)));
                                privKey.add(new DERSequence(fSeq2));
                            }
                            certSeq.add(new SafeBag(certBag, cs3.toASN1Primitive(), new DERSet(privKey)));
                            doneCerts2.put(cert2, cert2);
                            ks2 = cs2;
                            cAlgId2 = cAlgId;
                            cParams = pKCS12PBEParams;
                            cSalt = bArr;
                            keyS = aSN1EncodableVector;
                            ks = enumeration;
                            keySEncoded = bArr2;
                        } catch (CertificateEncodingException e11) {
                            e = e11;
                            aSN1EncodableVector = keyS;
                            enumeration = ks;
                            bArr2 = keySEncoded;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error encoding certificate: ");
                            stringBuilder.append(e.toString());
                            throw new IOException(stringBuilder.toString());
                        }
                    } catch (CertificateEncodingException e12) {
                        e = e12;
                        pKCS12PBEParams = cParams;
                        bArr = cSalt;
                        aSN1EncodableVector = keyS;
                        enumeration = ks;
                        bArr2 = keySEncoded;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error encoding certificate: ");
                        stringBuilder.append(e.toString());
                        throw new IOException(stringBuilder.toString());
                    }
                } catch (CertificateEncodingException e13) {
                    e = e13;
                    cs2 = cs;
                    cAlgId = cAlgId2;
                    pKCS12PBEParams = cParams;
                    bArr = cSalt;
                    aSN1EncodableVector = keyS;
                    enumeration = ks;
                    bArr2 = keySEncoded;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error encoding certificate: ");
                    stringBuilder.append(e.toString());
                    throw new IOException(stringBuilder.toString());
                }
            }
            cAlgId = cAlgId2;
            pKCS12PBEParams = cParams;
            bArr = cSalt;
            aSN1EncodableVector = keyS;
            enumeration = ks;
            bArr2 = keySEncoded;
            cs = this.certs.keys();
            while (cs.hasMoreElements()) {
                Enumeration cs4;
                try {
                    name = (String) cs.nextElement();
                    Certificate cert3 = (Certificate) this.certs.get(name);
                    boolean cAttrSet2 = false;
                    if (this.keys.get(name) == null) {
                        ASN1EncodableVector cs5;
                        boolean cAttrSet3;
                        CertBag cBag = new CertBag(x509Certificate, new DEROctetString(cert3.getEncoded()));
                        fSeq = new ASN1EncodableVector();
                        if (cert3 instanceof PKCS12BagAttributeCarrier) {
                            PKCS12BagAttributeCarrier bagAttrs3 = (PKCS12BagAttributeCarrier) cert3;
                            DERBMPString nm2 = (DERBMPString) bagAttrs3.getBagAttribute(pkcs_9_at_friendlyName);
                            if (nm2 != null) {
                                try {
                                    if (nm2.getString().equals(name)) {
                                        cs4 = cs;
                                        cs = bagAttrs3.getBagAttributeKeys();
                                        while (cs.hasMoreElements()) {
                                            oid = (ASN1ObjectIdentifier) cs.nextElement();
                                            Enumeration e14 = cs;
                                            if (oid.equals(PKCSObjectIdentifiers.pkcs_9_at_localKeyId) != null) {
                                                cs = e14;
                                            } else {
                                                cs5 = new ASN1EncodableVector();
                                                cs5.add(oid);
                                                DERBMPString nm3 = nm2;
                                                cs5.add(new DERSet(bagAttrs3.getBagAttribute(oid)));
                                                fSeq.add(new DERSequence(cs5));
                                                cAttrSet2 = true;
                                                cs = e14;
                                                nm2 = nm3;
                                            }
                                        }
                                        cAttrSet3 = cAttrSet2;
                                    }
                                } catch (CertificateEncodingException e15) {
                                    e = e15;
                                    cs4 = cs;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Error encoding certificate: ");
                                    stringBuilder.append(e.toString());
                                    throw new IOException(stringBuilder.toString());
                                }
                            }
                            cs4 = cs;
                            try {
                                bagAttrs3.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(name));
                                cs = bagAttrs3.getBagAttributeKeys();
                                while (cs.hasMoreElements()) {
                                }
                                cAttrSet3 = cAttrSet2;
                            } catch (CertificateEncodingException e16) {
                                e = e16;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Error encoding certificate: ");
                                stringBuilder.append(e.toString());
                                throw new IOException(stringBuilder.toString());
                            }
                        }
                        cs4 = cs;
                        cAttrSet3 = false;
                        if (!cAttrSet3) {
                            cs5 = new ASN1EncodableVector();
                            cs5.add(pkcs_9_at_friendlyName);
                            cs5.add(new DERSet(new DERBMPString(name)));
                            fSeq.add(new DERSequence(cs5));
                        }
                        certSeq.add(new SafeBag(certBag, cBag.toASN1Primitive(), new DERSet(fSeq)));
                        doneCerts2.put(cert3, cert3);
                        cs = cs4;
                    }
                } catch (CertificateEncodingException e17) {
                    e = e17;
                    cs4 = cs;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error encoding certificate: ");
                    stringBuilder.append(e.toString());
                    throw new IOException(stringBuilder.toString());
                }
            }
            Set usedSet = getUsedCertificateSet();
            ks = this.chainCerts.keys();
            while (ks.hasMoreElements()) {
                try {
                    CertId certId = (CertId) ks.nextElement();
                    ct = (Certificate) this.chainCerts.get(certId);
                    if (usedSet.contains(ct)) {
                        if (doneCerts2.get(ct) == null) {
                            Certificate cert4;
                            CertBag cBag2 = new CertBag(x509Certificate, new DEROctetString(ct.getEncoded()));
                            ASN1EncodableVector fName = new ASN1EncodableVector();
                            if (ct instanceof PKCS12BagAttributeCarrier) {
                                bagAttrs = (PKCS12BagAttributeCarrier) ct;
                                e3 = bagAttrs.getBagAttributeKeys();
                                while (e3.hasMoreElements()) {
                                    oid = (ASN1ObjectIdentifier) e3.nextElement();
                                    CertId certId2 = certId;
                                    if (oid.equals(PKCSObjectIdentifiers.pkcs_9_at_localKeyId)) {
                                        cert4 = ct;
                                        doneCerts = doneCerts2;
                                    } else {
                                        kSeq = new ASN1EncodableVector();
                                        kSeq.add(oid);
                                        cert4 = ct;
                                        doneCerts = doneCerts2;
                                        try {
                                            kSeq.add(new DERSet(bagAttrs.getBagAttribute(oid)));
                                            fName.add(new DERSequence(kSeq));
                                        } catch (CertificateEncodingException e18) {
                                            e = e18;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Error encoding certificate: ");
                                            stringBuilder2.append(e.toString());
                                            throw new IOException(stringBuilder2.toString());
                                        }
                                    }
                                    certId = certId2;
                                    ct = cert4;
                                    doneCerts2 = doneCerts;
                                }
                            }
                            cert4 = ct;
                            doneCerts = doneCerts2;
                            certSeq.add(new SafeBag(certBag, cBag2.toASN1Primitive(), new DERSet(fName)));
                            doneCerts2 = doneCerts;
                        }
                    }
                } catch (CertificateEncodingException e19) {
                    e = e19;
                    doneCerts = doneCerts2;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error encoding certificate: ");
                    stringBuilder2.append(e.toString());
                    throw new IOException(stringBuilder2.toString());
                }
            }
            doneCerts = doneCerts2;
            AlgorithmIdentifier cAlgId3 = cAlgId;
            cSalt = cryptData(true, cAlgId3, cArr, false, new DERSequence(certSeq).getEncoded(ASN1Encoding.DER));
            EncryptedData cInfo = new EncryptedData(data, cAlgId3, new BEROctetString(cSalt));
            ContentInfo[] info = new ContentInfo[]{new ContentInfo(data, keyString), new ContentInfo(encryptedData, cInfo.toASN1Primitive())};
            AuthenticatedSafe auth = new AuthenticatedSafe(info);
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            if (useDEREncoding) {
                asn1Out = new DEROutputStream(bOut);
            } else {
                asn1Out = new BEROutputStream(bOut);
            }
            DEROutputStream asn1Out2 = asn1Out;
            asn1Out2.writeObject(auth);
            byte[] certBytes = cSalt;
            cSalt = bOut.toByteArray();
            DEROutputStream asn1Out3 = asn1Out2;
            ByteArrayOutputStream bOut2 = bOut;
            ContentInfo mainInfo = new ContentInfo(data, new BEROctetString(cSalt));
            byte[] mSalt = new byte[20];
            byte[] pkg = cSalt;
            this.random.nextBytes(mSalt);
            byte[] data = ((ASN1OctetString) mainInfo.getContent()).getOctets();
            ContentInfo mainInfo2;
            try {
                byte[] mSalt2 = mSalt;
                mainInfo2 = mainInfo;
                ASN1ObjectIdentifier aSN1ObjectIdentifier = id_SHA1;
                byte[] bArr3 = mSalt2;
                int i = MIN_ITERATIONS;
                char[] cArr2 = cArr;
                try {
                } catch (Exception e20) {
                    e2 = e20;
                    info = mSalt2;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("error constructing MAC: ");
                    stringBuilder2.append(e2.toString());
                    throw new IOException(stringBuilder2.toString());
                }
                try {
                    mSalt = new Pfx(mainInfo2, new MacData(new DigestInfo(new AlgorithmIdentifier(id_SHA1, DERNull.INSTANCE), calculatePbeMac(aSN1ObjectIdentifier, bArr3, i, cArr2, 0, data)), mSalt2, MIN_ITERATIONS));
                    if (useDEREncoding) {
                        mainInfo = new DEROutputStream(outputStream);
                    } else {
                        mainInfo = new BEROutputStream(outputStream);
                    }
                    mainInfo.writeObject(mSalt);
                    return;
                } catch (Exception e21) {
                    e2 = e21;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("error constructing MAC: ");
                    stringBuilder2.append(e2.toString());
                    throw new IOException(stringBuilder2.toString());
                }
            } catch (Exception e22) {
                e2 = e22;
                mainInfo2 = mainInfo;
                AuthenticatedSafe authenticatedSafe = auth;
                ContentInfo[] contentInfoArr = info;
                EncryptedData encryptedData = cInfo;
                int i2 = MIN_ITERATIONS;
                bArr = certBytes;
                DEROutputStream dEROutputStream = asn1Out3;
                ByteArrayOutputStream byteArrayOutputStream = bOut2;
                byte[] bArr4 = pkg;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error constructing MAC: ");
                stringBuilder2.append(e2.toString());
                throw new IOException(stringBuilder2.toString());
            }
        }
        throw new NullPointerException("No password supplied for PKCS#12 KeyStore.");
    }

    private Set getUsedCertificateSet() {
        Set usedSet = new HashSet();
        Enumeration en = this.keys.keys();
        while (en.hasMoreElements()) {
            Certificate[] certs = engineGetCertificateChain((String) en.nextElement());
            for (int i = 0; i != certs.length; i++) {
                usedSet.add(certs[i]);
            }
        }
        en = this.certs.keys();
        while (en.hasMoreElements()) {
            usedSet.add(engineGetCertificate((String) en.nextElement()));
        }
        return usedSet;
    }

    private byte[] calculatePbeMac(ASN1ObjectIdentifier oid, byte[] salt, int itCount, char[] password, boolean wrongPkcs12Zero, byte[] data) throws Exception {
        PBEParameterSpec defParams = new PBEParameterSpec(salt, itCount);
        Mac mac = this.helper.createMac(oid.getId());
        mac.init(new PKCS12Key(password, wrongPkcs12Zero), defParams);
        mac.update(data);
        return mac.doFinal();
    }
}
