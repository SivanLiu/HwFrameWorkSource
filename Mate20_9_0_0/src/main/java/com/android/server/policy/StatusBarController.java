package com.android.server.policy;

import android.os.IBinder;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal.AppTransitionListener;

public class StatusBarController extends BarController {
    private final AppTransitionListener mAppTransitionListener = new AppTransitionListener() {
        public void onAppTransitionPendingLocked() {
            StatusBarController.this.mHandler.post(new Runnable() {
                public void run() {
                    StatusBarManagerInternal statusbar = StatusBarController.this.getStatusBarInternal();
                    if (statusbar != null) {
                        statusbar.appTransitionPending();
                    }
                }
            });
        }

        public int onAppTransitionStartingLocked(int transit, IBinder openToken, IBinder closeToken, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
            final long j = statusBarAnimationStartTime;
            final long j2 = statusBarAnimationDuration;
            StatusBarController.this.mHandler.post(new Runnable() {
                public void run() {
                    StatusBarManagerInternal statusbar = StatusBarController.this.getStatusBarInternal();
                    if (statusbar != null) {
                        statusbar.appTransitionStarting(j, j2);
                    }
                }
            });
            return 0;
        }

        public void onAppTransitionCancelledLocked(int transit) {
            StatusBarController.this.mHandler.post(new Runnable() {
                public void run() {
                    StatusBarManagerInternal statusbar = StatusBarController.this.getStatusBarInternal();
                    if (statusbar != null) {
                        statusbar.appTransitionCancelled();
                    }
                }
            });
        }

        public void onAppTransitionFinishedLocked(IBinder token) {
            StatusBarController.this.mHandler.post(new Runnable() {
                public void run() {
                    StatusBarManagerInternal statusbar = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                    if (statusbar != null) {
                        statusbar.appTransitionFinished();
                    }
                }
            });
        }
    };

    public StatusBarController() {
        super("StatusBar", 67108864, 268435456, 1073741824, 1, 67108864, 8);
    }

    public void setTopAppHidesStatusBar(boolean hidesStatusBar) {
        StatusBarManagerInternal statusbar = getStatusBarInternal();
        if (statusbar != null) {
            statusbar.setTopAppHidesStatusBar(hidesStatusBar);
        }
    }

    protected boolean skipAnimation() {
        return this.mWin.getAttrs().height == -1;
    }

    public AppTransitionListener getAppTransitionListener() {
        return this.mAppTransitionListener;
    }
}
