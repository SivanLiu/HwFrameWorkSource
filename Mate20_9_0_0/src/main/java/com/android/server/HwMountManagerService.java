package com.android.server;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import java.util.HashMap;

public final class HwMountManagerService {
    private static final String TAG = "HwMountManagerService";
    private static volatile HwMountManagerService mInstance = null;
    private Context mContext;
    private Handler mHandler;
    private final Object mLock = new Object();
    private final HashMap<String, MountingBinderInfo> mMBIMaps = new HashMap();
    private HandlerThread mThread;

    protected final class MountingBinderInfo implements DeathRecipient {
        public final IBinder mBinder;
        public String mMountPoint;

        public MountingBinderInfo(IBinder binder, String mountPoint) {
            this.mBinder = binder;
            this.mMountPoint = mountPoint;
        }

        public void binderDied() {
            HwMountManagerService.this.handleMountingBinderDeath(this);
        }
    }

    public static synchronized HwMountManagerService getInstance(Context context) {
        HwMountManagerService hwMountManagerService;
        synchronized (HwMountManagerService.class) {
            if (mInstance == null) {
                mInstance = new HwMountManagerService(context);
            }
            hwMountManagerService = mInstance;
        }
        return hwMountManagerService;
    }

    public HwMountManagerService(Context context) {
        this.mContext = context;
        this.mThread = new HandlerThread(TAG);
        this.mThread.start();
        this.mHandler = new Handler(this.mThread.getLooper());
    }

    public void handleMountingBinderDeath(final MountingBinderInfo mbi) {
        this.mHandler.post(new Runnable() {
            public void run() {
                String str = HwMountManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Binder Death: mountPoint=");
                stringBuilder.append(mbi.mMountPoint);
                Log.d(str, stringBuilder.toString());
                try {
                    synchronized (HwMountManagerService.this.mLock) {
                        HwMountManagerService.this.mMBIMaps.remove(mbi.mMountPoint);
                    }
                    StorageManagerService.sSelf.mVold.unmountCifs(mbi.mMountPoint);
                } catch (Exception e) {
                    Log.e(HwMountManagerService.TAG, "handleMountingBinderDeath unmountCifs error!");
                }
            }
        });
    }

    public String mountCifs(String source, String option, IBinder binder) {
        this.mContext.enforceCallingPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", "mountCifs called no permission");
        String mountPoint = null;
        try {
            mountPoint = StorageManagerService.sSelf.mVold.mountCifs(source, option);
            if (!(mountPoint == null || binder == null)) {
                synchronized (this.mLock) {
                    MountingBinderInfo mbi = new MountingBinderInfo(binder, mountPoint);
                    binder.linkToDeath(mbi, 0);
                    this.mMBIMaps.put(mountPoint, mbi);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "HwMountManagerService mountCifs error!");
        }
        return mountPoint;
    }

    public void unmountCifs(String mountPoint) {
        this.mContext.enforceCallingPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", "unmountCifs called no permission");
        try {
            StorageManagerService.sSelf.mVold.unmountCifs(mountPoint);
            synchronized (this.mLock) {
                MountingBinderInfo mbi = (MountingBinderInfo) this.mMBIMaps.get(mountPoint);
                if (mbi == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unmountCifs: mountPoint=");
                    stringBuilder.append(mountPoint);
                    stringBuilder.append(" [not found]");
                    Log.d(str, stringBuilder.toString());
                } else {
                    mbi.mBinder.unlinkToDeath(mbi, 0);
                    this.mMBIMaps.remove(mountPoint);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "HwMountManagerService unmountCifs error!");
        }
    }

    public int isSupportedCifs() {
        this.mContext.enforceCallingPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", "isSupportedCifs called no permission");
        try {
            return StorageManagerService.sSelf.mVold.supportCifs();
        } catch (RemoteException e) {
            Log.e(TAG, "HwMountManagerService isSupportedCifs error!");
            return -1;
        }
    }
}
