package com.android.server.statusbar;

import android.app.ActivityThread;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.notification.NotificationDelegate;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.policy.GlobalActionsProvider.GlobalActionsListener;
import com.android.server.power.IHwShutdownThread;
import com.android.server.power.ShutdownThread;
import com.android.server.wm.WindowManagerService;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StatusBarManagerService extends Stub {
    private static final boolean SPEW = false;
    private static final String TAG = "StatusBarManagerService";
    private volatile IStatusBar mBar;
    private final Context mContext;
    private int mCurrentUserId;
    private final ArrayList<DisableRecord> mDisableRecords = new ArrayList();
    private int mDisabled1 = 0;
    private int mDisabled2 = 0;
    private final Rect mDockedStackBounds = new Rect();
    private int mDockedStackSysUiVisibility;
    private final Rect mFullscreenStackBounds = new Rect();
    private int mFullscreenStackSysUiVisibility;
    private GlobalActionsListener mGlobalActionListener;
    private final GlobalActionsProvider mGlobalActionsProvider = new GlobalActionsProvider() {
        public boolean isGlobalActionsDisabled() {
            return (StatusBarManagerService.this.mDisabled2 & 8) != 0;
        }

        public void setGlobalActionsListener(GlobalActionsListener listener) {
            StatusBarManagerService.this.mGlobalActionListener = listener;
            StatusBarManagerService.this.mGlobalActionListener.onGlobalActionsAvailableChanged(StatusBarManagerService.this.mBar != null);
        }

        public void showGlobalActions() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showGlobalActionsMenu();
                } catch (RemoteException e) {
                }
            }
        }
    };
    private Handler mHandler = new Handler();
    private ArrayMap<String, StatusBarIcon> mIcons = new ArrayMap();
    private int mImeBackDisposition;
    private IBinder mImeToken = null;
    private int mImeWindowVis = 0;
    private final StatusBarManagerInternal mInternalService = new StatusBarManagerInternal() {
        private boolean mNotificationLightOn;

        public void setNotificationDelegate(NotificationDelegate delegate) {
            StatusBarManagerService.this.mNotificationDelegate = delegate;
        }

        public void showScreenPinningRequest(int taskId) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showScreenPinningRequest(taskId);
                } catch (RemoteException e) {
                }
            }
        }

        public void showAssistDisclosure() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showAssistDisclosure();
                } catch (RemoteException e) {
                }
            }
        }

        public void startAssist(Bundle args) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.startAssist(args);
                } catch (RemoteException e) {
                }
            }
        }

        public void onCameraLaunchGestureDetected(int source) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.onCameraLaunchGestureDetected(source);
                } catch (RemoteException e) {
                }
            }
        }

        public void topAppWindowChanged(boolean menuVisible) {
            StatusBarManagerService.this.topAppWindowChanged(menuVisible);
        }

        public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenBounds, Rect dockedBounds, String cause) {
            StatusBarManagerService.this.setSystemUiVisibility(vis, fullscreenStackVis, dockedStackVis, mask, fullscreenBounds, dockedBounds, cause);
        }

        public void toggleSplitScreen() {
            StatusBarManagerService.this.enforceStatusBarService();
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.toggleSplitScreen();
                } catch (RemoteException e) {
                }
            }
        }

        public void appTransitionFinished() {
            StatusBarManagerService.this.enforceStatusBarService();
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionFinished();
                } catch (RemoteException e) {
                }
            }
        }

        public void toggleRecentApps() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.toggleRecentApps();
                } catch (RemoteException e) {
                }
            }
        }

        public void setCurrentUser(int newUserId) {
            StatusBarManagerService.this.mCurrentUserId = newUserId;
        }

        public void preloadRecentApps() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.preloadRecentApps();
                } catch (RemoteException e) {
                }
            }
        }

        public void cancelPreloadRecentApps() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.cancelPreloadRecentApps();
                } catch (RemoteException e) {
                }
            }
        }

        public void showRecentApps(boolean triggeredFromAltTab) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showRecentApps(triggeredFromAltTab);
                } catch (RemoteException e) {
                }
            }
        }

        public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.hideRecentApps(triggeredFromAltTab, triggeredFromHomeKey);
                } catch (RemoteException e) {
                }
            }
        }

        public void dismissKeyboardShortcutsMenu() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.dismissKeyboardShortcutsMenu();
                } catch (RemoteException e) {
                }
            }
        }

        public void toggleKeyboardShortcutsMenu(int deviceId) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.toggleKeyboardShortcutsMenu(deviceId);
                } catch (RemoteException e) {
                }
            }
        }

        public void showChargingAnimation(int batteryLevel) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showWirelessChargingAnimation(batteryLevel);
                } catch (RemoteException e) {
                }
            }
        }

        public void showPictureInPictureMenu() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showPictureInPictureMenu();
                } catch (RemoteException e) {
                }
            }
        }

        public void setWindowState(int window, int state) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.setWindowState(window, state);
                } catch (RemoteException e) {
                }
            }
        }

        public void appTransitionPending() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionPending();
                } catch (RemoteException e) {
                }
            }
        }

        public void appTransitionCancelled() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionCancelled();
                } catch (RemoteException e) {
                }
            }
        }

        public void appTransitionStarting(long statusBarAnimationsStartTime, long statusBarAnimationsDuration) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionStarting(statusBarAnimationsStartTime, statusBarAnimationsDuration);
                } catch (RemoteException e) {
                }
            }
        }

        public void setTopAppHidesStatusBar(boolean hidesStatusBar) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.setTopAppHidesStatusBar(hidesStatusBar);
                } catch (RemoteException e) {
                }
            }
        }

        public boolean showShutdownUi(boolean isReboot, String reason) {
            if (StatusBarManagerService.this.mContext.getResources().getBoolean(17957020) && StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showShutdownUi(isReboot, reason);
                    return true;
                } catch (RemoteException e) {
                }
            }
            return false;
        }

        public void onProposedRotationChanged(int rotation, boolean isValid) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.onProposedRotationChanged(rotation, isValid);
                } catch (RemoteException e) {
                }
            }
        }
    };
    private final Object mLock = new Object();
    private boolean mMenuVisible = false;
    private NotificationDelegate mNotificationDelegate;
    private boolean mShowImeSwitcher;
    private IBinder mSysUiVisToken = new Binder();
    private int mSystemUiVisibility = 0;
    private final WindowManagerService mWindowManager;

    private class DisableRecord implements DeathRecipient {
        String pkg;
        IBinder token;
        int userId;
        int what1;
        int what2;

        public DisableRecord(int userId, IBinder token) {
            this.userId = userId;
            this.token = token;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        public void binderDied() {
            String str = StatusBarManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("binder died for pkg=");
            stringBuilder.append(this.pkg);
            Slog.i(str, stringBuilder.toString());
            StatusBarManagerService.this.disableForUser(0, this.token, this.pkg, this.userId);
            StatusBarManagerService.this.disable2ForUser(0, this.token, this.pkg, this.userId);
            this.token.unlinkToDeath(this, 0);
        }

        public void setFlags(int what, int which, String pkg) {
            switch (which) {
                case 1:
                    this.what1 = what;
                    return;
                case 2:
                    this.what2 = what;
                    return;
                default:
                    String str = StatusBarManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't set unsupported disable flag ");
                    stringBuilder.append(which);
                    stringBuilder.append(": 0x");
                    stringBuilder.append(Integer.toHexString(what));
                    Slog.w(str, stringBuilder.toString());
                    this.pkg = pkg;
                    return;
            }
        }

        public int getFlags(int which) {
            switch (which) {
                case 1:
                    return this.what1;
                case 2:
                    return this.what2;
                default:
                    String str = StatusBarManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't get unsupported disable flag ");
                    stringBuilder.append(which);
                    Slog.w(str, stringBuilder.toString());
                    return 0;
            }
        }

        public boolean isEmpty() {
            return this.what1 == 0 && this.what2 == 0;
        }

        public String toString() {
            return String.format("userId=%d what1=0x%08X what2=0x%08X pkg=%s token=%s", new Object[]{Integer.valueOf(this.userId), Integer.valueOf(this.what1), Integer.valueOf(this.what2), this.pkg, this.token});
        }
    }

    public StatusBarManagerService(Context context, WindowManagerService windowManager) {
        this.mContext = context;
        this.mWindowManager = windowManager;
        LocalServices.addService(StatusBarManagerInternal.class, this.mInternalService);
        LocalServices.addService(GlobalActionsProvider.class, this.mGlobalActionsProvider);
    }

    public void expandNotificationsPanel() {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateExpandNotificationsPanel();
            } catch (RemoteException e) {
            }
        }
    }

    public void collapsePanels() {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateCollapsePanels();
            } catch (RemoteException e) {
            }
        }
    }

    public void togglePanel() {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.togglePanel();
            } catch (RemoteException e) {
            }
        }
    }

    public void expandSettingsPanel(String subPanel) {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateExpandSettingsPanel(subPanel);
            } catch (RemoteException e) {
            }
        }
    }

    public void addTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.addQsTile(component);
            } catch (RemoteException e) {
            }
        }
    }

    public void remTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.remQsTile(component);
            } catch (RemoteException e) {
            }
        }
    }

    public void clickTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.clickQsTile(component);
            } catch (RemoteException e) {
            }
        }
    }

    public void handleSystemKey(int key) throws RemoteException {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.handleSystemKey(key);
            } catch (RemoteException e) {
            }
        }
    }

    public void showPinningEnterExitToast(boolean entering) throws RemoteException {
        if (this.mBar != null) {
            try {
                this.mBar.showPinningEnterExitToast(entering);
            } catch (RemoteException e) {
            }
        }
    }

    public void showPinningEscapeToast() throws RemoteException {
        if (this.mBar != null) {
            try {
                this.mBar.showPinningEscapeToast();
            } catch (RemoteException e) {
            }
        }
    }

    public void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver receiver) {
        if (this.mBar != null) {
            try {
                this.mBar.showFingerprintDialog(bundle, receiver);
            } catch (RemoteException e) {
            }
        }
    }

    public void onFingerprintAuthenticated() {
        if (this.mBar != null) {
            try {
                this.mBar.onFingerprintAuthenticated();
            } catch (RemoteException e) {
            }
        }
    }

    public void onFingerprintHelp(String message) {
        if (this.mBar != null) {
            try {
                this.mBar.onFingerprintHelp(message);
            } catch (RemoteException e) {
            }
        }
    }

    public void onFingerprintError(String error) {
        if (this.mBar != null) {
            try {
                this.mBar.onFingerprintError(error);
            } catch (RemoteException e) {
            }
        }
    }

    public void hideFingerprintDialog() {
        if (this.mBar != null) {
            try {
                this.mBar.hideFingerprintDialog();
            } catch (RemoteException e) {
            }
        }
    }

    public void disable(int what, IBinder token, String pkg) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.STATUSBAR_DISABLE);
        }
        disableForUser(what, token, pkg, this.mCurrentUserId);
    }

    public void disableForUser(int what, IBinder token, String pkg, int userId) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(userId, what, token, pkg, 1);
        }
    }

    public void disable2(int what, IBinder token, String pkg) {
        disable2ForUser(what, token, pkg, this.mCurrentUserId);
    }

    public void disable2ForUser(int what, IBinder token, String pkg, int userId) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(userId, what, token, pkg, 2);
        }
    }

    private void disableLocked(int userId, int what, IBinder token, String pkg, int whichFlag) {
        manageDisableListLocked(userId, what, token, pkg, whichFlag);
        final int net1 = gatherDisableActionsLocked(this.mCurrentUserId, 1);
        int net2 = gatherDisableActionsLocked(this.mCurrentUserId, 2);
        if (net1 != this.mDisabled1 || net2 != this.mDisabled2) {
            this.mDisabled1 = net1;
            this.mDisabled2 = net2;
            this.mHandler.post(new Runnable() {
                public void run() {
                    StatusBarManagerService.this.mNotificationDelegate.onSetDisabled(net1);
                }
            });
            if (this.mBar != null) {
                try {
                    this.mBar.disable(net1, net2);
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            Slog.i(TAG, "disableLocked mBar is null.");
        }
    }

    public void setIcon(String slot, String iconPackage, int iconId, int iconLevel, String contentDescription) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.STATUSBAR_SETICON);
        }
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon icon = new StatusBarIcon(iconPackage, UserHandle.SYSTEM, iconId, iconLevel, 0, contentDescription);
            this.mIcons.put(slot, icon);
            if (this.mBar != null) {
                try {
                    this.mBar.setIcon(slot, icon);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setIconVisibility(String slot, boolean visibility) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.STATUSBAR_SETICONVISIBILITY);
        }
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon icon = (StatusBarIcon) this.mIcons.get(slot);
            if (icon == null) {
                return;
            }
            if (icon.visible != visibility) {
                icon.visible = visibility;
                if (this.mBar != null) {
                    try {
                        this.mBar.setIcon(slot, icon);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public void removeIcon(String slot) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            this.mIcons.remove(slot);
            if (this.mBar != null) {
                try {
                    this.mBar.removeIcon(slot);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void topAppWindowChanged(final boolean menuVisible) {
        enforceStatusBar();
        synchronized (this.mLock) {
            this.mMenuVisible = menuVisible;
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.topAppWindowChanged(menuVisible);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        enforceStatusBar();
        synchronized (this.mLock) {
            this.mImeWindowVis = vis;
            this.mImeBackDisposition = backDisposition;
            this.mImeToken = token;
            this.mShowImeSwitcher = showImeSwitcher;
            final IBinder iBinder = token;
            final int i = vis;
            final int i2 = backDisposition;
            final boolean z = showImeSwitcher;
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.setImeWindowStatus(iBinder, i, i2, z);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
    }

    public void setSystemUiVisibility(int vis, int mask, String cause) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.STATUSBAR_SETSYSTEMUIVISIBILITY);
        }
        setSystemUiVisibility(vis, 0, 0, mask, this.mFullscreenStackBounds, this.mDockedStackBounds, cause);
    }

    private void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenBounds, Rect dockedBounds, String cause) {
        enforceStatusBarService();
        synchronized (this.mLock) {
            updateUiVisibilityLocked(vis, fullscreenStackVis, dockedStackVis, mask, fullscreenBounds, dockedBounds);
            disableLocked(this.mCurrentUserId, vis & 67043328, this.mSysUiVisToken, cause, 1);
        }
    }

    private void updateUiVisibilityLocked(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenBounds, Rect dockedBounds) {
        int i = vis;
        int i2 = fullscreenStackVis;
        int i3 = dockedStackVis;
        Rect rect = fullscreenBounds;
        Rect rect2 = dockedBounds;
        if (this.mSystemUiVisibility != i || this.mFullscreenStackSysUiVisibility != i2 || this.mDockedStackSysUiVisibility != i3 || !this.mFullscreenStackBounds.equals(rect) || !this.mDockedStackBounds.equals(rect2)) {
            this.mSystemUiVisibility = i;
            this.mFullscreenStackSysUiVisibility = i2;
            this.mDockedStackSysUiVisibility = i3;
            this.mFullscreenStackBounds.set(rect);
            this.mDockedStackBounds.set(rect2);
            final int i4 = i;
            final int i5 = i2;
            final int i6 = i3;
            final int i7 = mask;
            final Rect rect3 = rect;
            final Rect rect4 = rect2;
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.setSystemUiVisibility(i4, i5, i6, i7, rect3, rect4);
                        } catch (Exception ex) {
                            String str = StatusBarManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Exception had happend in method updateUiVisibilityLocked, getMessage=");
                            stringBuilder.append(ex.getMessage());
                            Slog.e(str, stringBuilder.toString());
                        }
                    }
                }
            });
        }
    }

    private void enforceStatusBarOrShell() {
        if (Binder.getCallingUid() != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            enforceStatusBar();
        }
    }

    private void enforceStatusBar() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", TAG);
    }

    private void enforceExpandStatusBar() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.EXPAND_STATUS_BAR", TAG);
    }

    private void enforceStatusBarService() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", TAG);
    }

    public void registerStatusBar(IStatusBar bar, List<String> iconSlots, List<StatusBarIcon> iconList, int[] switches, List<IBinder> binders, Rect fullscreenStackBounds, Rect dockedStackBounds) {
        enforceStatusBarService();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerStatusBar bar=");
        stringBuilder.append(bar);
        Slog.i(str, stringBuilder.toString());
        synchronized (this.mLock) {
            this.mBar = bar;
            try {
                this.mBar.asBinder().linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        StatusBarManagerService.this.mBar = null;
                        StatusBarManagerService.this.notifyBarAttachChanged();
                    }
                }, 0);
            } catch (RemoteException e) {
            }
            notifyBarAttachChanged();
        }
        synchronized (this.mIcons) {
            for (String slot : this.mIcons.keySet()) {
                iconSlots.add(slot);
                iconList.add((StatusBarIcon) this.mIcons.get(slot));
            }
        }
        synchronized (this.mLock) {
            switches[0] = gatherDisableActionsLocked(this.mCurrentUserId, 1);
            switches[1] = this.mSystemUiVisibility;
            switches[2] = this.mMenuVisible;
            switches[3] = this.mImeWindowVis;
            switches[4] = this.mImeBackDisposition;
            switches[5] = this.mShowImeSwitcher;
            switches[6] = gatherDisableActionsLocked(this.mCurrentUserId, 2);
            switches[7] = this.mFullscreenStackSysUiVisibility;
            switches[8] = this.mDockedStackSysUiVisibility;
            binders.add(this.mImeToken);
            fullscreenStackBounds.set(this.mFullscreenStackBounds);
            dockedStackBounds.set(this.mDockedStackBounds);
        }
    }

    private void notifyBarAttachChanged() {
        this.mHandler.post(new -$$Lambda$StatusBarManagerService$yJtT-4wu2t7bMtUpZNqcLBShMU8(this));
    }

    public static /* synthetic */ void lambda$notifyBarAttachChanged$0(StatusBarManagerService statusBarManagerService) {
        if (statusBarManagerService.mGlobalActionListener != null) {
            statusBarManagerService.mGlobalActionListener.onGlobalActionsAvailableChanged(statusBarManagerService.mBar != null);
        }
    }

    public void onPanelRevealed(boolean clearNotificationEffects, int numItems) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelRevealed(clearNotificationEffects, numItems);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void clearNotificationEffects() throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.clearEffects();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onPanelHidden() throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelHidden();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void shutdown() {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mHandler.post(-$$Lambda$StatusBarManagerService$izMbpkX9bmZwnjh3sH07yuoJPNY.INSTANCE);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void reboot(boolean safeMode) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mHandler.post(new -$$Lambda$StatusBarManagerService$r43hbhDcFisIPH512W_AYyyIFTg(safeMode));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    static /* synthetic */ void lambda$reboot$2(boolean safeMode) {
        if (safeMode) {
            ShutdownThread.rebootSafeMode(getUiContext(), true);
        } else {
            ShutdownThread.reboot(getUiContext(), "userrequested", false);
        }
    }

    public void onGlobalActionsShown() {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            if (this.mGlobalActionListener != null) {
                this.mGlobalActionListener.onGlobalActionsShown();
                Binder.restoreCallingIdentity(identity);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onGlobalActionsHidden() {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            if (this.mGlobalActionListener != null) {
                this.mGlobalActionListener.onGlobalActionsDismissed();
                Binder.restoreCallingIdentity(identity);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationClick(String key, NotificationVisibility nv) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClick(callingUid, callingPid, key, nv);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationActionClick(String key, int actionIndex, NotificationVisibility nv) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationActionClick(callingUid, callingPid, key, actionIndex, nv);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationError(String pkg, String tag, int id, int uid, int initialPid, String message, int userId) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationError(callingUid, callingPid, pkg, tag, id, uid, initialPid, message, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationClear(String pkg, String tag, int id, int userId, String key, int dismissalSurface, NotificationVisibility nv) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClear(callingUid, callingPid, pkg, tag, id, userId, key, dismissalSurface, nv);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationVisibilityChanged(NotificationVisibility[] newlyVisibleKeys, NotificationVisibility[] noLongerVisibleKeys) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationVisibilityChanged(newlyVisibleKeys, noLongerVisibleKeys);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationExpansionChanged(key, userAction, expanded);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationDirectReplied(String key) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationDirectReplied(key);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationSmartRepliesAdded(String key, int replyCount) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSmartRepliesAdded(key, replyCount);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationSmartReplySent(String key, int replyIndex) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSmartReplySent(key, replyIndex);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationSettingsViewed(String key) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSettingsViewed(key);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onClearAllNotifications(int userId) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onClearAll(callingUid, callingPid, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new StatusBarShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    public String[] getStatusBarIcons() {
        return this.mContext.getResources().getStringArray(17236037);
    }

    void manageDisableListLocked(int userId, int what, IBinder token, String pkg, int which) {
        if (Log.HWINFO) {
            String NUM_REGEX = "[0-9]++";
            if (!Pattern.compile("[0-9]++").matcher(pkg != null ? pkg : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).find()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("manageDisableList userId=");
                stringBuilder.append(userId);
                stringBuilder.append(" what=0x");
                stringBuilder.append(Integer.toHexString(what));
                stringBuilder.append(" pkg=");
                stringBuilder.append(pkg);
                Slog.d(str, stringBuilder.toString());
            }
        }
        int N = this.mDisableRecords.size();
        DisableRecord record = null;
        int i = 0;
        while (i < N) {
            DisableRecord r = (DisableRecord) this.mDisableRecords.get(i);
            if (r.token == token && r.userId == userId) {
                record = r;
                break;
            }
            i++;
        }
        if (!token.isBinderAlive()) {
            if (record != null) {
                this.mDisableRecords.remove(i);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Disable records remove token ");
                stringBuilder2.append(record);
                stringBuilder2.append(" list: ");
                stringBuilder2.append(this.mDisableRecords);
                Slog.i(str2, stringBuilder2.toString());
                record.token.unlinkToDeath(record, 0);
            }
        } else if (record != null) {
            record.setFlags(what, which, pkg);
            if (record.isEmpty()) {
                this.mDisableRecords.remove(i);
                record.token.unlinkToDeath(record, 0);
            }
        } else {
            record = new DisableRecord(userId, token);
            record.setFlags(what, which, pkg);
            this.mDisableRecords.add(record);
        }
    }

    int gatherDisableActionsLocked(int userId, int which) {
        int N = this.mDisableRecords.size();
        int net = 0;
        for (int i = 0; i < N; i++) {
            DisableRecord rec = (DisableRecord) this.mDisableRecords.get(i);
            if (rec.userId == userId) {
                net |= rec.getFlags(which);
            }
        }
        return net;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mLock) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  mDisabled1=0x");
                stringBuilder.append(Integer.toHexString(this.mDisabled1));
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mDisabled2=0x");
                stringBuilder.append(Integer.toHexString(this.mDisabled2));
                pw.println(stringBuilder.toString());
                int N = this.mDisableRecords.size();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  mDisableRecords.size=");
                stringBuilder2.append(N);
                pw.println(stringBuilder2.toString());
                for (int i = 0; i < N; i++) {
                    DisableRecord tok = (DisableRecord) this.mDisableRecords.get(i);
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("    [");
                    stringBuilder3.append(i);
                    stringBuilder3.append("] ");
                    stringBuilder3.append(tok);
                    pw.println(stringBuilder3.toString());
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  mCurrentUserId=");
                stringBuilder2.append(this.mCurrentUserId);
                pw.println(stringBuilder2.toString());
                pw.println("  mIcons=");
                for (String slot : this.mIcons.keySet()) {
                    pw.println("    ");
                    pw.print(slot);
                    pw.print(" -> ");
                    StatusBarIcon icon = (StatusBarIcon) this.mIcons.get(slot);
                    pw.print(icon);
                    if (!TextUtils.isEmpty(icon.contentDescription)) {
                        pw.print(" \"");
                        pw.print(icon.contentDescription);
                        pw.print("\"");
                    }
                    pw.println();
                }
            }
        }
    }

    public IStatusBar getStatusBar() {
        return this.mBar;
    }

    public boolean isNotificationsPanelExpand() {
        if (this.mBar == null) {
            return false;
        }
        try {
            return this.mBar.isNotificationPanelExpanded();
        } catch (RemoteException e) {
            return false;
        }
    }

    private static final Context getUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }
}
