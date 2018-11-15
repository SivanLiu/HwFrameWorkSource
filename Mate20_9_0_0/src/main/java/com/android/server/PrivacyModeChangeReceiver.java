package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManager.Stub;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings.Secure;
import android.util.Slog;
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
        this.am = (ActivityManager) this.mContext.getSystemService("activity");
        if (intent.getIntExtra("privacy_mode_value", 1) == 1) {
            new Thread("privacymodechange") {
                public void run() {
                    try {
                        PrivacyModeChangeReceiver.this.removeAllRecentTask();
                        PrivacyModeChangeReceiver.this.transactToPackageManagerService(1001, "setEnabledVisitorSetting", 2, 0);
                    } catch (Exception e) {
                        Slog.e(PrivacyModeChangeReceiver.TAG, "change to visitor  mode failure  ", e);
                    }
                }
            }.start();
        } else if (intent.getIntExtra("privacy_mode_value", 0) == 0) {
            new Thread("privacymodechange") {
                public void run() {
                    try {
                        PrivacyModeChangeReceiver.this.transactToPackageManagerService(1001, "setEnabledVisitorSetting", 1, 0);
                    } catch (Exception e) {
                        Slog.e(PrivacyModeChangeReceiver.TAG, "change to host mode failure ", e);
                    }
                }
            }.start();
        }
    }

    private void updateAppfromSetting(int state) {
        String pkgNameList = Secure.getString(this.mContext.getContentResolver(), "privacy_app_list");
        if (pkgNameList == null) {
            Slog.e(TAG, " pkgNameList = null ");
        } else if (pkgNameList.contains(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)) {
            String[] pkgNameArray = pkgNameList.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            int i = 0;
            while (i < MAX_PKG && i < pkgNameArray.length) {
                setApplicationEnabledSetting(pkgNameArray[i], state);
                i++;
            }
        } else {
            setApplicationEnabledSetting(pkgNameList, state);
        }
    }

    private void removeAllRecentTask() {
        List<RecentTaskInfo> recentTasks = this.am.getRecentTasks(MAX_NUM, 2);
        if (recentTasks != null) {
            int size = recentTasks.size();
            int i = 0;
            while (i < size && i < MAX_NUM) {
                RecentTaskInfo recentInfo = (RecentTaskInfo) recentTasks.get(i);
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
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("removeTask has error. [e.toString()]: ");
                        stringBuilder.append(e.toString());
                        Slog.e(str, stringBuilder.toString());
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
        if (homeInfo != null && homeInfo.packageName.equals(component.getPackageName()) && homeInfo.name.equals(component.getClassName())) {
            return true;
        }
        return false;
    }

    private void setApplicationEnabledSetting(String pkgName, int enabledStatus) {
        String str;
        StringBuilder stringBuilder;
        try {
            if (isAppExits(pkgName)) {
                this.pm.setApplicationEnabledSetting(pkgName, enabledStatus, 0);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("the pkg ");
                stringBuilder2.append(pkgName);
                stringBuilder2.append(" enablestatus: ");
                stringBuilder2.append(enabledStatus);
                Slog.e(str2, stringBuilder2.toString());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to change enabled state of package xxx SecurityException: ");
            stringBuilder.append(pkgName);
            stringBuilder.append(e.toString());
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            e2.printStackTrace();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to change enabled state of package 1: ");
            stringBuilder.append(pkgName);
            stringBuilder.append(e2.toString());
            Slog.e(str, stringBuilder.toString());
        }
    }

    private boolean isAppExits(String pkgName) {
        boolean z = false;
        if (this.pm == null || pkgName == null || "".equals(pkgName)) {
            return false;
        }
        try {
            if (this.pm.getPackageInfo(pkgName, 0) != null) {
                z = true;
            }
            return z;
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("The packageName ");
            stringBuilder.append(pkgName);
            stringBuilder.append(" is not exit: \n");
            stringBuilder.append(e.toString());
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    private IPackageManager getPackageManager() {
        if (this.mPackageManagerService == null) {
            this.mPackageManagerService = Stub.asInterface(ServiceManager.getService("package"));
        }
        return this.mPackageManagerService;
    }

    private synchronized boolean transactToPackageManagerService(int code, String transactName, int enabledStatus, int flag) {
        boolean success;
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        boolean success2 = false;
        success = false;
        try {
            IBinder packageManagerServiceBinder = getPackageManager().asBinder();
            if (packageManagerServiceBinder != null) {
                _data.writeInterfaceToken(DESCRIPTOR);
                _data.writeInt(enabledStatus);
                _data.writeInt(flag);
                _data.writeInt(this.mContext.getUserId());
                packageManagerServiceBinder.transact(code, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 0) {
                    success2 = true;
                }
                success = success2;
            }
            _reply.recycle();
        } catch (RemoteException localRemoteException) {
            try {
                localRemoteException.printStackTrace();
                _reply.recycle();
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        }
        _data.recycle();
        return success;
    }
}
