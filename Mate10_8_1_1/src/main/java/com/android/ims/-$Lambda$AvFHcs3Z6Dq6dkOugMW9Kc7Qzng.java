package com.android.ims;

import android.net.Uri;
import android.telephony.ims.ImsServiceProxy.INotifyStatusChanged;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng implements Consumer {
    public static final /* synthetic */ -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng $INST$0 = new -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng((byte) 0);
    public static final /* synthetic */ -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng $INST$1 = new -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng((byte) 1);
    public static final /* synthetic */ -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng $INST$2 = new -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng((byte) 2);
    public static final /* synthetic */ -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng $INST$3 = new -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng((byte) 3);
    public static final /* synthetic */ -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng $INST$4 = new -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng((byte) 4);
    public static final /* synthetic */ -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng $INST$5 = new -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng((byte) 5);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng$1 */
    final /* synthetic */ class AnonymousClass1 implements INotifyStatusChanged {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((ImsManager) this.-$f0).lambda$-com_android_ims_ImsManager_96779();
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void notifyStatusChanged() {
            $m$0();
        }
    }

    /* renamed from: com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((ImsManager) this.-$f0).-com_android_ims_ImsManager-mthref-0();
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng$3 */
    final /* synthetic */ class AnonymousClass3 implements Consumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((ImsConnectionStateListener) arg0).registrationAssociatedUriChanged((Uri[]) this.-$f0);
        }

        private final /* synthetic */ void $m$1(Object arg0) {
            ((ImsConnectionStateListener) arg0).onImsDisconnected((ImsReasonInfo) this.-$f0);
        }

        public /* synthetic */ AnonymousClass3(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void accept(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(obj);
                    return;
                case (byte) 1:
                    $m$1(obj);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng$4 */
    final /* synthetic */ class AnonymousClass4 implements Consumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((ImsConnectionStateListener) arg0).onImsConnected(this.-$f0);
        }

        private final /* synthetic */ void $m$1(Object arg0) {
            ((ImsConnectionStateListener) arg0).onImsProgressing(this.-$f0);
        }

        private final /* synthetic */ void $m$2(Object arg0) {
            ((ImsConnectionStateListener) arg0).onVoiceMessageCountChanged(this.-$f0);
        }

        public /* synthetic */ AnonymousClass4(byte b, int i) {
            this.$id = b;
            this.-$f0 = i;
        }

        public final void accept(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(obj);
                    return;
                case (byte) 1:
                    $m$1(obj);
                    return;
                case (byte) 2:
                    $m$2(obj);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng$5 */
    final /* synthetic */ class AnonymousClass5 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((ImsManager) this.-$f1).lambda$-com_android_ims_ImsManager_46661(this.-$f0);
        }

        private final /* synthetic */ void $m$1() {
            ((ImsManager) this.-$f1).lambda$-com_android_ims_ImsManager_49736(this.-$f0);
        }

        public /* synthetic */ AnonymousClass5(byte b, int i, Object obj) {
            this.$id = b;
            this.-$f0 = i;
            this.-$f1 = obj;
        }

        public final void run() {
            switch (this.$id) {
                case (byte) 0:
                    $m$0();
                    return;
                case (byte) 1:
                    $m$1();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng$6 */
    final /* synthetic */ class AnonymousClass6 implements Consumer {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((ImsConnectionStateListener) arg0).onRegistrationChangeFailed(this.-$f0, (ImsReasonInfo) this.-$f1);
        }

        public /* synthetic */ AnonymousClass6(int i, Object obj) {
            this.-$f0 = i;
            this.-$f1 = obj;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    /* renamed from: com.android.ims.-$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng$7 */
    final /* synthetic */ class AnonymousClass7 implements Consumer {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((ImsConnectionStateListener) arg0).onFeatureCapabilityChanged(this.-$f0, (int[]) this.-$f1, (int[]) this.-$f2);
        }

        public /* synthetic */ AnonymousClass7(int i, Object obj, Object obj2) {
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        ((ImsConnectionStateListener) arg0).onImsConnected(0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((ImsConnectionStateListener) arg0).onImsProgressing(0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        ((ImsConnectionStateListener) arg0).onImsResumed();
    }

    private final /* synthetic */ void $m$3(Object arg0) {
        ((ImsConnectionStateListener) arg0).onImsConnected(0);
    }

    private final /* synthetic */ void $m$4(Object arg0) {
        ((ImsConnectionStateListener) arg0).onImsSuspended();
    }

    private final /* synthetic */ void $m$5(Object arg0) {
        ((INotifyStatusChanged) arg0).notifyStatusChanged();
    }

    private /* synthetic */ -$Lambda$AvFHcs3Z6Dq6dkOugMW9Kc7Qzng(byte b) {
        this.$id = b;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            case (byte) 2:
                $m$2(obj);
                return;
            case ImsManager.CALL_DIALING /*3*/:
                $m$3(obj);
                return;
            case ImsManager.CALL_ALERTING /*4*/:
                $m$4(obj);
                return;
            case ImsManager.CALL_INCOMING /*5*/:
                $m$5(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
