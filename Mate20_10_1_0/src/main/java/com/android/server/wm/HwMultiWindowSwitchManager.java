package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.HwFoldScreenState;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.provider.Settings;
import android.util.Flog;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.DragEvent;
import android.view.IWindow;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.LayoutInflater;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.magicwin.HwMagicWindowService;
import com.android.server.multiwin.HwMultiWinConstants;
import com.android.server.multiwin.HwMultiWinUtils;
import com.android.server.multiwin.animation.HwMultiWinDragAnimationAdapter;
import com.android.server.multiwin.animation.HwMultiWinSplitBarController;
import com.android.server.multiwin.animation.interpolator.SharpCurveInterpolator;
import com.android.server.multiwin.listener.BlurListener;
import com.android.server.multiwin.listener.DragAnimationListener;
import com.android.server.multiwin.listener.HwMultiWinHotAreaConfigListener;
import com.android.server.multiwin.view.HwMultiWinClipImageView;
import com.android.server.multiwin.view.HwMultiWinHotAreaView;
import com.android.server.multiwin.view.HwMultiWinNotchPendingDropView;
import com.android.server.multiwin.view.HwMultiWinPushAcceptView;
import com.android.server.multiwin.view.HwMultiWinSwapAcceptView;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsALModeID;

public class HwMultiWindowSwitchManager implements DragAnimationListener, BlurListener, HwMultiWinHotAreaConfigListener {
    private static final String ALPHA_PROPERTY_NAME = "alpha";
    private static final String APP_LOCK_PACKAGE = "com.huawei.systemmanager";
    private static final int DIVIDE_BY_SIX = 6;
    private static final int DIVIDE_BY_THREE = 3;
    private static final int DIVIDE_BY_TWO = 2;
    private static final float FREEFORM_SWAP_REGION_HEIGHT_WEIGHT = 4.0f;
    private static final float FREEFORM_SWAP_REGION_WIDTH_WEIGHT = 1.0f;
    private static final float FULL_PUSH_FREE_FORM_REGION_WEIGHT = 3.0f;
    private static final float FULL_PUSH_LEFT_REGION_WEIGHT = 1.0f;
    private static final float FULL_PUSH_RIGHT_REGION_WEIGHT = 1.0f;
    private static final int ICONS_LIST_CAPACITY = 2;
    private static final float ROUNDING_PARAMETER = 0.5f;
    private static final long SCREEN_SHOT_COVER_REMOVE_ANIM_DELAY = 50;
    private static final long SCREEN_SHOT_COVER_REMOVE_ANIM_DURATION = 100;
    private static final int SNAPSHOT_LIST_CAPACITY = 2;
    private static final int SPLIT_RATIO_TWO = 2;
    private static final float SPLIT_SWAP_CENTER_REGION_WIDTH_WEIGHT = 1.0f;
    private static final float SPLIT_SWAP_REGION_HEIGHT_WEIGHT = 1.0f;
    private static final int STATUS_DRAGGING = 2;
    private static final int STATUS_DRAG_ENDED = 4;
    private static final int STATUS_NOT_TO_DRAG = 3;
    private static final String TAG = "HwMultiWindowSwitchMngr";
    private static volatile HwMultiWindowSwitchManager mInstance = null;
    private final Set<String> blackListPkgs = new HashSet(Arrays.asList("com.huawei.systemmanager", "com.huawei.camera"));
    private IBinder mActivityToken;
    private HwMultiWinClipImageView mAppFullView;
    private ActivityTaskManagerService mAtms;
    private HwMultiWinClipImageView mBlurAppFullView;
    private Rect mDisplayBound = new Rect();
    private HwMultiWinDragAnimationAdapter mDragAnimationAdapter;
    private LinearLayout mDropView;
    private Rect mFreeFormDropBound;
    private boolean mHasDragEnded = false;
    private boolean mHasDragStarted = false;
    private ViewGroup mHotArea;
    private HwActivityTaskManagerServiceEx mHwAtmsEx;
    private SurfaceControl mInputSurface;
    private HwMultiWindowInputInterceptor mInterceptor;
    private boolean mIsDragBarReset = true;
    private boolean mIsDropFailedCleanUp = false;
    private boolean mIsDropHandled = false;
    private boolean mIsInSplitScreenMode;
    private boolean mIsLeftRightSplit = false;
    private boolean mIsNotchStatusChanged = false;
    /* access modifiers changed from: private */
    public boolean mIsProcessingDrag = false;
    private boolean mIsScreenSwitchHandled = false;
    private int mLastDropSplitMode;
    private HwMultiWinHotAreaView mLeftSplitScreenRegion;
    private Rect mNavBarBound;
    private int mNavBarPos = -1;
    private Rect mNotchBound = new Rect();
    private HwMultiWinNotchPendingDropView mNotchHotArea;
    private int mNotchPos = -1;
    private int mNotchStatus = 0;
    private HwMultiWinHotAreaView mRightSplitScreenRegion;
    /* access modifiers changed from: private */
    public RelativeLayout mRootView;
    /* access modifiers changed from: private */
    public ImageView mScreenShotCover;
    private ObjectAnimator mScreenShotCoverRemoveAnimator;
    private float mSnapShotScaleFactor = 1.0f;
    private RelativeLayout mSplitBarContainer;
    private HwMultiWinSplitBarController mSplitBarController;
    private int mSplitRatio = 0;
    private Context mUiContext;
    private WindowManager mWindowManager;
    private int mWindowingMode = 0;
    /* access modifiers changed from: private */
    public WindowManagerService mWms;

    private HwMultiWindowSwitchManager(HwActivityTaskManagerServiceEx hwAtmsEx) {
        this.mHwAtmsEx = hwAtmsEx;
        this.mAtms = hwAtmsEx.mIAtmsInner.getATMS();
        this.mUiContext = hwAtmsEx.mIAtmsInner.getUiContext();
        this.mWms = this.mAtms.mWindowManager;
    }

    public static HwMultiWindowSwitchManager getInstance(HwActivityTaskManagerServiceEx hwAtmsEx) {
        if (mInstance == null) {
            synchronized (HwMultiWindowSwitchManager.class) {
                if (mInstance == null) {
                    mInstance = new HwMultiWindowSwitchManager(hwAtmsEx);
                }
            }
        }
        return mInstance;
    }

    public void onCaptionDropAnimationDone(IBinder activityToken) {
        try {
            if (this.mIsScreenSwitchHandled) {
                Slog.d(TAG, "mIsCaptionDropAnimationDone is true, do not drop activity");
                this.mIsScreenSwitchHandled = true;
                synchronized (this.mAtms.getGlobalLock()) {
                    Slog.d(TAG, "onCaptionDropAnimationDone: ensureActivitiesVisible");
                    this.mAtms.mRootActivityContainer.ensureActivitiesVisible((ActivityRecord) null, 0, false, true);
                    this.mAtms.mRootActivityContainer.resumeFocusedStacksTopActivities();
                }
                return;
            }
            Slog.i(TAG, "onCaptionDropAnimationDone " + activityToken + ", mLastDropTarget = " + this.mLastDropSplitMode + ", isFreeFormDragged = " + WindowConfiguration.isHwFreeFormWindowingMode(this.mWindowingMode));
            this.mActivityToken = activityToken;
            if (this.mLastDropSplitMode == 1 || this.mLastDropSplitMode == 3) {
                dropToSplitScreen(100);
            } else if (this.mLastDropSplitMode == 2 || this.mLastDropSplitMode == 4) {
                dropToSplitScreen(101);
            } else if (this.mLastDropSplitMode == 5) {
                dropToFreeForm();
            } else {
                handleRemoveHotArea();
            }
            this.mIsScreenSwitchHandled = true;
            synchronized (this.mAtms.getGlobalLock()) {
                Slog.d(TAG, "onCaptionDropAnimationDone: ensureActivitiesVisible");
                this.mAtms.mRootActivityContainer.ensureActivitiesVisible((ActivityRecord) null, 0, false, true);
                this.mAtms.mRootActivityContainer.resumeFocusedStacksTopActivities();
            }
        } catch (Throwable th) {
            this.mIsScreenSwitchHandled = true;
            synchronized (this.mAtms.getGlobalLock()) {
                Slog.d(TAG, "onCaptionDropAnimationDone: ensureActivitiesVisible");
                this.mAtms.mRootActivityContainer.ensureActivitiesVisible((ActivityRecord) null, 0, false, true);
                this.mAtms.mRootActivityContainer.resumeFocusedStacksTopActivities();
                throw th;
            }
        }
    }

    private WindowManager.LayoutParams createBasicLayoutParams(int subtractY) {
        Point displaySize = HwMultiWinUtils.getDisplaySize();
        int displayHeight = -1;
        int displayWidth = displaySize != null ? displaySize.x : -1;
        if (displaySize != null) {
            displayHeight = displaySize.y - subtractY;
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(displayWidth, displayHeight, 0, subtractY, HwArbitrationDEFS.MSG_HISTREAM_TRIGGER_MPPLINK_INTERNAL, HighBitsALModeID.MODE_SRE_DISABLE, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.gravity = 8388659;
        layoutParams.layoutInDisplayCutoutMode = 1;
        return layoutParams;
    }

    private WindowManager.LayoutParams initHotArea() {
        if (this.mRootView != null) {
            Slog.d(TAG, "mRootView not null");
            handleRemoveHotArea();
        }
        removeScreenShotCover();
        this.mRootView = (RelativeLayout) LayoutInflater.from(this.mUiContext).inflate(34013350, (ViewGroup) null);
        RelativeLayout relativeLayout = this.mRootView;
        if (relativeLayout == null) {
            Slog.e(TAG, "initHotArea failed, cause mRootView is null!");
            return null;
        }
        relativeLayout.setLayoutDirection(0);
        this.mDropView = (LinearLayout) this.mRootView.findViewById(34603261);
        LinearLayout linearLayout = this.mDropView;
        if (linearLayout == null) {
            Slog.e(TAG, "initHotArea failed, cause mDropView is null!");
            return null;
        }
        linearLayout.setLayoutDirection(0);
        this.mHotArea = (ViewGroup) this.mDropView.findViewById(34603262);
        ViewGroup viewGroup = this.mHotArea;
        if (viewGroup == null) {
            Slog.e(TAG, "initHotArea failed, cause mHotArea is null!");
            return null;
        }
        viewGroup.setLayoutDirection(0);
        WindowManager.LayoutParams layoutParams = createBasicLayoutParams(0);
        layoutParams.setTitle("MultiWindowHotArea");
        LinearLayout splitLayout = (LinearLayout) this.mHotArea.findViewById(34603281);
        if (splitLayout == null) {
            Slog.e(TAG, "initHotArea failed, cause splitLayout is null!");
            return null;
        }
        splitLayout.setLayoutDirection(0);
        if (this.mNotchPos != -1 && !this.mNotchBound.isEmpty()) {
            addNotchImageView();
        }
        if (this.mIsLeftRightSplit) {
            splitLayout.setOrientation(0);
        } else {
            splitLayout.setOrientation(1);
        }
        if (this.mWindowingMode == 102) {
            HwMultiWinHotAreaView freeformScreenRegion = (HwMultiWinHotAreaView) this.mHotArea.findViewById(34603256);
            if (freeformScreenRegion == null) {
                Slog.e(TAG, "initHotArea failed, cause freeformScreenRegion is null!");
                return null;
            }
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) freeformScreenRegion.getLayoutParams();
            if (this.mIsLeftRightSplit) {
                params.width = this.mDisplayBound.right / 3;
                params.height = -1;
            } else {
                params.width = -1;
                params.height = this.mDisplayBound.bottom / 3;
            }
            freeformScreenRegion.setLayoutParams(params);
        }
        this.mIsDropHandled = false;
        return layoutParams;
    }

    private void configNotchHotArea(HwMultiWinHotAreaView primaryExecutorView, HwMultiWinHotAreaView secondaryExecutorView) {
        HwMultiWinNotchPendingDropView hwMultiWinNotchPendingDropView = this.mNotchHotArea;
        if (hwMultiWinNotchPendingDropView != null) {
            hwMultiWinNotchPendingDropView.setDragAnimationListener(this);
            int i = this.mNotchPos;
            if (i == 0) {
                this.mNotchHotArea.setSplitMode(3);
                this.mNotchHotArea.setExecutorView(primaryExecutorView);
            } else if (i == 1) {
                this.mNotchHotArea.setSplitMode(1);
                this.mNotchHotArea.setExecutorView(primaryExecutorView);
            } else if (i == 2) {
                this.mNotchHotArea.setSplitMode(2);
                this.mNotchHotArea.setExecutorView(secondaryExecutorView);
            }
        }
    }

    private Bitmap getTargetScreenShotForNotchHotArea(List<Bitmap> screenShots, boolean isPushFullScreen) {
        if (isPushFullScreen) {
            return screenShots.get(0);
        }
        if (!isDragSplit()) {
            int i = this.mNotchPos;
            if (i == 0 || i == 1) {
                return screenShots.get(0);
            }
            if (i != 2 || screenShots.size() <= 1) {
                return null;
            }
            return screenShots.get(1);
        } else if (isDragPrimary()) {
            return screenShots.get(0);
        } else {
            if (screenShots.size() > 1) {
                return screenShots.get(1);
            }
            return null;
        }
    }

    private int adjustNotchPendingTargetSizeIfNeeded(int targetSize) {
        Rect rect;
        if (this.mNavBarPos == -1 || (rect = this.mNavBarBound) == null || rect.isEmpty()) {
            return targetSize;
        }
        if (isNotchPosTop() && this.mNavBarPos == 4 && isDragSecondary()) {
            return targetSize - this.mNavBarBound.height();
        }
        if (isNotchPosLeft() && this.mNavBarPos == 2 && isDragSecondary()) {
            return targetSize - this.mNavBarBound.width();
        }
        if (!isNotchPosRight() || this.mNavBarPos != 2) {
            return targetSize;
        }
        if (isDragPrimary()) {
            return (this.mNavBarBound.width() * 2) + targetSize;
        }
        if (isDragSecondary()) {
            return this.mNavBarBound.width() + targetSize;
        }
        return targetSize;
    }

    private void addNotchHotArea(List<Bitmap> screenShots) {
        if (this.mNotchPos != -1 && this.mNotchBound != null) {
            if (screenShots == null || screenShots.isEmpty()) {
                Slog.e(TAG, "addNotchHotArea failed, cause screenShots is null or empty");
                return;
            }
            boolean isPushFullScreen = screenShots.size() == 1;
            Bitmap targetScreenShot = getTargetScreenShotForNotchHotArea(screenShots, isPushFullScreen);
            if (targetScreenShot == null) {
                Slog.e(TAG, "addNotchHotArea failed, cause targetScreenShot is null!");
                return;
            }
            Point targetSize = getRealSnapShotSize(targetScreenShot);
            if (isPushFullScreen) {
                if (isNotchPosLeft() || isNotchPosRight()) {
                    targetSize.x /= 2;
                } else if (isNotchPosTop()) {
                    targetSize.y /= 2;
                } else {
                    return;
                }
            }
            int width = this.mNotchBound.width();
            int height = this.mNotchBound.height();
            Rect notchPendingDropBound = new Rect(this.mNotchBound);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, height);
            int i = this.mNotchPos;
            if (i == 0) {
                notchPendingDropBound.bottom = (this.mNotchBound.height() * 2) + adjustNotchPendingTargetSizeIfNeeded(targetSize.y);
                lp.addRule(10);
            } else if (i == 1) {
                notchPendingDropBound.right = (this.mNotchBound.width() * 2) + adjustNotchPendingTargetSizeIfNeeded(targetSize.x);
                lp.addRule(9);
            } else if (i == 2) {
                notchPendingDropBound.left = (notchPendingDropBound.right - (this.mNotchBound.width() * 2)) - adjustNotchPendingTargetSizeIfNeeded(targetSize.x);
                lp.addRule(11);
            } else {
                return;
            }
            this.mNotchHotArea = new HwMultiWinNotchPendingDropView(this.mUiContext, this.mNotchBound, notchPendingDropBound);
            this.mNotchHotArea.setLayoutParams(lp);
            this.mRootView.addView(this.mNotchHotArea);
            this.mNotchHotArea.bringToFront();
        }
    }

    private void addNotchImageView() {
        ImageView notchView = new ImageView(this.mUiContext);
        notchView.setLayoutParams(new LinearLayout.LayoutParams(this.mNotchBound.width(), this.mNotchBound.height()));
        if (Settings.Secure.getInt(this.mUiContext.getContentResolver(), "display_notch_status", 0) != 0) {
            if (this.mNotchPos == 0) {
                notchView.setImageBitmap(HwMultiWinUtils.getStatusBarScreenShot(this.mNotchBound.width(), this.mNotchBound.height()));
            }
        } else if (this.mNotchPos != 0 || !this.mIsInSplitScreenMode) {
            notchView.setBackgroundColor(-16777216);
        } else {
            notchView.setImageBitmap(HwMultiWinUtils.getStatusBarScreenShot(this.mNotchBound.width(), this.mNotchBound.height()));
        }
        ViewGroup.LayoutParams viewGroupLp = this.mHotArea.getLayoutParams();
        if (!(viewGroupLp instanceof LinearLayout.LayoutParams)) {
            Slog.w(TAG, "addNotchImageView cause viewGroupLp is not instance of LinearLayout.LayoutParams");
            return;
        }
        LinearLayout.LayoutParams hotAreaLp = (LinearLayout.LayoutParams) viewGroupLp;
        hotAreaLp.weight = 1.0f;
        if (isLandScape()) {
            this.mDropView.setOrientation(0);
            hotAreaLp.width = 0;
        } else {
            this.mDropView.setOrientation(1);
            hotAreaLp.height = 0;
        }
        if (isNotchPosRight()) {
            this.mDropView.addView(notchView);
        } else {
            this.mDropView.addView(notchView, 0);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00dd, code lost:
        if (r0 != false) goto L_0x00e4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00df, code lost:
        showNotSupportToast(r13.mUiContext, r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00e4, code lost:
        return r0;
     */
    public boolean isSupportedSplit(IBinder token) {
        int i;
        boolean isSupportedSplit = true;
        String topActivityName = "";
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(token);
            if (activityRecord == null) {
                Slog.e(TAG, "isSupportedSplit activityRecord is null, token:" + token);
                return false;
            }
            int windowingMode = activityRecord.getWindowingMode();
            if (windowingMode == 102) {
                i = 1035;
            } else {
                i = 1039;
            }
            reportStatisticInfo(i, false);
            if (WindowConfiguration.isHwSplitScreenWindowingMode(windowingMode)) {
                return true;
            }
            ActivityStack topStack = this.mHwAtmsEx.mHwMwm.getFilteredTopStack(activityRecord.getDisplay(), Arrays.asList(5, 2, 102));
            if (topStack == null) {
                return false;
            }
            if (topStack.inHwSplitScreenWindowingMode()) {
                return true;
            }
            if (topStack.getWindowingMode() != 1 && topStack.getWindowingMode() != 103) {
                return false;
            }
            ActivityRecord topRecord = topStack.getTopActivity();
            if (topRecord != null) {
                if (topRecord.appInfo != null) {
                    TaskRecord topTask = topStack.topTask();
                    if (topTask == null || !topTask.supportsHwSplitScreenWindowingMode() || topStack.getActivityType() != 1 || this.blackListPkgs.contains(topRecord.appInfo.packageName)) {
                        Slog.i(TAG, "topStack activity don't support split or in black list: " + topRecord.appInfo);
                        PackageManager pm = this.mUiContext.getPackageManager();
                        if (pm != null) {
                            topActivityName = topRecord.appInfo.loadLabel(pm).toString();
                        }
                        isSupportedSplit = false;
                    }
                }
            }
            Slog.w(TAG, "topStack activity record or appInfo is null: " + topStack);
            return false;
        }
    }

    private void showNotSupportToast(Context context, String activityName) {
        this.mAtms.mUiHandler.post(new Runnable(context, activityName) {
            /* class com.android.server.wm.$$Lambda$HwMultiWindowSwitchManager$jG0rfjYfPRgBRIqzV91ZHaeOq4w */
            private final /* synthetic */ Context f$0;
            private final /* synthetic */ String f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            public final void run() {
                HwMultiWindowSwitchManager.lambda$showNotSupportToast$0(this.f$0, this.f$1);
            }
        });
    }

    static /* synthetic */ void lambda$showNotSupportToast$0(Context context, String activityName) {
        Toast toast = Toast.makeText(context, context.getString(33685747, activityName), 0);
        toast.getWindowParams().privateFlags |= 16;
        toast.show();
    }

    /* access modifiers changed from: private */
    public boolean startDragAndDrop(View dragView, Bundle info) {
        boolean result = false;
        if (info == null) {
            Log.w(TAG, "startDragAndDrop failed! cause info is null!");
            return false;
        } else if (dragView == null) {
            Log.w(TAG, "startDragAndDrop failed! cause dragBar is null!");
            return false;
        } else if (!info.containsKey(HwMultiWinConstants.DRAG_SURFACE_SIZE_KEY)) {
            Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_SURFACE_SIZE_KEY!");
            return false;
        } else {
            Point dragSurfaceSize = (Point) info.getParcelable(HwMultiWinConstants.DRAG_SURFACE_SIZE_KEY);
            if (dragSurfaceSize == null) {
                Slog.w(TAG, "startDragAndDrop failed, cause dragSurfaceSize is null!");
                return false;
            }
            int clipWidth = HwMultiWinUtils.dip2px(this.mUiContext, 176.0f);
            int clipHeight = HwMultiWinUtils.dip2px(this.mUiContext, 312.0f);
            int clipWidth2 = clipWidth > dragSurfaceSize.x ? dragSurfaceSize.x : clipWidth;
            int clipHeight2 = clipHeight > dragSurfaceSize.y ? dragSurfaceSize.y : clipHeight;
            Drawable background = new ColorDrawable(this.mUiContext.getResources().getColor(33882864));
            if (!info.containsKey(HwMultiWinConstants.SCREEN_SHOT_KEY)) {
                Log.w(TAG, "startDragAndDrop failed! cause info has no SCREEN_SHOT_KEY!");
                return false;
            }
            Bitmap screenShot = (Bitmap) info.getParcelable(HwMultiWinConstants.SCREEN_SHOT_KEY);
            if (!info.containsKey(HwMultiWinConstants.INITIAL_CLIP_SIZE_KEY)) {
                Log.w(TAG, "startDragAndDrop failed! cause info has no INITIAL_CLIP_SIZE_KEY!");
                return false;
            }
            Point initialClipSize = (Point) info.getParcelable(HwMultiWinConstants.INITIAL_CLIP_SIZE_KEY);
            this.mDragAnimationAdapter = new HwMultiWinDragAnimationAdapter(dragView).setScreenShot(screenShot).setCaptionView(dragView).setDragBackground(background).setDraggingClipSize(clipWidth2, clipHeight2).setFloatingClipSize(initialClipSize.x, initialClipSize.y);
            if (!info.containsKey(HwMultiWinConstants.DRAG_BAR_DRAW_OFFSETS_KEY)) {
                Log.w(TAG, "startDragAndDrop failed! cause info has no DRAG_BAR_DRAW_OFFSETS_KEY!");
                return false;
            }
            Point dragBarDrawOffsets = (Point) info.getParcelable(HwMultiWinConstants.DRAG_BAR_DRAW_OFFSETS_KEY);
            if (!info.containsKey(HwMultiWinConstants.DRAG_BAR_BMP_KEY)) {
                Log.w(TAG, "startDragAndDrop failed! cause info has no DRAG_BAR_BMP_KEY!");
                return false;
            }
            this.mDragAnimationAdapter.setDragBarBmp((Bitmap) info.getParcelable(HwMultiWinConstants.DRAG_BAR_BMP_KEY), dragBarDrawOffsets);
            Drawable iconDrawable = getCurrentTokenIcon();
            if (iconDrawable != null) {
                this.mDragAnimationAdapter.setIconDrawable(iconDrawable);
                result = this.mDragAnimationAdapter.startDragAndDrop(info, isDragSplit(), this.mInterceptor.mServerChannel);
            }
            if (isDragFreeForm()) {
                this.mDragAnimationAdapter.startFreeFormDraggingAnimation();
            }
            return result;
        }
    }

    private boolean preStartDragAndDrop(Bundle info, List<Bitmap> screenShots) {
        Point initialClipSize;
        Point dragSurfaceSize;
        Rect freeFormRect = getFreeFormRect(this.mActivityToken);
        Bitmap dragSplitScreenShot = null;
        if (isDragFreeForm()) {
            dragSurfaceSize = new Point(freeFormRect.width(), freeFormRect.height());
            initialClipSize = dragSurfaceSize;
        } else if (isDragSplit()) {
            dragSplitScreenShot = getDragSplitScreenShot(screenShots);
            if (dragSplitScreenShot == null) {
                Slog.w(TAG, "addHotArea getCurrentTokenSnapShot is null, return!");
                return false;
            }
            dragSurfaceSize = getDragSplitSize(dragSplitScreenShot, freeFormRect);
            initialClipSize = getRealSnapShotSize(dragSplitScreenShot);
        } else {
            Slog.e(TAG, "addHotArea failed, cause drag window mode is unknown!");
            return false;
        }
        if (dragSurfaceSize == null || dragSurfaceSize.x <= 0 || dragSurfaceSize.y <= 0) {
            Slog.e(TAG, "preStartDragAndDrop failed, cause dragSurfaceSize is invalid!");
            return false;
        }
        info.putParcelable(HwMultiWinConstants.DRAG_SURFACE_SIZE_KEY, dragSurfaceSize);
        info.putParcelable(HwMultiWinConstants.INITIAL_CLIP_SIZE_KEY, initialClipSize);
        info.putParcelable(HwMultiWinConstants.SCREEN_SHOT_KEY, dragSplitScreenShot);
        return true;
    }

    private boolean getCurrentWindowingMode() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            boolean z = false;
            if (activityRecord == null) {
                Slog.e(TAG, "ActivityRecord is null for token " + this.mActivityToken);
                return false;
            }
            this.mWindowingMode = activityRecord.getWindowingMode();
            this.mDisplayBound = activityRecord.getDisplay().getWindowConfiguration().getBounds();
            Bundle bundle = HwMultiWindowManager.getSplitGearsByDisplay(activityRecord.getDisplay());
            if (bundle == null) {
                Slog.e(TAG, "bundle is null, cannot get isLeftRightSplit");
                return false;
            }
            if (bundle.getInt(HwMultiWindowManager.HW_SPLIT_SCREEN_PRIMARY_POSITION) == 1) {
                z = true;
            }
            this.mIsLeftRightSplit = z;
            return true;
        }
    }

    public void addHotArea(IBinder activityToken, final Bundle info) {
        Slog.d(TAG, "addHotArea token " + activityToken + ", info = " + info);
        if (this.mIsProcessingDrag) {
            Slog.w(TAG, "addHotArea failed, another addHostArea is doing");
            return;
        }
        this.mIsProcessingDrag = true;
        this.mActivityToken = activityToken;
        if (info == null) {
            Slog.w(TAG, "addHotArea failed, cause info is null");
            this.mIsProcessingDrag = false;
            return;
        }
        this.mIsDropFailedCleanUp = false;
        this.mHasDragStarted = false;
        this.mHasDragEnded = false;
        this.mIsScreenSwitchHandled = false;
        this.mIsNotchStatusChanged = false;
        boolean isInLazyMode = isInLazyMode();
        boolean isInSubFoldDisplayMode = isInSubFoldDisplayMode();
        if (isInLazyMode || isInSubFoldDisplayMode) {
            this.mIsProcessingDrag = false;
        } else if (!setUpInputSurface()) {
            this.mIsProcessingDrag = false;
            Slog.w(TAG, "addHotArea: set up input surface failed, not to drag!");
        } else if (!getCurrentWindowingMode()) {
            Slog.w(TAG, "addHotArea: get current windowing mode failed, not to drag!");
            cleanUpInputSurface();
            this.mIsProcessingDrag = false;
        } else {
            this.mNavBarBound = new Rect();
            this.mNavBarPos = getNavBarBoundOnScreen(this.mNavBarBound);
            this.mNotchBound.setEmpty();
            this.mNotchPos = getNotchBoundOnScreen(this.mNotchBound);
            saveNotchStatus();
            List<Bitmap> screenShots = getTopStackSnapShot();
            if (!preStartDragAndDrop(info, screenShots)) {
                Slog.w(TAG, "addHotArea: preStartDragAndDrop failed, just return!");
                cleanUpInputSurface();
                this.mIsProcessingDrag = false;
                return;
            }
            WindowManager.LayoutParams layoutParams = initHotArea();
            if (layoutParams == null) {
                cleanUpInputSurface();
                this.mIsProcessingDrag = false;
            } else if (screenShots.size() == 0) {
                Slog.w(TAG, "screenShots empty!");
                cleanUpInputSurface();
                this.mIsProcessingDrag = false;
            } else {
                configBaseRegions(screenShots);
                this.mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    /* class com.android.server.wm.HwMultiWindowSwitchManager.AnonymousClass1 */

                    public void onGlobalLayout() {
                        HwMultiWindowSwitchManager hwMultiWindowSwitchManager = HwMultiWindowSwitchManager.this;
                        if (!hwMultiWindowSwitchManager.startDragAndDrop(hwMultiWindowSwitchManager.mRootView, info)) {
                            Slog.w(HwMultiWindowSwitchManager.TAG, "startDragAndDrop failed");
                            boolean unused = HwMultiWindowSwitchManager.this.mIsProcessingDrag = false;
                        }
                        HwMultiWindowSwitchManager.this.cleanUpInputSurface();
                        HwMultiWindowSwitchManager.this.mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
                this.mRootView.setOnClickListener(new View.OnClickListener() {
                    /* class com.android.server.wm.$$Lambda$HwMultiWindowSwitchManager$Gwf5sTqiILELor1KTcXv1tu_E2k */

                    public final void onClick(View view) {
                        HwMultiWindowSwitchManager.this.lambda$addHotArea$1$HwMultiWindowSwitchManager(view);
                    }
                });
                configHotArea(this.mWindowingMode, this.mActivityToken, this.mLeftSplitScreenRegion, this.mRightSplitScreenRegion, screenShots);
                if (this.mWindowManager == null) {
                    this.mWindowManager = (WindowManager) this.mUiContext.getSystemService("window");
                }
                Slog.d(TAG, "before add hotarea");
                long token = Binder.clearCallingIdentity();
                try {
                    this.mWindowManager.addView(this.mRootView, layoutParams);
                } catch (RuntimeException e) {
                    Slog.e(TAG, "addHotArea: addView failed cause exception happened: " + e.toString());
                    cleanUpInputSurface();
                    this.mIsProcessingDrag = false;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
                Binder.restoreCallingIdentity(token);
                Slog.d(TAG, "after add hotarea");
            }
        }
    }

    public /* synthetic */ void lambda$addHotArea$1$HwMultiWindowSwitchManager(View v) {
        handleRemoveHotArea();
    }

    /* access modifiers changed from: private */
    public void cleanUpInputSurface() {
        if (this.mInputSurface != null) {
            Slog.d(TAG, "cleanUpInputSurface: mInputSurface");
            SurfaceControl.Transaction transaction = this.mWms.mTransactionFactory.make();
            transaction.remove(this.mInputSurface).apply();
            this.mInputSurface = null;
            transaction.close();
        }
        if (this.mInterceptor != null) {
            Slog.d(TAG, "cleanUpInputSurface: mInterceptor");
            this.mInterceptor.tearDown();
            this.mInterceptor = null;
        }
    }

    private boolean setUpInputSurface() {
        cleanUpInputSurface();
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            Slog.w(TAG, "setUpInputSurface: failed, cause display is null!");
            return false;
        }
        DisplayContent dc = this.mWms.mRoot.getDisplayContent(display.getDisplayId());
        if (dc == null) {
            Slog.w(TAG, "setUpInputSurface: failed, cause dc is null!");
            return false;
        }
        ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
        if (activityRecord == null || activityRecord.mAppWindowToken == null) {
            Slog.w(TAG, "setUpInputSurface: failed, cause activityRecord or appWindowToken is null!");
            return false;
        }
        WindowState currentFocus = activityRecord.mAppWindowToken.findMainWindow();
        if (currentFocus == null) {
            Slog.w(TAG, "setUpInputSurface: failed, cause currentFocus is null!");
            return false;
        }
        this.mInputSurface = this.mWms.makeSurfaceBuilder(dc.getSession()).setContainerLayer().setName("HwMultiWindowSwitchMngr Input Consumer").build();
        this.mInterceptor = new HwMultiWindowInputInterceptor(display);
        InputWindowHandle interceptorHandle = this.mInterceptor.mWindowHandle;
        SurfaceControl.Transaction transaction = this.mWms.mTransactionFactory.make();
        transaction.show(this.mInputSurface);
        transaction.setInputWindowInfo(this.mInputSurface, interceptorHandle);
        transaction.setLayer(this.mInputSurface, Integer.MAX_VALUE);
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        transaction.setWindowCrop(this.mInputSurface, new Rect(0, 0, displaySize.x, displaySize.y));
        transaction.transferTouchFocus(currentFocus.mInputChannel.getToken(), interceptorHandle.token);
        transaction.syncInputWindows();
        transaction.apply();
        transaction.close();
        Slog.d(TAG, "setUpInputSurface done");
        return true;
    }

    private boolean isDragFreeForm() {
        return WindowConfiguration.isHwFreeFormWindowingMode(this.mWindowingMode);
    }

    private boolean isDragSplit() {
        return isDragPrimary() || isDragSecondary();
    }

    private boolean isDragPrimary() {
        return WindowConfiguration.isHwSplitScreenPrimaryWindowingMode(this.mWindowingMode);
    }

    private boolean isDragSecondary() {
        return WindowConfiguration.isHwSplitScreenSecondaryWindowingMode(this.mWindowingMode);
    }

    private int getNotchBoundOnScreen(Rect outBound) {
        if (outBound == null) {
            Slog.w(TAG, "getNotchBoundOnScreen failed: outBound is null!");
            return -1;
        }
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.w(TAG, "getNotchBoundOnScreen failed: activityRecord is null!");
                return -1;
            }
            TaskRecord taskRecord = activityRecord.task;
            if (taskRecord == null) {
                Slog.w(TAG, "getNotchBoundOnScreen failed: taskRecord is null!");
                return -1;
            }
            ActivityStack activityStack = taskRecord.getStack();
            if (activityStack == null) {
                Slog.w(TAG, "getNotchBoundOnScreen failed: activityStack is null!");
                return -1;
            }
            HwMultiWindowManager manager = HwMultiWindowManager.getInstance(this.mAtms);
            ActivityDisplay activityDisplay = activityStack.getDisplay();
            if (activityDisplay == null) {
                Slog.w(TAG, "getNotchBoundOnScreen failed: activityDisplay is null!");
                return -1;
            }
            int notchPos = manager.getNotchBoundOnScreen(activityDisplay.mDisplayContent, outBound);
            Slog.d(TAG, "getNotchBoundOnScreen: notchBound = " + outBound + ", notchPos = " + notchPos);
            return notchPos;
        }
    }

    private int getNavBarBoundOnScreen(Rect outBound) {
        if (outBound == null) {
            Slog.w(TAG, "getNavBarBoundOnScreen failed: outBound is null!");
            return -1;
        }
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.w(TAG, "getNavBarBoundOnScreen failed: activityRecord is null!");
                return -1;
            }
            TaskRecord taskRecord = activityRecord.task;
            if (taskRecord == null) {
                Slog.w(TAG, "getNavBarBoundOnScreen failed: taskRecord is null!");
                return -1;
            }
            ActivityStack activityStack = taskRecord.getStack();
            if (activityStack == null) {
                Slog.w(TAG, "getNavBarBoundOnScreen failed: activityStack is null!");
                return -1;
            }
            HwMultiWindowManager manager = HwMultiWindowManager.getInstance(this.mAtms);
            ActivityDisplay activityDisplay = activityStack.getDisplay();
            if (activityDisplay == null) {
                Slog.w(TAG, "getNavBarBoundOnScreen failed: activityDisplay is null!");
                return -1;
            }
            int navPos = manager.getNavBarBoundOnScreen(activityDisplay.mDisplayContent, outBound);
            Slog.d(TAG, "getNavBarBoundOnScreen: navBarBound = " + outBound + ", navPos = " + navPos);
            return navPos;
        }
    }

    private boolean isInLazyMode() {
        WindowManagerService windowManagerService = this.mAtms.mWindowManager;
        if (windowManagerService == null) {
            Slog.w(TAG, "get is in lazy mode failed, cause windowManagerService is null!");
            return false;
        } else if (windowManagerService.getLazyMode() != 0) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInSubFoldDisplayMode() {
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal;
        if (HwFoldScreenState.isFoldScreenDevice() && (hwFoldScreenManagerInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class)) != null && hwFoldScreenManagerInternal.getDisplayMode() == 3) {
            return true;
        }
        return false;
    }

    private void configBaseRegions(List<Bitmap> screenShots) {
        this.mLeftSplitScreenRegion = (HwMultiWinHotAreaView) this.mHotArea.findViewById(34603260);
        if (this.mIsLeftRightSplit) {
            this.mLeftSplitScreenRegion.setSplitMode(1);
        } else {
            this.mLeftSplitScreenRegion.setSplitMode(3);
        }
        if (WindowConfiguration.isHwFreeFormWindowingMode(this.mWindowingMode)) {
            this.mLeftSplitScreenRegion.initSplitScaleAnimation();
        }
        if (!isDragPrimary()) {
            this.mLeftSplitScreenRegion.setScaleType(ImageView.ScaleType.FIT_XY);
            this.mLeftSplitScreenRegion.setImageBitmap(screenShots.get(0));
        }
        this.mLeftSplitScreenRegion.setDragAnimationListener(this);
        this.mRightSplitScreenRegion = (HwMultiWinHotAreaView) this.mHotArea.findViewById(34603273);
        if (this.mIsLeftRightSplit) {
            this.mRightSplitScreenRegion.setSplitMode(2);
        } else {
            this.mRightSplitScreenRegion.setSplitMode(4);
        }
        if (screenShots.size() > 1 && !isDragSecondary()) {
            this.mRightSplitScreenRegion.setImageBitmap(screenShots.get(1));
        }
        if (WindowConfiguration.isHwFreeFormWindowingMode(this.mWindowingMode)) {
            this.mRightSplitScreenRegion.initSplitScaleAnimation();
        }
        this.mRightSplitScreenRegion.setDragAnimationListener(this);
        HwMultiWinHotAreaView freeFormScreenRegion = (HwMultiWinHotAreaView) this.mHotArea.findViewById(34603256);
        adjustHotAreaBySplitRatio(this.mLeftSplitScreenRegion, this.mRightSplitScreenRegion, freeFormScreenRegion);
        setMarginForSplitBar(this.mLeftSplitScreenRegion, this.mRightSplitScreenRegion);
        addSplitBar();
        freeFormScreenRegion.setDragAnimationListener(this);
        freeFormScreenRegion.setSplitMode(5);
        freeFormScreenRegion.disableBorder();
    }

    private void setMarginForSplitBar(HwMultiWinHotAreaView leftSplitScreenRegion, HwMultiWinHotAreaView rightSplitScreenRegion) {
        ViewGroup.LayoutParams lp = leftSplitScreenRegion.getLayoutParams();
        if (!(lp instanceof LinearLayout.LayoutParams)) {
            Slog.w(TAG, "setMarginForSplitBar failed, cause lp is not LinearLayout.LayoutParams.");
            return;
        }
        LinearLayout.LayoutParams leftLp = (LinearLayout.LayoutParams) lp;
        ViewGroup.LayoutParams rp = rightSplitScreenRegion.getLayoutParams();
        if (!(rp instanceof LinearLayout.LayoutParams)) {
            Slog.w(TAG, "setMarginForSplitBar failed, cause rp is not LinearLayout.LayoutParams.");
            return;
        }
        LinearLayout.LayoutParams rightLp = (LinearLayout.LayoutParams) rp;
        if (isLandScape()) {
            leftLp.rightMargin = this.mUiContext.getResources().getDimensionPixelSize(34472540);
            rightLp.leftMargin = this.mUiContext.getResources().getDimensionPixelSize(34472540);
            return;
        }
        leftLp.bottomMargin = this.mUiContext.getResources().getDimensionPixelSize(34472540);
        rightLp.topMargin = this.mUiContext.getResources().getDimensionPixelSize(34472540);
    }

    private float getSplitFractionBySplitRatio() {
        int i = this.mSplitRatio;
        if (i == 0) {
            return 0.5f;
        }
        if (i == 1) {
            return 0.33333334f;
        }
        if (i == 2) {
            return 0.6666667f;
        }
        return 0.0f;
    }

    private int getSplitBarSwapMarginAdjustmentWithNavBar() {
        if (this.mNavBarPos == 4 && !isLandScape()) {
            return this.mNavBarBound.height();
        }
        if (this.mNavBarPos == 2 && isLandScape()) {
            return this.mNavBarBound.width();
        }
        if (this.mNavBarPos != 1 || !isLandScape()) {
            return 0;
        }
        return -this.mNavBarBound.width();
    }

    private Point getRealSnapShotSize(Bitmap dragSplitScreenShot) {
        Point point = new Point();
        float width = (float) dragSplitScreenShot.getWidth();
        float f = this.mSnapShotScaleFactor;
        if (f <= 0.0f) {
            f = 1.0f;
        }
        int width2 = (int) (width / f);
        float height = (float) dragSplitScreenShot.getHeight();
        float f2 = this.mSnapShotScaleFactor;
        if (f2 <= 0.0f) {
            f2 = 1.0f;
        }
        point.x = width2;
        point.y = (int) (height / f2);
        return point;
    }

    private Point getDragSplitSize(Bitmap dragSplitScreenShot, Rect freeFormRect) {
        Point realSnapShotSize = getRealSnapShotSize(dragSplitScreenShot);
        int width = realSnapShotSize.x;
        int height = realSnapShotSize.y;
        Point screenShotSize = new Point(width, height);
        int dragLength = isLandScape() ? width : height;
        int minLength = isLandScape() ? freeFormRect.width() : freeFormRect.height();
        if (dragLength < minLength) {
            if (isLandScape()) {
                screenShotSize.x = minLength;
            } else {
                screenShotSize.y = minLength;
            }
        }
        Slog.d(TAG, "getDragSplitSize dragSplitSize = " + screenShotSize + ", displaySize = ,screenShotSize = " + screenShotSize + ", isLandScape = " + isLandScape() + ", otherLength = " + minLength + ", dragLength = " + dragLength);
        return screenShotSize;
    }

    private boolean isNotchPosLeft() {
        return this.mNotchPos == 1;
    }

    private boolean isNotchPosRight() {
        return this.mNotchPos == 2;
    }

    private boolean isNotchPosTop() {
        return this.mNotchPos == 0;
    }

    private void addSplitBar() {
        int swapMargin;
        int margin;
        float marginFraction = getSplitFractionBySplitRatio();
        int splitBarContainerWidth = this.mUiContext.getResources().getDimensionPixelSize(34472536);
        int containerHeight = -1;
        int containerWidth = isLandScape() ? splitBarContainerWidth : -1;
        if (!isLandScape()) {
            containerHeight = splitBarContainerWidth;
        }
        int swapMarginAdjustmentWithNavBar = getSplitBarSwapMarginAdjustmentWithNavBar();
        RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(containerWidth, containerHeight);
        int hotAreaWidth = this.mDisplayBound.width();
        int hotAreaHeight = this.mDisplayBound.height();
        if (isLandScape()) {
            if (isNotchPosLeft() || isNotchPosRight()) {
                hotAreaWidth -= this.mNotchBound.width();
            }
            margin = ((int) (((float) hotAreaWidth) * marginFraction)) - (splitBarContainerWidth / 2);
            swapMargin = (((int) (((float) hotAreaWidth) * (1.0f - marginFraction))) - swapMarginAdjustmentWithNavBar) - (splitBarContainerWidth / 2);
            containerParams.leftMargin = margin;
        } else {
            if (isNotchPosTop()) {
                hotAreaHeight -= this.mNotchBound.height();
            }
            margin = ((int) (((float) hotAreaHeight) * marginFraction)) - (splitBarContainerWidth / 2);
            swapMargin = (((int) (((float) hotAreaHeight) * (1.0f - marginFraction))) - swapMarginAdjustmentWithNavBar) - (splitBarContainerWidth / 2);
            containerParams.topMargin = margin;
        }
        this.mSplitBarContainer = new RelativeLayout(this.mUiContext);
        this.mSplitBarContainer.setLayoutParams(containerParams);
        this.mSplitBarContainer.setBackgroundColor(-16777216);
        ImageView splitBar = new ImageView(this.mUiContext);
        splitBar.setImageDrawable(this.mUiContext.getDrawable(33751907));
        int splitBarWidth = this.mUiContext.getResources().getDimensionPixelSize(34472540);
        int splitBarLength = this.mUiContext.getResources().getDimensionPixelSize(34472539);
        RelativeLayout.LayoutParams splitBarParams = new RelativeLayout.LayoutParams(isLandScape() ? splitBarWidth : splitBarLength, isLandScape() ? splitBarLength : splitBarWidth);
        splitBarParams.addRule(13);
        splitBar.setLayoutParams(splitBarParams);
        this.mSplitBarContainer.addView(splitBar);
        this.mHotArea.addView(this.mSplitBarContainer);
        this.mSplitBarController = new HwMultiWinSplitBarController(this.mSplitBarContainer, isLandScape());
        this.mSplitBarController.setMargins(margin, swapMargin);
        ((LinearLayout) this.mHotArea.findViewById(34603281)).bringToFront();
        ((HwMultiWinHotAreaView) this.mHotArea.findViewById(34603256)).bringToFront();
    }

    private int getSplitRatio() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.e(TAG, "activityRecord is null for token " + this.mActivityToken);
                return 0;
            }
            ActivityStack topStack = this.mHwAtmsEx.mHwMwm.getFilteredTopStack(activityRecord.getDisplay(), Arrays.asList(5, 2, 102));
            if (topStack != null) {
                if (topStack.inHwSplitScreenWindowingMode()) {
                    return this.mHwAtmsEx.mHwMwm.getHwSplitScreenRatio(topStack);
                }
            }
            Slog.e(TAG, "Top stack is null or not split screen " + topStack);
            return 0;
        }
    }

    private void adjustHotAreaBySplitRatio(HwMultiWinHotAreaView leftSplitScreenRegion, HwMultiWinHotAreaView rightSplitScreenRegion, HwMultiWinHotAreaView freeformRegion) {
        this.mSplitRatio = getSplitRatio();
        if (this.mSplitRatio != 0) {
            LinearLayout.LayoutParams leftParams = (LinearLayout.LayoutParams) leftSplitScreenRegion.getLayoutParams();
            LinearLayout.LayoutParams rightParams = (LinearLayout.LayoutParams) rightSplitScreenRegion.getLayoutParams();
            if (this.mSplitRatio == 2) {
                rightParams.weight = 2.0f;
            } else {
                leftParams.weight = 2.0f;
            }
            rightSplitScreenRegion.setLayoutParams(rightParams);
            leftSplitScreenRegion.setLayoutParams(leftParams);
            if (this.mWindowingMode == 102) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) freeformRegion.getLayoutParams();
                if (this.mIsLeftRightSplit) {
                    if (this.mSplitRatio == 1) {
                        params.addRule(9, -1);
                        params.leftMargin = this.mDisplayBound.right / 6;
                    } else {
                        params.addRule(11, -1);
                        params.rightMargin = this.mDisplayBound.right / 6;
                    }
                } else if (this.mSplitRatio == 1) {
                    params.addRule(10, -1);
                    params.topMargin = this.mDisplayBound.bottom / 6;
                } else {
                    params.addRule(12, -1);
                    params.bottomMargin = this.mDisplayBound.bottom / 6;
                }
                freeformRegion.setLayoutParams(params);
            }
        }
    }

    private void configHotArea(int mode, IBinder activityToken, HwMultiWinHotAreaView leftSplitScreenRegion, HwMultiWinHotAreaView rightSplitScreenRegion, List<Bitmap> screenShots) {
        int i;
        int i2;
        int i3 = 2;
        if (WindowConfiguration.isHwFreeFormWindowingMode(mode)) {
            int screenShotsNum = screenShots.size();
            if (screenShotsNum == 1) {
                addNotchHotArea(screenShots);
                configHotAreaForPushFull(leftSplitScreenRegion, rightSplitScreenRegion);
            } else if (screenShotsNum == 2) {
                leftSplitScreenRegion.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
                leftSplitScreenRegion.setIsLandScape(isLandScape());
                rightSplitScreenRegion.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
                rightSplitScreenRegion.setIsLandScape(isLandScape());
                leftSplitScreenRegion.asHotArea(true);
                rightSplitScreenRegion.asHotArea(true);
                addNotchHotArea(screenShots);
                configNotchHotArea(leftSplitScreenRegion, rightSplitScreenRegion);
            }
            Slog.d(TAG, activityToken + " is in free form window mode.");
        } else if (WindowConfiguration.isHwSplitScreenPrimaryWindowingMode(mode)) {
            addNotchHotArea(screenShots);
            if (isLandScape()) {
                i2 = 2;
            } else {
                i2 = 4;
            }
            rightSplitScreenRegion.setDragSplitMode(i2);
            if (!isLandScape()) {
                i3 = 4;
            }
            rightSplitScreenRegion.setInitialDragSplitMode(i3);
            List<Drawable> icons = getTopAppIcons();
            if (icons != null && icons.size() > 1) {
                rightSplitScreenRegion.setIcon(icons.get(1));
            }
            configHotAreaForSwapSplit(leftSplitScreenRegion, rightSplitScreenRegion);
            Slog.d(TAG, activityToken + " is in primary window mode.");
        } else if (WindowConfiguration.isHwSplitScreenSecondaryWindowingMode(mode)) {
            addNotchHotArea(screenShots);
            int i4 = 3;
            if (isLandScape()) {
                i = 1;
            } else {
                i = 3;
            }
            leftSplitScreenRegion.setDragSplitMode(i);
            if (isLandScape()) {
                i4 = 1;
            }
            leftSplitScreenRegion.setInitialDragSplitMode(i4);
            List<Drawable> icons2 = getTopAppIcons();
            if (icons2 != null && icons2.size() > 0) {
                leftSplitScreenRegion.setIcon(icons2.get(0));
            }
            configHotAreaForSwapSplit(rightSplitScreenRegion, leftSplitScreenRegion);
            Slog.d(TAG, activityToken + " is in secondary window mode.");
        } else {
            Slog.d(TAG, activityToken + " is in undefined window mode.");
        }
    }

    private void configHotAreaForPushFull(HwMultiWinHotAreaView leftSplitScreenRegion, HwMultiWinHotAreaView rightSplitRegion) {
        this.mSplitBarController.hideSplitBar();
        int height = -1;
        int width = isLandScape() ? 0 : -1;
        if (!isLandScape()) {
            height = 0;
        }
        leftSplitScreenRegion.setLayoutParams(new LinearLayout.LayoutParams(width, height, 1.0f));
        rightSplitRegion.setLayoutParams(new LinearLayout.LayoutParams(width, height, 0.0f));
        this.mAppFullView = leftSplitScreenRegion;
        this.mAppFullView.setIsLandScape(isLandScape());
        LinearLayout blurImgContainer = createBlurImgContainer();
        HwMultiWinClipImageView blurImageView = createBlurImageView();
        this.mBlurAppFullView = blurImageView;
        this.mBlurAppFullView.setIsLandScape(isLandScape());
        List<Drawable> icons = getTopAppIcons();
        if (icons != null && icons.size() > 0) {
            this.mBlurAppFullView.setIcon(getTopAppIcons().get(0));
        }
        this.mBlurAppFullView.setAppFullView(this.mAppFullView);
        HwMultiWinUtils.blurForScreenShot(leftSplitScreenRegion, blurImageView, this, ImageView.ScaleType.FIT_XY);
        LinearLayout pushFullHotArea = createPushFullHotArea(blurImageView);
        blurImgContainer.addView(blurImageView);
        this.mHotArea.addView(blurImgContainer);
        this.mHotArea.addView(pushFullHotArea);
        ((ViewGroup) leftSplitScreenRegion.getParent()).bringToFront();
        pushFullHotArea.bringToFront();
    }

    private LinearLayout createPushFullHotArea(HwMultiWinClipImageView pushTarget) {
        int i;
        LinearLayout pushFullHotArea = new LinearLayout(this.mUiContext);
        pushFullHotArea.setLayoutDirection(0);
        int i2 = 1;
        pushFullHotArea.setOrientation(!isLandScape());
        pushFullHotArea.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        HwMultiWinPushAcceptView leftOrTopPushAcceptView = new HwMultiWinPushAcceptView(this.mUiContext);
        leftOrTopPushAcceptView.setLayoutParams(new LinearLayout.LayoutParams(isLandScape() ? 0 : -1, isLandScape() ? -1 : 0, 1.0f));
        if (!isLandScape()) {
            i2 = 3;
        }
        leftOrTopPushAcceptView.setSplitMode(i2);
        leftOrTopPushAcceptView.setPushTarget(pushTarget);
        leftOrTopPushAcceptView.setIsLandScape(isLandScape());
        leftOrTopPushAcceptView.setDragAnimationListener(this);
        leftOrTopPushAcceptView.setSplitBarController(this.mSplitBarController);
        leftOrTopPushAcceptView.setOriginalNotchStatus(this.mNotchStatus);
        leftOrTopPushAcceptView.setNotchPos(this.mNotchPos);
        HwMultiWinPushAcceptView rightOrBottomPushAcceptView = new HwMultiWinPushAcceptView(this.mUiContext);
        rightOrBottomPushAcceptView.setLayoutParams(new LinearLayout.LayoutParams(isLandScape() ? 0 : -1, isLandScape() ? -1 : 0, 1.0f));
        if (isLandScape()) {
            i = 2;
        } else {
            i = 4;
        }
        rightOrBottomPushAcceptView.setSplitMode(i);
        rightOrBottomPushAcceptView.setPushTarget(pushTarget);
        rightOrBottomPushAcceptView.setIsLandScape(isLandScape());
        rightOrBottomPushAcceptView.setDragAnimationListener(this);
        rightOrBottomPushAcceptView.setSplitBarController(this.mSplitBarController);
        rightOrBottomPushAcceptView.setOriginalNotchStatus(this.mNotchStatus);
        rightOrBottomPushAcceptView.setNotchPos(this.mNotchPos);
        HwMultiWinPushAcceptView freeFormAcceptView = new HwMultiWinPushAcceptView(this.mUiContext);
        freeFormAcceptView.setLayoutParams(new LinearLayout.LayoutParams(isLandScape() ? 0 : -1, isLandScape() ? -1 : 0, 3.0f));
        freeFormAcceptView.setSplitMode(5);
        freeFormAcceptView.setPushTarget(pushTarget);
        freeFormAcceptView.setIsLandScape(isLandScape());
        freeFormAcceptView.setDragAnimationListener(this);
        freeFormAcceptView.setSplitBarController(this.mSplitBarController);
        freeFormAcceptView.setOriginalNotchStatus(this.mNotchStatus);
        configNotchHotArea(leftOrTopPushAcceptView, rightOrBottomPushAcceptView);
        pushFullHotArea.addView(leftOrTopPushAcceptView);
        pushFullHotArea.addView(freeFormAcceptView);
        pushFullHotArea.addView(rightOrBottomPushAcceptView);
        return pushFullHotArea;
    }

    private HwMultiWinClipImageView createBlurImageView() {
        HwMultiWinClipImageView blurImageView = new HwMultiWinClipImageView(this.mUiContext);
        blurImageView.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        return blurImageView;
    }

    private LinearLayout createBlurImgContainer() {
        LinearLayout blurImgContainer = new LinearLayout(this.mUiContext);
        blurImgContainer.setOrientation(!isLandScape());
        blurImgContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        return blurImgContainer;
    }

    private boolean isLandScape() {
        return this.mIsLeftRightSplit;
    }

    private void configLeftOrTopHotAreaForSwapSplit(HwMultiWinSwapAcceptView leftOrTopSwapAcceptView, HwMultiWinHotAreaView dropTarget, HwMultiWinClipImageView swapTarget) {
        leftOrTopSwapAcceptView.setIsLandScape(isLandScape());
        leftOrTopSwapAcceptView.setHotAreaLayout(this.mHotArea);
        leftOrTopSwapAcceptView.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
        leftOrTopSwapAcceptView.initSplitSwapAnimation(dropTarget, swapTarget);
        leftOrTopSwapAcceptView.setHwMultiWinHotAreaConfigListener(this);
        leftOrTopSwapAcceptView.setDragAnimationListener(this);
        leftOrTopSwapAcceptView.setSplitBarController(this.mSplitBarController);
        leftOrTopSwapAcceptView.setNotchInfo(this.mNotchPos, this.mNotchBound);
        leftOrTopSwapAcceptView.setOriginalNotchStatus(this.mNotchStatus);
    }

    private void configRightOrBottomHotAreaForSwapSplit(HwMultiWinSwapAcceptView rightOrBottomSwapAcceptView, HwMultiWinHotAreaView dropTarget, HwMultiWinClipImageView swapTarget) {
        rightOrBottomSwapAcceptView.setIsLandScape(isLandScape());
        rightOrBottomSwapAcceptView.setHotAreaLayout(this.mHotArea);
        rightOrBottomSwapAcceptView.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
        rightOrBottomSwapAcceptView.initSplitSwapAnimation(dropTarget, swapTarget);
        rightOrBottomSwapAcceptView.setHwMultiWinHotAreaConfigListener(this);
        rightOrBottomSwapAcceptView.setDragAnimationListener(this);
        rightOrBottomSwapAcceptView.setSplitBarController(this.mSplitBarController);
        rightOrBottomSwapAcceptView.setNotchInfo(this.mNotchPos, this.mNotchBound);
        rightOrBottomSwapAcceptView.setOriginalNotchStatus(this.mNotchStatus);
    }

    @Override // com.android.server.multiwin.listener.HwMultiWinHotAreaConfigListener
    public HwMultiWinSwapAcceptView onAddHwFreeFormSwapRegion(int height, int topMargin, HwMultiWinHotAreaView swapTarget) {
        int displayWidth = this.mDisplayBound.width();
        if (isLandScape() && (isNotchPosLeft() || isNotchPosRight())) {
            displayWidth -= this.mNotchBound.width();
        }
        int width = (int) (((float) displayWidth) * 0.6666667f);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.topMargin = topMargin;
        params.height = height;
        params.width = width;
        params.addRule(14);
        HwMultiWinSwapAcceptView freeFormSwapRegion = new HwMultiWinSwapAcceptView(this.mUiContext, 5);
        freeFormSwapRegion.setNotchInfo(this.mNotchPos, this.mNotchBound);
        freeFormSwapRegion.setOriginalNotchStatus(this.mNotchStatus);
        freeFormSwapRegion.setIsLandScape(isLandScape());
        freeFormSwapRegion.setHotAreaLayout(this.mHotArea);
        freeFormSwapRegion.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
        freeFormSwapRegion.initSplitSwapAnimation(getFreeFormRect(this.mActivityToken), swapTarget);
        freeFormSwapRegion.setDragAnimationListener(this);
        freeFormSwapRegion.setSplitBarController(this.mSplitBarController);
        freeFormSwapRegion.setLayoutParams(params);
        this.mHotArea.addView(freeFormSwapRegion);
        freeFormSwapRegion.bringToFront();
        return freeFormSwapRegion;
    }

    private void configHotAreaForSwapSplit(HwMultiWinHotAreaView dropTarget, HwMultiWinClipImageView swapTarget) {
        int rightOrBottomSplitMode;
        swapTarget.setIsLandScape(isLandScape());
        dropTarget.disableBorder();
        int leftOrTopSplitMode = isLandScape() ? 1 : 3;
        if (isLandScape()) {
            rightOrBottomSplitMode = 2;
        } else {
            rightOrBottomSplitMode = 4;
        }
        HwMultiWinSwapAcceptView leftOrTopSwapAcceptView = new HwMultiWinSwapAcceptView(this.mUiContext, leftOrTopSplitMode);
        configLeftOrTopHotAreaForSwapSplit(leftOrTopSwapAcceptView, dropTarget, swapTarget);
        HwMultiWinSwapAcceptView rightOrBottomSwapAcceptView = new HwMultiWinSwapAcceptView(this.mUiContext, rightOrBottomSplitMode);
        configRightOrBottomHotAreaForSwapSplit(rightOrBottomSwapAcceptView, dropTarget, swapTarget);
        configNotchHotArea(leftOrTopSwapAcceptView, rightOrBottomSwapAcceptView);
        leftOrTopSwapAcceptView.setOtherSplitSwapAcceptView(rightOrBottomSwapAcceptView);
        rightOrBottomSwapAcceptView.setOtherSplitSwapAcceptView(leftOrTopSwapAcceptView);
        HwMultiWinSwapAcceptView freeFormSwapAcceptView = new HwMultiWinSwapAcceptView(this.mUiContext, 5);
        freeFormSwapAcceptView.setNotchInfo(this.mNotchPos, this.mNotchBound);
        freeFormSwapAcceptView.setOriginalNotchStatus(this.mNotchStatus);
        freeFormSwapAcceptView.setIsLandScape(isLandScape());
        freeFormSwapAcceptView.setHotAreaLayout(this.mHotArea);
        freeFormSwapAcceptView.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
        freeFormSwapAcceptView.initSplitSwapAnimation(getFreeFormRect(this.mActivityToken), swapTarget);
        freeFormSwapAcceptView.setDragAnimationListener(this);
        freeFormSwapAcceptView.setSplitBarController(this.mSplitBarController);
        LinearLayout hotAreaForDragSplit = createHotAreaForDragSplit();
        LinearLayout splitSwapHotArea = createSplitSwapHotArea();
        float freeFormSwapHotAreaWeight = 1.0f;
        leftOrTopSwapAcceptView.setLayoutParams(new LinearLayout.LayoutParams(isLandScape() ? 0 : -1, isLandScape() ? -1 : 0, 1.0f));
        rightOrBottomSwapAcceptView.setLayoutParams(new LinearLayout.LayoutParams(isLandScape() ? 0 : -1, isLandScape() ? -1 : 0, 1.0f));
        int freeFormSwapHotAreaWidth = isLandScape() ? -1 : 0;
        int freeFormSwapHotAreaHeight = isLandScape() ? 0 : -1;
        if (isLandScape()) {
            freeFormSwapHotAreaWeight = FREEFORM_SWAP_REGION_HEIGHT_WEIGHT;
        }
        freeFormSwapAcceptView.setLayoutParams(new LinearLayout.LayoutParams(freeFormSwapHotAreaWidth, freeFormSwapHotAreaHeight, freeFormSwapHotAreaWeight));
        splitSwapHotArea.addView(leftOrTopSwapAcceptView, 0);
        splitSwapHotArea.addView(rightOrBottomSwapAcceptView, 1);
        hotAreaForDragSplit.addView(splitSwapHotArea);
        hotAreaForDragSplit.addView(freeFormSwapAcceptView, this.mIsLeftRightSplit ? 1 : 0);
        addExtraFreeFormHotAreaForVertical(hotAreaForDragSplit, swapTarget);
        this.mHotArea.addView(hotAreaForDragSplit);
    }

    private LinearLayout createSplitSwapHotArea() {
        LinearLayout splitSwapHotArea = new LinearLayout(this.mUiContext);
        int splitSwapHotAreaHeight = 0;
        splitSwapHotArea.setLayoutDirection(0);
        splitSwapHotArea.setOrientation(!isLandScape());
        int splitSwapHotAreaWidth = isLandScape() ? -1 : 0;
        if (!isLandScape()) {
            splitSwapHotAreaHeight = -1;
        }
        if (isLandScape()) {
        }
        splitSwapHotArea.setLayoutParams(new LinearLayout.LayoutParams(splitSwapHotAreaWidth, splitSwapHotAreaHeight, 1.0f));
        return splitSwapHotArea;
    }

    private LinearLayout createHotAreaForDragSplit() {
        LinearLayout hotAreaForDragSplit = new LinearLayout(this.mUiContext);
        hotAreaForDragSplit.setLayoutDirection(0);
        hotAreaForDragSplit.setOrientation(isLandScape() ? 1 : 0);
        hotAreaForDragSplit.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        return hotAreaForDragSplit;
    }

    private void addExtraFreeFormHotAreaForVertical(LinearLayout hotAreaForDragSplit, HwMultiWinClipImageView swapTarget) {
        if (!this.mIsLeftRightSplit) {
            HwMultiWinSwapAcceptView freeFormSwapAcceptView = new HwMultiWinSwapAcceptView(this.mUiContext, 5);
            freeFormSwapAcceptView.setNotchInfo(this.mNotchPos, this.mNotchBound);
            freeFormSwapAcceptView.setOriginalNotchStatus(this.mNotchStatus);
            freeFormSwapAcceptView.setIsLandScape(this.mIsLeftRightSplit);
            freeFormSwapAcceptView.setHotAreaLayout(this.mHotArea);
            freeFormSwapAcceptView.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
            freeFormSwapAcceptView.initSplitSwapAnimation(getFreeFormRect(this.mActivityToken), swapTarget);
            freeFormSwapAcceptView.setDragAnimationListener(this);
            freeFormSwapAcceptView.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
            hotAreaForDragSplit.addView(freeFormSwapAcceptView);
        }
    }

    public void handleRemoveHotArea() {
        WindowManager windowManager;
        Slog.d(TAG, "handleRemoveHotArea " + this.mRootView);
        RelativeLayout relativeLayout = this.mRootView;
        if (!(relativeLayout == null || (windowManager = this.mWindowManager) == null)) {
            try {
                windowManager.removeView(relativeLayout);
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "handleRemoveHotArea " + e.getMessage());
            }
        }
        this.mRootView = null;
    }

    private void swapSplitScreen(ActivityStack thisStack, TaskRecord thisTask) {
        List<ActivityStack> combinedStacks = this.mHwAtmsEx.mHwMwm.findCombinedSplitScreenStacks(thisStack);
        if (combinedStacks == null || combinedStacks.isEmpty() || combinedStacks.get(0) == null) {
            Slog.e(TAG, "combinedSplitScreenStacks is empty" + combinedStacks);
            return;
        }
        ActivityStack otherSideStack = combinedStacks.get(0);
        TaskRecord otherSideTask = otherSideStack.topTask();
        if (otherSideTask == null) {
            Slog.e(TAG, "otherSideTask is null " + otherSideTask);
            return;
        }
        int i = this.mSplitRatio;
        if (i != 0) {
            this.mSplitRatio = i == 1 ? 2 : 1;
            HwMultiWindowSplitUI multiWindowsplitUi = HwMultiWindowSplitUI.getInstance(this.mUiContext, this.mAtms);
            HwMultiWindowManager.getInstance(this.mAtms).resizeHwSplitStacks(this.mSplitRatio, false);
            Slog.d(TAG, "resize split stacks and update split ui");
            multiWindowsplitUi.updateViewPos(this.mSplitRatio);
        }
        ((ActivityTaskManagerService) this.mAtms).mWindowManager.startFreezingScreen(0, 0);
        this.mAtms.mWindowManager.mShouldResetTime = true;
        otherSideTask.reparent(thisStack, true, 1, true, true, "swapDockedAndFullscreenStack other -> current");
        thisTask.reparent(otherSideStack, true, 1, true, true, "swapDockedAndFullscreenStack current -> other");
        this.mAtms.mWindowManager.stopFreezingScreen();
        reportStatisticInfo(1041, false);
        Slog.d(TAG, "after reparent split screen");
    }

    private void takeSnapshotForFreeformReplaced() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord != null) {
                if (activityRecord.getDisplay() != null) {
                    for (int stackNdx = 0; stackNdx <= activityRecord.getDisplay().getChildCount() - 1; stackNdx++) {
                        ActivityStack stack = activityRecord.getDisplay().getChildAt(stackNdx);
                        if (stack.inHwFreeFormWindowingMode()) {
                            if (stack.isAlwaysOnTop()) {
                                ActivityRecord topActivity = stack.getTopActivity();
                                if (topActivity != null) {
                                    this.mAtms.mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topActivity.appToken, true);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void moveFreeFormToMiddle(ActivityStack freeFormStack) {
        synchronized (this.mAtms.getGlobalLock()) {
            if (this.mFreeFormDropBound == null) {
                Slog.w(TAG, "moveFreeFormToMiddle failed, cause mFreeFormDropBound is null!");
            } else if (freeFormStack == null) {
                Slog.w(TAG, "moveFreeFormToMiddle failed, cause freeFormStack is null!");
            } else {
                freeFormStack.resize(this.mFreeFormDropBound, (Rect) null, (Rect) null);
                this.mHwAtmsEx.updateDragFreeFormPos(this.mFreeFormDropBound, freeFormStack.getDisplay());
            }
        }
    }

    private void dropToFreeForm() {
        ActivityStack stack = null;
        if (this.mWindowingMode == 102) {
            Slog.d(TAG, "The activity mode is freeform, do nothing");
            ActivityRecord record = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (record != null) {
                stack = record.getActivityStack();
            }
            moveFreeFormToMiddle(stack);
            reportStatisticInfo(1038, false);
            return;
        }
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord record2 = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (record2 == null) {
                Slog.e(TAG, "ActivityRecord is null for token " + this.mActivityToken);
                return;
            }
            TaskRecord task = record2.task;
            if (task == null) {
                Slog.e(TAG, "thisTask is null for token " + this.mActivityToken);
                return;
            }
            int taskId = task.taskId;
            ActivityStack stack2 = task.getStack();
            int windowMode = stack2.getWindowingMode();
            Slog.i(TAG, "windowMode = " + windowMode + " taskId " + taskId + " stack = " + stack2);
            takeSnapshotForFreeformReplaced();
            this.mAtms.setTaskWindowingMode(taskId, 102, true);
            moveFreeFormToMiddle(stack2);
            this.mAtms.mRootActivityContainer.ensureActivitiesVisible((ActivityRecord) null, 0, true);
            reportStatisticInfo(1040, true);
        }
    }

    private void dropToSplitScreen(int windowModeToChange) {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord record = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (record != null) {
                if (record.task != null) {
                    TaskRecord thisTask = record.task;
                    ActivityStack thisStack = thisTask.getStack();
                    int thisWindowMode = thisStack.getWindowingMode();
                    int otherSideModeToChange = 101;
                    if (windowModeToChange == 101) {
                        otherSideModeToChange = 100;
                    }
                    Slog.i(TAG, "dropToSplitScreen windowMode: " + thisWindowMode + " changeTo " + windowModeToChange + " stack: " + thisStack);
                    if (WindowConfiguration.isHwMultiStackWindowingMode(thisWindowMode) && thisWindowMode == otherSideModeToChange) {
                        swapSplitScreen(thisStack, thisTask);
                    } else if (WindowConfiguration.isHwFreeFormWindowingMode(thisWindowMode)) {
                        ActivityStack topStack = this.mHwAtmsEx.mHwMwm.getFilteredTopStack(thisStack.getDisplay(), Arrays.asList(5, 2, 102));
                        if (!(topStack == null || topStack.getActivityType() == 3)) {
                            if (topStack.getActivityType() != 2) {
                                Slog.d(TAG, "switch from freeform to split, top stack " + topStack);
                                if (topStack.getWindowingMode() != 1) {
                                    if (topStack.getWindowingMode() != 103) {
                                        if (topStack.inHwSplitScreenWindowingMode()) {
                                            thisStack.setWindowingMode(windowModeToChange);
                                            reportStatisticInfo(1036, false);
                                        } else {
                                            Slog.w(TAG, "Top stack is invalid " + topStack);
                                        }
                                        this.mHwAtmsEx.mHwMwm.addSplitScreenDividerBar(thisStack.mDisplayId, 100);
                                    }
                                }
                                HwMagicWindowService mMagicWinService = getMagicWindowService();
                                ActivityStack newTopStack = mMagicWinService != null ? mMagicWinService.getNewTopStack(topStack, otherSideModeToChange) : null;
                                (newTopStack != null ? newTopStack : topStack).setWindowingMode(otherSideModeToChange, false, false, false, true, false);
                                this.mAtms.setTaskWindowingMode(thisTask.taskId, windowModeToChange, true);
                                reportStatisticInfo(1037, false);
                                this.mHwAtmsEx.mHwMwm.addSplitScreenDividerBar(thisStack.mDisplayId, 100);
                            }
                        }
                        Slog.w(TAG, "Top stack is null or home or recents" + topStack);
                        return;
                    } else {
                        Slog.i(TAG, "No need to switch mode " + thisWindowMode);
                        reportStatisticInfo(1042, false);
                    }
                    return;
                }
            }
            Slog.e(TAG, "ActivityRecord or task is null for token " + this.mActivityToken);
        }
    }

    private Rect getHotAreaFreeFormDropBound() {
        Rect rect = new Rect();
        if (this.mRootView == null) {
            Slog.w(TAG, "mRootView is null, getHotAreaFreeFormDropBound failed!");
            return rect;
        }
        float centerRatio = 0.5f;
        if (this.mIsInSplitScreenMode && WindowConfiguration.isHwFreeFormWindowingMode(this.mWindowingMode)) {
            int i = this.mSplitRatio;
            centerRatio = i == 1 ? 0.33333334f : i == 2 ? 0.6666667f : 0.5f;
        }
        int left = 0;
        int top = 0;
        int right = this.mDisplayBound.width();
        int bottom = this.mDisplayBound.height();
        if (isLandScape()) {
            if (isNotchPosLeft()) {
                left = this.mNotchBound.width();
            }
            if (isNotchPosRight()) {
                right -= this.mNotchBound.width();
            }
        } else if (isNotchPosTop()) {
            top = this.mNotchBound.height();
        }
        float centerX = isLandScape() ? ((float) (left + right)) * centerRatio : ((float) (left + right)) / 2.0f;
        float centerY = isLandScape() ? ((float) (top + bottom)) / 2.0f : ((float) (top + bottom)) * centerRatio;
        Rect rect2 = getFreeFormRect(this.mActivityToken);
        int freeFormWidth = rect2.width();
        int freeFormHeight = rect2.height();
        Rect dropTargetBound = new Rect();
        dropTargetBound.left = (int) (centerX - (((float) freeFormWidth) / 2.0f));
        dropTargetBound.top = (int) (centerY - (((float) freeFormHeight) / 2.0f));
        dropTargetBound.right = (int) ((((float) freeFormWidth) / 2.0f) + centerX);
        dropTargetBound.bottom = (int) ((((float) freeFormHeight) / 2.0f) + centerY);
        Rect dropTargetBound2 = adjustFreeFormDropBound(this.mActivityToken, dropTargetBound);
        Slog.d(TAG, "getHotAreaFreeFormDropBound: dropTargetBound = " + dropTargetBound2 + ", mDisplayBound = " + this.mDisplayBound + ", centerRatio = " + centerRatio + ", mIsInSplitScreenMode = " + this.mIsInSplitScreenMode + ", mWindowingMode = " + this.mWindowingMode);
        this.mFreeFormDropBound = dropTargetBound2;
        return dropTargetBound2;
    }

    private Rect adjustFreeFormDropBound(IBinder freeFormToken, Rect originalBound) {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(freeFormToken);
            if (activityRecord == null) {
                Slog.w(TAG, "getFreeFormRect failed: activityRecord is null!");
                return originalBound;
            }
            TaskRecord taskRecord = activityRecord.task;
            if (taskRecord == null) {
                Slog.w(TAG, "getFreeFormRect failed: taskRecord is null!");
                return originalBound;
            }
            ActivityStack activityStack = taskRecord.getStack();
            if (activityStack == null) {
                Slog.w(TAG, "getFreeFormRect failed: activityStack is null!");
                return originalBound;
            }
            Rect bound = this.mHwAtmsEx.relocateOffScreenWindow(originalBound, activityStack);
            Slog.d(TAG, "adjustFreeFormDropBound: originalBound = " + originalBound + ", bound = " + bound);
            return bound;
        }
    }

    private Rect getCurrentTokenBound() {
        Rect rect = new Rect();
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.w(TAG, "getCurrentTokenBound failed: activityRecord is null!");
                return rect;
            }
            activityRecord.getBounds(rect);
            Slog.d(TAG, "getCurrentTokenBound = " + rect);
            return rect;
        }
    }

    private Rect getFreeFormRect(IBinder freeFormToken) {
        Rect rect = new Rect();
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(freeFormToken);
            if (activityRecord == null) {
                Slog.w(TAG, "getFreeFormRect failed: activityRecord is null!");
                return rect;
            }
            TaskRecord taskRecord = activityRecord.task;
            if (taskRecord == null) {
                Slog.w(TAG, "getFreeFormRect failed: taskRecord is null!");
                return rect;
            }
            ActivityStack activityStack = taskRecord.getStack();
            if (activityStack == null) {
                Slog.w(TAG, "getFreeFormRect failed: activityStack is null!");
                return rect;
            }
            HwMultiWindowManager manager = HwMultiWindowManager.getInstance(this.mAtms);
            ActivityDisplay activityDisplay = activityStack.getDisplay();
            if (activityDisplay == null) {
                Slog.w(TAG, "getFreeFormRect failed: activityDisplay is null!");
                return rect;
            }
            manager.calcDefaultFreeFormBounds(rect, activityDisplay.mDisplayContent);
            Slog.d(TAG, "getFreeFormRect: rect = " + rect);
            return rect;
        }
    }

    private int dipToPix(float dipValue) {
        return (int) ((dipValue * this.mUiContext.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private int getStatusBarHeight() {
        return this.mUiContext.getResources().getDimensionPixelSize(17105443);
    }

    private Bitmap getDragSplitScreenShot(List<Bitmap> splitScreenShots) {
        Bitmap dragBmp;
        if (splitScreenShots == null || splitScreenShots.isEmpty()) {
            Slog.w(TAG, "getDragSplitScreenShot failed, cause splitScreenShots is null!");
            return null;
        } else if (splitScreenShots.size() <= 1) {
            Slog.w(TAG, "getDragSplitScreenShot failed, cause splitScreenShots size is less than two");
            return null;
        } else {
            if (isDragPrimary()) {
                dragBmp = splitScreenShots.get(0);
            } else if (isDragSecondary()) {
                dragBmp = splitScreenShots.get(1);
            } else {
                Slog.w(TAG, "getDragSplitScreenShot failed, cause not drag split!");
                return null;
            }
            if (dragBmp == null) {
                Slog.w(TAG, "getDragSplitScreenShot failed, cause dragBmp is null");
                return dragBmp;
            }
            Slog.d(TAG, "getCurrentTokenSnapShot: dragBmp = " + dragBmp + ", width = " + dragBmp.getWidth() + ", height = " + dragBmp.getHeight() + ", mSnapShotScaleFactor = " + this.mSnapShotScaleFactor);
            if (HwMultiWinUtils.isNeedToResizeWithoutNavBar(HwMultiWinUtils.convertWindowMode2SplitMode(this.mWindowingMode, isLandScape()), this.mNavBarPos)) {
                return HwMultiWinUtils.getScreenShotBmpWithoutNavBar(dragBmp, this.mNavBarPos, this.mNavBarBound, this.mSnapShotScaleFactor);
            }
            return dragBmp;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:62:0x0151, code lost:
        if (r0.size() <= 1) goto L_0x0154;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x0153, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x0154, code lost:
        r17.mIsInSplitScreenMode = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:?, code lost:
        r0 = r0.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x015e, code lost:
        if (r0.hasNext() == false) goto L_0x01bc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x0160, code lost:
        r2 = r0.next();
        r6 = android.graphics.Bitmap.wrapHardwareBuffer(android.hardware.HardwareBuffer.createFromGraphicBuffer(r2.getSnapshot()), r2.getColorSpace());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x0176, code lost:
        if (r6 == null) goto L_0x019d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x0178, code lost:
        r7 = r6.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0183, code lost:
        if (r2.inHwMagicWindowingMode() == false) goto L_0x018a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0185, code lost:
        r7 = mergeWallpaperScreenShot(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x018c, code lost:
        if (r17.mIsInSplitScreenMode != false) goto L_0x0199;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x0192, code lost:
        if (isNotchPosRight() == false) goto L_0x0199;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x0194, code lost:
        r7 = cutoutRightNotchPart(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0199, code lost:
        r0.add(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x01a0, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x01a1, code lost:
        android.util.Slog.e(com.android.server.wm.HwMultiWindowSwitchManager.TAG, "wrapHardwareBuffer get IllegalArgumentException" + r0.getMessage());
     */
    private List<Bitmap> getTopStackSnapShot() {
        boolean z;
        int width;
        int height;
        List<Bitmap> maps = new ArrayList<>(2);
        List<ActivityManager.TaskSnapshot> snapShots = new ArrayList<>(2);
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                return maps;
            }
            ActivityStack topStack = this.mHwAtmsEx.mHwMwm.getFilteredTopStack(activityRecord.getDisplay(), Arrays.asList(5, 2, 102));
            if (topStack == null) {
                Slog.e(TAG, "Top stack is null topStack = " + topStack);
                return maps;
            }
            TaskRecord topTask = topStack.topTask();
            if (topTask == null) {
                Slog.e(TAG, "Top stack is null topTask = " + topTask);
                return maps;
            }
            Rect topStackBound = topStack.getBounds();
            boolean isUsingTaskBoundOffsetAsNotch = this.mNotchBound.isEmpty() && topStackBound != null;
            if (isUsingTaskBoundOffsetAsNotch) {
                this.mNotchBound.set(0, 0, topStackBound.right, topStackBound.top);
                this.mNotchPos = 0;
            }
            ActivityManager.TaskSnapshot shot = this.mHwAtmsEx.getTaskSnapshot(topTask.taskId, false);
            if (shot != null) {
                this.mSnapShotScaleFactor = shot.getScale();
                snapShots.add(shot);
            }
            if (topStack.inHwSplitScreenWindowingMode()) {
                List<ActivityStack> otherStacks = this.mHwAtmsEx.mHwMwm.findCombinedSplitScreenStacks(topStack);
                ActivityManager.TaskSnapshot shot2 = null;
                if (otherStacks != null && otherStacks.size() > 0) {
                    TaskRecord otherTask = otherStacks.get(0).topTask();
                    if (otherTask != null) {
                        shot2 = this.mHwAtmsEx.getTaskSnapshot(otherTask.taskId, false);
                    }
                    Rect otherStackBound = otherStacks.get(0).getBounds();
                    if (isUsingTaskBoundOffsetAsNotch && otherStackBound != null) {
                        if (this.mNotchBound.width() < otherStackBound.width()) {
                            width = this.mNotchBound.width();
                        } else {
                            width = otherStackBound.right;
                        }
                        if (this.mNotchBound.height() < otherStackBound.height()) {
                            height = this.mNotchBound.height();
                        } else {
                            height = otherStackBound.top;
                        }
                        this.mNotchBound.set(0, 0, width, height);
                    }
                }
                if (shot2 == null) {
                    z = false;
                } else if (topStack.inHwSplitScreenPrimaryWindowingMode()) {
                    snapShots.add(shot2);
                    z = false;
                } else {
                    z = false;
                    snapShots.add(0, shot2);
                }
            } else {
                z = false;
            }
            HwMagicWindowService mMagicWinService = getMagicWindowService();
            if (mMagicWinService != null) {
                mMagicWinService.addOtherSnapShot(topStack, this.mHwAtmsEx, snapShots);
            }
        }
        return maps;
    }

    private Bitmap cutoutRightNotchPart(Bitmap src) {
        if (src == null) {
            Slog.w(TAG, "cutoutRightNotchPart failed, cause src is null!");
            return src;
        }
        Rect rect = this.mNotchBound;
        if (rect != null && !rect.isEmpty()) {
            return Bitmap.createBitmap(src, 0, 0, src.getWidth() - this.mNotchBound.width(), src.getHeight());
        }
        Slog.w(TAG, "cutoutRightNotchPart failed, cause mNotchBound is null or empty!");
        return src;
    }

    private Bitmap mergeWallpaperScreenShot(Bitmap taskSnapShot) {
        Bitmap wallpaperScreenShot = HwMultiWinUtils.getWallpaperScreenShot(this.mAtms);
        if (wallpaperScreenShot == null || taskSnapShot == null) {
            Slog.w(TAG, "mergeWallpaperScreenShot failed, cause wallpaperScreenShot is null!");
            return taskSnapShot;
        }
        int taskSnapShotWidth = taskSnapShot.getWidth();
        int taskSnapShotHeight = taskSnapShot.getHeight();
        Bitmap wallpaperScreenShot2 = Bitmap.createScaledBitmap(wallpaperScreenShot, taskSnapShotWidth, taskSnapShotHeight, false);
        new Canvas(wallpaperScreenShot2).drawBitmap(taskSnapShot, new Rect(0, 0, taskSnapShotWidth, taskSnapShotHeight), new Rect(0, 0, wallpaperScreenShot2.getWidth(), wallpaperScreenShot2.getHeight()), new Paint());
        return wallpaperScreenShot2;
    }

    private void handleDragAdapterAnimation(Bundle info) {
        if (info == null) {
            Slog.w(TAG, "handleDragAdapterAnimation failed, cause info is null!");
            return;
        }
        int animType = 0;
        if (info.containsKey(HwMultiWinConstants.ANIM_TYPE_KEY_STR)) {
            animType = info.getInt(HwMultiWinConstants.ANIM_TYPE_KEY_STR);
        }
        Rect bound = null;
        if (info.containsKey(HwMultiWinConstants.DROP_TARGET_BOUND_KEY_STR)) {
            bound = (Rect) info.getParcelable(HwMultiWinConstants.DROP_TARGET_BOUND_KEY_STR);
        }
        Rect dropTargetBound = bound == null ? new Rect() : bound;
        Log.d(TAG, "handleDragAdapterAnimation: animType = " + animType + ", dropTargetBound = " + dropTargetBound);
        HwMultiWinDragAnimationAdapter hwMultiWinDragAnimationAdapter = this.mDragAnimationAdapter;
        if (hwMultiWinDragAnimationAdapter == null) {
            Log.w(TAG, "handleDragAdapterAnimation: mDragAnimationAdapter is null!");
            return;
        }
        switch (animType) {
            case 3:
                hwMultiWinDragAnimationAdapter.startRecoverAnimation();
                this.mDragAnimationAdapter.startExitSplitAlphaAnimation();
                return;
            case 4:
                hwMultiWinDragAnimationAdapter.startDropSplitScreenAnimation(dropTargetBound, null);
                return;
            case 5:
                hwMultiWinDragAnimationAdapter.startEnterSplitAlphaAnimation();
                return;
            case 6:
                hwMultiWinDragAnimationAdapter.startDropDisappearAnimation(dropTargetBound, null);
                return;
            case 7:
                hwMultiWinDragAnimationAdapter.startSplitEnterFreeFormAnimation();
                return;
            case 8:
                hwMultiWinDragAnimationAdapter.startSplitExitFreeFormAnimation();
                return;
            case 9:
                hwMultiWinDragAnimationAdapter.startSplitDropSplit(null);
                return;
            default:
                return;
        }
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragStarted() {
        if (this.mIsDragBarReset) {
            Slog.d(TAG, "onDragStarted: reset mIsDragBarReset to false");
            this.mIsDragBarReset = false;
        }
        if (!this.mHasDragStarted) {
            hideNotchStatusIfNeeded();
            this.mHasDragStarted = true;
            cleanUpInputSurface();
        }
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragEntered(View v, DragEvent event, int splitMode, int dragSurfaceAnimType) {
        Bundle info = new Bundle();
        info.putInt(HwMultiWinConstants.SPLIT_MODE_KEY_STR, splitMode);
        info.putInt(HwMultiWinConstants.ANIM_TYPE_KEY_STR, dragSurfaceAnimType);
        Slog.d(TAG, "onDragEntered: splitMode = " + splitMode + ", dragSurfaceAnimType = " + dragSurfaceAnimType);
        info.putInt(HwMultiWinConstants.STATUS_KEY_STR, 2);
        handleDragAdapterAnimation(info);
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragLocation() {
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragExited(View v) {
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDrop(View v, DragEvent event, int splitMode, Rect dropBounds, int dragSurfaceAnimType) {
        Rect dropTargetBound;
        Slog.d(TAG, "onDrop: splitMode = " + splitMode + ", dragSurfaceAnimType = " + dragSurfaceAnimType);
        this.mLastDropSplitMode = splitMode;
        Bundle info = new Bundle();
        info.putInt(HwMultiWinConstants.ANIM_TYPE_KEY_STR, dragSurfaceAnimType);
        if (v.getId() == 34603260 || v.getId() == 34603273) {
            Slog.d(TAG, "onDrop: view is left or right screen");
        } else if (v.getId() == 34603256) {
            Slog.i(TAG, "onDrop: view is freeform");
            info.putInt(HwMultiWinConstants.SPLIT_MODE_KEY_STR, 5);
            info.putParcelable(HwMultiWinConstants.DROP_TARGET_BOUND_KEY_STR, getHotAreaFreeFormDropBound());
        } else {
            Slog.i(TAG, "onDrop: view is other splitMode = " + splitMode);
            info.putInt(HwMultiWinConstants.SPLIT_MODE_KEY_STR, splitMode);
            if (splitMode == 5) {
                dropTargetBound = getHotAreaFreeFormDropBound();
            } else {
                dropTargetBound = dropBounds;
            }
            info.putParcelable(HwMultiWinConstants.DROP_TARGET_BOUND_KEY_STR, dropTargetBound);
        }
        handleDragAdapterAnimation(info);
        if (isDragFreeForm() || (isDragSplit() && this.mLastDropSplitMode == 5)) {
            handleSwitchScreen();
        }
        this.mIsDropHandled = true;
    }

    private void saveNotchStatus() {
        if (HwMultiWindowManager.IS_NOTCH_PROP) {
            this.mNotchStatus = Settings.Secure.getInt(this.mUiContext.getContentResolver(), "display_notch_status", 0);
        }
    }

    private void hideNotchStatusIfNeeded() {
        if (HwMultiWindowManager.IS_NOTCH_PROP && this.mNotchStatus != 0) {
            Slog.i(TAG, "onDragStarted: hide notch status, previous notchStatus = " + this.mNotchStatus);
            Settings.Secure.putInt(this.mUiContext.getContentResolver(), "display_notch_status", 0);
            this.mIsNotchStatusChanged = true;
        }
    }

    private void restoreNotchStatusIfNeeded() {
        if (HwMultiWindowManager.IS_NOTCH_PROP && this.mIsNotchStatusChanged) {
            Settings.Secure.putInt(this.mUiContext.getContentResolver(), "display_notch_status", this.mNotchStatus);
            Slog.i(TAG, "onDragEnded: restore notch status = " + this.mNotchStatus);
            this.mIsNotchStatusChanged = false;
        }
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragEnded(boolean isSuccessDrop) {
        if (!this.mHasDragEnded) {
            handleSwitchScreen();
            restoreNotchStatusIfNeeded();
            this.mHasDragEnded = true;
            this.mIsProcessingDrag = false;
            Slog.i(TAG, "onDragEnded Success");
        }
        Slog.d(TAG, "onDragEnded isSuccessDrop = " + isSuccessDrop);
        if (isSuccessDrop) {
            addScreenShotCover();
            HwMultiWinSplitBarController hwMultiWinSplitBarController = this.mSplitBarController;
            if (hwMultiWinSplitBarController != null) {
                hwMultiWinSplitBarController.showSplitBar();
            }
        }
        if (!this.mIsDropHandled && !this.mIsDropFailedCleanUp) {
            Slog.d(TAG, "onDragEnded: do clean up");
            handleRemoveHotArea();
            this.mIsDropFailedCleanUp = true;
        }
    }

    private void handleSwitchScreen() {
        if (this.mIsScreenSwitchHandled) {
            Slog.d(TAG, "handleSwitchScreen has been handled, just return!");
            return;
        }
        try {
            Slog.i(TAG, "handleSwitchScreen: mActivityToken = " + this.mActivityToken + ", mLastDropSplitMode = " + this.mLastDropSplitMode + ", isFreeFormDragged = " + WindowConfiguration.isHwFreeFormWindowingMode(this.mWindowingMode));
            if (this.mLastDropSplitMode != 1) {
                if (this.mLastDropSplitMode != 3) {
                    if (this.mLastDropSplitMode != 2) {
                        if (this.mLastDropSplitMode != 4) {
                            if (this.mLastDropSplitMode == 5) {
                                dropToFreeForm();
                            } else {
                                handleRemoveHotArea();
                            }
                        }
                    }
                    dropToSplitScreen(101);
                }
            }
            dropToSplitScreen(100);
        } finally {
            this.mIsScreenSwitchHandled = true;
        }
    }

    private void addScreenShotCover() {
        removeScreenShotCover();
        int topHeightSubstraction = 0;
        if (this.mNotchPos == 0 && !this.mNotchBound.isEmpty()) {
            topHeightSubstraction = this.mNotchBound.height();
        }
        Bitmap screenShot = HwMultiWinUtils.takeScreenshot(topHeightSubstraction);
        this.mScreenShotCover = new ImageView(this.mUiContext);
        this.mScreenShotCover.setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wm.HwMultiWindowSwitchManager.AnonymousClass2 */

            public void onClick(View v) {
                HwMultiWindowSwitchManager.this.removeScreenShotCover();
            }
        });
        this.mScreenShotCover.setImageBitmap(screenShot);
        WindowManager.LayoutParams layoutParams = createBasicLayoutParams(topHeightSubstraction);
        layoutParams.setTitle("MultiWindow - ScreenShotCover");
        this.mScreenShotCover.setLayoutParams(layoutParams);
        this.mWindowManager.addView(this.mScreenShotCover, layoutParams);
        Slog.d(TAG, "addScreenShotCover");
        this.mScreenShotCover.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            /* class com.android.server.wm.HwMultiWindowSwitchManager.AnonymousClass3 */

            public void onGlobalLayout() {
                HwMultiWindowSwitchManager.this.handleRemoveHotArea();
                HwMultiWindowSwitchManager.this.removeScreenShotCoverWithAnimation(HwMultiWindowSwitchManager.SCREEN_SHOT_COVER_REMOVE_ANIM_DELAY);
                HwMultiWindowSwitchManager.this.mScreenShotCover.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @SuppressLint({"NewApi"})
    public void removeScreenShotCoverWithAnimation(long delay) {
        ObjectAnimator objectAnimator = this.mScreenShotCoverRemoveAnimator;
        if (objectAnimator != null && objectAnimator.isRunning()) {
            this.mScreenShotCoverRemoveAnimator.cancel();
        }
        ImageView imageView = this.mScreenShotCover;
        if (imageView == null) {
            Slog.w(TAG, "removeScreenShotCoverWithAnimation failed, cause mScreenShotCover is null!");
            return;
        }
        float fromAlpha = imageView.getAlpha();
        this.mScreenShotCoverRemoveAnimator = ObjectAnimator.ofFloat(this.mScreenShotCover, ALPHA_PROPERTY_NAME, fromAlpha, 0.0f);
        this.mScreenShotCoverRemoveAnimator.setDuration(SCREEN_SHOT_COVER_REMOVE_ANIM_DURATION);
        this.mScreenShotCoverRemoveAnimator.setStartDelay(delay);
        this.mScreenShotCoverRemoveAnimator.setInterpolator(new SharpCurveInterpolator());
        this.mScreenShotCoverRemoveAnimator.addListener(new AnimatorListenerAdapter() {
            /* class com.android.server.wm.HwMultiWindowSwitchManager.AnonymousClass4 */

            public void onAnimationCancel(Animator animation) {
                HwMultiWindowSwitchManager.this.removeScreenShotCover();
            }

            public void onAnimationEnd(Animator animation) {
                HwMultiWindowSwitchManager.this.removeScreenShotCover();
            }
        });
        this.mScreenShotCoverRemoveAnimator.start();
    }

    public void removeScreenShotCover() {
        ImageView imageView = this.mScreenShotCover;
        if (imageView != null && this.mWindowManager != null) {
            try {
                imageView.setVisibility(8);
                this.mWindowManager.removeView(this.mScreenShotCover);
                Slog.d(TAG, "removeScreenShotCover " + this.mScreenShotCover);
                this.mScreenShotCover = null;
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "removeScreenShotCover " + e.getMessage());
            }
        }
    }

    /* JADX INFO: finally extract failed */
    private void reportStatisticInfo(int eventId, boolean isSwitchToFreeform) {
        String pkgName = "";
        String fullScreen = "";
        String primarySplit = "";
        String secondarySplit = "";
        String freeform = "";
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord != null) {
                pkgName = activityRecord.packageName;
            }
        }
        long token = Binder.clearCallingIdentity();
        try {
            List<ActivityManager.RunningTaskInfo> taskInfos = this.mHwAtmsEx.getVisibleTasks();
            Binder.restoreCallingIdentity(token);
            for (ActivityManager.RunningTaskInfo taskInfo : taskInfos) {
                if (taskInfo.topActivity == null) {
                    Slog.e(TAG, "topActivity is null for task: " + taskInfo);
                } else {
                    int i = taskInfo.windowMode;
                    if (i != 1) {
                        switch (i) {
                            case 100:
                                primarySplit = taskInfo.topActivity.getPackageName();
                                break;
                            case 101:
                                secondarySplit = taskInfo.topActivity.getPackageName();
                                break;
                            case 102:
                                if (!isSwitchToFreeform) {
                                    freeform = taskInfo.topActivity.getPackageName();
                                    break;
                                } else {
                                    freeform = pkgName;
                                    break;
                                }
                        }
                    }
                    fullScreen = taskInfo.topActivity.getPackageName();
                }
            }
            String statisticInfo = String.format(Locale.ROOT, "{\"pkg\":\"%s\", \"full\":\"%s\", \"prim\":\"%s\", \"2nd\":\"%s\", \"free\":\"%s\"}", pkgName, fullScreen, primarySplit, secondarySplit, freeform);
            Flog.bdReport(this.mUiContext, eventId, statisticInfo);
            Slog.i(TAG, "eventId: " + eventId + ", " + statisticInfo);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    @Override // com.android.server.multiwin.listener.BlurListener
    public void onBlurDone() {
        this.mAppFullView.playFullScreenShotDismissAnimation();
        this.mBlurAppFullView.playIconShowAnimation();
    }

    private Drawable getCurrentTokenIcon() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.e(TAG, "getTopAppIcons failed: activityRecord is null for token " + this.mActivityToken);
                return null;
            }
            return HwMultiWinUtils.getAppIcon(this.mUiContext, activityRecord.packageName, activityRecord.mUserId);
        }
    }

    private List<Drawable> getTopAppIcons() {
        List<Drawable> icons = new ArrayList<>(2);
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.e(TAG, "getTopAppIcons failed: activityRecord is null for token " + this.mActivityToken);
                return icons;
            }
            ActivityStack topStack = this.mHwAtmsEx.mHwMwm.getFilteredTopStack(activityRecord.getDisplay(), Arrays.asList(5, 2, 102));
            if (topStack != null) {
                if (topStack.topTask() != null) {
                    ActivityRecord topActivityRecord = topStack.topTask().getTopActivity();
                    if (topActivityRecord == null) {
                        Slog.w(TAG, "getTopAppIcons failed: topActivityRecord is null!");
                        return icons;
                    }
                    Drawable icon = HwMultiWinUtils.getAppIcon(this.mUiContext, topActivityRecord.packageName, topActivityRecord.mUserId);
                    if (icon != null) {
                        icons.add(icon);
                    }
                    if (topStack.inHwSplitScreenWindowingMode()) {
                        List<ActivityStack> otherStacks = this.mHwAtmsEx.mHwMwm.findCombinedSplitScreenStacks(topStack);
                        Drawable icon2 = null;
                        if (otherStacks != null) {
                            if (otherStacks.size() > 0) {
                                TaskRecord otherTask = otherStacks.get(0).topTask();
                                if (otherTask == null) {
                                    return icons;
                                }
                                ActivityRecord otherTopActivityRecord = otherTask.getTopActivity();
                                if (otherTopActivityRecord != null) {
                                    icon2 = HwMultiWinUtils.getAppIcon(this.mUiContext, otherTopActivityRecord.packageName, otherTopActivityRecord.mUserId);
                                }
                                if (icon2 != null) {
                                    if (topStack.inHwSplitScreenPrimaryWindowingMode()) {
                                        icons.add(icon2);
                                    } else {
                                        icons.add(0, icon2);
                                    }
                                }
                            }
                        }
                        return icons;
                    }
                    return icons;
                }
            }
            Slog.e(TAG, "getTopAppIcons failed: top stack or top task is null " + topStack);
            return icons;
        }
    }

    private HwMagicWindowService getMagicWindowService() {
        return HwMultiWindowManager.getInstance(this.mAtms).getHwMagicWindowService();
    }

    private final class HwMultiWindowInputInterceptor {
        InputApplicationHandle mApplicationHandle = new InputApplicationHandle(new Binder());
        InputChannel mClientChannel;
        InputChannel mServerChannel;
        InputWindowHandle mWindowHandle;

        HwMultiWindowInputInterceptor(Display display) {
            InputChannel[] channels = InputChannel.openInputChannelPair(HwMultiWindowSwitchManager.TAG);
            this.mServerChannel = channels[0];
            this.mClientChannel = channels[1];
            HwMultiWindowSwitchManager.this.mWms.mInputManager.registerInputChannel(this.mServerChannel, (IBinder) null);
            InputApplicationHandle inputApplicationHandle = this.mApplicationHandle;
            inputApplicationHandle.name = HwMultiWindowSwitchManager.TAG;
            inputApplicationHandle.dispatchingTimeoutNanos = 5000000000L;
            this.mWindowHandle = new InputWindowHandle(inputApplicationHandle, (IWindow) null, display.getDisplayId());
            InputWindowHandle inputWindowHandle = this.mWindowHandle;
            inputWindowHandle.name = HwMultiWindowSwitchManager.TAG;
            inputWindowHandle.token = this.mServerChannel.getToken();
            this.mWindowHandle.layer = HwMultiWindowSwitchManager.this.mWms.getDragLayerLocked();
            InputWindowHandle inputWindowHandle2 = this.mWindowHandle;
            inputWindowHandle2.layoutParamsFlags = 545259520;
            inputWindowHandle2.layoutParamsType = HwArbitrationDEFS.MSG_WIFI_PLUS_ENABLE;
            inputWindowHandle2.dispatchingTimeoutNanos = 5000000000L;
            inputWindowHandle2.visible = true;
            inputWindowHandle2.canReceiveKeys = false;
            inputWindowHandle2.hasFocus = true;
            inputWindowHandle2.hasWallpaper = false;
            inputWindowHandle2.paused = false;
            inputWindowHandle2.ownerPid = Process.myPid();
            this.mWindowHandle.ownerUid = Process.myUid();
            InputWindowHandle inputWindowHandle3 = this.mWindowHandle;
            inputWindowHandle3.inputFeatures = 0;
            inputWindowHandle3.scaleFactor = 1.0f;
            inputWindowHandle3.touchableRegion.setEmpty();
            InputWindowHandle inputWindowHandle4 = this.mWindowHandle;
            inputWindowHandle4.frameLeft = 0;
            inputWindowHandle4.frameTop = 0;
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            this.mWindowHandle.frameRight = displaySize.x;
            this.mWindowHandle.frameBottom = displaySize.y;
        }

        /* access modifiers changed from: package-private */
        public void tearDown() {
            HwMultiWindowSwitchManager.this.mWms.mInputManager.unregisterInputChannel(this.mServerChannel);
            this.mClientChannel.dispose();
            this.mServerChannel.dispose();
            this.mClientChannel = null;
            this.mServerChannel = null;
            this.mWindowHandle = null;
            this.mApplicationHandle = null;
        }
    }
}
