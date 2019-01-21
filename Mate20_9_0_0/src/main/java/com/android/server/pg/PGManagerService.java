package com.android.server.pg;

import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.WorkSource;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.LocationManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.power.PowerManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.huawei.pgmng.api.IPGManager.Stub;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class PGManagerService extends Stub {
    public static final int BLACK_LIST_TYPE_WIFI = 6;
    public static final int CODE_CLOSE_SOCKETS_FOR_UID = 1107;
    private static final int CODE_RESTRICT_WIFI_SCAN = 4001;
    public static final int CODE_SET_FIREWALL_RULE_FOR_PID = 1110;
    private static final int CONFIG_TYPE_GOOGLE_EBS = 3;
    private static final int CONFIG_TYPE_PROXY_SERVICE = 0;
    private static final int CONFIG_TYPE_PROXY_WAKELOCK = 2;
    private static final int CONFIG_TYPE_WIFI_RESTRICT = 1;
    private static final String DESCRIPTOR_IWIFIMANAGER = "android.net.wifi.IWifiManager";
    public static final String DESCRIPTOR_NETWORKMANAGEMENT_SERVICE = "android.os.INetworkManagementService";
    public static final int DISABLE_LIST_TYPE_QUICKTTFF = 4;
    public static final int SUBTYPE_DROP_WAKELOCK = 1;
    public static final int SUBTYPE_PROXY = 0;
    public static final int SUBTYPE_PROXY_NO_WORKSOURCE = 2;
    public static final int SUBTYPE_RELEASE_NO_WORKSOURCE = 3;
    public static final int SUBTYPE_UNPROXY = 1;
    public static final int SUBTYPE_UNPROXY_ALL = 2;
    private static final String TAG = "PGManagerService";
    public static final int WHITE_LIST_TYPE_GPS = 1;
    public static final int WHITE_LIST_TYPE_QUICKTTFF = 3;
    public static final int WHITE_LIST_TYPE_WIFI = 2;
    public static final int WHITE_LIST_TYPE_WIFI_SLEEP = 7;
    private static final Object mLock = new Object();
    private static HashSet<String> mProxyServiceList = new HashSet();
    private static PGManagerService sInstance = null;
    private ActivityManagerService mAM;
    private final Context mContext;
    private PGGoogleServicePolicy mGoogleServicePolicy = null;
    private LocationManagerService mLMS;
    private PowerManagerService mPms;
    private ProcBatteryStats mProcStats = null;
    private boolean mSystemReady;

    class LocalService extends PGManagerInternal {
        LocalService() {
        }

        public void noteStartWakeLock(String tag, WorkSource ws, String pkgName, int uid) {
            PGManagerService.this.mProcStats.processWakeLock(160, tag, ws, pkgName, uid);
        }

        public void noteStopWakeLock(String tag, WorkSource ws, String pkgName, int uid) {
            PGManagerService.this.mProcStats.processWakeLock(161, tag, ws, pkgName, uid);
        }

        public void noteChangeWakeLock(String tag, WorkSource ws, String pkgName, int uid, String newTag, WorkSource newWs, String newPkgName, int newUid) {
            PGManagerService.this.mProcStats.processWakeLock(161, tag, ws, pkgName, uid);
            PGManagerService.this.mProcStats.processWakeLock(160, newTag, newWs, newPkgName, newUid);
        }

        public boolean isServiceProxy(ComponentName name, String sourcePkg) {
            synchronized (PGManagerService.mProxyServiceList) {
                if (PGManagerService.mProxyServiceList.size() == 0) {
                    return false;
                }
                boolean isServiceMatchList = isServiceMatchList(name, sourcePkg, PGManagerService.mProxyServiceList);
                return isServiceMatchList;
            }
        }

        public boolean isServiceMatchList(ComponentName name, String sourcePkg, Collection<String> list) {
            if (!(name == null || list == null)) {
                String pkg = name.getPackageName();
                String pkgAndCls = new StringBuilder();
                pkgAndCls.append(pkg);
                pkgAndCls.append(SliceAuthority.DELIMITER);
                pkgAndCls.append(name.getClassName());
                pkgAndCls = pkgAndCls.toString();
                String pkgAndShortCls = new StringBuilder();
                pkgAndShortCls.append(pkg);
                pkgAndShortCls.append(SliceAuthority.DELIMITER);
                pkgAndShortCls.append(name.getShortClassName());
                pkgAndShortCls = pkgAndShortCls.toString();
                String anyPkgAndCls = new StringBuilder();
                anyPkgAndCls.append("*/");
                anyPkgAndCls.append(name.getClassName());
                anyPkgAndCls = anyPkgAndCls.toString();
                if (list.contains(pkg) || ((sourcePkg != null && list.contains(sourcePkg)) || list.contains(pkgAndCls) || list.contains(anyPkgAndCls) || (!pkgAndShortCls.equals(pkgAndCls) && list.contains(pkgAndShortCls)))) {
                    return true;
                }
            }
            return false;
        }

        public boolean isGmsWakeLockFilterTag(int flags, String packageName, WorkSource ws) {
            if (PGManagerService.this.mGoogleServicePolicy != null) {
                return PGManagerService.this.mGoogleServicePolicy.isGmsWakeLockFilterTag(flags, packageName, ws);
            }
            return false;
        }
    }

    public PGManagerService(Context context) {
        this.mContext = context;
        this.mProcStats = new ProcBatteryStats(this.mContext);
        LocalServices.addService(PGManagerInternal.class, new LocalService());
        this.mGoogleServicePolicy = new PGGoogleServicePolicy(this.mContext);
    }

    public static PGManagerService getInstance(Context context) {
        PGManagerService pGManagerService;
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new PGManagerService(context);
                ServiceManager.addService("pgservice", sInstance);
            }
            pGManagerService = sInstance;
        }
        return pGManagerService;
    }

    public void systemReady(ActivityManagerService activityManagerService, PowerManagerService powerManagerService, LocationManagerService location) {
        synchronized (mLock) {
            Log.i(TAG, "PGManagerService--systemReady--begain");
            this.mAM = activityManagerService;
            this.mPms = powerManagerService;
            this.mLMS = location;
            this.mSystemReady = true;
            this.mProcStats.onSystemReady();
            this.mGoogleServicePolicy.onSystemReady();
            Log.i(TAG, "PGManagerService--systemReady--end");
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (this.mProcStats.onTransact(code, data, reply, flags)) {
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    private boolean restrictWifiScan(List<String> pkgs, boolean restrict) {
        if (pkgs != null && pkgs.size() == 0) {
            Log.w(TAG, "pkgs is empty, nothing to do.");
            return false;
        } else if (restrict && pkgs == null) {
            Log.w(TAG, "illegal parameters.");
            return false;
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start restrictWifiScan:");
            stringBuilder.append(pkgs);
            stringBuilder.append(", restrict:");
            stringBuilder.append(restrict);
            Log.i(str, stringBuilder.toString());
            IBinder b = ServiceManager.getService("wifi");
            Parcel _data = Parcel.obtain();
            Parcel _reply = Parcel.obtain();
            if (b != null) {
                try {
                    _data.writeInterfaceToken(DESCRIPTOR_IWIFIMANAGER);
                    _data.writeStringList(pkgs);
                    _data.writeInt(restrict);
                    b.transact(CODE_RESTRICT_WIFI_SCAN, _data, _reply, 0);
                    _reply.readException();
                } catch (Exception e) {
                    Log.e(TAG, "restrict wifi scan error", e);
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            _reply.recycle();
            _data.recycle();
            return true;
        }
    }

    public long proxyBroadcast(List<String> pkgs, boolean proxy) {
        String str;
        StringBuilder stringBuilder;
        if (!this.mSystemReady) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("not ready for proxyBroadcast:");
            stringBuilder.append(pkgs);
            stringBuilder.append(" proxy:");
            stringBuilder.append(proxy);
            Log.w(str, stringBuilder.toString());
            return -1;
        } else if (1000 != Binder.getCallingUid()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("proxy broadcast permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return -1;
        } else {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("proxyBroadcast:");
            stringBuilder2.append(pkgs);
            stringBuilder2.append(" proxy:");
            stringBuilder2.append(proxy);
            Log.i(str, stringBuilder2.toString());
            return this.mAM.proxyBroadcast(pkgs, proxy);
        }
    }

    public long proxyBroadcastByPid(List<String> pids, boolean proxy) {
        String str;
        StringBuilder stringBuilder;
        if (!this.mSystemReady) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("not ready for proxyBroadcastByPid:");
            stringBuilder.append(pids);
            stringBuilder.append(" proxy:");
            stringBuilder.append(proxy);
            Log.w(str, stringBuilder.toString());
            return -1;
        } else if (1000 != Binder.getCallingUid()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("proxy broadcast permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return -1;
        } else {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("proxyBroadcastByPid:");
            stringBuilder2.append(pids);
            stringBuilder2.append(" proxy:");
            stringBuilder2.append(proxy);
            Log.i(str, stringBuilder2.toString());
            List<Integer> ipids = new ArrayList();
            if (pids != null) {
                for (String pid : pids) {
                    ipids.add(Integer.valueOf(Integer.parseInt(pid)));
                }
            } else {
                ipids = null;
            }
            return this.mAM.proxyBroadcastByPid(ipids, proxy);
        }
    }

    public void setProxyBCActions(List<String> actions) {
        String str;
        StringBuilder stringBuilder;
        if (!this.mSystemReady) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("not ready for setProxyBCActions:");
            stringBuilder.append(actions);
            Log.w(str, stringBuilder.toString());
        } else if (1000 != Binder.getCallingUid()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setProxyBCActions permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("proxy BC Actions:");
            stringBuilder.append(actions);
            Log.i(str, stringBuilder.toString());
            this.mAM.setProxyBCActions(actions);
        }
    }

    public void setActionExcludePkg(String action, String pkg) {
        String str;
        StringBuilder stringBuilder;
        if (!this.mSystemReady) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("not ready for setActionExcludePkg action:");
            stringBuilder.append(action);
            stringBuilder.append(" pkg:");
            stringBuilder.append(pkg);
            Log.w(str, stringBuilder.toString());
        } else if (1000 != Binder.getCallingUid()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setActionExcludePkg permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("set action:");
            stringBuilder.append(action);
            stringBuilder.append(" pkg:");
            stringBuilder.append(pkg);
            Log.i(str, stringBuilder.toString());
            this.mAM.setActionExcludePkg(action, pkg);
        }
    }

    public void proxyBCConfig(int type, String key, List<String> value) {
        String str;
        StringBuilder stringBuilder;
        if (!this.mSystemReady) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("not ready for proxyBCConfig type:");
            stringBuilder.append(type);
            stringBuilder.append(" key:");
            stringBuilder.append(key);
            stringBuilder.append(" value:");
            stringBuilder.append(value);
            Log.w(str, stringBuilder.toString());
        } else if (1000 != Binder.getCallingUid()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("proxyBCConfig permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("proxy config:");
            stringBuilder.append(type);
            stringBuilder.append(" ,");
            stringBuilder.append(key);
            stringBuilder.append(" ,");
            stringBuilder.append(value);
            Log.i(str, stringBuilder.toString());
            this.mAM.proxyBCConfig(type, key, value);
        }
    }

    public void proxyWakeLockByPidUid(int pid, int uid, boolean proxy) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("proxyWakeLockByPidUid, pid: ");
        stringBuilder.append(pid);
        stringBuilder.append(", uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(", proxy: ");
        stringBuilder.append(proxy);
        Log.i(str, stringBuilder.toString());
        if (1000 == Binder.getCallingUid() && this.mSystemReady) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mPms.proxyWakeLockByPidUid(pid, uid, proxy);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            Log.w(TAG, "proxyWakeLockByPidUid, system not ready!");
        }
    }

    public void forceReleaseWakeLockByPidUid(int pid, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("forceReleaseWakeLockByPidUid, pid: ");
        stringBuilder.append(pid);
        stringBuilder.append(", uid: ");
        stringBuilder.append(uid);
        Log.i(str, stringBuilder.toString());
        if (1000 == Binder.getCallingUid() && this.mSystemReady) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mPms.forceReleaseWakeLockByPidUid(pid, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            Log.w(TAG, "forceReleaseWakeLockByPidUid, system not ready!");
        }
    }

    public void forceRestoreWakeLockByPidUid(int pid, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("forceRestoreWakeLockByPidUid, pid: ");
        stringBuilder.append(pid);
        stringBuilder.append(", uid: ");
        stringBuilder.append(uid);
        Log.i(str, stringBuilder.toString());
        if (1000 == Binder.getCallingUid() && this.mSystemReady) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mPms.forceRestoreWakeLockByPidUid(pid, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            Log.w(TAG, "forceRestoreWakeLockByPidUid, system not ready!");
        }
    }

    private boolean proxyWakeLock(int subType, List<String> value) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("proxyWakeLock, subType: ");
        stringBuilder.append(subType);
        stringBuilder.append(", value: ");
        stringBuilder.append(value);
        Log.i(str, stringBuilder.toString());
        if (1000 == Binder.getCallingUid() && this.mSystemReady) {
            long ident = Binder.clearCallingIdentity();
            try {
                boolean proxyedWakeLock = this.mPms.proxyedWakeLock(subType, value);
                return proxyedWakeLock;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            Log.w(TAG, "proxyWakeLock, system not ready!");
            return false;
        }
    }

    public boolean getWakeLockByUid(int uid, int wakeflag) {
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for getWakeLockByUid ");
            return false;
        } else if (1000 == Binder.getCallingUid()) {
            return this.mPms.getWakeLockByUid(uid, wakeflag);
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getWakeLockByUid permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void setLcdRatio(int ratio, boolean autoAdjust) {
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for setLcdRatio");
        } else if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLcdRatio permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
        } else {
            this.mPms.setLcdRatio(ratio, autoAdjust);
        }
    }

    public boolean proxyApp(String pkg, int uid, boolean proxy) {
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for proxyApp");
            return false;
        } else if (1000 == Binder.getCallingUid()) {
            return this.mLMS.proxyGps(pkg, uid, proxy);
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("proxyApp permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void refreshPackageWhitelist(int type, List<String> pkgList) {
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for refreshGpsWhitelist");
        } else if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("refreshGpsWhitelist permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
        } else {
            if (type == 2 || type == 6 || type == 7) {
                HwFrameworkFactory.getHwInnerWifiManager().refreshPackageWhitelist(type, pkgList);
            } else {
                this.mLMS.refreshPackageWhitelist(type, pkgList);
            }
        }
    }

    public void configBrightnessRange(int ratioMin, int ratioMax, int autoLimit) {
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for configBrightnessRange");
        } else if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("configBrightnessRange permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
        } else {
            this.mPms.configBrightnessRange(ratioMin, ratioMax, autoLimit);
        }
    }

    public void getWlBatteryStats(List<String> list) {
        this.mProcStats.getWlBatteryStats(list);
    }

    public boolean setPgConfig(int type, int subType, List<String> value) {
        boolean isRestrict = false;
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for pgConfig");
            return false;
        } else if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pgConfig permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return false;
        } else {
            boolean ret = false;
            switch (type) {
                case 0:
                    ret = proxyService(subType, value);
                    break;
                case 1:
                    if (subType == 1) {
                        isRestrict = true;
                    }
                    ret = restrictWifiScan(value, isRestrict);
                    break;
                case 2:
                    break;
                case 3:
                    PowerManagerService powerManagerService = this.mPms;
                    if (subType == 1) {
                        isRestrict = true;
                    }
                    ret = powerManagerService.setGoogleEBS(isRestrict);
                    break;
            }
            ret = proxyWakeLock(subType, value);
            return ret;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0043, code skipped:
            r0.proxyService(r4, r5);
     */
    /* JADX WARNING: Missing block: B:12:0x0047, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean proxyService(int subType, List<String> value) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("proxyService, type:");
        stringBuilder.append(subType);
        stringBuilder.append(", list: ");
        stringBuilder.append(value);
        Log.i(str, stringBuilder.toString());
        JobSchedulerInternal jobScheduler = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        synchronized (mProxyServiceList) {
            switch (subType) {
                case 0:
                    mProxyServiceList.addAll(value);
                    break;
                case 1:
                    mProxyServiceList.removeAll(value);
                    break;
                case 2:
                    mProxyServiceList.clear();
                    break;
                default:
                    return false;
            }
        }
    }

    public boolean closeSocketsForUid(int uid) {
        boolean ret = false;
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for close socket");
            return false;
        } else if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("close socket permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return false;
        } else {
            IBinder b = ServiceManager.getService("network_management");
            Parcel _data = Parcel.obtain();
            Parcel _reply = Parcel.obtain();
            if (b != null) {
                try {
                    _data.writeInterfaceToken(DESCRIPTOR_NETWORKMANAGEMENT_SERVICE);
                    _data.writeInt(uid);
                    b.transact(CODE_CLOSE_SOCKETS_FOR_UID, _data, _reply, 0);
                    _reply.readException();
                    ret = true;
                } catch (RemoteException localRemoteException) {
                    Log.e(TAG, "close socket error", localRemoteException);
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            _reply.recycle();
            _data.recycle();
            return ret;
        }
    }

    public boolean setFirewallPidRule(int chain, int pid, int rule) {
        boolean ret = false;
        if (!this.mSystemReady) {
            Log.w(TAG, "not ready for setFirewallPidRule");
            return false;
        } else if (1000 != Binder.getCallingUid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set firewall rule permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return false;
        } else {
            IBinder b = ServiceManager.getService("network_management");
            Parcel _data = Parcel.obtain();
            Parcel _reply = Parcel.obtain();
            if (b != null) {
                try {
                    _data.writeInterfaceToken(DESCRIPTOR_NETWORKMANAGEMENT_SERVICE);
                    _data.writeInt(chain);
                    _data.writeInt(pid);
                    _data.writeInt(rule);
                    b.transact(CODE_SET_FIREWALL_RULE_FOR_PID, _data, _reply, 0);
                    _reply.readException();
                    ret = true;
                } catch (RemoteException localRemoteException) {
                    Log.e(TAG, "set firewall rule error", localRemoteException);
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            _reply.recycle();
            _data.recycle();
            return ret;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        synchronized (mProxyServiceList) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ProxyServiceList: ");
            stringBuilder.append(mProxyServiceList);
            fout.println(stringBuilder.toString());
        }
    }

    public void killProc(int pid) {
        String str;
        StringBuilder stringBuilder;
        if (1000 != Binder.getCallingUid()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("killProc permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Log.e(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("killProc pid=");
        stringBuilder.append(pid);
        Log.i(str, stringBuilder.toString());
        Process.killProcessQuiet(pid);
    }
}
