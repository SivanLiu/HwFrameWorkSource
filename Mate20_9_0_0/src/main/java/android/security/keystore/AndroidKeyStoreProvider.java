package android.security.keystore;

import android.os.SystemProperties;
import android.security.KeyStore;
import android.security.keymaster.ExportResult;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties.KeyAlgorithm;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;

public class AndroidKeyStoreProvider extends Provider {
    private static final String DESEDE_SYSTEM_PROPERTY = "ro.hardware.keystore_desede";
    private static final String PACKAGE_NAME = "android.security.keystore";
    public static final String PROVIDER_NAME = "AndroidKeyStore";

    public AndroidKeyStoreProvider() {
        super("AndroidKeyStore", 1.0d, "Android KeyStore security provider");
        boolean supports3DES = "true".equals(SystemProperties.get(DESEDE_SYSTEM_PROPERTY));
        put("KeyStore.AndroidKeyStore", "android.security.keystore.AndroidKeyStoreSpi");
        put("KeyPairGenerator.EC", "android.security.keystore.AndroidKeyStoreKeyPairGeneratorSpi$EC");
        put("KeyPairGenerator.RSA", "android.security.keystore.AndroidKeyStoreKeyPairGeneratorSpi$RSA");
        putKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_EC);
        putKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_RSA);
        put("KeyGenerator.AES", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$AES");
        put("KeyGenerator.HmacSHA1", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA1");
        put("KeyGenerator.HmacSHA224", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA224");
        put("KeyGenerator.HmacSHA256", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA256");
        put("KeyGenerator.HmacSHA384", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA384");
        put("KeyGenerator.HmacSHA512", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$HmacSHA512");
        if (supports3DES) {
            put("KeyGenerator.DESede", "android.security.keystore.AndroidKeyStoreKeyGeneratorSpi$DESede");
        }
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_AES);
        if (supports3DES) {
            putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_3DES);
        }
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA1);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA224);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA384);
        putSecretKeyFactoryImpl(KeyProperties.KEY_ALGORITHM_HMAC_SHA512);
    }

    public static void install() {
        Provider[] providers = Security.getProviders();
        int bcProviderIndex = -1;
        for (int i = 0; i < providers.length; i++) {
            if ("BC".equals(providers[i].getName())) {
                bcProviderIndex = i;
                break;
            }
        }
        Security.addProvider(new AndroidKeyStoreProvider());
        Provider workaroundProvider = new AndroidKeyStoreBCWorkaroundProvider();
        if (bcProviderIndex != -1) {
            Security.insertProviderAt(workaroundProvider, bcProviderIndex + 1);
        } else {
            Security.addProvider(workaroundProvider);
        }
    }

    private void putSecretKeyFactoryImpl(String algorithm) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SecretKeyFactory.");
        stringBuilder.append(algorithm);
        put(stringBuilder.toString(), "android.security.keystore.AndroidKeyStoreSecretKeyFactorySpi");
    }

    private void putKeyFactoryImpl(String algorithm) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("KeyFactory.");
        stringBuilder.append(algorithm);
        put(stringBuilder.toString(), "android.security.keystore.AndroidKeyStoreKeyFactorySpi");
    }

    public static long getKeyStoreOperationHandle(Object cryptoPrimitive) {
        if (cryptoPrimitive != null) {
            Object spi;
            if (cryptoPrimitive instanceof Signature) {
                spi = ((Signature) cryptoPrimitive).getCurrentSpi();
            } else if (cryptoPrimitive instanceof Mac) {
                spi = ((Mac) cryptoPrimitive).getCurrentSpi();
            } else if (cryptoPrimitive instanceof Cipher) {
                spi = ((Cipher) cryptoPrimitive).getCurrentSpi();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported crypto primitive: ");
                stringBuilder.append(cryptoPrimitive);
                stringBuilder.append(". Supported: Signature, Mac, Cipher");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            if (spi == null) {
                throw new IllegalStateException("Crypto primitive not initialized");
            } else if (spi instanceof KeyStoreCryptoOperation) {
                return ((KeyStoreCryptoOperation) spi).getOperationHandle();
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Crypto primitive not backed by AndroidKeyStore provider: ");
                stringBuilder2.append(cryptoPrimitive);
                stringBuilder2.append(", spi: ");
                stringBuilder2.append(spi);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }
        throw new NullPointerException();
    }

    public static AndroidKeyStorePublicKey getAndroidKeyStorePublicKey(String alias, int uid, String keyAlgorithm, byte[] x509EncodedForm) {
        StringBuilder stringBuilder;
        try {
            PublicKey publicKey = KeyFactory.getInstance(keyAlgorithm).generatePublic(new X509EncodedKeySpec(x509EncodedForm));
            if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(keyAlgorithm)) {
                return new AndroidKeyStoreECPublicKey(alias, uid, (ECPublicKey) publicKey);
            }
            if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(keyAlgorithm)) {
                return new AndroidKeyStoreRSAPublicKey(alias, uid, (RSAPublicKey) publicKey);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported Android Keystore public key algorithm: ");
            stringBuilder.append(keyAlgorithm);
            throw new ProviderException(stringBuilder.toString());
        } catch (NoSuchAlgorithmException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to obtain ");
            stringBuilder.append(keyAlgorithm);
            stringBuilder.append(" KeyFactory");
            throw new ProviderException(stringBuilder.toString(), e);
        } catch (InvalidKeySpecException e2) {
            throw new ProviderException("Invalid X.509 encoding of public key", e2);
        }
    }

    private static AndroidKeyStorePrivateKey getAndroidKeyStorePrivateKey(AndroidKeyStorePublicKey publicKey) {
        String keyAlgorithm = publicKey.getAlgorithm();
        if (KeyProperties.KEY_ALGORITHM_EC.equalsIgnoreCase(keyAlgorithm)) {
            return new AndroidKeyStoreECPrivateKey(publicKey.getAlias(), publicKey.getUid(), ((ECKey) publicKey).getParams());
        }
        if (KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(keyAlgorithm)) {
            return new AndroidKeyStoreRSAPrivateKey(publicKey.getAlias(), publicKey.getUid(), ((RSAKey) publicKey).getModulus());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported Android Keystore public key algorithm: ");
        stringBuilder.append(keyAlgorithm);
        throw new ProviderException(stringBuilder.toString());
    }

    private static KeyCharacteristics getKeyCharacteristics(KeyStore keyStore, String alias, int uid) throws UnrecoverableKeyException {
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        int errorCode = keyStore.getKeyCharacteristics(alias, null, null, uid, keyCharacteristics);
        if (errorCode == 1) {
            return keyCharacteristics;
        }
        throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Failed to obtain information about key").initCause(KeyStore.getKeyStoreException(errorCode)));
    }

    private static AndroidKeyStorePublicKey loadAndroidKeyStorePublicKeyFromKeystore(KeyStore keyStore, String privateKeyAlias, int uid, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        ExportResult exportResult = keyStore.exportKey(privateKeyAlias, 0, null, null, uid);
        if (exportResult.resultCode == 1) {
            byte[] x509EncodedPublicKey = exportResult.exportData;
            Integer keymasterAlgorithm = keyCharacteristics.getEnum(KeymasterDefs.KM_TAG_ALGORITHM);
            if (keymasterAlgorithm != null) {
                try {
                    return getAndroidKeyStorePublicKey(privateKeyAlias, uid, KeyAlgorithm.fromKeymasterAsymmetricKeyAlgorithm(keymasterAlgorithm.intValue()), x509EncodedPublicKey);
                } catch (IllegalArgumentException e) {
                    throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Failed to load private key").initCause(e));
                }
            }
            throw new UnrecoverableKeyException("Key algorithm unknown");
        }
        throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Failed to obtain X.509 form of public key").initCause(KeyStore.getKeyStoreException(exportResult.resultCode)));
    }

    public static AndroidKeyStorePublicKey loadAndroidKeyStorePublicKeyFromKeystore(KeyStore keyStore, String privateKeyAlias, int uid) throws UnrecoverableKeyException {
        return loadAndroidKeyStorePublicKeyFromKeystore(keyStore, privateKeyAlias, uid, getKeyCharacteristics(keyStore, privateKeyAlias, uid));
    }

    private static KeyPair loadAndroidKeyStoreKeyPairFromKeystore(KeyStore keyStore, String privateKeyAlias, int uid, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        AndroidKeyStorePublicKey publicKey = loadAndroidKeyStorePublicKeyFromKeystore(keyStore, privateKeyAlias, uid, keyCharacteristics);
        return new KeyPair(publicKey, getAndroidKeyStorePrivateKey(publicKey));
    }

    public static KeyPair loadAndroidKeyStoreKeyPairFromKeystore(KeyStore keyStore, String privateKeyAlias, int uid) throws UnrecoverableKeyException {
        return loadAndroidKeyStoreKeyPairFromKeystore(keyStore, privateKeyAlias, uid, getKeyCharacteristics(keyStore, privateKeyAlias, uid));
    }

    private static AndroidKeyStorePrivateKey loadAndroidKeyStorePrivateKeyFromKeystore(KeyStore keyStore, String privateKeyAlias, int uid, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        return (AndroidKeyStorePrivateKey) loadAndroidKeyStoreKeyPairFromKeystore(keyStore, privateKeyAlias, uid, keyCharacteristics).getPrivate();
    }

    public static AndroidKeyStorePrivateKey loadAndroidKeyStorePrivateKeyFromKeystore(KeyStore keyStore, String privateKeyAlias, int uid) throws UnrecoverableKeyException {
        return loadAndroidKeyStorePrivateKeyFromKeystore(keyStore, privateKeyAlias, uid, getKeyCharacteristics(keyStore, privateKeyAlias, uid));
    }

    private static AndroidKeyStoreSecretKey loadAndroidKeyStoreSecretKeyFromKeystore(String secretKeyAlias, int uid, KeyCharacteristics keyCharacteristics) throws UnrecoverableKeyException {
        Integer keymasterAlgorithm = keyCharacteristics.getEnum(KeymasterDefs.KM_TAG_ALGORITHM);
        if (keymasterAlgorithm != null) {
            int keymasterDigest;
            List<Integer> keymasterDigests = keyCharacteristics.getEnums(KeymasterDefs.KM_TAG_DIGEST);
            if (keymasterDigests.isEmpty()) {
                keymasterDigest = -1;
            } else {
                keymasterDigest = ((Integer) keymasterDigests.get(0)).intValue();
            }
            try {
                return new AndroidKeyStoreSecretKey(secretKeyAlias, uid, KeyAlgorithm.fromKeymasterSecretKeyAlgorithm(keymasterAlgorithm.intValue(), keymasterDigest));
            } catch (IllegalArgumentException e) {
                throw ((UnrecoverableKeyException) new UnrecoverableKeyException("Unsupported secret key type").initCause(e));
            }
        }
        throw new UnrecoverableKeyException("Key algorithm unknown");
    }

    public static AndroidKeyStoreKey loadAndroidKeyStoreKeyFromKeystore(KeyStore keyStore, String userKeyAlias, int uid) throws UnrecoverableKeyException {
        KeyCharacteristics keyCharacteristics = getKeyCharacteristics(keyStore, userKeyAlias, uid);
        Integer keymasterAlgorithm = keyCharacteristics.getEnum(KeymasterDefs.KM_TAG_ALGORITHM);
        if (keymasterAlgorithm == null) {
            throw new UnrecoverableKeyException("Key algorithm unknown");
        } else if (keymasterAlgorithm.intValue() == 128 || keymasterAlgorithm.intValue() == 32 || keymasterAlgorithm.intValue() == 33) {
            return loadAndroidKeyStoreSecretKeyFromKeystore(userKeyAlias, uid, keyCharacteristics);
        } else {
            if (keymasterAlgorithm.intValue() == 1 || keymasterAlgorithm.intValue() == 3) {
                return loadAndroidKeyStorePrivateKeyFromKeystore(keyStore, userKeyAlias, uid, keyCharacteristics);
            }
            throw new UnrecoverableKeyException("Key algorithm unknown");
        }
    }

    public static java.security.KeyStore getKeyStoreForUid(int uid) throws KeyStoreException, NoSuchProviderException {
        java.security.KeyStore result = java.security.KeyStore.getInstance("AndroidKeyStore", "AndroidKeyStore");
        try {
            result.load(new AndroidKeyStoreLoadStoreParameter(uid));
            return result;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to load AndroidKeyStore KeyStore for UID ");
            stringBuilder.append(uid);
            throw new KeyStoreException(stringBuilder.toString(), e);
        }
    }
}
