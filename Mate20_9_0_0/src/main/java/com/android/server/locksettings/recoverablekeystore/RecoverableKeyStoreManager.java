package com.android.server.locksettings.recoverablekeystore;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.security.KeyStore;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.RecoveryCertPath;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import com.android.internal.util.Preconditions;
import com.android.server.locksettings.recoverablekeystore.certificate.CertParsingException;
import com.android.server.locksettings.recoverablekeystore.certificate.CertUtils;
import com.android.server.locksettings.recoverablekeystore.certificate.CertValidationException;
import com.android.server.locksettings.recoverablekeystore.certificate.CertXml;
import com.android.server.locksettings.recoverablekeystore.certificate.SigXml;
import com.android.server.locksettings.recoverablekeystore.storage.ApplicationKeyStorage;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverableKeyStoreDb;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySessionStorage;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySessionStorage.Entry;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySnapshotStorage;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.AEADBadTagException;

public class RecoverableKeyStoreManager {
    private static final String TAG = "RecoverableKeyStoreMgr";
    private static RecoverableKeyStoreManager mInstance;
    private final ApplicationKeyStorage mApplicationKeyStorage;
    private final Context mContext;
    private final RecoverableKeyStoreDb mDatabase;
    private final ExecutorService mExecutorService;
    private final RecoverySnapshotListenersStorage mListenersStorage;
    private final PlatformKeyManager mPlatformKeyManager;
    private final RecoverableKeyGenerator mRecoverableKeyGenerator;
    private final RecoverySessionStorage mRecoverySessionStorage;
    private final RecoverySnapshotStorage mSnapshotStorage;
    private final TestOnlyInsecureCertificateHelper mTestCertHelper;

    public static synchronized RecoverableKeyStoreManager getInstance(Context context, KeyStore keystore) {
        RecoverableKeyStoreManager recoverableKeyStoreManager;
        synchronized (RecoverableKeyStoreManager.class) {
            if (mInstance == null) {
                RecoverableKeyStoreDb db = RecoverableKeyStoreDb.newInstance(context);
                try {
                    mInstance = new RecoverableKeyStoreManager(context.getApplicationContext(), db, new RecoverySessionStorage(), Executors.newSingleThreadExecutor(), RecoverySnapshotStorage.newInstance(), new RecoverySnapshotListenersStorage(), PlatformKeyManager.getInstance(context, db), ApplicationKeyStorage.getInstance(keystore), new TestOnlyInsecureCertificateHelper());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (KeyStoreException e2) {
                    throw new ServiceSpecificException(22, e2.getMessage());
                }
            }
            recoverableKeyStoreManager = mInstance;
        }
        return recoverableKeyStoreManager;
    }

    @VisibleForTesting
    RecoverableKeyStoreManager(Context context, RecoverableKeyStoreDb recoverableKeyStoreDb, RecoverySessionStorage recoverySessionStorage, ExecutorService executorService, RecoverySnapshotStorage snapshotStorage, RecoverySnapshotListenersStorage listenersStorage, PlatformKeyManager platformKeyManager, ApplicationKeyStorage applicationKeyStorage, TestOnlyInsecureCertificateHelper TestOnlyInsecureCertificateHelper) {
        this.mContext = context;
        this.mDatabase = recoverableKeyStoreDb;
        this.mRecoverySessionStorage = recoverySessionStorage;
        this.mExecutorService = executorService;
        this.mListenersStorage = listenersStorage;
        this.mSnapshotStorage = snapshotStorage;
        this.mPlatformKeyManager = platformKeyManager;
        this.mApplicationKeyStorage = applicationKeyStorage;
        this.mTestCertHelper = TestOnlyInsecureCertificateHelper;
        try {
            this.mRecoverableKeyGenerator = RecoverableKeyGenerator.newInstance(this.mDatabase);
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(TAG, "AES keygen algorithm not available. AOSP must support this.", e);
            throw new ServiceSpecificException(22, e.getMessage());
        }
    }

    @VisibleForTesting
    void initRecoveryService(String rootCertificateAlias, byte[] recoveryServiceCertFile) throws RemoteException {
        int rootCertificateAlias2;
        CertificateEncodingException e;
        long j;
        checkRecoverKeyStorePermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        String rootCertificateAlias3 = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(rootCertificateAlias);
        if (this.mTestCertHelper.isValidRootCertificateAlias(rootCertificateAlias3)) {
            String str;
            String activeRootAlias = this.mDatabase.getActiveRootOfTrust(userId, uid);
            StringBuilder stringBuilder;
            if (activeRootAlias == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Root of trust for recovery agent + ");
                stringBuilder.append(uid);
                stringBuilder.append(" is assigned for the first time to ");
                stringBuilder.append(rootCertificateAlias3);
                Log.d(str, stringBuilder.toString());
            } else if (!activeRootAlias.equals(rootCertificateAlias3)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Root of trust for recovery agent ");
                stringBuilder.append(uid);
                stringBuilder.append(" is changed to ");
                stringBuilder.append(rootCertificateAlias3);
                stringBuilder.append(" from  ");
                stringBuilder.append(activeRootAlias);
                Log.i(str, stringBuilder.toString());
            }
            if (this.mDatabase.setActiveRootOfTrust(userId, uid, rootCertificateAlias3) >= 0) {
                String str2;
                try {
                    CertXml certXml = CertXml.parse(recoveryServiceCertFile);
                    long newSerial = certXml.getSerial();
                    Long oldSerial = this.mDatabase.getRecoveryServiceCertSerial(userId, uid, rootCertificateAlias3);
                    if (oldSerial == null || oldSerial.longValue() < newSerial || this.mTestCertHelper.isTestOnlyCertificateAlias(rootCertificateAlias3)) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Updating the certificate with the new serial number ");
                        stringBuilder2.append(newSerial);
                        Log.i(str, stringBuilder2.toString());
                        X509Certificate rootCert = this.mTestCertHelper.getRootCertificate(rootCertificateAlias3);
                        CertXml certXml2;
                        try {
                            Log.d(TAG, "Getting and validating a random endpoint certificate");
                            CertPath certPath = certXml.getRandomEndpointCert(rootCert);
                            CertPath certPath2;
                            try {
                                Log.d(TAG, "Saving the randomly chosen endpoint certificate to database");
                                long updatedCertPathRows = this.mDatabase.setRecoveryServiceCertPath(userId, uid, rootCertificateAlias3, certPath);
                                if (updatedCertPathRows > 0) {
                                    String str3 = rootCertificateAlias3;
                                    rootCertificateAlias2 = 25;
                                    try {
                                        if (this.mDatabase.setRecoveryServiceCertSerial(userId, uid, str3, newSerial) >= 0) {
                                            if (this.mDatabase.getSnapshotVersion(userId, uid) != null) {
                                                this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
                                                Log.i(TAG, "This is a certificate change. Snapshot must be updated");
                                            } else {
                                                Log.i(TAG, "This is a certificate change. Snapshot didn't exist");
                                            }
                                            if (this.mDatabase.setCounterId(userId, uid, new SecureRandom().nextLong()) < 0) {
                                                Log.e(TAG, "Failed to set the counter id in the local DB.");
                                            }
                                        } else {
                                            throw new ServiceSpecificException(22, "Failed to set the certificate serial number in the local DB.");
                                        }
                                    } catch (CertificateEncodingException e2) {
                                        e = e2;
                                        Log.e(TAG, "Failed to encode CertPath", e);
                                        throw new ServiceSpecificException(rootCertificateAlias2, e.getMessage());
                                    }
                                }
                                certXml2 = certXml;
                                certPath2 = certPath;
                                str2 = rootCertificateAlias3;
                                rootCertificateAlias3 = 25;
                                if (updatedCertPathRows < 0) {
                                    throw new ServiceSpecificException(22, "Failed to set the certificate path in the local DB.");
                                }
                                return;
                            } catch (CertificateEncodingException e3) {
                                e = e3;
                                j = newSerial;
                                certXml2 = certXml;
                                certPath2 = certPath;
                                str2 = rootCertificateAlias3;
                                rootCertificateAlias2 = 25;
                                Log.e(TAG, "Failed to encode CertPath", e);
                                throw new ServiceSpecificException(rootCertificateAlias2, e.getMessage());
                            }
                        } catch (CertValidationException e4) {
                            j = newSerial;
                            certXml2 = certXml;
                            str2 = rootCertificateAlias3;
                            Log.e(TAG, "Invalid endpoint cert", e4);
                            throw new ServiceSpecificException(28, e4.getMessage());
                        }
                    } else if (oldSerial.longValue() == newSerial) {
                        Log.i(TAG, "The cert file serial number is the same, so skip updating.");
                        return;
                    } else {
                        Log.e(TAG, "The cert file serial number is older than the one in database.");
                        throw new ServiceSpecificException(29, "The cert file serial number is older than the one in database.");
                    }
                } catch (CertParsingException e5) {
                    str2 = rootCertificateAlias3;
                    rootCertificateAlias2 = 25;
                    CertParsingException certParsingException = e5;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Failed to parse the input as a cert file: ");
                    stringBuilder3.append(HexDump.toHexString(recoveryServiceCertFile));
                    Log.d(TAG, stringBuilder3.toString());
                    throw new ServiceSpecificException(rootCertificateAlias2, e5.getMessage());
                }
            }
            throw new ServiceSpecificException(22, "Failed to set the root of trust in the local DB.");
        }
        throw new ServiceSpecificException(28, "Invalid root certificate alias");
    }

    public void initRecoveryServiceWithSigFile(String rootCertificateAlias, byte[] recoveryServiceCertFile, byte[] recoveryServiceSigFile) throws RemoteException {
        checkRecoverKeyStorePermission();
        rootCertificateAlias = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(rootCertificateAlias);
        Preconditions.checkNotNull(recoveryServiceCertFile, "recoveryServiceCertFile is null");
        Preconditions.checkNotNull(recoveryServiceSigFile, "recoveryServiceSigFile is null");
        try {
            try {
                SigXml.parse(recoveryServiceSigFile).verifyFileSignature(this.mTestCertHelper.getRootCertificate(rootCertificateAlias), recoveryServiceCertFile);
                initRecoveryService(rootCertificateAlias, recoveryServiceCertFile);
            } catch (CertValidationException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The signature over the cert file is invalid. Cert: ");
                stringBuilder.append(HexDump.toHexString(recoveryServiceCertFile));
                stringBuilder.append(" Sig: ");
                stringBuilder.append(HexDump.toHexString(recoveryServiceSigFile));
                Log.d(TAG, stringBuilder.toString());
                throw new ServiceSpecificException(28, e.getMessage());
            }
        } catch (CertParsingException e2) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to parse the sig file: ");
            stringBuilder2.append(HexDump.toHexString(recoveryServiceSigFile));
            Log.d(TAG, stringBuilder2.toString());
            throw new ServiceSpecificException(25, e2.getMessage());
        }
    }

    public KeyChainSnapshot getKeyChainSnapshot() throws RemoteException {
        checkRecoverKeyStorePermission();
        KeyChainSnapshot snapshot = this.mSnapshotStorage.get(Binder.getCallingUid());
        if (snapshot != null) {
            return snapshot;
        }
        throw new ServiceSpecificException(21);
    }

    public void setSnapshotCreatedPendingIntent(PendingIntent intent) throws RemoteException {
        checkRecoverKeyStorePermission();
        this.mListenersStorage.setSnapshotListener(Binder.getCallingUid(), intent);
    }

    public void setServerParams(byte[] serverParams) throws RemoteException {
        checkRecoverKeyStorePermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        byte[] currentServerParams = this.mDatabase.getServerParams(userId, uid);
        if (Arrays.equals(serverParams, currentServerParams)) {
            Log.v(TAG, "Not updating server params - same as old value.");
        } else if (this.mDatabase.setServerParams(userId, uid, serverParams) < 0) {
            throw new ServiceSpecificException(22, "Database failure trying to set server params.");
        } else if (currentServerParams == null) {
            Log.i(TAG, "Initialized server params.");
        } else {
            if (this.mDatabase.getSnapshotVersion(userId, uid) != null) {
                this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
                Log.i(TAG, "Updated server params. Snapshot must be updated");
            } else {
                Log.i(TAG, "Updated server params. Snapshot didn't exist");
            }
        }
    }

    public void setRecoveryStatus(String alias, int status) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(alias, "alias is null");
        if (((long) this.mDatabase.setRecoveryStatus(Binder.getCallingUid(), alias, status)) < 0) {
            throw new ServiceSpecificException(22, "Failed to set the key recovery status in the local DB.");
        }
    }

    public Map<String, Integer> getRecoveryStatus() throws RemoteException {
        checkRecoverKeyStorePermission();
        return this.mDatabase.getStatusForAllKeys(Binder.getCallingUid());
    }

    public void setRecoverySecretTypes(int[] secretTypes) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(secretTypes, "secretTypes is null");
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        int[] currentSecretTypes = this.mDatabase.getRecoverySecretTypes(userId, uid);
        if (Arrays.equals(secretTypes, currentSecretTypes)) {
            Log.v(TAG, "Not updating secret types - same as old value.");
        } else if (this.mDatabase.setRecoverySecretTypes(userId, uid, secretTypes) < 0) {
            throw new ServiceSpecificException(22, "Database error trying to set secret types.");
        } else if (currentSecretTypes.length == 0) {
            Log.i(TAG, "Initialized secret types.");
        } else {
            Log.i(TAG, "Updated secret types. Snapshot pending.");
            if (this.mDatabase.getSnapshotVersion(userId, uid) != null) {
                this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
                Log.i(TAG, "Updated secret types. Snapshot must be updated");
            } else {
                Log.i(TAG, "Updated secret types. Snapshot didn't exist");
            }
        }
    }

    public int[] getRecoverySecretTypes() throws RemoteException {
        checkRecoverKeyStorePermission();
        return this.mDatabase.getRecoverySecretTypes(UserHandle.getCallingUserId(), Binder.getCallingUid());
    }

    @VisibleForTesting
    byte[] startRecoverySession(String sessionId, byte[] verifierPublicKey, byte[] vaultParams, byte[] vaultChallenge, List<KeyChainProtectionParams> secrets) throws RemoteException {
        checkRecoverKeyStorePermission();
        int uid = Binder.getCallingUid();
        if (secrets.size() == 1) {
            try {
                PublicKey publicKey = KeySyncUtils.deserializePublicKey(verifierPublicKey);
                if (publicKeysMatch(publicKey, vaultParams)) {
                    byte[] keyClaimant = KeySyncUtils.generateKeyClaimant();
                    byte[] kfHash = ((KeyChainProtectionParams) secrets.get(0)).getSecret();
                    this.mRecoverySessionStorage.add(uid, new Entry(sessionId, kfHash, keyClaimant, vaultParams));
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Received VaultParams for recovery: ");
                    stringBuilder.append(HexDump.toHexString(vaultParams));
                    Log.i(str, stringBuilder.toString());
                    try {
                        return KeySyncUtils.encryptRecoveryClaim(publicKey, vaultParams, vaultChallenge, KeySyncUtils.calculateThmKfHash(kfHash), keyClaimant);
                    } catch (NoSuchAlgorithmException e) {
                        Log.wtf(TAG, "SecureBox algorithm missing. AOSP must support this.", e);
                        throw new ServiceSpecificException(22, e.getMessage());
                    } catch (InvalidKeyException e2) {
                        throw new ServiceSpecificException(25, e2.getMessage());
                    }
                }
                throw new ServiceSpecificException(28, "The public keys given in verifierPublicKey and vaultParams do not match.");
            } catch (InvalidKeySpecException e3) {
                throw new ServiceSpecificException(25, e3.getMessage());
            }
        }
        throw new UnsupportedOperationException("Only a single KeyChainProtectionParams is supported");
    }

    public byte[] startRecoverySessionWithCertPath(String sessionId, String rootCertificateAlias, RecoveryCertPath verifierCertPath, byte[] vaultParams, byte[] vaultChallenge, List<KeyChainProtectionParams> secrets) throws RemoteException {
        checkRecoverKeyStorePermission();
        rootCertificateAlias = this.mTestCertHelper.getDefaultCertificateAliasIfEmpty(rootCertificateAlias);
        Preconditions.checkNotNull(sessionId, "invalid session");
        Preconditions.checkNotNull(verifierCertPath, "verifierCertPath is null");
        Preconditions.checkNotNull(vaultParams, "vaultParams is null");
        Preconditions.checkNotNull(vaultChallenge, "vaultChallenge is null");
        Preconditions.checkNotNull(secrets, "secrets is null");
        try {
            CertPath certPath = verifierCertPath.getCertPath();
            try {
                CertUtils.validateCertPath(this.mTestCertHelper.getRootCertificate(rootCertificateAlias), certPath);
                byte[] verifierPublicKey = ((Certificate) certPath.getCertificates().get(0)).getPublicKey().getEncoded();
                if (verifierPublicKey != null) {
                    return startRecoverySession(sessionId, verifierPublicKey, vaultParams, vaultChallenge, secrets);
                }
                Log.e(TAG, "Failed to encode verifierPublicKey");
                throw new ServiceSpecificException(25, "Failed to encode verifierPublicKey");
            } catch (CertValidationException e) {
                Log.e(TAG, "Failed to validate the given cert path", e);
                throw new ServiceSpecificException(28, e.getMessage());
            }
        } catch (CertificateException e2) {
            throw new ServiceSpecificException(25, e2.getMessage());
        }
    }

    public Map<String, String> recoverKeyChainSnapshot(String sessionId, byte[] encryptedRecoveryKey, List<WrappedApplicationKey> applicationKeys) throws RemoteException {
        checkRecoverKeyStorePermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        Entry sessionEntry = this.mRecoverySessionStorage.get(uid, sessionId);
        if (sessionEntry != null) {
            try {
                Map<String, String> importKeyMaterials = importKeyMaterials(userId, uid, recoverApplicationKeys(decryptRecoveryKey(sessionEntry, encryptedRecoveryKey), applicationKeys));
                sessionEntry.destroy();
                this.mRecoverySessionStorage.remove(uid);
                return importKeyMaterials;
            } catch (KeyStoreException e) {
                throw new ServiceSpecificException(22, e.getMessage());
            } catch (Throwable th) {
                sessionEntry.destroy();
                this.mRecoverySessionStorage.remove(uid);
            }
        } else {
            throw new ServiceSpecificException(24, String.format(Locale.US, "Application uid=%d does not have pending session '%s'", new Object[]{Integer.valueOf(uid), sessionId}));
        }
    }

    private Map<String, String> importKeyMaterials(int userId, int uid, Map<String, byte[]> keysByAlias) throws KeyStoreException {
        ArrayMap<String, String> grantAliasesByAlias = new ArrayMap(keysByAlias.size());
        for (String alias : keysByAlias.keySet()) {
            this.mApplicationKeyStorage.setSymmetricKeyEntry(userId, uid, alias, (byte[]) keysByAlias.get(alias));
            String grantAlias = getAlias(userId, uid, alias);
            Log.i(TAG, String.format(Locale.US, "Import %s -> %s", new Object[]{alias, grantAlias}));
            grantAliasesByAlias.put(alias, grantAlias);
        }
        return grantAliasesByAlias;
    }

    private String getAlias(int userId, int uid, String alias) {
        return this.mApplicationKeyStorage.getGrantAlias(userId, uid, alias);
    }

    public void closeSession(String sessionId) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(sessionId, "invalid session");
        this.mRecoverySessionStorage.remove(Binder.getCallingUid(), sessionId);
    }

    public void removeKey(String alias) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(alias, "alias is null");
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        if (this.mDatabase.removeKey(uid, alias)) {
            this.mDatabase.setShouldCreateSnapshot(userId, uid, true);
            this.mApplicationKeyStorage.deleteEntry(userId, uid, alias);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:6:0x002a A:{Splitter: B:3:0x001a, ExcHandler: java.security.KeyStoreException (r4_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:6:0x002a A:{Splitter: B:3:0x001a, ExcHandler: java.security.KeyStoreException (r4_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0042 A:{Splitter: B:1:0x0012, ExcHandler: java.security.KeyStoreException (r3_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0042 A:{Splitter: B:1:0x0012, ExcHandler: java.security.KeyStoreException (r3_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:6:0x002a, code:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:8:0x0034, code:
            throw new android.os.ServiceSpecificException(22, r4.getMessage());
     */
    /* JADX WARNING: Missing block: B:12:0x0042, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:14:0x004c, code:
            throw new android.os.ServiceSpecificException(22, r3.getMessage());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String generateKey(String alias) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(alias, "alias is null");
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getCallingUserId();
        try {
            try {
                this.mApplicationKeyStorage.setSymmetricKeyEntry(userId, uid, alias, this.mRecoverableKeyGenerator.generateAndStoreKey(this.mPlatformKeyManager.getEncryptKey(userId), userId, uid, alias));
                return getAlias(userId, uid, alias);
            } catch (Exception e) {
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        } catch (Exception e3) {
        } catch (InsecureUserException e4) {
            throw new ServiceSpecificException(23, e4.getMessage());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x0050 A:{Splitter: B:3:0x001d, ExcHandler: java.security.KeyStoreException (r2_6 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0050 A:{Splitter: B:3:0x001d, ExcHandler: java.security.KeyStoreException (r2_6 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0038 A:{Splitter: B:5:0x0025, ExcHandler: java.security.KeyStoreException (r2_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0038 A:{Splitter: B:5:0x0025, ExcHandler: java.security.KeyStoreException (r2_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:8:0x0038, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:10:0x0042, code:
            throw new android.os.ServiceSpecificException(22, r2.getMessage());
     */
    /* JADX WARNING: Missing block: B:14:0x0050, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:16:0x005a, code:
            throw new android.os.ServiceSpecificException(22, r2.getMessage());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String importKey(String alias, byte[] keyBytes) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(alias, "alias is null");
        Preconditions.checkNotNull(keyBytes, "keyBytes is null");
        if (keyBytes.length == 32) {
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getCallingUserId();
            try {
                try {
                    this.mRecoverableKeyGenerator.importKey(this.mPlatformKeyManager.getEncryptKey(userId), userId, uid, alias, keyBytes);
                    this.mApplicationKeyStorage.setSymmetricKeyEntry(userId, uid, alias, keyBytes);
                    return getAlias(userId, uid, alias);
                } catch (Exception e) {
                }
            } catch (NoSuchAlgorithmException e2) {
                throw new RuntimeException(e2);
            } catch (Exception e3) {
            } catch (InsecureUserException e4) {
                throw new ServiceSpecificException(23, e4.getMessage());
            }
        }
        Log.e(TAG, "The given key for import doesn't have the required length 256");
        throw new ServiceSpecificException(27, "The given key does not contain 256 bits.");
    }

    public String getKey(String alias) throws RemoteException {
        checkRecoverKeyStorePermission();
        Preconditions.checkNotNull(alias, "alias is null");
        return getAlias(UserHandle.getCallingUserId(), Binder.getCallingUid(), alias);
    }

    private byte[] decryptRecoveryKey(Entry sessionEntry, byte[] encryptedClaimResponse) throws RemoteException, ServiceSpecificException {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        try {
            try {
                return KeySyncUtils.decryptRecoveryKey(sessionEntry.getLskfHash(), KeySyncUtils.decryptRecoveryClaimResponse(sessionEntry.getKeyClaimant(), sessionEntry.getVaultParams(), encryptedClaimResponse));
            } catch (InvalidKeyException e) {
                Log.e(TAG, "Got InvalidKeyException during decrypting recovery key", e);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to decrypt recovery key ");
                stringBuilder.append(e.getMessage());
                throw new ServiceSpecificException(26, stringBuilder.toString());
            } catch (AEADBadTagException e2) {
                Log.e(TAG, "Got AEADBadTagException during decrypting recovery key", e2);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to decrypt recovery key ");
                stringBuilder.append(e2.getMessage());
                throw new ServiceSpecificException(26, stringBuilder.toString());
            } catch (NoSuchAlgorithmException e3) {
                throw new ServiceSpecificException(22, e3.getMessage());
            }
        } catch (InvalidKeyException e4) {
            Log.e(TAG, "Got InvalidKeyException during decrypting recovery claim response", e4);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to decrypt recovery key ");
            stringBuilder2.append(e4.getMessage());
            throw new ServiceSpecificException(26, stringBuilder2.toString());
        } catch (AEADBadTagException e22) {
            Log.e(TAG, "Got AEADBadTagException during decrypting recovery claim response", e22);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to decrypt recovery key ");
            stringBuilder2.append(e22.getMessage());
            throw new ServiceSpecificException(26, stringBuilder2.toString());
        } catch (NoSuchAlgorithmException e32) {
            throw new ServiceSpecificException(22, e32.getMessage());
        }
    }

    private Map<String, byte[]> recoverApplicationKeys(byte[] recoveryKey, List<WrappedApplicationKey> applicationKeys) throws RemoteException {
        StringBuilder stringBuilder;
        HashMap<String, byte[]> keyMaterialByAlias = new HashMap();
        for (WrappedApplicationKey applicationKey : applicationKeys) {
            String alias = applicationKey.getAlias();
            try {
                keyMaterialByAlias.put(alias, KeySyncUtils.decryptApplicationKey(recoveryKey, applicationKey.getEncryptedKeyMaterial()));
            } catch (NoSuchAlgorithmException e) {
                Log.wtf(TAG, "Missing SecureBox algorithm. AOSP required to support this.", e);
                throw new ServiceSpecificException(22, e.getMessage());
            } catch (InvalidKeyException e2) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Got InvalidKeyException during decrypting application key with alias: ");
                stringBuilder2.append(alias);
                Log.e(TAG, stringBuilder2.toString(), e2);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to recover key with alias '");
                stringBuilder.append(alias);
                stringBuilder.append("': ");
                stringBuilder.append(e2.getMessage());
                throw new ServiceSpecificException(26, stringBuilder.toString());
            } catch (AEADBadTagException e3) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Got AEADBadTagException during decrypting application key with alias: ");
                stringBuilder.append(alias);
                Log.e(str, stringBuilder.toString(), e3);
            }
        }
        if (applicationKeys.isEmpty() || !keyMaterialByAlias.isEmpty()) {
            return keyMaterialByAlias;
        }
        Log.e(TAG, "Failed to recover any of the application keys.");
        throw new ServiceSpecificException(26, "Failed to recover any of the application keys.");
    }

    public void lockScreenSecretAvailable(int storedHashType, String credential, int userId) {
        try {
            this.mExecutorService.execute(KeySyncTask.newInstance(this.mContext, this.mDatabase, this.mSnapshotStorage, this.mListenersStorage, userId, storedHashType, credential, false));
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(TAG, "Should never happen - algorithm unavailable for KeySync", e);
        } catch (KeyStoreException e2) {
            Log.e(TAG, "Key store error encountered during recoverable key sync", e2);
        } catch (InsecureUserException e3) {
            Log.wtf(TAG, "Impossible - insecure user, but user just entered lock screen", e3);
        }
    }

    public void lockScreenSecretChanged(int storedHashType, String credential, int userId) {
        try {
            this.mExecutorService.execute(KeySyncTask.newInstance(this.mContext, this.mDatabase, this.mSnapshotStorage, this.mListenersStorage, userId, storedHashType, credential, true));
        } catch (NoSuchAlgorithmException e) {
            Log.wtf(TAG, "Should never happen - algorithm unavailable for KeySync", e);
        } catch (KeyStoreException e2) {
            Log.e(TAG, "Key store error encountered during recoverable key sync", e2);
        } catch (InsecureUserException e3) {
            Log.e(TAG, "InsecureUserException during lock screen secret update", e3);
        }
    }

    private void checkRecoverKeyStorePermission() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Caller ");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(" doesn't have RecoverKeyStore permission.");
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVER_KEYSTORE", stringBuilder.toString());
    }

    private boolean publicKeysMatch(PublicKey publicKey, byte[] vaultParams) {
        byte[] encodedPublicKey = SecureBox.encodePublicKey(publicKey);
        return Arrays.equals(encodedPublicKey, Arrays.copyOf(vaultParams, encodedPublicKey.length));
    }
}
