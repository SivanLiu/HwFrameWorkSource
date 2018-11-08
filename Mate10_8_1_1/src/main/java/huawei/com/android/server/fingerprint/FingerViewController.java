package huawei.com.android.server.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Flog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.server.gesture.GestureNavConst;
import com.android.server.security.trustcircle.tlv.command.register.RET_REG_CANCEL;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.displayengine.DisplayEngineManager;
import huawei.android.aod.IAodManager;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;
import huawei.android.hwcolorpicker.HwColorPicker;
import huawei.com.android.server.fingerprint.fingerprintAnimation.BreathImageDrawable;
import huawei.com.android.server.policy.BlurUtils;
import java.util.List;

public class FingerViewController {
    private static final int ALPHA_ANIMATION_THRESHOLD = 15;
    private static final float ALPHA_LIMITED = 0.863f;
    private static final String APS_INIT_HEIGHT = "aps_init_height";
    private static final String APS_INIT_WIDTH = "aps_init_width";
    private static final int BLUR_RADIO = 25;
    private static final float BLUR_SCALE = 0.125f;
    private static final int BRIGHTNESS_LIFT_TIME_LONG = 200;
    private static final int BRIGHTNESS_LIFT_TIME_SHORT = 16;
    private static final int CAPTURE_BRIGHTNESS = 248;
    private static final int COLOR_BLACK = -16250872;
    private static int DEFAULT_BRIGHTNESS = 56;
    private static final int DEFAULT_INIT_HEIGHT = 2880;
    private static final int DEFAULT_INIT_WIDTH = 1440;
    public static final int FINGERPRINT_ACQUIRED_VENDOR = 6;
    public static final int FINGERPRINT_HELP_PAUSE_VENDORCODE = 11;
    public static final int FINGERPRINT_HELP_RESUME_VENDORCODE = 12;
    private static final String FINGERPRINT_IN_DISPLAY = "com.huawei.android.fingerprint.action.FINGERPRINT_IN_DISPLAY";
    private static final int FINGERPRINT_IN_DISPLAY_HELPCODE_CLOSE_VIEW_VALUE = 1011;
    private static final String FINGERPRINT_IN_DISPLAY_HELPCODE_KEY = "helpCode";
    private static final int FINGERPRINT_IN_DISPLAY_HELPCODE_SHOW_VIEW_VALUE = 1012;
    private static final int FINGERPRINT_IN_DISPLAY_HELPCODE_USE_PASSWORD_VALUE = 1010;
    private static final String FINGERPRINT_IN_DISPLAY_HELPSTRING_CLOSE_VIEW_VALUE = "finegrprint view closed";
    private static final String FINGERPRINT_IN_DISPLAY_HELPSTRING_KEY = "helpString";
    private static final String FINGERPRINT_IN_DISPLAY_HELPSTRING_SHOW_VIEW_VALUE = "finegrprint view show";
    private static final String FINGERPRINT_IN_DISPLAY_HELPSTRING_USE_PASSWORD_VALUE = "please use password";
    public static final int FINGERPRINT_USE_PASSWORD_ERROR_CODE = 10;
    private static final String FINGRPRINT_IMAGE_TITLE_NAME = "hw_ud_fingerprint_image";
    private static final String FINGRPRINT_VIEW_TITLE_NAME = "hw_ud_fingerprint_mask_hwsinglemode_window";
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE = 1;
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE_CANCELED = 4;
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE_SETTINGS = 3;
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE_SUCCESS = 2;
    public static final int HIGHLIGHT_TYPE_ENROLL = 0;
    public static final int HIGHLIGHT_TYPE_UNDEFINED = -1;
    public static final int HIGHLIGHT_VIEW_REMOVE_TIME = 3;
    private static final String HIGHLIGHT_VIEW_TITLE_NAME = "fingerprint_alpha_layer";
    private static final int INITIAL_BRIGHTNESS = -1;
    private static final int MAX_CAPTURE_TIME = 1000;
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED = 5;
    private static final String[] PACKAGES_USE_HWAUTH_INTERFACE = new String[]{"com.huawei.hwid", "com.huawei.wallet", "com.huawei.android.hwpay"};
    public static final String PKGNAME_OF_KEYGUARD = "com.android.systemui";
    private static final String PKGNAME_OF_SYSTEMMANAGER = "com.huawei.systemmanager";
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_CHECKING = 3;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_RESULT_FAIL = 1;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_RESULT_SUCCESS = 0;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_UNCHECKED = 2;
    private static final int SCREEN_AUTO_BRIGHTNESS = 1;
    private static final String TAG = "FingerViewController";
    public static final int TIME_UNIT = 1000;
    public static final int TYPE_DISMISS_ALL = 0;
    public static final int TYPE_FINGERPRINT_BUTTON = 2;
    public static final int TYPE_FINGERPRINT_VIEW = 1;
    private static final String UIDNAME_OF_KEYGUARD = "android.uid.systemui";
    private static final byte VIEW_STATE_DISABLED = (byte) 2;
    private static final byte VIEW_STATE_HIDE = (byte) 0;
    private static final byte VIEW_STATE_SHOW = (byte) 1;
    private static final byte VIEW_TYPE_BUTTON = (byte) 1;
    private static final byte VIEW_TYPE_FINGER_IMAGE = (byte) 3;
    private static final byte VIEW_TYPE_FINGER_MASK = (byte) 0;
    private static final byte VIEW_TYPE_HIGHLIGHT = (byte) 2;
    public static final int WAITTING_TIME = 30;
    private static FingerViewController sInstance;
    private boolean highLightViewAdded = false;
    private final Runnable mAddButtonViewRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.createAndAddButtonView();
        }
    };
    private final Runnable mAddFingerViewRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.createFingerprintView();
        }
    };
    private final Runnable mAddImageOnlyRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.mDefaultDisplayHeight = Global.getInt(FingerViewController.this.mContext.getContentResolver(), FingerViewController.APS_INIT_HEIGHT, FingerViewController.DEFAULT_INIT_HEIGHT);
            FingerViewController.this.mDefaultDisplayWidth = Global.getInt(FingerViewController.this.mContext.getContentResolver(), FingerViewController.APS_INIT_WIDTH, 1440);
            FingerViewController.this.createImageOnlyView();
        }
    };
    private BreathImageDrawable mAlipayDrawable;
    private IAodManager mAodService;
    private int mAuthenticateResult;
    private Bitmap mBLurBitmap;
    private BitmapDrawable mBlurDrawable;
    private int mButtonCenterX = -1;
    private int mButtonCenterY = -1;
    private SuspensionButton mButtonView;
    private boolean mButtonViewAdded = false;
    private int mButtonViewState = 1;
    private RelativeLayout mCancelView;
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mCurrentHeight;
    private int mCurrentRotation;
    private int mCurrentWidth;
    private int mDefaultDisplayHeight;
    private int mDefaultDisplayWidth;
    private DisplayEngineManager mDisplayEngineManager;
    private FingerprintView mFingerView;
    private ICallBack mFingerViewChangeCallback;
    private LayoutParams mFingerViewParams;
    private int mFingerprintCenterX = -1;
    private int mFingerprintCenterY = -1;
    private ImageView mFingerprintImageForAlipay;
    private FingerprintManagerEx mFingerprintManagerEx;
    private boolean mFingerprintOnlyViewAdded = false;
    private int[] mFingerprintPosition = new int[4];
    private ImageView mFingerprintView;
    private boolean mFingerprintViewAdded = false;
    private Handler mHandler = null;
    protected final HandlerThread mHandlerThread = new HandlerThread(TAG);
    private boolean mHasBackFingerprint = false;
    private boolean mHasUdFingerprint = true;
    private int mHighLightRemoveType;
    private int mHighLightShowType;
    private HighLightMaskView mHighLightView;
    private final Runnable mHighLightViewRunnable = new Runnable() {
        public void run() {
            int brightness = FingerViewController.this.getBrightness();
            switch (FingerViewController.this.mHighLightShowType) {
                case 0:
                case 3:
                    Log.d(FingerViewController.TAG, " SETTINGS enter");
                    FingerViewController.this.createAndAddHighLightView(-1);
                    FingerViewController.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            FingerViewController.this.setLightLevel(FingerViewController.CAPTURE_BRIGHTNESS, 100);
                        }
                    }, 100);
                    if (FingerViewController.this.mHighLightView != null) {
                        int endAlpha = FingerViewController.this.mHighLightView.getMaskAlpha(brightness);
                        Log.d(FingerViewController.TAG, " alpha:" + endAlpha);
                        FingerViewController.this.startAlphaValueAnimation(FingerViewController.this.mHighLightView, true, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, ((float) endAlpha) / 255.0f, 0, 200);
                        return;
                    }
                    return;
                case 1:
                    Log.d(FingerViewController.TAG, " AUTHENTICATE enter");
                    FingerViewController.this.createAndAddHighLightView(brightness);
                    FingerViewController.this.mHandler.postDelayed(FingerViewController.this.mNotifyRunnable, 30);
                    return;
                default:
                    Log.d(FingerViewController.TAG, " default no operation");
                    return;
            }
        }
    };
    private String mHint;
    private HintText mHintView;
    private boolean mIsFingerFrozen = false;
    private boolean mIsNeedReload;
    private boolean mIsSingleModeObserverRegistered = false;
    private boolean mKeepMaskAfterAuthentication = false;
    private RelativeLayout mLayoutForAlipay;
    private LayoutInflater mLayoutInflater;
    private RemainTimeCountDown mMyCountDown = null;
    private Runnable mNotifyRunnable = new Runnable() {
        public void run() {
            if (FingerViewController.this.mFingerViewChangeCallback != null) {
                Log.d(FingerViewController.TAG, " Runnable notifyCaptureOpticalImage ");
                FingerViewController.this.mFingerViewChangeCallback.onNotifyCaptureImage();
            }
        }
    };
    private Bundle mPkgAttributes;
    private String mPkgName;
    private PowerManager mPowerManager;
    private int mPreBrightness = 0;
    private IFingerprintServiceReceiver mReceiver;
    private int mRemainTimes = 5;
    private RelativeLayout mRemoteView;
    private final Runnable mRemoveButtonViewRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.removeButtonView();
        }
    };
    private final Runnable mRemoveFingerViewRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.removeFingerprintView();
        }
    };
    private final Runnable mRemoveHighLightViewTimeout = new Runnable() {
        public void run() {
            Log.d(FingerViewController.TAG, " mRemoveHighLightViewTimeout ");
            FingerViewController.this.removeHighLightViewInner();
        }
    };
    private final Runnable mRemoveHighlightCircleRunnable = new Runnable() {
        public void run() {
            Log.d(FingerViewController.TAG, "RemoveHighlightCircle highLightViewAdded = " + FingerViewController.this.highLightViewAdded);
            if (FingerViewController.this.highLightViewAdded && FingerViewController.this.mHighLightView != null) {
                FingerViewController.this.mHighLightView.setCircleVisibility(4);
            }
        }
    };
    private final Runnable mRemoveImageOnlyRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.removeImageOnlyView();
        }
    };
    private int mSavedButtonHeight;
    private int mSavedImageDpi;
    private int mSavedImageHeight;
    private int mSavedMaskDpi;
    private int mSavedMaskHeight;
    private int mSavedRotation;
    private Bitmap mScreenShot;
    private final Runnable mSetScene = new Runnable() {
        public void run() {
            if (FingerViewController.this.mDisplayEngineManager != null) {
                FingerViewController.this.mDisplayEngineManager.setScene(29, 0);
                Log.d(FingerViewController.TAG, "mDisplayEngineManager set scene 0");
            }
        }
    };
    private final Runnable mShowHighlightCircleRunnable = new Runnable() {
        public void run() {
            if (FingerViewController.this.mHighLightShowType == 0 || FingerViewController.this.mHighLightShowType == 3) {
                Log.d(FingerViewController.TAG, "highLightViewAdded = " + FingerViewController.this.highLightViewAdded);
                if (FingerViewController.this.highLightViewAdded && FingerViewController.this.mHighLightView.getCircleVisibility() == 4) {
                    FingerViewController.this.mHandler.removeCallbacks(FingerViewController.this.mRemoveHighlightCircleRunnable);
                    FingerViewController.this.mHighLightView.setCircleVisibility(0);
                    FingerViewController.this.mHandler.post(FingerViewController.this.mNotifyRunnable);
                    FingerViewController.this.mHandler.postDelayed(FingerViewController.this.mRemoveHighlightCircleRunnable, 1200);
                }
            }
        }
    };
    private SingleModeContentObserver mSingleContentObserver;
    private LayoutParams mSuspensionButtonParams;
    private final Runnable mUpdateButtonViewRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.updateButtonView();
        }
    };
    private final Runnable mUpdateFingerprintViewRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.updateFingerprintView();
        }
    };
    private final Runnable mUpdateMaskAttibuteRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.updateHintView(FingerViewController.this.mHint);
        }
    };
    private boolean mUseDefaultHint = true;
    private int mWidgetColor;
    private boolean mWidgetColorSet = false;
    private final WindowManager mWindowManager;
    private int mWindowType;
    private IPowerManager pm;
    private ContentObserver settingsDisplayObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            FingerViewController.this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    FingerViewController.this.mDefaultDisplayHeight = Global.getInt(FingerViewController.this.mContentResolver, FingerViewController.APS_INIT_HEIGHT, FingerViewController.DEFAULT_INIT_HEIGHT);
                    FingerViewController.this.mDefaultDisplayWidth = Global.getInt(FingerViewController.this.mContentResolver, FingerViewController.APS_INIT_WIDTH, 1440);
                    int currentHeight = SystemPropertiesEx.getInt("persist.sys.rog.height", FingerViewController.this.mDefaultDisplayHeight);
                    int currentWidth = SystemPropertiesEx.getInt("persist.sys.rog.width", FingerViewController.this.mDefaultDisplayWidth);
                    if (FingerViewController.this.mCurrentHeight == currentHeight && FingerViewController.this.mCurrentWidth == currentWidth) {
                        Log.d(FingerViewController.TAG, "onChange: need reload display parameter");
                        FingerViewController.this.mIsNeedReload = true;
                    } else {
                        FingerViewController.this.mCurrentHeight = currentHeight;
                        FingerViewController.this.mCurrentWidth = currentWidth;
                        FingerViewController.this.mIsNeedReload = false;
                    }
                    Log.d(FingerViewController.TAG, "onChange:" + FingerViewController.this.mDefaultDisplayHeight + "," + FingerViewController.this.mDefaultDisplayWidth + "," + FingerViewController.this.mCurrentHeight + "," + FingerViewController.this.mCurrentWidth);
                }
            }, 30);
        }
    };

    public interface ICallBack {
        void onFingerViewStateChange(int i);

        void onNotifyCaptureImage();
    }

    private class FingerprintViewCallback implements huawei.com.android.server.fingerprint.FingerprintView.ICallBack {
        private FingerprintViewCallback() {
        }

        public void userActivity() {
            FingerViewController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }

        public void onConfigurationChanged(Configuration newConfig) {
        }

        public void onDrawFinish() {
        }
    }

    private class RemainTimeCountDown extends CountDownTimer {
        public RemainTimeCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onTick(long millisUntilFinished) {
            int remainedSecs = (int) ((((double) millisUntilFinished) / 1000.0d) + 0.5d);
            if (remainedSecs > 0) {
                FingerViewController.this.updateHintView(FingerViewController.this.mContext.getResources().getQuantityString(34406409, remainedSecs, new Object[]{Integer.valueOf(remainedSecs)}));
            }
        }

        public void onFinish() {
            Log.d(FingerViewController.TAG, "RemainTimeCountDown onFinish");
            FingerViewController.this.mIsFingerFrozen = false;
            if (FingerViewController.this.mHasBackFingerprint) {
                FingerViewController.this.updateHintView(FingerViewController.this.mContext.getString(33685950));
            } else {
                FingerViewController.this.updateHintView(FingerViewController.this.mContext.getString(33685977));
            }
        }
    }

    private class SingleModeContentCallback implements huawei.com.android.server.fingerprint.SingleModeContentObserver.ICallBack {
        private SingleModeContentCallback() {
        }

        public void onContentChange() {
            if ((FingerViewController.this.mFingerprintView != null && FingerViewController.this.mFingerprintView.isAttachedToWindow()) || (FingerViewController.this.mLayoutForAlipay != null && FingerViewController.this.mLayoutForAlipay.isAttachedToWindow())) {
                String singleModeValue = Global.getString(FingerViewController.this.mContext.getContentResolver(), "single_hand_mode");
                if (singleModeValue != null && (singleModeValue.isEmpty() ^ 1) != 0) {
                    Global.putString(FingerViewController.this.mContext.getContentResolver(), "single_hand_mode", "");
                }
            }
        }
    }

    private class SuspensionButtonCallback implements huawei.com.android.server.fingerprint.SuspensionButton.ICallBack {
        private SuspensionButtonCallback() {
        }

        public void onButtonViewMoved(float endX, float endY) {
            if (FingerViewController.this.mButtonView != null) {
                FingerViewController.this.mSuspensionButtonParams.x = (int) (endX - (((float) FingerViewController.this.mSuspensionButtonParams.width) * 0.5f));
                FingerViewController.this.mSuspensionButtonParams.y = (int) (endY - (((float) FingerViewController.this.mSuspensionButtonParams.height) * 0.5f));
                Log.d(FingerViewController.TAG, "onButtonViewUpdate,x = " + FingerViewController.this.mSuspensionButtonParams.x + " ,y = " + FingerViewController.this.mSuspensionButtonParams.y);
                FingerViewController.this.mWindowManager.updateViewLayout(FingerViewController.this.mButtonView, FingerViewController.this.mSuspensionButtonParams);
            }
        }

        public void onButtonClick() {
            FingerViewController.this.mHandler.post(FingerViewController.this.mRemoveButtonViewRunnable);
            FingerViewController.this.mHandler.post(FingerViewController.this.mAddFingerViewRunnable);
            if (FingerViewController.this.mFingerViewChangeCallback != null) {
                FingerViewController.this.mFingerViewChangeCallback.onFingerViewStateChange(1);
            }
            if (FingerViewController.this.mReceiver != null) {
                try {
                    FingerViewController.this.mReceiver.onAcquired(0, 6, 12);
                } catch (RemoteException e) {
                    Log.d(FingerViewController.TAG, "catch exception");
                }
            }
            Flog.bdReport(FingerViewController.this.mContext, 501, "{PkgName:" + FingerViewController.this.mPkgName + "}");
        }

        public String getCurrentApp() {
            return FingerViewController.this.mPkgName;
        }

        public void userActivity() {
            FingerViewController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }

        public void onConfigurationChanged(Configuration newConfig) {
            FingerViewController.this.mHandler.post(FingerViewController.this.mUpdateButtonViewRunnable);
        }
    }

    private FingerViewController(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.pm = Stub.asInterface(ServiceManager.getService("power"));
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mFingerprintManagerEx = new FingerprintManagerEx(this.mContext);
        this.mAodService = getAodService();
        this.mDisplayEngineManager = new DisplayEngineManager();
        long identityToken = Binder.clearCallingIdentity();
        try {
            this.mContentResolver.registerContentObserver(Global.getUriFor(APS_INIT_HEIGHT), false, this.settingsDisplayObserver);
            this.mContentResolver.registerContentObserver(Global.getUriFor(APS_INIT_WIDTH), false, this.settingsDisplayObserver);
            this.mContentResolver.registerContentObserver(Global.getUriFor("display_size_forced"), false, this.settingsDisplayObserver);
            this.settingsDisplayObserver.onChange(true);
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private IAodManager getAodService() {
        return IAodManager.Stub.asInterface(ServiceManager.getService("aod_service"));
    }

    public static FingerViewController getInstance(Context context) {
        FingerViewController fingerViewController;
        synchronized (FingerViewController.class) {
            if (sInstance == null) {
                sInstance = new FingerViewController(context);
            }
            fingerViewController = sInstance;
        }
        return fingerViewController;
    }

    public void registCallback(ICallBack fingerViewChangeCallback) {
        this.mFingerViewChangeCallback = fingerViewChangeCallback;
    }

    public String getCurrentPackage() {
        Log.i(TAG, "current package is " + this.mPkgName);
        return this.mPkgName;
    }

    public void showMaskOrButton(String pkgName, Bundle bundle, IFingerprintServiceReceiver receiver, int type, boolean hasUdFingerprint, boolean hasBackFingerprint) {
        this.mPkgName = pkgName;
        this.mPkgAttributes = bundle;
        this.mWidgetColorSet = false;
        this.mHasUdFingerprint = hasUdFingerprint;
        this.mHasBackFingerprint = hasBackFingerprint;
        Log.d(TAG, "mPkgAttributes has been init, mPkgAttributes=" + bundle);
        if (this.mPkgAttributes == null || this.mPkgAttributes.getString("SystemTitle") == null) {
            this.mUseDefaultHint = true;
        } else {
            this.mUseDefaultHint = false;
        }
        this.mReceiver = receiver;
        if (PKGNAME_OF_SYSTEMMANAGER.equals(pkgName)) {
            Log.d(TAG, "do not show mask for systemmanager");
            return;
        }
        Log.d(TAG, "type of package " + pkgName + " is " + type);
        if (this.mPkgName == null || !this.mPkgName.equals(PKGNAME_OF_KEYGUARD)) {
            this.mWindowType = 2024;
            if (type == 0) {
                this.mHandler.post(this.mAddFingerViewRunnable);
                Flog.bdReport(this.mContext, 501, "{PkgName:" + this.mPkgName + "}");
            } else if (type == 1) {
                this.mHandler.post(this.mAddButtonViewRunnable);
            } else if (type == 3) {
                this.mHandler.post(this.mAddImageOnlyRunnable);
            }
            return;
        }
        this.mWindowType = 2014;
    }

    public void showMaskForApp(Bundle attribute) {
        this.mPkgName = getForegroundPkgName();
        this.mPkgAttributes = attribute;
        if (this.mPkgName == null || !this.mPkgName.equals(PKGNAME_OF_KEYGUARD)) {
            this.mWindowType = 2024;
        } else {
            this.mWindowType = 2014;
        }
        if (this.mPkgAttributes == null || this.mPkgAttributes.getString("SystemTitle") == null) {
            this.mUseDefaultHint = true;
        } else {
            this.mUseDefaultHint = false;
        }
        this.mHandler.post(this.mAddFingerViewRunnable);
    }

    public void showSuspensionButtonForApp(int centerX, int centerY, String callingUidName) {
        Log.d(TAG, "mButtonCenterX = " + this.mButtonCenterX + ",mButtonCenterY =" + this.mButtonCenterY + ",callingUidName = " + callingUidName);
        this.mButtonCenterX = centerX;
        this.mButtonCenterY = centerY;
        if (UIDNAME_OF_KEYGUARD.equals(callingUidName)) {
            this.mPkgName = PKGNAME_OF_KEYGUARD;
        }
        Log.d(TAG, "mButtonCenterX = " + this.mButtonCenterX + ",mButtonCenterY =" + this.mButtonCenterY);
        this.mWindowType = 2024;
        this.mPkgAttributes = null;
        if (this.mFingerViewChangeCallback != null) {
            this.mFingerViewChangeCallback.onFingerViewStateChange(2);
        }
        this.mHandler.post(this.mAddButtonViewRunnable);
    }

    public void removeMaskOrButton() {
        this.mHandler.post(this.mRemoveFingerViewRunnable);
        this.mHandler.post(this.mRemoveButtonViewRunnable);
        this.mHandler.post(this.mRemoveImageOnlyRunnable);
        this.mFingerViewChangeCallback.onFingerViewStateChange(0);
    }

    public void removeMaskAndShowButton() {
        this.mHandler.post(this.mRemoveFingerViewRunnable);
        this.mHandler.post(this.mAddButtonViewRunnable);
        this.mFingerViewChangeCallback.onFingerViewStateChange(2);
    }

    public void updateMaskViewAttributes(Bundle attributes, String pkgname) {
        if (attributes != null && pkgname != null) {
            String hint = attributes.getString("SystemTitle");
            if (this.mFingerprintViewAdded && hint != null) {
                Log.d(TAG, "updateMaskViewAttributes,hint = " + hint);
                this.mHint = hint;
                this.mHandler.post(this.mUpdateMaskAttibuteRunnable);
            }
        }
    }

    public void updateFingerprintView(int result, boolean keepMaskAfterAuthentication) {
        this.mAuthenticateResult = result;
        this.mKeepMaskAfterAuthentication = keepMaskAfterAuthentication;
        Log.d(TAG, "mUseDefaultHint = " + this.mUseDefaultHint);
        this.mHandler.post(this.mUpdateFingerprintViewRunnable);
    }

    public void updateFingerprintView(int result, int failTimes) {
        if (result != 2) {
            this.mRemainTimes = 5 - failTimes;
        }
        this.mAuthenticateResult = result;
        this.mHandler.post(this.mUpdateFingerprintViewRunnable);
    }

    public void showHighlightview(int type) {
        if (!this.mPowerManager.isInteractive() || this.highLightViewAdded) {
            Log.d(TAG, "is not Interactive or already added");
            return;
        }
        this.mHighLightShowType = type;
        Log.d(TAG, "show Highlightview mHighLightShowType:" + this.mHighLightShowType);
        this.mHandler.removeCallbacks(this.mHighLightViewRunnable);
        this.mHandler.removeCallbacks(this.mRemoveHighLightViewTimeout);
        this.mHandler.post(this.mHighLightViewRunnable);
        if (type == 1) {
            this.mHandler.postDelayed(this.mRemoveHighLightViewTimeout, 1200);
        }
    }

    public void removeHighlightview(int type) {
        Log.d(TAG, "removeHighlightview mHighLightShowType:" + this.mHighLightShowType);
        this.mHighLightRemoveType = type;
        this.mHandler.removeCallbacks(this.mRemoveHighLightViewTimeout);
        this.mHandler.removeCallbacks(this.mHighLightViewRunnable);
        if (this.mHighLightShowType == 3 || this.mHighLightShowType == 0) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    FingerViewController.this.setLightLevel(-1, 150);
                    if (FingerViewController.this.mHighLightView != null) {
                        FingerViewController.this.startAlphaValueAnimation(FingerViewController.this.mHighLightView, false, FingerViewController.this.mHighLightView.getAlpha() / 255.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 70, 80);
                    }
                }
            });
        } else {
            removeHighLightViewInner();
        }
    }

    private void createFingerprintView() {
        initBaseElement();
        updateBaseElementMargins();
        updateExtraElement();
        initFingerprintViewParams();
        Log.d(TAG, "createFingerprintView called ,reset Hint");
        this.mFingerViewParams.type = this.mWindowType;
        Log.d(TAG, "fingerviewadded,mWidgetColor = " + this.mWidgetColor);
        if (!this.mFingerprintViewAdded) {
            startBlurScreenshot();
            this.mWidgetColor = HwColorPicker.processBitmap(this.mScreenShot).getWidgetColor();
            this.mWidgetColorSet = true;
            this.mWindowManager.addView(this.mFingerView, this.mFingerViewParams);
            this.mFingerprintViewAdded = true;
            exitSingleHandMode();
            registerSingerHandObserver();
            Log.d(TAG, "addFingerprintView is done,is View Added = " + this.mFingerprintViewAdded);
        }
    }

    private void initBaseElement() {
        int curRotation = this.mWindowManager.getDefaultDisplay().getRotation();
        int dpi = SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)));
        if (this.mFingerView != null && this.mCurrentHeight == this.mSavedMaskHeight && curRotation == this.mSavedRotation && dpi == this.mSavedMaskDpi) {
            Log.d(TAG, "don't need to inflate mFingerView again");
            return;
        }
        Log.d(TAG, "dpi or rotation has changed, mCurrentHeight = " + this.mCurrentHeight + ", mSavedMaskHeight = " + this.mSavedMaskHeight + ",inflate mFingerView");
        this.mSavedMaskDpi = dpi;
        this.mSavedMaskHeight = this.mCurrentHeight;
        this.mSavedRotation = curRotation;
        this.mFingerView = (FingerprintView) this.mLayoutInflater.inflate(34013288, null);
        this.mFingerView.setCallback(new FingerprintViewCallback());
        this.mFingerprintView = (ImageView) this.mFingerView.findViewById(34603039);
        this.mCancelView = (RelativeLayout) this.mFingerView.findViewById(34603027);
        this.mRemoteView = (RelativeLayout) this.mFingerView.findViewById(34603142);
        this.mCancelView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FingerViewController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                FingerViewController.this.onCancelClick();
                Flog.bdReport(FingerViewController.this.mContext, 502, "{PkgName:" + FingerViewController.this.mPkgName + "}");
                String foregroundPkgName = FingerViewController.this.getForegroundPkgName();
                if (foregroundPkgName != null && FingerViewController.this.isBroadcastNeed(foregroundPkgName)) {
                    Intent cancelMaskIntent = new Intent(FingerViewController.FINGERPRINT_IN_DISPLAY);
                    cancelMaskIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPCODE_KEY, 1011);
                    cancelMaskIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_KEY, FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_CLOSE_VIEW_VALUE);
                    cancelMaskIntent.setPackage(foregroundPkgName);
                    FingerViewController.this.mContext.sendBroadcast(cancelMaskIntent);
                } else if (FingerViewController.this.mReceiver != null) {
                    try {
                        FingerViewController.this.mReceiver.onAcquired(0, 6, 11);
                    } catch (RemoteException e) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
    }

    private void updateBaseElementMargins() {
        getCurrentRotation();
        int[] fingerprintMargin = calculateFingerprintMargin();
        RelativeLayout relativeLayout = (RelativeLayout) this.mFingerView.findViewById(34603018);
        MarginLayoutParams fingerviewLayoutParams = (MarginLayoutParams) relativeLayout.getLayoutParams();
        float dpiScale = getDPIScale();
        if (this.mCurrentRotation == 1 || this.mCurrentRotation == 3) {
            fingerviewLayoutParams.width = (fingerprintMargin[0] * 2) + ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472222)) * dpiScale) + 0.5f));
            int[] layoutMargin = calculateFingerprintLayoutLeftMargin(fingerviewLayoutParams.width);
            fingerviewLayoutParams.leftMargin = layoutMargin[0];
            fingerviewLayoutParams.rightMargin = layoutMargin[1];
            relativeLayout.setLayoutParams(fingerviewLayoutParams);
            MarginLayoutParams remoteViewLayoutParams = (MarginLayoutParams) this.mRemoteView.getLayoutParams();
            int remoteViewLeftLayoutmargin = calculateRemoteViewLeftMargin(fingerviewLayoutParams.width);
            remoteViewLayoutParams.width = (int) ((((((float) this.mCurrentHeight) - (((float) (this.mContext.getResources().getDimensionPixelSize(34472238) * 2)) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472233)) * dpiScale)) - ((float) fingerviewLayoutParams.width)) + 0.5f);
            remoteViewLayoutParams.leftMargin = remoteViewLeftLayoutmargin;
            remoteViewLayoutParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472241)) * dpiScale) + 0.5f);
            remoteViewLayoutParams.rightMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472240)) * dpiScale) + 0.5f);
            remoteViewLayoutParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472237)) * dpiScale) + 0.5f);
            this.mRemoteView.setLayoutParams(remoteViewLayoutParams);
            Log.d(TAG, " RemoteviewLayoutParams.leftMargin =" + remoteViewLayoutParams.leftMargin + ",rightMargin =" + remoteViewLayoutParams.rightMargin);
        } else {
            fingerviewLayoutParams.width = this.mCurrentWidth;
            fingerviewLayoutParams.leftMargin = 0;
        }
        MarginLayoutParams fingerprintImageParams = (MarginLayoutParams) this.mFingerprintView.getLayoutParams();
        fingerprintImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472223)) * dpiScale) + 0.5f);
        fingerprintImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472222)) * dpiScale) + 0.5f);
        fingerprintImageParams.leftMargin = fingerprintMargin[0];
        fingerprintImageParams.topMargin = fingerprintMargin[1];
        Log.d(TAG, "fingerprintViewParams.width = " + fingerprintImageParams.width + ", fingerprintViewParams.height = " + fingerprintImageParams.height);
        this.mFingerprintView.setLayoutParams(fingerprintImageParams);
        MarginLayoutParams cancelViewParams = (MarginLayoutParams) this.mCancelView.getLayoutParams();
        if (this.mCurrentRotation == 1 || this.mCurrentRotation == 3) {
            cancelViewParams.topMargin = (int) (((((float) this.mCurrentWidth) - (((float) this.mContext.getResources().getDimensionPixelSize(34472120)) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472257)) * dpiScale)) + 0.5f);
        } else {
            cancelViewParams.topMargin = (int) (((((float) this.mCurrentHeight) - (((float) this.mContext.getResources().getDimensionPixelSize(34472120)) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472257)) * dpiScale)) + 0.5f);
        }
        Log.d(TAG, "cancelViewParams.topMargin = " + cancelViewParams.topMargin);
        cancelViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472219)) * dpiScale) + 0.5f);
        cancelViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472218)) * dpiScale) + 0.5f);
        this.mCancelView.setLayoutParams(cancelViewParams);
    }

    private void updateExtraElement() {
        float dpiScale = getDPIScale();
        RelativeLayout titleAndSummaryView = (RelativeLayout) this.mFingerView.findViewById(34603020);
        MarginLayoutParams titleAndSummaryViewParams = (MarginLayoutParams) titleAndSummaryView.getLayoutParams();
        titleAndSummaryViewParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472119)) * dpiScale) + 0.5f);
        titleAndSummaryView.setLayoutParams(titleAndSummaryViewParams);
        TextView appNameView = (TextView) this.mFingerView.findViewById(34603019);
        appNameView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472118)) * dpiScale) + 0.5f)));
        TextView accountMessageView = (TextView) this.mFingerView.findViewById(34603008);
        accountMessageView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34471966)) * dpiScale) + 0.5f)));
        MarginLayoutParams accountMessageViewParams = (MarginLayoutParams) accountMessageView.getLayoutParams();
        accountMessageViewParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472112)) * dpiScale) + 0.5f);
        accountMessageView.setLayoutParams(accountMessageViewParams);
        RelativeLayout usePasswordHotSpot = (RelativeLayout) this.mFingerView.findViewById(34603235);
        MarginLayoutParams usePasswordHotSpotParams = (MarginLayoutParams) usePasswordHotSpot.getLayoutParams();
        usePasswordHotSpotParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472259)) * dpiScale) + 0.5f);
        usePasswordHotSpotParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472258)) * dpiScale) + 0.5f);
        usePasswordHotSpot.setLayoutParams(usePasswordHotSpotParams);
        TextView usePasswordView = (TextView) this.mFingerView.findViewById(34603021);
        usePasswordView.setText(this.mContext.getString(33685976));
        usePasswordView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472260)) * dpiScale) + 0.5f)));
        this.mHintView = (HintText) this.mFingerView.findViewById(34603040);
        resetFrozenCountDownIfNeed();
        if (this.mHasBackFingerprint) {
            this.mHintView.setText(this.mContext.getString(33685950));
        } else {
            this.mHintView.setText(this.mContext.getString(33685977));
        }
        this.mHintView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472231)) * dpiScale) + 0.5f)));
        MarginLayoutParams hintViewParams = (MarginLayoutParams) this.mHintView.getLayoutParams();
        hintViewParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472230)) * dpiScale) + 0.5f);
        this.mHintView.setLayoutParams(hintViewParams);
        usePasswordHotSpot.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(FingerViewController.TAG, "onClick,UsePassword");
                FingerViewController.this.onUsePasswordClick();
                String foregroundPkgName = FingerViewController.this.getForegroundPkgName();
                Log.d(FingerViewController.TAG, "foregroundPackageName = " + foregroundPkgName);
                if (foregroundPkgName != null && FingerViewController.this.isBroadcastNeed(foregroundPkgName)) {
                    Intent usePasswordIntent = new Intent(FingerViewController.FINGERPRINT_IN_DISPLAY);
                    usePasswordIntent.setPackage(foregroundPkgName);
                    usePasswordIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPCODE_KEY, 1010);
                    usePasswordIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_KEY, FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_USE_PASSWORD_VALUE);
                    FingerViewController.this.mContext.sendBroadcast(usePasswordIntent);
                } else if (FingerViewController.this.mReceiver != null) {
                    try {
                        FingerViewController.this.mReceiver.onError(0, 10, 0);
                    } catch (RemoteException e) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
        if (this.mPkgAttributes != null) {
            Log.d(TAG, "mPkgAttributes != null");
            if (this.mPkgAttributes.getParcelable("CustView") != null) {
                this.mRemoteView.setVisibility(0);
                Log.d(TAG, "RemoteViews != null");
                this.mRemoteView.addView(((RemoteViews) this.mPkgAttributes.getParcelable("CustView")).apply(this.mContext, this.mRemoteView));
            } else {
                this.mRemoteView.setVisibility(4);
            }
            Log.d(TAG, "mPkgAttributes.getString= " + this.mPkgAttributes.getString("Title"));
            if (this.mPkgAttributes.getString("Title") != null) {
                appNameView.setVisibility(0);
                appNameView.setText(this.mPkgAttributes.getString("Title"));
            } else {
                appNameView.setVisibility(4);
            }
            if (this.mPkgAttributes.getString("Summary") != null) {
                accountMessageView.setVisibility(0);
                accountMessageView.setText(this.mPkgAttributes.getString("Summary"));
            } else {
                accountMessageView.setVisibility(8);
            }
            if (this.mPkgAttributes.getBoolean("UsePassword")) {
                usePasswordHotSpot.setVisibility(0);
            } else {
                usePasswordHotSpot.setVisibility(4);
            }
            if (this.mPkgAttributes.getString("SystemTitle") != null) {
                Log.d(TAG, "attributestring =" + this.mPkgAttributes.getString("SystemTitle"));
                this.mHintView.setText(this.mPkgAttributes.getString("SystemTitle"));
                return;
            }
            return;
        }
        this.mRemoteView.setVisibility(4);
        appNameView.setVisibility(4);
        accountMessageView.setVisibility(8);
        usePasswordHotSpot.setVisibility(4);
    }

    private void getCurrentRotation() {
        this.mCurrentRotation = this.mWindowManager.getDefaultDisplay().getRotation();
    }

    private void getCurrentFingerprintCenter() {
        this.mCurrentRotation = this.mWindowManager.getDefaultDisplay().getRotation();
        switch (this.mCurrentRotation) {
            case 0:
            case 2:
                this.mFingerprintCenterX = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
                this.mFingerprintCenterY = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
                return;
            case 1:
                this.mFingerprintCenterX = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
                this.mFingerprintCenterY = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
                return;
            case 3:
                this.mFingerprintCenterX = this.mDefaultDisplayHeight - ((this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2);
                this.mFingerprintCenterY = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
                return;
            default:
                return;
        }
    }

    private void initFingerprintViewParams() {
        if (this.mFingerViewParams == null) {
            this.mFingerViewParams = new LayoutParams(-1, -1);
            this.mFingerViewParams.flags = 201852424;
            LayoutParams layoutParams = this.mFingerViewParams;
            layoutParams.privateFlags |= 16;
            this.mFingerViewParams.format = -3;
            this.mFingerViewParams.screenOrientation = 14;
            this.mFingerViewParams.setTitle(FINGRPRINT_VIEW_TITLE_NAME);
        }
    }

    private void removeFingerprintView() {
        Log.d(TAG, "removeFingerprintView start added = " + this.mFingerprintViewAdded);
        if (this.mFingerView != null && this.mFingerprintViewAdded) {
            this.mWindowManager.removeView(this.mFingerView);
            resetFingerprintView();
            Log.d(TAG, "removeFingerprintView is done is View Added = " + this.mFingerprintViewAdded + "mFingerView =" + this.mFingerView);
        }
    }

    private void updateFingerprintView() {
        if (this.mAuthenticateResult == 1) {
            if (this.mUseDefaultHint) {
                updateHintView();
            }
        } else if (this.mAuthenticateResult == 0) {
            if (this.mUseDefaultHint) {
                updateHintView(this.mContext.getString(33685964));
            }
            String foregroundPkg = getForegroundPkgName();
            for (String pkgName : PACKAGES_USE_HWAUTH_INTERFACE) {
                if (pkgName.equals(foregroundPkg)) {
                    Log.d(TAG, "hw wallet Identifing,pkgName = " + pkgName);
                }
            }
            if (!this.mKeepMaskAfterAuthentication) {
                removeMaskOrButton();
            }
        } else if (this.mAuthenticateResult == 2) {
            if (!this.mUseDefaultHint) {
                return;
            }
            if (this.mRemainTimes != 5) {
                updateHintView();
            } else if (this.mHasBackFingerprint) {
                updateHintView(this.mContext.getString(33685950));
            } else {
                updateHintView(this.mContext.getString(33685977));
            }
        } else if (this.mAuthenticateResult == 3 && this.mUseDefaultHint) {
            updateHintView(this.mContext.getString(33685969));
        }
    }

    private void updateHintView() {
        Log.d(TAG, "updateFingerprintView start,mFingerprintViewAdded = " + this.mFingerprintViewAdded);
        if (this.mFingerprintViewAdded && this.mHintView != null) {
            if (this.mRemainTimes > 0 && this.mRemainTimes < 5) {
                Log.d(TAG, "remaind time = " + this.mRemainTimes);
                this.mHintView.setText(this.mContext.getResources().getQuantityString(34406410, this.mRemainTimes, new Object[]{Integer.valueOf(this.mRemainTimes)}));
            } else if (this.mRemainTimes == 0) {
                if (!this.mIsFingerFrozen) {
                    if (this.mMyCountDown != null) {
                        this.mMyCountDown.cancel();
                    }
                    this.mMyCountDown = new RemainTimeCountDown(30000, 1000);
                    this.mMyCountDown.start();
                    this.mIsFingerFrozen = true;
                } else {
                    return;
                }
            }
            this.mWindowManager.updateViewLayout(this.mFingerView, this.mFingerViewParams);
        }
    }

    private void updateHintView(String hint) {
        if (this.mHintView != null) {
            this.mHintView.setText(hint);
        }
        if (this.mFingerprintViewAdded) {
            this.mWindowManager.updateViewLayout(this.mFingerView, this.mFingerViewParams);
        }
    }

    private void resetFingerprintView() {
        this.mFingerprintViewAdded = false;
        if (this.mBLurBitmap != null) {
            this.mBLurBitmap.recycle();
        }
        if (this.mScreenShot != null) {
            this.mScreenShot.recycle();
            this.mScreenShot = null;
        }
        if (this.mRemoteView != null) {
            this.mRemoteView.removeAllViews();
        }
        unregisterSingerHandObserver();
    }

    private void createImageOnlyView() {
        int[] fingerprintMargin;
        MarginLayoutParams fingerprintImageParams;
        LayoutParams fingerprintOnlyLayoutParams;
        int curHeight = SystemPropertiesEx.getInt("persist.sys.rog.height", this.mDefaultDisplayHeight);
        int dpi = SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)));
        float dpiScale = getDPIScale();
        if (this.mLayoutForAlipay != null && curHeight == this.mSavedImageHeight) {
            if (dpi != this.mSavedImageDpi) {
            }
            this.mFingerprintImageForAlipay = (ImageView) this.mLayoutForAlipay.findViewById(34603038);
            if (this.mHasUdFingerprint) {
                this.mFingerprintImageForAlipay.setImageResource(33751135);
            } else {
                this.mAlipayDrawable = new BreathImageDrawable(this.mContext);
                this.mAlipayDrawable.setBreathImageDrawable(null, this.mContext.getDrawable(33751783));
                this.mFingerprintImageForAlipay.setImageDrawable(this.mAlipayDrawable);
                this.mFingerprintImageForAlipay.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case 0:
                                FingerViewController.this.mAlipayDrawable.startTouchDownBreathAnim();
                                break;
                            case 1:
                                FingerViewController.this.mAlipayDrawable.startTouchUpBreathAnim();
                                break;
                        }
                        return true;
                    }
                });
                this.mAlipayDrawable.startBreathAnim();
            }
            fingerprintMargin = calculateFingerprintImageMargin();
            fingerprintImageParams = (MarginLayoutParams) this.mFingerprintImageForAlipay.getLayoutParams();
            fingerprintImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472221)) * dpiScale) + 0.5f);
            fingerprintImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472220)) * dpiScale) + 0.5f);
            this.mFingerprintImageForAlipay.setLayoutParams(fingerprintImageParams);
            fingerprintOnlyLayoutParams = new LayoutParams();
            fingerprintOnlyLayoutParams.flags = 16777480;
            fingerprintOnlyLayoutParams.privateFlags |= 16;
            fingerprintOnlyLayoutParams.format = -3;
            fingerprintOnlyLayoutParams.screenOrientation = 14;
            fingerprintOnlyLayoutParams.setTitle(FINGRPRINT_IMAGE_TITLE_NAME);
            fingerprintOnlyLayoutParams.gravity = 51;
            fingerprintOnlyLayoutParams.type = this.mWindowType;
            fingerprintOnlyLayoutParams.x = fingerprintMargin[0];
            fingerprintOnlyLayoutParams.y = fingerprintMargin[1];
            fingerprintOnlyLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472221)) * dpiScale) + 0.5f);
            fingerprintOnlyLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472220)) * dpiScale) + 0.5f);
            Log.d(TAG, "fingerprintImage location = [" + fingerprintOnlyLayoutParams.x + "," + fingerprintOnlyLayoutParams.y + "]");
            if (!this.mFingerprintOnlyViewAdded) {
                this.mWindowManager.addView(this.mLayoutForAlipay, fingerprintOnlyLayoutParams);
                this.mFingerprintOnlyViewAdded = true;
                exitSingleHandMode();
                registerSingerHandObserver();
            }
        }
        this.mSavedImageHeight = curHeight;
        this.mSavedImageDpi = dpi;
        this.mLayoutForAlipay = (RelativeLayout) this.mLayoutInflater.inflate(34013287, null);
        this.mFingerprintImageForAlipay = (ImageView) this.mLayoutForAlipay.findViewById(34603038);
        if (this.mHasUdFingerprint) {
            this.mFingerprintImageForAlipay.setImageResource(33751135);
        } else {
            this.mAlipayDrawable = new BreathImageDrawable(this.mContext);
            this.mAlipayDrawable.setBreathImageDrawable(null, this.mContext.getDrawable(33751783));
            this.mFingerprintImageForAlipay.setImageDrawable(this.mAlipayDrawable);
            this.mFingerprintImageForAlipay.setOnTouchListener(/* anonymous class already generated */);
            this.mAlipayDrawable.startBreathAnim();
        }
        fingerprintMargin = calculateFingerprintImageMargin();
        fingerprintImageParams = (MarginLayoutParams) this.mFingerprintImageForAlipay.getLayoutParams();
        fingerprintImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472221)) * dpiScale) + 0.5f);
        fingerprintImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472220)) * dpiScale) + 0.5f);
        this.mFingerprintImageForAlipay.setLayoutParams(fingerprintImageParams);
        fingerprintOnlyLayoutParams = new LayoutParams();
        fingerprintOnlyLayoutParams.flags = 16777480;
        fingerprintOnlyLayoutParams.privateFlags |= 16;
        fingerprintOnlyLayoutParams.format = -3;
        fingerprintOnlyLayoutParams.screenOrientation = 14;
        fingerprintOnlyLayoutParams.setTitle(FINGRPRINT_IMAGE_TITLE_NAME);
        fingerprintOnlyLayoutParams.gravity = 51;
        fingerprintOnlyLayoutParams.type = this.mWindowType;
        fingerprintOnlyLayoutParams.x = fingerprintMargin[0];
        fingerprintOnlyLayoutParams.y = fingerprintMargin[1];
        fingerprintOnlyLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472221)) * dpiScale) + 0.5f);
        fingerprintOnlyLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472220)) * dpiScale) + 0.5f);
        Log.d(TAG, "fingerprintImage location = [" + fingerprintOnlyLayoutParams.x + "," + fingerprintOnlyLayoutParams.y + "]");
        if (!this.mFingerprintOnlyViewAdded) {
            this.mWindowManager.addView(this.mLayoutForAlipay, fingerprintOnlyLayoutParams);
            this.mFingerprintOnlyViewAdded = true;
            exitSingleHandMode();
            registerSingerHandObserver();
        }
    }

    private void removeImageOnlyView() {
        if (this.mFingerprintOnlyViewAdded && this.mLayoutForAlipay != null) {
            this.mWindowManager.removeView(this.mLayoutForAlipay);
            this.mFingerprintOnlyViewAdded = false;
            unregisterSingerHandObserver();
        }
    }

    private int[] calculateFingerprintImageMargin() {
        Log.d(TAG, "left = " + this.mFingerprintPosition[0] + "right = " + this.mFingerprintPosition[2]);
        Log.d(TAG, "top = " + this.mFingerprintPosition[1] + "button = " + this.mFingerprintPosition[3]);
        float dpiScale = getDPIScale();
        int[] margin = new int[2];
        int fingerPrintInScreenWidth = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472221)) * dpiScale) + 0.5f);
        int fingerPrintInScreenHeight = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472220)) * dpiScale) + 0.5f);
        Log.d(TAG, "fingerPrintInScreenWidth= " + fingerPrintInScreenWidth + "fingerPrintInScreenHeight = " + fingerPrintInScreenHeight);
        Log.d(TAG, "current height = " + this.mCurrentHeight + "mDefaultDisplayHeight = " + this.mDefaultDisplayHeight);
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        int marginTop;
        if (this.mCurrentRotation == 0 || this.mCurrentRotation == 2) {
            this.mFingerprintCenterX = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            int marginLeft = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
            marginTop = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
            margin[0] = marginLeft;
            margin[1] = marginTop;
            Log.d(TAG, "marginLeft = " + marginLeft + "marginTop = " + marginTop + "scale = " + scale);
        } else {
            this.mFingerprintCenterX = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            marginTop = (int) (((((float) this.mFingerprintCenterY) * scale) - (((float) fingerPrintInScreenHeight) / 2.0f)) + 0.5f);
            margin[0] = (int) ((((((float) (this.mDefaultDisplayHeight - this.mFingerprintCenterX)) * scale) - (((float) this.mContext.getResources().getDimensionPixelSize(34472233)) * dpiScale)) - (((float) fingerPrintInScreenWidth) / 2.0f)) + 0.5f);
            margin[1] = marginTop;
        }
        return margin;
    }

    private void createAndAddButtonView() {
        if (this.mButtonViewAdded) {
            adjustButtonViewVisibility();
            Log.d(TAG, " mButtonViewAdded return ");
            return;
        }
        calculateButtonPosition();
        Log.d(TAG, "createAndAddButtonView,pkg = " + this.mPkgName);
        float dpiScale = getDPIScale();
        if (this.mButtonView == null || this.mCurrentHeight != this.mSavedButtonHeight) {
            this.mButtonView = (SuspensionButton) this.mLayoutInflater.inflate(34013286, null);
            this.mButtonView.setCallback(new SuspensionButtonCallback());
            this.mSavedButtonHeight = this.mCurrentHeight;
        }
        ImageView buttonImage = (ImageView) this.mButtonView.findViewById(34603025);
        MarginLayoutParams buttonImageParams = (MarginLayoutParams) buttonImage.getLayoutParams();
        buttonImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472219)) * dpiScale) + 0.5f);
        buttonImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472218)) * dpiScale) + 0.5f);
        buttonImage.setLayoutParams(buttonImageParams);
        this.mSuspensionButtonParams = new LayoutParams();
        this.mSuspensionButtonParams.type = this.mWindowType;
        this.mSuspensionButtonParams.flags = 16777480;
        this.mSuspensionButtonParams.gravity = 51;
        this.mSuspensionButtonParams.x = this.mButtonCenterX - (this.mContext.getResources().getDimensionPixelSize(34472219) / 2);
        Log.d(TAG, "mSuspensionButtonParams.x=" + this.mSuspensionButtonParams.x);
        this.mSuspensionButtonParams.y = this.mButtonCenterY - (this.mContext.getResources().getDimensionPixelSize(34472218) / 2);
        Log.d(TAG, "mSuspensionButtonParams.y=" + this.mSuspensionButtonParams.y);
        this.mSuspensionButtonParams.width = this.mContext.getResources().getDimensionPixelSize(34472219);
        this.mSuspensionButtonParams.height = this.mContext.getResources().getDimensionPixelSize(34472218);
        this.mSuspensionButtonParams.format = -3;
        LayoutParams layoutParams = this.mSuspensionButtonParams;
        layoutParams.privateFlags |= 16;
        this.mSuspensionButtonParams.setTitle("fingerprintview_button");
        this.mButtonView.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
            public boolean onPreDraw() {
                FingerViewController.this.mWidgetColorSet = false;
                return true;
            }
        });
        if (!this.mWidgetColorSet) {
            Log.d(TAG, "mWidgetColorSet is false, get a new screenshot and calculate color");
            getScreenShot();
            this.mWidgetColor = HwColorPicker.processBitmap(this.mScreenShot).getWidgetColor();
            this.mWidgetColorSet = true;
        }
        Log.d(TAG, "mWidgetColor = " + this.mWidgetColor);
        buttonImage.setColorFilter(this.mWidgetColor);
        adjustButtonViewVisibility();
        this.mWindowManager.addView(this.mButtonView, this.mSuspensionButtonParams);
        this.mButtonViewAdded = true;
    }

    private void adjustButtonViewVisibility() {
        if (this.mButtonView == null) {
            Log.e(TAG, "mButtonView is null, cannot change visibility");
            return;
        }
        if (this.mButtonViewState == 2) {
            this.mButtonView.setVisibility(4);
        } else {
            this.mButtonView.setVisibility(0);
        }
    }

    private void removeButtonView() {
        if (this.mButtonView != null && this.mButtonViewAdded) {
            Log.d(TAG, "removeButtonView begin, mButtonViewAdded added = " + this.mButtonViewAdded + "mButtonView = " + this.mButtonView);
            this.mWindowManager.removeViewImmediate(this.mButtonView);
            this.mButtonViewAdded = false;
        }
    }

    private void updateButtonView() {
        calculateButtonPosition();
        if (this.mButtonView != null && this.mButtonViewAdded && this.mSuspensionButtonParams != null) {
            this.mSuspensionButtonParams.x = this.mButtonCenterX - (this.mContext.getResources().getDimensionPixelSize(34472257) / 2);
            Log.d(TAG, "mSuspensionButtonParams.x=" + this.mSuspensionButtonParams.x);
            this.mSuspensionButtonParams.y = this.mButtonCenterY - (this.mContext.getResources().getDimensionPixelSize(34472257) / 2);
            Log.d(TAG, "mSuspensionButtonParams.y=" + this.mSuspensionButtonParams.y);
            this.mWindowManager.updateViewLayout(this.mButtonView, this.mSuspensionButtonParams);
        }
    }

    private void startAlphaValueAnimation(final HighLightMaskView target, final boolean isAlphaUp, float startAlpha, float endAlpha, long startDelay, int duration) {
        if (target == null) {
            Log.d(TAG, " animation abort target null");
            return;
        }
        if (startAlpha > ALPHA_LIMITED) {
            startAlpha = ALPHA_LIMITED;
        } else if (startAlpha < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            startAlpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (endAlpha > ALPHA_LIMITED) {
            endAlpha = ALPHA_LIMITED;
        } else if (endAlpha < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            endAlpha = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (isAlphaUp || startAlpha != endAlpha) {
            Log.d(TAG, " startAlphaAnimation current alpha:" + target.getAlpha() + " startAlpha:" + startAlpha + " endAlpha: " + endAlpha + " duration :" + duration);
            final float endAlphaValue = endAlpha;
            ValueAnimator animator = ValueAnimator.ofFloat(new float[]{startAlpha, endAlpha});
            animator.setStartDelay(startDelay);
            animator.setInterpolator(endAlpha == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO ? new AccelerateInterpolator() : new DecelerateInterpolator());
            animator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    target.setAlpha((int) (((Float) animation.getAnimatedValue()).floatValue() * 255.0f));
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                boolean isCanceled = false;

                public void onAnimationEnd(Animator animation) {
                    Log.d(FingerViewController.TAG, " onAnimationEnd endAlpha:" + endAlphaValue + " isCanceled:" + this.isCanceled);
                    if (!(isAlphaUp || (this.isCanceled ^ 1) == 0)) {
                        FingerViewController.this.removeHighLightViewInner();
                    }
                    this.isCanceled = false;
                }

                public void onAnimationStart(Animator animation) {
                    Log.d(FingerViewController.TAG, "onAnimationStart");
                    this.isCanceled = false;
                }

                public void onAnimationCancel(Animator animation) {
                    this.isCanceled = true;
                    super.onAnimationCancel(animation);
                }
            });
            animator.setDuration((long) duration);
            animator.start();
            return;
        }
        Log.d(TAG, " endAlpha equals startAlpha ");
        removeHighLightViewInner();
    }

    public void showHighlightCircle() {
        this.mHandler.post(this.mShowHighlightCircleRunnable);
    }

    public void removeHighlightCircle() {
        this.mHandler.post(this.mRemoveHighlightCircleRunnable);
    }

    private void createAndAddHighLightView(int brightness) {
        if (this.mHighLightView == null) {
            this.mHighLightView = new HighLightMaskView(this.mContext, brightness);
        }
        getCurrentFingerprintCenter();
        this.mHighLightView.setCenterPoints(this.mFingerprintCenterX, this.mFingerprintCenterY);
        Log.d(TAG, "current height = " + this.mCurrentHeight);
        if (this.mIsNeedReload) {
            this.mCurrentHeight = SystemPropertiesEx.getInt("persist.sys.rog.height", this.mDefaultDisplayHeight);
            this.mIsNeedReload = false;
        }
        this.mHighLightView.setScale(((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight));
        this.mHighLightView.setType(this.mHighLightShowType);
        this.mHighLightView.setPackageName(this.mPkgName);
        if (this.mHighLightShowType == 1) {
            this.mHighLightView.setCircleVisibility(0);
        } else {
            this.mHighLightView.setCircleVisibility(4);
        }
        LayoutParams highLightViewParams = new LayoutParams(-1, -1);
        highLightViewParams.type = 2101;
        highLightViewParams.flags = 1304;
        highLightViewParams.privateFlags |= RET_REG_CANCEL.ID;
        highLightViewParams.format = -3;
        highLightViewParams.setTitle("hwSingleMode_window_for_UD_highlight_mask");
        if (this.mHighLightShowType == 1) {
            highLightViewParams.setTitle(HIGHLIGHT_VIEW_TITLE_NAME);
        }
        if (this.mHighLightView.getParent() != null) {
            Log.v(TAG, "REMOVE! mHighLightView before add");
            this.mWindowManager.removeView(this.mHighLightView);
        }
        this.mWindowManager.addView(this.mHighLightView, highLightViewParams);
        this.highLightViewAdded = true;
        if (this.mHighLightShowType == 1) {
            setBacklightViaAod(brightness);
        }
    }

    private void setBacklightViaAod(int brightness) {
        if (brightness < 4) {
            brightness = this.mPreBrightness == 0 ? DEFAULT_BRIGHTNESS : this.mPreBrightness;
            Log.d(TAG, "brightness invalid replaced by:" + brightness);
        }
        float currentBright = (((((float) brightness) - 4.0f) / 251.0f) * 1019.0f) + 4.0f;
        if (this.mAodService != null) {
            try {
                this.mAodService.setBacklight(994, (int) currentBright);
                Log.d(TAG, "aodService set Bright: max: " + 994.5817f + " current:" + currentBright);
                if (this.mDisplayEngineManager != null) {
                    this.mDisplayEngineManager.setScene(29, 994);
                    Log.d(TAG, "mDisplayEngineManager set scene");
                    return;
                }
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
                return;
            } catch (Exception e2) {
                e2.printStackTrace();
                return;
            }
        }
        Log.d(TAG, "aodService is null");
    }

    private void removeHighLightViewInner() {
        if (this.mHighLightView != null && this.mHighLightView.isAttachedToWindow()) {
            this.mWindowManager.removeView(this.mHighLightView);
            this.mHighLightView = null;
        }
        this.highLightViewAdded = false;
        this.mHandler.removeCallbacks(this.mSetScene);
        this.mHandler.postDelayed(this.mSetScene, 80);
    }

    private void onCancelClick() {
        this.mHandler.post(this.mRemoveFingerViewRunnable);
        this.mHandler.post(this.mAddButtonViewRunnable);
        if (this.mFingerViewChangeCallback != null) {
            this.mFingerViewChangeCallback.onFingerViewStateChange(2);
        }
    }

    private void onUsePasswordClick() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        this.mHandler.post(this.mRemoveFingerViewRunnable);
        this.mHandler.post(this.mRemoveButtonViewRunnable);
    }

    public void parseBundle4Keyguard(Bundle bundle) {
        int suspend = bundle.getInt("suspend", 0);
        Log.d(TAG, " suspend:" + suspend + " mButtonViewAdded:" + this.mButtonViewAdded + " mFingerprintViewAdded:" + this.mFingerprintViewAdded);
        if (suspend == -1) {
            if (this.mButtonViewAdded && this.mFingerViewChangeCallback != null) {
                this.mFingerViewChangeCallback.onFingerViewStateChange(2);
            }
            if (this.mFingerprintViewAdded) {
                return;
            }
        }
        byte[] viewType = bundle.getByteArray("viewType");
        byte[] viewState = bundle.getByteArray("viewState");
        for (int i = 0; i < viewType.length; i++) {
            byte type = viewType[i];
            byte state = viewState[i];
            Log.d(TAG, " type:" + type + " state:" + state);
            switch (type) {
                case (byte) 0:
                    if (state != (byte) 1) {
                        if (state != (byte) 0) {
                            break;
                        }
                        this.mHandler.post(this.mRemoveFingerViewRunnable);
                        if (this.mFingerViewChangeCallback == null) {
                            break;
                        }
                        this.mFingerViewChangeCallback.onFingerViewStateChange(1);
                        break;
                    }
                    this.mHandler.post(this.mAddFingerViewRunnable);
                    break;
                case (byte) 1:
                    int[] location;
                    if (state != (byte) 1) {
                        if (state != (byte) 0) {
                            if (state != (byte) 2) {
                                break;
                            }
                            location = bundle.getIntArray("buttonLocation");
                            this.mHandler.post(this.mRemoveFingerViewRunnable);
                            this.mButtonViewState = 2;
                            showSuspensionButtonForApp(location[0], location[1], UIDNAME_OF_KEYGUARD);
                            break;
                        }
                        this.mHandler.post(this.mRemoveButtonViewRunnable);
                        break;
                    }
                    location = bundle.getIntArray("buttonLocation");
                    this.mButtonViewState = 1;
                    showSuspensionButtonForApp(location[0], location[1], UIDNAME_OF_KEYGUARD);
                    break;
                default:
                    break;
            }
        }
    }

    public void setFingerprintPosition(int[] position) {
        this.mFingerprintPosition = (int[]) position.clone();
        Log.d(TAG, "setFingerprintPosition,left = " + this.mFingerprintPosition[0] + "right = " + this.mFingerprintPosition[2]);
        Log.d(TAG, "setFingerprintPosition,top = " + this.mFingerprintPosition[1] + "button = " + this.mFingerprintPosition[3]);
        this.mFingerprintCenterX = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
        this.mFingerprintCenterY = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
    }

    private int[] calculateFingerprintMargin() {
        Log.d(TAG, "left = " + this.mFingerprintPosition[0] + "right = " + this.mFingerprintPosition[2]);
        Log.d(TAG, "top = " + this.mFingerprintPosition[1] + "button = " + this.mFingerprintPosition[3]);
        float dpiScale = getDPIScale();
        int[] margin = new int[2];
        int fingerPrintInScreenWidth = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472223)) * dpiScale) + 0.5f);
        int fingerPrintInScreenHeight = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472222)) * dpiScale) + 0.5f);
        Log.d(TAG, "fingerPrintInScreenWidth= " + fingerPrintInScreenWidth + "fingerPrintInScreenHeight = " + fingerPrintInScreenHeight);
        Log.d(TAG, "current height = " + this.mCurrentHeight + "mDefaultDisplayHeight = " + this.mDefaultDisplayHeight);
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        int marginTop;
        if (this.mCurrentRotation == 0 || this.mCurrentRotation == 2) {
            this.mFingerprintCenterX = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            int marginLeft = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
            marginTop = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
            margin[0] = marginLeft;
            margin[1] = marginTop;
            Log.d(TAG, "marginLeft = " + marginLeft + "marginTop = " + marginTop + "scale = " + scale);
        } else {
            this.mFingerprintCenterX = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            marginTop = (int) (((((float) this.mFingerprintCenterY) * scale) - (((float) fingerPrintInScreenHeight) / 2.0f)) + 0.5f);
            margin[0] = (int) ((((((float) (this.mDefaultDisplayHeight - this.mFingerprintCenterX)) * scale) - (((float) this.mContext.getResources().getDimensionPixelSize(34472233)) * dpiScale)) - (((float) fingerPrintInScreenWidth) / 2.0f)) + 0.5f);
            margin[1] = marginTop;
        }
        return margin;
    }

    private float getDPIScale() {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 640));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        float scale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        Log.i(TAG, "initDPIView: lcdDpi: " + lcdDpi + " dpi: " + dpi + " realdpi: " + SystemProperties.getInt("persist.sys.realdpi", dpi) + " scale: " + scale);
        return scale;
    }

    private int[] calculateFingerprintLayoutLeftMargin(int width) {
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        float dpiScale = getDPIScale();
        int[] layoutMargin = new int[2];
        int leftmargin = 0;
        int rightmargin = 0;
        if (this.mCurrentRotation == 3) {
            leftmargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472233)) * dpiScale) + 0.5f);
            rightmargin = (int) (((((float) this.mFingerprintCenterX) * scale) - (((float) width) / 2.0f)) + 0.5f);
        } else if (this.mCurrentRotation == 1) {
            leftmargin = (int) (((((float) this.mFingerprintCenterX) * scale) - (((float) width) / 2.0f)) + 0.5f);
            rightmargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472233)) * dpiScale) + 0.5f);
        }
        layoutMargin[0] = leftmargin;
        layoutMargin[1] = rightmargin;
        return layoutMargin;
    }

    private int calculateRemoteViewLeftMargin(int fingerLayoutWidth) {
        float dpiScale = getDPIScale();
        if (this.mCurrentRotation == 3) {
            return (int) ((((((float) this.mContext.getResources().getDimensionPixelSize(34472238)) * dpiScale) + (((float) this.mContext.getResources().getDimensionPixelSize(34472233)) * dpiScale)) + ((float) fingerLayoutWidth)) + 0.5f);
        }
        if (this.mCurrentRotation == 1) {
            return (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472238)) * dpiScale) + 0.5f);
        }
        return 0;
    }

    private void calculateButtonPosition() {
        if (this.mPkgName != null && (this.mPkgName.equals(PKGNAME_OF_KEYGUARD) ^ 1) != 0) {
            getCurrentRotation();
            Log.d(TAG, "mCurrentRotation = " + this.mCurrentRotation);
            float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
            switch (this.mCurrentRotation) {
                case 0:
                case 2:
                    this.mButtonCenterX = (int) (((((float) (this.mFingerprintPosition[0] + this.mFingerprintPosition[2])) / 2.0f) * scale) + 0.5f);
                    this.mButtonCenterY = (this.mCurrentHeight - this.mContext.getResources().getDimensionPixelSize(34472120)) - (this.mContext.getResources().getDimensionPixelSize(34472257) / 2);
                    break;
                case 1:
                    this.mButtonCenterX = (int) (((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale) + 0.5f);
                    this.mButtonCenterY = (this.mCurrentWidth - this.mContext.getResources().getDimensionPixelSize(34472120)) - (this.mContext.getResources().getDimensionPixelSize(34472257) / 2);
                    break;
                case 3:
                    this.mButtonCenterX = this.mCurrentHeight - ((int) (((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale) + 0.5f));
                    this.mButtonCenterY = (this.mCurrentWidth - this.mContext.getResources().getDimensionPixelSize(34472120)) - (this.mContext.getResources().getDimensionPixelSize(34472257) / 2);
                    break;
            }
            Log.d(TAG, "mButtonCenterX = " + this.mButtonCenterX + ",mButtonCenterY =" + this.mButtonCenterY);
        }
    }

    private int getBrightness() {
        int brightness = -1;
        try {
            if (System.getIntForUser(this.mContentResolver, "screen_brightness_mode", -2) == 1) {
                brightness = System.getIntForUser(this.mContentResolver, "screen_auto_brightness", -2);
            } else {
                brightness = System.getIntForUser(this.mContentResolver, "screen_brightness", -2);
            }
            if (brightness == 0) {
                int i;
                Log.d(TAG, " brightness is 0 use pre value instead:" + this.mPreBrightness);
                if (this.mPreBrightness == 0) {
                    i = DEFAULT_BRIGHTNESS;
                } else {
                    i = this.mPreBrightness;
                }
                return i;
            }
            Log.d(TAG, " brightness:" + brightness);
            this.mPreBrightness = brightness;
            return brightness;
        } catch (SettingNotFoundException e) {
            Log.d(TAG, "settings not found");
        }
    }

    public void setLightLevel(int level, int lightLevelTime) {
        try {
            this.pm.setBrightnessNoLimit(level, lightLevelTime);
            Log.d(TAG, "setLightLevel :" + level + " time:" + lightLevelTime);
        } catch (RemoteException e) {
            Log.e(TAG, "setFingerprintviewHighlight catch RemoteException ");
        }
    }

    private String getForegroundPkgName() {
        try {
            List<RunningAppProcessInfo> procs = ActivityManagerNative.getDefault().getRunningAppProcesses();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                RunningAppProcessInfo proc = (RunningAppProcessInfo) procs.get(i);
                if (proc.importance == 100) {
                    Log.w(TAG, "foreground package is " + proc.processName);
                    return proc.processName;
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "am.getRunningAppProcesses() failed in getForegroundIfNeedBroadcast");
        }
        Log.w(TAG, "can not get foreground package");
        return null;
    }

    private boolean isBroadcastNeed(String pkgName) {
        for (String pkg : PACKAGES_USE_HWAUTH_INTERFACE) {
            if (pkg.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    private void startBlurScreenshot() {
        getScreenShot();
        if (this.mFingerView == null) {
            Log.e(TAG, "mFingerView is null, cannot set background");
        } else if (this.mScreenShot == null || (HwColorPicker.processBitmap(this.mScreenShot).getDomainColor() == COLOR_BLACK && (PKGNAME_OF_KEYGUARD.equals(this.mPkgName) ^ 1) != 0)) {
            this.mBLurBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
            this.mBLurBitmap.eraseColor(-7829368);
            this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
            this.mFingerView.setBackgroundDrawable(this.mBlurDrawable);
        } else {
            this.mBLurBitmap = BlurUtils.blurMaskImage(this.mContext, this.mScreenShot, this.mScreenShot, 25);
            this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
            this.mFingerView.setBackgroundDrawable(this.mBlurDrawable);
        }
    }

    private void getScreenShot() {
        try {
            this.mScreenShot = BlurUtils.screenShotBitmap(this.mContext, BLUR_SCALE);
        } catch (Exception e) {
            Log.d(TAG, "Exception in screenShotBitmap");
        } catch (Error err) {
            Log.d(TAG, "screenShotBitmap  Error er = " + err.getMessage());
        }
    }

    private void registerSingerHandObserver() {
        if (this.mSingleContentObserver == null) {
            this.mSingleContentObserver = new SingleModeContentObserver(new Handler(), new SingleModeContentCallback());
        }
        if (!this.mIsSingleModeObserverRegistered) {
            Log.d(TAG, "registerSingerHandObserver");
            this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("single_hand_mode"), true, this.mSingleContentObserver);
            this.mIsSingleModeObserverRegistered = true;
        }
    }

    private void unregisterSingerHandObserver() {
        if (this.mIsSingleModeObserverRegistered) {
            Log.d(TAG, "unregisterSingerHandObserver");
            this.mContext.getContentResolver().unregisterContentObserver(this.mSingleContentObserver);
            this.mIsSingleModeObserverRegistered = false;
        }
    }

    private void exitSingleHandMode() {
        Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
    }

    private void resetFrozenCountDownIfNeed() {
        this.mRemainTimes = this.mFingerprintManagerEx.getRemainingNum();
        Log.d(TAG, "getRemainingNum, mRemainTimes = " + this.mRemainTimes);
        if (this.mRemainTimes > 0 && this.mMyCountDown != null) {
            this.mMyCountDown.cancel();
            this.mIsFingerFrozen = false;
        }
    }

    public void notifyFingerprintViewCoverd(boolean covered, Rect winFrame) {
        Log.v(TAG, "notifyWinCovered covered=" + covered + "winFrame = " + winFrame);
        if (covered) {
            if (isFingerprintViewCoverd(getFingerprintViewRect(), winFrame)) {
                Log.v(TAG, "new window covers fingerprintview, suspend");
                if (this.mFingerViewChangeCallback != null) {
                    this.mFingerViewChangeCallback.onFingerViewStateChange(2);
                    return;
                }
                return;
            }
            Log.v(TAG, "new window doesn't cover fingerprintview");
        } else if (this.mFingerViewChangeCallback != null) {
            this.mFingerViewChangeCallback.onFingerViewStateChange(1);
        }
    }

    public void notifyTouchUp() {
        if (this.highLightViewAdded) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (FingerViewController.this.mHighLightView != null) {
                        FingerViewController.this.mHighLightView.setCircleVisibility(4);
                    }
                }
            });
        }
    }

    private Rect getFingerprintViewRect() {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 640));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        float scale = (((float) SystemProperties.getInt("persist.sys.realdpi", dpi)) * 1.0f) / ((float) dpi);
        float lcdscale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        Rect fingerprintViewRect = new Rect();
        fingerprintViewRect.left = (int) (((double) ((((float) (this.mFingerprintPosition[0] + this.mFingerprintPosition[2])) / 2.0f) * scale)) - ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472223)) * lcdscale)) * 0.5d) + 0.5d));
        fingerprintViewRect.top = (int) (((double) ((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale)) - ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472222)) * lcdscale)) * 0.5d) + 0.5d));
        fingerprintViewRect.right = (int) (((double) ((((float) (this.mFingerprintPosition[0] + this.mFingerprintPosition[2])) / 2.0f) * scale)) + ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472223)) * lcdscale)) * 0.5d) + 0.5d));
        fingerprintViewRect.bottom = (int) (((double) ((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale)) + ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472222)) * lcdscale)) * 0.5d) + 0.5d));
        Log.i(TAG, "getFingerprintViewRect, fingerprintViewRect = " + fingerprintViewRect);
        return fingerprintViewRect;
    }

    private boolean isFingerprintViewCoverd(Rect fingerprintViewRect, Rect winFrame) {
        if (fingerprintViewRect.right <= winFrame.left || winFrame.right <= fingerprintViewRect.left || fingerprintViewRect.bottom <= winFrame.top || winFrame.bottom <= fingerprintViewRect.top) {
            return false;
        }
        return true;
    }
}
