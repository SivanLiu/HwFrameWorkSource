package android.app;

import android.app.WallpaperManager.OnColorsChangedListener;
import android.util.Pair;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$zUW-hE_1K7BzT3PNwqZSM6y8x_4 implements Predicate {
    private final /* synthetic */ Object -$f0;

    /* renamed from: android.app.-$Lambda$zUW-hE_1K7BzT3PNwqZSM6y8x_4$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;

        private final /* synthetic */ void $m$0() {
            ((Globals) this.-$f2).lambda$-android_app_WallpaperManager$Globals_14226((Pair) this.-$f3, (WallpaperColors) this.-$f4, this.-$f0, this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(int i, int i2, Object obj, Object obj2, Object obj3) {
            this.-$f0 = i;
            this.-$f1 = i2;
            this.-$f2 = obj;
            this.-$f3 = obj2;
            this.-$f4 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return Globals.lambda$-android_app_WallpaperManager$Globals_13261((OnColorsChangedListener) this.-$f0, (Pair) arg0);
    }

    public /* synthetic */ -$Lambda$zUW-hE_1K7BzT3PNwqZSM6y8x_4(Object obj) {
        this.-$f0 = obj;
    }

    public final boolean test(Object obj) {
        return $m$0(obj);
    }
}
