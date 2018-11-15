package com.android.server.rms.ipcchecker;

import android.os.RemoteException;
import android.rms.IHwSysResManager;
import android.rms.utils.Utils;
import android.util.Log;
import android.util.ZRHung;
import android.zrhung.appeye.AppEyeBinderBlock;
import android.zrhung.appeye.AppEyeFwkBlock;
import com.android.server.rms.HwSysResManagerService;
import com.android.server.rms.IHwIpcMonitor;
import com.android.server.rms.record.ResourceRecordStore;
import com.android.server.rms.record.ResourceUtils;

public class HwIpcMonitorImpl implements IHwIpcMonitor {
    private static final String TAG = "RMS.HwIpcMonitorImpl";
    private static final short ZRHUNG_WP_IPC_OBJECT = (short) 19;
    AppEyeFwkBlock mAppEyeFwkBlock;
    protected long mLastRefreshTime;
    protected Object mLock;
    protected String mName;
    protected int mRecoverCount;

    protected HwIpcMonitorImpl(Object object, String name) {
        this.mRecoverCount = 1;
        this.mName = name;
        this.mLock = object;
        this.mLastRefreshTime = 0;
    }

    protected HwIpcMonitorImpl() {
        this.mRecoverCount = 1;
        this.mLastRefreshTime = 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0027  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x002e  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0029  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0027  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x002e  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0029  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static IHwIpcMonitor getHwIpcMonitor(Object object, String type, String name) {
        Object obj;
        int hashCode = type.hashCode();
        if (hashCode == -1396673086) {
            if (type.equals("backup")) {
                obj = 1;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 1901043637 && type.equals("location")) {
            obj = null;
            switch (obj) {
                case null:
                    return LocationIpcMonitor.getInstance(object, name);
                case 1:
                    return BackupIpcMonitor.getInstance(object, name);
                default:
                    return null;
            }
        }
        obj = -1;
        switch (obj) {
            case null:
                break;
            case 1:
                break;
            default:
                break;
        }
    }

    public void doMonitor() {
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" Begin checkt this monitor! ");
            stringBuilder.append(this.mName);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            if (Utils.DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" check this monitor! ");
                stringBuilder2.append(this.mName);
                Log.d(str2, stringBuilder2.toString());
            }
        }
    }

    public boolean action() {
        if (this.mName == null) {
            this.mName = "general";
        }
        this.mAppEyeFwkBlock = AppEyeFwkBlock.getInstance();
        uploadIPCMonitorFaultToZerohung(this.mName, this.mRecoverCount);
        if (recoverBlockingProcess(true)) {
            return true;
        }
        return false;
    }

    public boolean action(Object lock) {
        return false;
    }

    public String getMonitorName() {
        return this.mName;
    }

    public boolean recoverBlockingProcess(boolean needRecheck) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("recoverBlockingProcess monitor:");
        stringBuilder.append(this.mName);
        stringBuilder.append(" needRecheck:");
        stringBuilder.append(needRecheck);
        Log.i(str, stringBuilder.toString());
        if (this.mLock == null) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("recoverBlockingProcess failed due to null lock:");
            stringBuilder2.append(this.mName);
            Log.i(str, stringBuilder2.toString());
            return false;
        } else if (recoverBlockingProcessInner()) {
            return true;
        } else {
            if (needRecheck) {
                if (HwSysResManagerService.self() == null) {
                    Log.i(TAG, "recoverBlockingProcess failed due to null HwSysResManagerService");
                    return false;
                }
                HwSysResManagerService.self().recheckBlockIpcProcess(this);
            }
            return false;
        }
    }

    private boolean recoverBlockingProcessInner() {
        int pid = -1;
        if (this.mAppEyeFwkBlock != null) {
            pid = this.mAppEyeFwkBlock.getLockOwnerPid(this.mLock);
        }
        if (AppEyeBinderBlock.isNativeProcess(pid) == 0 && doRecoveryForApplication(pid)) {
            return true;
        }
        return false;
    }

    private boolean doRecoveryForNativeDaemon(int pid) {
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" doRecoveryForNativeDaemon:");
            stringBuilder.append(pid);
            Log.d(str, stringBuilder.toString());
        }
        return false;
    }

    private boolean doRecoveryForApplication(int pid) {
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" doRecoveryForApplication:");
            stringBuilder.append(pid);
            Log.d(str, stringBuilder.toString());
        }
        if (!ResourceUtils.killApplicationProcess(pid)) {
            return false;
        }
        checkUploadIPCFaultsLogs();
        return true;
    }

    private void checkUploadIPCFaultsLogs() {
        long currentTime = System.currentTimeMillis();
        if (Utils.IS_DEBUG_VERSION || currentTime - this.mLastRefreshTime >= 86400000) {
            uploadIPCMonitorFaults(this.mName);
            this.mLastRefreshTime = currentTime;
            this.mRecoverCount = 1;
            return;
        }
        this.mRecoverCount++;
    }

    protected void uploadIPCMonitorFaults(String name) {
        if (name == null) {
            name = "general";
        }
        IHwSysResManager mgr = null;
        if (HwSysResManagerService.self() != null) {
            mgr = HwSysResManagerService.self().getHwSysResManagerService();
        }
        IHwSysResManager mgr2 = mgr;
        if (mgr2 != null) {
            try {
                mgr2.recordResourceOverloadStatus(-1, name, 33, this.mRecoverCount, -1, this.mRecoverCount, null);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("upload IPCTIMEOUT error failed:");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    protected void uploadIPCMonitorFaultToZerohung(String name, int recoverCount) {
        StringBuilder message = new StringBuilder();
        message.append("MonitorName:");
        message.append(name);
        message.append(" RecoveryCount:");
        message.append(recoverCount);
        StringBuilder cmd = new StringBuilder("B,n=system_server");
        if (this.mAppEyeFwkBlock != null) {
            int pid = this.mAppEyeFwkBlock.getLockOwnerPid(this.mLock);
            if (pid > 0) {
                cmd.append(",p=");
                cmd.append(pid);
            }
        }
        if (!ZRHung.sendHungEvent(true, cmd.toString(), message.toString())) {
            Log.e(TAG, " ZRHung.sendHungEvent failed!");
        }
    }

    protected void uploadIPCMonitorFaultToIMonitor(String pkgName, int overloadCount, int timeoutDuration) {
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("uploadIPCMonitorFaultToIMonitor");
            stringBuilder.append(this.mLock);
            Log.d(str, stringBuilder.toString());
        }
        ResourceUtils.uploadBigDataLogToIMonitor(33, pkgName, overloadCount, timeoutDuration);
    }

    protected void uploadIPCMonitorFaultToBigDatainfo(String pkgName, int overloadCount, int recoverCount) {
        ResourceRecordStore resourceRecordStore = ResourceRecordStore.getInstance();
        if (resourceRecordStore != null) {
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("uploadIPCMonitorFaultToBigDatainfo");
                stringBuilder.append(this.mLock);
                Log.d(str, stringBuilder.toString());
            }
            resourceRecordStore.createAndCheckUploadBigDataInfos(-1, 33, pkgName, overloadCount, -1, recoverCount, null);
        }
    }
}
