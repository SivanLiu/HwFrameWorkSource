package com.android.server.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceProxy.Stub;
import android.metrics.LogMaker;
import android.nfc.INfcAdapter;
import android.os.Binder;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CameraServiceProxy extends SystemService implements Callback, DeathRecipient {
    private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
    public static final String CAMERA_SERVICE_PROXY_BINDER_NAME = "media.camera.proxy";
    private static final boolean DEBUG = false;
    public static final int DISABLE_POLLING_FLAGS = 4096;
    public static final int ENABLE_POLLING_FLAGS = 0;
    private static final int MAX_USAGE_HISTORY = 100;
    private static final int MSG_SWITCH_USER = 1;
    private static final String NFC_NOTIFICATION_PROP = "ro.camera.notify_nfc";
    private static final String NFC_SERVICE_BINDER_NAME = "nfc";
    private static final int RETRY_DELAY_TIME = 20;
    private static final String TAG = "CameraService_proxy";
    private static final IBinder nfcInterfaceToken = new Binder();
    private final ArrayMap<String, CameraUsageEvent> mActiveCameraUsage = new ArrayMap();
    private final Stub mCameraServiceProxy = new Stub() {
        public void pingForUserUpdate() {
            CameraServiceProxy.this.notifySwitchWithRetries(30);
        }

        public void notifyCameraState(String cameraId, int newCameraState, int facing, String clientName) {
            String state = CameraServiceProxy.cameraStateToString(newCameraState);
            String facingStr = CameraServiceProxy.cameraFacingToString(facing);
            CameraServiceProxy.this.updateActivityCount(cameraId, newCameraState, facing, clientName);
        }
    };
    private ICameraService mCameraServiceRaw;
    private final List<CameraUsageEvent> mCameraUsageHistory = new ArrayList();
    private final Context mContext;
    private Set<Integer> mEnabledCameraUsers;
    private final Handler mHandler;
    private final ServiceThread mHandlerThread;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.intent.action.USER_ADDED") || action.equals("android.intent.action.USER_REMOVED") || action.equals("android.intent.action.USER_INFO_CHANGED") || action.equals("android.intent.action.MANAGED_PROFILE_ADDED") || action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                    synchronized (CameraServiceProxy.this.mLock) {
                        if (CameraServiceProxy.this.mEnabledCameraUsers == null) {
                            return;
                        }
                        CameraServiceProxy.this.switchUserLocked(CameraServiceProxy.this.mLastUser);
                    }
                }
            }
        }
    };
    private int mLastUser;
    private final Object mLock = new Object();
    private final MetricsLogger mLogger = new MetricsLogger();
    private final boolean mNotifyNfc;
    private UserManager mUserManager;

    private static class CameraUsageEvent {
        public final int mCameraFacing;
        public final String mClientName;
        private boolean mCompleted = false;
        private long mDurationOrStartTimeMs = SystemClock.elapsedRealtime();

        public CameraUsageEvent(int facing, String clientName) {
            this.mCameraFacing = facing;
            this.mClientName = clientName;
        }

        public void markCompleted() {
            if (!this.mCompleted) {
                this.mCompleted = true;
                this.mDurationOrStartTimeMs = SystemClock.elapsedRealtime() - this.mDurationOrStartTimeMs;
            }
        }

        public long getDuration() {
            return this.mCompleted ? this.mDurationOrStartTimeMs : 0;
        }
    }

    public CameraServiceProxy(Context context) {
        boolean z = false;
        super(context);
        this.mContext = context;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper(), this);
        if (SystemProperties.getInt(NFC_NOTIFICATION_PROP, 0) > 0) {
            z = true;
        }
        this.mNotifyNfc = z;
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                notifySwitchWithRetries(msg.arg1);
                break;
            default:
                Slog.e(TAG, "CameraServiceProxy error, invalid message: " + msg.what);
                break;
        }
        return true;
    }

    public void onStart() {
        this.mUserManager = UserManager.get(this.mContext);
        if (this.mUserManager == null) {
            throw new IllegalStateException("UserManagerService must start before CameraServiceProxy!");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_INFO_CHANGED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
        publishBinderService(CAMERA_SERVICE_PROXY_BINDER_NAME, this.mCameraServiceProxy);
        publishLocalService(CameraServiceProxy.class, this);
        CameraStatsJobService.schedule(this.mContext);
    }

    public void onStartUser(int userHandle) {
        synchronized (this.mLock) {
            if (this.mEnabledCameraUsers == null) {
                switchUserLocked(userHandle);
            }
        }
    }

    public void onSwitchUser(int userHandle) {
        synchronized (this.mLock) {
            switchUserLocked(userHandle);
        }
    }

    public void binderDied() {
        synchronized (this.mLock) {
            this.mCameraServiceRaw = null;
            boolean wasEmpty = this.mActiveCameraUsage.isEmpty();
            this.mActiveCameraUsage.clear();
            if (this.mNotifyNfc && (wasEmpty ^ 1) != 0) {
                notifyNfcService(true);
            }
        }
    }

    void dumpUsageEvents() {
        synchronized (this.mLock) {
            Collections.shuffle(this.mCameraUsageHistory);
            for (CameraUsageEvent e : this.mCameraUsageHistory) {
                int subtype;
                switch (e.mCameraFacing) {
                    case 0:
                        subtype = 0;
                        break;
                    case 1:
                        subtype = 1;
                        break;
                    case 2:
                        subtype = 2;
                        break;
                    default:
                        continue;
                }
                this.mLogger.write(new LogMaker(1032).setType(4).setSubtype(subtype).setLatency(e.getDuration()).setPackageName(e.mClientName));
            }
            this.mCameraUsageHistory.clear();
        }
        CameraStatsJobService.schedule(this.mContext);
    }

    private void switchUserLocked(int userHandle) {
        Set<Integer> currentUserHandles = getEnabledUserHandles(userHandle);
        this.mLastUser = userHandle;
        if (this.mEnabledCameraUsers == null || (this.mEnabledCameraUsers.equals(currentUserHandles) ^ 1) != 0) {
            this.mEnabledCameraUsers = currentUserHandles;
            notifyMediaserverLocked(1, currentUserHandles);
        }
    }

    private Set<Integer> getEnabledUserHandles(int currentUserHandle) {
        int[] userProfiles = this.mUserManager.getEnabledProfileIds(currentUserHandle);
        Set<Integer> handles = new ArraySet(userProfiles.length);
        for (int id : userProfiles) {
            handles.add(Integer.valueOf(id));
        }
        return handles;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifySwitchWithRetries(int retries) {
        synchronized (this.mLock) {
            if (this.mEnabledCameraUsers == null) {
            } else if (notifyMediaserverLocked(1, this.mEnabledCameraUsers)) {
                retries = 0;
            }
        }
    }

    private boolean notifyMediaserverLocked(int eventType, Set<Integer> updatedUserHandles) {
        if (this.mCameraServiceRaw == null) {
            IBinder cameraServiceBinder = getBinderService(CAMERA_SERVICE_BINDER_NAME);
            if (cameraServiceBinder == null) {
                Slog.w(TAG, "Could not notify mediaserver, camera service not available.");
                return false;
            }
            try {
                cameraServiceBinder.linkToDeath(this, 0);
                this.mCameraServiceRaw = ICameraService.Stub.asInterface(cameraServiceBinder);
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not link to death of native camera service");
                return false;
            }
        }
        try {
            this.mCameraServiceRaw.notifySystemEvent(eventType, toArray(updatedUserHandles));
            return true;
        } catch (RemoteException e2) {
            Slog.w(TAG, "Could not notify mediaserver, remote exception: " + e2);
            return false;
        }
    }

    private void updateActivityCount(String cameraId, int newCameraState, int facing, String clientName) {
        synchronized (this.mLock) {
            boolean wasEmpty = this.mActiveCameraUsage.isEmpty();
            switch (newCameraState) {
                case 1:
                    CameraUsageEvent oldEvent = (CameraUsageEvent) this.mActiveCameraUsage.put(cameraId, new CameraUsageEvent(facing, clientName));
                    if (oldEvent != null) {
                        Slog.w(TAG, "Camera " + cameraId + " was already marked as active");
                        oldEvent.markCompleted();
                        this.mCameraUsageHistory.add(oldEvent);
                        break;
                    }
                    break;
                case 2:
                case 3:
                    CameraUsageEvent doneEvent = (CameraUsageEvent) this.mActiveCameraUsage.remove(cameraId);
                    if (doneEvent != null) {
                        doneEvent.markCompleted();
                        this.mCameraUsageHistory.add(doneEvent);
                        if (this.mCameraUsageHistory.size() > 100) {
                            dumpUsageEvents();
                            break;
                        }
                    }
                    break;
            }
            boolean isEmpty = this.mActiveCameraUsage.isEmpty();
            if (this.mNotifyNfc && wasEmpty != isEmpty) {
                notifyNfcService(isEmpty);
            }
        }
    }

    private void notifyNfcService(boolean enablePolling) {
        IBinder nfcServiceBinder = getBinderService(NFC_SERVICE_BINDER_NAME);
        if (nfcServiceBinder == null) {
            Slog.w(TAG, "Could not connect to NFC service to notify it of camera state");
            return;
        }
        try {
            INfcAdapter.Stub.asInterface(nfcServiceBinder).setReaderMode(nfcInterfaceToken, null, enablePolling ? 0 : 4096, null);
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not notify NFC service, remote exception: " + e);
        }
    }

    private static int[] toArray(Collection<Integer> c) {
        int[] ret = new int[c.size()];
        int idx = 0;
        for (Integer i : c) {
            int idx2 = idx + 1;
            ret[idx] = i.intValue();
            idx = idx2;
        }
        return ret;
    }

    private static String cameraStateToString(int newCameraState) {
        switch (newCameraState) {
            case 0:
                return "CAMERA_STATE_OPEN";
            case 1:
                return "CAMERA_STATE_ACTIVE";
            case 2:
                return "CAMERA_STATE_IDLE";
            case 3:
                return "CAMERA_STATE_CLOSED";
            default:
                return "CAMERA_STATE_UNKNOWN";
        }
    }

    private static String cameraFacingToString(int cameraFacing) {
        switch (cameraFacing) {
            case 0:
                return "CAMERA_FACING_BACK";
            case 1:
                return "CAMERA_FACING_FRONT";
            case 2:
                return "CAMERA_FACING_EXTERNAL";
            default:
                return "CAMERA_FACING_UNKNOWN";
        }
    }
}
