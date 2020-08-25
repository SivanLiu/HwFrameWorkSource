package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
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
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

public class SlideTouchEvent {
    private static final int GESTURE_EXCEED_MAX_DEGREE = 2;
    private static final int GESTURE_SINGLE_HAND = 1;
    private static final int GESTURE_UNCHECKED = 0;
    private static final String KEY_SINGLE_HAND_SCREEN_ZOOM = "single_hand_screen_zoom";
    private static final int LAZYMODE_THRESHOLD_DEGREE_MAX = 70;
    private static final int LAZYMODE_THRESHOLD_DEGREE_MIN = 20;
    private static final long LAZYMODE_USE_DURATION = 3600000;
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final float SCALE = 0.75f;
    public static final int STATE_LEFT = 1;
    public static final int STATE_MIDDLE = 0;
    public static final int STATE_RIGHT = 2;
    private static final int STOP_SDR_BINDER_ID = 1195;
    private static final int STOP_SDR_CMD = 1;
    private static final String TAG = "SlideTouchEvent";
    private Context mContext;
    private float[] mDownPoint = new float[2];
    private boolean mFirstOverThreshold;
    private int mGestureCheckState = 0;
    private Handler mHandler = new Handler();
    private boolean mIsSupport = true;
    private boolean mIsValidGuesture;
    private int mLastGestureCheckState = 0;
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
                this.mThreshHoldsLazyMode = (float) this.mContext.getResources().getDimensionPixelSize(17105305);
                boolean z = false;
                this.mScreenZoomEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "single_hand_screen_zoom", 1, ActivityManager.getCurrentUser()) == 1;
                if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, ActivityManager.getCurrentUser()) == 1) {
                    z = true;
                }
                this.mZoomGestureEnabled = z;
                registerObserver();
            }
        }
    }

    private void registerObserver() {
        ContentObserver slideObserver = new ContentObserver(this.mHandler) {
            /* class com.android.server.policy.SlideTouchEvent.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                SlideTouchEvent.this.updateSettings();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("single_hand_screen_zoom"), false, slideObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("accessibility_display_magnification_enabled"), false, slideObserver, -1);
    }

    public void updateSettings() {
        boolean z = false;
        this.mScreenZoomEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "single_hand_screen_zoom", 1, ActivityManager.getCurrentUser()) == 1;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, ActivityManager.getCurrentUser()) == 1) {
            z = true;
        }
        this.mZoomGestureEnabled = z;
        if (!this.mScreenZoomEnabled || this.mZoomGestureEnabled) {
            quitLazyMode();
        }
    }

    public void setGestureResultAtUp(boolean success) {
        Log.i(TAG, "gestureResultAtUp:" + success);
        this.mIsValidGuesture = this.mIsValidGuesture & success;
    }

    public boolean isBeginFailedAsExceedDegree() {
        return this.mLastGestureCheckState == 0 && this.mGestureCheckState == 2;
    }

    public boolean isSingleHandEnableAndAvailable() {
        return this.mIsSupport && this.mScreenZoomEnabled && !this.mZoomGestureEnabled;
    }

    private boolean shouldHandleTouchEvent(MotionEvent event) {
        if (isSingleHandEnableAndAvailable() && event != null && event.getPointerCount() <= 1) {
            return true;
        }
        return false;
    }

    public void handleTouchEvent(MotionEvent event) {
        if (shouldHandleTouchEvent(event)) {
            float x = event.getX();
            float y = event.getY();
            float distanceX = Math.abs(this.mDownPoint[0] - x);
            float distanceY = Math.abs(this.mDownPoint[1] - y);
            this.mLastGestureCheckState = this.mGestureCheckState;
            int action = event.getAction();
            if (action == 0) {
                Log.i(TAG, "MotionEvent.ACTION_DOWN");
                this.mDownPoint[0] = event.getX();
                this.mDownPoint[1] = event.getY();
                this.mIsValidGuesture = false;
                this.mFirstOverThreshold = false;
                this.mGestureCheckState = 0;
                this.mLastGestureCheckState = 0;
            } else if (action != 1) {
                if (action == 2) {
                    if (distanceY > this.mThreshHoldsLazyMode && !this.mFirstOverThreshold) {
                        this.mFirstOverThreshold = true;
                        double rotationDegree = (Math.atan((double) (distanceY / distanceX)) / 3.141592653589793d) * 180.0d;
                        Log.d(TAG, "rotationDegree=" + rotationDegree + ",mIsValidGuesture=" + this.mIsValidGuesture);
                        if (rotationDegree < 70.0d && rotationDegree > 20.0d && !this.mIsValidGuesture) {
                            this.mIsValidGuesture = true;
                            this.mGestureCheckState = 1;
                        } else if (rotationDegree >= 70.0d) {
                            this.mGestureCheckState = 2;
                        }
                    }
                }
            } else if (this.mIsValidGuesture) {
                double rotationDegree2 = (Math.atan((double) (distanceY / distanceX)) / 3.141592653589793d) * 180.0d;
                if (rotationDegree2 < 70.0d && rotationDegree2 > 20.0d && 1 == this.mContext.getResources().getConfiguration().orientation) {
                    Log.i(TAG, "preformStartLazyMode");
                    preformStartLazyMode(this.mDownPoint[0] - x);
                }
            } else {
                Log.i(TAG, "Sliding distance is too short, can not trigger the lazy mode");
            }
        }
    }

    public void preformStartLazyMode(final float x) {
        new AsyncTask<Void, Void, Void>() {
            /* class com.android.server.policy.SlideTouchEvent.AnonymousClass2 */

            /* access modifiers changed from: protected */
            /* JADX WARNING: Code restructure failed: missing block: B:15:0x0055, code lost:
                if (r5 != null) goto L_0x002d;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:16:0x0058, code lost:
                android.util.Log.d("APS", "APS: SDR: special: Lazymode process wait begin ");
             */
            /* JADX WARNING: Code restructure failed: missing block: B:18:0x0066, code lost:
                if (1 != android.os.SystemProperties.getInt("sys.sdr.special", 0)) goto L_0x0076;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:20:0x006a, code lost:
                if (r1 >= 100) goto L_0x0076;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:23:?, code lost:
                java.lang.Thread.sleep(10);
             */
            /* JADX WARNING: Code restructure failed: missing block: B:26:0x0076, code lost:
                android.util.Log.d("APS", "APS: SDR: special: Lazymode process wait end, sleepCount=" + r1);
             */
            /* JADX WARNING: Code restructure failed: missing block: B:37:?, code lost:
                return null;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:6:0x002b, code lost:
                if (r5 != null) goto L_0x002d;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:7:0x002d, code lost:
                r5.recycle();
             */
            public Void doInBackground(Void... params) {
                int sleepCount;
                if (1 != SystemProperties.getInt("sys.sdr.enter", 0)) {
                    return null;
                }
                sleepCount = 0;
                IBinder mFlinger = ServiceManager.getService("SurfaceFlinger");
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInt(1);
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    mFlinger.transact(SlideTouchEvent.STOP_SDR_BINDER_ID, data, reply, 0);
                    data.recycle();
                } catch (RemoteException ex) {
                    Log.e(SlideTouchEvent.TAG, "stop sdr exception. message = " + ex.getMessage());
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
                    throw th;
                }
                sleepCount++;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Void result) {
                SlideTouchEvent.this.startLazyMode(x);
            }
        }.execute(new Void[0]);
    }

    /* access modifiers changed from: private */
    public void startLazyMode(float distanceX) {
        String str = Settings.Global.getString(this.mContext.getContentResolver(), "single_hand_mode");
        if (distanceX > 0.0f && TextUtils.isEmpty(str)) {
            Flog.bdReport(this.mContext, 13);
            Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "right");
            this.mStartLazyModeTime = SystemClock.uptimeMillis();
        }
        if (distanceX < 0.0f && TextUtils.isEmpty(str)) {
            Flog.bdReport(this.mContext, 14);
            Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "left");
            this.mStartLazyModeTime = SystemClock.uptimeMillis();
        }
        if (distanceX < 0.0f && str != null && str.contains("left")) {
            quitLazyMode();
            reportLazyModeUsingTime();
        }
        if (distanceX > 0.0f && str != null && str.contains("right")) {
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
        String str = Settings.Global.getString(context.getContentResolver(), "single_hand_mode");
        if (TextUtils.isEmpty(str)) {
            return 0;
        }
        if (str.contains("left")) {
            return 1;
        }
        if (str.contains("right")) {
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
        Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0058, code lost:
        if (r4 != null) goto L_0x005a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x005a, code lost:
        r4.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x00ad, code lost:
        if (0 == 0) goto L_0x00b0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00b0, code lost:
        return true;
     */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00aa  */
    private boolean isSupportSingleHand() {
        boolean z = false;
        if (WindowManagerGlobal.getWindowManagerService() == null) {
            return false;
        }
        IBinder windowManagerBinder = WindowManagerGlobal.getWindowManagerService().asBinder();
        Parcel data = null;
        Parcel reply = null;
        try {
            Parcel data2 = Parcel.obtain();
            reply = Parcel.obtain();
            if (windowManagerBinder != null) {
                data2.writeInterfaceToken("android.view.IWindowManager");
                windowManagerBinder.transact(1991, data2, reply, 0);
                reply.readException();
                int single_hand_switch = reply.readInt();
                Log.i(TAG, "single_hand_switch = " + single_hand_switch);
                if (single_hand_switch == 1) {
                    z = true;
                }
                data2.recycle();
                reply.recycle();
                return z;
            } else if (data2 != null) {
                data2.recycle();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "read single_hand_switch exception. message = " + e.getMessage());
            int id = this.mContext.getResources().getIdentifier("single_hand_mode", "bool", "androidhwext");
            if (id != 0) {
                try {
                    boolean z2 = this.mContext.getResources().getBoolean(id);
                    if (0 != 0) {
                        data.recycle();
                    }
                    if (0 != 0) {
                        reply.recycle();
                    }
                    return z2;
                } catch (Resources.NotFoundException e2) {
                    Log.e(TAG, "isSupportSingleHand NotFoundException");
                    if (0 != 0) {
                        data.recycle();
                    }
                }
            }
            if (0 != 0) {
            }
        } catch (Throwable th) {
            if (0 != 0) {
                data.recycle();
            }
            if (0 != 0) {
                reply.recycle();
            }
            throw th;
        }
    }
}
