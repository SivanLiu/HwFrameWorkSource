package com.android.server.wm;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.HwMwUtils;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.internal.view.RotationPolicy;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.multiwin.HwBlur;
import com.android.server.multiwin.HwMultiWinUtils;
import com.android.server.multiwin.animation.HwSplitBarExitAniStrategy;
import com.android.server.multiwin.animation.HwSplitBarReboundStrategy;
import com.android.server.wm.HwMultiWindowSplitUI;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.os.UserHandleEx;
import java.util.List;

public class HwMultiWindowSplitUI {
    private static final int INVALID_POSITION = -200;
    private static final Object LOCK = new Object();
    private static final String TAG = "HwMultiWindowSplitUI";
    private static volatile HwMultiWindowSplitUI mSingleInstance = null;
    public static int sNavBarHeight;
    public static int sNavBarWidth;
    private static int sStatusBarHeight;
    private ActivityTaskManagerService mActivityTaskManagerService;
    private Bundle mAnimBundle = new Bundle();
    private View mBlackCoverInExitSplit;
    private int mBottom = 0;
    private Context mContext;
    private FrameLayout mCoverLayout = null;
    private WindowManager.LayoutParams mCoverLp = null;
    private int mCurrentSplitRatio;
    private int mDisplayHeight;
    /* access modifiers changed from: private */
    public int mDisplayId = 0;
    private int mDisplayWidth;
    private int mDividerBarHeight;
    private boolean mDown = false;
    private ImageView mDragLine;
    private ImageView mDragRegion;
    private View mDragView;
    private int mExitRegion = 0;
    private int mFloatState = 0;
    private Handler mHandler = null;
    private int mHeightColumns;
    /* access modifiers changed from: private */
    public HwMultiWindowManager mHwMultiWindowManager = null;
    private boolean mIsActionDown = false;
    private volatile boolean mIsAddedSplitBar = false;
    private boolean mIsDownAnimate = false;
    private boolean mIsLeftAndRightPos = false;
    private boolean mIsMoving = false;
    private boolean mIsNavBarMini = false;
    private boolean mIsNeedSetSplitBarVisible = false;
    private boolean mIsOneGear = false;
    private boolean mIsReadyToFull = false;
    private boolean mIsSetBackground = false;
    private boolean mIsSplitBarVisibleNow = false;
    /* access modifiers changed from: private */
    public Bitmap mLeftBitmap;
    /* access modifiers changed from: private */
    public RelativeLayout mLeftCover;
    private ImageView mLeftImg;
    private int mMoveCount = 0;
    private int mNavBarPos = -1;
    private int mNavBarPosWhenActionDown;
    private float mPreDownX;
    private float mPreDownY;
    /* access modifiers changed from: private */
    public Bitmap mRightBitmap;
    /* access modifiers changed from: private */
    public RelativeLayout mRightCover;
    private ImageView mRightImg;
    private int mRotation;
    private int mTouchSlop;
    private int mUiMode;
    private Vibrator mVibrator = null;
    private int mWidthColumns;
    private volatile int mWindowMode = -1;
    private int mWindowModeToAdded = -1;
    private int mWindowOffset;
    private int mWindowWH;
    private WindowManager mWm;
    public Rect primaryBounds;
    private int userRotationLocked = -1;

    private HwMultiWindowSplitUI(Context context, ActivityTaskManagerService service) {
        this.mContext = context;
        this.mActivityTaskManagerService = service;
        this.mUiMode = this.mContext.getResources().getConfiguration().uiMode & 48;
        initHandler(this.mActivityTaskManagerService.mUiHandler.getLooper());
        this.mHandler.post(new Runnable(context) {
            /* class com.android.server.wm.$$Lambda$HwMultiWindowSplitUI$Kd1tw4VhOOGjGBc_s_gtpE_k6cA */
            private final /* synthetic */ Context f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwMultiWindowSplitUI.this.lambda$new$0$HwMultiWindowSplitUI(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$new$0$HwMultiWindowSplitUI(Context context) {
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public static HwMultiWindowSplitUI getInstance(Context context, ActivityTaskManagerService service) {
        if (mSingleInstance == null) {
            synchronized (LOCK) {
                if (mSingleInstance == null) {
                    mSingleInstance = new HwMultiWindowSplitUI(context, service);
                }
            }
        }
        return mSingleInstance;
    }

    private void initHandler(Looper looper) {
        this.mHandler = new Handler(looper) {
            /* class com.android.server.wm.HwMultiWindowSplitUI.AnonymousClass1 */

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        HwMultiWindowSplitUI hwMultiWindowSplitUI = HwMultiWindowSplitUI.this;
                        hwMultiWindowSplitUI.createSplitCover(hwMultiWindowSplitUI.mDisplayId);
                        return;
                    case 1:
                        HwMultiWindowSplitUI.this.removeSplitBarWindow();
                        return;
                    case 2:
                        if (msg.obj instanceof Float) {
                            HwMultiWindowSplitUI.this.updateSplitBarPosition(msg.arg1, ((Float) msg.obj).floatValue());
                            return;
                        }
                        return;
                    case 3:
                        HwMultiWindowSplitUI.this.updatePositionOnConfigurationChanged();
                        return;
                    case 4:
                        HwMultiWindowSplitUI.this.setSplitBarVisibleIfNeeded();
                        return;
                    case 5:
                        HwMultiWindowSplitUI.this.updateSplitBarPos(msg.arg1);
                        return;
                    case 6:
                        HwMultiWindowSplitUI.this.updateSplitBarPos(-1);
                        HwMultiWindowSplitUI.this.updateSplitBarPos(1);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public void addDividerBarWindow(int displayId, int windowMode) {
        if (!(this.mWindowMode == windowMode || this.mWindowMode == -1)) {
            Slog.i(TAG, " windowMode to be changed, windowMode = " + windowMode + " mWindowMode = " + this.mWindowMode);
            this.mWindowModeToAdded = windowMode;
        }
        if (this.mCoverLp == null || this.mDragView.getVisibility() != 0) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 200);
            this.mIsAddedSplitBar = true;
            this.mWindowMode = windowMode;
            return;
        }
        Slog.i(TAG, "addDividerBarWindow, mCoverLp not null and mDragView visible");
        if (this.mIsLeftAndRightPos) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6), 100);
        }
    }

    /* access modifiers changed from: private */
    public void createSplitCover(int displayId) {
        Slog.i(TAG, "createSplitCover");
        this.mIsNeedSetSplitBarVisible = true;
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 350);
        if (this.mIsAddedSplitBar) {
            if (this.mContext == null) {
                this.mContext = this.mActivityTaskManagerService.mUiContext;
            }
            this.mWm = (WindowManager) this.mContext.getSystemService("window");
            this.mDisplayId = displayId;
            this.mHwMultiWindowManager = HwMultiWindowManager.getInstance(this.mActivityTaskManagerService);
            this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
            if (this.mCoverLp == null) {
                loadDimens();
            }
            if (this.mCoverLp != null) {
                setPosition();
            } else if (this.mCoverLayout == null) {
                Slog.i(TAG, "mCoverLayout is null");
            } else {
                this.mLeftImg = new ImageView(this.mContext);
                this.mRightImg = new ImageView(this.mContext);
                initDragView(this.mCoverLayout);
                setWindowParams();
                this.mIsSplitBarVisibleNow = true;
            }
        }
    }

    private void setWindowParams() {
        Slog.i(TAG, "set window params");
        this.mCoverLp = new WindowManager.LayoutParams();
        WindowManager.LayoutParams layoutParams = this.mCoverLp;
        layoutParams.flags = 545522441;
        layoutParams.privateFlags |= 64;
        WindowManager.LayoutParams layoutParams2 = this.mCoverLp;
        layoutParams2.type = HwArbitrationDEFS.MSG_QUERY_QOE_WM_TIMEOUT;
        layoutParams2.layoutInDisplayCutoutMode = 1;
        layoutParams2.layoutInDisplaySideMode = 1;
        layoutParams2.setTitle(HwSplitBarConstants.WINDOW_TITLE);
        setPosition();
        WindowManager.LayoutParams layoutParams3 = this.mCoverLp;
        layoutParams3.format = -3;
        layoutParams3.gravity = 51;
        this.mCoverLayout.setSystemUiVisibility(4);
        try {
            this.mWm.addView(this.mCoverLayout, this.mCoverLp);
        } catch (WindowManager.InvalidDisplayException e) {
            Slog.e(TAG, "setWindowParams add view error.");
        }
    }

    private void windowMax(View backgroundLayout, MotionEvent event) {
        createGaussianBlurCover(event);
        this.mLeftCover.setVisibility(0);
        this.mRightCover.setVisibility(0);
        boolean isNaviBarMini = isNavBarMini();
        Slog.i(TAG, " isNaviBarMini = " + isNaviBarMini + " sNavBarH = " + sNavBarHeight + " sNavBarW = " + sNavBarWidth);
        setLeftCoverParams(isNaviBarMini);
        if (this.mCoverLp != null) {
            this.mIsNavBarMini = isNaviBarMini;
            this.mNavBarPosWhenActionDown = this.mNavBarPos;
            setCoverLayoutParams(isNaviBarMini);
            this.mWm.updateViewLayout(backgroundLayout, this.mCoverLp);
            this.mDragView.setVisibility(0);
            if (HwMwUtils.IS_TABLET && HwMwUtils.ENABLED && this.mWindowMode == 103 && this.mLeftCover.getBackground() == null) {
                setCoverBackground();
            }
            addAppIcon();
        }
    }

    private void setLeftCoverParams(boolean isNaviBarMini) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.mLeftCover.getLayoutParams();
        this.mBottom = this.mHwMultiWindowManager.getPrimaryStackPos(this.mDisplayId);
        Slog.i(TAG, "windowMax bottom = " + this.mBottom);
        if (!this.mIsLeftAndRightPos) {
            params.height = this.mBottom - this.primaryBounds.top;
            params.width = this.mDisplayWidth - this.primaryBounds.left;
        } else if (!isNaviBarMini) {
            int i = this.mNavBarPos;
            if (i == 4) {
                params.width = this.mBottom - this.primaryBounds.left;
                params.height = (this.mDisplayHeight - this.primaryBounds.top) - sNavBarWidth;
            } else if (i == 1) {
                params.width = (this.mBottom - this.primaryBounds.left) - sNavBarWidth;
                params.height = this.mDisplayHeight - this.primaryBounds.top;
            } else {
                params.width = this.mBottom - this.primaryBounds.left;
                params.height = this.mDisplayHeight - this.primaryBounds.top;
            }
        } else {
            params.width = this.mBottom - this.primaryBounds.left;
            params.height = this.mDisplayHeight - this.primaryBounds.top;
        }
        this.mLeftCover.setLayoutParams(params);
    }

    private void setCoverLayoutParams(boolean isNaviBarMini) {
        this.mCoverLp.y = this.primaryBounds.top;
        this.mCoverLp.x = this.primaryBounds.left;
        int statusBarH = getNotchSizeOnRight();
        if (!isNaviBarMini) {
            int i = this.mNavBarPos;
            if (i == 4) {
                this.mCoverLp.height = (this.mDisplayHeight - this.primaryBounds.top) - sNavBarHeight;
                this.mCoverLp.width = (this.mDisplayWidth - this.primaryBounds.left) - statusBarH;
            } else if (i == 2 || i == 1) {
                this.mCoverLp.height = this.mDisplayHeight - this.primaryBounds.top;
                this.mCoverLp.width = ((this.mDisplayWidth - this.primaryBounds.left) - sNavBarWidth) - statusBarH;
            } else {
                this.mCoverLp.height = this.mDisplayHeight - this.primaryBounds.top;
                this.mCoverLp.width = (this.mDisplayWidth - this.primaryBounds.left) - statusBarH;
            }
        } else {
            this.mCoverLp.height = this.mDisplayHeight - this.primaryBounds.top;
            this.mCoverLp.width = (this.mDisplayWidth - this.primaryBounds.left) - statusBarH;
        }
    }

    private void addAppIcon() {
        Drawable rightDrawable;
        Drawable leftDrawable;
        Bundle bundle = this.mHwMultiWindowManager.getStackPackageNames();
        if (bundle != null) {
            List<String> packageName = bundle.getStringArrayList("pkgNames");
            List<Integer> userIds = bundle.getIntegerArrayList("pkgUserIds");
            if (packageName != null && userIds != null) {
                boolean isMagic = this.mWindowMode == 103 && packageName.size() == 1 && userIds.size() == 1;
                boolean isSplit = (this.mWindowMode == 100 && packageName.size() == 2) || userIds.size() == 2;
                if (isMagic) {
                    leftDrawable = getAppIcon(packageName.get(0), userIds.get(0).intValue());
                    rightDrawable = getAppIcon(packageName.get(0), userIds.get(0).intValue());
                } else if (isSplit) {
                    leftDrawable = getAppIcon(packageName.get(0), userIds.get(0).intValue());
                    rightDrawable = getAppIcon(packageName.get(1), userIds.get(1).intValue());
                } else {
                    Slog.i(TAG, "mWindowMode = " + this.mWindowMode + " package count = " + packageName.size() + " userId count = " + userIds.size());
                    return;
                }
                if (leftDrawable == null || rightDrawable == null) {
                    Slog.i(TAG, "leftDrawable or rightDrawable is null. leftDrawable = " + leftDrawable + " rightDrawable = " + rightDrawable);
                }
                this.mLeftImg.setBackground(leftDrawable);
                this.mRightImg.setBackground(rightDrawable);
                if (this.mLeftImg.getParent() == null && this.mRightImg.getParent() == null) {
                    Interpolator standardCurve = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
                    addIconViewAnim(this.mLeftCover, this.mLeftImg, standardCurve);
                    addIconViewAnim(this.mRightCover, this.mRightImg, standardCurve);
                    return;
                }
                Slog.i(TAG, "addAppIcon, mLeftImg has parent already");
            }
        }
    }

    private Drawable getAppIcon(String pkgName, int userId) {
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            if (packageManager == null) {
                return null;
            }
            Drawable icon = packageManager.getUserBadgedIcon(packageManager.getApplicationIcon(packageManager.getApplicationInfoAsUser(pkgName, 0, userId)), UserHandleEx.getUserHandle(userId));
            if (icon != null) {
                icon.setAlpha(255);
            }
            return icon;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "NameNotFoundException");
            return null;
        }
    }

    private void addIconViewAnim(RelativeLayout layout, ImageView icon, Interpolator standardCurve) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(-2, -2);
        params.addRule(13, -1);
        layout.addView(icon, params);
        icon.setScaleX(0.85f);
        icon.setScaleY(0.85f);
        icon.setAlpha(0.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(icon, View.ALPHA, icon.getAlpha(), 1.0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, icon.getScaleX(), 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, icon.getScaleX(), 1.0f);
        AnimatorSet sets = new AnimatorSet();
        sets.playTogether(alpha, scaleX, scaleY);
        sets.setDuration(200L);
        sets.setInterpolator(standardCurve);
        sets.start();
    }

    private void initDragView(View backgroundLayout) {
        this.mDragView = backgroundLayout.findViewById(34603249);
        this.mDragLine = (ImageView) backgroundLayout.findViewById(34603248);
        this.mDragRegion = (ImageView) backgroundLayout.findViewById(34603247);
        this.mDragRegion.setOnTouchListener(new View.OnTouchListener(backgroundLayout) {
            /* class com.android.server.wm.$$Lambda$HwMultiWindowSplitUI$brKiWjKFoKgZDS8LoIgYshv6pTk */
            private final /* synthetic */ View f$1;

            {
                this.f$1 = r2;
            }

            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return HwMultiWindowSplitUI.this.lambda$initDragView$1$HwMultiWindowSplitUI(this.f$1, view, motionEvent);
            }
        });
        if (this.mWindowMode == 103) {
            this.mDragView.setVisibility(0);
        } else {
            this.mDragView.setVisibility(4);
        }
    }

    public /* synthetic */ boolean lambda$initDragView$1$HwMultiWindowSplitUI(View backgroundLayout, View v, MotionEvent event) {
        int action = event.getAction();
        if (action != 0) {
            if (action == 1) {
                Slog.i(TAG, "ACTION_UP ");
                updateCoverViewGone(event);
            } else if (action == 2) {
                executeActionMove(event, backgroundLayout);
            } else if (action == 3) {
                Slog.i(TAG, "ACTION_CANCEL");
                updateCoverViewGone(event);
            }
            return true;
        }
        Slog.i(TAG, "ACTION_DOWN");
        initActionDown(event, backgroundLayout);
        return true;
    }

    private boolean isDragZoneForMagicWin(MotionEvent event) {
        if (event.getRawX() >= ((float) (this.mDisplayWidth / 4))) {
            float rawX = event.getRawX();
            int i = this.mDisplayWidth;
            if (rawX <= ((float) (i - (i / 4)))) {
                return true;
            }
        }
        return false;
    }

    private void initActionDown(MotionEvent event, View view) {
        if (this.mCoverLp != null) {
            this.userRotationLocked = Settings.System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, -2);
            Slog.i(TAG, " userRotationLocked = " + this.userRotationLocked);
            RotationPolicy.setRotationLockAtAngle(this.mContext, true, this.mRotation);
            this.mBottom = this.mHwMultiWindowManager.getPrimaryStackPos(this.mDisplayId);
            Slog.i(TAG, " primaryBounds = " + this.primaryBounds);
            this.mExitRegion = 0;
            this.mDragView.setVisibility(8);
            this.mMoveCount = 0;
            this.mDown = true;
            this.mIsActionDown = true;
            this.mIsSetBackground = false;
            this.mCoverLp.flags &= -9;
            this.mWm.updateViewLayout(view, this.mCoverLp);
            this.mPreDownX = event.getRawX();
            this.mPreDownY = event.getRawY();
            this.mFloatState = HwMultiWinUtils.getFloatTaskState(this.mContext);
            Slog.i(TAG, " split bar action down, the float task state: " + this.mFloatState);
            HwMultiWinUtils.putFloatTaskStateToSettings(false, this.mContext);
            setDateBundleInActionDown();
            this.mIsMoving = false;
        }
    }

    private boolean executeActionMove(MotionEvent event, View view) {
        this.mMoveCount++;
        if (this.mMoveCount < 2) {
            Slog.i(TAG, "count smaller than five ,count = " + this.mMoveCount);
            return true;
        }
        if (!HwMwUtils.IS_TABLET || !HwMwUtils.ENABLED || this.mWindowMode != 103 || isDragZoneForMagicWin(event)) {
            if (this.mDown) {
                windowMax(view, event);
                this.mDown = false;
                return true;
            }
            if (!this.mIsSetBackground && this.mLeftCover.getBackground() != null && this.mCoverLp.width > this.mWindowWH && this.mCoverLp.height > this.mWindowWH) {
                this.mCoverLayout.setBackgroundColor(-16777216);
                this.mIsSetBackground = true;
            }
            if (this.mIsMoving || exceededTouchSlop(event)) {
                upadateCoverSize(event);
                return true;
            }
            updateCoverLpIfNeed();
        }
        return false;
    }

    private void updateCoverLpIfNeed() {
        boolean isNavBarMini = isNavBarMini();
        if (isNavBarMini != this.mIsNavBarMini && isNavBarMini) {
            int i = this.mNavBarPosWhenActionDown;
            if (i == 4) {
                this.mCoverLp.height += sNavBarHeight;
            } else if (i == 1 || i == 2) {
                this.mCoverLp.width += sNavBarWidth;
            }
            this.mWm.updateViewLayout(this.mCoverLayout, this.mCoverLp);
            this.mIsNavBarMini = isNavBarMini;
        }
    }

    private boolean exceededTouchSlop(MotionEvent event) {
        if (((double) (Math.abs((this.mIsLeftAndRightPos ? event.getRawX() : event.getRawY()) - (this.mIsLeftAndRightPos ? this.mPreDownX : this.mPreDownY)) - ((float) this.mTouchSlop))) <= 1.0E-5d) {
            return false;
        }
        if (!this.mIsMoving) {
            this.mIsMoving = true;
        }
        return true;
    }

    private void upadateCoverSize(MotionEvent event) {
        float mDownX = event.getRawX();
        float mDownY = event.getRawY();
        if (this.mCoverLp != null) {
            dragToFullScreen(event);
            RelativeLayout.LayoutParams paramsLeftCover = (RelativeLayout.LayoutParams) this.mLeftCover.getLayoutParams();
            updateCoverLpIfNeed();
            if (this.mIsLeftAndRightPos) {
                paramsLeftCover.width = ((int) mDownX) - this.primaryBounds.left;
                paramsLeftCover.height = this.mCoverLp.height;
            } else {
                paramsLeftCover.height = ((int) mDownY) - this.primaryBounds.top;
            }
            this.mLeftCover.setLayoutParams(paramsLeftCover);
        }
    }

    public boolean isNavBarMini() {
        DisplayContent displayContent = this.mActivityTaskManagerService.getRootActivityContainer().getActivityDisplay(this.mDisplayId).mDisplayContent;
        boolean isNavBarMini = displayContent.getDisplayPolicy().mHwDisplayPolicyEx.isNaviBarMini();
        this.mNavBarPos = displayContent.getDisplayPolicy().navigationBarPosition(displayContent.getDisplayInfo().logicalWidth, displayContent.getDisplayInfo().logicalHeight, displayContent.getRotation());
        return isNavBarMini;
    }

    public int getNotchSizeOnRight() {
        if (!HwMultiWindowManager.IS_NOTCH_PROP || this.mRotation != 3 || !this.mIsLeftAndRightPos) {
            return 0;
        }
        return sStatusBarHeight;
    }

    public int getNavBarBottomUpDown() {
        if (isNavBarMini() || this.mNavBarPos != 4 || this.mIsLeftAndRightPos) {
            return 0;
        }
        return sNavBarHeight;
    }

    public int getNavBarRight() {
        if (isNavBarMini() || this.mNavBarPos != 2 || !this.mIsLeftAndRightPos) {
            return 0;
        }
        return sNavBarWidth;
    }

    private void createGaussianBlurCover(final MotionEvent event) {
        new Thread() {
            /* class com.android.server.wm.HwMultiWindowSplitUI.AnonymousClass2 */

            public void run() {
                setName("GaussianBlur");
                ActivityStack stackPrimary = HwMultiWindowSplitUI.this.mHwMultiWindowManager.getSplitScreenTopStack();
                if (stackPrimary == null || !stackPrimary.inHwMagicWindowingMode()) {
                    ActivityStack stackPrimary2 = HwMultiWindowSplitUI.this.mHwMultiWindowManager.getSplitScreenPrimaryStack();
                    if (stackPrimary2 == null || stackPrimary2.getChildAt(0) == null) {
                        Slog.i(HwMultiWindowSplitUI.TAG, "createGaussianBlurCover, stackPrimary null");
                        return;
                    }
                    int[] taskIds = HwMultiWindowSplitUI.this.mHwMultiWindowManager.getCombinedSplitScreenTaskIds(stackPrimary2);
                    if (taskIds == null || taskIds.length == 0) {
                        Slog.i(HwMultiWindowSplitUI.TAG, "createGaussianBlurCover, getCombinedSplitScreenTaskIDs return null");
                        return;
                    }
                    int stackPrimaryId = stackPrimary2.getChildAt(0).taskId;
                    HwMultiWindowSplitUI hwMultiWindowSplitUI = HwMultiWindowSplitUI.this;
                    Bitmap unused = hwMultiWindowSplitUI.mLeftBitmap = hwMultiWindowSplitUI.getTaskSnapshotBitmap(stackPrimaryId, false);
                    HwMultiWindowSplitUI hwMultiWindowSplitUI2 = HwMultiWindowSplitUI.this;
                    Bitmap unused2 = hwMultiWindowSplitUI2.mRightBitmap = hwMultiWindowSplitUI2.getTaskSnapshotBitmap(taskIds[0], false);
                } else {
                    Bitmap[] tmpBitmaps = HwMultiWindowSplitUI.this.createCoverBlurBitmap(event);
                    if (tmpBitmaps == null || tmpBitmaps.length <= 1 || tmpBitmaps[0] == null || tmpBitmaps[1] == null) {
                        HwMultiWindowSplitUI.this.setCoverBackground();
                        return;
                    } else {
                        Bitmap unused3 = HwMultiWindowSplitUI.this.mLeftBitmap = tmpBitmaps[0];
                        Bitmap unused4 = HwMultiWindowSplitUI.this.mRightBitmap = tmpBitmaps[1];
                    }
                }
                if (HwMultiWindowSplitUI.this.mLeftBitmap == null || HwMultiWindowSplitUI.this.mRightBitmap == null) {
                    Slog.i(HwMultiWindowSplitUI.TAG, "createGaussianBlurCover, mLeftBitmap null");
                    return;
                }
                HwMultiWindowSplitUI.this.mLeftCover.post(new Runnable(HwBlur.blur(HwMultiWindowSplitUI.this.mLeftBitmap, 200, 20, true), HwBlur.blur(HwMultiWindowSplitUI.this.mRightBitmap, 200, 20, true)) {
                    /* class com.android.server.wm.$$Lambda$HwMultiWindowSplitUI$2$xw12UlQ60XwENkVA_2jRoJrGwgw */
                    private final /* synthetic */ Bitmap f$1;
                    private final /* synthetic */ Bitmap f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        HwMultiWindowSplitUI.AnonymousClass2.this.lambda$run$0$HwMultiWindowSplitUI$2(this.f$1, this.f$2);
                    }
                });
            }

            public /* synthetic */ void lambda$run$0$HwMultiWindowSplitUI$2(Bitmap left, Bitmap right) {
                HwMultiWindowSplitUI hwMultiWindowSplitUI = HwMultiWindowSplitUI.this;
                hwMultiWindowSplitUI.setCoverCorner(hwMultiWindowSplitUI.mLeftCover, left);
                HwMultiWindowSplitUI hwMultiWindowSplitUI2 = HwMultiWindowSplitUI.this;
                hwMultiWindowSplitUI2.setCoverCorner(hwMultiWindowSplitUI2.mRightCover, right);
            }
        }.start();
    }

    /* access modifiers changed from: private */
    public void setCoverCorner(RelativeLayout layout, Bitmap bitmap) {
        layout.setBackground(new BitmapDrawable(bitmap));
        layout.setClipToOutline(true);
        layout.setOutlineProvider(ViewOutlineProvider.HW_MULTIWINDOW_SPLITSCREEN_OUTLINE_PROVIDER);
        View rect = getBoarderRect();
        if (rect != null && rect.getParent() == null && this.mUiMode == 32) {
            layout.addView(rect);
        }
    }

    /* access modifiers changed from: private */
    public Bitmap getTaskSnapshotBitmap(int taskId, boolean reducedResolution) {
        ActivityManager.TaskSnapshot snapshot = HwActivityTaskManager.getTaskSnapshot(taskId, reducedResolution);
        if (snapshot == null) {
            return null;
        }
        return Bitmap.wrapHardwareBuffer(HardwareBuffer.createFromGraphicBuffer(snapshot.getSnapshot()), snapshot.getColorSpace());
    }

    private Bitmap takeScreenshot() {
        Bitmap screenShot = SurfaceControl.screenshot(new Rect(0, 0, this.mDisplayWidth, this.mDisplayHeight), this.mDisplayWidth, this.mDisplayHeight, 1);
        if (screenShot != null) {
            screenShot.setHasAlpha(false);
        }
        return screenShot;
    }

    /* access modifiers changed from: private */
    public Bitmap[] createCoverBlurBitmap(MotionEvent event) {
        ActivityStack stackPrimary;
        Rect[] dragBounds;
        Bitmap screenShot = takeScreenshot();
        if (screenShot == null || (stackPrimary = this.mHwMultiWindowManager.getSplitScreenTopStack()) == null || (dragBounds = this.mHwMultiWindowManager.getRectForScreenShotForDrag(changeSplitRatio(event, stackPrimary.inHwMagicWindowingMode()))) == null || dragBounds.length < 2) {
            return null;
        }
        try {
            return new Bitmap[]{Bitmap.createBitmap(screenShot, dragBounds[0].left, dragBounds[0].top, dragBounds[0].width(), dragBounds[0].height()), Bitmap.createBitmap(screenShot, dragBounds[1].left, dragBounds[1].top, dragBounds[1].width(), dragBounds[1].height())};
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "create bitmap fail.");
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void setCoverBackground() {
        this.mLeftCover.post(new Runnable() {
            /* class com.android.server.wm.HwMultiWindowSplitUI.AnonymousClass3 */

            public void run() {
                HwMultiWindowSplitUI.this.mLeftCover.setBackgroundResource(33751924);
                HwMultiWindowSplitUI.this.mRightCover.setBackgroundResource(33751924);
            }
        });
    }

    private void updateCoverViewGone(MotionEvent event) {
        if (this.mFloatState == 1) {
            HwMultiWinUtils.putFloatTaskStateToSettings(true, this.mContext);
        }
        WindowManager.LayoutParams layoutParams = this.mCoverLp;
        if (layoutParams != null) {
            int delayed = 0;
            this.mMoveCount = 0;
            layoutParams.flags |= 8;
            ActivityStack activityStack = this.mHwMultiWindowManager.getSplitScreenTopStack();
            if (activityStack != null) {
                int type = changeSplitRatio(event, activityStack.inHwMagicWindowingMode());
                if (activityStack.inHwMagicWindowingMode()) {
                    this.mHwMultiWindowManager.resizeMagicWindowBounds(activityStack, type);
                } else {
                    this.mHwMultiWindowManager.resizeHwSplitStacks(type, true);
                }
                float endPosition = (float) this.mHwMultiWindowManager.getPrimaryStackPos(this.mDisplayId);
                float curPosition = this.mIsLeftAndRightPos ? event.getRawX() : event.getRawY();
                setDataBundleInActionUp(curPosition, endPosition);
                this.mAnimBundle.putInt(HwSplitBarConstants.SPLIT_RATIO, type);
                this.mIsActionDown = false;
                if (this.mExitRegion != 0) {
                    updateSplitBarPosition(type, curPosition);
                } else if (!this.mIsMoving || (this.mWindowMode == 103 && !isDragZoneForMagicWin(event))) {
                    Message msg = this.mHandler.obtainMessage(2);
                    msg.arg1 = type;
                    msg.obj = Float.valueOf(curPosition);
                    if (this.mIsMoving) {
                        delayed = 200;
                    }
                    this.mHandler.sendMessageDelayed(msg, (long) delayed);
                } else {
                    HwSplitBarReboundStrategy strategy = HwSplitBarReboundStrategy.getStrategy(this.mActivityTaskManagerService, this.mLeftCover, this.mDragView, this.mRightCover, this.mAnimBundle);
                    if (strategy == null) {
                        updateSplitBarPosition(type, curPosition);
                    } else {
                        strategy.startReboundAnim();
                    }
                }
            }
        }
    }

    public void updateSplitBarPosition(int type, float curPos) {
        boolean z = false;
        this.mIsSetBackground = false;
        if (this.mCoverLp != null) {
            if (!(type == 3 || type == 4)) {
                this.mLeftCover.removeAllViews();
                this.mRightCover.removeAllViews();
                this.mLeftCover.setVisibility(8);
                this.mRightCover.setVisibility(8);
                this.mDragView.setVisibility(0);
                this.mCoverLayout.setBackgroundColor(0);
                if (this.mIsLeftAndRightPos) {
                    this.mCoverLp.width = this.mWindowWH;
                } else {
                    this.mCoverLp.height = this.mWindowWH;
                }
            }
            updateViewPos(type, curPos);
            Context context = this.mContext;
            if (this.userRotationLocked == 0) {
                z = true;
            }
            RotationPolicy.setRotationLock(context, z);
            this.userRotationLocked = -1;
        }
    }

    private void updateViewPos(int displayType, float curPos) {
        if (this.mCoverLp != null) {
            this.mBottom = this.mHwMultiWindowManager.getPrimaryStackPos(this.mDisplayId);
            Slog.i(TAG, "updateViewPos bottom = " + this.mBottom);
            if (displayType == 0 || displayType == 1 || displayType == 2) {
                int i = this.mBottom;
                if (i == 0) {
                    Slog.i(TAG, "updateViewPos fail, will remove split bar.");
                    removeSplit(this.mWindowMode, true);
                    return;
                }
                if (this.mIsLeftAndRightPos) {
                    this.mCoverLp.x = i - this.mWindowOffset;
                } else {
                    this.mCoverLp.y = i - this.mWindowOffset;
                }
                this.mWm.updateViewLayout(this.mCoverLayout, this.mCoverLp);
            } else if (displayType == 3 || displayType == 4) {
                Slog.i(TAG, "updateViewPos, to full screen exitRegion = " + this.mExitRegion);
                if (this.mExitRegion == 0) {
                    this.mHwMultiWindowManager.removeSplitScreenDividerBar(this.mWindowMode, false);
                    return;
                }
                setDataBundleInActionUp(curPos, (float) this.mBottom);
                HwSplitBarExitAniStrategy strategy = HwSplitBarExitAniStrategy.getStrategy(this.mActivityTaskManagerService, this.mLeftCover, this.mDragView, this.mRightCover, this.mAnimBundle);
                if (strategy != null) {
                    strategy.split2FullAnimation();
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void updateViewPos(int displayType) {
        updateViewPos(displayType, 0.0f);
    }

    public void setLayoutBackground() {
        this.mCoverLayout.setBackgroundColor(0);
    }

    private int changeSplitRatio(MotionEvent event, boolean isMagicWindow) {
        float eventY = this.mIsLeftAndRightPos ? event.getRawX() : event.getRawY();
        int displayLen = this.mIsLeftAndRightPos ? this.mDisplayWidth : this.mDisplayHeight;
        int ratio = this.mIsLeftAndRightPos ? this.mWidthColumns : this.mHeightColumns;
        int offSet = this.mIsLeftAndRightPos ? this.primaryBounds.left : this.primaryBounds.top;
        float rightExitThreshold = (float) ((((((ratio - 1) * displayLen) / ratio) - getNotchSizeOnRight()) - getNavBarBottomUpDown()) - getNavBarRight());
        if (this.mIsOneGear) {
            if (rightExitThreshold < eventY) {
                return isMagicWindow ? 2 : 3;
            }
            if (eventY < ((float) ((displayLen / ratio) + offSet))) {
                return isMagicWindow ? 1 : 4;
            }
            return 0;
        } else if (eventY < ((float) ((displayLen / ratio) + offSet))) {
            return isMagicWindow ? 1 : 4;
        } else {
            if (eventY > rightExitThreshold) {
                return isMagicWindow ? 2 : 3;
            }
            if (eventY < ((float) offSet) + (((float) displayLen) * 0.42f)) {
                return 1;
            }
            return eventY > ((float) offSet) + (((float) displayLen) * 0.58f) ? 2 : 0;
        }
    }

    public void removeSplit(int windowMode, boolean immediately) {
        Slog.i(TAG, "removeSplit");
        if (immediately || (this.mIsAddedSplitBar && windowMode == this.mWindowMode && !this.mIsActionDown)) {
            Message msg = this.mHandler.obtainMessage(1);
            this.mHandler.removeMessages(0);
            this.mHandler.removeMessages(4);
            this.mHandler.removeMessages(6);
            this.mHandler.sendMessage(msg);
            return;
        }
        Slog.i(TAG, "removeSplit, mIsAddedSplitBar:" + this.mIsAddedSplitBar + " windowMode:" + windowMode + "," + this.mWindowMode + " mIsActionDown:" + this.mIsActionDown);
    }

    /* access modifiers changed from: private */
    public void removeSplitBarWindow() {
        if (this.mCoverLp == null) {
            Slog.i(TAG, "mCoverLp is null");
            return;
        }
        if (this.userRotationLocked == -1) {
            this.userRotationLocked = Settings.System.getIntForUser(this.mContext.getContentResolver(), "accelerometer_rotation", 0, -2);
        }
        RotationPolicy.setRotationLock(this.mContext, this.userRotationLocked == 0);
        this.userRotationLocked = -1;
        this.mCoverLayout.setVisibility(8);
        this.mRightCover.setVisibility(8);
        this.mDragView.setVisibility(8);
        this.mWm.removeView(this.mCoverLayout);
        this.mCoverLp = null;
        this.mWindowMode = -1;
        this.mIsAddedSplitBar = false;
        this.mIsActionDown = false;
        Slog.i(TAG, "remove split bar end");
        HwMultiWindowManager.getInstance(this.mActivityTaskManagerService).mIsAddSplitBar = false;
        addSplitBarIfNeeded();
    }

    private void addSplitBarIfNeeded() {
        int i = this.mWindowModeToAdded;
        if (i != -1 && i != this.mWindowMode) {
            Slog.i(TAG, "add split bar after remove if needed. mWindowModeToAdded:" + this.mWindowModeToAdded + " mWindowMode:" + this.mWindowMode);
            this.mIsAddedSplitBar = true;
            createSplitCover(this.mDisplayId);
            this.mWindowMode = this.mWindowModeToAdded;
            this.mWindowModeToAdded = -1;
        }
    }

    public void onConfigurationChanged(int displayId) {
        if (this.mDisplayId != displayId) {
            Slog.i(TAG, "displayId has changed");
            return;
        }
        Bundle bundle = getDisplayWidthAndHeight(displayId);
        if (bundle == null) {
            Slog.i(TAG, "get display width and height failed");
            return;
        }
        int height = bundle.getInt(HwSplitBarConstants.DISPLAY_HEIGHT);
        int width = bundle.getInt(HwSplitBarConstants.DISPLAY_WIDTH);
        int rotation = bundle.getInt(HwSplitBarConstants.DISPLAY_ROTATION);
        int currentUiMode = this.mContext.getResources().getConfiguration().uiMode & 48;
        Slog.i(TAG, "mUiMode = " + this.mUiMode + "  current ui mode = " + currentUiMode);
        if (this.mUiMode == currentUiMode && this.mDisplayHeight == height && this.mDisplayWidth == width && rotation == this.mRotation) {
            Slog.i(TAG, "orientation has not changed, mDisplayWidth: " + this.mDisplayWidth + ", current height: " + height);
            return;
        }
        this.mUiMode = currentUiMode;
        this.mDisplayWidth = width;
        this.mDisplayHeight = height;
        this.mRotation = rotation;
        if (this.mCoverLp == null) {
            Slog.i(TAG, "onConfigurationChanged mCoverLp null");
            return;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3));
    }

    /* access modifiers changed from: private */
    public void updatePositionOnConfigurationChanged() {
        if (this.mCoverLp != null) {
            this.mWm.removeView(this.mCoverLayout);
            loadDimens();
            initDragView(this.mCoverLayout);
            setPosition();
            this.mWm.addView(this.mCoverLayout, this.mCoverLp);
            this.mDragView.setVisibility(0);
        }
    }

    private void setPosition() {
        this.mBottom = this.mHwMultiWindowManager.getPrimaryStackPos(this.mDisplayId);
        Slog.i(TAG, "set position, bottom = " + this.mBottom);
        if (this.mBottom == 0) {
            Slog.i(TAG, "updateViewPos fail, will remove split bar.");
            removeSplit(this.mWindowMode, true);
            return;
        }
        boolean isNavBarMini = isNavBarMini();
        if (this.mIsLeftAndRightPos) {
            if (isNavBarMini || this.mNavBarPos != 4) {
                this.mCoverLp.height = this.mDisplayHeight - this.primaryBounds.top;
            } else {
                this.mCoverLp.height = (this.mDisplayHeight - this.primaryBounds.top) - sNavBarHeight;
            }
            WindowManager.LayoutParams layoutParams = this.mCoverLp;
            layoutParams.width = this.mWindowWH;
            layoutParams.y = 0;
            layoutParams.x = this.mBottom - this.mWindowOffset;
            return;
        }
        WindowManager.LayoutParams layoutParams2 = this.mCoverLp;
        layoutParams2.height = this.mWindowWH;
        layoutParams2.width = this.mDisplayWidth - this.primaryBounds.left;
        WindowManager.LayoutParams layoutParams3 = this.mCoverLp;
        layoutParams3.y = this.mBottom - this.mWindowOffset;
        layoutParams3.x = 0;
    }

    private void loadDimens() {
        this.mWindowWH = this.mContext.getResources().getDimensionPixelSize(34472538);
        this.mDividerBarHeight = this.mContext.getResources().getDimensionPixelSize(34472536);
        this.mWindowOffset = this.mContext.getResources().getDimensionPixelSize(34472537);
        sNavBarWidth = this.mContext.getResources().getDimensionPixelSize(17105310);
        sNavBarHeight = this.mContext.getResources().getDimensionPixelSize(17105305);
        sStatusBarHeight = this.mContext.getResources().getDimensionPixelSize(17105443);
        this.mBlackCoverInExitSplit = new View(this.mContext);
        this.mBlackCoverInExitSplit.setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
        this.mBlackCoverInExitSplit.setBackgroundColor(Color.parseColor(HwSplitBarConstants.EXIT_TO_FULL_BLACK_VIEW_COLOR));
        Bundle bundle = this.mHwMultiWindowManager.getSplitGearsByDisplayId(this.mDisplayId);
        if (bundle != null) {
            this.mWidthColumns = bundle.getInt(HwMultiWindowManager.WIDTH_COLUMNS);
            this.mHeightColumns = bundle.getInt(HwMultiWindowManager.HEIGHT_COLUMNS);
            Slog.i(TAG, "width columns = " + this.mWidthColumns + " height columns = " + this.mHeightColumns);
            if (bundle.getInt(HwMultiWindowManager.HW_SPLIT_SCREEN_PRIMARY_POSITION) == 1) {
                Slog.i(TAG, "left and right split screen");
                this.mIsLeftAndRightPos = true;
                this.mCoverLayout = (FrameLayout) LayoutInflater.from(this.mContext).inflate(34013361, (ViewGroup) null);
            } else {
                Slog.i(TAG, "up and down split screen");
                this.mIsLeftAndRightPos = false;
                this.mCoverLayout = (FrameLayout) LayoutInflater.from(this.mContext).inflate(34013362, (ViewGroup) null);
            }
            this.mLeftCover = (RelativeLayout) this.mCoverLayout.findViewById(34603259);
            this.mRightCover = (RelativeLayout) this.mCoverLayout.findViewById(34603272);
            this.mLeftCover.setVisibility(8);
            this.mRightCover.setVisibility(8);
            float[] splitRatios = bundle.getFloatArray(HwMultiWindowManager.HW_SPLIT_SCREEN_RATIO_VALUES);
            if (splitRatios != null && splitRatios.length != 0) {
                if (splitRatios.length == 1) {
                    this.mIsOneGear = true;
                } else {
                    this.mIsOneGear = false;
                }
            }
        }
    }

    private Bundle getDisplayWidthAndHeight(int displayId) {
        ActivityDisplay display = this.mActivityTaskManagerService.getRootActivityContainer().getActivityDisplay(displayId);
        Bundle bundle = new Bundle();
        if (display == null) {
            return bundle;
        }
        bundle.putInt(HwSplitBarConstants.DISPLAY_WIDTH, display.mDisplayContent.getDisplayInfo().logicalWidth);
        bundle.putInt(HwSplitBarConstants.DISPLAY_HEIGHT, display.mDisplayContent.getDisplayInfo().logicalHeight);
        bundle.putInt(HwSplitBarConstants.DISPLAY_ROTATION, display.mDisplayContent.getDisplayInfo().rotation);
        return bundle;
    }

    public void onSystemReady(int displayId) {
        Bundle bundle = getDisplayWidthAndHeight(displayId);
        if (bundle == null) {
            Slog.i(TAG, "get display width and height failed in createSplitCover");
            return;
        }
        this.mDisplayWidth = bundle.getInt(HwSplitBarConstants.DISPLAY_WIDTH);
        this.mDisplayHeight = bundle.getInt(HwSplitBarConstants.DISPLAY_HEIGHT);
        this.mRotation = bundle.getInt(HwSplitBarConstants.DISPLAY_ROTATION);
    }

    public void setSplitBarVisibility(boolean isVisibility) {
        View view;
        HwSurfaceInNotch surface;
        this.mHandler.removeMessages(4);
        if (this.mCoverLp == null || (view = this.mDragView) == null) {
            Slog.i(TAG, "setSplitBarVisibility, mCoverLp is null");
            return;
        }
        this.mIsSplitBarVisibleNow = isVisibility;
        if ((!isVisibility || view.getVisibility() != 0) && (isVisibility || this.mDragView.getVisibility() == 0)) {
            int visible = isVisibility ? 0 : 4;
            Slog.i(TAG, "set splitBar visible = " + visible);
            if (visible == 4 && (surface = HwMultiWindowManager.getInstance(this.mActivityTaskManagerService).mSurfaceInNotch) != null) {
                surface.remove();
            }
            ((ActivityTaskManagerService) this.mActivityTaskManagerService).mUiHandler.post(new Runnable(visible, isVisibility) {
                /* class com.android.server.wm.$$Lambda$HwMultiWindowSplitUI$h_3dkpW_dR18ucYsOqoJXuH6HA */
                private final /* synthetic */ int f$1;
                private final /* synthetic */ boolean f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    HwMultiWindowSplitUI.this.lambda$setSplitBarVisibility$2$HwMultiWindowSplitUI(this.f$1, this.f$2);
                }
            });
            return;
        }
        Slog.i(TAG, "set splitBar visible return because already visible:" + isVisibility);
    }

    public /* synthetic */ void lambda$setSplitBarVisibility$2$HwMultiWindowSplitUI(int visible, boolean isVisibility) {
        if (this.mCoverLp != null) {
            ImageView imageView = this.mDragRegion;
            if (imageView != null) {
                imageView.setVisibility(visible);
            }
            if (isVisibility && this.mCoverLp.y < 0) {
                setPosition();
                updateCoverLayout();
            }
            if (isVisibility) {
                this.mDragView.setVisibility(visible);
                this.mIsNeedSetSplitBarVisible = false;
            } else if (!isVisibility) {
                this.mDragView.setVisibility(visible);
                this.mIsNeedSetSplitBarVisible = true;
            }
        }
    }

    private View getBoarderRect() {
        float f;
        View rectFrameLayout = LayoutInflater.from(this.mContext).inflate(34013359, (ViewGroup) null);
        View rectFrameView = rectFrameLayout.findViewById(34603267);
        if (rectFrameView == null) {
            Slog.e(TAG, "updateView: rect frame view null.");
            return rectFrameLayout;
        }
        Drawable bg = rectFrameView.getBackground();
        if (bg == null) {
            bg = new GradientDrawable();
            rectFrameView.setBackground(bg);
        }
        if (!(bg instanceof GradientDrawable)) {
            Slog.e(TAG, "updateView: type error.");
            return rectFrameLayout;
        }
        ((GradientDrawable) bg).setStroke(dip2px(1.0f), Color.parseColor(HwSplitBarConstants.RECT_FRAME_BORDER_COLOR));
        GradientDrawable gradientDrawable = (GradientDrawable) bg;
        if (HwActivityManager.IS_PHONE) {
            f = 0.0f;
        } else {
            f = this.mContext.getResources().getDimension(34472527);
        }
        gradientDrawable.setCornerRadius(f);
        rectFrameView.setVisibility(0);
        return rectFrameLayout;
    }

    private int dip2px(float dpValue) {
        return (int) ((dpValue * this.mContext.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private float getRegionAndScale(MotionEvent event) {
        int rightOffSet;
        float len = (float) (this.mIsLeftAndRightPos ? this.mDisplayWidth : this.mDisplayHeight);
        float columns = (float) (this.mIsLeftAndRightPos ? this.mWidthColumns : this.mHeightColumns);
        float curPos = this.mIsLeftAndRightPos ? event.getRawX() : event.getRawY();
        int offSet = this.mIsLeftAndRightPos ? this.primaryBounds.left : this.primaryBounds.top;
        int statusBarH = getNotchSizeOnRight();
        if (this.mIsLeftAndRightPos) {
            rightOffSet = getNavBarRight() + statusBarH;
        } else {
            rightOffSet = getNavBarBottomUpDown() + statusBarH;
        }
        float stopScale = (float) offSet;
        float threshold = curPos;
        float translateValue = 0.0f;
        if (curPos < (len / columns) + ((float) offSet)) {
            if (!this.mIsReadyToFull) {
                setThresholdTip(this.mLeftCover, this.mBlackCoverInExitSplit);
            }
            this.mIsReadyToFull = true;
            threshold = (len / columns) + ((float) offSet);
            if (curPos <= threshold) {
                translateValue = curPos - threshold;
            }
            if (this.mIsLeftAndRightPos) {
                this.mExitRegion = 1;
                this.mLeftImg.setTranslationX(translateValue / 2.0f);
            } else {
                this.mExitRegion = 3;
                this.mLeftImg.setTranslationY(translateValue / 2.0f);
            }
        } else if (curPos > (((columns - 1.0f) * len) / columns) - ((float) rightOffSet)) {
            if (!this.mIsReadyToFull) {
                setThresholdTip(this.mRightCover, this.mBlackCoverInExitSplit);
            }
            this.mIsReadyToFull = true;
            threshold = (((columns - 1.0f) * len) / columns) - ((float) rightOffSet);
            stopScale = len - ((float) rightOffSet);
            if (curPos >= threshold) {
                translateValue = curPos - threshold;
            }
            if (this.mIsLeftAndRightPos) {
                this.mExitRegion = 2;
                this.mRightImg.setTranslationX(translateValue / 2.0f);
            } else {
                this.mExitRegion = 4;
                this.mRightImg.setTranslationY(translateValue / 2.0f);
            }
        } else {
            this.mIsReadyToFull = false;
        }
        return 1.0f - (((threshold - curPos) * 0.05f) / (threshold - stopScale));
    }

    private void setThresholdTip(RelativeLayout layout, View view) {
        if (view.getParent() != null || layout == null) {
            Slog.i(TAG, "black view has parent already");
            return;
        }
        layout.addView(view);
        if (this.mVibrator != null) {
            this.mVibrator.vibrate(VibrationEffect.createOneShot(100, -1));
        }
    }

    private void setScale(float scale, int zoneRegion) {
        if (zoneRegion == 1 || zoneRegion == 3) {
            this.mLeftCover.setScaleX(scale);
            this.mLeftCover.setScaleY(scale);
        } else if (zoneRegion == 2 || zoneRegion == 4) {
            this.mRightCover.setScaleX(scale);
            this.mRightCover.setScaleY(scale);
        }
    }

    private void dragToFullScreen(MotionEvent event) {
        float scale = getRegionAndScale(event);
        if (scale == 1.0f) {
            if (!this.mIsReadyToFull && this.mIsDownAnimate) {
                this.mIsDownAnimate = false;
                int i = this.mExitRegion;
                if (i == 1 || i == 3) {
                    this.mLeftCover.removeView(this.mBlackCoverInExitSplit);
                } else if (i == 2 || i == 4) {
                    this.mRightCover.removeView(this.mBlackCoverInExitSplit);
                } else {
                    return;
                }
                setScale(1.0f, this.mExitRegion);
                this.mExitRegion = 0;
            } else {
                return;
            }
        }
        if (this.mIsReadyToFull) {
            setScale(scale, this.mExitRegion);
            this.mIsDownAnimate = true;
        }
    }

    private void setDateBundleInActionDown() {
        this.mAnimBundle.putInt(HwSplitBarConstants.DISPLAY_WIDTH, this.mDisplayWidth);
        this.mAnimBundle.putInt(HwSplitBarConstants.DISPLAY_HEIGHT, this.mDisplayHeight);
        this.mAnimBundle.putInt(HwMultiWindowManager.WIDTH_COLUMNS, this.mWidthColumns);
        this.mAnimBundle.putInt(HwMultiWindowManager.HEIGHT_COLUMNS, this.mHeightColumns);
        this.mAnimBundle.putBoolean(HwSplitBarConstants.SPLIT_ORIENTATION, this.mIsLeftAndRightPos);
    }

    private void setDataBundleInActionUp(float curPosition, float endPosition) {
        this.mAnimBundle.putInt(HwSplitBarConstants.EXIT_REGION, this.mExitRegion);
        this.mAnimBundle.putFloat(HwSplitBarConstants.CURRENT_POSITION, curPosition);
        this.mAnimBundle.putFloat(HwSplitBarConstants.END_POSITION, endPosition);
    }

    /* access modifiers changed from: private */
    public void setSplitBarVisibleIfNeeded() {
        Slog.i(TAG, "setSplitBarVisibleIfNeeded");
        if (!this.mIsNeedSetSplitBarVisible || this.mCoverLp == null) {
            Slog.i(TAG, "setSplitBarVisibleIfNeeded  return");
        } else if (this.mDragView.getVisibility() != 0) {
            this.mDragView.setVisibility(0);
            this.mIsNeedSetSplitBarVisible = false;
        }
    }

    public void updateSplitBarPosForIm(int position) {
        if (this.mIsSplitBarVisibleNow) {
            this.mHandler.removeMessages(4);
            Message msg = this.mHandler.obtainMessage(5);
            msg.arg1 = position;
            this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void updateSplitBarPos(int position) {
        if (this.mCoverLp != null && this.mDragView != null && this.mDragRegion != null && this.mCoverLayout != null && this.mIsSplitBarVisibleNow) {
            int visible = position > 0 ? 0 : 4;
            this.mDragRegion.setVisibility(visible);
            this.mDragView.setVisibility(visible);
            if (position < 0) {
                this.mCoverLp.y = INVALID_POSITION;
                updateCoverLayout();
            } else if (this.mCoverLp.y < 0) {
                setPosition();
                updateCoverLayout();
            }
        }
    }

    private void updateCoverLayout() {
        FrameLayout frameLayout;
        WindowManager.LayoutParams layoutParams = this.mCoverLp;
        if (layoutParams != null && (frameLayout = this.mCoverLayout) != null) {
            try {
                this.mWm.updateViewLayout(frameLayout, layoutParams);
            } catch (WindowManager.InvalidDisplayException e) {
                Slog.e(TAG, "update view layout error.");
            }
        }
    }
}
