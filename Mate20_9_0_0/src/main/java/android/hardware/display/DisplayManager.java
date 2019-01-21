package android.hardware.display;

import android.annotation.SystemApi;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.VirtualDisplay.Callback;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Process;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;
import java.util.ArrayList;
import java.util.List;

public final class DisplayManager {
    public static final String ACTION_WIFI_DISPLAY_STATUS_CHANGED = "android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED";
    private static final boolean DEBUG = false;
    public static final String DISPLAY_CATEGORY_PRESENTATION = "android.hardware.display.category.PRESENTATION";
    public static final String EXTRA_WIFI_DISPLAY_CONN_FAILED_REASON = "android.hardware.display.extra.WIFI_DISPLAY_CONN_FAILED_REASON";
    public static final String EXTRA_WIFI_DISPLAY_STATUS = "android.hardware.display.extra.WIFI_DISPLAY_STATUS";
    private static final String TAG = "DisplayManager";
    public static final int VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 16;
    public static final int VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 32;
    public static final int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 256;
    public static final int VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = 8;
    public static final int VIRTUAL_DISPLAY_FLAG_PRESENTATION = 2;
    public static final int VIRTUAL_DISPLAY_FLAG_PUBLIC = 1;
    public static final int VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT = 128;
    public static final int VIRTUAL_DISPLAY_FLAG_SECURE = 4;
    public static final int VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH = 64;
    private final Context mContext;
    private final SparseArray<Display> mDisplays = new SparseArray();
    private final DisplayManagerGlobal mGlobal;
    private final Object mLock = new Object();
    private final ArrayList<Display> mTempDisplays = new ArrayList();

    public interface DisplayListener {
        void onDisplayAdded(int i);

        void onDisplayChanged(int i);

        void onDisplayRemoved(int i);
    }

    public DisplayManager(Context context) {
        this.mContext = context;
        this.mGlobal = DisplayManagerGlobal.getInstance();
    }

    public Display getDisplay(int displayId) {
        Display orCreateDisplayLocked;
        synchronized (this.mLock) {
            orCreateDisplayLocked = getOrCreateDisplayLocked(displayId, false);
        }
        return orCreateDisplayLocked;
    }

    public Display[] getDisplays() {
        return getDisplays(null);
    }

    public Display[] getDisplays(String category) {
        int[] displayIds = this.mGlobal.getDisplayIds();
        synchronized (this.mLock) {
            if (category == null) {
                try {
                    addAllDisplaysLocked(this.mTempDisplays, displayIds);
                } catch (Throwable th) {
                    this.mTempDisplays.clear();
                }
            } else if (category.equals(DISPLAY_CATEGORY_PRESENTATION)) {
                addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 3);
                addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 2);
                addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 4);
                addPresentationDisplaysLocked(this.mTempDisplays, displayIds, 5);
            }
            Display[] result = (Display[]) this.mTempDisplays.toArray(new Display[this.mTempDisplays.size()]);
            if (this.mContext == null || !((HwPCUtils.isValidExtDisplayId(this.mContext) || HwPCUtils.enabledInPad()) && isSpecialPackageForFilter(this.mContext.getPackageName()))) {
                this.mTempDisplays.clear();
                return result;
            }
            Display[] filterDisplaysForPCMode = filterDisplaysForPCMode(result);
            this.mTempDisplays.clear();
            return filterDisplaysForPCMode;
        }
    }

    private boolean isSpecialPackageForFilter(String packageName) {
        return "com.bankid.bus".equals(packageName) || "com.playstation.tornemobile".equals(packageName) || "com.fami_geki.video".equals(packageName) || "com.citrix.Receiver".equals(packageName);
    }

    private Display[] filterDisplaysForPCMode(Display[] displays) {
        if (displays == null || displays.length == 0) {
            return displays;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("target display:");
        stringBuilder.append(0);
        HwPCUtils.log(str, stringBuilder.toString());
        for (Display displayId : displays) {
            if (displayId.getDisplayId() == 0) {
                Display[] result = new Display[]{displays[i]};
                HwPCUtils.log(TAG, "find target display");
                return result;
            }
        }
        HwPCUtils.log(TAG, "fail to find target display");
        return new Display[0];
    }

    private void addAllDisplaysLocked(ArrayList<Display> displays, int[] displayIds) {
        for (Display display : displayIds) {
            Display display2 = getOrCreateDisplayLocked(display2, true);
            if (display2 != null) {
                displays.add(display2);
            }
        }
    }

    private void addPresentationDisplaysLocked(ArrayList<Display> displays, int[] displayIds, int matchType) {
        for (Display display : displayIds) {
            Display display2 = getOrCreateDisplayLocked(display2, true);
            if (!(display2 == null || (display2.getFlags() & 8) == 0 || display2.getType() != matchType)) {
                displays.add(display2);
            }
        }
    }

    private Display getOrCreateDisplayLocked(int displayId, boolean assumeValid) {
        Display display = (Display) this.mDisplays.get(displayId);
        if (display == null) {
            display = this.mGlobal.getCompatibleDisplay(displayId, (this.mContext.getDisplay().getDisplayId() == displayId ? this.mContext : this.mContext.getApplicationContext()).getResources());
            if (display == null) {
                return display;
            }
            this.mDisplays.put(displayId, display);
            return display;
        } else if (assumeValid || display.isValid()) {
            return display;
        } else {
            return null;
        }
    }

    public void registerDisplayListener(DisplayListener listener, Handler handler) {
        this.mGlobal.registerDisplayListener(listener, handler);
    }

    public void unregisterDisplayListener(DisplayListener listener) {
        this.mGlobal.unregisterDisplayListener(listener);
    }

    public void startWifiDisplayScan() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startWifiDisplayScan, pid:");
        stringBuilder.append(Process.myPid());
        stringBuilder.append(", tid:");
        stringBuilder.append(Process.myTid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Process.myUid());
        Log.d(str, stringBuilder.toString());
        this.mGlobal.startWifiDisplayScan();
    }

    public void stopWifiDisplayScan() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopWifiDisplayScan, pid:");
        stringBuilder.append(Process.myPid());
        stringBuilder.append(", tid:");
        stringBuilder.append(Process.myTid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Process.myUid());
        Log.d(str, stringBuilder.toString());
        this.mGlobal.stopWifiDisplayScan();
    }

    public void connectWifiDisplay(String deviceAddress) {
        this.mGlobal.connectWifiDisplay(deviceAddress);
    }

    public void pauseWifiDisplay() {
        this.mGlobal.pauseWifiDisplay();
    }

    public void resumeWifiDisplay() {
        this.mGlobal.resumeWifiDisplay();
    }

    public void disconnectWifiDisplay() {
        this.mGlobal.disconnectWifiDisplay();
    }

    public void renameWifiDisplay(String deviceAddress, String alias) {
        this.mGlobal.renameWifiDisplay(deviceAddress, alias);
    }

    public void forgetWifiDisplay(String deviceAddress) {
        this.mGlobal.forgetWifiDisplay(deviceAddress);
    }

    public WifiDisplayStatus getWifiDisplayStatus() {
        return this.mGlobal.getWifiDisplayStatus();
    }

    @SystemApi
    public void setSaturationLevel(float level) {
        this.mGlobal.setSaturationLevel(level);
    }

    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int densityDpi, Surface surface, int flags) {
        return createVirtualDisplay(name, width, height, densityDpi, surface, flags, null, null);
    }

    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int densityDpi, Surface surface, int flags, Callback callback, Handler handler) {
        return createVirtualDisplay(null, name, width, height, densityDpi, surface, flags, callback, handler, null);
    }

    public VirtualDisplay createVirtualDisplay(MediaProjection projection, String name, int width, int height, int densityDpi, Surface surface, int flags, Callback callback, Handler handler, String uniqueId) {
        return this.mGlobal.createVirtualDisplay(this.mContext, projection, name, width, height, densityDpi, surface, flags, callback, handler, uniqueId);
    }

    @SystemApi
    public Point getStableDisplaySize() {
        return this.mGlobal.getStableDisplaySize();
    }

    @SystemApi
    public List<BrightnessChangeEvent> getBrightnessEvents() {
        return this.mGlobal.getBrightnessEvents(this.mContext.getOpPackageName());
    }

    @SystemApi
    public List<AmbientBrightnessDayStats> getAmbientBrightnessStats() {
        return this.mGlobal.getAmbientBrightnessStats();
    }

    @SystemApi
    public void setBrightnessConfiguration(BrightnessConfiguration c) {
        setBrightnessConfigurationForUser(c, this.mContext.getUserId(), this.mContext.getPackageName());
    }

    public void setBrightnessConfigurationForUser(BrightnessConfiguration c, int userId, String packageName) {
        this.mGlobal.setBrightnessConfigurationForUser(c, userId, packageName);
    }

    @SystemApi
    public BrightnessConfiguration getBrightnessConfiguration() {
        return getBrightnessConfigurationForUser(this.mContext.getUserId());
    }

    public BrightnessConfiguration getBrightnessConfigurationForUser(int userId) {
        return this.mGlobal.getBrightnessConfigurationForUser(userId);
    }

    @SystemApi
    public BrightnessConfiguration getDefaultBrightnessConfiguration() {
        return this.mGlobal.getDefaultBrightnessConfiguration();
    }

    public void setTemporaryBrightness(int brightness) {
        this.mGlobal.setTemporaryBrightness(brightness);
    }

    public void setTemporaryAutoBrightnessAdjustment(float adjustment) {
        this.mGlobal.setTemporaryAutoBrightnessAdjustment(adjustment);
    }

    @SystemApi
    public Pair<float[], float[]> getMinimumBrightnessCurve() {
        return this.mGlobal.getMinimumBrightnessCurve();
    }
}
