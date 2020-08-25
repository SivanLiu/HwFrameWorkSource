package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.HwMwUtils;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavPolicy;
import com.android.server.magicwin.HwMagicWindowService;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.huawei.android.fsm.HwFoldScreenManager;
import com.huawei.server.multiwindowtip.HwMultiWindowTips;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HwMultiWindowManager {
    public static final int DEFATUL_FOREGROUND_FREEFORM_STACK_LIMIT = 1;
    private static final String DOCKBAR_PACKAGE_NAME = "com.huawei.hwdockbar";
    private static final float FLOAT_FRECISION = 0.001f;
    private static final int FREEFORM_DRAGBAR_RATIO = 3;
    private static final int GUTTER_IN_DP = 24;
    private static final int HALF_DIVISOR = 2;
    public static final String HEIGHT_COLUMNS = "heightColumns";
    public static final String HW_SPLIT_SCREEN_PRIMARY_BOUNDS = "primaryBounds";
    public static final int HW_SPLIT_SCREEN_PRIMARY_EITHER = -1;
    public static final int HW_SPLIT_SCREEN_PRIMARY_LEFT = 1;
    public static final String HW_SPLIT_SCREEN_PRIMARY_POSITION = "primaryPosition";
    public static final int HW_SPLIT_SCREEN_PRIMARY_TOP = 0;
    public static final int HW_SPLIT_SCREEN_RATIO_DEFAULT = 0;
    public static final int HW_SPLIT_SCREEN_RATIO_PRAIMARY_LESS_THAN_DEFAULT = 1;
    public static final int HW_SPLIT_SCREEN_RATIO_PRAIMARY_MORE_THAN_DEFAULT = 2;
    public static final int HW_SPLIT_SCREEN_RATIO_PRIMARY_FULL = 3;
    public static final int HW_SPLIT_SCREEN_RATIO_SECONDARY_FULL = 4;
    public static final String HW_SPLIT_SCREEN_RATIO_VALUES = "splitRatios";
    public static final String HW_SPLIT_SCREEN_SECONDARY_BOUNDS = "secondaryBounds";
    public static final boolean IS_HW_MULTIWINDOW_SUPPORTED = SystemProperties.getBoolean("ro.config.hw_multiwindow_optimization", false);
    public static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    public static final boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", ""));
    private static final String LAUNCHER_PACKAGE_NAME = "com.huawei.android.launcher";
    private static final int MAGIC_WINDOW_SAVITY_LEFT = 80;
    private static final int MARGIN_COLUMN12_IN_DP = 48;
    private static final int MARGIN_COLUMN4_IN_DP = 24;
    private static final int MARGIN_COLUMN8_IN_DP = 32;
    private static final Object M_LOCK = new Object();
    public static final int PC_MODE_FOREGROUND_FREEFORM_STACK_LIMIT = 8;
    public static final String TAG = "HwMultiWindowManager";
    public static final String WIDTH_COLUMNS = "widthColumns";
    private static int sDividerWindowWidth;
    private static float sFreeformCornerRadius;
    private static String sLaunchPkg = "";
    private static int sNavigationBarHeight;
    private static int sNavigationBarWidth;
    private static volatile HwMultiWindowManager sSingleInstance = null;
    private static int sStatusBarHeight;
    private int mCaptionViewHeight;
    private int mDargbarWidth;
    private Rect mDragedFreeFromPos = new Rect();
    private int mGestureNavHotArea;
    private boolean mHasSideinScreen;
    private HwMagicWindowService mHwMagicWindowService;
    private HwMultiWindowTips mHwMultiWindowTip = null;
    private List<HwSplitScreenCombination> mHwSplitScreenCombinations = new ArrayList();
    boolean mIsAddSplitBar = false;
    private boolean mIsStatusBarPermenantlyShowing;
    private int mSafeSideWidth;
    final ActivityTaskManagerService mService;
    HwSurfaceInNotch mSurfaceInNotch = null;

    private HwMultiWindowManager(ActivityTaskManagerService service) {
        this.mService = service;
        if (IS_HW_MULTIWINDOW_SUPPORTED) {
            this.mHwMultiWindowTip = HwMultiWindowTips.getInstance(service.mContext);
        }
    }

    public static HwMultiWindowManager getInstance(ActivityTaskManagerService service) {
        if (sSingleInstance == null) {
            synchronized (M_LOCK) {
                if (sSingleInstance == null) {
                    sSingleInstance = new HwMultiWindowManager(service);
                }
            }
        }
        return sSingleInstance;
    }

    private static void scale(Rect rect, float xscale, float yscale) {
        if (xscale != 1.0f) {
            rect.left = (int) ((((float) rect.left) * xscale) + 0.5f);
            rect.right = (int) ((((float) rect.right) * xscale) + 0.5f);
        }
        if (yscale != 1.0f) {
            rect.top = (int) ((((float) rect.top) * yscale) + 0.5f);
            rect.bottom = (int) ((((float) rect.bottom) * yscale) + 0.5f);
        }
    }

    public static void calcHwSplitStackBounds(ActivityDisplay display, int splitRatio, Rect primaryOutBounds, Rect secondaryOutBounds) {
        if (display != null) {
            Bundle bundle = getSplitGearsByDisplay(display);
            float[] splitRatios = bundle.getFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES);
            int tempSplitRatio = splitRatio;
            if (!(splitRatio == 0 || splitRatios == null || splitRatios.length != 1)) {
                tempSplitRatio = 0;
            }
            float primaryRatio = calcPrimaryRatio(tempSplitRatio);
            if (primaryRatio == 0.0f) {
                if (primaryOutBounds != null) {
                    primaryOutBounds.setEmpty();
                }
                if (secondaryOutBounds != null) {
                    secondaryOutBounds.setEmpty();
                    return;
                }
                return;
            }
            int primaryPos = bundle.getInt(HW_SPLIT_SCREEN_PRIMARY_POSITION);
            if (primaryPos == 1) {
                calcLeftRightSplitStackBounds(display, primaryRatio, primaryOutBounds, secondaryOutBounds);
            } else if (primaryPos == 0) {
                calcTopBottomSplitStackBounds(display, primaryRatio, primaryOutBounds, secondaryOutBounds);
            }
        }
    }

    private static void calcLeftRightSplitStackBounds(ActivityDisplay display, float primaryRatio, Rect primaryOutBounds, Rect secondaryOutBounds) {
        DisplayCutout cutout;
        int displayWidth = display.mDisplayContent.getDisplayInfo().logicalWidth;
        int displayHeight = display.mDisplayContent.getDisplayInfo().logicalHeight;
        int notchSize = 0;
        if (IS_NOTCH_PROP && ((display.mDisplayContent.getDisplayInfo().rotation == 1 || display.mDisplayContent.getDisplayInfo().rotation == 3) && (cutout = display.mDisplayContent.calculateDisplayCutoutForRotation(display.mDisplayContent.getRotation()).getDisplayCutout()) != null)) {
            notchSize = Math.max(cutout.getSafeInsetLeft(), cutout.getSafeInsetRight());
        }
        if (primaryOutBounds != null) {
            primaryOutBounds.set(0, 0, ((int) (((float) (displayWidth - notchSize)) * primaryRatio)) - (sDividerWindowWidth / 2), displayHeight);
        }
        if (secondaryOutBounds != null) {
            secondaryOutBounds.set(((int) (((float) (displayWidth - notchSize)) * primaryRatio)) + (sDividerWindowWidth / 2), 0, displayWidth - notchSize, displayHeight);
        }
        if (notchSize != 0 && display.mDisplayContent.getDisplayInfo().rotation == 1) {
            if (primaryOutBounds != null) {
                primaryOutBounds.offset(notchSize, 0);
            }
            if (secondaryOutBounds != null) {
                secondaryOutBounds.offset(notchSize, 0);
            }
        }
    }

    private static void calcTopBottomSplitStackBounds(ActivityDisplay display, float primaryRatio, Rect primaryOutBounds, Rect secondaryOutBounds) {
        int displayWidth = display.mDisplayContent.getDisplayInfo().logicalWidth;
        int displayHeight = display.mDisplayContent.getDisplayInfo().logicalHeight;
        int notchSize = IS_NOTCH_PROP ? sStatusBarHeight : 0;
        if (primaryOutBounds != null) {
            primaryOutBounds.set(0, notchSize, displayWidth, (((int) (((float) (displayHeight - notchSize)) * primaryRatio)) - (sDividerWindowWidth / 2)) + notchSize);
        }
        if (secondaryOutBounds != null) {
            secondaryOutBounds.set(0, ((int) (((float) (displayHeight - notchSize)) * primaryRatio)) + (sDividerWindowWidth / 2) + notchSize, displayWidth, displayHeight);
        }
    }

    private static int getColumnsByWidth(int widthInDp) {
        if (widthInDp > 0 && widthInDp < 320) {
            return 2;
        }
        if (widthInDp >= 320 && widthInDp < 600) {
            return 4;
        }
        if (widthInDp >= 600 && widthInDp < 840) {
            return 8;
        }
        if (widthInDp >= 840) {
            return 12;
        }
        return 1;
    }

    public static Bundle getSplitGearsByDisplay(ActivityDisplay display) {
        Bundle bundle = new Bundle();
        if (display == null || display.mDisplayContent == null) {
            return bundle;
        }
        float densityWithoutRog = getDensityDpiWithoutRog();
        int widthInDp = (int) (((float) (display.mDisplayContent.getDisplayInfo().logicalWidth * 160)) / densityWithoutRog);
        int heightInDp = (int) (((float) (display.mDisplayContent.getDisplayInfo().logicalHeight * 160)) / densityWithoutRog);
        int widthColumns = getColumnsByWidth(widthInDp);
        int heightColumns = getColumnsByWidth(heightInDp);
        if (widthColumns == 4 && heightColumns > 4) {
            bundle.putInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 0);
            bundle.putFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.33333334f, 0.5f, 0.6666667f});
        } else if (widthColumns > 4 && heightColumns == 4) {
            bundle.putInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 1);
            bundle.putFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES, (!IS_TABLET || widthColumns <= 8) ? new float[]{0.5f} : new float[]{0.33333334f, 0.5f, 0.6666667f});
        } else if (widthColumns == 8 && heightColumns == 8) {
            bundle.putInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 1);
            bundle.putFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.5f});
        } else if (widthColumns == 8 && heightColumns == 12) {
            bundle.putInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 0);
            bundle.putFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.33333334f, 0.5f, 0.6666667f});
        } else if (widthColumns == 12 && heightColumns == 8) {
            bundle.putInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 1);
            bundle.putFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.33333334f, 0.5f, 0.6666667f});
        } else {
            bundle.putInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, widthInDp < heightInDp ? 0 : 1);
            bundle.putFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES, new float[]{0.5f});
        }
        bundle.putInt(WIDTH_COLUMNS, widthColumns);
        bundle.putInt(HEIGHT_COLUMNS, heightColumns);
        return bundle;
    }

    public static void exitHwMultiStack(ActivityStack stack) {
        if (stack != null) {
            if (HwMwUtils.ENABLED) {
                if (HwMwUtils.performPolicy(16, new Object[]{Integer.valueOf(stack.mStackId)}).getBoolean("RESULT_HWMULTISTACK", false)) {
                    return;
                }
            }
            stack.setWindowingMode(1);
        }
    }

    public static void exitHwMultiStack(ActivityStack stack, boolean isAnimate, boolean isShowRecents, boolean isEnteringSplitScreenMode, boolean isDeferEnsuringVisibility, boolean isCreating) {
        if (stack != null) {
            if (HwMwUtils.ENABLED) {
                if (HwMwUtils.performPolicy(16, new Object[]{Integer.valueOf(stack.mStackId)}).getBoolean("RESULT_HWMULTISTACK", false)) {
                    return;
                }
            }
            stack.setWindowingMode(1, isAnimate, isShowRecents, isEnteringSplitScreenMode, isDeferEnsuringVisibility, isCreating);
        }
    }

    private static float calcPrimaryRatio(int splitRatio) {
        if (splitRatio == 0) {
            return 0.5f;
        }
        if (splitRatio == 1) {
            return 0.33333334f;
        }
        if (splitRatio != 2) {
            return (splitRatio == 3 || splitRatio != 4) ? 0.0f : 0.0f;
        }
        return 0.6666667f;
    }

    public void onSystemReady() {
        if (IS_HW_MULTIWINDOW_SUPPORTED || HwMwUtils.ENABLED) {
            loadDimens(0);
            HwMultiWindowSplitUI.getInstance(this.mService.mUiContext, this.mService).onSystemReady(0);
        }
        HwMultiWindowTips hwMultiWindowTips = this.mHwMultiWindowTip;
        if (hwMultiWindowTips != null) {
            hwMultiWindowTips.onSystemReady();
        }
    }

    public boolean hasSplitScreenCombinations() {
        return this.mHwSplitScreenCombinations.size() > 0;
    }

    public void setHwMagicWindowService(HwMagicWindowService hwMagicWindowService) {
        this.mHwMagicWindowService = hwMagicWindowService;
    }

    public HwMagicWindowService getHwMagicWindowService() {
        return this.mHwMagicWindowService;
    }

    public void addStackReferenceIfNeeded(ActivityStack stack) {
        ActivityDisplay activityDisplay;
        if (stack != null && stack.inHwMultiStackWindowingMode() && (activityDisplay = stack.getDisplay()) != null) {
            boolean isLeftRight = false;
            if (stack.inHwFreeFormWindowingMode()) {
                this.mService.mH.post(new Runnable(activityDisplay) {
                    /* class com.android.server.wm.$$Lambda$HwMultiWindowManager$WC4220KtkeJT8rPCfbM17bag0 */
                    private final /* synthetic */ ActivityDisplay f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        HwMultiWindowManager.this.lambda$addStackReferenceIfNeeded$0$HwMultiWindowManager(this.f$1);
                    }
                });
                HwMultiWindowTips hwMultiWindowTips = this.mHwMultiWindowTip;
                if (hwMultiWindowTips != null) {
                    hwMultiWindowTips.processFloatWinDockTip(false, stack.getStackId());
                }
            }
            if (stack.inHwSplitScreenWindowingMode()) {
                for (int i = this.mHwSplitScreenCombinations.size() - 1; i >= 0; i--) {
                    HwSplitScreenCombination screenCombination = this.mHwSplitScreenCombinations.get(i);
                    if (screenCombination.mDisplayId == activityDisplay.mDisplayId) {
                        if (!screenCombination.hasHwSplitScreenStack(stack)) {
                            if (!screenCombination.isSplitScreenCombined()) {
                                Slog.d(TAG, "combine stack: " + stack.toShortString() + ", in combination: " + screenCombination);
                                screenCombination.addStackReferenceIfNeeded(stack);
                                screenCombination.reportPkgNameEvent(this.mService);
                                this.mService.getTaskChangeNotificationController().notifyTaskStackChanged();
                                return;
                            } else if (screenCombination.isSplitScreenCombinedAndVisible()) {
                                Slog.d(TAG, "replace stack: " + stack.toShortString() + ", in combination: " + screenCombination);
                                screenCombination.replaceCombinedSplitScreenStack(stack);
                                screenCombination.reportPkgNameEvent(this.mService);
                                this.mService.getTaskChangeNotificationController().notifyTaskStackChanged();
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                }
                HwSplitScreenCombination newScreenCombination = new HwSplitScreenCombination();
                newScreenCombination.addStackReferenceIfNeeded(stack);
                this.mHwSplitScreenCombinations.add(newScreenCombination);
                Slog.d(TAG, "add combination for stack: " + stack.toShortString() + ", slipt screen combinations size: " + this.mHwSplitScreenCombinations.size());
                notifyNavMgrForMultiWindowChanged(0);
                if (this.mHwMultiWindowTip != null) {
                    if (getSplitGearsByDisplay(stack.getDisplay()).getInt(HW_SPLIT_SCREEN_PRIMARY_POSITION) == 1) {
                        isLeftRight = true;
                    }
                    this.mHwMultiWindowTip.processSplitWinDockTip(isLeftRight);
                }
            }
        }
    }

    public void removeStackReferenceIfNeeded(ActivityStack stack) {
        if (HwMwUtils.ENABLED && stack.inHwMagicWindowingMode()) {
            HwMagicWinCombineManager.getInstance().removeStackReferenceIfNeeded(stack);
        }
        if (this.mHwSplitScreenCombinations.size() > 0 && stack != null) {
            Iterator<HwSplitScreenCombination> iterator = this.mHwSplitScreenCombinations.iterator();
            while (iterator.hasNext()) {
                HwSplitScreenCombination screenCombination = iterator.next();
                if (screenCombination.hasHwSplitScreenStack(stack)) {
                    iterator.remove();
                    Slog.d(TAG, "remove combination for stack: " + stack.toShortString() + ", slipt screen combinations size: " + this.mHwSplitScreenCombinations.size());
                    screenCombination.removeStackReferenceIfNeeded(stack);
                    ActivityStack as = getFilteredTopStack(stack.getDisplay(), Arrays.asList(5, 2, 102));
                    if (as == null || as.getWindowingMode() == 1 || as.getWindowingMode() == 103) {
                        Slog.i(TAG, "getFilteredTopStack, remove divider bar");
                        removeSplitScreenDividerBar(100, false);
                    }
                    notifyNavMgrForMultiWindowChanged(1);
                    this.mService.getTaskChangeNotificationController().notifyTaskStackChanged();
                    return;
                }
            }
        }
    }

    private void notifyNavMgrForMultiWindowChanged(int state) {
        GestureNavPolicy gestureNavPolicy = (GestureNavPolicy) LocalServices.getService(GestureNavPolicy.class);
        if (gestureNavPolicy != null) {
            gestureNavPolicy.onMultiWindowChanged(state);
        }
    }

    public void moveStackToFrontEx(ActivityOptions options, ActivityStack stack, ActivityRecord startActivity) {
        try {
            if (!stack.inHwMultiStackWindowingMode() || (options != null && WindowConfiguration.isHwMultiStackWindowingMode(options.getLaunchWindowingMode()))) {
                if (options == null || !WindowConfiguration.isHwMultiStackWindowingMode(options.getLaunchWindowingMode())) {
                    if (startActivity != null && WindowConfiguration.isIncompatibleWindowingMode(startActivity.getWindowingMode(), stack.getWindowingMode())) {
                        startActivity.onParentChanged();
                    }
                    setAlwaysOnTopOnly(stack.getDisplay(), stack, false, false);
                } else if (stack.mTaskStack == null || !stack.mTaskStack.isVisible()) {
                    if (!stack.inHwMultiStackWindowingMode() || stack.getWindowingMode() != options.getLaunchWindowingMode()) {
                        stack.setWindowingMode(options.getLaunchWindowingMode());
                        Rect bounds = options.getLaunchBounds();
                        if (!stack.inHwSplitScreenWindowingMode() && bounds != null && !bounds.isEmpty()) {
                            stack.resize(bounds, (Rect) null, (Rect) null);
                        }
                    } else {
                        checkHwMultiStackBoundsWhenOptionsMatch(stack);
                    }
                    if (startActivity != null && WindowConfiguration.isIncompatibleWindowingMode(startActivity.getWindowingMode(), stack.getWindowingMode())) {
                        startActivity.onParentChanged();
                    }
                    setAlwaysOnTopOnly(stack.getDisplay(), stack, false, false);
                } else {
                    if (startActivity != null && WindowConfiguration.isIncompatibleWindowingMode(startActivity.getWindowingMode(), stack.getWindowingMode())) {
                        startActivity.onParentChanged();
                    }
                    setAlwaysOnTopOnly(stack.getDisplay(), stack, false, false);
                }
            } else if (stack.mTaskStack == null || !stack.mTaskStack.isVisible()) {
                exitHwMultiStack(stack, false, false, false, true, false);
                if (startActivity != null && WindowConfiguration.isIncompatibleWindowingMode(startActivity.getWindowingMode(), stack.getWindowingMode())) {
                    startActivity.onParentChanged();
                }
                setAlwaysOnTopOnly(stack.getDisplay(), stack, false, false);
            }
        } finally {
            if (startActivity != null && WindowConfiguration.isIncompatibleWindowingMode(startActivity.getWindowingMode(), stack.getWindowingMode())) {
                startActivity.onParentChanged();
            }
            setAlwaysOnTopOnly(stack.getDisplay(), stack, false, false);
        }
    }

    public List<ActivityStack> findCombinedSplitScreenStacks(ActivityStack stack) {
        for (int i = this.mHwSplitScreenCombinations.size() - 1; i >= 0; i--) {
            HwSplitScreenCombination screenCombination = this.mHwSplitScreenCombinations.get(i);
            if (screenCombination.isSplitScreenCombined() && screenCombination.hasHwSplitScreenStack(stack)) {
                return screenCombination.findCombinedSplitScreenStacks(stack);
            }
        }
        return null;
    }

    public ActivityStack getFilteredTopStack(ActivityDisplay activityDisplay, List<Integer> ignoreWindowModes) {
        ActivityStack stack = null;
        synchronized (this.mService.getGlobalLock()) {
            if (activityDisplay == null) {
                Slog.i(TAG, "getFilteredTopStack activityDisplay null, no TopStack");
                return null;
            }
            for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                stack = activityDisplay.getChildAt(stackNdx);
                StringBuilder sb = new StringBuilder();
                sb.append("getFilteredTopStack, stack window mode:");
                sb.append(stack.getWindowingMode());
                sb.append(" , ignore window mode: ");
                sb.append(ignoreWindowModes == null ? null : ignoreWindowModes.toString());
                Slog.i(TAG, sb.toString());
                if (ignoreWindowModes == null || !ignoreWindowModes.contains(Integer.valueOf(stack.getWindowingMode()))) {
                    return stack;
                }
            }
            return stack;
        }
    }

    public void resizeHwSplitStacks(int splitRatio, boolean isEnsureVisible) {
        synchronized (this.mService.getGlobalLock()) {
            if (this.mHwSplitScreenCombinations.size() > 0) {
                for (int i = this.mHwSplitScreenCombinations.size() - 1; i >= 0; i--) {
                    HwSplitScreenCombination screenCombination = this.mHwSplitScreenCombinations.get(i);
                    if (screenCombination.isSplitScreenCombinedAndVisible()) {
                        screenCombination.resizeHwSplitStacks(splitRatio, isEnsureVisible);
                        return;
                    }
                }
            }
        }
    }

    public void resizeMagicWindowBounds(ActivityStack stack, int splitRatio) {
        ActivityDisplay display = stack.getDisplay();
        if (display != null) {
            Rect leftBounds = new Rect();
            Rect rightBounds = new Rect();
            calcHwSplitStackBounds(display, splitRatio, leftBounds, rightBounds);
            HwMwUtils.performPolicy(62, new Object[]{stack.getTopActivity().task, leftBounds, rightBounds, Integer.valueOf(splitRatio)});
        }
    }

    private void calcHwSplitStackForConfigChange(ActivityStack stack, Rect outBounds) {
        float[] splitRatios;
        int splitRatio = 0;
        boolean isCombined = false;
        HwSplitScreenCombination visibleCombination = null;
        int i = this.mHwSplitScreenCombinations.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            HwSplitScreenCombination screenCombination = this.mHwSplitScreenCombinations.get(i);
            if (screenCombination.isSplitScreenCombinedAndVisible()) {
                visibleCombination = screenCombination;
            }
            if (screenCombination.hasStackAndMatchWindowMode(stack)) {
                isCombined = true;
                if (!(screenCombination.mSplitRatio == 0 || (splitRatios = getSplitGearsByDisplay(stack.getDisplay()).getFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES)) == null || splitRatios.length != 1)) {
                    screenCombination.mSplitRatio = 0;
                }
                splitRatio = screenCombination.mSplitRatio;
            } else {
                i--;
            }
        }
        if (!isCombined && visibleCombination != null) {
            splitRatio = visibleCombination.mSplitRatio;
        }
        if (stack.inHwSplitScreenPrimaryWindowingMode()) {
            calcHwSplitStackBounds(stack.getDisplay(), splitRatio, outBounds, null);
        } else if (stack.inHwSplitScreenSecondaryWindowingMode()) {
            calcHwSplitStackBounds(stack.getDisplay(), splitRatio, null, outBounds);
        }
    }

    public void calcHwMultiWindowStackBoundsForConfigChange(ActivityStack stack, Rect outBounds, Rect oldStackBounds, int oldDisplayWidth, int oldDisplayHeight, int newDisplayWidth, int newDisplayHeight, boolean isModeChanged) {
        Slog.d(TAG, "stack: " + stack.toShortString() + " oldStackBounds " + oldStackBounds + ", oldDisplayWidth: " + oldDisplayWidth + ", oldDisplayHeight: " + oldDisplayHeight + ", newDisplayWidth: " + newDisplayWidth + ", newDisplayHeight: " + newDisplayHeight + ", isModeChanged: " + isModeChanged + ", mDragedFreeFromPos: " + this.mDragedFreeFromPos);
        if (stack.inHwSplitScreenWindowingMode()) {
            calcHwSplitStackForConfigChange(stack, outBounds);
        }
        if (stack.inHwFreeFormWindowingMode() && allowedForegroundFreeForms(stack.getDisplay().mDisplayId) == 1) {
            if (this.mDragedFreeFromPos.isEmpty()) {
                calcDefaultFreeFormBounds(outBounds, stack.getDisplay().mDisplayContent);
                return;
            }
            if (stack.matchParentBounds() || isModeChanged) {
                oldStackBounds.set(this.mDragedFreeFromPos);
            }
            calcHwFreeFormBounds(stack, outBounds, oldStackBounds, oldDisplayWidth, oldDisplayHeight, newDisplayWidth, newDisplayHeight);
        }
    }

    private void calcHwMultiWindowStackBoundsDefault(ActivityStack stack, Rect outBounds) {
        if (stack != null) {
            if (stack.inHwSplitScreenPrimaryWindowingMode()) {
                calcHwSplitStackBounds(stack.getDisplay(), 0, outBounds, null);
            } else if (stack.inHwSplitScreenSecondaryWindowingMode()) {
                calcHwSplitStackBounds(stack.getDisplay(), 0, null, outBounds);
            }
        }
    }

    public ActivityStack getSplitScreenPrimaryStack() {
        ActivityStack stackPrimary;
        ActivityDisplay defaultDisplay = this.mService.mRootActivityContainer.getDefaultDisplay();
        if (defaultDisplay != null) {
            stackPrimary = defaultDisplay.getTopStackInWindowingMode(100);
        } else {
            stackPrimary = null;
        }
        return stackPrimary;
    }

    public ActivityStack getSplitScreenTopStack() {
        return getFilteredTopStack(this.mService.mRootActivityContainer.getDefaultDisplay(), Arrays.asList(5, 2, 102));
    }

    public Rect getLeftBoundsForMagicWindow(ActivityStack activityStack) {
        if (activityStack == null || activityStack.getTopActivity() == null) {
            return null;
        }
        TaskRecord taskRecord = activityStack.getTopActivity().task;
        for (int i = 0; i < taskRecord.getChildCount(); i++) {
            if (taskRecord.getChildAt(i) != null && taskRecord.getChildAt(i).getBounds().left < 80 && taskRecord.getChildAt(i).getBounds().right < taskRecord.getBounds().right && taskRecord.getChildAt(i).isInterestingToUserLocked()) {
                return taskRecord.getChildAt(i).getBounds();
            }
        }
        return null;
    }

    public void onConfigurationChanged(int displayId) {
        HwSurfaceInNotch hwSurfaceInNotch;
        ActivityDisplay display;
        if (IS_HW_MULTIWINDOW_SUPPORTED) {
            loadDimens(displayId);
            if (!(!HwFoldScreenManager.isFoldable() || (display = this.mService.mRootActivityContainer.getActivityDisplay(displayId)) == null || display.mDisplayContent == null)) {
                display.mDisplayContent.getDisplayPolicy().updateConfigurationAndScreenSizeDependentBehaviors();
            }
            HwMultiWindowSplitUI.getInstance(((ActivityTaskManagerService) this.mService).mUiContext, this.mService).onConfigurationChanged(displayId);
            if (IS_NOTCH_PROP && (hwSurfaceInNotch = this.mSurfaceInNotch) != null) {
                hwSurfaceInNotch.remove();
            }
        }
    }

    public Bundle getStackPackageNames() {
        HwMultiWindowSplitUI.getInstance(this.mService.mUiContext, this.mService);
        ActivityStack topStack = getSplitScreenTopStack();
        Bundle bundle = new Bundle();
        ArrayList<String> combinedStackPackageNames = new ArrayList<>();
        ArrayList<Integer> combinedStackAppUserIds = new ArrayList<>();
        if (topStack == null || !topStack.inHwMagicWindowingMode()) {
            ActivityStack stackPrimary = getSplitScreenPrimaryStack();
            if (stackPrimary == null) {
                return null;
            }
            List<ActivityStack> combinedStacks = findCombinedSplitScreenStacks(stackPrimary);
            if (stackPrimary.getTopActivity() != null) {
                combinedStackPackageNames.add(stackPrimary.getTopActivity().packageName);
                combinedStackAppUserIds.add(Integer.valueOf(stackPrimary.getTopActivity().mUserId));
            }
            if (combinedStacks != null && !combinedStacks.isEmpty()) {
                for (ActivityStack as : combinedStacks) {
                    if (!(as == null || as.getTopActivity() == null)) {
                        combinedStackPackageNames.add(as.getTopActivity().packageName);
                        combinedStackAppUserIds.add(Integer.valueOf(as.getTopActivity().mUserId));
                    }
                }
            }
            bundle.putStringArrayList("pkgNames", combinedStackPackageNames);
            bundle.putIntegerArrayList("pkgUserIds", combinedStackAppUserIds);
            return bundle;
        }
        if (topStack.getTopActivity() != null) {
            combinedStackPackageNames.add(topStack.getTopActivity().packageName);
            combinedStackAppUserIds.add(Integer.valueOf(topStack.getTopActivity().mUserId));
            bundle.putStringArrayList("pkgNames", combinedStackPackageNames);
            bundle.putIntegerArrayList("pkgUserIds", combinedStackAppUserIds);
        }
        return bundle;
    }

    public int[] getCombinedSplitScreenTaskIds(ActivityStack stack) {
        if (stack == null || this.mHwSplitScreenCombinations.isEmpty()) {
            return null;
        }
        List<Integer> combinedTaskIds = new ArrayList<>();
        Iterator<HwSplitScreenCombination> it = this.mHwSplitScreenCombinations.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            HwSplitScreenCombination screens = it.next();
            if (screens != null && screens.hasHwSplitScreenStack(stack)) {
                List<ActivityStack> combinedStacks = screens.findCombinedSplitScreenStacks(stack);
                if (combinedStacks != null && !combinedStacks.isEmpty()) {
                    for (ActivityStack as : combinedStacks) {
                        if (!(as == null || as.getChildCount() <= 0 || as.getChildAt(0) == null)) {
                            combinedTaskIds.add(Integer.valueOf(as.getChildAt(0).taskId));
                        }
                    }
                }
            }
        }
        if (combinedTaskIds.isEmpty()) {
            return null;
        }
        int[] results = new int[combinedTaskIds.size()];
        for (int i = combinedTaskIds.size() - 1; i >= 0; i--) {
            results[i] = combinedTaskIds.get(i).intValue();
        }
        return results;
    }

    public void removeSplitScreenDividerBar(int windowMode, boolean immediately) {
        HwSurfaceInNotch hwSurfaceInNotch;
        if (IS_HW_MULTIWINDOW_SUPPORTED) {
            HwMultiWindowSplitUI.getInstance(this.mService.mUiContext, this.mService).removeSplit(windowMode, immediately);
            this.mIsAddSplitBar = false;
            if (IS_NOTCH_PROP && (hwSurfaceInNotch = this.mSurfaceInNotch) != null) {
                hwSurfaceInNotch.remove();
            }
            if (WindowConfiguration.isHwSplitScreenWindowingMode(windowMode)) {
                this.mService.onMultiWindowModeChanged(false);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void addSplitScreenDividerBar(int displayId, int windowMode) {
        if (IS_HW_MULTIWINDOW_SUPPORTED) {
            HwMultiWindowSplitUI.getInstance(this.mService.mUiContext, this.mService).addDividerBarWindow(displayId, windowMode);
            this.mIsAddSplitBar = true;
            if (WindowConfiguration.isHwSplitScreenWindowingMode(windowMode)) {
                this.mService.onMultiWindowModeChanged(true);
            }
            if (IS_NOTCH_PROP && this.mSurfaceInNotch == null) {
                this.mSurfaceInNotch = new HwSurfaceInNotch(this.mService.mWindowManager.getDefaultDisplayContentLocked());
            }
        }
    }

    public void addSurfaceInNotchIfNeed() {
        int rotation;
        if (IS_NOTCH_PROP && this.mIsAddSplitBar && (rotation = this.mService.mWindowManager.getDefaultDisplayRotation()) != 0 && rotation != 2 && this.mSurfaceInNotch != null) {
            ActivityStack stack = getSplitScreenPrimaryStack();
            SurfaceControl surfaceControl = stack != null ? stack.mTaskStack.getSurfaceControl() : null;
            if (surfaceControl != null) {
                this.mSurfaceInNotch.show(rotation, surfaceControl);
            }
        }
    }

    public void focusStackChange(int currentUser, int displayId, ActivityStack currentFocusedStack, ActivityStack lastFocusedStack) {
        if (currentFocusedStack == null || !IS_HW_MULTIWINDOW_SUPPORTED) {
            return;
        }
        if (currentFocusedStack.inHwSplitScreenWindowingMode()) {
            addSplitScreenDividerBar(currentFocusedStack.mDisplayId, 100);
        } else if (currentFocusedStack.inHwFreeFormWindowingMode()) {
            currentFocusedStack.setAlwaysOnTopOnly(true);
            lambda$addStackReferenceIfNeeded$0$HwMultiWindowManager(currentFocusedStack.getDisplay());
        } else {
            int currentWindowMode = currentFocusedStack.getWindowingMode();
            if (currentWindowMode != 5 && currentWindowMode != 2) {
                removeSplitScreenDividerBar(100, currentFocusedStack.isHomeOrRecentsStack());
            }
        }
    }

    private void initHwFreeformWindowParam(DisplayContent displayContent) {
        DisplayMetrics displayMetrics = displayContent.getDisplayMetrics();
        this.mCaptionViewHeight = WindowManagerService.dipToPixel(36, displayMetrics);
        this.mDargbarWidth = WindowManagerService.dipToPixel(70, displayMetrics);
        this.mGestureNavHotArea = WindowManagerService.dipToPixel(18, displayMetrics);
    }

    public Rect relocateOffScreenWindow(Rect originalWindowBounds, ActivityStack stack) {
        Rect validInScreenWindowTopLeftLocation;
        ActivityDisplay activityDisplay = stack.getDisplay();
        if (activityDisplay == null) {
            Slog.w(TAG, "relocateOffScreenWindow: Invalid activityDisplay " + activityDisplay);
            return new Rect();
        }
        DisplayContent displayContent = activityDisplay.mDisplayContent;
        if (displayContent == null) {
            Slog.w(TAG, "relocateOffScreenWindow: Invalid displayContent " + displayContent);
            return new Rect();
        }
        initHwFreeformWindowParam(displayContent);
        int captionSpareWidth = (originalWindowBounds.width() - this.mDargbarWidth) / 2;
        Rect stableRect = getStableRect(displayContent);
        if (isPhoneLandscape(displayContent)) {
            validInScreenWindowTopLeftLocation = new Rect(stableRect.left - captionSpareWidth, (this.mHasSideinScreen ? this.mSafeSideWidth : 0) + (this.mIsStatusBarPermenantlyShowing ? sStatusBarHeight : sStatusBarHeight / 2), (stableRect.right - captionSpareWidth) - this.mDargbarWidth, (stableRect.bottom - this.mCaptionViewHeight) - this.mGestureNavHotArea);
        } else {
            validInScreenWindowTopLeftLocation = new Rect(stableRect.left - captionSpareWidth, stableRect.top, (stableRect.right - captionSpareWidth) - this.mDargbarWidth, (stableRect.bottom - this.mCaptionViewHeight) - this.mGestureNavHotArea);
        }
        int validLeft = Math.min(Math.max(originalWindowBounds.left, validInScreenWindowTopLeftLocation.left), validInScreenWindowTopLeftLocation.right);
        int validTop = Math.min(Math.max(originalWindowBounds.top, validInScreenWindowTopLeftLocation.top), validInScreenWindowTopLeftLocation.bottom);
        if (!(originalWindowBounds.left == validLeft && originalWindowBounds.top == validTop)) {
            originalWindowBounds.offsetTo(validLeft, validTop);
        }
        return originalWindowBounds;
    }

    public Point getDragBarCenterPoint(Rect originalWindowBounds, ActivityStack stack) {
        ActivityDisplay activityDisplay = stack.getDisplay();
        if (activityDisplay == null) {
            Slog.w(TAG, "getDragBarCenterPoint: Invalid activityDisplay " + activityDisplay);
            return new Point();
        }
        DisplayContent displayContent = activityDisplay.mDisplayContent;
        if (displayContent == null) {
            Slog.w(TAG, "getDragBarCenterPoint: displayContent " + displayContent);
            return new Point();
        }
        initHwFreeformWindowParam(displayContent);
        return new Point(originalWindowBounds.left + (originalWindowBounds.width() / 2), originalWindowBounds.top + (this.mCaptionViewHeight / 2));
    }

    /* renamed from: limitForegroundFreeformStackCount */
    public void lambda$addStackReferenceIfNeeded$0$HwMultiWindowManager(ActivityDisplay activityDisplay) {
        synchronized (this.mService.getGlobalLock()) {
            if (activityDisplay == null) {
                Slog.i(TAG, "limitForegroundFreeformStackCount activityDisplay null");
                return;
            }
            int stackLimit = allowedForegroundFreeForms(activityDisplay.mDisplayId);
            int visibleStackCnt = 0;
            for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                ActivityStack stack = activityDisplay.getChildAt(stackNdx);
                if (stack.inHwFreeFormWindowingMode() && (stack.isAlwaysOnTop() || stack.shouldBeVisible((ActivityRecord) null))) {
                    visibleStackCnt++;
                }
            }
            int stackNdx2 = 0;
            while (true) {
                if (stackNdx2 > activityDisplay.getChildCount() - 1) {
                    break;
                } else if (visibleStackCnt <= stackLimit) {
                    break;
                } else {
                    ActivityStack stack2 = activityDisplay.getChildAt(stackNdx2);
                    if (stack2.inHwFreeFormWindowingMode() && (stack2.isAlwaysOnTop() || stack2.shouldBeVisible((ActivityRecord) null))) {
                        if (stack2.isAlwaysOnTop()) {
                            stack2.setAlwaysOnTop(false);
                        } else {
                            activityDisplay.positionChildAtBottom(stack2);
                        }
                        visibleStackCnt--;
                    }
                    stackNdx2++;
                }
            }
        }
    }

    private Set<Integer> setFreefromStackInvisible(ActivityDisplay activityDisplay, Set<Integer> stackIdSet) {
        Set<Integer> processedStackIdSet = new HashSet<>();
        for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack = activityDisplay.getChildAt(stackNdx);
            if (stack.getTaskStack().inHwFreeFormWindowingMode() && stack.getTaskStack().isAlwaysOnTop() && stackIdSet.contains(Integer.valueOf(stack.mStackId))) {
                ActivityRecord topActivity = stack.getTopActivity();
                if (topActivity != null) {
                    this.mService.mWindowManager.getWindowManagerServiceEx().takeTaskSnapshot(topActivity.appToken, true);
                }
                stack.setAlwaysOnTop(false);
                processedStackIdSet.add(Integer.valueOf(stack.mStackId));
            }
        }
        ActivityRecord topActivity2 = activityDisplay.topRunningActivity();
        if (!(topActivity2 == null || !topActivity2.isState(ActivityStack.ActivityState.RESUMED) || topActivity2 == this.mService.getLastResumedActivityRecord())) {
            this.mService.setResumedActivityUncheckLocked(topActivity2, "setFreefromStackInvisible");
        }
        return processedStackIdSet;
    }

    private Set<Integer> setFreeformStackVisible(ActivityDisplay activityDisplay, Set<Integer> stackIdSet) {
        Set<Integer> processedStackIdSet = new HashSet<>();
        for (int stackNdx = 0; stackNdx <= activityDisplay.getChildCount() - 1; stackNdx++) {
            ActivityStack stack = activityDisplay.getChildAt(stackNdx);
            if (stack.getTaskStack().inHwFreeFormWindowingMode() && !stack.getTaskStack().isAlwaysOnTop() && stackIdSet.contains(Integer.valueOf(stack.mStackId))) {
                stack.setAlwaysOnTop(true);
                processedStackIdSet.add(Integer.valueOf(stack.mStackId));
            }
        }
        return processedStackIdSet;
    }

    public int[] setFreeformStackVisibility(int displayId, int[] stackIdArray, boolean isVisible) {
        Set<Integer> processedStackIdSet;
        if (!IS_HW_MULTIWINDOW_SUPPORTED) {
            return new int[0];
        }
        synchronized (this.mService.getGlobalLock()) {
            int tempDisplayId = displayId;
            if (displayId == -1) {
                ActivityStack topStack = this.mService.mStackSupervisor.mRootActivityContainer.getTopDisplayFocusedStack();
                tempDisplayId = topStack != null ? topStack.mDisplayId : 0;
            }
            ActivityDisplay activityDisplay = ((ActivityTaskManagerService) this.mService).mStackSupervisor.mRootActivityContainer.getActivityDisplay(tempDisplayId);
            if (activityDisplay == null) {
                Slog.i(TAG, "setFreeformStackVisibility activityDisplay null");
                return new int[0];
            }
            Set<Integer> newStackIdSet = new HashSet<>();
            if (stackIdArray == null || stackIdArray.length == 0) {
                for (int stackNdx = 0; stackNdx <= activityDisplay.getChildCount() - 1; stackNdx++) {
                    newStackIdSet.add(Integer.valueOf(activityDisplay.getChildAt(stackNdx).mStackId));
                }
            } else {
                for (int stackId : stackIdArray) {
                    newStackIdSet.add(Integer.valueOf(stackId));
                }
            }
            if (isVisible) {
                processedStackIdSet = setFreeformStackVisible(activityDisplay, newStackIdSet);
            } else {
                processedStackIdSet = setFreefromStackInvisible(activityDisplay, newStackIdSet);
            }
            int[] processedStackIdArray = new int[processedStackIdSet.size()];
            if (processedStackIdSet.size() != 0) {
                int i = 0;
                for (Integer num : processedStackIdSet) {
                    processedStackIdArray[i] = num.intValue();
                    i++;
                }
            }
            this.mService.mStackSupervisor.mRootActivityContainer.ensureActivitiesVisible((ActivityRecord) null, 0, false);
            this.mService.mStackSupervisor.mRootActivityContainer.resumeFocusedStacksTopActivities();
            return processedStackIdArray;
        }
    }

    public Bundle getSplitGearsByDisplayId(int displayId) {
        return getSplitGearsByDisplay(this.mService.getRootActivityContainer().getActivityDisplay(displayId));
    }

    private void loadDimens(int displayId) {
        sDividerWindowWidth = this.mService.mUiContext.getResources().getDimensionPixelSize(34472536);
        sStatusBarHeight = this.mService.mUiContext.getResources().getDimensionPixelSize(17105443);
        sNavigationBarWidth = this.mService.mUiContext.getResources().getDimensionPixelSize(17105310);
        sNavigationBarHeight = this.mService.mUiContext.getResources().getDimensionPixelSize(17105305);
        sFreeformCornerRadius = (float) this.mService.mUiContext.getResources().getDimensionPixelSize(34472524);
        this.mHasSideinScreen = HwDisplaySizeUtil.hasSideInScreen();
        this.mSafeSideWidth = HwDisplaySizeUtil.getInstance(this.mService.mWindowManager).getSafeSideWidth();
    }

    private static float getDensityDpiWithoutRog() {
        int srcDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
        int dpi = SystemProperties.getInt("persist.sys.dpi", srcDpi);
        int rogDpi = SystemProperties.getInt("persist.sys.realdpi", srcDpi);
        if (dpi <= 0) {
            dpi = srcDpi;
        }
        if (rogDpi <= 0) {
            rogDpi = srcDpi;
        }
        return ((((float) srcDpi) * 1.0f) * ((float) rogDpi)) / ((float) dpi);
    }

    private int dipToPixelWithoutRog(int dip, float densityDpiWithoutRog) {
        return (int) ((((float) dip) * densityDpiWithoutRog) / 160.0f);
    }

    /* access modifiers changed from: package-private */
    public void calcDefaultFreeFormBounds(Rect outBounds, DisplayContent displayContent) {
        if (displayContent != null && outBounds != null) {
            int displayWidth = displayContent.getDisplayInfo().logicalWidth;
            int displayHeight = displayContent.getDisplayInfo().logicalHeight;
            if (allowedForegroundFreeForms(displayContent.mDisplayId) == 1) {
                float densityWithoutRog = getDensityDpiWithoutRog();
                int widthColumns = getColumnsByWidth((int) (((float) (displayWidth * 160)) / densityWithoutRog));
                int heightColumns = getColumnsByWidth((int) (((float) (displayHeight * 160)) / densityWithoutRog));
                int minSide = Math.min(displayWidth, displayHeight);
                int gutterPixel = dipToPixelWithoutRog(24, densityWithoutRog);
                int marginPixel = dipToPixelWithoutRog(24, densityWithoutRog);
                initHwFreeformWindowParam(displayContent);
                Rect stableRect = getStableRect(displayContent);
                if (widthColumns == 4 && heightColumns > 4) {
                    outBounds.left = marginPixel;
                    outBounds.right = displayWidth - marginPixel;
                    int freeFormHeight = this.mCaptionViewHeight + ((int) (((((float) outBounds.width()) * 1.0f) * 16.0f) / 9.0f));
                    outBounds.top = (displayHeight - freeFormHeight) / 2;
                    outBounds.bottom = outBounds.top + freeFormHeight;
                } else if (widthColumns > 4 && heightColumns == 4) {
                    int sideWidth = this.mHasSideinScreen ? this.mSafeSideWidth : 0;
                    outBounds.right = stableRect.right - marginPixel;
                    outBounds.left = outBounds.right - (displayHeight - (marginPixel * 2));
                    int windowHeight = (displayHeight - sStatusBarHeight) - (sideWidth * 2);
                    if (outBounds.width() < windowHeight) {
                        windowHeight = outBounds.width();
                    }
                    int topPosition = (displayHeight - windowHeight) / 2;
                    if (this.mIsStatusBarPermenantlyShowing) {
                        outBounds.top = Math.max(sStatusBarHeight + sideWidth, topPosition);
                    } else {
                        outBounds.top = Math.max((sStatusBarHeight / 2) + sideWidth, topPosition);
                    }
                    outBounds.bottom = outBounds.top + windowHeight;
                } else if (widthColumns == 8 && heightColumns == 8) {
                    int marginPixel2 = dipToPixelWithoutRog(32, densityWithoutRog);
                    int freeFormWidth = ((minSide / 2) - marginPixel2) - (gutterPixel / 2);
                    outBounds.right = stableRect.right - marginPixel2;
                    outBounds.left = outBounds.right - freeFormWidth;
                    int freeFormHeight2 = ((int) (((((float) freeFormWidth) * 1.0f) * 16.0f) / 9.0f)) + this.mCaptionViewHeight;
                    outBounds.top = (displayHeight - freeFormHeight2) / 2;
                    outBounds.bottom = outBounds.top + freeFormHeight2;
                } else if ((widthColumns == 8 && heightColumns == 12) || (widthColumns == 12 && heightColumns == 8)) {
                    int freeFormWidth2 = (minSide - gutterPixel) / 2;
                    outBounds.right = stableRect.right - dipToPixelWithoutRog(32, densityWithoutRog);
                    outBounds.left = outBounds.right - freeFormWidth2;
                    int freeFormHeight3 = ((int) (((((float) freeFormWidth2) * 1.0f) * 16.0f) / 9.0f)) + this.mCaptionViewHeight;
                    outBounds.top = (displayHeight - freeFormHeight3) / 2;
                    outBounds.bottom = outBounds.top + freeFormHeight3;
                }
                if (widthColumns <= 4 || heightColumns != 4) {
                    outBounds.top = Math.max(outBounds.top, sStatusBarHeight);
                    outBounds.bottom = Math.min(outBounds.bottom, displayHeight - sStatusBarHeight);
                }
            }
        }
    }

    private void calcHwFreeFormBounds(ActivityStack stack, Rect outBounds, Rect oldStackBounds, int oldDisplayWidth, int oldDisplayHeight, int newDisplayWidth, int newDisplayHeight) {
        int oldDisplayWidth2 = oldDisplayWidth;
        int oldDisplayHeight2 = oldDisplayHeight;
        int minOldDisplaySide = Math.min(oldDisplayWidth, oldDisplayHeight);
        int maxOldDisplaySide = Math.max(oldDisplayWidth, oldDisplayHeight);
        int minNewDisplaySide = Math.min(newDisplayWidth, newDisplayHeight);
        int maxNewDisplaySide = Math.max(newDisplayWidth, newDisplayHeight);
        Rect defaultBounds = new Rect();
        calcDefaultFreeFormBounds(defaultBounds, stack.getDisplay().mDisplayContent);
        Point defaultdragBarCenter = getDragBarCenterPoint(defaultBounds, stack);
        if (minOldDisplaySide == minNewDisplaySide && maxOldDisplaySide == maxNewDisplaySide) {
            if (!this.mDragedFreeFromPos.isEmpty() && !(this.mDragedFreeFromPos.width() == defaultBounds.width() && this.mDragedFreeFromPos.height() == defaultBounds.height())) {
                oldDisplayWidth2 = newDisplayHeight;
                oldDisplayHeight2 = newDisplayWidth;
            }
            Point dragBarCenter = getDragBarCenterPoint(oldStackBounds, stack);
            defaultBounds.offset(((int) (((((float) dragBarCenter.x) * 1.0f) * ((float) newDisplayWidth)) / ((float) oldDisplayWidth2))) - defaultdragBarCenter.x, ((int) (((((float) dragBarCenter.y) * 1.0f) * ((float) newDisplayHeight)) / ((float) oldDisplayHeight2))) - defaultdragBarCenter.y);
            outBounds.set(relocateOffScreenWindow(defaultBounds, stack));
            this.mDragedFreeFromPos.set(outBounds);
            return;
        }
        if (Math.abs(((((float) maxNewDisplaySide) * 1.0f) / ((float) maxOldDisplaySide)) - ((((float) maxOldDisplaySide) * 1.0f) / ((float) minOldDisplaySide))) < 0.001f) {
            defaultBounds.offset(((int) ((((float) ((oldStackBounds.left + (oldStackBounds.width() / 2)) * newDisplayWidth)) * 1.0f) / ((float) oldDisplayWidth2))) - defaultdragBarCenter.x, ((int) (((((float) oldStackBounds.top) + ((((float) (this.mCaptionViewHeight * minOldDisplaySide)) * 1.0f) / ((float) (minNewDisplaySide * 2)))) * ((float) newDisplayHeight)) / ((float) oldDisplayHeight2))) - defaultdragBarCenter.y);
            outBounds.set(relocateOffScreenWindow(defaultBounds, stack));
            this.mDragedFreeFromPos.set(outBounds);
        } else if ((oldDisplayWidth2 == newDisplayWidth && oldDisplayHeight2 < newDisplayHeight) || (oldDisplayWidth2 < newDisplayWidth && oldDisplayHeight2 == newDisplayHeight)) {
            defaultBounds.offset(((newDisplayWidth - oldDisplayWidth2) + oldStackBounds.right) - defaultBounds.right, oldStackBounds.top - defaultBounds.top);
            outBounds.set(relocateOffScreenWindow(defaultBounds, stack));
            this.mDragedFreeFromPos.set(outBounds);
        } else if ((oldDisplayWidth2 != newDisplayHeight || oldDisplayHeight2 >= newDisplayWidth) && (oldDisplayHeight2 != newDisplayWidth || oldDisplayWidth2 >= newDisplayHeight)) {
            outBounds.set(defaultBounds);
            this.mDragedFreeFromPos.setEmpty();
        } else {
            if (oldDisplayWidth2 < newDisplayHeight) {
                defaultBounds.offset(((newDisplayHeight - oldDisplayWidth2) + oldStackBounds.right) - defaultBounds.right, oldStackBounds.top - defaultBounds.top);
            } else {
                defaultBounds.offset(oldStackBounds.right - defaultBounds.right, oldStackBounds.top - defaultBounds.top);
            }
            Point defaultdragBarCenter2 = getDragBarCenterPoint(defaultBounds, stack);
            defaultBounds.offset(((int) (((((float) defaultdragBarCenter2.x) * 1.0f) * ((float) newDisplayWidth)) / ((float) oldDisplayWidth2))) - defaultdragBarCenter2.x, ((int) (((((float) defaultdragBarCenter2.y) * 1.0f) * ((float) newDisplayHeight)) / ((float) oldDisplayHeight2))) - defaultdragBarCenter2.y);
            outBounds.set(relocateOffScreenWindow(defaultBounds, stack));
            this.mDragedFreeFromPos.set(outBounds);
        }
    }

    private int allowedForegroundFreeForms(int displayId) {
        if (!HwPCUtils.enabledInPad() || !HwPCUtils.isPcCastModeInServer() || !HwPCUtils.isValidExtDisplayId(displayId)) {
            return 1;
        }
        return 8;
    }

    public int getNavBarBoundOnScreen(DisplayContent displayContent, Rect outBound) {
        int naviPos = -1;
        if (!displayContent.getDisplayPolicy().mHwDisplayPolicyEx.isNaviBarMini()) {
            int displayWidth = displayContent.getDisplayInfo().logicalWidth;
            int displayHeight = displayContent.getDisplayInfo().logicalHeight;
            naviPos = displayContent.getDisplayPolicy().navigationBarPosition(displayWidth, displayHeight, displayContent.getRotation());
            if (naviPos == 4) {
                outBound.left = 0;
                outBound.top = displayHeight - sNavigationBarHeight;
                outBound.right = displayWidth;
                outBound.bottom = displayHeight;
            } else if (naviPos == 2) {
                outBound.left = displayWidth - sNavigationBarWidth;
                outBound.top = 0;
                outBound.right = displayWidth;
                outBound.bottom = displayHeight;
            } else if (naviPos == 1) {
                outBound.left = 0;
                outBound.top = 0;
                outBound.right = sNavigationBarWidth;
                outBound.bottom = sNavigationBarHeight;
            } else {
                outBound.left = 0;
                outBound.top = 0;
                outBound.right = 0;
                outBound.bottom = 0;
            }
        }
        return naviPos;
    }

    public int getNotchBoundOnScreen(DisplayContent displayContent, Rect outBound) {
        if (!IS_NOTCH_PROP) {
            return -1;
        }
        if (displayContent == null) {
            Slog.w(TAG, "getNotchBoundOnScreen failed, cause displayContent is null!");
            return -1;
        } else if (outBound == null) {
            Slog.w(TAG, "getNotchBoundOnScreen failed, cause outBound is null!");
            return -1;
        } else {
            int rotation = displayContent.getDisplayInfo().rotation;
            int displayWidth = displayContent.getDisplayInfo().logicalWidth;
            int displayHeight = displayContent.getDisplayInfo().logicalHeight;
            DisplayCutout cutout = displayContent.calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
            if (cutout == null) {
                Slog.w(TAG, "getNotchBoundOnScreen failed, cause cutout is null!");
                return -1;
            } else if (rotation == 0) {
                outBound.set(0, 0, displayWidth, sStatusBarHeight);
                return 0;
            } else if (rotation == 1) {
                outBound.set(0, 0, cutout.getSafeInsetLeft(), displayHeight);
                return 1;
            } else if (rotation == 3) {
                outBound.set(displayWidth - cutout.getSafeInsetRight(), 0, displayWidth, displayHeight);
                return 2;
            } else {
                outBound.setEmpty();
                return -1;
            }
        }
    }

    private Rect getStableRect(DisplayContent displayContent) {
        int displayWidth = displayContent.getDisplayInfo().logicalWidth;
        int displayHeight = displayContent.getDisplayInfo().logicalHeight;
        Rect stableRect = new Rect(0, 0, displayWidth, displayHeight);
        if (displayContent.mDisplayInfo.displayCutout != null) {
            stableRect.top = Math.max(displayContent.mDisplayInfo.displayCutout.getSafeInsetTop(), sStatusBarHeight);
        } else {
            stableRect.top = sStatusBarHeight;
        }
        if (!displayContent.getDisplayPolicy().mHwDisplayPolicyEx.isNaviBarMini()) {
            int naviPos = displayContent.getDisplayPolicy().navigationBarPosition(displayWidth, displayHeight, displayContent.mDisplayInfo.rotation);
            if (naviPos == 4) {
                stableRect.bottom -= sNavigationBarHeight;
            } else if (naviPos != 2) {
                return stableRect;
            } else {
                if (displayContent.mDisplayInfo.displayCutout != null) {
                    stableRect.right = (stableRect.right - sNavigationBarWidth) - displayContent.mDisplayInfo.displayCutout.getSafeInsetRight();
                } else {
                    stableRect.right -= sNavigationBarWidth;
                }
            }
        }
        return stableRect;
    }

    public void updateDragFreeFormPos(Rect bounds, ActivityDisplay activityDisplay) {
        if (activityDisplay != null && bounds != null && allowedForegroundFreeForms(activityDisplay.mDisplayId) == 1) {
            synchronized (this.mService.getGlobalLock()) {
                this.mDragedFreeFromPos.set(bounds);
                for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                    ActivityStack stack = activityDisplay.getChildAt(stackNdx);
                    if (stack.inHwFreeFormWindowingMode()) {
                        if (!bounds.equals(stack.getBounds())) {
                            stack.resize(bounds, (Rect) null, (Rect) null);
                        }
                    }
                }
            }
        }
    }

    public int getPrimaryStackPos(int displayId) {
        ActivityStack stackPrimary;
        Rect leftBounds;
        synchronized (this.mService.getGlobalLock()) {
            HwMultiWindowSplitUI splitUI = HwMultiWindowSplitUI.getInstance(this.mService.getUiContext(), this.mService);
            ActivityStack topStack = getSplitScreenTopStack();
            if (topStack == null || !topStack.inHwMagicWindowingMode() || (leftBounds = getLeftBoundsForMagicWindow(topStack)) == null) {
                ActivityDisplay display = this.mService.getRootActivityContainer().getActivityDisplay(displayId);
                if (display == null || (stackPrimary = display.getTopStackInWindowingMode(100)) == null) {
                    return 0;
                }
                Bundle bundle = getSplitGearsByDisplay(display);
                int splitRatio = 0;
                float[] splitRatios = bundle.getFloatArray(HW_SPLIT_SCREEN_RATIO_VALUES);
                for (HwSplitScreenCombination combination : this.mHwSplitScreenCombinations) {
                    if (combination.hasHwSplitScreenStack(stackPrimary) && splitRatios != null && splitRatios.length > 1) {
                        splitRatio = combination.mSplitRatio;
                    }
                }
                Rect primaryOutBounds = new Rect();
                calcHwSplitStackBounds(display, splitRatio, primaryOutBounds, null);
                splitUI.primaryBounds = primaryOutBounds;
                int primaryPos = bundle.getInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 0);
                if (primaryPos == 0) {
                    return primaryOutBounds.bottom;
                }
                if (primaryPos == 1) {
                    return primaryOutBounds.right;
                }
                return 0;
            }
            splitUI.primaryBounds = leftBounds;
            return leftBounds.right;
        }
    }

    public Bundle getSplitStacksPos(int displayId, int splitRatio) {
        synchronized (this.mService.getGlobalLock()) {
            ActivityDisplay display = this.mService.getRootActivityContainer().getActivityDisplay(displayId);
            if (display == null) {
                return null;
            }
            Rect primaryOutBounds = new Rect();
            Rect secondaryOutBounds = new Rect();
            calcHwSplitStackBounds(display, splitRatio, primaryOutBounds, secondaryOutBounds);
            Bundle bundle = new Bundle();
            bundle.putParcelable(HW_SPLIT_SCREEN_PRIMARY_BOUNDS, primaryOutBounds);
            bundle.putParcelable(HW_SPLIT_SCREEN_SECONDARY_BOUNDS, secondaryOutBounds);
            bundle.putInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, getSplitGearsByDisplay(display).getInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 0));
            return bundle;
        }
    }

    public void setSplitBarVisibility(boolean isVisibility) {
        HwMultiWindowSplitUI.getInstance(this.mService.mUiContext, this.mService).setSplitBarVisibility(isVisibility);
    }

    public Rect[] getRectForScreenShotForDrag(int splitRatio) {
        ActivityStack activityStack = getSplitScreenTopStack();
        if (activityStack == null || activityStack.getDisplay() == null) {
            return null;
        }
        Rect[] dragBounds = {new Rect(), new Rect()};
        calcHwSplitStackBounds(activityStack.getDisplay(), splitRatio, dragBounds[0], dragBounds[1]);
        return dragBounds;
    }

    public int getHwSplitScreenRatio(ActivityStack stack) {
        for (int i = this.mHwSplitScreenCombinations.size() - 1; i >= 0; i--) {
            HwSplitScreenCombination screenCombination = this.mHwSplitScreenCombinations.get(i);
            if (screenCombination.isSplitScreenCombined() && screenCombination.hasHwSplitScreenStack(stack)) {
                return screenCombination.mSplitRatio;
            }
        }
        return 0;
    }

    public boolean isSplitStackVisible(ActivityDisplay display, int primaryPosition) {
        if (!IS_HW_MULTIWINDOW_SUPPORTED) {
            return false;
        }
        synchronized (this.mService.getGlobalLock()) {
            if (this.mHwSplitScreenCombinations.size() > 0) {
                if (display != null) {
                    for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                        ActivityStack stack = display.getChildAt(stackNdx);
                        if (!stack.inHwMultiWindowingMode() && ((stack.isHomeOrRecentsStack() || !stack.isStackTranslucent((ActivityRecord) null)) && !stack.inPinnedWindowingMode())) {
                            return false;
                        }
                        if (stack.inHwSplitScreenWindowingMode() && stack.mTaskStack != null && stack.mTaskStack.isVisible() && (primaryPosition == -1 || primaryPosition == getSplitGearsByDisplay(display).getInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 0))) {
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }
    }

    public void setCallingPackage(String callingPkg) {
        sLaunchPkg = callingPkg;
    }

    public void setAlwaysOnTopOnly(ActivityDisplay display, ActivityStack stack, boolean isNewStack, boolean alwaysOnTop) {
        try {
            if (!"com.huawei.android.launcher".equals(sLaunchPkg)) {
                if (!"com.huawei.hwdockbar".equals(sLaunchPkg)) {
                    if (display != null) {
                        if (!stack.inHwMultiStackWindowingMode()) {
                            ActivityStack topStack = display.getTopStack();
                            if (topStack == null) {
                                sLaunchPkg = "";
                                return;
                            }
                            if (isNewStack) {
                                if (topStack.inHwFreeFormWindowingMode()) {
                                    topStack.setAlwaysOnTopOnly(alwaysOnTop);
                                }
                            } else if (topStack.inHwFreeFormWindowingMode() && stack.getTopActivity() != null && !stack.getTopActivity().visible) {
                                topStack.setAlwaysOnTopOnly(alwaysOnTop);
                            }
                            sLaunchPkg = "";
                            return;
                        }
                    }
                    sLaunchPkg = "";
                }
            }
        } finally {
            sLaunchPkg = "";
        }
    }

    private void checkHwMultiStackBoundsWhenOptionsMatch(ActivityStack stack) {
        ActivityDisplay activityDisplay;
        if (stack.inHwSplitScreenWindowingMode() && (activityDisplay = stack.getDisplay()) != null) {
            HwSplitScreenCombination visibleCombination = null;
            HwSplitScreenCombination stackPositionCombination = null;
            for (int i = this.mHwSplitScreenCombinations.size() - 1; i >= 0; i--) {
                HwSplitScreenCombination screenCombination = this.mHwSplitScreenCombinations.get(i);
                if (screenCombination.mDisplayId == activityDisplay.mDisplayId) {
                    if (screenCombination.isSplitScreenVisible()) {
                        visibleCombination = screenCombination;
                    } else if (screenCombination.hasHwSplitScreenStack(stack)) {
                        stackPositionCombination = screenCombination;
                    }
                }
            }
            if (visibleCombination != null && !visibleCombination.hasHwSplitScreenStack(stack)) {
                if (stackPositionCombination != null) {
                    this.mHwSplitScreenCombinations.remove(stackPositionCombination);
                    List<ActivityStack> combinedStacks = stackPositionCombination.findCombinedSplitScreenStacks(stack);
                    if (!combinedStacks.isEmpty()) {
                        for (ActivityStack combinedStack : combinedStacks) {
                            exitHwMultiStack(combinedStack, false, false, false, true, false);
                        }
                    }
                }
                Rect bounds = new Rect(visibleCombination.getHwSplitScreenStackBounds(stack.getWindowingMode()));
                if (bounds.isEmpty()) {
                    calcHwMultiWindowStackBoundsDefault(stack, bounds);
                }
                visibleCombination.replaceCombinedSplitScreenStack(stack);
                visibleCombination.reportPkgNameEvent(this.mService);
                if (!bounds.isEmpty()) {
                    stack.resize(bounds, (Rect) null, (Rect) null);
                }
                this.mService.getTaskChangeNotificationController().notifyTaskStackChanged();
            }
        }
    }

    public void updateSplitBarPosForIm(int position) {
        HwMultiWindowSplitUI.getInstance(this.mService.getUiContext(), this.mService).updateSplitBarPosForIm(position);
    }

    public Bundle getHwMultiWindowState() {
        Bundle result = new Bundle();
        synchronized (this.mService.getGlobalLock()) {
            DisplayContent displayContent = this.mService.mWindowManager.mRoot.getTopFocusedDisplayContent();
            WindowState focus = displayContent.mCurrentFocus;
            if (focus == null) {
                return result;
            }
            if (focus.inHwFreeFormWindowingMode()) {
                result.putBoolean("float_ime_state", isPhoneLandscape(displayContent));
            } else if (focus.inHwSplitScreenWindowingMode()) {
                boolean isLeftRightSplitWindow = false;
                if (getSplitGearsByDisplay(displayContent.mAcitvityDisplay).getInt(HW_SPLIT_SCREEN_PRIMARY_POSITION, 0) == 1 && isPhoneLandscape(displayContent)) {
                    isLeftRightSplitWindow = true;
                }
                result.putBoolean("is_leftright_split", isLeftRightSplitWindow);
            }
            result.putParcelable("ime_target_rect", focus.getBounds());
            return result;
        }
    }

    public boolean isPhoneLandscape(DisplayContent displayContent) {
        if (displayContent == null) {
            return false;
        }
        int displayWidth = displayContent.getDisplayInfo().logicalWidth;
        int displayHeight = displayContent.getDisplayInfo().logicalHeight;
        float densityWithoutRog = getDensityDpiWithoutRog();
        int widthColumns = getColumnsByWidth((int) (((float) (displayWidth * 160)) / densityWithoutRog));
        int heightColumns = getColumnsByWidth((int) (((float) (displayHeight * 160)) / densityWithoutRog));
        if (widthColumns < 8 || heightColumns != 4) {
            return false;
        }
        return true;
    }

    public void setHwWinCornerRaduis(WindowState win, SurfaceControl control) {
        if (win != null && control != null) {
            Rect rect = new Rect(0, 0, win.mWindowFrames.mDisplayFrame.width(), win.mWindowFrames.mDisplayFrame.height());
            rect.offsetTo(win.mAttrs.surfaceInsets.left, win.mAttrs.surfaceInsets.top);
            control.setWindowCrop(rect);
            control.setCornerRadius(sFreeformCornerRadius);
        }
    }

    public float getHwMultiWinCornerRadius(int windowingMode) {
        if (windowingMode != 102) {
            return 0.0f;
        }
        return sFreeformCornerRadius;
    }

    public boolean isStatusBarPermenantlyShowing() {
        return this.mIsStatusBarPermenantlyShowing;
    }

    public void adjustHwFreeformPosIfNeed(DisplayContent displayContent, boolean isStatusShowing) {
        if (isStatusShowing != this.mIsStatusBarPermenantlyShowing) {
            this.mIsStatusBarPermenantlyShowing = isStatusShowing;
            if (IS_HW_MULTIWINDOW_SUPPORTED && this.mIsStatusBarPermenantlyShowing && isPhoneLandscape(displayContent)) {
                Rect bounds = new Rect();
                int topHeight = (this.mHasSideinScreen ? this.mSafeSideWidth : 0) + sStatusBarHeight;
                Iterator it = displayContent.getStacks().iterator();
                while (it.hasNext()) {
                    TaskStack taskStack = (TaskStack) it.next();
                    if (taskStack.inHwFreeFormWindowingMode()) {
                        taskStack.getBounds(bounds);
                        if (bounds.top < topHeight) {
                            bounds.offset(0, topHeight - bounds.top);
                            taskStack.mActivityStack.resize(bounds, (Rect) null, (Rect) null);
                        }
                    }
                }
            }
        }
    }

    public boolean blockSwipeFromTop(MotionEvent event, DisplayContent display) {
        WindowState statusBar;
        if (!IS_HW_MULTIWINDOW_SUPPORTED || display == null || event == null || !isPhoneLandscape(display) || (statusBar = display.getDisplayPolicy().getStatusBar()) == null || statusBar.isVisible()) {
            return false;
        }
        boolean shouldBlock = false;
        for (int i = display.getStacks().size() - 1; i >= 0; i--) {
            TaskStack taskStack = (TaskStack) display.getStacks().get(i);
            if (!taskStack.inHwFreeFormWindowingMode()) {
                break;
            }
            if (taskStack.isVisible()) {
                Rect bounds = taskStack.getBounds();
                if (bounds.top <= statusBar.getFrameLw().bottom) {
                    int width = bounds.width();
                    shouldBlock |= event.getX() >= ((float) (bounds.left + (width / 3))) && ((float) (bounds.right - (width / 3))) >= event.getX();
                }
            }
        }
        return shouldBlock;
    }
}
