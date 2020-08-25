package com.android.server;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.VolumeInfo;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.pm.PackageManagerService;
import com.huawei.turbozone.ITurboService;
import java.util.Map;

public class HwStorageManagerServiceEx implements IHwStorageManagerServiceEx {
    private static final int CHECK_VOLUME_COMPLETED = 0;
    private static final int INVALID_VALUE = -10;
    private static final Object LOCK = LockGuard.installNewLock(4);
    private static final String TAG = "HwStorageManagerServiceEx";
    private static final int TURBOSERVICE_TIMEOUT_MS = 8192;
    final Context mContext;
    IHwStorageManagerInner mISmsInner = null;
    private PackageManagerService mPms;
    /* access modifiers changed from: private */
    public volatile ITurboService mTurbod;

    public HwStorageManagerServiceEx(IHwStorageManagerInner iSms, Context context) {
        this.mISmsInner = iSms;
        this.mContext = context;
        this.mPms = ServiceManager.getService("package");
    }

    public void onCheckVolumeCompleted(String volId, String diskId, String partGuid, int isSucc) {
        Slog.i(TAG, "onCheckVolumeCompleted :  volId = " + volId + ",diskId = " + diskId + ",partGuid = " + partGuid + ",isSucc = " + isSucc);
        synchronized (LOCK) {
            if (this.mISmsInner == null) {
                Slog.e(TAG, "onCheckVolumeCompleted error: HwStorageManagerInner is null");
                return;
            }
            VolumeInfo vol = this.mISmsInner.getVolumeInfo(volId);
            if (vol != null) {
                if (isSucc == 0) {
                    onCheckVolumeCompletedLocked(vol);
                } else {
                    int oldState = vol.state;
                    vol.state = 6;
                    this.mISmsInner.notifyVolumeStateChanged(vol, oldState, 6);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @GuardedBy({"LOCK"})
    public void onCheckVolumeCompletedLocked(VolumeInfo vol) {
        PackageManagerService packageManagerService = this.mPms;
        if (packageManagerService == null) {
            Slog.e(TAG, "onCheckVolumeCompletedLocked error: PackageManagerService is null");
        } else if (packageManagerService.isOnlyCoreApps()) {
            Slog.d(TAG, "onCheckVolumeCompletedLocked : System booted in core-only mode; ignoring volume " + vol.getId());
        } else if (vol.type == 0) {
            this.mISmsInner.mountAfterCheckCompleted(vol);
        } else {
            Slog.d(TAG, "onCheckVolumeCompletedLocked : Skipping automatic mounting of " + vol);
        }
    }

    public void startCheckVolume(VolumeInfo vol) {
        IHwStorageManagerInner iHwStorageManagerInner = this.mISmsInner;
        if (iHwStorageManagerInner == null) {
            Slog.e(TAG, "startCheckVolume error: HwStorageManagerInner is null");
        } else {
            iHwStorageManagerInner.onCheckStart(vol.id);
        }
    }

    public int startTurboZoneAdaptation() {
        DecisionUtil.bindService(this.mContext, new ServiceConnectionCallback() {
            /* class com.android.server.HwStorageManagerServiceEx.AnonymousClass1 */

            @Override // com.android.server.ServiceConnectionCallback
            public void onServiceConnected() {
                DecisionUtil.executeEvent("com.huawei.recsys.intent.action.queryMultiPeriodApps", "turboZoneStorage", new DecisionCallback() {
                    /* class com.android.server.HwStorageManagerServiceEx.AnonymousClass1.AnonymousClass1 */

                    @Override // com.android.server.DecisionCallback
                    public void onResult(Map result) {
                        if (result == null) {
                            Slog.e(HwStorageManagerServiceEx.TAG, "TurboZone: result = null ");
                        } else if (result.get("MultiPeriodApps") == null) {
                            Slog.e(HwStorageManagerServiceEx.TAG, "TurboZone: result value = null");
                        } else {
                            String userTopApps = String.valueOf(result.get("MultiPeriodApps"));
                            DecisionUtil.unbindService(HwStorageManagerServiceEx.this.mContext);
                            SystemProperties.set("ctl.start", "turbozoned");
                            HwStorageManagerServiceEx.this.connect();
                            if (HwStorageManagerServiceEx.this.mTurbod != null) {
                                try {
                                    HwStorageManagerServiceEx.this.mTurbod.StartTurboZoneAdaptation(userTopApps);
                                    Slog.d(HwStorageManagerServiceEx.TAG, "TurboZone:startTurboZoneAdaptation");
                                } catch (RemoteException e) {
                                    Slog.d(HwStorageManagerServiceEx.TAG, "TurboZone:startTurboZoneAdaptation fail");
                                }
                            }
                        }
                    }

                    @Override // com.android.server.DecisionCallback
                    public void onTimeout() {
                        DecisionUtil.unbindService(HwStorageManagerServiceEx.this.mContext);
                        Slog.e(HwStorageManagerServiceEx.TAG, "TurboZone: callback overtime");
                    }
                });
            }

            @Override // com.android.server.ServiceConnectionCallback
            public void onServiceDisconnected() {
            }
        });
        return 0;
    }

    public int stopTurboZoneAdaptation() {
        try {
            if (this.mTurbod == null) {
                return -1;
            }
            int ret = this.mTurbod.StopTurboZoneAdaptation();
            SystemProperties.set("ctl.stop", "turbozoned");
            Slog.d(TAG, "TurboZone stopTurboZoneAdaptation!");
            return ret;
        } catch (RemoteException e) {
            return -10;
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:16:?, code lost:
        java.lang.Thread.sleep((long) r1);
        android.util.Slog.d(com.android.server.HwStorageManagerServiceEx.TAG, "turbozoned time sleep " + r1 + "ms");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0046, code lost:
        android.util.Slog.e(com.android.server.HwStorageManagerServiceEx.TAG, "turbozoned time sleep error;");
     */
    public void connect() {
        IBinder binder = ServiceManager.getService("turbozoned");
        int sleepMs = 64;
        while (true) {
            if (sleepMs > 16384) {
                break;
            }
            synchronized (this) {
                if (binder == null) {
                    try {
                        binder = ServiceManager.getService("turbozoned");
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                if (binder != null) {
                }
            }
            sleepMs <<= 1;
        }
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    /* class com.android.server.HwStorageManagerServiceEx.AnonymousClass2 */

                    public void binderDied() {
                        Slog.e(HwStorageManagerServiceEx.TAG, "turbozoned died;");
                        ITurboService unused = HwStorageManagerServiceEx.this.mTurbod = null;
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder != null) {
            this.mTurbod = ITurboService.Stub.asInterface(binder);
        } else {
            Slog.e(TAG, "turbozoned not found;");
        }
    }
}
