package android.widget;

import android.graphics.RectF;
import android.text.Layout.SelectionRectangleConsumer;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SelectionActionModeHelper$cMbIRcH-yFkksR3CQmROa0_hmgM implements SelectionRectangleConsumer {
    private final /* synthetic */ List f$0;

    public /* synthetic */ -$$Lambda$SelectionActionModeHelper$cMbIRcH-yFkksR3CQmROa0_hmgM(List list) {
        this.f$0 = list;
    }

    public final void accept(float f, float f2, float f3, float f4, int i) {
        SelectionActionModeHelper.mergeRectangleIntoList(this.f$0, new RectF(f, f2, f3, f4), -$$Lambda$ChL7kntlZCrPaPVdRfaSzGdk1JU.INSTANCE, new -$$Lambda$SelectionActionModeHelper$mSUWA79GbPno-4-1PEW8ZDcf0L0(i));
    }
}
