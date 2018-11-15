package com.android.server.vr;

import android.app.ActivityManagerInternal;
import android.app.Vr2dDisplayProperties;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.os.Handler;
import android.os.RemoteException;
import android.service.vr.IPersistentVrStateCallbacks;
import android.service.vr.IPersistentVrStateCallbacks.Stub;
import android.service.vr.IVrManager;
import android.util.Log;
import android.view.Surface;
import com.android.server.wm.WindowManagerInternal;

class Vr2dDisplay {
    private static final boolean DEBUG = false;
    private static final String DEBUG_ACTION_SET_MODE = "com.android.server.vr.Vr2dDisplay.SET_MODE";
    private static final String DEBUG_ACTION_SET_SURFACE = "com.android.server.vr.Vr2dDisplay.SET_SURFACE";
    private static final String DEBUG_EXTRA_MODE_ON = "com.android.server.vr.Vr2dDisplay.EXTRA_MODE_ON";
    private static final String DEBUG_EXTRA_SURFACE = "com.android.server.vr.Vr2dDisplay.EXTRA_SURFACE";
    public static final int DEFAULT_VIRTUAL_DISPLAY_DPI = 320;
    public static final int DEFAULT_VIRTUAL_DISPLAY_HEIGHT = 1800;
    public static final int DEFAULT_VIRTUAL_DISPLAY_WIDTH = 1400;
    private static final String DISPLAY_NAME = "VR 2D Display";
    public static final int MIN_VR_DISPLAY_DPI = 1;
    public static final int MIN_VR_DISPLAY_HEIGHT = 1;
    public static final int MIN_VR_DISPLAY_WIDTH = 1;
    private static final int STOP_VIRTUAL_DISPLAY_DELAY_MILLIS = 2000;
    private static final String TAG = "Vr2dDisplay";
    private static final String UNIQUE_DISPLAY_ID = "277f1a09-b88d-4d1e-8716-796f114d080b";
    private final ActivityManagerInternal mActivityManagerInternal;
    private boolean mBootsToVr = false;
    private final DisplayManager mDisplayManager;
    private final Handler mHandler = new Handler();
    private ImageReader mImageReader;
    private boolean mIsPersistentVrModeEnabled;
    private boolean mIsVirtualDisplayAllowed = true;
    private boolean mIsVrModeOverrideEnabled;
    private Runnable mStopVDRunnable;
    private Surface mSurface;
    private final Object mVdLock = new Object();
    private VirtualDisplay mVirtualDisplay;
    private int mVirtualDisplayDpi;
    private int mVirtualDisplayHeight;
    private int mVirtualDisplayWidth;
    private final IVrManager mVrManager;
    private final IPersistentVrStateCallbacks mVrStateCallbacks = new Stub() {
        public void onPersistentVrStateChanged(boolean enabled) {
            if (enabled != Vr2dDisplay.this.mIsPersistentVrModeEnabled) {
                Vr2dDisplay.this.mIsPersistentVrModeEnabled = enabled;
                Vr2dDisplay.this.updateVirtualDisplay();
            }
        }
    };
    private final WindowManagerInternal mWindowManagerInternal;

    public Vr2dDisplay(DisplayManager displayManager, ActivityManagerInternal activityManagerInternal, WindowManagerInternal windowManagerInternal, IVrManager vrManager) {
        this.mDisplayManager = displayManager;
        this.mActivityManagerInternal = activityManagerInternal;
        this.mWindowManagerInternal = windowManagerInternal;
        this.mVrManager = vrManager;
        this.mVirtualDisplayWidth = DEFAULT_VIRTUAL_DISPLAY_WIDTH;
        this.mVirtualDisplayHeight = 1800;
        this.mVirtualDisplayDpi = DEFAULT_VIRTUAL_DISPLAY_DPI;
    }

    public void init(Context context, boolean bootsToVr) {
        startVrModeListener();
        startDebugOnlyBroadcastReceiver(context);
        this.mBootsToVr = bootsToVr;
        if (this.mBootsToVr) {
            updateVirtualDisplay();
        }
    }

    private void updateVirtualDisplay() {
        if (shouldRunVirtualDisplay()) {
            Log.i(TAG, "Attempting to start virtual display");
            startVirtualDisplay();
            return;
        }
        stopVirtualDisplay();
    }

    private void startDebugOnlyBroadcastReceiver(Context context) {
    }

    private void startVrModeListener() {
        if (this.mVrManager != null) {
            try {
                this.mVrManager.registerPersistentVrStateListener(this.mVrStateCallbacks);
            } catch (RemoteException e) {
                Log.e(TAG, "Could not register VR State listener.", e);
            }
        }
    }

    public void setVirtualDisplayProperties(Vr2dDisplayProperties displayProperties) {
        synchronized (this.mVdLock) {
            int width = displayProperties.getWidth();
            int height = displayProperties.getHeight();
            int dpi = displayProperties.getDpi();
            boolean resized = false;
            String str;
            StringBuilder stringBuilder;
            if (width < 1 || height < 1 || dpi < 1) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring Width/Height/Dpi values of ");
                stringBuilder.append(width);
                stringBuilder.append(",");
                stringBuilder.append(height);
                stringBuilder.append(",");
                stringBuilder.append(dpi);
                Log.i(str, stringBuilder.toString());
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Setting width/height/dpi to ");
                stringBuilder.append(width);
                stringBuilder.append(",");
                stringBuilder.append(height);
                stringBuilder.append(",");
                stringBuilder.append(dpi);
                Log.i(str, stringBuilder.toString());
                this.mVirtualDisplayWidth = width;
                this.mVirtualDisplayHeight = height;
                this.mVirtualDisplayDpi = dpi;
                resized = true;
            }
            if ((displayProperties.getFlags() & 1) == 1) {
                this.mIsVirtualDisplayAllowed = true;
            } else if ((displayProperties.getRemovedFlags() & 1) == 1) {
                this.mIsVirtualDisplayAllowed = false;
            }
            if (this.mVirtualDisplay != null && resized && this.mIsVirtualDisplayAllowed) {
                this.mVirtualDisplay.resize(this.mVirtualDisplayWidth, this.mVirtualDisplayHeight, this.mVirtualDisplayDpi);
                ImageReader oldImageReader = this.mImageReader;
                this.mImageReader = null;
                startImageReader();
                oldImageReader.close();
            }
            updateVirtualDisplay();
        }
    }

    public int getVirtualDisplayId() {
        synchronized (this.mVdLock) {
            if (this.mVirtualDisplay != null) {
                int virtualDisplayId = this.mVirtualDisplay.getDisplay().getDisplayId();
                return virtualDisplayId;
            }
            return -1;
        }
    }

    private void startVirtualDisplay() {
        if (this.mDisplayManager == null) {
            Log.w(TAG, "Cannot create virtual display because mDisplayManager == null");
            return;
        }
        synchronized (this.mVdLock) {
            if (this.mVirtualDisplay != null) {
                Log.i(TAG, "VD already exists, ignoring request");
                return;
            }
            this.mVirtualDisplay = this.mDisplayManager.createVirtualDisplay(null, DISPLAY_NAME, this.mVirtualDisplayWidth, this.mVirtualDisplayHeight, this.mVirtualDisplayDpi, null, (((64 | 128) | 1) | 8) | 256, null, null, UNIQUE_DISPLAY_ID);
            if (this.mVirtualDisplay != null) {
                updateDisplayId(this.mVirtualDisplay.getDisplay().getDisplayId());
                startImageReader();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("VD created: ");
                stringBuilder.append(this.mVirtualDisplay);
                Log.i(str, stringBuilder.toString());
                return;
            }
            Log.w(TAG, "Virtual display id is null after createVirtualDisplay");
            updateDisplayId(-1);
        }
    }

    private void updateDisplayId(int displayId) {
        this.mActivityManagerInternal.setVr2dDisplayId(displayId);
        this.mWindowManagerInternal.setVr2dDisplayId(displayId);
    }

    private void stopVirtualDisplay() {
        if (this.mStopVDRunnable == null) {
            this.mStopVDRunnable = new Runnable() {
                public void run() {
                    if (Vr2dDisplay.this.shouldRunVirtualDisplay()) {
                        Log.i(Vr2dDisplay.TAG, "Virtual Display destruction stopped: VrMode is back on.");
                        return;
                    }
                    Log.i(Vr2dDisplay.TAG, "Stopping Virtual Display");
                    synchronized (Vr2dDisplay.this.mVdLock) {
                        Vr2dDisplay.this.updateDisplayId(-1);
                        Vr2dDisplay.this.setSurfaceLocked(null);
                        if (Vr2dDisplay.this.mVirtualDisplay != null) {
                            Vr2dDisplay.this.mVirtualDisplay.release();
                            Vr2dDisplay.this.mVirtualDisplay = null;
                        }
                        Vr2dDisplay.this.stopImageReader();
                    }
                }
            };
        }
        this.mHandler.removeCallbacks(this.mStopVDRunnable);
        this.mHandler.postDelayed(this.mStopVDRunnable, 2000);
    }

    private void setSurfaceLocked(Surface surface) {
        if (this.mSurface == surface) {
            return;
        }
        if (surface == null || surface.isValid()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting the new surface from ");
            stringBuilder.append(this.mSurface);
            stringBuilder.append(" to ");
            stringBuilder.append(surface);
            Log.i(str, stringBuilder.toString());
            if (this.mVirtualDisplay != null) {
                this.mVirtualDisplay.setSurface(surface);
            }
            if (this.mSurface != null) {
                this.mSurface.release();
            }
            this.mSurface = surface;
        }
    }

    private void startImageReader() {
        if (this.mImageReader == null) {
            this.mImageReader = ImageReader.newInstance(this.mVirtualDisplayWidth, this.mVirtualDisplayHeight, 1, 2);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("VD startImageReader: res = ");
            stringBuilder.append(this.mVirtualDisplayWidth);
            stringBuilder.append("X");
            stringBuilder.append(this.mVirtualDisplayHeight);
            stringBuilder.append(", dpi = ");
            stringBuilder.append(this.mVirtualDisplayDpi);
            Log.i(str, stringBuilder.toString());
        }
        synchronized (this.mVdLock) {
            setSurfaceLocked(this.mImageReader.getSurface());
        }
    }

    private void stopImageReader() {
        if (this.mImageReader != null) {
            this.mImageReader.close();
            this.mImageReader = null;
        }
    }

    private boolean shouldRunVirtualDisplay() {
        return this.mIsVirtualDisplayAllowed && (this.mBootsToVr || this.mIsPersistentVrModeEnabled || this.mIsVrModeOverrideEnabled);
    }
}
