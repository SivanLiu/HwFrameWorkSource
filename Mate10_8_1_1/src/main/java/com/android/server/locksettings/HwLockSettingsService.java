package com.android.server.locksettings;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.security.keystore.KeyProtection.Builder;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import com.android.server.locksettings.LockSettingsService.Injector;
import com.android.server.locksettings.LockSettingsStorage.CredentialHash;
import com.android.server.rms.iaware.cpu.CPUFeature;
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

public class HwLockSettingsService extends LockSettingsService {
    public static final String DB_KEY_PRIVACY_USER_PWD_PROTECT = "privacy_user_pwd_protect";
    public static final String DESCRIPTOR = "com.android.internal.widget.ILockSettings";
    private static final String EMPTY_APP_NAME = "";
    private static final String KEY_CREDENTIAL_LEN = "lockscreen.hw_credential_len";
    private static final String KEY_HAS_HW_LOCK_HINT = "lockscreen.has_hw_hint_info";
    private static final String KEY_HW_LOCK_HINT = "lockscreen.hw_hint_info";
    private static final String KEY_HW_PIN_TYPE = "lockscreen.pin_type";
    private static final long KEY_UNLOCK_TYPE_NOT_SET = -1;
    private static final String KEY_UPDATE_WEAK_AUTH_TIME = "lockscreen.hw_weak_auth_time";
    public static final String LOCK_PASSWORD_FILE2 = "password2.key";
    public static final String NO_PWD_FOR_PWD_PROTECT = "no_pwd_for_protect_protect";
    private static final int PASSWORD_STATUS_CHANGED = 2;
    private static final int PASSWORD_STATUS_OFF = 0;
    private static final int PASSWORD_STATUS_ON = 1;
    private static final String PERMISSION_GET_LOCK_PASSWORD_CHANGED = "com.huawei.permission.GET_LOCK_PASSWORD_CHANGED";
    private static final String PKG_SETTINGS = "com.android.settings";
    private static final String PKG_SYSTEMUI = "com.android.systemui";
    private static final String PROFILE_KEY_USER_HINT_DECRYPT = "profile_key_user_hint_decrypt_";
    private static final String PROFILE_KEY_USER_HINT_ENCRYPT = "profile_key_user_hint_encrypt_";
    private static final String RECEIVER_ACTION_LOCK_PASSWORD_CHANGED = "com.huawei.locksettingsservice.action.LOCK_PASSWORD_CHANGED";
    private static final String RECEIVER_PACKAGE = "com.huawei.hwid";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TAG = "HwLockSettingsService";
    private static final String USER_LOCK_HINT_FILE = "hw_lock_hint.key";
    private static final int WEAK_AUTH_FACE = 10001;
    private static final int WEAK_AUTH_FINGER = 10002;
    public static final int transaction_checkvisitorpassword = 1002;
    public static final int transaction_setlockvisitorpassword = 1001;
    private final Context mContext;
    private HwAntiMalStatus mHwAntiMalStatus = null;

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
        Slog.i(TAG, "getPasswordStatus, currentCredentialType=" + currentCredentialType + ", oldCredentialType=" + oldCredentialType);
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
        Slog.i(TAG, "notifyPasswordStatusChanged:" + status + ", userId:" + userId);
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
                Flog.bdReport(this.mContext, CPUFeature.MSG_SET_INTERACTIVE_SPSAVE);
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
        boolean z = true;
        if (this.mContext == null) {
            return false;
        }
        if (Global.getInt(this.mContext.getContentResolver(), DB_KEY_PRIVACY_USER_PWD_PROTECT, 0) != 1) {
            z = false;
        }
        return z;
    }

    private void saveUserHintMessage(int userId, String hint) throws RuntimeException, IOException {
        byte[] randomLockSeed = hint.getBytes(StandardCharsets.UTF_8);
        KeyStore keyStore;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.setEntry(PROFILE_KEY_USER_HINT_ENCRYPT + userId, new SecretKeyEntry(secretKey), new Builder(1).setBlockModes(new String[]{"GCM"}).setEncryptionPaddings(new String[]{"NoPadding"}).build());
            keyStore.setEntry(PROFILE_KEY_USER_HINT_DECRYPT + userId, new SecretKeyEntry(secretKey), new Builder(2).setBlockModes(new String[]{"GCM"}).setEncryptionPaddings(new String[]{"NoPadding"}).setCriticalToDeviceEncryption(true).build());
            SecretKey keyStoreEncryptionKey = (SecretKey) keyStore.getKey(PROFILE_KEY_USER_HINT_ENCRYPT + userId, null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, keyStoreEncryptionKey);
            byte[] encryptionResult = cipher.doFinal(randomLockSeed);
            byte[] iv = cipher.getIV();
            keyStore.deleteEntry(PROFILE_KEY_USER_HINT_ENCRYPT + userId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                if (iv.length != 12) {
                    throw new RuntimeException("Invalid iv length: " + iv.length);
                }
                outputStream.write(iv);
                outputStream.write(encryptionResult);
                writeHwUserLockHint(userId, outputStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Failed to concatenate byte arrays", e);
            }
        } catch (Exception e2) {
            throw new RuntimeException("Failed to encrypt key", e2);
        } catch (Throwable th) {
            keyStore.deleteEntry(PROFILE_KEY_USER_HINT_ENCRYPT + userId);
        }
    }

    private String getUserHintMessage(int userId) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, CertificateException, IOException {
        byte[] storedData = readHwUserLockHint(userId);
        if (storedData == null) {
            throw new FileNotFoundException("Child profile lock file not found");
        }
        byte[] iv = Arrays.copyOfRange(storedData, 0, 12);
        byte[] encryptedPassword = Arrays.copyOfRange(storedData, 12, storedData.length);
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        SecretKey decryptionKey = (SecretKey) keyStore.getKey(PROFILE_KEY_USER_HINT_DECRYPT + userId, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, decryptionKey, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encryptedPassword), StandardCharsets.UTF_8);
    }

    String getHwUserLockHintFile(int userId) {
        return this.mStorage.getLockCredentialFilePathForUser(userId, USER_LOCK_HINT_FILE);
    }

    private void writeHwUserLockHint(int userId, byte[] lock) {
        this.mStorage.writeFile(getHwUserLockHintFile(userId), lock);
    }

    private byte[] readHwUserLockHint(int userId) {
        return this.mStorage.readFile(getHwUserLockHintFile(userId));
    }

    private boolean hasHwUserLockHint(int userId) {
        return this.mStorage.hasFile(getHwUserLockHintFile(userId));
    }

    public void setString(String key, String value, int userId) throws RemoteException {
        if (KEY_HW_LOCK_HINT.equals(key)) {
            checkWritePermission(userId);
            if ("com.android.settings".equals(getPackageNameFromPid(this.mContext, Binder.getCallingPid()))) {
                try {
                    if (TextUtils.isEmpty(value)) {
                        this.mStorage.deleteFile(getHwUserLockHintFile(userId));
                        Log.v(TAG, "delete UserHintMessage succ : " + key);
                    } else {
                        saveUserHintMessage(userId, value);
                        Log.v(TAG, "save UserHintMessage succ : " + key);
                    }
                    return;
                } catch (RuntimeException e) {
                    throw new RemoteException("saveHintInfo fail");
                }
            }
        }
        super.setString(key, value, userId);
    }

    private boolean isCalledFromSysUI(int pid) {
        return "com.android.systemui".equals(getPackageNameFromPid(this.mContext, pid));
    }

    public long getLong(String key, long defaultValue, int userId) throws RemoteException {
        if (!KEY_CREDENTIAL_LEN.equals(key) || isCalledFromSysUI(Binder.getCallingPid())) {
            return super.getLong(key, defaultValue, userId);
        }
        return -1;
    }

    public void setLong(String key, long value, int userId) throws RemoteException {
        if (KEY_CREDENTIAL_LEN.equals(key)) {
            Log.i(TAG, "invalid set credential from UID: " + Binder.getCallingUid());
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
        super.setLong(KEY_CREDENTIAL_LEN, (long) (TextUtils.isEmpty(savedCredential) ? 0 : savedCredential.length()), userId);
    }

    public void setLockCredential(String credential, int type, String savedCredential, int requestedQuality, int userId) throws RemoteException {
        clearPasswordType(userId);
        super.setLockCredential(credential, type, savedCredential, requestedQuality, userId);
    }

    public boolean setLockCredentialWithToken(String credential, int type, long tokenHandle, byte[] token, int requestedQuality, int userId) throws RemoteException {
        clearPasswordType(userId);
        return super.setLockCredentialWithToken(credential, type, tokenHandle, token, requestedQuality, userId);
    }

    private void reportSuccessfulWeakAuthUnlock(int type, int userId) {
        if (!"com.android.systemui".equals(getPackageNameFromPid(this.mContext, Binder.getCallingPid()))) {
            return;
        }
        if ((getStrongAuthForUser(userId) & 16) != 0) {
            Slog.e(TAG, "report WeakAuth but already timeout " + type);
            return;
        }
        if (10001 == type || 10002 == type) {
            this.mStrongAuth.reportSuccessfulWeakAuthUnlock(userId);
        }
    }

    public String getString(String key, String defaultValue, int userId) throws RemoteException {
        if (KEY_HW_LOCK_HINT.equals(key) && hasHwUserLockHint(userId)) {
            checkWritePermission(userId);
            String callProcessAppName = getPackageNameFromPid(this.mContext, Binder.getCallingPid());
            if ("com.android.systemui".equals(callProcessAppName) || "com.android.settings".equals(callProcessAppName)) {
                try {
                    String hintInfo = getUserHintMessage(userId);
                    if (!TextUtils.isEmpty(hintInfo)) {
                        defaultValue = hintInfo;
                    }
                    return defaultValue;
                } catch (KeyStoreException e) {
                    throw new RemoteException("getHintInfo fail.");
                }
            }
        }
        return super.getString(key, defaultValue, userId);
    }

    public boolean getBoolean(String key, boolean defaultValue, int userId) throws RemoteException {
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

    public void handleUserClearLockForAnti(int userId) {
        if (this.mHwAntiMalStatus == null) {
            this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
        }
        this.mHwAntiMalStatus.handleUserClearLockForAntiMal(userId);
    }

    private void clearPasswordType(int userId) throws RemoteException {
        setLong(KEY_HW_PIN_TYPE, -1, userId);
    }
}
