package com.android.internal.graphics;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ColorUtils$r4F1J7Y711kBAemoOw8QHbDba_8 implements ContrastCalculator {
    public static final /* synthetic */ -$$Lambda$ColorUtils$r4F1J7Y711kBAemoOw8QHbDba_8 INSTANCE = new -$$Lambda$ColorUtils$r4F1J7Y711kBAemoOw8QHbDba_8();

    private /* synthetic */ -$$Lambda$ColorUtils$r4F1J7Y711kBAemoOw8QHbDba_8() {
    }

    public final double calculateContrast(int i, int i2, int i3) {
        return ColorUtils.calculateContrast(ColorUtils.setAlphaComponent(i, i3), i2);
    }
}
