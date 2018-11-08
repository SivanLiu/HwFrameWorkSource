package com.android.server.autofill.ui;

import android.view.WindowManager.LayoutParams;
import com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback;

final /* synthetic */ class -$Lambda$TTOM-vgvIOJotO3pKgpKhg7oNlE implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0() {
        ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_3592((AutoFillUiCallback) this.-$f1);
    }

    private final /* synthetic */ void $m$1() {
        ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_12481((AutoFillUiCallback) this.-$f1);
    }

    private final /* synthetic */ void $m$2() {
        ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_4598((AutoFillUiCallback) this.-$f1);
    }

    private final /* synthetic */ void $m$3() {
        ((AutoFillUI) this.-$f0).lambda$-com_android_server_autofill_ui_AutoFillUI_3272((AutoFillUiCallback) this.-$f1);
    }

    private final /* synthetic */ void $m$4() {
        ((AutofillWindowPresenter) this.-$f0).lambda$-com_android_server_autofill_ui_FillUi$AutofillWindowPresenter_13643((LayoutParams) this.-$f1);
    }

    public /* synthetic */ -$Lambda$TTOM-vgvIOJotO3pKgpKhg7oNlE(byte b, Object obj, Object obj2) {
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
