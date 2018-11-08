package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl.addDownstreamCallback;
import android.hardware.tetheroffload.control.V1_0.IOffloadControl.getForwardedStatsCallback;
import android.hardware.tetheroffload.control.V1_0.IOffloadControl.initOffloadCallback;
import android.hardware.tetheroffload.control.V1_0.IOffloadControl.removeDownstreamCallback;
import android.hardware.tetheroffload.control.V1_0.IOffloadControl.setDataLimitCallback;
import android.hardware.tetheroffload.control.V1_0.IOffloadControl.setLocalPrefixesCallback;
import android.hardware.tetheroffload.control.V1_0.IOffloadControl.setUpstreamParametersCallback;
import android.hardware.tetheroffload.control.V1_0.IOffloadControl.stopOffloadCallback;
import android.hardware.tetheroffload.control.V1_0.NatTimeoutUpdate;
import com.android.server.connectivity.tethering.OffloadHardwareInterface.ForwardedStats;

final /* synthetic */ class -$Lambda$LVMU292iEsklodYmav2xkNUv4MU implements addDownstreamCallback {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$1 */
    final /* synthetic */ class AnonymousClass1 implements getForwardedStatsCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(long arg0, long arg1) {
            OffloadHardwareInterface.lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_5729((ForwardedStats) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(long j, long j2) {
            $m$0(j, j2);
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$2 */
    final /* synthetic */ class AnonymousClass2 implements initOffloadCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(boolean arg0, String arg1) {
            OffloadHardwareInterface.lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_4483((CbResults) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(boolean z, String str) {
            $m$0(z, str);
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$3 */
    final /* synthetic */ class AnonymousClass3 implements removeDownstreamCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(boolean arg0, String arg1) {
            OffloadHardwareInterface.lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_9458((CbResults) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass3(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(boolean z, String str) {
            $m$0(z, str);
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$4 */
    final /* synthetic */ class AnonymousClass4 implements setDataLimitCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(boolean arg0, String arg1) {
            OffloadHardwareInterface.lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_7086((CbResults) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass4(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(boolean z, String str) {
            $m$0(z, str);
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$5 */
    final /* synthetic */ class AnonymousClass5 implements setLocalPrefixesCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(boolean arg0, String arg1) {
            OffloadHardwareInterface.lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_6440((CbResults) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass5(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(boolean z, String str) {
            $m$0(z, str);
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$6 */
    final /* synthetic */ class AnonymousClass6 implements setUpstreamParametersCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(boolean arg0, String arg1) {
            OffloadHardwareInterface.lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_8155((CbResults) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass6(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(boolean z, String str) {
            $m$0(z, str);
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$7 */
    final /* synthetic */ class AnonymousClass7 implements stopOffloadCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(boolean arg0, String arg1) {
            ((OffloadHardwareInterface) this.-$f0).lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_4988(arg0, arg1);
        }

        public /* synthetic */ AnonymousClass7(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(boolean z, String str) {
            $m$0(z, str);
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$8 */
    final /* synthetic */ class AnonymousClass8 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TetheringOffloadCallback) this.-$f0).lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface$TetheringOffloadCallback_11706((NatTimeoutUpdate) this.-$f1);
        }

        public /* synthetic */ AnonymousClass8(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU$9 */
    final /* synthetic */ class AnonymousClass9 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TetheringOffloadCallback) this.-$f1).lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface$TetheringOffloadCallback_10626(this.-$f0);
        }

        public /* synthetic */ AnonymousClass9(int i, Object obj) {
            this.-$f0 = i;
            this.-$f1 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0(boolean arg0, String arg1) {
        OffloadHardwareInterface.lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_8802((CbResults) this.-$f0, arg0, arg1);
    }

    public /* synthetic */ -$Lambda$LVMU292iEsklodYmav2xkNUv4MU(Object obj) {
        this.-$f0 = obj;
    }

    public final void onValues(boolean z, String str) {
        $m$0(z, str);
    }
}
