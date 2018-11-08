package com.android.server.autofill;

import android.content.Intent;
import android.content.IntentSender;
import android.service.autofill.Dataset;
import android.service.autofill.ValueFinder;
import android.view.autofill.AutofillId;

final /* synthetic */ class -$Lambda$TkN02ChLwiW_wnL90EeXYJOcz-Q implements ValueFinder {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.autofill.-$Lambda$TkN02ChLwiW_wnL90EeXYJOcz-Q$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0() {
            ((Session) this.-$f1).lambda$-com_android_server_autofill_Session_23958(this.-$f0, (IntentSender) this.-$f2, (Intent) this.-$f3);
        }

        public /* synthetic */ AnonymousClass1(int i, Object obj, Object obj2, Object obj3) {
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
            this.-$f3 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.autofill.-$Lambda$TkN02ChLwiW_wnL90EeXYJOcz-Q$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0() {
            ((Session) this.-$f2).lambda$-com_android_server_autofill_Session_24580(this.-$f0, this.-$f1, (Dataset) this.-$f3);
        }

        public /* synthetic */ AnonymousClass2(int i, int i2, Object obj, Object obj2) {
            this.-$f0 = i;
            this.-$f1 = i2;
            this.-$f2 = obj;
            this.-$f3 = obj2;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ String $m$0(AutofillId arg0) {
        return ((Session) this.-$f0).lambda$-com_android_server_autofill_Session_37701(arg0);
    }

    public /* synthetic */ -$Lambda$TkN02ChLwiW_wnL90EeXYJOcz-Q(Object obj) {
        this.-$f0 = obj;
    }

    public final String findByAutofillId(AutofillId autofillId) {
        return $m$0(autofillId);
    }
}
