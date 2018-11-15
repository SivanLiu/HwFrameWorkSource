package com.android.server.media;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.INotificationManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.PendingIntent.OnFinished;
import android.common.HwFrameworkFactory;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.IRemoteVolumeController;
import android.media.ISessionTokensListener;
import android.media.MediaController2;
import android.media.SessionToken2;
import android.media.session.IActiveSessionsListener;
import android.media.session.ICallback;
import android.media.session.IOnMediaKeyListener;
import android.media.session.IOnVolumeKeyLongPressListener;
import android.media.session.ISession;
import android.media.session.ISessionCallback;
import android.media.session.ISessionManager.Stub;
import android.media.session.MediaSession.Token;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.HwServiceFactory;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.Watchdog.Monitor;
import com.android.server.wm.WindowManagerService.H;
import com.huawei.pgmng.log.LogPower;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaSessionService extends SystemService implements Monitor {
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final boolean DEBUG_KEY_EVENT = true;
    private static final boolean HWRIDEMODE_FEATURE_SUPPORTED = SystemProperties.getBoolean("ro.config.ride_mode", false);
    private static final int IAWARE_APP_TYPE_MUSIC = 7;
    private static final int IAWARE_TRANSACTION_GETAPPTYPEINFO = 10;
    private static final int MEDIA_KEY_LISTENER_TIMEOUT = 1000;
    private static final int NO_ERROR = 0;
    private static final String TAG = "MediaSessionService";
    static final boolean USE_MEDIA2_APIS = false;
    private static final int WAKELOCK_TIMEOUT = 5000;
    private AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    private IAudioService mAudioService;
    private ContentResolver mContentResolver;
    private FullUserRecord mCurrentFullUserRecord;
    private final SparseIntArray mFullUserIds = new SparseIntArray();
    private MediaSessionRecord mGlobalPrioritySession;
    private final MessageHandler mHandler = new MessageHandler();
    private boolean mHasFeatureLeanback;
    private IBinder mIAwareCMSService;
    private KeyguardManager mKeyguardManager;
    private final Object mLock = new Object();
    private final int mLongPressTimeout;
    private final WakeLock mMediaEventWakeLock;
    private final INotificationManager mNotificationManager;
    private final IPackageManager mPackageManager;
    private IRemoteVolumeController mRvc;
    private final SessionManagerImpl mSessionManagerImpl = new SessionManagerImpl();
    private final Map<SessionToken2, MediaController2> mSessionRecords = new ArrayMap();
    private final List<SessionTokensListenerRecord> mSessionTokensListeners = new ArrayList();
    private final ArrayList<SessionsListenerRecord> mSessionsListeners = new ArrayList();
    private SettingsObserver mSettingsObserver;
    private final SparseArray<FullUserRecord> mUserRecords = new SparseArray();

    private class ControllerCallback extends android.media.MediaController2.ControllerCallback {
        private final SessionToken2 mToken;

        ControllerCallback(SessionToken2 token) {
            this.mToken = token;
        }

        public void onDisconnected(MediaController2 controller) {
            MediaSessionService.this.destroySession2Internal(this.mToken);
        }
    }

    final class MessageHandler extends Handler {
        private static final int MSG_SESSIONS_CHANGED = 1;
        private static final int MSG_SESSIONS_TOKENS_CHANGED = 3;
        private static final int MSG_VOLUME_INITIAL_DOWN = 2;
        private final SparseArray<Integer> mIntegerCache = new SparseArray();

        MessageHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MediaSessionService.this.pushSessionsChanged(((Integer) msg.obj).intValue());
                    return;
                case 2:
                    synchronized (MediaSessionService.this.mLock) {
                        FullUserRecord user = (FullUserRecord) MediaSessionService.this.mUserRecords.get(msg.arg1);
                        if (!(user == null || user.mInitialDownVolumeKeyEvent == null)) {
                            MediaSessionService.this.dispatchVolumeKeyLongPressLocked(user.mInitialDownVolumeKeyEvent);
                            user.mInitialDownVolumeKeyEvent = null;
                        }
                    }
                    return;
                case 3:
                    MediaSessionService.this.pushSessionTokensChanged(((Integer) msg.obj).intValue());
                    return;
                default:
                    return;
            }
        }

        public void postSessionsChanged(int userId) {
            Integer userIdInteger = (Integer) this.mIntegerCache.get(userId);
            if (userIdInteger == null) {
                userIdInteger = Integer.valueOf(userId);
                this.mIntegerCache.put(userId, userIdInteger);
            }
            removeMessages(1, userIdInteger);
            obtainMessage(1, userIdInteger).sendToTarget();
        }
    }

    class SessionManagerImpl extends Stub {
        private static final String EXTRA_WAKELOCK_ACQUIRED = "android.media.AudioService.WAKELOCK_ACQUIRED";
        private static final int WAKELOCK_RELEASE_ON_FINISHED = 1980;
        private IHwBehaviorCollectManager mHwBehaviorManager;
        BroadcastReceiver mKeyEventDone = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        synchronized (MediaSessionService.this.mLock) {
                            if (extras.containsKey(SessionManagerImpl.EXTRA_WAKELOCK_ACQUIRED) && MediaSessionService.this.mMediaEventWakeLock.isHeld()) {
                                MediaSessionService.this.mMediaEventWakeLock.release();
                            }
                        }
                    }
                }
            }
        };
        private KeyEventWakeLockReceiver mKeyEventReceiver = new KeyEventWakeLockReceiver(MediaSessionService.this.mHandler);
        private boolean mVoiceButtonDown = false;
        private boolean mVoiceButtonHandled = false;

        class KeyEventWakeLockReceiver extends ResultReceiver implements Runnable, OnFinished {
            private final Handler mHandler;
            private int mLastTimeoutId = 0;
            private int mRefCount = 0;

            public KeyEventWakeLockReceiver(Handler handler) {
                super(handler);
                this.mHandler = handler;
            }

            public void onTimeout() {
                synchronized (MediaSessionService.this.mLock) {
                    if (this.mRefCount == 0) {
                        return;
                    }
                    this.mLastTimeoutId++;
                    this.mRefCount = 0;
                    releaseWakeLockLocked();
                }
            }

            public void aquireWakeLockLocked() {
                if (this.mRefCount == 0) {
                    MediaSessionService.this.mMediaEventWakeLock.acquire();
                }
                this.mRefCount++;
                this.mHandler.removeCallbacks(this);
                this.mHandler.postDelayed(this, 5000);
            }

            public void run() {
                onTimeout();
            }

            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode >= this.mLastTimeoutId) {
                    synchronized (MediaSessionService.this.mLock) {
                        if (this.mRefCount > 0) {
                            this.mRefCount--;
                            if (this.mRefCount == 0) {
                                releaseWakeLockLocked();
                            }
                        }
                    }
                }
            }

            private void releaseWakeLockLocked() {
                MediaSessionService.this.mMediaEventWakeLock.release();
                this.mHandler.removeCallbacks(this);
            }

            public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
                onReceiveResult(resultCode, null);
            }
        }

        private class MediaKeyListenerResultReceiver extends ResultReceiver implements Runnable {
            private final boolean mAsSystemService;
            private boolean mHandled;
            private final KeyEvent mKeyEvent;
            private final boolean mNeedWakeLock;
            private final String mPackageName;
            private final int mPid;
            private final int mUid;

            /* synthetic */ MediaKeyListenerResultReceiver(SessionManagerImpl x0, String x1, int x2, int x3, boolean x4, KeyEvent x5, boolean x6, AnonymousClass1 x7) {
                this(x1, x2, x3, x4, x5, x6);
            }

            private MediaKeyListenerResultReceiver(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
                super(MediaSessionService.this.mHandler);
                MediaSessionService.this.mHandler.postDelayed(this, 1000);
                this.mPackageName = packageName;
                this.mPid = pid;
                this.mUid = uid;
                this.mAsSystemService = asSystemService;
                this.mKeyEvent = keyEvent;
                this.mNeedWakeLock = needWakeLock;
            }

            public void run() {
                String str = MediaSessionService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The media key listener is timed-out for ");
                stringBuilder.append(this.mKeyEvent);
                Log.d(str, stringBuilder.toString());
                dispatchMediaKeyEvent();
            }

            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 1) {
                    this.mHandled = true;
                    MediaSessionService.this.mHandler.removeCallbacks(this);
                    return;
                }
                dispatchMediaKeyEvent();
            }

            private void dispatchMediaKeyEvent() {
                if (!this.mHandled) {
                    this.mHandled = true;
                    MediaSessionService.this.mHandler.removeCallbacks(this);
                    synchronized (MediaSessionService.this.mLock) {
                        if (MediaSessionService.this.isGlobalPriorityActiveLocked() || !SessionManagerImpl.this.isVoiceKey(this.mKeyEvent.getKeyCode())) {
                            SessionManagerImpl.this.dispatchMediaKeyEventLocked(this.mPackageName, this.mPid, this.mUid, this.mAsSystemService, this.mKeyEvent, this.mNeedWakeLock);
                        } else {
                            SessionManagerImpl.this.handleVoiceKeyEventLocked(this.mPackageName, this.mPid, this.mUid, this.mAsSystemService, this.mKeyEvent, this.mNeedWakeLock);
                        }
                    }
                }
            }
        }

        SessionManagerImpl() {
        }

        public ISession createSession(String packageName, ISessionCallback cb, String tag, int userId) throws RemoteException {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforcePackageName(packageName, uid);
                int resolvedUserId = ActivityManager.handleIncomingUser(pid, uid, userId, 0, true, "createSession", packageName);
                if (cb != null) {
                    ISession sessionBinder = MediaSessionService.this.createSessionInternal(pid, uid, resolvedUserId, packageName, cb, tag).getSessionBinder();
                    return sessionBinder;
                }
                throw new IllegalArgumentException("Controller callback cannot be null");
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public List<IBinder> getSessions(ComponentName componentName, int userId) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                int resolvedUserId = verifySessionsRequest(componentName, userId, pid, uid);
                ArrayList<IBinder> binders = new ArrayList();
                synchronized (MediaSessionService.this.mLock) {
                    for (MediaSessionRecord record : MediaSessionService.this.getActiveSessionsLocked(resolvedUserId)) {
                        binders.add(record.getControllerBinder().asBinder());
                    }
                }
                Binder.restoreCallingIdentity(token);
                return binders;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void addSessionsListener(IActiveSessionsListener listener, ComponentName componentName, int userId) throws RemoteException {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            ComponentName componentName2 = componentName;
            try {
                int resolvedUserId = verifySessionsRequest(componentName2, userId, pid, uid);
                synchronized (MediaSessionService.this.mLock) {
                    IActiveSessionsListener iActiveSessionsListener = listener;
                    int index = MediaSessionService.this.findIndexOfSessionsListenerLocked(iActiveSessionsListener);
                    if (index != -1) {
                        Log.w(MediaSessionService.TAG, "ActiveSessionsListener is already added, ignoring");
                    } else {
                        SessionsListenerRecord record = new SessionsListenerRecord(iActiveSessionsListener, componentName2, resolvedUserId, pid, uid);
                        try {
                            listener.asBinder().linkToDeath(record, 0);
                            MediaSessionService.this.mSessionsListeners.add(record);
                            Binder.restoreCallingIdentity(token);
                        } catch (RemoteException e) {
                            Log.e(MediaSessionService.TAG, "ActiveSessionsListener is dead, ignoring it", e);
                            Binder.restoreCallingIdentity(token);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void removeSessionsListener(IActiveSessionsListener listener) throws RemoteException {
            synchronized (MediaSessionService.this.mLock) {
                int index = MediaSessionService.this.findIndexOfSessionsListenerLocked(listener);
                if (index != -1) {
                    SessionsListenerRecord record = (SessionsListenerRecord) MediaSessionService.this.mSessionsListeners.remove(index);
                    try {
                        record.mListener.asBinder().unlinkToDeath(record, 0);
                    } catch (Exception e) {
                    }
                }
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:55:0x0141 A:{Catch:{ all -> 0x0173 }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void dispatchMediaKeyEvent(String packageName, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
            int i;
            List<MediaSessionRecord> list;
            String str;
            StringBuilder stringBuilder;
            Throwable th;
            KeyEvent keyEvent2 = keyEvent;
            if (keyEvent2 == null || !KeyEvent.isMediaKey(keyEvent.getKeyCode())) {
                Log.w(MediaSessionService.TAG, "Attempted to dispatch null or non-media key event.");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                List<MediaSessionRecord> records = MediaSessionService.this.getActiveSessionsLocked(-1);
                int size = records.size();
                String str2 = MediaSessionService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("dispatchMediaKeyEvent, pkg=");
                String str3 = packageName;
                stringBuilder2.append(str3);
                stringBuilder2.append(" pid=");
                stringBuilder2.append(pid);
                stringBuilder2.append(", uid=");
                stringBuilder2.append(uid);
                stringBuilder2.append(", asSystem=");
                stringBuilder2.append(asSystemService);
                stringBuilder2.append(", record_size=");
                stringBuilder2.append(size);
                stringBuilder2.append(", event=");
                stringBuilder2.append(keyEvent2);
                Log.d(str2, stringBuilder2.toString());
                if (!isUserSetupComplete()) {
                    Slog.i(MediaSessionService.TAG, "Not dispatching media key event because user setup is in progress.");
                } else if (MediaSessionService.HWRIDEMODE_FEATURE_SUPPORTED && SystemProperties.getBoolean("sys.ride_mode", false)) {
                    Slog.i(MediaSessionService.TAG, "Not dispatching media key event because Ride mode is enabled");
                    Binder.restoreCallingIdentity(token);
                } else {
                    synchronized (MediaSessionService.this.mLock) {
                        try {
                            boolean isGlobalPriorityActive = MediaSessionService.this.isGlobalPriorityActiveLocked();
                            if (!isGlobalPriorityActive || uid == 1000) {
                                if (!isGlobalPriorityActive) {
                                    if (MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyListener != null) {
                                        IOnMediaKeyListener access$3100;
                                        ResultReceiver mediaKeyListenerResultReceiver;
                                        ResultReceiver resultReceiver;
                                        str2 = MediaSessionService.TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Send ");
                                        stringBuilder2.append(keyEvent2);
                                        stringBuilder2.append(" to the media key listener");
                                        Log.d(str2, stringBuilder2.toString());
                                        try {
                                            access$3100 = MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyListener;
                                            mediaKeyListenerResultReceiver = mediaKeyListenerResultReceiver;
                                            resultReceiver = mediaKeyListenerResultReceiver;
                                        } catch (RemoteException e) {
                                            i = size;
                                            list = records;
                                            try {
                                                str = MediaSessionService.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Failed to send ");
                                                stringBuilder.append(keyEvent2);
                                                stringBuilder.append(" to the media key listener");
                                                Log.w(str, stringBuilder.toString());
                                                if (!isGlobalPriorityActive) {
                                                }
                                                dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent2, needWakeLock);
                                                Binder.restoreCallingIdentity(token);
                                                return;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                throw th;
                                            }
                                        }
                                        try {
                                            mediaKeyListenerResultReceiver = new MediaKeyListenerResultReceiver(this, str3, pid, uid, asSystemService, keyEvent2, needWakeLock, null);
                                            access$3100.onMediaKey(keyEvent2, resultReceiver);
                                            Binder.restoreCallingIdentity(token);
                                            return;
                                        } catch (RemoteException e2) {
                                            str = MediaSessionService.TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Failed to send ");
                                            stringBuilder.append(keyEvent2);
                                            stringBuilder.append(" to the media key listener");
                                            Log.w(str, stringBuilder.toString());
                                            if (isGlobalPriorityActive) {
                                            }
                                            dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent2, needWakeLock);
                                            Binder.restoreCallingIdentity(token);
                                            return;
                                        }
                                    }
                                }
                                list = records;
                                if (isGlobalPriorityActive || !isVoiceKey(keyEvent.getKeyCode())) {
                                    dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent2, needWakeLock);
                                } else {
                                    handleVoiceKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent2, needWakeLock);
                                }
                            } else {
                                try {
                                    Slog.i(MediaSessionService.TAG, "Only the system can dispatch media key event to the global priority session.");
                                    Binder.restoreCallingIdentity(token);
                                } catch (Throwable th3) {
                                    th = th3;
                                    i = size;
                                    list = records;
                                    throw th;
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            i = size;
                            list = records;
                            throw th;
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setCallback(ICallback callback) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (UserHandle.isSameApp(uid, 1002)) {
                    synchronized (MediaSessionService.this.mLock) {
                        int userId = UserHandle.getUserId(uid);
                        final FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                        String str;
                        StringBuilder stringBuilder;
                        if (user == null || user.mFullUserId != userId) {
                            str = MediaSessionService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Only the full user can set the callback, userId=");
                            stringBuilder.append(userId);
                            Log.w(str, stringBuilder.toString());
                            Binder.restoreCallingIdentity(token);
                            return;
                        }
                        user.mCallback = callback;
                        str = MediaSessionService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("The callback ");
                        stringBuilder.append(user.mCallback);
                        stringBuilder.append(" is set by ");
                        stringBuilder.append(MediaSessionService.this.getCallingPackageName(uid));
                        Log.d(str, stringBuilder.toString());
                        if (user.mCallback == null) {
                        } else {
                            try {
                                user.mCallback.asBinder().linkToDeath(new DeathRecipient() {
                                    public void binderDied() {
                                        synchronized (MediaSessionService.this.mLock) {
                                            user.mCallback = null;
                                        }
                                    }
                                }, 0);
                                user.pushAddressedPlayerChangedLocked();
                            } catch (RemoteException e) {
                                Log.w(MediaSessionService.TAG, "Failed to set callback", e);
                                user.mCallback = null;
                            }
                        }
                    }
                } else {
                    throw new SecurityException("Only Bluetooth service processes can set Callback");
                }
                return;
                Binder.restoreCallingIdentity(token);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setOnVolumeKeyLongPressListener(IOnVolumeKeyLongPressListener listener) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.this.getContext().checkPermission("android.permission.SET_VOLUME_KEY_LONG_PRESS_LISTENER", pid, uid) == 0) {
                    synchronized (MediaSessionService.this.mLock) {
                        int userId = UserHandle.getUserId(uid);
                        final FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                        String str;
                        StringBuilder stringBuilder;
                        if (user == null || user.mFullUserId != userId) {
                            str = MediaSessionService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Only the full user can set the volume key long-press listener, userId=");
                            stringBuilder.append(userId);
                            Log.w(str, stringBuilder.toString());
                            Binder.restoreCallingIdentity(token);
                            return;
                        } else if (user.mOnVolumeKeyLongPressListener == null || user.mOnVolumeKeyLongPressListenerUid == uid) {
                            user.mOnVolumeKeyLongPressListener = listener;
                            user.mOnVolumeKeyLongPressListenerUid = uid;
                            str = MediaSessionService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("The volume key long-press listener ");
                            stringBuilder.append(listener);
                            stringBuilder.append(" is set by ");
                            stringBuilder.append(MediaSessionService.this.getCallingPackageName(uid));
                            Log.d(str, stringBuilder.toString());
                            if (user.mOnVolumeKeyLongPressListener != null) {
                                try {
                                    user.mOnVolumeKeyLongPressListener.asBinder().linkToDeath(new DeathRecipient() {
                                        public void binderDied() {
                                            synchronized (MediaSessionService.this.mLock) {
                                                user.mOnVolumeKeyLongPressListener = null;
                                            }
                                        }
                                    }, 0);
                                } catch (RemoteException e) {
                                    String str2 = MediaSessionService.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Failed to set death recipient ");
                                    stringBuilder2.append(user.mOnVolumeKeyLongPressListener);
                                    Log.w(str2, stringBuilder2.toString());
                                    user.mOnVolumeKeyLongPressListener = null;
                                }
                            }
                        } else {
                            str = MediaSessionService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("The volume key long-press listener cannot be reset by another app , mOnVolumeKeyLongPressListener=");
                            stringBuilder.append(user.mOnVolumeKeyLongPressListenerUid);
                            stringBuilder.append(", uid=");
                            stringBuilder.append(uid);
                            Log.w(str, stringBuilder.toString());
                        }
                    }
                } else {
                    throw new SecurityException("Must hold the SET_VOLUME_KEY_LONG_PRESS_LISTENER permission.");
                }
                return;
                Binder.restoreCallingIdentity(token);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setOnMediaKeyListener(IOnMediaKeyListener listener) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.this.getContext().checkPermission("android.permission.SET_MEDIA_KEY_LISTENER", pid, uid) == 0) {
                    synchronized (MediaSessionService.this.mLock) {
                        int userId = UserHandle.getUserId(uid);
                        final FullUserRecord user = MediaSessionService.this.getFullUserRecordLocked(userId);
                        String str;
                        StringBuilder stringBuilder;
                        if (user == null || user.mFullUserId != userId) {
                            str = MediaSessionService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Only the full user can set the media key listener, userId=");
                            stringBuilder.append(userId);
                            Log.w(str, stringBuilder.toString());
                            Binder.restoreCallingIdentity(token);
                            return;
                        } else if (user.mOnMediaKeyListener == null || user.mOnMediaKeyListenerUid == uid) {
                            user.mOnMediaKeyListener = listener;
                            user.mOnMediaKeyListenerUid = uid;
                            str = MediaSessionService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("The media key listener ");
                            stringBuilder.append(user.mOnMediaKeyListener);
                            stringBuilder.append(" is set by ");
                            stringBuilder.append(MediaSessionService.this.getCallingPackageName(uid));
                            Log.d(str, stringBuilder.toString());
                            if (user.mOnMediaKeyListener != null) {
                                try {
                                    user.mOnMediaKeyListener.asBinder().linkToDeath(new DeathRecipient() {
                                        public void binderDied() {
                                            synchronized (MediaSessionService.this.mLock) {
                                                user.mOnMediaKeyListener = null;
                                            }
                                        }
                                    }, 0);
                                } catch (RemoteException e) {
                                    String str2 = MediaSessionService.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Failed to set death recipient ");
                                    stringBuilder2.append(user.mOnMediaKeyListener);
                                    Log.w(str2, stringBuilder2.toString());
                                    user.mOnMediaKeyListener = null;
                                }
                            }
                        } else {
                            str = MediaSessionService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("The media key listener cannot be reset by another app. , mOnMediaKeyListenerUid=");
                            stringBuilder.append(user.mOnMediaKeyListenerUid);
                            stringBuilder.append(", uid=");
                            stringBuilder.append(uid);
                            Log.w(str, stringBuilder.toString());
                        }
                    }
                } else {
                    throw new SecurityException("Must hold the SET_MEDIA_KEY_LISTENER permission.");
                }
                return;
                Binder.restoreCallingIdentity(token);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dispatchVolumeKeyEvent(String packageName, boolean asSystemService, KeyEvent keyEvent, int stream, boolean musicOnly) {
            Throwable th;
            KeyEvent keyEvent2 = keyEvent;
            String str;
            if (keyEvent2 == null || !(keyEvent.getKeyCode() == 24 || keyEvent.getKeyCode() == 25 || keyEvent.getKeyCode() == 164)) {
                str = packageName;
                Log.w(MediaSessionService.TAG, "Attempted to dispatch null or non-volume key event.");
                return;
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            String str2 = MediaSessionService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dispatchVolumeKeyEvent, pkg=");
            str = packageName;
            stringBuilder.append(str);
            stringBuilder.append(", pid=");
            stringBuilder.append(pid);
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", asSystem=");
            boolean z = asSystemService;
            stringBuilder.append(z);
            stringBuilder.append(", event=");
            stringBuilder.append(keyEvent2);
            Log.d(str2, stringBuilder.toString());
            try {
                synchronized (MediaSessionService.this.mLock) {
                    boolean z2;
                    try {
                        int i;
                        if (MediaSessionService.this.isGlobalPriorityActiveLocked() || MediaSessionService.this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener == null) {
                            dispatchVolumeKeyEventLocked(str, pid, uid, asSystemService, keyEvent2, stream, musicOnly);
                        } else if (keyEvent.getAction() == 0) {
                            try {
                                if (keyEvent.getRepeatCount() == 0) {
                                    MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent = KeyEvent.obtain(keyEvent);
                                    MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeStream = stream;
                                    MediaSessionService.this.mCurrentFullUserRecord.mInitialDownMusicOnly = musicOnly;
                                    MediaSessionService.this.mHandler.sendMessageDelayed(MediaSessionService.this.mHandler.obtainMessage(2, MediaSessionService.this.mCurrentFullUserRecord.mFullUserId, 0), (long) MediaSessionService.this.mLongPressTimeout);
                                } else {
                                    i = stream;
                                    z2 = musicOnly;
                                }
                                if (keyEvent.getRepeatCount() > 0 || keyEvent.isLongPress()) {
                                    MediaSessionService.this.mHandler.removeMessages(2);
                                    if (MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent != null) {
                                        MediaSessionService.this.dispatchVolumeKeyLongPressLocked(MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent);
                                        MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent = null;
                                    }
                                    MediaSessionService.this.dispatchVolumeKeyLongPressLocked(keyEvent2);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                i = stream;
                                z2 = musicOnly;
                                throw th;
                            }
                        } else {
                            i = stream;
                            z2 = musicOnly;
                            MediaSessionService.this.mHandler.removeMessages(2);
                            if (MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent == null || MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent.getDownTime() != keyEvent.getDownTime()) {
                                MediaSessionService.this.dispatchVolumeKeyLongPressLocked(keyEvent2);
                            } else {
                                dispatchVolumeKeyEventLocked(str, pid, uid, z, MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent, MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeStream, MediaSessionService.this.mCurrentFullUserRecord.mInitialDownMusicOnly);
                                dispatchVolumeKeyEventLocked(str, pid, uid, asSystemService, keyEvent2, stream, musicOnly);
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        private void dispatchVolumeKeyEventLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, int stream, boolean musicOnly) {
            boolean up = false;
            boolean down = keyEvent.getAction() == 0;
            if (keyEvent.getAction() == 1) {
                up = true;
            }
            int direction = 0;
            boolean isMute = false;
            int keyCode = keyEvent.getKeyCode();
            if (keyCode != 164) {
                switch (keyCode) {
                    case 24:
                        direction = 1;
                        break;
                    case H.SHOW_STRICT_MODE_VIOLATION /*25*/:
                        direction = -1;
                        break;
                }
            }
            isMute = true;
            if (down || up) {
                if (musicOnly) {
                    keyCode = 4096 | 512;
                } else if (up) {
                    keyCode = 4096 | 20;
                } else {
                    keyCode = 4096 | 17;
                }
                if (direction != 0) {
                    if (up) {
                        direction = 0;
                    }
                    dispatchAdjustVolumeLocked(packageName, pid, uid, asSystemService, stream, direction, keyCode);
                } else if (isMute && down && keyEvent.getRepeatCount() == 0) {
                    dispatchAdjustVolumeLocked(packageName, pid, uid, asSystemService, stream, 101, keyCode);
                }
            }
        }

        public void dispatchAdjustVolume(String packageName, int suggestedStream, int delta, int flags) {
            sendBehavior(BehaviorId.MEDIASESSION_DISPATCHADJUSTVOLUME, new Object[0]);
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    dispatchAdjustVolumeLocked(packageName, pid, uid, false, suggestedStream, delta, flags);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setRemoteVolumeController(IRemoteVolumeController rvc) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforceSystemUiPermission("listen for volume changes", pid, uid);
                MediaSessionService.this.mRvc = rvc;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isGlobalPriorityActive() {
            boolean access$2200;
            synchronized (MediaSessionService.this.mLock) {
                access$2200 = MediaSessionService.this.isGlobalPriorityActiveLocked();
            }
            return access$2200;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(MediaSessionService.this.getContext(), MediaSessionService.TAG, pw)) {
                pw.println("MEDIA SESSION SERVICE (dumpsys media_session)");
                pw.println();
                synchronized (MediaSessionService.this.mLock) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(MediaSessionService.this.mSessionsListeners.size());
                    stringBuilder.append(" sessions listeners.");
                    pw.println(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Global priority session is ");
                    stringBuilder.append(MediaSessionService.this.mGlobalPrioritySession);
                    pw.println(stringBuilder.toString());
                    if (MediaSessionService.this.mGlobalPrioritySession != null) {
                        MediaSessionService.this.mGlobalPrioritySession.dump(pw, "  ");
                    }
                    pw.println("User Records:");
                    int count = MediaSessionService.this.mUserRecords.size();
                    for (int i = 0; i < count; i++) {
                        ((FullUserRecord) MediaSessionService.this.mUserRecords.valueAt(i)).dumpLocked(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                    MediaSessionService.this.mAudioPlayerStateMonitor.dump(MediaSessionService.this.getContext(), pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
            }
        }

        public boolean isTrusted(String controllerPackageName, int controllerPid, int controllerUid) throws RemoteException {
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            String str;
            StringBuilder stringBuilder;
            try {
                if (MediaSessionService.this.getContext().getPackageManager().getPackageUidAsUser(controllerPackageName, UserHandle.getUserId(controllerUid)) != controllerUid) {
                    if (MediaSessionService.DEBUG) {
                        str = MediaSessionService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Package name ");
                        stringBuilder.append(controllerPackageName);
                        stringBuilder.append(" doesn't match with the uid ");
                        stringBuilder.append(controllerUid);
                        Log.d(str, stringBuilder.toString());
                    }
                    Binder.restoreCallingIdentity(token);
                    return false;
                }
                boolean hasMediaControlPermission = hasMediaControlPermission(UserHandle.getUserId(uid), controllerPackageName, controllerPid, controllerUid);
                Binder.restoreCallingIdentity(token);
                return hasMediaControlPermission;
            } catch (NameNotFoundException e) {
                if (MediaSessionService.DEBUG) {
                    str = MediaSessionService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Package ");
                    stringBuilder.append(controllerPackageName);
                    stringBuilder.append(" doesn't exist");
                    Log.d(str, stringBuilder.toString());
                }
                Binder.restoreCallingIdentity(token);
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean createSession2(Bundle sessionToken) {
            return false;
        }

        public void destroySession2(Bundle sessionToken) {
        }

        public List<Bundle> getSessionTokens(boolean activeSessionOnly, boolean sessionServiceOnly, String packageName) throws RemoteException {
            return null;
        }

        public void addSessionTokensListener(ISessionTokensListener listener, int userId, String packageName) throws RemoteException {
        }

        public void removeSessionTokensListener(ISessionTokensListener listener, String packageName) throws RemoteException {
        }

        private int verifySessionsRequest(ComponentName componentName, int userId, int pid, int uid) {
            String packageName = null;
            if (componentName != null) {
                packageName = componentName.getPackageName();
                MediaSessionService.this.enforcePackageName(packageName, uid);
            }
            int resolvedUserId = ActivityManager.handleIncomingUser(pid, uid, userId, true, true, "getSessions", packageName);
            MediaSessionService.this.enforceMediaPermissions(componentName, pid, uid, resolvedUserId);
            return resolvedUserId;
        }

        private int verifySessionsRequest2(int targetUserId, String callerPackageName, int callerPid, int callerUid) throws RemoteException {
            int resolvedUserId = ActivityManager.handleIncomingUser(callerPid, callerUid, targetUserId, true, true, "getSessionTokens", callerPackageName);
            if (hasMediaControlPermission(resolvedUserId, callerPackageName, callerPid, callerUid)) {
                return resolvedUserId;
            }
            throw new SecurityException("Missing permission to control media.");
        }

        private boolean hasMediaControlPermission(int resolvedUserId, String packageName, int pid, int uid) throws RemoteException {
            if (MediaSessionService.this.isCurrentVolumeController(pid, uid) || uid == 1000 || MediaSessionService.this.getContext().checkPermission("android.permission.MEDIA_CONTENT_CONTROL", pid, uid) == 0) {
                return true;
            }
            if (MediaSessionService.DEBUG) {
                String str = MediaSessionService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(packageName);
                stringBuilder.append(" (uid=");
                stringBuilder.append(uid);
                stringBuilder.append(") hasn't granted MEDIA_CONTENT_CONTROL");
                Log.d(str, stringBuilder.toString());
            }
            int userId = UserHandle.getUserId(uid);
            if (resolvedUserId != userId) {
                return false;
            }
            List<ComponentName> enabledNotificationListeners = MediaSessionService.this.mNotificationManager.getEnabledNotificationListeners(userId);
            if (enabledNotificationListeners != null) {
                for (int i = 0; i < enabledNotificationListeners.size(); i++) {
                    if (TextUtils.equals(packageName, ((ComponentName) enabledNotificationListeners.get(i)).getPackageName())) {
                        return true;
                    }
                }
            }
            if (MediaSessionService.DEBUG) {
                String str2 = MediaSessionService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(packageName);
                stringBuilder2.append(" (uid=");
                stringBuilder2.append(uid);
                stringBuilder2.append(") doesn't have an enabled notification listener");
                Log.d(str2, stringBuilder2.toString());
            }
            return false;
        }

        private void dispatchAdjustVolumeLocked(String packageName, int pid, int uid, boolean asSystemService, int suggestedStream, int direction, int flags) {
            MediaSessionRecord access$2300;
            int suggestedStream2 = suggestedStream;
            final int i = direction;
            final int i2 = flags;
            if (MediaSessionService.this.isGlobalPriorityActiveLocked()) {
                access$2300 = MediaSessionService.this.mGlobalPrioritySession;
            } else {
                access$2300 = MediaSessionService.this.mCurrentFullUserRecord.mPriorityStack.getDefaultVolumeSession();
            }
            MediaSessionRecord session = access$2300;
            boolean preferSuggestedStream = false;
            if (isValidLocalStreamType(suggestedStream2) && AudioSystem.isStreamActive(suggestedStream2, 0)) {
                preferSuggestedStream = true;
            }
            boolean preferSuggestedStream2 = preferSuggestedStream;
            String str = MediaSessionService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adjusting ");
            stringBuilder.append(session);
            stringBuilder.append(" by ");
            stringBuilder.append(i);
            stringBuilder.append(". flags=");
            stringBuilder.append(i2);
            stringBuilder.append(", suggestedStream=");
            stringBuilder.append(suggestedStream2);
            stringBuilder.append(", preferSuggestedStream=");
            stringBuilder.append(preferSuggestedStream2);
            Log.d(str, stringBuilder.toString());
            if (session == null || preferSuggestedStream2) {
                if (!((i2 & 512) == 0 || AudioSystem.isStreamActive(3, 0))) {
                    if (suggestedStream2 == Integer.MIN_VALUE && AudioSystem.isStreamActive(0, 0)) {
                        Log.d(MediaSessionService.TAG, "set suggestedStream to voice call");
                        suggestedStream2 = 0;
                    } else {
                        Log.d(MediaSessionService.TAG, "No active session to adjust, skipping media only volume event");
                        return;
                    }
                }
                final int temp = suggestedStream2;
                MediaSessionService.this.mHandler.post(new Runnable() {
                    public void run() {
                        try {
                            MediaSessionService.this.mAudioService.adjustSuggestedStreamVolume(i, temp, i2, MediaSessionService.this.getContext().getOpPackageName(), MediaSessionService.TAG);
                        } catch (RemoteException e) {
                            Log.e(MediaSessionService.TAG, "Error adjusting default volume.", e);
                        } catch (IllegalArgumentException e2) {
                            Log.e(MediaSessionService.TAG, "IllegalArgument when adjust stream volume.", e2);
                        }
                    }
                });
            } else {
                session.adjustVolume(packageName, pid, uid, null, asSystemService, i, i2, true);
            }
        }

        private void handleVoiceKeyEventLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
            KeyEvent keyEvent2;
            int action = keyEvent.getAction();
            boolean isLongPress = (keyEvent.getFlags() & 128) != 0;
            boolean z;
            if (action != 0) {
                z = needWakeLock;
                if (action == 1 && this.mVoiceButtonDown) {
                    this.mVoiceButtonDown = false;
                    if (!(this.mVoiceButtonHandled || keyEvent.isCanceled())) {
                        dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, KeyEvent.changeAction(keyEvent, 0), z);
                        dispatchMediaKeyEventLocked(packageName, pid, uid, asSystemService, keyEvent, needWakeLock);
                        return;
                    }
                }
            } else if (keyEvent.getRepeatCount() == 0) {
                this.mVoiceButtonDown = true;
                this.mVoiceButtonHandled = false;
                keyEvent2 = keyEvent;
                z = needWakeLock;
                return;
            } else if (this.mVoiceButtonDown && !this.mVoiceButtonHandled && isLongPress) {
                this.mVoiceButtonHandled = true;
                startVoiceInput(needWakeLock);
            } else {
                z = needWakeLock;
            }
            keyEvent2 = keyEvent;
        }

        private void dispatchMediaKeyEventLocked(String packageName, int pid, int uid, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
            KeyEvent keyEvent2 = keyEvent;
            MediaSessionRecord session = MediaSessionService.this.mCurrentFullUserRecord.getMediaButtonSessionLocked();
            int i = -1;
            if (session != null) {
                String str = MediaSessionService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sending ");
                stringBuilder.append(keyEvent2);
                stringBuilder.append(" to ");
                stringBuilder.append(session);
                Log.d(str, stringBuilder.toString());
                if (keyEvent.getAction() == 0) {
                    HwServiceFactory.reportMediaKeyToIAware(session.mOwnerUid);
                    LogPower.push(148, "mediakey", session.mPackageName, Integer.toString(session.mOwnerPid), new String[]{String.valueOf(keyEvent.getKeyCode())});
                }
                if (needWakeLock) {
                    this.mKeyEventReceiver.aquireWakeLockLocked();
                }
                if (needWakeLock) {
                    i = this.mKeyEventReceiver.mLastTimeoutId;
                }
                session.sendMediaButton(packageName, pid, uid, asSystemService, keyEvent2, i, this.mKeyEventReceiver);
                if (MediaSessionService.this.mCurrentFullUserRecord.mCallback != null) {
                    try {
                        MediaSessionService.this.mCurrentFullUserRecord.mCallback.onMediaKeyEventDispatchedToMediaSession(keyEvent2, new Token(session.getControllerBinder()));
                    } catch (RemoteException e) {
                        Log.w(MediaSessionService.TAG, "Failed to send callback", e);
                    }
                }
            } else if (MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver == null && MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver == null) {
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, "Sending media key ordered broadcast");
                }
                if (needWakeLock) {
                    MediaSessionService.this.mMediaEventWakeLock.acquire();
                }
                Intent keyIntent = new Intent("android.intent.action.MEDIA_BUTTON", null);
                keyIntent.addFlags(268435456);
                keyIntent.putExtra("android.intent.extra.KEY_EVENT", keyEvent2);
                if (needWakeLock) {
                    keyIntent.putExtra(EXTRA_WAKELOCK_ACQUIRED, WAKELOCK_RELEASE_ON_FINISHED);
                }
                if (checkPackage("com.android.mediacenter")) {
                    Log.d(MediaSessionService.TAG, "Sending media key to mediacenter apk");
                    keyIntent.setPackage("com.android.mediacenter");
                    keyIntent.addFlags(32);
                }
                MediaSessionService.this.getContext().sendOrderedBroadcastAsUser(keyIntent, UserHandle.CURRENT, null, this.mKeyEventDone, MediaSessionService.this.mHandler, -1, null, null);
            } else {
                if (needWakeLock) {
                    this.mKeyEventReceiver.aquireWakeLockLocked();
                }
                Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
                mediaButtonIntent.addFlags(268435456);
                mediaButtonIntent.putExtra("android.intent.extra.KEY_EVENT", keyEvent2);
                mediaButtonIntent.putExtra("android.intent.extra.PACKAGE_NAME", asSystemService ? MediaSessionService.this.getContext().getPackageName() : packageName);
                String str2;
                StringBuilder stringBuilder2;
                try {
                    ComponentName componentName;
                    if (MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver != null) {
                        PendingIntent receiver = MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver;
                        String str3 = MediaSessionService.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Sending ");
                        stringBuilder3.append(keyEvent2);
                        stringBuilder3.append(" to the last known PendingIntent ");
                        stringBuilder3.append(receiver);
                        Log.d(str3, stringBuilder3.toString());
                        Context context = MediaSessionService.this.getContext();
                        if (needWakeLock) {
                            i = this.mKeyEventReceiver.mLastTimeoutId;
                        }
                        receiver.send(context, i, mediaButtonIntent, this.mKeyEventReceiver, MediaSessionService.this.mHandler);
                        if (MediaSessionService.this.mCurrentFullUserRecord.mCallback != null) {
                            componentName = MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver.getIntent().getComponent();
                            if (componentName != null) {
                                MediaSessionService.this.mCurrentFullUserRecord.mCallback.onMediaKeyEventDispatchedToMediaButtonReceiver(keyEvent2, componentName);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    componentName = MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver;
                    str2 = MediaSessionService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Sending ");
                    stringBuilder2.append(keyEvent2);
                    stringBuilder2.append(" to the restored intent ");
                    stringBuilder2.append(componentName);
                    Log.d(str2, stringBuilder2.toString());
                    mediaButtonIntent.setComponent(componentName);
                    MediaSessionService.this.getContext().sendBroadcastAsUser(mediaButtonIntent, UserHandle.of(MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiverUserId));
                    if (MediaSessionService.this.mCurrentFullUserRecord.mCallback != null) {
                        MediaSessionService.this.mCurrentFullUserRecord.mCallback.onMediaKeyEventDispatchedToMediaButtonReceiver(keyEvent2, componentName);
                    }
                } catch (CanceledException e2) {
                    str2 = MediaSessionService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error sending key event to media button receiver ");
                    stringBuilder2.append(MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver);
                    Log.i(str2, stringBuilder2.toString(), e2);
                } catch (RemoteException e3) {
                    Log.w(MediaSessionService.TAG, "Failed to send callback", e3);
                }
            }
        }

        private boolean checkPackage(String packageName) {
            if (packageName == null || BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(packageName)) {
                return false;
            }
            try {
                ApplicationInfo info = MediaSessionService.this.getContext().getPackageManager().getApplicationInfo(packageName, 8192);
                return true;
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        private void startVoiceInput(boolean needWakeLock) {
            Intent voiceIntent;
            PowerManager pm = (PowerManager) MediaSessionService.this.getContext().getSystemService("power");
            boolean z = false;
            boolean isLocked = MediaSessionService.this.mKeyguardManager != null && MediaSessionService.this.mKeyguardManager.isKeyguardLocked();
            if (isLocked || !pm.isScreenOn()) {
                voiceIntent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
                String str = "android.speech.extras.EXTRA_SECURE";
                if (isLocked && MediaSessionService.this.mKeyguardManager.isKeyguardSecure()) {
                    z = true;
                }
                voiceIntent.putExtra(str, z);
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_VOICE_SEARCH_HANDS_FREE");
            } else {
                voiceIntent = new Intent("android.speech.action.WEB_SEARCH");
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_WEB_SEARCH");
            }
            if (needWakeLock) {
                MediaSessionService.this.mMediaEventWakeLock.acquire();
            }
            try {
                voiceIntent.setFlags(276824064);
                if (MediaSessionService.DEBUG) {
                    String str2 = MediaSessionService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("voiceIntent: ");
                    stringBuilder.append(voiceIntent);
                    Log.d(str2, stringBuilder.toString());
                }
                MediaSessionService.this.getContext().startActivityAsUser(voiceIntent, UserHandle.CURRENT);
                if (!needWakeLock) {
                    return;
                }
            } catch (ActivityNotFoundException e) {
                String str3 = MediaSessionService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("No activity for search: ");
                stringBuilder2.append(e);
                Log.w(str3, stringBuilder2.toString());
                if (!needWakeLock) {
                    return;
                }
            } catch (Throwable th) {
                if (needWakeLock) {
                    MediaSessionService.this.mMediaEventWakeLock.release();
                }
                throw th;
            }
            MediaSessionService.this.mMediaEventWakeLock.release();
        }

        private boolean isVoiceKey(int keyCode) {
            return keyCode == 79 || (!MediaSessionService.this.mHasFeatureLeanback && keyCode == 85);
        }

        private boolean isUserSetupComplete() {
            return Secure.getIntForUser(MediaSessionService.this.getContext().getContentResolver(), "user_setup_complete", 0, -2) != 0;
        }

        private boolean isValidLocalStreamType(int streamType) {
            return streamType >= 0 && streamType <= 5;
        }

        private void sendBehavior(BehaviorId bid, Object... params) {
            if (this.mHwBehaviorManager == null) {
                this.mHwBehaviorManager = HwFrameworkFactory.getHwBehaviorCollectManager();
            }
            if (this.mHwBehaviorManager == null) {
                Log.w(MediaSessionService.TAG, "HwBehaviorCollectManager is null");
            } else if (params == null || params.length == 0) {
                this.mHwBehaviorManager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid);
            } else {
                this.mHwBehaviorManager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid, params);
            }
        }
    }

    private final class SessionTokensListenerRecord implements DeathRecipient {
        private final ISessionTokensListener mListener;
        private final int mUserId;

        public SessionTokensListenerRecord(ISessionTokensListener listener, int userId) {
            this.mListener = listener;
            this.mUserId = userId;
        }

        public void binderDied() {
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.mSessionTokensListeners.remove(this);
            }
        }
    }

    final class SessionsListenerRecord implements DeathRecipient {
        private final ComponentName mComponentName;
        private final IActiveSessionsListener mListener;
        private final int mPid;
        private final int mUid;
        private final int mUserId;

        public SessionsListenerRecord(IActiveSessionsListener listener, ComponentName componentName, int userId, int pid, int uid) {
            this.mListener = listener;
            this.mComponentName = componentName;
            this.mUserId = userId;
            this.mPid = pid;
            this.mUid = uid;
        }

        public void binderDied() {
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.mSessionsListeners.remove(this);
            }
        }
    }

    final class SettingsObserver extends ContentObserver {
        private final Uri mSecureSettingsUri;

        /* synthetic */ SettingsObserver(MediaSessionService x0, AnonymousClass1 x1) {
            this();
        }

        private SettingsObserver() {
            super(null);
            this.mSecureSettingsUri = Secure.getUriFor("enabled_notification_listeners");
        }

        private void observe() {
            MediaSessionService.this.mContentResolver.registerContentObserver(this.mSecureSettingsUri, false, this, -1);
        }

        public void onChange(boolean selfChange, Uri uri) {
            MediaSessionService.this.updateActiveSessionListeners();
        }
    }

    final class FullUserRecord implements OnMediaButtonSessionChangedListener {
        private static final String COMPONENT_NAME_USER_ID_DELIM = ",";
        private ICallback mCallback;
        private final int mFullUserId;
        private boolean mInitialDownMusicOnly;
        private KeyEvent mInitialDownVolumeKeyEvent;
        private int mInitialDownVolumeStream;
        private PendingIntent mLastMediaButtonReceiver;
        private IOnMediaKeyListener mOnMediaKeyListener;
        private int mOnMediaKeyListenerUid;
        private IOnVolumeKeyLongPressListener mOnVolumeKeyLongPressListener;
        private int mOnVolumeKeyLongPressListenerUid;
        private final MediaSessionStack mPriorityStack;
        private ComponentName mRestoredMediaButtonReceiver;
        private int mRestoredMediaButtonReceiverUserId;

        public FullUserRecord(int fullUserId) {
            this.mFullUserId = fullUserId;
            this.mPriorityStack = new MediaSessionStack(MediaSessionService.this.mAudioPlayerStateMonitor, this);
            String mediaButtonReceiver = Secure.getStringForUser(MediaSessionService.this.mContentResolver, "media_button_receiver", this.mFullUserId);
            if (mediaButtonReceiver != null) {
                String[] tokens = mediaButtonReceiver.split(COMPONENT_NAME_USER_ID_DELIM);
                if (tokens != null && tokens.length == 2) {
                    this.mRestoredMediaButtonReceiver = ComponentName.unflattenFromString(tokens[0]);
                    this.mRestoredMediaButtonReceiverUserId = Integer.parseInt(tokens[1]);
                }
            }
        }

        public void destroySessionsForUserLocked(int userId) {
            for (MediaSessionRecord session : this.mPriorityStack.getPriorityList(false, userId)) {
                MediaSessionService.this.destroySessionLocked(session);
            }
        }

        public void dumpLocked(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("Record for full_user=");
            stringBuilder2.append(this.mFullUserId);
            pw.print(stringBuilder2.toString());
            int size = MediaSessionService.this.mFullUserIds.size();
            int i = 0;
            while (i < size) {
                if (MediaSessionService.this.mFullUserIds.keyAt(i) != MediaSessionService.this.mFullUserIds.valueAt(i) && MediaSessionService.this.mFullUserIds.valueAt(i) == this.mFullUserId) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(", profile_user=");
                    stringBuilder.append(MediaSessionService.this.mFullUserIds.keyAt(i));
                    pw.print(stringBuilder.toString());
                }
                i++;
            }
            pw.println();
            String indent = new StringBuilder();
            indent.append(prefix);
            indent.append("  ");
            indent = indent.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("Volume key long-press listener: ");
            stringBuilder.append(this.mOnVolumeKeyLongPressListener);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("Volume key long-press listener package: ");
            stringBuilder.append(MediaSessionService.this.getCallingPackageName(this.mOnVolumeKeyLongPressListenerUid));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("Media key listener: ");
            stringBuilder.append(this.mOnMediaKeyListener);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("Media key listener package: ");
            stringBuilder.append(MediaSessionService.this.getCallingPackageName(this.mOnMediaKeyListenerUid));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("Callback: ");
            stringBuilder.append(this.mCallback);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("Last MediaButtonReceiver: ");
            stringBuilder.append(this.mLastMediaButtonReceiver);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("Restored MediaButtonReceiver: ");
            stringBuilder.append(this.mRestoredMediaButtonReceiver);
            pw.println(stringBuilder.toString());
            this.mPriorityStack.dump(pw, indent);
        }

        public void onMediaButtonSessionChanged(MediaSessionRecord oldMediaButtonSession, MediaSessionRecord newMediaButtonSession) {
            String str = MediaSessionService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Media button session is changed to ");
            stringBuilder.append(newMediaButtonSession);
            Log.d(str, stringBuilder.toString());
            synchronized (MediaSessionService.this.mLock) {
                if (oldMediaButtonSession != null) {
                    MediaSessionService.this.mHandler.postSessionsChanged(oldMediaButtonSession.getUserId());
                }
                if (newMediaButtonSession != null) {
                    rememberMediaButtonReceiverLocked(newMediaButtonSession);
                    MediaSessionService.this.mHandler.postSessionsChanged(newMediaButtonSession.getUserId());
                }
                pushAddressedPlayerChangedLocked();
            }
        }

        public void rememberMediaButtonReceiverLocked(MediaSessionRecord record) {
            PendingIntent receiver = record.getMediaButtonReceiver();
            this.mLastMediaButtonReceiver = receiver;
            boolean shouldSaveReceiver = false;
            String componentName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (receiver != null) {
                ComponentName component = receiver.getIntent().getComponent();
                if (component != null && record.getPackageName().equals(component.getPackageName())) {
                    componentName = component.flattenToString();
                    String packageName = component.getPackageName();
                    if (checkAppHasButtonReceiver(packageName) && isAudioApp(packageName)) {
                        shouldSaveReceiver = true;
                        this.mRestoredMediaButtonReceiver = component;
                    }
                }
            }
            if (shouldSaveReceiver) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(componentName);
                stringBuilder.append(COMPONENT_NAME_USER_ID_DELIM);
                stringBuilder.append(record.getUserId());
                Secure.putStringForUser(MediaSessionService.this.mContentResolver, "media_button_receiver", stringBuilder.toString(), this.mFullUserId);
            }
        }

        private boolean checkAppHasButtonReceiver(String packageName) {
            if (packageName == null) {
                Log.v(MediaSessionService.TAG, " checkAppHasButtonReceiver : false, packageName is null");
                return false;
            }
            Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
            mediaButtonIntent.setPackage(packageName);
            List<ResolveInfo> ril = MediaSessionService.this.getContext().getPackageManager().queryBroadcastReceivers(mediaButtonIntent, 0);
            if (ril == null || ril.size() == 0) {
                String str = MediaSessionService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" checkAppHasButtonReceiver : false, packageName: ");
                stringBuilder.append(packageName);
                Log.v(str, stringBuilder.toString());
                return false;
            }
            String str2 = MediaSessionService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" checkAppHasButtonReceiver : true, packageName: ");
            stringBuilder2.append(packageName);
            Log.v(str2, stringBuilder2.toString());
            return true;
        }

        private IBinder getIAwareCMSService() {
            if (MediaSessionService.this.mIAwareCMSService == null) {
                MediaSessionService.this.mIAwareCMSService = ServiceManager.getService("IAwareCMSService");
            }
            return MediaSessionService.this.mIAwareCMSService;
        }

        private boolean isAudioApp(String packageName) {
            IBinder service = getIAwareCMSService();
            if (!(service == null || packageName == null)) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.rms.iaware.ICMSManager");
                data.writeString(packageName);
                try {
                    if (!service.transact(10, data, reply, 0)) {
                        return false;
                    }
                    reply.readExceptionCode();
                    if (reply.readInt() != 0 && reply.readInt() == 7) {
                        String str = MediaSessionService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("isAudioApp:");
                        stringBuilder.append(packageName);
                        stringBuilder.append(" is music app");
                        Log.i(str, stringBuilder.toString());
                        return true;
                    }
                } catch (RemoteException e) {
                    return false;
                }
            }
            return false;
        }

        private void pushAddressedPlayerChangedLocked() {
            if (this.mCallback != null) {
                try {
                    MediaSessionRecord mediaButtonSession = getMediaButtonSessionLocked();
                    if (mediaButtonSession != null) {
                        this.mCallback.onAddressedPlayerChangedToMediaSession(new Token(mediaButtonSession.getControllerBinder()));
                    } else if (MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver != null) {
                        this.mCallback.onAddressedPlayerChangedToMediaButtonReceiver(MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver.getIntent().getComponent());
                    } else if (MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver != null) {
                        this.mCallback.onAddressedPlayerChangedToMediaButtonReceiver(MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver);
                    }
                } catch (RemoteException e) {
                    Log.w(MediaSessionService.TAG, "Failed to pushAddressedPlayerChangedLocked", e);
                }
            }
        }

        private MediaSessionRecord getMediaButtonSessionLocked() {
            return MediaSessionService.this.isGlobalPriorityActiveLocked() ? MediaSessionService.this.mGlobalPrioritySession : this.mPriorityStack.getMediaButtonSession();
        }
    }

    public MediaSessionService(Context context) {
        super(context);
        this.mMediaEventWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "handleMediaEvent");
        this.mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        this.mNotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        this.mPackageManager = AppGlobals.getPackageManager();
    }

    public void onStart() {
        publishBinderService("media_session", this.mSessionManagerImpl);
        Watchdog.getInstance().addMonitor(this);
        this.mKeyguardManager = (KeyguardManager) getContext().getSystemService("keyguard");
        this.mAudioService = getAudioService();
        this.mAudioPlayerStateMonitor = AudioPlayerStateMonitor.getInstance();
        this.mAudioPlayerStateMonitor.registerListener(new -$$Lambda$MediaSessionService$za_9dlUSlnaiZw6eCdPVEZq0XLw(this), null);
        this.mAudioPlayerStateMonitor.registerSelfIntoAudioServiceIfNeeded(this.mAudioService);
        this.mContentResolver = getContext().getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this, null);
        this.mSettingsObserver.observe();
        this.mHasFeatureLeanback = getContext().getPackageManager().hasSystemFeature("android.software.leanback");
        updateUser();
        registerPackageBroadcastReceivers();
        buildMediaSessionService2List();
    }

    public static /* synthetic */ void lambda$onStart$0(MediaSessionService mediaSessionService, AudioPlaybackConfiguration config, boolean isRemoved) {
        if (!isRemoved && config.isActive() && config.getPlayerType() != 3) {
            synchronized (mediaSessionService.mLock) {
                FullUserRecord user = mediaSessionService.getFullUserRecordLocked(UserHandle.getUserId(config.getClientUid()));
                if (user != null) {
                    user.mPriorityStack.updateMediaButtonSessionIfNeeded();
                }
            }
        }
    }

    private IAudioService getAudioService() {
        return IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
    }

    private boolean isGlobalPriorityActiveLocked() {
        return this.mGlobalPrioritySession != null && this.mGlobalPrioritySession.isActive();
    }

    public void updateSession(MediaSessionRecord record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (user == null) {
                Log.w(TAG, "Unknown session updated. Ignoring.");
                return;
            }
            if ((record.getFlags() & 65536) != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Global priority session is updated, active=");
                stringBuilder.append(record.isActive());
                Log.d(str, stringBuilder.toString());
                user.pushAddressedPlayerChangedLocked();
            } else if (user.mPriorityStack.contains(record)) {
                user.mPriorityStack.onSessionStateChange(record);
            } else {
                Log.w(TAG, "Unknown session updated. Ignoring.");
                return;
            }
            this.mHandler.postSessionsChanged(record.getUserId());
        }
    }

    public void setGlobalPrioritySession(MediaSessionRecord record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (this.mGlobalPrioritySession != record) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Global priority session is changed from ");
                stringBuilder.append(this.mGlobalPrioritySession);
                stringBuilder.append(" to ");
                stringBuilder.append(record);
                Log.d(str, stringBuilder.toString());
                this.mGlobalPrioritySession = record;
                if (user != null && user.mPriorityStack.contains(record)) {
                    user.mPriorityStack.removeSession(record);
                }
            }
        }
    }

    private List<MediaSessionRecord> getActiveSessionsLocked(int userId) {
        List<MediaSessionRecord> records = new ArrayList();
        if (userId == -1) {
            int size = this.mUserRecords.size();
            for (int i = 0; i < size; i++) {
                records.addAll(((FullUserRecord) this.mUserRecords.valueAt(i)).mPriorityStack.getActiveSessions(userId));
            }
        } else {
            FullUserRecord user = getFullUserRecordLocked(userId);
            if (user == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSessions failed. Unknown user ");
                stringBuilder.append(userId);
                Log.w(str, stringBuilder.toString());
                return records;
            }
            records.addAll(user.mPriorityStack.getActiveSessions(userId));
        }
        if (isGlobalPriorityActiveLocked() && (userId == -1 || userId == this.mGlobalPrioritySession.getUserId())) {
            if (records.contains(this.mGlobalPrioritySession)) {
                records.remove(this.mGlobalPrioritySession);
            }
            records.add(0, this.mGlobalPrioritySession);
        }
        return records;
    }

    public void notifyRemoteVolumeChanged(int flags, MediaSessionRecord session) {
        if (this.mRvc != null && session.isActive()) {
            try {
                this.mRvc.remoteVolumeChanged(session.getControllerBinder(), flags);
            } catch (Exception e) {
                Log.wtf(TAG, "Error sending volume change to system UI.", e);
            }
        }
    }

    public void onSessionPlaystateChanged(MediaSessionRecord record, int oldState, int newState) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (user == null || !user.mPriorityStack.contains(record)) {
                Log.d(TAG, "Unknown session changed playback state. Ignoring.");
                return;
            }
            user.mPriorityStack.onPlaystateChanged(record, oldState, newState);
        }
    }

    public void onSessionPlaybackTypeChanged(MediaSessionRecord record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            if (user == null || !user.mPriorityStack.contains(record)) {
                Log.d(TAG, "Unknown session changed playback type. Ignoring.");
                return;
            }
            pushRemoteVolumeUpdateLocked(record.getUserId());
        }
    }

    public void onStartUser(int userId) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onStartUser: ");
            stringBuilder.append(userId);
            Log.d(str, stringBuilder.toString());
        }
        updateUser();
    }

    public void onSwitchUser(int userId) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSwitchUser: ");
            stringBuilder.append(userId);
            Log.d(str, stringBuilder.toString());
        }
        updateUser();
    }

    public void onStopUser(int userId) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onStopUser: ");
            stringBuilder.append(userId);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(userId);
            if (user != null) {
                if (user.mFullUserId == userId) {
                    user.destroySessionsForUserLocked(-1);
                    this.mUserRecords.remove(userId);
                } else {
                    user.destroySessionsForUserLocked(userId);
                }
            }
            updateUser();
        }
    }

    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    protected void enforcePhoneStatePermission(int pid, int uid) {
        if (getContext().checkPermission("android.permission.MODIFY_PHONE_STATE", pid, uid) != 0) {
            throw new SecurityException("Must hold the MODIFY_PHONE_STATE permission.");
        }
    }

    void sessionDied(MediaSessionRecord session) {
        synchronized (this.mLock) {
            destroySessionLocked(session);
        }
    }

    void destroySession(MediaSessionRecord session) {
        synchronized (this.mLock) {
            destroySessionLocked(session);
        }
    }

    private void updateUser() {
        synchronized (this.mLock) {
            UserManager manager = (UserManager) getContext().getSystemService("user");
            this.mFullUserIds.clear();
            List<UserInfo> allUsers = manager.getUsers();
            if (allUsers != null) {
                for (UserInfo userInfo : allUsers) {
                    if (userInfo.isManagedProfile()) {
                        this.mFullUserIds.put(userInfo.id, userInfo.profileGroupId);
                    } else {
                        this.mFullUserIds.put(userInfo.id, userInfo.id);
                        if (this.mUserRecords.get(userInfo.id) == null) {
                            this.mUserRecords.put(userInfo.id, new FullUserRecord(userInfo.id));
                        }
                    }
                }
            }
            int currentFullUserId = ActivityManager.getCurrentUser();
            this.mCurrentFullUserRecord = (FullUserRecord) this.mUserRecords.get(currentFullUserId);
            if (this.mCurrentFullUserRecord == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot find FullUserInfo for the current user ");
                stringBuilder.append(currentFullUserId);
                Log.w(str, stringBuilder.toString());
                this.mCurrentFullUserRecord = new FullUserRecord(currentFullUserId);
                this.mUserRecords.put(currentFullUserId, this.mCurrentFullUserRecord);
            }
            this.mFullUserIds.put(currentFullUserId, currentFullUserId);
        }
    }

    private void updateActiveSessionListeners() {
        synchronized (this.mLock) {
            for (int i = this.mSessionsListeners.size() - 1; i >= 0; i--) {
                SessionsListenerRecord listener = (SessionsListenerRecord) this.mSessionsListeners.get(i);
                try {
                    enforceMediaPermissions(listener.mComponentName, listener.mPid, listener.mUid, listener.mUserId);
                } catch (SecurityException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ActiveSessionsListener ");
                    stringBuilder.append(listener.mComponentName);
                    stringBuilder.append(" is no longer authorized. Disconnecting.");
                    Log.i(str, stringBuilder.toString());
                    this.mSessionsListeners.remove(i);
                    try {
                        listener.mListener.onActiveSessionsChanged(new ArrayList());
                    } catch (Exception e2) {
                    }
                }
            }
        }
    }

    private void destroySessionLocked(MediaSessionRecord session) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Destroying ");
        stringBuilder.append(session);
        Log.d(str, stringBuilder.toString());
        FullUserRecord user = getFullUserRecordLocked(session.getUserId());
        if (this.mGlobalPrioritySession == session) {
            this.mGlobalPrioritySession = null;
            if (session.isActive() && user != null) {
                user.pushAddressedPlayerChangedLocked();
            }
        } else if (user != null) {
            user.mPriorityStack.removeSession(session);
        }
        try {
            session.getCallback().asBinder().unlinkToDeath(session, 0);
        } catch (Exception e) {
        }
        session.onDestroy();
        this.mHandler.postSessionsChanged(session.getUserId());
    }

    private void registerPackageBroadcastReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        filter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        getContext().registerReceiverAsUser(new BroadcastReceiver() {
            /* JADX WARNING: Missing block: B:10:0x0055, code:
            if (r3.equals("android.intent.action.PACKAGE_ADDED") != false) goto L_0x009f;
     */
            /* JADX WARNING: Missing block: B:34:0x00a3, code:
            if (r1 != false) goto L_0x00ab;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(Context context, Intent intent) {
                if (intent.getIntExtra("android.intent.extra.user_handle", -10000) == -10000) {
                    String str = MediaSessionService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Intent broadcast does not contain user handle: ");
                    stringBuilder.append(intent);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                String str2;
                boolean z = false;
                boolean isReplacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                if (MediaSessionService.DEBUG) {
                    str2 = MediaSessionService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Received change in packages, intent=");
                    stringBuilder2.append(intent);
                    Log.d(str2, stringBuilder2.toString());
                }
                str2 = intent.getAction();
                switch (str2.hashCode()) {
                    case -1403934493:
                        if (str2.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                            z = true;
                            break;
                        }
                    case -1338021860:
                        if (str2.equals("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE")) {
                            z = true;
                            break;
                        }
                    case -1001645458:
                        if (str2.equals("android.intent.action.PACKAGES_SUSPENDED")) {
                            z = true;
                            break;
                        }
                    case -810471698:
                        if (str2.equals("android.intent.action.PACKAGE_REPLACED")) {
                            z = true;
                            break;
                        }
                    case 172491798:
                        if (str2.equals("android.intent.action.PACKAGE_CHANGED")) {
                            z = true;
                            break;
                        }
                    case 525384130:
                        if (str2.equals("android.intent.action.PACKAGE_REMOVED")) {
                            z = true;
                            break;
                        }
                    case 1290767157:
                        if (str2.equals("android.intent.action.PACKAGES_UNSUSPENDED")) {
                            z = true;
                            break;
                        }
                    case 1544582882:
                        break;
                    default:
                        z = true;
                        break;
                }
                switch (z) {
                    case false:
                    case true:
                    case true:
                    case true:
                        break;
                    case true:
                    case true:
                    case true:
                    case true:
                        MediaSessionService.this.buildMediaSessionService2List();
                        break;
                }
            }
        }, UserHandle.ALL, filter, null, BackgroundThread.getHandler());
    }

    private void buildMediaSessionService2List() {
    }

    private void enforcePackageName(String packageName, int uid) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName may not be empty");
        }
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        int packageCount = packages.length;
        int i = 0;
        while (i < packageCount) {
            if (!packageName.equals(packages[i])) {
                i++;
            } else {
                return;
            }
        }
        throw new IllegalArgumentException("packageName is not owned by the calling process");
    }

    private void enforceMediaPermissions(ComponentName compName, int pid, int uid, int resolvedUserId) {
        if (!isCurrentVolumeController(pid, uid) && getContext().checkPermission("android.permission.MEDIA_CONTENT_CONTROL", pid, uid) != 0 && !isEnabledNotificationListener(compName, UserHandle.getUserId(uid), resolvedUserId)) {
            throw new SecurityException("Missing permission to control media.");
        }
    }

    private boolean isCurrentVolumeController(int pid, int uid) {
        return getContext().checkPermission("android.permission.STATUS_BAR_SERVICE", pid, uid) == 0;
    }

    private void enforceSystemUiPermission(String action, int pid, int uid) {
        if (!isCurrentVolumeController(pid, uid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Only system ui may ");
            stringBuilder.append(action);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private boolean isEnabledNotificationListener(ComponentName compName, int userId, int forUserId) {
        if (userId != forUserId) {
            return false;
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Checking if enabled notification listener ");
            stringBuilder.append(compName);
            Log.d(str, stringBuilder.toString());
        }
        if (compName != null) {
            try {
                return this.mNotificationManager.isNotificationListenerAccessGrantedForUser(compName, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Dead NotificationManager in isEnabledNotificationListener", e);
            }
        }
        return false;
    }

    private MediaSessionRecord createSessionInternal(int callerPid, int callerUid, int userId, String callerPackageName, ISessionCallback cb, String tag) throws RemoteException {
        MediaSessionRecord createSessionLocked;
        synchronized (this.mLock) {
            createSessionLocked = createSessionLocked(callerPid, callerUid, userId, callerPackageName, cb, tag);
        }
        return createSessionLocked;
    }

    private MediaSessionRecord createSessionLocked(int callerPid, int callerUid, int userId, String callerPackageName, ISessionCallback cb, String tag) {
        int i;
        String str;
        String str2;
        int i2 = userId;
        FullUserRecord user = getFullUserRecordLocked(i2);
        if (user != null) {
            MediaSessionRecord session = new MediaSessionRecord(callerPid, callerUid, i2, callerPackageName, cb, tag, this, this.mHandler.getLooper());
            try {
                cb.asBinder().linkToDeath(session, 0);
                user.mPriorityStack.addSession(session);
                this.mHandler.postSessionsChanged(i2);
                String str3 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Created session for ");
                stringBuilder.append(callerPackageName);
                stringBuilder.append(" with tag ");
                stringBuilder.append(tag);
                stringBuilder.append(" from pid");
                stringBuilder.append(callerPid);
                Log.d(str3, stringBuilder.toString());
                return session;
            } catch (RemoteException e) {
                i = callerPid;
                str = callerPackageName;
                str2 = tag;
                throw new RuntimeException("Media Session owner died prematurely.", e);
            }
        }
        i = callerPid;
        str = callerPackageName;
        str2 = tag;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Request from invalid user: ");
        stringBuilder2.append(i2);
        Log.wtf(TAG, stringBuilder2.toString());
        throw new RuntimeException("Session request from invalid user.");
    }

    private int findIndexOfSessionsListenerLocked(IActiveSessionsListener listener) {
        for (int i = this.mSessionsListeners.size() - 1; i >= 0; i--) {
            if (((SessionsListenerRecord) this.mSessionsListeners.get(i)).mListener.asBinder() == listener.asBinder()) {
                return i;
            }
        }
        return -1;
    }

    private void pushSessionsChanged(int userId) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(userId);
            if (user == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pushSessionsChanged failed. No user with id=");
                stringBuilder.append(userId);
                Log.w(str, stringBuilder.toString());
                return;
            }
            List<MediaSessionRecord> records = getActiveSessionsLocked(userId);
            int size = records.size();
            int i = 0;
            if (size > 0 && ((MediaSessionRecord) records.get(0)).isPlaybackActive()) {
                user.rememberMediaButtonReceiverLocked((MediaSessionRecord) records.get(0));
            } else if (user != null) {
                user.mLastMediaButtonReceiver = null;
            }
            user.pushAddressedPlayerChangedLocked();
            ArrayList<Token> tokens = new ArrayList();
            while (i < size) {
                tokens.add(new Token(((MediaSessionRecord) records.get(i)).getControllerBinder()));
                i++;
            }
            pushRemoteVolumeUpdateLocked(userId);
            for (i = this.mSessionsListeners.size() - 1; i >= 0; i--) {
                SessionsListenerRecord record = (SessionsListenerRecord) this.mSessionsListeners.get(i);
                if (record.mUserId == -1 || record.mUserId == userId) {
                    try {
                        record.mListener.onActiveSessionsChanged(tokens);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Dead ActiveSessionsListener in pushSessionsChanged, removing", e);
                        this.mSessionsListeners.remove(i);
                    }
                }
            }
        }
    }

    private void pushRemoteVolumeUpdateLocked(int userId) {
        if (this.mRvc != null) {
            try {
                FullUserRecord user = getFullUserRecordLocked(userId);
                if (user == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("pushRemoteVolumeUpdateLocked failed. No user with id=");
                    stringBuilder.append(userId);
                    Log.w(str, stringBuilder.toString());
                    return;
                }
                MediaSessionRecord record = user.mPriorityStack.getDefaultRemoteSession(userId);
                this.mRvc.updateRemoteController(record == null ? null : record.getControllerBinder());
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error sending default remote volume to sys ui.", e);
            }
        }
    }

    public void onMediaButtonReceiverChanged(MediaSessionRecord record) {
        synchronized (this.mLock) {
            FullUserRecord user = getFullUserRecordLocked(record.getUserId());
            MediaSessionRecord mediaButtonSession = user.mPriorityStack.getMediaButtonSession();
            if (record == mediaButtonSession) {
                user.rememberMediaButtonReceiverLocked(mediaButtonSession);
            }
        }
    }

    private String getCallingPackageName(int uid) {
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages == null || packages.length <= 0) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        return packages[0];
    }

    private void dispatchVolumeKeyLongPressLocked(KeyEvent keyEvent) {
        if (this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener != null) {
            try {
                this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener.onVolumeKeyLongPress(keyEvent);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to send ");
                stringBuilder.append(keyEvent);
                stringBuilder.append(" to volume key long-press listener");
                Log.w(str, stringBuilder.toString());
            }
        }
    }

    private FullUserRecord getFullUserRecordLocked(int userId) {
        int fullUserId = this.mFullUserIds.get(userId, -1);
        if (fullUserId < 0) {
            return null;
        }
        return (FullUserRecord) this.mUserRecords.get(fullUserId);
    }

    void destroySession2Internal(SessionToken2 token) {
        synchronized (this.mLock) {
            boolean notifySessionTokensUpdated;
            if (token.getType() == 0) {
                notifySessionTokensUpdated = false | removeSessionRecordLocked(token);
            } else {
                notifySessionTokensUpdated = false | addSessionRecordLocked(token);
            }
            if (notifySessionTokensUpdated) {
                postSessionTokensUpdated(UserHandle.getUserId(token.getUid()));
            }
        }
    }

    private void postSessionTokensUpdated(int userId) {
        this.mHandler.obtainMessage(3, Integer.valueOf(userId)).sendToTarget();
    }

    private void pushSessionTokensChanged(int userId) {
        synchronized (this.mLock) {
            List<Bundle> tokens = new ArrayList();
            for (SessionToken2 token : this.mSessionRecords.keySet()) {
                if (UserHandle.getUserId(token.getUid()) == userId || -1 == userId) {
                    tokens.add(token.toBundle());
                }
            }
            for (SessionTokensListenerRecord record : this.mSessionTokensListeners) {
                if (record.mUserId == userId || record.mUserId == -1) {
                    try {
                        record.mListener.onSessionTokensChanged(tokens);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failed to notify session tokens changed", e);
                    }
                }
            }
        }
    }

    private boolean addSessionRecordLocked(SessionToken2 token) {
        return addSessionRecordLocked(token, null);
    }

    private boolean addSessionRecordLocked(SessionToken2 token, MediaController2 controller) {
        if (this.mSessionRecords.containsKey(token) && this.mSessionRecords.get(token) == controller) {
            return false;
        }
        this.mSessionRecords.put(token, controller);
        return true;
    }

    private boolean removeSessionRecordLocked(SessionToken2 token) {
        if (!this.mSessionRecords.containsKey(token)) {
            return false;
        }
        this.mSessionRecords.remove(token);
        return true;
    }
}
