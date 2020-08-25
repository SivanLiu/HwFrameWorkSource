package com.android.server.wm;

import android.app.AbsWallpaperManagerInner;
import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.cover.CoverManager;
import android.database.ContentObserver;
import android.freeform.HwFreeFormManager;
import android.freeform.HwFreeFormUtils;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.view.DisplayInfo;
import com.android.server.LocalServices;
import com.android.server.multiwin.HwMultiWinConstants;
import com.huawei.android.statistical.StatisticalUtils;
import com.huawei.android.view.HwExtDisplaySizeUtil;
import com.huawei.sidetouch.HwSideTouchManager;
import java.util.ArrayList;
import java.util.Iterator;

final class SingleHandAdapter {
    static final boolean DEBUG = false;
    private static final float INITIAL_SCALE = 0.75f;
    private static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    public static final String KEY_SINGLE_HAND_SCREEN_ZOOM = "single_hand_screen_zoom";
    private static final int MSG_CLEAR_WALLPAPER = 1;
    private static final int MSG_ENTER_SINGLEHAND_TIMEOUT = 2;
    private static final String SHOW_ROUNDED_CORNERS = "show_rounded_corners";
    static final String TAG = "SingleHand";
    /* access modifiers changed from: private */
    public static boolean isSingleHandEnabled = true;
    private static boolean mIsFirstEnteredSingleHandMode = true;
    static final Object mLock = new Object();
    private static int mRoundedStateFlag = 1;
    private static int mRoundedStateFlagPre = 0;
    static Bitmap scaleWallpaper = null;
    /* access modifiers changed from: private */
    public Bitmap mBlurBitmap;
    private AbsWallpaperManagerInner.IBlurWallpaperCallback mBlurCallback;
    /* access modifiers changed from: private */
    public final Context mContext;
    private CoverManager mCoverManager = null;
    private boolean mCoverOpen = true;
    private String mCurrentMode = "";
    /* access modifiers changed from: private */
    public DisplayInfo mDefaultDisplayInfo = new DisplayInfo();
    /* access modifiers changed from: private */
    public boolean mEnterSinglehandwindow2s = false;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private final ArrayList<SingleHandHandle> mOverlays = new ArrayList<>();
    private final Handler mPaperHandler;
    /* access modifiers changed from: private */
    public final WindowManagerService mService;
    private Handler mSideTouchHandler = new Handler();
    /* access modifiers changed from: private */
    public final Handler mUiHandler;
    private WallpaperManager mWallpaperManager;

    public SingleHandAdapter(Context context, Handler handler, Handler uiHandler, WindowManagerService service) {
        this.mHandler = handler;
        this.mContext = context;
        this.mUiHandler = uiHandler;
        this.mService = service;
        this.mDefaultDisplayInfo = this.mService.getDefaultDisplayContentLocked().getDisplayInfo();
        this.mWallpaperManager = (WallpaperManager) this.mContext.getSystemService("wallpaper");
        this.mBlurCallback = new blurCallback();
        this.mWallpaperManager.setCallback(this.mBlurCallback);
        this.mPaperHandler = new Handler(this.mUiHandler.getLooper()) {
            /* class com.android.server.wm.SingleHandAdapter.AnonymousClass1 */

            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 1) {
                    Slog.i(SingleHandAdapter.TAG, "update blur wallpaper");
                    SingleHandAdapter.this.updateScaleWallpaperForBlur();
                } else if (i == 2) {
                    Slog.i(SingleHandAdapter.TAG, "enter singlehandwindow 2s");
                    synchronized (SingleHandAdapter.mLock) {
                        boolean unused = SingleHandAdapter.this.mEnterSinglehandwindow2s = true;
                    }
                }
            }
        };
        updateBlur();
    }

    public void registerLocked() {
        Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
        this.mHandler.post(new Runnable() {
            /* class com.android.server.wm.SingleHandAdapter.AnonymousClass2 */

            public void run() {
                SingleHandAdapter.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("single_hand_mode"), true, new ContentObserver(SingleHandAdapter.this.mHandler) {
                    /* class com.android.server.wm.SingleHandAdapter.AnonymousClass2.AnonymousClass1 */

                    public void onChange(boolean selfChange) {
                        SingleHandAdapter.this.updateSingleHandMode();
                    }
                });
                SingleHandAdapter.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SingleHandAdapter.KEY_SINGLE_HAND_SCREEN_ZOOM), true, new ContentObserver(SingleHandAdapter.this.mHandler) {
                    /* class com.android.server.wm.SingleHandAdapter.AnonymousClass2.AnonymousClass2 */

                    public void onChange(boolean selfChange) {
                        boolean z = true;
                        if (Settings.System.getIntForUser(SingleHandAdapter.this.mContext.getContentResolver(), SingleHandAdapter.KEY_SINGLE_HAND_SCREEN_ZOOM, 1, ActivityManager.getCurrentUser()) != 1) {
                            z = false;
                        }
                        boolean unused = SingleHandAdapter.isSingleHandEnabled = z;
                        Slog.i(SingleHandAdapter.TAG, "singleHandEnabled:" + SingleHandAdapter.isSingleHandEnabled);
                        if (SingleHandAdapter.isSingleHandEnabled) {
                            SingleHandAdapter.this.updateBlur();
                            SingleHandAdapter.this.updateScaleWallpaperForBlur();
                            return;
                        }
                        synchronized (SingleHandAdapter.mLock) {
                            if (SingleHandAdapter.this.mBlurBitmap != null) {
                                SingleHandAdapter.this.mBlurBitmap.recycle();
                                Bitmap unused2 = SingleHandAdapter.this.mBlurBitmap = null;
                            }
                            if (SingleHandAdapter.scaleWallpaper != null) {
                                SingleHandAdapter.scaleWallpaper.recycle();
                                SingleHandAdapter.scaleWallpaper = null;
                            }
                        }
                    }
                });
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateSingleHandMode() {
        Handler handler;
        final String value = Settings.Global.getString(this.mContext.getContentResolver(), "single_hand_mode");
        Slog.i(TAG, "update singleHandMode to: " + value + " from: " + this.mCurrentMode);
        if (value == null) {
            value = "";
        }
        if (HwExtDisplaySizeUtil.getInstance().hasSideInScreen() && (handler = this.mSideTouchHandler) != null) {
            handler.post(new Runnable() {
                /* class com.android.server.wm.SingleHandAdapter.AnonymousClass3 */

                public void run() {
                    HwSideTouchManager hwSTInstance = HwSideTouchManager.getInstance(SingleHandAdapter.this.mContext);
                    if (hwSTInstance != null) {
                        hwSTInstance.updateTPModeForSingleHandeMode(value);
                    }
                }
            });
        }
        if (IS_NOTCH_PROP) {
            if (this.mCoverManager == null) {
                this.mCoverManager = new CoverManager();
            }
            CoverManager coverManager = this.mCoverManager;
            if (coverManager != null) {
                this.mCoverOpen = coverManager.isCoverOpen();
            }
            mRoundedStateFlag = "".equals(value) ? 1 : 0;
            if (this.mCoverOpen && mRoundedStateFlagPre != mRoundedStateFlag) {
                Settings.Global.putInt(this.mContext.getContentResolver(), SHOW_ROUNDED_CORNERS, mRoundedStateFlag);
            }
            mRoundedStateFlagPre = mRoundedStateFlag;
        }
        if (mIsFirstEnteredSingleHandMode) {
            updateBlur();
            updateScaleWallpaperForBlur();
        }
        if (!value.equals(this.mCurrentMode)) {
            if (HwFreeFormUtils.isFreeFormEnable()) {
                HwFreeFormManager.getInstance(this.mContext).removeFloatListView();
                WindowManagerInternal windowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                if (windowManagerInternal != null && windowManagerInternal.isStackVisibleLw(5)) {
                    try {
                        ActivityTaskManager.getService().removeStacksInWindowingModes(new int[]{5});
                    } catch (RemoteException e) {
                        Slog.i(TAG, "onFreeFormOutLineChanged is RemoteException");
                    }
                }
            }
            this.mCurrentMode = value;
            synchronized (mLock) {
                if (!this.mOverlays.isEmpty()) {
                    Slog.i(TAG, "Dismissing all singhand window");
                    Iterator<SingleHandHandle> it = this.mOverlays.iterator();
                    while (it.hasNext()) {
                        it.next().dismissLocked();
                    }
                    this.mOverlays.clear();
                    if (!this.mEnterSinglehandwindow2s) {
                        StatisticalUtils.reportc(this.mContext, 43);
                        this.mEnterSinglehandwindow2s = false;
                    }
                    StatisticalUtils.reportc(this.mContext, 42);
                }
                if (!"".equals(value)) {
                    boolean left = value.contains(HwMultiWinConstants.LEFT_HAND_LAZY_MODE_STR);
                    this.mOverlays.add(new SingleHandHandle(left));
                    this.mEnterSinglehandwindow2s = false;
                    String targetStateName = "false";
                    if (left) {
                        targetStateName = "true";
                    }
                    StatisticalUtils.reporte(this.mContext, 41, String.format("{state:left=%s}", targetStateName));
                    if (this.mPaperHandler.hasMessages(2)) {
                        this.mPaperHandler.removeMessages(2);
                    }
                    this.mPaperHandler.sendEmptyMessageDelayed(2, 2000);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateScaleWallpaperForBlur() {
        synchronized (mLock) {
            if (scaleWallpaper != null) {
                scaleWallpaper.recycle();
                scaleWallpaper = null;
            }
            if (this.mBlurBitmap == null) {
                Slog.e(TAG, "blurBitmap is null");
                return;
            }
            int wwidth = (int) (((float) this.mBlurBitmap.getWidth()) * 1.0f);
            int hheight = (int) (((float) this.mBlurBitmap.getHeight()) * 1.0f);
            scaleWallpaper = Bitmap.createBitmap(wwidth, hheight, Bitmap.Config.ARGB_8888);
            if (scaleWallpaper == null) {
                Slog.e(TAG, "scaleWallpaper is null");
                return;
            }
            Canvas canvas = new Canvas(scaleWallpaper);
            Paint p = new Paint();
            p.setColor(-1845493760);
            canvas.drawBitmap(this.mBlurBitmap, 0.0f, 0.0f, (Paint) null);
            canvas.drawRect(0.0f, 0.0f, (float) this.mBlurBitmap.getWidth(), (float) this.mBlurBitmap.getHeight(), p);
            int[] inPixels = new int[(wwidth * hheight)];
            scaleWallpaper.getPixels(inPixels, 0, wwidth, 0, 0, wwidth, hheight);
            for (int y = 0; y < hheight; y++) {
                for (int x = 0; x < wwidth; x++) {
                    int index = (y * wwidth) + x;
                    inPixels[index] = -16777216 | inPixels[index];
                }
            }
            scaleWallpaper.setPixels(inPixels, 0, wwidth, 0, 0, wwidth, hheight);
            if (!this.mOverlays.isEmpty()) {
                Iterator<SingleHandHandle> it = this.mOverlays.iterator();
                while (it.hasNext()) {
                    it.next().onBlurWallpaperChanged();
                }
            }
        }
    }

    private final class SingleHandHandle {
        private final Runnable mDismissRunnable = new Runnable() {
            /* class com.android.server.wm.SingleHandAdapter.SingleHandHandle.AnonymousClass3 */

            public void run() {
                SingleHandWindow window = SingleHandHandle.this.mWindow;
                SingleHandWindow unused = SingleHandHandle.this.mWindow = null;
                if (window != null) {
                    window.dismiss();
                }
                SingleHandWindow window2 = SingleHandHandle.this.mWindowWalltop;
                SingleHandWindow unused2 = SingleHandHandle.this.mWindowWalltop = null;
                if (window2 != null) {
                    window2.dismiss();
                }
            }
        };
        /* access modifiers changed from: private */
        public final boolean mLeft;
        private final Runnable mShowRunnable = new Runnable() {
            /* class com.android.server.wm.SingleHandAdapter.SingleHandHandle.AnonymousClass1 */

            public void run() {
                SingleHandWindow window = new SingleHandWindow(SingleHandHandle.this.mLeft, "virtual", SingleHandAdapter.this.mDefaultDisplayInfo.logicalWidth, SingleHandAdapter.this.mDefaultDisplayInfo.logicalHeight, SingleHandAdapter.this.mService);
                window.show();
                SingleHandWindow unused = SingleHandHandle.this.mWindow = window;
            }
        };
        private final Runnable mShowRunnableWalltop = new Runnable() {
            /* class com.android.server.wm.SingleHandAdapter.SingleHandHandle.AnonymousClass2 */

            public void run() {
                SingleHandWindow window = new SingleHandWindow(SingleHandHandle.this.mLeft, "blurpapertop", SingleHandAdapter.this.mDefaultDisplayInfo.logicalWidth, SingleHandAdapter.this.mDefaultDisplayInfo.logicalHeight, SingleHandAdapter.this.mService);
                window.show();
                SingleHandWindow unused = SingleHandHandle.this.mWindowWalltop = window;
            }
        };
        /* access modifiers changed from: private */
        public SingleHandWindow mWindow;
        /* access modifiers changed from: private */
        public SingleHandWindow mWindowWalltop;

        public SingleHandHandle(boolean left) {
            this.mLeft = left;
            synchronized (SingleHandAdapter.mLock) {
                SingleHandAdapter.this.mUiHandler.post(this.mShowRunnableWalltop);
                SingleHandAdapter.this.mUiHandler.postDelayed(this.mShowRunnable, 10);
            }
        }

        public void dismissLocked() {
            SingleHandAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnable);
            SingleHandAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnableWalltop);
            SingleHandAdapter.this.mUiHandler.post(this.mDismissRunnable);
        }

        public void onBlurWallpaperChanged() {
            SingleHandWindow singleHandWindow = this.mWindowWalltop;
            if (singleHandWindow != null) {
                singleHandWindow.onBlurWallpaperChanged();
            }
        }
    }

    private class blurCallback implements AbsWallpaperManagerInner.IBlurWallpaperCallback {
        public blurCallback() {
        }

        public void onBlurWallpaperChanged() {
            SingleHandAdapter.this.updateBlur();
        }
    }

    /* access modifiers changed from: private */
    public void updateBlur() {
        Bitmap bitmap = null;
        if (!(this.mWallpaperManager == null || (bitmap = this.mWallpaperManager.getBlurBitmap(new Rect(0, 0, this.mDefaultDisplayInfo.logicalWidth, this.mDefaultDisplayInfo.logicalHeight))) == null)) {
            mIsFirstEnteredSingleHandMode = false;
        }
        synchronized (mLock) {
            this.mBlurBitmap = bitmap;
        }
        if (this.mPaperHandler.hasMessages(1)) {
            this.mPaperHandler.removeMessages(1);
        }
        this.mPaperHandler.sendEmptyMessageDelayed(1, 1000);
    }
}
