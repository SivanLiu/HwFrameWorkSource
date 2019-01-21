package com.android.server.display;

import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.Curve;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.DisplayManagerInternal.DisplayPowerCallbacks;
import android.hardware.display.DisplayManagerInternal.DisplayPowerRequest;
import android.hardware.display.DisplayManagerInternal.DisplayTransactionListener;
import android.hardware.display.DisplayViewport;
import android.hardware.display.IDisplayManager.Stub;
import android.hardware.display.IDisplayManagerCallback;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.WifiDisplayStatus;
import android.hardware.input.InputManagerInternal;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HwBrightnessProcessor;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.BacklightBrightness;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Spline;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl.Transaction;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.AnimationThread;
import com.android.server.DisplayThread;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.display.DisplayAdapter.Listener;
import com.android.server.wm.SurfaceAnimationThread;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.zrhung.IZRHungService;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.hardware.display.IHwDisplayManager;
import com.huawei.pgmng.log.LogPower;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DisplayManagerService extends SystemService implements IHwDisplayManagerInner {
    private static final boolean DEBUG = false;
    private static final int DEFAULT_MAX_BRIGHTNESS = 255;
    private static final String FORCE_WIFI_DISPLAY_ENABLE = "persist.debug.wfd.enable";
    private static final int HIGH_PRECISION_MAX_BRIGHTNESS = 10000;
    private static final boolean HWFLOW;
    private static final boolean IS_DEBUG_VERSION;
    private static final int MSG_DELIVER_DISPLAY_EVENT = 3;
    private static final int MSG_LOAD_BRIGHTNESS_CONFIGURATION = 7;
    private static final int MSG_REGISTER_ADDITIONAL_DISPLAY_ADAPTERS = 2;
    private static final int MSG_REGISTER_BRIGHTNESS_TRACKER = 6;
    private static final int MSG_REGISTER_DEFAULT_DISPLAY_ADAPTERS = 1;
    private static final int MSG_REQUEST_TRAVERSAL = 4;
    private static final int MSG_UPDATE_VIEWPORT = 5;
    private static final String TAG = "DisplayManagerService";
    private static final long WAIT_FOR_DEFAULT_DISPLAY_TIMEOUT = 10000;
    public final SparseArray<CallbackRecord> mCallbacks;
    private final Context mContext;
    private int mCurrentUserId;
    private final int mDefaultDisplayDefaultColorMode;
    private final DisplayViewport mDefaultViewport;
    private final SparseArray<IntArray> mDisplayAccessUIDs;
    private final DisplayAdapterListener mDisplayAdapterListener;
    private final ArrayList<DisplayAdapter> mDisplayAdapters;
    private final ArrayList<DisplayDevice> mDisplayDevices;
    private DisplayPowerController mDisplayPowerController;
    private final CopyOnWriteArrayList<DisplayTransactionListener> mDisplayTransactionListeners;
    private final DisplayViewport mExternalTouchViewport;
    private int mGlobalAlpmState;
    private int mGlobalDisplayBrightness;
    private int mGlobalDisplayState;
    private final DisplayManagerHandler mHandler;
    IHwDisplayManagerServiceEx mHwDMSEx;
    HwInnerDisplayManagerService mHwInnerService;
    private final Injector mInjector;
    private InputManagerInternal mInputManagerInternal;
    private boolean mIsHighPrecision;
    private LocalDisplayAdapter mLocalDisplayAdapter;
    private final SparseArray<LogicalDisplay> mLogicalDisplays;
    private final Curve mMinimumBrightnessCurve;
    private final Spline mMinimumBrightnessSpline;
    private int mNextNonDefaultDisplayId;
    public boolean mOnlyCore;
    private boolean mPendingTraversal;
    private final PersistentDataStore mPersistentDataStore;
    private PowerManagerInternal mPowerManagerInternal;
    private final ArrayMap<String, HwBrightnessProcessor> mPowerProcessors;
    private IMediaProjectionManager mProjectionService;
    public boolean mSafeMode;
    private final boolean mSingleDisplayDemoMode;
    private Point mStableDisplaySize;
    private final SyncRoot mSyncRoot;
    private final ArrayList<CallbackRecord> mTempCallbacks;
    private final DisplayViewport mTempDefaultViewport;
    private final DisplayInfo mTempDisplayInfo;
    private final ArrayList<Runnable> mTempDisplayStateWorkQueue;
    private final DisplayViewport mTempExternalTouchViewport;
    private final ArrayList<DisplayViewport> mTempVirtualTouchViewports;
    private int mTemporaryScreenBrightnessSettingOverride;
    private final Handler mUiHandler;
    private VirtualDisplayAdapter mVirtualDisplayAdapter;
    private final ArrayList<DisplayViewport> mVirtualTouchViewports;
    private WifiDisplayAdapter mWifiDisplayAdapter;
    private int mWifiDisplayScanRequestCount;
    private WindowManagerInternal mWindowManagerInternal;

    @VisibleForTesting
    final class BinderService extends Stub {
        BinderService() {
        }

        public DisplayInfo getDisplayInfo(int displayId) {
            int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                DisplayInfo access$2200 = DisplayManagerService.this.getDisplayInfoInternal(displayId, callingUid);
                return access$2200;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int[] getDisplayIds() {
            int callingUid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                int[] access$2300 = DisplayManagerService.this.getDisplayIdsInternal(callingUid);
                return access$2300;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public Point getStableDisplaySize() {
            long token = Binder.clearCallingIdentity();
            try {
                Point access$2400 = DisplayManagerService.this.getStableDisplaySizeInternal();
                return access$2400;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void registerCallback(IDisplayManagerCallback callback) {
            if (callback != null) {
                int callingPid = Binder.getCallingPid();
                long token = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.registerCallbackInternal(callback, callingPid);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } else {
                throw new IllegalArgumentException("listener must not be null");
            }
        }

        public void startWifiDisplayScan() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to start wifi display scans");
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.startWifiDisplayScanInternal(callingPid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void stopWifiDisplayScan() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to stop wifi display scans");
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.stopWifiDisplayScanInternal(callingPid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void connectWifiDisplay(String address) {
            if (address != null) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to connect to a wifi display");
                long token = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.connectWifiDisplayInternal(address);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } else {
                throw new IllegalArgumentException("address must not be null");
            }
        }

        public void disconnectWifiDisplay() {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.disconnectWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void renameWifiDisplay(String address, String alias) {
            if (address != null) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to rename to a wifi display");
                long token = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.renameWifiDisplayInternal(address, alias);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } else {
                throw new IllegalArgumentException("address must not be null");
            }
        }

        public void forgetWifiDisplay(String address) {
            if (address != null) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to forget to a wifi display");
                long token = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.forgetWifiDisplayInternal(address);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } else {
                throw new IllegalArgumentException("address must not be null");
            }
        }

        public void pauseWifiDisplay() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to pause a wifi display session");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.pauseWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void resumeWifiDisplay() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY", "Permission required to resume a wifi display session");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.resumeWifiDisplayInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public WifiDisplayStatus getWifiDisplayStatus() {
            long token = Binder.clearCallingIdentity();
            try {
                WifiDisplayStatus access$3400 = DisplayManagerService.this.getWifiDisplayStatusInternal();
                return access$3400;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void requestColorMode(int displayId, int colorMode) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_COLOR_MODE", "Permission required to change the display color mode");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.requestColorModeInternal(displayId, colorMode);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setSaturationLevel(float level) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_SATURATION", "Permission required to set display saturation level");
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setSaturationLevelInternal(level);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int createVirtualDisplay(IVirtualDisplayCallback callback, IMediaProjection projection, String packageName, String name, int width, int height, int densityDpi, Surface surface, int flags, String uniqueId) {
            Throwable th;
            IMediaProjection iMediaProjection = projection;
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            String str = packageName;
            int i;
            if (!validatePackageName(callingUid, str)) {
                i = callingUid;
                throw new SecurityException("packageName must match the calling uid");
            } else if (callback == null) {
                i = callingUid;
                throw new IllegalArgumentException("appToken must not be null");
            } else if (TextUtils.isEmpty(name)) {
                i = callingUid;
                throw new IllegalArgumentException("name must be non-null and non-empty");
            } else if (width <= 0 || height <= 0 || densityDpi <= 0) {
                i = callingUid;
                throw new IllegalArgumentException("width, height, and densityDpi must be greater than 0");
            } else if (surface == null || !surface.isSingleBuffered()) {
                int flags2;
                int flags3;
                if ((flags & 1) != 0) {
                    flags2 = flags | 16;
                    if ((flags2 & 32) != 0) {
                        throw new IllegalArgumentException("Public display must not be marked as SHOW_WHEN_LOCKED_INSECURE");
                    }
                }
                flags2 = flags;
                if ((flags2 & 8) != 0) {
                    flags2 &= -17;
                }
                int flags4 = flags2;
                if (iMediaProjection != null) {
                    try {
                        if (DisplayManagerService.this.getProjectionService().isValidMediaProjection(iMediaProjection)) {
                            flags3 = iMediaProjection.applyVirtualDisplayFlags(flags4);
                        } else {
                            throw new SecurityException("Invalid media projection");
                        }
                    } catch (RemoteException e) {
                        throw new SecurityException("unable to validate media projection or flags");
                    }
                }
                flags3 = flags4;
                if (callingUid != 1000 && (flags3 & 16) != 0 && !canProjectVideo(iMediaProjection)) {
                    throw new SecurityException("Requires CAPTURE_VIDEO_OUTPUT or CAPTURE_SECURE_VIDEO_OUTPUT permission, or an appropriate MediaProjection token in order to create a screen sharing virtual display.");
                } else if ((flags3 & 4) == 0 || canProjectSecureVideo(iMediaProjection)) {
                    HwActivityManager.reportScreenRecord(callingUid, callingPid, 1);
                    LogPower.push(204, String.valueOf(callingPid), String.valueOf(callingUid), String.valueOf(2));
                    long token = Binder.clearCallingIdentity();
                    long token2;
                    try {
                        IMediaProjection iMediaProjection2 = iMediaProjection;
                        token2 = token;
                        try {
                            flags2 = DisplayManagerService.this.createVirtualDisplayInternal(callback, iMediaProjection2, callingUid, str, name, width, height, densityDpi, surface, flags3, uniqueId);
                            Binder.restoreCallingIdentity(token2);
                            return flags2;
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(token2);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        int i2 = callingPid;
                        i = callingUid;
                        token2 = token;
                        Binder.restoreCallingIdentity(token2);
                        throw th;
                    }
                } else {
                    throw new SecurityException("Requires CAPTURE_SECURE_VIDEO_OUTPUT or an appropriate MediaProjection token to create a secure virtual display.");
                }
            } else {
                throw new IllegalArgumentException("Surface can't be single-buffered");
            }
        }

        public void resizeVirtualDisplay(IVirtualDisplayCallback callback, int width, int height, int densityDpi) {
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.resizeVirtualDisplayInternal(callback.asBinder(), width, height, densityDpi);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setVirtualDisplaySurface(IVirtualDisplayCallback callback, Surface surface) {
            if (surface == null || !surface.isSingleBuffered()) {
                long token = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.setVirtualDisplaySurfaceInternal(callback.asBinder(), surface);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } else {
                throw new IllegalArgumentException("Surface can't be single-buffered");
            }
        }

        public void releaseVirtualDisplay(IVirtualDisplayCallback callback) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            HwActivityManager.reportScreenRecord(callingUid, callingPid, 0);
            LogPower.push(205, String.valueOf(callingPid), String.valueOf(callingUid), String.valueOf(2));
            try {
                DisplayManagerService.this.releaseVirtualDisplayInternal(callback.asBinder());
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(DisplayManagerService.this.mContext, DisplayManagerService.TAG, pw)) {
                long token = Binder.clearCallingIdentity();
                try {
                    DisplayManagerService.this.dumpInternal(pw);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        public ParceledListSlice<BrightnessChangeEvent> getBrightnessEvents(String callingPackage) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.BRIGHTNESS_SLIDER_USAGE", "Permission to read brightness events.");
            int callingUid = Binder.getCallingUid();
            int mode = ((AppOpsManager) DisplayManagerService.this.mContext.getSystemService(AppOpsManager.class)).noteOp(43, callingUid, callingPackage);
            boolean hasUsageStats = false;
            if (mode == 3) {
                if (DisplayManagerService.this.mContext.checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") == 0) {
                    hasUsageStats = true;
                }
            } else if (mode == 0) {
                hasUsageStats = true;
            }
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                ParceledListSlice brightnessEvents;
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    brightnessEvents = DisplayManagerService.this.mDisplayPowerController.getBrightnessEvents(userId, hasUsageStats);
                }
                Binder.restoreCallingIdentity(token);
                return brightnessEvents;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_AMBIENT_LIGHT_STATS", "Permission required to to access ambient light stats.");
            int userId = UserHandle.getUserId(Binder.getCallingUid());
            long token = Binder.clearCallingIdentity();
            try {
                ParceledListSlice ambientBrightnessStats;
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    ambientBrightnessStats = DisplayManagerService.this.mDisplayPowerController.getAmbientBrightnessStats(userId);
                }
                Binder.restoreCallingIdentity(token);
                return ambientBrightnessStats;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setBrightnessConfigurationForUser(BrightnessConfiguration c, int userId, String packageName) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to change the display's brightness configuration");
            if (userId != UserHandle.getCallingUserId()) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "Permission required to change the display brightness configuration of another user");
            }
            if (!(packageName == null || validatePackageName(getCallingUid(), packageName))) {
                packageName = null;
            }
            long token = Binder.clearCallingIdentity();
            try {
                DisplayManagerService.this.setBrightnessConfigurationForUserInternal(c, userId, packageName);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public BrightnessConfiguration getBrightnessConfigurationForUser(int userId) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to read the display's brightness configuration");
            if (userId != UserHandle.getCallingUserId()) {
                DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "Permission required to read the display brightness configuration of another user");
            }
            long token = Binder.clearCallingIdentity();
            try {
                BrightnessConfiguration config;
                int userSerial = DisplayManagerService.this.getUserManager().getUserSerialNumber(userId);
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    config = DisplayManagerService.this.mPersistentDataStore.getBrightnessConfiguration(userSerial);
                    if (config == null) {
                        config = DisplayManagerService.this.mDisplayPowerController.getDefaultBrightnessConfiguration();
                    }
                }
                Binder.restoreCallingIdentity(token);
                return config;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public BrightnessConfiguration getDefaultBrightnessConfiguration() {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONFIGURE_DISPLAY_BRIGHTNESS", "Permission required to read the display's default brightness configuration");
            long token = Binder.clearCallingIdentity();
            try {
                BrightnessConfiguration defaultBrightnessConfiguration;
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    defaultBrightnessConfiguration = DisplayManagerService.this.mDisplayPowerController.getDefaultBrightnessConfiguration();
                }
                Binder.restoreCallingIdentity(token);
                return defaultBrightnessConfiguration;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setTemporaryBrightness(int brightness) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's brightness");
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayManagerService.this.mDisplayPowerController.setTemporaryBrightness(brightness);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setTemporaryAutoBrightnessAdjustment(float adjustment) {
            DisplayManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_BRIGHTNESS", "Permission required to set the display's auto brightness adjustment");
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (DisplayManagerService.this.mSyncRoot) {
                    DisplayManagerService.this.mDisplayPowerController.setTemporaryAutoBrightnessAdjustment(adjustment);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            Throwable th;
            long token = Binder.clearCallingIdentity();
            try {
                try {
                    new DisplayManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
                    Binder.restoreCallingIdentity(token);
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }

        public Curve getMinimumBrightnessCurve() {
            long token = Binder.clearCallingIdentity();
            try {
                Curve minimumBrightnessCurveInternal = DisplayManagerService.this.getMinimumBrightnessCurveInternal();
                return minimumBrightnessCurveInternal;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public IBinder getHwInnerService() {
            return DisplayManagerService.this.mHwInnerService;
        }

        void setBrightness(int brightness) {
            System.putIntForUser(DisplayManagerService.this.mContext.getContentResolver(), "screen_brightness", brightness, -2);
        }

        void resetBrightnessConfiguration() {
            DisplayManagerService.this.setBrightnessConfigurationForUserInternal(null, DisplayManagerService.this.mContext.getUserId(), DisplayManagerService.this.mContext.getPackageName());
        }

        private boolean validatePackageName(int uid, String packageName) {
            if (packageName != null) {
                String[] packageNames = DisplayManagerService.this.mContext.getPackageManager().getPackagesForUid(uid);
                if (packageNames != null) {
                    for (String n : packageNames) {
                        if (n.equals(packageName)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean canProjectVideo(IMediaProjection projection) {
            if (projection != null) {
                try {
                    if (projection.canProjectVideo()) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Slog.e(DisplayManagerService.TAG, "Unable to query projection service for permissions", e);
                }
            }
            if (DisplayManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_VIDEO_OUTPUT") == 0) {
                return true;
            }
            return canProjectSecureVideo(projection);
        }

        private boolean canProjectSecureVideo(IMediaProjection projection) {
            boolean z = true;
            if (projection != null) {
                try {
                    if (projection.canProjectSecureVideo()) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Slog.e(DisplayManagerService.TAG, "Unable to query projection service for permissions", e);
                }
            }
            if (DisplayManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_SECURE_VIDEO_OUTPUT") != 0) {
                z = false;
            }
            return z;
        }
    }

    private final class CallbackRecord implements DeathRecipient {
        private final IDisplayManagerCallback mCallback;
        public final int mPid;
        public boolean mWifiDisplayScanRequested;

        public CallbackRecord(int pid, IDisplayManagerCallback callback) {
            this.mPid = pid;
            this.mCallback = callback;
        }

        public void binderDied() {
            DisplayManagerService.this.onCallbackDied(this);
        }

        public void notifyDisplayEventAsync(int displayId, int event) {
            try {
                this.mCallback.onDisplayEvent(displayId, event);
            } catch (RemoteException ex) {
                String str = DisplayManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to notify process ");
                stringBuilder.append(this.mPid);
                stringBuilder.append(" that displays changed, assuming it died.");
                Slog.w(str, stringBuilder.toString(), ex);
                if (DisplayManagerService.IS_DEBUG_VERSION) {
                    ArrayMap<String, Object> params = new ArrayMap();
                    params.put("checkType", "DisplayEventLostScene");
                    params.put("context", DisplayManagerService.this.mContext);
                    params.put("looper", DisplayThread.get().getLooper());
                    params.put(IZRHungService.PARAM_PID, Integer.valueOf(this.mPid));
                    if (HwServiceFactory.getWinFreezeScreenMonitor() != null) {
                        HwServiceFactory.getWinFreezeScreenMonitor().checkFreezeScreen(params);
                    }
                }
                binderDied();
            }
        }
    }

    private final class DisplayManagerHandler extends Handler {
        public DisplayManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 7) {
                switch (i) {
                    case 1:
                        DisplayManagerService.this.registerDefaultDisplayAdapters();
                        return;
                    case 2:
                        DisplayManagerService.this.registerAdditionalDisplayAdapters();
                        return;
                    case 3:
                        DisplayManagerService.this.deliverDisplayEvent(msg.arg1, msg.arg2);
                        return;
                    case 4:
                        DisplayManagerService.this.mWindowManagerInternal.requestTraversalFromDisplayManager();
                        return;
                    case 5:
                        synchronized (DisplayManagerService.this.mSyncRoot) {
                            DisplayManagerService.this.mTempDefaultViewport.copyFrom(DisplayManagerService.this.mDefaultViewport);
                            DisplayManagerService.this.mTempExternalTouchViewport.copyFrom(DisplayManagerService.this.mExternalTouchViewport);
                            if (!DisplayManagerService.this.mTempVirtualTouchViewports.equals(DisplayManagerService.this.mVirtualTouchViewports)) {
                                DisplayManagerService.this.mTempVirtualTouchViewports.clear();
                                Iterator it = DisplayManagerService.this.mVirtualTouchViewports.iterator();
                                while (it.hasNext()) {
                                    DisplayManagerService.this.mTempVirtualTouchViewports.add(((DisplayViewport) it.next()).makeCopy());
                                }
                            }
                        }
                        DisplayManagerService.this.mInputManagerInternal.setDisplayViewports(DisplayManagerService.this.mTempDefaultViewport, DisplayManagerService.this.mTempExternalTouchViewport, DisplayManagerService.this.mTempVirtualTouchViewports);
                        return;
                    default:
                        return;
                }
            }
            DisplayManagerService.this.loadBrightnessConfiguration();
        }
    }

    public class HwInnerDisplayManagerService extends IHwDisplayManager.Stub {
        DisplayManagerService mDMS;

        HwInnerDisplayManagerService(DisplayManagerService dms) {
            this.mDMS = dms;
        }

        public void startWifiDisplayScan(int channelId) {
            DisplayManagerService.this.mHwDMSEx.startWifiDisplayScan(channelId);
        }

        public void connectWifiDisplay(String address, String verificaitonCode) {
            DisplayManagerService.this.mHwDMSEx.connectWifiDisplay(address, verificaitonCode);
        }

        public void checkVerificationResult(boolean isRight) {
            DisplayManagerService.this.mHwDMSEx.checkVerificationResult(isRight);
        }
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        VirtualDisplayAdapter getVirtualDisplayAdapter(SyncRoot syncRoot, Context context, Handler handler, Listener displayAdapterListener) {
            return new VirtualDisplayAdapter(syncRoot, context, handler, displayAdapterListener);
        }

        long getDefaultDisplayDelayTimeout() {
            return 10000;
        }
    }

    private final class LocalService extends DisplayManagerInternal {
        private LocalService() {
        }

        public void initPowerManagement(final DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager) {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.mDisplayPowerController = new DisplayPowerController(DisplayManagerService.this.mContext, callbacks, handler, sensorManager, new DisplayBlanker() {
                    public void requestDisplayState(int state, int brightness) {
                        if (state == 1) {
                            DisplayManagerService.this.requestGlobalDisplayStateInternal(state, brightness);
                        }
                        callbacks.onDisplayStateChange(state);
                        if (state != 1) {
                            DisplayManagerService.this.requestGlobalDisplayStateInternal(state, brightness);
                        }
                    }
                });
            }
            DisplayManagerService.this.mHandler.sendEmptyMessage(7);
        }

        public boolean requestPowerState(DisplayPowerRequest request, boolean waitForNegativeProximity) {
            boolean requestPowerState;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                requestPowerState = DisplayManagerService.this.mDisplayPowerController.requestPowerState(request, waitForNegativeProximity);
            }
            return requestPowerState;
        }

        public void pcDisplayChange(boolean connected) {
            DisplayManagerService.this.pcDisplayChangeService(connected);
        }

        public void forceDisplayState(int screenState, int screenBrightness) {
            DisplayManagerService.this.requestGlobalDisplayStateInternal(screenState, screenBrightness);
        }

        public void setBacklightBrightness(BacklightBrightness backlightBrightness) {
            DisplayManagerService.this.mDisplayPowerController.setBacklightBrightness(backlightBrightness);
        }

        public void setCameraModeBrightnessLineEnable(boolean cameraModeBrightnessLineEnable) {
            DisplayManagerService.this.mDisplayPowerController.setCameraModeBrightnessLineEnable(cameraModeBrightnessLineEnable);
        }

        public void updateAutoBrightnessAdjustFactor(float adjustFactor) {
            DisplayManagerService.this.mDisplayPowerController.updateAutoBrightnessAdjustFactor(adjustFactor);
        }

        public int getMaxBrightnessForSeekbar() {
            return DisplayManagerService.this.mDisplayPowerController.getMaxBrightnessForSeekbar();
        }

        public boolean getRebootAutoModeEnable() {
            return DisplayManagerService.this.mDisplayPowerController.getRebootAutoModeEnable();
        }

        public void setBrightnessAnimationTime(boolean animationEnabled, int millisecond) {
            DisplayManagerService.this.mDisplayPowerController.setBrightnessAnimationTime(animationEnabled, millisecond);
        }

        public int getCoverModeBrightnessFromLastScreenBrightness() {
            return DisplayManagerService.this.mDisplayPowerController.getCoverModeBrightnessFromLastScreenBrightness();
        }

        public void setMaxBrightnessFromThermal(int brightness) {
            DisplayManagerService.this.mDisplayPowerController.setMaxBrightnessFromThermal(brightness);
        }

        public void setPoweroffModeChangeAutoEnable(boolean enable) {
            DisplayManagerService.this.mDisplayPowerController.setPoweroffModeChangeAutoEnable(enable);
        }

        public void setKeyguardLockedStatus(boolean isLocked) {
            DisplayManagerService.this.mDisplayPowerController.setKeyguardLockedStatus(isLocked);
        }

        public void setAodAlpmState(int globalState) {
            DisplayManagerService.this.mGlobalAlpmState = globalState;
            DisplayManagerService.this.mDisplayPowerController.setAodAlpmState(globalState);
        }

        public int setScreenBrightnessMappingtoIndoorMax(int brightness) {
            return DisplayManagerService.this.mDisplayPowerController.setScreenBrightnessMappingtoIndoorMax(brightness);
        }

        public void setBrightnessNoLimit(int brightness, int time) {
            DisplayManagerService.this.mDisplayPowerController.setBrightnessNoLimit(brightness, time);
        }

        public void setModeToAutoNoClearOffsetEnable(boolean enable) {
            DisplayManagerService.this.mDisplayPowerController.setModeToAutoNoClearOffsetEnable(enable);
        }

        public void setTemporaryScreenBrightnessSettingOverride(int brightness) {
            if (DisplayManagerService.this.mTemporaryScreenBrightnessSettingOverride != brightness) {
                DisplayManagerService.this.mDisplayPowerController.setTemporaryBrightness(brightness);
                DisplayManagerService.this.mTemporaryScreenBrightnessSettingOverride = brightness;
            }
        }

        public IBinder getDisplayToken(int displayId) {
            LogicalDisplay logicalDisplay = (LogicalDisplay) DisplayManagerService.this.mLogicalDisplays.get(displayId);
            if (logicalDisplay == null) {
                return null;
            }
            return logicalDisplay.getPrimaryDisplayDeviceLocked().getDisplayTokenLocked();
        }

        public boolean isProximitySensorAvailable() {
            boolean isProximitySensorAvailable;
            synchronized (DisplayManagerService.this.mSyncRoot) {
                isProximitySensorAvailable = DisplayManagerService.this.mDisplayPowerController.isProximitySensorAvailable();
            }
            return isProximitySensorAvailable;
        }

        public DisplayInfo getDisplayInfo(int displayId) {
            return DisplayManagerService.this.getDisplayInfoInternal(displayId, Process.myUid());
        }

        public void registerDisplayTransactionListener(DisplayTransactionListener listener) {
            if (listener != null) {
                DisplayManagerService.this.registerDisplayTransactionListenerInternal(listener);
                return;
            }
            throw new IllegalArgumentException("listener must not be null");
        }

        public void unregisterDisplayTransactionListener(DisplayTransactionListener listener) {
            if (listener != null) {
                DisplayManagerService.this.unregisterDisplayTransactionListenerInternal(listener);
                return;
            }
            throw new IllegalArgumentException("listener must not be null");
        }

        public void setDisplayInfoOverrideFromWindowManager(int displayId, DisplayInfo info) {
            DisplayManagerService.this.setDisplayInfoOverrideFromWindowManagerInternal(displayId, info);
        }

        public void updateCutoutInfoForRog(int displayId) {
            DisplayManagerService.this.updateCutoutInfoForRogInternal(displayId);
        }

        public void getNonOverrideDisplayInfo(int displayId, DisplayInfo outInfo) {
            DisplayManagerService.this.getNonOverrideDisplayInfoInternal(displayId, outInfo);
        }

        public void performTraversal(Transaction t) {
            DisplayManagerService.this.performTraversalInternal(t);
        }

        public void setDisplayProperties(int displayId, boolean hasContent, float requestedRefreshRate, int requestedMode, boolean inTraversal) {
            DisplayManagerService.this.setDisplayPropertiesInternal(displayId, hasContent, requestedRefreshRate, requestedMode, inTraversal);
        }

        public void setDisplayOffsets(int displayId, int x, int y) {
            DisplayManagerService.this.setDisplayOffsetsInternal(displayId, x, y);
        }

        public void setDisplayAccessUIDs(SparseArray<IntArray> newDisplayAccessUIDs) {
            DisplayManagerService.this.setDisplayAccessUIDsInternal(newDisplayAccessUIDs);
        }

        public boolean isUidPresentOnDisplay(int uid, int displayId) {
            return DisplayManagerService.this.isUidPresentOnDisplayInternal(uid, displayId);
        }

        public void persistBrightnessTrackerState() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.mDisplayPowerController.persistBrightnessTrackerState();
            }
        }

        public void onOverlayChanged() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                for (int i = 0; i < DisplayManagerService.this.mDisplayDevices.size(); i++) {
                    ((DisplayDevice) DisplayManagerService.this.mDisplayDevices.get(i)).onOverlayChangedLocked();
                }
            }
        }

        public boolean hwBrightnessSetData(String name, Bundle data, int[] result) {
            boolean ret = DisplayManagerService.this.mDisplayPowerController.hwBrightnessSetData(name, data, result);
            if (ret) {
                return ret;
            }
            HwBrightnessProcessor processor = (HwBrightnessProcessor) DisplayManagerService.this.mPowerProcessors.get(name);
            if (processor != null) {
                return processor.setData(data, result);
            }
            return ret;
        }

        public boolean hwBrightnessGetData(String name, Bundle data, int[] result) {
            boolean ret = DisplayManagerService.this.mDisplayPowerController.hwBrightnessGetData(name, data, result);
            if (ret) {
                return ret;
            }
            HwBrightnessProcessor processor = (HwBrightnessProcessor) DisplayManagerService.this.mPowerProcessors.get(name);
            if (processor != null) {
                return processor.getData(data, result);
            }
            return ret;
        }
    }

    public static final class SyncRoot {
    }

    private final class DisplayAdapterListener implements Listener {
        private DisplayAdapterListener() {
        }

        public void onDisplayDeviceEvent(DisplayDevice device, int event) {
            switch (event) {
                case 1:
                    DisplayManagerService.this.handleDisplayDeviceAdded(device);
                    return;
                case 2:
                    DisplayManagerService.this.handleDisplayDeviceChanged(device);
                    return;
                case 3:
                    DisplayManagerService.this.handleDisplayDeviceRemoved(device);
                    return;
                default:
                    return;
            }
        }

        public void onTraversalRequested() {
            synchronized (DisplayManagerService.this.mSyncRoot) {
                DisplayManagerService.this.scheduleTraversalLocked(false);
            }
        }
    }

    static {
        boolean z = false;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z2;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3) {
            z = true;
        }
        IS_DEBUG_VERSION = z;
    }

    public DisplayManagerService(Context context) {
        this(context, new Injector());
        loadHwBrightnessProcessors();
    }

    @VisibleForTesting
    DisplayManagerService(Context context, Injector injector) {
        super(context);
        this.mSyncRoot = new SyncRoot();
        this.mCallbacks = new SparseArray();
        this.mDisplayAdapters = new ArrayList();
        this.mDisplayDevices = new ArrayList();
        this.mLogicalDisplays = new SparseArray();
        this.mNextNonDefaultDisplayId = 1;
        this.mDisplayTransactionListeners = new CopyOnWriteArrayList();
        this.mGlobalDisplayState = 2;
        this.mGlobalDisplayBrightness = -1;
        this.mStableDisplaySize = new Point();
        this.mDefaultViewport = new DisplayViewport();
        this.mExternalTouchViewport = new DisplayViewport();
        this.mVirtualTouchViewports = new ArrayList();
        this.mPersistentDataStore = new PersistentDataStore();
        this.mTempCallbacks = new ArrayList();
        this.mTempDisplayInfo = new DisplayInfo();
        this.mTempDefaultViewport = new DisplayViewport();
        this.mTempExternalTouchViewport = new DisplayViewport();
        this.mTempVirtualTouchViewports = new ArrayList();
        this.mTempDisplayStateWorkQueue = new ArrayList();
        this.mDisplayAccessUIDs = new SparseArray();
        this.mIsHighPrecision = false;
        this.mGlobalAlpmState = -1;
        this.mTemporaryScreenBrightnessSettingOverride = -1;
        this.mPowerProcessors = new ArrayMap();
        this.mHwDMSEx = null;
        this.mHwInnerService = new HwInnerDisplayManagerService(this);
        this.mInjector = injector;
        this.mContext = context;
        this.mHandler = new DisplayManagerHandler(DisplayThread.get().getLooper());
        this.mUiHandler = UiThread.getHandler();
        this.mDisplayAdapterListener = new DisplayAdapterListener();
        this.mSingleDisplayDemoMode = SystemProperties.getBoolean("persist.demo.singledisplay", false);
        Resources resources = this.mContext.getResources();
        this.mDefaultDisplayDefaultColorMode = this.mContext.getResources().getInteger(17694763);
        float[] lux = getFloatArray(resources.obtainTypedArray(17236017));
        float[] nits = getFloatArray(resources.obtainTypedArray(17236018));
        this.mMinimumBrightnessCurve = new Curve(lux, nits);
        this.mMinimumBrightnessSpline = Spline.createSpline(lux, nits);
        this.mIsHighPrecision = true;
        this.mCurrentUserId = 0;
        this.mHwDMSEx = HwServiceExFactory.getHwDisplayManagerServiceEx(this, context);
    }

    public void setupSchedulerPolicies() {
        Process.setThreadGroupAndCpuset(DisplayThread.get().getThreadId(), 5);
        Process.setThreadGroupAndCpuset(AnimationThread.get().getThreadId(), 5);
        Process.setThreadGroupAndCpuset(SurfaceAnimationThread.get().getThreadId(), 5);
    }

    public void onStart() {
        synchronized (this.mSyncRoot) {
            this.mPersistentDataStore.loadIfNeeded();
            loadStableDisplayValuesLocked();
        }
        this.mHandler.sendEmptyMessage(1);
        publishBinderService("display", new BinderService(), true);
        publishLocalService(DisplayManagerInternal.class, new LocalService());
        publishLocalService(DisplayTransformManager.class, new DisplayTransformManager());
    }

    public void onBootPhase(int phase) {
        if (phase == 100) {
            synchronized (this.mSyncRoot) {
                long timeout = SystemClock.uptimeMillis() + this.mInjector.getDefaultDisplayDelayTimeout();
                while (true) {
                    if (this.mLogicalDisplays.get(0) != null) {
                        if (this.mVirtualDisplayAdapter != null) {
                        }
                    }
                    long delay = timeout - SystemClock.uptimeMillis();
                    if (delay > 0) {
                        try {
                            this.mSyncRoot.wait(delay);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Timeout waiting for default display to be initialized. DefaultDisplay=");
                        stringBuilder.append(this.mLogicalDisplays.get(0));
                        stringBuilder.append(", mVirtualDisplayAdapter=");
                        stringBuilder.append(this.mVirtualDisplayAdapter);
                        throw new RuntimeException(stringBuilder.toString());
                    }
                }
            }
        }
    }

    public void onSwitchUser(int newUserId) {
        int userSerial = getUserManager().getUserSerialNumber(newUserId);
        synchronized (this.mSyncRoot) {
            if (this.mCurrentUserId != newUserId) {
                this.mCurrentUserId = newUserId;
                this.mDisplayPowerController.setBrightnessConfiguration(this.mPersistentDataStore.getBrightnessConfiguration(userSerial));
            }
            this.mDisplayPowerController.onSwitchUser(newUserId);
        }
    }

    public void windowManagerAndInputReady() {
        synchronized (this.mSyncRoot) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
            scheduleTraversalLocked(false);
        }
    }

    public void pcDisplayChangeService(boolean connected) {
        if (this.mLocalDisplayAdapter != null) {
            this.mLocalDisplayAdapter.pcDisplayChangeService(connected);
        }
    }

    public void systemReady(boolean safeMode, boolean onlyCore) {
        synchronized (this.mSyncRoot) {
            this.mSafeMode = safeMode;
            this.mOnlyCore = onlyCore;
        }
        if (this.mLocalDisplayAdapter != null) {
            this.mLocalDisplayAdapter.registerContentObserver(this.mContext, this.mHandler);
        }
        this.mHandler.sendEmptyMessage(2);
        this.mHandler.sendEmptyMessage(6);
        this.mPowerManagerInternal = (PowerManagerInternal) getLocalService(PowerManagerInternal.class);
    }

    @VisibleForTesting
    Handler getDisplayHandler() {
        return this.mHandler;
    }

    private void loadStableDisplayValuesLocked() {
        Point size = this.mPersistentDataStore.getStableDisplaySize();
        if (size.x <= 0 || size.y <= 0) {
            Resources res = this.mContext.getResources();
            int width = res.getInteger(17694869);
            int height = res.getInteger(17694868);
            if (width > 0 && height > 0) {
                setStableDisplaySizeLocked(width, height);
                return;
            }
            return;
        }
        this.mStableDisplaySize.set(size.x, size.y);
    }

    private Point getStableDisplaySizeInternal() {
        Point r = new Point();
        synchronized (this.mSyncRoot) {
            if (this.mStableDisplaySize.x > 0 && this.mStableDisplaySize.y > 0) {
                r.set(this.mStableDisplaySize.x, this.mStableDisplaySize.y);
            }
        }
        return r;
    }

    private void registerDisplayTransactionListenerInternal(DisplayTransactionListener listener) {
        this.mDisplayTransactionListeners.add(listener);
    }

    private void unregisterDisplayTransactionListenerInternal(DisplayTransactionListener listener) {
        this.mDisplayTransactionListeners.remove(listener);
    }

    private void setDisplayInfoOverrideFromWindowManagerInternal(int displayId, DisplayInfo info) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.get(displayId);
            if (display != null && display.setDisplayInfoOverrideFromWindowManagerLocked(info)) {
                sendDisplayEventLocked(displayId, 2);
                scheduleTraversalLocked(false);
            }
        }
    }

    private void updateCutoutInfoForRogInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            DisplayDevice device = (DisplayDevice) this.mDisplayDevices.get(displayId);
            device.updateDesityforRog();
            handleDisplayDeviceChanged(device);
        }
    }

    private void getNonOverrideDisplayInfoInternal(int displayId, DisplayInfo outInfo) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.get(displayId);
            if (display != null) {
                display.getNonOverrideDisplayInfoLocked(outInfo);
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0010, code skipped:
            r0 = r2.mDisplayTransactionListeners.iterator();
     */
    /* JADX WARNING: Missing block: B:11:0x001a, code skipped:
            if (r0.hasNext() == false) goto L_0x0026;
     */
    /* JADX WARNING: Missing block: B:12:0x001c, code skipped:
            ((android.hardware.display.DisplayManagerInternal.DisplayTransactionListener) r0.next()).onDisplayTransaction();
     */
    /* JADX WARNING: Missing block: B:13:0x0026, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    void performTraversalInternal(Transaction t) {
        synchronized (this.mSyncRoot) {
            if (this.mPendingTraversal) {
                this.mPendingTraversal = false;
                performTraversalLocked(t);
            }
        }
    }

    /* JADX WARNING: Missing block: B:39:0x00a4, code skipped:
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:42:0x00ab, code skipped:
            if (r2 >= r6.mTempDisplayStateWorkQueue.size()) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:43:0x00ad, code skipped:
            ((java.lang.Runnable) r6.mTempDisplayStateWorkQueue.get(r2)).run();
            r2 = r2 + 1;
     */
    /* JADX WARNING: Missing block: B:44:0x00bb, code skipped:
            android.os.Trace.traceEnd(131072);
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            r6.mTempDisplayStateWorkQueue.clear();
     */
    /* JADX WARNING: Missing block: B:48:0x00c5, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void requestGlobalDisplayStateInternal(int state, int brightness) {
        StringBuilder stringBuilder;
        IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
        if (iZrHung != null) {
            ZrHungData arg = new ZrHungData();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Power: state=");
            stringBuilder.append(state);
            stringBuilder.append(", brightness=");
            stringBuilder.append(brightness);
            arg.putString("addScreenOnInfo", stringBuilder.toString());
            iZrHung.addInfo(arg);
            if (state == 2 && brightness > 0) {
                iZrHung.stop(null);
            }
        }
        if (state == 0) {
            state = 2;
        }
        if (!this.mIsHighPrecision) {
            if (state == 1) {
                brightness = 0;
            } else if (brightness < 0) {
                brightness = -1;
            } else if (brightness > 255) {
                brightness = 255;
            }
        }
        synchronized (this.mTempDisplayStateWorkQueue) {
            try {
                synchronized (this.mSyncRoot) {
                    if (this.mGlobalAlpmState == 0) {
                        brightness = 0;
                        Slog.d(TAG, "mGlobalAlpmState == 0(in AOD mode), set brightbess = 0 ");
                    }
                    if (this.mGlobalDisplayState == state && this.mGlobalDisplayBrightness == brightness) {
                        this.mTempDisplayStateWorkQueue.clear();
                        return;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("requestGlobalDisplayState(");
                    stringBuilder.append(Display.stateToString(state));
                    stringBuilder.append(", brightness=");
                    stringBuilder.append(brightness);
                    stringBuilder.append(")");
                    Trace.traceBegin(131072, stringBuilder.toString());
                    this.mGlobalDisplayState = state;
                    this.mGlobalDisplayBrightness = brightness;
                    applyGlobalDisplayStateLocked(this.mTempDisplayStateWorkQueue);
                }
            } catch (Throwable th) {
                this.mTempDisplayStateWorkQueue.clear();
            }
        }
    }

    /* JADX WARNING: Missing block: B:10:0x001e, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private DisplayInfo getDisplayInfoInternal(int displayId, int callingUid) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.get(displayId);
            if (display != null) {
                DisplayInfo info = display.getDisplayInfoLocked();
                if (info.hasAccess(callingUid) || isUidPresentOnDisplayInternal(callingUid, displayId)) {
                }
            }
            return null;
        }
    }

    private int[] getDisplayIdsInternal(int callingUid) {
        int[] displayIds;
        synchronized (this.mSyncRoot) {
            int count = this.mLogicalDisplays.size();
            displayIds = new int[count];
            int n = 0;
            for (int i = 0; i < count; i++) {
                if (((LogicalDisplay) this.mLogicalDisplays.valueAt(i)).getDisplayInfoLocked().hasAccess(callingUid)) {
                    int n2 = n + 1;
                    displayIds[n] = this.mLogicalDisplays.keyAt(i);
                    n = n2;
                }
            }
            if (n != count) {
                displayIds = Arrays.copyOfRange(displayIds, 0, n);
            }
        }
        return displayIds;
    }

    private void registerCallbackInternal(IDisplayManagerCallback callback, int callingPid) {
        synchronized (this.mSyncRoot) {
            if (this.mCallbacks.get(callingPid) == null) {
                CallbackRecord record = new CallbackRecord(callingPid, callback);
                try {
                    callback.asBinder().linkToDeath(record, 0);
                    this.mCallbacks.put(callingPid, record);
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new SecurityException("The calling process has already registered an IDisplayManagerCallback.");
        }
    }

    private void onCallbackDied(CallbackRecord record) {
        synchronized (this.mSyncRoot) {
            this.mCallbacks.remove(record.mPid);
            stopWifiDisplayScanLocked(record);
        }
    }

    private void startWifiDisplayScanInternal(int callingPid) {
        synchronized (this.mSyncRoot) {
            CallbackRecord record = (CallbackRecord) this.mCallbacks.get(callingPid);
            if (record != null) {
                startWifiDisplayScanLocked(record);
            } else {
                throw new IllegalStateException("The calling process has not registered an IDisplayManagerCallback.");
            }
        }
    }

    private void startWifiDisplayScanLocked(CallbackRecord record) {
        String str;
        StringBuilder stringBuilder;
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startWifiDisplayScanLocked mWifiDisplayScanRequestCount=");
            stringBuilder.append(this.mWifiDisplayScanRequestCount);
            Slog.i(str, stringBuilder.toString());
        }
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startWifiDisplayScanLocked record.mWifiDisplayScanRequested=");
            stringBuilder.append(record.mWifiDisplayScanRequested);
            Slog.i(str, stringBuilder.toString());
        }
        if (!record.mWifiDisplayScanRequested) {
            record.mWifiDisplayScanRequested = true;
            int i = this.mWifiDisplayScanRequestCount;
            this.mWifiDisplayScanRequestCount = i + 1;
            if (i == 0 && this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestStartScanLocked();
            }
        }
    }

    private void stopWifiDisplayScanInternal(int callingPid) {
        synchronized (this.mSyncRoot) {
            CallbackRecord record = (CallbackRecord) this.mCallbacks.get(callingPid);
            if (record != null) {
                stopWifiDisplayScanLocked(record);
            } else {
                throw new IllegalStateException("The calling process has not registered an IDisplayManagerCallback.");
            }
        }
    }

    private void stopWifiDisplayScanLocked(CallbackRecord record) {
        String str;
        StringBuilder stringBuilder;
        if (record.mWifiDisplayScanRequested) {
            record.mWifiDisplayScanRequested = false;
            int i = this.mWifiDisplayScanRequestCount - 1;
            this.mWifiDisplayScanRequestCount = i;
            if (i == 0) {
                if (this.mWifiDisplayAdapter != null) {
                    this.mWifiDisplayAdapter.requestStopScanLocked();
                }
            } else if (this.mWifiDisplayScanRequestCount < 0) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mWifiDisplayScanRequestCount became negative: ");
                stringBuilder2.append(this.mWifiDisplayScanRequestCount);
                Slog.wtf(str2, stringBuilder2.toString());
                this.mWifiDisplayScanRequestCount = 0;
            }
        }
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("stopWifiDisplayScanLocked record.mWifiDisplayScanRequested=");
            stringBuilder.append(record.mWifiDisplayScanRequested);
            Slog.i(str, stringBuilder.toString());
        }
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("stopWifiDisplayScanLocked mWifiDisplayScanRequestCount=");
            stringBuilder.append(this.mWifiDisplayScanRequestCount);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private void connectWifiDisplayInternal(String address) {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestConnectLocked(address);
            }
        }
    }

    private void pauseWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestPauseLocked();
            }
        }
    }

    private void resumeWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestResumeLocked();
            }
        }
    }

    private void disconnectWifiDisplayInternal() {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestDisconnectLocked();
            }
        }
    }

    private void renameWifiDisplayInternal(String address, String alias) {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestRenameLocked(address, alias);
            }
        }
    }

    private void forgetWifiDisplayInternal(String address) {
        synchronized (this.mSyncRoot) {
            if (this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestForgetLocked(address);
            }
        }
    }

    private WifiDisplayStatus getWifiDisplayStatusInternal() {
        synchronized (this.mSyncRoot) {
            WifiDisplayStatus wifiDisplayStatusLocked;
            if (this.mWifiDisplayAdapter != null) {
                wifiDisplayStatusLocked = this.mWifiDisplayAdapter.getWifiDisplayStatusLocked();
                return wifiDisplayStatusLocked;
            }
            wifiDisplayStatusLocked = new WifiDisplayStatus();
            return wifiDisplayStatusLocked;
        }
    }

    private void requestColorModeInternal(int displayId, int colorMode) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.get(displayId);
            if (!(display == null || display.getRequestedColorModeLocked() == colorMode)) {
                display.setRequestedColorModeLocked(colorMode);
                scheduleTraversalLocked(false);
            }
        }
    }

    private void setSaturationLevelInternal(float level) {
        if (level < 0.0f || level > 1.0f) {
            throw new IllegalArgumentException("Saturation level must be between 0 and 1");
        }
        ((DisplayTransformManager) LocalServices.getService(DisplayTransformManager.class)).setColorMatrix(150, level == 1.0f ? null : computeSaturationMatrix(level));
    }

    private static float[] computeSaturationMatrix(float saturation) {
        float desaturation = 1.0f - saturation;
        float[] luminance = new float[]{0.231f * desaturation, 0.715f * desaturation, 0.072f * desaturation};
        return new float[]{luminance[0] + saturation, luminance[0], luminance[0], 0.0f, luminance[1], luminance[1] + saturation, luminance[1], 0.0f, luminance[2], luminance[2], luminance[2] + saturation, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    }

    private int createVirtualDisplayInternal(IVirtualDisplayCallback callback, IMediaProjection projection, int callingUid, String packageName, String name, int width, int height, int densityDpi, Surface surface, int flags, String uniqueId) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                Slog.w(TAG, "Rejecting request to create private virtual display because the virtual display adapter is not available.");
                return -1;
            }
            DisplayDevice device = this.mVirtualDisplayAdapter.createVirtualDisplayLocked(callback, projection, callingUid, packageName, name, width, height, densityDpi, surface, flags, uniqueId);
            if (device == null) {
                return -1;
            }
            handleDisplayDeviceAddedLocked(device);
            LogicalDisplay display = findLogicalDisplayForDeviceLocked(device);
            if (display != null) {
                int displayIdLocked = display.getDisplayIdLocked();
                return displayIdLocked;
            }
            Slog.w(TAG, "Rejecting request to create virtual display because the logical display was not created.");
            this.mVirtualDisplayAdapter.releaseVirtualDisplayLocked(callback.asBinder());
            handleDisplayDeviceRemovedLocked(device);
            return -1;
        }
    }

    private void resizeVirtualDisplayInternal(IBinder appToken, int width, int height, int densityDpi) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                return;
            }
            this.mVirtualDisplayAdapter.resizeVirtualDisplayLocked(appToken, width, height, densityDpi);
        }
    }

    private void setVirtualDisplaySurfaceInternal(IBinder appToken, Surface surface) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                return;
            }
            this.mVirtualDisplayAdapter.setVirtualDisplaySurfaceLocked(appToken, surface);
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0015, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void releaseVirtualDisplayInternal(IBinder appToken) {
        synchronized (this.mSyncRoot) {
            if (this.mVirtualDisplayAdapter == null) {
                return;
            }
            DisplayDevice device = this.mVirtualDisplayAdapter.releaseVirtualDisplayLocked(appToken);
            if (device != null) {
                handleDisplayDeviceRemovedLocked(device);
            }
        }
    }

    private void registerDefaultDisplayAdapters() {
        synchronized (this.mSyncRoot) {
            this.mLocalDisplayAdapter = new LocalDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener);
            registerDisplayAdapterLocked(this.mLocalDisplayAdapter);
            this.mVirtualDisplayAdapter = this.mInjector.getVirtualDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener);
            if (this.mVirtualDisplayAdapter != null) {
                registerDisplayAdapterLocked(this.mVirtualDisplayAdapter);
            }
        }
    }

    private void registerAdditionalDisplayAdapters() {
        synchronized (this.mSyncRoot) {
            if (shouldRegisterNonEssentialDisplayAdaptersLocked()) {
                registerOverlayDisplayAdapterLocked();
                registerWifiDisplayAdapterLocked();
            }
        }
    }

    private void registerOverlayDisplayAdapterLocked() {
        registerDisplayAdapterLocked(new OverlayDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener, this.mUiHandler));
    }

    private void registerWifiDisplayAdapterLocked() {
        if (this.mContext.getResources().getBoolean(17956969) || SystemProperties.getInt(FORCE_WIFI_DISPLAY_ENABLE, -1) == 1) {
            this.mWifiDisplayAdapter = new WifiDisplayAdapter(this.mSyncRoot, this.mContext, this.mHandler, this.mDisplayAdapterListener, this.mPersistentDataStore);
            registerDisplayAdapterLocked(this.mWifiDisplayAdapter);
        }
    }

    private boolean shouldRegisterNonEssentialDisplayAdaptersLocked() {
        return (this.mSafeMode || this.mOnlyCore) ? false : true;
    }

    private void registerDisplayAdapterLocked(DisplayAdapter adapter) {
        this.mDisplayAdapters.add(adapter);
        adapter.registerLocked();
    }

    private void handleDisplayDeviceAdded(DisplayDevice device) {
        synchronized (this.mSyncRoot) {
            handleDisplayDeviceAddedLocked(device);
        }
    }

    private void handleDisplayDeviceAddedLocked(DisplayDevice device) {
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        String str;
        StringBuilder stringBuilder;
        if (this.mDisplayDevices.contains(device)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Attempted to add already added display device: ");
            stringBuilder.append(info);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        if ("com.hpplay.happycast".equals(info.ownerPackageName)) {
            info.densityDpi = 240;
            info.xDpi = 240.0f;
            info.yDpi = 240.0f;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Display device added: ");
        stringBuilder.append(info);
        Slog.i(str, stringBuilder.toString());
        device.mDebugLastLoggedDeviceInfo = info;
        this.mDisplayDevices.add(device);
        LogicalDisplay display = addLogicalDisplayLocked(device);
        Runnable work = updateDisplayStateLocked(device);
        if (work != null) {
            work.run();
        }
        DisplayInfo displayInfo = display.getDisplayInfoLocked();
        int displayID = display.getDisplayIdLocked();
        if (HwVRUtils.isVRDisplay(displayID, displayInfo.getNaturalWidth(), displayInfo.getNaturalHeight())) {
            Slog.i(TAG, "handleDisplayDeviceAddedLocked in vr mode");
            HwVRUtils.setVRDisplayID(displayID, true);
            HwFrameworkFactory.getVRSystemServiceManager().setVRDisplayConnected(true);
        }
        scheduleTraversalLocked(false);
    }

    /* JADX WARNING: Missing block: B:25:0x0097, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleDisplayDeviceChanged(DisplayDevice device) {
        synchronized (this.mSyncRoot) {
            DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
            if (this.mDisplayDevices.contains(device)) {
                int diff = device.mDebugLastLoggedDeviceInfo.diff(info);
                String str;
                StringBuilder stringBuilder;
                if (diff == 1) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Display device changed state: \"");
                    stringBuilder.append(info.name);
                    stringBuilder.append("\", ");
                    stringBuilder.append(Display.stateToString(info.state));
                    Slog.i(str, stringBuilder.toString());
                } else if (diff != 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Display device changed: ");
                    stringBuilder.append(info);
                    Slog.i(str, stringBuilder.toString());
                }
                if ((diff & 4) != 0) {
                    try {
                        this.mPersistentDataStore.setColorMode(device, info.colorMode);
                    } finally {
                        this.mPersistentDataStore.saveIfNeeded();
                    }
                }
                device.mDebugLastLoggedDeviceInfo = info;
                device.applyPendingDisplayDeviceInfoChangesLocked();
                if (updateLogicalDisplaysLocked()) {
                    scheduleTraversalLocked(false);
                }
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Attempted to change non-existent display device: ");
                stringBuilder2.append(info);
                Slog.w(str2, stringBuilder2.toString());
            }
        }
    }

    private void handleDisplayDeviceRemoved(DisplayDevice device) {
        synchronized (this.mSyncRoot) {
            handleDisplayDeviceRemovedLocked(device);
        }
    }

    private void handleDisplayDeviceRemovedLocked(DisplayDevice device) {
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        String str;
        StringBuilder stringBuilder;
        if (this.mDisplayDevices.remove(device)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Display device removed: ");
            stringBuilder.append(info);
            Slog.i(str, stringBuilder.toString());
            device.mDebugLastLoggedDeviceInfo = info;
            updateLogicalDisplaysLocked();
            scheduleTraversalLocked(false);
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Attempted to remove non-existent display device: ");
        stringBuilder.append(info);
        Slog.w(str, stringBuilder.toString());
    }

    private void applyGlobalDisplayStateLocked(List<Runnable> workQueue) {
        int count = this.mDisplayDevices.size();
        for (int i = 0; i < count; i++) {
            DisplayDevice device = (DisplayDevice) this.mDisplayDevices.get(i);
            if (!HwPCUtils.isPcCastModeInServer() || device.getDisplayDeviceInfoLocked().type != 2 || this.mPowerManagerInternal.shouldUpdatePCScreenState()) {
                Runnable runnable = updateDisplayStateLocked(device);
                if (runnable != null) {
                    workQueue.add(runnable);
                }
            }
        }
    }

    private Runnable updateDisplayStateLocked(DisplayDevice device) {
        if ((device.getDisplayDeviceInfoLocked().flags & 32) == 0) {
            return device.requestDisplayStateLocked(this.mGlobalDisplayState, this.mGlobalDisplayBrightness);
        }
        return null;
    }

    private LogicalDisplay addLogicalDisplayLocked(DisplayDevice device) {
        StringBuilder stringBuilder;
        DisplayDeviceInfo deviceInfo = device.getDisplayDeviceInfoLocked();
        boolean isDefault = (deviceInfo.flags & 1) != 0;
        if (isDefault && this.mLogicalDisplays.get(0) != null) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring attempt to add a second default display: ");
            stringBuilder.append(deviceInfo);
            Slog.w(str, stringBuilder.toString());
            isDefault = false;
        }
        String str2;
        if (isDefault || !this.mSingleDisplayDemoMode) {
            int displayId = assignDisplayIdLocked(isDefault);
            LogicalDisplay display = new LogicalDisplay(displayId, assignLayerStackLocked(displayId), device);
            display.updateLocked(this.mDisplayDevices);
            if (display.isValidLocked()) {
                configureColorModeLocked(display, device);
                if (isDefault) {
                    recordStableDisplayStatsIfNeededLocked(display);
                }
                this.mLogicalDisplays.put(displayId, display);
                if (isDefault) {
                    this.mSyncRoot.notifyAll();
                }
                sendDisplayEventLocked(displayId, 1);
                return display;
            }
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Ignoring display device because the logical display created from it was not considered valid: ");
            stringBuilder2.append(deviceInfo);
            Slog.w(str2, stringBuilder2.toString());
            return null;
        }
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Not creating a logical display for a secondary display  because single display demo mode is enabled: ");
        stringBuilder.append(deviceInfo);
        Slog.i(str2, stringBuilder.toString());
        return null;
    }

    private int assignDisplayIdLocked(boolean isDefault) {
        if (isDefault) {
            return 0;
        }
        int i = this.mNextNonDefaultDisplayId;
        this.mNextNonDefaultDisplayId = i + 1;
        return i;
    }

    private int assignLayerStackLocked(int displayId) {
        return displayId;
    }

    private void configureColorModeLocked(LogicalDisplay display, DisplayDevice device) {
        if (display.getPrimaryDisplayDeviceLocked() == device) {
            int colorMode = this.mPersistentDataStore.getColorMode(device);
            if (colorMode == -1) {
                if ((device.getDisplayDeviceInfoLocked().flags & 1) != 0) {
                    colorMode = this.mDefaultDisplayDefaultColorMode;
                } else {
                    colorMode = 0;
                }
            }
            display.setRequestedColorModeLocked(colorMode);
        }
    }

    private void recordStableDisplayStatsIfNeededLocked(LogicalDisplay d) {
        if (this.mStableDisplaySize.x <= 0 && this.mStableDisplaySize.y <= 0) {
            DisplayInfo info = d.getDisplayInfoLocked();
            setStableDisplaySizeLocked(info.getNaturalWidth(), info.getNaturalHeight());
        }
    }

    private void setStableDisplaySizeLocked(int width, int height) {
        this.mStableDisplaySize = new Point(width, height);
        try {
            this.mPersistentDataStore.setStableDisplaySize(this.mStableDisplaySize);
        } finally {
            this.mPersistentDataStore.saveIfNeeded();
        }
    }

    @VisibleForTesting
    Curve getMinimumBrightnessCurveInternal() {
        return this.mMinimumBrightnessCurve;
    }

    private void setBrightnessConfigurationForUserInternal(BrightnessConfiguration c, int userId, String packageName) {
        validateBrightnessConfiguration(c);
        int userSerial = getUserManager().getUserSerialNumber(userId);
        synchronized (this.mSyncRoot) {
            try {
                this.mPersistentDataStore.setBrightnessConfigurationForUser(c, userSerial, packageName);
                this.mPersistentDataStore.saveIfNeeded();
                if (userId == this.mCurrentUserId) {
                    this.mDisplayPowerController.setBrightnessConfiguration(c);
                }
            } catch (Throwable th) {
                this.mPersistentDataStore.saveIfNeeded();
            }
        }
    }

    @VisibleForTesting
    void validateBrightnessConfiguration(BrightnessConfiguration config) {
        if (config != null && isBrightnessConfigurationTooDark(config)) {
            throw new IllegalArgumentException("brightness curve is too dark");
        }
    }

    private boolean isBrightnessConfigurationTooDark(BrightnessConfiguration config) {
        Pair<float[], float[]> curve = config.getCurve();
        float[] lux = curve.first;
        float[] nits = curve.second;
        for (int i = 0; i < lux.length; i++) {
            if (nits[i] < this.mMinimumBrightnessSpline.interpolate(lux[i])) {
                return true;
            }
        }
        return false;
    }

    private void loadBrightnessConfiguration() {
        synchronized (this.mSyncRoot) {
            this.mDisplayPowerController.setBrightnessConfiguration(this.mPersistentDataStore.getBrightnessConfiguration(getUserManager().getUserSerialNumber(this.mCurrentUserId)));
        }
    }

    private boolean updateLogicalDisplaysLocked() {
        boolean changed = false;
        int i = this.mLogicalDisplays.size();
        while (true) {
            int i2 = i - 1;
            if (i <= 0) {
                return changed;
            }
            i = this.mLogicalDisplays.keyAt(i2);
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.valueAt(i2);
            this.mTempDisplayInfo.copyFrom(display.getDisplayInfoLocked());
            display.updateLocked(this.mDisplayDevices);
            if (!display.isValidLocked()) {
                this.mLogicalDisplays.removeAt(i2);
                if (HwVRUtils.isValidVRDisplayId(i)) {
                    Slog.i(TAG, "disconnect vr display");
                    HwFrameworkFactory.getVRSystemServiceManager().setVRDisplayConnected(false);
                }
                sendDisplayEventLocked(i, 3);
                changed = true;
            } else if (!this.mTempDisplayInfo.equals(display.getDisplayInfoLocked())) {
                sendDisplayEventLocked(i, 2);
                changed = true;
            }
            i = i2;
        }
    }

    private void performTraversalLocked(Transaction t) {
        clearViewportsLocked();
        int count = this.mDisplayDevices.size();
        for (int i = 0; i < count; i++) {
            DisplayDevice device = (DisplayDevice) this.mDisplayDevices.get(i);
            configureDisplayLocked(t, device);
            device.performTraversalLocked(t);
        }
        if (this.mInputManagerInternal != null) {
            this.mHandler.sendEmptyMessage(5);
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0038, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setDisplayPropertiesInternal(int displayId, boolean hasContent, float requestedRefreshRate, int requestedModeId, boolean inTraversal) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.get(displayId);
            if (display == null) {
                return;
            }
            if (display.hasContentLocked() != hasContent) {
                display.setHasContentLocked(hasContent);
                scheduleTraversalLocked(inTraversal);
            }
            if (requestedModeId == 0 && requestedRefreshRate != 0.0f) {
                requestedModeId = display.getDisplayInfoLocked().findDefaultModeByRefreshRate(requestedRefreshRate);
            }
            if (display.getRequestedModeIdLocked() != requestedModeId) {
                display.setRequestedModeIdLocked(requestedModeId);
                scheduleTraversalLocked(inTraversal);
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0023, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setDisplayOffsetsInternal(int displayId, int x, int y) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.get(displayId);
            if (display == null) {
            } else if (!(display.getDisplayOffsetXLocked() == x && display.getDisplayOffsetYLocked() == y)) {
                display.setDisplayOffsetsLocked(x, y);
                scheduleTraversalLocked(false);
            }
        }
    }

    private void setDisplayAccessUIDsInternal(SparseArray<IntArray> newDisplayAccessUIDs) {
        synchronized (this.mSyncRoot) {
            this.mDisplayAccessUIDs.clear();
            for (int i = newDisplayAccessUIDs.size() - 1; i >= 0; i--) {
                this.mDisplayAccessUIDs.append(newDisplayAccessUIDs.keyAt(i), (IntArray) newDisplayAccessUIDs.valueAt(i));
            }
        }
    }

    private boolean isUidPresentOnDisplayInternal(int uid, int displayId) {
        boolean z;
        synchronized (this.mSyncRoot) {
            IntArray displayUIDs = (IntArray) this.mDisplayAccessUIDs.get(displayId);
            z = (displayUIDs == null || displayUIDs.indexOf(uid) == -1) ? false : true;
        }
        return z;
    }

    private void clearViewportsLocked() {
        this.mDefaultViewport.valid = false;
        this.mExternalTouchViewport.valid = false;
        this.mVirtualTouchViewports.clear();
    }

    private void configureDisplayLocked(Transaction t, DisplayDevice device) {
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        boolean z = false;
        boolean ownContent = (info.flags & 128) != 0;
        LogicalDisplay display = findLogicalDisplayForDeviceLocked(device);
        if (!ownContent) {
            if (!(display == null || display.hasContentLocked())) {
                int displayID = display.getDisplayIdLocked();
                boolean isPCID = HwPCUtils.isValidExtDisplayId(displayID) && HwPCUtils.isPcCastModeInServerEarly();
                boolean isVRID = HwVRUtils.isValidVRDisplayId(displayID);
                if (!(isPCID || isVRID)) {
                    Slog.i(TAG, "do not mirror the default display content in pc or vr mode");
                    display = null;
                }
            }
            if (display == null && HwVRUtils.isVRMode() && HwFrameworkFactory.getVRSystemServiceManager().isVRDisplayConnected()) {
                display = (LogicalDisplay) this.mLogicalDisplays.get(HwVRUtils.getVRDisplayID());
            } else if (display == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Mirror the default display, device = ");
                stringBuilder.append(device);
                Slog.i(str, stringBuilder.toString());
                display = (LogicalDisplay) this.mLogicalDisplays.get(0);
            }
            if ("HW-VR-Virtual-Screen".equals(info.name)) {
                display = (LogicalDisplay) this.mLogicalDisplays.get(0);
            }
        }
        if (display == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Missing logical display to use for physical display device: ");
            stringBuilder2.append(device.getDisplayDeviceInfoLocked());
            Slog.w(str2, stringBuilder2.toString());
            return;
        }
        if (info.state == 1) {
            z = true;
        }
        display.configureDisplayLocked(t, device, z);
        if (!(this.mDefaultViewport.valid || (info.flags & 1) == 0)) {
            setViewportLocked(this.mDefaultViewport, display, device);
        }
        setExternalTouchViewport(device, info, display);
        if (!(this.mExternalTouchViewport.valid || info.touch != 2 || HwPCUtils.isPcCastModeInServer())) {
            setViewportLocked(this.mExternalTouchViewport, display, device);
        }
        if (info.touch == 3 && !TextUtils.isEmpty(info.uniqueId)) {
            setViewportLocked(getVirtualTouchViewportLocked(info.uniqueId), display, device);
        }
    }

    private void setExternalTouchViewport(DisplayDevice device, DisplayDeviceInfo info, LogicalDisplay display) {
        if (HwPCUtils.isPcCastModeInServer() && display != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setExternalTouchViewport display:");
            stringBuilder.append(display.getDisplayIdLocked());
            stringBuilder.append(", valid:");
            stringBuilder.append(this.mExternalTouchViewport.valid);
            stringBuilder.append(", flags:");
            stringBuilder.append(info.flags);
            stringBuilder.append(", isValidId:");
            stringBuilder.append(HwPCUtils.isValidExtDisplayId(display.getDisplayIdLocked()));
            HwPCUtils.log(str, stringBuilder.toString());
        }
        boolean isValidExternalTouchViewport = this.mExternalTouchViewport.valid;
        if (HwPCUtils.enabledInPad() && isValidExternalTouchViewport && !HwPCUtils.isValidExtDisplayId(this.mExternalTouchViewport.displayId)) {
            Slog.d(TAG, "setExternalTouchViewport isInValid displayId in PAD PC mode");
            isValidExternalTouchViewport = false;
        }
        if (HwPCUtils.isPcCastModeInServer() && !isValidExternalTouchViewport && (info.flags & 1) == 0 && display != null && HwPCUtils.isValidExtDisplayId(display.getDisplayIdLocked())) {
            try {
                if (!HwPCUtils.isPcCastMode()) {
                    return;
                }
                if (HwPCUtils.enabledInPad()) {
                    setViewportLocked(this.mExternalTouchViewport, display, device);
                    this.mExternalTouchViewport.logicalFrame.set(0, 0, info.height, info.width);
                    this.mExternalTouchViewport.deviceHeight = info.width;
                    this.mExternalTouchViewport.deviceWidth = info.height;
                    this.mExternalTouchViewport.displayId = display.getDisplayIdLocked();
                    this.mExternalTouchViewport.valid = true;
                    return;
                }
                setViewportLocked(this.mExternalTouchViewport, display, device);
                this.mExternalTouchViewport.logicalFrame.set(0, 0, info.width, info.height);
                this.mExternalTouchViewport.deviceHeight = info.height;
                this.mExternalTouchViewport.deviceWidth = info.width;
                this.mExternalTouchViewport.displayId = display.getDisplayIdLocked();
                this.mExternalTouchViewport.valid = true;
            } catch (Exception e) {
                Slog.w(TAG, "when set external touch view port", e);
            }
        }
    }

    private void loadHwBrightnessProcessors() {
    }

    private DisplayViewport getVirtualTouchViewportLocked(String uniqueId) {
        int count = this.mVirtualTouchViewports.size();
        for (int i = 0; i < count; i++) {
            DisplayViewport viewport = (DisplayViewport) this.mVirtualTouchViewports.get(i);
            if (uniqueId.equals(viewport.uniqueId)) {
                return viewport;
            }
        }
        DisplayViewport viewport2 = new DisplayViewport();
        viewport2.uniqueId = uniqueId;
        this.mVirtualTouchViewports.add(viewport2);
        return viewport2;
    }

    private static void setViewportLocked(DisplayViewport viewport, LogicalDisplay display, DisplayDevice device) {
        viewport.valid = true;
        viewport.displayId = display.getDisplayIdLocked();
        device.populateViewportLocked(viewport);
    }

    private LogicalDisplay findLogicalDisplayForDeviceLocked(DisplayDevice device) {
        int count = this.mLogicalDisplays.size();
        for (int i = 0; i < count; i++) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.valueAt(i);
            if (display.getPrimaryDisplayDeviceLocked() == device) {
                return display;
            }
        }
        return null;
    }

    private void sendDisplayEventLocked(int displayId, int event) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, displayId, event));
    }

    private void scheduleTraversalLocked(boolean inTraversal) {
        if (!this.mPendingTraversal && this.mWindowManagerInternal != null) {
            this.mPendingTraversal = true;
            if (!inTraversal) {
                this.mHandler.sendEmptyMessage(4);
            }
        }
    }

    private void deliverDisplayEvent(int displayId, int event) {
        int count;
        int i;
        synchronized (this.mSyncRoot) {
            count = this.mCallbacks.size();
            this.mTempCallbacks.clear();
            i = 0;
            for (int i2 = 0; i2 < count; i2++) {
                this.mTempCallbacks.add((CallbackRecord) this.mCallbacks.valueAt(i2));
            }
        }
        int count2 = count;
        while (true) {
            count = i;
            if (count < count2) {
                ((CallbackRecord) this.mTempCallbacks.get(count)).notifyDisplayEventAsync(displayId, event);
                i = count + 1;
            } else {
                this.mTempCallbacks.clear();
                return;
            }
        }
    }

    private IMediaProjectionManager getProjectionService() {
        if (this.mProjectionService == null) {
            this.mProjectionService = IMediaProjectionManager.Stub.asInterface(ServiceManager.getService("media_projection"));
        }
        return this.mProjectionService;
    }

    private UserManager getUserManager() {
        return (UserManager) this.mContext.getSystemService(UserManager.class);
    }

    private void dumpInternal(PrintWriter pw) {
        pw.println("DISPLAY MANAGER (dumpsys display)");
        synchronized (this.mSyncRoot) {
            StringBuilder stringBuilder;
            int i;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mOnlyCode=");
            stringBuilder2.append(this.mOnlyCore);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mSafeMode=");
            stringBuilder2.append(this.mSafeMode);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mPendingTraversal=");
            stringBuilder2.append(this.mPendingTraversal);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mGlobalDisplayState=");
            stringBuilder2.append(Display.stateToString(this.mGlobalDisplayState));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mNextNonDefaultDisplayId=");
            stringBuilder2.append(this.mNextNonDefaultDisplayId);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDefaultViewport=");
            stringBuilder2.append(this.mDefaultViewport);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mExternalTouchViewport=");
            stringBuilder2.append(this.mExternalTouchViewport);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mVirtualTouchViewports=");
            stringBuilder2.append(this.mVirtualTouchViewports);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDefaultDisplayDefaultColorMode=");
            stringBuilder2.append(this.mDefaultDisplayDefaultColorMode);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mSingleDisplayDemoMode=");
            stringBuilder2.append(this.mSingleDisplayDemoMode);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mWifiDisplayScanRequestCount=");
            stringBuilder2.append(this.mWifiDisplayScanRequestCount);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mStableDisplaySize=");
            stringBuilder2.append(this.mStableDisplaySize);
            pw.println(stringBuilder2.toString());
            IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "    ");
            ipw.increaseIndent();
            pw.println();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Display Adapters: size=");
            stringBuilder3.append(this.mDisplayAdapters.size());
            pw.println(stringBuilder3.toString());
            Iterator it = this.mDisplayAdapters.iterator();
            while (it.hasNext()) {
                DisplayAdapter adapter = (DisplayAdapter) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(adapter.getName());
                pw.println(stringBuilder.toString());
                adapter.dumpLocked(ipw);
            }
            pw.println();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Display Devices: size=");
            stringBuilder3.append(this.mDisplayDevices.size());
            pw.println(stringBuilder3.toString());
            it = this.mDisplayDevices.iterator();
            while (it.hasNext()) {
                DisplayDevice device = (DisplayDevice) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(device.getDisplayDeviceInfoLocked());
                pw.println(stringBuilder.toString());
                device.dumpLocked(ipw);
            }
            int logicalDisplayCount = this.mLogicalDisplays.size();
            pw.println();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Logical Displays: size=");
            stringBuilder4.append(logicalDisplayCount);
            pw.println(stringBuilder4.toString());
            int i2 = 0;
            for (i = 0; i < logicalDisplayCount; i++) {
                int displayId = this.mLogicalDisplays.keyAt(i);
                LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.valueAt(i);
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  Display ");
                stringBuilder5.append(displayId);
                stringBuilder5.append(":");
                pw.println(stringBuilder5.toString());
                display.dumpLocked(ipw);
            }
            i = this.mCallbacks.size();
            pw.println();
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("Callbacks: size=");
            stringBuilder6.append(i);
            pw.println(stringBuilder6.toString());
            while (i2 < i) {
                CallbackRecord callback = (CallbackRecord) this.mCallbacks.valueAt(i2);
                StringBuilder stringBuilder7 = new StringBuilder();
                stringBuilder7.append("  ");
                stringBuilder7.append(i2);
                stringBuilder7.append(": mPid=");
                stringBuilder7.append(callback.mPid);
                stringBuilder7.append(", mWifiDisplayScanRequested=");
                stringBuilder7.append(callback.mWifiDisplayScanRequested);
                pw.println(stringBuilder7.toString());
                i2++;
            }
            if (this.mDisplayPowerController != null) {
                this.mDisplayPowerController.dump(pw);
            }
            pw.println();
            this.mPersistentDataStore.dump(pw);
        }
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, Float.NaN);
        }
        array.recycle();
        return floatArray;
    }

    @VisibleForTesting
    DisplayDeviceInfo getDisplayDeviceInfoInternal(int displayId) {
        synchronized (this.mSyncRoot) {
            LogicalDisplay display = (LogicalDisplay) this.mLogicalDisplays.get(displayId);
            if (display != null) {
                DisplayDeviceInfo displayDeviceInfoLocked = display.getPrimaryDisplayDeviceLocked().getDisplayDeviceInfoLocked();
                return displayDeviceInfoLocked;
            }
            return null;
        }
    }

    public SyncRoot getLock() {
        return this.mSyncRoot;
    }

    public WifiDisplayAdapter getWifiDisplayAdapter() {
        return this.mWifiDisplayAdapter;
    }

    public void startWifiDisplayScanInner(int callingPid, int channelID) {
        synchronized (this.mSyncRoot) {
            CallbackRecord record = (CallbackRecord) this.mCallbacks.get(callingPid);
            if (record != null) {
                startWifiDisplayScanLocked(record, channelID);
            } else {
                throw new IllegalStateException("The calling process has not registered an IDisplayManagerCallback.");
            }
        }
    }

    private void startWifiDisplayScanLocked(CallbackRecord record, int channelID) {
        String str;
        StringBuilder stringBuilder;
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startWifiDisplayScanLocked mWifiDisplayScanRequestCount=");
            stringBuilder.append(this.mWifiDisplayScanRequestCount);
            Slog.i(str, stringBuilder.toString());
        }
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startWifiDisplayScanLocked record.mWifiDisplayScanRequested=");
            stringBuilder.append(record.mWifiDisplayScanRequested);
            Slog.i(str, stringBuilder.toString());
        }
        if (!record.mWifiDisplayScanRequested) {
            record.mWifiDisplayScanRequested = true;
            int i = this.mWifiDisplayScanRequestCount;
            this.mWifiDisplayScanRequestCount = i + 1;
            if (i == 0 && this.mWifiDisplayAdapter != null) {
                this.mWifiDisplayAdapter.requestStartScanLocked(channelID);
            }
        }
    }
}
