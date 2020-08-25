package com.android.server.wm;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.huawei.bd.Reporter;
import java.util.HashSet;
import java.util.Set;

public class HwSnsVideoManager {
    private static final int DEFAULT_CHECK_MAX_TIME = 1000;
    private static final int DEFAULT_CHECK_MAX_TIMES = 15;
    private static final int DEFAULT_CHECK_MIN_TIME = 750;
    private static final int DEFAULT_CHECK_STEP_TIME = 200;
    private static final int DEFAULT_STAY_MAX_TIME = 10000;
    private static final String GAME_DEEP_MODE = "game_deep_nodisturb_mode";
    private static final String GAME_DND_MODE = "game_dnd_mode";
    private static int GAME_DND_MODE_CLOSE = 2;
    private static int GAME_DND_MODE_DEFAULT = GAME_DND_MODE_CLOSE;
    private static int GAME_DND_MODE_OPEN = 1;
    /* access modifiers changed from: private */
    public static final boolean IS_DEBUG_VERSION;
    private static final int MSG_HANDLE_ADD_VIEW = 0;
    private static final int MSG_HANDLE_REMOVE_VIEW = 1;
    private static final int MSG_HANDLE_SHOW_TOAST = 3;
    private static final int MSG_HANDLE_UPDATE_VIEW = 2;
    private static final String QQ = "com.tencent.mobileqq";
    private static final String TAG = "HwSnsVideoManager";
    private static final String TIM = "com.tencent.tim";
    private static final String WECHAT = "com.tencent.mm";
    private static HwSnsVideoManager mInstance;
    /* access modifiers changed from: private */
    public static Set<String> sDeferLaunchingActivitys = new HashSet();
    private static Set<String> sTransferLaunchingActivitys = new HashSet();
    /* access modifiers changed from: private */
    public static int times = 0;
    private TextView mAnswerView;
    private ImageView mAppView;
    private boolean mAttached = false;
    private TextView mCancelView;
    private Runnable mCheckRunnable = new Runnable() {
        /* class com.android.server.wm.HwSnsVideoManager.AnonymousClass2 */

        public void run() {
            ActivityStack stack = HwSnsVideoManager.this.mService.mRootActivityContainer.getTopDisplayFocusedStack();
            ActivityRecord ar = stack != null ? stack.mResumedActivity : null;
            HwSnsVideoManager.access$708();
            if (HwSnsVideoManager.times >= 15) {
                if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                    Log.d(HwSnsVideoManager.TAG, "check to max times");
                }
                int unused = HwSnsVideoManager.times = 0;
                HwSnsVideoManager.this.cancelCheckRunnable();
            } else if (ar == null) {
                if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                    Log.d(HwSnsVideoManager.TAG, "ar in null, check after 200ms");
                }
                HwSnsVideoManager.this.postCheckRunnable(200);
            } else {
                if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                    Log.d(HwSnsVideoManager.TAG, "check " + ar.shortComponentName);
                }
                if (!HwActivityStartInterceptor.isAppLockActivity(ar.shortComponentName) && !HwSnsVideoManager.sDeferLaunchingActivitys.contains(ar.shortComponentName)) {
                    if (!HwSnsVideoManager.this.mService.mWindowManager.isInDisplayFrozen()) {
                        if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                            Log.d(HwSnsVideoManager.TAG, "handleShowToast");
                        }
                        int unused2 = HwSnsVideoManager.times = 0;
                        HwSnsVideoManager.this.showToast();
                        return;
                    }
                    if (HwSnsVideoManager.IS_DEBUG_VERSION) {
                        Log.d(HwSnsVideoManager.TAG, "window is freeze, check after 200ms");
                    }
                    HwSnsVideoManager.this.postCheckRunnable(200);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Toast mLastToast;
    /* access modifiers changed from: private */
    public String mPkgName;
    private boolean mReadyToShowActivity = false;
    private Runnable mRemoveRunnable = new Runnable() {
        /* class com.android.server.wm.HwSnsVideoManager.AnonymousClass1 */

        public void run() {
            HwSnsVideoManager.this.setReadyToShowActivity(false);
            HwSnsVideoManager.this.handleRemoveView();
            Context access$400 = HwSnsVideoManager.this.mContext;
            Reporter.e(access$400, 803, "{pkg:" + HwSnsVideoManager.this.mPkgName + "}");
        }
    };
    /* access modifiers changed from: private */
    public ActivityTaskManagerService mService;
    private SnsFloatNotification mSnsFloatView;
    private TextView mTipsView;
    private IntentSender mVideoCallIntent;
    private final WindowManager mWindowManager;

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
        sDeferLaunchingActivitys.add("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity");
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

    private final class SnsVideoManagerHandler extends Handler {
        public SnsVideoManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                HwSnsVideoManager.this.handleAddView();
            } else if (i == 1) {
                HwSnsVideoManager.this.handleRemoveView();
            } else if (i == 2) {
                HwSnsVideoManager.this.handleUpdateView();
            } else if (i == 3) {
                HwSnsVideoManager.this.handleShowToast();
            }
        }
    }

    private void postRemoveRunnable(int showTime) {
        this.mHandler.removeCallbacks(this.mRemoveRunnable);
        this.mHandler.postDelayed(this.mRemoveRunnable, (long) showTime);
    }

    public void cancelRemoveRunnable() {
        this.mHandler.removeCallbacks(this.mRemoveRunnable);
    }

    /* access modifiers changed from: private */
    public void postCheckRunnable(int showTime) {
        this.mHandler.removeCallbacks(this.mCheckRunnable);
        this.mHandler.postDelayed(this.mCheckRunnable, (long) showTime);
    }

    /* access modifiers changed from: private */
    public void cancelCheckRunnable() {
        this.mHandler.removeCallbacks(this.mCheckRunnable);
    }

    public void showToast() {
        Toast toast = this.mLastToast;
        if (toast != null) {
            toast.cancel();
        }
        this.mHandler.sendEmptyMessage(3);
    }

    /* access modifiers changed from: private */
    public void handleShowToast() {
        this.mLastToast = Toast.makeText(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui.Toast", null, null)), 33685713, 0);
        this.mLastToast.getWindowParams().privateFlags |= 16;
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

    /* access modifiers changed from: private */
    public void handleAddView() {
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

    /* access modifiers changed from: private */
    public void handleRemoveView() {
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

    /* access modifiers changed from: private */
    public void handleUpdateView() {
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
        View view = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(34013348, (ViewGroup) null);
        initView(view);
        this.mSnsFloatView.addView(view);
        return view;
    }

    private void initView(View view) {
        this.mCancelView = (TextView) view.findViewById(34603230);
        this.mCancelView.setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wm.HwSnsVideoManager.AnonymousClass3 */

            public void onClick(View v) {
                HwSnsVideoManager.this.cancelLaunchActivity();
            }
        });
        this.mAnswerView = (TextView) view.findViewById(34603231);
        this.mAnswerView.setOnClickListener(new View.OnClickListener() {
            /* class com.android.server.wm.HwSnsVideoManager.AnonymousClass4 */

            public void onClick(View v) {
                HwSnsVideoManager.this.confirmLaunchActivity();
            }
        });
        this.mAppView = (ImageView) view.findViewById(34603228);
        this.mAppView.setImageResource(pkgToDrawable(this.mPkgName));
        this.mTipsView = (TextView) view.findViewById(34603229);
        this.mTipsView.setText(pkgToString(this.mPkgName));
    }

    /* access modifiers changed from: private */
    public void confirmLaunchActivity() {
        if (IS_DEBUG_VERSION) {
            Log.d(TAG, "confirmLaunchActivity");
        }
        Context context = this.mContext;
        Reporter.e(context, 800, "{pkg:" + this.mPkgName + "}");
        int checkTime = DEFAULT_CHECK_MIN_TIME;
        if (TIM.equals(this.mPkgName)) {
            checkTime = 1000;
        }
        postCheckRunnable(checkTime);
        setReadyToShowActivity(true);
        handleRemoveView();
        this.mVideoCallIntent.getTarget().sendInner(0, (Intent) null, (String) null, (IBinder) null, (IIntentReceiver) null, (String) null, (IBinder) null, (String) null, 0, 0, 0, (Bundle) null);
    }

    /* access modifiers changed from: private */
    public void cancelLaunchActivity() {
        if (IS_DEBUG_VERSION) {
            Log.d(TAG, "cancelLaunchActivity");
        }
        Context context = this.mContext;
        Reporter.e(context, 801, "{pkg:" + this.mPkgName + "}");
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

    public void setActivityManager(ActivityTaskManagerService service) {
        this.mService = service;
    }

    private int pkgToDrawable(String pkgName) {
        if (pkgName.equals(WECHAT)) {
            return 33751962;
        }
        if (pkgName.equals(QQ)) {
            return 33751960;
        }
        return 33751961;
    }

    private int pkgToString(String pkgName) {
        if (pkgName.equals(WECHAT)) {
            return 33685705;
        }
        if (pkgName.equals(QQ)) {
            return 33685695;
        }
        return 33685697;
    }

    public boolean isGameDndOn() {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), GAME_DND_MODE, GAME_DND_MODE_DEFAULT, -2) == 1 || Settings.Secure.getIntForUser(this.mContext.getContentResolver(), GAME_DEEP_MODE, GAME_DND_MODE_DEFAULT, -2) == 1) {
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
