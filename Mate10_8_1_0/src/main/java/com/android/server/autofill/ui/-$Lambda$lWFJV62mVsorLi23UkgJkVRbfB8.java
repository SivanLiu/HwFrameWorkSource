package com.android.server.autofill.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.view.View;
import android.view.View.OnClickListener;

final /* synthetic */ class -$Lambda$lWFJV62mVsorLi23UkgJkVRbfB8 implements OnDismissListener {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.autofill.ui.-$Lambda$lWFJV62mVsorLi23UkgJkVRbfB8$1 */
    final /* synthetic */ class AnonymousClass1 implements OnClickListener {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(View arg0) {
            ((SaveUi) this.-$f0).lambda$-com_android_server_autofill_ui_SaveUi_11768(arg0);
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void onClick(View view) {
            $m$0(view);
        }
    }

    private final /* synthetic */ void $m$0(DialogInterface arg0) {
        ((SaveUi) this.-$f0).lambda$-com_android_server_autofill_ui_SaveUi_12236(arg0);
    }

    public /* synthetic */ -$Lambda$lWFJV62mVsorLi23UkgJkVRbfB8(Object obj) {
        this.-$f0 = obj;
    }

    public final void onDismiss(DialogInterface dialogInterface) {
        $m$0(dialogInterface);
    }
}
