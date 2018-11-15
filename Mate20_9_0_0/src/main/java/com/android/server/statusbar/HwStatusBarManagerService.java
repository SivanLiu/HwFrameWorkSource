package com.android.server.statusbar;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBar;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.wm.WindowManagerService;

public class HwStatusBarManagerService extends StatusBarManagerService {
    private static final boolean DEBUG = false;
    private static final String TAG = "HwStatusBarManagerService";
    volatile IStatusBar mBar;
    private Context mContext;
    private HwNotificationDelegate mHwNotificationDelegate;
    Object mLock = new Object();
    private boolean mStatusBarExpanded;

    public interface HwNotificationDelegate {
        void onNotificationResidentClear(String str, String str2, int i, int i2);
    }

    private void enforceStatusBar() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", "StatusBarManagerService");
    }

    public HwStatusBarManagerService(Context context, WindowManagerService windowManager) {
        super(context, windowManager);
        this.mContext = context;
    }

    public void setHwNotificationDelegate(HwNotificationDelegate delegate) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setHwNotificationCallbacks delegate = ");
        stringBuilder.append(delegate);
        Slog.d(str, stringBuilder.toString());
        this.mHwNotificationDelegate = delegate;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == 101) {
            data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
            transactToStatusBar(101, "resentapp", "resentapp", data.readInt());
            reply.writeNoException();
            return true;
        } else if (code != 121) {
            switch (code) {
                case 105:
                    data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
                    String packName = data.readString();
                    String tag = data.readString();
                    int id = data.readInt();
                    int usrid = data.readInt();
                    if (this.mHwNotificationDelegate != null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onNotificationResidentClear packName = ");
                        stringBuilder.append(packName);
                        stringBuilder.append(" usrid:");
                        stringBuilder.append(usrid);
                        Slog.d(str, stringBuilder.toString());
                        this.mHwNotificationDelegate.onNotificationResidentClear(packName, tag, id, usrid);
                    }
                    reply.writeNoException();
                    return true;
                case 106:
                    data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
                    transactToStatusBar(106, "SystemUIColor", data.readInt(), data.readInt(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    return true;
                case 107:
                    data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
                    toggleSplitScreenByLineGesture(107, "toggleSplitScreenByLineGesture", data.readInt(), data.readInt());
                    reply.writeNoException();
                    return true;
                case HwAPPQoEUtils.MSG_APP_STATE_UNKNOW /*108*/:
                    data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
                    transactRecentCall(code, "toggleRecentApps");
                    reply.writeNoException();
                    return true;
                case 109:
                    data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
                    transactRecentCall(code, "preloadRecentApps");
                    reply.writeNoException();
                    return true;
                case 110:
                    data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
                    transactRecentCall(code, "cancelPreloadRecentApps");
                    reply.writeNoException();
                    return true;
                case HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START /*111*/:
                    data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
                    transactToStatusBar(HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START, "unlockScreenPinningTest", null, 0);
                    reply.writeNoException();
                    Slog.d(TAG, "Transact unlockScreenPinningTest to status bar");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        } else {
            data.enforceInterface("com.android.internal.statusbar.IStatusBarService");
            notchTransactToStatusBarStatus(121, "notchTransactToStatusBarStatus", data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt());
            reply.writeNoException();
            return true;
        }
    }

    private void transactToStatusBar(int code, String transactName, String paramName, int paramValue) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", "StatusBarManagerService");
        synchronized (this.mLock) {
            this.mBar = getStatusBar();
            if (this.mBar != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    IBinder statusBarBinder = this.mBar.asBinder();
                    if (statusBarBinder != null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Transact:");
                        stringBuilder.append(transactName);
                        stringBuilder.append(" to status bar");
                        Slog.d(str, stringBuilder.toString());
                        data.writeInterfaceToken("com.android.internal.statusbar.IStatusBar");
                        if (paramName != null) {
                            data.writeInt(paramValue);
                        }
                        statusBarBinder.transact(code, data, reply, 0);
                    }
                    reply.recycle();
                } catch (RemoteException localRemoteException) {
                    try {
                        localRemoteException.printStackTrace();
                        reply.recycle();
                    } catch (Throwable th) {
                        reply.recycle();
                        data.recycle();
                    }
                }
                data.recycle();
            }
        }
    }

    private void transactToStatusBar(int code, String transactName, int isEmuiStyle, int statusbarColor, int navigationBarColor, int isEmuiLightStyle) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", "StatusBarManagerService");
        synchronized (this.mLock) {
            this.mBar = getStatusBar();
            if (this.mBar != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    IBinder statusBarBinder = this.mBar.asBinder();
                    if (statusBarBinder != null) {
                        data.writeInterfaceToken("com.android.internal.statusbar.IStatusBar");
                        data.writeInt(isEmuiStyle);
                        data.writeInt(statusbarColor);
                        data.writeInt(navigationBarColor);
                        data.writeInt(isEmuiLightStyle);
                        statusBarBinder.transact(code, data, reply, 0);
                    }
                    reply.recycle();
                } catch (RemoteException localRemoteException) {
                    try {
                        localRemoteException.printStackTrace();
                        reply.recycle();
                    } catch (Throwable th) {
                        reply.recycle();
                        data.recycle();
                    }
                }
                data.recycle();
            }
        }
    }

    public void expandNotificationsPanel() {
        super.expandNotificationsPanel();
        this.mStatusBarExpanded = true;
    }

    public void collapsePanels() {
        super.collapsePanels();
        this.mStatusBarExpanded = false;
    }

    public boolean statusBarExpanded() {
        return this.mStatusBarExpanded;
    }

    public void onPanelHidden() throws RemoteException {
        super.onPanelHidden();
        reportToAware(90015);
    }

    public void onPanelRevealed(boolean clearNotificationEffects, int numItems) {
        super.onPanelRevealed(clearNotificationEffects, numItems);
        reportToAware(20015);
    }

    private void reportToAware(int event) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager == null) {
            Slog.w(TAG, "iAware is not started");
            return;
        }
        int resid = ResourceType.getReousrceId(ResourceType.RESOURCE_STATUS_BAR);
        if (resManager.isResourceNeeded(resid)) {
            Bundle bundle = new Bundle();
            bundle.putInt("eventid", event);
            CollectData data = new CollectData(resid, System.currentTimeMillis(), bundle);
            long id = Binder.clearCallingIdentity();
            resManager.reportData(data);
            Binder.restoreCallingIdentity(id);
        }
    }

    public void toggleSplitScreenByLineGesture(int code, String transactName, int param1, int param2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", "StatusBarManagerService");
        synchronized (this.mLock) {
            this.mBar = getStatusBar();
            if (this.mBar != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    IBinder statusBarBinder = this.mBar.asBinder();
                    if (statusBarBinder != null) {
                        data.writeInterfaceToken("com.android.internal.statusbar.IStatusBar");
                        data.writeInt(param1);
                        data.writeInt(param2);
                        statusBarBinder.transact(code, data, reply, 0);
                    }
                    reply.recycle();
                } catch (RemoteException localRemoteException) {
                    try {
                        localRemoteException.printStackTrace();
                        reply.recycle();
                    } catch (Throwable th) {
                        reply.recycle();
                        data.recycle();
                    }
                }
                data.recycle();
            }
        }
    }

    private void transactRecentCall(int code, String transactName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", "StatusBarManagerService");
        synchronized (this.mLock) {
            this.mBar = getStatusBar();
            if (this.mBar != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    IBinder statusBarBinder = this.mBar.asBinder();
                    if (statusBarBinder != null) {
                        data.writeInterfaceToken("com.android.internal.statusbar.IStatusBar");
                        statusBarBinder.transact(code, data, reply, 0);
                    }
                    reply.recycle();
                } catch (RemoteException localRemoteException) {
                    try {
                        localRemoteException.printStackTrace();
                        reply.recycle();
                    } catch (Throwable th) {
                        reply.recycle();
                        data.recycle();
                    }
                }
                data.recycle();
            }
        }
    }

    private void notchTransactToStatusBarStatus(int code, String transactName, int isEmuiStyle, int statusbarColor, int navigationBarColor, int isEmuiLightStyle, int statusbarStateFlag, int barVisibility) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", "StatusBarManagerService");
        synchronized (this.mLock) {
            this.mBar = getStatusBar();
            if (this.mBar != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    IBinder statusBarBinder = this.mBar.asBinder();
                    if (statusBarBinder != null) {
                        data.writeInterfaceToken("com.android.internal.statusbar.IStatusBar");
                        data.writeInt(isEmuiStyle);
                        data.writeInt(statusbarColor);
                        data.writeInt(navigationBarColor);
                        data.writeInt(isEmuiLightStyle);
                        data.writeInt(statusbarStateFlag);
                        data.writeInt(barVisibility);
                        statusBarBinder.transact(code, data, reply, 0);
                    }
                    reply.recycle();
                } catch (RemoteException e) {
                    try {
                        Slog.d(TAG, "RemoteException happen in the method notchTransactToStatusBarStatus!");
                        reply.recycle();
                    } catch (Throwable th) {
                        reply.recycle();
                        data.recycle();
                    }
                }
                data.recycle();
            }
        }
    }
}
