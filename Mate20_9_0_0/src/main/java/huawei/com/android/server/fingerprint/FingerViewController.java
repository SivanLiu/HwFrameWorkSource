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
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HwExHandler;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.security.trustcircle.tlv.command.register.RET_REG_CANCEL;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.displayengine.DisplayEngineManager;
import huawei.android.aod.HwAodManager;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;
import huawei.android.hwcolorpicker.HwColorPicker;
import huawei.com.android.server.fingerprint.fingerprintAnimation.BreathImageDrawable;
import huawei.com.android.server.policy.BlurUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static final int COLOR_BLACK = -16250872;
    private static int DEFAULT_BRIGHTNESS = 56;
    private static final int DEFAULT_INIT_HEIGHT = 2880;
    private static final int DEFAULT_INIT_WIDTH = 1440;
    public static final int DISMISSED_REASON_NEGATIVE = 2;
    public static final int DISMISSED_REASON_POSITIVE = 1;
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
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE_SUCCESS = 2;
    public static final int HIGHLIGHT_TYPE_ENROLL = 0;
    public static final int HIGHLIGHT_TYPE_UNDEFINED = -1;
    public static final int HIGHLIGHT_VIEW_REMOVE_TIME = 3;
    private static final String HIGHLIGHT_VIEW_TITLE_NAME = "fingerprint_alpha_layer";
    private static final int INITIAL_BRIGHTNESS = -1;
    private static int INVALID_BRIGHTNESS = -1;
    private static final int INVALID_COLOR = 0;
    private static final int MAX_CAPTURE_TIME = 1000;
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED = 5;
    private static final String[] PACKAGES_USE_HWAUTH_INTERFACE = new String[]{"com.huawei.hwid", "com.huawei.wallet", "com.huawei.android.hwpay"};
    private static String PANEL_INFO_NODE = "/sys/class/graphics/fb0/panel_info";
    public static final String PKGNAME_OF_KEYGUARD = "com.android.systemui";
    private static final String PKGNAME_OF_SECURITYMGR = "com.huawei.securitymgr";
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
    private final Runnable mAddBackFingprintRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.mDefaultDisplayHeight = Global.getInt(FingerViewController.this.mContext.getContentResolver(), FingerViewController.APS_INIT_HEIGHT, FingerViewController.DEFAULT_INIT_HEIGHT);
            FingerViewController.this.mDefaultDisplayWidth = Global.getInt(FingerViewController.this.mContext.getContentResolver(), FingerViewController.APS_INIT_WIDTH, 1440);
            FingerViewController.this.createBackFingprintView();
        }
    };
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
    private HwAodManager mAodManager;
    private int mAuthenticateResult;
    private Bitmap mBLurBitmap;
    private Button mBackFingerprintCancelView;
    private TextView mBackFingerprintHintView;
    private Button mBackFingerprintUsePasswordView;
    private BackFingerprintView mBackFingerprintView;
    private BitmapDrawable mBlurDrawable;
    private int mBrightnessMode = 1;
    private int mButtonCenterX = -1;
    private int mButtonCenterY = -1;
    private int mButtonColor = 0;
    private SuspensionButton mButtonView;
    private boolean mButtonViewAdded = false;
    private int mButtonViewState = 1;
    private RelativeLayout mCancelView;
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mCurrentBrightness = -1;
    private int mCurrentHeight;
    private int mCurrentRotation;
    private int mCurrentWidth;
    private int mDefaultDisplayHeight;
    private int mDefaultDisplayWidth;
    private IBiometricPromptReceiver mDialogReceiver;
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
    private float mFontScale;
    private Handler mHandler = null;
    protected final HandlerThread mHandlerThread = new HandlerThread(TAG);
    private boolean mHasBackFingerprint = false;
    private boolean mHasUdFingerprint = true;
    private int mHighLightRemoveType;
    private int mHighLightShowType;
    private HighLightMaskView mHighLightView;
    private final Runnable mHighLightViewRunnable = new Runnable() {
        public void run() {
            switch (FingerViewController.this.mHighLightShowType) {
                case 0:
                    Log.d(FingerViewController.TAG, " SETTINGS enter");
                    FingerViewController.this.createAndAddHighLightView();
                    FingerViewController.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            FingerViewController.this.setLightLevel(FingerViewController.this.mHighlightBrightnessLevel, 100);
                        }
                    }, 100);
                    if (FingerViewController.this.mHighLightView != null) {
                        int endAlpha = FingerViewController.this.mHighLightView.getMaskAlpha(FingerViewController.this.mCurrentBrightness);
                        String str = FingerViewController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(" alpha:");
                        stringBuilder.append(endAlpha);
                        Log.d(str, stringBuilder.toString());
                        FingerViewController.this.startAlphaValueAnimation(FingerViewController.this.mHighLightView, true, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, ((float) endAlpha) / 255.0f, 0, 200);
                        return;
                    }
                    return;
                case 1:
                    Log.d(FingerViewController.TAG, " AUTHENTICATE enter");
                    FingerViewController.this.createAndAddHighLightView();
                    return;
                default:
                    Log.d(FingerViewController.TAG, " default no operation");
                    return;
            }
        }
    };
    private int mHighlightBrightnessLevel;
    private int mHighlightSpotRadius;
    private String mHint;
    private HintText mHintView;
    private boolean mIsFingerFrozen = false;
    private boolean mIsNeedReload;
    private boolean mIsSingleModeObserverRegistered = false;
    private boolean mKeepMaskAfterAuthentication = false;
    private RelativeLayout mLayoutForAlipay;
    private LayoutInflater mLayoutInflater;
    private RemainTimeCountDown mMyCountDown = null;
    private int mNormalizedMaxBrightness = INVALID_BRIGHTNESS;
    private int mNormalizedMinBrightness = INVALID_BRIGHTNESS;
    private Bundle mPkgAttributes;
    private String mPkgName;
    private PowerManager mPowerManager;
    private IFingerprintServiceReceiver mReceiver;
    private int mRemainTimes = 5;
    private int mRemainedSecs;
    private RelativeLayout mRemoteView;
    private final Runnable mRemoveBackFingprintRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.removeBackFingprintView();
        }
    };
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
    private final Runnable mRemoveHighLightView = new Runnable() {
        public void run() {
            Log.d(FingerViewController.TAG, "mRemoveHighLightView");
            FingerViewController.this.removeHighLightViewInner();
        }
    };
    private final Runnable mRemoveHighlightCircleRunnable = new Runnable() {
        public void run() {
            String str = FingerViewController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoveHighlightCircle highLightViewAdded = ");
            stringBuilder.append(FingerViewController.this.highLightViewAdded);
            Log.d(str, stringBuilder.toString());
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
    private int mSavedBackViewDpi;
    private int mSavedBackViewHeight;
    private int mSavedBackViewRotation;
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
            if (FingerViewController.this.mHighLightShowType == 0) {
                String str = FingerViewController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("highLightViewAdded = ");
                stringBuilder.append(FingerViewController.this.highLightViewAdded);
                Log.d(str, stringBuilder.toString());
                if (FingerViewController.this.highLightViewAdded && FingerViewController.this.mHighLightView.getCircleVisibility() == 4) {
                    FingerViewController.this.mHandler.removeCallbacks(FingerViewController.this.mRemoveHighlightCircleRunnable);
                    FingerViewController.this.getCurrentFingerprintCenter();
                    FingerViewController.this.mHighLightView.setCenterPoints(FingerViewController.this.mFingerprintCenterX, FingerViewController.this.mFingerprintCenterY);
                    FingerViewController.this.mHighLightView.setCircleVisibility(0);
                    FingerViewController.this.mHandler.postDelayed(FingerViewController.this.mRemoveHighlightCircleRunnable, 1200);
                }
            }
        }
    };
    private SingleModeContentObserver mSingleContentObserver;
    private String mSubTitle;
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
    private final Runnable mUpdateFingprintRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.updateBackFingprintView();
        }
    };
    private final Runnable mUpdateImageOnlyRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.updateImageOnlyView();
        }
    };
    private final Runnable mUpdateMaskAttibuteRunnable = new Runnable() {
        public void run() {
            FingerViewController.this.updateHintView(FingerViewController.this.mHint);
            if (FingerViewController.this.mFingerprintView != null) {
                FingerViewController.this.mFingerprintView.setContentDescription(FingerViewController.this.mHint);
            }
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
                    String str = FingerViewController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onChange:");
                    stringBuilder.append(FingerViewController.this.mDefaultDisplayHeight);
                    stringBuilder.append(",");
                    stringBuilder.append(FingerViewController.this.mDefaultDisplayWidth);
                    stringBuilder.append(",");
                    stringBuilder.append(FingerViewController.this.mCurrentHeight);
                    stringBuilder.append(",");
                    stringBuilder.append(FingerViewController.this.mCurrentWidth);
                    Log.d(str, stringBuilder.toString());
                }
            }, 30);
        }
    };

    public interface ICallBack {
        void onFingerViewStateChange(int i);

        void onNotifyBlueSpotDismiss();

        void onNotifyCaptureImage();
    }

    private class RemainTimeCountDown extends CountDownTimer {
        public RemainTimeCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onTick(long millisUntilFinished) {
            FingerViewController.this.mRemainedSecs = (int) ((((double) millisUntilFinished) / 1000.0d) + 0.5d);
            if (FingerViewController.this.mRemainedSecs <= 0) {
                return;
            }
            if (FingerViewController.this.mFingerView != null && FingerViewController.this.mFingerView.isAttachedToWindow()) {
                FingerViewController.this.updateHintView(FingerViewController.this.mContext.getResources().getQuantityString(34406409, FingerViewController.this.mRemainedSecs, new Object[]{Integer.valueOf(FingerViewController.this.mRemainedSecs)}));
                FingerViewController.this.mFingerprintView.setContentDescription("");
            } else if (FingerViewController.this.mBackFingerprintView != null && FingerViewController.this.mBackFingerprintView.isAttachedToWindow()) {
                FingerViewController.this.updateBackFingerprintHintView(FingerViewController.this.mContext.getResources().getQuantityString(34406409, FingerViewController.this.mRemainedSecs, new Object[]{Integer.valueOf(FingerViewController.this.mRemainedSecs)}));
            }
        }

        public void onFinish() {
            Log.d(FingerViewController.TAG, "RemainTimeCountDown onFinish");
            FingerViewController.this.mIsFingerFrozen = false;
            if (FingerViewController.this.mFingerView != null && FingerViewController.this.mFingerView.isAttachedToWindow()) {
                if (FingerViewController.this.mHasBackFingerprint) {
                    FingerViewController.this.updateHintView(FingerViewController.this.mContext.getString(33686061));
                } else {
                    FingerViewController.this.updateHintView(FingerViewController.this.mContext.getString(33686067));
                }
                FingerViewController.this.mFingerprintView.setContentDescription(FingerViewController.this.mContext.getString(33686191));
            } else if (FingerViewController.this.mBackFingerprintView != null && FingerViewController.this.mBackFingerprintView.isAttachedToWindow()) {
                FingerViewController.this.updateBackFingerprintHintView(FingerViewController.this.mSubTitle);
            }
        }
    }

    private class FingerprintViewCallback implements huawei.com.android.server.fingerprint.FingerprintView.ICallBack {
        private FingerprintViewCallback() {
        }

        /* synthetic */ FingerprintViewCallback(FingerViewController x0, AnonymousClass1 x1) {
            this();
        }

        public void userActivity() {
            FingerViewController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }

        public void onConfigurationChanged(Configuration newConfig) {
        }

        public void onDrawFinish() {
        }
    }

    private class SingleModeContentCallback implements huawei.com.android.server.fingerprint.SingleModeContentObserver.ICallBack {
        private SingleModeContentCallback() {
        }

        /* synthetic */ SingleModeContentCallback(FingerViewController x0, AnonymousClass1 x1) {
            this();
        }

        public void onContentChange() {
            if (((FingerViewController.this.mFingerprintView != null && FingerViewController.this.mFingerprintView.isAttachedToWindow()) || (FingerViewController.this.mLayoutForAlipay != null && FingerViewController.this.mLayoutForAlipay.isAttachedToWindow())) && !Global.getString(FingerViewController.this.mContext.getContentResolver(), "single_hand_mode").isEmpty()) {
                Global.putString(FingerViewController.this.mContext.getContentResolver(), "single_hand_mode", "");
            }
        }
    }

    private class SuspensionButtonCallback implements huawei.com.android.server.fingerprint.SuspensionButton.ICallBack {
        private SuspensionButtonCallback() {
        }

        /* synthetic */ SuspensionButtonCallback(FingerViewController x0, AnonymousClass1 x1) {
            this();
        }

        public void onButtonViewMoved(float endX, float endY) {
            if (FingerViewController.this.mButtonView != null) {
                FingerViewController.this.mSuspensionButtonParams.x = (int) (endX - (((float) FingerViewController.this.mSuspensionButtonParams.width) * 0.5f));
                FingerViewController.this.mSuspensionButtonParams.y = (int) (endY - (((float) FingerViewController.this.mSuspensionButtonParams.height) * 0.5f));
                String str = FingerViewController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onButtonViewUpdate,x = ");
                stringBuilder.append(FingerViewController.this.mSuspensionButtonParams.x);
                stringBuilder.append(" ,y = ");
                stringBuilder.append(FingerViewController.this.mSuspensionButtonParams.y);
                Log.d(str, stringBuilder.toString());
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
            Context access$1800 = FingerViewController.this.mContext;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{PkgName:");
            stringBuilder.append(FingerViewController.this.mPkgName);
            stringBuilder.append("}");
            Flog.bdReport(access$1800, 501, stringBuilder.toString());
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
        this.mHandler = new HwExHandler(this.mHandlerThread.getLooper());
        this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.pm = Stub.asInterface(ServiceManager.getService("power"));
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mFingerprintManagerEx = new FingerprintManagerEx(this.mContext);
        this.mAodManager = HwAodManager.getInstance();
        this.mDisplayEngineManager = new DisplayEngineManager();
        getBrightnessRangeFromPanelInfo();
        long identityToken = Binder.clearCallingIdentity();
        try {
            this.mContentResolver.registerContentObserver(Global.getUriFor(APS_INIT_HEIGHT), false, this.settingsDisplayObserver);
            this.mContentResolver.registerContentObserver(Global.getUriFor(APS_INIT_WIDTH), false, this.settingsDisplayObserver);
            this.mContentResolver.registerContentObserver(Global.getUriFor("display_size_forced"), false, this.settingsDisplayObserver);
            this.settingsDisplayObserver.onChange(true);
            Configuration curConfig = new Configuration();
            try {
                curConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
                this.mFontScale = curConfig.fontScale;
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to retrieve font size");
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("xyx1,current package is ");
        stringBuilder.append(this.mPkgName);
        Log.i(str, stringBuilder.toString());
        return this.mPkgName;
    }

    public void showMaskOrButton(String pkgName, Bundle bundle, IFingerprintServiceReceiver receiver, int type, boolean hasUdFingerprint, boolean hasBackFingerprint, IBiometricPromptReceiver dialogReceiver) {
        this.mPkgName = pkgName;
        this.mPkgAttributes = bundle;
        this.mWidgetColorSet = false;
        this.mHasUdFingerprint = hasUdFingerprint;
        this.mHasBackFingerprint = hasBackFingerprint;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mPkgAttributes has been init, mPkgAttributes=");
        stringBuilder.append(bundle);
        stringBuilder.append("mHighlightBrightnessLevel = ");
        stringBuilder.append(this.mHighlightBrightnessLevel);
        Log.d(str, stringBuilder.toString());
        if (this.mPkgAttributes == null || this.mPkgAttributes.getString("SystemTitle") == null) {
            this.mUseDefaultHint = true;
        } else {
            this.mUseDefaultHint = false;
        }
        this.mReceiver = receiver;
        this.mDialogReceiver = dialogReceiver;
        if ("com.huawei.systemmanager".equals(pkgName)) {
            Log.d(TAG, "do not show mask for systemmanager");
        } else if (PKGNAME_OF_SECURITYMGR.equals(pkgName)) {
            Log.d(TAG, "do not show mask for securitymgr");
        } else {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("type of package ");
            stringBuilder2.append(pkgName);
            stringBuilder2.append(" is ");
            stringBuilder2.append(type);
            Log.d(str2, stringBuilder2.toString());
            if (this.mPkgName == null || !this.mPkgName.equals(PKGNAME_OF_KEYGUARD)) {
                this.mWindowType = HwArbitrationDEFS.MSG_VPN_STATE_OPEN;
                if (type == 0) {
                    this.mHandler.post(this.mAddFingerViewRunnable);
                    Context context = this.mContext;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("{PkgName:");
                    stringBuilder.append(this.mPkgName);
                    stringBuilder.append("}");
                    Flog.bdReport(context, 501, stringBuilder.toString());
                } else if (type == 1) {
                    this.mHandler.post(this.mAddButtonViewRunnable);
                } else if (type == 3) {
                    this.mHandler.post(this.mAddImageOnlyRunnable);
                } else if (type == 4) {
                    this.mHandler.post(this.mAddBackFingprintRunnable);
                }
                return;
            }
            this.mWindowType = 2014;
        }
    }

    public void showMaskForApp(Bundle attribute) {
        this.mPkgName = getForegroundPkgName();
        this.mPkgAttributes = attribute;
        if (this.mPkgName == null || !this.mPkgName.equals(PKGNAME_OF_KEYGUARD)) {
            this.mWindowType = HwArbitrationDEFS.MSG_VPN_STATE_OPEN;
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mButtonCenterX = ");
        stringBuilder.append(this.mButtonCenterX);
        stringBuilder.append(",mButtonCenterY =");
        stringBuilder.append(this.mButtonCenterY);
        stringBuilder.append(",callingUidName = ");
        stringBuilder.append(callingUidName);
        Log.d(str, stringBuilder.toString());
        this.mButtonCenterX = centerX;
        this.mButtonCenterY = centerY;
        if (UIDNAME_OF_KEYGUARD.equals(callingUidName)) {
            this.mPkgName = PKGNAME_OF_KEYGUARD;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mButtonCenterX = ");
        stringBuilder.append(this.mButtonCenterX);
        stringBuilder.append(",mButtonCenterY =");
        stringBuilder.append(this.mButtonCenterY);
        Log.d(str, stringBuilder.toString());
        this.mWindowType = HwArbitrationDEFS.MSG_VPN_STATE_OPEN;
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
        this.mHandler.post(this.mRemoveBackFingprintRunnable);
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateMaskViewAttributes,hint = ");
                stringBuilder.append(hint);
                Log.d(str, stringBuilder.toString());
                this.mHint = hint;
                this.mHandler.post(this.mUpdateMaskAttibuteRunnable);
            }
        }
    }

    public void updateFingerprintView(int result, boolean keepMaskAfterAuthentication) {
        this.mAuthenticateResult = result;
        this.mKeepMaskAfterAuthentication = keepMaskAfterAuthentication;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mUseDefaultHint = ");
        stringBuilder.append(this.mUseDefaultHint);
        Log.d(str, stringBuilder.toString());
        if (this.mFingerView != null && this.mFingerView.isAttachedToWindow()) {
            this.mHandler.post(this.mUpdateFingerprintViewRunnable);
        } else if (this.mBackFingerprintView != null && this.mBackFingerprintView.isAttachedToWindow()) {
            this.mHandler.post(this.mUpdateFingprintRunnable);
        }
    }

    public void updateFingerprintView(int result, int failTimes) {
        if (result != 2) {
            this.mRemainTimes = 5 - failTimes;
        }
        this.mAuthenticateResult = result;
        if (this.mFingerView != null && this.mFingerView.isAttachedToWindow()) {
            this.mHandler.post(this.mUpdateFingerprintViewRunnable);
        } else if (this.mBackFingerprintView != null && this.mBackFingerprintView.isAttachedToWindow()) {
            this.mHandler.post(this.mUpdateFingprintRunnable);
        }
        if (this.mLayoutForAlipay != null && this.mLayoutForAlipay.isAttachedToWindow()) {
            this.mHandler.post(this.mUpdateImageOnlyRunnable);
        }
    }

    public void showHighlightview(int type) {
        if (!isScreenOn() || this.highLightViewAdded) {
            Log.d(TAG, "Screen not on or already added");
            return;
        }
        this.mHighLightShowType = type;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("show Highlightview mHighLightShowType:");
        stringBuilder.append(this.mHighLightShowType);
        Log.d(str, stringBuilder.toString());
        this.mHandler.removeCallbacks(this.mHighLightViewRunnable);
        this.mHandler.removeCallbacks(this.mRemoveHighLightView);
        this.mHandler.post(this.mHighLightViewRunnable);
        if (type == 1) {
            this.mHandler.postDelayed(this.mRemoveHighLightView, 1200);
        }
    }

    public void removeHighlightview(int type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeHighlightview mHighLightShowType:");
        stringBuilder.append(this.mHighLightShowType);
        Log.d(str, stringBuilder.toString());
        this.mHighLightRemoveType = type;
        if (this.highLightViewAdded) {
            this.mHandler.removeCallbacks(this.mRemoveHighLightView);
        }
        this.mHandler.removeCallbacks(this.mHighLightViewRunnable);
        if (this.mHighLightShowType == 0) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    FingerViewController.this.setLightLevel(-1, 150);
                    if (FingerViewController.this.mHighLightView != null) {
                        FingerViewController.this.startAlphaValueAnimation(FingerViewController.this.mHighLightView, false, FingerViewController.this.mHighLightView.getAlpha() / 255.0f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 70, 80);
                    }
                }
            });
        } else {
            this.mHandler.post(this.mRemoveHighLightView);
        }
    }

    private void initFingerPrintViewSubContentDes() {
        if (this.mFingerprintView != null) {
            this.mFingerprintView.setContentDescription(this.mContext.getString(33686191));
        }
        if (this.mCancelView != null) {
            this.mCancelView.setContentDescription(this.mContext.getString(33686192));
        }
    }

    private void initAddButtonViwSubContentDes() {
        if (this.mButtonView != null) {
            this.mButtonView.setContentDescription(this.mContext.getString(33686193));
        }
    }

    private void createFingerprintView() {
        initBaseElement();
        updateBaseElementMargins();
        updateExtraElement();
        initFingerprintViewParams();
        Log.d(TAG, "createFingerprintView called ,reset Hint");
        this.mFingerViewParams.type = this.mWindowType;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fingerviewadded,mWidgetColor = ");
        stringBuilder.append(this.mWidgetColor);
        Log.d(str, stringBuilder.toString());
        initFingerPrintViewSubContentDes();
        if (!this.mFingerprintViewAdded) {
            startBlurScreenshot();
            this.mWidgetColor = HwColorPicker.processBitmap(this.mScreenShot).getWidgetColor();
            this.mWidgetColorSet = true;
            this.mWindowManager.addView(this.mFingerView, this.mFingerViewParams);
            this.mFingerprintViewAdded = true;
            exitSingleHandMode();
            registerSingerHandObserver();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("addFingerprintView is done,is View Added = ");
            stringBuilder.append(this.mFingerprintViewAdded);
            Log.d(str, stringBuilder.toString());
        }
    }

    private void initBaseElement() {
        int curRotation = this.mWindowManager.getDefaultDisplay().getRotation();
        int dpi = SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)));
        if (this.mFingerView != null && this.mCurrentHeight == this.mSavedMaskHeight && curRotation == this.mSavedRotation && dpi == this.mSavedMaskDpi) {
            Log.d(TAG, "don't need to inflate mFingerView again");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dpi or rotation has changed, mCurrentHeight = ");
        stringBuilder.append(this.mCurrentHeight);
        stringBuilder.append(", mSavedMaskHeight = ");
        stringBuilder.append(this.mSavedMaskHeight);
        stringBuilder.append(",inflate mFingerView");
        Log.d(str, stringBuilder.toString());
        this.mSavedMaskDpi = dpi;
        this.mSavedMaskHeight = this.mCurrentHeight;
        this.mSavedRotation = curRotation;
        this.mFingerView = (FingerprintView) this.mLayoutInflater.inflate(34013295, null);
        this.mFingerView.setCallback(new FingerprintViewCallback(this, null));
        this.mFingerprintView = (ImageView) this.mFingerView.findViewById(34603056);
        this.mCancelView = (RelativeLayout) this.mFingerView.findViewById(34603033);
        this.mRemoteView = (RelativeLayout) this.mFingerView.findViewById(34603247);
        this.mCancelView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FingerViewController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                FingerViewController.this.onCancelClick();
                Context access$1800 = FingerViewController.this.mContext;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("{PkgName:");
                stringBuilder.append(FingerViewController.this.mPkgName);
                stringBuilder.append("}");
                Flog.bdReport(access$1800, 502, stringBuilder.toString());
                String foregroundPkgName = FingerViewController.this.getForegroundPkgName();
                if (foregroundPkgName != null && FingerViewController.this.isBroadcastNeed(foregroundPkgName)) {
                    Intent cancelMaskIntent = new Intent(FingerViewController.FINGERPRINT_IN_DISPLAY);
                    cancelMaskIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPCODE_KEY, 1011);
                    cancelMaskIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_KEY, FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_CLOSE_VIEW_VALUE);
                    cancelMaskIntent.setPackage(foregroundPkgName);
                    FingerViewController.this.mContext.sendBroadcast(cancelMaskIntent);
                } else if (FingerViewController.this.mDialogReceiver != null) {
                    try {
                        FingerViewController.this.mDialogReceiver.onDialogDismissed(2);
                    } catch (RemoteException e) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                } else if (FingerViewController.this.mReceiver != null) {
                    try {
                        FingerViewController.this.mReceiver.onAcquired(0, 6, 11);
                    } catch (RemoteException e2) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
    }

    private void updateBaseElementMargins() {
        StringBuilder stringBuilder;
        getCurrentRotation();
        int[] fingerprintMargin = calculateFingerprintMargin();
        RelativeLayout relativeLayout = (RelativeLayout) this.mFingerView.findViewById(34603021);
        MarginLayoutParams fingerviewLayoutParams = (MarginLayoutParams) relativeLayout.getLayoutParams();
        float dpiScale = getDPIScale();
        if (this.mCurrentRotation == 1 || this.mCurrentRotation == 3) {
            fingerviewLayoutParams.width = (fingerprintMargin[0] * 2) + ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472302)) * dpiScale) + 0.5f));
            int[] layoutMargin = calculateFingerprintLayoutLeftMargin(fingerviewLayoutParams.width);
            fingerviewLayoutParams.leftMargin = layoutMargin[0];
            fingerviewLayoutParams.rightMargin = layoutMargin[1];
            relativeLayout.setLayoutParams(fingerviewLayoutParams);
            MarginLayoutParams remoteViewLayoutParams = (MarginLayoutParams) this.mRemoteView.getLayoutParams();
            int remoteViewLeftLayoutmargin = calculateRemoteViewLeftMargin(fingerviewLayoutParams.width);
            remoteViewLayoutParams.width = (int) ((((((float) this.mCurrentHeight) - (((float) (2 * this.mContext.getResources().getDimensionPixelSize(34472380))) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472370)) * dpiScale)) - ((float) fingerviewLayoutParams.width)) + 0.5f);
            remoteViewLayoutParams.leftMargin = remoteViewLeftLayoutmargin;
            remoteViewLayoutParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472383)) * dpiScale) + 0.5f);
            remoteViewLayoutParams.rightMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472382)) * dpiScale) + 0.5f);
            remoteViewLayoutParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472379)) * dpiScale) + 0.5f);
            this.mRemoteView.setLayoutParams(remoteViewLayoutParams);
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" RemoteviewLayoutParams.leftMargin =");
            stringBuilder.append(remoteViewLayoutParams.leftMargin);
            stringBuilder.append(",rightMargin =");
            stringBuilder.append(remoteViewLayoutParams.rightMargin);
            Log.d(str, stringBuilder.toString());
        } else {
            fingerviewLayoutParams.width = this.mCurrentWidth;
            fingerviewLayoutParams.leftMargin = 0;
        }
        MarginLayoutParams fingerprintImageParams = (MarginLayoutParams) this.mFingerprintView.getLayoutParams();
        fingerprintImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472303)) * dpiScale) + 0.5f);
        fingerprintImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472302)) * dpiScale) + 0.5f);
        fingerprintImageParams.leftMargin = fingerprintMargin[0];
        fingerprintImageParams.topMargin = fingerprintMargin[1];
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerprintViewParams.width = ");
        stringBuilder2.append(fingerprintImageParams.width);
        stringBuilder2.append(", fingerprintViewParams.height = ");
        stringBuilder2.append(fingerprintImageParams.height);
        Log.d(str2, stringBuilder2.toString());
        this.mFingerprintView.setLayoutParams(fingerprintImageParams);
        MarginLayoutParams cancelViewParams = (MarginLayoutParams) this.mCancelView.getLayoutParams();
        if (this.mCurrentRotation == 1 || this.mCurrentRotation == 3) {
            cancelViewParams.topMargin = (int) (((((float) this.mCurrentWidth) - (((float) this.mContext.getResources().getDimensionPixelSize(34472255)) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472416)) * dpiScale)) + 0.5f);
        } else {
            cancelViewParams.topMargin = (int) (((((float) this.mCurrentHeight) - (((float) this.mContext.getResources().getDimensionPixelSize(34472255)) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472416)) * dpiScale)) + 0.5f);
        }
        ImageView cancelImage = (ImageView) this.mCancelView.findViewById(34603032);
        MarginLayoutParams cancelImageParams = (MarginLayoutParams) cancelImage.getLayoutParams();
        cancelImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472415)) * dpiScale) + 0.5f);
        cancelImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472415)) * dpiScale) + 0.5f);
        cancelImage.setLayoutParams(cancelImageParams);
        String str3 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("cancelViewParams.topMargin = ");
        stringBuilder.append(cancelViewParams.topMargin);
        Log.d(str3, stringBuilder.toString());
        cancelViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472299)) * dpiScale) + 0.5f);
        cancelViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472298)) * dpiScale) + 0.5f);
        this.mCancelView.setLayoutParams(cancelViewParams);
    }

    private void updateExtraElement() {
        float dpiScale = getDPIScale();
        RelativeLayout titleAndSummaryView = (RelativeLayout) this.mFingerView.findViewById(34603023);
        MarginLayoutParams titleAndSummaryViewParams = (MarginLayoutParams) titleAndSummaryView.getLayoutParams();
        titleAndSummaryViewParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472121)) * dpiScale) + 0.5f);
        titleAndSummaryView.setLayoutParams(titleAndSummaryViewParams);
        TextView appNameView = (TextView) this.mFingerView.findViewById(34603022);
        appNameView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472120)) * dpiScale) + 0.5f)));
        TextView accountMessageView = (TextView) this.mFingerView.findViewById(34603008);
        accountMessageView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34471966)) * dpiScale) + 0.5f)));
        MarginLayoutParams accountMessageViewParams = (MarginLayoutParams) accountMessageView.getLayoutParams();
        accountMessageViewParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472083)) * dpiScale) + 0.5f);
        accountMessageView.setLayoutParams(accountMessageViewParams);
        RelativeLayout usePasswordHotSpot = (RelativeLayout) this.mFingerView.findViewById(34603290);
        MarginLayoutParams usePasswordHotSpotParams = (MarginLayoutParams) usePasswordHotSpot.getLayoutParams();
        usePasswordHotSpotParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472421)) * dpiScale) + 0.5f);
        usePasswordHotSpotParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472420)) * dpiScale) + 0.5f);
        usePasswordHotSpot.setLayoutParams(usePasswordHotSpotParams);
        TextView usePasswordView = (TextView) this.mFingerView.findViewById(34603024);
        usePasswordView.setText(this.mContext.getString(33686066));
        usePasswordView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472422)) * dpiScale) + 0.5f)));
        RelativeLayout usePasswordHotspotLayout = (RelativeLayout) this.mFingerView.findViewById(34603290);
        if (usePasswordHotspotLayout != null) {
            usePasswordHotspotLayout.setContentDescription(this.mContext.getString(33686066));
        }
        this.mHintView = (HintText) this.mFingerView.findViewById(34603057);
        resetFrozenCountDownIfNeed();
        if (this.mIsFingerFrozen) {
            this.mHintView.setText(this.mContext.getResources().getQuantityString(34406409, this.mRemainedSecs, new Object[]{Integer.valueOf(this.mRemainedSecs)}));
        } else if (this.mHasBackFingerprint) {
            this.mHintView.setText(this.mContext.getString(33686061));
        } else {
            this.mHintView.setText(this.mContext.getString(33686067));
        }
        this.mHintView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472318)) * dpiScale) + 0.5f)));
        MarginLayoutParams hintViewParams = (MarginLayoutParams) this.mHintView.getLayoutParams();
        hintViewParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472317)) * dpiScale) + 0.5f);
        this.mHintView.setLayoutParams(hintViewParams);
        usePasswordHotSpot.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(FingerViewController.TAG, "onClick,UsePassword");
                FingerViewController.this.onUsePasswordClick();
                String foregroundPkgName = FingerViewController.this.getForegroundPkgName();
                String str = FingerViewController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("foregroundPackageName = ");
                stringBuilder.append(foregroundPkgName);
                Log.d(str, stringBuilder.toString());
                if (foregroundPkgName != null && FingerViewController.this.isBroadcastNeed(foregroundPkgName)) {
                    Intent usePasswordIntent = new Intent(FingerViewController.FINGERPRINT_IN_DISPLAY);
                    usePasswordIntent.setPackage(foregroundPkgName);
                    usePasswordIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPCODE_KEY, 1010);
                    usePasswordIntent.putExtra(FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_KEY, FingerViewController.FINGERPRINT_IN_DISPLAY_HELPSTRING_USE_PASSWORD_VALUE);
                    FingerViewController.this.mContext.sendBroadcast(usePasswordIntent);
                } else if (FingerViewController.this.mDialogReceiver != null) {
                    try {
                        FingerViewController.this.mDialogReceiver.onDialogDismissed(1);
                    } catch (RemoteException e) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                } else if (FingerViewController.this.mReceiver != null) {
                    try {
                        FingerViewController.this.mReceiver.onError(0, 10, 0);
                    } catch (RemoteException e2) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
        if (this.mPkgAttributes != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mPkgAttributes =");
            stringBuilder.append(this.mPkgAttributes);
            Log.d(str, stringBuilder.toString());
            StringBuilder stringBuilder2;
            if (this.mPkgAttributes.getString("googleFlag") == null) {
                if (this.mPkgAttributes.getParcelable("CustView") != null) {
                    this.mRemoteView.setVisibility(0);
                    Log.d(TAG, "RemoteViews != null");
                    this.mRemoteView.addView(((RemoteViews) this.mPkgAttributes.getParcelable("CustView")).apply(this.mContext, this.mRemoteView));
                } else {
                    this.mRemoteView.setVisibility(4);
                }
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mPkgAttributes.getString= ");
                stringBuilder2.append(this.mPkgAttributes.getString("Title"));
                Log.d(str, stringBuilder2.toString());
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
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("attributestring =");
                    stringBuilder2.append(this.mPkgAttributes.getString("SystemTitle"));
                    Log.d(str, stringBuilder2.toString());
                    this.mHintView.setText(this.mPkgAttributes.getString("SystemTitle"));
                    return;
                }
                return;
            }
            if (this.mPkgAttributes.getString("title") != null) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("title =");
                stringBuilder2.append(this.mPkgAttributes.getString("title"));
                Log.d(str, stringBuilder2.toString());
                appNameView.setVisibility(0);
                appNameView.setText(this.mPkgAttributes.getString("title"));
            }
            if (this.mPkgAttributes.getString("subtitle") != null) {
                accountMessageView.setVisibility(0);
                accountMessageView.setText(this.mPkgAttributes.getString("subtitle"));
            } else {
                accountMessageView.setVisibility(8);
            }
            if (this.mPkgAttributes.getString("description") != null) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("description =");
                stringBuilder2.append(this.mPkgAttributes.getString("description"));
                Log.d(str, stringBuilder2.toString());
                this.mHintView.setText(this.mPkgAttributes.getString("description"));
            }
            if (this.mPkgAttributes.getString("positive_text") != null) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("positive_text =");
                stringBuilder2.append(this.mPkgAttributes.getString("positive_text"));
                Log.d(str, stringBuilder2.toString());
                usePasswordHotSpot.setVisibility(0);
                usePasswordView.setText(this.mPkgAttributes.getString("positive_text"));
                return;
            }
            usePasswordHotSpot.setVisibility(4);
            return;
        }
        int i = 4;
        this.mRemoteView.setVisibility(i);
        appNameView.setVisibility(i);
        accountMessageView.setVisibility(8);
        usePasswordHotSpot.setVisibility(i);
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
            this.mFingerViewParams.layoutInDisplayCutoutMode = 1;
            this.mFingerViewParams.flags = 201852424;
            LayoutParams layoutParams = this.mFingerViewParams;
            layoutParams.privateFlags |= 16;
            this.mFingerViewParams.format = -3;
            this.mFingerViewParams.screenOrientation = 14;
            this.mFingerViewParams.setTitle(FINGRPRINT_VIEW_TITLE_NAME);
        }
    }

    private void removeFingerprintView() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeFingerprintView start added = ");
        stringBuilder.append(this.mFingerprintViewAdded);
        Log.d(str, stringBuilder.toString());
        if (this.mFingerView != null && this.mFingerprintViewAdded) {
            this.mWindowManager.removeView(this.mFingerView);
            resetFingerprintView();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeFingerprintView is done is View Added = ");
            stringBuilder.append(this.mFingerprintViewAdded);
            stringBuilder.append("mFingerView =");
            stringBuilder.append(this.mFingerView);
            Log.d(str, stringBuilder.toString());
        }
    }

    private void updateFingerprintView() {
        if (this.mAuthenticateResult == 1) {
            if (this.mUseDefaultHint) {
                updateHintView();
            }
        } else if (this.mAuthenticateResult == 0) {
            if (this.mUseDefaultHint) {
                updateHintView(this.mContext.getString(33686063));
            }
            if (this.mFingerprintView != null) {
                this.mFingerprintView.setContentDescription(this.mContext.getString(33686063));
            }
            String foregroundPkg = getForegroundPkgName();
            for (String pkgName : PACKAGES_USE_HWAUTH_INTERFACE) {
                if (pkgName.equals(foregroundPkg)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("hw wallet Identifing,pkgName = ");
                    stringBuilder.append(pkgName);
                    Log.d(str, stringBuilder.toString());
                }
            }
            if (!this.mKeepMaskAfterAuthentication) {
                removeMaskOrButton();
            }
        } else if (this.mAuthenticateResult == 2) {
            if (!this.mUseDefaultHint) {
                return;
            }
            if (this.mRemainTimes == 5) {
                if (this.mHasBackFingerprint) {
                    updateHintView(this.mContext.getString(33686061));
                } else {
                    updateHintView(this.mContext.getString(33686067));
                }
                if (this.mFingerprintView != null) {
                    this.mFingerprintView.setContentDescription(this.mContext.getString(33686191));
                    return;
                }
                return;
            }
            updateHintView();
        } else if (this.mAuthenticateResult == 3) {
            if (this.mFingerprintView != null) {
                this.mFingerprintView.setContentDescription(this.mContext.getString(33686064));
            }
            if (this.mUseDefaultHint) {
                updateHintView(this.mContext.getString(33686064));
            }
        }
    }

    private void updateHintView() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateFingerprintView start,mFingerprintViewAdded = ");
        stringBuilder.append(this.mFingerprintViewAdded);
        Log.d(str, stringBuilder.toString());
        if (this.mFingerprintViewAdded && this.mHintView != null) {
            if (this.mRemainTimes > 0 && this.mRemainTimes < 5) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("remaind time = ");
                stringBuilder2.append(this.mRemainTimes);
                Log.d(str, stringBuilder2.toString());
                str = this.mContext.getResources().getQuantityString(34406410, this.mRemainTimes, new Object[]{Integer.valueOf(this.mRemainTimes)});
                this.mHintView.setText(str);
                if (this.mFingerprintView != null) {
                    this.mFingerprintView.setContentDescription(str);
                }
            } else if (this.mRemainTimes == 0) {
                if (!this.mIsFingerFrozen) {
                    if (this.mMyCountDown != null) {
                        this.mMyCountDown.cancel();
                    }
                    if (this.mFingerprintView != null) {
                        this.mFingerprintView.setContentDescription(this.mContext.getResources().getQuantityString(34406409, 30, new Object[]{Integer.valueOf(30)}));
                    }
                    this.mMyCountDown = new RemainTimeCountDown(HwArbitrationDEFS.DelayTimeMillisA, 1000);
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
        int curHeight = SystemPropertiesEx.getInt("persist.sys.rog.height", this.mDefaultDisplayHeight);
        int dpi = SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)));
        float dpiScale = getDPIScale();
        if (!(this.mLayoutForAlipay != null && curHeight == this.mSavedImageHeight && dpi == this.mSavedImageDpi)) {
            this.mSavedImageHeight = curHeight;
            this.mSavedImageDpi = dpi;
            this.mLayoutForAlipay = (RelativeLayout) this.mLayoutInflater.inflate(34013294, null);
            this.mFingerprintImageForAlipay = (ImageView) this.mLayoutForAlipay.findViewById(34603055);
        }
        if (this.mHasUdFingerprint) {
            this.mAlipayDrawable = new BreathImageDrawable(this.mContext);
            this.mAlipayDrawable.setBreathImageDrawable(null, this.mContext.getDrawable(33751843));
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
        } else {
            this.mFingerprintImageForAlipay.setImageResource(33751826);
        }
        int[] fingerprintMargin = calculateFingerprintImageMargin();
        MarginLayoutParams fingerprintImageParams = (MarginLayoutParams) this.mFingerprintImageForAlipay.getLayoutParams();
        fingerprintImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472301)) * dpiScale) + 0.5f);
        fingerprintImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472300)) * dpiScale) + 0.5f);
        this.mFingerprintImageForAlipay.setLayoutParams(fingerprintImageParams);
        LayoutParams fingerprintOnlyLayoutParams = new LayoutParams();
        fingerprintOnlyLayoutParams.flags = 16777480;
        fingerprintOnlyLayoutParams.privateFlags |= 16;
        fingerprintOnlyLayoutParams.format = -3;
        fingerprintOnlyLayoutParams.screenOrientation = 14;
        fingerprintOnlyLayoutParams.setTitle(FINGRPRINT_IMAGE_TITLE_NAME);
        fingerprintOnlyLayoutParams.gravity = 51;
        fingerprintOnlyLayoutParams.type = this.mWindowType;
        fingerprintOnlyLayoutParams.x = fingerprintMargin[0];
        fingerprintOnlyLayoutParams.y = fingerprintMargin[1];
        fingerprintOnlyLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472301)) * dpiScale) + 0.5f);
        fingerprintOnlyLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472300)) * dpiScale) + 0.5f);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fingerprintImage location = [");
        stringBuilder.append(fingerprintOnlyLayoutParams.x);
        stringBuilder.append(",");
        stringBuilder.append(fingerprintOnlyLayoutParams.y);
        stringBuilder.append("]");
        Log.d(str, stringBuilder.toString());
        if (!this.mFingerprintOnlyViewAdded) {
            this.mWindowManager.addView(this.mLayoutForAlipay, fingerprintOnlyLayoutParams);
            this.mFingerprintOnlyViewAdded = true;
            exitSingleHandMode();
            registerSingerHandObserver();
        }
    }

    private void updateImageOnlyView() {
        if (this.mAuthenticateResult == 0) {
            this.mHandler.post(this.mRemoveImageOnlyRunnable);
            if (this.mFingerViewChangeCallback != null) {
                this.mFingerViewChangeCallback.onFingerViewStateChange(0);
            }
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("left = ");
        stringBuilder.append(this.mFingerprintPosition[0]);
        stringBuilder.append("right = ");
        stringBuilder.append(this.mFingerprintPosition[2]);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("top = ");
        stringBuilder.append(this.mFingerprintPosition[1]);
        stringBuilder.append("button = ");
        stringBuilder.append(this.mFingerprintPosition[3]);
        Log.d(str, stringBuilder.toString());
        float dpiScale = getDPIScale();
        int[] margin = new int[2];
        int fingerPrintInScreenWidth = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472301)) * dpiScale) + 1056964608);
        int fingerPrintInScreenHeight = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472300)) * dpiScale) + 1056964608);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerPrintInScreenWidth= ");
        stringBuilder2.append(fingerPrintInScreenWidth);
        stringBuilder2.append("fingerPrintInScreenHeight = ");
        stringBuilder2.append(fingerPrintInScreenHeight);
        Log.d(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("current height = ");
        stringBuilder2.append(this.mCurrentHeight);
        stringBuilder2.append("mDefaultDisplayHeight = ");
        stringBuilder2.append(this.mDefaultDisplayHeight);
        Log.d(str2, stringBuilder2.toString());
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        getCurrentRotation();
        int marginLeft;
        if (this.mCurrentRotation == 0 || this.mCurrentRotation == 2) {
            this.mFingerprintCenterX = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            marginLeft = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
            int marginTop = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
            margin[0] = marginLeft;
            margin[1] = marginTop;
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("marginLeft = ");
            stringBuilder3.append(marginLeft);
            stringBuilder3.append("marginTop = ");
            stringBuilder3.append(marginTop);
            stringBuilder3.append("scale = ");
            stringBuilder3.append(scale);
            Log.d(str3, stringBuilder3.toString());
        } else {
            this.mFingerprintCenterX = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            marginLeft = (int) (((((float) this.mFingerprintCenterY) * scale) - (((float) fingerPrintInScreenHeight) / 2.0f)) + 1056964608);
            margin[0] = (int) ((((((float) (this.mDefaultDisplayHeight - this.mFingerprintCenterX)) * scale) - (((float) this.mContext.getResources().getDimensionPixelSize(34472370)) * dpiScale)) - (((float) fingerPrintInScreenWidth) / 2.0f)) + 1056964608);
            margin[1] = marginLeft;
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createAndAddButtonView,pkg = ");
        stringBuilder.append(this.mPkgName);
        Log.d(str, stringBuilder.toString());
        float dpiScale = getDPIScale();
        if (this.mButtonView == null || this.mCurrentHeight != this.mSavedButtonHeight) {
            this.mButtonView = (SuspensionButton) this.mLayoutInflater.inflate(34013293, null);
            this.mButtonView.setCallback(new SuspensionButtonCallback(this, null));
            this.mSavedButtonHeight = this.mCurrentHeight;
        }
        initAddButtonViwSubContentDes();
        this.mButtonView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
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
                Context access$1800 = FingerViewController.this.mContext;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("{PkgName:");
                stringBuilder.append(FingerViewController.this.mPkgName);
                stringBuilder.append("}");
                Flog.bdReport(access$1800, 501, stringBuilder.toString());
            }
        });
        ImageView buttonImage = (ImageView) this.mButtonView.findViewById(34603031);
        MarginLayoutParams buttonImageParams = (MarginLayoutParams) buttonImage.getLayoutParams();
        buttonImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472299)) * dpiScale) + 0.5f);
        buttonImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472298)) * dpiScale) + 0.5f);
        buttonImage.setLayoutParams(buttonImageParams);
        this.mSuspensionButtonParams = new LayoutParams();
        if (PKGNAME_OF_KEYGUARD.equals(this.mPkgName)) {
            this.mSuspensionButtonParams.type = 2014;
        } else {
            this.mSuspensionButtonParams.type = 2003;
        }
        this.mSuspensionButtonParams.flags = 16777480;
        this.mSuspensionButtonParams.gravity = 51;
        this.mSuspensionButtonParams.x = (int) ((((float) this.mButtonCenterX) - ((((float) this.mContext.getResources().getDimensionPixelSize(34472299)) * dpiScale) / 2.0f)) + 0.5f);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mSuspensionButtonParams.x=");
        stringBuilder2.append(this.mSuspensionButtonParams.x);
        Log.d(str2, stringBuilder2.toString());
        this.mSuspensionButtonParams.y = (int) ((((float) this.mButtonCenterY) - ((((float) this.mContext.getResources().getDimensionPixelSize(34472298)) * dpiScale) / 2.0f)) + 0.5f);
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mSuspensionButtonParams.y=");
        stringBuilder2.append(this.mSuspensionButtonParams.y);
        Log.d(str2, stringBuilder2.toString());
        this.mSuspensionButtonParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472299)) * dpiScale) + 0.5f);
        this.mSuspensionButtonParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472298)) * dpiScale) + 0.5f);
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
        if (PKGNAME_OF_KEYGUARD.equals(this.mPkgName) && this.mButtonColor != 0) {
            this.mWidgetColor = this.mButtonColor;
            this.mWidgetColorSet = true;
        }
        if (!this.mWidgetColorSet) {
            Log.d(TAG, "mWidgetColorSet is false, get a new screenshot and calculate color");
            getScreenShot();
            this.mWidgetColor = HwColorPicker.processBitmap(this.mScreenShot).getWidgetColor();
            this.mWidgetColorSet = true;
        }
        String str3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("mWidgetColor = ");
        stringBuilder3.append(this.mWidgetColor);
        Log.d(str3, stringBuilder3.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeButtonView begin, mButtonViewAdded added = ");
            stringBuilder.append(this.mButtonViewAdded);
            stringBuilder.append("mButtonView = ");
            stringBuilder.append(this.mButtonView);
            Log.d(str, stringBuilder.toString());
            this.mWindowManager.removeViewImmediate(this.mButtonView);
            this.mButtonViewAdded = false;
        }
    }

    private void updateButtonView() {
        calculateButtonPosition();
        if (this.mButtonView != null && this.mButtonViewAdded && this.mSuspensionButtonParams != null) {
            this.mSuspensionButtonParams.x = this.mButtonCenterX - (this.mContext.getResources().getDimensionPixelSize(34472416) / 2);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mSuspensionButtonParams.x=");
            stringBuilder.append(this.mSuspensionButtonParams.x);
            Log.d(str, stringBuilder.toString());
            this.mSuspensionButtonParams.y = this.mButtonCenterY - (this.mContext.getResources().getDimensionPixelSize(34472416) / 2);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mSuspensionButtonParams.y=");
            stringBuilder.append(this.mSuspensionButtonParams.y);
            Log.d(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" startAlphaAnimation current alpha:");
            stringBuilder.append(target.getAlpha());
            stringBuilder.append(" startAlpha:");
            stringBuilder.append(startAlpha);
            stringBuilder.append(" endAlpha: ");
            stringBuilder.append(endAlpha);
            stringBuilder.append(" duration :");
            stringBuilder.append(duration);
            Log.d(str, stringBuilder.toString());
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
                    String str = FingerViewController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" onAnimationEnd endAlpha:");
                    stringBuilder.append(endAlphaValue);
                    stringBuilder.append(" isCanceled:");
                    stringBuilder.append(this.isCanceled);
                    Log.d(str, stringBuilder.toString());
                    if (!(isAlphaUp || this.isCanceled)) {
                        FingerViewController.this.mHandler.post(FingerViewController.this.mRemoveHighLightView);
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
        this.mHandler.post(this.mRemoveHighLightView);
    }

    public void showHighlightCircle() {
        this.mHandler.post(this.mShowHighlightCircleRunnable);
    }

    public void removeHighlightCircle() {
        this.mHandler.post(this.mRemoveHighlightCircleRunnable);
    }

    public void notifyCaptureImage() {
        if (this.mFingerViewChangeCallback != null) {
            Log.d(TAG, "onNotifyCaptureImage ");
            this.mFingerViewChangeCallback.onNotifyCaptureImage();
        }
    }

    public void notifyDismissBlueSpot() {
        if (this.mFingerViewChangeCallback != null) {
            Log.d(TAG, "onNotifyBlueSpotDismiss ");
            this.mFingerViewChangeCallback.onNotifyBlueSpotDismiss();
        }
    }

    private void createAndAddHighLightView() {
        if (this.mHighLightView == null) {
            this.mHighLightView = new HighLightMaskView(this.mContext, this.mCurrentBrightness, this.mHighlightSpotRadius);
        }
        getCurrentFingerprintCenter();
        this.mHighLightView.setCenterPoints(this.mFingerprintCenterX, this.mFingerprintCenterY);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("current height = ");
        stringBuilder.append(this.mCurrentHeight);
        Log.d(str, stringBuilder.toString());
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
        highLightViewParams.layoutInDisplayCutoutMode = 1;
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
            setBacklightViaAod();
        }
    }

    private void setBacklightViaAod() {
        float maxBright = transformBrightnessViaScreen(this.mHighlightBrightnessLevel);
        float currentBright = transformBrightnessViaScreen(this.mCurrentBrightness);
        if (this.mAodManager != null) {
            this.mAodManager.setBacklight((int) maxBright, (int) currentBright);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mAodManager set Bright: max: ");
            stringBuilder.append(maxBright);
            stringBuilder.append(" current:");
            stringBuilder.append(currentBright);
            Log.d(str, stringBuilder.toString());
            if (this.mDisplayEngineManager != null) {
                this.mDisplayEngineManager.setScene(29, (int) maxBright);
                Log.d(TAG, "mDisplayEngineManager set scene");
                return;
            }
            return;
        }
        Log.d(TAG, "mAodManager is null");
    }

    private float transformBrightnessViaScreen(int brightness) {
        if (INVALID_BRIGHTNESS == this.mNormalizedMaxBrightness || INVALID_BRIGHTNESS == this.mNormalizedMinBrightness) {
            Log.i(TAG, "have not get the valid brightness, try again");
            getBrightnessRangeFromPanelInfo();
        }
        return (((((float) brightness) - 4.0f) / 251.0f) * ((float) (this.mNormalizedMaxBrightness - this.mNormalizedMinBrightness))) + ((float) this.mNormalizedMinBrightness);
    }

    private void removeHighLightViewInner() {
        if (this.highLightViewAdded && this.mHighLightView != null) {
            Log.i(TAG, "highlightview is show, remove highlightview");
            this.mWindowManager.removeView(this.mHighLightView);
        }
        this.mHighLightView = null;
        this.highLightViewAdded = false;
        this.mHandler.removeCallbacks(this.mSetScene);
        this.mHandler.postDelayed(this.mSetScene, 80);
    }

    private void createBackFingprintView() {
        int currentRotation = this.mWindowManager.getDefaultDisplay().getRotation();
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 640));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        Configuration curConfig = new Configuration();
        try {
            curConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }
        boolean fontScaleChange = curConfig.fontScale != this.mFontScale;
        if (fontScaleChange) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fontScaleChange before createBackFingprintView, curCenfig.fontScale : ");
            stringBuilder.append(curConfig.fontScale);
            stringBuilder.append(", mFontScale : ");
            stringBuilder.append(this.mFontScale);
            Log.e(str, stringBuilder.toString());
            this.mContext.getResources().updateConfiguration(curConfig, null);
            this.mFontScale = curConfig.fontScale;
        }
        if (!(this.mBackFingerprintView != null && this.mCurrentHeight == this.mSavedBackViewHeight && currentRotation == this.mSavedBackViewRotation && dpi == this.mSavedBackViewDpi && !fontScaleChange)) {
            this.mBackFingerprintView = (BackFingerprintView) this.mLayoutInflater.inflate(34013326, null);
            this.mSavedBackViewDpi = dpi;
            this.mSavedBackViewHeight = this.mCurrentHeight;
            this.mSavedBackViewRotation = currentRotation;
        }
        float dpiScale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        this.mBackFingerprintHintView = (TextView) this.mBackFingerprintView.findViewById(34603301);
        this.mBackFingerprintUsePasswordView = (Button) this.mBackFingerprintView.findViewById(34603303);
        this.mBackFingerprintCancelView = (Button) this.mBackFingerprintView.findViewById(34603298);
        RelativeLayout buttonLayout = (RelativeLayout) this.mBackFingerprintView.findViewById(34603297);
        MarginLayoutParams usePasswordViewParams = (MarginLayoutParams) this.mBackFingerprintUsePasswordView.getLayoutParams();
        usePasswordViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472241)) * dpiScale) + 0.5f);
        usePasswordViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472122)) * dpiScale) + 0.5f);
        this.mBackFingerprintUsePasswordView.setLayoutParams(usePasswordViewParams);
        MarginLayoutParams cancelViewParams = (MarginLayoutParams) this.mBackFingerprintCancelView.getLayoutParams();
        cancelViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472241)) * dpiScale) + 0.5f);
        cancelViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472122)) * dpiScale) + 0.5f);
        this.mBackFingerprintCancelView.setLayoutParams(cancelViewParams);
        MarginLayoutParams buttonLayoutParams = (MarginLayoutParams) buttonLayout.getLayoutParams();
        buttonLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472240)) * dpiScale) + 0.5f);
        buttonLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472239)) * dpiScale) + 0.5f);
        buttonLayout.setLayoutParams(buttonLayoutParams);
        TextView backFingerprintTitle = (TextView) this.mBackFingerprintView.findViewById(34603302);
        TextView backFingerprintDescription = (TextView) this.mBackFingerprintView.findViewById(34603299);
        if (this.mPkgAttributes != null) {
            String str2;
            StringBuilder stringBuilder2;
            if (this.mPkgAttributes.getString("title") != null) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("title =");
                stringBuilder3.append(this.mPkgAttributes.getString("title"));
                Log.d(str3, stringBuilder3.toString());
                backFingerprintTitle.setVisibility(0);
                backFingerprintTitle.setText(this.mPkgAttributes.getString("title"));
            }
            if (this.mPkgAttributes.getString("subtitle") != null) {
                this.mBackFingerprintHintView.setVisibility(0);
                this.mBackFingerprintHintView.setText(this.mPkgAttributes.getString("subtitle"));
                this.mSubTitle = this.mPkgAttributes.getString("subtitle");
            } else {
                this.mBackFingerprintHintView.setVisibility(4);
            }
            if (this.mPkgAttributes.getString("description") != null) {
                str2 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("description =");
                stringBuilder4.append(this.mPkgAttributes.getString("description"));
                Log.d(str2, stringBuilder4.toString());
                backFingerprintDescription.setText(this.mPkgAttributes.getString("description"));
            }
            if (this.mPkgAttributes.getString("positive_text") != null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("positive_text =");
                stringBuilder2.append(this.mPkgAttributes.getString("positive_text"));
                Log.d(str2, stringBuilder2.toString());
                this.mBackFingerprintUsePasswordView.setVisibility(0);
                this.mBackFingerprintUsePasswordView.setText(this.mPkgAttributes.getString("positive_text"));
            } else {
                this.mBackFingerprintUsePasswordView.setVisibility(4);
            }
            if (this.mPkgAttributes.getString("negative_text") != null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("negative_text =");
                stringBuilder2.append(this.mPkgAttributes.getString("negative_text"));
                Log.d(str2, stringBuilder2.toString());
                this.mBackFingerprintCancelView.setText(this.mPkgAttributes.getString("negative_text"));
            }
        }
        resetFrozenCountDownIfNeed();
        if (this.mIsFingerFrozen) {
            this.mBackFingerprintHintView.setText(this.mContext.getResources().getQuantityString(34406409, this.mRemainedSecs, new Object[]{Integer.valueOf(this.mRemainedSecs)}));
        }
        this.mBackFingerprintUsePasswordView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (FingerViewController.this.mDialogReceiver != null) {
                    try {
                        Log.i(FingerViewController.TAG, "back fingerprint view, usepassword clicked");
                        FingerViewController.this.mDialogReceiver.onDialogDismissed(1);
                    } catch (RemoteException e) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
        this.mBackFingerprintCancelView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FingerViewController.this.mHandler.post(FingerViewController.this.mRemoveBackFingprintRunnable);
                if (FingerViewController.this.mDialogReceiver != null) {
                    try {
                        Log.i(FingerViewController.TAG, "back fingerprint view, cancel clicked");
                        FingerViewController.this.mDialogReceiver.onDialogDismissed(2);
                    } catch (RemoteException e) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
        LayoutParams backFingerprintLayoutParams = new LayoutParams();
        backFingerprintLayoutParams.layoutInDisplayCutoutMode = 1;
        backFingerprintLayoutParams.flags = 201852424;
        backFingerprintLayoutParams.privateFlags |= 16;
        backFingerprintLayoutParams.format = -3;
        backFingerprintLayoutParams.type = HwArbitrationDEFS.MSG_VPN_STATE_OPEN;
        backFingerprintLayoutParams.screenOrientation = 14;
        backFingerprintLayoutParams.setTitle("back_fingerprint_view");
        if (!this.mBackFingerprintView.isAttachedToWindow()) {
            startBlurBackViewScreenshot();
            this.mWindowManager.addView(this.mBackFingerprintView, backFingerprintLayoutParams);
        }
    }

    private void removeBackFingprintView() {
        if (this.mBackFingerprintView != null && this.mBackFingerprintView.isAttachedToWindow()) {
            this.mWindowManager.removeView(this.mBackFingerprintView);
            if (this.mBLurBitmap != null) {
                this.mBLurBitmap.recycle();
            }
            if (this.mScreenShot != null) {
                this.mScreenShot.recycle();
                this.mScreenShot = null;
            }
        }
    }

    private void updateBackFingprintView() {
        if (this.mAuthenticateResult == 1) {
            updateBackFingerprintHintView();
        } else if (this.mAuthenticateResult == 0) {
            updateBackFingerprintHintView(this.mContext.getString(33686063));
            removeBackFingprintView();
        }
    }

    private void updateBackFingerprintHintView() {
        Log.d(TAG, "updateBackFingerprintHintView start");
        if (this.mBackFingerprintView != null && this.mBackFingerprintView.isAttachedToWindow() && this.mBackFingerprintHintView != null) {
            if (this.mRemainTimes > 0 && this.mRemainTimes < 5) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remaind time = ");
                stringBuilder.append(this.mRemainTimes);
                Log.d(str, stringBuilder.toString());
                this.mBackFingerprintHintView.setText(this.mContext.getResources().getQuantityString(34406410, this.mRemainTimes, new Object[]{Integer.valueOf(this.mRemainTimes)}));
            } else if (this.mRemainTimes == 0) {
                if (!this.mIsFingerFrozen) {
                    if (this.mMyCountDown != null) {
                        this.mMyCountDown.cancel();
                    }
                    this.mMyCountDown = new RemainTimeCountDown(HwArbitrationDEFS.DelayTimeMillisA, 1000);
                    this.mMyCountDown.start();
                    this.mIsFingerFrozen = true;
                } else {
                    return;
                }
            }
            this.mBackFingerprintView.postInvalidate();
        }
    }

    private void updateBackFingerprintHintView(String hint) {
        if (this.mBackFingerprintHintView != null) {
            this.mBackFingerprintHintView.setText(hint);
        }
        if (this.mBackFingerprintView != null && this.mBackFingerprintView.isAttachedToWindow()) {
            this.mBackFingerprintView.postInvalidate();
        }
    }

    private void startBlurBackViewScreenshot() {
        getScreenShot();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mScreenShot = ");
        stringBuilder.append(this.mScreenShot);
        Log.i(str, stringBuilder.toString());
        if (this.mScreenShot == null || (HwColorPicker.processBitmap(this.mScreenShot).getDomainColor() == COLOR_BLACK && !PKGNAME_OF_KEYGUARD.equals(this.mPkgName))) {
            this.mBLurBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
            this.mBLurBitmap.eraseColor(-7829368);
            this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
            this.mBackFingerprintView.setBackgroundDrawable(this.mBlurDrawable);
            return;
        }
        this.mBLurBitmap = BlurUtils.blurMaskImage(this.mContext, this.mScreenShot, this.mScreenShot, 25);
        this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
        this.mBackFingerprintView.setBackgroundDrawable(this.mBlurDrawable);
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" suspend:");
        stringBuilder.append(suspend);
        stringBuilder.append(" mButtonViewAdded:");
        stringBuilder.append(this.mButtonViewAdded);
        stringBuilder.append(" mFingerprintViewAdded:");
        stringBuilder.append(this.mFingerprintViewAdded);
        Log.d(str, stringBuilder.toString());
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
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" type:");
            stringBuilder2.append(type);
            stringBuilder2.append(" state:");
            stringBuilder2.append(state);
            Log.d(str2, stringBuilder2.toString());
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
                    this.mButtonColor = bundle.getInt("buttonColor", 0);
                    showSuspensionButtonForApp(location[0], location[1], UIDNAME_OF_KEYGUARD);
                    break;
                default:
                    break;
            }
        }
    }

    public void setFingerprintPosition(int[] position) {
        this.mFingerprintPosition = (int[]) position.clone();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setFingerprintPosition,left = ");
        stringBuilder.append(this.mFingerprintPosition[0]);
        stringBuilder.append("right = ");
        stringBuilder.append(this.mFingerprintPosition[2]);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setFingerprintPosition,top = ");
        stringBuilder.append(this.mFingerprintPosition[1]);
        stringBuilder.append("button = ");
        stringBuilder.append(this.mFingerprintPosition[3]);
        Log.d(str, stringBuilder.toString());
        this.mFingerprintCenterX = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
        this.mFingerprintCenterY = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
    }

    public void setHighLightBrightnessLevel(int brightness) {
        String str;
        StringBuilder stringBuilder;
        if (brightness > 255) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("brightness is ");
            stringBuilder.append(brightness);
            stringBuilder.append(",adjust it to 255");
            Log.i(str, stringBuilder.toString());
            brightness = 255;
        } else if (brightness < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("brightness is ");
            stringBuilder.append(brightness);
            stringBuilder.append(",adjust it to 0");
            Log.i(str, stringBuilder.toString());
            brightness = 0;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("brightness to be set is ");
        stringBuilder.append(brightness);
        Log.i(str, stringBuilder.toString());
        this.mHighlightBrightnessLevel = brightness;
    }

    public void setHighLightSpotRadius(int radius) {
        this.mHighlightSpotRadius = radius;
    }

    private int[] calculateFingerprintMargin() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("left = ");
        stringBuilder.append(this.mFingerprintPosition[0]);
        stringBuilder.append("right = ");
        stringBuilder.append(this.mFingerprintPosition[2]);
        Log.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("top = ");
        stringBuilder.append(this.mFingerprintPosition[1]);
        stringBuilder.append("button = ");
        stringBuilder.append(this.mFingerprintPosition[3]);
        Log.d(str, stringBuilder.toString());
        float dpiScale = getDPIScale();
        int[] margin = new int[2];
        int fingerPrintInScreenWidth = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472303)) * dpiScale) + 1056964608);
        int fingerPrintInScreenHeight = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472302)) * dpiScale) + 1056964608);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("fingerPrintInScreenWidth= ");
        stringBuilder2.append(fingerPrintInScreenWidth);
        stringBuilder2.append("fingerPrintInScreenHeight = ");
        stringBuilder2.append(fingerPrintInScreenHeight);
        Log.d(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("current height = ");
        stringBuilder2.append(this.mCurrentHeight);
        stringBuilder2.append("mDefaultDisplayHeight = ");
        stringBuilder2.append(this.mDefaultDisplayHeight);
        Log.d(str2, stringBuilder2.toString());
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        int marginLeft;
        if (this.mCurrentRotation == 0 || this.mCurrentRotation == 2) {
            this.mFingerprintCenterX = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            marginLeft = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
            int marginTop = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
            margin[0] = marginLeft;
            margin[1] = marginTop;
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("marginLeft = ");
            stringBuilder3.append(marginLeft);
            stringBuilder3.append("marginTop = ");
            stringBuilder3.append(marginTop);
            stringBuilder3.append("scale = ");
            stringBuilder3.append(scale);
            Log.d(str3, stringBuilder3.toString());
        } else {
            this.mFingerprintCenterX = (this.mFingerprintPosition[1] + this.mFingerprintPosition[3]) / 2;
            this.mFingerprintCenterY = (this.mFingerprintPosition[0] + this.mFingerprintPosition[2]) / 2;
            marginLeft = (int) (((((float) this.mFingerprintCenterY) * scale) - (((float) fingerPrintInScreenHeight) / 2.0f)) + 1056964608);
            margin[0] = (int) ((((((float) (this.mDefaultDisplayHeight - this.mFingerprintCenterX)) * scale) - (((float) this.mContext.getResources().getDimensionPixelSize(34472370)) * dpiScale)) - (((float) fingerPrintInScreenWidth) / 2.0f)) + 1056964608);
            margin[1] = marginLeft;
        }
        return margin;
    }

    private float getDPIScale() {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 640));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        int realdpi = SystemProperties.getInt("persist.sys.realdpi", dpi);
        float scale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initDPIView: lcdDpi: ");
        stringBuilder.append(lcdDpi);
        stringBuilder.append(" dpi: ");
        stringBuilder.append(dpi);
        stringBuilder.append(" realdpi: ");
        stringBuilder.append(realdpi);
        stringBuilder.append(" scale: ");
        stringBuilder.append(scale);
        Log.i(str, stringBuilder.toString());
        return scale;
    }

    private int[] calculateFingerprintLayoutLeftMargin(int width) {
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        float dpiScale = getDPIScale();
        int[] layoutMargin = new int[2];
        int leftmargin = 0;
        int rightmargin = 0;
        if (this.mCurrentRotation == 3) {
            leftmargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472370)) * dpiScale) + 0.5f);
            rightmargin = (int) (((((float) this.mFingerprintCenterX) * scale) - (((float) width) / 2.0f)) + 0.5f);
        } else if (this.mCurrentRotation == 1) {
            leftmargin = (int) (((((float) this.mFingerprintCenterX) * scale) - (((float) width) / 2.0f)) + 0.5f);
            rightmargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472370)) * dpiScale) + 0.5f);
        }
        layoutMargin[0] = leftmargin;
        layoutMargin[1] = rightmargin;
        return layoutMargin;
    }

    private int calculateRemoteViewLeftMargin(int fingerLayoutWidth) {
        float dpiScale = getDPIScale();
        if (this.mCurrentRotation == 3) {
            return (int) ((((((float) this.mContext.getResources().getDimensionPixelSize(34472380)) * dpiScale) + (((float) this.mContext.getResources().getDimensionPixelSize(34472370)) * dpiScale)) + ((float) fingerLayoutWidth)) + 0.5f);
        }
        if (this.mCurrentRotation == 1) {
            return (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472380)) * dpiScale) + 0.5f);
        }
        return 0;
    }

    private void calculateButtonPosition() {
        if (this.mPkgName != null && !this.mPkgName.equals(PKGNAME_OF_KEYGUARD)) {
            getCurrentRotation();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mCurrentRotation = ");
            stringBuilder.append(this.mCurrentRotation);
            Log.d(str, stringBuilder.toString());
            float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
            switch (this.mCurrentRotation) {
                case 0:
                case 2:
                    this.mButtonCenterX = (int) (((((float) (this.mFingerprintPosition[0] + this.mFingerprintPosition[2])) / 2.0f) * scale) + 0.5f);
                    this.mButtonCenterY = (this.mCurrentHeight - this.mContext.getResources().getDimensionPixelSize(34472255)) - (this.mContext.getResources().getDimensionPixelSize(34472416) / 2);
                    break;
                case 1:
                    this.mButtonCenterX = (int) (((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale) + 0.5f);
                    this.mButtonCenterY = (this.mCurrentWidth - this.mContext.getResources().getDimensionPixelSize(34472255)) - (this.mContext.getResources().getDimensionPixelSize(34472416) / 2);
                    break;
                case 3:
                    this.mButtonCenterX = this.mCurrentHeight - ((int) (((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale) + 0.5f));
                    this.mButtonCenterY = (this.mCurrentWidth - this.mContext.getResources().getDimensionPixelSize(34472255)) - (this.mContext.getResources().getDimensionPixelSize(34472416) / 2);
                    break;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mButtonCenterX = ");
            stringBuilder2.append(this.mButtonCenterX);
            stringBuilder2.append(",mButtonCenterY =");
            stringBuilder2.append(this.mButtonCenterY);
            Log.d(str2, stringBuilder2.toString());
        }
    }

    private boolean isScreenOn() {
        if (this.mPowerManager.isInteractive()) {
            getBrightness();
            if (this.mCurrentBrightness != 0) {
                return true;
            }
            Log.i(TAG, "brightness is not set");
            return false;
        }
        Log.i(TAG, "screen is not Interactive");
        return false;
    }

    private void getBrightness() {
        this.mBrightnessMode = 1;
        this.mCurrentBrightness = -1;
        try {
            this.mBrightnessMode = System.getIntForUser(this.mContentResolver, "screen_brightness_mode", -2);
            if (this.mBrightnessMode == 1) {
                this.mCurrentBrightness = System.getIntForUser(this.mContentResolver, "screen_auto_brightness", -2);
            } else {
                this.mCurrentBrightness = System.getIntForUser(this.mContentResolver, "screen_brightness", -2);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" currentbrightness is");
            stringBuilder.append(this.mCurrentBrightness);
            stringBuilder.append("currentbrightnessMode is ");
            stringBuilder.append(this.mBrightnessMode);
            Log.d(str, stringBuilder.toString());
        } catch (SettingNotFoundException e) {
            Log.d(TAG, "settings screen_brightness_mode or screen_auto_brightness or screen_brightness not found");
        }
    }

    public void setLightLevel(int level, int lightLevelTime) {
        try {
            this.pm.setBrightnessNoLimit(level, lightLevelTime);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLightLevel :");
            stringBuilder.append(level);
            stringBuilder.append(" time:");
            stringBuilder.append(lightLevelTime);
            Log.d(str, stringBuilder.toString());
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("foreground package is ");
                    stringBuilder.append(proc.processName);
                    Log.w(str, stringBuilder.toString());
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
        if (this.mScreenShot == null || (HwColorPicker.processBitmap(this.mScreenShot).getDomainColor() == COLOR_BLACK && !PKGNAME_OF_KEYGUARD.equals(this.mPkgName))) {
            this.mBLurBitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
            this.mBLurBitmap.eraseColor(-7829368);
            this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
            this.mFingerView.setBackgroundDrawable(this.mBlurDrawable);
            return;
        }
        this.mBLurBitmap = BlurUtils.blurMaskImage(this.mContext, this.mScreenShot, this.mScreenShot, 25);
        this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
        this.mFingerView.setBackgroundDrawable(this.mBlurDrawable);
    }

    private void getScreenShot() {
        try {
            this.mScreenShot = BlurUtils.screenShotBitmap(this.mContext, BLUR_SCALE);
            if (this.mScreenShot != null) {
                this.mScreenShot = this.mScreenShot.copy(Config.ARGB_8888, true);
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception in screenShotBitmap");
        } catch (Error err) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("screenShotBitmap  Error er = ");
            stringBuilder.append(err.getMessage());
            Log.d(str, stringBuilder.toString());
        }
    }

    private void registerSingerHandObserver() {
        if (this.mSingleContentObserver == null) {
            this.mSingleContentObserver = new SingleModeContentObserver(new Handler(), new SingleModeContentCallback(this, null));
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRemainingNum, mRemainTimes = ");
        stringBuilder.append(this.mRemainTimes);
        Log.d(str, stringBuilder.toString());
        if (this.mRemainTimes > 0 && this.mMyCountDown != null) {
            this.mMyCountDown.cancel();
            this.mIsFingerFrozen = false;
        }
    }

    public void notifyFingerprintViewCoverd(boolean covered, Rect winFrame) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyWinCovered covered=");
        stringBuilder.append(covered);
        stringBuilder.append("winFrame = ");
        stringBuilder.append(winFrame);
        Log.v(str, stringBuilder.toString());
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

    public void notifyTouchUp(float x, float y) {
        if (this.highLightViewAdded) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UD Fingerprint notifyTouchUp point (");
            stringBuilder.append(x);
            stringBuilder.append(" , ");
            stringBuilder.append(y);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
            if (isFingerViewTouched(x, y)) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        if (FingerViewController.this.highLightViewAdded && FingerViewController.this.mHighLightView != null) {
                            Log.i(FingerViewController.TAG, "notifyTouchUp setCircleVisibility INVISIBLE");
                            FingerViewController.this.mHighLightView.setCircleVisibility(4);
                        }
                    }
                });
            } else {
                Log.i(TAG, "notifyTouchUp,point not in fingerprint view");
            }
        }
    }

    private Rect getFingerprintViewRect() {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 640));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        float scale = (((float) SystemProperties.getInt("persist.sys.realdpi", dpi)) * 1.0f) / ((float) dpi);
        float lcdscale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        Rect fingerprintViewRect = new Rect();
        fingerprintViewRect.left = (int) (((double) ((((float) (this.mFingerprintPosition[0] + this.mFingerprintPosition[2])) / 2.0f) * scale)) - ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472303)) * lcdscale)) * 0.5d) + 0.5d));
        fingerprintViewRect.top = (int) (((double) ((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale)) - ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472302)) * lcdscale)) * 0.5d) + 0.5d));
        fingerprintViewRect.right = (int) (((double) ((((float) (this.mFingerprintPosition[0] + this.mFingerprintPosition[2])) / 2.0f) * scale)) + ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472303)) * lcdscale)) * 0.5d) + 0.5d));
        fingerprintViewRect.bottom = (int) (((double) ((((float) (this.mFingerprintPosition[1] + this.mFingerprintPosition[3])) / 2.0f) * scale)) + ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472302)) * lcdscale)) * 0.5d) + 0.5d));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getFingerprintViewRect, fingerprintViewRect = ");
        stringBuilder.append(fingerprintViewRect);
        Log.i(str, stringBuilder.toString());
        return fingerprintViewRect;
    }

    private boolean isFingerprintViewCoverd(Rect fingerprintViewRect, Rect winFrame) {
        return fingerprintViewRect.right > winFrame.left && winFrame.right > fingerprintViewRect.left && fingerprintViewRect.bottom > winFrame.top && winFrame.bottom > fingerprintViewRect.top;
    }

    private boolean isFingerViewTouched(float x, float y) {
        Rect fingerprintViewRect = getFingerprintViewRect();
        return ((float) fingerprintViewRect.left) < x && ((float) fingerprintViewRect.right) > x && ((float) fingerprintViewRect.bottom) > y && ((float) fingerprintViewRect.top) < y;
    }

    private boolean getBrightnessRangeFromPanelInfo() {
        File file = new File(PANEL_INFO_NODE);
        if (file.exists()) {
            BufferedReader reader = null;
            FileInputStream fis = null;
            String readLine;
            StringBuilder stringBuilder;
            try {
                fis = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                readLine = reader.readLine();
                String tempString = readLine;
                if (readLine != null) {
                    readLine = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getBrightnessRangeFromPanelInfo String = ");
                    stringBuilder.append(tempString);
                    Log.i(readLine, stringBuilder.toString());
                    if (tempString.length() == 0) {
                        Log.e(TAG, "getBrightnessRangeFromPanelInfo error! String is null");
                        reader.close();
                        close(reader, fis);
                        return false;
                    }
                    String[] stringSplited = tempString.split(",");
                    if (stringSplited.length < 2) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("split failed! String = ");
                        stringBuilder2.append(tempString);
                        Log.e(str, stringBuilder2.toString());
                        reader.close();
                        close(reader, fis);
                        return false;
                    } else if (parsePanelInfo(stringSplited)) {
                        reader.close();
                        close(reader, fis);
                        return true;
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "getBrightnessRangeFromPanelInfo error! FileNotFoundException");
            } catch (IOException e2) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getBrightnessRangeFromPanelInfo error! IOException ");
                stringBuilder.append(e2);
                Log.e(readLine, stringBuilder.toString());
            } catch (Exception e3) {
                readLine = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getBrightnessRangeFromPanelInfo error! Exception ");
                stringBuilder.append(e3);
                Log.e(readLine, stringBuilder.toString());
            } catch (Throwable th) {
                close(reader, fis);
            }
            close(reader, fis);
            return false;
        }
        String str2 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("getBrightnessRangeFromPanelInfo PANEL_INFO_NODE:");
        stringBuilder3.append(PANEL_INFO_NODE);
        stringBuilder3.append(" isn't exist");
        Log.w(str2, stringBuilder3.toString());
        return false;
    }

    private void close(BufferedReader reader, FileInputStream fis) {
        if (reader != null || fis != null) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private boolean parsePanelInfo(String[] stringSplited) {
        if (stringSplited == null) {
            return false;
        }
        String key = null;
        int index = -1;
        int min = -1;
        int max = -1;
        int i = 0;
        while (i < stringSplited.length) {
            try {
                key = "blmax:";
                index = stringSplited[i].indexOf(key);
                if (index != -1) {
                    max = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                } else {
                    key = "blmin:";
                    index = stringSplited[i].indexOf(key);
                    if (index != -1) {
                        min = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                    }
                }
                i++;
            } catch (NumberFormatException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parsePanelInfo() error! ");
                stringBuilder.append(key);
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        if (max == -1 || min == -1) {
            return false;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getBrightnessRangeFromPanelInfo success! min = ");
        stringBuilder2.append(min);
        stringBuilder2.append(", max = ");
        stringBuilder2.append(max);
        Log.i(str2, stringBuilder2.toString());
        this.mNormalizedMaxBrightness = max;
        this.mNormalizedMinBrightness = min;
        return true;
    }
}
