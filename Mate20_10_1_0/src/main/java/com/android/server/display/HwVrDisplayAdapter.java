package com.android.server.display;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.OverlayDisplayWindow;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class HwVrDisplayAdapter extends DisplayAdapter {
    private static final int DEFAULT_DISPLAY_PARAMS_LEN = 3;
    private static final int DEFAULT_OVERLAY_NUM = 1;
    private static final long REFRESH_SCALE = 1000000000;
    private static final String TAG = "HwVrDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "hwoverlay:";
    private final int INDEX_DENSITY = 2;
    private final int INDEX_HEIGHT = 1;
    private final int INDEX_WIDTH = 0;
    private final int LEN_OF_OVERMODE = 4;
    private final List<OverlayDisplayHandle> mOverlays = new ArrayList(16);
    /* access modifiers changed from: private */
    public final Handler mUiHandler;

    public /* bridge */ /* synthetic */ void dumpLocked(PrintWriter x0) {
        HwVrDisplayAdapter.super.dumpLocked(x0);
    }

    public HwVrDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, Listener listener, Handler uiHandler) {
        super(syncRoot, context, handler, listener, TAG);
        this.mUiHandler = uiHandler;
    }

    public void registerLocked() {
        HwVrDisplayAdapter.super.registerLocked();
    }

    public boolean createVrDisplay(String displayName, int[] displayParams) {
        boolean createVrDisplayLocked;
        Log.i(TAG, "createVrDisplay " + displayName + " with params " + Arrays.toString(displayParams));
        synchronized (getSyncRoot()) {
            createVrDisplayLocked = createVrDisplayLocked(displayName, displayParams);
        }
        return createVrDisplayLocked;
    }

    public boolean destroyVrDisplay(String displayName) {
        boolean destroyVrDisplayLocked;
        Log.i(TAG, "destroyVrDisplay " + displayName);
        synchronized (getSyncRoot()) {
            destroyVrDisplayLocked = destroyVrDisplayLocked(displayName);
        }
        return destroyVrDisplayLocked;
    }

    public boolean destroyAllVrDisplay() {
        synchronized (getSyncRoot()) {
            Log.i(TAG, "destroyAllVrDisplay");
            if (this.mOverlays != null) {
                if (!this.mOverlays.isEmpty()) {
                    Iterator<OverlayDisplayHandle> iterator = this.mOverlays.iterator();
                    while (iterator.hasNext()) {
                        iterator.next().dismissLocked();
                        iterator.remove();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private boolean createVrDisplayLocked(String displayName, int[] displayParams) {
        if (displayParams.length != 3) {
            Log.w(TAG, "displayParams is error");
            return false;
        }
        int width = displayParams[0];
        int height = displayParams[1];
        int density = displayParams[2];
        ArrayList<OverlayMode> modes = new ArrayList<>(4);
        modes.add(new OverlayMode(width, height, density));
        this.mOverlays.add(new OverlayDisplayHandle(displayName, modes, 8388659, true, 1));
        return true;
    }

    private boolean destroyVrDisplayLocked(String displayName) {
        List<OverlayDisplayHandle> list = this.mOverlays;
        if (list == null || displayName == null) {
            return false;
        }
        if (list.isEmpty()) {
            return true;
        }
        Log.i(TAG, "Dismissing all overlay display devices.");
        for (OverlayDisplayHandle overlay : this.mOverlays) {
            overlay.dismissLocked();
        }
        this.mOverlays.clear();
        return true;
    }

    private final class OverlayDisplayHandle implements OverlayDisplayWindow.Listener {
        private static final int DEFAULT_MODE_INDEX = 0;
        /* access modifiers changed from: private */
        public final boolean isSecure;
        /* access modifiers changed from: private */
        public int mActiveMode;
        private OverlayDisplayDevice mDevice;
        private final Runnable mDismissRunnable = new Runnable() {
            /* class com.android.server.display.HwVrDisplayAdapter.OverlayDisplayHandle.AnonymousClass3 */

            public void run() {
                OverlayDisplayWindow window;
                synchronized (HwVrDisplayAdapter.this.getSyncRoot()) {
                    window = OverlayDisplayHandle.this.mWindow;
                    OverlayDisplayWindow unused = OverlayDisplayHandle.this.mWindow = null;
                }
                if (window != null) {
                    window.dismiss();
                }
            }
        };
        /* access modifiers changed from: private */
        public final int mGravity;
        /* access modifiers changed from: private */
        public final ArrayList<OverlayMode> mModes;
        /* access modifiers changed from: private */
        public final String mName;
        private final int mNumber;
        private final Runnable mResizeRunnable = new Runnable() {
            /* class com.android.server.display.HwVrDisplayAdapter.OverlayDisplayHandle.AnonymousClass4 */

            public void run() {
                synchronized (HwVrDisplayAdapter.this.getSyncRoot()) {
                    if (OverlayDisplayHandle.this.mWindow != null) {
                        if (OverlayDisplayHandle.this.mActiveMode >= 0) {
                            if (OverlayDisplayHandle.this.mActiveMode < OverlayDisplayHandle.this.mModes.size()) {
                                OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                                OverlayDisplayHandle.this.mWindow.resize(mode.mWidth, mode.mHeight, mode.mDensityDpi);
                                return;
                            }
                        }
                        Log.e(HwVrDisplayAdapter.TAG, "error in resize, mode index " + OverlayDisplayHandle.this.mActiveMode + " error.");
                    }
                }
            }
        };
        private final Runnable mShowRunnable = new Runnable() {
            /* class com.android.server.display.HwVrDisplayAdapter.OverlayDisplayHandle.AnonymousClass2 */

            public void run() {
                if (OverlayDisplayHandle.this.mActiveMode < 0 || OverlayDisplayHandle.this.mActiveMode >= OverlayDisplayHandle.this.mModes.size()) {
                    Log.e(HwVrDisplayAdapter.TAG, "error in show, mode index " + OverlayDisplayHandle.this.mActiveMode + " error.");
                    return;
                }
                OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                OverlayDisplayWindow window = new OverlayDisplayWindow(HwVrDisplayAdapter.this.getContext(), OverlayDisplayHandle.this.mName, mode.mWidth, mode.mHeight, mode.mDensityDpi, OverlayDisplayHandle.this.mGravity, OverlayDisplayHandle.this.isSecure, OverlayDisplayHandle.this);
                window.show();
                synchronized (HwVrDisplayAdapter.this.getSyncRoot()) {
                    OverlayDisplayWindow unused = OverlayDisplayHandle.this.mWindow = window;
                }
            }
        };
        /* access modifiers changed from: private */
        public OverlayDisplayWindow mWindow;

        public OverlayDisplayHandle(String name, ArrayList<OverlayMode> modes, int gravity, boolean secure, int number) {
            this.mName = name;
            this.mModes = modes;
            this.mGravity = gravity;
            this.isSecure = secure;
            this.mNumber = number;
            this.mActiveMode = 0;
            showLocked();
        }

        private void showLocked() {
            HwVrDisplayAdapter.this.mUiHandler.post(this.mShowRunnable);
        }

        public void dismissLocked() {
            HwVrDisplayAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnable);
            HwVrDisplayAdapter.this.mUiHandler.post(this.mDismissRunnable);
        }

        /* access modifiers changed from: private */
        public void onActiveModeChangedLocked(int index) {
            HwVrDisplayAdapter.this.mUiHandler.removeCallbacks(this.mResizeRunnable);
            this.mActiveMode = index;
            if (this.mWindow != null) {
                HwVrDisplayAdapter.this.mUiHandler.post(this.mResizeRunnable);
            }
        }

        public void onWindowCreated(SurfaceTexture surfaceTexture, float refreshRate, long presentationDeadlineNanos, int state) {
            synchronized (HwVrDisplayAdapter.this.getSyncRoot()) {
                this.mDevice = new OverlayDisplayDevice(SurfaceControl.createDisplay(this.mName, this.isSecure), this.mName, this.mModes, this.mActiveMode, 0, refreshRate, presentationDeadlineNanos, this.isSecure, state, surfaceTexture, this.mNumber) {
                    /* class com.android.server.display.HwVrDisplayAdapter.OverlayDisplayHandle.AnonymousClass1 */

                    @Override // com.android.server.display.HwVrDisplayAdapter.OverlayDisplayDevice
                    public void onModeChangedLocked(int index) {
                        OverlayDisplayHandle.this.onActiveModeChangedLocked(index);
                    }
                };
                HwVrDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 1);
            }
        }

        public void onWindowDestroyed() {
            synchronized (HwVrDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.destroyLocked();
                    HwVrDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 3);
                }
            }
        }

        public void onStateChanged(int state) {
            synchronized (HwVrDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.setStateLocked(state);
                    HwVrDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 2);
                }
            }
        }
    }

    private abstract class OverlayDisplayDevice extends DisplayDevice {
        private final boolean isSecure;
        private int mActiveMode;
        private final int mDefaultMode;
        private final long mDisplayPresentationDeadlineNanos;
        private DisplayDeviceInfo mInfo;
        private final Display.Mode[] mModes;
        private final String mName;
        private final List<OverlayMode> mRawModes;
        private final float mRefreshRate;
        private int mState;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;

        public abstract void onModeChangedLocked(int i);

        public OverlayDisplayDevice(IBinder displayToken, String name, List<OverlayMode> modes, int activeMode, int defaultMode, float refreshRate, long presentationDeadlineNanos, boolean secure, int state, SurfaceTexture surfaceTexture, int number) {
            super(HwVrDisplayAdapter.this, displayToken, HwVrDisplayAdapter.UNIQUE_ID_PREFIX + number);
            this.mName = name;
            this.mRefreshRate = refreshRate;
            this.mDisplayPresentationDeadlineNanos = presentationDeadlineNanos;
            this.isSecure = secure;
            this.mState = state;
            this.mSurfaceTexture = surfaceTexture;
            this.mRawModes = modes;
            this.mModes = new Display.Mode[modes.size()];
            int modeSize = modes.size();
            for (int i = 0; i < modeSize; i++) {
                OverlayMode mode = modes.get(i);
                this.mModes[i] = DisplayAdapter.createMode(mode.mWidth, mode.mHeight, refreshRate);
            }
            this.mActiveMode = activeMode;
            this.mDefaultMode = defaultMode;
        }

        public void destroyLocked() {
            this.mSurfaceTexture = null;
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        public boolean hasStableUniqueId() {
            return false;
        }

        public void performTraversalLocked(SurfaceControl.Transaction transaction) {
            SurfaceTexture surfaceTexture = this.mSurfaceTexture;
            if (surfaceTexture != null) {
                if (this.mSurface == null) {
                    this.mSurface = new Surface(surfaceTexture);
                }
                setSurfaceLocked(transaction, this.mSurface);
            }
        }

        public void setStateLocked(int state) {
            this.mState = state;
            this.mInfo = null;
        }

        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                int i = this.mActiveMode;
                if (i < 0 || i >= this.mRawModes.size()) {
                    Log.e(HwVrDisplayAdapter.TAG, "error in getDisplayDeviceInfoLocked, mode index " + this.mActiveMode + " error.");
                    return new DisplayDeviceInfo();
                }
                Display.Mode[] modeArr = this.mModes;
                int i2 = this.mActiveMode;
                Display.Mode mode = modeArr[i2];
                OverlayMode rawMode = this.mRawModes.get(i2);
                this.mInfo = new DisplayDeviceInfo();
                DisplayDeviceInfo displayDeviceInfo = this.mInfo;
                displayDeviceInfo.name = this.mName;
                displayDeviceInfo.uniqueId = getUniqueId();
                this.mInfo.width = mode.getPhysicalWidth();
                this.mInfo.height = mode.getPhysicalHeight();
                this.mInfo.modeId = mode.getModeId();
                this.mInfo.defaultModeId = this.mModes[0].getModeId();
                DisplayDeviceInfo displayDeviceInfo2 = this.mInfo;
                displayDeviceInfo2.supportedModes = this.mModes;
                displayDeviceInfo2.densityDpi = rawMode.mDensityDpi;
                this.mInfo.xDpi = (float) rawMode.mDensityDpi;
                this.mInfo.yDpi = (float) rawMode.mDensityDpi;
                DisplayDeviceInfo displayDeviceInfo3 = this.mInfo;
                displayDeviceInfo3.presentationDeadlineNanos = this.mDisplayPresentationDeadlineNanos + (1000000000 / ((long) ((int) this.mRefreshRate)));
                displayDeviceInfo3.flags = 1088;
                if (this.isSecure) {
                    displayDeviceInfo3.flags |= 4;
                }
                DisplayDeviceInfo displayDeviceInfo4 = this.mInfo;
                displayDeviceInfo4.type = 4;
                displayDeviceInfo4.touch = 3;
                displayDeviceInfo4.state = this.mState;
            }
            return this.mInfo;
        }
    }

    /* access modifiers changed from: private */
    public static final class OverlayMode {
        final int mDensityDpi;
        final int mHeight;
        final int mWidth;

        OverlayMode(int width, int height, int densityDpi) {
            this.mWidth = width;
            this.mHeight = height;
            this.mDensityDpi = densityDpi;
        }
    }
}
