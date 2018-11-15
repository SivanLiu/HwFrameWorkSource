package org.bouncycastle.jcajce.provider.keystore.pkcs12;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
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
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BEROctetString;
import org.bouncycastle.asn1.BEROutputStream;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.ntt.NTTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.AuthenticatedSafe;
import org.bouncycastle.asn1.pkcs.CertBag;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.EncryptedData;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.MacData;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.Pfx;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.SafeBag;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.cms.CMSEnvelopedGenerator;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.jcajce.PKCS12Key;
import org.bouncycastle.jcajce.PKCS12StoreParameter;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.interfaces.BCKeyStore;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JDKPKCS12StoreParameter;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;
import org.bouncycastle.util.Properties;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

public class PKCS12KeyStoreSpi extends KeyStoreSpi implements PKCSObjectIdentifiers, X509ObjectIdentifiers, BCKeyStore {
    static final int CERTIFICATE = 1;
    static final int KEY = 2;
    static final int KEY_PRIVATE = 0;
    static final int KEY_PUBLIC = 1;
    static final int KEY_SECRET = 2;
    private static final int MIN_ITERATIONS = 51200;
    static final int NULL = 0;
    static final String PKCS12_MAX_IT_COUNT_PROPERTY = "org.bouncycastle.pkcs12.max_it_count";
    private static final int SALT_SIZE = 20;
    static final int SEALED = 4;
    static final int SECRET = 3;
    private static final DefaultSecretKeyProvider keySizeProvider = new DefaultSecretKeyProvider();
    private static Provider provider = null;
    private ASN1ObjectIdentifier certAlgorithm;
    private CertificateFactory certFact;
    private IgnoresCaseHashtable certs;
    private Hashtable chainCerts;
    private final JcaJceHelper helper;
    private int itCount;
    private ASN1ObjectIdentifier keyAlgorithm;
    private Hashtable keyCerts;
    private IgnoresCaseHashtable keys;
    private Hashtable localIds;
    private AlgorithmIdentifier macAlgorithm;
    protected SecureRandom random;
    private int saltLength;

    private class CertId {
        byte[] id;

        CertId(PublicKey publicKey) {
            this.id = PKCS12KeyStoreSpi.this.createSubjectKeyId(publicKey).getKeyIdentifier();
        }

        CertId(byte[] bArr) {
            this.id = bArr;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof CertId)) {
                return false;
            }
            return Arrays.areEqual(this.id, ((CertId) obj).id);
        }

        public int hashCode() {
            return Arrays.hashCode(this.id);
        }
    }

    private static class DefaultSecretKeyProvider {
        private final Map KEY_SIZES;

        DefaultSecretKeyProvider() {
            Map hashMap = new HashMap();
            hashMap.put(new ASN1ObjectIdentifier(CMSEnvelopedGenerator.CAST5_CBC), Integers.valueOf(128));
            hashMap.put(PKCSObjectIdentifiers.des_EDE3_CBC, Integers.valueOf(192));
            hashMap.put(NISTObjectIdentifiers.id_aes128_CBC, Integers.valueOf(128));
            hashMap.put(NISTObjectIdentifiers.id_aes192_CBC, Integers.valueOf(192));
            hashMap.put(NISTObjectIdentifiers.id_aes256_CBC, Integers.valueOf(256));
            hashMap.put(NTTObjectIdentifiers.id_camellia128_cbc, Integers.valueOf(128));
            hashMap.put(NTTObjectIdentifiers.id_camellia192_cbc, Integers.valueOf(192));
            hashMap.put(NTTObjectIdentifiers.id_camellia256_cbc, Integers.valueOf(256));
            hashMap.put(CryptoProObjectIdentifiers.gostR28147_gcfb, Integers.valueOf(256));
            this.KEY_SIZES = Collections.unmodifiableMap(hashMap);
        }

        public int getKeySize(AlgorithmIdentifier algorithmIdentifier) {
            Integer num = (Integer) this.KEY_SIZES.get(algorithmIdentifier.getAlgorithm());
            return num != null ? num.intValue() : -1;
        }
    }

    private static class IgnoresCaseHashtable {
        private Hashtable keys;
        private Hashtable orig;

        private IgnoresCaseHashtable() {
            this.orig = new Hashtable();
            this.keys = new Hashtable();
        }

        public Enumeration elements() {
            return this.orig.elements();
        }

        public Object get(String str) {
            str = (String) this.keys.get(str == null ? null : Strings.toLowerCase(str));
            return str == null ? null : this.orig.get(str);
        }

        public Enumeration keys() {
            return this.orig.keys();
        }

        public void put(String str, Object obj) {
            Object toLowerCase = str == null ? null : Strings.toLowerCase(str);
            String str2 = (String) this.keys.get(toLowerCase);
            if (str2 != null) {
                this.orig.remove(str2);
            }
            this.keys.put(toLowerCase, str);
            this.orig.put(str, obj);
        }

        public Object remove(String str) {
            str = (String) this.keys.remove(str == null ? null : Strings.toLowerCase(str));
            return str == null ? null : this.orig.remove(str);
        }
    }

    public static class BCPKCS12KeyStore3DES extends PKCS12KeyStoreSpi {
        public BCPKCS12KeyStore3DES() {
            super(PKCS12KeyStoreSpi.getBouncyCastleProvider(), pbeWithSHAAnd3_KeyTripleDES_CBC, pbeWithSHAAnd3_KeyTripleDES_CBC);
        }
    }

    public static class BCPKCS12KeyStore extends PKCS12KeyStoreSpi {
        public BCPKCS12KeyStore() {
            super(PKCS12KeyStoreSpi.getBouncyCastleProvider(), pbeWithSHAAnd3_KeyTripleDES_CBC, pbeWithSHAAnd40BitRC2_CBC);
        }
    }

    public static class DefPKCS12KeyStore3DES extends PKCS12KeyStoreSpi {
        public DefPKCS12KeyStore3DES() {
            super(null, pbeWithSHAAnd3_KeyTripleDES_CBC, pbeWithSHAAnd3_KeyTripleDES_CBC);
        }
    }

    public static class DefPKCS12KeyStore extends PKCS12KeyStoreSpi {
        public DefPKCS12KeyStore() {
            super(null, pbeWithSHAAnd3_KeyTripleDES_CBC, pbeWithSHAAnd40BitRC2_CBC);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:10:0x0061 in {3, 5, 9, 12} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public PKCS12KeyStoreSpi(java.security.Provider r4, org.bouncycastle.asn1.ASN1ObjectIdentifier r5, org.bouncycastle.asn1.ASN1ObjectIdentifier r6) {
        /*
        r3 = this;
        r3.<init>();
        r0 = new org.bouncycastle.jcajce.util.BCJcaJceHelper;
        r0.<init>();
        r3.helper = r0;
        r0 = new org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi$IgnoresCaseHashtable;
        r1 = 0;
        r0.<init>();
        r3.keys = r0;
        r0 = new java.util.Hashtable;
        r0.<init>();
        r3.localIds = r0;
        r0 = new org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi$IgnoresCaseHashtable;
        r0.<init>();
        r3.certs = r0;
        r0 = new java.util.Hashtable;
        r0.<init>();
        r3.chainCerts = r0;
        r0 = new java.util.Hashtable;
        r0.<init>();
        r3.keyCerts = r0;
        r0 = new java.security.SecureRandom;
        r0.<init>();
        r3.random = r0;
        r0 = new org.bouncycastle.asn1.x509.AlgorithmIdentifier;
        r1 = org.bouncycastle.asn1.oiw.OIWObjectIdentifiers.idSHA1;
        r2 = org.bouncycastle.asn1.DERNull.INSTANCE;
        r0.<init>(r1, r2);
        r3.macAlgorithm = r0;
        r0 = 102400; // 0x19000 float:1.43493E-40 double:5.05923E-319;
        r3.itCount = r0;
        r0 = 20;
        r3.saltLength = r0;
        r3.keyAlgorithm = r5;
        r3.certAlgorithm = r6;
        if (r4 == 0) goto L_0x005a;
    L_0x004f:
        r5 = "X.509";	 Catch:{ Exception -> 0x0058 }
        r4 = java.security.cert.CertificateFactory.getInstance(r5, r4);	 Catch:{ Exception -> 0x0058 }
    L_0x0055:
        r3.certFact = r4;	 Catch:{ Exception -> 0x0058 }
        return;	 Catch:{ Exception -> 0x0058 }
    L_0x0058:
        r4 = move-exception;	 Catch:{ Exception -> 0x0058 }
        goto L_0x0062;	 Catch:{ Exception -> 0x0058 }
    L_0x005a:
        r4 = "X.509";	 Catch:{ Exception -> 0x0058 }
        r4 = java.security.cert.CertificateFactory.getInstance(r4);	 Catch:{ Exception -> 0x0058 }
        goto L_0x0055;
        return;
    L_0x0062:
        r5 = new java.lang.IllegalArgumentException;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r0 = "can't create cert factory - ";
        r6.append(r0);
        r4 = r4.toString();
        r6.append(r4);
        r4 = r6.toString();
        r5.<init>(r4);
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi.<init>(java.security.Provider, org.bouncycastle.asn1.ASN1ObjectIdentifier, org.bouncycastle.asn1.ASN1ObjectIdentifier):void");
    }

    private byte[] calculatePbeMac(ASN1ObjectIdentifier aSN1ObjectIdentifier, byte[] bArr, int i, char[] cArr, boolean z, byte[] bArr2) throws Exception {
        AlgorithmParameterSpec pBEParameterSpec = new PBEParameterSpec(bArr, i);
        Mac createMac = this.helper.createMac(aSN1ObjectIdentifier.getId());
        createMac.init(new PKCS12Key(cArr, z), pBEParameterSpec);
        createMac.update(bArr2);
        return createMac.doFinal();
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:10:0x00b0 in {2, 3, 6, 8, 9} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private javax.crypto.Cipher createCipher(int r11, char[] r12, org.bouncycastle.asn1.x509.AlgorithmIdentifier r13) throws java.security.NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException, javax.crypto.NoSuchPaddingException, java.security.InvalidKeyException, java.security.InvalidAlgorithmParameterException, java.security.NoSuchProviderException {
        /*
        r10 = this;
        r13 = r13.getParameters();
        r13 = org.bouncycastle.asn1.pkcs.PBES2Parameters.getInstance(r13);
        r0 = r13.getKeyDerivationFunc();
        r0 = r0.getParameters();
        r0 = org.bouncycastle.asn1.pkcs.PBKDF2Params.getInstance(r0);
        r1 = r13.getEncryptionScheme();
        r1 = org.bouncycastle.asn1.x509.AlgorithmIdentifier.getInstance(r1);
        r2 = r10.helper;
        r3 = r13.getKeyDerivationFunc();
        r3 = r3.getAlgorithm();
        r3 = r3.getId();
        r2 = r2.createSecretKeyFactory(r3);
        r3 = r0.isDefaultPrf();
        if (r3 == 0) goto L_0x0050;
    L_0x0034:
        r3 = new javax.crypto.spec.PBEKeySpec;
        r4 = r0.getSalt();
        r0 = r0.getIterationCount();
        r0 = r10.validateIterationCount(r0);
        r5 = keySizeProvider;
        r1 = r5.getKeySize(r1);
        r3.<init>(r12, r4, r0, r1);
        r12 = r2.generateSecret(r3);
        goto L_0x0071;
    L_0x0050:
        r9 = new org.bouncycastle.jcajce.spec.PBKDF2KeySpec;
        r5 = r0.getSalt();
        r3 = r0.getIterationCount();
        r6 = r10.validateIterationCount(r3);
        r3 = keySizeProvider;
        r7 = r3.getKeySize(r1);
        r8 = r0.getPrf();
        r3 = r9;
        r4 = r12;
        r3.<init>(r4, r5, r6, r7, r8);
        r12 = r2.generateSecret(r9);
    L_0x0071:
        r0 = r13.getEncryptionScheme();
        r0 = r0.getAlgorithm();
        r0 = r0.getId();
        r0 = javax.crypto.Cipher.getInstance(r0);
        r13 = r13.getEncryptionScheme();
        r13 = r13.getParameters();
        r1 = r13 instanceof org.bouncycastle.asn1.ASN1OctetString;
        if (r1 == 0) goto L_0x009e;
    L_0x008d:
        r1 = new javax.crypto.spec.IvParameterSpec;
        r13 = org.bouncycastle.asn1.ASN1OctetString.getInstance(r13);
        r13 = r13.getOctets();
        r1.<init>(r13);
    L_0x009a:
        r0.init(r11, r12, r1);
        return r0;
    L_0x009e:
        r13 = org.bouncycastle.asn1.cryptopro.GOST28147Parameters.getInstance(r13);
        r1 = new org.bouncycastle.jcajce.spec.GOST28147ParameterSpec;
        r2 = r13.getEncryptionParamSet();
        r13 = r13.getIV();
        r1.<init>(r2, r13);
        goto L_0x009a;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi.createCipher(int, char[], org.bouncycastle.asn1.x509.AlgorithmIdentifier):javax.crypto.Cipher");
    }

    private SubjectKeyIdentifier createSubjectKeyId(PublicKey publicKey) {
        try {
            return new SubjectKeyIdentifier(getDigest(SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())));
        } catch (Exception e) {
            throw new RuntimeException("error creating key");
        }
    }

    private void doStore(OutputStream outputStream, char[] cArr, boolean z) throws IOException {
        StringBuilder stringBuilder;
        OutputStream outputStream2 = outputStream;
        char[] cArr2 = cArr;
        if (cArr2 != null) {
            ASN1EncodableVector aSN1EncodableVector;
            Enumeration bagAttributeKeys;
            String str;
            Certificate engineGetCertificate;
            CertBag certBag;
            ASN1EncodableVector aSN1EncodableVector2;
            Enumeration bagAttributeKeys2;
            Object obj;
            ASN1EncodableVector aSN1EncodableVector3 = new ASN1EncodableVector();
            Enumeration keys = this.keys.keys();
            while (keys.hasMoreElements()) {
                Object obj2;
                byte[] bArr = new byte[20];
                this.random.nextBytes(bArr);
                String str2 = (String) keys.nextElement();
                PrivateKey privateKey = (PrivateKey) this.keys.get(str2);
                PKCS12PBEParams pKCS12PBEParams = new PKCS12PBEParams(bArr, MIN_ITERATIONS);
                EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(new AlgorithmIdentifier(this.keyAlgorithm, pKCS12PBEParams.toASN1Primitive()), wrapKey(this.keyAlgorithm.getId(), privateKey, pKCS12PBEParams, cArr2));
                aSN1EncodableVector = new ASN1EncodableVector();
                if (privateKey instanceof PKCS12BagAttributeCarrier) {
                    PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier = (PKCS12BagAttributeCarrier) privateKey;
                    DERBMPString dERBMPString = (DERBMPString) pKCS12BagAttributeCarrier.getBagAttribute(pkcs_9_at_friendlyName);
                    if (dERBMPString == null || !dERBMPString.getString().equals(str2)) {
                        pKCS12BagAttributeCarrier.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(str2));
                    }
                    if (pKCS12BagAttributeCarrier.getBagAttribute(pkcs_9_at_localKeyId) == null) {
                        pKCS12BagAttributeCarrier.setBagAttribute(pkcs_9_at_localKeyId, createSubjectKeyId(engineGetCertificate(str2).getPublicKey()));
                    }
                    bagAttributeKeys = pKCS12BagAttributeCarrier.getBagAttributeKeys();
                    obj2 = null;
                    while (bagAttributeKeys.hasMoreElements()) {
                        ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) bagAttributeKeys.nextElement();
                        ASN1EncodableVector aSN1EncodableVector4 = new ASN1EncodableVector();
                        aSN1EncodableVector4.add(aSN1ObjectIdentifier);
                        aSN1EncodableVector4.add(new DERSet(pKCS12BagAttributeCarrier.getBagAttribute(aSN1ObjectIdentifier)));
                        aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector4));
                        obj2 = 1;
                    }
                } else {
                    obj2 = null;
                }
                if (obj2 == null) {
                    ASN1EncodableVector aSN1EncodableVector5 = new ASN1EncodableVector();
                    Certificate engineGetCertificate2 = engineGetCertificate(str2);
                    aSN1EncodableVector5.add(pkcs_9_at_localKeyId);
                    aSN1EncodableVector5.add(new DERSet(createSubjectKeyId(engineGetCertificate2.getPublicKey())));
                    aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector5));
                    aSN1EncodableVector5 = new ASN1EncodableVector();
                    aSN1EncodableVector5.add(pkcs_9_at_friendlyName);
                    aSN1EncodableVector5.add(new DERSet(new DERBMPString(str2)));
                    aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector5));
                }
                aSN1EncodableVector3.add(new SafeBag(pkcs8ShroudedKeyBag, encryptedPrivateKeyInfo.toASN1Primitive(), new DERSet(aSN1EncodableVector)));
            }
            ASN1Encodable bEROctetString = new BEROctetString(new DERSequence(aSN1EncodableVector3).getEncoded(ASN1Encoding.DER));
            byte[] bArr2 = new byte[20];
            this.random.nextBytes(bArr2);
            ASN1EncodableVector aSN1EncodableVector6 = new ASN1EncodableVector();
            AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(this.certAlgorithm, new PKCS12PBEParams(bArr2, MIN_ITERATIONS).toASN1Primitive());
            Hashtable hashtable = new Hashtable();
            Enumeration keys2 = this.keys.keys();
            while (keys2.hasMoreElements()) {
                try {
                    Enumeration enumeration;
                    str = (String) keys2.nextElement();
                    engineGetCertificate = engineGetCertificate(str);
                    certBag = new CertBag(x509Certificate, new DEROctetString(engineGetCertificate.getEncoded()));
                    aSN1EncodableVector2 = new ASN1EncodableVector();
                    if (engineGetCertificate instanceof PKCS12BagAttributeCarrier) {
                        PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier2 = (PKCS12BagAttributeCarrier) engineGetCertificate;
                        DERBMPString dERBMPString2 = (DERBMPString) pKCS12BagAttributeCarrier2.getBagAttribute(pkcs_9_at_friendlyName);
                        if (dERBMPString2 == null || !dERBMPString2.getString().equals(str)) {
                            pKCS12BagAttributeCarrier2.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(str));
                        }
                        if (pKCS12BagAttributeCarrier2.getBagAttribute(pkcs_9_at_localKeyId) == null) {
                            pKCS12BagAttributeCarrier2.setBagAttribute(pkcs_9_at_localKeyId, createSubjectKeyId(engineGetCertificate.getPublicKey()));
                        }
                        bagAttributeKeys2 = pKCS12BagAttributeCarrier2.getBagAttributeKeys();
                        obj = null;
                        while (bagAttributeKeys2.hasMoreElements()) {
                            ASN1ObjectIdentifier aSN1ObjectIdentifier2 = (ASN1ObjectIdentifier) bagAttributeKeys2.nextElement();
                            enumeration = keys2;
                            aSN1EncodableVector = new ASN1EncodableVector();
                            aSN1EncodableVector.add(aSN1ObjectIdentifier2);
                            Enumeration enumeration2 = bagAttributeKeys2;
                            aSN1EncodableVector.add(new DERSet(pKCS12BagAttributeCarrier2.getBagAttribute(aSN1ObjectIdentifier2)));
                            aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector));
                            keys2 = enumeration;
                            bagAttributeKeys2 = enumeration2;
                            obj = 1;
                        }
                        enumeration = keys2;
                    } else {
                        enumeration = keys2;
                        obj = null;
                    }
                    if (obj == null) {
                        aSN1EncodableVector = new ASN1EncodableVector();
                        aSN1EncodableVector.add(pkcs_9_at_localKeyId);
                        aSN1EncodableVector.add(new DERSet(createSubjectKeyId(engineGetCertificate.getPublicKey())));
                        aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector));
                        aSN1EncodableVector = new ASN1EncodableVector();
                        aSN1EncodableVector.add(pkcs_9_at_friendlyName);
                        aSN1EncodableVector.add(new DERSet(new DERBMPString(str)));
                        aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector));
                    }
                    aSN1EncodableVector6.add(new SafeBag(certBag, certBag.toASN1Primitive(), new DERSet(aSN1EncodableVector2)));
                    hashtable.put(engineGetCertificate, engineGetCertificate);
                    keys2 = enumeration;
                } catch (CertificateEncodingException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error encoding certificate: ");
                    stringBuilder.append(e.toString());
                    throw new IOException(stringBuilder.toString());
                }
            }
            keys2 = this.certs.keys();
            while (keys2.hasMoreElements()) {
                try {
                    str = (String) keys2.nextElement();
                    engineGetCertificate = (Certificate) this.certs.get(str);
                    if (this.keys.get(str) == null) {
                        Enumeration enumeration3;
                        certBag = new CertBag(x509Certificate, new DEROctetString(engineGetCertificate.getEncoded()));
                        aSN1EncodableVector2 = new ASN1EncodableVector();
                        if (engineGetCertificate instanceof PKCS12BagAttributeCarrier) {
                            PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier3 = (PKCS12BagAttributeCarrier) engineGetCertificate;
                            DERBMPString dERBMPString3 = (DERBMPString) pKCS12BagAttributeCarrier3.getBagAttribute(pkcs_9_at_friendlyName);
                            if (dERBMPString3 == null || !dERBMPString3.getString().equals(str)) {
                                pKCS12BagAttributeCarrier3.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(str));
                            }
                            Enumeration bagAttributeKeys3 = pKCS12BagAttributeCarrier3.getBagAttributeKeys();
                            obj = null;
                            while (bagAttributeKeys3.hasMoreElements()) {
                                enumeration3 = keys2;
                                ASN1ObjectIdentifier aSN1ObjectIdentifier3 = (ASN1ObjectIdentifier) bagAttributeKeys3.nextElement();
                                Enumeration enumeration4 = bagAttributeKeys3;
                                if (aSN1ObjectIdentifier3.equals(PKCSObjectIdentifiers.pkcs_9_at_localKeyId)) {
                                    keys2 = enumeration3;
                                    bagAttributeKeys3 = enumeration4;
                                } else {
                                    ASN1EncodableVector aSN1EncodableVector7 = new ASN1EncodableVector();
                                    aSN1EncodableVector7.add(aSN1ObjectIdentifier3);
                                    aSN1EncodableVector7.add(new DERSet(pKCS12BagAttributeCarrier3.getBagAttribute(aSN1ObjectIdentifier3)));
                                    aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector7));
                                    keys2 = enumeration3;
                                    bagAttributeKeys3 = enumeration4;
                                    obj = 1;
                                }
                            }
                            enumeration3 = keys2;
                        } else {
                            enumeration3 = keys2;
                            obj = null;
                        }
                        if (obj == null) {
                            aSN1EncodableVector = new ASN1EncodableVector();
                            aSN1EncodableVector.add(pkcs_9_at_friendlyName);
                            aSN1EncodableVector.add(new DERSet(new DERBMPString(str)));
                            aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector));
                        }
                        aSN1EncodableVector6.add(new SafeBag(certBag, certBag.toASN1Primitive(), new DERSet(aSN1EncodableVector2)));
                        hashtable.put(engineGetCertificate, engineGetCertificate);
                        keys2 = enumeration3;
                    }
                } catch (CertificateEncodingException e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error encoding certificate: ");
                    stringBuilder.append(e2.toString());
                    throw new IOException(stringBuilder.toString());
                }
            }
            Set usedCertificateSet = getUsedCertificateSet();
            bagAttributeKeys = this.chainCerts.keys();
            while (bagAttributeKeys.hasMoreElements()) {
                try {
                    engineGetCertificate = (Certificate) this.chainCerts.get((CertId) bagAttributeKeys.nextElement());
                    if (usedCertificateSet.contains(engineGetCertificate)) {
                        if (hashtable.get(engineGetCertificate) == null) {
                            Hashtable hashtable2;
                            certBag = new CertBag(x509Certificate, new DEROctetString(engineGetCertificate.getEncoded()));
                            aSN1EncodableVector2 = new ASN1EncodableVector();
                            if (engineGetCertificate instanceof PKCS12BagAttributeCarrier) {
                                PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier4 = (PKCS12BagAttributeCarrier) engineGetCertificate;
                                bagAttributeKeys2 = pKCS12BagAttributeCarrier4.getBagAttributeKeys();
                                while (bagAttributeKeys2.hasMoreElements()) {
                                    ASN1ObjectIdentifier aSN1ObjectIdentifier4 = (ASN1ObjectIdentifier) bagAttributeKeys2.nextElement();
                                    if (!aSN1ObjectIdentifier4.equals(PKCSObjectIdentifiers.pkcs_9_at_localKeyId)) {
                                        ASN1EncodableVector aSN1EncodableVector8 = new ASN1EncodableVector();
                                        aSN1EncodableVector8.add(aSN1ObjectIdentifier4);
                                        hashtable2 = hashtable;
                                        aSN1EncodableVector8.add(new DERSet(pKCS12BagAttributeCarrier4.getBagAttribute(aSN1ObjectIdentifier4)));
                                        aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector8));
                                        hashtable = hashtable2;
                                    }
                                }
                            }
                            hashtable2 = hashtable;
                            aSN1EncodableVector6.add(new SafeBag(certBag, certBag.toASN1Primitive(), new DERSet(aSN1EncodableVector2)));
                            hashtable = hashtable2;
                        }
                    }
                } catch (CertificateEncodingException e22) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error encoding certificate: ");
                    stringBuilder.append(e22.toString());
                    throw new IOException(stringBuilder.toString());
                }
            }
            EncryptedData encryptedData = new EncryptedData(data, algorithmIdentifier, new BEROctetString(cryptData(true, algorithmIdentifier, cArr2, false, new DERSequence(aSN1EncodableVector6).getEncoded(ASN1Encoding.DER))));
            ASN1Encodable authenticatedSafe = new AuthenticatedSafe(new ContentInfo[]{new ContentInfo(data, bEROctetString), new ContentInfo(encryptedData, encryptedData.toASN1Primitive())});
            OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            (z ? new DEROutputStream(byteArrayOutputStream) : new BEROutputStream(byteArrayOutputStream)).writeObject(authenticatedSafe);
            ContentInfo contentInfo = new ContentInfo(data, new BEROctetString(byteArrayOutputStream.toByteArray()));
            byte[] bArr3 = new byte[this.saltLength];
            this.random.nextBytes(bArr3);
            try {
                (z ? new DEROutputStream(outputStream2) : new BEROutputStream(outputStream2)).writeObject(new Pfx(contentInfo, new MacData(new DigestInfo(this.macAlgorithm, calculatePbeMac(this.macAlgorithm.getAlgorithm(), bArr3, this.itCount, cArr2, false, ((ASN1OctetString) contentInfo.getContent()).getOctets())), bArr3, this.itCount)));
                return;
            } catch (Exception e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("error constructing MAC: ");
                stringBuilder.append(e3.toString());
                throw new IOException(stringBuilder.toString());
            }
        }
        throw new NullPointerException("No password supplied for PKCS#12 KeyStore.");
    }

    private static synchronized Provider getBouncyCastleProvider() {
        synchronized (PKCS12KeyStoreSpi.class) {
            Provider provider;
            if (Security.getProvider("BC") != null) {
                provider = Security.getProvider("BC");
                return provider;
            }
            if (provider == null) {
                provider = new BouncyCastleProvider();
            }
            provider = provider;
            return provider;
        }
    }

    private static byte[] getDigest(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        Digest createSHA1 = DigestFactory.createSHA1();
        byte[] bArr = new byte[createSHA1.getDigestSize()];
        byte[] bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        createSHA1.update(bytes, 0, bytes.length);
        createSHA1.doFinal(bArr, 0);
        return bArr;
    }

    private Set getUsedCertificateSet() {
        Set hashSet = new HashSet();
        Enumeration keys = this.keys.keys();
        while (keys.hasMoreElements()) {
            Certificate[] engineGetCertificateChain = engineGetCertificateChain((String) keys.nextElement());
            for (int i = 0; i != engineGetCertificateChain.length; i++) {
                hashSet.add(engineGetCertificateChain[i]);
            }
        }
        keys = this.certs.keys();
        while (keys.hasMoreElements()) {
            hashSet.add(engineGetCertificate((String) keys.nextElement()));
        }
        return hashSet;
    }

    private int validateIterationCount(BigInteger bigInteger) {
        int intValue = bigInteger.intValue();
        if (intValue >= 0) {
            BigInteger asBigInteger = Properties.asBigInteger(PKCS12_MAX_IT_COUNT_PROPERTY);
            if (asBigInteger == null || asBigInteger.intValue() >= intValue) {
                return intValue;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iteration count ");
            stringBuilder.append(intValue);
            stringBuilder.append(" greater than ");
            stringBuilder.append(asBigInteger.intValue());
            throw new IllegalStateException(stringBuilder.toString());
        }
        throw new IllegalStateException("negative iteration count found");
    }

    protected byte[] cryptData(boolean z, AlgorithmIdentifier algorithmIdentifier, char[] cArr, boolean z2, byte[] bArr) throws IOException {
        StringBuilder stringBuilder;
        ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();
        int i = z ? 1 : 2;
        if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
            PKCS12PBEParams instance = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());
            try {
                AlgorithmParameterSpec pBEParameterSpec = new PBEParameterSpec(instance.getIV(), instance.getIterations().intValue());
                Key pKCS12Key = new PKCS12Key(cArr, z2);
                Cipher createCipher = this.helper.createCipher(algorithm.getId());
                createCipher.init(i, pKCS12Key, pBEParameterSpec);
                return createCipher.doFinal(bArr);
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception decrypting data - ");
                stringBuilder.append(e.toString());
                throw new IOException(stringBuilder.toString());
            }
        } else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
            try {
                return createCipher(i, cArr, algorithmIdentifier).doFinal(bArr);
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception decrypting data - ");
                stringBuilder.append(e2.toString());
                throw new IOException(stringBuilder.toString());
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unknown PBE algorithm: ");
            stringBuilder2.append(algorithm);
            throw new IOException(stringBuilder2.toString());
        }
    }

    public Enumeration engineAliases() {
        Hashtable hashtable = new Hashtable();
        Enumeration keys = this.certs.keys();
        while (keys.hasMoreElements()) {
            hashtable.put(keys.nextElement(), "cert");
        }
        keys = this.keys.keys();
        while (keys.hasMoreElements()) {
            String str = (String) keys.nextElement();
            if (hashtable.get(str) == null) {
                hashtable.put(str, "key");
            }
        }
        return hashtable.keys();
    }

    public boolean engineContainsAlias(String str) {
        return (this.certs.get(str) == null && this.keys.get(str) == null) ? false : true;
    }

    public void engineDeleteEntry(String str) throws KeyStoreException {
        Key key = (Key) this.keys.remove(str);
        Certificate certificate = (Certificate) this.certs.remove(str);
        if (certificate != null) {
            this.chainCerts.remove(new CertId(certificate.getPublicKey()));
        }
        if (key != null) {
            str = (String) this.localIds.remove(str);
            if (str != null) {
                certificate = (Certificate) this.keyCerts.remove(str);
            }
            if (certificate != null) {
                this.chainCerts.remove(new CertId(certificate.getPublicKey()));
            }
        }
    }

    public Certificate engineGetCertificate(String str) {
        if (str != null) {
            Certificate certificate = (Certificate) this.certs.get(str);
            if (certificate != null) {
                return certificate;
            }
            String str2 = (String) this.localIds.get(str);
            return (Certificate) (str2 != null ? this.keyCerts.get(str2) : this.keyCerts.get(str));
        }
        throw new IllegalArgumentException("null alias passed to getCertificate.");
    }

    public String engineGetCertificateAlias(Certificate certificate) {
        String str;
        Enumeration elements = this.certs.elements();
        Enumeration keys = this.certs.keys();
        while (elements.hasMoreElements()) {
            str = (String) keys.nextElement();
            if (((Certificate) elements.nextElement()).equals(certificate)) {
                return str;
            }
        }
        elements = this.keyCerts.elements();
        keys = this.keyCerts.keys();
        while (elements.hasMoreElements()) {
            str = (String) keys.nextElement();
            if (((Certificate) elements.nextElement()).equals(certificate)) {
                return str;
            }
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00ac  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Certificate[] engineGetCertificateChain(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null alias passed to getCertificateChain.");
        } else if (!engineIsKeyEntry(str)) {
            return null;
        } else {
            Certificate engineGetCertificate = engineGetCertificate(str);
            if (engineGetCertificate == null) {
                return null;
            }
            Vector vector = new Vector();
            while (engineGetCertificate != null) {
                Certificate certificate;
                X509Certificate x509Certificate = (X509Certificate) engineGetCertificate;
                byte[] extensionValue = x509Certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
                if (extensionValue != null) {
                    try {
                        AuthorityKeyIdentifier instance = AuthorityKeyIdentifier.getInstance(new ASN1InputStream(((ASN1OctetString) new ASN1InputStream(extensionValue).readObject()).getOctets()).readObject());
                        if (instance.getKeyIdentifier() != null) {
                            certificate = (Certificate) this.chainCerts.get(new CertId(instance.getKeyIdentifier()));
                            if (certificate == null) {
                                Principal issuerDN = x509Certificate.getIssuerDN();
                                if (!issuerDN.equals(x509Certificate.getSubjectDN())) {
                                    Enumeration keys = this.chainCerts.keys();
                                    while (keys.hasMoreElements()) {
                                        Certificate certificate2 = (X509Certificate) this.chainCerts.get(keys.nextElement());
                                        if (certificate2.getSubjectDN().equals(issuerDN)) {
                                            try {
                                                x509Certificate.verify(certificate2.getPublicKey());
                                                certificate = certificate2;
                                                break;
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                }
                            }
                            if (!vector.contains(engineGetCertificate)) {
                                vector.addElement(engineGetCertificate);
                                if (certificate != engineGetCertificate) {
                                    engineGetCertificate = certificate;
                                }
                            }
                            engineGetCertificate = null;
                        }
                    } catch (IOException e2) {
                        throw new RuntimeException(e2.toString());
                    }
                }
                certificate = null;
                if (certificate == null) {
                }
                if (vector.contains(engineGetCertificate)) {
                }
                engineGetCertificate = null;
            }
            Certificate[] certificateArr = new Certificate[vector.size()];
            for (int i = 0; i != certificateArr.length; i++) {
                certificateArr[i] = (Certificate) vector.elementAt(i);
            }
            return certificateArr;
        }
    }

    public Date engineGetCreationDate(String str) {
        if (str != null) {
            return (this.keys.get(str) == null && this.certs.get(str) == null) ? null : new Date();
        } else {
            throw new NullPointerException("alias == null");
        }
    }

    public Key engineGetKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (str != null) {
            return (Key) this.keys.get(str);
        }
        throw new IllegalArgumentException("null alias passed to getKey.");
    }

    public boolean engineIsCertificateEntry(String str) {
        return this.certs.get(str) != null && this.keys.get(str) == null;
    }

    public boolean engineIsKeyEntry(String str) {
        return this.keys.get(str) != null;
    }

    public void engineLoad(InputStream inputStream, char[] cArr) throws IOException {
        StringBuilder stringBuilder;
        InputStream inputStream2 = inputStream;
        char[] cArr2 = cArr;
        if (inputStream2 != null) {
            if (cArr2 != null) {
                InputStream bufferedInputStream = new BufferedInputStream(inputStream2);
                bufferedInputStream.mark(10);
                if (bufferedInputStream.read() == 48) {
                    bufferedInputStream.reset();
                    try {
                        boolean z;
                        int i;
                        SafeBag instance;
                        Enumeration objects;
                        PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier;
                        ASN1Encodable bagAttribute;
                        String str;
                        Pfx instance2 = Pfx.getInstance(new ASN1InputStream(bufferedInputStream).readObject());
                        ContentInfo authSafe = instance2.getAuthSafe();
                        Vector vector = new Vector();
                        int i2 = 1;
                        int i3 = 0;
                        if (instance2.getMacData() != null) {
                            MacData macData = instance2.getMacData();
                            DigestInfo mac = macData.getMac();
                            this.macAlgorithm = mac.getAlgorithmId();
                            byte[] salt = macData.getSalt();
                            this.itCount = validateIterationCount(macData.getIterationCount());
                            this.saltLength = salt.length;
                            byte[] octets = ((ASN1OctetString) authSafe.getContent()).getOctets();
                            try {
                                boolean z2;
                                byte[] calculatePbeMac = calculatePbeMac(this.macAlgorithm.getAlgorithm(), salt, this.itCount, cArr2, false, octets);
                                byte[] digest = mac.getDigest();
                                if (Arrays.constantTimeAreEqual(calculatePbeMac, digest)) {
                                    z2 = false;
                                } else if (cArr2.length <= 0) {
                                    if (Arrays.constantTimeAreEqual(calculatePbeMac(this.macAlgorithm.getAlgorithm(), salt, this.itCount, cArr2, true, octets), digest)) {
                                        z2 = true;
                                    } else {
                                        throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                                    }
                                } else {
                                    throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                                }
                                z = z2;
                            } catch (IOException e) {
                                throw e;
                            } catch (Exception e2) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("error constructing MAC: ");
                                stringBuilder.append(e2.toString());
                                throw new IOException(stringBuilder.toString());
                            }
                        }
                        z = false;
                        AnonymousClass1 anonymousClass1 = null;
                        this.keys = new IgnoresCaseHashtable();
                        this.localIds = new Hashtable();
                        if (authSafe.getContentType().equals(data)) {
                            ContentInfo[] contentInfo = AuthenticatedSafe.getInstance(new ASN1InputStream(((ASN1OctetString) authSafe.getContent()).getOctets()).readObject()).getContentInfo();
                            int i4 = 0;
                            i = i4;
                            while (i4 != contentInfo.length) {
                                ASN1Sequence aSN1Sequence;
                                int i5;
                                EncryptedPrivateKeyInfo instance3;
                                PrivateKey unwrapKey;
                                ASN1OctetString aSN1OctetString;
                                Object obj;
                                ASN1Encodable bagAttribute2;
                                String str2;
                                PrintStream printStream;
                                StringBuilder stringBuilder2;
                                if (contentInfo[i4].getContentType().equals(data)) {
                                    aSN1Sequence = (ASN1Sequence) new ASN1InputStream(((ASN1OctetString) contentInfo[i4].getContent()).getOctets()).readObject();
                                    i5 = i3;
                                    while (i5 != aSN1Sequence.size()) {
                                        ASN1Sequence aSN1Sequence2;
                                        instance = SafeBag.getInstance(aSN1Sequence.getObjectAt(i5));
                                        if (instance.getBagId().equals(pkcs8ShroudedKeyBag)) {
                                            instance3 = EncryptedPrivateKeyInfo.getInstance(instance.getBagValue());
                                            unwrapKey = unwrapKey(instance3.getEncryptionAlgorithm(), instance3.getEncryptedData(), cArr2, z);
                                            if (instance.getBagAttributes() != null) {
                                                objects = instance.getBagAttributes().getObjects();
                                                aSN1OctetString = anonymousClass1;
                                                obj = aSN1OctetString;
                                                while (objects.hasMoreElements()) {
                                                    ASN1Encodable aSN1Encodable;
                                                    ASN1Sequence aSN1Sequence3 = (ASN1Sequence) objects.nextElement();
                                                    ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) aSN1Sequence3.getObjectAt(i3);
                                                    ASN1Set aSN1Set = (ASN1Set) aSN1Sequence3.getObjectAt(i2);
                                                    if (aSN1Set.size() > 0) {
                                                        aSN1Encodable = (ASN1Primitive) aSN1Set.getObjectAt(0);
                                                        if (unwrapKey instanceof PKCS12BagAttributeCarrier) {
                                                            pKCS12BagAttributeCarrier = (PKCS12BagAttributeCarrier) unwrapKey;
                                                            aSN1Sequence2 = aSN1Sequence;
                                                            bagAttribute2 = pKCS12BagAttributeCarrier.getBagAttribute(aSN1ObjectIdentifier);
                                                            if (bagAttribute2 == null) {
                                                                pKCS12BagAttributeCarrier.setBagAttribute(aSN1ObjectIdentifier, aSN1Encodable);
                                                            } else if (!bagAttribute2.toASN1Primitive().equals(aSN1Encodable)) {
                                                                throw new IOException("attempt to add existing attribute with different value");
                                                            }
                                                        }
                                                        aSN1Sequence2 = aSN1Sequence;
                                                    } else {
                                                        aSN1Sequence2 = aSN1Sequence;
                                                        aSN1Encodable = null;
                                                    }
                                                    if (aSN1ObjectIdentifier.equals(pkcs_9_at_friendlyName)) {
                                                        obj = ((DERBMPString) aSN1Encodable).getString();
                                                        this.keys.put(obj, unwrapKey);
                                                    } else if (aSN1ObjectIdentifier.equals(pkcs_9_at_localKeyId)) {
                                                        aSN1OctetString = (ASN1OctetString) aSN1Encodable;
                                                    }
                                                    aSN1Sequence = aSN1Sequence2;
                                                    i2 = 1;
                                                    i3 = 0;
                                                }
                                                aSN1Sequence2 = aSN1Sequence;
                                            } else {
                                                aSN1Sequence2 = aSN1Sequence;
                                                aSN1OctetString = null;
                                                obj = null;
                                            }
                                            if (aSN1OctetString != null) {
                                                str2 = new String(Hex.encode(aSN1OctetString.getOctets()));
                                                if (obj == null) {
                                                    this.keys.put(str2, unwrapKey);
                                                } else {
                                                    this.localIds.put(obj, str2);
                                                }
                                            } else {
                                                this.keys.put("unmarked", unwrapKey);
                                                i = 1;
                                            }
                                        } else {
                                            aSN1Sequence2 = aSN1Sequence;
                                            if (instance.getBagId().equals(certBag)) {
                                                vector.addElement(instance);
                                            } else {
                                                printStream = System.out;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("extra in data ");
                                                stringBuilder2.append(instance.getBagId());
                                                printStream.println(stringBuilder2.toString());
                                                System.out.println(ASN1Dump.dumpAsString(instance));
                                            }
                                        }
                                        i5++;
                                        aSN1Sequence = aSN1Sequence2;
                                        i2 = 1;
                                        i3 = 0;
                                        anonymousClass1 = null;
                                    }
                                    continue;
                                } else if (contentInfo[i4].getContentType().equals(encryptedData)) {
                                    EncryptedData instance4 = EncryptedData.getInstance(contentInfo[i4].getContent());
                                    aSN1Sequence = (ASN1Sequence) ASN1Primitive.fromByteArray(cryptData(false, instance4.getEncryptionAlgorithm(), cArr2, z, instance4.getContent().getOctets()));
                                    i5 = 0;
                                    while (i5 != aSN1Sequence.size()) {
                                        ASN1Sequence aSN1Sequence4;
                                        instance = SafeBag.getInstance(aSN1Sequence.getObjectAt(i5));
                                        ASN1ObjectIdentifier aSN1ObjectIdentifier2;
                                        if (instance.getBagId().equals(certBag)) {
                                            vector.addElement(instance);
                                            aSN1Sequence4 = aSN1Sequence;
                                        } else if (instance.getBagId().equals(pkcs8ShroudedKeyBag)) {
                                            instance3 = EncryptedPrivateKeyInfo.getInstance(instance.getBagValue());
                                            unwrapKey = unwrapKey(instance3.getEncryptionAlgorithm(), instance3.getEncryptedData(), cArr2, z);
                                            PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier2 = (PKCS12BagAttributeCarrier) unwrapKey;
                                            objects = instance.getBagAttributes().getObjects();
                                            ASN1OctetString aSN1OctetString2 = null;
                                            Object obj2 = null;
                                            while (objects.hasMoreElements()) {
                                                ASN1Sequence aSN1Sequence5 = (ASN1Sequence) objects.nextElement();
                                                aSN1ObjectIdentifier2 = (ASN1ObjectIdentifier) aSN1Sequence5.getObjectAt(0);
                                                aSN1Sequence4 = aSN1Sequence;
                                                ASN1Set aSN1Set2 = (ASN1Set) aSN1Sequence5.getObjectAt(1);
                                                if (aSN1Set2.size() > 0) {
                                                    bagAttribute2 = (ASN1Primitive) aSN1Set2.getObjectAt(0);
                                                    bagAttribute = pKCS12BagAttributeCarrier2.getBagAttribute(aSN1ObjectIdentifier2);
                                                    if (bagAttribute == null) {
                                                        pKCS12BagAttributeCarrier2.setBagAttribute(aSN1ObjectIdentifier2, bagAttribute2);
                                                    } else if (!bagAttribute.toASN1Primitive().equals(bagAttribute2)) {
                                                        throw new IOException("attempt to add existing attribute with different value");
                                                    }
                                                }
                                                bagAttribute2 = null;
                                                if (aSN1ObjectIdentifier2.equals(pkcs_9_at_friendlyName)) {
                                                    str2 = ((DERBMPString) bagAttribute2).getString();
                                                    this.keys.put(str2, unwrapKey);
                                                    obj2 = str2;
                                                } else if (aSN1ObjectIdentifier2.equals(pkcs_9_at_localKeyId)) {
                                                    aSN1OctetString2 = (ASN1OctetString) bagAttribute2;
                                                }
                                                aSN1Sequence = aSN1Sequence4;
                                            }
                                            aSN1Sequence4 = aSN1Sequence;
                                            str2 = new String(Hex.encode(aSN1OctetString2.getOctets()));
                                            if (obj2 == null) {
                                                this.keys.put(str2, unwrapKey);
                                            } else {
                                                this.localIds.put(obj2, str2);
                                            }
                                        } else {
                                            aSN1Sequence4 = aSN1Sequence;
                                            if (instance.getBagId().equals(keyBag)) {
                                                PrivateKey privateKey = BouncyCastleProvider.getPrivateKey(PrivateKeyInfo.getInstance(instance.getBagValue()));
                                                PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier3 = (PKCS12BagAttributeCarrier) privateKey;
                                                objects = instance.getBagAttributes().getObjects();
                                                aSN1OctetString = null;
                                                obj = null;
                                                while (objects.hasMoreElements()) {
                                                    ASN1Sequence instance5 = ASN1Sequence.getInstance(objects.nextElement());
                                                    aSN1ObjectIdentifier2 = ASN1ObjectIdentifier.getInstance(instance5.getObjectAt(0));
                                                    ASN1Set instance6 = ASN1Set.getInstance(instance5.getObjectAt(1));
                                                    if (instance6.size() > 0) {
                                                        ASN1Primitive aSN1Primitive = (ASN1Primitive) instance6.getObjectAt(0);
                                                        bagAttribute = pKCS12BagAttributeCarrier3.getBagAttribute(aSN1ObjectIdentifier2);
                                                        if (bagAttribute == null) {
                                                            pKCS12BagAttributeCarrier3.setBagAttribute(aSN1ObjectIdentifier2, aSN1Primitive);
                                                        } else if (!bagAttribute.toASN1Primitive().equals(aSN1Primitive)) {
                                                            throw new IOException("attempt to add existing attribute with different value");
                                                        }
                                                        if (aSN1ObjectIdentifier2.equals(pkcs_9_at_friendlyName)) {
                                                            obj = ((DERBMPString) aSN1Primitive).getString();
                                                            this.keys.put(obj, privateKey);
                                                        } else if (aSN1ObjectIdentifier2.equals(pkcs_9_at_localKeyId)) {
                                                            aSN1OctetString = (ASN1OctetString) aSN1Primitive;
                                                        }
                                                    }
                                                }
                                                str = new String(Hex.encode(aSN1OctetString.getOctets()));
                                                if (obj == null) {
                                                    this.keys.put(str, privateKey);
                                                } else {
                                                    this.localIds.put(obj, str);
                                                }
                                            } else {
                                                printStream = System.out;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("extra in encryptedData ");
                                                stringBuilder2.append(instance.getBagId());
                                                printStream.println(stringBuilder2.toString());
                                                System.out.println(ASN1Dump.dumpAsString(instance));
                                            }
                                        }
                                        i5++;
                                        aSN1Sequence = aSN1Sequence4;
                                    }
                                    continue;
                                } else {
                                    printStream = System.out;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("extra ");
                                    stringBuilder.append(contentInfo[i4].getContentType().getId());
                                    printStream.println(stringBuilder.toString());
                                    printStream = System.out;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("extra ");
                                    stringBuilder.append(ASN1Dump.dumpAsString(contentInfo[i4].getContent()));
                                    printStream.println(stringBuilder.toString());
                                }
                                i4++;
                                i2 = 1;
                                i3 = 0;
                                anonymousClass1 = null;
                            }
                        } else {
                            i = 0;
                        }
                        this.certs = new IgnoresCaseHashtable();
                        this.chainCerts = new Hashtable();
                        this.keyCerts = new Hashtable();
                        int i6 = 0;
                        while (i6 != vector.size()) {
                            instance = (SafeBag) vector.elementAt(i6);
                            CertBag instance7 = CertBag.getInstance(instance.getBagValue());
                            if (instance7.getCertId().equals(x509Certificate)) {
                                try {
                                    String str3;
                                    ASN1OctetString aSN1OctetString3;
                                    Certificate generateCertificate = this.certFact.generateCertificate(new ByteArrayInputStream(((ASN1OctetString) instance7.getCertValue()).getOctets()));
                                    if (instance.getBagAttributes() != null) {
                                        objects = instance.getBagAttributes().getObjects();
                                        str3 = null;
                                        aSN1OctetString3 = str3;
                                        while (objects.hasMoreElements()) {
                                            ASN1Sequence instance8 = ASN1Sequence.getInstance(objects.nextElement());
                                            ASN1ObjectIdentifier instance9 = ASN1ObjectIdentifier.getInstance(instance8.getObjectAt(0));
                                            ASN1Set instance10 = ASN1Set.getInstance(instance8.getObjectAt(1));
                                            if (instance10.size() > 0) {
                                                ASN1Primitive aSN1Primitive2 = (ASN1Primitive) instance10.getObjectAt(0);
                                                if (generateCertificate instanceof PKCS12BagAttributeCarrier) {
                                                    pKCS12BagAttributeCarrier = (PKCS12BagAttributeCarrier) generateCertificate;
                                                    bagAttribute = pKCS12BagAttributeCarrier.getBagAttribute(instance9);
                                                    if (bagAttribute == null) {
                                                        pKCS12BagAttributeCarrier.setBagAttribute(instance9, aSN1Primitive2);
                                                    } else if (!bagAttribute.toASN1Primitive().equals(aSN1Primitive2)) {
                                                        throw new IOException("attempt to add existing attribute with different value");
                                                    }
                                                }
                                                if (instance9.equals(pkcs_9_at_friendlyName)) {
                                                    str3 = ((DERBMPString) aSN1Primitive2).getString();
                                                } else if (instance9.equals(pkcs_9_at_localKeyId)) {
                                                    aSN1OctetString3 = (ASN1OctetString) aSN1Primitive2;
                                                }
                                            }
                                        }
                                    } else {
                                        str3 = null;
                                        aSN1OctetString3 = str3;
                                    }
                                    this.chainCerts.put(new CertId(generateCertificate.getPublicKey()), generateCertificate);
                                    if (i == 0) {
                                        if (aSN1OctetString3 != null) {
                                            this.keyCerts.put(new String(Hex.encode(aSN1OctetString3.getOctets())), generateCertificate);
                                        }
                                        if (str3 != null) {
                                            this.certs.put(str3, generateCertificate);
                                        }
                                    } else if (this.keyCerts.isEmpty()) {
                                        str = new String(Hex.encode(createSubjectKeyId(generateCertificate.getPublicKey()).getKeyIdentifier()));
                                        this.keyCerts.put(str, generateCertificate);
                                        this.keys.put(str, this.keys.remove("unmarked"));
                                    }
                                    i6++;
                                } catch (Exception e22) {
                                    throw new RuntimeException(e22.toString());
                                }
                            }
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Unsupported certificate type: ");
                            stringBuilder3.append(instance7.getCertId());
                            throw new RuntimeException(stringBuilder3.toString());
                        }
                        return;
                    } catch (Exception e222) {
                        throw new IOException(e222.getMessage());
                    }
                }
                throw new IOException("stream does not represent a PKCS12 key store");
            }
            throw new NullPointerException("No password supplied for PKCS#12 KeyStore.");
        }
    }

    public void engineSetCertificateEntry(String str, Certificate certificate) throws KeyStoreException {
        if (this.keys.get(str) == null) {
            this.certs.put(str, certificate);
            this.chainCerts.put(new CertId(certificate.getPublicKey()), certificate);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("There is a key entry with the name ");
        stringBuilder.append(str);
        stringBuilder.append(".");
        throw new KeyStoreException(stringBuilder.toString());
    }

    public void engineSetKeyEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) throws KeyStoreException {
        boolean z = key instanceof PrivateKey;
        if (!z) {
            throw new KeyStoreException("PKCS12 does not support non-PrivateKeys");
        } else if (z && certificateArr == null) {
            throw new KeyStoreException("no certificate chain for private key");
        } else {
            if (this.keys.get(str) != null) {
                engineDeleteEntry(str);
            }
            this.keys.put(str, key);
            if (certificateArr != null) {
                int i = 0;
                this.certs.put(str, certificateArr[0]);
                while (i != certificateArr.length) {
                    this.chainCerts.put(new CertId(certificateArr[i].getPublicKey()), certificateArr[i]);
                    i++;
                }
            }
        }
    }

    public void engineSetKeyEntry(String str, byte[] bArr, Certificate[] certificateArr) throws KeyStoreException {
        throw new RuntimeException("operation not supported");
    }

    public int engineSize() {
        Hashtable hashtable = new Hashtable();
        Enumeration keys = this.certs.keys();
        while (keys.hasMoreElements()) {
            hashtable.put(keys.nextElement(), "cert");
        }
        keys = this.keys.keys();
        while (keys.hasMoreElements()) {
            String str = (String) keys.nextElement();
            if (hashtable.get(str) == null) {
                hashtable.put(str, "key");
            }
        }
        return hashtable.size();
    }

    public void engineStore(OutputStream outputStream, char[] cArr) throws IOException {
        doStore(outputStream, cArr, false);
    }

    public void engineStore(LoadStoreParameter loadStoreParameter) throws IOException, NoSuchAlgorithmException, CertificateException {
        if (loadStoreParameter != null) {
            boolean z = loadStoreParameter instanceof PKCS12StoreParameter;
            StringBuilder stringBuilder;
            if (z || (loadStoreParameter instanceof JDKPKCS12StoreParameter)) {
                PKCS12StoreParameter pKCS12StoreParameter;
                char[] cArr;
                if (z) {
                    pKCS12StoreParameter = (PKCS12StoreParameter) loadStoreParameter;
                } else {
                    JDKPKCS12StoreParameter jDKPKCS12StoreParameter = (JDKPKCS12StoreParameter) loadStoreParameter;
                    pKCS12StoreParameter = new PKCS12StoreParameter(jDKPKCS12StoreParameter.getOutputStream(), loadStoreParameter.getProtectionParameter(), jDKPKCS12StoreParameter.isUseDEREncoding());
                }
                ProtectionParameter protectionParameter = loadStoreParameter.getProtectionParameter();
                if (protectionParameter == null) {
                    cArr = null;
                } else if (protectionParameter instanceof PasswordProtection) {
                    cArr = ((PasswordProtection) protectionParameter).getPassword();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No support for protection parameter of type ");
                    stringBuilder.append(protectionParameter.getClass().getName());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                doStore(pKCS12StoreParameter.getOutputStream(), cArr, pKCS12StoreParameter.isForDEREncoding());
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("No support for 'param' of type ");
            stringBuilder.append(loadStoreParameter.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        throw new IllegalArgumentException("'param' arg cannot be null");
    }

    public void setRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
    }

    protected PrivateKey unwrapKey(AlgorithmIdentifier algorithmIdentifier, byte[] bArr, char[] cArr, boolean z) throws IOException {
        ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();
        try {
            if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
                PKCS12PBEParams instance = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());
                AlgorithmParameterSpec pBEParameterSpec = new PBEParameterSpec(instance.getIV(), validateIterationCount(instance.getIterations()));
                Cipher createCipher = this.helper.createCipher(algorithm.getId());
                createCipher.init(4, new PKCS12Key(cArr, z), pBEParameterSpec);
                return (PrivateKey) createCipher.unwrap(bArr, "", 2);
            } else if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
                return (PrivateKey) createCipher(4, cArr, algorithmIdentifier).unwrap(bArr, "", 2);
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

    protected byte[] wrapKey(String str, Key key, PKCS12PBEParams pKCS12PBEParams, char[] cArr) throws IOException {
        KeySpec pBEKeySpec = new PBEKeySpec(cArr);
        try {
            SecretKeyFactory createSecretKeyFactory = this.helper.createSecretKeyFactory(str);
            AlgorithmParameterSpec pBEParameterSpec = new PBEParameterSpec(pKCS12PBEParams.getIV(), pKCS12PBEParams.getIterations().intValue());
            Cipher createCipher = this.helper.createCipher(str);
            createCipher.init(3, createSecretKeyFactory.generateSecret(pBEKeySpec), pBEParameterSpec);
            return createCipher.wrap(key);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception encrypting data - ");
            stringBuilder.append(e.toString());
            throw new IOException(stringBuilder.toString());
        }
    }
}
