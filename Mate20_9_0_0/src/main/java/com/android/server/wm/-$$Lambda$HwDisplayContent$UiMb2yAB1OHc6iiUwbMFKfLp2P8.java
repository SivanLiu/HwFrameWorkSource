package com.android.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.util.MutableBoolean;
import com.android.internal.util.ToBooleanFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwDisplayContent$UiMb2yAB1OHc6iiUwbMFKfLp2P8 implements ToBooleanFunction {
    private final /* synthetic */ HwDisplayContent f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ boolean f$3;
    private final /* synthetic */ IBinder f$4;
    private final /* synthetic */ MutableBoolean f$5;
    private final /* synthetic */ boolean f$6;
    private final /* synthetic */ Rect f$7;
    private final /* synthetic */ Rect f$8;

    public /* synthetic */ -$$Lambda$HwDisplayContent$UiMb2yAB1OHc6iiUwbMFKfLp2P8(HwDisplayContent hwDisplayContent, int i, boolean z, boolean z2, IBinder iBinder, MutableBoolean mutableBoolean, boolean z3, Rect rect, Rect rect2) {
        this.f$0 = hwDisplayContent;
        this.f$1 = i;
        this.f$2 = z;
        this.f$3 = z2;
        this.f$4 = iBinder;
        this.f$5 = mutableBoolean;
        this.f$6 = z3;
        this.f$7 = rect;
        this.f$8 = rect2;
    }

    public final boolean apply(Object obj) {
        return HwDisplayContent.lambda$screenshotApplicationsForExternalDisplay$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, (WindowState) obj);
    }
}
