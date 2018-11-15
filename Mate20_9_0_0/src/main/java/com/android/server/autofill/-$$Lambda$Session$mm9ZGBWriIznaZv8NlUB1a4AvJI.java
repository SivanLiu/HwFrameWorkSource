package com.android.server.autofill;

import android.os.Bundle;
import android.os.RemoteCallback.OnResultListener;
import android.util.ArraySet;
import android.view.autofill.AutofillId;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Session$mm9ZGBWriIznaZv8NlUB1a4AvJI implements OnResultListener {
    private final /* synthetic */ Session f$0;
    private final /* synthetic */ ArraySet f$1;
    private final /* synthetic */ ArrayList f$10;
    private final /* synthetic */ ArrayList f$11;
    private final /* synthetic */ ArrayList f$2;
    private final /* synthetic */ ArrayList f$3;
    private final /* synthetic */ ArrayList f$4;
    private final /* synthetic */ ArrayList f$5;
    private final /* synthetic */ int f$6;
    private final /* synthetic */ AutofillId[] f$7;
    private final /* synthetic */ String[] f$8;
    private final /* synthetic */ String[] f$9;

    public /* synthetic */ -$$Lambda$Session$mm9ZGBWriIznaZv8NlUB1a4AvJI(Session session, ArraySet arraySet, ArrayList arrayList, ArrayList arrayList2, ArrayList arrayList3, ArrayList arrayList4, int i, AutofillId[] autofillIdArr, String[] strArr, String[] strArr2, ArrayList arrayList5, ArrayList arrayList6) {
        this.f$0 = session;
        this.f$1 = arraySet;
        this.f$2 = arrayList;
        this.f$3 = arrayList2;
        this.f$4 = arrayList3;
        this.f$5 = arrayList4;
        this.f$6 = i;
        this.f$7 = autofillIdArr;
        this.f$8 = strArr;
        this.f$9 = strArr2;
        this.f$10 = arrayList5;
        this.f$11 = arrayList6;
    }

    public final void onResult(Bundle bundle) {
        Session.lambda$logFieldClassificationScoreLocked$1(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, bundle);
    }
}
