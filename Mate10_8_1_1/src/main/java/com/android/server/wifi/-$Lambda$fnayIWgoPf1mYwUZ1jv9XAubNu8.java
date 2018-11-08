package com.android.server.wifi;

import android.hardware.wifi.supplicant.V1_0.ISupplicant.getInterfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicant.listInterfacesCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.addNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.getNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface.listNetworksCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getMacAddressCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.startWpsPinDisplayCallback;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import java.util.ArrayList;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.VowifiDetectCallback;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getApVendorInfoCallback;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getCapabRsdbCallback;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getMssStateCallback;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.getWpasConfigCallback;
import vendor.huawei.hardware.wifi.supplicant.V1_0.ISupplicantStaIface.heartBeatCallback;

final /* synthetic */ class -$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8 implements getInterfaceCallback {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$10 */
    final /* synthetic */ class AnonymousClass10 implements getMssStateCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_99742((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass10(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$11 */
    final /* synthetic */ class AnonymousClass11 implements getWpasConfigCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_98168((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass11(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$12 */
    final /* synthetic */ class AnonymousClass12 implements heartBeatCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_92403((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass12(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$1 */
    final /* synthetic */ class AnonymousClass1 implements listInterfacesCallback {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            SupplicantStaIfaceHal.lambda$-com_android_server_wifi_SupplicantStaIfaceHal_12261((ArrayList) this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$2 */
    final /* synthetic */ class AnonymousClass2 implements addNetworkCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ISupplicantNetwork arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_30285((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass2(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
            $m$0(supplicantStatus, iSupplicantNetwork);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$3 */
    final /* synthetic */ class AnonymousClass3 implements getNetworkCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ISupplicantNetwork arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_32882((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass3(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
            $m$0(supplicantStatus, iSupplicantNetwork);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$4 */
    final /* synthetic */ class AnonymousClass4 implements listNetworksCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, ArrayList arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_34661((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass4(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
            $m$0(supplicantStatus, arrayList);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$5 */
    final /* synthetic */ class AnonymousClass5 implements getMacAddressCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, byte[] arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_51626((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass5(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
            $m$0(supplicantStatus, bArr);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$6 */
    final /* synthetic */ class AnonymousClass6 implements startWpsPinDisplayCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_65764((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass6(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$7 */
    final /* synthetic */ class AnonymousClass7 implements VowifiDetectCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_91463((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass7(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$8 */
    final /* synthetic */ class AnonymousClass8 implements getApVendorInfoCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_100627((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass8(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    /* renamed from: com.android.server.wifi.-$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8$9 */
    final /* synthetic */ class AnonymousClass9 implements getCapabRsdbCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(SupplicantStatus arg0, String arg1) {
            ((SupplicantStaIfaceHal) this.-$f0).lambda$-com_android_server_wifi_SupplicantStaIfaceHal_97278((Mutable) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass9(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(SupplicantStatus supplicantStatus, String str) {
            $m$0(supplicantStatus, str);
        }
    }

    private final /* synthetic */ void $m$0(SupplicantStatus arg0, ISupplicantIface arg1) {
        SupplicantStaIfaceHal.lambda$-com_android_server_wifi_SupplicantStaIfaceHal_13372((Mutable) this.-$f0, arg0, arg1);
    }

    public /* synthetic */ -$Lambda$fnayIWgoPf1mYwUZ1jv9XAubNu8(Object obj) {
        this.-$f0 = obj;
    }

    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        $m$0(supplicantStatus, iSupplicantIface);
    }
}
