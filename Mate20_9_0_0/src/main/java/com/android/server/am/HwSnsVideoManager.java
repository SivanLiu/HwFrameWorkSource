package com.android.server.am;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.huawei.bd.Reporter;
import java.util.HashSet;
import java.util.Set;

public class HwSnsVideoManager {
    private static final String APP_LOCK = "com.huawei.systemmanager/.applock.password.AuthLaunchLockedAppActivity";
    private static final int DEFAULT_CHECK_MAX_TIME = 1000;
    private static final int DEFAULT_CHECK_MAX_TIMES = 15;
    private static final int DEFAULT_CHECK_MIN_TIME = 750;
    private static final int DEFAULT_CHECK_STEP_TIME = 200;
    private static final int DEFAULT_STAY_MAX_TIME = 10000;
    private static String GAME_DEEP_MODE = "game_deep_nodisturb_mode";
    private static String GAME_DND_MODE = "game_dnd_mode";
    private static int GAME_DND_MODE_CLOSE = 2;
    private static int GAME_DND_MODE_DEFAULT = GAME_DND_MODE_CLOSE;
    private static int GAME_DND_MODE_OPEN = 1;
    static final boolean IS_DEBUG_VERSION;
    private static final int MSG_HANDLE_ADD_VIEW = 0;
    private static final int MSG_HANDLE_REMOVE_VIEW = 1;
    private static final int MSG_HANDLE_SHOW_TOAST = 3;
    private static final int MSG_HANDLE_UPDATE_VIEW = 2;
    private static final String QQ = "com.tencent.mobileqq";
    private static final String TAG = "HwSnsVideoManager";
    private static final String TIM = "com.tencent.tim";
    private static final String WECHAT = "com.tencent.mm";
    private static HwSnsVideoManager mInstance;
    private static Set<String> sDeferLaunchingActivitys = new HashSet();
    private static Set<String> sTransferLaunchingActivitys = new HashSet();
    private static int times = 0;
    private TextView mAnswerView;
    private ImageView mAppView;
    private boolean mAttached = false;
    private TextView mCancelView;
    private Runnable mCheckRunnable = new Runnable() {
        public void run() {
            ActivityStack stack = HwSnsVideoManager.this.mService.getFocusedStack();
            ActivityRecord ar = stack != null ? stack.mResumedActivity : null;
            HwSnsVideoManager.access$708();
            if (HwSnsVideoManager.times >= 15) {
                if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                    Log.d(HwSnsVideoManager.TAG, "check to max times");
                }
                HwSnsVideoManager.times = 0;
                HwSnsVideoManager.this.cancelCheckRunnable();
            } else if (ar == null) {
                if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                    Log.d(HwSnsVideoManager.TAG, "ar in null, check after 200ms");
                }
                HwSnsVideoManager.this.postCheckRunnable(200);
            } else {
                if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                    String str = HwSnsVideoManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("check ");
                    stringBuilder.append(ar.shortComponentName);
                    Log.d(str, stringBuilder.toString());
                }
                if (!(HwSnsVideoManager.APP_LOCK.equals(ar.shortComponentName) || HwSnsVideoManager.sDeferLaunchingActivitys.contains(ar.shortComponentName))) {
                    if (HwSnsVideoManager.this.mService.mWindowManager.isInDisplayFrozen()) {
                        if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                            Log.d(HwSnsVideoManager.TAG, "window is freeze, check after 200ms");
                        }
                        HwSnsVideoManager.this.postCheckRunnable(200);
                    } else {
                        if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                            Log.d(HwSnsVideoManager.TAG, "handleShowToast");
                        }
                        HwSnsVideoManager.times = 0;
                        HwSnsVideoManager.this.showToast();
                    }
                }
            }
        }
    };
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Toast mLastToast;
    private String mPkgName;
    private boolean mReadyToShowActivity = false;
    private Runnable mRemoveRunnable = new Runnable() {
        public void run() {
            HwSnsVideoManager.this.setReadyToShowActivity(false);
            HwSnsVideoManager.this.handleRemoveView();
            Context access$400 = HwSnsVideoManager.this.mContext;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{pkg:");
            stringBuilder.append(HwSnsVideoManager.this.mPkgName);
            stringBuilder.append("}");
            Reporter.e(access$400, 803, stringBuilder.toString());
        }
    };
    private ActivityManagerService mService;
    private SnsFloatNotification mSnsFloatView;
    private TextView mTipsView;
    private IntentSender mVideoCallIntent;
    private final WindowManager mWindowManager;

    private final class SnsVideoManagerHandler extends Handler {
        public SnsVideoManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    HwSnsVideoManager.this.handleAddView();
                    return;
                case 1:
                    HwSnsVideoManager.this.handleRemoveView();
                    return;
                case 2:
                    HwSnsVideoManager.this.handleUpdateView();
                    return;
                case 3:
                    HwSnsVideoManager.this.handleShowToast();
                    return;
                default:
                    return;
            }
        }
    }

    static /* synthetic */ int access$708() {
        int i = times;
        times = i + 1;
        return i;
    }

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3) {
            z = true;
        }
        IS_DEBUG_VERSION = z;
        sDeferLaunchingActivitys.add("com.tencent.mm/.plugin.voip.ui.VideoActivity");
        sDeferLaunchingActivitys.add("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteFull");
        sDeferLaunchingActivitys.add("com.tencent.tim/com.tencent.av.ui.VideoInviteFull");
        sDeferLaunchingActivitys.add("com.tencent.mobileqq/com.tencent.av.ui.VChatActivity");
        sDeferLaunchingActivitys.add("com.tencent.tim/com.tencent.av.ui.VChatActivity");
        sTransferLaunchingActivitys.add("com.tencent.mobileqq/com.tencent.av.ui.VChatActivity");
        sTransferLaunchingActivitys.add("com.tencent.tim/com.tencent.av.ui.VChatActivity");
    }

    private HwSnsVideoManager(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        initHandlerThread();
    }

    public static synchronized HwSnsVideoManager getInstance(Context context) {
        HwSnsVideoManager hwSnsVideoManager;
        synchronized (HwSnsVideoManager.class) {
            if (mInstance == null) {
                mInstance = new HwSnsVideoManager(context);
            }
            hwSnsVideoManager = mInstance;
        }
        return hwSnsVideoManager;
    }

    private void initHandlerThread() {
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mHandler = new SnsVideoManagerHandler(this.mHandlerThread.getLooper());
    }

    private void postRemoveRunnable(int showTime) {
        this.mHandler.removeCallbacks(this.mRemoveRunnable);
        this.mHandler.postDelayed(this.mRemoveRunnable, (long) showTime);
    }

    public void cancelRemoveRunnable() {
        this.mHandler.removeCallbacks(this.mRemoveRunnable);
    }

    private void postCheckRunnable(int showTime) {
        this.mHandler.removeCallbacks(this.mCheckRunnable);
        this.mHandler.postDelayed(this.mCheckRunnable, (long) showTime);
    }

    public void cancelCheckRunnable() {
        this.mHandler.removeCallbacks(this.mCheckRunnable);
    }

    public void showToast() {
        if (this.mLastToast != null) {
            this.mLastToast.cancel();
        }
        this.mHandler.sendEmptyMessage(3);
    }

    private void handleShowToast() {
        this.mLastToast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), 33686074, 0);
        LayoutParams windowParams = this.mLastToast.getWindowParams();
        windowParams.privateFlags |= 16;
        this.mLastToast.show();
    }

    public void addFloatView(String pkgName, IntentSender sendor) {
        if (!this.mAttached) {
            postRemoveRunnable(10000);
            this.mPkgName = pkgName;
            this.mVideoCallIntent = sendor;
            this.mHandler.sendEmptyMessage(0);
        }
    }

    private void handleAddView() {
        if (IS_DEBUG_VERSION) {
            Log.d(TAG, "handleAddView");
        }
        createFloatViewIfNeeded(true);
        this.mSnsFloatView.addNotificationView(inflateView());
        this.mAttached = true;
    }

    public void removeFloatView() {
        if (this.mAttached) {
            cancelRemoveRunnable();
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private void handleRemoveView() {
        if (this.mAttached) {
            if (IS_DEBUG_VERSION) {
                Log.d(TAG, "handleRemoveView");
            }
            cancelRemoveRunnable();
            this.mSnsFloatView.removeNotificationView();
            this.mAttached = false;
        }
    }

    public void updateFloatView(String pkgName, IntentSender sendor) {
        if (this.mAttached) {
            postRemoveRunnable(10000);
            this.mPkgName = pkgName;
            this.mVideoCallIntent = sendor;
            this.mHandler.sendEmptyMessage(2);
        }
    }

    private void handleUpdateView() {
        if (this.mAttached) {
            if (IS_DEBUG_VERSION) {
                Log.d(TAG, "handleUpdateView");
            }
            this.mAppView.setImageResource(pkgToDrawable(this.mPkgName));
            this.mTipsView.setText(pkgToString(this.mPkgName));
        }
    }

    private void createFloatViewIfNeeded(boolean init) {
        if (this.mSnsFloatView == null) {
            this.mSnsFloatView = new SnsFloatNotification(this.mContext);
        }
        if (init) {
            this.mSnsFloatView.initLayout();
        }
    }

    private View inflateView() {
        View view = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(34013297, null);
        initView(view);
        this.mSnsFloatView.addView(view);
        return view;
    }

    private void initView(View view) {
        this.mCancelView = (TextView) view.findViewById(34603064);
        this.mCancelView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                HwSnsVideoManager.this.cancelLaunchActivity();
            }
        });
        this.mAnswerView = (TextView) view.findViewById(34603065);
        this.mAnswerView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                HwSnsVideoManager.this.confirmLaunchActivity();
            }
        });
        this.mAppView = (ImageView) view.findViewById(34603062);
        this.mAppView.setImageResource(pkgToDrawable(this.mPkgName));
        this.mTipsView = (TextView) view.findViewById(34603063);
        this.mTipsView.setText(pkgToString(this.mPkgName));
    }

    private void confirmLaunchActivity() {
        if (IS_DEBUG_VERSION) {
            Log.d(TAG, "confirmLaunchActivity");
        }
        Context context = this.mContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{pkg:");
        stringBuilder.append(this.mPkgName);
        stringBuilder.append("}");
        Reporter.e(context, 800, stringBuilder.toString());
        int checkTime = DEFAULT_CHECK_MIN_TIME;
        if (TIM.equals(this.mPkgName)) {
            checkTime = 1000;
        }
        postCheckRunnable(checkTime);
        setReadyToShowActivity(true);
        handleRemoveView();
        ((PendingIntentRecord) this.mVideoCallIntent.getTarget()).sendInner(0, null, null, null, null, null, null, null, 0, 0, 0, null);
    }

    private void cancelLaunchActivity() {
        if (IS_DEBUG_VERSION) {
            Log.d(TAG, "cancelLaunchActivity");
        }
        Context context = this.mContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{pkg:");
        stringBuilder.append(this.mPkgName);
        stringBuilder.append("}");
        Reporter.e(context, 801, stringBuilder.toString());
        setReadyToShowActivity(false);
        handleRemoveView();
    }

    public void setReadyToShowActivity(boolean isReady) {
        this.mReadyToShowActivity = isReady;
    }

    public boolean getReadyToShowActivity(Intent intent) {
        if (intent.getComponent() != null && sDeferLaunchingActivitys.contains(intent.getComponent().flattenToShortString())) {
            return this.mReadyToShowActivity;
        }
        return true;
    }

    public boolean isAttached() {
        createFloatViewIfNeeded(false);
        return this.mSnsFloatView.isAttached();
    }

    public void setAttached(boolean attached) {
        this.mAttached = attached;
    }

    public String getPkgName() {
        return this.mPkgName;
    }

    public Set<String> getDeferLaunchingActivitys() {
        return sDeferLaunchingActivitys;
    }

    public void setActivityManager(ActivityManagerService service) {
        this.mService = service;
    }

    private int pkgToDrawable(String pkgName) {
        if (pkgName.equals(WECHAT)) {
            return 33751947;
        }
        if (pkgName.equals(QQ)) {
            return 33751945;
        }
        return 33751946;
    }

    private int pkgToString(String pkgName) {
        if (pkgName.equals(WECHAT)) {
            return 33686073;
        }
        if (pkgName.equals(QQ)) {
            return 33686069;
        }
        return 33686070;
    }

    public boolean isGameDndOn() {
        if (Secure.getIntForUser(this.mContext.getContentResolver(), GAME_DND_MODE, GAME_DND_MODE_DEFAULT, -2) == 1 || Secure.getIntForUser(this.mContext.getContentResolver(), GAME_DEEP_MODE, GAME_DND_MODE_DEFAULT, -2) == 1) {
            return true;
        }
        return false;
    }

    public boolean isTransferActivity(Intent intent) {
        if (intent.getComponent() != null && sTransferLaunchingActivitys.contains(intent.getComponent().flattenToShortString())) {
            return true;
        }
        return false;
    }
}
