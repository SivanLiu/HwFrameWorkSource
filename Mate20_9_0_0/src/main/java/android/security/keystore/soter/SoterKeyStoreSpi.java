package android.security.keystore.soter;

import android.security.keystore.SoterKeyStoreProvider;
import com.huawei.security.HwCredentials;
import com.huawei.security.HwKeystoreManager;
import com.huawei.security.keystore.HwUniversalKeyStoreProvider;
import com.huawei.security.keystore.HwUniversalKeyStoreSpi;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class SoterKeyStoreSpi extends HwUniversalKeyStoreSpi {
    public static final String TAG = "HwUniversalKeyStore";

    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (!isPrivateKeyEntry(alias)) {
            return null;
        }
        String privateKeyAlias = new StringBuilder();
        privateKeyAlias.append(HwCredentials.USER_PRIVATE_KEY);
        privateKeyAlias.append(alias);
        privateKeyAlias = privateKeyAlias.toString();
        if (password == null || !"from_soter_ui".equals(String.valueOf(password))) {
            return HwUniversalKeyStoreProvider.loadAndroidKeyStorePrivateKeyFromKeystore(getKeyStoreManager(), privateKeyAlias, getUid());
        }
        return SoterKeyStoreProvider.loadAndroidKeyStorePublicKeyFromKeystore(getKeyStoreManager(), privateKeyAlias, getUid(), 1);
    }

    private boolean isPrivateKeyEntry(String alias) {
        if (alias != null) {
            HwKeystoreManager keyStoreManager = getKeyStoreManager();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(HwCredentials.USER_PRIVATE_KEY);
            stringBuilder.append(alias);
            return keyStoreManager.contains(stringBuilder.toString(), getUid());
        }
        throw new NullPointerException("alias == null");
    }
}
