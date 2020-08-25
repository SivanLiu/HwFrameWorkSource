package com.android.server.imm;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.view.inputmethod.CursorAnchorInfo;
import com.android.internal.annotations.GuardedBy;
import com.android.server.gesture.GestureNavConst;
import com.huawei.android.inputmethod.IHwInputContentListener;
import com.huawei.android.inputmethod.IHwInputMethodListener;
import java.lang.reflect.InvocationTargetException;

public final class HwInputMethodManagerServiceEx implements IHwInputMethodManagerServiceEx {
    static final String TAG = "HwInputMethodManagerServiceEx";
    private IHwInputContentListener mHwInputContentListener;
    private IHwInputMethodListener mHwInputMethodListener;
    IHwInputMethodManagerInner mIImsInner = null;

    public HwInputMethodManagerServiceEx(IHwInputMethodManagerInner iims, Context context) {
        this.mIImsInner = iims;
    }

    public boolean isTriNavigationBar(Context context) {
        boolean isEnableNavBar = Settings.System.getIntForUser(context.getContentResolver(), "enable_navbar", getNaviBarEnabledDefValue(), -2) != 0;
        boolean isGestureNavigation = Settings.Secure.getIntForUser(context.getContentResolver(), GestureNavConst.KEY_SECURE_GESTURE_NAVIGATION, 0, -2) != 0;
        Slog.i(TAG, "--- show navigation bar status: isEnableNavBar = " + isEnableNavBar + " ,isGestureNavigation = " + isGestureNavigation);
        if (!isEnableNavBar) {
            return false;
        }
        return !isGestureNavigation;
    }

    @GuardedBy({"mHwInputMethodListener"})
    public void registerInputMethodListener(IHwInputMethodListener listener) {
        this.mHwInputMethodListener = listener;
    }

    @GuardedBy({"mHwInputMethodListener"})
    public void unregisterInputMethodListener() {
        this.mHwInputMethodListener = null;
    }

    @GuardedBy({"mHwInputMethodListener"})
    public void onStartInput() {
        IHwInputMethodListener iHwInputMethodListener = this.mHwInputMethodListener;
        if (iHwInputMethodListener != null) {
            try {
                iHwInputMethodListener.onStartInput();
            } catch (RemoteException e) {
                Slog.e(TAG, "onInputStart RemoteException");
            }
        }
    }

    @GuardedBy({"mHwInputMethodListener"})
    public void onFinishInput() {
        IHwInputMethodListener iHwInputMethodListener = this.mHwInputMethodListener;
        if (iHwInputMethodListener != null) {
            try {
                iHwInputMethodListener.onFinishInput();
            } catch (RemoteException e) {
                Slog.e(TAG, "onFinishInput RemoteException");
            }
        }
    }

    @GuardedBy({"mHwInputMethodListener"})
    public void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {
        IHwInputMethodListener iHwInputMethodListener = this.mHwInputMethodListener;
        if (iHwInputMethodListener != null) {
            try {
                iHwInputMethodListener.onUpdateCursorAnchorInfo(cursorAnchorInfo);
            } catch (RemoteException e) {
                Slog.e(TAG, "onUpdateCursorAnchorInfo RemoteException");
            }
        }
    }

    @GuardedBy({"mHwInputContentListener"})
    public void registerInputContentListener(IHwInputContentListener listener) {
        Slog.d(TAG, "registerInputContentListener");
        this.mHwInputContentListener = listener;
    }

    @GuardedBy({"mHwInputContentListener"})
    public void unregisterInputContentListener() {
        Slog.d(TAG, "unregisterInputContentListener");
        this.mHwInputContentListener = null;
    }

    @GuardedBy({"mHwInputContentListener"})
    public void onReceivedInputContent(String content) {
        IHwInputContentListener iHwInputContentListener = this.mHwInputContentListener;
        if (iHwInputContentListener != null) {
            try {
                iHwInputContentListener.onReceivedInputContent(content);
            } catch (RemoteException e) {
                Slog.e(TAG, "onReceivedInputContent RemoteException");
            }
        } else {
            Slog.w(TAG, "null input content listener");
        }
    }

    @GuardedBy({"mHwInputMethodListener"})
    public void onShowInputRequested() {
        IHwInputMethodListener iHwInputMethodListener = this.mHwInputMethodListener;
        if (iHwInputMethodListener != null) {
            try {
                iHwInputMethodListener.onShowInputRequested();
            } catch (RemoteException e) {
                Slog.e(TAG, "onShowInputRequested RemoteException");
            }
        }
    }

    @GuardedBy({"mHwInputMethodListener"})
    public void onContentChanged(String text) {
        IHwInputMethodListener iHwInputMethodListener = this.mHwInputMethodListener;
        if (iHwInputMethodListener != null) {
            try {
                iHwInputMethodListener.onContentChanged(text);
            } catch (RemoteException e) {
                Slog.e(TAG, "onContentChanged RemoteException");
            }
        }
    }

    @GuardedBy({"mHwInputContentListener"})
    public void onReceivedComposingText(String content) {
        IHwInputContentListener iHwInputContentListener = this.mHwInputContentListener;
        if (iHwInputContentListener != null) {
            try {
                iHwInputContentListener.onReceivedComposingText(content);
            } catch (RemoteException e) {
                Slog.e(TAG, "onReceivedComposingText RemoteException");
            }
        } else {
            Slog.w(TAG, "null input content listener");
        }
    }

    private int getNaviBarEnabledDefValue() {
        int defValue;
        boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
        int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
        if (!FRONT_FINGERPRINT_NAVIGATION) {
            defValue = 1;
        } else {
            boolean isTrikeyExist = isTrikeyExist();
            if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1 && isTrikeyExist) {
                defValue = 0;
            } else if (SystemProperties.get("ro.config.hw_optb", "0").equals("156")) {
                defValue = 0;
            } else {
                defValue = 1;
            }
        }
        Slog.i(TAG, "NaviBar defValue = " + defValue);
        return defValue;
    }

    private boolean isTrikeyExist() {
        try {
            Class clazz = Class.forName("huawei.android.os.HwGeneralManager");
            return ((Boolean) clazz.getDeclaredMethod("isSupportTrikey", new Class[0]).invoke(clazz.getDeclaredMethod("getInstance", new Class[0]).invoke(clazz, new Object[0]), new Object[0])).booleanValue();
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | NullPointerException | InvocationTargetException e) {
            Slog.e(TAG, "isTrikeyExist, reflect method handle, and has exception: " + e);
            return false;
        } catch (Exception ex) {
            Slog.e(TAG, "isTrikeyExist, other exception: " + ex);
            return false;
        }
    }
}
