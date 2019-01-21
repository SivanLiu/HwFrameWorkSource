package android.security.keystore;

import android.security.Credentials;
import android.security.GateKeeper;
import android.security.KeyStore;
import android.security.KeyStoreParameter;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties.BlockMode;
import android.security.keystore.KeyProperties.Digest;
import android.security.keystore.KeyProperties.EncryptionPadding;
import android.security.keystore.KeyProperties.KeyAlgorithm;
import android.security.keystore.KeyProperties.Purpose;
import android.security.keystore.KeyProtection.Builder;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore.Entry;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.SecretKey;
import libcore.util.EmptyArray;

public class AndroidKeyStoreSpi extends KeyStoreSpi {
    public static final String NAME = "AndroidKeyStore";
    private KeyStore mKeyStore;
    private int mUid = -1;

    static class KeyStoreX509Certificate extends DelegatingX509Certificate {
        private final String mPrivateKeyAlias;
        private final int mPrivateKeyUid;

        KeyStoreX509Certificate(String privateKeyAlias, int privateKeyUid, X509Certificate delegate) {
            super(delegate);
            this.mPrivateKeyAlias = privateKeyAlias;
            this.mPrivateKeyUid = privateKeyUid;
        }

        public PublicKey getPublicKey() {
            PublicKey original = super.getPublicKey();
            return AndroidKeyStoreProvider.getAndroidKeyStorePublicKey(this.mPrivateKeyAlias, this.mPrivateKeyUid, original.getAlgorithm(), original.getEncoded());
        }
    }

    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        String userKeyAlias = new StringBuilder();
        userKeyAlias.append(Credentials.USER_PRIVATE_KEY);
        userKeyAlias.append(alias);
        userKeyAlias = userKeyAlias.toString();
        if (!this.mKeyStore.contains(userKeyAlias, this.mUid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.USER_SECRET_KEY);
            stringBuilder.append(alias);
            userKeyAlias = stringBuilder.toString();
            if (!this.mKeyStore.contains(userKeyAlias, this.mUid)) {
                return null;
            }
        }
        return AndroidKeyStoreProvider.loadAndroidKeyStoreKeyFromKeystore(this.mKeyStore, userKeyAlias, this.mUid);
    }

    public Certificate[] engineGetCertificateChain(String alias) {
        if (alias != null) {
            X509Certificate leaf = (X509Certificate) engineGetCertificate(alias);
            if (leaf == null) {
                return null;
            }
            Certificate[] caList;
            byte[] caBytes = this.mKeyStore;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.CA_CERTIFICATE);
            stringBuilder.append(alias);
            caBytes = caBytes.get(stringBuilder.toString(), this.mUid);
            int i = 1;
            if (caBytes != null) {
                Collection<X509Certificate> caChain = toCertificates(caBytes);
                caList = new Certificate[(caChain.size() + 1)];
                for (Certificate certificate : caChain) {
                    int i2 = i + 1;
                    caList[i] = certificate;
                    i = i2;
                }
            } else {
                caList = new Certificate[1];
            }
            Certificate[] caList2 = caList;
            caList2[0] = leaf;
            return caList2;
        }
        throw new NullPointerException("alias == null");
    }

    public Certificate engineGetCertificate(String alias) {
        if (alias != null) {
            byte[] encodedCert = this.mKeyStore;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.USER_CERTIFICATE);
            stringBuilder.append(alias);
            encodedCert = encodedCert.get(stringBuilder.toString(), this.mUid);
            if (encodedCert != null) {
                return getCertificateForPrivateKeyEntry(alias, encodedCert);
            }
            KeyStore keyStore = this.mKeyStore;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Credentials.CA_CERTIFICATE);
            stringBuilder2.append(alias);
            encodedCert = keyStore.get(stringBuilder2.toString(), this.mUid);
            if (encodedCert != null) {
                return getCertificateForTrustedCertificateEntry(encodedCert);
            }
            return null;
        }
        throw new NullPointerException("alias == null");
    }

    private Certificate getCertificateForTrustedCertificateEntry(byte[] encodedCert) {
        return toCertificate(encodedCert);
    }

    private Certificate getCertificateForPrivateKeyEntry(String alias, byte[] encodedCert) {
        X509Certificate cert = toCertificate(encodedCert);
        if (cert == null) {
            return null;
        }
        String privateKeyAlias = new StringBuilder();
        privateKeyAlias.append(Credentials.USER_PRIVATE_KEY);
        privateKeyAlias.append(alias);
        privateKeyAlias = privateKeyAlias.toString();
        if (this.mKeyStore.contains(privateKeyAlias, this.mUid)) {
            return wrapIntoKeyStoreCertificate(privateKeyAlias, this.mUid, cert);
        }
        return cert;
    }

    private static KeyStoreX509Certificate wrapIntoKeyStoreCertificate(String privateKeyAlias, int uid, X509Certificate certificate) {
        return certificate != null ? new KeyStoreX509Certificate(privateKeyAlias, uid, certificate) : null;
    }

    private static X509Certificate toCertificate(byte[] bytes) {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) {
            Log.w("AndroidKeyStore", "Couldn't parse certificate in keystore", e);
            return null;
        }
    }

    private static Collection<X509Certificate> toCertificates(byte[] bytes) {
        try {
            return CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) {
            Log.w("AndroidKeyStore", "Couldn't parse certificates in keystore", e);
            return new ArrayList();
        }
    }

    private Date getModificationDate(String alias) {
        long epochMillis = this.mKeyStore.getmtime(alias, this.mUid);
        if (epochMillis == -1) {
            return null;
        }
        return new Date(epochMillis);
    }

    public Date engineGetCreationDate(String alias) {
        if (alias != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.USER_PRIVATE_KEY);
            stringBuilder.append(alias);
            Date d = getModificationDate(stringBuilder.toString());
            if (d != null) {
                return d;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Credentials.USER_SECRET_KEY);
            stringBuilder2.append(alias);
            d = getModificationDate(stringBuilder2.toString());
            if (d != null) {
                return d;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Credentials.USER_CERTIFICATE);
            stringBuilder2.append(alias);
            d = getModificationDate(stringBuilder2.toString());
            if (d != null) {
                return d;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Credentials.CA_CERTIFICATE);
            stringBuilder2.append(alias);
            return getModificationDate(stringBuilder2.toString());
        }
        throw new NullPointerException("alias == null");
    }

    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
        if (password != null && password.length > 0) {
            throw new KeyStoreException("entries cannot be protected with passwords");
        } else if (key instanceof PrivateKey) {
            setPrivateKeyEntry(alias, (PrivateKey) key, chain, null);
        } else if (key instanceof SecretKey) {
            setSecretKeyEntry(alias, (SecretKey) key, null);
        } else {
            throw new KeyStoreException("Only PrivateKey and SecretKey are supported");
        }
    }

    private static KeyProtection getLegacyKeyProtectionParameter(PrivateKey key) throws KeyStoreException {
        Builder specBuilder;
        String keyAlgorithm = key.getAlgorithm();
        if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(keyAlgorithm)) {
            specBuilder = new Builder(12);
            specBuilder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
        } else if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(keyAlgorithm)) {
            specBuilder = new Builder(15);
            specBuilder.setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512);
            specBuilder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);
            specBuilder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS);
            specBuilder.setRandomizedEncryptionRequired(false);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key algorithm: ");
            stringBuilder.append(keyAlgorithm);
            throw new KeyStoreException(stringBuilder.toString());
        }
        specBuilder.setUserAuthenticationRequired(false);
        return specBuilder.build();
    }

    /* JADX WARNING: Removed duplicated region for block: B:156:0x0345  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x0345  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x0345  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x0345  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setPrivateKeyEntry(String alias, PrivateKey key, Certificate[] chain, ProtectionParameter param) throws KeyStoreException {
        KeyProtection spec;
        RuntimeException e;
        byte[] chainBytes;
        byte[] userCertBytes;
        Throwable th;
        String str = alias;
        PrivateKey privateKey = key;
        Certificate[] certificateArr = chain;
        ProtectionParameter protectionParameter = param;
        int flags = 0;
        if (protectionParameter == null) {
            spec = getLegacyKeyProtectionParameter(key);
        } else if (protectionParameter instanceof KeyStoreParameter) {
            spec = getLegacyKeyProtectionParameter(key);
            if (((KeyStoreParameter) protectionParameter).isEncryptionRequired()) {
                flags = 1;
            }
        } else if (protectionParameter instanceof KeyProtection) {
            spec = (KeyProtection) protectionParameter;
            if (spec.isCriticalToDeviceEncryption()) {
                flags = 0 | 8;
            }
            if (spec.isStrongBoxBacked()) {
                flags |= 16;
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported protection parameter class:");
            stringBuilder.append(param.getClass().getName());
            stringBuilder.append(". Supported: ");
            stringBuilder.append(KeyProtection.class.getName());
            stringBuilder.append(", ");
            stringBuilder.append(KeyStoreParameter.class.getName());
            throw new KeyStoreException(stringBuilder.toString());
        }
        int flags2 = flags;
        if (certificateArr == null || certificateArr.length == 0) {
            throw new KeyStoreException("Must supply at least one Certificate with PrivateKey");
        }
        StringBuilder stringBuilder2;
        X509Certificate[] x509chain = new X509Certificate[certificateArr.length];
        int i = 0;
        while (i < certificateArr.length) {
            if (!"X.509".equals(certificateArr[i].getType())) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Certificates must be in X.509 format: invalid cert #");
                stringBuilder2.append(i);
                throw new KeyStoreException(stringBuilder2.toString());
            } else if (certificateArr[i] instanceof X509Certificate) {
                x509chain[i] = (X509Certificate) certificateArr[i];
                i++;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Certificates must be in X.509 format: invalid cert #");
                stringBuilder2.append(i);
                throw new KeyStoreException(stringBuilder2.toString());
            }
        }
        X509Certificate[] x509CertificateArr;
        try {
            byte[] chainBytes2;
            int i2;
            String pkeyAlias;
            boolean z;
            byte[] bArr;
            KeymasterArguments importArgs;
            byte[] userCertBytes2 = x509chain[0].getEncoded();
            if (certificateArr.length > 1) {
                byte[][] certsBytes = new byte[(x509chain.length - 1)][];
                int totalCertLength = 0;
                int i3 = 0;
                while (i3 < certsBytes.length) {
                    try {
                        certsBytes[i3] = x509chain[i3 + 1].getEncoded();
                        totalCertLength += certsBytes[i3].length;
                        i3++;
                    } catch (CertificateEncodingException e2) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Failed to encode certificate #");
                        stringBuilder3.append(i3);
                        throw new KeyStoreException(stringBuilder3.toString(), e2);
                    }
                }
                chainBytes2 = new byte[totalCertLength];
                int outputOffset = 0;
                i2 = 0;
                while (i2 < certsBytes.length) {
                    int certLength = certsBytes[i2].length;
                    System.arraycopy(certsBytes[i2], 0, chainBytes2, outputOffset, certLength);
                    outputOffset += certLength;
                    certsBytes[i2] = null;
                    i2++;
                    certificateArr = chain;
                }
                pkeyAlias = null;
            } else {
                pkeyAlias = null;
                chainBytes2 = null;
            }
            byte[] chainBytes3 = chainBytes2;
            if (privateKey instanceof AndroidKeyStorePrivateKey) {
                pkeyAlias = ((AndroidKeyStoreKey) privateKey).getAlias();
            }
            String keyFormat;
            String pkeyAlias2;
            if (pkeyAlias == null || !pkeyAlias.startsWith(Credentials.USER_PRIVATE_KEY)) {
                keyFormat = key.getFormat();
                if (keyFormat == null || !"PKCS#8".equals(keyFormat)) {
                    z = true;
                    x509CertificateArr = x509chain;
                    bArr = userCertBytes2;
                    chainBytes3 = bArr;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Unsupported private key export format: ");
                    stringBuilder4.append(keyFormat);
                    stringBuilder4.append(". Only private keys which export their key material in PKCS#8 format are supported.");
                    throw new KeyStoreException(stringBuilder4.toString());
                }
                chainBytes2 = key.getEncoded();
                if (chainBytes2 != null) {
                    KeymasterArguments importArgs2 = new KeymasterArguments();
                    try {
                        importArgs2.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, KeyAlgorithm.toKeymasterAsymmetricKeyAlgorithm(key.getAlgorithm()));
                        flags = spec.getPurposes();
                        importArgs2.addEnums(KeymasterDefs.KM_TAG_PURPOSE, Purpose.allToKeymaster(flags));
                        if (spec.isDigestsSpecified()) {
                            try {
                                importArgs2.addEnums(KeymasterDefs.KM_TAG_DIGEST, Digest.allToKeymaster(spec.getDigests()));
                            } catch (IllegalArgumentException | IllegalStateException e3) {
                                e = e3;
                                z = true;
                            }
                        }
                        importArgs2.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, BlockMode.allToKeymaster(spec.getBlockModes()));
                        int[] keymasterEncryptionPaddings = EncryptionPadding.allToKeymaster(spec.getEncryptionPaddings());
                        if ((flags & 1) != 0) {
                            try {
                                if (spec.isRandomizedEncryptionRequired()) {
                                    i2 = keymasterEncryptionPaddings.length;
                                    flags = 0;
                                    while (flags < i2) {
                                        pkeyAlias2 = pkeyAlias;
                                        pkeyAlias = keymasterEncryptionPaddings[flags];
                                        try {
                                            if (KeymasterUtils.isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(pkeyAlias)) {
                                                flags++;
                                                pkeyAlias = pkeyAlias2;
                                            } else {
                                                StringBuilder stringBuilder5 = new StringBuilder();
                                                z = true;
                                                try {
                                                    stringBuilder5.append("Randomized encryption (IND-CPA) required but is violated by encryption padding mode: ");
                                                    stringBuilder5.append(EncryptionPadding.fromKeymaster(pkeyAlias));
                                                    stringBuilder5.append(". See KeyProtection documentation.");
                                                    throw new KeyStoreException(stringBuilder5.toString());
                                                } catch (IllegalArgumentException | IllegalStateException e4) {
                                                    e = e4;
                                                    bArr = userCertBytes2;
                                                    userCertBytes2 = chainBytes3;
                                                    chainBytes3 = bArr;
                                                    throw new KeyStoreException(e);
                                                }
                                            }
                                        } catch (IllegalArgumentException | IllegalStateException e5) {
                                            e = e5;
                                            z = true;
                                            x509CertificateArr = x509chain;
                                            bArr = userCertBytes2;
                                            userCertBytes2 = chainBytes3;
                                            chainBytes3 = bArr;
                                            throw new KeyStoreException(e);
                                        }
                                    }
                                    z = true;
                                    importArgs2.addEnums(KeymasterDefs.KM_TAG_PADDING, keymasterEncryptionPaddings);
                                    importArgs2.addEnums(KeymasterDefs.KM_TAG_PADDING, SignaturePadding.allToKeymaster(spec.getSignaturePaddings()));
                                    KeymasterUtils.addUserAuthArgs(importArgs2, spec);
                                    importArgs2.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, spec.getKeyValidityStart());
                                    importArgs2.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, spec.getKeyValidityForOriginationEnd());
                                    importArgs2.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, spec.getKeyValidityForConsumptionEnd());
                                    pkeyAlias = chainBytes2;
                                    importArgs = importArgs2;
                                }
                            } catch (IllegalArgumentException | IllegalStateException e6) {
                                e = e6;
                                pkeyAlias2 = pkeyAlias;
                                z = true;
                                x509CertificateArr = x509chain;
                                bArr = userCertBytes2;
                                userCertBytes2 = chainBytes3;
                                chainBytes3 = bArr;
                                throw new KeyStoreException(e);
                            }
                        }
                        int i4 = flags;
                        pkeyAlias2 = pkeyAlias;
                        z = true;
                        try {
                            importArgs2.addEnums(KeymasterDefs.KM_TAG_PADDING, keymasterEncryptionPaddings);
                            importArgs2.addEnums(KeymasterDefs.KM_TAG_PADDING, SignaturePadding.allToKeymaster(spec.getSignaturePaddings()));
                            KeymasterUtils.addUserAuthArgs(importArgs2, spec);
                            importArgs2.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, spec.getKeyValidityStart());
                            importArgs2.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, spec.getKeyValidityForOriginationEnd());
                            importArgs2.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, spec.getKeyValidityForConsumptionEnd());
                            pkeyAlias = chainBytes2;
                            importArgs = importArgs2;
                        } catch (IllegalArgumentException | IllegalStateException e7) {
                            e = e7;
                            x509CertificateArr = x509chain;
                            bArr = userCertBytes2;
                            userCertBytes2 = chainBytes3;
                            chainBytes3 = bArr;
                            throw new KeyStoreException(e);
                        }
                    } catch (IllegalArgumentException | IllegalStateException e8) {
                        e = e8;
                        pkeyAlias2 = pkeyAlias;
                        z = true;
                        x509CertificateArr = x509chain;
                        bArr = userCertBytes2;
                        userCertBytes2 = chainBytes3;
                        chainBytes3 = bArr;
                        throw new KeyStoreException(e);
                    }
                }
                z = true;
                x509CertificateArr = x509chain;
                bArr = userCertBytes2;
                userCertBytes2 = chainBytes3;
                chainBytes3 = bArr;
                throw new KeyStoreException("Private key did not export any key material");
            }
            keyFormat = pkeyAlias.substring(Credentials.USER_PRIVATE_KEY.length());
            if (str.equals(keyFormat)) {
                pkeyAlias2 = pkeyAlias;
                pkeyAlias = null;
                z = false;
                importArgs = null;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Can only replace keys with same alias: ");
                stringBuilder2.append(str);
                stringBuilder2.append(" != ");
                stringBuilder2.append(keyFormat);
                throw new KeyStoreException(stringBuilder2.toString());
            }
            boolean success = false;
            if (z) {
                try {
                    Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
                    KeyCharacteristics resultingKeyCharacteristics = new KeyCharacteristics();
                    KeyStore keyStore = this.mKeyStore;
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append(Credentials.USER_PRIVATE_KEY);
                    stringBuilder6.append(str);
                    chainBytes = chainBytes3;
                    userCertBytes = userCertBytes2;
                    x509CertificateArr = x509chain;
                    try {
                        flags = keyStore.importKey(stringBuilder6.toString(), importArgs, 1, pkeyAlias, this.mUid, flags2, resultingKeyCharacteristics);
                        i = 1;
                        if (flags != 1) {
                            throw new KeyStoreException("Failed to store private key", KeyStore.getKeyStoreException(flags));
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        userCertBytes2 = chainBytes;
                        chainBytes3 = userCertBytes;
                        if (!success) {
                            if (z) {
                                Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
                            } else {
                                Credentials.deleteCertificateTypesForAlias(this.mKeyStore, str, this.mUid);
                                Credentials.deleteLegacyKeyForAlias(this.mKeyStore, str, this.mUid);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    x509CertificateArr = x509chain;
                    bArr = userCertBytes2;
                    userCertBytes2 = chainBytes3;
                    chainBytes3 = bArr;
                    if (success) {
                    }
                    throw th;
                }
            }
            chainBytes = chainBytes3;
            userCertBytes = userCertBytes2;
            x509CertificateArr = x509chain;
            i = 1;
            try {
                Credentials.deleteCertificateTypesForAlias(this.mKeyStore, str, this.mUid);
                Credentials.deleteLegacyKeyForAlias(this.mKeyStore, str, this.mUid);
            } catch (Throwable th4) {
                th = th4;
                userCertBytes2 = chainBytes;
                chainBytes3 = userCertBytes;
                if (success) {
                }
                throw th;
            }
            KeyStore keyStore2 = this.mKeyStore;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Credentials.USER_CERTIFICATE);
            stringBuilder2.append(str);
            try {
                flags = keyStore2.insert(stringBuilder2.toString(), userCertBytes, this.mUid, flags2);
                if (flags == i) {
                    KeyStore keyStore3 = this.mKeyStore;
                    StringBuilder stringBuilder7 = new StringBuilder();
                    stringBuilder7.append(Credentials.CA_CERTIFICATE);
                    stringBuilder7.append(str);
                    try {
                        flags = keyStore3.insert(stringBuilder7.toString(), chainBytes, this.mUid, flags2);
                        if (flags != i) {
                            throw new KeyStoreException("Failed to store certificate chain", KeyStore.getKeyStoreException(flags));
                        } else if (1 != 0) {
                            return;
                        } else {
                            if (z) {
                                Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
                                return;
                            }
                            Credentials.deleteCertificateTypesForAlias(this.mKeyStore, str, this.mUid);
                            Credentials.deleteLegacyKeyForAlias(this.mKeyStore, str, this.mUid);
                            return;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        if (success) {
                        }
                        throw th;
                    }
                }
                throw new KeyStoreException("Failed to store certificate #0", KeyStore.getKeyStoreException(flags));
            } catch (Throwable th6) {
                th = th6;
                userCertBytes2 = chainBytes;
                if (success) {
                }
                throw th;
            }
        } catch (CertificateEncodingException e22) {
            x509CertificateArr = x509chain;
            throw new KeyStoreException("Failed to encode certificate #0", e22);
        }
    }

    private void setSecretKeyEntry(String entryAlias, SecretKey key, ProtectionParameter param) throws KeyStoreException {
        RuntimeException e;
        KeymasterArguments keymasterArguments;
        String str = entryAlias;
        SecretKey secretKey = key;
        ProtectionParameter protectionParameter = param;
        if (protectionParameter == null || (protectionParameter instanceof KeyProtection)) {
            KeyProtection params = (KeyProtection) protectionParameter;
            String keyAliasPrefix;
            StringBuilder stringBuilder;
            if (secretKey instanceof AndroidKeyStoreSecretKey) {
                String keyAliasInKeystore = ((AndroidKeyStoreSecretKey) secretKey).getAlias();
                if (keyAliasInKeystore != null) {
                    keyAliasPrefix = Credentials.USER_PRIVATE_KEY;
                    if (!keyAliasInKeystore.startsWith(keyAliasPrefix)) {
                        keyAliasPrefix = Credentials.USER_SECRET_KEY;
                        if (!keyAliasInKeystore.startsWith(keyAliasPrefix)) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("KeyStore-backed secret key has invalid alias: ");
                            stringBuilder2.append(keyAliasInKeystore);
                            throw new KeyStoreException(stringBuilder2.toString());
                        }
                    }
                    String keyEntryAlias = keyAliasInKeystore.substring(keyAliasPrefix.length());
                    if (!str.equals(keyEntryAlias)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Can only replace KeyStore-backed keys with same alias: ");
                        stringBuilder.append(str);
                        stringBuilder.append(" != ");
                        stringBuilder.append(keyEntryAlias);
                        throw new KeyStoreException(stringBuilder.toString());
                    } else if (params != null) {
                        throw new KeyStoreException("Modifying KeyStore-backed key using protection parameters not supported");
                    } else {
                        return;
                    }
                }
                throw new KeyStoreException("KeyStore-backed secret key does not have an alias");
            } else if (params != null) {
                keyAliasPrefix = key.getFormat();
                StringBuilder stringBuilder3;
                if (keyAliasPrefix == null) {
                    throw new KeyStoreException("Only secret keys that export their key material are supported");
                } else if ("RAW".equals(keyAliasPrefix)) {
                    byte[] keyMaterial = key.getEncoded();
                    if (keyMaterial != null) {
                        KeymasterArguments args = new KeymasterArguments();
                        try {
                            int[] keymasterDigests;
                            int[] keymasterDigestsFromParams;
                            StringBuilder stringBuilder4;
                            int keymasterAlgorithm = KeyAlgorithm.toKeymasterSecretKeyAlgorithm(key.getAlgorithm());
                            args.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, keymasterAlgorithm);
                            int i = 0;
                            if (keymasterAlgorithm == 128) {
                                try {
                                    int keymasterImpliedDigest = KeyAlgorithm.toKeymasterDigest(key.getAlgorithm());
                                    if (keymasterImpliedDigest != -1) {
                                        keymasterDigests = new int[]{keymasterImpliedDigest};
                                        if (params.isDigestsSpecified()) {
                                            keymasterDigestsFromParams = Digest.allToKeymaster(params.getDigests());
                                            if (keymasterDigestsFromParams.length != 1 || keymasterDigestsFromParams[0] != keymasterImpliedDigest) {
                                                stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("Unsupported digests specification: ");
                                                stringBuilder4.append(Arrays.asList(params.getDigests()));
                                                stringBuilder4.append(". Only ");
                                                stringBuilder4.append(Digest.fromKeymaster(keymasterImpliedDigest));
                                                stringBuilder4.append(" supported for HMAC key algorithm ");
                                                stringBuilder4.append(key.getAlgorithm());
                                                throw new KeyStoreException(stringBuilder4.toString());
                                            }
                                        }
                                    } else {
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("HMAC key algorithm digest unknown for key algorithm ");
                                        stringBuilder.append(key.getAlgorithm());
                                        throw new ProviderException(stringBuilder.toString());
                                    }
                                } catch (IllegalArgumentException | IllegalStateException e2) {
                                    e = e2;
                                    keymasterArguments = args;
                                    throw new KeyStoreException(e);
                                }
                            } else if (params.isDigestsSpecified()) {
                                keymasterDigests = Digest.allToKeymaster(params.getDigests());
                            } else {
                                keymasterDigests = EmptyArray.INT;
                            }
                            int[] keymasterDigests2 = keymasterDigests;
                            args.addEnums(KeymasterDefs.KM_TAG_DIGEST, keymasterDigests2);
                            int purposes = params.getPurposes();
                            keymasterDigestsFromParams = BlockMode.allToKeymaster(params.getBlockModes());
                            if ((purposes & 1) != 0) {
                                if (params.isRandomizedEncryptionRequired()) {
                                    int length = keymasterDigestsFromParams.length;
                                    while (i < length) {
                                        int keymasterBlockMode = keymasterDigestsFromParams[i];
                                        if (KeymasterUtils.isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(keymasterBlockMode)) {
                                            i++;
                                        } else {
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("Randomized encryption (IND-CPA) required but may be violated by block mode: ");
                                            stringBuilder4.append(BlockMode.fromKeymaster(keymasterBlockMode));
                                            stringBuilder4.append(". See KeyProtection documentation.");
                                            throw new KeyStoreException(stringBuilder4.toString());
                                        }
                                    }
                                }
                            }
                            args.addEnums(KeymasterDefs.KM_TAG_PURPOSE, Purpose.allToKeymaster(purposes));
                            if (params.isRollbackResistant()) {
                                args.addEnum(KeymasterDefs.KM_TAG_PURPOSE, KeymasterDefs.KM_PURPOSE_ROLLBACK_RESISTANT);
                            }
                            args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, keymasterDigestsFromParams);
                            if (params.getSignaturePaddings().length <= 0) {
                                args.addEnums(KeymasterDefs.KM_TAG_PADDING, EncryptionPadding.allToKeymaster(params.getEncryptionPaddings()));
                                KeymasterUtils.addUserAuthArgs(args, params);
                                KeymasterUtils.addMinMacLengthAuthorizationIfNecessary(args, keymasterAlgorithm, keymasterDigestsFromParams, keymasterDigests2);
                                args.addDateIfNotNull(KeymasterDefs.KM_TAG_ACTIVE_DATETIME, params.getKeyValidityStart());
                                args.addDateIfNotNull(KeymasterDefs.KM_TAG_ORIGINATION_EXPIRE_DATETIME, params.getKeyValidityForOriginationEnd());
                                args.addDateIfNotNull(KeymasterDefs.KM_TAG_USAGE_EXPIRE_DATETIME, params.getKeyValidityForConsumptionEnd());
                                if ((purposes & 1) != 0) {
                                    if (!params.isRandomizedEncryptionRequired()) {
                                        args.addBoolean(KeymasterDefs.KM_TAG_CALLER_NONCE);
                                    }
                                }
                                keymasterAlgorithm = 0;
                                if (params.isCriticalToDeviceEncryption()) {
                                    keymasterAlgorithm = 0 | 8;
                                }
                                if (params.isStrongBoxBacked()) {
                                    keymasterAlgorithm |= 16;
                                }
                                Credentials.deleteAllTypesForAlias(this.mKeyStore, str, this.mUid);
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(Credentials.USER_PRIVATE_KEY);
                                stringBuilder3.append(str);
                                String keyAliasInKeystore2 = stringBuilder3.toString();
                                keymasterDigests2 = this.mKeyStore.importKey(keyAliasInKeystore2, args, 3, keyMaterial, this.mUid, keymasterAlgorithm, new KeyCharacteristics());
                                if (keymasterDigests2 != 1) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to import secret key. Keystore error code: ");
                                    stringBuilder.append(keymasterDigests2);
                                    throw new KeyStoreException(stringBuilder.toString());
                                }
                                return;
                            }
                            try {
                                throw new KeyStoreException("Signature paddings not supported for symmetric keys");
                            } catch (IllegalArgumentException | IllegalStateException e3) {
                                e = e3;
                                throw new KeyStoreException(e);
                            }
                        } catch (IllegalArgumentException | IllegalStateException e4) {
                            e = e4;
                            keymasterArguments = args;
                            throw new KeyStoreException(e);
                        }
                    }
                    throw new KeyStoreException("Key did not export its key material despite supporting RAW format export");
                } else {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unsupported secret key material export format: ");
                    stringBuilder3.append(keyAliasPrefix);
                    throw new KeyStoreException(stringBuilder3.toString());
                }
            } else {
                throw new KeyStoreException("Protection parameters must be specified when importing a symmetric key");
            }
        }
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append("Unsupported protection parameter class: ");
        stringBuilder5.append(param.getClass().getName());
        stringBuilder5.append(". Supported: ");
        stringBuilder5.append(KeyProtection.class.getName());
        throw new KeyStoreException(stringBuilder5.toString());
    }

    private void setWrappedKeyEntry(String alias, WrappedKeyEntry entry, ProtectionParameter param) throws KeyStoreException {
        if (param == null) {
            String mode;
            byte[] maskingKey = new byte[32];
            KeymasterArguments args = new KeymasterArguments();
            String[] parts = entry.getTransformation().split("/");
            String algorithm = parts[0];
            if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(algorithm)) {
                args.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 1);
            } else if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(algorithm)) {
                args.addEnum(KeymasterDefs.KM_TAG_ALGORITHM, 1);
            }
            if (parts.length > 1) {
                mode = parts[1];
                if (KeyProperties.BLOCK_MODE_ECB.equalsIgnoreCase(mode)) {
                    args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 1);
                } else if (KeyProperties.BLOCK_MODE_CBC.equalsIgnoreCase(mode)) {
                    args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 2);
                } else if (KeyProperties.BLOCK_MODE_CTR.equalsIgnoreCase(mode)) {
                    args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 3);
                } else if (KeyProperties.BLOCK_MODE_GCM.equalsIgnoreCase(mode)) {
                    args.addEnums(KeymasterDefs.KM_TAG_BLOCK_MODE, 32);
                }
            }
            if (parts.length > 2) {
                String padding = parts[2];
                if (!KeyProperties.ENCRYPTION_PADDING_NONE.equalsIgnoreCase(padding)) {
                    if (KeyProperties.ENCRYPTION_PADDING_PKCS7.equalsIgnoreCase(padding)) {
                        args.addEnums(KeymasterDefs.KM_TAG_PADDING, 64);
                    } else if (KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1.equalsIgnoreCase(padding)) {
                        args.addEnums(KeymasterDefs.KM_TAG_PADDING, 4);
                    } else if (KeyProperties.ENCRYPTION_PADDING_RSA_OAEP.equalsIgnoreCase(padding)) {
                        args.addEnums(KeymasterDefs.KM_TAG_PADDING, 2);
                    }
                }
            }
            KeyGenParameterSpec spec = (KeyGenParameterSpec) entry.getAlgorithmParameterSpec();
            if (spec.isDigestsSpecified()) {
                String digest = spec.getDigests()[0];
                if (!KeyProperties.DIGEST_NONE.equalsIgnoreCase(digest)) {
                    if (KeyProperties.DIGEST_MD5.equalsIgnoreCase(digest)) {
                        args.addEnums(KeymasterDefs.KM_TAG_DIGEST, 1);
                    } else if (KeyProperties.DIGEST_SHA1.equalsIgnoreCase(digest)) {
                        args.addEnums(KeymasterDefs.KM_TAG_DIGEST, 2);
                    } else if (KeyProperties.DIGEST_SHA224.equalsIgnoreCase(digest)) {
                        args.addEnums(KeymasterDefs.KM_TAG_DIGEST, 3);
                    } else if (KeyProperties.DIGEST_SHA256.equalsIgnoreCase(digest)) {
                        args.addEnums(KeymasterDefs.KM_TAG_DIGEST, 4);
                    } else if (KeyProperties.DIGEST_SHA384.equalsIgnoreCase(digest)) {
                        args.addEnums(KeymasterDefs.KM_TAG_DIGEST, 5);
                    } else if (KeyProperties.DIGEST_SHA512.equalsIgnoreCase(digest)) {
                        args.addEnums(KeymasterDefs.KM_TAG_DIGEST, 6);
                    }
                }
            }
            int errorCode = this.mKeyStore;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.USER_SECRET_KEY);
            stringBuilder.append(alias);
            mode = stringBuilder.toString();
            byte[] wrappedKeyBytes = entry.getWrappedKeyBytes();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Credentials.USER_PRIVATE_KEY);
            stringBuilder2.append(entry.getWrappingKeyAlias());
            String stringBuilder3 = stringBuilder2.toString();
            long secureUserId = GateKeeper.getSecureUserId();
            long j = secureUserId;
            int i = 1;
            errorCode = errorCode.importWrappedKey(mode, wrappedKeyBytes, stringBuilder3, maskingKey, args, j, 0, this.mUid, new KeyCharacteristics());
            if (errorCode == -100) {
                throw new SecureKeyImportUnavailableException("Could not import wrapped key");
            } else if (errorCode != i) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to import wrapped key. Keystore error code: ");
                stringBuilder.append(errorCode);
                throw new KeyStoreException(stringBuilder.toString());
            } else {
                return;
            }
        }
        throw new KeyStoreException("Protection parameters are specified inside wrapped keys");
    }

    public void engineSetKeyEntry(String alias, byte[] userKey, Certificate[] chain) throws KeyStoreException {
        throw new KeyStoreException("Operation not supported because key encoding is unknown");
    }

    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        if (isKeyEntry(alias)) {
            throw new KeyStoreException("Entry exists and is not a trusted certificate");
        } else if (cert != null) {
            try {
                byte[] encoded = cert.getEncoded();
                KeyStore keyStore = this.mKeyStore;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(Credentials.CA_CERTIFICATE);
                stringBuilder.append(alias);
                if (!keyStore.put(stringBuilder.toString(), encoded, this.mUid, 0)) {
                    throw new KeyStoreException("Couldn't insert certificate; is KeyStore initialized?");
                }
            } catch (CertificateEncodingException e) {
                throw new KeyStoreException(e);
            }
        } else {
            throw new NullPointerException("cert == null");
        }
    }

    public void engineDeleteEntry(String alias) throws KeyStoreException {
        if (!Credentials.deleteAllTypesForAlias(this.mKeyStore, alias, this.mUid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to delete entry: ");
            stringBuilder.append(alias);
            throw new KeyStoreException(stringBuilder.toString());
        }
    }

    private Set<String> getUniqueAliases() {
        String[] rawAliases = this.mKeyStore.list("", this.mUid);
        if (rawAliases == null) {
            return new HashSet();
        }
        Set<String> aliases = new HashSet(rawAliases.length);
        for (String alias : rawAliases) {
            int idx = alias.indexOf(95);
            if (idx == -1 || alias.length() <= idx) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid alias: ");
                stringBuilder.append(alias);
                Log.e("AndroidKeyStore", stringBuilder.toString());
            } else {
                aliases.add(new String(alias.substring(idx + 1)));
            }
        }
        return aliases;
    }

    public Enumeration<String> engineAliases() {
        return Collections.enumeration(getUniqueAliases());
    }

    public boolean engineContainsAlias(String alias) {
        if (alias != null) {
            KeyStore keyStore = this.mKeyStore;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.USER_PRIVATE_KEY);
            stringBuilder.append(alias);
            if (!keyStore.contains(stringBuilder.toString(), this.mUid)) {
                keyStore = this.mKeyStore;
                stringBuilder = new StringBuilder();
                stringBuilder.append(Credentials.USER_SECRET_KEY);
                stringBuilder.append(alias);
                if (!keyStore.contains(stringBuilder.toString(), this.mUid)) {
                    keyStore = this.mKeyStore;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(Credentials.USER_CERTIFICATE);
                    stringBuilder.append(alias);
                    if (!keyStore.contains(stringBuilder.toString(), this.mUid)) {
                        keyStore = this.mKeyStore;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(Credentials.CA_CERTIFICATE);
                        stringBuilder.append(alias);
                        if (!keyStore.contains(stringBuilder.toString(), this.mUid)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        throw new NullPointerException("alias == null");
    }

    public int engineSize() {
        return getUniqueAliases().size();
    }

    public boolean engineIsKeyEntry(String alias) {
        return isKeyEntry(alias);
    }

    private boolean isKeyEntry(String alias) {
        KeyStore keyStore = this.mKeyStore;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Credentials.USER_PRIVATE_KEY);
        stringBuilder.append(alias);
        if (!keyStore.contains(stringBuilder.toString(), this.mUid)) {
            keyStore = this.mKeyStore;
            stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.USER_SECRET_KEY);
            stringBuilder.append(alias);
            if (!keyStore.contains(stringBuilder.toString(), this.mUid)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCertificateEntry(String alias) {
        if (alias != null) {
            KeyStore keyStore = this.mKeyStore;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Credentials.CA_CERTIFICATE);
            stringBuilder.append(alias);
            return keyStore.contains(stringBuilder.toString(), this.mUid);
        }
        throw new NullPointerException("alias == null");
    }

    public boolean engineIsCertificateEntry(String alias) {
        return !isKeyEntry(alias) && isCertificateEntry(alias);
    }

    public String engineGetCertificateAlias(Certificate cert) {
        if (cert == null || !"X.509".equalsIgnoreCase(cert.getType())) {
            return null;
        }
        try {
            byte[] targetCertBytes = cert.getEncoded();
            if (targetCertBytes == null) {
                return null;
            }
            int length;
            byte[] certBytes;
            StringBuilder stringBuilder;
            String alias;
            Set<String> nonCaEntries = new HashSet();
            String[] certAliases = this.mKeyStore.list(Credentials.USER_CERTIFICATE, this.mUid);
            int i = 0;
            if (certAliases != null) {
                for (String alias2 : certAliases) {
                    certBytes = this.mKeyStore;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(Credentials.USER_CERTIFICATE);
                    stringBuilder.append(alias2);
                    certBytes = certBytes.get(stringBuilder.toString(), this.mUid);
                    if (certBytes != null) {
                        nonCaEntries.add(alias2);
                        if (Arrays.equals(certBytes, targetCertBytes)) {
                            return alias2;
                        }
                    }
                }
            }
            String[] caAliases = this.mKeyStore.list(Credentials.CA_CERTIFICATE, this.mUid);
            if (certAliases != null) {
                length = caAliases.length;
                while (i < length) {
                    alias2 = caAliases[i];
                    if (!nonCaEntries.contains(alias2)) {
                        KeyStore keyStore = this.mKeyStore;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(Credentials.CA_CERTIFICATE);
                        stringBuilder.append(alias2);
                        certBytes = keyStore.get(stringBuilder.toString(), this.mUid);
                        if (certBytes != null && Arrays.equals(certBytes, targetCertBytes)) {
                            return alias2;
                        }
                    }
                    i++;
                }
            }
            return null;
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    public void engineStore(OutputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        throw new UnsupportedOperationException("Can not serialize AndroidKeyStore to OutputStream");
    }

    public void engineLoad(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        if (stream != null) {
            throw new IllegalArgumentException("InputStream not supported");
        } else if (password == null) {
            this.mKeyStore = KeyStore.getInstance();
            this.mUid = -1;
        } else {
            throw new IllegalArgumentException("password not supported");
        }
    }

    public void engineLoad(LoadStoreParameter param) throws IOException, NoSuchAlgorithmException, CertificateException {
        int uid = -1;
        if (param != null) {
            if (param instanceof AndroidKeyStoreLoadStoreParameter) {
                uid = ((AndroidKeyStoreLoadStoreParameter) param).getUid();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported param type: ");
                stringBuilder.append(param.getClass());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        this.mKeyStore = KeyStore.getInstance();
        this.mUid = uid;
    }

    public void engineSetEntry(String alias, Entry entry, ProtectionParameter param) throws KeyStoreException {
        if (entry != null) {
            Credentials.deleteAllTypesForAlias(this.mKeyStore, alias, this.mUid);
            if (entry instanceof TrustedCertificateEntry) {
                engineSetCertificateEntry(alias, ((TrustedCertificateEntry) entry).getTrustedCertificate());
                return;
            }
            if (entry instanceof PrivateKeyEntry) {
                PrivateKeyEntry prE = (PrivateKeyEntry) entry;
                setPrivateKeyEntry(alias, prE.getPrivateKey(), prE.getCertificateChain(), param);
            } else if (entry instanceof SecretKeyEntry) {
                setSecretKeyEntry(alias, ((SecretKeyEntry) entry).getSecretKey(), param);
            } else if (entry instanceof WrappedKeyEntry) {
                setWrappedKeyEntry(alias, (WrappedKeyEntry) entry, param);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Entry must be a PrivateKeyEntry, SecretKeyEntry or TrustedCertificateEntry; was ");
                stringBuilder.append(entry);
                throw new KeyStoreException(stringBuilder.toString());
            }
            return;
        }
        throw new KeyStoreException("entry == null");
    }
}
