package com.android.systemui.shared.system;

import android.os.RemoteException;
import android.util.Log;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.IRemoteAnimationRunner.Stub;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationTarget;

public class RemoteAnimationAdapterCompat {
    private final RemoteAnimationAdapter mWrapped;

    public RemoteAnimationAdapterCompat(RemoteAnimationRunnerCompat runner, long duration, long statusBarTransitionDelay) {
        this.mWrapped = new RemoteAnimationAdapter(wrapRemoteAnimationRunner(runner), duration, statusBarTransitionDelay);
    }

    RemoteAnimationAdapter getWrapped() {
        return this.mWrapped;
    }

    private static Stub wrapRemoteAnimationRunner(final RemoteAnimationRunnerCompat remoteAnimationAdapter) {
        return new Stub() {
            public void onAnimationStart(RemoteAnimationTarget[] apps, final IRemoteAnimationFinishedCallback finishedCallback) throws RemoteException {
                remoteAnimationAdapter.onAnimationStart(RemoteAnimationTargetCompat.wrap(apps), new Runnable() {
                    public void run() {
                        try {
                            finishedCallback.onAnimationFinished();
                        } catch (RemoteException e) {
                            Log.e("ActivityOptionsCompat", "Failed to call app controlled animation finished callback", e);
                        }
                    }
                });
            }

            public void onAnimationCancelled() throws RemoteException {
                remoteAnimationAdapter.onAnimationCancelled();
            }
        };
    }
}
