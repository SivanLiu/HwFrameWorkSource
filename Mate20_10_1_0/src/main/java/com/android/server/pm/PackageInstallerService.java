package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PackageDeleteObserver;
import android.app.PackageInstallObserver;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManagerInternal;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageInstallerCallback;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
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
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import huawei.android.security.IHwBehaviorCollectManager;
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
import java.util.function.IntPredicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageInstallerService extends IPackageInstaller.Stub implements PackageSessionProvider {
    private static final boolean LOGD = false;
    private static final long MAX_ACTIVE_SESSIONS = 1024;
    private static final long MAX_AGE_MILLIS = 259200000;
    private static final long MAX_HISTORICAL_SESSIONS = 1048576;
    private static final long MAX_TIME_SINCE_UPDATE_MILLIS = 604800000;
    private static final String TAG = "PackageInstaller";
    private static final String TAG_SESSIONS = "sessions";
    private static final FilenameFilter sStageFilter = new FilenameFilter() {
        /* class com.android.server.pm.PackageInstallerService.AnonymousClass1 */

        public boolean accept(File dir, String name) {
            return PackageInstallerService.isStageName(name);
        }
    };
    @GuardedBy({"mSessions"})
    private final SparseBooleanArray mAllocatedSessions = new SparseBooleanArray();
    private final ApexManager mApexManager;
    private AppOpsManager mAppOps;
    /* access modifiers changed from: private */
    public final Callbacks mCallbacks;
    private final Context mContext;
    @GuardedBy({"mSessions"})
    private final List<String> mHistoricalSessions = new ArrayList();
    @GuardedBy({"mSessions"})
    private final SparseIntArray mHistoricalSessionsByInstaller = new SparseIntArray();
    /* access modifiers changed from: private */
    public final Handler mInstallHandler;
    private final HandlerThread mInstallThread;
    private final InternalCallback mInternalCallback = new InternalCallback();
    @GuardedBy({"mSessions"})
    private final SparseBooleanArray mLegacySessions = new SparseBooleanArray();
    /* access modifiers changed from: private */
    public volatile boolean mOkToSendBroadcasts = false;
    private final PermissionManagerServiceInternal mPermissionManager;
    /* access modifiers changed from: private */
    public final PackageManagerService mPm;
    private final Random mRandom = new SecureRandom();
    /* access modifiers changed from: private */
    @GuardedBy({"mSessions"})
    public final SparseArray<PackageInstallerSession> mSessions = new SparseArray<>();
    private final File mSessionsDir;
    private final AtomicFile mSessionsFile;
    /* access modifiers changed from: private */
    public final StagingManager mStagingManager;

    public PackageInstallerService(Context context, PackageManagerService pm, ApexManager am) {
        this.mContext = context;
        this.mPm = pm;
        this.mPermissionManager = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
        this.mInstallThread = new HandlerThread(TAG);
        this.mInstallThread.start();
        this.mInstallHandler = new Handler(this.mInstallThread.getLooper());
        this.mCallbacks = new Callbacks(this.mInstallThread.getLooper());
        this.mSessionsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "install_sessions.xml"), "package-session");
        this.mSessionsDir = new File(Environment.getDataSystemDirectory(), "install_sessions");
        this.mSessionsDir.mkdirs();
        this.mApexManager = am;
        this.mStagingManager = new StagingManager(this, am, context);
    }

    /* access modifiers changed from: package-private */
    public boolean okToSendBroadcasts() {
        return this.mOkToSendBroadcasts;
    }

    public void systemReady() {
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        synchronized (this.mSessions) {
            readSessionsLocked();
            reconcileStagesLocked(StorageManager.UUID_PRIVATE_INTERNAL);
            ArraySet<File> unclaimedIcons = newArraySet(this.mSessionsDir.listFiles());
            for (int i = 0; i < this.mSessions.size(); i++) {
                unclaimedIcons.remove(buildAppIconFile(this.mSessions.valueAt(i).sessionId));
            }
            Iterator<File> it = unclaimedIcons.iterator();
            while (it.hasNext()) {
                File icon = it.next();
                Slog.w(TAG, "Deleting orphan icon " + icon);
                icon.delete();
            }
            writeSessionsLocked();
        }
    }

    /* access modifiers changed from: package-private */
    public void restoreAndApplyStagedSessionIfNeeded() {
        List<PackageInstallerSession> stagedSessionsToRestore = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (session.isStaged()) {
                    stagedSessionsToRestore.add(session);
                }
            }
        }
        for (PackageInstallerSession session2 : stagedSessionsToRestore) {
            this.mStagingManager.restoreSession(session2);
        }
        this.mOkToSendBroadcasts = true;
    }

    @GuardedBy({"mSessions"})
    private void reconcileStagesLocked(String volumeUuid) {
        ArraySet<File> unclaimedStages = newArraySet(getTmpSessionDir(volumeUuid).listFiles(sStageFilter));
        for (int i = 0; i < this.mSessions.size(); i++) {
            unclaimedStages.remove(this.mSessions.valueAt(i).stageDir);
        }
        Iterator<File> it = unclaimedStages.iterator();
        while (it.hasNext()) {
            File stage = it.next();
            Slog.w(TAG, "Deleting orphan stage " + stage);
            synchronized (this.mPm.mInstallLock) {
                this.mPm.removeCodePathLI(stage);
            }
        }
    }

    public void onPrivateVolumeMounted(String volumeUuid) {
        synchronized (this.mSessions) {
            reconcileStagesLocked(volumeUuid);
        }
    }

    public static boolean isStageName(String name) {
        return (name.startsWith("vmdl") && name.endsWith(".tmp")) || (name.startsWith("smdl") && name.endsWith(".tmp")) || name.startsWith("smdl2tmp");
    }

    @Deprecated
    public File allocateStageDirLegacy(String volumeUuid, boolean isEphemeral) throws IOException {
        File sessionStageDir;
        synchronized (this.mSessions) {
            try {
                int sessionId = allocateSessionIdLocked();
                this.mLegacySessions.put(sessionId, true);
                sessionStageDir = buildTmpSessionDir(sessionId, volumeUuid);
                prepareStageDir(sessionStageDir);
            } catch (IllegalStateException e) {
                throw new IOException(e);
            } catch (Throwable th) {
                throw th;
            }
        }
        return sessionStageDir;
    }

    @Deprecated
    public String allocateExternalStageCidLegacy() {
        String str;
        synchronized (this.mSessions) {
            int sessionId = allocateSessionIdLocked();
            this.mLegacySessions.put(sessionId, true);
            str = "smdl" + sessionId + ".tmp";
        }
        return str;
    }

    @GuardedBy({"mSessions"})
    private void readSessionsLocked() {
        boolean valid;
        this.mSessions.clear();
        FileInputStream fis = null;
        try {
            fis = this.mSessionsFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            while (true) {
                int type = in.next();
                if (type == 1) {
                    break;
                } else if (type == 2 && "session".equals(in.getName())) {
                    try {
                        PackageInstallerSession session = PackageInstallerSession.readFromXml(in, this.mInternalCallback, this.mContext, this.mPm, this.mInstallThread.getLooper(), this.mStagingManager, this.mSessionsDir, this);
                        try {
                            long age = System.currentTimeMillis() - session.createdMillis;
                            long timeSinceUpdate = System.currentTimeMillis() - session.getUpdatedMillis();
                            if (session.isStaged()) {
                                if (timeSinceUpdate < 604800000 || !session.isStagedAndInTerminalState()) {
                                    valid = true;
                                } else {
                                    valid = false;
                                }
                            } else if (age >= MAX_AGE_MILLIS) {
                                Slog.w(TAG, "Abandoning old session created at " + session.createdMillis);
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
                        } catch (IOException | XmlPullParserException e) {
                            Slog.wtf(TAG, "Failed reading install sessions", e);
                        } catch (Throwable th) {
                            IoUtils.closeQuietly(fis);
                            throw th;
                        }
                    } catch (Exception e2) {
                        Slog.e(TAG, "Could not read session", e2);
                    }
                }
            }
        } catch (FileNotFoundException e3) {
        }
        IoUtils.closeQuietly(fis);
        for (int i = 0; i < this.mSessions.size(); i++) {
            this.mSessions.valueAt(i).sealAndValidateIfNecessary();
        }
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mSessions"})
    public void addHistoricalSessionLocked(PackageInstallerSession session) {
        CharArrayWriter writer = new CharArrayWriter();
        session.dump(new IndentingPrintWriter(writer, "    "));
        this.mHistoricalSessions.add(writer.toString());
        int installerUid = session.getInstallerUid();
        SparseIntArray sparseIntArray = this.mHistoricalSessionsByInstaller;
        sparseIntArray.put(installerUid, sparseIntArray.get(installerUid) + 1);
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mSessions"})
    public void writeSessionsLocked() {
        try {
            FileOutputStream fos = this.mSessionsFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.startTag(null, TAG_SESSIONS);
            int size = this.mSessions.size();
            for (int i = 0; i < size; i++) {
                this.mSessions.valueAt(i).write(out, this.mSessionsDir);
            }
            out.endTag(null, TAG_SESSIONS);
            out.endDocument();
            this.mSessionsFile.finishWrite(fos);
        } catch (IOException e) {
            if (0 != 0) {
                this.mSessionsFile.failWrite(null);
            }
        }
    }

    /* access modifiers changed from: private */
    public File buildAppIconFile(int sessionId) {
        File file = this.mSessionsDir;
        return new File(file, "app_icon." + sessionId + ".png");
    }

    /* access modifiers changed from: private */
    public void writeSessionsAsync() {
        IoThread.getHandler().post(new Runnable() {
            /* class com.android.server.pm.PackageInstallerService.AnonymousClass2 */

            public void run() {
                synchronized (PackageInstallerService.this.mSessions) {
                    PackageInstallerService.this.writeSessionsLocked();
                }
            }
        });
    }

    public int createSession(PackageInstaller.SessionParams params, String installerPackageName, int userId) {
        try {
            return createSessionInternal(params, installerPackageName, userId);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:103:?, code lost:
        r34.mSessions.put(r3, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:0x0235, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:106:0x023a, code lost:
        if (r35.isStaged == false) goto L_0x0241;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:0x023c, code lost:
        r34.mStagingManager.createSession(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:109:0x0248, code lost:
        if ((r0.params.installFlags & com.android.server.pm.DumpState.DUMP_VOLUMES) != 0) goto L_0x0253;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x024a, code lost:
        com.android.server.pm.PackageInstallerService.Callbacks.access$200(r34.mCallbacks, r0.sessionId, r0.userId);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:111:0x0253, code lost:
        writeSessionsAsync();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:112:0x0256, code lost:
        return r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:113:0x0257, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:0x025b, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:118:?, code lost:
        monitor-exit(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:119:0x0261, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:120:0x0262, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:90:0x01c6, code lost:
        r28 = java.lang.System.currentTimeMillis();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:0x01ce, code lost:
        if (r35.isMultiPackage != false) goto L_0x01e8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:93:0x01d4, code lost:
        if ((r35.installFlags & 16) == 0) goto L_0x01df;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:94:0x01d6, code lost:
        r30 = buildSessionDir(r3, r35);
        r31 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:0x01df, code lost:
        r30 = null;
        r31 = buildExternalStageCid(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:96:0x01e8, code lost:
        r30 = null;
        r31 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:0x01ec, code lost:
        r0 = new com.android.server.pm.PackageInstallerSession(r34.mInternalCallback, r34.mContext, r34.mPm, r34, r34.mInstallThread.getLooper(), r34.mStagingManager, r3, r37, r36, r14, r35, r28, r30, r31, false, false, false, null, -1, false, false, false, 0, "");
        r4 = r34.mSessions;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:98:0x022d, code lost:
        monitor-enter(r4);
     */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x009b  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00ab  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00ad  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00c1  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00e2  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x01a5 A[SYNTHETIC, Splitter:B:81:0x01a5] */
    private int createSessionInternal(PackageInstaller.SessionParams params, String installerPackageName, int userId) throws IOException {
        boolean isApex;
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, userId, true, true, "createSession");
        if (!this.mPm.isUserRestricted(userId, "no_install_apps")) {
            if (callingUid != 2000) {
                if (callingUid != 0) {
                    if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
                        this.mAppOps.checkPackage(callingUid, installerPackageName);
                    }
                    params.installFlags &= -33;
                    params.installFlags &= -65;
                    params.installFlags &= -5;
                    params.installFlags |= 2;
                    if ((params.installFlags & 65536) != 0 && !this.mPm.isCallerVerifier(callingUid)) {
                        params.installFlags &= -65537;
                    }
                    if (!Build.IS_DEBUGGABLE || isDowngradeAllowedForCaller(callingUid)) {
                        params.installFlags |= DumpState.DUMP_DEXOPT;
                    } else {
                        params.installFlags &= -1048577;
                        params.installFlags &= -129;
                    }
                    if (callingUid != 1000) {
                        params.installFlags &= -524289;
                    }
                    isApex = (params.installFlags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) == 0;
                    if (params.isStaged || isApex) {
                        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
                    }
                    if (isApex) {
                        if (!this.mApexManager.isApexSupported()) {
                            throw new IllegalArgumentException("This device doesn't support the installation of APEX files");
                        } else if (!params.isStaged) {
                            throw new IllegalArgumentException("APEX files can only be installed as part of a staged session.");
                        }
                    }
                    if (!params.isMultiPackage) {
                        if ((params.installFlags & 256) == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") != -1) {
                            if (params.appIcon != null) {
                                int iconSize = ((ActivityManager) this.mContext.getSystemService("activity")).getLauncherLargeIconSize();
                                if (params.appIcon.getWidth() > iconSize * 2 || params.appIcon.getHeight() > iconSize * 2) {
                                    params.appIcon = Bitmap.createScaledBitmap(params.appIcon, iconSize, iconSize, true);
                                }
                            }
                            int i = params.mode;
                            if (i != 1 && i != 2) {
                                throw new IllegalArgumentException("Invalid install mode: " + params.mode);
                            } else if ((params.installFlags & 16) != 0) {
                                if (!PackageHelper.fitsOnInternal(this.mContext, params)) {
                                    throw new IOException("No suitable internal storage available");
                                }
                            } else if ((params.installFlags & 512) != 0) {
                                params.installFlags |= 16;
                            } else {
                                params.installFlags |= 16;
                                long ident = Binder.clearCallingIdentity();
                                try {
                                    SystemProperties.set("persist.sys.install_no_quota", "0");
                                    params.volumeUuid = PackageHelper.resolveInstallVolume(this.mContext, params);
                                } finally {
                                    SystemProperties.set("persist.sys.install_no_quota", "1");
                                    Binder.restoreCallingIdentity(ident);
                                }
                            }
                        } else {
                            throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
                        }
                    }
                    synchronized (this.mSessions) {
                        try {
                            if (((long) getSessionCount(this.mSessions, callingUid)) >= MAX_ACTIVE_SESSIONS) {
                                throw new IllegalStateException("Too many active sessions for UID " + callingUid);
                            } else if (((long) this.mHistoricalSessionsByInstaller.get(callingUid)) < MAX_HISTORICAL_SESSIONS) {
                                try {
                                    int sessionId = allocateSessionIdLocked();
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } else {
                                try {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Too many historical sessions for UID ");
                                    sb.append(callingUid);
                                    throw new IllegalStateException(sb.toString());
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    }
                }
            }
            params.installFlags |= 32;
            if (!Build.IS_DEBUGGABLE) {
            }
            params.installFlags |= DumpState.DUMP_DEXOPT;
            if (callingUid != 1000) {
            }
            if ((params.installFlags & DumpState.DUMP_INTENT_FILTER_VERIFIERS) == 0) {
            }
            this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
            if (isApex) {
            }
            if (!params.isMultiPackage) {
            }
            synchronized (this.mSessions) {
            }
        } else {
            throw new SecurityException("User restriction prevents installing");
        }
    }

    private boolean isDowngradeAllowedForCaller(int callingUid) {
        return callingUid == 1000 || callingUid == 0 || callingUid == 2000;
    }

    public void updateSessionAppIcon(int sessionId, Bitmap appIcon) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
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
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.params.appLabel = appLabel;
            this.mInternalCallback.onSessionBadgingChanged(session);
        }
    }

    public void abandonSession(int sessionId) {
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
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
            session = this.mSessions.get(sessionId);
            if (session == null || !isCallingUidOwner(session)) {
                throw new SecurityException("Caller has no access to session " + sessionId);
            }
            session.open();
        }
        return session;
    }

    @GuardedBy({"mSessions"})
    private int allocateSessionIdLocked() {
        int n = 0;
        while (true) {
            int sessionId = this.mRandom.nextInt(2147483646) + 1;
            if (!this.mAllocatedSessions.get(sessionId, false)) {
                this.mAllocatedSessions.put(sessionId, true);
                return sessionId;
            }
            int n2 = n + 1;
            if (n < 32) {
                n = n2;
            } else {
                throw new IllegalStateException("Failed to allocate session ID");
            }
        }
    }

    private File getTmpSessionDir(String volumeUuid) {
        return Environment.getDataAppDirectory(volumeUuid);
    }

    private File buildTmpSessionDir(int sessionId, String volumeUuid) {
        File sessionStagingDir = getTmpSessionDir(volumeUuid);
        return new File(sessionStagingDir, "vmdl" + sessionId + ".tmp");
    }

    private File buildSessionDir(int sessionId, PackageInstaller.SessionParams params) {
        if (!params.isStaged && !params.isHep) {
            return buildTmpSessionDir(sessionId, params.volumeUuid);
        }
        File sessionStagingDir = Environment.getDataStagingDirectory(params.volumeUuid);
        return new File(sessionStagingDir, "session_" + sessionId);
    }

    static void prepareStageDir(File stageDir) throws IOException {
        if (!stageDir.exists()) {
            try {
                Os.mkdir(stageDir.getAbsolutePath(), 509);
                Os.chmod(stageDir.getAbsolutePath(), 509);
                if (!SELinux.restorecon(stageDir)) {
                    throw new IOException("Failed to restorecon session dir: " + stageDir);
                }
            } catch (ErrnoException e) {
                throw new IOException("Failed to prepare session dir: " + stageDir, e);
            }
        } else {
            throw new IOException("Session dir already exists: " + stageDir);
        }
    }

    private String buildExternalStageCid(int sessionId) {
        return "smdl" + sessionId + ".tmp";
    }

    public PackageInstaller.SessionInfo getSessionInfo(int sessionId) {
        PackageInstaller.SessionInfo generateInfo;
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            generateInfo = session != null ? session.generateInfo() : null;
        }
        return generateInfo;
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getStagedSessions() {
        return this.mStagingManager.getSessions();
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getAllSessions(int userId) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getAllSessions");
        List<PackageInstaller.SessionInfo> result = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                if (session.userId == userId && !session.hasParentSessionId()) {
                    result.add(session.generateInfo(false));
                }
            }
        }
        return new ParceledListSlice<>(result);
    }

    public ParceledListSlice<PackageInstaller.SessionInfo> getMySessions(String installerPackageName, int userId) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getMySessions");
        this.mAppOps.checkPackage(Binder.getCallingUid(), installerPackageName);
        List<PackageInstaller.SessionInfo> result = new ArrayList<>();
        synchronized (this.mSessions) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                PackageInstallerSession session = this.mSessions.valueAt(i);
                PackageInstaller.SessionInfo info = session.generateInfo(false);
                if (Objects.equals(info.getInstallerPackageName(), installerPackageName) && session.userId == userId && !session.hasParentSessionId()) {
                    result.add(info);
                }
            }
        }
        return new ParceledListSlice<>(result);
    }

    /* JADX INFO: finally extract failed */
    public void uninstall(VersionedPackage versionedPackage, String callerPackageName, int flags, IntentSender statusReceiver, int userId) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(IHwBehaviorCollectManager.BehaviorId.PACKAGEINSTALLER_UNINSTALL);
        int callingUid = Binder.getCallingUid();
        this.mPermissionManager.enforceCrossUserPermission(callingUid, userId, true, true, "uninstall");
        if (!(callingUid == 2000 || callingUid == 0)) {
            this.mAppOps.checkPackage(callingUid, callerPackageName);
        }
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        boolean canSilentlyInstallPackage = dpmi != null && dpmi.canSilentlyInstallPackage(callerPackageName, callingUid);
        PackageDeleteObserverAdapter adapter = new PackageDeleteObserverAdapter(this.mContext, statusReceiver, versionedPackage.getPackageName(), canSilentlyInstallPackage, userId);
        String packageName = versionedPackage.getPackageName();
        if (this.mPm.getHwPMSEx() != null && this.mPm.getHwPMSEx().isNeedForbidShellFunc(packageName)) {
            try {
                IPackageDeleteObserver2 binder = adapter.getBinder();
                binder.onPackageDeleted(packageName, -5, packageName + " is not allowed to uninstall.");
            } catch (RemoteException e) {
                Slog.e(TAG, "Observer no longer exists.");
            }
        } else if (this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") == 0) {
            this.mPm.deletePackageVersioned(versionedPackage, adapter.getBinder(), userId, flags);
        } else if (canSilentlyInstallPackage) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mPm.deletePackageVersioned(versionedPackage, adapter.getBinder(), userId, flags);
                Binder.restoreCallingIdentity(ident);
                DevicePolicyEventLogger.createEvent((int) HdmiCecKeycode.CEC_KEYCODE_F1_BLUE).setAdmin(callerPackageName).write();
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            if (this.mPm.getApplicationInfo(callerPackageName, 0, userId).targetSdkVersion >= 28) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.REQUEST_DELETE_PACKAGES", null);
            }
            Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
            intent.setData(Uri.fromParts("package", versionedPackage.getPackageName(), null));
            intent.putExtra("android.content.pm.extra.CALLBACK", adapter.getBinder().asBinder());
            adapter.onUserActionRequired(intent);
        }
    }

    public void installExistingPackage(String packageName, int installFlags, int installReason, IntentSender statusReceiver, int userId, List<String> whiteListedPermissions) {
        this.mPm.installExistingPackageAsUser(packageName, userId, installFlags, installReason, whiteListedPermissions, statusReceiver);
    }

    public void setPermissionsResult(int sessionId, boolean accepted) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", TAG);
        synchronized (this.mSessions) {
            PackageInstallerSession session = this.mSessions.get(sessionId);
            if (session != null) {
                session.setPermissionsResult(accepted);
            }
        }
    }

    public void registerCallback(IPackageInstallerCallback callback, int userId) {
        this.mPermissionManager.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "registerCallback");
        registerCallback(callback, new IntPredicate(userId) {
            /* class com.android.server.pm.$$Lambda$PackageInstallerService$vra5ZkE3juVvcgDBu5xv0wVzno8 */
            private final /* synthetic */ int f$0;

            {
                this.f$0 = r1;
            }

            public final boolean test(int i) {
                return PackageInstallerService.lambda$registerCallback$0(this.f$0, i);
            }
        });
    }

    static /* synthetic */ boolean lambda$registerCallback$0(int userId, int eventUserId) {
        return userId == eventUserId;
    }

    public void registerCallback(IPackageInstallerCallback callback, IntPredicate userCheck) {
        this.mCallbacks.register(callback, userCheck);
    }

    public void unregisterCallback(IPackageInstallerCallback callback) {
        this.mCallbacks.unregister(callback);
    }

    @Override // com.android.server.pm.PackageSessionProvider
    public PackageInstallerSession getSession(int sessionId) {
        PackageInstallerSession packageInstallerSession;
        synchronized (this.mSessions) {
            packageInstallerSession = this.mSessions.get(sessionId);
        }
        return packageInstallerSession;
    }

    private static int getSessionCount(SparseArray<PackageInstallerSession> sessions, int installerUid) {
        int count = 0;
        int size = sessions.size();
        for (int i = 0; i < size; i++) {
            if (sessions.valueAt(i).getInstallerUid() == installerUid) {
                count++;
            }
        }
        return count;
    }

    private boolean isCallingUidOwner(PackageInstallerSession session) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0) {
            return true;
        }
        if (session == null || callingUid != session.getInstallerUid()) {
            return false;
        }
        return true;
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
                Context context2 = this.mContext;
                this.mNotification = PackageInstallerService.buildSuccessNotification(context2, context2.getResources().getString(17040714), packageName, userId);
                return;
            }
            this.mNotification = null;
        }

        public void onUserActionRequired(Intent intent) {
            if (this.mTarget != null) {
                Intent fillIn = new Intent();
                fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
                fillIn.putExtra("android.content.pm.extra.STATUS", -1);
                fillIn.putExtra("android.intent.extra.INTENT", intent);
                try {
                    this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
                } catch (IntentSender.SendIntentException e) {
                }
            }
        }

        public void onPackageDeleted(String basePackageName, int returnCode, String msg) {
            if (1 == returnCode && this.mNotification != null) {
                ((NotificationManager) this.mContext.getSystemService("notification")).notify(basePackageName, 21, this.mNotification);
            }
            if (this.mTarget != null) {
                Intent fillIn = new Intent();
                fillIn.putExtra("android.content.pm.extra.PACKAGE_NAME", this.mPackageName);
                fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.deleteStatusToPublicStatus(returnCode));
                fillIn.putExtra("android.content.pm.extra.STATUS_MESSAGE", PackageManager.deleteStatusToString(returnCode, msg));
                fillIn.putExtra("android.content.pm.extra.LEGACY_STATUS", returnCode);
                try {
                    this.mTarget.sendIntent(this.mContext, 0, fillIn, null, null);
                } catch (IntentSender.SendIntentException e) {
                }
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
            } catch (IntentSender.SendIntentException e) {
            }
        }

        public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
            int i;
            boolean update = true;
            if (1 == returnCode && this.mShowNotification) {
                if (extras == null || !extras.getBoolean("android.intent.extra.REPLACING")) {
                    update = false;
                }
                Context context = this.mContext;
                Resources resources = context.getResources();
                if (update) {
                    i = 17040716;
                } else {
                    i = 17040715;
                }
                Notification notification = PackageInstallerService.buildSuccessNotification(context, resources.getString(i), basePackageName, this.mUserId);
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
            } catch (IntentSender.SendIntentException e) {
            }
        }
    }

    /* access modifiers changed from: private */
    public static Notification buildSuccessNotification(Context context, String contentText, String basePackageName, int userId) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = AppGlobals.getPackageManager().getPackageInfo(basePackageName, (int) DumpState.DUMP_HANDLE, userId);
        } catch (RemoteException e) {
        }
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            Slog.w(TAG, "Notification not built for package: " + basePackageName);
            return null;
        }
        PackageManager pm = context.getPackageManager();
        return new Notification.Builder(context, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(17302340).setColor(context.getResources().getColor(17170460)).setContentTitle(packageInfo.applicationInfo.loadLabel(pm)).setContentText(contentText).setStyle(new Notification.BigTextStyle().bigText(contentText)).setLargeIcon(ImageUtils.buildScaledBitmap(packageInfo.applicationInfo.loadIcon(pm), context.getResources().getDimensionPixelSize(17104901), context.getResources().getDimensionPixelSize(17104902))).build();
    }

    public static <E> ArraySet<E> newArraySet(E... elements) {
        ArraySet<E> set = new ArraySet<>();
        if (elements != null) {
            set.ensureCapacity(elements.length);
            Collections.addAll(set, elements);
        }
        return set;
    }

    /* access modifiers changed from: private */
    public static class Callbacks extends Handler {
        private static final int MSG_SESSION_ACTIVE_CHANGED = 3;
        private static final int MSG_SESSION_BADGING_CHANGED = 2;
        private static final int MSG_SESSION_CREATED = 1;
        private static final int MSG_SESSION_FINISHED = 5;
        private static final int MSG_SESSION_PROGRESS_CHANGED = 4;
        private final RemoteCallbackList<IPackageInstallerCallback> mCallbacks = new RemoteCallbackList<>();

        public Callbacks(Looper looper) {
            super(looper);
        }

        public void register(IPackageInstallerCallback callback, IntPredicate userCheck) {
            this.mCallbacks.register(callback, userCheck);
        }

        public void unregister(IPackageInstallerCallback callback) {
            this.mCallbacks.unregister(callback);
        }

        public void handleMessage(Message msg) {
            int userId = msg.arg2;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IPackageInstallerCallback callback = this.mCallbacks.getBroadcastItem(i);
                if (((IntPredicate) this.mCallbacks.getBroadcastCookie(i)).test(userId)) {
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
            int i = msg.what;
            if (i == 1) {
                callback.onSessionCreated(sessionId);
            } else if (i == 2) {
                callback.onSessionBadgingChanged(sessionId);
            } else if (i == 3) {
                callback.onSessionActiveChanged(sessionId, ((Boolean) msg.obj).booleanValue());
            } else if (i == 4) {
                callback.onSessionProgressChanged(sessionId, ((Float) msg.obj).floatValue());
            } else if (i == 5) {
                callback.onSessionFinished(sessionId, ((Boolean) msg.obj).booleanValue());
            }
        }

        /* access modifiers changed from: private */
        public void notifySessionCreated(int sessionId, int userId) {
            obtainMessage(1, sessionId, userId).sendToTarget();
        }

        /* access modifiers changed from: private */
        public void notifySessionBadgingChanged(int sessionId, int userId) {
            obtainMessage(2, sessionId, userId).sendToTarget();
        }

        /* access modifiers changed from: private */
        public void notifySessionActiveChanged(int sessionId, int userId, boolean active) {
            obtainMessage(3, sessionId, userId, Boolean.valueOf(active)).sendToTarget();
        }

        /* access modifiers changed from: private */
        public void notifySessionProgressChanged(int sessionId, int userId, float progress) {
            obtainMessage(4, sessionId, userId, Float.valueOf(progress)).sendToTarget();
        }

        public void notifySessionFinished(int sessionId, int userId, boolean success) {
            obtainMessage(5, sessionId, userId, Boolean.valueOf(success)).sendToTarget();
        }
    }

    /* access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mSessions) {
            pw.println("Active install sessions:");
            pw.increaseIndent();
            int N = this.mSessions.size();
            for (int i = 0; i < N; i++) {
                this.mSessions.valueAt(i).dump(pw);
                pw.println();
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Historical install sessions:");
            pw.increaseIndent();
            int N2 = this.mHistoricalSessions.size();
            for (int i2 = 0; i2 < N2; i2++) {
                pw.print(this.mHistoricalSessions.get(i2));
                pw.println();
            }
            pw.println();
            pw.decreaseIndent();
            pw.println("Legacy install sessions:");
            pw.increaseIndent();
            pw.println(this.mLegacySessions.toString());
            pw.decreaseIndent();
        }
    }

    class InternalCallback {
        InternalCallback() {
        }

        public void onSessionBadgingChanged(PackageInstallerSession session) {
            if ((session.params.installFlags & DumpState.DUMP_VOLUMES) == 0) {
                PackageInstallerService.this.mCallbacks.notifySessionBadgingChanged(session.sessionId, session.userId);
            }
            PackageInstallerService.this.writeSessionsAsync();
        }

        public void onSessionActiveChanged(PackageInstallerSession session, boolean active) {
            if ((session.params.installFlags & DumpState.DUMP_VOLUMES) == 0) {
                PackageInstallerService.this.mCallbacks.notifySessionActiveChanged(session.sessionId, session.userId, active);
            }
        }

        public void onSessionProgressChanged(PackageInstallerSession session, float progress) {
            if ((session.params.installFlags & DumpState.DUMP_VOLUMES) == 0) {
                PackageInstallerService.this.mCallbacks.notifySessionProgressChanged(session.sessionId, session.userId, progress);
            }
        }

        public void onStagedSessionChanged(PackageInstallerSession session) {
            session.markUpdated();
            PackageInstallerService.this.writeSessionsAsync();
            if (PackageInstallerService.this.mOkToSendBroadcasts) {
                PackageInstallerService.this.mPm.sendSessionUpdatedBroadcast(session.generateInfo(false), session.userId);
            }
        }

        public void onSessionFinished(final PackageInstallerSession session, final boolean success) {
            if ((session.params.installFlags & DumpState.DUMP_VOLUMES) == 0) {
                PackageInstallerService.this.mCallbacks.notifySessionFinished(session.sessionId, session.userId, success);
            }
            PackageInstallerService.this.mInstallHandler.post(new Runnable() {
                /* class com.android.server.pm.PackageInstallerService.InternalCallback.AnonymousClass1 */

                public void run() {
                    if (session.isStaged() && !success) {
                        PackageInstallerService.this.mStagingManager.abortSession(session);
                    }
                    synchronized (PackageInstallerService.this.mSessions) {
                        if (!session.isStaged() || !success) {
                            PackageInstallerService.this.mSessions.remove(session.sessionId);
                        }
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
}
