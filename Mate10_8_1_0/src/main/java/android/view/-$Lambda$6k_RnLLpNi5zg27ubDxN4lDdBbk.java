package android.view;

import android.view.FocusFinder.UserSpecifiedFocusComparator.NextFocusGetter;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$6k_RnLLpNi5zg27ubDxN4lDdBbk implements NextFocusGetter {
    public static final /* synthetic */ -$Lambda$6k_RnLLpNi5zg27ubDxN4lDdBbk $INST$0 = new -$Lambda$6k_RnLLpNi5zg27ubDxN4lDdBbk((byte) 0);
    public static final /* synthetic */ -$Lambda$6k_RnLLpNi5zg27ubDxN4lDdBbk $INST$1 = new -$Lambda$6k_RnLLpNi5zg27ubDxN4lDdBbk((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: android.view.-$Lambda$6k_RnLLpNi5zg27ubDxN4lDdBbk$1 */
    final /* synthetic */ class AnonymousClass1 implements Comparator {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
            return ((FocusSorter) this.-$f0).lambda$-android_view_FocusFinder$FocusSorter_31960((View) arg0, (View) arg1);
        }

        private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
            return ((FocusSorter) this.-$f0).lambda$-android_view_FocusFinder$FocusSorter_32420((View) arg0, (View) arg1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final int compare(Object obj, Object obj2) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(obj, obj2);
                case (byte) 1:
                    return $m$1(obj, obj2);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ View $m$0(View arg0, View arg1) {
        return FocusFinder.lambda$-android_view_FocusFinder_2148(arg0, arg1);
    }

    private final /* synthetic */ View $m$1(View arg0, View arg1) {
        return FocusFinder.lambda$-android_view_FocusFinder_2406(arg0, arg1);
    }

    private /* synthetic */ -$Lambda$6k_RnLLpNi5zg27ubDxN4lDdBbk(byte b) {
        this.$id = b;
    }

    public final View get(View view, View view2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(view, view2);
            case (byte) 1:
                return $m$1(view, view2);
            default:
                throw new AssertionError();
        }
    }
}
