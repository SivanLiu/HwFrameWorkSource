package com.android.server.wm;

import android.app.ActivityManager.TaskSnapshot;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.HwSlog;
import android.util.HwVRUtils;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayCutout.ParcelableWrapper;
import android.view.DisplayInfo;
import android.view.IRotationWatcher;
import android.view.IWindow;
import android.view.IWindowLayoutObserver;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.InputEventReceiver.Factory;
import android.view.SurfaceControl;
import android.view.WindowManager.LayoutParams;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.HwServiceFactory;
import com.android.server.UiThread;
import com.android.server.am.ActivityRecord;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.input.HwInputManagerService;
import com.android.server.input.InputManagerService;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.WindowManagerPolicy.InputConsumer;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.wm.IntelliServiceManager.FaceRotationCallback;
import com.android.server.wm.WindowManagerService.H;
import com.huawei.forcerotation.HwForceRotationManager;
import huawei.android.app.IHwWindowCallback.Stub;
import huawei.android.os.HwGeneralManager;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwWindowManagerService extends WindowManagerService {
    static final boolean DEBUG = false;
    private static final int IBINDER_CODE_FREEZETHAWROTATION = 208;
    private static final int IBINDER_CODE_IS_KEYGUARD_DISABLE = 1000;
    private static final boolean IS_NOTCH_PROP = (SystemProperties.get("ro.config.hw_notch_size", "").equals("") ^ 1);
    private static final long MSG_ROG_FREEZE_TIME_DELEAYED = 6000;
    public static final int ROG_FREEZE_TIMEOUT = 100;
    private static final int SET_NAVIBAR_SHOWLEFT_TRANSACTION = 2201;
    private static final int SINGLE_HAND_STATE = 1989;
    private static final int SINGLE_HAND_SWITCH = 1990;
    static final String TAG = HwWindowManagerService.class.getSimpleName();
    public static final int UPDATE_NAVIGATIONBAR = 99;
    private boolean IS_SUPPORT_PRESSURE = false;
    final int TRANSACTION_GETTOUCHCOUNTINFO = 1006;
    final int TRANSACTION_isDimLayerVisible = 1007;
    final int TRANSACTION_isIMEVisble = 1004;
    final int TRANSACTION_registerWindowCallback = 1002;
    final int TRANSACTION_registerWindowObserver = 1009;
    final int TRANSACTION_setCoverState = 1008;
    final int TRANSACTION_unRegisterWindowCallback = 1003;
    final int TRANSACTION_unregisterWindowObserver = 1010;
    IWindow mCurrentWindow = null;
    private WindowState mCurrentWindowState = null;
    FaceRotationCallback mFaceRotationCallback = new FaceRotationCallback() {
        public void onEvent(int faceRotation) {
            HwWindowManagerService.this.updateRotationUnchecked(false, false);
        }
    };
    AppWindowToken mFocusedAppForNavi = null;
    protected int mFocusedDisplayId = -1;
    final Handler mHandler = new H(this);
    private boolean mHasRecord = false;
    private WindowState mHoldWindow;
    private Handler mHwHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 99:
                    HwWindowManagerService hwWindowManagerService = HwWindowManagerService.this;
                    boolean z = true;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    hwWindowManagerService.updateNavigationBar(z);
                    return;
                case 100:
                    Slog.d(HwWindowManagerService.TAG, "ROG_FREEZE_TIMEOUT");
                    SurfaceControl.unfreezeDisplay();
                    return;
                default:
                    return;
            }
        }
    };
    private RectF mImeDockShownFrame = new RectF();
    boolean mIsCoverOpen = true;
    long mLastRelayoutNotifyTime;
    private int mLayerIndex = -1;
    boolean mLayoutNaviBar = false;
    private LockPatternUtils mLockPatternUtils;
    private WindowState mPCHoldWindow;
    private final Runnable mReevaluateStatusBarSize = new Runnable() {
        public void run() {
            synchronized (HwWindowManagerService.this.mWindowMap) {
                HwWindowManagerService.this.mIgnoreFrozen = true;
                if (HwWindowManagerService.this.mLayoutNaviBar) {
                    HwWindowManagerService.this.mLayoutNaviBar = false;
                    HwWindowManagerService.this.mCurNaviConfiguration = HwWindowManagerService.this.computeNewConfigurationLocked(HwWindowManagerService.this.getDefaultDisplayContentLocked().getDisplayId());
                    if (HwWindowManagerService.this.mRoot.mWallpaperController.getWallpaperTarget() != null) {
                        HwWindowManagerService.this.mRoot.mWallpaperController.updateWallpaperVisibility();
                    }
                    HwWindowManagerService.this.performhwLayoutAndPlaceSurfacesLocked();
                } else {
                    HwWindowManagerService.this.performhwLayoutAndPlaceSurfacesLocked();
                }
            }
        }
    };
    long mRelayoutNotifyPeriod;
    private ArrayList<WindowState> mSecureScreenRecords = new ArrayList();
    private ArrayList<WindowState> mSecureScreenShot = new ArrayList();
    private volatile long mSetTime = 0;
    private SingleHandAdapter mSingleHandAdapter;
    private int mSingleHandSwitch;
    private boolean mSplitMode = false;
    private int mTempOrientation = -3;
    private AppWindowToken mTempToken = null;
    private final Handler mUiHandler;
    IWindowLayoutObserver mWindowLayoutObserver = null;

    public HwWindowManagerService(Context context, InputManagerService inputManager, boolean haveInputMethods, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy) {
        super(context, inputManager, haveInputMethods, showBootMsgs, onlyCore, policy);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUiHandler = UiThread.getHandler();
        HwGestureNavWhiteConfig.getInstance().initWmsServer(this, context);
    }

    private boolean judgeSingleHandSwitchBySize() {
        return this.mContext.getResources().getBoolean(34537473);
    }

    protected void setCropOnSingleHandMode(int singleHandleMode, boolean isMultiWindowApp, int dw, int dh, Rect crop) {
        float singleHandCutPercent = 1.0f - 0.75f;
        float verticalBlank = ((float) dh) * singleHandCutPercent;
        float horizontalBlank = ((float) dw) * singleHandCutPercent;
        if (singleHandleMode == 1) {
            crop.right -= (int) horizontalBlank;
        } else {
            crop.left += (int) horizontalBlank;
        }
        if (isMultiWindowApp) {
            if (crop.top == 0) {
                crop.top += (int) (((float) dh) * singleHandCutPercent);
            } else {
                crop.top = (int) ((((float) crop.top) * 0.75f) + (((float) dh) * singleHandCutPercent));
            }
            crop.bottom = (int) ((((float) crop.bottom) * 0.75f) + (((float) dh) * singleHandCutPercent));
            return;
        }
        if (crop.top > 0) {
            crop.top = (int) ((((float) crop.top) * 0.75f) + verticalBlank);
        } else {
            crop.top = (int) verticalBlank;
        }
        if (crop.bottom < dh) {
            crop.bottom = (int) (((float) crop.bottom) + (((float) (dh - crop.bottom)) * singleHandCutPercent));
        }
    }

    protected void hwProcessOnMatrix(int rotation, int width, int height, Rect frame, Matrix outMatrix) {
        switch (rotation) {
            case 1:
            case 3:
                outMatrix.postRotate(90.0f);
                outMatrix.postTranslate((float) width, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
                return;
            default:
                return;
        }
    }

    boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows) {
        boolean ret = super.updateFocusedWindowLocked(mode, updateInputWindows);
        HwGestureNavWhiteConfig.getInstance().updatewindow(this.mCurrentFocus);
        return ret;
    }

    public boolean isGestureNavMisTouch() {
        return HwGestureNavWhiteConfig.getInstance().isEnable();
    }

    public int addWindow(Session session, IWindow client, int seq, LayoutParams attrs, int viewVisibility, int displayId, Rect outFrame, Rect outContentInsets, Rect outStableInsets, Rect outOutsets, ParcelableWrapper outDisplayCutout, InputChannel outInputChannel) {
        String str;
        LayoutParams layoutParams = attrs;
        int i = displayId;
        if (layoutParams.type == 2101) {
            layoutParams.token = null;
        }
        if (HwPCUtils.isPcCastModeInServer() && this.mHardKeyboardAvailable && HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK == layoutParams.type) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addInputMethodWindow: displayId = ");
            stringBuilder.append(i);
            Slog.i(str, stringBuilder.toString());
        }
        int newDisplayId = HwPCUtils.getPCDisplayID();
        if (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(newDisplayId) && newDisplayId != i) {
            int newDisplayId2;
            StringBuilder stringBuilder2;
            if ("HwGlobalActions".equals(attrs.getTitle()) || "VolumeDialog".equals(attrs.getTitle()) || "com.ss.android.article.news".equals(layoutParams.packageName) || HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL == layoutParams.type || HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK == layoutParams.type || HwArbitrationDEFS.MSG_GAME_MPLINK_START_HIRADIO == layoutParams.type || 2003 == layoutParams.type) {
                newDisplayId2 = newDisplayId;
            } else if ((HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS == layoutParams.type && FingerViewController.PKGNAME_OF_KEYGUARD.equals(layoutParams.packageName)) || ((HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL == layoutParams.type && FingerViewController.PKGNAME_OF_KEYGUARD.equals(layoutParams.packageName)) || "com.google.android.marvin.talkback".equals(layoutParams.packageName))) {
                newDisplayId2 = newDisplayId;
            } else if (layoutParams.type >= 1000 && layoutParams.type <= 1999) {
                WindowState parentWindow = windowForClientLocked(null, layoutParams.token, false);
                if (parentWindow != null && parentWindow.mAttrs.type == HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("addSubWindow Title = ");
                    stringBuilder2.append(attrs.getTitle());
                    stringBuilder2.append("packageName = ");
                    stringBuilder2.append(layoutParams.packageName);
                    stringBuilder2.append(",setdisplayId = ");
                    stringBuilder2.append(newDisplayId);
                    stringBuilder2.append(" oldDisplayID=");
                    stringBuilder2.append(i);
                    HwPCUtils.log(str, stringBuilder2.toString());
                    return super.addWindow(session, client, seq, layoutParams, viewVisibility, newDisplayId, outFrame, outContentInsets, outStableInsets, outOutsets, outDisplayCutout, outInputChannel);
                }
            }
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("addWindow Title = ");
            stringBuilder2.append(attrs.getTitle());
            stringBuilder2.append("packageName = ");
            stringBuilder2.append(layoutParams.packageName);
            stringBuilder2.append(",setdisplayId = ");
            stringBuilder2.append(newDisplayId2);
            stringBuilder2.append(" oldDisplayID=");
            stringBuilder2.append(i);
            HwPCUtils.log(str, stringBuilder2.toString());
            return super.addWindow(session, client, seq, layoutParams, viewVisibility, newDisplayId2, outFrame, outContentInsets, outStableInsets, outOutsets, outDisplayCutout, outInputChannel);
        }
        return super.addWindow(session, client, seq, attrs, viewVisibility, displayId, outFrame, outContentInsets, outStableInsets, outOutsets, outDisplayCutout, outInputChannel);
    }

    public void setCoverManagerState(boolean isCoverOpen) {
        this.mIsCoverOpen = isCoverOpen;
        HwServiceFactory.setIfCoverClosed(isCoverOpen ^ 1);
    }

    public boolean isCoverOpen() {
        return this.mIsCoverOpen;
    }

    public void freezeOrThawRotation(int rotation) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        } else if (rotation < -1 || rotation > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("freezeRotationTemporarily: rotation=");
            stringBuilder.append(rotation);
            Slog.v(str, stringBuilder.toString());
            if (this.mPolicy instanceof HwPhoneWindowManager) {
                ((HwPhoneWindowManager) this.mPolicy).freezeOrThawRotation(rotation);
            }
            super.updateRotationUnchecked(false, false);
        }
    }

    public boolean isKeyguardOccluded() {
        if (this.mPolicy instanceof HwPhoneWindowManager) {
            return ((HwPhoneWindowManager) this.mPolicy).isKeyguardOccluded();
        }
        return false;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != HwMpLinkServiceImpl.MPLINK_MSG_WIFIPRO_SWITCH_ENABLE) {
            boolean z = false;
            if (code != SET_NAVIBAR_SHOWLEFT_TRANSACTION) {
                RuntimeException e;
                int i;
                switch (code) {
                    case 203:
                        data.enforceInterface("android.view.IWindowManager");
                        rotateWithHoldDialog();
                        reply.writeNoException();
                        return true;
                    case 204:
                        data.enforceInterface("android.view.IWindowManager");
                        this.mHwHandler.sendMessage(this.mHwHandler.obtainMessage(99, data.readInt(), 0));
                        reply.writeNoException();
                        return true;
                    case 205:
                        data.enforceInterface("android.view.IWindowManager");
                        if ((this.mPolicy instanceof HwPhoneWindowManager) != null) {
                            ((HwPhoneWindowManager) this.mPolicy).swipeFromTop();
                        }
                        reply.writeNoException();
                        return true;
                    case 206:
                        data.enforceInterface("android.view.IWindowManager");
                        e = null;
                        if (this.mPolicy instanceof HwPhoneWindowManager) {
                            e = ((HwPhoneWindowManager) this.mPolicy).isTopIsFullscreen();
                        }
                        if (e != null) {
                            i = 1;
                        }
                        reply.writeInt(i);
                        return true;
                    case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_DISCONNETED /*207*/:
                        data.enforceInterface("android.view.IWindowManager");
                        if ((this.mPolicy instanceof HwPhoneWindowManager) != null) {
                            ((HwPhoneWindowManager) this.mPolicy).showHwTransientBars();
                        }
                        return true;
                    default:
                        switch (code) {
                            case 1001:
                                data.enforceInterface("android.view.IWindowManager");
                                e = this.mLockPatternUtils.isLockScreenDisabled(0);
                                reply.writeNoException();
                                if (e != null) {
                                    i = 1;
                                }
                                reply.writeInt(i);
                                return true;
                            case 1002:
                                data.enforceInterface("android.view.IWindowManager");
                                e = Stub.asInterface(data.readStrongBinder());
                                if (this.mPolicy instanceof HwPhoneWindowManager) {
                                    ((HwPhoneWindowManager) this.mPolicy).setHwWindowCallback(e);
                                }
                                reply.writeNoException();
                                return true;
                            case 1003:
                                data.enforceInterface("android.view.IWindowManager");
                                if ((this.mPolicy instanceof HwPhoneWindowManager) != null) {
                                    ((HwPhoneWindowManager) this.mPolicy).setHwWindowCallback(null);
                                }
                                reply.writeNoException();
                                return true;
                            case 1004:
                                data.enforceInterface("android.view.IWindowManager");
                                if (this.mContext.checkPermission("com.huawei.permission.HUAWEI_IME_STATE_ACCESS", Binder.getCallingPid(), Binder.getCallingUid()) != null) {
                                    reply.writeInt(-1);
                                    return true;
                                }
                                e = (this.mInputMethodWindow == null || this.mInputMethodWindow.isVisibleLw() == null) ? null : 1;
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("imeVis=");
                                stringBuilder.append(e);
                                HwSlog.d(str, stringBuilder.toString());
                                reply.writeNoException();
                                if (e != null) {
                                    i = 1;
                                }
                                reply.writeInt(i);
                                return true;
                            default:
                                switch (code) {
                                    case 1006:
                                        data.enforceInterface("android.view.IWindowManager");
                                        if ((this.mPolicy instanceof HwPhoneWindowManager) == null) {
                                            Slog.w(TAG, "onTransct->current is not hw pwm");
                                            return true;
                                        } else if (this.mContext.checkPermission("com.huawei.permission.GET_TOUCH_COUNT_INFO", Binder.getCallingPid(), Binder.getCallingUid()) != null) {
                                            reply.writeIntArray(((HwPhoneWindowManager) this.mPolicy).getDefaultTouchCountInfo());
                                            return true;
                                        } else {
                                            reply.writeIntArray(((HwPhoneWindowManager) this.mPolicy).getTouchCountInfo());
                                            reply.writeNoException();
                                            return true;
                                        }
                                    case 1007:
                                        data.enforceInterface("android.view.IWindowManager");
                                        e = isDLayerVisible();
                                        reply.writeNoException();
                                        reply.writeInt(e);
                                        return true;
                                    case 1008:
                                        data.enforceInterface("android.view.IWindowManager");
                                        if (data.readInt() != null) {
                                            z = true;
                                        }
                                        setCoverManagerState(z);
                                        reply.writeNoException();
                                        return true;
                                    case 1009:
                                        data.enforceInterface("android.view.IWindowManager");
                                        registerWindowObserver(IWindowLayoutObserver.Stub.asInterface(data.readStrongBinder()), data.readLong());
                                        reply.writeNoException();
                                        return true;
                                    case 1010:
                                        data.enforceInterface("android.view.IWindowManager");
                                        unRegisterWindowObserver(IWindowLayoutObserver.Stub.asInterface(data.readStrongBinder()));
                                        reply.writeNoException();
                                        return true;
                                    default:
                                        switch (code) {
                                            case SINGLE_HAND_SWITCH /*1990*/:
                                                data.enforceInterface("android.view.IWindowManager");
                                                reply.writeNoException();
                                                reply.writeInt(this.mLazyModeOn);
                                                return true;
                                            case 1991:
                                                e = TAG;
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("mSingleHandSwitch =");
                                                stringBuilder2.append(this.mSingleHandSwitch);
                                                Slog.i(e, stringBuilder2.toString());
                                                this.mSingleHandSwitch = judgeSingleHandSwitchBySize();
                                                data.enforceInterface("android.view.IWindowManager");
                                                reply.writeNoException();
                                                reply.writeInt(this.mSingleHandSwitch);
                                                return true;
                                            default:
                                                try {
                                                    return super.onTransact(code, data, reply, flags);
                                                } catch (RuntimeException e2) {
                                                    if (!(e2 instanceof SecurityException)) {
                                                        Slog.w(TAG, "Window Manager Crash");
                                                    }
                                                    throw e2;
                                                }
                                        }
                                }
                        }
                }
            }
            data.enforceInterface("android.view.IWindowManager");
            if (this.mContext.checkPermission("com.huawei.permission.NAVIBAR_LEFT_WHENLAND", Binder.getCallingPid(), Binder.getCallingUid()) != null) {
                reply.writeInt(-1);
                return true;
            }
            if ((this.mPolicy instanceof HwPhoneWindowManager) != null) {
                if (data.readInt() == 1) {
                    z = true;
                }
                ((HwPhoneWindowManager) this.mPolicy).setNavibarAlignLeftWhenLand(z);
            }
            return true;
        }
        data.enforceInterface("android.view.IWindowManager");
        freezeOrThawRotation(data.readInt());
        reply.writeNoException();
        return true;
    }

    public int isDLayerVisible() {
        return 0;
    }

    public Bitmap getTaskSnapshotForPc(int displayId, IBinder binder, int taskId, int userId) {
        synchronized (this.mWindowMap) {
            DisplayContent dc = this.mRoot.getDisplayContent(displayId);
            if (dc == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[getTaskSnapshotForPc]fail to get displaycontent, displayId:");
                stringBuilder.append(displayId);
                stringBuilder.append(" taskId:");
                stringBuilder.append(taskId);
                HwPCUtils.log(str, stringBuilder.toString());
                return null;
            }
            AppWindowToken appWindowToken = dc.getAppWindowToken(binder);
            if (appWindowToken == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[getTaskSnapshotForPc]fail to get app window token, taskId:");
                stringBuilder2.append(taskId);
                HwPCUtils.log(str2, stringBuilder2.toString());
                return null;
            }
            Task task = appWindowToken.getTask();
            if (task == null) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[getTaskSnapshotForPc]fail to get task, taskId:");
                stringBuilder3.append(taskId);
                HwPCUtils.log(str3, stringBuilder3.toString());
                return null;
            }
            ArraySet<Task> taskSet = new ArraySet();
            taskSet.add(task);
            this.mTaskSnapshotController.snapshotTasks(taskSet);
            TaskSnapshot taskSnapshot = getTaskSnapshot(taskId, userId, null);
            if (taskSnapshot == null) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("[getTaskSnapshotForPc] to get snapshot, taskId:");
                stringBuilder4.append(taskId);
                HwPCUtils.log(str4, stringBuilder4.toString());
                return null;
            }
            Bitmap createHardwareBitmap = Bitmap.createHardwareBitmap(taskSnapshot.getSnapshot());
            return createHardwareBitmap;
        }
    }

    private void updateNavigationBar(boolean minNaviBar) {
        this.mPolicy.updateNavigationBar(minNaviBar);
        synchronized (this.mWindowMap) {
            if (mSupporInputMethodFilletAdaptation && getDefaultDisplayContentLocked().getRotation() == 0 && this.mInputMethodWindow != null && this.mInputMethodWindow.isImeWithHwFlag() && this.mInputMethodWindow.isVisible() && this.mInputMethodWindow.mWinAnimator.mInsetSurfaceOverlay != null) {
                if (minNaviBar) {
                    this.mInputMethodWindow.showInsetSurfaceOverlayImmediately();
                } else {
                    this.mHwHandler.postDelayed(new Runnable() {
                        public void run() {
                            synchronized (HwWindowManagerService.this.mWindowMap) {
                                if (HwWindowManagerService.this.mInputMethodWindow != null) {
                                    HwWindowManagerService.this.mInputMethodWindow.hideInsetSurfaceOverlayImmediately();
                                }
                            }
                        }
                    }, 300);
                }
            }
        }
    }

    public void setFocusedAppForNavi(IBinder token) {
        if (token == null) {
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Clearing focused app, was ");
                stringBuilder.append(this.mFocusedAppForNavi);
                Slog.v(str, stringBuilder.toString());
            }
            this.mFocusedAppForNavi = null;
        } else {
            AppWindowToken newFocus;
            synchronized (this.mWindowMap) {
                newFocus = this.mRoot.getAppWindowToken(token);
            }
            String str2;
            StringBuilder stringBuilder2;
            if (newFocus == null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Attempted to set focus to non-existing app token: ");
                stringBuilder2.append(token);
                Slog.w(str2, stringBuilder2.toString());
                return;
            }
            if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Set focused app to: ");
                stringBuilder2.append(newFocus);
                stringBuilder2.append(" old focus=");
                stringBuilder2.append(this.mFocusedAppForNavi);
                Slog.v(str2, stringBuilder2.toString());
            }
            if (this.mPolicy instanceof HwPhoneWindowManager) {
                HwPhoneWindowManager policy = this.mPolicy;
                if (policy.getHwWindowCallback() != null) {
                    ActivityRecord r = ActivityRecord.forToken(newFocus.token);
                    if (!(r == null || r.info.applicationInfo.packageName.equals("com.android.gallery3d"))) {
                        try {
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("setFocuedAppChanged for:");
                            stringBuilder3.append(r.info.applicationInfo.packageName);
                            Slog.d(str3, stringBuilder3.toString());
                            policy.getHwWindowCallback().focusedAppChanged();
                        } catch (Exception ex) {
                            Slog.w(TAG, "mIHwWindowCallback focusedAppChanged", ex);
                        }
                    }
                }
            }
            this.mFocusedAppForNavi = newFocus;
        }
    }

    public void setNaviBarFlag() {
        this.mPolicy.setInputMethodWindowVisible(this.mInputMethodWindow == null ? false : this.mInputMethodWindow.isVisibleLw());
        if (this.mFocusedAppForNavi != null) {
            this.mPolicy.setNaviBarFlag(this.mFocusedAppForNavi.navigationBarHide);
        }
    }

    public void reevaluateStatusBarSize(boolean layoutNaviBar) {
        synchronized (this.mWindowMap) {
            this.mLayoutNaviBar = layoutNaviBar;
            this.mH.post(this.mReevaluateStatusBarSize);
        }
    }

    public Configuration getCurNaviConfiguration() {
        return this.mCurNaviConfiguration;
    }

    private void rotateWithHoldDialog() {
        this.mHandler.removeMessages(17);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(17));
        this.mHandler.removeMessages(11);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(11));
    }

    public void systemReady() {
        super.systemReady();
        this.mSingleHandSwitch = judgeSingleHandSwitchBySize();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMS systemReady mSingleHandSwitch = ");
        stringBuilder.append(this.mSingleHandSwitch);
        Slog.i(str, stringBuilder.toString());
        if (this.mSingleHandSwitch > 0) {
            this.mSingleHandAdapter = new SingleHandAdapter(this.mContext, this.mHandler, this.mUiHandler, this);
            this.mSingleHandAdapter.registerLocked();
        }
        this.IS_SUPPORT_PRESSURE = HwGeneralManager.getInstance().isSupportForce();
    }

    public int getLazyMode() {
        return this.mLazyModeOn;
    }

    public void setLazyMode(int lazyMode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cur: ");
        stringBuilder.append(this.mLazyModeOn);
        stringBuilder.append(" to: ");
        stringBuilder.append(lazyMode);
        Slog.i(str, stringBuilder.toString());
        if (this.mLazyModeOn != lazyMode) {
            this.mHwWMSEx.reportLazyModeToIAware(lazyMode);
            this.mLazyModeOn = lazyMode;
        }
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        super.setCurrentUser(newUserId, currentProfileIds);
        ((HwInputManagerService) this.mInputManager).setCurrentUser(newUserId, currentProfileIds);
        if (this.mPolicy instanceof HwPhoneWindowManager) {
            ((HwPhoneWindowManager) this.mPolicy).setCurrentUser(newUserId, currentProfileIds);
        }
    }

    public void setForcedDisplayDensityAndSize(int displayId, int density, int width, int height) {
        int i = displayId;
        int i2 = density;
        int i3 = width;
        int i4 = height;
        super.setForcedDisplayDensityAndSize(displayId, density, width, height);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setForcedDisplayDensityAndSize size: ");
        stringBuilder.append(i3);
        stringBuilder.append("x");
        stringBuilder.append(i4);
        Slog.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setForcedDisplayDensityAndSize density: ");
        stringBuilder.append(i2);
        Slog.d(str, stringBuilder.toString());
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        } else if (i == 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mWindowMap) {
                    DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                    if (displayContent != null) {
                        i3 = Math.min(Math.max(i3, 200), displayContent.mInitialDisplayWidth * 2);
                        i4 = Math.min(Math.max(i4, 200), displayContent.mInitialDisplayHeight * 2);
                        displayContent.mBaseDisplayWidth = i3;
                        displayContent.mBaseDisplayHeight = i4;
                        displayContent.mBaseDisplayDensity = i2;
                        this.mHwHandler.removeMessages(100);
                        this.mHwHandler.sendEmptyMessageDelayed(100, MSG_ROG_FREEZE_TIME_DELEAYED);
                        updateResourceConfiguration(i, i2, i3, i4);
                        reconfigureDisplayLocked(displayContent);
                        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(i);
                        if (screenRotationAnimation != null) {
                            screenRotationAnimation.kill();
                        }
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(i3);
                        int MIN_WIDTH = 200;
                        stringBuilder2.append(",");
                        stringBuilder2.append(i4);
                        Global.putString(this.mContext.getContentResolver(), "display_size_forced", stringBuilder2.toString());
                        List<UserInfo> userList = UserManager.get(this.mContext).getUsers();
                        if (userList != null) {
                            int i5 = 0;
                            while (i5 < userList.size()) {
                                List<UserInfo> userList2 = userList;
                                Secure.putStringForUser(this.mContext.getContentResolver(), "display_density_forced", Integer.toString(density), ((UserInfo) userList.get(i5)).id);
                                i5++;
                                userList = userList2;
                                i = displayId;
                            }
                        }
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(i2);
                        stringBuilder3.append("");
                        SystemProperties.set("persist.sys.realdpi", stringBuilder3.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(i3);
                        stringBuilder3.append("");
                        SystemProperties.set("persist.sys.rog.width", stringBuilder3.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(i4);
                        stringBuilder3.append("");
                        SystemProperties.set("persist.sys.rog.height", stringBuilder3.toString());
                        if (IS_NOTCH_PROP) {
                            this.mDisplayManagerInternal.updateCutoutInfoForRog(0);
                            str = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("updateCutoutInfoForRog width: ");
                            stringBuilder3.append(i3);
                            stringBuilder3.append(" height ");
                            stringBuilder3.append(i4);
                            Slog.d(str, stringBuilder3.toString());
                        }
                    }
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            throw new IllegalArgumentException("Can only set the default display");
        }
    }

    public void updateResourceConfiguration(int displayId, int density, int width, int height) {
        if (density == 0) {
            Slog.e(TAG, "setForcedDisplayDensityAndSize density is 0");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setForcedDisplay and updateResourceConfiguration, density = ");
        stringBuilder.append(density);
        stringBuilder.append(" width = ");
        stringBuilder.append(width);
        stringBuilder.append(" height = ");
        stringBuilder.append(height);
        Slog.d(str, stringBuilder.toString());
        Configuration mTempResourceConfiguration = new Configuration(this.mRoot.getDisplayContent(displayId).getConfiguration());
        DisplayMetrics mTempMetrics = this.mContext.getResources().getDisplayMetrics();
        mTempResourceConfiguration.densityDpi = density;
        mTempResourceConfiguration.screenWidthDp = (width * 160) / density;
        mTempResourceConfiguration.smallestScreenWidthDp = (width * 160) / density;
        mTempMetrics.density = ((float) density) / 160.0f;
        mTempMetrics.densityDpi = density;
        this.mContext.getResources().updateConfiguration(mTempResourceConfiguration, mTempMetrics);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setForcedDisplay and updateResourceConfiguration, mTempResourceConfiguration is: ");
        stringBuilder2.append(mTempResourceConfiguration);
        Slog.d(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setForcedDisplay and updateResourceConfiguration, mTempMetrics is: ");
        stringBuilder2.append(mTempMetrics);
        Slog.d(str2, stringBuilder2.toString());
    }

    public void setPCScreenDisplayMode(int mode) {
        String propVal = "normal";
        switch (mode) {
            case 1:
                propVal = "minor";
                break;
            case 2:
                propVal = "smaller";
                break;
        }
        SystemProperties.set("hw.pc.display.mode", propVal);
        synchronized (this.mWindowMap) {
            performhwLayoutAndPlaceSurfacesLocked();
        }
    }

    public int getPCScreenDisplayMode() {
        String strMode = SystemProperties.get("hw.pc.display.mode");
        if (strMode.equals("minor")) {
            return 1;
        }
        if (strMode.equals("smaller")) {
            return 2;
        }
        return 0;
    }

    public boolean detectSafeMode() {
        if (HwDeviceManager.disallowOp(10)) {
            Slog.i(TAG, "safemode is disabled by dpm");
            this.mSafeMode = false;
            this.mPolicy.setSafeMode(this.mSafeMode);
            return this.mSafeMode;
        } else if (!"1".equals(SystemProperties.get("sys.bootfail.safemode"))) {
            return super.detectSafeMode();
        } else {
            Slog.i(TAG, "safemode is enabled eRecovery");
            this.mSafeMode = true;
            this.mPolicy.setSafeMode(this.mSafeMode);
            return this.mSafeMode;
        }
    }

    public boolean isSplitMode() {
        return this.mSplitMode;
    }

    public void setSplittable(boolean splittable) {
        this.mSplitMode = splittable;
    }

    public int getLayerIndex(String appName, int windowType) {
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        if (displayContent == null) {
            return -1;
        }
        try {
            displayContent.forAllWindows(new -$$Lambda$HwWindowManagerService$QU-cD2orMmsft5-oQxGQy29TJsU(this, appName), false);
        } catch (Exception e) {
            Slog.w(TAG, "getLayerIndex exception!");
        }
        return this.mLayerIndex;
    }

    public static /* synthetic */ void lambda$getLayerIndex$0(HwWindowManagerService hwWindowManagerService, String appName, WindowState ws) {
        if (ws.getWindowTag().toString().indexOf(appName) > -1) {
            hwWindowManagerService.mLayerIndex = ws.mLayer;
        }
    }

    public void setKeyguardGoingAway(boolean keyGuardGoingAway) {
        if (keyGuardGoingAway) {
            this.mSetTime = SystemClock.elapsedRealtime();
        }
        super.setKeyguardGoingAway(keyGuardGoingAway);
    }

    protected boolean shouldHideIMExitAnim(WindowState win) {
        boolean ret = SystemClock.elapsedRealtime() - this.mSetTime <= 500 && (win.mAttrs.type == HwArbitrationDEFS.MSG_GAME_MPLINK_START_HIRADIO || win.mAttrs.type == HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK);
        if (SystemClock.elapsedRealtime() - this.mSetTime < MemoryConstant.MIN_INTERVAL_OP_TIMEOUT && (win.mAttrs.type == HwArbitrationDEFS.MSG_GAME_MPLINK_START_HIRADIO || win.mAttrs.type == HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("KeyguardGoingAway:");
            stringBuilder.append(this.mKeyguardGoingAway);
            stringBuilder.append(";Realtime():");
            stringBuilder.append(SystemClock.elapsedRealtime());
            stringBuilder.append(";mSetTime:");
            stringBuilder.append(this.mSetTime);
            stringBuilder.append(";type:");
            stringBuilder.append(win.mAttrs.type);
            stringBuilder.append(" reval = ");
            stringBuilder.append(ret);
            stringBuilder.append(" win:");
            stringBuilder.append(win);
            Slog.d(str, stringBuilder.toString());
        }
        return ret;
    }

    public void registerWindowObserver(IWindowLayoutObserver observer, long period) throws RemoteException {
        if (!checkCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "registerWindowObserver()")) {
            return;
        }
        if (period <= 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerWindowObserver with wrong period ");
            stringBuilder.append(period);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        this.mRelayoutNotifyPeriod = period;
        if (this.mRelayoutNotifyPeriod < 500) {
            this.mRelayoutNotifyPeriod = 500;
        }
        this.mLastRelayoutNotifyTime = 0;
        this.mWindowLayoutObserver = observer;
        WindowState ws = super.getFocusedWindow();
        synchronized (this.mWindowMap) {
            for (Entry<IBinder, WindowState> entry : this.mWindowMap.entrySet()) {
                if (ws == entry.getValue()) {
                    this.mCurrentWindow = IWindow.Stub.asInterface((IBinder) entry.getKey());
                    break;
                }
            }
        }
        if (this.mCurrentWindow != null) {
            try {
                this.mCurrentWindow.registerWindowObserver(observer, period);
            } catch (RemoteException e) {
                Slog.w(TAG, "registerWindowObserver get RemoteException");
            }
        }
    }

    public void unRegisterWindowObserver(IWindowLayoutObserver observer) throws RemoteException {
        if (checkCallingPermission("com.huawei.permission.CONTENT_SENSOR_PERMISSION", "unRegisterWindowObserver()")) {
            if (this.mCurrentWindow != null) {
                try {
                    this.mCurrentWindow.unRegisterWindowObserver(observer);
                } catch (RemoteException e) {
                    Slog.w(TAG, "unRegisterWindowObserver get RemoteException");
                }
            }
            this.mWindowLayoutObserver = null;
            this.mCurrentWindow = null;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unRegisterWindowObserver OK, observer = ");
            stringBuilder.append(observer);
            Slog.d(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:24:0x004a, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:40:0x007d, code skipped:
            if (r1.size() <= 0) goto L_0x00a2;
     */
    /* JADX WARNING: Missing block: B:41:0x007f, code skipped:
            r2 = r1.size() - 1;
     */
    /* JADX WARNING: Missing block: B:42:0x0084, code skipped:
            if (r2 < 0) goto L_0x00b3;
     */
    /* JADX WARNING: Missing block: B:43:0x0086, code skipped:
            r3 = ((java.lang.Integer) r1.get(r2)).intValue();
     */
    /* JADX WARNING: Missing block: B:44:0x0090, code skipped:
            if (r3 < 0) goto L_0x009f;
     */
    /* JADX WARNING: Missing block: B:45:0x0092, code skipped:
            r7.mHandler.sendMessage(r7.mHandler.obtainMessage(104, r3, 0));
     */
    /* JADX WARNING: Missing block: B:46:0x009f, code skipped:
            r2 = r2 - 1;
     */
    /* JADX WARNING: Missing block: B:48:0x00a6, code skipped:
            if (android.util.HwPCUtils.isValidExtDisplayId(r8) == false) goto L_0x00b3;
     */
    /* JADX WARNING: Missing block: B:50:0x00ae, code skipped:
            if (r10.equals("handleTapOutsideTask-1-1") == false) goto L_0x00b3;
     */
    /* JADX WARNING: Missing block: B:51:0x00b0, code skipped:
            setPCLauncherFocused(true);
     */
    /* JADX WARNING: Missing block: B:53:0x00b7, code skipped:
            if (r0 == getPCLauncherFocused()) goto L_0x00d4;
     */
    /* JADX WARNING: Missing block: B:55:0x00bf, code skipped:
            if (r10.equals("handleTapOutsideTaskXY") != false) goto L_0x00d4;
     */
    /* JADX WARNING: Missing block: B:56:0x00c1, code skipped:
            r2 = r7.mWindowMap;
     */
    /* JADX WARNING: Missing block: B:57:0x00c3, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:59:?, code skipped:
            r3 = r7.mRoot.getDisplayContent(r8);
     */
    /* JADX WARNING: Missing block: B:60:0x00ca, code skipped:
            if (r3 == null) goto L_0x00cf;
     */
    /* JADX WARNING: Missing block: B:61:0x00cc, code skipped:
            r3.layoutAndAssignWindowLayersIfNeeded();
     */
    /* JADX WARNING: Missing block: B:62:0x00cf, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:67:0x00d4, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setFocusedDisplay(int displayId, boolean findTopTask, String reason) {
        boolean oldPCLauncherFocused = getPCLauncherFocused();
        List<Integer> tasks = new ArrayList();
        synchronized (this.mWindowMap) {
            DisplayContent dc = this.mRoot.getDisplayContent(displayId);
            if (dc == null) {
            } else if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad() && "lockScreen".equals(reason)) {
            } else if (dc.getDisplayId() != this.mFocusedDisplayId || (HwPCUtils.isPcCastModeInServer() && HwPCUtils.enabledInPad() && "unlockScreen".equals(reason))) {
                this.mFocusedDisplayId = dc.getDisplayId();
                if (this.mFocusedDisplayId == 0) {
                    setPCLauncherFocused(false);
                }
                if (findTopTask && (dc instanceof HwDisplayContent)) {
                    tasks = ((HwDisplayContent) dc).taskIdFromTop();
                }
                if (!HwPCUtils.enabledInPad() && this.mHardKeyboardAvailable) {
                    relaunchIMEProcess();
                }
                updateFocusedWindowLocked(0, true);
            }
        }
    }

    void notifyHardKeyboardStatusChange() {
        super.notifyHardKeyboardStatusChange();
        if (!HwPCUtils.enabledInPad()) {
            relaunchIMEProcess();
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0019, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getWindowSystemUiVisibility(IBinder token) {
        synchronized (this.mWindowMap) {
            AppWindowToken appToken = this.mRoot.getAppWindowToken(token);
            if (appToken != null) {
                WindowState win = appToken.findMainWindow();
                if (win != null) {
                    int systemUiVisibility = win.getSystemUiVisibility();
                    return systemUiVisibility;
                }
            }
        }
    }

    public void setPCLauncherFocused(boolean focus) {
        synchronized (this.mWindowMap) {
            if (focus == this.mPCLauncherFocused) {
                return;
            }
            this.mPCLauncherFocused = focus;
        }
    }

    private void relaunchIMEProcess() {
        if (this.mPCManager == null) {
            this.mPCManager = HwPCUtils.getHwPCManager();
        }
        if (this.mPCManager != null) {
            try {
                this.mPCManager.relaunchIMEIfNecessary();
            } catch (RemoteException e) {
                Log.e(TAG, "relaunchIMEProcess()");
            }
        }
    }

    public int getFocusedDisplayId() {
        return HwPCUtils.isPcCastModeInServer() ? this.mFocusedDisplayId : 0;
    }

    public void togglePCMode(boolean pcmode, int displayId) {
        if (pcmode) {
            if (this.mPolicy instanceof HwPhoneWindowManager) {
                HwPCUtils.log(TAG, "registerExternalPointerEventListener for screenlock");
                ((HwPhoneWindowManager) this.mPolicy).registerExternalPointerEventListener();
            }
            return;
        }
        if (this.mPolicy instanceof HwPhoneWindowManager) {
            HwPCUtils.log(TAG, "unRegisterExternalPointerEventListener for screenlock");
            ((HwPhoneWindowManager) this.mPolicy).unRegisterExternalPointerEventListener();
        }
        synchronized (this.mWindowMap) {
            DisplayContent dc = this.mRoot.getDisplayContent(displayId);
            if (dc != null && (dc instanceof HwDisplayContent)) {
                ((HwDisplayContent) dc).togglePCMode(pcmode);
            }
        }
        setFocusedDisplay(0, true, "resetToDefault");
    }

    public Bitmap getDisplayBitmap(int displayId, int width, int height) {
        return SurfaceControl.screenshot(this.mDisplayManagerInternal.getDisplayToken(displayId), width, height);
    }

    void getStableInsetsLocked(int displayId, Rect outInsets) {
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            outInsets.setEmpty();
            DisplayContent dc = this.mRoot.getDisplayContent(displayId);
            if (dc != null) {
                DisplayInfo di = dc.getDisplayInfo();
                this.mPolicy.getStableInsetsLw(di.rotation, di.logicalWidth, di.logicalHeight, outInsets, displayId, di.displayCutout);
            }
            return;
        }
        super.getStableInsetsLocked(displayId, outInsets);
    }

    public DisplayManager getDisplayManager() {
        return this.mDisplayManager;
    }

    public WindowManagerPolicy getPolicy() {
        return this.mPolicy;
    }

    private boolean isDisplayIdInVrMode(int displayId) {
        if (this.mDisplayManager != null) {
            Display display = this.mDisplayManager.getDisplay(displayId);
            if (display != null) {
                DisplayInfo displayInfo = new DisplayInfo();
                if (display.getDisplayInfo(displayInfo)) {
                    int width = displayInfo.getNaturalWidth();
                    int height = displayInfo.getNaturalHeight();
                    if (width == 2880 && height == 1600) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void onDisplayAdded(int displayId) {
        super.onDisplayAdded(displayId);
        if (isDisplayIdInVrMode(displayId)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDisplayAdded, displayId = ");
            stringBuilder.append(displayId);
            stringBuilder.append(" is VR mode");
            Log.i(str, stringBuilder.toString());
            HwVRUtils.setVRDisplayID(displayId, true);
            return;
        }
        if (this.mPCManager == null) {
            this.mPCManager = HwPCUtils.getHwPCManager();
        }
        if (this.mPCManager != null) {
            try {
                this.mPCManager.scheduleDisplayAdded(displayId);
            } catch (RemoteException e) {
                Log.e(TAG, "onDisplayAdded()");
            }
        }
        sendDisplayStateBroadcast(true, displayId);
    }

    public void onDisplayChanged(int displayId) {
        super.onDisplayChanged(displayId);
        if (isDisplayIdInVrMode(displayId)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDisplayChanged, displayId = ");
            stringBuilder.append(displayId);
            stringBuilder.append(" is VR mode");
            Log.i(str, stringBuilder.toString());
            HwVRUtils.setVRDisplayID(displayId, true);
            return;
        }
        if (this.mPCManager == null) {
            this.mPCManager = HwPCUtils.getHwPCManager();
        }
        if (this.mPCManager != null) {
            try {
                this.mPCManager.scheduleDisplayChanged(displayId);
            } catch (RemoteException e) {
                Log.e(TAG, "onDisplayChanged()");
            }
        }
    }

    public void onDisplayRemoved(int displayId) {
        super.onDisplayRemoved(displayId);
        if (HwVRUtils.isValidVRDisplayId(displayId)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDisplayRemoved, displayId = ");
            stringBuilder.append(displayId);
            stringBuilder.append(" is VR mode");
            Log.i(str, stringBuilder.toString());
            HwVRUtils.setVRDisplayID(-1, false);
            HwFrameworkFactory.getVRSystemServiceManager().setVirtualScreenMode(false);
            return;
        }
        if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(displayId)) {
            setFocusedDisplay(0, true, "resetToDefault");
        }
        if (this.mPolicy instanceof HwPhoneWindowManager) {
            this.mPolicy.resetCurrentNaviBarHeightExternal();
        }
        if (this.mPCManager == null) {
            this.mPCManager = HwPCUtils.getHwPCManager();
        }
        if (this.mPCManager != null) {
            try {
                this.mPCManager.scheduleDisplayRemoved(displayId);
            } catch (RemoteException e) {
                Log.e(TAG, "onDisplayRemoved()");
            }
        }
        sendDisplayStateBroadcast(false, displayId);
    }

    /* JADX WARNING: Missing block: B:23:0x0086, code skipped:
            super.addWindowToken(r6, r7, r8);
     */
    /* JADX WARNING: Missing block: B:24:0x0089, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addWindowToken(IBinder binder, int type, int displayId) {
        if (checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addWindowToken()")) {
            synchronized (this.mWindowMap) {
                if (HwPCUtils.isPcCastModeInServer() && type == HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK) {
                    String str;
                    StringBuilder stringBuilder;
                    if (this.mHardKeyboardAvailable) {
                        displayId = getFocusedDisplayId();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("addInputMethodWindowToken: displayId = ");
                        stringBuilder.append(displayId);
                        Slog.i(str, stringBuilder.toString());
                    }
                    if (HwPCUtils.enabledInPad()) {
                        displayId = HwPCUtils.getPCDisplayID();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("addWindowToken: displayId = ");
                        stringBuilder.append(displayId);
                        Slog.v(str, stringBuilder.toString());
                    }
                }
                if (HwPCUtils.isValidExtDisplayId(displayId) && this.mDisplayManager.getDisplay(displayId) == null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("addWindowToken: Attempted to add binder token: ");
                    stringBuilder2.append(binder);
                    stringBuilder2.append(" for non-exiting displayId=");
                    stringBuilder2.append(displayId);
                    Slog.w("WindowManager", stringBuilder2.toString());
                    return;
                }
            }
        } else {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
    }

    protected boolean isTokenFound(IBinder binder, DisplayContent dc) {
        if (!HwPCUtils.isPcCastModeInServer()) {
            return false;
        }
        for (int i = 0; i < this.mRoot.mChildren.size(); i++) {
            DisplayContent displayContent = (DisplayContent) this.mRoot.mChildren.get(i);
            if (displayContent.getDisplayId() != dc.getDisplayId()) {
                WindowToken windowToken = displayContent.getWindowToken(binder);
                if (windowToken != null && windowToken.windowType == HwArbitrationDEFS.MSG_ARBITRATION_REQUEST_MPLINK) {
                    displayContent.removeWindowToken(binder);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeWindowToken isTokenFound in display:");
                    stringBuilder.append(displayContent.getDisplayId());
                    HwPCUtils.log(str, stringBuilder.toString());
                    return true;
                }
            }
        }
        return false;
    }

    boolean isSecureLocked(WindowState w) {
        if (HwPCUtils.isExtDynamicStack(w.getStackId()) && (w.getDisplayInfo().flags & 2) == 0) {
            return false;
        }
        return super.isSecureLocked(w);
    }

    protected boolean isDisplayOkForAnimation(int width, int height, int transit, AppWindowToken atoken) {
        HwForceRotationManager forceRotationManager = HwForceRotationManager.getDefault();
        if (forceRotationManager.isForceRotationSupported() && forceRotationManager.isForceRotationSwitchOpen(this.mContext) && width > height && ((transit == 7 || transit == 6) && forceRotationManager.isAppForceLandRotatable(atoken.appPackageName, atoken.appToken.asBinder()))) {
            return false;
        }
        return true;
    }

    protected boolean checkAppOrientationForForceRotation(AppWindowToken aToken) {
        HwForceRotationManager forceRotationManager = HwForceRotationManager.getDefault();
        if (!forceRotationManager.isForceRotationSupported() || !forceRotationManager.isForceRotationSwitchOpen(this.mContext) || aToken == null) {
            return false;
        }
        int or = aToken.mOrientation;
        if (!(aToken == this.mTempToken && or == this.mTempOrientation)) {
            this.mHasRecord = forceRotationManager.saveOrUpdateForceRotationAppInfo(aToken.appPackageName, aToken.appComponentName, aToken.appToken.asBinder(), or);
            this.mTempToken = aToken;
            this.mTempOrientation = or;
        }
        if (!this.mHasRecord) {
            return false;
        }
        if (or != 1 && or != 7 && or != 9 && or != 12) {
            return false;
        }
        forceRotationManager.showToastIfNeeded(aToken.appPackageName, aToken.appPid, aToken.appProcessName, aToken.appToken.asBinder());
        return true;
    }

    public void showWallpaperIfNeed(WindowState w) {
        HwForceRotationManager forceRotationManager = HwForceRotationManager.getDefault();
        if (!forceRotationManager.isForceRotationSupported() || !forceRotationManager.isForceRotationSwitchOpen(this.mContext) || w == null || w.isInMultiWindowMode()) {
            return;
        }
        if (w.mAppToken == null || !forceRotationManager.isAppForceLandRotatable(w.mAppToken.appPackageName, w.mAppToken.appToken.asBinder())) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("current window do not support force rotation mAppToken:");
            stringBuilder.append(w.mAppToken);
            Slog.v(str, stringBuilder.toString());
            return;
        }
        DisplayContent dc = getDefaultDisplayContentLocked();
        if (dc != null) {
            Display dp = dc.getDisplay();
            if (dp != null) {
                DisplayMetrics dm = new DisplayMetrics();
                dp.getMetrics(dm);
                LayoutParams layoutParams;
                if (dm.widthPixels < dm.heightPixels) {
                    layoutParams = w.mAttrs;
                    layoutParams.flags &= -1048577;
                } else {
                    layoutParams = w.mAttrs;
                    layoutParams.flags |= HighBitsCompModeID.MODE_COLOR_ENHANCE;
                }
            }
        }
    }

    public void prepareForForceRotation(IBinder token, String packageName, int pid, String processName) {
        if (HwForceRotationManager.getDefault().isForceRotationSupported()) {
            synchronized (this.mWindowMap) {
                AppWindowToken aToken = this.mRoot.getAppWindowToken(token);
                if (aToken == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempted to set orientation of non-existing app token: ");
                    stringBuilder.append(token);
                    Slog.w(str, stringBuilder.toString());
                    return;
                }
                aToken.appPackageName = packageName;
                aToken.appPid = pid;
                aToken.appProcessName = processName;
            }
        }
    }

    protected void setHwSecureScreen(WindowState win) {
        WindowStateAnimator winAnimator = win.mWinAnimator;
        if (winAnimator.mSurfaceController != null) {
            if ((win.mAttrs.hwFlags & 4096) != 0) {
                if (!this.mSecureScreenShot.contains(win)) {
                    winAnimator.mSurfaceController.setSecureScreenShot(true);
                    this.mSecureScreenShot.add(win);
                }
            } else if (this.mSecureScreenShot.contains(win)) {
                this.mSecureScreenShot.remove(win);
                if (this.mSecureScreenShot.size() == 0) {
                    winAnimator.mSurfaceController.setSecureScreenShot(false);
                }
            }
            if ((win.mAttrs.hwFlags & 8192) != 0) {
                if (!this.mSecureScreenRecords.contains(win)) {
                    winAnimator.mSurfaceController.setSecureScreenRecord(true);
                    this.mSecureScreenRecords.add(win);
                }
            } else if (this.mSecureScreenRecords.contains(win)) {
                this.mSecureScreenRecords.remove(win);
                if (this.mSecureScreenRecords.size() == 0) {
                    winAnimator.mSurfaceController.setSecureScreenRecord(false);
                }
            }
        }
    }

    private void sendDisplayStateBroadcast(boolean isAdded, int displayId) {
        if (!(!SystemProperties.getBoolean("ro.config.vrbroad", false) || displayId == 0 || this.mContext == null)) {
            Intent intent = new Intent();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("send broadcast displayState, displayId:");
            stringBuilder.append(displayId);
            stringBuilder.append(" isAdded:");
            stringBuilder.append(isAdded);
            Log.i(str, stringBuilder.toString());
            intent.setAction("com.huawei.display.vr.added");
            intent.setPackage("com.huawei.vrservice");
            intent.putExtra("displayId", displayId);
            if (isAdded) {
                intent.putExtra("displayState", PreciseIgnore.COMP_SCREEN_ON_VALUE_);
            } else {
                intent.putExtra("displayState", "off");
            }
            this.mContext.sendBroadcast(intent, "com.huawei.display.vr.permission");
        }
    }

    public void updateFingerprintSlideSwitch() {
        if (HwPCUtils.enabled() && (this.mInputManager instanceof HwInputManagerService)) {
            this.mInputManager.updateFingerprintSlideSwitchValue();
        }
    }

    public void startIntelliServiceFR(final int orientation) {
        this.mHwHandler.post(new Runnable() {
            public void run() {
                if (IntelliServiceManager.isIntelliServiceEnabled(HwWindowManagerService.this.mContext, orientation, HwWindowManagerService.this.mCurrentUserId)) {
                    IntelliServiceManager.getInstance(HwWindowManagerService.this.mContext).startIntelliService(HwWindowManagerService.this.mFaceRotationCallback);
                } else {
                    IntelliServiceManager.getInstance(HwWindowManagerService.this.mContext).setKeepPortrait(false);
                }
            }
        });
    }

    public InputConsumer createInputConsumer(Looper looper, String name, Factory inputEventReceiverFactory) {
        if (name != null) {
            return super.createInputConsumer(looper, name, inputEventReceiverFactory);
        }
        Slog.e(TAG, "createInputConsumer name is null");
        return null;
    }

    public boolean inputMethodClientHasFocus(IInputMethodClient client) {
        if (client != null) {
            return super.inputMethodClientHasFocus(client);
        }
        Slog.e(TAG, "inputMethodClientHasFocus name is null");
        return false;
    }

    public IWindowSession openSession(IWindowSessionCallback callback, IInputMethodClient client, IInputContext inputContext) {
        if (client != null) {
            return super.openSession(callback, client, inputContext);
        }
        Slog.e(TAG, "openSession client is null");
        return null;
    }

    public void setDockedStackDividerTouchRegion(Rect touchRegion) {
        if (touchRegion == null) {
            Slog.e(TAG, "setDockedStackDividerTouchRegion touchRegion is null");
        } else {
            super.setDockedStackDividerTouchRegion(touchRegion);
        }
    }

    public void removeRotationWatcher(IRotationWatcher watcher) {
        if (watcher == null) {
            Slog.e(TAG, "removeRotationWatcher watcher is null");
        } else {
            super.removeRotationWatcher(watcher);
        }
    }

    public int watchRotation(IRotationWatcher watcher, int displayId) {
        if (watcher != null) {
            return super.watchRotation(watcher, displayId);
        }
        Slog.e(TAG, "watchRotation watcher is null");
        return 0;
    }

    void setHoldScreenLocked(Session newHoldScreen) {
        super.setHoldScreenLocked(newHoldScreen);
        if (HwPCUtils.isPcCastModeInServer()) {
            boolean hold = newHoldScreen != null;
            boolean pcState = this.mPCHoldingScreenWakeLock.isHeld();
            if (hold) {
                WindowState holdWindow = this.mRoot.mHoldScreenWindow;
                if (HwPCUtils.isValidExtDisplayId(holdWindow.getDisplayId())) {
                    this.mPCHoldWindow = holdWindow;
                } else {
                    this.mHoldWindow = holdWindow;
                }
            }
            if (hold != pcState) {
                if (!hold) {
                    this.mPCHoldingScreenWakeLock.release();
                } else if (HwPCUtils.isValidExtDisplayId(this.mRoot.mHoldScreenWindow.getDisplayId())) {
                    this.mPCHoldingScreenWakeLock.acquire();
                }
            }
            if (this.mHoldWindow == null || !this.mHoldWindow.isVisibleLw() || (this.mHoldWindow.mAttrs.flags & 128) == 0) {
                this.mHoldWindow = null;
                this.mHoldingScreenWakeLock.release();
            }
            if (this.mPCHoldWindow == null || !this.mPCHoldWindow.isVisibleLw() || (this.mPCHoldWindow.mAttrs.flags & 128) == 0) {
                this.mPCHoldWindow = null;
                this.mPCHoldingScreenWakeLock.release();
            }
        }
    }

    public ArrayList<WindowState> getSecureScreenWindow() {
        ArrayList<WindowState> secureScreenWindow = new ArrayList();
        secureScreenWindow.addAll(this.mSecureScreenRecords);
        secureScreenWindow.addAll(this.mSecureScreenShot);
        return secureScreenWindow;
    }

    public void removeSecureScreenWindow(WindowState win) {
        if (win != null) {
            WindowStateAnimator winAnimator = win.mWinAnimator;
            if (winAnimator != null && winAnimator.mSurfaceController != null) {
                if (this.mSecureScreenRecords.contains(win)) {
                    this.mSecureScreenRecords.remove(win);
                    if (this.mSecureScreenRecords.size() == 0) {
                        winAnimator.mSurfaceController.setSecureScreenRecord(false);
                    }
                }
                if (this.mSecureScreenShot.contains(win)) {
                    this.mSecureScreenShot.remove(win);
                    if (this.mSecureScreenShot.size() == 0) {
                        winAnimator.mSurfaceController.setSecureScreenShot(false);
                    }
                }
            }
        }
    }
}
