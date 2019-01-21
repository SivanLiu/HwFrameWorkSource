package com.android.server.display;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings.Global;
import android.util.Slog;
import android.view.Display.Mode;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.display.DisplayManagerService.SyncRoot;
import com.android.server.display.OverlayDisplayWindow.Listener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OverlayDisplayAdapter extends DisplayAdapter {
    static final boolean DEBUG = false;
    private static final Pattern DISPLAY_PATTERN = Pattern.compile("([^,]+)(,[a-z]+)*");
    private static final int MAX_HEIGHT = 4096;
    private static final int MAX_WIDTH = 4096;
    private static final int MIN_HEIGHT = 100;
    private static final int MIN_WIDTH = 100;
    private static final Pattern MODE_PATTERN = Pattern.compile("(\\d+)x(\\d+)/(\\d+)");
    static final String TAG = "OverlayDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "overlay:";
    private String mCurrentOverlaySetting = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private final ArrayList<OverlayDisplayHandle> mOverlays = new ArrayList();
    private final Handler mUiHandler;

    private static final class OverlayMode {
        final int mDensityDpi;
        final int mHeight;
        final int mWidth;

        OverlayMode(int width, int height, int densityDpi) {
            this.mWidth = width;
            this.mHeight = height;
            this.mDensityDpi = densityDpi;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{");
            stringBuilder.append("width=");
            stringBuilder.append(this.mWidth);
            stringBuilder.append(", height=");
            stringBuilder.append(this.mHeight);
            stringBuilder.append(", densityDpi=");
            stringBuilder.append(this.mDensityDpi);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private abstract class OverlayDisplayDevice extends DisplayDevice {
        private int mActiveMode;
        private final int mDefaultMode;
        private final long mDisplayPresentationDeadlineNanos;
        private DisplayDeviceInfo mInfo;
        private final Mode[] mModes;
        private final String mName;
        private final List<OverlayMode> mRawModes;
        private final float mRefreshRate;
        private final boolean mSecure;
        private int mState;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;
        final /* synthetic */ OverlayDisplayAdapter this$0;

        public abstract void onModeChangedLocked(int i);

        public OverlayDisplayDevice(OverlayDisplayAdapter overlayDisplayAdapter, IBinder displayToken, String name, List<OverlayMode> modes, int activeMode, int defaultMode, float refreshRate, long presentationDeadlineNanos, boolean secure, int state, SurfaceTexture surfaceTexture, int number) {
            OverlayDisplayAdapter overlayDisplayAdapter2 = overlayDisplayAdapter;
            List<OverlayMode> list = modes;
            float f = refreshRate;
            this.this$0 = overlayDisplayAdapter2;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(OverlayDisplayAdapter.UNIQUE_ID_PREFIX);
            stringBuilder.append(number);
            super(overlayDisplayAdapter2, displayToken, stringBuilder.toString());
            this.mName = name;
            this.mRefreshRate = f;
            this.mDisplayPresentationDeadlineNanos = presentationDeadlineNanos;
            this.mSecure = secure;
            this.mState = state;
            this.mSurfaceTexture = surfaceTexture;
            this.mRawModes = list;
            this.mModes = new Mode[modes.size()];
            for (int i = 0; i < modes.size(); i++) {
                OverlayMode mode = (OverlayMode) list.get(i);
                this.mModes[i] = DisplayAdapter.createMode(mode.mWidth, mode.mHeight, f);
            }
            this.mActiveMode = activeMode;
            this.mDefaultMode = defaultMode;
        }

        public void destroyLocked() {
            this.mSurfaceTexture = null;
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        public boolean hasStableUniqueId() {
            return false;
        }

        public void performTraversalLocked(Transaction t) {
            if (this.mSurfaceTexture != null) {
                if (this.mSurface == null) {
                    this.mSurface = new Surface(this.mSurfaceTexture);
                }
                setSurfaceLocked(t, this.mSurface);
            }
        }

        public void setStateLocked(int state) {
            this.mState = state;
            this.mInfo = null;
        }

        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                Mode mode = this.mModes[this.mActiveMode];
                OverlayMode rawMode = (OverlayMode) this.mRawModes.get(this.mActiveMode);
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = mode.getPhysicalWidth();
                this.mInfo.height = mode.getPhysicalHeight();
                this.mInfo.modeId = mode.getModeId();
                this.mInfo.defaultModeId = this.mModes[0].getModeId();
                this.mInfo.supportedModes = this.mModes;
                this.mInfo.densityDpi = rawMode.mDensityDpi;
                this.mInfo.xDpi = (float) rawMode.mDensityDpi;
                this.mInfo.yDpi = (float) rawMode.mDensityDpi;
                this.mInfo.presentationDeadlineNanos = this.mDisplayPresentationDeadlineNanos + (1000000000 / ((long) ((int) this.mRefreshRate)));
                this.mInfo.flags = 64;
                if (this.mSecure) {
                    DisplayDeviceInfo displayDeviceInfo = this.mInfo;
                    displayDeviceInfo.flags |= 4;
                }
                this.mInfo.type = 4;
                this.mInfo.touch = 0;
                this.mInfo.state = this.mState;
            }
            return this.mInfo;
        }

        public void requestDisplayModesLocked(int color, int id) {
            int index = -1;
            if (id == 0) {
                index = 0;
            } else {
                for (int i = 0; i < this.mModes.length; i++) {
                    if (this.mModes[i].getModeId() == id) {
                        index = i;
                        break;
                    }
                }
            }
            if (index == -1) {
                String str = OverlayDisplayAdapter.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to locate mode ");
                stringBuilder.append(id);
                stringBuilder.append(", reverting to default.");
                Slog.w(str, stringBuilder.toString());
                index = this.mDefaultMode;
            }
            if (this.mActiveMode != index) {
                this.mActiveMode = index;
                this.mInfo = null;
                this.this$0.sendDisplayDeviceEventLocked(this, 2);
                onModeChangedLocked(index);
            }
        }
    }

    private final class OverlayDisplayHandle implements Listener {
        private static final int DEFAULT_MODE_INDEX = 0;
        private int mActiveMode;
        private OverlayDisplayDevice mDevice;
        private final Runnable mDismissRunnable = new Runnable() {
            public void run() {
                OverlayDisplayWindow window;
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    window = OverlayDisplayHandle.this.mWindow;
                    OverlayDisplayHandle.this.mWindow = null;
                }
                if (window != null) {
                    window.dismiss();
                }
            }
        };
        private final int mGravity;
        private final List<OverlayMode> mModes;
        private final String mName;
        private final int mNumber;
        private final Runnable mResizeRunnable = new Runnable() {
            public void run() {
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    if (OverlayDisplayHandle.this.mWindow == null) {
                        return;
                    }
                    OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                    OverlayDisplayWindow window = OverlayDisplayHandle.this.mWindow;
                    window.resize(mode.mWidth, mode.mHeight, mode.mDensityDpi);
                }
            }
        };
        private final boolean mSecure;
        private final Runnable mShowRunnable = new Runnable() {
            public void run() {
                OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                OverlayDisplayWindow window = new OverlayDisplayWindow(OverlayDisplayAdapter.this.getContext(), OverlayDisplayHandle.this.mName, mode.mWidth, mode.mHeight, mode.mDensityDpi, OverlayDisplayHandle.this.mGravity, OverlayDisplayHandle.this.mSecure, OverlayDisplayHandle.this);
                window.show();
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    OverlayDisplayHandle.this.mWindow = window;
                }
            }
        };
        private OverlayDisplayWindow mWindow;

        public OverlayDisplayHandle(String name, List<OverlayMode> modes, int gravity, boolean secure, int number) {
            this.mName = name;
            this.mModes = modes;
            this.mGravity = gravity;
            this.mSecure = secure;
            this.mNumber = number;
            this.mActiveMode = 0;
            showLocked();
        }

        private void showLocked() {
            OverlayDisplayAdapter.this.mUiHandler.post(this.mShowRunnable);
        }

        public void dismissLocked() {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnable);
            OverlayDisplayAdapter.this.mUiHandler.post(this.mDismissRunnable);
        }

        private void onActiveModeChangedLocked(int index) {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mResizeRunnable);
            this.mActiveMode = index;
            if (this.mWindow != null) {
                OverlayDisplayAdapter.this.mUiHandler.post(this.mResizeRunnable);
            }
        }

        public void onWindowCreated(SurfaceTexture surfaceTexture, float refreshRate, long presentationDeadlineNanos, int state) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                this.mDevice = new OverlayDisplayDevice(this, SurfaceControl.createDisplay(this.mName, this.mSecure), this.mName, this.mModes, this.mActiveMode, 0, refreshRate, presentationDeadlineNanos, this.mSecure, state, surfaceTexture, this.mNumber) {
                    final /* synthetic */ OverlayDisplayHandle this$1;

                    public void onModeChangedLocked(int index) {
                        this.this$1.onActiveModeChangedLocked(index);
                    }
                };
                OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 1);
            }
        }

        public void onWindowDestroyed() {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.destroyLocked();
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 3);
                }
            }
        }

        public void onStateChanged(int state) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.setStateLocked(state);
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 2);
                }
            }
        }

        public void dumpLocked(PrintWriter pw) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  ");
            stringBuilder.append(this.mName);
            stringBuilder.append(":");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mModes=");
            stringBuilder.append(Arrays.toString(this.mModes.toArray()));
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mActiveMode=");
            stringBuilder.append(this.mActiveMode);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mGravity=");
            stringBuilder.append(this.mGravity);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mSecure=");
            stringBuilder.append(this.mSecure);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("    mNumber=");
            stringBuilder.append(this.mNumber);
            pw.println(stringBuilder.toString());
            if (this.mWindow != null) {
                PrintWriter ipw = new IndentingPrintWriter(pw, "    ");
                ipw.increaseIndent();
                DumpUtils.dumpAsync(OverlayDisplayAdapter.this.mUiHandler, this.mWindow, ipw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 200);
            }
        }
    }

    public OverlayDisplayAdapter(SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, Handler uiHandler) {
        super(syncRoot, context, handler, listener, TAG);
        this.mUiHandler = uiHandler;
    }

    public void dumpLocked(PrintWriter pw) {
        super.dumpLocked(pw);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentOverlaySetting=");
        stringBuilder.append(this.mCurrentOverlaySetting);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mOverlays: size=");
        stringBuilder.append(this.mOverlays.size());
        pw.println(stringBuilder.toString());
        Iterator it = this.mOverlays.iterator();
        while (it.hasNext()) {
            ((OverlayDisplayHandle) it.next()).dumpLocked(pw);
        }
    }

    public void registerLocked() {
        super.registerLocked();
        getHandler().post(new Runnable() {
            public void run() {
                OverlayDisplayAdapter.this.getContext().getContentResolver().registerContentObserver(Global.getUriFor("overlay_display_devices"), true, new ContentObserver(OverlayDisplayAdapter.this.getHandler()) {
                    public void onChange(boolean selfChange) {
                        OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
                    }
                });
                OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
            }
        });
    }

    private void updateOverlayDisplayDevices() {
        synchronized (getSyncRoot()) {
            updateOverlayDisplayDevicesLocked();
        }
    }

    private void updateOverlayDisplayDevicesLocked() {
        Matcher matcher;
        String value = Global.getString(getContext().getContentResolver(), "overlay_display_devices");
        if (value == null) {
            value = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String value2 = value;
        if (!value2.equals(this.mCurrentOverlaySetting)) {
            this.mCurrentOverlaySetting = value2;
            if (!this.mOverlays.isEmpty()) {
                Slog.i(TAG, "Dismissing all overlay display devices.");
                Iterator it = this.mOverlays.iterator();
                while (it.hasNext()) {
                    ((OverlayDisplayHandle) it.next()).dismissLocked();
                }
                this.mOverlays.clear();
            }
            int number = value2.split(";");
            int length = number.length;
            int count = 0;
            int i = 0;
            while (i < length) {
                Object obj;
                StringBuilder stringBuilder;
                Matcher displayMatcher = DISPLAY_PATTERN.matcher(number[i]);
                if (!displayMatcher.matches()) {
                    obj = number;
                } else if (count >= 4) {
                    value = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Too many overlay display devices specified: ");
                    stringBuilder.append(value2);
                    Slog.w(value, stringBuilder.toString());
                    break;
                } else {
                    String modeString;
                    int width;
                    String modeString2 = displayMatcher.group(1);
                    String flagString = displayMatcher.group(2);
                    ArrayList<OverlayMode> arrayList = new ArrayList();
                    String[] split = modeString2.split("\\|");
                    int length2 = split.length;
                    int i2 = 0;
                    while (i2 < length2) {
                        String mode = split[i2];
                        String[] strArr = split;
                        Matcher modeMatcher = MODE_PATTERN.matcher(mode);
                        if (modeMatcher.matches()) {
                            modeString = modeString2;
                            try {
                                width = Integer.parseInt(modeMatcher.group(1), 10);
                                obj = number;
                                try {
                                    number = Integer.parseInt(modeMatcher.group(2), 10);
                                    try {
                                        int densityDpi = Integer.parseInt(modeMatcher.group(3), 10);
                                        if (width < 100 || width > 4096 || number < 100 || number > 4096 || densityDpi < 120 || densityDpi > 640) {
                                            modeString2 = TAG;
                                            StringBuilder stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Ignoring out-of-range overlay display mode: ");
                                            stringBuilder2.append(mode);
                                            Slog.w(modeString2, stringBuilder2.toString());
                                        } else {
                                            arrayList.add(new OverlayMode(width, number, densityDpi));
                                        }
                                    } catch (NumberFormatException e) {
                                    }
                                } catch (NumberFormatException e2) {
                                    matcher = modeMatcher;
                                }
                            } catch (NumberFormatException e3) {
                                matcher = modeMatcher;
                                obj = number;
                            }
                        } else {
                            matcher = modeMatcher;
                            modeString = modeString2;
                            obj = number;
                            if (mode.isEmpty()) {
                            }
                        }
                        i2++;
                        split = strArr;
                        modeString2 = modeString;
                        number = obj;
                    }
                    modeString = modeString2;
                    obj = number;
                    if (!arrayList.isEmpty()) {
                        width = count + 1;
                        number = width;
                        modeString2 = getContext().getResources().getString(17039946, new Object[]{Integer.valueOf(number)});
                        int gravity = chooseOverlayGravity(number);
                        boolean secure = flagString != null && flagString.contains(",secure");
                        String str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Showing overlay display device #");
                        stringBuilder.append(number);
                        stringBuilder.append(": name=");
                        stringBuilder.append(modeString2);
                        stringBuilder.append(", modes=");
                        stringBuilder.append(Arrays.toString(arrayList.toArray()));
                        Slog.i(str, stringBuilder.toString());
                        OverlayDisplayHandle overlayDisplayHandle = r1;
                        int count2 = width;
                        ArrayList arrayList2 = this.mOverlays;
                        ArrayList<OverlayMode> modes = arrayList;
                        OverlayDisplayHandle overlayDisplayHandle2 = new OverlayDisplayHandle(modeString2, arrayList, gravity, secure, number);
                        arrayList2.add(overlayDisplayHandle);
                        count = count2;
                        i++;
                        number = obj;
                    }
                }
                value = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Malformed overlay display devices setting: ");
                stringBuilder.append(value2);
                Slog.w(value, stringBuilder.toString());
                i++;
                number = obj;
            }
        }
    }

    private static int chooseOverlayGravity(int overlayNumber) {
        switch (overlayNumber) {
            case 1:
                return 51;
            case 2:
                return 85;
            case 3:
                return 53;
            default:
                return 83;
        }
    }
}
