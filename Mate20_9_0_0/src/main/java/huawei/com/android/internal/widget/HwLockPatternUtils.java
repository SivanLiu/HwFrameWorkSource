package huawei.com.android.internal.widget;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;
import android.content.ContentResolver;
import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.huawei.hsm.permission.StubController;
import java.nio.charset.StandardCharsets;

public class HwLockPatternUtils extends LockPatternUtils {
    private static final String DESCRIPTOR = "com.android.internal.widget.ILockSettings";
    private static final String PERMISSION = "com.huawei.locksettings.permission.ACCESS_HWKEYGUARD_SECURE_STORAGE";
    private static final String TAG = "HwLockPatternUtils";
    private static final int transaction_checkvisitorpassword = 1002;
    public static final int transaction_setActiveVisitorPasswordState = 1003;
    private static final int transaction_setlockvisitorpassword = 1001;
    private final ContentResolver mContentResolver;
    private final Context mContext;

    public HwLockPatternUtils(Context context) {
        super(context);
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
    }

    public void clearLockEx(boolean isFallback, boolean isHwFallback) {
        clearLockEx(isHwFallback, ActivityManager.getCurrentUser());
    }

    public void clearLockEx(boolean isHwFallback, int userHandle) {
        checkPermission();
        if (!isHwFallback) {
            setLong("lockscreen.password_type", 0, userHandle);
            setLong("lockscreen.password_type_alternate", 0, userHandle);
        }
    }

    public void saveLockPassword(String password, String savedPassword, int quality, boolean isHwFallback, int userHandle) {
        String str = password;
        int i = quality;
        int i2 = userHandle;
        checkPermission();
        try {
            DevicePolicyManager dpm = getDevicePolicyManager();
            if (str == null || password.length() < 4) {
                throw new IllegalArgumentException("password must not be null and at least of length 4");
            }
            int type;
            int qual;
            int nonletter;
            getLockSettings().setLockCredential(str, 2, savedPassword, i, i2);
            PasswordMetrics metrics = PasswordMetrics.computeForPassword(password);
            int computedQuality = metrics.quality;
            if (i2 == 0 && LockPatternUtils.isDeviceEncryptionEnabled()) {
                if (shouldEncryptWithCredentials(true)) {
                    boolean numeric = computedQuality == StubController.PERMISSION_CONTACTS_DELETE;
                    boolean numericComplex = computedQuality == 196608;
                    if (!numeric) {
                        if (!numericComplex) {
                            type = 0;
                            updateEncryptionPassword(type, str);
                        }
                    }
                    type = 2;
                    updateEncryptionPassword(type, str);
                } else {
                    clearEncryptionPassword();
                }
            }
            if (isHwFallback) {
                setLong("lockscreen.password_type", 65536, i2);
                dpm.setActivePasswordState(new PasswordMetrics(StubController.PERMISSION_SMSLOG_WRITE, 0), i2);
            } else {
                qual = i > computedQuality ? i : computedQuality;
                setLong("lockscreen.password_type", (long) qual, i2);
                if (computedQuality != 0) {
                    int qual2;
                    int symbols = 0;
                    int leng = password.length();
                    int numbers = 0;
                    nonletter = 0;
                    int lowercase = 0;
                    int uppercase = 0;
                    int letters = 0;
                    type = 0;
                    while (true) {
                        int leng2 = leng;
                        if (type >= leng2) {
                            break;
                        }
                        int leng3 = leng2;
                        qual2 = qual;
                        char c = str.charAt(type);
                        if (c >= 'A' && c <= 'Z') {
                            letters++;
                            uppercase++;
                        } else if (c >= 'a' && c <= 'z') {
                            letters++;
                            lowercase++;
                        } else if (c < '0' || c > '9') {
                            symbols++;
                            nonletter++;
                        } else {
                            numbers++;
                            nonletter++;
                        }
                        type++;
                        leng = leng3;
                        qual = qual2;
                    }
                    qual2 = qual;
                    metrics.quality = i > computedQuality ? i : computedQuality;
                    metrics.length = password.length();
                    metrics.letters = letters;
                    metrics.upperCase = uppercase;
                    metrics.lowerCase = lowercase;
                    metrics.numeric = numbers;
                    metrics.symbols = symbols;
                    metrics.nonLetter = nonletter;
                    dpm.setActivePasswordState(metrics, i2);
                } else {
                    dpm.setActivePasswordState(new PasswordMetrics(0, 0), i2);
                }
            }
            String passwordHistory = getString("lockscreen.passwordhistory", i2);
            if (passwordHistory == null) {
                passwordHistory = "";
            }
            nonletter = getRequestedPasswordHistoryLength(i2);
            if (nonletter == 0) {
                passwordHistory = "";
            } else {
                byte[] hash = passwordToHash(str, i2);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(new String(hash, StandardCharsets.UTF_8));
                stringBuilder.append(",");
                stringBuilder.append(passwordHistory);
                passwordHistory = stringBuilder.toString();
                qual = ((hash.length * nonletter) + nonletter) - 1;
                passwordHistory = passwordHistory.substring(0, qual < passwordHistory.length() ? qual : passwordHistory.length());
            }
            setString("lockscreen.passwordhistory", passwordHistory, i2);
            onAfterChangingPassword(i2);
        } catch (RemoteException re) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to save lock password ");
            stringBuilder2.append(re);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public long resetLockoutDeadline() {
        checkPermission();
        return resetLockoutDeadline(ActivityManager.getCurrentUser());
    }

    public long resetLockoutDeadline(int userHandle) {
        checkPermission();
        this.mLockoutDeadlines.put(userHandle, 0);
        return 0;
    }

    private final void checkPermission() {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "HwLockSettings Write");
    }

    public boolean setExtendLockScreenPassword(String password, String phoneNumber, int userHandle) {
        if (!SystemProperties.getBoolean("ro.config.operator_remote_lock", false)) {
            return false;
        }
        try {
            return getLockSettings().setExtendLockScreenPassword(password, phoneNumber, userHandle);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean clearExtendLockScreenPassword(String password, int userHandle) {
        if (!SystemProperties.getBoolean("ro.config.operator_remote_lock", false)) {
            return false;
        }
        try {
            return getLockSettings().clearExtendLockScreenPassword(password, userHandle);
        } catch (RemoteException e) {
            return false;
        }
    }
}
