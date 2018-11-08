package com.huawei.security.keystore;

import android.security.keystore.UserNotAuthenticatedException;
import com.huawei.security.HwKeystoreManager;
import com.huawei.security.keymaster.HwKeymasterDefs;
import com.huawei.security.keystore.ArrayUtils.EmptyArray;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;

public class HwUniversalKeyStoreCryptoOperationUtils {
    private static volatile SecureRandom sRng;

    private HwUniversalKeyStoreCryptoOperationUtils() {
    }

    static InvalidKeyException getInvalidKeyExceptionForInit(HwKeystoreManager keyStore, HwUniversalKeyStoreKey key, int beginOpResultCode) {
        if (beginOpResultCode == 1) {
            return null;
        }
        InvalidKeyException e = keyStore.getInvalidKeyException(key.getAlias(), key.getUid(), beginOpResultCode);
        switch (beginOpResultCode) {
            case HwKeystoreManager.OP_AUTH_NEEDED /*15*/:
                if (e instanceof UserNotAuthenticatedException) {
                    return null;
                }
                break;
        }
        return e;
    }

    public static GeneralSecurityException getExceptionForCipherInit(HwKeystoreManager keyStore, HwUniversalKeyStoreKey key, int beginOpResultCode) {
        if (beginOpResultCode == 1) {
            return null;
        }
        switch (beginOpResultCode) {
            case HwKeymasterDefs.KM_ERROR_CALLER_NONCE_PROHIBITED /*-55*/:
                return new InvalidAlgorithmParameterException("Caller-provided IV not permitted");
            case HwKeymasterDefs.KM_ERROR_INVALID_NONCE /*-52*/:
                return new InvalidAlgorithmParameterException("Invalid IV");
            default:
                return getInvalidKeyExceptionForInit(keyStore, key, beginOpResultCode);
        }
    }

    static byte[] getRandomBytesToMixIntoKeystoreRng(SecureRandom rng, int sizeBytes) {
        if (sizeBytes <= 0) {
            return EmptyArray.BYTE;
        }
        if (rng == null) {
            rng = getRng();
        }
        byte[] result = new byte[sizeBytes];
        rng.nextBytes(result);
        return result;
    }

    private static SecureRandom getRng() {
        if (sRng == null) {
            sRng = new SecureRandom();
        }
        return sRng;
    }
}
