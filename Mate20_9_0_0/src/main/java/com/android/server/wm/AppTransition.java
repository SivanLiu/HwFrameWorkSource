package com.android.server.wm;

import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ResourceId;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.GraphicBuffer;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Flog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.RemoteAnimationAdapter;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ClipRectAnimation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import com.android.internal.R;
import com.android.internal.util.DumpUtils.Dump;
import com.android.server.AttributeCache;
import com.android.server.AttributeCache.Entry;
import com.android.server.HwServiceFactory;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.usb.UsbAudioDevice;
import com.android.server.wm.WindowManagerInternal.AppTransitionListener;
import com.android.server.wm.WindowManagerService.H;
import com.android.server.wm.animation.ClipRectLRAnimation;
import com.android.server.wm.animation.ClipRectTBAnimation;
import com.android.server.wm.animation.CurvedTranslateAnimation;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppTransition implements Dump {
    private static final int APP_STATE_IDLE = 0;
    private static final int APP_STATE_READY = 1;
    private static final int APP_STATE_RUNNING = 2;
    private static final int APP_STATE_TIMEOUT = 3;
    private static final long APP_TRANSITION_GETSPECSFUTURE_TIMEOUT_MS = 5000;
    private static final long APP_TRANSITION_TIMEOUT_MS = 5000;
    private static final int CLIP_REVEAL_TRANSLATION_Y_DP = 8;
    static final int DEFAULT_APP_TRANSITION_DURATION = 250;
    private static final float LAUNCHER_ENTER_ALPHA_TIME_RATIO = 0.2f;
    private static final float LAUNCHER_ENTER_HIDE_TIME_RATIO = 0.3f;
    private static final float LAUNCHER_ENTER_SCALE_TIME_RATIO = 0.7f;
    private static final int MAX_CLIP_REVEAL_TRANSITION_DURATION = 420;
    private static final int NEXT_TRANSIT_TYPE_CLIP_REVEAL = 8;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM = 1;
    private static final int NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE = 7;
    private static final int NEXT_TRANSIT_TYPE_NONE = 0;
    private static final int NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS = 9;
    private static final int NEXT_TRANSIT_TYPE_REMOTE = 10;
    private static final int NEXT_TRANSIT_TYPE_SCALE_UP = 2;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN = 6;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP = 5;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN = 4;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP = 3;
    private static final float RECENTS_THUMBNAIL_FADEIN_FRACTION = 0.5f;
    private static final float RECENTS_THUMBNAIL_FADEOUT_FRACTION = 0.5f;
    private static final String TAG = "WindowManager";
    private static final int THUMBNAIL_APP_TRANSITION_DURATION = 200;
    private static final Interpolator THUMBNAIL_DOCK_INTERPOLATOR = new PathInterpolator(0.85f, 0.0f, 1.0f, 1.0f);
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_DOWN = 2;
    private static final int THUMBNAIL_TRANSITION_ENTER_SCALE_UP = 0;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_DOWN = 3;
    private static final int THUMBNAIL_TRANSITION_EXIT_SCALE_UP = 1;
    static final Interpolator TOUCH_RESPONSE_INTERPOLATOR = new PathInterpolator(LAUNCHER_ENTER_HIDE_TIME_RATIO, 0.0f, 0.1f, 1.0f);
    private float LAZY_MODE_COMP_FACTOR = 0.125f;
    private float LAZY_MODE_WIN_SCALE_FACTOR = 0.75f;
    private TimeInterpolator[] mAlphaInterpolators = new TimeInterpolator[]{this.mConstantInterpolator, new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f), this.mConstantInterpolator};
    private IRemoteCallback mAnimationFinishedCallback;
    private int mAppTransitionState = 0;
    private final Interpolator mClipHorizontalInterpolator = new PathInterpolator(0.0f, 0.0f, 0.4f, 1.0f);
    private final int mClipRevealTranslationY;
    private final int mConfigShortAnimTime;
    private TimeInterpolator mConstantInterpolator = new TimeInterpolator() {
        public float getInterpolation(float input) {
            return 1.0f;
        }
    };
    private final Context mContext;
    private int mCurrentUserId = 0;
    private final Interpolator mDecelerateInterpolator;
    private final ExecutorService mDefaultExecutor = Executors.newSingleThreadExecutor();
    private AppTransitionAnimationSpec mDefaultNextAppTransitionAnimationSpec;
    private final Interpolator mFastOutLinearInInterpolator;
    private final Interpolator mFastOutSlowInInterpolator;
    private final boolean mGridLayoutRecentsEnabled;
    public boolean mIgnoreShowRecentApps = false;
    private int mLastClipRevealMaxTranslation;
    private long mLastClipRevealTransitionDuration = 250;
    private String mLastClosingApp;
    private boolean mLastHadClipReveal;
    private String mLastOpeningApp;
    private int mLastUsedAppTransition = -1;
    private final Interpolator mLinearOutSlowInInterpolator;
    private final ArrayList<AppTransitionListener> mListeners = new ArrayList();
    private final boolean mLowRamRecentsEnabled;
    private int mNextAppTransition = -1;
    private final SparseArray<AppTransitionAnimationSpec> mNextAppTransitionAnimationsSpecs = new SparseArray();
    private IAppTransitionAnimationSpecsFuture mNextAppTransitionAnimationsSpecsFuture;
    private boolean mNextAppTransitionAnimationsSpecsPending;
    private IRemoteCallback mNextAppTransitionCallback;
    private int mNextAppTransitionEnter;
    private int mNextAppTransitionExit;
    private int mNextAppTransitionFlags = 0;
    private IRemoteCallback mNextAppTransitionFutureCallback;
    private int mNextAppTransitionInPlace;
    private Rect mNextAppTransitionInsets = new Rect();
    private String mNextAppTransitionPackage;
    private boolean mNextAppTransitionScaleUp;
    private int mNextAppTransitionType = 0;
    private RemoteAnimationController mRemoteAnimationController;
    private final WindowManagerService mService;
    private TimeInterpolator[] mSizeBigInterpolators = new TimeInterpolator[]{new PathInterpolator(0.44f, 0.43f, LAUNCHER_ENTER_SCALE_TIME_RATIO, 0.75f), new PathInterpolator(0.13f, 0.79f, LAUNCHER_ENTER_HIDE_TIME_RATIO, 1.0f)};
    private TimeInterpolator[] mSizeSmallInterpolators = new TimeInterpolator[]{new PathInterpolator(0.41f, 0.38f, LAUNCHER_ENTER_SCALE_TIME_RATIO, 0.71f), new PathInterpolator(0.16f, 0.64f, 0.33f, 1.0f)};
    private final Interpolator mThumbnailFadeInInterpolator;
    private final Interpolator mThumbnailFadeOutInterpolator;
    private Rect mTmpFromClipRect = new Rect();
    private final Rect mTmpRect = new Rect();
    private Rect mTmpToClipRect = new Rect();

    public AppTransition(Context context, WindowManagerService service) {
        this.mContext = context;
        this.mService = service;
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mConfigShortAnimTime = context.getResources().getInteger(17694720);
        this.mDecelerateInterpolator = AnimationUtils.loadInterpolator(context, 17563651);
        this.mThumbnailFadeInInterpolator = new Interpolator() {
            public float getInterpolation(float input) {
                if (input < 0.5f) {
                    return 0.0f;
                }
                return AppTransition.this.mFastOutLinearInInterpolator.getInterpolation((input - 0.5f) / 0.5f);
            }
        };
        this.mThumbnailFadeOutInterpolator = new Interpolator() {
            public float getInterpolation(float input) {
                if (input >= 0.5f) {
                    return 1.0f;
                }
                return AppTransition.this.mLinearOutSlowInInterpolator.getInterpolation(input / 0.5f);
            }
        };
        this.mClipRevealTranslationY = (int) (8.0f * this.mContext.getResources().getDisplayMetrics().density);
        this.mGridLayoutRecentsEnabled = SystemProperties.getBoolean("ro.recents.grid", false);
        this.mLowRamRecentsEnabled = ActivityManager.isLowRamDeviceStatic();
    }

    boolean isTransitionSet() {
        return this.mNextAppTransition != -1;
    }

    boolean isTransitionEqual(int transit) {
        return this.mNextAppTransition == transit;
    }

    int getAppTransition() {
        return this.mNextAppTransition;
    }

    private void setAppTransition(int transit, int flags) {
        if (transit != this.mNextAppTransition) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set app transition from ");
            stringBuilder.append(appTransitionToString(transit));
            stringBuilder.append(" to ");
            stringBuilder.append(appTransitionToString(this.mNextAppTransition));
            Flog.i(310, stringBuilder.toString());
        }
        this.mNextAppTransition = transit;
        this.mNextAppTransitionFlags |= flags;
        setLastAppTransition(-1, null, null);
        updateBooster();
    }

    void setLastAppTransition(int transit, AppWindowToken openingApp, AppWindowToken closingApp) {
        this.mLastUsedAppTransition = transit;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(openingApp);
        this.mLastOpeningApp = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(closingApp);
        this.mLastClosingApp = stringBuilder.toString();
    }

    boolean isReady() {
        return this.mAppTransitionState == 1 || this.mAppTransitionState == 3;
    }

    void setReady() {
        setAppTransitionState(1);
        fetchAppTransitionSpecsFromFuture();
    }

    boolean isRunning() {
        return this.mAppTransitionState == 2;
    }

    void setIdle() {
        setAppTransitionState(0);
    }

    boolean isTimeout() {
        return this.mAppTransitionState == 3;
    }

    void setTimeout() {
        setAppTransitionState(3);
    }

    GraphicBuffer getAppTransitionThumbnailHeader(int taskId) {
        AppTransitionAnimationSpec spec = (AppTransitionAnimationSpec) this.mNextAppTransitionAnimationsSpecs.get(taskId);
        if (spec == null) {
            spec = this.mDefaultNextAppTransitionAnimationSpec;
        }
        return spec != null ? spec.buffer : null;
    }

    boolean isNextThumbnailTransitionAspectScaled() {
        return this.mNextAppTransitionType == 5 || this.mNextAppTransitionType == 6;
    }

    boolean isNextThumbnailTransitionScaleUp() {
        return this.mNextAppTransitionScaleUp;
    }

    boolean isNextAppTransitionThumbnailUp() {
        return this.mNextAppTransitionType == 3 || this.mNextAppTransitionType == 5;
    }

    boolean isNextAppTransitionThumbnailDown() {
        return this.mNextAppTransitionType == 4 || this.mNextAppTransitionType == 6;
    }

    boolean isNextAppTransitionOpenCrossProfileApps() {
        return this.mNextAppTransitionType == 9;
    }

    boolean isFetchingAppTransitionsSpecs() {
        return this.mNextAppTransitionAnimationsSpecsPending;
    }

    private boolean prepare() {
        if (isRunning()) {
            return false;
        }
        setAppTransitionState(0);
        notifyAppTransitionPendingLocked();
        this.mLastHadClipReveal = false;
        this.mLastClipRevealMaxTranslation = 0;
        this.mLastClipRevealTransitionDuration = 250;
        return true;
    }

    int goodToGo(int transit, AppWindowToken topOpeningApp, AppWindowToken topClosingApp, ArraySet<AppWindowToken> openingApps, ArraySet<AppWindowToken> arraySet) {
        AnimationAdapter animation;
        IBinder iBinder;
        long statusBarTransitionsStartTime;
        AppWindowToken appWindowToken = topOpeningApp;
        AppWindowToken appWindowToken2 = topClosingApp;
        this.mNextAppTransition = -1;
        this.mNextAppTransitionFlags = 0;
        setAppTransitionState(2);
        IBinder iBinder2 = null;
        if (appWindowToken != null) {
            animation = topOpeningApp.getAnimation();
        } else {
            animation = null;
        }
        AnimationAdapter topOpeningAnim = animation;
        if (appWindowToken != null) {
            iBinder = appWindowToken.token;
        } else {
            iBinder = null;
        }
        if (appWindowToken2 != null) {
            iBinder2 = appWindowToken2.token;
        }
        IBinder iBinder3 = iBinder2;
        long durationHint = topOpeningAnim != null ? topOpeningAnim.getDurationHint() : 0;
        if (topOpeningAnim != null) {
            statusBarTransitionsStartTime = topOpeningAnim.getStatusBarTransitionsStartTime();
        } else {
            statusBarTransitionsStartTime = SystemClock.uptimeMillis();
        }
        int redoLayout = notifyAppTransitionStartingLocked(transit, iBinder, iBinder3, durationHint, statusBarTransitionsStartTime, 120);
        this.mService.getDefaultDisplayContentLocked().getDockedDividerController().notifyAppTransitionStarting(openingApps, transit);
        this.mIgnoreShowRecentApps = false;
        if (this.mRemoteAnimationController != null) {
            this.mRemoteAnimationController.goodToGo();
        }
        return redoLayout;
    }

    void clear() {
        this.mNextAppTransitionType = 0;
        this.mNextAppTransitionPackage = null;
        this.mNextAppTransitionAnimationsSpecs.clear();
        this.mRemoteAnimationController = null;
        this.mNextAppTransitionAnimationsSpecsFuture = null;
        this.mDefaultNextAppTransitionAnimationSpec = null;
        this.mAnimationFinishedCallback = null;
    }

    void freeze() {
        if (this.mNextAppTransition == 20 && !this.mService.mOpeningApps.isEmpty()) {
            this.mIgnoreShowRecentApps = true;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("freeze set mIgnoreShowRecentApps ");
            stringBuilder.append(this.mIgnoreShowRecentApps);
            Slog.v(str, stringBuilder.toString());
        }
        int transit = this.mNextAppTransition;
        setAppTransition(-1, 0);
        clear();
        setReady();
        notifyAppTransitionCancelledLocked(transit);
    }

    private void setAppTransitionState(int state) {
        this.mAppTransitionState = state;
        updateBooster();
    }

    void updateBooster() {
        WindowManagerService.sThreadPriorityBooster.setAppTransitionRunning(needsBoosting());
    }

    private boolean needsBoosting() {
        boolean recentsAnimRunning = this.mService.getRecentsAnimationController() != null;
        if (this.mNextAppTransition != -1 || this.mAppTransitionState == 1 || this.mAppTransitionState == 2 || recentsAnimRunning) {
            return true;
        }
        return false;
    }

    void registerListenerLocked(AppTransitionListener listener) {
        this.mListeners.add(listener);
    }

    public void notifyAppTransitionFinishedLocked(IBinder token) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((AppTransitionListener) this.mListeners.get(i)).onAppTransitionFinishedLocked(token);
        }
    }

    private void notifyAppTransitionPendingLocked() {
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((AppTransitionListener) this.mListeners.get(i)).onAppTransitionPendingLocked();
        }
    }

    private void notifyAppTransitionCancelledLocked(int transit) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((AppTransitionListener) this.mListeners.get(i)).onAppTransitionCancelledLocked(transit);
        }
    }

    private int notifyAppTransitionStartingLocked(int transit, IBinder openToken, IBinder closeToken, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
        int redoLayout = 0;
        for (int i = 0; i < this.mListeners.size(); i++) {
            redoLayout |= ((AppTransitionListener) this.mListeners.get(i)).onAppTransitionStartingLocked(transit, openToken, closeToken, duration, statusBarAnimationStartTime, statusBarAnimationDuration);
        }
        return redoLayout;
    }

    private Entry getCachedAnimations(LayoutParams lp) {
        if (lp == null || lp.windowAnimations == 0) {
            return null;
        }
        String packageName = lp.packageName != null ? lp.packageName : PackageManagerService.PLATFORM_PACKAGE_NAME;
        int resId = lp.windowAnimations;
        if ((UsbAudioDevice.kAudioDeviceMetaMask & resId) == DumpState.DUMP_SERVICE_PERMISSIONS) {
            packageName = PackageManagerService.PLATFORM_PACKAGE_NAME;
        }
        return AttributeCache.instance().get(packageName, resId, R.styleable.WindowAnimation, this.mCurrentUserId);
    }

    protected Entry getCachedAnimations(String packageName, int resId) {
        if (packageName == null) {
            return null;
        }
        if ((UsbAudioDevice.kAudioDeviceMetaMask & resId) == DumpState.DUMP_SERVICE_PERMISSIONS) {
            packageName = PackageManagerService.PLATFORM_PACKAGE_NAME;
        }
        return AttributeCache.instance().get(packageName, resId, R.styleable.WindowAnimation, this.mCurrentUserId);
    }

    Animation loadAnimationAttr(LayoutParams lp, int animAttr, int transit) {
        int resId = 0;
        Context context = this.mContext;
        if (animAttr >= 0) {
            Entry ent = getCachedAnimations(lp);
            if (ent != null) {
                context = ent.context;
                resId = ent.array.getResourceId(animAttr, 0);
            }
            Entry ent2 = HwServiceFactory.getHwAppTransition().overrideAnimation(lp, animAttr, this.mContext, ent, this);
            if (ent2 != null) {
                context = ent2.context;
                resId = ent2.array.getResourceId(animAttr, 0);
            }
        }
        resId = updateToTranslucentAnimIfNeeded(resId, transit);
        if (ResourceId.isValid(resId)) {
            return AnimationUtils.loadAnimation(context, resId);
        }
        return null;
    }

    Animation loadAnimationRes(LayoutParams lp, int resId) {
        Context context = this.mContext;
        if (!ResourceId.isValid(resId)) {
            return null;
        }
        Entry ent = getCachedAnimations(lp);
        if (ent != null) {
            context = ent.context;
        }
        return AnimationUtils.loadAnimation(context, resId);
    }

    private Animation loadAnimationRes(String packageName, int resId) {
        if (ResourceId.isValid(resId)) {
            Entry ent = getCachedAnimations(packageName, resId);
            if (ent != null) {
                return AnimationUtils.loadAnimation(ent.context, resId);
            }
        }
        return null;
    }

    private int updateToTranslucentAnimIfNeeded(int anim, int transit) {
        if (transit == 24 && anim == 17432591) {
            return 17432594;
        }
        if (transit == 25 && anim == 17432590) {
            return 17432593;
        }
        return anim;
    }

    private static float computePivot(int startPos, float finalScale) {
        float denom = finalScale - 1.0f;
        if (Math.abs(denom) < 1.0E-4f) {
            return (float) startPos;
        }
        return ((float) (-startPos)) / denom;
    }

    private Animation createScaleUpAnimationLocked(int transit, boolean enter, Rect containingFrame) {
        Animation alpha;
        long duration;
        int i = transit;
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        if (enter) {
            float scaleW = ((float) this.mTmpRect.width()) / ((float) appWidth);
            float scaleH = ((float) this.mTmpRect.height()) / ((float) appHeight);
            Animation scale = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, computePivot(this.mTmpRect.left, scaleW), computePivot(this.mTmpRect.top, scaleH));
            scale.setInterpolator(this.mDecelerateInterpolator);
            alpha = new AlphaAnimation(0.0f, 1.0f);
            alpha.setInterpolator(this.mThumbnailFadeOutInterpolator);
            Animation set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha);
            set.setDetachWallpaper(true);
            alpha = set;
        } else if (i == 14 || i == 15) {
            alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setDetachWallpaper(true);
        } else {
            alpha = new AlphaAnimation(1.0f, 1.0f);
        }
        switch (i) {
            case 6:
            case 7:
                duration = (long) this.mConfigShortAnimTime;
                break;
            default:
                duration = 250;
                break;
        }
        alpha.setDuration(duration);
        alpha.setFillAfter(true);
        alpha.setInterpolator(this.mDecelerateInterpolator);
        alpha.initialize(appWidth, appHeight, appWidth, appHeight);
        return alpha;
    }

    private static float computeFloatPivot(float startPos, float finalScale) {
        float denom = finalScale - 1.0f;
        if (Math.abs(denom) < 1.0E-4f) {
            return startPos;
        }
        return (-startPos) / denom;
    }

    private float[] adjustPivotsInLazyMode(float originPivotX, float originPivotY, int iconWidth, int iconHeight, WindowState win) {
        int lazyMode = this.mService.getLazyMode();
        float[] pivots = new float[]{originPivotX, originPivotY};
        if (lazyMode != 0) {
            Rect bounds = win.getBounds();
            if (bounds == null) {
                Slog.w(TAG, "bounds is null! return.");
                return pivots;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("app exit to launcher, lazymode bounds = ");
            stringBuilder.append(bounds);
            Slog.d(str, stringBuilder.toString());
            float winStartX = 0.0f;
            float winStartY = (1.0f - this.LAZY_MODE_WIN_SCALE_FACTOR) * ((float) bounds.height());
            if (lazyMode == 2) {
                winStartX = (1.0f - this.LAZY_MODE_WIN_SCALE_FACTOR) * ((float) bounds.width());
            }
            float compensationX = this.LAZY_MODE_COMP_FACTOR * ((float) iconWidth);
            float compensationY = this.LAZY_MODE_COMP_FACTOR * ((float) iconHeight);
            if (lazyMode == 1) {
                compensationX = -compensationX;
            }
            pivots[0] = ((this.LAZY_MODE_WIN_SCALE_FACTOR * originPivotX) + winStartX) - compensationX;
            pivots[1] = ((this.LAZY_MODE_WIN_SCALE_FACTOR * originPivotY) + winStartY) - compensationY;
        } else {
            int i = iconWidth;
            int i2 = iconHeight;
        }
        return pivots;
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x01ce, element type: float, insn element type: null */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    Animation createAppExitToIconAnimation(AppWindowToken atoken, int containingHeight, int iconWidth, int iconHeight, float originPivotX, float originPivotY, Bitmap icon) {
        int i = iconWidth;
        int i2 = iconHeight;
        Bitmap bitmap = icon;
        if (atoken == null) {
            Slog.w(TAG, "create app exit animation find no app window token!");
            return null;
        }
        WindowState window = atoken.findMainWindow();
        if (window == null) {
            Slog.w(TAG, "create app exit animation find no app main window!");
            return null;
        }
        float[] pivots = adjustPivotsInLazyMode(originPivotX, originPivotY, i, i2, window);
        float pivotXAdj = pivots[0];
        float pivotYAdj = pivots[1];
        Rect winDecorFrame = window.mDecorFrame;
        if (winDecorFrame == null) {
            Slog.w(TAG, "create app exit animation find no app window frame!");
            return null;
        }
        Rect winDisplayFrame = window.mDisplayFrame;
        if (winDisplayFrame == null) {
            Slog.w(TAG, "create app exit animation find no app window displayFrame!");
            return null;
        }
        int winWidth = winDecorFrame.right - winDecorFrame.left;
        int winHeight = ((containingHeight - winDecorFrame.top) - winDisplayFrame.top) - (containingHeight - winDisplayFrame.bottom);
        float f;
        float f2;
        Rect rect;
        int i3;
        WindowState windowState;
        Rect rect2;
        int pivotXAdj2;
        if (winWidth <= 0) {
            f = pivotXAdj;
            f2 = pivotYAdj;
            rect = winDecorFrame;
            i3 = i2;
            pivots = bitmap;
            windowState = window;
            rect2 = winDisplayFrame;
            winHeight = i;
        } else if (winHeight <= 0) {
            float[] fArr = pivots;
            f = pivotXAdj;
            f2 = pivotYAdj;
            rect = winDecorFrame;
            i3 = i2;
            pivots = bitmap;
            windowState = window;
            rect2 = winDisplayFrame;
            pivotXAdj2 = winHeight;
            winHeight = i;
        } else {
            int finalIconWidth;
            i3 = winWidth < 0 ? -winWidth : winWidth;
            winHeight = winHeight < 0 ? -winHeight : winHeight;
            boolean isHorizontal = i3 > winHeight;
            float middleYRatio = 0.44f;
            float middleXRatio = isHorizontal ? 0.54f : 0.44f;
            if (!isHorizontal) {
                middleYRatio = 0.54f;
            }
            float middleX = 1.0f - ((((float) (i3 - i)) * middleXRatio) / ((float) i3));
            float middleY = 1.0f - ((((float) (winHeight - i2)) * middleYRatio) / ((float) winHeight));
            int finalIconHeight = i2;
            int finalIconWidth2 = i;
            boolean isHorizontal2 = isHorizontal;
            if (this.mService.mExitFlag) {
                winWidth = (int) (((float) i2) * 1053609165);
                finalIconWidth = (int) (((float) i) * 0.4f);
            } else {
                winWidth = finalIconHeight;
                finalIconWidth = finalIconWidth2;
            }
            float toX = ((float) finalIconWidth) / ((float) i3);
            float toY = ((float) winWidth) / ((float) winHeight);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("now set the app exit scale animation for: ");
            stringBuilder.append(window);
            stringBuilder.append(", [winWidth, winHeight] = [");
            stringBuilder.append(i3);
            stringBuilder.append(", ");
            stringBuilder.append(winHeight);
            stringBuilder.append("][originPivotX, originPivotY] = [");
            stringBuilder.append(originPivotX);
            stringBuilder.append(", ");
            stringBuilder.append(originPivotY);
            stringBuilder.append("][fromX, fromY] = [");
            stringBuilder.append(1.0f);
            stringBuilder.append(", ");
            stringBuilder.append(1.0f);
            stringBuilder.append("][toX, toY] = [");
            stringBuilder.append(toX);
            stringBuilder.append(", ");
            stringBuilder.append(toY);
            stringBuilder.append("]");
            Slog.d(str, stringBuilder.toString());
            float iconLeft = pivotXAdj - (((float) finalIconWidth) / 2.0f);
            middleY = pivotYAdj - (((float) winWidth) / 2.0f);
            pivotXAdj = computeFloatPivot(iconLeft, toX) + 0.5f;
            float pivotY = computeFloatPivot(middleY, toY) + 0.5f;
            Rect winFrame = window.mFrame;
            if (winFrame == null) {
                Slog.w(TAG, "create app exit animation find no app window frame!");
                return null;
            }
            TimeInterpolator[] sizeYInterpolators;
            int i4 = finalIconWidth;
            int offsetY = winDecorFrame.top > winFrame.top ? winDecorFrame.top : winFrame.top;
            float pivotY2 = pivotY - (((float) offsetY) * toY);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Retrieved [pivotX, pivotY] = [");
            stringBuilder2.append(pivotXAdj);
            stringBuilder2.append(", ");
            stringBuilder2.append(pivotY2);
            stringBuilder2.append("]");
            Slog.d(str2, stringBuilder2.toString());
            AnimationSet appExitToIconAnimation = new AnimationSet(0.0f);
            float pivotXCompensation = 0.5f;
            AlphaAnimation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
            alphaAnim.setDuration(350);
            alphaAnim.setFillEnabled(true);
            alphaAnim.setFillBefore(true);
            alphaAnim.setFillAfter(true);
            float[] alphaOutValues = new float[]{1.0f, 1.0f, 0.0f, 0.0f};
            int winHeight2 = winHeight;
            WindowState window2 = window;
            PhaseInterpolator alphaInterpolator = new PhaseInterpolator(new float[]{0.0f, 0.16f, 0.32f, 1.0f}, alphaOutValues, this.mAlphaInterpolators);
            alphaAnim.setInterpolator(alphaInterpolator);
            float[] scaleInValues = new float[]{0.0f, 0.16f, 1.0f};
            AnimationSet appExitToIconAnimation2 = new float[]{1065353216, middleX, toX};
            winWidth = new float[]{1065353216, middleY, toY};
            float fromX;
            if (isHorizontal2) {
                fromX = 1.0f;
                pivots = this.mSizeBigInterpolators;
            } else {
                fromX = 1.0f;
                pivots = this.mSizeSmallInterpolators;
            }
            float fromY;
            if (isHorizontal2) {
                fromY = 1.0f;
                sizeYInterpolators = this.mSizeSmallInterpolators;
            } else {
                fromY = 1.0f;
                sizeYInterpolators = this.mSizeBigInterpolators;
            }
            PhaseInterpolator interpolatorX = new PhaseInterpolator(scaleInValues, appExitToIconAnimation2, pivots);
            TimeInterpolator[] sizeXInterpolators = pivots;
            pivots = new PhaseInterpolator(scaleInValues, winWidth, sizeYInterpolators);
            float f3 = pivotXAdj;
            float f4 = pivotY2;
            ScaleAnimation animX = new ScaleAnimation(0.0f, 1.0f, 1.0f, 1.0f, f3, f4);
            animX.setFillEnabled(true);
            animX.setFillBefore(true);
            animX.setFillAfter(true);
            animX.setDuration(350);
            animX.setInterpolator(interpolatorX);
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f, f3, f4);
            scaleAnimation.setFillEnabled(true);
            scaleAnimation.setFillBefore(true);
            scaleAnimation.setFillAfter(true);
            scaleAnimation.setDuration(350);
            scaleAnimation.setInterpolator(pivots);
            appExitToIconAnimation.addAnimation(alphaAnim);
            appExitToIconAnimation.addAnimation(animX);
            appExitToIconAnimation.addAnimation(scaleAnimation);
            appExitToIconAnimation.setZAdjustment(1);
            PhaseInterpolator interpolatorY = pivots;
            pivotXAdj2 = winHeight2;
            pivots = ((float) Math.min(i3, pivotXAdj2)) * LAUNCHER_ENTER_ALPHA_TIME_RATIO;
            float roundy = pivots;
            WindowState window3 = window2;
            window3.mWinAnimator.setWindowClipFlag(1);
            float roundy2 = roundy;
            window3.mWinAnimator.setWindowClipRound(pivots, roundy2);
            float roundx = pivots;
            if (atoken.mShouldDrawIcon == null) {
                window3.mWinAnimator.setWindowClipFlag(2);
            }
            pivots = icon;
            if (pivots != null) {
                windowState = window3;
                float f5 = roundy2;
                window3.mWinAnimator.setWindowClipIcon(iconWidth, iconHeight, pivots);
            } else {
                winHeight = iconWidth;
                i3 = iconHeight;
            }
            return appExitToIconAnimation;
        }
        return null;
    }

    Animation createLauncherEnterAnimation(AppWindowToken atoken, int containingHeight, int iconWidth, int iconHeight, float originPivotX, float originPivotY) {
        int i = iconWidth;
        int i2 = iconHeight;
        if (atoken == null) {
            Slog.w(TAG, "create launcher enter animation find no app window token!");
            return null;
        }
        WindowState window = atoken.findMainWindow();
        if (window == null) {
            Slog.w(TAG, "create launcher enter animation find no app main window!");
            return null;
        }
        Rect winDecorFrame = window.mDecorFrame;
        Rect winVisibleFrame = window.mVisibleFrame;
        Rect rect;
        WindowState windowState;
        if (winDecorFrame == null) {
            rect = winDecorFrame;
        } else if (winVisibleFrame == null) {
            windowState = window;
            rect = winDecorFrame;
        } else {
            int winWidth = winDecorFrame.right - winDecorFrame.left;
            int winHeight = containingHeight - winDecorFrame.top;
            if (winWidth <= 0) {
                rect = winDecorFrame;
            } else if (winHeight <= 0) {
                windowState = window;
                rect = winDecorFrame;
            } else {
                winWidth = winWidth < 0 ? -winWidth : winWidth;
                winHeight = winHeight < 0 ? -winHeight : winHeight;
                float toX = ((float) i) / ((float) winWidth);
                float toY = ((float) i2) / ((float) winHeight);
                float iconTop = originPivotY - ((float) (i2 / 2));
                float pivotX = computePivot((int) (originPivotX - ((float) (i / 2))), toX);
                float pivotY = computePivot((int) iconTop, toY);
                Rect winFrame = window.mFrame;
                if (winFrame == null) {
                    Slog.w(TAG, "create launcher enter animation find no app window frame!");
                    return null;
                }
                int offsetY = winDecorFrame.top > winFrame.top ? winDecorFrame.top : winFrame.top;
                pivotY -= ((float) offsetY) * toY;
                if (originPivotX < 0.0f || originPivotY < 0.0f) {
                    window = ((float) winWidth) / 1073741824;
                    pivotY = ((float) winHeight) / 2.0f;
                } else {
                    Rect rect2 = winFrame;
                    window = pivotX;
                }
                long duration = 350;
                if (this.mService.mExitIconWidth < 0 || this.mService.mExitIconHeight < 0 || this.mService.mExitIconBitmap == null) {
                    duration = 200;
                }
                long duration2 = duration;
                AnimationSet launcherEnterAnimation = new AnimationSet(false);
                Animation scaleAnimation = new ScaleAnimation(0.93f, 1.0f, 0.93f, 1.0f, window, pivotY);
                scaleAnimation.setFillEnabled(true);
                scaleAnimation.setFillBefore(true);
                scaleAnimation.setFillAfter(true);
                float pivotX2 = window;
                scaleAnimation.setStartOffset((long) (((float) duration2) * LAUNCHER_ENTER_HIDE_TIME_RATIO));
                scaleAnimation.setDuration((long) (((float) duration2) * LAUNCHER_ENTER_SCALE_TIME_RATIO));
                scaleAnimation.setInterpolator(new PathInterpolator(LAUNCHER_ENTER_ALPHA_TIME_RATIO, 0.0f, 0.1f, 1.0f));
                launcherEnterAnimation.addAnimation(scaleAnimation);
                AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
                alphaAnimation.setFillEnabled(true);
                alphaAnimation.setFillBefore(true);
                alphaAnimation.setFillAfter(true);
                alphaAnimation.setDuration((long) (((float) duration2) * 1045220557));
                alphaAnimation.setStartOffset((long) (((float) duration2) * LAUNCHER_ENTER_HIDE_TIME_RATIO));
                alphaAnimation.setInterpolator(new LinearInterpolator());
                launcherEnterAnimation.addAnimation(alphaAnimation);
                launcherEnterAnimation.setDetachWallpaper(true);
                launcherEnterAnimation.setZAdjustment(null);
                return launcherEnterAnimation;
            }
            return null;
        }
        Slog.w(TAG, "create launcher enter animation find no app window frame!");
        return null;
    }

    private void getDefaultNextAppTransitionStartRect(Rect rect) {
        if (this.mDefaultNextAppTransitionAnimationSpec == null || this.mDefaultNextAppTransitionAnimationSpec.rect == null) {
            Slog.e(TAG, "Starting rect for app requested, but none available", new Throwable());
            rect.setEmpty();
            return;
        }
        rect.set(this.mDefaultNextAppTransitionAnimationSpec.rect);
    }

    void getNextAppTransitionStartRect(int taskId, Rect rect) {
        AppTransitionAnimationSpec spec = (AppTransitionAnimationSpec) this.mNextAppTransitionAnimationsSpecs.get(taskId);
        if (spec == null) {
            spec = this.mDefaultNextAppTransitionAnimationSpec;
        }
        if (spec == null || spec.rect == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting rect for task: ");
            stringBuilder.append(taskId);
            stringBuilder.append(" requested, but not available");
            Slog.e(str, stringBuilder.toString(), new Throwable());
            rect.setEmpty();
            return;
        }
        rect.set(spec.rect);
    }

    private void putDefaultNextAppTransitionCoordinates(int left, int top, int width, int height, GraphicBuffer buffer) {
        this.mDefaultNextAppTransitionAnimationSpec = new AppTransitionAnimationSpec(-1, buffer, new Rect(left, top, left + width, top + height));
    }

    long getLastClipRevealTransitionDuration() {
        return this.mLastClipRevealTransitionDuration;
    }

    int getLastClipRevealMaxTranslation() {
        return this.mLastClipRevealMaxTranslation;
    }

    boolean hadClipRevealAnimation() {
        return this.mLastHadClipReveal;
    }

    private long calculateClipRevealTransitionDuration(boolean cutOff, float translationX, float translationY, Rect displayFrame) {
        if (cutOff) {
            return (long) (250.0f + (170.0f * Math.max(Math.abs(translationX) / ((float) displayFrame.width()), Math.abs(translationY) / ((float) displayFrame.height()))));
        }
        return 250;
    }

    private Animation createClipRevealAnimationLocked(int transit, boolean enter, Rect appFrame, Rect displayFrame) {
        int i = transit;
        Rect rect = appFrame;
        if (enter) {
            int translationYCorrection;
            int translationY;
            int clipStartY;
            Interpolator interpolator;
            int max;
            int appWidth = appFrame.width();
            int appHeight = appFrame.height();
            getDefaultNextAppTransitionStartRect(this.mTmpRect);
            float t = 0.0f;
            if (appHeight > 0) {
                t = ((float) this.mTmpRect.top) / ((float) displayFrame.height());
            }
            int translationY2 = this.mClipRevealTranslationY + ((int) ((((float) displayFrame.height()) / 7.0f) * t));
            int translationX = 0;
            int translationYCorrection2 = translationY2;
            int centerX = this.mTmpRect.centerX();
            int centerY = this.mTmpRect.centerY();
            int halfWidth = this.mTmpRect.width() / 2;
            int halfHeight = this.mTmpRect.height() / 2;
            int clipStartX = (centerX - halfWidth) - rect.left;
            int clipStartY2 = (centerY - halfHeight) - rect.top;
            boolean cutOff = false;
            if (rect.top > centerY - halfHeight) {
                cutOff = true;
                translationYCorrection = 0;
                translationY = (centerY - halfHeight) - rect.top;
                clipStartY = 0;
            } else {
                translationY = translationY2;
                translationYCorrection = translationYCorrection2;
                clipStartY = clipStartY2;
            }
            if (rect.left > centerX - halfWidth) {
                translationX = (centerX - halfWidth) - rect.left;
                clipStartX = 0;
                cutOff = true;
            }
            if (rect.right < centerX + halfWidth) {
                translationX = (centerX + halfWidth) - rect.right;
                clipStartX = appWidth - this.mTmpRect.width();
                cutOff = true;
            }
            clipStartY2 = clipStartX;
            boolean cutOff2 = cutOff;
            int translationX2 = translationX;
            long duration = calculateClipRevealTransitionDuration(cutOff2, (float) translationX2, (float) translationY, displayFrame);
            Animation clipAnimLR = new ClipRectLRAnimation(clipStartY2, this.mTmpRect.width() + clipStartY2, 0, appWidth);
            clipAnimLR.setInterpolator(this.mClipHorizontalInterpolator);
            int clipStartX2 = clipStartY2;
            clipAnimLR.setDuration((long) (((float) duration) / 2.5f));
            TranslateAnimation translate = new TranslateAnimation((float) translationX2, 0.0f, (float) translationY, 0.0f);
            if (cutOff2) {
                interpolator = TOUCH_RESPONSE_INTERPOLATOR;
            } else {
                interpolator = this.mLinearOutSlowInInterpolator;
            }
            translate.setInterpolator(interpolator);
            translate.setDuration(duration);
            long duration2 = duration;
            int translationX3 = translationX2;
            boolean cutOff3 = cutOff2;
            int appHeight2 = appHeight;
            Animation clipAnimTB = new ClipRectTBAnimation(clipStartY, clipStartY + this.mTmpRect.height(), 0, appHeight, translationYCorrection, 0, this.mLinearOutSlowInInterpolator);
            clipAnimTB.setInterpolator(TOUCH_RESPONSE_INTERPOLATOR);
            duration = duration2;
            clipAnimTB.setDuration(duration);
            long alphaDuration = duration / 4;
            AlphaAnimation alpha = new AlphaAnimation(0.5f, 1.0f);
            alpha.setDuration(alphaDuration);
            alpha.setInterpolator(this.mLinearOutSlowInInterpolator);
            Animation set = new AnimationSet(false);
            set.addAnimation(clipAnimLR);
            set.addAnimation(clipAnimTB);
            set.addAnimation(translate);
            set.addAnimation(alpha);
            set.setZAdjustment(1);
            set.initialize(appWidth, appHeight2, appWidth, appHeight2);
            Animation anim = set;
            this.mLastHadClipReveal = true;
            this.mLastClipRevealTransitionDuration = duration;
            if (cutOff3) {
                max = Math.max(Math.abs(translationY), Math.abs(translationX3));
            } else {
                TranslateAnimation translateAnimation = translate;
                max = 0;
            }
            this.mLastClipRevealMaxTranslation = max;
            return anim;
        }
        long duration3;
        Animation anim2;
        boolean z;
        switch (i) {
            case 6:
            case 7:
                duration3 = (long) this.mConfigShortAnimTime;
                break;
            default:
                duration3 = 250;
                break;
        }
        if (i == 14 || i == 15) {
            anim2 = new AlphaAnimation(1.0f, 0.0f);
            z = true;
            anim2.setDetachWallpaper(true);
        } else {
            anim2 = new AlphaAnimation(1.0f, 1.0f);
            z = true;
        }
        anim2.setInterpolator(this.mDecelerateInterpolator);
        anim2.setDuration(duration3);
        anim2.setFillAfter(z);
        return anim2;
    }

    Animation prepareThumbnailAnimationWithDuration(Animation a, int appWidth, int appHeight, long duration, Interpolator interpolator) {
        if (duration > 0) {
            a.setDuration(duration);
        }
        a.setFillAfter(true);
        if (interpolator != null) {
            a.setInterpolator(interpolator);
        }
        a.initialize(appWidth, appHeight, appWidth, appHeight);
        return a;
    }

    Animation prepareThumbnailAnimation(Animation a, int appWidth, int appHeight, int transit) {
        int duration;
        switch (transit) {
            case 6:
            case 7:
                duration = this.mConfigShortAnimTime;
                break;
            default:
                duration = DEFAULT_APP_TRANSITION_DURATION;
                break;
        }
        return prepareThumbnailAnimationWithDuration(a, appWidth, appHeight, (long) duration, this.mDecelerateInterpolator);
    }

    int getThumbnailTransitionState(boolean enter) {
        if (enter) {
            if (this.mNextAppTransitionScaleUp) {
                return 0;
            }
            return 2;
        } else if (this.mNextAppTransitionScaleUp) {
            return 1;
        } else {
            return 3;
        }
    }

    GraphicBuffer createCrossProfileAppsThumbnail(int thumbnailDrawableRes, Rect frame) {
        int width = frame.width();
        int height = frame.height();
        Picture picture = new Picture();
        Canvas canvas = picture.beginRecording(width, height);
        canvas.drawColor(Color.argb(0.6f, 0.0f, 0.0f, 0.0f));
        int thumbnailSize = this.mService.mContext.getResources().getDimensionPixelSize(17104986);
        Drawable drawable = this.mService.mContext.getDrawable(thumbnailDrawableRes);
        drawable.setBounds((width - thumbnailSize) / 2, (height - thumbnailSize) / 2, (width + thumbnailSize) / 2, (height + thumbnailSize) / 2);
        drawable.setTint(this.mContext.getColor(17170443));
        drawable.draw(canvas);
        picture.endRecording();
        return Bitmap.createBitmap(picture).createGraphicBufferHandle();
    }

    Animation createCrossProfileAppsThumbnailAnimationLocked(Rect appRect) {
        return prepareThumbnailAnimationWithDuration(loadAnimationRes(PackageManagerService.PLATFORM_PACKAGE_NAME, 17432611), appRect.width(), appRect.height(), 0, null);
    }

    Animation createThumbnailAspectScaleAnimationLocked(Rect appRect, Rect contentInsets, GraphicBuffer thumbnailHeader, int taskId, int uiMode, int orientation) {
        float f;
        float fromX;
        float toX;
        float toY;
        float pivotX;
        float fromY;
        float pivotY;
        int appWidth;
        Animation a;
        Rect rect = appRect;
        Rect rect2 = contentInsets;
        int thumbWidthI = thumbnailHeader.getWidth();
        float thumbWidth = thumbWidthI > 0 ? (float) thumbWidthI : 1.0f;
        int thumbHeightI = thumbnailHeader.getHeight();
        int appWidth2 = appRect.width();
        float scaleW = ((float) appWidth2) / thumbWidth;
        getNextAppTransitionStartRect(taskId, this.mTmpRect);
        if (shouldScaleDownThumbnailTransition(uiMode, orientation)) {
            f = (float) this.mTmpRect.left;
            fromX = (float) this.mTmpRect.top;
            toX = (((float) (this.mTmpRect.width() / 2)) * (scaleW - 1.0f)) + ((float) rect.left);
            toY = (((float) (appRect.height() / 2)) * (1.0f - (1.0f / scaleW))) + ((float) rect.top);
            float width = (float) (this.mTmpRect.width() / 2);
            pivotX = ((float) (appRect.height() / 2)) / scaleW;
            if (this.mGridLayoutRecentsEnabled != null) {
                fromX -= (float) thumbHeightI;
                toY -= ((float) thumbHeightI) * scaleW;
            }
            fromY = fromX;
            pivotY = pivotX;
            fromX = f;
            pivotX = width;
        } else {
            fromX = (float) this.mTmpRect.left;
            pivotX = 0.0f;
            pivotY = 0.0f;
            fromY = (float) this.mTmpRect.top;
            toX = (float) rect.left;
            toY = (float) rect.top;
        }
        f = toY;
        long duration = getAspectScaleDuration();
        Interpolator interpolator = getAspectScaleInterpolator();
        long duration2 = duration;
        float f2;
        Animation scaleAnimation;
        Animation translate;
        float fromY2;
        int i;
        int i2;
        if (this.mNextAppTransitionScaleUp) {
            long j;
            Object obj = null;
            long duration3 = duration2;
            f2 = 0.0f;
            scaleAnimation = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleW, pivotX, pivotY);
            scaleAnimation.setInterpolator(interpolator);
            duration = duration3;
            scaleAnimation.setDuration(duration);
            appWidth = appWidth2;
            Animation alpha = new AlphaAnimation(1065353216, f2);
            alpha.setInterpolator(this.mNextAppTransition == 19 ? THUMBNAIL_DOCK_INTERPOLATOR : this.mThumbnailFadeOutInterpolator);
            if (this.mNextAppTransition == 19) {
                j = duration / 2;
            } else {
                j = duration;
            }
            alpha.setDuration(j);
            translate = createCurvedMotion(fromX, toX, fromY, f);
            translate.setInterpolator(interpolator);
            translate.setDuration(duration);
            this.mTmpFromClipRect.set(0, 0, thumbWidthI, thumbHeightI);
            this.mTmpToClipRect.set(appRect);
            this.mTmpToClipRect.offsetTo(0, 0);
            this.mTmpToClipRect.right = (int) (((float) this.mTmpToClipRect.right) / scaleW);
            this.mTmpToClipRect.bottom = (int) (((float) this.mTmpToClipRect.bottom) / scaleW);
            rect2 = contentInsets;
            if (rect2 != null) {
                fromY2 = fromY;
                this.mTmpToClipRect.inset((int) (((float) (-rect2.left)) * scaleW), (int) (((float) (-rect2.top)) * scaleW), (int) (((float) (-rect2.right)) * scaleW), (int) (((float) (-rect2.bottom)) * scaleW));
            } else {
                fromY2 = fromY;
                i = thumbWidthI;
                i2 = thumbHeightI;
            }
            fromY = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
            fromY.setInterpolator(interpolator);
            fromY.setDuration(duration);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(scaleAnimation);
            if (!this.mGridLayoutRecentsEnabled) {
                set.addAnimation(alpha);
            }
            set.addAnimation(translate);
            set.addAnimation(fromY);
            a = set;
            thumbWidthI = duration;
            f2 = fromY2;
        } else {
            fromY2 = fromY;
            i = thumbWidthI;
            float f3 = thumbWidth;
            i2 = thumbHeightI;
            appWidth = appWidth2;
            f2 = 0.0f;
            long duration4 = duration2;
            scaleAnimation = new ScaleAnimation(scaleW, 1.0f, scaleW, 1.0f, pivotX, pivotY);
            scaleAnimation.setInterpolator(interpolator);
            scaleAnimation.setDuration(duration4);
            Animation alpha2 = new AlphaAnimation(f2, 1.0f);
            alpha2.setInterpolator(this.mThumbnailFadeInInterpolator);
            alpha2.setDuration(duration4);
            f2 = fromY2;
            translate = createCurvedMotion(toX, fromX, f, f2);
            translate.setInterpolator(interpolator);
            translate.setDuration(duration4);
            a = new AnimationSet(false);
            a.addAnimation(scaleAnimation);
            if (!this.mGridLayoutRecentsEnabled) {
                a.addAnimation(alpha2);
            }
            a.addAnimation(translate);
        }
        return prepareThumbnailAnimationWithDuration(a, appWidth, appRect.height(), 0, null);
    }

    private Animation createCurvedMotion(float fromX, float toX, float fromY, float toY) {
        if (Math.abs(toX - fromX) < 1.0f || this.mNextAppTransition != 19) {
            return new TranslateAnimation(fromX, toX, fromY, toY);
        }
        return new CurvedTranslateAnimation(createCurvedPath(fromX, toX, fromY, toY));
    }

    private Path createCurvedPath(float fromX, float toX, float fromY, float toY) {
        Path path = new Path();
        path.moveTo(fromX, fromY);
        if (fromY > toY) {
            path.cubicTo(fromX, fromY, toX, (0.9f * fromY) + (0.1f * toY), toX, toY);
        } else {
            path.cubicTo(fromX, fromY, fromX, (0.1f * fromY) + (0.9f * toY), toX, toY);
        }
        return path;
    }

    private long getAspectScaleDuration() {
        if (this.mNextAppTransition == 19) {
            return 270;
        }
        return 200;
    }

    private Interpolator getAspectScaleInterpolator() {
        if (this.mNextAppTransition == 19) {
            return this.mFastOutSlowInInterpolator;
        }
        return TOUCH_RESPONSE_INTERPOLATOR;
    }

    Animation createAspectScaledThumbnailEnterExitAnimationLocked(int thumbTransitState, int uiMode, int orientation, int transit, Rect containingFrame, Rect contentInsets, Rect surfaceInsets, Rect stableInsets, boolean freeform, int taskId) {
        Animation clipAnim;
        int i = transit;
        Rect rect = containingFrame;
        Rect rect2 = contentInsets;
        Rect rect3 = surfaceInsets;
        Rect rect4 = stableInsets;
        int i2 = taskId;
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = this.mTmpRect.width();
        float thumbWidth = thumbWidthI > 0 ? (float) thumbWidthI : 1.0f;
        int thumbHeightI = this.mTmpRect.height();
        float thumbHeight = thumbHeightI > 0 ? (float) thumbHeightI : 1.0f;
        int thumbStartX = (this.mTmpRect.left - rect.left) - rect2.left;
        int thumbStartY = this.mTmpRect.top - rect.top;
        Animation set;
        switch (thumbTransitState) {
            case 0:
            case 3:
                boolean scaleUp = thumbTransitState == 0;
                if (!freeform || !scaleUp) {
                    if (!freeform) {
                        set = new AnimationSet(true);
                        this.mTmpFromClipRect.set(rect);
                        this.mTmpToClipRect.set(rect);
                        this.mTmpFromClipRect.offsetTo(0, 0);
                        this.mTmpToClipRect.offsetTo(0, 0);
                        this.mTmpFromClipRect.inset(rect2);
                        this.mNextAppTransitionInsets.set(rect2);
                        if (shouldScaleDownThumbnailTransition(uiMode, orientation)) {
                            Animation clipRectAnimation;
                            Animation translateAnim;
                            float scale = thumbWidth / ((float) ((appWidth - rect2.left) - rect2.right));
                            if (!this.mGridLayoutRecentsEnabled) {
                                this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + ((int) (thumbHeight / scale));
                            }
                            this.mNextAppTransitionInsets.set(rect2);
                            Animation scaleAnimation = new ScaleAnimation(scaleUp ? scale : 1.0f, scaleUp ? 1.0f : scale, scaleUp ? scale : 1.0f, scaleUp ? 1.0f : scale, ((float) containingFrame.width()) / 2.0f, (((float) containingFrame.height()) / 2.0f) + ((float) rect2.top));
                            float targetX = (float) (this.mTmpRect.left - rect.left);
                            float x = (((float) containingFrame.width()) / 2.0f) - ((((float) containingFrame.width()) / 2.0f) * scale);
                            float targetY = (float) (this.mTmpRect.top - rect.top);
                            float y = (((float) containingFrame.height()) / 2.0f) - ((((float) containingFrame.height()) / 2.0f) * scale);
                            if (this.mLowRamRecentsEnabled && rect2.top == 0 && scaleUp) {
                                Rect rect5 = this.mTmpFromClipRect;
                                rect5.top += rect4.top;
                                y += (float) rect4.top;
                            }
                            scale = targetX - x;
                            float startY = targetY - y;
                            if (scaleUp) {
                                clipRectAnimation = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                            } else {
                                float f = x;
                                clipRectAnimation = new ClipRectAnimation(this.mTmpToClipRect, this.mTmpFromClipRect);
                            }
                            Animation clipAnim2 = clipRectAnimation;
                            if (scaleUp) {
                                translateAnim = createCurvedMotion(scale, 0.0f, startY - ((float) rect2.top), 0.0f);
                            } else {
                                translateAnim = createCurvedMotion(0.0f, scale, 0.0f, startY - ((float) rect2.top));
                            }
                            set.addAnimation(clipAnim2);
                            set.addAnimation(scaleAnimation);
                            set.addAnimation(translateAnim);
                        } else {
                            Animation translateAnim2;
                            this.mTmpFromClipRect.bottom = this.mTmpFromClipRect.top + thumbHeightI;
                            this.mTmpFromClipRect.right = this.mTmpFromClipRect.left + thumbWidthI;
                            if (scaleUp) {
                                clipAnim = new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect);
                            } else {
                                clipAnim = new ClipRectAnimation(this.mTmpToClipRect, this.mTmpFromClipRect);
                            }
                            if (scaleUp) {
                                translateAnim2 = createCurvedMotion((float) thumbStartX, 0.0f, (float) (thumbStartY - rect2.top), 0.0f);
                            } else {
                                translateAnim2 = createCurvedMotion(0.0f, (float) thumbStartX, 0.0f, (float) (thumbStartY - rect2.top));
                            }
                            set.addAnimation(clipAnim);
                            set.addAnimation(translateAnim2);
                        }
                        clipAnim = set;
                        clipAnim.setZAdjustment(1);
                        break;
                    }
                    set = createAspectScaledThumbnailExitFreeformAnimationLocked(rect, rect3, i2);
                } else {
                    set = createAspectScaledThumbnailEnterFreeformAnimationLocked(rect, rect3, i2);
                }
                clipAnim = set;
                break;
            case 1:
                if (i != 14) {
                    clipAnim = new AlphaAnimation(1.0f, 1.0f);
                    set = clipAnim;
                    break;
                }
                clipAnim = new AlphaAnimation(1.0f, 0.0f);
                set = clipAnim;
                break;
            case 2:
                if (i != 14) {
                    clipAnim = new AlphaAnimation(1.0f, 1.0f);
                    break;
                }
                clipAnim = new AlphaAnimation(0.0f, 1.0f);
                set = clipAnim;
                break;
            default:
                throw new RuntimeException("Invalid thumbnail transition state");
        }
        int i3 = thumbStartX;
        return prepareThumbnailAnimationWithDuration(clipAnim, appWidth, appHeight, getAspectScaleDuration(), getAspectScaleInterpolator());
    }

    private Animation createAspectScaledThumbnailEnterFreeformAnimationLocked(Rect frame, Rect surfaceInsets, int taskId) {
        getNextAppTransitionStartRect(taskId, this.mTmpRect);
        return createAspectScaledThumbnailFreeformAnimationLocked(this.mTmpRect, frame, surfaceInsets, true);
    }

    private Animation createAspectScaledThumbnailExitFreeformAnimationLocked(Rect frame, Rect surfaceInsets, int taskId) {
        getNextAppTransitionStartRect(taskId, this.mTmpRect);
        return createAspectScaledThumbnailFreeformAnimationLocked(frame, this.mTmpRect, surfaceInsets, false);
    }

    private AnimationSet createAspectScaledThumbnailFreeformAnimationLocked(Rect sourceFrame, Rect destFrame, Rect surfaceInsets, boolean enter) {
        ScaleAnimation scale;
        TranslateAnimation translation;
        Rect rect = sourceFrame;
        Rect rect2 = destFrame;
        Rect rect3 = surfaceInsets;
        float sourceWidth = (float) sourceFrame.width();
        float sourceHeight = (float) sourceFrame.height();
        float destWidth = (float) destFrame.width();
        float destHeight = (float) destFrame.height();
        float scaleH = enter ? sourceWidth / destWidth : destWidth / sourceWidth;
        float scaleV = enter ? sourceHeight / destHeight : destHeight / sourceHeight;
        AnimationSet set = new AnimationSet(true);
        int i = 0;
        int surfaceInsetsH = rect3 == null ? 0 : rect3.left + rect3.right;
        if (rect3 != null) {
            i = rect3.top + rect3.bottom;
        }
        int surfaceInsetsV = i;
        float scaleHCenter = ((enter ? destWidth : sourceWidth) + ((float) surfaceInsetsH)) / 2.0f;
        float scaleVCenter = ((enter ? destHeight : sourceHeight) + ((float) surfaceInsetsV)) / 2.0f;
        if (enter) {
            scale = new ScaleAnimation(scaleH, 1.0f, scaleV, 1.0f, scaleHCenter, scaleVCenter);
        } else {
            int i2 = surfaceInsetsH;
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, scaleH, 1.0f, scaleV, scaleHCenter, scaleVCenter);
        }
        int sourceHCenter = rect.left + (sourceFrame.width() / 2);
        surfaceInsetsV = rect.top + (sourceFrame.height() / 2);
        surfaceInsetsH = rect2.left + (destFrame.width() / 2);
        int destVCenter = rect2.top + (destFrame.height() / 2);
        int fromX = enter ? sourceHCenter - surfaceInsetsH : surfaceInsetsH - sourceHCenter;
        int fromY = enter ? surfaceInsetsV - destVCenter : destVCenter - surfaceInsetsV;
        if (enter) {
            translation = new TranslateAnimation((float) fromX, 0.0f, (float) fromY, 0.0f);
        } else {
            float f = sourceWidth;
            float f2 = sourceHeight;
            translation = new TranslateAnimation(0.0f, (float) fromX, 0.0f, (float) fromY);
        }
        set.addAnimation(scale);
        set.addAnimation(translation);
        final IRemoteCallback callback = this.mAnimationFinishedCallback;
        if (callback != null) {
            set.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    AppTransition.this.mService.mH.obtainMessage(26, callback).sendToTarget();
                }

                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
        return set;
    }

    Animation createThumbnailScaleAnimationLocked(int appWidth, int appHeight, int transit, GraphicBuffer thumbnailHeader) {
        Animation a;
        int i = appWidth;
        int i2 = appHeight;
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader.getWidth();
        float thumbWidth = thumbWidthI > 0 ? (float) thumbWidthI : 1.0f;
        int thumbHeightI = thumbnailHeader.getHeight();
        float thumbHeight = thumbHeightI > 0 ? (float) thumbHeightI : 1.0f;
        float scaleW;
        float scaleH;
        Animation scale;
        if (this.mNextAppTransitionScaleUp) {
            scaleW = ((float) i) / thumbWidth;
            scaleH = ((float) i2) / thumbHeight;
            scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleH, computePivot(this.mTmpRect.left, 1.0f / scaleW), computePivot(this.mTmpRect.top, 1.0f / scaleH));
            scale.setInterpolator(this.mDecelerateInterpolator);
            Animation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setInterpolator(this.mThumbnailFadeOutInterpolator);
            Animation set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha);
            a = set;
        } else {
            scaleW = ((float) i) / thumbWidth;
            scaleH = ((float) i2) / thumbHeight;
            scale = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, computePivot(this.mTmpRect.left, 1.0f / scaleW), computePivot(this.mTmpRect.top, 1.0f / scaleH));
        }
        return prepareThumbnailAnimation(a, i, i2, transit);
    }

    Animation createThumbnailEnterExitAnimationLocked(int thumbTransitState, Rect containingFrame, int transit, int taskId) {
        Animation a;
        int i = transit;
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        GraphicBuffer thumbnailHeader = getAppTransitionThumbnailHeader(taskId);
        getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader != null ? thumbnailHeader.getWidth() : appWidth;
        float thumbWidth = thumbWidthI > 0 ? (float) thumbWidthI : 1.0f;
        int thumbHeightI = thumbnailHeader != null ? thumbnailHeader.getHeight() : appHeight;
        float thumbHeight = thumbHeightI > 0 ? (float) thumbHeightI : 1.0f;
        switch (thumbTransitState) {
            case 0:
                float scaleW = thumbWidth / ((float) appWidth);
                float scaleH = thumbHeight / ((float) appHeight);
                a = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, computePivot(this.mTmpRect.left, scaleW), computePivot(this.mTmpRect.top, scaleH));
                break;
            case 1:
                if (i != 14) {
                    a = new AlphaAnimation(1.0f, 1.0f);
                    break;
                }
                a = new AlphaAnimation(1.0f, 0.0f);
                break;
            case 2:
                a = new AlphaAnimation(1.0f, 1.0f);
                break;
            case 3:
                float scaleW2 = thumbWidth / ((float) appWidth);
                float scaleH2 = thumbHeight / ((float) appHeight);
                Animation scale = new ScaleAnimation(1.0f, scaleW2, 1.0f, scaleH2, computePivot(this.mTmpRect.left, scaleW2), computePivot(this.mTmpRect.top, scaleH2));
                Animation alpha = new AlphaAnimation(1.0f, 0.0f);
                Animation set = new AnimationSet(true);
                set.addAnimation(scale);
                set.addAnimation(alpha);
                set.setZAdjustment(1);
                a = set;
                break;
            default:
                throw new RuntimeException("Invalid thumbnail transition state");
        }
        return prepareThumbnailAnimation(a, appWidth, appHeight, i);
    }

    private Animation createRelaunchAnimation(Rect containingFrame, Rect contentInsets) {
        getDefaultNextAppTransitionStartRect(this.mTmpFromClipRect);
        int left = this.mTmpFromClipRect.left;
        int top = this.mTmpFromClipRect.top;
        this.mTmpFromClipRect.offset(-left, -top);
        this.mTmpToClipRect.set(0, 0, containingFrame.width(), containingFrame.height());
        AnimationSet set = new AnimationSet(true);
        float fromWidth = (float) this.mTmpFromClipRect.width();
        float toWidth = (float) this.mTmpToClipRect.width();
        float fromHeight = (float) this.mTmpFromClipRect.height();
        float toHeight = (float) ((this.mTmpToClipRect.height() - contentInsets.top) - contentInsets.bottom);
        int translateAdjustment = 0;
        if (fromWidth > toWidth || fromHeight > toHeight) {
            set.addAnimation(new ScaleAnimation(fromWidth / toWidth, 1.0f, fromHeight / toHeight, 1.0f));
            translateAdjustment = (int) ((((float) contentInsets.top) * fromHeight) / toHeight);
        } else {
            set.addAnimation(new ClipRectAnimation(this.mTmpFromClipRect, this.mTmpToClipRect));
        }
        set.addAnimation(new TranslateAnimation((float) (left - containingFrame.left), 0.0f, (float) ((top - containingFrame.top) - translateAdjustment), 0.0f));
        set.setDuration(250);
        set.setZAdjustment(1);
        return set;
    }

    boolean canSkipFirstFrame() {
        return (this.mNextAppTransitionType == 1 || this.mNextAppTransitionType == 7 || this.mNextAppTransitionType == 8 || this.mNextAppTransition == 20) ? false : true;
    }

    RemoteAnimationController getRemoteAnimationController() {
        return this.mRemoteAnimationController;
    }

    /* JADX WARNING: Missing block: B:116:0x0301, code:
            if (r14 == false) goto L_0x0304;
     */
    /* JADX WARNING: Missing block: B:117:0x0304, code:
            r4 = 7;
     */
    /* JADX WARNING: Missing block: B:118:0x0305, code:
            r1 = r4;
     */
    /* JADX WARNING: Missing block: B:119:0x0307, code:
            if (r14 == false) goto L_0x030c;
     */
    /* JADX WARNING: Missing block: B:120:0x0309, code:
            r2 = 4;
     */
    /* JADX WARNING: Missing block: B:121:0x030c, code:
            r1 = r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    Animation loadAnimation(LayoutParams lp, int transit, boolean enter, int uiMode, int orientation, Rect frame, Rect displayFrame, Rect insets, Rect surfaceInsets, Rect stableInsets, boolean isVoiceInteraction, boolean freeform, int taskId) {
        Animation a;
        int animAttr;
        LayoutParams layoutParams = lp;
        int i = transit;
        boolean z = enter;
        Rect rect = frame;
        if (isKeyguardGoingAwayTransit(transit) && z) {
            a = loadKeyguardExitAnimation(i);
        } else if (i == 22) {
            a = null;
        } else if (i == 23 && !z) {
            a = loadAnimationRes(layoutParams, 17432764);
        } else if (i == 26) {
            a = null;
        } else {
            int i2 = 6;
            int i3;
            String str;
            StringBuilder stringBuilder;
            if (isVoiceInteraction && (i == 6 || i == 8 || i == 10)) {
                if (z) {
                    i3 = 17432751;
                } else {
                    i3 = 17432752;
                }
                a = loadAnimationRes(layoutParams, i3);
                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("applyAnimation voice: anim=");
                    stringBuilder.append(a);
                    stringBuilder.append(" transit=");
                    stringBuilder.append(appTransitionToString(transit));
                    stringBuilder.append(" isEntrance=");
                    stringBuilder.append(z);
                    stringBuilder.append(" Callers=");
                    stringBuilder.append(Debug.getCallers(3));
                    Slog.v(str, stringBuilder.toString());
                }
            } else {
                int i4 = 9;
                if (isVoiceInteraction && (i == 7 || i == 9 || i == 11)) {
                    if (z) {
                        i3 = 17432749;
                    } else {
                        i3 = 17432750;
                    }
                    a = loadAnimationRes(layoutParams, i3);
                    if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("applyAnimation voice: anim=");
                        stringBuilder.append(a);
                        stringBuilder.append(" transit=");
                        stringBuilder.append(appTransitionToString(transit));
                        stringBuilder.append(" isEntrance=");
                        stringBuilder.append(z);
                        stringBuilder.append(" Callers=");
                        stringBuilder.append(Debug.getCallers(3));
                        Slog.v(str, stringBuilder.toString());
                    }
                } else if (i == 18) {
                    a = createRelaunchAnimation(rect, insets);
                    if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("applyAnimation: anim=");
                        stringBuilder.append(a);
                        stringBuilder.append(" nextAppTransition=");
                        stringBuilder.append(this.mNextAppTransition);
                        stringBuilder.append(" transit=");
                        stringBuilder.append(appTransitionToString(transit));
                        stringBuilder.append(" Callers=");
                        stringBuilder.append(Debug.getCallers(3));
                        Slog.v(str, stringBuilder.toString());
                    }
                } else {
                    Rect rect2 = insets;
                    if (this.mNextAppTransitionType == 1) {
                        a = loadAnimationRes(this.mNextAppTransitionPackage, z ? this.mNextAppTransitionEnter : this.mNextAppTransitionExit);
                        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyAnimation: anim=");
                            stringBuilder.append(a);
                            stringBuilder.append(" nextAppTransition=ANIM_CUSTOM transit=");
                            stringBuilder.append(appTransitionToString(transit));
                            stringBuilder.append(" isEntrance=");
                            stringBuilder.append(z);
                            stringBuilder.append(" Callers=");
                            stringBuilder.append(Debug.getCallers(3));
                            Slog.v(str, stringBuilder.toString());
                        }
                    } else if (this.mNextAppTransitionType == 7) {
                        a = loadAnimationRes(this.mNextAppTransitionPackage, this.mNextAppTransitionInPlace);
                        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("applyAnimation: anim=");
                            stringBuilder.append(a);
                            stringBuilder.append(" nextAppTransition=ANIM_CUSTOM_IN_PLACE transit=");
                            stringBuilder.append(appTransitionToString(transit));
                            stringBuilder.append(" Callers=");
                            stringBuilder.append(Debug.getCallers(3));
                            Slog.v(str, stringBuilder.toString());
                        }
                    } else {
                        Animation a2;
                        String str2;
                        StringBuilder stringBuilder2;
                        if (this.mNextAppTransitionType == 8) {
                            a2 = createClipRevealAnimationLocked(i, z, rect, displayFrame);
                            if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("applyAnimation: anim=");
                                stringBuilder2.append(a2);
                                stringBuilder2.append(" nextAppTransition=ANIM_CLIP_REVEAL transit=");
                                stringBuilder2.append(appTransitionToString(transit));
                                stringBuilder2.append(" Callers=");
                                stringBuilder2.append(Debug.getCallers(3));
                                Slog.v(str2, stringBuilder2.toString());
                            }
                        } else {
                            Rect a3 = displayFrame;
                            int i5;
                            String str3;
                            StringBuilder stringBuilder3;
                            if (this.mNextAppTransitionType == 2) {
                                a2 = createScaleUpAnimationLocked(i, z, rect);
                                if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("applyAnimation: anim=");
                                    stringBuilder2.append(a2);
                                    stringBuilder2.append(" nextAppTransition=ANIM_SCALE_UP transit=");
                                    stringBuilder2.append(appTransitionToString(transit));
                                    stringBuilder2.append(" isEntrance=");
                                    stringBuilder2.append(z);
                                    stringBuilder2.append(" Callers=");
                                    stringBuilder2.append(Debug.getCallers(3));
                                    Slog.v(str2, stringBuilder2.toString());
                                }
                            } else if (this.mNextAppTransitionType == 3 || this.mNextAppTransitionType == 4) {
                                i5 = 3;
                                this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == i5;
                                a = createThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(z), rect, i, taskId);
                                if (!WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                                    return a;
                                }
                                str2 = this.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_SCALE_UP" : "ANIM_THUMBNAIL_SCALE_DOWN";
                                str3 = TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("applyAnimation: anim=");
                                stringBuilder3.append(a);
                                stringBuilder3.append(" nextAppTransition=");
                                stringBuilder3.append(str2);
                                stringBuilder3.append(" transit=");
                                stringBuilder3.append(appTransitionToString(transit));
                                stringBuilder3.append(" isEntrance=");
                                stringBuilder3.append(z);
                                stringBuilder3.append(" Callers=");
                                stringBuilder3.append(Debug.getCallers(i5));
                                Slog.v(str3, stringBuilder3.toString());
                                return a;
                            } else {
                                int i6 = 5;
                                if (this.mNextAppTransitionType == 5 || this.mNextAppTransitionType == 6) {
                                    this.mNextAppTransitionScaleUp = this.mNextAppTransitionType == 5;
                                    i5 = 3;
                                    a = createAspectScaledThumbnailEnterExitAnimationLocked(getThumbnailTransitionState(z), uiMode, orientation, i, rect, rect2, surfaceInsets, stableInsets, freeform, taskId);
                                    if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                                        str = this.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_ASPECT_SCALE_UP" : "ANIM_THUMBNAIL_ASPECT_SCALE_DOWN";
                                        str2 = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("applyAnimation: anim=");
                                        stringBuilder2.append(a);
                                        stringBuilder2.append(" nextAppTransition=");
                                        stringBuilder2.append(str);
                                        stringBuilder2.append(" transit=");
                                        stringBuilder2.append(appTransitionToString(transit));
                                        stringBuilder2.append(" isEntrance=");
                                        stringBuilder2.append(z);
                                        stringBuilder2.append(" Callers=");
                                        stringBuilder2.append(Debug.getCallers(i5));
                                        Slog.v(str2, stringBuilder2.toString());
                                    }
                                } else if (this.mNextAppTransitionType == 9 && z) {
                                    a2 = loadAnimationRes(PackageManagerService.PLATFORM_PACKAGE_NAME, 17432741);
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("applyAnimation NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS: anim=");
                                    stringBuilder2.append(a2);
                                    stringBuilder2.append(" transit=");
                                    stringBuilder2.append(appTransitionToString(transit));
                                    stringBuilder2.append(" isEntrance=true Callers=");
                                    stringBuilder2.append(Debug.getCallers(3));
                                    Slog.v(str2, stringBuilder2.toString());
                                } else {
                                    animAttr = 0;
                                    int i7 = 19;
                                    if (i != 19) {
                                        switch (i) {
                                            case 6:
                                                break;
                                            case 7:
                                                break;
                                            case 8:
                                                break;
                                            case 9:
                                                animAttr = z ? 10 : 11;
                                                break;
                                            case 10:
                                                if (z) {
                                                    i6 = 12;
                                                } else {
                                                    i6 = 13;
                                                }
                                                animAttr = i6;
                                                break;
                                            case 11:
                                                if (z) {
                                                    i6 = 14;
                                                } else {
                                                    i6 = 15;
                                                }
                                                animAttr = i6;
                                                break;
                                            case 12:
                                                if (!this.mService.mPolicy.isKeyguardShowingOrOccluded()) {
                                                    if (z) {
                                                        i7 = 18;
                                                    }
                                                    animAttr = i7;
                                                    break;
                                                }
                                                break;
                                            case 13:
                                                if (z) {
                                                    i6 = 16;
                                                } else {
                                                    i6 = 17;
                                                }
                                                animAttr = i6;
                                                break;
                                            case 14:
                                                if (z) {
                                                    i6 = 20;
                                                } else {
                                                    i6 = 21;
                                                }
                                                animAttr = i6;
                                                break;
                                            case 15:
                                                animAttr = z ? 22 : 23;
                                                break;
                                            case 16:
                                                if (z) {
                                                    i6 = 25;
                                                } else {
                                                    i6 = 24;
                                                }
                                                animAttr = i6;
                                                break;
                                            default:
                                                switch (i) {
                                                    case 24:
                                                        break;
                                                    case H.SHOW_STRICT_MODE_VIOLATION /*25*/:
                                                        break;
                                                }
                                                break;
                                        }
                                    }
                                    if (z) {
                                        i4 = 8;
                                    }
                                    animAttr = i4;
                                    Animation a4 = animAttr != 0 ? loadAnimationAttr(layoutParams, animAttr, i) : null;
                                    if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
                                        str3 = TAG;
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("applyAnimation: anim=");
                                        stringBuilder3.append(a4);
                                        stringBuilder3.append(" animAttr=0x");
                                        stringBuilder3.append(Integer.toHexString(animAttr));
                                        stringBuilder3.append(" transit=");
                                        stringBuilder3.append(appTransitionToString(transit));
                                        stringBuilder3.append(" isEntrance=");
                                        stringBuilder3.append(z);
                                        stringBuilder3.append(" Callers=");
                                        stringBuilder3.append(Debug.getCallers(3));
                                        Slog.v(str3, stringBuilder3.toString());
                                    }
                                    animAttr = taskId;
                                    return a4;
                                }
                            }
                        }
                        a = a2;
                    }
                }
            }
        }
        animAttr = taskId;
        return a;
    }

    private Animation loadKeyguardExitAnimation(int transit) {
        if ((this.mNextAppTransitionFlags & 2) != 0) {
            return null;
        }
        boolean z = true;
        boolean toShade = (this.mNextAppTransitionFlags & 1) != 0;
        WindowManagerPolicy windowManagerPolicy = this.mService.mPolicy;
        if (transit != 21) {
            z = false;
        }
        return windowManagerPolicy.createHiddenByKeyguardExit(z, toShade);
    }

    int getAppStackClipMode() {
        if (this.mNextAppTransition == 20 || this.mNextAppTransition == 21) {
            return 1;
        }
        int i;
        if (this.mNextAppTransition == 18 || this.mNextAppTransition == 19 || this.mNextAppTransitionType == 8) {
            i = 2;
        } else {
            i = 0;
        }
        return i;
    }

    public int getTransitFlags() {
        return this.mNextAppTransitionFlags;
    }

    void postAnimationCallback() {
        if (this.mNextAppTransitionCallback != null) {
            this.mService.mH.sendMessage(this.mService.mH.obtainMessage(26, this.mNextAppTransitionCallback));
            this.mNextAppTransitionCallback = null;
        }
    }

    void overridePendingAppTransition(String packageName, int enterAnim, int exitAnim, IRemoteCallback startedCallback) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 1;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionEnter = enterAnim;
            this.mNextAppTransitionExit = exitAnim;
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
        }
    }

    void overridePendingAppTransitionScaleUp(int startX, int startY, int startWidth, int startHeight) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 2;
            putDefaultNextAppTransitionCoordinates(startX, startY, startWidth, startHeight, null);
            postAnimationCallback();
        }
    }

    void overridePendingAppTransitionClipReveal(int startX, int startY, int startWidth, int startHeight) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 8;
            putDefaultNextAppTransitionCoordinates(startX, startY, startWidth, startHeight, null);
            postAnimationCallback();
        }
    }

    void overridePendingAppTransitionThumb(GraphicBuffer srcThumb, int startX, int startY, IRemoteCallback startedCallback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            int i;
            clear();
            if (scaleUp) {
                i = 3;
            } else {
                i = 4;
            }
            this.mNextAppTransitionType = i;
            this.mNextAppTransitionScaleUp = scaleUp;
            putDefaultNextAppTransitionCoordinates(startX, startY, 0, 0, srcThumb);
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
        }
    }

    void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer srcThumb, int startX, int startY, int targetWidth, int targetHeight, IRemoteCallback startedCallback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            int i;
            clear();
            if (scaleUp) {
                i = 5;
            } else {
                i = 6;
            }
            this.mNextAppTransitionType = i;
            this.mNextAppTransitionScaleUp = scaleUp;
            putDefaultNextAppTransitionCoordinates(startX, startY, targetWidth, targetHeight, srcThumb);
            postAnimationCallback();
            this.mNextAppTransitionCallback = startedCallback;
        }
    }

    void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] specs, IRemoteCallback onAnimationStartedCallback, IRemoteCallback onAnimationFinishedCallback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            int i;
            clear();
            if (scaleUp) {
                i = 5;
            } else {
                i = 6;
            }
            this.mNextAppTransitionType = i;
            this.mNextAppTransitionScaleUp = scaleUp;
            if (specs != null) {
                for (i = 0; i < specs.length; i++) {
                    AppTransitionAnimationSpec spec = specs[i];
                    if (spec != null) {
                        this.mNextAppTransitionAnimationsSpecs.put(spec.taskId, spec);
                        if (i == 0) {
                            Rect rect = spec.rect;
                            putDefaultNextAppTransitionCoordinates(rect.left, rect.top, rect.width(), rect.height(), spec.buffer);
                        }
                    }
                }
            }
            postAnimationCallback();
            this.mNextAppTransitionCallback = onAnimationStartedCallback;
            this.mAnimationFinishedCallback = onAnimationFinishedCallback;
        }
    }

    void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback callback, boolean scaleUp) {
        if (canOverridePendingAppTransition()) {
            int i;
            clear();
            if (scaleUp) {
                i = 5;
            } else {
                i = 6;
            }
            this.mNextAppTransitionType = i;
            this.mNextAppTransitionAnimationsSpecsFuture = specsFuture;
            this.mNextAppTransitionScaleUp = scaleUp;
            this.mNextAppTransitionFutureCallback = callback;
        }
    }

    void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) {
        if (isTransitionSet()) {
            clear();
            this.mNextAppTransitionType = 10;
            this.mRemoteAnimationController = new RemoteAnimationController(this.mService, remoteAnimationAdapter, this.mService.mH);
        }
    }

    void overrideInPlaceAppTransition(String packageName, int anim) {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 7;
            this.mNextAppTransitionPackage = packageName;
            this.mNextAppTransitionInPlace = anim;
        }
    }

    void overridePendingAppTransitionStartCrossProfileApps() {
        if (canOverridePendingAppTransition()) {
            clear();
            this.mNextAppTransitionType = 9;
            postAnimationCallback();
        }
    }

    private boolean canOverridePendingAppTransition() {
        return isTransitionSet() && this.mNextAppTransitionType != 10;
    }

    private void fetchAppTransitionSpecsFromFuture() {
        if (this.mNextAppTransitionAnimationsSpecsFuture != null) {
            this.mNextAppTransitionAnimationsSpecsPending = true;
            IAppTransitionAnimationSpecsFuture future = this.mNextAppTransitionAnimationsSpecsFuture;
            this.mNextAppTransitionAnimationsSpecsFuture = null;
            this.mDefaultExecutor.execute(new -$$Lambda$AppTransition$CyT0POoZKxhd7Ybm_eVYXG4NCrI(this, future));
        }
    }

    public static /* synthetic */ void lambda$fetchAppTransitionSpecsFromFuture$0(AppTransition appTransition, IAppTransitionAnimationSpecsFuture future) {
        appTransition.mService.mH.removeMessages(102);
        appTransition.mService.mH.sendEmptyMessageDelayed(102, 5000);
        AppTransitionAnimationSpec[] specs = null;
        try {
            Binder.allowBlocking(future.asBinder());
            specs = future.get();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to fetch app transition specs: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
        }
        appTransition.mService.mH.removeMessages(102);
        synchronized (appTransition.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                appTransition.mNextAppTransitionAnimationsSpecsPending = false;
                appTransition.overridePendingAppTransitionMultiThumb(specs, appTransition.mNextAppTransitionFutureCallback, null, appTransition.mNextAppTransitionScaleUp);
                appTransition.mNextAppTransitionFutureCallback = null;
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        appTransition.mService.requestTraversal();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNextAppTransition=");
        stringBuilder.append(appTransitionToString(this.mNextAppTransition));
        return stringBuilder.toString();
    }

    public static String appTransitionToString(int transition) {
        switch (transition) {
            case -1:
                return "TRANSIT_UNSET";
            case 0:
                return "TRANSIT_NONE";
            case 6:
                return "TRANSIT_ACTIVITY_OPEN";
            case 7:
                return "TRANSIT_ACTIVITY_CLOSE";
            case 8:
                return "TRANSIT_TASK_OPEN";
            case 9:
                return "TRANSIT_TASK_CLOSE";
            case 10:
                return "TRANSIT_TASK_TO_FRONT";
            case 11:
                return "TRANSIT_TASK_TO_BACK";
            case 12:
                return "TRANSIT_WALLPAPER_CLOSE";
            case 13:
                return "TRANSIT_WALLPAPER_OPEN";
            case 14:
                return "TRANSIT_WALLPAPER_INTRA_OPEN";
            case 15:
                return "TRANSIT_WALLPAPER_INTRA_CLOSE";
            case 16:
                return "TRANSIT_TASK_OPEN_BEHIND";
            case 18:
                return "TRANSIT_ACTIVITY_RELAUNCH";
            case H.REPORT_WINDOWS_CHANGE /*19*/:
                return "TRANSIT_DOCK_TASK_FROM_RECENTS";
            case 20:
                return "TRANSIT_KEYGUARD_GOING_AWAY";
            case BackupHandler.MSG_OP_COMPLETE /*21*/:
                return "TRANSIT_KEYGUARD_GOING_AWAY_ON_WALLPAPER";
            case H.REPORT_HARD_KEYBOARD_STATUS_CHANGE /*22*/:
                return "TRANSIT_KEYGUARD_OCCLUDE";
            case H.BOOT_TIMEOUT /*23*/:
                return "TRANSIT_KEYGUARD_UNOCCLUDE";
            case 24:
                return "TRANSIT_TRANSLUCENT_ACTIVITY_OPEN";
            case H.SHOW_STRICT_MODE_VIOLATION /*25*/:
                return "TRANSIT_TRANSLUCENT_ACTIVITY_CLOSE";
            case H.DO_ANIMATION_CALLBACK /*26*/:
                return "TRANSIT_CRASHING_ACTIVITY_CLOSE";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<UNKNOWN: ");
                stringBuilder.append(transition);
                stringBuilder.append(">");
                return stringBuilder.toString();
        }
    }

    private String appStateToString() {
        switch (this.mAppTransitionState) {
            case 0:
                return "APP_STATE_IDLE";
            case 1:
                return "APP_STATE_READY";
            case 2:
                return "APP_STATE_RUNNING";
            case 3:
                return "APP_STATE_TIMEOUT";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown state=");
                stringBuilder.append(this.mAppTransitionState);
                return stringBuilder.toString();
        }
    }

    private String transitTypeToString() {
        switch (this.mNextAppTransitionType) {
            case 0:
                return "NEXT_TRANSIT_TYPE_NONE";
            case 1:
                return "NEXT_TRANSIT_TYPE_CUSTOM";
            case 2:
                return "NEXT_TRANSIT_TYPE_SCALE_UP";
            case 3:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP";
            case 4:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN";
            case 5:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_UP";
            case 6:
                return "NEXT_TRANSIT_TYPE_THUMBNAIL_ASPECT_SCALE_DOWN";
            case 7:
                return "NEXT_TRANSIT_TYPE_CUSTOM_IN_PLACE";
            case 9:
                return "NEXT_TRANSIT_TYPE_OPEN_CROSS_PROFILE_APPS";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown type=");
                stringBuilder.append(this.mNextAppTransitionType);
                return stringBuilder.toString();
        }
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1159641169921L, this.mAppTransitionState);
        proto.write(1159641169922L, this.mLastUsedAppTransition);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println(this);
        pw.print(prefix);
        pw.print("mAppTransitionState=");
        pw.println(appStateToString());
        if (this.mNextAppTransitionType != 0) {
            pw.print(prefix);
            pw.print("mNextAppTransitionType=");
            pw.println(transitTypeToString());
        }
        switch (this.mNextAppTransitionType) {
            case 1:
                pw.print(prefix);
                pw.print("mNextAppTransitionPackage=");
                pw.println(this.mNextAppTransitionPackage);
                pw.print(prefix);
                pw.print("mNextAppTransitionEnter=0x");
                pw.print(Integer.toHexString(this.mNextAppTransitionEnter));
                pw.print(" mNextAppTransitionExit=0x");
                pw.println(Integer.toHexString(this.mNextAppTransitionExit));
                break;
            case 2:
                getDefaultNextAppTransitionStartRect(this.mTmpRect);
                pw.print(prefix);
                pw.print("mNextAppTransitionStartX=");
                pw.print(this.mTmpRect.left);
                pw.print(" mNextAppTransitionStartY=");
                pw.println(this.mTmpRect.top);
                pw.print(prefix);
                pw.print("mNextAppTransitionStartWidth=");
                pw.print(this.mTmpRect.width());
                pw.print(" mNextAppTransitionStartHeight=");
                pw.println(this.mTmpRect.height());
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                pw.print(prefix);
                pw.print("mDefaultNextAppTransitionAnimationSpec=");
                pw.println(this.mDefaultNextAppTransitionAnimationSpec);
                pw.print(prefix);
                pw.print("mNextAppTransitionAnimationsSpecs=");
                pw.println(this.mNextAppTransitionAnimationsSpecs);
                pw.print(prefix);
                pw.print("mNextAppTransitionScaleUp=");
                pw.println(this.mNextAppTransitionScaleUp);
                break;
            case 7:
                pw.print(prefix);
                pw.print("mNextAppTransitionPackage=");
                pw.println(this.mNextAppTransitionPackage);
                pw.print(prefix);
                pw.print("mNextAppTransitionInPlace=0x");
                pw.print(Integer.toHexString(this.mNextAppTransitionInPlace));
                break;
        }
        if (this.mNextAppTransitionCallback != null) {
            pw.print(prefix);
            pw.print("mNextAppTransitionCallback=");
            pw.println(this.mNextAppTransitionCallback);
        }
        if (this.mLastUsedAppTransition != 0) {
            pw.print(prefix);
            pw.print("mLastUsedAppTransition=");
            pw.println(appTransitionToString(this.mLastUsedAppTransition));
            pw.print(prefix);
            pw.print("mLastOpeningApp=");
            pw.println(this.mLastOpeningApp);
            pw.print(prefix);
            pw.print("mLastClosingApp=");
            pw.println(this.mLastClosingApp);
        }
        pw.print(prefix);
        pw.print("mNextAppTransitionAnimationsSpecsPending= ");
        pw.println(this.mNextAppTransitionAnimationsSpecsPending);
    }

    public void setCurrentUser(int newUserId) {
        this.mCurrentUserId = newUserId;
    }

    boolean prepareAppTransitionLocked(int transit, boolean alwaysKeepCurrent, int flags, boolean forceOverride) {
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Prepare app transition: transit=");
            stringBuilder.append(appTransitionToString(transit));
            stringBuilder.append(" ");
            stringBuilder.append(this);
            stringBuilder.append(" alwaysKeepCurrent=");
            stringBuilder.append(alwaysKeepCurrent);
            stringBuilder.append(" Callers=");
            stringBuilder.append(Debug.getCallers(3));
            Slog.v(str, stringBuilder.toString());
        }
        if (isKeyguardGoingAwayTransit(transit)) {
            this.mService.mUnknownAppVisibilityController.clear();
        }
        boolean allowSetCrashing = !isKeyguardTransit(this.mNextAppTransition) && transit == 26;
        if (forceOverride || isKeyguardTransit(transit) || !isTransitionSet() || this.mNextAppTransition == 0 || allowSetCrashing) {
            setAppTransition(transit, flags);
        } else if (!(alwaysKeepCurrent || isKeyguardTransit(this.mNextAppTransition) || this.mNextAppTransition == 26)) {
            if (transit == 8 && isTransitionEqual(9)) {
                setAppTransition(transit, flags);
            } else if (transit == 6 && isTransitionEqual(7)) {
                setAppTransition(transit, flags);
            } else if (isTaskTransit(transit) && isActivityTransit(this.mNextAppTransition)) {
                setAppTransition(transit, flags);
            }
        }
        boolean prepared = prepare();
        if (isTransitionSet()) {
            this.mService.mH.removeMessages(13);
            this.mService.mH.sendEmptyMessageDelayed(13, 5000);
        }
        return prepared;
    }

    public static boolean isKeyguardGoingAwayTransit(int transit) {
        return transit == 20 || transit == 21;
    }

    private static boolean isKeyguardTransit(int transit) {
        return isKeyguardGoingAwayTransit(transit) || transit == 22 || transit == 23;
    }

    static boolean isTaskTransit(int transit) {
        return isTaskOpenTransit(transit) || transit == 9 || transit == 11 || transit == 17;
    }

    private static boolean isTaskOpenTransit(int transit) {
        return transit == 8 || transit == 16 || transit == 10;
    }

    static boolean isActivityTransit(int transit) {
        return transit == 6 || transit == 7 || transit == 18;
    }

    private boolean shouldScaleDownThumbnailTransition(int uiMode, int orientation) {
        return this.mGridLayoutRecentsEnabled || orientation == 1;
    }
}
