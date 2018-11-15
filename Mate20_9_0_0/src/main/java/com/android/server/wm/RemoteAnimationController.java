package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.IRemoteAnimationFinishedCallback.Stub;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import com.android.internal.util.FastPrintWriter;
import com.android.server.wm.utils.InsetUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

class RemoteAnimationController implements DeathRecipient {
    private static final String TAG;
    private static final long TIMEOUT_MS = 2000;
    private boolean mCanceled;
    private FinishedCallback mFinishedCallback;
    private final Handler mHandler;
    private boolean mLinkedToDeathOfRunner;
    private final ArrayList<RemoteAnimationAdapterWrapper> mPendingAnimations = new ArrayList();
    private final RemoteAnimationAdapter mRemoteAnimationAdapter;
    private final WindowManagerService mService;
    private final Runnable mTimeoutRunnable = new -$$Lambda$RemoteAnimationController$uQS8vaPKQ-E3x_9G8NCxPQmw1fw(this);
    private final Rect mTmpRect = new Rect();

    private static final class FinishedCallback extends Stub {
        RemoteAnimationController mOuter;

        FinishedCallback(RemoteAnimationController outer) {
            this.mOuter = outer;
        }

        public void onAnimationFinished() throws RemoteException {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                String access$200 = RemoteAnimationController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("app-onAnimationFinished(): mOuter=");
                stringBuilder.append(this.mOuter);
                Slog.d(access$200, stringBuilder.toString());
            }
            long token = Binder.clearCallingIdentity();
            try {
                if (this.mOuter != null) {
                    this.mOuter.onAnimationFinished();
                    this.mOuter = null;
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        void release() {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                String access$200 = RemoteAnimationController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("app-release(): mOuter=");
                stringBuilder.append(this.mOuter);
                Slog.d(access$200, stringBuilder.toString());
            }
            this.mOuter = null;
        }
    }

    private class RemoteAnimationAdapterWrapper implements AnimationAdapter {
        private final AppWindowToken mAppWindowToken;
        private OnAnimationFinishedCallback mCapturedFinishCallback;
        private SurfaceControl mCapturedLeash;
        private final Point mPosition = new Point();
        private final Rect mStackBounds = new Rect();
        private RemoteAnimationTarget mTarget;

        RemoteAnimationAdapterWrapper(AppWindowToken appWindowToken, Point position, Rect stackBounds) {
            this.mAppWindowToken = appWindowToken;
            this.mPosition.set(position.x, position.y);
            this.mStackBounds.set(stackBounds);
        }

        RemoteAnimationTarget createRemoteAppAnimation() {
            Task task = this.mAppWindowToken.getTask();
            WindowState mainWindow = this.mAppWindowToken.findMainWindow();
            if (task == null || mainWindow == null || this.mCapturedFinishCallback == null || this.mCapturedLeash == null) {
                return null;
            }
            Rect insets = new Rect(mainWindow.mContentInsets);
            InsetUtils.addInsets(insets, this.mAppWindowToken.getLetterboxInsets());
            this.mTarget = new RemoteAnimationTarget(task.mTaskId, getMode(), this.mCapturedLeash, this.mAppWindowToken.fillsParent() ^ 1, mainWindow.mWinAnimator.mLastClipRect, insets, this.mAppWindowToken.getPrefixOrderIndex(), this.mPosition, this.mStackBounds, task.getWindowConfiguration(), false);
            return this.mTarget;
        }

        private int getMode() {
            if (RemoteAnimationController.this.mService.mOpeningApps.contains(this.mAppWindowToken)) {
                return 0;
            }
            return 1;
        }

        public boolean getDetachWallpaper() {
            return false;
        }

        public boolean getShowWallpaper() {
            return false;
        }

        public int getBackgroundColor() {
            return 0;
        }

        public void startAnimation(SurfaceControl animationLeash, Transaction t, OnAnimationFinishedCallback finishCallback) {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                Slog.d(RemoteAnimationController.TAG, "startAnimation");
            }
            t.setLayer(animationLeash, this.mAppWindowToken.getPrefixOrderIndex());
            t.setPosition(animationLeash, (float) this.mPosition.x, (float) this.mPosition.y);
            RemoteAnimationController.this.mTmpRect.set(this.mStackBounds);
            RemoteAnimationController.this.mTmpRect.offsetTo(0, 0);
            t.setWindowCrop(animationLeash, RemoteAnimationController.this.mTmpRect);
            this.mCapturedLeash = animationLeash;
            this.mCapturedFinishCallback = finishCallback;
        }

        public void onAnimationCancelled(SurfaceControl animationLeash) {
            RemoteAnimationController.this.mPendingAnimations.remove(this);
            if (RemoteAnimationController.this.mPendingAnimations.isEmpty()) {
                RemoteAnimationController.this.mHandler.removeCallbacks(RemoteAnimationController.this.mTimeoutRunnable);
                RemoteAnimationController.this.releaseFinishedCallback();
                RemoteAnimationController.this.invokeAnimationCancelled();
                RemoteAnimationController.this.sendRunningRemoteAnimation(false);
            }
        }

        public long getDurationHint() {
            return RemoteAnimationController.this.mRemoteAnimationAdapter.getDuration();
        }

        public long getStatusBarTransitionsStartTime() {
            return SystemClock.uptimeMillis() + RemoteAnimationController.this.mRemoteAnimationAdapter.getStatusBarTransitionDelay();
        }

        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.print("token=");
            pw.println(this.mAppWindowToken);
            if (this.mTarget != null) {
                pw.print(prefix);
                pw.println("Target:");
                RemoteAnimationTarget remoteAnimationTarget = this.mTarget;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("  ");
                remoteAnimationTarget.dump(pw, stringBuilder.toString());
                return;
            }
            pw.print(prefix);
            pw.println("Target: null");
        }

        public void writeToProto(ProtoOutputStream proto) {
            long token = proto.start(1146756268034L);
            if (this.mTarget != null) {
                this.mTarget.writeToProto(proto, 1146756268033L);
            }
            proto.end(token);
        }
    }

    static {
        String str = (!WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS || WindowManagerDebugConfig.DEBUG_APP_TRANSITIONS) ? "WindowManager" : "RemoteAnimationController";
        TAG = str;
    }

    RemoteAnimationController(WindowManagerService service, RemoteAnimationAdapter remoteAnimationAdapter, Handler handler) {
        this.mService = service;
        this.mRemoteAnimationAdapter = remoteAnimationAdapter;
        this.mHandler = handler;
    }

    AnimationAdapter createAnimationAdapter(AppWindowToken appWindowToken, Point position, Rect stackBounds) {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createAnimationAdapter(): token=");
            stringBuilder.append(appWindowToken);
            Slog.d(str, stringBuilder.toString());
        }
        RemoteAnimationAdapterWrapper adapter = new RemoteAnimationAdapterWrapper(appWindowToken, position, stackBounds);
        this.mPendingAnimations.add(adapter);
        return adapter;
    }

    void goodToGo() {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "goodToGo()");
        }
        if (this.mPendingAnimations.isEmpty() || this.mCanceled) {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("goodToGo(): Animation finished already, canceled=");
                stringBuilder.append(this.mCanceled);
                stringBuilder.append(" mPendingAnimations=");
                stringBuilder.append(this.mPendingAnimations.size());
                Slog.d(str, stringBuilder.toString());
            }
            onAnimationFinished();
            return;
        }
        this.mHandler.postDelayed(this.mTimeoutRunnable, (long) (2000.0f * this.mService.getCurrentAnimatorScale()));
        this.mFinishedCallback = new FinishedCallback(this);
        RemoteAnimationTarget[] animations = createAnimations();
        if (animations.length == 0) {
            if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                Slog.d(TAG, "goodToGo(): No apps to animate");
            }
            onAnimationFinished();
            return;
        }
        this.mService.mAnimator.addAfterPrepareSurfacesRunnable(new -$$Lambda$RemoteAnimationController$f_Hsu4PN7pGOiq9Nl8vxzEA3wa0(this, animations));
        sendRunningRemoteAnimation(true);
    }

    public static /* synthetic */ void lambda$goodToGo$1(RemoteAnimationController remoteAnimationController, RemoteAnimationTarget[] animations) {
        try {
            remoteAnimationController.linkToDeathOfRunner();
            remoteAnimationController.mRemoteAnimationAdapter.getRunner().onAnimationStart(animations, remoteAnimationController.mFinishedCallback);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to start remote animation", e);
            remoteAnimationController.onAnimationFinished();
        }
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "startAnimation(): Notify animation start:");
            remoteAnimationController.writeStartDebugStatement();
        }
    }

    private void cancelAnimation(String reason) {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancelAnimation(): reason=");
            stringBuilder.append(reason);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this.mService.getWindowManagerLock()) {
            if (this.mCanceled) {
                return;
            }
            this.mCanceled = true;
            onAnimationFinished();
            invokeAnimationCancelled();
        }
    }

    private void writeStartDebugStatement() {
        Slog.i(TAG, "Starting remote animation");
        StringWriter sw = new StringWriter();
        FastPrintWriter pw = new FastPrintWriter(sw);
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            ((RemoteAnimationAdapterWrapper) this.mPendingAnimations.get(i)).dump(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        pw.close();
        Slog.i(TAG, sw.toString());
    }

    private RemoteAnimationTarget[] createAnimations() {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.d(TAG, "createAnimations()");
        }
        ArrayList<RemoteAnimationTarget> targets = new ArrayList();
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            RemoteAnimationAdapterWrapper wrapper = (RemoteAnimationAdapterWrapper) this.mPendingAnimations.get(i);
            RemoteAnimationTarget target = wrapper.createRemoteAppAnimation();
            String str;
            StringBuilder stringBuilder;
            if (target != null) {
                if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("\tAdd token=");
                    stringBuilder.append(wrapper.mAppWindowToken);
                    Slog.d(str, stringBuilder.toString());
                }
                targets.add(target);
            } else {
                if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("\tRemove token=");
                    stringBuilder.append(wrapper.mAppWindowToken);
                    Slog.d(str, stringBuilder.toString());
                }
                if (wrapper.mCapturedFinishCallback != null) {
                    wrapper.mCapturedFinishCallback.onAnimationFinished(wrapper);
                }
                this.mPendingAnimations.remove(i);
            }
        }
        return (RemoteAnimationTarget[]) targets.toArray(new RemoteAnimationTarget[targets.size()]);
    }

    private void onAnimationFinished() {
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onAnimationFinished(): mPendingAnimations=");
            stringBuilder.append(this.mPendingAnimations.size());
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                unlinkToDeathOfRunner();
                releaseFinishedCallback();
                this.mService.openSurfaceTransaction();
                if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                    Slog.d(TAG, "onAnimationFinished(): Notify animation finished:");
                }
                for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
                    RemoteAnimationAdapterWrapper adapter = (RemoteAnimationAdapterWrapper) this.mPendingAnimations.get(i);
                    adapter.mCapturedFinishCallback.onAnimationFinished(adapter);
                    if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("\t");
                        stringBuilder2.append(adapter.mAppWindowToken);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                }
                this.mService.closeSurfaceTransaction("RemoteAnimationController#finished");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to finish remote animation", e);
                throw e;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        sendRunningRemoteAnimation(false);
        if (WindowManagerDebugConfig.DEBUG_REMOTE_ANIMATIONS) {
            Slog.i(TAG, "Finishing remote animation");
        }
    }

    private void invokeAnimationCancelled() {
        try {
            this.mRemoteAnimationAdapter.getRunner().onAnimationCancelled();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to notify cancel", e);
        }
    }

    private void releaseFinishedCallback() {
        if (this.mFinishedCallback != null) {
            this.mFinishedCallback.release();
            this.mFinishedCallback = null;
        }
    }

    private void sendRunningRemoteAnimation(boolean running) {
        int pid = this.mRemoteAnimationAdapter.getCallingPid();
        if (pid != 0) {
            this.mService.sendSetRunningRemoteAnimation(pid, running);
            return;
        }
        throw new RuntimeException("Calling pid of remote animation was null");
    }

    private void linkToDeathOfRunner() throws RemoteException {
        if (!this.mLinkedToDeathOfRunner) {
            this.mRemoteAnimationAdapter.getRunner().asBinder().linkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = true;
        }
    }

    private void unlinkToDeathOfRunner() {
        if (this.mLinkedToDeathOfRunner) {
            this.mRemoteAnimationAdapter.getRunner().asBinder().unlinkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = false;
        }
    }

    public void binderDied() {
        cancelAnimation("binderDied");
    }
}
