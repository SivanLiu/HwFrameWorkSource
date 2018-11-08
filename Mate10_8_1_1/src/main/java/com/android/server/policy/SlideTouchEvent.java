package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
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
import com.android.server.gesture.GestureNavConst;

public class SlideTouchEvent {
    public static final String KEY_SINGLE_HAND_SCREEN_ZOOM = "single_hand_screen_zoom";
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

    private boolean isSupportSingleHand() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x00a0 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r12 = this;
        r7 = 1;
        r8 = 0;
        r9 = android.view.WindowManagerGlobal.getWindowManagerService();
        r6 = r9.asBinder();
        r0 = 0;
        r4 = 0;
        r0 = android.os.Parcel.obtain();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r4 = android.os.Parcel.obtain();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        if (r6 == 0) goto L_0x0052;	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
    L_0x0016:
        r9 = "android.view.IWindowManager";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r0.writeInterfaceToken(r9);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9 = 1991; // 0x7c7 float:2.79E-42 double:9.837E-321;	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = 0;	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r6.transact(r9, r0, r4, r10);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r4.readException();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r5 = r4.readInt();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9 = "SlideTouchEvent";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10.<init>();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r11 = "single_hand_switch = ";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = r10.append(r11);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = r10.append(r5);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = r10.toString();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        android.util.Log.i(r9, r10);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        if (r5 != r7) goto L_0x0050;
    L_0x0045:
        if (r0 == 0) goto L_0x004a;
    L_0x0047:
        r0.recycle();
    L_0x004a:
        if (r4 == 0) goto L_0x004f;
    L_0x004c:
        r4.recycle();
    L_0x004f:
        return r7;
    L_0x0050:
        r7 = r8;
        goto L_0x0045;
    L_0x0052:
        if (r0 == 0) goto L_0x0057;
    L_0x0054:
        r0.recycle();
    L_0x0057:
        if (r4 == 0) goto L_0x005c;
    L_0x0059:
        r4.recycle();
    L_0x005c:
        return r7;
    L_0x005d:
        r1 = move-exception;
        r8 = "SlideTouchEvent";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9.<init>();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = "read single_hand_switch exception. message = ";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9 = r9.append(r10);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = r1.getMessage();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9 = r9.append(r10);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9 = r9.toString();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        android.util.Log.e(r8, r9);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r8 = r12.mContext;	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r8 = r8.getResources();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r9 = "single_hand_mode";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r10 = "bool";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r11 = "androidhwext";	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        r3 = r8.getIdentifier(r9, r10, r11);	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
        if (r3 == 0) goto L_0x00aa;
    L_0x0091:
        r8 = r12.mContext;	 Catch:{ NotFoundException -> 0x00a6 }
        r8 = r8.getResources();	 Catch:{ NotFoundException -> 0x00a6 }
        r7 = r8.getBoolean(r3);	 Catch:{ NotFoundException -> 0x00a6 }
        if (r0 == 0) goto L_0x00a0;
    L_0x009d:
        r0.recycle();
    L_0x00a0:
        if (r4 == 0) goto L_0x00a5;
    L_0x00a2:
        r4.recycle();
    L_0x00a5:
        return r7;
    L_0x00a6:
        r2 = move-exception;
        r2.printStackTrace();	 Catch:{ RemoteException -> 0x005d, all -> 0x00b5 }
    L_0x00aa:
        if (r0 == 0) goto L_0x00af;
    L_0x00ac:
        r0.recycle();
    L_0x00af:
        if (r4 == 0) goto L_0x005c;
    L_0x00b1:
        r4.recycle();
        goto L_0x005c;
    L_0x00b5:
        r7 = move-exception;
        if (r0 == 0) goto L_0x00bb;
    L_0x00b8:
        r0.recycle();
    L_0x00bb:
        if (r4 == 0) goto L_0x00c0;
    L_0x00bd:
        r4.recycle();
    L_0x00c0:
        throw r7;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.SlideTouchEvent.isSupportSingleHand():boolean");
    }

    public SlideTouchEvent(Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        boolean z = true;
        if (this.mContext != null) {
            this.mIsSupport = isSupportSingleHand();
            if (this.mIsSupport) {
                this.mThreshHoldsLazyMode = (float) this.mContext.getResources().getDimensionPixelSize(17105147);
                this.mScreenZoomEnabled = System.getIntForUser(this.mContext.getContentResolver(), "single_hand_screen_zoom", 1, ActivityManager.getCurrentUser()) == 1;
                if (Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, ActivityManager.getCurrentUser()) != 1) {
                    z = false;
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
        boolean z = true;
        this.mScreenZoomEnabled = System.getIntForUser(this.mContext.getContentResolver(), "single_hand_screen_zoom", 1, ActivityManager.getCurrentUser()) == 1;
        if (Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, ActivityManager.getCurrentUser()) != 1) {
            z = false;
        }
        this.mZoomGestureEnabled = z;
        if (!this.mScreenZoomEnabled || this.mZoomGestureEnabled) {
            quitLazyMode();
        }
    }

    public void setGestureResultAtUp(boolean success) {
        Log.i(TAG, "gestureResultAtUp:" + success);
        this.mIsValidGuesture &= success;
    }

    private boolean shouldHandleTouchEvent(MotionEvent event) {
        boolean should = true;
        if (event == null || event.getPointerCount() > 1) {
            should = false;
        }
        if (this.mIsSupport && (this.mScreenZoomEnabled ^ 1) == 0 && !this.mZoomGestureEnabled) {
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
                    } else if ((Math.atan((double) (distanceY / distanceX)) / 3.141592653589793d) * 180.0d < 70.0d && 1 == this.mContext.getResources().getConfiguration().orientation) {
                        Log.i(TAG, "preformStartLazyMode");
                        preformStartLazyMode(this.mDownPoint[0] - x);
                        return;
                    }
                case 2:
                    if (distanceY > this.mThreshHoldsLazyMode && (this.mFirstOverThreshold ^ 1) != 0) {
                        this.mFirstOverThreshold = true;
                        double rotationDegree = (Math.atan((double) (distanceY / distanceX)) / 3.141592653589793d) * 180.0d;
                        Log.d(TAG, "rotationDegree=" + rotationDegree + ",mIsValidGuesture=" + this.mIsValidGuesture);
                        if (rotationDegree < 70.0d && (this.mIsValidGuesture ^ 1) != 0) {
                            this.mIsValidGuesture = true;
                            break;
                        }
                    }
            }
        }
    }

    public void preformStartLazyMode(final float x) {
        float distanceX = x;
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                if (1 == SystemProperties.getInt("sys.sdr.enter", 0)) {
                    int sleepCount = 0;
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
                        if (reply != null) {
                            reply.recycle();
                        }
                    } catch (RemoteException ex) {
                        Log.e(SlideTouchEvent.TAG, "stop sdr exception. message = " + ex.getMessage());
                        if (data != null) {
                            data.recycle();
                        }
                        if (reply != null) {
                            reply.recycle();
                        }
                    } catch (Throwable th) {
                        if (data != null) {
                            data.recycle();
                        }
                        if (reply != null) {
                            reply.recycle();
                        }
                    }
                    Log.d("APS", "APS: SDR: special: Lazymode process wait begin ");
                    while (1 == SystemProperties.getInt("sys.sdr.special", 0) && sleepCount < 100) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                        sleepCount++;
                    }
                    Log.d("APS", "APS: SDR: special: Lazymode process wait end, sleepCount=" + sleepCount);
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                SlideTouchEvent.this.startLazyMode(x);
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
}
