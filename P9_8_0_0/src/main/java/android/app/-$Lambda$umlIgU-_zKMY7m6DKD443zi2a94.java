package android.app;

import android.app.KeyguardManager.KeyguardDismissCallback;

final /* synthetic */ class -$Lambda$umlIgU-_zKMY7m6DKD443zi2a94 implements Runnable {
    private final /* synthetic */ Object -$f0;

    /* renamed from: android.app.-$Lambda$umlIgU-_zKMY7m6DKD443zi2a94$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((KeyguardDismissCallback) this.-$f0).onDismissError();
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: android.app.-$Lambda$umlIgU-_zKMY7m6DKD443zi2a94$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((KeyguardDismissCallback) this.-$f0).onDismissSucceeded();
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        ((KeyguardDismissCallback) this.-$f0).onDismissCancelled();
    }

    public /* synthetic */ -$Lambda$umlIgU-_zKMY7m6DKD443zi2a94(Object obj) {
        this.-$f0 = obj;
    }

    public final void run() {
        $m$0();
    }
}
