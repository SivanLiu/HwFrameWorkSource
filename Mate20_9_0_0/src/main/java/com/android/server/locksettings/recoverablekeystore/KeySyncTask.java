package com.android.server.locksettings.recoverablekeystore;

import android.content.Context;
import android.security.Scrypt;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainProtectionParams.Builder;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.KeyDerivationParams;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySnapshotStorage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class KeySyncTask implements Runnable {
    private static final int LENGTH_PREFIX_BYTES = 4;
    private static final String LOCK_SCREEN_HASH_ALGORITHM = "SHA-256";
    private static final String RECOVERY_KEY_ALGORITHM = "AES";
    private static final int RECOVERY_KEY_SIZE_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    @VisibleForTesting
    static final int SCRYPT_PARAM_N = 4096;
    @VisibleForTesting
    static final int SCRYPT_PARAM_OUTLEN_BYTES = 32;
    @VisibleForTesting
    static final int SCRYPT_PARAM_P = 1;
    @VisibleForTesting
    static final int SCRYPT_PARAM_R = 8;
    private static final String TAG = "KeySyncTask";
    private static final int TRUSTED_HARDWARE_MAX_ATTEMPTS = 10;
    private final String mCredential;
    private final int mCredentialType;
    private final boolean mCredentialUpdated;
    private final PlatformKeyManager mPlatformKeyManager;
    private final RecoverableKeyStoreDb mRecoverableKeyStoreDb;
    private final RecoverySnapshotStorage mRecoverySnapshotStorage;
    private final Scrypt mScrypt;
    private final RecoverySnapshotListenersStorage mSnapshotListenersStorage;
    private final TestOnlyInsecureCertificateHelper mTestOnlyInsecureCertificateHelper;
    private final int mUserId;

    public static KeySyncTask newInstance(Context context, RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySnapshotStorage snapshotStorage, RecoverySnapshotListenersStorage recoverySnapshotListenersStorage, int userId, int credentialType, String credential, boolean credentialUpdated) throws NoSuchAlgorithmException, KeyStoreException, InsecureUserException {
        return new KeySyncTask(recoverableKeyStoreDb, snapshotStorage, recoverySnapshotListenersStorage, userId, credentialType, credential, credentialUpdated, PlatformKeyManager.getInstance(context, recoverableKeyStoreDb), new TestOnlyInsecureCertificateHelper(), new Scrypt());
    }

    @VisibleForTesting
    KeySyncTask(RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySnapshotStorage snapshotStorage, RecoverySnapshotListenersStorage recoverySnapshotListenersStorage, int userId, int credentialType, String credential, boolean credentialUpdated, PlatformKeyManager platformKeyManager, TestOnlyInsecureCertificateHelper testOnlyInsecureCertificateHelper, Scrypt scrypt) {
        this.mSnapshotListenersStorage = recoverySnapshotListenersStorage;
        this.mRecoverableKeyStoreDb = recoverableKeyStoreDb;
        this.mUserId = userId;
        this.mCredentialType = credentialType;
        this.mCredential = credential;
        this.mCredentialUpdated = credentialUpdated;
        this.mPlatformKeyManager = platformKeyManager;
        this.mRecoverySnapshotStorage = snapshotStorage;
        this.mTestOnlyInsecureCertificateHelper = testOnlyInsecureCertificateHelper;
        this.mScrypt = scrypt;
    }

    public void run() {
        try {
            synchronized (KeySyncTask.class) {
                syncKeys();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception thrown during KeySyncTask", e);
        }
    }

    private void syncKeys() {
        String str;
        StringBuilder stringBuilder;
        if (this.mCredentialType == -1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Credentials are not set for user ");
            stringBuilder.append(this.mUserId);
            Log.w(str, stringBuilder.toString());
            this.mPlatformKeyManager.invalidatePlatformKey(this.mUserId, this.mPlatformKeyManager.getGenerationId(this.mUserId));
        } else if (isCustomLockScreen()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported credential type ");
            stringBuilder.append(this.mCredentialType);
            stringBuilder.append("for user ");
            stringBuilder.append(this.mUserId);
            Log.w(str, stringBuilder.toString());
            this.mRecoverableKeyStoreDb.invalidateKeysForUserIdOnCustomScreenLock(this.mUserId);
        } else {
            List<Integer> recoveryAgents = this.mRecoverableKeyStoreDb.getRecoveryAgents(this.mUserId);
            for (Integer uid : recoveryAgents) {
                int uid2 = uid.intValue();
                try {
                    syncKeysForAgent(uid2);
                } catch (IOException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("IOException during sync for agent ");
                    stringBuilder2.append(uid2);
                    Log.e(str2, stringBuilder2.toString(), e);
                }
            }
            if (recoveryAgents.isEmpty()) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("No recovery agent initialized for user ");
                stringBuilder3.append(this.mUserId);
                Log.w(str3, stringBuilder3.toString());
            }
        }
    }

    private boolean isCustomLockScreen() {
        return (this.mCredentialType == -1 || this.mCredentialType == 1 || this.mCredentialType == 2) ? false : true;
    }

    private void syncKeysForAgent(int recoveryAgentUid) throws IOException {
        boolean z;
        PublicKey publicKey;
        byte[] bArr;
        PublicKey publicKey2;
        boolean z2;
        byte[] bArr2;
        NoSuchAlgorithmException noSuchAlgorithmException;
        String str;
        GeneralSecurityException generalSecurityException;
        int i = recoveryAgentUid;
        boolean shouldRecreateCurrentVersion = false;
        if (!shouldCreateSnapshot(recoveryAgentUid)) {
            z = this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, i) != null && this.mRecoverySnapshotStorage.get(i) == null;
            shouldRecreateCurrentVersion = z;
            if (shouldRecreateCurrentVersion) {
                Log.d(TAG, "Recreating most recent snapshot");
            } else {
                Log.d(TAG, "Key sync not needed.");
                return;
            }
        }
        z = shouldRecreateCurrentVersion;
        String rootCertAlias = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(this.mRecoverableKeyStoreDb.getActiveRootOfTrust(this.mUserId, i));
        CertPath certPath = this.mRecoverableKeyStoreDb.getRecoveryServiceCertPath(this.mUserId, i, rootCertAlias);
        if (certPath != null) {
            Log.d(TAG, "Using the public key in stored CertPath for syncing");
            publicKey = ((Certificate) certPath.getCertificates().get(0)).getPublicKey();
        } else {
            Log.d(TAG, "Using the stored raw public key for syncing");
            publicKey = this.mRecoverableKeyStoreDb.getRecoveryServicePublicKey(this.mUserId, i);
        }
        PublicKey publicKey3 = publicKey;
        if (publicKey3 == null) {
            Log.w(TAG, "Not initialized for KeySync: no public key set. Cancelling task.");
            return;
        }
        byte[] vaultHandle = this.mRecoverableKeyStoreDb.getServerParams(this.mUserId, i);
        String str2;
        StringBuilder stringBuilder;
        if (vaultHandle == null) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("No device ID set for user ");
            stringBuilder.append(this.mUserId);
            Log.w(str2, stringBuilder.toString());
            return;
        }
        byte[] localLskfHash;
        if (this.mTestOnlyInsecureCertificateHelper.isTestOnlyCertificateAlias(rootCertAlias)) {
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Insecure root certificate is used by recovery agent ");
            stringBuilder2.append(i);
            Log.w(str2, stringBuilder2.toString());
            if (this.mTestOnlyInsecureCertificateHelper.doesCredentialSupportInsecureMode(this.mCredentialType, this.mCredential)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Whitelisted credential is used to generate snapshot by recovery agent ");
                stringBuilder2.append(i);
                Log.w(str2, stringBuilder2.toString());
            } else {
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Non whitelisted credential is used to generate recovery snapshot by ");
                stringBuilder.append(i);
                stringBuilder.append(" - ignore attempt.");
                Log.w(str2, stringBuilder.toString());
                return;
            }
        }
        boolean useScryptToHashCredential = shouldUseScryptToHashCredential();
        byte[] salt = generateSalt();
        if (useScryptToHashCredential) {
            localLskfHash = hashCredentialsByScrypt(salt, this.mCredential);
        } else {
            localLskfHash = hashCredentialsBySaltedSha256(salt, this.mCredential);
        }
        byte[] localLskfHash2 = localLskfHash;
        try {
            Map<String, SecretKey> rawKeys = getKeysToSync(recoveryAgentUid);
            if (this.mTestOnlyInsecureCertificateHelper.isTestOnlyCertificateAlias(rootCertAlias)) {
                rawKeys = this.mTestOnlyInsecureCertificateHelper.keepOnlyWhitelistedInsecureKeys(rawKeys);
            }
            Map<String, SecretKey> rawKeys2 = rawKeys;
            try {
                SecretKey recoveryKey = generateRecoveryKey();
                try {
                    Long counterId;
                    Map<String, byte[]> encryptedApplicationKeys = KeySyncUtils.encryptKeysWithRecoveryKey(recoveryKey, rawKeys2);
                    if (this.mCredentialUpdated) {
                        counterId = Long.valueOf(generateAndStoreCounterId(recoveryAgentUid));
                    } else {
                        counterId = this.mRecoverableKeyStoreDb.getCounterId(this.mUserId, i);
                        if (counterId == null) {
                            counterId = Long.valueOf(generateAndStoreCounterId(recoveryAgentUid));
                        }
                    }
                    Long counterId2 = counterId;
                    CertPath certPath2 = certPath;
                    byte[] vaultParams = KeySyncUtils.packVaultParams(publicKey3, counterId2.longValue(), 10, vaultHandle);
                    try {
                        KeyDerivationParams keyDerivationParams;
                        certPath = KeySyncUtils.thmEncryptRecoveryKey(publicKey3, localLskfHash2, vaultParams, recoveryKey);
                        if (useScryptToHashCredential) {
                            keyDerivationParams = KeyDerivationParams.createScryptParams(salt, 4096);
                        } else {
                            keyDerivationParams = KeyDerivationParams.createSha256Params(salt);
                        }
                        KeyDerivationParams keyDerivationParams2 = keyDerivationParams;
                        KeyChainProtectionParams keyChainProtectionParams = new Builder().setUserSecretType(100).setLockScreenUiFormat(getUiFormat(this.mCredentialType, this.mCredential)).setKeyDerivationParams(keyDerivationParams2).setSecret(new byte[0]).build();
                        ArrayList publicKey4 = new ArrayList();
                        publicKey4.add(keyChainProtectionParams);
                        vaultParams = new KeyChainSnapshot.Builder().setSnapshotVersion(getSnapshotVersion(i, z)).setMaxAttempts(10).setCounterId(counterId2.longValue()).setServerParams(vaultHandle).setKeyChainProtectionParams(publicKey4).setWrappedApplicationKeys(createApplicationKeyEntries(encryptedApplicationKeys)).setEncryptedRecoveryKeyBlob(certPath);
                        try {
                            vaultParams.setTrustedHardwareCertPath(certPath2);
                            this.mRecoverySnapshotStorage.put(i, vaultParams.build());
                            this.mSnapshotListenersStorage.recoverySnapshotAvailable(i);
                            this.mRecoverableKeyStoreDb.setShouldCreateSnapshot(this.mUserId, i, false);
                        } catch (CertificateException e) {
                            CertificateException certificateException = e;
                            Log.wtf(TAG, "Cannot serialize CertPath when calling setTrustedHardwareCertPath", e);
                        }
                    } catch (NoSuchAlgorithmException e2) {
                        bArr = vaultParams;
                        publicKey2 = publicKey3;
                        z2 = useScryptToHashCredential;
                        bArr2 = salt;
                        useScryptToHashCredential = certPath2;
                        noSuchAlgorithmException = e2;
                        Log.wtf(TAG, "SecureBox encrypt algorithms unavailable", e2);
                    } catch (InvalidKeyException e3) {
                        bArr = vaultParams;
                        publicKey2 = publicKey3;
                        z2 = useScryptToHashCredential;
                        bArr2 = salt;
                        useScryptToHashCredential = certPath2;
                        InvalidKeyException invalidKeyException = e3;
                        Log.e(TAG, "Could not encrypt with recovery key", e3);
                    }
                } catch (InvalidKeyException | NoSuchAlgorithmException e4) {
                    str = rootCertAlias;
                    publicKey2 = publicKey3;
                    z2 = useScryptToHashCredential;
                    bArr2 = salt;
                    useScryptToHashCredential = certPath;
                    generalSecurityException = e4;
                    Log.wtf(TAG, "Should be impossible: could not encrypt application keys with random key", e4);
                }
            } catch (NoSuchAlgorithmException e22) {
                str = rootCertAlias;
                publicKey2 = publicKey3;
                z2 = useScryptToHashCredential;
                bArr2 = salt;
                useScryptToHashCredential = certPath;
                noSuchAlgorithmException = e22;
                Log.wtf("AES should never be unavailable", e22);
            }
        } catch (GeneralSecurityException e42) {
            str = rootCertAlias;
            publicKey2 = publicKey3;
            z2 = useScryptToHashCredential;
            bArr2 = salt;
            generalSecurityException = e42;
            Log.e(TAG, "Failed to load recoverable keys for sync", e42);
        } catch (InsecureUserException e5) {
            str = rootCertAlias;
            publicKey2 = publicKey3;
            z2 = useScryptToHashCredential;
            bArr2 = salt;
            useScryptToHashCredential = certPath;
            InsecureUserException insecureUserException = e5;
            Log.e(TAG, "A screen unlock triggered the key sync flow, so user must have lock screen. This should be impossible.", e5);
        } catch (BadPlatformKeyException e6) {
            str = rootCertAlias;
            publicKey2 = publicKey3;
            z2 = useScryptToHashCredential;
            bArr2 = salt;
            useScryptToHashCredential = certPath;
            BadPlatformKeyException badPlatformKeyException = e6;
            Log.e(TAG, "Loaded keys for same generation ID as platform key, so BadPlatformKeyException should be impossible.", e6);
        } catch (IOException e7) {
            str = rootCertAlias;
            publicKey2 = publicKey3;
            z2 = useScryptToHashCredential;
            bArr2 = salt;
            useScryptToHashCredential = certPath;
            IOException iOException = e7;
            Log.e(TAG, "Local database error.", e7);
        }
    }

    @VisibleForTesting
    int getSnapshotVersion(int recoveryAgentUid, boolean shouldRecreateCurrentVersion) throws IOException {
        Long snapshotVersion = this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, recoveryAgentUid);
        long j = 1;
        if (shouldRecreateCurrentVersion) {
            if (snapshotVersion != null) {
                j = snapshotVersion.longValue();
            }
            snapshotVersion = Long.valueOf(j);
        } else {
            if (snapshotVersion != null) {
                j = 1 + snapshotVersion.longValue();
            }
            snapshotVersion = Long.valueOf(j);
        }
        if (this.mRecoverableKeyStoreDb.setSnapshotVersion(this.mUserId, recoveryAgentUid, snapshotVersion.longValue()) >= 0) {
            return snapshotVersion.intValue();
        }
        Log.e(TAG, "Failed to set the snapshot version in the local DB.");
        throw new IOException("Failed to set the snapshot version in the local DB.");
    }

    private long generateAndStoreCounterId(int recoveryAgentUid) throws IOException {
        long counter = new SecureRandom().nextLong();
        if (this.mRecoverableKeyStoreDb.setCounterId(this.mUserId, recoveryAgentUid, counter) >= 0) {
            return counter;
        }
        Log.e(TAG, "Failed to set the snapshot version in the local DB.");
        throw new IOException("Failed to set counterId in the local DB.");
    }

    private Map<String, SecretKey> getKeysToSync(int recoveryAgentUid) throws InsecureUserException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPlatformKeyException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        PlatformDecryptionKey decryptKey = this.mPlatformKeyManager.getDecryptKey(this.mUserId);
        return WrappedKey.unwrapKeys(decryptKey, this.mRecoverableKeyStoreDb.getAllKeys(this.mUserId, recoveryAgentUid, decryptKey.getGenerationId()));
    }

    private boolean shouldCreateSnapshot(int recoveryAgentUid) {
        if (!ArrayUtils.contains(this.mRecoverableKeyStoreDb.getRecoverySecretTypes(this.mUserId, recoveryAgentUid), 100)) {
            return false;
        }
        if (!this.mCredentialUpdated || this.mRecoverableKeyStoreDb.getSnapshotVersion(this.mUserId, recoveryAgentUid) == null) {
            return this.mRecoverableKeyStoreDb.getShouldCreateSnapshot(this.mUserId, recoveryAgentUid);
        }
        this.mRecoverableKeyStoreDb.setShouldCreateSnapshot(this.mUserId, recoveryAgentUid, true);
        return true;
    }

    @VisibleForTesting
    static int getUiFormat(int credentialType, String credential) {
        if (credentialType == 1) {
            return 3;
        }
        if (isPin(credential)) {
            return 1;
        }
        return 2;
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    @VisibleForTesting
    static boolean isPin(String credential) {
        if (credential == null) {
            return false;
        }
        int length = credential.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isDigit(credential.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    static byte[] hashCredentialsBySaltedSha256(byte[] salt, String credentials) {
        byte[] credentialsBytes = credentials.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate((salt.length + credentialsBytes.length) + 8);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(salt.length);
        byteBuffer.put(salt);
        byteBuffer.putInt(credentialsBytes.length);
        byteBuffer.put(credentialsBytes);
        try {
            return MessageDigest.getInstance(LOCK_SCREEN_HASH_ALGORITHM).digest(byteBuffer.array());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] hashCredentialsByScrypt(byte[] salt, String credentials) {
        return this.mScrypt.scrypt(credentials.getBytes(StandardCharsets.UTF_8), salt, 4096, 8, 1, 32);
    }

    private static SecretKey generateRecoveryKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(RECOVERY_KEY_ALGORITHM);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private static List<WrappedApplicationKey> createApplicationKeyEntries(Map<String, byte[]> encryptedApplicationKeys) {
        ArrayList<WrappedApplicationKey> keyEntries = new ArrayList();
        for (String alias : encryptedApplicationKeys.keySet()) {
            keyEntries.add(new WrappedApplicationKey.Builder().setAlias(alias).setEncryptedKeyMaterial((byte[]) encryptedApplicationKeys.get(alias)).build());
        }
        return keyEntries;
    }

    private boolean shouldUseScryptToHashCredential() {
        return this.mCredentialType == 2;
    }
}
