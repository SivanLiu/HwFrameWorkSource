package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.os.Binder;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.concurrent.Executor;

public class CallbackProxies {

    public static class SessionStateCallbackProxy extends StateCallback {
        private final StateCallback mCallback;
        private final Executor mExecutor;

        public SessionStateCallbackProxy(Executor executor, StateCallback callback) {
            this.mExecutor = (Executor) Preconditions.checkNotNull(executor, "executor must not be null");
            this.mCallback = (StateCallback) Preconditions.checkNotNull(callback, "callback must not be null");
        }

        public void onConfigured(CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new -$$Lambda$CallbackProxies$SessionStateCallbackProxy$soW0qC12Osypoky6AfL3P2-TeDw(this, session));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onConfigureFailed(CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new -$$Lambda$CallbackProxies$SessionStateCallbackProxy$gvbTsp9UPpKJAbdycdci_ZW5BeI(this, session));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onReady(CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new -$$Lambda$CallbackProxies$SessionStateCallbackProxy$Hoz-iT1tD_pl7sCGu4flyo-xB90(this, session));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onActive(CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new -$$Lambda$CallbackProxies$SessionStateCallbackProxy$ISQyEhLUI1khcOCin3OIsRyTUoU(this, session));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onCaptureQueueEmpty(CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new -$$Lambda$CallbackProxies$SessionStateCallbackProxy$hoQOYc189Bss2NBtrutabMRw4VU(this, session));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onClosed(CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new -$$Lambda$CallbackProxies$SessionStateCallbackProxy$9H0ZdANdMrdpoq2bfIL2l3DVsKk(this, session));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new -$$Lambda$CallbackProxies$SessionStateCallbackProxy$tuajQwbKz3BV5CZZJdjl97HF6Tw(this, session, surface));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private CallbackProxies() {
        throw new AssertionError();
    }
}
