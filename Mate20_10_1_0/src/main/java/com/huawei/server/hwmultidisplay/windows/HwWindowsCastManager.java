package com.huawei.server.hwmultidisplay.windows;

import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.server.pm.HwResolverManager;
import com.android.server.wm.WindowManagerInternalEx;
import com.huawei.android.app.WallpaperManagerExt;
import com.huawei.android.app.WindowManagerExt;
import com.huawei.android.inputmethod.HwInputMethodManager;
import com.huawei.android.os.PowerManagerEx;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.android.view.DisplayEx;
import com.huawei.android.view.DisplayInfoEx;
import com.huawei.server.UiThreadEx;
import com.huawei.server.hwmultidisplay.HwMultiDisplayUtils;
import com.huawei.server.hwmultidisplay.power.HwMultiDisplayPowerManager;
import com.huawei.server.policy.PhoneWindowManagerEx;
import com.huawei.utils.HwPartResourceUtils;

public class HwWindowsCastManager extends DefaultHwWindowsCastManager {
    private static final String BROADCAST_NOTIFY_WINDOW_CAST_MODE = "com.huawei.hwmultidisplay.action.WINDOW_CAST_MODE";
    private static final Object LOCK = new Object();
    private static final int MSG_ADD_SECURE_VIEW = 1;
    private static final int MSG_REMOVE_SECURE_VIEW = 2;
    private static final String PERMISSION_BROADCAST_WINDOW_CAST_MODE = "com.huawei.hwmultidisplay.permission.WINDOW_CAST_MODE";
    public static final int REMINDER_TYPE_INVALID = -1;
    public static final int REMINDER_TYPE_KEYGUARD_LOCKED = 1;
    public static final int REMINDER_TYPE_SECURE_VIEW = 2;
    private static final String TAG = "HwWindowsCastManager";
    private static final int WAKE_REASON_APPLICATION = 2;
    private static HwWindowsCastManager mSingleInstance = null;
    private final float DEFAULT_HEIGHT = 2340.0f;
    private final int DEFAULT_IMG_SIZE = 72;
    private final float DEFAULT_SCALE = 1.0f;
    private final int DEFAULT_TEXT_SIZE = 16;
    /* access modifiers changed from: private */
    public Context mContext;
    private int mCurrentViewType = -1;
    private float mDensity;
    private boolean mIsNetworkReconnecting = false;
    private boolean mIsScreenLocked = false;
    /* access modifiers changed from: private */
    public PhoneWindowManagerEx mPolicy;
    private float mScale = 1.0f;
    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        /* class com.huawei.server.hwmultidisplay.windows.HwWindowsCastManager.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                KeyguardManager keyguardManager = (KeyguardManager) HwWindowsCastManager.this.mContext.getSystemService("keyguard");
                String action = intent.getAction();
                char c = 65535;
                int hashCode = action.hashCode();
                if (hashCode != -2128145023) {
                    if (hashCode != -1454123155) {
                        if (hashCode == 823795052 && action.equals("android.intent.action.USER_PRESENT")) {
                            c = 2;
                        }
                    } else if (action.equals("android.intent.action.SCREEN_ON")) {
                        c = 1;
                    }
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    c = 0;
                }
                if (c == 0) {
                    HwPCUtils.log(HwWindowsCastManager.TAG, "Receive broadcast: ACTION_SCREEN_OFF");
                    if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
                        HwPCUtils.log(HwWindowsCastManager.TAG, "Keyguard is locked, need show lockview");
                        HwWindowsCastManager.this.sendShowViewMsg(1);
                    }
                } else if (c == 1) {
                    HwPCUtils.log(HwWindowsCastManager.TAG, "Receive broadcast: ACTION_SCREEN_ON");
                    if (!HwWindowsCastManager.this.mPolicy.isKeyguardShowingAndNotOccluded()) {
                        HwPCUtils.log(HwWindowsCastManager.TAG, "Keyguard is not showing or occluded, need hide lockview");
                        HwWindowsCastManager.this.sendHideViewMsg(1);
                    }
                } else if (c == 2) {
                    HwPCUtils.log(HwWindowsCastManager.TAG, "Receive broadcast: ACTION_USER_PRESENT");
                    HwWindowsCastManager.this.sendHideViewMsg(1);
                }
            }
        }
    };
    Handler mUIHandler = new Handler(UiThreadEx.getLooper()) {
        /* class com.huawei.server.hwmultidisplay.windows.HwWindowsCastManager.AnonymousClass1 */

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                HwWindowsCastManager.this.showReminderIfNeeded(msg.arg1);
            } else if (i == 2) {
                HwWindowsCastManager.this.hideReminderIfNeeded(msg.arg1);
            }
        }
    };
    private View mViewForWindowCastMode = null;
    private int mWindowCastModeDisplayId = -1;
    private WindowManagerInternalEx mWindowManagerInternalEx = new WindowManagerInternalEx();

    public void setNetworkReconnectionState(boolean isNetworkReconnecting) {
        this.mIsNetworkReconnecting = isNetworkReconnecting;
    }

    public static HwWindowsCastManager getDefault() {
        if (mSingleInstance == null) {
            synchronized (LOCK) {
                if (mSingleInstance == null) {
                    mSingleInstance = new HwWindowsCastManager();
                }
            }
        }
        return mSingleInstance;
    }

    public void onDisplayAdded(Context context, int displayId) {
        HwPCUtils.log(TAG, "onDisplayAdded " + displayId);
        this.mContext = context;
        this.mWindowCastModeDisplayId = displayId;
        this.mPolicy = new PhoneWindowManagerEx();
        SystemPropertiesEx.set("hw.multi.window.cast.mode", "true");
        HwMultiDisplayUtils.setIsWindowsCastMode(true);
        HwPCUtils.setWindowsCastDisplayId(displayId);
        broadcastWindowsCastMode(true);
        this.mWindowManagerInternalEx.setFocusedDisplay(displayId, false, "enterWindowsCast");
        HwInputMethodManager.restartInputMethodForMultiDisplay();
        IntentFilter unlockFilter = new IntentFilter();
        unlockFilter.addAction("android.intent.action.USER_PRESENT");
        unlockFilter.addAction("android.intent.action.SCREEN_OFF");
        unlockFilter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiver(this.mScreenStateReceiver, unlockFilter);
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (powerManager != null && !powerManager.isInteractive()) {
            HwPCUtils.log(TAG, "wakeup if phone is in power-off state");
            PowerManagerEx.wakeUp(powerManager, SystemClock.uptimeMillis(), 2, "DisplayAdd for Windowscast");
        }
        KeyguardManager keyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguardManager == null || !keyguardManager.isKeyguardLocked()) {
            HwMultiDisplayPowerManager.getDefault().setScreenPowerInner(false, false);
        } else {
            sendShowViewMsg(1);
        }
        updateWindowsCastDisplayinfo();
        HwResolverManager.getInstance().clearFirstOpenFileTypeTags();
    }

    public void onDisplayRemoved(Context context, int displayId) {
        HwPCUtils.log(TAG, "onDisplayRemoved " + displayId);
        this.mContext = context;
        this.mWindowCastModeDisplayId = -1;
        this.mViewForWindowCastMode = null;
        SystemPropertiesEx.set("hw.multi.window.cast.mode", "false");
        hideReminderIfNeeded(this.mCurrentViewType);
        this.mContext.unregisterReceiver(this.mScreenStateReceiver);
        HwMultiDisplayUtils.setIsWindowsCastMode(false);
        broadcastWindowsCastMode(false);
        HwPCUtils.setWindowsCastDisplayId(-1);
        WindowManagerExt.updateFocusWindowFreezed(true);
        HwInputMethodManager.restartInputMethodForMultiDisplay();
        if (this.mIsNetworkReconnecting) {
            HwMultiDisplayPowerManager.getDefault().setScreenPowerInner(true, false);
            PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
            if (pm != null) {
                PowerManagerEx.userActivity(pm, SystemClock.uptimeMillis(), false);
            }
        } else {
            HwMultiDisplayPowerManager.getDefault().lockScreenWhenDisconnected(this.mContext);
            HwMultiDisplayPowerManager.getDefault().setScreenPowerOn(true);
        }
        setNetworkReconnectionState(false);
        HwResolverManager.getInstance().clearFirstOpenFileTypeTags();
    }

    public void sendShowViewMsg(int type) {
        Message msg = Message.obtain();
        msg.what = 1;
        msg.arg1 = type;
        this.mUIHandler.sendMessage(msg);
    }

    public void sendHideViewMsg(int type) {
        Message msg = Message.obtain();
        msg.what = 2;
        msg.arg1 = type;
        this.mUIHandler.sendMessage(msg);
    }

    public void onKeyguardOccludedChangedLw(boolean occluded) {
        HwPCUtils.log(TAG, "onKeyguardOccludedChangedLw occluded：" + occluded);
        KeyguardManager keyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (keyguardManager == null || !keyguardManager.isKeyguardLocked() || occluded) {
            sendHideViewMsg(1);
        } else {
            sendShowViewMsg(1);
        }
    }

    /* access modifiers changed from: private */
    public void showReminderIfNeeded(int type) {
        HwPCUtils.log(TAG, "showReminderIfNeeded, type:" + type);
        if (this.mContext != null) {
            if (type == 1) {
                this.mIsScreenLocked = true;
            }
            Display targetDisplay = getWindowCastModeDisplay();
            if (targetDisplay != null) {
                if (this.mViewForWindowCastMode == null) {
                    Context displayContext = this.mContext.createDisplayContext(targetDisplay);
                    this.mViewForWindowCastMode = LayoutInflater.from(displayContext).inflate(HwPartResourceUtils.getResourceId("window_cast_mode_reminder_secure"), (ViewGroup) null);
                    updateViewType(type);
                    updateViewSize();
                    Drawable background = getBackgroundBlur();
                    if (background != null) {
                        this.mViewForWindowCastMode.setBackgroundDrawable(background);
                    }
                    ((WindowManager) displayContext.getSystemService("window")).addView(this.mViewForWindowCastMode, new WindowManager.LayoutParams(2004));
                    if (type == 2 && !HwMultiDisplayUtils.getInstance().isScreenOnForHwMultiDisplay()) {
                        HwMultiDisplayUtils.getInstance().lightScreenOnForHwMultiDisplay();
                        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
                        if (pm != null) {
                            PowerManagerEx.userActivity(pm, SystemClock.uptimeMillis(), false);
                        }
                    }
                } else {
                    updateViewType(type);
                }
                this.mCurrentViewType = type;
            }
        }
    }

    private void updateViewType(int type) {
        View view = this.mViewForWindowCastMode;
        if (view != null) {
            ImageView reminderImg = (ImageView) view.findViewById(HwPartResourceUtils.getResourceId("reminder_img"));
            TextView reminderText = (TextView) this.mViewForWindowCastMode.findViewById(HwPartResourceUtils.getResourceId("reminder_text"));
            if (type == 1) {
                reminderImg.setImageResource(HwPartResourceUtils.getResourceId("ic_lock_window_cast_mode"));
                reminderText.setText(HwPartResourceUtils.getResourceId("window_cast_mode_reminder_locked"));
                return;
            }
            reminderImg.setImageResource(HwPartResourceUtils.getResourceId("ic_safe_window_cast_mode"));
            reminderText.setText(HwPartResourceUtils.getResourceId("window_cast_mode_reminder_secure_string"));
        }
    }

    private void updateViewSize() {
        if (this.mScale != 1.0f) {
            ImageView reminderImg = (ImageView) this.mViewForWindowCastMode.findViewById(HwPartResourceUtils.getResourceId("reminder_img"));
            ViewGroup.LayoutParams imgViewParams = reminderImg.getLayoutParams();
            float f = this.mScale;
            float f2 = this.mDensity;
            imgViewParams.height = (int) ((f * 72.0f * f2) + 0.5f);
            imgViewParams.width = (int) ((f * 72.0f * f2) + 0.5f);
            reminderImg.setLayoutParams(imgViewParams);
            ((TextView) this.mViewForWindowCastMode.findViewById(HwPartResourceUtils.getResourceId("reminder_text"))).setTextSize(this.mScale * 16.0f);
        }
    }

    private void updateWindowsCastDisplayinfo() {
        Display targetDisplay = getWindowCastModeDisplay();
        if (targetDisplay != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            targetDisplay.getMetrics(displayMetrics);
            HwPCUtils.log(TAG, "Display info:" + displayMetrics);
            if (displayMetrics.heightPixels > displayMetrics.widthPixels) {
                this.mScale = ((float) displayMetrics.heightPixels) / 2340.0f;
            } else {
                this.mScale = ((float) displayMetrics.widthPixels) / 2340.0f;
            }
            this.mDensity = displayMetrics.density;
        }
    }

    /* access modifiers changed from: private */
    public void hideReminderIfNeeded(int type) {
        HwPCUtils.log(TAG, "hideReminderIfNeeded, type:" + type);
        if (type == 1) {
            this.mIsScreenLocked = false;
        }
        if (this.mContext != null && this.mCurrentViewType == type) {
            if (this.mIsScreenLocked) {
                showReminderIfNeeded(1);
                return;
            }
            Display targetDisplay = getWindowCastModeDisplay();
            if (targetDisplay != null) {
                if (this.mViewForWindowCastMode != null) {
                    ((WindowManager) this.mContext.createDisplayContext(targetDisplay).getSystemService("window")).removeView(this.mViewForWindowCastMode);
                }
                this.mViewForWindowCastMode = null;
                this.mCurrentViewType = -1;
            }
        }
    }

    private Drawable getBackgroundBlur() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this.mContext);
        Display display = getWindowCastModeDisplay();
        DisplayInfoEx displayInfo = new DisplayInfoEx();
        if (display == null || !DisplayEx.getDisplayInfo(display, displayInfo)) {
            return null;
        }
        return new BitmapDrawable(this.mContext.getResources(), WallpaperManagerExt.getBlurBitmap(wallpaperManager, new Rect(0, 0, displayInfo.getLogicalWidth(), displayInfo.getLogicalHeight())));
    }

    private Display getWindowCastModeDisplay() {
        DisplayManager dm;
        int i;
        Context context = this.mContext;
        if (context == null || (dm = (DisplayManager) context.getSystemService("display")) == null || (i = this.mWindowCastModeDisplayId) == -1) {
            return null;
        }
        return dm.getDisplay(i);
    }

    private void broadcastWindowsCastMode(boolean isInWindowsCastMode) {
        HwPCUtils.log(TAG, "Broadcast WindowsCast mode:" + isInWindowsCastMode);
        Intent intent = new Intent(BROADCAST_NOTIFY_WINDOW_CAST_MODE);
        intent.putExtra("mode", isInWindowsCastMode);
        this.mContext.sendBroadcast(intent, PERMISSION_BROADCAST_WINDOW_CAST_MODE);
    }
}
