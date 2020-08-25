package com.android.server.devicepolicy;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.huawei.android.util.HwPasswordUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;

public class HwCustDevicePolicyManagerServiceImpl extends HwCustDevicePolicyManagerService {
    private static final String DB_KEY_WIPE_DATA = "wipedata_authentication";
    private static final int DEFAULT_ATT_SWITCH = 0;
    private static final int DEFAULT_MAX_ERROR_TIMES = 5;
    private static final boolean FORBIDDEN_SIMPLE_PWD = SystemProperties.getBoolean("ro.config.not_allow_simple_pwd", false);
    private static final boolean IS_ATT = ((!SystemProperties.get("ro.config.hw_opta", "0").equals("07") || !SystemProperties.get("ro.config.hw_optb", "0").equals("840")) ? DEFAULT_ATT_SWITCH : true);
    private static final boolean IS_NOT_ALLOWED_CERTI_NOTIF = SystemProperties.getBoolean("ro.config.hw_disable_certifNoti", false);
    private static final boolean IS_WIPE_STORAGE_DATA = SystemProperties.getBoolean("ro.config.hw_eas_sdformat", false);
    private static final String LOG_TAG = "HwCustDevicePolicyManagerServiceImpl";

    public void wipeStorageData(Context context) {
        if (IS_WIPE_STORAGE_DATA && context != null) {
            wipeExternalStorage(context);
            Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
            intent.addFlags(268435456);
            intent.putExtra("masterClearWipeDataFactory", true);
            context.sendBroadcast(intent);
        }
    }

    /* access modifiers changed from: private */
    public void wipeExternalStorage(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService("storage");
        for (VolumeInfo vol : storageManager.getVolumes()) {
            if (vol.getDisk() != null && vol.getDisk().isSd()) {
                storageManager.partitionPublic(vol.getDisk().getId());
                return;
            }
        }
    }

    public boolean wipeDataAndReset(Context context) {
        if (context == null) {
            return false;
        }
        if (((UserManager) context.getSystemService("user")).hasUserRestriction("no_factory_reset")) {
            Slog.w(LOG_TAG, "Remote Wiping data is not allowed for this user.");
            return false;
        }
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        intent.addFlags(285212672);
        intent.putExtra("masterClearWipeDataFactoryLowlevel", true);
        context.sendBroadcast(intent);
        return true;
    }

    public boolean isAttEraseDataOn(Context context) {
        if (!IS_ATT || context == null) {
            return false;
        }
        try {
            boolean isOpen = true;
            if (Settings.Global.getInt(context.getContentResolver(), DB_KEY_WIPE_DATA, DEFAULT_ATT_SWITCH) != 1) {
                isOpen = DEFAULT_ATT_SWITCH;
            }
            return isOpen;
        } catch (SecurityException e) {
            Slog.w(LOG_TAG, "can't get erase data switch value " + e.getMessage());
            return false;
        } catch (Exception e2) {
            Slog.w(LOG_TAG, "can't get erase data switch value " + e2.getMessage());
            return false;
        }
    }

    public void isStartEraseAllDataForAtt(final Context context, int failedAttemps) {
        int maxTimes;
        if (context != null) {
            try {
                maxTimes = Settings.System.getIntForUser(context.getContentResolver(), "password_authentication_threshold", DEFAULT_MAX_ERROR_TIMES, -2);
            } catch (SecurityException e) {
                maxTimes = DEFAULT_MAX_ERROR_TIMES;
                Slog.w(LOG_TAG, "can't max error times return default " + e.getMessage());
            } catch (Exception e2) {
                maxTimes = DEFAULT_MAX_ERROR_TIMES;
                Slog.w(LOG_TAG, "can't max error times return default " + e2.getMessage());
            }
            Slog.i(LOG_TAG, "Start erase data, failedAttemps = " + failedAttemps + " maxTimes = " + maxTimes);
            if (failedAttemps >= maxTimes) {
                new AsyncTask<Void, Void, Void>() {
                    /* class com.android.server.devicepolicy.HwCustDevicePolicyManagerServiceImpl.AnonymousClass1 */

                    /* access modifiers changed from: protected */
                    public Void doInBackground(Void... params) {
                        HwCustDevicePolicyManagerServiceImpl.this.wipeExternalStorage(context);
                        return null;
                    }

                    /* access modifiers changed from: protected */
                    public void onPostExecute(Void result) {
                        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                        intent.addFlags(285212672);
                        intent.putExtra("masterClearWipeDataFactoryLowlevel", true);
                        context.sendBroadcast(intent);
                    }
                }.execute(new Void[DEFAULT_ATT_SWITCH]);
            }
        }
    }

    public boolean shouldActiveDeviceAdmins(String policyPath) {
        if (UserHandle.getCallingUserId() == 0 && !TextUtils.isEmpty(policyPath) && SystemProperties.getBoolean("ro.config.hw_preset_da", false)) {
            return !new File(policyPath).exists();
        }
        return false;
    }

    public void activeDeviceAdmins(String policyPath) {
        if (UserHandle.getCallingUserId() != 0) {
            Slog.e(LOG_TAG, "activeDeviceAdmins only allowed for system user.");
        } else if (!TextUtils.isEmpty(policyPath)) {
            try {
                File devicePolices = HwCfgFilePolicy.getCfgFile("xml/device_policies.xml", (int) DEFAULT_ATT_SWITCH);
                if (devicePolices != null && devicePolices.canRead()) {
                    boolean ret = FileUtils.copyFile(devicePolices, new File(policyPath));
                    Slog.d(LOG_TAG, "active preset device admins result : " + ret);
                }
            } catch (NoClassDefFoundError e) {
                Slog.e(LOG_TAG, "HwCfgFilePolicy NoClassDefFoundError");
            }
        }
    }

    public boolean eraseStorageForEAS(final Context context) {
        if (!IS_WIPE_STORAGE_DATA || context == null) {
            return false;
        }
        if (((UserManager) context.getSystemService("user")).hasUserRestriction("no_factory_reset")) {
            Slog.w(LOG_TAG, "Remote Wiping data is not allowed for this user.");
            return false;
        }
        new AsyncTask<Void, Void, Void>() {
            /* class com.android.server.devicepolicy.HwCustDevicePolicyManagerServiceImpl.AnonymousClass2 */

            /* access modifiers changed from: protected */
            public Void doInBackground(Void... params) {
                HwCustDevicePolicyManagerServiceImpl.this.wipeExternalStorage(context);
                return null;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Void result) {
                HwCustDevicePolicyManagerServiceImpl.this.wipeDataAndReset(context);
            }
        }.execute(new Void[DEFAULT_ATT_SWITCH]);
        return true;
    }

    public boolean isForbiddenSimplePwdFeatureEnable() {
        return FORBIDDEN_SIMPLE_PWD;
    }

    public boolean isNewPwdSimpleCheck(String password, Context context) {
        HwPasswordUtils.loadSimplePasswordTable(context);
        if (HwPasswordUtils.isSimpleAlphaNumericPassword(password) || HwPasswordUtils.isOrdinalCharatersPassword(password) || HwPasswordUtils.isSimplePasswordInDictationary(password)) {
            return true;
        }
        return false;
    }

    public boolean isCertNotificationAllowed(String applicationLable) {
        if (!IS_NOT_ALLOWED_CERTI_NOTIF || !"MobileIron".equals(applicationLable)) {
            return true;
        }
        Slog.i(LOG_TAG, "not allowed certificate notification for application: " + applicationLable);
        return false;
    }
}
