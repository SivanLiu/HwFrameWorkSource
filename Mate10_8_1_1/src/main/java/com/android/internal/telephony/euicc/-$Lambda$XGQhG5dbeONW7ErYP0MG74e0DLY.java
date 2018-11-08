package com.android.internal.telephony.euicc;

import android.service.euicc.GetDefaultDownloadableSubscriptionListResult;
import android.service.euicc.GetDownloadableSubscriptionMetadataResult;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.telephony.euicc.EuiccInfo;
import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass10;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass11;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass2;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass3;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass4;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass5;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass6;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass7;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass8;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass9;

final /* synthetic */ class -$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    /* renamed from: com.android.internal.telephony.euicc.-$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((AnonymousClass10) this.-$f1).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$10_36674((BaseEuiccCommandCallback) this.-$f2, this.-$f0);
        }

        private final /* synthetic */ void $m$1() {
            ((AnonymousClass11) this.-$f1).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$11_37560((BaseEuiccCommandCallback) this.-$f2, this.-$f0);
        }

        private final /* synthetic */ void $m$2() {
            ((AnonymousClass3) this.-$f1).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$3_29724((BaseEuiccCommandCallback) this.-$f2, this.-$f0);
        }

        private final /* synthetic */ void $m$3() {
            ((AnonymousClass7) this.-$f1).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$7_33731((BaseEuiccCommandCallback) this.-$f2, this.-$f0);
        }

        private final /* synthetic */ void $m$4() {
            ((AnonymousClass8) this.-$f1).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$8_34755((BaseEuiccCommandCallback) this.-$f2, this.-$f0);
        }

        private final /* synthetic */ void $m$5() {
            ((AnonymousClass9) this.-$f1).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$9_35803((BaseEuiccCommandCallback) this.-$f2, this.-$f0);
        }

        public /* synthetic */ AnonymousClass1(byte b, int i, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
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
                case (byte) 4:
                    $m$4();
                    return;
                case (byte) 5:
                    $m$5();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ void $m$0() {
        ((com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass1) this.-$f0).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$1_27371((BaseEuiccCommandCallback) this.-$f1, (String) this.-$f2);
    }

    private final /* synthetic */ void $m$1() {
        ((AnonymousClass2) this.-$f0).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$2_28578((BaseEuiccCommandCallback) this.-$f1, (GetDownloadableSubscriptionMetadataResult) this.-$f2);
    }

    private final /* synthetic */ void $m$2() {
        ((AnonymousClass4) this.-$f0).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$4_30676((BaseEuiccCommandCallback) this.-$f1, (GetEuiccProfileInfoListResult) this.-$f2);
    }

    private final /* synthetic */ void $m$3() {
        ((AnonymousClass5) this.-$f0).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$5_31902((BaseEuiccCommandCallback) this.-$f1, (GetDefaultDownloadableSubscriptionListResult) this.-$f2);
    }

    private final /* synthetic */ void $m$4() {
        ((AnonymousClass6) this.-$f0).lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$6_32764((BaseEuiccCommandCallback) this.-$f1, (EuiccInfo) this.-$f2);
    }

    public /* synthetic */ -$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY(byte b, Object obj, Object obj2, Object obj3) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
        this.-$f2 = obj3;
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
            case (byte) 4:
                $m$4();
                return;
            default:
                throw new AssertionError();
        }
    }
}
