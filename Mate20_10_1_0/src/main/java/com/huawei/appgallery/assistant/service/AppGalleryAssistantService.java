package com.huawei.appgallery.assistant.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IGameObserver;
import com.huawei.android.app.IGameObserverEx;
import com.huawei.android.app.IHwActivityNotifierEx;
import com.huawei.gameassistant.booster.INotifyGameSwitch;
import com.huawei.hms.jos.dock.INotifyAppStateService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AppGalleryAssistantService {
    private static final String ACTION_HMS_SERVICE = "com.huawei.hwid.NOTIFY_APP_STATE_SERVICE";
    public static final int GAME_TO_BACKGROUND = 0;
    public static final int GAME_TO_FOREGROUND = 1;
    public static final int GAME_TO_GAME = 3;
    private static final long HALF_DAY = 43200000;
    private static final long ONE_DAY = 86400000;
    private static final String PACKAGE_NAME_APPASSISTANT = "com.huawei.gameassistant";
    private static final String PACKAGE_NAME_HMS = "com.huawei.hwid";
    public static final int SCREEN_ON_BY_FOREGROUND = 2;
    public static final String TAG = "AssistantService-1030200";
    private static final String URI_ASSISTANT_SYNC = "content://com.huawei.gameassistant.provider.syncApp/sync";
    private static AppGalleryAssistantService mAppGalleryAssistantService;
    /* access modifiers changed from: private */
    public int appAssistantRetryTime = 0;
    private ServiceConnection appAssistantServerConnection = new ServiceConnection() {
        /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass5 */

        public void onServiceDisconnected(ComponentName name) {
            Log.d(AppGalleryAssistantService.TAG, "appAssistantServerConnection onServiceDisconnected");
            INotifyGameSwitch unused = AppGalleryAssistantService.this.notifyGameSwitchService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(AppGalleryAssistantService.TAG, "appAssistantServerConnection onServiceConnected");
            INotifyGameSwitch unused = AppGalleryAssistantService.this.notifyGameSwitchService = INotifyGameSwitch.Stub.asInterface(service);
            if (AppGalleryAssistantService.this.notifyGameSwitchService == null) {
                Log.e(AppGalleryAssistantService.TAG, "appAssistantServerConnection onServiceConnected fail, notifyGameSwitchService is null!");
            } else if (!TextUtils.isEmpty(AppGalleryAssistantService.this.mGamePackageName)) {
                Log.d(AppGalleryAssistantService.TAG, "onServiceConnected gameSwitch, package:" + AppGalleryAssistantService.this.mGamePackageName + ", state:" + AppGalleryAssistantService.this.mGameStatus);
                AppGalleryAssistantService.this.singleThreadScheduledPool.execute(new Runnable() {
                    /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass5.AnonymousClass1 */

                    public void run() {
                        AppGalleryAssistantService.this.notifyAppAssistantService();
                    }
                });
            }
        }
    };
    /* access modifiers changed from: private */
    public String fromAppPackage;
    Handler handler = null;
    private ServiceConnection hmsServerConnection = new ServiceConnection() {
        /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass4 */

        public void onServiceDisconnected(ComponentName name) {
            Log.d(AppGalleryAssistantService.TAG, "hmsServerConnection onServiceDisconnected");
            INotifyAppStateService unused = AppGalleryAssistantService.this.notifyAppStateService = null;
            String unused2 = AppGalleryAssistantService.this.tmpAppPackageName = null;
            int unused3 = AppGalleryAssistantService.this.tmpAppState = -1;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(AppGalleryAssistantService.TAG, "hmsServerConnection onServiceConnected");
            INotifyAppStateService unused = AppGalleryAssistantService.this.notifyAppStateService = INotifyAppStateService.Stub.asInterface(service);
            if (AppGalleryAssistantService.this.notifyAppStateService == null) {
                Log.e(AppGalleryAssistantService.TAG, "hmsServerConnection onServiceConnected fail, notifyAppStateService is null!");
                String unused2 = AppGalleryAssistantService.this.tmpAppPackageName = null;
                int unused3 = AppGalleryAssistantService.this.tmpAppState = -1;
            } else if (AppGalleryAssistantService.this.tmpAppPackageName != null && AppGalleryAssistantService.this.tmpAppState >= 0) {
                Log.d(AppGalleryAssistantService.TAG, "onServiceConnected notify tmp state, package:" + AppGalleryAssistantService.this.tmpAppPackageName + ", state:" + AppGalleryAssistantService.this.tmpAppState);
                AppGalleryAssistantService.this.singleThreadScheduledPool.execute(new Runnable() {
                    /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass4.AnonymousClass1 */

                    public void run() {
                        AppGalleryAssistantService.this.notifyHMSService(AppGalleryAssistantService.this.tmpAppPackageName, AppGalleryAssistantService.this.tmpAppState);
                        String unused = AppGalleryAssistantService.this.tmpAppPackageName = null;
                        int unused2 = AppGalleryAssistantService.this.tmpAppState = -1;
                    }
                });
            }
        }
    };
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public String mGamePackageName;
    /* access modifiers changed from: private */
    public int mGameStatus = 0;
    private Map<String, Integer> mSystemAppMap = new HashMap();
    /* access modifiers changed from: private */
    public INotifyAppStateService notifyAppStateService = null;
    private long notifyAssistantTime = 0;
    /* access modifiers changed from: private */
    public INotifyGameSwitch notifyGameSwitchService = null;
    /* access modifiers changed from: private */
    public ScheduledExecutorService singleThreadScheduledPool = Executors.newSingleThreadScheduledExecutor();
    /* access modifiers changed from: private */
    public String tmpAppPackageName;
    /* access modifiers changed from: private */
    public int tmpAppState = -1;
    /* access modifiers changed from: private */
    public String toAppPackage;

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
        Log.i(TAG, "Init AppGalleryAssistantService.");
        this.mContext = context;
        registerGameObserver();
        registerAppObserver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_PRESENT");
        context.registerReceiver(new ScreenOnReceiver(), filter);
    }

    public int getmGameStatus() {
        return this.mGameStatus;
    }

    public String getmGamePackageName() {
        return this.mGamePackageName;
    }

    private void registerAppObserver() {
        try {
            ActivityManagerEx.registerHwActivityNotifier(new IHwActivityNotifierEx() {
                /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass1 */

                public void call(Bundle extras) {
                    if (extras != null) {
                        String unused = AppGalleryAssistantService.this.fromAppPackage = extras.getString("fromPackage");
                        String unused2 = AppGalleryAssistantService.this.toAppPackage = extras.getString("toPackage");
                        Log.i(AppGalleryAssistantService.TAG, "registerHwActivityNotifier call fromPackage:" + AppGalleryAssistantService.this.fromAppPackage + ", toPackage:" + AppGalleryAssistantService.this.toAppPackage);
                        AppGalleryAssistantService.this.singleThreadScheduledPool.execute(new Runnable() {
                            /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass1.AnonymousClass1 */

                            public void run() {
                                Log.d(AppGalleryAssistantService.TAG, "handleMessage app switch fromPackage:" + AppGalleryAssistantService.this.fromAppPackage + ", toPackage:" + AppGalleryAssistantService.this.toAppPackage);
                                if (!AppGalleryAssistantService.this.isSystemApp(AppGalleryAssistantService.this.toAppPackage)) {
                                    AppGalleryAssistantService.this.notifyAssistant();
                                }
                            }
                        });
                    }
                }
            }, "appSwitch");
        } catch (Throwable e) {
            Log.w(TAG, "registerProcessStatusObserver error:" + e.getMessage());
        }
    }

    /* access modifiers changed from: private */
    public void notifyAssistant() {
        if (System.currentTimeMillis() - this.notifyAssistantTime > HALF_DAY) {
            Log.d(TAG, "start notifyAssistant.");
            this.notifyAssistantTime = System.currentTimeMillis();
            Cursor cursor = null;
            try {
                Cursor cursor2 = this.mContext.getContentResolver().query(ContentUris.withAppendedId(Uri.parse(URI_ASSISTANT_SYNC), 0), null, null, null, null);
                Log.i(TAG, "notifyAssistant cursor == null :" + (cursor2 == null));
                if (!(cursor2 == null || cursor2.getCount() == 0)) {
                    Log.i(TAG, "notifyAssistant cursor.getcount : " + cursor2.getCount());
                    cursor2.moveToFirst();
                    Log.i(TAG, "notifyAssistant returnCode =  " + cursor2.getString(cursor2.getColumnIndex("rtnCode")));
                }
                if (cursor2 != null) {
                    cursor2.close();
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    cursor.close();
                }
                throw th;
            }
        }
    }

    private void registerGameObserver() {
        Log.d(TAG, "registerGameObserver.");
        this.handler = new Handler();
        boolean isNewGameGameObserver = false;
        try {
            ActivityManagerEx.registerGameObserverEx(new HwGameObserverEx());
            isNewGameGameObserver = true;
        } catch (NoClassDefFoundError e) {
            Log.w(TAG, "registerGameObserverEx NoClassDefFoundError:" + e.getMessage());
        } catch (Exception e2) {
            Log.e(TAG, "registerGameObserverEx Exception:", e2);
        } catch (Throwable e3) {
            Log.e(TAG, "registerGameObserverEx Throwable:", e3);
        }
        if (!isNewGameGameObserver) {
            ActivityManagerEx.registerGameObserver(new HwGameObserver());
        }
    }

    /* access modifiers changed from: private */
    public boolean isAppAssistantAttention() {
        boolean isSupportBooster;
        boolean isSupportPeripherals;
        boolean isSupportLR;
        if (SystemProperties.getInt("ro.config.gameassist_booster", 0) == 1) {
            isSupportBooster = true;
        } else {
            isSupportBooster = false;
        }
        if (SystemProperties.getInt("ro.config.gameassist.peripherals", 0) == 1) {
            isSupportPeripherals = true;
        } else {
            isSupportPeripherals = false;
        }
        if (!TextUtils.isEmpty(SystemProperties.get("ro.config.hw_curved_side_disp", (String) null))) {
            isSupportLR = true;
        } else {
            isSupportLR = false;
        }
        return isSupportBooster || isSupportPeripherals || isSupportLR;
    }

    private boolean isSupportBuoy() {
        return SystemProperties.getInt("ro.config.gameassist.full-finger", 0) == 1 && Settings.Secure.getInt(this.mContext.getContentResolver(), "game_buoy", 1) == 1 && Settings.Secure.getInt(this.mContext.getContentResolver(), "game_buoy_startup", 1) == 1;
    }

    private class HwGameObserver extends IGameObserver.Stub {
        private HwGameObserver() {
        }

        public void onGameListChanged() {
        }

        public void onGameStatusChanged(String packageName, int event) {
            AppGalleryAssistantService.this.onGameStatusChange("IGameObserver", packageName, event);
        }
    }

    private class HwGameObserverEx extends IGameObserverEx.Stub {
        private HwGameObserverEx() {
        }

        public void onGameListChanged() {
        }

        public void onGameStatusChanged(int event, Bundle bundle) {
            if (bundle != null) {
                try {
                    if (bundle.containsKey("packageName")) {
                        AppGalleryAssistantService.this.onGameStatusChange("IGameObserverEx", bundle.getString("packageName"), event);
                    }
                } catch (Exception e) {
                    Log.e(AppGalleryAssistantService.TAG, "HwGameObserverEx onGameStatusChanged e:", e);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void onGameStatusChange(String methodName, String packageName, int event) {
        Log.i(TAG, methodName + " onGameStatusChanged packageName = " + packageName + ", event = " + event);
        if (event == 1 || event == 2 || event == 3) {
            this.handler.removeCallbacksAndMessages(null);
            this.mGamePackageName = packageName;
            switch (event) {
                case 1:
                    this.mGameStatus = 1;
                    break;
                case 2:
                default:
                    this.mGameStatus = 0;
                    break;
                case GAME_TO_GAME /*{ENCODED_INT: 3}*/:
                    this.mGameStatus = 3;
                    break;
            }
            this.singleThreadScheduledPool.execute(new Runnable() {
                /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass2 */

                public void run() {
                    Log.d(AppGalleryAssistantService.TAG, "handleMessage game switch mGamePackageName:" + AppGalleryAssistantService.this.mGamePackageName + ", mGameStatus:" + AppGalleryAssistantService.this.mGameStatus);
                    AppGalleryAssistantService.this.startDealHMSEvent(AppGalleryAssistantService.this.mGamePackageName, AppGalleryAssistantService.this.mGameStatus);
                    if (AppGalleryAssistantService.this.isAppAssistantAttention()) {
                        AppGalleryAssistantService.this.startAppAssistantService();
                    }
                    AssistantCallDndHelper.notifyGameBackground(AppGalleryAssistantService.this.mContext, AppGalleryAssistantService.this.mGameStatus);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void startDealHMSEvent(String gamePackageName, int gameStatus) {
        switch (gameStatus) {
            case 0:
                notifyHMSBackgroundEvent(gamePackageName);
                return;
            case 1:
            case 2:
            case GAME_TO_GAME /*{ENCODED_INT: 3}*/:
                notifyHMSForegroundEvent(gamePackageName);
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: private */
    public boolean isSystemApp(String packageName) {
        boolean z;
        if (this.mSystemAppMap.containsKey(packageName)) {
            if (this.mSystemAppMap.get(packageName).intValue() == 1) {
                z = true;
            } else {
                z = false;
            }
            return z;
        }
        try {
            if ((this.mContext.getPackageManager().getPackageInfo(packageName, 1).applicationInfo.flags & 1) != 0) {
                this.mSystemAppMap.put(packageName, 1);
                return true;
            }
            this.mSystemAppMap.put(packageName, 0);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "isSystemApp exception:" + e.getMessage());
            return false;
        }
    }

    private void notifyHMSForegroundEvent(String packageName) {
        if (isSupportBuoy()) {
            Log.i(TAG, "notifyHMSForegroundEvent, packageName = " + packageName);
            startHMSService(packageName, 1);
            return;
        }
        Log.i(TAG, "not supportBuoy");
    }

    private void notifyHMSBackgroundEvent(String packageName) {
        Log.i(TAG, "notifyHMSBackgroundEvent, packageName = " + packageName);
        startHMSService(packageName, 0);
    }

    private void startHMSService(String packageName, int appState) {
        if (!supportHMSAIDL()) {
            Log.i(TAG, "start HMSService");
            Intent intent = new Intent(ACTION_HMS_SERVICE);
            intent.setPackage(PACKAGE_NAME_HMS);
            intent.putExtra("packageName", packageName);
            intent.putExtra("appState", appState);
            try {
                this.mContext.startService(intent);
            } catch (Throwable e) {
                Log.e(TAG, "Failure startHMSService.", e);
            }
        } else if (this.notifyAppStateService == null) {
            this.tmpAppPackageName = packageName;
            this.tmpAppState = appState;
            bindHMSService();
        } else {
            notifyHMSService(packageName, appState);
        }
    }

    private boolean supportHMSAIDL() {
        try {
            Uri hmsBuoyUri = Uri.parse("content://com.huawei.hwid.peripheralprovider");
            Bundle extra = new Bundle();
            extra.putString("function", "FWKAIDL");
            Bundle bundle = this.mContext.getContentResolver().call(hmsBuoyUri, "hasFunction", (String) null, extra);
            if (bundle != null && bundle.getBoolean("function", false)) {
                Log.d(TAG, "HMS return support FWKAIDL");
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "notify HMS hasFunction exception.", e);
        }
        Log.d(TAG, "HMS not support FWKAIDL");
        return false;
    }

    private class ScreenOnReceiver extends BroadcastReceiver {
        private ScreenOnReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && "android.intent.action.USER_PRESENT".equals(intent.getAction())) {
                Log.d(AppGalleryAssistantService.TAG, "ScreenOnReceiver on");
                int unused = AppGalleryAssistantService.this.appAssistantRetryTime = 0;
                if (AppGalleryAssistantService.this.mGameStatus != 0) {
                    int unused2 = AppGalleryAssistantService.this.mGameStatus = 2;
                    AppGalleryAssistantService.this.startAppAssistantService();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void startAppAssistantService() {
        int targetSDKVersion = getAppAssistantSDKVersion();
        Log.d(TAG, "AppAssistant targetSDKVersion:" + targetSDKVersion);
        if (targetSDKVersion >= 26) {
            if (this.notifyGameSwitchService == null) {
                Log.d(TAG, "start bind appAssistant service");
                Intent serviceIntent = new Intent("com.huawei.gameassistant.NOTIFY_GAME_SWITCH");
                serviceIntent.setPackage(PACKAGE_NAME_APPASSISTANT);
                boolean result = this.mContext.getApplicationContext().bindService(serviceIntent, this.appAssistantServerConnection, 1);
                Log.i(TAG, "bind appAssistant Service result:" + result);
                if (result) {
                    this.handler.removeCallbacksAndMessages(null);
                    this.appAssistantRetryTime = 0;
                } else if (this.appAssistantRetryTime >= 0 && this.appAssistantRetryTime < 3) {
                    this.appAssistantRetryTime++;
                    Log.i(TAG, "retry bind appAssistant time:" + this.appAssistantRetryTime);
                    this.handler.postDelayed(new Runnable() {
                        /* class com.huawei.appgallery.assistant.service.AppGalleryAssistantService.AnonymousClass3 */

                        public void run() {
                            AppGalleryAssistantService.this.startAppAssistantService();
                        }
                    }, 100);
                }
            } else {
                notifyAppAssistantService();
            }
        } else if (targetSDKVersion > 0) {
            Log.i(TAG, "startService, pkgName = " + this.mGamePackageName + ", direction = " + this.mGameStatus);
            Intent intent = new Intent("com.huawei.gameassistant.NOTIFY_GAME_SWITCH");
            intent.setPackage(PACKAGE_NAME_APPASSISTANT);
            intent.putExtra("pkgName", this.mGamePackageName);
            intent.putExtra("direction", this.mGameStatus);
            try {
                this.mContext.startService(intent);
            } catch (Throwable e) {
                Log.e(TAG, "Failure starting HwGameAssistantService", e);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyAppAssistantService() {
        Log.d(TAG, "start gameSwitch, pkgName = " + this.mGamePackageName + ", direction = " + this.mGameStatus);
        try {
            this.notifyGameSwitchService.gameSwitch(this.mGamePackageName, this.mGameStatus);
        } catch (RemoteException e) {
            Log.e(TAG, "Failure starting appAssistant Service", e);
        } catch (Throwable e2) {
            Log.e(TAG, "Failure starting appAssistant Service Throwable", e2);
        }
        try {
            this.mContext.getApplicationContext().unbindService(this.appAssistantServerConnection);
            this.notifyGameSwitchService = null;
        } catch (Throwable e3) {
            Log.e(TAG, "Failure unbindService appAssistant Service Throwable", e3);
        }
    }

    private int getAppAssistantSDKVersion() {
        try {
            return this.mContext.getPackageManager().getApplicationInfo(PACKAGE_NAME_APPASSISTANT, 128).targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failure getAppAssistantSDKVersion", e);
            return -1;
        }
    }

    private void bindHMSService() {
        Intent serviceIntent = new Intent(ACTION_HMS_SERVICE);
        serviceIntent.setPackage(PACKAGE_NAME_HMS);
        boolean result = this.mContext.getApplicationContext().bindService(serviceIntent, this.hmsServerConnection, 1);
        Log.i(TAG, "bindHMSService result:" + result);
        if (!result) {
            this.tmpAppPackageName = null;
            this.tmpAppState = -1;
        }
    }

    /* access modifiers changed from: private */
    public void notifyHMSService(String packageName, int appState) {
        Log.i(TAG, "notify HMSService packageName:" + packageName + ", appState:" + appState);
        try {
            this.notifyAppStateService.notify(packageName, appState);
        } catch (RemoteException e) {
            Log.e(TAG, "Failure starting HMSService", e);
        } catch (Throwable e2) {
            Log.e(TAG, "Failure starting HMSService Throwable", e2);
        }
        try {
            this.mContext.getApplicationContext().unbindService(this.hmsServerConnection);
            this.notifyAppStateService = null;
        } catch (Throwable e3) {
            Log.e(TAG, "Failure unbindService HMSService Throwable", e3);
        }
    }
}
