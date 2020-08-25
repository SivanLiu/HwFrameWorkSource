package com.android.server.display;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.hardware.display.HwFoldScreenState;
import android.hardware.sidekick.SidekickInternal;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayAddress;
import android.view.DisplayCutout;
import android.view.DisplayEventReceiver;
import android.view.SurfaceControl;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class LocalDisplayAdapter extends DisplayAdapter {
    private static final Long BUILT_IN_DISPLAY_ID_HDMI = 1L;
    /* access modifiers changed from: private */
    public static final Long BUILT_IN_DISPLAY_ID_MAIN = 0L;
    private static final boolean DEBUG = false;
    /* access modifiers changed from: private */
    public static final int DEFAULT_DENSITYDPI = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
    /* access modifiers changed from: private */
    public static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    /* access modifiers changed from: private */
    public static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
    private static final int PAD_DISPLAY_ID = 100000;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    public static final int REGION_FULL = 7;
    public static final int REGION_MAIN = 1;
    public static final int REGION_SUB = 4;
    private static final String TAG = "LocalDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "local:";
    private static final boolean isChinaArea = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    /* access modifiers changed from: private */
    public int defaultNaviMode = 0;
    /* access modifiers changed from: private */
    public int mButtonLightMode = 1;
    /* access modifiers changed from: private */
    public boolean mDeviceProvisioned = true;
    /* access modifiers changed from: private */
    public final LongSparseArray<LocalDisplayDevice> mDevices = new LongSparseArray<>();
    private HwFoldScreenState mHwFoldScreenState;
    private PhysicalDisplayEventReceiver mPhysicalDisplayEventReceiver;
    /* access modifiers changed from: private */
    public ContentResolver mResolver;
    /* access modifiers changed from: private */
    public SettingsObserver mSettingsObserver;
    /* access modifiers changed from: private */
    public int mTrikeyNaviMode = -1;

    public LocalDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, Listener listener) {
        super(syncRoot, context, handler, listener, TAG);
    }

    @Override // com.android.server.display.DisplayAdapter
    public void registerLocked() {
        super.registerLocked();
        this.mPhysicalDisplayEventReceiver = new PhysicalDisplayEventReceiver(getHandler().getLooper());
        for (long physicalDisplayId : SurfaceControl.getPhysicalDisplayIds()) {
            tryConnectDisplayLocked(physicalDisplayId);
        }
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "tryConnectPadVirtualDisplayLocked");
            tryConnectDisplayLocked(100000);
        }
    }

    /* access modifiers changed from: private */
    public void tryConnectDisplayLocked(long physicalDisplayId) {
        IBinder displayToken;
        int activeColorMode;
        long j;
        LocalDisplayAdapter localDisplayAdapter;
        int i;
        LocalDisplayDevice device;
        IBinder displayToken2 = SurfaceControl.getPhysicalDisplayToken(physicalDisplayId);
        if (physicalDisplayId == 100000) {
            displayToken = SurfaceControl.getPhysicalDisplayToken(0);
        } else {
            displayToken = displayToken2;
        }
        if (displayToken != null) {
            SurfaceControl.PhysicalDisplayInfo[] configs = SurfaceControl.getDisplayConfigs(displayToken);
            if (configs == null) {
                Slog.w(TAG, "No valid configs found for display device " + physicalDisplayId);
                return;
            }
            int activeConfig = SurfaceControl.getActiveConfig(displayToken);
            if (activeConfig < 0) {
                Slog.w(TAG, "No active config found for display device " + physicalDisplayId);
                return;
            }
            int activeColorMode2 = SurfaceControl.getActiveColorMode(displayToken);
            if (activeColorMode2 < 0) {
                Slog.w(TAG, "Unable to get active color mode for display device " + physicalDisplayId);
                activeColorMode = -1;
            } else {
                activeColorMode = activeColorMode2;
            }
            int[] colorModes = SurfaceControl.getDisplayColorModes(displayToken);
            int[] allowedConfigs = SurfaceControl.getAllowedDisplayConfigs(displayToken);
            LocalDisplayDevice device2 = this.mDevices.get(physicalDisplayId);
            if (device2 == null) {
                boolean isInternal = this.mDevices.size() == 0;
                if (physicalDisplayId != BUILT_IN_DISPLAY_ID_MAIN.longValue() && physicalDisplayId != 100000) {
                    device = new LocalDisplayDevice(this, displayToken, physicalDisplayId, configs, activeConfig, allowedConfigs, colorModes, activeColorMode, isInternal);
                    i = 1;
                    j = physicalDisplayId;
                    localDisplayAdapter = this;
                } else if (this.mHwFoldScreenState != null || !HwFoldScreenState.isFoldScreenDevice()) {
                    i = 1;
                    j = physicalDisplayId;
                    localDisplayAdapter = this;
                    device = new LocalDisplayDevice(this, displayToken, physicalDisplayId, configs, activeConfig, allowedConfigs, colorModes, activeColorMode, isInternal);
                } else {
                    this.mHwFoldScreenState = HwServiceFactory.getHwFoldScreenState(getContext());
                    Slog.d(TAG, "new LocalDisplayDevice() physicalDisplayId=" + physicalDisplayId);
                    i = 1;
                    j = physicalDisplayId;
                    localDisplayAdapter = this;
                    device = new LocalDisplayDevice(displayToken, physicalDisplayId, configs, activeConfig, allowedConfigs, colorModes, activeColorMode, isInternal, this.mHwFoldScreenState);
                }
                localDisplayAdapter.mDevices.put(j, device);
                localDisplayAdapter.sendDisplayDeviceEventLocked(device, i);
            } else if (device2.updatePhysicalDisplayInfoLocked(configs, activeConfig, allowedConfigs, colorModes, activeColorMode)) {
                sendDisplayDeviceEventLocked(device2, 2);
            }
        }
    }

    /* access modifiers changed from: private */
    public void tryDisconnectDisplayLocked(long physicalDisplayId) {
        LocalDisplayDevice device = this.mDevices.get(physicalDisplayId);
        if (device != null) {
            this.mDevices.remove(physicalDisplayId);
            sendDisplayDeviceEventLocked(device, 3);
            return;
        }
        Log.i(TAG, "tryDisconnectDisplayLocked: device == null");
    }

    static int getPowerModeForState(int state) {
        if (state == 1) {
            return 0;
        }
        if (state == 6) {
            return 4;
        }
        if (state == 3) {
            return 1;
        }
        if (state != 4) {
            return 2;
        }
        return 3;
    }

    static int setDisplayStatus(IBinder token, int displayMode, int flodState) {
        int region;
        Slog.d(TAG, "setDisplayStatus: displayMode=" + displayMode + ", flodState=" + flodState);
        Rect rectMain = HwFoldScreenState.getScreenPhysicalRect(2);
        Rect rectSub = HwFoldScreenState.getScreenPhysicalRect(3);
        Rect rectFull = HwFoldScreenState.getScreenPhysicalRect(1);
        if (2 == displayMode) {
            region = 1;
        } else if (3 == displayMode) {
            region = 4;
        } else {
            region = 7;
        }
        if (flodState == 1) {
            SurfaceControl.setDisplayStatus(token, region, flodState, rectFull, rectSub);
            return 0;
        }
        SurfaceControl.setDisplayStatus(token, region, flodState, rectMain, rectSub);
        return 0;
    }

    public void registerContentObserver(Context context, Handler handler) {
        if (context != null && FRONT_FINGERPRINT_NAVIGATION && FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
            try {
                this.mResolver = context.getContentResolver();
                this.mSettingsObserver = new SettingsObserver(handler);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.USER_SWITCHED");
                context.registerReceiver(new UserSwtichReceiver(), intentFilter);
            } catch (Exception exp) {
                Log.e(TAG, "registerContentObserver:" + exp.getMessage());
            }
        }
    }

    /* access modifiers changed from: private */
    public class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
            boolean unused = LocalDisplayAdapter.this.mDeviceProvisioned = Settings.Secure.getIntForUser(LocalDisplayAdapter.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0;
            int unused2 = LocalDisplayAdapter.this.mTrikeyNaviMode = Settings.System.getIntForUser(LocalDisplayAdapter.this.mResolver, "swap_key_position", LocalDisplayAdapter.this.defaultNaviMode, ActivityManager.getCurrentUser());
            int unused3 = LocalDisplayAdapter.this.mButtonLightMode = Settings.System.getIntForUser(LocalDisplayAdapter.this.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
        }

        public void registerContentObserver(int userId) {
            LocalDisplayAdapter.this.mResolver.registerContentObserver(Settings.System.getUriFor("device_provisioned"), false, this, userId);
            LocalDisplayAdapter.this.mResolver.registerContentObserver(Settings.System.getUriFor("swap_key_position"), false, this, userId);
            LocalDisplayAdapter.this.mResolver.registerContentObserver(Settings.System.getUriFor("button_light_mode"), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            LocalDisplayAdapter localDisplayAdapter = LocalDisplayAdapter.this;
            boolean z = false;
            if (Settings.Secure.getIntForUser(localDisplayAdapter.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0) {
                z = true;
            }
            boolean unused = localDisplayAdapter.mDeviceProvisioned = z;
            LocalDisplayAdapter localDisplayAdapter2 = LocalDisplayAdapter.this;
            int unused2 = localDisplayAdapter2.mTrikeyNaviMode = Settings.System.getIntForUser(localDisplayAdapter2.mResolver, "swap_key_position", LocalDisplayAdapter.this.defaultNaviMode, ActivityManager.getCurrentUser());
            LocalDisplayAdapter localDisplayAdapter3 = LocalDisplayAdapter.this;
            int unused3 = localDisplayAdapter3.mButtonLightMode = Settings.System.getIntForUser(localDisplayAdapter3.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
            Slog.i(LocalDisplayAdapter.TAG, "mTrikeyNaviMode:" + LocalDisplayAdapter.this.mTrikeyNaviMode + " mButtonLightMode:" + LocalDisplayAdapter.this.mButtonLightMode);
        }
    }

    private class UserSwtichReceiver extends BroadcastReceiver {
        private UserSwtichReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && "android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                int newUserId = intent.getIntExtra("android.intent.extra.user_handle", UserHandle.myUserId());
                Slog.i(LocalDisplayAdapter.TAG, "UserSwtichReceiver:" + newUserId);
                if (LocalDisplayAdapter.this.mSettingsObserver != null) {
                    LocalDisplayAdapter.this.mSettingsObserver.registerContentObserver(newUserId);
                    LocalDisplayAdapter.this.mSettingsObserver.onChange(true);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isButtonLightTimeout() {
        return SystemProperties.getBoolean("sys.button.light.timeout", false);
    }

    /* access modifiers changed from: private */
    public void setButtonLightTimeout(boolean timeout) {
        SystemProperties.set("sys.button.light.timeout", String.valueOf(timeout));
    }

    /* access modifiers changed from: private */
    public final class LocalDisplayDevice extends DisplayDevice {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private int mActiveColorMode;
        private boolean mActiveColorModeInvalid;
        private int mActiveModeId;
        private boolean mActiveModeInvalid;
        private int mActivePhysIndex;
        private int[] mAllowedModeIds;
        private boolean mAllowedModeIdsInvalid;
        private int[] mAllowedPhysIndexes;
        /* access modifiers changed from: private */
        public final Light mBacklight;
        private int mBrightness;
        private final Light mButtonlight;
        private int mDefaultModeId;
        private SurfaceControl.PhysicalDisplayInfo[] mDisplayInfos;
        private boolean mHavePendingChanges;
        private Display.HdrCapabilities mHdrCapabilities;
        private DisplayDeviceInfo mInfo;
        private final boolean mIsInternal;
        private final long mPhysicalDisplayId;
        public boolean mRogChange;
        /* access modifiers changed from: private */
        public boolean mSidekickActive;
        /* access modifiers changed from: private */
        public SidekickInternal mSidekickInternal;
        private int mState;
        private final ArrayList<Integer> mSupportedColorModes;
        private final SparseArray<DisplayModeRecord> mSupportedModes;

        public LocalDisplayDevice(LocalDisplayAdapter localDisplayAdapter, IBinder displayToken, long physicalDisplayId, SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo, int[] allowedDisplayInfos, int[] colorModes, int activeColorMode, boolean isInternal) {
            this(displayToken, physicalDisplayId, physicalDisplayInfos, activeDisplayInfo, allowedDisplayInfos, colorModes, activeColorMode, isInternal, null);
        }

        public LocalDisplayDevice(IBinder displayToken, long physicalDisplayId, SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo, int[] allowedDisplayInfos, int[] colorModes, int activeColorMode, boolean isInternal, HwFoldScreenState foldScreenState) {
            super(LocalDisplayAdapter.this, displayToken, LocalDisplayAdapter.UNIQUE_ID_PREFIX + physicalDisplayId, foldScreenState);
            this.mSupportedModes = new SparseArray<>();
            this.mSupportedColorModes = new ArrayList<>();
            this.mRogChange = false;
            this.mState = 0;
            this.mBrightness = -1;
            this.mPhysicalDisplayId = physicalDisplayId;
            this.mIsInternal = isInternal;
            updatePhysicalDisplayInfoLocked(physicalDisplayInfos, activeDisplayInfo, allowedDisplayInfos, colorModes, activeColorMode);
            updateColorModesLocked(colorModes, activeColorMode);
            this.mSidekickInternal = (SidekickInternal) LocalServices.getService(SidekickInternal.class);
            if (this.mIsInternal) {
                LightsManager lights = (LightsManager) LocalServices.getService(LightsManager.class);
                this.mBacklight = lights.getLight(0);
                if (!LocalDisplayAdapter.FRONT_FINGERPRINT_NAVIGATION || LocalDisplayAdapter.FRONT_FINGERPRINT_NAVIGATION_TRIKEY != 1) {
                    this.mButtonlight = null;
                } else {
                    this.mButtonlight = lights.getLight(2);
                }
            } else {
                this.mBacklight = null;
                this.mButtonlight = null;
            }
            this.mHdrCapabilities = SurfaceControl.getHdrCapabilities(displayToken);
        }

        @Override // com.android.server.display.DisplayDevice
        public boolean hasStableUniqueId() {
            return true;
        }

        @Override // com.android.server.display.DisplayDevice
        public void updateDesityforRog() {
            this.mHavePendingChanges = true;
            this.mRogChange = true;
        }

        public boolean updatePhysicalDisplayInfoLocked(SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo, int[] allowedDisplayInfos, int[] colorModes, int activeColorMode) {
            this.mDisplayInfos = (SurfaceControl.PhysicalDisplayInfo[]) Arrays.copyOf(physicalDisplayInfos, physicalDisplayInfos.length);
            this.mActivePhysIndex = activeDisplayInfo;
            this.mAllowedPhysIndexes = Arrays.copyOf(allowedDisplayInfos, allowedDisplayInfos.length);
            ArrayList<DisplayModeRecord> records = new ArrayList<>();
            boolean modesAdded = false;
            for (SurfaceControl.PhysicalDisplayInfo info : physicalDisplayInfos) {
                boolean existingMode = false;
                int j = 0;
                while (true) {
                    if (j >= records.size()) {
                        break;
                    } else if (records.get(j).hasMatchingMode(info)) {
                        existingMode = true;
                        break;
                    } else {
                        j++;
                    }
                }
                if (!existingMode) {
                    DisplayModeRecord record = findDisplayModeRecord(info);
                    if (record == null) {
                        record = new DisplayModeRecord(info);
                        modesAdded = true;
                    }
                    records.add(record);
                }
            }
            DisplayModeRecord activeRecord = null;
            int i = 0;
            while (true) {
                if (i >= records.size()) {
                    break;
                }
                DisplayModeRecord record2 = records.get(i);
                if (record2.hasMatchingMode(physicalDisplayInfos[activeDisplayInfo])) {
                    activeRecord = record2;
                    break;
                }
                i++;
            }
            int i2 = this.mActiveModeId;
            if (!(i2 == 0 || i2 == activeRecord.mMode.getModeId())) {
                this.mActiveModeInvalid = true;
                LocalDisplayAdapter.this.sendTraversalRequestLocked();
            }
            if (!(records.size() != this.mSupportedModes.size() || modesAdded)) {
                return false;
            }
            this.mHavePendingChanges = true;
            this.mSupportedModes.clear();
            Iterator<DisplayModeRecord> it = records.iterator();
            while (it.hasNext()) {
                DisplayModeRecord record3 = it.next();
                this.mSupportedModes.put(record3.mMode.getModeId(), record3);
            }
            if (findDisplayInfoIndexLocked(this.mDefaultModeId) < 0) {
                if (this.mDefaultModeId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Default display mode no longer available, using currently active mode as default.");
                }
                this.mDefaultModeId = activeRecord.mMode.getModeId();
            }
            if (this.mSupportedModes.indexOfKey(this.mActiveModeId) < 0) {
                if (this.mActiveModeId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active display mode no longer available, reverting to default mode.");
                }
                this.mActiveModeId = this.mDefaultModeId;
                this.mActiveModeInvalid = true;
            }
            this.mAllowedModeIds = new int[]{this.mActiveModeId};
            int[] iArr = this.mAllowedPhysIndexes;
            int[] allowedModeIds = new int[iArr.length];
            int size = 0;
            for (int physIndex : iArr) {
                int modeId = findMatchingModeIdLocked(physIndex);
                if (modeId > 0) {
                    allowedModeIds[size] = modeId;
                    size++;
                }
            }
            this.mAllowedModeIdsInvalid = !Arrays.equals(allowedModeIds, this.mAllowedModeIds);
            LocalDisplayAdapter.this.sendTraversalRequestLocked();
            return true;
        }

        @Override // com.android.server.display.DisplayDevice
        public boolean isFoldable() {
            return ((this.mPhysicalDisplayId > LocalDisplayAdapter.BUILT_IN_DISPLAY_ID_MAIN.longValue() ? 1 : (this.mPhysicalDisplayId == LocalDisplayAdapter.BUILT_IN_DISPLAY_ID_MAIN.longValue() ? 0 : -1)) == 0 || (this.mPhysicalDisplayId > 100000 ? 1 : (this.mPhysicalDisplayId == 100000 ? 0 : -1)) == 0) && HwFoldScreenState.isFoldScreenDevice();
        }

        @Override // com.android.server.display.DisplayDevice
        public Rect getScreenDispRect(int orientation) {
            if (!isFoldable() || this.mHwFoldScreenState == null) {
                return null;
            }
            return this.mHwFoldScreenState.getScreenDispRect(orientation);
        }

        @Override // com.android.server.display.DisplayDevice
        public int getDisplayState() {
            if (!isFoldable() || this.mHwFoldScreenState == null) {
                return 0;
            }
            int state = this.mHwFoldScreenState.getDisplayMode();
            Slog.d(LocalDisplayAdapter.TAG, "getDisplayState: " + state);
            return state;
        }

        @Override // com.android.server.display.DisplayDevice
        public int setDisplayState(int state) {
            Slog.d(LocalDisplayAdapter.TAG, "setDisplayState=" + state);
            if (!isFoldable() || this.mHwFoldScreenState == null) {
                Slog.d(LocalDisplayAdapter.TAG, "setDisplayState: not a foldable device");
                return 0;
            }
            this.mHwFoldScreenState.getDisplayMode();
            return this.mHwFoldScreenState.setDisplayMode(state);
        }

        @Override // com.android.server.display.DisplayDevice
        public int setDisplayState(int displayMode, int flodState) {
            LocalDisplayAdapter.setDisplayStatus(getDisplayTokenLocked(), displayMode, flodState);
            setDisplayState(displayMode);
            return 0;
        }

        private boolean updateColorModesLocked(int[] colorModes, int activeColorMode) {
            List<Integer> pendingColorModes = new ArrayList<>();
            if (colorModes == null) {
                return false;
            }
            boolean colorModesAdded = false;
            for (int colorMode : colorModes) {
                if (!this.mSupportedColorModes.contains(Integer.valueOf(colorMode))) {
                    colorModesAdded = true;
                }
                pendingColorModes.add(Integer.valueOf(colorMode));
            }
            if (!(pendingColorModes.size() != this.mSupportedColorModes.size() || colorModesAdded)) {
                return false;
            }
            this.mHavePendingChanges = true;
            this.mSupportedColorModes.clear();
            this.mSupportedColorModes.addAll(pendingColorModes);
            Collections.sort(this.mSupportedColorModes);
            if (!this.mSupportedColorModes.contains(Integer.valueOf(this.mActiveColorMode))) {
                if (this.mActiveColorMode != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active color mode no longer available, reverting to default mode.");
                    this.mActiveColorMode = 0;
                    this.mActiveColorModeInvalid = true;
                } else if (!this.mSupportedColorModes.isEmpty()) {
                    Slog.e(LocalDisplayAdapter.TAG, "Default and active color mode is no longer available! Reverting to first available mode.");
                    this.mActiveColorMode = this.mSupportedColorModes.get(0).intValue();
                    this.mActiveColorModeInvalid = true;
                } else {
                    Slog.e(LocalDisplayAdapter.TAG, "No color modes available!");
                }
            }
            return true;
        }

        private DisplayModeRecord findDisplayModeRecord(SurfaceControl.PhysicalDisplayInfo info) {
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                DisplayModeRecord record = this.mSupportedModes.valueAt(i);
                if (record.hasMatchingMode(info)) {
                    return record;
                }
            }
            return null;
        }

        @Override // com.android.server.display.DisplayDevice
        public void applyPendingDisplayDeviceInfoChangesLocked() {
            if (this.mHavePendingChanges) {
                this.mInfo = null;
                this.mHavePendingChanges = false;
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                SurfaceControl.PhysicalDisplayInfo phys = this.mDisplayInfos[this.mActivePhysIndex];
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.width = phys.width;
                this.mInfo.height = phys.height;
                DisplayDeviceInfo displayDeviceInfo = this.mInfo;
                displayDeviceInfo.modeId = this.mActiveModeId;
                displayDeviceInfo.defaultModeId = this.mDefaultModeId;
                displayDeviceInfo.supportedModes = getDisplayModes(this.mSupportedModes);
                DisplayDeviceInfo displayDeviceInfo2 = this.mInfo;
                displayDeviceInfo2.colorMode = this.mActiveColorMode;
                displayDeviceInfo2.supportedColorModes = new int[this.mSupportedColorModes.size()];
                for (int i = 0; i < this.mSupportedColorModes.size(); i++) {
                    this.mInfo.supportedColorModes[i] = this.mSupportedColorModes.get(i).intValue();
                }
                DisplayDeviceInfo displayDeviceInfo3 = this.mInfo;
                displayDeviceInfo3.hdrCapabilities = this.mHdrCapabilities;
                displayDeviceInfo3.appVsyncOffsetNanos = phys.appVsyncOffsetNanos;
                this.mInfo.presentationDeadlineNanos = phys.presentationDeadlineNanos;
                DisplayDeviceInfo displayDeviceInfo4 = this.mInfo;
                displayDeviceInfo4.state = this.mState;
                displayDeviceInfo4.uniqueId = getUniqueId();
                DisplayAddress.Physical physicalAddress = DisplayAddress.fromPhysicalDisplayId(this.mPhysicalDisplayId);
                this.mInfo.address = physicalAddress;
                if (phys.secure) {
                    this.mInfo.flags = 12;
                }
                Resources res = LocalDisplayAdapter.this.getOverlayContext().getResources();
                if (this.mIsInternal) {
                    this.mInfo.name = res.getString(17039991);
                    DisplayDeviceInfo displayDeviceInfo5 = this.mInfo;
                    displayDeviceInfo5.flags = 3 | displayDeviceInfo5.flags;
                    if (res.getBoolean(17891477) || (Build.IS_EMULATOR && SystemProperties.getBoolean(LocalDisplayAdapter.PROPERTY_EMULATOR_CIRCULAR, false))) {
                        this.mInfo.flags |= 256;
                    }
                    if (res.getBoolean(17891478)) {
                        this.mInfo.flags |= 2048;
                    }
                    int width = SystemProperties.getInt("persist.sys.rog.width", this.mInfo.width);
                    int height = SystemProperties.getInt("persist.sys.rog.height", this.mInfo.height);
                    this.mInfo.displayCutout = DisplayCutout.fromResourcesRectApproximation(res, width, height);
                    Slog.v(LocalDisplayAdapter.TAG, "getDisplayDeviceInfoLocked called," + width + "x" + height + ", " + this.mInfo.width + "x" + this.mInfo.height + "displayCutout " + this.mInfo.displayCutout);
                    DisplayDeviceInfo displayDeviceInfo6 = this.mInfo;
                    displayDeviceInfo6.type = 1;
                    displayDeviceInfo6.densityDpi = (int) ((phys.density * 160.0f) + 0.5f);
                    this.mInfo.xDpi = phys.xDpi;
                    this.mInfo.yDpi = phys.yDpi;
                    this.mInfo.touch = 1;
                } else if (this.mPhysicalDisplayId == 100000) {
                    DisplayDeviceInfo displayDeviceInfo7 = this.mInfo;
                    displayDeviceInfo7.name = "HUAWEI PAD PC Display";
                    displayDeviceInfo7.flags |= 2;
                    DisplayDeviceInfo displayDeviceInfo8 = this.mInfo;
                    displayDeviceInfo8.type = 2;
                    displayDeviceInfo8.touch = 1;
                    displayDeviceInfo8.densityDpi = LocalDisplayAdapter.DEFAULT_DENSITYDPI == 0 ? (int) ((phys.density * 160.0f) + 0.5f) : LocalDisplayAdapter.DEFAULT_DENSITYDPI;
                    HwPCUtils.log(LocalDisplayAdapter.TAG, "PAD_DISPLAY_ID densityDpi:" + this.mInfo.densityDpi);
                    this.mInfo.xDpi = phys.xDpi;
                    this.mInfo.yDpi = phys.yDpi;
                    HwPCUtils.log(LocalDisplayAdapter.TAG, "PAD_DISPLAY_ID mInfo.xDpi:" + this.mInfo.xDpi + ",mInfo.yDpi:" + this.mInfo.yDpi);
                } else {
                    DisplayDeviceInfo displayDeviceInfo9 = this.mInfo;
                    displayDeviceInfo9.displayCutout = null;
                    displayDeviceInfo9.type = 2;
                    displayDeviceInfo9.flags |= 64;
                    this.mInfo.name = LocalDisplayAdapter.this.getContext().getResources().getString(17039992);
                    this.mInfo.touch = 2;
                    if (HwPCUtils.enabled()) {
                        this.mInfo.densityDpi = (int) ((phys.density * 160.0f) + 0.5f);
                        HwPCUtils.log(LocalDisplayAdapter.TAG, "densityDpi:" + this.mInfo.densityDpi);
                        DisplayDeviceInfo displayDeviceInfo10 = this.mInfo;
                        displayDeviceInfo10.xDpi = (float) displayDeviceInfo10.densityDpi;
                        DisplayDeviceInfo displayDeviceInfo11 = this.mInfo;
                        displayDeviceInfo11.yDpi = (float) displayDeviceInfo11.densityDpi;
                    } else {
                        this.mInfo.setAssumedDensityForExternalDisplay(phys.width, phys.height);
                    }
                    if (HwFrameworkFactory.getVRSystemServiceManager() != null && HwFrameworkFactory.getVRSystemServiceManager().isVRDisplay((int) this.mPhysicalDisplayId, this.mInfo.width, this.mInfo.height)) {
                        DisplayDeviceInfo displayDeviceInfo12 = this.mInfo;
                        HwFrameworkFactory.getVRSystemServiceManager();
                        displayDeviceInfo12.name = "HUAWEI VR Display";
                    }
                    if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
                        this.mInfo.rotation = 3;
                    }
                    if (SystemProperties.getBoolean("persist.demo.hdmirotates", false)) {
                        this.mInfo.flags |= 2;
                    }
                    if (!res.getBoolean(17891473)) {
                        this.mInfo.flags |= 128;
                    }
                    if (isDisplayPrivate(physicalAddress)) {
                        this.mInfo.flags |= 16;
                    }
                }
            }
            return this.mInfo;
        }

        @Override // com.android.server.display.DisplayDevice
        public Runnable requestDisplayStateLocked(final int state, final int brightness) {
            final boolean brightnessChanged = true;
            boolean stateChanged = this.mState != state;
            if (this.mBrightness == brightness || this.mBacklight == null) {
                brightnessChanged = false;
            }
            if (!stateChanged && !brightnessChanged) {
                return null;
            }
            final long physicalDisplayId = this.mPhysicalDisplayId;
            final IBinder token = getDisplayTokenLocked();
            final int oldState = this.mState;
            if (stateChanged) {
                this.mState = state;
                updateDeviceInfoLocked();
            }
            if (brightnessChanged) {
                this.mBrightness = brightness;
            }
            return new Runnable() {
                /* class com.android.server.display.LocalDisplayAdapter.LocalDisplayDevice.AnonymousClass1 */

                public void run() {
                    int i;
                    int i2;
                    int currentState = oldState;
                    if (Display.isSuspendedState(oldState) || oldState == 0) {
                        if (!Display.isSuspendedState(state)) {
                            setDisplayState(state);
                            currentState = state;
                        } else {
                            int i3 = state;
                            if (i3 == 4 || (i2 = oldState) == 4) {
                                setDisplayState(3);
                                currentState = 3;
                            } else if (i3 == 6 || i2 == 6) {
                                setDisplayState(2);
                                currentState = 2;
                            } else {
                                return;
                            }
                        }
                    }
                    boolean vrModeChange = false;
                    if ((state == 5 || currentState == 5) && currentState != (i = state)) {
                        setVrMode(i == 5);
                        vrModeChange = true;
                    }
                    if (brightnessChanged || vrModeChange) {
                        setDisplayBrightness(brightness);
                    }
                    int i4 = state;
                    if (i4 != currentState) {
                        setDisplayState(i4);
                    }
                }

                private void setVrMode(boolean isVrEnabled) {
                    Slog.d(LocalDisplayAdapter.TAG, "setVrMode(id=" + physicalDisplayId + ", state=" + Display.stateToString(state) + ")");
                    if (LocalDisplayDevice.this.mBacklight != null) {
                        LocalDisplayDevice.this.mBacklight.setVrMode(isVrEnabled);
                    }
                }

                /* JADX INFO: finally extract failed */
                private void setDisplayState(int state) {
                    if (LocalDisplayDevice.this.mSidekickActive) {
                        Trace.traceBegin(131072, "SidekickInternal#endDisplayControl");
                        try {
                            LocalDisplayDevice.this.mSidekickInternal.endDisplayControl();
                            Trace.traceEnd(131072);
                            boolean unused = LocalDisplayDevice.this.mSidekickActive = false;
                        } catch (Throwable th) {
                            Trace.traceEnd(131072);
                            throw th;
                        }
                    }
                    int mode = LocalDisplayAdapter.getPowerModeForState(state);
                    Trace.traceBegin(131072, "setDisplayState(id=" + physicalDisplayId + ", state=" + Display.stateToString(state) + ")");
                    try {
                        SurfaceControl.setDisplayPowerMode(token, mode);
                        Trace.traceCounter(131072, "DisplayPowerMode", mode);
                        Trace.traceEnd(131072);
                        if (Display.isSuspendedState(state) && state != 1 && LocalDisplayDevice.this.mSidekickInternal != null && !LocalDisplayDevice.this.mSidekickActive) {
                            Trace.traceBegin(131072, "SidekickInternal#startDisplayControl");
                            try {
                                boolean unused2 = LocalDisplayDevice.this.mSidekickActive = LocalDisplayDevice.this.mSidekickInternal.startDisplayControl(state);
                            } finally {
                                Trace.traceEnd(131072);
                            }
                        }
                    } catch (Throwable th2) {
                        Trace.traceEnd(131072);
                        throw th2;
                    }
                }

                private void setDisplayBrightness(int brightness) {
                    Trace.traceBegin(131072, "setDisplayBrightness(id=" + physicalDisplayId + ", brightness=" + brightness + ")");
                    try {
                        LocalDisplayDevice.this.mBacklight.setBrightness(brightness);
                        LocalDisplayDevice.this.updateButtonBrightness(brightness);
                        Trace.traceCounter(131072, "ScreenBrightness", brightness);
                    } finally {
                        Trace.traceEnd(131072);
                    }
                }
            };
        }

        /* access modifiers changed from: private */
        public void updateButtonBrightness(int brightness) {
            if (this.mButtonlight != null && LocalDisplayAdapter.this.mDeviceProvisioned) {
                if (LocalDisplayAdapter.this.mTrikeyNaviMode >= 0) {
                    if (LocalDisplayAdapter.this.mButtonLightMode != 0) {
                        LocalDisplayAdapter.this.setButtonLightTimeout(false);
                    } else if (brightness == 0) {
                        LocalDisplayAdapter.this.setButtonLightTimeout(false);
                    }
                    if (!LocalDisplayAdapter.this.isButtonLightTimeout()) {
                        this.mButtonlight.setBrightness(brightness);
                        return;
                    }
                    return;
                }
                LocalDisplayAdapter.this.setButtonLightTimeout(false);
                this.mButtonlight.setBrightness(0);
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public void setRequestedColorModeLocked(int colorMode) {
            if (requestColorModeLocked(colorMode)) {
                updateDeviceInfoLocked();
            }
        }

        @Override // com.android.server.display.DisplayDevice
        public void setAllowedDisplayModesLocked(int[] modes) {
            updateAllowedModesLocked(modes);
        }

        @Override // com.android.server.display.DisplayDevice
        public void onOverlayChangedLocked() {
            updateDeviceInfoLocked();
        }

        public void onActivePhysicalDisplayModeChangedLocked(int physIndex) {
            if (updateActiveModeLocked(physIndex)) {
                updateDeviceInfoLocked();
            }
        }

        public boolean updateActiveModeLocked(int activePhysIndex) {
            boolean z = false;
            if (this.mActivePhysIndex == activePhysIndex) {
                return false;
            }
            if (activePhysIndex < 0) {
                Slog.w(LocalDisplayAdapter.TAG, "updateActiveModeLocked activePhysIndex : " + activePhysIndex);
                return false;
            }
            this.mActivePhysIndex = activePhysIndex;
            this.mActiveModeId = findMatchingModeIdLocked(activePhysIndex);
            if (this.mActiveModeId == 0) {
                z = true;
            }
            this.mActiveModeInvalid = z;
            if (this.mActiveModeInvalid) {
                Slog.w(LocalDisplayAdapter.TAG, "In unknown mode after setting allowed configs: allowedPhysIndexes=" + this.mAllowedPhysIndexes + ", activePhysIndex=" + this.mActivePhysIndex);
            }
            return true;
        }

        public void updateAllowedModesLocked(int[] allowedModes) {
            if ((!Arrays.equals(allowedModes, this.mAllowedModeIds) || this.mAllowedModeIdsInvalid) && updateAllowedModesInternalLocked(allowedModes)) {
                updateDeviceInfoLocked();
            }
        }

        public boolean updateAllowedModesInternalLocked(int[] allowedModes) {
            int[] allowedPhysIndexes = new int[allowedModes.length];
            int size = 0;
            for (int modeId : allowedModes) {
                int physIndex = findDisplayInfoIndexLocked(modeId);
                if (physIndex < 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Requested mode ID " + modeId + " not available, dropping from allowed set.");
                } else {
                    allowedPhysIndexes[size] = physIndex;
                    size++;
                }
            }
            if (size != allowedModes.length) {
                allowedPhysIndexes = Arrays.copyOf(allowedPhysIndexes, size);
            }
            if (size == 0) {
                int i = this.mDefaultModeId;
                allowedModes = new int[]{i};
                allowedPhysIndexes = new int[]{findDisplayInfoIndexLocked(i)};
            }
            this.mAllowedModeIds = allowedModes;
            this.mAllowedModeIdsInvalid = false;
            if (Arrays.equals(this.mAllowedPhysIndexes, allowedPhysIndexes)) {
                return false;
            }
            this.mAllowedPhysIndexes = allowedPhysIndexes;
            SurfaceControl.setAllowedDisplayConfigs(getDisplayTokenLocked(), allowedPhysIndexes);
            return updateActiveModeLocked(SurfaceControl.getActiveConfig(getDisplayTokenLocked()));
        }

        public boolean requestColorModeLocked(int colorMode) {
            if (this.mActiveColorMode == colorMode) {
                return false;
            }
            if (!this.mSupportedColorModes.contains(Integer.valueOf(colorMode))) {
                Slog.w(LocalDisplayAdapter.TAG, "Unable to find color mode " + colorMode + ", ignoring request.");
                return false;
            }
            SurfaceControl.setActiveColorMode(getDisplayTokenLocked(), colorMode);
            this.mActiveColorMode = colorMode;
            this.mActiveColorModeInvalid = false;
            return true;
        }

        @Override // com.android.server.display.DisplayDevice
        public void dumpLocked(PrintWriter pw) {
            super.dumpLocked(pw);
            pw.println("mPhysicalDisplayId=" + this.mPhysicalDisplayId);
            pw.println("mAllowedPhysIndexes=" + Arrays.toString(this.mAllowedPhysIndexes));
            pw.println("mAllowedModeIds=" + Arrays.toString(this.mAllowedModeIds));
            pw.println("mAllowedModeIdsInvalid=" + this.mAllowedModeIdsInvalid);
            pw.println("mActivePhysIndex=" + this.mActivePhysIndex);
            pw.println("mActiveModeId=" + this.mActiveModeId);
            pw.println("mActiveColorMode=" + this.mActiveColorMode);
            pw.println("mDefaultModeId=" + this.mDefaultModeId);
            pw.println("mState=" + Display.stateToString(this.mState));
            pw.println("mBrightness=" + this.mBrightness);
            pw.println("mBacklight=" + this.mBacklight);
            pw.println("mDisplayInfos=");
            for (int i = 0; i < this.mDisplayInfos.length; i++) {
                pw.println("  " + this.mDisplayInfos[i]);
            }
            pw.println("mSupportedModes=");
            for (int i2 = 0; i2 < this.mSupportedModes.size(); i2++) {
                pw.println("  " + this.mSupportedModes.valueAt(i2));
            }
            pw.print("mSupportedColorModes=[");
            for (int i3 = 0; i3 < this.mSupportedColorModes.size(); i3++) {
                if (i3 != 0) {
                    pw.print(", ");
                }
                pw.print(this.mSupportedColorModes.get(i3));
            }
            pw.println("]");
        }

        private int findDisplayInfoIndexLocked(int modeId) {
            DisplayModeRecord record = this.mSupportedModes.get(modeId);
            if (record == null) {
                return -1;
            }
            int i = 0;
            while (true) {
                SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfoArr = this.mDisplayInfos;
                if (i >= physicalDisplayInfoArr.length) {
                    return -1;
                }
                if (record.hasMatchingMode(physicalDisplayInfoArr[i])) {
                    return i;
                }
                i++;
            }
        }

        private int findMatchingModeIdLocked(int physIndex) {
            SurfaceControl.PhysicalDisplayInfo info = this.mDisplayInfos[physIndex];
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                DisplayModeRecord record = this.mSupportedModes.valueAt(i);
                if (record.hasMatchingMode(info)) {
                    return record.mMode.getModeId();
                }
            }
            return 0;
        }

        private void updateDeviceInfoLocked() {
            this.mInfo = null;
            LocalDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
        }

        private Display.Mode[] getDisplayModes(SparseArray<DisplayModeRecord> records) {
            int size = records.size();
            Display.Mode[] modes = new Display.Mode[size];
            for (int i = 0; i < size; i++) {
                modes[i] = records.valueAt(i).mMode;
            }
            return modes;
        }

        private boolean isDisplayPrivate(DisplayAddress.Physical physicalAddress) {
            int[] ports;
            if (!(physicalAddress == null || (ports = LocalDisplayAdapter.this.getOverlayContext().getResources().getIntArray(17236031)) == null)) {
                int port = physicalAddress.getPort();
                for (int p : ports) {
                    if (p == port) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* access modifiers changed from: package-private */
    public Context getOverlayContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    private static final class DisplayModeRecord {
        public final Display.Mode mMode;

        public DisplayModeRecord(SurfaceControl.PhysicalDisplayInfo phys) {
            this.mMode = DisplayAdapter.createMode(phys.width, phys.height, phys.refreshRate);
        }

        public boolean hasMatchingMode(SurfaceControl.PhysicalDisplayInfo info) {
            return this.mMode.getPhysicalWidth() == info.width && this.mMode.getPhysicalHeight() == info.height && Float.floatToIntBits(this.mMode.getRefreshRate()) == Float.floatToIntBits(info.refreshRate);
        }

        public String toString() {
            return "DisplayModeRecord{mMode=" + this.mMode + "}";
        }
    }

    public void pcDisplayChangeService(boolean connected) {
        if (HwPCUtils.enabledInPad()) {
            Slog.w(TAG, "pcDisplayChangeService connected = " + connected);
            synchronized (getSyncRoot()) {
                if (!connected) {
                    Slog.w(TAG, "pcDisplayChangeService tryDisconnectDisplayLocked");
                    tryDisconnectDisplayLocked(100000);
                } else if (this.mDevices.get(100000) == null) {
                    Slog.w(TAG, "pcDisplayChangeService tryDisconnectDisplayLocked");
                    tryConnectDisplayLocked(100000);
                }
            }
        }
    }

    private final class PhysicalDisplayEventReceiver extends DisplayEventReceiver {
        PhysicalDisplayEventReceiver(Looper looper) {
            super(looper, 0);
        }

        public void onHotplug(long timestampNanos, long physicalDisplayId, boolean connected) {
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                Log.i(LocalDisplayAdapter.TAG, "PhysicalDisplayEventReceiver onHotplug: physicalDisplayId=" + physicalDisplayId + " connected=" + connected);
                if (connected) {
                    LocalDisplayAdapter.this.tryConnectDisplayLocked(physicalDisplayId);
                } else {
                    LocalDisplayAdapter.this.tryDisconnectDisplayLocked(physicalDisplayId);
                }
            }
        }

        public void onConfigChanged(long timestampNanos, long physicalDisplayId, int physIndex) {
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                LocalDisplayDevice device = (LocalDisplayDevice) LocalDisplayAdapter.this.mDevices.get(physicalDisplayId);
                if (device != null) {
                    device.onActivePhysicalDisplayModeChangedLocked(physIndex);
                }
            }
        }

        public void dispose() {
            LocalDisplayAdapter.super.dispose();
            Log.i(LocalDisplayAdapter.TAG, "PhysicalDisplayEventReceiver dispose()");
        }
    }
}
