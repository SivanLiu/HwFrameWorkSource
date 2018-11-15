package com.android.server.pm;

import android.app.admin.DevicePolicyManager;
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
import android.content.pm.Signature;
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
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.MathUtils;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.NativeLibraryHelper.Handle;
import com.android.internal.content.PackageHelper;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.radar.FrameworkRadar;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
import libcore.io.Libcore;
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
    private static final int MSG_COMMIT = 0;
    private static final int MSG_SESSION_FINISHED_WITH_EXCEPTION = 1;
    private static final String REMOVE_SPLIT_MARKER_EXTENSION = ".removed";
    private static final String TAG = "PackageInstaller";
    private static final String TAG_GRANTED_RUNTIME_PERMISSION = "granted-runtime-permission";
    static final String TAG_SESSION = "session";
    private static HwFrameworkMonitor mMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
    private static final FileFilter sAddedFilter = new FileFilter() {
        public boolean accept(File file) {
            if (file.isDirectory() || file.getName().endsWith(PackageInstallerSession.REMOVE_SPLIT_MARKER_EXTENSION)) {
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
    private final ArrayList<FileBridge> mBridges = new ArrayList();
    private final InternalCallback mCallback;
    @GuardedBy("mLock")
    private Certificate[][] mCertificates;
    @GuardedBy("mLock")
    private float mClientProgress = 0.0f;
    @GuardedBy("mLock")
    private boolean mCommitted = false;
    private final Context mContext;
    @GuardedBy("mLock")
    private boolean mDestroyed = false;
    @GuardedBy("mLock")
    private final ArrayList<RevocableFileDescriptor> mFds = new ArrayList();
    @GuardedBy("mLock")
    private String mFinalMessage;
    @GuardedBy("mLock")
    private int mFinalStatus;
    private final Handler mHandler;
    private final Callback mHandlerCallback = new Callback() {
        public boolean handleMessage(Message msg) {
            PackageManagerException e;
            switch (msg.what) {
                case 0:
                    synchronized (PackageInstallerSession.this.mLock) {
                        try {
                            PackageInstallerSession.this.commitLocked();
                        } catch (PackageManagerException e2) {
                            String completeMsg = ExceptionUtils.getCompleteMessage(e2);
                            Slog.e(PackageInstallerSession.TAG, "Commit of session " + PackageInstallerSession.this.sessionId + " failed: " + completeMsg);
                            PackageInstallerSession.this.destroyInternal();
                            PackageInstallerSession.this.dispatchSessionFinished(e2.error, completeMsg, null);
                        }
                    }
                    break;
                case 1:
                    e2 = (PackageManagerException) msg.obj;
                    PackageInstallerSession.this.dispatchSessionFinished(e2.error, ExceptionUtils.getCompleteMessage(e2), null);
                    break;
            }
            return true;
        }
    };
    private boolean mHwPermissionsAccepted = true;
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
    private boolean mPermissionsManuallyAccepted = false;
    private final PackageManagerService mPm;
    @GuardedBy("mLock")
    private boolean mPrepared = false;
    @GuardedBy("mLock")
    private float mProgress = 0.0f;
    @GuardedBy("mLock")
    private boolean mRelinquished = false;
    @GuardedBy("mLock")
    private IPackageInstallObserver2 mRemoteObserver;
    @GuardedBy("mLock")
    private float mReportedProgress = -1.0f;
    @GuardedBy("mLock")
    private File mResolvedBaseFile;
    @GuardedBy("mLock")
    private final List<File> mResolvedInheritedFiles = new ArrayList();
    @GuardedBy("mLock")
    private final List<String> mResolvedInstructionSets = new ArrayList();
    @GuardedBy("mLock")
    private File mResolvedStageDir;
    @GuardedBy("mLock")
    private final List<File> mResolvedStagedFiles = new ArrayList();
    @GuardedBy("mLock")
    private boolean mSealed = false;
    @GuardedBy("mLock")
    private Signature[] mSignatures;
    @GuardedBy("mLock")
    private int mVersionCode;
    final SessionParams params;
    final int sessionId;
    final String stageCid;
    final File stageDir;
    final int userId;

    private boolean isInstallerDeviceOwnerLocked() {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        return dpm != null ? dpm.isDeviceOwnerAppOnCallingUser(this.mInstallerPackageName) : false;
    }

    private boolean needToAskForPermissionsLocked() {
        boolean z = true;
        if (this.mPermissionsManuallyAccepted) {
            return false;
        }
        boolean isPermissionGranted = this.mPm.checkUidPermission("android.permission.INSTALL_PACKAGES", this.mInstallerUid) == 0;
        boolean isInstallerRoot = this.mInstallerUid == 0;
        if (!((this.params.installFlags & 1024) != 0)) {
            int isInstallerDeviceOwnerLocked;
            if (!(isPermissionGranted || isInstallerRoot)) {
                isInstallerDeviceOwnerLocked = isInstallerDeviceOwnerLocked();
            }
            z = isInstallerDeviceOwnerLocked ^ 1;
        }
        return z;
    }

    public PackageInstallerSession(InternalCallback callback, Context context, PackageManagerService pm, Looper looper, int sessionId, int userId, String installerPackageName, int installerUid, SessionParams params, long createdMillis, File stageDir, String stageCid, boolean prepared, boolean sealed) {
        this.mCallback = callback;
        this.mContext = context;
        this.mPm = pm;
        this.mHandler = new Handler(looper, this.mHandlerCallback);
        this.sessionId = sessionId;
        this.userId = userId;
        this.mOriginalInstallerUid = installerUid;
        this.mInstallerPackageName = installerPackageName;
        this.mInstallerUid = installerUid;
        this.params = params;
        this.createdMillis = createdMillis;
        this.stageDir = stageDir;
        this.stageCid = stageCid;
        if ((stageDir == null ? 1 : null) == (stageCid == null ? 1 : null)) {
            throw new IllegalArgumentException("Exactly one of stageDir or stageCid stage must be set");
        }
        this.mPrepared = prepared;
        if (sealed) {
            synchronized (this.mLock) {
                try {
                    sealAndValidateLocked();
                } catch (Exception e) {
                    destroyInternal();
                    throw new IllegalArgumentException(e);
                }
            }
        }
        if ((params.installFlags & 32) != 0) {
            boolean z;
            if (SystemProperties.getBoolean("ro.config.hwRemoveADBMonitor", false)) {
                z = true;
            } else {
                z = HwAdbManager.autoPermitInstall();
            }
            this.mHwPermissionsAccepted = z;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.defaultContainerGid = UserHandle.getSharedAppGid(this.mPm.getPackageUid("com.android.defcontainer", DumpState.DUMP_DEXOPT, 0));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public SessionInfo generateInfo() {
        return generateInfo(true);
    }

    public SessionInfo generateInfo(boolean includeIcon) {
        String str = null;
        boolean z = false;
        SessionInfo info = new SessionInfo();
        synchronized (this.mLock) {
            info.sessionId = this.sessionId;
            info.installerPackageName = this.mInstallerPackageName;
            if (this.mResolvedBaseFile != null) {
                str = this.mResolvedBaseFile.getAbsolutePath();
            }
            info.resolvedBaseCodePath = str;
            info.progress = this.mProgress;
            info.sealed = this.mSealed;
            if (this.mActiveCount.get() > 0) {
                z = true;
            }
            info.active = z;
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

    private void assertPreparedAndNotSealedLocked(String cookie) {
        assertPreparedAndNotCommittedOrDestroyedLocked(cookie);
        if (this.mSealed) {
            throw new SecurityException(cookie + " not allowed after sealing");
        }
    }

    private void assertPreparedAndNotCommittedOrDestroyedLocked(String cookie) {
        assertPreparedAndNotDestroyedLocked(cookie);
        if (this.mCommitted) {
            throw new SecurityException(cookie + " not allowed after commit");
        }
    }

    private void assertPreparedAndNotDestroyedLocked(String cookie) {
        if (!this.mPrepared) {
            throw new IllegalStateException(cookie + " before prepared");
        } else if (this.mDestroyed) {
            throw new SecurityException(cookie + " not allowed after destruction");
        }
    }

    private File resolveStageDirLocked() throws IOException {
        if (this.mResolvedStageDir == null) {
            if (this.stageDir != null) {
                this.mResolvedStageDir = this.stageDir;
            } else {
                String path = PackageHelper.getSdDir(this.stageCid);
                if (path != null) {
                    this.mResolvedStageDir = new File(path);
                } else {
                    throw new IOException("Failed to resolve path to container " + this.stageCid);
                }
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
            String markerName = splitName + REMOVE_SPLIT_MARKER_EXTENSION;
            if (FileUtils.isValidExtFilename(markerName)) {
                File target = new File(resolveStageDirLocked(), markerName);
                target.createNewFile();
                Os.chmod(target.getAbsolutePath(), 0);
                return;
            }
            throw new IllegalArgumentException("Invalid marker: " + markerName);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    public ParcelFileDescriptor openWrite(String name, long offsetBytes, long lengthBytes) {
        try {
            return openWriteInternal(name, offsetBytes, lengthBytes);
        } catch (IOException e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private ParcelFileDescriptor openWriteInternal(String name, long offsetBytes, long lengthBytes) throws IOException {
        FileBridge bridge;
        synchronized (this.mLock) {
            RevocableFileDescriptor fd;
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
            File stageDir = resolveStageDirLocked();
        }
        long identity;
        try {
            if (FileUtils.isValidExtFilename(name)) {
                identity = Binder.clearCallingIdentity();
                File target = new File(stageDir, name);
                Binder.restoreCallingIdentity(identity);
                FileDescriptor targetFd = Libcore.os.open(target.getAbsolutePath(), OsConstants.O_CREAT | OsConstants.O_WRONLY, 420);
                Os.chmod(target.getAbsolutePath(), 420);
                if (stageDir != null && lengthBytes > 0) {
                    ((StorageManager) this.mContext.getSystemService(StorageManager.class)).allocateBytes(targetFd, lengthBytes, PackageHelper.translateAllocateFlags(this.params.installFlags));
                }
                if (offsetBytes > 0) {
                    Libcore.os.lseek(targetFd, offsetBytes, OsConstants.SEEK_SET);
                }
                if (PackageInstaller.ENABLE_REVOCABLE_FD) {
                    fd.init(this.mContext, targetFd);
                    return fd.getRevocableFileDescriptor();
                }
                bridge.setTargetFile(targetFd);
                bridge.start();
                return new ParcelFileDescriptor(bridge.getClientSocket());
            }
            throw new IllegalArgumentException("Invalid name: " + name);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
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
                return new ParcelFileDescriptor(Libcore.os.open(new File(resolveStageDirLocked(), name).getAbsolutePath(), OsConstants.O_RDONLY, 0));
            }
            throw new IllegalArgumentException("Invalid name: " + name);
        } catch (ErrnoException e) {
            throw e.rethrowAsIOException();
        }
    }

    private void assertCallerIsOwnerOrRootLocked() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != this.mInstallerUid) {
            throw new SecurityException("Session does not belong to uid " + callingUid);
        }
    }

    private void assertNoWriteFileTransfersOpenLocked() {
        for (RevocableFileDescriptor fd : this.mFds) {
            if (!fd.isRevoked()) {
                throw new SecurityException("Files still open");
            }
        }
        for (FileBridge bridge : this.mBridges) {
            if (!bridge.isClosed()) {
                throw new SecurityException("Files still open");
            }
        }
    }

    public void commit(IntentSender statusReceiver, boolean forTransfer) {
        Preconditions.checkNotNull(statusReceiver);
        synchronized (this.mLock) {
            assertCallerIsOwnerOrRootLocked();
            assertPreparedAndNotDestroyedLocked("commit");
            this.mRemoteObserver = new PackageInstallObserverAdapter(this.mContext, statusReceiver, this.sessionId, isInstallerDeviceOwnerLocked(), this.userId).getBinder();
            if (forTransfer) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
                if (this.mInstallerUid == this.mOriginalInstallerUid) {
                    throw new IllegalArgumentException("Session has not been transferred");
                }
            } else if (this.mInstallerUid != this.mOriginalInstallerUid) {
                throw new IllegalArgumentException("Session has been transferred");
            }
            boolean wasSealed = this.mSealed;
            if (!this.mSealed) {
                try {
                    sealAndValidateLocked();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                } catch (PackageManagerException e2) {
                    destroyInternal();
                    this.mHandler.obtainMessage(1, e2).sendToTarget();
                    return;
                }
            }
            this.mClientProgress = 1.0f;
            computeProgressLocked(true);
            this.mActiveCount.incrementAndGet();
            this.mCommitted = true;
            this.mHandler.obtainMessage(0).sendToTarget();
        }
        if (!wasSealed) {
            this.mCallback.onSessionSealedBlocking(this);
        }
    }

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
            throw new SecurityException("Destination package " + packageName + " does not have " + "the " + "android.permission.INSTALL_PACKAGES" + " permission");
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

    private void commitLocked() throws PackageManagerException {
        if (this.mDestroyed) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session destroyed");
        } else if (this.mSealed) {
            Preconditions.checkNotNull(this.mPackageName);
            Preconditions.checkNotNull(this.mSignatures);
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
                intent = intent;
                intent.putExtra("android.content.pm.extra.SESSION_ID", this.sessionId);
                try {
                    this.mRemoteObserver.onUserActionRequired(intent);
                } catch (RemoteException e) {
                }
                closeInternal(false);
                return;
            }
            UserHandle user;
            if (this.stageCid != null) {
                resizeContainer(this.stageCid, calculateInstalledSize());
            }
            if (this.params.mode == 2) {
                try {
                    List<File> fromFiles = this.mResolvedInheritedFiles;
                    File toDir = resolveStageDirLocked();
                    Slog.d(TAG, "Inherited files: " + this.mResolvedInheritedFiles);
                    if (!this.mResolvedInheritedFiles.isEmpty() && this.mInheritedFilesBase == null) {
                        throw new IllegalStateException("mInheritedFilesBase == null");
                    } else if (isLinkPossible(fromFiles, toDir)) {
                        if (!this.mResolvedInstructionSets.isEmpty()) {
                            File file = new File(toDir, "oat");
                            createOatDirs(this.mResolvedInstructionSets, file);
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
            extractNativeLibraries(this.mResolvedStageDir, this.params.abiOverride);
            if (this.stageCid != null) {
                finalizeAndFixContainer(this.stageCid);
            }
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
                user = UserHandle.ALL;
            } else {
                user = new UserHandle(this.userId);
            }
            this.mRelinquished = true;
            this.mPm.installStage(this.mPackageName, this.stageDir, this.stageCid, localObserver, this.params, this.mInstallerPackageName, this.mInstallerUid, user, this.mCertificates);
        } else {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Session not sealed");
        }
    }

    private void validateInstallLocked(PackageInfo pkgInfo) throws PackageManagerException {
        this.mPackageName = null;
        this.mVersionCode = -1;
        this.mSignatures = null;
        this.mResolvedBaseFile = null;
        this.mResolvedStagedFiles.clear();
        this.mResolvedInheritedFiles.clear();
        try {
            int i;
            int length;
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
            File file;
            String splitName;
            ArraySet<String> stagedSplits = new ArraySet();
            i = 0;
            length = addedFiles.length;
            while (i < length) {
                File addedFile = addedFiles[i];
                int flags = 256;
                try {
                    if ((this.params.installFlags & 2048) != 0) {
                        flags = 2304;
                    }
                    ApkLite apk = PackageParser.parseApkLite(addedFile, flags);
                    if (stagedSplits.add(apk.splitName)) {
                        String targetName;
                        if (this.mPackageName == null) {
                            this.mPackageName = apk.packageName;
                            this.mVersionCode = apk.versionCode;
                        }
                        if (this.mSignatures == null) {
                            this.mSignatures = apk.signatures;
                            this.mCertificates = apk.certificates;
                        }
                        assertApkConsistentLocked(String.valueOf(addedFile), apk);
                        if (apk.splitName == null) {
                            targetName = "base.apk";
                        } else {
                            targetName = "split_" + apk.splitName + ".apk";
                        }
                        if (FileUtils.isValidExtFilename(targetName)) {
                            file = new File(this.mResolvedStageDir, targetName);
                            if (!addedFile.equals(file)) {
                                addedFile.renameTo(file);
                            }
                            if (apk.splitName == null) {
                                this.mResolvedBaseFile = file;
                            }
                            this.mResolvedStagedFiles.add(file);
                            i++;
                        } else {
                            throw new PackageManagerException(-2, "Invalid filename: " + targetName);
                        }
                    }
                    throw new PackageManagerException(-2, "Split " + apk.splitName + " was defined multiple times");
                } catch (PackageParserException e) {
                    throw PackageManagerException.from(e);
                }
            }
            if (removeSplitList.size() > 0) {
                if (pkgInfo == null) {
                    throw new PackageManagerException(-2, "Missing existing base package for " + this.mPackageName);
                }
                for (String splitName2 : removeSplitList) {
                    if (!ArrayUtils.contains(pkgInfo.splitNames, splitName2)) {
                        throw new PackageManagerException(-2, "Split not found: " + splitName2);
                    }
                }
                if (this.mPackageName == null) {
                    this.mPackageName = pkgInfo.packageName;
                    this.mVersionCode = pkgInfo.versionCode;
                }
                if (this.mSignatures == null) {
                    this.mSignatures = pkgInfo.signatures;
                }
            }
            if (this.params.mode == 1) {
                if (!stagedSplits.contains(null)) {
                    throw new PackageManagerException(-2, "Full install must include a base package");
                }
            } else if (pkgInfo == null || pkgInfo.applicationInfo == null) {
                throw new PackageManagerException(-2, "Missing existing base package for " + this.mPackageName);
            } else {
                ApplicationInfo appInfo = pkgInfo.applicationInfo;
                try {
                    PackageLite existing = PackageParser.parsePackageLite(new File(appInfo.getCodePath()), 0);
                    assertApkConsistentLocked("Existing base", PackageParser.parseApkLite(new File(appInfo.getBaseCodePath()), 256));
                    if (this.mResolvedBaseFile == null) {
                        this.mResolvedBaseFile = new File(appInfo.getBaseCodePath());
                        this.mResolvedInheritedFiles.add(this.mResolvedBaseFile);
                    }
                    if (!ArrayUtils.isEmpty(existing.splitNames)) {
                        for (int i2 = 0; i2 < existing.splitNames.length; i2++) {
                            splitName2 = existing.splitNames[i2];
                            file = new File(existing.splitCodePaths[i2]);
                            boolean splitRemoved = removeSplitList.contains(splitName2);
                            if (!(stagedSplits.contains(splitName2) || (splitRemoved ^ 1) == 0)) {
                                this.mResolvedInheritedFiles.add(file);
                            }
                        }
                    }
                    File packageInstallDir = new File(appInfo.getBaseCodePath()).getParentFile();
                    this.mInheritedFilesBase = packageInstallDir;
                    file = new File(packageInstallDir, "oat");
                    if (file.exists()) {
                        File[] archSubdirs = file.listFiles();
                        if (archSubdirs != null && archSubdirs.length > 0) {
                            String[] instructionSets = InstructionSets.getAllDexCodeInstructionSets();
                            for (File archSubDir : archSubdirs) {
                                if (ArrayUtils.contains(instructionSets, archSubDir.getName())) {
                                    this.mResolvedInstructionSets.add(archSubDir.getName());
                                    for (File oatFile : Arrays.asList(archSubDir.listFiles())) {
                                        if (oatFile.getName().equals("base.art") || oatFile.getName().equals("base.odex") || oatFile.getName().equals("base.vdex")) {
                                            this.mResolvedInheritedFiles.add(oatFile);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (PackageParserException e2) {
                    throw PackageManagerException.from(e2);
                }
            }
        } catch (IOException e3) {
            throw new PackageManagerException(-18, "Failed to resolve stage location", e3);
        }
    }

    private void assertApkConsistentLocked(String tag, ApkLite apk) throws PackageManagerException {
        if (!this.mPackageName.equals(apk.packageName)) {
            throw new PackageManagerException(-2, tag + " package " + apk.packageName + " inconsistent with " + this.mPackageName);
        } else if (this.params.appPackageName != null && (this.params.appPackageName.equals(apk.packageName) ^ 1) != 0) {
            throw new PackageManagerException(-2, tag + " specified package " + this.params.appPackageName + " inconsistent with " + apk.packageName);
        } else if (this.mVersionCode != apk.versionCode) {
            throw new PackageManagerException(-2, tag + " version code " + apk.versionCode + " inconsistent with " + this.mVersionCode);
        } else if (!Signature.areExactMatch(this.mSignatures, apk.signatures)) {
            throw new PackageManagerException(-2, tag + " signatures are inconsistent");
        }
    }

    private long calculateInstalledSize() throws PackageManagerException {
        Preconditions.checkNotNull(this.mResolvedBaseFile);
        try {
            ApkLite baseApk = PackageParser.parseApkLite(this.mResolvedBaseFile, 0);
            List<String> splitPaths = new ArrayList();
            for (File file : this.mResolvedStagedFiles) {
                if (!this.mResolvedBaseFile.equals(file)) {
                    splitPaths.add(file.getAbsolutePath());
                }
            }
            for (File file2 : this.mResolvedInheritedFiles) {
                if (!this.mResolvedBaseFile.equals(file2)) {
                    splitPaths.add(file2.getAbsolutePath());
                }
            }
            try {
                return PackageHelper.calculateInstalledSize(new PackageLite(null, baseApk, null, null, null, null, (String[]) splitPaths.toArray(new String[splitPaths.size()]), null), (this.params.installFlags & 1) != 0, this.params.abiOverride);
            } catch (IOException e) {
                throw new PackageManagerException(-2, "Failed to calculate install size", e);
            }
        } catch (PackageParserException e2) {
            throw PackageManagerException.from(e2);
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
            Slog.w(TAG, "Failed to detect if linking possible: " + e);
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
        if (pathStr.contains("/.")) {
            throw new IOException("Invalid path (was relative) : " + pathStr);
        } else if (pathStr.startsWith(baseStr)) {
            return pathStr.substring(baseStr.length());
        } else {
            throw new IOException("File: " + pathStr + " outside base: " + baseStr);
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
                throw new IOException("failed linkOrCreateDir(" + relativePath + ", " + fromDir + ", " + toDir + ")", e);
            }
        }
        Slog.d(TAG, "Linked " + fromFiles.size() + " files into " + toDir);
    }

    private static void copyFiles(List<File> fromFiles, File toDir) throws IOException {
        for (File file : toDir.listFiles()) {
            if (file.getName().endsWith(".tmp")) {
                file.delete();
            }
        }
        for (File fromFile : fromFiles) {
            File tmpFile = File.createTempFile("inherit", ".tmp", toDir);
            Slog.d(TAG, "Copying " + fromFile + " to " + tmpFile);
            if (FileUtils.copyFile(fromFile, tmpFile)) {
                try {
                    Os.chmod(tmpFile.getAbsolutePath(), 420);
                    File toFile = new File(toDir, fromFile.getName());
                    Slog.d(TAG, "Renaming " + tmpFile + " to " + toFile);
                    if (!tmpFile.renameTo(toFile)) {
                        throw new IOException("Failed to rename " + tmpFile + " to " + toFile);
                    }
                } catch (ErrnoException e) {
                    throw new IOException("Failed to chmod " + tmpFile);
                }
            }
            throw new IOException("Failed to copy " + fromFile + " to " + tmpFile);
        }
        Slog.d(TAG, "Copied " + fromFiles.size() + " files into " + toDir);
    }

    private static void extractNativeLibraries(File packageDir, String abiOverride) throws PackageManagerException {
        File libDir = new File(packageDir, "lib");
        NativeLibraryHelper.removeNativeBinariesFromDirLI(libDir, true);
        try {
            Handle handle = Handle.create(packageDir);
            int res = NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libDir, abiOverride);
            if (res != 1) {
                throw new PackageManagerException(res, "Failed to extract native libraries, res=" + res);
            }
            IoUtils.closeQuietly(handle);
        } catch (IOException e) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Failed to extract native libraries", e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
    }

    private static void resizeContainer(String cid, long targetSize) throws PackageManagerException {
        String path = PackageHelper.getSdDir(cid);
        if (path == null) {
            String reason = "rC;c(" + cid + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "PIS::rC", reason);
            uploadInstallErrRadar(reason);
            throw new PackageManagerException(-18, "Failed to find mounted " + cid);
        }
        long currentSize = new File(path).getTotalSpace();
        if (currentSize > targetSize) {
            Slog.w(TAG, "Current size " + currentSize + " is larger than target size " + targetSize + "; skipping resize");
        } else if (!PackageHelper.unMountSdDir(cid)) {
            reason = "PH:uMSD;c(" + cid + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "PIS::rC", reason);
            uploadInstallErrRadar(reason);
            throw new PackageManagerException(-18, "Failed to unmount " + cid + " before resize");
        } else if (!PackageHelper.resizeSdDir(targetSize, cid, PackageManagerService.getEncryptKey())) {
            reason = "PH:rSD;c(" + cid + ")tS(" + targetSize + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "PIS::rC", reason);
            uploadInstallErrRadar(reason);
            throw new PackageManagerException(-18, "Failed to resize " + cid + " to " + targetSize + " bytes");
        } else if (PackageHelper.mountSdDir(cid, PackageManagerService.getEncryptKey(), 1000, false) == null) {
            reason = "PH:mSD;c(" + cid + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "PIS::rC", reason);
            uploadInstallErrRadar(reason);
            throw new PackageManagerException(-18, "Failed to mount " + cid + " after resize");
        }
    }

    private void finalizeAndFixContainer(String cid) throws PackageManagerException {
        String reason;
        if (!PackageHelper.finalizeSdDir(cid)) {
            reason = "PH:fSD;c(" + cid + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "PIS::fAFC", reason);
            uploadInstallErrRadar(reason);
            throw new PackageManagerException(-18, "Failed to finalize container " + cid);
        } else if (!PackageHelper.fixSdPermissions(cid, this.defaultContainerGid, null)) {
            reason = "PH:fSP;c(" + cid + ")g(" + this.defaultContainerGid + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "PIS::fAFC", reason);
            uploadInstallErrRadar(reason);
            throw new PackageManagerException(-18, "Failed to fix permissions on container " + cid);
        }
    }

    void setPermissionsResult(boolean accepted) {
        if (!this.mSealed) {
            throw new SecurityException("Must be sealed to accept permissions");
        } else if (accepted) {
            synchronized (this.mLock) {
                this.mHwPermissionsAccepted = true;
                this.mPermissionsManuallyAccepted = true;
                this.mHandler.obtainMessage(0).sendToTarget();
            }
        } else {
            destroyInternal();
            dispatchSessionFinished(-115, "User rejected permissions", null);
        }
    }

    public void open() throws IOException {
        if (this.mActiveCount.getAndIncrement() == 0) {
            this.mCallback.onSessionActiveChanged(this, true);
        }
        synchronized (this.mLock) {
            boolean wasPrepared = this.mPrepared;
            if (!this.mPrepared) {
                if (this.stageDir != null) {
                    PackageInstallerService.prepareStageDir(this.stageDir);
                } else if (this.stageCid != null) {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        PackageInstallerService.prepareExternalStageCid(this.stageCid, this.params.sizeBytes);
                        this.mInternalProgress = 0.25f;
                        computeProgressLocked(true);
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                } else {
                    throw new IllegalArgumentException("Exactly one of stageDir or stageCid stage must be set");
                }
                this.mPrepared = true;
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
        synchronized (this.mLock) {
            if (checkCaller) {
                assertCallerIsOwnerOrRootLocked();
            }
            int activeCount = this.mActiveCount.decrementAndGet();
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
        synchronized (this.mLock) {
            this.mFinalStatus = returnCode;
            this.mFinalMessage = msg;
            IPackageInstallObserver2 observer = this.mRemoteObserver;
            String packageName = this.mPackageName;
        }
        if (observer != null) {
            try {
                observer.onPackageInstalled(packageName, returnCode, msg, extras);
            } catch (RemoteException e) {
            }
        }
        boolean success = returnCode == 1;
        int i = extras != null ? extras.getBoolean("android.intent.extra.REPLACING") ^ 1 : 1;
        if (success && i != 0) {
            this.mPm.sendSessionCommitBroadcast(generateInfo(), this.userId);
        }
        this.mCallback.onSessionFinished(this, success);
    }

    private void destroyInternal() {
        synchronized (this.mLock) {
            this.mSealed = true;
            this.mDestroyed = true;
            for (RevocableFileDescriptor fd : this.mFds) {
                fd.revoke();
            }
            for (FileBridge bridge : this.mBridges) {
                bridge.forceClose();
            }
        }
        if (this.stageDir != null) {
            try {
                this.mPm.mInstaller.rmPackageDir(this.stageDir.getAbsolutePath());
            } catch (InstallerException e) {
            }
        }
        if (this.stageCid != null) {
            PackageHelper.destroySdDir(this.stageCid);
        }
    }

    void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            dumpLocked(pw);
        }
    }

    private void dumpLocked(IndentingPrintWriter pw) {
        pw.println("Session " + this.sessionId + ":");
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
                XmlUtils.writeStringAttribute(out, ATTR_NAME, permission);
                out.endTag(null, TAG_GRANTED_RUNTIME_PERMISSION);
            }
        }
    }

    private static File buildAppIconFile(int sessionId, File sessionsDir) {
        return new File(sessionsDir, "app_icon." + sessionId + ".png");
    }

    void write(XmlSerializer out, File sessionsDir) throws IOException {
        IOException e;
        Object obj;
        Throwable th;
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
            File appIconFile = buildAppIconFile(this.sessionId, sessionsDir);
            if (this.params.appIcon == null && appIconFile.exists()) {
                appIconFile.delete();
            } else if (!(this.params.appIcon == null || appIconFile.lastModified() == this.params.appIconLastModified)) {
                Slog.w(TAG, "Writing changed icon " + appIconFile);
                AutoCloseable autoCloseable = null;
                try {
                    FileOutputStream os = new FileOutputStream(appIconFile);
                    try {
                        this.params.appIcon.compress(CompressFormat.PNG, 90, os);
                        IoUtils.closeQuietly(os);
                        FileOutputStream fileOutputStream = os;
                    } catch (IOException e2) {
                        e = e2;
                        obj = os;
                        try {
                            Slog.w(TAG, "Failed to write icon " + appIconFile + ": " + e.getMessage());
                            IoUtils.closeQuietly(autoCloseable);
                            this.params.appIconLastModified = appIconFile.lastModified();
                            writeGrantedRuntimePermissionsLocked(out, this.params.grantedRuntimePermissions);
                            out.endTag(null, TAG_SESSION);
                        } catch (Throwable th2) {
                            th = th2;
                            IoUtils.closeQuietly(autoCloseable);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        obj = os;
                        IoUtils.closeQuietly(autoCloseable);
                        throw th;
                    }
                } catch (IOException e3) {
                    e = e3;
                    Slog.w(TAG, "Failed to write icon " + appIconFile + ": " + e.getMessage());
                    IoUtils.closeQuietly(autoCloseable);
                    this.params.appIconLastModified = appIconFile.lastModified();
                    writeGrantedRuntimePermissionsLocked(out, this.params.grantedRuntimePermissions);
                    out.endTag(null, TAG_SESSION);
                }
                this.params.appIconLastModified = appIconFile.lastModified();
            }
            writeGrantedRuntimePermissionsLocked(out, this.params.grantedRuntimePermissions);
            out.endTag(null, TAG_SESSION);
        }
    }

    private static String[] readGrantedRuntimePermissions(XmlPullParser in) throws IOException, XmlPullParserException {
        List permissions = null;
        int outerDepth = in.getDepth();
        while (true) {
            int type = in.next();
            if (type == 1 || (type == 3 && in.getDepth() <= outerDepth)) {
                if (permissions == null) {
                    return null;
                }
                String[] permissionsArray = new String[permissions.size()];
                permissions.toArray(permissionsArray);
                return permissionsArray;
            } else if (!(type == 3 || type == 4 || !TAG_GRANTED_RUNTIME_PERMISSION.equals(in.getName()))) {
                String permission = XmlUtils.readStringAttribute(in, ATTR_NAME);
                if (permissions == null) {
                    permissions = new ArrayList();
                }
                permissions.add(permission);
            }
        }
        if (permissions == null) {
            return null;
        }
        String[] permissionsArray2 = new String[permissions.size()];
        permissions.toArray(permissionsArray2);
        return permissionsArray2;
    }

    public static PackageInstallerSession readFromXml(XmlPullParser in, InternalCallback callback, Context context, PackageManagerService pm, Looper installerThread, File sessionsDir) throws IOException, XmlPullParserException {
        int sessionId = XmlUtils.readIntAttribute(in, ATTR_SESSION_ID);
        int userId = XmlUtils.readIntAttribute(in, ATTR_USER_ID);
        String installerPackageName = XmlUtils.readStringAttribute(in, ATTR_INSTALLER_PACKAGE_NAME);
        int installerUid = XmlUtils.readIntAttribute(in, ATTR_INSTALLER_UID, pm.getPackageUid(installerPackageName, 8192, userId));
        long createdMillis = XmlUtils.readLongAttribute(in, ATTR_CREATED_MILLIS);
        String stageDirRaw = XmlUtils.readStringAttribute(in, ATTR_SESSION_STAGE_DIR);
        File file = stageDirRaw != null ? new File(stageDirRaw) : null;
        String stageCid = XmlUtils.readStringAttribute(in, ATTR_SESSION_STAGE_CID);
        boolean prepared = XmlUtils.readBooleanAttribute(in, ATTR_PREPARED, true);
        boolean sealed = XmlUtils.readBooleanAttribute(in, ATTR_SEALED);
        SessionParams params = new SessionParams(-1);
        params.mode = XmlUtils.readIntAttribute(in, ATTR_MODE);
        params.installFlags = XmlUtils.readIntAttribute(in, ATTR_INSTALL_FLAGS);
        params.installLocation = XmlUtils.readIntAttribute(in, ATTR_INSTALL_LOCATION);
        params.sizeBytes = XmlUtils.readLongAttribute(in, ATTR_SIZE_BYTES);
        params.appPackageName = XmlUtils.readStringAttribute(in, ATTR_APP_PACKAGE_NAME);
        params.appIcon = XmlUtils.readBitmapAttribute(in, ATTR_APP_ICON);
        params.appLabel = XmlUtils.readStringAttribute(in, ATTR_APP_LABEL);
        params.originatingUri = XmlUtils.readUriAttribute(in, ATTR_ORIGINATING_URI);
        params.originatingUid = XmlUtils.readIntAttribute(in, ATTR_ORIGINATING_UID, -1);
        params.referrerUri = XmlUtils.readUriAttribute(in, ATTR_REFERRER_URI);
        params.abiOverride = XmlUtils.readStringAttribute(in, ATTR_ABI_OVERRIDE);
        params.volumeUuid = XmlUtils.readStringAttribute(in, ATTR_VOLUME_UUID);
        params.grantedRuntimePermissions = readGrantedRuntimePermissions(in);
        params.installReason = XmlUtils.readIntAttribute(in, ATTR_INSTALL_REASON);
        File appIconFile = buildAppIconFile(sessionId, sessionsDir);
        if (appIconFile.exists()) {
            params.appIcon = BitmapFactory.decodeFile(appIconFile.getAbsolutePath());
            params.appIconLastModified = appIconFile.lastModified();
        }
        return new PackageInstallerSession(callback, context, pm, installerThread, sessionId, userId, installerPackageName, installerUid, params, createdMillis, file, stageCid, prepared, sealed);
    }

    private static void uploadInstallErrRadar(String reason) {
        Bundle data = new Bundle();
        data.putString(HwBroadcastRadarUtil.KEY_PACKAGE, "PMS");
        data.putString(HwBroadcastRadarUtil.KEY_VERSION_NAME, "0");
        data.putString("extra", reason);
        if (mMonitor != null) {
            mMonitor.monitor(907400000, data);
        }
    }
}
