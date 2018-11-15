package com.android.server.autofill;

import android.os.Bundle;
import android.os.RemoteCallback;
import android.service.autofill.IAutofillFieldClassificationService;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FieldClassificationStrategy$gMvCLMmbA3vJ4CLoCwKkhYrCMsQ implements Command {
    private final /* synthetic */ RemoteCallback f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ Bundle f$2;
    private final /* synthetic */ List f$3;
    private final /* synthetic */ String[] f$4;

    public /* synthetic */ -$$Lambda$FieldClassificationStrategy$gMvCLMmbA3vJ4CLoCwKkhYrCMsQ(RemoteCallback remoteCallback, String str, Bundle bundle, List list, String[] strArr) {
        this.f$0 = remoteCallback;
        this.f$1 = str;
        this.f$2 = bundle;
        this.f$3 = list;
        this.f$4 = strArr;
    }

    public final void run(IAutofillFieldClassificationService iAutofillFieldClassificationService) {
        iAutofillFieldClassificationService.getScores(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
    }
}
