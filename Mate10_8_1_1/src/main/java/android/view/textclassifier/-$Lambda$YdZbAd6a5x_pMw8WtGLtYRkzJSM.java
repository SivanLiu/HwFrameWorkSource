package android.view.textclassifier;

import java.util.Comparator;

final /* synthetic */ class -$Lambda$YdZbAd6a5x_pMw8WtGLtYRkzJSM implements Comparator {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((EntityConfidence) this.-$f0).lambda$-android_view_textclassifier_EntityConfidence_1225(arg0, arg1);
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return ((EntityConfidence) this.-$f0).lambda$-android_view_textclassifier_EntityConfidence_1225(arg0, arg1);
    }

    public /* synthetic */ -$Lambda$YdZbAd6a5x_pMw8WtGLtYRkzJSM(byte b, Object obj) {
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
