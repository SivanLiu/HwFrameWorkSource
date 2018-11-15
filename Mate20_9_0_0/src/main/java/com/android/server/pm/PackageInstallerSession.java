package com.android.server.pm;

import android.app.admin.DevicePolicyManagerInternal;
import android.common.HwFrameworkFactory;
import android.common.HwFrameworkMonitor;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstallerSession.Stub;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionInfo;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.ApkLite;
import android.content.pm.PackageParser.PackageLite;
import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.dex.DexMetadataHelper;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileBridge;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.RemoteException;
import android.os.RevocableFileDescriptor;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.system.ErrnoException;
import android.system.Int64Ref;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.MathUtils;
import android.util.Slog;
import android.util.apk.ApkSignatureVerifier;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.NativeLibraryHelper.Handle;
import com.android.internal.content.PackageHelper;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.power.IHwShutdownThread;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageInstallerSession extends Stub {
    private static final String ATTR_ABI_OVERRIDE = "abiOverride";
    @Deprecated
    private static final String ATTR_APP_ICON = "appIcon";
    private static final String ATTR_APP_LABEL = "appLabel";
    private static final String ATTR_APP_PACKAGE_NAME = "appPackageName";
    private static final String ATTR_CREATED_MILLIS = "createdMillis";
    private static final String ATTR_INSTALLER_PACKAGE_NAME = "installerPackageName";
    private static final String ATTR_INSTALLER_UID = "installerUid";
    private static final String ATTR_INSTALL_FLAGS = "installFlags";
    private static final String ATTR_INSTALL_LOCATION = "installLocation";
    private static final String ATTR_INSTALL_REASON = "installRason";
    private static final String ATTR_MODE = "mode";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_ORIGINATING_UID = "originatingUid";
    private static final String ATTR_ORIGINATING_URI = "originatingUri";
    private static final String ATTR_PREPARED = "prepared";
    private static final String ATTR_REFERRER_URI = "referrerUri";
    private static final String ATTR_SEALED = "sealed";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_SESSION_STAGE_CID = "sessionStageCid";
    private static final String ATTR_SESSION_STAGE_DIR = "sessionStageDir";
    private static final String ATTR_SIZE_BYTES = "sizeBytes";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_VOLUME_UUID = "volumeUuid";
    private static final boolean LOGD = true;
    private static final int MSG_COMMIT = 1;
    private static final int MSG_EARLY_BIND = 0;
    private static final int MSG_ON_PACKAGE_INSTALLED = 2;
    private static final String PROPERTY_NAME_INHERIT_NATIVE = "pi.inherit_native_on_dont_kill";
    private static final String REMOVE_SPLIT_MARKER_EXTENSION = ".removed";
    private static final String TAG = "PackageInstaller";
    private static final String TAG_GRANTED_RUNTIME_PERMISSION = "granted-runtime-permission";
    static final String TAG_SESSION = "session";
    private static HwFrameworkMonitor mMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
    private static final FileFilter sAddedFilter = new FileFilter() {
        public boolean accept(File file) {
            if (file.isDirectory() || file.getName().endsWith(PackageInstallerSession.REMOVE_SPLIT_MARKER_EXTENSION) || DexMetadataHelper.isDexMetadataFile(file)) {
                return false;
            }
            return true;
        }
    };
    private static final FileFilter sRemovedFilter = new FileFilter() {
        public boolean accept(File file) {
            if (!file.isDirectory() && file.getName().endsWith(PackageInstallerSession.REMOVE_SPLIT_MARKER_EXTENSION)) {
                return true;
            }
            return false;
        }
    };
    final long createdMillis;
    final int defaultContainerGid;
    private final AtomicInteger mActiveCount = new AtomicInteger();
    @GuardedBy("mLock")
    private final ArrayList<FileBridge> mBridges;
    private final InternalCallback mCallback;
    @GuardedBy("mLock")
    private float mClientProgress = 0.0f;
    @GuardedBy("mLock")
    private boolean mCommitted;
    private final Context mContext;
    @GuardedBy("mLock")
    private boolean mDestroyed;
    @GuardedBy("mLock")
    private final ArrayList<RevocableFileDescriptor> mFds;
    @GuardedBy("mLock")
    private String mFinalMessage;
    @GuardedBy("mLock")
    private int mFinalStatus;
    private final Handler mHandler;
    private final Callback mHandlerCallback;
    private boolean mHwPermissionsAccepted;
    @GuardedBy("mLock")
    private File mInheritedFilesBase;
    @GuardedBy("mLock")
    private String mInstallerPackageName;
    @GuardedBy("mLock")
    private int mInstallerUid;
    @GuardedBy("mLock")
    private float mInternalProgress = 0.0f;
    private final Object mLock = new Object();
    private final int mOriginalInstallerUid;
    @GuardedBy("mLock")
    private String mPackageName;
    @GuardedBy("mLock")
    private boolean mPermissionsManuallyAccepted;
    private final PackageManagerService mPm;
    @GuardedBy("mLock")
    private boolean mPrepared;
    @GuardedBy("mLock")
    private float mProgress = 0.0f;
    @GuardedBy("mLock")
    private boolean mRelinquished;
    @GuardedBy("mLock")
    private IPackageInstallObserver2 mRemoteObserver;
    @GuardedBy("mLock")
    private float mReportedProgress = -1.0f;
    @GuardedBy("mLock")
    private File mResolvedBaseFile;
    @GuardedBy("mLock")
    private final List<File> mResolvedInheritedFiles;
    @GuardedBy("mLock")
    private final List<String> mResolvedInstructionSets;
    @GuardedBy("mLock")
    private final List<String> mResolvedNativeLibPaths;
    @GuardedBy("mLock")
    private File mResolvedStageDir;
    @GuardedBy("mLock")
    private final List<File> mResolvedStagedFiles;
    @GuardedBy("mLock")
    private boolean mSealed;
    @GuardedBy("mLock")
    private SigningDetails mSigningDetails;
    @GuardedBy("mLock")
    private long mVersionCode;
    final SessionParams params;
    final int sessionId;
    final String stageCid;
    final File stageDir;
    final int userId;

    private void earlyBindToDefContainer() {
        this.mPm.earlyBindToDefContainer();
    }

    @GuardedBy("mLock")
    private boolean isInstallerDeviceOwnerOrAffiliatedProfileOwnerLocked() {
        DevicePolicyManagerInternal dpmi = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        return dpmi != null && dpmi.isActiveAdminWithPolicy(this.mInstallerUid, -1) && dpmi.isUserAffiliatedWithDevice(this.userId);
    }

    @GuardedBy("mLock")
    private boolean needToAskForPermissionsLocked() {
        boolean z = false;
        if (this.mPermissionsManuallyAccepted) {
            return false;
        }
        boolean isInstallPermissionGranted = this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGES", this.mInstallerUid) == 0;
        boolean isSelfUpdatePermissionGranted = this.mPm.checkUidPermission("android.permission.INSTALL_SELF_UPDATES", this.mInstallerUid) == 0;
        boolean isUpdatePermissionGranted = this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGE_UPDATES", this.mInstallerUid) == 0;
        int targetPackageUid = this.mPm.getPackageUid(this.mPackageName, 0, this.userId);
        boolean isPermissionGranted = isInstallPermissionGranted || ((isUpdatePermissionGranted && targetPackageUid != -1) || (isSelfUpdatePermissionGranted && targetPackageUid == this.mInstallerUid));
        boolean isInstallerRoot = this.mInstallerUid == 0;
        boolean isInstallerSystem = this.mInstallerUid == 1000;
        if (((this.params.installFlags & 1024) != 0) || !(isPermissionGranted || isInstallerRoot || isInstallerSystem || isInstallerDeviceOwnerOrAffiliatedProfileOwnerLocked())) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x00b3 A:{Catch:{ PackageManagerException -> 0x00b3, PackageManagerException -> 0x00b3 }, Splitter: B:10:0x00ab, ExcHandler: com.android.server.pm.PackageManagerException (r0_8 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:15:0x00b3, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:16:0x00b4, code:
            r16 = r0;
            destroyInternal();
     */
    /* JADX WARNING: Missing block: B:17:0x00be, code:
            throw new java.lang.IllegalArgumentException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public PackageInstallerSession(InternalCallback callback, Context context, PackageManagerService pm, Looper looper, int sessionId, int userId, String installerPackageName, int installerUid, SessionParams params, long createdMillis, File stageDir, String stageCid, boolean prepared, boolean sealed) {
        Throwable th;
        long identity;
        int i = installerUid;
        SessionParams sessionParams = params;
        File file = stageDir;
        String str = stageCid;
        boolean z = false;
        this.mPrepared = false;
        this.mSealed = false;
        this.mCommitted = false;
        boolean z2 = true;
        this.mHwPermissionsAccepted = true;
        this.mRelinquished = false;
        this.mDestroyed = false;
        this.mPermissionsManuallyAccepted = false;
        this.mFds = new ArrayList();
        this.mBridges = new ArrayList();
        this.mResolvedStagedFiles = new ArrayList();
        this.mResolvedInheritedFiles = new ArrayList();
        this.mResolvedInstructionSets = new ArrayList();
        this.mResolvedNativeLibPaths = new ArrayList();
        this.mHandlerCallback = new Callback() {
            public boolean handleMessage(Message msg) {
                String completeMsg;
                switch (msg.what) {
                    case 0:
                        PackageInstallerSession.this.earlyBindToDefContainer();
                        break;
                    case 1:
                        synchronized (PackageInstallerSession.this.mLock) {
                            try {
                                PackageInstallerSession.this.commitLocked();
                            } catch (PackageManagerException e) {
                                completeMsg = ExceptionUtils.getCompleteMessage(e);
                                String str = PackageInstallerSession.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Commit of session ");
                                stringBuilder.append(PackageInstallerSession.this.sessionId);
                                stringBuilder.append(" failed: ");
                                stringBuilder.append(completeMsg);
                                Slog.e(str, stringBuilder.toString());
                                PackageInstallerSession.this.destroyInternal();
                                PackageInstallerSession.this.dispatchSessionFinished(e.error, completeMsg, null);
                            }
                        }
                    case 2:
                        SomeArgs args = msg.obj;
                        String packageName = args.arg1;
                        completeMsg = args.arg2;
                        Bundle extras = args.arg3;
                        IPackageInstallObserver2 observer = args.arg4;
                        int returnCode = args.argi1;
                        args.recycle();
                        try {
                            observer.onPackageInstalled(packageName, returnCode, completeMsg, extras);
                            break;
                        } catch (RemoteException e2) {
                            break;
                        }
                }
                return true;
            }
        };
        this.mCallback = callback;
        this.mContext = context;
        this.mPm = pm;
        this.mHandler = new Handler(looper, this.mHandlerCallback);
        this.sessionId = sessionId;
        this.userId = userId;
        this.mOriginalInstallerUid = i;
        this.mInstallerPackageName = installerPackageName;
        this.mInstallerUid = i;
        this.params = sessionParams;
        this.createdMillis = createdMillis;
        this.stageDir = file;
        this.stageCid = str;
        if (file != null) {
            z2 = false;
        }
        if (str == null) {
            z = true;
        }
        if (z2 != z) {
            this.mPrepared = prepared;
            if (sealed) {
                synchronized (this.mLock) {
                    try {
                        sealAndValidateLocked();
                    } catch (Exception e) {
                    }
                }
            }
            boolean isAdb = (sessionParams.installFlags & 32) != 0;
            if (isAdb) {
                z = SystemProperties.getBoolean("ro.config.hwRemoveADBMonitor", false) || HwAdbManager.autoPermitInstall();
                this.mHwPermissionsAccepted = z;
            }
            long identity2 = Binder.clearCallingIdentity();
            try {
                try {
                    this.defaultContainerGid = UserHandle.getSharedAppGid(this.mPm.getPackageUid(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, DumpState.DUMP_DEXOPT, 0));
                    Binder.restoreCallingIdentity(identity2);
                    if ((sessionParams.installFlags & 2048) != 0) {
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(0));
                        return;
                    }
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    identity = identity2;
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                boolean z3 = isAdb;
                identity = identity2;
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }
        z2 = prepared;
        throw new IllegalArgumentException("Exactly one of stageDir or stageCid stage must be set");
    }

    public SessionInfo generateInfo() {
        return generateInfo(true);
    }

    public SessionInfo generateInfo(boolean includeIcon) {
        SessionInfo info = new SessionInfo();
        synchronized (this.mLock) {
            info.sessionId = this.sessionId;
            info.installerPackageName = this.mInstallerPackageName;
            info.resolvedBaseCodePath = this.mResolvedBaseFile != null ? this.mResolvedBaseFile.getAbsolutePath() : null;
            info.progress = this.mProgress;
            info.sealed = this.mSealed;
            info.active = this.mActiveCount.get() > 0;
            info.mode = this.params.mode;
            info.installReason = this.params.installReason;
            info.sizeBytes = this.params.sizeBytes;
            info.appPackageName = this.params.appPackageName;
            if (includeIcon) {
                info.appIcon = this.params.appIcon;
            }
            info.appLabel = this.params.appLabel;
            info.installLocation = this.params.installLocation;
            info.originatingUri = this.params.originatingUri;
            info.originatingUid = this.params.originatingUid;
            info.referrerUri = this.params.referrerUri;
            info.grantedRuntimePermissions = this.params.grantedRuntimePermissions;
            info.installFlags = this.params.installFlags;
        }
        return info;
    }

    public boolean isPrepared() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPrepared;
        }
        return z;
    }

    public boolean isSealed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSealed;
        }
        return z;
    }

    @GuardedBy("mLock")
    private void assertPreparedAndNotSealedLocked(String cookie) {
        assertPreparedAndNotCommittedOrDestroyedLocked(cookie);
        if (this.mSealed) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(cookie);
            stringBuilder.append(" not allowed after sealing");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    private void assertPreparedAndNotCommittedOrDestroyedLocked(String cookie) {
        assertPreparedAndNotDestroyedLocked(cookie);
        if (this.mCommitted) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(cookie);
            stringBuilder.append(" not allowed after commit");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    private void assertPreparedAndNotDestroyedLocked(String cookie) {
        StringBuilder stringBuilder;
        if (!this.mPrepared) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(cookie);
            stringBuilder.append(" before prepared");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (this.mDestroyed) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(cookie);
            stringBuilder.append(" not allowed after destruction");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    private File resolveStageDirLocked() throws IOException {
        if (this.mResolvedStageDir == null) {
            if (this.stageDir != null) {
                this.mResolvedStageDir = this.stageDir;
            } else {
                throw new IOException("Missing stageDir");
            }
        }
        return this.mResolvedStageDir;
    }

    public void setClientProgress(float progress) {
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            boolean forcePublish = this.mClientProgress == 0.0f;
            this.mClientProgress = progress;
            computeProgressLocked(forcePublish);
        }
    }

    public void addClientProgress(float progress) {
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            setClientProgress(this.mClientProgress + progress);
        }
    }

    @GuardedBy("mLock")
    private void computeProgressLocked(boolean forcePublish) {
        this.mProgress = MathUtils.constrain(this.mClientProgress * 0.8f, 0.0f, 0.8f) + MathUtils.constrain(this.mInternalProgress * 0.2f, 0.0f, 0.2f);
        if (forcePublish || ((double) Math.abs(this.mProgress - this.mReportedProgress)) >= 0.01d) {
            this.mReportedProgress = this.mProgress;
            this.mCallback.onSessionProgressChanged(this, this.mProgress);
        }
    }

    public String[] getNames() {
        String[] list;
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotCommittedOrDestroyedLocked("getNames");
            try {
                list = resolveStageDirLocked().list();
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
        return list;
    }

    public void removeSplit(String splitName) {
        if (TextUtils.isEmpty(this.params.appPackageName)) {
            throw new IllegalStateException("Must specify package name to remove a split");
        }
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotCommittedOrDestroyedLocked("removeSplit");
            try {
                createRemoveSplitMarkerLocked(splitName);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
    }

    private void createRemoveSplitMarkerLocked(String splitName) throws IOException {
        try {
            String markerName = new StringBuilder();
            markerName.append(splitName);
            markerName.append(REMOVE_SPLIT_MARKER_EXTENSION);
            markerName = markerName.toString();
            if (FileUtils.isValidExtFilename(markerName)) {
                File target = new File(resolveStageDirLocked(), markerName);
                target.createNewFile();
                Os.chmod(target.getAbsolutePath(), 0);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid marker: ");
            stringBuilder.append(markerName);
            throw new IllegalArgumentException(stringBuilder.toString());
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public ParcelFileDescriptor openWrite(String name, long offsetBytes, long lengthBytes) {
        try {
            return doWriteInternal(name, offsetBytes, lengthBytes, null);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    public void write(String name, long offsetBytes, long lengthBytes, ParcelFileDescriptor fd) {
        try {
            doWriteInternal(name, offsetBytes, lengthBytes, fd);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:59:0x00f2 A:{SYNTHETIC, Splitter: B:59:0x00f2} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ParcelFileDescriptor doWriteInternal(String name, long offsetBytes, long lengthBytes, ParcelFileDescriptor incomingFd) throws IOException {
        RevocableFileDescriptor fd;
        FileBridge bridge;
        File stageDir;
        Throwable th;
        File file;
        long j;
        String str = name;
        long j2 = offsetBytes;
        long j3 = lengthBytes;
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotSealedLocked("openWrite");
            if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                fd = new RevocableFileDescriptor();
                bridge = null;
                this.mFds.add(fd);
            } else {
                fd = null;
                bridge = new FileBridge();
                this.mBridges.add(bridge);
            }
            stageDir = resolveStageDirLocked();
        }
        RevocableFileDescriptor fd2 = fd;
        FileBridge bridge2 = bridge;
        if (FileUtils.isValidExtFilename(name)) {
            long identity = Binder.clearCallingIdentity();
            try {
                File target = new File(stageDir, str);
                Binder.restoreCallingIdentity(identity);
                FileDescriptor targetFd = Os.open(target.getAbsolutePath(), OsConstants.O_CREAT | OsConstants.O_WRONLY, 420);
                Os.chmod(target.getAbsolutePath(), 420);
                if (stageDir != null && j3 > 0) {
                    ((StorageManager) this.mContext.getSystemService(StorageManager.class)).allocateBytes(targetFd, j3, PackageHelper.translateAllocateFlags(this.params.installFlags));
                }
                if (j2 > 0) {
                    Os.lseek(targetFd, j2, OsConstants.SEEK_SET);
                }
                FileDescriptor targetFd2;
                if (incomingFd != null) {
                    int callingUid = Binder.getCallingUid();
                    if (callingUid == 0 || callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
                        try {
                            Int64Ref last = new Int64Ref(0);
                            targetFd2 = targetFd;
                            try {
                                FileUtils.copy(incomingFd.getFileDescriptor(), targetFd, new -$$Lambda$PackageInstallerSession$0Oqu1oanLjaOBEcFPtJVCRQ0lHs(this, last), null, j3);
                                IoUtils.closeQuietly(targetFd2);
                                IoUtils.closeQuietly(incomingFd);
                                synchronized (this.mLock) {
                                    try {
                                        if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                                            this.mFds.remove(fd2);
                                        } else {
                                            this.mBridges.remove(bridge2);
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                }
                                return null;
                            } catch (Throwable th3) {
                                th = th3;
                                IoUtils.closeQuietly(targetFd2);
                                IoUtils.closeQuietly(incomingFd);
                                synchronized (this.mLock) {
                                    try {
                                        if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                                            this.mFds.remove(fd2);
                                        } else {
                                            this.mBridges.remove(bridge2);
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            targetFd2 = targetFd;
                            file = target;
                            j = identity;
                            IoUtils.closeQuietly(targetFd2);
                            IoUtils.closeQuietly(incomingFd);
                            synchronized (this.mLock) {
                            }
                            throw th;
                        }
                    }
                    throw new SecurityException("Reverse mode only supported from shell");
                }
                targetFd2 = targetFd;
                file = target;
                j = identity;
                if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                    fd2.init(this.mContext, targetFd2);
                    return fd2.getRevocableFileDescriptor();
                }
                bridge2.setTargetFile(targetFd2);
                bridge2.start();
                return new ParcelFileDescriptor(bridge2.getClientSocket());
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            } catch (Throwable th6) {
                Binder.restoreCallingIdentity(identity);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid name: ");
        stringBuilder.append(str);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$doWriteInternal$0(PackageInstallerSession packageInstallerSession, Int64Ref last, long progress) {
        if (packageInstallerSession.params.sizeBytes > 0) {
            long delta = progress - last.value;
            last.value = progress;
            packageInstallerSession.addClientProgress(((float) delta) / ((float) packageInstallerSession.params.sizeBytes));
        }
    }

    public ParcelFileDescriptor openRead(String name) {
        ParcelFileDescriptor openReadInternalLocked;
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotCommittedOrDestroyedLocked("openRead");
            try {
                openReadInternalLocked = openReadInternalLocked(name);
            } catch (IOException e) {
                throw ExceptionUtils.wrap(e);
            }
        }
        return openReadInternalLocked;
    }

    private ParcelFileDescriptor openReadInternalLocked(String name) throws IOException {
        try {
            if (FileUtils.isValidExtFilename(name)) {
                return new ParcelFileDescriptor(Os.open(new File(resolveStageDirLocked(), name).getAbsolutePath(), OsConstants.O_RDONLY, 0));
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid name: ");
            stringBuilder.append(name);
            throw new IllegalArgumentException(stringBuilder.toString());
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    @GuardedBy("mLock")
    private void assertCallerIsOwnerOrRootLocked() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != this.mInstallerUid) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Session does not belong to uid ");
            stringBuilder.append(callingUid);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    private void assertNoWriteFileTransfersOpenLocked() {
        Iterator it = this.mFds.iterator();
        while (it.hasNext()) {
            if (!((RevocableFileDescriptor) it.next()).isRevoked()) {
                throw new SecurityException("Files still open");
            }
        }
        it = this.mBridges.iterator();
        while (it.hasNext()) {
            if (!((FileBridge) it.next()).isClosed()) {
                throw new SecurityException("Files still open");
            }
        }
    }

    public void commit(IntentSender statusReceiver, boolean forTransfer) {
        boolean wasSealed;
        Preconditions.checkNotNull(statusReceiver);
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotDestroyedLocked("commit");
            this.mRemoteObserver = new PackageInstallObserverAdapter(this.mContext, statusReceiver, this.sessionId, isInstallerDeviceOwnerOrAffiliatedProfileOwnerLocked(), this.userId).getBinder();
            if (forTransfer) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
                if (this.mInstallerUid == this.mOriginalInstallerUid) {
                    throw new IllegalArgumentException("Session has not been transferred");
                }
            } else if (this.mInstallerUid != this.mOriginalInstallerUid) {
                throw new IllegalArgumentException("Session has been transferred");
            }
            wasSealed = this.mSealed;
            if (!this.mSealed) {
                try {
                    sealAndValidateLocked();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                } catch (PackageManagerException e2) {
                    destroyInternal();
                    dispatchSessionFinished(e2.error, ExceptionUtils.getCompleteMessage(e2), null);
                    return;
                }
            }
            this.mClientProgress = 1.0f;
            computeProgressLocked(true);
            this.mActiveCount.incrementAndGet();
            this.mCommitted = true;
            this.mHandler.obtainMessage(1).sendToTarget();
        }
        if (!wasSealed) {
            this.mCallback.onSessionSealedBlocking(this);
        }
    }

    @GuardedBy("mLock")
    private void sealAndValidateLocked() throws PackageManagerException, IOException {
        assertNoWriteFileTransfersOpenLocked();
        assertPreparedAndNotDestroyedLocked("sealing of session");
        PackageInfo pkgInfo = this.mPm.getPackageInfo(this.params.appPackageName, 67108928, this.userId);
        resolveStageDirLocked();
        this.mSealed = true;
        try {
            validateInstallLocked(pkgInfo);
        } catch (PackageManagerException e) {
            throw e;
        } catch (Throwable e2) {
            PackageManagerException packageManagerException = new PackageManagerException(e2);
        }
    }

    public void transfer(String packageName) {
        Preconditions.checkNotNull(packageName);
        ApplicationInfo newOwnerAppInfo = this.mPm.getApplicationInfo(packageName, 0, this.userId);
        if (newOwnerAppInfo == null) {
            throw new ParcelableException(new NameNotFoundException(packageName));
        } else if (this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGES", newOwnerAppInfo.uid) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Destination package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" does not have the ");
            stringBuilder.append("android.permission.INSTALL_PACKAGES");
            stringBuilder.append(" permission");
            throw new SecurityException(stringBuilder.toString());
        } else if (this.params.areHiddenOptionsSet()) {
            synchronized (this.mLock) {
                assertCallerIsOwnerOrRootLocked();
                assertPreparedAndNotSealedLocked("transfer");
                try {
                    sealAndValidateLocked();
                    if (this.mPackageName.equals(this.mInstallerPackageName)) {
                        this.mInstallerPackageName = packageName;
                        this.mInstallerUid = newOwnerAppInfo.uid;
                    } else {
                        throw new SecurityException("Can only transfer sessions that update the original installer");
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } catch (PackageManagerException e2) {
                    destroyInternal();
                    dispatchSessionFinished(e2.error, ExceptionUtils.getCompleteMessage(e2), null);
                    throw new IllegalArgumentException("Package is not valid", e2);
                }
            }
            this.mCallback.onSessionSealedBlocking(this);
        } else {
            throw new SecurityException("Can only transfer sessions that use public options");
        }
    }

    @GuardedBy("mLock")
    private void commitLocked() throws PackageManagerException {
        if (this.mDestroyed) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session destroyed");
        } else if (this.mSealed) {
            Preconditions.checkNotNull(this.mPackageName);
            Preconditions.checkNotNull(this.mSigningDetails);
            Preconditions.checkNotNull(this.mResolvedBaseFile);
            if (this.params.hdbEncode != null) {
                if (!HwAdbManager.startHdbVerification(this.params.hdbArgs, this.params.hdbArgIndex, this.params.hdbEncode)) {
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Failure [INSTALL_HDB_VERIFY_FAILED]");
                }
            } else if (!this.mHwPermissionsAccepted && HwAdbManager.startPackageInstallerForConfirm(this.mContext, this.sessionId)) {
                Slog.d(TAG, "start PackageInstallerActivity success, close current install!");
                closeInternal(false);
                return;
            }
            if (needToAskForPermissionsLocked()) {
                Intent intent = new Intent("android.content.pm.action.CONFIRM_PERMISSIONS");
                intent.setPackage(this.mContext.getPackageManager().getPermissionControllerPackageName());
                intent.putExtra("android.content.pm.extra.SESSION_ID", this.sessionId);
                try {
                    this.mRemoteObserver.onUserActionRequired(intent);
                } catch (RemoteException e) {
                }
                closeInternal(false);
                return;
            }
            UserHandle userHandle;
            if (this.params.mode == 2) {
                try {
                    List<File> fromFiles = this.mResolvedInheritedFiles;
                    File toDir = resolveStageDirLocked();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Inherited files: ");
                    stringBuilder.append(this.mResolvedInheritedFiles);
                    Slog.d(str, stringBuilder.toString());
                    if (!this.mResolvedInheritedFiles.isEmpty() && this.mInheritedFilesBase == null) {
                        throw new IllegalStateException("mInheritedFilesBase == null");
                    } else if (isLinkPossible(fromFiles, toDir)) {
                        if (!this.mResolvedInstructionSets.isEmpty()) {
                            createOatDirs(this.mResolvedInstructionSets, new File(toDir, "oat"));
                        }
                        if (!this.mResolvedNativeLibPaths.isEmpty()) {
                            for (String libPath : this.mResolvedNativeLibPaths) {
                                int splitIndex = libPath.lastIndexOf(47);
                                if (splitIndex < 0 || splitIndex >= libPath.length() - 1) {
                                    String str2 = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Skipping native library creation for linking due to invalid path: ");
                                    stringBuilder2.append(libPath);
                                    Slog.e(str2, stringBuilder2.toString());
                                } else {
                                    File libDir = new File(toDir, libPath.substring(1, splitIndex));
                                    if (!libDir.exists()) {
                                        NativeLibraryHelper.createNativeLibrarySubdir(libDir);
                                    }
                                    NativeLibraryHelper.createNativeLibrarySubdir(new File(libDir, libPath.substring(splitIndex + 1)));
                                }
                            }
                        }
                        linkFiles(fromFiles, toDir, this.mInheritedFilesBase);
                    } else {
                        copyFiles(fromFiles, toDir);
                    }
                } catch (IOException e2) {
                    throw new PackageManagerException(-4, "Failed to inherit existing install", e2);
                }
            }
            this.mInternalProgress = 0.5f;
            computeProgressLocked(true);
            extractNativeLibraries(this.mResolvedStageDir, this.params.abiOverride, mayInheritNativeLibs());
            IPackageInstallObserver2 localObserver = new IPackageInstallObserver2.Stub() {
                public void onUserActionRequired(Intent intent) {
                    throw new IllegalStateException();
                }

                public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
                    PackageInstallerSession.this.destroyInternal();
                    PackageInstallerSession.this.dispatchSessionFinished(returnCode, msg, extras);
                }
            };
            if ((this.params.installFlags & 64) != 0) {
                userHandle = UserHandle.ALL;
            } else {
                userHandle = new UserHandle(this.userId);
            }
            UserHandle user = userHandle;
            this.mRelinquished = true;
            this.mPm.installStage(this.mPackageName, this.stageDir, localObserver, this.params, this.mInstallerPackageName, this.mInstallerUid, user, this.mSigningDetails);
        } else {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session not sealed");
        }
    }

    private static void maybeRenameFile(File from, File to) throws PackageManagerException {
        if (!from.equals(to) && !from.renameTo(to)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not rename file ");
            stringBuilder.append(from);
            stringBuilder.append(" to ");
            stringBuilder.append(to);
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, stringBuilder.toString());
        }
    }

    private boolean mayInheritNativeLibs() {
        return SystemProperties.getBoolean(PROPERTY_NAME_INHERIT_NATIVE, true) && this.params.mode == 2 && (this.params.installFlags & 1) != 0;
    }

    @GuardedBy("mLock")
    private void validateInstallLocked(PackageInfo pkgInfo) throws PackageManagerException {
        IOException iOException;
        PackageInfo packageInfo = pkgInfo;
        this.mPackageName = null;
        this.mVersionCode = -1;
        this.mSigningDetails = SigningDetails.UNKNOWN;
        this.mResolvedBaseFile = null;
        this.mResolvedStagedFiles.clear();
        this.mResolvedInheritedFiles.clear();
        try {
            resolveStageDirLocked();
            File[] removedFiles = this.mResolvedStageDir.listFiles(sRemovedFilter);
            List<String> removeSplitList = new ArrayList();
            if (!ArrayUtils.isEmpty(removedFiles)) {
                for (File removedFile : removedFiles) {
                    String fileName = removedFile.getName();
                    removeSplitList.add(fileName.substring(0, fileName.length() - REMOVE_SPLIT_MARKER_EXTENSION.length()));
                }
            }
            File[] addedFiles = this.mResolvedStageDir.listFiles(sAddedFilter);
            if (ArrayUtils.isEmpty(addedFiles) && removeSplitList.size() == 0) {
                throw new PackageManagerException(-2, "No packages staged");
            }
            File addedFile;
            File targetFile;
            File dexMetadataFile;
            File targetDexMetadataFile;
            StringBuilder stringBuilder;
            ArraySet<String> stagedSplits = new ArraySet();
            int length = addedFiles.length;
            int i = 0;
            while (i < length) {
                addedFile = addedFiles[i];
                try {
                    ApkLite apk = PackageParser.parseApkLite(addedFile, 32);
                    if (stagedSplits.add(apk.splitName)) {
                        String targetName;
                        if (this.mPackageName == null) {
                            this.mPackageName = apk.packageName;
                            this.mVersionCode = apk.getLongVersionCode();
                        }
                        if (this.mSigningDetails == SigningDetails.UNKNOWN) {
                            this.mSigningDetails = apk.signingDetails;
                        }
                        assertApkConsistentLocked(String.valueOf(addedFile), apk);
                        if (apk.splitName == null) {
                            targetName = "base.apk";
                        } else {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("split_");
                            stringBuilder2.append(apk.splitName);
                            stringBuilder2.append(".apk");
                            targetName = stringBuilder2.toString();
                        }
                        if (FileUtils.isValidExtFilename(targetName)) {
                            targetFile = new File(this.mResolvedStageDir, targetName);
                            maybeRenameFile(addedFile, targetFile);
                            if (apk.splitName == null) {
                                this.mResolvedBaseFile = targetFile;
                            }
                            this.mResolvedStagedFiles.add(targetFile);
                            dexMetadataFile = DexMetadataHelper.findDexMetadataForFile(addedFile);
                            if (dexMetadataFile != null) {
                                if (FileUtils.isValidExtFilename(dexMetadataFile.getName())) {
                                    targetDexMetadataFile = new File(this.mResolvedStageDir, DexMetadataHelper.buildDexMetadataPathForApk(targetName));
                                    this.mResolvedStagedFiles.add(targetDexMetadataFile);
                                    maybeRenameFile(dexMetadataFile, targetDexMetadataFile);
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Invalid filename: ");
                                    stringBuilder.append(dexMetadataFile);
                                    throw new PackageManagerException(-2, stringBuilder.toString());
                                }
                            }
                            i++;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid filename: ");
                            stringBuilder.append(targetName);
                            throw new PackageManagerException(-2, stringBuilder.toString());
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Split ");
                    stringBuilder.append(apk.splitName);
                    stringBuilder.append(" was defined multiple times");
                    throw new PackageManagerException(-2, stringBuilder.toString());
                } catch (PackageParserException e) {
                    PackageParserException packageParserException = e;
                    throw PackageManagerException.from(e);
                }
            }
            if (removeSplitList.size() > 0) {
                if (packageInfo != null) {
                    for (String splitName : removeSplitList) {
                        if (!ArrayUtils.contains(packageInfo.splitNames, splitName)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Split not found: ");
                            stringBuilder.append(splitName);
                            throw new PackageManagerException(-2, stringBuilder.toString());
                        }
                    }
                    if (this.mPackageName == null) {
                        this.mPackageName = packageInfo.packageName;
                        this.mVersionCode = pkgInfo.getLongVersionCode();
                    }
                    if (this.mSigningDetails == SigningDetails.UNKNOWN) {
                        try {
                            this.mSigningDetails = ApkSignatureVerifier.plsCertsNoVerifyOnlyCerts(packageInfo.applicationInfo.sourceDir, 1);
                        } catch (PackageParserException e2) {
                            throw new PackageManagerException(-2, "Couldn't obtain signatures from base APK");
                        }
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Missing existing base package for ");
                stringBuilder.append(this.mPackageName);
                throw new PackageManagerException(-2, stringBuilder.toString());
            }
            File[] fileArr;
            List<String> list;
            if (this.params.mode == 1) {
                if (stagedSplits.contains(null)) {
                    fileArr = removedFiles;
                    list = removeSplitList;
                    return;
                }
                throw new PackageManagerException(-2, "Full install must include a base package");
            } else if (packageInfo == null || packageInfo.applicationInfo == null) {
                list = removeSplitList;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Missing existing base package for ");
                stringBuilder3.append(this.mPackageName);
                throw new PackageManagerException(-2, stringBuilder3.toString());
            } else {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                try {
                    File[] archSubdirs;
                    int length2;
                    PackageLite existing = PackageParser.parsePackageLite(new File(appInfo.getCodePath()), 0);
                    assertApkConsistentLocked("Existing base", PackageParser.parseApkLite(new File(appInfo.getBaseCodePath()), 32));
                    if (this.mResolvedBaseFile == null) {
                        this.mResolvedBaseFile = new File(appInfo.getBaseCodePath());
                        this.mResolvedInheritedFiles.add(this.mResolvedBaseFile);
                        targetDexMetadataFile = DexMetadataHelper.findDexMetadataForFile(this.mResolvedBaseFile);
                        if (targetDexMetadataFile != null) {
                            this.mResolvedInheritedFiles.add(targetDexMetadataFile);
                        }
                    }
                    if (!ArrayUtils.isEmpty(existing.splitNames)) {
                        for (int i2 = 0; i2 < existing.splitNames.length; i2++) {
                            String splitName2 = existing.splitNames[i2];
                            addedFile = new File(existing.splitCodePaths[i2]);
                            boolean splitRemoved = removeSplitList.contains(splitName2);
                            if (!(stagedSplits.contains(splitName2) || splitRemoved)) {
                                this.mResolvedInheritedFiles.add(addedFile);
                                targetFile = DexMetadataHelper.findDexMetadataForFile(addedFile);
                                if (targetFile != null) {
                                    this.mResolvedInheritedFiles.add(targetFile);
                                }
                            }
                        }
                    }
                    File packageInstallDir = new File(appInfo.getBaseCodePath()).getParentFile();
                    this.mInheritedFilesBase = packageInstallDir;
                    addedFile = new File(packageInstallDir, "oat");
                    if (addedFile.exists()) {
                        archSubdirs = addedFile.listFiles();
                        if (archSubdirs != null && archSubdirs.length > 0) {
                            String[] instructionSets = InstructionSets.getAllDexCodeInstructionSets();
                            length2 = archSubdirs.length;
                            int i3 = 0;
                            while (i3 < length2) {
                                File archSubDir = archSubdirs[i3];
                                File[] archSubdirs2 = archSubdirs;
                                if (ArrayUtils.contains(instructionSets, archSubDir.getName())) {
                                    this.mResolvedInstructionSets.add(archSubDir.getName());
                                    List<File> oatFiles = Arrays.asList(archSubDir.listFiles());
                                    if (!oatFiles.isEmpty()) {
                                        this.mResolvedInheritedFiles.addAll(oatFiles);
                                    }
                                }
                                i3++;
                                archSubdirs = archSubdirs2;
                                packageInfo = pkgInfo;
                            }
                        }
                    }
                    if (mayInheritNativeLibs() && removeSplitList.isEmpty()) {
                        File[] libDirs = new File[]{new File(packageInstallDir, "lib"), new File(packageInstallDir, "lib64")};
                        int length3 = libDirs.length;
                        length2 = 0;
                        while (length2 < length3) {
                            File[] libDirs2;
                            dexMetadataFile = libDirs[length2];
                            if (!dexMetadataFile.exists()) {
                                libDirs2 = libDirs;
                                fileArr = removedFiles;
                                list = removeSplitList;
                            } else if (dexMetadataFile.isDirectory()) {
                                List<File> libDirsToInherit = new LinkedList();
                                archSubdirs = dexMetadataFile.listFiles();
                                int length4 = archSubdirs.length;
                                libDirs2 = libDirs;
                                int libDirs3 = 0;
                                while (libDirs3 < length4) {
                                    File[] fileArr2;
                                    fileArr = removedFiles;
                                    removedFiles = archSubdirs[libDirs3];
                                    if (removedFiles.isDirectory()) {
                                        try {
                                            fileArr2 = archSubdirs;
                                            list = removeSplitList;
                                            String relLibPath = getRelativePath(removedFiles, packageInstallDir);
                                            if (!this.mResolvedNativeLibPaths.contains(relLibPath)) {
                                                this.mResolvedNativeLibPaths.add(relLibPath);
                                            }
                                            File[] fileArr3 = removedFiles;
                                            removedFiles = libDirsToInherit;
                                            removedFiles.addAll(Arrays.asList(removedFiles.listFiles()));
                                        } catch (IOException e3) {
                                            File archSubDir2 = removedFiles;
                                            list = removeSplitList;
                                            removedFiles = libDirsToInherit;
                                            iOException = e3;
                                            Slog.e(TAG, "Skipping linking of native library directory!", e3);
                                            removedFiles.clear();
                                        }
                                    } else {
                                        fileArr2 = archSubdirs;
                                        list = removeSplitList;
                                        removedFiles = libDirsToInherit;
                                    }
                                    libDirs3++;
                                    libDirsToInherit = removedFiles;
                                    removedFiles = fileArr;
                                    archSubdirs = fileArr2;
                                    removeSplitList = list;
                                }
                                fileArr = removedFiles;
                                list = removeSplitList;
                                removedFiles = libDirsToInherit;
                                this.mResolvedInheritedFiles.addAll(removedFiles);
                            } else {
                                libDirs2 = libDirs;
                                fileArr = removedFiles;
                                list = removeSplitList;
                            }
                            length2++;
                            libDirs = libDirs2;
                            removedFiles = fileArr;
                            removeSplitList = list;
                        }
                    }
                    list = removeSplitList;
                } catch (PackageParserException e4) {
                    fileArr = removedFiles;
                    list = removeSplitList;
                    throw PackageManagerException.from(e4);
                }
            }
        } catch (IOException e32) {
            iOException = e32;
            throw new PackageManagerException(-18, "Failed to resolve stage location", e32);
        }
    }

    @GuardedBy("mLock")
    private void assertApkConsistentLocked(String tag, ApkLite apk) throws PackageManagerException {
        StringBuilder stringBuilder;
        if (!this.mPackageName.equals(apk.packageName)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(" package ");
            stringBuilder.append(apk.packageName);
            stringBuilder.append(" inconsistent with ");
            stringBuilder.append(this.mPackageName);
            throw new PackageManagerException(-2, stringBuilder.toString());
        } else if (this.params.appPackageName != null && !this.params.appPackageName.equals(apk.packageName)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(" specified package ");
            stringBuilder.append(this.params.appPackageName);
            stringBuilder.append(" inconsistent with ");
            stringBuilder.append(apk.packageName);
            throw new PackageManagerException(-2, stringBuilder.toString());
        } else if (this.mVersionCode != apk.getLongVersionCode()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(" version code ");
            stringBuilder.append(apk.versionCode);
            stringBuilder.append(" inconsistent with ");
            stringBuilder.append(this.mVersionCode);
            throw new PackageManagerException(-2, stringBuilder.toString());
        } else if (!this.mSigningDetails.signaturesMatchExactly(apk.signingDetails)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(" signatures are inconsistent");
            throw new PackageManagerException(-2, stringBuilder.toString());
        }
    }

    private boolean isLinkPossible(List<File> fromFiles, File toDir) {
        try {
            StructStat toStat = Os.stat(toDir.getAbsolutePath());
            for (File fromFile : fromFiles) {
                if (Os.stat(fromFile.getAbsolutePath()).st_dev != toStat.st_dev) {
                    return false;
                }
            }
            return true;
        } catch (ErrnoException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to detect if linking possible: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    public int getInstallerUid() {
        int i;
        synchronized (this.mLock) {
            i = this.mInstallerUid;
        }
        return i;
    }

    private static String getRelativePath(File file, File base) throws IOException {
        String pathStr = file.getAbsolutePath();
        String baseStr = base.getAbsolutePath();
        StringBuilder stringBuilder;
        if (pathStr.contains("/.")) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid path (was relative) : ");
            stringBuilder.append(pathStr);
            throw new IOException(stringBuilder.toString());
        } else if (pathStr.startsWith(baseStr)) {
            return pathStr.substring(baseStr.length());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("File: ");
            stringBuilder.append(pathStr);
            stringBuilder.append(" outside base: ");
            stringBuilder.append(baseStr);
            throw new IOException(stringBuilder.toString());
        }
    }

    private void createOatDirs(List<String> instructionSets, File fromDir) throws PackageManagerException {
        for (String instructionSet : instructionSets) {
            try {
                this.mPm.mInstaller.createOatDir(fromDir.getAbsolutePath(), instructionSet);
            } catch (InstallerException e) {
                throw PackageManagerException.from(e);
            }
        }
    }

    private void linkFiles(List<File> fromFiles, File toDir, File fromDir) throws IOException {
        for (File fromFile : fromFiles) {
            String relativePath = getRelativePath(fromFile, fromDir);
            try {
                this.mPm.mInstaller.linkFile(relativePath, fromDir.getAbsolutePath(), toDir.getAbsolutePath());
            } catch (InstallerException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("failed linkOrCreateDir(");
                stringBuilder.append(relativePath);
                stringBuilder.append(", ");
                stringBuilder.append(fromDir);
                stringBuilder.append(", ");
                stringBuilder.append(toDir);
                stringBuilder.append(")");
                throw new IOException(stringBuilder.toString(), e);
            }
        }
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Linked ");
        stringBuilder2.append(fromFiles.size());
        stringBuilder2.append(" files into ");
        stringBuilder2.append(toDir);
        Slog.d(str, stringBuilder2.toString());
    }

    private static void copyFiles(List<File> fromFiles, File toDir) throws IOException {
        File file;
        for (File file2 : toDir.listFiles()) {
            if (file2.getName().endsWith(".tmp")) {
                file2.delete();
            }
        }
        for (File fromFile : fromFiles) {
            File tmpFile = File.createTempFile("inherit", ".tmp", toDir);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Copying ");
            stringBuilder.append(fromFile);
            stringBuilder.append(" to ");
            stringBuilder.append(tmpFile);
            Slog.d(str, stringBuilder.toString());
            if (FileUtils.copyFile(fromFile, tmpFile)) {
                try {
                    Os.chmod(tmpFile.getAbsolutePath(), 420);
                    file2 = new File(toDir, fromFile.getName());
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Renaming ");
                    stringBuilder2.append(tmpFile);
                    stringBuilder2.append(" to ");
                    stringBuilder2.append(file2);
                    Slog.d(str2, stringBuilder2.toString());
                    if (!tmpFile.renameTo(file2)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to rename ");
                        stringBuilder.append(tmpFile);
                        stringBuilder.append(" to ");
                        stringBuilder.append(file2);
                        throw new IOException(stringBuilder.toString());
                    }
                } catch (ErrnoException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to chmod ");
                    stringBuilder.append(tmpFile);
                    throw new IOException(stringBuilder.toString());
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Failed to copy ");
            stringBuilder3.append(fromFile);
            stringBuilder3.append(" to ");
            stringBuilder3.append(tmpFile);
            throw new IOException(stringBuilder3.toString());
        }
        String str3 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Copied ");
        stringBuilder4.append(fromFiles.size());
        stringBuilder4.append(" files into ");
        stringBuilder4.append(toDir);
        Slog.d(str3, stringBuilder4.toString());
    }

    private static void extractNativeLibraries(File packageDir, String abiOverride, boolean inherit) throws PackageManagerException {
        File libDir = new File(packageDir, "lib");
        if (!inherit) {
            NativeLibraryHelper.removeNativeBinariesFromDirLI(libDir, true);
        }
        Handle handle = null;
        try {
            handle = Handle.create(packageDir);
            int res = NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libDir, abiOverride);
            if (res == 1) {
                IoUtils.closeQuietly(handle);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to extract native libraries, res=");
            stringBuilder.append(res);
            throw new PackageManagerException(res, stringBuilder.toString());
        } catch (IOException e) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Failed to extract native libraries", e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(handle);
        }
    }

    void setPermissionsResult(boolean accepted) {
        if (!this.mSealed) {
            throw new SecurityException("Must be sealed to accept permissions");
        } else if (accepted) {
            synchronized (this.mLock) {
                this.mHwPermissionsAccepted = true;
                this.mPermissionsManuallyAccepted = true;
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        } else {
            destroyInternal();
            dispatchSessionFinished(-115, "User rejected permissions", null);
        }
    }

    public void open() throws IOException {
        boolean wasPrepared;
        if (this.mActiveCount.getAndIncrement() == 0) {
            this.mCallback.onSessionActiveChanged(this, true);
        }
        synchronized (this.mLock) {
            wasPrepared = this.mPrepared;
            if (!this.mPrepared) {
                if (this.stageDir != null) {
                    PackageInstallerService.prepareStageDir(this.stageDir);
                    this.mPrepared = true;
                } else {
                    throw new IllegalArgumentException("stageDir must be set");
                }
            }
        }
        if (!wasPrepared) {
            this.mCallback.onSessionPrepared(this);
        }
    }

    public void close() {
        closeInternal(true);
    }

    private void closeInternal(boolean checkCaller) {
        int activeCount;
        synchronized (this.mLock) {
            if (checkCaller) {
                assertCallerIsOwnerOrRootLocked();
            }
            activeCount = this.mActiveCount.decrementAndGet();
        }
        if (activeCount == 0) {
            this.mCallback.onSessionActiveChanged(this, false);
        }
    }

    public void abandon() {
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            if (this.mRelinquished) {
                Slog.d(TAG, "Ignoring abandon after commit relinquished control");
                return;
            }
            destroyInternal();
            dispatchSessionFinished(-115, "Session was abandoned", null);
        }
    }

    private void dispatchSessionFinished(int returnCode, String msg, Bundle extras) {
        IPackageInstallObserver2 observer;
        String packageName;
        synchronized (this.mLock) {
            this.mFinalStatus = returnCode;
            this.mFinalMessage = msg;
            observer = this.mRemoteObserver;
            packageName = this.mPackageName;
        }
        if (observer != null) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = packageName;
            args.arg2 = msg;
            args.arg3 = extras;
            args.arg4 = observer;
            args.argi1 = returnCode;
            this.mHandler.obtainMessage(2, args).sendToTarget();
        }
        boolean isNewInstall = false;
        boolean success = returnCode == 1;
        if (extras == null || !extras.getBoolean("android.intent.extra.REPLACING")) {
            isNewInstall = true;
        }
        if (success && isNewInstall) {
            this.mPm.sendSessionCommitBroadcast(generateInfo(), this.userId);
        }
        this.mCallback.onSessionFinished(this, success);
    }

    private void destroyInternal() {
        synchronized (this.mLock) {
            this.mSealed = true;
            this.mDestroyed = true;
            Iterator it = this.mFds.iterator();
            while (it.hasNext()) {
                ((RevocableFileDescriptor) it.next()).revoke();
            }
            it = this.mBridges.iterator();
            while (it.hasNext()) {
                ((FileBridge) it.next()).forceClose();
            }
        }
        if (this.stageDir != null) {
            try {
                this.mPm.mInstaller.rmPackageDir(this.stageDir.getAbsolutePath());
            } catch (InstallerException e) {
            }
        }
    }

    void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            dumpLocked(pw);
        }
    }

    @GuardedBy("mLock")
    private void dumpLocked(IndentingPrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Session ");
        stringBuilder.append(this.sessionId);
        stringBuilder.append(":");
        pw.println(stringBuilder.toString());
        pw.increaseIndent();
        pw.printPair(ATTR_USER_ID, Integer.valueOf(this.userId));
        pw.printPair("mOriginalInstallerUid", Integer.valueOf(this.mOriginalInstallerUid));
        pw.printPair("mInstallerPackageName", this.mInstallerPackageName);
        pw.printPair("mInstallerUid", Integer.valueOf(this.mInstallerUid));
        pw.printPair(ATTR_CREATED_MILLIS, Long.valueOf(this.createdMillis));
        pw.printPair("stageDir", this.stageDir);
        pw.printPair("stageCid", this.stageCid);
        pw.println();
        this.params.dump(pw);
        pw.printPair("mClientProgress", Float.valueOf(this.mClientProgress));
        pw.printPair("mProgress", Float.valueOf(this.mProgress));
        pw.printPair("mSealed", Boolean.valueOf(this.mSealed));
        pw.printPair("mPermissionsManuallyAccepted", Boolean.valueOf(this.mPermissionsManuallyAccepted));
        pw.printPair("mRelinquished", Boolean.valueOf(this.mRelinquished));
        pw.printPair("mDestroyed", Boolean.valueOf(this.mDestroyed));
        pw.printPair("mFds", Integer.valueOf(this.mFds.size()));
        pw.printPair("mBridges", Integer.valueOf(this.mBridges.size()));
        pw.printPair("mFinalStatus", Integer.valueOf(this.mFinalStatus));
        pw.printPair("mFinalMessage", this.mFinalMessage);
        pw.println();
        pw.decreaseIndent();
    }

    private static void writeGrantedRuntimePermissionsLocked(XmlSerializer out, String[] grantedRuntimePermissions) throws IOException {
        if (grantedRuntimePermissions != null) {
            for (String permission : grantedRuntimePermissions) {
                out.startTag(null, TAG_GRANTED_RUNTIME_PERMISSION);
                XmlUtils.writeStringAttribute(out, "name", permission);
                out.endTag(null, TAG_GRANTED_RUNTIME_PERMISSION);
            }
        }
    }

    private static File buildAppIconFile(int sessionId, File sessionsDir) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("app_icon.");
        stringBuilder.append(sessionId);
        stringBuilder.append(".png");
        return new File(sessionsDir, stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:37:0x016a, code:
            r9.endTag(null, TAG_SESSION);
     */
    /* JADX WARNING: Missing block: B:38:0x0170, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void write(XmlSerializer out, File sessionsDir) throws IOException {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            out.startTag(null, TAG_SESSION);
            XmlUtils.writeIntAttribute(out, ATTR_SESSION_ID, this.sessionId);
            XmlUtils.writeIntAttribute(out, ATTR_USER_ID, this.userId);
            XmlUtils.writeStringAttribute(out, ATTR_INSTALLER_PACKAGE_NAME, this.mInstallerPackageName);
            XmlUtils.writeIntAttribute(out, ATTR_INSTALLER_UID, this.mInstallerUid);
            XmlUtils.writeLongAttribute(out, ATTR_CREATED_MILLIS, this.createdMillis);
            if (this.stageDir != null) {
                XmlUtils.writeStringAttribute(out, ATTR_SESSION_STAGE_DIR, this.stageDir.getAbsolutePath());
            }
            if (this.stageCid != null) {
                XmlUtils.writeStringAttribute(out, ATTR_SESSION_STAGE_CID, this.stageCid);
            }
            XmlUtils.writeBooleanAttribute(out, ATTR_PREPARED, isPrepared());
            XmlUtils.writeBooleanAttribute(out, ATTR_SEALED, isSealed());
            XmlUtils.writeIntAttribute(out, ATTR_MODE, this.params.mode);
            XmlUtils.writeIntAttribute(out, ATTR_INSTALL_FLAGS, this.params.installFlags);
            XmlUtils.writeIntAttribute(out, ATTR_INSTALL_LOCATION, this.params.installLocation);
            XmlUtils.writeLongAttribute(out, ATTR_SIZE_BYTES, this.params.sizeBytes);
            XmlUtils.writeStringAttribute(out, ATTR_APP_PACKAGE_NAME, this.params.appPackageName);
            XmlUtils.writeStringAttribute(out, ATTR_APP_LABEL, this.params.appLabel);
            XmlUtils.writeUriAttribute(out, ATTR_ORIGINATING_URI, this.params.originatingUri);
            XmlUtils.writeIntAttribute(out, ATTR_ORIGINATING_UID, this.params.originatingUid);
            XmlUtils.writeUriAttribute(out, ATTR_REFERRER_URI, this.params.referrerUri);
            XmlUtils.writeStringAttribute(out, ATTR_ABI_OVERRIDE, this.params.abiOverride);
            XmlUtils.writeStringAttribute(out, ATTR_VOLUME_UUID, this.params.volumeUuid);
            XmlUtils.writeIntAttribute(out, ATTR_INSTALL_REASON, this.params.installReason);
            writeGrantedRuntimePermissionsLocked(out, this.params.grantedRuntimePermissions);
            File appIconFile = buildAppIconFile(this.sessionId, sessionsDir);
            if (this.params.appIcon == null && appIconFile.exists()) {
                appIconFile.delete();
            } else if (!(this.params.appIcon == null || appIconFile.lastModified() == this.params.appIconLastModified)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Writing changed icon ");
                stringBuilder.append(appIconFile);
                Slog.w(str, stringBuilder.toString());
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(appIconFile);
                    this.params.appIcon.compress(CompressFormat.PNG, 90, os);
                    IoUtils.closeQuietly(os);
                } catch (IOException e) {
                    try {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failed to write icon ");
                        stringBuilder2.append(appIconFile);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(e.getMessage());
                        Slog.w(str2, stringBuilder2.toString());
                    } finally {
                        IoUtils.closeQuietly(os);
                    }
                }
                this.params.appIconLastModified = appIconFile.lastModified();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x003f  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x003d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static String[] readGrantedRuntimePermissions(XmlPullParser in) throws IOException, XmlPullParserException {
        List<String> permissions = null;
        int outerDepth = in.getDepth();
        while (true) {
            int next = in.next();
            int type = next;
            if (next == 1 || (type == 3 && in.getDepth() <= outerDepth)) {
                if (permissions != null) {
                    return null;
                }
                String[] permissionsArray = new String[permissions.size()];
                permissions.toArray(permissionsArray);
                return permissionsArray;
            } else if (type != 3) {
                if (type != 4) {
                    if (TAG_GRANTED_RUNTIME_PERMISSION.equals(in.getName())) {
                        String permission = XmlUtils.readStringAttribute(in, "name");
                        if (permissions == null) {
                            permissions = new ArrayList();
                        }
                        permissions.add(permission);
                    }
                }
            }
        }
        if (permissions != null) {
        }
    }

    public static PackageInstallerSession readFromXml(XmlPullParser in, InternalCallback callback, Context context, PackageManagerService pm, Looper installerThread, File sessionsDir) throws IOException, XmlPullParserException {
        XmlPullParser xmlPullParser = in;
        int sessionId = XmlUtils.readIntAttribute(xmlPullParser, ATTR_SESSION_ID);
        int userId = XmlUtils.readIntAttribute(xmlPullParser, ATTR_USER_ID);
        String installerPackageName = XmlUtils.readStringAttribute(xmlPullParser, ATTR_INSTALLER_PACKAGE_NAME);
        PackageManagerService packageManagerService = pm;
        int installerUid = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALLER_UID, packageManagerService.getPackageUid(installerPackageName, 8192, userId));
        long createdMillis = XmlUtils.readLongAttribute(xmlPullParser, ATTR_CREATED_MILLIS);
        String stageDirRaw = XmlUtils.readStringAttribute(xmlPullParser, ATTR_SESSION_STAGE_DIR);
        File stageDir = stageDirRaw != null ? new File(stageDirRaw) : null;
        String stageCid = XmlUtils.readStringAttribute(xmlPullParser, ATTR_SESSION_STAGE_CID);
        boolean prepared = XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_PREPARED, true);
        boolean sealed = XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_SEALED);
        SessionParams params = new SessionParams(-1);
        params.mode = XmlUtils.readIntAttribute(xmlPullParser, ATTR_MODE);
        params.installFlags = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALL_FLAGS);
        params.installLocation = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALL_LOCATION);
        params.sizeBytes = XmlUtils.readLongAttribute(xmlPullParser, ATTR_SIZE_BYTES);
        params.appPackageName = XmlUtils.readStringAttribute(xmlPullParser, ATTR_APP_PACKAGE_NAME);
        params.appIcon = XmlUtils.readBitmapAttribute(xmlPullParser, ATTR_APP_ICON);
        params.appLabel = XmlUtils.readStringAttribute(xmlPullParser, ATTR_APP_LABEL);
        params.originatingUri = XmlUtils.readUriAttribute(xmlPullParser, ATTR_ORIGINATING_URI);
        params.originatingUid = XmlUtils.readIntAttribute(xmlPullParser, ATTR_ORIGINATING_UID, -1);
        params.referrerUri = XmlUtils.readUriAttribute(xmlPullParser, ATTR_REFERRER_URI);
        params.abiOverride = XmlUtils.readStringAttribute(xmlPullParser, ATTR_ABI_OVERRIDE);
        params.volumeUuid = XmlUtils.readStringAttribute(xmlPullParser, ATTR_VOLUME_UUID);
        params.installReason = XmlUtils.readIntAttribute(xmlPullParser, ATTR_INSTALL_REASON);
        params.grantedRuntimePermissions = readGrantedRuntimePermissions(in);
        File appIconFile = buildAppIconFile(sessionId, sessionsDir);
        if (appIconFile.exists()) {
            params.appIcon = BitmapFactory.decodeFile(appIconFile.getAbsolutePath());
            params.appIconLastModified = appIconFile.lastModified();
        }
        return new PackageInstallerSession(callback, context, packageManagerService, installerThread, sessionId, userId, installerPackageName, installerUid, params, createdMillis, stageDir, stageCid, prepared, sealed);
    }

    private static void uploadInstallErrRadar(String reason) {
        Bundle data = new Bundle();
        data.putString("package", "PMS");
        data.putString(HwBroadcastRadarUtil.KEY_VERSION_NAME, "0");
        data.putString("extra", reason);
        if (mMonitor != null) {
            mMonitor.monitor(907400000, data);
        }
    }
}
