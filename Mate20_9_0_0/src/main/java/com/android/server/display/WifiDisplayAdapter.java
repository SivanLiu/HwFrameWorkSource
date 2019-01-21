package com.android.server.display;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplaySessionInfo;
import android.hardware.display.WifiDisplayStatus;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Slog;
import android.view.Display.Mode;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.HwServiceFactory;
import com.android.server.display.DisplayManagerService.SyncRoot;
import com.android.server.display.WifiDisplayController.Listener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class WifiDisplayAdapter extends DisplayAdapter implements IWifiDisplayAdapterInner {
    private static final String ACTION_DISCONNECT = "android.server.display.wfd.DISCONNECT";
    private static final String ACTION_WIFI_DISPLAY_CASTING = "com.huawei.hardware.display.action.WIFI_DISPLAY_CASTING";
    private static final boolean DEBUG = false;
    private static final String DISPLAY_NAME_PREFIX = "wifi:";
    private static final int MSG_SEND_CAST_CHANGE_BROADCAST = 2;
    private static final int MSG_SEND_STATUS_CHANGE_BROADCAST = 1;
    private static final String TAG = "WifiDisplayAdapter";
    private static final String WIFI_DISPLAY_CASTING_PERMISSION = "com.huawei.wfd.permission.ACCESS_WIFI_DISPLAY_CASTING";
    private static final String WIFI_DISPLAY_UIBC_INFO = "com.huawei.hardware.display.action.WIFI_DISPLAY_UIBC_INFO";
    private WifiDisplay mActiveDisplay;
    private int mActiveDisplayState;
    private WifiDisplay[] mAvailableDisplays = WifiDisplay.EMPTY_ARRAY;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiDisplayAdapter.ACTION_DISCONNECT)) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplayAdapter.this.requestDisconnectLocked();
                }
            }
        }
    };
    private int mConnectionFailedReason = -1;
    private WifiDisplayStatus mCurrentStatus;
    private WifiDisplayController mDisplayController;
    private WifiDisplayDevice mDisplayDevice;
    private WifiDisplay[] mDisplays = WifiDisplay.EMPTY_ARRAY;
    private int mFeatureState;
    private final WifiDisplayHandler mHandler;
    IHwWifiDisplayAdapterEx mHwAdapterEx = null;
    private boolean mPendingStatusChangeBroadcast;
    private final PersistentDataStore mPersistentDataStore;
    private WifiDisplay[] mRememberedDisplays = WifiDisplay.EMPTY_ARRAY;
    private int mScanState;
    private WifiDisplaySessionInfo mSessionInfo;
    private final boolean mSupportsProtectedBuffers;
    private int mUibcCap = 0;
    private final Listener mWifiDisplayListener = new Listener() {
        public void onFeatureStateChanged(int featureState) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                if (WifiDisplayAdapter.this.mFeatureState != featureState) {
                    WifiDisplayAdapter.this.mFeatureState = featureState;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onScanStarted() {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                if (WifiDisplayAdapter.this.mScanState != 1) {
                    WifiDisplayAdapter.this.mScanState = 1;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onScanResults(WifiDisplay[] availableDisplays) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                availableDisplays = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAliases(availableDisplays);
                boolean changed = Arrays.equals(WifiDisplayAdapter.this.mAvailableDisplays, availableDisplays) ^ true;
                int i = 0;
                while (!changed && i < availableDisplays.length) {
                    changed = availableDisplays[i].canConnect() != WifiDisplayAdapter.this.mAvailableDisplays[i].canConnect();
                    i++;
                }
                if (changed) {
                    WifiDisplayAdapter.this.mAvailableDisplays = availableDisplays;
                    WifiDisplayAdapter.this.fixRememberedDisplayNamesFromAvailableDisplaysLocked();
                    WifiDisplayAdapter.this.updateDisplaysLocked();
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onScanFinished() {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                if (WifiDisplayAdapter.this.mScanState != 0) {
                    WifiDisplayAdapter.this.mScanState = 0;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onDisplayConnecting(WifiDisplay display) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                display = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                if (!(WifiDisplayAdapter.this.mActiveDisplayState == 1 && WifiDisplayAdapter.this.mActiveDisplay != null && WifiDisplayAdapter.this.mActiveDisplay.equals(display))) {
                    WifiDisplayAdapter.this.mActiveDisplayState = 1;
                    WifiDisplayAdapter.this.mActiveDisplay = display;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onDisplayConnectionFailed() {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                if (!(WifiDisplayAdapter.this.mActiveDisplayState == 0 && WifiDisplayAdapter.this.mActiveDisplay == null)) {
                    WifiDisplayAdapter.this.mActiveDisplayState = 0;
                    WifiDisplayAdapter.this.mActiveDisplay = null;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onDisplayConnected(WifiDisplay display, Surface surface, int width, int height, int flags) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                display = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayRemembered(WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display));
                if ((flags & 256) != 0) {
                    WifiDisplayAdapter.this.mPersistentDataStore.addHdcpSupportedDevice(display.getDeviceAddress());
                }
                WifiDisplayAdapter.this.addDisplayDeviceLocked(display, surface, width, height, flags);
                if (!(WifiDisplayAdapter.this.mActiveDisplayState == 2 && WifiDisplayAdapter.this.mActiveDisplay != null && WifiDisplayAdapter.this.mActiveDisplay.equals(display))) {
                    WifiDisplayAdapter.this.mActiveDisplayState = 2;
                    WifiDisplayAdapter.this.mActiveDisplay = display;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onDisplaySessionInfo(WifiDisplaySessionInfo sessionInfo) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                WifiDisplayAdapter.this.mSessionInfo = sessionInfo;
                WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
            }
        }

        public void onDisplayChanged(WifiDisplay display) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                display = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                if (!(WifiDisplayAdapter.this.mActiveDisplay == null || !WifiDisplayAdapter.this.mActiveDisplay.hasSameAddress(display) || WifiDisplayAdapter.this.mActiveDisplay.equals(display))) {
                    WifiDisplayAdapter.this.mActiveDisplay = display;
                    WifiDisplayAdapter.this.renameDisplayDeviceLocked(display.getFriendlyDisplayName());
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onDisplayDisconnected() {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                WifiDisplayAdapter.this.removeDisplayDeviceLocked();
                if (!(WifiDisplayAdapter.this.mActiveDisplayState == 0 && WifiDisplayAdapter.this.mActiveDisplay == null)) {
                    WifiDisplayAdapter.this.mActiveDisplayState = 0;
                    WifiDisplayAdapter.this.mActiveDisplay = null;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }
        }

        public void onSetConnectionFailedReason(int reason) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                String str = WifiDisplayAdapter.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onSetConnectionFailedReason, reason=");
                stringBuilder.append(reason);
                Slog.d(str, stringBuilder.toString());
                WifiDisplayAdapter.this.mConnectionFailedReason = reason;
            }
        }

        public void onDisplayCasting(WifiDisplay display) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                display = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                if (WifiDisplayAdapter.this.mActiveDisplayState == 2 && WifiDisplayAdapter.this.mActiveDisplay != null && WifiDisplayAdapter.this.mActiveDisplay.equals(display)) {
                    Slog.d(WifiDisplayAdapter.TAG, "onDisplayCasting .....");
                    WifiDisplayAdapter.this.mHandler.sendEmptyMessage(2);
                    WifiDisplayAdapter.this.LaunchMKForWifiMode();
                } else {
                    String str = WifiDisplayAdapter.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onDisplayCasting mActiveDisplayState ");
                    stringBuilder.append(WifiDisplayAdapter.this.mActiveDisplayState);
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }

        public void onSetUibcInfo(int capSupport) {
            synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                WifiDisplayAdapter.this.mUibcCap = capSupport;
            }
        }
    };

    private final class WifiDisplayHandler extends Handler {
        public WifiDisplayHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WifiDisplayAdapter.this.handleSendStatusChangeBroadcast();
                    return;
                case 2:
                    WifiDisplayAdapter.this.handleSendCastingBroadcast();
                    return;
                default:
                    return;
            }
        }
    }

    private final class WifiDisplayDevice extends DisplayDevice {
        private final String mAddress;
        private final int mFlags;
        private final int mHeight;
        private DisplayDeviceInfo mInfo;
        private final Mode mMode;
        private String mName;
        private final float mRefreshRate;
        private Surface mSurface;
        private final int mWidth;

        public WifiDisplayDevice(IBinder displayToken, String name, int width, int height, float refreshRate, int flags, String address, Surface surface) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(WifiDisplayAdapter.DISPLAY_NAME_PREFIX);
            stringBuilder.append(address);
            super(WifiDisplayAdapter.this, displayToken, stringBuilder.toString());
            this.mName = name;
            this.mWidth = width;
            this.mHeight = height;
            this.mRefreshRate = refreshRate;
            this.mFlags = flags;
            this.mAddress = address;
            this.mSurface = surface;
            this.mMode = DisplayAdapter.createMode(width, height, refreshRate);
        }

        public boolean hasStableUniqueId() {
            return true;
        }

        public void destroyLocked() {
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        public void setNameLocked(String name) {
            this.mName = name;
            this.mInfo = null;
        }

        public void performTraversalLocked(Transaction t) {
            if (this.mSurface != null) {
                setSurfaceLocked(t, this.mSurface);
            }
            Slog.w(WifiDisplayAdapter.TAG, "performTraversalInTransactionLocked: ");
            int vrLayerStackId;
            String str;
            StringBuilder stringBuilder;
            if (HwFrameworkFactory.getVRSystemServiceManager().isVRDisplayConnected()) {
                vrLayerStackId = HwVRUtils.getVRDisplayID();
                if (vrLayerStackId > 0) {
                    str = WifiDisplayAdapter.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("performTraversalInTransactionLocked: setDisplayLayerStack VR layer stack id:");
                    stringBuilder.append(vrLayerStackId);
                    Slog.i(str, stringBuilder.toString());
                    SurfaceControl.setDisplayLayerStack(getDisplayTokenLocked(), vrLayerStackId);
                    return;
                }
                SurfaceControl.setDisplayLayerStack(getDisplayTokenLocked(), 0);
            } else if (!HwPCUtils.enabledInPad()) {
            } else {
                if (HwPCUtils.isPcCastModeInServer()) {
                    vrLayerStackId = HwPCUtils.getPCDisplayID();
                    str = WifiDisplayAdapter.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("performTraversalInTransactionLocked: setDisplayLayerStack layerStack = ");
                    stringBuilder.append(vrLayerStackId);
                    Slog.d(str, stringBuilder.toString());
                    SurfaceControl.setDisplayLayerStack(getDisplayTokenLocked(), vrLayerStackId);
                    return;
                }
                SurfaceControl.setDisplayLayerStack(getDisplayTokenLocked(), 0);
            }
        }

        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = this.mWidth;
                this.mInfo.height = this.mHeight;
                this.mInfo.modeId = this.mMode.getModeId();
                this.mInfo.defaultModeId = this.mMode.getModeId();
                this.mInfo.supportedModes = new Mode[]{this.mMode};
                this.mInfo.presentationDeadlineNanos = 1000000000 / ((long) ((int) this.mRefreshRate));
                this.mInfo.flags = this.mFlags;
                this.mInfo.type = 3;
                this.mInfo.address = this.mAddress;
                this.mInfo.touch = 2;
                if (HwPCUtils.enabled()) {
                    this.mInfo.densityDpi = ((this.mWidth < this.mHeight ? this.mWidth : this.mHeight) * 240) / 1080;
                    String str = WifiDisplayAdapter.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PC mode densityDpi:");
                    stringBuilder.append(this.mInfo.densityDpi);
                    Slog.i(str, stringBuilder.toString());
                    this.mInfo.xDpi = (float) this.mInfo.densityDpi;
                    this.mInfo.yDpi = (float) this.mInfo.densityDpi;
                } else {
                    this.mInfo.setAssumedDensityForExternalDisplay(this.mWidth, this.mHeight);
                }
            }
            return this.mInfo;
        }
    }

    public WifiDisplayAdapter(SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, PersistentDataStore persistentDataStore) {
        super(syncRoot, context, handler, listener, TAG);
        this.mHandler = new WifiDisplayHandler(handler.getLooper());
        this.mPersistentDataStore = persistentDataStore;
        this.mSupportsProtectedBuffers = context.getResources().getBoolean(17957070);
        this.mHwAdapterEx = HwServiceFactory.getHwWifiDisplayAdapterEx(this);
    }

    public void dumpLocked(PrintWriter pw) {
        super.dumpLocked(pw);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentStatus=");
        stringBuilder.append(getWifiDisplayStatusLocked());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mFeatureState=");
        stringBuilder.append(this.mFeatureState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mScanState=");
        stringBuilder.append(this.mScanState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mActiveDisplayState=");
        stringBuilder.append(this.mActiveDisplayState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mActiveDisplay=");
        stringBuilder.append(this.mActiveDisplay);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDisplays=");
        stringBuilder.append(Arrays.toString(this.mDisplays));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAvailableDisplays=");
        stringBuilder.append(Arrays.toString(this.mAvailableDisplays));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mRememberedDisplays=");
        stringBuilder.append(Arrays.toString(this.mRememberedDisplays));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mPendingStatusChangeBroadcast=");
        stringBuilder.append(this.mPendingStatusChangeBroadcast);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mSupportsProtectedBuffers=");
        stringBuilder.append(this.mSupportsProtectedBuffers);
        pw.println(stringBuilder.toString());
        if (this.mDisplayController == null) {
            pw.println("mDisplayController=null");
            return;
        }
        pw.println("mDisplayController:");
        PrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.increaseIndent();
        DumpUtils.dumpAsync(getHandler(), this.mDisplayController, ipw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 200);
    }

    public void registerLocked() {
        super.registerLocked();
        updateRememberedDisplaysLocked();
        getHandler().post(new Runnable() {
            public void run() {
                WifiDisplayAdapter.this.mDisplayController = new WifiDisplayController(WifiDisplayAdapter.this.getContext(), WifiDisplayAdapter.this.getHandler(), WifiDisplayAdapter.this.mWifiDisplayListener);
                WifiDisplayAdapter.this.getContext().registerReceiverAsUser(WifiDisplayAdapter.this.mBroadcastReceiver, UserHandle.ALL, new IntentFilter(WifiDisplayAdapter.ACTION_DISCONNECT), null, WifiDisplayAdapter.this.mHandler);
            }
        });
    }

    public void requestStartScanLocked() {
        getHandler().post(new Runnable() {
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestStartScan();
                }
            }
        });
    }

    public void requestStopScanLocked() {
        getHandler().post(new Runnable() {
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestStopScan();
                }
            }
        });
    }

    public void requestConnectLocked(final String address) {
        getHandler().post(new Runnable() {
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestConnect(address);
                    if (WifiDisplayAdapter.this.mHwAdapterEx != null) {
                        WifiDisplayAdapter.this.mHwAdapterEx.setConnectParameters(address);
                    }
                }
            }
        });
    }

    public void requestPauseLocked() {
        getHandler().post(new Runnable() {
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestPause();
                }
            }
        });
    }

    public void requestResumeLocked() {
        getHandler().post(new Runnable() {
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestResume();
                }
            }
        });
    }

    public void requestDisconnectLocked() {
        getHandler().post(new Runnable() {
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestDisconnect();
                }
            }
        });
    }

    public void requestRenameLocked(String address, String alias) {
        if (alias != null) {
            alias = alias.trim();
            if (alias.isEmpty() || alias.equals(address)) {
                alias = null;
            }
        }
        WifiDisplay display = this.mPersistentDataStore.getRememberedWifiDisplay(address);
        if (!(display == null || Objects.equals(display.getDeviceAlias(), alias))) {
            if (this.mPersistentDataStore.rememberWifiDisplay(new WifiDisplay(address, display.getDeviceName(), alias, false, false, false))) {
                this.mPersistentDataStore.saveIfNeeded();
                updateRememberedDisplaysLocked();
                scheduleStatusChangedBroadcastLocked();
            }
        }
        if (this.mActiveDisplay != null && this.mActiveDisplay.getDeviceAddress().equals(address)) {
            renameDisplayDeviceLocked(this.mActiveDisplay.getFriendlyDisplayName());
        }
    }

    public void requestForgetLocked(String address) {
        if (this.mPersistentDataStore.forgetWifiDisplay(address)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        if (this.mActiveDisplay != null && this.mActiveDisplay.getDeviceAddress().equals(address)) {
            requestDisconnectLocked();
        }
    }

    public WifiDisplayStatus getWifiDisplayStatusLocked() {
        if (this.mCurrentStatus == null) {
            this.mCurrentStatus = new WifiDisplayStatus(this.mFeatureState, this.mScanState, this.mActiveDisplayState, this.mActiveDisplay, this.mDisplays, this.mSessionInfo);
        }
        return this.mCurrentStatus;
    }

    private void updateDisplaysLocked() {
        List<WifiDisplay> displays = new ArrayList(this.mAvailableDisplays.length + this.mRememberedDisplays.length);
        boolean[] remembered = new boolean[this.mAvailableDisplays.length];
        for (WifiDisplay d : this.mRememberedDisplays) {
            boolean available = false;
            for (int i = 0; i < this.mAvailableDisplays.length; i++) {
                if (d.equals(this.mAvailableDisplays[i])) {
                    available = true;
                    remembered[i] = true;
                    break;
                }
            }
            if (!available) {
                WifiDisplay wifiDisplay = r9;
                WifiDisplay wifiDisplay2 = new WifiDisplay(d.getDeviceAddress(), d.getDeviceName(), d.getDeviceAlias(), false, false, true);
                displays.add(wifiDisplay);
            }
        }
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < this.mAvailableDisplays.length) {
                WifiDisplay d2 = this.mAvailableDisplays[i3];
                displays.add(new WifiDisplay(d2.getDeviceAddress(), d2.getDeviceName(), d2.getDeviceAlias(), true, d2.canConnect(), remembered[i3]));
                i2 = i3 + 1;
            } else {
                this.mDisplays = (WifiDisplay[]) displays.toArray(WifiDisplay.EMPTY_ARRAY);
                return;
            }
        }
    }

    private void updateRememberedDisplaysLocked() {
        this.mRememberedDisplays = this.mPersistentDataStore.getRememberedWifiDisplays();
        this.mActiveDisplay = this.mPersistentDataStore.applyWifiDisplayAlias(this.mActiveDisplay);
        this.mAvailableDisplays = this.mPersistentDataStore.applyWifiDisplayAliases(this.mAvailableDisplays);
        updateDisplaysLocked();
    }

    private void fixRememberedDisplayNamesFromAvailableDisplaysLocked() {
        boolean changed = false;
        for (int i = 0; i < this.mRememberedDisplays.length; i++) {
            WifiDisplay rememberedDisplay = this.mRememberedDisplays[i];
            WifiDisplay availableDisplay = findAvailableDisplayLocked(rememberedDisplay.getDeviceAddress());
            if (!(availableDisplay == null || rememberedDisplay.equals(availableDisplay))) {
                this.mRememberedDisplays[i] = availableDisplay;
                changed |= this.mPersistentDataStore.rememberWifiDisplay(availableDisplay);
            }
        }
        if (changed) {
            this.mPersistentDataStore.saveIfNeeded();
        }
    }

    private WifiDisplay findAvailableDisplayLocked(String address) {
        for (WifiDisplay display : this.mAvailableDisplays) {
            if (display.getDeviceAddress().equals(address)) {
                return display;
            }
        }
        return null;
    }

    private void addDisplayDeviceLocked(WifiDisplay display, Surface surface, int width, int height, int flags) {
        removeDisplayDeviceLocked();
        if (this.mPersistentDataStore.rememberWifiDisplay(display)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        boolean secure = (flags & 1) != 0;
        int deviceFlags = 64;
        if (secure) {
            deviceFlags = 64 | 4;
            if (this.mSupportsProtectedBuffers) {
                deviceFlags |= 8;
            }
        }
        int deviceFlags2 = deviceFlags;
        String name = display.getFriendlyDisplayName();
        WifiDisplayDevice wifiDisplayDevice = r0;
        WifiDisplayDevice wifiDisplayDevice2 = new WifiDisplayDevice(SurfaceControl.createDisplay(name, secure), name, width, height, 60.0f, deviceFlags2, display.getDeviceAddress(), surface);
        this.mDisplayDevice = wifiDisplayDevice;
        sendDisplayDeviceEventLocked(this.mDisplayDevice, 1);
    }

    private void removeDisplayDeviceLocked() {
        if (this.mDisplayDevice != null) {
            this.mDisplayDevice.destroyLocked();
            sendDisplayDeviceEventLocked(this.mDisplayDevice, 3);
            this.mDisplayDevice = null;
        }
    }

    private void renameDisplayDeviceLocked(String name) {
        if (this.mDisplayDevice != null && !this.mDisplayDevice.getNameLocked().equals(name)) {
            this.mDisplayDevice.setNameLocked(name);
            sendDisplayDeviceEventLocked(this.mDisplayDevice, 2);
        }
    }

    private void scheduleStatusChangedBroadcastLocked() {
        this.mCurrentStatus = null;
        if (!this.mPendingStatusChangeBroadcast) {
            this.mPendingStatusChangeBroadcast = true;
            this.mHandler.sendEmptyMessage(1);
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0056, code skipped:
            getContext().sendBroadcastAsUser(r1, android.os.UserHandle.ALL);
     */
    /* JADX WARNING: Missing block: B:15:0x0060, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleSendStatusChangeBroadcast() {
        synchronized (getSyncRoot()) {
            if (this.mPendingStatusChangeBroadcast) {
                this.mPendingStatusChangeBroadcast = false;
                Intent intent = new Intent("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
                intent.addFlags(1073741824);
                intent.putExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS", getWifiDisplayStatusLocked());
                if (this.mConnectionFailedReason != -1) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleSendStatusChangeBroadcast, connection failed reason is ");
                    stringBuilder.append(this.mConnectionFailedReason);
                    Slog.d(str, stringBuilder.toString());
                    intent.putExtra("android.hardware.display.extra.WIFI_DISPLAY_CONN_FAILED_REASON", this.mConnectionFailedReason);
                    this.mConnectionFailedReason = -1;
                }
                if (this.mActiveDisplayState == 2) {
                    intent.putExtra(WIFI_DISPLAY_UIBC_INFO, this.mUibcCap);
                }
            }
        }
    }

    private void handleSendCastingBroadcast() {
        Intent intent = new Intent(ACTION_WIFI_DISPLAY_CASTING);
        intent.addFlags(1073741824);
        getContext().sendBroadcastAsUser(intent, UserHandle.ALL, WIFI_DISPLAY_CASTING_PERMISSION);
    }

    public Handler getHandlerInner() {
        return getHandler();
    }

    public WifiDisplayController getmDisplayControllerInner() {
        return this.mDisplayController;
    }

    public PersistentDataStore getmPersistentDataStoreInner() {
        return this.mPersistentDataStore;
    }

    public void requestStartScanLocked(int channelID) {
        if (this.mHwAdapterEx != null) {
            this.mHwAdapterEx.requestStartScanLocked(channelID);
        }
    }

    public void requestConnectLocked(String address, String verificaitonCode) {
        if (this.mHwAdapterEx != null) {
            this.mHwAdapterEx.requestConnectLocked(address, verificaitonCode);
        }
    }

    public void checkVerificationResultLocked(boolean isRight) {
        if (this.mHwAdapterEx != null) {
            this.mHwAdapterEx.checkVerificationResultLocked(isRight);
        }
    }

    public void LaunchMKForWifiMode() {
        if (this.mHwAdapterEx != null) {
            this.mHwAdapterEx.LaunchMKForWifiMode();
        }
    }
}
