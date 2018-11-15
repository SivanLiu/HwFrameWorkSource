package huawei.com.android.server.policy.stylus;

import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.content.Context;
import android.database.ContentObserver;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.os.RemoteException;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.Log;
import java.util.NoSuchElementException;
import vendor.huawei.hardware.tp.V1_0.ITouchscreen;

public class StylusGestureManager {
    private static final int FLAG_STYLUS = 1;
    private static final String KEY_STYLUS_ACTIVATE = "stylus_state_activate";
    private static final String KEY_STYLUS_STATE_ENABLE = "stylus_enable";
    private static final String KEY_STYLUS_STATE_INTRODUCE = "stylus_state_introduce";
    private static int STYLUS_ACTIVATE_DISABLE = 0;
    private static int STYLUS_ACTIVATE_ENABLE = 1;
    private static final int STYLUS_DISABLE = 0;
    private static final int STYLUS_ENABLE = 1;
    private static int STYLUS_INTRODUCED_NO = 0;
    private static final String STYLUS_TP_DISABLE = "0";
    private static final String STYLUS_TP_LOWFREQUENCY = "2";
    private static final String STYLUS_TP_NORMAL = "1";
    private static final String TAG = "StylusGestureManager";
    private static final int TP_HAL_DEATH_COOKIE = 1001;
    private Context mContext;
    private final Object mLock = new Object();
    private ITouchscreen mProxy = null;
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private ContentObserver mStylusActivateObserver;
    private ContentObserver mStylusIntroduceObserver;
    private int mStylusIntroduced = 0;
    private ContentObserver mStylusObserver;
    private int mStylusState = 0;

    final class ServiceNotification extends Stub {
        ServiceNotification() {
        }

        public void onRegistration(String fqName, String name, boolean preexisting) {
            String str = StylusGestureManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("tp hal service started ");
            stringBuilder.append(fqName);
            stringBuilder.append(" ");
            stringBuilder.append(name);
            Log.d(str, stringBuilder.toString());
            StylusGestureManager.this.connectToProxy();
        }
    }

    final class DeathRecipient implements android.os.IHwBinder.DeathRecipient {
        DeathRecipient() {
        }

        public void serviceDied(long cookie) {
            if (cookie == 1001) {
                String str = StylusGestureManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("tp hal service died cookie: ");
                stringBuilder.append(cookie);
                Log.d(str, stringBuilder.toString());
                synchronized (StylusGestureManager.this.mLock) {
                    StylusGestureManager.this.mProxy = null;
                }
            }
        }
    }

    public StylusGestureManager(Context context) {
        this.mContext = context;
        getTouchService();
        connectToProxy();
        initStylusStateObserver();
        initUserSwtichObserver();
        initStylusIntroducedObserver();
        initStylusActivateObserver();
    }

    private void initStylusStateObserver() {
        if (this.mContext == null) {
            Log.w(TAG, "mContext is null");
            return;
        }
        this.mStylusObserver = new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                StylusGestureManager.this.mStylusState = System.getIntForUser(StylusGestureManager.this.mContext.getContentResolver(), StylusGestureManager.KEY_STYLUS_STATE_ENABLE, 1, ActivityManager.getCurrentUser());
                String str = StylusGestureManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stylus_enable_state onChange: ");
                stringBuilder.append(StylusGestureManager.this.mStylusState);
                Log.i(str, stringBuilder.toString());
                int stylusActivated = Global.getInt(StylusGestureManager.this.mContext.getContentResolver(), StylusGestureManager.KEY_STYLUS_ACTIVATE, StylusGestureManager.STYLUS_ACTIVATE_DISABLE);
                String str2 = StylusGestureManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isStylusActivated: ");
                stringBuilder2.append(stylusActivated);
                Log.i(str2, stringBuilder2.toString());
                StylusGestureManager.this.setStylusWakeupGestureToHal(StylusGestureManager.this.mStylusState, stylusActivated);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor(KEY_STYLUS_STATE_ENABLE), false, this.mStylusObserver, -1);
        this.mStylusObserver.onChange(true);
    }

    private void initStylusIntroducedObserver() {
        if (this.mContext == null) {
            Log.w(TAG, "mContext is null");
            return;
        }
        this.mStylusIntroduceObserver = new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                StylusGestureManager.this.mStylusIntroduced = System.getIntForUser(StylusGestureManager.this.mContext.getContentResolver(), StylusGestureManager.KEY_STYLUS_STATE_INTRODUCE, StylusGestureManager.STYLUS_INTRODUCED_NO, ActivityManager.getCurrentUser());
                String str = StylusGestureManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stylus_state_introduce onChange: ");
                stringBuilder.append(StylusGestureManager.this.mStylusIntroduced);
                Log.i(str, stringBuilder.toString());
            }
        };
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor(KEY_STYLUS_STATE_INTRODUCE), false, this.mStylusIntroduceObserver, -1);
        this.mStylusIntroduceObserver.onChange(true);
    }

    private void initStylusActivateObserver() {
        if (this.mContext == null) {
            Log.w(TAG, "mContext is null");
            return;
        }
        this.mStylusActivateObserver = new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                int stylusState = System.getIntForUser(StylusGestureManager.this.mContext.getContentResolver(), StylusGestureManager.KEY_STYLUS_STATE_ENABLE, 1, ActivityManager.getCurrentUser());
                String str = StylusGestureManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("get stylus state : ");
                stringBuilder.append(stylusState);
                Log.i(str, stringBuilder.toString());
                int stylusActivated = Global.getInt(StylusGestureManager.this.mContext.getContentResolver(), StylusGestureManager.KEY_STYLUS_ACTIVATE, StylusGestureManager.STYLUS_ACTIVATE_DISABLE);
                String str2 = StylusGestureManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stylus_state_activate onChange: ");
                stringBuilder2.append(stylusActivated);
                Log.i(str2, stringBuilder2.toString());
                StylusGestureManager.this.setStylusWakeupGestureToHal(stylusState, stylusActivated);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor(KEY_STYLUS_ACTIVATE), false, this.mStylusActivateObserver);
    }

    private void initUserSwtichObserver() {
        try {
            ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                public void onUserSwitching(int newUserId) {
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    String str = StylusGestureManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onUserSwitchComplete: ");
                    stringBuilder.append(newUserId);
                    Log.i(str, stringBuilder.toString());
                    if (StylusGestureManager.this.mStylusObserver != null) {
                        StylusGestureManager.this.mStylusObserver.onChange(true);
                    }
                    if (StylusGestureManager.this.mStylusIntroduceObserver != null) {
                        StylusGestureManager.this.mStylusIntroduceObserver.onChange(true);
                    }
                }
            }, TAG);
        } catch (RemoteException e) {
            Log.e(TAG, "registerUserSwitchObserver fail", e);
        } catch (Exception e2) {
            Log.w(TAG, "registerReceiverAsUser fail ", e2);
        }
    }

    public boolean isStylusEnabled() {
        return this.mStylusState == 1;
    }

    public boolean isStylusActivate() {
        return Global.getInt(this.mContext.getContentResolver(), KEY_STYLUS_ACTIVATE, STYLUS_ACTIVATE_DISABLE) == STYLUS_ACTIVATE_ENABLE;
    }

    public boolean isStylusIntroduced() {
        boolean z = true;
        if (this.mStylusIntroduced != STYLUS_INTRODUCED_NO) {
            return true;
        }
        if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            z = false;
        }
        return z;
    }

    private void getTouchService() {
        try {
            if (!IServiceManager.getService().registerForNotifications(ITouchscreen.kInterfaceName, "", this.mServiceNotification)) {
                Log.e(TAG, "Failed to register service start notification");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register service start notification", e);
        }
    }

    private void connectToProxy() {
        synchronized (this.mLock) {
            if (this.mProxy != null) {
                Log.i(TAG, "mProxy has registered, do not register again");
                return;
            }
            try {
                this.mProxy = ITouchscreen.getService();
                if (this.mProxy != null) {
                    Log.d(TAG, "connectToProxy: mProxy get success.");
                    this.mProxy.linkToDeath(new DeathRecipient(), 1001);
                } else {
                    Log.d(TAG, "connectToProxy: mProxy get failed.");
                }
            } catch (NoSuchElementException e) {
                Log.e(TAG, "connectToProxy: tp hal service not found. Did the service fail to start?", e);
            } catch (RemoteException e2) {
                Log.e(TAG, "connectToProxy: tp hal service not responding", e2);
            }
        }
    }

    private void setStylusWakeupGestureToHal(int status, int stylusActivated) {
        synchronized (this.mLock) {
            if (this.mProxy == null) {
                Log.d(TAG, "mProxy is null, return");
                return;
            }
            String tpStatus = "2";
            if (status == 0) {
                tpStatus = "0";
            } else if (stylusActivated == STYLUS_ACTIVATE_DISABLE) {
                tpStatus = "2";
            } else {
                tpStatus = "1";
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setStylusWakeupGestureToHal: ");
            stringBuilder.append(tpStatus);
            Log.i(str, stringBuilder.toString());
            try {
                if (this.mProxy.hwSetFeatureConfig(1, tpStatus) == 0) {
                    Log.d(TAG, "setStylusWakeupGestureToHal success");
                } else {
                    Log.d(TAG, "setStylusWakeupGestureToHal error");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to set stylus mode:", e);
            }
        }
    }
}
