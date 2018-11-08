package com.android.server.autofill.ui;

import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.view.View;
import android.view.View.OnClickListener;

final /* synthetic */ class -$Lambda$At7ghLP7ePb8IjX4T-3J791grHE implements OnClickListener {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0(View arg0) {
        ((FillUi) this.-$f0).lambda$-com_android_server_autofill_ui_FillUi_5115((FillResponse) this.-$f1, arg0);
    }

    private final /* synthetic */ void $m$1(View arg0) {
        ((SaveUi) this.-$f0).lambda$-com_android_server_autofill_ui_SaveUi_11594((SaveInfo) this.-$f1, arg0);
    }

    public /* synthetic */ -$Lambda$At7ghLP7ePb8IjX4T-3J791grHE(byte b, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final void onClick(View view) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(view);
                return;
            case (byte) 1:
                $m$1(view);
                return;
            default:
                throw new AssertionError();
        }
    }
}
