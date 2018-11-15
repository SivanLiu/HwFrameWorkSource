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
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Slog;
import com.huawei.android.util.HwPasswordUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;

public class HwCustDevicePolicyManagerServiceImpl extends HwCustDevicePolicyManagerService {
    private static final String DB_KEY_WIPE_DATA = "wipedata_authentication";
    private static final int DEFAULT_ATT_SWITCH = 0;
    private static final int DEFAULT_MAX_ERROR_TIMES = 5;
    private static boolean FORBIDDEN_SIMPLE_PWD = SystemProperties.getBoolean("ro.config.not_allow_simple_pwd", false);
    private static final boolean IS_ATT;
    private static final boolean IS_NOT_ALLOWED_CERTI_NOTIF = SystemProperties.getBoolean("ro.config.hw_disable_certifNoti", false);
    private static final boolean IS_WIPE_STORAGE_DATA = SystemProperties.getBoolean("ro.config.hw_eas_sdformat", false);
    private static final String LOG_TAG = "HwCustDevicePolicyManagerServiceImpl";

    static {
        boolean z = SystemProperties.get("ro.config.hw_opta", "0").equals("07") && SystemProperties.get("ro.config.hw_optb", "0").equals("840");
        IS_ATT = z;
    }

    public void wipeStorageData(Context context) {
        if (IS_WIPE_STORAGE_DATA && context != null) {
            wipeExternalStorage(context);
            Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
            intent.addFlags(268435456);
            intent.putExtra("masterClearWipeDataFactory", true);
            context.sendBroadcast(intent);
        }
    }

    private void wipeExternalStorage(Context context) {
        StorageManager mStorageManager = (StorageManager) context.getSystemService("storage");
        for (VolumeInfo vol : mStorageManager.getVolumes()) {
            if (vol.getDisk() != null && vol.getDisk().isSd()) {
                mStorageManager.partitionPublic(vol.getDisk().getId());
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
        boolean z = false;
        if (!IS_ATT || context == null) {
            return false;
        }
        boolean isopen = false;
        try {
            if (Global.getInt(context.getContentResolver(), DB_KEY_WIPE_DATA, 0) == 1) {
                z = true;
            }
            isopen = z;
        } catch (Exception e) {
            isopen = false;
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("can't get erase data switch value ");
            stringBuilder.append(e.toString());
            Slog.w(str, stringBuilder.toString());
        }
        return isopen;
    }

    public void isStartEraseAllDataForAtt(final Context context, int failedAttemps) {
        if (context != null) {
            int maxTimes;
            int maxTimes2 = DEFAULT_MAX_ERROR_TIMES;
            try {
                maxTimes = System.getIntForUser(context.getContentResolver(), "password_authentication_threshold", DEFAULT_MAX_ERROR_TIMES, -2);
            } catch (Exception e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("can't max error times return default ");
                stringBuilder.append(e.toString());
                Slog.w(str, stringBuilder.toString());
                maxTimes = DEFAULT_MAX_ERROR_TIMES;
            }
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Start erase data, failedAttemps = ");
            stringBuilder2.append(failedAttemps);
            stringBuilder2.append(" maxTimes = ");
            stringBuilder2.append(maxTimes);
            Slog.i(str2, stringBuilder2.toString());
            if (failedAttemps >= maxTimes) {
                new AsyncTask<Void, Void, Void>() {
                    protected Void doInBackground(Void... params) {
                        HwCustDevicePolicyManagerServiceImpl.this.wipeExternalStorage(context);
                        return null;
                    }

                    protected void onPostExecute(Void result) {
                        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                        intent.addFlags(285212672);
                        intent.putExtra("masterClearWipeDataFactoryLowlevel", true);
                        context.sendBroadcast(intent);
                    }
                }.execute(new Void[0]);
            }
        }
    }

    public boolean shouldActiveDeviceAdmins(String policyPath) {
        if (UserHandle.getCallingUserId() == 0 && !TextUtils.isEmpty(policyPath) && SystemProperties.getBoolean("ro.config.hw_preset_da", false)) {
            return new File(policyPath).exists() ^ 1;
        }
        return false;
    }

    public void activeDeviceAdmins(String policyPath) {
        if (UserHandle.getCallingUserId() != 0) {
            Slog.e(LOG_TAG, "activeDeviceAdmins only allowed for system user.");
        } else if (!TextUtils.isEmpty(policyPath)) {
            try {
                File devicePolices = HwCfgFilePolicy.getCfgFile("xml/device_policies.xml", 0);
                if (devicePolices != null && devicePolices.canRead()) {
                    boolean ret = FileUtils.copyFile(devicePolices, new File(policyPath));
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("active preset device admins result : ");
                    stringBuilder.append(ret);
                    Slog.d(str, stringBuilder.toString());
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
            protected Void doInBackground(Void... params) {
                HwCustDevicePolicyManagerServiceImpl.this.wipeExternalStorage(context);
                return null;
            }

            protected void onPostExecute(Void result) {
                HwCustDevicePolicyManagerServiceImpl.this.wipeDataAndReset(context);
            }
        }.execute(new Void[0]);
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
        if (!IS_NOT_ALLOWED_CERTI_NOTIF || applicationLable == null || !applicationLable.equals("MobileIron")) {
            return true;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("not allowed certificate notification for application: ");
        stringBuilder.append(applicationLable);
        Slog.i(str, stringBuilder.toString());
        return false;
    }
}
