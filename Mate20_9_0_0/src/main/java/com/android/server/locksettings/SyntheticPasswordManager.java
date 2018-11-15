package com.android.server.locksettings;

import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.weaver.V1_0.IWeaver;
import android.hardware.weaver.V1_0.WeaverConfig;
import android.hardware.weaver.V1_0.WeaverReadResponse;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.gatekeeper.GateKeeperResponse;
import android.service.gatekeeper.IGateKeeperService;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.locksettings.LockSettingsStorage.PersistentData;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import libcore.util.HexEncoding;

public class SyntheticPasswordManager {
    public static final long DEFAULT_HANDLE = 0;
    private static final String DEFAULT_PASSWORD = "default-password";
    private static final int INVALID_WEAVER_SLOT = -1;
    private static final String PASSWORD_DATA_NAME = "pwd";
    private static final int PASSWORD_SALT_LENGTH = 16;
    private static final int PASSWORD_SCRYPT_N = 11;
    private static final int PASSWORD_SCRYPT_P = 1;
    private static final int PASSWORD_SCRYPT_R = 3;
    private static final int PASSWORD_TOKEN_LENGTH = 32;
    private static final byte[] PERSONALISATION_SECDISCARDABLE = "secdiscardable-transform".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_KEY = "weaver-key".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_PASSWORD = "weaver-pwd".getBytes();
    private static final byte[] PERSONALISATION_WEAVER_TOKEN = "weaver-token".getBytes();
    private static final byte[] PERSONALIZATION_AUTHSECRET_KEY = "authsecret-hal".getBytes();
    private static final byte[] PERSONALIZATION_E0 = "e0-encryption".getBytes();
    private static final byte[] PERSONALIZATION_FBE_KEY = "fbe-key".getBytes();
    private static final byte[] PERSONALIZATION_KEY_STORE_PASSWORD = "keystore-password".getBytes();
    private static final byte[] PERSONALIZATION_PASSWORD_HASH = "pw-hash".getBytes();
    private static final byte[] PERSONALIZATION_SP_GK_AUTH = "sp-gk-authentication".getBytes();
    private static final byte[] PERSONALIZATION_SP_SPLIT = "sp-split".getBytes();
    private static final byte[] PERSONALIZATION_USER_GK_AUTH = "user-gk-authentication".getBytes();
    private static final int SECDISCARDABLE_LENGTH = 16384;
    private static final String SECDISCARDABLE_NAME = "secdis";
    private static final String SP_BLOB_NAME = "spblob";
    private static final String SP_E0_NAME = "e0";
    private static final String SP_HANDLE_NAME = "handle";
    private static final String SP_P1_NAME = "p1";
    private static final byte SYNTHETIC_PASSWORD_LENGTH = (byte) 32;
    private static final byte SYNTHETIC_PASSWORD_PASSWORD_BASED = (byte) 0;
    private static final byte SYNTHETIC_PASSWORD_TOKEN_BASED = (byte) 1;
    private static final byte SYNTHETIC_PASSWORD_VERSION = (byte) 2;
    private static final byte SYNTHETIC_PASSWORD_VERSION_V1 = (byte) 1;
    private static final String TAG = "SyntheticPasswordManager";
    private static final String WEAVER_SLOT_NAME = "weaver";
    private static final byte WEAVER_VERSION = (byte) 1;
    protected static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final Context mContext;
    private LockSettingsStorage mStorage;
    private final UserManager mUserManager;
    private IWeaver mWeaver;
    private WeaverConfig mWeaverConfig;
    private ArrayMap<Integer, ArrayMap<Long, TokenData>> tokenMap = new ArrayMap();

    static class AuthenticationResult {
        public AuthenticationToken authToken;
        public int credentialType;
        public VerifyCredentialResponse gkResponse;

        AuthenticationResult() {
        }
    }

    static class AuthenticationToken {
        private byte[] E0;
        private byte[] P1;
        private String syntheticPassword;

        AuthenticationToken() {
        }

        public String deriveKeyStorePassword() {
            return SyntheticPasswordManager.bytesToHex(SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_KEY_STORE_PASSWORD, this.syntheticPassword.getBytes()));
        }

        public byte[] deriveGkPassword() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_SP_GK_AUTH, this.syntheticPassword.getBytes());
        }

        public byte[] deriveDiskEncryptionKey() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_FBE_KEY, this.syntheticPassword.getBytes());
        }

        public byte[] deriveVendorAuthSecret() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_AUTHSECRET_KEY, this.syntheticPassword.getBytes());
        }

        public byte[] derivePasswordHashFactor() {
            return SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_PASSWORD_HASH, this.syntheticPassword.getBytes());
        }

        private void initialize(byte[] P0, byte[] P1) {
            this.P1 = P1;
            this.syntheticPassword = String.valueOf(HexEncoding.encode(SyntheticPasswordCrypto.personalisedHash(SyntheticPasswordManager.PERSONALIZATION_SP_SPLIT, P0, P1)));
            this.E0 = SyntheticPasswordCrypto.encrypt(this.syntheticPassword.getBytes(), SyntheticPasswordManager.PERSONALIZATION_E0, P0);
        }

        public void recreate(byte[] secret) {
            initialize(secret, this.P1);
        }

        protected static AuthenticationToken create() {
            AuthenticationToken result = new AuthenticationToken();
            result.initialize(SyntheticPasswordManager.secureRandom(32), SyntheticPasswordManager.secureRandom(32));
            return result;
        }

        public byte[] computeP0() {
            if (this.E0 == null) {
                return null;
            }
            return SyntheticPasswordCrypto.decrypt(this.syntheticPassword.getBytes(), SyntheticPasswordManager.PERSONALIZATION_E0, this.E0);
        }
    }

    static class PasswordData {
        public byte[] passwordHandle;
        public int passwordType;
        byte[] salt;
        byte scryptN;
        byte scryptP;
        byte scryptR;

        PasswordData() {
        }

        public static PasswordData create(int passwordType) {
            PasswordData result = new PasswordData();
            result.scryptN = (byte) 11;
            result.scryptR = (byte) 3;
            result.scryptP = (byte) 1;
            result.passwordType = passwordType;
            result.salt = SyntheticPasswordManager.secureRandom(16);
            return result;
        }

        public static PasswordData fromBytes(byte[] data) {
            PasswordData result = new PasswordData();
            ByteBuffer buffer = ByteBuffer.allocate(data.length);
            buffer.put(data, 0, data.length);
            buffer.flip();
            result.passwordType = buffer.getInt();
            result.scryptN = buffer.get();
            result.scryptR = buffer.get();
            result.scryptP = buffer.get();
            result.salt = new byte[buffer.getInt()];
            buffer.get(result.salt);
            int handleLen = buffer.getInt();
            if (handleLen > 0) {
                result.passwordHandle = new byte[handleLen];
                buffer.get(result.passwordHandle);
            } else {
                result.passwordHandle = null;
            }
            return result;
        }

        public byte[] toBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(((11 + this.salt.length) + 4) + (this.passwordHandle != null ? this.passwordHandle.length : 0));
            buffer.putInt(this.passwordType);
            buffer.put(this.scryptN);
            buffer.put(this.scryptR);
            buffer.put(this.scryptP);
            buffer.putInt(this.salt.length);
            buffer.put(this.salt);
            if (this.passwordHandle == null || this.passwordHandle.length <= 0) {
                buffer.putInt(0);
            } else {
                buffer.putInt(this.passwordHandle.length);
                buffer.put(this.passwordHandle);
            }
            return buffer.array();
        }
    }

    static class TokenData {
        byte[] aggregatedSecret;
        byte[] secdiscardableOnDisk;
        byte[] weaverSecret;

        TokenData() {
        }
    }

    native byte[] nativeScrypt(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4);

    native long nativeSidFromPasswordHandle(byte[] bArr);

    public SyntheticPasswordManager(Context context, LockSettingsStorage storage, UserManager userManager) {
        this.mContext = context;
        this.mStorage = storage;
        this.mUserManager = userManager;
    }

    @VisibleForTesting
    protected IWeaver getWeaverService() throws RemoteException {
        try {
            return IWeaver.getService();
        } catch (NoSuchElementException e) {
            Slog.i(TAG, "Device does not support weaver");
            return null;
        }
    }

    public synchronized void initWeaverService() {
        if (this.mWeaver == null) {
            try {
                this.mWeaverConfig = null;
                this.mWeaver = getWeaverService();
                if (this.mWeaver != null) {
                    this.mWeaver.getConfig(new -$$Lambda$SyntheticPasswordManager$WjMV-qfQ1YUbeAiLzyAhyepqPFI(this));
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to get weaver service", e);
            }
        } else {
            return;
        }
        return;
    }

    public static /* synthetic */ void lambda$initWeaverService$0(SyntheticPasswordManager syntheticPasswordManager, int status, WeaverConfig config) {
        if (status != 0 || config.slots <= 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get weaver config, status ");
            stringBuilder.append(status);
            stringBuilder.append(" slots: ");
            stringBuilder.append(config.slots);
            Slog.e(str, stringBuilder.toString());
            syntheticPasswordManager.mWeaver = null;
            return;
        }
        syntheticPasswordManager.mWeaverConfig = config;
    }

    private synchronized boolean isWeaverAvailable() {
        boolean z;
        if (this.mWeaver == null) {
            initWeaverService();
        }
        z = this.mWeaver != null && this.mWeaverConfig.slots > 0;
        return z;
    }

    private byte[] weaverEnroll(int slot, byte[] key, byte[] value) throws RemoteException {
        if (slot == -1 || slot >= this.mWeaverConfig.slots) {
            throw new RuntimeException("Invalid slot for weaver");
        }
        if (key == null) {
            key = new byte[this.mWeaverConfig.keySize];
        } else if (key.length != this.mWeaverConfig.keySize) {
            throw new RuntimeException("Invalid key size for weaver");
        }
        if (value == null) {
            value = secureRandom(this.mWeaverConfig.valueSize);
        }
        int writeStatus = this.mWeaver.write(slot, toByteArrayList(key), toByteArrayList(value));
        if (writeStatus == 0) {
            return value;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("weaver write failed, slot: ");
        stringBuilder.append(slot);
        stringBuilder.append(" status: ");
        stringBuilder.append(writeStatus);
        Log.e(str, stringBuilder.toString());
        return null;
    }

    private VerifyCredentialResponse weaverVerify(int slot, byte[] key) throws RemoteException {
        if (slot == -1 || slot >= this.mWeaverConfig.slots) {
            throw new RuntimeException("Invalid slot for weaver");
        }
        if (key == null) {
            key = new byte[this.mWeaverConfig.keySize];
        } else if (key.length != this.mWeaverConfig.keySize) {
            throw new RuntimeException("Invalid key size for weaver");
        }
        VerifyCredentialResponse[] response = new VerifyCredentialResponse[1];
        this.mWeaver.read(slot, toByteArrayList(key), new -$$Lambda$SyntheticPasswordManager$aWnbfYziDTrRrLqWFePMTj6-dy0(response, slot));
        return response[0];
    }

    static /* synthetic */ void lambda$weaverVerify$1(VerifyCredentialResponse[] response, int slot, int status, WeaverReadResponse readResponse) {
        String str;
        StringBuilder stringBuilder;
        switch (status) {
            case 0:
                response[0] = new VerifyCredentialResponse(fromByteArrayList(readResponse.value));
                return;
            case 1:
                response[0] = VerifyCredentialResponse.ERROR;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("weaver read failed (FAILED), slot: ");
                stringBuilder.append(slot);
                Log.e(str, stringBuilder.toString());
                return;
            case 2:
                if (readResponse.timeout == 0) {
                    response[0] = VerifyCredentialResponse.ERROR;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("weaver read failed (INCORRECT_KEY), slot: ");
                    stringBuilder.append(slot);
                    Log.e(str, stringBuilder.toString());
                    return;
                }
                response[0] = new VerifyCredentialResponse(readResponse.timeout);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("weaver read failed (INCORRECT_KEY/THROTTLE), slot: ");
                stringBuilder.append(slot);
                Log.e(str, stringBuilder.toString());
                return;
            case 3:
                response[0] = new VerifyCredentialResponse(readResponse.timeout);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("weaver read failed (THROTTLE), slot: ");
                stringBuilder.append(slot);
                Log.e(str, stringBuilder.toString());
                return;
            default:
                response[0] = VerifyCredentialResponse.ERROR;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("weaver read unknown status ");
                stringBuilder.append(status);
                stringBuilder.append(", slot: ");
                stringBuilder.append(slot);
                Log.e(str, stringBuilder.toString());
                return;
        }
    }

    public void removeUser(int userId) {
        for (Long handle : this.mStorage.listSyntheticPasswordHandlesForUser(SP_BLOB_NAME, userId)) {
            long handle2 = handle.longValue();
            destroyWeaverSlot(handle2, userId);
            destroySPBlobKey(getHandleName(handle2));
        }
    }

    public int getCredentialType(long handle, int userId) {
        byte[] passwordData = loadState(PASSWORD_DATA_NAME, handle, userId);
        if (passwordData != null) {
            return PasswordData.fromBytes(passwordData).passwordType;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCredentialType: encountered empty password data for user ");
        stringBuilder.append(userId);
        Log.w(str, stringBuilder.toString());
        return -1;
    }

    public AuthenticationToken newSyntheticPasswordAndSid(IGateKeeperService gatekeeper, byte[] hash, String credential, int userId) throws RemoteException {
        AuthenticationToken result = AuthenticationToken.create();
        if (hash != null) {
            GateKeeperResponse response = gatekeeper.enroll(userId, hash, credential.getBytes(), result.deriveGkPassword());
            if (response.getResponseCode() != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Fail to migrate SID, assuming no SID, user ");
                stringBuilder.append(userId);
                Log.w(str, stringBuilder.toString());
                clearSidForUser(userId);
            } else {
                saveSyntheticPasswordHandle(response.getPayload(), userId);
            }
        } else {
            clearSidForUser(userId);
        }
        saveEscrowData(result, userId);
        return result;
    }

    public void newSidForUser(IGateKeeperService gatekeeper, AuthenticationToken authToken, int userId) throws RemoteException {
        GateKeeperResponse response = gatekeeper.enroll(userId, null, null, authToken.deriveGkPassword());
        if (response.getResponseCode() != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Fail to create new SID for user ");
            stringBuilder.append(userId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        saveSyntheticPasswordHandle(response.getPayload(), userId);
    }

    public void clearSidForUser(int userId) {
        destroyState(SP_HANDLE_NAME, 0, userId);
    }

    public boolean hasSidForUser(int userId) {
        return hasState(SP_HANDLE_NAME, 0, userId);
    }

    private byte[] loadSyntheticPasswordHandle(int userId) {
        return loadState(SP_HANDLE_NAME, 0, userId);
    }

    private void saveSyntheticPasswordHandle(byte[] spHandle, int userId) {
        saveState(SP_HANDLE_NAME, spHandle, 0, userId);
    }

    private boolean loadEscrowData(AuthenticationToken authToken, int userId) {
        authToken.E0 = loadState(SP_E0_NAME, 0, userId);
        authToken.P1 = loadState(SP_P1_NAME, 0, userId);
        return (authToken.E0 == null || authToken.P1 == null) ? false : true;
    }

    private void saveEscrowData(AuthenticationToken authToken, int userId) {
        saveState(SP_E0_NAME, authToken.E0, 0, userId);
        saveState(SP_P1_NAME, authToken.P1, 0, userId);
    }

    public boolean hasEscrowData(int userId) {
        return hasState(SP_E0_NAME, 0, userId) && hasState(SP_P1_NAME, 0, userId);
    }

    public void destroyEscrowData(int userId) {
        destroyState(SP_E0_NAME, 0, userId);
        destroyState(SP_P1_NAME, 0, userId);
    }

    private int loadWeaverSlot(long handle, int userId) {
        byte[] data = loadState(WEAVER_SLOT_NAME, handle, userId);
        if (data == null || data.length != 5) {
            return -1;
        }
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put(data, 0, data.length);
        buffer.flip();
        if (buffer.get() == (byte) 1) {
            return buffer.getInt();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid weaver slot version of handle ");
        stringBuilder.append(handle);
        Log.e(str, stringBuilder.toString());
        return -1;
    }

    private void saveWeaverSlot(int slot, long handle, int userId) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte) 1);
        buffer.putInt(slot);
        saveState(WEAVER_SLOT_NAME, buffer.array(), handle, userId);
    }

    private void destroyWeaverSlot(long handle, int userId) {
        int slot = loadWeaverSlot(handle, userId);
        destroyState(WEAVER_SLOT_NAME, handle, userId);
        if (slot == -1) {
            return;
        }
        String str;
        StringBuilder stringBuilder;
        if (getUsedWeaverSlots().contains(Integer.valueOf(slot))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Skip destroying reused weaver slot ");
            stringBuilder.append(slot);
            stringBuilder.append(" for user ");
            stringBuilder.append(userId);
            Log.w(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Destroy weaver slot ");
        stringBuilder.append(slot);
        stringBuilder.append(" for user ");
        stringBuilder.append(userId);
        Log.i(str, stringBuilder.toString());
        try {
            weaverEnroll(slot, null, null);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to destroy slot", e);
        }
    }

    private Set<Integer> getUsedWeaverSlots() {
        Map<Integer, List<Long>> slotHandles = this.mStorage.listSyntheticPasswordHandlesForAllUsers(WEAVER_SLOT_NAME);
        HashSet<Integer> slots = new HashSet();
        for (Entry<Integer, List<Long>> entry : slotHandles.entrySet()) {
            for (Long handle : (List) entry.getValue()) {
                slots.add(Integer.valueOf(loadWeaverSlot(handle.longValue(), ((Integer) entry.getKey()).intValue())));
            }
        }
        return slots;
    }

    private int getNextAvailableWeaverSlot() {
        Set<Integer> usedSlots = getUsedWeaverSlots();
        for (int i = 0; i < this.mWeaverConfig.slots; i++) {
            if (!usedSlots.contains(Integer.valueOf(i))) {
                return i;
            }
        }
        throw new RuntimeException("Run out of weaver slots.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x0087  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x002e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long createPasswordBasedSyntheticPassword(IGateKeeperService gatekeeper, String credential, int credentialType, AuthenticationToken authToken, int requestedQuality, int userId) throws RemoteException {
        String credential2;
        int credentialType2;
        long handle;
        PasswordData pwd;
        byte[] pwdToken;
        long sid;
        byte[] applicationId;
        long handle2;
        IGateKeeperService iGateKeeperService = gatekeeper;
        int i = requestedQuality;
        int i2 = userId;
        int i3;
        if (credential != null) {
            i3 = credentialType;
            if (i3 != -1) {
                credential2 = credential;
                credentialType2 = i3;
                handle = generateHandle();
                pwd = PasswordData.create(credentialType2);
                pwdToken = computePasswordToken(credential2, pwd);
                if (isWeaverAvailable()) {
                    iGateKeeperService.clearSecureUserId(fakeUid(i2));
                    GateKeeperResponse response = iGateKeeperService.enroll(fakeUid(i2), null, null, passwordTokenToGkInput(pwdToken));
                    if (response.getResponseCode() != 0) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Fail to enroll user password when creating SP for user ");
                        stringBuilder.append(i2);
                        Log.e(str, stringBuilder.toString());
                        return 0;
                    }
                    pwd.passwordHandle = response.getPayload();
                    long sid2 = sidFromPasswordHandle(pwd.passwordHandle);
                    byte[] applicationId2 = transformUnderSecdiscardable(pwdToken, createSecdiscardable(handle, i2));
                    synchronizeFrpPassword(pwd, i, i2);
                    sid = sid2;
                    applicationId = applicationId2;
                } else {
                    int weaverSlot = getNextAvailableWeaverSlot();
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Weaver enroll password to slot ");
                    stringBuilder2.append(weaverSlot);
                    stringBuilder2.append(" for user ");
                    stringBuilder2.append(i2);
                    Log.i(str2, stringBuilder2.toString());
                    byte[] weaverSecret = weaverEnroll(weaverSlot, passwordTokenToWeaverKey(pwdToken), null);
                    if (weaverSecret == null) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Fail to enroll user password under weaver ");
                        stringBuilder3.append(i2);
                        Log.e(str3, stringBuilder3.toString());
                        return 0;
                    }
                    saveWeaverSlot(weaverSlot, handle, i2);
                    synchronizeWeaverFrpPassword(pwd, i, i2, weaverSlot);
                    pwd.passwordHandle = null;
                    applicationId = transformUnderWeaverSecret(pwdToken, weaverSecret);
                    sid = 0;
                }
                saveState(PASSWORD_DATA_NAME, pwd.toBytes(), handle, i2);
                handle2 = handle;
                createSyntheticPasswordBlob(handle, (byte) 0, authToken, applicationId, sid, i2);
                return handle2;
            }
        }
        i3 = credentialType;
        credential2 = DEFAULT_PASSWORD;
        credentialType2 = -1;
        handle = generateHandle();
        pwd = PasswordData.create(credentialType2);
        pwdToken = computePasswordToken(credential2, pwd);
        if (isWeaverAvailable()) {
        }
        saveState(PASSWORD_DATA_NAME, pwd.toBytes(), handle, i2);
        handle2 = handle;
        createSyntheticPasswordBlob(handle, (byte) 0, authToken, applicationId, sid, i2);
        return handle2;
    }

    public VerifyCredentialResponse verifyFrpCredential(IGateKeeperService gatekeeper, String userCredential, int credentialType, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        PersistentData persistentData = this.mStorage.readPersistentDataBlock();
        if (persistentData.type == 1) {
            PasswordData pwd = PasswordData.fromBytes(persistentData.payload);
            return VerifyCredentialResponse.fromGateKeeperResponse(gatekeeper.verifyChallenge(fakeUid(persistentData.userId), 0, pwd.passwordHandle, passwordTokenToGkInput(computePasswordToken(userCredential, pwd))));
        } else if (persistentData.type == 2) {
            return weaverVerify(persistentData.userId, passwordTokenToWeaverKey(computePasswordToken(userCredential, PasswordData.fromBytes(persistentData.payload)))).stripPayload();
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("persistentData.type must be TYPE_SP or TYPE_SP_WEAVER, but is ");
            stringBuilder.append(persistentData.type);
            Log.e(str, stringBuilder.toString());
            return VerifyCredentialResponse.ERROR;
        }
    }

    public void migrateFrpPasswordLocked(long handle, UserInfo userInfo, int requestedQuality) {
        if (this.mStorage.getPersistentDataBlock() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, userInfo)) {
            PasswordData pwd = PasswordData.fromBytes(loadState(PASSWORD_DATA_NAME, handle, userInfo.id));
            if (pwd.passwordType != -1) {
                int weaverSlot = loadWeaverSlot(handle, userInfo.id);
                if (weaverSlot != -1) {
                    synchronizeWeaverFrpPassword(pwd, requestedQuality, userInfo.id, weaverSlot);
                } else {
                    synchronizeFrpPassword(pwd, requestedQuality, userInfo.id);
                }
            }
        }
    }

    private void synchronizeFrpPassword(PasswordData pwd, int requestedQuality, int userId) {
        if (this.mStorage.getPersistentDataBlock() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, this.mUserManager.getUserInfo(userId))) {
            if (pwd.passwordType != -1) {
                this.mStorage.writePersistentDataBlock(1, userId, requestedQuality, pwd.toBytes());
            } else {
                this.mStorage.writePersistentDataBlock(0, userId, 0, null);
            }
        }
    }

    private void synchronizeWeaverFrpPassword(PasswordData pwd, int requestedQuality, int userId, int weaverSlot) {
        if (this.mStorage.getPersistentDataBlock() != null && LockPatternUtils.userOwnsFrpCredential(this.mContext, this.mUserManager.getUserInfo(userId))) {
            if (pwd.passwordType != -1) {
                this.mStorage.writePersistentDataBlock(2, weaverSlot, requestedQuality, pwd.toBytes());
            } else {
                this.mStorage.writePersistentDataBlock(0, 0, 0, null);
            }
        }
    }

    public long createTokenBasedSyntheticPassword(byte[] token, int userId) {
        long handle = generateHandle();
        if (!this.tokenMap.containsKey(Integer.valueOf(userId))) {
            this.tokenMap.put(Integer.valueOf(userId), new ArrayMap());
        }
        TokenData tokenData = new TokenData();
        byte[] secdiscardable = secureRandom(16384);
        if (isWeaverAvailable()) {
            tokenData.weaverSecret = secureRandom(this.mWeaverConfig.valueSize);
            tokenData.secdiscardableOnDisk = SyntheticPasswordCrypto.encrypt(tokenData.weaverSecret, PERSONALISATION_WEAVER_TOKEN, secdiscardable);
        } else {
            tokenData.secdiscardableOnDisk = secdiscardable;
            tokenData.weaverSecret = null;
        }
        tokenData.aggregatedSecret = transformUnderSecdiscardable(token, secdiscardable);
        ((ArrayMap) this.tokenMap.get(Integer.valueOf(userId))).put(Long.valueOf(handle), tokenData);
        return handle;
    }

    public Set<Long> getPendingTokensForUser(int userId) {
        if (this.tokenMap.containsKey(Integer.valueOf(userId))) {
            return ((ArrayMap) this.tokenMap.get(Integer.valueOf(userId))).keySet();
        }
        return Collections.emptySet();
    }

    public boolean removePendingToken(long handle, int userId) {
        boolean z = false;
        if (!this.tokenMap.containsKey(Integer.valueOf(userId))) {
            return false;
        }
        if (((ArrayMap) this.tokenMap.get(Integer.valueOf(userId))).remove(Long.valueOf(handle)) != null) {
            z = true;
        }
        return z;
    }

    public boolean activateTokenBasedSyntheticPassword(long handle, AuthenticationToken authToken, int userId) {
        if (!this.tokenMap.containsKey(Integer.valueOf(userId))) {
            return false;
        }
        TokenData tokenData = (TokenData) ((ArrayMap) this.tokenMap.get(Integer.valueOf(userId))).get(Long.valueOf(handle));
        if (tokenData == null) {
            return false;
        }
        if (loadEscrowData(authToken, userId)) {
            if (isWeaverAvailable()) {
                int slot = getNextAvailableWeaverSlot();
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Weaver enroll token to slot ");
                    stringBuilder.append(slot);
                    stringBuilder.append(" for user ");
                    stringBuilder.append(userId);
                    Log.i(str, stringBuilder.toString());
                    weaverEnroll(slot, null, tokenData.weaverSecret);
                    saveWeaverSlot(slot, handle, userId);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to enroll weaver secret when activating token", e);
                    return false;
                }
            }
            saveSecdiscardable(handle, tokenData.secdiscardableOnDisk, userId);
            createSyntheticPasswordBlob(handle, (byte) 1, authToken, tokenData.aggregatedSecret, 0, userId);
            ((ArrayMap) this.tokenMap.get(Integer.valueOf(userId))).remove(Long.valueOf(handle));
            return true;
        }
        Log.w(TAG, "User is not escrowable");
        return false;
    }

    private void createSyntheticPasswordBlob(long handle, byte type, AuthenticationToken authToken, byte[] applicationId, long sid, int userId) {
        byte[] computeP0;
        boolean z;
        byte b = type;
        int i = userId;
        if (b == (byte) 1) {
            computeP0 = authToken.computeP0();
        } else {
            computeP0 = authToken.syntheticPassword.getBytes();
        }
        byte[] secret = computeP0;
        UserInfo userInfo = this.mUserManager.getUserInfo(i);
        if (userInfo == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("userId ");
            stringBuilder.append(i);
            stringBuilder.append(" do not have userInfo, we dont think it's a ManagedProfile");
            Log.e(str, stringBuilder.toString());
        }
        String handleName = getHandleName(handle);
        if (userInfo == null) {
            z = false;
        } else {
            z = userInfo.isManagedProfile();
        }
        byte[] content = createSPBlob(handleName, secret, applicationId, sid, z);
        byte[] blob = new byte[((content.length + 1) + 1)];
        blob[0] = (byte) 2;
        blob[1] = b;
        System.arraycopy(content, 0, blob, 2, content.length);
        saveState(SP_BLOB_NAME, blob, handle, i);
    }

    public AuthenticationResult unwrapPasswordBasedSyntheticPassword(IGateKeeperService gatekeeper, long handle, String credential, int userId, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        String credential2;
        int responseCode;
        long sid;
        byte[] applicationId;
        AuthenticationResult result;
        long j = handle;
        int i = userId;
        if (credential == null) {
            credential2 = DEFAULT_PASSWORD;
        } else {
            credential2 = credential;
        }
        AuthenticationResult result2 = new AuthenticationResult();
        PasswordData pwd = PasswordData.fromBytes(loadState(PASSWORD_DATA_NAME, j, i));
        result2.credentialType = pwd.passwordType;
        byte[] pwdToken = computePasswordToken(credential2, pwd);
        int weaverSlot = loadWeaverSlot(j, i);
        int weaverSlot2;
        if (weaverSlot == -1) {
            byte[] gkPwdToken = passwordTokenToGkInput(pwdToken);
            GateKeeperResponse response = gatekeeper.verifyChallenge(fakeUid(i), 0, pwd.passwordHandle, gkPwdToken);
            responseCode = response.getResponseCode();
            GateKeeperResponse gateKeeperResponse;
            byte[] bArr;
            if (responseCode == 0) {
                result2.gkResponse = VerifyCredentialResponse.OK;
                if (response.getShouldReEnroll()) {
                    GateKeeperResponse reenrollResponse = gatekeeper.enroll(fakeUid(i), pwd.passwordHandle, gkPwdToken, gkPwdToken);
                    if (reenrollResponse.getResponseCode() == 0) {
                        int i2;
                        pwd.passwordHandle = reenrollResponse.getPayload();
                        weaverSlot2 = weaverSlot;
                        gkPwdToken = responseCode;
                        weaverSlot = 1;
                        saveState(PASSWORD_DATA_NAME, pwd.toBytes(), j, i);
                        if (pwd.passwordType == weaverSlot) {
                            i2 = 65536;
                        } else {
                            i2 = 327680;
                        }
                        synchronizeFrpPassword(pwd, i2, i);
                    } else {
                        gateKeeperResponse = response;
                        bArr = gkPwdToken;
                        weaverSlot2 = weaverSlot;
                        gkPwdToken = responseCode;
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Fail to re-enroll user password for user ");
                        stringBuilder.append(i);
                        Log.w(str, stringBuilder.toString());
                    }
                } else {
                    bArr = gkPwdToken;
                    weaverSlot2 = weaverSlot;
                }
                sid = sidFromPasswordHandle(pwd.passwordHandle);
                applicationId = transformUnderSecdiscardable(pwdToken, loadSecdiscardable(j, i));
            } else {
                gateKeeperResponse = response;
                bArr = gkPwdToken;
                int i3 = weaverSlot;
                PasswordData passwordData = pwd;
                result = result2;
                int responseCode2 = responseCode;
                if (responseCode2 == 1) {
                    result.gkResponse = new VerifyCredentialResponse(gateKeeperResponse.getTimeout());
                    return result;
                }
                result.gkResponse = VerifyCredentialResponse.ERROR;
                return result;
            }
        } else if (isWeaverAvailable()) {
            result2.gkResponse = weaverVerify(weaverSlot, passwordTokenToWeaverKey(pwdToken));
            if (result2.gkResponse.getResponseCode() != 0) {
                return result2;
            }
            sid = 0;
            applicationId = transformUnderWeaverSecret(pwdToken, result2.gkResponse.getPayload());
            weaverSlot2 = weaverSlot;
        } else {
            Log.e(TAG, "No weaver service to unwrap password based SP");
            result2.gkResponse = VerifyCredentialResponse.ERROR;
            return result2;
        }
        if (progressCallback != null) {
            progressCallback.onCredentialVerified();
        }
        result2.authToken = unwrapSyntheticPasswordBlob(j, (byte) 0, applicationId, sid, i);
        responseCode = pwdToken;
        result = result2;
        result.gkResponse = verifyChallenge(gatekeeper, result2.authToken, 0, i);
        return result;
    }

    public AuthenticationResult unwrapTokenBasedSyntheticPassword(IGateKeeperService gatekeeper, long handle, byte[] token, int userId) throws RemoteException {
        long j = handle;
        int i = userId;
        AuthenticationResult result = new AuthenticationResult();
        byte[] secdiscardable = loadSecdiscardable(j, i);
        int slotId = loadWeaverSlot(j, i);
        if (slotId != -1) {
            if (isWeaverAvailable()) {
                VerifyCredentialResponse response = weaverVerify(slotId, null);
                if (response.getResponseCode() != 0 || response.getPayload() == null) {
                    Log.e(TAG, "Failed to retrieve weaver secret when unwrapping token");
                    result.gkResponse = VerifyCredentialResponse.ERROR;
                    return result;
                }
                secdiscardable = SyntheticPasswordCrypto.decrypt(response.getPayload(), PERSONALISATION_WEAVER_TOKEN, secdiscardable);
            } else {
                Log.e(TAG, "No weaver service to unwrap token based SP");
                result.gkResponse = VerifyCredentialResponse.ERROR;
                return result;
            }
        }
        result.authToken = unwrapSyntheticPasswordBlob(j, (byte) 1, transformUnderSecdiscardable(token, secdiscardable), 0, i);
        if (result.authToken != null) {
            result.gkResponse = verifyChallenge(gatekeeper, result.authToken, 0, i);
            if (result.gkResponse == null) {
                result.gkResponse = VerifyCredentialResponse.OK;
            }
        } else {
            result.gkResponse = VerifyCredentialResponse.ERROR;
        }
        return result;
    }

    private AuthenticationToken unwrapSyntheticPasswordBlob(long handle, byte type, byte[] applicationId, long sid, int userId) {
        byte b = type;
        byte[] bArr = applicationId;
        byte b2 = userId;
        long j = handle;
        byte[] blob = loadState(SP_BLOB_NAME, j, b2);
        if (blob == null) {
            return null;
        }
        byte version = blob[0];
        if (version != (byte) 2 && version != (byte) 1) {
            throw new RuntimeException("Unknown blob version");
        } else if (blob[1] == b) {
            byte[] secret;
            if (version == (byte) 1) {
                secret = SyntheticPasswordCrypto.decryptBlobV1(getHandleName(handle), Arrays.copyOfRange(blob, 2, blob.length), bArr);
            } else {
                secret = decryptSPBlob(getHandleName(handle), Arrays.copyOfRange(blob, 2, blob.length), bArr);
            }
            byte[] secret2 = secret;
            String str;
            StringBuilder stringBuilder;
            if (secret2 == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Fail to decrypt SP for user ");
                stringBuilder.append(b2);
                Log.e(str, stringBuilder.toString());
                return null;
            }
            AuthenticationToken result;
            AuthenticationToken result2 = new AuthenticationToken();
            if (b != (byte) 1) {
                result2.syntheticPassword = new String(secret2);
            } else if (loadEscrowData(result2, b2)) {
                result2.recreate(secret2);
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("User is not escrowable: ");
                stringBuilder.append(b2);
                Log.e(str, stringBuilder.toString());
                return null;
            }
            if (version == (byte) 1) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Upgrade v1 SP blob for user ");
                stringBuilder2.append(b2);
                stringBuilder2.append(", type = ");
                stringBuilder2.append(b);
                Log.i(str2, stringBuilder2.toString());
                result = result2;
                createSyntheticPasswordBlob(j, b, result2, bArr, sid, b2);
            } else {
                result = result2;
                byte b3 = version;
            }
            return result;
        } else {
            throw new RuntimeException("Invalid blob type");
        }
    }

    public VerifyCredentialResponse verifyChallenge(IGateKeeperService gatekeeper, AuthenticationToken auth, long challenge, int userId) throws RemoteException {
        byte[] spHandle = loadSyntheticPasswordHandle(userId);
        if (spHandle == null) {
            return null;
        }
        VerifyCredentialResponse result;
        GateKeeperResponse response = gatekeeper.verifyChallenge(userId, challenge, spHandle, auth.deriveGkPassword());
        int responseCode = response.getResponseCode();
        if (responseCode == 0) {
            result = new VerifyCredentialResponse(response.getPayload());
            if (response.getShouldReEnroll()) {
                response = gatekeeper.enroll(userId, spHandle, spHandle, auth.deriveGkPassword());
                if (response.getResponseCode() == 0) {
                    saveSyntheticPasswordHandle(response.getPayload(), userId);
                    return verifyChallenge(gatekeeper, auth, challenge, userId);
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Fail to re-enroll SP handle for user ");
                stringBuilder.append(userId);
                Log.w(str, stringBuilder.toString());
            }
        } else {
            result = responseCode == 1 ? new VerifyCredentialResponse(response.getTimeout()) : VerifyCredentialResponse.ERROR;
        }
        return result;
    }

    public boolean existsHandle(long handle, int userId) {
        return hasState(SP_BLOB_NAME, handle, userId);
    }

    public void destroyTokenBasedSyntheticPassword(long handle, int userId) {
        destroySyntheticPassword(handle, userId);
        destroyState(SECDISCARDABLE_NAME, handle, userId);
    }

    public void destroyPasswordBasedSyntheticPassword(long handle, int userId) {
        destroySyntheticPassword(handle, userId);
        destroyState(SECDISCARDABLE_NAME, handle, userId);
        destroyState(PASSWORD_DATA_NAME, handle, userId);
    }

    private void destroySyntheticPassword(long handle, int userId) {
        destroyState(SP_BLOB_NAME, handle, userId);
        destroySPBlobKey(getHandleName(handle));
        if (hasState(WEAVER_SLOT_NAME, handle, userId)) {
            destroyWeaverSlot(handle, userId);
        }
    }

    private byte[] transformUnderWeaverSecret(byte[] data, byte[] secret) {
        byte[] weaverSecret = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_WEAVER_PASSWORD, secret);
        byte[] result = new byte[(data.length + weaverSecret.length)];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(weaverSecret, 0, result, data.length, weaverSecret.length);
        return result;
    }

    private byte[] transformUnderSecdiscardable(byte[] data, byte[] rawSecdiscardable) {
        byte[] secdiscardable = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_SECDISCARDABLE, rawSecdiscardable);
        byte[] result = new byte[(data.length + secdiscardable.length)];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(secdiscardable, 0, result, data.length, secdiscardable.length);
        return result;
    }

    private byte[] createSecdiscardable(long handle, int userId) {
        byte[] data = secureRandom(16384);
        saveSecdiscardable(handle, data, userId);
        return data;
    }

    private void saveSecdiscardable(long handle, byte[] secdiscardable, int userId) {
        saveState(SECDISCARDABLE_NAME, secdiscardable, handle, userId);
    }

    private byte[] loadSecdiscardable(long handle, int userId) {
        return loadState(SECDISCARDABLE_NAME, handle, userId);
    }

    private boolean hasState(String stateName, long handle, int userId) {
        return ArrayUtils.isEmpty(loadState(stateName, handle, userId)) ^ 1;
    }

    private byte[] loadState(String stateName, long handle, int userId) {
        return this.mStorage.readSyntheticPasswordState(userId, handle, stateName);
    }

    private void saveState(String stateName, byte[] data, long handle, int userId) {
        this.mStorage.writeSyntheticPasswordState(userId, handle, stateName, data);
    }

    private void destroyState(String stateName, long handle, int userId) {
        this.mStorage.deleteSyntheticPasswordState(userId, handle, stateName);
    }

    protected byte[] decryptSPBlob(String blobKeyName, byte[] blob, byte[] applicationId) {
        return SyntheticPasswordCrypto.decryptBlob(blobKeyName, blob, applicationId);
    }

    protected byte[] createSPBlob(String blobKeyName, byte[] data, byte[] applicationId, long sid, boolean managedProfile) {
        return SyntheticPasswordCrypto.createBlob(blobKeyName, data, applicationId, sid, managedProfile);
    }

    protected byte[] createSPBlob(String blobKeyName, byte[] data, byte[] applicationId, long sid) {
        return SyntheticPasswordCrypto.createBlob(blobKeyName, data, applicationId, sid, false);
    }

    protected void destroySPBlobKey(String keyAlias) {
        SyntheticPasswordCrypto.destroyBlobKey(keyAlias);
    }

    public static long generateHandle() {
        long result;
        SecureRandom rng = new SecureRandom();
        do {
            result = rng.nextLong();
        } while (result == 0);
        return result;
    }

    private int fakeUid(int uid) {
        return 100000 + uid;
    }

    protected static byte[] secureRandom(int length) {
        try {
            return SecureRandom.getInstance("SHA1PRNG").generateSeed(length);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getHandleName(long handle) {
        return String.format("%s%x", new Object[]{"synthetic_password_", Long.valueOf(handle)});
    }

    private byte[] computePasswordToken(String password, PasswordData data) {
        return scrypt(password, data.salt, 1 << data.scryptN, 1 << data.scryptR, 1 << data.scryptP, 32);
    }

    private byte[] passwordTokenToGkInput(byte[] token) {
        return SyntheticPasswordCrypto.personalisedHash(PERSONALIZATION_USER_GK_AUTH, token);
    }

    private byte[] passwordTokenToWeaverKey(byte[] token) {
        byte[] key = SyntheticPasswordCrypto.personalisedHash(PERSONALISATION_WEAVER_KEY, token);
        if (key.length >= this.mWeaverConfig.keySize) {
            return Arrays.copyOf(key, this.mWeaverConfig.keySize);
        }
        throw new RuntimeException("weaver key length too small");
    }

    protected long sidFromPasswordHandle(byte[] handle) {
        return nativeSidFromPasswordHandle(handle);
    }

    protected byte[] scrypt(String password, byte[] salt, int N, int r, int p, int outLen) {
        return nativeScrypt(password.getBytes(), salt, N, r, p, outLen);
    }

    protected static ArrayList<Byte> toByteArrayList(byte[] data) {
        ArrayList<Byte> result = new ArrayList(data.length);
        for (byte valueOf : data) {
            result.add(Byte.valueOf(valueOf));
        }
        return result;
    }

    protected static byte[] fromByteArrayList(ArrayList<Byte> data) {
        byte[] result = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            result[i] = ((Byte) data.get(i)).byteValue();
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        char[] hexChars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[(j * 2) + 1] = hexArray[v & 15];
        }
        return new String(hexChars);
    }
}
