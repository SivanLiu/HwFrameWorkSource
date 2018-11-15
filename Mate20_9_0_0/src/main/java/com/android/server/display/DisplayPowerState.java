package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.FloatProperty;
import android.util.Flog;
import android.util.IntProperty;
import android.util.Log;
import android.util.Slog;
import android.view.Choreographer;
import android.view.Display;
import com.android.server.FingerprintUnlockDataCollector;
import java.io.PrintWriter;

public final class DisplayPowerState {
    private static final String AOD_BACK_LIGHT_KEY = "sys.current_backlight";
    public static final FloatProperty<DisplayPowerState> COLOR_FADE_LEVEL = new FloatProperty<DisplayPowerState>("electronBeamLevel") {
        public void setValue(DisplayPowerState object, float value) {
            object.setColorFadeLevel(value);
        }

        public Float get(DisplayPowerState object) {
            return Float.valueOf(object.getColorFadeLevel());
        }
    };
    private static String COUNTER_COLOR_FADE = "ColorFadeLevel";
    private static boolean DEBUG = false;
    private static boolean DEBUG_Controller = false;
    private static boolean DEBUG_FPLOG = false;
    private static final int DEFAULT_MAX_BRIGHTNESS = 255;
    private static final int HIGH_PRECISION_MAX_BRIGHTNESS = 10000;
    public static final IntProperty<DisplayPowerState> SCREEN_BRIGHTNESS = new IntProperty<DisplayPowerState>("screenBrightness") {
        public void setValue(DisplayPowerState object, int value) {
            object.setScreenBrightness(value);
        }

        public Integer get(DisplayPowerState object) {
            return Integer.valueOf(object.getScreenBrightness());
        }
    };
    private static final String TAG = "DisplayPowerState";
    private static final boolean mSupportAod = "1".equals(SystemProperties.get("ro.config.support_aod", null));
    private FingerprintUnlockDataCollector fpDataCollector;
    private final DisplayBlanker mBlanker;
    private final Choreographer mChoreographer = Choreographer.getInstance();
    private Runnable mCleanListener;
    private final ColorFade mColorFade;
    private boolean mColorFadeDrawPending;
    private final Runnable mColorFadeDrawRunnable = new Runnable() {
        public void run() {
            DisplayPowerState.this.mColorFadeDrawPending = false;
            if (DisplayPowerState.this.mColorFadePrepared) {
                DisplayPowerState.this.mColorFade.draw(DisplayPowerState.this.mColorFadeLevel);
                Trace.traceCounter(131072, DisplayPowerState.COUNTER_COLOR_FADE, Math.round(DisplayPowerState.this.mColorFadeLevel * 100.0f));
            }
            DisplayPowerState.this.mColorFadeReady = true;
            DisplayPowerState.this.invokeCleanListenerIfNeeded();
        }
    };
    private float mColorFadeLevel;
    private boolean mColorFadePrepared;
    private boolean mColorFadeReady;
    private final Handler mHandler = new Handler(true);
    private final PhotonicModulator mPhotonicModulator;
    private int mScreenBrightness = -1;
    private boolean mScreenReady;
    private int mScreenState;
    private boolean mScreenUpdatePending;
    private final Runnable mScreenUpdateRunnable = new Runnable() {
        public void run() {
            boolean z = false;
            DisplayPowerState.this.mScreenUpdatePending = false;
            if (DisplayPowerState.this.mScreenState != 1 && DisplayPowerState.this.mColorFadeLevel > 0.0f) {
                z = DisplayPowerState.this.mScreenBrightness;
            }
            if (DisplayPowerState.this.mPhotonicModulator.setState(DisplayPowerState.this.mScreenState, z)) {
                if (DisplayPowerState.DEBUG_Controller) {
                    Slog.d(DisplayPowerState.TAG, "Screen ready");
                }
                DisplayPowerState.this.mScreenReady = true;
                DisplayPowerState.this.invokeCleanListenerIfNeeded();
            } else if (DisplayPowerState.DEBUG_Controller) {
                Slog.d(DisplayPowerState.TAG, "Screen not ready");
            }
        }
    };

    private final class PhotonicModulator extends Thread {
        private static final int INITIAL_BACKLIGHT = -1;
        private static final int INITIAL_SCREEN_STATE = 1;
        private int mActualBacklight = -1;
        private int mActualState = 1;
        private boolean mBacklightChangeInProgress;
        private final Object mLock = new Object();
        private int mPendingBacklight = -1;
        private int mPendingState = 1;
        private boolean mStateChangeInProgress;

        public PhotonicModulator() {
            super("PhotonicModulator");
        }

        public boolean setState(int state, int backlight) {
            boolean z;
            synchronized (this.mLock) {
                z = false;
                boolean stateChanged = state != this.mPendingState;
                boolean backlightChanged = backlight != this.mPendingBacklight;
                if (stateChanged || backlightChanged) {
                    if (DisplayPowerState.DEBUG_Controller) {
                        String str = DisplayPowerState.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Requesting new screen state: state=");
                        stringBuilder.append(Display.stateToString(state));
                        stringBuilder.append(", backlight=");
                        stringBuilder.append(backlight);
                        Slog.d(str, stringBuilder.toString());
                    }
                    this.mPendingState = state;
                    this.mPendingBacklight = backlight;
                    boolean changeInProgress = this.mStateChangeInProgress || this.mBacklightChangeInProgress;
                    boolean z2 = stateChanged || this.mStateChangeInProgress;
                    this.mStateChangeInProgress = z2;
                    if (backlightChanged || this.mBacklightChangeInProgress) {
                        z = true;
                    }
                    this.mBacklightChangeInProgress = z;
                    if (!changeInProgress) {
                        this.mLock.notifyAll();
                    }
                }
                z = this.mStateChangeInProgress ^ true;
            }
            return z;
        }

        public void dump(PrintWriter pw) {
            synchronized (this.mLock) {
                pw.println();
                pw.println("Photonic Modulator State:");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  mPendingState=");
                stringBuilder.append(Display.stateToString(this.mPendingState));
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mPendingBacklight=");
                stringBuilder.append(this.mPendingBacklight);
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mActualState=");
                stringBuilder.append(Display.stateToString(this.mActualState));
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mActualBacklight=");
                stringBuilder.append(this.mActualBacklight);
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mStateChangeInProgress=");
                stringBuilder.append(this.mStateChangeInProgress);
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mBacklightChangeInProgress=");
                stringBuilder.append(this.mBacklightChangeInProgress);
                pw.println(stringBuilder.toString());
            }
        }

        public void run() {
            while (true) {
                synchronized (this.mLock) {
                    int state = this.mPendingState;
                    boolean screenOnorOff = true;
                    boolean stateChanged = state != this.mActualState;
                    int backlight = this.mPendingBacklight;
                    boolean backlightChanged = backlight != this.mActualBacklight;
                    if (!stateChanged) {
                        DisplayPowerState.this.postScreenUpdateThreadSafe();
                        this.mStateChangeInProgress = false;
                    }
                    if (!backlightChanged) {
                        this.mBacklightChangeInProgress = false;
                    }
                    if (stateChanged || backlightChanged) {
                        this.mActualState = state;
                        if (!(backlight == 0 || this.mActualBacklight == 0)) {
                            screenOnorOff = false;
                        }
                        this.mActualBacklight = backlight;
                        if (screenOnorOff) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("UL_Power Updating screen state: state=");
                            stringBuilder.append(Display.stateToString(state));
                            stringBuilder.append(", backlight=");
                            stringBuilder.append(backlight);
                            Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
                            if (2 == state && backlight > 0 && DisplayPowerState.mSupportAod) {
                                String str = DisplayPowerState.AOD_BACK_LIGHT_KEY;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                stringBuilder.append(backlight);
                                SystemProperties.set(str, stringBuilder.toString());
                            }
                        }
                        DisplayPowerState.this.mBlanker.requestDisplayState(state, backlight);
                    } else {
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            while (true) {
            }
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z2;
        z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        DEBUG_Controller = z2;
        if (!DEBUG) {
            z = false;
        }
        DEBUG_FPLOG = z;
    }

    public DisplayPowerState(DisplayBlanker blanker, ColorFade colorFade) {
        this.mBlanker = blanker;
        this.mColorFade = colorFade;
        this.mPhotonicModulator = new PhotonicModulator();
        this.mPhotonicModulator.start();
        initialize(null);
    }

    public DisplayPowerState(Context context, DisplayBlanker blanker, ColorFade colorFade) {
        this.mBlanker = blanker;
        this.mColorFade = colorFade;
        this.mPhotonicModulator = new PhotonicModulator();
        this.mPhotonicModulator.start();
        initialize(context);
    }

    private void initialize(Context context) {
        this.mScreenState = 2;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(",init mScreenBrightness=");
            stringBuilder.append(this.mScreenBrightness);
            Slog.i(str, stringBuilder.toString());
        }
        scheduleScreenUpdate();
        this.mColorFadePrepared = false;
        this.mColorFadeLevel = 1.0f;
        this.mColorFadeReady = true;
        this.fpDataCollector = FingerprintUnlockDataCollector.getInstance();
    }

    public void setScreenState(int state) {
        if (this.mScreenState != state) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UL_Power setScreenState: state=");
            stringBuilder.append(state);
            Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
            this.mScreenState = state;
            this.mScreenReady = false;
            if (DEBUG_FPLOG) {
                String stateStr = Display.stateToString(state);
                if (this.fpDataCollector != null) {
                    this.fpDataCollector.reportScreenStateOn(stateStr);
                }
            }
            scheduleScreenUpdate();
        }
    }

    public int getScreenState() {
        return this.mScreenState;
    }

    public void setScreenBrightness(int brightness) {
        if (this.mScreenBrightness != brightness) {
            if (DEBUG && DEBUG_Controller) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setScreenBrightness: brightness=");
                stringBuilder.append(brightness);
                Slog.d(str, stringBuilder.toString());
            }
            this.mScreenBrightness = brightness;
            if (this.mScreenState != 1) {
                this.mScreenReady = false;
                scheduleScreenUpdate();
            }
        }
    }

    public int getScreenBrightness() {
        return this.mScreenBrightness;
    }

    public boolean prepareColorFade(Context context, int mode) {
        if (this.mColorFade == null || !this.mColorFade.prepare(context, mode)) {
            this.mColorFadePrepared = false;
            this.mColorFadeReady = true;
            return false;
        }
        this.mColorFadePrepared = true;
        this.mColorFadeReady = false;
        scheduleColorFadeDraw();
        return true;
    }

    public void dismissColorFade() {
        Trace.traceCounter(131072, COUNTER_COLOR_FADE, 100);
        if (this.mColorFade != null) {
            this.mColorFade.dismiss();
        }
        this.mColorFadePrepared = false;
        this.mColorFadeReady = true;
    }

    public void dismissColorFadeResources() {
        if (this.mColorFade != null) {
            this.mColorFade.dismissResources();
        }
    }

    public void setColorFadeLevel(float level) {
        if (this.mColorFadeLevel != level) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UL_Power setColorFadeLevel: level=");
            stringBuilder.append(level);
            Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
            this.mColorFadeLevel = level;
            if (this.mScreenState != 1) {
                this.mScreenReady = false;
                scheduleScreenUpdate();
            }
            if (this.mColorFadePrepared) {
                this.mColorFadeReady = false;
                scheduleColorFadeDraw();
            }
        }
    }

    public float getColorFadeLevel() {
        return this.mColorFadeLevel;
    }

    public boolean waitUntilClean(Runnable listener) {
        if (this.mScreenReady && this.mColorFadeReady) {
            this.mCleanListener = null;
            return true;
        }
        this.mCleanListener = listener;
        return false;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Display Power State:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenState=");
        stringBuilder.append(Display.stateToString(this.mScreenState));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenBrightness=");
        stringBuilder.append(this.mScreenBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenReady=");
        stringBuilder.append(this.mScreenReady);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenUpdatePending=");
        stringBuilder.append(this.mScreenUpdatePending);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mColorFadePrepared=");
        stringBuilder.append(this.mColorFadePrepared);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mColorFadeLevel=");
        stringBuilder.append(this.mColorFadeLevel);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mColorFadeReady=");
        stringBuilder.append(this.mColorFadeReady);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mColorFadeDrawPending=");
        stringBuilder.append(this.mColorFadeDrawPending);
        pw.println(stringBuilder.toString());
        this.mPhotonicModulator.dump(pw);
        if (this.mColorFade != null) {
            this.mColorFade.dump(pw);
        }
    }

    private void scheduleScreenUpdate() {
        if (!this.mScreenUpdatePending) {
            this.mScreenUpdatePending = true;
            postScreenUpdateThreadSafe();
        }
    }

    private void postScreenUpdateThreadSafe() {
        this.mHandler.removeCallbacks(this.mScreenUpdateRunnable);
        this.mHandler.post(this.mScreenUpdateRunnable);
    }

    private void scheduleColorFadeDraw() {
        if (!this.mColorFadeDrawPending) {
            this.mColorFadeDrawPending = true;
            this.mChoreographer.postCallback(2, this.mColorFadeDrawRunnable, null);
        }
    }

    private void invokeCleanListenerIfNeeded() {
        Runnable listener = this.mCleanListener;
        if (listener != null && this.mScreenReady && this.mColorFadeReady) {
            this.mCleanListener = null;
            listener.run();
        }
    }
}
