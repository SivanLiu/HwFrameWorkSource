package huawei.com.android.server.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.biometrics.IBiometricServiceReceiver;
import android.hardware.biometrics.IBiometricServiceReceiverInternal;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HwExHandler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.hidata.wavemapping.cons.WMStateCons;
import com.android.server.multiwin.HwMultiWinConstants;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.systemui.shared.system.MetricsLoggerCompat;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.os.HwPowerManager;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.displayengine.IDisplayEngineServiceEx;
import com.huawei.server.fingerprint.FingerprintAnimByThemeModel;
import com.huawei.server.fingerprint.FingerprintAnimByThemeView;
import com.huawei.server.fingerprint.FingerprintCircleOverlay;
import com.huawei.server.fingerprint.FingerprintMaskOverlay;
import com.huawei.server.fingerprint.FingerprintViewUtils;
import com.huawei.server.fingerprint.HighLightMaskView;
import com.huawei.server.fingerprint.SuspensionButton;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.android.aod.HwAodManager;
import huawei.android.hardware.fingerprint.FingerprintManagerEx;
import huawei.android.hwcolorpicker.HwColorPicker;
import huawei.com.android.server.fingerprint.FingerprintView;
import huawei.com.android.server.fingerprint.SingleModeContentObserver;
import huawei.com.android.server.fingerprint.fingerprintAnimation.BreathImageDrawable;
import huawei.com.android.server.policy.BlurUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FingerViewController {
    private static final int ALPHA_ANIMATION_THRESHOLD = 15;
    private static final float ALPHA_LIMITED = 0.863f;
    private static final String APS_INIT_HEIGHT = "aps_init_height";
    private static final String APS_INIT_WIDTH = "aps_init_width";
    private static final int BLUR_RADIO = 25;
    private static final float BLUR_SCALE = 0.125f;
    private static final int BRIGHTNESS_ENTER_TIME = 150;
    private static final int BRIGHTNESS_EXIT_TIME = 110;
    private static final int BRIGHTNESS_LIFT_TIME_LONG = 200;
    private static final int BRIGHTNESS_LIFT_TIME_SHORT = 16;
    private static final int CIRCLE_LAYER = 2147483645;
    private static final int COLOR_BLACK = -16250872;
    private static final boolean DEBUG = true;
    private static int DEFAULT_BRIGHTNESS = 56;
    private static final int DEFAULT_INIT_HEIGHT = 2880;
    private static final int DEFAULT_INIT_WIDTH = 1440;
    private static int DEFAULT_LCD_DPI = 640;
    private static final int DESTORY_ANIM_DELAY_TIME = 10000;
    public static final int DISMISSED_REASON_NEGATIVE = 2;
    public static final int DISMISSED_REASON_POSITIVE = 1;
    private static final String DISPLAY_NOTCH_STATUS = "display_notch_status";
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
    private static final int FINGERPRINT_IN_DISPLAY_POSITION_VIEW_VALUE_ELLE = 2101;
    private static final int FINGERPRINT_IN_DISPLAY_POSITION_VIEW_VALUE_VOGUE = 2133;
    public static final int FINGERPRINT_USE_PASSWORD_ERROR_CODE = 10;
    private static final int FINGER_BUTTON_SIZE_HALF = 2;
    private static final float FINGER_BUTTON_SIZE_TRANS_PARAMETER = 0.5f;
    private static final String FINGRPRINT_VIEW_TITLE_NAME = "hw_ud_fingerprint_mask_hwsinglemode_window";
    private static final int FLAG_FOR_HBM_SYNC = 500;
    public static final int HBM_TYPE_AHEAD = 1;
    public static final int HBM_TYPE_FINGERDOWN = 0;
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE = 1;
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE_CANCELED = 4;
    public static final int HIGHLIGHT_TYPE_AUTHENTICATE_SUCCESS = 2;
    public static final int HIGHLIGHT_TYPE_ENROLL = 0;
    public static final int HIGHLIGHT_TYPE_SCREENON = 5;
    public static final int HIGHLIGHT_TYPE_UNDEFINED = -1;
    public static final int HIGHLIGHT_VIEW_REMOVE_TIME = 3;
    private static final String HIGHLIGHT_VIEW_TITLE_NAME = "fingerprint_alpha_layer";
    private static final int INITIAL_BRIGHTNESS = -1;
    private static int INVALID_BRIGHTNESS = -1;
    private static final int INVALID_COLOR = 0;
    private static final float LV_FINGERPRINT_POSITION_VIEW_HIGHT_SCALE = 0.78f;
    private static final int MASK_LAYER = 2147483644;
    private static final float MAX_BRIGHTNESS_LEVEL = 255.0f;
    private static final int MAX_CAPTURE_TIME = 1000;
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED = 5;
    private static final float MIN_ALPHA = 0.004f;
    private static final int NOTCH_CORNER_STATUS_HIDE = 0;
    private static final int NOTCH_CORNER_STATUS_SHOW = 1;
    private static final int NOTCH_ROUND_CORNER_CODE = 8002;
    private static final int NOTCH_STATUS_DEFAULT = 0;
    private static final int NOTCH_STATUS_HIDE = 1;
    private static final String[] PACKAGES_USE_CANCEL_HOTSPOT_INTERFACE = {PKGNAME_OF_WALLET};
    private static final String[] PACKAGES_USE_HWAUTH_INTERFACE = {"com.huawei.hwid", PKGNAME_OF_WALLET, "com.huawei.android.hwpay"};
    private static String PANEL_INFO_NODE = "/sys/class/graphics/fb0/panel_info";
    public static final String PKGNAME_OF_KEYGUARD = "com.android.systemui";
    private static final String PKGNAME_OF_SECURITYMGR = "com.huawei.securitymgr";
    private static final String PKGNAME_OF_SETTINGS = "com.android.settings";
    private static final String PKGNAME_OF_SYSTEMMANAGER = "com.huawei.systemmanager";
    private static final String PKGNAME_OF_WALLET = "com.huawei.wallet";
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_CHECKING = 3;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_RESULT_FAIL = 1;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_RESULT_SUCCESS = 0;
    private static final int RESULT_FINGERPRINT_AUTHENTICATION_UNCHECKED = 2;
    private static final long SET_SCENE_DELAY_TIME = 80;
    private static final int SLIDE_HEIGHT_INDEXES = 1;
    private static final int SLIDE_PARAMETER_NUM = 4;
    private static final String SLIDE_PROP = SystemProperties.get("ro.config.hw_curved_side_disp", "");
    private static final String TAG = "FingerViewControllerTag";
    private static final String TAG_THREAD = "FingerViewControllerThread";
    public static final int TIME_UNIT = 1000;
    public static final int TYPE_DISMISS_ALL = 0;
    public static final int TYPE_FINGERPRINT_BUTTON = 2;
    public static final int TYPE_FINGERPRINT_VIEW = 1;
    public static final int TYPE_FINGER_VIEW = 2105;
    private static int UD_BUFFSIZE = 12;
    private static final String UIDNAME_OF_KEYGUARD = "android.uid.systemui";
    private static final byte VIEW_STATE_DISABLED = 2;
    private static final byte VIEW_STATE_HIDE = 0;
    private static final byte VIEW_STATE_SHOW = 1;
    private static final byte VIEW_TYPE_BUTTON = 1;
    private static final byte VIEW_TYPE_FINGER_IMAGE = 3;
    private static final byte VIEW_TYPE_FINGER_MASK = 0;
    private static final byte VIEW_TYPE_HIGHLIGHT = 2;
    public static final int WAITTING_TIME = 30;
    private static float mFingerPositionHeightScale = 0.0f;
    private static int[] sDefaultSampleAlpha = {234, 229, HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_OPEN, HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE, HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED, 211, HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED, 205, 187, HwMultiWinConstants.DRAGGING_FLOAT_WIN_WIDTH_DP, HwSecDiagnoseConstant.OEMINFO_ID_DEVICE_RENEW, 163, CPUFeature.MSG_AUX_RTG_SCHED, CPUFeature.MSG_SET_BG_UIDS, WMStateCons.MSG_CHECK_4G_COVERAGE, WMStateCons.MSG_CHECK_4G_COVERAGE, 125, 121, 111, 101, 92, 81, 81, 69, 68, 58, 56, 46, 44, 35, 34, 30, 22, 23, 18, 0, 0};
    private static int[] sDefaultSampleBrightness = {4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 30, 34, 40, 46, 50, 56, 64, 74, 84, 94, 104, HwArbitrationDEFS.MSG_INSTANT_TRAVEL_APP_END, CPUFeature.MSG_SET_CPUCONFIG, 134, CPUFeature.MSG_RESET_VIP_THREAD, CPUFeature.MSG_RESET_ON_FIRE, 164, 174, CPUFeature.MSG_HIGH_LOAD_THREAD, 194, 204, HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED, MetricsLoggerCompat.OVERVIEW_ACTIVITY, 234, 244, 248, 255};
    private static FingerViewController sInstance;
    private RelativeLayout.LayoutParams appParamsCancelView;
    private RelativeLayout.LayoutParams appParamsUsePassworView;
    private int cancelButtonMarginEnd;
    private int cancelButtonMarginStart;
    /* access modifiers changed from: private */
    public boolean highLightViewAdded = false;
    private boolean isBiometricRequireConfirmation;
    private final Runnable mAddBackFingprintRunnable;
    private final Runnable mAddButtonViewRunnable;
    /* access modifiers changed from: private */
    public final Runnable mAddFingerViewRunnable;
    private final Runnable mAddImageOnlyRunnable;
    /* access modifiers changed from: private */
    public BreathImageDrawable mAlipayDrawable;
    /* access modifiers changed from: private */
    public List<String> mAnimFileNames = new ArrayList(30);
    private HwAodManager mAodManager;
    private int mAuthenticateResult;
    private Bitmap mBLurBitmap;
    private Button mBackFingerprintCancelView;
    private TextView mBackFingerprintHintView;
    private Button mBackFingerprintUsePasswordView;
    /* access modifiers changed from: private */
    public BackFingerprintView mBackFingerprintView;
    /* access modifiers changed from: private */
    public IBiometricServiceReceiverInternal mBiometricServiceReceiver;
    private BitmapDrawable mBlurDrawable;
    private int mButtonCenterX = -1;
    private int mButtonCenterY = -1;
    private int mButtonColor = 0;
    /* access modifiers changed from: private */
    public SuspensionButton mButtonView;
    private boolean mButtonViewAdded = false;
    private int mButtonViewState = 1;
    private RelativeLayout mCancelView;
    private RelativeLayout mCancelViewImageOnly;
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentAlpha = 0;
    /* access modifiers changed from: private */
    public int mCurrentBrightness = -1;
    /* access modifiers changed from: private */
    public int mCurrentHeight;
    private int mCurrentRotation;
    /* access modifiers changed from: private */
    public int mCurrentWidth;
    /* access modifiers changed from: private */
    public int mDefaultDisplayHeight;
    /* access modifiers changed from: private */
    public int mDefaultDisplayWidth;
    private final Runnable mDestroyAnimViewRunnable;
    /* access modifiers changed from: private */
    public IBiometricServiceReceiver mDialogReceiver;
    /* access modifiers changed from: private */
    public DisplayEngineManager mDisplayEngineManager;
    private int mDisplayNotchStatus = 0;
    private int mEnrollDigitalBrigtness;
    private Handler mFingerHandler;
    private int mFingerLogoRadius;
    /* access modifiers changed from: private */
    public FingerprintView mFingerView;
    /* access modifiers changed from: private */
    public ICallBack mFingerViewChangeCallback;
    private WindowManager.LayoutParams mFingerViewParams;
    /* access modifiers changed from: private */
    public FingerprintAnimByThemeView mFingerprintAnimByThemeView;
    /* access modifiers changed from: private */
    public FingerprintAnimationView mFingerprintAnimationView;
    /* access modifiers changed from: private */
    public int mFingerprintCenterX = -1;
    /* access modifiers changed from: private */
    public int mFingerprintCenterY = -1;
    /* access modifiers changed from: private */
    public FingerprintCircleOverlay mFingerprintCircleOverlay;
    private ImageView mFingerprintImageForAlipay;
    private FingerprintManagerEx mFingerprintManagerEx;
    /* access modifiers changed from: private */
    public FingerprintMaskOverlay mFingerprintMaskOverlay;
    private final Runnable mFingerprintMaskSetAlpha;
    private boolean mFingerprintOnlyViewAdded = false;
    /* access modifiers changed from: private */
    public int[] mFingerprintPosition = new int[4];
    /* access modifiers changed from: private */
    public ImageView mFingerprintView;
    private boolean mFingerprintViewAdded = false;
    private float mFontScale;
    /* access modifiers changed from: private */
    public Handler mHandler = null;
    protected final HandlerThread mHandlerThread = new HandlerThread("FingerViewController");
    /* access modifiers changed from: private */
    public boolean mHasBackFingerprint = false;
    private boolean mHasUdFingerprint = true;
    private int mHighLightRemoveType;
    /* access modifiers changed from: private */
    public int mHighLightShowType;
    /* access modifiers changed from: private */
    public HighLightMaskView mHighLightView;
    private final Runnable mHighLightViewRunnable;
    private int mHighlightBrightnessLevel;
    private int mHighlightSpotColor;
    private int mHighlightSpotRadius;
    /* access modifiers changed from: private */
    public String mHint;
    private HintText mHintView;
    private boolean mIsBiometricPrompt;
    private boolean mIsCancelHotSpotPkgAdded;
    /* access modifiers changed from: private */
    public boolean mIsFingerFrozen = false;
    private boolean mIsKeygaurdCoverd;
    /* access modifiers changed from: private */
    public boolean mIsNeedReload;
    private boolean mIsSingleModeObserverRegistered = false;
    private boolean mKeepMaskAfterAuthentication = false;
    private KeyguardManager mKeyguardManager;
    private Button mLVBackFingerprintCancelView;
    private Button mLVBackFingerprintUsePasswordView;
    /* access modifiers changed from: private */
    public RelativeLayout mLayoutForAlipay;
    private LayoutInflater mLayoutInflater;
    private final Runnable mLoadFingerprintAnimViewRunnable;
    /* access modifiers changed from: private */
    public float mMaxDigitalBrigtness;
    private RemainTimeCountDown mMyCountDown = null;
    private int mNormalizedMaxBrightness;
    private int mNormalizedMinBrightness;
    private boolean mNotchConerStatusChanged = false;
    private int mNotchHeight;
    private Bundle mPkgAttributes;
    /* access modifiers changed from: private */
    public String mPkgName;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public IFingerprintServiceReceiver mReceiver;
    private int mRemainTimes = 5;
    /* access modifiers changed from: private */
    public int mRemainedSecs;
    private RelativeLayout mRemoteView;
    /* access modifiers changed from: private */
    public final Runnable mRemoveBackFingprintRunnable;
    /* access modifiers changed from: private */
    public final Runnable mRemoveButtonViewRunnable;
    /* access modifiers changed from: private */
    public final Runnable mRemoveFingerHighLightView;
    /* access modifiers changed from: private */
    public final Runnable mRemoveFingerViewRunnable;
    /* access modifiers changed from: private */
    public final Runnable mRemoveHighLightView;
    /* access modifiers changed from: private */
    public final Runnable mRemoveHighlightCircleRunnable;
    private final Runnable mRemoveImageOnlyRunnable;
    private int[] mSampleAlpha = null;
    private int[] mSampleBrightness = null;
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
    /* access modifiers changed from: private */
    public final Runnable mSetEnrollLightLevelRunnable;
    private final Runnable mSetOutProtectEye;
    private final Runnable mSetProtectEye;
    private final Runnable mSetScene;
    private final Runnable mShowHighlightCircleRunnable;
    private SingleModeContentObserver mSingleContentObserver;
    /* access modifiers changed from: private */
    public String mSubTitle;
    /* access modifiers changed from: private */
    public WindowManager.LayoutParams mSuspensionButtonParams;
    /* access modifiers changed from: private */
    public final Runnable mUpdateButtonViewRunnable;
    private final Runnable mUpdateFingerprintViewRunnable;
    private final Runnable mUpdateFingprintRunnable;
    private final Runnable mUpdateImageOnlyRunnable;
    private final Runnable mUpdateMaskAttibuteRunnable;
    private boolean mUseDefaultHint = true;
    private int mWidgetColor;
    /* access modifiers changed from: private */
    public boolean mWidgetColorSet = false;
    /* access modifiers changed from: private */
    public final WindowManager mWindowManager;
    private int mWindowType;
    private IPowerManager pm;
    private ContentObserver settingsDisplayObserver;

    public interface ICallBack {
        void onFingerViewStateChange(int i);

        void onNotifyBlueSpotDismiss();

        void onNotifyCaptureImage();
    }

    private FingerViewController(Context context) {
        int i = INVALID_BRIGHTNESS;
        this.mNormalizedMinBrightness = i;
        this.mNormalizedMaxBrightness = i;
        this.mIsCancelHotSpotPkgAdded = false;
        this.cancelButtonMarginStart = 0;
        this.cancelButtonMarginEnd = 0;
        this.settingsDisplayObserver = new ContentObserver(this.mHandler) {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                Log.d(FingerViewController.TAG, "settingsDisplayObserver onChange");
                FingerViewController.this.mHandler.postDelayed(new Runnable() {
                    /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass1.AnonymousClass1 */

                    public void run() {
                        int unused = FingerViewController.this.mDefaultDisplayHeight = Settings.Global.getInt(FingerViewController.this.mContentResolver, FingerViewController.APS_INIT_HEIGHT, FingerViewController.DEFAULT_INIT_HEIGHT);
                        int unused2 = FingerViewController.this.mDefaultDisplayWidth = Settings.Global.getInt(FingerViewController.this.mContentResolver, FingerViewController.APS_INIT_WIDTH, 1440);
                        int currentHeight = SystemPropertiesEx.getInt("persist.sys.rog.height", FingerViewController.this.mDefaultDisplayHeight);
                        int currentWidth = SystemPropertiesEx.getInt("persist.sys.rog.width", FingerViewController.this.mDefaultDisplayWidth);
                        if (FingerViewController.this.mCurrentHeight == currentHeight && FingerViewController.this.mCurrentWidth == currentWidth) {
                            boolean unused3 = FingerViewController.this.mIsNeedReload = true;
                        } else {
                            int unused4 = FingerViewController.this.mCurrentHeight = currentHeight;
                            int unused5 = FingerViewController.this.mCurrentWidth = currentWidth;
                            boolean unused6 = FingerViewController.this.mIsNeedReload = false;
                        }
                        Log.i(FingerViewController.TAG_THREAD, "settingsDisplayObserver onChange:" + FingerViewController.this.mDefaultDisplayHeight + "," + FingerViewController.this.mDefaultDisplayWidth + "," + FingerViewController.this.mCurrentHeight + "," + FingerViewController.this.mCurrentWidth + "," + FingerViewController.this.mIsNeedReload);
                    }
                }, 30);
            }
        };
        this.mUpdateMaskAttibuteRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass2 */

            public void run() {
                Log.d(FingerViewController.TAG_THREAD, "mUpdateMaskAttibuteRunnable mHint:" + FingerViewController.this.mHint);
                FingerViewController fingerViewController = FingerViewController.this;
                fingerViewController.updateHintView(fingerViewController.mHint);
                if (FingerViewController.this.mFingerprintView != null) {
                    FingerViewController.this.mFingerprintView.setContentDescription(FingerViewController.this.mHint);
                }
            }
        };
        this.mFingerprintMaskSetAlpha = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass3 */

            public void run() {
                if (FingerViewController.this.mFingerprintMaskOverlay.isVisible()) {
                    Log.d(FingerViewController.TAG, "fingerprintMaskSetAlpha");
                    SurfaceControl.openTransaction();
                    try {
                        FingerViewController.this.mFingerprintMaskOverlay.setAlpha(((float) FingerViewController.this.mCurrentAlpha) / FingerViewController.MAX_BRIGHTNESS_LEVEL);
                    } finally {
                        SurfaceControl.closeTransaction();
                    }
                }
            }
        };
        this.mAddFingerViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass10 */

            public void run() {
                FingerViewController.this.createFingerprintView();
                Log.d(FingerViewController.TAG, "begin mAddFingerViewRunnable");
            }
        };
        this.mRemoveFingerViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass11 */

            public void run() {
                Log.i(FingerViewController.TAG, "begin mRemoveFingerViewRunnable");
                FingerViewController.this.removeFingerprintView();
            }
        };
        this.mUpdateFingerprintViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass12 */

            public void run() {
                Log.i(FingerViewController.TAG, "begin mUpdateFingerprintViewRunnable");
                FingerViewController.this.updateFingerprintView();
            }
        };
        this.mAddImageOnlyRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass17 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mAddImageOnlyRunnable");
                if (FingerViewController.this.isNewMagazineViewForDownFP()) {
                    FingerViewController.this.setFPAuthState(true);
                }
                FingerViewController fingerViewController = FingerViewController.this;
                int unused = fingerViewController.mDefaultDisplayHeight = Settings.Global.getInt(fingerViewController.mContext.getContentResolver(), FingerViewController.APS_INIT_HEIGHT, FingerViewController.DEFAULT_INIT_HEIGHT);
                FingerViewController fingerViewController2 = FingerViewController.this;
                int unused2 = fingerViewController2.mDefaultDisplayWidth = Settings.Global.getInt(fingerViewController2.mContext.getContentResolver(), FingerViewController.APS_INIT_WIDTH, 1440);
                FingerViewController.this.createImageOnlyView();
            }
        };
        this.mUpdateImageOnlyRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass18 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mUpdateImageOnlyRunnable");
                FingerViewController.this.updateImageOnlyView();
            }
        };
        this.mDestroyAnimViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass19 */

            public void run() {
                Log.i(FingerViewController.TAG, "come in  mDestroyAnimViewRunnable");
                if (FingerViewController.this.mFingerprintAnimByThemeView != null) {
                    FingerViewController.this.removeAnimViewIfAttached();
                    FingerViewController.this.mFingerprintAnimByThemeView.destroy();
                    FingerprintAnimByThemeView unused = FingerViewController.this.mFingerprintAnimByThemeView = null;
                    FingerprintAnimByThemeModel.setDestoryAmount();
                }
            }
        };
        this.mLoadFingerprintAnimViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass20 */

            public void run() {
                Log.d(FingerViewController.TAG, "come in  mLoadFingerprintAnimViewRunnable");
                if (FingerprintAnimByThemeModel.isGetThemeError(FingerViewController.this.mContext)) {
                    FingerViewController.this.mAnimFileNames.clear();
                } else if (FingerprintAnimByThemeModel.isThemChanged(FingerViewController.this.mFingerprintAnimByThemeView, FingerViewController.this.getPxScale()) || FingerViewController.this.mFingerprintAnimByThemeView == null || FingerViewController.this.mFingerprintAnimByThemeView.isLanguageChange()) {
                    FingerViewController.this.removeAnimViewIfAttached();
                    List<String> fpLoadFileNames = FingerprintAnimByThemeModel.preloadFilesFromPath(FingerViewController.this.mContext);
                    FingerViewController.this.mAnimFileNames.clear();
                    FingerprintAnimByThemeView unused = FingerViewController.this.mFingerprintAnimByThemeView = null;
                    if (fpLoadFileNames.isEmpty()) {
                        Log.i(FingerViewController.TAG, "the  mAnimFileNames is empty, we use default theme");
                        return;
                    }
                    FingerViewController.this.mAnimFileNames.addAll(fpLoadFileNames);
                    FingerViewController.this.initFingerprintAnimView();
                } else {
                    Log.i(FingerViewController.TAG, "come in isLanguageChange");
                }
            }
        };
        this.mRemoveImageOnlyRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass21 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mRemoveImageOnlyRunnable");
                FingerViewController.this.setFPAuthState(false);
                FingerViewController.this.removeImageOnlyView();
            }
        };
        this.mAddButtonViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass24 */

            public void run() {
                Log.i(FingerViewController.TAG, "begin mAddButtonViewRunnable");
                FingerViewController.this.createAndAddButtonView();
            }
        };
        this.mRemoveButtonViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass25 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mRemoveButtonViewRunnable");
                FingerViewController.this.removeButtonView();
            }
        };
        this.mUpdateButtonViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass26 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mUpdateButtonViewRunnable");
                FingerViewController.this.updateButtonView();
            }
        };
        this.mRemoveFingerHighLightView = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass31 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mRemoveFingerHighLightView");
                if (FingerViewController.this.mHandler.hasCallbacks(FingerViewController.this.mSetEnrollLightLevelRunnable)) {
                    FingerViewController.this.mHandler.removeCallbacks(FingerViewController.this.mSetEnrollLightLevelRunnable);
                    Log.i(FingerViewController.TAG_THREAD, "remove mSetEnrollLightLevelRunnable");
                }
                if (FingerViewController.this.mDisplayEngineManager != null) {
                    FingerViewController.this.mDisplayEngineManager.setScene(34, 0);
                    Log.i(FingerViewController.TAG_THREAD, "mRemoveFingerHighLightView exit hbm");
                }
            }
        };
        this.mRemoveHighLightView = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass32 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mRemoveHighLightView");
                FingerViewController.this.removeHighLightViewInner();
            }
        };
        this.mHighLightViewRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass33 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mHighLightViewRunnable type" + FingerViewController.this.mHighLightShowType);
                int access$6000 = FingerViewController.this.mHighLightShowType;
                if (access$6000 == 0) {
                    FingerViewController.this.createAndAddHighLightView();
                    FingerViewController.this.mHandler.postDelayed(FingerViewController.this.mSetEnrollLightLevelRunnable, 150);
                    if (FingerViewController.this.mHighLightView != null) {
                        FingerViewController fingerViewController = FingerViewController.this;
                        int endAlpha = fingerViewController.getMaskAlpha(fingerViewController.mCurrentBrightness);
                        FingerViewController fingerViewController2 = FingerViewController.this;
                        fingerViewController2.startAlphaValueAnimation(fingerViewController2.mHighLightView, true, 0.0f, ((float) endAlpha) / FingerViewController.MAX_BRIGHTNESS_LEVEL, 150, 0);
                    }
                } else if (access$6000 == 1) {
                    FingerViewController.this.createAndAddHighLightView();
                } else if (access$6000 == 5) {
                    FingerViewController.this.createMaskAndCircleOnKeyguard();
                }
            }
        };
        this.mSetEnrollLightLevelRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass34 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mSetEnrollLightLevelRunnable");
                if (FingerViewController.this.mDisplayEngineManager != null) {
                    int digitalBrigtness = FingerViewController.this.getEnrollDigitalBrigtness();
                    FingerViewController.this.mDisplayEngineManager.setScene(34, digitalBrigtness);
                    Log.d(FingerViewController.TAG_THREAD, "mDisplayEngineManager enter hbm level digitalBrigtness:" + digitalBrigtness);
                }
            }
        };
        this.mRemoveHighlightCircleRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass35 */

            public void run() {
                Log.d(FingerViewController.TAG_THREAD, "RemoveHighlightCircle mHighLightShowType = " + FingerViewController.this.mHighLightShowType);
                if (FingerViewController.this.mFingerprintCircleOverlay != null && FingerViewController.this.mFingerprintCircleOverlay.isVisible()) {
                    SurfaceControl.openTransaction();
                    try {
                        FingerViewController.this.mFingerprintCircleOverlay.hide();
                    } finally {
                        SurfaceControl.closeTransaction();
                    }
                } else if (FingerViewController.this.highLightViewAdded && FingerViewController.this.mHighLightView != null && FingerViewController.this.mHighLightView.isAttachedToWindow()) {
                    FingerViewController.this.mHighLightView.setCircleVisibility(4);
                }
            }
        };
        this.mShowHighlightCircleRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass36 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mShowHighlightCircleRunnable, mHighLightShowType = " + FingerViewController.this.mHighLightShowType + ",highLightViewAdded = " + FingerViewController.this.highLightViewAdded);
                if (FingerViewController.this.mHighLightShowType == 0 && FingerViewController.this.highLightViewAdded && FingerViewController.this.mHighLightView.getCircleVisibility() == 4) {
                    FingerViewController.this.mHandler.removeCallbacks(FingerViewController.this.mRemoveHighlightCircleRunnable);
                    FingerViewController.this.getCurrentFingerprintCenter();
                    FingerViewController.this.mHighLightView.setCenterPoints(FingerViewController.this.mFingerprintCenterX, FingerViewController.this.mFingerprintCenterY);
                    FingerViewController.this.mHighLightView.setCircleVisibility(0);
                    FingerViewController.this.mHandler.postDelayed(FingerViewController.this.mRemoveHighlightCircleRunnable, 1200);
                }
            }
        };
        this.mSetProtectEye = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass37 */

            public void run() {
                Log.i(FingerViewController.TAG, "begin mSetProtectEye");
                if (FingerViewController.this.mDisplayEngineManager != null) {
                    FingerViewController.this.mDisplayEngineManager.setScene(31, 16);
                    Log.w(FingerViewController.TAG, "mDisplayEngineManager set ProtectEye");
                }
            }
        };
        this.mSetOutProtectEye = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass38 */

            public void run() {
                Log.i(FingerViewController.TAG, "begin mSetOutProtectEye");
                if (FingerViewController.this.mDisplayEngineManager != null) {
                    FingerViewController.this.mDisplayEngineManager.setScene(31, 17);
                    Log.w(FingerViewController.TAG, "mDisplayEngineManager set OutProtectEye");
                }
            }
        };
        this.mSetScene = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass39 */

            public void run() {
                Log.i(FingerViewController.TAG, "begin mSetScene");
                if (FingerViewController.this.mDisplayEngineManager != null) {
                    FingerViewController.this.mDisplayEngineManager.setScene(29, 0);
                    Log.d(FingerViewController.TAG, "mDisplayEngineManager set scene 0");
                }
            }
        };
        this.mAddBackFingprintRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass40 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mAddBackFingprintRunnable");
                FingerViewController fingerViewController = FingerViewController.this;
                int unused = fingerViewController.mDefaultDisplayHeight = Settings.Global.getInt(fingerViewController.mContext.getContentResolver(), FingerViewController.APS_INIT_HEIGHT, FingerViewController.DEFAULT_INIT_HEIGHT);
                FingerViewController fingerViewController2 = FingerViewController.this;
                int unused2 = fingerViewController2.mDefaultDisplayWidth = Settings.Global.getInt(fingerViewController2.mContext.getContentResolver(), FingerViewController.APS_INIT_WIDTH, 1440);
                FingerViewController.this.createBackFingprintView();
            }
        };
        this.mRemoveBackFingprintRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass41 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mRemoveBackFingprintRunnable");
                FingerViewController.this.removeBackFingprintView();
            }
        };
        this.mUpdateFingprintRunnable = new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass42 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "begin mUpdateFingprintRunnable");
                FingerViewController.this.updateBackFingprintView();
            }
        };
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mHandlerThread.start();
        this.mHandler = new HwExHandler(this.mHandlerThread.getLooper(), 500);
        this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mFingerprintManagerEx = new FingerprintManagerEx(this.mContext);
        this.mAodManager = HwAodManager.getInstance();
        this.mDisplayEngineManager = new DisplayEngineManager();
        this.mFingerprintMaskOverlay = new FingerprintMaskOverlay();
        this.mFingerprintCircleOverlay = new FingerprintCircleOverlay(context);
        getBrightnessRangeFromPanelInfo();
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        long identityToken = Binder.clearCallingIdentity();
        try {
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(APS_INIT_HEIGHT), false, this.settingsDisplayObserver);
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(APS_INIT_WIDTH), false, this.settingsDisplayObserver);
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("display_size_forced"), false, this.settingsDisplayObserver);
            this.settingsDisplayObserver.onChange(true);
            try {
                initBrightnessAlphaConfig();
            } catch (Exception e) {
                Log.e(TAG, "initBrightnessAlphaConfig fail ");
            }
            Configuration curConfig = new Configuration();
            try {
                curConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
                this.mFontScale = curConfig.fontScale;
            } catch (RemoteException e2) {
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
        Log.i(TAG, "current package is " + this.mPkgName);
        return this.mPkgName;
    }

    public Handler getFingerHandler() {
        return this.mFingerHandler;
    }

    public void setFingerHandler(Handler fingerHandler) {
        this.mFingerHandler = fingerHandler;
    }

    public void showMaskOrButton(String pkgName, Bundle bundle, IFingerprintServiceReceiver receiver, int type, boolean hasUdFingerprint, boolean hasBackFingerprint, IBiometricServiceReceiver dialogReceiver) {
        this.mPkgName = pkgName;
        this.mPkgAttributes = bundle;
        this.mWidgetColorSet = false;
        this.mHasUdFingerprint = hasUdFingerprint;
        this.mHasBackFingerprint = hasBackFingerprint;
        Log.d(TAG, "showMaskOrButton, mPkgAttributes=" + bundle + "mHighlightBrightnessLevel = " + this.mHighlightBrightnessLevel + " package " + pkgName + " type is " + type);
        Bundle bundle2 = this.mPkgAttributes;
        if (bundle2 == null || bundle2.getString("SystemTitle") == null) {
            this.mUseDefaultHint = true;
        } else {
            this.mUseDefaultHint = false;
        }
        this.mReceiver = receiver;
        this.mDialogReceiver = dialogReceiver;
        if ("com.huawei.systemmanager".equals(pkgName) || PKGNAME_OF_SECURITYMGR.equals(pkgName) || PKGNAME_OF_WALLET.equals(pkgName)) {
            Log.d(TAG, "do not show mask for " + pkgName);
            return;
        }
        String str = this.mPkgName;
        if (str == null || !str.equals(PKGNAME_OF_KEYGUARD)) {
            this.mWindowType = HwArbitrationDEFS.MSG_VPN_STATE_OPEN;
            this.mNotchHeight = FingerprintViewUtils.getFingerprintNotchHeight(this.mContext);
            if (type == 0) {
                this.mHandler.post(this.mAddFingerViewRunnable);
                Context context = this.mContext;
                Flog.bdReport(context, 501, "{PkgName:" + this.mPkgName + "}");
            } else if (type == 1) {
                this.mHandler.post(this.mAddButtonViewRunnable);
            } else if (type == 3) {
                this.mIsCancelHotSpotPkgAdded = isCancelHotSpotViewVisble(this.mPkgName);
                Log.d(TAG, "isCancelHotSpotNeed(mPkgName: " + this.mPkgName + " mIsCancelHotSpotPkgAdded: " + this.mIsCancelHotSpotPkgAdded);
                this.mHandler.post(this.mAddImageOnlyRunnable);
            } else if (type == 4) {
                this.mHandler.post(this.mAddBackFingprintRunnable);
            }
        } else {
            this.mWindowType = 2014;
        }
    }

    public void showMaskForApp(Bundle attribute) {
        this.mPkgName = getForegroundPkgName();
        this.mPkgAttributes = attribute;
        String str = this.mPkgName;
        if (str == null || !str.equals(PKGNAME_OF_KEYGUARD)) {
            this.mWindowType = HwArbitrationDEFS.MSG_VPN_STATE_OPEN;
        } else {
            this.mWindowType = 2014;
        }
        Bundle bundle = this.mPkgAttributes;
        if (bundle == null || bundle.getString("SystemTitle") == null) {
            this.mUseDefaultHint = true;
        } else {
            this.mUseDefaultHint = false;
        }
        this.mHandler.post(this.mAddFingerViewRunnable);
    }

    public void showSuspensionButtonForApp(int centerX, int centerY, String callingUidName) {
        Log.d(TAG, "mButtonCenterX = " + this.mButtonCenterX + ",mButtonCenterY =" + this.mButtonCenterY + ",callingUidName = " + callingUidName + ", centerX = " + centerX + ",centerY =" + centerY);
        this.mButtonCenterX = centerX;
        this.mButtonCenterY = centerY;
        if (UIDNAME_OF_KEYGUARD.equals(callingUidName)) {
            this.mPkgName = PKGNAME_OF_KEYGUARD;
        }
        this.mWindowType = HwArbitrationDEFS.MSG_VPN_STATE_OPEN;
        this.mPkgAttributes = null;
        ICallBack iCallBack = this.mFingerViewChangeCallback;
        if (iCallBack != null) {
            iCallBack.onFingerViewStateChange(2);
        }
        this.mHandler.post(this.mAddButtonViewRunnable);
    }

    public void closeEyeProtecttionMode(String pkgName) {
        Log.i(TAG, "closeEyeProtecttionMode pkgName:" + pkgName);
        if (!PKGNAME_OF_KEYGUARD.equals(pkgName) && !"com.android.settings".equals(pkgName)) {
            this.mHandler.removeCallbacks(this.mSetProtectEye);
            this.mHandler.post(this.mSetProtectEye);
        }
    }

    public void reopenEyeProtecttionMode(String pkgName) {
        Log.i(TAG, "reopenEyeProtecttionMode pkgName:" + pkgName);
        if (!PKGNAME_OF_KEYGUARD.equals(pkgName) && !"com.android.settings".equals(pkgName)) {
            this.mHandler.post(this.mSetOutProtectEye);
        }
    }

    public void removeMaskOrButton() {
        removeMaskOrButton(false);
    }

    public void removeMaskOrButton(boolean async) {
        Log.i(TAG, "removeMaskOrButton pkgName:" + this.mPkgName + ", async:" + async);
        this.mHandler.post(this.mRemoveFingerViewRunnable);
        this.mHandler.post(this.mRemoveButtonViewRunnable);
        this.mHandler.post(this.mRemoveImageOnlyRunnable);
        this.mHandler.post(this.mRemoveBackFingprintRunnable);
        this.mHandler.post(this.mRemoveHighlightCircleRunnable);
        if (async) {
            this.mHandler.post(new Runnable() {
                /* class huawei.com.android.server.fingerprint.$$Lambda$FingerViewController$dlqdPs5ubPZGdNE2I7elTXiwb7k */

                public final void run() {
                    FingerViewController.this.lambda$removeMaskOrButton$0$FingerViewController();
                }
            });
        } else {
            this.mFingerViewChangeCallback.onFingerViewStateChange(0);
        }
    }

    public /* synthetic */ void lambda$removeMaskOrButton$0$FingerViewController() {
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
        FingerprintView fingerprintView = this.mFingerView;
        if (fingerprintView == null || !fingerprintView.isAttachedToWindow()) {
            BackFingerprintView backFingerprintView = this.mBackFingerprintView;
            if (backFingerprintView != null && backFingerprintView.isAttachedToWindow()) {
                this.mHandler.post(this.mUpdateFingprintRunnable);
                return;
            }
            return;
        }
        this.mHandler.post(this.mUpdateFingerprintViewRunnable);
    }

    public void updateFingerprintView(int result, int failTimes) {
        if (result != 2) {
            this.mRemainTimes = 5 - failTimes;
        }
        this.mAuthenticateResult = result;
        FingerprintView fingerprintView = this.mFingerView;
        if (fingerprintView == null || !fingerprintView.isAttachedToWindow()) {
            BackFingerprintView backFingerprintView = this.mBackFingerprintView;
            if (backFingerprintView != null && backFingerprintView.isAttachedToWindow()) {
                this.mHandler.post(this.mUpdateFingprintRunnable);
            }
        } else {
            this.mHandler.post(this.mUpdateFingerprintViewRunnable);
        }
        RelativeLayout relativeLayout = this.mLayoutForAlipay;
        if (relativeLayout != null && relativeLayout.isAttachedToWindow()) {
            this.mHandler.post(this.mUpdateImageOnlyRunnable);
        }
    }

    public void showHighlightview(int type) {
        if (!isScreenOn() || this.highLightViewAdded) {
            Log.d(TAG, "Screen not on or already added");
            return;
        }
        this.mHighLightShowType = type;
        Log.d(TAG, "show Highlightview mHighLightShowType:" + this.mHighLightShowType);
        this.mHandler.removeCallbacks(this.mHighLightViewRunnable);
        this.mHandler.removeCallbacks(this.mRemoveHighLightView);
        this.mHandler.post(this.mHighLightViewRunnable);
        if (type == 1) {
            this.mHandler.postDelayed(this.mRemoveHighLightView, 1200);
        }
    }

    public void showHighlightviewOnKeyguard() {
        this.mHighLightShowType = 5;
        Log.d(TAG, "show Highlightview mHighLightShowType:" + this.mHighLightShowType);
        this.mHandler.removeCallbacks(this.mHighLightViewRunnable);
        this.mHandler.post(this.mHighLightViewRunnable);
    }

    public void loadFingerviewAnimOnKeyguard() {
        this.mHandler.removeCallbacks(this.mLoadFingerprintAnimViewRunnable);
        this.mHandler.removeCallbacks(this.mDestroyAnimViewRunnable);
        this.mHandler.post(this.mLoadFingerprintAnimViewRunnable);
    }

    public void removeHighlightviewOnKeyguard() {
        this.mHandler.post(new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass4 */

            public void run() {
                Log.i(FingerViewController.TAG_THREAD, "removeHighlightviewOnKeyguard");
                SurfaceControl.openTransaction();
                try {
                    FingerViewController.this.mFingerprintMaskOverlay.hide();
                    FingerViewController.this.mFingerprintCircleOverlay.hide();
                    if (!FingerViewController.this.mAnimFileNames.isEmpty()) {
                        FingerViewController.this.removeAnimViewIfAttached();
                    } else if (FingerViewController.this.mFingerprintAnimationView != null && FingerViewController.this.mFingerprintAnimationView.isAttachedToWindow()) {
                        FingerViewController.this.mFingerprintAnimationView.setAddState(false);
                        FingerViewController.this.mWindowManager.removeView(FingerViewController.this.mFingerprintAnimationView);
                    }
                } finally {
                    SurfaceControl.closeTransaction();
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void removeAnimViewIfAttached() {
        FingerprintAnimByThemeView fingerprintAnimByThemeView = this.mFingerprintAnimByThemeView;
        if (fingerprintAnimByThemeView != null && fingerprintAnimByThemeView.isAttachedToWindow() && this.mFingerprintAnimByThemeView.isAdded()) {
            this.mFingerprintAnimByThemeView.setAddState(false);
            this.mWindowManager.removeView(this.mFingerprintAnimByThemeView);
        }
    }

    public void destroyHighlightviewOnKeyguard() {
        this.mHandler.post(new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass5 */

            public void run() {
                FingerViewController.this.mFingerprintMaskOverlay.destroy();
                FingerViewController.this.mFingerprintCircleOverlay.destroy();
                FingerViewController.this.removeFingerprintAnimationView();
            }
        });
    }

    public void destroyFingerAnimViewOnKeyguard() {
        Log.i(TAG, "come in destroyFingerAnimViewOnKeyguard");
        this.mHandler.post(new Runnable() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass6 */

            public void run() {
                FingerViewController.this.removeAnimViewIfAttached();
            }
        });
        this.mHandler.removeCallbacks(this.mDestroyAnimViewRunnable);
        this.mHandler.postDelayed(this.mDestroyAnimViewRunnable, 10000);
    }

    /* access modifiers changed from: private */
    public void removeFingerprintAnimationView() {
        FingerprintAnimationView fingerprintAnimationView = this.mFingerprintAnimationView;
        if (fingerprintAnimationView != null && fingerprintAnimationView.isAttachedToWindow() && this.mWindowManager != null && this.mFingerprintAnimationView.isAdded()) {
            this.mFingerprintAnimationView.setAddState(false);
            this.mWindowManager.removeView(this.mFingerprintAnimationView);
            Log.i(TAG_THREAD, "removeFingerprintAnimationView");
        }
    }

    public void showHighlightCircleOnKeyguard() {
        if (!isScreenOn()) {
            Log.i(TAG, "Screen not on, return");
        } else {
            this.mHandler.post(new Runnable() {
                /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass7 */

                /* JADX INFO: finally extract failed */
                public void run() {
                    Log.i(FingerViewController.TAG_THREAD, "showHighlightCircleOnKeyguard");
                    SurfaceControl.openTransaction();
                    try {
                        if (!FingerViewController.this.mFingerprintCircleOverlay.isCreate()) {
                            Log.i(FingerViewController.TAG, "circle not created, create it before show");
                            float scale = FingerViewController.this.getPxScale();
                            int unused = FingerViewController.this.mFingerprintCenterX = (FingerViewController.this.mFingerprintPosition[0] + FingerViewController.this.mFingerprintPosition[2]) / 2;
                            int unused2 = FingerViewController.this.mFingerprintCenterY = (FingerViewController.this.mFingerprintPosition[1] + FingerViewController.this.mFingerprintPosition[3]) / 2;
                            FingerViewController.this.mFingerprintCircleOverlay.create(FingerViewController.this.mFingerprintCenterX, FingerViewController.this.mFingerprintCenterY, scale);
                        }
                        FingerViewController.this.mFingerprintCircleOverlay.setLayer(FingerViewController.CIRCLE_LAYER);
                        FingerViewController.this.mFingerprintCircleOverlay.show();
                        FingerViewController.this.getBrightness();
                        if (!FingerViewController.this.mFingerprintMaskOverlay.isCreate()) {
                            Log.i(FingerViewController.TAG, "mask not created, create it before show");
                            FingerViewController.this.mFingerprintMaskOverlay.create(FingerViewController.this.mCurrentWidth, FingerViewController.this.mCurrentHeight, 0);
                        }
                        int unused3 = FingerViewController.this.mCurrentAlpha = FingerViewController.this.getMaskAlpha(FingerViewController.this.mCurrentBrightness);
                        FingerViewController.this.setBacklightViaDisplayEngine(1, (float) ((int) FingerViewController.this.mMaxDigitalBrigtness), FingerViewController.this.transformBrightnessViaScreen(FingerViewController.this.mCurrentBrightness));
                        FingerViewController.this.mFingerprintMaskOverlay.setLayer(FingerViewController.MASK_LAYER);
                        FingerViewController.this.mFingerprintMaskOverlay.setAlpha(((float) FingerViewController.this.mCurrentAlpha) / FingerViewController.MAX_BRIGHTNESS_LEVEL);
                        FingerViewController.this.mFingerprintMaskOverlay.show();
                        SurfaceControl.closeTransaction();
                        FingerViewController.this.showFingerAnimViewByTheme();
                    } catch (Throwable th) {
                        SurfaceControl.closeTransaction();
                        throw th;
                    }
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void showFingerAnimViewByTheme() {
        if (this.mFingerprintAnimByThemeView == null || this.mAnimFileNames.isEmpty()) {
            Log.i(TAG, "come in mFingerprintAnimationView");
            if (this.mFingerprintAnimationView == null) {
                this.mFingerprintAnimationView = new FingerprintAnimationView(this.mContext);
            }
            this.mFingerprintAnimationView.setCenterPoints(this.mFingerprintCenterX, this.mFingerprintCenterY);
            this.mFingerprintAnimationView.setScale(getPxScale());
            if (!this.mFingerprintAnimationView.isAdded()) {
                this.mFingerprintAnimationView.setAddState(true);
                WindowManager windowManager = this.mWindowManager;
                FingerprintAnimationView fingerprintAnimationView = this.mFingerprintAnimationView;
                windowManager.addView(fingerprintAnimationView, fingerprintAnimationView.getViewParams());
            }
        } else {
            this.mFingerprintAnimByThemeView.setCenterPoints(this.mFingerprintCenterX, this.mFingerprintCenterY);
            this.mFingerprintAnimByThemeView.setScale(getPxScale());
            this.mFingerprintAnimByThemeView.post(new Runnable() {
                /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass8 */

                public void run() {
                    FingerViewController.this.mFingerprintAnimByThemeView.setAnimationPosition();
                }
            });
            if (!this.mFingerprintAnimByThemeView.isAdded()) {
                Log.i(TAG, "come in mFingerprintAnimByThemeView");
                WindowManager windowManager2 = this.mWindowManager;
                FingerprintAnimByThemeView fingerprintAnimByThemeView = this.mFingerprintAnimByThemeView;
                windowManager2.addView(fingerprintAnimByThemeView, fingerprintAnimByThemeView.getFingerViewParams());
                this.mFingerprintAnimByThemeView.setAddState(true);
            }
        }
        Log.i(TAG_THREAD, "finished showHighlightCircleOnKeyguard");
    }

    /* access modifiers changed from: private */
    public void createMaskAndCircleOnKeyguard() {
        this.mFingerprintMaskOverlay.create(this.mCurrentWidth, this.mCurrentHeight, 0);
        int[] iArr = this.mFingerprintPosition;
        this.mFingerprintCenterX = (iArr[0] + iArr[2]) / 2;
        this.mFingerprintCenterY = (iArr[1] + iArr[3]) / 2;
        this.mFingerprintCircleOverlay.create(this.mFingerprintCenterX, this.mFingerprintCenterY, getPxScale());
    }

    public void removeHighlightview(int type) {
        Log.d(TAG, "removeHighlightview mHighLightRemoveType:" + type);
        this.mHighLightRemoveType = type;
        if (this.highLightViewAdded) {
            this.mHandler.removeCallbacks(this.mRemoveHighLightView);
        }
        this.mHandler.removeCallbacks(this.mHighLightViewRunnable);
        if (type == 0) {
            this.mHandler.post(new Runnable() {
                /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass9 */

                public void run() {
                    Log.i(FingerViewController.TAG, "begin anonymous runnable in removeHighlightview");
                    FingerViewController.this.mHandler.postDelayed(FingerViewController.this.mRemoveFingerHighLightView, 110);
                    if (FingerViewController.this.mHighLightView != null) {
                        FingerViewController fingerViewController = FingerViewController.this;
                        fingerViewController.startAlphaValueAnimation(fingerViewController.mHighLightView, false, FingerViewController.this.mHighLightView.getAlpha() / FingerViewController.MAX_BRIGHTNESS_LEVEL, 0.0f, 110, 0);
                    }
                }
            });
        } else {
            this.mHandler.post(this.mRemoveHighLightView);
        }
    }

    private void initFingerPrintViewSubContentDes() {
        ImageView imageView = this.mFingerprintView;
        if (imageView != null) {
            imageView.setContentDescription(this.mContext.getString(33686246));
        }
        RelativeLayout relativeLayout = this.mCancelView;
        if (relativeLayout != null) {
            relativeLayout.setContentDescription(this.mContext.getString(33686247));
        }
    }

    private void initAddButtonViwSubContentDes() {
        SuspensionButton suspensionButton = this.mButtonView;
        if (suspensionButton != null) {
            suspensionButton.setContentDescription(this.mContext.getString(33686248));
        }
    }

    /* access modifiers changed from: private */
    public void createFingerprintView() {
        Log.d(TAG_THREAD, "fingerviewadded,mWidgetColor = " + this.mWidgetColor + ", addView:" + this.mFingerprintViewAdded);
        initBaseElement();
        updateBaseElementMargins();
        updateExtraElement();
        initFingerprintViewParams();
        this.mFingerViewParams.type = this.mWindowType;
        initFingerPrintViewSubContentDes();
        if (!this.mFingerprintViewAdded) {
            startBlurScreenshot();
            this.mWidgetColor = HwColorPicker.processBitmap(this.mScreenShot).getWidgetColor();
            this.mWidgetColorSet = true;
            this.mWindowManager.addView(this.mFingerView, this.mFingerViewParams);
            this.mFingerprintViewAdded = true;
            exitSingleHandMode();
            registerSingerHandObserver();
            getNotchState();
            transferNotchRoundCorner(0);
        }
    }

    public boolean getFingerPrintRealHeightScale() {
        int[] iArr = this.mFingerprintPosition;
        int fingerprintPositionHight = (iArr[1] + iArr[3]) / 2;
        int i = this.mDefaultDisplayHeight;
        if (i != 0) {
            mFingerPositionHeightScale = (((float) fingerprintPositionHight) * 1.0f) / ((float) i);
        }
        Log.d(TAG, "isNewMagazineViewForDownFP,mFingerPositionHeightScale:" + mFingerPositionHeightScale);
        return true;
    }

    public boolean isNewMagazineViewForDownFP() {
        if (mFingerPositionHeightScale == 0.0f) {
            getFingerPrintRealHeightScale();
        }
        return mFingerPositionHeightScale > LV_FINGERPRINT_POSITION_VIEW_HIGHT_SCALE;
    }

    /* access modifiers changed from: private */
    public void backFingerprintUsePasswordViewOnclick() {
        onUsePasswordClick();
        String foregroundPkgName = getForegroundPkgName();
        Log.d(TAG, "onClick UsePwd, foregroundPkgName = " + foregroundPkgName);
        if (foregroundPkgName == null || !isBroadcastNeed(foregroundPkgName)) {
            IBiometricServiceReceiver iBiometricServiceReceiver = this.mDialogReceiver;
            if (iBiometricServiceReceiver != null) {
                try {
                    iBiometricServiceReceiver.onDialogDismissed(1);
                } catch (RemoteException e) {
                    Log.d(TAG, "catch exception mDialogReceiver");
                }
            } else {
                IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mReceiver;
                if (iFingerprintServiceReceiver != null) {
                    try {
                        iFingerprintServiceReceiver.onError(0, 10, 0);
                    } catch (RemoteException e2) {
                        Log.d(TAG, "catch exception mReceiver");
                    }
                } else {
                    IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal = this.mBiometricServiceReceiver;
                    if (iBiometricServiceReceiverInternal != null) {
                        try {
                            iBiometricServiceReceiverInternal.onDialogDismissed(1);
                        } catch (RemoteException e3) {
                            Log.d(TAG, "catch exception mBiometricServiceReceiver");
                        }
                    } else {
                        Log.v(TAG, "do nothing");
                    }
                }
            }
        } else {
            Intent usePasswordIntent = new Intent(FINGERPRINT_IN_DISPLAY);
            usePasswordIntent.setPackage(foregroundPkgName);
            usePasswordIntent.putExtra(FINGERPRINT_IN_DISPLAY_HELPCODE_KEY, 1010);
            usePasswordIntent.putExtra(FINGERPRINT_IN_DISPLAY_HELPSTRING_KEY, FINGERPRINT_IN_DISPLAY_HELPSTRING_USE_PASSWORD_VALUE);
            this.mContext.sendBroadcast(usePasswordIntent);
        }
    }

    /* access modifiers changed from: private */
    public void cancelHotSpotViewOnclick() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        this.mHandler.post(this.mRemoveImageOnlyRunnable);
        sendKeyEvent();
    }

    /* access modifiers changed from: private */
    public void backFingerprintCancelViewOnclick() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        onCancelClick();
        Context context = this.mContext;
        Flog.bdReport(context, 502, "{PkgName:" + this.mPkgName + "}");
        String foregroundPkgName = getForegroundPkgName();
        if (foregroundPkgName == null || !isBroadcastNeed(foregroundPkgName)) {
            IBiometricServiceReceiver iBiometricServiceReceiver = this.mDialogReceiver;
            if (iBiometricServiceReceiver != null) {
                try {
                    iBiometricServiceReceiver.onDialogDismissed(2);
                } catch (RemoteException e) {
                    Log.d(TAG, "catch exception");
                }
            } else {
                IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mReceiver;
                if (iFingerprintServiceReceiver != null) {
                    try {
                        iFingerprintServiceReceiver.onAcquired(0, 6, 11);
                    } catch (RemoteException e2) {
                        Log.d(TAG, "catch exception");
                    }
                } else {
                    IBiometricServiceReceiverInternal iBiometricServiceReceiverInternal = this.mBiometricServiceReceiver;
                    if (iBiometricServiceReceiverInternal != null) {
                        try {
                            iBiometricServiceReceiverInternal.onDialogDismissed(2);
                        } catch (RemoteException e3) {
                            Log.d(TAG, "catch exception backFingerprintCancelViewOnclick");
                        }
                    } else {
                        Log.v(TAG, "do nothing");
                    }
                }
            }
        } else {
            Intent cancelMaskIntent = new Intent(FINGERPRINT_IN_DISPLAY);
            cancelMaskIntent.putExtra(FINGERPRINT_IN_DISPLAY_HELPCODE_KEY, 1011);
            cancelMaskIntent.putExtra(FINGERPRINT_IN_DISPLAY_HELPSTRING_KEY, FINGERPRINT_IN_DISPLAY_HELPSTRING_CLOSE_VIEW_VALUE);
            cancelMaskIntent.setPackage(foregroundPkgName);
            this.mContext.sendBroadcast(cancelMaskIntent);
        }
    }

    private void initBaseElement() {
        int curRotation = this.mWindowManager.getDefaultDisplay().getRotation();
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        if (this.mFingerView != null && this.mCurrentHeight == this.mSavedMaskHeight && curRotation == this.mSavedRotation && dpi == this.mSavedMaskDpi) {
            Log.d(TAG, "don't need to inflate mFingerView again");
            return;
        }
        this.mSavedMaskDpi = dpi;
        this.mSavedMaskHeight = this.mCurrentHeight;
        this.mSavedRotation = curRotation;
        if (isNewMagazineViewForDownFP()) {
            this.mFingerView = (FingerprintView) this.mLayoutInflater.inflate(34013413, (ViewGroup) null);
            Log.d(TAG, " add inflate mLVFingerView!!");
        } else {
            this.mFingerView = (FingerprintView) this.mLayoutInflater.inflate(34013346, (ViewGroup) null);
        }
        this.mFingerView.setCallback(new FingerprintViewCallback());
        this.mFingerprintView = (ImageView) this.mFingerView.findViewById(34603170);
        this.mRemoteView = (RelativeLayout) this.mFingerView.findViewById(34603439);
        if (isNewMagazineViewForDownFP()) {
            float dpiScale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
            this.mLVBackFingerprintUsePasswordView = (Button) this.mFingerView.findViewById(34603371);
            this.mLVBackFingerprintCancelView = (Button) this.mFingerView.findViewById(34603370);
            RelativeLayout buttonLayout = (RelativeLayout) this.mFingerView.findViewById(34603369);
            if (buttonLayout != null) {
                ViewGroup.LayoutParams buttonLayoutParams = buttonLayout.getLayoutParams();
                if (buttonLayoutParams instanceof RelativeLayout.LayoutParams) {
                    buttonLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472673)) * dpiScale) + 0.5f);
                    buttonLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472672)) * dpiScale) + 0.5f);
                    ((RelativeLayout.LayoutParams) buttonLayoutParams).bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472671)) * dpiScale) + 0.5f);
                    buttonLayout.setLayoutParams(buttonLayoutParams);
                }
            }
            Button button = this.mLVBackFingerprintUsePasswordView;
            if (button != null) {
                button.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472676)) * dpiScale) + 0.5f)));
                ViewGroup.LayoutParams usePasswordViewParams = this.mLVBackFingerprintUsePasswordView.getLayoutParams();
                if (usePasswordViewParams instanceof RelativeLayout.LayoutParams) {
                    usePasswordViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472119)) * dpiScale) + 0.5f);
                    usePasswordViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472112)) * dpiScale) + 0.5f);
                    getCurrentRotation();
                    int i = this.mCurrentRotation;
                    if (i == 1 || i == 3) {
                        ((RelativeLayout.LayoutParams) usePasswordViewParams).topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472674)) * dpiScale) + 0.5f);
                    }
                    this.appParamsUsePassworView = new RelativeLayout.LayoutParams((RelativeLayout.LayoutParams) usePasswordViewParams);
                    this.mLVBackFingerprintUsePasswordView.setLayoutParams(usePasswordViewParams);
                    Log.i(TAG, "zc initBaseElement usePasswordViewParams " + usePasswordViewParams.width + " " + usePasswordViewParams.height);
                }
                this.mLVBackFingerprintUsePasswordView.setOnClickListener(new View.OnClickListener() {
                    /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass13 */

                    public void onClick(View v) {
                        FingerViewController.this.backFingerprintUsePasswordViewOnclick();
                    }
                });
            }
            Button button2 = this.mLVBackFingerprintCancelView;
            if (button2 != null) {
                button2.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472676)) * dpiScale) + 0.5f)));
                ViewGroup.LayoutParams cancelViewParams = this.mLVBackFingerprintCancelView.getLayoutParams();
                if (cancelViewParams instanceof RelativeLayout.LayoutParams) {
                    cancelViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472119)) * dpiScale) + 0.5f);
                    cancelViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472112)) * dpiScale) + 0.5f);
                    getCurrentRotation();
                    int i2 = this.mCurrentRotation;
                    if (i2 == 1 || i2 == 3) {
                        ((RelativeLayout.LayoutParams) cancelViewParams).topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472678)) * dpiScale) + 0.5f);
                    }
                    this.appParamsCancelView = new RelativeLayout.LayoutParams((RelativeLayout.LayoutParams) cancelViewParams);
                    this.cancelButtonMarginStart = ((RelativeLayout.LayoutParams) cancelViewParams).getMarginStart();
                    this.cancelButtonMarginEnd = ((RelativeLayout.LayoutParams) cancelViewParams).getMarginEnd();
                    this.mLVBackFingerprintCancelView.setLayoutParams(cancelViewParams);
                }
                this.mLVBackFingerprintCancelView.setOnClickListener(new View.OnClickListener() {
                    /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass14 */

                    public void onClick(View v) {
                        FingerViewController.this.backFingerprintCancelViewOnclick();
                    }
                });
                return;
            }
            return;
        }
        this.mCancelView = (RelativeLayout) this.mFingerView.findViewById(34603040);
        this.mCancelView.setOnClickListener(new View.OnClickListener() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass15 */

            public void onClick(View v) {
                FingerViewController.this.backFingerprintCancelViewOnclick();
            }
        });
    }

    private void updateBaseElementMargins() {
        getCurrentRotation();
        int[] fingerprintMargin = calculateFingerprintMargin();
        RelativeLayout relativeLayout = (RelativeLayout) this.mFingerView.findViewById(34603024);
        ViewGroup.MarginLayoutParams fingerviewLayoutParams = (ViewGroup.MarginLayoutParams) relativeLayout.getLayoutParams();
        float dpiScale = getDPIScale();
        float scale = getPxScale();
        if (isNewMagazineViewForDownFP()) {
            Log.i(TAG, "fingerviewLayoutParams.width = " + fingerviewLayoutParams.width + ",fingerviewLayoutParams.");
            ViewGroup.MarginLayoutParams fingerprintImageParams = (ViewGroup.MarginLayoutParams) this.mFingerprintView.getLayoutParams();
            int i = this.mFingerLogoRadius;
            fingerprintImageParams.width = (int) (((float) i) * scale * 2.0f);
            fingerprintImageParams.height = (int) (((float) i) * scale * 2.0f);
            fingerprintImageParams.leftMargin = fingerprintMargin[0];
            fingerprintImageParams.topMargin = fingerprintMargin[1];
            Log.i(TAG, "zc fingerprintViewParams.width = " + fingerprintImageParams.width + ", fingerprintViewParams.height = " + fingerprintImageParams.height);
            this.mFingerprintView.setLayoutParams(fingerprintImageParams);
            int i2 = this.mCurrentRotation;
            if (i2 == 1 || i2 == 3) {
                fingerviewLayoutParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472670)) * dpiScale) + 0.5f);
                fingerviewLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472683)) * dpiScale) + 0.5f);
                relativeLayout.setLayoutParams(fingerviewLayoutParams);
                ViewGroup.MarginLayoutParams remoteViewLayoutParams = (ViewGroup.MarginLayoutParams) this.mRemoteView.getLayoutParams();
                remoteViewLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472683)) * dpiScale) + 0.5f);
                remoteViewLayoutParams.leftMargin = calculateRemoteViewLeftMargin(fingerprintImageParams.width);
                remoteViewLayoutParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472712)) * dpiScale) + 0.5f);
                remoteViewLayoutParams.rightMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472711)) * dpiScale) + 0.5f);
                remoteViewLayoutParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472709)) * dpiScale) + 0.5f);
                this.mRemoteView.setLayoutParams(remoteViewLayoutParams);
            }
        } else {
            int i3 = this.mCurrentRotation;
            if (i3 == 1 || i3 == 3) {
                fingerviewLayoutParams.width = (fingerprintMargin[0] * 2) + ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472496)) * dpiScale) + 0.5f));
                int[] layoutMargin = calculateFingerprintLayoutLeftMargin(fingerviewLayoutParams.width);
                fingerviewLayoutParams.leftMargin = layoutMargin[0];
                fingerviewLayoutParams.rightMargin = layoutMargin[1];
                relativeLayout.setLayoutParams(fingerviewLayoutParams);
                ViewGroup.MarginLayoutParams remoteViewLayoutParams2 = (ViewGroup.MarginLayoutParams) this.mRemoteView.getLayoutParams();
                int remoteViewLeftLayoutmargin = calculateRemoteViewLeftMargin(fingerviewLayoutParams.width);
                remoteViewLayoutParams2.width = (int) ((((((float) this.mCurrentHeight) - (((float) (this.mContext.getResources().getDimensionPixelSize(34472710) * 2)) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472686)) * dpiScale)) - ((float) fingerviewLayoutParams.width)) + 0.5f);
                remoteViewLayoutParams2.leftMargin = remoteViewLeftLayoutmargin;
                remoteViewLayoutParams2.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472712)) * dpiScale) + 0.5f);
                remoteViewLayoutParams2.rightMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472711)) * dpiScale) + 0.5f);
                remoteViewLayoutParams2.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472709)) * dpiScale) + 0.5f);
                this.mRemoteView.setLayoutParams(remoteViewLayoutParams2);
                Log.d(TAG, " RemoteviewLayoutParams.leftMargin =" + remoteViewLayoutParams2.leftMargin + ",rightMargin =" + remoteViewLayoutParams2.rightMargin);
            } else {
                fingerviewLayoutParams.width = this.mCurrentWidth;
                fingerviewLayoutParams.leftMargin = 0;
            }
        }
        ViewGroup.MarginLayoutParams fingerprintImageParams2 = (ViewGroup.MarginLayoutParams) this.mFingerprintView.getLayoutParams();
        int i4 = this.mFingerLogoRadius;
        fingerprintImageParams2.width = (int) (((float) i4) * scale * 2.0f);
        fingerprintImageParams2.height = (int) (((float) i4) * scale * 2.0f);
        fingerprintImageParams2.leftMargin = fingerprintMargin[0];
        fingerprintImageParams2.topMargin = fingerprintMargin[1];
        Log.d(TAG, "fingerprintViewParams.width = " + fingerprintImageParams2.width + ", fingerprintViewParams.height = " + fingerprintImageParams2.height);
        this.mFingerprintView.setLayoutParams(fingerprintImageParams2);
        if (!isNewMagazineViewForDownFP()) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateBaseElementMargins mFingerprintCenterX = ");
            int[] iArr = this.mFingerprintPosition;
            sb.append((iArr[1] + iArr[3]) / 2);
            Log.d(TAG, sb.toString());
            int[] iArr2 = this.mFingerprintPosition;
            int fingerprintPosition = (iArr2[1] + iArr2[3]) / 2;
            int cancelButtonsize = this.mContext.getResources().getDimensionPixelSize(34472266);
            ViewGroup.MarginLayoutParams cancelViewParams = (ViewGroup.MarginLayoutParams) this.mCancelView.getLayoutParams();
            if (fingerprintPosition == FINGERPRINT_IN_DISPLAY_POSITION_VIEW_VALUE_ELLE || fingerprintPosition == FINGERPRINT_IN_DISPLAY_POSITION_VIEW_VALUE_VOGUE) {
                cancelButtonsize = this.mContext.getResources().getDimensionPixelSize(34472267);
            }
            int i5 = this.mCurrentRotation;
            if (i5 == 1 || i5 == 3) {
                cancelViewParams.topMargin = (int) (((((float) this.mCurrentWidth) - (((float) cancelButtonsize) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472773)) * dpiScale)) + 0.5f);
            } else {
                cancelViewParams.topMargin = (int) (((((float) this.mCurrentHeight) - (((float) cancelButtonsize) * dpiScale)) - (((float) this.mContext.getResources().getDimensionPixelSize(34472773)) * dpiScale)) + 0.5f);
            }
            ImageView cancelImage = (ImageView) this.mCancelView.findViewById(34603038);
            ViewGroup.MarginLayoutParams cancelImageParams = (ViewGroup.MarginLayoutParams) cancelImage.getLayoutParams();
            cancelImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472772)) * dpiScale) + 0.5f);
            cancelImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472772)) * dpiScale) + 0.5f);
            cancelImage.setLayoutParams(cancelImageParams);
            Log.d(TAG, "cancelViewParams.topMargin = " + cancelViewParams.topMargin);
            cancelViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472493)) * dpiScale) + 0.5f);
            cancelViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472492)) * dpiScale) + 0.5f);
            this.mCancelView.setLayoutParams(cancelViewParams);
        }
    }

    private void updateUsePasswordAndCancelView(float dpiScale) {
        RelativeLayout.LayoutParams layoutParams = this.appParamsCancelView;
        if (layoutParams != null) {
            layoutParams.addRule(16, 34603144);
            this.appParamsCancelView.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472679)) * dpiScale) + 0.5f);
            int i = this.cancelButtonMarginStart;
            if (i != 0) {
                this.appParamsCancelView.setMarginStart(i);
            }
            int i2 = this.cancelButtonMarginEnd;
            if (i2 != 0) {
                this.appParamsCancelView.setMarginEnd(i2);
            }
            this.mLVBackFingerprintCancelView.setLayoutParams(this.appParamsCancelView);
        }
        RelativeLayout.LayoutParams layoutParams2 = this.appParamsUsePassworView;
        if (layoutParams2 != null) {
            layoutParams2.addRule(17, 34603144);
            this.appParamsUsePassworView.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472679)) * dpiScale) + 0.5f);
            this.mLVBackFingerprintUsePasswordView.setLayoutParams(this.appParamsUsePassworView);
        }
    }

    private void updateExtraElement() {
        int i;
        int i2;
        float dpiScale = getDPIScale();
        RelativeLayout usePasswordHotSpot = (RelativeLayout) this.mFingerView.findViewById(34603518);
        TextView usePasswordView = (TextView) this.mFingerView.findViewById(34603027);
        RelativeLayout usePasswordHotspotLayout = (RelativeLayout) this.mFingerView.findViewById(34603518);
        TextView fingerprintCancel = (TextView) this.mFingerView.findViewById(34603023);
        RelativeLayout titleAndSummaryView = (RelativeLayout) this.mFingerView.findViewById(34603026);
        ViewGroup.MarginLayoutParams titleAndSummaryViewParams = (ViewGroup.MarginLayoutParams) titleAndSummaryView.getLayoutParams();
        if (isNewMagazineViewForDownFP() && ((i2 = this.mCurrentRotation) == 1 || i2 == 3)) {
            titleAndSummaryViewParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472085)) * dpiScale) + 0.5f);
        }
        titleAndSummaryViewParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472085)) * dpiScale) + 0.5f);
        titleAndSummaryView.setLayoutParams(titleAndSummaryViewParams);
        TextView appNameView = (TextView) this.mFingerView.findViewById(34603025);
        appNameView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472084)) * dpiScale) + 0.5f)));
        TextView accountMessageView = (TextView) this.mFingerView.findViewById(34603008);
        accountMessageView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34471948)) * dpiScale) + 0.5f)));
        ViewGroup.MarginLayoutParams accountMessageViewParams = (ViewGroup.MarginLayoutParams) accountMessageView.getLayoutParams();
        accountMessageViewParams.topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34471949)) * dpiScale) + 0.5f);
        accountMessageView.setLayoutParams(accountMessageViewParams);
        if (isNewMagazineViewForDownFP()) {
            Button button = this.mLVBackFingerprintUsePasswordView;
            if (button != null) {
                button.setText(this.mContext.getString(33685687));
            }
            Button button2 = this.mLVBackFingerprintCancelView;
            if (button2 != null) {
                button2.setText(this.mContext.getString(33685746));
            }
        } else {
            ViewGroup.MarginLayoutParams usePasswordHotSpotParams = (ViewGroup.MarginLayoutParams) usePasswordHotSpot.getLayoutParams();
            usePasswordHotSpotParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472807)) * dpiScale) + 0.5f);
            usePasswordHotSpotParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472806)) * dpiScale) + 0.5f);
            usePasswordHotSpot.setLayoutParams(usePasswordHotSpotParams);
            usePasswordView.setText(this.mContext.getString(33685687));
            usePasswordView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472808)) * dpiScale) + 0.5f)));
            if (usePasswordHotspotLayout != null) {
                usePasswordHotspotLayout.setContentDescription(this.mContext.getString(33685687));
            }
            if (this.mIsBiometricPrompt && fingerprintCancel != null) {
                ViewGroup.MarginLayoutParams fingerpirntCancelHotSpotParams = (ViewGroup.MarginLayoutParams) fingerprintCancel.getLayoutParams();
                fingerpirntCancelHotSpotParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472806)) * dpiScale) + 0.5f);
                fingerprintCancel.setLayoutParams(fingerpirntCancelHotSpotParams);
                fingerprintCancel.setText(this.mContext.getString(33685746));
                fingerprintCancel.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472808)) * dpiScale) + 0.5f)));
                fingerprintCancel.setContentDescription(this.mContext.getString(33685746));
            }
        }
        this.mHintView = (HintText) this.mFingerView.findViewById(34603180);
        resetFrozenCountDownIfNeed();
        if (this.mIsFingerFrozen) {
            HintText hintText = this.mHintView;
            Resources resources = this.mContext.getResources();
            int i3 = this.mRemainedSecs;
            hintText.setText(resources.getQuantityString(34406411, i3, Integer.valueOf(i3)));
        } else if (this.mHasBackFingerprint) {
            this.mHintView.setText(this.mContext.getString(33685678));
        } else {
            this.mHintView.setText(this.mContext.getString(33685690));
        }
        this.mHintView.setTextSize(0, (float) ((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472514)) * dpiScale) + 0.5f)));
        if (!isNewMagazineViewForDownFP()) {
            ViewGroup.MarginLayoutParams hintViewParams = (ViewGroup.MarginLayoutParams) this.mHintView.getLayoutParams();
            hintViewParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472513)) * dpiScale) + 0.5f);
            this.mHintView.setLayoutParams(hintViewParams);
            usePasswordHotSpot.setOnClickListener(new View.OnClickListener() {
                /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass16 */

                public void onClick(View v) {
                    FingerViewController.this.backFingerprintUsePasswordViewOnclick();
                }
            });
        } else {
            ViewGroup.LayoutParams hintViewParams2 = this.mHintView.getLayoutParams();
            if (hintViewParams2 instanceof RelativeLayout.LayoutParams) {
                getCurrentRotation();
                int i4 = this.mCurrentRotation;
                if (i4 == 1 || i4 == 3) {
                    ((RelativeLayout.LayoutParams) hintViewParams2).topMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472682)) * dpiScale) + 0.5f);
                } else {
                    ((RelativeLayout.LayoutParams) hintViewParams2).bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472681)) * dpiScale) + 0.5f);
                }
                this.mHintView.setLayoutParams(hintViewParams2);
            }
        }
        if (this.mPkgAttributes != null) {
            Log.d(TAG, "mPkgAttributes =" + this.mPkgAttributes);
            updateUsePasswordAndCancelView(dpiScale);
            if (this.mPkgAttributes.getString("googleFlag") == null) {
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
                    if (!isNewMagazineViewForDownFP()) {
                        usePasswordHotSpot.setVisibility(0);
                    } else {
                        this.mLVBackFingerprintUsePasswordView.setVisibility(0);
                        ViewGroup.LayoutParams params = this.mLVBackFingerprintCancelView.getLayoutParams();
                        if (params instanceof RelativeLayout.LayoutParams) {
                            params.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472679)) * dpiScale) + 0.5f);
                            this.mLVBackFingerprintCancelView.setLayoutParams(params);
                        }
                    }
                } else if (!isNewMagazineViewForDownFP()) {
                    usePasswordHotSpot.setVisibility(4);
                } else {
                    getCurrentRotation();
                    updateButtomLayoutInNewFinger(dpiScale);
                }
                if (this.mPkgAttributes.getString("SystemTitle") != null) {
                    Log.d(TAG, "attributestring =" + this.mPkgAttributes.getString("SystemTitle"));
                    this.mHintView.setText(this.mPkgAttributes.getString("SystemTitle"));
                    return;
                }
                return;
            }
            if (this.mPkgAttributes.getString("title") != null) {
                Log.d(TAG, "title =" + this.mPkgAttributes.getString("title"));
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
                Log.d(TAG, "description =" + this.mPkgAttributes.getString("description"));
                this.mHintView.setText(this.mPkgAttributes.getString("description"));
            }
            if (this.mPkgAttributes.getString("positive_text") != null) {
                Log.d(TAG, "positive_text =" + this.mPkgAttributes.getString("positive_text"));
                if (!isNewMagazineViewForDownFP()) {
                    i = 0;
                    usePasswordHotSpot.setVisibility(0);
                    usePasswordView.setText(this.mPkgAttributes.getString("positive_text"));
                } else {
                    i = 0;
                    this.mLVBackFingerprintUsePasswordView.setVisibility(0);
                    this.mLVBackFingerprintUsePasswordView.setText(this.mPkgAttributes.getString("positive_text"));
                }
            } else {
                i = 0;
                if (!isNewMagazineViewForDownFP()) {
                    usePasswordHotSpot.setVisibility(4);
                } else if (this.mLVBackFingerprintUsePasswordView != null) {
                    updateButtomLayoutInNewFinger(dpiScale);
                }
            }
            if (this.mPkgAttributes.getString("negative_text") != null) {
                Log.d(TAG, "negative_text =" + this.mPkgAttributes.getString("negative_text"));
                Button button3 = this.mLVBackFingerprintCancelView;
                if (button3 != null) {
                    button3.setText(this.mPkgAttributes.getString("negative_text"));
                }
                if (fingerprintCancel != null) {
                    if (!this.mIsBiometricPrompt) {
                        i = 8;
                    }
                    fingerprintCancel.setVisibility(i);
                    fingerprintCancel.setText(this.mPkgAttributes.getString("negative_text"));
                }
            } else if (fingerprintCancel != null) {
                fingerprintCancel.setVisibility(8);
            }
        } else {
            this.mRemoteView.setVisibility(4);
            appNameView.setVisibility(4);
            accountMessageView.setVisibility(8);
            if (!isNewMagazineViewForDownFP()) {
                usePasswordHotSpot.setVisibility(4);
                if (fingerprintCancel != null) {
                    fingerprintCancel.setVisibility(8);
                    return;
                }
                return;
            }
            getCurrentRotation();
            updateButtomLayoutInNewFinger(dpiScale);
        }
    }

    private void updateButtomLayoutInNewFinger(float dpiScale) {
        int i = this.mCurrentRotation;
        if (i == 1 || i == 3) {
            this.mLVBackFingerprintUsePasswordView.setVisibility(4);
        } else {
            this.mLVBackFingerprintUsePasswordView.setVisibility(8);
        }
        ViewGroup.LayoutParams params = this.mLVBackFingerprintCancelView.getLayoutParams();
        if (params instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) params).removeRule(16);
            ((RelativeLayout.LayoutParams) params).addRule(1);
            ((RelativeLayout.LayoutParams) params).width = -2;
            ((RelativeLayout.LayoutParams) params).setMarginStart(0);
            ((RelativeLayout.LayoutParams) params).setMarginEnd(0);
            this.mLVBackFingerprintCancelView.setLayoutParams(params);
        }
    }

    private void getCurrentRotation() {
        this.mCurrentRotation = this.mWindowManager.getDefaultDisplay().getRotation();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x005b  */
    /* JADX WARNING: Removed duplicated region for block: B:12:? A[RETURN, SYNTHETIC] */
    public void getCurrentFingerprintCenter() {
        this.mCurrentRotation = this.mWindowManager.getDefaultDisplay().getRotation();
        int i = this.mCurrentRotation;
        if (i != 0) {
            if (i == 1) {
                int[] iArr = this.mFingerprintPosition;
                this.mFingerprintCenterX = (iArr[1] + iArr[3]) / 2;
                this.mFingerprintCenterY = (iArr[0] + iArr[2]) / 2;
            } else if (i != 2) {
                if (i == 3) {
                    int i2 = this.mDefaultDisplayHeight;
                    int[] iArr2 = this.mFingerprintPosition;
                    this.mFingerprintCenterX = i2 - ((iArr2[1] + iArr2[3]) / 2);
                    this.mFingerprintCenterY = (iArr2[0] + iArr2[2]) / 2;
                }
            }
            if (this.mCurrentRotation != 1) {
                this.mFingerprintCenterX -= this.mNotchHeight;
                return;
            }
            return;
        }
        int[] iArr3 = this.mFingerprintPosition;
        this.mFingerprintCenterX = (iArr3[0] + iArr3[2]) / 2;
        this.mFingerprintCenterY = (iArr3[1] + iArr3[3]) / 2;
        if (this.mCurrentRotation != 1) {
        }
    }

    private void initFingerprintViewParams() {
        if (this.mFingerViewParams == null) {
            this.mFingerViewParams = new WindowManager.LayoutParams(-1, -1);
            WindowManager.LayoutParams layoutParams = this.mFingerViewParams;
            layoutParams.layoutInDisplayCutoutMode = 1;
            layoutParams.flags = 201852424;
            layoutParams.privateFlags |= 16;
            WindowManager.LayoutParams layoutParams2 = this.mFingerViewParams;
            layoutParams2.format = -3;
            layoutParams2.screenOrientation = 14;
            layoutParams2.setTitle(FINGRPRINT_VIEW_TITLE_NAME);
        }
    }

    /* access modifiers changed from: private */
    public void removeFingerprintView() {
        Log.d(TAG, "removeFingerprintView start added = " + this.mFingerprintViewAdded);
        FingerprintView fingerprintView = this.mFingerView;
        if (fingerprintView != null && this.mFingerprintViewAdded && fingerprintView.isAttachedToWindow()) {
            this.mWindowManager.removeView(this.mFingerView);
            resetFingerprintView();
            if (this.mNotchConerStatusChanged) {
                transferNotchRoundCorner(1);
            }
            Log.d(TAG, "removeFingerprintView is done is View Added = " + this.mFingerprintViewAdded + "mFingerView =" + this.mFingerView);
        }
    }

    /* access modifiers changed from: private */
    public void updateFingerprintView() {
        int i = this.mAuthenticateResult;
        if (i == 1) {
            if (this.mUseDefaultHint) {
                updateHintView();
            }
        } else if (i == 0) {
            if (this.mUseDefaultHint) {
                updateHintView(this.mContext.getString(33685680));
            }
            ImageView imageView = this.mFingerprintView;
            if (imageView != null) {
                imageView.setContentDescription(this.mContext.getString(33685680));
            }
            String foregroundPkg = getForegroundPkgName();
            String[] strArr = PACKAGES_USE_HWAUTH_INTERFACE;
            for (String pkgName : strArr) {
                if (pkgName.equals(foregroundPkg)) {
                    Log.d(TAG, "hw wallet Identifing,pkgName = " + pkgName);
                }
            }
            if (!this.mKeepMaskAfterAuthentication) {
                removeMaskOrButton();
            }
        } else if (i == 2) {
            if (!this.mUseDefaultHint) {
                return;
            }
            if (this.mRemainTimes == 5) {
                if (this.mHasBackFingerprint) {
                    updateHintView(this.mContext.getString(33685678));
                } else {
                    updateHintView(this.mContext.getString(33685690));
                }
                ImageView imageView2 = this.mFingerprintView;
                if (imageView2 != null) {
                    imageView2.setContentDescription(this.mContext.getString(33686246));
                    return;
                }
                return;
            }
            updateHintView();
        } else if (i == 3) {
            ImageView imageView3 = this.mFingerprintView;
            if (imageView3 != null) {
                imageView3.setContentDescription(this.mContext.getString(33685684));
            }
            if (this.mUseDefaultHint) {
                updateHintView(this.mContext.getString(33685684));
            }
        }
    }

    private void updateHintView() {
        Log.d(TAG, "updateFingerprintView start,mFingerprintViewAdded = " + this.mFingerprintViewAdded);
        if (this.mFingerprintViewAdded && this.mHintView != null) {
            int i = this.mRemainTimes;
            if (i > 0 && i < 5) {
                Log.d(TAG, "remaind time = " + this.mRemainTimes);
                Resources resources = this.mContext.getResources();
                int i2 = this.mRemainTimes;
                String trymoreStr = resources.getQuantityString(34406412, i2, Integer.valueOf(i2));
                this.mHintView.setText(trymoreStr);
                ImageView imageView = this.mFingerprintView;
                if (imageView != null) {
                    imageView.setContentDescription(trymoreStr);
                }
            } else if (this.mRemainTimes == 0) {
                if (!this.mIsFingerFrozen) {
                    RemainTimeCountDown remainTimeCountDown = this.mMyCountDown;
                    if (remainTimeCountDown != null) {
                        remainTimeCountDown.cancel();
                    }
                    ImageView imageView2 = this.mFingerprintView;
                    if (imageView2 != null) {
                        imageView2.setContentDescription(this.mContext.getResources().getQuantityString(34406411, 30, 30));
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

    /* access modifiers changed from: private */
    public void updateHintView(String hint) {
        HintText hintText = this.mHintView;
        if (hintText != null) {
            hintText.setText(hint);
        }
        if (this.mFingerprintViewAdded) {
            this.mWindowManager.updateViewLayout(this.mFingerView, this.mFingerViewParams);
        }
    }

    private void resetFingerprintView() {
        this.mFingerprintViewAdded = false;
        Bitmap bitmap = this.mBLurBitmap;
        if (bitmap != null) {
            bitmap.recycle();
            FingerprintView fingerprintView = this.mFingerView;
            if (fingerprintView != null) {
                fingerprintView.setBackgroundDrawable(null);
            }
        }
        Bitmap bitmap2 = this.mScreenShot;
        if (bitmap2 != null) {
            bitmap2.recycle();
            this.mScreenShot = null;
        }
        RelativeLayout relativeLayout = this.mRemoteView;
        if (relativeLayout != null) {
            relativeLayout.removeAllViews();
        }
        unregisterSingerHandObserver();
    }

    /* access modifiers changed from: private */
    public void initFingerprintAnimView() {
        Log.i(TAG, "come in  initFingerprintAnimView");
        this.mFingerprintAnimByThemeView = new FingerprintAnimByThemeView(this.mContext, this.mAnimFileNames, FingerprintAnimByThemeModel.getFpAnimFps(this.mContext));
        FingerprintAnimByThemeModel.setLoadAmount();
        Log.i(TAG, "initFingerprintAnimView mFingerprintAnimViewByTheme is" + this.mFingerprintAnimByThemeView);
    }

    private void sendKeyEvent() {
        int[] actions;
        for (int i : new int[]{0, 1}) {
            long curTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(new KeyEvent(curTime, curTime, i, 4, 0, 0, -1, 0, 8, 257), 0);
        }
    }

    /* access modifiers changed from: private */
    public void createImageOnlyView() {
        int curHeight = SystemPropertiesEx.getInt("persist.sys.rog.height", this.mDefaultDisplayHeight);
        int dpi = SystemProperties.getInt("persist.sys.dpi", SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 0)));
        if (!(this.mLayoutForAlipay != null && curHeight == this.mSavedImageHeight && dpi == this.mSavedImageDpi)) {
            this.mSavedImageHeight = curHeight;
            this.mSavedImageDpi = dpi;
            this.mLayoutForAlipay = (RelativeLayout) this.mLayoutInflater.inflate(34013345, (ViewGroup) null);
            this.mFingerprintImageForAlipay = (ImageView) this.mLayoutForAlipay.findViewById(34603143);
            this.mCancelViewImageOnly = (RelativeLayout) this.mLayoutForAlipay.findViewById(34603041);
            if (this.mIsCancelHotSpotPkgAdded) {
                this.mCancelViewImageOnly.setVisibility(0);
                this.mCancelViewImageOnly.setOnClickListener(new View.OnClickListener() {
                    /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass22 */

                    public void onClick(View v) {
                        FingerViewController.this.cancelHotSpotViewOnclick();
                    }
                });
            } else {
                this.mCancelViewImageOnly.setVisibility(8);
            }
        }
        if (!this.mHasUdFingerprint) {
            this.mFingerprintImageForAlipay.setImageResource(33751215);
        } else if (isNewMagazineViewForDownFP()) {
            this.mFingerprintImageForAlipay.setImageResource(33751367);
        } else {
            this.mAlipayDrawable = new BreathImageDrawable(this.mContext);
            this.mAlipayDrawable.setBreathImageDrawable(null, this.mContext.getDrawable(33751367));
            this.mFingerprintImageForAlipay.setImageDrawable(this.mAlipayDrawable);
            this.mFingerprintImageForAlipay.setOnTouchListener(new View.OnTouchListener() {
                /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass23 */

                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();
                    if (action == 0) {
                        FingerViewController.this.mAlipayDrawable.startTouchDownBreathAnim();
                    } else if (action == 1) {
                        FingerViewController.this.mAlipayDrawable.startTouchUpBreathAnim();
                    }
                    return true;
                }
            });
            this.mAlipayDrawable.startBreathAnim();
        }
        int[] fingerprintMargin = calculateFingerprintImageMargin();
        ViewGroup.MarginLayoutParams fingerprintImageParams = (ViewGroup.MarginLayoutParams) this.mFingerprintImageForAlipay.getLayoutParams();
        float dpiScale = getDPIScale();
        fingerprintImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472495)) * dpiScale) + 0.5f);
        fingerprintImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472494)) * dpiScale) + 0.5f);
        this.mFingerprintImageForAlipay.setLayoutParams(fingerprintImageParams);
        WindowManager.LayoutParams fingerprintOnlyLayoutParams = new WindowManager.LayoutParams();
        FingerprintViewUtils.setFingerprintOnlyLayoutParams(this.mContext, fingerprintOnlyLayoutParams, this.mWindowType, fingerprintMargin);
        if (this.mIsCancelHotSpotPkgAdded) {
            fingerprintOnlyLayoutParams.y = fingerprintMargin[1] - (((int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472492)) * dpiScale) + 0.5f)) * 2);
            fingerprintOnlyLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472494)) * dpiScale) + (((float) this.mContext.getResources().getDimensionPixelSize(34472492)) * dpiScale * 2.0f) + 0.5f);
            ViewGroup.MarginLayoutParams cancelViewParams = (ViewGroup.MarginLayoutParams) this.mCancelViewImageOnly.getLayoutParams();
            cancelViewParams.bottomMargin = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34471941)) * dpiScale) + 0.5f);
            ImageView cancelImage = (ImageView) this.mCancelViewImageOnly.findViewById(34603039);
            ViewGroup.MarginLayoutParams cancelImageParams = (ViewGroup.MarginLayoutParams) cancelImage.getLayoutParams();
            cancelImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472677)) * dpiScale) + 0.5f);
            cancelImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472677)) * dpiScale) + 0.5f);
            cancelImage.setLayoutParams(cancelImageParams);
            cancelViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472493)) * dpiScale) + 0.5f);
            cancelViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472492)) * dpiScale) + 0.5f);
            this.mCancelViewImageOnly.setLayoutParams(cancelViewParams);
        } else {
            fingerprintOnlyLayoutParams.y = fingerprintMargin[1];
            fingerprintOnlyLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472494)) * dpiScale) + 0.5f);
        }
        Log.d(TAG, "fingerprintImage location = [" + fingerprintOnlyLayoutParams.x + "," + fingerprintOnlyLayoutParams.y + "]");
        if (!this.mFingerprintOnlyViewAdded) {
            this.mWindowManager.addView(this.mLayoutForAlipay, fingerprintOnlyLayoutParams);
            this.mFingerprintOnlyViewAdded = true;
            exitSingleHandMode();
            registerSingerHandObserver();
        }
    }

    /* access modifiers changed from: private */
    public void updateImageOnlyView() {
        if (this.mAuthenticateResult == 0) {
            this.mHandler.post(this.mRemoveImageOnlyRunnable);
            this.mFingerViewChangeCallback.onFingerViewStateChange(0);
        }
    }

    /* access modifiers changed from: private */
    public void removeImageOnlyView() {
        RelativeLayout relativeLayout;
        if (this.mFingerprintOnlyViewAdded && (relativeLayout = this.mLayoutForAlipay) != null) {
            this.mWindowManager.removeView(relativeLayout);
            this.mFingerprintOnlyViewAdded = false;
            unregisterSingerHandObserver();
        }
    }

    private int slideOffsetDistance(float scale) {
        int offsetDistance;
        if (TextUtils.isEmpty(SLIDE_PROP)) {
            return 0;
        }
        String[] slideProps = SLIDE_PROP.split(",");
        int slideDistance = 0;
        if (slideProps.length == 4) {
            try {
                slideDistance = Integer.valueOf(slideProps[1]).intValue();
            } catch (NumberFormatException e) {
                Log.e(TAG, "slideOffsetDistance NumberFormatException format exception");
            }
        }
        if (slideDistance == 0) {
            return 0;
        }
        int offsetDistance2 = this.mDefaultDisplayWidth;
        if (offsetDistance2 > slideDistance) {
            offsetDistance = offsetDistance2 - slideDistance;
        } else {
            offsetDistance = slideDistance - offsetDistance2;
        }
        if (slideDistance == 0) {
            return 0;
        }
        int offsetDistance3 = (int) (((((float) offsetDistance) * scale) + 0.5f) / 2.0f);
        Log.i(TAG, "slideOffsetDistance offsetDistance = " + offsetDistance3);
        return offsetDistance3;
    }

    private int[] calculateFingerprintImageMargin() {
        Log.d(TAG, "left = " + this.mFingerprintPosition[0] + "right = " + this.mFingerprintPosition[2]);
        Log.d(TAG, "top = " + this.mFingerprintPosition[1] + "button = " + this.mFingerprintPosition[3]);
        float dpiScale = getDPIScale();
        int[] margin = new int[2];
        int fingerPrintInScreenWidth = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472495)) * dpiScale) + 0.5f);
        int fingerPrintInScreenHeight = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472494)) * dpiScale) + 0.5f);
        Log.d(TAG, "current height = " + this.mCurrentHeight + "mDefaultDisplayHeight = " + this.mDefaultDisplayHeight);
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        getCurrentRotation();
        int i = this.mCurrentRotation;
        if (i == 0 || i == 2) {
            int[] iArr = this.mFingerprintPosition;
            this.mFingerprintCenterX = (iArr[0] + iArr[2]) / 2;
            this.mFingerprintCenterY = (iArr[1] + iArr[3]) / 2;
            int marginLeft = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
            int marginTop = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
            margin[0] = marginLeft;
            margin[1] = marginTop;
            Log.d(TAG, "marginLeft = " + marginLeft + "marginTop = " + marginTop + "scale = " + scale);
        } else {
            int[] iArr2 = this.mFingerprintPosition;
            this.mFingerprintCenterX = (iArr2[1] + iArr2[3]) / 2;
            this.mFingerprintCenterY = (iArr2[0] + iArr2[2]) / 2;
            int marginTop2 = (int) (((((float) this.mFingerprintCenterY) * scale) - ((float) (fingerPrintInScreenHeight / 2))) + ((float) slideOffsetDistance(scale)) + 0.5f);
            int marginLeft2 = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
            if (this.mCurrentRotation == 3) {
                marginLeft2 = (this.mCurrentHeight - marginLeft2) - fingerPrintInScreenWidth;
            }
            margin[0] = marginLeft2;
            margin[1] = marginTop2;
        }
        return margin;
    }

    /* access modifiers changed from: private */
    public void createAndAddButtonView() {
        int i;
        if (this.mButtonViewAdded) {
            adjustButtonViewVisibility(this.mPkgName);
            Log.d(TAG, " mButtonViewAdded return ");
            return;
        }
        calculateButtonPosition();
        Log.d(TAG, "createAndAddButtonView,pkg = " + this.mPkgName);
        float dpiScale = getDPIScale();
        if (this.mButtonView == null || this.mCurrentHeight != this.mSavedButtonHeight) {
            this.mButtonView = (SuspensionButton) this.mLayoutInflater.inflate(34013344, (ViewGroup) null);
            this.mButtonView.setCallback(new SuspensionButtonCallback());
            this.mSavedButtonHeight = this.mCurrentHeight;
        }
        initAddButtonViwSubContentDes();
        this.mButtonView.setOnClickListener(new View.OnClickListener() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass27 */

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
                Context access$3900 = FingerViewController.this.mContext;
                Flog.bdReport(access$3900, 501, "{PkgName:" + FingerViewController.this.mPkgName + "}");
            }
        });
        ImageView buttonImage = (ImageView) this.mButtonView.findViewById(34603037);
        ViewGroup.MarginLayoutParams buttonImageParams = (ViewGroup.MarginLayoutParams) buttonImage.getLayoutParams();
        buttonImageParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472493)) * dpiScale) + 0.5f);
        buttonImageParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472492)) * dpiScale) + 0.5f);
        buttonImage.setLayoutParams(buttonImageParams);
        this.mSuspensionButtonParams = new WindowManager.LayoutParams();
        if (PKGNAME_OF_KEYGUARD.equals(this.mPkgName)) {
            this.mSuspensionButtonParams.type = 2014;
        } else {
            this.mSuspensionButtonParams.type = 2003;
        }
        WindowManager.LayoutParams layoutParams = this.mSuspensionButtonParams;
        layoutParams.flags = 16777480;
        layoutParams.gravity = 8388659;
        layoutParams.x = (int) ((((float) this.mButtonCenterX) - ((((float) this.mContext.getResources().getDimensionPixelSize(34472493)) * dpiScale) / 2.0f)) + 0.5f);
        Log.d(TAG, "mSuspensionButtonParams.x=" + this.mSuspensionButtonParams.x);
        this.mSuspensionButtonParams.y = (int) ((((float) this.mButtonCenterY) - ((((float) this.mContext.getResources().getDimensionPixelSize(34472492)) * dpiScale) / 2.0f)) + 0.5f);
        Log.d(TAG, "mSuspensionButtonParams.y=" + this.mSuspensionButtonParams.y);
        this.mSuspensionButtonParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472493)) * dpiScale) + 0.5f);
        this.mSuspensionButtonParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472492)) * dpiScale) + 0.5f);
        WindowManager.LayoutParams layoutParams2 = this.mSuspensionButtonParams;
        layoutParams2.format = -3;
        layoutParams2.privateFlags |= 16;
        this.mSuspensionButtonParams.setTitle("fingerprintview_button");
        this.mButtonView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass28 */

            public boolean onPreDraw() {
                boolean unused = FingerViewController.this.mWidgetColorSet = false;
                return true;
            }
        });
        if (PKGNAME_OF_KEYGUARD.equals(this.mPkgName) && (i = this.mButtonColor) != 0) {
            this.mWidgetColor = i;
            this.mWidgetColorSet = true;
        }
        if (!this.mWidgetColorSet) {
            Log.d(TAG, "mWidgetColorSet is false, get a new screenshot and calculate color");
            getScreenShot();
            this.mWidgetColor = HwColorPicker.processBitmap(this.mScreenShot).getWidgetColor();
            this.mWidgetColorSet = true;
        }
        Log.d(TAG, "mWidgetColor = " + this.mWidgetColor);
        buttonImage.setColorFilter(this.mWidgetColor);
        adjustButtonViewVisibility(this.mPkgName);
        this.mWindowManager.addView(this.mButtonView, this.mSuspensionButtonParams);
        this.mButtonViewAdded = true;
    }

    private void adjustButtonViewVisibility(String packageName) {
        if (this.mButtonView == null) {
            Log.e(TAG, "mButtonView is null, cannot change visibility");
        } else if (this.mButtonViewState != 2 || !PKGNAME_OF_KEYGUARD.equals(packageName)) {
            this.mButtonView.setVisibility(0);
        } else {
            this.mButtonView.setVisibility(4);
        }
    }

    /* access modifiers changed from: private */
    public void removeButtonView() {
        if (this.mButtonView != null && this.mButtonViewAdded) {
            Log.d(TAG, "removeButtonView begin, mButtonViewAdded added = " + this.mButtonViewAdded + "mButtonView = " + this.mButtonView);
            this.mWindowManager.removeViewImmediate(this.mButtonView);
            this.mButtonViewAdded = false;
        }
    }

    /* access modifiers changed from: private */
    public void updateButtonView() {
        WindowManager.LayoutParams layoutParams;
        calculateButtonPosition();
        if (this.mButtonView != null && this.mButtonViewAdded && (layoutParams = this.mSuspensionButtonParams) != null) {
            layoutParams.x = this.mButtonCenterX - (this.mContext.getResources().getDimensionPixelSize(34472773) / 2);
            this.mSuspensionButtonParams.y = this.mButtonCenterY - (this.mContext.getResources().getDimensionPixelSize(34472773) / 2);
            Log.d(TAG, "updateButtonView x=" + this.mSuspensionButtonParams.x + " y=" + this.mSuspensionButtonParams.y);
            this.mWindowManager.updateViewLayout(this.mButtonView, this.mSuspensionButtonParams);
        }
    }

    /* access modifiers changed from: private */
    public void startAlphaValueAnimation(final HighLightMaskView target, final boolean isAlphaUp, float startAlpha, final float endAlpha, long startDelay, int duration) {
        if (target == null) {
            Log.d(TAG, " animation abort target null");
            return;
        }
        if (startAlpha > ALPHA_LIMITED) {
            startAlpha = ALPHA_LIMITED;
        } else if (startAlpha < 0.0f) {
            startAlpha = 0.0f;
        }
        if (endAlpha > ALPHA_LIMITED) {
            endAlpha = ALPHA_LIMITED;
        } else if (endAlpha < 0.0f) {
            endAlpha = 0.0f;
        }
        if (isAlphaUp || startAlpha != endAlpha) {
            Log.d(TAG, " startAlphaAnimation current alpha:" + target.getAlpha() + " startAlpha:" + startAlpha + " endAlpha: " + endAlpha + " duration :" + duration);
            ValueAnimator animator = ValueAnimator.ofFloat(startAlpha, endAlpha);
            animator.setStartDelay(startDelay);
            animator.setInterpolator(endAlpha == 0.0f ? new AccelerateInterpolator() : new DecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass29 */

                public void onAnimationUpdate(ValueAnimator animation) {
                    target.setAlpha((int) (((Float) animation.getAnimatedValue()).floatValue() * FingerViewController.MAX_BRIGHTNESS_LEVEL));
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass30 */
                boolean isCanceled = false;

                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    Log.d(FingerViewController.TAG, " onAnimationEnd endAlpha:" + endAlpha + " isCanceled:" + this.isCanceled);
                    if (!isAlphaUp && !this.isCanceled) {
                        FingerViewController.this.mHandler.post(FingerViewController.this.mRemoveHighLightView);
                    }
                    this.isCanceled = false;
                }

                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
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

    /* access modifiers changed from: private */
    public int getEnrollDigitalBrigtness() {
        return this.mEnrollDigitalBrigtness;
    }

    public void setEnrollDigitalBrigtness(int enrollDigitalBrigtness) {
        this.mEnrollDigitalBrigtness = enrollDigitalBrigtness;
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

    /* access modifiers changed from: private */
    public void createAndAddHighLightView() {
        if (this.mHighLightView == null) {
            this.mHighLightView = new HighLightMaskView(this.mContext, this.mCurrentBrightness, this.mHighlightSpotRadius, this.mHighlightSpotColor);
        }
        Log.d(TAG, "SpotColor = " + this.mHighlightSpotColor);
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
        WindowManager.LayoutParams highLightViewParams = this.mHighLightView.getHighlightViewParams();
        highLightViewParams.setTitle("hwSingleMode_window_for_UD_highlight_mask");
        if (this.mHighLightShowType == 1) {
            highLightViewParams.setTitle(HIGHLIGHT_VIEW_TITLE_NAME);
        }
        int i = this.mHighLightShowType;
        if (i == 1) {
            this.mHighLightView.setCircleVisibility(0);
        } else if (i == 0) {
            this.mHighLightView.setCircleVisibility(4);
        }
        if (this.mHighLightView.getParent() != null) {
            Log.v(TAG, "REMOVE! mHighLightView before add");
            this.mWindowManager.removeView(this.mHighLightView);
        }
        this.mWindowManager.addView(this.mHighLightView, highLightViewParams);
        this.highLightViewAdded = true;
        if (this.mHighLightShowType == 1) {
            this.mMaxDigitalBrigtness = transformBrightnessViaScreen(this.mHighlightBrightnessLevel);
            setBacklightViaDisplayEngine(1, this.mMaxDigitalBrigtness, transformBrightnessViaScreen(this.mCurrentBrightness));
            DisplayEngineManager displayEngineManager = this.mDisplayEngineManager;
            if (displayEngineManager != null) {
                displayEngineManager.setScene(29, (int) this.mMaxDigitalBrigtness);
                Log.d(TAG, "mDisplayEngineManager set scene");
            }
        }
    }

    private void setBacklightViaDisplayEngine(float maxBright) {
        setBacklightViaDisplayEngine(1, maxBright, maxBright);
    }

    /* access modifiers changed from: private */
    public void setBacklightViaDisplayEngine(int scene, float maxBright, float currentBright) {
        if (this.mDisplayEngineManager == null) {
            Log.d(TAG, "mDisplayEngineManager is null");
            return;
        }
        Log.d(TAG, "set scene: " + scene + "Bright max: " + maxBright + " current:" + currentBright);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putIntArray("Buffer", new int[]{scene, (int) maxBright, (int) currentBright});
        bundle.putInt("BufferLength", UD_BUFFSIZE);
        this.mDisplayEngineManager.setData(13, bundle);
        Log.d(TAG, "mDisplayEngineManager set scene: " + scene + "Bright max: " + maxBright + " current:" + currentBright);
    }

    /* access modifiers changed from: private */
    public float transformBrightnessViaScreen(int brightness) {
        int i = INVALID_BRIGHTNESS;
        if (i == this.mNormalizedMaxBrightness || i == this.mNormalizedMinBrightness) {
            Log.i(TAG, "have not get the valid brightness, try again");
            getBrightnessRangeFromPanelInfo();
        }
        int i2 = this.mNormalizedMaxBrightness;
        int i3 = this.mNormalizedMinBrightness;
        return (((((float) brightness) - 4.0f) / 251.0f) * ((float) (i2 - i3))) + ((float) i3);
    }

    /* access modifiers changed from: private */
    public void removeHighLightViewInner() {
        if (this.highLightViewAdded && this.mHighLightView != null) {
            Log.i(TAG, "highlightview is show, remove highlightview");
            this.mWindowManager.removeView(this.mHighLightView);
        }
        this.mHighLightView = null;
        this.highLightViewAdded = false;
        this.mHandler.removeCallbacks(this.mSetScene);
        this.mHandler.postDelayed(this.mSetScene, SET_SCENE_DELAY_TIME);
        getInstance(this.mContext).notifyDismissBlueSpot();
    }

    /* access modifiers changed from: private */
    public void createBackFingprintView() {
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
            Log.e(TAG, "fontScaleChange before createBackFingprintView, curCenfig.fontScale : " + curConfig.fontScale + ", mFontScale : " + this.mFontScale);
            this.mContext.getResources().updateConfiguration(curConfig, null);
            this.mFontScale = curConfig.fontScale;
        }
        if (!(this.mBackFingerprintView != null && this.mCurrentHeight == this.mSavedBackViewHeight && currentRotation == this.mSavedBackViewRotation && dpi == this.mSavedBackViewDpi && !fontScaleChange)) {
            this.mBackFingerprintView = (BackFingerprintView) this.mLayoutInflater.inflate(34013462, (ViewGroup) null);
            this.mSavedBackViewDpi = dpi;
            this.mSavedBackViewHeight = this.mCurrentHeight;
            this.mSavedBackViewRotation = currentRotation;
        }
        float dpiScale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        this.mBackFingerprintHintView = (TextView) this.mBackFingerprintView.findViewById(34603531);
        this.mBackFingerprintUsePasswordView = (Button) this.mBackFingerprintView.findViewById(34603533);
        this.mBackFingerprintCancelView = (Button) this.mBackFingerprintView.findViewById(34603528);
        RelativeLayout buttonLayout = (RelativeLayout) this.mBackFingerprintView.findViewById(34603527);
        ViewGroup.MarginLayoutParams usePasswordViewParams = (ViewGroup.MarginLayoutParams) this.mBackFingerprintUsePasswordView.getLayoutParams();
        usePasswordViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472119)) * dpiScale) + 0.5f);
        usePasswordViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472112)) * dpiScale) + 0.5f);
        this.mBackFingerprintUsePasswordView.setLayoutParams(usePasswordViewParams);
        ViewGroup.MarginLayoutParams cancelViewParams = (ViewGroup.MarginLayoutParams) this.mBackFingerprintCancelView.getLayoutParams();
        cancelViewParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472119)) * dpiScale) + 0.5f);
        cancelViewParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472112)) * dpiScale) + 0.5f);
        this.mBackFingerprintCancelView.setLayoutParams(cancelViewParams);
        ViewGroup.MarginLayoutParams buttonLayoutParams = (ViewGroup.MarginLayoutParams) buttonLayout.getLayoutParams();
        buttonLayoutParams.width = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472118)) * dpiScale) + 0.5f);
        buttonLayoutParams.height = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472117)) * dpiScale) + 0.5f);
        buttonLayout.setLayoutParams(buttonLayoutParams);
        TextView backFingerprintTitle = (TextView) this.mBackFingerprintView.findViewById(34603532);
        TextView backFingerprintDescription = (TextView) this.mBackFingerprintView.findViewById(34603529);
        Bundle bundle = this.mPkgAttributes;
        if (bundle != null) {
            if (bundle.getString("title") != null) {
                Log.d(TAG, "title =" + this.mPkgAttributes.getString("title"));
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
                Log.d(TAG, "description =" + this.mPkgAttributes.getString("description"));
                backFingerprintDescription.setText(this.mPkgAttributes.getString("description"));
            }
            if (this.mPkgAttributes.getString("positive_text") != null) {
                Log.d(TAG, "positive_text =" + this.mPkgAttributes.getString("positive_text"));
                this.mBackFingerprintUsePasswordView.setVisibility(0);
                this.mBackFingerprintUsePasswordView.setText(this.mPkgAttributes.getString("positive_text"));
            } else {
                this.mBackFingerprintUsePasswordView.setVisibility(4);
            }
            if (this.mPkgAttributes.getString("negative_text") != null) {
                Log.d(TAG, "negative_text =" + this.mPkgAttributes.getString("negative_text"));
                this.mBackFingerprintCancelView.setText(this.mPkgAttributes.getString("negative_text"));
            }
        }
        resetFrozenCountDownIfNeed();
        if (this.mIsFingerFrozen) {
            TextView textView = this.mBackFingerprintHintView;
            Resources resources = this.mContext.getResources();
            int i = this.mRemainedSecs;
            textView.setText(resources.getQuantityString(34406411, i, Integer.valueOf(i)));
        }
        this.mBackFingerprintUsePasswordView.setOnClickListener(new View.OnClickListener() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass43 */

            public void onClick(View v) {
                if (FingerViewController.this.mDialogReceiver != null) {
                    try {
                        Log.i(FingerViewController.TAG, "back fingerprint view, usepassword clicked");
                        FingerViewController.this.mDialogReceiver.onDialogDismissed(1);
                    } catch (RemoteException e) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
                if (FingerViewController.this.mBiometricServiceReceiver != null) {
                    try {
                        Log.i(FingerViewController.TAG, "back fingerprint view, usepassword clicked");
                        FingerViewController.this.mBiometricServiceReceiver.onDialogDismissed(2);
                    } catch (RemoteException e2) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
        this.mBackFingerprintCancelView.setOnClickListener(new View.OnClickListener() {
            /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass44 */

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
                if (FingerViewController.this.mBiometricServiceReceiver != null) {
                    try {
                        Log.i(FingerViewController.TAG, "back fingerprint view, cancel clicked");
                        FingerViewController.this.mBiometricServiceReceiver.onDialogDismissed(2);
                    } catch (RemoteException e2) {
                        Log.d(FingerViewController.TAG, "catch exception");
                    }
                }
            }
        });
        WindowManager.LayoutParams backFingerprintLayoutParams = new WindowManager.LayoutParams();
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

    /* access modifiers changed from: private */
    public void removeBackFingprintView() {
        BackFingerprintView backFingerprintView = this.mBackFingerprintView;
        if (backFingerprintView != null && backFingerprintView.isAttachedToWindow()) {
            this.mWindowManager.removeView(this.mBackFingerprintView);
            Bitmap bitmap = this.mBLurBitmap;
            if (bitmap != null) {
                bitmap.recycle();
                BackFingerprintView backFingerprintView2 = this.mBackFingerprintView;
                if (backFingerprintView2 != null) {
                    backFingerprintView2.setBackgroundDrawable(null);
                }
            }
            Bitmap bitmap2 = this.mScreenShot;
            if (bitmap2 != null) {
                bitmap2.recycle();
                this.mScreenShot = null;
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateBackFingprintView() {
        int i = this.mAuthenticateResult;
        if (i == 1) {
            updateBackFingerprintHintView();
        } else if (i == 0) {
            updateBackFingerprintHintView(this.mContext.getString(33685680));
            removeBackFingprintView();
        }
    }

    private void updateBackFingerprintHintView() {
        Log.d(TAG, "updateBackFingerprintHintView start remaind time = " + this.mRemainTimes);
        BackFingerprintView backFingerprintView = this.mBackFingerprintView;
        if (backFingerprintView != null && backFingerprintView.isAttachedToWindow() && this.mBackFingerprintHintView != null) {
            int i = this.mRemainTimes;
            if (i > 0 && i < 5) {
                Resources resources = this.mContext.getResources();
                int i2 = this.mRemainTimes;
                this.mBackFingerprintHintView.setText(resources.getQuantityString(34406412, i2, Integer.valueOf(i2)));
            } else if (this.mRemainTimes == 0) {
                if (!this.mIsFingerFrozen) {
                    RemainTimeCountDown remainTimeCountDown = this.mMyCountDown;
                    if (remainTimeCountDown != null) {
                        remainTimeCountDown.cancel();
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

    /* access modifiers changed from: private */
    public void updateBackFingerprintHintView(String hint) {
        TextView textView = this.mBackFingerprintHintView;
        if (textView != null) {
            textView.setText(hint);
        }
        BackFingerprintView backFingerprintView = this.mBackFingerprintView;
        if (backFingerprintView != null && backFingerprintView.isAttachedToWindow()) {
            this.mBackFingerprintView.postInvalidate();
        }
    }

    private void startBlurBackViewScreenshot() {
        getScreenShot();
        Log.i(TAG, "mScreenShot = " + this.mScreenShot);
        Bitmap bitmap = this.mScreenShot;
        if (bitmap == null || (HwColorPicker.processBitmap(bitmap).getDomainColor() == COLOR_BLACK && !PKGNAME_OF_KEYGUARD.equals(this.mPkgName))) {
            this.mBLurBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            this.mBLurBitmap.eraseColor(-7829368);
            this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
            this.mBackFingerprintView.setBackgroundDrawable(this.mBlurDrawable);
            return;
        }
        Context context = this.mContext;
        Bitmap bitmap2 = this.mScreenShot;
        this.mBLurBitmap = BlurUtils.blurMaskImage(context, bitmap2, bitmap2, 25);
        this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
        this.mBackFingerprintView.setBackgroundDrawable(this.mBlurDrawable);
    }

    private void onCancelClick() {
        this.mHandler.post(this.mRemoveFingerViewRunnable);
        if (!this.mIsFingerFrozen) {
            this.mHandler.post(this.mAddButtonViewRunnable);
        }
        ICallBack iCallBack = this.mFingerViewChangeCallback;
        if (iCallBack != null) {
            iCallBack.onFingerViewStateChange(2);
        }
    }

    private void onUsePasswordClick() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        this.mHandler.post(this.mRemoveFingerViewRunnable);
        this.mHandler.post(this.mRemoveButtonViewRunnable);
    }

    public void parseBundle4Keyguard(Bundle bundle) {
        int[] location;
        ICallBack iCallBack;
        int suspend = bundle.getInt("suspend", 0);
        Log.d(TAG, " suspend:" + suspend + " mButtonViewAdded:" + this.mButtonViewAdded + " mFingerprintViewAdded:" + this.mFingerprintViewAdded);
        if (suspend == -1) {
            if (this.mButtonViewAdded && (iCallBack = this.mFingerViewChangeCallback) != null) {
                iCallBack.onFingerViewStateChange(2);
            }
            if (this.mFingerprintViewAdded) {
                return;
            }
        }
        byte[] viewType = bundle.getByteArray("viewType");
        byte[] viewState = bundle.getByteArray("viewState");
        if (viewType == null || viewState == null) {
            Log.e(TAG, "viewType or viewState is null");
            return;
        }
        int i = 0;
        while (i < viewType.length && i < viewState.length) {
            if (i % 100 == 0) {
                Log.i(TAG, "do loop in parseBundle4Keyguard, time = " + i);
            }
            byte type = viewType[i];
            byte state = viewState[i];
            Log.d(TAG, " type:" + ((int) type) + " state:" + ((int) state));
            if (type != 0) {
                if (type != 1) {
                    if (type != 2 && type == 3) {
                    }
                } else if (state == 1) {
                    int[] location2 = bundle.getIntArray("buttonLocation");
                    if (location2 != null) {
                        this.mButtonViewState = 1;
                        this.mButtonColor = bundle.getInt("buttonColor", 0);
                        showSuspensionButtonForApp(location2[0], location2[1], UIDNAME_OF_KEYGUARD);
                    }
                } else if (state == 0) {
                    this.mHandler.post(this.mRemoveButtonViewRunnable);
                } else if (state == 2 && (location = bundle.getIntArray("buttonLocation")) != null) {
                    this.mHandler.post(this.mRemoveFingerViewRunnable);
                    this.mButtonViewState = 2;
                    showSuspensionButtonForApp(location[0], location[1], UIDNAME_OF_KEYGUARD);
                }
            } else if (state == 1) {
                this.mHandler.post(this.mAddFingerViewRunnable);
            } else if (state == 0) {
                this.mHandler.post(this.mRemoveFingerViewRunnable);
                ICallBack iCallBack2 = this.mFingerViewChangeCallback;
                if (iCallBack2 != null) {
                    iCallBack2.onFingerViewStateChange(1);
                }
            }
            i++;
        }
    }

    public void setFingerprintPosition(int[] position) {
        this.mFingerprintPosition = (int[]) position.clone();
        Log.d(TAG, "setFingerprintPosition,left = " + this.mFingerprintPosition[0] + "right = " + this.mFingerprintPosition[2]);
        Log.d(TAG, "setFingerprintPosition,top = " + this.mFingerprintPosition[1] + "button = " + this.mFingerprintPosition[3]);
        int[] iArr = this.mFingerprintPosition;
        this.mFingerprintCenterX = (iArr[0] + iArr[2]) / 2;
        this.mFingerprintCenterY = (iArr[1] + iArr[3]) / 2;
    }

    public void setHighLightBrightnessLevel(int brightness) {
        if (brightness > 255) {
            Log.i(TAG, "brightness is " + brightness + ",adjust it to 255");
            brightness = 255;
        } else if (brightness < 0) {
            Log.i(TAG, "brightness is " + brightness + ",adjust it to 0");
            brightness = 0;
        }
        Log.i(TAG, "brightness to be set is " + brightness);
        this.mHighlightBrightnessLevel = brightness;
        this.mMaxDigitalBrigtness = transformBrightnessViaScreen(this.mHighlightBrightnessLevel);
        setBacklightViaDisplayEngine((float) ((int) this.mMaxDigitalBrigtness));
    }

    public void setHighLightSpotColor(int color) {
        Log.i(TAG, "color to be set is " + color);
        this.mHighlightSpotColor = color;
        FingerprintCircleOverlay fingerprintCircleOverlay = this.mFingerprintCircleOverlay;
        if (fingerprintCircleOverlay != null) {
            fingerprintCircleOverlay.setColor(color);
        }
    }

    public void setHighLightSpotRadius(int radius) {
        this.mHighlightSpotRadius = radius;
        FingerprintCircleOverlay fingerprintCircleOverlay = this.mFingerprintCircleOverlay;
        if (fingerprintCircleOverlay != null) {
            fingerprintCircleOverlay.setRadius(radius);
        }
    }

    public void setFingerPrintLogoRadius(int radius) {
        this.mFingerLogoRadius = radius;
        Log.i(TAG, "setFingerPrintLogoRadius: " + this.mFingerLogoRadius);
    }

    private int[] calculateFingerprintMargin() {
        int marginLeft;
        Log.d(TAG, "left = " + this.mFingerprintPosition[0] + "right = " + this.mFingerprintPosition[2]);
        Log.d(TAG, "top = " + this.mFingerprintPosition[1] + "button = " + this.mFingerprintPosition[3]);
        float dpiScale = getDPIScale();
        int[] margin = new int[2];
        int fingerPrintInScreenWidth = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472497)) * dpiScale) + 0.5f);
        int fingerPrintInScreenHeight = (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472496)) * dpiScale) + 0.5f);
        Log.d(TAG, "current height = " + this.mCurrentHeight + "mDefaultDisplayHeight = " + this.mDefaultDisplayHeight);
        float scale = ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight);
        if (isNewMagazineViewForDownFP()) {
            int i = this.mCurrentRotation;
            if (i == 0 || i == 2) {
                int[] iArr = this.mFingerprintPosition;
                this.mFingerprintCenterX = (iArr[0] + iArr[2]) / 2;
                this.mFingerprintCenterY = (iArr[1] + iArr[3]) / 2;
                int marginLeft2 = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
                int marginTop = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
                margin[0] = marginLeft2;
                margin[1] = marginTop;
                Log.d(TAG, "marginLeft = " + marginLeft2 + "marginTop = " + marginTop + "scale = " + scale);
            } else {
                int[] iArr2 = this.mFingerprintPosition;
                this.mFingerprintCenterY = (iArr2[0] + iArr2[2]) / 2;
                this.mFingerprintCenterX = (iArr2[1] + iArr2[3]) / 2;
                int marginLeft3 = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
                int marginTop2 = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
                if (i == 3) {
                    marginLeft = (this.mCurrentHeight - marginLeft3) - fingerPrintInScreenWidth;
                } else {
                    marginLeft = marginLeft3 - this.mNotchHeight;
                }
                margin[0] = marginLeft;
                margin[1] = marginTop2;
                Log.d(TAG, "marginLeft = " + marginLeft + "marginTop = " + marginTop2 + "scale = " + scale);
            }
        } else {
            int i2 = this.mCurrentRotation;
            if (i2 == 0 || i2 == 2) {
                int[] iArr3 = this.mFingerprintPosition;
                this.mFingerprintCenterX = (iArr3[0] + iArr3[2]) / 2;
                this.mFingerprintCenterY = (iArr3[1] + iArr3[3]) / 2;
                int marginLeft4 = ((int) ((((float) this.mFingerprintCenterX) * scale) + 0.5f)) - (fingerPrintInScreenHeight / 2);
                int marginTop3 = ((int) ((((float) this.mFingerprintCenterY) * scale) + 0.5f)) - (fingerPrintInScreenWidth / 2);
                margin[0] = marginLeft4;
                margin[1] = marginTop3;
                Log.d(TAG, "marginLeft = " + marginLeft4 + "marginTop = " + marginTop3 + "scale = " + scale);
            } else {
                int[] iArr4 = this.mFingerprintPosition;
                this.mFingerprintCenterX = (iArr4[1] + iArr4[3]) / 2;
                this.mFingerprintCenterY = (iArr4[0] + iArr4[2]) / 2;
                int marginTop4 = (int) (((((float) this.mFingerprintCenterY) * scale) - (((float) fingerPrintInScreenHeight) / 2.0f)) + 0.5f);
                int marginLeft5 = (int) ((((((float) (this.mDefaultDisplayHeight - this.mFingerprintCenterX)) * scale) - (((float) this.mContext.getResources().getDimensionPixelSize(34472686)) * dpiScale)) - (((float) fingerPrintInScreenWidth) / 2.0f)) + 0.5f);
                margin[0] = marginLeft5;
                margin[1] = marginTop4;
                Log.d(TAG, "marginLeft = " + marginLeft5 + "marginTop = " + marginTop4 + "scale = " + scale + "mDefaultDisplayHeight = " + this.mDefaultDisplayHeight);
            }
        }
        return margin;
    }

    private float getDPIScale() {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 640));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        int realdpi = SystemProperties.getInt("persist.sys.realdpi", dpi);
        float scale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        Log.i(TAG, "getDPIScale: lcdDpi: " + lcdDpi + " dpi: " + dpi + " realdpi: " + realdpi + " scale: " + scale);
        return scale;
    }

    /* access modifiers changed from: private */
    public float getPxScale() {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", DEFAULT_LCD_DPI));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        int realdpi = SystemProperties.getInt("persist.sys.realdpi", dpi);
        float scale = (((float) realdpi) * 1.0f) / ((float) dpi);
        Log.i(TAG, "getPxScale: lcdDpi: " + lcdDpi + " dpi: " + dpi + " realdpi: " + realdpi + " scale: " + scale);
        return scale;
    }

    private int[] calculateFingerprintLayoutLeftMargin(int width) {
        return FingerprintViewUtils.calculateFingerprintLayoutLeftMargin(width, ((float) this.mCurrentHeight) / ((float) this.mDefaultDisplayHeight), this.mCurrentRotation, this.mContext, this.mFingerprintCenterX);
    }

    private int calculateRemoteViewLeftMargin(int fingerLayoutWidth) {
        float dpiScale = getDPIScale();
        int i = this.mCurrentRotation;
        if (i == 3) {
            return (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472710)) * dpiScale) + (((float) this.mContext.getResources().getDimensionPixelSize(34472686)) * dpiScale) + ((float) fingerLayoutWidth) + 0.5f);
        }
        if (i == 1) {
            return (int) ((((float) this.mContext.getResources().getDimensionPixelSize(34472710)) * dpiScale) + 0.5f);
        }
        return 0;
    }

    private void calculateButtonPosition() {
        String str = this.mPkgName;
        if (str != null && !str.equals(PKGNAME_OF_KEYGUARD)) {
            getCurrentRotation();
            Log.d(TAG, "mCurrentRotation = " + this.mCurrentRotation);
            int i = this.mCurrentHeight;
            float scale = ((float) i) / ((float) this.mDefaultDisplayHeight);
            int i2 = this.mCurrentRotation;
            if (i2 != 0) {
                if (i2 == 1) {
                    int[] iArr = this.mFingerprintPosition;
                    this.mButtonCenterX = (int) (((((float) (iArr[1] + iArr[3])) / 2.0f) * scale) + 0.5f);
                    this.mButtonCenterY = (this.mCurrentWidth - this.mContext.getResources().getDimensionPixelSize(34472266)) - (this.mContext.getResources().getDimensionPixelSize(34472773) / 2);
                } else if (i2 != 2) {
                    if (i2 == 3) {
                        int[] iArr2 = this.mFingerprintPosition;
                        this.mButtonCenterX = i - ((int) (((((float) (iArr2[1] + iArr2[3])) / 2.0f) * scale) + 0.5f));
                        this.mButtonCenterY = (this.mCurrentWidth - this.mContext.getResources().getDimensionPixelSize(34472266)) - (this.mContext.getResources().getDimensionPixelSize(34472773) / 2);
                    }
                }
                Log.d(TAG, "mButtonCenterX = " + this.mButtonCenterX + ",mButtonCenterY =" + this.mButtonCenterY);
            }
            int[] iArr3 = this.mFingerprintPosition;
            this.mButtonCenterX = (int) (((((float) (iArr3[0] + iArr3[2])) / 2.0f) * scale) + 0.5f);
            this.mButtonCenterY = (this.mCurrentHeight - this.mContext.getResources().getDimensionPixelSize(34472266)) - (this.mContext.getResources().getDimensionPixelSize(34472773) / 2);
            Log.d(TAG, "mButtonCenterX = " + this.mButtonCenterX + ",mButtonCenterY =" + this.mButtonCenterY);
        }
    }

    private boolean isScreenOn() {
        if (!this.mPowerManager.isInteractive()) {
            Log.i(TAG, "screen is not Interactive");
            return false;
        }
        getBrightness();
        if (this.mCurrentBrightness != 0) {
            return true;
        }
        Log.i(TAG, "brightness is not set");
        return false;
    }

    /* access modifiers changed from: private */
    public void getBrightness() {
        Bundle data = new Bundle();
        if (HwPowerManager.getHwBrightnessData("CurrentBrightness", data) != 0) {
            this.mCurrentBrightness = -1;
            Log.w(TAG, "get currentBrightness failed!");
        } else {
            this.mCurrentBrightness = data.getInt("Brightness");
        }
        Log.i(TAG, "currentBrightness=" + this.mCurrentBrightness);
    }

    public void setLightLevel(int level, int lightLevelTime) {
        try {
            this.pm.setBrightnessNoLimit(level, lightLevelTime);
            Log.d(TAG, "setLightLevel :" + level + " time:" + lightLevelTime);
        } catch (RemoteException e) {
            Log.e(TAG, "setFingerprintviewHighlight catch RemoteException ");
        }
    }

    /* JADX INFO: finally extract failed */
    private String getForegroundPkgName() {
        long identityToken = Binder.clearCallingIdentity();
        try {
            ActivityInfo info = ActivityManagerEx.getLastResumedActivity();
            Binder.restoreCallingIdentity(identityToken);
            String name = null;
            if (info != null) {
                name = info.packageName;
            }
            Log.w(TAG, "foreground package is " + name);
            return name;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    private boolean isBroadcastNeed(String pkgName) {
        for (String pkg : PACKAGES_USE_HWAUTH_INTERFACE) {
            if (pkg.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCancelHotSpotNeed(String pkgName) {
        for (String pkg : PACKAGES_USE_CANCEL_HOTSPOT_INTERFACE) {
            if (pkg.equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCancelHotSpotViewVisble(String pkgName) {
        int i;
        getCurrentRotation();
        return isCancelHotSpotNeed(pkgName) && isNewMagazineViewForDownFP() && ((i = this.mCurrentRotation) == 0 || i == 2);
    }

    private void startBlurScreenshot() {
        getScreenShot();
        Bitmap bitmap = this.mScreenShot;
        if (bitmap == null || (HwColorPicker.processBitmap(bitmap).getDomainColor() == COLOR_BLACK && !PKGNAME_OF_KEYGUARD.equals(this.mPkgName))) {
            this.mBLurBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            this.mBLurBitmap.eraseColor(-7829368);
            this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
            this.mFingerView.setBackgroundDrawable(this.mBlurDrawable);
            return;
        }
        Context context = this.mContext;
        Bitmap bitmap2 = this.mScreenShot;
        this.mBLurBitmap = BlurUtils.blurMaskImage(context, bitmap2, bitmap2, 25);
        this.mBlurDrawable = new BitmapDrawable(this.mContext.getResources(), this.mBLurBitmap);
        this.mFingerView.setBackgroundDrawable(this.mBlurDrawable);
    }

    private void getScreenShot() {
        try {
            this.mScreenShot = BlurUtils.screenShotBitmap(this.mContext, BLUR_SCALE);
            if (this.mScreenShot != null) {
                this.mScreenShot = this.mScreenShot.copy(Bitmap.Config.ARGB_8888, true);
            }
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
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("single_hand_mode"), true, this.mSingleContentObserver);
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
        Settings.Global.putString(this.mContext.getContentResolver(), "single_hand_mode", "");
    }

    private void resetFrozenCountDownIfNeed() {
        RemainTimeCountDown remainTimeCountDown;
        this.mRemainTimes = this.mFingerprintManagerEx.getRemainingNum();
        Log.d(TAG, "getRemainingNum, mRemainTimes = " + this.mRemainTimes);
        if (this.mRemainTimes > 0 && (remainTimeCountDown = this.mMyCountDown) != null) {
            remainTimeCountDown.cancel();
            this.mIsFingerFrozen = false;
        }
    }

    public void notifyFingerprintViewCoverd(boolean covered, Rect winFrame) {
        Log.v(TAG, "notifyWinCovered covered=" + covered + "winFrame = " + winFrame);
        if (!covered) {
            ICallBack iCallBack = this.mFingerViewChangeCallback;
            if (iCallBack != null) {
                iCallBack.onFingerViewStateChange(1);
            }
        } else if (isFingerprintViewCoverd(getFingerprintViewRect(), winFrame)) {
            Log.v(TAG, "new window covers fingerprintview, suspend");
            ICallBack iCallBack2 = this.mFingerViewChangeCallback;
            if (iCallBack2 != null) {
                iCallBack2.onFingerViewStateChange(2);
            }
        } else {
            Log.v(TAG, "new window doesn't cover fingerprintview");
        }
    }

    public void notifyTouchUp(float x, float y) {
        if (this.highLightViewAdded || this.mFingerprintCircleOverlay.isVisible()) {
            Log.d(TAG_THREAD, "UD Fingerprint notifyTouchUp point (" + x + " , " + y + ")");
            if (!isFingerViewTouched(x, y)) {
                Log.i(TAG, "notifyTouchUp,point not in fingerprint view");
            } else {
                this.mHandler.post(new Runnable() {
                    /* class huawei.com.android.server.fingerprint.FingerViewController.AnonymousClass45 */

                    public void run() {
                        Log.i(FingerViewController.TAG, "begin anonymous runnable in notifyTouchUp");
                        FingerViewController.this.removeHighlightCircle();
                    }
                });
            }
        }
    }

    private Rect getFingerprintViewRect() {
        int lcdDpi = SystemProperties.getInt("ro.sf.real_lcd_density", SystemProperties.getInt("ro.sf.lcd_density", 640));
        int dpi = SystemProperties.getInt("persist.sys.dpi", lcdDpi);
        float scale = (((float) SystemProperties.getInt("persist.sys.realdpi", dpi)) * 1.0f) / ((float) dpi);
        float lcdscale = (((float) lcdDpi) * 1.0f) / ((float) dpi);
        Rect fingerprintViewRect = new Rect();
        int[] iArr = this.mFingerprintPosition;
        fingerprintViewRect.left = (int) (((double) ((((float) (iArr[0] + iArr[2])) / 2.0f) * scale)) - ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472497)) * lcdscale)) * 0.5d) + 0.5d));
        int[] iArr2 = this.mFingerprintPosition;
        fingerprintViewRect.top = (int) (((double) ((((float) (iArr2[1] + iArr2[3])) / 2.0f) * scale)) - ((((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472496)) * lcdscale)) * 0.5d) + 0.5d));
        int[] iArr3 = this.mFingerprintPosition;
        fingerprintViewRect.right = (int) (((double) ((((float) (iArr3[0] + iArr3[2])) / 2.0f) * scale)) + (((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472497)) * lcdscale)) * 0.5d) + 0.5d);
        int[] iArr4 = this.mFingerprintPosition;
        fingerprintViewRect.bottom = (int) (((double) ((((float) (iArr4[1] + iArr4[3])) / 2.0f) * scale)) + (((double) (((float) this.mContext.getResources().getDimensionPixelSize(34472496)) * lcdscale)) * 0.5d) + 0.5d);
        Log.d(TAG, "getFingerprintViewRect: " + fingerprintViewRect);
        return fingerprintViewRect;
    }

    private boolean isFingerprintViewCoverd(Rect fingerprintViewRect, Rect winFrame) {
        return fingerprintViewRect.right > winFrame.left && winFrame.right > fingerprintViewRect.left && fingerprintViewRect.bottom > winFrame.top && winFrame.bottom > fingerprintViewRect.top;
    }

    private boolean isFingerViewTouched(float x, float y) {
        Rect fingerprintViewRect = getFingerprintViewRect();
        return ((float) fingerprintViewRect.left) < x && ((float) fingerprintViewRect.right) > x && ((float) fingerprintViewRect.bottom) > y && ((float) fingerprintViewRect.top) < y;
    }

    public void setHighlightViewAlpha(int brightness) {
        this.mCurrentBrightness = brightness;
        this.mCurrentAlpha = getMaskAlpha(brightness);
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.w(TAG, "mHandler is null");
            return;
        }
        if (handler.hasCallbacks(this.mFingerprintMaskSetAlpha)) {
            this.mHandler.removeCallbacks(this.mFingerprintMaskSetAlpha);
        }
        this.mHandler.postAtFrontOfQueue(this.mFingerprintMaskSetAlpha);
    }

    private class SuspensionButtonCallback implements SuspensionButton.InterfaceCallBack {
        private SuspensionButtonCallback() {
        }

        @Override // com.huawei.server.fingerprint.SuspensionButton.InterfaceCallBack
        public void onButtonViewMoved(float endX, float endY) {
            if (FingerViewController.this.mButtonView != null) {
                FingerViewController.this.mSuspensionButtonParams.x = (int) (endX - (((float) FingerViewController.this.mSuspensionButtonParams.width) * 0.5f));
                FingerViewController.this.mSuspensionButtonParams.y = (int) (endY - (((float) FingerViewController.this.mSuspensionButtonParams.height) * 0.5f));
                Log.d(FingerViewController.TAG, "onButtonViewUpdate,x = " + FingerViewController.this.mSuspensionButtonParams.x + " ,y = " + FingerViewController.this.mSuspensionButtonParams.y);
                FingerViewController.this.mWindowManager.updateViewLayout(FingerViewController.this.mButtonView, FingerViewController.this.mSuspensionButtonParams);
            }
        }

        @Override // com.huawei.server.fingerprint.SuspensionButton.InterfaceCallBack
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
                    Log.w(FingerViewController.TAG, "catch exception");
                }
            }
            Context access$3900 = FingerViewController.this.mContext;
            Flog.bdReport(access$3900, 501, "{PkgName:" + FingerViewController.this.mPkgName + "}");
        }

        @Override // com.huawei.server.fingerprint.SuspensionButton.InterfaceCallBack
        public String getCurrentApp() {
            return FingerViewController.this.mPkgName;
        }

        @Override // com.huawei.server.fingerprint.SuspensionButton.InterfaceCallBack
        public void userActivity() {
            FingerViewController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }

        @Override // com.huawei.server.fingerprint.SuspensionButton.InterfaceCallBack
        public void onConfigurationChanged(Configuration newConfig) {
            FingerViewController.this.mHandler.post(FingerViewController.this.mUpdateButtonViewRunnable);
        }
    }

    private class FingerprintViewCallback implements FingerprintView.ICallBack {
        private FingerprintViewCallback() {
        }

        @Override // huawei.com.android.server.fingerprint.FingerprintView.ICallBack
        public void userActivity() {
            FingerViewController.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }

        @Override // huawei.com.android.server.fingerprint.FingerprintView.ICallBack
        public void onConfigurationChanged(Configuration newConfig) {
        }

        @Override // huawei.com.android.server.fingerprint.FingerprintView.ICallBack
        public void onDrawFinish() {
        }
    }

    private class SingleModeContentCallback implements SingleModeContentObserver.ICallBack {
        private SingleModeContentCallback() {
        }

        @Override // huawei.com.android.server.fingerprint.SingleModeContentObserver.ICallBack
        public void onContentChange() {
            if (((FingerViewController.this.mFingerprintView != null && FingerViewController.this.mFingerprintView.isAttachedToWindow()) || (FingerViewController.this.mLayoutForAlipay != null && FingerViewController.this.mLayoutForAlipay.isAttachedToWindow())) && !Settings.Global.getString(FingerViewController.this.mContext.getContentResolver(), "single_hand_mode").isEmpty()) {
                Settings.Global.putString(FingerViewController.this.mContext.getContentResolver(), "single_hand_mode", "");
            }
        }
    }

    private class RemainTimeCountDown extends CountDownTimer {
        public RemainTimeCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onTick(long millisUntilFinished) {
            int unused = FingerViewController.this.mRemainedSecs = (int) ((((double) millisUntilFinished) / 1000.0d) + 0.5d);
            if (FingerViewController.this.mRemainedSecs <= 0) {
                return;
            }
            if (FingerViewController.this.mFingerView != null && FingerViewController.this.mFingerView.isAttachedToWindow()) {
                FingerViewController fingerViewController = FingerViewController.this;
                fingerViewController.updateHintView(fingerViewController.mContext.getResources().getQuantityString(34406411, FingerViewController.this.mRemainedSecs, Integer.valueOf(FingerViewController.this.mRemainedSecs)));
                FingerViewController.this.mFingerprintView.setContentDescription("");
            } else if (FingerViewController.this.mBackFingerprintView != null && FingerViewController.this.mBackFingerprintView.isAttachedToWindow()) {
                FingerViewController fingerViewController2 = FingerViewController.this;
                fingerViewController2.updateBackFingerprintHintView(fingerViewController2.mContext.getResources().getQuantityString(34406411, FingerViewController.this.mRemainedSecs, Integer.valueOf(FingerViewController.this.mRemainedSecs)));
            }
        }

        public void onFinish() {
            Log.d(FingerViewController.TAG, "RemainTimeCountDown onFinish");
            boolean unused = FingerViewController.this.mIsFingerFrozen = false;
            if (FingerViewController.this.mFingerView != null && FingerViewController.this.mFingerView.isAttachedToWindow()) {
                if (FingerViewController.this.mHasBackFingerprint) {
                    FingerViewController fingerViewController = FingerViewController.this;
                    fingerViewController.updateHintView(fingerViewController.mContext.getString(33685678));
                } else {
                    FingerViewController fingerViewController2 = FingerViewController.this;
                    fingerViewController2.updateHintView(fingerViewController2.mContext.getString(33685690));
                }
                FingerViewController.this.mFingerprintView.setContentDescription(FingerViewController.this.mContext.getString(33686246));
            } else if (FingerViewController.this.mBackFingerprintView != null && FingerViewController.this.mBackFingerprintView.isAttachedToWindow()) {
                FingerViewController fingerViewController3 = FingerViewController.this;
                fingerViewController3.updateBackFingerprintHintView(fingerViewController3.mSubTitle);
            }
            if (FingerViewController.this.mHandler != null) {
                FingerViewController.this.mHandler.post(FingerViewController.this.mRemoveFingerViewRunnable);
            }
            if (FingerViewController.this.mFingerViewChangeCallback != null) {
                FingerViewController.this.mFingerViewChangeCallback.onFingerViewStateChange(0);
            }
        }
    }

    private boolean getBrightnessRangeFromPanelInfo() {
        File file = new File(PANEL_INFO_NODE);
        if (!file.exists()) {
            Log.w(TAG, "getBrightnessRangeFromPanelInfo PANEL_INFO_NODE:" + PANEL_INFO_NODE + " isn't exist");
            return false;
        }
        BufferedReader reader = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String tempString = reader.readLine();
            if (tempString != null) {
                Log.i(TAG, "getBrightnessRangeFromPanelInfo String = " + tempString);
                if (tempString.length() == 0) {
                    Log.e(TAG, "getBrightnessRangeFromPanelInfo error! String is null");
                    reader.close();
                    close(reader, fis);
                    return false;
                }
                String[] stringSplited = tempString.split(",");
                if (stringSplited.length < 2) {
                    Log.e(TAG, "split failed! String = " + tempString);
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
            Log.e(TAG, "getBrightnessRangeFromPanelInfo error! IOException " + e2);
        } catch (Exception e3) {
            Log.e(TAG, "getBrightnessRangeFromPanelInfo error! Exception " + e3);
        } catch (Throwable th) {
            close(null, null);
            throw th;
        }
        close(reader, fis);
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
        int max = -1;
        int min = -1;
        String key = null;
        int i = 0;
        while (i < stringSplited.length) {
            try {
                if (i % 100 == 0) {
                    Log.i(TAG, "do loop in parseBundle4Keyguard, time = " + i);
                }
                key = "blmax:";
                int index = stringSplited[i].indexOf(key);
                if (index != -1) {
                    try {
                        max = Integer.parseInt(stringSplited[i].substring(key.length() + index));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "parsePanelInfo max NumberFormatException");
                    }
                } else {
                    key = "blmin:";
                    int index2 = stringSplited[i].indexOf(key);
                    if (index2 != -1) {
                        try {
                            min = Integer.parseInt(stringSplited[i].substring(key.length() + index2));
                        } catch (NumberFormatException e2) {
                            Log.e(TAG, "parsePanelInfo min NumberFormatException");
                        }
                    }
                }
                i++;
            } catch (NumberFormatException e3) {
                Log.e(TAG, "parsePanelInfo() error! " + key + e3);
                return false;
            }
        }
        if (max == -1 || min == -1) {
            return false;
        }
        Log.i(TAG, "getBrightnessRangeFromPanelInfo success! min = " + min + ", max = " + max);
        this.mNormalizedMaxBrightness = max;
        this.mNormalizedMinBrightness = min;
        return true;
    }

    private File getBrightnessAlphaConfigFile() {
        String lcdname = getLcdPanelName();
        String lcdversion = getVersionFromLCD();
        ArrayList<String> xmlPathList = new ArrayList<>();
        if (!(lcdversion == null || lcdname == null)) {
            xmlPathList.add(String.format("/display/effect/displayengine/%s_%s_%s%s", "udfp", lcdname, lcdversion, ".xml"));
        }
        if (lcdname != null) {
            xmlPathList.add(String.format("/display/effect/displayengine/%s_%s%s", "udfp", lcdname, ".xml"));
        }
        xmlPathList.add(String.format("/display/effect/displayengine/%s%s", "udfp", ".xml"));
        File xmlFile = null;
        int listSize = xmlPathList.size();
        for (int i = 0; i < listSize; i++) {
            xmlFile = HwCfgFilePolicy.getCfgFile(xmlPathList.get(i), 2);
            if (xmlFile != null) {
                Log.i(TAG, "getBrightnessAlphaConfigFile ");
                return xmlFile;
            }
        }
        return xmlFile;
    }

    private void initBrightnessAlphaConfig() throws IOException {
        File xmlFile = getBrightnessAlphaConfigFile();
        if (xmlFile == null) {
            Log.e(TAG, "brightnessAlphaConfigFile: config file is not exist !");
            return;
        }
        FileInputStream inputStream = null;
        try {
            FileInputStream inputStream2 = new FileInputStream(xmlFile);
            if (getBrightnessAlphaConfig(inputStream2)) {
                xmlFile.getAbsolutePath();
            }
            try {
                inputStream2.close();
            } catch (IOException e) {
                Log.e(TAG, "catch IOException when inputStream close");
            }
        } catch (FileNotFoundException e2) {
            Log.e(TAG, "initBrightnessAlphaConfig error! FileNotFoundException");
            if (0 != 0) {
                inputStream.close();
            }
        } catch (Exception e3) {
            Log.e(TAG, "initBrightnessAlphaConfig error! Exception");
            if (0 != 0) {
                inputStream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    Log.e(TAG, "catch IOException when inputStream close");
                }
            }
            throw th;
        }
        Log.i(TAG, "initBrightnessAlphaConfig end .");
    }

    private boolean getBrightnessAlphaConfig(InputStream inStream) {
        boolean configGroupLoadStarted = false;
        boolean loadFinished = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 2) {
                    String name = parser.getName();
                    if (name.equals("BrightnessAndAlphaConfig")) {
                        configGroupLoadStarted = true;
                    } else if (name.equals("Brightness")) {
                        this.mSampleBrightness = FingerprintViewUtils.covertToIntArray(parser.nextText());
                    } else if (name.equals("Alpha")) {
                        this.mSampleAlpha = FingerprintViewUtils.covertToIntArray(parser.nextText());
                    } else if (name.equals("Description")) {
                        parser.nextText();
                    }
                } else if (eventType == 3 && parser.getName().equals("BrightnessAndAlphaConfig") && configGroupLoadStarted) {
                    loadFinished = true;
                    configGroupLoadStarted = false;
                }
                if (loadFinished) {
                    break;
                }
            }
            if (loadFinished) {
                Log.d(TAG, "getBrightnessAlphaConfig success, xml Description.");
                return true;
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "getBrightnessAlphaConfig error! XmlPullParserException");
        } catch (IOException e2) {
            Log.e(TAG, "getBrightnessAlphaConfig error! IOException");
        } catch (NumberFormatException e3) {
            Log.e(TAG, "getBrightnessAlphaConfig error! NumberFormatException");
        } catch (Exception e4) {
            Log.e(TAG, "getBrightnessAlphaConfig error! Exception");
        }
        return true;
    }

    private String getLcdPanelName() {
        IBinder binder = ServiceManager.getService("DisplayEngineExService");
        if (binder == null) {
            Log.i(TAG, "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = IDisplayEngineServiceEx.Stub.asInterface(binder);
        if (mService == null) {
            Log.e(TAG, "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[128];
        try {
            int ret = mService.getEffect(14, 0, name, name.length);
            if (ret != 0) {
                Log.e(TAG, "getLcdPanelName() getEffect failed! ret=" + ret);
                return null;
            }
            String panelName = null;
            try {
                panelName = new String(name, "UTF-8").trim().replace(' ', '_');
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding type!");
            }
            Log.i(TAG, "panel get finished.");
            return panelName;
        } catch (RemoteException e2) {
            Log.e(TAG, "getLcdPanelName() RemoteException " + e2);
            return null;
        }
    }

    private String getVersionFromLCD() {
        IBinder binder = ServiceManager.getService("DisplayEngineExService");
        if (binder == null) {
            Log.w(TAG, "getVersionFromLCD() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = IDisplayEngineServiceEx.Stub.asInterface(binder);
        if (mService == null) {
            Log.w(TAG, "getVersionFromLCD() mService is null!");
            return null;
        }
        byte[] name = new byte[32];
        try {
            int ret = mService.getEffect(14, 3, name, name.length);
            if (ret != 0) {
                Log.e(TAG, "getVersionFromLCD() getEffect failed! ret=" + ret);
                return null;
            }
            String panelVersion = null;
            try {
                String lcdVersion = new String(name, "UTF-8");
                Log.i(TAG, "getVersionFromLCD() lcdVersion=" + lcdVersion);
                String lcdVersion2 = lcdVersion.trim();
                int index = lcdVersion2.indexOf("VER:");
                if (index != -1) {
                    panelVersion = lcdVersion2.substring("VER:".length() + index);
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding type!");
            }
            Log.i(TAG, "getVersionFromLCD() panelVersion=" + panelVersion);
            return panelVersion;
        } catch (RemoteException e2) {
            Log.e(TAG, "getVersionFromLCD() RemoteException " + e2);
            return null;
        }
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public int[] getSampleBrightness() {
        return this.mSampleBrightness;
    }

    public int[] getSampleAlpha() {
        return this.mSampleAlpha;
    }

    /* access modifiers changed from: private */
    public void setFPAuthState(boolean authState) {
        HwPhoneWindowManager policy = (HwPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        if (policy != null) {
            Log.d(TAG_THREAD, "setFPAuthState:" + authState);
            policy.getPhoneWindowManagerEx().setFPAuthState(authState);
        }
    }

    private void getNotchState() {
        this.mDisplayNotchStatus = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "display_notch_status", 0, -2);
        Log.d(TAG, "getNotchState = " + this.mDisplayNotchStatus);
    }

    private void transferNotchRoundCorner(int status) {
        if (this.mDisplayNotchStatus == 1) {
            transferSwitchStatusToSurfaceFlinger(status);
        }
    }

    private void transferSwitchStatusToSurfaceFlinger(int value) {
        StringBuilder sb;
        Parcel dataIn = Parcel.obtain();
        try {
            IBinder sfBinder = ServiceManager.getService("SurfaceFlinger");
            dataIn.writeInt(value);
            if (sfBinder != null && !sfBinder.transact(NOTCH_ROUND_CORNER_CODE, dataIn, null, 1)) {
                Log.d(TAG, "transferSwitchStatusToSurfaceFlinger error!");
            }
            if (value == 0) {
                this.mNotchConerStatusChanged = true;
            } else {
                this.mNotchConerStatusChanged = false;
            }
            sb = new StringBuilder();
        } catch (RemoteException e) {
            Log.e(TAG, "transferSwitchStatusToSurfaceFlinger catch RemoteException");
            if (value == 0) {
                this.mNotchConerStatusChanged = true;
            } else {
                this.mNotchConerStatusChanged = false;
            }
            sb = new StringBuilder();
        } catch (Exception e2) {
            Log.e(TAG, "transferSwitchStatusToSurfaceFlinger catch Exception ");
            if (value == 0) {
                this.mNotchConerStatusChanged = true;
            } else {
                this.mNotchConerStatusChanged = false;
            }
            sb = new StringBuilder();
        } catch (Throwable th) {
            if (value == 0) {
                this.mNotchConerStatusChanged = true;
            } else {
                this.mNotchConerStatusChanged = false;
            }
            Log.d(TAG, "notch coner status change to  = " + value);
            dataIn.recycle();
            throw th;
        }
        sb.append("notch coner status change to  = ");
        sb.append(value);
        Log.d(TAG, sb.toString());
        dataIn.recycle();
    }

    public int getMaskAlpha(int currentLight) {
        int[] iArr;
        int alpha = 0;
        int[] iArr2 = this.mSampleBrightness;
        if (iArr2 == null || (iArr = this.mSampleAlpha) == null || iArr2.length == 0 || iArr.length == 0 || iArr2.length != iArr.length) {
            Log.i(TAG, "get Brightness and Alpha config error, use default config.");
            this.mSampleBrightness = sDefaultSampleBrightness;
            this.mSampleAlpha = sDefaultSampleAlpha;
        }
        int[] iArr3 = this.mSampleBrightness;
        if (currentLight > iArr3[iArr3.length - 1]) {
            Log.d(TAG, "currentLight:" + currentLight);
            return 0;
        }
        int i = 0;
        while (true) {
            int[] iArr4 = this.mSampleBrightness;
            if (i >= iArr4.length) {
                break;
            } else if (currentLight == iArr4[i]) {
                alpha = this.mSampleAlpha[i];
                break;
            } else if (currentLight >= iArr4[i]) {
                i++;
            } else if (i == 0) {
                alpha = this.mSampleAlpha[0];
            } else {
                int i2 = iArr4[i - 1];
                int[] iArr5 = this.mSampleAlpha;
                alpha = queryAlphaImpl(currentLight, i2, iArr5[i - 1], iArr4[i], iArr5[i]);
            }
        }
        if (alpha > this.mSampleAlpha[0] || alpha < 0) {
            alpha = 0;
        }
        Log.d(TAG, "alpha:" + alpha + ",currentLight:" + currentLight);
        return alpha;
    }

    private int queryAlphaImpl(int currLight, int preLevelLight, int preLevelAlpha, int lastLevelLight, int lastLevelAlpha) {
        return (((currLight - preLevelLight) * (lastLevelAlpha - preLevelAlpha)) / (lastLevelLight - preLevelLight)) + preLevelAlpha;
    }

    public void setBiometricServiceReceiver(IBiometricServiceReceiverInternal biometricServiceReceiver) {
        this.mBiometricServiceReceiver = biometricServiceReceiver;
    }

    public void setBiometricRequireConfirmation(boolean isBiometricConfirmation) {
        this.isBiometricRequireConfirmation = isBiometricConfirmation;
    }

    public void setBiometricPrompt(boolean biometricPrompt) {
        this.mIsBiometricPrompt = biometricPrompt;
    }
}
