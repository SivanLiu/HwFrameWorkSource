package com.android.server.policy.keyguard;

import android.aft.HwAftPolicyManager;
import android.aft.IHwAftPolicyService;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.policy.IKeyguardService;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavPolicy;
import com.android.server.input.HwInputManagerService;
import com.android.server.policy.keyguard.KeyguardStateMonitor;

public class HwKeyguardStateMonitor extends KeyguardStateMonitor {
    private static final String TAG = "HwKeyguardStateMonitor";
    private GestureNavPolicy mGestureNavPolicy = ((GestureNavPolicy) LocalServices.getService(GestureNavPolicy.class));
    private HwInputManagerService.HwInputManagerLocalService mHwInputManagerInternal = ((HwInputManagerService.HwInputManagerLocalService) LocalServices.getService(HwInputManagerService.HwInputManagerLocalService.class));

    public HwKeyguardStateMonitor(Context context, IKeyguardService service, StateCallback callback) {
        super(context, service, callback);
    }

    public void onShowingStateChanged(boolean showing) {
        HwKeyguardStateMonitor.super.onShowingStateChanged(showing);
        notifyKeyguardStateChanged(showing);
    }

    private void notifyKeyguardStateChanged(boolean showing) {
        HwInputManagerService.HwInputManagerLocalService hwInputManagerLocalService = this.mHwInputManagerInternal;
        if (hwInputManagerLocalService != null) {
            hwInputManagerLocalService.setKeyguardState(showing);
        }
        GestureNavPolicy gestureNavPolicy = this.mGestureNavPolicy;
        if (gestureNavPolicy != null) {
            gestureNavPolicy.onKeyguardShowingChanged(showing);
        }
        IHwAftPolicyService hwAft = HwAftPolicyManager.getService();
        if (hwAft != null) {
            try {
                hwAft.notifyKeyguardStateChange(showing);
            } catch (RemoteException e) {
                Slog.e(TAG, "notifyKeyguardStateChange throw exception");
            }
        }
    }
}
