package android.security.keystore;

import android.hardware.fingerprint.FingerprintManager;
import android.security.GateKeeper;
import android.security.KeyStore;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties.Digest;
import android.util.JlogConstants;
import com.android.internal.util.ArrayUtils;
import java.security.ProviderException;

public abstract class KeymasterUtils {
    private KeymasterUtils() {
    }

    public static int getDigestOutputSizeBits(int keymasterDigest) {
        switch (keymasterDigest) {
            case 0:
                return -1;
            case 1:
                return 128;
            case 2:
                return 160;
            case 3:
                return 224;
            case 4:
                return 256;
            case 5:
                return JlogConstants.JLID_ACTIVITY_START_RECORD_TIME;
            case 6:
                return 512;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown digest: ");
                stringBuilder.append(keymasterDigest);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static boolean isKeymasterBlockModeIndCpaCompatibleWithSymmetricCrypto(int keymasterBlockMode) {
        if (keymasterBlockMode != 32) {
            switch (keymasterBlockMode) {
                case 1:
                    return false;
                case 2:
                case 3:
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported block mode: ");
                    stringBuilder.append(keymasterBlockMode);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return true;
    }

    public static boolean isKeymasterPaddingSchemeIndCpaCompatibleWithAsymmetricCrypto(int keymasterPadding) {
        if (keymasterPadding != 4) {
            switch (keymasterPadding) {
                case 1:
                    return false;
                case 2:
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported asymmetric encryption padding scheme: ");
                    stringBuilder.append(keymasterPadding);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return true;
    }

    public static void addUserAuthArgs(KeymasterArguments args, UserAuthArgs spec) {
        if (spec.isUserConfirmationRequired()) {
            args.addBoolean(KeymasterDefs.KM_TAG_TRUSTED_CONFIRMATION_REQUIRED);
        }
        if (spec.isUserPresenceRequired()) {
            args.addBoolean(KeymasterDefs.KM_TAG_TRUSTED_USER_PRESENCE_REQUIRED);
        }
        if (spec.isUnlockedDeviceRequired()) {
            args.addBoolean(KeymasterDefs.KM_TAG_UNLOCKED_DEVICE_REQUIRED);
        }
        if (spec.isUserAuthenticationRequired()) {
            if (spec.getUserAuthenticationValidityDurationSeconds() == -1) {
                FingerprintManager fingerprintManager = (FingerprintManager) KeyStore.getApplicationContext().getSystemService(FingerprintManager.class);
                long fingerprintOnlySid = fingerprintManager != null ? fingerprintManager.getAuthenticatorId() : 0;
                if (fingerprintOnlySid != 0) {
                    long sid;
                    if (spec.getBoundToSpecificSecureUserId() != 0) {
                        sid = spec.getBoundToSpecificSecureUserId();
                    } else if (spec.isInvalidatedByBiometricEnrollment()) {
                        sid = fingerprintOnlySid;
                    } else {
                        sid = getRootSid();
                    }
                    args.addUnsignedLong(KeymasterDefs.KM_TAG_USER_SECURE_ID, KeymasterArguments.toUint64(sid));
                    args.addEnum(KeymasterDefs.KM_TAG_USER_AUTH_TYPE, 2);
                    if (spec.isUserAuthenticationValidWhileOnBody()) {
                        throw new ProviderException("Key validity extension while device is on-body is not supported for keys requiring fingerprint authentication");
                    }
                }
                throw new IllegalStateException("At least one fingerprint must be enrolled to create keys requiring user authentication for every use");
            }
            long sid2;
            if (spec.getBoundToSpecificSecureUserId() != 0) {
                sid2 = spec.getBoundToSpecificSecureUserId();
            } else {
                sid2 = getRootSid();
            }
            args.addUnsignedLong(KeymasterDefs.KM_TAG_USER_SECURE_ID, KeymasterArguments.toUint64(sid2));
            args.addEnum(KeymasterDefs.KM_TAG_USER_AUTH_TYPE, 3);
            args.addUnsignedInt(KeymasterDefs.KM_TAG_AUTH_TIMEOUT, (long) spec.getUserAuthenticationValidityDurationSeconds());
            if (spec.isUserAuthenticationValidWhileOnBody()) {
                args.addBoolean(KeymasterDefs.KM_TAG_ALLOW_WHILE_ON_BODY);
            }
            return;
        }
        args.addBoolean(KeymasterDefs.KM_TAG_NO_AUTH_REQUIRED);
    }

    public static void addMinMacLengthAuthorizationIfNecessary(KeymasterArguments args, int keymasterAlgorithm, int[] keymasterBlockModes, int[] keymasterDigests) {
        if (keymasterAlgorithm != 32) {
            if (keymasterAlgorithm == 128) {
                if (keymasterDigests.length == 1) {
                    int keymasterDigest = keymasterDigests[0];
                    int digestOutputSizeBits = getDigestOutputSizeBits(keymasterDigest);
                    if (digestOutputSizeBits != -1) {
                        args.addUnsignedInt(KeymasterDefs.KM_TAG_MIN_MAC_LENGTH, (long) digestOutputSizeBits);
                        return;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HMAC key authorized for unsupported digest: ");
                    stringBuilder.append(Digest.fromKeymaster(keymasterDigest));
                    throw new ProviderException(stringBuilder.toString());
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unsupported number of authorized digests for HMAC key: ");
                stringBuilder2.append(keymasterDigests.length);
                stringBuilder2.append(". Exactly one digest must be authorized");
                throw new ProviderException(stringBuilder2.toString());
            }
        } else if (ArrayUtils.contains(keymasterBlockModes, 32)) {
            args.addUnsignedInt(KeymasterDefs.KM_TAG_MIN_MAC_LENGTH, 96);
        }
    }

    private static long getRootSid() {
        long rootSid = GateKeeper.getSecureUserId();
        if (rootSid != 0) {
            return rootSid;
        }
        throw new IllegalStateException("Secure lock screen must be enabled to create keys requiring user authentication");
    }
}
