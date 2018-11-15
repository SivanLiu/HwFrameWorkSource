package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PackageDeleteObserver;
import android.app.PackageInstallObserver;
import android.app.admin.DevicePolicyManagerInternal;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstaller.Stub;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller.SessionInfo;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.ExceptionUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageHelper;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.ImageUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.pm.permission.PermissionManagerInternal;
import com.android.server.power.IHwShutdownThread;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class PackageInstallerService extends Stub {
    private static final boolean LOGD = false;
    private static final long MAX_ACTIVE_SESSIONS = 1024;
    private static final long MAX_AGE_MILLIS = 259200000;
    private static final long MAX_HISTORICAL_SESSIONS = 1048576;
    private static final String TAG = "PackageInstaller";
    private static final String TAG_SESSIONS = "sessions";
    private static final FilenameFilter sStageFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return PackageInstallerService.isStageName(name);
        }
    };
    @GuardedBy("mSessions")
    private final SparseBooleanArray mAllocatedSessions = new SparseBooleanArray();
    private AppOpsManager mAppOps;
    private final Callbacks mCallbacks;
    private final Context mContext;
    @GuardedBy("mSessions")
    private final List<String> mHistoricalSessions = new ArrayList();
    @GuardedBy("mSessions")
    private final SparseIntArray mHistoricalSessionsByInstaller = new SparseIntArray();
    private final Handler mInstallHandler;
    private final HandlerThread mInstallThread;
    private final InternalCallback mInternalCallback = new InternalCallback();
    @GuardedBy("mSessions")
    private final SparseBooleanArray mLegacySessions = new SparseBooleanArray();
    private final PermissionManagerInternal mPermissionManager;
    private final PackageManagerService mPm;
    private final Random mRandom = new SecureRandom();
    @GuardedBy("mSessions")
    private final SparseArray<PackageInstallerSession> mSessions = new SparseArray();
    private final File mSessionsDir;
    private final AtomicFile mSessionsFile;

    private static class Callbacks extends Handler {
        private static final int MSG_SESSION_ACTIVE_CHANGED = 3;
        private static final int MSG_SESSION_BADGING_CHANGED = 2;
        private static final int MSG_SESSION_CREATED = 1;
        private static final int MSG_SESSION_FINISHED = 5;
        private static final int MSG_SESSION_PROGRESS_CHANGED = 4;
        private final RemoteCallbackList<IPackageInstallerCallback> mCallbacks = new RemoteCallbackList();

        public Callbacks(Looper looper) {
            super(looper);
        }

        public void register(IPackageInstallerCallback callback, int userId) {
            this.mCallbacks.register(callback, new UserHandle(userId));
        }

        public void unregister(IPackageInstallerCallback callback) {
            this.mCallbacks.unregister(callback);
        }

        public void handleMessage(Message msg) {
            int userId = msg.arg2;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IPackageInstallerCallback callback = (IPackageInstallerCallback) this.mCallbacks.getBroadcastItem(i);
                if (userId == ((UserHandle) this.mCallbacks.getBroadcastCookie(i)).getIdentifier()) {
                    try {
                        invokeCallback(callback, msg);
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mCallbacks.finishBroadcast();
        }

        private void invokeCallback(IPackageInstallerCallback callback, Message msg) throws RemoteException {
            int sessionId = msg.arg1;
            switch (msg.what) {
                case 1:
                    callback.onSessionCreated(sessionId);
                    return;
                case 2:
                    callback.onSessionBadgingChanged(sessionId);
                    return;
                case 3:
                    callback.onSessionActiveChanged(sessionId, ((Boolean) msg.obj).booleanValue());
                    return;
                case 4:
                    callback.onSessionProgressChanged(sessionId, ((Float) msg.obj).floatValue());
                    return;
                case 5:
                    callback.onSessionFinished(sessionId, ((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }

        private void notifySessionCreated(int sessionId, int userId) {
            obtainMessage(1, sessionId, userId).sendToTarget();
        }

        private void notifySessionBadgingChanged(int sessionId, int userId) {
            obtainMessage(2, sessionId, userId).sendToTarget();
        }

        private void notifySessionActiveChanged(int sessionId, int userId, boolean active) {
            obtainMessage(3, sessionId, userId, Boolean.valueOf(active)).sendToTarget();
        }

        private void notifySessionProgressChanged(int sessionId, int userId, float progress) {
            obtainMessage(4, sessionId, userId, Float.valueOf(progress)).sendToTarget();
        }

        public void notifySessionFinished(int sessionId, int userId, boolean success) {
            obtainMessage(5, sessionId, userId, Boolean.valueOf(success)).sendToTarget();
        }
    }

    class InternalCallback {
        InternalCallback() {
        }

        public void onSessionBadgingChanged(PackageInstallerSession session) {
            PackageInstallerService.this.mCallbacks.notifySessionBadgingChanged(session.sessionId, session.userId);
            PackageInstallerService.this.writeSessionsAsync();
        }

        public void onSessionActiveChanged(PackageInstallerSession session, boolean active) {
            PackageInstallerService.this.mCallbacks.notifySessionActiveChanged(session.sessionId, session.userId, active);
        }

        public void onSessionProgressChanged(PackageInstallerSession session, float progress) {
            PackageInstallerService.this.mCallbacks.notifySessionProgressChanged(session.sessionId, session.userId, progress);
        }

        public void onSessionFinished(final PackageInstallerSession session, boolean success) {
            PackageInstallerService.this.mCallbacks.notifySessionFinished(session.sessionId, session.userId, success);
            PackageInstallerService.this.mInstallHandler.post(new Runnable() {
                public void run() {
                    synchronized (PackageInstallerService.this.mSessions) {
                        PackageInstallerService.this.mSessions.remove(session.sessionId);
                        PackageInstallerService.this.addHistoricalSessionLocked(session);
                        File appIconFile = PackageInstallerService.this.buildAppIconFile(session.sessionId);
                        if (appIconFile.exists()) {
                            appIconFile.delete();
                        }
                        PackageInstallerService.this.writeSessionsLocked();
                    }
                }
            });
        }

        public void onSessionPrepared(PackageInstallerSession session) {
            PackageInstallerService.this.writeSessionsAsync();
        }

        public void onSessionSealedBlocking(PackageInstallerSession session) {
            synchronized (PackageInstallerService.this.mSessions) {
                PackageInstallerService.this.writeSessionsLocked();
            }
        }
    }

    static class PackageDeleteObserverAdapter extends PackageDeleteObserver {
        private final Context mContext;
        private final Notification mNotification;
        private final String mPackageName;
        private final IntentSender mTarget;

        public PackageDeleteObserverAdapter(Context context, IntentSender target, String packageName, boolean showNotification, int userId) {
            this.mContext = context;
            this.mTarget = target;
            this.mPackageName = packageName;
            if (showNotification) {
                this.mNotification = PackageInstallerService.buildSuccessNotification(this.mContext, this.mContext.getResources().getString(17040627), packageName, userId);
            } else {
                this.mNotification = null;
            }
        }

        public void onUserActionRequired(Intent intent) {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            fillIn.putExtra("android.content.pm.extra.STATUS", -1);
            fillIn.putExtra("android.intent.extra.INTENT", intent);
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (SendIntentException e) {
            }
        }

        public void onPackageDeleted(String basePackageName, int returnCode, String msg) {
            if (1 == returnCode && this.mNotification != null) {
                ((NotificationManager) this.mContext.getSystemService("notification")).notify(basePackageName, 21, this.mNotification);
            }
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
            fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.deleteStatusToPublicStatus(returnCode));
            fillIn.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.deleteStatusToString(returnCode, msg));
            fillIn.putExtra("android.content.pm.extra.LEGACY_STATUS", returnCode);
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (SendIntentException e) {
            }
        }
    }

    static class PackageInstallObserverAdapter extends PackageInstallObserver {
        private final Context mContext;
        private final int mSessionId;
        private final boolean mShowNotification;
        private final IntentSender mTarget;
        private final int mUserId;

        public PackageInstallObserverAdapter(Context context, IntentSender target, int sessionId, boolean showNotification, int userId) {
            this.mContext = context;
            this.mTarget = target;
            this.mSessionId = sessionId;
            this.mShowNotification = showNotification;
            this.mUserId = userId;
        }

        public void onUserActionRequired(Intent intent) {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.SESSION_ID", this.mSessionId);
            fillIn.putExtra("android.content.pm.extra.STATUS", -1);
            fillIn.putExtra("android.intent.extra.INTENT", intent);
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (SendIntentException e) {
            }
        }

        public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
            boolean update = true;
            if (1 == returnCode && this.mShowNotification) {
                int i;
                if (extras == null || !extras.getBoolean("android.intent.extra.REPLACING")) {
                    update = false;
                }
                Notification notification = this.mContext;
                Resources resources = this.mContext.getResources();
                if (update) {
                    i = 17040629;
                } else {
                    i = 17040628;
                }
                notification = PackageInstallerService.buildSuccessNotification(notification, resources.getString(i), basePackageName, this.mUserId);
                if (notification != null) {
                    ((NotificationManager) this.mContext.getSystemService("notification")).notify(basePackageName, 21, notification);
                }
            }
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", basePackageName);
            fillIn.putExtra("android.content.pm.extra.SESSION_ID", this.mSessionId);
            fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.installStatusToPublicStatus(returnCode));
            fillIn.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.installStatusToString(returnCode, msg));
            fillIn.putExtra("android.content.pm.extra.LEGACY_STATUS", returnCode);
            if (extras != null) {
                String existing = extras.getString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE");
                if (!TextUtils.isEmpty(existing)) {
                    fillIn.putExtra("android.content.pm.extra.OTHER_PACKAGE_NAME", existing);
                }
            }
            try {
                this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
            } catch (SendIntentException e) {
            }
        }
    }

    public PackageInstallerService(Context context, PackageManagerService pm) {
        this.mContext = context;
        this.mPm = pm;
        this.mPermissionManager = (PermissionManagerInternal) LocalServices.getService(PermissionManagerInternal.class);
        this.mInstallThread = new HandlerThread(TAG);
        this.mInstallThread.start();
        this.mInstallHandler = new Handler(this.mInstallThread.getLooper());
        this.mCallbacks = new Callbacks(this.mInstallThread.getLooper());
        this.mSessionsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "install_sessions.xml"), "package-session");
        this.mSessionsDir = new File(Environment.getDataSystemDirectory(), "install_sessions");
        this.mSessionsDir.mkdirs();
    }

    public void systemReady() {
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        synchronized (this.mSessions) {
            readSessionsLocked();
            int i = 0;
            reconcileStagesLocked(StorageManager.UUID_PRIVATE_INTERNAL, false);
            reconcileStagesLocked(StorageManager.UUID_PRIVATE_INTERNAL, true);
            ArraySet<File> unclaimedIcons = newArraySet(this.mSessionsDir.listFiles());
            while (i < this.mSessions.size()) {
                unclaimedIcons.remove(buildAppIconFile(((PackageInstallerSession) this.mSessions.valueAt(i)).sessionId));
                i++;
            }
            Iterator it = unclaimedIcons.iterator();
            while (it.hasNext()) {
                File icon = (File) it.next();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Deleting orphan icon ");
                stringBuilder.append(icon);
                Slog.w(str, stringBuilder.toString());
                icon.delete();
            }
        }
    }

    @GuardedBy("mSessions")
    private void reconcileStagesLocked(String volumeUuid, boolean isEphemeral) {
        ArraySet<File> unclaimedStages = newArraySet(buildStagingDir(volumeUuid, isEphemeral).listFiles(sStageFilter));
        for (int i = 0; i < this.mSessions.size(); i++) {
            unclaimedStages.remove(((PackageInstallerSession) this.mSessions.valueAt(i)).stageDir);
        }
        Iterator it = unclaimedStages.iterator();
        while (it.hasNext()) {
            File stage = (File) it.next();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Deleting orphan stage ");
            stringBuilder.append(stage);
            Slog.w(str, stringBuilder.toString());
            synchronized (this.mPm.mInstallLock) {
                this.mPm.removeCodePathLI(stage);
            }
        }
    }

    public void onPrivateVolumeMounted(String volumeUuid) {
        synchronized (this.mSessions) {
            reconcileStagesLocked(volumeUuid, false);
        }
    }

    public static boolean isStageName(String name) {
        boolean isFile = name.startsWith("vmdl") && name.endsWith(".tmp");
        boolean isContainer = name.startsWith("smdl") && name.endsWith(".tmp");
        boolean isLegacyContainer = name.startsWith("smdl2tmp");
        if (isFile || isContainer || isLegacyContainer) {
            return true;
        }
        return false;
    }

    @Deprecated
    public File allocateStageDirLegacy(String volumeUuid, boolean isEphemeral) throws IOException {
        File stageDir;
        synchronized (this.mSessions) {
            try {
                int sessionId = allocateSessionIdLocked();
                this.mLegacySessions.put(sessionId, true);
                stageDir = buildStageDir(volumeUuid, sessionId, isEphemeral);
                prepareStageDir(stageDir);
            } catch (IllegalStateException e) {
                throw new IOException(e);
            }
        }
        return stageDir;
    }

    @Deprecated
    public String allocateExternalStageCidLegacy() {
        String stringBuilder;
        synchronized (this.mSessions) {
            int sessionId = allocateSessionIdLocked();
            this.mLegacySessions.put(sessionId, true);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("smdl");
            stringBuilder2.append(sessionId);
            stringBuilder2.append(".tmp");
            stringBuilder = stringBuilder2.toString();
        }
        return stringBuilder;
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x0092 A:{PHI: r0 , Splitter: B:1:0x0006, ExcHandler: java.io.IOException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:25:0x0092, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:27:?, code:
            android.util.Slog.wtf(TAG, "Failed reading install sessions", r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mSessions")
    private void readSessionsLocked() {
        this.mSessions.clear();
        FileInputStream fis = null;
        try {
            fis = this.mSessionsFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            while (true) {
                int next = in.next();
                int type = next;
                if (next == 1) {
                    break;
                } else if (type == 2) {
                    if ("session".equals(in.getName())) {
                        try {
                            boolean valid;
                            PackageInstallerSession session = PackageInstallerSession.readFromXml(in, this.mInternalCallback, this.mContext, this.mPm, this.mInstallThread.getLooper(), this.mSessionsDir);
                            if (System.currentTimeMillis() - session.createdMillis >= MAX_AGE_MILLIS) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Abandoning old session first created at ");
                                stringBuilder.append(session.createdMillis);
                                Slog.w(str, stringBuilder.toString());
                                valid = false;
                            } else {
                                valid = true;
                            }
                            if (valid) {
                                this.mSessions.put(session.sessionId, session);
                            } else {
                                addHistoricalSessionLocked(session);
                            }
                            this.mAllocatedSessions.put(session.sessionId, true);
                        } catch (Exception e) {
                            Slog.e(TAG, "Could not read session", e);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e2) {
        } catch (Exception e3) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(fis);
        }
        IoUtils.closeQuietly(fis);
    }

    @GuardedBy("mSessions")
    private void addHistoricalSessionLocked(PackageInstallerSession session) {
        CharArrayWriter writer = new CharArrayWriter();
        session.dump(new IndentingPrintWriter(writer, "    "));
        this.mHistoricalSessions.add(writer.toString());
        int installerUid = session.getInstallerUid();
        this.mHistoricalSessionsByInstaller.put(installerUid, this.mHistoricalSessionsByInstaller.get(installerUid) + 1);
    }

    @GuardedBy("mSessions")
    private void writeSessionsLocked() {
        FileOutputStream fos = null;
        try {
            fos = this.mSessionsFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, TAG_SESSIONS);
            int size = this.mSessions.size();
            for (int i = 0; i < size; i++) {
                ((PackageInstallerSession) this.mSessions.valueAt(i)).write(out, this.mSessionsDir);
            }
            out.endTag(null, TAG_SESSIONS);
            out.endDocument();
            this.mSessionsFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mSessionsFile.failWrite(fos);
            }
        }
    }

    private File buildAppIconFile(int sessionId) {
        File file = this.mSessionsDir;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("app_icon.");
        stringBuilder.append(sessionId);
        stringBuilder.append(".png");
        return new File(file, stringBuilder.toString());
    }

    private void writeSessionsAsync() {
        IoThread.getHandler().post(new Runnable() {
            public void run() {
                synchronized (PackageInstallerService.this.mSessions) {
                    PackageInstallerService.this.writeSessionsLocked();
                }
            }
        });
    }

    public int createSession(SessionParams params, String installerPackageName, int userId) {
        try {
            return createSessionInternal(params, installerPackageName, userId);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    /* JADX WARNING: Missing block: B:66:0x0163, code:
            r18 = java.lang.System.currentTimeMillis();
     */
    /* JADX WARNING: Missing block: B:67:0x016d, code:
            if ((r15.installFlags & 16) == 0) goto L_0x0183;
     */
    /* JADX WARNING: Missing block: B:69:0x0173, code:
            if ((r15.installFlags & 2048) == 0) goto L_0x0176;
     */
    /* JADX WARNING: Missing block: B:70:0x0176, code:
            r2 = false;
     */
    /* JADX WARNING: Missing block: B:71:0x0177, code:
            r20 = buildStageDir(r15.volumeUuid, r11, r2);
            r21 = null;
     */
    /* JADX WARNING: Missing block: B:72:0x0183, code:
            r20 = null;
            r21 = buildExternalStageCid(r11);
     */
    /* JADX WARNING: Missing block: B:73:0x018b, code:
            r22 = r11;
            r23 = r14;
            r2 = new com.android.server.pm.PackageInstallerSession(r1.mInternalCallback, r1.mContext, r1.mPm, r1.mInstallThread.getLooper(), r11, r12, r13, r14, r15, r18, r20, r21, false, false);
            r3 = r1.mSessions;
     */
    /* JADX WARNING: Missing block: B:74:0x01b2, code:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:77:0x01b5, code:
            r4 = r22;
     */
    /* JADX WARNING: Missing block: B:79:?, code:
            r1.mSessions.put(r4, r2);
     */
    /* JADX WARNING: Missing block: B:80:0x01ba, code:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:81:0x01bb, code:
            com.android.server.pm.PackageInstallerService.Callbacks.access$200(r1.mCallbacks, r2.sessionId, r2.userId);
            writeSessionsAsync();
     */
    /* JADX WARNING: Missing block: B:82:0x01c7, code:
            return r4;
     */
    /* JADX WARNING: Missing block: B:83:0x01c8, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:84:0x01ca, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:85:0x01cb, code:
            r4 = r22;
     */
    /* JADX WARNING: Missing block: B:87:?, code:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:88:0x01ce, code:
            throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int createSessionInternal(SessionParams params, String installerPackageName, int userId) throws IOException {
        Throwable th;
        SessionParams sessionParams = params;
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, userId, true, true, "createSession");
        int i = userId;
        SessionParams sessionParams2;
        if (this.mPm.isUserRestricted(i, "no_install_apps")) {
            sessionParams2 = sessionParams;
            throw new SecurityException("User restriction prevents installing");
        }
        String str;
        if (callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingUid == 0) {
            str = installerPackageName;
            sessionParams.installFlags |= 32;
        } else {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
                str = installerPackageName;
                this.mAppOps.checkPackage(callingUid, str);
            } else {
                str = installerPackageName;
            }
            sessionParams.installFlags &= -33;
            sessionParams.installFlags &= -65;
            sessionParams.installFlags |= 2;
            if (!((sessionParams.installFlags & 65536) == 0 || this.mPm.isCallerVerifier(callingUid))) {
                sessionParams.installFlags &= -65537;
            }
        }
        if ((sessionParams.installFlags & 256) == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") != -1) {
            boolean isInstant = true;
            if ((sessionParams.installFlags & 1) == 0 && (sessionParams.installFlags & 8) == 0) {
                if (sessionParams.appIcon != null) {
                    int iconSize = ((ActivityManager) this.mContext.getSystemService("activity")).getLauncherLargeIconSize();
                    if (sessionParams.appIcon.getWidth() > iconSize * 2 || sessionParams.appIcon.getHeight() > iconSize * 2) {
                        sessionParams.appIcon = Bitmap.createScaledBitmap(sessionParams.appIcon, iconSize, iconSize, true);
                    }
                }
                switch (sessionParams.mode) {
                    case 1:
                    case 2:
                        long ident;
                        int i2;
                        if ((sessionParams.installFlags & 16) != 0) {
                            if (!PackageHelper.fitsOnInternal(this.mContext, sessionParams)) {
                                throw new IOException("No suitable internal storage available");
                            }
                        } else if ((sessionParams.installFlags & 8) != 0) {
                            if (!PackageHelper.fitsOnExternal(this.mContext, sessionParams)) {
                                throw new IOException("No suitable external storage available");
                            }
                        } else if ((sessionParams.installFlags & 512) != 0) {
                            params.setInstallFlagsInternal();
                        } else {
                            params.setInstallFlagsInternal();
                            ident = Binder.clearCallingIdentity();
                            try {
                                SystemProperties.set("persist.sys.install_no_quota", "0");
                                sessionParams.volumeUuid = PackageHelper.resolveInstallVolume(this.mContext, sessionParams);
                                SystemProperties.set("persist.sys.install_no_quota", "1");
                                Binder.restoreCallingIdentity(ident);
                            } catch (Throwable th2) {
                                i2 = callingUid;
                                SystemProperties.set("persist.sys.install_no_quota", "1");
                                Binder.restoreCallingIdentity(ident);
                            }
                        }
                        synchronized (this.mSessions) {
                            try {
                                if (((long) getSessionCount(this.mSessions, callingUid)) >= MAX_ACTIVE_SESSIONS) {
                                    i2 = callingUid;
                                    ident = new StringBuilder();
                                    ident.append("Too many active sessions for UID ");
                                    ident.append(i2);
                                    throw new IllegalStateException(ident.toString());
                                } else if (((long) this.mHistoricalSessionsByInstaller.get(callingUid)) < MAX_HISTORICAL_SESSIONS) {
                                    try {
                                        int sessionId = allocateSessionIdLocked();
                                        break;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        i2 = callingUid;
                                        throw th;
                                    }
                                } else {
                                    int callingUid2 = callingUid;
                                    try {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Too many historical sessions for UID ");
                                        stringBuilder.append(callingUid2);
                                        throw new IllegalStateException(stringBuilder.toString());
                                    } catch (Throwable th4) {
                                        th = th4;
                                        throw th;
                                    }
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                i2 = callingUid;
                                throw th;
                            }
                        }
                    default:
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid install mode: ");
                        stringBuilder2.append(params.mode);
                        throw new IllegalArgumentException(stringBuilder2.toString());
                }
            }
            sessionParams2 = sessionParams;
            throw new IllegalArgumentException("New installs into ASEC containers no longer supported");
        }
        throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
    }

    public void updateSessionAppIcon(int sessionId, Bitmap appIcon) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = (PackageInstallerSession) this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Caller has no access to session ");
                stringBuilder.append(sessionId);
                throw new SecurityException(stringBuilder.toString());
            }
            if (appIcon != null) {
                int iconSize = ((ActivityManager) this.mContext.getSystemService("activity")).getLauncherLargeIconSize();
                if (appIcon.getWidth() > iconSize * 2 || appIcon.getHeight() > iconSize * 2) {
                    appIcon = Bitmap.createScaledBitmap(appIcon, iconSize, iconSize, true);
                }
            }
            session.params.appIcon = appIcon;
            session.params.appIconLastModified = -1;
            this.mInternalCallback.onSessionBadgingChanged(session);
        }
    }

    public void updateSessionAppLabel(int sessionId, String appLabel) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = (PackageInstallerSession) this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Caller has no access to session ");
                stringBuilder.append(sessionId);
                throw new SecurityException(stringBuilder.toString());
            }
            session.params.appLabel = appLabel;
            this.mInternalCallback.onSessionBadgingChanged(session);
        }
    }

    public void abandonSession(int sessionId) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = (PackageInstallerSession) this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Caller has no access to session ");
                stringBuilder.append(sessionId);
                throw new SecurityException(stringBuilder.toString());
            }
            session.abandon();
        }
    }

    public IPackageInstallerSession openSession(int sessionId) {
        try {
            return openSessionInternal(sessionId);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private IPackageInstallerSession openSessionInternal(int sessionId) throws IOException {
        PackageInstallerSession session;
        synchronized (this.mSessions) {
            session = (PackageInstallerSession) this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Caller has no access to session ");
                stringBuilder.append(sessionId);
                throw new SecurityException(stringBuilder.toString());
            }
            session.open();
        }
        return session;
    }

    @GuardedBy("mSessions")
    private int allocateSessionIdLocked() {
        int n = 0;
        while (true) {
            int sessionId = this.mRandom.nextInt(2147483646) + 1;
            if (this.mAllocatedSessions.get(sessionId, false)) {
                int n2 = n + 1;
                if (n < 32) {
                    n = n2;
                } else {
                    throw new IllegalStateException("Failed to allocate session ID");
                }
            }
            this.mAllocatedSessions.put(sessionId, true);
            return sessionId;
        }
    }

    private File buildStagingDir(String volumeUuid, boolean isEphemeral) {
        return Environment.getDataAppDirectory(volumeUuid);
    }

    private File buildStageDir(String volumeUuid, int sessionId, boolean isEphemeral) {
        File stagingDir = buildStagingDir(volumeUuid, isEphemeral);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("vmdl");
        stringBuilder.append(sessionId);
        stringBuilder.append(".tmp");
        return new File(stagingDir, stringBuilder.toString());
    }

    static void prepareStageDir(File stageDir) throws IOException {
        StringBuilder stringBuilder;
        if (stageDir.exists()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Session dir already exists: ");
            stringBuilder.append(stageDir);
            throw new IOException(stringBuilder.toString());
        }
        try {
            Os.mkdir(stageDir.getAbsolutePath(), 493);
            Os.chmod(stageDir.getAbsolutePath(), 493);
            if (!SELinux.restorecon(stageDir)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to restorecon session dir: ");
                stringBuilder.append(stageDir);
                throw new IOException(stringBuilder.toString());
            }
        } catch (ErrnoException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to prepare session dir: ");
            stringBuilder2.append(stageDir);
            throw new IOException(stringBuilder2.toString(), e);
        }
    }

    private String buildExternalStageCid(int sessionId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("smdl");
        stringBuilder.append(sessionId);
        stringBuilder.append(".tmp");
        return stringBuilder.toString();
    }

    public SessionInfo getSessionInfo(int sessionId) {
        SessionInfo generateInfo;
        synchronized (this.mSessions) {
            PackageInstallerSession session = (PackageInstallerSession) this.mSessions.get(sessionId);
            generateInfo = session != null ? session.generateInfo() : null;
        }
        return generateInfo;
    }

    public ParceledListSlice<SessionInfo> getAllSessions(int userId) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getAllSessions");
        List<SessionInfo> result = new ArrayList();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = (PackageInstallerSession) this.mSessions.valueAt(i);
                if (session.userId == userId) {
                    result.add(session.generateInfo(false));
                }
            }
        }
        return new ParceledListSlice(result);
    }

    public ParceledListSlice<SessionInfo> getMySessions(String installerPackageName, int userId) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getMySessions");
        this.mAppOps.checkPackage(Binder.getCallingUid(), installerPackageName);
        List<SessionInfo> result = new ArrayList();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = (PackageInstallerSession) this.mSessions.valueAt(i);
                SessionInfo info = session.generateInfo(false);
                if (Objects.equals(info.getInstallerPackageName(), installerPackageName) && session.userId == userId) {
                    result.add(info);
                }
            }
        }
        return new ParceledListSlice(result);
    }

    public void uninstall(VersionedPackage versionedPackage, String callerPackageName, int flags, IntentSender statusReceiver, int userId) throws RemoteException {
        VersionedPackage versionedPackage2 = versionedPackage;
        String str = callerPackageName;
        int i = flags;
        int i2 = userId;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.PACKAGEINSTALLER_UNINSTALL);
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, i2, true, true, "uninstall");
        if (!(callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingUid == 0)) {
            this.mAppOps.checkPackage(callingUid, str);
        }
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        ApplicationInfo applicationInfo = (dpmi != null && dpmi.isActiveAdminWithPolicy(callingUid, -1) && dpmi.isUserAffiliatedWithDevice(UserHandle.getUserId(callingUid))) ? true : null;
        boolean isDeviceOwnerOrAffiliatedProfileOwner = applicationInfo;
        PackageDeleteObserverAdapter adapter = new PackageDeleteObserverAdapter(this.mContext, statusReceiver, versionedPackage.getPackageName(), isDeviceOwnerOrAffiliatedProfileOwner, i2);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") == 0) {
            this.mPm.deletePackageVersioned(versionedPackage2, adapter.getBinder(), i2, i);
        } else if (isDeviceOwnerOrAffiliatedProfileOwner) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mPm.deletePackageVersioned(versionedPackage2, adapter.getBinder(), i2, i);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            if (this.mPm.getApplicationInfo(str, 0, i2).targetSdkVersion >= 28) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.REQUEST_DELETE_PACKAGES", null);
            }
            Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
            intent.setData(Uri.fromParts("package", versionedPackage.getPackageName(), null));
            intent.putExtra("android.content.pm.extra.CALLBACK", adapter.getBinder().asBinder());
            adapter.onUserActionRequired(intent);
        }
    }

    public void setPermissionsResult(int sessionId, boolean accepted) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
        synchronized (this.mSessions) {
            PackageInstallerSession session = (PackageInstallerSession) this.mSessions.get(sessionId);
            if (session != null) {
                session.setPermissionsResult(accepted);
            }
        }
    }

    public void registerCallback(IPackageInstallerCallback callback, int userId) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "registerCallback");
        this.mCallbacks.register(callback, userId);
    }

    public void unregisterCallback(IPackageInstallerCallback callback) {
        this.mCallbacks.unregister(callback);
    }

    private static int getSessionCount(SparseArray<PackageInstallerSession> sessions, int installerUid) {
        int count = 0;
        int size = sessions.size();
        for (int i = 0; i < size; i++) {
            if (((PackageInstallerSession) sessions.valueAt(i)).getInstallerUid() == installerUid) {
                count++;
            }
        }
        return count;
    }

    private boolean isCallingUidOwner(PackageInstallerSession session) {
        int callingUid = Binder.getCallingUid();
        boolean z = true;
        if (callingUid == 0) {
            return true;
        }
        if (session == null || callingUid != session.getInstallerUid()) {
            z = false;
        }
        return z;
    }

    private static Notification buildSuccessNotification(Context context, String contentText, String basePackageName, int userId) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = AppGlobals.getPackageManager().getPackageInfo(basePackageName, 67108864, userId);
        } catch (RemoteException e) {
        }
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Notification not built for package: ");
            stringBuilder.append(basePackageName);
            Slog.w(str, stringBuilder.toString());
            return null;
        }
        PackageManager pm = context.getPackageManager();
        return new Builder(context, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302305).setColor(context.getResources().getColor(17170784)).setContentTitle(packageInfo.applicationInfo.loadLabel(pm)).setContentText(contentText).setStyle(new BigTextStyle().bigText(contentText)).setLargeIcon(ImageUtils.buildScaledBitmap(packageInfo.applicationInfo.loadIcon(pm), context.getResources().getDimensionPixelSize(17104901), context.getResources().getDimensionPixelSize(17104902))).build();
    }

    public static <E> ArraySet<E> newArraySet(E... elements) {
        ArraySet<E> set = new ArraySet();
        if (elements != null) {
            set.ensureCapacity(elements.length);
            Collections.addAll(set, elements);
        }
        return set;
    }

    void dump(IndentingPrintWriter pw) {
        synchronized (this.mSessions) {
            pw.println("Active install sessions:");
            pw.increaseIndent();
            int N = this.mSessions.size();
            int i = 0;
            for (int i2 = 0; i2 < N; i2++) {
                ((PackageInstallerSession) this.mSessions.valueAt(i2)).dump(pw);
                pw.println();
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Historical install sessions:");
            pw.increaseIndent();
            N = this.mHistoricalSessions.size();
            while (i < N) {
                pw.print((String) this.mHistoricalSessions.get(i));
                pw.println();
                i++;
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Legacy install sessions:");
            pw.increaseIndent();
            pw.println(this.mLegacySessions.toString());
            pw.decreaseIndent();
        }
    }
}
