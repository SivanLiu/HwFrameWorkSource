package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.server.gesture.GestureNavConst;

public class SlideTouchEvent {
    public static final String KEY_SINGLE_HAND_SCREEN_ZOOM = "single_hand_screen_zoom";
    private static final int LAZYMODE_THRESHOLD_DEGREE_MAX = 70;
    private static final int LAZYMODE_THRESHOLD_DEGREE_MIN = 20;
    private static final long LAZYMODE_USE_DURATION = 3600000;
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    public static final float SCALE = 0.75f;
    public static final int STATE_LEFT = 1;
    public static final int STATE_MIDDLE = 0;
    public static final int STATE_RIGHT = 2;
    private static final int STOP_SDR_BINDER_ID = 1195;
    private static final int STOP_SDR_CMD = 1;
    private static final String TAG = "SlideTouchEvent";
    private Context mContext;
    private float[] mDownPoint = new float[2];
    private boolean mFirstOverThreshold;
    private Handler mHandler = new Handler();
    private boolean mIsSupport = true;
    private boolean mIsValidGuesture;
    private boolean mScreenZoomEnabled = true;
    private long mStartLazyModeTime = 0;
    private float mThreshHoldsLazyMode;
    private boolean mZoomGestureEnabled = false;

    public SlideTouchEvent(Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        if (this.mContext != null) {
            this.mIsSupport = isSupportSingleHand();
            if (this.mIsSupport) {
                this.mThreshHoldsLazyMode = (float) this.mContext.getResources().getDimensionPixelSize(17105186);
                boolean z = false;
                this.mScreenZoomEnabled = System.getIntForUser(this.mContext.getContentResolver(), "single_hand_screen_zoom", 1, ActivityManager.getCurrentUser()) == 1;
                if (Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, ActivityManager.getCurrentUser()) == 1) {
                    z = true;
                }
                this.mZoomGestureEnabled = z;
                registerObserver();
            }
        }
    }

    private void registerObserver() {
        ContentObserver slideObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                SlideTouchEvent.this.updateSettings();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("single_hand_screen_zoom"), false, slideObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("accessibility_display_magnification_enabled"), false, slideObserver, -1);
    }

    public void updateSettings() {
        boolean z = false;
        this.mScreenZoomEnabled = System.getIntForUser(this.mContext.getContentResolver(), "single_hand_screen_zoom", 1, ActivityManager.getCurrentUser()) == 1;
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, ActivityManager.getCurrentUser()) == 1) {
            z = true;
        }
        this.mZoomGestureEnabled = z;
        if (!this.mScreenZoomEnabled || this.mZoomGestureEnabled) {
            quitLazyMode();
        }
    }

    public void setGestureResultAtUp(boolean success) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("gestureResultAtUp:");
        stringBuilder.append(success);
        Log.i(str, stringBuilder.toString());
        this.mIsValidGuesture &= success;
    }

    private boolean shouldHandleTouchEvent(MotionEvent event) {
        boolean should = true;
        if (event == null || event.getPointerCount() > 1) {
            should = false;
        }
        if (this.mIsSupport && this.mScreenZoomEnabled && !this.mZoomGestureEnabled) {
            return should;
        }
        return false;
    }

    public void handleTouchEvent(MotionEvent event) {
        if (shouldHandleTouchEvent(event)) {
            float x = event.getX();
            float y = event.getY();
            float distanceX = Math.abs(this.mDownPoint[0] - x);
            float distanceY = Math.abs(this.mDownPoint[1] - y);
            double rotationDegree;
            switch (event.getAction()) {
                case 0:
                    Log.i(TAG, "MotionEvent.ACTION_DOWN");
                    this.mDownPoint[0] = event.getX();
                    this.mDownPoint[1] = event.getY();
                    this.mIsValidGuesture = false;
                    this.mFirstOverThreshold = false;
                    break;
                case 1:
                    if (!this.mIsValidGuesture) {
                        Log.i(TAG, "Sliding distance is too short, can not trigger the lazy mode");
                        break;
                    }
                    rotationDegree = (Math.atan((double) (distanceY / distanceX)) / 3.141592653589793d) * 180.0d;
                    if (rotationDegree < 70.0d && rotationDegree > 20.0d && 1 == this.mContext.getResources().getConfiguration().orientation) {
                        Log.i(TAG, "preformStartLazyMode");
                        preformStartLazyMode(this.mDownPoint[0] - x);
                        return;
                    }
                case 2:
                    if (distanceY > this.mThreshHoldsLazyMode && !this.mFirstOverThreshold) {
                        this.mFirstOverThreshold = true;
                        rotationDegree = (Math.atan((double) (distanceY / distanceX)) / 3.141592653589793d) * 180.0d;
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("rotationDegree=");
                        stringBuilder.append(rotationDegree);
                        stringBuilder.append(",mIsValidGuesture=");
                        stringBuilder.append(this.mIsValidGuesture);
                        Log.d(str, stringBuilder.toString());
                        if (rotationDegree < 70.0d && rotationDegree > 20.0d && !this.mIsValidGuesture) {
                            this.mIsValidGuesture = true;
                            break;
                        }
                    }
            }
        }
    }

    public void preformStartLazyMode(float x) {
        final float distanceX = x;
        new AsyncTask<Void, Void, Void>() {
            /* JADX WARNING: Missing block: B:7:0x002c, code:
            if (r5 != null) goto L_0x002e;
     */
            /* JADX WARNING: Missing block: B:8:0x002e, code:
            r5.recycle();
     */
            /* JADX WARNING: Missing block: B:15:0x0055, code:
            if (r5 != null) goto L_0x002e;
     */
            /* JADX WARNING: Missing block: B:16:0x0058, code:
            android.util.Log.d("APS", "APS: SDR: special: Lazymode process wait begin ");
     */
            /* JADX WARNING: Missing block: B:18:0x0066, code:
            if (1 != android.os.SystemProperties.getInt("sys.sdr.special", 0)) goto L_0x0076;
     */
            /* JADX WARNING: Missing block: B:20:0x006a, code:
            if (r0 >= 100) goto L_0x0076;
     */
            /* JADX WARNING: Missing block: B:23:?, code:
            java.lang.Thread.sleep(10);
     */
            /* JADX WARNING: Missing block: B:26:0x0076, code:
            r2 = new java.lang.StringBuilder();
            r2.append("APS: SDR: special: Lazymode process wait end, sleepCount=");
            r2.append(r0);
            android.util.Log.d("APS", r2.toString());
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            protected Void doInBackground(Void... params) {
                int sleepCount;
                if (1 == SystemProperties.getInt("sys.sdr.enter", 0)) {
                    sleepCount = 0;
                    IBinder mFlinger = ServiceManager.getService("SurfaceFlinger");
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInt(1);
                        data.writeInterfaceToken("android.ui.ISurfaceComposer");
                        mFlinger.transact(SlideTouchEvent.STOP_SDR_BINDER_ID, data, reply, 0);
                        if (data != null) {
                            data.recycle();
                        }
                    } catch (RemoteException ex) {
                        String str = SlideTouchEvent.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("stop sdr exception. message = ");
                        stringBuilder.append(ex.getMessage());
                        Log.e(str, stringBuilder.toString());
                        if (data != null) {
                            data.recycle();
                        }
                    } catch (Throwable th) {
                        if (data != null) {
                            data.recycle();
                        }
                        if (reply != null) {
                            reply.recycle();
                        }
                    }
                }
                return null;
                sleepCount++;
            }

            protected void onPostExecute(Void result) {
                SlideTouchEvent.this.startLazyMode(distanceX);
            }
        }.execute(new Void[0]);
    }

    private void startLazyMode(float distanceX) {
        String str = Global.getString(this.mContext.getContentResolver(), "single_hand_mode");
        if (distanceX > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && TextUtils.isEmpty(str)) {
            Flog.bdReport(this.mContext, 13);
            Global.putString(this.mContext.getContentResolver(), "single_hand_mode", RIGHT);
            this.mStartLazyModeTime = SystemClock.uptimeMillis();
        }
        if (distanceX < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && TextUtils.isEmpty(str)) {
            Flog.bdReport(this.mContext, 14);
            Global.putString(this.mContext.getContentResolver(), "single_hand_mode", LEFT);
            this.mStartLazyModeTime = SystemClock.uptimeMillis();
        }
        if (distanceX < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && str != null && str.contains(LEFT)) {
            quitLazyMode();
            reportLazyModeUsingTime();
        }
        if (distanceX > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && str != null && str.contains(RIGHT)) {
            quitLazyMode();
            reportLazyModeUsingTime();
        }
    }

    private void reportLazyModeUsingTime() {
        if (this.mStartLazyModeTime > 0 && SystemClock.uptimeMillis() - this.mStartLazyModeTime >= 3600000) {
            Log.i(TAG, "BDReporter.EVENT_ID_SINGLE_HAND_USING_ONEHOUR");
        }
    }

    public static int getLazyState(Context context) {
        String str = Global.getString(context.getContentResolver(), "single_hand_mode");
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        if (str.contains(LEFT)) {
            return 1;
        }
        if (str.contains(RIGHT)) {
            return 2;
        }
        return 0;
    }

    public static Rect getScreenshotRect(Context context) {
        Display display = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        int state = getLazyState(context);
        if (1 == state) {
            return new Rect(0, (int) (((float) displayMetrics.heightPixels) * 0.25f), (int) (((float) displayMetrics.widthPixels) * 0.75f), displayMetrics.heightPixels);
        }
        if (2 == state) {
            return new Rect((int) (((float) displayMetrics.widthPixels) * 0.25f), (int) (((float) displayMetrics.heightPixels) * 0.25f), displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
        return null;
    }

    public static boolean isLazyMode(Context context) {
        return getLazyState(context) != 0;
    }

    private void quitLazyMode() {
        Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x00ad  */
    /* JADX WARNING: Missing block: B:17:0x005c, code:
            if (r3 != null) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:18:0x005e, code:
            r3.recycle();
     */
    /* JADX WARNING: Missing block: B:36:0x00b0, code:
            if (r3 == null) goto L_0x00b3;
     */
    /* JADX WARNING: Missing block: B:37:0x00b3, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isSupportSingleHand() {
        boolean z = false;
        if (WindowManagerGlobal.getWindowManagerService() == null) {
            return false;
        }
        IBinder windowManagerBinder = WindowManagerGlobal.getWindowManagerService().asBinder();
        Parcel data = null;
        Parcel reply = null;
        int single_hand_switch;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            if (windowManagerBinder != null) {
                data.writeInterfaceToken("android.view.IWindowManager");
                windowManagerBinder.transact(1991, data, reply, 0);
                reply.readException();
                single_hand_switch = reply.readInt();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("single_hand_switch = ");
                stringBuilder.append(single_hand_switch);
                Log.i(str, stringBuilder.toString());
                if (single_hand_switch == 1) {
                    z = true;
                }
                if (data != null) {
                    data.recycle();
                }
                if (reply != null) {
                    reply.recycle();
                }
                return z;
            } else if (data != null) {
                data.recycle();
            }
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("read single_hand_switch exception. message = ");
            stringBuilder2.append(e.getMessage());
            Log.e(str2, stringBuilder2.toString());
            single_hand_switch = this.mContext.getResources().getIdentifier("single_hand_mode", "bool", "androidhwext");
            if (single_hand_switch != 0) {
                try {
                    boolean z2 = this.mContext.getResources().getBoolean(single_hand_switch);
                    if (data != null) {
                        data.recycle();
                    }
                    if (reply != null) {
                        reply.recycle();
                    }
                    return z2;
                } catch (NotFoundException ex) {
                    ex.printStackTrace();
                    if (data != null) {
                        data.recycle();
                    }
                }
            }
            if (data != null) {
            }
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }
}
