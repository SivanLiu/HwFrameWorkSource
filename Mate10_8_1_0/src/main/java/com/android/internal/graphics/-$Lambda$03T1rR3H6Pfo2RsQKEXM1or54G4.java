package com.android.internal.graphics;

final /* synthetic */ class -$Lambda$03T1rR3H6Pfo2RsQKEXM1or54G4 implements ContrastCalculator {
    public static final /* synthetic */ -$Lambda$03T1rR3H6Pfo2RsQKEXM1or54G4 $INST$0 = new -$Lambda$03T1rR3H6Pfo2RsQKEXM1or54G4();

    /* renamed from: com.android.internal.graphics.-$Lambda$03T1rR3H6Pfo2RsQKEXM1or54G4$1 */
    final /* synthetic */ class AnonymousClass1 implements ContrastCalculator {
        private final /* synthetic */ int -$f0;

        private final /* synthetic */ double $m$0(int arg0, int arg1, int arg2) {
            return ColorUtils.calculateContrast(arg0, ColorUtils.setAlphaComponent(ColorUtils.blendARGB(this.-$f0, arg1, ((float) arg2) / 255.0f), 255));
        }

        public /* synthetic */ AnonymousClass1(int i) {
            this.-$f0 = i;
        }

        public final double calculateContrast(int i, int i2, int i3) {
            return $m$0(i, i2, i3);
        }
    }

    private final /* synthetic */ double $m$0(int arg0, int arg1, int arg2) {
        return ColorUtils.calculateContrast(ColorUtils.setAlphaComponent(arg0, arg2), arg1);
    }

    private /* synthetic */ -$Lambda$03T1rR3H6Pfo2RsQKEXM1or54G4() {
    }

    public final double calculateContrast(int i, int i2, int i3) {
        return $m$0(i, i2, i3);
    }
}
