package com.android.server;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import java.util.List;

public class PrivacyModeChangeReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String DESCRIPTOR = "huawei.com.android.server.IPackageManager";
    private static int MAX_NUM = 20;
    private static int MAX_PKG = 100;
    private static final String TAG = "PrivacyModeChangeReceiver";
    public static final int transaction_setEnabledVisitorSetting = 1001;
    private ActivityManager am;
    private Context mContext;
    private IPackageManager mPackageManagerService;
    private PackageManager pm;

    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        this.pm = this.mContext.getPackageManager();
        this.am = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        if (intent.getIntExtra("privacy_mode_value", 1) == 1) {
            new Thread("privacymodechange") {
                /* class com.android.server.PrivacyModeChangeReceiver.AnonymousClass1 */

                public void run() {
                    try {
                        PrivacyModeChangeReceiver.this.removeAllRecentTask();
                        boolean unused = PrivacyModeChangeReceiver.this.transactToPackageManagerService(1001, "setEnabledVisitorSetting", 2, 0);
                    } catch (Exception e) {
                        Slog.e(PrivacyModeChangeReceiver.TAG, "change to visitor  mode failure  ", e);
                    }
                }
            }.start();
        } else if (intent.getIntExtra("privacy_mode_value", 0) == 0) {
            new Thread("privacymodechange") {
                /* class com.android.server.PrivacyModeChangeReceiver.AnonymousClass2 */

                public void run() {
                    try {
                        boolean unused = PrivacyModeChangeReceiver.this.transactToPackageManagerService(1001, "setEnabledVisitorSetting", 1, 0);
                    } catch (Exception e) {
                        Slog.e(PrivacyModeChangeReceiver.TAG, "change to host mode failure ", e);
                    }
                }
            }.start();
        }
    }

    private void updateAppfromSetting(int state) {
        String pkgNameList = Settings.Secure.getString(this.mContext.getContentResolver(), "privacy_app_list");
        if (pkgNameList == null) {
            Slog.e(TAG, " pkgNameList = null ");
        } else if (pkgNameList.contains(";")) {
            String[] pkgNameArray = pkgNameList.split(";");
            int i = 0;
            while (i < MAX_PKG && i < pkgNameArray.length) {
                setApplicationEnabledSetting(pkgNameArray[i], state);
                i++;
            }
        } else {
            setApplicationEnabledSetting(pkgNameList, state);
        }
    }

    /* access modifiers changed from: private */
    public void removeAllRecentTask() {
        List<ActivityManager.RecentTaskInfo> recentTasks = this.am.getRecentTasks(MAX_NUM, 2);
        if (recentTasks != null) {
            int size = recentTasks.size();
            int i = 0;
            while (i < size && i < MAX_NUM) {
                ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);
                Intent intent = new Intent(recentInfo.baseIntent);
                if (recentInfo.origActivity != null) {
                    intent.setComponent(recentInfo.origActivity);
                }
                if (isCurrentHomeActivity(intent.getComponent(), null)) {
                    Slog.e(TAG, " isCurrentHomeActivity");
                } else if (!intent.getComponent().getPackageName().equals(this.mContext.getPackageName())) {
                    try {
                        ActivityManager.getService().removeTask(recentInfo.persistentId);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "removeTask has error.");
                    }
                    recentTasks.clear();
                }
                i++;
            }
        }
    }

    private boolean isCurrentHomeActivity(ComponentName component, ActivityInfo homeInfo) {
        if (homeInfo == null) {
            homeInfo = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").resolveActivityInfo(this.mContext.getPackageManager(), 0);
        }
        if (homeInfo == null || !homeInfo.packageName.equals(component.getPackageName()) || !homeInfo.name.equals(component.getClassName())) {
            return false;
        }
        return true;
    }

    private void setApplicationEnabledSetting(String pkgName, int enabledStatus) {
        try {
            if (isAppExits(pkgName)) {
                this.pm.setApplicationEnabledSetting(pkgName, enabledStatus, 0);
                Slog.e(TAG, "the pkg " + pkgName + " enablestatus: " + enabledStatus);
            }
        } catch (SecurityException e) {
            Slog.e(TAG, "Unable to change enabled state of package xxx SecurityException: " + pkgName);
        } catch (Exception e2) {
            Slog.e(TAG, "Unable to change enabled state of package 1: " + pkgName);
        }
    }

    private boolean isAppExits(String pkgName) {
        if (this.pm == null || pkgName == null || "".equals(pkgName)) {
            return false;
        }
        try {
            if (this.pm.getPackageInfo(pkgName, 0) != null) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "The packageName " + pkgName + " is not exit");
            return false;
        }
    }

    private IPackageManager getPackageManager() {
        if (this.mPackageManagerService == null) {
            this.mPackageManagerService = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }
        return this.mPackageManagerService;
    }

    /* access modifiers changed from: private */
    public synchronized boolean transactToPackageManagerService(int code, String transactName, int enabledStatus, int flag) {
        boolean success;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        success = false;
        try {
            IBinder packageManagerServiceBinder = getPackageManager().asBinder();
            if (packageManagerServiceBinder != null) {
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(enabledStatus);
                    _data.writeInt(flag);
                    _data.writeInt(this.mContext.getUserId());
                    boolean z = false;
                    packageManagerServiceBinder.transact(code, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        z = true;
                    }
                    success = z;
                } catch (RemoteException e) {
                    localRemoteException = e;
                    try {
                        Slog.e(TAG, "transactToPackageManagerService RemoteException : " + localRemoteException.getMessage());
                        _reply.recycle();
                        _data.recycle();
                        return success;
                    } catch (Throwable th) {
                        th = th;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }
            _reply.recycle();
            _data.recycle();
        } catch (RemoteException e2) {
            localRemoteException = e2;
            Slog.e(TAG, "transactToPackageManagerService RemoteException : " + localRemoteException.getMessage());
            _reply.recycle();
            _data.recycle();
            return success;
        }
        return success;
    }
}
