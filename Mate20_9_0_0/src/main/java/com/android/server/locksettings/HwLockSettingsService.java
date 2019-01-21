package com.android.server.locksettings;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.security.keystore.KeyProtection.Builder;
import android.service.gatekeeper.GateKeeperResponse;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.ICheckCredentialProgressCallback.Stub;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.locksettings.LockSettingsService.Injector;
import com.android.server.locksettings.LockSettingsStorage.CredentialHash;
import com.android.server.security.antimal.HwAntiMalStatus;
import com.huawei.android.os.UserManagerEx;
import com.huawei.pwdprotect.PwdProtectManager;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONException;
import org.json.JSONObject;

public class HwLockSettingsService extends LockSettingsService {
    public static final String DB_KEY_PRIVACY_USER_PWD_PROTECT = "privacy_user_pwd_protect";
    public static final String DESCRIPTOR = "com.android.internal.widget.ILockSettings";
    private static final String EMPTY_APP_NAME = "";
    private static final boolean ENHANCED_GK_RULE = SystemProperties.getBoolean("ro.build.enhanced_gk_rule", false);
    private static final String JSON_ERROR_COUNT = "error";
    private static final String JSON_LEVEL = "level";
    private static final String JSON_START_ELAPSED = "elapsed";
    private static final String JSON_START_RTC = "rtc";
    private static final String JSON_STOP_ELAPSED = "stop";
    private static final String KEY_CREDENTIAL_LEN = "lockscreen.hw_credential_len";
    private static final String KEY_HAS_HW_LOCK_HINT = "lockscreen.has_hw_hint_info";
    private static final String KEY_HW_LOCK_HINT = "lockscreen.hw_hint_info";
    private static final String KEY_HW_PIN_TYPE = "lockscreen.pin_type";
    private static final long KEY_UNLOCK_TYPE_NOT_SET = -1;
    private static final String KEY_UPDATE_WEAK_AUTH_TIME = "lockscreen.hw_weak_auth_time";
    public static final String LOCK_PASSWORD_FILE2 = "password2.key";
    public static final String NO_PWD_FOR_PWD_PROTECT = "no_pwd_for_protect_protect";
    private static final String NRLTAG = "LockSettingsRule";
    private static final int PASSWORD_STATUS_CHANGED = 2;
    private static final int PASSWORD_STATUS_OFF = 0;
    private static final int PASSWORD_STATUS_ON = 1;
    private static final String PERMISSION = "android.permission.ACCESS_KEYGUARD_SECURE_STORAGE";
    private static final String PERMISSION_GET_LOCK_PASSWORD_CHANGED = "com.huawei.permission.GET_LOCK_PASSWORD_CHANGED";
    private static final String PKG_HWOUC = "com.huawei.android.hwouc";
    private static final String PKG_SECURITYMGR = "com.huawei.securitymgr";
    private static final String PKG_SETTINGS = "com.android.settings";
    private static final String PKG_SYSTEMUI = "com.android.systemui";
    private static final String PROFILE_KEY_USER_HINT_DECRYPT = "profile_key_user_hint_decrypt_";
    private static final String PROFILE_KEY_USER_HINT_ENCRYPT = "profile_key_user_hint_encrypt_";
    private static final String PWD_ERROR_COUNT = "gk_rule_error_count";
    private static final String PWD_START_TIME_ELAPSED = "password_start_time_elapsed";
    private static final String PWD_VERIFY_INFO = "password_verification_information";
    private static final String RECEIVER_ACTION_LOCK_PASSWORD_CHANGED = "com.huawei.locksettingsservice.action.LOCK_PASSWORD_CHANGED";
    private static final String RECEIVER_PACKAGE = "com.huawei.hwid";
    private static final int SECURITY_LOCK_SETTINGS = 2;
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TAG = "HwLockSettingsService";
    private static final String USER_LOCK_HINT_FILE = "hw_lock_hint.key";
    private static final int WEAK_AUTH_FACE = 10001;
    private static final int WEAK_AUTH_FINGER = 10002;
    public static final int transaction_checkvisitorpassword = 1002;
    public static final int transaction_setlockvisitorpassword = 1001;
    private final Context mContext;
    private HwAntiMalStatus mHwAntiMalStatus = null;
    private final HwLockSettingsStorage mStorage2;

    static class HwInjector extends Injector {
        public HwInjector(Context context) {
            super(context);
        }

        public LockSettingsStrongAuth getStrongAuth() {
            return new HwLockSettingsStrongAuth(this.mContext);
        }
    }

    public HwLockSettingsService(Context context) {
        super(new HwInjector(context));
        this.mStorage2 = new HwLockSettingsStorage(context);
        this.mContext = context;
        this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
    }

    private void setVisitorLockPassword(String password, int userId) throws RemoteException {
        checkWritePermission(userId);
        setKeystorePassword(password, userId);
    }

    public boolean checkVisitorPassword(String password, int userId) throws RemoteException {
        return false;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case 1001:
                data.enforceInterface("com.android.internal.widget.ILockSettings");
                setVisitorLockPassword(data.readString(), data.readInt());
                reply.writeInt(0);
                reply.writeNoException();
                return true;
            case 1002:
                data.enforceInterface("com.android.internal.widget.ILockSettings");
                if (checkVisitorPassword(data.readString(), data.readInt())) {
                    reply.writeInt(0);
                } else {
                    reply.writeInt(1);
                }
                reply.writeNoException();
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    protected int getOldCredentialType(int userId) {
        CredentialHash oldCredentialHash = this.mStorage.readCredentialHash(userId);
        if (oldCredentialHash != null) {
            return oldCredentialHash.type;
        }
        return -1;
    }

    protected int getPasswordStatus(int currentCredentialType, int oldCredentialType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPasswordStatus, currentCredentialType=");
        stringBuilder.append(currentCredentialType);
        stringBuilder.append(", oldCredentialType=");
        stringBuilder.append(oldCredentialType);
        Slog.i(str, stringBuilder.toString());
        if (currentCredentialType == -1) {
            return 0;
        }
        if (oldCredentialType == -1) {
            return 1;
        }
        return 2;
    }

    protected void notifyPasswordStatusChanged(int userId, int status) {
        Intent intent = new Intent(RECEIVER_ACTION_LOCK_PASSWORD_CHANGED);
        intent.setPackage(RECEIVER_PACKAGE);
        intent.putExtra("status", status);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyPasswordStatusChanged:");
        stringBuilder.append(status);
        stringBuilder.append(", userId:");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(userId), PERMISSION_GET_LOCK_PASSWORD_CHANGED);
        if (status != 0) {
            this.mStrongAuth.reportSuccessfulStrongAuthUnlock(userId);
        }
    }

    protected void notifyModifyPwdForPrivSpacePwdProtect(String credential, String savedCredential, int userId) {
        if (isPrivSpacePwdProtectOpened() && isPrivacyUser(this.mContext, userId)) {
            Slog.i(TAG, "notify privPw ");
            if (!PwdProtectManager.getInstance().modifyPrivPwd(credential)) {
                Log.e(TAG, "modifyPrivSpacePn fail");
            }
            if (UserHandle.myUserId() == 0) {
                Flog.bdReport(this.mContext, 131);
            }
        }
        if (isPrivSpacePwdProtectOpened() && userId == 0) {
            Slog.i(TAG, "notify mainPw ");
            if (TextUtils.isEmpty(savedCredential)) {
                savedCredential = NO_PWD_FOR_PWD_PROTECT;
            }
            if (TextUtils.isEmpty(credential)) {
                credential = NO_PWD_FOR_PWD_PROTECT;
            }
            if (!PwdProtectManager.getInstance().modifyMainPwd(savedCredential, credential)) {
                Log.e(TAG, "modifyMainSpacePn fail");
            }
        }
    }

    protected void notifyBigDataForPwdProtectFail(int userId) {
        if (isPrivSpacePwdProtectOpened() && isPrivacyUser(this.mContext, userId) && UserHandle.myUserId() == 0) {
            Flog.bdReport(this.mContext, 132);
        }
    }

    private boolean isPrivacyUser(Context context, int userId) {
        if (context == null) {
            return false;
        }
        return UserManagerEx.isHwHiddenSpace(UserManagerEx.getUserInfoEx((UserManager) context.getSystemService("user"), userId));
    }

    private boolean isPrivSpacePwdProtectOpened() {
        boolean z = false;
        if (this.mContext == null) {
            return false;
        }
        if (Global.getInt(this.mContext.getContentResolver(), DB_KEY_PRIVACY_USER_PWD_PROTECT, 0) == 1) {
            z = true;
        }
        return z;
    }

    @VisibleForTesting
    private void saveUserHintMessage(int userId, String hint) throws RuntimeException, IOException {
        byte[] randomLockSeed = hint.getBytes(StandardCharsets.UTF_8);
        KeyStore keyStore;
        StringBuilder stringBuilder;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            stringBuilder = new StringBuilder();
            stringBuilder.append(PROFILE_KEY_USER_HINT_ENCRYPT);
            stringBuilder.append(userId);
            keyStore.setEntry(stringBuilder.toString(), new SecretKeyEntry(secretKey), new Builder(1).setBlockModes(new String[]{"GCM"}).setEncryptionPaddings(new String[]{"NoPadding"}).build());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PROFILE_KEY_USER_HINT_DECRYPT);
            stringBuilder.append(userId);
            keyStore.setEntry(stringBuilder.toString(), new SecretKeyEntry(secretKey), new Builder(2).setBlockModes(new String[]{"GCM"}).setEncryptionPaddings(new String[]{"NoPadding"}).setCriticalToDeviceEncryption(true).build());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PROFILE_KEY_USER_HINT_ENCRYPT);
            stringBuilder.append(userId);
            SecretKey keyStoreEncryptionKey = (SecretKey) keyStore.getKey(stringBuilder.toString(), null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, keyStoreEncryptionKey);
            byte[] encryptionResult = cipher.doFinal(randomLockSeed);
            byte[] iv = cipher.getIV();
            stringBuilder = new StringBuilder();
            stringBuilder.append(PROFILE_KEY_USER_HINT_ENCRYPT);
            stringBuilder.append(userId);
            keyStore.deleteEntry(stringBuilder.toString());
            keyGenerator = new ByteArrayOutputStream();
            try {
                if (iv.length == 12) {
                    keyGenerator.write(iv);
                    keyGenerator.write(encryptionResult);
                    writeHwUserLockHint(userId, keyGenerator.toByteArray());
                    return;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid iv length: ");
                stringBuilder2.append(iv.length);
                throw new RuntimeException(stringBuilder2.toString());
            } catch (IOException secretKey2) {
                throw new RuntimeException("Failed to concatenate byte arrays", secretKey2);
            }
        } catch (IOException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to encrypt key", e);
        } catch (Throwable th) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(PROFILE_KEY_USER_HINT_ENCRYPT);
            stringBuilder.append(userId);
            keyStore.deleteEntry(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    private String getUserHintMessage(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, CertificateException, IOException {
        byte[] storedData = readHwUserLockHint(userId);
        if (storedData != null) {
            byte[] iv = Arrays.copyOfRange(storedData, null, 12);
            byte[] encryptedPassword = Arrays.copyOfRange(storedData, 12, storedData.length);
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PROFILE_KEY_USER_HINT_DECRYPT);
            stringBuilder.append(userId);
            SecretKey decryptionKey = (SecretKey) keyStore.getKey(stringBuilder.toString(), null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(2, decryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encryptedPassword), StandardCharsets.UTF_8);
        }
        throw new FileNotFoundException("Child profile lock file not found");
    }

    @VisibleForTesting
    String getHwUserLockHintFile(int userId) {
        return this.mStorage.getLockCredentialFilePathForUser(userId, USER_LOCK_HINT_FILE);
    }

    @VisibleForTesting
    private void writeHwUserLockHint(int userId, byte[] lock) {
        this.mStorage.writeFile(getHwUserLockHintFile(userId), lock);
    }

    private byte[] readHwUserLockHint(int userId) {
        return this.mStorage.readFile(getHwUserLockHintFile(userId));
    }

    private boolean hasHwUserLockHint(int userId) {
        return this.mStorage.hasFile(getHwUserLockHintFile(userId));
    }

    public void setString(String key, String value, int userId) {
        if (KEY_HW_LOCK_HINT.equals(key)) {
            checkWritePermission(userId);
            if ("com.android.settings".equals(getPackageNameFromPid(this.mContext, Binder.getCallingPid()))) {
                try {
                    String str;
                    StringBuilder stringBuilder;
                    if (TextUtils.isEmpty(value)) {
                        this.mStorage.deleteFile(getHwUserLockHintFile(userId));
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("delete UserHintMessage succ : ");
                        stringBuilder.append(key);
                        Log.v(str, stringBuilder.toString());
                    } else {
                        saveUserHintMessage(userId, value);
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("save UserHintMessage succ : ");
                        stringBuilder.append(key);
                        Log.v(str, stringBuilder.toString());
                    }
                    return;
                } catch (IOException | RuntimeException e) {
                    Log.e(TAG, "save fail");
                }
            }
        }
        if (PWD_VERIFY_INFO.equals(key) && isUseGKRule(userId)) {
            resetTime(userId);
        } else if (PWD_START_TIME_ELAPSED.equals(key) && isUseGKRule(userId)) {
            try {
                JSONObject obj = new JSONObject(value);
                setStartTime(userId, obj.getLong(JSON_START_RTC), obj.getLong(JSON_START_ELAPSED), obj.getLong(JSON_STOP_ELAPSED));
            } catch (JSONException e2) {
                Log.e(TAG, "setStartTime JSONException");
            }
        } else {
            super.setString(key, value, userId);
        }
    }

    private boolean isCalledFromSysUI(int pid) {
        return "com.android.systemui".equals(getPackageNameFromPid(this.mContext, pid));
    }

    public long getLong(String key, long defaultValue, int userId) {
        if (!KEY_CREDENTIAL_LEN.equals(key) || isCalledFromSettings(Binder.getCallingPid())) {
            return super.getLong(key, defaultValue, userId);
        }
        return -1;
    }

    public void setLong(String key, long value, int userId) {
        if (KEY_CREDENTIAL_LEN.equals(key)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid set credential from UID: ");
            stringBuilder.append(Binder.getCallingUid());
            Log.i(str, stringBuilder.toString());
        } else if (KEY_UPDATE_WEAK_AUTH_TIME.equals(key)) {
            if (isCalledFromSysUI(Binder.getCallingPid())) {
                reportSuccessfulWeakAuthUnlock((int) value, userId);
            }
        } else {
            super.setLong(key, value, userId);
        }
    }

    protected void setLockCredentialInternal(String credential, int credentialType, String savedCredential, int requestedQuality, int userId) throws RemoteException {
        super.setLockCredentialInternal(credential, credentialType, savedCredential, requestedQuality, userId);
        super.setLong(KEY_CREDENTIAL_LEN, TextUtils.isEmpty(credential) ? 0 : (long) credential.length(), userId);
    }

    private void reportSuccessfulWeakAuthUnlock(int type, int userId) {
        if (!"com.android.systemui".equals(getPackageNameFromPid(this.mContext, Binder.getCallingPid()))) {
            return;
        }
        if ((getStrongAuthForUser(userId) & 16) != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("report WeakAuth but already timeout ");
            stringBuilder.append(type);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        if (10001 == type || 10002 == type) {
            this.mStrongAuth.reportSuccessfulWeakAuthUnlock(userId);
        }
    }

    public String getString(String key, String defaultValue, int userId) {
        if (KEY_HW_LOCK_HINT.equals(key) && hasHwUserLockHint(userId)) {
            checkWritePermission(userId);
            String callProcessAppName = getPackageNameFromPid(this.mContext, Binder.getCallingPid());
            if ("com.android.systemui".equals(callProcessAppName) || "com.android.settings".equals(callProcessAppName)) {
                try {
                    String hintInfo = getUserHintMessage(userId);
                    return TextUtils.isEmpty(hintInfo) ? defaultValue : hintInfo;
                } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                    Log.e(TAG, "get UserHintMessage fail");
                }
            }
        }
        if (PWD_VERIFY_INFO.equals(key) && isUseGKRule(userId)) {
            return getVerifyInfo(userId).toString();
        }
        if (PWD_ERROR_COUNT.equals(key) && isUseGKRule(userId)) {
            return String.valueOf(getErrorCount(userId));
        }
        return super.getString(key, defaultValue, userId);
    }

    public boolean getBoolean(String key, boolean defaultValue, int userId) {
        checkWritePermission(userId);
        if (KEY_HAS_HW_LOCK_HINT.equals(key)) {
            return hasHwUserLockHint(userId);
        }
        return super.getBoolean(key, defaultValue, userId);
    }

    private static String getPackageNameFromPid(Context context, int pid) {
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        if (am == null) {
            Log.e(TAG, "can't get ACTIVITY_SERVICE");
            return "";
        }
        List<RunningAppProcessInfo> acts = am.getRunningAppProcesses();
        if (acts == null) {
            Log.e(TAG, "can't get Running App");
            return "";
        }
        int len = acts.size();
        for (int i = 0; i < len; i++) {
            RunningAppProcessInfo rapInfo = (RunningAppProcessInfo) acts.get(i);
            if (rapInfo.pid == pid) {
                return rapInfo.processName;
            }
        }
        return "";
    }

    private VerifyCredentialResponse verifyCredentialEx(int userId, CredentialHash storedHash, String credential, boolean hasChallenge, long challenge, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        if ((storedHash == null || storedHash.hash.length == 0) && TextUtils.isEmpty(credential)) {
            Slog.w(TAG, "no stored Password/Pattern, verifyCredential success");
            return VerifyCredentialResponse.OK;
        } else if (storedHash == null || TextUtils.isEmpty(credential)) {
            Slog.w(TAG, "no entered Password/Pattern, verifyCredential ERROR");
            return VerifyCredentialResponse.ERROR;
        } else {
            StrictMode.noteDiskRead();
            try {
                if (getGateKeeperService() == null) {
                    return VerifyCredentialResponse.ERROR;
                }
                return convertResponse(getGateKeeperService().verifyChallenge(userId, challenge, storedHash.hash, credential.getBytes(StandardCharsets.UTF_8)));
            } catch (RemoteException e) {
                return VerifyCredentialResponse.ERROR;
            }
        }
    }

    private VerifyCredentialResponse doVerifyCredentialEx(String credential, int credentialType, boolean hasChallenge, long challenge, int userId, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        int i = userId;
        CredentialHash storedHash = this.mStorage2.readCredentialHashEx(i);
        if (storedHash != null && storedHash.hash != null && storedHash.hash.length != 0) {
            return verifyCredentialEx(i, storedHash, credential, hasChallenge, challenge, progressCallback);
        }
        Slog.w(TAG, "no Pattern saved VerifyPattern success");
        return VerifyCredentialResponse.OK;
    }

    private boolean checkPasswordEx(String password, int userId, ICheckCredentialProgressCallback progressCallback) {
        if (password == null || password.equals("")) {
            return false;
        }
        try {
            if (doVerifyCredentialEx(password, 2, false, 0, userId, progressCallback).getResponseCode() == 0) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    private byte[] enrollCredentialEx(byte[] enrolledHandle, String enrolledCredential, String toEnroll, int userId) throws RemoteException {
        byte[] toEnrollBytes = null;
        byte[] enrolledCredentialBytes = enrolledCredential == null ? null : enrolledCredential.getBytes(StandardCharsets.UTF_8);
        if (toEnroll != null) {
            toEnrollBytes = toEnroll.getBytes(StandardCharsets.UTF_8);
        }
        if (getGateKeeperService() == null) {
            Slog.e(TAG, "getGateKeeperService fail.");
            return new byte[0];
        }
        GateKeeperResponse response = getGateKeeperService().enroll(userId, enrolledHandle, enrolledCredentialBytes, toEnrollBytes);
        if (response != null) {
            return response.getPayload();
        }
        Slog.w(TAG, "enrollCredential response null");
        return new byte[0];
    }

    private void checkPasswordReadPermission(int userId) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsRead");
    }

    private VerifyCredentialResponse convertResponse(GateKeeperResponse gateKeeperResponse) {
        return VerifyCredentialResponse.fromGateKeeperResponse(gateKeeperResponse);
    }

    public boolean setExtendLockScreenPassword(String password, String phoneNumber, int userHandle) {
        checkWritePermission(userHandle);
        try {
            byte[] enrolledHandle = enrollCredentialEx(this.mStorage2.readCredentialHashEx(userHandle).hash, null, password, userHandle);
            if (enrolledHandle.length == 0) {
                return false;
            }
            this.mStorage2.writeCredentialHashEx(CredentialHash.create(enrolledHandle, 2), userHandle);
            Intent intent = new Intent("com.huawei.intent.action.OPERATOR_REMOTE_LOCK");
            intent.setPackage("com.android.systemui");
            intent.putExtra("PhoneNumber", phoneNumber);
            this.mContext.sendBroadcast(intent);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean clearExtendLockScreenPassword(String password, int userHandle) {
        checkPasswordReadPermission(userHandle);
        if (!this.mStorage2.hasSetPassword(userHandle)) {
            Slog.i(TAG, "has not set password");
            return false;
        } else if (checkPasswordEx(password, userHandle, false)) {
            this.mStorage2.deleteExPasswordFile(userHandle);
            Intent intent = new Intent("com.huawei.intent.action.OPERATOR_REMOTE_UNLOCK");
            intent.setPackage("com.android.systemui");
            this.mContext.sendBroadcast(intent);
            return true;
        } else {
            Slog.e(TAG, "wrong unlock password");
            return false;
        }
    }

    public void handleUserClearLockForAnti(int userId) {
        if (this.mHwAntiMalStatus == null) {
            this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
        }
        this.mHwAntiMalStatus.handleUserClearLockForAntiMal(userId);
    }

    public void setLockCredential(String credential, int type, String savedCredential, int requestedQuality, int userId) throws RemoteException {
        clearPasswordType(userId);
        super.setLockCredential(credential, type, savedCredential, requestedQuality, userId);
    }

    public boolean setLockCredentialWithToken(String credential, int type, long tokenHandle, byte[] token, int requestedQuality, int userId) throws RemoteException {
        clearPasswordType(userId);
        return super.setLockCredentialWithToken(credential, type, tokenHandle, token, requestedQuality, userId);
    }

    private void clearPasswordType(int userId) throws RemoteException {
        super.setLong(KEY_HW_PIN_TYPE, -1, userId);
    }

    private int getErrorCount(int userId) {
        return Integer.parseInt(this.mStorage2.readKeyValue(JSON_ERROR_COUNT, "0", userId));
    }

    private void addErrorCount(int userId) {
        int errorCount = getErrorCount(userId) + 1;
        String str = NRLTAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("locksettingsservice addErrorCount=");
        stringBuilder.append(errorCount);
        stringBuilder.append(", userId ");
        stringBuilder.append(userId);
        Slog.w(str, stringBuilder.toString());
        this.mStorage2.writeKeyValue(JSON_ERROR_COUNT, String.valueOf(errorCount), userId);
    }

    private void resetErrorCount(int userId) {
        String str = NRLTAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("locksettingsservice resetErrorCount, userId");
        stringBuilder.append(userId);
        Slog.w(str, stringBuilder.toString());
        this.mStorage2.writeKeyValue(JSON_ERROR_COUNT, String.valueOf(0), userId);
        resetTime(userId);
    }

    private void setStartTime(int userId, long startTimeRTC, long startTimeElapsed, long stopTimeInFuture) {
        if (startTimeElapsed >= SystemClock.elapsedRealtime() || stopTimeInFuture > SystemClock.elapsedRealtime()) {
            this.mStorage2.writeKeyValue(JSON_START_RTC, String.valueOf(startTimeRTC), userId);
            this.mStorage2.writeKeyValue(JSON_START_ELAPSED, String.valueOf(startTimeElapsed), userId);
        }
    }

    private void resetTime(int userId) {
        this.mStorage2.writeKeyValue(JSON_START_RTC, String.valueOf(0), userId);
        this.mStorage2.writeKeyValue(JSON_START_ELAPSED, String.valueOf(0), userId);
    }

    private long getStartTimeRTC(int userId) {
        return Long.parseLong(this.mStorage2.readKeyValue(JSON_START_RTC, "0", userId));
    }

    private long getStartTimeElapsed(int userId) {
        return Long.parseLong(this.mStorage2.readKeyValue(JSON_START_ELAPSED, "0", userId));
    }

    private synchronized JSONObject getVerifyInfo(int userId) {
        JSONObject obj;
        obj = new JSONObject();
        try {
            obj.put("level", 2);
            obj.put(JSON_ERROR_COUNT, getErrorCount(userId));
            obj.put(JSON_START_RTC, getStartTimeRTC(userId));
            obj.put(JSON_START_ELAPSED, getStartTimeElapsed(userId));
        } catch (JSONException e) {
            Slog.e(TAG, "toJson error", e);
        }
        return obj;
    }

    private boolean isCalledFromSettings(int pid) {
        String callProcessAppName = getPackageNameFromPid(this.mContext, pid);
        return "com.android.settings".equals(callProcessAppName) || PKG_HWOUC.equals(callProcessAppName) || "com.android.systemui".equals(callProcessAppName) || PKG_SECURITYMGR.equals(callProcessAppName);
    }

    public VerifyCredentialResponse checkCredential(String credential, int type, final int userId, ICheckCredentialProgressCallback progressCallback) throws RemoteException {
        if (!isUseGKRule(userId)) {
            return super.checkCredential(credential, type, userId, progressCallback);
        }
        final ICheckCredentialProgressCallback callback = progressCallback;
        VerifyCredentialResponse response = super.checkCredential(credential, type, userId, new Stub() {
            public void onCredentialVerified() throws RemoteException {
                if (callback != null) {
                    callback.onCredentialVerified();
                }
                HwLockSettingsService.this.checkError(0, userId, 0);
            }
        });
        checkError(response.getResponseCode(), userId, response.getTimeout());
        return response;
    }

    public VerifyCredentialResponse verifyCredential(String credential, int type, long challenge, int userId) throws RemoteException {
        VerifyCredentialResponse response = super.verifyCredential(credential, type, challenge, userId);
        if (isUseGKRule(userId)) {
            checkError(response.getResponseCode(), userId, response.getTimeout());
        }
        return response;
    }

    private void checkError(int responseCode, int userId, int timeOut) {
        String str = NRLTAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkError responseCode:");
        stringBuilder.append(responseCode);
        Slog.i(str, stringBuilder.toString());
        switch (responseCode) {
            case -1:
                addErrorCount(userId);
                return;
            case 0:
                resetErrorCount(userId);
                resetStrongAuth(userId);
                return;
            case 1:
                long startTimeElapsed = SystemClock.elapsedRealtime();
                int i = userId;
                setStartTime(i, System.currentTimeMillis(), startTimeElapsed, startTimeElapsed + ((long) timeOut));
                addErrorCount(userId);
                return;
            default:
                return;
        }
    }

    private void resetStrongAuth(int userId) {
        if ((getStrongAuthForUser(userId) & 8) != 0) {
            Slog.w(NRLTAG, "clear AFTER_LOCKOUT_AUTH flag after verifyCredential");
            requireStrongAuth(0, userId);
        }
    }

    private boolean isUseGKRule(int userId) {
        if (!ENHANCED_GK_RULE || !isCalledFromSettings(Binder.getCallingPid())) {
            return false;
        }
        checkWritePermission(userId);
        return true;
    }
}
