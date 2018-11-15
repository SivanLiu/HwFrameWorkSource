package android.os;

import java.util.Iterator;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HidlSupport$oV2DlGQSAfcavBj7TK20nYhwS0U implements Predicate {
    private final /* synthetic */ Iterator f$0;

    public /* synthetic */ -$$Lambda$HidlSupport$oV2DlGQSAfcavBj7TK20nYhwS0U(Iterator it) {
        this.f$0 = it;
    }

    public final boolean test(Object obj) {
        return HidlSupport.deepEquals(this.f$0.next(), obj);
    }
}
