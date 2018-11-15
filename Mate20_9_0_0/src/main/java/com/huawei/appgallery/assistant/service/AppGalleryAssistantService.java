package com.huawei.appgallery.assistant.service;

import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IProcessObserver.Stub;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IGameObserver;
import java.util.HashMap;
import java.util.Map;

public class AppGalleryAssistantService {
    private static final String ACTION_HMS_SERVICE = "com.huawei.hwid.NOTIFY_APP_STATE_SERVICE";
    private static final int GAME_TO_BACKGROUND = 0;
    private static final int GAME_TO_FOREGROUND = 1;
    private static final String MAP_KEY_TIME = "time";
    private static final String MAP_KEY_VALUE = "value";
    private static final int MAP_VALUE_SHOW = 1;
    private static final long ONE_DAY = 86400000;
    private static final String PACKAGE_NAME_HMS = "com.huawei.hwid";
    private static final int SCREEN_ON_BY_FOREGROUND = 2;
    private static final int SDK_TYPE_GAMESDK = 1;
    private static final int SDK_TYPE_HMSSDK = 2;
    private static final int SDK_TYPE_NOSDK = 0;
    private static final String TAG = "AssistantService";
    private static final String URI_HMS_SHOWBUOY = "content://com.huawei.hwid.gameservice.inshowbuoylist/showbuoy";
    private static AppGalleryAssistantService mAppGalleryAssistantService;
    private String launcherPackageName;
    private IActivityManager mActivityManager;
    private String mAppPackageName;
    private int mAppStatus = 0;
    private HwBoosterProcessObserver mBoosterProcessObserver;
    private final Context mContext;
    private String mGamePackageName;
    private int mGameStatus = 0;
    private Map<String, Map<String, Object>> mJointAppMap = new HashMap();
    private Map<String, Map<String, Object>> mSdkInfoMap = new HashMap();
    private Map<String, Integer> mSystemAppMap = new HashMap();

    private class HwBoosterProcessObserver extends Stub {
        private HwBoosterProcessObserver() {
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            String appPackageName = AppGalleryAssistantService.this.getPackageName(uid);
            Log.d(AppGalleryAssistantService.TAG, "onForegroundActivitiesChanged packageName = " + appPackageName + ", foregroundActivities = " + foregroundActivities + ", mAppPackageName:" + AppGalleryAssistantService.this.mAppPackageName);
            if (appPackageName.equals(AppGalleryAssistantService.this.launcherPackageName) && foregroundActivities) {
                if (!TextUtils.isEmpty(AppGalleryAssistantService.this.mAppPackageName)) {
                    AppGalleryAssistantService.this.backgroundEvent(AppGalleryAssistantService.this.mAppPackageName);
                }
            } else if (AppGalleryAssistantService.this.isSystemApp(appPackageName)) {
                Log.d(AppGalleryAssistantService.TAG, "issystemapp:" + appPackageName);
            } else if (foregroundActivities) {
                AppGalleryAssistantService.this.mAppStatus = 1;
                AppGalleryAssistantService.this.foregroundEvent(appPackageName);
            } else {
                AppGalleryAssistantService.this.mAppStatus = 0;
                AppGalleryAssistantService.this.backgroundEvent(appPackageName);
            }
        }

        public void onProcessDied(int pid, int uid) {
            String appPackageName = AppGalleryAssistantService.this.getPackageName(uid);
            Log.d(AppGalleryAssistantService.TAG, "onProcessDied appPackageName:" + appPackageName + "pid:" + pid + ", uid:" + uid);
            if (!TextUtils.isEmpty(AppGalleryAssistantService.this.mAppPackageName) && AppGalleryAssistantService.this.mAppPackageName.equals(appPackageName)) {
                AppGalleryAssistantService.this.notifyHMSBackgroundEvent(AppGalleryAssistantService.this.mAppPackageName);
            }
        }
    }

    private class HwGameObserver extends IGameObserver.Stub {
        private HwGameObserver() {
        }

        public void onGameListChanged() {
        }

        public void onGameStatusChanged(String packageName, int event) {
            int i = 1;
            Log.d(AppGalleryAssistantService.TAG, "onGameStatusChanged packageName = " + packageName + ", event = " + event);
            if (event == 1 || event == 2) {
                AppGalleryAssistantService.this.mGamePackageName = packageName;
                AppGalleryAssistantService appGalleryAssistantService = AppGalleryAssistantService.this;
                if (event != 1) {
                    i = 0;
                }
                appGalleryAssistantService.mGameStatus = i;
                AppGalleryAssistantService.this.startService();
            }
        }
    }

    private class ScreenOnReceiver extends BroadcastReceiver {
        private ScreenOnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.d(AppGalleryAssistantService.TAG, "ScreenOnReceiver");
            if (intent != null && "android.intent.action.SCREEN_ON".equals(intent.getAction()) && AppGalleryAssistantService.this.mGameStatus != 0) {
                AppGalleryAssistantService.this.mGameStatus = 2;
                AppGalleryAssistantService.this.startService();
            }
        }
    }

    public static synchronized AppGalleryAssistantService getInstance(Context context) {
        AppGalleryAssistantService appGalleryAssistantService;
        synchronized (AppGalleryAssistantService.class) {
            if (mAppGalleryAssistantService == null) {
                mAppGalleryAssistantService = new AppGalleryAssistantService(context);
            }
            appGalleryAssistantService = mAppGalleryAssistantService;
        }
        return appGalleryAssistantService;
    }

    private AppGalleryAssistantService(Context context) {
        Log.d(TAG, "Init AppGalleryAssistantService.");
        this.mContext = context;
        registerGameObserver(context);
        registerAppObserver(context);
    }

    private void registerAppObserver(Context context) {
        this.mBoosterProcessObserver = new HwBoosterProcessObserver();
        this.mActivityManager = ActivityManagerNative.getDefault();
        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
            if (res.activityInfo != null) {
                this.launcherPackageName = TextUtils.isEmpty(res.activityInfo.packageName) ? BuildConfig.FLAVOR : res.activityInfo.packageName;
            }
            Log.d(TAG, "launcherPackageName:" + this.launcherPackageName);
            this.mActivityManager.registerProcessObserver(this.mBoosterProcessObserver);
        } catch (RemoteException e) {
            Log.w(TAG, "registerProcessStatusObserver failed!");
        } catch (Throwable e2) {
            Log.w(TAG, "registerProcessStatusObserver error:" + e2.getMessage());
        }
    }

    private void registerGameObserver(Context context) {
        if (SystemProperties.getInt("ro.config.gameassist_booster", 0) == 1 || SystemProperties.getInt("ro.config.gameassist.peripherals", 0) == 1) {
            Log.d(TAG, "registerGameObserver.");
            ActivityManagerEx.registerGameObserver(new HwGameObserver());
            context.registerReceiver(new ScreenOnReceiver(), new IntentFilter("android.intent.action.SCREEN_ON"));
        }
    }

    private boolean isSystemApp(String packageName) {
        if (this.mSystemAppMap.containsKey(packageName)) {
            boolean z;
            if (((Integer) this.mSystemAppMap.get(packageName)).intValue() == 1) {
                z = true;
            } else {
                z = false;
            }
            return z;
        }
        try {
            if ((this.mContext.getPackageManager().getPackageInfo(packageName, 1).applicationInfo.flags & 1) != 0) {
                this.mSystemAppMap.put(packageName, Integer.valueOf(1));
                return true;
            }
            this.mSystemAppMap.put(packageName, Integer.valueOf(0));
            return false;
        } catch (Exception e) {
            Log.e(TAG, "isSystemApp exception:" + e.getMessage());
            return false;
        }
    }

    public void backgroundEvent(String packageName) {
        if (getAppSDKType(packageName) == 0) {
            if (appInJoint(packageName, false)) {
                notifyHMSBackgroundEvent(packageName);
            }
        } else if (getAppSDKType(packageName) == 2) {
            notifyHMSBackgroundEvent(packageName);
        }
    }

    public void foregroundEvent(String packageName) {
        long sdkType = getAppSDKType(packageName);
        if (sdkType == 0) {
            if (appInJoint(packageName, true)) {
                notifyHMSForegroundEvent(packageName);
            }
        } else if (sdkType == 2) {
            this.mAppPackageName = packageName;
        }
    }

    private void notifyHMSForegroundEvent(String packageName) {
        Log.d(TAG, "notifyHMSForegroundEvent, packageName = " + packageName);
        this.mAppPackageName = packageName;
        startHMSService(packageName, 1);
    }

    private String getPackageName(int uid) {
        String packageName = null;
        String[] packageNameList = null;
        IPackageManager pm = AppGlobals.getPackageManager();
        if (pm != null) {
            try {
                packageNameList = pm.getPackagesForUid(uid);
            } catch (RemoteException e) {
                Log.e(TAG, "getPackagesForUid exception:" + e.getMessage());
                return null;
            }
        }
        if (packageNameList != null && packageNameList.length > 0) {
            packageName = packageNameList[0];
        }
        if (packageName == null) {
            packageName = BuildConfig.FLAVOR;
        }
        return packageName;
    }

    private void notifyHMSBackgroundEvent(String packageName) {
        Log.d(TAG, "notifyHMSBackgroundEvent, packageName = " + packageName);
        startHMSService(packageName, 0);
        if (!TextUtils.isEmpty(this.mAppPackageName) && this.mAppPackageName.equals(packageName)) {
            this.mAppPackageName = null;
        }
    }

    private void startHMSService(String packageName, int appState) {
        Intent intent = new Intent(ACTION_HMS_SERVICE);
        intent.setPackage(PACKAGE_NAME_HMS);
        intent.putExtra("packageName", packageName);
        intent.putExtra("appState", appState);
        try {
            this.mContext.startService(intent);
        } catch (Throwable th) {
            Log.e(TAG, "Failure startHMSService.");
        }
    }

    private boolean appInJoint(String packageName, boolean checkTime) {
        if (!this.mJointAppMap.containsKey(packageName)) {
            return getAppTypeFromHMS(packageName);
        }
        Map<String, Object> appMap = (Map) this.mJointAppMap.get(packageName);
        int type = ((Integer) appMap.get(MAP_KEY_VALUE)).intValue();
        long time = ((Long) appMap.get(MAP_KEY_TIME)).longValue();
        if (checkTime && System.currentTimeMillis() - time > ONE_DAY) {
            return getAppTypeFromHMS(packageName);
        }
        Log.d(TAG, "appInJoint:" + type);
        return type == 1;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getAppTypeFromHMS(String packageName) {
        Log.i(TAG, "query HMS InShowBuoy:" + packageName);
        Cursor cursor = null;
        try {
            cursor = this.mContext.getContentResolver().query(ContentUris.withAppendedId(Uri.parse(URI_HMS_SHOWBUOY), 0), null, "packageName=?", new String[]{packageName}, null);
            Log.i(TAG, "cursor == null :" + (cursor == null));
            if (!(cursor == null || cursor.getCount() == 0)) {
                Log.i(TAG, "cursor.getcount : " + cursor.getCount());
                cursor.moveToFirst();
                String returnCode = cursor.getString(cursor.getColumnIndex("rtnCode"));
                Log.i(TAG, "returnCode =  " + returnCode);
                Object obj = -1;
                switch (returnCode.hashCode()) {
                    case 48:
                        if (returnCode.equals("0")) {
                            obj = null;
                        }
                    default:
                        switch (obj) {
                            case null:
                                String pkn = cursor.getString(cursor.getColumnIndex("packages"));
                                Log.d(TAG, "packageName = " + pkn);
                                Map<String, Object> info = new HashMap();
                                info.put(MAP_KEY_TIME, Long.valueOf(System.currentTimeMillis()));
                                if (packageName.equals(pkn)) {
                                    info.put(MAP_KEY_VALUE, Integer.valueOf(1));
                                    this.mJointAppMap.put(packageName, info);
                                    if (cursor == null) {
                                        return true;
                                    }
                                    cursor.close();
                                    return true;
                                }
                                info.put(MAP_KEY_VALUE, Integer.valueOf(0));
                                this.mJointAppMap.put(packageName, info);
                                if (cursor == null) {
                                    return false;
                                }
                                cursor.close();
                                return false;
                        }
                        break;
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    private long getAppSDKType(String packageName) {
        if (!this.mSdkInfoMap.containsKey(packageName)) {
            return getSDKTypeFromInstallApp(packageName);
        }
        Map<String, Object> appMap = (Map) this.mSdkInfoMap.get(packageName);
        int type = ((Integer) appMap.get(MAP_KEY_VALUE)).intValue();
        if (System.currentTimeMillis() - ((Long) appMap.get(MAP_KEY_TIME)).longValue() > ONE_DAY) {
            return getSDKTypeFromInstallApp(packageName);
        }
        Log.d(TAG, "getAppSDKType packageName:" + packageName + ", type:" + type);
        return (long) type;
    }

    private long getSDKTypeFromInstallApp(String packageName) {
        try {
            PackageManager pm = this.mContext.getPackageManager();
            Bundle metaData = pm.getApplicationInfo(packageName, 128).metaData;
            Map<String, Object> appMap;
            if (metaData == null || metaData.get("com.huawei.hms.client.appid") == null) {
                ActivityInfo[] activities = pm.getPackageInfo(packageName, 1).activities;
                if (activities != null) {
                    boolean isGameSDK = false;
                    for (ActivityInfo info : activities) {
                        if ("com.huawei.gameservice.sdk.control.DummyActivity".equals(info.name)) {
                            isGameSDK = true;
                            break;
                        }
                    }
                    if (isGameSDK) {
                        Log.d(TAG, "sdktype:GameSDK :" + packageName);
                        appMap = new HashMap();
                        appMap.put(MAP_KEY_VALUE, Integer.valueOf(1));
                        appMap.put(MAP_KEY_TIME, Long.valueOf(System.currentTimeMillis()));
                        this.mSdkInfoMap.put(packageName, appMap);
                        return 1;
                    }
                }
                Log.d(TAG, "sdktype:NoSDK :" + packageName);
                appMap = new HashMap();
                appMap.put(MAP_KEY_VALUE, Integer.valueOf(0));
                appMap.put(MAP_KEY_TIME, Long.valueOf(System.currentTimeMillis()));
                this.mSdkInfoMap.put(packageName, appMap);
                return 0;
            }
            Log.d(TAG, "sdktype:HMSSDK :" + packageName);
            appMap = new HashMap();
            appMap.put(MAP_KEY_VALUE, Integer.valueOf(2));
            appMap.put(MAP_KEY_TIME, Long.valueOf(System.currentTimeMillis()));
            this.mSdkInfoMap.put(packageName, appMap);
            return 2;
        } catch (Exception e) {
            Log.e(TAG, "getSDKTypeFromInstallApp error:" + e.getMessage());
        }
    }

    private void startService() {
        Log.d(TAG, "startService, pkgName = " + this.mGamePackageName + ", direction = " + this.mGameStatus);
        Intent intent = new Intent("com.huawei.gameassistant.NOTIFY_GAME_SWITCH");
        intent.setPackage("com.huawei.gameassistant");
        intent.putExtra("pkgName", this.mGamePackageName);
        intent.putExtra("direction", this.mGameStatus);
        try {
            this.mContext.startService(intent);
        } catch (Throwable th) {
            Log.e(TAG, "Failure starting HwGameAssistantService");
        }
    }
}
