package com.android.internal.graphics;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ColorUtils$zbDH-52c8D9XBeqmvTHi3Boxl14 implements ContrastCalculator {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$ColorUtils$zbDH-52c8D9XBeqmvTHi3Boxl14(int i) {
        this.f$0 = i;
    }

    public final double calculateContrast(int i, int i2, int i3) {
        return ColorUtils.calculateContrast(i, ColorUtils.setAlphaComponent(ColorUtils.blendARGB(this.f$0, i2, ((float) i3) / 1132396544), 255));
    }
}
