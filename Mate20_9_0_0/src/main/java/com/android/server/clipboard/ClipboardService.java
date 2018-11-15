package com.android.server.clipboard;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.common.HwFrameworkFactory;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IClipboard.Stub;
import android.content.IOnPrimaryClipChangedListener;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.hdm.HwDeviceManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import android.widget.Toast;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.clipboard.HostClipboardMonitor.HostClipboardCallback;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.util.HashSet;
import java.util.List;

public class ClipboardService extends SystemService {
    private static final boolean IS_EMULATOR = SystemProperties.getBoolean("ro.kernel.qemu", false);
    private static final String TAG = "ClipboardService";
    private final IActivityManager mAm = ActivityManager.getService();
    private final AppOpsManager mAppOps = ((AppOpsManager) getContext().getSystemService("appops"));
    private final SparseArray<PerUserClipboard> mClipboards = new SparseArray();
    private HostClipboardMonitor mHostClipboardMonitor = null;
    private Thread mHostMonitorThread = null;
    private final IBinder mPermissionOwner;
    private final PackageManager mPm = getContext().getPackageManager();
    private final IUserManager mUm = ((IUserManager) ServiceManager.getService("user"));

    private class ClipboardImpl extends Stub {
        private ClipboardImpl() {
        }

        /* synthetic */ ClipboardImpl(ClipboardService x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf("clipboard", "Exception: ", e);
                }
                throw e;
            }
        }

        /* JADX WARNING: Missing block: B:12:0x0034, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setPrimaryClip(ClipData clip, String callingPackage) {
            synchronized (this) {
                if (clip != null) {
                    if (clip.getItemCount() > 0) {
                        int callingUid = Binder.getCallingUid();
                        if (ClipboardService.this.clipboardAccessAllowed(30, callingPackage, callingUid)) {
                            ClipboardService.this.checkDataOwnerLocked(clip, callingUid);
                            ClipboardService.this.setPrimaryClipInternal(clip, callingUid);
                            return;
                        }
                        final Context context = ClipboardService.this.getContext();
                        if (HwDeviceManager.disallowOp(23) && context != null) {
                            UiThread.getHandler().post(new Runnable() {
                                public void run() {
                                    Toast toast = Toast.makeText(context, context.getResources().getString(33685904), 1);
                                    toast.getWindowParams().type = 2006;
                                    toast.show();
                                }
                            });
                        }
                    }
                }
                throw new IllegalArgumentException("No items");
            }
        }

        public void clearPrimaryClip(String callingPackage) {
            synchronized (this) {
                int callingUid = Binder.getCallingUid();
                if (ClipboardService.this.clipboardAccessAllowed(30, callingPackage, callingUid)) {
                    ClipboardService.this.setPrimaryClipInternal(null, callingUid);
                    return;
                }
            }
        }

        public ClipData getPrimaryClip(String pkg) {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.CLIPBOARD_GETPRIMARYCLIP);
            synchronized (this) {
                if (!ClipboardService.this.clipboardAccessAllowed(29, pkg, Binder.getCallingUid()) || ClipboardService.this.isDeviceLocked()) {
                    return null;
                }
                ClipboardService.this.addActiveOwnerLocked(Binder.getCallingUid(), pkg);
                ClipData clipData = ClipboardService.this.getClipboard().primaryClip;
                return clipData;
            }
        }

        /* JADX WARNING: Missing block: B:10:0x002b, code:
            return r1;
     */
        /* JADX WARNING: Missing block: B:12:0x002d, code:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ClipDescription getPrimaryClipDescription(String callingPackage) {
            synchronized (this) {
                ClipDescription clipDescription = null;
                if (!ClipboardService.this.clipboardAccessAllowed(29, callingPackage, Binder.getCallingUid()) || ClipboardService.this.isDeviceLocked()) {
                } else {
                    PerUserClipboard clipboard = ClipboardService.this.getClipboard();
                    if (clipboard.primaryClip != null) {
                        clipDescription = clipboard.primaryClip.getDescription();
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:10:0x0026, code:
            return r1;
     */
        /* JADX WARNING: Missing block: B:12:0x0028, code:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean hasPrimaryClip(String callingPackage) {
            synchronized (this) {
                boolean z = false;
                if (!ClipboardService.this.clipboardAccessAllowed(29, callingPackage, Binder.getCallingUid()) || ClipboardService.this.isDeviceLocked()) {
                } else if (ClipboardService.this.getClipboard().primaryClip != null) {
                    z = true;
                }
            }
        }

        public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage) {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.CLIPBOARD_ADDPRIMARYCLIPCHANGEDLISTENER);
            synchronized (this) {
                ClipboardService.this.getClipboard().primaryClipListeners.register(listener, new ListenerInfo(Binder.getCallingUid(), callingPackage));
            }
        }

        public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
            synchronized (this) {
                ClipboardService.this.getClipboard().primaryClipListeners.unregister(listener);
            }
        }

        /* JADX WARNING: Missing block: B:14:0x0038, code:
            return r1;
     */
        /* JADX WARNING: Missing block: B:18:0x003c, code:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean hasClipboardText(String callingPackage) {
            synchronized (this) {
                boolean z = false;
                if (!ClipboardService.this.clipboardAccessAllowed(29, callingPackage, Binder.getCallingUid()) || ClipboardService.this.isDeviceLocked()) {
                } else {
                    PerUserClipboard clipboard = ClipboardService.this.getClipboard();
                    if (clipboard.primaryClip != null) {
                        CharSequence text = clipboard.primaryClip.getItemAt(0).getText();
                        if (text != null && text.length() > 0) {
                            z = true;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    private class ListenerInfo {
        final String mPackageName;
        final int mUid;

        ListenerInfo(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }
    }

    private class PerUserClipboard {
        final HashSet<String> activePermissionOwners = new HashSet();
        ClipData primaryClip;
        final RemoteCallbackList<IOnPrimaryClipChangedListener> primaryClipListeners = new RemoteCallbackList();
        int primaryClipUid = 9999;
        final int userId;

        PerUserClipboard(int userId) {
            this.userId = userId;
        }
    }

    public ClipboardService(Context context) {
        super(context);
        IBinder permOwner = null;
        try {
            permOwner = this.mAm.newUriPermissionOwner("clipboard");
        } catch (RemoteException e) {
            Slog.w("clipboard", "AM dead", e);
        }
        this.mPermissionOwner = permOwner;
        if (IS_EMULATOR) {
            this.mHostClipboardMonitor = new HostClipboardMonitor(new HostClipboardCallback() {
                public void onHostClipboardUpdated(String contents) {
                    ClipData clip = new ClipData("host clipboard", new String[]{"text/plain"}, new Item(contents));
                    synchronized (ClipboardService.this.mClipboards) {
                        ClipboardService.this.setPrimaryClipInternal(ClipboardService.this.getClipboard(0), clip, 1000);
                    }
                }
            });
            this.mHostMonitorThread = new Thread(this.mHostClipboardMonitor);
            this.mHostMonitorThread.start();
        }
    }

    public void onStart() {
        publishBinderService("clipboard", new ClipboardImpl(this, null));
    }

    public void onCleanupUser(int userId) {
        synchronized (this.mClipboards) {
            this.mClipboards.remove(userId);
        }
    }

    private PerUserClipboard getClipboard() {
        return getClipboard(UserHandle.getCallingUserId());
    }

    private PerUserClipboard getClipboard(int userId) {
        PerUserClipboard puc;
        synchronized (this.mClipboards) {
            puc = (PerUserClipboard) this.mClipboards.get(userId);
            if (puc == null) {
                puc = new PerUserClipboard(userId);
                this.mClipboards.put(userId, puc);
            }
        }
        return puc;
    }

    List<UserInfo> getRelatedProfiles(int userId) {
        long origId = Binder.clearCallingIdentity();
        try {
            List<UserInfo> related = this.mUm.getProfiles(userId, true);
            Binder.restoreCallingIdentity(origId);
            return related;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Remote Exception calling UserManager: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            Binder.restoreCallingIdentity(origId);
            return null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
    }

    private boolean hasRestriction(String restriction, int userId) {
        try {
            return this.mUm.hasUserRestriction(restriction, userId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception calling UserManager.getUserRestrictions: ", e);
            return true;
        }
    }

    void setPrimaryClipInternal(ClipData clip, int callingUid) {
        int i = 0;
        if (this.mHostClipboardMonitor != null) {
            if (clip == null) {
                this.mHostClipboardMonitor.setHostClipboard(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            } else if (clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null) {
                    this.mHostClipboardMonitor.setHostClipboard(text.toString());
                }
            }
        }
        int userId = UserHandle.getUserId(callingUid);
        setPrimaryClipInternal(getClipboard(userId), clip, callingUid);
        List<UserInfo> related = getRelatedProfiles(userId);
        if (related != null) {
            int size = related.size();
            if (size > 1) {
                int i2;
                if (hasRestriction("no_cross_profile_copy_paste", userId) ^ true) {
                    clip = new ClipData(clip);
                    for (i2 = clip.getItemCount() - 1; i2 >= 0; i2--) {
                        clip.setItemAt(i2, new Item(clip.getItemAt(i2)));
                    }
                    clip.fixUrisLight(userId);
                } else {
                    clip = null;
                }
                while (i < size) {
                    i2 = ((UserInfo) related.get(i)).id;
                    if (i2 != userId && (hasRestriction("no_sharing_into_profile", i2) ^ true)) {
                        setPrimaryClipInternal(getClipboard(i2), clip, callingUid);
                    }
                    i++;
                }
            }
        }
    }

    void setPrimaryClipInternal(PerUserClipboard clipboard, ClipData clip, int callingUid) {
        revokeUris(clipboard);
        clipboard.activePermissionOwners.clear();
        if (clip != null || clipboard.primaryClip != null) {
            clipboard.primaryClip = clip;
            if (clip != null) {
                clipboard.primaryClipUid = callingUid;
            } else {
                clipboard.primaryClipUid = 9999;
            }
            if (clip != null) {
                ClipDescription description = clip.getDescription();
                if (description != null) {
                    description.setTimestamp(System.currentTimeMillis());
                }
            }
            long ident = Binder.clearCallingIdentity();
            int n = clipboard.primaryClipListeners.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ListenerInfo li = (ListenerInfo) clipboard.primaryClipListeners.getBroadcastCookie(i);
                    if (clipboardAccessAllowed(29, li.mPackageName, li.mUid)) {
                        ((IOnPrimaryClipChangedListener) clipboard.primaryClipListeners.getBroadcastItem(i)).dispatchPrimaryClipChanged();
                    }
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    clipboard.primaryClipListeners.finishBroadcast();
                    Binder.restoreCallingIdentity(ident);
                }
            }
            clipboard.primaryClipListeners.finishBroadcast();
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isDeviceLocked() {
        int callingUserId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(KeyguardManager.class);
            boolean z = keyguardManager != null && keyguardManager.isDeviceLocked(callingUserId);
            Binder.restoreCallingIdentity(token);
            return z;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private final void checkUriOwnerLocked(Uri uri, int sourceUid) {
        if (uri != null && "content".equals(uri.getScheme())) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mAm.checkGrantUriPermission(sourceUid, null, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void checkItemOwnerLocked(Item item, int uid) {
        if (item.getUri() != null) {
            checkUriOwnerLocked(item.getUri(), uid);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            checkUriOwnerLocked(intent.getData(), uid);
        }
    }

    private final void checkDataOwnerLocked(ClipData data, int uid) {
        int N = data.getItemCount();
        for (int i = 0; i < N; i++) {
            checkItemOwnerLocked(data.getItemAt(i), uid);
        }
    }

    private final void grantUriLocked(Uri uri, int sourceUid, String targetPkg, int targetUserId) {
        if (uri != null && "content".equals(uri.getScheme())) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mAm.grantUriPermissionFromOwner(this.mPermissionOwner, sourceUid, targetPkg, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)), targetUserId);
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void grantItemLocked(Item item, int sourceUid, String targetPkg, int targetUserId) {
        if (item.getUri() != null) {
            grantUriLocked(item.getUri(), sourceUid, targetPkg, targetUserId);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriLocked(intent.getData(), sourceUid, targetPkg, targetUserId);
        }
    }

    private final void addActiveOwnerLocked(int uid, String pkg) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int targetUserHandle = UserHandle.getCallingUserId();
        long oldIdentity = Binder.clearCallingIdentity();
        int i = 0;
        try {
            PackageInfo pi = pm.getPackageInfo(pkg, 0, targetUserHandle);
            StringBuilder stringBuilder;
            if (pi != null) {
                if (!UserHandle.isSameApp(pi.applicationInfo.uid, uid)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Calling uid ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" does not own package ");
                    stringBuilder.append(pkg);
                    throw new SecurityException(stringBuilder.toString());
                }
                Binder.restoreCallingIdentity(oldIdentity);
                PerUserClipboard clipboard = getClipboard();
                if (clipboard.primaryClip != null && !clipboard.activePermissionOwners.contains(pkg)) {
                    int N = clipboard.primaryClip.getItemCount();
                    while (i < N) {
                        grantItemLocked(clipboard.primaryClip.getItemAt(i), clipboard.primaryClipUid, pkg, UserHandle.getUserId(uid));
                        i++;
                    }
                    clipboard.activePermissionOwners.add(pkg);
                    return;
                }
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown package ");
            stringBuilder.append(pkg);
            throw new IllegalArgumentException(stringBuilder.toString());
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldIdentity);
        }
    }

    private final void revokeUriLocked(Uri uri, int sourceUid) {
        if (uri != null && "content".equals(uri.getScheme())) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mAm.revokeUriPermissionFromOwner(this.mPermissionOwner, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
            } catch (RemoteException e) {
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void revokeItemLocked(Item item, int sourceUid) {
        if (item.getUri() != null) {
            revokeUriLocked(item.getUri(), sourceUid);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            revokeUriLocked(intent.getData(), sourceUid);
        }
    }

    private final void revokeUris(PerUserClipboard clipboard) {
        if (clipboard.primaryClip != null) {
            int N = clipboard.primaryClip.getItemCount();
            for (int i = 0; i < N; i++) {
                revokeItemLocked(clipboard.primaryClip.getItemAt(i), clipboard.primaryClipUid);
            }
        }
    }

    private boolean clipboardAccessAllowed(int op, String callingPackage, int callingUid) {
        if (this.mAppOps.noteOp(op, callingUid, callingPackage) != 0) {
            return false;
        }
        if (HwDeviceManager.disallowOp(23)) {
            Slog.i(TAG, "Clipboard is not allowed by MDM!");
            return false;
        }
        try {
            if (AppGlobals.getPackageManager().isInstantApp(callingPackage, UserHandle.getUserId(callingUid))) {
                return this.mAm.isAppForeground(callingUid);
            }
            return true;
        } catch (RemoteException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get Instant App status for package ");
            stringBuilder.append(callingPackage);
            Slog.e("clipboard", stringBuilder.toString(), e);
            return false;
        }
    }
}
