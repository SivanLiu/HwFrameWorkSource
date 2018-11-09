package android.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

final /* synthetic */ class -$Lambda$tTszxdFZ0V9nXhnBpPsqeBMO0fw implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: android.widget.-$Lambda$tTszxdFZ0V9nXhnBpPsqeBMO0fw$1 */
    final /* synthetic */ class AnonymousClass1 implements Supplier {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ Object $m$0() {
            return ((TextClassificationHelper) this.-$f0).classifyText();
        }

        private final /* synthetic */ Object $m$1() {
            return ((TextClassificationHelper) this.-$f0).suggestSelection();
        }

        private final /* synthetic */ Object $m$2() {
            return ((TextClassificationHelper) this.-$f0).classifyText();
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final Object get() {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0();
                case (byte) 1:
                    return $m$1();
                case (byte) 2:
                    return $m$2();
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        ((SelectionActionModeHelper) this.-$f0).-android_widget_SelectionActionModeHelper-mthref-4((SelectionResult) arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((SelectionActionModeHelper) this.-$f0).-android_widget_SelectionActionModeHelper-mthref-2((SelectionResult) arg0);
    }

    public /* synthetic */ -$Lambda$tTszxdFZ0V9nXhnBpPsqeBMO0fw(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
