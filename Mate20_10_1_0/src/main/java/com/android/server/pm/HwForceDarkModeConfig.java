package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareConstant;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.intellicom.common.SmartDualCardConsts;

public class HwForceDarkModeConfig {
    private static final int APP_FORCE_DARK_USER_SET_FLAG = 128;
    private static final boolean IS_DEBUG = SystemProperties.get("ro.dbg.pms_log", "0").equals("on");
    private static final long REFRESH_FORCE_DARK_MODE_DELAY_TIME = 15000;
    private static final int REFRESH_FORCE_DARK_MODE_MESSAGE = 119;
    private static final int REFRESH_FORCE_DARK_MODE_TIMES = 10;
    private static final String TAG = "HwForceDarkModeConfig";
    private static HwForceDarkModeConfig instance;
    /* access modifiers changed from: private */
    public AppTypeRecoManager mAppTypeRecoManager = AppTypeRecoManager.getInstance();
    private ContentObserver mContentObserver = new ContentObserver(this.mHandler) {
        /* class com.android.server.pm.HwForceDarkModeConfig.AnonymousClass2 */

        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            long currentTime = System.currentTimeMillis();
            long delayTime = 15000;
            if (HwForceDarkModeConfig.this.mDataUpdateTime != 0) {
                delayTime = 15000 - (currentTime - HwForceDarkModeConfig.this.mDataUpdateTime);
                if (delayTime < 0) {
                    delayTime = 0;
                }
            }
            HwForceDarkModeConfig.this.sendMessageRefreshData(delayTime);
        }
    };
    /* access modifiers changed from: private */
    public long mDataUpdateTime = 0;
    private Handler mHandler = new Handler() {
        /* class com.android.server.pm.HwForceDarkModeConfig.AnonymousClass1 */

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            long unused = HwForceDarkModeConfig.this.mDataUpdateTime = System.currentTimeMillis();
            if (HwForceDarkModeConfig.this.mAppTypeRecoManager.isReady()) {
                HwForceDarkModeConfig.this.initForceDarkModeConfig();
            } else if (HwForceDarkModeConfig.this.mRetryTime < 10) {
                HwForceDarkModeConfig hwForceDarkModeConfig = HwForceDarkModeConfig.this;
                int unused2 = hwForceDarkModeConfig.mRetryTime = hwForceDarkModeConfig.mRetryTime + 1;
                HwForceDarkModeConfig.this.mAppTypeRecoManager.init(HwForceDarkModeConfig.this.mPms.mContext);
                HwForceDarkModeConfig.this.sendMessageRefreshData(15000);
            }
        }
    };
    /* access modifiers changed from: private */
    public PackageManagerService mPms = ServiceManager.getService("package");
    /* access modifiers changed from: private */
    public ForceDarkModeReceiver mReceiver = new ForceDarkModeReceiver();
    /* access modifiers changed from: private */
    public int mRetryTime = 0;

    private HwForceDarkModeConfig() {
    }

    public void registeAppTypeRecoReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
        this.mPms.mContext.registerReceiver(this.mReceiver, filter);
        this.mPms.mContext.getContentResolver().registerContentObserver(AwareConstant.Database.APPTYPE_URI, true, this.mContentObserver);
    }

    /* access modifiers changed from: private */
    public class ForceDarkModeReceiver extends BroadcastReceiver {
        private ForceDarkModeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (HwForceDarkModeConfig.this.mReceiver != null) {
                HwForceDarkModeConfig.this.mPms.mContext.unregisterReceiver(HwForceDarkModeConfig.this.mReceiver);
            }
            HwForceDarkModeConfig.this.sendMessageRefreshData(0);
        }
    }

    /* access modifiers changed from: private */
    public void sendMessageRefreshData(long delayTime) {
        if (this.mHandler.hasMessages(119)) {
            this.mHandler.removeMessages(119);
        }
        Message message = this.mHandler.obtainMessage();
        message.what = 119;
        this.mHandler.sendMessageDelayed(message, delayTime);
    }

    /* access modifiers changed from: private */
    public void initForceDarkModeConfig() {
        AppTypeRecoManager appTypeRecoManager;
        Slog.d(TAG, "do init");
        if (this.mPms == null || (appTypeRecoManager = this.mAppTypeRecoManager) == null) {
            Slog.e(TAG, "init failed, pms or AppTypeRecoManager is null");
            return;
        }
        appTypeRecoManager.loadInstalledAppTypeInfo();
        synchronized (this.mPms.getPackagesLock()) {
            boolean isChanged = false;
            ArrayMap<String, PackageSetting> packageSettings = this.mPms.getSettings().mPackages;
            for (String packageName : packageSettings.keySet()) {
                PackageSetting pkgSetting = packageSettings.get(packageName);
                if (!(pkgSetting == null || pkgSetting.pkg == null)) {
                    if (pkgSetting.pkg.applicationInfo != null) {
                        if (!pkgSetting.isSystem()) {
                            if (!pkgSetting.isUpdatedSystem()) {
                                boolean isUserSet = (pkgSetting.getForceDarkMode() & 128) != 0;
                                int oldMode = pkgSetting.getForceDarkMode() & -129;
                                int mode = getForceDarkModeFromAppTypeRecoManager(packageName, pkgSetting);
                                if (IS_DEBUG) {
                                    Slog.d(TAG, "initForceDarkMode packageName: " + packageName + ", isUserSet: " + isUserSet + ", oldMode: " + oldMode + ", mode: " + mode);
                                }
                                if (oldMode != mode && (mode == 2 || (mode != 2 && !isUserSet))) {
                                    isChanged = true;
                                    pkgSetting.setForceDarkMode(mode);
                                    pkgSetting.pkg.applicationInfo.forceDarkMode = mode;
                                }
                            }
                        }
                    }
                }
                Slog.d(TAG, "initForceDarkMode pkgSetting is null for packageName: " + packageName);
            }
            if (isChanged) {
                this.mPms.getSettings().writeLPr();
            }
        }
        Slog.d(TAG, "end to init");
    }

    public int getForceDarkModeFromAppTypeRecoManager(String pkgName, PackageSetting pkgSetting) {
        if (!isSupportForceDark(pkgSetting)) {
            return 2;
        }
        int appAttr = this.mAppTypeRecoManager.getAppAttribute(pkgName);
        if (IS_DEBUG) {
            Slog.d(TAG, "get app attribute from AppTypeRecoManager, pkgName = " + pkgName + ", appAttr = " + appAttr);
        }
        if (appAttr != -1 && (appAttr & 536870912) == 536870912) {
            return 1;
        }
        return 2;
    }

    private boolean isSupportForceDark(PackageSetting pkgSetting) {
        if (pkgSetting == null || pkgSetting.pkg == null || pkgSetting.pkg.applicationInfo == null) {
            Slog.i(TAG, "pkgSetting is null.");
            return false;
        } else if (pkgSetting.isSystem() || pkgSetting.isUpdatedSystem()) {
            return false;
        } else {
            return true;
        }
    }

    public static synchronized HwForceDarkModeConfig getInstance() {
        HwForceDarkModeConfig hwForceDarkModeConfig;
        synchronized (HwForceDarkModeConfig.class) {
            if (instance == null) {
                instance = new HwForceDarkModeConfig();
            }
            hwForceDarkModeConfig = instance;
        }
        return hwForceDarkModeConfig;
    }
}
