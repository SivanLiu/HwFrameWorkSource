package com.android.server.display;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.sidekick.SidekickInternal;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.Display.HdrCapabilities;
import android.view.Display.Mode;
import android.view.DisplayCutout;
import android.view.DisplayEventReceiver;
import android.view.SurfaceControl;
import android.view.SurfaceControl.PhysicalDisplayInfo;
import com.android.server.LocalServices;
import com.android.server.display.DisplayAdapter.Listener;
import com.android.server.display.DisplayManagerService.SyncRoot;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class LocalDisplayAdapter extends DisplayAdapter {
    private static final int[] BUILT_IN_DISPLAY_IDS_TO_SCAN = new int[]{0, 1};
    private static final boolean DEBUG = false;
    private static final int DEFAULT_DENSITYDPI = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
    private static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
    private static final int PAD_DISPLAY_ID = 100000;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    private static final String TAG = "LocalDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "local:";
    private static final boolean isChinaArea = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private int defaultNaviMode = 0;
    private int mButtonLightMode = 1;
    private boolean mDeviceProvisioned = true;
    private final SparseArray<LocalDisplayDevice> mDevices = new SparseArray();
    private HotplugDisplayEventReceiver mHotplugReceiver;
    private ContentResolver mResolver;
    private SettingsObserver mSettingsObserver;
    private int mTrikeyNaviMode = -1;

    private static final class DisplayModeRecord {
        public final Mode mMode;

        public DisplayModeRecord(PhysicalDisplayInfo phys) {
            this.mMode = DisplayAdapter.createMode(phys.width, phys.height, phys.refreshRate);
        }

        public boolean hasMatchingMode(PhysicalDisplayInfo info) {
            return this.mMode.getPhysicalWidth() == info.width && this.mMode.getPhysicalHeight() == info.height && Float.floatToIntBits(this.mMode.getRefreshRate()) == Float.floatToIntBits(info.refreshRate);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DisplayModeRecord{mMode=");
            stringBuilder.append(this.mMode);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private final class HotplugDisplayEventReceiver extends DisplayEventReceiver {
        public HotplugDisplayEventReceiver(Looper looper) {
            super(looper, 0);
        }

        public void onHotplug(long timestampNanos, int builtInDisplayId, boolean connected) {
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                if (connected) {
                    LocalDisplayAdapter.this.tryConnectDisplayLocked(builtInDisplayId);
                } else {
                    LocalDisplayAdapter.this.tryDisconnectDisplayLocked(builtInDisplayId);
                }
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            registerContentObserver(UserHandle.myUserId());
            boolean z = false;
            if (Secure.getIntForUser(LocalDisplayAdapter.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0) {
                z = true;
            }
            LocalDisplayAdapter.this.mDeviceProvisioned = z;
            LocalDisplayAdapter.this.mTrikeyNaviMode = System.getIntForUser(LocalDisplayAdapter.this.mResolver, "swap_key_position", LocalDisplayAdapter.this.defaultNaviMode, ActivityManager.getCurrentUser());
            LocalDisplayAdapter.this.mButtonLightMode = System.getIntForUser(LocalDisplayAdapter.this.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
        }

        public void registerContentObserver(int userId) {
            LocalDisplayAdapter.this.mResolver.registerContentObserver(System.getUriFor("device_provisioned"), false, this, userId);
            LocalDisplayAdapter.this.mResolver.registerContentObserver(System.getUriFor("swap_key_position"), false, this, userId);
            LocalDisplayAdapter.this.mResolver.registerContentObserver(System.getUriFor("button_light_mode"), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            LocalDisplayAdapter localDisplayAdapter = LocalDisplayAdapter.this;
            boolean z = false;
            if (Secure.getIntForUser(LocalDisplayAdapter.this.mResolver, "device_provisioned", 0, ActivityManager.getCurrentUser()) != 0) {
                z = true;
            }
            localDisplayAdapter.mDeviceProvisioned = z;
            LocalDisplayAdapter.this.mTrikeyNaviMode = System.getIntForUser(LocalDisplayAdapter.this.mResolver, "swap_key_position", LocalDisplayAdapter.this.defaultNaviMode, ActivityManager.getCurrentUser());
            LocalDisplayAdapter.this.mButtonLightMode = System.getIntForUser(LocalDisplayAdapter.this.mResolver, "button_light_mode", 1, ActivityManager.getCurrentUser());
            String str = LocalDisplayAdapter.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mTrikeyNaviMode:");
            stringBuilder.append(LocalDisplayAdapter.this.mTrikeyNaviMode);
            stringBuilder.append(" mButtonLightMode:");
            stringBuilder.append(LocalDisplayAdapter.this.mButtonLightMode);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private class UserSwtichReceiver extends BroadcastReceiver {
        private UserSwtichReceiver() {
        }

        /* JADX WARNING: Missing block: B:9:0x0051, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            if (!(intent == null || intent.getAction() == null || !"android.intent.action.USER_SWITCHED".equals(intent.getAction()))) {
                int newUserId = intent.getIntExtra("android.intent.extra.user_handle", UserHandle.myUserId());
                String str = LocalDisplayAdapter.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UserSwtichReceiver:");
                stringBuilder.append(newUserId);
                Slog.i(str, stringBuilder.toString());
                if (LocalDisplayAdapter.this.mSettingsObserver != null) {
                    LocalDisplayAdapter.this.mSettingsObserver.registerContentObserver(newUserId);
                    LocalDisplayAdapter.this.mSettingsObserver.onChange(true);
                }
            }
        }
    }

    private final class LocalDisplayDevice extends DisplayDevice {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private int mActiveColorMode;
        private boolean mActiveColorModeInvalid;
        private int mActiveModeId;
        private boolean mActiveModeInvalid;
        private int mActivePhysIndex;
        private final Light mBacklight;
        private int mBrightness = -1;
        private final int mBuiltInDisplayId;
        private final Light mButtonlight;
        private int mDefaultModeId;
        private PhysicalDisplayInfo[] mDisplayInfos;
        private boolean mHavePendingChanges;
        private HdrCapabilities mHdrCapabilities;
        private DisplayDeviceInfo mInfo;
        public boolean mRogChange = false;
        private boolean mSidekickActive;
        private SidekickInternal mSidekickInternal;
        private int mState = 0;
        private final ArrayList<Integer> mSupportedColorModes = new ArrayList();
        private final SparseArray<DisplayModeRecord> mSupportedModes = new SparseArray();

        static {
            Class cls = LocalDisplayAdapter.class;
        }

        public LocalDisplayDevice(IBinder displayToken, int builtInDisplayId, PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo, int[] colorModes, int activeColorMode) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(LocalDisplayAdapter.UNIQUE_ID_PREFIX);
            stringBuilder.append(builtInDisplayId);
            super(LocalDisplayAdapter.this, displayToken, stringBuilder.toString());
            this.mBuiltInDisplayId = builtInDisplayId;
            updatePhysicalDisplayInfoLocked(physicalDisplayInfos, activeDisplayInfo, colorModes, activeColorMode);
            updateColorModesLocked(colorModes, activeColorMode);
            this.mSidekickInternal = (SidekickInternal) LocalServices.getService(SidekickInternal.class);
            if (this.mBuiltInDisplayId == 0) {
                LightsManager lights = (LightsManager) LocalServices.getService(LightsManager.class);
                this.mBacklight = lights.getLight(0);
                if (LocalDisplayAdapter.FRONT_FINGERPRINT_NAVIGATION && LocalDisplayAdapter.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                    this.mButtonlight = lights.getLight(2);
                } else {
                    this.mButtonlight = null;
                }
            } else {
                this.mBacklight = null;
                this.mButtonlight = null;
            }
            this.mHdrCapabilities = SurfaceControl.getHdrCapabilities(displayToken);
        }

        public boolean hasStableUniqueId() {
            return true;
        }

        public void updateDesityforRog() {
            this.mHavePendingChanges = true;
            this.mRogChange = true;
        }

        public boolean updatePhysicalDisplayInfoLocked(PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo, int[] colorModes, int activeColorMode) {
            DisplayModeRecord record;
            this.mDisplayInfos = (PhysicalDisplayInfo[]) Arrays.copyOf(physicalDisplayInfos, physicalDisplayInfos.length);
            this.mActivePhysIndex = activeDisplayInfo;
            ArrayList<DisplayModeRecord> records = new ArrayList();
            boolean modesAdded = false;
            for (PhysicalDisplayInfo info : physicalDisplayInfos) {
                boolean existingMode = false;
                for (int j = 0; j < records.size(); j++) {
                    if (((DisplayModeRecord) records.get(j)).hasMatchingMode(info)) {
                        existingMode = true;
                        break;
                    }
                }
                if (!existingMode) {
                    record = findDisplayModeRecord(info);
                    if (record == null) {
                        record = new DisplayModeRecord(info);
                        modesAdded = true;
                    }
                    records.add(record);
                }
            }
            DisplayModeRecord activeRecord = null;
            for (int i = 0; i < records.size(); i++) {
                DisplayModeRecord record2 = (DisplayModeRecord) records.get(i);
                if (record2.hasMatchingMode(physicalDisplayInfos[activeDisplayInfo])) {
                    activeRecord = record2;
                    break;
                }
            }
            if (!(this.mActiveModeId == 0 || this.mActiveModeId == activeRecord.mMode.getModeId())) {
                this.mActiveModeInvalid = true;
                LocalDisplayAdapter.this.sendTraversalRequestLocked();
            }
            boolean recordsChanged = records.size() != this.mSupportedModes.size() || modesAdded;
            if (!recordsChanged) {
                return false;
            }
            this.mHavePendingChanges = true;
            this.mSupportedModes.clear();
            Iterator it = records.iterator();
            while (it.hasNext()) {
                record = (DisplayModeRecord) it.next();
                this.mSupportedModes.put(record.mMode.getModeId(), record);
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
            LocalDisplayAdapter.this.sendTraversalRequestLocked();
            return true;
        }

        private boolean updateColorModesLocked(int[] colorModes, int activeColorMode) {
            List<Integer> pendingColorModes = new ArrayList();
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
            boolean colorModesChanged = pendingColorModes.size() != this.mSupportedColorModes.size() || colorModesAdded;
            if (!colorModesChanged) {
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
                } else if (this.mSupportedColorModes.isEmpty()) {
                    Slog.e(LocalDisplayAdapter.TAG, "No color modes available!");
                } else {
                    Slog.e(LocalDisplayAdapter.TAG, "Default and active color mode is no longer available! Reverting to first available mode.");
                    this.mActiveColorMode = ((Integer) this.mSupportedColorModes.get(0)).intValue();
                    this.mActiveColorModeInvalid = true;
                }
            }
            return true;
        }

        private DisplayModeRecord findDisplayModeRecord(PhysicalDisplayInfo info) {
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                DisplayModeRecord record = (DisplayModeRecord) this.mSupportedModes.valueAt(i);
                if (record.hasMatchingMode(info)) {
                    return record;
                }
            }
            return null;
        }

        public void applyPendingDisplayDeviceInfoChangesLocked() {
            if (this.mHavePendingChanges) {
                this.mInfo = null;
                this.mHavePendingChanges = false;
            }
        }

        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                int i;
                PhysicalDisplayInfo phys = this.mDisplayInfos[this.mActivePhysIndex];
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.width = phys.width;
                this.mInfo.height = phys.height;
                this.mInfo.modeId = this.mActiveModeId;
                this.mInfo.defaultModeId = this.mDefaultModeId;
                this.mInfo.supportedModes = new Mode[this.mSupportedModes.size()];
                for (i = 0; i < this.mSupportedModes.size(); i++) {
                    this.mInfo.supportedModes[i] = ((DisplayModeRecord) this.mSupportedModes.valueAt(i)).mMode;
                }
                this.mInfo.colorMode = this.mActiveColorMode;
                this.mInfo.supportedColorModes = new int[this.mSupportedColorModes.size()];
                for (i = 0; i < this.mSupportedColorModes.size(); i++) {
                    this.mInfo.supportedColorModes[i] = ((Integer) this.mSupportedColorModes.get(i)).intValue();
                }
                this.mInfo.hdrCapabilities = this.mHdrCapabilities;
                this.mInfo.appVsyncOffsetNanos = phys.appVsyncOffsetNanos;
                this.mInfo.presentationDeadlineNanos = phys.presentationDeadlineNanos;
                this.mInfo.state = this.mState;
                this.mInfo.uniqueId = getUniqueId();
                if (phys.secure) {
                    this.mInfo.flags = 12;
                }
                Resources res = LocalDisplayAdapter.this.getOverlayContext().getResources();
                DisplayDeviceInfo displayDeviceInfo;
                DisplayDeviceInfo displayDeviceInfo2;
                if (this.mBuiltInDisplayId == 0) {
                    this.mInfo.name = res.getString(17039943);
                    displayDeviceInfo = this.mInfo;
                    displayDeviceInfo.flags = 3 | displayDeviceInfo.flags;
                    if (res.getBoolean(17956992) || (Build.IS_EMULATOR && SystemProperties.getBoolean(LocalDisplayAdapter.PROPERTY_EMULATOR_CIRCULAR, false))) {
                        displayDeviceInfo2 = this.mInfo;
                        displayDeviceInfo2.flags |= 256;
                    }
                    int width = SystemProperties.getInt("persist.sys.rog.width", this.mInfo.width);
                    int height = SystemProperties.getInt("persist.sys.rog.height", this.mInfo.height);
                    this.mInfo.displayCutout = DisplayCutout.fromResources(res, width, height);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getDisplayDeviceInfoLocked called,");
                    stringBuilder.append(width);
                    stringBuilder.append("x");
                    stringBuilder.append(height);
                    stringBuilder.append(", ");
                    stringBuilder.append(this.mInfo.width);
                    stringBuilder.append("x");
                    stringBuilder.append(this.mInfo.height);
                    stringBuilder.append("displayCutout ");
                    stringBuilder.append(this.mInfo.displayCutout);
                    Slog.v("TAG", stringBuilder.toString());
                    this.mInfo.type = 1;
                    this.mInfo.densityDpi = (int) ((phys.density * 160.0f) + 0.5f);
                    this.mInfo.xDpi = phys.xDpi;
                    this.mInfo.yDpi = phys.yDpi;
                    this.mInfo.touch = 1;
                } else if (this.mBuiltInDisplayId == LocalDisplayAdapter.PAD_DISPLAY_ID) {
                    this.mInfo.name = "HUAWEI PAD PC Display";
                    displayDeviceInfo2 = this.mInfo;
                    displayDeviceInfo2.flags |= 2;
                    this.mInfo.type = 2;
                    this.mInfo.touch = 1;
                    this.mInfo.densityDpi = LocalDisplayAdapter.DEFAULT_DENSITYDPI == 0 ? (int) ((phys.density * 160.0f) + 0.5f) : LocalDisplayAdapter.DEFAULT_DENSITYDPI;
                    String str = LocalDisplayAdapter.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("PAD_DISPLAY_ID densityDpi:");
                    stringBuilder2.append(this.mInfo.densityDpi);
                    HwPCUtils.log(str, stringBuilder2.toString());
                    this.mInfo.xDpi = phys.xDpi;
                    this.mInfo.yDpi = phys.yDpi;
                    str = LocalDisplayAdapter.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("PAD_DISPLAY_ID mInfo.xDpi:");
                    stringBuilder2.append(this.mInfo.xDpi);
                    stringBuilder2.append(",mInfo.yDpi:");
                    stringBuilder2.append(this.mInfo.yDpi);
                    HwPCUtils.log(str, stringBuilder2.toString());
                } else {
                    this.mInfo.displayCutout = null;
                    this.mInfo.type = 2;
                    displayDeviceInfo = this.mInfo;
                    displayDeviceInfo.flags |= 64;
                    this.mInfo.name = LocalDisplayAdapter.this.getContext().getResources().getString(17039944);
                    this.mInfo.touch = 2;
                    if (HwPCUtils.enabled()) {
                        this.mInfo.densityDpi = (int) ((phys.density * 160.0f) + 0.5f);
                        String str2 = LocalDisplayAdapter.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("densityDpi:");
                        stringBuilder3.append(this.mInfo.densityDpi);
                        HwPCUtils.log(str2, stringBuilder3.toString());
                        this.mInfo.xDpi = (float) this.mInfo.densityDpi;
                        this.mInfo.yDpi = (float) this.mInfo.densityDpi;
                    } else {
                        this.mInfo.setAssumedDensityForExternalDisplay(phys.width, phys.height);
                    }
                    if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
                        this.mInfo.rotation = 3;
                    }
                    if (SystemProperties.getBoolean("persist.demo.hdmirotates", false)) {
                        displayDeviceInfo2 = this.mInfo;
                        displayDeviceInfo2.flags |= 2;
                    }
                    if (!res.getBoolean(17956988)) {
                        displayDeviceInfo2 = this.mInfo;
                        displayDeviceInfo2.flags |= 128;
                    }
                    if (res.getBoolean(17956989)) {
                        displayDeviceInfo2 = this.mInfo;
                        displayDeviceInfo2.flags |= 16;
                    }
                }
            }
            return this.mInfo;
        }

        public Runnable requestDisplayStateLocked(int state, int brightness) {
            int i = state;
            int i2 = brightness;
            boolean z = false;
            boolean stateChanged = this.mState != i;
            if (!(this.mBrightness == i2 || this.mBacklight == null)) {
                z = true;
            }
            boolean brightnessChanged = z;
            if (!stateChanged && !brightnessChanged) {
                return null;
            }
            int displayId = this.mBuiltInDisplayId;
            IBinder token = getDisplayTokenLocked();
            int oldState = this.mState;
            if (stateChanged) {
                this.mState = i;
                updateDeviceInfoLocked();
            }
            if (brightnessChanged) {
                this.mBrightness = i2;
            }
            final int i3 = oldState;
            final int i4 = i;
            final boolean z2 = brightnessChanged;
            final int i5 = i2;
            final int i6 = displayId;
            final IBinder iBinder = token;
            return new Runnable() {
                public void run() {
                    int currentState = i3;
                    if (Display.isSuspendedState(i3) || i3 == 0) {
                        if (!Display.isSuspendedState(i4)) {
                            setDisplayState(i4);
                            currentState = i4;
                        } else if (i4 == 4 || i3 == 4) {
                            setDisplayState(3);
                            currentState = 3;
                        } else if (i4 == 6 || i3 == 6) {
                            setDisplayState(2);
                            currentState = 2;
                        } else {
                            return;
                        }
                    }
                    boolean vrModeChange = false;
                    if ((i4 == 5 || currentState == 5) && currentState != i4) {
                        setVrMode(i4 == 5);
                        vrModeChange = true;
                    }
                    if (z2 || vrModeChange) {
                        setDisplayBrightness(i5);
                    }
                    if (i4 != currentState) {
                        setDisplayState(i4);
                    }
                }

                private void setVrMode(boolean isVrEnabled) {
                    String str = LocalDisplayAdapter.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setVrMode(id=");
                    stringBuilder.append(i6);
                    stringBuilder.append(", state=");
                    stringBuilder.append(Display.stateToString(i4));
                    stringBuilder.append(")");
                    Slog.d(str, stringBuilder.toString());
                    if (LocalDisplayDevice.this.mBacklight != null) {
                        LocalDisplayDevice.this.mBacklight.setVrMode(isVrEnabled);
                    }
                }

                private void setDisplayState(int state) {
                    if (LocalDisplayDevice.this.mSidekickActive) {
                        Trace.traceBegin(131072, "SidekickInternal#endDisplayControl");
                        try {
                            LocalDisplayDevice.this.mSidekickInternal.endDisplayControl();
                            LocalDisplayDevice.this.mSidekickActive = false;
                        } finally {
                            Trace.traceEnd(131072);
                        }
                    }
                    int mode = LocalDisplayAdapter.getPowerModeForState(state);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setDisplayState(id=");
                    stringBuilder.append(i6);
                    stringBuilder.append(", state=");
                    stringBuilder.append(Display.stateToString(state));
                    stringBuilder.append(")");
                    Trace.traceBegin(131072, stringBuilder.toString());
                    try {
                        SurfaceControl.setDisplayPowerMode(iBinder, mode);
                        Trace.traceCounter(131072, "DisplayPowerMode", mode);
                        if (Display.isSuspendedState(state) && state != 1 && LocalDisplayDevice.this.mSidekickInternal != null && !LocalDisplayDevice.this.mSidekickActive) {
                            Trace.traceBegin(131072, "SidekickInternal#startDisplayControl");
                            try {
                                LocalDisplayDevice.this.mSidekickActive = LocalDisplayDevice.this.mSidekickInternal.startDisplayControl(state);
                            } finally {
                                Trace.traceEnd(131072);
                            }
                        }
                    } finally {
                        Trace.traceEnd(131072);
                    }
                }

                private void setDisplayBrightness(int brightness) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setDisplayBrightness(id=");
                    stringBuilder.append(i6);
                    stringBuilder.append(", brightness=");
                    stringBuilder.append(brightness);
                    stringBuilder.append(")");
                    Trace.traceBegin(131072, stringBuilder.toString());
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

        private void updateButtonBrightness(int brightness) {
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

        public void requestDisplayModesLocked(int colorMode, int modeId) {
            if (requestModeLocked(modeId) || requestColorModeLocked(colorMode)) {
                updateDeviceInfoLocked();
            }
        }

        public void onOverlayChangedLocked() {
            updateDeviceInfoLocked();
        }

        public boolean requestModeLocked(int modeId) {
            if (modeId == 0) {
                modeId = this.mDefaultModeId;
            } else if (this.mSupportedModes.indexOfKey(modeId) < 0) {
                String str = LocalDisplayAdapter.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Requested mode ");
                stringBuilder.append(modeId);
                stringBuilder.append(" is not supported by this display, reverting to default display mode.");
                Slog.w(str, stringBuilder.toString());
                modeId = this.mDefaultModeId;
            }
            int physIndex = findDisplayInfoIndexLocked(modeId);
            if (physIndex < 0) {
                String str2 = LocalDisplayAdapter.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Requested mode ID ");
                stringBuilder2.append(modeId);
                stringBuilder2.append(" not available, trying with default mode ID");
                Slog.w(str2, stringBuilder2.toString());
                modeId = this.mDefaultModeId;
                physIndex = findDisplayInfoIndexLocked(modeId);
            }
            if (this.mActivePhysIndex == physIndex) {
                return false;
            }
            SurfaceControl.setActiveConfig(getDisplayTokenLocked(), physIndex);
            this.mActivePhysIndex = physIndex;
            this.mActiveModeId = modeId;
            this.mActiveModeInvalid = false;
            return true;
        }

        public boolean requestColorModeLocked(int colorMode) {
            if (this.mActiveColorMode == colorMode) {
                return false;
            }
            if (this.mSupportedColorModes.contains(Integer.valueOf(colorMode))) {
                SurfaceControl.setActiveColorMode(getDisplayTokenLocked(), colorMode);
                this.mActiveColorMode = colorMode;
                this.mActiveColorModeInvalid = false;
                return true;
            }
            String str = LocalDisplayAdapter.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to find color mode ");
            stringBuilder.append(colorMode);
            stringBuilder.append(", ignoring request.");
            Slog.w(str, stringBuilder.toString());
            return false;
        }

        public void dumpLocked(PrintWriter pw) {
            int i;
            StringBuilder stringBuilder;
            super.dumpLocked(pw);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mBuiltInDisplayId=");
            stringBuilder2.append(this.mBuiltInDisplayId);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mActivePhysIndex=");
            stringBuilder2.append(this.mActivePhysIndex);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mActiveModeId=");
            stringBuilder2.append(this.mActiveModeId);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mActiveColorMode=");
            stringBuilder2.append(this.mActiveColorMode);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mState=");
            stringBuilder2.append(Display.stateToString(this.mState));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mBrightness=");
            stringBuilder2.append(this.mBrightness);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mBacklight=");
            stringBuilder2.append(this.mBacklight);
            pw.println(stringBuilder2.toString());
            pw.println("mDisplayInfos=");
            int i2 = 0;
            for (Object append : this.mDisplayInfos) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(append);
                pw.println(stringBuilder.toString());
            }
            pw.println("mSupportedModes=");
            for (i = 0; i < this.mSupportedModes.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mSupportedModes.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.print("mSupportedColorModes=[");
            while (i2 < this.mSupportedColorModes.size()) {
                if (i2 != 0) {
                    pw.print(", ");
                }
                pw.print(this.mSupportedColorModes.get(i2));
                i2++;
            }
            pw.println("]");
        }

        private int findDisplayInfoIndexLocked(int modeId) {
            DisplayModeRecord record = (DisplayModeRecord) this.mSupportedModes.get(modeId);
            if (record != null) {
                for (int i = 0; i < this.mDisplayInfos.length; i++) {
                    if (record.hasMatchingMode(this.mDisplayInfos[i])) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private void updateDeviceInfoLocked() {
            this.mInfo = null;
            LocalDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
        }
    }

    public LocalDisplayAdapter(SyncRoot syncRoot, Context context, Handler handler, Listener listener) {
        super(syncRoot, context, handler, listener, TAG);
    }

    public void registerLocked() {
        super.registerLocked();
        this.mHotplugReceiver = new HotplugDisplayEventReceiver(getHandler().getLooper());
        for (int builtInDisplayId : BUILT_IN_DISPLAY_IDS_TO_SCAN) {
            tryConnectDisplayLocked(builtInDisplayId);
        }
        if (HwPCUtils.enabledInPad()) {
            HwPCUtils.log(TAG, "tryConnectPadVirtualDisplayLocked");
            tryConnectDisplayLocked(PAD_DISPLAY_ID);
        }
    }

    private void tryConnectDisplayLocked(int builtInDisplayId) {
        int i = builtInDisplayId;
        IBinder displayToken = SurfaceControl.getBuiltInDisplay(builtInDisplayId);
        if (i == PAD_DISPLAY_ID) {
            displayToken = SurfaceControl.getBuiltInDisplay(0);
        }
        IBinder displayToken2 = displayToken;
        if (displayToken2 != null) {
            PhysicalDisplayInfo[] configs = SurfaceControl.getDisplayConfigs(displayToken2);
            String str;
            StringBuilder stringBuilder;
            if (configs == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("No valid configs found for display device ");
                stringBuilder.append(i);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            int activeConfig = SurfaceControl.getActiveConfig(displayToken2);
            if (activeConfig < 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("No active config found for display device ");
                stringBuilder.append(i);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            int activeColorMode = SurfaceControl.getActiveColorMode(displayToken2);
            if (activeColorMode < 0) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to get active color mode for display device ");
                stringBuilder2.append(i);
                Slog.w(str2, stringBuilder2.toString());
                activeColorMode = -1;
            }
            int activeColorMode2 = activeColorMode;
            int[] colorModes = SurfaceControl.getDisplayColorModes(displayToken2);
            LocalDisplayDevice device = (LocalDisplayDevice) this.mDevices.get(i);
            if (device == null) {
                LocalDisplayDevice device2 = new LocalDisplayDevice(displayToken2, i, configs, activeConfig, colorModes, activeColorMode2);
                this.mDevices.put(i, device2);
                sendDisplayDeviceEventLocked(device2, 1);
            } else if (device.updatePhysicalDisplayInfoLocked(configs, activeConfig, colorModes, activeColorMode2)) {
                sendDisplayDeviceEventLocked(device, 2);
            }
        }
    }

    private void tryDisconnectDisplayLocked(int builtInDisplayId) {
        LocalDisplayDevice device = (LocalDisplayDevice) this.mDevices.get(builtInDisplayId);
        if (device != null) {
            this.mDevices.remove(builtInDisplayId);
            sendDisplayDeviceEventLocked(device, 3);
        }
    }

    static int getPowerModeForState(int state) {
        if (state == 1) {
            return 0;
        }
        if (state == 6) {
            return 4;
        }
        switch (state) {
            case 3:
                return 1;
            case 4:
                return 3;
            default:
                return 2;
        }
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("registerContentObserver:");
                stringBuilder.append(exp.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    private boolean isButtonLightTimeout() {
        return SystemProperties.getBoolean("sys.button.light.timeout", false);
    }

    private void setButtonLightTimeout(boolean timeout) {
        SystemProperties.set("sys.button.light.timeout", String.valueOf(timeout));
    }

    Context getOverlayContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    public void pcDisplayChangeService(boolean connected) {
        if (HwPCUtils.enabledInPad()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pcDisplayChangeService connected = ");
            stringBuilder.append(connected);
            Slog.w(str, stringBuilder.toString());
            synchronized (getSyncRoot()) {
                if (!connected) {
                    Slog.w(TAG, "pcDisplayChangeService tryDisconnectDisplayLocked");
                    tryDisconnectDisplayLocked(PAD_DISPLAY_ID);
                } else if (((LocalDisplayDevice) this.mDevices.get(PAD_DISPLAY_ID)) == null) {
                    Slog.w(TAG, "pcDisplayChangeService tryDisconnectDisplayLocked");
                    tryConnectDisplayLocked(PAD_DISPLAY_ID);
                }
            }
        }
    }
}
