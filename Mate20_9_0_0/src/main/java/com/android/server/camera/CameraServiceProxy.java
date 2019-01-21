package com.android.server.camera;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.cover.IHwCoverManager;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceProxy.Stub;
import android.metrics.LogMaker;
import android.nfc.INfcAdapter;
import android.os.BatteryManagerInternal;
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
import com.android.server.HwServiceFactory;
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
    private static final String HW_CAMERA_NAME = "com.huawei.camera";
    private static final int MAX_USAGE_HISTORY = 100;
    private static final int MSG_CLOSE_CAMERA = 4;
    private static final int MSG_OPEN_CAMERA = 5;
    private static final int MSG_SWITCH_FRONT_HW = 2;
    private static final int MSG_SWITCH_FRONT_THRID = 3;
    private static final int MSG_SWITCH_USER = 1;
    private static final String NFC_NOTIFICATION_PROP = "ro.camera.notify_nfc";
    private static final String NFC_SERVICE_BINDER_NAME = "nfc";
    private static final int RETRY_DELAY_TIME = 20;
    private static final String SYSTEM_UI_NAME = "com.android.systemui";
    private static final String TAG = "CameraService_proxy";
    private static final int TIPS_TYPE = 0;
    private static final int TOAST_TYPE = 1;
    private static final IBinder nfcInterfaceToken = new Binder();
    private IHwCoverManager coverManager = null;
    private IHwCameraServiceProxy hwcsp;
    private final ArrayMap<String, CameraUsageEvent> mActiveCameraUsage = new ArrayMap();
    private final Stub mCameraServiceProxy = new Stub() {
        public void pingForUserUpdate() {
            if (Binder.getCallingUid() != 1047) {
                String str = CameraServiceProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Calling UID: ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(" doesn't match expected  camera service UID!");
                Slog.e(str, stringBuilder.toString());
                return;
            }
            CameraServiceProxy.this.notifySwitchWithRetries(250);
        }

        public void notifyCameraState(String cameraId, int newCameraState, int facing, String clientName, int apiLevel) {
            String str;
            if (Binder.getCallingUid() != 1047) {
                str = CameraServiceProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Calling UID: ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(" doesn't match expected  camera service UID!");
                Slog.e(str, stringBuilder.toString());
                return;
            }
            str = CameraServiceProxy.cameraStateToString(newCameraState);
            String facingStr = CameraServiceProxy.cameraFacingToString(facing);
            CameraServiceProxy.this.updateActivityCount(cameraId, newCameraState, facing, clientName, apiLevel);
            if (SystemProperties.getBoolean("ro.config.led_close_by_camera", false)) {
                BatteryManagerInternal batteryManager = (BatteryManagerInternal) CameraServiceProxy.this.getLocalService(BatteryManagerInternal.class);
                if (facing == 1 && (newCameraState == 0 || newCameraState == 1)) {
                    batteryManager.notifyFrontCameraStates(true);
                } else {
                    batteryManager.notifyFrontCameraStates(false);
                }
            }
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
                Object obj = -1;
                switch (action.hashCode()) {
                    case -2061058799:
                        if (action.equals("android.intent.action.USER_REMOVED")) {
                            obj = 1;
                            break;
                        }
                        break;
                    case -385593787:
                        if (action.equals("android.intent.action.MANAGED_PROFILE_ADDED")) {
                            obj = 3;
                            break;
                        }
                        break;
                    case -201513518:
                        if (action.equals("android.intent.action.USER_INFO_CHANGED")) {
                            obj = 2;
                            break;
                        }
                        break;
                    case 1051477093:
                        if (action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                            obj = 4;
                            break;
                        }
                        break;
                    case 1121780209:
                        if (action.equals("android.intent.action.USER_ADDED")) {
                            obj = null;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        synchronized (CameraServiceProxy.this.mLock) {
                            if (CameraServiceProxy.this.mEnabledCameraUsers != null) {
                                CameraServiceProxy.this.switchUserLocked(CameraServiceProxy.this.mLastUser);
                                break;
                            } else {
                                return;
                            }
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
        public final int mAPILevel;
        public final int mCameraFacing;
        public final String mClientName;
        private boolean mCompleted = false;
        private long mDurationOrStartTimeMs = SystemClock.elapsedRealtime();

        public CameraUsageEvent(int facing, String clientName, int apiLevel) {
            this.mCameraFacing = facing;
            this.mClientName = clientName;
            this.mAPILevel = apiLevel;
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
        super(context);
        this.mContext = context;
        boolean z = false;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper(), this);
        if (SystemProperties.getInt(NFC_NOTIFICATION_PROP, 0) > 0) {
            z = true;
        }
        this.mNotifyNfc = z;
        this.coverManager = HwFrameworkFactory.getCoverManager();
    }

    public boolean handleMessage(Message msg) {
        String str;
        StringBuilder stringBuilder;
        switch (msg.what) {
            case 1:
                notifySwitchWithRetries(msg.arg1);
                break;
            case 2:
                if (this.hwcsp == null) {
                    Slog.v(TAG, "HwCamera switch front, hwscp = null");
                    break;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwCamera switch front, hwscp != null");
                stringBuilder.append(this.hwcsp.toString());
                Slog.v(str, stringBuilder.toString());
                this.hwcsp.setType(0);
                this.hwcsp.showSlidedownTip();
                break;
            case 3:
                if (this.hwcsp == null) {
                    Slog.v(TAG, "Thrid Camera switch front, hwscp = null");
                    break;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Thrid Camera switch front, hwscp != null");
                stringBuilder.append(this.hwcsp.toString());
                Slog.v(str, stringBuilder.toString());
                this.hwcsp.setType(1);
                this.hwcsp.showSlidedownTip();
                break;
            case 4:
                Slog.v(TAG, "MSG_CLOSE_CAMERA");
                if (this.hwcsp == null) {
                    Slog.v(TAG, "close camera, hwcsp = null");
                    break;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("close camera, hwscp != null");
                stringBuilder.append(this.hwcsp.toString());
                Slog.v(str, stringBuilder.toString());
                this.hwcsp.unRegesiterService();
                break;
            case 5:
                Slog.v(TAG, "MSG_OPEN_CAMERA");
                if (this.hwcsp != null) {
                    this.hwcsp.unRegesiterService();
                }
                this.hwcsp = HwServiceFactory.getHwCameraServiceProxy(this.mContext);
                this.hwcsp.regesiterService();
                break;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CameraServiceProxy error, invalid message: ");
                stringBuilder.append(msg.what);
                Slog.e(str, stringBuilder.toString());
                break;
        }
        return true;
    }

    public void onStart() {
        this.mUserManager = UserManager.get(this.mContext);
        if (this.mUserManager != null) {
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
            return;
        }
        throw new IllegalStateException("UserManagerService must start before CameraServiceProxy!");
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
            if (this.mNotifyNfc && !wasEmpty) {
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
                this.mLogger.write(new LogMaker(1032).setType(4).setSubtype(subtype).setLatency(e.getDuration()).addTaggedData(1322, Integer.valueOf(e.mAPILevel)).setPackageName(e.mClientName));
            }
            this.mCameraUsageHistory.clear();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            CameraStatsJobService.schedule(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void switchUserLocked(int userHandle) {
        Set<Integer> currentUserHandles = getEnabledUserHandles(userHandle);
        this.mLastUser = userHandle;
        if (this.mEnabledCameraUsers == null || !this.mEnabledCameraUsers.equals(currentUserHandles)) {
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

    /* JADX WARNING: Missing block: B:11:0x0014, code skipped:
            if (r7 > 0) goto L_0x0017;
     */
    /* JADX WARNING: Missing block: B:12:0x0016, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:13:0x0017, code skipped:
            android.util.Slog.i(TAG, "Could not notify camera service of user switch, retrying...");
            r6.mHandler.sendMessageDelayed(r6.mHandler.obtainMessage(1, r7 - 1, 0, null), 20);
     */
    /* JADX WARNING: Missing block: B:14:0x002f, code skipped:
            return;
     */
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not notify mediaserver, remote exception: ");
            stringBuilder.append(e2);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    private void removeTipsMessage() {
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
    }

    private void removeStatusMessage() {
        this.mHandler.removeMessages(5);
        this.mHandler.removeMessages(4);
    }

    private void updateActivityCount(String cameraId, int newCameraState, int facing, String clientName, int apiLevel) {
        synchronized (this.mLock) {
            boolean wasEmpty = this.mActiveCameraUsage.isEmpty();
            CameraUsageEvent oldEvent;
            String str;
            StringBuilder stringBuilder;
            switch (newCameraState) {
                case 0:
                    removeStatusMessage();
                    this.mHandler.sendEmptyMessage(5);
                    if (1 == facing && !SYSTEM_UI_NAME.equals(clientName)) {
                        removeTipsMessage();
                        if (this.coverManager.getHallState(1) == 0) {
                            if (!HW_CAMERA_NAME.equals(clientName)) {
                                this.mHandler.sendEmptyMessage(3);
                                break;
                            } else {
                                this.mHandler.sendEmptyMessage(2);
                                break;
                            }
                        }
                    }
                    break;
                case 1:
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Camera ");
                    stringBuilder2.append(cameraId);
                    stringBuilder2.append("in ");
                    stringBuilder2.append(clientName);
                    stringBuilder2.append(" active");
                    Slog.d(str2, stringBuilder2.toString());
                    oldEvent = (CameraUsageEvent) this.mActiveCameraUsage.put(cameraId, new CameraUsageEvent(facing, clientName, apiLevel));
                    if (oldEvent != null) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Camera ");
                        stringBuilder3.append(cameraId);
                        stringBuilder3.append(" was already marked as active");
                        Slog.w(str3, stringBuilder3.toString());
                        oldEvent.markCompleted();
                        this.mCameraUsageHistory.add(oldEvent);
                        break;
                    }
                    break;
                case 2:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Camera ");
                    stringBuilder.append(cameraId);
                    stringBuilder.append("idle");
                    Slog.d(str, stringBuilder.toString());
                    oldEvent = (CameraUsageEvent) this.mActiveCameraUsage.remove(cameraId);
                    if (oldEvent != null) {
                        oldEvent.markCompleted();
                        this.mCameraUsageHistory.add(oldEvent);
                        if (this.mCameraUsageHistory.size() > 100) {
                            dumpUsageEvents();
                            break;
                        }
                    }
                    break;
                case 3:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Camera ");
                    stringBuilder.append(cameraId);
                    stringBuilder.append("close, facing=");
                    stringBuilder.append(facing);
                    Slog.d(str, stringBuilder.toString());
                    removeTipsMessage();
                    removeStatusMessage();
                    this.mHandler.sendEmptyMessage(4);
                    oldEvent = (CameraUsageEvent) this.mActiveCameraUsage.remove(cameraId);
                    if (oldEvent != null) {
                        oldEvent.markCompleted();
                        this.mCameraUsageHistory.add(oldEvent);
                        if (this.mCameraUsageHistory.size() > 100) {
                            dumpUsageEvents();
                            break;
                        }
                    }
                    break;
                default:
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not notify NFC service, remote exception: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
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
