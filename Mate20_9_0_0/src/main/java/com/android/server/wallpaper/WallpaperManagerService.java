package com.android.server.wallpaper;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IWallpaperManager.Stub;
import android.app.IWallpaperManagerCallback;
import android.app.PendingIntent;
import android.app.UserSwitchObserver;
import android.app.WallpaperColors;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.service.wallpaper.IWallpaperConnection;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.system.ErrnoException;
import android.system.Os;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WallpaperManagerService extends Stub implements IWallpaperManagerService {
    static final boolean DEBUG = false;
    static final boolean DEBUG_LIVE = true;
    static final int MAX_WALLPAPER_COMPONENT_LOG_LENGTH = 128;
    static final long MIN_WALLPAPER_CRASH_TIME = 10000;
    static final String TAG = "WallpaperManagerService";
    static final String WALLPAPER = "wallpaper_orig";
    static final String WALLPAPER_CROP = "wallpaper";
    static final String WALLPAPER_INFO = "wallpaper_info.xml";
    static final String WALLPAPER_LOCK_CROP = "wallpaper_lock";
    static final String WALLPAPER_LOCK_ORIG = "wallpaper_lock_orig";
    public static Handler mHandler = new Handler(Looper.getMainLooper());
    static final String[] sPerUserFiles = new String[]{WALLPAPER, WALLPAPER_CROP, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP, WALLPAPER_INFO};
    final AppOpsManager mAppOpsManager;
    final SparseArray<RemoteCallbackList<IWallpaperManagerCallback>> mColorsChangedListeners;
    final Context mContext;
    int mCurrentUserId = -10000;
    ComponentName mDefaultWallpaperComponent;
    final IPackageManager mIPackageManager;
    final IWindowManager mIWindowManager;
    final ComponentName mImageWallpaper;
    boolean mInAmbientMode;
    boolean mIsLoadLiveWallpaper = false;
    IWallpaperManagerCallback mKeyguardListener;
    WallpaperData mLastWallpaper;
    final Object mLock = new Object();
    final SparseArray<WallpaperData> mLockWallpaperMap = new SparseArray();
    final MyPackageMonitor mMonitor;
    boolean mShuttingDown;
    boolean mSuccess = false;
    int mThemeMode;
    final SparseArray<Boolean> mUserRestorecon = new SparseArray();
    boolean mWaitingForUnlock;
    int mWallpaperId;
    final SparseArray<WallpaperData> mWallpaperMap = new SparseArray();

    class MyPackageMonitor extends PackageMonitor {
        static final String PACKAGE_SYSTEMUI = "com.android.systemui";

        MyPackageMonitor() {
        }

        /* JADX WARNING: Missing block: B:17:0x0085, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onPackageUpdateFinished(String packageName, int uid) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    ComponentName wpService = wallpaper.wallpaperComponent;
                    if (wpService != null && wpService.getPackageName().equals(packageName)) {
                        String str = WallpaperManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Wallpaper ");
                        stringBuilder.append(wpService);
                        stringBuilder.append(" update has finished");
                        Slog.i(str, stringBuilder.toString());
                        wallpaper.wallpaperUpdating = false;
                        WallpaperManagerService.this.clearWallpaperComponentLocked(wallpaper);
                        if (!WallpaperManagerService.this.bindWallpaperComponentLocked(wpService, false, false, wallpaper, null)) {
                            str = WallpaperManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Wallpaper ");
                            stringBuilder.append(wpService);
                            stringBuilder.append(" no longer available; reverting to default");
                            Slog.w(str, stringBuilder.toString());
                            WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                        }
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:16:0x0038, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:18:0x003a, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onPackageModified(String packageName) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    if (wallpaper.wallpaperComponent != null) {
                        if (wallpaper.wallpaperComponent.getPackageName().equals(packageName)) {
                            doPackagesChangedLocked(true, wallpaper);
                        }
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:17:0x0063, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onPackageUpdateStarted(String packageName, int uid) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (!(wallpaper == null || wallpaper.wallpaperComponent == null || !wallpaper.wallpaperComponent.getPackageName().equals(packageName))) {
                    String str = WallpaperManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Wallpaper service ");
                    stringBuilder.append(wallpaper.wallpaperComponent);
                    stringBuilder.append(" is updating");
                    Slog.i(str, stringBuilder.toString());
                    wallpaper.wallpaperUpdating = true;
                    if (wallpaper.connection != null) {
                        FgThread.getHandler().removeCallbacks(wallpaper.connection.mResetRunnable);
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:18:0x0042, code skipped:
            return r1;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            synchronized (WallpaperManagerService.this.mLock) {
                boolean changed = false;
                int i = 0;
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return false;
                }
                int length = packages.length;
                while (i < length) {
                    if (PACKAGE_SYSTEMUI.equals(packages[i])) {
                        doit = false;
                        Slog.w(WallpaperManagerService.TAG, "SystemUI has been forced to stop!");
                        break;
                    }
                    i++;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    changed = false | doPackagesChangedLocked(doit, wallpaper);
                }
            }
        }

        /* JADX WARNING: Missing block: B:11:0x0026, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onSomePackagesChanged() {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mCurrentUserId != getChangingUserId()) {
                    return;
                }
                WallpaperData wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(WallpaperManagerService.this.mCurrentUserId);
                if (wallpaper != null) {
                    doPackagesChangedLocked(true, wallpaper);
                }
            }
        }

        boolean doPackagesChangedLocked(boolean doit, WallpaperData wallpaper) {
            int change;
            boolean changed = false;
            if (wallpaper.wallpaperComponent != null) {
                change = isPackageDisappearing(wallpaper.wallpaperComponent.getPackageName());
                if (change == 3 || change == 2) {
                    changed = true;
                    if (doit) {
                        String str = WallpaperManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Wallpaper uninstalled, removing: ");
                        stringBuilder.append(wallpaper.wallpaperComponent);
                        Slog.w(str, stringBuilder.toString());
                        WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                        WallpaperManagerService.this.mContext.sendBroadcastAsUser(new Intent("android.intent.action.WALLPAPER_CHANGED"), new UserHandle(WallpaperManagerService.this.mCurrentUserId));
                    }
                }
            }
            if (wallpaper.nextWallpaperComponent != null) {
                change = isPackageDisappearing(wallpaper.nextWallpaperComponent.getPackageName());
                if (change == 3 || change == 2) {
                    wallpaper.nextWallpaperComponent = null;
                }
            }
            if (wallpaper.wallpaperComponent != null && isPackageModified(wallpaper.wallpaperComponent.getPackageName())) {
                try {
                    if (WallpaperManagerService.this.mIPackageManager.getServiceInfo(wallpaper.wallpaperComponent, 0, WallpaperManagerService.this.mCurrentUserId) == null) {
                        String str2 = WallpaperManagerService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Wallpaper component gone, removing: ");
                        stringBuilder2.append(wallpaper.wallpaperComponent);
                        Slog.w(str2, stringBuilder2.toString());
                        WallpaperManagerService.this.clearWallpaperLocked(false, 1, wallpaper.userId, null);
                    }
                } catch (RemoteException e) {
                    Slog.e(WallpaperManagerService.TAG, " mIPackageManager  RemoteException error");
                }
            }
            if (wallpaper.nextWallpaperComponent != null && isPackageModified(wallpaper.nextWallpaperComponent.getPackageName())) {
                try {
                    if (WallpaperManagerService.this.mIPackageManager.getServiceInfo(wallpaper.nextWallpaperComponent, 0, WallpaperManagerService.this.mCurrentUserId) == null) {
                        wallpaper.nextWallpaperComponent = null;
                    }
                } catch (RemoteException e2) {
                    Slog.e(WallpaperManagerService.TAG, " nextWallpaperComponent mIPackageManager  RemoteException error");
                }
            }
            return changed;
        }
    }

    private class ThemeSettingsObserver extends ContentObserver {
        public ThemeSettingsObserver(Handler handler) {
            super(handler);
        }

        public void startObserving(Context context) {
            context.getContentResolver().registerContentObserver(Secure.getUriFor("theme_mode"), false, this);
        }

        public void stopObserving(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange) {
            WallpaperManagerService.this.onThemeSettingsChanged();
        }
    }

    class WallpaperConnection extends IWallpaperConnection.Stub implements ServiceConnection {
        private static final long WALLPAPER_RECONNECT_TIMEOUT_MS = 10000;
        boolean mDimensionsChanged = false;
        IWallpaperEngine mEngine;
        final WallpaperInfo mInfo;
        boolean mPaddingChanged = false;
        IRemoteCallback mReply;
        private Runnable mResetRunnable = new -$$Lambda$WallpaperManagerService$WallpaperConnection$QhODF3v-swnwSYvDbeEhU85gOBw(this);
        IWallpaperService mService;
        final Binder mToken = new Binder();
        WallpaperData mWallpaper;

        /* JADX WARNING: Missing block: B:14:0x004f, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static /* synthetic */ void lambda$new$0(WallpaperConnection wallpaperConnection) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mShuttingDown) {
                    Slog.i(WallpaperManagerService.TAG, "Ignoring relaunch timeout during shutdown");
                } else if (!wallpaperConnection.mWallpaper.wallpaperUpdating && wallpaperConnection.mWallpaper.userId == WallpaperManagerService.this.mCurrentUserId) {
                    String str = WallpaperManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Wallpaper reconnect timed out for ");
                    stringBuilder.append(wallpaperConnection.mWallpaper.wallpaperComponent);
                    stringBuilder.append(", reverting to built-in wallpaper!");
                    Slog.w(str, stringBuilder.toString());
                    WallpaperManagerService.this.clearWallpaperLocked(true, 1, wallpaperConnection.mWallpaper.userId, null);
                }
            }
        }

        public WallpaperConnection(WallpaperInfo info, WallpaperData wallpaper) {
            this.mInfo = info;
            this.mWallpaper = wallpaper;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            String str = WallpaperManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Service Connected: ");
            stringBuilder.append(name);
            stringBuilder.append(" userId:");
            stringBuilder.append(this.mWallpaper.userId);
            Slog.i(str, stringBuilder.toString());
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection == this) {
                    this.mService = IWallpaperService.Stub.asInterface(service);
                    WallpaperManagerService.this.attachServiceLocked(this, this.mWallpaper);
                    WallpaperManagerService.this.saveSettingsLocked(this.mWallpaper.userId);
                    FgThread.getHandler().removeCallbacks(this.mResetRunnable);
                    if (!(name == null || name.equals(WallpaperManagerService.this.mImageWallpaper))) {
                        WallpaperManagerService.this.mContext.sendBroadcastAsUser(new Intent("android.intent.action.WALLPAPER_CHANGED"), new UserHandle(WallpaperManagerService.this.mCurrentUserId));
                    }
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (WallpaperManagerService.this.mLock) {
                String str = WallpaperManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Wallpaper service gone: ");
                stringBuilder.append(name);
                Slog.w(str, stringBuilder.toString());
                if (!Objects.equals(name, this.mWallpaper.wallpaperComponent)) {
                    str = WallpaperManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Does not match expected wallpaper component ");
                    stringBuilder.append(this.mWallpaper.wallpaperComponent);
                    Slog.e(str, stringBuilder.toString());
                }
                this.mService = null;
                this.mEngine = null;
                if (this.mWallpaper.connection == this && !this.mWallpaper.wallpaperUpdating) {
                    WallpaperManagerService.this.mContext.getMainThreadHandler().postDelayed(new -$$Lambda$WallpaperManagerService$WallpaperConnection$-zrxaVg2Hu5N6--4jvUZ0DkaLJY(this), 1000);
                }
            }
        }

        public void scheduleTimeoutLocked() {
            Handler fgHandler = FgThread.getHandler();
            fgHandler.removeCallbacks(this.mResetRunnable);
            fgHandler.postDelayed(this.mResetRunnable, 10000);
            String str = WallpaperManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Started wallpaper reconnect timeout for ");
            stringBuilder.append(this.mWallpaper.wallpaperComponent);
            Slog.i(str, stringBuilder.toString());
        }

        private void processDisconnect(ServiceConnection connection) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (connection == this.mWallpaper.connection) {
                    ComponentName wpService = this.mWallpaper.wallpaperComponent;
                    if (!(this.mWallpaper.wallpaperUpdating || this.mWallpaper.userId != WallpaperManagerService.this.mCurrentUserId || Objects.equals(WallpaperManagerService.this.mDefaultWallpaperComponent, wpService) || Objects.equals(WallpaperManagerService.this.mImageWallpaper, wpService))) {
                        if (this.mWallpaper.lastDiedTime == 0 || this.mWallpaper.lastDiedTime + 10000 <= SystemClock.uptimeMillis()) {
                            this.mWallpaper.lastDiedTime = SystemClock.uptimeMillis();
                            WallpaperManagerService.this.clearWallpaperComponentLocked(this.mWallpaper);
                            if (WallpaperManagerService.this.bindWallpaperComponentLocked(wpService, false, false, this.mWallpaper, null)) {
                                this.mWallpaper.connection.scheduleTimeoutLocked();
                            } else {
                                Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                                WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                            }
                        } else {
                            Slog.w(WallpaperManagerService.TAG, "Reverting to built-in wallpaper!");
                            WallpaperManagerService.this.clearWallpaperLocked(true, 1, this.mWallpaper.userId, null);
                        }
                        String flattened = wpService.flattenToString();
                        EventLog.writeEvent(EventLogTags.WP_WALLPAPER_CRASHED, flattened.substring(0, Math.min(flattened.length(), 128)));
                    }
                } else {
                    Slog.i(WallpaperManagerService.TAG, "Wallpaper changed during disconnect tracking; ignoring");
                }
            }
        }

        /* JADX WARNING: Missing block: B:11:0x002d, code skipped:
            r0 = r1;
     */
        /* JADX WARNING: Missing block: B:12:0x002e, code skipped:
            if (r0 == 0) goto L_0x0037;
     */
        /* JADX WARNING: Missing block: B:13:0x0030, code skipped:
            com.android.server.wallpaper.WallpaperManagerService.access$100(r4.this$0, r4.mWallpaper, r0);
     */
        /* JADX WARNING: Missing block: B:14:0x0037, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onWallpaperColorsChanged(WallpaperColors primaryColors) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (WallpaperManagerService.this.mImageWallpaper.equals(this.mWallpaper.wallpaperComponent)) {
                    return;
                }
                this.mWallpaper.primaryColors = primaryColors;
                int which = 1;
                if (((WallpaperData) WallpaperManagerService.this.mLockWallpaperMap.get(this.mWallpaper.userId)) == null) {
                    which = 1 | 2;
                }
            }
        }

        public void attachEngine(IWallpaperEngine engine) {
            String str = WallpaperManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("attachEngine userId:");
            stringBuilder.append(this.mWallpaper.userId);
            Slog.i(str, stringBuilder.toString());
            synchronized (WallpaperManagerService.this.mLock) {
                this.mEngine = engine;
                if (this.mDimensionsChanged) {
                    try {
                        this.mEngine.setDesiredSize(this.mWallpaper.width, this.mWallpaper.height);
                    } catch (RemoteException e) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper dimensions", e);
                    }
                    this.mDimensionsChanged = false;
                }
                if (this.mPaddingChanged) {
                    try {
                        this.mEngine.setDisplayPadding(this.mWallpaper.padding);
                    } catch (RemoteException e2) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set wallpaper padding", e2);
                    }
                    this.mPaddingChanged = false;
                }
                if (this.mInfo != null && this.mInfo.getSupportsAmbientMode()) {
                    try {
                        this.mEngine.setInAmbientMode(WallpaperManagerService.this.mInAmbientMode, false);
                    } catch (RemoteException e22) {
                        Slog.w(WallpaperManagerService.TAG, "Failed to set ambient mode state", e22);
                    }
                }
                try {
                    this.mEngine.requestWallpaperColors();
                } catch (RemoteException e222) {
                    Slog.w(WallpaperManagerService.TAG, "Failed to request wallpaper colors", e222);
                }
            }
            return;
        }

        public void engineShown(IWallpaperEngine engine) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mReply != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        this.mReply.sendResult(null);
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(ident);
                    }
                    this.mReply = null;
                }
            }
        }

        public ParcelFileDescriptor setWallpaper(String name) {
            synchronized (WallpaperManagerService.this.mLock) {
                if (this.mWallpaper.connection == this) {
                    ParcelFileDescriptor updateWallpaperBitmapLocked = WallpaperManagerService.this.updateWallpaperBitmapLocked(name, this.mWallpaper, null);
                    return updateWallpaperBitmapLocked;
                }
                return null;
            }
        }
    }

    public static class WallpaperData {
        boolean allowBackup;
        private RemoteCallbackList<IWallpaperManagerCallback> callbacks = new RemoteCallbackList();
        WallpaperConnection connection;
        final File cropFile;
        final Rect cropHint = new Rect(0, 0, 0, 0);
        int[] currOffsets = new int[]{-1, -1, -1, -1};
        int height = -1;
        boolean imageWallpaperPending;
        long lastDiedTime;
        String name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        int[] nextOffsets = new int[]{-1, -1, -1, -1};
        ComponentName nextWallpaperComponent;
        final Rect padding = new Rect(0, 0, 0, 0);
        WallpaperColors primaryColors;
        IWallpaperManagerCallback setComplete;
        ThemeSettingsObserver themeSettingsObserver;
        int userId;
        ComponentName wallpaperComponent;
        final File wallpaperFile;
        int wallpaperId;
        WallpaperObserver wallpaperObserver;
        boolean wallpaperUpdating;
        int whichPending;
        int width = -1;

        WallpaperData(int userId, String inputFileName, String cropFileName) {
            this.userId = userId;
            File wallpaperDir = WallpaperManagerService.getWallpaperDir(userId);
            this.wallpaperFile = new File(wallpaperDir, inputFileName);
            this.cropFile = new File(wallpaperDir, cropFileName);
        }

        boolean cropExists() {
            return this.cropFile.exists();
        }

        boolean sourceExists() {
            return this.wallpaperFile.exists();
        }

        public void setWidth(int w) {
            this.width = w;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public RemoteCallbackList<IWallpaperManagerCallback> getCallbacks() {
            return this.callbacks;
        }
    }

    private class WallpaperObserver extends FileObserver {
        final int mUserId;
        final WallpaperData mWallpaper;
        final File mWallpaperDir;
        final File mWallpaperFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER);
        final File mWallpaperLockFile = new File(this.mWallpaperDir, WallpaperManagerService.WALLPAPER_LOCK_ORIG);

        public WallpaperObserver(WallpaperData wallpaper) {
            super(WallpaperManagerService.getWallpaperDir(wallpaper.userId).getAbsolutePath(), 1672);
            this.mUserId = wallpaper.userId;
            this.mWallpaperDir = WallpaperManagerService.getWallpaperDir(wallpaper.userId);
            this.mWallpaper = wallpaper;
        }

        private WallpaperData dataForEvent(boolean sysChanged, boolean lockChanged) {
            WallpaperData wallpaper = null;
            synchronized (WallpaperManagerService.this.mLock) {
                if (lockChanged) {
                    try {
                        wallpaper = (WallpaperData) WallpaperManagerService.this.mLockWallpaperMap.get(this.mUserId);
                    } catch (Throwable th) {
                        while (true) {
                        }
                    }
                }
                if (wallpaper == null) {
                    wallpaper = (WallpaperData) WallpaperManagerService.this.mWallpaperMap.get(this.mUserId);
                }
            }
            return wallpaper != null ? wallpaper : this.mWallpaper;
        }

        /* JADX WARNING: Removed duplicated region for block: B:63:0x00e1 A:{SYNTHETIC, Splitter:B:63:0x00e1} */
        /* JADX WARNING: Removed duplicated region for block: B:70:0x00ee  */
        /* JADX WARNING: Missing block: B:28:0x0071, code skipped:
            if (r15.imageWallpaperPending != false) goto L_0x0079;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onEvent(int event, String path) {
            Throwable th;
            Object obj;
            int i = event;
            String str = path;
            if (str != null) {
                boolean moved = i == 128;
                boolean written = i == 8 || moved;
                File changedFile = new File(this.mWallpaperDir, str);
                boolean sysWallpaperChanged = this.mWallpaperFile.equals(changedFile);
                boolean lockWallpaperChanged = this.mWallpaperLockFile.equals(changedFile);
                int notifyColorsWhich = 0;
                WallpaperData wallpaper = dataForEvent(sysWallpaperChanged, lockWallpaperChanged);
                if (moved && lockWallpaperChanged) {
                    SELinux.restorecon(changedFile);
                    WallpaperManagerService.this.notifyLockWallpaperChanged();
                    WallpaperManagerService.this.notifyWallpaperColorsChanged(wallpaper, 2);
                    return;
                }
                Object obj2 = WallpaperManagerService.this.mLock;
                synchronized (obj2) {
                    WallpaperData wallpaperData;
                    if (sysWallpaperChanged || lockWallpaperChanged) {
                        try {
                            WallpaperManagerService.this.updateWallpaperOffsets(this.mWallpaper);
                            WallpaperManagerService.this.handleWallpaperObserverEvent(this.mWallpaper);
                            WallpaperManagerService.this.notifyCallbacksLocked(wallpaper);
                            if (wallpaper.wallpaperComponent != null && i == 8) {
                                try {
                                } catch (Throwable th2) {
                                    th = th2;
                                    obj = obj2;
                                    wallpaperData = wallpaper;
                                    throw th;
                                }
                            }
                            if (written) {
                                int i2;
                                WallpaperData wallpaper2;
                                SELinux.restorecon(changedFile);
                                if (moved) {
                                    SELinux.restorecon(changedFile);
                                    WallpaperManagerService.this.loadSettingsLocked(wallpaper.userId, true);
                                }
                                WallpaperManagerService.this.generateCrop(wallpaper);
                                wallpaper.imageWallpaperPending = false;
                                if (sysWallpaperChanged) {
                                    try {
                                        obj = obj2;
                                        i2 = 2;
                                        wallpaper2 = wallpaper;
                                        try {
                                            WallpaperManagerService.this.bindWallpaperComponentLocked(WallpaperManagerService.this.mImageWallpaper, false, false, wallpaper2, null);
                                            notifyColorsWhich = 0 | 1;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            wallpaperData = wallpaper2;
                                            throw th;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                        obj = obj2;
                                        wallpaperData = wallpaper;
                                        throw th;
                                    }
                                }
                                obj = obj2;
                                wallpaper2 = wallpaper;
                                i2 = 2;
                                if (lockWallpaperChanged) {
                                    wallpaperData = wallpaper2;
                                } else {
                                    wallpaperData = wallpaper2;
                                    if ((i2 & wallpaperData.whichPending) != 0) {
                                    }
                                    WallpaperManagerService.this.saveSettingsLocked(wallpaperData.userId);
                                    if (wallpaperData.setComplete != null) {
                                        try {
                                            wallpaperData.setComplete.onWallpaperChanged();
                                        } catch (RemoteException e) {
                                        }
                                    }
                                }
                                if (!lockWallpaperChanged) {
                                    WallpaperManagerService.this.mLockWallpaperMap.remove(wallpaperData.userId);
                                }
                                WallpaperManagerService.this.notifyLockWallpaperChanged();
                                notifyColorsWhich |= 2;
                                WallpaperManagerService.this.saveSettingsLocked(wallpaperData.userId);
                                if (wallpaperData.setComplete != null) {
                                }
                            } else {
                                obj = obj2;
                                wallpaperData = wallpaper;
                            }
                            if (notifyColorsWhich != 0) {
                                WallpaperManagerService.this.notifyWallpaperColorsChanged(wallpaperData, notifyColorsWhich);
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            obj = obj2;
                            wallpaperData = wallpaper;
                            throw th;
                        }
                    }
                    obj = obj2;
                    wallpaperData = wallpaper;
                    try {
                        if (notifyColorsWhich != 0) {
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        throw th;
                    }
                }
            }
        }
    }

    public static class Lifecycle extends SystemService {
        private IWallpaperManagerService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        public void onStart() {
            try {
                this.mService = (IWallpaperManagerService) Class.forName(getContext().getResources().getString(17039846)).getConstructor(new Class[]{Context.class}).newInstance(new Object[]{getContext()});
                publishBinderService(WallpaperManagerService.WALLPAPER_CROP, this.mService);
            } catch (Exception exp) {
                Slog.wtf(WallpaperManagerService.TAG, "Failed to instantiate WallpaperManagerService", exp);
            }
        }

        public void onBootPhase(int phase) {
            if (this.mService != null) {
                this.mService.onBootPhase(phase);
            }
        }

        public void onUnlockUser(int userHandle) {
            if (this.mService != null) {
                this.mService.onUnlockUser(userHandle);
            }
        }
    }

    private boolean needUpdateLocked(WallpaperColors colors, int themeMode) {
        boolean z = false;
        if (colors == null || themeMode == this.mThemeMode) {
            return false;
        }
        boolean result = true;
        boolean supportDarkTheme = (colors.getColorHints() & 2) != 0;
        switch (themeMode) {
            case 0:
                if (this.mThemeMode != 1) {
                    if (!supportDarkTheme) {
                        z = true;
                    }
                    result = z;
                    break;
                }
                result = supportDarkTheme;
                break;
            case 1:
                if (this.mThemeMode == 0) {
                    result = supportDarkTheme;
                    break;
                }
                break;
            case 2:
                if (this.mThemeMode == 0) {
                    if (!supportDarkTheme) {
                        z = true;
                    }
                    result = z;
                    break;
                }
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unkonwn theme mode ");
                stringBuilder.append(themeMode);
                Slog.w(str, stringBuilder.toString());
                return false;
        }
        this.mThemeMode = themeMode;
        return result;
    }

    /* JADX WARNING: Missing block: B:8:0x0026, code skipped:
            if (r1 == null) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:9:0x0028, code skipped:
            notifyWallpaperColorsChanged(r1, 1);
     */
    /* JADX WARNING: Missing block: B:10:0x002c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void onThemeSettingsChanged() {
        synchronized (this.mLock) {
            WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.get(this.mCurrentUserId);
            if (!needUpdateLocked(wallpaper.primaryColors, Secure.getInt(this.mContext.getContentResolver(), "theme_mode", 0))) {
            }
        }
    }

    void notifyLockWallpaperChanged() {
        IWallpaperManagerCallback cb = this.mKeyguardListener;
        if (cb != null) {
            try {
                cb.onWallpaperChanged();
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x002d, code skipped:
            notifyColorListeners(r5.primaryColors, r6, r5.userId);
     */
    /* JADX WARNING: Missing block: B:16:0x0034, code skipped:
            if (r1 == false) goto L_0x004e;
     */
    /* JADX WARNING: Missing block: B:17:0x0036, code skipped:
            extractColors(r5);
            r0 = r4.mLock;
     */
    /* JADX WARNING: Missing block: B:18:0x003b, code skipped:
            monitor-enter(r0);
     */
    /* JADX WARNING: Missing block: B:21:0x003e, code skipped:
            if (r5.primaryColors != null) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:22:0x0040, code skipped:
            monitor-exit(r0);
     */
    /* JADX WARNING: Missing block: B:23:0x0041, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:24:0x0042, code skipped:
            monitor-exit(r0);
     */
    /* JADX WARNING: Missing block: B:25:0x0043, code skipped:
            notifyColorListeners(r5.primaryColors, r6, r5.userId);
     */
    /* JADX WARNING: Missing block: B:30:0x004e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifyWallpaperColorsChanged(WallpaperData wallpaper, int which) {
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> userAllColorListeners = (RemoteCallbackList) this.mColorsChangedListeners.get(-1);
            if (emptyCallbackList((RemoteCallbackList) this.mColorsChangedListeners.get(wallpaper.userId)) && emptyCallbackList(userAllColorListeners)) {
                return;
            }
            boolean needsExtraction = wallpaper.primaryColors == null;
        }
    }

    private static <T extends IInterface> boolean emptyCallbackList(RemoteCallbackList<T> list) {
        return list == null || list.getRegisteredCallbackCount() == 0;
    }

    private void notifyColorListeners(WallpaperColors wallpaperColors, int which, int userId) {
        IWallpaperManagerCallback keyguardListener;
        int i;
        ArrayList<IWallpaperManagerCallback> colorListeners = new ArrayList();
        synchronized (this.mLock) {
            int count;
            int i2;
            RemoteCallbackList<IWallpaperManagerCallback> currentUserColorListeners = (RemoteCallbackList) this.mColorsChangedListeners.get(userId);
            RemoteCallbackList<IWallpaperManagerCallback> userAllColorListeners = (RemoteCallbackList) this.mColorsChangedListeners.get(-1);
            keyguardListener = this.mKeyguardListener;
            i = 0;
            if (currentUserColorListeners != null) {
                count = currentUserColorListeners.beginBroadcast();
                for (i2 = 0; i2 < count; i2++) {
                    colorListeners.add((IWallpaperManagerCallback) currentUserColorListeners.getBroadcastItem(i2));
                }
                currentUserColorListeners.finishBroadcast();
            }
            if (userAllColorListeners != null) {
                count = userAllColorListeners.beginBroadcast();
                for (i2 = 0; i2 < count; i2++) {
                    colorListeners.add((IWallpaperManagerCallback) userAllColorListeners.getBroadcastItem(i2));
                }
                userAllColorListeners.finishBroadcast();
            }
            wallpaperColors = getThemeColorsLocked(wallpaperColors);
        }
        IWallpaperManagerCallback keyguardListener2 = keyguardListener;
        int count2 = colorListeners.size();
        while (true) {
            int i3 = i;
            if (i3 >= count2) {
                break;
            }
            try {
                ((IWallpaperManagerCallback) colorListeners.get(i3)).onWallpaperColorsChanged(wallpaperColors, which, userId);
            } catch (RemoteException e) {
            }
            i = i3 + 1;
        }
        if (keyguardListener2 != null) {
            try {
                keyguardListener2.onWallpaperColorsChanged(wallpaperColors, which, userId);
            } catch (RemoteException e2) {
            }
        }
    }

    private void extractColors(WallpaperData wallpaper) {
        int wallpaperId;
        String cropFile = null;
        synchronized (this.mLock) {
            boolean imageWallpaper;
            if (!this.mImageWallpaper.equals(wallpaper.wallpaperComponent)) {
                if (wallpaper.wallpaperComponent != null) {
                    imageWallpaper = false;
                    if (imageWallpaper && wallpaper.cropFile != null && wallpaper.cropFile.exists()) {
                        cropFile = wallpaper.cropFile.getAbsolutePath();
                    }
                    wallpaperId = wallpaper.wallpaperId;
                }
            }
            imageWallpaper = true;
            cropFile = wallpaper.cropFile.getAbsolutePath();
            wallpaperId = wallpaper.wallpaperId;
        }
        WallpaperColors colors = null;
        if (cropFile != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(cropFile);
            if (bitmap != null) {
                colors = WallpaperColors.fromBitmap(bitmap);
                bitmap.recycle();
            }
        }
        WallpaperColors colors2 = colors;
        if (colors2 == null) {
            Slog.w(TAG, "Cannot extract colors because wallpaper could not be read.");
            return;
        }
        synchronized (this.mLock) {
            if (wallpaper.wallpaperId == wallpaperId) {
                wallpaper.primaryColors = colors2;
                saveSettingsLocked(wallpaper.userId);
            } else {
                Slog.w(TAG, "Not setting primary colors since wallpaper changed");
            }
        }
    }

    private WallpaperColors getThemeColorsLocked(WallpaperColors colors) {
        if (colors == null) {
            Slog.w(TAG, "Cannot get theme colors because WallpaperColors is null.");
            return null;
        }
        int colorHints = colors.getColorHints();
        boolean supportDarkTheme = (colorHints & 2) != 0;
        if (this.mThemeMode == 0 || ((this.mThemeMode == 1 && !supportDarkTheme) || (this.mThemeMode == 2 && supportDarkTheme))) {
            return colors;
        }
        WallpaperColors themeColors = new WallpaperColors(colors.getPrimaryColor(), colors.getSecondaryColor(), colors.getTertiaryColor());
        if (this.mThemeMode == 1) {
            colorHints &= -3;
        } else if (this.mThemeMode == 2) {
            colorHints |= 2;
        }
        themeColors.setColorHints(colorHints);
        return themeColors;
    }

    /* JADX WARNING: Removed duplicated region for block: B:96:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:102:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x01cb  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x01cb  */
    /* JADX WARNING: Removed duplicated region for block: B:102:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:102:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x01cb  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x01cb  */
    /* JADX WARNING: Removed duplicated region for block: B:102:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:102:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x01cb  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void generateCrop(WallpaperData wallpaper) {
        boolean success;
        Throwable th;
        WallpaperData wallpaperData = wallpaper;
        boolean success2 = false;
        Rect cropHint = new Rect(wallpaperData.cropHint);
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(wallpaperData.wallpaperFile.getAbsolutePath(), options);
        Rect rect;
        if (options.outWidth <= 0) {
            success = false;
            rect = cropHint;
        } else if (options.outHeight <= 0) {
            success = false;
            rect = cropHint;
        } else {
            boolean needCrop = false;
            if (cropHint.isEmpty()) {
                cropHint.top = 0;
                cropHint.left = 0;
                cropHint.right = options.outWidth;
                cropHint.bottom = options.outHeight;
            } else {
                int i;
                int i2;
                if (cropHint.right > options.outWidth) {
                    i = options.outWidth - cropHint.right;
                } else {
                    i = 0;
                }
                if (cropHint.bottom > options.outHeight) {
                    i2 = options.outHeight - cropHint.bottom;
                } else {
                    i2 = 0;
                }
                cropHint.offset(i, i2);
                if (cropHint.left < 0) {
                    cropHint.left = 0;
                }
                if (cropHint.top < 0) {
                    cropHint.top = 0;
                }
                boolean z = options.outHeight > cropHint.height() || options.outWidth > cropHint.width();
                needCrop = z;
            }
            boolean needScale = wallpaperData.height != (cropHint.height() > cropHint.width() ? cropHint.height() : cropHint.width());
            if (needScale && wallpaperData.width == cropHint.width() && wallpaperData.height == cropHint.height()) {
                needScale = false;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("same size wallpaper ,not need to scale.  w=");
                stringBuilder.append(cropHint.width());
                stringBuilder.append(" h=");
                stringBuilder.append(cropHint.height());
                Slog.v(str, stringBuilder.toString());
            }
            if (needCrop || needScale) {
                FileOutputStream f = null;
                Options scaler = null;
                BufferedOutputStream bos = null;
                try {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(wallpaperData.wallpaperFile.getAbsolutePath(), false);
                    int scale = 1;
                    while (2 * scale < cropHint.height() / wallpaperData.height) {
                        scale *= 2;
                    }
                    if (scale > 1) {
                        try {
                            scaler = new Options();
                            scaler.inSampleSize = scale;
                        } catch (Exception e) {
                            success = false;
                            rect = cropHint;
                        } catch (Throwable th2) {
                            th = th2;
                            success = false;
                            rect = cropHint;
                            IoUtils.closeQuietly(bos);
                            IoUtils.closeQuietly(f);
                            throw th;
                        }
                    }
                    Bitmap cropped = decoder.decodeRegion(cropHint, scaler);
                    decoder.recycle();
                    if (cropped == null) {
                        Slog.e(TAG, "Could not decode new wallpaper");
                        rect = cropHint;
                    } else {
                        cropHint.offsetTo(0, 0);
                        cropHint.right /= scale;
                        cropHint.bottom /= scale;
                        int destWidth = (int) (((float) cropHint.width()) * (((float) wallpaperData.height) / ((float) cropHint.height())));
                        success = false;
                        try {
                        } catch (Exception e2) {
                            rect = cropHint;
                            IoUtils.closeQuietly(bos);
                            IoUtils.closeQuietly(f);
                            success2 = success;
                            if (!success2) {
                            }
                            if (wallpaperData.cropFile.exists()) {
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            rect = cropHint;
                            IoUtils.closeQuietly(bos);
                            IoUtils.closeQuietly(f);
                            throw th;
                        }
                        try {
                            Bitmap finalCrop = Bitmap.createScaledBitmap(cropped, destWidth, wallpaperData.height, true);
                            f = new FileOutputStream(wallpaperData.cropFile);
                            bos = new BufferedOutputStream(f, 32768);
                            finalCrop.compress(CompressFormat.JPEG, 100, bos);
                            bos.flush();
                            success2 = true;
                        } catch (Exception e3) {
                            IoUtils.closeQuietly(bos);
                            IoUtils.closeQuietly(f);
                            success2 = success;
                            if (success2) {
                            }
                            if (wallpaperData.cropFile.exists()) {
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            IoUtils.closeQuietly(bos);
                            IoUtils.closeQuietly(f);
                            throw th;
                        }
                    }
                    IoUtils.closeQuietly(bos);
                    IoUtils.closeQuietly(f);
                } catch (Exception e4) {
                    success = false;
                    rect = cropHint;
                    IoUtils.closeQuietly(bos);
                    IoUtils.closeQuietly(f);
                    success2 = success;
                    if (success2) {
                    }
                    if (wallpaperData.cropFile.exists()) {
                    }
                } catch (Throwable th5) {
                    th = th5;
                    success = false;
                    rect = cropHint;
                    IoUtils.closeQuietly(bos);
                    IoUtils.closeQuietly(f);
                    throw th;
                }
                if (success2) {
                    Slog.e(TAG, "Unable to apply new wallpaper");
                    wallpaperData.cropFile.delete();
                }
                if (wallpaperData.cropFile.exists()) {
                    SELinux.restorecon(wallpaperData.cropFile.getAbsoluteFile());
                    return;
                }
                return;
            }
            success2 = FileUtils.copyFile(wallpaperData.wallpaperFile, wallpaperData.cropFile);
            if (!success2) {
                wallpaperData.cropFile.delete();
            }
            rect = cropHint;
            if (success2) {
            }
            if (wallpaperData.cropFile.exists()) {
            }
        }
        Slog.w(TAG, "Invalid wallpaper data");
        success2 = false;
        if (success2) {
        }
        if (wallpaperData.cropFile.exists()) {
        }
    }

    int makeWallpaperIdLocked() {
        do {
            this.mWallpaperId++;
        } while (this.mWallpaperId == 0);
        return this.mWallpaperId;
    }

    public WallpaperManagerService(Context context) {
        this.mContext = context;
        this.mShuttingDown = false;
        this.mImageWallpaper = ComponentName.unflattenFromString(context.getResources().getString(17040204));
        this.mDefaultWallpaperComponent = WallpaperManager.getDefaultWallpaperComponent(context);
        this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mMonitor = new MyPackageMonitor();
        this.mColorsChangedListeners = new SparseArray();
    }

    void initialize() {
        this.mMonitor.register(this.mContext, null, UserHandle.ALL, true);
        getWallpaperDir(0).mkdirs();
        loadSettingsLocked(0, false);
        getWallpaperSafeLocked(0, 1);
    }

    private static File getWallpaperDir(int userId) {
        return Environment.getUserSystemDirectory(userId);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        for (int i = 0; i < this.mWallpaperMap.size(); i++) {
            ((WallpaperData) this.mWallpaperMap.valueAt(i)).wallpaperObserver.stopWatching();
        }
    }

    void systemReady() {
        initialize();
        WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.get(0);
        if (this.mImageWallpaper.equals(wallpaper.nextWallpaperComponent)) {
            if (!wallpaper.cropExists()) {
                generateCrop(wallpaper);
            }
            if (!wallpaper.cropExists()) {
                clearWallpaperLocked(false, 1, 0, null);
            }
        }
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                    WallpaperManagerService.this.onRemoveUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
                }
            }
        }, userFilter);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                    synchronized (WallpaperManagerService.this.mLock) {
                        WallpaperManagerService.this.mShuttingDown = true;
                    }
                }
            }
        }, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    WallpaperManagerService.this.switchUser(newUserId, reply);
                }
            }, TAG);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    public String getName() {
        if (Binder.getCallingUid() == 1000) {
            String str;
            synchronized (this.mLock) {
                str = ((WallpaperData) this.mWallpaperMap.get(0)).name;
            }
            return str;
        }
        throw new RuntimeException("getName() can only be called from the system process");
    }

    void stopObserver(WallpaperData wallpaper) {
        if (wallpaper != null) {
            if (wallpaper.wallpaperObserver != null) {
                wallpaper.wallpaperObserver.stopWatching();
                wallpaper.wallpaperObserver = null;
            }
            if (wallpaper.themeSettingsObserver != null) {
                wallpaper.themeSettingsObserver.stopObserving(this.mContext);
                wallpaper.themeSettingsObserver = null;
            }
        }
    }

    void stopObserversLocked(int userId) {
        stopObserver((WallpaperData) this.mWallpaperMap.get(userId));
        stopObserver((WallpaperData) this.mLockWallpaperMap.get(userId));
        this.mWallpaperMap.remove(userId);
        this.mLockWallpaperMap.remove(userId);
    }

    public void onBootPhase(int phase) {
        if (phase == 550) {
            systemReady();
        } else if (phase == 600) {
            switchUser(0, null);
        }
    }

    public void onUnlockUser(final int userId) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == userId) {
                if (this.mWaitingForUnlock) {
                    switchWallpaper(getWallpaperSafeLocked(userId, 1), null);
                }
                if (this.mUserRestorecon.get(userId) != Boolean.TRUE) {
                    this.mUserRestorecon.put(userId, Boolean.TRUE);
                    BackgroundThread.getHandler().post(new Runnable() {
                        public void run() {
                            File wallpaperDir = WallpaperManagerService.getWallpaperDir(userId);
                            for (String filename : WallpaperManagerService.sPerUserFiles) {
                                File f = new File(wallpaperDir, filename);
                                if (f.exists()) {
                                    SELinux.restorecon(f);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    void onRemoveUser(int userId) {
        if (userId >= 1) {
            File wallpaperDir = getWallpaperDir(userId);
            synchronized (this.mLock) {
                stopObserversLocked(userId);
                for (String filename : sPerUserFiles) {
                    new File(wallpaperDir, filename).delete();
                }
                this.mUserRestorecon.remove(userId);
            }
        }
    }

    void switchUser(int userId, IRemoteCallback reply) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == userId && this.mIsLoadLiveWallpaper) {
                return;
            }
            this.mIsLoadLiveWallpaper = true;
            this.mCurrentUserId = userId;
            if (this.mLastWallpaper != null) {
                handleWallpaperObserverEvent(this.mLastWallpaper);
            }
            WallpaperData systemWallpaper = getWallpaperSafeLocked(userId, 1);
            WallpaperData tmpLockWallpaper = (WallpaperData) this.mLockWallpaperMap.get(userId);
            WallpaperData lockWallpaper = tmpLockWallpaper == null ? systemWallpaper : tmpLockWallpaper;
            if (systemWallpaper.wallpaperObserver == null) {
                systemWallpaper.wallpaperObserver = new WallpaperObserver(systemWallpaper);
                systemWallpaper.wallpaperObserver.startWatching();
            }
            if (systemWallpaper.themeSettingsObserver == null) {
                systemWallpaper.themeSettingsObserver = new ThemeSettingsObserver(null);
                systemWallpaper.themeSettingsObserver.startObserving(this.mContext);
            }
            this.mThemeMode = Secure.getInt(this.mContext.getContentResolver(), "theme_mode", 0);
            switchWallpaper(systemWallpaper, reply);
            FgThread.getHandler().post(new -$$Lambda$WallpaperManagerService$KpV9TczlJklVG4VNZncaU86_KtQ(this, systemWallpaper, lockWallpaper));
        }
    }

    public static /* synthetic */ void lambda$switchUser$0(WallpaperManagerService wallpaperManagerService, WallpaperData systemWallpaper, WallpaperData lockWallpaper) {
        wallpaperManagerService.notifyWallpaperColorsChanged(systemWallpaper, 1);
        wallpaperManagerService.notifyWallpaperColorsChanged(lockWallpaper, 2);
    }

    /* JADX WARNING: Missing block: B:26:0x006f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void switchWallpaper(WallpaperData wallpaper, IRemoteCallback reply) {
        Throwable th;
        WallpaperData wallpaperData = wallpaper;
        synchronized (this.mLock) {
            IRemoteCallback iRemoteCallback;
            try {
                this.mWaitingForUnlock = false;
                ComponentName cname = wallpaperData.wallpaperComponent != null ? wallpaperData.wallpaperComponent : wallpaperData.nextWallpaperComponent;
                if (bindWallpaperComponentLocked(cname, true, false, wallpaperData, reply)) {
                    iRemoteCallback = reply;
                } else {
                    ServiceInfo si;
                    ServiceInfo si2 = null;
                    try {
                        si = this.mIPackageManager.getServiceInfo(cname, 262144, wallpaperData.userId);
                    } catch (RemoteException e) {
                        si = si2;
                    }
                    if (si == null) {
                        Slog.w(TAG, "Failure starting previous wallpaper; clearing");
                        clearWallpaperLocked(false, 1, wallpaperData.userId, reply);
                    } else {
                        iRemoteCallback = reply;
                        Slog.w(TAG, "Wallpaper isn't direct boot aware; using fallback until unlocked");
                        wallpaperData.wallpaperComponent = wallpaperData.nextWallpaperComponent;
                        WallpaperData fallback = new WallpaperData(wallpaperData.userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                        ensureSaneWallpaperData(fallback);
                        bindWallpaperComponentLocked(this.mImageWallpaper, true, false, fallback, iRemoteCallback);
                        this.mWaitingForUnlock = true;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void clearWallpaper(String callingPackage, int which, int userId) {
        checkPermission("android.permission.SET_WALLPAPER");
        if (isWallpaperSupported(callingPackage) && isSetWallpaperAllowed(callingPackage)) {
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "clearWallpaper", null);
            userId = null;
            synchronized (this.mLock) {
                clearWallpaperLocked(false, which, userId2, null);
                if (which == 2) {
                    userId = (WallpaperData) this.mLockWallpaperMap.get(userId2);
                }
                if (which == 1 || userId == 0) {
                    userId = (WallpaperData) this.mWallpaperMap.get(userId2);
                }
            }
            if (userId != 0) {
                notifyWallpaperColorsChanged(userId, which);
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:28:0x006c=Splitter:B:28:0x006c, B:48:0x009c=Splitter:B:48:0x009c} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void clearWallpaperLocked(boolean defaultFailed, int which, int userId, IRemoteCallback reply) {
        int i = which;
        int i2 = userId;
        IRemoteCallback iRemoteCallback = reply;
        if (i == 1 || i == 2) {
            WallpaperData wallpaper;
            if (i == 2) {
                wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i2);
                if (wallpaper == null) {
                    return;
                }
            }
            wallpaper = (WallpaperData) this.mWallpaperMap.get(i2);
            if (wallpaper == null) {
                loadSettingsLocked(i2, false);
                wallpaper = (WallpaperData) this.mWallpaperMap.get(i2);
            }
            WallpaperData wallpaper2 = wallpaper;
            if (wallpaper2 != null) {
                long ident = Binder.clearCallingIdentity();
                try {
                    if (wallpaper2.wallpaperFile.exists()) {
                        wallpaper2.wallpaperFile.delete();
                        wallpaper2.cropFile.delete();
                        if (i == 2) {
                            this.mLockWallpaperMap.remove(i2);
                            IWallpaperManagerCallback cb = this.mKeyguardListener;
                            if (cb != null) {
                                try {
                                    cb.onWallpaperChanged();
                                } catch (RemoteException e) {
                                }
                            }
                            saveSettingsLocked(i2);
                            return;
                        }
                    }
                    RuntimeException e2 = null;
                    try {
                        wallpaper2.primaryColors = null;
                        wallpaper2.imageWallpaperPending = false;
                        if (i2 != this.mCurrentUserId) {
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                        ComponentName componentName;
                        this.mDefaultWallpaperComponent = null;
                        if (defaultFailed) {
                            componentName = this.mImageWallpaper;
                        } else {
                            componentName = null;
                        }
                        if (bindWallpaperComponentLocked(componentName, true, false, wallpaper2, iRemoteCallback)) {
                            Binder.restoreCallingIdentity(ident);
                            return;
                        }
                        Slog.e(TAG, "Default wallpaper component not found!", e2);
                        clearWallpaperComponentLocked(wallpaper2);
                        if (iRemoteCallback != null) {
                            try {
                                iRemoteCallback.sendResult(null);
                            } catch (RemoteException e3) {
                            }
                        }
                        Binder.restoreCallingIdentity(ident);
                        return;
                    } catch (IllegalArgumentException e1) {
                        e2 = e1;
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                return;
            }
        }
        throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to clear");
    }

    public boolean hasNamedWallpaper(String name) {
        synchronized (this.mLock) {
            long ident = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = ((UserManager) this.mContext.getSystemService("user")).getUsers();
                for (UserInfo user : users) {
                    if (!user.isManagedProfile()) {
                        WallpaperData wd = (WallpaperData) this.mWallpaperMap.get(user.id);
                        if (wd == null) {
                            loadSettingsLocked(user.id, false);
                            wd = (WallpaperData) this.mWallpaperMap.get(user.id);
                        }
                        if (wd != null && name.equals(wd.name)) {
                            return true;
                        }
                    }
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private Point getDefaultDisplaySize() {
        Point p = new Point();
        ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRealSize(p);
        return p;
    }

    /* JADX WARNING: Missing block: B:30:0x0065, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setDimensionHints(int width, int height, String callingPackage) throws RemoteException {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (isWallpaperSupported(callingPackage)) {
            synchronized (this.mLock) {
                int userId = UserHandle.getCallingUserId();
                WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
                if (width <= 0 || height <= 0) {
                    throw new IllegalArgumentException("width and height must be > 0");
                }
                Point displaySize = getDefaultDisplaySize();
                width = Math.max(width, displaySize.x);
                height = Math.max(height, displaySize.y);
                if (!(width == wallpaper.width && height == wallpaper.height)) {
                    wallpaper.width = width;
                    wallpaper.height = height;
                    saveSettingsLocked(userId);
                    if (this.mCurrentUserId != userId) {
                    } else if (wallpaper.connection != null) {
                        if (wallpaper.connection.mEngine != null) {
                            try {
                                wallpaper.connection.mEngine.setDesiredSize(width, height);
                            } catch (RemoteException e) {
                            }
                            notifyCallbacksLocked(wallpaper);
                        } else if (wallpaper.connection.mService != null) {
                            wallpaper.connection.mDimensionsChanged = true;
                        }
                    }
                }
            }
        }
    }

    public int getWidthHint() throws RemoteException {
        synchronized (this.mLock) {
            WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaper != null) {
                int i = wallpaper.width;
                return i;
            }
            return 0;
        }
    }

    public int getHeightHint() throws RemoteException {
        synchronized (this.mLock) {
            WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.get(UserHandle.getCallingUserId());
            if (wallpaper != null) {
                int i = wallpaper.height;
                return i;
            }
            return 0;
        }
    }

    /* JADX WARNING: Missing block: B:33:0x0060, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setDisplayPadding(Rect padding, String callingPackage) {
        checkPermission("android.permission.SET_WALLPAPER_HINTS");
        if (isWallpaperSupported(callingPackage)) {
            synchronized (this.mLock) {
                int userId = UserHandle.getCallingUserId();
                WallpaperData wallpaper = getWallpaperSafeLocked(userId, 1);
                if (padding.left < 0 || padding.top < 0 || padding.right < 0 || padding.bottom < 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("padding must be positive: ");
                    stringBuilder.append(padding);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (!padding.equals(wallpaper.padding)) {
                    wallpaper.padding.set(padding);
                    saveSettingsLocked(userId);
                    if (this.mCurrentUserId != userId) {
                    } else if (wallpaper.connection != null) {
                        if (wallpaper.connection.mEngine != null) {
                            try {
                                wallpaper.connection.mEngine.setDisplayPadding(padding);
                            } catch (RemoteException e) {
                            }
                            notifyCallbacksLocked(wallpaper);
                        } else if (wallpaper.connection.mService != null) {
                            wallpaper.connection.mPaddingChanged = true;
                        }
                    }
                }
            }
        }
    }

    private void enforceCallingOrSelfPermissionAndAppOp(String permission, String callingPkg, int callingUid, String message) {
        this.mContext.enforceCallingOrSelfPermission(permission, message);
        String opName = AppOpsManager.permissionToOp(permission);
        if (opName != null && this.mAppOpsManager.noteOp(opName, callingUid, callingPkg) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(message);
            stringBuilder.append(": ");
            stringBuilder.append(callingPkg);
            stringBuilder.append(" is not allowed to ");
            stringBuilder.append(permission);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public ParcelFileDescriptor getWallpaper(String callingPkg, IWallpaperManagerCallback cb, int which, Bundle outParams, int wallpaperUserId) {
        FileNotFoundException e;
        IWallpaperManagerCallback iWallpaperManagerCallback = cb;
        int i = which;
        Bundle bundle = outParams;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_WALLPAPER_INTERNAL") != 0) {
            enforceCallingOrSelfPermissionAndAppOp("android.permission.READ_EXTERNAL_STORAGE", callingPkg, Binder.getCallingUid(), "read wallpaper");
        } else {
            String str = callingPkg;
        }
        int wallpaperUserId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), wallpaperUserId, false, true, "getWallpaper", null);
        if (i == 1 || i == 2) {
            synchronized (this.mLock) {
                SparseArray<WallpaperData> sparseArray;
                if (i == 2) {
                    try {
                        sparseArray = this.mLockWallpaperMap;
                    } catch (FileNotFoundException e2) {
                        Slog.w(TAG, "Error getting wallpaper", e2);
                        return null;
                    } catch (NullPointerException e22) {
                        Slog.w(TAG, "getting wallpaper null", e22);
                        return null;
                    } catch (Throwable th) {
                    }
                } else {
                    sparseArray = this.mWallpaperMap;
                }
                WallpaperData wallpaper = (WallpaperData) sparseArray.get(wallpaperUserId2);
                if (wallpaper == null) {
                    return null;
                }
                if (bundle != null) {
                    bundle.putInt("width", wallpaper.width);
                    bundle.putInt("height", wallpaper.height);
                }
                if (iWallpaperManagerCallback != null && wallpaper.callbacks.isContainIBinder(iWallpaperManagerCallback) == null) {
                    wallpaper.callbacks.register(iWallpaperManagerCallback);
                }
                if (wallpaper.cropFile.exists() == null) {
                    return null;
                }
                e22 = ParcelFileDescriptor.open(wallpaper.cropFile, 268435456);
                return e22;
            }
        }
        throw new IllegalArgumentException("Must specify exactly one kind of wallpaper to read");
    }

    public WallpaperInfo getWallpaperInfo(int userId) {
        userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getWallpaperInfo", null);
        synchronized (this.mLock) {
            WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.get(userId);
            if (wallpaper == null || wallpaper.connection == null) {
                return null;
            }
            WallpaperInfo wallpaperInfo = wallpaper.connection.mInfo;
            return wallpaperInfo;
        }
    }

    public int getWallpaperIdForUser(int which, int userId) {
        userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getWallpaperIdForUser", null);
        if (which == 1 || which == 2) {
            SparseArray<WallpaperData> map = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
            synchronized (this.mLock) {
                WallpaperData wallpaper = (WallpaperData) map.get(userId);
                if (wallpaper != null) {
                    int i = wallpaper.wallpaperId;
                    return i;
                }
                return -1;
            }
        }
        throw new IllegalArgumentException("Must specify exactly one kind of wallpaper");
    }

    public void registerWallpaperColorsCallback(IWallpaperManagerCallback cb, int userId) {
        userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "registerWallpaperColorsCallback", null);
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> userColorsChangedListeners = (RemoteCallbackList) this.mColorsChangedListeners.get(userId);
            if (userColorsChangedListeners == null) {
                userColorsChangedListeners = new RemoteCallbackList();
                this.mColorsChangedListeners.put(userId, userColorsChangedListeners);
            }
            userColorsChangedListeners.register(cb);
        }
    }

    public void unregisterWallpaperColorsCallback(IWallpaperManagerCallback cb, int userId) {
        userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, true, true, "unregisterWallpaperColorsCallback", null);
        synchronized (this.mLock) {
            RemoteCallbackList<IWallpaperManagerCallback> userColorsChangedListeners = (RemoteCallbackList) this.mColorsChangedListeners.get(userId);
            if (userColorsChangedListeners != null) {
                userColorsChangedListeners.unregister(cb);
            }
        }
    }

    public void setInAmbientMode(boolean inAmbienMode, boolean animated) {
        IWallpaperEngine engine;
        synchronized (this.mLock) {
            IWallpaperEngine engine2;
            this.mInAmbientMode = inAmbienMode;
            WallpaperData data = (WallpaperData) this.mWallpaperMap.get(this.mCurrentUserId);
            if (data == null || data.connection == null || data.connection.mInfo == null || !data.connection.mInfo.getSupportsAmbientMode()) {
                engine2 = null;
            } else {
                engine2 = data.connection.mEngine;
            }
            engine = engine2;
        }
        if (engine != null) {
            try {
                engine.setInAmbientMode(inAmbienMode, animated);
            } catch (RemoteException e) {
            }
        }
    }

    public boolean setLockWallpaperCallback(IWallpaperManagerCallback cb) {
        checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW");
        synchronized (this.mLock) {
            this.mKeyguardListener = cb;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:23:0x004b, code skipped:
            if (r0 == false) goto L_0x0050;
     */
    /* JADX WARNING: Missing block: B:24:0x004d, code skipped:
            extractColors(r2);
     */
    /* JADX WARNING: Missing block: B:25:0x0050, code skipped:
            r1 = r9.mLock;
     */
    /* JADX WARNING: Missing block: B:26:0x0052, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:28:?, code skipped:
            r3 = getThemeColorsLocked(r2.primaryColors);
     */
    /* JADX WARNING: Missing block: B:29:0x0059, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:30:0x005a, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public WallpaperColors getWallpaperColors(int which, int userId) throws RemoteException {
        boolean shouldExtract = true;
        if (which == 2 || which == 1) {
            userId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "getWallpaperColors", null);
            WallpaperData wallpaperData = null;
            synchronized (this.mLock) {
                if (which == 2) {
                    try {
                        wallpaperData = (WallpaperData) this.mLockWallpaperMap.get(userId);
                    } catch (Throwable th) {
                        while (true) {
                        }
                    }
                }
                if (wallpaperData == null) {
                    wallpaperData = (WallpaperData) this.mWallpaperMap.get(userId);
                }
                if (wallpaperData == null) {
                    return null;
                } else if (wallpaperData.primaryColors != null) {
                    shouldExtract = false;
                }
            }
        } else {
            throw new IllegalArgumentException("which should be either FLAG_LOCK or FLAG_SYSTEM");
        }
    }

    public ParcelFileDescriptor setWallpaper(String name, String callingPackage, Rect cropHint, boolean allowBackup, Bundle extras, int which, IWallpaperManagerCallback completion, int userId) {
        Throwable th;
        String str = callingPackage;
        Rect rect = cropHint;
        int i = which;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.WALLPAPERMANAGER_SETWALLPAPER);
        if (isMdmDisableChangeWallpaper()) {
            return null;
        }
        int userId2 = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), userId, false, true, "changing wallpaper", null);
        checkPermission("android.permission.SET_WALLPAPER");
        String str2;
        boolean z;
        Bundle bundle;
        IWallpaperManagerCallback iWallpaperManagerCallback;
        if ((i & 3) == 0) {
            str2 = name;
            z = allowBackup;
            bundle = extras;
            iWallpaperManagerCallback = completion;
            String msg = "Must specify a valid wallpaper category to set";
            Slog.e(TAG, "Must specify a valid wallpaper category to set");
            throw new IllegalArgumentException("Must specify a valid wallpaper category to set");
        } else if (isWallpaperSupported(str) && isSetWallpaperAllowed(str)) {
            Rect cropHint2;
            if (rect == null) {
                cropHint2 = new Rect(0, 0, 0, 0);
            } else if (cropHint.isEmpty() || rect.left < 0 || rect.top < 0) {
                str2 = name;
                z = allowBackup;
                bundle = extras;
                iWallpaperManagerCallback = completion;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid crop rect supplied: ");
                stringBuilder.append(rect);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else {
                cropHint2 = rect;
            }
            synchronized (this.mLock) {
                if ((i & 3) == 0) {
                    try {
                        if (this.mLockWallpaperMap.get(userId2) == null) {
                            migrateSystemToLockWallpaperLocked(userId2);
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        str2 = name;
                        z = allowBackup;
                        bundle = extras;
                        iWallpaperManagerCallback = completion;
                        throw th;
                    }
                }
                WallpaperData wallpaper = getWallpaperSafeLocked(userId2, i);
                long ident = Binder.clearCallingIdentity();
                try {
                    ParcelFileDescriptor pfd = updateWallpaperBitmapLocked(name, wallpaper, extras);
                    if (pfd != null) {
                        wallpaper.imageWallpaperPending = true;
                        wallpaper.whichPending = i;
                        try {
                            wallpaper.setComplete = completion;
                            wallpaper.cropHint.set(cropHint2);
                            try {
                                wallpaper.allowBackup = allowBackup;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            z = allowBackup;
                            Binder.restoreCallingIdentity(ident);
                            throw th;
                        }
                    }
                    z = allowBackup;
                    iWallpaperManagerCallback = completion;
                    Binder.restoreCallingIdentity(ident);
                    return pfd;
                } catch (Throwable th5) {
                    th = th5;
                    throw th;
                }
            }
        } else {
            str2 = name;
            z = allowBackup;
            bundle = extras;
            iWallpaperManagerCallback = completion;
            return null;
        }
    }

    private void migrateSystemToLockWallpaperLocked(int userId) {
        WallpaperData sysWP = (WallpaperData) this.mWallpaperMap.get(userId);
        if (sysWP != null) {
            WallpaperData lockWP = new WallpaperData(userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
            lockWP.wallpaperId = sysWP.wallpaperId;
            lockWP.cropHint.set(sysWP.cropHint);
            lockWP.width = sysWP.width;
            lockWP.height = sysWP.height;
            lockWP.allowBackup = sysWP.allowBackup;
            lockWP.primaryColors = sysWP.primaryColors;
            try {
                Os.rename(sysWP.wallpaperFile.getAbsolutePath(), lockWP.wallpaperFile.getAbsolutePath());
                Os.rename(sysWP.cropFile.getAbsolutePath(), lockWP.cropFile.getAbsolutePath());
                this.mLockWallpaperMap.put(userId, lockWP);
            } catch (ErrnoException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't migrate system wallpaper: ");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
                lockWP.wallpaperFile.delete();
                lockWP.cropFile.delete();
            }
        }
    }

    ParcelFileDescriptor updateWallpaperBitmapLocked(String name, WallpaperData wallpaper, Bundle extras) {
        if (name == null) {
            name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        try {
            File dir = getWallpaperDir(wallpaper.userId);
            if (!dir.exists()) {
                dir.mkdir();
                FileUtils.setPermissions(dir.getPath(), 505, -1, -1);
            }
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(wallpaper.wallpaperFile, 1006632960);
            if (!SELinux.restorecon(wallpaper.wallpaperFile)) {
                return null;
            }
            wallpaper.name = name;
            wallpaper.wallpaperId = makeWallpaperIdLocked();
            if (extras != null) {
                extras.putInt("android.service.wallpaper.extra.ID", wallpaper.wallpaperId);
            }
            wallpaper.primaryColors = null;
            return fd;
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "Error setting wallpaper", e);
            return null;
        }
    }

    public void setWallpaperComponentChecked(ComponentName name, String callingPackage, int userId) {
        if (isWallpaperSupported(callingPackage) && isSetWallpaperAllowed(callingPackage)) {
            setWallpaperComponent(name, userId);
        }
    }

    public void setWallpaperComponent(ComponentName name) {
        setWallpaperComponent(name, UserHandle.getCallingUserId());
    }

    private void setWallpaperComponent(ComponentName name, int userId) {
        if (!isMdmDisableChangeWallpaper()) {
            WallpaperData wallpaper;
            userId = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), userId, false, true, "changing live wallpaper", null);
            checkPermission("android.permission.SET_WALLPAPER_COMPONENT");
            int which = 1;
            boolean shouldNotifyColors = false;
            synchronized (this.mLock) {
                wallpaper = (WallpaperData) this.mWallpaperMap.get(userId);
                if (wallpaper != null) {
                    long ident = Binder.clearCallingIdentity();
                    if (this.mLockWallpaperMap.get(userId) == null) {
                        which = 1 | 2;
                    }
                    try {
                        wallpaper.imageWallpaperPending = false;
                        boolean same = changingToSame(name, wallpaper);
                        handleWallpaperObserverEvent(wallpaper);
                        if (bindWallpaperComponentLocked(name, false, true, wallpaper, null)) {
                            if (!same) {
                                wallpaper.primaryColors = null;
                            }
                            wallpaper.nextWallpaperComponent = wallpaper.wallpaperComponent;
                            wallpaper.wallpaperId = makeWallpaperIdLocked();
                            notifyCallbacksLocked(wallpaper);
                            shouldNotifyColors = true;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Wallpaper not yet initialized for user ");
                    stringBuilder.append(userId);
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
            WallpaperData wallpaper2 = wallpaper;
            if (shouldNotifyColors) {
                notifyWallpaperColorsChanged(wallpaper2, which);
            }
        }
    }

    private boolean changingToSame(ComponentName componentName, WallpaperData wallpaper) {
        if (wallpaper.connection != null) {
            if (wallpaper.wallpaperComponent == null) {
                if (componentName == null) {
                    return true;
                }
            } else if (wallpaper.wallpaperComponent.equals(componentName)) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:89:0x01f6  */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x01f0  */
    /* JADX WARNING: Missing block: B:38:0x00f0, code skipped:
            r9 = new android.app.WallpaperInfo(r1.mContext, (android.content.pm.ResolveInfo) r11.get(r12));
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean bindWallpaperComponentLocked(ComponentName componentName, boolean force, boolean fromUser, WallpaperData wallpaper, IRemoteCallback reply) {
        RemoteException e;
        String msg;
        ComponentName componentName2 = componentName;
        WallpaperData wallpaperData = wallpaper;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bindWallpaperComponentLocked: componentName=");
        stringBuilder.append(componentName2);
        Slog.v(str, stringBuilder.toString());
        if (!force && changingToSame(componentName2, wallpaperData)) {
            return true;
        }
        if (componentName2 == null) {
            try {
                componentName2 = this.mDefaultWallpaperComponent;
                if (componentName2 == null) {
                    componentName2 = this.mImageWallpaper;
                    Slog.v(TAG, "No default component; using image wallpaper");
                }
            } catch (XmlPullParserException e2) {
                if (fromUser) {
                    throw new IllegalArgumentException(e2);
                }
                Slog.w(TAG, e2);
                return false;
            } catch (IOException e3) {
                if (fromUser) {
                    throw new IllegalArgumentException(e3);
                }
                Slog.w(TAG, e3);
                return false;
            } catch (RemoteException e4) {
                e = e4;
                IRemoteCallback iRemoteCallback = reply;
                msg = new StringBuilder();
                msg.append("Remote exception for ");
                msg.append(componentName2);
                msg.append("\n");
                msg.append(e);
                msg = msg.toString();
                if (fromUser) {
                }
            }
        }
        int serviceUserId = wallpaperData.userId;
        ServiceInfo si = this.mIPackageManager.getServiceInfo(componentName2, 4224, serviceUserId);
        if (si == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Attempted wallpaper ");
            stringBuilder.append(componentName2);
            stringBuilder.append(" is unavailable");
            Slog.w(str, stringBuilder.toString());
            this.mIsLoadLiveWallpaper = false;
            return false;
        } else if ("android.permission.BIND_WALLPAPER".equals(si.permission)) {
            WallpaperInfo wi = null;
            Intent intent = new Intent("android.service.wallpaper.WallpaperService");
            if (!(componentName2 == null || componentName2.equals(this.mImageWallpaper))) {
                List<ResolveInfo> ris = this.mIPackageManager.queryIntentServices(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 128, serviceUserId).getList();
                int i = 0;
                while (true) {
                    int i2 = i;
                    if (i2 >= ris.size()) {
                        break;
                    }
                    ServiceInfo rsi = ((ResolveInfo) ris.get(i2)).serviceInfo;
                    if (rsi.name.equals(si.name) && rsi.packageName.equals(si.packageName)) {
                        break;
                    }
                    i = i2 + 1;
                }
                if (wi == null) {
                    str = new StringBuilder();
                    str.append("Selected service is not a wallpaper: ");
                    str.append(componentName2);
                    str = str.toString();
                    if (fromUser) {
                        throw new SecurityException(str);
                    }
                    Slog.w(TAG, str);
                    return false;
                }
            }
            WallpaperConnection newConn = new WallpaperConnection(wi, wallpaperData);
            intent.setComponent(componentName2);
            intent.putExtra("android.intent.extra.client_label", 17041349);
            intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(this.mContext, 0, Intent.createChooser(new Intent("android.intent.action.SET_WALLPAPER"), this.mContext.getText(17039750)), 0, null, new UserHandle(serviceUserId)));
            if (this.mContext.bindServiceAsUser(intent, newConn, 570425345, new UserHandle(serviceUserId))) {
                reportWallpaper(componentName2);
                if (wallpaperData.userId == this.mCurrentUserId && this.mLastWallpaper != null) {
                    detachWallpaperLocked(this.mLastWallpaper);
                }
                wallpaperData.wallpaperComponent = componentName2;
                wallpaperData.connection = newConn;
                try {
                    newConn.mReply = reply;
                    try {
                        if (wallpaperData.userId == this.mCurrentUserId) {
                            this.mIWindowManager.addWindowToken(newConn.mToken, 2013, 0);
                            this.mLastWallpaper = wallpaperData;
                        }
                    } catch (RemoteException e5) {
                    }
                    return true;
                } catch (RemoteException e6) {
                    e = e6;
                    msg = new StringBuilder();
                    msg.append("Remote exception for ");
                    msg.append(componentName2);
                    msg.append("\n");
                    msg.append(e);
                    msg = msg.toString();
                    if (fromUser) {
                        Slog.w(TAG, msg);
                        return false;
                    }
                    throw new IllegalArgumentException(msg);
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to bind service: ");
            stringBuilder2.append(componentName2);
            str = stringBuilder2.toString();
            if (fromUser) {
                throw new IllegalArgumentException(str);
            }
            Slog.w(TAG, str);
            return false;
        } else {
            str = new StringBuilder();
            str.append("Selected service does not require android.permission.BIND_WALLPAPER: ");
            str.append(componentName2);
            str = str.toString();
            if (fromUser) {
                throw new SecurityException(str);
            }
            Slog.w(TAG, str);
            return false;
        }
    }

    void detachWallpaperLocked(WallpaperData wallpaper) {
        if (wallpaper.connection != null) {
            if (wallpaper.connection.mReply != null) {
                try {
                    wallpaper.connection.mReply.sendResult(null);
                } catch (RemoteException e) {
                }
                wallpaper.connection.mReply = null;
            }
            if (wallpaper.connection.mEngine != null) {
                try {
                    wallpaper.connection.mEngine.destroy();
                } catch (RemoteException e2) {
                }
            }
            this.mContext.unbindService(wallpaper.connection);
            try {
                this.mIWindowManager.removeWindowToken(wallpaper.connection.mToken, 0);
            } catch (RemoteException e3) {
            }
            wallpaper.connection.mService = null;
            wallpaper.connection.mEngine = null;
            wallpaper.connection = null;
        }
    }

    void clearWallpaperComponentLocked(WallpaperData wallpaper) {
        wallpaper.wallpaperComponent = null;
        detachWallpaperLocked(wallpaper);
    }

    void attachServiceLocked(WallpaperConnection conn, WallpaperData wallpaper) {
        try {
            conn.mService.attach(conn, conn.mToken, 2013, false, wallpaper.width, wallpaper.height, wallpaper.padding);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed attaching wallpaper; clearing", e);
            if (!wallpaper.wallpaperUpdating) {
                bindWallpaperComponentLocked(null, false, false, wallpaper, null);
            }
        }
    }

    private void notifyCallbacksLocked(WallpaperData wallpaper) {
        synchronized (wallpaper.callbacks) {
            int n = wallpaper.callbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    ((IWallpaperManagerCallback) wallpaper.callbacks.getBroadcastItem(i)).onWallpaperChanged();
                } catch (RemoteException e) {
                }
            }
            wallpaper.callbacks.finishBroadcast();
        }
        this.mContext.sendBroadcastAsUser(new Intent("android.intent.action.WALLPAPER_CHANGED"), new UserHandle(this.mCurrentUserId));
    }

    private void checkPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Access denied to process: ");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", must have permission ");
            stringBuilder.append(permission);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    public boolean isWallpaperSupported(String callingPackage) {
        return this.mAppOpsManager.checkOpNoThrow(48, Binder.getCallingUid(), callingPackage) == 0;
    }

    public boolean isSetWallpaperAllowed(String callingPackage) {
        if (!Arrays.asList(this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid())).contains(callingPackage)) {
            return false;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
        if (dpm.isDeviceOwnerApp(callingPackage) || dpm.isProfileOwnerApp(callingPackage)) {
            return true;
        }
        return 1 ^ ((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_set_wallpaper");
    }

    public boolean isWallpaperBackupEligible(int which, int userId) {
        if (Binder.getCallingUid() == 1000) {
            WallpaperData wallpaper;
            if (which == 2) {
                wallpaper = (WallpaperData) this.mLockWallpaperMap.get(userId);
            } else {
                wallpaper = (WallpaperData) this.mWallpaperMap.get(userId);
            }
            return wallpaper != null ? wallpaper.allowBackup : false;
        } else {
            throw new SecurityException("Only the system may call isWallpaperBackupEligible");
        }
    }

    private static JournaledFile makeJournaledFile(int userId) {
        String base = new File(getWallpaperDir(userId), WALLPAPER_INFO).getAbsolutePath();
        File file = new File(base);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(base);
        stringBuilder.append(".tmp");
        return new JournaledFile(file, new File(stringBuilder.toString()));
    }

    private void saveSettingsLocked(int userId) {
        JournaledFile journal = makeJournaledFile(userId);
        try {
            XmlSerializer out = new FastXmlSerializer();
            FileOutputStream fstream = new FileOutputStream(journal.chooseForWrite(), false);
            BufferedOutputStream stream = new BufferedOutputStream(fstream);
            out.setOutput(stream, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.get(userId);
            if (wallpaper != null) {
                writeWallpaperAttributes(out, "wp", wallpaper);
            }
            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(userId);
            if (wallpaper != null) {
                writeWallpaperAttributes(out, "kwp", wallpaper);
            }
            out.endDocument();
            stream.flush();
            FileUtils.sync(fstream);
            stream.close();
            journal.commit();
        } catch (IOException e) {
            IoUtils.closeQuietly(null);
            journal.rollback();
        }
    }

    private void writeWallpaperAttributes(XmlSerializer out, String tag, WallpaperData wallpaper) throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag(null, tag);
        out.attribute(null, "id", Integer.toString(wallpaper.wallpaperId));
        out.attribute(null, "width", Integer.toString(wallpaper.width));
        out.attribute(null, "height", Integer.toString(wallpaper.height));
        out.attribute(null, "cropLeft", Integer.toString(wallpaper.cropHint.left));
        out.attribute(null, "cropTop", Integer.toString(wallpaper.cropHint.top));
        out.attribute(null, "cropRight", Integer.toString(wallpaper.cropHint.right));
        out.attribute(null, "cropBottom", Integer.toString(wallpaper.cropHint.bottom));
        if (wallpaper.padding.left != 0) {
            out.attribute(null, "paddingLeft", Integer.toString(wallpaper.padding.left));
        }
        if (wallpaper.padding.top != 0) {
            out.attribute(null, "paddingTop", Integer.toString(wallpaper.padding.top));
        }
        if (wallpaper.padding.right != 0) {
            out.attribute(null, "paddingRight", Integer.toString(wallpaper.padding.right));
        }
        if (wallpaper.padding.bottom != 0) {
            out.attribute(null, "paddingBottom", Integer.toString(wallpaper.padding.bottom));
        }
        if (wallpaper.primaryColors != null) {
            int colorsCount = wallpaper.primaryColors.getMainColors().size();
            out.attribute(null, "colorsCount", Integer.toString(colorsCount));
            if (colorsCount > 0) {
                for (int i = 0; i < colorsCount; i++) {
                    Color wc = (Color) wallpaper.primaryColors.getMainColors().get(i);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("colorValue");
                    stringBuilder.append(i);
                    out.attribute(null, stringBuilder.toString(), Integer.toString(wc.toArgb()));
                }
            }
            out.attribute(null, "colorHints", Integer.toString(wallpaper.primaryColors.getColorHints()));
        }
        out.attribute(null, Settings.ATTR_NAME, wallpaper.name);
        if (!(wallpaper.wallpaperComponent == null || wallpaper.wallpaperComponent.equals(this.mImageWallpaper))) {
            out.attribute(null, "component", wallpaper.wallpaperComponent.flattenToShortString());
        }
        if (wallpaper.allowBackup) {
            out.attribute(null, HealthServiceWrapper.INSTANCE_HEALTHD, "true");
        }
        out.endTag(null, tag);
    }

    private void migrateFromOld() {
        File preNWallpaper = new File(getWallpaperDir(0), WALLPAPER_CROP);
        File originalWallpaper = new File("/data/data/com.android.settings/files/wallpaper");
        File newWallpaper = new File(getWallpaperDir(0), WALLPAPER);
        if (preNWallpaper.exists()) {
            if (!newWallpaper.exists()) {
                FileUtils.copyFile(preNWallpaper, newWallpaper);
            }
        } else if (originalWallpaper.exists()) {
            File oldInfo = new File("/data/system/wallpaper_info.xml");
            if (oldInfo.exists()) {
                oldInfo.renameTo(new File(getWallpaperDir(0), WALLPAPER_INFO));
            }
            FileUtils.copyFile(originalWallpaper, preNWallpaper);
            originalWallpaper.renameTo(newWallpaper);
        }
    }

    private int getAttributeInt(XmlPullParser parser, String name, int defValue) {
        String value = parser.getAttributeValue(null, name);
        if (value == null) {
            return defValue;
        }
        return Integer.parseInt(value);
    }

    private WallpaperData getWallpaperSafeLocked(int userId, int which) {
        SparseArray<WallpaperData> whichSet = which == 2 ? this.mLockWallpaperMap : this.mWallpaperMap;
        WallpaperData wallpaper = (WallpaperData) whichSet.get(userId);
        if (wallpaper != null) {
            return wallpaper;
        }
        loadSettingsLocked(userId, false);
        wallpaper = (WallpaperData) whichSet.get(userId);
        if (wallpaper != null) {
            return wallpaper;
        }
        if (which == 2) {
            wallpaper = new WallpaperData(userId, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
            this.mLockWallpaperMap.put(userId, wallpaper);
            ensureSaneWallpaperData(wallpaper);
            return wallpaper;
        }
        Slog.wtf(TAG, "Didn't find wallpaper in non-lock case!");
        wallpaper = new WallpaperData(userId, WALLPAPER, WALLPAPER_CROP);
        this.mWallpaperMap.put(userId, wallpaper);
        ensureSaneWallpaperData(wallpaper);
        return wallpaper;
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01ba  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x01d3  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01ea  */
    /* JADX WARNING: Removed duplicated region for block: B:77:? A:{SYNTHETIC, RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadSettingsLocked(int userId, boolean keepDimensionHints) {
        String str;
        StringBuilder stringBuilder;
        NullPointerException e;
        NumberFormatException e2;
        XmlPullParserException e3;
        IOException e4;
        IndexOutOfBoundsException e5;
        int i = userId;
        FileInputStream stream = null;
        File file = makeJournaledFile(userId).chooseForRead();
        WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.get(i);
        if (wallpaper == null) {
            migrateFromOld();
            wallpaper = new WallpaperData(i, WALLPAPER, WALLPAPER_CROP);
            wallpaper.allowBackup = true;
            this.mWallpaperMap.put(i, wallpaper);
            if (!wallpaper.cropExists()) {
                if (wallpaper.sourceExists()) {
                    generateCrop(wallpaper);
                } else {
                    Slog.i(TAG, "No static wallpaper imagery; defaults will be shown");
                }
            }
        }
        WallpaperData wallpaper2 = wallpaper;
        boolean success = false;
        boolean z;
        try {
            stream = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            int type;
            do {
                type = parser.next();
                if (type == 2) {
                    String tag = parser.getName();
                    if ("wp".equals(tag)) {
                        try {
                            parseWallpaperAttributes(parser, wallpaper2, keepDimensionHints);
                            ComponentName componentName = null;
                            String comp = parser.getAttributeValue(null, "component");
                            if (comp != null) {
                                componentName = ComponentName.unflattenFromString(comp);
                            }
                            wallpaper2.nextWallpaperComponent = componentName;
                            if (wallpaper2.nextWallpaperComponent == null || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(wallpaper2.nextWallpaperComponent.getPackageName())) {
                                wallpaper2.nextWallpaperComponent = this.mImageWallpaper;
                            }
                        } catch (FileNotFoundException e6) {
                            Slog.w(TAG, "no current wallpaper -- first boot?");
                            this.mDefaultWallpaperComponent = WallpaperManager.getDefaultWallpaperComponent(userId);
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("first boot set mDefaultWallpaperComponent=");
                            stringBuilder.append(this.mDefaultWallpaperComponent);
                            Slog.w(str, stringBuilder.toString());
                            IoUtils.closeQuietly(stream);
                            this.mSuccess = success;
                            if (success) {
                            }
                            ensureSaneWallpaperData(wallpaper2);
                            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
                            if (wallpaper != null) {
                            }
                        } catch (NullPointerException e7) {
                            e = e7;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("failed parsing ");
                            stringBuilder.append(file);
                            stringBuilder.append(" ");
                            stringBuilder.append(e);
                            Slog.w(str, stringBuilder.toString());
                            IoUtils.closeQuietly(stream);
                            this.mSuccess = success;
                            if (success) {
                            }
                            ensureSaneWallpaperData(wallpaper2);
                            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
                            if (wallpaper != null) {
                            }
                        } catch (NumberFormatException e8) {
                            e2 = e8;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("failed parsing ");
                            stringBuilder.append(file);
                            stringBuilder.append(" ");
                            stringBuilder.append(e2);
                            Slog.w(str, stringBuilder.toString());
                            IoUtils.closeQuietly(stream);
                            this.mSuccess = success;
                            if (success) {
                            }
                            ensureSaneWallpaperData(wallpaper2);
                            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
                            if (wallpaper != null) {
                            }
                        } catch (XmlPullParserException e9) {
                            e3 = e9;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("failed parsing ");
                            stringBuilder.append(file);
                            stringBuilder.append(" ");
                            stringBuilder.append(e3);
                            Slog.w(str, stringBuilder.toString());
                            IoUtils.closeQuietly(stream);
                            this.mSuccess = success;
                            if (success) {
                            }
                            ensureSaneWallpaperData(wallpaper2);
                            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
                            if (wallpaper != null) {
                            }
                        } catch (IOException e10) {
                            e4 = e10;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("failed parsing ");
                            stringBuilder.append(file);
                            stringBuilder.append(" ");
                            stringBuilder.append(e4);
                            Slog.w(str, stringBuilder.toString());
                            IoUtils.closeQuietly(stream);
                            this.mSuccess = success;
                            if (success) {
                            }
                            ensureSaneWallpaperData(wallpaper2);
                            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
                            if (wallpaper != null) {
                            }
                        } catch (IndexOutOfBoundsException e11) {
                            e5 = e11;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("failed parsing ");
                            stringBuilder.append(file);
                            stringBuilder.append(" ");
                            stringBuilder.append(e5);
                            Slog.w(str, stringBuilder.toString());
                            IoUtils.closeQuietly(stream);
                            this.mSuccess = success;
                            if (success) {
                            }
                            ensureSaneWallpaperData(wallpaper2);
                            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
                            if (wallpaper != null) {
                            }
                        }
                    } else {
                        z = keepDimensionHints;
                        if ("kwp".equals(tag)) {
                            WallpaperData lockWallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
                            if (lockWallpaper == null) {
                                lockWallpaper = new WallpaperData(i, WALLPAPER_LOCK_ORIG, WALLPAPER_LOCK_CROP);
                                this.mLockWallpaperMap.put(i, lockWallpaper);
                            }
                            parseWallpaperAttributes(parser, lockWallpaper, false);
                        }
                    }
                } else {
                    z = keepDimensionHints;
                }
            } while (type != 1);
            success = true;
        } catch (FileNotFoundException e12) {
            z = keepDimensionHints;
            Slog.w(TAG, "no current wallpaper -- first boot?");
            this.mDefaultWallpaperComponent = WallpaperManager.getDefaultWallpaperComponent(userId);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("first boot set mDefaultWallpaperComponent=");
            stringBuilder.append(this.mDefaultWallpaperComponent);
            Slog.w(str, stringBuilder.toString());
            IoUtils.closeQuietly(stream);
            this.mSuccess = success;
            if (success) {
            }
            ensureSaneWallpaperData(wallpaper2);
            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
            if (wallpaper != null) {
            }
        } catch (NullPointerException e13) {
            e = e13;
            z = keepDimensionHints;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
            IoUtils.closeQuietly(stream);
            this.mSuccess = success;
            if (success) {
            }
            ensureSaneWallpaperData(wallpaper2);
            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
            if (wallpaper != null) {
            }
        } catch (NumberFormatException e14) {
            e2 = e14;
            z = keepDimensionHints;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e2);
            Slog.w(str, stringBuilder.toString());
            IoUtils.closeQuietly(stream);
            this.mSuccess = success;
            if (success) {
            }
            ensureSaneWallpaperData(wallpaper2);
            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
            if (wallpaper != null) {
            }
        } catch (XmlPullParserException e15) {
            e3 = e15;
            z = keepDimensionHints;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e3);
            Slog.w(str, stringBuilder.toString());
            IoUtils.closeQuietly(stream);
            this.mSuccess = success;
            if (success) {
            }
            ensureSaneWallpaperData(wallpaper2);
            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
            if (wallpaper != null) {
            }
        } catch (IOException e16) {
            e4 = e16;
            z = keepDimensionHints;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e4);
            Slog.w(str, stringBuilder.toString());
            IoUtils.closeQuietly(stream);
            this.mSuccess = success;
            if (success) {
            }
            ensureSaneWallpaperData(wallpaper2);
            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
            if (wallpaper != null) {
            }
        } catch (IndexOutOfBoundsException e17) {
            e5 = e17;
            z = keepDimensionHints;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e5);
            Slog.w(str, stringBuilder.toString());
            IoUtils.closeQuietly(stream);
            this.mSuccess = success;
            if (success) {
            }
            ensureSaneWallpaperData(wallpaper2);
            wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
            if (wallpaper != null) {
            }
        }
        IoUtils.closeQuietly(stream);
        this.mSuccess = success;
        if (success) {
            wallpaper2.width = -1;
            wallpaper2.height = -1;
            wallpaper2.cropHint.set(0, 0, 0, 0);
            wallpaper2.padding.set(0, 0, 0, 0);
            wallpaper2.name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.mLockWallpaperMap.remove(i);
        } else if (wallpaper2.wallpaperId <= 0) {
            wallpaper2.wallpaperId = makeWallpaperIdLocked();
        }
        ensureSaneWallpaperData(wallpaper2);
        wallpaper = (WallpaperData) this.mLockWallpaperMap.get(i);
        if (wallpaper != null) {
            ensureSaneWallpaperData(wallpaper);
        }
    }

    private void ensureSaneWallpaperData(WallpaperData wallpaper) {
        int baseSize = getMaximumSizeDimension();
        if (wallpaper.width < baseSize && !getHwWallpaperWidth(wallpaper, this.mSuccess)) {
            wallpaper.width = baseSize;
        }
        if (wallpaper.height < baseSize) {
            wallpaper.height = baseSize;
        }
        if (wallpaper.cropHint.width() <= 0 || wallpaper.cropHint.height() <= 0) {
            wallpaper.cropHint.set(0, 0, wallpaper.width, wallpaper.height);
        }
    }

    private void parseWallpaperAttributes(XmlPullParser parser, WallpaperData wallpaper, boolean keepDimensionHints) {
        int id;
        String idString = parser.getAttributeValue(null, "id");
        if (idString != null) {
            id = Integer.parseInt(idString);
            wallpaper.wallpaperId = id;
            if (id > this.mWallpaperId) {
                this.mWallpaperId = id;
            }
        } else {
            wallpaper.wallpaperId = makeWallpaperIdLocked();
        }
        if (!keepDimensionHints) {
            wallpaper.width = Integer.parseInt(parser.getAttributeValue(null, "width"));
            wallpaper.height = Integer.parseInt(parser.getAttributeValue(null, "height"));
        }
        wallpaper.cropHint.left = getAttributeInt(parser, "cropLeft", 0);
        wallpaper.cropHint.top = getAttributeInt(parser, "cropTop", 0);
        wallpaper.cropHint.right = getAttributeInt(parser, "cropRight", 0);
        wallpaper.cropHint.bottom = getAttributeInt(parser, "cropBottom", 0);
        wallpaper.padding.left = getAttributeInt(parser, "paddingLeft", 0);
        wallpaper.padding.top = getAttributeInt(parser, "paddingTop", 0);
        wallpaper.padding.right = getAttributeInt(parser, "paddingRight", 0);
        wallpaper.padding.bottom = getAttributeInt(parser, "paddingBottom", 0);
        id = getAttributeInt(parser, "colorsCount", 0);
        if (id > 0) {
            Color tertiary = null;
            Color secondary = null;
            Color primary = null;
            for (int i = 0; i < id; i++) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("colorValue");
                stringBuilder.append(i);
                Color color = Color.valueOf(getAttributeInt(parser, stringBuilder.toString(), 0));
                if (i != 0) {
                    if (i != 1) {
                        if (i != 2) {
                            break;
                        }
                        tertiary = color;
                    } else {
                        secondary = color;
                    }
                } else {
                    primary = color;
                }
            }
            wallpaper.primaryColors = new WallpaperColors(primary, secondary, tertiary, getAttributeInt(parser, "colorHints", 0));
        }
        wallpaper.name = parser.getAttributeValue(null, Settings.ATTR_NAME);
        wallpaper.allowBackup = "true".equals(parser.getAttributeValue(null, HealthServiceWrapper.INSTANCE_HEALTHD));
    }

    private int getMaximumSizeDimension() {
        return ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getMaximumSizeDimension();
    }

    public void settingsRestored() {
        if (Binder.getCallingUid() == 1000) {
            WallpaperData wallpaper;
            boolean success;
            synchronized (this.mLock) {
                loadSettingsLocked(0, false);
                wallpaper = (WallpaperData) this.mWallpaperMap.get(0);
                wallpaper.wallpaperId = makeWallpaperIdLocked();
                wallpaper.allowBackup = true;
                if (wallpaper.nextWallpaperComponent == null || wallpaper.nextWallpaperComponent.equals(this.mImageWallpaper)) {
                    if (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(wallpaper.name)) {
                        success = true;
                    } else {
                        success = restoreNamedResourceLocked(wallpaper);
                    }
                    if (success) {
                        generateCrop(wallpaper);
                        bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, true, false, wallpaper, null);
                    }
                } else {
                    if (!bindWallpaperComponentLocked(wallpaper.nextWallpaperComponent, false, false, wallpaper, null)) {
                        bindWallpaperComponentLocked(null, false, false, wallpaper, null);
                    }
                    success = true;
                }
            }
            if (!success) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to restore wallpaper: '");
                stringBuilder.append(wallpaper.name);
                stringBuilder.append("'");
                Slog.e(str, stringBuilder.toString());
                wallpaper.name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                getWallpaperDir(0).delete();
            }
            synchronized (this.mLock) {
                saveSettingsLocked(0);
            }
            return;
        }
        throw new RuntimeException("settingsRestored() can only be called from the system process");
    }

    /* JADX WARNING: Missing block: B:51:0x0127, code skipped:
            if (r14 != null) goto L_0x0129;
     */
    /* JADX WARNING: Missing block: B:52:0x0129, code skipped:
            android.os.FileUtils.sync(r14);
     */
    /* JADX WARNING: Missing block: B:53:0x012c, code skipped:
            libcore.io.IoUtils.closeQuietly(null);
            libcore.io.IoUtils.closeQuietly(r14);
     */
    /* JADX WARNING: Missing block: B:60:0x0152, code skipped:
            if (r14 != null) goto L_0x0129;
     */
    /* JADX WARNING: Missing block: B:67:0x0179, code skipped:
            if (r14 != null) goto L_0x0129;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean restoreNamedResourceLocked(WallpaperData wallpaper) {
        String str;
        StringBuilder stringBuilder;
        WallpaperData wallpaperData = wallpaper;
        if (wallpaperData.name.length() > 4 && "res:".equals(wallpaperData.name.substring(0, 4))) {
            String resName = wallpaperData.name.substring(4);
            String pkg = null;
            int colon = resName.indexOf(58);
            if (colon > 0) {
                pkg = resName.substring(0, colon);
            }
            String pkg2 = pkg;
            pkg = null;
            int slash = resName.lastIndexOf(47);
            if (slash > 0) {
                pkg = resName.substring(slash + 1);
            }
            String ident = pkg;
            pkg = null;
            if (colon > 0 && slash > 0 && slash - colon > 1) {
                pkg = resName.substring(colon + 1, slash);
            }
            String type = pkg;
            if (!(pkg2 == null || ident == null || type == null)) {
                int resId = -1;
                FileOutputStream cos = null;
                try {
                    Context c = this.mContext.createPackageContext(pkg2, 4);
                    Resources r = c.getResources();
                    resId = r.getIdentifier(resName, null, null);
                    if (resId == 0) {
                        pkg = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("couldn't resolve identifier pkg=");
                        stringBuilder2.append(pkg2);
                        stringBuilder2.append(" type=");
                        stringBuilder2.append(type);
                        stringBuilder2.append(" ident=");
                        stringBuilder2.append(ident);
                        Slog.e(pkg, stringBuilder2.toString());
                        IoUtils.closeQuietly(null);
                        if (null != null) {
                            FileUtils.sync(null);
                        }
                        if (cos != null) {
                            FileUtils.sync(cos);
                        }
                        IoUtils.closeQuietly(null);
                        IoUtils.closeQuietly(cos);
                        return false;
                    }
                    InputStream res = r.openRawResource(resId);
                    if (wallpaperData.wallpaperFile.exists()) {
                        wallpaperData.wallpaperFile.delete();
                        wallpaperData.cropFile.delete();
                    }
                    FileOutputStream fos = new FileOutputStream(wallpaperData.wallpaperFile);
                    cos = new FileOutputStream(wallpaperData.cropFile);
                    byte[] buffer = new byte[32768];
                    while (true) {
                        int read = res.read(buffer);
                        int amt = read;
                        if (read > 0) {
                            fos.write(buffer, 0, amt);
                            cos.write(buffer, 0, amt);
                        } else {
                            String str2 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Restored wallpaper: ");
                            stringBuilder3.append(resName);
                            Slog.v(str2, stringBuilder3.toString());
                            IoUtils.closeQuietly(res);
                            FileUtils.sync(fos);
                            FileUtils.sync(cos);
                            IoUtils.closeQuietly(fos);
                            IoUtils.closeQuietly(cos);
                            return true;
                        }
                    }
                } catch (NameNotFoundException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Package name ");
                    stringBuilder.append(pkg2);
                    stringBuilder.append(" not found");
                    Slog.e(str, stringBuilder.toString());
                    IoUtils.closeQuietly(null);
                    if (null != null) {
                        FileUtils.sync(null);
                    }
                } catch (NotFoundException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Resource not found: ");
                    stringBuilder.append(resId);
                    Slog.e(str, stringBuilder.toString());
                    IoUtils.closeQuietly(null);
                    if (null != null) {
                        FileUtils.sync(null);
                    }
                } catch (IOException e3) {
                    Slog.e(TAG, "IOException while restoring wallpaper ", e3);
                    IoUtils.closeQuietly(null);
                    if (null != null) {
                        FileUtils.sync(null);
                    }
                } catch (Throwable th) {
                    IoUtils.closeQuietly(null);
                    if (null != null) {
                        FileUtils.sync(null);
                    }
                    if (cos != null) {
                        FileUtils.sync(cos);
                    }
                    IoUtils.closeQuietly(null);
                    IoUtils.closeQuietly(cos);
                }
            }
        }
        return false;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                pw.println("System wallpaper state:");
                int i = 0;
                for (int i2 = 0; i2 < this.mWallpaperMap.size(); i2++) {
                    WallpaperData wallpaper = (WallpaperData) this.mWallpaperMap.valueAt(i2);
                    pw.print(" User ");
                    pw.print(wallpaper.userId);
                    pw.print(": id=");
                    pw.println(wallpaper.wallpaperId);
                    pw.print("  mWidth=");
                    pw.print(wallpaper.width);
                    pw.print(" mHeight=");
                    pw.println(wallpaper.height);
                    pw.print("  mCropHint=");
                    pw.println(wallpaper.cropHint);
                    pw.print("  mPadding=");
                    pw.println(wallpaper.padding);
                    pw.print("  mName=");
                    pw.println(wallpaper.name);
                    pw.print("  mAllowBackup=");
                    pw.println(wallpaper.allowBackup);
                    pw.print("  mWallpaperComponent=");
                    pw.println(wallpaper.wallpaperComponent);
                    if (wallpaper.connection != null) {
                        WallpaperConnection conn = wallpaper.connection;
                        pw.print("  Wallpaper connection ");
                        pw.print(conn);
                        pw.println(":");
                        if (conn.mInfo != null) {
                            pw.print("    mInfo.component=");
                            pw.println(conn.mInfo.getComponent());
                        }
                        pw.print("    mToken=");
                        pw.println(conn.mToken);
                        pw.print("    mService=");
                        pw.println(conn.mService);
                        pw.print("    mEngine=");
                        pw.println(conn.mEngine);
                        pw.print("    mLastDiedTime=");
                        pw.println(wallpaper.lastDiedTime - SystemClock.uptimeMillis());
                    }
                }
                pw.println("Lock wallpaper state:");
                while (i < this.mLockWallpaperMap.size()) {
                    WallpaperData wallpaper2 = (WallpaperData) this.mLockWallpaperMap.valueAt(i);
                    pw.print(" User ");
                    pw.print(wallpaper2.userId);
                    pw.print(": id=");
                    pw.println(wallpaper2.wallpaperId);
                    pw.print("  mWidth=");
                    pw.print(wallpaper2.width);
                    pw.print(" mHeight=");
                    pw.println(wallpaper2.height);
                    pw.print("  mCropHint=");
                    pw.println(wallpaper2.cropHint);
                    pw.print("  mPadding=");
                    pw.println(wallpaper2.padding);
                    pw.print("  mName=");
                    pw.println(wallpaper2.name);
                    pw.print("  mAllowBackup=");
                    pw.println(wallpaper2.allowBackup);
                    i++;
                }
            }
        }
    }

    public void handleWallpaperObserverEvent(WallpaperData wallpaper) {
    }

    public ParcelFileDescriptor getBlurWallpaper(IWallpaperManagerCallback cb) {
        return null;
    }

    protected SparseArray<WallpaperData> getWallpaperMap() {
        return this.mWallpaperMap;
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected Object getLock() {
        return this.mLock;
    }

    protected int getCurrentUserId() {
        return this.mCurrentUserId;
    }

    protected boolean getHwWallpaperWidth(WallpaperData wallpaper, boolean success) {
        return false;
    }

    protected void updateWallpaperOffsets(WallpaperData wallpaper) {
    }

    public int[] getCurrOffsets() throws RemoteException {
        return null;
    }

    public void setCurrOffsets(int[] offsets) throws RemoteException {
    }

    public void setNextOffsets(int[] offsets) throws RemoteException {
    }

    public int getWallpaperUserId() {
        return this.mCurrentUserId;
    }

    public Bitmap scaleWallpaperBitmapToScreenSize(Bitmap bitmap) {
        return bitmap;
    }

    public boolean isMdmDisableChangeWallpaper() {
        if (!HwDeviceManager.disallowOp(35)) {
            return false;
        }
        Slog.v(TAG, "WallpaperManagerService:MDM policy forbidden setWallpaper");
        mHandler.postDelayed(new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(WallpaperManagerService.this.mContext, 33685955, 0);
                toast.getWindowParams().type = 2010;
                LayoutParams windowParams = toast.getWindowParams();
                windowParams.privateFlags |= 16;
                toast.show();
            }
        }, 300);
        return true;
    }

    public void reportWallpaper(ComponentName componentName) {
    }
}
