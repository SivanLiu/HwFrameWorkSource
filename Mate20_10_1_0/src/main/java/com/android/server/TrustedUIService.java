package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.hardware.display.HwFoldScreenState;
import android.os.Binder;
import android.os.Handler;
import android.os.ITrustedUIService;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.pm.PackageManagerService;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;

public class TrustedUIService extends ITrustedUIService.Stub {
    public static final String HW_FOLD_DISPLAY_MODE = "hw_fold_display_mode";
    public static final String HW_FOLD_SCRREN_STATE = "hw_fold_screen_state";
    private static final String PHONE_OUTGOING_ACTION = "android.intent.action.NEW_OUTGOING_CALL";
    private static final String PHONE_STATE_ACTION = "android.intent.action.PHONE_STATE";
    private static final String TAG = "TrustedUIService";
    public static final int TUI_FOLD_FULL = 1;
    public static final int TUI_FOLD_SUB = 3;
    public static final int TUI_POLL_FOLD = 26;
    /* access modifiers changed from: private */
    public static boolean mTUIStatus = false;
    int curDisplayMode = -1;
    int curFoldableState = -1;
    private HwFoldScreenManagerInternal fsmInternal;
    private boolean isFoldableScreen = false;
    /* access modifiers changed from: private */
    public final Context mContext;
    private TUIEventListener mListener;
    private final BroadcastReceiver mReceiver;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager;
    int[] screenInfo = new int[10];
    Rect subDisplayRect = HwFoldScreenState.getScreenPhysicalRect(3);

    private native int nativeSendTUICmd(int i, int i2, int[] iArr);

    private native void nativeSendTUIExitCmd();

    private native void nativeTUILibraryDeInit();

    private native boolean nativeTUILibraryInit();

    public void getScreenSize() {
        int i = this.mContext.getResources().getDisplayMetrics().densityDpi;
        float f = this.mContext.getResources().getDisplayMetrics().scaledDensity;
        float f2 = this.mContext.getResources().getDisplayMetrics().density;
        float xdpi = this.mContext.getResources().getDisplayMetrics().xdpi;
        float ydpi = this.mContext.getResources().getDisplayMetrics().ydpi;
        int bottomHeight = 0;
        int resourceId = this.mContext.getResources().getIdentifier("navigation_bar_height", "dimen", PackageManagerService.PLATFORM_PACKAGE_NAME);
        if (resourceId > 0) {
            bottomHeight = this.mContext.getResources().getDimensionPixelSize(resourceId);
        }
        int width = this.mContext.getResources().getDisplayMetrics().widthPixels;
        int height = this.mContext.getResources().getDisplayMetrics().heightPixels + bottomHeight;
        this.curFoldableState = getFoldState();
        this.curDisplayMode = getScreenState();
        int[] iArr = this.screenInfo;
        iArr[0] = (int) ((((float) width) / xdpi) * (((float) width) / xdpi));
        iArr[1] = (int) ((((float) height) / ydpi) * (((float) width) / xdpi));
        if (this.curDisplayMode == 3) {
            iArr[2] = this.subDisplayRect.width();
        } else {
            iArr[2] = width;
        }
        int[] iArr2 = this.screenInfo;
        iArr2[3] = height;
        iArr2[4] = this.curFoldableState;
        iArr2[5] = this.curDisplayMode;
    }

    public int getFoldState() {
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal;
        this.isFoldableScreen = !SystemProperties.get("ro.config.hw_fold_disp").isEmpty();
        if (this.isFoldableScreen) {
            this.fsmInternal = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
        }
        if (!this.isFoldableScreen || (hwFoldScreenManagerInternal = this.fsmInternal) == null) {
            Log.d(TAG, "This device do not support fold screen !\n");
            return -1;
        }
        this.curFoldableState = hwFoldScreenManagerInternal.getFoldableState();
        return this.curFoldableState;
    }

    public int getScreenState() {
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal;
        if (!this.isFoldableScreen || (hwFoldScreenManagerInternal = this.fsmInternal) == null) {
            Log.d(TAG, "This device do not support fold screen !\n");
            return -1;
        }
        this.curDisplayMode = hwFoldScreenManagerInternal.getDisplayMode();
        return this.curDisplayMode;
    }

    public TrustedUIService(Context context) {
        this.mContext = context;
        this.mListener = new TUIEventListener(this, context);
        new Thread(this.mListener, TUIEventListener.class.getName()).start();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PHONE_STATE_ACTION);
        filter.addAction(PHONE_OUTGOING_ACTION);
        this.mReceiver = new BroadcastReceiver() {
            /* class com.android.server.TrustedUIService.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TrustedUIService.TAG, " Broadcast Receiver: " + action);
                if (action != null && !action.equals(TrustedUIService.PHONE_OUTGOING_ACTION)) {
                    TelephonyManager unused = TrustedUIService.this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
                    if (TrustedUIService.this.mTelephonyManager.getCallState() == 1) {
                        Log.d(TrustedUIService.TAG, "Phone incoming status action received, mTUIStatus: " + TrustedUIService.mTUIStatus);
                        if (TrustedUIService.mTUIStatus) {
                            boolean unused2 = TrustedUIService.mTUIStatus = false;
                            TrustedUIService.this.sendTUIExitCmd();
                        }
                    }
                }
            }
        };
        this.mContext.registerReceiver(this.mReceiver, filter);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(HW_FOLD_DISPLAY_MODE), true, new ContentObserver(new Handler()) {
            /* class com.android.server.TrustedUIService.AnonymousClass2 */

            public void onChange(boolean selfChange) {
                Settings.Secure.getIntForUser(TrustedUIService.this.mContext.getContentResolver(), TrustedUIService.HW_FOLD_DISPLAY_MODE, 1, -2);
                TrustedUIService.this.getScreenSize();
                TrustedUIService trustedUIService = TrustedUIService.this;
                trustedUIService.sendTUICmd(26, 0, trustedUIService.screenInfo);
                if (TrustedUIService.mTUIStatus) {
                    TrustedUIService.this.sendTUIExitCmd();
                }
            }
        }, -1);
    }

    public void setTrustedUIStatus(boolean status) {
        Log.d(TAG, " setTrustedUIStatus: " + status);
        mTUIStatus = status;
    }

    public boolean getTrustedUIStatus() {
        Log.d(TAG, " getTrustedUIStatus: " + mTUIStatus);
        if (Binder.getCallingUid() == 1000) {
            return mTUIStatus;
        }
        throw new SecurityException("getTrustedUIStatus should only be called by TrustedUIService");
    }

    public void sendTUIExitCmd() {
        if (Binder.getCallingUid() == 1000) {
            nativeSendTUIExitCmd();
            return;
        }
        throw new SecurityException("sendTUIExitCmd should only be called by TrustedUIService");
    }

    public int sendTUICmd(int eventType, int value, int[] screenInfo2) {
        if (Binder.getCallingUid() == 1000) {
            if ((eventType == 26 && value == 0 && screenInfo2 != null) ? false : true) {
                Log.d(TAG, "invalid parmaters for jni");
                return -1;
            }
            int ret = nativeSendTUICmd(eventType, value, screenInfo2);
            Log.d(TAG, " sendTUICmd: eventType=" + eventType + " value=" + value + " ret=" + ret);
            return ret;
        }
        throw new SecurityException("sendTUICmd should only be called by TrustedUIService");
    }

    public boolean TUIServiceLibraryInit() {
        return nativeTUILibraryInit();
    }
}
