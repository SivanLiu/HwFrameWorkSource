package com.android.server.wm;

import android.app.AppOpsManager;
import android.aps.IApsManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.Binder;
import android.os.Debug;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.SuggestionSpan;
import android.util.DisplayMetrics;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.DisplayCutout.ParcelableWrapper;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.IApplicationToken;
import android.view.IWindow;
import android.view.IWindowFocusObserver;
import android.view.IWindowId.Stub;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import android.view.SurfaceSession;
import android.view.WindowInfo;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwWindowStateAnimator;
import com.android.server.LocalServices;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputWindowHandle;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.IHwShutdownThread;
import com.android.server.wm.WindowManagerService.H;
import com.android.server.wm.utils.WmDisplayCutout;
import huawei.android.hwutil.HwFullScreenDisplay;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

public class WindowState extends WindowContainer<WindowState> implements com.android.server.policy.WindowManagerPolicy.WindowState {
    private static final float DEFAULT_DIM_AMOUNT_DEAD_WINDOW = 0.5f;
    private static final boolean IS_FULL_SCREEN = HwFullScreenDisplay.isFullScreenDevice();
    private static final boolean IS_NOTCH_PROP = (SystemProperties.get("ro.config.hw_notch_size", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) ^ 1);
    public static final int LOW_RESOLUTION_COMPOSITION_OFF = 1;
    public static final int LOW_RESOLUTION_COMPOSITION_ON = 2;
    public static final int LOW_RESOLUTION_FEATURE_OFF = 0;
    static final int MINIMUM_VISIBLE_HEIGHT_IN_DP = 36;
    static final int MINIMUM_VISIBLE_WIDTH_IN_DP = 42;
    static final int RESIZE_HANDLE_WIDTH_IN_DP = 10;
    static final boolean SHOW_TRANSACTIONS = false;
    static final String TAG = "WindowManager";
    private static final Comparator<WindowState> sWindowSubLayerComparator = new Comparator<WindowState>() {
        public int compare(WindowState w1, WindowState w2) {
            int layer1 = w1.mSubLayer;
            int layer2 = w2.mSubLayer;
            if (layer1 < layer2 || (layer1 == layer2 && layer2 < 0)) {
                return -1;
            }
            return 1;
        }
    };
    boolean isBinderDiedCalling;
    private boolean mAnimateReplacingWindow;
    boolean mAnimatingExit;
    boolean mAppDied;
    boolean mAppFreezing;
    final int mAppOp;
    public boolean mAppOpVisibility;
    AppWindowToken mAppToken;
    public final LayoutParams mAttrs;
    final int mBaseLayer;
    private boolean mCanCarryColors;
    final IWindow mClient;
    private InputChannel mClientChannel;
    final Rect mCompatFrame;
    final Rect mContainingFrame;
    boolean mContentChanged;
    private final Rect mContentFrame;
    final Rect mContentInsets;
    private boolean mContentInsetsChanged;
    final Context mContext;
    private DeadWindowEventReceiver mDeadWindowEventReceiver;
    final DeathRecipient mDeathRecipient;
    final Rect mDecorFrame;
    private int mDecorTopCompensation;
    boolean mDestroying;
    WmDisplayCutout mDisplayCutout;
    private boolean mDisplayCutoutChanged;
    final Rect mDisplayFrame;
    private boolean mDragResizing;
    private boolean mDragResizingChangeReported;
    private WakeLock mDrawLock;
    private boolean mDrawnStateEvaluated;
    boolean mEnforceSizeCompat;
    private RemoteCallbackList<IWindowFocusObserver> mFocusCallbacks;
    int mForceCompatMode;
    private boolean mForceHideNonSystemOverlayWindow;
    final Rect mFrame;
    private long mFrameNumber;
    private boolean mFrameSizeChanged;
    final Rect mGivenContentInsets;
    boolean mGivenInsetsPending;
    final Region mGivenTouchableRegion;
    final Rect mGivenVisibleInsets;
    float mGlobalScale;
    float mHScale;
    boolean mHasSurface;
    boolean mHaveFrame;
    boolean mHidden;
    private boolean mHiddenWhileSuspended;
    boolean mInRelayout;
    InputChannel mInputChannel;
    final InputWindowHandle mInputWindowHandle;
    private final Rect mInsetFrame;
    float mInvGlobalScale;
    private boolean mIsChildWindow;
    private boolean mIsDimming;
    private final boolean mIsFloatingLayer;
    final boolean mIsImWindow;
    final boolean mIsWallpaper;
    final Rect mLastContentInsets;
    private WmDisplayCutout mLastDisplayCutout;
    final Rect mLastFrame;
    int mLastFreezeDuration;
    float mLastHScale;
    private final Rect mLastOutsets;
    private final Rect mLastOverscanInsets;
    final Rect mLastRelayoutContentInsets;
    private final MergedConfiguration mLastReportedConfiguration;
    private int mLastRequestedHeight;
    private int mLastRequestedWidth;
    private final Rect mLastStableInsets;
    final Rect mLastSurfaceInsets;
    CharSequence mLastTitle;
    float mLastVScale;
    private final Rect mLastVisibleInsets;
    int mLastVisibleLayoutRotation;
    int mLayer;
    final boolean mLayoutAttached;
    boolean mLayoutNeeded;
    int mLayoutSeq;
    public Point mLazyModeSurfacePosition;
    private boolean mMovedByResize;
    boolean mObscured;
    private boolean mOrientationChangeTimedOut;
    private boolean mOrientationChanging;
    private final Rect mOutsetFrame;
    final Rect mOutsets;
    private boolean mOutsetsChanged;
    private final Rect mOverscanFrame;
    final Rect mOverscanInsets;
    private boolean mOverscanInsetsChanged;
    protected Point mOverscanPosition;
    final boolean mOwnerCanAddInternalSystemWindow;
    final int mOwnerUid;
    final Rect mParentFrame;
    private boolean mParentFrameWasClippedByDisplayCutout;
    boolean mPermanentlyHidden;
    final WindowManagerPolicy mPolicy;
    boolean mPolicyVisibility;
    boolean mPolicyVisibilityAfterAnim;
    private PowerManagerWrapper mPowerManagerWrapper;
    boolean mRelayoutCalled;
    boolean mRemoveOnExit;
    boolean mRemoved;
    private WindowState mReplacementWindow;
    private boolean mReplacingRemoveRequested;
    boolean mReportOrientationChanged;
    int mRequestedHeight;
    int mRequestedWidth;
    private int mResizeMode;
    boolean mResizedWhileGone;
    boolean mSeamlesslyRotated;
    int mSeq;
    final Session mSession;
    private boolean mShowToOwnerOnly;
    boolean mSkipEnterAnimationForSeamlessReplacement;
    private final Rect mStableFrame;
    final Rect mStableInsets;
    private boolean mStableInsetsChanged;
    private String mStringNameCache;
    final int mSubLayer;
    private final Point mSurfacePosition;
    int mSystemUiVisibility;
    private TapExcludeRegionHolder mTapExcludeRegionHolder;
    final Matrix mTmpMatrix;
    private final Rect mTmpRect;
    WindowToken mToken;
    int mTouchableInsets;
    float mVScale;
    int mViewVisibility;
    final Rect mVisibleFrame;
    final Rect mVisibleInsets;
    private boolean mVisibleInsetsChanged;
    int mWallpaperDisplayOffsetX;
    int mWallpaperDisplayOffsetY;
    boolean mWallpaperVisible;
    float mWallpaperX;
    float mWallpaperXStep;
    float mWallpaperY;
    float mWallpaperYStep;
    private boolean mWasExiting;
    private boolean mWasVisibleBeforeClientHidden;
    boolean mWillReplaceWindow;
    final WindowStateAnimator mWinAnimator;
    final WindowId mWindowId;
    boolean mWindowRemovalAllowed;

    private final class DeadWindowEventReceiver extends InputEventReceiver {
        DeadWindowEventReceiver(InputChannel inputChannel) {
            super(inputChannel, WindowState.this.mService.mH.getLooper());
        }

        public void onInputEvent(InputEvent event, int displayId) {
            finishInputEvent(event, true);
        }
    }

    private class DeathRecipient implements android.os.IBinder.DeathRecipient {
        private DeathRecipient() {
        }

        /* synthetic */ DeathRecipient(WindowState x0, AnonymousClass1 x1) {
            this();
        }

        public void binderDied() {
            boolean resetSplitScreenResizing = false;
            try {
                synchronized (WindowState.this.mService.mWindowMap) {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState win = WindowState.this.mService.windowForClientLocked(WindowState.this.mSession, WindowState.this.mClient, false);
                    String str = WindowState.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("WIN DEATH: ");
                    stringBuilder.append(win);
                    Slog.i(str, stringBuilder.toString());
                    if (win != null) {
                        DisplayContent dc = WindowState.this.getDisplayContent();
                        if (win.mAppToken != null && win.mAppToken.findMainWindow() == win) {
                            WindowState.this.mService.mTaskSnapshotController.onAppDied(win.mAppToken);
                        }
                        WindowState.this.isBinderDiedCalling = true;
                        win.removeIfPossible(WindowState.this.shouldKeepVisibleDeadAppWindow());
                        if (win.mAttrs.type == 2034) {
                            TaskStack stack = dc.getSplitScreenPrimaryStackIgnoringVisibility();
                            if (stack != null) {
                                stack.resetDockedStackToMiddle();
                            }
                            resetSplitScreenResizing = true;
                        }
                    } else if (WindowState.this.mHasSurface) {
                        Slog.e(WindowState.TAG, "!!! LEAK !!! Window removed but surface still valid.");
                        WindowState.this.removeIfPossible();
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (resetSplitScreenResizing) {
                    WindowState.this.mService.mActivityManager.setSplitScreenResizing(false);
                }
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            } catch (IllegalArgumentException e2) {
            } catch (Throwable th) {
                while (true) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    interface PowerManagerWrapper {
        boolean isInteractive();

        void wakeUp(long j, String str);
    }

    static final class UpdateReportedVisibilityResults {
        boolean nowGone = true;
        int numDrawn;
        int numInteresting;
        int numVisible;

        UpdateReportedVisibilityResults() {
        }

        void reset() {
            this.numInteresting = 0;
            this.numVisible = 0;
            this.numDrawn = 0;
            this.nowGone = true;
        }
    }

    private static final class WindowId extends Stub {
        private final WeakReference<WindowState> mOuter;

        /* synthetic */ WindowId(WindowState x0, AnonymousClass1 x1) {
            this(x0);
        }

        private WindowId(WindowState outer) {
            this.mOuter = new WeakReference(outer);
        }

        public void registerFocusObserver(IWindowFocusObserver observer) {
            WindowState outer = (WindowState) this.mOuter.get();
            if (outer != null) {
                outer.registerFocusObserver(observer);
            }
        }

        public void unregisterFocusObserver(IWindowFocusObserver observer) {
            WindowState outer = (WindowState) this.mOuter.get();
            if (outer != null) {
                outer.unregisterFocusObserver(observer);
            }
        }

        public boolean isFocused() {
            WindowState outer = (WindowState) this.mOuter.get();
            return outer != null && outer.isFocused();
        }
    }

    private final class MoveAnimationSpec implements AnimationSpec {
        private final long mDuration;
        private Point mFrom;
        private Interpolator mInterpolator;
        private Point mTo;

        /* synthetic */ MoveAnimationSpec(WindowState x0, int x1, int x2, int x3, int x4, AnonymousClass1 x5) {
            this(x1, x2, x3, x4);
        }

        private MoveAnimationSpec(int fromX, int fromY, int toX, int toY) {
            this.mFrom = new Point();
            this.mTo = new Point();
            Animation anim = AnimationUtils.loadAnimation(WindowState.this.mContext, 17432765);
            this.mDuration = (long) (((float) anim.computeDurationHint()) * WindowState.this.mService.getWindowAnimationScaleLocked());
            this.mInterpolator = anim.getInterpolator();
            this.mFrom.set(fromX, fromY);
            this.mTo.set(toX, toY);
        }

        public long getDuration() {
            return this.mDuration;
        }

        public void apply(Transaction t, SurfaceControl leash, long currentPlayTime) {
            float v = this.mInterpolator.getInterpolation(((float) currentPlayTime) / ((float) getDuration()));
            t.setPosition(leash, ((float) this.mFrom.x) + (((float) (this.mTo.x - this.mFrom.x)) * v), ((float) this.mFrom.y) + (((float) (this.mTo.y - this.mFrom.y)) * v));
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.print("from=");
            pw.print(this.mFrom);
            pw.print(" to=");
            pw.print(this.mTo);
            pw.print(" duration=");
            pw.println(this.mDuration);
        }

        public void writeToProtoInner(ProtoOutputStream proto) {
            long token = proto.start(1146756268034L);
            this.mFrom.writeToProto(proto, 1146756268033L);
            this.mTo.writeToProto(proto, 1146756268034L);
            proto.write(1112396529667L, this.mDuration);
            proto.end(token);
        }
    }

    WindowState(WindowManagerService service, Session s, IWindow c, WindowToken token, WindowState parentWindow, int appOp, int seq, LayoutParams a, int viewVisibility, int ownerId, boolean ownerCanAddInternalSystemWindow, int forceCompatFlag) {
        final WindowManagerService windowManagerService = service;
        this(windowManagerService, s, c, token, parentWindow, appOp, seq, a, viewVisibility, ownerId, ownerCanAddInternalSystemWindow, new PowerManagerWrapper() {
            public void wakeUp(long time, String reason) {
                windowManagerService.mPowerManager.wakeUp(time, reason);
            }

            public boolean isInteractive() {
                return windowManagerService.mPowerManager.isInteractive();
            }
        }, forceCompatFlag);
    }

    WindowState(WindowManagerService service, Session s, IWindow c, WindowToken token, WindowState parentWindow, int appOp, int seq, LayoutParams a, int viewVisibility, int ownerId, boolean ownerCanAddInternalSystemWindow, PowerManagerWrapper powerManagerWrapper, int forceCompatFlag) {
        IWindow iWindow = c;
        WindowToken windowToken = token;
        WindowState windowState = parentWindow;
        LayoutParams layoutParams = a;
        int i = forceCompatFlag;
        super(service);
        this.mAttrs = new LayoutParams();
        this.mPolicyVisibility = true;
        this.mPolicyVisibilityAfterAnim = true;
        this.mAppOpVisibility = true;
        this.mHidden = true;
        this.mDragResizingChangeReported = true;
        this.mLayoutSeq = -1;
        this.mLastReportedConfiguration = new MergedConfiguration();
        this.mVisibleInsets = new Rect();
        this.mLastVisibleInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mLastContentInsets = new Rect();
        this.mLastRelayoutContentInsets = new Rect();
        this.mOverscanInsets = new Rect();
        this.mLastOverscanInsets = new Rect();
        this.mStableInsets = new Rect();
        this.mLastStableInsets = new Rect();
        this.mOutsets = new Rect();
        this.mLastOutsets = new Rect();
        this.mOutsetsChanged = false;
        this.mDisplayCutout = WmDisplayCutout.NO_CUTOUT;
        this.mLastDisplayCutout = WmDisplayCutout.NO_CUTOUT;
        this.mGivenContentInsets = new Rect();
        this.mGivenVisibleInsets = new Rect();
        this.mGivenTouchableRegion = new Region();
        this.mTouchableInsets = 0;
        this.mGlobalScale = 1.0f;
        this.mInvGlobalScale = 1.0f;
        this.mHScale = 1.0f;
        this.mVScale = 1.0f;
        this.mLastHScale = 1.0f;
        this.mLastVScale = 1.0f;
        this.mTmpMatrix = new Matrix();
        this.mFrame = new Rect();
        this.mLastFrame = new Rect();
        this.mFrameSizeChanged = false;
        this.mCompatFrame = new Rect();
        this.mContainingFrame = new Rect();
        this.mParentFrame = new Rect();
        this.mDisplayFrame = new Rect();
        this.mOverscanFrame = new Rect();
        this.mStableFrame = new Rect();
        this.mDecorFrame = new Rect();
        this.mContentFrame = new Rect();
        this.mVisibleFrame = new Rect();
        this.mOutsetFrame = new Rect();
        this.mInsetFrame = new Rect();
        this.mWallpaperX = -1.0f;
        this.mWallpaperY = -1.0f;
        this.mWallpaperXStep = -1.0f;
        this.mWallpaperYStep = -1.0f;
        this.mWallpaperDisplayOffsetX = Integer.MIN_VALUE;
        this.mWallpaperDisplayOffsetY = Integer.MIN_VALUE;
        this.mLastVisibleLayoutRotation = -1;
        this.mHasSurface = false;
        this.mWillReplaceWindow = false;
        this.mReplacingRemoveRequested = false;
        this.mAnimateReplacingWindow = false;
        this.mReplacementWindow = null;
        this.mSkipEnterAnimationForSeamlessReplacement = false;
        this.mForceCompatMode = 0;
        this.mTmpRect = new Rect();
        this.mResizedWhileGone = false;
        this.mSeamlesslyRotated = false;
        this.mLastSurfaceInsets = new Rect();
        this.mSurfacePosition = new Point();
        this.mFrameNumber = -1;
        this.mOverscanPosition = new Point();
        this.mIsDimming = false;
        this.isBinderDiedCalling = false;
        this.mDecorTopCompensation = 0;
        this.mLazyModeSurfacePosition = new Point();
        this.mSession = s;
        this.mClient = iWindow;
        this.mAppOp = appOp;
        this.mToken = windowToken;
        this.mAppToken = this.mToken.asAppWindowToken();
        this.mOwnerUid = ownerId;
        this.mOwnerCanAddInternalSystemWindow = ownerCanAddInternalSystemWindow;
        this.mWindowId = new WindowId(this, null);
        this.mAttrs.copyFrom(layoutParams);
        this.mLastSurfaceInsets.set(this.mAttrs.surfaceInsets);
        this.mViewVisibility = viewVisibility;
        this.mPolicy = this.mService.mPolicy;
        this.mContext = this.mService.mContext;
        DeathRecipient deathRecipient = new DeathRecipient(this, null);
        this.mSeq = seq;
        this.mEnforceSizeCompat = (this.mAttrs.privateFlags & 128) != 0;
        this.mPowerManagerWrapper = powerManagerWrapper;
        try {
            boolean z;
            InputApplicationHandle inputApplicationHandle;
            c.asBinder().linkToDeath(deathRecipient, 0);
            this.mDeathRecipient = deathRecipient;
            if (this.mAttrs.type < 1000 || this.mAttrs.type > 1999) {
                this.mBaseLayer = (this.mPolicy.getWindowLayerLw(this) * 10000) + 1000;
                this.mSubLayer = 0;
                this.mIsChildWindow = false;
                this.mLayoutAttached = false;
                z = this.mAttrs.type == 2011 || this.mAttrs.type == 2012;
                this.mIsImWindow = z;
                this.mIsWallpaper = this.mAttrs.type == 2013;
            } else {
                this.mBaseLayer = (this.mPolicy.getWindowLayerLw(windowState) * 10000) + 1000;
                this.mSubLayer = this.mPolicy.getSubWindowLayerFromTypeLw(layoutParams.type);
                this.mIsChildWindow = true;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Adding ");
                stringBuilder.append(this);
                stringBuilder.append(" to ");
                stringBuilder.append(windowState);
                Slog.v(str, stringBuilder.toString());
                windowState.addChild((WindowContainer) this, sWindowSubLayerComparator);
                this.mLayoutAttached = this.mAttrs.type != 1003;
                z = windowState.mAttrs.type == 2011 || windowState.mAttrs.type == 2012;
                this.mIsImWindow = z;
                this.mIsWallpaper = windowState.mAttrs.type == 2013;
            }
            z = this.mIsImWindow || this.mIsWallpaper;
            this.mIsFloatingLayer = z;
            if (i == -3) {
                this.mForceCompatMode = getTopParentWindow().mForceCompatMode;
            } else {
                this.mForceCompatMode = i;
            }
            if (this.mAppToken != null && this.mAppToken.mShowForAllUsers) {
                LayoutParams layoutParams2 = this.mAttrs;
                layoutParams2.flags |= DumpState.DUMP_FROZEN;
            }
            IHwWindowStateAnimator iwsa = HwServiceFactory.getHuaweiWindowStateAnimator();
            if (iwsa != null) {
                this.mWinAnimator = iwsa.getInstance(this);
            } else {
                this.mWinAnimator = new WindowStateAnimator(this);
            }
            this.mWinAnimator.mAlpha = layoutParams.alpha;
            this.mRequestedWidth = 0;
            this.mRequestedHeight = 0;
            this.mLastRequestedWidth = 0;
            this.mLastRequestedHeight = 0;
            this.mLayer = 0;
            if (this.mAppToken != null) {
                inputApplicationHandle = this.mAppToken.mInputApplicationHandle;
            } else {
                inputApplicationHandle = null;
            }
            this.mInputWindowHandle = new InputWindowHandle(inputApplicationHandle, this, iWindow, getDisplayId());
        } catch (RemoteException e) {
            this.mDeathRecipient = null;
            this.mIsChildWindow = false;
            this.mLayoutAttached = false;
            this.mIsImWindow = false;
            this.mIsWallpaper = false;
            this.mIsFloatingLayer = false;
            this.mBaseLayer = 0;
            this.mSubLayer = 0;
            this.mInputWindowHandle = null;
            this.mWinAnimator = null;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Window = ");
            stringBuilder2.append(this);
            stringBuilder2.append(" token = ");
            stringBuilder2.append(windowToken);
            Slog.v(str2, stringBuilder2.toString());
        }
    }

    void attach() {
        this.mSession.windowAddedLocked(this.mAttrs.packageName);
    }

    boolean getDrawnStateEvaluated() {
        return this.mDrawnStateEvaluated;
    }

    void setDrawnStateEvaluated(boolean evaluated) {
        this.mDrawnStateEvaluated = evaluated;
    }

    void onParentSet() {
        super.onParentSet();
        setDrawnStateEvaluated(false);
        getDisplayContent().reapplyMagnificationSpec();
    }

    public int getOwningUid() {
        return this.mOwnerUid;
    }

    public String getOwningPackage() {
        return this.mAttrs.packageName;
    }

    public boolean canAddInternalSystemWindow() {
        return this.mOwnerCanAddInternalSystemWindow;
    }

    public boolean canAcquireSleepToken() {
        return this.mSession.mCanAcquireSleepToken;
    }

    private void subtractInsets(Rect frame, Rect layoutFrame, Rect insetFrame, Rect displayFrame) {
        frame.inset(Math.max(0, insetFrame.left - Math.max(layoutFrame.left, displayFrame.left)), Math.max(0, insetFrame.top - Math.max(layoutFrame.top, displayFrame.top)), Math.max(0, Math.min(layoutFrame.right, displayFrame.right) - insetFrame.right), Math.max(0, Math.min(layoutFrame.bottom, displayFrame.bottom) - insetFrame.bottom));
    }

    private void correctionCutoutRegion(DisplayCutout displayCutout, Rect rect) {
        int insertTop = displayCutout.getSafeInsetTop();
        int insetBottom = displayCutout.getSafeInsetBottom();
        int insetLeft = displayCutout.getSafeInsetLeft();
        int insetRight = displayCutout.getSafeInsetRight();
        if (insertTop != 0 || insetBottom != 0) {
            rect.set(rect.left + 1, rect.top, rect.right - 1, rect.bottom);
        } else if (insetLeft != 0 || insetRight != 0) {
            rect.set(rect.left, rect.top + 1, rect.right, rect.bottom - 1);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:111:0x046f  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x03ff  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x05b7  */
    /* JADX WARNING: Removed duplicated region for block: B:165:0x066e  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x0640  */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x067a  */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x0674  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x01a5  */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x01e9  */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x01e7  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x01ec  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0248  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0202  */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x031e  */
    /* JADX WARNING: Removed duplicated region for block: B:100:0x02fc  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x03ff  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x046f  */
    /* JADX WARNING: Removed duplicated region for block: B:147:0x05b7  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x0640  */
    /* JADX WARNING: Removed duplicated region for block: B:165:0x066e  */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x0674  */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x067a  */
    /* JADX WARNING: Missing block: B:160:0x0650, code:
            if (r26 != r6.mFrame.height()) goto L_0x0655;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void computeFrameLw(Rect parentFrame, Rect displayFrame, Rect overscanFrame, Rect contentFrame, Rect visibleFrame, Rect decorFrame, Rect stableFrame, Rect outsetFrame, WmDisplayCutout displayCutout, boolean parentFrameWasClippedByDisplayCutout) {
        Rect rect = parentFrame;
        Rect rect2 = displayFrame;
        Rect rect3 = contentFrame;
        Rect rect4 = outsetFrame;
        if (!this.mWillReplaceWindow || (!this.mAnimatingExit && this.mReplacingRemoveRequested)) {
            Rect frozen;
            int layoutXDiff;
            int layoutYDiff;
            Rect layoutContainingFrame;
            int pw;
            int ph;
            boolean hasOutsets;
            int fw;
            int fh;
            int fh2;
            Task task;
            WmDisplayCutout displayCutout2;
            this.mHaveFrame = true;
            this.mParentFrameWasClippedByDisplayCutout = parentFrameWasClippedByDisplayCutout;
            Task task2 = getTask();
            boolean inFullscreenContainer = inFullscreenContainer();
            boolean z = task2 != null && task2.isFloating();
            boolean windowsAreFloating = z;
            DisplayContent dc = getDisplayContent();
            if (task2 == null || !isInMultiWindowMode()) {
                this.mInsetFrame.setEmpty();
            } else {
                task2.getTempInsetBounds(this.mInsetFrame);
            }
            DisplayContent dc2;
            if (inFullscreenContainer) {
                dc2 = dc;
            } else if (layoutInParentFrame()) {
                dc2 = dc;
            } else {
                int pw2;
                int ph2;
                getBounds(this.mContainingFrame);
                if (!(this.mAppToken == null || this.mAppToken.mFrozenBounds.isEmpty())) {
                    frozen = (Rect) this.mAppToken.mFrozenBounds.peek();
                    this.mContainingFrame.right = this.mContainingFrame.left + frozen.width();
                    this.mContainingFrame.bottom = this.mContainingFrame.top + frozen.height();
                }
                WindowState imeWin = this.mService.mInputMethodWindow;
                WindowState imeWin2 = imeWin;
                dc2 = dc;
                this.mService.mHwWMSEx.adjustWindowPosForPadPC(this.mContainingFrame, rect3, imeWin, this.mService.mInputMethodTarget, this);
                if (imeWin2 != null && imeWin2.isVisibleNow() && isInputMethodTarget()) {
                    if (inFreeformWindowingMode() && this.mContainingFrame.bottom > rect3.bottom) {
                        frozen = this.mContainingFrame;
                        frozen.top -= this.mContainingFrame.bottom - rect3.bottom;
                    } else if (!inPinnedWindowingMode() && this.mContainingFrame.bottom > rect.bottom) {
                        this.mContainingFrame.bottom = rect.bottom;
                    }
                }
                if (windowsAreFloating && this.mContainingFrame.isEmpty()) {
                    this.mContainingFrame.set(rect3);
                }
                TaskStack stack = getStack();
                if (inPinnedWindowingMode() && stack != null && stack.lastAnimatingBoundsWasToFullscreen()) {
                    this.mInsetFrame.intersectUnchecked(rect);
                    this.mContainingFrame.intersectUnchecked(rect);
                }
                this.mDisplayFrame.set(this.mContainingFrame);
                layoutXDiff = !this.mInsetFrame.isEmpty() ? this.mInsetFrame.left - this.mContainingFrame.left : 0;
                layoutYDiff = !this.mInsetFrame.isEmpty() ? this.mInsetFrame.top - this.mContainingFrame.top : 0;
                layoutContainingFrame = !this.mInsetFrame.isEmpty() ? this.mInsetFrame : this.mContainingFrame;
                int layoutXDiff2 = layoutXDiff;
                this.mTmpRect.set(0, 0, dc2.getDisplayInfo().logicalWidth, dc2.getDisplayInfo().logicalHeight);
                subtractInsets(this.mDisplayFrame, layoutContainingFrame, rect2, this.mTmpRect);
                if (!layoutInParentFrame()) {
                    subtractInsets(this.mContainingFrame, layoutContainingFrame, rect, this.mTmpRect);
                    subtractInsets(this.mInsetFrame, layoutContainingFrame, rect, this.mTmpRect);
                }
                frozen = rect2;
                frozen.intersect(layoutContainingFrame);
                layoutXDiff = layoutXDiff2;
                pw = this.mContainingFrame.width();
                ph = this.mContainingFrame.height();
                if (!this.mParentFrame.equals(rect)) {
                    this.mParentFrame.set(rect);
                    this.mContentChanged = true;
                }
                if (!(this.mRequestedWidth == this.mLastRequestedWidth && this.mRequestedHeight == this.mLastRequestedHeight)) {
                    this.mLastRequestedWidth = this.mRequestedWidth;
                    this.mLastRequestedHeight = this.mRequestedHeight;
                    this.mContentChanged = true;
                }
                this.mOverscanFrame.set(overscanFrame);
                this.mContentFrame.set(rect3);
                this.mVisibleFrame.set(visibleFrame);
                this.mDecorFrame.set(decorFrame);
                this.mStableFrame.set(stableFrame);
                hasOutsets = rect4 == null;
                if (hasOutsets) {
                    this.mOutsetFrame.set(rect4);
                }
                fw = this.mFrame.width();
                fh = this.mFrame.height();
                applyGravityAndUpdateFrame(layoutContainingFrame, frozen);
                if (hasOutsets) {
                    pw2 = pw;
                    ph2 = ph;
                    boolean z2 = hasOutsets;
                    this.mOutsets.set(0, 0, 0, 0);
                } else {
                    ph2 = ph;
                    pw2 = pw;
                    this.mOutsets.set(Math.max(this.mContentFrame.left - this.mOutsetFrame.left, 0), Math.max(this.mContentFrame.top - this.mOutsetFrame.top, 0), Math.max(this.mOutsetFrame.right - this.mContentFrame.right, 0), Math.max(this.mOutsetFrame.bottom - this.mContentFrame.bottom, 0));
                }
                int i;
                int i2;
                int i3;
                if (windowsAreFloating || this.mFrame.isEmpty()) {
                    fh2 = fh;
                    task = task2;
                    if (this.mAttrs.type != 2034) {
                        dc2.getDockedDividerController().positionDockedStackedDivider(this.mFrame);
                        this.mContentFrame.set(this.mFrame);
                        if (!this.mFrame.equals(this.mLastFrame)) {
                            z = true;
                            this.mMovedByResize = true;
                        }
                    } else {
                        z = true;
                        offsetSystemDialog(rect3);
                        this.mContentFrame.set(Math.max(this.mContentFrame.left, this.mFrame.left), Math.max(this.mContentFrame.top, this.mFrame.top), Math.min(this.mContentFrame.right, this.mFrame.right), Math.min(this.mContentFrame.bottom, this.mFrame.bottom));
                        this.mVisibleFrame.set(Math.max(this.mVisibleFrame.left, this.mFrame.left), Math.max(this.mVisibleFrame.top, this.mFrame.top), Math.min(this.mVisibleFrame.right, this.mFrame.right), Math.min(this.mVisibleFrame.bottom, this.mFrame.bottom));
                        this.mStableFrame.set(Math.max(this.mStableFrame.left, this.mFrame.left), Math.max(this.mStableFrame.top, this.mFrame.top), Math.min(this.mStableFrame.right, this.mFrame.right), Math.min(this.mStableFrame.bottom, this.mFrame.bottom));
                    }
                    if (inFullscreenContainer && !windowsAreFloating) {
                        this.mOverscanInsets.set(Math.max(this.mOverscanFrame.left - layoutContainingFrame.left, 0), Math.max(this.mOverscanFrame.top - layoutContainingFrame.top, 0), Math.max(layoutContainingFrame.right - this.mOverscanFrame.right, 0), Math.max(layoutContainingFrame.bottom - this.mOverscanFrame.bottom, 0));
                    }
                    if (this.mAttrs.type == 2034) {
                        WmDisplayCutout c = displayCutout.calculateRelativeTo(this.mDisplayFrame);
                        this.mTmpRect.set(this.mDisplayFrame);
                        this.mTmpRect.inset(c.getDisplayCutout().getSafeInsets());
                        this.mTmpRect.intersectUnchecked(this.mStableFrame);
                        this.mStableInsets.set(Math.max(this.mTmpRect.left - this.mDisplayFrame.left, 0), Math.max(this.mTmpRect.top - this.mDisplayFrame.top, 0), Math.max(this.mDisplayFrame.right - this.mTmpRect.right, 0), Math.max(this.mDisplayFrame.bottom - this.mTmpRect.bottom, 0));
                        this.mContentInsets.setEmpty();
                        this.mVisibleInsets.setEmpty();
                        displayCutout2 = WmDisplayCutout.NO_CUTOUT;
                        Rect rect5 = layoutContainingFrame;
                    } else {
                        int i4;
                        displayCutout2 = displayCutout;
                        getDisplayContent().getBounds(this.mTmpRect);
                        boolean overrideRightInset = (windowsAreFloating || inFullscreenContainer || this.mFrame.right <= this.mTmpRect.right) ? false : z;
                        if (windowsAreFloating || inFullscreenContainer || this.mFrame.bottom <= this.mTmpRect.bottom) {
                            z = false;
                        }
                        rect = this.mContentInsets;
                        fh = this.mContentFrame.left - this.mFrame.left;
                        i = this.mContentFrame.top - this.mFrame.top;
                        if (overrideRightInset) {
                            i2 = this.mTmpRect.right - this.mContentFrame.right;
                        } else {
                            i2 = this.mFrame.right - this.mContentFrame.right;
                        }
                        if (z) {
                            i4 = this.mTmpRect.bottom - this.mContentFrame.bottom;
                        } else {
                            i4 = this.mFrame.bottom - this.mContentFrame.bottom;
                        }
                        rect.set(fh, i, i2, i4);
                        layoutContainingFrame = this.mVisibleInsets;
                        i3 = this.mVisibleFrame.left - this.mFrame.left;
                        fh = this.mVisibleFrame.top - this.mFrame.top;
                        if (overrideRightInset) {
                            i = this.mTmpRect.right - this.mVisibleFrame.right;
                        } else {
                            i = this.mFrame.right - this.mVisibleFrame.right;
                        }
                        if (z) {
                            i2 = this.mTmpRect.bottom - this.mVisibleFrame.bottom;
                        } else {
                            i2 = this.mFrame.bottom - this.mVisibleFrame.bottom;
                        }
                        layoutContainingFrame.set(i3, fh, i, i2);
                        layoutContainingFrame = this.mStableInsets;
                        i3 = Math.max(this.mStableFrame.left - this.mFrame.left, 0);
                        i = Math.max(this.mStableFrame.top - this.mFrame.top, 0);
                        if (overrideRightInset) {
                            i2 = Math.max(this.mTmpRect.right - this.mStableFrame.right, 0);
                        } else {
                            i2 = Math.max(this.mFrame.right - this.mStableFrame.right, 0);
                        }
                        if (z) {
                            i4 = Math.max(this.mTmpRect.bottom - this.mStableFrame.bottom, 0);
                        } else {
                            i4 = Math.max(this.mFrame.bottom - this.mStableFrame.bottom, 0);
                        }
                        layoutContainingFrame.set(i3, i, i2, i4);
                        adjustVisibleInsetsInSplitMode();
                    }
                    this.mDisplayCutout = displayCutout2.calculateRelativeTo(this.mFrame);
                    this.mFrame.offset(-layoutXDiff, -layoutYDiff);
                    this.mCompatFrame.offset(-layoutXDiff, -layoutYDiff);
                    this.mContentFrame.offset(-layoutXDiff, -layoutYDiff);
                    this.mVisibleFrame.offset(-layoutXDiff, -layoutYDiff);
                    this.mStableFrame.offset(-layoutXDiff, -layoutYDiff);
                    this.mCompatFrame.set(this.mFrame);
                    if (this.mEnforceSizeCompat) {
                        this.mOverscanInsets.scale(this.mInvGlobalScale);
                        this.mContentInsets.scale(this.mInvGlobalScale);
                        this.mVisibleInsets.scale(this.mInvGlobalScale);
                        this.mStableInsets.scale(this.mInvGlobalScale);
                        this.mOutsets.scale(this.mInvGlobalScale);
                        this.mCompatFrame.scale(this.mInvGlobalScale);
                        if (!(!IS_NOTCH_PROP || this.mPolicy.getLayoutBeyondDisplayCutout() || this.mCompatFrame.width() == this.mCompatFrame.height())) {
                            frozen = this.mDisplayCutout.getDisplayCutout().getBounds().getBounds();
                            frozen.scale(this.mInvGlobalScale);
                            correctionCutoutRegion(this.mDisplayCutout.getDisplayCutout(), frozen);
                            Region tmpRegion = new Region();
                            tmpRegion.set(frozen);
                            this.mDisplayCutout = WmDisplayCutout.computeSafeInsets(DisplayCutout.fromBounds(tmpRegion.getBoundaryPath()), this.mCompatFrame.width(), this.mCompatFrame.height());
                        }
                        adjustContentsInsetsInCompatMode();
                    }
                    if (this.mIsWallpaper) {
                        if (fw != this.mFrame.width()) {
                        }
                        DisplayContent displayContent = getDisplayContent();
                        if (displayContent != null) {
                            DisplayInfo displayInfo = displayContent.getDisplayInfo();
                            getDisplayContent().mWallpaperController.updateWallpaperOffset(this, displayInfo.logicalWidth, displayInfo.logicalHeight, false);
                        }
                    }
                    if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Resolving window");
                        stringBuilder.append(this);
                        stringBuilder.append("(mRequestedWidth=");
                        stringBuilder.append(this.mRequestedWidth);
                        stringBuilder.append(", mRequestedheight=");
                        stringBuilder.append(this.mRequestedHeight);
                        stringBuilder.append(") to (pw=");
                        stringBuilder.append(pw2);
                        stringBuilder.append(", ph=");
                        stringBuilder.append(ph2);
                        stringBuilder.append("): frame=");
                        stringBuilder.append(this.mFrame.toShortString());
                        stringBuilder.append(" ci=");
                        stringBuilder.append(this.mContentInsets.toShortString());
                        stringBuilder.append(" vi=");
                        stringBuilder.append(this.mVisibleInsets.toShortString());
                        stringBuilder.append(" si=");
                        stringBuilder.append(this.mStableInsets.toShortString());
                        stringBuilder.append(" of=");
                        stringBuilder.append(this.mOutsets.toShortString());
                        Slog.v(str, stringBuilder.toString());
                    } else {
                        fh = ph2;
                        i3 = pw2;
                    }
                }
                Rect limitFrame = task2.inPinnedWindowingMode() ? this.mFrame : this.mContentFrame;
                ph = Math.min(this.mFrame.height(), limitFrame.height());
                i3 = Math.min(limitFrame.width(), this.mFrame.width());
                DisplayMetrics displayMetrics = getDisplayContent().getDisplayMetrics();
                i2 = Math.min(ph, WindowManagerService.dipToPixel(36, displayMetrics));
                int minVisibleWidth = Math.min(i3, WindowManagerService.dipToPixel(42, displayMetrics));
                fh2 = fh;
                fh = Math.max(limitFrame.top, Math.min(this.mFrame.top, limitFrame.bottom - i2));
                i = Math.max((limitFrame.left + minVisibleWidth) - i3, Math.min(this.mFrame.left, limitFrame.right - minVisibleWidth));
                this.mFrame.set(i, fh, i + i3, fh + ph);
                this.mContentFrame.set(this.mFrame);
                this.mVisibleFrame.set(this.mContentFrame);
                this.mStableFrame.set(this.mContentFrame);
                z = true;
                this.mOverscanInsets.set(Math.max(this.mOverscanFrame.left - layoutContainingFrame.left, 0), Math.max(this.mOverscanFrame.top - layoutContainingFrame.top, 0), Math.max(layoutContainingFrame.right - this.mOverscanFrame.right, 0), Math.max(layoutContainingFrame.bottom - this.mOverscanFrame.bottom, 0));
                if (this.mAttrs.type == 2034) {
                }
                this.mDisplayCutout = displayCutout2.calculateRelativeTo(this.mFrame);
                this.mFrame.offset(-layoutXDiff, -layoutYDiff);
                this.mCompatFrame.offset(-layoutXDiff, -layoutYDiff);
                this.mContentFrame.offset(-layoutXDiff, -layoutYDiff);
                this.mVisibleFrame.offset(-layoutXDiff, -layoutYDiff);
                this.mStableFrame.offset(-layoutXDiff, -layoutYDiff);
                this.mCompatFrame.set(this.mFrame);
                if (this.mEnforceSizeCompat) {
                }
                if (this.mIsWallpaper) {
                }
                if (WindowManagerDebugConfig.DEBUG_FOCUS) {
                }
            }
            this.mContainingFrame.set(rect);
            this.mDisplayFrame.set(rect2);
            frozen = rect2;
            layoutContainingFrame = rect;
            layoutXDiff = 0;
            layoutYDiff = 0;
            pw = this.mContainingFrame.width();
            ph = this.mContainingFrame.height();
            if (this.mParentFrame.equals(rect)) {
            }
            this.mLastRequestedWidth = this.mRequestedWidth;
            this.mLastRequestedHeight = this.mRequestedHeight;
            this.mContentChanged = true;
            this.mOverscanFrame.set(overscanFrame);
            this.mContentFrame.set(rect3);
            this.mVisibleFrame.set(visibleFrame);
            this.mDecorFrame.set(decorFrame);
            this.mStableFrame.set(stableFrame);
            if (rect4 == null) {
            }
            if (hasOutsets) {
            }
            fw = this.mFrame.width();
            fh = this.mFrame.height();
            applyGravityAndUpdateFrame(layoutContainingFrame, frozen);
            if (hasOutsets) {
            }
            if (windowsAreFloating) {
            }
            fh2 = fh;
            task = task2;
            if (this.mAttrs.type != 2034) {
            }
            this.mOverscanInsets.set(Math.max(this.mOverscanFrame.left - layoutContainingFrame.left, 0), Math.max(this.mOverscanFrame.top - layoutContainingFrame.top, 0), Math.max(layoutContainingFrame.right - this.mOverscanFrame.right, 0), Math.max(layoutContainingFrame.bottom - this.mOverscanFrame.bottom, 0));
            if (this.mAttrs.type == 2034) {
            }
            this.mDisplayCutout = displayCutout2.calculateRelativeTo(this.mFrame);
            this.mFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mCompatFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mContentFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mVisibleFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mStableFrame.offset(-layoutXDiff, -layoutYDiff);
            this.mCompatFrame.set(this.mFrame);
            if (this.mEnforceSizeCompat) {
            }
            if (this.mIsWallpaper) {
            }
            if (WindowManagerDebugConfig.DEBUG_FOCUS) {
            }
        }
    }

    private void adjustContentsInsetsInCompatMode() {
        if (this.mDecorTopCompensation == 1 && this.mContentInsets.top > 0) {
            Rect rect = this.mContentInsets;
            rect.top -= this.mDecorTopCompensation;
        }
        WindowState imeWin = this.mService.mInputMethodWindow;
        if (imeWin != null && imeWin.isVisibleNow() && this.mService.mInputMethodTarget == this && imeWin.isDisplayedLw() && this.mContentInsets.bottom != 0 && (this.mAttrs.softInputMode & 240) != 48 && this.mContentInsets.bottom >= this.mVisibleInsets.bottom) {
            Rect rect2 = this.mContentInsets;
            rect2.bottom--;
        }
    }

    private void adjustVisibleInsetsInSplitMode() {
        if (this.mService.isSplitMode() && !isInMultiWindowMode()) {
            this.mVisibleInsets.right = this.mTmpRect.right - this.mVisibleFrame.right;
        }
    }

    public Rect getBounds() {
        if (isInMultiWindowMode()) {
            return getTask().getBounds();
        }
        if (this.mAppToken != null) {
            return this.mAppToken.getBounds();
        }
        return super.getBounds();
    }

    public Rect getFrameLw() {
        return this.mFrame;
    }

    public Rect getDisplayFrameLw() {
        return this.mDisplayFrame;
    }

    public Rect getOverscanFrameLw() {
        return this.mOverscanFrame;
    }

    public Rect getContentFrameLw() {
        return this.mContentFrame;
    }

    public Rect getVisibleFrameLw() {
        return this.mVisibleFrame;
    }

    Rect getStableFrameLw() {
        return this.mStableFrame;
    }

    public boolean getGivenInsetsPendingLw() {
        return this.mGivenInsetsPending;
    }

    public Rect getGivenContentInsetsLw() {
        return this.mGivenContentInsets;
    }

    public Rect getGivenVisibleInsetsLw() {
        return this.mGivenVisibleInsets;
    }

    public LayoutParams getAttrs() {
        return this.mAttrs;
    }

    public boolean getNeedsMenuLw(com.android.server.policy.WindowManagerPolicy.WindowState bottom) {
        return getDisplayContent().getNeedsMenu(this, bottom);
    }

    public int getSystemUiVisibility() {
        return this.mSystemUiVisibility;
    }

    public int getSurfaceLayer() {
        return this.mLayer;
    }

    public int getBaseType() {
        return getTopParentWindow().mAttrs.type;
    }

    public IApplicationToken getAppToken() {
        return this.mAppToken != null ? this.mAppToken.appToken : null;
    }

    public boolean isVoiceInteraction() {
        return this.mAppToken != null && this.mAppToken.mVoiceInteraction;
    }

    boolean setReportResizeHints() {
        this.mOverscanInsetsChanged |= this.mLastOverscanInsets.equals(this.mOverscanInsets) ^ 1;
        this.mContentInsetsChanged |= this.mLastContentInsets.equals(this.mContentInsets) ^ 1;
        this.mVisibleInsetsChanged |= this.mLastVisibleInsets.equals(this.mVisibleInsets) ^ 1;
        this.mStableInsetsChanged |= this.mLastStableInsets.equals(this.mStableInsets) ^ 1;
        this.mOutsetsChanged |= this.mLastOutsets.equals(this.mOutsets) ^ 1;
        boolean z = this.mFrameSizeChanged;
        int i = (this.mLastFrame.width() == this.mFrame.width() && this.mLastFrame.height() == this.mFrame.height()) ? 0 : 1;
        this.mFrameSizeChanged = z | i;
        this.mDisplayCutoutChanged |= this.mLastDisplayCutout.equals(this.mDisplayCutout) ^ 1;
        if (this.mOverscanInsetsChanged || this.mContentInsetsChanged || this.mVisibleInsetsChanged || this.mOutsetsChanged || this.mFrameSizeChanged || this.mDisplayCutoutChanged) {
            return true;
        }
        return false;
    }

    void updateResizingWindowIfNeeded() {
        WindowStateAnimator winAnimator = this.mWinAnimator;
        if (this.mHasSurface && getDisplayContent().mLayoutSeq == this.mLayoutSeq && !isGoneForLayoutLw()) {
            Task task = getTask();
            if (task == null || !task.mStack.isAnimatingBounds()) {
                setReportResizeHints();
                boolean configChanged = isConfigChanged();
                if (WindowManagerDebugConfig.DEBUG_CONFIGURATION && configChanged) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Win ");
                    stringBuilder.append(this);
                    stringBuilder.append(" config changed: ");
                    stringBuilder.append(getConfiguration());
                    Slog.v(str, stringBuilder.toString());
                }
                boolean dragResizingChanged = isDragResizeChanged() && !isDragResizingChangeReported();
                this.mLastFrame.set(this.mFrame);
                StringBuilder stringBuilder2;
                if (this.mContentInsetsChanged || this.mVisibleInsetsChanged || this.mStableInsetsChanged || winAnimator.mSurfaceResized || this.mOutsetsChanged || this.mFrameSizeChanged || this.mDisplayCutoutChanged || configChanged || dragResizingChanged || this.mReportOrientationChanged) {
                    String str2;
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Resize reasons for w=");
                        stringBuilder2.append(this);
                        stringBuilder2.append(":  contentInsetsChanged=");
                        stringBuilder2.append(this.mContentInsetsChanged);
                        stringBuilder2.append(" ");
                        stringBuilder2.append(this.mContentInsets.toShortString());
                        stringBuilder2.append(" visibleInsetsChanged=");
                        stringBuilder2.append(this.mVisibleInsetsChanged);
                        stringBuilder2.append(" ");
                        stringBuilder2.append(this.mVisibleInsets.toShortString());
                        stringBuilder2.append(" stableInsetsChanged=");
                        stringBuilder2.append(this.mStableInsetsChanged);
                        stringBuilder2.append(" ");
                        stringBuilder2.append(this.mStableInsets.toShortString());
                        stringBuilder2.append(" outsetsChanged=");
                        stringBuilder2.append(this.mOutsetsChanged);
                        stringBuilder2.append(" ");
                        stringBuilder2.append(this.mOutsets.toShortString());
                        stringBuilder2.append(" surfaceResized=");
                        stringBuilder2.append(winAnimator.mSurfaceResized);
                        stringBuilder2.append(" configChanged=");
                        stringBuilder2.append(configChanged);
                        stringBuilder2.append(" dragResizingChanged=");
                        stringBuilder2.append(dragResizingChanged);
                        stringBuilder2.append(" reportOrientationChanged=");
                        stringBuilder2.append(this.mReportOrientationChanged);
                        stringBuilder2.append(" displayCutoutChanged=");
                        stringBuilder2.append(this.mDisplayCutoutChanged);
                        Slog.v(str2, stringBuilder2.toString());
                    }
                    if (this.mAppToken == null || !this.mAppDied) {
                        updateLastInsetValues();
                        this.mService.makeWindowFreezingScreenIfNeededLocked(this);
                        if (getOrientationChanging() || dragResizingChanged) {
                            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Orientation or resize start waiting for draw, mDrawState=DRAW_PENDING in ");
                                stringBuilder2.append(this);
                                stringBuilder2.append(", surfaceController ");
                                stringBuilder2.append(winAnimator.mSurfaceController);
                                Slog.v(str2, stringBuilder2.toString());
                            }
                            winAnimator.mDrawState = 1;
                            if (this.mAppToken != null) {
                                this.mAppToken.clearAllDrawn();
                            }
                        }
                        if (!this.mService.mResizingWindows.contains(this)) {
                            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                                str2 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Resizing window ");
                                stringBuilder3.append(this);
                                Slog.v(str2, stringBuilder3.toString());
                            }
                            this.mService.mResizingWindows.add(this);
                        }
                    } else {
                        this.mAppToken.removeDeadWindows();
                    }
                } else if (getOrientationChanging() && isDrawnLw()) {
                    if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                        String str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Orientation not waiting for draw in ");
                        stringBuilder2.append(this);
                        stringBuilder2.append(", surfaceController ");
                        stringBuilder2.append(winAnimator.mSurfaceController);
                        Slog.v(str3, stringBuilder2.toString());
                    }
                    setOrientationChanging(false);
                    this.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mService.mDisplayFreezeTime);
                }
            }
        }
    }

    boolean getOrientationChanging() {
        return ((!this.mOrientationChanging && (!isVisible() || getConfiguration().orientation == getLastReportedConfiguration().orientation)) || this.mSeamlesslyRotated || this.mOrientationChangeTimedOut) ? false : true;
    }

    void setOrientationChanging(boolean changing) {
        this.mOrientationChanging = changing;
        this.mOrientationChangeTimedOut = false;
    }

    void orientationChangeTimedOut() {
        this.mOrientationChangeTimedOut = true;
    }

    DisplayContent getDisplayContent() {
        return this.mToken.getDisplayContent();
    }

    void onDisplayChanged(DisplayContent dc) {
        super.onDisplayChanged(dc);
        if (dc != null) {
            this.mLayoutSeq = dc.mLayoutSeq - 1;
            this.mInputWindowHandle.displayId = dc.getDisplayId();
        }
    }

    DisplayInfo getDisplayInfo() {
        DisplayContent displayContent = getDisplayContent();
        return displayContent != null ? displayContent.getDisplayInfo() : null;
    }

    public int getDisplayId() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return -1;
        }
        return displayContent.getDisplayId();
    }

    Task getTask() {
        return this.mAppToken != null ? this.mAppToken.getTask() : null;
    }

    TaskStack getStack() {
        Task task = getTask();
        if (task != null && task.mStack != null) {
            return task.mStack;
        }
        DisplayContent dc = getDisplayContent();
        TaskStack homeStack = (this.mAttrs.type < IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || dc == null) ? null : dc.getHomeStack();
        return homeStack;
    }

    void getVisibleBounds(Rect bounds) {
        Task task = getTask();
        boolean intersectWithStackBounds = task != null && task.cropWindowsToStackBounds();
        bounds.setEmpty();
        this.mTmpRect.setEmpty();
        if (intersectWithStackBounds) {
            TaskStack stack = task.mStack;
            if (stack != null) {
                stack.getDimBounds(this.mTmpRect);
            } else {
                intersectWithStackBounds = false;
            }
        }
        bounds.set(this.mVisibleFrame);
        if (intersectWithStackBounds) {
            bounds.intersect(this.mTmpRect);
        }
        if (bounds.isEmpty()) {
            bounds.set(this.mFrame);
            if (intersectWithStackBounds) {
                bounds.intersect(this.mTmpRect);
            }
        }
    }

    public long getInputDispatchingTimeoutNanos() {
        if (this.mAppToken != null) {
            return this.mAppToken.mInputDispatchingTimeoutNanos;
        }
        return 5000000000L;
    }

    public boolean hasAppShownWindows() {
        return this.mAppToken != null && (this.mAppToken.firstWindowDrawn || this.mAppToken.startingDisplayed);
    }

    /* JADX WARNING: Missing block: B:19:0x0034, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:20:0x0035, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:21:0x0036, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean isIdentityMatrix(float dsdx, float dtdx, float dsdy, float dtdy) {
        if (dsdx < 0.99999f || dsdx > 1.00001f || dtdy < 0.99999f || dtdy > 1.00001f || dtdx < -1.0E-6f || dtdx > 1.0E-6f || dsdy < -1.0E-6f || dsdy > 1.0E-6f) {
            return false;
        }
        return true;
    }

    void prelayout() {
        if (this.mEnforceSizeCompat) {
            String pkgName = null;
            if (this.mAppToken != null) {
                pkgName = this.mAppToken.appPackageName;
            }
            if (this.mAppToken == null || pkgName == null || pkgName.isEmpty()) {
                pkgName = getOwningPackage();
            }
            IApsManager apsManager = (IApsManager) LocalServices.getService(IApsManager.class);
            float resolutionRatio = 0.0f;
            if (apsManager != null) {
                resolutionRatio = apsManager.getResolution(pkgName);
            }
            if (0.0f >= resolutionRatio || resolutionRatio >= 1.0f) {
                this.mGlobalScale = getDisplayContent().mCompatibleScreenScale;
                this.mInvGlobalScale = 1.0f / this.mGlobalScale;
                return;
            }
            this.mInvGlobalScale = resolutionRatio;
            if (Float.compare(0.6667f, resolutionRatio) == 0) {
                this.mGlobalScale = 1.5f;
                return;
            } else {
                this.mGlobalScale = 1.0f / this.mInvGlobalScale;
                return;
            }
        }
        this.mInvGlobalScale = 1.0f;
        this.mGlobalScale = 1.0f;
    }

    boolean hasContentToDisplay() {
        if (this.mAppFreezing || !isDrawnLw() || (this.mViewVisibility != 0 && (!this.mWinAnimator.isAnimationSet() || this.mService.mAppTransition.isTransitionSet()))) {
            return super.hasContentToDisplay();
        }
        return true;
    }

    boolean isVisible() {
        return wouldBeVisibleIfPolicyIgnored() && this.mPolicyVisibility;
    }

    boolean wouldBeVisibleIfPolicyIgnored() {
        return (!this.mHasSurface || isParentWindowHidden() || this.mAnimatingExit || this.mDestroying || (this.mIsWallpaper && !this.mWallpaperVisible)) ? false : true;
    }

    public boolean isVisibleLw() {
        return isVisible();
    }

    boolean isWinVisibleLw() {
        return (this.mAppToken == null || !this.mAppToken.hiddenRequested || this.mAppToken.isSelfAnimating()) && isVisible();
    }

    boolean isVisibleNow() {
        return (!this.mToken.isHidden() || this.mAttrs.type == 3) && isVisible();
    }

    boolean isPotentialDragTarget() {
        return (!isVisibleNow() || this.mRemoved || this.mInputChannel == null || this.mInputWindowHandle == null) ? false : true;
    }

    boolean isVisibleOrAdding() {
        AppWindowToken atoken = this.mAppToken;
        return (this.mHasSurface || (!this.mRelayoutCalled && this.mViewVisibility == 0)) && this.mPolicyVisibility && !isParentWindowHidden() && !((atoken != null && atoken.hiddenRequested) || this.mAnimatingExit || this.mDestroying);
    }

    boolean isOnScreen() {
        boolean z = false;
        if (!this.mHasSurface || this.mDestroying || !this.mPolicyVisibility) {
            return false;
        }
        AppWindowToken atoken = this.mAppToken;
        if (atoken != null) {
            if (!(isParentWindowHidden() || atoken.hiddenRequested) || this.mWinAnimator.isAnimationSet()) {
                z = true;
            }
            return z;
        }
        if (!isParentWindowHidden() || this.mWinAnimator.isAnimationSet()) {
            z = true;
        }
        return z;
    }

    boolean mightAffectAllDrawn() {
        boolean isAppType = this.mWinAnimator.mAttrType == 1 || this.mWinAnimator.mAttrType == 4;
        if ((!isOnScreen() && !isAppType) || this.mAnimatingExit || this.mDestroying) {
            return false;
        }
        return true;
    }

    boolean isInteresting() {
        return (this.mAppToken == null || this.mAppDied || (this.mAppToken.isFreezingScreen() && this.mAppFreezing)) ? false : true;
    }

    boolean isReadyForDisplay() {
        boolean z = false;
        if (this.mToken.waitingToShow && this.mService.mAppTransition.isTransitionSet()) {
            return false;
        }
        if (this.mHasSurface && this.mPolicyVisibility && !this.mDestroying && (!(isParentWindowHidden() || this.mViewVisibility != 0 || this.mToken.isHidden()) || this.mWinAnimator.isAnimationSet())) {
            z = true;
        }
        return z;
    }

    public boolean canAffectSystemUiFlags() {
        boolean z = true;
        if (this.mAttrs.alpha == 0.0f) {
            return false;
        }
        boolean exiting;
        if (this.mAppToken == null) {
            boolean shown = this.mWinAnimator.getShown();
            exiting = this.mAnimatingExit || this.mDestroying;
            if (!shown || exiting) {
                z = false;
            }
            return z;
        }
        Task task = getTask();
        exiting = task != null && task.canAffectSystemUiFlags();
        if (!exiting || this.mAppToken.isHidden()) {
            z = false;
        }
        return z;
    }

    public boolean isDisplayedLw() {
        AppWindowToken atoken = this.mAppToken;
        return isDrawnLw() && this.mPolicyVisibility && ((!isParentWindowHidden() && (atoken == null || !atoken.hiddenRequested)) || this.mWinAnimator.isAnimationSet());
    }

    public boolean isAnimatingLw() {
        return isAnimating();
    }

    public boolean isGoneForLayoutLw() {
        AppWindowToken atoken = this.mAppToken;
        return this.mViewVisibility == 8 || !this.mRelayoutCalled || ((atoken == null && this.mToken.isHidden()) || ((atoken != null && atoken.hiddenRequested) || isParentWindowHidden() || ((this.mAnimatingExit && !isAnimatingLw()) || this.mDestroying)));
    }

    public boolean isDrawFinishedLw() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 2 || this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    public boolean isDrawnLw() {
        return this.mHasSurface && !this.mDestroying && (this.mWinAnimator.mDrawState == 3 || this.mWinAnimator.mDrawState == 4);
    }

    private boolean isOpaqueDrawn() {
        return ((!this.mIsWallpaper && this.mAttrs.format == -1) || (this.mIsWallpaper && this.mWallpaperVisible)) && isDrawnLw() && !this.mWinAnimator.isAnimationSet();
    }

    void onMovedByResize() {
        this.mMovedByResize = true;
        super.onMovedByResize();
    }

    boolean onAppVisibilityChanged(boolean visible, boolean runningAppAnimation) {
        boolean changed = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            changed |= ((WindowState) this.mChildren.get(i)).onAppVisibilityChanged(visible, runningAppAnimation);
        }
        if (this.mAttrs.type == 3) {
            if (!visible && isVisibleNow() && this.mAppToken.isSelfAnimating()) {
                this.mAnimatingExit = true;
                this.mRemoveOnExit = true;
                this.mWindowRemovalAllowed = true;
            }
            return changed;
        }
        boolean isVisibleNow = isVisibleNow();
        if (visible != isVisibleNow) {
            if (!runningAppAnimation && isVisibleNow) {
                AccessibilityController accessibilityController = this.mService.mAccessibilityController;
                this.mWinAnimator.applyAnimationLocked(2, false);
                if (accessibilityController != null && getDisplayId() == 0) {
                    accessibilityController.onWindowTransitionLocked(this, 2);
                }
            }
            changed = true;
            setDisplayLayoutNeeded();
        }
        return changed;
    }

    boolean onSetAppExiting() {
        DisplayContent displayContent = getDisplayContent();
        boolean changed = false;
        if (isVisibleNow()) {
            this.mWinAnimator.applyAnimationLocked(2, false);
            if (this.mService.mAccessibilityController != null && isDefaultDisplay()) {
                this.mService.mAccessibilityController.onWindowTransitionLocked(this, 2);
            }
            changed = true;
            if (displayContent != null) {
                displayContent.setLayoutNeeded();
            }
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            changed |= ((WindowState) this.mChildren.get(i)).onSetAppExiting();
        }
        return changed;
    }

    void onResize() {
        ArrayList<WindowState> resizingWindows = this.mService.mResizingWindows;
        if (!(!this.mHasSurface || isGoneForLayoutLw() || resizingWindows.contains(this))) {
            resizingWindows.add(this);
        }
        if (isGoneForLayoutLw()) {
            this.mResizedWhileGone = true;
        }
        super.onResize();
    }

    void onUnfreezeBounds() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).onUnfreezeBounds();
        }
        if (this.mHasSurface) {
            this.mLayoutNeeded = true;
            setDisplayLayoutNeeded();
            if (!this.mService.mResizingWindows.contains(this)) {
                this.mService.mResizingWindows.add(this);
            }
        }
    }

    void handleWindowMovedIfNeeded() {
        if (hasMoved()) {
            int left = this.mFrame.left;
            int top = this.mFrame.top;
            Task task = getTask();
            boolean adjustedForMinimizedDockOrIme = task != null && (task.mStack.isAdjustedForMinimizedDockedStack() || task.mStack.isAdjustedForIme());
            if (!(!this.mToken.okToAnimate() || (this.mAttrs.privateFlags & 64) != 0 || isDragResizing() || adjustedForMinimizedDockOrIme || !getWindowConfiguration().hasMovementAnimations() || this.mWinAnimator.mLastHidden || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(getDisplayId())))) {
                startMoveAnimation(left, top);
            }
            if (this.mService.mAccessibilityController != null && getDisplayContent().getDisplayId() == 0) {
                this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
            }
            try {
                this.mClient.moved(left, top);
            } catch (RemoteException e) {
            }
            this.mMovedByResize = false;
        }
    }

    private boolean hasMoved() {
        return this.mHasSurface && !((!this.mContentChanged && !this.mMovedByResize) || this.mAnimatingExit || ((this.mFrame.top == this.mLastFrame.top && this.mFrame.left == this.mLastFrame.left) || (this.mIsChildWindow && getParentWindow().hasMoved())));
    }

    boolean isObscuringDisplay() {
        Task task = getTask();
        boolean z = false;
        if (task != null && task.mStack != null && !task.mStack.fillsParent()) {
            return false;
        }
        if (isOpaqueDrawn() && fillsDisplay()) {
            z = true;
        }
        return z;
    }

    boolean fillsDisplay() {
        DisplayInfo displayInfo = getDisplayInfo();
        return this.mFrame.left <= 0 && this.mFrame.top <= 0 && this.mFrame.right >= displayInfo.appWidth && this.mFrame.bottom >= displayInfo.appHeight;
    }

    boolean isConfigChanged() {
        return getLastReportedConfiguration().equals(getConfiguration()) ^ 1;
    }

    void onWindowReplacementTimeout() {
        if (this.mWillReplaceWindow) {
            removeImmediately();
            return;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).onWindowReplacementTimeout();
        }
    }

    void forceWindowsScaleableInTransaction(boolean force) {
        if (this.mWinAnimator != null && this.mWinAnimator.hasSurface()) {
            this.mWinAnimator.mSurfaceController.forceScaleableInTransaction(force);
        }
        super.forceWindowsScaleableInTransaction(force);
    }

    void removeImmediately() {
        super.removeImmediately();
        if (this.mRemoved) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WS.removeImmediately: ");
            stringBuilder.append(this);
            stringBuilder.append(" Already removed...");
            Slog.v(str, stringBuilder.toString());
            return;
        }
        this.mRemoved = true;
        this.mWillReplaceWindow = false;
        if (this.mReplacementWindow != null) {
            this.mReplacementWindow.mSkipEnterAnimationForSeamlessReplacement = false;
        }
        DisplayContent dc = getDisplayContent();
        if (isInputMethodTarget()) {
            dc.computeImeTarget(true);
        }
        if (WindowManagerService.excludeWindowTypeFromTapOutTask(this.mAttrs.type) || WindowManagerService.excludeWindowsFromTapOutTask(this)) {
            dc.mTapExcludedWindows.remove(this);
        }
        if (this.mTapExcludeRegionHolder != null) {
            dc.mTapExcludeProvidingWindows.remove(this);
        }
        this.mPolicy.removeWindowLw(this);
        disposeInputChannel();
        this.mWinAnimator.destroyDeferredSurfaceLocked();
        this.mWinAnimator.destroySurfaceLocked();
        this.mSession.windowRemovedLocked();
        try {
            this.mClient.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
        } catch (RuntimeException e) {
        }
        this.mService.postWindowRemoveCleanupLocked(this);
    }

    void removeIfPossible() {
        super.removeIfPossible();
        removeIfPossible(false);
    }

    private void removeIfPossible(boolean keepVisibleDeadWindow) {
        String str;
        this.mWindowRemovalAllowed = true;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeIfPossible: ");
        stringBuilder.append(this);
        Slog.v(str2, stringBuilder.toString());
        boolean startingWindow = this.mAttrs.type == 3;
        int transit = 5;
        if (WindowManagerDebugConfig.DEBUG_FOCUS || (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT && this == this.mService.mCurrentFocus)) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Remove ");
            stringBuilder2.append(this);
            stringBuilder2.append(" client=");
            stringBuilder2.append(Integer.toHexString(System.identityHashCode(this.mClient.asBinder())));
            stringBuilder2.append(", surfaceController=");
            stringBuilder2.append(this.mWinAnimator.mSurfaceController);
            stringBuilder2.append(" Callers=");
            stringBuilder2.append(Debug.getCallers(5));
            Slog.v(str, stringBuilder2.toString());
        }
        long origId = Binder.clearCallingIdentity();
        disposeInputChannel();
        if (WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Remove ");
            stringBuilder3.append(this);
            stringBuilder3.append(": mSurfaceController=");
            stringBuilder3.append(this.mWinAnimator.mSurfaceController);
            stringBuilder3.append(" mAnimatingExit=");
            stringBuilder3.append(this.mAnimatingExit);
            stringBuilder3.append(" mRemoveOnExit=");
            stringBuilder3.append(this.mRemoveOnExit);
            stringBuilder3.append(" mHasSurface=");
            stringBuilder3.append(this.mHasSurface);
            stringBuilder3.append(" surfaceShowing=");
            stringBuilder3.append(this.mWinAnimator.getShown());
            stringBuilder3.append(" isAnimationSet=");
            stringBuilder3.append(this.mWinAnimator.isAnimationSet());
            stringBuilder3.append(" app-animation=");
            stringBuilder3.append(this.mAppToken != null ? Boolean.valueOf(this.mAppToken.isSelfAnimating()) : "false");
            stringBuilder3.append(" mWillReplaceWindow=");
            stringBuilder3.append(this.mWillReplaceWindow);
            stringBuilder3.append(" inPendingTransaction=");
            stringBuilder3.append(this.mAppToken != null ? this.mAppToken.inPendingTransaction : false);
            stringBuilder3.append(" mDisplayFrozen=");
            stringBuilder3.append(this.mService.mDisplayFrozen);
            stringBuilder3.append(" callers=");
            stringBuilder3.append(Debug.getCallers(6));
            Slog.v(str, stringBuilder3.toString());
        }
        boolean wasVisible = false;
        int displayId = getDisplayId();
        if (this.mHasSurface && this.mToken.okToAnimate()) {
            String str3;
            StringBuilder stringBuilder4;
            if (this.mWillReplaceWindow) {
                str3 = TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Preserving ");
                stringBuilder4.append(this);
                stringBuilder4.append(" until the new one is added");
                Slog.v(str3, stringBuilder4.toString());
                this.mAnimatingExit = true;
                this.mReplacingRemoveRequested = true;
                return;
            }
            wasVisible = isWinVisibleLw();
            if (keepVisibleDeadWindow) {
                str3 = TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Not removing ");
                stringBuilder4.append(this);
                stringBuilder4.append(" because app died while it's visible");
                Slog.v(str3, stringBuilder4.toString());
                this.mAppDied = true;
                setDisplayLayoutNeeded();
                this.mService.mWindowPlacerLocked.performSurfacePlacement();
                openInputChannel(null);
                this.mService.mInputMonitor.updateInputWindowsLw(true);
                Binder.restoreCallingIdentity(origId);
                return;
            }
            if (wasVisible) {
                if (!startingWindow) {
                    transit = 2;
                }
                try {
                    if (this.mWinAnimator.applyAnimationLocked(transit, false)) {
                        this.mAnimatingExit = true;
                        setDisplayLayoutNeeded();
                        this.mService.requestTraversal();
                    }
                    if (this.mService.mAccessibilityController != null && displayId == 0) {
                        this.mService.mAccessibilityController.onWindowTransitionLocked(this, transit);
                    }
                } finally {
                    Binder.restoreCallingIdentity(origId);
                }
            }
            if (this.isBinderDiedCalling && HwPCUtils.enabled() && getDisplayId() != 0) {
                this.isBinderDiedCalling = false;
                this.mAnimatingExit = false;
            }
            boolean isAnimating = this.mWinAnimator.isAnimationSet() && (this.mAppToken == null || !this.mAppToken.isWaitingForTransitionStart());
            boolean lastWindowIsStartingWindow = startingWindow && this.mAppToken != null && this.mAppToken.isLastWindow(this);
            if (this.mWinAnimator.getShown() && this.mAnimatingExit && (!lastWindowIsStartingWindow || isAnimating)) {
                String str4 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Not removing ");
                stringBuilder.append(this);
                stringBuilder.append(" due to exit animation ");
                Slog.v(str4, stringBuilder.toString());
                setupWindowForRemoveOnExit();
                if (this.mAppToken != null) {
                    this.mAppToken.updateReportedVisibilityLocked();
                }
                Binder.restoreCallingIdentity(origId);
                return;
            }
        }
        removeImmediately();
        if (wasVisible && this.mService.updateOrientationFromAppTokensLocked(displayId)) {
            this.mService.mH.obtainMessage(18, Integer.valueOf(displayId)).sendToTarget();
        }
        if (this.mLastTitle == null || !this.mLastTitle.toString().contains("Emui:ProximityWnd")) {
            this.mService.updateFocusedWindowLocked(0, true);
        } else {
            this.mService.updateFocusedWindowLocked(2, true);
        }
        Binder.restoreCallingIdentity(origId);
    }

    private void setupWindowForRemoveOnExit() {
        this.mRemoveOnExit = true;
        setDisplayLayoutNeeded();
        boolean focusChanged = this.mService.updateFocusedWindowLocked(3, false);
        this.mService.mWindowPlacerLocked.performSurfacePlacement();
        if (focusChanged) {
            this.mService.mInputMonitor.updateInputWindowsLw(false);
        }
    }

    void setHasSurface(boolean hasSurface) {
        this.mHasSurface = hasSurface;
    }

    boolean canBeImeTarget() {
        if (this.mIsImWindow) {
            return false;
        }
        boolean windowsAreFocusable = this.mAppToken == null || this.mAppToken.windowsAreFocusable();
        if (!windowsAreFocusable) {
            return false;
        }
        int fl = this.mAttrs.flags & 131080;
        int type = this.mAttrs.type;
        if (fl != 0 && fl != 131080 && type != 3) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isVisibleOrAdding ");
            stringBuilder.append(this);
            stringBuilder.append(": ");
            stringBuilder.append(isVisibleOrAdding());
            Slog.i(str, stringBuilder.toString());
            if (!isVisibleOrAdding()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mSurfaceController=");
                stringBuilder.append(this.mWinAnimator.mSurfaceController);
                stringBuilder.append(" relayoutCalled=");
                stringBuilder.append(this.mRelayoutCalled);
                stringBuilder.append(" viewVis=");
                stringBuilder.append(this.mViewVisibility);
                stringBuilder.append(" policyVis=");
                stringBuilder.append(this.mPolicyVisibility);
                stringBuilder.append(" policyVisAfterAnim=");
                stringBuilder.append(this.mPolicyVisibilityAfterAnim);
                stringBuilder.append(" parentHidden=");
                stringBuilder.append(isParentWindowHidden());
                stringBuilder.append(" exiting=");
                stringBuilder.append(this.mAnimatingExit);
                stringBuilder.append(" destroying=");
                stringBuilder.append(this.mDestroying);
                Slog.i(str, stringBuilder.toString());
                if (this.mAppToken != null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  mAppToken.hiddenRequested=");
                    stringBuilder.append(this.mAppToken.hiddenRequested);
                    Slog.i(str, stringBuilder.toString());
                }
            }
        }
        return isVisibleOrAdding();
    }

    void openInputChannel(InputChannel outInputChannel) {
        if (this.mInputChannel == null) {
            InputChannel[] inputChannels = InputChannel.openInputChannelPair(getName());
            this.mInputChannel = inputChannels[0];
            this.mClientChannel = inputChannels[1];
            this.mInputWindowHandle.inputChannel = inputChannels[0];
            if (outInputChannel != null) {
                this.mClientChannel.transferTo(outInputChannel);
                this.mClientChannel.dispose();
                this.mClientChannel = null;
            } else {
                this.mDeadWindowEventReceiver = new DeadWindowEventReceiver(this.mClientChannel);
            }
            this.mService.mInputManager.registerInputChannel(this.mInputChannel, this.mInputWindowHandle);
            return;
        }
        throw new IllegalStateException("Window already has an input channel.");
    }

    void disposeInputChannel() {
        if (this.mDeadWindowEventReceiver != null) {
            this.mDeadWindowEventReceiver.dispose();
            this.mDeadWindowEventReceiver = null;
        }
        if (this.mInputChannel != null) {
            this.mService.mInputManager.unregisterInputChannel(this.mInputChannel);
            this.mInputChannel.dispose();
            this.mInputChannel = null;
        }
        if (this.mClientChannel != null) {
            this.mClientChannel.dispose();
            this.mClientChannel = null;
        }
        this.mInputWindowHandle.inputChannel = null;
    }

    boolean removeReplacedWindowIfNeeded(WindowState replacement) {
        if (this.mWillReplaceWindow && this.mReplacementWindow == replacement && replacement.hasDrawnLw()) {
            replacement.mSkipEnterAnimationForSeamlessReplacement = false;
            removeReplacedWindow();
            return true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowState) this.mChildren.get(i)).removeReplacedWindowIfNeeded(replacement)) {
                return true;
            }
        }
        return false;
    }

    private void removeReplacedWindow() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Removing replaced window: ");
        stringBuilder.append(this);
        Slog.d(str, stringBuilder.toString());
        this.mWillReplaceWindow = false;
        this.mAnimateReplacingWindow = false;
        this.mReplacingRemoveRequested = false;
        this.mReplacementWindow = null;
        if (this.mAnimatingExit || !this.mAnimateReplacingWindow) {
            removeImmediately();
        }
    }

    boolean setReplacementWindowIfNeeded(WindowState replacementCandidate) {
        boolean replacementSet = false;
        if (this.mWillReplaceWindow && this.mReplacementWindow == null && getWindowTag().toString().equals(replacementCandidate.getWindowTag().toString())) {
            this.mReplacementWindow = replacementCandidate;
            replacementCandidate.mSkipEnterAnimationForSeamlessReplacement = this.mAnimateReplacingWindow ^ 1;
            replacementSet = true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            replacementSet |= ((WindowState) this.mChildren.get(i)).setReplacementWindowIfNeeded(replacementCandidate);
        }
        return replacementSet;
    }

    void setDisplayLayoutNeeded() {
        DisplayContent dc = getDisplayContent();
        if (dc != null) {
            dc.setLayoutNeeded();
        }
    }

    void applyAdjustForImeIfNeeded() {
        Task task = getTask();
        if (task != null && task.mStack != null && task.mStack.isAdjustedForIme()) {
            task.mStack.applyAdjustForImeIfNeeded(task);
        }
    }

    void switchUser() {
        super.switchUser();
        if (isHiddenFromUserLocked()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("user changing, hiding ");
                stringBuilder.append(this);
                stringBuilder.append(", attrs=");
                stringBuilder.append(this.mAttrs.type);
                stringBuilder.append(", belonging to ");
                stringBuilder.append(this.mOwnerUid);
                Slog.w(str, stringBuilder.toString());
            }
            hideLw(false);
        }
    }

    int getTouchableRegion(Region region, int flags) {
        if (!((flags & 40) == 0) || this.mAppToken == null) {
            getTouchableRegion(region);
        } else {
            flags |= 32;
            Task task = getTask();
            if (task != null) {
                task.getDimBounds(this.mTmpRect);
            } else {
                getStack().getDimBounds(this.mTmpRect);
            }
            if (inFreeformWindowingMode() || inHwPCFreeformWindowingMode()) {
                int delta = WindowManagerService.dipToPixel(10, getDisplayContent().getDisplayMetrics());
                this.mTmpRect.inset(-delta, -delta);
            }
            region.set(this.mTmpRect);
            cropRegionToStackBoundsIfNeeded(region);
        }
        return flags;
    }

    void checkPolicyVisibilityChange() {
        if (this.mPolicyVisibility != this.mPolicyVisibilityAfterAnim) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Policy visibility changing after anim in ");
                stringBuilder.append(this.mWinAnimator);
                stringBuilder.append(": ");
                stringBuilder.append(this.mPolicyVisibilityAfterAnim);
                Slog.v(str, stringBuilder.toString());
            }
            this.mPolicyVisibility = this.mPolicyVisibilityAfterAnim;
            if (!this.mPolicyVisibility) {
                this.mWinAnimator.hide("checkPolicyVisibilityChange");
                if (this.mService.mCurrentFocus == this) {
                    if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                        Slog.i(TAG, "setAnimationLocked: setting mFocusMayChange true");
                    }
                    this.mService.mFocusMayChange = true;
                }
                setDisplayLayoutNeeded();
                this.mService.enableScreenIfNeededLocked();
            }
        }
    }

    void setRequestedSize(int requestedWidth, int requestedHeight) {
        if (this.mRequestedWidth != requestedWidth || this.mRequestedHeight != requestedHeight) {
            this.mLayoutNeeded = true;
            this.mRequestedWidth = requestedWidth;
            this.mRequestedHeight = requestedHeight;
        }
    }

    void prepareWindowToDisplayDuringRelayout(boolean wasVisible) {
        boolean hasTurnScreenOnFlag = (this.mAttrs.flags & DumpState.DUMP_COMPILER_STATS) != 0;
        boolean allowTheaterMode = this.mService.mAllowTheaterModeWakeFromLayout || Global.getInt(this.mService.mContext.getContentResolver(), "theater_mode_on", 0) == 0;
        boolean canTurnScreenOn = this.mAppToken == null || this.mAppToken.canTurnScreenOn() || (toString().contains("com.android.incallui/com.android.incallui.InCallActivity") && this.mViewVisibility == 0);
        if (hasTurnScreenOnFlag) {
            if (allowTheaterMode && canTurnScreenOn && !this.mPowerManagerWrapper.isInteractive()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Relayout window turning screen on: ");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
                this.mPowerManagerWrapper.wakeUp(SystemClock.uptimeMillis(), "android.server.wm:TURN_ON");
            }
            if (this.mAppToken != null) {
                this.mAppToken.setCanTurnScreenOn(false);
            }
        }
        if (wasVisible) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Already visible and does not turn on screen, skip preparing: ");
            stringBuilder2.append(this);
            Flog.i(307, stringBuilder2.toString());
            return;
        }
        if ((this.mAttrs.softInputMode & 240) == 16) {
            this.mLayoutNeeded = true;
        }
        if (isDrawnLw() && this.mToken.okToAnimate()) {
            this.mWinAnimator.applyEnterAnimationLocked();
        }
    }

    void getMergedConfiguration(MergedConfiguration outConfiguration) {
        outConfiguration.setConfiguration(this.mService.mRoot.getConfiguration(), getMergedOverrideConfiguration());
        if (this.mService.mHwWMSEx != null) {
            this.mService.mHwWMSEx.onChangeConfiguration(outConfiguration, this);
        }
    }

    void setLastReportedMergedConfiguration(MergedConfiguration config) {
        this.mLastReportedConfiguration.setTo(config);
    }

    void getLastReportedMergedConfiguration(MergedConfiguration config) {
        config.setTo(this.mLastReportedConfiguration);
    }

    private Configuration getLastReportedConfiguration() {
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(getDisplayId()) && this.mAppToken == null) {
            return getConfiguration();
        }
        return this.mLastReportedConfiguration.getMergedConfiguration();
    }

    void adjustStartingWindowFlags() {
        if (this.mAttrs.type == 1 && this.mAppToken != null && this.mAppToken.startingWindow != null) {
            LayoutParams sa = this.mAppToken.startingWindow.mAttrs;
            sa.flags = (sa.flags & -4718594) | (this.mAttrs.flags & 4718593);
        }
    }

    void setWindowScale(int requestedWidth, int requestedHeight) {
        float f = 1.0f;
        if ((this.mAttrs.flags & 16384) != 0) {
            this.mHScale = this.mAttrs.width != requestedWidth ? ((float) this.mAttrs.width) / ((float) requestedWidth) : 1.0f;
            if (this.mAttrs.height != requestedHeight) {
                f = ((float) this.mAttrs.height) / ((float) requestedHeight);
            }
            this.mVScale = f;
            return;
        }
        this.mVScale = 1.0f;
        this.mHScale = 1.0f;
    }

    /* JADX WARNING: Missing block: B:14:0x0032, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean shouldKeepVisibleDeadAppWindow() {
        if (!isWinVisibleLw() || this.mAppToken == null || this.mAppToken.isClientHidden() || this.mAttrs.token != this.mClient.asBinder() || this.mAttrs.type == 3) {
            return false;
        }
        return getWindowConfiguration().keepVisibleDeadAppWindowOnScreen();
    }

    boolean canReceiveKeys() {
        return isVisibleOrAdding() && this.mViewVisibility == 0 && !this.mRemoveOnExit && (this.mAttrs.flags & 8) == 0 && ((this.mAppToken == null || this.mAppToken.windowsAreFocusable()) && !canReceiveTouchInput());
    }

    boolean canReceiveTouchInput() {
        return (this.mAppToken == null || this.mAppToken.getTask() == null || !this.mAppToken.getTask().mStack.shouldIgnoreInput()) ? false : true;
    }

    public boolean hasDrawnLw() {
        return this.mWinAnimator.mDrawState == 4;
    }

    public boolean showLw(boolean doAnimation) {
        return showLw(doAnimation, true);
    }

    boolean showLw(boolean doAnimation, boolean requestAnim) {
        if (isHiddenFromUserLocked() || !this.mAppOpVisibility || this.mPermanentlyHidden || this.mHiddenWhileSuspended || this.mForceHideNonSystemOverlayWindow) {
            return false;
        }
        if (this.mPolicyVisibility && this.mPolicyVisibilityAfterAnim) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Policy visibility true: ");
            stringBuilder.append(this);
            Slog.v(str, stringBuilder.toString());
        }
        if (doAnimation) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("doAnimation: mPolicyVisibility=");
                stringBuilder.append(this.mPolicyVisibility);
                stringBuilder.append(" isAnimationSet=");
                stringBuilder.append(this.mWinAnimator.isAnimationSet());
                Slog.v(str, stringBuilder.toString());
            }
            if (!this.mToken.okToAnimate()) {
                doAnimation = false;
            } else if (this.mPolicyVisibility && !this.mWinAnimator.isAnimationSet()) {
                doAnimation = false;
            }
        }
        this.mPolicyVisibility = true;
        this.mPolicyVisibilityAfterAnim = true;
        if (doAnimation) {
            this.mWinAnimator.applyAnimationLocked(1, true);
        }
        if (requestAnim) {
            this.mService.scheduleAnimationLocked();
        }
        if ((this.mAttrs.flags & 8) == 0) {
            this.mService.updateFocusedWindowLocked(0, false);
        }
        return true;
    }

    public boolean hideLw(boolean doAnimation) {
        return hideLw(doAnimation, true);
    }

    boolean hideLw(boolean doAnimation, boolean requestAnim) {
        if (doAnimation && !this.mToken.okToAnimate()) {
            doAnimation = false;
        }
        if (!(doAnimation ? this.mPolicyVisibilityAfterAnim : this.mPolicyVisibility)) {
            return false;
        }
        if (doAnimation) {
            this.mWinAnimator.applyAnimationLocked(2, false);
            if (!this.mWinAnimator.isAnimationSet()) {
                doAnimation = false;
            }
        }
        this.mPolicyVisibilityAfterAnim = false;
        if (!doAnimation) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Policy visibility false: ");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            this.mPolicyVisibility = false;
            this.mService.enableScreenIfNeededLocked();
            if (this.mService.mCurrentFocus == this) {
                if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                    Slog.i(TAG, "WindowState.hideLw: setting mFocusMayChange true");
                }
                this.mService.mFocusMayChange = true;
            }
        }
        if (requestAnim) {
            this.mService.scheduleAnimationLocked();
        }
        if (this.mService.mCurrentFocus == this) {
            this.mService.updateFocusedWindowLocked(0, false);
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:14:0x0029, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void setForceHideNonSystemOverlayWindowIfNeeded(boolean forceHide) {
        if (!this.mOwnerCanAddInternalSystemWindow && ((LayoutParams.isSystemAlertWindowType(this.mAttrs.type) || this.mAttrs.type == 2005) && this.mForceHideNonSystemOverlayWindow != forceHide)) {
            this.mForceHideNonSystemOverlayWindow = forceHide;
            if (forceHide) {
                hideLw(true, true);
            } else {
                showLw(true, true);
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0029, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void setHiddenWhileSuspended(boolean hide) {
        if (!this.mOwnerCanAddInternalSystemWindow && ((LayoutParams.isSystemAlertWindowType(this.mAttrs.type) || this.mAttrs.type == 2005) && this.mHiddenWhileSuspended != hide)) {
            this.mHiddenWhileSuspended = hide;
            if (hide) {
                hideLw(true, true);
            } else {
                showLw(true, true);
            }
        }
    }

    public void setAppOpVisibilityLw(boolean state) {
        if (this.mAppOpVisibility != state) {
            this.mAppOpVisibility = state;
            if (state) {
                showLw(true, true);
            } else {
                hideLw(true, true);
            }
        }
    }

    void initAppOpsState() {
        if (this.mAppOp != -1 && this.mAppOpVisibility) {
            int mode = this.mService.mAppOps.startOpNoThrow(this.mAppOp, getOwningUid(), getOwningPackage(), true);
            if (!(mode == 0 || mode == 3)) {
                this.mService.mHwWMSEx.setAppOpHideHook(this, false);
            }
            this.mService.mHwWMSEx.addWindowReport(this, mode);
            this.mService.mHwWMSEx.setVisibleFromParent(this);
        }
    }

    void resetAppOpsState() {
        if (this.mAppOp != -1 && this.mAppOpVisibility) {
            this.mService.mAppOps.finishOp(this.mAppOp, getOwningUid(), getOwningPackage());
        }
    }

    void updateAppOpsState() {
        if (this.mAppOp != -1) {
            int uid = getOwningUid();
            String packageName = getOwningPackage();
            int mode;
            if (this.mAppOpVisibility) {
                mode = this.mService.mAppOps.checkOpNoThrow(this.mAppOp, uid, packageName);
                if (!(mode == 0 || mode == 3)) {
                    this.mService.mAppOps.finishOp(this.mAppOp, uid, packageName);
                    setAppOpVisibilityLw(false);
                }
            } else {
                mode = this.mService.mAppOps.startOpNoThrow(this.mAppOp, uid, packageName, true);
                if (mode == 0 || mode == 3) {
                    setAppOpVisibilityLw(true);
                }
            }
        }
    }

    public void hidePermanentlyLw() {
        if (!this.mPermanentlyHidden) {
            this.mPermanentlyHidden = true;
            hideLw(true, true);
            HwServiceFactory.reportToastHiddenToIAware(this.mSession.mPid, System.identityHashCode(this));
        }
    }

    public void pokeDrawLockLw(long timeout) {
        if (isVisibleOrAdding()) {
            if (this.mDrawLock == null) {
                CharSequence tag = getWindowTag();
                PowerManager powerManager = this.mService.mPowerManager;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Window:");
                stringBuilder.append(tag);
                this.mDrawLock = powerManager.newWakeLock(128, stringBuilder.toString());
                this.mDrawLock.setReferenceCounted(false);
                this.mDrawLock.setWorkSource(new WorkSource(this.mOwnerUid, this.mAttrs.packageName));
            }
            this.mDrawLock.acquire(timeout);
        }
    }

    public boolean isAlive() {
        return this.mClient.asBinder().isBinderAlive();
    }

    boolean isClosing() {
        return this.mAnimatingExit || (this.mAppToken != null && this.mAppToken.isClosingOrEnteringPip());
    }

    void addWinAnimatorToList(ArrayList<WindowStateAnimator> animators) {
        animators.add(this.mWinAnimator);
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).addWinAnimatorToList(animators);
        }
    }

    void sendAppVisibilityToClients() {
        super.sendAppVisibilityToClients();
        boolean clientHidden = this.mAppToken.isClientHidden();
        if (this.mAttrs.type != 3 || !clientHidden) {
            if (clientHidden) {
                for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                    ((WindowState) this.mChildren.get(i)).mWinAnimator.detachChildren();
                }
                this.mWinAnimator.detachChildren();
            }
            try {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Setting visibility of ");
                    stringBuilder.append(this);
                    stringBuilder.append(": ");
                    stringBuilder.append(clientHidden ^ 1);
                    Slog.v(str, stringBuilder.toString());
                }
                this.mClient.dispatchAppVisibility(clientHidden ^ 1);
            } catch (RemoteException e) {
            }
        }
    }

    void onStartFreezingScreen() {
        this.mAppFreezing = true;
        int i = this.mChildren.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                ((WindowState) this.mChildren.get(i2)).onStartFreezingScreen();
                i = i2 - 1;
            } else {
                return;
            }
        }
    }

    boolean onStopFreezingScreen() {
        boolean unfrozeWindows = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            unfrozeWindows |= ((WindowState) this.mChildren.get(i)).onStopFreezingScreen();
        }
        if (!this.mAppFreezing) {
            return unfrozeWindows;
        }
        this.mAppFreezing = false;
        if (!(!this.mHasSurface || getOrientationChanging() || this.mService.mWindowsFreezingScreen == 2)) {
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set mOrientationChanging of ");
                stringBuilder.append(this);
                Slog.v(str, stringBuilder.toString());
            }
            setOrientationChanging(true);
            this.mService.mRoot.mOrientationChangeComplete = false;
        }
        this.mLastFreezeDuration = 0;
        setDisplayLayoutNeeded();
        return true;
    }

    boolean destroySurface(boolean cleanupOnResume, boolean appStopped) {
        boolean destroyedSomething = false;
        ArrayList<WindowState> childWindows = new ArrayList(this.mChildren);
        for (int i = childWindows.size() - 1; i >= 0; i--) {
            destroyedSomething |= ((WindowState) childWindows.get(i)).destroySurface(cleanupOnResume, appStopped);
        }
        if (!appStopped && !this.mWindowRemovalAllowed && !cleanupOnResume && !"com.touchtype.swiftkey".equals(getOwningPackage())) {
            return destroyedSomething;
        }
        if (appStopped || this.mWindowRemovalAllowed) {
            this.mWinAnimator.destroyPreservedSurfaceLocked();
        }
        if (this.mDestroying) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("win=");
            stringBuilder.append(this);
            stringBuilder.append(" destroySurfaces: appStopped=");
            stringBuilder.append(appStopped);
            stringBuilder.append(" win.mWindowRemovalAllowed=");
            stringBuilder.append(this.mWindowRemovalAllowed);
            stringBuilder.append(" win.mRemoveOnExit=");
            stringBuilder.append(this.mRemoveOnExit);
            Slog.e(str, stringBuilder.toString());
            if (!cleanupOnResume || this.mRemoveOnExit) {
                destroySurfaceUnchecked();
            }
            if (this.mRemoveOnExit) {
                removeImmediately();
            }
            if (cleanupOnResume) {
                requestUpdateWallpaperIfNeeded();
            }
            this.mDestroying = false;
            destroyedSomething = true;
        }
        return destroyedSomething;
    }

    void destroySurfaceUnchecked() {
        this.mWinAnimator.destroySurfaceLocked();
        this.mAnimatingExit = false;
    }

    public boolean isDefaultDisplay() {
        DisplayContent displayContent = getDisplayContent();
        if (displayContent == null) {
            return false;
        }
        return displayContent.isDefaultDisplay;
    }

    void setShowToOwnerOnlyLocked(boolean showToOwnerOnly) {
        this.mShowToOwnerOnly = showToOwnerOnly;
    }

    private boolean isHiddenFromUserLocked() {
        WindowState win = getTopParentWindow();
        boolean z = false;
        if (win.mAttrs.type < IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && win.mAppToken != null && win.mAppToken.mShowForAllUsers && win.mFrame.left <= win.mDisplayFrame.left && win.mFrame.top <= win.mDisplayFrame.top && win.mFrame.right >= win.mStableFrame.right && win.mFrame.bottom >= win.mStableFrame.bottom) {
            return false;
        }
        if (win.mShowToOwnerOnly && !this.mService.isCurrentProfileLocked(UserHandle.getUserId(win.mOwnerUid))) {
            z = true;
        }
        return z;
    }

    private static void applyInsets(Region outRegion, Rect frame, Rect inset) {
        outRegion.set(frame.left + inset.left, frame.top + inset.top, frame.right - inset.right, frame.bottom - inset.bottom);
    }

    void getTouchableRegion(Region outRegion) {
        Rect frame = this.mFrame;
        switch (this.mTouchableInsets) {
            case 1:
                applyInsets(outRegion, frame, this.mGivenContentInsets);
                break;
            case 2:
                applyInsets(outRegion, frame, this.mGivenVisibleInsets);
                break;
            case 3:
                outRegion.set(this.mGivenTouchableRegion);
                outRegion.translate(frame.left, frame.top);
                break;
            default:
                outRegion.set(frame);
                break;
        }
        cropRegionToStackBoundsIfNeeded(outRegion);
    }

    private void cropRegionToStackBoundsIfNeeded(Region region) {
        Task task = getTask();
        if (task != null && task.cropWindowsToStackBounds()) {
            TaskStack stack = task.mStack;
            if (stack != null) {
                stack.getDimBounds(this.mTmpRect);
                region.op(this.mTmpRect, Op.INTERSECT);
            }
        }
    }

    void reportFocusChangedSerialized(boolean focused, boolean inTouchMode) {
        try {
            this.mClient.windowFocusChanged(focused, inTouchMode);
        } catch (RemoteException e) {
        }
        if (this.mFocusCallbacks != null) {
            int N = this.mFocusCallbacks.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IWindowFocusObserver obs = (IWindowFocusObserver) this.mFocusCallbacks.getBroadcastItem(i);
                if (focused) {
                    try {
                        obs.focusGained(this.mWindowId.asBinder());
                    } catch (RemoteException e2) {
                    }
                } else {
                    obs.focusLost(this.mWindowId.asBinder());
                }
            }
            this.mFocusCallbacks.finishBroadcast();
        }
    }

    public Configuration getConfiguration() {
        if (this.mAppToken == null || this.mAppToken.mFrozenMergedConfig.size() <= 0) {
            return super.getConfiguration();
        }
        return (Configuration) this.mAppToken.mFrozenMergedConfig.peek();
    }

    void reportResized() {
        boolean z;
        WindowState windowState;
        String str;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wm.reportResized_");
        stringBuilder.append(getWindowTag());
        Trace.traceBegin(32, stringBuilder.toString());
        StringBuilder stringBuilder2;
        try {
            Rect frame;
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Reporting new frame to ");
                stringBuilder2.append(this);
                stringBuilder2.append(": ");
                stringBuilder2.append(this.mCompatFrame);
                Slog.v(str2, stringBuilder2.toString());
            }
            MergedConfiguration mergedConfiguration = new MergedConfiguration(this.mService.mRoot.getConfiguration(), getMergedOverrideConfiguration());
            if (this.mService.mHwWMSEx != null) {
                this.mService.mHwWMSEx.onChangeConfiguration(mergedConfiguration, this);
            }
            setLastReportedMergedConfiguration(mergedConfiguration);
            boolean reportDraw = true;
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION && this.mWinAnimator.mDrawState == 1) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Resizing ");
                stringBuilder3.append(this);
                stringBuilder3.append(" WITH DRAW PENDING");
                Slog.i(str3, stringBuilder3.toString());
            }
            if (this.mEnforceSizeCompat) {
                frame = this.mCompatFrame;
            } else {
                frame = this.mFrame;
            }
            final Rect overscanInsets = this.mLastOverscanInsets;
            final Rect contentInsets = this.mLastContentInsets;
            final Rect visibleInsets = this.mLastVisibleInsets;
            final Rect stableInsets = this.mLastStableInsets;
            final Rect outsets = this.mLastOutsets;
            if (this.mWinAnimator.mDrawState != 1) {
                reportDraw = false;
            }
            final boolean reportOrientation = this.mReportOrientationChanged;
            final int displayId = getDisplayId();
            final DisplayCutout displayCutout = this.mDisplayCutout.getDisplayCutout();
            if (this.mAttrs.type == 3 || !(this.mClient instanceof IWindow.Stub)) {
                z = false;
                windowState = this;
                try {
                    dispatchResized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, outsets, reportDraw, mergedConfiguration, reportOrientation, displayId, displayCutout);
                } catch (RemoteException e) {
                    windowState.setOrientationChanging(z);
                    windowState.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - windowState.mService.mDisplayFreezeTime);
                    windowState.mOverscanInsetsChanged = z;
                    windowState.mContentInsetsChanged = z;
                    windowState.mVisibleInsetsChanged = z;
                    windowState.mStableInsetsChanged = z;
                    windowState.mWinAnimator.mSurfaceResized = z;
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to report 'resized' to the client of ");
                    stringBuilder2.append(windowState);
                    stringBuilder2.append(", removing this window.");
                    Slog.w(str, stringBuilder2.toString());
                    windowState.mService.mPendingRemove.add(windowState);
                    windowState.mService.mWindowPlacerLocked.requestTraversal();
                    Trace.traceEnd(32);
                }
            }
            AnonymousClass3 anonymousClass3 = anonymousClass3;
            AnonymousClass3 anonymousClass32 = anonymousClass3;
            H h = this.mService.mH;
            final MergedConfiguration mergedConfiguration2 = mergedConfiguration;
            try {
                anonymousClass3 = new Runnable() {
                    public void run() {
                        try {
                            WindowState.this.dispatchResized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, outsets, reportDraw, mergedConfiguration2, reportOrientation, displayId, displayCutout);
                        } catch (RemoteException e) {
                        }
                    }
                };
                h.post(anonymousClass32);
                z = false;
                windowState = this;
            } catch (RemoteException e2) {
                z = false;
                windowState = this;
                windowState.setOrientationChanging(z);
                windowState.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - windowState.mService.mDisplayFreezeTime);
                windowState.mOverscanInsetsChanged = z;
                windowState.mContentInsetsChanged = z;
                windowState.mVisibleInsetsChanged = z;
                windowState.mStableInsetsChanged = z;
                windowState.mWinAnimator.mSurfaceResized = z;
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to report 'resized' to the client of ");
                stringBuilder2.append(windowState);
                stringBuilder2.append(", removing this window.");
                Slog.w(str, stringBuilder2.toString());
                windowState.mService.mPendingRemove.add(windowState);
                windowState.mService.mWindowPlacerLocked.requestTraversal();
                Trace.traceEnd(32);
            }
            if (windowState.mService.mAccessibilityController != null && getDisplayId() == 0) {
                windowState.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
            }
            windowState.mOverscanInsetsChanged = z;
            windowState.mContentInsetsChanged = z;
            windowState.mVisibleInsetsChanged = z;
            windowState.mStableInsetsChanged = z;
            windowState.mOutsetsChanged = z;
            windowState.mFrameSizeChanged = z;
            windowState.mDisplayCutoutChanged = z;
            windowState.mWinAnimator.mSurfaceResized = z;
            windowState.mReportOrientationChanged = z;
        } catch (RemoteException e3) {
            windowState = this;
            z = false;
            windowState.setOrientationChanging(z);
            windowState.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - windowState.mService.mDisplayFreezeTime);
            windowState.mOverscanInsetsChanged = z;
            windowState.mContentInsetsChanged = z;
            windowState.mVisibleInsetsChanged = z;
            windowState.mStableInsetsChanged = z;
            windowState.mWinAnimator.mSurfaceResized = z;
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to report 'resized' to the client of ");
            stringBuilder2.append(windowState);
            stringBuilder2.append(", removing this window.");
            Slog.w(str, stringBuilder2.toString());
            windowState.mService.mPendingRemove.add(windowState);
            windowState.mService.mWindowPlacerLocked.requestTraversal();
            Trace.traceEnd(32);
        }
        Trace.traceEnd(32);
    }

    Rect getBackdropFrame(Rect frame) {
        boolean resizing = isDragResizing() || isDragResizeChanged();
        if (getWindowConfiguration().useWindowFrameForBackdrop() || !resizing) {
            return frame;
        }
        DisplayInfo displayInfo = getDisplayInfo();
        this.mTmpRect.set(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
        return this.mTmpRect;
    }

    protected int getStackId() {
        TaskStack stack = getStack();
        if (stack == null) {
            return -1;
        }
        return stack.mStackId;
    }

    private void dispatchResized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, Rect outsets, boolean reportDraw, MergedConfiguration mergedConfiguration, boolean reportOrientation, int displayId, DisplayCutout displayCutout) throws RemoteException {
        if (reportDraw && toString().contains("StatusBar")) {
            Slog.d(TAG, "dispatchResized called to report draw");
        }
        boolean forceRelayout = isDragResizeChanged() || reportOrientation;
        boolean z = true;
        this.mClient.resized(frame, overscanInsets, contentInsets, visibleInsets, stableInsets, outsets, reportDraw, mergedConfiguration, getBackdropFrame(frame), forceRelayout, this.mPolicy.isNavBarForcedShownLw(this), displayId, new ParcelableWrapper(displayCutout));
        this.mDragResizingChangeReported = z;
    }

    public void registerFocusObserver(IWindowFocusObserver observer) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mFocusCallbacks == null) {
                    this.mFocusCallbacks = new RemoteCallbackList();
                }
                this.mFocusCallbacks.register(observer);
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void unregisterFocusObserver(IWindowFocusObserver observer) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mFocusCallbacks != null) {
                    this.mFocusCallbacks.unregister(observer);
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isFocused() {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = this.mService.mCurrentFocus == this;
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        return z;
    }

    public boolean isInMultiWindowMode() {
        Task task = getTask();
        return (task == null || task.isFullscreen()) ? false : true;
    }

    private boolean inFullscreenContainer() {
        boolean z = true;
        if ((IS_FULL_SCREEN || (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer())) && this.mService != null && !this.mService.mKeyguardGoingAway && this.mService.isKeyguardLocked() && (this.mAttrs.flags & DumpState.DUMP_FROZEN) != 0) {
            return true;
        }
        if (this.mAppToken != null && (!this.mAppToken.matchParentBounds() || isInMultiWindowMode())) {
            z = false;
        }
        return z;
    }

    boolean isLetterboxedAppWindow() {
        return !(isInMultiWindowMode() || this.mAppToken == null || this.mAppToken.matchParentBounds()) || isLetterboxedForDisplayCutoutLw();
    }

    public boolean isLetterboxedForDisplayCutoutLw() {
        if (this.mAppToken != null && this.mParentFrameWasClippedByDisplayCutout && this.mAttrs.layoutInDisplayCutoutMode != 1 && this.mAttrs.isFullscreen()) {
            return frameCoversEntireAppTokenBounds() ^ true;
        }
        return false;
    }

    private boolean frameCoversEntireAppTokenBounds() {
        this.mTmpRect.set(this.mAppToken.getBounds());
        this.mTmpRect.intersectUnchecked(this.mFrame);
        return this.mAppToken.getBounds().equals(this.mTmpRect);
    }

    public boolean isLetterboxedOverlappingWith(Rect rect) {
        return this.mAppToken != null && this.mAppToken.isLetterboxOverlappingWith(rect);
    }

    boolean isDragResizeChanged() {
        return this.mDragResizing != computeDragResizing();
    }

    void setWaitingForDrawnIfResizingChanged() {
        if (isDragResizeChanged()) {
            this.mService.mWaitingForDrawn.add(this);
        }
        super.setWaitingForDrawnIfResizingChanged();
    }

    private boolean isDragResizingChangeReported() {
        return this.mDragResizingChangeReported;
    }

    void resetDragResizingChangeReported() {
        this.mDragResizingChangeReported = false;
        super.resetDragResizingChangeReported();
    }

    int getResizeMode() {
        return this.mResizeMode;
    }

    private boolean computeDragResizing() {
        Task task = getTask();
        boolean z = false;
        if (task == null) {
            return false;
        }
        if ((!inSplitScreenWindowingMode() && !inFreeformWindowingMode()) || this.mAttrs.width != -1 || this.mAttrs.height != -1) {
            return false;
        }
        if (task.isDragResizing()) {
            return true;
        }
        if (!((!getDisplayContent().mDividerControllerLocked.isResizing() && (this.mAppToken == null || this.mAppToken.mFrozenBounds.isEmpty())) || task.inFreeformWindowingMode() || task.inHwPCFreeformWindowingMode() || isGoneForLayoutLw())) {
            z = true;
        }
        return z;
    }

    void setDragResizing() {
        boolean resizing = computeDragResizing();
        if (resizing != this.mDragResizing) {
            this.mDragResizing = resizing;
            Task task = getTask();
            if (task == null || !task.isDragResizing()) {
                int i;
                if (this.mDragResizing && getDisplayContent().mDividerControllerLocked.isResizing()) {
                    i = 1;
                } else {
                    i = 0;
                }
                this.mResizeMode = i;
            } else {
                this.mResizeMode = task.getDragResizeMode();
            }
        }
    }

    boolean isDragResizing() {
        return this.mDragResizing;
    }

    boolean isDockedResizing() {
        if (this.mDragResizing && getResizeMode() == 1) {
            return true;
        }
        if (isChildWindow() && getParentWindow().isDockedResizing()) {
            return true;
        }
        return false;
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, boolean trim) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, trim);
        writeIdentifierToProto(proto, 1146756268034L);
        proto.write(1120986464259L, getDisplayId());
        proto.write(1120986464260L, getStackId());
        this.mAttrs.writeToProto(proto, 1146756268037L);
        this.mGivenContentInsets.writeToProto(proto, 1146756268038L);
        this.mFrame.writeToProto(proto, 1146756268039L);
        this.mContainingFrame.writeToProto(proto, 1146756268040L);
        this.mParentFrame.writeToProto(proto, 1146756268041L);
        this.mContentFrame.writeToProto(proto, 1146756268042L);
        this.mContentInsets.writeToProto(proto, 1146756268043L);
        this.mAttrs.surfaceInsets.writeToProto(proto, 1146756268044L);
        this.mSurfacePosition.writeToProto(proto, 1146756268048L);
        this.mWinAnimator.writeToProto(proto, 1146756268045L);
        proto.write(1133871366158L, this.mAnimatingExit);
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((WindowState) this.mChildren.get(i)).writeToProto(proto, 2246267895823L, trim);
        }
        proto.write(1120986464274L, this.mRequestedWidth);
        proto.write(1120986464275L, this.mRequestedHeight);
        proto.write(1120986464276L, this.mViewVisibility);
        proto.write(1120986464277L, this.mSystemUiVisibility);
        proto.write(1133871366166L, this.mHasSurface);
        proto.write(1133871366167L, isReadyForDisplay());
        this.mDisplayFrame.writeToProto(proto, 1146756268056L);
        this.mOverscanFrame.writeToProto(proto, 1146756268057L);
        this.mVisibleFrame.writeToProto(proto, 1146756268058L);
        this.mDecorFrame.writeToProto(proto, 1146756268059L);
        this.mOutsetFrame.writeToProto(proto, 1146756268060L);
        this.mOverscanInsets.writeToProto(proto, 1146756268061L);
        this.mVisibleInsets.writeToProto(proto, 1146756268062L);
        this.mStableInsets.writeToProto(proto, 1146756268063L);
        this.mOutsets.writeToProto(proto, 1146756268064L);
        this.mDisplayCutout.getDisplayCutout().writeToProto(proto, 1146756268065L);
        proto.write(1133871366178L, this.mRemoveOnExit);
        proto.write(1133871366179L, this.mDestroying);
        proto.write(1133871366180L, this.mRemoved);
        proto.write(1133871366181L, isOnScreen());
        proto.write(1133871366182L, isVisible());
        proto.end(token);
    }

    public void writeIdentifierToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, System.identityHashCode(this));
        proto.write(1120986464258L, UserHandle.getUserId(this.mOwnerUid));
        CharSequence title = getWindowTag();
        if (title != null) {
            proto.write(1138166333443L, title.toString());
        }
        proto.end(token);
    }

    void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        StringBuilder stringBuilder;
        TaskStack stack = getStack();
        pw.print(prefix);
        pw.print("mDisplayId=");
        pw.print(getDisplayId());
        if (stack != null) {
            pw.print(" stackId=");
            pw.print(stack.mStackId);
        }
        pw.print(" mSession=");
        pw.print(this.mSession);
        pw.print(" mClient=");
        pw.println(this.mClient.asBinder());
        pw.print(prefix);
        pw.print("mOwnerUid=");
        pw.print(this.mOwnerUid);
        pw.print(" mShowToOwnerOnly=");
        pw.print(this.mShowToOwnerOnly);
        pw.print(" package=");
        pw.print(this.mAttrs.packageName);
        pw.print(" appop=");
        pw.println(AppOpsManager.opToName(this.mAppOp));
        pw.print(prefix);
        pw.print("mAttrs=");
        pw.println(this.mAttrs.toString(prefix));
        pw.print(prefix);
        pw.print("Requested w=");
        pw.print(this.mRequestedWidth);
        pw.print(" h=");
        pw.print(this.mRequestedHeight);
        pw.print(" mLayoutSeq=");
        pw.println(this.mLayoutSeq);
        if (!(this.mRequestedWidth == this.mLastRequestedWidth && this.mRequestedHeight == this.mLastRequestedHeight)) {
            pw.print(prefix);
            pw.print("LastRequested w=");
            pw.print(this.mLastRequestedWidth);
            pw.print(" h=");
            pw.println(this.mLastRequestedHeight);
        }
        if (this.mIsChildWindow || this.mLayoutAttached) {
            pw.print(prefix);
            pw.print("mParentWindow=");
            pw.print(getParentWindow());
            pw.print(" mLayoutAttached=");
            pw.println(this.mLayoutAttached);
        }
        if (this.mIsImWindow || this.mIsWallpaper || this.mIsFloatingLayer) {
            pw.print(prefix);
            pw.print("mIsImWindow=");
            pw.print(this.mIsImWindow);
            pw.print(" mIsWallpaper=");
            pw.print(this.mIsWallpaper);
            pw.print(" mIsFloatingLayer=");
            pw.print(this.mIsFloatingLayer);
            pw.print(" mWallpaperVisible=");
            pw.println(this.mWallpaperVisible);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mBaseLayer=");
            pw.print(this.mBaseLayer);
            pw.print(" mSubLayer=");
            pw.print(this.mSubLayer);
            pw.print(" mAnimLayer=");
            pw.print(this.mLayer);
            pw.print("+");
            pw.print("=");
            pw.print(this.mWinAnimator.mAnimLayer);
            pw.print(" mLastLayer=");
            pw.println(this.mWinAnimator.mLastLayer);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mToken=");
            pw.println(this.mToken);
            if (this.mAppToken != null) {
                pw.print(prefix);
                pw.print("mAppToken=");
                pw.println(this.mAppToken);
                pw.print(prefix);
                pw.print(" isAnimatingWithSavedSurface()=");
                pw.print(" mAppDied=");
                pw.print(this.mAppDied);
                pw.print(prefix);
                pw.print("drawnStateEvaluated=");
                pw.print(getDrawnStateEvaluated());
                pw.print(prefix);
                pw.print("mightAffectAllDrawn=");
                pw.println(mightAffectAllDrawn());
            }
            pw.print(prefix);
            pw.print("mViewVisibility=0x");
            pw.print(Integer.toHexString(this.mViewVisibility));
            pw.print(" mHaveFrame=");
            pw.print(this.mHaveFrame);
            pw.print(" mObscured=");
            pw.println(this.mObscured);
            pw.print(prefix);
            pw.print("mSeq=");
            pw.print(this.mSeq);
            pw.print(" mSystemUiVisibility=0x");
            pw.println(Integer.toHexString(this.mSystemUiVisibility));
        }
        if (!this.mPolicyVisibility || !this.mPolicyVisibilityAfterAnim || !this.mAppOpVisibility || isParentWindowHidden() || this.mPermanentlyHidden || this.mForceHideNonSystemOverlayWindow || this.mHiddenWhileSuspended) {
            pw.print(prefix);
            pw.print("mPolicyVisibility=");
            pw.print(this.mPolicyVisibility);
            pw.print(" mPolicyVisibilityAfterAnim=");
            pw.print(this.mPolicyVisibilityAfterAnim);
            pw.print(" mAppOpVisibility=");
            pw.print(this.mAppOpVisibility);
            pw.print(" parentHidden=");
            pw.print(isParentWindowHidden());
            pw.print(" mPermanentlyHidden=");
            pw.print(this.mPermanentlyHidden);
            pw.print(" mHiddenWhileSuspended=");
            pw.print(this.mHiddenWhileSuspended);
            pw.print(" mForceHideNonSystemOverlayWindow=");
            pw.println(this.mForceHideNonSystemOverlayWindow);
        }
        if (!this.mRelayoutCalled || this.mLayoutNeeded) {
            pw.print(prefix);
            pw.print("mRelayoutCalled=");
            pw.print(this.mRelayoutCalled);
            pw.print(" mLayoutNeeded=");
            pw.println(this.mLayoutNeeded);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mGivenContentInsets=");
            this.mGivenContentInsets.printShortString(pw);
            pw.print(" mGivenVisibleInsets=");
            this.mGivenVisibleInsets.printShortString(pw);
            pw.println();
            if (this.mTouchableInsets != 0 || this.mGivenInsetsPending) {
                pw.print(prefix);
                pw.print("mTouchableInsets=");
                pw.print(this.mTouchableInsets);
                pw.print(" mGivenInsetsPending=");
                pw.println(this.mGivenInsetsPending);
                Region region = new Region();
                getTouchableRegion(region);
                pw.print(prefix);
                pw.print("touchable region=");
                pw.println(region);
            }
            pw.print(prefix);
            pw.print("mFullConfiguration=");
            pw.println(getConfiguration());
            pw.print(prefix);
            pw.print("mLastReportedConfiguration=");
            pw.println(getLastReportedConfiguration());
        }
        pw.print(prefix);
        pw.print("mHasSurface=");
        pw.print(this.mHasSurface);
        pw.print(" isReadyForDisplay()=");
        pw.print(isReadyForDisplay());
        pw.print(" mWindowRemovalAllowed=");
        pw.println(this.mWindowRemovalAllowed);
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mFrame=");
            this.mFrame.printShortString(pw);
            pw.print(" last=");
            this.mLastFrame.printShortString(pw);
            pw.println();
        }
        if (this.mEnforceSizeCompat) {
            pw.print(prefix);
            pw.print("mCompatFrame=");
            this.mCompatFrame.printShortString(pw);
            pw.println();
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("Frames: containing=");
            this.mContainingFrame.printShortString(pw);
            pw.print(" parent=");
            this.mParentFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    display=");
            this.mDisplayFrame.printShortString(pw);
            pw.print(" overscan=");
            this.mOverscanFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    content=");
            this.mContentFrame.printShortString(pw);
            pw.print(" visible=");
            this.mVisibleFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    decor=");
            this.mDecorFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("    outset=");
            this.mOutsetFrame.printShortString(pw);
            pw.println();
            pw.print(prefix);
            pw.print("Cur insets: overscan=");
            this.mOverscanInsets.printShortString(pw);
            pw.print(" content=");
            this.mContentInsets.printShortString(pw);
            pw.print(" visible=");
            this.mVisibleInsets.printShortString(pw);
            pw.print(" stable=");
            this.mStableInsets.printShortString(pw);
            pw.print(" surface=");
            this.mAttrs.surfaceInsets.printShortString(pw);
            pw.print(" outsets=");
            this.mOutsets.printShortString(pw);
            stringBuilder = new StringBuilder();
            stringBuilder.append(" cutout=");
            stringBuilder.append(this.mDisplayCutout.getDisplayCutout());
            pw.print(stringBuilder.toString());
            pw.println();
            pw.print(prefix);
            pw.print("Lst insets: overscan=");
            this.mLastOverscanInsets.printShortString(pw);
            pw.print(" content=");
            this.mLastContentInsets.printShortString(pw);
            pw.print(" visible=");
            this.mLastVisibleInsets.printShortString(pw);
            pw.print(" stable=");
            this.mLastStableInsets.printShortString(pw);
            pw.print(" physical=");
            this.mLastOutsets.printShortString(pw);
            pw.print(" outset=");
            this.mLastOutsets.printShortString(pw);
            stringBuilder = new StringBuilder();
            stringBuilder.append(" cutout=");
            stringBuilder.append(this.mLastDisplayCutout);
            pw.print(stringBuilder.toString());
            pw.println();
        }
        super.dump(pw, prefix, dumpAll);
        pw.print(prefix);
        pw.print(this.mWinAnimator);
        pw.println(":");
        WindowStateAnimator windowStateAnimator = this.mWinAnimator;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append("  ");
        windowStateAnimator.dump(pw, stringBuilder2.toString(), dumpAll);
        if (this.mAnimatingExit || this.mRemoveOnExit || this.mDestroying || this.mRemoved) {
            pw.print(prefix);
            pw.print("mAnimatingExit=");
            pw.print(this.mAnimatingExit);
            pw.print(" mRemoveOnExit=");
            pw.print(this.mRemoveOnExit);
            pw.print(" mDestroying=");
            pw.print(this.mDestroying);
            pw.print(" mRemoved=");
            pw.println(this.mRemoved);
        }
        boolean z = true;
        if (getOrientationChanging() || this.mAppFreezing || this.mReportOrientationChanged) {
            boolean z2;
            pw.print(prefix);
            pw.print("mOrientationChanging=");
            pw.print(this.mOrientationChanging);
            pw.print(" configOrientationChanging=");
            if (getLastReportedConfiguration().orientation != getConfiguration().orientation) {
                z2 = true;
            } else {
                z2 = false;
            }
            pw.print(z2);
            pw.print(" mAppFreezing=");
            pw.print(this.mAppFreezing);
            pw.print(" mReportOrientationChanged=");
            pw.println(this.mReportOrientationChanged);
        }
        if (this.mLastFreezeDuration != 0) {
            pw.print(prefix);
            pw.print("mLastFreezeDuration=");
            TimeUtils.formatDuration((long) this.mLastFreezeDuration, pw);
            pw.println();
        }
        if (!(this.mHScale == 1.0f && this.mVScale == 1.0f)) {
            pw.print(prefix);
            pw.print("mHScale=");
            pw.print(this.mHScale);
            pw.print(" mVScale=");
            pw.println(this.mVScale);
        }
        if (!(this.mWallpaperX == -1.0f && this.mWallpaperY == -1.0f)) {
            pw.print(prefix);
            pw.print("mWallpaperX=");
            pw.print(this.mWallpaperX);
            pw.print(" mWallpaperY=");
            pw.println(this.mWallpaperY);
        }
        if (!(this.mWallpaperXStep == -1.0f && this.mWallpaperYStep == -1.0f)) {
            pw.print(prefix);
            pw.print("mWallpaperXStep=");
            pw.print(this.mWallpaperXStep);
            pw.print(" mWallpaperYStep=");
            pw.println(this.mWallpaperYStep);
        }
        if (!(this.mWallpaperDisplayOffsetX == Integer.MIN_VALUE && this.mWallpaperDisplayOffsetY == Integer.MIN_VALUE)) {
            pw.print(prefix);
            pw.print("mWallpaperDisplayOffsetX=");
            pw.print(this.mWallpaperDisplayOffsetX);
            pw.print(" mWallpaperDisplayOffsetY=");
            pw.println(this.mWallpaperDisplayOffsetY);
        }
        if (this.mDrawLock != null) {
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("mDrawLock=");
            stringBuilder.append(this.mDrawLock);
            pw.println(stringBuilder.toString());
        }
        if (isDragResizing()) {
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("isDragResizing=");
            stringBuilder.append(isDragResizing());
            pw.println(stringBuilder.toString());
        }
        if (computeDragResizing()) {
            pw.print(prefix);
            stringBuilder = new StringBuilder();
            stringBuilder.append("computeDragResizing=");
            stringBuilder.append(computeDragResizing());
            pw.println(stringBuilder.toString());
        }
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("isOnScreen=");
        stringBuilder.append(isOnScreen());
        pw.println(stringBuilder.toString());
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("isVisible=");
        stringBuilder.append(isVisible());
        pw.println(stringBuilder.toString());
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("canReceiveKeys=");
        stringBuilder.append(canReceiveKeys());
        pw.println(stringBuilder.toString());
        pw.print(prefix);
        stringBuilder = new StringBuilder();
        stringBuilder.append("hwNotchSupport=");
        if ((this.mAttrs.hwFlags & 65536) == 0) {
            z = getHwNotchSupport();
        }
        stringBuilder.append(z);
        pw.println(stringBuilder.toString());
        pw.print(prefix);
        pw.print("mEnforceSizeCompat=");
        pw.print(this.mEnforceSizeCompat);
        pw.print(" compat=");
        this.mCompatFrame.printShortString(pw);
        pw.print(" mInvGlobalScale=");
        pw.print(this.mInvGlobalScale);
        pw.print(" mGlobalScale=");
        pw.println(this.mGlobalScale);
    }

    String getName() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(getWindowTag());
        return stringBuilder.toString();
    }

    CharSequence getWindowTag() {
        CharSequence tag = this.mAttrs.getTitle();
        if (tag == null || tag.length() <= 0) {
            return this.mAttrs.packageName;
        }
        return tag;
    }

    public String toString() {
        CharSequence title = getWindowTag();
        if (!(this.mStringNameCache != null && this.mLastTitle == title && this.mWasExiting == this.mAnimatingExit)) {
            boolean containPrivacyInfo;
            this.mLastTitle = title;
            this.mWasExiting = this.mAnimatingExit;
            if (this.mLastTitle instanceof Spanned) {
                Spanned text = this.mLastTitle;
                containPrivacyInfo = false;
                for (SuggestionSpan tmpSpan : text.getSpans(0, text.length(), Object.class)) {
                    if (tmpSpan instanceof SuggestionSpan) {
                        String[] suggestions = tmpSpan.getSuggestions();
                        if (suggestions.length > 0) {
                            boolean equals = suggestions[0].equals("privacy title");
                            containPrivacyInfo = equals;
                            if (equals) {
                                break;
                            }
                        } else {
                            continue;
                        }
                    }
                }
            } else {
                containPrivacyInfo = false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Window{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" u");
            stringBuilder.append(UserHandle.getUserId(this.mOwnerUid));
            stringBuilder.append(" ");
            stringBuilder.append(containPrivacyInfo ? "xxxxxx" : this.mLastTitle);
            stringBuilder.append(this.mAnimatingExit ? " EXITING}" : "}");
            this.mStringNameCache = stringBuilder.toString();
        }
        return this.mStringNameCache;
    }

    void transformClipRectFromScreenToSurfaceSpace(Rect clipRect) {
        if (this.mHScale >= 0.0f) {
            clipRect.left = (int) (((float) clipRect.left) / this.mHScale);
            clipRect.right = (int) Math.ceil((double) (((float) clipRect.right) / this.mHScale));
        }
        if (this.mVScale >= 0.0f) {
            clipRect.top = (int) (((float) clipRect.top) / this.mVScale);
            clipRect.bottom = (int) Math.ceil((double) (((float) clipRect.bottom) / this.mVScale));
        }
    }

    void applyGravityAndUpdateFrame(Rect containingFrame, Rect displayFrame) {
        int w;
        int h;
        float x;
        float y;
        Rect rect = containingFrame;
        int pw = containingFrame.width();
        int ph = containingFrame.height();
        Task task = getTask();
        boolean inNonFullscreenContainer = inFullscreenContainer() ^ 1;
        boolean z = false;
        boolean noLimits = (this.mAttrs.flags & 512) != 0;
        if (!(task != null && inNonFullscreenContainer && (this.mAttrs.type == 1 || noLimits))) {
            z = true;
        }
        boolean fitToDisplay = z;
        if ((this.mAttrs.flags & 16384) != 0) {
            if (this.mAttrs.width < 0) {
                w = pw;
            } else if (this.mEnforceSizeCompat) {
                w = (int) ((((float) this.mAttrs.width) * this.mGlobalScale) + 0.5f);
            } else {
                w = this.mAttrs.width;
            }
            if (this.mAttrs.height < 0) {
                h = ph;
            } else if (this.mEnforceSizeCompat) {
                h = (int) ((((float) this.mAttrs.height) * this.mGlobalScale) + 0.5f);
            } else {
                h = this.mAttrs.height;
            }
        } else {
            if (this.mAttrs.width == -1) {
                w = pw;
            } else if (this.mEnforceSizeCompat) {
                w = (int) ((((float) this.mRequestedWidth) * this.mGlobalScale) + 0.5f);
            } else {
                w = this.mRequestedWidth;
            }
            if (this.mAttrs.height == -1) {
                h = ph;
            } else if (this.mEnforceSizeCompat) {
                h = (int) ((((float) this.mRequestedHeight) * this.mGlobalScale) + 0.5f);
            } else {
                h = this.mRequestedHeight;
            }
        }
        if (this.mEnforceSizeCompat) {
            x = ((float) this.mAttrs.x) * this.mGlobalScale;
            y = ((float) this.mAttrs.y) * this.mGlobalScale;
        } else {
            x = (float) this.mAttrs.x;
            y = (float) this.mAttrs.y;
        }
        float x2 = x;
        float y2 = y;
        if (inNonFullscreenContainer && !layoutInParentFrame()) {
            w = Math.min(w, pw);
            h = Math.min(h, ph);
        }
        Gravity.apply(this.mAttrs.gravity, w, h, rect, (int) (x2 + (this.mAttrs.horizontalMargin * ((float) pw))), (int) (y2 + (this.mAttrs.verticalMargin * ((float) ph))), this.mFrame);
        if (fitToDisplay) {
            Gravity.applyDisplay(this.mAttrs.gravity, displayFrame, this.mFrame);
        } else {
            Rect rect2 = displayFrame;
        }
        this.mCompatFrame.set(this.mFrame);
        if (this.mEnforceSizeCompat) {
            this.mCompatFrame.scale(this.mInvGlobalScale);
            String title = getWindowTag() != null ? getWindowTag().toString() : null;
            if (title == null || !title.equals("Toast") || this.mRequestedWidth - this.mCompatFrame.width() != 1) {
                return;
            }
            Rect rect3;
            if (this.mFrame.right + 1 <= rect.right) {
                rect3 = this.mFrame;
                rect3.right++;
                rect3 = this.mCompatFrame;
                rect3.right++;
            } else if (this.mFrame.left - 1 >= rect.left) {
                rect3 = this.mFrame;
                rect3.left--;
                rect3 = this.mCompatFrame;
                rect3.left--;
            }
        }
    }

    boolean isChildWindow() {
        return this.mIsChildWindow;
    }

    boolean layoutInParentFrame() {
        return this.mIsChildWindow && (this.mAttrs.privateFlags & 65536) != 0;
    }

    boolean hideNonSystemOverlayWindowsWhenVisible() {
        return (this.mAttrs.privateFlags & DumpState.DUMP_FROZEN) != 0 && this.mSession.mCanHideNonSystemOverlayWindows;
    }

    WindowState getParentWindow() {
        return this.mIsChildWindow ? (WindowState) super.getParent() : null;
    }

    WindowState getTopParentWindow() {
        WindowState topParent = this;
        WindowState current = topParent;
        while (current != null && current.mIsChildWindow) {
            current = current.getParentWindow();
            if (current != null) {
                topParent = current;
            }
        }
        return topParent;
    }

    boolean isParentWindowHidden() {
        WindowState parent = getParentWindow();
        return parent != null && parent.mHidden;
    }

    void setWillReplaceWindow(boolean animate) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).setWillReplaceWindow(animate);
        }
        if ((this.mAttrs.privateFlags & 32768) == 0 && this.mAttrs.type != 3) {
            this.mWillReplaceWindow = true;
            this.mReplacementWindow = null;
            this.mAnimateReplacingWindow = animate;
        }
    }

    void clearWillReplaceWindow() {
        this.mWillReplaceWindow = false;
        this.mReplacementWindow = null;
        this.mAnimateReplacingWindow = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).clearWillReplaceWindow();
        }
    }

    boolean waitingForReplacement() {
        if (this.mWillReplaceWindow) {
            return true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowState) this.mChildren.get(i)).waitingForReplacement()) {
                return true;
            }
        }
        return false;
    }

    void requestUpdateWallpaperIfNeeded() {
        DisplayContent dc = getDisplayContent();
        if (!(dc == null || (this.mAttrs.flags & DumpState.DUMP_DEXOPT) == 0)) {
            dc.pendingLayoutChanges |= 4;
            dc.setLayoutNeeded();
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).requestUpdateWallpaperIfNeeded();
        }
    }

    float translateToWindowX(float x) {
        float winX = x - ((float) this.mFrame.left);
        if (this.mEnforceSizeCompat) {
            return winX * this.mGlobalScale;
        }
        return winX;
    }

    float translateToWindowY(float y) {
        float winY = y - ((float) this.mFrame.top);
        if (this.mEnforceSizeCompat) {
            return winY * this.mGlobalScale;
        }
        return winY;
    }

    boolean shouldBeReplacedWithChildren() {
        return this.mIsChildWindow || this.mAttrs.type == 2 || this.mAttrs.type == 4;
    }

    void setWillReplaceChildWindows() {
        if (shouldBeReplacedWithChildren()) {
            setWillReplaceWindow(false);
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).setWillReplaceChildWindows();
        }
    }

    WindowState getReplacingWindow() {
        if (this.mAnimatingExit && this.mWillReplaceWindow && this.mAnimateReplacingWindow) {
            return this;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState replacing = ((WindowState) this.mChildren.get(i)).getReplacingWindow();
            if (replacing != null) {
                return replacing;
            }
        }
        return null;
    }

    public int getRotationAnimationHint() {
        if (this.mAppToken != null) {
            return this.mAppToken.mRotationAnimationHint;
        }
        return -1;
    }

    public boolean isInputMethodWindow() {
        return this.mIsImWindow;
    }

    boolean performShowLocked() {
        if (isHiddenFromUserLocked()) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hiding ");
                stringBuilder.append(this);
                stringBuilder.append(", belonging to ");
                stringBuilder.append(this.mOwnerUid);
                Slog.w(str, stringBuilder.toString());
            }
            hideLw(false);
            return false;
        }
        logPerformShow("performShow on ");
        int drawState = this.mWinAnimator.mDrawState;
        if (!((drawState != 4 && drawState != 3) || this.mAttrs.type == 3 || this.mAppToken == null)) {
            this.mAppToken.onFirstWindowDrawn(this, this.mWinAnimator);
        }
        if (this.mWinAnimator.mDrawState != 3 || !isReadyForDisplay()) {
            return false;
        }
        logPerformShow("Showing ");
        this.mService.enableScreenIfNeededLocked();
        this.mWinAnimator.applyEnterAnimationLocked();
        this.mWinAnimator.mLastAlpha = -1.0f;
        this.mWinAnimator.mDrawState = 4;
        this.mService.scheduleAnimationLocked();
        if (this.mHidden) {
            this.mHidden = false;
            DisplayContent displayContent = getDisplayContent();
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                WindowState c = (WindowState) this.mChildren.get(i);
                if (c.mWinAnimator.mSurfaceController != null) {
                    c.performShowLocked();
                    if (displayContent != null) {
                        displayContent.setLayoutNeeded();
                    }
                }
            }
        }
        if (this.mAttrs.type == 2011) {
            getDisplayContent().mDividerControllerLocked.resetImeHideRequested();
        }
        return true;
    }

    private void logPerformShow(String prefix) {
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(this);
            stringBuilder.append(": mDrawState=");
            stringBuilder.append(this.mWinAnimator.drawStateToString());
            stringBuilder.append(" readyForDisplay=");
            stringBuilder.append(isReadyForDisplay());
            stringBuilder.append(" starting=");
            boolean z = false;
            stringBuilder.append(this.mAttrs.type == 3);
            stringBuilder.append(" during animation: policyVis=");
            stringBuilder.append(this.mPolicyVisibility);
            stringBuilder.append(" parentHidden=");
            stringBuilder.append(isParentWindowHidden());
            stringBuilder.append(" tok.hiddenRequested=");
            boolean z2 = this.mAppToken != null && this.mAppToken.hiddenRequested;
            stringBuilder.append(z2);
            stringBuilder.append(" tok.hidden=");
            z2 = this.mAppToken != null && this.mAppToken.isHidden();
            stringBuilder.append(z2);
            stringBuilder.append(" animationSet=");
            stringBuilder.append(this.mWinAnimator.isAnimationSet());
            stringBuilder.append(" tok animating=");
            if (this.mAppToken != null && this.mAppToken.isSelfAnimating()) {
                z = true;
            }
            stringBuilder.append(z);
            stringBuilder.append(" Callers=");
            stringBuilder.append(Debug.getCallers(4));
            Slog.v(str, stringBuilder.toString());
        }
    }

    WindowInfo getWindowInfo() {
        WindowInfo windowInfo = WindowInfo.obtain();
        windowInfo.type = this.mAttrs.type;
        windowInfo.layer = this.mLayer;
        windowInfo.token = this.mClient.asBinder();
        if (this.mAppToken != null) {
            windowInfo.activityToken = this.mAppToken.appToken.asBinder();
        }
        windowInfo.title = this.mAttrs.accessibilityTitle;
        int j = 0;
        boolean z = true;
        boolean isPanelWindow = this.mAttrs.type >= 1000 && this.mAttrs.type <= 1999;
        boolean isAccessibilityOverlay = windowInfo.type == 2032;
        if (TextUtils.isEmpty(windowInfo.title) && (isPanelWindow || isAccessibilityOverlay)) {
            windowInfo.title = this.mAttrs.getTitle();
        }
        windowInfo.accessibilityIdOfAnchor = this.mAttrs.accessibilityIdOfAnchor;
        windowInfo.focused = isFocused();
        Task task = getTask();
        if (task == null || !task.inPinnedWindowingMode()) {
            z = false;
        }
        windowInfo.inPictureInPicture = z;
        if (this.mIsChildWindow) {
            windowInfo.parentToken = getParentWindow().mClient.asBinder();
        }
        int childCount = this.mChildren.size();
        if (childCount > 0) {
            if (windowInfo.childTokens == null) {
                windowInfo.childTokens = new ArrayList(childCount);
            }
            while (j < childCount) {
                windowInfo.childTokens.add(((WindowState) this.mChildren.get(j)).mClient.asBinder());
                j++;
            }
        }
        return windowInfo;
    }

    int getHighestAnimLayer() {
        int highest = this.mWinAnimator.mAnimLayer;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            int childLayer = ((WindowState) this.mChildren.get(i)).getHighestAnimLayer();
            if (childLayer > highest) {
                highest = childLayer;
            }
        }
        return highest;
    }

    boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        if (this.mChildren.isEmpty()) {
            return applyInOrderWithImeWindows(callback, traverseTopToBottom);
        }
        if (traverseTopToBottom) {
            return forAllWindowTopToBottom(callback);
        }
        return forAllWindowBottomToTop(callback);
    }

    private boolean forAllWindowBottomToTop(ToBooleanFunction<WindowState> callback) {
        int i = 0;
        int count = this.mChildren.size();
        WindowState child = this.mChildren.get(0);
        while (true) {
            child = child;
            if (i >= count || child.mSubLayer >= 0) {
                break;
            } else if (child.applyInOrderWithImeWindows(callback, false)) {
                return true;
            } else {
                i++;
                if (i >= count) {
                    break;
                }
                child = this.mChildren.get(i);
            }
        }
        if (applyInOrderWithImeWindows(callback, false)) {
            return true;
        }
        while (i < count) {
            if (child.applyInOrderWithImeWindows(callback, false)) {
                return true;
            }
            i++;
            if (i >= count) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        return false;
    }

    private boolean forAllWindowTopToBottom(ToBooleanFunction<WindowState> callback) {
        int i = this.mChildren.size() - 1;
        WindowState child = (WindowState) this.mChildren.get(i);
        while (i >= 0 && child.mSubLayer >= 0) {
            if (child.applyInOrderWithImeWindows(callback, true)) {
                return true;
            }
            i--;
            if (i < 0) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        if (applyInOrderWithImeWindows(callback, true)) {
            return true;
        }
        while (i >= 0) {
            if (child.applyInOrderWithImeWindows(callback, true)) {
                return true;
            }
            i--;
            if (i < 0) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        return false;
    }

    private boolean applyInOrderWithImeWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        if (traverseTopToBottom) {
            if ((isInputMethodTarget() && getDisplayContent().forAllImeWindows(callback, traverseTopToBottom)) || callback.apply(this)) {
                return true;
            }
        } else if (callback.apply(this)) {
            return true;
        } else {
            if (isInputMethodTarget() && getDisplayContent().forAllImeWindows(callback, traverseTopToBottom)) {
                return true;
            }
        }
        return false;
    }

    WindowState getWindow(Predicate<WindowState> callback) {
        WindowState windowState = null;
        if (this.mChildren.isEmpty()) {
            if (callback.test(this)) {
                windowState = this;
            }
            return windowState;
        }
        int i = this.mChildren.size() - 1;
        WindowState child = (WindowState) this.mChildren.get(i);
        while (i >= 0 && child.mSubLayer >= 0) {
            if (callback.test(child)) {
                return child;
            }
            i--;
            if (i < 0) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        if (callback.test(this)) {
            return this;
        }
        while (i >= 0) {
            if (callback.test(child)) {
                return child;
            }
            i--;
            if (i < 0) {
                break;
            }
            child = (WindowState) this.mChildren.get(i);
        }
        return null;
    }

    @VisibleForTesting
    boolean isSelfOrAncestorWindowAnimatingExit() {
        WindowState window = this;
        while (!window.mAnimatingExit) {
            window = window.getParentWindow();
            if (window == null) {
                return false;
            }
        }
        return true;
    }

    void onExitAnimationDone() {
        if (!this.mChildren.isEmpty()) {
            ArrayList<WindowState> childWindows = new ArrayList(this.mChildren);
            for (int i = childWindows.size() - 1; i >= 0; i--) {
                ((WindowState) childWindows.get(i)).onExitAnimationDone();
            }
        }
        String str;
        if (this.mWinAnimator == null) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onExitAnimationDone: ");
            stringBuilder.append(this);
            Slog.d(str, stringBuilder.toString());
            return;
        }
        if (this.mWinAnimator.mEnteringAnimation) {
            this.mWinAnimator.mEnteringAnimation = false;
            this.mService.requestTraversal();
            if (this.mAppToken == null) {
                try {
                    this.mClient.dispatchWindowShown();
                } catch (RemoteException e) {
                }
            }
        }
        if (!isSelfAnimating()) {
            if (this.mService.mAccessibilityController != null && getDisplayId() == 0) {
                this.mService.mAccessibilityController.onSomeWindowResizedOrMovedLocked();
            }
            if (isSelfOrAncestorWindowAnimatingExit()) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exit animation finished in ");
                stringBuilder2.append(this);
                stringBuilder2.append(": remove=");
                stringBuilder2.append(this.mRemoveOnExit);
                Slog.v(str, stringBuilder2.toString());
                this.mDestroying = true;
                boolean hasSurface = this.mWinAnimator.hasSurface();
                if (WindowManagerService.HW_SUPPORT_LAUNCHER_EXIT_ANIM && this.mWinAnimator.mSurfaceController != null) {
                    this.mWinAnimator.mSurfaceController.setWindowClipFlag(0);
                }
                this.mWinAnimator.hide(getPendingTransaction(), "onExitAnimationDone");
                if (this.mAppToken != null) {
                    this.mAppToken.destroySurfaces();
                } else {
                    if (hasSurface) {
                        this.mService.mDestroySurface.add(this);
                    }
                    if (this.mRemoveOnExit) {
                        this.mService.mPendingRemove.add(this);
                        this.mRemoveOnExit = false;
                    }
                }
                this.mAnimatingExit = false;
                getDisplayContent().mWallpaperController.hideWallpapers(this);
            }
        }
    }

    boolean clearAnimatingFlags() {
        boolean didSomething = false;
        if (!(this.mWillReplaceWindow || this.mRemoveOnExit)) {
            if (this.mAnimatingExit) {
                this.mAnimatingExit = false;
                didSomething = true;
            }
            if (this.mDestroying) {
                this.mDestroying = false;
                this.mService.mDestroySurface.remove(this);
                didSomething = true;
            }
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            didSomething |= ((WindowState) this.mChildren.get(i)).clearAnimatingFlags();
        }
        return didSomething;
    }

    public boolean isRtl() {
        return getConfiguration().getLayoutDirection() == 1;
    }

    void hideWallpaperWindow(boolean wasDeferred, String reason) {
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            ((WindowState) this.mChildren.get(j)).hideWallpaperWindow(wasDeferred, reason);
        }
        if (!this.mWinAnimator.mLastHidden || wasDeferred) {
            this.mWinAnimator.hide(reason);
            dispatchWallpaperVisibility(false);
            DisplayContent displayContent = getDisplayContent();
            if (displayContent != null) {
                displayContent.pendingLayoutChanges |= 4;
            }
        }
    }

    void dispatchWallpaperVisibility(boolean visible) {
        boolean hideAllowed = getDisplayContent().mWallpaperController.mDeferredHideWallpaper == null;
        if (this.mWallpaperVisible == visible) {
            return;
        }
        if (hideAllowed || visible) {
            this.mWallpaperVisible = visible;
            try {
                if (WindowManagerDebugConfig.DEBUG_VISIBILITY || WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Updating vis of wallpaper ");
                    stringBuilder.append(this);
                    stringBuilder.append(": ");
                    stringBuilder.append(visible);
                    stringBuilder.append(" from:\n");
                    stringBuilder.append(Debug.getCallers(4, "  "));
                    Slog.v(str, stringBuilder.toString());
                }
                this.mClient.dispatchAppVisibility(visible);
            } catch (RemoteException e) {
            }
        }
    }

    boolean hasVisibleNotDrawnWallpaper() {
        if (this.mWallpaperVisible && !isDrawnLw()) {
            return true;
        }
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            if (((WindowState) this.mChildren.get(j)).hasVisibleNotDrawnWallpaper()) {
                return true;
            }
        }
        return false;
    }

    void updateReportedVisibility(UpdateReportedVisibilityResults results) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).updateReportedVisibility(results);
        }
        if (!this.mAppFreezing && this.mViewVisibility == 0 && this.mAttrs.type != 3 && !this.mDestroying) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Win ");
                stringBuilder.append(this);
                stringBuilder.append(": isDrawn=");
                stringBuilder.append(isDrawnLw());
                stringBuilder.append(", isAnimationSet=");
                stringBuilder.append(this.mWinAnimator.isAnimationSet());
                Slog.v(str, stringBuilder.toString());
                if (!isDrawnLw()) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Not displayed: s=");
                    stringBuilder.append(this.mWinAnimator.mSurfaceController);
                    stringBuilder.append(" pv=");
                    stringBuilder.append(this.mPolicyVisibility);
                    stringBuilder.append(" mDrawState=");
                    stringBuilder.append(this.mWinAnimator.mDrawState);
                    stringBuilder.append(" ph=");
                    stringBuilder.append(isParentWindowHidden());
                    stringBuilder.append(" th=");
                    stringBuilder.append(this.mAppToken != null ? this.mAppToken.hiddenRequested : false);
                    stringBuilder.append(" a=");
                    stringBuilder.append(this.mWinAnimator.isAnimationSet());
                    Slog.v(str, stringBuilder.toString());
                }
            }
            results.numInteresting++;
            if (isDrawnLw()) {
                results.numDrawn++;
                if (!this.mWinAnimator.isAnimationSet()) {
                    results.numVisible++;
                }
                results.nowGone = false;
            } else if (this.mWinAnimator.isAnimationSet()) {
                results.nowGone = false;
            }
        }
    }

    private boolean skipDecorCrop() {
        if (this.mDecorFrame.isEmpty()) {
            return true;
        }
        if (this.mAppToken != null) {
            return false;
        }
        return this.mToken.canLayerAboveSystemBars();
    }

    void calculatePolicyCrop(Rect policyCrop) {
        DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
        if (!isDefaultDisplay()) {
            policyCrop.set(0, 0, this.mCompatFrame.width(), this.mCompatFrame.height());
            policyCrop.intersect(-this.mCompatFrame.left, -this.mCompatFrame.top, displayInfo.logicalWidth - this.mCompatFrame.left, displayInfo.logicalHeight - this.mCompatFrame.top);
        } else if (skipDecorCrop()) {
            policyCrop.set(0, 0, this.mCompatFrame.width(), this.mCompatFrame.height());
        } else {
            calculateSystemDecorRect(policyCrop);
        }
    }

    private void computeDecorTopCompensation(int systemDeocrRectTop, float scale) {
        this.mDecorTopCompensation = ((int) ((((float) systemDeocrRectTop) * scale) + 1056964608)) - ((int) ((((float) systemDeocrRectTop) * scale) - 0.5f));
    }

    private void calculateSystemDecorRect(Rect systemDecorRect) {
        Rect decorRect = this.mDecorFrame;
        int width = this.mFrame.width();
        int height = this.mFrame.height();
        int left = this.mFrame.left;
        int top = this.mFrame.top;
        boolean z = false;
        if (isDockedResizing() || getDisplayContent().mDividerControllerLocked.isResizing()) {
            DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
            systemDecorRect.set(0, 0, Math.max(width, displayInfo.logicalWidth), Math.max(height, displayInfo.logicalHeight));
        } else {
            systemDecorRect.set(0, 0, width, height);
        }
        if (!(((inFreeformWindowingMode() || inHwPCFreeformWindowingMode()) && isAnimatingLw()) || isDockedResizing())) {
            z = true;
        }
        if (z) {
            systemDecorRect.intersect(decorRect.left - left, decorRect.top - top, decorRect.right - left, decorRect.bottom - top);
        }
        if (this.mEnforceSizeCompat && this.mInvGlobalScale != 1.0f) {
            float scale = this.mInvGlobalScale;
            computeDecorTopCompensation(systemDecorRect.top, scale);
            systemDecorRect.left = (int) ((((float) systemDecorRect.left) * scale) - 0.5f);
            systemDecorRect.top = (int) ((((float) systemDecorRect.top) * scale) - 0.5f);
            systemDecorRect.right = (int) ((((float) systemDecorRect.right) * scale) + 0.5f);
            systemDecorRect.bottom = (int) ((((float) systemDecorRect.bottom) * scale) + 0.5f);
        }
    }

    void expandForSurfaceInsets(Rect r) {
        r.inset(-this.mAttrs.surfaceInsets.left, -this.mAttrs.surfaceInsets.top, -this.mAttrs.surfaceInsets.right, -this.mAttrs.surfaceInsets.bottom);
    }

    boolean surfaceInsetsChanging() {
        return this.mLastSurfaceInsets.equals(this.mAttrs.surfaceInsets) ^ 1;
    }

    int relayoutVisibleWindow(int result, int attrChanges, int oldVisibility) {
        boolean wasVisible = isVisibleLw();
        int i = 0;
        int i2 = (wasVisible && isDrawnLw()) ? 0 : 2;
        result |= i2;
        if (this.mAnimatingExit) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("relayoutVisibleWindow: ");
            stringBuilder.append(this);
            stringBuilder.append(" mAnimatingExit=true, mRemoveOnExit=");
            stringBuilder.append(this.mRemoveOnExit);
            stringBuilder.append(", mDestroying=");
            stringBuilder.append(this.mDestroying);
            Slog.d(str, stringBuilder.toString());
            this.mWinAnimator.cancelExitAnimationForNextAnimationLocked();
            this.mAnimatingExit = false;
        }
        if (this.mDestroying) {
            this.mDestroying = false;
            this.mService.mDestroySurface.remove(this);
        }
        boolean dockedResizing = true;
        if (oldVisibility == 8) {
            this.mWinAnimator.mEnterAnimationPending = true;
        }
        this.mLastVisibleLayoutRotation = getDisplayContent().getRotation();
        this.mWinAnimator.mEnteringAnimation = true;
        prepareWindowToDisplayDuringRelayout(wasVisible);
        if (wasVisible && !this.mService.mPowerManager.isScreenOn() && (this.mAttrs.flags & DumpState.DUMP_COMPILER_STATS) != 0 && this.mOwnerUid == NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE) {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "Turning screen on as FLAG_TURN_SCREEN_ON!");
            }
            this.mService.mPowerManager.wakeUp(SystemClock.uptimeMillis());
        }
        boolean isFormatChanged = false;
        if (this.mAttrs != null && this.mAttrs.format == -3 && oldVisibility == 4) {
            isFormatChanged = true;
            Slog.i(TAG, "relayoutVisibleWindow: set isFormatChanged");
        }
        if ((isFormatChanged || (attrChanges & 8) != 0) && !this.mWinAnimator.tryChangeFormatInPlaceLocked()) {
            this.mWinAnimator.preserveSurfaceLocked();
            result |= 6;
        }
        if (isDragResizeChanged()) {
            setDragResizing();
            if (this.mHasSurface && !isChildWindow()) {
                this.mWinAnimator.preserveSurfaceLocked();
                result |= 6;
            }
        }
        boolean freeformResizing = isDragResizing() && getResizeMode() == 0;
        if (!(isDragResizing() && getResizeMode() == 1)) {
            dockedResizing = false;
        }
        result |= freeformResizing ? 16 : 0;
        if (dockedResizing) {
            i = 8;
        }
        return result | i;
    }

    boolean isLaidOut() {
        return this.mLayoutSeq != -1;
    }

    void updateLastInsetValues() {
        this.mLastOverscanInsets.set(this.mOverscanInsets);
        this.mLastContentInsets.set(this.mContentInsets);
        this.mLastVisibleInsets.set(this.mVisibleInsets);
        this.mLastStableInsets.set(this.mStableInsets);
        this.mLastOutsets.set(this.mOutsets);
        this.mLastDisplayCutout = this.mDisplayCutout;
    }

    void startAnimation(Animation anim) {
        AnimationAdapter adapter;
        DisplayInfo displayInfo = getDisplayContent().getDisplayInfo();
        anim.initialize(this.mFrame.width(), this.mFrame.height(), displayInfo.appWidth, displayInfo.appHeight);
        anim.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        if (("com.android.contacts".equals(this.mAttrs.packageName) || "com.android.mms".equals(this.mAttrs.packageName)) && this.mAttrs.type == 3) {
            Slog.v(TAG, "skip contacts or mms animation when application starting ");
            anim.scaleCurrentDuration(0.0f);
        } else {
            anim.scaleCurrentDuration(this.mService.getWindowAnimationScaleLocked());
        }
        int lazyMode = this.mService.getLazyMode();
        if (!(getDisplayId() == -1 || getDisplayId() == 0)) {
            lazyMode = 0;
        }
        if (lazyMode == 0) {
            adapter = new LocalAnimationAdapter(new WindowAnimationSpec(anim, this.mSurfacePosition, false), this.mService.mSurfaceAnimationRunner);
        } else {
            adapter = new LocalAnimationAdapter(new WindowAnimationSpec(anim, this.mLazyModeSurfacePosition, false), this.mService.mSurfaceAnimationRunner);
        }
        startAnimation(this.mPendingTransaction, adapter);
        commitPendingTransaction();
    }

    private void startMoveAnimation(int left, int top) {
        Point oldPosition = new Point();
        Point newPosition = new Point();
        transformFrameToSurfacePosition(this.mLastFrame.left, this.mLastFrame.top, oldPosition);
        transformFrameToSurfacePosition(left, top, newPosition);
        startAnimation(getPendingTransaction(), new LocalAnimationAdapter(new MoveAnimationSpec(this, oldPosition.x, oldPosition.y, newPosition.x, newPosition.y, null), this.mService.mSurfaceAnimationRunner));
    }

    private void startAnimation(Transaction t, AnimationAdapter adapter) {
        startAnimation(t, adapter, this.mWinAnimator.mLastHidden);
    }

    protected void onAnimationFinished() {
        this.mWinAnimator.onAnimationFinished();
    }

    void getTransformationMatrix(float[] float9, Matrix outMatrix) {
        float9[0] = this.mWinAnimator.mDsDx;
        float9[3] = this.mWinAnimator.mDtDx;
        float9[1] = this.mWinAnimator.mDtDy;
        float9[4] = this.mWinAnimator.mDsDy;
        int x = this.mSurfacePosition.x;
        int y = this.mSurfacePosition.y;
        WindowContainer parent = getParent();
        if (isChildWindow()) {
            WindowState parentWindow = getParentWindow();
            x += parentWindow.mFrame.left - parentWindow.mAttrs.surfaceInsets.left;
            y += parentWindow.mFrame.top - parentWindow.mAttrs.surfaceInsets.top;
        } else if (parent != null) {
            Rect parentBounds = parent.getBounds();
            x += parentBounds.left;
            y += parentBounds.top;
        }
        float9[2] = (float) x;
        float9[5] = (float) y;
        float9[6] = 0.0f;
        float9[7] = 0.0f;
        float9[8] = 1.0f;
        outMatrix.setValues(float9);
    }

    boolean shouldMagnify() {
        if (this.mAttrs.type == 2011 || this.mAttrs.type == 2012 || this.mAttrs.type == 2027 || this.mAttrs.type == 2019 || this.mAttrs.type == 2024) {
            return false;
        }
        return true;
    }

    SurfaceSession getSession() {
        if (this.mSession.mSurfaceSession != null) {
            return this.mSession.mSurfaceSession;
        }
        return getParent().getSession();
    }

    boolean needsZBoost() {
        if (this.mIsImWindow && this.mService.mInputMethodTarget != null) {
            AppWindowToken appToken = this.mService.mInputMethodTarget.mAppToken;
            if (appToken != null) {
                return appToken.needsZBoost();
            }
        }
        return this.mWillReplaceWindow;
    }

    private void applyDims(Dimmer dimmer) {
        if (!this.mAnimatingExit && this.mAppDied) {
            this.mIsDimming = true;
            dimmer.dimAbove(getPendingTransaction(), this, 0.5f);
        } else if ((this.mAttrs.flags & 2) != 0 && isVisibleNow() && !this.mHidden) {
            this.mIsDimming = true;
            dimmer.dimBelow(getPendingTransaction(), this, this.mAttrs.dimAmount);
        }
    }

    void prepareSurfaces() {
        Dimmer dimmer = getDimmer();
        this.mIsDimming = false;
        if (dimmer != null) {
            applyDims(dimmer);
        }
        updateSurfacePosition();
        this.mWinAnimator.prepareSurfaceLocked(true);
        if (!((!this.mWinAnimator.mLazyIsExiting && !this.mWinAnimator.mLazyIsEntering) || this.mService.getDefaultDisplayContentLocked() == null || this.mService.getDefaultDisplayContentLocked().mTmpWindowAnimator == null)) {
            this.mService.getDefaultDisplayContentLocked().mTmpWindowAnimator.setIsLazying(true);
        }
        super.prepareSurfaces();
    }

    public void onAnimationLeashCreated(Transaction t, SurfaceControl leash) {
        super.onAnimationLeashCreated(t, leash);
        t.setPosition(this.mSurfaceControl, 0.0f, 0.0f);
        this.mLastSurfacePosition.set(0, 0);
        this.mLazyModeSurfacePosition.set(0, 0);
    }

    public void onAnimationLeashDestroyed(Transaction t) {
        super.onAnimationLeashDestroyed(t);
        updateSurfacePosition(t);
    }

    void updateSurfacePosition() {
        updateSurfacePosition(getPendingTransaction());
    }

    private void updateSurfacePosition(Transaction t) {
        if (this.mSurfaceControl != null) {
            transformFrameToSurfacePosition(this.mFrame.left, this.mFrame.top, this.mSurfacePosition);
            if (!(this.mSurfaceAnimator.hasLeash() || this.mLastSurfacePosition.equals(this.mSurfacePosition))) {
                t.setPosition(this.mSurfaceControl, (float) this.mSurfacePosition.x, (float) this.mSurfacePosition.y);
                this.mLastSurfacePosition.set(this.mSurfacePosition.x, this.mSurfacePosition.y);
                this.mLazyModeSurfacePosition.set(this.mSurfacePosition.x, this.mSurfacePosition.y);
                if (surfaceInsetsChanging() && this.mWinAnimator.hasSurface()) {
                    this.mLastSurfaceInsets.set(this.mAttrs.surfaceInsets);
                    t.deferTransactionUntil(this.mSurfaceControl, this.mWinAnimator.mSurfaceController.mSurfaceControl.getHandle(), getFrameNumber());
                }
            }
        }
    }

    public void updateSurfacePosition(int x, int y) {
        int lazyMode = this.mService.getLazyMode();
        if (!(getDisplayId() == -1 || getDisplayId() == 0)) {
            lazyMode = 0;
        }
        if (lazyMode != 0 || this.mWinAnimator.mLazyIsExiting) {
            WindowState parent = getParentWindow();
            if (parent != null) {
                x -= parent.mWinAnimator.mShownPosition.x;
                y -= parent.mWinAnimator.mShownPosition.y;
            }
            if (inMultiWindowMode() && getParentWindow() == null && getStack() != null && getTask() != null) {
                x -= getStack().mLastSurfacePosition.x + getTask().mLastSurfacePosition.x;
                y -= getStack().mLastSurfacePosition.y + getTask().mLastSurfacePosition.y;
            }
            x = (int) (((float) x) - (((float) this.mAttrs.surfaceInsets.left) * this.mLazyScale));
            y = (int) (((float) y) - (((float) this.mAttrs.surfaceInsets.top) * this.mLazyScale));
        } else {
            x = this.mSurfacePosition.x;
            y = this.mSurfacePosition.y;
        }
        if ((this.mLazyModeSurfacePosition.x != x || this.mLazyModeSurfacePosition.y != y) && !this.mSurfaceAnimator.hasLeash()) {
            getPendingTransaction().setPosition(this.mSurfaceControl, (float) x, (float) y);
            this.mLazyModeSurfacePosition.set(x, y);
        }
    }

    private void transformFrameToSurfacePosition(int left, int top, Point outPoint) {
        outPoint.set(left, top);
        WindowContainer parentWindowContainer = getParent();
        if (isChildWindow()) {
            WindowState parent = getParentWindow();
            outPoint.offset((-parent.mFrame.left) + parent.mAttrs.surfaceInsets.left, (-parent.mFrame.top) + parent.mAttrs.surfaceInsets.top);
        } else if (parentWindowContainer != null) {
            Rect parentBounds = parentWindowContainer.getBounds();
            outPoint.offset(-parentBounds.left, -parentBounds.top);
        }
        TaskStack stack = getStack();
        if (stack != null) {
            int outset = stack.getStackOutset();
            outPoint.offset(outset, outset);
        }
        outPoint.offset(-this.mAttrs.surfaceInsets.left, -this.mAttrs.surfaceInsets.top);
        if (this.mService != null && this.mService.mHwWMSEx != null) {
            this.mService.mHwWMSEx.updateSurfacePositionForPCMode(this, outPoint);
        }
    }

    boolean needsRelativeLayeringToIme() {
        boolean inTokenWithAndAboveImeTarget = false;
        if (!inSplitScreenWindowingMode()) {
            return false;
        }
        if (isChildWindow()) {
            if (getParentWindow().isInputMethodTarget()) {
                return true;
            }
        } else if (this.mAppToken != null) {
            WindowState imeTarget = this.mService.mInputMethodTarget;
            if (imeTarget != null && imeTarget != this && imeTarget.mToken == this.mToken && imeTarget.compareTo(this) <= 0) {
                inTokenWithAndAboveImeTarget = true;
            }
            return inTokenWithAndAboveImeTarget;
        }
        return false;
    }

    void assignLayer(Transaction t, int layer) {
        if (needsRelativeLayeringToIme()) {
            getDisplayContent().assignRelativeLayerForImeTargetChild(t, this);
        } else {
            super.assignLayer(t, layer);
        }
    }

    public boolean isDimming() {
        return this.mIsDimming;
    }

    public void assignChildLayers(Transaction t) {
        DisplayContent dc = getDisplayContent();
        if (dc != null && toString().contains(dc.mObserveWinTitle)) {
            dc.mObserveWin = this;
        }
        int layer = 1;
        for (int i = 0; i < this.mChildren.size(); i++) {
            WindowState w = (WindowState) this.mChildren.get(i);
            if (w.mAttrs.type == NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE) {
                w.assignLayer(t, -2);
            } else if (w.mAttrs.type == 1004) {
                w.assignLayer(t, -1);
            } else {
                w.assignLayer(t, layer);
            }
            w.assignChildLayers(t);
            layer++;
        }
    }

    void updateTapExcludeRegion(int regionId, int left, int top, int width, int height) {
        DisplayContent currentDisplay = getDisplayContent();
        if (currentDisplay != null) {
            Task task;
            if (this.mTapExcludeRegionHolder == null) {
                this.mTapExcludeRegionHolder = new TapExcludeRegionHolder();
                currentDisplay.mTapExcludeProvidingWindows.add(this);
            }
            this.mTapExcludeRegionHolder.updateRegion(regionId, left, top, width, height);
            boolean isAppFocusedOnDisplay = this.mService.mFocusedApp != null && this.mService.mFocusedApp.getDisplayContent() == currentDisplay;
            if (isAppFocusedOnDisplay) {
                task = this.mService.mFocusedApp.getTask();
            } else {
                task = null;
            }
            currentDisplay.setTouchExcludeRegion(task);
            return;
        }
        throw new IllegalStateException("Trying to update window not attached to any display.");
    }

    void amendTapExcludeRegion(Region region) {
        this.mTapExcludeRegionHolder.amendRegion(region, getBounds());
    }

    public boolean isInputMethodTarget() {
        return this.mService.mInputMethodTarget == this;
    }

    long getFrameNumber() {
        return this.mFrameNumber;
    }

    void setFrameNumber(long frameNumber) {
        this.mFrameNumber = frameNumber;
    }

    public boolean canCarryColors() {
        return this.mCanCarryColors;
    }

    public void setCanCarryColors(boolean carry) {
        this.mCanCarryColors = carry;
    }

    private boolean isStatusBarWindow() {
        return this.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || this.mAttrs.type == 2014 || this.mAttrs.type == 2019 || this.mAttrs.type == 2024;
    }

    /* JADX WARNING: Missing block: B:6:0x0017, code:
            return 2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int getLowResolutionMode() {
        String pkgName = getOwningPackage();
        if ((pkgName != null && (pkgName.contains("com.huawei.hwid") || pkgName.contains("com.huawei.gameassistant"))) || this.mAttrs.type == 2020) {
            return 2;
        }
        boolean z = false;
        if (isStatusBarWindow()) {
            if ((this.mAttrs.hwFlags & 4) != 0) {
                z = true;
            }
            boolean statusBarExpanded = z;
            if ((this.mAttrs.type == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || this.mAttrs.type == 2014) && statusBarExpanded) {
                return 1;
            }
            return 2;
        }
        IApsManager apsManager = (IApsManager) LocalServices.getService(IApsManager.class);
        if (apsManager == null) {
            return 0;
        }
        float resolutionRatio = apsManager.getResolution(getOwningPackage());
        if (0.0f >= resolutionRatio || resolutionRatio >= 1.0f) {
            return 0;
        }
        return 2;
    }

    private void offsetSystemDialog(Rect contentFrame) {
        if (this.mAttrs.type == 2008 && HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(getDisplayId()) && this.mFrame.bottom > contentFrame.bottom) {
            int offsetY;
            int i;
            if (contentFrame.bottom - this.mFrame.bottom > contentFrame.top - this.mFrame.top) {
                offsetY = contentFrame.bottom;
                i = this.mFrame.bottom;
            } else {
                offsetY = contentFrame.top;
                i = this.mFrame.top;
            }
            this.mFrame.offset(0, offsetY - i);
        }
    }

    public boolean getHwNotchSupport() {
        return this.mAppToken != null ? this.mAppToken.getHwNotchSupport() : false;
    }

    public boolean isWindowUsingNotch() {
        switch (this.mService.mHwWMSEx.getAppUseNotchMode(getOwningPackage())) {
            case 1:
                return true;
            case 2:
                return false;
            default:
                boolean z = true;
                if (!((getAttrs().hwFlags & 65536) != 0 || getAttrs().layoutInDisplayCutoutMode == 1 || getHwNotchSupport() || this.mService.mHwWMSEx.isInNotchAppWhitelist(this))) {
                    z = false;
                }
                return z;
        }
    }

    public int getHwGestureNavOptions() {
        return (this.mAppToken != null ? this.mAppToken.getHwGestureNavOptions() : 0) | getAttrs().hwFlags;
    }

    public int getLayer() {
        return this.mToken.getLayer();
    }

    public boolean isInAboveAppWindows() {
        return (this.mToken.asAppWindowToken() != null || this.mToken.windowType == 2013 || this.mToken.windowType == 2103 || this.mToken.windowType == 2011 || this.mToken.windowType == 2012) ? false : true;
    }

    public boolean isImeWithHwFlag() {
        String packageName = getAttrs().packageName;
        if (this.mAttrs.type == 2011 && packageName != null && packageName.contains("com.baidu.input_huawei")) {
            return true;
        }
        return false;
    }

    public void showInsetSurfaceOverlayImmediately() {
        this.mService.openSurfaceTransaction();
        try {
            synchronized (this.mWinAnimator.mInsetSurfaceLock) {
                if (this.mWinAnimator.mInsetSurfaceOverlay != null) {
                    this.mWinAnimator.mInsetSurfaceOverlay.show();
                }
            }
            this.mService.closeSurfaceTransaction("showInsetSurfaceOverlayImmediately");
        } catch (Throwable th) {
            this.mService.closeSurfaceTransaction("showInsetSurfaceOverlayImmediately");
        }
    }

    public void hideInsetSurfaceOverlayImmediately() {
        this.mService.openSurfaceTransaction();
        try {
            synchronized (this.mWinAnimator.mInsetSurfaceLock) {
                if (this.mWinAnimator.mInsetSurfaceOverlay != null) {
                    this.mWinAnimator.mInsetSurfaceOverlay.hide();
                }
            }
            this.mService.closeSurfaceTransaction("destroyInsetSurfaceOverlayImmediately");
        } catch (Throwable th) {
            this.mService.closeSurfaceTransaction("destroyInsetSurfaceOverlayImmediately");
        }
    }
}
