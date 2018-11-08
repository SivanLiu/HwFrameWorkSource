package android.hardware.radio;

import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;

final /* synthetic */ class -$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: android.hardware.radio.-$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TunerCallbackAdapter) this.-$f0).lambda$-android_hardware_radio_TunerCallbackAdapter_1643((BandConfig) this.-$f1);
        }

        private final /* synthetic */ void $m$1() {
            ((TunerCallbackAdapter) this.-$f0).lambda$-android_hardware_radio_TunerCallbackAdapter_1927((ProgramInfo) this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
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

    /* renamed from: android.hardware.radio.-$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TunerCallbackAdapter) this.-$f1).lambda$-android_hardware_radio_TunerCallbackAdapter_1493(this.-$f0);
        }

        public /* synthetic */ AnonymousClass2(int i, Object obj) {
            this.-$f0 = i;
            this.-$f1 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: android.hardware.radio.-$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o$3 */
    final /* synthetic */ class AnonymousClass3 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TunerCallbackAdapter) this.-$f1).lambda$-android_hardware_radio_TunerCallbackAdapter_2521(this.-$f0);
        }

        private final /* synthetic */ void $m$1() {
            ((TunerCallbackAdapter) this.-$f1).lambda$-android_hardware_radio_TunerCallbackAdapter_2682(this.-$f0);
        }

        private final /* synthetic */ void $m$2() {
            ((TunerCallbackAdapter) this.-$f1).lambda$-android_hardware_radio_TunerCallbackAdapter_2376(this.-$f0);
        }

        private final /* synthetic */ void $m$3() {
            ((TunerCallbackAdapter) this.-$f1).lambda$-android_hardware_radio_TunerCallbackAdapter_2227(this.-$f0);
        }

        public /* synthetic */ AnonymousClass3(byte b, boolean z, Object obj) {
            this.$id = b;
            this.-$f0 = z;
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
                case (byte) 2:
                    $m$2();
                    return;
                case (byte) 3:
                    $m$3();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ void $m$0() {
        ((TunerCallbackAdapter) this.-$f0).lambda$-android_hardware_radio_TunerCallbackAdapter_2836();
    }

    private final /* synthetic */ void $m$1() {
        ((TunerCallbackAdapter) this.-$f0).lambda$-android_hardware_radio_TunerCallbackAdapter_2965();
    }

    public /* synthetic */ -$Lambda$JnOBQcNE2QHtc2zY4hNL33J974o(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
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
