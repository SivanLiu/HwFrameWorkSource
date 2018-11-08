package android.widget;

import android.transition.Transition;
import android.transition.Transition.TransitionListener;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewTreeObserver.OnScrollChangedListener;

final /* synthetic */ class -$Lambda$ISuHLqeK-K4pmesAfzlFglc3xF4 implements OnLayoutChangeListener {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: android.widget.-$Lambda$ISuHLqeK-K4pmesAfzlFglc3xF4$1 */
    final /* synthetic */ class AnonymousClass1 implements OnScrollChangedListener {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((PopupWindow) this.-$f0).-android_widget_PopupWindow-mthref-0();
        }

        private final /* synthetic */ void $m$1() {
            ((PopupWindow) this.-$f0).-android_widget_PopupWindow-mthref-0();
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void onScrollChanged() {
            switch (this.$id) {
                case (byte) 0:
                    $m$0();
                    return;
                case (byte) 1:
                    $m$1();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: android.widget.-$Lambda$ISuHLqeK-K4pmesAfzlFglc3xF4$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0() {
            ((PopupDecorView) this.-$f0).lambda$-android_widget_PopupWindow$PopupDecorView_98089((TransitionListener) this.-$f1, (Transition) this.-$f2, (View) this.-$f3);
        }

        public /* synthetic */ AnonymousClass2(Object obj, Object obj2, Object obj3, Object obj4) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0(View arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8) {
        ((PopupWindow) this.-$f0).lambda$-android_widget_PopupWindow_10383(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    private final /* synthetic */ void $m$1(View arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8) {
        ((PopupWindow) this.-$f0).lambda$-android_widget_PopupWindow_10383(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public /* synthetic */ -$Lambda$ISuHLqeK-K4pmesAfzlFglc3xF4(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(view, i, i2, i3, i4, i5, i6, i7, i8);
                return;
            case (byte) 1:
                $m$1(view, i, i2, i3, i4, i5, i6, i7, i8);
                return;
            default:
                throw new AssertionError();
        }
    }
}
