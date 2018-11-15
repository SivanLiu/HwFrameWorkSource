package com.android.server.tv;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.media.PlaybackParams;
import android.media.tv.DvbDeviceInfo;
import android.media.tv.ITvInputClient;
import android.media.tv.ITvInputHardware;
import android.media.tv.ITvInputHardwareCallback;
import android.media.tv.ITvInputManager.Stub;
import android.media.tv.ITvInputManagerCallback;
import android.media.tv.ITvInputService;
import android.media.tv.ITvInputServiceCallback;
import android.media.tv.ITvInputSession;
import android.media.tv.ITvInputSessionCallback;
import android.media.tv.TvContentRating;
import android.media.tv.TvContentRatingSystemInfo;
import android.media.tv.TvContract;
import android.media.tv.TvContract.WatchedPrograms;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputInfo.Builder;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputChannel;
import android.view.Surface;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TvInputManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String DVB_DIRECTORY = "/dev/dvb";
    private static final String TAG = "TvInputManagerService";
    private static final Pattern sAdapterDirPattern = Pattern.compile("^adapter([0-9]+)$");
    private static final Pattern sFrontEndDevicePattern = Pattern.compile("^dvb([0-9]+)\\.frontend([0-9]+)$");
    private static final Pattern sFrontEndInAdapterDirPattern = Pattern.compile("^frontend([0-9]+)$");
    private final Context mContext;
    private int mCurrentUserId = 0;
    private final Object mLock = new Object();
    private final TvInputHardwareManager mTvInputHardwareManager;
    private final SparseArray<UserState> mUserStates = new SparseArray();
    private final WatchLogHandler mWatchLogHandler;

    private final class BinderService extends Stub {
        private BinderService() {
        }

        /* synthetic */ BinderService(TvInputManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public List<TvInputInfo> getTvInputList(int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputList");
            long identity = Binder.clearCallingIdentity();
            try {
                List<TvInputInfo> inputList;
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    inputList = new ArrayList();
                    for (TvInputState state : userState.inputMap.values()) {
                        inputList.add(state.info);
                    }
                }
                Binder.restoreCallingIdentity(identity);
                return inputList;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public TvInputInfo getTvInputInfo(String inputId, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputInfo");
            long identity = Binder.clearCallingIdentity();
            try {
                TvInputInfo access$1400;
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputState state = (TvInputState) TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).inputMap.get(inputId);
                    access$1400 = state == null ? null : state.info;
                }
                Binder.restoreCallingIdentity(identity);
                return access$1400;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void updateTvInputInfo(TvInputInfo inputInfo, int userId) {
            String inputInfoPackageName = inputInfo.getServiceInfo().packageName;
            String callingPackageName = getCallingPackageName();
            if (TextUtils.equals(inputInfoPackageName, callingPackageName) || TvInputManagerService.this.mContext.checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
                int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "updateTvInputInfo");
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (TvInputManagerService.this.mLock) {
                        TvInputManagerService.this.updateTvInputInfoLocked(TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId), inputInfo);
                    }
                    Binder.restoreCallingIdentity(identity);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("calling package ");
                stringBuilder.append(callingPackageName);
                stringBuilder.append(" is not allowed to change TvInputInfo for ");
                stringBuilder.append(inputInfoPackageName);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        private String getCallingPackageName() {
            String[] packages = TvInputManagerService.this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            if (packages == null || packages.length <= 0) {
                return Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            return packages[0];
        }

        public int getTvInputState(String inputId, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputState");
            long identity = Binder.clearCallingIdentity();
            try {
                int access$4000;
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputState state = (TvInputState) TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).inputMap.get(inputId);
                    access$4000 = state == null ? 0 : state.state;
                }
                Binder.restoreCallingIdentity(identity);
                return access$4000;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<TvContentRatingSystemInfo> getTvContentRatingSystemList(int userId) {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.READ_CONTENT_RATING_SYSTEMS") == 0) {
                int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvContentRatingSystemList");
                long identity = Binder.clearCallingIdentity();
                try {
                    List<TvContentRatingSystemInfo> access$1500;
                    synchronized (TvInputManagerService.this.mLock) {
                        access$1500 = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).contentRatingSystemList;
                    }
                    Binder.restoreCallingIdentity(identity);
                    return access$1500;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                throw new SecurityException("The caller does not have permission to read content rating systems");
            }
        }

        public void sendTvInputNotifyIntent(Intent intent, int userId) {
            StringBuilder stringBuilder;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.NOTIFY_TV_INPUTS") != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("The caller: ");
                stringBuilder.append(getCallingPackageName());
                stringBuilder.append(" doesn't have permission: ");
                stringBuilder.append("android.permission.NOTIFY_TV_INPUTS");
                throw new SecurityException(stringBuilder.toString());
            } else if (TextUtils.isEmpty(intent.getPackage())) {
                throw new IllegalArgumentException("Must specify package name to notify.");
            } else {
                String action = intent.getAction();
                Object obj = -1;
                int hashCode = action.hashCode();
                if (hashCode != -160295064) {
                    if (hashCode != 1568780589) {
                        if (hashCode == 2011523553 && action.equals("android.media.tv.action.PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT")) {
                            obj = 2;
                        }
                    } else if (action.equals("android.media.tv.action.PREVIEW_PROGRAM_BROWSABLE_DISABLED")) {
                        obj = null;
                    }
                } else if (action.equals("android.media.tv.action.WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED")) {
                    obj = 1;
                }
                switch (obj) {
                    case null:
                        if (intent.getLongExtra("android.media.tv.extra.PREVIEW_PROGRAM_ID", -1) < 0) {
                            throw new IllegalArgumentException("Invalid preview program ID.");
                        }
                        break;
                    case 1:
                        if (intent.getLongExtra("android.media.tv.extra.WATCH_NEXT_PROGRAM_ID", -1) < 0) {
                            throw new IllegalArgumentException("Invalid watch next program ID.");
                        }
                        break;
                    case 2:
                        if (intent.getLongExtra("android.media.tv.extra.PREVIEW_PROGRAM_ID", -1) < 0) {
                            throw new IllegalArgumentException("Invalid preview program ID.");
                        } else if (intent.getLongExtra("android.media.tv.extra.WATCH_NEXT_PROGRAM_ID", -1) < 0) {
                            throw new IllegalArgumentException("Invalid watch next program ID.");
                        }
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid TV input notifying action: ");
                        stringBuilder.append(intent.getAction());
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
                int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "sendTvInputNotifyIntent");
                long identity = Binder.clearCallingIdentity();
                try {
                    TvInputManagerService.this.getContext().sendBroadcastAsUser(intent, new UserHandle(resolvedUserId));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void registerCallback(final ITvInputManagerCallback callback, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "registerCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    final UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    userState.callbackSet.add(callback);
                    try {
                        callback.asBinder().linkToDeath(new DeathRecipient() {
                            public void binderDied() {
                                synchronized (TvInputManagerService.this.mLock) {
                                    if (userState.callbackSet != null) {
                                        userState.callbackSet.remove(callback);
                                    }
                                }
                            }
                        }, 0);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unregisterCallback(ITvInputManagerCallback callback, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "unregisterCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).callbackSet.remove(callback);
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isParentalControlsEnabled(int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "isParentalControlsEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                boolean isParentalControlsEnabled;
                synchronized (TvInputManagerService.this.mLock) {
                    isParentalControlsEnabled = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).persistentDataStore.isParentalControlsEnabled();
                }
                Binder.restoreCallingIdentity(identity);
                return isParentalControlsEnabled;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setParentalControlsEnabled(boolean enabled, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "setParentalControlsEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).persistentDataStore.setParentalControlsEnabled(enabled);
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isRatingBlocked(String rating, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "isRatingBlocked");
            long identity = Binder.clearCallingIdentity();
            try {
                boolean isRatingBlocked;
                synchronized (TvInputManagerService.this.mLock) {
                    isRatingBlocked = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).persistentDataStore.isRatingBlocked(TvContentRating.unflattenFromString(rating));
                }
                Binder.restoreCallingIdentity(identity);
                return isRatingBlocked;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<String> getBlockedRatings(int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getBlockedRatings");
            long identity = Binder.clearCallingIdentity();
            try {
                List<String> ratings;
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    ratings = new ArrayList();
                    for (TvContentRating rating : userState.persistentDataStore.getBlockedRatings()) {
                        ratings.add(rating.flattenToString());
                    }
                }
                Binder.restoreCallingIdentity(identity);
                return ratings;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void addBlockedRating(String rating, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "addBlockedRating");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).persistentDataStore.addBlockedRating(TvContentRating.unflattenFromString(rating));
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeBlockedRating(String rating, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "removeBlockedRating");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).persistentDataStore.removeBlockedRating(TvContentRating.unflattenFromString(rating));
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void ensureParentalControlsPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.MODIFY_PARENTAL_CONTROLS") != 0) {
                throw new SecurityException("The caller does not have parental controls permission");
            }
        }

        /* JADX WARNING: Missing block: B:52:0x011c, code:
            android.os.Binder.restoreCallingIdentity(r23);
     */
        /* JADX WARNING: Missing block: B:53:0x0122, code:
            return;
     */
        /* JADX WARNING: Missing block: B:62:0x012d, code:
            r0 = th;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void createSession(ITvInputClient client, String inputId, boolean isRecordingSession, int seq, int userId) {
            Throwable th;
            long identity;
            int i;
            String str = inputId;
            int i2 = userId;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i2, "createSession");
            long identity2 = Binder.clearCallingIdentity();
            int resolvedUserId2;
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        if (i2 == TvInputManagerService.this.mCurrentUserId || isRecordingSession) {
                            UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                            TvInputState inputState = (TvInputState) userState.inputMap.get(str);
                            if (inputState == null) {
                                String str2 = TvInputManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to find input state for inputId=");
                                stringBuilder.append(str);
                                Slog.w(str2, stringBuilder.toString());
                                TvInputManagerService.this.sendSessionTokenToClientLocked(client, str, null, null, seq);
                                Binder.restoreCallingIdentity(identity2);
                                return;
                            }
                            TvInputInfo info = inputState.info;
                            ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(info.getComponent());
                            if (serviceState == null) {
                                serviceState = new ServiceState(TvInputManagerService.this, info.getComponent(), resolvedUserId, null);
                                userState.serviceStateMap.put(info.getComponent(), serviceState);
                            }
                            ServiceState serviceState2 = serviceState;
                            if (serviceState2.reconnecting) {
                                String str3 = str;
                                TvInputManagerService.this.sendSessionTokenToClientLocked(client, str3, null, null, seq);
                                Binder.restoreCallingIdentity(identity2);
                                return;
                            }
                            ServiceState serviceState3 = serviceState2;
                            IBinder sessionToken = new Binder();
                            TvInputManagerService tvInputManagerService = TvInputManagerService.this;
                            String id = info.getId();
                            ComponentName component = info.getComponent();
                            TvInputInfo info2 = info;
                            SessionState sessionState = sessionState;
                            TvInputManagerService tvInputManagerService2 = tvInputManagerService;
                            long identity3 = identity2;
                            resolvedUserId2 = resolvedUserId;
                            try {
                                userState.sessionStateMap.put(sessionToken, new SessionState(tvInputManagerService2, sessionToken, id, component, isRecordingSession, client, seq, callingUid, resolvedUserId2, null));
                                serviceState3.sessionTokens.add(sessionToken);
                                if (serviceState3.service != null) {
                                    try {
                                        TvInputManagerService.this.createSessionInternalLocked(serviceState3.service, sessionToken, resolvedUserId2);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        identity = identity3;
                                    }
                                } else {
                                    TvInputManagerService.this.updateServiceConnectionLocked(info2.getComponent(), resolvedUserId2);
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                identity = identity3;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                }
                                throw th;
                            }
                        } else {
                            try {
                                TvInputManagerService.this.sendSessionTokenToClientLocked(client, str, null, null, seq);
                                Binder.restoreCallingIdentity(identity2);
                            } catch (Throwable th5) {
                                th = th5;
                                identity = identity2;
                                resolvedUserId2 = resolvedUserId;
                                i = callingUid;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        identity = identity2;
                        resolvedUserId2 = resolvedUserId;
                        i = callingUid;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } catch (Throwable th7) {
                th = th7;
                identity = identity2;
                resolvedUserId2 = resolvedUserId;
                i = callingUid;
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }

        public void releaseSession(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "releaseSession");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.releaseSessionLocked(sessionToken, callingUid, resolvedUserId);
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Missing block: B:19:0x0055, code:
            android.os.Binder.restoreCallingIdentity(r2);
     */
        /* JADX WARNING: Missing block: B:20:0x0059, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setMainSession(IBinder sessionToken, int userId) {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CHANGE_HDMI_CEC_ACTIVE_SOURCE") == 0) {
                int callingUid = Binder.getCallingUid();
                int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setMainSession");
                long identity = Binder.clearCallingIdentity();
                try {
                    synchronized (TvInputManagerService.this.mLock) {
                        UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                        if (userState.mainSessionToken == sessionToken) {
                            Binder.restoreCallingIdentity(identity);
                            return;
                        }
                        IBinder oldMainSessionToken = userState.mainSessionToken;
                        userState.mainSessionToken = sessionToken;
                        if (sessionToken != null) {
                            TvInputManagerService.this.setMainLocked(sessionToken, true, callingUid, userId);
                        }
                        if (oldMainSessionToken != null) {
                            TvInputManagerService.this.setMainLocked(oldMainSessionToken, false, 1000, userId);
                        }
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                throw new SecurityException("The caller does not have CHANGE_HDMI_CEC_ACTIVE_SOURCE permission");
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:10:0x0044 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:10:0x0044, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:12:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in setSurface", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setSurface(IBinder sessionToken, Surface surface, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setSurface");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        if (sessionState.hardwareSessionToken == null) {
                            TvInputManagerService.this.getSessionLocked(sessionState).setSurface(surface);
                        } else {
                            TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId).setSurface(surface);
                        }
                    } catch (Exception e) {
                    }
                }
                if (surface != null) {
                    surface.release();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                if (surface != null) {
                    surface.release();
                }
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:9:0x0042 A:{Splitter: B:4:0x001b, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:9:0x0042, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:11:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in dispatchSurfaceChanged", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void dispatchSurfaceChanged(IBinder sessionToken, int format, int width, int height, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "dispatchSurfaceChanged");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).dispatchSurfaceChanged(format, width, height);
                        if (sessionState.hardwareSessionToken != null) {
                            TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId).dispatchSurfaceChanged(format, width, height);
                        }
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x004e A:{Splitter: B:4:0x001f, ExcHandler: android.os.RemoteException (r7_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:12:0x004e, code:
            r7 = move-exception;
     */
        /* JADX WARNING: Missing block: B:14:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in setVolume", r7);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setVolume(IBinder sessionToken, float volume, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setVolume");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).setVolume(volume);
                        if (sessionState.hardwareSessionToken != null) {
                            ITvInputSession access$5400 = TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId);
                            float f = 0.0f;
                            if (volume > 0.0f) {
                                f = 1.0f;
                            }
                            access$5400.setVolume(f);
                        }
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:21:0x0082 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_6 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:21:0x0082, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:23:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in tune", r5);
     */
        /* JADX WARNING: Missing block: B:25:0x008b, code:
            android.os.Binder.restoreCallingIdentity(r2);
     */
        /* JADX WARNING: Missing block: B:26:0x008f, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void tune(IBinder sessionToken, Uri channelUri, Bundle params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "tune");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).tune(channelUri, params);
                        if (TvContract.isChannelUriForPassthroughInput(channelUri)) {
                            Binder.restoreCallingIdentity(identity);
                            return;
                        }
                        SessionState sessionState = (SessionState) TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId).sessionStateMap.get(sessionToken);
                        if (sessionState.isRecordingSession) {
                            Binder.restoreCallingIdentity(identity);
                            return;
                        }
                        SomeArgs args = SomeArgs.obtain();
                        args.arg1 = sessionState.componentName.getPackageName();
                        args.arg2 = Long.valueOf(System.currentTimeMillis());
                        args.arg3 = Long.valueOf(ContentUris.parseId(channelUri));
                        args.arg4 = params;
                        args.arg5 = sessionToken;
                        TvInputManagerService.this.mWatchLogHandler.obtainMessage(1, args).sendToTarget();
                    } catch (Exception e) {
                    }
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x002b A:{Splitter: B:4:0x001f, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x002b, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in unblockContent", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void unblockContent(IBinder sessionToken, String unblockedRating, int userId) {
            ensureParentalControlsPermission();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "unblockContent");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).unblockContent(unblockedRating);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in setCaptionEnabled", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setCaptionEnabled(IBinder sessionToken, boolean enabled, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setCaptionEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).setCaptionEnabled(enabled);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in selectTrack", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void selectTrack(IBinder sessionToken, int type, String trackId, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "selectTrack");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).selectTrack(type, trackId);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in appPrivateCommand", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void sendAppPrivateCommand(IBinder sessionToken, String command, Bundle data, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendAppPrivateCommand");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).appPrivateCommand(command, data);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0027 A:{Splitter: B:4:0x001b, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0027, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in createOverlayView", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void createOverlayView(IBinder sessionToken, IBinder windowToken, Rect frame, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "createOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).createOverlayView(windowToken, frame);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in relayoutOverlayView", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void relayoutOverlayView(IBinder sessionToken, Rect frame, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "relayoutOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).relayoutOverlayView(frame);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in removeOverlayView", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void removeOverlayView(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "removeOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).removeOverlayView();
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in timeShiftPlay", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void timeShiftPlay(IBinder sessionToken, Uri recordedProgramUri, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftPlay");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftPlay(recordedProgramUri);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in timeShiftPause", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void timeShiftPause(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftPause");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftPause();
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in timeShiftResume", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void timeShiftResume(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftResume");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftResume();
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in timeShiftSeekTo", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void timeShiftSeekTo(IBinder sessionToken, long timeMs, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftSeekTo");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftSeekTo(timeMs);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in timeShiftSetPlaybackParams", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void timeShiftSetPlaybackParams(IBinder sessionToken, PlaybackParams params, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftSetPlaybackParams");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftSetPlaybackParams(params);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in timeShiftEnablePositionTracking", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void timeShiftEnablePositionTracking(IBinder sessionToken, boolean enable, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "timeShiftEnablePositionTracking");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).timeShiftEnablePositionTracking(enable);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in startRecording", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void startRecording(IBinder sessionToken, Uri programUri, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "startRecording");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).startRecording(programUri);
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:7:0x0028 A:{Splitter: B:4:0x001c, ExcHandler: android.os.RemoteException (r5_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:7:0x0028, code:
            r5 = move-exception;
     */
        /* JADX WARNING: Missing block: B:9:?, code:
            android.util.Slog.e(com.android.server.tv.TvInputManagerService.TAG, "error in stopRecording", r5);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void stopRecording(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "stopRecording");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).stopRecording();
                    } catch (Exception e) {
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<TvInputHardwareInfo> getHardwareList() throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                List<TvInputHardwareInfo> hardwareList = TvInputManagerService.this.mTvInputHardwareManager.getHardwareList();
                return hardwareList;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public ITvInputHardware acquireTvInputHardware(int deviceId, ITvInputHardwareCallback callback, TvInputInfo info, int userId) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            try {
                ITvInputHardware acquireHardware = TvInputManagerService.this.mTvInputHardwareManager.acquireHardware(deviceId, callback, info, callingUid, TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "acquireTvInputHardware"));
                return acquireHardware;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void releaseTvInputHardware(int deviceId, ITvInputHardware hardware, int userId) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") == 0) {
                long identity = Binder.clearCallingIdentity();
                int callingUid = Binder.getCallingUid();
                try {
                    TvInputManagerService.this.mTvInputHardwareManager.releaseHardware(deviceId, hardware, callingUid, TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "releaseTvInputHardware"));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public List<DvbDeviceInfo> getDvbDeviceList() throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.DVB_DEVICE") == 0) {
                long identity = Binder.clearCallingIdentity();
                try {
                    int i;
                    ArrayList<DvbDeviceInfo> deviceInfosFromPattern1 = new ArrayList();
                    File devDirectory = new File("/dev");
                    String[] list = devDirectory.list();
                    int length = list.length;
                    boolean dvbDirectoryFound = false;
                    List<DvbDeviceInfo> dvbDirectoryFound2 = null;
                    while (true) {
                        i = 1;
                        if (dvbDirectoryFound2 >= length) {
                            break;
                        }
                        String fileName = list[dvbDirectoryFound2];
                        Matcher matcher = TvInputManagerService.sFrontEndDevicePattern.matcher(fileName);
                        if (matcher.find()) {
                            deviceInfosFromPattern1.add(new DvbDeviceInfo(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
                        }
                        if (TextUtils.equals("dvb", fileName)) {
                            dvbDirectoryFound = true;
                        }
                        dvbDirectoryFound2++;
                    }
                    if (dvbDirectoryFound) {
                        File dvbDirectory;
                        List<DvbDeviceInfo> unmodifiableList;
                        File dvbDirectory2 = new File(TvInputManagerService.DVB_DIRECTORY);
                        ArrayList<DvbDeviceInfo> deviceInfosFromPattern2 = new ArrayList();
                        String[] list2 = dvbDirectory2.list();
                        int length2 = list2.length;
                        int i2 = 0;
                        while (i2 < length2) {
                            String fileNameInDvb = list2[i2];
                            Matcher adapterMatcher = TvInputManagerService.sAdapterDirPattern.matcher(fileNameInDvb);
                            if (adapterMatcher.find()) {
                                int adapterId = Integer.parseInt(adapterMatcher.group(i));
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("/dev/dvb/");
                                stringBuilder.append(fileNameInDvb);
                                File adapterDirectory = new File(stringBuilder.toString());
                                String[] list3 = adapterDirectory.list();
                                i = list3.length;
                                int adapterDirectory2 = 0;
                                while (adapterDirectory2 < i) {
                                    String[] strArr;
                                    File devDirectory2 = devDirectory;
                                    dvbDirectory = dvbDirectory2;
                                    String fileNameInAdapter = list3[adapterDirectory2];
                                    Matcher frontendMatcher = TvInputManagerService.sFrontEndInAdapterDirPattern.matcher(fileNameInAdapter);
                                    if (frontendMatcher.find()) {
                                        strArr = list2;
                                        deviceInfosFromPattern2.add(new DvbDeviceInfo(adapterId, Integer.parseInt(frontendMatcher.group(1))));
                                    } else {
                                        strArr = list2;
                                    }
                                    adapterDirectory2++;
                                    devDirectory = devDirectory2;
                                    dvbDirectory2 = dvbDirectory;
                                    list2 = strArr;
                                }
                            }
                            i2++;
                            devDirectory = devDirectory;
                            dvbDirectory2 = dvbDirectory2;
                            list2 = list2;
                            i = 1;
                        }
                        dvbDirectory = dvbDirectory2;
                        if (deviceInfosFromPattern2.isEmpty()) {
                            unmodifiableList = Collections.unmodifiableList(deviceInfosFromPattern1);
                        } else {
                            unmodifiableList = Collections.unmodifiableList(deviceInfosFromPattern2);
                        }
                        Binder.restoreCallingIdentity(identity);
                        return unmodifiableList;
                    }
                    dvbDirectoryFound2 = Collections.unmodifiableList(deviceInfosFromPattern1);
                    return dvbDirectoryFound2;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            } else {
                throw new SecurityException("Requires DVB_DEVICE permission");
            }
        }

        public ParcelFileDescriptor openDvbDevice(DvbDeviceInfo info, int device) throws RemoteException {
            int i = device;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.DVB_DEVICE") == 0) {
                String deviceFileName;
                File devDirectory = new File("/dev");
                String[] list = devDirectory.list();
                int length = list.length;
                boolean dvbDeviceFound = false;
                int dvbDeviceFound2 = 0;
                while (dvbDeviceFound2 < length) {
                    File devDirectory2;
                    String[] strArr;
                    if (TextUtils.equals("dvb", list[dvbDeviceFound2])) {
                        String[] list2 = new File(TvInputManagerService.DVB_DIRECTORY).list();
                        int length2 = list2.length;
                        boolean dvbDeviceFound3 = dvbDeviceFound;
                        int dvbDeviceFound4 = 0;
                        while (dvbDeviceFound4 < length2) {
                            String fileNameInDvb = list2[dvbDeviceFound4];
                            if (TvInputManagerService.sAdapterDirPattern.matcher(fileNameInDvb).find()) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("/dev/dvb/");
                                stringBuilder.append(fileNameInDvb);
                                File adapterDirectory = new File(stringBuilder.toString());
                                String[] list3 = adapterDirectory.list();
                                boolean length3 = list3.length;
                                boolean dvbDeviceFound5 = false;
                                while (dvbDeviceFound5 < length3) {
                                    devDirectory2 = devDirectory;
                                    strArr = list;
                                    if (TvInputManagerService.sFrontEndInAdapterDirPattern.matcher(list3[dvbDeviceFound5]).find()) {
                                        dvbDeviceFound3 = true;
                                        break;
                                    }
                                    dvbDeviceFound5++;
                                    devDirectory = devDirectory2;
                                    list = strArr;
                                }
                            }
                            devDirectory2 = devDirectory;
                            strArr = list;
                            if (dvbDeviceFound3) {
                                dvbDeviceFound = dvbDeviceFound3;
                                break;
                            }
                            dvbDeviceFound4++;
                            devDirectory = devDirectory2;
                            list = strArr;
                        }
                        devDirectory2 = devDirectory;
                        strArr = list;
                        dvbDeviceFound = dvbDeviceFound3;
                    } else {
                        devDirectory2 = devDirectory;
                        strArr = list;
                    }
                    if (dvbDeviceFound) {
                        break;
                    }
                    dvbDeviceFound2++;
                    devDirectory = devDirectory2;
                    list = strArr;
                }
                long identity = Binder.clearCallingIdentity();
                switch (i) {
                    case 0:
                        deviceFileName = String.format(dvbDeviceFound ? "/dev/dvb/adapter%d/demux%d" : "/dev/dvb%d.demux%d", new Object[]{Integer.valueOf(info.getAdapterId()), Integer.valueOf(info.getDeviceId())});
                        break;
                    case 1:
                        deviceFileName = String.format(dvbDeviceFound ? "/dev/dvb/adapter%d/dvr%d" : "/dev/dvb%d.dvr%d", new Object[]{Integer.valueOf(info.getAdapterId()), Integer.valueOf(info.getDeviceId())});
                        break;
                    case 2:
                        deviceFileName = String.format(dvbDeviceFound ? "/dev/dvb/adapter%d/frontend%d" : "/dev/dvb%d.frontend%d", new Object[]{Integer.valueOf(info.getAdapterId()), Integer.valueOf(info.getDeviceId())});
                        break;
                    default:
                        try {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Invalid DVB device: ");
                            stringBuilder2.append(i);
                            throw new IllegalArgumentException(stringBuilder2.toString());
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(identity);
                        }
                }
                try {
                    File file = new File(deviceFileName);
                    if (2 == i) {
                        dvbDeviceFound2 = 805306368;
                    } else {
                        dvbDeviceFound2 = 268435456;
                    }
                    ParcelFileDescriptor open = ParcelFileDescriptor.open(file, dvbDeviceFound2);
                    Binder.restoreCallingIdentity(identity);
                    return open;
                } catch (FileNotFoundException e) {
                    Binder.restoreCallingIdentity(identity);
                    return null;
                }
            }
            throw new SecurityException("Requires DVB_DEVICE permission");
        }

        public List<TvStreamConfig> getAvailableTvStreamConfigList(String inputId, int userId) throws RemoteException {
            ensureCaptureTvInputPermission();
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            try {
                List<TvStreamConfig> availableTvStreamConfigList = TvInputManagerService.this.mTvInputHardwareManager.getAvailableTvStreamConfigList(inputId, callingUid, TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "getAvailableTvStreamConfigList"));
                return availableTvStreamConfigList;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Missing block: B:25:?, code:
            r2 = com.android.server.tv.TvInputManagerService.access$5600(r11.this$0);
     */
        /* JADX WARNING: Missing block: B:26:0x0091, code:
            if (r10 == null) goto L_0x0095;
     */
        /* JADX WARNING: Missing block: B:27:0x0093, code:
            r3 = r10;
     */
        /* JADX WARNING: Missing block: B:28:0x0095, code:
            r3 = r12;
     */
        /* JADX WARNING: Missing block: B:29:0x0096, code:
            r2 = r2.captureFrame(r3, r13, r14, r8, r9);
     */
        /* JADX WARNING: Missing block: B:30:0x009e, code:
            android.os.Binder.restoreCallingIdentity(r0);
     */
        /* JADX WARNING: Missing block: B:31:0x00a1, code:
            return r2;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean captureFrame(String inputId, Surface surface, TvStreamConfig config, int userId) throws RemoteException {
            Throwable th;
            ensureCaptureTvInputPermission();
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "captureFrame");
            String hardwareInputId = null;
            try {
                boolean z;
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                        Object obj = userState.inputMap.get(inputId);
                        if (obj == null) {
                            String str = TvInputManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("input not found for ");
                            stringBuilder.append(inputId);
                            Slog.e(str, stringBuilder.toString());
                            z = false;
                        } else {
                            z = userState.sessionStateMap.values().iterator();
                            while (z.hasNext()) {
                                SessionState sessionState = (SessionState) z.next();
                                if (sessionState.inputId.equals(inputId) && sessionState.hardwareSessionToken != null) {
                                    z = ((SessionState) userState.sessionStateMap.get(sessionState.hardwareSessionToken)).inputId;
                                    hardwareInputId = z;
                                    break;
                                }
                            }
                            String hardwareInputId2 = hardwareInputId;
                            try {
                            } catch (Throwable th2) {
                                th = th2;
                                hardwareInputId = hardwareInputId2;
                                throw th;
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                }
                return z;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX WARNING: Missing block: B:18:0x0062, code:
            android.os.Binder.restoreCallingIdentity(r0);
     */
        /* JADX WARNING: Missing block: B:19:0x0065, code:
            return true;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean isSingleSessionActive(int userId) throws RemoteException {
            ensureCaptureTvInputPermission();
            long identity = Binder.clearCallingIdentity();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "isSingleSessionActive");
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(resolvedUserId);
                    if (userState.sessionStateMap.size() == 1) {
                        Binder.restoreCallingIdentity(identity);
                        return true;
                    }
                    if (userState.sessionStateMap.size() == 2) {
                        SessionState[] sessionStates = (SessionState[]) userState.sessionStateMap.values().toArray(new SessionState[2]);
                        if (!(sessionStates[0].hardwareSessionToken == null && sessionStates[1].hardwareSessionToken == null)) {
                        }
                    }
                    Binder.restoreCallingIdentity(identity);
                    return false;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void ensureCaptureTvInputPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_TV_INPUT") != 0) {
                throw new SecurityException("Requires CAPTURE_TV_INPUT permission");
            }
        }

        public void requestChannelBrowsable(Uri channelUri, int userId) throws RemoteException {
            String callingPackageName = getCallingPackageName();
            long identity = Binder.clearCallingIdentity();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "requestChannelBrowsable");
            try {
                Intent intent = new Intent("android.media.tv.action.CHANNEL_BROWSABLE_REQUESTED");
                List<ResolveInfo> list = TvInputManagerService.this.getContext().getPackageManager().queryBroadcastReceivers(intent, 0);
                if (list != null) {
                    for (ResolveInfo info : list) {
                        String receiverPackageName = info.activityInfo.packageName;
                        intent.putExtra("android.media.tv.extra.CHANNEL_ID", ContentUris.parseId(channelUri));
                        intent.putExtra("android.media.tv.extra.PACKAGE_NAME", callingPackageName);
                        intent.setPackage(receiverPackageName);
                        TvInputManagerService.this.getContext().sendBroadcastAsUser(intent, new UserHandle(resolvedUserId));
                    }
                }
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            if (DumpUtils.checkDumpPermission(TvInputManagerService.this.mContext, TvInputManagerService.TAG, pw)) {
                synchronized (TvInputManagerService.this.mLock) {
                    int i;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("User Ids (Current user: ");
                    stringBuilder.append(TvInputManagerService.this.mCurrentUserId);
                    stringBuilder.append("):");
                    pw.println(stringBuilder.toString());
                    pw.increaseIndent();
                    int i2 = 0;
                    for (i = 0; i < TvInputManagerService.this.mUserStates.size(); i++) {
                        pw.println(Integer.valueOf(TvInputManagerService.this.mUserStates.keyAt(i)));
                    }
                    pw.decreaseIndent();
                    while (i2 < TvInputManagerService.this.mUserStates.size()) {
                        StringBuilder stringBuilder2;
                        StringBuilder stringBuilder3;
                        i = TvInputManagerService.this.mUserStates.keyAt(i2);
                        UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(i);
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("UserState (");
                        stringBuilder4.append(i);
                        stringBuilder4.append("):");
                        pw.println(stringBuilder4.toString());
                        pw.increaseIndent();
                        pw.println("inputMap: inputId -> TvInputState");
                        pw.increaseIndent();
                        for (Entry<String, TvInputState> entry : userState.inputMap.entrySet()) {
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append((String) entry.getKey());
                            stringBuilder5.append(": ");
                            stringBuilder5.append(entry.getValue());
                            pw.println(stringBuilder5.toString());
                        }
                        pw.decreaseIndent();
                        pw.println("packageSet:");
                        pw.increaseIndent();
                        for (String packageName : userState.packageSet) {
                            pw.println(packageName);
                        }
                        pw.decreaseIndent();
                        pw.println("clientStateMap: ITvInputClient -> ClientState");
                        pw.increaseIndent();
                        for (Entry<IBinder, ClientState> entry2 : userState.clientStateMap.entrySet()) {
                            ClientState client = (ClientState) entry2.getValue();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(entry2.getKey());
                            stringBuilder2.append(": ");
                            stringBuilder2.append(client);
                            pw.println(stringBuilder2.toString());
                            pw.increaseIndent();
                            pw.println("sessionTokens:");
                            pw.increaseIndent();
                            for (IBinder token : client.sessionTokens) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                stringBuilder3.append(token);
                                pw.println(stringBuilder3.toString());
                            }
                            pw.decreaseIndent();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("clientTokens: ");
                            stringBuilder2.append(client.clientToken);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("userId: ");
                            stringBuilder2.append(client.userId);
                            pw.println(stringBuilder2.toString());
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("serviceStateMap: ComponentName -> ServiceState");
                        pw.increaseIndent();
                        for (Entry<ComponentName, ServiceState> entry3 : userState.serviceStateMap.entrySet()) {
                            ServiceState service = (ServiceState) entry3.getValue();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(entry3.getKey());
                            stringBuilder2.append(": ");
                            stringBuilder2.append(service);
                            pw.println(stringBuilder2.toString());
                            pw.increaseIndent();
                            pw.println("sessionTokens:");
                            pw.increaseIndent();
                            for (IBinder token2 : service.sessionTokens) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                stringBuilder3.append(token2);
                                pw.println(stringBuilder3.toString());
                            }
                            pw.decreaseIndent();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("service: ");
                            stringBuilder2.append(service.service);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("callback: ");
                            stringBuilder2.append(service.callback);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("bound: ");
                            stringBuilder2.append(service.bound);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("reconnecting: ");
                            stringBuilder2.append(service.reconnecting);
                            pw.println(stringBuilder2.toString());
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("sessionStateMap: ITvInputSession -> SessionState");
                        pw.increaseIndent();
                        for (Entry<IBinder, SessionState> entry4 : userState.sessionStateMap.entrySet()) {
                            SessionState session = (SessionState) entry4.getValue();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(entry4.getKey());
                            stringBuilder2.append(": ");
                            stringBuilder2.append(session);
                            pw.println(stringBuilder2.toString());
                            pw.increaseIndent();
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("inputId: ");
                            stringBuilder2.append(session.inputId);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("client: ");
                            stringBuilder2.append(session.client);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("seq: ");
                            stringBuilder2.append(session.seq);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("callingUid: ");
                            stringBuilder2.append(session.callingUid);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("userId: ");
                            stringBuilder2.append(session.userId);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("sessionToken: ");
                            stringBuilder2.append(session.sessionToken);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("session: ");
                            stringBuilder2.append(session.session);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("logUri: ");
                            stringBuilder2.append(session.logUri);
                            pw.println(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("hardwareSessionToken: ");
                            stringBuilder2.append(session.hardwareSessionToken);
                            pw.println(stringBuilder2.toString());
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("callbackSet:");
                        pw.increaseIndent();
                        for (ITvInputManagerCallback callback : userState.callbackSet) {
                            pw.println(callback.toString());
                        }
                        pw.decreaseIndent();
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("mainSessionToken: ");
                        stringBuilder4.append(userState.mainSessionToken);
                        pw.println(stringBuilder4.toString());
                        pw.decreaseIndent();
                        i2++;
                    }
                }
                TvInputManagerService.this.mTvInputHardwareManager.dump(fd, writer, args);
            }
        }
    }

    private final class ClientState implements DeathRecipient {
        private IBinder clientToken;
        private final List<IBinder> sessionTokens = new ArrayList();
        private final int userId;

        ClientState(IBinder clientToken, int userId) {
            this.clientToken = clientToken;
            this.userId = userId;
        }

        public boolean isEmpty() {
            return this.sessionTokens.isEmpty();
        }

        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                ClientState clientState = (ClientState) TvInputManagerService.this.getOrCreateUserStateLocked(this.userId).clientStateMap.get(this.clientToken);
                if (clientState != null) {
                    while (clientState.sessionTokens.size() > 0) {
                        TvInputManagerService.this.releaseSessionLocked((IBinder) clientState.sessionTokens.get(0), 1000, this.userId);
                    }
                }
                this.clientToken = null;
            }
        }
    }

    private final class InputServiceConnection implements ServiceConnection {
        private final ComponentName mComponent;
        private final int mUserId;

        /* synthetic */ InputServiceConnection(TvInputManagerService x0, ComponentName x1, int x2, AnonymousClass1 x3) {
            this(x1, x2);
        }

        private InputServiceConnection(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        /* JADX WARNING: Missing block: B:54:0x0126, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onServiceConnected(ComponentName component, IBinder service) {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = (UserState) TvInputManagerService.this.mUserStates.get(this.mUserId);
                if (userState == null) {
                    TvInputManagerService.this.mContext.unbindService(this);
                    return;
                }
                ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(this.mComponent);
                serviceState.service = ITvInputService.Stub.asInterface(service);
                if (serviceState.isHardware && serviceState.callback == null) {
                    serviceState.callback = new ServiceCallback(this.mComponent, this.mUserId);
                    try {
                        serviceState.service.registerCallback(serviceState.callback);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in registerCallback", e);
                    }
                }
                for (IBinder sessionToken : serviceState.sessionTokens) {
                    TvInputManagerService.this.createSessionInternalLocked(serviceState.service, sessionToken, this.mUserId);
                }
                for (TvInputState inputState : userState.inputMap.values()) {
                    if (inputState.info.getComponent().equals(component) && inputState.state != 0) {
                        TvInputManagerService.this.notifyInputStateChangedLocked(userState, inputState.info.getId(), inputState.state, null);
                    }
                }
                if (serviceState.isHardware) {
                    serviceState.hardwareInputMap.clear();
                    for (TvInputHardwareInfo hardware : TvInputManagerService.this.mTvInputHardwareManager.getHardwareList()) {
                        try {
                            serviceState.service.notifyHardwareAdded(hardware);
                        } catch (RemoteException e2) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e2);
                        }
                    }
                    for (HdmiDeviceInfo device : TvInputManagerService.this.mTvInputHardwareManager.getHdmiDeviceList()) {
                        try {
                            serviceState.service.notifyHdmiDeviceAdded(device);
                        } catch (RemoteException e22) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e22);
                        }
                    }
                }
            }
        }

        public void onServiceDisconnected(ComponentName component) {
            if (this.mComponent.equals(component)) {
                synchronized (TvInputManagerService.this.mLock) {
                    ServiceState serviceState = (ServiceState) TvInputManagerService.this.getOrCreateUserStateLocked(this.mUserId).serviceStateMap.get(this.mComponent);
                    if (serviceState != null) {
                        serviceState.reconnecting = true;
                        serviceState.bound = false;
                        serviceState.service = null;
                        serviceState.callback = null;
                        TvInputManagerService.this.abortPendingCreateSessionRequestsLocked(serviceState, null, this.mUserId);
                    }
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Mismatched ComponentName: ");
            stringBuilder.append(this.mComponent);
            stringBuilder.append(" (expected), ");
            stringBuilder.append(component);
            stringBuilder.append(" (actual).");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private final class ServiceCallback extends ITvInputServiceCallback.Stub {
        private final ComponentName mComponent;
        private final int mUserId;

        ServiceCallback(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        private void ensureHardwarePermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                throw new SecurityException("The caller does not have hardware permission");
            }
        }

        private void ensureValidInput(TvInputInfo inputInfo) {
            if (inputInfo.getId() == null || !this.mComponent.equals(inputInfo.getComponent())) {
                throw new IllegalArgumentException("Invalid TvInputInfo");
            }
        }

        private void addHardwareInputLocked(TvInputInfo inputInfo) {
            TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId).hardwareInputMap.put(inputInfo.getId(), inputInfo);
            TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
        }

        public void addHardwareInput(int deviceId, TvInputInfo inputInfo) {
            ensureHardwarePermission();
            ensureValidInput(inputInfo);
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.mTvInputHardwareManager.addHardwareInput(deviceId, inputInfo);
                addHardwareInputLocked(inputInfo);
            }
        }

        public void addHdmiInput(int id, TvInputInfo inputInfo) {
            ensureHardwarePermission();
            ensureValidInput(inputInfo);
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.mTvInputHardwareManager.addHdmiInput(id, inputInfo);
                addHardwareInputLocked(inputInfo);
            }
        }

        public void removeHardwareInput(String inputId) {
            ensureHardwarePermission();
            synchronized (TvInputManagerService.this.mLock) {
                if (TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId).hardwareInputMap.remove(inputId) != null) {
                    TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
                    TvInputManagerService.this.mTvInputHardwareManager.removeHardwareInput(inputId);
                } else {
                    String str = TvInputManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("failed to remove input ");
                    stringBuilder.append(inputId);
                    Slog.e(str, stringBuilder.toString());
                }
            }
        }
    }

    private final class ServiceState {
        private boolean bound;
        private ServiceCallback callback;
        private final ComponentName component;
        private final ServiceConnection connection;
        private final Map<String, TvInputInfo> hardwareInputMap;
        private final boolean isHardware;
        private boolean reconnecting;
        private ITvInputService service;
        private final List<IBinder> sessionTokens;

        /* synthetic */ ServiceState(TvInputManagerService x0, ComponentName x1, int x2, AnonymousClass1 x3) {
            this(x1, x2);
        }

        private ServiceState(ComponentName component, int userId) {
            this.sessionTokens = new ArrayList();
            this.hardwareInputMap = new HashMap();
            this.component = component;
            this.connection = new InputServiceConnection(TvInputManagerService.this, component, userId, null);
            this.isHardware = TvInputManagerService.hasHardwarePermission(TvInputManagerService.this.mContext.getPackageManager(), component);
        }
    }

    private final class SessionCallback extends ITvInputSessionCallback.Stub {
        private final InputChannel[] mChannels;
        private final SessionState mSessionState;

        SessionCallback(SessionState sessionState, InputChannel[] channels) {
            this.mSessionState = sessionState;
            this.mChannels = channels;
        }

        public void onSessionCreated(ITvInputSession session, IBinder hardwareSessionToken) {
            synchronized (TvInputManagerService.this.mLock) {
                this.mSessionState.session = session;
                this.mSessionState.hardwareSessionToken = hardwareSessionToken;
                if (session == null || !addSessionTokenToClientStateLocked(session)) {
                    TvInputManagerService.this.removeSessionStateLocked(this.mSessionState.sessionToken, this.mSessionState.userId);
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.inputId, null, null, this.mSessionState.seq);
                } else {
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.inputId, this.mSessionState.sessionToken, this.mChannels[0], this.mSessionState.seq);
                }
                this.mChannels[0].dispose();
            }
        }

        private boolean addSessionTokenToClientStateLocked(ITvInputSession session) {
            try {
                session.asBinder().linkToDeath(this.mSessionState, 0);
                IBinder clientToken = this.mSessionState.client.asBinder();
                UserState userState = TvInputManagerService.this.getOrCreateUserStateLocked(this.mSessionState.userId);
                ClientState clientState = (ClientState) userState.clientStateMap.get(clientToken);
                if (clientState == null) {
                    clientState = new ClientState(clientToken, this.mSessionState.userId);
                    try {
                        clientToken.linkToDeath(clientState, 0);
                        userState.clientStateMap.put(clientToken, clientState);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died", e);
                        return false;
                    }
                }
                clientState.sessionTokens.add(this.mSessionState.sessionToken);
                return true;
            } catch (RemoteException e2) {
                Slog.e(TvInputManagerService.TAG, "session process has already died", e2);
                return false;
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onChannelRetuned(Uri channelUri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onChannelRetuned(channelUri, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onChannelRetuned", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onTracksChanged(List<TvTrackInfo> tracks) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onTracksChanged(tracks, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTracksChanged", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onTrackSelected(int type, String trackId) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onTrackSelected(type, trackId, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTrackSelected", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onVideoAvailable() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onVideoAvailable(this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onVideoAvailable", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onVideoUnavailable(int reason) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onVideoUnavailable(reason, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onVideoUnavailable", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onContentAllowed() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onContentAllowed(this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onContentAllowed", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onContentBlocked(String rating) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onContentBlocked(rating, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onContentBlocked", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0037, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onLayoutSurface(int left, int top, int right, int bottom) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onLayoutSurface(left, top, right, bottom, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onLayoutSurface", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onSessionEvent(String eventType, Bundle eventArgs) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onSessionEvent(eventType, eventArgs, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onSessionEvent", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onTimeShiftStatusChanged(int status) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onTimeShiftStatusChanged(status, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTimeShiftStatusChanged", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onTimeShiftStartPositionChanged(long timeMs) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onTimeShiftStartPositionChanged(timeMs, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTimeShiftStartPositionChanged", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onTimeShiftCurrentPositionChanged(long timeMs) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onTimeShiftCurrentPositionChanged(timeMs, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTimeShiftCurrentPositionChanged", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onTuned(Uri channelUri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onTuned(this.mSessionState.seq, channelUri);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTuned", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onRecordingStopped(Uri recordedProgramUri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onRecordingStopped(recordedProgramUri, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onRecordingStopped", e);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:15:0x0033, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onError(int error) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                } else {
                    try {
                        this.mSessionState.client.onError(error, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onError", e);
                    }
                }
            }
        }
    }

    private static class SessionNotFoundException extends IllegalArgumentException {
        public SessionNotFoundException(String name) {
            super(name);
        }
    }

    private final class SessionState implements DeathRecipient {
        private final int callingUid;
        private final ITvInputClient client;
        private final ComponentName componentName;
        private IBinder hardwareSessionToken;
        private final String inputId;
        private final boolean isRecordingSession;
        private Uri logUri;
        private final int seq;
        private ITvInputSession session;
        private final IBinder sessionToken;
        private final int userId;

        /* synthetic */ SessionState(TvInputManagerService x0, IBinder x1, String x2, ComponentName x3, boolean x4, ITvInputClient x5, int x6, int x7, int x8, AnonymousClass1 x9) {
            this(x1, x2, x3, x4, x5, x6, x7, x8);
        }

        private SessionState(IBinder sessionToken, String inputId, ComponentName componentName, boolean isRecordingSession, ITvInputClient client, int seq, int callingUid, int userId) {
            this.sessionToken = sessionToken;
            this.inputId = inputId;
            this.componentName = componentName;
            this.isRecordingSession = isRecordingSession;
            this.client = client;
            this.seq = seq;
            this.callingUid = callingUid;
            this.userId = userId;
        }

        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                this.session = null;
                TvInputManagerService.this.clearSessionAndNotifyClientLocked(this);
            }
        }
    }

    private static final class TvInputState {
        private TvInputInfo info;
        private int state;

        private TvInputState() {
            this.state = 0;
        }

        /* synthetic */ TvInputState(AnonymousClass1 x0) {
            this();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("info: ");
            stringBuilder.append(this.info);
            stringBuilder.append("; state: ");
            stringBuilder.append(this.state);
            return stringBuilder.toString();
        }
    }

    private static final class UserState {
        private final Set<ITvInputManagerCallback> callbackSet;
        private final Map<IBinder, ClientState> clientStateMap;
        private final List<TvContentRatingSystemInfo> contentRatingSystemList;
        private Map<String, TvInputState> inputMap;
        private IBinder mainSessionToken;
        private final Set<String> packageSet;
        private final PersistentDataStore persistentDataStore;
        private final Map<ComponentName, ServiceState> serviceStateMap;
        private final Map<IBinder, SessionState> sessionStateMap;

        /* synthetic */ UserState(Context x0, int x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private UserState(Context context, int userId) {
            this.inputMap = new HashMap();
            this.packageSet = new HashSet();
            this.contentRatingSystemList = new ArrayList();
            this.clientStateMap = new HashMap();
            this.serviceStateMap = new HashMap();
            this.sessionStateMap = new HashMap();
            this.callbackSet = new HashSet();
            this.mainSessionToken = null;
            this.persistentDataStore = new PersistentDataStore(context, userId);
        }
    }

    private static final class WatchLogHandler extends Handler {
        static final int MSG_LOG_WATCH_END = 2;
        static final int MSG_LOG_WATCH_START = 1;
        static final int MSG_SWITCH_CONTENT_RESOLVER = 3;
        private ContentResolver mContentResolver;

        WatchLogHandler(ContentResolver contentResolver, Looper looper) {
            super(looper);
            this.mContentResolver = contentResolver;
        }

        public void handleMessage(Message msg) {
            SomeArgs args;
            long watchStartTime;
            switch (msg.what) {
                case 1:
                    args = (SomeArgs) msg.obj;
                    String packageName = args.arg1;
                    watchStartTime = ((Long) args.arg2).longValue();
                    long channelId = ((Long) args.arg3).longValue();
                    Bundle tuneParams = args.arg4;
                    IBinder sessionToken = args.arg5;
                    ContentValues values = new ContentValues();
                    values.put("package_name", packageName);
                    values.put("watch_start_time_utc_millis", Long.valueOf(watchStartTime));
                    values.put("channel_id", Long.valueOf(channelId));
                    if (tuneParams != null) {
                        values.put("tune_params", encodeTuneParams(tuneParams));
                    }
                    values.put("session_token", sessionToken.toString());
                    this.mContentResolver.insert(WatchedPrograms.CONTENT_URI, values);
                    args.recycle();
                    return;
                case 2:
                    args = msg.obj;
                    IBinder sessionToken2 = args.arg1;
                    watchStartTime = ((Long) args.arg2).longValue();
                    ContentValues values2 = new ContentValues();
                    values2.put("watch_end_time_utc_millis", Long.valueOf(watchStartTime));
                    values2.put("session_token", sessionToken2.toString());
                    this.mContentResolver.insert(WatchedPrograms.CONTENT_URI, values2);
                    args.recycle();
                    return;
                case 3:
                    this.mContentResolver = (ContentResolver) msg.obj;
                    return;
                default:
                    String str = TvInputManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unhandled message code: ");
                    stringBuilder.append(msg.what);
                    Slog.w(str, stringBuilder.toString());
                    return;
            }
        }

        private String encodeTuneParams(Bundle tuneParams) {
            StringBuilder builder = new StringBuilder();
            Iterator<String> it = tuneParams.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                Object value = tuneParams.get(key);
                if (value != null) {
                    builder.append(replaceEscapeCharacters(key));
                    builder.append("=");
                    builder.append(replaceEscapeCharacters(value.toString()));
                    if (it.hasNext()) {
                        builder.append(", ");
                    }
                }
            }
            return builder.toString();
        }

        private String replaceEscapeCharacters(String src) {
            String ENCODING_TARGET_CHARACTERS = "%=,";
            StringBuilder builder = new StringBuilder();
            for (char ch : src.toCharArray()) {
                if ("%=,".indexOf(ch) >= 0) {
                    builder.append('%');
                }
                builder.append(ch);
            }
            return builder.toString();
        }
    }

    private final class HardwareListener implements Listener {
        private HardwareListener() {
        }

        /* synthetic */ HardwareListener(TvInputManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onStateChanged(String inputId, int state) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.setStateLocked(inputId, state, TvInputManagerService.this.mCurrentUserId);
            }
        }

        public void onHardwareDeviceAdded(TvInputHardwareInfo info) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware) {
                        if (serviceState.service != null) {
                            try {
                                serviceState.service.notifyHardwareAdded(info);
                            } catch (RemoteException e) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e);
                            }
                        }
                    }
                }
            }
        }

        public void onHardwareDeviceRemoved(TvInputHardwareInfo info) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware) {
                        if (serviceState.service != null) {
                            try {
                                serviceState.service.notifyHardwareRemoved(info);
                            } catch (RemoteException e) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHardwareRemoved", e);
                            }
                        }
                    }
                }
            }
        }

        public void onHdmiDeviceAdded(HdmiDeviceInfo deviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware) {
                        if (serviceState.service != null) {
                            try {
                                serviceState.service.notifyHdmiDeviceAdded(deviceInfo);
                            } catch (RemoteException e) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e);
                            }
                        }
                    }
                }
            }
        }

        public void onHdmiDeviceRemoved(HdmiDeviceInfo deviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware) {
                        if (serviceState.service != null) {
                            try {
                                serviceState.service.notifyHdmiDeviceRemoved(deviceInfo);
                            } catch (RemoteException e) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceRemoved", e);
                            }
                        }
                    }
                }
            }
        }

        public void onHdmiDeviceUpdated(String inputId, HdmiDeviceInfo deviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                Integer state;
                switch (deviceInfo.getDevicePowerStatus()) {
                    case 0:
                        state = Integer.valueOf(0);
                        break;
                    case 1:
                    case 2:
                    case 3:
                        state = Integer.valueOf(1);
                        break;
                    default:
                        state = null;
                        break;
                }
                if (state != null) {
                    TvInputManagerService.this.setStateLocked(inputId, state.intValue(), TvInputManagerService.this.mCurrentUserId);
                }
            }
        }
    }

    public TvInputManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mWatchLogHandler = new WatchLogHandler(this.mContext.getContentResolver(), IoThread.get().getLooper());
        this.mTvInputHardwareManager = new TvInputHardwareManager(context, new HardwareListener(this, null));
        synchronized (this.mLock) {
            getOrCreateUserStateLocked(this.mCurrentUserId);
        }
    }

    public void onStart() {
        publishBinderService("tv_input", new BinderService(this, null));
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            registerBroadcastReceivers();
        } else if (phase == 600) {
            synchronized (this.mLock) {
                buildTvInputListLocked(this.mCurrentUserId, null);
                buildTvContentRatingSystemListLocked(this.mCurrentUserId);
            }
        }
        this.mTvInputHardwareManager.onBootPhase(phase);
    }

    public void onUnlockUser(int userHandle) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId != userHandle) {
                return;
            }
            buildTvInputListLocked(this.mCurrentUserId, null);
            buildTvContentRatingSystemListLocked(this.mCurrentUserId);
        }
    }

    private void registerBroadcastReceivers() {
        new PackageMonitor() {
            private void buildTvInputList(String[] packages) {
                synchronized (TvInputManagerService.this.mLock) {
                    if (TvInputManagerService.this.mCurrentUserId == getChangingUserId()) {
                        TvInputManagerService.this.buildTvInputListLocked(TvInputManagerService.this.mCurrentUserId, packages);
                        TvInputManagerService.this.buildTvContentRatingSystemListLocked(TvInputManagerService.this.mCurrentUserId);
                    }
                }
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                buildTvInputList(new String[]{packageName});
            }

            public void onPackagesAvailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInputList(packages);
                }
            }

            public void onPackagesUnavailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInputList(packages);
                }
            }

            public void onSomePackagesChanged() {
                if (!isReplacing()) {
                    buildTvInputList(null);
                }
            }

            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                return true;
            }
        }.register(this.mContext, null, UserHandle.ALL, true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    TvInputManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    TvInputManagerService.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    private static boolean hasHardwarePermission(PackageManager pm, ComponentName component) {
        return pm.checkPermission("android.permission.TV_INPUT_HARDWARE", component.getPackageName()) == 0;
    }

    private void buildTvInputListLocked(int userId, String[] updatedPackages) {
        String str;
        UserState userState = getOrCreateUserStateLocked(userId);
        userState.packageSet.clear();
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.media.tv.TvInputService"), 132, userId);
        List<TvInputInfo> inputList = new ArrayList();
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if ("android.permission.BIND_TV_INPUT".equals(si.permission)) {
                ComponentName component = new ComponentName(si.packageName, si.name);
                if (hasHardwarePermission(pm, component)) {
                    ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
                    if (serviceState == null) {
                        userState.serviceStateMap.put(component, new ServiceState(this, component, userId, null));
                        updateServiceConnectionLocked(component, userId);
                    } else {
                        inputList.addAll(serviceState.hardwareInputMap.values());
                    }
                } else {
                    try {
                        inputList.add(new Builder(this.mContext, ri).build());
                    } catch (Exception e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("failed to load TV input ");
                        stringBuilder.append(si.name);
                        Slog.e(str2, stringBuilder.toString(), e);
                    }
                }
                userState.packageSet.add(si.packageName);
            } else {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Skipping TV input ");
                stringBuilder2.append(si.name);
                stringBuilder2.append(": it does not require the permission ");
                stringBuilder2.append("android.permission.BIND_TV_INPUT");
                Slog.w(str, stringBuilder2.toString());
            }
        }
        Map<String, TvInputState> inputMap = new HashMap();
        for (TvInputInfo info : inputList) {
            TvInputState inputState = (TvInputState) userState.inputMap.get(info.getId());
            if (inputState == null) {
                inputState = new TvInputState();
            }
            inputState.info = info;
            inputMap.put(info.getId(), inputState);
        }
        for (String str3 : inputMap.keySet()) {
            if (!userState.inputMap.containsKey(str3)) {
                notifyInputAddedLocked(userState, str3);
            } else if (updatedPackages != null) {
                ComponentName component2 = ((TvInputState) inputMap.get(str3)).info.getComponent();
                for (String updatedPackage : updatedPackages) {
                    if (component2.getPackageName().equals(updatedPackage)) {
                        updateServiceConnectionLocked(component2, userId);
                        notifyInputUpdatedLocked(userState, str3);
                        break;
                    }
                }
            }
        }
        for (String str32 : userState.inputMap.keySet()) {
            if (!inputMap.containsKey(str32)) {
                ServiceState serviceState2 = (ServiceState) userState.serviceStateMap.get(((TvInputState) userState.inputMap.get(str32)).info.getComponent());
                if (serviceState2 != null) {
                    abortPendingCreateSessionRequestsLocked(serviceState2, str32, userId);
                }
                notifyInputRemovedLocked(userState, str32);
            }
        }
        userState.inputMap.clear();
        userState.inputMap = inputMap;
    }

    private void buildTvContentRatingSystemListLocked(int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        userState.contentRatingSystemList.clear();
        for (ResolveInfo resolveInfo : this.mContext.getPackageManager().queryBroadcastReceivers(new Intent("android.media.tv.action.QUERY_CONTENT_RATING_SYSTEMS"), 128)) {
            ActivityInfo receiver = resolveInfo.activityInfo;
            Bundle metaData = receiver.metaData;
            if (metaData != null) {
                int xmlResId = metaData.getInt("android.media.tv.metadata.CONTENT_RATING_SYSTEMS");
                if (xmlResId == 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Missing meta-data 'android.media.tv.metadata.CONTENT_RATING_SYSTEMS' on receiver ");
                    stringBuilder.append(receiver.packageName);
                    stringBuilder.append(SliceAuthority.DELIMITER);
                    stringBuilder.append(receiver.name);
                    Slog.w(str, stringBuilder.toString());
                } else {
                    userState.contentRatingSystemList.add(TvContentRatingSystemInfo.createTvContentRatingSystemInfo(xmlResId, receiver.applicationInfo));
                }
            }
        }
    }

    private void switchUser(int userId) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == userId) {
                return;
            }
            UserState userState = (UserState) this.mUserStates.get(this.mCurrentUserId);
            List<SessionState> sessionStatesToRelease = new ArrayList();
            for (SessionState sessionState : userState.sessionStateMap.values()) {
                if (!(sessionState.session == null || sessionState.isRecordingSession)) {
                    sessionStatesToRelease.add(sessionState);
                }
            }
            for (SessionState sessionState2 : sessionStatesToRelease) {
                try {
                    sessionState2.session.release();
                } catch (RemoteException e) {
                    Slog.e(TAG, "error in release", e);
                }
                clearSessionAndNotifyClientLocked(sessionState2);
            }
            Iterator<ComponentName> it = userState.serviceStateMap.keySet().iterator();
            while (it.hasNext()) {
                ServiceState serviceState = (ServiceState) userState.serviceStateMap.get((ComponentName) it.next());
                if (serviceState != null && serviceState.sessionTokens.isEmpty()) {
                    if (serviceState.callback != null) {
                        try {
                            serviceState.service.unregisterCallback(serviceState.callback);
                        } catch (RemoteException e2) {
                            Slog.e(TAG, "error in unregisterCallback", e2);
                        }
                    }
                    this.mContext.unbindService(serviceState.connection);
                    it.remove();
                }
            }
            this.mCurrentUserId = userId;
            getOrCreateUserStateLocked(userId);
            buildTvInputListLocked(userId, null);
            buildTvContentRatingSystemListLocked(userId);
            this.mWatchLogHandler.obtainMessage(3, getContentResolverForUser(userId)).sendToTarget();
        }
    }

    private void clearSessionAndNotifyClientLocked(SessionState state) {
        if (state.client != null) {
            try {
                state.client.onSessionReleased(state.seq);
            } catch (RemoteException e) {
                Slog.e(TAG, "error in onSessionReleased", e);
            }
        }
        for (SessionState sessionState : getOrCreateUserStateLocked(state.userId).sessionStateMap.values()) {
            if (state.sessionToken == sessionState.hardwareSessionToken) {
                releaseSessionLocked(sessionState.sessionToken, 1000, state.userId);
                try {
                    sessionState.client.onSessionReleased(sessionState.seq);
                } catch (RemoteException e2) {
                    Slog.e(TAG, "error in onSessionReleased", e2);
                }
            }
        }
        removeSessionStateLocked(state.sessionToken, state.userId);
    }

    private void removeUser(int userId) {
        synchronized (this.mLock) {
            UserState userState = (UserState) this.mUserStates.get(userId);
            if (userState == null) {
                return;
            }
            for (SessionState state : userState.sessionStateMap.values()) {
                if (state.session != null) {
                    try {
                        state.session.release();
                    } catch (RemoteException e) {
                        Slog.e(TAG, "error in release", e);
                    }
                }
            }
            userState.sessionStateMap.clear();
            for (ServiceState serviceState : userState.serviceStateMap.values()) {
                if (serviceState.service != null) {
                    if (serviceState.callback != null) {
                        try {
                            serviceState.service.unregisterCallback(serviceState.callback);
                        } catch (RemoteException e2) {
                            Slog.e(TAG, "error in unregisterCallback", e2);
                        }
                    }
                    this.mContext.unbindService(serviceState.connection);
                }
            }
            userState.serviceStateMap.clear();
            userState.inputMap.clear();
            userState.packageSet.clear();
            userState.contentRatingSystemList.clear();
            userState.clientStateMap.clear();
            userState.callbackSet.clear();
            userState.mainSessionToken = null;
            this.mUserStates.remove(userId);
        }
    }

    private ContentResolver getContentResolverForUser(int userId) {
        Context context;
        UserHandle user = new UserHandle(userId);
        try {
            context = this.mContext.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, user);
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("failed to create package context as user ");
            stringBuilder.append(user);
            Slog.e(str, stringBuilder.toString());
            context = this.mContext;
        }
        return context.getContentResolver();
    }

    private UserState getOrCreateUserStateLocked(int userId) {
        UserState userState = (UserState) this.mUserStates.get(userId);
        if (userState != null) {
            return userState;
        }
        userState = new UserState(this.mContext, userId, null);
        this.mUserStates.put(userId, userState);
        return userState;
    }

    private ServiceState getServiceStateLocked(ComponentName component, int userId) {
        ServiceState serviceState = (ServiceState) getOrCreateUserStateLocked(userId).serviceStateMap.get(component);
        if (serviceState != null) {
            return serviceState;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Service state not found for ");
        stringBuilder.append(component);
        stringBuilder.append(" (userId=");
        stringBuilder.append(userId);
        stringBuilder.append(")");
        throw new IllegalStateException(stringBuilder.toString());
    }

    private SessionState getSessionStateLocked(IBinder sessionToken, int callingUid, int userId) {
        SessionState sessionState = (SessionState) getOrCreateUserStateLocked(userId).sessionStateMap.get(sessionToken);
        StringBuilder stringBuilder;
        if (sessionState == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Session state not found for token ");
            stringBuilder.append(sessionToken);
            throw new SessionNotFoundException(stringBuilder.toString());
        } else if (callingUid == 1000 || callingUid == sessionState.callingUid) {
            return sessionState;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal access to the session with token ");
            stringBuilder.append(sessionToken);
            stringBuilder.append(" from uid ");
            stringBuilder.append(callingUid);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private ITvInputSession getSessionLocked(IBinder sessionToken, int callingUid, int userId) {
        return getSessionLocked(getSessionStateLocked(sessionToken, callingUid, userId));
    }

    private ITvInputSession getSessionLocked(SessionState sessionState) {
        ITvInputSession session = sessionState.session;
        if (session != null) {
            return session;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Session not yet created for token ");
        stringBuilder.append(sessionState.sessionToken);
        throw new IllegalStateException(stringBuilder.toString());
    }

    private int resolveCallingUserId(int callingPid, int callingUid, int requestedUserId, String methodName) {
        return ActivityManager.handleIncomingUser(callingPid, callingUid, requestedUserId, false, false, methodName, null);
    }

    private void updateServiceConnectionLocked(ComponentName component, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
        if (serviceState != null) {
            boolean z = false;
            if (serviceState.reconnecting) {
                if (serviceState.sessionTokens.isEmpty()) {
                    serviceState.reconnecting = false;
                } else {
                    return;
                }
            }
            boolean shouldBind;
            if (userId == this.mCurrentUserId) {
                if (!serviceState.sessionTokens.isEmpty() || serviceState.isHardware) {
                    z = true;
                }
                shouldBind = z;
            } else {
                shouldBind = serviceState.sessionTokens.isEmpty() ^ true;
            }
            if (serviceState.service == null && shouldBind) {
                if (!serviceState.bound) {
                    serviceState.bound = this.mContext.bindServiceAsUser(new Intent("android.media.tv.TvInputService").setComponent(component), serviceState.connection, 33554433, new UserHandle(userId));
                }
            } else if (!(serviceState.service == null || shouldBind)) {
                this.mContext.unbindService(serviceState.connection);
                userState.serviceStateMap.remove(component);
            }
        }
    }

    private void abortPendingCreateSessionRequestsLocked(ServiceState serviceState, String inputId, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        List<SessionState> sessionsToAbort = new ArrayList();
        for (IBinder sessionToken : serviceState.sessionTokens) {
            SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
            if (sessionState.session == null && (inputId == null || sessionState.inputId.equals(inputId))) {
                sessionsToAbort.add(sessionState);
            }
        }
        for (SessionState sessionState2 : sessionsToAbort) {
            removeSessionStateLocked(sessionState2.sessionToken, sessionState2.userId);
            sendSessionTokenToClientLocked(sessionState2.client, sessionState2.inputId, null, null, sessionState2.seq);
        }
        updateServiceConnectionLocked(serviceState.component, userId);
    }

    private void createSessionInternalLocked(ITvInputService service, IBinder sessionToken, int userId) {
        ITvInputService iTvInputService = service;
        IBinder iBinder = sessionToken;
        int i = userId;
        SessionState sessionState = (SessionState) getOrCreateUserStateLocked(i).sessionStateMap.get(iBinder);
        InputChannel[] channels = InputChannel.openInputChannelPair(sessionToken.toString());
        SessionCallback callback = new SessionCallback(sessionState, channels);
        try {
            if (sessionState.isRecordingSession) {
                iTvInputService.createRecordingSession(callback, sessionState.inputId);
            } else {
                iTvInputService.createSession(channels[1], callback, sessionState.inputId);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "error in createSession", e);
            removeSessionStateLocked(iBinder, i);
            sendSessionTokenToClientLocked(sessionState.client, sessionState.inputId, null, null, sessionState.seq);
        }
        channels[1].dispose();
    }

    private void sendSessionTokenToClientLocked(ITvInputClient client, String inputId, IBinder sessionToken, InputChannel channel, int seq) {
        try {
            client.onSessionCreated(inputId, sessionToken, channel, seq);
        } catch (RemoteException e) {
            Slog.e(TAG, "error in onSessionCreated", e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x002a A:{Splitter: B:1:0x0002, ExcHandler: android.os.RemoteException (r2_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:8:0x0022, code:
            if (r1 != null) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:9:0x0024, code:
            com.android.server.tv.TvInputManagerService.SessionState.access$1702(r1, null);
     */
    /* JADX WARNING: Missing block: B:11:0x002a, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            android.util.Slog.e(TAG, "error in releaseSession", r2);
     */
    /* JADX WARNING: Missing block: B:14:0x0032, code:
            if (r1 != null) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:15:0x0035, code:
            removeSessionStateLocked(r6, r8);
     */
    /* JADX WARNING: Missing block: B:16:0x0038, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void releaseSessionLocked(IBinder sessionToken, int callingUid, int userId) {
        SessionState sessionState = null;
        try {
            sessionState = getSessionStateLocked(sessionToken, callingUid, userId);
            if (sessionState.session != null) {
                if (sessionToken == getOrCreateUserStateLocked(userId).mainSessionToken) {
                    setMainLocked(sessionToken, false, callingUid, userId);
                }
                sessionState.session.release();
            }
        } catch (Exception e) {
        } catch (Throwable th) {
            if (sessionState != null) {
                sessionState.session = null;
            }
        }
    }

    private void removeSessionStateLocked(IBinder sessionToken, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        if (sessionToken == userState.mainSessionToken) {
            userState.mainSessionToken = null;
        }
        SessionState sessionState = (SessionState) userState.sessionStateMap.remove(sessionToken);
        if (sessionState != null) {
            ClientState clientState = (ClientState) userState.clientStateMap.get(sessionState.client.asBinder());
            if (clientState != null) {
                clientState.sessionTokens.remove(sessionToken);
                if (clientState.isEmpty()) {
                    userState.clientStateMap.remove(sessionState.client.asBinder());
                }
            }
            ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(sessionState.componentName);
            if (serviceState != null) {
                serviceState.sessionTokens.remove(sessionToken);
            }
            updateServiceConnectionLocked(sessionState.componentName, userId);
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = sessionToken;
            args.arg2 = Long.valueOf(System.currentTimeMillis());
            this.mWatchLogHandler.obtainMessage(2, args).sendToTarget();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:8:0x002c A:{Splitter: B:0:0x0000, ExcHandler: android.os.RemoteException (r0_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:8:0x002c, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:9:0x002d, code:
            android.util.Slog.e(TAG, "error in setMain", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setMainLocked(IBinder sessionToken, boolean isMain, int callingUid, int userId) {
        try {
            SessionState sessionState = getSessionStateLocked(sessionToken, callingUid, userId);
            if (sessionState.hardwareSessionToken != null) {
                sessionState = getSessionStateLocked(sessionState.hardwareSessionToken, 1000, userId);
            }
            if (getServiceStateLocked(sessionState.componentName, userId).isHardware) {
                getSessionLocked(sessionState).setMain(isMain);
            }
        } catch (Exception e) {
        }
    }

    private void notifyInputAddedLocked(UserState userState, String inputId) {
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onInputAdded(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report added input to callback", e);
            }
        }
    }

    private void notifyInputRemovedLocked(UserState userState, String inputId) {
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onInputRemoved(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report removed input to callback", e);
            }
        }
    }

    private void notifyInputUpdatedLocked(UserState userState, String inputId) {
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onInputUpdated(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated input to callback", e);
            }
        }
    }

    private void notifyInputStateChangedLocked(UserState userState, String inputId, int state, ITvInputManagerCallback targetCallback) {
        if (targetCallback == null) {
            for (ITvInputManagerCallback callback : userState.callbackSet) {
                try {
                    callback.onInputStateChanged(inputId, state);
                } catch (RemoteException e) {
                    Slog.e(TAG, "failed to report state change to callback", e);
                }
            }
            return;
        }
        try {
            targetCallback.onInputStateChanged(inputId, state);
        } catch (RemoteException e2) {
            Slog.e(TAG, "failed to report state change to callback", e2);
        }
    }

    private void updateTvInputInfoLocked(UserState userState, TvInputInfo inputInfo) {
        String inputId = inputInfo.getId();
        TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
        if (inputState == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("failed to set input info - unknown input id ");
            stringBuilder.append(inputId);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        inputState.info = inputInfo;
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onTvInputInfoUpdated(inputInfo);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated input info to callback", e);
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x003f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setStateLocked(String inputId, int state, int userId) {
        UserState userState = getOrCreateUserStateLocked(userId);
        TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(inputState.info.getComponent());
        int oldState = inputState.state;
        inputState.state = state;
        if ((serviceState == null || serviceState.service != null || (serviceState.sessionTokens.isEmpty() && !serviceState.isHardware)) && oldState != state) {
            notifyInputStateChangedLocked(userState, inputId, state, null);
        }
    }
}
