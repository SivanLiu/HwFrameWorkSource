package android.text;

import android.graphics.Path;
import android.graphics.Path.Direction;
import android.text.Layout.SelectionRectangleConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Layout$MzjK2UE2G8VG0asK8_KWY3gHAmY implements SelectionRectangleConsumer {
    private final /* synthetic */ Path f$0;

    public /* synthetic */ -$$Lambda$Layout$MzjK2UE2G8VG0asK8_KWY3gHAmY(Path path) {
        this.f$0 = path;
    }

    public final void accept(float f, float f2, float f3, float f4, int i) {
        this.f$0.addRect(f, f2, f3, f4, Direction.CW);
    }
}
