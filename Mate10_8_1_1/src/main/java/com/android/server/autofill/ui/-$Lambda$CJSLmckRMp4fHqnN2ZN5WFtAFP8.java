package com.android.server.autofill.ui;

import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.IBinder;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.view.autofill.AutofillId;
import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

final /* synthetic */ class -$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8 implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    /* renamed from: com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;

        private final /* synthetic */ void $m$0() {
            ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_6451((AutoFillUiCallback) this.-$f1, (FillResponse) this.-$f2, (AutofillId) this.-$f3, (String) this.-$f4, (LogMaker) this.-$f5);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
            this.-$f4 = obj5;
            this.-$f5 = obj6;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;
        private final /* synthetic */ Object -$f6;
        private final /* synthetic */ Object -$f7;
        private final /* synthetic */ Object -$f8;
        private final /* synthetic */ Object -$f9;

        private final /* synthetic */ void $m$0() {
            ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_9969((AutoFillUiCallback) this.-$f1, (PendingUi) this.-$f2, (CharSequence) this.-$f3, (Drawable) this.-$f4, (String) this.-$f5, (String) this.-$f6, (SaveInfo) this.-$f7, (ValueFinder) this.-$f8, (LogMaker) this.-$f9);
        }

        public /* synthetic */ AnonymousClass2(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7, Object obj8, Object obj9, Object obj10) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
            this.-$f4 = obj5;
            this.-$f5 = obj6;
            this.-$f6 = obj7;
            this.-$f7 = obj8;
            this.-$f8 = obj9;
            this.-$f9 = obj10;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8$3 */
    final /* synthetic */ class AnonymousClass3 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((AutoFillUI) this.-$f1).lambda$-com_android_server_autofill_ui_AutoFillUI_12114(this.-$f0, (IBinder) this.-$f2);
        }

        public /* synthetic */ AnonymousClass3(int i, Object obj, Object obj2) {
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.autofill.ui.-$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8$4 */
    final /* synthetic */ class AnonymousClass4 implements Runnable {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0() {
            ((AutoFillUI) this.-$f1).lambda$-com_android_server_autofill_ui_AutoFillUI_12732((PendingUi) this.-$f2, (AutoFillUiCallback) this.-$f3, this.-$f0);
        }

        public /* synthetic */ AnonymousClass4(boolean z, Object obj, Object obj2, Object obj3) {
            this.-$f0 = z;
            this.-$f1 = obj;
            this.-$f2 = obj2;
            this.-$f3 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_4872((AutoFillUiCallback) this.-$f1, (String) this.-$f2);
    }

    private final /* synthetic */ void $m$1() {
        ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_4184((AutoFillUiCallback) this.-$f1, (CharSequence) this.-$f2);
    }

    public /* synthetic */ -$Lambda$CJSLmckRMp4fHqnN2ZN5WFtAFP8(byte b, Object obj, Object obj2, Object obj3) {
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
            default:
                throw new AssertionError();
        }
    }
}
