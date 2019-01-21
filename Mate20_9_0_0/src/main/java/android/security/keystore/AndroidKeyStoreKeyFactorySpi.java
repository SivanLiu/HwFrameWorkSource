package android.security.keystore;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.storage.VolumeInfo;
import android.security.Credentials;
import android.security.KeyStore;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class AndroidKeyStoreKeyFactorySpi extends KeyFactorySpi {
    private final KeyStore mKeyStore = KeyStore.getInstance();

    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpecClass) throws InvalidKeySpecException {
        StringBuilder stringBuilder;
        if (key == null) {
            throw new InvalidKeySpecException("key == null");
        } else if (!(key instanceof AndroidKeyStorePrivateKey) && !(key instanceof AndroidKeyStorePublicKey)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key type: ");
            stringBuilder.append(key.getClass().getName());
            stringBuilder.append(". This KeyFactory supports only Android Keystore asymmetric keys");
            throw new InvalidKeySpecException(stringBuilder.toString());
        } else if (keySpecClass == null) {
            throw new InvalidKeySpecException("keySpecClass == null");
        } else if (KeyInfo.class.equals(keySpecClass)) {
            if (key instanceof AndroidKeyStorePrivateKey) {
                AndroidKeyStorePrivateKey keystorePrivateKey = (AndroidKeyStorePrivateKey) key;
                String keyAliasInKeystore = keystorePrivateKey.getAlias();
                if (keyAliasInKeystore.startsWith(Credentials.USER_PRIVATE_KEY)) {
                    return AndroidKeyStoreSecretKeyFactorySpi.getKeyInfo(this.mKeyStore, keyAliasInKeystore.substring(Credentials.USER_PRIVATE_KEY.length()), keyAliasInKeystore, keystorePrivateKey.getUid());
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid key alias: ");
                stringBuilder2.append(keyAliasInKeystore);
                throw new InvalidKeySpecException(stringBuilder2.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key type: ");
            stringBuilder.append(key.getClass().getName());
            stringBuilder.append(". KeyInfo can be obtained only for Android Keystore private keys");
            throw new InvalidKeySpecException(stringBuilder.toString());
        } else if (X509EncodedKeySpec.class.equals(keySpecClass)) {
            if (key instanceof AndroidKeyStorePublicKey) {
                return new X509EncodedKeySpec(((AndroidKeyStorePublicKey) key).getEncoded());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key type: ");
            stringBuilder.append(key.getClass().getName());
            stringBuilder.append(". X509EncodedKeySpec can be obtained only for Android Keystore public keys");
            throw new InvalidKeySpecException(stringBuilder.toString());
        } else if (PKCS8EncodedKeySpec.class.equals(keySpecClass)) {
            if (key instanceof AndroidKeyStorePrivateKey) {
                throw new InvalidKeySpecException("Key material export of Android Keystore private keys is not supported");
            }
            throw new InvalidKeySpecException("Cannot export key material of public key in PKCS#8 format. Only X.509 format (X509EncodedKeySpec) supported for public keys.");
        } else if (RSAPublicKeySpec.class.equals(keySpecClass)) {
            if (key instanceof AndroidKeyStoreRSAPublicKey) {
                AndroidKeyStoreRSAPublicKey rsaKey = (AndroidKeyStoreRSAPublicKey) key;
                return new RSAPublicKeySpec(rsaKey.getModulus(), rsaKey.getPublicExponent());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Obtaining RSAPublicKeySpec not supported for ");
            stringBuilder.append(key.getAlgorithm());
            stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            stringBuilder.append(key instanceof AndroidKeyStorePrivateKey ? VolumeInfo.ID_PRIVATE_INTERNAL : "public");
            stringBuilder.append(" key");
            throw new InvalidKeySpecException(stringBuilder.toString());
        } else if (!ECPublicKeySpec.class.equals(keySpecClass)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key spec: ");
            stringBuilder.append(keySpecClass.getName());
            throw new InvalidKeySpecException(stringBuilder.toString());
        } else if (key instanceof AndroidKeyStoreECPublicKey) {
            AndroidKeyStoreECPublicKey ecKey = (AndroidKeyStoreECPublicKey) key;
            return new ECPublicKeySpec(ecKey.getW(), ecKey.getParams());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Obtaining ECPublicKeySpec not supported for ");
            stringBuilder.append(key.getAlgorithm());
            stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            stringBuilder.append(key instanceof AndroidKeyStorePrivateKey ? VolumeInfo.ID_PRIVATE_INTERNAL : "public");
            stringBuilder.append(" key");
            throw new InvalidKeySpecException(stringBuilder.toString());
        }
    }

    protected PrivateKey engineGeneratePrivate(KeySpec spec) throws InvalidKeySpecException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("To generate a key pair in Android Keystore, use KeyPairGenerator initialized with ");
        stringBuilder.append(KeyGenParameterSpec.class.getName());
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected PublicKey engineGeneratePublic(KeySpec spec) throws InvalidKeySpecException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("To generate a key pair in Android Keystore, use KeyPairGenerator initialized with ");
        stringBuilder.append(KeyGenParameterSpec.class.getName());
        throw new InvalidKeySpecException(stringBuilder.toString());
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("key == null");
        } else if ((key instanceof AndroidKeyStorePrivateKey) || (key instanceof AndroidKeyStorePublicKey)) {
            return key;
        } else {
            throw new InvalidKeyException("To import a key into Android Keystore, use KeyStore.setEntry");
        }
    }
}
